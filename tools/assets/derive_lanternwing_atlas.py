#!/usr/bin/env python3
"""Derive the production Lanternwing palette from the approved Glimmerling atlas.

Both forms deliberately share a motion rig so controls and animation timing stay
identical. Lanternwing gains a moonlit blue-green coat, brighter gold markings,
and a restrained lantern bloom while preserving the hand-painted silhouettes.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageFilter


def transform(source: Image.Image) -> Image.Image:
    image = source.convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue
            # Preserve warm lantern paint while evolving foliage hues toward
            # deeper blue-green and increasing form readability at night.
            if red > green * 1.08 and red > blue * 1.35:
                pixels[x, y] = (
                    min(255, int(red * 1.10 + 12)),
                    min(255, int(green * 1.04 + 8)),
                    max(22, int(blue * .78)),
                    alpha,
                )
            else:
                luminance = (red + green + blue) / 3
                pixels[x, y] = (
                    max(18, int(red * .55 + luminance * .12)),
                    min(235, int(green * .72 + 26)),
                    min(255, int(blue * 1.05 + 42)),
                    alpha,
                )

    alpha = image.getchannel("A")
    glow_alpha = alpha.filter(ImageFilter.GaussianBlur(5)).point(lambda value: int(value * .12))
    glow = Image.new("RGBA", image.size, (112, 220, 213, 0))
    glow.putalpha(glow_alpha)
    return Image.alpha_composite(glow, image)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    arguments = parser.parse_args()
    with Image.open(arguments.source) as source:
        output = transform(source)
    arguments.destination.parent.mkdir(parents=True, exist_ok=True)
    output.save(arguments.destination, "PNG", optimize=True)


if __name__ == "__main__":
    main()
