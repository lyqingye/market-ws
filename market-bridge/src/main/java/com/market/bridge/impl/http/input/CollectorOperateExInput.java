package com.market.bridge.impl.http.input;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CollectorOperateExInput extends CollectorOperateInput {
    /**
     * json额外配置
     */
    private String jsonConfig;
}
