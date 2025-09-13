# MineCrates

A modern, MiniMessage‑powered crates plugin for Paper 1.20+ with an in‑game editor, animation previews, holograms, and PlaceholderAPI support.

## Features

- Crate types: BLOCK or VIRTUAL
- Per‑crate editor GUI
  - Rewards: weight, rarity, announce, message, commands, money, XP levels
  - Command‑only rewards (no items) toggle
  - Reward display name (MiniMessage) and separate display item (icon)
  - Multi‑item rewards (add/replace/remove)
  - Book editor integration for long text (message/commands/hologram lines)
  - Particles: shape, type, radius, Y‑offset, points
  - Animation: Roulette/Reveal/Cascade + speeds/cycles/close delay
  - Cooldown, key requirement/ID, cost (Vault/EXP/Levels)
  - Hologram: enable, Y‑offset, lines (MiniMessage)
  - Crate meta: display (MiniMessage) and type (BLOCK/VIRTUAL)
  - Live hologram refresh button
- Preview GUI with rarity filters and configurable navigation slots
- Open animation with configurable markers
- Permissions for type‑specific open and cooldown bypass
- PlaceholderAPI expansion

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
- `/minecrates reload` – reload configs and data

## Permissions

- `minecrates.use` – base permission (help), default: true
- `minecrates.preview` – allow `/minecrates preview`, default: true
- `minecrates.open` – allow opening per‑crate via `minecrates.open.<id>` gate
- `minecrates.open.block` – allow opening BLOCK crates
- `minecrates.open.virtual` – allow opening VIRTUAL crates
- `minecrates.givekey`, `minecrates.giveall`, `minecrates.set`, `minecrates.remove`, `minecrates.reload`, `minecrates.testroll` – admin actions
- `minecrates.bypass.cooldown` – bypass crate cooldowns
- `minecrates.bypass.cost` – bypass opening costs
- `minecrates.bypass.breakbound` – break bound blocks without unbinding

## PlaceholderAPI

When PlaceholderAPI is present the following placeholders are available:

- `%minecrates_keys_{key}%` – virtual key balance for the specified key ID
- `%minecrates_opened%` – total number of crates the player has opened
- `%minecrates_cooldown_{crate}%` – seconds remaining on that crate’s cooldown
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
  - `gui.preview.highlight-selected-filter` (boolean)
  - `gui.preview.items-per-page` (int)
  - `gui.preview.close-slot`, `prev-slot`, `next-slot` (int slots)
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
      legendary_sword:
        weight: 2
        rarity: LEGENDARY
        announce: true
        display: "<gold>Legendary Sword</gold>"
        display-item:
          material: NETHERITE_SWORD
          amount: 1
        message: "<gold>Legendary!</gold> <gray>You received a</gray> <yellow>Legendary Sword</yellow>"
        commands:
          - "broadcast <player> won a &6Legendary Sword&7 from &dMystic!"
      broadcast_only:
        weight: 10
        rarity: RARE
        display: "Shoutout"
        display-item:
          material: PAPER
          amount: 1
        # No items section -> command-only
        commands:
          - "broadcast &d<player> &7got a &bShoutout&7!"
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
  - Ctrl+Drop: rename reward (MiniMessage)
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
