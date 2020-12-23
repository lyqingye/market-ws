package com.market.bridge;

import com.market.common.messages.bridge.Message;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public interface MarketBridge {

    /**
     * 启动桥接器
     *
     * @param vertx   vertx
     * @param config  配置
     * @param handler 结果处理
     */
    void start(Vertx vertx, JsonObject config, Handler<AsyncResult<MarketBridge>> handler);

    /**
     * 停止桥接器
     *
     * @param handler 结果处理器
     */
    void stop(Handler<AsyncResult<Void>> handler);

    /**
     * 处理消息
     *
     * @param source 来源
     * @param client 处理器
     */
    void process(String source, AsyncResult<Message<?>> client);

    /**
     * 广播消息
     *
     * @param buffer 二进制消息
     */
    void broadcast(Buffer buffer);

    /**
     * 广播消息
     *
     * @param text 文本消息
     */
    void broadcast(String text);

    /**
     * 广播消息并且排除指定socket地址
     *
     * @param buffer     二进制消息
     * @param socketAddr socket地址
     */
    void broadcastWithExcludeSocketAddr(Buffer buffer, String socketAddr);

    /**
     * 广播消息并且排除指定socket地址
     *
     * @param text       文本消息
     * @param socketAddr socket地址
     */
    void broadcastWithExcludeSocketAddr(String text, String socketAddr);
}
