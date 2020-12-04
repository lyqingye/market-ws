package com.market.common.def;

public enum Topics {
    /**
     * K线tick数据主题 (多笔交易合成)
     * {@link com.market.common.messages.payload.kline.KlineTickResp}
     */
    KLINE_TICK_TOPIC,

    /**
     * 本交易细节数据主题 (单笔交易)
     * {@link com.market.common.messages.payload.detail.TradeDetailResp}
     */
    TRADE_DETAIL_TOPIC,

    /**
     * 深度数据主题
     * 是一个 List {@link com.market.common.messages.payload.depth.DepthTickResp}
     */
    DEPTH_CHART_TOPIC,

    /**
     * 最新市场价格主题
     * {@link com.market.common.messages.bridge.PriceChangeMessage}
     */
    MARKET_PRICE_TOPIC,
}

