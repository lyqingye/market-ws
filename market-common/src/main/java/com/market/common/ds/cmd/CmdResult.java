package com.market.common.ds.cmd;

import lombok.Data;

import java.util.concurrent.CompletableFuture;

@Data
public class CmdResult<T> extends CompletableFuture<T> {
    private boolean success;
}
