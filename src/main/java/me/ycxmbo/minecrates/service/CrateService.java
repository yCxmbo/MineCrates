package me.ycxmbo.minecrates.service;

import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

public interface CrateService {

    CompletableFuture<Void> reloadAllAsync();

    Collection<Crate> crates();
    Crate crate(String id);

    // RNG helpers
    Reward pick(Crate crate, Random rng);
    double weightPercent(Crate crate, Reward reward);

    /**
     * Attempts to open the given crate for the player. Implementations
     * should handle cost, keys and cooldowns before invoking an animation.
     * The returned future completes once the reward has been granted.
     *
     * @param player player opening the crate
     * @param crate  crate being opened
     * @return future completed with {@code true} if a reward was granted
     */
    CompletableFuture<Boolean> open(Player player, Crate crate);

    // Keys
    Set<String> keyIds();
    ItemStack createKeyItem(String keyId, int amount);
    String keyDisplay(String keyId);
    void giveVirtualKeys(UUID playerId, String keyId, int amount);
    int virtualKeys(UUID playerId, String keyId);

    // Bindings (block crates)
    void bind(Location location, String crateId);
    void unbind(Location location);
    Optional<String> boundAt(Location location);
    Map<Location, String> allBindings();

    // PAPI data
    long cooldownRemaining(UUID playerId, String crateId);
    long totalOpened(UUID playerId);
    String lastRewardId(UUID playerId);
    /** Opens remaining before the crate's pity guarantee triggers, or -1 if pity is disabled. */
    long pityRemaining(UUID playerId, String crateId);

    // Player data lifecycle
    /** Warms the in-memory cache for a joining player (async load). */
    void onJoin(UUID playerId);
    /** Persists and evicts a quitting player's data. */
    void onQuit(UUID playerId);
    /** Returns cached data for an online player, or loads it asynchronously for an offline one. */
    CompletableFuture<PlayerData> loadPlayerData(UUID playerId);
    /** Asynchronously persists every cached player record (used by the autosave task). */
    CompletableFuture<Void> persistAll();

    // Shutdown
    void shutdown();

    // Executor for async tasks if needed
    default Executor io() { return Runnable::run; }
}
