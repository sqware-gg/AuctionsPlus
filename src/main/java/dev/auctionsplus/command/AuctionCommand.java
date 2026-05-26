package dev.auctionsplus.command;

import dev.auctionsplus.gui.AuctionGui;
import dev.auctionsplus.listing.AuctionActionResult;
import dev.auctionsplus.listing.AuctionListing;
import dev.auctionsplus.listing.AuctionService;
import dev.auctionsplus.listing.ListingSort;
import dev.auctionsplus.util.DurationParser;
import dev.auctionsplus.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AuctionCommand implements CommandExecutor, TabCompleter {
    private final AuctionService service;
    private final AuctionGui gui;

    public AuctionCommand(AuctionService service, AuctionGui gui) {
        this.service = service;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            message(sender, "players-only", Map.of());
            return true;
        }
        if (!player.hasPermission("auctionsplus.use")) {
            message(player, "no-permission", Map.of());
            return true;
        }
        if (args.length == 0) {
            gui.openBrowse(player, null, ListingSort.NEWEST, 0);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell" -> sell(player, args);
            case "auction" -> auction(player, args);
            case "buy" -> buy(player, args);
            case "bid" -> bid(player, args);
            case "active", "listings", "mine" -> gui.openActive(player, 0);
            case "claim", "claims", "mail", "expired", "won" -> openClaim(player);
            case "search" -> search(player, args);
            case "cancel" -> cancel(player, args);
            case "help", "guide", "commands", "?" -> help(player, args);
            default -> message(player, "help-ah", Map.of());
        }
        return true;
    }

    private void help(Player player, String[] args) {
        if (args.length < 2) {
            message(player, "help-ah", Map.of());
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "sell" -> message(player, "usage-sell", Map.of());
            case "auction", "auc" -> message(player, "usage-auction", Map.of());
            case "buy" -> message(player, "usage-buy", Map.of());
            case "bid" -> message(player, "usage-bid", Map.of());
            case "search" -> message(player, "usage-search", Map.of());
            case "cancel" -> message(player, "usage-cancel", Map.of());
            case "claim", "mail" -> message(player, "usage-claim", Map.of());
            default -> message(player, "help-ah", Map.of());
        }
    }

    private void sell(Player player, String[] args) {
        if (!player.hasPermission("auctionsplus.sell")) {
            message(player, "no-permission", Map.of());
            return;
        }
        if (args.length < 2 || args.length > 4) {
            message(player, "usage-sell", Map.of());
            return;
        }
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            message(player, "invalid-price", Map.of(
                    "min", service.formatMoney(service.config().minPrice()),
                    "max", service.config().maxPrice() <= 0.0D ? "unlimited" : service.formatMoney(service.config().maxPrice())
            ));
            return;
        }

        int amount = player.getInventory().getItemInMainHand().getAmount();
        long duration = service.config().defaultDurationMillis();
        if (args.length >= 3) {
            Integer parsedAmount = parseInt(args[2]);
            if (parsedAmount == null) {
                OptionalLong parsedDuration = DurationParser.parseMillis(args[2]);
                if (parsedDuration.isEmpty()) {
                    message(player, "invalid-amount-or-duration", Map.of());
                    return;
                }
                duration = parsedDuration.getAsLong();
            } else {
                amount = parsedAmount;
            }
        }
        if (args.length >= 4) {
            OptionalLong parsedDuration = DurationParser.parseMillis(args[3]);
            if (parsedDuration.isEmpty()) {
                message(player, "invalid-duration", Map.of());
                return;
            }
            duration = parsedDuration.getAsLong();
        }

        send(player, service.sell(player, price, amount, duration));
    }

    private void auction(Player player, String[] args) {
        if (!player.hasPermission("auctionsplus.auction")) {
            message(player, "no-permission", Map.of());
            return;
        }
        if (args.length < 2 || args.length > 4) {
            message(player, "usage-auction", Map.of());
            return;
        }
        double startingBid;
        try {
            startingBid = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            message(player, "invalid-price", Map.of(
                    "min", service.formatMoney(service.config().minPrice()),
                    "max", service.config().maxPrice() <= 0.0D ? "unlimited" : service.formatMoney(service.config().maxPrice())
            ));
            return;
        }

        int amount = player.getInventory().getItemInMainHand().getAmount();
        long duration = service.config().defaultDurationMillis();
        if (args.length >= 3) {
            Integer parsedAmount = parseInt(args[2]);
            if (parsedAmount == null) {
                OptionalLong parsedDuration = DurationParser.parseMillis(args[2]);
                if (parsedDuration.isEmpty()) {
                    message(player, "invalid-amount-or-duration", Map.of());
                    return;
                }
                duration = parsedDuration.getAsLong();
            } else {
                amount = parsedAmount;
            }
        }
        if (args.length >= 4) {
            OptionalLong parsedDuration = DurationParser.parseMillis(args[3]);
            if (parsedDuration.isEmpty()) {
                message(player, "invalid-duration", Map.of());
                return;
            }
            duration = parsedDuration.getAsLong();
        }

        send(player, service.auction(player, startingBid, amount, duration));
    }

    private void openClaim(Player player) {
        if (service.claimItems(player.getUniqueId()).isEmpty()) {
            message(player, "claim-empty", Map.of());
            return;
        }
        gui.openClaim(player, 0);
    }

    private void buy(Player player, String[] args) {
        if (!player.hasPermission("auctionsplus.buy")) {
            message(player, "no-permission", Map.of());
            return;
        }
        if (args.length != 2) {
            message(player, "usage-buy", Map.of());
            return;
        }
        Long listingId = parseLong(args[1]);
        if (listingId == null) {
            message(player, "invalid-listing-id", Map.of());
            return;
        }
        send(player, service.purchase(listingId, player));
    }

    private void bid(Player player, String[] args) {
        if (!player.hasPermission("auctionsplus.bid")) {
            message(player, "no-permission", Map.of());
            return;
        }
        if (args.length != 3) {
            message(player, "usage-bid", Map.of());
            return;
        }
        Long listingId = parseLong(args[1]);
        if (listingId == null) {
            message(player, "invalid-listing-id", Map.of());
            return;
        }
        Double bid = parseDouble(args[2]);
        if (bid == null) {
            message(player, "invalid-bid", Map.of());
            return;
        }
        send(player, service.bid(listingId, player, bid));
    }

    private void search(Player player, String[] args) {
        if (!player.hasPermission("auctionsplus.search")) {
            message(player, "no-permission", Map.of());
            return;
        }
        if (args.length < 2) {
            message(player, "usage-search", Map.of());
            return;
        }
        String query = String.join(" ", List.of(args).subList(1, args.length));
        gui.openBrowse(player, query, ListingSort.NEWEST, 0);
    }

    private void cancel(Player player, String[] args) {
        if (!player.hasPermission("auctionsplus.cancel")) {
            message(player, "no-permission", Map.of());
            return;
        }
        if (args.length < 2) {
            message(player, "usage-cancel", Map.of());
            return;
        }
        Long listingId = parseLong(args[1]);
        if (listingId == null) {
            message(player, "invalid-listing-id", Map.of());
            return;
        }
        send(player, service.cancel(listingId, player, false));
    }

    private Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void send(CommandSender sender, AuctionActionResult result) {
        message(sender, result.messageKey(), result.placeholders());
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
        if (!(sender instanceof Player) || !sender.hasPermission("auctionsplus.use")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("sell", "auction", "buy", "bid", "active", "claim", "search", "cancel", "help"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("help")) {
            return filter(List.of("sell", "auction", "buy", "bid", "claim", "search", "cancel"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("auction"))) {
            return filter(List.of("100", "500", "1000"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            return filter(service.browseListings(null, ListingSort.NEWEST).stream()
                    .filter(listing -> !listing.bidding())
                    .map(listing -> Long.toString(listing.id()))
                    .toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("bid")) {
            return filter(service.browseListings(null, ListingSort.NEWEST).stream()
                    .filter(AuctionListing::bidding)
                    .map(listing -> Long.toString(listing.id()))
                    .toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
            Player player = (Player) sender;
            return filter(service.activeListings(player.getUniqueId()).stream()
                    .map(listing -> Long.toString(listing.id()))
                    .toList(), args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("auction"))) {
            return filter(List.of("1", "8", "16", "32", "64", "2h", "12h", "3d"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("bid")) {
            return filter(suggestBidAmounts(args[1]), args[2]);
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("auction"))) {
            return filter(List.of("2h", "12h", "1d", "3d", "7d"), args[3]);
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

    private List<String> suggestBidAmounts(String listingInput) {
        Long listingId = parseLong(listingInput);
        if (listingId == null) {
            return List.of("100", "500", "1000");
        }
        return service.browseListings(null, ListingSort.NEWEST).stream()
                .filter(AuctionListing::bidding)
                .filter(listing -> listing.id() == listingId)
                .findFirst()
                .map(listing -> {
                    double minimum = listing.hasBid()
                            ? listing.highestBid() + service.config().minBidIncrement()
                            : listing.price();
                    String formatted = minimum == Math.rint(minimum)
                            ? Long.toString((long) minimum)
                            : Double.toString(minimum);
                    return List.of(formatted);
                })
                .orElse(List.of("100", "500", "1000"));
    }
}
