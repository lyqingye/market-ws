package com.market.common.ds;

import com.market.common.def.Period;
import com.market.common.ds.cmd.CmdResult;
import com.market.common.ds.cmd.PollTicksCmd;
import com.market.common.ds.cmd.UpdateTickCmd;
import com.market.common.messages.payload.detail.MarketDetailResp;
import com.market.common.messages.payload.detail.MarketDetailTick;
import com.market.common.messages.payload.kline.KlineTickResp;
import com.market.common.messages.payload.kline.KlineTradeResp;
import com.market.common.utils.TimeUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.market.common.utils.TimeUtils.*;

public class KlineTimeLine {

    /**
     * 时间轮创建的时间
     */
    private long tt;

    /**
     * 时间轮创建时头部对应的时间
     */
    private long ht;

    /**
     * 时间轮存放的具体数据
     */
    private Object[] data;

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

    /**
     * 统计项
     */
    private boolean autoAggregate = false;
    private BigDecimal high = BigDecimal.ZERO,low = BigDecimal.ZERO,vol = BigDecimal.ZERO,open = BigDecimal.ZERO,close = BigDecimal.ZERO,amount = BigDecimal.ZERO;
    private int count = 0;

    /**
     * 命令队列
     */
    private ConcurrentLinkedQueue<Object> cmdBuffer = new ConcurrentLinkedQueue<Object>();

    public KlineTimeLine (long period, int numOfPeriod, boolean autoAggregate) {
        this.period = period;
        this.numOfPeriod = numOfPeriod;
        this.totalPeriodSize = period * numOfPeriod;
        long now = System.currentTimeMillis();
        this.tt = alignWithPeriod(now, period);
        this.data = new Object[numOfPeriod];
        this.ht = tt - totalPeriodSize + period;
        this.autoAggregate = autoAggregate;
    }

    public CmdResult<KlineTickResp> update (KlineTickResp tick) {
        UpdateTickCmd cmd = new UpdateTickCmd();
        cmd.setTick(tick);
        cmdBuffer.offer(cmd);
        return cmd.getResult();
    }

    public CmdResult<List<KlineTickResp>> poll (long start, long end, int partIdx) {
        PollTicksCmd cmd = new PollTicksCmd();
        cmd.setEndTime(end);
        cmd.setStartTime(start);
        cmd.setPartIdx(partIdx);
        cmdBuffer.offer(cmd);
        return cmd.getResult();
    }

    public CmdResult<MarketDetailTick> tick () {
        CmdResult<MarketDetailTick> result = new CmdResult<>();
        if (execUpdateWindow()) {
            result.setSuccess(true);
            result.complete(snapAggregate());
        }else {
            result.complete(null);
        }
        Object cmd;
        while ((cmd = cmdBuffer.poll()) != null) {
            if (cmd instanceof UpdateTickCmd) {
                execUpdateTick((UpdateTickCmd) cmd);
            }else if (cmd instanceof PollTicksCmd) {
                execPollTicks((PollTicksCmd) cmd);
            }
        }
        return result;
    }

    private void execUpdateTick(UpdateTickCmd cmd) {
        KlineTickResp newObj = cmd.getTick();
        int idx = calculateIdx(newObj.getTime());
        if (idx < 0 || idx >= numOfPeriod) {
            cmd.getResult().setSuccess(false);
            cmd.getResult().complete(null);
            return;
        }
        KlineTickResp oldObj = (KlineTickResp) data[idx];
        if (oldObj != null) {
            if (alignWithPeriod(newObj.getTime(), period) != alignWithPeriod(oldObj.getTime(), period)) {
                data[idx] = newObj;
            } else {
                data[idx] = oldObj.merge(newObj);
            }
        } else {
            data[idx] = newObj;
        }
        // aggregate the window
        doAggregate(newObj);
        // complete
        cmd.getResult().setSuccess(true);
        cmd.getResult().complete((KlineTickResp) data[idx]);
    }

    private void execPollTicks(PollTicksCmd cmd) {
        int partIdx = cmd.getPartIdx();
        long startTime = cmd.getStartTime();
        long endTime = cmd.getEndTime();
        final int partSize = 300;
        int startIdx = partSize * partIdx;
        int endIdx = Math.min(startIdx + partSize, numOfPeriod);
        List<KlineTickResp> result = new ArrayList<>(partSize);
        while (startIdx < endIdx) {
            KlineTickResp obj = (KlineTickResp) this.data[startIdx];
            if (obj != null) {
                if (obj.getTime() >= startTime && obj.getTime() <= endTime) {
                    result.add(obj);
                }
            }
            startIdx++;
        }
        cmd.getResult().setSuccess(true);
        cmd.getResult().complete(result);
    }

    private boolean execUpdateWindow() {
        long now = alignWithPeriod(System.currentTimeMillis(), period);
        int roteCount = Math.toIntExact((now - tt) / period);
        if (roteCount != 0) {
            clearAggregate();
            if (roteCount < numOfPeriod) {
                int sPos = roteCount;
                int dPos = 0;
                int length = numOfPeriod - roteCount;
                for (int i = 0; i < length;i ++ ) {
                    this.data[dPos] = this.data[sPos];
                    // clear src data
                    this.data[sPos] = null;
                    doAggregate((KlineTickResp) this.data[dPos]);
                    dPos++;
                    sPos++;
                }
            } else {
                Arrays.fill(this.data, null);
            }
            ht += period * roteCount;
            tt += period * roteCount;
            return true;
        }else {
            // the window already updated
            return false;
        }
    }

    private void clearAggregate () {
        if (!autoAggregate)
            return;
        low = high = vol = open = close = amount = BigDecimal.ZERO;
        count = 0;
    }

    private void doAggregate (KlineTickResp tick) {
        if (!autoAggregate || tick == null)
            return;
        count += tick.getCount();
        amount = amount.add(tick.getAmount());
        vol = vol.add(tick.getVol());
        close = tick.getClose();
        if (open.compareTo(BigDecimal.ZERO) == 0)
            open = tick.getOpen();
        close = tick.getClose();
        if (tick.getHigh().compareTo(high) > 0)
            high = tick.getHigh();
        if (tick.getLow().compareTo(low) > 0)
            low = tick.getLow();
    }

    private MarketDetailTick snapAggregate() {
        MarketDetailTick detail = new MarketDetailTick();
        detail.setVol(vol);
        detail.setAmount(amount);
        detail.setClose(close);
        detail.setOpen(open);
        detail.setCount(count);
        detail.setHigh(high);
        detail.setLow(low);
        return detail;
    }

    private int calculateIdx(long t) {
        long at = alignWithPeriod(t, period);
        return Math.toIntExact(((at - ht) % totalPeriodSize) / period);
    }

    private String dumpData () {
        StringBuilder sb = new StringBuilder();
        for (Object obj : data) {
            KlineTickResp tick = KlineTickResp.class.cast(obj);
            if (tick != null) {
                sb.append(tick.getTime());
            }else {
                sb.append("null");
            }
            sb.append(" | ");
        }
        return sb.toString();
    }
}
