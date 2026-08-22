#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source_atlas="$project_root/app/src/main/assets/game/atlas/pet/pet_moonmoth_glimmerling_atlas.webp"
target_atlas="$project_root/app/src/main/assets/game/atlas/pet/pet_moonmoth_lanternwing_atlas.webp"
work_dir="$project_root/tools/assets/work/lanternwing"
mkdir -p "$work_dir"

convert "$source_atlas" -fill '#566FA3' -colorize 8% "$work_dir/tinted_atlas.miff"
convert "$source_atlas" -alpha extract -morphology Dilate Octagon:2 "$work_dir/aura_mask.miff"
convert -size 2048x3072 xc:'#F8D879' "$work_dir/aura_mask.miff" -alpha off \
    -compose copy_opacity -composite "$work_dir/aura.miff"
convert "$work_dir/aura.miff" "$work_dir/tinted_atlas.miff" -compose over -composite \
    -define webp:lossless=true "$target_atlas"
identify "$target_atlas"
