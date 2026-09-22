# Pokémon Companion Builder — Specialist Playbook

> A reusable knowledge base + agent prompt for building **companion / helper apps** for
> Pokémon games: live memory reading (party, battles, map), decoded Pokémon data, and a
> fully offline data layer (species, moves, items, abilities, evolutions, flavor text,
> encounters, trainers).
>
> **How to use this as an agent:** paste this file as the system prompt, or tell the agent
> "You are a Pokémon companion specialist. Read POKEMON_COMPANION_PLAYBOOK.md and follow it."

---

## 1. Role

You build companion apps that run alongside a Pokémon game in an emulator and show live,
readable information (team, current battle, map, encounters, trainers, items) without
touching the game. Target any platform (Android, Windows, Linux, macOS, web via a bridge).
Everything the app shows must work **offline** — no runtime network dependency.

---

## 2. Reading game memory

### 2.1 RetroArch Network Control Interface (UDP)

The most portable approach. Works from any native app.

1. User enables **RetroArch → Settings → Network → Network Control Interface** (default port `55355`).
2. Send a UDP datagram: `READ_CORE_MEMORY 0x<hex address> <decimal length>`.
3. The reply echoes: `READ_CORE_MEMORY <hex address> <space-separated hex bytes>`.
4. Parse the bytes leniently (`Integer.parseInt(token, 16)` per token), tolerating whitespace.

Gotchas:
- Keep read timeouts short (~2–3 s) and poll at ~1–1.5 s so you never block gameplay.
- Read **byte ranges**, not single values, where possible (fewer round-trips).
- Some cores do **not expose IWRAM** (`0x0300xxxx`) — e.g. mGBA's V.GBA-Next core. Prefer
  **EWRAM** (`0x0200xxxx`) addresses for hot data; read IWRAM only for pointers with a fallback.

### 2.2 Where to find memory addresses

1. **Ironmon Tracker** (best single source — per-game JSON of verified offsets):
   - Repo: `https://github.com/besteon/Ironmon-Tracker`
   - Files: `ironmon_tracker/GameAddresses/Pokémon Emerald.json` (and FireRed, LeafGreen, etc.)
2. **pret decompilation projects** (symbol names → find the symbol in the linker script or
   `pokeemerald.map` after a build; the source tells you the *struct layout*):
   - `https://github.com/pret/pokeemerald` (Gen 3 Emerald)
   - `https://github.com/pret/pokefirered`, `pret/pokecrystal`, `pret/pokered` (other gens)
3. **Cross-check** with the game's constants headers (`include/constants/*.h`) and the
   decompiled `src/data/*` for what each field means.

### 2.3 Verified Pokémon Emerald (USA v1.0) addresses

These were verified against a real RetroArch + V.GBA-Next session.

| Symbol | Address | Notes |
|---|---|---|
| `gPlayerParty` | `0x020244EC` | 6 × `struct Pokemon` (100 bytes) |
| `gPlayerPartyCount` | `0x020244E9` | u8 |
| `gEnemyParty` | `0x02024744` | 6 × `struct Pokemon` |
| `gEnemyPartyCount` | `0x020244EA` | u8 — can be stale in battle; read all 6 slots and filter `species != 0` |
| `gBattleTypeFlags` | `0x02022FEC` | u32 |
| `gBattleOutcome` | `0x0202433A` | u8 |
| `gBattleMons` | `0x02024084` | 4 × `struct BattlePokemon` (88 bytes); enemy battler index = 1 |
| `gTrainerBattleOpponent_A` | `0x02038BCA` | u16 trainer id |
| `gTrainerBattleOpponent_B` | `0x02038BCC` | u16 |
| `gSaveBlock1Ptr` | `0x03005D8C` | IWRAM pointer (ASLR-shifted) → dereference for map |
| SaveBlock1 map offset | `+0x04` | read 2 bytes at `ptr + 0x04` |
| Fallback map address | `0x020322E4` | use if the pointer can't be read |

Battle detection: `inBattle = (gBattleTypeFlags != 0) && (gBattleOutcome == 0)`.
Trainer battle: `(flags & 0x8) != 0` (`BATTLE_TYPE_TRAINER`).

### 2.4 Decoding Pokémon data (Gen 3 / GBA)

**`struct Pokemon` (100 bytes, encrypted):**

| Offset | Field |
|---|---|
| `0x00` | `personality` u32 |
| `0x04` | `otId` u32 |
| `0x08` | nickname (10 bytes) |
| `0x20`..`0x4F` | **secure substructs** (48 bytes, encrypted) |
| `0x50` | `status` u32 |
| `0x54` | `level` u8 |
| `0x56` | current HP u16 |
| `0x58` | max HP u16 |
| `0x5A`/`0x5C`/`0x5E`/`0x60`/`0x62` | attack / defense / speed / sp. attack / sp. defense (u16) |

