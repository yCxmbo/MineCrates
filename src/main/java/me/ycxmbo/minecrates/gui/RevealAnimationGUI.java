package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public final class RevealAnimationGUI {

    private RevealAnimationGUI() {}

    /**
     * Shows a center slot reveal with a few fake flickers, then stops on the chosen reward.
     *
     * @param p         Player
     * @param crate     Crate
     * @param flickers  number of random flickers (e.g., 12–24)
     * @param speed     ticks between flickers (e.g., 5–8)
     * @param onFinish  callback with chosen reward
     */
    public static void open(Player p, Crate crate, int flickers, int speed, Consumer<Reward> onFinish) {
        final Inventory inv = Bukkit.createInventory(p, 27, ItemUtil.mm("<gold>Opening</gold> <gray>•</gray> <white>" + crate.displayName() + "</white>"));
        p.openInventory(inv);

        List<Reward> pool = new ArrayList<>(crate.rewards());
        if (pool.isEmpty()) pool.add(dummyReward());

        Random rng = new Random();
        final int[] ticks = {0};
        final int total = Math.max(6, flickers);
        Bukkit.getScheduler().runTaskTimer(Bukkit.getPluginManager().getPlugin("MineCrates"), task -> {
            if (!p.isOnline() || p.getOpenInventory() == null || !p.getOpenInventory().getTopInventory().equals(inv)) {
                task.cancel();
                return;
            }
            ticks[0]++;

            Reward fake = pool.get(rng.nextInt(pool.size()));
            inv.setItem(13, iconFor(fake));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.8f);

            if (ticks[0] >= total) {
                task.cancel();
                // final chosen via crate picker (service will do the actual grant)
                Reward chosen = pool.get(rng.nextInt(pool.size()));
                inv.setItem(13, iconFor(chosen));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MineCrates"), () -> {
                    p.closeInventory();
                    try { onFinish.accept(chosen); } catch (Throwable ignored) {}
                }, 12L);
            }
        }, speed, speed);
    }

    private static Reward dummyReward() {
        return new Reward("nothing", 1.0, Reward.Rarity.COMMON, List.of(), List.of(), false, "Nothing");
    }

    private static ItemStack iconFor(Reward r) {
        ItemStack icon = r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone();
        ItemUtil.applyName(icon, "<yellow>" + (r.displayName() == null ? r.id() : r.displayName()) + "</yellow>");
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Rarity:</gray> <white>" + r.rarity().name() + "</white>");
        ItemUtil.applyLore(icon, lore);
        return icon;
    }
}
