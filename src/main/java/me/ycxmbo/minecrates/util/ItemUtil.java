package me.ycxmbo.minecrates.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
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
        return it;
    }

    public static void applyName(ItemStack it, String name) {
        if (it == null || name == null || name.isEmpty()) return;
        ItemMeta meta = it.getItemMeta();
        var comp = (name.indexOf('&') >= 0 || name.indexOf('§') >= 0)
                ? LEGACY_AMP.deserialize(name)
                : MM.deserialize(name);
        meta.displayName(comp.decoration(TextDecoration.ITALIC, false));
        it.setItemMeta(meta);
    }

    public static void applyLore(ItemStack it, List<String> lore) {
        if (it == null || lore == null || lore.isEmpty()) return;
        ItemMeta meta = it.getItemMeta();
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
