package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import io.javalin.Javalin;

import java.util.Map;

public class AlertBotApp {

    public static void main(String[] args) {

        int threshold = Integer.parseInt(System.getenv().getOrDefault("ALERT_THRESHOLD", "5"));
        String webhook = System.getenv("ALERT_WEBHOOK_URL");

        AlertBotService bot = new AlertBotService(new AlertPolicy(threshold), new SimulatedSocialPoster(webhook));
        AlertSubscriber subscriber = new AlertSubscriber(MqConfig.BROKER_URL, bot::handle, 5000);
        subscriber.start();
        Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));

        Javalin app = Javalin.create().start(7054);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Posts proactive delay notifications to public transit social media pages (simulated).)
        // Mechanism: Outbound webhook, simulated social post

        app.get("/alerts", ctx -> ctx.json(bot.history()));

        app.get("/config", ctx -> ctx.json(Map.of("threshold", bot.threshold())));

    }
}

// MQ TODO (stretch goal): subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
