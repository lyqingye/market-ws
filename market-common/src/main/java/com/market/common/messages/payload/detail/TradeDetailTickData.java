package com.market.common.messages.payload.detail;

import lombok.Data;

/**
 * https://huobiapi.github.io/docs/spot/v1/cn/#56c6c47284-2
 *
 * @author yjt
 * @since 2020/10/15 下午6:49
 */
@Data
public class TradeDetailTickData {
    private String amount;

    private String price;

    private Long ts;

    private String direction;
}
