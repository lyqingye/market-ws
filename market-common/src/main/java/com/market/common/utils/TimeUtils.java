package com.market.common.utils;

/**
 * 时间工具类
 *
 * @author yjt
 */
public final class TimeUtils {

    /**
     * 时间戳按区间对齐
     *
     * @param t 需要对其的时间戳
     * @param p 区间大小
     * @return 对齐后的时间戳
     */
    public static long alignWithPeriod(long t, long p) {
        return t - t % p;
    }
}
