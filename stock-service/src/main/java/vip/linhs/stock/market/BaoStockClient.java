package vip.linhs.stock.market;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.InflaterInputStream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Minimal BaoStock TCP client ported from stock_quant's baostock provider.
 */
public final class BaoStockClient implements AutoCloseable {
    private static final String HOST = "public-api.baostock.com";
    private static final int PORT = 10030;
    private static final String SEP = "\1";
    private static final String END = "<![CDATA[]]>\n";
    private static final String VERSION = "00.9.30";
    private static final int HEADER_LENGTH = 21;
    private final Socket socket;
    private final String userId = "anonymous";

    public BaoStockClient() throws IOException {
        socket = new Socket(HOST, PORT);
        socket.setSoTimeout(30000);
        ensureSuccess(request("00", "login" + SEP + userId + SEP + "123456" + SEP + "0", false));
    }

    public List<String> tradeDates(String start, String end) throws IOException {
        String body = "query_trade_dates" + SEP + userId + SEP + "1" + SEP + "2000" + SEP + start + SEP + end;
        Response response = request("33", body, false);
        ensureSuccess(response);
        JSONArray records = records(response.bodyParts[6]);
        List<String> dates = new ArrayList<>();
        for (Object value : records) {
            JSONArray row = (JSONArray) value;
            if (row.size() > 1 && "1".equals(row.getString(1))) dates.add(row.getString(0));
        }
        return dates;
    }

    public List<DailyBar> daily(String code, String start, String end) throws IOException {
        String body = "query_history_k_data_plus" + SEP + userId + SEP + "1" + SEP + "2000"
                + SEP + code + SEP + "date,open,high,low,close,volume,amount"
                + SEP + start + SEP + end + SEP + "d" + SEP + "3";
        Response response = request("95", body, true);
        ensureSuccess(response);
        JSONArray records = records(response.bodyParts[6]);
        List<DailyBar> bars = new ArrayList<>();
        for (Object value : records) {
            JSONArray row = (JSONArray) value;
            if (row.size() < 7 || row.getString(4) == null || row.getString(4).isEmpty()) continue;
            bars.add(new DailyBar(row.getString(0), decimal(row.getString(1)),
                    decimal(row.getString(2)), decimal(row.getString(3)),
                    decimal(row.getString(4)), decimal(row.getString(5)),
                    decimal(row.getString(6))));
        }
        return bars;
    }

    private static BigDecimal decimal(String value) {
        return value == null || value.isEmpty() ? null : new BigDecimal(value);
    }

    private static JSONArray records(String json) {
        JSONObject root = JSON.parseObject(json == null || json.isEmpty() ? "{}" : json);
        JSONArray records = root.getJSONArray("record");
        return records == null ? new JSONArray() : records;
    }

    private static void ensureSuccess(Response response) throws IOException {
        if (response.bodyParts.length < 2 || !"0".equals(response.bodyParts[0])) {
            throw new IOException("BaoStock error " + (response.bodyParts.length > 0 ? response.bodyParts[0] : "unknown")
                    + ": " + (response.bodyParts.length > 1 ? response.bodyParts[1] : ""));
        }
    }

    private Response request(String type, String body, boolean compressed) throws IOException {
        String header = VERSION + SEP + type + SEP + String.format("%010d",
                body.getBytes(StandardCharsets.UTF_8).length);
        String headBody = header + body;
        CRC32 crc = new CRC32();
        crc.update(headBody.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write((headBody + SEP + crc.getValue() + "\n").getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        byte[] received = readMessage();
        String responseHeader = new String(received, 0, HEADER_LENGTH, StandardCharsets.UTF_8);
        int bodyLength = Integer.parseInt(responseHeader.substring(11, 21));
        byte[] payload = Arrays.copyOfRange(received, HEADER_LENGTH, HEADER_LENGTH + bodyLength);
        if (compressed) payload = inflate(payload);
        return new Response(new String(payload, StandardCharsets.UTF_8).split(SEP, -1));
    }

    private byte[] readMessage() throws IOException {
        InputStream input = socket.getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (true) {
            int count = input.read(buffer);
            if (count < 0) throw new IOException("BaoStock connection closed");
            output.write(buffer, 0, count);
            byte[] value = output.toByteArray();
            if (endsWith(value, END.getBytes(StandardCharsets.UTF_8))) {
                return Arrays.copyOf(value, value.length - END.length());
            }
        }
    }

    private static boolean endsWith(byte[] value, byte[] suffix) {
        if (value.length < suffix.length) return false;
        for (int i = 1; i <= suffix.length; i++) {
            if (value[value.length - i] != suffix[suffix.length - i]) return false;
        }
        return true;
    }

    private static byte[] inflate(byte[] payload) throws IOException {
        InflaterInputStream stream = new InflaterInputStream(new ByteArrayInputStream(payload));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) >= 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    @Override
    public void close() throws IOException {
        try {
            request("02", "logout" + SEP + userId + SEP + System.currentTimeMillis(), false);
        } finally {
            socket.close();
        }
    }

    public static final class DailyBar {
        public final String date;
        public final BigDecimal open;
        public final BigDecimal high;
        public final BigDecimal low;
        public final BigDecimal close;
        public final BigDecimal volume;
        public final BigDecimal amount;

        DailyBar(String date, BigDecimal open, BigDecimal high, BigDecimal low,
                 BigDecimal close, BigDecimal volume, BigDecimal amount) {
            this.date = date;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.amount = amount;
        }
    }

    private static final class Response {
        final String[] bodyParts;
        Response(String[] bodyParts) {
            this.bodyParts = bodyParts;
        }
    }
}
