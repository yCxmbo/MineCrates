package me.ycxmbo.minecrates.crate;

import me.ycxmbo.minecrates.util.ItemUtil;
import me.ycxmbo.minecrates.MineCrates;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class Crate {

    public enum Type { BLOCK, VIRTUAL }
    public enum CostCurrency { VAULT, EXP, EXP_LEVELS }
    public enum AnimationType { ROULETTE, REVEAL, CASCADE }

    private final String id;
    private final String display;
    private final Type type;
    private final boolean requiresKey;
    private final long cooldownMillis;
    private final Key key;
    private final String keyDisplayOverride;

    private final List<Reward> rewards;
    private final RewardPicker picker;

    // Hologram
    private final boolean holoEnabled;
    private final double holoYOffset;
    private final List<String> holoLines;

    // Particles (per-crate)
    public enum ParticleShape {
        RING,
        RING_SPIN,
        SPIRAL,
        DOUBLE_HELIX,
        COLUMN,
        STAR
    }
    private final boolean particlesEnabled;
    private final ParticleShape particleShape;
    private final String particleType; // org.bukkit.Particle name
    private final double particleRadius;
    private final double particleYOffset;
    private final int particlePoints;

    // Cost (optional)
    private final boolean costEnabled;
    private final CostCurrency costCurrency;
    private final double costAmount;

    // Animation (per-crate)
    private final AnimationType animationType;
    private final int rouletteCycles;
    private final int rouletteSpeedTicks;
    private final int revealFlickers;
    private final int revealSpeedTicks;
    private final int cascadeSpeedTicks;
    private final long closeDelayTicks;

    public Crate(String id, String display, Type type, boolean requiresKey, long cooldownMillis,
                 Key key, String keyDisplayOverride, List<Reward> rewards,
                 boolean holoEnabled, double holoYOffset, List<String> holoLines,
                 boolean costEnabled, CostCurrency costCurrency, double costAmount,
                 boolean particlesEnabled, ParticleShape particleShape, String particleType,
                 double particleRadius, double particleYOffset, int particlePoints,
                 AnimationType animationType, int rouletteCycles, int rouletteSpeedTicks,
                 int revealFlickers, int revealSpeedTicks, int cascadeSpeedTicks, long closeDelayTicks) {

        this.id = id.toLowerCase(Locale.ROOT);
        this.display = display;
        this.type = type == null ? Type.BLOCK : type;
        this.requiresKey = requiresKey;
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.key = key;
        this.keyDisplayOverride = (keyDisplayOverride == null || keyDisplayOverride.isBlank()) ? null : keyDisplayOverride;

        this.rewards = Collections.unmodifiableList(new ArrayList<>(rewards));
        this.picker = new RewardPicker(this.rewards);

        this.holoEnabled = holoEnabled;
        this.holoYOffset = holoYOffset;
        this.holoLines = Collections.unmodifiableList(new ArrayList<>(holoLines));

        this.costEnabled = costEnabled;
        this.costCurrency = costCurrency == null ? CostCurrency.VAULT : costCurrency;
        this.costAmount = Math.max(0D, costAmount);

        this.particlesEnabled = particlesEnabled;
        this.particleShape = particleShape == null ? ParticleShape.RING : particleShape;
        this.particleType = (particleType == null || particleType.isEmpty()) ? "FLAME" : particleType;
        this.particleRadius = Math.max(0.5, particleRadius);
        this.particleYOffset = particleYOffset;
        this.particlePoints = Math.max(8, particlePoints);

        this.animationType = animationType == null ? AnimationType.ROULETTE : animationType;
        this.rouletteCycles = Math.max(1, rouletteCycles);
        this.rouletteSpeedTicks = Math.max(1, rouletteSpeedTicks);
        this.revealFlickers = Math.max(1, revealFlickers);
        this.revealSpeedTicks = Math.max(1, revealSpeedTicks);
        this.cascadeSpeedTicks = Math.max(1, cascadeSpeedTicks);
        this.closeDelayTicks = Math.max(0L, closeDelayTicks);
    }

    public String id() { return id; }
    public String displayName() { return display; }
    public Type type() { return type; }
    public boolean requiresKey() { return requiresKey; }
    public long cooldownMillis() { return cooldownMillis; }
    public Key key() { return key; }
    public String keyDisplayOverride() { return keyDisplayOverride; }
    public List<Reward> rewards() { return rewards; }
    public RewardPicker picker() { return picker; }

    public boolean hologramEnabled() { return holoEnabled; }
    public double hologramYOffset() { return holoYOffset; }
    public List<String> holoLines() { return holoLines; }

    public boolean costEnabled() { return costEnabled; }
    public CostCurrency costCurrency() { return costCurrency; }
    public double costAmount() { return costAmount; }

    // Particles getters
    public boolean particlesEnabled() { return particlesEnabled; }
    public ParticleShape particleShape() { return particleShape; }
    public String particleType() { return particleType; }
    public double particleRadius() { return particleRadius; }
    public double particleYOffset() { return particleYOffset; }
    public int particlePoints() { return particlePoints; }

    // Animation getters
    public AnimationType animationType() { return animationType; }
    public int rouletteCycles() { return rouletteCycles; }
    public int rouletteSpeedTicks() { return rouletteSpeedTicks; }
    public int revealFlickers() { return revealFlickers; }
    public int revealSpeedTicks() { return revealSpeedTicks; }
    public int cascadeSpeedTicks() { return cascadeSpeedTicks; }
    public long closeDelayTicks() { return closeDelayTicks; }

    public static Crate fromSection(String id, ConfigurationSection sec, Map<String, Key> keys) {
        String display = sec.getString("display", id);
        Type type = Type.valueOf(sec.getString("type","BLOCK").toUpperCase(Locale.ROOT));
        boolean reqKey = sec.getBoolean("requires-key", true);
        String keyId = sec.getString("key", "");
        String keyDisplayOverride = sec.getString("key-display", null);
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

        // particles (per-crate)
        boolean pEnabled = true;
        ParticleShape pShape = ParticleShape.RING;
        String pType = "FLAME";
        double pRadius = 1.8;
        double pYOffset = 1.2;
        int pPoints = 48;
        if (sec.isConfigurationSection("particles")) {
            ConfigurationSection ps = sec.getConfigurationSection("particles");
            pEnabled = ps.getBoolean("enabled", true);
            try { pShape = ParticleShape.valueOf(ps.getString("shape","RING").toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
            pType = ps.getString("type", "FLAME");
            pRadius = ps.getDouble("radius", 1.8);
            pYOffset = ps.getDouble("y-offset", 1.2);
            pPoints = ps.getInt("points", 48);
        }

        // animation (per-crate), with fallback to global config defaults
        var global = MineCrates.get() != null ? MineCrates.get().getConfig() : null;
        String defType = global != null ? global.getString("animation.type", "ROULETTE") : "ROULETTE";
        int defRouletteCycles = global != null ? global.getInt("animation.roulette.cycles", 40) : 40;
        int defRouletteSpeed = global != null ? global.getInt("animation.roulette.speed-ticks", 2) : 2;
        int defRevealFlickers = global != null ? global.getInt("animation.reveal.flickers", 16) : 16;
        int defRevealSpeed = global != null ? global.getInt("animation.reveal.speed-ticks", 6) : 6;
        int defCascadeSpeed = global != null ? global.getInt("animation.cascade.speed-ticks", 2) : 2;
        long defCloseDelay = global != null ? global.getLong("animation.close-delay-ticks", 20L) : 20L;

        AnimationType aType = AnimationType.valueOf(defType.toUpperCase(Locale.ROOT));
        int aRouletteCycles = defRouletteCycles;
        int aRouletteSpeed = defRouletteSpeed;
        int aRevealFlickers = defRevealFlickers;
        int aRevealSpeed = defRevealSpeed;
        int aCascadeSpeed = defCascadeSpeed;
        long aCloseDelay = defCloseDelay;

        if (sec.isConfigurationSection("animation")) {
            ConfigurationSection as = sec.getConfigurationSection("animation");
            String t = as.getString("type", defType);
            try { aType = AnimationType.valueOf(t.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
            // nested sections per type (same keys as global)
            ConfigurationSection rs = as.getConfigurationSection("roulette");
            if (rs != null) {
                aRouletteCycles = rs.getInt("cycles", defRouletteCycles);
                aRouletteSpeed = Math.max(1, rs.getInt("speed-ticks", defRouletteSpeed));
            }
            ConfigurationSection rev = as.getConfigurationSection("reveal");
            if (rev != null) {
                aRevealFlickers = rev.getInt("flickers", defRevealFlickers);
                aRevealSpeed = Math.max(1, rev.getInt("speed-ticks", defRevealSpeed));
            }
            ConfigurationSection cas = as.getConfigurationSection("cascade");
            if (cas != null) {
                aCascadeSpeed = Math.max(1, cas.getInt("speed-ticks", defCascadeSpeed));
            }
            aCloseDelay = as.getLong("close-delay-ticks", defCloseDelay);
        }

        return new Crate(id, display, type, reqKey, cooldownMs, key, keyDisplayOverride, rewards,
                ho, yoff, lines, costEnabled, currency, amount,
                pEnabled, pShape, pType, pRadius, pYOffset, pPoints,
                aType, aRouletteCycles, aRouletteSpeed, aRevealFlickers, aRevealSpeed, aCascadeSpeed, aCloseDelay);
    }
}
