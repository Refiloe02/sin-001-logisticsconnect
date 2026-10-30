package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlertPolicyTest {

    private final AlertPolicy policy = new AlertPolicy(5);

    @Test
    void stagesBelowTheThresholdStaySilent() {
        assertTrue(policy.evaluate("H-501", 3, "t").isEmpty());
    }

    @Test
    void crossingTheThresholdRaisesOneAlert() {
        Optional<Alert> alert = policy.evaluate("H-501", 5, "2026-07-18T10:15:00Z");
        assertEquals("RAISED", alert.orElseThrow().kind());
        assertEquals("H-501", alert.get().hubId());
        assertTrue(alert.get().message().contains("stage 5"));
    }

    @Test
    void stayingAboveTheThresholdDoesNotRepeatTheAlert() {
        policy.evaluate("H-501", 5, "t");
        assertTrue(policy.evaluate("H-501", 6, "t").isEmpty());
        assertTrue(policy.evaluate("H-501", 8, "t").isEmpty());
    }

    @Test
    void droppingBackBelowTheThresholdClearsTheAlert() {
        policy.evaluate("H-501", 7, "t");
        assertEquals("CLEARED", policy.evaluate("H-501", 2, "t").orElseThrow().kind());
    }

    @Test
    void hubsAreTrackedIndependently() {
        policy.evaluate("H-501", 6, "t");
        assertEquals("RAISED", policy.evaluate("H-502", 6, "t").orElseThrow().kind());
    }

    @Test
    void thresholdIsConfigurable() {
        assertTrue(new AlertPolicy(2).evaluate("H-501", 2, "t").isPresent());
    }
}
