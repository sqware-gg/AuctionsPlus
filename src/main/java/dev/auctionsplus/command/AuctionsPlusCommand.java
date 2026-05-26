package dev.auctionsplus.command;

import dev.auctionsplus.listing.AuctionActionResult;
import dev.auctionsplus.listing.AuctionService;
import dev.auctionsplus.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class AuctionsPlusCommand implements CommandExecutor, TabCompleter {
    private final AuctionService service;

    public AuctionsPlusCommand(AuctionService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("auctionsplus.admin")) {
            message(sender, "no-permission", Map.of());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
            message(sender, "status", Map.of(
                    "active", Integer.toString(service.activeCount()),
                    "total", Integer.toString(service.totalListings()),
                    "claims", Integer.toString(service.totalClaims()),
                    "economy", service.economy().providerName()
            ));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                service.reload();
                message(sender, "reloaded", Map.of());
            }
            case "save" -> {
                service.save();
                message(sender, "saved", Map.of());
            }
            case "cancel" -> cancel(sender, args);
            default -> message(sender, "usage-admin", Map.of());
        }
        return true;
    }

    private void cancel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            message(sender, "usage-admin-cancel", Map.of());
            return;
        }
        Long listingId = parseLong(args[1]);
        if (listingId == null) {
            message(sender, "invalid-listing-id", Map.of());
            return;
        }
        AuctionActionResult result = service.cancel(listingId, null, true);
        message(sender, result.messageKey(), result.placeholders());
    }

    private Long parseLong(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void message(CommandSender sender, String key, Map<String, String> placeholders) {
        String rendered = Text.color(service.config().prefix()
                + Text.render(service.config().message(key), placeholders));
        for (String line : rendered.split("\\R", -1)) {
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("auctionsplus.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("stats", "reload", "save", "cancel"), args[0]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
        }
        return result;
    }
}
