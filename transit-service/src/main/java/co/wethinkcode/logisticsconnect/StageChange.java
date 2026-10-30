package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StageChange(String hubId, Integer stage, String timestamp) {
}
