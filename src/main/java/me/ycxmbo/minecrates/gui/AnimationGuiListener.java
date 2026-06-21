package me.ycxmbo.minecrates.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Locks the crate opening animation inventory so players cannot pull the
 * rolling/displayed reward items out (or push their own items in) while the
 * crate is spinning. The animation grants the reward through its own callback;
 * any direct interaction with the inventory must be blocked.
 */
public final class AnimationGuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // Cancel any click while an animation inventory is open on top. This
        // covers clicks in the animation itself as well as shift-clicks, number
        // keys and double-click collection originating from the player's own
        // inventory that could otherwise move items into the locked view.
        if (OpenAnimationGUI.isAnimation(e.getView().getTopInventory())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (OpenAnimationGUI.isAnimation(e.getView().getTopInventory())) {
            e.setCancelled(true);
        }
    }
}
