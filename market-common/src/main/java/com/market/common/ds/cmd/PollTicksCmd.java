package com.market.common.ds.cmd;

import com.market.common.messages.payload.detail.TradeDetailTick;
import com.market.common.messages.payload.kline.KlineTickResp;
import lombok.Data;

import java.util.List;

@Data
public class PollTicksCmd {
    private CmdResult<List<KlineTickResp>> result = new CmdResult<>();
    private long startTime, endTime;
    private int partIdx;
}
