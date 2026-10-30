package co.wethinkcode.logisticsconnect;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

public final class DelayStageService {

    public record Update(String hubId, int stage, int previousStage, boolean changed, boolean published) {
    }

    private final DelayStageStore store;
    private final StagePublisher publisher;
    private final Clock clock;

    public DelayStageService(DelayStageStore store, StagePublisher publisher, Clock clock) {
        this.store = store;
        this.publisher = publisher;
        this.clock = clock;
    }

    public Update update(String hubId, int stage) {
        String id = DelayStageStore.normalise(hubId);
        int previous = store.set(id, stage);
        boolean changed = previous != stage;
        boolean published = false;
        if (changed) {
            StageChange message = new StageChange(id, stage, clock.instant().truncatedTo(ChronoUnit.SECONDS).toString());
            try {
                publisher.publish(message);
                published = true;
            } catch (Exception e) {
                System.err.println("Could not publish " + message + ": " + e.getMessage());
            }
        }
        return new Update(id, stage, previous, changed, published);
    }
}
