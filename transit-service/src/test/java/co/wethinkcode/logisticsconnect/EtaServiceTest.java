package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EtaServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-18T10:00:00Z"), ZoneOffset.UTC);

    private static final Map<String, Hub> HUBS = Map.of(
            "H-501", new Hub("H-501", "Western Cape", "Cape Town Port", true),
            "H-502", new Hub("H-502", "Gauteng", "Pretoria North", false),
            "H-517", new Hub("H-517", "Northern Cape", "Kimberley Hub", null));

    private EtaService service(int stage) {
        EtaService.HubSource hubs = id -> Optional.ofNullable(HUBS.get(id));
        DelayStageSource stages = new DelayStageSource() {
            public int stageFor(String hubId) {
                return stage;
            }

            public String name() {
                return "fake";
            }
        };
        return new EtaService(hubs, stages, new EtaCalculator(), CLOCK);
    }

    @Test
    void activeHubReturnsAnEtaWindow() throws IOException {
        Map<String, Object> eta = service(2).estimate("H-501");
        assertEquals("H-501", eta.get("hubId"));
        assertEquals("Cape Town Port", eta.get("sortingCenter"));
        assertEquals(2, eta.get("delayStage"));
        assertEquals("fake", eta.get("delayStageSource"));
        assertEquals("2026-07-19T22:00:00Z", eta.get("earliestArrival"));   // 24h + 2*6h
        assertEquals("2026-07-20T16:00:00Z", eta.get("latestArrival"));     // + 12h + 2*3h
    }

    @Test
    void hubWithUnknownActiveFlagStillGetsAnEta() throws IOException {
        assertEquals("H-517", service(0).estimate("H-517").get("hubId"));
    }

    @Test
    void unknownHubIsReported() {
        assertThrows(EtaService.UnknownHubException.class, () -> service(0).estimate("H-999"));
    }

    @Test
    void inactiveHubIsRefused() {
        assertThrows(EtaService.HubInactiveException.class, () -> service(0).estimate("H-502"));
    }

    @Test
    void hubServiceFailureSurfacesAsIOException() {
        EtaService.HubSource down = id -> {
            throw new IOException("hub-service down");
        };
        EtaService svc = new EtaService(down, new StageCache(), new EtaCalculator(), CLOCK);
        assertThrows(IOException.class, () -> svc.estimate("H-501"));
    }
}
