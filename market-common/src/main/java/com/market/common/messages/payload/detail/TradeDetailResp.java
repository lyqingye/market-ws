package com.market.common.messages.payload.detail;

import com.market.common.messages.bridge.TradeMessage;
import com.market.common.utils.RequestUtils;
import lombok.Data;

import java.util.Collections;

/**
 * https://huobiapi.github.io/docs/spot/v1/cn/#56c6c47284-2
 *
 * @author yjt
 * @since 2020/10/15 下午6:52
 */
@Data
public class TradeDetailResp {
    private String ch;

    private final Long ts = System.currentTimeMillis();

    private TradeDetailTick tick;

    public TradeDetailResp() {
    }

    public TradeDetailResp(TradeMessage msg) {
        TradeDetailTick tradeDetailTick = new TradeDetailTick();
        TradeDetailTickData tradeDetailTickData = new TradeDetailTickData();
        tradeDetailTickData.setTs(msg.getTs());
        tradeDetailTickData.setAmount(msg.getQuantity().toPlainString());
        tradeDetailTickData.setDirection(msg.getDirection());
        tradeDetailTickData.setPrice(msg.getPrice().toPlainString());
        tradeDetailTick.setData(Collections.singletonList(tradeDetailTickData));
        this.setCh(RequestUtils.toTradeDetailSub(msg.getSymbol()));
        this.setTick(tradeDetailTick);
    }
}
