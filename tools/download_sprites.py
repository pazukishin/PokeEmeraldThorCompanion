import json
import pathlib
import urllib.request

BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites"
ROOT = pathlib.Path(r"C:\Repos\ThorCompanion")
PKMN_DIR = ROOT / "app/src/main/assets/pokemon"
ITEM_DIR = ROOT / "app/src/main/assets/items"


def download(url, dest):
    if dest.exists() and dest.stat().st_size > 0:
        return "skip"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "ThorCompanion"})
        with urllib.request.urlopen(req, timeout=20) as r:
            data = r.read()
        dest.write_bytes(data)
        return "ok" if len(data) > 0 else "empty"
    except Exception as e:  # noqa: BLE001
        return f"ERR {e}"


def main():
    PKMN_DIR.mkdir(parents=True, exist_ok=True)
    ITEM_DIR.mkdir(parents=True, exist_ok=True)

    ok = skip = err = 0
    for i in range(1, 387):
        r = download(f"{BASE}/pokemon/versions/generation-iii/emerald/{i}.png", PKMN_DIR / f"{i}.png")
        if r == "ok":
            ok += 1
        elif r == "skip":
            skip += 1
        else:
            err += 1
            print("pokemon", i, r)

    sprite_keys = set()
    catalog = json.loads((ROOT / "app/src/main/assets/emerald_map_content.json").read_text(encoding="utf-8"))
    for m in catalog["maps"].values():
        for it in m.get("items", []):
            sprite_keys.add(it.get("spriteKey", ""))
    # hardcoded fallback items from MapContentCatalog.kt
    sprite_keys.update(["potion", "oran-berry", "pecha-berry", "guard-spec", "pp-up", "cheri-berry", "leppa-berry"])
    sprite_keys.discard("")

    item_ok = item_err = 0
    for slug in sorted(sprite_keys):
        r = download(f"{BASE}/items/{slug}.png", ITEM_DIR / f"{slug}.png")
        if r in ("ok", "skip"):
            item_ok += 1
        else:
            item_err += 1
            print("item", slug, r)

    print(f"pokemon: {ok} ok, {skip} skip, {err} err | items: {item_ok} ok, {item_err} err")


if __name__ == "__main__":
    main()
