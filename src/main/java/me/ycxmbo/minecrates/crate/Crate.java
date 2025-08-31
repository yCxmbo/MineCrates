package me.ycxmbo.minecrates.crate;

import me.ycxmbo.minecrates.util.ItemUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class Crate {

    public enum Type { BLOCK, VIRTUAL }
    public enum CostCurrency { VAULT, EXP, EXP_LEVELS }

    private final String id;
    private final String display;
    private final Type type;
    private final boolean requiresKey;
    private final long cooldownMillis;
    private final Key key;

    private final List<Reward> rewards;
    private final RewardPicker picker;

    // Hologram
    private final boolean holoEnabled;
    private final double holoYOffset;
    private final List<String> holoLines;

    // Cost (optional)
    private final boolean costEnabled;
    private final CostCurrency costCurrency;
    private final double costAmount;

    public Crate(String id, String display, Type type, boolean requiresKey, long cooldownMillis,
                 Key key, List<Reward> rewards,
                 boolean holoEnabled, double holoYOffset, List<String> holoLines,
                 boolean costEnabled, CostCurrency costCurrency, double costAmount) {

        this.id = id.toLowerCase(Locale.ROOT);
        this.display = display;
        this.type = type == null ? Type.BLOCK : type;
        this.requiresKey = requiresKey;
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.key = key;

        this.rewards = Collections.unmodifiableList(new ArrayList<>(rewards));
        this.picker = new RewardPicker(this.rewards);

        this.holoEnabled = holoEnabled;
        this.holoYOffset = holoYOffset;
        this.holoLines = Collections.unmodifiableList(new ArrayList<>(holoLines));

        this.costEnabled = costEnabled;
        this.costCurrency = costCurrency == null ? CostCurrency.VAULT : costCurrency;
        this.costAmount = Math.max(0D, costAmount);
    }

    public String id() { return id; }
    public String displayName() { return display; }
    public Type type() { return type; }
    public boolean requiresKey() { return requiresKey; }
    public long cooldownMillis() { return cooldownMillis; }
    public Key key() { return key; }
    public List<Reward> rewards() { return rewards; }
    public RewardPicker picker() { return picker; }

    public boolean hologramEnabled() { return holoEnabled; }
    public double hologramYOffset() { return holoYOffset; }
    public List<String> holoLines() { return holoLines; }

    public boolean costEnabled() { return costEnabled; }
    public CostCurrency costCurrency() { return costCurrency; }
    public double costAmount() { return costAmount; }

    public static Crate fromSection(String id, ConfigurationSection sec, Map<String, Key> keys) {
        String display = sec.getString("display", id);
        Type type = Type.valueOf(sec.getString("type","BLOCK").toUpperCase(Locale.ROOT));
        boolean reqKey = sec.getBoolean("requires-key", true);
        String keyId = sec.getString("key", "");
        Key key = keys.getOrDefault(keyId, new Key("dummy_key", "Key", new ItemStack(Material.TRIPWIRE_HOOK)));
        long cooldownMs = sec.getLong("cooldown-seconds", 0) * 1000L;

        // hologram
        boolean ho = false;
        double yoff = 1.25;
        List<String> lines = List.of("<gold>"+display+"</gold>", "<gray>Right-click with a key</gray>");
        if (sec.isConfigurationSection("hologram")) {
            ConfigurationSection h = sec.getConfigurationSection("hologram");
            ho = h.getBoolean("enabled", false);
            yoff = h.getDouble("y-offset", 1.25);
            if (h.isList("lines")) lines = new ArrayList<>(h.getStringList("lines"));
        }

        // rewards
        List<Reward> rewards = new ArrayList<>();
        ConfigurationSection rsec = sec.getConfigurationSection("rewards");
        if (rsec != null) {
            for (String rId : rsec.getKeys(false)) {
                ConfigurationSection rs = rsec.getConfigurationSection(rId);
                if (rs == null) continue;
                rewards.add(Reward.fromSection(rId, rs));
            }
        }

        // cost
        boolean costEnabled = false;
        CostCurrency currency = CostCurrency.VAULT;
        double amount = 0D;
        if (sec.isConfigurationSection("cost")) {
            ConfigurationSection c = sec.getConfigurationSection("cost");
            costEnabled = c.getBoolean("enabled", false);
            currency = CostCurrency.valueOf(c.getString("currency", "VAULT").toUpperCase(Locale.ROOT));
            amount = c.getDouble("amount", 0D);
        }

        return new Crate(id, display, type, reqKey, cooldownMs, key, rewards,
                ho, yoff, lines, costEnabled, currency, amount);
    }
}
