package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class CrateEditGUI implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final MineCrates plugin;
    private final CrateService service;
    private final String crateId;
    private String keyId;
    private boolean requiresKey;
    private boolean costEnabled;
    private Crate.CostCurrency costCurrency;
    private double costAmount;
    private final Inventory inv;
    private String newKeyId;

    private static final int REWARD_START = 9;
    private static final int REWARD_END = 45; // exclusive

    public CrateEditGUI(MineCrates plugin, CrateService service, Crate crate) {
        this.plugin = plugin;
        this.service = service;
        this.crateId = crate.id();
        this.keyId = crate.key() == null ? "" : crate.key().id();
        this.requiresKey = crate.requiresKey();
        this.costEnabled = crate.costEnabled();
        this.costCurrency = crate.costCurrency();
        this.costAmount = crate.costAmount();
        this.inv = Bukkit.createInventory(this, 54, Component.text("Edit: " + crate.id()));
        build(crate);
    }

    private void build(Crate crate) {
        inv.clear();
        rebuildTop();
        int slot = REWARD_START;
        for (Reward r : crate.rewards()) {
            if (slot >= REWARD_END) break;
            ItemStack it = r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone();
            inv.setItem(slot++, it);
        }
    }

    private void rebuildTop() {
        ItemStack keyIt = new ItemStack(requiresKey ? Material.TRIPWIRE_HOOK : Material.BARRIER);
        ItemMeta km = keyIt.getItemMeta();
        if (requiresKey) km.displayName(MM.deserialize("<yellow>Key: " + keyId + "</yellow>"));
        else km.displayName(MM.deserialize("<red>No key</red>"));
        keyIt.setItemMeta(km);
        inv.setItem(0, keyIt);

        ItemStack costIt = new ItemStack(costEnabled ? Material.GOLD_INGOT : Material.BARRIER);
        ItemMeta cm = costIt.getItemMeta();
        if (costEnabled) cm.displayName(MM.deserialize("<yellow>Cost: " + costAmount + " " + costCurrency.name() + "</yellow>"));
        else cm.displayName(MM.deserialize("<red>No cost</red>"));
        costIt.setItemMeta(cm);
        inv.setItem(1, costIt);
    }

    public void open(Player p) { p.openInventory(inv); }
    @Override public Inventory getInventory() { return inv; }

    public void handleClick(InventoryClickEvent e) {
        int slot = e.getRawSlot();
        if (slot == 0) {
            e.setCancelled(true);
            if (e.isRightClick()) {
                requiresKey = !requiresKey;
            } else if (e.isLeftClick() && requiresKey) {
                List<String> ids = new ArrayList<>(service.keyIds());
                if (!ids.isEmpty()) {
                    int idx = ids.indexOf(keyId);
                    idx = (idx + 1) % ids.size();
                    keyId = ids.get(idx);
                }
            } else if (e.isShiftClick()) {
                newKeyId = "key_" + System.currentTimeMillis();
                keyId = newKeyId;
                requiresKey = true;
            }
            rebuildTop();
            return;
        }
        if (slot == 1) {
            e.setCancelled(true);
            if (e.isMiddleClick()) {
                Crate.CostCurrency[] vals = Crate.CostCurrency.values();
                costCurrency = vals[(costCurrency.ordinal() + 1) % vals.length];
                costEnabled = true;
            } else if (e.isLeftClick()) {
                costAmount += e.isShiftClick() ? 10 : 1;
                costEnabled = true;
            } else if (e.isRightClick()) {
                costAmount -= e.isShiftClick() ? 10 : 1;
                if (costAmount <= 0) { costAmount = 0; costEnabled = false; }
            }
            rebuildTop();
            return;
        }
        if (slot >= REWARD_START && slot < REWARD_END) {
            // allow normal item placement
            return;
        }
        e.setCancelled(true);
    }

    public void handleClose(InventoryCloseEvent e) {
        save((Player) e.getPlayer());
    }

    private void save(Player p) {
        try {
            File f = new File(plugin.getDataFolder(), "crates.yml");
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            String base = "crates." + crateId;
            y.set(base + ".requires-key", requiresKey);
            if (requiresKey) y.set(base + ".key", keyId); else y.set(base + ".key", null);
            y.set(base + ".cost.enabled", costEnabled);
            y.set(base + ".cost.currency", costCurrency.name());
            y.set(base + ".cost.amount", costAmount);
            y.set(base + ".rewards", null);
            int idx = 1;
            for (int s = REWARD_START; s < REWARD_END; s++) {
                ItemStack it = inv.getItem(s);
                if (it == null || it.getType() == Material.AIR) continue;
                String rPath = base + ".rewards.reward" + idx;
                y.set(rPath + ".weight", 1.0);
                y.set(rPath + ".rarity", "COMMON");
                y.set(rPath + ".items.i0.material", it.getType().name());
                y.set(rPath + ".items.i0.amount", it.getAmount());
                idx++;
            }
            y.save(f);

            if (newKeyId != null) {
                File kf = new File(plugin.getDataFolder(), "keys.yml");
                YamlConfiguration ky = YamlConfiguration.loadConfiguration(kf);
                ky.set("keys." + newKeyId + ".material", "TRIPWIRE_HOOK");
                ky.set("keys." + newKeyId + ".display", newKeyId);
                ky.save(kf);
            }

            service.reloadAllAsync();
            Messages.msg(p, "<green>Crate saved.</green>");
        } catch (Exception ex) {
            Messages.msg(p, "<red>Save failed:</red> " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
