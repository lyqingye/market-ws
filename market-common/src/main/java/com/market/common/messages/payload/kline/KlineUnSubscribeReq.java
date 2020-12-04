package com.market.common.messages.payload.kline;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/9/28 上午9:17
 */
@Data
public class KlineUnSubscribeReq {
    private String[] args;

    private String id;

    private String unsub;

    public String getUnSub() {
        return unsub;
    }

    public void setUnSub(String sub) {
        this.unsub = sub;

        if (sub == null || sub.isEmpty()) {
            args = new String[0];
        } else {
            args = sub.split("\\.");
        }
    }

    public String getSymbolId() {
        if (args.length != 4) {
            return null;
        }
        return args[1];
    }

    public String getTime() {
        if (args.length != 4) {
            return null;
        }
        return args[3];
    }
}
