package co.wethinkcode.logisticsconnect;

import java.util.ArrayList;
import java.util.List;

public final class AlertBotService {

    private static final int MAX_HISTORY = 100;

    private final AlertPolicy policy;
    private final SocialPoster poster;
    private final List<Alert> history = new ArrayList<>();

    public AlertBotService(AlertPolicy policy, SocialPoster poster) {
        this.policy = policy;
        this.poster = poster;
    }

    public void handle(StageChange change) {
        if (change.hubId() == null || change.stage() == null || change.stage() < 0 || change.stage() > 8) {
            System.err.println("alertbot ignored an invalid stage message: " + change);
            return;
        }
        policy.evaluate(change.hubId(), change.stage(), change.timestamp()).ifPresent(alert -> {
            poster.post(alert);
            synchronized (history) {
                history.add(alert);
                if (history.size() > MAX_HISTORY) {
                    history.remove(0);
                }
            }
        });
    }

    public List<Alert> history() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    public int threshold() {
        return policy.threshold();
    }
}
