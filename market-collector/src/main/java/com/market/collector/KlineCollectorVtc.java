package com.market.collector;

import com.hazelcast.config.Config;
import com.market.common.def.ServiceAddress;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.collector.KlineCollectorService;
import com.market.common.service.collector.dto.CollectorStatusDto;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * @author yjt
 * @since 2020/10/10 下午3:45
 */
public class KlineCollectorVtc extends AbstractVerticle {

    /**
     * expose service
     */
    private ServiceBinder serviceBinder;

    /**
     * consumer
     */
    private MessageConsumer<JsonObject> serviceConsumer;

    /**
     * 开放服务
     */
    private KlineCollectorService openService;

    public KlineCollectorVtc() {
    }

    @Override
    public void start() throws Exception {
        // 暴露服务
        serviceBinder = new ServiceBinder(vertx).setAddress(ServiceAddress.COLLECTOR.name());
        openService = new KlineCollectorServiceImpl(vertx, EventBusFactory.eventbus());

        if (vertx.isClustered()) {
            serviceConsumer = serviceBinder
                    .register(KlineCollectorService.class, openService);
        } else {
            serviceConsumer = serviceBinder
                    .registerLocal(KlineCollectorService.class, openService);
        }

        System.out.println("[Market-KlineCollector]: start success!");
    }

    @Override
    public void stop() throws Exception {
        serviceBinder.unregister(serviceConsumer);
        // 停止所有收集器
        if (openService != null) {
            openService.listCollector(cr -> {
                if (cr.succeeded()) {
                    for (CollectorStatusDto collector : cr.result()) {
                        openService.stopCollector(collector.getName(), ignored -> {
                        });
                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        Config hazelcastConfig = new Config();
        HazelcastClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        options.setClusterManager(mgr);
        Vertx.clusteredVertx(options, ar -> {
            if (ar.succeeded()) {
                EventBusFactory.createDistributeEventBus(mgr.getHazelcastInstance());
                ar.result().deployVerticle(new KlineCollectorVtc());
            }else {
                ar.cause().printStackTrace();
            }
        });
    }
}
