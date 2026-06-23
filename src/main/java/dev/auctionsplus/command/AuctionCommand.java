package dev.auctionsplus.command;

import dev.auctionsplus.gui.AuctionGui;
import dev.auctionsplus.listing.AuctionActionResult;
import dev.auctionsplus.listing.AuctionListing;
import dev.auctionsplus.listing.AuctionService;
import dev.auctionsplus.listing.ClaimItem;
import dev.auctionsplus.listing.ListingSort;
import dev.auctionsplus.util.DurationFormatter;
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
import org.bukkit.inventory.ItemStack;

public final class AuctionCommand implements CommandExecutor, TabCompleter {
    private static final int TEXT_PAGE_SIZE = 8;

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
            case "list", "browse", "text" -> listListings(player, args);
            case "sell" -> sell(player, args);
            case "auction" -> auction(player, args);
            case "buy" -> buy(player, args);
            case "bid" -> bid(player, args);
            case "active", "listings", "mine" -> active(player, args);
            case "claim", "claims", "mail", "expired", "won" -> claim(player, args);
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
            case "list", "browse" -> line(player, "&7Use &#2b98fd/ah list [search] [page]&7.");
            case "active" -> line(player, "&7Use &#2b98fd/ah active list [page]&7 for text, or &f/ah active&7 for GUI.");
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

    private void listListings(Player player, String[] args) {
        int end = args.length;
        int page = 1;
        if (end > 1) {
            Integer parsedPage = parseInt(args[end - 1]);
            if (parsedPage != null && parsedPage > 0) {
                page = parsedPage;
                end--;
            }
        }
        String query = end > 1 ? String.join(" ", List.of(args).subList(1, end)).trim() : "";
        if (!query.isBlank() && !player.hasPermission("auctionsplus.search")) {
            message(player, "no-permission", Map.of());
            return;
        }
        String title = query.isBlank() ? "Auction Listings" : "Auction Listings matching \"" + query + "\"";
        String baseCommand = query.isBlank() ? "/ah list" : "/ah list " + query;
        sendListingPage(player, title, service.browseListings(query, ListingSort.NEWEST), page, baseCommand);
    }

    private void active(Player player, String[] args) {
        if (args.length >= 2 && textListArgument(args[1])) {
            int page = 1;
            if (args.length >= 3) {
                Integer parsedPage = parseInt(args[2]);
                if (parsedPage == null || parsedPage <= 0) {
                    line(player, "&#ED4245Use a positive page number.");
                    return;
                }
                page = parsedPage;
            }
            sendListingPage(player, "Your Active Listings", service.activeListings(player.getUniqueId()), page,
                    "/ah active list");
            return;
        }
        gui.openActive(player, 0);
    }

    private void openClaim(Player player) {
        if (service.claimItems(player.getUniqueId()).isEmpty()) {
            message(player, "claim-empty", Map.of());
            return;
        }
        gui.openClaim(player, 0);
    }

    private void claim(Player player, String[] args) {
        if (!player.hasPermission("auctionsplus.claim")) {
            message(player, "no-permission", Map.of());
            return;
        }
        if (args.length == 1) {
            openClaim(player);
            return;
        }
        if (args.length != 2) {
            message(player, "usage-claim", Map.of());
            return;
        }
        if (textListArgument(args[1])) {
            listClaims(player);
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            claimAll(player);
            return;
        }
        Long claimId = parseLong(args[1]);
        if (claimId == null) {
            message(player, "usage-claim", Map.of());
            return;
        }
        send(player, service.claim(claimId, player));
    }

    private void listClaims(Player player) {
        List<ClaimItem> claims = service.claimItems(player.getUniqueId());
        if (claims.isEmpty()) {
            message(player, "claim-empty", Map.of());
            return;
        }
        line(player, "&fClaim Mail &8(&7" + claims.size() + "&8)");
        for (ClaimItem claim : claims.stream().limit(TEXT_PAGE_SIZE).toList()) {
            line(player, "&#2b98fd#" + claim.id() + " &8- &f" + itemLabel(claim.item())
                    + " &7from listing &f#" + claim.sourceListingId()
                    + " &8(" + claim.reason().name().toLowerCase(Locale.ROOT).replace('_', ' ') + "&8)"
                    + " &8- &7/ah claim " + claim.id());
        }
        if (claims.size() > TEXT_PAGE_SIZE) {
            line(player, "&7Showing " + TEXT_PAGE_SIZE + " of &f" + claims.size()
                    + "&7 claim items. Use &f/ah claim all&7 to collect all possible items.");
        }
    }

