package dev.lixqa.egyptiancasino.exchange;

import dev.lixqa.egyptiancasino.CasinoConfig;
import dev.lixqa.egyptiancasino.EgyptianCasinoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PriestOfRaManager {

    private final EgyptianCasinoPlugin plugin;
    private final NamespacedKey priestKey;

    public PriestOfRaManager(EgyptianCasinoPlugin plugin) {
        this.plugin = plugin;
        this.priestKey = new NamespacedKey(plugin, "priest_of_ra");
    }

    public Villager spawnPriest(Player player) {
        Location location = player.getLocation();
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.customName(Component.text("Priest of Ra", NamedTextColor.GOLD));
        villager.setCustomNameVisible(true);
        villager.setVillagerType(Villager.Type.DESERT);
        villager.setProfession(Villager.Profession.CLERIC);
        villager.setVillagerLevel(5);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        markAsPriest(villager);

        CasinoConfig config = plugin.getCasinoConfig();
        if (config.freezePriest()) {
            villager.setAI(false);
        }
        if (config.invulnerablePriest()) {
            villager.setInvulnerable(true);
        }

        plugin.sendMessage(player, Component.text("Summoned a Priest of Ra.", NamedTextColor.GREEN));
        return villager;
    }

    public boolean isPriest(Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return false;
        }
        PersistentDataContainer container = villager.getPersistentDataContainer();
        return container.has(priestKey, PersistentDataType.BYTE);
    }

    public void markAsPriest(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.set(priestKey, PersistentDataType.BYTE, (byte) 1);
        villager.customName(Component.text("Priest of Ra", NamedTextColor.GOLD));
        villager.setCustomNameVisible(true);
    }
}
