package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs the real hubs-global.csv through the whole pipeline. */
class IngestionPipelineTest {

    private static IngestionResult result;

    @BeforeAll
    static void loadRealCsv() throws Exception {
        try (InputStream in = IngestionPipelineTest.class.getResourceAsStream("/hubs-global.csv")) {
            result = IngestionPipeline.run(HubCsvReader.read(in));
        }
    }

    private HubRecord hub(String id) {
        return result.find(id).orElseThrow();
    }

    @Test
    void eighteenRawRowsBecomeTenHubs() {
        assertEquals(18, result.report().rowsRead());
        assertEquals(10, result.hubs().size());
        assertEquals(8, result.report().duplicatesMerged());
        assertEquals(0, result.report().rowsSkipped());
    }

    @Test
    void johannesburgCentralDuplicatesAreMergedIntoTheLowestId() {
        HubRecord jhb = hub("H-500");
        assertEquals("Johannesburg Central", jhb.sortingCenter());
        assertEquals("Gauteng", jhb.province());
        assertEquals(List.of("H-504", "H-510", "H-515"), jhb.mergedFrom());
        // flags were Y, true, FALSE, YES -> majority true
        assertEquals(Boolean.TRUE, jhb.active());
    }

    @Test
    void activeTieFailsSafeToFalse() {
        HubRecord pretoria = hub("H-502");              // H-502 = 0 (false), H-508 = yes (true)
        assertEquals(List.of("H-508"), pretoria.mergedFrom());
        assertEquals(Boolean.FALSE, pretoria.active());
        assertEquals("Gauteng", pretoria.province());   // H-508 had a blank province, must not erase it
    }

    @Test
    void provinceSpellingVariantsMergeIntoOneDurbanHub() {
        HubRecord durban = hub("H-503");
        assertEquals("KwaZulu-Natal", durban.province());
        assertEquals(List.of("H-506", "H-516"), durban.mergedFrom());
    }

    @Test
    void unknownFlagsStayNullInsteadOfBeingGuessed() {
        assertNull(hub("H-511").active());   // "unknown"
        assertNull(hub("H-517").active());   // "N/A"
    }

    @Test
    void mergedIdsStillResolveToTheSurvivingRecord() {
        assertEquals("H-500", hub("h-504").id());
        assertTrue(result.find("H-999").isEmpty());
    }

    @Test
    void everyNoteExplainsAMerge() {
        assertEquals(4, result.report().notes().size());
        assertTrue(result.report().notes().stream().anyMatch(n -> n.contains("tie: failing safe")));
    }
}
