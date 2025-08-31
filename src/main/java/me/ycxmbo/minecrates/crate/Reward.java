package me.ycxmbo.minecrates.crate;

import me.ycxmbo.minecrates.util.ItemUtil;
import me.ycxmbo.minecrates.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class Reward {
    public enum Rarity { COMMON, RARE, EPIC, LEGENDARY }

    private final String id;
    private final double weight;
    private final Rarity rarity;
    private final List<ItemStack> items;
    private final List<String> commands;
    private final boolean announce;
    private final String message;

    public Reward(String id, double weight, Rarity rarity, List<ItemStack> items,
                  List<String> commands, boolean announce, String message) {
        this.id = id;
        this.weight = Math.max(0.00001, weight);
        this.rarity = rarity == null ? Rarity.COMMON : rarity;
        this.items = Collections.unmodifiableList(new ArrayList<>(items == null ? List.of() : items));
        this.commands = Collections.unmodifiableList(new ArrayList<>(commands == null ? List.of() : commands));
        this.announce = announce;
        this.message = message == null ? "" : message;
    }

    public String id() { return id; }
    public double weight() { return weight; }
    public Rarity rarity() { return rarity; }
    public List<ItemStack> items() { return items; }
    public List<String> commands() { return commands; }
    public boolean announce() { return announce; }
    public String message() { return message; }

    public void give(Player p) {
        // items
        for (ItemStack it : items) {
            if (it == null || it.getType() == Material.AIR) continue;
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(it.clone());
            left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
        }
        // commands
        for (String raw : commands) {
            String cmd = raw.replace("<player>", p.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        // messages
        if (!message.isEmpty()) Messages.msg(p, message);
        if (announce) Bukkit.broadcast(MessageFormatter.winBroadcast(p.getName(), id), ""); // no perm gate
    }

    public static Reward fromSection(String id, ConfigurationSection sec) {
        double weight = sec.getDouble("weight", 1.0);
        Rarity rarity = Rarity.valueOf(sec.getString("rarity","COMMON").toUpperCase(Locale.ROOT));
        boolean announce = sec.getBoolean("announce", false);
        String message = sec.getString("message", "");

        List<ItemStack> items = new ArrayList<>();
        if (sec.isConfigurationSection("items")) {
            ConfigurationSection is = sec.getConfigurationSection("items");
            for (String k : is.getKeys(false)) {
                ItemStack it = ItemUtil.itemFromSection(is.getConfigurationSection(k));
                if (it != null) items.add(it);
            }
        }
        List<String> cmds = sec.getStringList("commands");

        return new Reward(id, weight, rarity, items, cmds, announce, message);
    }

    private static final class MessageFormatter {
        static String winBroadcast(String player, String rewardId) {
            return "<yellow>" + player + "</yellow> <gray>won</gray> <gold>" + rewardId + "</gold> <gray>from a crate!</gray>";
        }
    }
}
