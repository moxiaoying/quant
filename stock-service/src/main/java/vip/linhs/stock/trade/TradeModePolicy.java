package vip.linhs.stock.trade;

public final class TradeModePolicy {

    private TradeModePolicy() {}

    public static boolean allows(String mode, String method) {
        if ("real".equalsIgnoreCase(mode)) {
            return true;
        }
        if (!"readonly".equalsIgnoreCase(mode)) {
            return false;
        }
        return "authentication".equals(method)
                || "authentication_check".equals(method)
                || "yzm".equals(method)
                || method.startsWith("get_")
                || method.startsWith("cr_get_")
                || "cr_query_collateral".equals(method);
    }
}
