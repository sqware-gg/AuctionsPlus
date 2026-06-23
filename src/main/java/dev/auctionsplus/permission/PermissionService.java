package dev.auctionsplus.permission;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PermissionService {
    private final JavaPlugin plugin;
    private Permission permissions;
    private boolean warnedLuckPerms;
    private boolean warnedVaultGroups;

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

    public Set<String> rankNames(Player player) {
        Set<String> ranks = new LinkedHashSet<>();
        addLuckPermsPrimaryGroup(player, ranks);
        addVaultGroups(player, ranks);
        return ranks;
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

    private void addLuckPermsPrimaryGroup(Player player, Set<String> ranks) {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return;
        }
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(luckPermsClass);
            if (registration == null) {
                return;
            }
            Object luckPerms = registration.getProvider();
            Object userManager = invoke(luckPerms, "getUserManager");
            Object user = invoke(userManager, "getUser", player.getUniqueId());
            if (user == null) {
                return;
            }
            addRank(ranks, stringValue(invoke(user, "getPrimaryGroup")));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (!warnedLuckPerms) {
                warnedLuckPerms = true;
                plugin.getLogger().warning("Could not read LuckPerms rank data: " + e.getMessage());
            }
        }
    }

    private void addVaultGroups(Player player, Set<String> ranks) {
        if (player == null || permissions == null) {
            return;
        }
        try {
            addRank(ranks, permissions.getPrimaryGroup(player));
            for (String group : permissions.getPlayerGroups(player)) {
                addRank(ranks, group);
            }
        } catch (RuntimeException e) {
            if (!warnedVaultGroups) {
                warnedVaultGroups = true;
                plugin.getLogger().warning("Could not read Vault permission groups: " + e.getMessage());
            }
        }
    }

    private void addRank(Set<String> ranks, String rank) {
        if (rank == null || rank.isBlank()) {
            return;
        }
        ranks.add(rank.toLowerCase(Locale.ROOT));
    }

    private Object invoke(Object target, String method, Object... arguments) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method match = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (candidate.getName().equals(method) && compatible(candidate.getParameterTypes(), arguments)) {
                match = candidate;
                break;
            }
        }
        if (match == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + method);
        }
        try {
            return match.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException reflective) {
                throw reflective;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw e;
        }
    }

    private boolean compatible(Class<?>[] parameterTypes, Object[] arguments) {
        if (parameterTypes.length != arguments.length) {
            return false;
        }
        for (int index = 0; index < parameterTypes.length; index++) {
            Object argument = arguments[index];
            if (argument != null && !wrap(parameterTypes[index]).isInstance(argument)) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    public enum PermissionCheck {
        ALLOWED,
        DENIED,
        UNAVAILABLE
    }

    public record UUIDName(java.util.UUID uuid, String name) {
    }
}
