package me.ycxmbo.minecrates.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import org.bukkit.OfflinePlayer;

public class MineCratesExpansion extends PlaceholderExpansion {

    @Override public String getIdentifier() { return "minecrates"; }
    @Override public String getAuthor() { return "ycxmbo"; }
    @Override public String getVersion() { return MineCrates.get().getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer p, String params) {
        if (p == null) return "";
        var plugin = MineCrates.get();

        // %minecrates_keys_{crateId}%
        if (params.startsWith("keys_")) {
            String id = params.substring("keys_".length());
            int vk = plugin.getDataStore().getVirtualKeys(p.getUniqueId(), id);
            // physical keys count would need scanning inventories; keep virtual for performance
            return String.valueOf(vk);
        }

        // %minecrates_opened%
        if (params.equalsIgnoreCase("opened")) {
            return String.valueOf(plugin.getDataStore().getOpensTotal(p.getUniqueId()));
        }

        // %minecrates_cooldown_{crateId}%
        if (params.startsWith("cooldown_")) {
            String id = params.substring("cooldown_".length());
            long now = System.currentTimeMillis() / 1000L;
            long cd = plugin.getDataStore().getCooldown(p.getUniqueId(), id);
            return String.valueOf(Math.max(0, cd - now));
        }

        // %minecrates_last_reward%
        if (params.equalsIgnoreCase("last_reward")) {
            return plugin.getDataStore().getLastRewardName(p.getUniqueId());
        }

        // %minecrates_crate_display_{crateId}%
        if (params.startsWith("crate_display_")) {
            String id = params.substring("crate_display_".length());
            Crate c = plugin.getCrateManager().getCrate(id);
            return c == null ? "" : c.getDisplayName();
        }
        return "";
    }
}
