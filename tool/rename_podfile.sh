#!/usr/bin/env bash
cd "$(dirname "$0")" || exit
BASE_PATH=$(pwd)
BUILD_PATH=../all/build

# Make Repository
cd "$BASE_PATH" || exit
mkdir -p $BUILD_PATH/cocoapods/repository/debug
mkdir -p $BUILD_PATH/cocoapods/repository/release

# Copy Podspec
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/debug || exit
cp kgrpc.podspec ../../repository/kgrpc-debug.podspec
cd ../../repository/ || exit
sed -i -e "s|'kgrpc'|'kgrpc-debug'|g" kgrpc-debug.podspec
sed -i -e "s|'kgrpc.xcframework'|'debug/kgrpc.xcframework'|g" kgrpc-debug.podspec
rm *.podspec-e
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/release || exit
cp kgrpc.podspec ../../repository/kgrpc-release.podspec
cd ../../repository/ || exit
sed -i -e "s|'kgrpc'|'kgrpc-release'|g" kgrpc-release.podspec
sed -i -e "s|'kgrpc.xcframework'|'release/kgrpc.xcframework'|g" kgrpc-release.podspec
rm *.podspec-e

# Copy Framework
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/debug || exit
cp -r kgrpc.xcframework ../../repository/debug/kgrpc.xcframework
cd "$BASE_PATH" || exit
cd $BUILD_PATH/cocoapods/publish/release || exit
cp -r kgrpc.xcframework ../../repository/release/kgrpc.xcframework

# Copy LICENSE
cd "$BASE_PATH" || exit
cd ../ || exit
cp ./LICENSE ./all/build/cocoapods/repository/LICENSE
