package dev.lixqa.egyptiancasino;

import org.bukkit.configuration.file.FileConfiguration;

public record CasinoConfig(long tokensPerCrystal, boolean freezePriest, boolean invulnerablePriest) {

    public CasinoConfig(FileConfiguration configuration) {
        this(Math.max(1L, configuration.getLong("tokens-per-crystal", 1L)),
                configuration.getBoolean("priest.disable-movement", true),
                configuration.getBoolean("priest.invulnerable", true));
    }
}
