package me.ycxmbo.minecrates.listener;

import me.ycxmbo.minecrates.service.CrateService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Warms the player-data cache on join and flushes it on quit so virtual keys,
 * cooldowns and stats survive restarts.
 */
public final class PlayerSessionListener implements Listener {

    private final CrateService service;

    public PlayerSessionListener(CrateService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        service.onJoin(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        service.onQuit(e.getPlayer().getUniqueId());
    }
}
