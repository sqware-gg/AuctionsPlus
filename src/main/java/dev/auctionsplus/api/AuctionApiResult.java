package dev.auctionsplus.api;

import java.util.Map;

public record AuctionApiResult(boolean success, String messageKey, Map<String, String> placeholders) {
    public AuctionApiResult {
        placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    }
}
