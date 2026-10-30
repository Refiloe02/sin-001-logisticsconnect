package co.wethinkcode.logisticsconnect;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class DelayStageStore {

    public static final int MIN_STAGE = 0;
    public static final int MAX_STAGE = 8;

    private final Map<String, Integer> stages = new ConcurrentHashMap<>();

    public int get(String hubId) {
        return stages.getOrDefault(normalise(hubId), MIN_STAGE);
    }

    public int set(String hubId, int stage) {
        if (stage < MIN_STAGE || stage > MAX_STAGE) {
            throw new IllegalArgumentException("stage must be between " + MIN_STAGE + " and " + MAX_STAGE + " but was " + stage);
        }
        Integer previous = stages.put(normalise(hubId), stage);
        return previous == null ? MIN_STAGE : previous;
    }

    public Map<String, Integer> all() {
        return new TreeMap<>(stages);
    }

    static String normalise(String hubId) {
        return hubId.trim().toUpperCase(Locale.ROOT);
    }
}
