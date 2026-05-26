package dev.auctionsplus.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class AuctionWonEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final long listingId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID winnerUuid;
    private final String winnerName;
    private final ItemStack item;
    private final String itemName;
    private final double winningBid;
    private final String formattedWinningBid;
    private final double sellerRevenue;
    private final String formattedSellerRevenue;
    private final double tax;
    private final String formattedTax;
    private final long wonAtMillis;

    public AuctionWonEvent(long listingId, UUID sellerUuid, String sellerName, UUID winnerUuid,
                           String winnerName, ItemStack item, String itemName, double winningBid,
                           String formattedWinningBid, double sellerRevenue, String formattedSellerRevenue,
                           double tax, String formattedTax, long wonAtMillis) {
        this.listingId = listingId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.winnerUuid = winnerUuid;
        this.winnerName = winnerName;
        this.item = item.clone();
        this.itemName = itemName;
        this.winningBid = winningBid;
        this.formattedWinningBid = formattedWinningBid;
        this.sellerRevenue = sellerRevenue;
        this.formattedSellerRevenue = formattedSellerRevenue;
        this.tax = tax;
        this.formattedTax = formattedTax;
        this.wonAtMillis = wonAtMillis;
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

    public UUID winnerUuid() {
        return winnerUuid;
    }

    public String winnerName() {
        return winnerName;
    }

    public ItemStack item() {
        return item.clone();
    }

    public String itemName() {
        return itemName;
    }

    public double winningBid() {
        return winningBid;
    }

    public String formattedWinningBid() {
        return formattedWinningBid;
    }

    public double sellerRevenue() {
        return sellerRevenue;
    }

    public String formattedSellerRevenue() {
        return formattedSellerRevenue;
    }

    public double tax() {
        return tax;
    }

    public String formattedTax() {
        return formattedTax;
    }

    public long wonAtMillis() {
        return wonAtMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
