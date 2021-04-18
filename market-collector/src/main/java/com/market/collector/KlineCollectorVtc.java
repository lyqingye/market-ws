package com.market.collector;

import com.hazelcast.config.Config;
import com.market.common.def.ServiceAddress;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.collector.KlineCollectorService;
import com.market.common.service.collector.dto.CollectorStatusDto;
import com.market.common.utils.VertxUtil;
import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.Arrays;
import java.util.List;

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
    private KlineCollectorServiceImpl openService;

    public KlineCollectorVtc() {
    }

    @Override
    public void start(Promise<Void> promise) throws Exception {
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
        JsonObject config = config();
        String collectorName = VertxUtil.jsonGetValue(config, "market.collector.name", String.class);
        List<String> subscribe = VertxUtil.jsonListValue(config, "market.collector.subscribe", String.class);
        if (collectorName != null && !collectorName.isEmpty()) {
            Future<Boolean> future = openService.deployCollector(collectorName)
                                                 .compose(ignored -> openService.startCollector(collectorName));
            for (String subscribeSymbol : subscribe) {
                future = future.compose(ignored -> openService.subscribe(collectorName,subscribeSymbol));
            }
            future.onFailure(promise::fail);
            future.onSuccess(ignored -> {
                System.out.println("[Market-KlineCollector]: start success!");
                System.out.println("[Market-KlineCollector]: deploy collector: " + collectorName);
                System.out.println("[Market-KlineCollector]: subscribe: " + subscribe);
                promise.complete();
            });
        }
    }

    @Override
    public void stop() throws Exception {
        serviceBinder.unregister(serviceConsumer);
        // 停止所有收集器
        if (openService != null) {
            openService.listCollector(cr -> {
                if (cr.succeeded()) {
                    for (CollectorStatusDto collector : cr.result()) {
                        openService.stopCollector(collector.getName(), stopRs -> {
                            if (stopRs.failed()) {
                                stopRs.cause().printStackTrace();
                            }
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
