package com.market.common.ds.cmd;

import com.market.common.messages.payload.kline.KlineTickResp;
import lombok.Data;

@Data
public class UpdateTickCmd {
    private CmdResult<KlineTickResp> result = new CmdResult<>();
    private KlineTickResp tick;
}
