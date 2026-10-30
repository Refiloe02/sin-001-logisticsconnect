package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.io.InputStream;
import java.util.Map;

public class IngestionServiceApp {

    public static void main(String[] args) throws Exception {
        IngestionResult result = load();

        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        app.get("/hubs", ctx -> ctx.json(result.hubs()));

        app.get("/hubs/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");
            result.find(hubId).ifPresentOrElse(
                    ctx::json,
                    () -> ctx.status(404).json(Map.of("error", "Unknown hub: " + hubId)));
        });

        app.get("/report", ctx -> ctx.json(result.report()));
    }

    static IngestionResult load() throws Exception {
        try (InputStream in = IngestionServiceApp.class.getResourceAsStream("/hubs-global.csv")) {
            if (in == null) {
                throw new IllegalStateException("hubs-global.csv not found on the classpath");
            }
            return IngestionPipeline.run(HubCsvReader.read(in));
        }

    }
}
