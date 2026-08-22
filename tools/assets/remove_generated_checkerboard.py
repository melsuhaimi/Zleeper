#!/usr/bin/env python3
"""Convert a baked neutral checkerboard into a real alpha channel.

The image generator occasionally renders its transparency preview into the
pixels.  This repair is intentionally narrow: only bright, near-neutral
pixels are removed, preserving the warm cream and gold edges of Moonmoth art.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


def alpha_for(red: int, green: int, blue: int) -> int:
    low = min(red, green, blue)
    high = max(red, green, blue)
    chroma = high - low

    if low >= 238 and chroma <= 7:
        return 0
    if low >= 226 and chroma <= 5:
        return max(0, min(255, (238 - low) * 21))
    return 255


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    source = Image.open(args.input).convert("RGBA")
    repaired = Image.new("RGBA", source.size)
    repaired.putdata(
        [
            (red, green, blue, min(source_alpha, alpha_for(red, green, blue)))
            for red, green, blue, source_alpha in source.getdata()
        ]
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    repaired.save(args.output)


if __name__ == "__main__":
    main()
