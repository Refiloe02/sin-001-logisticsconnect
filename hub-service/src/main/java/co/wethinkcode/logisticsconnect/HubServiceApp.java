package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;
import java.util.List;
import java.util.Map;

/**
 * Stage 2: serves provinces and sorting centres. Its data comes from ingestion-service's cleaned
 * output over REST, so this service never parses the messy CSV itself.
 */
public class HubServiceApp {

    private static final String INGESTION_URL = System.getenv().getOrDefault("INGESTION_URL", "http://localhost:7050");
    private static final IngestionClient INGESTION = new IngestionClient(INGESTION_URL);

    private static volatile HubCatalog catalog;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7051);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/hubs", ctx -> withCatalog(ctx, c -> {
            String province = ctx.queryParam("province");
            ctx.json(province == null ? c.all() : c.inProvince(province));
        }));

        app.get("/hubs/{hubId}", ctx -> withCatalog(ctx, c -> {
            String hubId = ctx.pathParam("hubId");
            c.find(hubId).ifPresentOrElse(
                    ctx::json,
                    () -> ctx.status(404).json(Map.of("error", "Unknown hub: " + hubId)));
        }));

        app.get("/provinces", ctx -> withCatalog(ctx, c -> ctx.json(c.provinces())));

        app.post("/refresh", ctx -> {
            if (tryLoad()) {
                ctx.json(Map.of("hubs", catalog.all().size()));
            } else {
                ctx.status(503).json(Map.of("error", "ingestion-service unavailable at " + INGESTION_URL));
            }
        });

        tryLoad();
    }

    private static boolean tryLoad() {
        try {
            List<Hub> hubs = INGESTION.fetchHubs();
            catalog = new HubCatalog(hubs);
            System.out.println("hub-service loaded " + hubs.size() + " hubs from " + INGESTION_URL);
            return true;
        } catch (Exception e) {
            System.err.println("hub-service could not load hubs from " + INGESTION_URL + ": " + e.getMessage());
            return false;
        }
    }

    private interface CatalogHandler {
        void handle(HubCatalog catalog) throws Exception;
    }

    private static void withCatalog(io.javalin.http.Context ctx, CatalogHandler handler) throws Exception {
        if (catalog == null && !tryLoad()) {
            ctx.status(503).json(Map.of("error", "hub data not available: ingestion-service unreachable"));
            return;
        }
        handler.handle(catalog);
    }
}
