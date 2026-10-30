package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StageCacheTest {

    private final StageCache cache = new StageCache();

    @Test
    void hubsWeHaveNotHeardAboutAreAtStageZero() {
        assertEquals(0, cache.stageFor("H-501"));
    }

    @Test
    void latestUpdateWins() {
        cache.update("H-501", 2);
        cache.update("H-501", 6);
        assertEquals(6, cache.stageFor("H-501"));
    }

    @Test
    void idsAreNormalised() {
        cache.update(" h-501 ", 4);
        assertEquals(4, cache.stageFor("H-501"));
        assertEquals(4, cache.snapshot().get("H-501"));
    }
}
