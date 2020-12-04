package com.market.common.messages.parser;

import com.market.common.def.MessageType;
import com.market.common.messages.bridge.Message;
import com.market.common.messages.bridge.PriceChangeMessage;
import com.market.common.messages.bridge.TradeMessage;
import com.market.common.messages.payload.depth.DepthTickResp;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

import java.util.List;

/**
 * @author ex
 */
public class FrameParser {

    private Buffer _buffer;
    private int _offset;

    public void handle(Buffer buffer, Handler<AsyncResult<Message<?>>> client) {
        append(buffer);

        int offset;

        while (true) {
            // set a rewind point. if a failure occurs,
            // wait for the next handle()/append() and try again
            offset = _offset;

            // how many bytes are in the buffer
            int remainingBytes = bytesRemaining();

            // at least 4 bytes
            if (remainingBytes < 4) {
                break;
            }

            // what is the length of the message
            int length = _buffer.getInt(_offset);
            _offset += 4;

            if (remainingBytes - 4 >= length) {
                // we have a complete message
                int readOffset = _offset;
                Message<Object> rs = new Message<>();
                MessageType msgType = MessageType.valueOf(_buffer.getByte(readOffset));
                readOffset += 1;
                if (msgType != null) {
                    rs.setType(msgType);
                    rs.setTs(_buffer.getLong(readOffset));
                    readOffset += 8;
                    // read message data
                    switch (msgType) {
                        case TRADE_RESULT: {
                            TradeMessage msg = TradeMessage.of(_buffer, readOffset, length);
                            if (msg == null) {
                                client.handle(Future.failedFuture("invalid trade symbol length"));
                            }
//                            System.out.println(Json.encode(msg));
                            rs.setData(msg);
                            client.handle(Future.succeededFuture(rs));
                            break;
                        }
                        case DEPTH_CHART: {
                            List<DepthTickResp> msg = DepthTickResp.of(_buffer, readOffset, length);
                            if (msg == null) {
                                client.handle(Future.failedFuture("invalid depth msg"));
                            }
//                            System.out.println(Json.encode(msg.get(0)));
                            rs.setData(msg);
                            client.handle(Future.succeededFuture(rs));
                            break;
                        }
                        case MARKET_PRICE: {
                            PriceChangeMessage msg = PriceChangeMessage.of(_buffer, readOffset, length);
                            if (msg == null) {
                                client.handle(Future.failedFuture("invalid price change msg"));
                            }
//                            System.out.println(Json.encode(msg));
                            rs.setData(msg);
                            client.handle(Future.succeededFuture(rs));
                            break;
                        }
                        default: {
                            // ignored
                        }
                    }
                } else {
                    client.handle(Future.failedFuture("invalid msg type"));
                }
                // next package
                _offset += length;
            } else {
                // not enough data: rewind, and wait
                // for the next packet to appear
                _offset = offset;
                break;
            }
        }
    }

    private void append(Buffer newBuffer) {
        if (newBuffer == null) {
            return;
        }

        // first run
        if (_buffer == null) {
            _buffer = newBuffer;

            return;
        }

        // out of data
        if (_offset >= _buffer.length()) {
            _buffer = newBuffer;
            _offset = 0;

            return;
        }

        // very large packet
        if (_offset > 0) {
            _buffer = _buffer.getBuffer(_offset, _buffer.length());
        }
        _buffer.appendBuffer(newBuffer);

        _offset = 0;
    }

    private int bytesRemaining() {
        return (_buffer.length() - _offset) < 0 ? 0 : (_buffer.length() - _offset);
    }
}
