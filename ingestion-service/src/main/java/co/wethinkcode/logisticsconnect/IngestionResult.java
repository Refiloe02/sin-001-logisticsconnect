package co.wethinkcode.logisticsconnect;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record IngestionResult(List<HubRecord> hubs, CleaningReport report) {

    public Optional<HubRecord> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.trim().toUpperCase(Locale.ROOT);
        return hubs.stream()
                .filter(h -> h.id().equals(wanted) || h.mergedFrom().contains(wanted))
                .findFirst();
    }
}
