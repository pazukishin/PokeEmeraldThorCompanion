import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
ASSETS = Path(r"C:\Repos\ThorCompanion\app\src\main\assets")


def title(name: str) -> str:
    return name.replace("_", " ").lower().title()


def parse_items_enum(text: str) -> dict:
    """ITEM_X -> 0-based enum value from include/constants/items.h."""
    m = re.search(r"enum\s*\{", text)
    if not m:
        return {}
    depth = 1
    i = m.end()
    while i < len(text) and depth > 0:
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
        i += 1
    body = text[m.end():i - 1]
    # Strip // comments: some contain ITEM_* references (e.g. ITEM_HAS_EFFECT) that
    # must not be counted as enum members.
    body = re.sub(r"//[^\n]*", "", body)
    out = {}
    idx = 0
    for idm in re.finditer(r"(ITEM_\w+)", body):
        name = idm.group(1)
        if name not in out:
            out[name] = idx
            idx += 1
    return out


def main() -> None:
    # --- moves ---
    moves_const = (SOURCE / "include/constants/moves.h").read_text(encoding="utf-8")
    move_ids = {m.group(1): int(m.group(2)) for m in re.finditer(r"#define MOVE_(\w+)\s+(\d+)", moves_const)}
    move_names = (SOURCE / "src/data/text/move_names.h").read_text(encoding="utf-8")
    moves = {}
    for name, name_str in re.findall(r"\[MOVE_(\w+)\]\s*=\s*_\(\"([^\"]*)\"", move_names):
        if name in move_ids and name_str != "-":
            moves[move_ids[name]] = name_str.title()

    # --- items ---
    items_const = (SOURCE / "include/constants/items.h").read_text(encoding="utf-8")
    item_ids = parse_items_enum(items_const)
    items_data = (SOURCE / "src/data/items.h").read_text(encoding="utf-8")
    items = {}
    for m in re.finditer(r"\[(ITEM_\w+)\]\s*=\s*\{", items_data):
        item_name = m.group(1)
        if item_name not in item_ids:
            continue
        block_start = m.end()
        nxt = items_data.find("[ITEM_", block_start)
        block = items_data[block_start:nxt if nxt != -1 else len(items_data)]
        nm = re.search(r"\.name\s*=\s*_\(\"([^\"]*)\"", block)
        if nm and nm.group(1) != "????????":
            items[item_ids[item_name]] = nm.group(1).title().replace("Ã©", "é")

    (ASSETS / "items.json").write_text(json.dumps(items, ensure_ascii=False), encoding="utf-8")
    (ASSETS / "moves.json").write_text(json.dumps(moves, ensure_ascii=False), encoding="utf-8")
    print(f"items: {len(items)} | moves: {len(moves)}")
    print("sample items:", {k: items.get(k) for k in list(items)[:5]})
    print("sample moves:", {k: moves.get(k) for k in list(moves)[:5]})


if __name__ == "__main__":
    main()
