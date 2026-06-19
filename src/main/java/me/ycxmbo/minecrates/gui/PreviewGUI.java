package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.config.ConfigManager;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * PreviewGUI displays rewards of a crate with pagination.
 * - Uses a dedicated InventoryHolder to reliably identify the menu (no title matching).
 * - All text is MiniMessage from messages.yml via ConfigManager.
 * - Clicks are cancelled; only navigation/close are actionable.
 */
public final class PreviewGUI implements Listener {

    private static PreviewGUI INSTANCE;

    private final MineCrates plugin;
    private final CrateService service;
    private final ConfigManager config;
    private final MiniMessage mm;

    private static final int ROWS = 6; // 54 slots
    private static final int SIZE = ROWS * 9;
    private static final int CONTENT_SLOTS = 45; // 0..44

    public PreviewGUI(MineCrates plugin, CrateService service, ConfigManager config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.service = Objects.requireNonNull(service, "service");
        this.config = Objects.requireNonNull(config, "config");
        this.mm = config.miniMessage();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        INSTANCE = this; // bridge for legacy static listener/class references
    }

    public void open(Player player, Crate crate) {
        open(player, crate, 1);
    }

    public void open(Player player, Crate crate, int page) {
        if (player == null || crate == null) return;

        int totalRewards = crate.rewards().size();
        int perPage = Math.max(1, plugin.getConfig().getInt("gui.preview.items-per-page", CONTENT_SLOTS));
        int pages = Math.max(1, (int) Math.ceil(totalRewards / (double) perPage));
        int safePage = Math.min(Math.max(1, page), pages);

        Component title = mm.deserialize(
                config.msg("preview.title"),
                Placeholder.parsed("crate_id", crate.id()),
                Placeholder.parsed("crate_display", crate.displayName()),
                Placeholder.parsed("page", String.valueOf(safePage)),
                Placeholder.parsed("pages", String.valueOf(pages))
        );

        Inventory inv = Bukkit.createInventory(new PreviewHolder(crate.id(), page), SIZE, title);

        // Page slice
        List<Reward> rewards = new ArrayList<>(crate.rewards());
        int start = Math.max(0, (safePage - 1) * perPage);
        int endExclusive = Math.min(rewards.size(), start + perPage);

        // Body items
        int slot = 0;
        for (int i = start; i < endExclusive; i++) {
            Reward r = rewards.get(i);
            ItemStack icon = r.displayItem() != null
                    ? r.displayItem()
                    : (r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone());

            List<String> lore = new ArrayList<>();
            // The weight is the player's chance to receive this reward.
            double pct = service.weightPercent(crate, r) * 100.0;
            lore.add(config.msg("preview.chance-line", Map.of("percent", String.format(Locale.US, "%.2f", pct))));

            // Honor a display name set directly on the display item; otherwise build one
            // from the reward display, falling back to a humanized material name.
            boolean iconHasName = icon.hasItemMeta() && icon.getItemMeta().hasDisplayName();
            if (!iconHasName) {
                String label = r.displayName();
                if (label == null || label.isEmpty() || label.equalsIgnoreCase(r.id())) {
                    label = ItemUtil.prettyMaterialName(icon.getType());
                }
                ItemUtil.applyName(icon, config.msg("preview.reward-name", Map.of(
                        "reward_id", r.id(),
                        "reward_display", label
                )));
            }
            boolean showDetails = plugin.getConfig().getBoolean("gui.preview.show-details", false);
            if (showDetails) {
                // include items/commands list (trimmed)
                int itemLines = 0;
                for (ItemStack it : r.items()) {
                    if (it == null || it.getType().isAir()) continue;
                    if (itemLines++ >= 5) break;
                    lore.add("<gray>-</gray> <white>" + it.getAmount() + "x " + it.getType() + "</white>");
                }
                // Intentionally hide reward commands from preview
            }
            ItemUtil.applyLore(icon, lore);
            inv.setItem(slot++, icon);
        }

        // Empty state
        if (slot == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemUtil.applyName(empty, config.msg("preview.empty.title"));
            ItemUtil.applyLore(empty, List.of(config.msg("preview.empty.lore")));
            inv.setItem(22, empty);
        }

        // Navigation slots from config
        int closeSlot = Math.max(0, Math.min(SIZE-1, plugin.getConfig().getInt("gui.preview.close-slot", 49)));
        int prevSlot = Math.max(0, Math.min(SIZE-1, plugin.getConfig().getInt("gui.preview.prev-slot", 52)));
        int nextSlot = Math.max(0, Math.min(SIZE-1, plugin.getConfig().getInt("gui.preview.next-slot", 53)));

        // Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemUtil.applyName(close, config.msg("preview.close"));
        inv.setItem(closeSlot, close);

        // Prev/Next (52/53)
        if (start > 0 && prevSlot < SIZE) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemUtil.applyName(prev, config.msg("preview.prev"));
            inv.setItem(prevSlot, prev);
        }
        if (endExclusive < rewards.size() && nextSlot < SIZE) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemUtil.applyName(next, config.msg("preview.next"));
            inv.setItem(nextSlot, next);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.7f, 1.4f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof PreviewHolder holder)) return;

        e.setCancelled(true);
        Crate crate = service.crate(holder.crateId);
        if (crate == null) {
            player.closeInventory();
            player.sendMessage(mm.deserialize(config.msg("preview.crate-missing")));
            return;
        }

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return; // only our GUI

        // Navigation
        int closeSlot = Math.max(0, Math.min(SIZE-1, plugin.getConfig().getInt("gui.preview.close-slot", 49)));
        int prevSlot = Math.max(0, Math.min(SIZE-1, plugin.getConfig().getInt("gui.preview.prev-slot", 52)));
        int nextSlot = Math.max(0, Math.min(SIZE-1, plugin.getConfig().getInt("gui.preview.next-slot", 53)));

        if (slot == closeSlot) { // close
            player.closeInventory();
            return;
        }
        if (slot == prevSlot) { // prev
            int delta = e.isShiftClick() ? 5 : 1;
            open(player, crate, Math.max(1, holder.page - delta));
            return;
        }
        if (slot == nextSlot) { // next
            int delta = e.isShiftClick() ? 5 : 1;
            open(player, crate, holder.page + delta);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof PreviewHolder) {
            // no persistent state to clear; method reserved for future extensions
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Nothing persistent to clear; reserved for future session tracking
    }

    // Static bridges to satisfy old code paths
    public static boolean isPreview(org.bukkit.inventory.Inventory inv) {
        return inv != null && inv.getHolder() instanceof PreviewHolder;
    }

    public static void handleClick(Player player, int slot) {
        if (INSTANCE == null || player == null) return;
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof PreviewHolder holder)) return;
        Crate crate = INSTANCE.service.crate(holder.crateId);
        if (crate == null) {
            player.closeInventory();
            return;
        }
        if (slot == 49) { player.closeInventory(); return; }
        if (slot == 52) { INSTANCE.open(player, crate, Math.max(1, holder.page - 1)); return; }
        if (slot == 53) { INSTANCE.open(player, crate, holder.page + 1); }
    }

    public static void open(Player player, Object ignored, Crate crate) {
        if (INSTANCE != null) INSTANCE.open(player, crate);
    }

    private record PreviewHolder(String crateId, int page) implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
}
