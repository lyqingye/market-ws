import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


public class SubscribeTest {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();


        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", "config.yaml"));

        ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");


        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx,options);

        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                System.out.println(ar.result().encodePrettily());
            }
        });
    }
}
