package co.wethinkcode.logisticsconnect;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class HubCatalog {

    private final List<Hub> hubs;

    public HubCatalog(List<Hub> hubs) {
        this.hubs = List.copyOf(hubs);
    }

    public List<Hub> all() {
        return hubs;
    }

    public Optional<Hub> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.trim().toUpperCase(Locale.ROOT);
        return hubs.stream()
                .filter(h -> h.id().equals(wanted) || h.mergedFrom().contains(wanted))
                .findFirst();
    }

    public List<String> provinces() {
        return hubs.stream().map(Hub::province).filter(p -> p != null).distinct().sorted().toList();
    }

    public List<Hub> inProvince(String province) {
        return hubs.stream().filter(h -> h.province() != null && h.province().equalsIgnoreCase(province.trim())).toList();
    }
}
