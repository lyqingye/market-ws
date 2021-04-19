package com.market.publish;

import com.hazelcast.config.Config;
import com.market.common.def.Topics;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.messages.payload.kline.KlinePingResp;
import com.market.common.session.SessionManager;
import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.VertxUtil;
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

import static com.market.common.utils.GZIPUtils.compressAsync;

/**
 * @author ex
 */
public class MarketPublishVtc extends AbstractVerticle {

  /**
   * websocket 服务
   */
  private HttpServer ws;

  /**
   * 推送上下文
   */
  private PublishContext ctx;

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

    JsonObject config = config();
    String host = "localhost";
    int port = 8089;
    boolean compressPing = false;
    if (config != null) {
      host = VertxUtil.jsonGetValue(config, "market.publish.websockets.host", String.class, host);
      port = VertxUtil.jsonGetValue(config, "market.publish.websockets.port", Integer.class, port);
      compressPing = VertxUtil.jsonGetValue(config, "market.publish.compress.ping", Boolean.class, false);
    }

    System.out.println("[Market-Publish]: listen host: " + host);
    System.out.println("[Market-Publish]: listen port: " + port);

    // 创建推送上下文
    ctx = new PublishContext(vertx);

    // 会话管理器 (支持ttl)
    final SessionManager<WsSessionWrapper> sm = ctx.getSm();
    vertx.createHttpServer().webSocketHandler(ws -> {
      // 更新ttl
      WsSessionWrapper wrapper = sm.get(ws.textHandlerID(), k -> WsSessionWrapper.of(ws));
      ws.frameHandler(frame -> {
        if (frame.isText() && frame.isFinal()) {
          Object obj = Json.decodeValue(frame.textData());
          if (obj instanceof JsonObject) {
            final Cmd cmd = CmdFactory.createForWs((JsonObject) obj);
            if (cmd != null) {
              VertxUtil.asyncFastCallIgnoreRs(vertx, () -> {
                cmd.execute((JsonObject) obj, ctx, wrapper);
              });
            }
          } else if (obj instanceof JsonArray) {
            final Cmd cmd = CmdFactory.createForWs((JsonArray) obj);
            if (cmd != null) {
              VertxUtil.asyncFastCallIgnoreRs(vertx, () -> {
                cmd.execute((JsonArray) obj, ctx, wrapper);
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
    }).listen(port, host)
            .onSuccess( rs -> {
              ws = rs;
            })
            .onFailure(Throwable::printStackTrace);

    // 定时发送心跳
    boolean finalCompressPing = compressPing;
    vertx.setPeriodic(10000, timer -> {
      KlinePingResp pingMsg = KlinePingResp.ping();
      if (finalCompressPing) {
        compressAsync(vertx, Json.encodeToBuffer(pingMsg))
            .onSuccess(ping -> {
              VertxUtil.asyncFastCallIgnoreRs(vertx, () -> {
                // 发送给所有会话
                sm.asMap().values().forEach(session -> {
                  session.getSocket().writeBinaryMessage(ping);
                });
              });
            })
            .onFailure(Throwable::printStackTrace);
      } else {
        VertxUtil.asyncFastCallIgnoreRs(vertx, () -> {
          sm.asMap().values().forEach(session -> {
            session.getSocket().writeTextMessage(Json.encode(pingMsg));
          });
        });
      }
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
      Object obj = Json.decodeValue(String.valueOf(o));

      if (obj instanceof JsonObject) {
        Cmd cmd = CmdFactory.createForTopic((JsonObject) obj);
        if (cmd != null) {
          cmd.execute((JsonObject) obj, ctx, null);
        }
      } else if (obj instanceof JsonArray) {
        Cmd cmd = CmdFactory.createForTopic((JsonArray) obj);
        if (cmd != null) {
          cmd.execute((JsonArray) obj, ctx, null);
        }
      }
    };

    // 订阅k线数据
    EventBusFactory.eventbus()
                   .subscribe(Topics.KLINE_TICK_TOPIC.name(), consumer, subRs -> {
                     if (subRs.failed()) {
                       subRs.cause().printStackTrace();
                     }
                   });

    // 订阅深度数据
    EventBusFactory.eventbus()
                   .subscribe(Topics.DEPTH_CHART_TOPIC.name(), consumer, subRs -> {
                     if (subRs.failed()) {
                       subRs.cause().printStackTrace();
                     }
                   });

    // 订阅成交细节数据
    EventBusFactory.eventbus()
                   .subscribe(Topics.TRADE_DETAIL_TOPIC.name(), consumer, subRs -> {
                     if (subRs.failed()) {
                       subRs.cause().printStackTrace();
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
        ar.result().deployVerticle(new MarketPublishVtc());
      } else {
        ar.cause().printStackTrace();
      }
    });
  }
}
