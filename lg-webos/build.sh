#!/usr/bin/env bash
# 2TV LG webOS Package Builder
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
CORE_DIR="$ROOT_DIR/web-tv-core"
OUTPUT_IPK="$SCRIPT_DIR/2TV-LGwebOS.ipk"
OUTPUT_ZIP="$SCRIPT_DIR/2TV-LGwebOS.zip"

echo "=========================================="
echo "📦 Packaging 2TV for LG Smart TV (webOS)"
echo "=========================================="

mkdir -p "$SCRIPT_DIR/css" "$SCRIPT_DIR/js"
cp "$CORE_DIR/css/style.css" "$SCRIPT_DIR/css/style.css"
cp "$CORE_DIR/js/m3u-parser.js" "$SCRIPT_DIR/js/m3u-parser.js"
cp "$CORE_DIR/js/player.js" "$SCRIPT_DIR/js/player.js"
cp "$CORE_DIR/js/receiver.js" "$SCRIPT_DIR/js/receiver.js"
cp "$CORE_DIR/js/key-adapter.js" "$SCRIPT_DIR/js/key-adapter.js"
cp "$CORE_DIR/lib/qrcode.min.js" "$SCRIPT_DIR/js/qrcode.min.js"
cp "$SCRIPT_DIR/../samsung-tv/js/server.js" "$SCRIPT_DIR/js/server.js"
cp "$SCRIPT_DIR/../samsung-tv/js/app.js" "$SCRIPT_DIR/js/app.js"

# Remove old packages
rm -f "$OUTPUT_IPK" "$OUTPUT_ZIP"

# Zip contents into webOS app package
cd "$SCRIPT_DIR"
zip -r "$OUTPUT_ZIP" appinfo.json index.html css/ js/ > /dev/null
cp "$OUTPUT_ZIP" "$OUTPUT_IPK"

echo "✅ LG webOS Package created successfully!"
echo "📍 Location: $OUTPUT_IPK"
echo "=========================================="
