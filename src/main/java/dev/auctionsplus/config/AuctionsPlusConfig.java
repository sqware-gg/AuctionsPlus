package dev.auctionsplus.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuctionsPlusConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration defaultConfig;
    private Set<Material> blacklistedMaterials = EnumSet.noneOf(Material.class);
    private Map<String, Integer> activeLimitByRank = Map.of();

    public AuctionsPlusConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        defaultConfig = loadBundledConfig();
        activeLimitByRank = rankLimits("listings.rank-limits");
        blacklistedMaterials = EnumSet.noneOf(Material.class);
        for (String materialName : config.getStringList("listings.blacklisted-materials")) {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Ignoring invalid blacklisted material: " + materialName);
                continue;
            }
            blacklistedMaterials.add(material);
        }
    }

    public double minPrice() {
        return Math.max(0.0D, config.getDouble("economy.min-price", 1.0D));
    }

    public double maxPrice() {
        return Math.max(0.0D, config.getDouble("economy.max-price", 1000000000.0D));
    }

    public boolean allowBuyOwnListings() {
        return config.getBoolean("economy.allow-buy-own-listings", false);
    }

    public boolean biddingEnabled() {
        return config.getBoolean("bidding.enabled", true);
    }

    public double minBidIncrement() {
        return Math.max(0.01D, config.getDouble("bidding.min-increment-flat", 1.0D));
    }

    public double maxBid() {
        return Math.max(0.0D, config.getDouble("bidding.max-bid", maxPrice()));
    }

    public long defaultDurationMillis() {
        return Math.max(1L, config.getLong("listings.default-duration-hours", 72L)) * 3600000L;
    }

    public long maxDurationMillis() {
        return Math.max(1L, config.getLong("listings.max-duration-hours", 168L)) * 3600000L;
    }

    public int maxActivePerPlayer() {
        return Math.max(1, config.getInt("listings.max-active-per-player", 20));
    }

    public Map<String, Integer> activeLimitByRank() {
        return activeLimitByRank;
    }

    public long expireCheckTicks() {
        return Math.max(20L, config.getLong("listings.expire-check-seconds", 60L) * 20L);
    }

    public long saveIntervalTicks() {
        return Math.max(20L, config.getLong("listings.save-interval-seconds", 300L) * 20L);
    }

    public boolean blacklisted(Material material) {
        return blacklistedMaterials.contains(material);
    }

    public double listingFee() {
        return Math.max(0.0D, config.getDouble("fees.listing-flat", 0.0D));
    }

    public double saleTaxPercent() {
        return Math.max(0.0D, Math.min(100.0D, config.getDouble("fees.sale-tax-percent", 0.0D)));
    }

    public boolean announcementsEnabled() {
        return config.getBoolean("announcements.enabled", defaultConfig.getBoolean("announcements.enabled", true));
    }

    public String announcementAudience() {
        return config.getString("announcements.audience", defaultConfig.getString("announcements.audience", "permission"));
    }

    public String announcementPermission() {
        return config.getString("announcements.permission",
                defaultConfig.getString("announcements.permission", "auctionsplus.notify"));
    }

    public boolean announcementEnabled(String eventKey) {
        String path = "announcements.events." + eventKey;
        return announcementsEnabled() && config.getBoolean(path, defaultConfig.getBoolean(path, false));
    }

    public String announcementMessage(String eventKey) {
        String path = "announcements.messages." + eventKey;
        return config.getString(path, defaultConfig.getString(path, ""));
    }

    public String guiTitle(String key) {
        return config.getString("gui." + key.toLowerCase(Locale.ROOT), "");
    }

    public String prefix() {
        return message("prefix");
    }

    public String message(String key) {
        String path = "messages." + key;
        String message = config.getString(path);
        if (message != null && !message.isBlank()) {
            return message;
        }
        message = defaultConfig.getString(path);
        if (message != null && !message.isBlank()) {
            return message;
        }
        return "Missing message: " + key;
    }

    private FileConfiguration loadBundledConfig() {
        try (InputStream inputStream = plugin.getResource("config.yml")) {
            if (inputStream == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load bundled config defaults: " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    private Map<String, Integer> rankLimits(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return Map.of();
        }
        Map<String, Integer> limits = new LinkedHashMap<>();
        for (String rank : section.getKeys(false)) {
            if (rank == null || rank.isBlank()) {
                continue;
            }
            int limit = section.getInt(rank, -1);
            if (limit <= 0) {
                plugin.getLogger().warning("Ignoring invalid rank limit at " + path + "." + rank + ": " + limit);
                continue;
            }
            limits.put(rank.toLowerCase(Locale.ROOT), limit);
        }
        return Collections.unmodifiableMap(limits);
    }
}
