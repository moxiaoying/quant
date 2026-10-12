package vip.linhs.stock.market;

import java.util.List;

import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.po.StockInfo;

public interface MarketDataProvider {

    MarketDataResult<List<StockInfo>> getStockList();

    MarketDataResult<List<DailyIndex>> getDailyIndex(List<String> codes);

    MarketDataResult<List<IntradayPoint>> getIntraday(String code);

    MarketDataResult<String> health();
}
