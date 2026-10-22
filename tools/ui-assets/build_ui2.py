#!/usr/bin/env python3
"""PixelLab Pro로 뽑은 UI 조각(raw2/)을 게임용 나인패치 PNG(assets/ui/)로 정리한다.

    python3 tools/ui-assets/build_ui2.py            # 프로젝트 루트에서

하는 일
- 투명 여백을 잘라낸다.
- 나인패치로 늘릴 가장자리 줄과 가운데를 한 가지 패턴으로 평평하게 만든다
  (늘어날 때 무늬가 줄무늬처럼 번지지 않게).
- 버튼 하나(raw2/button.png)를 색상만 돌려 여러 색으로 만들고, 눌린 모양(_down)도 만든다.
  모든 버튼 모양이 똑같아서 화면이 정돈돼 보인다.

나인패치 여백 값은 UiSkin.java의 상수와 짝이다. 여기서 바꾸면 거기도 바꾼다.
"""
import colorsys
import os
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RAW = os.path.join(os.path.dirname(os.path.abspath(__file__)), "raw2")
OUT = os.path.join(ROOT, "assets", "ui")

# 버튼 색: (이름, 색상(도), 채도 배율, 명도 배율). 원본은 하늘색(약 200도).
BUTTON_COLORS = [
    ("blue", None, 1.0, 1.0),
    ("green", 100, 0.95, 0.92),
    ("gold", 42, 1.0, 1.04),
    ("rose", 350, 0.85, 1.0),
    ("purple", 275, 0.7, 0.95),
    ("off", None, 0.12, 0.8),
]


def load(name):
    im = Image.open(os.path.join(RAW, name)).convert("RGBA")
    return im.crop(im.getbbox())


def flatten9(im, l, r, t, b):
    """가장자리 띠의 가운데 구간을 한 줄 패턴으로 반복하고, 가운데 칸은 중심 색 하나로 채운다."""
    im = im.copy()
    w, h = im.size
    px = im.load()
    cx, cy = (l + w - r) // 2, (t + h - b) // 2
    for y in list(range(t)) + list(range(h - b, h)):
        for x in range(l, w - r):
            px[x, y] = px[cx, y]
    for x in list(range(l)) + list(range(w - r, w)):
        for y in range(t, h - b):
            px[x, y] = px[x, cy]
    fill = px[cx, cy]
    for y in range(t, h - b):
        for x in range(l, w - r):
            px[x, y] = fill
    return im


def recolor(im, hue, sat_mul, val_mul):
    """어두운 외곽선은 그대로 두고 나머지 픽셀의 색상·채도·명도를 바꾼다."""
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if v < 0.3 and s < 0.6:  # 외곽선(진한 보라)
                continue
            if hue is not None:
                hh = hue / 360.0
            s = min(1.0, s * sat_mul)
            v = min(1.0, v * val_mul)
            rr, gg, bb = colorsys.hsv_to_rgb(hh, s, v)
            px[x, y] = (round(rr * 255), round(gg * 255), round(bb * 255), a)
    return out


def pressed(im, depth=2):
    """눌린 버튼: 윗부분을 depth만큼 내리고 아래 두께(립)를 없앤다. 맨 아래 외곽선 줄은 유지."""
    w, h = im.size
    out = Image.new("RGBA", (w, h))
    body = im.crop((0, 0, w, h - 1 - depth))
    out.paste(body, (0, depth))
    out.paste(im.crop((0, h - 1, w, h)), (0, h - 1))
    # 아래 모서리 모양은 원본 그대로 (둥근 모서리가 각지지 않게)
    for x in list(range(3)) + list(range(w - 3, w)):
        for y in range(h - 3, h):
            out.putpixel((x, y), im.getpixel((x, y)))
    return out


def hollow(im, threshold=150):
    """고리 안쪽의 어두운 칸을 투명하게 뚫는다(가운데에서 어두운 픽셀만 따라 채우기).
    게임에서는 아이콘·쿨타임을 먼저 그리고 고리를 위에 덮는다."""
    im = im.copy()
    px = im.load()
    w, h = im.size
    stack = [(w // 2, h // 2)]
    seen = set()
    while stack:
        x, y = stack.pop()
        if (x, y) in seen or not (0 <= x < w and 0 <= y < h):
            continue
        seen.add((x, y))
        r, g, b, a = px[x, y]
        if a == 0 or r + g + b >= threshold:
            continue
        px[x, y] = (0, 0, 0, 0)
        stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    return im


def save(im, name):
    im.save(os.path.join(OUT, name))
    print("  %-22s %dx%d" % (name, im.width, im.height))


def main():
    os.makedirs(OUT, exist_ok=True)
    print("-> " + OUT)

    # 패널: 나무 액자 + 금 모서리 + 양피지. 나인패치 12/12/12/12
    save(flatten9(load("panel.png"), 12, 12, 12, 12), "panel.png")

    # 버튼: 39x16, 나인패치 L6 R6 T5 B6 (눌림: T7 B4)
    base = flatten9(load("button.png"), 6, 6, 5, 6)
    for name, hue, sm, vm in BUTTON_COLORS:
        up = base if hue is None and sm == 1.0 and vm == 1.0 else recolor(base, hue, sm, vm)
        save(up, "btn_%s.png" % name)
        save(pressed(up), "btn_%s_down.png" % name)

    # 하단 탭: 어두운 나무(평소) / 밝은 나무(선택). 나인패치 8/8/8/9
    save(flatten9(load("tab.png"), 8, 8, 8, 9), "tab.png")
    save(flatten9(load("tab_on.png"), 8, 8, 8, 9), "tab_on.png")

    # 골드 표시 캡슐: 나인패치 L8 R8 T4 B4
    save(flatten9(load("pill.png"), 8, 8, 4, 4), "pill.png")

    # 통째로 쓰는 그림
    save(hollow(load("slot.png")), "slot.png")
    save(load("badge.png"), "badge.png")

    # 어두운 패널(상단 바·스킬 줄): 나인패치 11/11/11/11
    save(flatten9(load("panel_dark.png"), 11, 11, 11, 11), "panel_dark.png")
    # 전투 영역 액자(가운데 투명): 나인패치 10/10/10/10
    save(flatten9(load("frame.png"), 10, 10, 10, 10), "frame.png")
    # 제목 리본: 나인패치 L12 R12 T4 B5
    save(flatten9(load("ribbon.png"), 12, 12, 4, 5), "ribbon.png")
    # 게이지 틀(45x11): 안쪽 홈을 투명하게 뚫어 채움 막대를 먼저 그리고 틀을 덮는다. 나인패치 L6 R6 T5 B5
    save(hollow(flatten9(load("gauge.png"), 6, 6, 5, 5), threshold=200), "gauge.png")


if __name__ == "__main__":
    sys.exit(main())
