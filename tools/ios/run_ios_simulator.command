#!/bin/bash
# 더블클릭하면 아이폰 시뮬레이터로 빌드하고 실행합니다. (macOS + Xcode 필요)
cd "$(dirname "$0")/../.."
LOG="$PWD/_to_delete/ios-build.log"
mkdir -p _to_delete
{
  echo "=== $(date) ==="
  xcodebuild -version | head -1
  export JAVA_HOME="$(/usr/libexec/java_home -v 17+ 2>/dev/null || /usr/libexec/java_home)"
  # RoboVM은 'Simulator' 앱을 이름으로 찾는데, 등록이 안 된 경우가 있어 직접 실행합니다.
  SIMAPP="$(xcode-select -p)/Applications/Simulator.app"
  DEV="$(xcrun simctl list devices available | grep -m1 'iPhone 16 Pro (' | grep -oE '[0-9A-F-]{36}')"
  echo "--- device $DEV, simulator app: $SIMAPP"
  echo "--- build"
  ./gradlew ios:launchIPhoneSimulator --console=plain || true
  APP="$PWD/ios/build/robovm.tmp/IOSLauncher.app"
  [ -d "$APP" ] || { echo "build failed: no app"; exit 1; }
  echo "--- boot & install"
  xcrun simctl boot "$DEV" 2>&1 | grep -v "current state: Booted"
  open "$SIMAPP"
  xcrun simctl bootstatus "$DEV" -b
  xcrun simctl install "$DEV" "$APP"
  xcrun simctl launch "$DEV" com.trialmobile.rollonhero
  echo "=== done $? ==="
} 2>&1 | tee "$LOG"
