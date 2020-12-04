package com.market.publish;

import com.hazelcast.config.Config;
import com.market.common.def.Topics;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.messages.payload.kline.KlinePingResp;
import com.market.common.session.SessionManager;
import com.market.common.session.WsSessionWrapper;
import com.market.publish.cmd.Cmd;
import com.market.publish.cmd.CmdFactory;
import com.market.publish.context.PublishContext;
import com.market.publish.worker.KlineWorker;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.function.Consumer;

/**
 * @author ex
 */
public class MarketPublishVtc extends AbstractVerticle {

    /**
     * websocket host
     */
    private String host;

    /**
     * websocket port
     */
    private int port;

    /**
     * websocket 服务
     */
    private HttpServer ws;

    /**
     * 推送上下文
     */
    private PublishContext ctx;

    public MarketPublishVtc(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        this.createHttpServer(ar -> {
            if (ar.succeeded()) {
                this.ws = ar.result();
                startPromise.complete();
                System.out.println("[Market-Publish]: start success!");
            } else {
                ar.cause().printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (ws != null) {
            ws.close();
        }
    }

    /**
     * 创建消息推送服务
     *
     * @param handler 结果处理器
     */
    private void createHttpServer(Handler<AsyncResult<HttpServer>> handler) {
        // 创建推送上下文
        ctx = new PublishContext(vertx);

        // 会话管理器 (支持ttl)
        final SessionManager<WsSessionWrapper> sm = ctx.getSm();
        ws = vertx.createHttpServer().webSocketHandler(ws -> {
            // 更新ttl
            WsSessionWrapper wrapper = sm.get(ws.textHandlerID(), k -> WsSessionWrapper.of(ws));
            ws.frameHandler(frame -> {
                if (frame.isText() && frame.isFinal()) {
                    Object obj =  Json.decodeValue(frame.textData());
                    if (obj instanceof JsonObject) {
                        final Cmd cmd = CmdFactory.createForWs((JsonObject) obj);
                        if (cmd != null) {
                            vertx.executeBlocking(promise -> cmd.execute((JsonObject) obj, ctx, wrapper), ignored -> {
                            });
                        }
                    }else if (obj instanceof JsonArray) {
                        final Cmd cmd = CmdFactory.createForWs((JsonArray) obj);
                        if (cmd != null) {
                            vertx.executeBlocking(promise -> cmd.execute((JsonArray) obj, ctx, wrapper), ignored -> {
                            });
                        }
                    }

                }
            });

            ws.closeHandler((ignored) -> {
                sm.invalidate(ws.textHandlerID());
                ctx.getDepthSM().remove(ws.textHandlerID());
                ctx.getKlineSM().remove(ws.textHandlerID());
                ctx.getDetailSM().remove(ws.textHandlerID());
            });
        }).listen(port, host);

        // 定时发送心跳
        vertx.setPeriodic(10000, timer -> {
            vertx.executeBlocking(prom -> {
                final String pingMsg = Json.encode(KlinePingResp.ping());
                // 发送给所有会话
                sm.asMap().values().forEach(session -> {
                    session.getSocket().writeTextMessage(pingMsg);
                });
                prom.complete();
            }, ignored -> {
            });
        });

        // 部署K线服务 (k线服务需要计算以及持久化所以作为worker)
        vertx.deployVerticle(new KlineWorker(ctx), new DeploymentOptions().setWorker(true), ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });

        // 监听数据并且推送
        this.listenTopicAndProcess();
    }


    /**
     * 监听数据主题并且处理
     */
    private void listenTopicAndProcess() {
        Consumer<Object> consumer = o -> {
            Object obj =  Json.decodeValue(String.valueOf(o));

            if (obj instanceof JsonObject) {
                Cmd cmd = CmdFactory.createForTopic((JsonObject) obj);
                if (cmd != null) {
                    cmd.execute((JsonObject) obj, ctx, null);
                }
            }else if (obj instanceof JsonArray) {
                Cmd cmd = CmdFactory.createForTopic((JsonArray) obj);
                if (cmd != null) {
                    cmd.execute((JsonArray) obj, ctx, null);
                }
            }
        };

        // 订阅k线数据
        EventBusFactory.eventbus()
                .subscribe(Topics.KLINE_TICK_TOPIC.name(), consumer, ignored -> {
                });

        // 订阅深度数据
        EventBusFactory.eventbus()
                .subscribe(Topics.DEPTH_CHART_TOPIC.name(), consumer, ignored -> {
                });

        // 订阅成交细节数据
        EventBusFactory.eventbus()
                .subscribe(Topics.TRADE_DETAIL_TOPIC.name(), consumer, ignored -> {
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
                ar.result().deployVerticle(new MarketPublishVtc("0.0.0.0",8087));
            }else {
                ar.cause().printStackTrace();
            }
        });
    }
}
