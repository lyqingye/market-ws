package com.market.common.eventbus;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

import java.util.function.Consumer;

public interface EventBus {

    /**
     * 发送消息
     *
     * @param topic   主题
     * @param message 消息
     * @param handler 异步处理器
     */
    void publish(String topic, Object message, Handler<AsyncResult<Void>> handler);

    /**
     * 订阅主题
     *
     * @param topic    主题
     * @param consumer 消费者
     * @param handler  异步处理器
     */
    void subscribe(String topic, Consumer<Object> consumer, Handler<AsyncResult<String>> handler);

    /**
     * 订阅主题
     *
     * @param topic    主题
     * @param consumer 消费者
     * @return promise
     */
    default Promise<String> subscribe(String topic, Consumer<Object> consumer) {
        return null;
    }

    /**
     * 取消订阅主题
     *
     * @param topic      主题
     * @param registryId 注册id
     * @param handler    异步处理器
     */
    void unSubscribe(String topic, String registryId, Handler<AsyncResult<Void>> handler);
}
