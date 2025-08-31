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

    public RingParticles(Plugin plugin, CrateService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<Location,String> e : service.allBindings().entrySet()) {
                Location base = e.getKey().clone().add(0.5, 0.2, 0.5);
                Crate c = service.crate(e.getValue());
                double radius = 1.8; // slightly expanded ring
                int points = 48;
                for (int i = 0; i < points; i++) {
                    double a = (2 * Math.PI * i) / points;
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    base.getWorld().spawnParticle(Particle.FLAME, base.getX()+x, base.getY(), base.getZ()+z, 1, 0, 0, 0, 0);
                }
            }
        }, 20L, 10L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }
}
