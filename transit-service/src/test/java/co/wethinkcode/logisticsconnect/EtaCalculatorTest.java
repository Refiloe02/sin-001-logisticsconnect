package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EtaCalculatorTest {

    private static final Instant T0 = Instant.parse("2026-07-18T10:00:00Z");
    private final EtaCalculator calculator = new EtaCalculator();

    @Test
    void noDelayMeansTwentyFourToThirtySixHours() {
        EtaCalculator.Window w = calculator.calculate(T0, 0);
        assertEquals(Instant.parse("2026-07-19T10:00:00Z"), w.earliest());
        assertEquals(Instant.parse("2026-07-19T22:00:00Z"), w.latest());
    }

    @Test
    void eachStageAddsSixHoursAndWidensTheWindowByThree() {
        EtaCalculator.Window w = calculator.calculate(T0, 3);
        assertEquals(Instant.parse("2026-07-20T04:00:00Z"), w.earliest());   // +24h +18h
        assertEquals(Instant.parse("2026-07-21T01:00:00Z"), w.latest());     // +12h +9h
    }

    @Test
    void worstStageIsThreeDaysPlus() {
        EtaCalculator.Window w = calculator.calculate(T0, 8);
        assertEquals(Instant.parse("2026-07-21T10:00:00Z"), w.earliest());   // +24h +48h
        assertEquals(Instant.parse("2026-07-22T22:00:00Z"), w.latest());     // +12h +24h
    }

    @org.junit.Test
    void invalidStagesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(T0, -1));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(T0, 9));
    }
}
