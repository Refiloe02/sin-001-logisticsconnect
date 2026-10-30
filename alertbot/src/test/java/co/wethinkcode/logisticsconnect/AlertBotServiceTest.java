package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertBotServiceTest {

    private final List<Alert> posted = new ArrayList<>();
    private final AlertBotService bot = new AlertBotService(new AlertPolicy(5), posted::add);

    @org.junit.Test
    void crossingTheThresholdPostsAndRemembersTheAlert() {
        bot.handle(new StageChange("H-501", 6, "2026-07-18T10:15:00Z"));

        assertEquals(1, posted.size());
        assertEquals(posted, bot.history());
    }

    @Test
    void lowStagesPostNothing() {
        bot.handle(new StageChange("H-501", 2, "t"));
        assertTrue(posted.isEmpty());
    }

    @Test
    void invalidMessagesAreIgnored() {
        bot.handle(new StageChange(null, 6, "t"));
        bot.handle(new StageChange("H-501", null, "t"));
        bot.handle(new StageChange("H-501", 42, "t"));
        assertTrue(posted.isEmpty());
    }
}
