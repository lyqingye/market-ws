/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.market.common.service.repository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyUtils;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;

import java.util.List;
/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/

@SuppressWarnings({"unchecked", "rawtypes"})
public class KlineRepositoryServiceVertxEBProxy implements KlineRepositoryService {
    private Vertx _vertx;
    private String _address;
    private DeliveryOptions _options;
    private boolean closed;

    public KlineRepositoryServiceVertxEBProxy(Vertx vertx, String address) {
        this(vertx, address, null);
    }

    public KlineRepositoryServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
        this._vertx = vertx;
        this._address = address;
        this._options = options;
        try {
            this._vertx.eventBus().registerDefaultCodec(ServiceException.class, new ServiceExceptionMessageCodec());
        } catch (IllegalStateException ex) {
        }
    }

    @Override
    public void sizeOfKlineTicks(String symbol, Handler<AsyncResult<Integer>> handler) {
        if (closed) {
            handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
            return;
        }
        JsonObject _json = new JsonObject();
        _json.put("symbol", symbol);

        DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
        _deliveryOptions.addHeader("action", "sizeOfKlineTicks");
        _vertx.eventBus().<Integer>request(_address, _json, _deliveryOptions, res -> {
            if (res.failed()) {
                handler.handle(Future.failedFuture(res.cause()));
            } else {
                handler.handle(Future.succeededFuture(res.result().body()));
            }
        });
    }

    @Override
    public void listKlineTicksLimit(String symbol, int start, int stop, Handler<AsyncResult<List<String>>> handler) {
        if (closed) {
            handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
            return;
        }
        JsonObject _json = new JsonObject();
        _json.put("symbol", symbol);
        _json.put("start", start);
        _json.put("stop", stop);

        DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
        _deliveryOptions.addHeader("action", "listKlineTicksLimit");
        _vertx.eventBus().<JsonArray>request(_address, _json, _deliveryOptions, res -> {
            if (res.failed()) {
                handler.handle(Future.failedFuture(res.cause()));
            } else {
                handler.handle(Future.succeededFuture(ProxyUtils.convertList(res.result().body().getList())));
            }
        });
    }

    @Override
    public void listKlineTicks(String symbol, Handler<AsyncResult<List<String>>> handler) {
        if (closed) {
            handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
            return;
        }
        JsonObject _json = new JsonObject();
        _json.put("symbol", symbol);

        DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
        _deliveryOptions.addHeader("action", "listKlineTicks");
        _vertx.eventBus().<JsonArray>request(_address, _json, _deliveryOptions, res -> {
            if (res.failed()) {
                handler.handle(Future.failedFuture(res.cause()));
            } else {
                handler.handle(Future.succeededFuture(ProxyUtils.convertList(res.result().body().getList())));
            }
        });
    }
}
