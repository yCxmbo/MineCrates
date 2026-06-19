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
                    case HEART -> drawHeart(base, p, c.particleRadius(), c.particlePoints());
                    case VORTEX -> drawVortex(base, p, c.particleRadius(), c.particlePoints());
                    case SPHERE -> drawSphere(base, p, c.particleRadius(), c.particlePoints());
                    case WAVE -> drawWave(base, p, c.particleRadius(), c.particlePoints());
                    case GALAXY -> drawGalaxy(base, p, c.particleRadius(), c.particlePoints());
                    case ATOM -> drawAtom(base, p, c.particleRadius(), c.particlePoints());
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

    /** A heart outline standing upright, slowly rotating around the vertical axis. */
    private void drawHeart(Location base, Particle p, double radius, int points) {
        int pts = Math.max(24, points);
        double scale = radius / 17.0; // the parametric heart spans roughly +/-16
        double rot = (tick * 0.08) % (2 * Math.PI);
        double cos = Math.cos(rot), sin = Math.sin(rot);
        for (int i = 0; i < pts; i++) {
            double t = (2 * Math.PI * i) / pts;
            double hx = 16 * Math.pow(Math.sin(t), 3);
            double hy = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            double x = hx * scale;
            double y = hy * scale;
            // Rotate the flat heart around the Y axis so it is visible from all sides.
            double px = x * cos;
            double pz = x * sin;
            base.getWorld().spawnParticle(p, base.getX() + px, base.getY() + y, base.getZ() + pz, 1, 0, 0, 0, 0);
        }
    }

    /** A tornado-like cone: radius grows with height while spinning. */
    private void drawVortex(Location base, Particle p, double radius, int points) {
        int pts = Math.max(24, points);
        double height = Math.max(1.6, radius + 1.0);
        double rot = (tick * 0.22) % (2 * Math.PI);
        int turns = 4;
        for (int i = 0; i < pts; i++) {
            double f = i / (double) pts; // 0..1 bottom to top
            double r = radius * (0.15 + 0.85 * f);
            double a = (2 * Math.PI * turns * f) + rot;
            double y = base.getY() - height / 2.0 + height * f;
            double x = Math.cos(a) * r;
            double z = Math.sin(a) * r;
            base.getWorld().spawnParticle(p, base.getX() + x, y, base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    /** Points spread over a sphere surface using a golden-angle spiral. */
    private void drawSphere(Location base, Particle p, double radius, int points) {
        int pts = Math.max(24, points);
        double rot = (tick * 0.06) % (2 * Math.PI);
        double golden = Math.PI * (3 - Math.sqrt(5)); // golden angle
        for (int i = 0; i < pts; i++) {
            double y = 1 - (i / (double) (pts - 1)) * 2; // 1..-1
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double a = golden * i + rot;
            double x = Math.cos(a) * r * radius;
            double z = Math.sin(a) * r * radius;
            base.getWorld().spawnParticle(p, base.getX() + x, base.getY() + y * radius, base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    /** A ring whose height ripples like a wave, the wave travelling around it. */
    private void drawWave(Location base, Particle p, double radius, int points) {
        int pts = Math.max(16, points);
        double amplitude = Math.max(0.25, radius * 0.35);
        double phase = tick * 0.2;
        for (int i = 0; i < pts; i++) {
            double a = (2 * Math.PI * i) / pts;
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            double y = Math.sin(a * 3 + phase) * amplitude;
            base.getWorld().spawnParticle(p, base.getX() + x, base.getY() + y, base.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    /** A flat galaxy with several logarithmic spiral arms rotating together. */
    private void drawGalaxy(Location base, Particle p, double radius, int points) {
        int arms = 3;
        int perArm = Math.max(8, points / arms);
        double rot = (tick * 0.1) % (2 * Math.PI);
        for (int arm = 0; arm < arms; arm++) {
            double armOffset = (2 * Math.PI * arm) / arms;
            for (int i = 0; i < perArm; i++) {
                double f = i / (double) perArm; // 0..1 from centre outwards
                double r = radius * f;
                double a = armOffset + rot + f * Math.PI * 2.5; // sweep of the arm
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                base.getWorld().spawnParticle(p, base.getX() + x, base.getY(), base.getZ() + z, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Three orbital rings on different planes, like a stylised atom. */
    private void drawAtom(Location base, Particle p, double radius, int points) {
        int perOrbit = Math.max(12, points / 3);
        double rot = (tick * 0.18) % (2 * Math.PI);
        // tilt angles for the three orbits (radians)
        double[] tilts = { 0.0, Math.PI / 3, 2 * Math.PI / 3 };
        for (double tilt : tilts) {
            double ct = Math.cos(tilt), st = Math.sin(tilt);
            for (int i = 0; i < perOrbit; i++) {
                double a = (2 * Math.PI * i) / perOrbit + rot;
                double cx = Math.cos(a) * radius;
                double cy = Math.sin(a) * radius;
                // rotate the flat circle around the X axis by the tilt
                double x = cx;
                double y = cy * ct;
                double z = cy * st;
                base.getWorld().spawnParticle(p, base.getX() + x, base.getY() + y, base.getZ() + z, 1, 0, 0, 0, 0);
            }
        }
    }
}
