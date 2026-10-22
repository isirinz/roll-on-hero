#!/bin/bash
# 출시용 APK를 에뮬레이터(없으면 첫 AVD를 켬)에 설치·실행하고 화면과 로그를 _to_delete/에 남깁니다.
cd "$(dirname "$0")/../.."
SDK="$(grep sdk.dir local.properties | cut -d= -f2)"
ADB="$SDK/platform-tools/adb"; EMU="$SDK/emulator/emulator"
OUT=_to_delete/android-test.log
{
  echo "=== $(date)"
  if ! "$ADB" devices | grep -q "device$"; then
    AVD=$("$EMU" -list-avds | head -1); echo "starting AVD $AVD"
    nohup "$EMU" -avd "$AVD" -no-snapshot-save >/dev/null 2>&1 &
    "$ADB" wait-for-device
    until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
  fi
  "$ADB" devices
  "$ADB" uninstall com.trialmobile.rollonhero >/dev/null 2>&1
  "$ADB" install -r dist/RollOnHero-1.0.0.apk
  "$ADB" logcat -c
  "$ADB" shell am start -n com.trialmobile.rollonhero/com.trialmobile.myfantasy.android.AndroidLauncher
  sleep 12; "$ADB" exec-out screencap -p > _to_delete/android-title.png
  "$ADB" shell input tap 540 1200; sleep 8; "$ADB" exec-out screencap -p > _to_delete/android-battle.png
  sleep 6; "$ADB" exec-out screencap -p > _to_delete/android-battle2.png
  echo "--- pid: $("$ADB" shell pidof com.trialmobile.rollonhero)"
  echo "--- crashes"; "$ADB" logcat -d -b crash | tail -40
  echo "--- errors"; "$ADB" logcat -d '*:E' | grep -iE "rollonhero|gdx|AndroidRuntime|FATAL" | tail -30
  echo "=== done"
} > "$OUT" 2>&1
