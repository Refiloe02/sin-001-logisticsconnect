package co.wethinkcode.logisticsconnect;

import java.time.Duration;
import java.time.Instant;

public final class EtaCalculator {

    static final Duration BASE_TRANSIT = Duration.ofHours(24);
    static final Duration DELAY_PER_STAGE = Duration.ofHours(6);
    static final Duration BASE_WINDOW = Duration.ofHours(12);
    static final Duration WINDOW_PER_STAGE = Duration.ofHours(3);

    public record Window(Instant earliest, Instant latest) {
    }

    public Window calculate(Instant dispatchedAt, int delayStage) {
        if (delayStage < 0 || delayStage > 8) {
            throw new IllegalArgumentException("delay stage must be 0-8 but was " + delayStage);
        }
        Instant earliest = dispatchedAt.plus(BASE_TRANSIT).plus(DELAY_PER_STAGE.multipliedBy(delayStage));
        Instant latest = earliest.plus(BASE_WINDOW).plus(WINDOW_PER_STAGE.multipliedBy(delayStage));
        return new Window(earliest, latest);
    }
}
