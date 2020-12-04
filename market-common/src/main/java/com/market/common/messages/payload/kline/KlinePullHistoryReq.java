package com.market.common.messages.payload.kline;

/**
 * @author yjt
 * @since 2020/9/28 上午9:15
 */
public class KlinePullHistoryReq {
    private String id;
    private String req;
    private Long from;
    private Long to;

    private String[] args;

    public int getPartIdx() {
        return id.charAt(id.length() - 1) - '0';
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getTo() {
        return to;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public String getReq() {
        return req;
    }

    public void setReq(String req) {
        this.req = req;
        if (req == null || req.isEmpty()) {
            args = new String[0];
        } else {
            args = req.split("\\.");
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
