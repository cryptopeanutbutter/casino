package dev.lixqa.egyptiancasino.tokens.storage;

import java.sql.SQLException;
import java.util.UUID;

public interface TokenStorage {

    void initialize() throws SQLException;

    long loadBalance(UUID playerId) throws SQLException;

    void saveBalance(UUID playerId, long balance) throws SQLException;

    void close();
}
