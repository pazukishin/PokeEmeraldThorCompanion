import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
ASSETS = Path(r"C:\Repos\ThorCompanion\app\src\main\assets")


def title(name: str) -> str:
    return name.replace("_", " ").lower().title()


def main() -> None:
    # ABILITY_NAME -> id (from include/constants/abilities.h)
    constants = (SOURCE / "include/constants/abilities.h").read_text(encoding="utf-8")
    ability_ids = {
        m.group(1): int(m.group(2))
        for m in re.finditer(r"#define ABILITY_(\w+)\s+(\d+)", constants)
    }

    text = (SOURCE / "src/data/text/abilities.h").read_text(encoding="utf-8")

    # sXDescription[] = _("...") -> description symbol name -> text
    descriptions = {
        m.group(1): m.group(2)
        for m in re.finditer(r's(\w+Description)\[\]\s*=\s*_\("([^"]*)"\);', text)
    }

    # [ABILITY_NAME] = sXDescription,  -> ability name -> description symbol
    pointer_map = dict(re.findall(r'\[ABILITY_(\w+)\]\s*=\s*s(\w+),', text))

    out = {}
    for ability_name, ability_id in ability_ids.items():
        if ability_id == 0:
            continue  # ABILITY_NONE has no meaningful description
        symbol = pointer_map.get(ability_name)
        if symbol is None:
            continue
        description = descriptions.get(symbol)
        if description is None:
            continue
        out[title(ability_name).lower()] = description

    (ASSETS / "ability_descriptions.json").write_text(
        json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"ability_descriptions: {len(out)} abilities")
    print("overgrow ->", out.get("overgrow"))


if __name__ == "__main__":
    main()
