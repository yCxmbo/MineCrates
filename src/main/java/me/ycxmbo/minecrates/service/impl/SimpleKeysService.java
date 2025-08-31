package me.ycxmbo.minecrates.service.impl;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.service.KeysService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class SimpleKeysService implements KeysService {

    private final MineCrates plugin;
    private final NamespacedKey PDC_KEY;
    private final Map<UUID, Map<String, Integer>> virtual = new HashMap<>();
    private final Set<String> knownKeyIds = new HashSet<>();

    public SimpleKeysService(MineCrates plugin) {
        this.plugin = plugin;
        this.PDC_KEY = new NamespacedKey(plugin, "key-id");
    }

    @Override public NamespacedKey keyTag() { return PDC_KEY; }

    // ── Virtual ────────────────────────────────────────────────────────────────
    @Override
    public int getVirtual(UUID player, String keyId) {
        return virtual.getOrDefault(player, Collections.emptyMap()).getOrDefault(keyId.toLowerCase(Locale.ROOT), 0);
    }
    @Override
    public void addVirtual(UUID player, String keyId, int amount) {
        if (amount == 0) return;
        var inner = virtual.computeIfAbsent(player, k -> new HashMap<>());
        inner.merge(keyId.toLowerCase(Locale.ROOT), amount, Integer::sum);
        if (inner.get(keyId.toLowerCase(Locale.ROOT)) <= 0) inner.remove(keyId.toLowerCase(Locale.ROOT));
        knownKeyIds.add(keyId.toLowerCase(Locale.ROOT));
    }

    // ── Physical ───────────────────────────────────────────────────────────────
    @Override
    public Optional<ItemStack> createPhysicalKey(String keyId, int amount) {
        ItemStack it = new ItemStack(Material.TRIPWIRE_HOOK, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Crate Key" + ChatColor.GRAY + " (" + keyId + ")");
            meta.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.STRING, keyId.toLowerCase(Locale.ROOT));
            it.setItemMeta(meta);
        }
        knownKeyIds.add(keyId.toLowerCase(Locale.ROOT));
        return Optional.of(it);
    }

    @Override public Collection<String> allKeyIds() { return Collections.unmodifiableSet(knownKeyIds); }

    @Override
    public boolean isKey(ItemStack stack, String keyId) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta(); if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        String tag = pdc.get(PDC_KEY, PersistentDataType.STRING);
        return tag != null && tag.equalsIgnoreCase(keyId);
    }

    @Override
    public boolean takeOneInHand(Player player, String keyId) {
        ItemStack in = player.getInventory().getItemInMainHand();
        if (!isKey(in, keyId)) return false;
        int amt = in.getAmount();
        if (amt <= 1) player.getInventory().setItemInMainHand(null);
        else in.setAmount(amt - 1);
        return true;
    }
}
