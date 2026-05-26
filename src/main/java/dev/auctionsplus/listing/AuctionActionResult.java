package dev.auctionsplus.listing;

import java.util.Map;

public record AuctionActionResult(boolean success, String messageKey, Map<String, String> placeholders) {
    public static AuctionActionResult success(String key, Map<String, String> placeholders) {
        return new AuctionActionResult(true, key, placeholders);
    }

    public static AuctionActionResult failure(String key, Map<String, String> placeholders) {
        return new AuctionActionResult(false, key, placeholders);
    }
}
