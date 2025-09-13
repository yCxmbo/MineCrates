package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BookEditManager {

    public enum Mode { MESSAGE, COMMANDS, HOLOGRAM_LINES }

    public static record Session(Mode mode, RewardDetailGUI rdGui, CrateEditGUI ceGui) {}

    private static final Map<UUID, Session> SESS = new ConcurrentHashMap<>();
    private static final NamespacedKey TAG = NamespacedKey.fromString("minecrates:editor");

    private BookEditManager() {}

    public static boolean startMessage(Player p, RewardDetailGUI gui, String initial) {
        return openBook(p, new Session(Mode.MESSAGE, gui, null), initial == null ? List.of("") : List.of(initial));
    }

    public static boolean startCommands(Player p, RewardDetailGUI gui, List<String> commands) {
        List<String> pages = (commands == null || commands.isEmpty()) ? List.of("") : new ArrayList<>(commands);
        return openBook(p, new Session(Mode.COMMANDS, gui, null), pages);
    }

    public static boolean startHologramLines(Player p, CrateEditGUI gui, List<String> lines) {
        List<String> pages = (lines == null || lines.isEmpty()) ? List.of("") : new ArrayList<>(lines);
        return openBook(p, new Session(Mode.HOLOGRAM_LINES, null, gui), pages);
    }

    private static boolean openBook(Player p, Session session, List<String> pages) {
        try {
            ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            meta.title(Component.text("MineCrates Editor"));
            meta.author(Component.text("MineCrates"));
            List<Component> comps = new ArrayList<>();
            for (String s : pages) comps.add(Component.text(s == null ? "" : s));
            meta.pages(comps);
            if (TAG != null) meta.getPersistentDataContainer().set(TAG, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            book.setItemMeta(meta);

            // Add to inventory and instruct
            p.getInventory().addItem(book);
            Messages.msg(p, "<gray>Editor book added to your inventory.</gray> <white>Edit and press Done.</white>");
            SESS.put(p.getUniqueId(), session);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static Session session(UUID id) { return SESS.get(id); }
    static void clear(UUID id) { SESS.remove(id); }
    static NamespacedKey tag() { return TAG; }
    static List<String> pagesToLines(BookMeta meta) {
        List<String> out = new ArrayList<>();
        if (meta == null || meta.pages() == null) return out;
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        for (Component c : meta.pages()) out.add(plain.serialize(c));
        return out;
    }
}
