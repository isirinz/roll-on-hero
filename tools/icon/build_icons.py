"""모든 플랫폼 아이콘 생성. out/ 아래에 프로젝트와 같은 경로로 쓴다."""
import json, os, sys
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))  # 저장소 루트에서: python3 tools/icon/build_icons.py
import make_icon as M

OUT = '.'
FONT_BOLD = 'assets/fonts/Galmuri11-Bold.ttf'


def save(im, path):
    p = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p)


def rounded(im, r):
    m = Image.new('L', im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, im.width - 1, im.height - 1], int(im.width * r), fill=255)
    o = im.convert('RGBA')
    o.putalpha(m)
    return o


master = M.up(M.compose(80), 13).crop((8, 8, 1032, 1032)).convert('RGB')
save(master, 'docs/icon/icon_1024.png')

# ---------- iOS: 불투명 정사각형, 이름 그대로 덮어쓰기 ----------
ios = 'ios/data/Media.xcassets/AppIcon.appiconset'
sizes = {
    'app-store-icon-1024@1x.png': 1024, 'ipad-app-icon-76@1x.png': 76, 'ipad-app-icon-76@2x.png': 152,
    'ipad-notifications-icon-20@1x.png': 20, 'ipad-notifications-icon-20@2x.png': 40,
    'ipad-pro-app-icon-83.5@2x.png': 167, 'ipad-settings-icon-29@1x.png': 29, 'ipad-settings-icon-29@2x.png': 58,
    'ipad-spotlight-icon-40@1x.png': 40, 'ipad-spotlight-icon-40@2x.png': 80, 'iphone-app-icon-60@2x.png': 120,
    'iphone-app-icon-60@3x.png': 180, 'iphone-notification-icon-20@2x.png': 40,
    'iphone-notification-icon-20@3x.png': 60, 'iphone-spotlight-icon-40@2x.png': 80,
    'iphone-spotlight-icon-40@3x.png': 120, 'iphone-spotlight-settings-icon-29@2x.png': 58,
    'iphone-spotlight-settings-icon-29@3x.png': 87,
}
for name, s in sizes.items():
    save(master.resize((s, s), Image.LANCZOS), f'{ios}/{name}')

# iOS 실행 화면 로고(libGDX 로고 자리): 제목 글자
def title_logo(scale):
    w, h = 243 * scale, 41 * scale
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(FONT_BOLD, 30 * scale)
    d.text((w / 2, h / 2), 'Roll On, Hero!', font=f, fill=(255, 211, 110, 255), anchor='mm',
           stroke_width=3 * scale, stroke_fill=(43, 29, 46, 255))
    return im
logo = 'ios/data/Media.xcassets/Logo.imageset'
for k in (1, 2, 3):
    save(title_logo(k), f'{logo}/logo@{k}x.png')
json.dump({"images": [{"idiom": "universal", "filename": f"logo@{k}x.png", "scale": f"{k}x"} for k in (1, 2, 3)],
           "info": {"version": 1, "author": "xcode"}},
          open(os.path.join(OUT, logo, 'Contents.json'), 'w'), indent=2)

# ---------- Android ----------
dens = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
# 레거시(API 25 이하): 둥근 사각형 전체 아이콘 48dp
for q, k in dens.items():
    s = int(48 * k)
    save(rounded(master.resize((s, s), Image.LANCZOS), 0.18), f'android/res/drawable-{q}/ic_launcher.png')
# 적응형(API 26+): 108dp 캔버스, 가운데 72dp가 보이고 66dp가 안전 영역
G = 108
bg = M.background(G, cy=G * 0.47, span=72 / 108)
fg = Image.new('RGBA', (G, G), (0, 0, 0, 0))
subj = M.subject()
fg.alpha_composite(subj, ((G - subj.width) // 2, 92 - subj.height))
for q, k in dens.items():
    s = int(G * k)
    rs = lambda im: im.resize((s, s), Image.NEAREST)
    save(rs(fg), f'android/res/drawable-{q}/ic_launcher_fg.png')
    save(rs(bg), f'android/res/drawable-{q}/ic_launcher_bg.png')
os.makedirs(f'{OUT}/android/res/drawable-anydpi-v26', exist_ok=True)
open(f'{OUT}/android/res/drawable-anydpi-v26/ic_launcher.xml', 'w').write(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
    '    <background android:drawable="@drawable/ic_launcher_bg"/>\n'
    '    <foreground android:drawable="@drawable/ic_launcher_fg"/>\n'
    '</adaptive-icon>\n')
save(master.resize((512, 512), Image.LANCZOS), 'android/ic_launcher-web.png')  # Play 스토어용 512

# 적응형 미리보기(원/둥근사각형 마스크)
prev = Image.new('RGB', (3 * 260 + 20, 280), (235, 235, 235))
full = bg.copy(); full.alpha_composite(fg)
full = full.resize((432, 432), Image.NEAREST).crop((72, 72, 360, 360))  # 보이는 72dp
for i, shape in enumerate(['circle', 'squircle', 'square']):
    m = Image.new('L', full.size, 0); d = ImageDraw.Draw(m)
    if shape == 'circle': d.ellipse([0, 0, 287, 287], fill=255)
    elif shape == 'squircle': d.rounded_rectangle([0, 0, 287, 287], 80, fill=255)
    else: d.rounded_rectangle([0, 0, 287, 287], 24, fill=255)
    t = full.resize((240, 240), Image.LANCZOS); m = m.resize((240, 240), Image.LANCZOS)
    prev.paste(t, (20 + i * 260, 20), m)
prev.save('docs/icon/android_adaptive_preview.png')

# ---------- 데스크톱 창 아이콘 ----------
for s in (128, 64, 32, 16):
    save(master.resize((s, s), Image.LANCZOS), f'lwjgl3/src/main/resources/icon{s}.png')
print('ok')
