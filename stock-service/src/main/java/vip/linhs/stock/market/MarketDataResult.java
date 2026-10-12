package vip.linhs.stock.market;

import java.util.Date;

public class MarketDataResult<T> {

    private boolean success;
    private String source;
    private Date timestamp;
    private T data;
    private String message;

    public static <T> MarketDataResult<T> success(String source, T data) {
        MarketDataResult<T> result = new MarketDataResult<>();
        result.success = true;
        result.source = source;
        result.timestamp = new Date();
        result.data = data;
        result.message = "ok";
        return result;
    }

    public static <T> MarketDataResult<T> failure(String source, String message) {
        MarketDataResult<T> result = new MarketDataResult<>();
        result.success = false;
        result.source = source;
        result.timestamp = new Date();
        result.message = message;
        return result;
    }

    public boolean isSuccess() { return success; }
    public String getSource() { return source; }
    public Date getTimestamp() { return timestamp; }
    public T getData() { return data; }
    public String getMessage() { return message; }
}
