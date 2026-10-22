"""PixelLab VFX 스프라이트시트(PNG+JSON)를 내려받아 assets/fx/<이름>.png 가로 띠와 fx/index.json으로 바꾼다.
사용: python3 tools/sprites/convert_fx.py <프로젝트 루트>   (fx_list.json의 url 기준)"""
import json, os, sys, time, urllib.request
from PIL import Image
root = sys.argv[1]
here = os.path.dirname(os.path.abspath(__file__))
lst = json.load(open(os.path.join(here, 'fx_list.json')))
out_dir = os.path.join(root, 'assets', 'fx'); os.makedirs(out_dir, exist_ok=True)
raw_dir = os.path.join(here, 'raw_fx'); os.makedirs(raw_dir, exist_ok=True)
index = {}
for name, cfg in lst.items():
    png = os.path.join(raw_dir, name + '.png'); js = os.path.join(raw_dir, name + '.json')
    for src, dst in ((cfg['url'] + '.png', png), (cfg['url'] + '.json', js)):
        for attempt in range(4):
            if os.path.exists(dst): break
            try:
                urllib.request.urlretrieve(src, dst + '.part'); os.replace(dst + '.part', dst)
            except OSError as e:
                print('retry', src, e); time.sleep(2)
    sheet = Image.open(png).convert('RGBA'); meta = json.load(open(js))
    frames = meta['frames']
    fw, fh = frames[0]['sourceSize']['w'], frames[0]['sourceSize']['h']
    strip = Image.new('RGBA', (fw * len(frames), fh), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        r = f['frame']; s = f['spriteSourceSize']
        strip.paste(sheet.crop((r['x'], r['y'], r['x'] + r['w'], r['y'] + r['h'])), (i * fw + s['x'], s['y']))
    strip.save(os.path.join(out_dir, name + '.png'))
    dur = sum(f.get('duration', 60) for f in frames) / len(frames) / 1000.0
    pv = frames[0].get('pivot', {'x': 0.5, 'y': 0.5})
    if not (0 <= pv['x'] <= 1 and 0 <= pv['y'] <= 1): pv = {'x': 0.5, 'y': 0.5}  # 가끔 잘못된 값이 온다
    index[name] = {'frameWidth': fw, 'frameHeight': fh, 'frames': len(frames), 'duration': round(dur, 4),
                   'scale': cfg['scale'], 'pivotX': pv['x'], 'pivotY': round(1.0 - pv['y'], 4)}  # y는 아래 기준
    print(name, fw, fh, len(frames), round(dur, 3), pv)
json.dump(index, open(os.path.join(out_dir, 'index.json'), 'w'), indent=1)
