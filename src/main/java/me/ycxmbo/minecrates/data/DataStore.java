package me.ycxmbo.minecrates.data;

import org.bukkit.Location;

import java.util.*;

/**
 * Lightweight in-memory store so everything compiles & runs.
 * File IO/async can be layered later.
 */
public class DataStore {

    private final Map<Location, String> crateBindings = new HashMap<>();
    private final Map<UUID, Map<String,Integer>> virtualKeys = new HashMap<>();
    private final Map<UUID, Map<String,Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> opensTotal = new HashMap<>();
    private final Map<UUID, String> lastReward = new HashMap<>();

    // --- crate bindings ---
    public Map<Location,String> getAllCrateBindings() { return Collections.unmodifiableMap(crateBindings); }
    public void setCrateAt(Location loc, String crateId) { if (loc!=null && crateId!=null) crateBindings.put(loc, crateId); }
    public void removeCrateAt(Location loc) { if (loc!=null) crateBindings.remove(loc); }
    public String getCrateAt(Location loc) { return loc == null ? null : crateBindings.get(loc); }

    // --- virtual keys ---
    public int getVirtualKeys(UUID uuid, String keyOrCrateId) {
        return virtualKeys.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(keyOrCrateId.toLowerCase(Locale.ROOT), 0);
    }
    public void addVirtualKeys(UUID uuid, String keyOrCrateId, int delta) {
        virtualKeys.computeIfAbsent(uuid, u -> new HashMap<>())
                .merge(keyOrCrateId.toLowerCase(Locale.ROOT), delta, Integer::sum);
        if (getVirtualKeys(uuid, keyOrCrateId) < 0) {
            virtualKeys.get(uuid).put(keyOrCrateId.toLowerCase(Locale.ROOT), 0);
        }
    }

    // --- cooldowns (epoch seconds) ---
    public long getCooldown(UUID uuid, String crateId) {
        return cooldowns.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(crateId.toLowerCase(Locale.ROOT), 0L);
    }
    public void setCooldown(UUID uuid, String crateId, long untilEpochSeconds) {
        cooldowns.computeIfAbsent(uuid, u -> new HashMap<>())
                .put(crateId.toLowerCase(Locale.ROOT), untilEpochSeconds);
    }

    // --- stats ---
    public void addOpen(String crateId) { /* global counter could be added later */ }
    public int getOpensTotal(UUID uuid) { return opensTotal.getOrDefault(uuid, 0); }
    public void incrOpens(UUID uuid) { opensTotal.merge(uuid, 1, Integer::sum); }

    public void setLastRewardName(UUID uuid, String name) { if (uuid!=null) lastReward.put(uuid, name); }
    public String getLastRewardName(UUID uuid) { return lastReward.getOrDefault(uuid, ""); }

    // Persistence stubs
    public void saveAll() { /* write to disk later */ }
}
