package com.market.bridge.impl.http.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.Json;
import lombok.Data;

/**
 * @author yjt
 * @since 2020/10/13 下午5:38
 */
@Data
public class R<T> {

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private T data;

    private boolean success;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private String message;

    public static <E> String success () {
        R<E> r = new R<>();
        r.setSuccess(true);
        r.setData(null);
        return Json.encodePrettily(r);
    }

    public static <E> String success (E data) {
        R<E> r = new R<>();
        r.setSuccess(true);
        r.setData(data);
        return Json.encodePrettily(r);
    }

    public static <E> String error (String message) {
        R<E> r = new R<>();
        r.setSuccess(false);
        r.setMessage(message);
        return Json.encodePrettily(r);
    }
    public static <E> String error (Throwable throwable) {
        R<E> r = new R<>();
        r.setSuccess(false);
        r.setMessage(throwable.getMessage());
        return Json.encodePrettily(r);
    }

}
