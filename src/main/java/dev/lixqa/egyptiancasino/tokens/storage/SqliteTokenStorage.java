package dev.lixqa.egyptiancasino.tokens.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class SqliteTokenStorage implements TokenStorage {

    private static final String TABLE = "egyptian_tokens";

    private final File databaseFile;
    private Connection connection;

    public SqliteTokenStorage(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public void initialize() throws SQLException {
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                    "player_uuid TEXT PRIMARY KEY, " +
                    "balance INTEGER NOT NULL" +
                    ")");
        }
    }

    @Override
    public long loadBalance(UUID playerId) throws SQLException {
        ensureConnection();
        String query = "SELECT balance FROM " + TABLE + " WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("balance");
                }
            }
        }
        return 0L;
    }

    @Override
    public void saveBalance(UUID playerId, long balance) throws SQLException {
        ensureConnection();
        String update = "INSERT INTO " + TABLE + " (player_uuid, balance) VALUES (?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET balance = excluded.balance";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setString(1, playerId.toString());
            statement.setLong(2, balance);
            statement.executeUpdate();
        }
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
