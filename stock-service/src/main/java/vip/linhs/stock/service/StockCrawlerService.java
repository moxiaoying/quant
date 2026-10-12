package vip.linhs.stock.service;

import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.po.StockInfo;
import vip.linhs.stock.market.IntradayPoint;

import java.util.List;

public interface StockCrawlerService {

    List<StockInfo> getStockList();

    DailyIndex getDailyIndex(String code);

    List<DailyIndex> getDailyIndex(List<String> codeList);

    List<IntradayPoint> getIntraday(String code);

    List<DailyIndex> getDailyIndexFromEastMoney();

    List<DailyIndex> getHistoryDailyIndexs(String code);

    String getHistoryDailyIndexsString(String code);

    String getHistoryDailyIndexsStringFrom163(String code, int year, int season);

}
