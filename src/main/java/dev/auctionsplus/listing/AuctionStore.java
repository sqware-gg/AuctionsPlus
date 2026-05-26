package dev.auctionsplus.listing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuctionStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<Long, AuctionListing> listings = new LinkedHashMap<>();
    private final Map<Long, ClaimItem> claims = new LinkedHashMap<>();
    private long nextListingId = 1L;
    private long nextClaimId = 1L;

    public AuctionStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auctions.yml");
        reload();
    }

    public synchronized void reload() {
        listings.clear();
        claims.clear();
        nextListingId = 1L;
        nextClaimId = 1L;
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextListingId = Math.max(1L, yaml.getLong("meta.next-listing-id", 1L));
        nextClaimId = Math.max(1L, yaml.getLong("meta.next-claim-id", 1L));
        loadListings(yaml.getConfigurationSection("listings"));
        loadClaims(yaml.getConfigurationSection("claims"));
        for (Long id : listings.keySet()) {
            nextListingId = Math.max(nextListingId, id + 1L);
        }
        for (Long id : claims.keySet()) {
            nextClaimId = Math.max(nextClaimId, id + 1L);
        }
    }

    private void loadListings(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection listingSection = section.getConfigurationSection(key);
            if (listingSection == null) {
                continue;
            }
            try {
                long id = Long.parseLong(key);
                UUID sellerUuid = UUID.fromString(listingSection.getString("seller.uuid", ""));
                String sellerName = listingSection.getString("seller.name", "");
                ItemStack item = listingSection.getItemStack("item");
                if (item == null) {
                    plugin.getLogger().warning("Ignoring listing " + key + " because it has no item.");
                    continue;
                }
                ListingStatus status = parseStatus(listingSection.getString("status", "ACTIVE"));
                UUID buyerUuid = parseUuid(listingSection.getString("buyer.uuid", "")).orElse(null);
                ListingType type = parseType(listingSection.getString("type", "FIXED_PRICE"));
                UUID highestBidderUuid = parseUuid(listingSection.getString("highest-bidder.uuid", "")).orElse(null);
                AuctionListing listing = new AuctionListing(
                        id,
                        sellerUuid,
                        sellerName,
                        item,
                        type,
                        listingSection.getDouble("price", 0.0D),
                        listingSection.getLong("created-at", 0L),
                        listingSection.getLong("expires-at", 0L),
                        status,
                        buyerUuid,
                        listingSection.getString("buyer.name", ""),
                        listingSection.getLong("sold-at", 0L),
                        highestBidderUuid,
                        listingSection.getString("highest-bidder.name", ""),
                        listingSection.getDouble("highest-bid", 0.0D),
                        listingSection.getLong("last-bid-at", 0L),
                        listingSection.getInt("bid-count", 0)
                );
                listings.put(id, listing);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid listing " + key + ": " + e.getMessage());
            }
        }
    }

    private void loadClaims(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection claimSection = section.getConfigurationSection(key);
            if (claimSection == null) {
                continue;
            }
            try {
                long id = Long.parseLong(key);
                UUID ownerUuid = UUID.fromString(claimSection.getString("owner.uuid", ""));
                ItemStack item = claimSection.getItemStack("item");
                if (item == null) {
                    plugin.getLogger().warning("Ignoring claim " + key + " because it has no item.");
                    continue;
                }
                ClaimReason reason = parseClaimReason(claimSection.getString("reason", "EXPIRED"));
                ClaimItem claim = new ClaimItem(
                        id,
                        ownerUuid,
                        claimSection.getString("owner.name", ""),
                        claimSection.getLong("source-listing-id", 0L),
                        reason,
                        claimSection.getLong("created-at", 0L),
                        item
                );
                claims.put(id, claim);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid claim " + key + ": " + e.getMessage());
            }
        }
    }

    public synchronized AuctionListing createListing(UUID sellerUuid, String sellerName, ItemStack item, double price,
                                                     long createdAtMillis, long expiresAtMillis) {
        return createListing(sellerUuid, sellerName, item, ListingType.FIXED_PRICE, price, createdAtMillis, expiresAtMillis);
    }

    public synchronized AuctionListing createListing(UUID sellerUuid, String sellerName, ItemStack item,
                                                     ListingType type, double price, long createdAtMillis,
                                                     long expiresAtMillis) {
        AuctionListing listing = new AuctionListing(nextListingId++, sellerUuid, sellerName, item, type, price,
                createdAtMillis, expiresAtMillis, ListingStatus.ACTIVE, null, "", 0L,
                null, "", 0.0D, 0L, 0);
        listings.put(listing.id(), listing);
        return listing;
    }

    public synchronized ClaimItem createClaim(UUID ownerUuid, String ownerName, long sourceListingId,
                                              ClaimReason reason, ItemStack item, long createdAtMillis) {
        ClaimItem claim = new ClaimItem(nextClaimId++, ownerUuid, ownerName, sourceListingId, reason,
                createdAtMillis, item);
        claims.put(claim.id(), claim);
        return claim;
    }

    public synchronized Optional<AuctionListing> listing(long id) {
        return Optional.ofNullable(listings.get(id));
    }

    public synchronized Optional<ClaimItem> claim(long id) {
        return Optional.ofNullable(claims.get(id));
    }

    public synchronized void removeClaim(long id) {
        claims.remove(id);
    }

    public synchronized Collection<AuctionListing> listings() {
        return new ArrayList<>(listings.values());
    }

    public synchronized Collection<ClaimItem> claims() {
        return new ArrayList<>(claims.values());
    }

    public synchronized int totalListings() {
        return listings.size();
    }

    public synchronized int totalClaims() {
        return claims.size();
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("meta.next-listing-id", nextListingId);
        yaml.set("meta.next-claim-id", nextClaimId);
        for (AuctionListing listing : listings.values()) {
            String path = "listings." + listing.id();
            yaml.set(path + ".seller.uuid", listing.sellerUuid().toString());
            yaml.set(path + ".seller.name", listing.sellerName());
            yaml.set(path + ".item", listing.item());
            yaml.set(path + ".type", listing.type().name());
            yaml.set(path + ".price", listing.price());
            yaml.set(path + ".created-at", listing.createdAtMillis());
            yaml.set(path + ".expires-at", listing.expiresAtMillis());
            yaml.set(path + ".status", listing.status().name());
            yaml.set(path + ".buyer.uuid", listing.buyerUuid() == null ? "" : listing.buyerUuid().toString());
            yaml.set(path + ".buyer.name", listing.buyerName());
            yaml.set(path + ".sold-at", listing.soldAtMillis());
            yaml.set(path + ".highest-bidder.uuid", listing.highestBidderUuid() == null ? "" : listing.highestBidderUuid().toString());
            yaml.set(path + ".highest-bidder.name", listing.highestBidderName());
            yaml.set(path + ".highest-bid", listing.highestBid());
            yaml.set(path + ".last-bid-at", listing.lastBidAtMillis());
            yaml.set(path + ".bid-count", listing.bidCount());
        }
        for (ClaimItem claim : claims.values()) {
            String path = "claims." + claim.id();
            yaml.set(path + ".owner.uuid", claim.ownerUuid().toString());
            yaml.set(path + ".owner.name", claim.ownerName());
            yaml.set(path + ".source-listing-id", claim.sourceListingId());
            yaml.set(path + ".reason", claim.reason().name());
            yaml.set(path + ".created-at", claim.createdAtMillis());
            yaml.set(path + ".item", claim.item());
        }
        try {
            Files.createDirectories(file.toPath().getParent());
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save auctions.yml: " + e.getMessage());
        }
    }

    private ListingStatus parseStatus(String value) {
        try {
            return ListingStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ListingStatus.ACTIVE;
        }
    }

    private ListingType parseType(String value) {
        try {
            if (value == null || value.isBlank()) {
                return ListingType.FIXED_PRICE;
            }
            return ListingType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ListingType.FIXED_PRICE;
        }
    }

    private ClaimReason parseClaimReason(String value) {
        try {
            return ClaimReason.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ClaimReason.EXPIRED;
        }
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
