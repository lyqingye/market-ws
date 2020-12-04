package com.market.common.service.config.dto;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yjt
 * @since 2020/11/15 12:57
 */
@Data
@DataObject
public class Mapping {

    public Mapping() {
    }

    public Mapping(JsonObject json) {
        final Mapping t = json.mapTo(Mapping.class);
        this.source = t.source;
        this.target = t.target;
    }

    public Mapping(Map.Entry<Object, Object> entry) {
        this.source = String.valueOf(entry.getKey());
        this.target = String.valueOf(entry.getValue());
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    public static Mapping of(Map.Entry<String, String> entry) {
        Mapping mapping = new Mapping();
        mapping.setSource(entry.getKey());
        mapping.setTarget(entry.getValue());
        return mapping;
    }

    public static List<Mapping> toMappings(Map<String, String> map) {
        return map.entrySet()
                .stream()
                .map(Mapping::of)
                .collect(Collectors.toList());
    }

    public static Map<String,String> toMap (List<Mapping> mappings) {
        return mappings.stream()
                .collect(Collectors.toMap(Mapping::getSource, Mapping::getTarget));
    }

    /**
     * 来源
     */
    private String source;

    /**
     * 目标
     */
    private String target;
}
