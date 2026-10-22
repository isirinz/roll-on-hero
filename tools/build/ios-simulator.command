#!/bin/bash
# 아이폰 시뮬레이터에서 실행: Finder에서 더블클릭하거나 터미널에서 실행.
cd "$(dirname "$0")/../.."
LOG=build/ios-simulator.log; mkdir -p build
export JAVA_HOME="$(/usr/libexec/java_home -v 17+ 2>/dev/null)"
{
  echo "== $(date) =="; echo "JAVA_HOME=$JAVA_HOME"; sw_vers; xcodebuild -version; xcode-select -p
  xcrun simctl list devices available | head -40
  ./gradlew --console=plain -Dorg.gradle.logging.level=lifecycle ios:launchIPhoneSimulator
  echo "EXIT=$?"
} 2>&1 | tee "$LOG"
