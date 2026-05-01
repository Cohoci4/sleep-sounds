#!/usr/bin/env bash
# Regenerate the six bundled placeholder sounds + cover JPGs that ship with
# the app. Replace these with licensed assets before publishing.
#
# Requires: ffmpeg, ImageMagick (`convert`).
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$SCRIPT_DIR/.." && pwd)

SOUNDS=$ROOT/app/src/main/assets/sounds
COVERS=$ROOT/app/src/main/assets/covers
mkdir -p "$SOUNDS" "$COVERS"

ENC="-ar 44100 -ac 2 -b:a 192k -codec:a libmp3lame"

# 1. Rain — pink noise, mid/high band shaping
ffmpeg -y -hide_banner -loglevel error \
  -f lavfi -i "anoisesrc=color=pink:amplitude=0.45:duration=60" \
  -af "highpass=f=150,lowpass=f=8000,acompressor=threshold=-20dB:ratio=4:attack=10:release=200,volume=0.85,afade=t=in:st=0:d=2,afade=t=out:st=58:d=2" \
  $ENC "$SOUNDS/rain.mp3"

# 2. Fireplace — brown noise + slow tremolo
ffmpeg -y -hide_banner -loglevel error \
  -f lavfi -i "anoisesrc=color=brown:amplitude=0.50:duration=60" \
  -af "lowpass=f=1200,highpass=f=80,tremolo=f=0.3:d=0.15,volume=0.9,afade=t=in:st=0:d=2,afade=t=out:st=58:d=2" \
  $ENC "$SOUNDS/fireplace.mp3"

# 3. Ocean — brown noise with slow swell
ffmpeg -y -hide_banner -loglevel error \
  -f lavfi -i "anoisesrc=color=brown:amplitude=0.55:duration=60" \
  -af "lowpass=f=900,highpass=f=60,tremolo=f=0.1:d=0.5,volume=0.95,afade=t=in:st=0:d=3,afade=t=out:st=57:d=3" \
  $ENC "$SOUNDS/ocean.mp3"

# 4. Forest night — pink noise + chirp-like tremolo on 2 kHz sine
ffmpeg -y -hide_banner -loglevel error \
  -f lavfi -i "anoisesrc=color=pink:amplitude=0.20:duration=60" \
  -f lavfi -i "sine=frequency=2000:sample_rate=44100:duration=60" \
  -filter_complex "[1:a]tremolo=f=0.6:d=0.95,volume=0.05[chirp];[0:a][chirp]amix=inputs=2:duration=longest:weights='1 0.4',lowpass=f=6000,highpass=f=200,afade=t=in:st=0:d=2,afade=t=out:st=58:d=2" \
  $ENC "$SOUNDS/forest.mp3"

# 5. Singing bowls — three layered sines with detuned tremolo
ffmpeg -y -hide_banner -loglevel error \
  -f lavfi -i "sine=frequency=196:sample_rate=44100:duration=60" \
  -f lavfi -i "sine=frequency=294:sample_rate=44100:duration=60" \
  -f lavfi -i "sine=frequency=392:sample_rate=44100:duration=60" \
  -filter_complex "[0:a]volume=0.45,tremolo=f=0.4:d=0.6[a];[1:a]volume=0.30,tremolo=f=0.5:d=0.7[b];[2:a]volume=0.20,tremolo=f=0.3:d=0.6[c];[a][b][c]amix=inputs=3:duration=longest:weights='1 1 1',afade=t=in:st=0:d=4,afade=t=out:st=56:d=4" \
  $ENC "$SOUNDS/singing_bowls.mp3"

# 6. White noise — gentle filtered
ffmpeg -y -hide_banner -loglevel error \
  -f lavfi -i "anoisesrc=color=white:amplitude=0.35:duration=60" \
  -af "lowpass=f=10000,highpass=f=80,volume=0.8,afade=t=in:st=0:d=1,afade=t=out:st=59:d=1" \
  $ENC "$SOUNDS/white_noise.mp3"

declare -a COVERS_DEF=(
  "rain|#3B5C8A|#A6C0E5|RAIN|~ on the window ~"
  "fireplace|#5B2E0E|#F1A66A|FIRE|~ wood crackling ~"
  "ocean|#0F3D54|#7FD0C8|OCEAN|~ waves on the shore ~"
  "forest|#1B3A2A|#A5C99B|FOREST|~ deep night ~"
  "singing_bowls|#3B244F|#E5C28A|BOWLS|~ meditative tones ~"
  "white_noise|#252B33|#C5CAD2|NOISE|~ smooth focus ~"
)

for entry in "${COVERS_DEF[@]}"; do
  IFS='|' read -r name top bottom title subtitle <<<"$entry"
  convert -size 1024x1024 \
    "gradient:${top}-${bottom}" \
    -fill 'rgba(255,255,255,0.55)' -gravity center \
    -font DejaVu-Sans-Bold -pointsize 96 \
    -annotate +0-40 "$title" \
    -fill 'rgba(255,255,255,0.7)' -pointsize 36 \
    -annotate +0+60 "$subtitle" \
    -quality 88 "$COVERS/${name}.jpg"
done

echo "Regenerated assets in:"
ls -lh "$SOUNDS" "$COVERS"
