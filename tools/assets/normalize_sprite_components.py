#!/usr/bin/env python3
"""Normalize a generated sprite row by detecting complete character islands."""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("out_dir", type=Path)
    parser.add_argument("--frames", type=int, default=8)
    parser.add_argument("--frame-size", type=int, default=256)
    parser.add_argument("--content-size", type=int, default=220)
    parser.add_argument("--anchor", type=Path)
    args = parser.parse_args()

    source = Image.open(args.input).convert("RGBA")
    pixels = np.asarray(source)
    mask = pixels[:, :, 3] > 32
    labels, _ = ndimage.label(mask, structure=np.ones((3, 3), dtype=np.uint8))

    components: list[tuple[int, int, tuple[int, int, int, int]]] = []
    for label, slices in enumerate(ndimage.find_objects(labels), start=1):
        if slices is None:
            continue
        area = int((labels[slices] == label).sum())
        if area < 200:
            continue
        y_slice, x_slice = slices
        components.append(
            (area, label, (x_slice.start, y_slice.start, x_slice.stop, y_slice.stop))
        )

    components = sorted(components, reverse=True)[: args.frames]
    if len(components) != args.frames:
        raise SystemExit(
            f"Expected {args.frames} complete sprite islands, found {len(components)}"
        )

    selected = sorted(((label, box) for _, label, box in components), key=lambda value: value[1][0])
    boxes = [box for _, box in selected]
    max_width = max(right - left for left, _, right, _ in boxes)
    max_height = max(bottom - top for _, top, _, bottom in boxes)
    scale = min(args.content_size / max_width, args.content_size / max_height)

    args.out_dir.mkdir(parents=True, exist_ok=True)
    for index, (label, box) in enumerate(selected, start=1):
        sprite = source.crop(box)
        left, top, right, bottom = box
        component_mask = Image.fromarray(
            np.where(labels[top:bottom, left:right] == label, 255, 0).astype(np.uint8),
            mode="L",
        )
        sprite.putalpha(component_mask)
        width = max(1, round(sprite.width * scale))
        height = max(1, round(sprite.height * scale))
        sprite = sprite.resize((width, height), Image.Resampling.LANCZOS)
        frame = Image.new("RGBA", (args.frame_size, args.frame_size))
        frame.alpha_composite(sprite, ((args.frame_size - width) // 2, args.frame_size - height))
        frame.save(args.out_dir / f"frame_{index:02d}.png")

    if args.anchor:
        anchor = Image.open(args.anchor).convert("RGBA")
        if anchor.size != (args.frame_size, args.frame_size):
            raise SystemExit("Anchor must already match the requested frame size")
        anchor.save(args.out_dir / "frame_01.png")


if __name__ == "__main__":
    main()
