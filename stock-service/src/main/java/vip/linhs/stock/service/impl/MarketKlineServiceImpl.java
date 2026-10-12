package vip.linhs.stock.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vip.linhs.stock.dao.DailyIndexDao;
import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.vo.MarketKlineVo;
import vip.linhs.stock.service.MarketKlineService;
import vip.linhs.stock.util.StockUtil;

@Service
public class MarketKlineServiceImpl implements MarketKlineService {
    @Autowired private DailyIndexDao dailyIndexDao;

    @Override
    public List<MarketKlineVo> query(String symbol, String period, int limit) {
        String canonical = StockUtil.getCanonicalCode(symbol);
        List<DailyIndex> source = dailyIndexDao.getDailyHistory(canonical, 2000);
        if (!"day".equalsIgnoreCase(period)) source = aggregate(source, period);
        List<MarketKlineVo> result = new ArrayList<>();
        for (DailyIndex row : source) {
            MarketKlineVo v = new MarketKlineVo();
            v.setDate(row.getDate()); v.setOpeningPrice(row.getOpeningPrice());
            v.setHighestPrice(row.getHighestPrice()); v.setLowestPrice(row.getLowestPrice());
            v.setClosingPrice(row.getClosingPrice()); v.setTradingVolume(row.getTradingVolume());
            result.add(v);
        }
        calculate(result);
        int start = Math.max(0, result.size() - Math.max(1, Math.min(limit, 500)));
        return new ArrayList<>(result.subList(start, result.size()));
    }

    private List<DailyIndex> aggregate(List<DailyIndex> rows, String period) {
        Map<String, DailyIndex> grouped = new LinkedHashMap<>();
        for (DailyIndex r : rows) {
            Calendar c = Calendar.getInstance(); c.setTime(r.getDate());
            String key;
            if ("week".equalsIgnoreCase(period)) {
                c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                key = String.format("%tF", c.getTime());
            } else if ("month".equalsIgnoreCase(period)) {
                key = String.format("%tY-%tM", r.getDate(), r.getDate());
            } else {
                key = String.format("%tY", r.getDate());
            }
            DailyIndex current = grouped.get(key);
            if (current == null) {
                current = new DailyIndex(); current.setDate(r.getDate());
                current.setOpeningPrice(r.getOpeningPrice()); current.setHighestPrice(r.getHighestPrice());
                current.setLowestPrice(r.getLowestPrice()); current.setClosingPrice(r.getClosingPrice());
                current.setTradingVolume(r.getTradingVolume()); current.setTradingValue(r.getTradingValue());
                grouped.put(key, current);
            } else {
                current.setDate(r.getDate()); current.setHighestPrice(current.getHighestPrice().max(r.getHighestPrice()));
                current.setLowestPrice(current.getLowestPrice().min(r.getLowestPrice()));
                current.setClosingPrice(r.getClosingPrice());
                current.setTradingVolume(current.getTradingVolume() + r.getTradingVolume());
                current.setTradingValue(current.getTradingValue().add(r.getTradingValue()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    private void calculate(List<MarketKlineVo> rows) {
        BigDecimal ema12 = null, ema26 = null, signal = null;
        BigDecimal k = BigDecimal.valueOf(50), d = BigDecimal.valueOf(50);
        for (int i = 0; i < rows.size(); i++) {
            MarketKlineVo r = rows.get(i); BigDecimal close = r.getClosingPrice();
            r.setMa5(avg(rows, i, 5)); r.setMa10(avg(rows, i, 10)); r.setMa20(avg(rows, i, 20)); r.setMa60(avg(rows, i, 60));
            BigDecimal middle = r.getMa20(); if (middle != null) {
                BigDecimal sd = std(rows, i, 20, middle);
                r.setBollMiddle(middle); r.setBollUpper(middle.add(sd.multiply(BigDecimal.valueOf(2))));
                r.setBollLower(middle.subtract(sd.multiply(BigDecimal.valueOf(2))));
            }
            ema12 = ema(ema12, close, 12); ema26 = ema(ema26, close, 26);
            BigDecimal macd = ema12.subtract(ema26);
            signal = ema(signal, macd, 9); r.setMacd(macd); r.setMacdSignal(signal); r.setMacdHistogram(macd.subtract(signal).multiply(BigDecimal.valueOf(2)));
            r.setRsi(rsi(rows, i, 14));
            int start = Math.max(0, i - 8); BigDecimal high = rows.get(start).getHighestPrice(), low = rows.get(start).getLowestPrice();
            for (int j = start + 1; j <= i; j++) { high = high.max(rows.get(j).getHighestPrice()); low = low.min(rows.get(j).getLowestPrice()); }
            BigDecimal rsv = high.compareTo(low) == 0 ? BigDecimal.valueOf(50) : close.subtract(low).divide(high.subtract(low), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            k = k.multiply(BigDecimal.valueOf(2)).add(rsv).divide(BigDecimal.valueOf(3), 8, RoundingMode.HALF_UP);
            d = d.multiply(BigDecimal.valueOf(2)).add(k).divide(BigDecimal.valueOf(3), 8, RoundingMode.HALF_UP);
            r.setK(k); r.setD(d); r.setJ(k.multiply(BigDecimal.valueOf(3)).subtract(d.multiply(BigDecimal.valueOf(2))));
        }
    }
    private BigDecimal avg(List<MarketKlineVo> a, int i, int n) { int s = Math.max(0, i - n + 1); BigDecimal x = BigDecimal.ZERO; for (int j=s;j<=i;j++) x=x.add(a.get(j).getClosingPrice()); return x.divide(BigDecimal.valueOf(i-s+1), 4, RoundingMode.HALF_UP); }
    private BigDecimal std(List<MarketKlineVo> a, int i, int n, BigDecimal mean) { int s=Math.max(0,i-n+1); BigDecimal x=BigDecimal.ZERO; for(int j=s;j<=i;j++){BigDecimal d=a.get(j).getClosingPrice().subtract(mean); x=x.add(d.multiply(d));} return BigDecimal.valueOf(Math.sqrt(x.divide(BigDecimal.valueOf(i-s+1),8,RoundingMode.HALF_UP).doubleValue())).setScale(4,RoundingMode.HALF_UP); }
    private BigDecimal ema(BigDecimal previous, BigDecimal value, int n) { BigDecimal alpha=BigDecimal.valueOf(2.0/(n+1)); return previous==null?value: value.multiply(alpha).add(previous.multiply(BigDecimal.ONE.subtract(alpha))); }
    private BigDecimal rsi(List<MarketKlineVo> a,int i,int n){if(i==0)return BigDecimal.valueOf(50);int s=Math.max(1,i-n+1);BigDecimal gain=BigDecimal.ZERO,loss=BigDecimal.ZERO;for(int j=s;j<=i;j++){BigDecimal d=a.get(j).getClosingPrice().subtract(a.get(j-1).getClosingPrice());if(d.signum()>0)gain=gain.add(d);else loss=loss.subtract(d);}if(loss.signum()==0)return BigDecimal.valueOf(100);return gain.divide(loss,8,RoundingMode.HALF_UP).divide(BigDecimal.ONE.add(gain.divide(loss,8,RoundingMode.HALF_UP)),8,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));}
}
