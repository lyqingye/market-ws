package com.market.standalone;

import com.market.bridge.MarketBridgeVtc;
import com.market.collector.KlineCollectorVtc;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.collector.KlineCollectorService;
import com.market.common.utils.TimeUtils;
import com.market.common.utils.VertxUtil;
import com.market.publish.MarketPublishVtc;
import com.market.repository.RepositoryVtc;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 单体服务
 */
public class StandaloneEndpoint {
  private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
      long start = System.currentTimeMillis();
      JsonObject config = loadConfig();
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

    @SneakyThrows
    private static JsonObject loadConfig() {
      ConfigStoreOptions fileStore = new ConfigStoreOptions()
          .setType("file")
          .setFormat("yaml")
          .setConfig(new JsonObject().put("path", "config.yaml"));

      ConfigRetrieverOptions options = new ConfigRetrieverOptions()
          .addStore(fileStore);
      ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
      CompletableFuture<JsonObject> cf = new CompletableFuture<>();
      retriever.getConfig(ar -> {
        if (ar.succeeded()) {
          cf.complete(ar.result());
        }else {
          cf.completeExceptionally(ar.cause());
        }
      });
      return cf.get();
    }
}
