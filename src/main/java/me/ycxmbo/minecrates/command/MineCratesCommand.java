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

    // Configurable help using messages.yml entries
    private void sendConfigurableHelp(CommandSender to, String label) {
        var cfg = plugin.configManager();
        Messages.msg(to, cfg.msg("help.header-top"));
        Messages.msg(to, cfg.msg("help.title"));
        Messages.msg(to, cfg.msg("help.header-bottom"));

        Messages.msg(to, helpLine(label, "list", cfg.msg("help.entries.list"), true, true));
        Messages.msg(to, helpLineSuggest(label, "preview ", cfg.msg("help.entries.preview"), hasPerm(to, "minecrates.preview")));
        Messages.msg(to, helpLineSuggest(label, "open ", cfg.msg("help.entries.open"), hasPerm(to, "minecrates.open")));
        Messages.msg(to, helpLineSuggest(label, "givekey ", cfg.msg("help.entries.givekey"), hasPerm(to, "minecrates.givekey")));
        Messages.msg(to, helpLineSuggest(label, "giveall ", cfg.msg("help.entries.giveall"), hasPerm(to, "minecrates.giveall")));
        Messages.msg(to, helpLineSuggest(label, "set ", cfg.msg("help.entries.set"), hasPerm(to, "minecrates.set")));
        Messages.msg(to, helpLineSuggest(label, "remove ", cfg.msg("help.entries.remove"), hasPerm(to, "minecrates.remove")));
        Messages.msg(to, helpLineSuggest(label, "testroll ", cfg.msg("help.entries.testroll"), hasPerm(to, "minecrates.testroll")));
        Messages.msg(to, helpLine(label, "reload", cfg.msg("help.entries.reload"), hasPerm(to, "minecrates.reload"), false));
        Messages.msg(to, helpLine(label, "editor", cfg.msg("help.entries.editor"), true, false));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendConfigurableHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "editor" -> {
                if (!(sender instanceof Player p)) { Messages.cmd(sender, "<red>Players only.</red>"); return true; }
                new CrateListGUI(plugin, service).open(p);
            }
            case "list" -> {
                Collection<Crate> crates = service.crates();
                if (crates.isEmpty()) {
                    Messages.cmd(sender, "<gray>━━━</gray>");
                } else {
                    String ids = crates.stream().map(Crate::id).sorted().collect(Collectors.joining("<gray>━━━</gray>"));
                    Messages.cmd(sender, "<green>Loaded crates:</green> <white>" + ids + "</white>");
                }
            }

            case "preview" -> {
                if (!(sender instanceof Player p)) { Messages.cmd(sender, "<red>Players only.</red>"); return true; }
                if (args.length < 2) { Messages.cmd(sender, "<red>Usage:</red> /" + label + " preview <crate>"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.cmd(sender, "<red>Unknown crate.</red>"); return true; }
                if (!p.hasPermission("minecrates.preview." + crate.id())) { Messages.cmd(p, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("perm.preview-deny")); return true; }
                me.ycxmbo.minecrates.gui.PreviewGUI.open(p, null, crate);
            }

            case "open" -> {
                if (args.length < 2) { Messages.cmd(sender, "<red>Usage:</red> /" + label + " open <crate> [player]"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.cmd(sender, "<red>Unknown crate.</red>"); return true; }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) { Messages.cmd(sender, "<red>Player not found.</red>"); return true; }
                } else {
                    if (!(sender instanceof Player p)) { Messages.cmd(sender, "<red>Specify a player.</red>"); return true; }
                    target = p;
                }
                if (!target.hasPermission("minecrates.open." + crate.id())) { Messages.cmd(sender, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("perm.open-deny")); return true; }
                service.open(target, crate).whenComplete((ok, ex) -> {
                    if (ex != null) Messages.cmd(sender, "<red>Open failed:</red> " + ex.getMessage());
                    else if (Boolean.FALSE.equals(ok)) Messages.cmd(sender, "<red>Open cancelled.</red>");
                });
            }

            case "openmulti" -> {
                if (args.length < 3) { Messages.cmd(sender, "<red>Usage:</red> /" + label + " openmulti <crate> <n> [player]"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.cmd(sender, "<red>Unknown crate.</red>"); return true; }
                int n; try { n = Math.min(100, Math.max(1, Integer.parseInt(args[2]))); } catch (Exception e) { Messages.cmd(sender, "<red>Invalid n.</red>"); return true; }
                Player target;
                if (args.length >= 4) {
                    target = Bukkit.getPlayerExact(args[3]);
                    if (target == null) { Messages.cmd(sender, "<red>Player not found.</red>"); return true; }
                } else { if (!(sender instanceof Player p)) { Messages.cmd(sender, "<red>Specify a player.</red>"); return true; } target = p; }
                if (!target.hasPermission("minecrates.open." + crate.id())) { Messages.cmd(sender, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("perm.open-deny")); return true; }
                // Open n times quickly (no animation): pick and give directly on main thread
                Bukkit.getScheduler().runTask(me.ycxmbo.minecrates.MineCrates.get(), () -> {
                    int granted = 0;
                    for (int i=0;i<n;i++) {
                        Reward r = service.pick(crate, java.util.concurrent.ThreadLocalRandom.current());
                        if (r == null) continue;
                        r.give(target, me.ycxmbo.minecrates.MineCrates.get().vault());
                        granted++;
                    }
                    Messages.cmd(sender, "<green>Granted</green> <yellow>"+granted+"</yellow> rewards to <white>"+target.getName()+"</white>.");
                });
            }

            case "binds" -> {
                java.util.Map<org.bukkit.Location,String> map = service.allBindings();
                if (args.length >= 2) {
                    String id = args[1].toLowerCase(java.util.Locale.ROOT);
                    map = map.entrySet().stream().filter(e -> e.getValue().equals(id)).collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue));
                }
                if (map.isEmpty()) { Messages.cmd(sender, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("binds.none")); return true; }
                Messages.cmd(sender, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("binds.header").replace("<count>", String.valueOf(map.size())));
                for (var e : map.entrySet()) {
                    var l = e.getKey();
                    String line = me.ycxmbo.minecrates.MineCrates.get().configManager().msg("binds.line")
                            .replace("<world>", l.getWorld().getName())
                            .replace("<x>", String.valueOf(l.getBlockX()))
                            .replace("<y>", String.valueOf(l.getBlockY()))
                            .replace("<z>", String.valueOf(l.getBlockZ()))
                            .replace("<crate>", e.getValue());
                    Messages.cmd(sender, line);
                }
            }

            case "unbindhere" -> {
                if (!(sender instanceof Player p)) { Messages.cmd(sender, "<red>Players only.</red>"); return true; }
                org.bukkit.block.Block b = p.getTargetBlockExact(6);
                if (b == null) { Messages.cmd(sender, "<red>Look at a block within 6 blocks.</red>"); return true; }
                service.unbind(b.getLocation());
                Messages.cmd(sender, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("binds.unbindhere-done"));
            }

            case "remove" -> {
                if (!(sender instanceof Player p)) { Messages.cmd(sender, "<red>Players only.</red>"); return true; }
                Block b = p.getTargetBlockExact(6);
                if (b == null) { Messages.cmd(sender, "<red>Look at a block within 6 blocks.</red>"); return true; }
                service.unbind(b.getLocation());
                Messages.cmd(sender, me.ycxmbo.minecrates.MineCrates.get().configManager().msg("binds.unbindhere-done"));
            }

            case "givekey" -> {
                if (args.length < 4) { Messages.cmd(sender, "<red>Usage:</red> /" + label + " givekey <player> <key> <amount> [virtual]"); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { Messages.cmd(sender, "<red>Player not found.</red>"); return true; }
                String keyId = args[2].toLowerCase(Locale.ROOT);
                int amt;
                try { amt = Math.max(1, Integer.parseInt(args[3])); } catch (Exception e) { Messages.cmd(sender, "<red>Invalid amount.</red>"); return true; }
                boolean virtual = args.length >= 5 && args[4].equalsIgnoreCase("virtual");

                if (virtual) {
                    service.giveVirtualKeys(t.getUniqueId(), keyId, amt);
                    String keyName = service.keyDisplay(keyId);
                    Messages.cmd(sender, "<green>Gave</green> <yellow>" + amt + "</yellow> <white>virtual</white> keys <white>" + keyName + "</white> to <white>" + t.getName() + "</white>.");
                } else {
                    ItemStack item = service.createKeyItem(keyId, amt);
                    if (item == null || item.getType() == Material.AIR) { Messages.cmd(sender, "<red>Unknown key.</red>"); return true; }
                    t.getInventory().addItem(item);
                    String keyName = service.keyDisplay(keyId);
                    Messages.cmd(sender, "<green>Gave</green> <yellow>" + amt + "</yellow> key items <white>"+keyName+"</white> to <white>"+t.getName()+"</white>.");
                }
            }

            case "giveall" -> {
                if (args.length < 3) { Messages.cmd(sender, "<red>Usage:</red> /" + label + " giveall <key> <amount> [virtual]"); return true; }
                String keyId = args[1].toLowerCase(Locale.ROOT);
                int amt; try { amt = Math.max(1, Integer.parseInt(args[2])); } catch (Exception e) { Messages.cmd(sender, "<red>Invalid amount.</red>"); return true; }
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
                String keyNameAll = service.keyDisplay(keyId);
                Messages.cmd(sender, "<green>Gave</green> <yellow>" + amt + "</yellow> " + (virtual? "virtual " : "") + "keys <white>" + keyNameAll + "</white> to <white>" + count + "</white> player(s).");
            }

            case "set" -> {
                if (!(sender instanceof Player p)) { Messages.cmd(sender, "<red>Players only.</red>"); return true; }
                if (args.length < 2) { Messages.cmd(sender, "<red>Usage:</red> /" + label + " set <crate>"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.cmd(sender, "<red>Unknown crate.</red>"); return true; }
                Block target = p.getTargetBlockExact(6);
                if (target == null) { Messages.cmd(sender, "<red>Look at a block within 6 blocks.</red>"); return true; }
                service.bind(target.getLocation(), crate.id());
                Messages.cmd(sender, "<green>Bound</green> <white>" + crate.displayName() + "</white> at <yellow>"
                        + target.getX() + "," + target.getY() + "," + target.getZ() + "</yellow>.");
            }

            case "reload" -> {
                // Reload config.yml and messages.yml first so crate defaults see new config values
                plugin.configManager().reload();
                service.reloadAllAsync().whenComplete((v, ex) -> {
                    if (ex != null) {
                        String fail = plugin.configManager().config().getString(
                                "messages.reload-failed",
                                "<red>Reload failed:</red> <white><error></white>")
                                .replace("<error>", String.valueOf(ex.getMessage()));
                        Messages.cmd(sender, fail);
                    } else {
                        String ok = plugin.configManager().config().getString(
                                "messages.reload-success",
                                "<green>Plugin has been reloaded.</green>");
                        Messages.cmd(sender, ok);
                    }
                });
            }

            case "testroll" -> {
                if (args.length < 3) { Messages.cmd(sender, "<red>Usage:</red> /" + label + " testroll <crate> <n>"); return true; }
                Crate crate = service.crate(args[1]);
                if (crate == null) { Messages.cmd(sender, "<red>Unknown crate.</red>"); return true; }
                int n; try { n = Math.max(1, Integer.parseInt(args[2])); } catch (Exception e) { Messages.cmd(sender, "<red>Invalid n.</red>"); return true; }
                Map<String,Integer> counts = new HashMap<>();
                for (int i=0;i<n;i++) {
                    Reward r = service.pick(crate, ThreadLocalRandom.current());
                    counts.merge(r.id(), 1, Integer::sum);
                }
                Messages.cmd(sender, "<gold>Results (" + n + "):</gold>");
                counts.forEach((id,c) -> Messages.cmd(sender, " - <yellow>" + id + "</yellow>: <white>" + c + "</white>"));
            }

            default -> sendConfigurableHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender to, String label) {
        Messages.cmd(to, """
                <gray>━━━</gray> <green><b>Mine</b></green><white><b>Crates</b></white> <gray>━━━</gray>
                <white>/%s list</white> <gray>━━━</gray>
                <white>/%s preview <crate></white> <gray>━━━</gray>
                <white>/%s open <crate> [player]</white> <gray>━━━</gray>
                <white>/%s givekey <player> <key> <amount> [virtual]</white>
                <white>/%s giveall <key> <amount> [virtual]</white>
                <white>/%s set <crate></white> <gray>━━━</gray>
                <white>/%s testroll <crate> <n></white>
                <white>/%s reload</white>
                <white>/%s editor</white> <gray>━━━</gray>
                """.formatted(label, label, label, label, label, label, label, label, label));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€ Tab Complete â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("help","list","preview","open","givekey","giveall","set","remove","reload","testroll","editor"));
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

    // Fancy help renderer with click/hover actions
    private void sendFancyHelp(CommandSender to, String label) {
        Messages.cmd(to, "<gray>┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓</gray>");
        Messages.cmd(to, "<gradient:#00E1FF:#FF00B3><b> MineCrates</b></gradient> <gray>|</gray> <white>Help</white>");
        Messages.cmd(to, "<gray>┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛</gray>");

        Messages.msg(to, helpLine(label, "list", "Show loaded crates", true, true));
        Messages.msg(to, helpLineSuggest(label, "preview ", "Preview a crate", hasPerm(to, "minecrates.preview")));
        Messages.msg(to, helpLineSuggest(label, "open ", "Open crate (self/other)", hasPerm(to, "minecrates.open")));
        Messages.msg(to, helpLineSuggest(label, "givekey ", "Give a key to player", hasPerm(to, "minecrates.givekey")));
        Messages.msg(to, helpLineSuggest(label, "giveall ", "Give a key to all", hasPerm(to, "minecrates.giveall")));
        Messages.msg(to, helpLineSuggest(label, "set ", "Bind targeted block to crate", hasPerm(to, "minecrates.set")));
        Messages.msg(to, helpLineSuggest(label, "testroll ", "Simulate n rolls", hasPerm(to, "minecrates.testroll")));
        Messages.msg(to, helpLine(label, "reload", "Reload configs", hasPerm(to, "minecrates.reload"), false));
        Messages.msg(to, helpLine(label, "editor", "Open crate editor", true, false));
    }

    private boolean hasPerm(CommandSender s, String perm) {
        return !(s instanceof Player) || s.hasPermission(perm);
    }

    private String helpLine(String label, String sub, String desc, boolean show, boolean run) {
        if (!show) return "";
        String cmd = "/" + label + " " + sub;
        String click = run ? "run_command" : "suggest_command";
        return "<click:" + click + ":" + cmd + "><hover:show_text:'" + desc + "'><white>" + cmd + "</white></hover></click> <gray>—</gray> <gray>" + desc + "</gray>";
    }

    private String helpLineSuggest(String label, String subWithSpace, String desc, boolean show) {
        if (!show) return "";
        String cmd = "/" + label + " " + subWithSpace;
        return "<click:suggest_command:" + cmd + "><hover:show_text:'" + desc + "'><white>" + cmd + "</white></hover></click> <gray>—</gray> <gray>" + desc + "</gray>";
    }
}



