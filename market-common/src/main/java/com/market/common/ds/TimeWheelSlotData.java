package com.market.common.ds;

/**
 * @author yjt
 * @since 2020/9/29 上午10:19
 */
public interface TimeWheelSlotData {

    /**
     * 当前当前数据槽对应的时间
     *
     * @return 单位 mill
     */
    long getTime();

    /**
     * 合并两个数据槽的数据
     *
     * @param target 目标
     * @return 合并后的数据
     */
    TimeWheelSlotData merge(TimeWheelSlotData target);
}
