package me.ycxmbo.minecrates.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Messages {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void msg(CommandSender to, String mm) {
        to.sendMessage(MM.deserialize(mm));
    }

    private Messages(){}
}
