package dev.lixqa.egyptiancasino.game;

import java.util.Set;

public interface CasinoGame {

    String getId();

    default Set<String> getAliases() {
        return Set.of();
    }

    String getDescription();

    void play(GameContext context);
}
