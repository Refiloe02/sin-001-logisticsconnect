package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Hub(String id, String province, String sortingCenter, Boolean active, List<String> mergedFrom) {

    public Hub {
        mergedFrom = mergedFrom == null ? List.of() : List.copyOf(mergedFrom);
    }
}
