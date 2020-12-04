package com.market.common.messages.payload.kline;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/9/28 上午9:19
 */
@Data
public class KlineSubscribeResp {

    private String id;

    private String status;

    private String subbed;

    private Long ts;
}
