package dev.auctionsplus.gui;

import dev.auctionsplus.listing.AuctionListing;
import dev.auctionsplus.listing.AuctionService;
import dev.auctionsplus.listing.ClaimItem;
import dev.auctionsplus.listing.ListingSort;
import dev.auctionsplus.util.DurationFormatter;
import dev.auctionsplus.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AuctionGui {
    private static final int MENU_SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private final AuctionService service;

    public AuctionGui(AuctionService service) {
        this.service = service;
    }

    public void openBrowse(Player player, String search, ListingSort sort, int requestedPage) {
        List<AuctionListing> listings = service.browseListings(search, sort);
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) PAGE_SIZE));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.BROWSE, page, search, sort, 0L);
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE,
                Text.color(service.config().guiTitle("title-browse")));
        holder.attach(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, listings.size());
        for (int index = start; index < end; index++) {
            int slot = index - start;
            AuctionListing listing = listings.get(index);
            inventory.setItem(slot, listingItem(listing));
            holder.mapListing(slot, listing.id());
        }
        if (listings.isEmpty()) {
            inventory.setItem(22, icon(Material.BARRIER, "&#ED4245No listings", List.of("&7Try another search.")));
        }

        inventory.setItem(45, icon(Material.ARROW, "&#2b98fdPrevious Page", List.of("&7Page &f" + (page + 1) + "&8/&f" + pages)));
        inventory.setItem(46, icon(Material.HOPPER, "&7Sort: &#2b98fd" + (sort == null ? ListingSort.NEWEST : sort).label(),
                List.of("&7Click to cycle sort mode.")));
        inventory.setItem(47, icon(Material.CHEST, "&#2b98fdYour Listings", List.of("&7View and cancel active listings.")));
        inventory.setItem(48, icon(Material.ENDER_CHEST, "&#2b98fdClaim Mail", List.of("&7Collect purchased or returned items.")));
        inventory.setItem(49, icon(Material.SUNFLOWER, "&#2b98fdRefresh", List.of("&7Reload this auction page.")));
        String searchText = search == null || search.isBlank() ? "None" : search;
        inventory.setItem(50, icon(Material.NAME_TAG, "&7Search: &#2b98fd" + searchText, List.of("&7Use &#2b98fd/ah search <text>&7.")));
        inventory.setItem(53, icon(Material.ARROW, "&#2b98fdNext Page", List.of("&7Page &f" + (page + 1) + "&8/&f" + pages)));

        player.openInventory(inventory);
    }

    public void openActive(Player player, int requestedPage) {
        List<AuctionListing> listings = service.activeListings(player.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) PAGE_SIZE));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.ACTIVE, page, null, ListingSort.NEWEST, 0L);
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE,
                Text.color(service.config().guiTitle("title-active")));
        holder.attach(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, listings.size());
        for (int index = start; index < end; index++) {
            int slot = index - start;
            AuctionListing listing = listings.get(index);
            inventory.setItem(slot, listingItem(listing, List.of("&#ED4245Click to cancel and return to claim mail.")));
            holder.mapListing(slot, listing.id());
        }
        if (listings.isEmpty()) {
            inventory.setItem(22, icon(Material.BARRIER, "&#ED4245No active listings", List.of("&7Use &#2b98fd/ah sell &7to list an item.")));
        }
        inventory.setItem(45, icon(Material.ARROW, "&#2b98fdPrevious Page", List.of("&7Page &f" + (page + 1) + "&8/&f" + pages)));
        inventory.setItem(49, icon(Material.SUNFLOWER, "&#2b98fdRefresh", List.of("&7Reload your listings.")));
        inventory.setItem(53, icon(Material.ARROW, "&#2b98fdNext Page", List.of("&7Page &f" + (page + 1) + "&8/&f" + pages)));
        player.openInventory(inventory);
    }

    public void openClaim(Player player, int requestedPage) {
        List<ClaimItem> claims = service.claimItems(player.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(claims.size() / (double) PAGE_SIZE));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.CLAIM, page, null, ListingSort.NEWEST, 0L);
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE,
                Text.color(service.config().guiTitle("title-claim")));
        holder.attach(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, claims.size());
        for (int index = start; index < end; index++) {
            int slot = index - start;
            ClaimItem claim = claims.get(index);
            inventory.setItem(slot, claimItem(claim));
            holder.mapClaim(slot, claim.id());
        }
        if (claims.isEmpty()) {
            inventory.setItem(22, icon(Material.BARRIER, "&#ED4245No claim mail", List.of("&7Purchased and returned items appear here.")));
        }
        inventory.setItem(45, icon(Material.ARROW, "&#2b98fdPrevious Page", List.of("&7Page &f" + (page + 1) + "&8/&f" + pages)));
        inventory.setItem(49, icon(Material.SUNFLOWER, "&#2b98fdRefresh", List.of("&7Reload claim mail.")));
        inventory.setItem(53, icon(Material.ARROW, "&#2b98fdNext Page", List.of("&7Page &f" + (page + 1) + "&8/&f" + pages)));
        player.openInventory(inventory);
    }

    public void openConfirm(Player player, long listingId, String search, ListingSort sort, int page) {
        AuctionMenuHolder holder = new AuctionMenuHolder(MenuType.CONFIRM_BUY, page, search, sort, listingId);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Text.color(service.config().guiTitle("title-confirm")));
        holder.attach(inventory);
        service.browseListings(search, sort).stream()
                .filter(listing -> listing.id() == listingId)
                .findFirst()
                .ifPresent(listing -> {
                    inventory.setItem(13, listingItem(listing));
                    if (listing.bidding()) {
                        double minBid = listing.hasBid() ? listing.highestBid() + service.config().minBidIncrement() : listing.price();
                        inventory.setItem(11, icon(Material.LIME_CONCRETE, "&#57F287Confirm Bid",
                                List.of("&7Bid &#2b98fd" + service.formatMoney(minBid) + "&7 on this auction.")));
                    } else {
                        inventory.setItem(11, icon(Material.LIME_CONCRETE, "&#57F287Confirm Purchase", List.of("&7Click to buy this listing.")));
                    }
                });
        inventory.setItem(15, icon(Material.RED_CONCRETE, "&#ED4245Cancel", List.of("&7Return to the auction browser.")));
        player.openInventory(inventory);
    }

    private ItemStack listingItem(AuctionListing listing) {
        return listingItem(listing, listing.bidding()
                ? List.of("&#57F287Click to bid the minimum.")
                : List.of("&#57F287Click to buy."));
    }

    private ItemStack listingItem(AuctionListing listing, List<String> extraLore) {
        ItemStack item = listing.item();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.add("");
        lore.add("&7Listing: &#2b98fd#" + listing.id());
        lore.add("&7Seller: &f" + listing.sellerName());
        if (listing.bidding()) {
            double minBid = listing.hasBid() ? listing.highestBid() + service.config().minBidIncrement() : listing.price();
            lore.add("&7Type: &#2b98fdBidding");
            lore.add("&7Starting Bid: &#2b98fd" + service.formatMoney(listing.price()));
            lore.add("&7Current Bid: &#2b98fd" + (listing.hasBid() ? service.formatMoney(listing.highestBid()) : "None"));
            lore.add("&7Next Bid: &#2b98fd" + service.formatMoney(minBid));
            lore.add("&7Highest Bidder: &f" + (listing.hasBid() ? listing.highestBidderName() : "None"));
            lore.add("&7Bids: &f" + listing.bidCount());
        } else {
            lore.add("&7Type: &#2b98fdBuy Now");
            lore.add("&7Price: &#2b98fd" + service.formatMoney(listing.price()));
        }
        lore.add("&7Expires: &f" + DurationFormatter.compact(listing.expiresAtMillis() - System.currentTimeMillis()));
        lore.addAll(extraLore);
        meta.setLore(color(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack claimItem(ClaimItem claim) {
        ItemStack item = claim.item();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.add("");
        lore.add("&7Claim: &#2b98fd#" + claim.id());
        lore.add("&7Source Listing: &#2b98fd#" + claim.sourceListingId());
        lore.add("&7Reason: &f" + claim.reason().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        lore.add("&#57F287Click to claim.");
        meta.setLore(color(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            meta.setLore(color(lore));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> color(List<String> lore) {
        return lore.stream().map(Text::color).toList();
    }
}
