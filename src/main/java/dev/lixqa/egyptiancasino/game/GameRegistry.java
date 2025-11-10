package dev.lixqa.egyptiancasino.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class GameRegistry {

    private final Map<String, CasinoGame> games = new HashMap<>();

    public void register(CasinoGame game) {
        games.put(game.getId().toLowerCase(Locale.ENGLISH), game);
        game.getAliases().forEach(alias -> games.put(alias.toLowerCase(Locale.ENGLISH), game));
    }

    public Optional<CasinoGame> find(String idOrAlias) {
        if (idOrAlias == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(games.get(idOrAlias.toLowerCase(Locale.ENGLISH)));
    }

    public Collection<CasinoGame> all() {
        return games.values().stream().distinct().toList();
    }
}
