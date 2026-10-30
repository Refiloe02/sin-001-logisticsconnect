package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DelayStageServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-18T10:15:00.789Z"), ZoneOffset.UTC);

    private final DelayStageStore store = new DelayStageStore();
    private final List<StageChange> sent = new ArrayList<>();

    private DelayStageService serviceWith(StagePublisher publisher) {
        return new DelayStageService(store, publisher, CLOCK);
    }

    @Test
    void aChangePublishesExactlyOneMessage() {
        DelayStageService.Update update = serviceWith(sent::add).update("h-501", 5);

        assertTrue(update.changed());
        assertTrue(update.published());
        assertEquals(List.of(new StageChange("H-501", 5, "2026-07-18T10:15:00Z")), sent);
    }

    @Test
    void settingTheSameStageAgainPublishesNothing() {
        DelayStageService service = serviceWith(sent::add);
        service.update("H-501", 5);
        DelayStageService.Update again = service.update("H-501", 5);

        assertFalse(again.changed());
        assertFalse(again.published());
        assertEquals(1, sent.size());
    }

    @Test
    void brokerOutageDoesNotFailTheUpdate() {
        DelayStageService.Update update = serviceWith(change -> {
            throw new IllegalStateException("broker down");
        }).update("H-501", 3);

        assertTrue(update.changed());
        assertFalse(update.published());
        assertEquals(3, store.get("H-501"));    // state is still updated
    }

    @Test
    void invalidStageIsRejectedAndNothingIsPublished() {
        assertThrows(IllegalArgumentException.class, () -> serviceWith(sent::add).update("H-501", 9));
        assertTrue(sent.isEmpty());
    }
}
