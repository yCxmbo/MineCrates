package me.ycxmbo.minecrates.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class EditorListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof CrateListGUI gui) {
            gui.handleClick(e);
        } else if (e.getInventory().getHolder() instanceof CrateEditGUI gui) {
            gui.handleClick(e);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof CrateEditGUI gui) {
            gui.handleClose(e);
        }
    }
}
