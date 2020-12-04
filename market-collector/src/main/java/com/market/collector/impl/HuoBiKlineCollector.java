package com.market.collector.impl;


import com.market.common.def.Period;
import com.market.common.utils.GZIPUtils;
import com.market.common.utils.RequestUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author ex
 */
public class HuoBiKlineCollector extends GenericWsCollector {

    /**
     * vertx 实例
     */
    private Vertx vertx;

    /**
     * 数据消费者
     */
    private Consumer<JsonObject> consumer;

    /**
     * 额外参数
     */
    private Config config;

    /**
     * websocket实例
     */
    private WebSocket ws;

    /**
     * http 客户端
     */
    private HttpClient hc;

    /**
     * 订阅ID
     */
    private String subIdPrefix = UUID.randomUUID().toString();

    /**
     * 部署一个收集器
     *
     * @param vertx    vertx 实例
     * @param consumer 数据消费器
     * @param args     附加参数 (可以为空)
     * @return 是否部署成功
     * @throws Exception 如果部署失败
     */
    @Override
    public boolean deploy(Vertx vertx,
                          Consumer<JsonObject> consumer,
                          JsonObject args) {
        boolean result = super.deploy(vertx, consumer, args);

        if (!result) {
            return false;
        }

        if (args != null) {
            this.config = args.mapTo(Config.class);
        }

        if (this.config == null ||
                this.config.getHost() == null ||
                this.config.reqUrl == null) {
            this.config = new Config();
        }

        this.vertx = vertx;
        this.consumer = consumer;
        return true;
    }

    /**
     * 开启收集数据
     *
     * @param handler 回调
     */
    @Override
    public void start(Handler<AsyncResult<Boolean>> handler) {

        super.start(ar -> {
            if (ar.succeeded()) {
                this.hc = this.vertx.createHttpClient();
                HuoBiKlineCollector that = this;
                // 创建websocket 链接
                hc.webSocket(this.config.host, this.config.reqUrl, wsAr -> {
                    if (wsAr.succeeded()) {
                        that.ws = wsAr.result();
                        this.registerMsgHandler(that.ws);
                        // 重新订阅
                        for (String symbol : super.listSubscribedSymbol()) {
                            this.subscribe(symbol);
                        }
                        handler.handle(Future.succeededFuture(true));
                    } else {
                        handler.handle(Future.failedFuture(wsAr.cause()));
                    }
                });
            } else {
                handler.handle(ar);
            }
        });

    }

    /**
     * 取消部署收集器
     *
     * @param args 附加参数可以为空
     * @return 如果取消部署失败
     * @throws Exception 如果取消部署失败
     */
    @Override
    public boolean unDeploy(JsonObject args) {
        boolean result = super.unDeploy(args);
        if (this.hc != null) {
            try {
                this.hc.close();
                this.hc = null;
                return result;
            } catch (Exception e) {
                result = false;
            }
        }
        return result;
    }

    /**
     * 停止数据收集
     *
     * @return 是否停止成功
     */
    @Override
    public boolean stop() {
        boolean result = super.stop();
        if (this.hc != null) {
            try {
                this.hc.close();
                this.hc = null;
                return result;
            } catch (Exception e) {
                result = false;
            }
        }
        return result;
    }

    /**
     * 获取websocket实例
     *
     * @return 实例
     */
    @Override
    public WebSocket ws() {
        return this.ws;
    }

    /**
     * 订阅一个交易对
     *
     * @param symbol 交易对
     * @return 是否订阅成功
     */
    @Override
    public boolean subscribe(String symbol) {
        boolean result = super.subscribe(symbol);
        if (!result) {
            return false;
        }

        if (this.ws != null && !this.ws.isClosed()) {
            JsonObject obj = new JsonObject();

            //
            // 这里只订阅 1min的交易, 其它的都由 1min 来进行计算得到
            //
            obj.put("sub", RequestUtils.toKlineSub(toGenericSymbol(symbol), Period._1_MIN));
            obj.put("id", subIdPrefix + symbol);
            try {
                this.ws.writeTextMessage(obj.toString());
                result = true;
            } catch (Exception ex) {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * 取消订阅一个交易对
     *
     * @param symbol 交易对
     * @return 是否取消订阅成功
     */
    @Override
    public boolean unSubscribe(String symbol) {
        boolean result = super.unSubscribe(symbol);

        if (!result) {
            return false;
        }
        if (this.ws != null && !this.ws.isClosed()) {
            JsonObject obj = new JsonObject();

            //
            // 这里只订阅 1min的交易, 其它的都由 1min 来进行计算得到
            //
            obj.put("unsub", RequestUtils.toKlineSub(toGenericSymbol(symbol), Period._1_MIN));
            obj.put("id", subIdPrefix + symbol);
            try {
                this.ws.writeTextMessage(obj.toString());
                result = true;
            } catch (Exception ex) {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public String name() {
        return HuoBiKlineCollector.class.getName();
    }

    /**
     * 描述一个收集器
     *
     * @return 收集器描述
     */
    @Override
    public String desc() {
        return "火币数据收集器";
    }

    /**
     * 注册websocket消息处理事件
     *
     * @param ws
     */
    private void registerMsgHandler(WebSocket ws) {
        ws.frameHandler(frame -> {

            // 处理二进制帧并且确保是最终帧
            if (frame.isBinary() && frame.isFinal()) {
                byte[] data = frame.binaryData()
                        .getBytes();
                try {
                    // 数据解压并且转换为JSON
                    data = GZIPUtils.decompress(data);
                    JsonObject obj = (JsonObject) Json.decodeValue(new String(data, StandardCharsets.UTF_8));
                    // 如果是 ping 消息则需要回复 pong
                    if (isPingMsg(obj)) {
                        this.writePong(ws);
                    } else if (isTickMsg(obj)) {
                        // 如果是交易 tick 则进行消费
                        if (consumer != null) {
                            consumer.accept(obj);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 判断是否为ping消息
     *
     * @param object 消息对象
     * @return 是否为ping消息
     */
    private boolean isPingMsg(JsonObject object) {
        return object.containsKey("ping");
    }

    /**
     * 判断消息是否为tick消息
     *
     * @param object 消息对象
     * @return 是否为tick消息
     */
    private boolean isTickMsg(JsonObject object) {
        return object.containsKey("tick");
    }

    /**
     * 回复pong消息
     *
     * @param ws websocket
     */
    private void writePong(WebSocket ws) {
        ws.writeTextMessage("{\"pong\":" + System.currentTimeMillis() + "}");
    }

    @Data
    private static class Config {
        /**
         * 域名
         */
        private String host = "api.huobiasia.vip";

        /**
         * 请求url
         */
        private String reqUrl = "/ws";

        /**
         * 心跳周期 默认5秒
         */
        private Long heartbeat = TimeUnit.SECONDS.toMillis(5);
    }

    private String toGenericSymbol(String symbol) {
        return symbol.replace("-", "")
                .replace("/", "")
                .toLowerCase();
    }
}
