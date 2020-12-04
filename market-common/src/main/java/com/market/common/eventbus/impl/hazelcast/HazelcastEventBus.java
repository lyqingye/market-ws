package com.market.common.eventbus.impl.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.market.common.eventbus.EventBus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.Objects;
import java.util.function.Consumer;

public class HazelcastEventBus implements EventBus {

    /**
     * hazelcast 实例
     */
    private HazelcastInstance haze;

    public HazelcastEventBus(HazelcastInstance haze) {
        this.haze = Objects.requireNonNull(haze);
    }

    /**
     * 发送消息
     *
     * @param topic   主题
     * @param message 消息
     * @param handler 异步处理器
     */
    @Override
    public void publish(String topic, Object message, Handler<AsyncResult<Void>> handler) {
        haze.getTopic(topic)
                .publish(message);
        handler.handle(Future.succeededFuture());
    }

    /**
     * 订阅主题
     *
     * @param topic    主题
     * @param consumer 消费者
     * @param handler  异步处理器
     */
    @Override
    public void subscribe(String topic, Consumer<Object> consumer, Handler<AsyncResult<String>> handler) {
        String registryId = haze.getTopic(topic)
                .addMessageListener(message -> consumer.accept(message.getMessageObject()));
        handler.handle(Future.succeededFuture(registryId));
    }

    /**
     * 取消订阅主题
     *
     * @param topic      主题
     * @param registryId 注册id
     * @param handler    异步处理器
     */
    @Override
    public void unSubscribe(String topic, String registryId, Handler<AsyncResult<Void>> handler) {
        if (haze.getTopic(topic)
                .removeMessageListener(registryId)) {
            handler.handle(Future.succeededFuture());
        } else {
            handler.handle(Future.failedFuture("unSubscribe fail!"));
        }
    }
}
