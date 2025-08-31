package me.ycxmbo.minecrates.listener;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.gui.PreviewGUI;
import me.ycxmbo.minecrates.util.*;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
                Messages.msg(p, "<red>You can't remove this crate.</red>");
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

        // RIGHT click = attempt to open (service handles key and checks)
        if (e.getAction().isRightClick() && crate.getType() == Crate.Type.BLOCK) {
            MineCrates.get().crates().open(p, crate);
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
            Messages.msg(e.getPlayer(), "<red>This block is bound to a crate.</red>");
            return;
        }

        // allowed to break: clean up
        data.removeCrateAt(b.getLocation());
        HologramBridge.remove(b.getLocation());
        ParticlesManager.stop(b.getLocation());
    }
}
