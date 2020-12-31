package com.market.common.eventbus.impl.disruptor;

import com.market.common.eventbus.EventBus;
import com.market.common.utils.disruptor.AbstractDisruptorConsumer;
import com.market.common.utils.disruptor.DisruptorQueue;
import com.market.common.utils.disruptor.DisruptorQueueFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class DisruptorEventBus implements EventBus {
    /**
     * disruptor
     */
    private DisruptorQueue<MessageWrapper> queue;

    /**
     * 主题与消费者映射
     */
    private ConcurrentHashMap<String, List<ConsumerWrapper>> topicConsumerMap;

    private ConcurrentHashMap<String, ExecutorService> topicExecutorMap;

    public DisruptorEventBus(int queueSize, ThreadFactory tf) {
        queue = DisruptorQueueFactory.createQueue(queueSize, tf, new AbstractDisruptorConsumer<MessageWrapper>() {
            @Override
            public void process(MessageWrapper event) {
                List<ConsumerWrapper> wrappers = topicConsumerMap.get(event.getTopic());
                if (wrappers != null && !wrappers.isEmpty()) {
                    for (ConsumerWrapper wrapper : wrappers) {
                        Consumer<Object> consumer = wrapper.getConsumer();
                        if (consumer != null) {
                            ExecutorService executorService = topicExecutorMap.get(event.getTopic());
                            if (executorService != null) {
                              executorService.submit(() -> {
                                  consumer.accept(event.getData());
                              });
                            }
                        }
                    }
                }
            }
        });
        topicConsumerMap = new ConcurrentHashMap<>(16);
        topicExecutorMap = new ConcurrentHashMap<>(16);
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
        if (topic == null || message == null) {
            handler.handle(Future.failedFuture("message is null"));
            return;
        }
        MessageWrapper wrapper = new MessageWrapper();
        wrapper.setData(message);
        wrapper.setTopic(topic);
        queue.add(wrapper);
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
        ConsumerWrapper consumerWrapper = new ConsumerWrapper();
        consumerWrapper.setRegistryId(UUID.randomUUID().toString());
        consumerWrapper.setConsumer(Objects.requireNonNull(consumer));
        topicConsumerMap.computeIfAbsent(topic, k -> new Vector<>())
                .add(consumerWrapper);
        topicExecutorMap.computeIfAbsent(topic, k -> Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("EventBus-Topic-" + topic);
                return thread;
            }
        }));
        handler.handle(Future.succeededFuture(consumerWrapper.getRegistryId()));
    }

    /**
     * 订阅主题
     *
     * @param topic    主题
     * @param consumer 消费者
     * @return promise
     */
    @Override
    public Future<String> subscribe(String topic, Consumer<Object> consumer) {
        Promise<String> promise = Promise.promise();
        ConsumerWrapper consumerWrapper = new ConsumerWrapper();
        consumerWrapper.setRegistryId(UUID.randomUUID().toString());
        consumerWrapper.setConsumer(Objects.requireNonNull(consumer));
        topicConsumerMap.computeIfAbsent(topic, k -> new Vector<>())
                .add(consumerWrapper);
        topicExecutorMap.computeIfAbsent(topic, k -> Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("EventBus-Topic-" + topic);
                return thread;
            }
        }));
        promise.complete(consumerWrapper.getRegistryId());
        return promise.future();
    }

    /**
     * 取消订阅主题
     *
     * @param topic      主题
     * @param registryId 注册ID
     * @param handler    异步处理器
     */
    @Override
    public void unSubscribe(String topic, String registryId, Handler<AsyncResult<Void>> handler) {
        List<ConsumerWrapper> wrappers = topicConsumerMap.get(topic);
        if (wrappers.removeIf((consumer) -> consumer.getRegistryId().equals(registryId))) {
            handler.handle(Future.succeededFuture());
        } else {
            handler.handle(Future.failedFuture("subscribe not found"));
        }
    }
}
