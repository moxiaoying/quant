package vip.linhs.stock.model.vo;

import java.math.BigDecimal;
import java.util.Date;

public class MarketKlineVo {
    private Date date;
    private BigDecimal openingPrice;
    private BigDecimal highestPrice;
    private BigDecimal lowestPrice;
    private BigDecimal closingPrice;
    private Long tradingVolume;
    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal bollUpper;
    private BigDecimal bollMiddle;
    private BigDecimal bollLower;
    private BigDecimal macd;
    private BigDecimal macdSignal;
    private BigDecimal macdHistogram;
    private BigDecimal rsi;
    private BigDecimal k;
    private BigDecimal d;
    private BigDecimal j;

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public BigDecimal getOpeningPrice() { return openingPrice; }
    public void setOpeningPrice(BigDecimal value) { this.openingPrice = value; }
    public BigDecimal getHighestPrice() { return highestPrice; }
    public void setHighestPrice(BigDecimal value) { this.highestPrice = value; }
    public BigDecimal getLowestPrice() { return lowestPrice; }
    public void setLowestPrice(BigDecimal value) { this.lowestPrice = value; }
    public BigDecimal getClosingPrice() { return closingPrice; }
    public void setClosingPrice(BigDecimal value) { this.closingPrice = value; }
    public Long getTradingVolume() { return tradingVolume; }
    public void setTradingVolume(Long value) { this.tradingVolume = value; }
    public BigDecimal getMa5() { return ma5; }
    public void setMa5(BigDecimal value) { this.ma5 = value; }
    public BigDecimal getMa10() { return ma10; }
    public void setMa10(BigDecimal value) { this.ma10 = value; }
    public BigDecimal getMa20() { return ma20; }
    public void setMa20(BigDecimal value) { this.ma20 = value; }
    public BigDecimal getMa60() { return ma60; }
    public void setMa60(BigDecimal value) { this.ma60 = value; }
    public BigDecimal getBollUpper() { return bollUpper; }
    public void setBollUpper(BigDecimal value) { this.bollUpper = value; }
    public BigDecimal getBollMiddle() { return bollMiddle; }
    public void setBollMiddle(BigDecimal value) { this.bollMiddle = value; }
    public BigDecimal getBollLower() { return bollLower; }
    public void setBollLower(BigDecimal value) { this.bollLower = value; }
    public BigDecimal getMacd() { return macd; }
    public void setMacd(BigDecimal value) { this.macd = value; }
    public BigDecimal getMacdSignal() { return macdSignal; }
    public void setMacdSignal(BigDecimal value) { this.macdSignal = value; }
    public BigDecimal getMacdHistogram() { return macdHistogram; }
    public void setMacdHistogram(BigDecimal value) { this.macdHistogram = value; }
    public BigDecimal getRsi() { return rsi; }
    public void setRsi(BigDecimal value) { this.rsi = value; }
    public BigDecimal getK() { return k; }
    public void setK(BigDecimal value) { this.k = value; }
    public BigDecimal getD() { return d; }
    public void setD(BigDecimal value) { this.d = value; }
    public BigDecimal getJ() { return j; }
    public void setJ(BigDecimal value) { this.j = value; }
}
