#!/usr/bin/env bash
# 2TV Samsung Smart TV (.wgt) Package Builder
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
CORE_DIR="$ROOT_DIR/web-tv-core"
OUTPUT_WGT="$SCRIPT_DIR/2TV-SamsungTV.wgt"

echo "=========================================="
echo "📦 Packaging 2TV for Samsung Smart TV (Tizen)"
echo "=========================================="

# Sync shared web-tv-core files into samsung-tv directory
echo "🔄 Syncing shared core files (web-tv-core)..."
mkdir -p "$SCRIPT_DIR/css" "$SCRIPT_DIR/js"
cp "$CORE_DIR/index.html" "$SCRIPT_DIR/index.html"
cp "$CORE_DIR/css/style.css" "$SCRIPT_DIR/css/style.css"
cp "$CORE_DIR/js/m3u-parser.js" "$SCRIPT_DIR/js/m3u-parser.js"
cp "$CORE_DIR/js/player.js" "$SCRIPT_DIR/js/player.js"
cp "$CORE_DIR/js/receiver.js" "$SCRIPT_DIR/js/receiver.js"
cp "$CORE_DIR/js/key-adapter.js" "$SCRIPT_DIR/js/key-adapter.js"
cp "$CORE_DIR/lib/qrcode.min.js" "$SCRIPT_DIR/js/qrcode.min.js"

# Remove old package
rm -f "$OUTPUT_WGT"

# Zip contents into .wgt (Tizen Widget format)
cd "$SCRIPT_DIR"
zip -r "$OUTPUT_WGT" config.xml index.html css/ js/ icon.png README.md > /dev/null

echo "✅ Samsung Tizen Package created successfully!"
echo "📍 Location: $OUTPUT_WGT"
echo "=========================================="
