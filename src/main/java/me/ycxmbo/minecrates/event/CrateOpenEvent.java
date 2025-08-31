package me.ycxmbo.minecrates.event;

import me.ycxmbo.minecrates.crate.Crate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player attempts to open a crate, after basic checks (cost/key)
 * but before the animation and reward selection.
 * Listeners may cancel to prevent opening.
 */
public final class CrateOpenEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private boolean cancelled;

    public CrateOpenEvent(Player player, Crate crate) {
        // Synchronous: fired on the main thread before opening animation
        super(false);
        this.player = player;
        this.crate = crate;
    }

    public Player getPlayer() { return player; }
    public Crate getCrate() { return crate; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
