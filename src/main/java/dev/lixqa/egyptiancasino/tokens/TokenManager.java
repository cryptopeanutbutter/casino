package dev.lixqa.egyptiancasino.tokens;

import dev.lixqa.egyptiancasino.tokens.storage.TokenStorage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenManager {

    private final TokenStorage storage;
    private final Map<UUID, Long> cache = new HashMap<>();

    public TokenManager(TokenStorage storage) {
        this.storage = storage;
    }

    public void initialize() throws SQLException {
        storage.initialize();
    }

    public synchronized long getBalance(UUID playerId) {
        return cache.computeIfAbsent(playerId, id -> {
            try {
                return storage.loadBalance(id);
            } catch (SQLException exception) {
                throw new RuntimeException("Failed to load balance for " + id, exception);
            }
        });
    }

    public synchronized void deposit(UUID playerId, long amount) {
        if (amount <= 0) {
            return;
        }

        long current = getBalance(playerId);
        long updated = Math.addExact(current, amount);
        cache.put(playerId, updated);
        persistBalance(playerId, updated);
    }

    public synchronized boolean withdraw(UUID playerId, long amount) {
        if (amount <= 0) {
            return false;
        }

        long current = getBalance(playerId);
        if (current < amount) {
            return false;
        }

        long updated = current - amount;
        cache.put(playerId, updated);
        persistBalance(playerId, updated);
        return true;
    }

    public synchronized void setBalance(UUID playerId, long balance) {
        cache.put(playerId, Math.max(0L, balance));
        persistBalance(playerId, Math.max(0L, balance));
    }

    private void persistBalance(UUID playerId, long balance) {
        try {
            storage.saveBalance(playerId, balance);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to persist balance for " + playerId, exception);
        }
    }

    public void shutdown() {
        storage.close();
    }
}
