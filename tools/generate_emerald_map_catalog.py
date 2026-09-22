import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
OUTPUT = Path(r"C:\Repos\ThorCompanion\app\src\main\assets\emerald_map_content.json")
TRAINER_OUTPUT = Path(r"C:\Repos\ThorCompanion\app\src\main\assets\trainers")


def blocks(text):
    found = {}
    starts = list(re.finditer(r"(?m)^([A-Za-z0-9_]+)::\s*$", text))
    for index, match in enumerate(starts):
        end = starts[index + 1].start() if index + 1 < len(starts) else len(text)
        found[match.group(1)] = text[match.start():end]
    return found


def display_name(value):
    words = value.replace("_", " ").lower().split()
    return " ".join(word.capitalize() for word in words)


TM_NUMBERS = {}
HM_NUMBERS = {}


def _load_machines():
    text = (SOURCE / "include/constants/tms_hms.h").read_text(encoding="utf-8")

    def parse(macro):
        m = re.search(r"#define " + macro + r"\(F\) \\(.*?)\n\n", text, re.S)
        return {name: i + 1 for i, name in enumerate(re.findall(r"F\((\w+)\)", m.group(1)))} if m else {}

    TM_NUMBERS.update(parse("FOREACH_TM"))
    HM_NUMBERS.update(parse("FOREACH_HM"))


_load_machines()


def resolve_item(item_key):
    """Return (display name, sprite key) for a pokeemerald ITEM_* constant (without the ITEM_ prefix)."""
    if item_key.startswith("TM_") and item_key[3:] in TM_NUMBERS:
        return f"TM{TM_NUMBERS[item_key[3:]]:02d}", "tm-normal"
    if item_key.startswith("HM_") and item_key[3:] in HM_NUMBERS:
        return f"HM{HM_NUMBERS[item_key[3:]]:02d}", "hm-normal"
    if item_key == "X_DEFEND":
        return "X Defense", "x-defense"
    if item_key == "X_SPECIAL":
        return "X Special", "x-sp-atk"
    if item_key in ("ROOM_1_KEY", "ROOM_2_KEY", "ROOM_4_KEY", "ROOM_6_KEY"):
        return display_name(item_key), "basement-key"
    return display_name(item_key), item_key.lower().replace("_", "-")


def parse_trainers():
    trainers_text = (SOURCE / "src/data/trainers.h").read_text(encoding="utf-8")
    parties_text = (SOURCE / "src/data/trainer_parties.h").read_text(encoding="utf-8")
    party_blocks = {
        match.group(1): match.group(2)
        for match in re.finditer(
            r"static const struct [^{]+ (sParty_[A-Za-z0-9_]+)\[\]\s*=\s*\{(.*?)(?=\nstatic const struct |\Z)",
            parties_text,
            re.S,
        )
    }
    trainers = {}
    trainer_pattern = re.compile(r"\[([A-Z0-9_]+)\]\s*=\s*\{(.*?)(?=\n\s*\},\n\s*\n\s*\[|\Z)", re.S)
    for match in trainer_pattern.finditer(trainers_text):
        trainer_id, body = match.groups()
        if trainer_id == "TRAINER_NONE":
            continue
        party_match = re.search(r"party\s*=\s*(?:NO_ITEM_DEFAULT_MOVES|ITEM_DEFAULT_MOVES|CUSTOM_MOVES)\((sParty_[A-Za-z0-9_]+)", body)
        party_name = party_match.group(1) if party_match else None
        party = []
        if party_name and party_name in party_blocks:
            for level, species in re.findall(r"\.lvl\s*=\s*(\d+).*?\.species\s*=\s*SPECIES_([A-Z0-9_]+)", party_blocks[party_name], re.S):
                party.append({"name": display_name(species), "level": int(level)})
        name_match = re.search(r"\.trainerName\s*=\s*_\(\"([^\"]*)\"", body)
        class_match = re.search(r"\.trainerClass\s*=\s*TRAINER_CLASS_([A-Z0-9_]+)", body)
        pic_match = re.search(r"\.trainerPic\s*=\s*TRAINER_PIC_([A-Z0-9_]+)", body)
        trainers[trainer_id] = {
            "name": name_match.group(1).title() if name_match else display_name(trainer_id.replace("TRAINER_", "")),
            "className": display_name(class_match.group(1)) if class_match else "Entrenador",
            "pokemon": party,
            "spriteKey": pic_match.group(1).lower() if pic_match else "",
            "isSpecial": any(token in trainer_id for token in ("MAY", "BRENDAN", "RIVAL", "GYM", "ELITE", "CHAMPION", "STEVEN", "WALLACE"))
        }
    return trainers


def parse_trades():
    text = (SOURCE / "src/data/trade.h").read_text(encoding="utf-8")
    entries = {}
    pattern = re.compile(r"\[INGAME_TRADE_([A-Z0-9_]+)\]\s*=\s*\{(.*?)(?=\n\s*\},\n\s*\[|\n\s*\};)", re.S)
    for match in pattern.finditer(text):
        trade_key, body = match.groups()
        offered = re.search(r"\.species\s*=\s*SPECIES_([A-Z0-9_]+)", body)
        requested = re.search(r"\.requestedSpecies\s*=\s*SPECIES_([A-Z0-9_]+)", body)
        if offered and requested:
            entries[trade_key] = {"offered": display_name(offered.group(1)), "requested": display_name(requested.group(1))}
    return entries


