package me.ycxmbo.minecrates.hook;

import me.ycxmbo.minecrates.MineCrates;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {
    private final Economy econ;

    public VaultHook(MineCrates plugin) {
        Economy e = null;
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) e = rsp.getProvider();
            }
        } catch (Throwable ignored) {}
        econ = e;
        if (econ == null) plugin.getLogger().info("[MineCrates] Vault not present – cost features disabled.");
    }

    public boolean present() { return econ != null; }

    public boolean withdraw(Player p, double amount) {
        if (econ == null) return false;
        return econ.withdrawPlayer(p, amount).transactionSuccess();
    }

    public void deposit(Player p, double amount) {
        if (econ != null) econ.depositPlayer(p, amount);
    }

    public String format(double amount) {
        return econ == null ? String.valueOf(amount) : econ.format(amount);
    }
}
