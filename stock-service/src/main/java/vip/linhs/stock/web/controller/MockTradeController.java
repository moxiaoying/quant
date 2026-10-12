package vip.linhs.stock.web.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vip.linhs.stock.trade.BrokerAdapter;
import vip.linhs.stock.trade.OrderCommand;
import vip.linhs.stock.trade.OrderResult;

@RestController
@RequestMapping("mock-trade")
public class MockTradeController extends BaseController {

    @Autowired
    private BrokerAdapter brokerAdapter;

    @PostMapping("orders")
    public OrderResult submit(String stockCode, String side, BigDecimal price, int quantity) {
        return brokerAdapter.submit(new OrderCommand(stockCode, side, price, quantity));
    }

    @PostMapping("orders/cancel")
    public OrderResult cancel(String orderId) {
        return brokerAdapter.cancel(orderId);
    }
}
