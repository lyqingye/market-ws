package com.market.publish.cmd;

import com.market.common.session.WsSessionWrapper;
import com.market.publish.context.PublishContext;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author yjt
 * @since 2020/11/14 18:21
 */
public interface Cmd {

    boolean canExecute(JsonObject json);

    default boolean canExecute(JsonArray jsonArray) {return false;}

    default void execute (JsonArray jsonArray,PublishContext ctx, WsSessionWrapper curSession) {
        throw new UnsupportedOperationException();
    }

    void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession);
}
