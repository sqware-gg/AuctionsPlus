package dev.auctionsplus.gui;

import dev.auctionsplus.listing.AuctionActionResult;
import dev.auctionsplus.listing.AuctionService;
import dev.auctionsplus.util.Text;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class AuctionMenuListener implements Listener {
    private final AuctionService service;
    private final AuctionGui gui;

    public AuctionMenuListener(AuctionService service, AuctionGui gui) {
        this.service = service;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof AuctionMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topInventory.getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        switch (holder.type()) {
            case BROWSE -> handleBrowse(player, holder, slot);
            case CONFIRM_BUY -> handleConfirm(player, holder, slot);
            case ACTIVE -> handleActive(player, holder, slot);
            case CLAIM -> handleClaim(player, holder, slot);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AuctionMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleBrowse(Player player, AuctionMenuHolder holder, int slot) {
        Long listingId = holder.listingAt(slot);
        if (listingId != null) {
            String permission = service.biddingListing(listingId) ? "auctionsplus.bid" : "auctionsplus.buy";
            if (!player.hasPermission(permission)) {
                message(player, "no-permission", Map.of());
                return;
            }
            gui.openConfirm(player, listingId, holder.search(), holder.sort(), holder.page());
            return;
        }
        if (slot == 45 && holder.page() > 0) {
            gui.openBrowse(player, holder.search(), holder.sort(), holder.page() - 1);
        } else if (slot == 46) {
            gui.openBrowse(player, holder.search(), holder.sort().next(), holder.page());
        } else if (slot == 47) {
            gui.openActive(player, 0);
        } else if (slot == 48) {
            gui.openClaim(player, 0);
        } else if (slot == 49) {
            gui.openBrowse(player, holder.search(), holder.sort(), holder.page());
        } else if (slot == 53) {
            gui.openBrowse(player, holder.search(), holder.sort(), holder.page() + 1);
        }
    }

    private void handleConfirm(Player player, AuctionMenuHolder holder, int slot) {
        if (slot == 11) {
            AuctionActionResult result = service.biddingListing(holder.listingId())
                    ? service.minimumBid(holder.listingId(), player)
                    : service.purchase(holder.listingId(), player);
            send(player, result);
            gui.openBrowse(player, holder.search(), holder.sort(), holder.page());
        } else if (slot == 15) {
            gui.openBrowse(player, holder.search(), holder.sort(), holder.page());
        }
    }

    private void handleActive(Player player, AuctionMenuHolder holder, int slot) {
        Long listingId = holder.listingAt(slot);
        if (listingId != null) {
            if (!player.hasPermission("auctionsplus.cancel")) {
                message(player, "no-permission", Map.of());
                return;
            }
            send(player, service.cancel(listingId, player, false));
            gui.openActive(player, holder.page());
            return;
        }
        if (slot == 45 && holder.page() > 0) {
            gui.openActive(player, holder.page() - 1);
        } else if (slot == 49) {
            gui.openActive(player, holder.page());
        } else if (slot == 53) {
            gui.openActive(player, holder.page() + 1);
        }
    }

    private void handleClaim(Player player, AuctionMenuHolder holder, int slot) {
        Long claimId = holder.claimAt(slot);
        if (claimId != null) {
            if (!player.hasPermission("auctionsplus.claim")) {
                message(player, "no-permission", Map.of());
                return;
            }
            send(player, service.claim(claimId, player));
            gui.openClaim(player, holder.page());
            return;
        }
        if (slot == 45 && holder.page() > 0) {
            gui.openClaim(player, holder.page() - 1);
        } else if (slot == 49) {
            gui.openClaim(player, holder.page());
        } else if (slot == 53) {
            gui.openClaim(player, holder.page() + 1);
        }
    }

    private void send(Player player, AuctionActionResult result) {
        message(player, result.messageKey(), result.placeholders());
    }

    private void message(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(Text.color(service.config().prefix()
                + Text.render(service.config().message(key), placeholders)));
    }
}
