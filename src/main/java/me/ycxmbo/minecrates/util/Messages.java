package me.ycxmbo.minecrates.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Messages {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void msg(CommandSender to, String mm) {
        to.sendMessage(MM.deserialize(mm));
    }

    /**
     * Send a command response with the configurable prefix from config.yml (messages.prefix).
     * Keeps existing MiniMessage formatting for the message body.
     */
    public static void cmd(CommandSender to, String mm) {
        String prefix;
        try {
            prefix = me.ycxmbo.minecrates.MineCrates.get()
                    .configManager()
                    .config()
                    .getString("messages.prefix", "");
        } catch (Throwable t) {
            prefix = ""; // fallback if plugin not initialized
        }
        to.sendMessage(MM.deserialize((prefix == null ? "" : prefix) + (mm == null ? "" : mm)));
    }

    private Messages(){}
}
