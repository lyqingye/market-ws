package com.market.common.utils;

import com.hazelcast.cp.internal.operation.integration.AppendRequestOp;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP 工具类
 *
 * @author ex
 */
public class GZIPUtils {

    /**
     * 异步解压
     *
     * @param vertx vertx
     * @param data 需要解压的数据
     * @return future
     */
    public static Future<byte[]> decompressAsync(Vertx vertx,byte [] data) {
        Promise<byte[]> promise = Promise.promise();
        decompressAsync(vertx,data,promise);
        return promise.future();
    }

    /**
     * 异步解压
     *
     * @param vertx vertx
     * @param data 需要解压的数据
     * @param handler 结果处理器
     */
    public static void decompressAsync (Vertx vertx,byte[] data, Handler<AsyncResult<byte[]>> handler) {
        vertx.executeBlocking(promise -> {
            decompress(data, h -> {
                if (h.succeeded()) {
                    promise.complete(h.result());
                }else {
                    promise.fail(h.cause());
                }
            });
        }, handler);
    }

    /**
     * 解压数据
     *
     * @param data 需要解压的数据
     * @return 解压后的数据
     * @throws IOException 如果解压失败
     */
    public static void decompress(byte[] data,Handler<AsyncResult<byte[]>> handler){
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            decompress(bais, baos);
            baos.flush();
            handler.handle(Future.succeededFuture(baos.toByteArray()));
        }catch (Exception ex) {
            handler.handle(Future.failedFuture(ex));
        } finally {
            try {
                baos.close();
                bais.close();
            }catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 解压数据
     *
     * @param is 输入流
     * @param os 输出流
     * @throws IOException 如果解压失败
     */
    public static void decompress(InputStream is, OutputStream os) throws IOException {
        try(GZIPInputStream gis = new GZIPInputStream(is)){
            int count;
            byte[] data = new byte[gis.available()];
            while ((count = gis.read(data, 0, gis.available())) != -1) {
                os.write(data, 0, count);
            }
        }
    }

    /**
     * 压缩数据
     *
     * @param data 需要压缩的数据
     * @return 压缩后的数据
     * @throws IOException 如果压缩失败
     */
    public static void compress(byte[] data,Handler<AsyncResult<byte[]>> handler)  {
        try {
            handler.handle(Future.succeededFuture(compress(data)));
        } catch (IOException exception) {
            handler.handle(Future.failedFuture(exception));
        }
    }

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(data);
            gos.finish();
            gos.flush();
            gos.close();
            return bos.toByteArray();
        }
        finally {
            try {
                bos.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * 异步压缩数据
     *
     * @param vertx vertx
     * @param data 需要压缩的数据
     * @param handler 结果处理器
     */
    public static void compressAsync(Vertx vertx,Buffer data, Handler<AsyncResult<Buffer>> handler) {
        vertx.executeBlocking(promise -> {
            compress(data.getBytes(),h -> {
                if (h.succeeded()) {
                    promise.complete(Buffer.buffer(h.result()));
                }else {
                    promise.fail(h.cause());
                }
            });
        }, handler);
    }

    /**
     * 异步压缩数据
     *
     * @param vertx vertx
     * @param data 需要压缩的数据
     * @return future
     */
    public static Future<Buffer> compressAsync (Vertx vertx,Buffer data) {
        Promise<Buffer> promise = Promise.promise();
        compressAsync(vertx,data,promise);
        return promise.future();
    }
}
