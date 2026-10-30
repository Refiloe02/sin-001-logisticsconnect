package co.wethinkcode.logisticsconnect;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertPolicy {

    private final int threshold;
    private final Map<String, Integer> lastStage = new ConcurrentHashMap<>();

    public AlertPolicy(int threshold) {
        this.threshold = threshold;
    }

    public int threshold() {
        return threshold;
    }

    public Optional<Alert> evaluate(String hubId, int stage, String timestamp) {
        String id = hubId.trim().toUpperCase(Locale.ROOT);
        int previous = lastStage.getOrDefault(id, 0);
        lastStage.put(id, stage);

        if (previous < threshold && stage >= threshold) {
            return Optional.of(new Alert(id, stage, "RAISED",
                    "Transit delay alert: hub " + id + " is at delay stage " + stage
                            + " of 8. Expect late parcels.", timestamp));
        }
        if (previous >= threshold && stage < threshold) {
            return Optional.of(new Alert(id, stage, "CLEARED",
                    "Update: hub " + id + " has dropped to delay stage " + stage
                            + ". Normal service is resuming.", timestamp));
        }
        return Optional.empty();
    }
}
