import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
ASSETS = Path(r"C:\Repos\ThorCompanion\app\src\main\assets")


def parse_dex_numbers():
    text = (SOURCE / "include/constants/pokedex.h").read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r"\bNATIONAL_DEX_(\w+)\b", text):
        name = m.group(1)
        if name not in out and name != "NONE":
            out[name] = len(out) + 1
    return out


def main():
    dex = parse_dex_numbers()

    entries = (SOURCE / "src/data/pokemon/pokedex_entries.h").read_text(encoding="utf-8")
    # [NATIONAL_DEX_X] = { ... .description = gYyyPokedexText, ... }
    symbol_by_dex = {}
    for block in re.finditer(r"\[(NATIONAL_DEX_\w+)\]\s*=\s*\{(.*?)(?=\n\s*\[NATIONAL_DEX_|\n\s*\};)", entries, re.S):
        dex_name, body = block.groups()
        number = dex.get(dex_name[13:])  # strip "NATIONAL_DEX_"
        if number is None:
            continue
        m = re.search(r"\.description\s*=\s*(\w+)", body)
        if m:
            symbol_by_dex[number] = m.group(1)

    texts = (SOURCE / "src/data/pokemon/pokedex_text.h").read_text(encoding="utf-8")
    desc_by_symbol = {}
    for m in re.finditer(r"const u8 (g\w+PokedexText)\[\]\s*=\s*_\((.*?)\)\s*;", texts, re.S):
        symbol, body = m.groups()
        parts = re.findall(r'"([^"]*)"', body)
        desc = "".join(parts)
        desc = desc.replace("\\n", " ").replace("\\t", " ").replace("\\\"", '"')
        desc_by_symbol[symbol] = desc.strip()

    flavor = {}
    for number, symbol in symbol_by_dex.items():
        desc = desc_by_symbol.get(symbol)
        if desc and desc != "This is a newly discovered POKéMON.":
            flavor[number] = desc

    (ASSETS / "emerald_flavor_text.json").write_text(json.dumps(flavor, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"flavor text: {len(flavor)} species")
    print("bulbasaur:", flavor.get(1))


if __name__ == "__main__":
    main()
