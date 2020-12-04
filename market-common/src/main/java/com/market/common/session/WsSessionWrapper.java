package com.market.common.session;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.DefaultAttributeMap;
import io.vertx.core.http.ServerWebSocket;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author yjt
 * @since 2020/9/28 下午4:44
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class WsSessionWrapper extends DefaultAttributeMap {
    /**
     * 深度订阅key
     */
    public static final AttributeKey<String> DEPTH_SUBSCRIBE_KEY = AttributeKey.valueOf("DEPTH_SUBSCRIBE");

    /**
     * K线订阅key
     */
    public static final AttributeKey<String> KLINE_SUBSCRIBE_KEY = AttributeKey.valueOf("KLINE_SUBSCRIBE");

    /**
     * 市场详情订阅key (可能同时订阅多个)
     */
    public static final AttributeKey<String> DETAIL_SUBSCRIBE_KEY = AttributeKey.valueOf("DETAIL_SUBSCRIBE");

    /**
     * 交易详情订阅key
     */
    public static final AttributeKey<String> TRADE_DETAIL_SUBSCRIBE_KEY = AttributeKey.valueOf("TRADE_DETAIL_SUBSCRIBE");

    /**
     * socket
     */
    private final ServerWebSocket socket;

    public WsSessionWrapper(ServerWebSocket socket) {
        this.socket = socket;
    }

    public static WsSessionWrapper of(ServerWebSocket socket) {
        return new WsSessionWrapper(socket);
    }

    /**
     * 深度订阅
     *
     * @param sub 订阅内容
     */
    public void subDepth(String sub) {
        this.attr(DEPTH_SUBSCRIBE_KEY).set(sub);
    }

    /**
     * 给定一个订阅内容, 判断当前session是否已经订阅了该深度
     *
     * @param sub 订阅内容
     * @return 是否订阅
     */
    public boolean isSubDepthSub(String sub) {
        return sub.equalsIgnoreCase(this.attr(DEPTH_SUBSCRIBE_KEY).get());
    }

    /**
     * 订阅k线
     *
     * @param sub 订阅内容
     */
    public void subKline(String sub) {
        this.attr(KLINE_SUBSCRIBE_KEY).set(sub);
    }

    /**
     * 给定一个订阅内容, 判断当前session是否已经订阅了该k线
     *
     * @param sub 订阅内容
     * @return 是否订阅
     */
    public boolean isSubKlineSub(String sub) {
        return sub.equalsIgnoreCase(this.attr(KLINE_SUBSCRIBE_KEY).get());
    }

    /**
     * 获取k线订阅
     *
     * @return 订阅
     */
    public String getKlineSub() {
        return this.attr(KLINE_SUBSCRIBE_KEY).get();
    }

    /**
     * 订阅市场详情 (支持多个同时订阅)
     *
     * @param sub 订阅内容
     */
    public void subMarketDetail(String sub) {
        Attribute<String> value = this.attr(DETAIL_SUBSCRIBE_KEY);
        if (value.get() == null) {
            value.set(sub);
        } else if (!value.get().contains(sub)) {
            value.set(value.get() + "|" + sub);
        }
    }

    /**
     * 是否订阅了该市场详情
     *
     * @param sub 订阅内容
     */
    public boolean isSubMarketDetail(String sub) {
        String actual = this.attr(DETAIL_SUBSCRIBE_KEY).get();
        return actual != null && actual.contains(sub);
    }

    /**
     * 订阅交易详情
     *
     * @param sub 订阅内容
     */
    public void subTradeDetail(String sub) {
        this.attr(TRADE_DETAIL_SUBSCRIBE_KEY).set(sub);
    }

    /**
     * 是否订阅该交易详情
     *
     * @param sub 订阅内容
     */
    public boolean isSubTradeDetail(String sub) {
        return sub.equalsIgnoreCase(this.attr(TRADE_DETAIL_SUBSCRIBE_KEY).get());
    }
}
