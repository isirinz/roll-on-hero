"""앱 아이콘 마스터 합성: PixelLab 흉상(64px) + 픽셀 배경. 그리드 단위로 그린 뒤 정수배 확대."""
import math, sys
from PIL import Image, ImageDraw

SUBJ = 'tools/icon/knight_bust.png'  # PixelLab create_image_pro 64px 후보 중 하나

# 배경 색 (게임 UI 팔레트)
BANDS = [(255, 228, 140), (255, 211, 110), (246, 176, 82), (226, 132, 62), (176, 82, 58), (112, 50, 52), (74, 36, 46)]
RAY = 0.06


def background(n, cx=None, cy=None, span=1.0):
    """n x n 그리드 배경: 계단식 원형 그라데이션 + 은은한 광선."""
    cx = n / 2 if cx is None else cx
    cy = n * 0.46 if cy is None else cy
    im = Image.new('RGBA', (n, n))
    px = im.load()
    rmax = math.hypot(n / 2, n / 2) * span
    for y in range(n):
        for x in range(n):
            dx, dy = x + .5 - cx, y + .5 - cy
            r = math.hypot(dx, dy) / rmax
            ang = math.atan2(dy, dx)
            ray = 1 if (int((ang + math.pi) / (2 * math.pi) * 16) % 2 == 0) else 0
            t = r * 1.15 - ray * RAY
            i = min(len(BANDS) - 1, max(0, int(t * len(BANDS))))
            px[x, y] = BANDS[i] + (255,)
    return im


def subject():
    s = Image.open(SUBJ).convert('RGBA')
    return s.crop(s.getbbox())


def compose(n, subj_scale=1, oy=0):
    """n 그리드 캔버스 가운데에 흉상을 놓은 전체 아이콘."""
    bg = background(n)
    s = subject()
    if subj_scale != 1:
        s = s.resize((s.width * subj_scale, s.height * subj_scale), Image.NEAREST)
    x = (n - s.width) // 2
    y = n - s.height + oy  # 흉상은 아래 가장자리에 붙인다
    bg.alpha_composite(s, (x, y))
    return bg


def up(im, k):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


if __name__ == '__main__':
    full = compose(80, oy=0)              # 80 그리드
    big = up(full, 13).crop((8, 8, 1032, 1032))  # 1040 -> 1024
    big.convert('RGB').save(sys.argv[2] if len(sys.argv) > 2 else 'master_1024.png')
