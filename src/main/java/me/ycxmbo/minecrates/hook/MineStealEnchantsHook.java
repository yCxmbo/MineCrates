package me.ycxmbo.minecrates.hook;

import me.ycxmbo.minecrates.MineCrates;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Optional integration with the MineStealEnchants plugin.
 *
 * <p>MineStealEnchants is not published to a public Maven repository, so we talk to its
 * public API ({@code com.minestealenchants.api.CustomEnchantAPI}) purely via reflection.
 * This keeps the MineCrates build self-contained and lets the plugin run fine when
 * MineStealEnchants is absent.</p>
 */
public final class MineStealEnchantsHook {

    private final boolean present;
    private final Method applyEnchant; // CustomEnchantAPI#applyEnchant(ItemStack, String, int) -> boolean

    public MineStealEnchantsHook(MineCrates plugin) {
        boolean p = false;
        Method m = null;
        try {
            if (Bukkit.getPluginManager().getPlugin("MineStealEnchants") != null) {
                Class<?> api = Class.forName("com.minestealenchants.api.CustomEnchantAPI");
                m = api.getMethod("applyEnchant", ItemStack.class, String.class, int.class);
                p = true;
            }
        } catch (Throwable ignored) {
            // API missing or incompatible – treat as absent.
            p = false;
            m = null;
        }
        this.present = p;
        this.applyEnchant = m;
        if (!present) {
            plugin.getLogger().info("[MineCrates] MineStealEnchants not present – custom enchant rewards disabled.");
        } else {
            plugin.getLogger().info("[MineCrates] MineStealEnchants hooked – custom enchant rewards enabled.");
        }
    }

    public boolean present() { return present && applyEnchant != null; }

    /**
     * Apply a MineStealEnchants custom enchant to an item.
     *
     * @return {@code true} on success; {@code false} if the hook is inactive, the id is
     *         unknown, the level is out of range, or the material isn't applicable.
     */
    public boolean applyEnchant(ItemStack item, String id, int level) {
        if (!present() || item == null || id == null) return false;
        try {
            Object result = applyEnchant.invoke(null, item, id, level);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }
}
