#!/bin/bash
# 시뮬레이터에서 게임을 켜고 화면을 _to_delete/sim*.png 로 저장합니다.
cd "$(dirname "$0")/../.."
mkdir -p _to_delete
DEV="$(xcrun simctl list devices booted | grep -m1 -oE '[0-9A-F-]{36}')"
xcrun simctl launch "$DEV" com.trialmobile.rollonhero
sleep 8;  xcrun simctl io "$DEV" screenshot _to_delete/sim.png
sleep 10; xcrun simctl io "$DEV" screenshot _to_delete/sim2.png
