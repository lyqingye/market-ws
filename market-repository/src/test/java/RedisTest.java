import com.market.repository.RepositoryVtc;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class RedisTest {
    public static void main(String[] args) throws InterruptedException {
        final RepositoryVtc repositoryVtc = new RepositoryVtc();
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(repositoryVtc, new DeploymentOptions().setWorker(true)
                .setWorkerPoolSize(20));

//        new RedisRepo(vertx,"redis://:@localhost:6379/6")
//                .onConnect(repo -> {
//                    repo.hGetAll(CacheKey.SYMBOL_CUSTOM_TO_GENERIC.name(),ar -> {
//                        System.out.println();
//                    });
//                });

        System.out.println();

    }
}
