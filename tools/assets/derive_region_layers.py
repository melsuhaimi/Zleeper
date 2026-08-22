#!/usr/bin/env python3
"""Create painterly mid/near parallax masks from an approved region plate."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


def cover(image: Image.Image, width: int, height: int) -> Image.Image:
    ratio = max(width / image.width, height / image.height)
    resized = image.resize((round(image.width * ratio), round(image.height * ratio)), Image.Resampling.LANCZOS)
    left = (resized.width - width) // 2
    top = (resized.height - height) // 2
    return resized.crop((left, top, left + width, top + height)).convert("RGBA")


def masked_layer(plate: Image.Image, near: bool) -> Image.Image:
    grayscale = plate.convert("L")
    output = plate.copy()
    alpha = Image.new("L", plate.size, 0)
    values = alpha.load()
    light = grayscale.load()
    for y in range(plate.height):
        vertical = (y / plate.height - (.66 if near else .34)) / (.34 if near else .66)
        if vertical <= 0:
            continue
        for x in range(plate.width):
            edge = abs(x / plate.width - .5) * 2
            shape = max(vertical, edge * (.72 if near else .50))
            darkness = 1 - light[x, y] / 255
            values[x, y] = int(255 * min(1, shape) * (.20 + darkness * .58))
    alpha = alpha.filter(ImageFilter.GaussianBlur(2 if near else 5))
    output.putalpha(alpha)
    return output


def save(image: Image.Image, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, "WEBP", quality=88, method=6)


def crop_atlas(plate: Image.Image, boxes: list[tuple[int, int, int, int]]) -> Image.Image:
    atlas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    for index, box in enumerate(boxes):
        tile = plate.crop(box).resize((256, 256), Image.Resampling.LANCZOS)
        atlas.alpha_composite(tile, ((index % 2) * 256, (index // 2) * 256))
    return atlas


def glow_strip(accent: tuple[int, int, int]) -> Image.Image:
    strip = Image.new("RGBA", (512, 128), (0, 0, 0, 0))
    for frame in range(4):
        layer = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
        draw = ImageDraw.Draw(layer)
        radius = 16 + frame * 5
        for ring in range(radius, 2, -2):
            alpha = int(7 + (radius - ring) * 6)
            draw.ellipse((64 - ring, 64 - ring, 64 + ring, 64 + ring), fill=(*accent, min(120, alpha)))
        draw.ellipse((58, 58, 70, 70), fill=(255, 246, 190, 235))
        strip.alpha_composite(layer.filter(ImageFilter.GaussianBlur(max(0, 3 - frame // 2))), (frame * 128, 0))
    return strip


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination_prefix", type=Path)
    parser.add_argument("--expedition", type=Path)
    parser.add_argument("--accent", default="a9dfc2")
    arguments = parser.parse_args()
    with Image.open(arguments.source) as source:
        plate = cover(source, 1280, 720)
    save(masked_layer(plate, near=False), Path(f"{arguments.destination_prefix}_mid.webp"))
    near = masked_layer(plate, near=True)
    save(near, Path(f"{arguments.destination_prefix}_near.webp"))
    base = str(arguments.destination_prefix).removesuffix("_bg")
    save(plate.crop((0, 300, 1280, 720)).resize((512, 512), Image.Resampling.LANCZOS), Path(f"{base}_tiles.webp"))
    save(crop_atlas(plate, [(0, 80, 420, 500), (860, 80, 1280, 500), (0, 360, 520, 720), (760, 360, 1280, 720)]), Path(f"{base}_props_static.webp"))
    save(near.resize((1024, 512), Image.Resampling.LANCZOS), Path(f"{base}_foreground.webp"))
    save(plate.crop((70, 250, 520, 700)).resize((256, 256), Image.Resampling.LANCZOS), Path(f"{base}_interactables.webp"))
    save(plate.crop((760, 310, 1210, 720)).resize((256, 256), Image.Resampling.LANCZOS), Path(f"{base}_collectibles.webp"))
    accent = tuple(bytes.fromhex(arguments.accent))
    effect = glow_strip(accent)
    save(effect, Path(f"{base}_props_animated.webp"))
    save(effect.transpose(Image.Transpose.FLIP_LEFT_RIGHT), Path(f"{base}_vfx.webp"))
    if arguments.expedition:
        save(plate.resize((1024, 512), Image.Resampling.LANCZOS), arguments.expedition)


if __name__ == "__main__":
    main()
