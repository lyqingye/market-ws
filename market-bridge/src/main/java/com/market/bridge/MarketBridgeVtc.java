package com.market.bridge;

import com.hazelcast.config.Config;
import com.market.bridge.impl.TcpMarketBridge;
import com.market.bridge.impl.http.HttpOpenApi;
import com.market.common.def.Topics;
import com.market.common.eventbus.EventBus;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.messages.bridge.Message;
import com.market.common.messages.bridge.PriceChangeMessage;
import com.market.common.messages.bridge.TradeMessage;
import com.market.common.messages.payload.depth.DepthTickResp;
import com.market.common.messages.payload.detail.TradeDetailResp;
import com.market.common.messages.payload.kline.KlineTradeResp;
import com.market.common.service.config.ConfigService;
import com.market.common.service.config.dto.Mapping;
import com.market.common.utils.VertxUtil;
import io.netty.util.internal.StringUtil;
import io.vertx.core.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarketBridgeVtc extends AbstractVerticle {

    /**
     * 桥接服务
     */
    private MarketBridge bridge;

    /**
     * http open api
     */
    private HttpOpenApi httpOpenApi;

    /**
     * 配置服务
     */
    private ConfigService configService;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // 配置服务
        configService = ConfigService.createProxy(vertx);
        this.bridge = this.createBridge();
        this.bridge.start(vertx, config(), ar -> {
            if (ar.succeeded()) {
                System.out.println("[MarketBridge-TcpBridge]: start success!");
                // 启动 Http API
                httpOpenApi = new HttpOpenApi();
                httpOpenApi.start(vertx, config(), api -> {
                    if (api.succeeded()) {
                        System.out.println("[MarketBridge-OpenApi]: start success!");
                        // 监听并处理价格变动事件
                        this.listenAndProcess()
                            .onSuccess(ignored -> {
                                System.out.println("[MarketBridge]: start success!");
                                startPromise.complete();
                            })
                            .onFailure(startPromise::fail);
                    } else {
                        startPromise.fail(api.cause());
                    }
                });
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

    /**
     * 创建桥接器
     *
     * @return 桥接器
     */
    private MarketBridge createBridge() {
        return new TcpMarketBridge() {
            @Override
            public void process(String source, AsyncResult<Message<?>> client) {
                if (client.succeeded()) {
                    Message<?> msg = client.result();
                    switch (msg.getType()) {
                        case DEPTH_CHART: {
                            processDepth(msg);
                            break;
                        }
                        case TRADE_RESULT: {
                            processTradeResult(source, msg);
                            break;
                        }
                        default: {
                        }
                    }
                } else {
                    client.cause().printStackTrace();
                }
            }
        };
    }

    /**
     * 处理深度数据
     *
     * @param msg msg
     */
    @SuppressWarnings("unchecked")
    private void processDepth(Message<?> msg) {
        VertxUtil.asyncFastCallIgnoreRs(vertx, () -> {
            // 盘口数据
            List<DepthTickResp> data = (List<DepthTickResp>) msg.getData();
            EventBusFactory.eventbus()
                    .publishIgnoreRs(Topics.DEPTH_CHART_TOPIC.name(), Json.encode(data));
        });
    }

    /**
     * 处理交易结果
     *
     * @param source 数据来源
     * @param msg    msg
     */
    private void processTradeResult(String source, Message<?> msg) {
        EventBus eb = EventBusFactory.eventbus();
        VertxUtil.asyncFastCallIgnoreRs(vertx, () -> {
            // 交易数据
            TradeMessage data = (TradeMessage) msg.getData();
            // 推送交易细节
            eb.publishIgnoreRs(Topics.TRADE_DETAIL_TOPIC.name(),Json.encode(new TradeDetailResp(data)));

            // 推送成交数据
            eb.publishIgnoreRs(Topics.KLINE_TICK_TOPIC.name(),Json.encode(new KlineTradeResp(data)));

            // 推送价格变动数据 (考虑分布式)
            eb.publishIgnoreRs(Topics.MARKET_PRICE_TOPIC.name(),Json.encode(new PriceChangeMessage(data, source)));
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        this.bridge.stop(stopPromise);
    }

    /**
     * 监听价格变动事件, 然后广播
     */
    private Future<Void> listenAndProcess() {
        Promise<Void> promise = Promise.promise();
        // 预先缓存交易对映射
        configService.g2cMappings(mpRs -> {
            if (mpRs.succeeded()) {
                Map<String, String> g2cCache = new ConcurrentHashMap<>(Mapping.toMap(mpRs.result()));
                // 监听价格变动信息并且推向所有撮合引擎（排除来源）
                EventBusFactory.eventbus()
                        .subscribe(Topics.MARKET_PRICE_TOPIC.name(), msg -> {
                            String jsonMsg = String.valueOf(msg);
                            PriceChangeMessage pc = Json.decodeValue(jsonMsg, PriceChangeMessage.class);
                            // 交易对转换
                            String custom = g2cCache.get(pc.getSymbol());
                            if (custom != null) {
                                // 广播价格变动消息, 并且忽略指定服务器ip
                                pc.setSymbol(custom);
                                this.bridge.broadcastWithExcludeSocketAddr(PriceChangeMessage.toBuf(pc), pc.getSource());
                            } else {
                                // 缓存中存在则直接去配置服务拿
                                configService.g2c(pc.getSymbol(), to -> {
                                    if (to.succeeded()) {
                                        String result = to.result();
                                        if (StringUtil.isNullOrEmpty(result)) {
                                            System.err.println("[MarketBridge]: 找不到交易对 " + pc.getSymbol() + " 的g2c映射！");
                                        } else {
                                            g2cCache.put(pc.getSymbol(), result);
                                            pc.setSymbol(result);
                                            this.bridge.broadcastWithExcludeSocketAddr(PriceChangeMessage.toBuf(pc), pc.getSource());
                                        }
                                    } else {
                                        to.cause().printStackTrace();
                                    }
                                });
                            }
                        }, ar -> {
                            if (ar.succeeded()) {
                                System.out.println("[MarketBridge]: subscribe market price topic success!");
                                promise.complete();
                            } else {
                                promise.fail(ar.cause());
                            }
                        });
            } else {
                mpRs.cause().printStackTrace();
            }
        });
        return promise.future();
    }

    public static void main(String[] args) {
        Config hazelcastConfig = new Config();
        HazelcastClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        options.setClusterManager(mgr);
        Vertx.clusteredVertx(options, ar -> {
            if (ar.succeeded()) {
                EventBusFactory.createDistributeEventBus(mgr.getHazelcastInstance());
                ar.result().deployVerticle(new MarketBridgeVtc());
            }else {
                ar.cause().printStackTrace();
            }
        });
    }
}
