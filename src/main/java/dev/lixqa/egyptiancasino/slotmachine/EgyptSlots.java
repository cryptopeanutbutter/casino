package dev.lixqa.egyptiancasino.slotmachine;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Centralizes the default reel symbols and payout logic for Pharaoh Slots so operators can tweak the experience.
 */
public final class EgyptSlots {

    private static final List<SlotSymbol> DEFAULT_SYMBOLS = Arrays.asList(
            SlotSymbol.regular(Material.SAND, "Sacred Sand", NamedTextColor.YELLOW),
            SlotSymbol.regular(Material.LAPIS_LAZULI, "Deep Desert Lapis", NamedTextColor.BLUE),
            SlotSymbol.regular(Material.EMERALD, "Pharaoh's Emerald", NamedTextColor.GREEN),
            SlotSymbol.regular(Material.GOLD_INGOT, "Sun-Kissed Gold", NamedTextColor.GOLD),
            SlotSymbol.jackpot(Material.NETHER_STAR, "Ra's Divine Relic")
    );

    private EgyptSlots() {
    }

    public static List<SlotSymbol> createDefaultSymbolList() {
        return new ArrayList<>(DEFAULT_SYMBOLS);
    }

    public static long rewardForMatches(int matchCount) {
        return switch (matchCount) {
            case 3 -> 5;
            case 2 -> 1;
            default -> 0;
        };
    }
}
