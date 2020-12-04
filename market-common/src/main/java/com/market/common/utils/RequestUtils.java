package com.market.common.utils;

import com.market.common.def.DepthLevel;
import com.market.common.def.Period;
import io.vertx.core.json.JsonObject;

/**
 * @author yjt
 * @since 2020/9/28 下午2:06
 */
public final class RequestUtils {

    public static boolean isPongReq(JsonObject obj) {
        if (obj == null) {
            return false;
        }
        return obj.containsKey("pong");
    }

    public static boolean isSubscribeReq(JsonObject obj) {
        if (obj == null) {
            return false;
        }

        return obj.containsKey("sub");
    }

    public static boolean isDepthSubscribeReq(JsonObject obj) {
        if (isSubscribeReq(obj)) {
            String sub = obj.getString("sub");

            if (sub != null) {
                return sub.contains("depth");
            }
        }
        return false;
    }

    public static boolean isDepthUnSubscribeReq(JsonObject obj) {
        if (isUnSubscribeReq(obj)) {
            String sub = obj.getString("unsub");

            if (sub != null) {
                return sub.contains("depth");
            }
        }
        return false;
    }

    public static boolean isKlineSubscribeReq(JsonObject obj) {
        if (isSubscribeReq(obj)) {
            String sub = obj.getString("sub");

            if (sub != null) {
                return sub.contains("kline");
            }
        }
        return false;
    }

    public static boolean isKlineUnSubscribeReq(JsonObject obj) {
        if (isUnSubscribeReq(obj)) {
            String sub = obj.getString("unsub");

            if (sub != null) {
                return sub.contains("kline");
            }
        }
        return false;
    }

    public static boolean isDetailSubscribeReq(JsonObject obj) {
        if (isSubscribeReq(obj)) {
            String sub = obj.getString("sub");

            String[] split = sub.split("\\.");

            if (split.length != 3) {
                return false;
            }
            return "detail".equals(split[2]);
        }
        return false;
    }

    public static boolean isTradeDetailSubscribeReq(JsonObject obj) {
        if (isSubscribeReq(obj)) {
            String sub = obj.getString("sub");

            String[] split = sub.split("\\.");

            if (split.length != 4) {
                return false;
            }
            return "trade".equals(split[2]) && "detail".equals(split[3]);
        }
        return false;
    }

    public static boolean isDetailUnSubscribeReq(JsonObject obj) {
        if (isUnSubscribeReq(obj)) {
            String sub = obj.getString("unsub");

            String[] split = sub.split("\\.");

            if (split.length != 3) {
                return false;
            }
            return "detail".equals(split[2]);
        }
        return false;
    }


    public static boolean isUnSubscribeReq(JsonObject obj) {
        if (obj == null) {
            return false;
        }

        return obj.containsKey("unsub");
    }

    public static boolean isPullHistoryReq(JsonObject obj) {
        if (obj == null) {
            return false;
        }

        return obj.containsKey("req") &&
                obj.containsKey("from") &&
                obj.containsKey("to");
    }

    public static String getSymbolFromKlineSub(String sub) {
        String[] split = sub.split("\\.");

        if (split.length != 4) {
            return null;
        }
        return split[1];
    }

    public static String toKlineSub(String symbolId, Period period) {
        return String.format("market.%s.kline.%s", symbolId, period.getSymbol());
    }

    public static String toDepthSub(String symbolId, DepthLevel depth) {
        return String.format("market.%s.depth.%s", symbolId, depth.name());
    }

    public static String toDetailSub(String symbolId) {
        return String.format("market.%s.detail", symbolId);
    }

    public static String toTradeDetailSub(String symbolId) {
        return String.format("market.%s.trade.detail", symbolId);
    }


    public static String toSymbol(String symbol) {
        return symbol.replace("-", "")
                .replace("/", "")
                .toLowerCase();
    }
}
