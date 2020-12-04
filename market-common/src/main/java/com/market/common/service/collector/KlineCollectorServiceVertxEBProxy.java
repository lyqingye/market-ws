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

package com.market.common.service.collector;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.serviceproxy.ProxyUtils;

import com.market.common.service.collector.dto.CollectorStatusDto;
import java.util.List;
import com.market.common.service.collector.KlineCollectorService;
import io.vertx.core.Vertx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/

@SuppressWarnings({"unchecked", "rawtypes"})
public class KlineCollectorServiceVertxEBProxy implements KlineCollectorService {
  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public KlineCollectorServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public KlineCollectorServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try{
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class, new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  @Override
  public  void listCollector(Handler<AsyncResult<List<CollectorStatusDto>>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "listCollector");
    _vertx.eventBus().<JsonArray>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body().stream()
          .map(o -> { if (o == null) return null;
              return o instanceof Map ? new CollectorStatusDto(new JsonObject((Map) o)) : new CollectorStatusDto((JsonObject) o);
            })
          .collect(Collectors.toList())));
      }
    });
  }
  @Override
  public  void deployCollectorEx(String collectorName, String configJson, Handler<AsyncResult<Boolean>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("collectorName", collectorName);
    _json.put("configJson", configJson);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "deployCollectorEx");
    _vertx.eventBus().<Boolean>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void deployCollector(String collectorName, Handler<AsyncResult<Boolean>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("collectorName", collectorName);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "deployCollector");
    _vertx.eventBus().<Boolean>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void unDeployCollector(String collectorName, Handler<AsyncResult<Boolean>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("collectorName", collectorName);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "unDeployCollector");
    _vertx.eventBus().<Boolean>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void startCollector(String collectorName, Handler<AsyncResult<Boolean>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("collectorName", collectorName);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "startCollector");
    _vertx.eventBus().<Boolean>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void stopCollector(String collectorName, Handler<AsyncResult<Boolean>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("collectorName", collectorName);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "stopCollector");
    _vertx.eventBus().<Boolean>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void subscribe(String collectorName, String symbol, Handler<AsyncResult<Boolean>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("collectorName", collectorName);
    _json.put("symbol", symbol);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "subscribe");
    _vertx.eventBus().<Boolean>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void unsubscribe(String collectorName, String symbol, Handler<AsyncResult<Boolean>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("collectorName", collectorName);
    _json.put("symbol", symbol);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "unsubscribe");
    _vertx.eventBus().<Boolean>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
}