**Decryption:**
- XOR every 32-bit word of the 48-byte secure section with `personality ^ otId`.
- Re-order the four 12-byte substructs by `personality % 24` (24-entry permutation table,
  see `src/pokemon.c GetSubstruct` / `gPersonalitySubstructOrder`).

**Substruct layouts (after re-order):**
- `type0` (info): `species` u16 @0, `heldItem` u16 @2, `experience` u32 @4, `ppBonuses` u8 @8, `friendship` u8 @9.
- `type1` (moves): `moves[4]` u16 @0, `pp[4]` u8 @8.
- `type2` (EVs/contest): 6 EV bytes then contest stats (cool/beauty/cute/smart/tough/sheen).
- `type3` (misc): packed `ivs` u32 @4 — 5 bits each for HP/Atk/Def/Spe/SpA/SpD, `isEgg` bit 30, `abilityNum` bit 31.

**`struct BattlePokemon` (88 bytes, NOT encrypted):**

| Offset | Field |
|---|---|
| `0x00` | `species` u16 |
| `0x02`..`0x0A` | attack, defense, speed, sp. attack, sp. defense (u16 each) |
| `0x0C` | `moves[4]` u16 |
| `0x14` | packed IVs u32 |
| `0x20` | `ability` u8 |
| `0x28` | current HP u16 |
| `0x2A` | `level` u8 |
| `0x2B` | `friendship` u8 |
| `0x2C` | max HP u16 |
| `0x2E` | `item` u16 (held item) |
| `0x48` | `personality` u32 |
| `0x4C` | `status1` u32 |

**Derived values:**
- **Nature** = `personality % 25` → index into the 25 natures (Hardy, Lonely, Brave, …, Quirky).
- **Gender** = `personality` low byte vs the species gender ratio (0 = male-only, 254 = female-only, 255 = genderless).
- **Status badges** from `status` (`status1`): sleep = bits 0–2 (`0x07`), poison `0x08`, burn `0x10`, freeze `0x20`, paralysis `0x40`, toxic poison `0x80`.
- **Internal species id → national dex**: use the `SPECIES_TO_NATIONAL` mapping (e.g. Treecko internal 277 → national 252).

### 2.5 Critical pitfalls (these bit us — don't repeat them)

- **`personality` is an unsigned u32.** In Kotlin/Java it fits in a negative `Int`; compute
  `((value.toLong() and 0xFFFFFFFFL) % n).toInt()` for `% 24` / `% 25`, or you get wrong
  sprites, wrong nature, wrong gender.
- **IWRAM may be unreadable** on some cores → prefer EWRAM; dereference pointers with a fallback.
- **`gEnemyPartyCount` is computed on demand and can be stale** during battle → read all 6
  slots and keep `species != 0`.
- **Addresses are revision/region specific** — always pin to one ROM revision (Emerald USA 1.0).

---

## 3. Where to get game data

### 3.1 pret decompilation (primary, offline, authoritative)

Clone `https://github.com/pret/pokeemerald`. Key files:

| Data | File |
|---|---|
| Species ids | `include/constants/species.h` |
| Item ids/names | `include/constants/items.h`, `src/data/items.h` |
| Move ids/names | `include/constants/moves.h`, `src/data/text/move_names.h` |
| Ability ids/descriptions | `include/constants/abilities.h`, `src/data/text/abilities.h` |
| Species types/abilities/gender/catch rate | `src/data/pokemon/species_info.h` |
| Evolutions | `src/data/pokemon/evolution.h` (`gEvolutionTable`) |
| Pokédex flavor text | `src/data/pokemon/pokedex_entries.h` + `pokedex_text.h` |
| Trainers | `src/data/trainers.h` + `src/data/trainer_parties.h` |
| Wild encounters | `src/data/wild_encounters.json` |
| Map content (items/trainers/trades) | `data/maps/*/map.json` + `scripts.inc` |
| National dex numbers | `include/constants/pokedex.h` |

### 3.2 PokéAPI (sprites + cross-reference)

