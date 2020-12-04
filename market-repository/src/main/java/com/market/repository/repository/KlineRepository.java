package com.market.repository.repository;

import com.market.common.def.Period;
import com.market.common.ds.TimeWheel;
import com.market.common.messages.payload.kline.KlineTickResp;
import com.market.common.service.config.dto.Mapping;
import com.market.common.utils.RequestUtils;
import com.market.repository.RedisRepo;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class KlineRepository {
    /**
     * redis repo
     */
    private final RedisRepo redisRepo;

    /**
     * 缓存的1min ticks
     */
    private final Map<String, TimeWheel<KlineTickResp>> cacheTick = new ConcurrentHashMap<>();

    /**
     * 需要初始化数据的交易对数量
     */
    private int totalSymbolNeedToInit = 0;
    /**
     * 已经初始化数据的交易对数量
     */
    private final AtomicInteger numOfInitSuccessSymbol = new AtomicInteger(0);

    /**
     * 初始化仓库
     *
     * @param redisRepo  redis仓库
     * @param configRepo 配置仓库
     * @param handler    结果处理器
     */
    public KlineRepository(RedisRepo redisRepo,
                           ConfigRepository configRepo,
                           Handler<AsyncResult<KlineRepository>> handler) {
        this.redisRepo = Objects.requireNonNull(redisRepo);
        //
        // 2020.11.19 你看这像不像回调地狱
        //
        configRepo.c2gMappings(mpRs -> {
            if (mpRs.succeeded()) {
                List<Mapping> mappings = mpRs.result();
                if (!mappings.isEmpty()) {
                    totalSymbolNeedToInit = mappings.size();
                    for (Mapping mapping : mappings) {
                        this.sizeOfKlineTicks(mapping.getTarget(), sizeRs -> {
                            if (sizeRs.succeeded()) {
                                Integer result = sizeRs.result();
                                if (result != null) {
                                    // 只截取最新的部分
                                    int start = 0;
                                    if (result >= Period._1_MIN.getNumOfPeriod()) {
                                        start = result - Period._1_MIN.getNumOfPeriod();
                                    }
                                    this.listKlineTicksLimit(mapping.getTarget(), start, -1, ticksRs -> {
                                        if (ticksRs.succeeded()) {
                                            TimeWheel<KlineTickResp> wheel = getOrCreateCachedTicks(mapping.getTarget());
                                            for (String tickJson : ticksRs.result()) {
                                                wheel.updateNow(Json.decodeValue(tickJson, KlineTickResp.class));
                                            }
                                            if (numOfInitSuccessSymbol.addAndGet(1) >= totalSymbolNeedToInit) {
                                                handler.handle(Future.succeededFuture(this));
                                            }
                                        } else {
                                            handler.handle(Future.failedFuture(ticksRs.cause()));
                                        }
                                    });
                                } else {
                                    handler.handle(Future.failedFuture("ticks size is null, key: " + mapping.getTarget()));
                                }
                            } else {
                                handler.handle(Future.failedFuture(sizeRs.cause()));
                            }
                        });
                    }
                } else {
                    handler.handle(Future.succeededFuture(this));
                }
            } else {
                handler.handle(Future.failedFuture(mpRs.cause()));
            }
        });
    }

    /**
     * 获取指定交易对K线数据长度
     *
     * @param symbol  交易对
     * @param handler 结果处理器
     */
    public void sizeOfKlineTicks(String symbol, Handler<AsyncResult<Integer>> handler) {
        redisRepo.zCard(RequestUtils.toKlineSub(symbol, Period._1_MIN), handler);
    }

    /**
     * 根据交易对获取指定区间内的交易ticks
     *
     * @param symbol  交易对
     * @param start   开始索引 0开始
     * @param stop    结束索引 size - 1
     * @param handler 结果处理器
     */
    public void listKlineTicksLimit(String symbol, int start, int stop, Handler<AsyncResult<List<String>>> handler) {
        redisRepo.zRange(RequestUtils.toKlineSub(symbol, Period._1_MIN), start, stop, handler);
    }

    /**
     * 根据交易对获取合并后的ticks
     *
     * @param symbol  交易对
     * @param handler 结果处理器
     */
    public void listKlineTicks(String symbol, Handler<AsyncResult<List<String>>> handler) {
        listKlineTicksLimit(RequestUtils.toKlineSub(symbol, Period._1_MIN), 0, -1, handler);
    }

    /**
     * 更新k线
     *
     * @param sub  交易对
     * @param tick tick
     */
    private void updateKlineTickAsync(String sub, KlineTickResp tick, Handler<AsyncResult<Void>> handler) {
        // 构造redis命令
        List<Request> batchCmd = new ArrayList<>(2);
        long time = tick.getTime();
        Request removeCmd = Request.cmd(Command.ZREMRANGEBYSCORE)
                .arg(sub)
                .arg(time).arg(time);

        batchCmd.add(removeCmd);

        Request addCmd = Request.cmd(Command.ZADD)
                .arg(sub)
                .arg(time)
                .arg(Json.encode(tick));
        batchCmd.add(addCmd);

        redisRepo.batch(batchCmd, ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /**
     * 需要自己更新数据
     *
     * @param data data
     */
    public void forUpdateKline(Object data) {
        if (data instanceof String) {
            JsonObject jsonObj = (JsonObject) Json.decodeValue((String) data);
            String sub = jsonObj.getString("ch");
            if (sub == null || sub.isEmpty()) {
                return;
            }
            // 可能会发生异常 (无需catch)
            KlineTickResp tick = jsonObj.getJsonObject("tick").mapTo(KlineTickResp.class);

            // 异步更新到redis
            this.updateKlineTickAsync(sub, getOrCreateCachedTicks(sub).updateNow(tick), ar -> {
                if (ar.failed()) {
                    ar.cause().printStackTrace();
                }
            });
        } else {
            System.err.println("[KlineRepository]: null or not string value trade result!");
        }
    }

    /**
     * 获取或者创建的ticks
     *
     * @param symbol 交易对
     * @return ticks
     */
    private TimeWheel<KlineTickResp> getOrCreateCachedTicks(String symbol) {
        return cacheTick.computeIfAbsent(symbol,
                k -> new TimeWheel<>(Period._1_MIN.getMill(),
                        Period._1_MIN.getNumOfPeriod(),
                        Collections.emptyList()));
    }
}
