package com.market.repository.impl;

import com.market.common.service.repository.KlineRepositoryService;
import com.market.repository.repository.KlineRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;
import java.util.Objects;

/**
 * @author yjt
 * @since 2020/11/15 13:17
 */
public class KlineServiceImpl implements KlineRepositoryService {

    /**
     * 仓库
     */
    private KlineRepository repo;

    public KlineServiceImpl(KlineRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    /**
     * 根据交易对获取ticks长度
     *
     * @param symbol  交易对
     * @param handler 结果处理器
     */
    @Override
    public void sizeOfKlineTicks(String symbol, Handler<AsyncResult<Integer>> handler) {
        repo.sizeOfKlineTicks(symbol, handler);
    }

    /**
     * 根据交易对获取指定区间内的交易ticks
     *
     * @param symbol  交易对
     * @param start   开始索引 0开始
     * @param stop    结束索引 size - 1
     * @param handler 结果处理器
     */
    @Override
    public void listKlineTicksLimit(String symbol, int start, int stop, Handler<AsyncResult<List<String>>> handler) {
        repo.listKlineTicksLimit(symbol, start, stop, handler);
    }

    /**
     * 根据交易对获取合并后的ticks
     *
     * @param symbol  交易对
     * @param handler 结果处理器
     */
    @Override
    public void listKlineTicks(String symbol, Handler<AsyncResult<List<String>>> handler) {
        repo.listKlineTicks(symbol, handler);
    }
}
