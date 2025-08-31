package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.util.EconomyHook;
import me.ycxmbo.minecrates.util.ItemUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class OpenFlow {

    private OpenFlow() {}

    /**
     * Executes all prechecks then defers to animation GUI.
     * Assumes key checks were done earlier if required.
     */
    public static void open(Player p, Crate crate) {
        long now = System.currentTimeMillis() / 1000L;
        long cdUntil = MineCrates.get().getDataStore().getCooldown(p.getUniqueId(), crate.getId());
        if (cdUntil > now && !p.hasPermission("minecrates.bypass.cooldown")) {
            long remain = cdUntil - now;
            p.sendMessage(ItemUtil.color("&cPlease wait &e" + remain + "s &cbefore opening again."));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return;
        }

        if (crate.isCostEnabled() && !p.hasPermission("minecrates.bypass.cost")) {
            if (!EconomyHook.canAfford(p, crate) || !EconomyHook.charge(p, crate)) {
                p.sendMessage(ItemUtil.color("&cYou can't afford this crate (&e" + crate.getCostAmount() + " " + crate.getCostCurrency() + "&c)."));
                return;
            }
        }

        // cooldown set up-front to avoid dup spam on fast-click
        if (crate.getCooldownSec() > 0 && !p.hasPermission("minecrates.bypass.cooldown")) {
            MineCrates.get().getDataStore().setCooldown(p.getUniqueId(), crate.getId(), now + crate.getCooldownSec());
        }

        switch (crate.getAnimationType()) {
            case ROULETTE -> OpenAnimationGUI.openRoulette(p, crate);
            case REVEAL -> OpenAnimationGUI.openReveal(p, crate);
        }
    }
}
