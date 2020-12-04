package com.market.common.messages.payload.kline;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/9/28 上午9:18
 */
@Data
public class KlinePingResp {
    private long ping;

    public static KlinePingResp ping() {
        KlinePingResp ping = new KlinePingResp();
        ping.setPing(System.currentTimeMillis());
        return ping;
    }
}
