package com.market.common.eventbus.impl.kafka;

import com.market.common.eventbus.EventBus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class KafkaEventBus implements EventBus {

    private Map<String,KafkaConsumer<String, Object>> consumerMap = new ConcurrentHashMap<>();
    private KafkaProducer<String, Object> producer;
    private Vertx vertx;
    private Map<String,String> kafkaConfig;

    public KafkaEventBus (Vertx vertx,Map<String, String> kafkaConfig) {
        producer = KafkaProducer.create(vertx, kafkaConfig);
        this.vertx = vertx;
        this.kafkaConfig = kafkaConfig;
    }

    @Override
    public void publish(String topic, Object message, Handler<AsyncResult<Void>> handler) {
        KafkaProducerRecord<String, Object> record = KafkaProducerRecord.create(topic, message);
        producer.write(record,handler);
    }

    @Override
    public void subscribe(String topic, Consumer<Object> consumer, Handler<AsyncResult<String>> handler) {
        KafkaConsumer<String, Object> c = KafkaConsumer.create(vertx, kafkaConfig);
        String id = UUID.randomUUID().toString();
        c.subscribe(topic,rs -> {
            if (rs.succeeded()) {
                handler.handle(Future.succeededFuture(id));
                consumerMap.put(id,c);
            }else {
                handler.handle(Future.failedFuture(rs.cause()));
            }
        }).handler(record -> {
            if (consumer != null) {
                record.value();
            }
        }).exceptionHandler(Throwable::printStackTrace);
    }

    @Override
    public void unSubscribe(String topic, String registryId, Handler<AsyncResult<Void>> handler) {
        KafkaConsumer<String, Object> consumer = consumerMap.get(registryId);
        if (consumer == null) {
            handler.handle(Future.failedFuture("consumer not found"));
        }else {
            consumer.close(handler);
        }
    }
}
