package com.market.common.def;

/**
 * @author yjt
 * @since 2020/10/11 12:40
 */
public enum DepthLevel {
    step0,
    step1,
    step2,
    step3,
    step4,
    step5;

    public static DepthLevel of(int level) {
        for (DepthLevel v : values()) {
            if (v.ordinal() == level) {
                return v;
            }
        }
        return null;
    }
}
