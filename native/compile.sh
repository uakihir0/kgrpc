#!/bin/bash
set -e

# Cross-compile kgrpc-native for all supported targets
# Usage: ./compile.sh [target...]
# If no targets specified, builds for the current host platform.
#
# Supported targets:
#   aarch64-apple-darwin     (macOS ARM)
#   x86_64-apple-darwin      (macOS Intel)
#   aarch64-apple-ios        (iOS ARM)
#   aarch64-apple-ios-sim    (iOS Simulator ARM)
#   x86_64-apple-ios         (iOS Simulator Intel)
#   x86_64-unknown-linux-gnu (Linux x64) - requires 'cross'
#   x86_64-pc-windows-gnu    (Windows mingw) - requires 'cross'
#
# Prerequisites:
#   - Rust toolchain (rustup)
#   - For Apple targets: Xcode command line tools
#   - For Linux/Windows cross-compile: cargo install cross

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Apple targets can be built directly with cargo
APPLE_TARGETS=(
    "aarch64-apple-darwin"
    "x86_64-apple-darwin"
    "aarch64-apple-ios"
    "aarch64-apple-ios-sim"
    "x86_64-apple-ios"
)

# Linux/Windows targets require 'cross' (Docker-based)
CROSS_TARGETS=(
    "x86_64-unknown-linux-gnu"
    "x86_64-pc-windows-gnu"
)

build_apple_target() {
    local target=$1
    echo "Building for $target (cargo)..."
    rustup target add "$target" 2>/dev/null || true
    cargo build --release --target "$target"
    echo "  -> target/release/$target/libkgrpc_native.a"
}

build_cross_target() {
    local target=$1
    echo "Building for $target (cross)..."
    if ! command -v cross &>/dev/null; then
        echo "  Error: 'cross' not found. Install with: cargo install cross"
        return 1
    fi
    cross build --release --target "$target"
    echo "  -> target/$target/release/libkgrpc_native.a"
}

if [ $# -eq 0 ]; then
    echo "No targets specified. Building for current host..."
    cargo build --release
    echo "Done."
    exit 0
fi

for target in "$@"; do
    is_apple=false
    for apple in "${APPLE_TARGETS[@]}"; do
        if [ "$target" = "$apple" ]; then
            is_apple=true
            break
        fi
    done

    if [ "$is_apple" = true ]; then
        build_apple_target "$target"
    else
        build_cross_target "$target"
    fi
done

echo ""
echo "All builds completed successfully."
