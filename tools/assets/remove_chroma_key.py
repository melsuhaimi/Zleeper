#!/usr/bin/env python3
"""Extract generated sprite strips from a uniform chroma background."""

from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image, ImageStat


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    source = Image.open(args.input).convert("RGB")
    border = Image.new("RGB", (source.width * 2 + source.height * 2, 1))
    samples = (
        list(source.crop((0, 0, source.width, 1)).getdata())
        + list(source.crop((0, source.height - 1, source.width, source.height)).getdata())
        + list(source.crop((0, 0, 1, source.height)).getdata())
        + list(source.crop((source.width - 1, 0, source.width, source.height)).getdata())
    )
    border.putdata(samples)
    key = tuple(round(channel) for channel in ImageStat.Stat(border).median)

    result = Image.new("RGBA", source.size)
    output = []
    for red, green, blue in source.getdata():
        distance = math.sqrt(
            (red - key[0]) ** 2 + (green - key[1]) ** 2 + (blue - key[2]) ** 2
        )
        alpha = round(max(0.0, min(1.0, (distance - 14.0) / 52.0)) * 255)
        magenta_excess = min(red, blue) - green
        if magenta_excess > 6:
            alpha = round(alpha * max(0.0, min(1.0, (48 - magenta_excess) / 42)))
        if alpha < 255:
            # Reduce magenta spill while retaining warm gold and cream edges.
            spill = max(0, magenta_excess)
            red = max(0, red - round(spill * (1 - alpha / 255) * 0.55))
            blue = max(0, blue - round(spill * (1 - alpha / 255) * 0.55))
        output.append((red, green, blue, alpha))
    result.putdata(output)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    result.save(args.output)


if __name__ == "__main__":
    main()
