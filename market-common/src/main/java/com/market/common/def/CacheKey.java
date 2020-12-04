package com.market.common.def;

/**
 * @author ex
 */
public enum CacheKey {
    /**
     * 自定义交易对转通用交易对
     */
    SYMBOL_CUSTOM_TO_GENERIC,

    /**
     * 通用交易对转自定义交易对
     */
    SYMBOL_GENERIC_TO_CUSTOM,

    /**
     * 市场概括
     */
    MARKET_DETAIL,

    /**
     * 市场价格
     */
    MARKET_PRICE;
}
