package com.market.publish.cmd.impl.detail;

import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.RequestUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class MarketDetailSubCmd implements Cmd {
    @Override
    public boolean canExecute(JsonObject json) {
        return RequestUtils.isDetailSubscribeReq(json);
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        String sub = json.getString("sub");
        curSession.subMarketDetail(sub);
        ctx.getDetailSM().put(curSession.getSocket().textHandlerID(), curSession);
        // 发送历史消息
        Buffer buffer = ctx.getMarketDetailCache().get(sub);
        if (buffer != null) {
            curSession.getSocket().write(buffer);
        }
    }
}
