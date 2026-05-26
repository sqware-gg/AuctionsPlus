package dev.auctionsplus.api;

import dev.auctionsplus.listing.AuctionActionResult;
import dev.auctionsplus.listing.AuctionListing;
import dev.auctionsplus.listing.AuctionService;
import dev.auctionsplus.listing.ListingSort;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuctionsPlusApi {
    private static AuctionService service;

    private AuctionsPlusApi() {
    }

    public static void register(AuctionService auctionService) {
        service = auctionService;
    }

    public static void unregister() {
        service = null;
    }

    public static AuctionApiResult bid(UUID bidderUuid, String bidderName, long listingId, double bid) {
        if (service == null) {
            return new AuctionApiResult(false, "api-unavailable", Map.of());
        }
        AuctionActionResult result = service.bidOffline(listingId, bidderUuid, bidderName, bid);
        return new AuctionApiResult(result.success(), result.messageKey(), result.placeholders());
    }

    public static List<AuctionListingView> activeAuctions(int limit) {
        if (service == null) {
            return List.of();
        }
        int cappedLimit = Math.max(1, Math.min(25, limit));
        return service.browseListings(null, ListingSort.NEWEST).stream()
                .filter(AuctionListing::bidding)
                .limit(cappedLimit)
                .map(service::view)
                .toList();
    }
}
