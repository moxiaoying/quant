from __future__ import annotations

from contextlib import contextmanager
from datetime import date

import pandas as pd


class BaoStockError(RuntimeError):
    pass


def normalize_market_code(symbol: str) -> str:
    """Return the canonical external code, for example ``sh.600000``."""
    value = str(symbol).strip().lower()
    if "." in value:
        exchange, number = value.split(".", 1)
        if exchange in {"sh", "sz", "bj"}:
            return f"{exchange}.{number.zfill(6)}"
    if value[:2] in {"sh", "sz", "bj"}:
        return f"{value[:2]}.{value[2:].zfill(6)}"
    number = value.zfill(6)
    exchange = "sh" if number.startswith(("6", "9")) else "sz"
    return f"{exchange}.{number}"


def to_stock_master_code(symbol: str) -> str:
    """Return the undotted code used by stock-master MySQL tables."""
    exchange, number = normalize_market_code(symbol).split(".", 1)
    return f"{exchange}{number}"


def to_baostock_code(symbol: str) -> str:
    return normalize_market_code(symbol)


@contextmanager
def logged_in():
    import baostock as bs

    login = bs.login()
    if login.error_code != "0":
        raise BaoStockError(f"baostock login failed: {login.error_msg}")
    try:
        yield bs
    finally:
        bs.logout()


def query_stock_names() -> dict[str, str]:
    with logged_in() as bs:
        result = bs.query_stock_basic(code="", code_name="")
        if result.error_code != "0":
            raise BaoStockError(result.error_msg)
        names: dict[str, str] = {}
        while result.next():
            row = result.get_row_data()
            if len(row) >= 2 and row[0] and row[1]:
                names[row[0].split(".")[-1]] = row[1].strip()
        return names


def query_trade_dates(start_date: str, end_date: str) -> list[str]:
    with logged_in() as bs:
        result = bs.query_trade_dates(start_date=start_date, end_date=end_date)
        if result.error_code != "0":
            raise BaoStockError(result.error_msg)
        dates: list[str] = []
        while result.next():
            row = result.get_row_data()
            if len(row) >= 2 and row[1] in {"1", "true", "True"}:
                dates.append(row[0])
        return dates


def query_daily(
    symbol: str,
    start_date: str,
    end_date: str | None = None,
    adjustflag: str = "3",
) -> pd.DataFrame:
    end_date = end_date or date.today().isoformat()
    with logged_in() as bs:
        result = bs.query_history_k_data_plus(
            to_baostock_code(symbol),
            "date,open,high,low,close,volume,amount",
            start_date=start_date,
            end_date=end_date,
            frequency="d",
            adjustflag=adjustflag,
        )
        if result.error_code != "0":
            raise BaoStockError(result.error_msg)
        rows: list[list[str]] = []
        while result.next():
            rows.append(result.get_row_data())
    frame = pd.DataFrame(
        rows,
        columns=["date", "open", "high", "low", "close", "volume", "turnover"],
    )
    for column in frame.columns[1:]:
        frame[column] = pd.to_numeric(frame[column], errors="coerce")
    return frame.dropna(subset=["close"]).query("volume > 0")
