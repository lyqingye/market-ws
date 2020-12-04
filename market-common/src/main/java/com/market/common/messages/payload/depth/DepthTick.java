package com.market.common.messages.payload.depth;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/10/12 上午9:18
 */
@Data
public class DepthTick {

    private static final String[][] EMPTY = new String[0][0];

    private String[][] bids = EMPTY;

    private String[][] asks = EMPTY;

    private Long ts;
}
