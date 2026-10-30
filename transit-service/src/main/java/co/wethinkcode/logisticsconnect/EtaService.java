package co.wethinkcode.logisticsconnect;

import java.io.IOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EtaService {

    public interface HubSource {
        Optional<Hub> find(String hubId) throws IOException;
    }

    public static class UnknownHubException extends RuntimeException {
        public UnknownHubException(String hubId) {
            super("Unknown hub: " + hubId);
        }
    }

    public static class HubInactiveException extends RuntimeException {
        public HubInactiveException(String hubId) {
            super("Hub " + hubId + " is inactive and cannot receive parcels");
        }
    }

    private final HubSource hubs;
    private final DelayStageSource stages;
    private final EtaCalculator calculator;
    private final Clock clock;

    public EtaService(HubSource hubs, DelayStageSource stages, EtaCalculator calculator, Clock clock) {
        this.hubs = hubs;
        this.stages = stages;
        this.calculator = calculator;
        this.clock = clock;
    }

    public Map<String, Object> estimate(String hubId) throws IOException {
        Hub hub = hubs.find(hubId).orElseThrow(() -> new UnknownHubException(hubId));
        if (Boolean.FALSE.equals(hub.active())) {
            throw new HubInactiveException(hub.id());
        }
        int stage = stages.stageFor(hub.id());
        EtaCalculator.Window window = calculator.calculate(clock.instant().truncatedTo(ChronoUnit.SECONDS), stage);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hubId", hub.id());
        body.put("sortingCenter", hub.sortingCenter());
        body.put("province", hub.province());
        body.put("delayStage", stage);
        body.put("delayStageSource", stages.name());
        body.put("earliestArrival", window.earliest().toString());
        body.put("latestArrival", window.latest().toString());
        return body;
    }
}
