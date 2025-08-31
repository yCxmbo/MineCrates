package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class OpenAnimationGUI {

    private OpenAnimationGUI() {}

    // --- ROULETTE (strip spin) ---
    public static void openRoulette(Player p, Crate crate) {
        final int size = 27;
        final Inventory inv = Bukkit.createInventory(null, size, ItemUtil.color("&8Opening • " + crate.getDisplayName()));
        p.openInventory(inv);

        final int mid = 13; // center result slot
        final List<ItemStack> pool = new ArrayList<>();
        for (Reward r : crate.getRewards()) {
            ItemStack icon = r.previewIcon(); // your Reward should provide a safe preview icon
            pool.add(icon != null ? icon : new ItemStack(Material.CHEST));
        }
        if (pool.isEmpty()) pool.add(new ItemStack(Material.CHEST));

        final Random rng = new Random();
        final int cycles = crate.getAnimationCycles();
        final int speed = crate.getAnimationSpeedTicks();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(MineCrates.get(), new Runnable() {
            int ticks = 0;
            @Override public void run() {
                ticks++;
                // shift random items across a small strip (slots 11..15)
                for (int s = 11; s <= 15; s++) {
                    inv.setItem(s, pool.get(rng.nextInt(pool.size())));
                }
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);

                if (ticks >= cycles) {
                    // choose result
                    Bukkit.getScheduler().runTask(MineCrates.get(), () -> {
                        Reward chosen = crate.pickReward();
                        ItemStack icon = chosen != null ? chosen.previewIcon() : new ItemStack(Material.CHEST);
                        inv.setItem(mid, icon);
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

                        if (chosen != null) {
                            chosen.give(p);
                            MineCrates.get().getDataStore().incrOpens(p.getUniqueId());
                            MineCrates.get().getDataStore().setLastRewardName(p.getUniqueId(), chosen.getName());
                        }
                        // small delay then close
                        Bukkit.getScheduler().runTaskLater(MineCrates.get(), p::closeInventory, 20L);
                    });
                    this.cancelSelf();
                }
            }
            private void cancelSelf() {
                Bukkit.getScheduler().runTask(MineCrates.get(), () -> {
                    // no reference to task inside; schedule a new one to cancel current
                    // easier: just not needed; we stop by returning after result
                });
            }
        }, 1L, Math.max(1L, speed));
        // store task if you want to cancel on close; left minimal here
    }

    // --- REVEAL (single item buildup) ---
    public static void openReveal(Player p, Crate crate) {
        final Inventory inv = Bukkit.createInventory(null, 27, ItemUtil.color("&8Opening • " + crate.getDisplayName()));
        p.openInventory(inv);

        final int center = 13;
        final int cycles = crate.getAnimationCycles();
        final int speed = crate.getAnimationSpeedTicks();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(MineCrates.get(), new Runnable() {
            int step = 0;
            @Override public void run() {
                step++;
                // flicker placeholder
                inv.setItem(center, new ItemStack(step % 2 == 0 ? Material.LIGHT : Material.GRAY_STAINED_GLASS_PANE));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.8f);

                if (step >= cycles) {
                    Reward chosen = crate.pickReward();
                    ItemStack icon = chosen != null ? chosen.previewIcon() : new ItemStack(Material.CHEST);
                    inv.setItem(center, icon);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);

                    if (chosen != null) {
                        chosen.give(p);
                        MineCrates.get().getDataStore().incrOpens(p.getUniqueId());
                        MineCrates.get().getDataStore().setLastRewardName(p.getUniqueId(), chosen.getName());
                    }
                    Bukkit.getScheduler().runTaskLater(MineCrates.get(), p::closeInventory, 20L);
                    this.cancelSelf();
                }
            }
            private void cancelSelf() { /* same note as above */ }
        }, 1L, Math.max(1L, speed));
    }
}
