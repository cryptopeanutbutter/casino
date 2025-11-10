package dev.lixqa.egyptiancasino.tokens.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class YamlTokenStorage implements TokenStorage {

    private final File dataFile;
    private FileConfiguration configuration;

    public YamlTokenStorage(File dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public void initialize() throws SQLException {
        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Unable to create token data directory: " + parent.getAbsolutePath());
        }
        configuration = YamlConfiguration.loadConfiguration(dataFile);
    }

    @Override
    public long loadBalance(UUID playerId) {
        ensureLoaded();
        return configuration.getLong(playerId.toString(), 0L);
    }

    @Override
    public void saveBalance(UUID playerId, long balance) throws SQLException {
        ensureLoaded();
        configuration.set(playerId.toString(), balance);
        try {
            configuration.save(dataFile);
        } catch (IOException exception) {
            throw new SQLException("Failed to save token balances", exception);
        }
    }

    private void ensureLoaded() {
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }
    }

    @Override
    public void close() {
        configuration = null;
    }
}
