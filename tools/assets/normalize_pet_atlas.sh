#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
work_dir="$project_root/tools/assets/work"
clean_dir="$work_dir/clean"
frames_dir="$work_dir/frames"
output_dir="$project_root/app/src/main/assets/game/atlas/pet"
seed="$work_dir/pet_moonmoth_glimmerling_seed_256.png"
animations=(idle blink walk run jump_start jump_loop fall land interact sleep wake celebrate)

command -v convert >/dev/null
command -v identify >/dev/null
mkdir -p "$frames_dir" "$output_dir"

for animation in "${animations[@]}"; do
    source_image="$clean_dir/$animation.png"
    target_dir="$frames_dir/$animation"
    mkdir -p "$target_dir"
    find "$target_dir" -type f -name '*.png' -delete

    mapfile -t geometries < <(
        convert "$source_image" -alpha extract -threshold 20% \
            -define connected-components:verbose=true -connected-components 8 null: 2>&1 |
        awk '$0 ~ /gray\(255\)/ { split($2, parts, "+"); split(parts[1], size, "x"); if (size[1] * size[2] > 4000) print parts[2], $2 }' |
        sort -n | awk '{print $2}'
    )
    if (( ${#geometries[@]} < 7 )); then
        echo "Animation $animation yielded fewer than seven usable frames" >&2
        exit 1
    fi

    max_width=0
    max_height=0
    extracted=()
    for index in "${!geometries[@]}"; do
        geometry="${geometries[$index]}"
        frame="$target_dir/raw_$((index + 1)).png"
        convert "$source_image" -crop "$geometry" +repage -bordercolor none -border 6 "$frame"
        read -r width height < <(identify -format '%w %h\n' "$frame")
        if (( width > max_width )); then max_width=$width; fi
        if (( height > max_height )); then max_height=$height; fi
        extracted+=("$frame")
    done

    width_percent=$(( 23600 / max_width ))
    height_percent=$(( 22000 / max_height ))
    scale_percent=$width_percent
    if (( height_percent < scale_percent )); then scale_percent=$height_percent; fi
    if (( scale_percent > 125 )); then scale_percent=125; fi

    convert -size 2048x256 xc:none "$seed" -geometry +0+0 -composite "$target_dir/strip.png"
    output_slot=1
    for source_index in $(seq 1 $((${#extracted[@]} - 1))); do
        if (( output_slot >= 8 )); then break; fi
        normalized="$target_dir/frame_$((output_slot + 1)).png"
        convert "${extracted[$source_index]}" -resize "${scale_percent}%" -gravity south -background none -extent 256x256 "$normalized"
        convert "$target_dir/strip.png" "$normalized" -geometry +$((output_slot * 256))+0 -composite "$target_dir/strip_next.png"
        mv "$target_dir/strip_next.png" "$target_dir/strip.png"
        ((output_slot += 1))
    done
    while (( output_slot < 8 )); do
        convert "$target_dir/strip.png" "$seed" -geometry +$((output_slot * 256))+0 -composite "$target_dir/strip_next.png"
        mv "$target_dir/strip_next.png" "$target_dir/strip.png"
        ((output_slot += 1))
    done
done

convert \
    "$frames_dir/idle/strip.png" "$frames_dir/blink/strip.png" "$frames_dir/walk/strip.png" "$frames_dir/run/strip.png" \
    "$frames_dir/jump_start/strip.png" "$frames_dir/jump_loop/strip.png" "$frames_dir/fall/strip.png" "$frames_dir/land/strip.png" \
    "$frames_dir/interact/strip.png" "$frames_dir/sleep/strip.png" "$frames_dir/wake/strip.png" "$frames_dir/celebrate/strip.png" \
    -append -define webp:lossless=true "$output_dir/pet_moonmoth_glimmerling_atlas.webp"

convert "$output_dir/pet_moonmoth_glimmerling_atlas.webp" -channel A \
    -fx '((g>0.75 && g>r*1.35 && g>b*1.2)||(r>0.75 && r>g*1.5 && r>b*1.5))?0:a' \
    +channel -define webp:lossless=true "$output_dir/pet_moonmoth_glimmerling_atlas_clean.webp"
mv "$output_dir/pet_moonmoth_glimmerling_atlas_clean.webp" "$output_dir/pet_moonmoth_glimmerling_atlas.webp"

identify "$output_dir/pet_moonmoth_glimmerling_atlas.webp"
