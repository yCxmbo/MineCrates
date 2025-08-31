package me.ycxmbo.minecrates.config;

import me.ycxmbo.minecrates.MineCrates;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

/**
 * Centralized configuration and messages access.
 * - Saves defaults from resources on first run.
 * - Provides MiniMessage instance and helpers to fetch message strings by path.
 */
public final class ConfigManager {

    private final MineCrates plugin;
    private final MiniMessage mm;

    private FileConfiguration config;
    private YamlConfiguration messages;

    public ConfigManager(MineCrates plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.mm = MiniMessage.miniMessage();
    }

    public void load() {
        // config.yml
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        // messages.yml
        File msgFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!msgFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) Files.copy(in, msgFile.toPath());
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to save default messages.yml: " + ex.getMessage());
            }
        }
        this.messages = YamlConfiguration.loadConfiguration(msgFile);
        this.messages.setDefaults(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(
                Objects.requireNonNull(plugin.getResource("messages.yml")), StandardCharsets.UTF_8)));
        this.messages.options().copyDefaults(true);
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        load();
    }

    public MiniMessage miniMessage() { return mm; }

    public FileConfiguration config() { return config; }

    public String msg(String path) {
        String s = messages.getString(path);
        if (s == null) s = "<gray>" + path + "</gray>";
        return s;
    }

    public String msg(String path, Map<String, String> placeholders) {
        String raw = msg(path);
        if (placeholders == null || placeholders.isEmpty()) return raw;
        String out = raw;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("<" + e.getKey() + ">", e.getValue());
        }
        return out;
    }
}

