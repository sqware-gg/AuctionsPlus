package dev.auctionsplus.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class AuctionListingSoldEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final long listingId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID buyerUuid;
    private final String buyerName;
    private final ItemStack item;
    private final String itemName;
    private final double price;
    private final String formattedPrice;
    private final double sellerRevenue;
    private final String formattedSellerRevenue;
    private final double tax;
    private final String formattedTax;
    private final long soldAtMillis;

    public AuctionListingSoldEvent(long listingId, UUID sellerUuid, String sellerName, UUID buyerUuid,
                                   String buyerName, ItemStack item, String itemName, double price,
                                   String formattedPrice, double sellerRevenue, String formattedSellerRevenue,
                                   double tax, String formattedTax, long soldAtMillis) {
        this.listingId = listingId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
        this.item = item.clone();
        this.itemName = itemName;
        this.price = price;
        this.formattedPrice = formattedPrice;
        this.sellerRevenue = sellerRevenue;
        this.formattedSellerRevenue = formattedSellerRevenue;
        this.tax = tax;
        this.formattedTax = formattedTax;
        this.soldAtMillis = soldAtMillis;
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

    public UUID buyerUuid() {
        return buyerUuid;
    }

    public String buyerName() {
        return buyerName;
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

    public long soldAtMillis() {
        return soldAtMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