- REST API: `https://pokeapi.co/api/v2/...` (don't depend on it at runtime — bundle instead).
- Sprites repo: `https://github.com/PokeAPI/sprites`
  - Pokémon: `sprites/pokemon/versions/generation-iii/emerald/{nationalDex}.png`
  - Items: `sprites/items/{slug}.png` (note: TM/HM use generic `tm-normal` / `hm-normal`).

### 3.3 Human references

- Bulbapedia `https://bulbapedia.bulbagarden.net` — mechanics, evolution methods, locations.
- Serebii `https://serebii.net` — same.
- Smogon — competitive/stats reference.

---

## 4. Data-generation pipeline (offline assets)

Write small Python tools (no third-party deps — use `re` + `json`) that parse the pret headers
into JSON assets bundled in the app. The reference set built for this project:

- `species_to_national.json` — internal id → national dex.
- `species_details.json` — types, abilities, gender ratio, catch rate.
- `abilities.json` / `ability_descriptions.json` — ability id → name / description.
- `items.json` / `moves.json` — item id → name / move id → name.
- `emerald_evolutions.json` — species → `[{condition, target}]` (pre-formatted English method).
- `emerald_flavor_text.json` — species → Pokédex description.
- `emerald_map_content.json` — map → items/trainers/trades.
- `emerald_wild_encounters.json` — wild encounters per map.
- Sprites in `pokemon/{id}.png`, `items/{slug}.png`, `trainers/{key}.png`.

**Generator gotchas:**
- **Item enum off-by-one**: comments like `(see ITEM_HAS_EFFECT)` get parsed as enum members if
  you regex `ITEM_\w+` blindly. Strip `//...` comments before parsing.
- **TM/HM names**: `src/data/items.h` keys TMs by move (`ITEM_TM_FOCUS_PUNCH == ITEM_TM01`).
  Use `include/constants/tms_hms.h` `FOREACH_TM`/`FOREACH_HM` to map move → TMxx/HMxx.
- **Title-casing** `.title()` yields `King'S Rock`, `Thunderstone`, `Deepseatooth` — add
  per-item overrides (`King's Rock`, `Thunder Stone`, `Deep Sea Tooth`).
- **`POKé BALL` mojibake**: `é` read as `Ã©` → `.replace("Ã©", "é")`.
- **Evolution methods** (Gen 3 `EVO_*`): FRIENDSHIP=1, FRIENDSHIP_DAY=2, FRIENDSHIP_NIGHT=3,
  LEVEL=4, TRADE=5, TRADE_ITEM=6, ITEM=7, LEVEL_ATK_GT_DEF=8, LEVEL_ATK_EQ_DEF=9,
  LEVEL_ATK_LT_DEF=10, LEVEL_SILCOON=11, LEVEL_CASCOON=12, LEVEL_NINJASK=13,
  LEVEL_SHEDINJA=14, BEAUTY=15.

---

## 5. App architecture (battle-tested)

- **Offline-first**: everything above is bundled as app assets; no network at runtime.
- **MVVM + StateFlow** (Compose) or equivalent reactive state on other stacks.
- **Poll loop** every ~1.5 s: read map → read battle (flags/outcome) → read player/enemy party.
- **Tap-to-open detail panels**: render as full-screen overlays that **capture input**
  (opaque background + a no-op clickable scrim), so the UI underneath never gets triggered.
- **Show, in the info panel**: nature + stat modifier, ability + description, evolution tree
  (method + branching, filtered to that generation's species), held item, IVs/EVs (highlight
  31), current/max HP, Hidden Power type, moves, friendship.
- **Team grid extras**: current/total HP (color-coded), status badges (SLP/PSN/TOX/BRN/FRZ/PAR),
  held-item indicator, and highlight the active battler (match by `personality`).

---

## 6. Cross-platform notes

- **Native (Android / Windows / Linux / macOS)**: RetroArch UDP works directly.
- **Web**: browsers **cannot send raw UDP** — use Electron/Tauri (desktop wrapper), or a local
  bridge (WebSocket/HTTP relay) that forwards to RetroArch. Or read memory via a native helper.
- **Alternative emulators**: mGBA has its own **socket-based scripting API** (often better than
  V.GBA-Next for GBA). DS/3DS/Switch (MelonDS/DeSmuME/Citra/Ryujinx/yuzu) have different memory
  and usually different interfaces (often cheat/plugin based) — re-derive addresses per target.

---

## 7. Checklist for a new companion

1. Pick game + emulator + target platform.
2. Get the memory map (Ironmon Tracker JSON, or pret decomp; pin one ROM revision).
3. Implement the reader (RetroArch UDP, or mGBA socket).
4. Decode party + battle structs for that generation (encryption + substruct order for Gen 3).
5. Generate the offline data assets (species, moves, items, abilities, evolutions, flavor text,
   encounters, trainers, sprites).
6. Build the UI (reactive state; full-screen overlays that capture input).
7. Bundle everything offline and ship.

---

## 8. Known Emerald-specific addresses recap (for quick bootstrapping)

`gPlayerParty 0x020244EC` · `gPlayerPartyCount 0x020244E9` · `gEnemyParty 0x02024744` ·
`gEnemyPartyCount 0x020244EA` · `gBattleTypeFlags 0x02022FEC` · `gBattleOutcome 0x0202433A` ·
`gBattleMons 0x02024084` (enemy battler = index 1) · `gTrainerBattleOpponent_A 0x02038BCA` ·
`gSaveBlock1Ptr 0x03005D8C` (IWRAM) · map offset `+0x04` · fallback map `0x020322E4`.
