package com.market.common.utils;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class VertxUtil {

  /**
   * 部署 verticle
   *
   * @param vertx    vertx
   * @param verticle verticle
   * @return future
   */
  public static Future<String> deploy(Vertx vertx, Verticle verticle) {
    Future<String> future = Promise.<String>promise().future();
    vertx.deployVerticle(verticle, future);
    return future;
  }

  /**
   * 部署 verticle
   *
   * @param vertx    vertx
   * @param verticle verticle
   * @param options  options
   * @return future
   */
  public static Future<String> deploy(Vertx vertx, Verticle verticle, DeploymentOptions options) {
    Future<String> future = Promise.<String>promise().future();
    vertx.deployVerticle(verticle, options, future);
    return future;
  }

  /**
   * 部署 verticle
   *
   * @param vertx    vertx
   * @param verticle verticle
   * @param config   config
   * @return future
   */
  public static Future<String> deploy(Vertx vertx, Verticle verticle, JsonObject config) {
    return deploy(vertx, verticle, new DeploymentOptions().setConfig(Objects.requireNonNull(config)));
  }

  /**
   * 获取异步共享map
   *
   * @param vertx vertx
   * @param name  name of map
   * @param <K>   key
   * @param <V>   value
   * @return future
   */
  public static <K, V> Future<AsyncMap<K, V>> getAsyncMap(Vertx vertx, String name) {
    Promise<AsyncMap<K, V>> promise = Promise.promise();
    vertx.sharedData().getAsyncMap(name, promise);
    return promise.future();
  }

  /**
   * 异步执行并且忽略返回值
   *
   * @param vertx vertx
   * @param cmd   需要执行的内容
   */
  public static void asyncFastCallIgnoreRs(Vertx vertx, Runnable cmd) {
    vertx.executeBlocking(promise -> {
      cmd.run();
      promise.complete();
    }, ignored -> {
    });
  }

  /**
   * 递归获取json中的值
   *
   * @param obj json
   * @param key key 支持 key1.key2.key3 递归
   * @param clazz 值类型
   * @param <T> 值类型
   * @return 目标值
   */
  @SuppressWarnings("Duplicates")
  public static <T> T jsonGetValue(JsonObject obj, String key, Class<T> clazz) {
    return jsonGetValue(obj, key, clazz, null);
  }

  /**
   * 递归获取json中的值
   *
   * @param obj json
   * @param key key 支持 key1.key2.key3 递归
   * @param clazz 值类型
   * @param defaultValue 默认值
   * @param <T> 值类型
   * @return 目标值
   */
  @SuppressWarnings("Duplicates")
  public static <T> T jsonGetValue(JsonObject obj, String key, Class<T> clazz, T defaultValue) {
    Objects.requireNonNull(obj);
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("key is blank");
    }
    if (clazz == null) {
      throw new IllegalArgumentException("invalid clazz of return value");
    }
    JsonObject curObj = obj;
    T result = defaultValue;
    String[] keys = key.split("\\.");
    for (int i = 0; i < keys.length; i++) {
      if (curObj == null) {
        break;
      }
      String objKey = keys[i];
      if (i == keys.length - 1) {
        // find target
        result = clazz.cast(curObj.getValue(objKey));
      } else {
        curObj = curObj.getJsonObject(objKey);
      }
    }
    if (result == null) {
      return defaultValue;
    }
    return result;
  }

  /**
   * 递归获取json中的值
   *
   * @param obj json
   * @param key key 支持 key1.key2.key3 递归
   * @param clazz 值类型
   * @param <T> 值类型
   * @return 目标值
   */
  @SuppressWarnings("Duplicates")
  public static <T> List<T> jsonListValue(JsonObject obj, String key, Class<T> clazz) {
    Objects.requireNonNull(obj);
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("key is blank");
    }
    if (clazz == null) {
      throw new IllegalArgumentException("invalid clazz of return value");
    }
    JsonObject curObj = obj;
    List<T> result = Collections.emptyList();
    String[] keys = key.split("\\.");
    for (int i = 0; i < keys.length; i++) {
      if (curObj == null) {
        break;
      }
      String objKey = keys[i];
      if (i == keys.length - 1) {
        // find target
        JsonArray jsonArray = curObj.getJsonArray(objKey);
        if (jsonArray != null) {
          result = new ArrayList<>(jsonArray.size());
          for (Object o : jsonArray) {
            result.add(clazz.cast(o));
          }
        }
      } else {
        curObj = curObj.getJsonObject(objKey);
      }
    }
    return result;
  }
}

