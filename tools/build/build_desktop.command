#!/bin/bash
# 더블클릭: 맥(Apple Silicon·Intel)·윈도우용 실행 파일을 만들어 dist/ 에 넣습니다. (JDK 17 이상 필요)
cd "$(dirname "$0")/../.."
ROOT="$PWD"
LOG="$ROOT/_to_delete/desktop-build.log"
mkdir -p _to_delete dist
{
  echo "=== $(date)"
  export JAVA_HOME="$(/usr/libexec/java_home -v 17+ 2>/dev/null || /usr/libexec/java_home)"
  echo "java: $JAVA_HOME"
  ./gradlew --console=plain lwjgl3:packageMacM1 lwjgl3:packageMacX64 lwjgl3:packageWinX64
  RC=$?
  echo "--- gradle exit $RC"
  [ $RC -eq 0 ] || exit $RC
  OUT="$ROOT/lwjgl3/build/construo/dist"
  ls -la "$OUT"
  STAGE="$ROOT/_to_delete/pack"; rm -rf "$STAGE"; mkdir -p "$STAGE"
  # 맥: zip 안의 .app을 그대로 다시 묶는다 (이름: Roll On, Hero!.app)
  for t in macM1:Mac-AppleSilicon macX64:Mac-Intel; do
    src="${t%%:*}"; dst="${t##*:}"
    z=$(ls "$OUT"/*"$src"*.zip | head -1); d="$STAGE/$src"; mkdir -p "$d"
    (cd "$d" && unzip -q "$z")
    ls "$d"
    rm -f "dist/RollOnHero-$dst.zip"
    (cd "$d" && ditto -c -k --sequesterRsrc --keepParent "Roll On, Hero!.app" "$ROOT/dist/RollOnHero-$dst.zip")
  done
  # 윈도우: RollOnHero-Windows/ 폴더에 rollonhero.exe
  z=$(ls "$OUT"/*winX64*.zip | head -1); d="$STAGE/win/RollOnHero-Windows"; mkdir -p "$d"
  (cd "$d" && unzip -q "$z")
  # zip 안에 폴더가 하나만 있으면 그 안의 내용을 위로 올린다
  if [ "$(ls -A "$d" | wc -l)" -eq 1 ] && [ -d "$d/$(ls -A "$d")" ]; then inner="$d/$(ls -A "$d")"; mv "$inner"/* "$d"/; rmdir "$inner"; fi
  ls "$d"
  rm -f dist/RollOnHero-Windows-x64.zip
  (cd "$STAGE/win" && zip -qr "$ROOT/dist/RollOnHero-Windows-x64.zip" RollOnHero-Windows)
  # 바로 실행해 볼 수 있게 이 맥용 .app도 dist/에 풀어 둔다
  ARCH=$(uname -m); [ "$ARCH" = arm64 ] && A=macM1 || A=macX64
  rm -rf "dist/Roll On, Hero!.app"; cp -R "$STAGE/$A/Roll On, Hero!.app" dist/
  ls -la dist
  echo "=== done"
} 2>&1 | tee "$LOG"
