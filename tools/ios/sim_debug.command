#!/bin/bash
# 시뮬레이터에 게임을 다시 설치·실행하고 콘솔 출력과 로그를 _to_delete/ 에 모읍니다.
cd "$(dirname "$0")/../.."
OUT=_to_delete/sim-debug.log
APP="$PWD/ios/build/robovm.tmp/IOSLauncher.app"
{
  echo "=== $(date)"
  xcrun simctl list devices booted
  /usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$APP/Info.plist"
  /usr/libexec/PlistBuddy -c "Print :CFBundleExecutable" "$APP/Info.plist"
  xcrun simctl install booted "$APP" && echo installed
  xcrun simctl listapps booted | grep -i -B1 -A2 "trialmobile" | head
  ( xcrun simctl launch --console-pty booted com.trialmobile.rollonhero & PID=$!; sleep 20; kill $PID 2>/dev/null ) 2>&1 | head -150
  echo "=== system log"
  xcrun simctl spawn booted log show --last 2m --style compact --predicate 'process == "IOSLauncher" OR (eventMessage CONTAINS[c] "trialmobile" AND messageType >= 16)' 2>&1 | tail -60
  echo "=== crash reports"
  ls -t ~/Library/Logs/DiagnosticReports/ 2>/dev/null | head -5
} > "$OUT" 2>&1
xcrun simctl io booted screenshot _to_delete/sim.png
