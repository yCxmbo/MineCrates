# MineCrates

A modern, MiniMessage‑powered crates plugin for Paper 1.20+ with an in‑game editor, animation previews, holograms, and PlaceholderAPI support.

## Features

- Crate types: BLOCK or VIRTUAL
- Per‑crate editor GUI
  - Rewards: weight (the player's chance to win), announce, message, commands, money, XP levels
  - Command‑only rewards (no items) toggle
  - Reward display name (MiniMessage), applied to the separate display item (icon)
  - Multi‑item rewards (add/replace/remove)
  - Enchanted reward items: vanilla enchants (`enchants:`) and MineStealEnchants custom enchants (`mse-enchants:`)
  - Book editor integration for long text (message/commands/hologram lines)
  - Particles: shape, type, radius, Y‑offset, points
  - Animation: Roulette/Reveal/Cascade + speeds/cycles/close delay
  - Cooldown, key requirement/ID, cost (Vault/EXP/Levels)
  - Hologram: enable, Y‑offset, lines (MiniMessage)
  - Crate meta: display (MiniMessage) and type (BLOCK/VIRTUAL)
  - Live hologram refresh button
- Preview GUI with pagination and configurable navigation slots
- Open animation with configurable markers
- Configurable sounds per event (`sounds:` in `config.yml`)
- Pity / milestone system: guarantee a reward after a dry streak (per‑crate `pity:`)
- Persistent player data (virtual keys, cooldowns, opens, last reward, pity) that survives restarts
- Permissions for type‑specific open and cooldown bypass
- PlaceholderAPI expansion
- Optional anonymous bStats metrics

## Commands

- `/minecrates help` – show help
- `/minecrates editor` – open the in‑game editor
- `/minecrates list` – list loaded crates
- `/minecrates preview <crate>` – open the preview GUI
- `/minecrates open <crate> [player]` – open a crate for a player
- `/minecrates openmulti <crate> <n> [player]` – grant N rewards without animation
- `/minecrates givekey <player> <key> <amount> [virtual]` – give key(s)
- `/minecrates giveall <key> <amount> [virtual]` – give key(s) to all online
- `/minecrates set <crate>` – bind targeted block to crate
- `/minecrates remove` – unbind targeted block
- `/minecrates binds [crate]` – list bindings (optionally filtered by crate)
- `/minecrates testroll <crate> <n>` – sample reward distribution
- `/minecrates stats [player]` – show a player's opens, virtual keys, last reward and pity progress
- `/minecrates reload` – reload configs and data

## Permissions

- `minecrates.use` – base permission (help), default: true
- `minecrates.preview` – allow `/minecrates preview`, default: true
- `minecrates.open.<id>` – open a specific crate; **registered at load with default: true** so normal players can open crates out of the box (deny per crate to restrict)
- `minecrates.preview.<id>` – preview a specific crate; **registered at load with default: true** so normal players can preview crates out of the box (deny per crate to restrict)
- `minecrates.open.block` – allow opening BLOCK crates, default: true
- `minecrates.open.virtual` – allow opening VIRTUAL crates via command, default: op
- `minecrates.givekey`, `minecrates.giveall`, `minecrates.set`, `minecrates.remove`, `minecrates.reload`, `minecrates.testroll`, `minecrates.stats` – admin actions
- `minecrates.bypass.cooldown` – bypass crate cooldowns
- `minecrates.bypass.cost` – bypass opening costs
- `minecrates.bypass.breakbound` – break bound blocks without unbinding

## PlaceholderAPI

When PlaceholderAPI is present the following placeholders are available:

- `%minecrates_keys_{key}%` – virtual key balance for the specified key ID
- `%minecrates_opened%` – total number of crates the player has opened
- `%minecrates_cooldown_{crate}%` – seconds remaining on that crate’s cooldown
- `%minecrates_pity_{crate}%` – opens remaining until the crate’s guaranteed reward (blank if pity disabled)
- `%minecrates_last_reward%` – ID of the last reward the player received
- `%minecrates_crate_name_{id}%` – display name of the crate with the given ID
- `%minecrates_crate_display_{id}%` – alias of `crate_name`
- `%minecrates_chance_{crate}_{reward}%` – reward chance percent (two decimals)
- `%minecrates_key_name_{id}%` – display name of the key with the given ID

## Configuration

Global config (`config.yml`) highlights:

- Messages prefix and reload strings (MiniMessage)
- Animation defaults and markers
- Preview GUI:
  - `gui.preview.items-per-page` (int)
  - `gui.preview.close-slot`, `prev-slot`, `next-slot` (int slots)
- Particle shapes reference (`particles.shapes:`): RING, RING_SPIN, SPIRAL, DOUBLE_HELIX, COLUMN, STAR, HEART, VORTEX, SPHERE, WAVE, GALAXY, ATOM
- Rewards inventory overflow policy: `rewards.inventory-overflow-policy: drop|deny`
- Holograms refresh interval

Crates (`crates.yml`) example (excerpt):

```
crates:
  mystic:
    display: "<gradient:#8A2BE2:#00FFFF>Mystic Crate</gradient>"
    type: BLOCK
    requires-key: true
    key: mystic_key
    cooldown-seconds: 30
    hologram:
      enabled: true
      y-offset: 2.3
      lines:
        - "<gradient:#D84AFF:#A64DFF><b>Mystic Crate</b></gradient>"
        - "<light_purple>Left Click to Preview</light_purple>"
        - "<light_purple>Right Click to Open</light_purple>"
    animation:
      type: ROULETTE
    rewards:
      # 'weight' is the chance to win: a reward's chance = its weight / sum of all weights.
      legendary_sword:
        weight: 2
        announce: true
        display: "<gold>Legendary Sword</gold>"
        display-item:
          material: NETHERITE_SWORD
          amount: 1
          # Optional explicit display name for the icon (overrides 'display' on the icon).
          name: "<gold>Legendary Sword</gold>"
        message: "<gold>Legendary!</gold> <gray>You received a</gray> <yellow>Legendary Sword</yellow>"
        commands:
          - "broadcast <player> won a &6Legendary Sword&7 from &dMystic!"
      broadcast_only:
        weight: 10
        display: "Shoutout"
        display-item:
          material: PAPER
          amount: 1
        # No items section -> command-only
        commands:
          - "broadcast &d<player> &7got a &bShoutout&7!"
```

### Enchanted reward items

Any item under a reward's `items:` map can carry enchantments via two optional blocks:

- `enchants:` – vanilla Bukkit enchants, keyed by modern Minecraft name (`sharpness`,
  `protection`, `unbreaking`, `fire_aspect`, `thorns`, `mending`, …). Legacy Bukkit names
  (e.g. `DAMAGE_ALL`) also work. Levels may exceed the vanilla cap.
- `mse-enchants:` – [MineStealEnchants](https://www.spigotmc.org/) custom enchants, keyed by
  lowercase id (run `/mse list` for the full set). Each enchant only applies to its intended
  material/slot; a mismatched or unknown id is skipped with a console warning. Requires the
  MineStealEnchants plugin — without it, these entries are simply skipped.

```
    rewards:
      cursed_blade:
        weight: 5
        display: "Cursed Blade"
        display-item:
          material: NETHERITE_SWORD
        items:
          i1:
            material: NETHERITE_SWORD
            name: "<red>Cursed Blade</red>"
            enchants:            # vanilla
              sharpness: 5
              fire_aspect: 2
            mse-enchants:        # MineStealEnchants custom enchants (id: level)
              bleed: 2
              venom: 1
      enchanted_armor:
        weight: 10
        display: "Guardian's Chestplate"
        display-item:
          material: DIAMOND_CHESTPLATE
        items:
          i1:
            material: DIAMOND_CHESTPLATE
            enchants:
              protection: 4
              unbreaking: 3
              thorns: 2
```

Keys (`keys.yml`) example:

```
keys:
  mystic_key:
    material: TRIPWIRE_HOOK
    display: "<gradient:#FFB84D:#FFD480>Mystic Key</gradient>"
```

## Editor Tips

- In the crate editor:
  - Slot 7: edit crate display (MiniMessage), clear, cycle type
  - Slot 8: refresh holograms for this crate
  - Slot 4: toggle hologram, adjust Y, Ctrl+Drop to edit lines
- Reward tiles:
  - Swap‑Offhand: toggle command‑only
  - Cursor+Left: set display item (icon)
  - Middle or Ctrl+Drop: set the display item's name (MiniMessage)
  - Left/Right: adjust weight (the win chance); the tile shows the resulting chance %
  - Right (no shift): open Reward Details
- Reward Details:
  - Announce, Message, Commands, Money, XP
  - Middle‑click Commands to view/remove entries in‑GUI
  - Items area: add/replace/remove items; Shift+Drop clears all
  - Book editor is used for long text (falls back to chat if unavailable)

## Building

```
mvn -DskipTests package
```

Requires a modern JDK compatible with your Paper target (1.20+).

Build output: a single shaded jar at `target/minecrates-<version>.jar`.
