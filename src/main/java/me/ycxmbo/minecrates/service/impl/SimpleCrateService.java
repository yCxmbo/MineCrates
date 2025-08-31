package me.ycxmbo.minecrates.service.impl;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Key;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.crate.RewardPicker;
import me.ycxmbo.minecrates.hook.HologramManager;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public final class SimpleCrateService implements CrateService {

    private final MineCrates plugin;
    private final VaultHook vault;
    private final HologramManager holograms;

    // Data (memory)
    private final Map<String, Crate> crates = new LinkedHashMap<>();
    private final Map<String, Key> keys = new LinkedHashMap<>();
    private final Map<Location, String> bindings = new HashMap<>();

    private final Map<UUID, Map<String, Integer>> virtKeys = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> opened = new HashMap<>();
    private final Map<UUID, String> lastReward = new HashMap<>();

    private final MiniMessage mm = MiniMessage.miniMessage();

    public SimpleCrateService(MineCrates plugin, VaultHook vault, HologramManager holograms) {
        this.plugin = plugin;
        this.vault = vault;
        this.holograms = holograms;
    }

    @Override
    public CompletableFuture<Void> reloadAllAsync() {
        return CompletableFuture.runAsync(() -> {
            crates.clear();
            keys.clear();
            bindings.clear();

            ensureDefaults();

            loadKeys(new File(plugin.getDataFolder(), "keys.yml"));
            loadCrates(new File(plugin.getDataFolder(), "crates.yml"));

            loadBindings(new File(plugin.getDataFolder(), "bindings.yml"));

        }, Executors.newSingleThreadExecutor()).thenRun(() -> {
            // back to main thread: refresh holograms
            Bukkit.getScheduler().runTask(plugin, () -> holograms.refreshAll(bindings, this::crate));
        });
    }

    private void ensureDefaults() {
        plugin.saveResource("config.yml", false);
        plugin.saveResource("crates.yml", false);
        plugin.saveResource("keys.yml", false);
        plugin.saveResource("rewards.yml", false);
    }

    private void loadKeys(File f) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("keys");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection ks = sec.getConfigurationSection(id);
            if (ks == null) continue;
            String display = ks.getString("display", id);
            Material mat = Material.matchMaterial(ks.getString("material","TRIPWIRE_HOOK"));
            ItemStack base = new ItemStack(mat == null ? Material.TRIPWIRE_HOOK : mat);
            ItemUtil.applyName(base, display);
            Key k = new Key(id.toLowerCase(Locale.ROOT), display, base);
            keys.put(k.id(), k);
        }
    }

    private void loadCrates(File f) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("crates");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection cs = sec.getConfigurationSection(id);
            if (cs == null) continue;
            Crate crate = Crate.fromSection(id, cs, keys);
            crates.put(crate.id(), crate);
        }
    }

    private void loadBindings(File f) {
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
                int x = Integer.parseInt(xyz[0]); int yv = Integer.parseInt(xyz[1]); int z = Integer.parseInt(xyz[2]);
                String id = ws.getString(key);
                org.bukkit.World w = Bukkit.getWorld(world);
                if (w != null && id != null) {
                    bindings.put(new Location(w, x, yv, z), id.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    private void saveBindings() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<Location,String> e : bindings.entrySet()) {
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
        return CompletableFuture.supplyAsync(() -> {
            // Cooldown
// inside open(Player, Crate) BEFORE picking the reward:
            if (crate.costEnabled() && !player.hasPermission("minecrates.bypass.cost")) {
                switch (crate.costCurrency()) {
                    case VAULT -> {
                        if (!vault.present()) {
                            me.ycxmbo.minecrates.util.Messages.msg(player, "<red>Economy unavailable.</red>");
                            return false;
                        }
                        if (!vault.withdraw(player, crate.costAmount())) {
                            me.ycxmbo.minecrates.util.Messages.msg(player, "<red>Need:</red> <white>" + vault.format(crate.costAmount()) + "</white>");
                            return false;
                        }
                    }
                    case EXP -> {
                        int totalExp = player.getTotalExperience();
                        int need = (int)Math.ceil(crate.costAmount());
                        if (totalExp < need) {
                            me.ycxmbo.minecrates.util.Messages.msg(player, "<red>Need</red> <white>"+need+" exp</white>.");
                            return false;
                        }
                        player.giveExp(-need);
                    }
                    case EXP_LEVELS -> {
                        int needLv = (int)Math.ceil(crate.costAmount());
                        if (player.getLevel() < needLv) {
                            me.ycxmbo.minecrates.util.Messages.msg(player, "<red>Need</red> <white>"+needLv+" levels</white>.");
                            return false;
                        }
                        player.setLevel(player.getLevel() - needLv);
                    }
                }
            }


            // Key check (virtual first, then item-inventory by tag)
            if (crate.requiresKey()) {
                boolean has = false;
                // virtual
                if (virtualKeys(player.getUniqueId(), crate.key().id()) > 0) {
                    giveVirtualKeys(player.getUniqueId(), crate.key().id(), -1);
                    has = true;
                } else {
                    // physical tagged keys
                    int need = 1;
                    for (ItemStack it : player.getInventory().getContents()) {
                        if (it == null || it.getType().isAir()) continue;
                        if (keys.get(crate.key().id()).matches(it)) {
                            int take = Math.min(need, it.getAmount());
                            it.setAmount(it.getAmount() - take);
                            need -= take;
                            if (need <= 0) { has = true; break; }
                        }
                    }
                }
                if (!has) {
                    Messages.msg(player, "<red>You don't have a key for this crate.</red>");
                    return false;
                }
            }

            Reward r = crate.picker().pick();
            // reward grant must happen on main thread
            Bukkit.getScheduler().runTask(plugin, () -> r.give(player));

            // record stats + cooldown
            lastReward.put(player.getUniqueId(), r.id());
            opened.merge(player.getUniqueId(), 1L, Long::sum);
            cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(crate.id(), System.currentTimeMillis() + crate.cooldownMillis());

            return true;


        });
    }

    @Override
    public Set<String> keyIds() { return Collections.unmodifiableSet(keys.keySet()); }

    @Override
    public ItemStack createKeyItem(String keyId, int amount) {
        Key k = keys.get(keyId);
        if (k == null) return null;
        ItemStack it = k.asItem();
        it.setAmount(Math.max(1, amount));
        return it;
    }

    @Override
    public void giveVirtualKeys(UUID playerId, String keyId, int amount) {
        virtKeys.computeIfAbsent(playerId, u -> new HashMap<>())
                .merge(keyId.toLowerCase(Locale.ROOT), amount, Integer::sum);
        if (virtKeys.get(playerId).get(keyId) <= 0) virtKeys.get(playerId).remove(keyId);
    }

    @Override
    public int virtualKeys(UUID playerId, String keyId) {
        return virtKeys.getOrDefault(playerId, Collections.emptyMap()).getOrDefault(keyId.toLowerCase(Locale.ROOT), 0);
    }

    @Override
    public void bind(Location location, String crateId) {
        bindings.put(location.getBlock().getLocation(), crateId.toLowerCase(Locale.ROOT));
        saveBindings();
        holograms.upsert(location, crate(crateId));
    }

    @Override
    public void unbind(Location location) {
        bindings.remove(location.getBlock().getLocation());
        saveBindings();
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
        Map<String, Long> m = cooldowns.get(playerId);
        if (m == null) return 0;
        Long until = m.get(crateId.toLowerCase(Locale.ROOT));
        if (until == null) return 0;
        long rem = (until - System.currentTimeMillis()) / 1000L;
        return Math.max(0, rem);
    }

    @Override public long totalOpened(UUID playerId) { return opened.getOrDefault(playerId, 0L); }
    @Override public String lastRewardId(UUID playerId) { return lastReward.getOrDefault(playerId, ""); }

    @Override
    public void shutdown() {
        // nothing long-running yet
    }
}
