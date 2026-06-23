package dev.auctionsplus.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigReferenceWriter {
    private static final int CURRENT_CONFIG_VERSION = 10;

    private ConfigReferenceWriter() {
    }

    public static void saveDefaultAndReferenceIfNeeded(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        int installedVersion = plugin.getConfig().getInt("config-version", 0);
        if (installedVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        Path referencePath = plugin.getDataFolder().toPath().resolve("config-new.yml");
        try (InputStream inputStream = plugin.getResource("config.yml")) {
            if (inputStream == null) {
                plugin.getLogger().warning("Could not find bundled config.yml for update reference.");
                return;
            }
            byte[] bundledConfig = inputStream.readAllBytes();
            if (Files.exists(referencePath) && Arrays.equals(Files.readAllBytes(referencePath), bundledConfig)) {
                plugin.getLogger().warning("Your config.yml is outdated or missing config-version.");
                plugin.getLogger().warning("config-new.yml already exists with the latest reference config. Your current config was not changed.");
                return;
            }
            Files.createDirectories(referencePath.getParent());
            Files.write(referencePath, bundledConfig);
            plugin.getLogger().warning("Your config.yml is outdated or missing config-version.");
            plugin.getLogger().warning("A new reference config was saved to config-new.yml. Your current config was not changed.");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save config-new.yml: " + e.getMessage());
        }
    }
}
