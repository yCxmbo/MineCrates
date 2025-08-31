package me.ycxmbo.minecrates.service;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Virtual balances + physical keys (PDC-tagged). */
public interface KeysService {

    NamespacedKey keyTag(); // PDC for physical keys: string key-id

    // Virtual
    int getVirtual(UUID player, String keyId);
    void addVirtual(UUID player, String keyId, int amount);

    // Physical factory
    Optional<ItemStack> createPhysicalKey(String keyId, int amount);
    Collection<String> allKeyIds();

    // Helpers
    boolean isKey(ItemStack stack, String keyId);
    boolean takeOneInHand(Player player, String keyId); // removes 1 if the held item is that key
}
