import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
ASSETS = Path(r"C:\Repos\ThorCompanion\app\src\main\assets")


def title(name: str) -> str:
    return name.replace("_", " ").lower().title()


def resolve_gender_ratio(expr: str) -> int:
    expr = expr.strip()
    if expr == "MON_GENDERLESS":
        return 255
    if expr == "MON_FEMALE":
        return 254
    if expr == "MON_MALE":
        return 0
    m = re.match(r"PERCENT_FEMALE\(\s*(\d+(?:\.\d+)?)\s*\)", expr)
    if m:
        return min(254, int(float(m.group(1)) * 255 / 100))
    try:
        return int(expr)
    except ValueError:
        return 255


def parse_defines(path: str, prefix: str) -> dict:
    """Map NAME -> value for '#define PREFIX_NAME value' lines."""
    out = {}
    text = Path(SOURCE, path).read_text(encoding="utf-8")
    for m in re.finditer(rf"#define {prefix}_(\w+)\s+(\d+)", text):
        out[m.group(1)] = int(m.group(2))
    return out


def parse_enum(path: str, prefix: str) -> dict:
    """Parse 'enum { PREFIX_A, PREFIX_B, ... }' — value is the 1-based position."""
    text = Path(SOURCE, path).read_text(encoding="utf-8")
    # crude: find the enum block starting with the prefix, then collect identifiers
    out = {}
    for m in re.finditer(rf"\b{prefix}_(\w+)\b", text):
        name = m.group(1)
        if name not in out and name != "NONE":
            out[name] = len(out) + 1
    return out


def main() -> None:
    species = parse_defines("include/constants/species.h", "SPECIES")
    abilities = parse_defines("include/constants/abilities.h", "ABILITY")
    types = parse_defines("include/constants/pokemon.h", "TYPE")

    # National dex numbers (enum is 1-based: BULBASAUR = 1 ... DEOXYS = 386)
    pokedex = parse_enum("include/constants/pokedex.h", "NATIONAL_DEX")

    # species constant -> national dex number
    pokemon_c = (SOURCE / "src/pokemon.c").read_text(encoding="utf-8")
    species_to_national = {}
    for name in re.findall(r"SPECIES_TO_NATIONAL\((\w+)\)", pokemon_c):
        if name in species:
            national = pokedex.get(name)
            if national is None:
                # legacy OLD_UNOWN_* slots all map to Unown (201)
                national = pokedex.get("UNOWN", 201)
            species_to_national[species[name]] = national

    # species info (types + abilities) per species constant
    info_text = (SOURCE / "src/data/pokemon/species_info.h").read_text(encoding="utf-8")
    details = {}
    for m in re.finditer(r"\[(SPECIES_\w+)\]\s*=\s*(?:\{|\b)", info_text):
        sp_name = m.group(1)[len("SPECIES_"):]
        if sp_name not in species:
            continue
        block_start = m.end()
        # find the closing of this initializer: read until the next [SPECIES_ or end
        nxt = info_text.find("[SPECIES_", block_start)
        block = info_text[block_start:nxt if nxt != -1 else len(info_text)]
        tm = re.search(r"\.types\s*=\s*\{\s*TYPE_(\w+)\s*,\s*TYPE_(\w+)", block)
        am = re.search(r"\.abilities\s*=\s*\{\s*ABILITY_(\w+)\s*,\s*ABILITY_(\w+)", block)
        if not tm or not am:
            continue
        national = species_to_national.get(species[sp_name])
        if national is None:
            continue
        type_list = list(dict.fromkeys(t for t in (title(tm.group(1)), title(tm.group(2))) if t != "None"))
        ability_list = list(dict.fromkeys(a for a in (title(am.group(1)), title(am.group(2))) if a != "None"))
        gm = re.search(r"\.genderRatio\s*=\s*([^,\n]+)", block)
        cm = re.search(r"\.catchRate\s*=\s*(\d+)", block)
        gender_ratio = resolve_gender_ratio(gm.group(1)) if gm else 255
        catch_rate = int(cm.group(1)) if cm else 0
        details[national] = {
            "types": type_list,
            "abilities": ability_list,
            "genderRatio": gender_ratio,
            "catchRate": catch_rate,
        }

    ability_names = {v: title(k) for k, v in abilities.items() if v != 0}

    (ASSETS / "species_to_national.json").write_text(
        json.dumps(species_to_national, ensure_ascii=False), encoding="utf-8")
    (ASSETS / "species_details.json").write_text(
        json.dumps(details, ensure_ascii=False), encoding="utf-8")
    (ASSETS / "abilities.json").write_text(
        json.dumps(ability_names, ensure_ascii=False), encoding="utf-8")

    print(f"species_to_national: {len(species_to_national)} entries")
    print(f"species_details: {len(details)} species")
    print(f"abilities: {len(ability_names)} abilities")
    # sanity checks
    print("Treecko ->", species_to_national.get(species.get("TREECKO")), "(expect 252)")
    print("Deoxys ->", species_to_national.get(species.get("DEOXYS")), "(expect 386)")
    print("Bulbasaur ->", species_to_national.get(species.get("BULBASAUR")), "(expect 1)")


if __name__ == "__main__":
    main()
