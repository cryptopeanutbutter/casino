package dev.lixqa.egyptiancasino.game;

import dev.lixqa.egyptiancasino.EgyptianCasinoPlugin;
import dev.lixqa.egyptiancasino.tokens.TokenManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class GameContext {

    private final EgyptianCasinoPlugin plugin;
    private final Player player;
    private final long bet;
    private final String[] args;

    public GameContext(EgyptianCasinoPlugin plugin, Player player, long bet, String[] args) {
        this.plugin = plugin;
        this.player = player;
        this.bet = bet;
        this.args = args;
    }

    public Player getPlayer() {
        return player;
    }

    public long getBet() {
        return bet;
    }

    public String[] getArgs() {
        return args;
    }

    public EgyptianCasinoPlugin getPlugin() {
        return plugin;
    }

    public void winWithMultiplier(double multiplier) {
        long payout = Math.round(bet * multiplier);
        payout(payout);
    }

    public void payout(long amount) {
        if (amount <= 0) {
            return;
        }

        TokenManager manager = plugin.getTokenManager();
        manager.deposit(player.getUniqueId(), amount);
        plugin.sendMessage(player, Component.text("You won ")
                .append(plugin.formatTokens(amount))
                .append(Component.text("!", NamedTextColor.GOLD)));
    }

    public void refundBet() {
        TokenManager manager = plugin.getTokenManager();
        manager.deposit(player.getUniqueId(), bet);
        plugin.sendMessage(player, Component.text("Your bet was returned.", NamedTextColor.YELLOW));
    }

    public void lose() {
        plugin.sendMessage(player, Component.text("You lost the bet.", NamedTextColor.RED));
    }

    public void sendMessage(Component component) {
        plugin.sendMessage(player, component);
    }
}
