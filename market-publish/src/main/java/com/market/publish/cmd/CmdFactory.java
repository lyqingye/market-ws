package com.market.publish.cmd;

import com.market.common.utils.RequestUtils;
import com.market.publish.cmd.impl.depth.MarketDepthProcessCmd;
import com.market.publish.cmd.impl.depth.MarketDepthSubCmd;
import com.market.publish.cmd.impl.detail.MarketDetailSubCmd;
import com.market.publish.cmd.impl.kline.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author yjt
 * @since 2020/11/14 18:21
 */
public class CmdFactory {

    private static Cmd[] WS_CMD_ARRAY;

    private static Cmd[] TOPIC_CMD_ARRAY;

    static {
        WS_CMD_ARRAY = new Cmd[]{
                new KlinePullCmd(),
                new KlineSubCmd(),
                new TradeDetailSubCmd(),
                new MarketDepthSubCmd(),
                new MarketDetailSubCmd(),
                new KlineUnSubCmd()
        };

        TOPIC_CMD_ARRAY = new Cmd[]{
                new KlineTickProcessCmd(),
                new TradeDetailProcessCmd(),
                new MarketDepthProcessCmd()
        };
    }

    public static Cmd createForWs(JsonObject json) {
        // 忽略心跳
        if (!RequestUtils.isPongReq(json)) {
            for (Cmd cmd : WS_CMD_ARRAY) {
                if (cmd.canExecute(json)) {
                    return cmd;
                }
            }
        }
        return null;
    }

    public static Cmd createForWs(JsonArray jsonArray) {
        for (Cmd cmd : WS_CMD_ARRAY) {
            if (cmd.canExecute(jsonArray)) {
                return cmd;
            }
        }
        return null;
    }

    public static Cmd createForTopic(JsonObject json) {
        for (Cmd cmd : TOPIC_CMD_ARRAY) {
            if (cmd.canExecute(json)) {
                return cmd;
            }
        }
        return null;
    }

    public static Cmd createForTopic(JsonArray jsonArray) {
        for (Cmd cmd : TOPIC_CMD_ARRAY) {
            if (cmd.canExecute(jsonArray)) {
                return cmd;
            }
        }
        return null;
    }
}
