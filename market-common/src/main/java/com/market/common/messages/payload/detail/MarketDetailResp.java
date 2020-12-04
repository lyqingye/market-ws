package com.market.common.messages.payload.detail;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/10/15 上午9:33
 */
@Data
public class MarketDetailResp {

    private String ch;

    private final Long ts = System.currentTimeMillis();

    private MarketDetailTick tick;

    public static MarketDetailResp of(String ch, MarketDetailTick tick) {
        MarketDetailResp resp = new MarketDetailResp();
        resp.setCh(ch);
        resp.setTick(tick);
        return resp;
    }
}
