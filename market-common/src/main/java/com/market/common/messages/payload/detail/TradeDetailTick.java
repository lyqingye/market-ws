package com.market.common.messages.payload.detail;

import lombok.Data;

import java.util.List;

/**
 * @author yjt
 * @since 2020/10/15 下午6:58
 */
@Data
public class TradeDetailTick {

    private final Long id = System.currentTimeMillis() / 1000;
    private final Long ts = System.currentTimeMillis() / 1000;

    private List<TradeDetailTickData> data;
}
