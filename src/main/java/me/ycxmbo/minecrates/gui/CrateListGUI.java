package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.service.CrateService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public final class CrateListGUI implements InventoryHolder {

    private final MineCrates plugin;
    private final CrateService service;
    private final Inventory inv;
    private final Map<Integer, String> slotToId = new HashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public CrateListGUI(MineCrates plugin, CrateService service) {
        this.plugin = plugin;
        this.service = service;
        this.inv = Bukkit.createInventory(this, 54, Component.text("Crate Editor"));
        rebuild();
    }

    private void rebuild() {
        inv.clear();
        slotToId.clear();
        int slot = 0;
        for (Crate crate : service.crates()) {
            if (slot >= inv.getSize()) break;
            ItemStack it = new ItemStack(Material.CHEST);
            ItemMeta meta = it.getItemMeta();
            meta.displayName(MM.deserialize("<yellow>" + crate.displayName() + "</yellow>"));
            it.setItemMeta(meta);
            inv.setItem(slot, it);
            slotToId.put(slot, crate.id());
            slot++;
        }
    }

    public void open(Player p) {
        p.openInventory(inv);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int slot = e.getRawSlot();
        String id = slotToId.get(slot);
        if (id == null) return;
        Crate crate = service.crate(id);
        if (crate == null) return;
        new CrateEditGUI(plugin, service, crate).open((Player) e.getWhoClicked());
    }
}
