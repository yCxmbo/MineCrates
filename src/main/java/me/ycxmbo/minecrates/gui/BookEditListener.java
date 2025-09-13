package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

public final class BookEditListener implements Listener {

    @EventHandler
    public void onEdit(PlayerEditBookEvent e) {
        Player p = e.getPlayer();
        var sess = BookEditManager.session(p.getUniqueId());
        if (sess == null) return;

        // Only process our tagged editor books
        try {
            BookMeta prev = e.getPreviousBookMeta();
            BookMeta next = e.getNewBookMeta();
            boolean ours = false;
            if (prev != null && BookEditManager.tag() != null && prev.getPersistentDataContainer().has(BookEditManager.tag(), org.bukkit.persistence.PersistentDataType.INTEGER)) ours = true;
            if (!ours && next != null && BookEditManager.tag() != null && next.getPersistentDataContainer().has(BookEditManager.tag(), org.bukkit.persistence.PersistentDataType.INTEGER)) ours = true;
            if (!ours) return;
        } catch (Throwable ignored) {}

        e.setCancelled(true); // prevent converting to written book

        List<String> lines = BookEditManager.pagesToLines(e.getNewBookMeta());
        switch (sess.mode()) {
            case MESSAGE -> {
                var gui = sess.rdGui();
                var er = gui == null ? null : gui.target();
                if (er != null) {
                    String msg = String.join("\n", lines).trim();
                    er.message = msg;
                    Messages.msg(p, "<green>Message updated.</green>");
                    Bukkit.getScheduler().runTask(MineCrates.get(), () -> gui.open(p));
                }
            }
            case COMMANDS -> {
                var gui = sess.rdGui();
                var er = gui == null ? null : gui.target();
                if (er != null) {
                    er.commands.clear();
                    for (String s : lines) if (s != null && !s.isBlank()) er.commands.add(s.trim());
                    Messages.msg(p, "<green>Commands updated.</green>");
                    Bukkit.getScheduler().runTask(MineCrates.get(), () -> gui.open(p));
                }
            }
            case HOLOGRAM_LINES -> {
                var gui = sess.ceGui();
                if (gui != null) {
                    java.util.List<String> ls = new java.util.ArrayList<>();
                    for (String s : lines) if (s != null && !s.isBlank()) ls.add(s);
                    gui.setHologramLines(ls);
                    Messages.msg(p, "<green>Hologram lines updated.</green>");
                    Bukkit.getScheduler().runTask(MineCrates.get(), () -> {
                        gui.refreshTop();
                        gui.open(p);
                    });
                }
            }
        }

        // Remove our editor book from inventory
        try {
            for (int i = 0; i < p.getInventory().getSize(); i++) {
                ItemStack it = p.getInventory().getItem(i);
                if (it != null && it.getType() == Material.WRITABLE_BOOK && it.hasItemMeta()) {
                    var bm = (BookMeta) it.getItemMeta();
                    if (BookEditManager.tag() != null && bm.getPersistentDataContainer().has(BookEditManager.tag(), org.bukkit.persistence.PersistentDataType.INTEGER)) {
                        p.getInventory().setItem(i, null);
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}

        BookEditManager.clear(p.getUniqueId());
    }
}
