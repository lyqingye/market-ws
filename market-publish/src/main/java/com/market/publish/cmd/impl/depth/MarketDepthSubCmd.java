package com.market.publish.cmd.impl.depth;

import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.RequestUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class MarketDepthSubCmd implements Cmd {
    @Override
    public boolean canExecute(JsonObject json) {
        return RequestUtils.isDepthSubscribeReq(json);
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        String sub = json.getString("sub");
        curSession.subDepth(sub);
        ctx.getDepthSM().put(curSession.getSocket().textHandlerID(), curSession);
        // 发送历史数据
        Buffer buffer = ctx.getDepthChartCache().get(sub);
        if (buffer != null) {
            curSession.getSocket().write(buffer);
        }
    }
}
