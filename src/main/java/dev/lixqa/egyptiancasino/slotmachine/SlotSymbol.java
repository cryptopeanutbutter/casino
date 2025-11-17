package dev.lixqa.egyptiancasino.slotmachine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a displayable symbol that can appear on the Pharaoh slot reels.
 */
public record SlotSymbol(Material material, Component displayName, boolean jackpotSymbol) {

    public SlotSymbol {
        if (material == null) {
            throw new IllegalArgumentException("material cannot be null");
        }
    }

    public static SlotSymbol jackpot(Material material, String name) {
        return new SlotSymbol(material, Component.text(name, NamedTextColor.GOLD), true);
    }

    public static SlotSymbol regular(Material material, String name, NamedTextColor color) {
        return new SlotSymbol(material, Component.text(name, color), false);
    }

    public ItemStack createDisplayItem() {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (displayName != null) {
            meta.displayName(displayName);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
