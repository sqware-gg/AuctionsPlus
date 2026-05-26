package dev.auctionsplus;

import dev.auctionsplus.command.AuctionCommand;
import dev.auctionsplus.command.AuctionsPlusCommand;
import dev.auctionsplus.api.AuctionsPlusApi;
import dev.auctionsplus.config.AuctionsPlusConfig;
import dev.auctionsplus.config.ConfigReferenceWriter;
import dev.auctionsplus.economy.EconomyService;
import dev.auctionsplus.gui.AuctionGui;
import dev.auctionsplus.gui.AuctionMenuListener;
import dev.auctionsplus.listing.AuctionService;
import dev.auctionsplus.listing.AuctionStore;
import dev.auctionsplus.permission.PermissionService;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuctionsPlusPlugin extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 31603;

    private AuctionsPlusConfig auctionsConfig;
    private AuctionStore auctionStore;
    private EconomyService economyService;
    private PermissionService permissionService;
    private AuctionService auctionService;
    private AuctionGui auctionGui;

    @Override
    public void onEnable() {
        new Metrics(this, BSTATS_PLUGIN_ID);
        ConfigReferenceWriter.saveDefaultAndReferenceIfNeeded(this);

        auctionsConfig = new AuctionsPlusConfig(this);
        auctionStore = new AuctionStore(this);
        economyService = new EconomyService(this);
        permissionService = new PermissionService(this);
        auctionService = new AuctionService(this, auctionsConfig, auctionStore, economyService, permissionService);
        auctionGui = new AuctionGui(auctionService);
        AuctionsPlusApi.register(auctionService);

        registerCommands();
        getServer().getPluginManager().registerEvents(new AuctionMenuListener(auctionService, auctionGui), this);
        auctionService.start();

        if (!economyService.available()) {
            getLogger().warning("Vault is installed, but no economy provider is registered. Selling and buying are disabled until one is available.");
        } else {
            getLogger().info("Hooked Vault economy provider: " + economyService.providerName());
        }
    }

    @Override
    public void onDisable() {
        AuctionsPlusApi.unregister();
        if (auctionService != null) {
            auctionService.stop();
        }
    }

    private void registerCommands() {
        AuctionCommand auctionCommand = new AuctionCommand(auctionService, auctionGui);
        PluginCommand ah = getCommand("ah");
        if (ah != null) {
            ah.setExecutor(auctionCommand);
            ah.setTabCompleter(auctionCommand);
        }

        AuctionsPlusCommand adminCommand = new AuctionsPlusCommand(auctionService);
        PluginCommand admin = getCommand("auctionsplus");
        if (admin != null) {
            admin.setExecutor(adminCommand);
            admin.setTabCompleter(adminCommand);
        }
    }
}
