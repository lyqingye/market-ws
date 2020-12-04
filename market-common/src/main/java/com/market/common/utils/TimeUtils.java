package com.market.common.utils;

public class TimeUtils {


    public static long alignWithPeriod(long t, long p) {
        return t - t % p;
    }
}
