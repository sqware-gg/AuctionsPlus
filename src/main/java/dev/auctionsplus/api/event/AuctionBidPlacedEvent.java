package dev.auctionsplus.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class AuctionBidPlacedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final long listingId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID bidderUuid;
    private final String bidderName;
    private final UUID previousBidderUuid;
    private final String previousBidderName;
    private final ItemStack item;
    private final String itemName;
    private final double bid;
    private final String formattedBid;
    private final double previousBid;
    private final String formattedPreviousBid;
    private final long bidAtMillis;

    public AuctionBidPlacedEvent(long listingId, UUID sellerUuid, String sellerName, UUID bidderUuid,
                                 String bidderName, UUID previousBidderUuid, String previousBidderName,
                                 ItemStack item, String itemName, double bid, String formattedBid,
                                 double previousBid, String formattedPreviousBid, long bidAtMillis) {
        this.listingId = listingId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.bidderUuid = bidderUuid;
        this.bidderName = bidderName;
        this.previousBidderUuid = previousBidderUuid;
        this.previousBidderName = previousBidderName == null ? "" : previousBidderName;
        this.item = item.clone();
        this.itemName = itemName;
        this.bid = bid;
        this.formattedBid = formattedBid;
        this.previousBid = previousBid;
        this.formattedPreviousBid = formattedPreviousBid;
        this.bidAtMillis = bidAtMillis;
    }

    public long listingId() {
        return listingId;
    }

    public UUID sellerUuid() {
        return sellerUuid;
    }

    public String sellerName() {
        return sellerName;
    }

    public UUID bidderUuid() {
        return bidderUuid;
    }

    public String bidderName() {
        return bidderName;
    }

    public UUID previousBidderUuid() {
        return previousBidderUuid;
    }

    public String previousBidderName() {
        return previousBidderName;
    }

    public ItemStack item() {
        return item.clone();
    }

    public String itemName() {
        return itemName;
    }

    public double bid() {
        return bid;
    }

    public String formattedBid() {
        return formattedBid;
    }

    public double previousBid() {
        return previousBid;
    }

    public String formattedPreviousBid() {
        return formattedPreviousBid;
    }

    public long bidAtMillis() {
        return bidAtMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
