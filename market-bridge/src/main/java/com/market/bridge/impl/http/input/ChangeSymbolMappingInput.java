package com.market.bridge.impl.http.input;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/10/13 下午3:36
 */
@Data
public class ChangeSymbolMappingInput {

    private String source;

    private String target;
}
