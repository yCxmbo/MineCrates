package me.ycxmbo.minecrates.util;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class ParticlesManager {

    private static final Map<Location, BukkitTask> rings = new HashMap<>();

    private ParticlesManager() {}

    public static void startRing(MineCrates plugin, Location base, String crateId) {
        stop(base);
        Crate c = MineCrates.get().getCrateManager().getCrate(crateId);
        if (c == null) return;

        final double r = Math.max(1.8, c.getParticleRadius()); // expanded ring
        final int points = Math.max(48, c.getParticlePoints());
        final double yo = c.getParticleYOffset();
        final Particle type = Particle.valueOf(c.getParticleType());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (base.getWorld() == null) return;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double x = base.getX() + 0.5 + Math.cos(angle) * r;
                double z = base.getZ() + 0.5 + Math.sin(angle) * r;
                base.getWorld().spawnParticle(type, x, base.getY() + yo, z, 1, 0, 0, 0, 0);
            }
        }, 10L, Math.max(2L, c.getParticlePeriod()));
        rings.put(base, task);
    }

    public static void stop(Location base) {
        BukkitTask t = rings.remove(base);
        if (t != null) t.cancel();
    }

    public static void stopAll() {
        rings.values().forEach(BukkitTask::cancel);
        rings.clear();
    }

    public static void refreshAll() {
        stopAll();
        MineCrates pl = MineCrates.get();
        MineCrates.get().getDataStore().getAllCrateBindings().forEach((loc, id) -> startRing(pl, loc, id));
    }
}
