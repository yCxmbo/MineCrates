package me.ycxmbo.minecrates.crate;

import me.ycxmbo.minecrates.hook.VaultHook;
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
    private final String display;
    private final double weight;
    private final Rarity rarity;
    private final List<ItemStack> items;
    private final List<String> commands;
    private final boolean announce;
    private final String message;
    private final double money; // optional Vault payout
    private final int expLevels; // optional XP levels

    public Reward(String id, String display, double weight, Rarity rarity, List<ItemStack> items,
                  List<String> commands, boolean announce, String message,
                  double money, int expLevels) {
        this.id = id;
        this.display = (display == null || display.isEmpty()) ? id : display;
        this.weight = Math.max(0.00001, weight);
        this.rarity = rarity == null ? Rarity.COMMON : rarity;
        this.items = Collections.unmodifiableList(new ArrayList<>(items == null ? List.of() : items));
        this.commands = Collections.unmodifiableList(new ArrayList<>(commands == null ? List.of() : commands));
        this.announce = announce;
        this.message = message == null ? "" : message;
        this.money = Math.max(0D, money);
        this.expLevels = Math.max(0, expLevels);
    }

    public String id() { return id; }
    public String displayName() { return display; }
    public double weight() { return weight; }
    public Rarity rarity() { return rarity; }
    public List<ItemStack> items() { return items; }
    public List<String> commands() { return commands; }
    public boolean announce() { return announce; }
    public String message() { return message; }

    public void give(Player p, VaultHook vault) {
        // items
        String overflow = me.ycxmbo.minecrates.MineCrates.get().getConfig().getString("rewards.inventory-overflow-policy", "drop").toLowerCase(java.util.Locale.ROOT);
        for (ItemStack it : items) {
            if (it == null || it.getType() == Material.AIR) continue;
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(it.clone());
            if (!left.isEmpty()) {
                if ("deny".equals(overflow)) {
                    me.ycxmbo.minecrates.util.Messages.msg(p, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("reward.inventory-full"));
                } else {
                    left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
                }
            }
        }
        // money
        if (money > 0 && vault != null && vault.present()) {
            vault.deposit(p, money);
        }
        // xp levels
        if (expLevels > 0) {
            p.setLevel(p.getLevel() + expLevels);
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
        String display = sec.getString("display", id);
        boolean announce = sec.getBoolean("announce", false);
        String message = sec.getString("message", "");
        double money = sec.getDouble("money", 0D);
        int exp = sec.getInt("xp-levels", 0);

        List<ItemStack> items = new ArrayList<>();
        if (sec.isConfigurationSection("items")) {
            ConfigurationSection is = sec.getConfigurationSection("items");
            for (String k : is.getKeys(false)) {
                ItemStack it = ItemUtil.itemFromSection(is.getConfigurationSection(k));
                if (it != null) items.add(it);
            }
        }
        List<String> cmds = sec.getStringList("commands");

        return new Reward(id, display, weight, rarity, items, cmds, announce, message, money, exp);
    }

    private static final class MessageFormatter {
        static String winBroadcast(String player, String rewardId) {
            return "<yellow>" + player + "</yellow> <gray>won</gray> <gold>" + rewardId + "</gold> <gray>from a crate!</gray>";
        }
    }
}
