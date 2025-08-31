package me.ycxmbo.minecrates.util;

import me.ycxmbo.minecrates.crate.Crate;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyHook {
    private static Economy econ;

    private EconomyHook() {}

    public static void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }

    public static boolean isReady() { return econ != null; }

    public static boolean canAfford(Player p, Crate c) {
        if (!c.isCostEnabled()) return true;
        switch (c.getCostCurrency()) {
            case VAULT -> { return isReady() && econ.getBalance(p) >= c.getCostAmount(); }
            case EXP -> { return p.getTotalExperience() >= c.getCostAmount(); }
            case EXP_LEVELS -> { return p.getLevel() >= (int)Math.ceil(c.getCostAmount()); }
            default -> { return true; }
        }
    }

    /** Deduct cost; returns true if success or not required. */
    public static boolean charge(Player p, Crate c) {
        if (!c.isCostEnabled()) return true;
        double amt = c.getCostAmount();
        switch (c.getCostCurrency()) {
            case VAULT -> {
                if (!isReady()) return false;
                return econ.withdrawPlayer(p, amt).transactionSuccess();
            }
            case EXP -> {
                int take = (int)Math.ceil(amt);
                if (p.getTotalExperience() < take) return false;
                p.giveExp(-take);
                return true;
            }
            case EXP_LEVELS -> {
                int lv = (int)Math.ceil(amt);
                if (p.getLevel() < lv) return false;
                p.setLevel(p.getLevel() - lv);
                return true;
            }
            default -> { return true; }
        }
    }

    public static void deposit(Player p, double amount) {
        if (!isReady()) return;
        econ.depositPlayer(p, amount);
    }
}
