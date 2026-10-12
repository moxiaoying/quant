package vip.linhs.stock.trade;

import java.math.BigDecimal;

public class OrderCommand {
    private String stockCode;
    private String side;
    private BigDecimal price;
    private int quantity;

    public OrderCommand() {}

    public OrderCommand(String stockCode, String side, BigDecimal price, int quantity) {
        this.stockCode = stockCode;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }

    public String getStockCode() { return stockCode; }
    public String getSide() { return side; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
}
