package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import io.javalin.Javalin;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;

public class TransitServiceApp {

    public static void main(String[] args) {
        String hubUrl = System.getenv().getOrDefault("HUB_SERVICE_URL", "http://localhost:7051");

        StageCache stageCache = new StageCache();
        StageSubscriber subscriber = new StageSubscriber(MqConfig.BROKER_URL, stageCache);
        subscriber.start();
        Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));

        EtaService etaService = new EtaService(new HubClient(hubUrl), stageCache, new EtaCalculator(), Clock.systemUTC());

        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Calculates estimated arrival windows based on hub and delay stage.)
        // Add domain endpoints for transit-service here.

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/eta/{hubId}", ctx -> {
            try {
                ctx.json(etaService.estimate(ctx.pathParam("hubId")));
            } catch (EtaService.UnknownHubException e) {
                ctx.status(404).json(Map.of("error", e.getMessage()));
            } catch (EtaService.HubInactiveException e) {
                ctx.status(409).json(Map.of("error", e.getMessage()));
            } catch (IOException e) {
                ctx.status(502).json(Map.of("error", "Upstream service unavailable: " + e.getMessage()));
            }
        });

        // GET /stages -> what the MQ subscription has taught us so far, e.g. {"H-501": 5}
        app.get("/stages", ctx -> ctx.json(stageCache.snapshot()));

    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
