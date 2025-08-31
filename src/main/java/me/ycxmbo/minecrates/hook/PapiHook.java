package me.ycxmbo.minecrates.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.service.CrateService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PapiHook {

    public PapiHook(MineCrates plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        new Expansion(plugin, plugin.crates()).register();
        plugin.getLogger().info("[MineCrates] PlaceholderAPI expansion registered.");
    }

    private static final class Expansion extends PlaceholderExpansion {
        private final MineCrates plugin;
        private final CrateService service;

        private Expansion(MineCrates plugin, CrateService service) {
            this.plugin = plugin;
            this.service = service;
        }

        @Override public @NotNull String getIdentifier() { return "minecrates"; }
        @Override public @NotNull String getAuthor() { return "ycxmbo"; }
        @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

        @Override
        public @Nullable String onPlaceholderRequest(Player p, @NotNull String params) {
            if (p == null) return "";

            // %minecrates_keys_{key}%
            if (params.startsWith("keys_")) {
                String keyId = params.substring("keys_".length());
                return String.valueOf(service.virtualKeys(p.getUniqueId(), keyId));
            }

            // %minecrates_opened%
            if (params.equals("opened")) {
                return String.valueOf(service.totalOpened(p.getUniqueId()));
            }

            // %minecrates_cooldown_{crate}%
            if (params.startsWith("cooldown_")) {
                String crateId = params.substring("cooldown_".length());
                return String.valueOf(service.cooldownRemaining(p.getUniqueId(), crateId));
            }

            // %minecrates_last_reward%
            if (params.equals("last_reward")) {
                return service.lastRewardId(p.getUniqueId());
            }
            return "";
        }
    }
}
