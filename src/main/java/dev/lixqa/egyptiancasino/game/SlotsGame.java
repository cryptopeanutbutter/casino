package dev.lixqa.egyptiancasino.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SlotsGame implements CasinoGame {

    private static final List<String> SYMBOLS = List.of("☥", "♆", "♣", "7", "★", "Φ");

    @Override
    public String getId() {
        return "slots";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("slot");
    }

    @Override
    public String getDescription() {
        return "Spin the reels and match symbols for a payout.";
    }

    @Override
    public void play(GameContext context) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String[] results = new String[3];
        for (int i = 0; i < results.length; i++) {
            results[i] = SYMBOLS.get(random.nextInt(SYMBOLS.size()));
        }

        Component reelDisplay = Component.text("[" + String.join(" | ", results) + "]", NamedTextColor.AQUA);
        context.sendMessage(Component.text("The slots spin... ").append(reelDisplay));

        boolean allMatch = results[0].equals(results[1]) && results[1].equals(results[2]);
        boolean anyPair = results[0].equals(results[1]) || results[0].equals(results[2]) || results[1].equals(results[2]);

        if (allMatch) {
            context.winWithMultiplier(5.0);
        } else if (anyPair) {
            context.winWithMultiplier(2.0);
        } else {
            context.lose();
        }
    }
}
