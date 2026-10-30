package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class SimulatedSocialPoster implements SocialPoster {

    private final String webhookUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public SimulatedSocialPoster(String webhookUrl) {
        this.webhookUrl = (webhookUrl == null || webhookUrl.isBlank()) ? null : webhookUrl;
    }

    @Override
    public void post(Alert alert) {
        System.out.println("[SIMULATED POST] " + alert.message());
        if (webhookUrl == null) {
            return;
        }
        try {
            String body = mapper.writeValueAsString(Map.of("text", alert.message()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[WEBHOOK] " + webhookUrl + " answered HTTP " + response.statusCode());
        } catch (Exception e) {
            System.err.println("[WEBHOOK] failed: " + e.getMessage());
        }
    }
}
