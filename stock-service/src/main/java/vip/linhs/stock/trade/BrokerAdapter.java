package vip.linhs.stock.trade;

public interface BrokerAdapter {
    OrderResult submit(OrderCommand command);
    OrderResult cancel(String orderId);
}