def parse_script_blocks(script_text):
    result = blocks(script_text)
    # Labels with a single colon are movement/data labels and do not define event blocks.
    return result


def parse_map(map_file, trainers, trades):
    map_data = json.loads(map_file.read_text(encoding="utf-8"))
    scripts_file = map_file.parent / "scripts.inc"
    script_text = scripts_file.read_text(encoding="utf-8") if scripts_file.exists() else ""
    local_script_blocks = parse_script_blocks(script_text)
    # Resolve script labels from both the map's own scripts and the global scripts,
    # but only attribute trainers from this map's own scripts below. Global scripts
    # (e.g. Gabby & Ty's roaming battles) must never be attributed to every map.
    script_blocks = dict(local_script_blocks)
    global_scripts = SOURCE / "data/scripts"
    for global_file in global_scripts.glob("*.inc"):
        script_blocks.update(parse_script_blocks(global_file.read_text(encoding="utf-8")))
    items = []
    trainers_found = {}
    trades_found = []
    for trade_key in re.findall(r"INGAME_TRADE_([A-Z0-9_]+)", script_text):
        if trade_key in trades:
            trades_found.append(trades[trade_key])
    for event in map_data.get("object_events", []):
        script_name = event.get("script", "")
        block = script_blocks.get(script_name, "")
        item_matches = re.findall(r"\b(?:giveitem(?:_with_message)?|finditem)\s+ITEM_([A-Z0-9_]+)(?:\s*,\s*(\d+))?", block)
        for item, quantity in item_matches:
            item_name, sprite_key = resolve_item(item)
            items.append({
                "name": item_name,
                "quantity": int(quantity or 1),
                "spriteKey": sprite_key,
                "kind": "item_ball" if event.get("graphics_id") == "OBJ_EVENT_GFX_ITEM_BALL" else "npc_reward",
                "x": event.get("x"),
                "y": event.get("y"),
                "flag": event.get("flag", "0")
            })
        battle_ids = re.findall(r"\btrainerbattle_[A-Za-z0-9_]+\s+(TRAINER_[A-Z0-9_]+)", block)
        for trainer_id in battle_ids:
            if trainer_id in trainers:
                entry = dict(trainers[trainer_id])
                entry["trainerId"] = trainer_id
                entry["x"] = event.get("x")
                entry["y"] = event.get("y")
                trainers_found[trainer_id] = entry
    # Hidden items live in the map's background events, not in scripts.
    for bg_event in map_data.get("bg_events", []):
        if bg_event.get("type") == "hidden_item":
            item = bg_event.get("item", "")
            if item.startswith("ITEM_"):
                item_key = item[len("ITEM_"):]
                item_name, sprite_key = resolve_item(item_key)
                items.append({
                    "name": item_name,
                    "quantity": 1,
                    "spriteKey": sprite_key,
                    "kind": "hidden_item",
                    "x": bg_event.get("x"),
                    "y": bg_event.get("y"),
                    "flag": bg_event.get("flag", "0")
                })
    # Include scripted battles even when the NPC is hidden or trainer_type is NONE, such as the rival.
    # Only scan this map's own scripts so roaming/global battles are not duplicated across maps.
    for script_name, block in local_script_blocks.items():
        battle_ids = re.findall(r"\btrainerbattle_[A-Za-z0-9_]+\s+(TRAINER_[A-Z0-9_]+)", block)
        for trainer_id in battle_ids:
            if trainer_id in trainers and trainer_id not in trainers_found:
                entry = dict(trainers[trainer_id])
                entry["trainerId"] = trainer_id
                trainers_found[trainer_id] = entry
    return {
        "items": items,
        "trainers": list(trainers_found.values()),
        "trades": trades_found
    }


def main():
    trainers = parse_trainers()
    trades = parse_trades()
    maps = {}
    for map_file in (SOURCE / "data/maps").glob("*/map.json"):
        data = parse_map(map_file, trainers, trades)
        if data["items"] or data["trainers"] or data["trades"]:
            maps[json.loads(map_file.read_text(encoding="utf-8"))["id"]] = data
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    TRAINER_OUTPUT.mkdir(parents=True, exist_ok=True)
    source_trainer_pics = SOURCE / "graphics/trainers/front_pics"
    for image in source_trainer_pics.glob("*.png"):
        target = TRAINER_OUTPUT / image.name
        target.write_bytes(image.read_bytes())
    OUTPUT.write_text(json.dumps({"source": "pret/pokeemerald", "maps": maps}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generated {len(maps)} maps, {sum(len(value['items']) for value in maps.values())} items, {sum(len(value['trainers']) for value in maps.values())} trainers")


if __name__ == "__main__":
    main()
