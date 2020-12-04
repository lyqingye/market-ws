package com.market.common.messages.payload.kline;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/9/28 上午9:20
 */
@Data
public class KlineUnSubscribeResp {
    private String id;

    private String status;

    private String unsubbed;

    private Long ts;
}
