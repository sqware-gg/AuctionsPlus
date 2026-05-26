package dev.auctionsplus.listing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AuctionListing {
    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final ListingType type;
    private final double price;
    private final long createdAtMillis;
    private final long expiresAtMillis;
    private ListingStatus status;
    private UUID buyerUuid;
    private String buyerName;
    private long soldAtMillis;
    private UUID highestBidderUuid;
    private String highestBidderName;
    private double highestBid;
    private long lastBidAtMillis;
    private int bidCount;

    public AuctionListing(long id, UUID sellerUuid, String sellerName, ItemStack item, double price,
                          long createdAtMillis, long expiresAtMillis) {
        this(id, sellerUuid, sellerName, item, ListingType.FIXED_PRICE, price, createdAtMillis, expiresAtMillis,
                ListingStatus.ACTIVE, null, "", 0L, null, "", 0.0D, 0L, 0);
    }

    public AuctionListing(long id, UUID sellerUuid, String sellerName, ItemStack item, double price,
                          long createdAtMillis, long expiresAtMillis, ListingStatus status,
                          UUID buyerUuid, String buyerName, long soldAtMillis) {
        this(id, sellerUuid, sellerName, item, ListingType.FIXED_PRICE, price, createdAtMillis, expiresAtMillis,
                status, buyerUuid, buyerName, soldAtMillis, null, "", 0.0D, 0L, 0);
    }

    public AuctionListing(long id, UUID sellerUuid, String sellerName, ItemStack item, ListingType type, double price,
                          long createdAtMillis, long expiresAtMillis, ListingStatus status,
                          UUID buyerUuid, String buyerName, long soldAtMillis, UUID highestBidderUuid,
                          String highestBidderName, double highestBid, long lastBidAtMillis, int bidCount) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName == null ? "" : sellerName;
        this.item = item.clone();
        this.type = type == null ? ListingType.FIXED_PRICE : type;
        this.price = price;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.status = status == null ? ListingStatus.ACTIVE : status;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName == null ? "" : buyerName;
        this.soldAtMillis = soldAtMillis;
        this.highestBidderUuid = highestBidderUuid;
        this.highestBidderName = highestBidderName == null ? "" : highestBidderName;
        this.highestBid = Math.max(0.0D, highestBid);
        this.lastBidAtMillis = lastBidAtMillis;
        this.bidCount = Math.max(0, bidCount);
    }

    public long id() {
        return id;
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

    public ListingType type() {
        return type;
    }

    public boolean bidding() {
        return type == ListingType.BID;
    }

    public double price() {
        return price;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    public ListingStatus status() {
        return status;
    }

    public UUID buyerUuid() {
        return buyerUuid;
    }

    public String buyerName() {
        return buyerName;
    }

    public long soldAtMillis() {
        return soldAtMillis;
    }

    public UUID highestBidderUuid() {
        return highestBidderUuid;
    }

    public String highestBidderName() {
        return highestBidderName;
    }

    public double highestBid() {
        return highestBid;
    }

    public long lastBidAtMillis() {
        return lastBidAtMillis;
    }

    public int bidCount() {
        return bidCount;
    }

    public boolean hasBid() {
        return highestBidderUuid != null && highestBid > 0.0D;
    }

    public double currentPrice() {
        return hasBid() ? highestBid : price;
    }

    public boolean active() {
        return status == ListingStatus.ACTIVE;
    }

    public boolean expired(long nowMillis) {
        return active() && expiresAtMillis <= nowMillis;
    }

    public void markSold(UUID buyerUuid, String buyerName, long nowMillis) {
        status = ListingStatus.SOLD;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName == null ? "" : buyerName;
        soldAtMillis = nowMillis;
    }

    public void markActive() {
        status = ListingStatus.ACTIVE;
        buyerUuid = null;
        buyerName = "";
        soldAtMillis = 0L;
    }

    public void placeBid(UUID bidderUuid, String bidderName, double bid, long nowMillis) {
        highestBidderUuid = bidderUuid;
        highestBidderName = bidderName == null ? "" : bidderName;
        highestBid = bid;
        lastBidAtMillis = nowMillis;
        bidCount++;
    }

    public void markCancelled() {
        status = ListingStatus.CANCELLED;
    }

    public void markExpired() {
        status = ListingStatus.EXPIRED;
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        List<String> haystack = new ArrayList<>();
        haystack.add(Long.toString(id));
        haystack.add(sellerName);
        haystack.add(item.getType().name().replace('_', ' '));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                haystack.add(meta.getDisplayName());
            }
            if (meta.hasLore() && meta.getLore() != null) {
                haystack.addAll(meta.getLore());
            }
        }
        for (Enchantment enchantment : item.getEnchantments().keySet()) {
            haystack.add(enchantment.getKey().getKey());
        }
        return haystack.stream()
                .map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(normalized));
    }
}
