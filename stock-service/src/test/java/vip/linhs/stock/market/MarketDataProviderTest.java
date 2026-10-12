package vip.linhs.stock.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import vip.linhs.stock.model.po.DailyIndex;

public class MarketDataProviderTest {

    @Test
    public void mockProviderReturnsDeterministicStockData() {
        MockMarketDataProvider provider = new MockMarketDataProvider();

        MarketDataResult<?> result = provider.getStockList();

        assertTrue(result.isSuccess());
        assertEquals("mock", result.getSource());
        assertEquals(2, ((java.util.List<?>) result.getData()).size());
    }

    @Test
    public void mockProviderReturnsDailyDataForRequestedCode() {
        MockMarketDataProvider provider = new MockMarketDataProvider();

        MarketDataResult<?> result = provider.getDailyIndex(Arrays.asList("sz.000001"));

        assertTrue(result.isSuccess());
        DailyIndex dailyIndex = (DailyIndex) ((java.util.List<?>) result.getData()).get(0);
        assertEquals("sz.000001", dailyIndex.getCode());
    }
}
