package me.ycxmbo.minecrates.util;

import me.ycxmbo.minecrates.MineCrates;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final NamespacedKey KEY_TAG = NamespacedKey.fromString("minecrates:key");

    public static ItemStack itemFromSection(ConfigurationSection sec) {
        if (sec == null) return null;
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        int amt = Math.max(1, sec.getInt("amount", 1));
        ItemStack it = new ItemStack(mat == null ? Material.STONE : mat, amt);
        applyName(it, sec.getString("name", null));
        if (sec.isList("lore")) applyLore(it, sec.getStringList("lore"));
        if (sec.isConfigurationSection("enchants")) applyEnchants(it, sec.getConfigurationSection("enchants"));
        if (sec.isConfigurationSection("mse-enchants")) applyMseEnchants(it, sec.getConfigurationSection("mse-enchants"));
        return it;
    }

    /**
     * Apply vanilla Bukkit enchantments from an {@code enchants:} section (map of name → level).
     * Names use modern Minecraft keys (e.g. {@code sharpness}, {@code protection},
     * {@code unbreaking}); legacy Bukkit names (e.g. {@code DAMAGE_ALL}) are also accepted.
     * Levels above the vanilla cap are honoured via {@link ItemStack#addUnsafeEnchantment}.
     */
    public static void applyEnchants(ItemStack it, ConfigurationSection sec) {
        if (it == null || sec == null) return;
        boolean book = it.getType() == Material.ENCHANTED_BOOK;
        EnchantmentStorageMeta bookMeta = null;
        if (book) {
            ItemMeta m = it.getItemMeta();
            if (m instanceof EnchantmentStorageMeta esm) bookMeta = esm;
        }
        for (String name : sec.getKeys(false)) {
            int level = Math.max(1, sec.getInt(name, 1));
            Enchantment ench = resolveEnchant(name);
            if (ench == null) {
                MineCrates.get().getLogger().warning("[MineCrates] Unknown enchantment '" + name + "' – skipped.");
                continue;
            }
            if (bookMeta != null) {
                bookMeta.addStoredEnchant(ench, level, true);
            } else {
                it.addUnsafeEnchantment(ench, level);
            }
        }
        if (bookMeta != null) it.setItemMeta(bookMeta);
    }

    private static Enchantment resolveEnchant(String name) {
        if (name == null || name.isEmpty()) return null;
        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        if (ench == null) {
            try {
                ench = Enchantment.getByName(name.toUpperCase(Locale.ROOT));
            } catch (Throwable ignored) {}
        }
        return ench;
    }

    /**
     * Apply MineStealEnchants custom enchantments from an {@code mse-enchants:} section
     * (map of id → level). Requires the MineStealEnchants plugin; entries are skipped with a
     * warning when the hook is absent or the enchant can't be applied to the item.
     */
    public static void applyMseEnchants(ItemStack it, ConfigurationSection sec) {
        if (it == null || sec == null) return;
        MineCrates plugin = MineCrates.get();
        if (plugin == null) return; // e.g. unit tests with no running server
        if (plugin.mse() == null || !plugin.mse().present()) {
            plugin.getLogger().warning("[MineCrates] mse-enchants configured but MineStealEnchants is not available – skipped.");
            return;
        }
        for (String id : sec.getKeys(false)) {
            int level = Math.max(1, sec.getInt(id, 1));
            if (!plugin.mse().applyEnchant(it, id.toLowerCase(Locale.ROOT), level)) {
                plugin.getLogger().warning("[MineCrates] Could not apply MineStealEnchants enchant '" + id
                        + "' (level " + level + ") to " + it.getType() + " – check the id (/mse list) and material.");
            }
        }
    }

    public static void applyName(ItemStack it, String name) {
        if (it == null || name == null || name.isEmpty()) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        var comp = (name.indexOf('&') >= 0 || name.indexOf('§') >= 0)
                ? LEGACY_AMP.deserialize(name)
                : MM.deserialize(name);
        meta.displayName(comp.decoration(TextDecoration.ITALIC, false));
        it.setItemMeta(meta);
    }

    public static void applyLore(ItemStack it, List<String> lore) {
        if (it == null || lore == null || lore.isEmpty()) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
        for (String s : lore) {
            var comp = (s != null && (s.indexOf('&') >= 0 || s.indexOf('§') >= 0))
                    ? LEGACY_AMP.deserialize(s)
                    : MM.deserialize(s == null ? "" : s);
            lines.add(comp.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        it.setItemMeta(meta);
    }

    /**
     * Append lore lines to an item, preserving any lore already present (e.g. lore
     * configured on a reward's {@code display-item}). Existing lines are kept on top;
     * the supplied lines are deserialized (MiniMessage, or '&'/§ legacy codes) and
     * added below them.
     */
    public static void appendLore(ItemStack it, List<String> lore) {
        if (it == null || lore == null || lore.isEmpty()) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        List<net.kyori.adventure.text.Component> lines = meta.hasLore()
                ? new ArrayList<>(meta.lore())
                : new ArrayList<>();
        for (String s : lore) {
            var comp = (s != null && (s.indexOf('&') >= 0 || s.indexOf('§') >= 0))
                    ? LEGACY_AMP.deserialize(s)
                    : MM.deserialize(s == null ? "" : s);
            lines.add(comp.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        it.setItemMeta(meta);
    }

    public static String prettyMaterialName(Material mat) {
        if (mat == null) return "";
        String base = mat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder sb = new StringBuilder(base.length());
        boolean cap = true;
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (cap && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
            if (c == ' ') cap = true;
        }
        return sb.toString();
    }

    public static void tagKey(ItemStack it, String keyId) {
        if (it == null || KEY_TAG == null) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KEY_TAG, PersistentDataType.STRING, keyId);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(meta);
    }

    public static boolean hasKeyTag(ItemStack it, String expected) {
        if (it == null || !it.hasItemMeta()) return false;
        if (KEY_TAG == null) return false;
        String id = it.getItemMeta().getPersistentDataContainer().get(KEY_TAG, PersistentDataType.STRING);
        return expected.equalsIgnoreCase(String.valueOf(id));
    }

    private ItemUtil(){}
}
