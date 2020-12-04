package com.market.common.messages.payload.depth;

import com.market.common.def.DepthLevel;
import com.market.common.utils.RequestUtils;
import io.vertx.core.buffer.Buffer;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yjt
 * @since 2020/10/12 上午9:34
 */
@Data
public class DepthTickResp {
    private String ch;

    private final Long ts = System.currentTimeMillis();

    private DepthTick tick;

    public static List<DepthTickResp> of(Buffer buf, int readOffset, int msgSize) {
        // | msg size (4byte) | msg type (1byte) | ts (8byte) |
        // | symbol.size [4byte] data[bytes] | numOfStep (1byte)  |
        // repeated | depth (1byte) | numOfBid (1byte) | repeated bids (32byte) |
        // | numOfAsk (1Byte) | repeated asks (32byte)
        int offset = readOffset;
        int symbolLength = buf.getInt(offset);
        if (symbolLength <= 0) {
            return null;
        }
        offset += 4;
        byte[] symbolBytes = buf.getBytes(offset, offset + symbolLength);
        offset += symbolLength;
        // get symbol
        String symbol = new String(symbolBytes);
        // num of step
        byte numOfStep = buf.getByte(offset);
        if (numOfStep > DepthLevel.values().length) {
            return null;
        }
        offset += 1;
        List<DepthTickResp> charts = new ArrayList<>(numOfStep);
        for (byte step = 0; step < numOfStep; step++) {
            DepthTickResp chart = new DepthTickResp();
            charts.add(chart);
            DepthLevel depth = DepthLevel.of(buf.getByte(offset));
            if (depth == null) {
                return null;
            }
            chart.setCh(RequestUtils.toDepthSub(symbol, depth));
            offset += 1;
            // bids
            int numOfBids = buf.getInt(offset);
            offset += 4;
            DepthTick tick = new DepthTick();
            chart.setTick(tick);
            if (numOfBids > 0) {
                String[][] bids = new String[numOfBids][];
                for (int i = 0; i < numOfBids; i++) {
                    bids[i] = buildDepth(buf, offset);
                    offset += 32;
                }
                tick.setBids(bids);
            }
            // asks
            int numOfAsks = buf.getInt(offset);
            offset += 4;
            if (numOfAsks > 0) {
                String[][] asks = new String[numOfAsks][];
                for (int i = 0; i < numOfAsks; i++) {
                    asks[i] = buildDepth(buf, offset);
                    offset += 32;
                }
                tick.setAsks(asks);
            }
            chart.setTick(tick);
        }
        return charts;
    }

    private static String[] buildDepth(Buffer buf, int readOffset) {
        String[] result = new String[3];
        int offset = readOffset;
        result[0] = BigDecimal.valueOf(buf.getDouble(offset)).toPlainString();
        offset += 8;
        BigDecimal total = BigDecimal.valueOf(buf.getDouble(offset));
        // skip executed amount
        offset += 16;
        BigDecimal leaves = BigDecimal.valueOf(buf.getDouble(offset));
        result[1] = leaves.toPlainString();
        result[2] = leaves.divide(total, RoundingMode.DOWN)
                .setScale(2, RoundingMode.DOWN).toPlainString();
        return result;
    }
}
