package vip.linhs.stock.trade;

import java.util.Date;

public class OrderResult {
    private boolean success;
    private String orderId;
    private String state;
    private String message;
    private Date timestamp;

    public static OrderResult success(String orderId, String state) {
        OrderResult result = new OrderResult();
        result.success = true;
        result.orderId = orderId;
        result.state = state;
        result.message = "ok";
        result.timestamp = new Date();
        return result;
    }

    public static OrderResult failure(String message) {
        OrderResult result = new OrderResult();
        result.success = false;
        result.state = "REJECTED";
        result.message = message;
        result.timestamp = new Date();
        return result;
    }

    public boolean isSuccess() { return success; }
    public String getOrderId() { return orderId; }
    public String getState() { return state; }
    public String getMessage() { return message; }
    public Date getTimestamp() { return timestamp; }
}
