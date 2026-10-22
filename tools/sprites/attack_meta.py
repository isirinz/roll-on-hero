#!/usr/bin/env python3
"""
공격 동작 메타데이터(와 발 위치 footY)를 그림에서 계산해 assets/sprites/index.json에 적는다(convert_chars.py 다음에 실행).

    python3 tools/sprites/attack_meta.py

- impact: 대기 자세(첫 프레임)와 가장 많이 달라지는 프레임 = 무기가 뻗은 순간. 그 프레임의 가운데 시점을
  동작 길이 대비 비율로 적는다. 전투 화면은 이때 피해 숫자·타격 이펙트·소리를 낸다.
- 자동 판정이 틀리면 attack_overrides.json에 {"이름": 비율} 또는 {"이름": {"impact": 비율, "recover": true}}.
- recover: 마지막 프레임이 첫 프레임과 많이 다르면(휘두른 채로 끝남) true. 게임이 몇 프레임을 되감아
  제자리로 돌아간 뒤 대기로 넘어가게 해, 대기로 '툭' 바뀌는 것을 막는다.
"""
import json
import os

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SPRITES = os.path.join(ROOT, 'assets', 'sprites')
RECOVER_RATIO = 0.35  # 마지막 프레임 차이가 최대 차이의 이 비율보다 크면 되감기


def frames(path, fw):
    a = np.array(Image.open(path).convert('RGBA')).astype(int)
    return [a[:, i * fw:(i + 1) * fw] for i in range(a.shape[1] // fw)]


def diff(a, b):
    return int((np.abs(a - b).sum(axis=2) > 60).sum())


def extent(f, faces_right):
    """앞쪽으로 뻗은 정도: 몬스터는 (폭 - 가장 왼쪽 불투명 열), 영웅은 가장 오른쪽 열."""
    cols = np.where((f[..., 3] > 0).any(axis=0))[0]
    if len(cols) == 0:
        return 0
    return int(cols.max()) if faces_right else int(f.shape[1] - cols.min())


def main():
    index_path = os.path.join(SPRITES, 'index.json')
    index = json.load(open(index_path))
    # 자동 판정이 틀린 캐릭터는 attack_overrides.json에 impact 비율을 직접 적는다
    ov_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'attack_overrides.json')
    overrides = json.load(open(ov_path)) if os.path.exists(ov_path) else {}
    for name, entry in sorted(index.items()):
        # footY: 대기 자세의 발(가장 아래 불투명 줄)이 칸 바닥보다 몇 픽셀 위인지. 게임이 그만큼 내려 그린다.
        idle_path = os.path.join(SPRITES, name, 'idle.png')
        if os.path.exists(idle_path):
            bottoms = [int(np.where((f[..., 3] > 20).any(axis=1))[0].max()) for f in frames(idle_path, entry['frameWidth'])]
            entry['footY'] = max(0, entry['frameHeight'] - 1 - max(bottoms))
        meta = entry['anims'].get('attack')
        path = os.path.join(SPRITES, name, 'attack.png')
        if not meta or not os.path.exists(path):
            continue
        fr = frames(path, entry['frameWidth'])
        d = [diff(fr[0], f) for f in fr]
        # 몬스터는 왼쪽 아래(영웅 쪽)를 보고 있으므로, 가장 왼쪽까지 뻗은 프레임이 타격 순간.
        # 영웅(facesRight)은 반대로 가장 오른쪽. 뻗는 폭이 작으면(3px 미만) 대기와 가장 다른 프레임으로.
        reach = [extent(f, entry.get('facesRight', False)) for f in fr]
        # 다시 만든 공격(10프레임 이상: 준비 → 타격 → 복귀)은 가운데 구간에서, 옛 공격은 앞 두 프레임을 빼고 찾는다
        # (준비 동작에서 팔을 뒤로 젖히며 앞으로 뻗는 경우가 있어 앞부분을 피한다)
        n = len(fr)
        window = range(n // 3, n - 2) if n >= 10 else range(2, n)
        if max(reach[i] for i in window) - reach[0] >= 3:
            peak = max(window, key=lambda i: (reach[i], d[i]))
        else:
            peak = max(window, key=lambda i: d[i])
        # 덮어쓰기: 숫자 하나면 impact, 객체면 {"impact": 비율, "recover": true/false} 중 적힌 것만
        ov = overrides.get(name, {})
        if not isinstance(ov, dict):
            ov = {'impact': ov}
        meta['impact'] = ov.get('impact', round((peak + 0.5) / len(fr), 2))
        # 10프레임 이상으로 다시 만든 공격은 제자리로 돌아오게 만들었으므로 되감기 없음(끝 자세가 다르면 덮어쓰기로 켠다)
        meta['recover'] = ov.get('recover', len(fr) < 10 and d[-1] > RECOVER_RATIO * max(1, d[peak]))
        print(f"{name:16s} frames={len(fr):2d} peak={peak} impact={meta['impact']:.2f} recover={meta['recover']}")
    json.dump(index, open(index_path, 'w'), indent=1, sort_keys=True)


if __name__ == '__main__':
    main()
