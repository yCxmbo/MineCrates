package me.ycxmbo.minecrates.listener;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.gui.OpenFlow;
import me.ycxmbo.minecrates.gui.PreviewGUI;
import me.ycxmbo.minecrates.util.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CrateInteractListener implements Listener {

    private final me.ycxmbo.minecrates.crate.CrateManager manager;
    private final me.ycxmbo.minecrates.data.DataStore data;

    public CrateInteractListener(me.ycxmbo.minecrates.crate.CrateManager manager,
                                 me.ycxmbo.minecrates.data.DataStore data) {
        this.manager = manager;
        this.data = data;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getHand() != EquipmentSlot.HAND) return;

        Block b = e.getClickedBlock();
        String id = data.getCrateAt(b.getLocation());
        if (id == null) return;

        Crate crate = manager.getCrate(id);
        if (crate == null) return;

        Player p = e.getPlayer();

        // Shift-LEFT = remove binding (admin)
        if (p.isSneaking() && e.getAction().isLeftClick()) {
            if (!p.hasPermission("minecrates.set") && !p.hasPermission("minecrates.bypass.breakbound")) {
                Messages.error(p, "You can't remove this crate.");
                return;
            }
            data.removeCrateAt(b.getLocation());
            HologramBridge.remove(b.getLocation());
            ParticlesManager.stop(b.getLocation());
            Messages.msg(p, "&aCrate removed from that block.");
            e.setCancelled(true);
            return;
        }

        // LEFT click = preview
        if (e.getAction().isLeftClick()) {
            PreviewGUI.open(p, null, crate);
            e.setCancelled(true);
            return;
        }

        // RIGHT click = try open with a key in hand (BLOCK type)
        if (e.getAction().isRightClick() && crate.getType() == Crate.Type.BLOCK) {
            // Key check (requiresKey)
            if (crate.requiresKey()) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!KeyTag.isKeyFor(hand, id)) {
                    // push back + message
                    Vector away = p.getLocation().toVector().subtract(b.getLocation().add(0.5, 0.5, 0.5).toVector()).normalize().multiply(0.6).setY(0.3);
                    p.setVelocity(away);
                    if (MineCrates.get().getConfig().getBoolean("effects.pushback-particles", true)) {
                        b.getWorld().spawnParticle(Particle.SMOKE_NORMAL, b.getLocation().add(0.5, 1.1, 0.5), 12, 0.2, 0.2, 0.2, 0.01);
                    }
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.4f);
                    Messages.error(p, "You don't have a key to open this crate.");
                    e.setCancelled(true);
                    return;
                }
                // consume 1 key
                hand.setAmount(hand.getAmount() - 1);
                if (hand.getAmount() <= 0) p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
            // open with animation
            OpenFlow.open(p, crate);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        String id = data.getCrateAt(b.getLocation());
        if (id == null) return;

        if (!e.getPlayer().hasPermission("minecrates.bypass.breakbound")) {
            e.setCancelled(true);
            Messages.error(e.getPlayer(), "This block is bound to a crate.");
            return;
        }

        // allowed to break: clean up
        data.removeCrateAt(b.getLocation());
        HologramBridge.remove(b.getLocation());
        ParticlesManager.stop(b.getLocation());
    }
}
