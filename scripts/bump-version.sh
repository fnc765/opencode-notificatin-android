#!/usr/bin/env bash
set -euo pipefail

FILE="app/build.gradle.kts"

VERSION_CODE=$(grep 'versionCode' "$FILE" | grep -oP '\d+')
VERSION_NAME=$(grep 'versionName' "$FILE" | grep -oP '"[^"]*"' | tr -d '"')

NEW_CODE=$((VERSION_CODE + 1))
TODAY=$(date +%Y.%m.%d)
NEW_NAME="${TODAY}.${NEW_CODE}"

echo "Bumping version: $VERSION_NAME (code $VERSION_CODE) -> $NEW_NAME (code $NEW_CODE)"

sed -i "s/versionCode = $VERSION_CODE/versionCode = $NEW_CODE/" "$FILE"
sed -i "s/versionName = \"$VERSION_NAME\"/versionName = \"$NEW_NAME\"/" "$FILE"

echo "Done: $(grep -E 'versionCode|versionName' "$FILE")"
