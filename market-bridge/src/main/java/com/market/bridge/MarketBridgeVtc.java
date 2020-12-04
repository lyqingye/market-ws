package com.market.bridge;

import com.hazelcast.config.Config;
import com.market.bridge.impl.TcpMarketBridge;
import com.market.bridge.impl.http.HttpManagerBridge;
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
import io.netty.util.internal.StringUtil;
import io.vertx.core.*;
import io.vertx.core.json.Json;
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
     * 配置服务
     */
    private ConfigService configService;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // 配置服务
        configService = ConfigService.createProxy(vertx);
        this.bridge = new TcpMarketBridge() {
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
        this.bridge.start(vertx, null, ar -> {
            if (ar.succeeded()) {
                System.out.println("[MarketBridge]: start success!");

                // 启动 Http API
                new HttpManagerBridge().start(vertx, null, manager -> {
                    if (manager.succeeded()) {
                        System.out.println("[MarketManagerHttpAPI]: start success!");
                        // 监听并处理价格变动事件
                        this.listenAndProcess();
                        startPromise.complete();
                    } else {
                        manager.cause().printStackTrace();
                    }
                });
            } else {
                ar.cause().printStackTrace();
            }
        });
    }

    /**
     * 处理深度数据
     *
     * @param msg msg
     */
    @SuppressWarnings("unchecked")
    private void processDepth(Message<?> msg) {
        vertx.executeBlocking(promise -> {
            // 盘口数据
            List<DepthTickResp> data = (List<DepthTickResp>) msg.getData();
            EventBusFactory.eventbus()
                    .publish(Topics.DEPTH_CHART_TOPIC.name(),
                            Json.encode(data), ignored -> {
                            });
        }, ignored -> {
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
        vertx.executeBlocking(promise -> {
            // 交易数据
            TradeMessage data = (TradeMessage) msg.getData();
            // 推送交易细节
            eb.publish(Topics.TRADE_DETAIL_TOPIC.name(),
                    Json.encode(new TradeDetailResp(data)), ignored -> {
                    });

            // 推送成交数据
            eb.publish(Topics.KLINE_TICK_TOPIC.name(),
                    Json.encode(new KlineTradeResp(data)), ignored -> {
                    });

            // 推送价格变动数据 (考虑分布式)
            eb.publish(Topics.MARKET_PRICE_TOPIC.name(),
                    Json.encode(new PriceChangeMessage(data, source)), ignored -> {
                    });

        }, ignored -> {
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        this.bridge.stop(ignored -> {
        });
    }

    /**
     * 监听价格变动事件, 然后广播
     */
    private void listenAndProcess() {
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
                            } else {
                                ar.cause().printStackTrace();
                            }
                        });
            } else {
                mpRs.cause().printStackTrace();
            }
        });

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
