package me.ycxmbo.minecrates.util;

import me.ycxmbo.minecrates.MineCrates;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays configurable sounds defined in {@code config.yml} under {@code sounds:}.
 * Each entry is {@code "SOUND_NAME:volume:pitch"} (e.g. {@code "ENTITY_PLAYER_LEVELUP:1.0:1.0"}).
 * A blank entry disables that sound. Invalid sound names are logged once and skipped.
 */
public final class SoundUtil {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    // Fallback specs preserving the plugin's historical sounds when config.yml predates the 'sounds' section.
    private static final Map<String, String> DEFAULTS = Map.of(
            "open", "BLOCK_CHEST_OPEN:0.7:1.0",
            "reveal-tick", "UI_BUTTON_CLICK:0.5:1.6",
            "reward-common", "ENTITY_ITEM_PICKUP:1.0:1.0",
            "reward-rare", "ENTITY_EXPERIENCE_ORB_PICKUP:1.0:1.2",
            "reward-epic", "ENTITY_PLAYER_LEVELUP:1.0:1.0",
            "reward-legendary", "UI_TOAST_CHALLENGE_COMPLETE:1.0:1.0");

    private SoundUtil() {}

    /** Plays the sound configured at {@code sounds.<key>} for the given player. */
    public static void play(Player player, String key) {
        if (player == null) return;
        String spec = MineCrates.get().getConfig().getString("sounds." + key, DEFAULTS.getOrDefault(key, ""));
        playSpec(player, spec);
    }

    /** Plays a raw {@code SOUND:volume:pitch} spec. Blank or malformed specs are ignored. */
    public static void playSpec(Player player, String spec) {
        if (player == null || spec == null || spec.isBlank()) return;
        String[] parts = spec.split(":");
        String name = parts[0].trim();
        if (name.isEmpty()) return;
        float volume = parseFloat(parts.length > 1 ? parts[1] : null, 1.0f);
        float pitch = parseFloat(parts.length > 2 ? parts[2] : null, 1.0f);

        Sound sound = resolve(name);
        if (sound == null) {
            if (WARNED.add(name)) {
                MineCrates.get().getLogger().warning("Unknown sound name in config: '" + name + "' (skipping).");
            }
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static Sound resolve(String name) {
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static float parseFloat(String raw, float def) {
        if (raw == null) return def;
        try { return Float.parseFloat(raw.trim()); } catch (NumberFormatException ex) { return def; }
    }
}
