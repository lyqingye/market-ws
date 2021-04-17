package com.market.common.ds.cmd;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.CompletableFuture;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmdResult<T> extends CompletableFuture<T> {
    private boolean success;
}
