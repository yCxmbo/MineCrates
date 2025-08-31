package me.ycxmbo.minecrates.visual;

import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.service.CrateService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public final class RingParticles {

    private final Plugin plugin;
    private final CrateService service;
    private BukkitTask task;
    private int tick;

    public RingParticles(Plugin plugin, CrateService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick++;
            for (Map.Entry<Location,String> e : service.allBindings().entrySet()) {
                Location host = e.getKey();
                Crate c = service.crate(e.getValue());
                if (c == null || !c.particlesEnabled()) continue;

                // Center slightly above the bound block
                Location base = host.clone().add(0.5, c.particleYOffset(), 0.5);

                // Particle choice (fallback safe)
                Particle p;
                try { p = Particle.valueOf(c.particleType().toUpperCase(java.util.Locale.ROOT)); }
                catch (Exception ex) { p = Particle.FLAME; }

                // Shapes
                switch (c.particleShape()) {
                    case RING -> drawRing(base, p, c.particleRadius(), c.particlePoints(), 0);
                    case RING_SPIN -> {
                        double rot = (tick * 0.12) % (2 * Math.PI);
                        drawRing(base, p, c.particleRadius(), c.particlePoints(), rot);
                    }
                    case COLUMN -> drawColumn(base, p, c.particleRadius(), c.particlePoints());
                    case SPIRAL -> drawSpiral(base, p, c.particleRadius(), c.particlePoints(), 1);
                    case DOUBLE_HELIX -> drawDoubleHelix(base, p, c.particleRadius(), c.particlePoints());
                    case STAR -> drawStar(base, p, c.particleRadius(), c.particlePoints());
                    default -> drawRing(base, p, c.particleRadius(), c.particlePoints(), 0);
                }
            }
        }, 20L, 10L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    private void drawRing(Location base, Particle p, double radius, int points, double rot) {
        for (int i = 0; i < points; i++) {
            double a = (2 * Math.PI * i) / points + rot;
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            base.getWorld().spawnParticle(p, base.getX() + x, base.getY(), base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    private void drawColumn(Location base, Particle p, double radius, int points) {
        int levels = Math.max(3, Math.min(8, points / 8));
        int ringPts = Math.max(8, points / levels);
        double height = Math.max(1.2, Math.min(2.5, radius + 0.8));
        for (int l = 0; l < levels; l++) {
            double y = base.getY() - (height / 2.0) + (height * l / (levels - 1));
            double rot = ((tick * 0.08) + (l * 0.6)) % (2 * Math.PI);
            for (int i = 0; i < ringPts; i++) {
                double a = (2 * Math.PI * i) / ringPts + rot;
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                base.getWorld().spawnParticle(p, base.getX() + x, y, base.getZ() + z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawSpiral(Location base, Particle p, double radius, int points, int turns) {
        double height = Math.max(1.6, radius + 0.8);
        double rot = (tick * 0.15) % (2 * Math.PI);
        for (int i = 0; i < points; i++) {
            double f = i / (double) points; // 0..1
            double a = (2 * Math.PI * turns * f) + rot;
            double y = base.getY() - height / 2.0 + height * f;
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            base.getWorld().spawnParticle(p, base.getX() + x, y, base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    private void drawDoubleHelix(Location base, Particle p, double radius, int points) {
        int levels = Math.max(24, points);
        double height = Math.max(1.6, radius + 0.8);
        double rot = (tick * 0.15) % (2 * Math.PI);
        for (int i = 0; i < levels; i++) {
            double f = i / (double) levels; // 0..1
            double y = base.getY() - height / 2.0 + height * f;
            double a1 = (2 * Math.PI * f) + rot;
            double a2 = a1 + Math.PI; // opposite phase
            double x1 = Math.cos(a1) * radius;
            double z1 = Math.sin(a1) * radius;
            double x2 = Math.cos(a2) * radius;
            double z2 = Math.sin(a2) * radius;
            base.getWorld().spawnParticle(p, base.getX() + x1, y, base.getZ() + z1, 1, 0, 0, 0, 0);
            base.getWorld().spawnParticle(p, base.getX() + x2, y, base.getZ() + z2, 1, 0, 0, 0, 0);
        }
    }

    private void drawStar(Location base, Particle p, double radius, int points) {
        int pts = Math.max(10, points);
        double rot = (tick * 0.1) % (2 * Math.PI);
        for (int i = 0; i < pts; i++) {
            double t = (2 * Math.PI * i) / pts;
            // 5-point star radial modulation
            double r = radius * (0.65 + 0.35 * Math.cos(5 * t));
            double a = t + rot;
            double x = Math.cos(a) * r;
            double z = Math.sin(a) * r;
            base.getWorld().spawnParticle(p, base.getX() + x, base.getY(), base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }
}
