import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
OUTPUT = Path(r"C:\Repos\ThorCompanion\app\src\main\java\com\thorcompanion\EmeraldMapCatalog.kt")

CONNECTORS = {"of", "and", "the"}


def camel_to_display(name: str) -> str:
    s = name.replace("_", " ")
    # "PetalburgCity" -> "Petalburg City"
    s = re.sub(r"(?<=[a-z])(?=[A-Z])", " ", s)
    # "Route101" -> "Route 101", "Down01" -> "Down 01" (keeps "B1F"/"1F" intact)
    s = re.sub(r"(?<=[a-z])(?=\d)", " ", s)
    words = s.split()
    words = [w if (i == 0 or w.lower() not in CONNECTORS) else w.lower() for i, w in enumerate(words)]
    s = " ".join(words)
    # Restore common abbreviations
    s = re.sub(r"\bMt\b", "Mt.", s)
    s = re.sub(r"\bMr\b", "Mr.", s)
    s = re.sub(r"\bSSTidal\b", "S.S. Tidal", s)
    return s


def main() -> None:
    groups = json.loads((SOURCE / "data/maps/map_groups.json").read_text(encoding="utf-8"))
    group_order = groups["group_order"]

    lines = []
    for group_num, group_name in enumerate(group_order):
        for number, folder in enumerate(groups[group_name]):
            canonical = json.loads((SOURCE / "data/maps" / folder / "map.json").read_text(encoding="utf-8"))["id"]
            display = camel_to_display(folder)
            lines.append(f'        add({group_num}, {number}, "{canonical}", "{display}")')

    header = '''package com.thorcompanion

/** Map group/number order from pokeemerald's data/maps/map_groups.json. */
object EmeraldMapCatalog {
    private val entries = mutableMapOf<Pair<Int, Int>, EmeraldMap>().apply {
'''

    footer = '''
    }

    private fun MutableMap<Pair<Int, Int>, EmeraldMap>.add(group: Int, number: Int, id: String, name: String) {
        put(group to number, EmeraldMap(id, displayName(name)))
    }

    private fun displayName(name: String): String = when {
        name.startsWith("Route ") -> name.replace("Route ", "Ruta ")
        name == "Petalburg City" -> "Ciudad Petalburg"
        name == "Slateport City" -> "Ciudad Portual"
        name == "Mauville City" -> "Ciudad Malvalona"
        name == "Rustboro City" -> "Ciudad Férrica"
        name == "Littleroot Town" -> "Pueblo Raíz"
        name == "Oldale Town" -> "Pueblo Escaso"
        name == "Dewford Town" -> "Pueblo Azuliza"
        name == "Lavaridge Town" -> "Pueblo Lavacalda"
        name == "Fallarbor Town" -> "Pueblo Pardal"
        name == "Verdanturf Town" -> "Pueblo Verdegal"
        name == "Pacifidlog Town" -> "Pueblo Oromar"
        else -> name
    }

    fun resolveMapId(mapGroup: Int, mapNumber: Int): EmeraldMap = entries[mapGroup to mapNumber]
        ?: EmeraldMap("UNKNOWN_GROUP_${mapGroup}_MAP_${mapNumber}", "Mapa desconocido ($mapGroup:$mapNumber)")

    fun fromMemoryValue(value: Int): EmeraldMap = resolveMapId((value ushr 8) and 0xFF, value and 0xFF)
}
'''

    OUTPUT.write_text(header + "\n".join(lines) + "\n" + footer, encoding="utf-8")
    print(f"Generated {len(group_order)} groups ({sum(len(groups[g]) for g in group_order)} maps) -> {OUTPUT}")


if __name__ == "__main__":
    main()
