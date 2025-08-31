package me.ycxmbo.minecrates.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.service.CrateService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

            // %minecrates_crate_name_{id}%
            if (params.startsWith("crate_name_")) {
                String crateId = params.substring("crate_name_".length());
                Crate crate = service.crate(crateId);
                return crate != null ? crate.displayName() : "";
            }

            // %minecrates_crate_display_{id}% (alias of crate_name)
            if (params.startsWith("crate_display_")) {
                String crateId = params.substring("crate_display_".length());
                Crate crate = service.crate(crateId);
                return crate != null ? crate.displayName() : "";
            }

            // %minecrates_chance_{crate}_{reward}% -> percent with two decimals
            if (params.startsWith("chance_")) {
                String rest = params.substring("chance_".length());
                int idx = rest.indexOf('_');
                if (idx > 0) {
                    String crateId = rest.substring(0, idx);
                    String rewardId = rest.substring(idx+1);
                    Crate crate = service.crate(crateId);
                    if (crate != null) {
                        for (var r : crate.rewards()) {
                            if (r.id().equalsIgnoreCase(rewardId)) {
                                double pct = service.weightPercent(crate, r) * 100.0;
                                return String.format(java.util.Locale.US, "%.2f", pct);
                            }
                        }
                    }
                }
                return "0";
            }

            // %minecrates_key_name_{id}%
            if (params.startsWith("key_name_")) {
                String keyId = params.substring("key_name_".length());
                ItemStack item = service.createKeyItem(keyId, 1);
                return item != null && item.hasItemMeta() ? item.getItemMeta().getDisplayName() : "";
            }

            // %minecrates_last_reward%
            if (params.equals("last_reward")) {
                return service.lastRewardId(p.getUniqueId());
            }
            return "";
        }
    }
}
