package me.ycxmbo.minecrates.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class EditorListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof CrateListGUI gui) {
            gui.handleClick(e);
        } else if (e.getInventory().getHolder() instanceof CrateEditGUI gui) {
            gui.handleClick(e);
        } else if (e.getInventory().getHolder() instanceof RewardDetailGUI gui) {
            gui.handleClick(e);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof CrateEditGUI gui) {
            gui.handleClose(e);
        } else if (e.getInventory().getHolder() instanceof RewardDetailGUI gui) {
            gui.handleClose(e);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        // If a rename session is active, consume the message
        org.bukkit.entity.Player p = e.getPlayer();
        boolean used = CrateEditGUI.handleChatCrateRename(p, e.getMessage());
        if (!used) used = CrateEditGUI.handleChatHologramLines(p, e.getMessage());
        if (!used) used = CrateEditGUI.handleChatRename(p, e.getMessage());
        if (!used) used = RewardDetailGUI.handleChatInput(p, e.getMessage());
        if (used) e.setCancelled(true);
    }
}
