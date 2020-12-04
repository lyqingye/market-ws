package com.market.common.messages.payload.common;

import lombok.Getter;
import lombok.Setter;

/**
 * @author yjt
 * @since 2020/9/28 上午9:17
 */
public class SubscribeReq {
    private String[] args;

    private String sub;

    @Getter
    @Setter
    private String id;

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;

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

    public boolean isKline() {
        if (args.length != 4) {
            return false;
        }
        return "kline".equalsIgnoreCase(args[2]);
    }

    public boolean isDepth() {
        if (args.length != 4) {
            return false;
        }
        return "depth".equalsIgnoreCase(args[2]);
    }

    public boolean isDetail() {
        if (args.length != 3) {
            return false;
        }
        return "detail".equalsIgnoreCase(args[2]);
    }

    public int getDepth() {
        String step = args[3];

        if ("step0".equalsIgnoreCase(step)) {
            return 0;
        }
        if ("step1".equalsIgnoreCase(step)) {
            return 1;
        }
        if ("step2".equalsIgnoreCase(step)) {
            return 2;
        }
        if ("step3".equalsIgnoreCase(step)) {
            return 3;
        }
        if ("step4".equalsIgnoreCase(step)) {
            return 4;
        }
        if ("step5".equalsIgnoreCase(step)) {
            return 5;
        }
        return -1;
    }

    public String getTime() {
        if (args.length != 4) {
            return null;
        }
        return args[3];
    }
}
