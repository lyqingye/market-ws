package com.market.publish.cmd.impl.depth;

import com.market.common.session.WsSessionWrapper;
import com.market.common.utils.GZIPUtils;
import com.market.publish.cmd.Cmd;
import com.market.publish.context.PublishContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.HashMap;

public class MarketDepthProcessCmd implements Cmd {
    @Override
    public boolean canExecute(JsonObject json) {
        return false;
    }

    @Override
    public void execute(JsonObject json, PublishContext ctx, WsSessionWrapper curSession) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canExecute(JsonArray jsonArray) {
        if (jsonArray.isEmpty()) {
            return false;
        }
        String ch = jsonArray.getJsonObject(0).getString("ch");
        if (ch == null) {
            return false;
        }
        return ch.contains("depth");
    }

    @Override
    public void execute(JsonArray jsonArray, PublishContext ctx, WsSessionWrapper curSession) {
        HashMap<String, Buffer> sendBuffer = new HashMap<>(6);
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject json = jsonArray.getJsonObject(i);
            String sub = json.getString("ch");
            Buffer buffer = null;
            try {
                buffer = Buffer.buffer(GZIPUtils.compress(json.toBuffer().getBytes()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (buffer != null){
                // 更新到缓存
                ctx.getDepthChartCache().put(sub, buffer);
                // 记录需要推送的数据
                sendBuffer.put(sub, buffer);
            }
        }
        if (sendBuffer.isEmpty()) {
            return;
        }
        // 推送深度图
        ctx.getDepthSM().values()
                .forEach(wrapper -> {
                    sendBuffer.forEach((sub, buffer) -> {
                        if (wrapper.isSubDepthSub(sub)) {
                            wrapper.getSocket().write(buffer);
                        }
                    });
                });
    }
}
