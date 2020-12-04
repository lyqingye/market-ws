package com.market.common.session;

import io.netty.util.DefaultAttributeMap;
import io.vertx.core.net.NetSocket;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yjt
 * @since 2020/10/11 19:17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TcpSessionWrapper extends DefaultAttributeMap {
    /**
     * 会话
     */
    private NetSocket socket;

    public static TcpSessionWrapper of(NetSocket socket) {
        final TcpSessionWrapper wrapper = new TcpSessionWrapper();
        wrapper.setSocket(socket);
        return wrapper;
    }
}
