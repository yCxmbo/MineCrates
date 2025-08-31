package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public final class PreviewGUI implements Listener {

    private final CrateService service;

    public PreviewGUI(CrateService service) {
        this.service = service;
        Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("MineCrates"));
    }

    public void open(Player p, Crate crate, int page, @SuppressWarnings("SameParameterValue") Reward.Rarity filter) {
        int size = 54;
        Inventory inv = Bukkit.createInventory(p, size, ItemUtil.mm("<gold>Preview</gold> <gray>•</gray> <white>" + crate.displayName() + "</white>"));
        // cache state in title via PDC is overkill; we’ll keep a simple map
        State.set(p, new State(crate.id(), page, filter));

        List<Reward> all = crate.rewards();
        if (filter != null) {
            all = all.stream().filter(r -> r.rarity() == filter).collect(Collectors.toList());
        }

        // body
        int per = 45;
        int start = Math.max(0, (page-1)*per);
        int end = Math.min(all.size(), start+per);

        for (int i = start, slot = 0; i < end; i++, slot++) {
            Reward r = all.get(i);
            ItemStack icon = r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone();

            List<String> lore = new ArrayList<>();
            double pct = service.weightPercent(crate, r) * 100.0;
            lore.add("<gray>Rarity:</gray> <white>" + r.rarity().name() + "</white>");
            lore.add("<gray>Chance:</gray> <white>" + String.format(Locale.US, "%.2f", pct) + "%</white>");
            if (!r.commands().isEmpty()) lore.add("<gray>Commands:</gray> <white>" + r.commands().size() + "</white>");

            ItemUtil.applyName(icon, "<yellow>" + (r.displayName() == null ? r.id() : r.displayName()) + "</yellow>");
            ItemUtil.applyLore(icon, lore);

            inv.setItem(slot, icon);
        }

        // controls: rarity filter bar
        int bar = 45;
        for (Reward.Rarity rr : Reward.Rarity.values()) {
            ItemStack it = new ItemStack(rr.icon());
            ItemUtil.applyName(it, "<white>Filter:</white> <yellow>" + rr.name() + "</yellow>");
            if (filter == rr) ItemUtil.glow(it, true);
            inv.setItem(bar++, it);
        }

        // prev/next
        if (start > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemUtil.applyName(prev, "<gray>Previous</gray>");
            inv.setItem(52, prev);
        }
        if (end < all.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemUtil.applyName(next, "<gray>Next</gray>");
            inv.setItem(53, next);
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 0.7f, 1.4f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var st = State.get(p);
        if (st == null) return;
        if (!e.getView().title().equals(ItemUtil.mm("<gold>Preview</gold> <gray>•</gray> <white>" + st.crateId + "</white>")) &&
                !e.getView().getTitle().contains("Preview")) {
            return;
        }

        e.setCancelled(true);
        int slot = e.getRawSlot();
        Crate crate = service.crate(st.crateId);
        if (crate == null) { p.closeInventory(); return; }

        // prev/next
        if (slot == 52) { open(p, crate, Math.max(1, st.page-1), st.filter); return; }
        if (slot == 53) { open(p, crate, st.page+1, st.filter); return; }

        // filter clicks on 45..(45+rarityCount-1)
        if (slot >= 45 && slot < 45 + Reward.Rarity.values().length) {
            Reward.Rarity rr = Reward.Rarity.values()[slot - 45];
            open(p, crate, 1, rr == st.filter ? null : rr);
        }
    }

    // simple per-player viewer state
    private static final class State {
        private static final Map<UUID, State> map = new HashMap<>();
        final String crateId;
        final int page;
        final Reward.Rarity filter;
        State(String id, int page, Reward.Rarity filter) { this.crateId = id; this.page = page; this.filter = filter; }
        static void set(Player p, State s) { map.put(p.getUniqueId(), s); }
        static State get(Player p) { return map.get(p.getUniqueId()); }
        static void clear(Player p) { map.remove(p.getUniqueId()); }
    }
}
