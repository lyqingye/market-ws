package com.market.common.eventbus.impl;

import com.hazelcast.core.HazelcastInstance;
import com.market.common.eventbus.EventBus;
import com.market.common.eventbus.impl.disruptor.DisruptorEventBus;
import com.market.common.eventbus.impl.hazelcast.HazelcastEventBus;

public final class EventBusFactory {

    private static volatile EventBus INSTANCE;

    public static EventBus eventbus() {
        return INSTANCE;
    }

    /**
     * 本地消息总线
     *
     * @param queueSize 队列大小
     */
    public static void createLocalEventBus(int queueSize) {
        INSTANCE = new DisruptorEventBus(queueSize, r -> {
            Thread tr = new Thread(r);
            tr.setName("disruptor-event-bus");
            return tr;
        });
    }

    /**
     * 分布式消息总线
     *
     * @param hazelcastInstance hazelcast
     */
    public static void createDistributeEventBus(HazelcastInstance hazelcastInstance) {
        INSTANCE = new HazelcastEventBus(hazelcastInstance);
    }
}
