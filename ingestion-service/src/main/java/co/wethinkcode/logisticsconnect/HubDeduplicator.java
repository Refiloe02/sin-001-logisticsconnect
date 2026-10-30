package co.wethinkcode.logisticsconnect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class HubDeduplicator {

    public record Result(List<HubRecord> hubs, List<String> notes) {
    }

    public Result merge(List<HubRecord> records) {
        Map<String, List<HubRecord>> groups = new LinkedHashMap<>();
        for (HubRecord record : records) {
            groups.computeIfAbsent(identityKey(record), key -> new ArrayList<>()).add(record);
        }

        List<HubRecord> hubs = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        for (List<HubRecord> group : groups.values()) {
            hubs.add(mergeGroup(group, notes));
        }
        hubs.sort(BY_ID);
        return new Result(hubs, notes);
    }

    private static final Comparator<HubRecord> BY_ID =
            Comparator.comparingInt((HubRecord h) -> numericId(h.id())).thenComparing(HubRecord::id);

    private static String identityKey(HubRecord record) {
        return record.sortingCenter() != null
                ? "centre:" + record.sortingCenter().toLowerCase(Locale.ROOT)
                : "id:" + record.id();
    }

    private static HubRecord mergeGroup(List<HubRecord> group, List<String> notes) {
        List<HubRecord> ordered = new ArrayList<>(group);
        ordered.sort(BY_ID);
        HubRecord keeper = ordered.get(0);

        List<String> mergedIds = ordered.stream()
                .map(HubRecord::id)
                .filter(id -> !id.equals(keeper.id()))
                .distinct()
                .toList();

        List<String> provinces = ordered.stream().map(HubRecord::province).filter(Objects::nonNull).toList();
        String province = mostCommon(provinces);

        long yes = ordered.stream().filter(r -> Boolean.TRUE.equals(r.active())).count();
        long no = ordered.stream().filter(r -> Boolean.FALSE.equals(r.active())).count();
        Boolean active = (yes + no == 0) ? null : Boolean.valueOf(yes > no);

        if (!mergedIds.isEmpty()) {
            StringBuilder note = new StringBuilder()
                    .append(keeper.id()).append(' ').append(keeper.sortingCenter())
                    .append(": merged ").append(String.join(", ", mergedIds));
            if (yes > 0 && no > 0) {
                note.append("; active flags disagreed (").append(yes).append(" true / ").append(no).append(" false) -> ")
                        .append(active).append(yes == no ? " (tie: failing safe)" : " (majority)");
            }
            if (provinces.stream().distinct().count() > 1) {
                note.append("; province values disagreed -> ").append(province);
            }
            notes.add(note.toString());
        }
        return new HubRecord(keeper.id(), province, keeper.sortingCenter(), active, mergedIds);
    }

    private static String mostCommon(List<String> values) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String value : values) {
            counts.merge(value, 1, Integer::sum);
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private static int numericId(String id) {
        int dash = id.lastIndexOf('-');
        try {
            return Integer.parseInt(id.substring(dash + 1));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
