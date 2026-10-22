# 캐릭터·이펙트 스프라이트 도구

PixelLab에서 만든 캐릭터·이펙트를 게임 형식으로 바꾸는 스크립트. Python 3 + Pillow 필요.

## 캐릭터 (`convert_chars.py`)

- `chars_list.json`: 게임 그림 id → PixelLab 캐릭터 id, 전투에서 쓰는 방향(영웅 `east`, 몬스터 `south-west` — 몬스터는 왼쪽 아래, 즉 영웅과 화면 쪽을 함께 바라보는 3/4 시점), 화면 배율.
  그림 id는 `MonsterTable`의 몬스터 종류 id와 같아야 한다(영웅은 `hero`).
- 실행: `python3 tools/sprites/convert_chars.py . [id ...]` (프로젝트 루트에서, id를 주면 그것만)
- 결과: `assets/sprites/<id>/<동작>.png` (가로 프레임 띠) + `assets/sprites/index.json`.
- 동작 이름: `idle`(반복), `attack`, `hurt`, `cast`(영웅), `death`(마지막 프레임 유지). 없는 동작은 게임에서 대기로 대신한다.
- PixelLab에서 만들 때 쓴 설정: `create_character(mode="v3", view="side", size=64)` (보스는 96),
  애니메이션은 `animate_character(mode="v3", directions=[방향], frame_count=6, animation_name=<동작>)` (피격은 4프레임).
  `animation_name`이 그대로 동작 이름이 된다. 같은 이름이 다른 방향 그룹에도 있으면 PixelLab이 `_2`를 붙이는데, 변환기가 떼어 낸다.
  몬스터 방향을 바꾸려면 그 방향으로 4동작을 다시 만들고(`directions=["<방향>"]`) `chars_list.json`의 direction을 바꾼 뒤 변환한다.

## 이펙트 (`convert_fx.py`)

- `fx_list.json`: 이펙트 이름 → PixelLab VFX 다운로드 주소(확장자 제외), 화면 배율.
- 실행: `python3 tools/sprites/convert_fx.py .`
- 결과: `assets/fx/<이름>.png` + `assets/fx/index.json` (프레임 시간, 기준점 pivot 포함).
- PixelLab: `create_vfx(artist="lite")` 결과의 `download ...` 주소를 `fx_list.json`에 넣는다.

게임에서 쓰는 이펙트 이름: `hit_slash`, `spell_bolt`, `spell_heal`, `spell_vorpal`, `spell_fire`, `spell_hellbomb`,
`level_up`, `defeat_poof` (`BattleScreen` 참고). 파일이 없으면 그 연출만 빠진다.
