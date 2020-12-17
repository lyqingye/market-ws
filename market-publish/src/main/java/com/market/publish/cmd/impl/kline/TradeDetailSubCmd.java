package com.market.publish.cmd.impl.kline;

import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.RequestUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * @author yjt
 * @since 2020/11/14 18:28
 */
public class TradeDetailSubCmd implements Cmd {
    @Override
    public boolean canExecute(JsonObject json) {
        return RequestUtils.isTradeDetailSubscribeReq(json);
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        String sub = json.getString("sub");
        Buffer buffer = ctx.getLatestTradeBufferCache().get(sub);
        curSession.subTradeDetail(sub);
        if (buffer != null) {
            curSession.getSocket().write(buffer);
        }
    }
}
