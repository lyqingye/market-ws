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

package com.market.common.service.config;

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

import java.util.List;
import io.vertx.core.Vertx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import com.market.common.service.config.ConfigService;
import com.market.common.service.config.dto.Mapping;
/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/

@SuppressWarnings({"unchecked", "rawtypes"})
public class ConfigServiceVertxEBProxy implements ConfigService {
  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public ConfigServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public ConfigServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try{
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class, new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  @Override
  public  void putC2G(String custom, String generic, Handler<AsyncResult<Void>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("custom", custom);
    _json.put("generic", generic);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "putC2G");
    _vertx.eventBus().<Void>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void putG2C(String generic, String custom, Handler<AsyncResult<Void>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("generic", generic);
    _json.put("custom", custom);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "putG2C");
    _vertx.eventBus().<Void>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void c2g(String custom, Handler<AsyncResult<String>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("custom", custom);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "c2g");
    _vertx.eventBus().<String>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void g2c(String generic, Handler<AsyncResult<String>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("generic", generic);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "g2c");
    _vertx.eventBus().<String>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void c2gMappings(Handler<AsyncResult<List<Mapping>>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "c2gMappings");
    _vertx.eventBus().<JsonArray>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body().stream()
          .map(o -> { if (o == null) return null;
              return o instanceof Map ? new Mapping(new JsonObject((Map) o)) : new Mapping((JsonObject) o);
            })
          .collect(Collectors.toList())));
      }
    });
  }
  @Override
  public  void g2cMappings(Handler<AsyncResult<List<Mapping>>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "g2cMappings");
    _vertx.eventBus().<JsonArray>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body().stream()
          .map(o -> { if (o == null) return null;
              return o instanceof Map ? new Mapping(new JsonObject((Map) o)) : new Mapping((JsonObject) o);
            })
          .collect(Collectors.toList())));
      }
    });
  }
  @Override
  public  void updateMarketPrice(String symbol, double price, Handler<AsyncResult<Void>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("symbol", symbol);
    _json.put("price", price);

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "updateMarketPrice");
    _vertx.eventBus().<Void>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
  @Override
  public  void listMarketPrice(Handler<AsyncResult<List<Mapping>>> handler){
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();

    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "listMarketPrice");
    _vertx.eventBus().<JsonArray>request(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body().stream()
          .map(o -> { if (o == null) return null;
              return o instanceof Map ? new Mapping(new JsonObject((Map) o)) : new Mapping((JsonObject) o);
            })
          .collect(Collectors.toList())));
      }
    });
  }
}
