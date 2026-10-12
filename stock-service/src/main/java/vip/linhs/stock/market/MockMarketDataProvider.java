package vip.linhs.stock.market;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Component;

import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.po.StockInfo;

@Component("mockMarketDataProvider")
public class MockMarketDataProvider implements MarketDataProvider {

    private static final String SOURCE = "mock";

    @Override
    public MarketDataResult<List<StockInfo>> getStockList() {
        StockInfo sh = stock("600000", "浦发银行", "sh");
        StockInfo sz = stock("000001", "平安银行", "sz");
        return MarketDataResult.success(SOURCE, Arrays.asList(sh, sz));
    }

    @Override
    public MarketDataResult<List<DailyIndex>> getDailyIndex(List<String> codes) {
        DailyIndex index = new DailyIndex();
        index.setCode(codes == null || codes.isEmpty() ? "sh.600000" : codes.get(0));
        index.setDate(new Date());
        index.setOpeningPrice(new BigDecimal("10.00"));
        index.setPreClosingPrice(new BigDecimal("9.95"));
        index.setHighestPrice(new BigDecimal("10.18"));
        index.setClosingPrice(new BigDecimal("10.12"));
        index.setLowestPrice(new BigDecimal("9.91"));
        index.setTradingVolume(1200000L);
        index.setTradingValue(new BigDecimal("12000000.00"));
        index.setRurnoverRate(new BigDecimal("1.20"));
        return MarketDataResult.success(SOURCE, Arrays.asList(index));
    }

    @Override
    public MarketDataResult<List<IntradayPoint>> getIntraday(String code) {
        List<IntradayPoint> points = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            IntradayPoint point = new IntradayPoint();
            point.setTime(String.format("09:%02d", 30 + i * 5));
            point.setPrice(new BigDecimal("10.00").add(new BigDecimal(i).multiply(new BigDecimal("0.01"))));
            point.setAveragePrice(point.getPrice());
            point.setVolume((long) (1000 + i * 100));
            points.add(point);
        }
        return MarketDataResult.success(SOURCE, points);
    }

    @Override
    public MarketDataResult<String> health() {
        return MarketDataResult.success(SOURCE, "available");
    }

    private StockInfo stock(String code, String name, String exchange) {
        StockInfo stock = new StockInfo();
        stock.setCode(code);
        stock.setName(name);
        stock.setExchange(exchange);
        stock.setAbbreviation(name);
        stock.setState(0);
        stock.setType(0);
        return stock;
    }
}
