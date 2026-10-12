package vip.linhs.stock.trade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class MockBrokerAdapterTest {

    @Test
    public void rejectsNonPositiveQuantity() {
        MockBrokerAdapter broker = new MockBrokerAdapter();

        OrderResult result = broker.submit(new OrderCommand("sh600000", "BUY", new BigDecimal("10.00"), 0));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("quantity"));
    }

    @Test
    public void submitsAndCancelsMockOrder() {
        MockBrokerAdapter broker = new MockBrokerAdapter();

        OrderResult submitted = broker.submit(new OrderCommand("sh600000", "BUY", new BigDecimal("10.00"), 100));
        OrderResult cancelled = broker.cancel(submitted.getOrderId());

        assertTrue(submitted.isSuccess());
        assertTrue(cancelled.isSuccess());
        assertTrue("CANCELLED".equals(cancelled.getState()));
    }
}
