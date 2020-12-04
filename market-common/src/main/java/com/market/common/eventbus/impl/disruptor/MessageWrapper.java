package com.market.common.eventbus.impl.disruptor;

import lombok.Data;

/**
 * 消息包装
 */
@Data
public class MessageWrapper {
    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息数据
     */
    private Object data;
}
