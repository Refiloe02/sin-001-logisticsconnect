package co.wethinkcode.logisticsconnect;

import java.util.List;

public record HubRecord(String id, String province, String sortingCenter, Boolean active, List<String> mergedFrom) {

    public HubRecord {
        mergedFrom = mergedFrom == null ? List.of() : List.copyOf(mergedFrom);
    }
}
