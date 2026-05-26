package dev.auctionsplus.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class AuctionListingCancelledEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final long listingId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID cancelledByUuid;
    private final String cancelledByName;
    private final boolean admin;
    private final ItemStack item;
    private final String itemName;
    private final double price;
    private final String formattedPrice;
    private final long cancelledAtMillis;

    public AuctionListingCancelledEvent(long listingId, UUID sellerUuid, String sellerName, UUID cancelledByUuid,
                                        String cancelledByName, boolean admin, ItemStack item, String itemName,
                                        double price, String formattedPrice, long cancelledAtMillis) {
        this.listingId = listingId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.cancelledByUuid = cancelledByUuid;
        this.cancelledByName = cancelledByName;
        this.admin = admin;
        this.item = item.clone();
        this.itemName = itemName;
        this.price = price;
        this.formattedPrice = formattedPrice;
        this.cancelledAtMillis = cancelledAtMillis;
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

    public UUID cancelledByUuid() {
        return cancelledByUuid;
    }

    public String cancelledByName() {
        return cancelledByName;
    }

    public boolean admin() {
        return admin;
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

    public long cancelledAtMillis() {
        return cancelledAtMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
