package com.market.publish.worker;

import com.market.common.def.Period;
import com.market.common.ds.TimeWheel;
import com.market.common.messages.payload.kline.KlineTickResp;
import com.market.common.service.config.ConfigService;
import com.market.common.service.config.dto.Mapping;
import com.market.common.service.repository.KlineRepositoryService;
import com.market.common.utils.ScheduleUtils;
import com.market.publish.context.PublishContext;
import io.vertx.core.*;
import io.vertx.core.json.Json;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class KlineWorker extends AbstractVerticle {

    /**
     * 消息推送上下文
     */
    private PublishContext ctx;

    /**
     * K线持久化服务
     */
    private KlineRepositoryService klineRepo;

    /**
     * 消息推送配置服务
     */
    private ConfigService configService;

    /**
     * 需要初始化数据的交易对数量
     */
    private int totalSymbolNeedToInit = 0;
    /**
     * 已经初始化数据的交易对数量 (至于为什么这么写, 而不是用 CountLatchDown, 你可能得问看下 netty的 event loop了)
     * <p>
     * 后面可能改成其它并行方式(rxJava? maybe), 所以用了cas
     */
    private AtomicInteger numOfInitSuccessSymbol = new AtomicInteger(0);

    public KlineWorker(PublishContext ctx) {
        this.ctx = Objects.requireNonNull(ctx);
    }

    @Override
    public void start(Promise<Void> sp) throws Exception {
        configService = ConfigService.createProxy(vertx);
        klineRepo = KlineRepositoryService.createProxy(vertx);
        // 从仓库获取历史K线数据
        configService.c2gMappings(ar -> {
            if (ar.succeeded()) {
                List<Mapping> result = ar.result();
                if (!result.isEmpty()) {
                    List<String> symbols = result.stream()
                            .map(Mapping::getTarget)
                            .collect(Collectors.toList());
                    totalSymbolNeedToInit = symbols.size();
                    // 初始化每一个交易对的数据
                    for (String symbol : symbols) {
                        this.initKlineFromRepo(symbol, initAr -> {
                            if (initAr.succeeded()) {
                                // 更新市场详情
                                ctx.updateMarketDetailTick(symbol);
                                System.out.println("[KlineWorker]: load kline ticks with: " + symbol);
                                if (numOfInitSuccessSymbol.addAndGet(1) >= totalSymbolNeedToInit) {
                                    // 通知已经初始化完毕
                                    sp.complete();
                                }
                            } else {
                                initAr.cause().printStackTrace();
                            }
                        });
                    }
                } else {
                    sp.complete();
                }
            } else {
                ar.cause().printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
    }

    /**
     * 初始化指定交易对的k线数据
     *
     * @param symbol  交易对
     * @param handler 结果处理器
     */
    @SuppressWarnings("unchecked")
    private void initKlineFromRepo(String symbol, Handler<AsyncResult<Void>> handler) {
        // 批次大小
        final int batchSize = 10;
        klineRepo.sizeOfKlineTicks(symbol, ar -> {
            if (ar.succeeded()) {
                // 计算批次数量
                int dataSize = ar.result();
                if (dataSize == 0) {
                    handler.handle(Future.succeededFuture());
                    return;
                }
                int numOfBatch = dataSize / batchSize;
                if (dataSize % batchSize != 0) {
                    numOfBatch++;
                }
                // 所有批次结果
                AtomicInteger batchCount = new AtomicInteger(0);
                // 按批次拉取
                for (int i = 0; i < numOfBatch; i++) {
                    int startIndex = i * batchSize;
                    int stopIndex = -1;
                    // 如果为最后一批则默认取完剩余即索引为 -1
                    if (i != numOfBatch - 1) {
                        stopIndex = startIndex + batchSize - 1;
                    }
                    int finalNumOfBatch = numOfBatch;
                    int curBatch = i;
                    klineRepo.listKlineTicksLimit(symbol, startIndex, stopIndex, tickRs -> {
                        if (curBatch != batchCount.get()) {
                            System.err.println("批次不等, 理论上这个不是bug! 理论上支持并发, 理论上不会出现并发");
                        }
                        if (tickRs.succeeded()) {
                            List<KlineTickResp> ticks = tickRs.result()
                                    .stream()
                                    .map((json) -> Json.decodeValue(json, KlineTickResp.class))
                                    .collect(Collectors.toList());
                            for (Period period : Period.values()) {
                                TimeWheel<KlineTickResp> timeWheel = ctx.getOrCreateTimeWheel(symbol, period);
                                for (KlineTickResp tick : ticks) {
                                    timeWheel.updateNow(tick.clone().clone());
                                }
                            }
                        }
                        // 所有批次数据已经就绪
                        if (batchCount.addAndGet(1) >= finalNumOfBatch) {
                            // 调度事件轮
                            for (Period period : Period.values()) {
                                this.scheduleTimeWheel(ctx,symbol,period);
                            }
                            handler.handle(Future.succeededFuture());
                        }
                    });
                }
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
                ar.cause().printStackTrace();
            }
        });
    }

    /**
     * 调度时间轮子
     *
     * @param ctx    上下文
     * @param symbol 交易对
     * @param period 时间区间
     */
    private void scheduleTimeWheel(PublishContext ctx, String symbol, Period period) {
        TimeWheel<KlineTickResp> timeWheel = ctx.getOrCreateTimeWheel(symbol, period);
        ScheduleUtils.schedule(vertx, period, () -> {
            timeWheel.rollToNow(null);
        });
    }
}
