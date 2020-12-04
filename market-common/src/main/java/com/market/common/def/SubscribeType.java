package com.market.common.def;

/**
 * @author yjt
 * @since 2020/10/13 下午4:01
 */
public enum SubscribeType {
    /**
     * k线订阅
     */
    KLINE,
    /**
     * 深度订阅
     */
    DEPTH,
    /**
     * 市场概括订阅
     */
    DETAIL,
    /**
     * 最新成交记录订阅
     */
    BBO;

    public String toValue() {
        return name().toLowerCase();
    }
}
