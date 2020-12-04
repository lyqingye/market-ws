package com.market.standalone;

import com.market.bridge.MarketBridgeVtc;
import com.market.collector.KlineCollectorVtc;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.collector.KlineCollectorService;
import com.market.publish.MarketPublishVtc;
import com.market.repository.RepositoryVtc;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

/**
 * 单体服务
 */
public class StandaloneEndpoint {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        // 创建本地事件总线
        EventBusFactory.createLocalEventBus(1 << 16);
        vertx.exceptionHandler(Throwable::printStackTrace);

        // 部署仓库服务 (worker)
        DeploymentOptions workerOption = new DeploymentOptions().setWorker(true);
        vertx.deployVerticle(new RepositoryVtc(), workerOption, repoRs -> {
            vertx.deployVerticle(new MarketPublishVtc("0.0.0.0", 8089), pubRs -> {
                vertx.deployVerticle(new KlineCollectorVtc(), ctRs -> {
                    vertx.deployVerticle(new MarketBridgeVtc(), mbRs -> {
                        if (mbRs.succeeded()) {
                            KlineCollectorService collectorService = KlineCollectorService.createProxy(vertx);
                            collectorService.deployCollector("com.market.collector.impl.HuoBiKlineCollector", dpRs -> {
                                if (dpRs.succeeded()) {
                                    collectorService.startCollector("com.market.collector.impl.HuoBiKlineCollector", sctRs -> {
                                        if (sctRs.succeeded()) {
                                            collectorService.subscribe("com.market.collector.impl.HuoBiKlineCollector", "btcusdt", subRs -> {
                                                if (subRs.succeeded()) {

                                                } else {
                                                    subRs.cause().printStackTrace();
                                                }
                                            });
                                        } else {
                                            sctRs.cause().printStackTrace();
                                        }
                                    });
                                } else {
                                    dpRs.cause().printStackTrace();
                                }
                            });
                        }else {
                            mbRs.cause().printStackTrace();
                        }
                    });

                });
            });
        });
    }
}
