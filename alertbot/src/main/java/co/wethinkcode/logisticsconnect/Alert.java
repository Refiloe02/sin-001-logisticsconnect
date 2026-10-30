package co.wethinkcode.logisticsconnect;

public record Alert(String hubId, int stage, String kind, String message, String timestamp) {
}
