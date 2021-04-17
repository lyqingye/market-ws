package com.market.standalone;

import com.market.bridge.MarketBridgeVtc;
import com.market.collector.KlineCollectorVtc;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.collector.KlineCollectorService;
import com.market.common.utils.TimeUtils;
import com.market.common.utils.VertxUtil;
import com.market.publish.MarketPublishVtc;
import com.market.repository.RepositoryVtc;
import io.netty.channel.Channel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;
import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 单体服务
 */
public class StandaloneEndpoint {
  private static final Vertx vertx;

  static {
    VertxOptions options = new VertxOptions();
    vertx = Vertx.vertx(options);
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
  }

    public static void main(String[] args) {
      long start = System.currentTimeMillis();
      JsonObject config = VertxUtil.readYamlConfig(vertx,"config.yaml");
      // 创建本地事件总线
        EventBusFactory.createLocalEventBus(1 << 16);
        vertx.exceptionHandler(Throwable::printStackTrace);
        VertxUtil.deploy(vertx,new RepositoryVtc(),new DeploymentOptions().setWorker(true).setConfig(config))
                 .compose(ignored -> VertxUtil.deploy(vertx, new MarketPublishVtc(),config))
                 .compose(ignored -> VertxUtil.deploy(vertx, new KlineCollectorVtc(),config))
                 .compose(ignored -> VertxUtil.deploy(vertx, new MarketBridgeVtc(), config))
                 .onSuccess(ignored -> {

                   System.out.println("[StandaloneEndpoint]: start success, using " +
                                      (System.currentTimeMillis() - start) + " ms");
                 })
                 .onFailure(fail -> {
                   fail.printStackTrace();
                   System.exit(-1);
                 });
    }
}
