package com.market.publish.cmd.impl.kline;

import com.market.common.messages.payload.detail.TradeDetailResp;
import com.market.common.messages.payload.detail.TradeDetailTickData;
import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.GZIPUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TradeDetailProcessCmd implements Cmd {
    @Override
    public boolean canExecute(JsonObject json) {
        String ch = json.getString("ch");
        if (ch == null) {
            return false;
        }
        return ch.contains(".trade.detail");
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        String ch = json.getString("ch");
        TradeDetailResp detail = json.mapTo(TradeDetailResp.class);
        // 更新交易缓存并且推送
        this.broadcastTradeDetail(ctx, ch, this.updateCache(detail, ctx));
    }

    /**
     * 更新交易缓存
     *
     * @param detail 交易详情
     * @param ctx    上下文
     * @return 更新后的缓存
     */
    private Buffer updateCache(TradeDetailResp detail, PublishContext ctx) {
        // 获取该订阅历史成交数据列表
        List<TradeDetailTickData> cache = ctx.getLatestTradeCache()
                .computeIfAbsent(detail.getCh(), k -> new ArrayList<>());
        // 将当前交易详情类加进缓存数据列表
        cache.addAll(detail.getTick().getData());

        // 只保留30条历史数据
        List<TradeDetailTickData> finalCache = cache;
        if (cache.size() > 30) {
            finalCache = cache.subList(cache.size() - 30, cache.size() - 1);
        }
        ctx.getLatestTradeCache().put(detail.getCh(), finalCache);

        // 重新构建 detail, 然后转换为buffer, 目的是用户订阅的时候无需再转换为json
        // 即可发送全量历史数据
        detail.getTick().setData(finalCache);
        Buffer buffer = null;
        try {
            buffer = GZIPUtils.compress(Json.encodeToBuffer(detail));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (buffer != null) {
            ctx.getLatestTradeBufferCache().put(detail.getCh(), buffer);
        }
        return buffer;
    }

    /**
     * 广播交易详情
     *
     * @param ctx    上下文
     * @param sub    订阅
     * @param buffer 数据
     */
    private void broadcastTradeDetail(PublishContext ctx, String sub, Buffer buffer) {
        if (buffer == null) {
            return;
        }
        ctx.getKlineSM().values()
                .forEach((wrapper -> {
                    if (wrapper.isSubTradeDetail(sub)) {
                        wrapper.getSocket().write(buffer);
                    }
                }));
    }
}
