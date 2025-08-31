package me.ycxmbo.minecrates.command;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.gui.CrateListGUI;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.util.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class MineCratesCommand implements CommandExecutor, TabCompleter {

    private final MineCrates plugin;
    private final CrateService service;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MineCratesCommand(MineCrates plugin, CrateService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "editor" -> {
                if (!(sender instanceof Player p)) { Messages.msg(sender, "<red>Players only.</red>"); return true; }
                new CrateListGUI(plugin, service).open(p);
            }
            case "list" -> {
                Collection<Crate> crates = service.crates();
                if (crates.isEmpty()) {
                    Messages.msg(sender, "<gray>No crates loaded.</gray>");
                } else {
                    String ids = crates.stream().map(Crate::id).sorted().collect(Collectors.joining("<gray>, </gray>"));
                    Messages.msg(sender, "<green>Loaded crates:</green> <white>" + ids + "</white>");
                }
            }

            case "preview" -> {
                if (!(sender instanceof Player p)) { Messages.msg(sender, "<red>Players only.</red>"); return true; }
                if (args.length < 2) { Messages.msg(sender, "<red>Usage:</red> /" + label + " preview <crate>"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.msg(sender, "<red>Unknown crate.</red>"); return true; }
                // Simple text preview (GUI arrives in next bundle)
                Messages.msg(p, "<gold>Preview:</gold> <white>" + crate.displayName() + "</white>");
                for (Reward r : crate.rewards()) {
                    double pct = service.weightPercent(crate, r) * 100.0;
                    Messages.msg(p, " - <yellow>" + r.id() + "</yellow> <gray>(" + String.format(Locale.US, "%.2f", pct) + "%)</gray>");
                }
            }

            case "open" -> {
                if (args.length < 2) { Messages.msg(sender, "<red>Usage:</red> /" + label + " open <crate> [player]"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.msg(sender, "<red>Unknown crate.</red>"); return true; }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) { Messages.msg(sender, "<red>Player not found.</red>"); return true; }
                } else {
                    if (!(sender instanceof Player p)) { Messages.msg(sender, "<red>Specify a player.</red>"); return true; }
                    target = p;
                }
                service.open(target, crate).whenComplete((ok, ex) -> {
                    if (ex != null) Messages.msg(sender, "<red>Open failed:</red> " + ex.getMessage());
                    else if (Boolean.FALSE.equals(ok)) Messages.msg(sender, "<red>Open cancelled.</red>");
                });
            }

            case "givekey" -> {
                if (args.length < 4) { Messages.msg(sender, "<red>Usage:</red> /" + label + " givekey <player> <key> <amount> [virtual]"); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { Messages.msg(sender, "<red>Player not found.</red>"); return true; }
                String keyId = args[2].toLowerCase(Locale.ROOT);
                int amt;
                try { amt = Math.max(1, Integer.parseInt(args[3])); } catch (Exception e) { Messages.msg(sender, "<red>Invalid amount.</red>"); return true; }
                boolean virtual = args.length >= 5 && args[4].equalsIgnoreCase("virtual");

                if (virtual) {
                    service.giveVirtualKeys(t.getUniqueId(), keyId, amt);
                    Messages.msg(sender, "<green>Gave</green> <yellow>" + amt + "</yellow> <white>virtual</white> keys <gray>(</gray>"+keyId+"<gray>)</gray> to <white>"+t.getName()+"</white>.");
                } else {
                    ItemStack item = service.createKeyItem(keyId, amt);
                    if (item == null || item.getType() == Material.AIR) { Messages.msg(sender, "<red>Unknown key.</red>"); return true; }
                    t.getInventory().addItem(item);
                    Messages.msg(sender, "<green>Gave</green> <yellow>" + amt + "</yellow> key items <gray>(</gray>"+keyId+"<gray>)</gray> to <white>"+t.getName()+"</white>.");
                }
            }

            case "giveall" -> {
                if (args.length < 3) { Messages.msg(sender, "<red>Usage:</red> /" + label + " giveall <key> <amount> [virtual]"); return true; }
                String keyId = args[1].toLowerCase(Locale.ROOT);
                int amt; try { amt = Math.max(1, Integer.parseInt(args[2])); } catch (Exception e) { Messages.msg(sender, "<red>Invalid amount.</red>"); return true; }
                boolean virtual = args.length >= 4 && args[3].equalsIgnoreCase("virtual");
                int count = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (virtual) service.giveVirtualKeys(p.getUniqueId(), keyId, amt);
                    else {
                        ItemStack item = service.createKeyItem(keyId, amt);
                        if (item != null && item.getType() != Material.AIR) p.getInventory().addItem(item);
                    }
                    count++;
                }
                Messages.msg(sender, "<green>Gave</green> <yellow>" + amt + "</yellow> " + (virtual? "virtual " : "") + "keys to <white>" + count + "</white> player(s).");
            }

            case "set" -> {
                if (!(sender instanceof Player p)) { Messages.msg(sender, "<red>Players only.</red>"); return true; }
                if (args.length < 2) { Messages.msg(sender, "<red>Usage:</red> /" + label + " set <crate>"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.msg(sender, "<red>Unknown crate.</red>"); return true; }
                Block target = p.getTargetBlockExact(6);
                if (target == null) { Messages.msg(sender, "<red>Look at a block within 6 blocks.</red>"); return true; }
                service.bind(target.getLocation(), crate.id());
                Messages.msg(sender, "<green>Bound</green> <white>" + crate.displayName() + "</white> at <yellow>"
                        + target.getX() + "," + target.getY() + "," + target.getZ() + "</yellow>.");
            }

            case "reload" -> {
                service.reloadAllAsync().whenComplete((v, ex) -> {
                    if (ex != null) Messages.msg(sender, "<red>Reload failed:</red> " + ex.getMessage());
                    else Messages.msg(sender, "<green>Reloaded.</green>");
                });
            }

            case "testroll" -> {
                if (args.length < 3) { Messages.msg(sender, "<red>Usage:</red> /" + label + " testroll <crate> <n>"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.msg(sender, "<red>Unknown crate.</red>"); return true; }
                int n; try { n = Math.max(1, Integer.parseInt(args[2])); } catch (Exception e) { Messages.msg(sender, "<red>Invalid n.</red>"); return true; }
                Map<String,Integer> counts = new HashMap<>();
                for (int i=0;i<n;i++) {
                    Reward r = service.pick(crate, ThreadLocalRandom.current());
                    counts.merge(r.id(), 1, Integer::sum);
                }
                Messages.msg(sender, "<gold>Results (" + n + "):</gold>");
                counts.forEach((id,c) -> Messages.msg(sender, " - <yellow>" + id + "</yellow>: <white>" + c + "</white>"));
            }

            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender to, String label) {
        Messages.msg(to, """
                <gray>———</gray> <green><b>Mine</b></green><white><b>Crates</b></white> <gray>———</gray>
                <white>/%s list</white> <gray>— list loaded crates</gray>
                <white>/%s preview <crate></white> <gray>— preview rewards</gray>
                <white>/%s open <crate> [player]</white> <gray>— open crate</gray>
                <white>/%s givekey <player> <key> <amount> [virtual]</white>
                <white>/%s giveall <key> <amount> [virtual]</white>
                <white>/%s set <crate></white> <gray>— bind to targeted block</gray>
                <white>/%s testroll <crate> <n></white>
                <white>/%s reload</white>
                <white>/%s editor</white> <gray>— edit crates</gray>
                """.formatted(label, label, label, label, label, label, label, label));
    }

    // ───────── Tab Complete ─────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("help","list","preview","open","givekey","giveall","set","reload","testroll","editor"));
        } else {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "preview","open","set","testroll" -> {
                    if (args.length == 2) out.addAll(service.crates().stream().map(Crate::id).toList());
                }
                case "givekey" -> {
                    if (args.length == 2) out.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                    if (args.length == 3) out.addAll(service.keyIds());
                    if (args.length == 4) out.addAll(List.of("1","5","10","64"));
                    if (args.length == 5) out.addAll(List.of("virtual"));
                }
                case "giveall" -> {
                    if (args.length == 2) out.addAll(service.keyIds());
                    if (args.length == 3) out.addAll(List.of("1","5","10","64"));
                    if (args.length == 4) out.addAll(List.of("virtual"));
                }
            }
        }
        String last = args[args.length-1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(last)).sorted().toList();
    }
}
