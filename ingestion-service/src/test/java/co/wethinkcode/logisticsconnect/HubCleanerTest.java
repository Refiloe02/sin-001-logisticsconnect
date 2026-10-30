package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class HubCleanerTest {

    @Test
    void tidyTrimsPadsAndCollapsesDoubleSpaces() {
        assertEquals("Cape Town Port", HubCleaner.tidy("  Cape Town  Port "));
    }

    @Test
    void tidyTurnsEveryPlaceholderIntoNull() {
        for (String placeholder : new String[] {"", "  ", "N/A", "n/a", "TBD", "unknown", "-", "NaN"}) {
            assertNull(HubCleaner.tidy(placeholder), "placeholder: '" + placeholder + "'");
        }
        assertNull(HubCleaner.tidy(null));
    }

    @Test
    void idsAreTrimmedAndUpperCased() {
        assertEquals("H-502", HubCleaner.normaliseId("H-502 "));
        assertEquals("H-501", HubCleaner.normaliseId("h-501"));
    }

    @Test
    void provinceSpellingVariantsCollapseToOneName() {
        for (String variant : new String[] {"Kwa-Zulu Natal", "KwaZulu Natal", "kwazulu-natal", "KWAZULU-NATAL"}) {
            assertEquals("KwaZulu-Natal", HubCleaner.normaliseProvince(variant), variant);
        }
        assertEquals("Gauteng", HubCleaner.normaliseProvince(" gauteng "));
        assertEquals("Western Cape", HubCleaner.normaliseProvince("western cape"));
        assertNull(HubCleaner.normaliseProvince(""));
    }

    @Test
    void sortingCentreNamesAreTitleCased() {
        assertEquals("Johannesburg Central", HubCleaner.normaliseName("johannesburg central"));
    }

    @Test
    void flagsAreNormalisedToBooleans() {
        for (String yes : new String[] {"Y", "yes", "YES", "true", "TRUE", "1"}) {
            assertEquals(Boolean.TRUE, HubCleaner.parseFlag(yes), yes);
        }
        for (String no : new String[] {"N", "no", "FALSE", "false", "0", "n"}) {
            assertEquals(Boolean.FALSE, HubCleaner.parseFlag(no), no);
        }
        assertNull(HubCleaner.parseFlag("unknown"));
        assertNull(HubCleaner.parseFlag("N/A"));
        assertNull(HubCleaner.parseFlag("banana"));
    }

    @Test
    void readmeWorkedExampleProducesTheDocumentedShape() {
        List<String[]> rows = List.of(
                new String[] {"hub_id", " Province ", "sorting_center", "active"},
                new String[] {"H-502 ", "gauteng", "Pretoria North", "0"},
                new String[] {"H-505", "Western Cape ", "Cape Town  Port", "TRUE"});

        List<HubRecord> cleaned = new HubCleaner().clean(rows);

        assertEquals(2, cleaned.size());
        assertEquals(new HubRecord("H-502", "Gauteng", "Pretoria North", false, List.of()), cleaned.get(0));
        assertEquals(new HubRecord("H-505", "Western Cape", "Cape Town Port", true, List.of()), cleaned.get(1));
    }

    @Test
    void rowsWithoutAnIdAreSkipped() {
        List<String[]> rows = List.of(
                new String[] {"hub_id", "province", "sorting_center", "active"},
                new String[] {"", "Gauteng", "Nowhere", "Y"},
                new String[] {""});
        assertEquals(0, new HubCleaner().clean(rows).size());
    }

    @Test
    void missingColumnIsReportedClearly() {
        List<String[]> rows = List.<String[]>of(new String[] {"hub_id", "province"});
        assertThrows(IllegalArgumentException.class, () -> new HubCleaner().clean(rows));
    }
}
