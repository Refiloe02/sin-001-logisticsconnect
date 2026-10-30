package co.wethinkcode.logisticsconnect;

import java.util.List;

/** clean -> de-duplicate -> report.*/
public final class IngestionPipeline {

    private IngestionPipeline() {
    }

    public static IngestionResult run(List<String[]> rows) {
        List<HubRecord> cleaned = new HubCleaner().clean(rows);
        HubDeduplicator.Result merged = new HubDeduplicator().merge(cleaned);

        int rowsRead = Math.max(0, rows.size() - 1);              // minus the header
        int rowsSkipped = rowsRead - cleaned.size();
        int duplicatesMerged = cleaned.size() - merged.hubs().size();
        CleaningReport report = new CleaningReport(
                rowsRead, rowsSkipped, duplicatesMerged, merged.hubs().size(), merged.notes());
        return new IngestionResult(merged.hubs(), report);
    }
}
