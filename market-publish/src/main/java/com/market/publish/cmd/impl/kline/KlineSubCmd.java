package com.market.publish.cmd.impl.kline;

import com.market.common.messages.payload.common.SubscribeReq;
import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.RequestUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.json.JsonObject;

/**
 * @author yjt
 * @since 2020/11/14 18:26
 */
public class KlineSubCmd implements Cmd {

    private SubscribeReq req;

    @Override
    public boolean canExecute(JsonObject json) {
        return RequestUtils.isKlineSubscribeReq(json);
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        curSession.subKline(json.getString("sub"));
        // k线会话分区
        ctx.getKlineSM().put(curSession.getSocket().textHandlerID(), curSession);
    }
}
