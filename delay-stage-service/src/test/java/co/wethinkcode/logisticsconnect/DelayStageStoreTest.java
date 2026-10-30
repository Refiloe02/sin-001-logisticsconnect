package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DelayStageStoreTest {

    private final DelayStageStore store = new DelayStageStore();

    @Test
    void unknownHubsAreAtStageZero() {
        assertEquals(0, store.get("H-501"));
    }

    @Test
    void setReturnsThePreviousStageAndStoresTheNewOne() {
        assertEquals(0, store.set("H-501", 3));
        assertEquals(3, store.set("H-501", 5));
        assertEquals(5, store.get("H-501"));
    }

    @Test
    void hubIdsAreCaseAndPaddingInsensitive() {
        store.set(" h-501 ", 4);
        assertEquals(4, store.get("H-501"));
    }

    @Test
    void stagesOutsideZeroToEightAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> store.set("H-501", -1));
        assertThrows(IllegalArgumentException.class, () -> store.set("H-501", 9));
        assertEquals(0, store.get("H-501"));    // a rejected update must not change anything
    }

    @Test
    void boundaryStagesAreAccepted() {
        store.set("H-501", 0);
        store.set("H-501", 8);
        assertEquals(8, store.get("H-501"));
    }
}
