#!/usr/bin/env bash
# 2TV Samsung Smart TV (.wgt) Package Builder
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_WGT="$SCRIPT_DIR/2TV-SamsungTV.wgt"

echo "=========================================="
echo "📦 Packaging 2TV for Samsung Smart TV (Tizen)"
echo "=========================================="

# Remove old package
rm -f "$OUTPUT_WGT"

# Zip contents into .wgt (Tizen Widget format)
cd "$SCRIPT_DIR"
zip -r "$OUTPUT_WGT" config.xml index.html css/ js/ icon.png README.md > /dev/null

echo "✅ Package created successfully!"
echo "📍 Location: $OUTPUT_WGT"
echo "=========================================="
