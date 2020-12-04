package com.market.common.messages.payload.kline;

import com.market.common.def.Period;
import com.market.common.messages.bridge.TradeMessage;
import com.market.common.utils.RequestUtils;
import lombok.Data;

@Data
public class KlineTradeResp {
    private String ch;

    private Long ts;

    private KlineTickResp tick;

    public KlineTradeResp() {
    }

    public KlineTradeResp(String ch, KlineTickResp tick) {
        this.ch = ch;
        this.tick = tick;
        this.ts = System.currentTimeMillis();
    }

    public KlineTradeResp(TradeMessage msg) {
        this.setCh(RequestUtils.toKlineSub(msg.getSymbol(), Period._1_MIN));
        this.setTs(System.currentTimeMillis());
        KlineTickResp tick = new KlineTickResp();
        this.setTick(tick);
        tick.setCount(1);
        tick.setAmount(msg.getQuantity());
        tick.setHigh(msg.getPrice());
        tick.setLow(msg.getPrice());
        tick.setOpen(msg.getPrice());
        tick.setClose(msg.getPrice());
        tick.setId(msg.getTs() / 1000);
        tick.setVol(msg.getPrice().multiply(msg.getQuantity()));
    }
}
