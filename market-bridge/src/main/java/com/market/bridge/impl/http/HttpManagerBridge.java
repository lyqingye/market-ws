package com.market.bridge.impl.http;

import com.market.bridge.ManagerBridge;
import com.market.bridge.impl.http.input.*;
import com.market.bridge.impl.http.output.R;
import com.market.common.def.Topics;
import com.market.common.eventbus.impl.EventBusFactory;
import com.market.common.messages.bridge.TradeMessage;
import com.market.common.messages.payload.kline.KlineTradeResp;
import com.market.common.service.collector.KlineCollectorService;
import com.market.common.service.config.ConfigService;
import com.market.common.service.config.dto.Mapping;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpManagerBridge implements ManagerBridge {
    /**
     * vertx 实例
     */
    private Vertx vertx;

    /**
     * http 服务器
     */
    private HttpServer httpServer;

    /**
     * 配置服务
     */
    private ConfigService configService;

    /**
     * k线收集器服务
     */
    private KlineCollectorService collectorService;

    /**
     * 路由
     */
    private Router router;

    public HttpManagerBridge() {
    }

    /**
     * 启动桥接器
     *
     * @param vertx      vertx
     * @param jsonConfig 配置
     * @param handler    结果处理
     */
    @Override
    public void start(Vertx vertx, String jsonConfig, Handler<AsyncResult<ManagerBridge>> handler) {
        this.vertx = Objects.requireNonNull(vertx);

        // 默认配置
        HttpConfig config = new HttpConfig();

        // 判断是否提供了配置
        if (jsonConfig != null) {
            config = Json.decodeValue(jsonConfig, HttpConfig.class);
        }

        // 初始化依赖的服务项
        this.configService = ConfigService.createProxy(vertx);
        this.collectorService = KlineCollectorService.createProxy(vertx);
        // 路由
        router = Router.router(vertx);
        // 收集器路由注册
        this.registerCollectorApi(router);
        // 配置路由注册
        this.registerConfigApi(router);
        // 创建并且启动服务器
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(config.port, config.host, ar -> {
                    if (ar.succeeded()) {
                        this.httpServer = ar.result();
                        handler.handle(Future.succeededFuture());
                    } else {
                        handler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /**
     * 停止桥接器
     *
     * @param handler 结果处理器
     */
    @Override
    public void stop(Handler<AsyncResult<Void>> handler) {
        if (this.httpServer != null) {
            this.httpServer.close(handler);
        }
    }

    /**
     * k线收集器路由注册
     *
     * @param router
     */
    private void registerCollectorApi(Router router) {
        // 获取所有收集器
        router.route(HttpMethod.GET, "/market/collector/list")
                .handler(buf -> {
                    collectorService.listCollector(ar -> {
                        if (ar.succeeded()) {
                            buf.response().end(R.success(ar.result()));
                        } else {
                            buf.response().end(R.error(ar.cause()));
                        }
                    });
                });

        // 部署一个收集器
        router.route(HttpMethod.PUT, "/market/collector/deploy")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        CollectorOperateInput in = body.toJsonObject().mapTo(CollectorOperateInput.class);
                        if (StringUtil.isNullOrEmpty(in.getCollectorName())) {
                            buf.response().end(R.error("收集器名称不能为空"));
                        } else {
                            collectorService.deployCollector(in.getCollectorName(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error("部署失败！"));
                                }
                            });
                        }
                    });
                });

        // 部署一个收集器
        router.route(HttpMethod.PUT, "/market/collector/deploy/by/config")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        CollectorOperateExInput in = body.toJsonObject().mapTo(CollectorOperateExInput.class);
                        if (StringUtil.isNullOrEmpty(in.getCollectorName())) {
                            buf.response().end(R.error("收集器名称不能为空"));
                        } else {
                            collectorService.deployCollectorEx(in.getCollectorName(), in.getJsonConfig(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error("部署失败！"));
                                }
                            });
                        }
                    });

                });

        // 取消部署一个收集器
        router.route(HttpMethod.DELETE, "/market/collector/undeploy")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        CollectorOperateInput in = body.toJsonObject().mapTo(CollectorOperateInput.class);
                        if (StringUtil.isNullOrEmpty(in.getCollectorName())) {
                            buf.response().end(R.error("收集器名称不能为空"));
                        } else {
                            collectorService.unDeployCollector(in.getCollectorName(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error("取消部署失败!"));
                                }
                            });
                        }
                    });
                });

        // 启动一个收集器
        router.route(HttpMethod.PUT, "/market/collector/start")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        CollectorOperateInput in = body.toJsonObject().mapTo(CollectorOperateInput.class);
                        if (StringUtil.isNullOrEmpty(in.getCollectorName())) {
                            buf.response().end(R.error("收集器名称不能为空"));
                        } else {
                            collectorService.startCollector(in.getCollectorName(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error("启动收集器失败!"));
                                }
                            });
                        }
                    });

                });

        // 停止一个收集器
        router.route(HttpMethod.PUT, "/market/collector/stop")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        CollectorOperateInput in = body.toJsonObject().mapTo(CollectorOperateInput.class);
                        if (StringUtil.isNullOrEmpty(in.getCollectorName())) {
                            buf.response().end(R.error("收集器名称不能为空"));
                        } else {
                            collectorService.stopCollector(in.getCollectorName(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error("停止收集器失败!"));
                                }
                            });
                        }
                    });

                });

        // 订阅交易对
        router.route(HttpMethod.PUT, "/market/collector/subscribe")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        CollectorSubOptInput in = body.toJsonObject().mapTo(CollectorSubOptInput.class);
                        if (StringUtil.isNullOrEmpty(in.getCollectorName()) || StringUtil.isNullOrEmpty(in.getSymbol())) {
                            buf.response().end(R.error("参数不能为空!"));
                        } else {
                            collectorService.subscribe(in.getCollectorName(), in.getSymbol(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error("订阅交易对失败!"));
                                }
                            });
                        }
                    });

                });

        // 取消订阅交易对
        router.route(HttpMethod.PUT, "/market/collector/unsubscribe")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        CollectorSubOptInput in = body.toJsonObject().mapTo(CollectorSubOptInput.class);
                        if (StringUtil.isNullOrEmpty(in.getCollectorName()) || StringUtil.isNullOrEmpty(in.getSymbol())) {
                            buf.response().end(R.error("参数有误!"));
                        } else {
                            collectorService.unsubscribe(in.getCollectorName(), in.getSymbol(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error("取消订阅交易对失败!"));
                                }
                            });
                        }
                    });

                });
    }

    /**
     * 配置路由注册
     *
     * @param router 路由
     */
    private void registerConfigApi(Router router) {
        // [查询] 自定义的交易对 -> 通用的交易对
        router.route(HttpMethod.GET, "/market/symbol/c2g/mappings")
                .handler(buf -> {
                    configService.g2cMappings(ar -> {
                        if (ar.succeeded()) {
                            buf.response().end(R.success(Mapping.toMap(ar.result())));
                        } else {
                            buf.response().end(R.error(ar.cause()));
                        }
                    });
                });

        // [更新] 自定义的交易对 -> 通用的交易对
        router.route(HttpMethod.PUT, "/market/symbol/c2g/mapping")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        ChangeSymbolMappingInput dto = body.toJsonObject().mapTo(ChangeSymbolMappingInput.class);
                        if (dto.getSource() != null && dto.getTarget() != null) {
                            configService.putC2G(dto.getSource(), dto.getTarget(), ar -> {
                                if (ar.succeeded()) {
                                    buf.response().end(R.success());
                                } else {
                                    buf.response().end(R.error(ar.cause()));
                                }
                            });
                        } else {
                            buf.response().end(R.error("交易对信息不能为空!"));
                        }
                    });
                });

        // 市场价格
        router.route(HttpMethod.GET, "/market/price/latest")
                .handler(buf -> {
                    configService.listMarketPrice(ar -> {
                        if (ar.succeeded()) {
                            buf.response().end(R.success(Mapping.toMap(ar.result())));
                        } else {
                            buf.response().end(R.error(ar.cause()));
                        }
                    });
                });

        // 市场价格v2
        router.route(HttpMethod.GET, "/market/price/latest/v2")
                .handler(buf -> {
                    configService.listMarketPrice(ar -> {
                        if (ar.succeeded()) {
                            List<Mapping> mappings = ar.result();
                            // 转换交易对
                            configService.g2cMappings(g2cRs -> {
                                if (g2cRs.succeeded()) {
                                    Map<String, String> g2cMapping = Mapping.toMap(g2cRs.result());
                                    for (Mapping mapping : mappings) {
                                        String custom = g2cMapping.get(mapping.getSource());
                                        if (StringUtil.isNullOrEmpty(custom)) {
                                            continue;
                                        }
                                        mapping.setSource(custom);
                                    }
                                    buf.response().end(Json.encodePrettily(Mapping.toMap(mappings)));
                                } else {
                                    buf.response().end(R.error(g2cRs.cause()));
                                }
                            });

                        } else {
                            buf.response().end(R.error(ar.cause()));
                        }
                    });
                });

        // 修改市场价格
        router.route(HttpMethod.PUT, "/market/price")
                .handler(buf -> {
                    buf.request().bodyHandler(body -> {
                        ChangePriceInput dto = body.toJsonObject().mapTo(ChangePriceInput.class);
                        if (dto.getSymbol() != null && dto.getPrice() != null) {
                            // 需要对交易对进行处理
                            configService.c2g(dto.getSymbol(), ar -> {
                                if (ar.succeeded()) {
                                    String generic = ar.result();
                                    if (StringUtil.isNullOrEmpty(generic)) {
                                        buf.response().end(R.error("没有配置交易对映射!"));
                                    } else {
                                        configService.updateMarketPrice(generic, dto.getPrice().doubleValue(), store -> {
                                            if (store.succeeded()) {
                                                // 生成交易数据然后推送 (影响市场价和k线)
                                                // 推送成交数据
                                                TradeMessage tm = new TradeMessage();
                                                tm.setSymbol(generic);
                                                tm.setQuantity(BigDecimal.valueOf(0.00000001));
                                                tm.setPrice(dto.getPrice());
                                                tm.setTs(System.currentTimeMillis());
                                                EventBusFactory.eventbus().publish(Topics.KLINE_TICK_TOPIC.name(),
                                                        Json.encode(new KlineTradeResp(tm)), ignored -> {
                                                        });
                                                buf.response().end(R.success());
                                            } else {
                                                buf.response().end(R.error(store.cause()));
                                            }
                                        });
                                    }
                                } else {
                                    buf.response().end(R.error(ar.cause()));
                                }
                            });
                        } else {
                            buf.response().end(R.error("交易对信息不能为空!"));
                        }
                    });
                });
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class HttpConfig {
        private String host = "localhost";
        private int port = 8087;
    }
}
