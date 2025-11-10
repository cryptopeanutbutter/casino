package dev.lixqa.egyptiancasino.commands;

import dev.lixqa.egyptiancasino.EgyptianCasinoPlugin;
import dev.lixqa.egyptiancasino.game.CasinoGame;
import dev.lixqa.egyptiancasino.game.GameContext;
import dev.lixqa.egyptiancasino.tokens.TokenManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EgyptianCasinoCommand implements CommandExecutor, TabCompleter {

    private final EgyptianCasinoPlugin plugin;

    public EgyptianCasinoCommand(EgyptianCasinoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use casino commands.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ENGLISH);
        switch (subcommand) {
            case "balance" -> showBalance(player);
            case "spawnpriest" -> spawnPriest(player);
            case "givetokens" -> giveTokenItems(player, args);
            default -> playGame(player, subcommand, args);
        }

        return true;
    }

    private void showBalance(Player player) {
        depositFromInventory(player);
        long balance = plugin.getTokenManager().getBalance(player.getUniqueId());
        plugin.sendMessage(player, Component.text("You currently have ").append(plugin.formatTokens(balance)).append(Component.text(".")));
    }

    private void spawnPriest(Player player) {
        if (!player.hasPermission("egyptiancasino.admin")) {
            plugin.sendMessage(player, Component.text("You do not have permission to spawn the Priest of Ra.", NamedTextColor.RED));
            return;
        }

        plugin.getPriestManager().spawnPriest(player);
    }

    private void giveTokenItems(Player player, String[] args) {
        if (!player.hasPermission("egyptiancasino.admin")) {
            plugin.sendMessage(player, Component.text("You do not have permission to spawn Egyptian Tokens.", NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException exception) {
                plugin.sendMessage(player, Component.text("Amount must be a whole number.", NamedTextColor.RED));
                return;
            }
        }

        ItemStack tokenItem = plugin.createEgyptianTokenItem(amount);
        player.getInventory().addItem(tokenItem);
        plugin.sendMessage(player, Component.text("Spawned " + amount + " Egyptian Token item" + (amount == 1 ? "" : "s") + ".", NamedTextColor.GREEN));
    }

    private void playGame(Player player, String subcommand, String[] args) {
        Optional<CasinoGame> gameOptional = plugin.getGameRegistry().find(subcommand);
        if (gameOptional.isEmpty()) {
            plugin.sendMessage(player, Component.text("Unknown command. Use /egyptiancasino for help.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(player, Component.text("Usage: /egyptiancasino " + subcommand + " <bet>", NamedTextColor.YELLOW));
            return;
        }

        long bet;
        try {
            bet = Long.parseLong(args[1]);
        } catch (NumberFormatException exception) {
            plugin.sendMessage(player, Component.text("Your bet must be a positive number.", NamedTextColor.RED));
            return;
        }

        if (bet <= 0) {
            plugin.sendMessage(player, Component.text("Your bet must be greater than zero.", NamedTextColor.RED));
            return;
        }

        depositFromInventory(player);
        TokenManager tokenManager = plugin.getTokenManager();
        if (!tokenManager.withdraw(player.getUniqueId(), bet)) {
            plugin.sendMessage(player, Component.text("You do not have enough Egyptian Tokens.", NamedTextColor.RED));
            return;
        }

        CasinoGame game = gameOptional.get();
        String[] gameArgs = new String[Math.max(0, args.length - 2)];
        if (gameArgs.length > 0) {
            System.arraycopy(args, 2, gameArgs, 0, gameArgs.length);
        }

        GameContext context = new GameContext(plugin, player, bet, gameArgs);
        game.play(context);
    }

    private void sendHelp(Player player) {
        Component header = Component.text("=== Egyptian Casino ===", NamedTextColor.GOLD);
        plugin.sendMessage(player, header);
        plugin.sendMessage(player, Component.text("/egyptiancasino balance", NamedTextColor.YELLOW)
                .append(Component.text(" - Check your Egyptian Tokens.")));
        plugin.sendMessage(player, Component.text("/egyptiancasino spawnpriest", NamedTextColor.YELLOW)
                .append(Component.text(" - Summon the Priest of Ra (admin).")));
        plugin.sendMessage(player, Component.text("/egyptiancasino givetokens [amount]", NamedTextColor.YELLOW)
                .append(Component.text(" - Spawn Egyptian Token items for resource pack testing (admin).")));
        plugin.getGameRegistry().all().forEach(game ->
                plugin.sendMessage(player, Component.text("/egyptiancasino " + game.getId() + " <bet>", NamedTextColor.YELLOW)
                        .append(Component.text(" - " + game.getDescription()))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("balance");
            if (sender.hasPermission("egyptiancasino.admin")) {
                completions.add("spawnpriest");
                completions.add("givetokens");
            }
            plugin.getGameRegistry().all().stream()
                    .map(CasinoGame::getId)
                    .forEach(completions::add);
        }
        return completions;
    }

    private void depositFromInventory(Player player) {
        long converted = plugin.depositTokenItems(player);
        if (converted > 0) {
            plugin.sendMessage(player, Component.text("Banked " + converted + " Egyptian Token" + (converted == 1 ? "" : "s") + " from your inventory.", NamedTextColor.GREEN));
        }
    }
}
