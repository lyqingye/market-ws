package com.market.repository;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.net.NetClientOptions;
import io.vertx.redis.client.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class RedisRepo {
  /**
   * vertx 实例
   */
  private Vertx vertx;

  /**
   * redis 连接
   */
  private RedisConnection redisConn;

  /**
   * redis api
   */
  private RedisAPI redisApi;

  /**
   * 创建redis仓库
   *
   * @param vertx            vertx
   * @param connectionString 链接字符串
   * @return future
   */
  public static Future<RedisRepo> create(Vertx vertx, String connectionString) {
    Promise<RedisRepo> promise = Promise.promise();
    RedisRepo self = new RedisRepo();
    NetClientOptions netOptions = new NetClientOptions()
        .setConnectTimeout(5000)
        .setReconnectAttempts(2)
        .setReconnectInterval(1000);
    RedisOptions redisOptions = new RedisOptions()
        .setConnectionString(connectionString)
        .setNetClientOptions(netOptions);
    Redis.createClient(vertx, redisOptions)
         .connect(onConnect -> {
           if (onConnect.succeeded()) {
             self.redisConn = onConnect.result();
             self.redisApi = RedisAPI.api(self.redisConn);
             promise.complete(self);
             // 打印异常堆栈
             self.redisConn.exceptionHandler(Throwable::printStackTrace);
           } else {
             promise.fail(onConnect.cause());
           }
         });
    return promise.future();
  }

  /**
   * Hash get all
   *
   * @param key     key
   * @param handler 返回值处理
   */
  public void hGetAll(String key, Handler<AsyncResult<Map<String, String>>> handler) {
    redisApi.hgetall(key, ar -> {
      if (ar.succeeded()) {
        Response response = ar.result();
        int size = response.size();
        if (size > 0 && size % 2 == 0) {
          Map<String, String> result = new HashMap<>(size >> 1);
          for (int i = 0; i < size; i += 2) {
            // 这里不处理 key 为null的结果
            String k = response.get(i).toString();
            if (k != null) {
              result.put(k, response.get(i + 1).toString());
            }
          }
          handler.handle(Future.succeededFuture(result));
        } else if (size == 0) {
          handler.handle(Future.succeededFuture(Collections.emptyMap()));
        } else {
          handler.handle(Future.failedFuture("invalid response"));
        }
      } else {
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  /**
   * Hash set
   *
   * @param key     key
   * @param hashKey hashKey
   * @param value   value
   * @param handler handler
   */
  public void hSet(String key, String hashKey, String value,
                   Handler<AsyncResult<Void>> handler) {
    redisApi.hset(Arrays.asList(key, hashKey, value), ar -> {
      if (ar.succeeded()) {
        handler.handle(Future.succeededFuture());
      } else {
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  /**
   * SoredSet ZCard
   *
   * @param key     key
   * @param handler handler
   */
  public void zCard(String key,
                    Handler<AsyncResult<Integer>> handler) {
    redisApi.zcard(key, ar -> {
      if (ar.succeeded()) {
        handler.handle(Future.succeededFuture(this.responseToObj(ar.result())));
      } else {
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  /**
   * SoredSet ZRange
   *
   * @param key     key
   * @param start   start
   * @param stop    stop
   * @param handler handler
   */
  public void zRange(String key, int start, int stop,
                     Handler<AsyncResult<List<String>>> handler) {
    final List<String> cmd = Arrays.asList(key, String.valueOf(start), String.valueOf(stop));
    redisApi.zrange(cmd, ar -> {
      if (ar.succeeded()) {
        final Response response = ar.result();
        final List<String> objects = response.stream()
                                             .map(obj -> (String) this.responseToObj(obj))
                                             .collect(Collectors.toList());
        handler.handle(Future.succeededFuture(objects));
      } else {
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  /**
   * 批量命令
   *
   * @param commands 命令
   * @param onSend   结果
   */
  public void batch(List<Request> commands,
                    Handler<AsyncResult<List<@Nullable Response>>> onSend) {
    redisConn.batch(commands, onSend);
  }

  /**
   * 关闭redis
   */
  public void close() {
    redisApi.close();
  }

  /**
   * 处理消息响应
   *
   * @param r 响应体
   * @return 目标真实对象
   */
  @SuppressWarnings("unchecked")
  private <T> T responseToObj(Response r) {
    switch (r.type()) {
      case INTEGER: {
        return (T) r.toInteger();
      }
      case MULTI:
      case BULK: {
        // 统一用String
        return (T) r.toString(StandardCharsets.UTF_8);
      }
      case SIMPLE: {
        return (T) r.toString();
      }
      case ERROR: {
        System.out.println("redis error response, error msg: " + r.toString());
      }
    }
    return null;
  }
}