    private void claimAll(Player player) {
        List<ClaimItem> claims = service.claimItems(player.getUniqueId());
        if (claims.isEmpty()) {
            message(player, "claim-empty", Map.of());
            return;
        }
        int claimed = 0;
        for (ClaimItem claim : claims) {
            AuctionActionResult result = service.claim(claim.id(), player);
            if (!result.success()) {
                if (claimed == 0) {
                    send(player, result);
                } else {
                    line(player, "&7Stopped after claiming &f" + claimed + "&7 item(s).");
                    send(player, result);
                }
                return;
            }
            claimed++;
        }
        line(player, "&7Claimed &f" + claimed + "&7 item(s).");
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

    private void sendListingPage(Player player, String title, List<AuctionListing> listings, int requestedPage,
                                 String baseCommand) {
        if (listings.isEmpty()) {
            line(player, "&#ED4245No listings found.");
            return;
        }
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) TEXT_PAGE_SIZE));
        int page = Math.max(1, Math.min(requestedPage, pages));
        int start = (page - 1) * TEXT_PAGE_SIZE;
        int end = Math.min(start + TEXT_PAGE_SIZE, listings.size());
        line(player, "&f" + title + " &8(&7page " + page + "/" + pages + ", " + listings.size() + " total&8)");
        long now = System.currentTimeMillis();
        for (AuctionListing listing : listings.subList(start, end)) {
            line(player, listingLine(listing, now));
        }
        if (page < pages) {
            line(player, "&7Next page: &f" + baseCommand + " " + (page + 1));
        }
    }

    private String listingLine(AuctionListing listing, long now) {
        String item = itemLabel(listing.item());
        String time = DurationFormatter.compact(Math.max(0L, listing.expiresAtMillis() - now));
        if (listing.bidding()) {
            double nextBid = listing.hasBid()
                    ? listing.highestBid() + service.config().minBidIncrement()
                    : listing.price();
            return "&#2b98fd#" + listing.id() + " &8- &f" + item
                    + " &7by &f" + listing.sellerName()
                    + " &8| &7bid &f" + service.formatMoney(listing.currentPrice())
                    + " &8| &7next &f" + service.formatMoney(nextBid)
                    + " &8| &7" + listing.bidCount() + " bids"
                    + " &8| &7ends &f" + time
                    + " &8- &7/ah bid " + listing.id() + " " + moneyInput(nextBid);
        }
        return "&#2b98fd#" + listing.id() + " &8- &f" + item
                + " &7by &f" + listing.sellerName()
                + " &8| &7price &f" + service.formatMoney(listing.price())
                + " &8| &7ends &f" + time
                + " &8- &7/ah buy " + listing.id();
    }

    private String itemLabel(ItemStack item) {
        int amount = item == null ? 1 : Math.max(1, item.getAmount());
        return amount + "x " + service.displayName(item);
    }

    private String moneyInput(double amount) {
        return amount == Math.rint(amount) ? Long.toString((long) amount) : Double.toString(amount);
    }

    private boolean textListArgument(String value) {
        return value.equalsIgnoreCase("list")
                || value.equalsIgnoreCase("text")
                || value.equalsIgnoreCase("chat");
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

    private void line(CommandSender sender, String line) {
        sender.sendMessage(Text.color(service.config().prefix() + line));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("auctionsplus.use")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("list", "sell", "auction", "buy", "bid", "active", "claim", "search", "cancel", "help"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("help")) {
            return filter(List.of("list", "sell", "auction", "buy", "bid", "claim", "active", "search", "cancel"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("auction"))) {
            return filter(List.of("100", "500", "1000"), args[1]);
        }
        if (args.length == 2 && List.of("active", "mine", "listings").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(List.of("list", "text", "chat"), args[1]);
        }
        if (args.length == 2 && List.of("claim", "claims", "mail").contains(args[0].toLowerCase(Locale.ROOT))) {
            Player player = (Player) sender;
            List<String> values = new ArrayList<>(service.claimItems(player.getUniqueId()).stream()
                    .map(claim -> Long.toString(claim.id()))
                    .toList());
            values.add("list");
            values.add("all");
            return filter(values, args[1]);
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
