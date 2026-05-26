package dev.auctionsplus.gui;

import dev.auctionsplus.listing.ListingSort;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AuctionMenuHolder implements InventoryHolder {
    private final MenuType type;
    private final int page;
    private final String search;
    private final ListingSort sort;
    private final long listingId;
    private final Map<Integer, Long> listingSlots = new HashMap<>();
    private final Map<Integer, Long> claimSlots = new HashMap<>();
    private Inventory inventory;

    public AuctionMenuHolder(MenuType type, int page, String search, ListingSort sort, long listingId) {
        this.type = type;
        this.page = page;
        this.search = search;
        this.sort = sort == null ? ListingSort.NEWEST : sort;
        this.listingId = listingId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    public MenuType type() {
        return type;
    }

    public int page() {
        return page;
    }

    public String search() {
        return search;
    }

    public ListingSort sort() {
        return sort;
    }

    public long listingId() {
        return listingId;
    }

    public void mapListing(int slot, long id) {
        listingSlots.put(slot, id);
    }

    public void mapClaim(int slot, long id) {
        claimSlots.put(slot, id);
    }

    public Long listingAt(int slot) {
        return listingSlots.get(slot);
    }

    public Long claimAt(int slot) {
        return claimSlots.get(slot);
    }
}
