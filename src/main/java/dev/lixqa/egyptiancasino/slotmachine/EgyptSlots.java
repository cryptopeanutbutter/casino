package dev.lixqa.egyptiancasino.slotmachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Centralizes the default reel symbols and payout logic for Pharaoh Slots so operators can tweak the experience.
 */
public final class EgyptSlots {

    private static final List<Integer> DEFAULT_SYMBOL_MODELS = Arrays.asList(3001, 3002, 3003);

    private EgyptSlots() {
    }

    public static List<Integer> createDefaultSymbolList() {
        return new ArrayList<>(DEFAULT_SYMBOL_MODELS);
    }

    public static long rewardForMatches(int matchCount) {
        return switch (matchCount) {
            case 3 -> 5;
            case 2 -> 1;
            default -> 0;
        };
    }
}
