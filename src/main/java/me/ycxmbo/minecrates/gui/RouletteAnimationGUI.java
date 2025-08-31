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
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

public final class RouletteAnimationGUI {

    private RouletteAnimationGUI() {}

    /**
     * Opens a 27-slot roulette strip. Spins several cycles, then lands on the chosen reward.
     *
     * @param p         Player
     * @param crate     Crate
     * @param cycles    total "moves" to advance the cursor (e.g., 32–48)
     * @param speedTicks ticks between steps (e.g., 2–4)
     * @param onFinish  callback with the final chosen reward
     */
    public static void open(Player p, Crate crate, int cycles, int speedTicks, Consumer<Reward> onFinish) {
        final Inventory inv = Bukkit.createInventory(p, 27, ItemUtil.mm("<gold>Opening</gold> <gray>•</gray> <white>" + crate.displayName() + "</white>"));
        // Create a strip of candidate items (randomized snapshot of all rewards)
        List<Reward> pool = new ArrayList<>(crate.rewards());
        if (pool.isEmpty()) pool.add(dummyReward());

        Random rng = new Random();
        // If pool smaller than 21, just repeat to fill
        while (pool.size() < 21) pool.add(pool.get(rng.nextInt(pool.size())));

        // Initial fill
        fillStrip(inv, pool, 0);

        p.openInventory(inv);

        final int[] step = {0};
        final int total = Math.max(8, cycles);
        final int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bukkit.getPluginManager().getPlugin("MineCrates"), () -> {
            if (!p.isOnline() || p.getOpenInventory() == null || !p.getOpenInventory().getTopInventory().equals(inv)) {
                Bukkit.getScheduler().cancelTask(step[0]);
                return;
            }
            step[0]++;

            // advance one
            fillStrip(inv, pool, step[0]);

            // tick sound
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);

            if (step[0] >= total) {
                Bukkit.getScheduler().cancelTask(step[0]);

                // Choose the middle slot (13) reward
                Reward chosen = visualToReward(inv.getItem(13), pool);
                if (chosen == null) chosen = crate.rewards().isEmpty() ? dummyReward() : crate.rewards().get(rng.nextInt(crate.rewards().size()));

                // landing sounds
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // small delay so player sees it
                final Reward fin = chosen;
                Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MineCrates"), () -> {
                    p.closeInventory();
                    try { onFinish.accept(fin); } catch (Throwable ignored) {}
                }, 10L);
            }
        }, speedTicks, speedTicks);
        // store the task id in step[0] (hack to cancel properly)
        step[0] = taskId;
    }

    private static Reward dummyReward() {
        // lightweight placeholder; not granted
        return new Reward("nothing", 1.0, Reward.Rarity.COMMON, List.of(), List.of(), false, "Nothing");
    }

    private static void fillStrip(Inventory inv, List<Reward> pool, int offset) {
        // Strip is slots 3..23 (21 slots), middle is 13
        for (int slot = 3; slot <= 23; slot++) {
            Reward r = pool.get((slot + offset) % pool.size());
            inv.setItem(slot, iconFor(r));
        }
        // Add glass borders
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemUtil.applyName(pane, "<gray>…</gray>");
        for (int i = 0; i < 27; i++) {
            if (i < 3 || i > 23 || i == 13) {
                inv.setItem(i, pane);
            }
        }
    }

    private static ItemStack iconFor(Reward r) {
        ItemStack icon = r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone();
        ItemUtil.applyName(icon, "<yellow>" + (r.displayName() == null ? r.id() : r.displayName()) + "</yellow>");
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Rarity:</gray> <white>" + r.rarity().name() + "</white>");
        // weight display will be added by Preview GUI, here keep it simple
        ItemUtil.applyLore(icon, lore);
        return icon;
    }

    private static Reward visualToReward(ItemStack it, List<Reward> pool) {
        if (it == null || it.getType() == Material.AIR) return null;
        String plain = ItemUtil.plain(it.getItemMeta() != null ? it.getItemMeta().displayName() : null);
        if (plain == null) return null;
        String name = plain.toLowerCase(Locale.ROOT);
        for (Reward r : pool) {
            String rn = (r.displayName() == null ? r.id() : r.displayName()).toLowerCase(Locale.ROOT);
            if (name.contains(rn)) return r;
        }
        return null;
    }
}
