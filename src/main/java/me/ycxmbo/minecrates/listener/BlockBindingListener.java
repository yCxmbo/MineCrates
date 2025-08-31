package me.ycxmbo.minecrates.listener;

import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.util.ItemUtil;
import me.ycxmbo.minecrates.util.Messages;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class BlockBindingListener implements Listener {

    private final CrateService service;

    public BlockBindingListener(CrateService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Optional<String> bound = service.boundAt(b.getLocation());
        if (bound.isEmpty()) return;
        if (!e.getPlayer().hasPermission("minecrates.bypass.breakbound")) {
            e.setCancelled(true);
            Messages.msg(e.getPlayer(), "<red>This block is bound to a crate.</red>");
        } else {
            service.unbind(b.getLocation());
            Messages.msg(e.getPlayer(), "<gray>Unbound crate from this block.</gray>");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null || b.getType() == Material.AIR) return;

        Optional<String> bound = service.boundAt(b.getLocation());
        if (bound.isEmpty()) return;

        Player p = e.getPlayer();
        Crate crate = service.crate(bound.get());
        if (crate == null) return;

        // Shift-LEFT removes binding (admin convenience)
        if (e.getAction() == Action.LEFT_CLICK_BLOCK && p.isSneaking()) {
            if (p.hasPermission("minecrates.set")) {
                e.setCancelled(true);
                service.unbind(b.getLocation());
                Messages.msg(p, "<green>Crate unbound.</green>");
            }
            return;
        }

        // LEFT_CLICK previews the crate (non-sneaking)
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (!p.hasPermission("minecrates.preview." + crate.id())) {
                Messages.msg(p, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("perm.preview-deny"));
                return;
            }
            // open preview menu
            me.ycxmbo.minecrates.gui.PreviewGUI.open(p, null, crate);
            return;
        }

        // RIGHT_CLICK opens (or pushback if no key)
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);

            boolean hasKey = true;
            if (crate.requiresKey()) {
                hasKey = service.virtualKeys(p.getUniqueId(), crate.key().id()) > 0
                        || ItemUtil.hasKeyTag(p.getInventory().getItemInMainHand(), crate.key().id())
                        || ItemUtil.hasKeyTag(p.getInventory().getItemInOffHand(), crate.key().id());
            }

            if (!hasKey) {
                // push back & notify
                p.setVelocity(p.getLocation().getDirection().multiply(-0.5));
                Messages.msg(p, "<gray>[MineCrates]</gray> <red>You don't have a key to open this crate.</red>");
                return;
            }
            if (!p.hasPermission("minecrates.open." + crate.id())) {
                Messages.msg(p, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("perm.open-deny"));
                return;
            }
            service.open(p, crate);
        }
    }
}
