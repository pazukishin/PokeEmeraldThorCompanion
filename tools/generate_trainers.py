import json
import re
from pathlib import Path

SOURCE = Path(r"C:\Repos\pokeemerald-data")
ASSETS = Path(r"C:\Repos\ThorCompanion\app\src\main\assets")


def main() -> None:
    opponents = (SOURCE / "include/constants/opponents.h").read_text(encoding="utf-8")
    trainer_ids = {m.group(1): int(m.group(2)) for m in re.finditer(r"#define (TRAINER_\w+)\s+(\d+)", opponents)}

    trainers_text = (SOURCE / "src/data/trainers.h").read_text(encoding="utf-8")
    trainers = {}
    for m in re.finditer(r"\[(TRAINER_\w+)\]\s*=\s*\{(.*?)(?=\n\s*\},\s*\n\s*\[|\Z)", trainers_text, re.S):
        trainer_id = m.group(1)
        if trainer_id not in trainer_ids:
            continue
        body = m.group(2)
        name_match = re.search(r"\.trainerName\s*=\s*_\(\"([^\"]*)\"", body)
        pic_match = re.search(r"\.trainerPic\s*=\s*TRAINER_PIC_([A-Z0-9_]+)", body)
        name = name_match.group(1).title() if name_match else trainer_id.replace("TRAINER_", "").replace("_", " ").title()
        sprite_key = pic_match.group(1).lower() if pic_match else ""
        trainers[trainer_ids[trainer_id]] = {"name": name, "spriteKey": sprite_key}

    (ASSETS / "trainers.json").write_text(json.dumps(trainers, ensure_ascii=False), encoding="utf-8")
    print(f"trainers: {len(trainers)}")
    print("sample:", {k: trainers[k] for k in list(trainers)[:5]})


if __name__ == "__main__":
    main()
