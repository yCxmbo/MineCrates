package me.ycxmbo.minecrates.gui;

import me.ycxmbo.minecrates.MineCrates;
import me.ycxmbo.minecrates.crate.Crate;
import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.service.CrateService;
import me.ycxmbo.minecrates.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class CrateEditGUI implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final MineCrates plugin;
    private final CrateService service;
    private final String crateId;
    private String keyId;
    private boolean requiresKey;
    private boolean costEnabled;
    private Crate.CostCurrency costCurrency;
    private double costAmount;
    private long cooldownSeconds;
    // Particles (edit state)
    private boolean particlesEnabled;
    private String particleType;
    private double particleRadius;
    private double particleYOffset;
    private int particlePoints;
    private Crate.ParticleShape particleShape;

    // Hologram
    private boolean holoEnabled;
    private double holoYOffset;
    private java.util.List<String> holoLines;

    // Animation (per-crate)
    private Crate.AnimationType animType;
    private int rouletteCycles;
    private int rouletteSpeed;
    private int revealFlickers;
    private int revealSpeed;
    private int cascadeSpeed;
    private long closeDelay;

    // Key display override
    private boolean keyDisplayOverrideEnabled;
    private String keyDisplayOverride;
    private final Inventory inv;
    private String newKeyId;

    // Crate meta
    private Crate.Type crateType;
    private String crateDisplay;

    private static final int REWARD_START = 9;
    private static final int REWARD_END = 45; // exclusive

    final List<EditedReward> rewards = new ArrayList<>();

    static final class EditedReward {
        ItemStack icon;          // display icon (not necessarily a given item)
        double weight;
        Reward.Rarity rarity;
        boolean announce;
        boolean commandOnly;     // if true, do not give items (commands/money/xp only)
        String display;          // MiniMessage-formatted display title (nullable)
        List<ItemStack> items = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        String message = "";
        double money = 0D;
        int xpLevels = 0;
        EditedReward(ItemStack icon, double weight, Reward.Rarity rarity, boolean announce, boolean commandOnly, String display) {
            this.icon = icon == null ? new ItemStack(Material.CHEST) : icon.clone();
            this.weight = Math.max(0.0001, weight);
            this.rarity = rarity == null ? Reward.Rarity.COMMON : rarity;
            this.announce = announce;
            this.commandOnly = commandOnly;
            this.display = (display == null || display.isBlank()) ? null : display;
        }
    }

    // Chat rename support for reward display (MiniMessage)
    private static final java.util.Map<java.util.UUID, RenameSession> RENAME_SESSIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private record RenameSession(CrateEditGUI gui, int index) {}

    private void startRename(org.bukkit.entity.Player player, int index) {
        RENAME_SESSIONS.put(player.getUniqueId(), new RenameSession(this, index));
        Messages.msg(player, "<gray>Type the new reward name in chat.</gray> <white>MiniMessage</white> supported. Type '-' to clear.");
    }

    public static boolean handleChatRename(org.bukkit.entity.Player player, String input) {
        RenameSession s = RENAME_SESSIONS.remove(player.getUniqueId());
        if (s == null) return false;
        MineCrates pl = MineCrates.get();
        if (pl == null) return false;
        org.bukkit.Bukkit.getScheduler().runTask(pl, () -> {
            try {
                CrateEditGUI gui = s.gui();
                if (gui == null) return;
                int idx = s.index();
                String normalized = (input == null ? "" : input.trim());
                if (idx >= 0) {
                    if (idx >= gui.rewards.size()) return;
                    EditedReward er = gui.rewards.get(idx);
                    if (normalized.equalsIgnoreCase("-") || normalized.equalsIgnoreCase("clear")) {
                        er.display = null; // revert to default
                        Messages.msg(player, "<yellow>Cleared</yellow> reward display name.");
                    } else {
                        er.display = normalized; // store MiniMessage string
                        Messages.msg(player, "<green>Set</green> reward name to: " + normalized);
                    }
                    gui.redrawRewards();
                } else {
                    // idx < 0 means crate display rename
                    if (normalized.equalsIgnoreCase("-") || normalized.equalsIgnoreCase("clear")) {
                        gui.crateDisplay = null;
                        Messages.msg(player, "<yellow>Cleared</yellow> crate display.");
                    } else {
                        gui.crateDisplay = normalized;
                        Messages.msg(player, "<green>Set</green> crate name to: " + normalized);
                    }
                    gui.rebuildTop();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        return true;
    }

    private void startCrateRename(org.bukkit.entity.Player player) {
        // use index -1 to denote crate rename
        RENAME_SESSIONS.put(player.getUniqueId(), new RenameSession(this, -1));
        Messages.msg(player, "<gray>Type the new crate display in chat.</gray> <white>MiniMessage</white> supported. '-' to clear.");
    }

    // separate handler to allow EditorListener to check without consuming reward rename session
    public static boolean handleChatCrateRename(org.bukkit.entity.Player player, String input) {
        RenameSession s = RENAME_SESSIONS.get(player.getUniqueId());
        if (s == null || s.index() >= 0) return false; // not a crate rename
        // delegate to main handler to consume and apply
        return handleChatRename(player, input);
    }

    // Hologram lines editor (chat, multiline)
    private static final java.util.Map<java.util.UUID, HoloSession> HOLO_SESSIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private record HoloSession(CrateEditGUI gui, java.util.List<String> lines) {}

    private void startHologramLinesEdit(org.bukkit.entity.Player player) {
        java.util.List<String> tmp = new java.util.ArrayList<>();
        if (holoLines != null) tmp.addAll(holoLines);
        HOLO_SESSIONS.put(player.getUniqueId(), new HoloSession(this, tmp));
        Messages.msg(player, "<gray>Editing hologram lines.</gray> Send lines one by one. Type 'done' to finish, 'clear' to empty, or 'cancel'. MiniMessage supported.");
        player.closeInventory();
    }

    public static boolean handleChatHologramLines(org.bukkit.entity.Player player, String msg) {
        HoloSession s = HOLO_SESSIONS.get(player.getUniqueId());
        if (s == null) return false;
        if (msg == null) msg = "";
        String m = msg.trim();
        if (m.equalsIgnoreCase("cancel")) {
            HOLO_SESSIONS.remove(player.getUniqueId());
            Messages.msg(player, "<gray>Cancelled.</gray>");
            org.bukkit.Bukkit.getScheduler().runTask(MineCrates.get(), () -> {
                if (s.gui() != null) s.gui().open((org.bukkit.entity.Player) player);
            });
            return true;
        }
        if (m.equalsIgnoreCase("clear")) {
            s.lines().clear();
            Messages.msg(player, "<yellow>Cleared</yellow> hologram lines.");
            return true;
        }
        if (m.equalsIgnoreCase("done")) {
            HOLO_SESSIONS.remove(player.getUniqueId());
            CrateEditGUI gui = s.gui();
            if (gui != null) {
                gui.holoLines = new java.util.ArrayList<>(s.lines());
                org.bukkit.Bukkit.getScheduler().runTask(MineCrates.get(), gui::rebuildTop);
                org.bukkit.Bukkit.getScheduler().runTask(MineCrates.get(), () -> gui.open((org.bukkit.entity.Player) player));
            }
            Messages.msg(player, "<green>Hologram lines saved.</green>");
            return true;
        }
        // regular line
        s.lines().add(msg);
        Messages.msg(player, "<gray>Added line:</gray> " + msg);
        return true;
    }

    public CrateEditGUI(MineCrates plugin, CrateService service, Crate crate) {
        this.plugin = plugin;
        this.service = service;
        this.crateId = crate.id();
        this.keyId = crate.key() == null ? "" : crate.key().id();
        this.requiresKey = crate.requiresKey();
        this.costEnabled = crate.costEnabled();
        this.costCurrency = crate.costCurrency();
        this.costAmount = crate.costAmount();
        this.cooldownSeconds = Math.max(0L, crate.cooldownMillis() / 1000L);
        this.inv = Bukkit.createInventory(this, 54, Component.text("Edit: " + crate.id()));
        // particles snapshot
        this.particlesEnabled = crate.particlesEnabled();
        this.particleShape = crate.particleShape();
        this.particleType = crate.particleType();
        this.particleRadius = crate.particleRadius();
        this.particleYOffset = crate.particleYOffset();
        this.particlePoints = crate.particlePoints();

        // hologram snapshot
        this.holoEnabled = crate.hologramEnabled();
        this.holoYOffset = crate.hologramYOffset();
        this.holoLines = new ArrayList<>(crate.holoLines());

        // animation snapshot
        this.animType = crate.animationType();
        this.rouletteCycles = crate.rouletteCycles();
        this.rouletteSpeed = crate.rouletteSpeedTicks();
        this.revealFlickers = crate.revealFlickers();
        this.revealSpeed = crate.revealSpeedTicks();
        this.cascadeSpeed = crate.cascadeSpeedTicks();
        this.closeDelay = crate.closeDelayTicks();

        // key display override
        this.keyDisplayOverrideEnabled = crate.keyDisplayOverride() != null;
        this.keyDisplayOverride = crate.keyDisplayOverride();

        // crate meta snapshot
        this.crateType = crate.type();
        this.crateDisplay = crate.displayName();
        build(crate);
    }

    private void build(Crate crate) {
        inv.clear();
        fillBackground();
        rebuildTop();
        // Load rewards into editable list
        rewards.clear();
        for (Reward r : crate.rewards()) {
            ItemStack it = r.displayItem() != null ? r.displayItem().clone()
                    : (r.items().isEmpty() ? new ItemStack(Material.CHEST) : r.items().get(0).clone());
            boolean cmdOnly = r.items() == null || r.items().isEmpty();
            EditedReward er = new EditedReward(it, r.weight(), r.rarity(), r.announce(), cmdOnly, r.displayName());
            // Load item list
            for (ItemStack ri : r.items()) if (ri != null && !ri.getType().isAir()) er.items.add(ri.clone());
            // Load commands/message/money/xp
            er.commands.addAll(r.commands());
            er.message = r.message();
            er.money = r.money();
            er.xpLevels = r.expLevels();
            rewards.add(er);
        }
        redrawRewards();
        // Controls bottom row: add/save/close
        inv.setItem(45, item(Material.LIME_CONCRETE, "<green>Add Reward</green>", List.of("<gray>Click:</gray> <white>add new reward</white>")));
        inv.setItem(53, item(Material.EMERALD_BLOCK, "<green>Save</green>", List.of("<gray>Click:</gray> <white>save changes</white>")));
        inv.setItem(49, item(Material.BARRIER, "<red>Close</red>", List.of("<gray>Click:</gray> <white>close without saving</white>")));
    }

    private void rebuildTop() {
        // Slot 0: Key requirement / key id
        List<String> kLore = new ArrayList<>();
        kLore.add("<gray>Left:</gray> next key id");
        kLore.add("<gray>Right:</gray> toggle requires key");
        kLore.add("<gray>Shift-Left:</gray> create new key");
        String kName = requiresKey ? ("<yellow>Key:</yellow> <white>" + keyId + "</white>") : "<red>No key required</red>";
        inv.setItem(0, item(requiresKey ? Material.TRIPWIRE_HOOK : Material.BARRIER, kName, kLore));

        // Slot 1: Cost controls
        List<String> cLore = new ArrayList<>();
        cLore.add("<gray>Left/Right:</gray> +/- amount");
        cLore.add("<gray>Shift:</gray> x10 step");
        cLore.add("<gray>Middle:</gray> cycle currency");
        String cName = costEnabled ? ("<yellow>Cost:</yellow> <white>" + String.format(java.util.Locale.US, "%.2f", costAmount) + "</white> <gray>" + costCurrency.name() + "</gray>") : "<red>No cost</red>";
        inv.setItem(1, item(costEnabled ? Material.GOLD_INGOT : Material.BARRIER, cName, cLore));

        // Slot 2: Particles
        List<String> pLore = new ArrayList<>();
        pLore.add("<gray>Left:</gray> cycle type");
        pLore.add("<gray>Middle:</gray> cycle shape");
        pLore.add("<gray>Shift-Left/Right:</gray> radius +/- 0.2");
        String pName = particlesEnabled ? ("<yellow>Particles:</yellow> <white>" + particleType + "</white> <gray>shape=" + particleShape + ", r=" + String.format(java.util.Locale.US, "%.1f", particleRadius) + "</gray>") : "<red>Particles disabled</red>";
        inv.setItem(2, item(particlesEnabled ? Material.BLAZE_POWDER : Material.GRAY_DYE, pName, pLore));

        // Slot 3: Cooldown
        List<String> cdLore = List.of("<gray>Left/Right:</gray> +/- 1s", "<gray>Shift:</gray> x10 step");
        inv.setItem(3, item(Material.CLOCK, "<yellow>Cooldown:</yellow> <white>" + cooldownSeconds + "s</white>", cdLore));

        // Slot 4: Hologram
        List<String> hLore = new ArrayList<>();
        hLore.add("<gray>Left/Right:</gray> y-offset +/- 0.1");
        hLore.add("<gray>Middle:</gray> toggle enabled");
        hLore.add("<gray>Ctrl+Drop:</gray> edit lines (MiniMessage)");
        hLore.add("<gray>Lines:</gray> <white>" + (holoLines == null ? 0 : holoLines.size()) + "</white>");
        String hName = holoEnabled ? ("<yellow>Hologram:</yellow> <white>enabled</white> <gray>y=" + String.format(java.util.Locale.US, "%.1f", holoYOffset) + "</gray>") : "<red>Hologram disabled</red>";
        inv.setItem(4, item(holoEnabled ? Material.NAME_TAG : Material.GRAY_DYE, hName, hLore));

        // Slot 7: Crate meta (display + type)
        List<String> mLore = new ArrayList<>();
        mLore.add("<gray>Left:</gray> edit display (MiniMessage)");
        mLore.add("<gray>Right:</gray> clear to id");
        mLore.add("<gray>Middle:</gray> cycle type");
        String mName = "<yellow>Crate:</yellow> <white>" + (crateDisplay == null || crateDisplay.isBlank() ? crateId : crateDisplay) + "</white> <gray>(" + crateType + ")</gray>";
        inv.setItem(7, item(Material.WRITABLE_BOOK, mName, mLore));

        // Slot 8: Hologram refresh
        inv.setItem(8, item(Material.GLOWSTONE_DUST, "<yellow>Refresh Holograms</yellow>", List.of("<gray>Click:</gray> refresh crate holograms")));

        // Slot 5: Animation
        List<String> aLore = new ArrayList<>();
        aLore.add("<gray>Left:</gray> cycle type");
        aLore.add("<gray>Shift-Left:</gray> adjust speed");
        aLore.add("<gray>Shift-Right:</gray> adjust cycles/flickers");
        aLore.add("<gray>Drop:</gray> close delay +5");
        String detail;
        switch (animType) {
            case REVEAL -> detail = "flickers=" + revealFlickers + ", speed=" + revealSpeed;
            case CASCADE -> detail = "speed=" + cascadeSpeed;
            default -> detail = "cycles=" + rouletteCycles + ", speed=" + rouletteSpeed;
        }
        inv.setItem(5, item(Material.FIREWORK_ROCKET, "<yellow>Animation:</yellow> <white>" + animType + "</white> <gray>(" + detail + ", close=" + closeDelay + ")</gray>", aLore));

        // Slot 6: Key display override
        List<String> kdLore = new ArrayList<>();
        kdLore.add("<gray>Middle:</gray> toggle override");
        kdLore.add("<gray>Left:</gray> set default '<crate> Key'");
        kdLore.add("<gray>Right:</gray> clear override");
        String kdName = keyDisplayOverrideEnabled ? ("<yellow>Key Display:</yellow> <white>" + (keyDisplayOverride == null ? "" : keyDisplayOverride) + "</white>") : "<gray>Using keys.yml display</gray>";
        inv.setItem(6, item(Material.PAPER, kdName, kdLore));
    }

    public void open(Player p) { p.openInventory(inv); }
    @Override public Inventory getInventory() { return inv; }

    public void handleClick(InventoryClickEvent e) {
        int slot = e.getRawSlot();
        if (slot == 0) {
            e.setCancelled(true);
            if (e.isRightClick()) {
                requiresKey = !requiresKey;
            } else if (e.isLeftClick() && requiresKey) {
                List<String> ids = new ArrayList<>(service.keyIds());
                if (!ids.isEmpty()) {
                    int idx = ids.indexOf(keyId);
                    idx = (idx + 1) % ids.size();
                    keyId = ids.get(idx);
                }
            } else if (e.isShiftClick()) {
                newKeyId = "key_" + System.currentTimeMillis();
                keyId = newKeyId;
                requiresKey = true;
            }
            rebuildTop();
            return;
        }
        if (slot == 1) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.MIDDLE) {
                Crate.CostCurrency[] vals = Crate.CostCurrency.values();
                costCurrency = vals[(costCurrency.ordinal() + 1) % vals.length];
                costEnabled = true;
            } else if (e.isLeftClick()) {
                costAmount += e.isShiftClick() ? 10 : 1;
                costEnabled = true;
            } else if (e.isRightClick()) {
                costAmount -= e.isShiftClick() ? 10 : 1;
                if (costAmount <= 0) { costAmount = 0; costEnabled = false; }
            }
            rebuildTop();
            return;
        }
        if (slot == 2) {
            e.setCancelled(true);
            if (e.isLeftClick()) {
                String[] pool = new String[] { "FLAME", "END_ROD", "VILLAGER_HAPPY", "SPELL_WITCH", "HEART", "CRIT", "REDSTONE" };
                int idx = java.util.Arrays.asList(pool).indexOf(particleType.toUpperCase(java.util.Locale.ROOT));
                idx = (idx + 1) % pool.length;
                particleType = pool[idx];
                particlesEnabled = true;
            } else if (e.getClick() == ClickType.MIDDLE) {
                Crate.ParticleShape[] shapes = Crate.ParticleShape.values();
                particleShape = shapes[(particleShape.ordinal() + 1) % shapes.length];
                particlesEnabled = true;
            } else if (e.isRightClick()) {
                particlesEnabled = !particlesEnabled;
            }
            if (e.isShiftClick()) {
                if (e.isLeftClick()) particleRadius += 0.2; else if (e.isRightClick()) particleRadius -= 0.2;
                if (particleRadius < 0.5) particleRadius = 0.5;
                particlesEnabled = true;
            }
            rebuildTop();
            return;
        }
        if (slot == 3) {
            e.setCancelled(true);
            if (e.isLeftClick()) cooldownSeconds += e.isShiftClick() ? 10 : 1;
            else if (e.isRightClick()) cooldownSeconds = Math.max(0, cooldownSeconds - (e.isShiftClick() ? 10 : 1));
            rebuildTop();
            return;
        }
        if (slot == 4) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.MIDDLE) { holoEnabled = !holoEnabled; }
            else if (e.isLeftClick()) { holoYOffset += 0.1; }
            else if (e.isRightClick()) { holoYOffset -= 0.1; }
            else if (e.getClick() == ClickType.CONTROL_DROP) {
                boolean ok = BookEditManager.startHologramLines((Player)e.getWhoClicked(), this, holoLines);
                if (!ok) { startHologramLinesEdit((Player) e.getWhoClicked()); }
                return;
            }
            rebuildTop();
            return;
        }
        if (slot == 7) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.MIDDLE) {
                Crate.Type[] ts = Crate.Type.values();
                crateType = ts[(crateType.ordinal() + 1) % ts.length];
            } else if (e.isLeftClick()) {
                startCrateRename((Player) e.getWhoClicked());
            } else if (e.isRightClick()) {
                crateDisplay = null;
            }
            rebuildTop();
            return;
        }
        if (slot == 5) {
            e.setCancelled(true);
            if (e.isLeftClick() && !e.isShiftClick()) {
                Crate.AnimationType[] types = Crate.AnimationType.values();
                animType = types[(animType.ordinal() + 1) % types.length];
            } else if (e.isLeftClick() && e.isShiftClick()) {
                switch (animType) {
                    case REVEAL -> revealSpeed = Math.max(1, revealSpeed + 1);
                    case CASCADE -> cascadeSpeed = Math.max(1, cascadeSpeed + 1);
                    default -> rouletteSpeed = Math.max(1, rouletteSpeed + 1);
                }
            } else if (e.isRightClick() && e.isShiftClick()) {
                switch (animType) {
                    case REVEAL -> revealFlickers = Math.max(1, revealFlickers + 1);
                    case CASCADE -> cascadeSpeed = Math.max(1, cascadeSpeed - 1);
                    default -> rouletteCycles = Math.max(1, rouletteCycles + 1);
                }
            } else if (e.getClick() == ClickType.DROP) {
                closeDelay += 5;
            }
            rebuildTop();
            return;
        }
        if (slot == 6) {
            e.setCancelled(true);
            if (e.getClick() == ClickType.MIDDLE) {
                keyDisplayOverrideEnabled = !keyDisplayOverrideEnabled;
                if (!keyDisplayOverrideEnabled) keyDisplayOverride = null;
            } else if (e.isLeftClick()) {
                keyDisplayOverride = "<yellow>" + MineCrates.get().crates().crate(crateId).displayName() + "</yellow> Key";
                keyDisplayOverrideEnabled = true;
            } else if (e.isRightClick()) {
                keyDisplayOverride = null; keyDisplayOverrideEnabled = false;
            }
            rebuildTop();
            return;
        }
        if (slot == 8) {
            e.setCancelled(true);
            refreshHolograms();
            Messages.msg(e.getWhoClicked(), "<green>Holograms refreshed.</green>");
            return;
        }
        if (slot == 45) { // add reward
            e.setCancelled(true);
            rewards.add(new EditedReward(new ItemStack(Material.CHEST), 1.0, Reward.Rarity.COMMON, false, false, null));
            redrawRewards();
            return;
        }
        if (slot == 53) { // save
            e.setCancelled(true);
            save((Player) e.getWhoClicked());
            return;
        }
        if (slot == 49) { // close without save
            e.setCancelled(true);
            e.getWhoClicked().closeInventory();
            return;
        }
        if (slot >= REWARD_START && slot < REWARD_END) {
            e.setCancelled(true);
            int idx = slot - REWARD_START;
            if (idx < 0 || idx >= rewards.size()) return;
            EditedReward er = rewards.get(idx);
            if (e.getClick() == ClickType.MIDDLE) {
                // cycle rarity
                Reward.Rarity[] rs = Reward.Rarity.values();
                er.rarity = rs[(er.rarity.ordinal() + 1) % rs.length];
            } else if (e.getClick() == ClickType.SWAP_OFFHAND) {
                // toggle command-only (items disabled)
                er.commandOnly = !er.commandOnly;
            } else if (e.getClick() == ClickType.CONTROL_DROP) {
                // start chat-based rename with MiniMessage support
                startRename((Player) e.getWhoClicked(), idx);
            } else if (!e.isShiftClick() && e.isRightClick() && (e.getCursor() == null || e.getCursor().getType() == Material.AIR)) {
                // Open detailed editor
                new RewardDetailGUI(this, idx).open((Player) e.getWhoClicked());
            } else if (e.isLeftClick() && e.getCursor() != null && e.getCursor().getType() != Material.AIR) {
                // set icon from cursor
                er.icon = e.getCursor().clone();
            } else if (e.isLeftClick()) {
                er.weight += e.isShiftClick() ? 10.0 : 1.0;
            } else if (e.isRightClick()) {
                er.weight = Math.max(0.0001, er.weight - (e.isShiftClick() ? 10.0 : 1.0));
            } else if (e.getClick() == ClickType.DROP) {
                rewards.remove(idx);
            }
            redrawRewards();
            return;
        }
        e.setCancelled(true);
    }

    public void handleClose(InventoryCloseEvent e) {
        save((Player) e.getPlayer());
    }

    private void save(Player p) {
        try {
            File f = new File(plugin.getDataFolder(), "crates.yml");
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            String base = "crates." + crateId;
            y.set(base + ".requires-key", requiresKey);
            if (requiresKey) y.set(base + ".key", keyId); else y.set(base + ".key", null);
            y.set(base + ".display", (crateDisplay == null || crateDisplay.isBlank()) ? null : crateDisplay);
            y.set(base + ".type", crateType.name());
            y.set(base + ".cost.enabled", costEnabled);
            y.set(base + ".cost.currency", costCurrency.name());
            y.set(base + ".cost.amount", costAmount);
            y.set(base + ".cooldown-seconds", cooldownSeconds);
            y.set(base + ".rewards", null);
            int idx = 1;
            for (EditedReward er : rewards) {
                String rPath = base + ".rewards.reward" + idx;
                y.set(rPath + ".weight", er.weight);
                y.set(rPath + ".rarity", er.rarity.name());
                y.set(rPath + ".announce", er.announce);
                // Reward message, money, xp
                y.set(rPath + ".message", (er.message == null || er.message.isBlank()) ? null : er.message);
                y.set(rPath + ".money", Math.max(0D, er.money));
                y.set(rPath + ".xp-levels", Math.max(0, er.xpLevels));
                // Save separate display item used for GUI/animation
                y.set(rPath + ".display-item.material", er.icon.getType().name());
                y.set(rPath + ".display-item.amount", Math.max(1, er.icon.getAmount()));
                // Save reward display name from editor field (MiniMessage)
                if (er.display != null && ! er.display.isBlank()) {
                    y.set(rPath + ".display", er.display);
                }
                // Commands
                if (er.commands == null || er.commands.isEmpty()) y.set(rPath + ".commands", null);
                else y.set(rPath + ".commands", new java.util.ArrayList<>(er.commands));

                // Items: only if not command-only
                if (!er.commandOnly) {
                    // write list
                    if (er.items == null || er.items.isEmpty()) {
                        y.set(rPath + ".items", null);
                    } else {
                        // clear and write i1..iN
                        y.set(rPath + ".items", null);
                        int i = 1;
                        for (ItemStack it : er.items) {
                            if (it == null || it.getType().isAir()) continue;
                            String ip = rPath + ".items.i" + i;
                            y.set(ip + ".material", it.getType().name());
                            y.set(ip + ".amount", Math.max(1, it.getAmount()));
                            i++;
                        }
                    }
                } else {
                    y.set(rPath + ".items", null);
                }
                idx++;
            }
            // particles
            y.set(base + ".particles.enabled", particlesEnabled);
            y.set(base + ".particles.shape", particleShape.name());
            y.set(base + ".particles.type", particleType);
            y.set(base + ".particles.radius", particleRadius);
            y.set(base + ".particles.y-offset", particleYOffset);
            y.set(base + ".particles.points", particlePoints);

            // hologram
            y.set(base + ".hologram.enabled", holoEnabled);
            y.set(base + ".hologram.y-offset", holoYOffset);
            if (holoLines == null || holoLines.isEmpty()) y.set(base + ".hologram.lines", null); else y.set(base + ".hologram.lines", new java.util.ArrayList<>(holoLines));

            // animation
            y.set(base + ".animation.type", animType.name());
            y.set(base + ".animation.roulette.speed-ticks", rouletteSpeed);
            y.set(base + ".animation.roulette.cycles", rouletteCycles);
            y.set(base + ".animation.reveal.speed-ticks", revealSpeed);
            y.set(base + ".animation.reveal.flickers", revealFlickers);
            y.set(base + ".animation.cascade.speed-ticks", cascadeSpeed);
            y.set(base + ".animation.close-delay-ticks", closeDelay);

            // key-display override
            if (keyDisplayOverrideEnabled && keyDisplayOverride != null && !keyDisplayOverride.isBlank())
                y.set(base + ".key-display", keyDisplayOverride);
            else y.set(base + ".key-display", null);
            y.save(f);

            if (newKeyId != null) {
                File kf = new File(plugin.getDataFolder(), "keys.yml");
                YamlConfiguration ky = YamlConfiguration.loadConfiguration(kf);
                ky.set("keys." + newKeyId + ".material", "TRIPWIRE_HOOK");
                ky.set("keys." + newKeyId + ".display", newKeyId);
                ky.save(kf);
            }

            java.util.concurrent.CompletableFuture<Void> fut = service.reloadAllAsync();
            fut.whenComplete((v, ex) -> {
                org.bukkit.Bukkit.getScheduler().runTask(MineCrates.get(), () -> {
                    try { refreshHolograms(); } catch (Throwable ignored) {}
                });
            });
            Messages.msg(p, "<green>Crate saved.</green>");
        } catch (Exception ex) {
            Messages.msg(p, "<red>Save failed:</red> " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MM.deserialize(name));
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String s : lore) lines.add(MM.deserialize(s));
            meta.lore(lines);
        }
        it.setItemMeta(meta);
        return it;
    }

    private void fillBackground() {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, "<gray> ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private void redrawRewards() {
        for (int i = REWARD_START; i < REWARD_END; i++) inv.setItem(i, null);
        int slot = REWARD_START;
        for (EditedReward er : rewards) {
            if (slot >= REWARD_END) break;
            ItemStack icon = er.icon.clone();
            ItemMeta meta = icon.getItemMeta();
            List<Component> lore = new ArrayList<>();
            String shown = (er.display == null ? "<gray>(default)</gray>" : er.display);
            lore.add(MM.deserialize("<gray>Name:</gray> " + shown));
            lore.add(MM.deserialize("<gray>Rarity:</gray> <white>" + er.rarity.name() + "</white>"));
            lore.add(MM.deserialize("<gray>Weight:</gray> <white>" + String.format(java.util.Locale.US, "%.1f", er.weight) + "</white>"));
            lore.add(MM.deserialize("<gray>Command-only:</gray> <white>" + (er.commandOnly ? "yes" : "no") + "</white>"));
            lore.add(MM.deserialize("<gray>Swap-Offhand:</gray> <white>toggle command-only</white>"));
            lore.add(MM.deserialize("<gray>Cursor+Left:</gray> <white>set display item</white>"));
            lore.add(MM.deserialize("<gray>Ctrl+Drop:</gray> <white>edit name (MiniMessage)</white>"));
            lore.add(MM.deserialize("<gray>Right (no shift):</gray> <white>open details</white>"));
            lore.add(MM.deserialize("<gray>Left/Right:</gray> <white>+/– weight</white>"));
            lore.add(MM.deserialize("<gray>Shift:</gray> <white>x10 step</white>"));
            lore.add(MM.deserialize("<gray>Middle:</gray> <white>cycle rarity</white>"));
            lore.add(MM.deserialize("<gray>Drop:</gray> <white>remove</white>"));
            meta.lore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
        }
    }

    private void refreshHolograms() {
        try {
            var holo = MineCrates.get().holograms();
            if (holo == null) return;
            for (var e : service.allBindings().entrySet()) {
                if (crateId.equalsIgnoreCase(e.getValue())) {
                    holo.upsert(e.getKey(), service.crate(crateId));
                }
            }
        } catch (Throwable ignored) {}
    }

    // Expose minimal helpers for detail GUI
    public EditedReward rewardAt(int index) { return (index < 0 || index >= rewards.size()) ? null : rewards.get(index); }
    public void refreshRewards() { redrawRewards(); }
    public void setHologramLines(java.util.List<String> lines) { this.holoLines = new java.util.ArrayList<>(lines == null ? java.util.List.of() : lines); }
    public void refreshTop() { rebuildTop(); }
}
