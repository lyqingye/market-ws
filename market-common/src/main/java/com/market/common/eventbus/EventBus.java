package com.market.common.eventbus;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
     * 发送消息并且忽略返回值
     *
     * @param topic topic
     * @param message 消息
     */
    default void publishIgnoreRs(String topic,Object message) {
        publish(topic,message,rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            }
        });
    }

    /**
     * 发送消息
     *
     * @param topic   主题
     * @param message 消息
     * @return  future
     */
    default Future<Void> publish(String topic, Object message) {
        Promise<Void> promise = Promise.promise();
        publish(topic,message,promise);
        return promise.future();
    }

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
    default Future<String> subscribe(String topic, Consumer<Object> consumer) {
        Promise<String> promise = Promise.promise();
        subscribe(topic, consumer,promise);
        return promise.future();
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
