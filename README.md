# Thor Companion

**Disclaimer**: This project was entirely developed with the assistance of AI.

Companion Android application for playing Pokemon Emerald on RetroArch from an Ayn Thor.

## Features

- Compact horizontal screen, designed for the 3.92-inch secondary display.
- UDP connection with a short timeout to avoid blocking the gameplay.
- Reads the game memory with `READ_CORE_MEMORY`.
- Current map and encounter list (species, levels, method and capture rate).
- Offline-first data: encounters, trainers, items (including hidden items), species names,
  Pokémon/item sprites, types, abilities and ability descriptions are bundled locally; PokéAPI
  is only a fallback for flavor text and evolution chains.
- Automatic detection of wild battles, trainer battles and the party summary screen.

### Live party / battle info

- Full-screen, opaque panels that capture taps, so the UI underneath is never triggered.
- Structured info: section headers, type badges with colours, stats in a 2×3 grid, IVs/EVs with
  perfect-31 values highlighted, nature (with stat modifiers), ability + its description,
  happiness and Hidden Power type.
- Evolution chains shown as a tree with the method (level, stone, trade, happiness, special
  form, …), including branching evolutions (e.g. Eevee, Wurmple). Only species obtainable in
  Emerald (national dex ≤ 386) are shown.
- Held item: shown in the info of team, wild and trainer Pokémon, and marked on the team grid
  with a small Poké Ball overlapped on the sprite's bottom-right corner.
- Team view shows current/total HP (colour-coded by health) and the classic status badges
  (`SLP`, `PSN`, `TOX`, `BRN`, `FRZ`, `PAR`) when a Pokémon is afflicted.
- Trainer battle: larger sprites/text and a yellow border around the active opponent.

## System Requirements

### Pokémon Emerald ROM
Configured for **Pokémon Emerald Version 1.0 (USA)**, the version used in RetroAchievements,
with the **V.GBA-Next** core.

### RetroArch
Enable `Settings > Network > Network Control Interface` (memory reading must be allowed) and
configure the UDP port used by the app.

## How to Run

Build from Android Studio with the `app` configuration (Android SDK 35), or install a prebuilt
APK from `releases/`. The device must be able to reach the Ayn Thor's IP address.

## Memory Reading

The app reads game memory via `READ_CORE_MEMORY`. All offsets are centralized in
`EmeraldAddresses.kt` so they can be adjusted without touching the UI.

**Map** — `RetroArchClient.kt` dereferences `gSaveBlock1Ptr` (`0x03005D8C`) to resolve the
ASLR-shifted save block, then reads the location at `pointer + 0x04` as two bytes (map group
and number). It falls back to the fixed `0x020322E4` if the pointer cannot be read. The profile
is isolated in `CoreProfiles` to add ROM revisions without touching the UI.

**Battle and party** — the app reads:

- `gBattleTypeFlags` (`0x02022FEC`) and `gBattleOutcome` (`0x0202433A`) to detect an active battle
  (flags set and no outcome recorded yet). `BATTLE_TYPE_TRAINER = 0x8` distinguishes trainer battles.
- `gBattleMons` (`0x02024084`, 4 × 88 bytes) for the current opponent; the enemy battler index is `1` in single battles.
- `gEnemyParty` (`0x02024744`, all 6 slots) for the trainer's team.
- `gPlayerParty` (`0x020244EC`, all 6 slots) for the team tab.
- `gTrainerBattleOpponent_A/B` (`0x02038BCA` / `0x02038BCC`) for the current trainer.

The 100-byte `struct Pokemon` substructs are decrypted with the `personality ^ otId` XOR key and
re-ordered by `personality % 24` (see `PokemonDecoder.kt`). The in-RAM `species` value is
pokeemerald's internal id (e.g. Treecko = 277), mapped to the national-dex number (252) via
`species_to_national.json`. The `status` field (offset `0x50`) is decoded for the status badges,
and `personality` is used to identify the active battler in trainer battles.

## Offline Data and Generators

Bundled assets are generated from `C:\Repos\pokeemerald-data` (pret/pokeemerald) with the scripts
in `tools/`:

- `generate_map_catalog.py` — regenerates `EmeraldMapCatalog.kt` (34 map groups, 518 maps).
- `generate_emerald_map_catalog.py` — regenerates `emerald_map_content.json` (trainers, items, trades and hidden items).
- `generate_species_details.py` — generates `species_to_national.json`, `species_details.json` (types, abilities, gender ratio and catch rate) and `abilities.json`.
- `generate_ability_descriptions.py` — generates `ability_descriptions.json` (ability name → in-game description).
- `generate_item_move_data.py` — generates `items.json` (item names) and `moves.json` (move names).
- `generate_trainers.py` — generates `trainers.json` (trainer id → name and sprite).
- `download_sprites.py` — downloads the 386 Pokémon sprites and the item sprites into `assets/pokemon/` and `assets/items/`.
- `generate_icon.py` — generates the launcher icon (Poké Ball on an emerald gradient) as PNG mipmaps.

## Changelog

- **1.2.6** — Status condition badges (`SLP`/`PSN`/`TOX`/`BRN`/`FRZ`/`PAR`) next to HP in the
  team grid; the Poké Ball held-item indicator is now overlapped on the sprite's corner.
- **1.2.5** — Fixed held-item name lookup (an off-by-one in `items.json`); added the Poké Ball
  held-item indicator; yellow border on the active trainer Pokémon; evolutions filtered to
  Emerald-only species.
- **1.2.4** — Evolution tree with correct ordering and branching; IVs/EVs in 2 rows × 3 columns;
  evolution info in team Pokémon; held item shown for team/wild/trainer; HP shown in the team grid.
- **1.2.3** — Opaque full-screen panels that capture taps; beautified info panels (type badges,
  sections, stat grid); larger sprites/text in trainer battles.
- **1.2.2** — Ability descriptions (offline).
- **1.2.1** — Homogeneous full-screen detail panels; trainer team read from all 6 slots; team tap fixed.

## Known Issues

- **Some item sprites are missing (TMs/HMs, keys, X Defend/X Special)**: pokeemerald names TMs by
  move (`ITEM_TM_ATTRACT`) while PokeAPI numbers them (`tm01`), so those sprites don't resolve
  and show a blank box. Will be fixed in a later version.

## Next Recommended Iteration

- Persist host and port settings and add a configuration screen to select the ROM profile.
- Flavor-text descriptions and evolution data still come from PokéAPI when online
  (types, abilities and ability descriptions are offline).
