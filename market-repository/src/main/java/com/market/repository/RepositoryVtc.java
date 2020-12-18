package com.market.repository;

import com.hazelcast.config.Config;
import com.market.common.def.ServiceAddress;
import com.market.common.def.Topics;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.service.config.ConfigService;
import com.market.common.service.repository.KlineRepositoryService;
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
     * redis 仓库
     */
    public RedisRepo redisRepo;

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
//        redisRepo = new RedisRepo(vertx, "redis://:AgdKA!YHgwOII4TA@119.23.49.169:6379/6");
        redisRepo = new RedisRepo(vertx, "redis://:@localhost:6379/6");
        redisRepo.onConnect((repo) -> {
            klineServiceBinder = new ServiceBinder(vertx).setAddress(ServiceAddress.KLINE_REPOSITORY.name());
            configServiceBinder = new ServiceBinder(vertx).setAddress(ServiceAddress.CONFIG.name());

            // K线交易数据仓库
            ConfigRepository configRepo = new ConfigRepository(redisRepo);
            new KlineRepository(redisRepo, configRepo, initAr -> {
                if (initAr.succeeded()) {
                    KlineRepository klineRepository = initAr.result();

                    // 监听交易数据
                    EventBusFactory.eventbus()
                                   .subscribe(Topics.KLINE_TICK_TOPIC.name(), klineRepository::forUpdateKline)
                                   .compose(ignored -> {
                                       System.out.println("[KlineRepository]: subscribe trade result topic success!");
                                       return Future.succeededFuture();
                                   })
                                   .compose(ignored -> EventBusFactory.eventbus().subscribe(Topics.MARKET_PRICE_TOPIC.name(), configRepo::forUpdatePrice))
                                   .compose(ignored -> {
                                       System.out.println("[KlineRepository]: subscribe price topic success!");
                                       return Future.succeededFuture();
                                   })
                                   .compose(ignored -> {
                                       // 部署K线仓库服务
                                       klineOpenApi = new KlineServiceImpl(klineRepository);
                                       return Future.succeededFuture();
                                   })
                                   .compose(ignored -> ConfigServiceImpl.create(vertx, configRepo))
                                   .onSuccess(rs -> {
                                       configOpenApi = rs;
                                       if (vertx.isClustered()) {
                                           configServiceConsumer = configServiceBinder.register(ConfigService.class, configOpenApi);
                                           klineServiceConsumer = klineServiceBinder.register(KlineRepositoryService.class, klineOpenApi);
                                       } else {
                                           configServiceConsumer = configServiceBinder.registerLocal(ConfigService.class, configOpenApi);
                                           klineServiceConsumer = klineServiceBinder.registerLocal(KlineRepositoryService.class, klineOpenApi);
                                       }
                                       startPromise.complete();
                                       System.out.println("[Market-Repository]: start success!");
                                   }).onFailure(Throwable::printStackTrace);
                } else {
                    initAr.cause().printStackTrace();
                }
            });
        });
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
