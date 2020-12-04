package com.market.common.utils;

import com.market.common.def.Period;
import io.vertx.core.Vertx;

/**
 * 定时调度工具
 *
 * @author yjt
 * @since 2020/10/10 下午3:18
 */
public final class ScheduleUtils {

    /**
     * 准点调度
     *
     * @param vertx  vertx实例
     * @param period 周期
     * @param r      runnable
     */
    public static void schedule(Vertx vertx, Period period, Runnable r) {
        long now = System.currentTimeMillis();
        long nowAligned = TimeUtils.alignWithPeriod(now, period.getMill());
        long next = period.getMill() - (now - nowAligned);
        vertx.setTimer(next, (ignored) -> {
            r.run();
            vertx.setPeriodic(period.getMill(), (tid) -> {
                r.run();
            });
        });
    }
}
