package co.wethinkcode.logisticsconnect;

import java.util.List;

public record CleaningReport(int rowsRead, int rowsSkipped, int duplicatesMerged, int hubsServed, List<String> notes) {
}
