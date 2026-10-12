package vip.linhs.stock.trade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TradeModePolicyTest {

    @Test
    public void readonlyAllowsHoldingsAndAccountQueries() {
        assertTrue(TradeModePolicy.allows("readonly", "get_stock_list"));
        assertTrue(TradeModePolicy.allows("readonly", "get_asserts"));
        assertTrue(TradeModePolicy.allows("readonly", "authentication"));
    }

    @Test
    public void readonlyRejectsMutatingTradeMethods() {
        assertFalse(TradeModePolicy.allows("readonly", "submit"));
        assertFalse(TradeModePolicy.allows("readonly", "revoke"));
        assertFalse(TradeModePolicy.allows("readonly", "cr_submit"));
    }

    @Test
    public void mockRejectsExternalMethods() {
        assertFalse(TradeModePolicy.allows("mock", "get_stock_list"));
    }
}
