from __future__ import annotations

import argparse
import logging
import os
from datetime import date, timedelta

import pymysql

from .provider import normalize_market_code, query_daily, query_stock_names, query_trade_dates

LOG = logging.getLogger("stock-data")


def connection():
    return pymysql.connect(
        host=os.getenv("STOCK_DB_HOST", "127.0.0.1"),
        port=int(os.getenv("STOCK_DB_PORT", "3306")),
        user=os.getenv("STOCK_DB_USER", "root"),
        password=os.getenv("STOCK_DB_PASSWORD", "123456"),
        database=os.getenv("STOCK_DB_NAME", "stock_master"),
        charset="utf8mb4",
        autocommit=False,
    )


def _symbols(conn) -> list[str]:
    with conn.cursor() as cur:
        cur.execute("SELECT CONCAT(exchange, '.', code) FROM stock_info WHERE state <> 2")
        return [str(row[0]) for row in cur.fetchall()]


def _last_date(conn, code: str):
    with conn.cursor() as cur:
        cur.execute("SELECT MAX(date) FROM daily_index WHERE code=%s", (code,))
        return cur.fetchone()[0]


def _previous_close(conn, code: str, before_date: str) -> float | None:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT closing_price FROM daily_index "
            "WHERE code=%s AND date < %s ORDER BY date DESC LIMIT 1",
            (code, before_date),
        )
        row = cur.fetchone()
    return float(row[0]) if row and row[0] is not None else None


def _existing_dates(conn, code: str, start: str, end: str) -> set[str]:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT DATE_FORMAT(date, '%%Y-%%m-%%d') FROM daily_index "
            "WHERE code=%s AND date BETWEEN %s AND %s",
            (code, start, end),
        )
        return {str(row[0]) for row in cur.fetchall()}


def sync_symbol(conn, code: str, name: str | None, start: str, end: str) -> int:
    canonical = normalize_market_code(code)
    frame = query_daily(canonical, start, end, adjustflag=os.getenv("BAOSTOCK_ADJUSTFLAG", "3"))
    if frame.empty:
        return 0
    rows = []
    previous = _previous_close(conn, canonical, start)
    for row in frame.itertuples(index=False):
        close = float(row.close)
        pre_close = previous if previous is not None else close
        previous = close
        rows.append((
            canonical, row.date, row.open, pre_close, row.high, row.close, row.low,
            int(row.volume), row.turnover, 0,
        ))
    sql = """
    INSERT INTO daily_index
      (code,date,opening_price,pre_closing_price,highest_price,closing_price,
       lowest_price,trading_volume,trading_value,rurnover_rate)
    VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
    ON DUPLICATE KEY UPDATE
      opening_price=VALUES(opening_price),
      pre_closing_price=VALUES(pre_closing_price),
      highest_price=VALUES(highest_price),
      closing_price=VALUES(closing_price),
      lowest_price=VALUES(lowest_price),
      trading_volume=VALUES(trading_volume),
      trading_value=VALUES(trading_value),
      rurnover_rate=VALUES(rurnover_rate)
    """
    with conn.cursor() as cur:
        cur.executemany(sql, rows)
    return len(rows)


def run(backfill: bool, symbols: list[str] | None, start_date: str, end_date: str) -> int:
    conn = connection()
    total = 0
    try:
        try:
            names = query_stock_names()
        except Exception as exc:
            LOG.warning("stock names unavailable, continuing without names: %s", exc)
            names = {}
        selected = symbols or _symbols(conn)
        trade_dates = set(query_trade_dates(start_date, end_date))
        for code in selected:
            canonical = normalize_market_code(code)
            start = start_date
            if not backfill:
                last = _last_date(conn, canonical)
                if last:
                    start = (last + timedelta(days=1)).isoformat()
            existing = _existing_dates(conn, canonical, start_date, end_date)
            missing = sorted(trade_dates - existing)
            if missing:
                start = min(start, missing[0])
            if start > end_date or not trade_dates.intersection(
                {d for d in trade_dates if start <= d <= end_date}
            ):
                continue
            try:
                count = sync_symbol(conn, canonical, names.get(canonical.split(".", 1)[1]), start, end_date)
                conn.commit()
                total += count
                LOG.info("%s: wrote %d rows", canonical, count)
            except Exception:
                conn.rollback()
                LOG.exception("%s: sync failed", code)
        return total
    finally:
        conn.close()


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync BaoStock daily data into stock-master MySQL")
    parser.add_argument("--backfill", action="store_true")
    parser.add_argument("--symbols", help="comma-separated codes, e.g. sh.600000,sz.000001")
    parser.add_argument("--start-date", default=os.getenv("STOCK_DATA_START_DATE", "2024-01-01"))
    parser.add_argument("--end-date", default=date.today().isoformat())
    args = parser.parse_args()
    logging.basicConfig(level=os.getenv("STOCK_DATA_LOG_LEVEL", "INFO"),
                        format="%(asctime)s %(levelname)s %(message)s")
    symbols = [x.strip() for x in args.symbols.split(",") if x.strip()] if args.symbols else None
    count = run(args.backfill, symbols, args.start_date, args.end_date)
    LOG.info("sync complete: %d rows", count)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
