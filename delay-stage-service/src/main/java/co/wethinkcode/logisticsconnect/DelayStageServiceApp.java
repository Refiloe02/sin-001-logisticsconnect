package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;
import java.time.Clock;
import java.util.Map;

public class DelayStageServiceApp {

    public static void main(String[] args) {

        DelayStageStore store = new DelayStageStore();
        ActiveMqStagePublisher publisher = new ActiveMqStagePublisher();
        DelayStageService service = new DelayStageService(store, publisher, Clock.systemUTC());
        Runtime.getRuntime().addShutdownHook(new Thread(publisher::close));

        Javalin app = Javalin.create().start(7052);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Tracks the Transit Delay Stage (0-8, e.g. weather shutdowns).)
        // Add domain endpoints for delay-stage-service here.

        app.get("/delay-stage", ctx -> ctx.json(store.all()));

        app.get("/delay-stage/{hubId}", ctx -> {
            String hubId = DelayStageStore.normalise(ctx.pathParam("hubId"));
            ctx.json(Map.of("hubId", hubId, "stage", store.get(hubId)));
        });

        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = DelayStageStore.normalise(ctx.pathParam("hubId"));
            StageRequest request;
            try {
                request = ctx.bodyAsClass(StageRequest.class);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Body must be JSON like {\"stage\": 3}"));
                return;
            }
            if (request == null || request.stage() == null) {
                ctx.status(400).json(Map.of("error", "Missing field: stage"));
                return;
            }
            DelayStageService.Update update;
            try {
                update = service.update(hubId, request.stage());
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
                return;
            }
            ctx.json(update);
        });
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
