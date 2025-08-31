package me.ycxmbo.minecrates.util;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.hook.HologramManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class HologramBridge {
    private HologramBridge() {}

    public static void createOrUpdate(Location loc, Crate crate) {
        HologramManager.createOrUpdate(loc, crate);
    }
    public static void remove(Location loc) {
        HologramManager.remove(loc);
    }
    public static void refreshAll() {
        HologramManager.refreshAll();
    }
    public static void refreshFor(Player p) {
        // Viewer-specific refresh is unnecessary with DH default visibility
    }
}
