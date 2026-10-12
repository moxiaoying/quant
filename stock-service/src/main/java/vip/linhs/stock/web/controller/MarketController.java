package vip.linhs.stock.web.controller;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import vip.linhs.stock.market.MarketDataProvider;
import vip.linhs.stock.market.MarketDataResult;
import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.po.StockInfo;
import vip.linhs.stock.market.IntradayPoint;
import vip.linhs.stock.model.vo.MarketKlineVo;
import vip.linhs.stock.service.MarketKlineService;

@RestController
@RequestMapping("market")
public class MarketController extends BaseController {

    private final MarketDataProvider mockProvider;
    private final MarketDataProvider externalProvider;
    private final MarketKlineService marketKlineService;

    @Value("${market.provider:mock}")
    private String providerName;

    public MarketController(@Qualifier("mockMarketDataProvider") MarketDataProvider mockMarketDataProvider,
            @Qualifier("externalMarketDataProvider") MarketDataProvider externalMarketDataProvider,
            MarketKlineService marketKlineService) {
        this.mockProvider = mockMarketDataProvider;
        this.externalProvider = externalMarketDataProvider;
        this.marketKlineService = marketKlineService;
    }

    @RequestMapping(value = "health", method = RequestMethod.GET)
    public MarketDataResult<String> health() {
        return provider().health();
    }

    @RequestMapping(value = "stocks", method = RequestMethod.GET)
    public MarketDataResult<List<StockInfo>> stocks() {
        return provider().getStockList();
    }

    @RequestMapping(value = "daily-index", method = RequestMethod.GET)
    public MarketDataResult<List<DailyIndex>> dailyIndex(String code) {
        return provider().getDailyIndex(code == null ? Arrays.asList("sh.600000") : Arrays.asList(code));
    }

    @RequestMapping(value = "intraday", method = RequestMethod.GET)
    public MarketDataResult<List<IntradayPoint>> intraday(String code) {
        return provider().getIntraday(code == null ? "sh000001" : code);
    }

    /**
     * Current snapshots for the index preview. Codes are comma separated and
     * deliberately pass through the provider boundary so the browser never
     * calls an external market site directly.
     */
    @RequestMapping(value = "indices", method = RequestMethod.GET)
    public MarketDataResult<List<DailyIndex>> indices(String codes) {
        List<String> codeList = new ArrayList<>();
        if (codes == null || codes.trim().isEmpty()) {
            codeList.addAll(Arrays.asList("sh000001", "sz399001", "sz399006", "sh000300", "sh000688", "sh000680"));
        } else {
            for (String code : codes.split(",")) {
                if (code != null && !code.trim().isEmpty()) {
                    codeList.add(code.trim());
                }
            }
        }
        return provider().getDailyIndex(codeList);
    }

    @RequestMapping(value = "kline", method = RequestMethod.GET)
    public MarketDataResult<List<MarketKlineVo>> kline(String code, String period, Integer limit) {
        if (code == null || code.trim().isEmpty()) return MarketDataResult.failure("stock_daily", "code is required");
        return MarketDataResult.success("stock_daily", marketKlineService.query(code, period == null ? "day" : period, limit == null ? 240 : limit));
    }

    private MarketDataProvider provider() {
        return ("external".equalsIgnoreCase(providerName) || "sina".equalsIgnoreCase(providerName))
                ? externalProvider : mockProvider;
    }
}
