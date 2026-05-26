package dev.auctionsplus.api;

public record AuctionListingView(long id, String sellerName, String itemName, String listingType,
                                 double price, String formattedPrice, double currentPrice,
                                 String formattedCurrentPrice, double minimumBid, String formattedMinimumBid,
                                 int bidCount, long createdAtMillis, long expiresAtMillis) {
    public boolean bidding() {
        return "bid".equalsIgnoreCase(listingType);
    }
}
