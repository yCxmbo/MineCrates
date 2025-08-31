package me.ycxmbo.minecrates.event;

import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired just before a reward is granted to a player.
 * Listeners may cancel to block granting the reward (e.g., custom logic).
 */
public final class CrateRewardGiveEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private final Reward reward;
    private boolean cancelled;

    public CrateRewardGiveEvent(Player player, Crate crate, Reward reward) {
        // Synchronous: reward is granted on the main thread
        super(false);
        this.player = player;
        this.crate = crate;
        this.reward = reward;
    }

    public Player getPlayer() { return player; }
    public Crate getCrate() { return crate; }
    public Reward getReward() { return reward; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
