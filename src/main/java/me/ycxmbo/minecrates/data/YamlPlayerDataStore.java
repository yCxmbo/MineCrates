package me.ycxmbo.minecrates.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Flat-file {@link PlayerDataStore} backend. Each player is stored as
 * {@code <dataFolder>/<uuid>.yml}. All file IO runs synchronously on the caller
 * thread for the {@code save}/{@code load} variants; the {@code *Async} variants
 * dispatch onto a dedicated single-thread executor so the server thread is never
 * blocked.
 */
public final class YamlPlayerDataStore implements PlayerDataStore {

    private final File folder;
    private final Logger logger;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MineCrates-PlayerData-IO");
        t.setDaemon(true);
        return t;
    });

    public YamlPlayerDataStore(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("Could not create player data folder: " + folder.getAbsolutePath());
        }
    }

    private File fileFor(UUID id) {
        return new File(folder, id + ".yml");
    }

    @Override
    public PlayerData load(UUID id) {
        PlayerData data = new PlayerData(id);
        File f = fileFor(id);
        if (!f.exists()) return data;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);

        data.setOpened(y.getLong("opened", 0L));
        data.setLastReward(y.getString("last-reward", ""));

        ConfigurationSection keys = y.getConfigurationSection("virtual-keys");
        if (keys != null) {
            for (String k : keys.getKeys(false)) data.virtualKeys().put(k, keys.getInt(k));
        }
        ConfigurationSection cds = y.getConfigurationSection("cooldowns");
        if (cds != null) {
            for (String k : cds.getKeys(false)) data.cooldowns().put(k, cds.getLong(k));
        }
        ConfigurationSection pity = y.getConfigurationSection("pity");
        if (pity != null) {
            for (String k : pity.getKeys(false)) data.pityCounters().put(k, pity.getInt(k));
        }
        return data;
    }

    @Override
    public CompletableFuture<PlayerData> loadAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> load(id), io);
    }

    @Override
    public void save(PlayerData data) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("opened", data.opened());
        y.set("last-reward", data.lastReward());
        data.virtualKeys().forEach((k, v) -> y.set("virtual-keys." + k, v));
        data.cooldowns().forEach((k, v) -> y.set("cooldowns." + k, v));
        data.pityCounters().forEach((k, v) -> y.set("pity." + k, v));
        try {
            y.save(fileFor(data.id()));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed saving player data for " + data.id(), ex);
        }
    }

    @Override
    public CompletableFuture<Void> saveAsync(PlayerData data) {
        return CompletableFuture.runAsync(() -> save(data), io);
    }

    @Override
    public CompletableFuture<Void> saveAll(Collection<PlayerData> all) {
        return CompletableFuture.runAsync(() -> all.forEach(this::save), io);
    }

    @Override
    public void close() {
        io.shutdown();
    }
}
