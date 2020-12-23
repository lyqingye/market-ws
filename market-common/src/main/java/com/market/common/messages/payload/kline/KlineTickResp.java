package com.market.common.messages.payload.kline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.market.common.ds.TimeWheelSlotData;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.Data;

import java.math.BigDecimal;

@Data
@DataObject
public class KlineTickResp implements TimeWheelSlotData {
    private Long id;

    private BigDecimal amount;

    private Integer count;

    private BigDecimal open;

    private BigDecimal close;

    private BigDecimal low;

    private BigDecimal high;

    private BigDecimal vol;

    public KlineTickResp() {
    }

    public KlineTickResp(JsonObject json) {
        final KlineTickResp tick = json.mapTo(KlineTickResp.class);
        this.id = tick.id;
        this.open = tick.open;
        this.amount = tick.amount;
        this.count = tick.count;
        this.close = tick.close;
        this.low = tick.low;
        this.high = tick.high;
        this.vol = tick.vol;
    }

    /**
     * 当前当前数据槽对应的时间
     *
     * @return 单位 mill
     */
    @Override
    @JsonIgnore
    public long getTime() {
        return id * 1000;
    }


    @Override
    public KlineTickResp clone () {
        KlineTickResp tick = new KlineTickResp();
        tick.id = this.id;
        tick.amount = this.amount;
        tick.vol = this.vol;
        tick.high = this.high;
        tick.low = this.low;
        tick.open = this.open;
        tick.close = this.close;
        tick.count = this.count;
        return tick;
    }

    /**
     * 合并两个数据槽的数据
     *
     * @param target 目标
     * @return 合并后的数据
     */
    @Override
    public TimeWheelSlotData merge(TimeWheelSlotData target) {
        KlineTickResp tick = (KlineTickResp) target;

        this.count += tick.getCount();
        this.amount = this.amount.add(tick.getAmount());
        this.vol = this.vol.add(tick.getVol());
        this.close = tick.close;

        if (tick.high.compareTo(this.high) > 0) {
            this.high = tick.high;
        }
        if (tick.low.compareTo(this.low) < 0) {
            this.low = tick.low;
        }
        return this;
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }
}
