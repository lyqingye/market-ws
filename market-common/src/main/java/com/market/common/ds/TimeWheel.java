package com.market.common.ds;

import com.market.common.utils.TimeUtils;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 时间轮
 *
 * @author ex
 */
@SuppressWarnings("unchecked")
public class TimeWheel<T extends TimeWheelSlotData> {

    /**
     * 时间轮创建的时间
     */
    private volatile long tt;

    /**
     * 时间轮创建时头部对应的时间
     */
    private volatile long ht;

    /**
     * 时间轮存放的具体数据
     */
    private volatile Object[] data;

    /**
     * 周期大小
     */
    private final long period;

    /**
     * 周期数
     */
    private final int numOfPeriod;

    /**
     * 总周期
     */
    private final long totalPeriodSize;

    public TimeWheel(long period, int numOfPeriod, Collection<T> slots) {
        this.period = period;
        this.numOfPeriod = numOfPeriod;
        totalPeriodSize = period * numOfPeriod;

        long now = System.currentTimeMillis();
        //
        // 创建时间 = 当前时间按照周期大小对齐
        // 例:
        // 当前时间 = 2020.09.27 07:27:42
        // 周期大小 = 1min (6000ms)
        // 那么对齐后的时间 = 2020.09.27 07:27:00
        tt = TimeUtils.alignWithPeriod(now, period);

        data = new Object[numOfPeriod];
        // 时间轮头部对应的时间
        ht = tt - totalPeriodSize + period;

        // 初始化数据
        this.fill(slots);
    }

    public long getNow() {
        return this.tt;
    }

    public long getPeriod() {
        return this.period;
    }

    /**
     * 设置值
     *
     * @param slot 数据
     * @return 设置后的值
     */
    public T updateNow(T slot) {
        AtomicReference<T> newObj = new AtomicReference<>(slot);
        this.rollToNow(() -> {
            int idx = calculateIdx(slot.getTime());
            if (idx < 0 || idx >= numOfPeriod) {
                return;
            }
            T oldObj = (T) data[idx];
            if (oldObj != null) {
                if (TimeUtils.alignWithPeriod(newObj.get().getTime(), period) != TimeUtils.alignWithPeriod(oldObj.getTime(), period)) {
                    data[idx] = newObj;
                } else {
                    newObj.set((T) oldObj.merge(slot));
                }
            } else {
                data[idx] = newObj.get();
            }
        });
        return newObj.get();
    }

    /**
     * 部分拉取
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param partIdx 1~5
     * @return 数据
     */
    public Collection<T> pull(long start, long end, int partIdx) {
        final int partSize = 300;
        int startIdx = partSize * partIdx;
        int endIdx = Math.min(startIdx + partSize, numOfPeriod - 1);
        Collection<T> result = new ArrayList<>(partSize);
        Object[] dataRef = this.data;
        while (startIdx < endIdx) {
            T obj = (T) dataRef[startIdx];
            if (obj != null) {
                if (obj.getTime() >= start && obj.getTime() <= end) {
                    result.add((T) obj);
                }
            }
            startIdx++;
        }
        return result;
    }

    /**
     * 时间轮转动一个周期
     */
    @Synchronized
    public void rollToNow(Runnable r) {
        long now = TimeUtils.alignWithPeriod(System.currentTimeMillis(), period);
        int roteCount = Math.toIntExact((now - tt) / period);

        if (roteCount != 0) {
            Object[] newData = this.data;
            if (roteCount < numOfPeriod) {
                newData = new Object[numOfPeriod];
                System.arraycopy(this.data, roteCount, newData, 0, numOfPeriod - roteCount);
            } else {
                Arrays.fill(newData, null);
            }
            this.data = newData;
            ht += period * roteCount;
            tt += period * roteCount;
        }

        if (tt != now) {
            System.out.println();
        }


        if (r != null) {
            r.run();
        }
    }

    /**
     * 转换为数组
     *
     * @return 数组
     */
    public <E> E[] toArray() {
        Object[] newData = new Object[numOfPeriod];
        System.arraycopy(this.data, 0, newData, 0, numOfPeriod);
        return (E[]) newData;
    }

    private int calculateIdx(long t) {
        long at = TimeUtils.alignWithPeriod(t, period);
        return Math.toIntExact(((at - ht) % totalPeriodSize) / period);
    }

    private void fill(Collection<T> list) {
        if (list.isEmpty()) {
            return;
        }
        long start = ht;
        int idx = 0;

        for (T t : list) {
            if (idx == numOfPeriod) {
                return;
            }
            long time = TimeUtils.alignWithPeriod(t.getTime(), period);
            if (time == start) {
                this.data[idx] = t;
            } else {
                this.data[idx] = null;
            }
            start += period;
            idx++;
        }
    }
}
