package vip.linhs.stock.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import vip.linhs.stock.market.BaoStockClient;
import vip.linhs.stock.service.StockDataSyncService;

@Service
public class StockDataSyncServiceImpl implements StockDataSyncService {
    private final Logger logger = LoggerFactory.getLogger(StockDataSyncServiceImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final Executor executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String lastMessage = "未执行";
    private volatile int lastExitCode = 0;
    @Value("${stock.data.sync.concurrency:2}")
    private int concurrency;

    public StockDataSyncServiceImpl(JdbcTemplate jdbcTemplate,
            @Qualifier("threadPoolTaskExecutor") Executor threadPoolTaskExecutor) {
        this.jdbcTemplate = jdbcTemplate;
        this.executor = threadPoolTaskExecutor;
    }

    @Override
    public Map<String, Object> start(boolean backfill, String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!running.compareAndSet(false, true)) {
            result.put("started", false);
            result.put("message", "数据同步正在运行");
            return result;
        }
        result.put("started", true);
        result.put("message", "Java BaoStock 数据同步已启动");
        executor.execute(() -> run(backfill, startDate, endDate));
        return result;
    }

    private void run(boolean backfill, String startDate, String endDate) {
        String start = startDate == null || startDate.trim().isEmpty() ? "2024-01-01" : startDate;
        String end = endDate == null || endDate.trim().isEmpty()
                ? java.time.LocalDate.now().toString() : endDate;
        AtomicInteger rows = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        try (BaoStockClient calendarClient = new BaoStockClient()) {
            Set<String> tradingDates = new HashSet<>(calendarClient.tradeDates(start, end));
            List<String> codes = jdbcTemplate.query(
                    "select concat(exchange, '.', code) from stock_info where state <> 2",
                    (rs, rowNum) -> rs.getString(1));
            int workers = Math.max(1, Math.min(2, concurrency));
            ExecutorService pool = Executors.newFixedThreadPool(workers);
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                final int index = worker;
                futures.add(pool.submit(() -> syncPartition(
                        codes, index, workers, tradingDates, backfill, start, end, rows, failed)));
            }
            for (Future<?> future : futures) future.get();
            pool.shutdown();
            lastExitCode = failed.get() == 0 ? 0 : 1;
            lastMessage = "完成：写入 " + rows.get() + " 条，失败 " + failed.get() + " 只";
        } catch (Exception e) {
            lastExitCode = -1;
            lastMessage = "同步失败：" + e.getMessage();
            logger.error("BaoStock sync failed", e);
        } finally {
            running.set(false);
        }
    }

    private void syncPartition(List<String> codes, int index, int workers,
            Set<String> tradingDates, boolean backfill, String start, String end,
            AtomicInteger rows, AtomicInteger failed) {
        try (BaoStockClient client = new BaoStockClient()) {
            for (int i = index; i < codes.size(); i += workers) {
                String code = codes.get(i);
                try {
                    String fetchStart = backfill ? start : nextDate(code, start);
                    Set<String> existing = existingDates(code, start, end);
                    List<String> missing = new ArrayList<>(tradingDates);
                    missing.removeAll(existing);
                    if (!missing.isEmpty()) {
                        java.util.Collections.sort(missing);
                        if (missing.get(0).compareTo(fetchStart) < 0) fetchStart = missing.get(0);
                    }
                    if (fetchStart.compareTo(end) > 0) continue;
                    List<BaoStockClient.DailyBar> bars = client.daily(code, fetchStart, end);
                    rows.addAndGet(save(code, bars));
                    logger.info("BaoStock worker {} {} wrote {} rows", index, code, bars.size());
                } catch (Exception e) {
                    failed.incrementAndGet();
                    logger.error("BaoStock sync failed for {}", code, e);
                }
            }
        } catch (Exception e) {
            failed.incrementAndGet();
            logger.error("BaoStock worker {} failed", index, e);
        }
    }

    private String nextDate(String code, String fallback) {
        java.sql.Date date = jdbcTemplate.query(
                "select max(trade_date) from stock_daily where symbol = ? and adjust_flag = 3",
                ps -> ps.setString(1, code),
                rs -> rs.next() ? rs.getDate(1) : null);
        return date == null ? fallback : date.toLocalDate().plusDays(1).toString();
    }

    private Set<String> existingDates(String code, String start, String end) {
        return new HashSet<>(jdbcTemplate.query(
                "select date_format(trade_date, '%Y-%m-%d') from stock_daily "
                        + "where symbol = ? and adjust_flag = 3 and trade_date between ? and ?",
                ps -> {
                    ps.setString(1, code);
                    ps.setString(2, start);
                    ps.setString(3, end);
                },
                (rs, rowNum) -> rs.getString(1)));
    }

    private int save(String code, List<BaoStockClient.DailyBar> bars) {
        if (bars.isEmpty()) return 0;
        BigDecimal previous = jdbcTemplate.query(
                "select close_price from stock_daily where symbol = ? and adjust_flag = 3 and trade_date < ? "
                        + "order by trade_date desc limit 1",
                ps -> {
                    ps.setString(1, code);
                    ps.setString(2, bars.get(0).date);
                },
                rs -> rs.next() ? rs.getBigDecimal(1) : null);
        List<Object[]> args = new ArrayList<>();
        for (BaoStockClient.DailyBar bar : bars) {
            BigDecimal preClose = previous == null ? bar.close : previous;
            args.add(new Object[] {code, bar.date, bar.open, preClose, bar.high, bar.close,
                    bar.low, bar.volume == null ? 0 : bar.volume.longValue(),
                    bar.amount, BigDecimal.ZERO});
            previous = bar.close;
        }
        String sql = "insert into stock_daily "
                + "(symbol,trade_date,open_price,pre_close_price,high_price,close_price,"
                + "low_price,volume,amount,turnover,adjust_flag,source) "
                + "values (?,?,?,?,?,?,?,?,?,?,3,'baostock') on duplicate key update "
                + "open_price=values(open_price),pre_close_price=values(pre_close_price),"
                + "high_price=values(high_price),close_price=values(close_price),"
                + "low_price=values(low_price),volume=values(volume),"
                + "amount=values(amount),turnover=values(turnover),source='baostock',"
                + "updated_at=current_timestamp";
        jdbcTemplate.batchUpdate(sql, args);
        return args.size();
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("running", running.get());
        result.put("exitCode", lastExitCode);
        result.put("message", lastMessage);
        return result;
    }
}
