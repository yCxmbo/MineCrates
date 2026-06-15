package me.ycxmbo.minecrates.data;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage abstraction for {@link PlayerData}. The current backend is
 * {@link YamlPlayerDataStore}; a SQLite (or other) backend can be added later
 * without touching callers by implementing this interface.
 */
public interface PlayerDataStore {

    /** Synchronously reads a player's data, returning a fresh empty record if none exists. Never returns null. */
    PlayerData load(UUID id);

    /** Asynchronously reads a player's data off the main thread. */
    CompletableFuture<PlayerData> loadAsync(UUID id);

    /** Synchronously persists a single player's data. */
    void save(PlayerData data);

    /** Asynchronously persists a single player's data off the main thread. */
    CompletableFuture<Void> saveAsync(PlayerData data);

    /** Asynchronously persists every supplied record. */
    CompletableFuture<Void> saveAll(Collection<PlayerData> all);

    /** Releases any resources held by the backend (executors, connections). */
    void close();
}
