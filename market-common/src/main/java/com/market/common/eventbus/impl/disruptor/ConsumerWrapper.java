package com.market.common.eventbus.impl.disruptor;

import lombok.Data;

import java.util.function.Consumer;

@Data
public class ConsumerWrapper {
    /**
     * 注册id
     */
    private String registryId;

    /**
     * 消费者
     */
    private Consumer<Object> consumer;
}
