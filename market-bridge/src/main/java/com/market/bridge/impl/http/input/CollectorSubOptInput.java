package com.market.bridge.impl.http.input;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CollectorSubOptInput extends CollectorOperateInput{
    /**
     * 交易对
     */
    private String symbol;
}
