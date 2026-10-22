#!/bin/bash
# 더블클릭하면 이 프로젝트를 GitHub(isirinz/roll-on-hero)에 올립니다. 맥에 저장된 GitHub 로그인을 씁니다.
cd "$(dirname "$0")/../.."
LOG="$PWD/_to_delete/git-push.log"
mkdir -p _to_delete
{
  echo "=== $(date)"
  [ -d .git ] || git init -q -b main
  git remote get-url origin >/dev/null 2>&1 || git remote add origin https://github.com/isirinz/roll-on-hero.git
  git config user.name >/dev/null || git config user.name "isirinz"
  git config user.email >/dev/null || git config user.email "isirinz@naver.com"
  git add -A
  echo "files: $(git ls-files | wc -l)"
  git ls-files | grep -E "keystore|\.jks$|local.properties|^dist/|\.aab$|\.apk$" && { echo "STOP: secret or build file staged"; exit 1; }
  if ! git rev-parse HEAD >/dev/null 2>&1; then
    git commit -q -F - <<'MSG'
Roll On, Hero! (굴려라 용사여!) 1.0.0

Pixel-art idle RPG built with libGDX: dice combat, 10 chapters, 50 monsters,
rebirth, offline progress, Korean and English.
GitHub Pages site and privacy policy in docs/.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01YHj9XfXtsU3kDFt7cQpeh7
MSG
  else
    git diff --cached --quiet || git commit -q -m "Update"
  fi
  git log --oneline | head -3
  git push -u origin main
  echo "=== exit $?"
} 2>&1 | tee "$LOG"
