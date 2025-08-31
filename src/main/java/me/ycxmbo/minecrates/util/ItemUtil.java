package me.ycxmbo.minecrates.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();
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

    public static void applyName(ItemStack it, String mmName) {
        if (it == null || mmName == null || mmName.isEmpty()) return;
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MM.deserialize(mmName));
        it.setItemMeta(meta);
    }

    public static void applyLore(ItemStack it, List<String> mmLore) {
        if (it == null || mmLore == null || mmLore.isEmpty()) return;
        ItemMeta meta = it.getItemMeta();
        List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
        for (String s : mmLore) lines.add(MM.deserialize(s));
        meta.lore(lines);
        it.setItemMeta(meta);
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
