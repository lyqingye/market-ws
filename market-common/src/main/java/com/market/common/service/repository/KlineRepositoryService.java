package com.market.common.service.repository;

import com.market.common.def.ServiceAddress;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

/**
 * @author yjt
 * @since 2020/11/14 19:39
 */
@ProxyGen
public interface KlineRepositoryService {

    static KlineRepositoryService createProxy(Vertx vertx) {
        return new KlineRepositoryServiceVertxEBProxy(vertx,
                ServiceAddress.KLINE_REPOSITORY.name());
    }

    /**
     * 根据交易对获取ticks长度
     *
     * @param symbol  交易对
     * @param handler 结果处理器
     */
    void sizeOfKlineTicks(String symbol,
                          Handler<AsyncResult<Integer>> handler);

    /**
     * 根据交易对获取指定区间内的交易ticks
     *
     * @param symbol  交易对
     * @param start   开始索引 0开始
     * @param stop    结束索引 size - 1
     * @param handler 结果处理器
     */
    void listKlineTicksLimit(String symbol,
                             int start, int stop,
                             Handler<AsyncResult<List<String>>> handler);

    /**
     * 根据交易对获取合并后的ticks
     *
     * @param symbol  交易对
     * @param handler 结果处理器
     */
    void listKlineTicks(String symbol,
                        Handler<AsyncResult<List<String>>> handler);
}
