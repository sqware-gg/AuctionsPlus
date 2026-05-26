package dev.auctionsplus.util;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern PART = Pattern.compile("(\\d+)([dhms])");

    private DurationParser() {
    }

    public static OptionalLong parseMillis(String input) {
        if (input == null || input.isBlank()) {
            return OptionalLong.empty();
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (normalized.matches("\\d+")) {
            return OptionalLong.of(Long.parseLong(normalized) * 1000L);
        }

        Matcher matcher = PART.matcher(normalized);
        long total = 0L;
        int position = 0;
        while (matcher.find()) {
            if (matcher.start() != position) {
                return OptionalLong.empty();
            }
            long value = Long.parseLong(matcher.group(1));
            total += switch (matcher.group(2)) {
                case "d" -> value * 86400000L;
                case "h" -> value * 3600000L;
                case "m" -> value * 60000L;
                case "s" -> value * 1000L;
                default -> 0L;
            };
            position = matcher.end();
        }
        if (position != normalized.length() || total <= 0L) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(total);
    }
}
