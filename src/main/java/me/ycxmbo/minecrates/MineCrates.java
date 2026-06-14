package me.ycxmbo.minecrates;

import me.ycxmbo.minecrates.command.MineCratesCommand;
import me.ycxmbo.minecrates.config.ConfigManager;
import me.ycxmbo.minecrates.gui.PreviewGUI;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.service.impl.SimpleCrateService;
import me.ycxmbo.minecrates.hook.VaultHook;
import me.ycxmbo.minecrates.hook.PapiHook;
import me.ycxmbo.minecrates.hook.HologramManager;
import me.ycxmbo.minecrates.listener.BlockBindingListener;
import me.ycxmbo.minecrates.visual.RingParticles;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class MineCrates extends JavaPlugin {

    /** bStats plugin id from https://bstats.org. Set to a value &gt; 0 to enable metrics. */
    private static final int BSTATS_PLUGIN_ID = 0;

    private static MineCrates INSTANCE;
    private CrateService crateService;
    private ConfigManager configManager;
    private VaultHook vault;
    private PapiHook papi;
    private HologramManager holograms;
    private RingParticles particles;

    public static MineCrates get() { return INSTANCE; }
    public CrateService crates() { return crateService; }
    public ConfigManager configManager() { return configManager; }
    public VaultHook vault() { return vault; }
    public HologramManager holograms() { return holograms; }

    @Override
    public void onEnable() {
        INSTANCE = this;
        final Logger log = getLogger();

        // Basic self-check
        log.info(() -> "[MineCrates] Booting on " + Bukkit.getVersion());
        
        // Configs & messages
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // Save default configs (we’ll add more files next drop)
        saveDefaultConfig();

        // Hooks (soft)
        vault = new VaultHook(this);
        papi = new PapiHook(this); // registers expansion if PAPI present
        holograms = new HologramManager(this);

        // Core service
        crateService = new SimpleCrateService(this, vault, holograms);
        crateService.reloadAllAsync().thenRun(() ->
                log.info("[MineCrates] Crates & keys loaded.")
        ).exceptionally(ex -> {
            log.log(Level.SEVERE, "[MineCrates] Failed to load data", ex);
            return null;
        });

        getServer().getPluginManager().registerEvents(new BlockBindingListener(crateService), this);
        getServer().getPluginManager().registerEvents(
                new me.ycxmbo.minecrates.listener.PlayerSessionListener(crateService), this);
        // Preview GUI
        new PreviewGUI(this, crateService, configManager);
        getServer().getPluginManager().registerEvents(new me.ycxmbo.minecrates.gui.EditorListener(), this);
        getServer().getPluginManager().registerEvents(new me.ycxmbo.minecrates.gui.BookEditListener(), this);
        particles = new RingParticles(this, crateService);
        particles.start();

        // Command
        final PluginCommand cmd = getCommand("minecrates");
        if (cmd != null) {
            MineCratesCommand exec = new MineCratesCommand(this, crateService);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        } else {
            log.severe("[MineCrates] Command 'minecrates' missing from plugin.yml!");
        }

        // Periodic autosave of cached player data
        int autosaveSeconds = getConfig().getInt("data.autosave-seconds", 300);
        if (autosaveSeconds > 0) {
            long ticks = autosaveSeconds * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> crateService.persistAll(), ticks, ticks);
        }

        // bStats metrics (opt-out via config; only active once a real plugin id is set)
        if (getConfig().getBoolean("data.metrics", true) && BSTATS_PLUGIN_ID > 0) {
            try {
                org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(this, BSTATS_PLUGIN_ID);
                metrics.addCustomChart(new org.bstats.charts.SingleLineChart(
                        "crates_loaded", () -> crateService.crates().size()));
            } catch (Throwable t) {
                log.log(Level.FINE, "bStats metrics could not be initialised", t);
            }
        }

        log.info("[MineCrates] Enabled.");
    }

    @Override
    public void onDisable() {
        try {
            if (crateService != null) crateService.shutdown();
        } catch (Throwable t) {
            getLogger().warning("[MineCrates] Shutdown error: " + t.getMessage());
        }
        try { if (holograms != null) holograms.shutdown(); } catch (Throwable ignored) {}
        getLogger().info("[MineCrates] Disabled.");

        if (particles != null) particles.stop();
    }
}
