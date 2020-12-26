package com.market.repository;

import com.hazelcast.config.Config;
import com.market.common.def.ServiceAddress;
import com.market.common.def.Topics;
import com.market.common.eventbus.EventBus;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.config.ConfigService;
import com.market.common.service.repository.KlineRepositoryService;
import com.market.common.utils.VertxUtil;
import com.market.repository.impl.ConfigServiceImpl;
import com.market.repository.impl.KlineServiceImpl;
import com.market.repository.repository.ConfigRepository;
import com.market.repository.repository.KlineRepository;
import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class RepositoryVtc extends AbstractVerticle {

  /**
   * redis仓库
   */
  private RedisRepo redisRepo;

  /**
   * 配置仓库
   */
  private ConfigRepository configRepo;

  /**
   * k线仓库开放api
   */
  private KlineRepositoryService klineOpenApi;

  /**
   * 配置开放api
   */
  private ConfigService configOpenApi;

  /**
   * expose service
   */
  private ServiceBinder klineServiceBinder, configServiceBinder;

  /**
   * consumer
   */
  private MessageConsumer<JsonObject> klineServiceConsumer, configServiceConsumer;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    EventBus eb = EventBusFactory.eventbus();
    JsonObject config = config();
    String redisConnString = "redis://:@localhost:6378/6";
    if (config != null) {
      redisConnString = VertxUtil.jsonGetValue(config, "market.repository.redis.connectionString", String.class, redisConnString);
    }
    System.out.println("[Market-Repository]: using redis with url: " + redisConnString);
    // 创建redis仓库
    RedisRepo.create(vertx, redisConnString)
             .compose(repo -> {
               redisRepo = repo;
               // 创建配置仓库
               configRepo = new ConfigRepository(redisRepo);
               // K线交易数据仓库
               return KlineRepository.create(redisRepo, configRepo);
             })
             .compose(klineRepository -> {
               return eb
                   // 监听交易数据
                   .subscribe(Topics.KLINE_TICK_TOPIC.name(), klineRepository::forUpdateKline)
                   .compose(ignored -> {
                     System.out.println("[KlineRepository]: subscribe trade result topic success!");
                     return Future.succeededFuture();
                   })

                   // 监听价格数据
                   .compose(ignored -> eb.subscribe(Topics.MARKET_PRICE_TOPIC.name(), configRepo::forUpdatePrice))
                   .compose(ignored -> {
                     System.out.println("[KlineRepository]: subscribe price topic success!");
                     return Future.succeededFuture();
                   })

                   // 部署K线仓库服务
                   .compose(ignored -> {
                     klineOpenApi = new KlineServiceImpl(klineRepository);
                     return Future.succeededFuture();
                   })

                   // 部署配置服务
                   .compose(ignored -> ConfigServiceImpl.create(vertx, configRepo))
                   .onSuccess(configService -> {

                     //
                     // 启动服务监听
                     //
                     configOpenApi = configService;
                     klineServiceBinder = new ServiceBinder(vertx).setAddress(ServiceAddress.KLINE_REPOSITORY.name());
                     configServiceBinder = new ServiceBinder(vertx).setAddress(ServiceAddress.CONFIG.name());
                     if (vertx.isClustered()) {
                       configServiceConsumer = configServiceBinder.register(ConfigService.class, configOpenApi);
                       klineServiceConsumer = klineServiceBinder.register(KlineRepositoryService.class, klineOpenApi);
                     } else {
                       configServiceConsumer = configServiceBinder.registerLocal(ConfigService.class, configOpenApi);
                       klineServiceConsumer = klineServiceBinder.registerLocal(KlineRepositoryService.class, klineOpenApi);
                     }
                   });
             })
             .onSuccess(success -> {
               startPromise.complete();
               System.out.println("[Market-Repository]: start success!");
             })
             .onFailure(Throwable::printStackTrace);
  }

  @Override
  public void stop() throws Exception {
    if (redisRepo != null) {
      redisRepo.close();
    }
    if (klineServiceBinder != null) {
      klineServiceBinder.unregister(klineServiceConsumer);
    }
    if (configServiceBinder != null) {
      configServiceBinder.unregister(configServiceConsumer);
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
        ar.result().deployVerticle(new RepositoryVtc(), new DeploymentOptions().setWorker(true));
      } else {
        ar.cause().printStackTrace();
      }
    });
  }
}
