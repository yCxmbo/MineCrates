package me.ycxmbo.minecrates.hook;

import eu.decentsoftware.holograms.api.DHAPI;
import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class HologramManager {

    private final boolean present;
    private final Plugin plugin;

    public HologramManager(MineCrates plugin) {
        this.plugin = plugin;
        this.present = Bukkit.getPluginManager().getPlugin("DecentHolograms") != null;
        if (!present) plugin.getLogger().info("[MineCrates] DecentHolograms not present – holograms disabled.");
    }

    private String id(Location l) {
        return "mc_"+l.getWorld().getName()+"_"+l.getBlockX()+"_"+l.getBlockY()+"_"+l.getBlockZ();
    }

    public void upsert(Location loc, Crate crate) {
        if (!present || crate == null || !crate.hologramEnabled()) return;
        Location base = loc.clone().add(0.5, crate.hologramYOffset(), 0.5);
        String name = id(loc);
        try {
            if (DHAPI.getHologram(name) == null) {
                DHAPI.createHologram(name, base, crate.holoLines());
            } else {
                DHAPI.moveHologram(name, base);
                DHAPI.setHologramLines(DHAPI.getHologram(name), crate.holoLines());
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[MineCrates] DH upsert failed at " + name + ": " + t.getMessage());
        }
    }

    public void remove(Location loc) {
        if (!present) return;
        String name = id(loc);
        try {
            if (DHAPI.getHologram(name) != null) DHAPI.removeHologram(name);
        } catch (Throwable t) {
            plugin.getLogger().warning("[MineCrates] DH remove failed at " + name + ": " + t.getMessage());
        }
    }

    public void refreshAll(Map<Location,String> bindings, Function<String, Crate> resolver) {
        if (!present) return;
        // remove orphans first
        // (DH doesn’t provide quick list by prefix; we simply upsert all live bindings)
        bindings.forEach((loc, id) -> upsert(loc, resolver.apply(id)));
    }
}
