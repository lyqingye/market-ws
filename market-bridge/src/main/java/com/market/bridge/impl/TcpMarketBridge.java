package com.market.bridge.impl;

import com.market.bridge.MarketBridge;
import com.market.common.messages.parser.FrameParser;
import com.market.common.session.TcpSessionWrapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TcpMarketBridge implements MarketBridge {

    /**
     * 会话管理
     */
    private Map<String, TcpSessionWrapper> sessionManager;

    /**
     * tcp服务器
     */
    private NetServer tcpServer;

    /**
     * 启动桥接器
     *
     * @param vertx      vertx
     * @param jsonConfig 配置
     * @param handler    结果处理
     */
    @Override
    public void start(Vertx vertx, String jsonConfig,
                      Handler<AsyncResult<MarketBridge>> handler) {
        // 默认配置
        TcpBridgeConfig config = new TcpBridgeConfig();

        // 判断是否提供了配置
        if (jsonConfig != null) {
            config = Json.decodeValue(jsonConfig, TcpBridgeConfig.class);
        }
        // 初始化会话管理器
        sessionManager = new ConcurrentHashMap<>();
        final NetServerOptions options = new NetServerOptions();
        options.setTcpKeepAlive(true);

        // 消息解析器
        FrameParser parser = new FrameParser();

        tcpServer = vertx.createNetServer(options)
                .connectHandler(socket -> {
                    // 保存会话
                    sessionManager.putIfAbsent(socket.writeHandlerID(), TcpSessionWrapper.of(socket));

                    // 解码器
                    socket.handler(buf -> {
                        // 解码器
                        parser.handle(buf, ar -> {
                            process(socket.remoteAddress().toString(), ar);
                        });
                    });

                    socket.closeHandler(v -> {
                        sessionManager.remove(socket.writeHandlerID());
                    });
                })
                .exceptionHandler(Throwable::printStackTrace)
                .listen(config.port, config.host, ar -> {
                    if (ar.succeeded()) {
                        handler.handle(Future.succeededFuture());
                    } else {
                        handler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /**
     * 停止桥接器
     *
     * @param handler 结果处理器
     */
    @Override
    public void stop(Handler<AsyncResult<Void>> handler) {
        sessionManager.clear();
        tcpServer.close(handler);
    }

    /**
     * 广播消息
     *
     * @param buffer 二进制消息
     */
    @Override
    public void broadcast(Buffer buffer) {
        for (TcpSessionWrapper wrapper : sessionManager.values()) {
            wrapper.getSocket().write(buffer);
        }
    }

    /**
     * 广播消息
     *
     * @param text 文本消息
     */
    @Override
    public void broadcast(String text) {
        for (TcpSessionWrapper wrapper : sessionManager.values()) {
            wrapper.getSocket().write(text);
        }
    }

    /**
     * 广播消息并且排除指定socket地址
     *
     * @param buffer     二进制消息
     * @param socketAddr socket地址
     */
    @Override
    public void broadcastWithExcludeSocketAddr(Buffer buffer, String socketAddr) {
        for (TcpSessionWrapper wrapper : sessionManager.values()) {
            NetSocket socket = wrapper.getSocket();
            if (!socket.remoteAddress().toString().equals(socketAddr)) {
                socket.write(buffer);
            }
        }
    }

    /**
     * 广播消息并且排除指定socket地址
     *
     * @param text       文本消息
     * @param socketAddr socket地址
     */
    @Override
    public void broadcastWithExcludeSocketAddr(String text, String socketAddr) {
        for (TcpSessionWrapper wrapper : sessionManager.values()) {
            NetSocket socket = wrapper.getSocket();
            if (!socket.remoteAddress().toString().equals(socketAddr)) {
                socket.write(text);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class TcpBridgeConfig {
        private String host = "localhost";
        private int port = 8888;
    }
}
