package com.market.publish.cmd.impl.kline;

import com.market.common.session.WsSessionWrapper;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.json.JsonObject;

/**
 * @author yjt
 * @since 2020/11/14 18:26
 */
public class KlineUnSubCmd implements Cmd {
    @Override
    public boolean canExecute(JsonObject json) {
        return false;
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {

    }
}
