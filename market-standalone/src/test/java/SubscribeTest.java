import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class SubscribeTest {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Map<String, WebSocket> socketMap = new HashMap<>();

        int counter = 0;
        while (true) {
            System.out.println("start " + counter++);
            AtomicInteger create_counter = new AtomicInteger();
            AtomicInteger close_counter = new AtomicInteger();
            socketMap.clear();
            for (int i = 0; i < 10000; i++) {
                vertx.createHttpClient().webSocket(8888,"localhost","/")
                        .onSuccess(ws -> {
                            create_counter.getAndIncrement();
                            socketMap.put(ws.textHandlerID(),ws);
                            ws.frameHandler(frame -> {
                                if (frame.isFinal() && frame.isText()) {
//                                    System.out.println(frame.textData());
                                }
                            });
                            for (int j = 0; j < 100; j++) {
                                ws.writeFinalTextFrame("ping");
                            }
                        })
                        .onFailure(err -> {
                            create_counter.getAndIncrement();
                            err.printStackTrace();
                        });
            }
            System.out.println("wait create " + counter);
            while (create_counter.get() != 10000) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (WebSocket ws : socketMap.values()) {
                ws.close(h -> {
                    close_counter.getAndIncrement();
                });
            }

            System.out.println("wait close " + counter);
            while (close_counter.get() != 10000) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
