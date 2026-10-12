package vip.linhs.stock.service;

import java.util.List;
import vip.linhs.stock.model.vo.MarketKlineVo;

public interface MarketKlineService {
    List<MarketKlineVo> query(String symbol, String period, int limit);
}
