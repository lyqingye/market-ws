package com.market.common.messages.bus;

import com.market.common.messages.payload.kline.KlineTickResp;
import lombok.Data;

/**
 * 消息总线传输k线tick的消息体
 */
@Data
public class EbKlineTickMessage {
    private String symbolId;
    private KlineTickResp tick;
}
