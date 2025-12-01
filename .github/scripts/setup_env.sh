#!/bin/bash
set -e

# Use the base_folder env var if set, otherwise default to current directory
TARGET_DIR="${base_folder:-.}"

echo ">> Setting up environment in: $TARGET_DIR"
cd "$TARGET_DIR"

# 1. Decode google-services.json
if [ -n "$GOOGLE_SERVICES" ]; then
    echo "$GOOGLE_SERVICES" | base64 --decode > ./app/google-services.json
    echo "✓ google-services.json created successfully"
else
    echo "::error::GOOGLE_SERVICES environment variable is not set!"
    exit 1
fi

# 2. Decode Keystore
mkdir -p keystore
if [ -n "$KEYSTORE_BASE64" ]; then
    echo "$KEYSTORE_BASE64" | base64 --decode > keystore/ci-debug.keystore
    echo "✓ ci-debug.keystore created successfully"
else
    echo "::warning::KEYSTORE_BASE64 is not set. Keystore will not be created."
fi

# 3. Ensure gradlew is executable
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    echo "✓ gradlew made executable"
else
    echo "::error::gradlew not found!"
    exit 1
fi