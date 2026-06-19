package me.ycxmbo.minecrates.service.impl;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Key;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.crate.RewardPicker;
import me.ycxmbo.minecrates.data.PlayerData;
import me.ycxmbo.minecrates.data.PlayerDataStore;
import me.ycxmbo.minecrates.data.YamlPlayerDataStore;
import me.ycxmbo.minecrates.hook.HologramManager;
import me.ycxmbo.minecrates.gui.OpenAnimationGUI;
import me.ycxmbo.minecrates.event.CrateOpenEvent;
import me.ycxmbo.minecrates.event.CrateRewardGiveEvent;
import me.ycxmbo.minecrates.hook.VaultHook;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.util.ItemUtil;
import me.ycxmbo.minecrates.util.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class SimpleCrateService implements CrateService {

    private final MineCrates plugin;
    private final VaultHook vault;
    private final HologramManager holograms;

    // Data (memory)
    private volatile Map<String, Crate> crates = new ConcurrentHashMap<>();
    private volatile Map<String, Key> keys = new ConcurrentHashMap<>();
    private volatile Map<Location, String> bindings = new ConcurrentHashMap<>();

    // Persistent player data (cache + backing store)
    private final PlayerDataStore store;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();

    private final MiniMessage mm = MiniMessage.miniMessage();

    public SimpleCrateService(MineCrates plugin, VaultHook vault, HologramManager holograms) {
        this.plugin = plugin;
        this.vault = vault;
        this.holograms = holograms;
        this.store = new YamlPlayerDataStore(new File(plugin.getDataFolder(), "playerdata"), plugin.getLogger());
    }

    /** Returns the cached record for the player, loading synchronously if it is not warmed yet. */
    private PlayerData data(UUID id) {
        return players.computeIfAbsent(id, store::load);
    }

    @Override
    public CompletableFuture<Void> reloadAllAsync() {
        return CompletableFuture.runAsync(() -> {
            Map<String, Crate> newCrates = new ConcurrentHashMap<>();
            Map<String, Key> newKeys = new ConcurrentHashMap<>();
            Map<Location, String> newBindings = new ConcurrentHashMap<>();

            ensureDefaults();

            loadKeys(new File(plugin.getDataFolder(), "keys.yml"), newKeys);
            loadCrates(new File(plugin.getDataFolder(), "crates.yml"), newCrates, newKeys);

            loadBindings(new File(plugin.getDataFolder(), "bindings.yml"), newBindings);

            synchronized (this) {
                crates = newCrates;
                keys = newKeys;
                bindings = newBindings;
            }

        }, Executors.newSingleThreadExecutor()).thenRun(() -> {
            // back to main thread: refresh holograms
            Bukkit.getScheduler().runTask(plugin, () -> holograms.refreshAll(bindings, this::crate));
        });
    }

    private void ensureDefaults() {
        ensureDefault("config.yml");
        ensureDefault("crates.yml");
        ensureDefault("keys.yml");
        ensureDefault("rewards.yml");
    }

    private void ensureDefault(String name) {
        try {
            File f = new File(plugin.getDataFolder(), name);
            if (!f.exists()) plugin.saveResource(name, false);
        } catch (Exception ignored) {}
    }

    private void loadKeys(File f, Map<String, Key> target) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("keys");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection ks = sec.getConfigurationSection(id);
            if (ks == null) continue;
            String display = ks.getString("display", ks.getString("key-display", id));
            Material mat = Material.matchMaterial(ks.getString("material","TRIPWIRE_HOOK"));
            ItemStack base = new ItemStack(mat == null ? Material.TRIPWIRE_HOOK : mat);
            ItemUtil.applyName(base, display);
            Key k = new Key(id.toLowerCase(Locale.ROOT), display, base);
            target.put(k.id(), k);
        }
    }

    private void loadCrates(File f, Map<String, Crate> target, Map<String, Key> keys) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("crates");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection cs = sec.getConfigurationSection(id);
            if (cs == null) continue;
            Crate crate = Crate.fromSection(id, cs, keys);
            target.put(crate.id(), crate);
        }
    }

    private void loadBindings(File f, Map<Location, String> target) {
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection b = y.getConfigurationSection("bindings");
        if (b == null) return;
        for (String world : b.getKeys(false)) {
            ConfigurationSection ws = b.getConfigurationSection(world);
            if (ws == null) continue;
            for (String key : ws.getKeys(false)) {
                String[] xyz = key.split(",");
                if (xyz.length != 3) continue;
                int x, yv, z;
                try {
                    x = Integer.parseInt(xyz[0].trim());
                    yv = Integer.parseInt(xyz[1].trim());
                    z = Integer.parseInt(xyz[2].trim());
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("Skipping malformed binding coordinate '" + key + "' in world '" + world + "'.");
                    continue;
                }
                String id = ws.getString(key);
                org.bukkit.World w = Bukkit.getWorld(world);
                if (w != null && id != null) {
                    target.put(new Location(w, x, yv, z), id.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    private void saveData() {
        YamlConfiguration y = new YamlConfiguration();
        Map<Location, String> snapshot = new HashMap<>(bindings);
        for (Map.Entry<Location, String> e : snapshot.entrySet()) {
            Location l = e.getKey();
            String id = e.getValue();
            String path = "bindings." + l.getWorld().getName() + "." + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
            y.set(path, id);
        }
        try { y.save(new File(plugin.getDataFolder(), "bindings.yml")); }
        catch (Exception ex) { plugin.getLogger().warning("Failed saving bindings: " + ex.getMessage()); }
    }

    // ───── API ─────

    @Override public Collection<Crate> crates() { return Collections.unmodifiableCollection(crates.values()); }
    @Override public Crate crate(String id) { return id == null ? null : crates.get(id.toLowerCase(Locale.ROOT)); }

    @Override
    public Reward pick(Crate crate, Random rng) {
        return crate.picker().pick();
    }

    @Override
    public double weightPercent(Crate crate, Reward reward) {
        return crate.picker().weightPercent(reward);
    }

    @Override
    public CompletableFuture<Boolean> open(Player player, Crate crate) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Type-specific open permission (in addition to per-crate gate below)
            if (crate.type() == Crate.Type.BLOCK) {
                if (!player.hasPermission("minecrates.open.block")) {
                    Messages.msg(player, MineCrates.get().configManager().msg("perm.open-deny"));
                    future.complete(false); return;
                }
            } else { // VIRTUAL
                if (!player.hasPermission("minecrates.open.virtual")) {
                    Messages.msg(player, MineCrates.get().configManager().msg("perm.open-deny"));
                    future.complete(false); return;
                }
            }

            PlayerData pd = data(player.getUniqueId());
            long now = System.currentTimeMillis();
            long until = pd.cooldownUntil(crate.id());
            boolean bypassCd = player.hasPermission("minecrates.bypass.cooldown");
            if (!bypassCd && until > now) {
                long remain = (until - now) / 1000L;
                Messages.msg(player, "<red>Please wait <white>" + remain + "s</white> before opening again.</red>");
                try {
                    player.sendActionBar(MiniMessage.miniMessage().deserialize(MineCrates.get().configManager().msg("cooldown.actionbar").replace("<seconds>", String.valueOf(remain))));
                } catch (Throwable ignored) {}
                future.complete(false);
                return;
            }

            if (crate.costEnabled() && !player.hasPermission("minecrates.bypass.cost")) {
                switch (crate.costCurrency()) {
                    case VAULT -> {
                        if (!vault.present()) { Messages.msg(player, "<red>Economy unavailable.</red>"); future.complete(false); return; }
                        if (!vault.withdraw(player, crate.costAmount())) { Messages.msg(player, "<red>Need:</red> <white>" + vault.format(crate.costAmount()) + "</white>"); future.complete(false); return; }
                    }
                    case EXP -> {
                        int need = (int)Math.ceil(crate.costAmount());
                        if (player.getTotalExperience() < need) { Messages.msg(player, "<red>Need</red> <white>"+need+" exp</white>."); future.complete(false); return; }
                        player.giveExp(-need);
                    }
                    case EXP_LEVELS -> {
                        int lv = (int)Math.ceil(crate.costAmount());
                        if (player.getLevel() < lv) { Messages.msg(player, "<red>Need</red> <white>"+lv+" levels</white>."); future.complete(false); return; }
                        player.setLevel(player.getLevel() - lv);
                    }
                }
            }

            // Key checks
            if (crate.requiresKey()) {
                Key requiredKey = crate.key() == null ? null : keys.get(crate.key().id());
                if (requiredKey == null) {
                    plugin.getLogger().warning("Crate '" + crate.id() + "' requires key '"
                            + (crate.key() == null ? "?" : crate.key().id()) + "' which is not defined in keys.yml.");
                    Messages.msg(player, "<red>This crate is misconfigured (missing key).</red>");
                    future.complete(false); return;
                }
                boolean has = false;
                if (virtualKeys(player.getUniqueId(), requiredKey.id()) > 0) { giveVirtualKeys(player.getUniqueId(), requiredKey.id(), -1); has = true; }
                else {
                    int need = 1;
                    for (ItemStack it : player.getInventory().getContents()) {
                        if (it == null || it.getType().isAir()) continue;
                        if (requiredKey.matches(it)) {
                            int take = Math.min(need, it.getAmount());
                            it.setAmount(it.getAmount() - take);
                            need -= take;
                            if (need <= 0) { has = true; break; }
                        }
                    }
                }
                if (!has) { Messages.msg(player, "<red>You don't have a key for this crate.</red>"); future.complete(false); return; }
            }

            if (!player.hasPermission("minecrates.open." + crate.id())) { Messages.msg(player, MineCrates.get().configManager().msg("perm.open-deny")); future.complete(false); return; }

            CrateOpenEvent openEvent = new CrateOpenEvent(player, crate);
            Bukkit.getPluginManager().callEvent(openEvent);
            if (openEvent.isCancelled()) { future.complete(false); return; }

            // set cooldown now (unless bypass)
            if (!bypassCd) {
                pd.setCooldownUntil(crate.id(), System.currentTimeMillis() + crate.cooldownMillis());
            }

            Reward picked = crate.picker().pick();
            if (picked == null) { future.complete(false); return; }
            final Reward reward = applyPity(pd, crate, picked);

            OpenAnimationGUI.play(player, crate, reward, r -> {
                CrateRewardGiveEvent giveEvent = new CrateRewardGiveEvent(player, crate, r);
                Bukkit.getPluginManager().callEvent(giveEvent);
                if (!giveEvent.isCancelled()) {
                    r.give(player, vault);
                    logRollAsync(player.getUniqueId(), player.getName(), crate.id(), r.id());
                }
                pd.setLastReward(r.id());
                pd.incrementOpened();
                future.complete(true);
            });
        });
        return future;
    }

    /**
     * Applies the crate's pity rule. Increments the player's pity counter; if the natural
     * roll already satisfies the pity tier the counter resets, and once the counter reaches
     * the threshold the guaranteed reward is substituted and the counter resets.
     */
    private Reward applyPity(PlayerData pd, Crate crate, Reward natural) {
        if (!crate.pityEnabled()) return natural;
        String crateId = crate.id();
        if (satisfiesPity(crate, natural)) {
            pd.setPityCounter(crateId, 0);
            return natural;
        }
        int count = pd.pityCounter(crateId) + 1;
        if (count >= crate.pityThreshold()) {
            Reward forced = pickPityReward(crate);
            pd.setPityCounter(crateId, 0);
            return forced != null ? forced : natural;
        }
        pd.setPityCounter(crateId, count);
        return natural;
    }

    private boolean satisfiesPity(Crate crate, Reward reward) {
        Crate.Pity pity = crate.pity();
        if (pity.rewardId() != null) return reward.id().equalsIgnoreCase(pity.rewardId());
        return false;
    }

    private Reward pickPityReward(Crate crate) {
        Crate.Pity pity = crate.pity();
        if (pity.rewardId() != null) {
            for (Reward r : crate.rewards()) {
                if (r.id().equalsIgnoreCase(pity.rewardId())) return r;
            }
        }
        return null;
    }

    private void logRollAsync(UUID uuid, String name, String crateId, String rewardId) {
        CompletableFuture.runAsync(() -> {
            try {
                java.nio.file.Path dir = plugin.getDataFolder().toPath().resolve("logs");
                java.nio.file.Files.createDirectories(dir);
                java.nio.file.Path f = dir.resolve("rolls.log");
                String line = java.time.Instant.now() + "," + uuid + "," + name + "," + crateId + "," + rewardId + System.lineSeparator();
                java.nio.file.Files.write(f, line.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        });
    }

    @Override
    public Set<String> keyIds() { return Collections.unmodifiableSet(keys.keySet()); }

    @Override
    public ItemStack createKeyItem(String keyId, int amount) {
        Key k = keys.get(keyId == null ? null : keyId.toLowerCase(Locale.ROOT));
        if (k == null) return null;
        ItemStack it = k.asItem();
        it.setAmount(Math.max(1, amount));
        return it;
    }

    @Override
    public String keyDisplay(String keyId) {
        if (keyId == null || keyId.isEmpty()) return "";
        String id = keyId.toLowerCase(Locale.ROOT);
        for (Crate c : crates.values()) {
            if (c.key() != null && c.key().id().equalsIgnoreCase(id)) {
                String ov = c.keyDisplayOverride();
                if (ov != null && !ov.isBlank()) return ov;
            }
        }
        Key k = keys.get(id);
        return k == null ? keyId : k.display();
    }

    @Override
    public void giveVirtualKeys(UUID playerId, String keyId, int amount) {
        data(playerId).addVirtualKeys(keyId.toLowerCase(Locale.ROOT), amount);
    }

    @Override
    public int virtualKeys(UUID playerId, String keyId) {
        return data(playerId).virtualKeys(keyId.toLowerCase(Locale.ROOT));
    }

    @Override
    public void bind(Location location, String crateId) {
        bindings.put(location.getBlock().getLocation(), crateId.toLowerCase(Locale.ROOT));
        saveData();
        holograms.upsert(location, crate(crateId));
    }

    @Override
    public void unbind(Location location) {
        bindings.remove(location.getBlock().getLocation());
        saveData();
        holograms.remove(location);
    }

    @Override
    public Optional<String> boundAt(Location location) {
        return Optional.ofNullable(bindings.get(location.getBlock().getLocation()));
    }

    @Override
    public Map<Location, String> allBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    @Override
    public long cooldownRemaining(UUID playerId, String crateId) {
        long until = data(playerId).cooldownUntil(crateId.toLowerCase(Locale.ROOT));
        if (until <= 0) return 0;
        long rem = (until - System.currentTimeMillis()) / 1000L;
        return Math.max(0, rem);
    }

    @Override public long totalOpened(UUID playerId) { return data(playerId).opened(); }
    @Override public String lastRewardId(UUID playerId) { return data(playerId).lastReward(); }

    @Override
    public long pityRemaining(UUID playerId, String crateId) {
        Crate crate = crate(crateId);
        if (crate == null || !crate.pityEnabled()) return -1;
        int remaining = crate.pityThreshold() - data(playerId).pityCounter(crate.id());
        return Math.max(0, remaining);
    }

    // ───── Player data lifecycle ─────

    @Override
    public void onJoin(UUID playerId) {
        store.loadAsync(playerId).thenAccept(pd -> players.putIfAbsent(playerId, pd));
    }

    @Override
    public void onQuit(UUID playerId) {
        PlayerData pd = players.remove(playerId);
        if (pd != null) store.saveAsync(pd);
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID playerId) {
        PlayerData cached = players.get(playerId);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return store.loadAsync(playerId);
    }

    @Override
    public CompletableFuture<Void> persistAll() {
        return store.saveAll(new ArrayList<>(players.values()));
    }

    @Override
    public void shutdown() {
        try {
            for (PlayerData pd : players.values()) store.save(pd);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Error while saving player data on shutdown", ex);
        } finally {
            store.close();
        }
    }
}
