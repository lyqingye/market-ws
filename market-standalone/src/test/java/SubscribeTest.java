import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.market.common.utils.GZIPUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SubscribeTest {

    public static void main(String[] args) {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
        IMap<Object, Object> map = hazelcastInstance.getMap("sfaf");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            String lock = UUID.randomUUID().toString();
            map.executeOnKey(lock, new EntryProcessor() {
                @Override
                public Object process(Map.Entry entry) {
                    return null;
                }

                @Override
                public EntryBackupProcessor getBackupProcessor() {
                    return null;
                }
            });
            map.tryLock(lock);
            map.put(lock, UUID.randomUUID().toString());
            map.unlock(lock);
        }
        long end = System.currentTimeMillis();
        System.out.println(TimeUnit.MILLISECONDS.toSeconds(end - start));
    }
}
