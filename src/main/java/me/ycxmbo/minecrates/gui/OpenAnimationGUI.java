package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Opening animations for crates.
 * Supported types: ROULETTE, REVEAL, CASCADE (select via config animation.type)
 */
public final class OpenAnimationGUI {

    private OpenAnimationGUI() {}

    public static void play(Player player, Crate crate, Reward reward, Consumer<Reward> finish) {
        me.ycxmbo.minecrates.util.SoundUtil.play(player, "open");
        switch (crate.animationType()) {
            case REVEAL -> playReveal(player, crate, reward, finish);
            case CASCADE -> playCascade(player, crate, reward, finish);
            case ROULETTE -> playRoulette(player, crate, reward, finish);
            default -> playRoulette(player, crate, reward, finish);
        }
    }

    private static List<ItemStack> buildIconPool(Crate crate) {
        List<ItemStack> pool = new ArrayList<>();
        for (Reward r : crate.rewards()) {
            ItemStack icon = r.displayItem() != null
                    ? r.displayItem()
                    : (r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone());
            String label = r.displayName();
            if (label == null || label.isEmpty() || label.equalsIgnoreCase(r.id())) {
                label = ItemUtil.prettyMaterialName(icon.getType());
            }
            ItemUtil.applyName(icon, "<yellow>" + label + "</yellow>");
            pool.add(icon);
        }
        if (pool.isEmpty()) pool.add(new ItemStack(Material.CHEST));
        return pool;
    }

    private static net.kyori.adventure.text.Component title(Crate crate) {
        String mmTitle = "<gray>Opening</gray> <dark_gray>-</dark_gray> " + crate.displayName();
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(mmTitle);
    }

    private static ItemStack prizeMarker() {
        var cfg = MineCrates.get().getConfig();
        Material mat = Material.matchMaterial(cfg.getString("animation.markers.prize.material", "REDSTONE_TORCH"));
        if (mat == null) mat = Material.REDSTONE_TORCH;
        ItemStack it = new ItemStack(mat);
        me.ycxmbo.minecrates.util.ItemUtil.applyName(it, cfg.getString("animation.markers.prize.name", "<gold>Prize</gold>"));
        return it;
    }

    private static ItemStack fillerItem() {
        var cfg = MineCrates.get().getConfig();
        Material mat = Material.matchMaterial(cfg.getString("animation.markers.filler.material", "BLACK_STAINED_GLASS_PANE"));
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack it = new ItemStack(mat);
        me.ycxmbo.minecrates.util.ItemUtil.applyName(it, cfg.getString("animation.markers.filler.name", "<gray> </gray>"));
        return it;
    }

    private static void applyMarkersAndFill(Inventory inv) {
        ItemStack filler = fillerItem();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        ItemStack prize = prizeMarker();
        // Above and below center (13)
        if (inv.getSize() >= 23) {
            inv.setItem(4, prize);
            inv.setItem(22, prize);
        }
    }

    // ROULETTE strip animation
    private static void playRoulette(Player player, Crate crate, Reward reward, Consumer<Reward> finish) {
        final Inventory inv = Bukkit.createInventory(player, 27, title(crate));
        player.openInventory(inv);

        applyMarkersAndFill(inv);
        List<ItemStack> pool = buildIconPool(crate);
        final int cycles = crate.rouletteCycles();
        final int speed = Math.max(1, crate.rouletteSpeedTicks());
        final Random rng = new Random();

        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                step++;
                for (int slot = 11; slot <= 15; slot++) inv.setItem(slot, pool.get(rng.nextInt(pool.size())));
                me.ycxmbo.minecrates.util.SoundUtil.play(player, "reveal-tick");
                if (step >= cycles) { endWithReward(inv, player, crate, reward, finish); cancel(); }
            }
        }.runTaskTimer(MineCrates.get(), 1L, speed);
    }

    // REVEAL center flicker
    private static void playReveal(Player player, Crate crate, Reward reward, Consumer<Reward> finish) {
        final Inventory inv = Bukkit.createInventory(player, 27, title(crate));
        player.openInventory(inv);

        applyMarkersAndFill(inv);
        List<ItemStack> pool = buildIconPool(crate);
        final int flickers = crate.revealFlickers();
        final int speed = Math.max(1, crate.revealSpeedTicks());
        final Random rng = new Random();

        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                step++;
                inv.setItem(13, pool.get(rng.nextInt(pool.size())));
                me.ycxmbo.minecrates.util.SoundUtil.play(player, "reveal-tick");
                if (step >= flickers) { endWithReward(inv, player, crate, reward, finish); cancel(); }
            }
        }.runTaskTimer(MineCrates.get(), 1L, speed);
    }

    // CASCADE fill left-to-right then reveal center
    private static void playCascade(Player player, Crate crate, Reward reward, Consumer<Reward> finish) {
        final Inventory inv = Bukkit.createInventory(player, 27, title(crate));
        player.openInventory(inv);

        applyMarkersAndFill(inv);
        List<ItemStack> pool = buildIconPool(crate);
        final int speed = Math.max(1, crate.cascadeSpeedTicks());
        final int total = inv.getSize();
        final Random rng = new Random();

        new BukkitRunnable() {
            int slot = 0;
            @Override public void run() {
                if (slot < total) {
                    if (slot == 4 || slot == 22) { slot++; return; }
                    inv.setItem(slot, pool.get(rng.nextInt(pool.size())));
                    slot++;
                    me.ycxmbo.minecrates.util.SoundUtil.play(player, "reveal-tick");
                } else { endWithReward(inv, player, crate, reward, finish); cancel(); }
            }
        }.runTaskTimer(MineCrates.get(), 1L, speed);
    }

    private static void endWithReward(Inventory inv, Player player, Crate crate, Reward reward, Consumer<Reward> finish) {
        ItemStack icon = reward.displayItem() != null
                ? reward.displayItem()
                : (reward.items().isEmpty() ? new ItemStack(Material.CHEST) : reward.items().get(0).clone());
        // Honor a display name already set on the display item; otherwise derive one.
        boolean iconHasName = icon.hasItemMeta() && icon.getItemMeta().hasDisplayName();
        if (!iconHasName) {
            String label = reward.displayName();
            if (label == null || label.isEmpty() || label.equalsIgnoreCase(reward.id())) label = ItemUtil.prettyMaterialName(icon.getType());
            ItemUtil.applyName(icon, "<yellow>" + label + "</yellow>");
        }
        inv.setItem(13, icon);
        me.ycxmbo.minecrates.util.SoundUtil.play(player, "reward");
        Bukkit.getScheduler().runTaskLater(MineCrates.get(), () -> {
            player.closeInventory();
            finish.accept(reward);
        }, Math.max(0L, crate.closeDelayTicks()));
    }
}
