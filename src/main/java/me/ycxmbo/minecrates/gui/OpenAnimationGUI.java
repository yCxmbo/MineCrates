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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Simple roulette style animation used when opening a crate.
 * A predetermined reward is shown after a short spin and then the
 * callback is invoked so the service can grant the reward and
 * update statistics.
 */
public final class OpenAnimationGUI {

    private OpenAnimationGUI() {
    }

    /**
     * Plays a small roulette animation. Random reward icons flicker in the
     * middle strip of the inventory before landing on the provided reward.
     *
     * @param player  viewer
     * @param crate   crate being opened
     * @param reward  reward that should be displayed at the end
     * @param finish  callback executed once animation has finished
     */
    public static void play(Player player, Crate crate, Reward reward, Consumer<Reward> finish) {
        final Inventory inv = Bukkit.createInventory(player, 27, "Opening • " + crate.displayName());
        player.openInventory(inv);

        // Build a pool of icons to cycle through during the spin
        List<ItemStack> pool = new ArrayList<>();
        for (Reward r : crate.rewards()) {
            ItemStack icon = r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone();
            ItemUtil.applyName(icon, "<yellow>" + r.id() + "</yellow>");
            pool.add(icon);
        }
        if (pool.isEmpty()) {
            pool.add(new ItemStack(Material.CHEST));
        }

        final int cycles = 40; // ticks the strip will spin for
        final int speed = 2;    // ticks between updates
        final Random rng = new Random();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                step++;

                // Fill strip slots with random icons
                for (int slot = 11; slot <= 15; slot++) {
                    inv.setItem(slot, pool.get(rng.nextInt(pool.size())));
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.6f);

                if (step >= cycles) {
                    cancel();

                    // Display the final reward in the centre slot
                    ItemStack icon = reward.items().isEmpty() ? new ItemStack(Material.CHEST)
                            : reward.items().get(0).clone();
                    ItemUtil.applyName(icon, "<yellow>" + reward.id() + "</yellow>");
                    inv.setItem(13, icon);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                    // Give players a moment to view the reward then close
                    Bukkit.getScheduler().runTaskLater(MineCrates.get(), () -> {
                        player.closeInventory();
                        finish.accept(reward);
                    }, 20L);
                }
            }
        }.runTaskTimer(MineCrates.get(), 1L, speed);
    }
}

