package me.ycxmbo.minecrates.service;

import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
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

    // Open flow
    CompletableFuture<Boolean> open(Player player, Crate crate);

    // Keys
    Set<String> keyIds();
    ItemStack createKeyItem(String keyId, int amount);
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

    // Shutdown
    void shutdown();

    // Executor for async tasks if needed
    default Executor io() { return Runnable::run; }
}
