# UI 픽셀아트 에셋

그리는 방식: Nearest 필터 + 정수배 확대. 패널·버튼·탭·슬롯은 3배(`UiSkin.SCALE`), 게이지는 2배(`UiSkin.BAR_SCALE`).
나인패치 여백 값은 `ui/UiSkin.java`와 `tools/ui-assets/build_ui2.py`에 같은 값으로 들어 있다(한쪽을 바꾸면 다른 쪽도).

| 파일 | 출처 | 나인패치 (L R T B) |
|---|---|---|
| `panel.png` | PixelLab Pro, 나무 액자 + 금 모서리 + 양피지 | 12 12 12 12 |
| `panel_dark.png` | PixelLab Pro, 어두운 나무 + 보라 바탕 (상단 바) | 11 11 11 11 |
| `frame.png` | PixelLab Pro, 어두운 나무 액자, 가운데 투명 (전투 영역) | 10 10 10 10 |
| `ribbon.png` | PixelLab Pro, 빨간 리본 (상점 소제목) | 12 12 4 5 |
| `pill.png` | PixelLab Pro, 금테 캡슐 (골드 표시) | 8 8 4 4 |
| `tab.png`, `tab_on.png` | PixelLab Pro, 나무 타일 (하단 탭 평소/선택) | 8 8 8 9 |
| `btn_<색>.png`, `btn_<색>_down.png` | PixelLab Pro 버튼 하나를 색만 돌려 만듦: blue green gold rose purple off | 6 6 5 6 (눌림 6 6 7 4) |
| `slot.png` | PixelLab Pro, 금 고리 (스킬 슬롯). 안쪽은 투명하게 뚫려 있음 | 통그림 |
| `gauge.png` | PixelLab Pro, 금테 게이지 틀 45x11, 안쪽 홈 투명 (체력·경험치, `ui/Gauge`). 채움 색 띠는 `UiSkin`이 코드로 만든다 | 6 6 5 5 |
| `badge.png` | PixelLab Pro, 방패 (상단 바 스테이지 표시) | 통그림 |
| `bar_<색>.png` | `tools/ui-assets/gen_ui.py`로 직접 생성 | 3 3 3 3 |
| `battle_bg.png` | PixelLab pixflux 168x148 | 전투 영역은 아랫부분 168x140을 4배로 그린다 |
| `icon_*.png`, `skill_<Skill.id>.png` | PixelLab pixflux 32x32 | 통그림 |

다시 만들기: PixelLab에서 고른 원본은 `tools/ui-assets/raw2/`에 있다.
원본을 바꾼 뒤 `python3 tools/ui-assets/build_ui2.py` (Pillow 필요)를 실행하면 이 폴더의 그림이 다시 만들어진다.
새 스킬을 추가하면 `skill_<id>.png`를 만들고 `UiSkin.SKILL_IDS`에 id를 추가한다(파일이 없으면 아이콘 없이 동작).
