package co.wethinkcode.logisticsconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HubCleaner {

    private static final Set<String> PLACEHOLDERS =
            Set.of("", "n/a", "na", "tbd", "unknown", "-", "nan", "null", "none");

    private static final Map<String, String> PROVINCES = Map.of(
            "gauteng", "Gauteng",
            "westerncape", "Western Cape",
            "easterncape", "Eastern Cape",
            "northerncape", "Northern Cape",
            "kwazulunatal", "KwaZulu-Natal",
            "freestate", "Free State",
            "limpopo", "Limpopo",
            "mpumalanga", "Mpumalanga",
            "northwest", "North West");

    private static final Set<String> TRUE_FLAGS = Set.of("y", "yes", "true", "1", "t");
    private static final Set<String> FALSE_FLAGS = Set.of("n", "no", "false", "0", "f");

    public static String tidy(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll("\\s+", " ");
        return PLACEHOLDERS.contains(value.toLowerCase(Locale.ROOT)) ? null : value;
    }

     public static String normaliseId(String raw) {
        String value = tidy(raw);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

     public static String normaliseProvince(String raw) {
        String value = tidy(raw);
        if (value == null) {
            return null;
        }
        String key = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return PROVINCES.getOrDefault(key, titleCase(value));
    }

    public static String normaliseName(String raw) {
        String value = tidy(raw);
        return value == null ? null : titleCase(value);
    }

    public static Boolean parseFlag(String raw) {
        String value = tidy(raw);
        if (value == null) {
            return null;
        }
        String key = value.toLowerCase(Locale.ROOT);
        if (TRUE_FLAGS.contains(key)) {
            return Boolean.TRUE;
        }
        if (FALSE_FLAGS.contains(key)) {
            return Boolean.FALSE;
        }
        return null;
    }

    static String titleCase(String text) {
        StringBuilder out = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    public List<HubRecord> clean(List<String[]> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        String[] header = rows.get(0);
        int id = column(header, "hub_id");
        int province = column(header, "province");
        int centre = column(header, "sorting_center");
        int active = column(header, "active");

        List<HubRecord> cleaned = new ArrayList<>();
        for (String[] row : rows.subList(1, rows.size())) {
            String hubId = normaliseId(cell(row, id));
            if (hubId == null) {
                continue;
            }
            cleaned.add(new HubRecord(
                    hubId,
                    normaliseProvince(cell(row, province)),
                    normaliseName(cell(row, centre)),
                    parseFlag(cell(row, active)),
                    List.of()));
        }
        return cleaned;
    }

    private static int column(String[] header, String name) {
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null && header[i].trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("CSV is missing required column: " + name);
    }

    private static String cell(String[] row, int index) {
        return index < row.length ? row[index] : null;
    }
}
