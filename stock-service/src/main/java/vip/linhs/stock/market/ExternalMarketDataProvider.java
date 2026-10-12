package vip.linhs.stock.market;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.po.StockInfo;
import vip.linhs.stock.service.StockCrawlerService;

@Component("externalMarketDataProvider")
public class ExternalMarketDataProvider implements MarketDataProvider {

    private static final String SOURCE = "sina";

    @Autowired
    private StockCrawlerService stockCrawlerService;

    @Override
    public MarketDataResult<List<StockInfo>> getStockList() {
        try {
            List<StockInfo> data = stockCrawlerService.getStockList();
            if (data == null || data.isEmpty()) {
                return MarketDataResult.failure(SOURCE, "sina stock provider returned no data");
            }
            return MarketDataResult.success(SOURCE, data);
        } catch (Exception e) {
            return MarketDataResult.failure(SOURCE, e.getMessage());
        }
    }

    @Override
    public MarketDataResult<List<DailyIndex>> getDailyIndex(List<String> codes) {
        try {
            List<DailyIndex> data = stockCrawlerService.getDailyIndex(codes == null ? Collections.emptyList() : codes);
            if (data == null || data.isEmpty()) {
                return MarketDataResult.failure(SOURCE, "sina daily provider returned no data");
            }
            return MarketDataResult.success(SOURCE, data);
        } catch (Exception e) {
            return MarketDataResult.failure(SOURCE, e.getMessage());
        }
    }

    @Override
    public MarketDataResult<List<IntradayPoint>> getIntraday(String code) {
        try {
            List<IntradayPoint> data = stockCrawlerService.getIntraday(code);
            return data == null || data.isEmpty()
                    ? MarketDataResult.failure(SOURCE, "sina intraday provider returned no data")
                    : MarketDataResult.success(SOURCE, data);
        } catch (Exception e) {
            return MarketDataResult.failure(SOURCE, e.getMessage());
        }
    }

    @Override
    public MarketDataResult<String> health() {
        return MarketDataResult.success(SOURCE, "configured; HTTP polling, not WebSocket");
    }
}
