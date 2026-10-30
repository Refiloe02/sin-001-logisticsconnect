package co.wethinkcode.logisticsconnect;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class StageCache implements DelayStageSource {

    private final Map<String, Integer> stages = new ConcurrentHashMap<>();

    public void update(String hubId, int stage) {
        stages.put(normalise(hubId), stage);
    }

    @Override
    public int stageFor(String hubId) {
        return stages.getOrDefault(normalise(hubId), 0);
    }

    @Override
    public String name() {
        return "mq:package-status-topic";
    }

    public Map<String, Integer> snapshot() {
        return new TreeMap<>(stages);
    }

    private static String normalise(String hubId) {
        return hubId.trim().toUpperCase(Locale.ROOT);
    }
}
