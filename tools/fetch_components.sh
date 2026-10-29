#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$REPO_DIR/app/src/main/assets/bundled_components"
RELEASE_URL="${COMPONENTS_URL:-https://github.com/jaredgei/wow-forever-android/releases/download/components-v1}"
FILES=(
    proton-11.0-90624-arm64ec.wcp
    dxvk-2.4.1-wow-aarch64-test.wcp
    turnip-wow-scheduler-test.zip
    turnip-V32-RP6sched.zip
)

mkdir -p "$DEST"
for f in "${FILES[@]}"; do
    if [ -s "$DEST/$f" ]; then
        echo "have $f"
    elif [ -n "${1:-}" ] && [ -s "$1/$f" ]; then
        cp "$1/$f" "$DEST/$f"
        echo "copied $f"
    else
        if [ "$f" = "turnip-V32-RP6sched.zip" ]; then
            curl -fL --retry 3 -o "$DEST/$f.part" "https://raw.githubusercontent.com/arusiasotto/wow-forever-a840/main/driver/turnip-V32-RP6sched.zip"
        else
            curl -fL --retry 3 -o "$DEST/$f.part" "$RELEASE_URL/$f"
        fi
        mv "$DEST/$f.part" "$DEST/$f"
        echo "downloaded $f"
    fi
done

(cd "$DEST" && shasum -a 256 -c "$REPO_DIR/tools/components.sha256")

# GameNative's Turnip v26.2.0 R4, used by the 3.3.5a container on Adreno 740 (see LegacyDriver.kt).
# Repacked so meta.json sits at the zip's root, which is where installDriver looks for it.
EXTRA=turnip-v26.2.0-R4.zip
EXTRA_URL="https://downloads.gamenative.app/drivers/Turnio_v26.2.0_R4.zip"
if [ -s "$DEST/$EXTRA" ]; then
    echo "have $EXTRA"
elif [ -n "${1:-}" ] && [ -s "$1/$EXTRA" ]; then
    cp "$1/$EXTRA" "$DEST/$EXTRA"
    echo "copied $EXTRA"
else
    TMP="$(mktemp -d)"
    curl -fL --retry 3 -o "$TMP/src.zip" "$EXTRA_URL"
    echo "source $(shasum -a 256 "$TMP/src.zip")"
    unzip -q "$TMP/src.zip" -d "$TMP/x"
    META="$(find "$TMP/x" -name meta.json | head -1)"
    if [ -z "$META" ]; then
        echo "no meta.json in $EXTRA_URL" >&2
        unzip -l "$TMP/src.zip" >&2
        exit 1
    fi
    cat "$META"
    (cd "$(dirname "$META")" && zip -qr "$DEST/$EXTRA.part" .)
    mv "$DEST/$EXTRA.part" "$DEST/$EXTRA"
    rm -rf "$TMP"
    echo "downloaded $EXTRA"
fi
unzip -l "$DEST/$EXTRA"
