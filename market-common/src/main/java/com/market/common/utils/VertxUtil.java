package com.market.common.utils;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;

public final class VertxUtil {

    /**
     * 部署 verticle
     *
     * @param vertx vertx
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
     * @param vertx vertx
     * @param verticle verticle
     * @param options options
     * @return future
     */
    public static Future<String> deploy(Vertx vertx, Verticle verticle, DeploymentOptions options) {
        Future<String> future = Promise.<String>promise().future();
        vertx.deployVerticle(verticle,options,future);
        return future;
    }

    /**
     * 获取异步共享map
     *
     * @param vertx vertx
     * @param name name of map
     * @param <K> key
     * @param <V> value
     * @return future
     */
    public static <K,V> Future<AsyncMap<K, V>> getAsyncMap (Vertx vertx,String name) {
        Promise<AsyncMap<K, V>> promise = Promise.promise();
        vertx.sharedData().getAsyncMap(name,promise);
        return promise.future();
    }

    /**
     * 异步执行并且忽略返回值
     *
     * @param vertx vertx
     * @param cmd 需要执行的内容
     */
    public static void asyncFastCallIgnoreRs(Vertx vertx, Runnable cmd) {
        vertx.executeBlocking(promise -> {
            cmd.run();
            promise.complete();
        }, ignored -> {});
    }
}
