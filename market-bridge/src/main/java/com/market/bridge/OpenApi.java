package com.market.bridge;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface OpenApi {
    /**
     * 启动桥接器
     *
     * @param vertx   vertx
     * @param config  配置
     * @param handler 结果处理
     */
    void start(Vertx vertx, JsonObject config, Handler<AsyncResult<OpenApi>> handler);

    /**
     * 停止桥接器
     *
     * @param handler 结果处理器
     */
    void stop(Handler<AsyncResult<Void>> handler);
}
