package com.market.common.utils;

import com.hazelcast.cp.internal.operation.integration.AppendRequestOp;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
     * 解压数据
     *
     * @param data 需要解压的数据
     * @return 解压后的数据
     * @throws IOException 如果解压失败
     */
    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        decompress(bais, baos);
        baos.flush();
        baos.close();
        bais.close();
        return baos.toByteArray();
    }

    /**
     * 解压数据
     *
     * @param is 输入流
     * @param os 输出流
     * @throws IOException 如果解压失败
     */
    public static void decompress(InputStream is, OutputStream os) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(is);
        int count;
        byte[] data = new byte[gis.available()];
        while ((count = gis.read(data, 0, gis.available())) != -1) {
            os.write(data, 0, count);
        }
        gis.close();
    }

    /**
     * 压缩数据
     *
     * @param data 需要压缩的数据
     * @return 压缩后的数据
     * @throws IOException 如果压缩失败
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        gos.write(data);
        gos.finish();
        return bos.toByteArray();
    }

    /**
     * 压缩数据
     *
     * @param data 需要压缩的数据
     * @return 压缩后的数据
     * @throws IOException 如果压缩失败
     */
    public static Buffer compress(Buffer data) throws IOException {
        return Buffer.buffer(compress(data.getBytes()));
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
            try {
                promise.complete(compress(data));
            } catch (IOException e) {
                promise.fail(e);
            }
        }, handler);
    }
}
