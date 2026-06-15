package me.ycxmbo.minecrates.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player persistent state: virtual keys, cooldowns, open count, last reward
 * and per-crate pity counters. Pure data holder with no Bukkit dependencies so
 * it can be serialized by any {@link PlayerDataStore} backend and unit-tested.
 */
public final class PlayerData {

    private final UUID id;
    private final Map<String, Integer> virtualKeys = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();   // crateId -> epoch millis "until"
    private final Map<String, Integer> pityCounters = new ConcurrentHashMap<>(); // crateId -> opens since last pity hit
    private volatile long opened;
    private volatile String lastReward = "";

    public PlayerData(UUID id) {
        this.id = id;
    }

    public UUID id() { return id; }

    // ── virtual keys ──
    public Map<String, Integer> virtualKeys() { return virtualKeys; }

    public int virtualKeys(String keyId) {
        return virtualKeys.getOrDefault(keyId, 0);
    }

    public void addVirtualKeys(String keyId, int delta) {
        int now = virtualKeys.merge(keyId, delta, Integer::sum);
        if (now <= 0) virtualKeys.remove(keyId);
    }

    // ── cooldowns (epoch millis) ──
    public Map<String, Long> cooldowns() { return cooldowns; }

    public long cooldownUntil(String crateId) {
        return cooldowns.getOrDefault(crateId, 0L);
    }

    public void setCooldownUntil(String crateId, long untilMillis) {
        cooldowns.put(crateId, untilMillis);
    }

    // ── pity counters ──
    public Map<String, Integer> pityCounters() { return pityCounters; }

    public int pityCounter(String crateId) {
        return pityCounters.getOrDefault(crateId, 0);
    }

    public void setPityCounter(String crateId, int value) {
        if (value <= 0) pityCounters.remove(crateId);
        else pityCounters.put(crateId, value);
    }

    // ── stats ──
    public long opened() { return opened; }
    public void setOpened(long opened) { this.opened = opened; }
    public void incrementOpened() { this.opened++; }

    public String lastReward() { return lastReward; }
    public void setLastReward(String lastReward) { this.lastReward = lastReward == null ? "" : lastReward; }
}
