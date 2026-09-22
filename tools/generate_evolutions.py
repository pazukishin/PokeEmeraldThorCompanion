import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
ASSETS = Path(r"C:\Repos\ThorCompanion\app\src\main\assets")

ITEM_NAME_OVERRIDES = {
    96: "Thunder Stone",
    187: "King's Rock",
    192: "Deep Sea Tooth",
    193: "Deep Sea Scale",
}


def parse_defines(path, prefix):
    text = (SOURCE / path).read_text(encoding="utf-8")
    return {m.group(1): int(m.group(2)) for m in re.finditer(rf"#define {prefix}_(\w+)\s+(\d+)", text)}


def parse_items_enum(text):
    m = re.search(r"enum\s*\{", text)
    depth = 1
    i = m.end()
    while i < len(text) and depth > 0:
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
        i += 1
    body = re.sub(r"//[^\n]*", "", text[m.end():i - 1])
    out, idx = {}, 0
    for idm in re.finditer(r"(ITEM_\w+)", body):
        name = idm.group(1)
        if name not in out:
            out[name] = idx
            idx += 1
    return out


def condition(method, param, item_ids, item_names):
    def item_name(p):
        iid = item_ids.get(p)
        name = item_names.get(iid) if iid is not None else None
        return ITEM_NAME_OVERRIDES.get(iid, name or p.replace("ITEM_", "").replace("_", " ").title())

    if method == "EVO_FRIENDSHIP":
        return "High friendship"
    if method == "EVO_FRIENDSHIP_DAY":
        return "High friendship (day)"
    if method == "EVO_FRIENDSHIP_NIGHT":
        return "High friendship (night)"
    if method == "EVO_LEVEL":
        return f"Lv. {param}"
    if method == "EVO_TRADE":
        return "Trade"
    if method == "EVO_TRADE_ITEM":
        return f"Trade holding {item_name(param)}"
    if method == "EVO_ITEM":
        return f"Use {item_name(param)}"
    if method == "EVO_LEVEL_ATK_GT_DEF":
        return f"Lv. {param} (Atk > Def)"
    if method == "EVO_LEVEL_ATK_EQ_DEF":
        return f"Lv. {param} (Atk = Def)"
    if method == "EVO_LEVEL_ATK_LT_DEF":
        return f"Lv. {param} (Atk < Def)"
    if method == "EVO_LEVEL_SILCOON":
        return f"Lv. {param} (Silcoon personality)"
    if method == "EVO_LEVEL_CASCOON":
        return f"Lv. {param} (Cascoon personality)"
    if method == "EVO_LEVEL_NINJASK":
        return f"Lv. {param}"
    if method == "EVO_LEVEL_SHEDINJA":
        return f"Lv. {param} (Shedinja: empty slot + Poké Ball)"
    if method == "EVO_BEAUTY":
        return f"Beauty {param}+"
    return method.replace("EVO_", "").replace("_", " ").title()


def main():
    species = parse_defines("include/constants/species.h", "SPECIES")
    internal_to_national = json.loads((ASSETS / "species_to_national.json").read_text(encoding="utf-8"))
    internal_to_national = {int(k): v for k, v in internal_to_national.items()}

    item_ids = parse_items_enum((SOURCE / "include/constants/items.h").read_text(encoding="utf-8"))
    item_names = {int(k): v for k, v in json.loads((ASSETS / "items.json").read_text(encoding="utf-8")).items()}

    text = (SOURCE / "src/data/pokemon/evolution.h").read_text(encoding="utf-8")
    evolutions = {}
    for block in re.finditer(r"\[(SPECIES_\w+)\]\s*=\s*\{(.*?)(?=\n\s*\[SPECIES_|\n\};)", text, re.S):
        sp_name, body = block.groups()
        internal = species.get(sp_name.removeprefix("SPECIES_"))
        if internal is None:
            continue
        national = internal_to_national.get(internal)
        if national is None:
            continue
        branches = []
        for m in re.finditer(r"\{(\w+)\s*,\s*([^,}]+)\s*,\s*SPECIES_(\w+)\}", body):
            method, param, target = m.groups()
            target_internal = species.get(target)
            target_national = internal_to_national.get(target_internal) if target_internal is not None else None
            if target_national is None:
                continue
            param_value = param.strip()
            branches.append({"condition": condition(method, param_value, item_ids, item_names), "target": target_national})
        if branches:
            evolutions[national] = branches

    (ASSETS / "emerald_evolutions.json").write_text(json.dumps(evolutions, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"evolutions: {len(evolutions)} species")
    print("eevee:", evolutions.get(133))
    print("wurmple:", evolutions.get(265))


if __name__ == "__main__":
    main()
