package com.market.standalone;

import com.market.bridge.MarketBridgeVtc;
import com.market.collector.KlineCollectorVtc;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.collector.KlineCollectorService;
import com.market.common.utils.VertxUtil;
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

        VertxUtil.deploy(vertx,new RepositoryVtc(),new DeploymentOptions().setWorker(true))
                 .compose(ignored -> VertxUtil.deploy(vertx, new MarketPublishVtc("0.0.0.0", 8089)))
                 .compose(ignored -> VertxUtil.deploy(vertx, new KlineCollectorVtc()))
                 .compose(ignored -> VertxUtil.deploy(vertx, new MarketBridgeVtc()))
                 .onFailure(Throwable::printStackTrace);
    }
}
