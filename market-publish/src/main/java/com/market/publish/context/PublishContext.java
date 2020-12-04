package com.market.publish.context;

import com.market.common.def.Period;
import com.market.common.ds.TimeWheel;
import com.market.common.messages.payload.detail.MarketDetailResp;
import com.market.common.messages.payload.detail.MarketDetailTick;
import com.market.common.messages.payload.detail.TradeDetailTickData;
import com.market.common.messages.payload.kline.KlineTickResp;
import com.market.common.session.SessionManager;
import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.GZIPUtils;
import com.market.common.utils.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import lombok.Data;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yjt
 * @since 2020/11/14 18:13
 */
@Data
public class PublishContext {
    /**
     * 会话管理
     * <p>
     * channelId -> websocket session
     */
    private SessionManager<WsSessionWrapper> sm = new SessionManager<>(4096);

    /**
     * 订阅K线了的会话
     */
    private Map<String, WsSessionWrapper> klineSM = new ConcurrentHashMap<>(4096);

    /**
     * 订阅深度了的会话
     */
    private Map<String, WsSessionWrapper> depthSM = new ConcurrentHashMap<>(4096);

    /**
     * 订阅市场详情了的会话
     */
    private Map<String, WsSessionWrapper> detailSM = new ConcurrentHashMap<>(4096);

    /**
     * K线实时热点数据
     */
    private Map<String, TimeWheel<KlineTickResp>> tickTimeWheelCache = new ConcurrentHashMap<>(64);

    /**
     * 市场详情缓存数据
     */
    private Map<String, Buffer> marketDetailCache = new ConcurrentHashMap<>();

    /**
     * 最后成交数据缓存
     */
    private Map<String, Buffer> latestTradeBufferCache = new ConcurrentHashMap<>();

    /**
     * 最后成交数据缓存
     */
    private Map<String, List<TradeDetailTickData>> latestTradeCache = new ConcurrentHashMap<>();

    /**
     * 盘口缓存
     */
    private Map<String, Buffer> depthChartCache = new ConcurrentHashMap<>();

    /**
     * vertx 实例
     */
    private Vertx vertx;

    /**
     * hide default constructor
     */
    private PublishContext() {
    }

    public PublishContext(Vertx vertx) {
        this.vertx = Objects.requireNonNull(vertx);
    }

    /**
     * 获取或者创建时间轮
     *
     * @param symbol 交易对
     * @param period 区间
     * @return 时间轮
     */
    public TimeWheel<KlineTickResp> getOrCreateTimeWheel(String symbol,
                                                         Period period) {
        return getTickTimeWheelCache()
                .computeIfAbsent(RequestUtils.toKlineSub(symbol, period),
                        k -> new TimeWheel<>(period.getMill(),
                                period.getNumOfPeriod(),
                                Collections.emptyList()));
    }

    /**
     * 更新市场缓存
     *
     * @param symbol 交易对
     * @return 更新后的缓存
     */
    public Buffer updateMarketDetailTick(String symbol) {
        MarketDetailTick detail = new MarketDetailTick(this.getOrCreateTimeWheel(symbol, Period._1_MIN).toArray());
        // 真正的发送的市场详情对象
        String detailSub = RequestUtils.toDetailSub(symbol);
        MarketDetailResp resp = MarketDetailResp.of(detailSub, detail);
        Buffer buffer = null;
        try {
            buffer = GZIPUtils.compress(Json.encodeToBuffer(resp));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.marketDetailCache.put(detailSub, buffer);
        return buffer;
    }
}
