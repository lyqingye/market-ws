package com.market.common.messages.payload.kline;

import lombok.Data;

/**
 * @author yjt
 * @since 2020/9/28 上午9:20
 */
@Data
public class KlineTemplateResp<T> {

    /**
     * OK状态
     */
    public static final String RESPONSE_STATUS_OK = "ok";

    private String id;

    private String status;

    private String rep;

    /**
     * 返回的数据
     */
    private T data;

    public static <E> KlineTemplateResp<E> ok(String id, String rep, E data) {
        KlineTemplateResp<E> t = new KlineTemplateResp<>();
        t.setStatus(RESPONSE_STATUS_OK);
        t.setId(id);
        t.setRep(rep);
        t.setData(data);
        return t;
    }
}
