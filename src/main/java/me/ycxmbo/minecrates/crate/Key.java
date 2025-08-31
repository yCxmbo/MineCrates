package me.ycxmbo.minecrates.crate;

import me.ycxmbo.minecrates.util.ItemUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

public final class Key {
    private final String id;
    private final String display;
    private final ItemStack base;

    public Key(String id, String display, ItemStack base) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.display = display;
        this.base = base == null ? new ItemStack(org.bukkit.Material.TRIPWIRE_HOOK) : base.clone();
        ItemUtil.tagKey(this.base, this.id);
        ItemUtil.applyName(this.base, display);
    }

    public String id() { return id; }
    public String display() { return display; }
    public ItemStack asItem() { return base.clone(); }

    public boolean matches(ItemStack item) {
        return ItemUtil.hasKeyTag(item, id);
    }
}
