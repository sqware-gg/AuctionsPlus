package dev.auctionsplus.util;

import java.util.ArrayList;
import java.util.List;

public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String compact(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0L) {
            parts.add(days + "d");
        }
        if (hours > 0L) {
            parts.add(hours + "h");
        }
        if (minutes > 0L) {
            parts.add(minutes + "m");
        }
        if (parts.isEmpty() || seconds > 0L) {
            parts.add(seconds + "s");
        }
        return String.join(" ", parts);
    }
}
