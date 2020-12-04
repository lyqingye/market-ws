package com.market.common.def;

/**
 * @author yjt
 * @since 2020/10/11 11:49
 */
public enum MessageType {
    /**
     * 市场价格变动消息
     */
    MARKET_PRICE,
    /**
     * 深度图消息
     */
    DEPTH_CHART,
    /**
     * 交易结果消息
     */
    TRADE_RESULT;

    public static MessageType ofName(String name) {
        for (MessageType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public static MessageType valueOf(byte ordinal) {
        for (MessageType t : values()) {
            if (t.ordinal() == ordinal) {
                return t;
            }
        }
        return null;
    }
}
