package vip.linhs.stock.trade;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class MockBrokerAdapter implements BrokerAdapter {

    private final Map<String, OrderCommand> orders = new ConcurrentHashMap<>();

    @Override
    public OrderResult submit(OrderCommand command) {
        if (command == null || command.getStockCode() == null || command.getStockCode().trim().isEmpty()) {
            return OrderResult.failure("stockCode is required");
        }
        if (!"BUY".equalsIgnoreCase(command.getSide()) && !"SELL".equalsIgnoreCase(command.getSide())) {
            return OrderResult.failure("side must be BUY or SELL");
        }
        if (command.getPrice() == null || command.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return OrderResult.failure("price must be positive");
        }
        if (command.getQuantity() <= 0) {
            return OrderResult.failure("quantity must be positive");
        }
        String orderId = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        orders.put(orderId, command);
        return OrderResult.success(orderId, "FILLED");
    }

    @Override
    public OrderResult cancel(String orderId) {
        if (orderId == null || !orders.containsKey(orderId)) {
            return OrderResult.failure("order not found");
        }
        orders.remove(orderId);
        return OrderResult.success(orderId, "CANCELLED");
    }
}
