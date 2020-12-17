package com.market.repository.repository;

import com.market.common.def.CacheKey;
import com.market.common.service.config.dto.Mapping;
import com.market.repository.RedisRepo;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigRepository {
    /**
     * redis repo
     */
    private RedisRepo redisRepo;

    public ConfigRepository(RedisRepo redisRepo) {
        this.redisRepo = Objects.requireNonNull(redisRepo);
    }

    /**
     * 添加/更新交易对映射（custom:generic）
     *
     * @param custom  自定义交易对
     * @param generic 通用交易对
     * @param handler 结果处理器
     */
    public void putC2G(String custom, String generic, Handler<AsyncResult<Void>> handler) {
        redisRepo.hSet(CacheKey.SYMBOL_CUSTOM_TO_GENERIC.name(), custom, generic, handler);
    }

    /**
     * 添加/更新交易对映射（custom:generic）
     *
     * @param custom 自定义交易对
     * @param generic 通用交易对
     * @return future
     */
    public Future<Void> putC2G(String custom, String generic) {
        Promise<Void> promise = Promise.promise();
        redisRepo.hSet(CacheKey.SYMBOL_CUSTOM_TO_GENERIC.name(), custom, generic, promise);
        return promise.future();
    }

    /**
     * 添加/更新交易对映射 (generic:custom)
     *
     * @param generic 通用交易对
     * @param custom  默认交易对
     * @param handler 结果处理器
     */
    public void putG2C(String generic, String custom, Handler<AsyncResult<Void>> handler) {
        redisRepo.hSet(CacheKey.SYMBOL_GENERIC_TO_CUSTOM.name(), generic, custom, handler);
    }

    /**
     * custom:generic 交易对映射集合
     *
     * @param handler 结果集合
     */
    public void c2gMappings(Handler<AsyncResult<List<Mapping>>> handler) {
        redisRepo.hGetAll(CacheKey.SYMBOL_CUSTOM_TO_GENERIC.name(), ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture(Mapping.toMappings(ar.result())));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /**
     * custom:generic 交易对映射集合
     *
     * @return  future
     */
    public Future<List<Mapping>> c2gMappings () {
        Promise<Map<String,String>> promise = Promise.promise();
        redisRepo.hGetAll(CacheKey.SYMBOL_CUSTOM_TO_GENERIC.name(),promise);
        return promise.future().compose(rs -> Future.succeededFuture(Mapping.toMappings(rs)));
    }

    /**
     * generic:custom 交易对映射集合
     *
     * @param handler 结果处理器
     */
    public void g2cMappings(Handler<AsyncResult<List<Mapping>>> handler) {
        redisRepo.hGetAll(CacheKey.SYMBOL_GENERIC_TO_CUSTOM.name(), ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture(Mapping.toMappings(ar.result())));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /**
     * generic:custom 交易对映射集合
     *
     * @return  future
     */
    public Future<List<Mapping>> g2cMappings() {
        Promise<Map<String,String>> promise = Promise.promise();
        redisRepo.hGetAll(CacheKey.SYMBOL_GENERIC_TO_CUSTOM.name(),promise);
        return promise.future().compose(rs -> Future.succeededFuture(Mapping.toMappings(rs)));
    }

    /**
     * 更新市场价格
     *
     * note:
     * 可能你也注意到价格的数据类型是price, 对的没错用的就是 double, 因为这个东西又不拿来计算
     * 所以没必要用 BigDecimal
     *
     * @param symbol 交易对
     * @param price 价格
     * @param handler 结果处理器
     */
    public void updateMarketPrice(String symbol,double price, Handler<AsyncResult<Void>> handler) {
        redisRepo.hSet(CacheKey.MARKET_PRICE.name(), symbol,String.valueOf(price),handler);
    }

    /**
     * 获取所有交易对的价格
     *
     * @param handler 结果处理器
     */
    public void listMarketPrice(Handler<AsyncResult<List<Mapping>>> handler) {
        redisRepo.hGetAll(CacheKey.MARKET_PRICE.name(), ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture(Mapping.toMappings(ar.result())));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /**
     * 获取所有交易对的价格
     *
     * @return 结果future
     */
    public Future<List<Mapping>> listMarketPrice () {
        Promise<Map<String,String>> promise = Promise.promise();
        redisRepo.hGetAll(CacheKey.MARKET_PRICE.name(),promise);
        return promise.future().compose(rs -> Future.succeededFuture(Mapping.toMappings(rs)));
    }

    /**
     * 需要自己更新数据
     *
     * @param data data
     */
    public void forUpdatePrice(Object data) {
        if (data instanceof String) {
            JsonObject jsonObj = (JsonObject) Json.decodeValue((String) data);
            String symbol = jsonObj.getString("symbol");
            if (symbol == null || symbol.isEmpty()) {
                return;
            }
            // 异步更新到redis
            this.updateMarketPrice(symbol, jsonObj.getDouble("price"), ar -> {
                if (ar.failed()) {
                    ar.cause().printStackTrace();
                }
            });
        } else {
            System.err.println("[KlineRepository]: null or not string value trade result!");
        }
    }
}
