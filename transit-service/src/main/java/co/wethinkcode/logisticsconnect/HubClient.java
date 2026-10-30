package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class HubClient implements EtaService.HubSource {

    private final RestClient rest = new RestClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public HubClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Optional<Hub> find(String hubId) throws IOException {
        RestClient.Reply reply = rest.get(baseUrl + "/hubs/" + URLEncoder.encode(hubId, StandardCharsets.UTF_8));
        if (reply.status() == 404) {
            return Optional.empty();
        }
        if (reply.status() != 200) {
            throw new IOException("hub-service answered HTTP " + reply.status());
        }
        return Optional.of(mapper.readValue(reply.body(), Hub.class));
    }
}
