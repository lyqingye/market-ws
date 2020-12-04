package com.market.bridge.impl.http.input;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author yjt
 * @since 2020/10/13 下午8:11
 */
@Data
public class ChangePriceInput {

    private String symbol;

    private BigDecimal price;
}
