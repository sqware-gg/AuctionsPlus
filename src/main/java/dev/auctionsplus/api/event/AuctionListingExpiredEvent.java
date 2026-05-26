package dev.auctionsplus.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class AuctionListingExpiredEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final long listingId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final String itemName;
    private final double price;
    private final String formattedPrice;
    private final long expiredAtMillis;

    public AuctionListingExpiredEvent(long listingId, UUID sellerUuid, String sellerName, ItemStack item,
                                      String itemName, double price, String formattedPrice, long expiredAtMillis) {
        this.listingId = listingId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.itemName = itemName;
        this.price = price;
        this.formattedPrice = formattedPrice;
        this.expiredAtMillis = expiredAtMillis;
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

    public ItemStack item() {
        return item.clone();
    }

    public String itemName() {
        return itemName;
    }

    public double price() {
        return price;
    }

    public String formattedPrice() {
        return formattedPrice;
    }

    public long expiredAtMillis() {
        return expiredAtMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
