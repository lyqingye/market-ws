package com.market.common.utils;

import io.vertx.core.*;

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
}
