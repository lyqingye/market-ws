package com.market.publish.cmd.impl.kline;

import com.market.common.ds.TimeWheel;
import com.market.common.messages.payload.kline.KlinePullHistoryReq;
import com.market.common.messages.payload.kline.KlineTemplateResp;
import com.market.common.messages.payload.kline.KlineTickResp;
import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.GZIPUtils;
import com.market.common.utils.RequestUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;


/**
 * @author yjt
 * @since 2020/11/14 18:27
 */
public class KlinePullCmd implements Cmd {

    @Override
    public boolean canExecute(JsonObject json) {
        return RequestUtils.isPullHistoryReq(json);
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        KlinePullHistoryReq req = json.mapTo(KlinePullHistoryReq.class);
        // 拉取历史消息也算是订阅 ps: 前端组件问题
        curSession.subKline(req.getReq());
        // 根据订阅内容从时间轮获取k线数据历史数据
        TimeWheel<KlineTickResp> timeWheel = ctx.getTickTimeWheelCache().get(req.getReq());
        Collection<KlineTickResp> history = Collections.emptyList();
        if (timeWheel != null) {
            history = timeWheel.pull(req.getFrom() * 1000,
                    req.getTo() * 1000,
                    req.getPartIdx());
        }
        // 转换为json二进制
        Buffer jsonBuffer = Json.encodeToBuffer(KlineTemplateResp.ok(req.getId(), req.getReq(), history));
        // 异步压缩
        GZIPUtils.compressAsync(ctx.getVertx(),jsonBuffer, ar -> {
            if (ar.succeeded()) {
                curSession.getSocket().write(ar.result());
            }else {
                ar.cause().printStackTrace();
            }
        });
    }
}
