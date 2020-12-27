package com.market.bridge.impl;

import com.market.bridge.MarketBridge;
import com.market.common.messages.parser.FrameParser;
import com.market.common.session.TcpSessionWrapper;
import com.market.common.utils.VertxUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
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
  public void start(Vertx vertx, JsonObject jsonConfig,
                    Handler<AsyncResult<MarketBridge>> handler) {
    String host = "localhost";
    int port = 8888;
    boolean debug = false;

    // 判断是否提供了配置
    if (jsonConfig != null) {
      host = VertxUtil.jsonGetValue(jsonConfig,"market.bridge.engine.tcp.host",String.class, host);
      port = VertxUtil.jsonGetValue(jsonConfig,"market.bridge.engine.tcp.port",Integer.class, port);
      debug = VertxUtil.jsonGetValue(jsonConfig,"market.bridge.engine.tcp.debug",Boolean.class,false);
    }

    System.out.println("[MarketBridge-TcpBridge]: listen host: " + host);
    System.out.println("[MarketBridge-TcpBridge]: listen port: " + port);
    System.out.println("[MarketBridge-TcpBridge]: debug: " + debug);

    // 初始化会话管理器
    sessionManager = new ConcurrentHashMap<>();
    final NetServerOptions options = new NetServerOptions();
    options.setTcpKeepAlive(true);

    boolean finalDebug = debug;
    tcpServer = vertx.createNetServer(options)
                     .connectHandler(socket -> {
                       // 保存会话
                       sessionManager.putIfAbsent(socket.writeHandlerID(), TcpSessionWrapper.of(socket));
                       final FrameParser parser = new FrameParser();
                       // 解码器
                       socket.handler(buf -> {
                         // 解码器
                         parser.handle(buf, ar -> {
                           if (finalDebug) {
                             System.out.println("[MarketBridge-TcpBridge]: " + buf.toString());
                           }
                           process(socket.remoteAddress().toString(), ar);
                         });
                       });

                       socket.closeHandler(v -> {
                         sessionManager.remove(socket.writeHandlerID());
                       });
                     })
                     .exceptionHandler(Throwable::printStackTrace)
                     .listen(port, host, ar -> {
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
}
