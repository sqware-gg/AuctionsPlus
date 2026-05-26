package dev.auctionsplus.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    public static boolean canFit(Inventory inventory, ItemStack item) {
        if (isEmpty(item)) {
            return true;
        }
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();
        for (ItemStack slot : inventory.getStorageContents()) {
            if (isEmpty(slot)) {
                remaining -= maxStack;
            } else if (slot.isSimilar(item)) {
                remaining -= Math.max(0, maxStack - slot.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }
}
