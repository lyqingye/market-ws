package com.market.publish.cmd.impl.kline;

import com.market.common.def.Period;
import com.market.common.ds.TimeWheel;
import com.market.common.messages.payload.kline.KlineTickResp;
import com.market.common.messages.payload.kline.KlineTradeResp;
import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.GZIPUtils;
import com.market.common.utils.RequestUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class KlineTickProcessCmd implements Cmd {
    @Override
    public boolean canExecute(JsonObject json) {
        String ch = json.getString("ch");
        if (ch == null) {
            return false;
        }
        return ch.contains(".kline.");
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        String sub = json.getString("ch");
        String symbol = RequestUtils.getSymbolFromKlineSub(sub);
        if (symbol == null) {
            return;
        }
        KlineTickResp tick = json.getJsonObject("tick").mapTo(KlineTickResp.class);
        // 更新到缓存后并推送
        this.broadcastTick(this.updateCache(ctx, symbol, tick), ctx);

        // 生成市场概要并且推送
        this.broadcastDetail(ctx, symbol, ctx.updateMarketDetailTick(symbol));
    }

    /**
     * 更新时间轮并且返回更新后的数据
     *
     * @param ctx    上下文
     * @param symbol 交易对
     * @param tick   tick
     * @return 更新后的数据
     */
    private Map<String, Buffer> updateCache(PublishContext ctx, String symbol,
                                            KlineTickResp tick) {
        Map<String, Buffer> updated = new HashMap<>(Period.values().length);
        for (Period period : Period.values()) {
            TimeWheel<KlineTickResp> wheel = ctx.getOrCreateTimeWheel(symbol, period);
            String key = RequestUtils.toKlineSub(symbol, period);
            Buffer jsonBuffer = Json.encodeToBuffer(new KlineTradeResp(key, wheel.updateNow(tick).clone()));
            try {
                updated.put(key, GZIPUtils.compress(jsonBuffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    /**
     * 广播k线数据
     *
     * @param updateData 需要广播的数据
     */
    private void broadcastTick(Map<String, Buffer> updateData,
                               PublishContext ctx) {
        ctx.getVertx().executeBlocking(promise -> {
            ctx.getKlineSM().values().forEach(wrapper -> {
                String required = wrapper.getKlineSub();
                if (required != null) {
                    Buffer buffer = updateData.get(required);
                    if (buffer != null) {
                        // 只向已经订阅了该交易对的
                        wrapper.getSocket().write(buffer);
                    }
                }
            });
        }, ignored -> {
        });
    }

    /**
     *
     * 广播市场详情
     *
     * @param ctx
     * @param detail
     */
    private void broadcastDetail(PublishContext ctx, String symbol, Buffer detail) {
        String sub = RequestUtils.toDetailSub(symbol);
        ctx.getVertx().executeBlocking(promise -> {
            ctx.getDetailSM().values().forEach(wrapper -> {
                if (wrapper.isSubMarketDetail(sub)) {
                    wrapper.getSocket().write(detail);
                }
            });
        }, ignored -> {
        });
    }
}
