package me.ycxmbo.minecrates.service;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Key;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.hook.HologramManager;
import me.ycxmbo.minecrates.util.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public final class CrateServiceImpl implements CrateService {

    private final MineCrates plugin;
    private final Map<String, Crate> crates = new LinkedHashMap<>();
    private final Map<String, Key> keys = new LinkedHashMap<>();
    private final Map<String, String> bindings = new HashMap<>(); // locKey -> crateId
    private final Map<UUID, Map<String,Integer>> virtualKeys = new HashMap<>(); // quick in-mem

    public CrateServiceImpl(MineCrates plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override public Map<String, Crate> crates() { return Collections.unmodifiableMap(crates); }
    @Override public Map<String, Key> keys() { return Collections.unmodifiableMap(keys); }
    @Override public Optional<Crate> crate(String id){ return Optional.ofNullable(crates.get(s(id))); }
    @Override public Optional<Key> key(String id){ return Optional.ofNullable(keys.get(s(id))); }

    private String s(String s){ return s == null ? "" : s.toLowerCase(Locale.ROOT); }

    @Override
    public void reload() {
        crates.clear(); keys.clear(); bindings.clear();
        // keys.yml
        File keysFile = ensure(plugin, "keys.yml");
        YamlConfiguration kc = YamlConfiguration.loadConfiguration(keysFile);
        ConfigurationSection ks = kc.getConfigurationSection("keys");
        if (ks != null) {
            for (String id : ks.getKeys(false)) {
                String mat = ks.getString(id + ".material", "TRIPWIRE_HOOK");
                ItemStack item = new ItemStack(Material.matchMaterial(mat) == null ?
                        Material.TRIPWIRE_HOOK : Material.matchMaterial(mat), 1);
                Key key = new Key(id, item, new NamespacedKey(plugin, "key."+id.toLowerCase()));
                keys.put(key.id(), key);
            }
        }
        // crates.yml (minimal: rewards only items via material/amount)
        File cratesFile = ensure(plugin, "crates.yml");
        YamlConfiguration cc = YamlConfiguration.loadConfiguration(cratesFile);
        ConfigurationSection cs = cc.getConfigurationSection("crates");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                ConfigurationSection csec = cs.getConfigurationSection(id);
                if (csec == null) continue;
                List<Reward> rewards = new ArrayList<>();
                ConfigurationSection rs = csec.getConfigurationSection("rewards");
                if (rs != null) {
                    for (String rid : rs.getKeys(false)) {
                        ConfigurationSection r = rs.getConfigurationSection(rid);
                        if (r == null) continue;
                        double weight = r.getDouble("weight", 1.0);
                        String rarity = r.getString("rarity", "COMMON");
                        List<ItemStack> items = new ArrayList<>();
                        ConfigurationSection is = r.getConfigurationSection("items");
                        if (is != null) for (String k : is.getKeys(false)) {
                            String mat = is.getString(k + ".material", "STONE");
                            int amt = is.getInt(k + ".amount", 1);
                            Material m = Material.matchMaterial(mat);
                            items.add(new ItemStack(m == null ? Material.STONE : m, Math.max(1, amt)));
                        }
                        Reward.Rarity rr = Reward.Rarity.valueOf(rarity.toUpperCase());
                        rewards.add(new Reward(rid, weight, rr, items, r.getStringList("commands"),
                                r.getBoolean("announce", false), r.getString("message", null)));
                    }
                }
                Crate crate = Crate.fromSection(id, csec, rewards);
                crates.put(crate.id(), crate);
            }
        }
        // bindings.yml (optional)
        File b = ensure(plugin, "bindings.yml");
        YamlConfiguration bc = YamlConfiguration.loadConfiguration(b);
        for (String key : bc.getKeys(false)) bindings.put(key, bc.getString(key));

        // refresh visuals
        HologramManager.refreshAll(plugin, this);
    }

    private static File ensure(MineCrates pl, String name) {
        File f = new File(pl.getDataFolder(), name);
        if (!f.exists()) pl.saveResource(name, false);
        return f;
    }

    @Override
    public int getVirtualKeys(Player p, String keyId) {
        return virtualKeys.getOrDefault(p.getUniqueId(), Map.of()).getOrDefault(s(keyId), 0);
    }

    @Override
    public void addVirtualKeys(Player p, String keyId, int delta) {
        virtualKeys.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        virtualKeys.get(p.getUniqueId()).merge(s(keyId), delta, Integer::sum);
        if (virtualKeys.get(p.getUniqueId()).get(s(keyId)) < 0)
            virtualKeys.get(p.getUniqueId()).put(s(keyId), 0);
    }

    private static String locKey(Location l){
        return l.getWorld().getName()+":"+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }

    @Override
    public void bind(Location loc, String crateId) {
        bindings.put(locKey(loc), s(crateId));
        saveBindings();
        crate(crateId).ifPresent(c -> HologramManager.upsert(plugin, loc, c));
    }

    @Override public void unbind(Location loc) {
        bindings.remove(locKey(loc));
        saveBindings();
        HologramManager.remove(plugin, loc);
    }

    @Override public String boundAt(Location loc){ return bindings.get(locKey(loc)); }

    private void saveBindings() {
        try {
            File f = new File(plugin.getDataFolder(), "bindings.yml");
            YamlConfiguration cfg = new YamlConfiguration();
            bindings.forEach(cfg::set);
            cfg.save(f);
        } catch (Exception ignored) {}
    }

    @Override
    public Reward open(Player p, Crate crate) {
        Reward r = crate.picker().pick(new Random());
        if (r == null) return null;

        // give items
        r.giveItems(p);
        // run commands
        for (String c : r.commands()) {
            String cmd = c.replace("<player>", p.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        // optional announce
        if (r.announce()) {
            Bukkit.broadcastMessage("§d" + p.getName() + " §7won §e" + r.id() + " §7from §b" + crate.displayName());
        }
        return r;
    }

    @Override
    public double chancePercent(Crate crate, Reward r) {
        double total = crate.rewards().stream().mapToDouble(Reward::weight).sum();
        if (total <= 0) return 0;
        return (r.weight() / total) * 100.0;
    }
}
