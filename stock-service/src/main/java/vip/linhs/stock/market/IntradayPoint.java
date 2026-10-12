package vip.linhs.stock.market;

import java.math.BigDecimal;

public class IntradayPoint {
    private String time;
    private BigDecimal price;
    private BigDecimal averagePrice;
    private Long volume;
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
}
