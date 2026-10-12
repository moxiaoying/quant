package vip.linhs.stock.service;

import java.util.Map;

public interface StockDataSyncService {
    Map<String, Object> start(boolean backfill, String startDate, String endDate);

    Map<String, Object> status();
}
