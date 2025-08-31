package me.ycxmbo.minecrates.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class PreviewGuiListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (!PreviewGUI.isPreview(e.getView().getTopInventory())) return;
        e.setCancelled(true);
        if (e.getWhoClicked() == null) return;
        PreviewGUI.handleClick((org.bukkit.entity.Player) e.getWhoClicked(), e.getRawSlot());
    }
}
