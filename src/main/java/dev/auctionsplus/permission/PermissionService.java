package dev.auctionsplus.permission;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PermissionService {
    private final JavaPlugin plugin;
    private Permission permissions;

    public PermissionService(JavaPlugin plugin) {
        this.plugin = plugin;
        refresh();
    }

    public void refresh() {
        RegisteredServiceProvider<Permission> registration =
                plugin.getServer().getServicesManager().getRegistration(Permission.class);
        permissions = registration == null ? null : registration.getProvider();
    }

    public boolean available() {
        return permissions != null;
    }

    public PermissionCheck check(UUIDName player, String permission) {
        Player online = Bukkit.getPlayer(player.uuid());
        if (online != null && online.isOnline()) {
            return online.hasPermission(permission) ? PermissionCheck.ALLOWED : PermissionCheck.DENIED;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.uuid());
        if (offlinePlayer.isOp()) {
            return PermissionCheck.ALLOWED;
        }
        if (permissions == null) {
            return PermissionCheck.UNAVAILABLE;
        }
        try {
            return permissions.playerHas(null, offlinePlayer, permission) ? PermissionCheck.ALLOWED : PermissionCheck.DENIED;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Could not check offline permission '" + permission + "' for "
                    + player.uuid() + ": " + e.getMessage());
            return PermissionCheck.UNAVAILABLE;
        }
    }

    public enum PermissionCheck {
        ALLOWED,
        DENIED,
        UNAVAILABLE
    }

    public record UUIDName(java.util.UUID uuid, String name) {
    }
}
