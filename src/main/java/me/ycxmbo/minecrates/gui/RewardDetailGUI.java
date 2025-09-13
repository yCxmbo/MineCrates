package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-reward editor for commands/money/xp/message/announce and item list.
 * Opens from CrateEditGUI reward tile (Right click, no shift).
 */
public final class RewardDetailGUI implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final CrateEditGUI parent;
    private final int index;
    private final Inventory inv;

    private static final int SIZE = 54;
    private static final int ITEMS_START = 27; // 27..44 (18 slots)
    private static final int ITEMS_END = 45;   // exclusive
    private boolean showCommands = false;

    RewardDetailGUI(CrateEditGUI parent, int index) {
        this.parent = parent;
        this.index = index;
        this.inv = Bukkit.createInventory(this, SIZE, Component.text("Reward Details"));
        rebuild();
    }

    public void open(Player p) { p.openInventory(inv); }
    @Override public Inventory getInventory() { return inv; }
    public CrateEditGUI.EditedReward target() { return er(); }

    private CrateEditGUI.EditedReward er() { return parent == null ? null : parent.rewardAt(index); }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MM.deserialize(name));
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String s : lore) lines.add(MM.deserialize(s));
            meta.lore(lines);
        }
        it.setItemMeta(meta);
        return it;
    }

    private void rebuild() {
        inv.clear();
        CrateEditGUI.EditedReward er = er();
        if (er == null) return;

        // Header buttons
        inv.setItem(10, item(er.announce ? Material.LIME_DYE : Material.GRAY_DYE,
                "<yellow>Announce</yellow>:", List.of("<white>" + (er.announce ? "enabled" : "disabled") + "</white>", "<gray>Left:</gray> toggle")));

        inv.setItem(12, item(Material.PAPER, "<yellow>Message</yellow>", List.of(
                "<gray>Left:</gray> edit via chat",
                "<gray>Right:</gray> clear",
                (er.message == null || er.message.isBlank()) ? "<gray>Current:</gray> <dark_gray>(none)</dark_gray>" : "<gray>Current:</gray> " + er.message
        )));

        inv.setItem(14, item(Material.COMMAND_BLOCK, "<yellow>Commands</yellow>", List.of(
                "<gray>Left:</gray> edit in book/chat",
                "<gray>Middle:</gray> view/remove entries",
                "<gray>Right:</gray> clear",
                "<gray>Count:</gray> <white>" + er.commands.size() + "</white>"
        )));

        inv.setItem(20, item(Material.GOLD_INGOT, "<yellow>Money</yellow>", List.of(
                "<gray>Left/Right:</gray> +/- 1",
                "<gray>Shift:</gray> x10",
                "<gray>Middle:</gray> clear",
                "<gray>Current:</gray> <white>" + String.format(java.util.Locale.US, "%.2f", er.money) + "</white>"
        )));

        inv.setItem(22, item(Material.EXPERIENCE_BOTTLE, "<yellow>XP Levels</yellow>", List.of(
                "<gray>Left/Right:</gray> +/- 1",
                "<gray>Shift:</gray> x10",
                "<gray>Middle:</gray> clear",
                "<gray>Current:</gray> <white>" + er.xpLevels + "</white>"
        )));

        // Items/Commands area
        int slot = ITEMS_START;
        if (!showCommands) {
            for (int i = 0; i < er.items.size() && slot < ITEMS_END; i++) {
                ItemStack it = er.items.get(i).clone();
                ItemMeta meta = it.getItemMeta();
                List<Component> lore = new ArrayList<>();
                lore.add(MM.deserialize("<gray>Slot:</gray> <white>" + i + "</white>"));
                lore.add(MM.deserialize("<gray>Drop:</gray> <white>remove item</white>"));
                lore.add(MM.deserialize("<gray>Cursor+Left:</gray> <white>replace here</white>"));
                if (meta != null) meta.lore(lore);
                it.setItemMeta(meta);
                inv.setItem(slot++, it);
            }
            while (slot < ITEMS_END) {
                inv.setItem(slot++, item(Material.GRAY_STAINED_GLASS_PANE, "<gray>Empty</gray>", List.of("<gray>Cursor+Left:</gray> <white>add here</white>", "<gray>Shift+Drop:</gray> <white>clear all</white>")));
            }
        } else {
            // Render commands as papers
            if (er.commands.isEmpty()) {
                inv.setItem(ITEMS_START, item(Material.BARRIER, "<gray>No commands</gray>", List.of("<gray>Left:</gray> use editor to add")));
            } else {
                for (int i = 0; i < er.commands.size() && slot < ITEMS_END; i++) {
                    String cmd = er.commands.get(i);
                    ItemStack paper = new ItemStack(Material.PAPER);
                    ItemMeta pm = paper.getItemMeta();
                    pm.displayName(MM.deserialize("<white>#" + (i+1) + ":</white> <yellow>" + cmd + "</yellow>"));
                    pm.lore(List.of(
                            MM.deserialize("<gray>Drop:</gray> <white>remove</white>")
                    ));
                    paper.setItemMeta(pm);
                    inv.setItem(slot++, paper);
                }
            }
            while (slot < ITEMS_END) inv.setItem(slot++, null);
        }

        // Footer
        inv.setItem(49, item(Material.ARROW, "<red>Back</red>", List.of("<gray>Click:</gray> return")));
        inv.setItem(53, item(Material.EMERALD_BLOCK, "<green>Save & Back</green>", List.of("<gray>Click:</gray> return")));
    }

    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true);
        CrateEditGUI.EditedReward er = er();
        if (er == null) return;
        int slot = e.getRawSlot();
        if (slot == 10) { // announce toggle
            if (e.isLeftClick()) er.announce = !er.announce;
            rebuild();
            return;
        }
        if (slot == 12) { // message edit
            if (e.isLeftClick()) {
                boolean ok = BookEditManager.startMessage((Player)e.getWhoClicked(), this, er.message);
                if (!ok) startMessageEdit((Player)e.getWhoClicked());
            }
            else if (e.isRightClick()) { er.message = ""; rebuild(); }
            return;
        }
        if (slot == 14) { // commands edit
            if (e.getClick() == ClickType.MIDDLE) {
                showCommands = !showCommands;
                rebuild();
            } else if (e.isLeftClick()) {
                boolean ok = BookEditManager.startCommands((Player)e.getWhoClicked(), this, er.commands);
                if (!ok) startCommandsEdit((Player)e.getWhoClicked());
            } else if (e.isRightClick()) { er.commands.clear(); rebuild(); }
            return;
        }
        if (slot == 20) { // money
            if (e.getClick() == ClickType.MIDDLE) er.money = 0D;
            else if (e.isLeftClick()) er.money += e.isShiftClick() ? 10D : 1D;
            else if (e.isRightClick()) er.money = Math.max(0D, er.money - (e.isShiftClick() ? 10D : 1D));
            rebuild();
            return;
        }
        if (slot == 22) { // xp levels
            if (e.getClick() == ClickType.MIDDLE) er.xpLevels = 0;
            else if (e.isLeftClick()) er.xpLevels += e.isShiftClick() ? 10 : 1;
            else if (e.isRightClick()) er.xpLevels = Math.max(0, er.xpLevels - (e.isShiftClick() ? 10 : 1));
            rebuild();
            return;
        }
        if (slot >= ITEMS_START && slot < ITEMS_END) {
            int idx = slot - ITEMS_START;
            if (!showCommands) {
                if (e.getClick() == ClickType.DROP) {
                    if (idx < er.items.size()) er.items.remove(idx);
                    rebuild();
                    return;
                }
                if (e.getClick() == ClickType.CONTROL_DROP || e.isShiftClick() && e.getClick() == ClickType.DROP) {
                    er.items.clear();
                    rebuild();
                    return;
                }
                ItemStack cursor = e.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    ItemStack clone = cursor.clone();
                    if (idx < er.items.size()) er.items.set(idx, clone);
                    else er.items.add(clone);
                    rebuild();
                }
            } else {
                // Commands viewer: Drop removes this command
                if (e.getClick() == ClickType.DROP) {
                    if (idx < er.commands.size()) er.commands.remove(idx);
                    rebuild();
                }
            }
            return;
        }
        if (slot == 49 || slot == 53) {
            // Back to parent
            parent.refreshRewards();
            ((Player)e.getWhoClicked()).openInventory(parent.getInventory());
        }
    }

    public void handleClose(InventoryCloseEvent e) {
        // No auto-save here; parent Save does disk persistence
    }

    // --- Chat editing support ---
    private record MsgSession(RewardDetailGUI gui, boolean commands) {}
    private static final Map<UUID, MsgSession> SESS = new ConcurrentHashMap<>();

    private void startMessageEdit(Player p) {
        SESS.put(p.getUniqueId(), new MsgSession(this, false));
        Messages.msg(p, "<gray>Type the reward message (MiniMessage allowed). '-' to clear. 'cancel' to abort.</gray>");
        p.closeInventory();
    }

    private void startCommandsEdit(Player p) {
        SESS.put(p.getUniqueId(), new MsgSession(this, true));
        Messages.msg(p, "<gray>Enter commands, one per message. Use '<player>' placeholder. Type 'done' to finish, 'clear' to empty, or 'cancel'.</gray>");
        p.closeInventory();
    }

    public static boolean handleChatInput(Player p, String msg) {
        MsgSession s = SESS.get(p.getUniqueId());
        if (s == null) return false;
        RewardDetailGUI gui = s.gui();
        if (gui == null) { SESS.remove(p.getUniqueId()); return true; }
        CrateEditGUI.EditedReward er = gui.er();
        if (er == null) { SESS.remove(p.getUniqueId()); return true; }

        if (!s.commands()) {
            // message editing
            if (msg.equalsIgnoreCase("cancel")) {
                Messages.msg(p, "<gray>Cancelled.</gray>");
            } else if (msg.equalsIgnoreCase("-") || msg.equalsIgnoreCase("clear")) {
                er.message = "";
                Messages.msg(p, "<yellow>Cleared</yellow> message.");
            } else {
                er.message = msg;
                Messages.msg(p, "<green>Set message.</green>");
            }
            SESS.remove(p.getUniqueId());
            // Reopen GUI next tick
            Bukkit.getScheduler().runTask(MineCrates.get(), () -> gui.open(p));
            return true;
        } else {
            // commands editor mode (multi-line until 'done')
            if (msg.equalsIgnoreCase("cancel")) {
                SESS.remove(p.getUniqueId());
                Messages.msg(p, "<gray>Cancelled.</gray>");
                Bukkit.getScheduler().runTask(MineCrates.get(), () -> gui.open(p));
                return true;
            }
            if (msg.equalsIgnoreCase("clear")) {
                er.commands.clear();
                Messages.msg(p, "<yellow>Cleared</yellow> commands.");
                return true;
            }
            if (msg.equalsIgnoreCase("done")) {
                SESS.remove(p.getUniqueId());
                Messages.msg(p, "<green>Commands saved.</green>");
                Bukkit.getScheduler().runTask(MineCrates.get(), () -> gui.open(p));
                return true;
            }
            // Add line as a command
            er.commands.add(msg);
            Messages.msg(p, "<gray>Added:</gray> <white>" + msg + "</white>");
            return true;
        }
    }
}
