package com.market.common.messages.bridge;

import com.market.common.def.MessageType;
import com.market.common.def.OrderSide;
import io.vertx.core.buffer.Buffer;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author yjt
 * @since 2020/10/11 15:09
 */

@Data
public class TradeMessage {
    /**
     * 交易对
     */
    private String symbol;

    /**
     * 最终的成交价
     */
    private BigDecimal price;

    /**
     * 成交量
     */
    private BigDecimal quantity;

    /**
     * 交易时间
     */
    private Long ts;

    /**
     * 成交主动方
     */
    private String direction;

    public static Message<TradeMessage> of(TradeMessage ts) {
        final Message<TradeMessage> msg = new Message<>();
        msg.setType(MessageType.TRADE_RESULT);
        msg.setData(ts);
        return msg;
    }

    public static TradeMessage of(Buffer buf, int readOffset, int msgSize) {
        // | msg size (4byte) | msg type (1byte) | ts (8byte) |
        // | symbol.size [4byte] data[bytes] | price (8byte) | quantity (8byte) | direction (1byte) | ts (8byte)
        int offset = readOffset;
        TradeMessage ts = new TradeMessage();
        int symbolLength = buf.getInt(offset);
        if (symbolLength != msgSize - 38) {
            return null;
        }
        offset += 4;
        byte[] symbolBytes = buf.getBytes(offset, offset + symbolLength);
        offset += symbolLength;
        ts.setSymbol(new String(symbolBytes));
        ts.setPrice(BigDecimal.valueOf(buf.getDouble(offset)));
        offset += 8;
        ts.setQuantity(BigDecimal.valueOf(buf.getDouble(offset)));
        offset += 8;
        ts.setDirection(OrderSide.toSide(buf.getByte(offset)).toDirection());
        offset += 1;
        ts.setTs(buf.getLong(offset));
        return ts;
    }
}
