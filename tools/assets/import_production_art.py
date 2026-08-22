#!/usr/bin/env python3
"""Slice approved production sheets into deterministic Android runtime assets."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


ITEMS = (
    "item_collectible_blue_spore_icon",
    "item_consumable_minttea_icon",
    "item_cosmetic_moon_ribbon_icon",
    "item_equipment_acorn_cap_icon",
    "item_equipment_bell_charm_icon",
    "item_equipment_leaf_pack_icon",
    "item_equipment_mushroom_crown_icon",
    "item_key_bramble_gate_icon",
    "item_material_bramblewood_icon",
    "item_material_cloudfluff_icon",
    "item_material_dewdrop_icon",
    "item_material_softmoss_icon",
    "item_material_starthread_icon",
    "item_quest_keepers_note_icon",
    "item_relic_cloudglass_icon",
    "item_relic_dawn_compass_icon",
)
NPCS = ("npc_keeper_orin_atlas", "npc_pip_atlas", "npc_mara_atlas")


def cell(image: Image.Image, columns: int, rows: int, column: int, row: int) -> Image.Image:
    left = round(image.width * column / columns)
    top = round(image.height * row / rows)
    right = round(image.width * (column + 1) / columns)
    bottom = round(image.height * (row + 1) / rows)
    return image.crop((left, top, right, bottom))


def save_webp(image: Image.Image, destination: Path, size: tuple[int, int]) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.convert("RGBA").resize(size, Image.Resampling.LANCZOS).save(
        destination,
        "WEBP",
        lossless=True,
        quality=100,
        method=6,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--items", type=Path, required=True)
    parser.add_argument("--npcs", type=Path, required=True)
    parser.add_argument("--asset-root", type=Path, required=True)
    arguments = parser.parse_args()

    with Image.open(arguments.items) as sheet:
        sheet = sheet.convert("RGBA")
        for index, name in enumerate(ITEMS):
            save_webp(
                cell(sheet, 4, 4, index % 4, index // 4),
                arguments.asset_root / "game/item/icon" / f"{name}.webp",
                (128, 128),
            )

    with Image.open(arguments.npcs) as sheet:
        sheet = sheet.convert("RGBA")
        for row, name in enumerate(NPCS):
            strip = cell(sheet, 1, 3, 0, row)
            save_webp(strip, arguments.asset_root / "game/atlas/npc" / f"{name}.webp", (384, 128))


if __name__ == "__main__":
    main()
