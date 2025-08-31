package me.ycxmbo.minecrates.util;

import me.ycxmbo.minecrates.crate.Crate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class Particles {

    private final org.bukkit.plugin.Plugin plugin;
    private final Map<Location, BukkitTask> rings = new HashMap<>();

    public Particles(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    public void start(Location loc, Crate crate) {
        stop(loc);
        final double r = Math.max(0.5, crate.getParticleRadius());
        final int points = Math.max(8, crate.getParticlePoints());
        final double yoff = crate.getParticleYOffset();
        final Particle type = safe(crate.getParticleType(), Particle.FLAME);
        final int period = Math.max(1, crate.getParticlePeriod());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (loc.getWorld() == null) return;
            for (int i = 0; i < points; i++) {
                double ang = 2 * Math.PI * i / points;
                double x = r * Math.cos(ang);
                double z = r * Math.sin(ang);
                Location at = loc.clone().add(0.5 + x, yoff + 0.2, 0.5 + z);
                loc.getWorld().spawnParticle(type, at, 1, 0, 0, 0, 0);
            }
        }, 20L, period);
        rings.put(loc, task);
    }

    public void stop(Location loc) {
        BukkitTask t = rings.remove(loc);
        if (t != null) t.cancel();
    }

    public void stopAll() {
        for (BukkitTask t : rings.values()) t.cancel();
        rings.clear();
    }

    private static Particle safe(String name, Particle def) {
        try { return Particle.valueOf(name.toUpperCase()); } catch (Exception ignored) { return def; }
    }
}
