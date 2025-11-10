package dev.lixqa.egyptiancasino.exchange;

import dev.lixqa.egyptiancasino.EgyptianCasinoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class CrystalExchangeListener implements Listener {

    private final EgyptianCasinoPlugin plugin;
    private final PriestOfRaManager priestManager;

    public CrystalExchangeListener(EgyptianCasinoPlugin plugin, PriestOfRaManager priestManager) {
        this.plugin = plugin;
        this.priestManager = priestManager;
    }

    @EventHandler
    public void onPriestInteraction(PlayerInteractEntityEvent event) {
        if (!priestManager.isPriest(event.getRightClicked())) {
            return;
        }

        event.setCancelled(true);
        long tokensAwarded = plugin.convertCrystals(event.getPlayer());
        if (tokensAwarded == -1) {
            plugin.sendMessage(event.getPlayer(), Component.text("The Priest of Ra cannot convert crystals right now. Install and enable CrystalMath or AmethystControl to redeem them.", NamedTextColor.RED));
            return;
        }

        if (tokensAwarded > 0) {
            plugin.sendMessage(event.getPlayer(), Component.text("Converted your minted crystals into ")
                    .append(plugin.formatTokens(tokensAwarded))
                    .append(Component.text(".")));
        } else {
            plugin.sendMessage(event.getPlayer(), Component.text("You have no minted crystals to convert.", NamedTextColor.RED));
        }
    }
}
