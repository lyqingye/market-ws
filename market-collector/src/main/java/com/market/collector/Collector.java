package com.market.collector;

import com.market.common.service.collector.dto.CollectorStatusDto;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.function.Consumer;

/**
 * 三方数据收集器接口定义, 支持的功能如下：
 * 1. 部署数据采集器和取消部署
 * 2. 订阅交易对以及取消定义指定交易对
 * 3. 查看正在定义的交易对
 * 4. 开始和停止采集数据
 *
 * @author ex
 */
public interface Collector {
    /**
     * 返回当前收集器的名称
     *
     * @return 收集器名称
     */
    String name();

    /**
     * 描述一个收集器
     *
     * @return 收集器描述
     */
    default String desc() {
        return name();
    }

    /**
     * 部署一个收集器
     *
     * @param vertx    vertx 实例
     * @param consumer 数据消费器
     * @param args     附加参数 (可以为空)
     * @return 是否部署成功
     * @throws Exception 如果部署失败
     */
    boolean deploy(Vertx vertx,
                   Consumer<JsonObject> consumer,
                   JsonObject args);

    /**
     * 取消部署收集器
     *
     * @param args 附加参数可以为空
     * @return 如果取消部署失败
     * @throws Exception 如果取消部署失败
     */
    boolean unDeploy(JsonObject args);

    /**
     * 订阅一个交易对
     *
     * @param symbol 交易对
     * @return 是否订阅成功
     */
    boolean subscribe(String symbol);

    /**
     * 取消订阅一个交易对
     *
     * @param symbol 交易对
     * @return 是否取消订阅成功
     */
    boolean unSubscribe(String symbol);

    /**
     * 获取当前正在订阅的交易对
     *
     * @return 当前正在订阅的交易对列表
     */
    List<String> listSubscribedSymbol();

    /**
     * 开启收集数据
     *
     * @param handler 回调
     */
    void start(Handler<AsyncResult<Boolean>> handler);

    /**
     * 停止数据收集
     *
     * @return 是否停止成功
     */
    boolean stop();

    /**
     * 是否正在收集
     *
     * @return 是否正在收集
     */
    boolean isRunning();

    /**
     * 是否已经部署
     *
     * @return 是否已经部署
     */
    boolean isDeployed();

    /**
     * 快照状态
     *
     * @return 状态快照
     */
    default CollectorStatusDto snapStatus() {
        CollectorStatusDto status = new CollectorStatusDto();
        status.setName(name());
        status.setDesc(desc());
        status.setRunning(isRunning());
        status.setSubscribedSymbols(listSubscribedSymbol());
        status.setDeployed(isDeployed());
        return status;
    }
}
