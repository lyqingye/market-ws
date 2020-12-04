package com.market.common.messages.payload.detail;

import com.market.common.messages.payload.kline.KlineTickResp;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author yjt
 * @since 2020/10/14 下午7:53
 */
@Data
public class MarketDetailTick {
    /**
     * unix 时间
     */
    private final Integer id = Math.toIntExact(System.currentTimeMillis() / 1000);

    /**
     * 24小时成交量
     */
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * 24小时成交笔数
     */
    private Integer count = 0;

    /**
     * 开盘价
     */
    private BigDecimal open;

    /**
     * 收盘价
     */
    private BigDecimal close = BigDecimal.ZERO;

    /**
     * 最高价
     */
    private BigDecimal high = BigDecimal.ZERO;

    /**
     * 最低价
     */
    private BigDecimal low = BigDecimal.ZERO;

    /**
     * 成交额
     */
    private BigDecimal vol = BigDecimal.ZERO;

    public MarketDetailTick() {
    }

    /**
     * 累加
     *
     * @param ticks 交易tick {@link KlineTickResp}
     */
    public MarketDetailTick(Object[] ticks) {
        for (Object o : ticks) {
            KlineTickResp t = (KlineTickResp) o;
            if (o != null) {
                if (this.open == null) {
                    this.open = t.getOpen();
                }
                this.close = t.getClose();
                if (t.getHigh().compareTo(this.high) > 0) {
                    this.high = t.getHigh();
                }
                if (this.low.compareTo(BigDecimal.ZERO) == 0) {
                    this.low = t.getLow();
                } else {
                    if (t.getLow().compareTo(this.low) < 0) {
                        this.low = t.getLow();
                    }
                }
                this.count = t.getCount() + this.count;
                this.vol = t.getVol().add(this.vol);
                this.amount = t.getAmount().add(this.amount);
            }
        }
    }
}
