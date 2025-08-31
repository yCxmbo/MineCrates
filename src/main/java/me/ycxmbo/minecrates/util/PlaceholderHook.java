package me.ycxmbo.minecrates.util;

import org.bukkit.Bukkit;

public final class PlaceholderHook {
    public static boolean available() { return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null; }
    private PlaceholderHook() {}
}
