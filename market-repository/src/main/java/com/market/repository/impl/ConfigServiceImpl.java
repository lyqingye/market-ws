package com.market.repository.impl;

import com.market.common.def.CacheKey;
import com.market.common.service.config.ConfigService;
import com.market.common.service.config.dto.Mapping;
import com.market.repository.repository.ConfigRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.SharedData;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigServiceImpl implements ConfigService {
    /**
     * custom:generic
     */
    private AsyncMap<Object, Object> c2gMappings;

    /**
     * generic:custom
     */
    private AsyncMap<Object, Object> g2cMappings;

    /**
     * 仓库
     */
    private ConfigRepository repo;

    /**
     * 同步方式创建代理
     *
     * @param vertx 实例
     */
    public ConfigServiceImpl(ConfigRepository repository, Vertx vertx, Handler<AsyncResult<Void>> handler) {
        this.repo = repository;
        final SharedData sd = vertx.sharedData();
        sd.getAsyncMap(CacheKey.SYMBOL_CUSTOM_TO_GENERIC.name(), ar -> {
            if (ar.succeeded()) {
                c2gMappings = ar.result();
                repository.c2gMappings(c2gMpRs -> {
                    if (c2gMpRs.succeeded()) {
                        for (Mapping mapping : c2gMpRs.result()) {
                            c2gMappings.put(mapping.getSource(), mapping.getTarget(), ignored -> {
                            });
                        }
                        sd.getAsyncMap(CacheKey.SYMBOL_GENERIC_TO_CUSTOM.name(), ar2 -> {
                            if (ar2.succeeded()) {
                                g2cMappings = ar2.result();
                                repository.g2cMappings(g2cMpRs -> {
                                    if (g2cMpRs.succeeded()) {
                                        for (Mapping mapping : g2cMpRs.result()) {
                                            g2cMappings.put(mapping.getSource(), mapping.getTarget(), ignored -> {
                                            });
                                        }
                                        handler.handle(Future.succeededFuture());
                                    } else {
                                        handler.handle(Future.failedFuture(g2cMpRs.cause()));
                                    }
                                });
                            } else {
                                ar2.cause().printStackTrace();
                                handler.handle(Future.failedFuture(ar2.cause()));
                            }
                        });
                    } else {
                        handler.handle(Future.failedFuture(c2gMpRs.cause()));
                    }
                });
            } else {
                ar.cause().printStackTrace();
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });


    }

    /**
     * 添加/更新交易对映射（custom:generic）
     *
     * @param custom  自定义交易对
     * @param generic 通用交易对
     * @param handler 结果处理器
     */
    @Override
    public void putC2G(String custom, String generic, Handler<AsyncResult<Void>> handler) {
        repo.putC2G(custom, generic, store -> {
            if (store.succeeded()) {
                c2gMappings.put(custom, generic, handler);
                // 更新 g2c
                this.putG2C(generic,custom, ignored -> {});
            } else {
                handler.handle(Future.failedFuture(store.cause()));
            }
        });
    }

    /**
     * 添加/更新交易对映射 (generic:custom)
     *
     * @param generic 通用交易对
     * @param custom  默认交易对
     * @param handler 结果处理器
     */
    @Override
    public void putG2C(String generic, String custom, Handler<AsyncResult<Void>> handler) {
        repo.putG2C(generic, custom, store -> {
            if (store.succeeded()) {
                g2cMappings.put(generic, custom, handler);
            } else {
                handler.handle(Future.failedFuture(store.cause()));
            }
        });
    }

    /**
     * 自定义交易对转换为通用交易对
     *
     * @param custom  自定义交易对
     * @param handler 结果处理器
     */
    @Override
    public void c2g(String custom, Handler<AsyncResult<String>> handler) {
        c2gMappings.get(custom, ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture((String) ar.result()));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /**
     * 通用交易对转换为自定义交易对
     *
     * @param generic 通用交易对
     * @param handler 结果处理器
     */
    @Override
    public void g2c(String generic, Handler<AsyncResult<String>> handler) {
        g2cMappings.get(generic, ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture((String) ar.result()));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /**
     * custom:generic 交易对映射集合
     *
     * @param handler 结果集合
     */
    @Override
    public void c2gMappings(Handler<AsyncResult<List<Mapping>>> handler) {
        this.asyncMapToMappings(c2gMappings, handler);
    }

    /**
     * generic:custom 交易对映射集合
     *
     * @param handler 结果处理器
     */
    @Override
    public void g2cMappings(Handler<AsyncResult<List<Mapping>>> handler) {
        this.asyncMapToMappings(g2cMappings, handler);
    }

    /**
     * 更新市场价格
     * <p>
     * note:
     * 可能你也注意到价格的数据类型是price, 对的没错用的就是 double, 因为这个东西又不拿来计算
     * 所以没必要用 Bigdecimal
     *
     * @param symbol  交易对
     * @param price   价格
     * @param handler 结果处理器
     */
    @Override
    public void updateMarketPrice(String symbol, double price, Handler<AsyncResult<Void>> handler) {
        repo.updateMarketPrice(symbol, price, handler);
    }

    /**
     * 获取所有交易对的价格
     *
     * @param handler 结果处理器
     */
    @Override
    public void listMarketPrice(Handler<AsyncResult<List<Mapping>>> handler) {
        repo.listMarketPrice(handler);
    }

    /**
     * 异步map转换为mapping列表
     *
     * @param map     异步map
     * @param handler 结果处理器
     */
    private void asyncMapToMappings(AsyncMap<Object, Object> map, Handler<AsyncResult<List<Mapping>>> handler) {
        map.entries(ar -> {
            if (ar.succeeded()) {
                final List<Mapping> mappings = ar.result()
                        .entrySet()
                        .stream()
                        .map(Mapping::new)
                        .collect(Collectors.toList());
                handler.handle(Future.succeededFuture(mappings));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }
}
