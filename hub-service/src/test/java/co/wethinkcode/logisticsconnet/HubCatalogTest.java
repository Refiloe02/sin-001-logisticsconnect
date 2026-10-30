package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HubCatalogTest {

    private final HubCatalog catalog = new HubCatalog(List.of(
            new Hub("H-500", "Gauteng", "Johannesburg Central", true, List.of("H-504", "H-510")),
            new Hub("H-501", "Western Cape", "Cape Town Port", true, List.of("H-505")),
            new Hub("H-502", "Gauteng", "Pretoria North", false, List.of()),
            new Hub("H-517", null, "Kimberley Hub", null, List.of())));

    @Test
    void findIsCaseInsensitiveAndTrimsInput() {
        assertEquals("H-501", catalog.find(" h-501 ").orElseThrow().id());
    }

    @Test
    void mergedDuplicateIdsResolveToTheSurvivingHub() {
        assertEquals("H-500", catalog.find("H-504").orElseThrow().id());
    }

    @Test
    void unknownIdIsEmpty() {
        assertTrue(catalog.find("H-999").isEmpty());
        assertTrue(catalog.find(null).isEmpty());
    }

    @org.junit.Test
    void provincesAreDistinctSortedAndSkipMissingOnes() {
        assertEquals(List.of("Gauteng", "Western Cape"), catalog.provinces());
    }

    @Test
    void inProvinceFiltersIgnoringCase() {
        assertEquals(2, catalog.inProvince("gauteng").size());
        assertEquals(0, catalog.inProvince("Limpopo").size());
    }
}
