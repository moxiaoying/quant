package vip.linhs.stock.service.impl;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.po.StockInfo;
import vip.linhs.stock.parser.DailyIndexParser;
import vip.linhs.stock.parser.StockInfoParser;
import vip.linhs.stock.parser.StockInfoParser.EmStock;
import vip.linhs.stock.service.StockCrawlerService;
import vip.linhs.stock.market.IntradayPoint;
import vip.linhs.stock.util.HttpUtil;
import vip.linhs.stock.util.StockUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockCrawlerServiceImpl implements StockCrawlerService {

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    @Qualifier("eastmoneyStockInfoParser")
    private StockInfoParser stockInfoParser;

    @Autowired
    private DailyIndexParser dailyIndexParser;

    @Override
    public List<StockInfo> getStockList() {
        List<EmStock> list = getStockList("f12,f13,f14");
        list.forEach(v -> v.getStockInfo().setAbbreviation(StockUtil.getPinyin(v.getStockInfo().getName())));
        return list.stream().map(EmStock::getStockInfo).collect(Collectors.toList());
    }

    private List<EmStock> getStockList(String fields) {
        String content = HttpUtil.sendGet(httpClient, "http://20.push2.eastmoney.com/api/qt/clist/get?pn=1&pz=10000000&np=1&fid=f3&fields=" + fields + "&fs=m:0+t:6,m:0+t:13,m:0+t:80,m:0+t:81+s:2048,m:1+t:2,m:1+t:23,b:MK0021,b:MK0022,b:MK0023,b:MK0024");
        if (content != null) {
            List<EmStock> list = stockInfoParser.parseStockInfoList(content);
            list = list.stream().filter(v -> v.getStockInfo().getExchange() != null).collect(Collectors.toList());
            return list;
        }
        return Collections.emptyList();
    }

    @Override
    public DailyIndex getDailyIndex(String code) {
        List<DailyIndex> dailyIndexList = getDailyIndex(Collections.singletonList(code));
        return dailyIndexList.isEmpty() ? null : dailyIndexList.get(0);
    }

    @Override
    public List<DailyIndex> getDailyIndex(List<String> codeList) {
        String codes = codeList.stream().map(StockUtil::getFullCode).collect(Collectors.joining(","));
        HashMap<String, String> header = new HashMap<>();
        header.put("Referer", "https://finance.sina.com.cn/");
        String content = HttpUtil.sendGet(httpClient, "https://hq.sinajs.cn/list=" + codes, header, "gbk");
        if (content != null) {
            return dailyIndexParser.parseDailyIndexList(content);
        }
        return Collections.emptyList();
    }

    public List<IntradayPoint> getIntraday(String code) {
        String symbol = StockUtil.getFullCode(code);
        String content = HttpUtil.sendGet(httpClient,
                "https://quotes.sina.cn/cn/api/quotes.php?symbol=" + symbol + "&scale=5&ma=no&datalen=242");
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        JSONObject root = JSON.parseObject(content);
        JSONObject result = root.getJSONObject("result");
        JSONArray data = result == null ? null : result.getJSONArray("data");
        if (data == null) {
            return Collections.emptyList();
        }
        List<IntradayPoint> points = new java.util.ArrayList<>();
        for (Object value : data) {
            JSONObject row = (JSONObject) value;
            IntradayPoint point = new IntradayPoint();
            point.setTime(row.getString("day"));
            point.setPrice(row.getBigDecimal("close"));
            point.setAveragePrice(row.getBigDecimal("ma_price"));
            point.setVolume(row.getLong("volume"));
            if (point.getPrice() != null) {
                points.add(point);
            }
        }
        return points;
    }

    @Override
    public List<DailyIndex> getDailyIndexFromEastMoney() {
        List<EmStock> list = getStockList("f2,f5,f6,f8,f12,f13,f15,f16,f17,f18");
        return list.stream().map(EmStock::getDailyIndex).collect(Collectors.toList());
    }


    @Override
    public List<DailyIndex> getHistoryDailyIndexs(String code) {
        String content = getHistoryDailyIndexsString(code);
        if (content != null) {
            return dailyIndexParser.parseHistoryDailyIndexList(content);
        }
        return Collections.emptyList();
    }

    @Override
    public String getHistoryDailyIndexsString(String code) {
        return HttpUtil.sendGet(httpClient, "http://www.aigaogao.com/tools/history.html?s=" + code, "gbk");
    }

    @Override
    public String getHistoryDailyIndexsStringFrom163(String code, int year, int season) {
        return HttpUtil.sendGet(httpClient, String.format("https://quotes.money.163.com/trade/lsjysj_%s.html?year=%d&season=%d", code, year, season));
    }

}
