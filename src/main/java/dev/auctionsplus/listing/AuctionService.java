package dev.auctionsplus.listing;

import dev.auctionsplus.api.event.AuctionBidPlacedEvent;
import dev.auctionsplus.api.AuctionListingView;
import dev.auctionsplus.api.event.AuctionListingCancelledEvent;
import dev.auctionsplus.api.event.AuctionListingCreatedEvent;
import dev.auctionsplus.api.event.AuctionListingExpiredEvent;
import dev.auctionsplus.api.event.AuctionListingSoldEvent;
import dev.auctionsplus.api.event.AuctionWonEvent;
import dev.auctionsplus.config.AuctionsPlusConfig;
import dev.auctionsplus.economy.EconomyService;
import dev.auctionsplus.permission.PermissionService;
import dev.auctionsplus.permission.PermissionService.PermissionCheck;
import dev.auctionsplus.permission.PermissionService.UUIDName;
import dev.auctionsplus.util.DurationFormatter;
import dev.auctionsplus.util.InventoryUtil;
import dev.auctionsplus.util.Text;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AuctionService {
    private final JavaPlugin plugin;
    private final AuctionsPlusConfig config;
    private final AuctionStore store;
    private final EconomyService economy;
    private final PermissionService permissions;
    private BukkitTask expireTask;
    private BukkitTask saveTask;

    public AuctionService(JavaPlugin plugin, AuctionsPlusConfig config, AuctionStore store, EconomyService economy,
                          PermissionService permissions) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
        this.economy = economy;
        this.permissions = permissions;
    }

    public void start() {
        expireTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expireListings,
                config.expireCheckTicks(), config.expireCheckTicks());
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, store::save,
                config.saveIntervalTicks(), config.saveIntervalTicks());
    }

    public void stop() {
        if (expireTask != null) {
            expireTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }
        store.save();
    }

    public void reload() {
        config.reload();
        economy.refresh();
        permissions.refresh();
        if (expireTask != null) {
            expireTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }
        start();
    }

    public AuctionsPlusConfig config() {
        return config;
    }

    public EconomyService economy() {
        return economy;
    }

    public synchronized AuctionActionResult sell(Player seller, double price, int amount, long durationMillis) {
        if (!economy.available()) {
            return AuctionActionResult.failure("economy-unavailable", Map.of());
        }
        if (!validPrice(price)) {
            return AuctionActionResult.failure("invalid-price", Map.of(
                    "min", economy.format(config.minPrice()),
                    "max", config.maxPrice() <= 0.0D ? "unlimited" : economy.format(config.maxPrice())
            ));
        }
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (InventoryUtil.isEmpty(hand)) {
            return AuctionActionResult.failure("no-item", Map.of());
        }
        if (config.blacklisted(hand.getType())) {
            return AuctionActionResult.failure("blacklisted-item", Map.of());
        }
        if (amount <= 0 || amount > hand.getAmount()) {
            return AuctionActionResult.failure("invalid-amount", Map.of());
        }
        if (!seller.hasPermission("auctionsplus.limit.bypass")
                && activeListings(seller.getUniqueId()).size() >= config.maxActivePerPlayer()) {
            return AuctionActionResult.failure("listing-limit", Map.of(
                    "limit", Integer.toString(config.maxActivePerPlayer())
            ));
        }

        long duration = Math.min(Math.max(1000L, durationMillis), config.maxDurationMillis());
        double listingFee = seller.hasPermission("auctionsplus.fees.bypass") ? 0.0D : config.listingFee();
        if (listingFee > 0.0D && !economy.has(seller, listingFee)) {
            return AuctionActionResult.failure("listing-fee-failed", Map.of("fee", economy.format(listingFee)));
        }
        if (listingFee > 0.0D) {
            EconomyResponse fee = economy.withdraw(seller, listingFee);
            if (!fee.transactionSuccess()) {
                return AuctionActionResult.failure("purchase-failed", Map.of("reason", errorMessage(fee)));
            }
        }

        ItemStack listedItem = hand.clone();
        listedItem.setAmount(amount);
        hand.setAmount(hand.getAmount() - amount);
        if (hand.getAmount() <= 0) {
            seller.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            seller.getInventory().setItemInMainHand(hand);
        }

        long now = System.currentTimeMillis();
        AuctionListing listing = store.createListing(seller.getUniqueId(), seller.getName(), listedItem,
                ListingType.FIXED_PRICE, price, now, now + duration);
        store.save();
        Bukkit.getPluginManager().callEvent(new AuctionListingCreatedEvent(
                listing.id(),
                seller.getUniqueId(),
                seller.getName(),
                listedItem,
                displayName(listedItem),
                listing.type().name().toLowerCase(Locale.ROOT),
                price,
                economy.format(price),
                listing.createdAtMillis(),
                listing.expiresAtMillis()
        ));
        announce("listing-created", seller, Map.of(
                "id", Long.toString(listing.id()),
                "seller", seller.getName(),
                "amount", Integer.toString(amount),
                "item", displayName(listedItem),
                "price_label", "for",
                "price", economy.format(price),
                "duration", DurationFormatter.compact(duration)
        ));
        return AuctionActionResult.success("listing-created", Map.of(
                "id", Long.toString(listing.id()),
                "amount", Integer.toString(amount),
                "item", displayName(listedItem),
                "price", economy.format(price),
                "duration", DurationFormatter.compact(duration)
        ));
    }

    public synchronized AuctionActionResult auction(Player seller, double startingBid, int amount, long durationMillis) {
        if (!config.biddingEnabled()) {
            return AuctionActionResult.failure("bidding-disabled", Map.of());
        }
        if (!economy.available()) {
            return AuctionActionResult.failure("economy-unavailable", Map.of());
        }
        if (!validPrice(startingBid)) {
            return AuctionActionResult.failure("invalid-price", Map.of(
                    "min", economy.format(config.minPrice()),
                    "max", config.maxPrice() <= 0.0D ? "unlimited" : economy.format(config.maxPrice())
            ));
        }
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (InventoryUtil.isEmpty(hand)) {
            return AuctionActionResult.failure("no-item", Map.of());
        }
        if (config.blacklisted(hand.getType())) {
            return AuctionActionResult.failure("blacklisted-item", Map.of());
        }
        if (amount <= 0 || amount > hand.getAmount()) {
            return AuctionActionResult.failure("invalid-amount", Map.of());
        }
        if (!seller.hasPermission("auctionsplus.limit.bypass")
                && activeListings(seller.getUniqueId()).size() >= config.maxActivePerPlayer()) {
            return AuctionActionResult.failure("listing-limit", Map.of(
                    "limit", Integer.toString(config.maxActivePerPlayer())
            ));
        }

        long duration = Math.min(Math.max(1000L, durationMillis), config.maxDurationMillis());
        double listingFee = seller.hasPermission("auctionsplus.fees.bypass") ? 0.0D : config.listingFee();
        if (listingFee > 0.0D && !economy.has(seller, listingFee)) {
            return AuctionActionResult.failure("listing-fee-failed", Map.of("fee", economy.format(listingFee)));
        }
        if (listingFee > 0.0D) {
            EconomyResponse fee = economy.withdraw(seller, listingFee);
            if (!fee.transactionSuccess()) {
                return AuctionActionResult.failure("purchase-failed", Map.of("reason", errorMessage(fee)));
            }
        }

        ItemStack listedItem = hand.clone();
        listedItem.setAmount(amount);
        hand.setAmount(hand.getAmount() - amount);
        if (hand.getAmount() <= 0) {
            seller.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            seller.getInventory().setItemInMainHand(hand);
        }

        long now = System.currentTimeMillis();
        AuctionListing listing = store.createListing(seller.getUniqueId(), seller.getName(), listedItem,
                ListingType.BID, startingBid, now, now + duration);
        store.save();
        Bukkit.getPluginManager().callEvent(new AuctionListingCreatedEvent(
                listing.id(),
                seller.getUniqueId(),
                seller.getName(),
                listedItem,
                displayName(listedItem),
                listing.type().name().toLowerCase(Locale.ROOT),
                startingBid,
                economy.format(startingBid),
                listing.createdAtMillis(),
                listing.expiresAtMillis()
        ));
        announce("listing-created", seller, Map.of(
                "id", Long.toString(listing.id()),
                "seller", seller.getName(),
                "amount", Integer.toString(amount),
                "item", displayName(listedItem),
                "price_label", "starting at",
                "price", economy.format(startingBid),
                "duration", DurationFormatter.compact(duration)
        ));
        return AuctionActionResult.success("auction-created", Map.of(
                "id", Long.toString(listing.id()),
                "amount", Integer.toString(amount),
                "item", displayName(listedItem),
                "price", economy.format(startingBid),
                "duration", DurationFormatter.compact(duration)
        ));
    }

    public synchronized AuctionActionResult purchase(long listingId, Player buyer) {
        if (!economy.available()) {
            return AuctionActionResult.failure("economy-unavailable", Map.of());
        }
        Optional<AuctionListing> optionalListing = store.listing(listingId);
        if (optionalListing.isEmpty()) {
            return AuctionActionResult.failure("listing-not-found", Map.of());
        }
        AuctionListing listing = optionalListing.get();
        long now = System.currentTimeMillis();
        if (listing.expired(now)) {
            expireListing(listing, now);
            store.save();
            return AuctionActionResult.failure("listing-expired", Map.of());
        }
        if (!listing.active()) {
            return AuctionActionResult.failure("listing-unavailable", Map.of());
        }
        if (listing.bidding()) {
            return AuctionActionResult.failure("listing-requires-bid", Map.of(
                    "id", Long.toString(listing.id()),
                    "min_bid", economy.format(minimumBid(listing))
            ));
        }
        if (!config.allowBuyOwnListings() && listing.sellerUuid().equals(buyer.getUniqueId())) {
            return AuctionActionResult.failure("cannot-buy-own", Map.of());
        }
        if (!economy.has(buyer, listing.price())) {
            return AuctionActionResult.failure("not-enough-money", Map.of("price", economy.format(listing.price())));
        }

        EconomyResponse withdraw = economy.withdraw(buyer, listing.price());
        if (!withdraw.transactionSuccess()) {
            return AuctionActionResult.failure("purchase-failed", Map.of("reason", errorMessage(withdraw)));
        }

        Player onlineSeller = Bukkit.getPlayer(listing.sellerUuid());
        double tax = onlineSeller != null && onlineSeller.hasPermission("auctionsplus.fees.bypass")
                ? 0.0D
                : listing.price() * (config.saleTaxPercent() / 100.0D);
        double sellerRevenue = Math.max(0.0D, listing.price() - tax);
        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.sellerUuid());
        EconomyResponse deposit = economy.deposit(seller, sellerRevenue);
        if (!deposit.transactionSuccess()) {
            economy.deposit(buyer, listing.price());
            return AuctionActionResult.failure("purchase-failed", Map.of("reason", errorMessage(deposit)));
        }

        listing.markSold(buyer.getUniqueId(), buyer.getName(), now);
        DeliveryResult delivery = deliverOrClaim(buyer.getUniqueId(), buyer.getName(), listing.id(),
                ClaimReason.PURCHASED, listing.item(), now);
        store.save();

        if (onlineSeller != null && onlineSeller.isOnline()) {
            onlineSeller.sendMessage(dev.auctionsplus.util.Text.color(config.prefix()
                    + dev.auctionsplus.util.Text.render(config.message("sale-notify"), Map.of(
                    "buyer", buyer.getName(),
                    "item", displayName(listing.item()),
                    "price", economy.format(listing.price()),
                    "tax", economy.format(tax)
            ))));
        }
        Bukkit.getPluginManager().callEvent(new AuctionListingSoldEvent(
                listing.id(),
                listing.sellerUuid(),
                listing.sellerName(),
                buyer.getUniqueId(),
                buyer.getName(),
                listing.item(),
                displayName(listing.item()),
                listing.price(),
                economy.format(listing.price()),
                sellerRevenue,
                economy.format(sellerRevenue),
                tax,
                economy.format(tax),
                now
        ));
        announce("listing-sold", buyer, Map.of(
                "id", Long.toString(listing.id()),
                "seller", listing.sellerName(),
                "buyer", buyer.getName(),
                "item", displayName(listing.item()),
                "price", economy.format(listing.price()),
                "tax", economy.format(tax)
        ));
        return AuctionActionResult.success(delivery.claimMail() ? "purchase-complete-mail" : "purchase-complete", Map.of(
                "item", displayName(listing.item()),
                "price", economy.format(listing.price())
        ));
    }

    public synchronized AuctionActionResult bid(long listingId, Player bidder, double bid) {
        if (!bidder.hasPermission("auctionsplus.bid")) {
            return AuctionActionResult.failure("no-permission", Map.of());
        }
        return bidInternal(listingId, bidder.getUniqueId(), bidder.getName(), bidder, bid);
    }

    public synchronized AuctionActionResult bidOffline(long listingId, UUID bidderUuid, String bidderName, double bid) {
        if (bidderUuid == null) {
            return AuctionActionResult.failure("invalid-player", Map.of());
        }
        String resolvedName = bidderName == null ? "" : bidderName.trim();
        if (resolvedName.isBlank()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(bidderUuid);
            resolvedName = offlinePlayer.getName() == null || offlinePlayer.getName().isBlank()
                    ? bidderUuid.toString()
                    : offlinePlayer.getName();
        }

        PermissionCheck permission = permissions.check(new UUIDName(bidderUuid, resolvedName), "auctionsplus.bid");
        if (permission == PermissionCheck.DENIED) {
            return AuctionActionResult.failure("no-permission", Map.of());
        }
        if (permission == PermissionCheck.UNAVAILABLE) {
            return AuctionActionResult.failure("permission-check-unavailable", Map.of());
        }
        return bidInternal(listingId, bidderUuid, resolvedName, Bukkit.getOfflinePlayer(bidderUuid), bid);
    }

    private AuctionActionResult bidInternal(long listingId, UUID bidderUuid, String bidderName, OfflinePlayer bidder,
                                            double bid) {
        Player onlineBidder = Bukkit.getPlayer(bidderUuid);
        if (!config.biddingEnabled()) {
            return AuctionActionResult.failure("bidding-disabled", Map.of());
        }
        if (!economy.available()) {
            return AuctionActionResult.failure("economy-unavailable", Map.of());
        }
        Optional<AuctionListing> optionalListing = store.listing(listingId);
        if (optionalListing.isEmpty()) {
            return AuctionActionResult.failure("listing-not-found", Map.of());
        }
        AuctionListing listing = optionalListing.get();
        long now = System.currentTimeMillis();
        if (listing.expired(now)) {
            expireListing(listing, now);
            store.save();
            return AuctionActionResult.failure("listing-expired", Map.of());
        }
        if (!listing.active()) {
            return AuctionActionResult.failure("listing-unavailable", Map.of());
        }
        if (!listing.bidding()) {
            return AuctionActionResult.failure("not-biddable", Map.of(
                    "id", Long.toString(listing.id()),
                    "price", economy.format(listing.price())
            ));
        }
        if (!config.allowBuyOwnListings() && listing.sellerUuid().equals(bidderUuid)) {
            return AuctionActionResult.failure("cannot-bid-own", Map.of());
        }

        double minimumBid = minimumBid(listing);
        if (!Double.isFinite(bid) || bid < minimumBid) {
            return AuctionActionResult.failure("bid-too-low", Map.of(
                    "min_bid", economy.format(minimumBid),
                    "current_bid", economy.format(listing.currentPrice())
            ));
        }
        double maxBid = config.maxBid();
        if (maxBid > 0.0D && bid > maxBid) {
            return AuctionActionResult.failure("bid-too-high", Map.of(
                    "max_bid", economy.format(maxBid)
            ));
        }

        UUID previousBidderUuid = listing.highestBidderUuid();
        String previousBidderName = listing.highestBidderName();
        double previousBid = listing.highestBid();
        double withdrawal = previousBidderUuid != null && previousBidderUuid.equals(bidderUuid)
                ? Math.max(0.0D, bid - previousBid)
                : bid;
        if (!economy.has(bidder, withdrawal)) {
            return AuctionActionResult.failure("not-enough-money", Map.of("price", economy.format(withdrawal)));
        }

        EconomyResponse withdraw = economy.withdraw(bidder, withdrawal);
        if (!withdraw.transactionSuccess()) {
            return AuctionActionResult.failure("purchase-failed", Map.of("reason", errorMessage(withdraw)));
        }

        if (previousBidderUuid != null && !previousBidderUuid.equals(bidderUuid) && previousBid > 0.0D) {
            EconomyResponse refund = economy.deposit(Bukkit.getOfflinePlayer(previousBidderUuid), previousBid);
            if (!refund.transactionSuccess()) {
                economy.deposit(bidder, withdrawal);
                return AuctionActionResult.failure("purchase-failed", Map.of("reason", errorMessage(refund)));
            }
        }

        listing.placeBid(bidderUuid, bidderName, bid, now);
        store.save();

        notifyBidPlayers(listing, bidderUuid, bidderName, previousBidderUuid, previousBidderName, bid);
        Bukkit.getPluginManager().callEvent(new AuctionBidPlacedEvent(
                listing.id(),
                listing.sellerUuid(),
                listing.sellerName(),
                bidderUuid,
                bidderName,
                previousBidderUuid,
                previousBidderName,
                listing.item(),
                displayName(listing.item()),
                bid,
                economy.format(bid),
                previousBid,
                economy.format(previousBid),
                now
        ));
        announce("bid-placed", onlineBidder, Map.of(
                "id", Long.toString(listing.id()),
                "seller", listing.sellerName(),
                "bidder", bidderName,
                "previous_bidder", previousBidderName == null ? "" : previousBidderName,
                "item", displayName(listing.item()),
                "bid", economy.format(bid),
                "previous_bid", economy.format(previousBid),
                "next_bid", economy.format(minimumBid(listing)),
                "duration", DurationFormatter.compact(listing.expiresAtMillis() - now)
        ));
        return AuctionActionResult.success("bid-complete", Map.of(
                "id", Long.toString(listing.id()),
                "item", displayName(listing.item()),
                "bid", economy.format(bid),
                "bidder", bidderName,
                "next_bid", economy.format(minimumBid(listing))
        ));
    }

    public synchronized AuctionActionResult minimumBid(long listingId, Player bidder) {
        Optional<AuctionListing> listing = store.listing(listingId);
        if (listing.isEmpty()) {
            return AuctionActionResult.failure("listing-not-found", Map.of());
        }
        return bid(listingId, bidder, minimumBid(listing.get()));
    }

    public synchronized boolean biddingListing(long listingId) {
        return store.listing(listingId)
                .map(AuctionListing::bidding)
                .orElse(false);
    }

    public synchronized AuctionActionResult cancel(long listingId, Player player, boolean admin) {
        Optional<AuctionListing> optionalListing = store.listing(listingId);
        if (optionalListing.isEmpty()) {
            return AuctionActionResult.failure("listing-not-found", Map.of());
        }
        AuctionListing listing = optionalListing.get();
        if (listing.expired(System.currentTimeMillis())) {
            expireListing(listing, System.currentTimeMillis());
            store.save();
            return AuctionActionResult.failure("listing-expired", Map.of());
        }
        if (!listing.active()) {
            return AuctionActionResult.failure("listing-unavailable", Map.of());
        }
        if (!admin && !listing.sellerUuid().equals(player.getUniqueId())) {
            return AuctionActionResult.failure("cancel-denied", Map.of());
        }
        if (listing.bidding() && listing.hasBid()) {
            EconomyResponse refund = economy.deposit(Bukkit.getOfflinePlayer(listing.highestBidderUuid()), listing.highestBid());
            if (!refund.transactionSuccess()) {
                return AuctionActionResult.failure("purchase-failed", Map.of("reason", errorMessage(refund)));
            }
        }
        ClaimReason reason = admin ? ClaimReason.ADMIN_RETURN : ClaimReason.CANCELLED;
        long now = System.currentTimeMillis();
        listing.markCancelled();
        DeliveryResult delivery = deliverOrClaim(listing.sellerUuid(), listing.sellerName(), listing.id(), reason,
                listing.item(), now);
        store.save();
        Bukkit.getPluginManager().callEvent(new AuctionListingCancelledEvent(
                listing.id(),
                listing.sellerUuid(),
                listing.sellerName(),
                player == null ? null : player.getUniqueId(),
                player == null ? "Console" : player.getName(),
                admin,
                listing.item(),
                displayName(listing.item()),
                listing.price(),
                economy.format(listing.price()),
                now
        ));
        announce("listing-cancelled", player, Map.of(
                "id", Long.toString(listing.id()),
                "seller", listing.sellerName(),
                "cancelled_by", player == null ? "Console" : player.getName(),
                "item", displayName(listing.item()),
                "price", economy.format(listing.currentPrice())
        ));
        if (admin) {
            return AuctionActionResult.success("admin-cancel-complete", Map.of(
                    "id", Long.toString(listing.id()),
                    "seller", listing.sellerName()
            ));
        }
        return AuctionActionResult.success(delivery.claimMail() ? "cancel-complete-mail" : "cancel-complete",
                Map.of("id", Long.toString(listing.id())));
    }

    public synchronized AuctionActionResult claim(long claimId, Player player) {
        Optional<ClaimItem> optionalClaim = store.claim(claimId);
        if (optionalClaim.isEmpty() || !optionalClaim.get().ownerUuid().equals(player.getUniqueId())) {
            return AuctionActionResult.failure("claim-empty", Map.of());
        }
        ClaimItem claim = optionalClaim.get();
        ItemStack item = claim.item();
        if (!InventoryUtil.canFit(player.getInventory(), item)) {
            return AuctionActionResult.failure("claim-inventory-full", Map.of());
        }
        player.getInventory().addItem(item);
        store.removeClaim(claim.id());
        store.save();
        return AuctionActionResult.success("claim-complete", Map.of("item", displayName(item)));
    }

    public synchronized void expireListings() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (AuctionListing listing : store.listings()) {
            if (listing.expired(now)) {
                expireListing(listing, now);
                changed = true;
            }
        }
        if (changed) {
            store.save();
        }
    }

    public synchronized List<AuctionListing> browseListings(String search, ListingSort sort) {
        expireListings();
        Comparator<AuctionListing> comparator = switch (sort == null ? ListingSort.NEWEST : sort) {
            case NEWEST -> Comparator.comparingLong(AuctionListing::createdAtMillis).reversed();
            case PRICE_ASC -> Comparator.comparingDouble(AuctionListing::price);
            case PRICE_DESC -> Comparator.comparingDouble(AuctionListing::price).reversed();
        };
        return store.listings().stream()
                .filter(AuctionListing::active)
                .filter(listing -> listing.matches(search))
                .sorted(comparator)
                .toList();
    }

    public synchronized AuctionListingView view(AuctionListing listing) {
        double minimumBid = listing.bidding() ? minimumBid(listing) : 0.0D;
        return new AuctionListingView(
                listing.id(),
                listing.sellerName(),
                displayName(listing.item()),
                listing.type().name().toLowerCase(Locale.ROOT),
                listing.price(),
                economy.format(listing.price()),
                listing.currentPrice(),
                economy.format(listing.currentPrice()),
                minimumBid,
                minimumBid > 0.0D ? economy.format(minimumBid) : "",
                listing.bidCount(),
                listing.createdAtMillis(),
                listing.expiresAtMillis()
        );
    }

    public synchronized List<AuctionListing> activeListings(UUID sellerUuid) {
        expireListings();
        return store.listings().stream()
                .filter(AuctionListing::active)
                .filter(listing -> listing.sellerUuid().equals(sellerUuid))
                .sorted(Comparator.comparingLong(AuctionListing::createdAtMillis).reversed())
                .toList();
    }

    public synchronized List<ClaimItem> claimItems(UUID ownerUuid) {
        return store.claims().stream()
                .filter(claim -> claim.ownerUuid().equals(ownerUuid))
                .sorted(Comparator.comparingLong(ClaimItem::createdAtMillis))
                .toList();
    }

    public synchronized int activeCount() {
        expireListings();
        return (int) store.listings().stream().filter(AuctionListing::active).count();
    }

    public int totalListings() {
        return store.totalListings();
    }

    public int totalClaims() {
        return store.totalClaims();
    }

    public void save() {
        store.save();
    }

    public String formatMoney(double amount) {
        return economy.format(amount);
    }

    public String displayName(ItemStack item) {
        if (item == null) {
            return "Unknown";
        }
        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String name = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private boolean validPrice(double price) {
        if (!Double.isFinite(price) || price < config.minPrice()) {
            return false;
        }
        return config.maxPrice() <= 0.0D || price <= config.maxPrice();
    }

    private String errorMessage(EconomyResponse response) {
        if (response == null || response.errorMessage == null || response.errorMessage.isBlank()) {
            return "unknown";
        }
        return response.errorMessage;
    }

    private void expireListing(AuctionListing listing, long now) {
        if (!listing.active()) {
            return;
        }
        if (listing.bidding() && listing.hasBid()) {
            completeBiddingAuction(listing, now);
            return;
        }
        listing.markExpired();
        DeliveryResult delivery = deliverOrClaim(listing.sellerUuid(), listing.sellerName(), listing.id(),
                ClaimReason.EXPIRED, listing.item(), now);
        Bukkit.getPluginManager().callEvent(new AuctionListingExpiredEvent(
                listing.id(),
                listing.sellerUuid(),
                listing.sellerName(),
                listing.item(),
                displayName(listing.item()),
                listing.price(),
                economy.format(listing.price()),
                now
        ));
        announce("listing-expired", null, Map.of(
                "id", Long.toString(listing.id()),
                "seller", listing.sellerName(),
                "item", displayName(listing.item()),
                "price", economy.format(listing.price())
        ));
        Player onlineSeller = Bukkit.getPlayer(listing.sellerUuid());
        if (onlineSeller != null && onlineSeller.isOnline()) {
            send(onlineSeller, delivery.claimMail() ? "listing-expired-mail" : "listing-expired-returned", Map.of(
                    "id", Long.toString(listing.id()),
                    "item", displayName(listing.item())
            ));
        }
    }

    private void completeBiddingAuction(AuctionListing listing, long now) {
        UUID winnerUuid = listing.highestBidderUuid();
        String winnerName = listing.highestBidderName();
        double winningBid = listing.highestBid();
        Player onlineSeller = Bukkit.getPlayer(listing.sellerUuid());
        Player onlineWinner = Bukkit.getPlayer(winnerUuid);
        double tax = onlineSeller != null && onlineSeller.hasPermission("auctionsplus.fees.bypass")
                ? 0.0D
                : winningBid * (config.saleTaxPercent() / 100.0D);
        double sellerRevenue = Math.max(0.0D, winningBid - tax);
        EconomyResponse deposit = economy.deposit(Bukkit.getOfflinePlayer(listing.sellerUuid()), sellerRevenue);
        if (!deposit.transactionSuccess()) {
            listing.markExpired();
            deliverOrClaim(listing.sellerUuid(), listing.sellerName(), listing.id(), ClaimReason.EXPIRED,
                    listing.item(), now);
            EconomyResponse refund = economy.deposit(Bukkit.getOfflinePlayer(winnerUuid), winningBid);
            if (!refund.transactionSuccess()) {
                plugin.getLogger().warning("Could not pay seller or refund winning bid for auction #"
                        + listing.id() + ": " + errorMessage(deposit) + "; refund: " + errorMessage(refund));
            }
            store.save();
            return;
        }

        listing.markSold(winnerUuid, winnerName, now);
        DeliveryResult delivery = deliverOrClaim(winnerUuid, winnerName, listing.id(), ClaimReason.WON,
                listing.item(), now);
        store.save();

        if (onlineWinner != null && onlineWinner.isOnline()) {
            send(onlineWinner, delivery.claimMail() ? "auction-won-buyer-mail" : "auction-won-buyer", Map.of(
                    "item", displayName(listing.item()),
                    "price", economy.format(winningBid)
            ));
        }
        if (onlineSeller != null && onlineSeller.isOnline()) {
            send(onlineSeller, "auction-won-seller", Map.of(
                    "winner", winnerName,
                    "item", displayName(listing.item()),
                    "price", economy.format(winningBid),
                    "tax", economy.format(tax)
            ));
        }

        Bukkit.getPluginManager().callEvent(new AuctionWonEvent(
                listing.id(),
                listing.sellerUuid(),
                listing.sellerName(),
                winnerUuid,
                winnerName,
                listing.item(),
                displayName(listing.item()),
                winningBid,
                economy.format(winningBid),
                sellerRevenue,
                economy.format(sellerRevenue),
                tax,
                economy.format(tax),
                now
        ));
        announce("auction-won", onlineWinner, Map.of(
                "id", Long.toString(listing.id()),
                "seller", listing.sellerName(),
                "winner", winnerName,
                "item", displayName(listing.item()),
                "price", economy.format(winningBid),
                "tax", economy.format(tax)
        ));
    }

    private void notifyBidPlayers(AuctionListing listing, UUID bidderUuid, String bidderName, UUID previousBidderUuid,
                                  String previousBidderName, double bid) {
        Player seller = Bukkit.getPlayer(listing.sellerUuid());
        if (seller != null && seller.isOnline()) {
            send(seller, "bid-seller-notify", Map.of(
                    "bidder", bidderName,
                    "item", displayName(listing.item()),
                    "bid", economy.format(bid)
            ));
        }
        if (previousBidderUuid != null && !previousBidderUuid.equals(bidderUuid)) {
            Player previousBidder = Bukkit.getPlayer(previousBidderUuid);
            if (previousBidder != null && previousBidder.isOnline()) {
                send(previousBidder, "outbid-notify", Map.of(
                        "bidder", bidderName,
                        "item", displayName(listing.item()),
                        "bid", economy.format(bid),
                        "previous_bidder", previousBidderName == null ? "" : previousBidderName
                ));
            }
        }
    }

    private double minimumBid(AuctionListing listing) {
        if (!listing.hasBid()) {
            return listing.price();
        }
        return listing.highestBid() + config.minBidIncrement();
    }

    private DeliveryResult deliverOrClaim(UUID ownerUuid, String ownerName, long sourceListingId,
                                          ClaimReason reason, ItemStack item, long now) {
        Player onlineOwner = ownerUuid == null ? null : Bukkit.getPlayer(ownerUuid);
        if (onlineOwner != null && onlineOwner.isOnline() && InventoryUtil.canFit(onlineOwner.getInventory(), item)) {
            Map<Integer, ItemStack> leftovers = onlineOwner.getInventory().addItem(item.clone());
            if (leftovers.isEmpty()) {
                return new DeliveryResult(false);
            }
            for (ItemStack leftover : leftovers.values()) {
                if (!InventoryUtil.isEmpty(leftover)) {
                    store.createClaim(ownerUuid, ownerName, sourceListingId, reason, leftover, now);
                }
            }
            plugin.getLogger().warning("Inventory changed while delivering auction item for listing #"
                    + sourceListingId + "; remaining items were moved to claim mail.");
            return new DeliveryResult(true);
        }
        store.createClaim(ownerUuid, ownerName, sourceListingId, reason, item, now);
        return new DeliveryResult(true);
    }

    private void send(Player player, String messageKey, Map<String, String> placeholders) {
        String rendered = Text.color(config.prefix() + Text.render(config.message(messageKey), placeholders));
        for (String line : rendered.split("\\R", -1)) {
            player.sendMessage(line);
        }
    }

    private void announce(String eventKey, Player source, Map<String, String> placeholders) {
        if (!config.announcementEnabled(eventKey)) {
            return;
        }
        String template = config.announcementMessage(eventKey);
        if (template == null || template.isBlank()) {
            return;
        }
        String message = Text.color(config.prefix() + Text.render(template, placeholders));
        String audience = config.announcementAudience().toLowerCase(Locale.ROOT);
        if ("none".equals(audience)) {
            return;
        }
        if ("world".equals(audience) && source != null) {
            source.getWorld().getPlayers().forEach(player -> player.sendMessage(message));
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!"permission".equals(audience) || player.hasPermission(config.announcementPermission())) {
                player.sendMessage(message);
            }
        }
    }

    private record DeliveryResult(boolean claimMail) {
    }
}
