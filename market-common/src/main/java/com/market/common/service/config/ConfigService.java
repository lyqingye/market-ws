package com.market.common.service.config;

import com.market.common.def.CacheKey;
import com.market.common.def.ServiceAddress;
import com.market.common.def.Topics;
import com.market.common.service.config.dto.Mapping;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

/**
 * @author yjt
 * @since 2020/11/14 23:31
 */
@ProxyGen
public interface ConfigService {

    /**
     * 同步方式创建代理
     *
     * @param vertx 实例
     * @return 服务对象
     */
    static ConfigService createProxy(Vertx vertx) {
        return new ConfigServiceVertxEBProxy(vertx, ServiceAddress.CONFIG.name());
    }

    /**
     * 添加/更新交易对映射（custom:generic）
     *
     * @param custom  自定义交易对
     * @param generic 通用交易对
     * @param handler 结果处理器
     */
    void putC2G(String custom, String generic, Handler<AsyncResult<Void>> handler);

    /**
     * 添加/更新交易对映射 (generic:custom)
     *
     * @param generic 通用交易对
     * @param custom  默认交易对
     * @param handler 结果处理器
     */
    void putG2C(String generic, String custom, Handler<AsyncResult<Void>> handler);

    /**
     * 自定义交易对转换为通用交易对
     *
     * @param custom  自定义交易对
     * @param handler 结果处理器
     */
    void c2g(String custom, Handler<AsyncResult<String>> handler);

    /**
     * 通用交易对转换为自定义交易对
     *
     * @param generic 通用交易对
     * @param handler 结果处理器
     */
    void g2c(String generic, Handler<AsyncResult<String>> handler);

    /**
     * custom:generic 交易对映射集合
     *
     * @param handler 结果集合
     */
    void c2gMappings(Handler<AsyncResult<List<Mapping>>> handler);

    /**
     * generic:custom 交易对映射集合
     *
     * @param handler 结果处理器
     */
    void g2cMappings(Handler<AsyncResult<List<Mapping>>> handler);

    /**
     * 更新市场价格
     *
     * note:
     * 可能你也注意到价格的数据类型是price, 对的没错用的就是 double, 因为这个东西又不拿来计算
     * 所以没必要用 Bigdecimal
     *
     * @param symbol 交易对
     * @param price 价格
     * @param handler 结果处理器
     */
    void updateMarketPrice(String symbol,double price, Handler<AsyncResult<Void>> handler);

    /**
     * 获取所有交易对的价格
     *
     * @param handler 结果处理器
     */
     void listMarketPrice(Handler<AsyncResult<List<Mapping>>> handler);
}
