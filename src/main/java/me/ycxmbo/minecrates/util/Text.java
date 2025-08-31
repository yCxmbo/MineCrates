package me.ycxmbo.minecrates.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Text helpers for converting MiniMessage strings into legacy color codes
 * for APIs that do not support Adventure (e.g., DecentHolograms).
 */
public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Text() {}

    /**
     * Deserialize a MiniMessage string and serialize to legacy section codes.
     */
    public static String toLegacy(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) return "";
        Component c = MM.deserialize(miniMessage);
        return LEGACY.serialize(c);
    }

    /**
     * Convert a list of MiniMessage strings to legacy for holograms.
     */
    public static List<String> toLegacy(List<String> miniMessages) {
        if (miniMessages == null || miniMessages.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>(miniMessages.size());
        for (String s : miniMessages) out.add(toLegacy(s));
        return out;
    }
}
