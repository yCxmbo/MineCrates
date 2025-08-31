package me.ycxmbo.minecrates.util;

import org.bukkit.ChatColor;

/** Maps reward rarity to a color. Works with enum name() or plain string. */
public final class RarityStyle {

    public static String colorizeName(String base, Object rarity) {
        ChatColor c = colorFor(rarity);
        return c + (base == null ? "Reward" : base);
    }

    public static ChatColor colorFor(Object rarity) {
        String r = rarity == null ? "" : rarity.toString().toUpperCase();
        return switch (r) {
            case "COMMON" -> ChatColor.WHITE;
            case "UNCOMMON" -> ChatColor.GREEN;
            case "RARE" -> ChatColor.AQUA;
            case "EPIC" -> ChatColor.DARK_PURPLE;
            case "LEGENDARY" -> ChatColor.GOLD;
            case "MYTHIC", "MYTHICAL" -> ChatColor.RED;
            default -> ChatColor.WHITE;
        };
    }

    private RarityStyle(){}
}
