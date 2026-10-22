#!/bin/bash
# 더블클릭하면 Google Play에 올릴 AAB(와 시험 설치용 APK)를 만듭니다. 결과: dist/RollOnHero-<버전>.aab
cd "$(dirname "$0")/../.."
LOG="$PWD/_to_delete/android-build.log"
mkdir -p _to_delete dist
{
  echo "=== $(date)"
  export JAVA_HOME="$(/usr/libexec/java_home -v 17+ 2>/dev/null || /usr/libexec/java_home)"
  echo "java: $JAVA_HOME"
  grep sdk.dir local.properties; ls "$(grep sdk.dir local.properties | cut -d= -f2)/platforms" 2>&1 | tail -3
  [ -f keystore/keystore.properties ] && echo "signing: keystore/keystore.properties" || echo "signing: NONE (unsigned)"
  ./gradlew android:bundleRelease android:assembleRelease --console=plain
  RC=$?
  V=$(grep -m1 versionName android/build.gradle | sed 's/.*"\(.*\)".*/\1/')
  cp android/build/outputs/bundle/release/android-release.aab "dist/RollOnHero-$V.aab" 2>/dev/null
  cp android/build/outputs/apk/release/android-release.apk "dist/RollOnHero-$V.apk" 2>/dev/null
  ls -la dist/RollOnHero-$V.* 2>&1
  echo "=== exit $RC"
} 2>&1 | tee "$LOG"
