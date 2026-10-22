"""PixelLab 캐릭터 스프라이트시트를 내려받아 assets/sprites/<id>/<동작>.png 가로 띠와 sprites/index.json으로 바꾼다.
사용: python3 tools/sprites/convert_chars.py <프로젝트 루트> [id ...]

- 전투에 쓰는 한 방향(chars_list.json의 direction)의 애니메이션만 쓴다. 대기(idle)가 없으면 그 방향 정지 그림 1장으로 대신한다.
- 모든 동작을 같은 크기로 자르되, 그림이 칸 아래쪽에 붙도록(발이 바닥에 오도록) 위아래 여백을 잘라낸다.
  PixelLab 시트는 모든 칸이 같은 기준점(칸 가운데)이라 동작 사이 위치가 어긋나지 않는다.
"""
import io, json, os, re, sys, time, urllib.request, zipfile
from PIL import Image

DURATION = {'idle': 0.14, 'attack': 0.075, 'hurt': 0.08, 'cast': 0.08, 'death': 0.1}
LOOP = {'idle'}

root = sys.argv[1]; only = set(sys.argv[2:])
here = os.path.dirname(os.path.abspath(__file__))
lst = json.load(open(os.path.join(here, 'chars_list.json')))
sprites_dir = os.path.join(root, 'assets', 'sprites')
index_path = os.path.join(sprites_dir, 'index.json')
index = json.load(open(index_path)) if os.path.exists(index_path) else {}

def fetch(url):
    for attempt in range(5):
        try:
            return urllib.request.urlopen(url, timeout=60).read()
        except Exception as e:
            print('  retry', e); time.sleep(3)
    raise SystemExit('download failed: ' + url)

for cid, cfg in lst.items():
    if only and cid not in only: continue
    z = zipfile.ZipFile(io.BytesIO(fetch('https://api.pixellab.ai/mcp/characters/%s/spritesheet' % cfg['characterId'])))
    meta = json.load(z.open([n for n in z.namelist() if n.endswith('.json')][0]))
    sheet = Image.open(z.open([n for n in z.namelist() if n.endswith('.png')][0])).convert('RGBA')
    ss = meta['spritesheet']; cw, ch = ss['cell_size']['width'], ss['cell_size']['height']
    direction = cfg['direction']
    anims = {}
    for row in ss['rows']:
        if row['type'] == 'rotations':
            col = row['directions'].index(direction)
            anims.setdefault('_still', [sheet.crop((col * cw, row['row'] * ch, col * cw + cw, row['row'] * ch + ch))])
        elif row['type'] == 'animation' and row.get('direction') == direction:
            # 같은 이름의 애니메이션이 여러 방향 그룹에 있으면 PixelLab이 _2, _3을 붙인다 → 떼어 낸다
            name = re.sub(r'_\d+$', '', row['animation'])
            anims[name] = [sheet.crop((i * cw, row['row'] * ch, i * cw + cw, row['row'] * ch + ch)) for i in range(row['frame_count'])]
    if 'idle' not in anims: anims['idle'] = anims['_still']
    # 다시 만든 공격(strike, 10프레임·제자리 복귀)이 있으면 그걸 공격으로 쓴다.
    # chars_list.json의 "attack"에 다른 애니메이션 이름(예: strike2)을 적으면 그걸 쓴다.
    pick = cfg.get('attack', 'strike')
    if pick in anims: anims['attack'] = anims[pick]
    for k in [k for k in anims if k.startswith('strike')]: anims.pop(k)
    # 피격·사망·대기도 다시 만든 것(예: "hurt": "hurt2")을 고를 수 있다
    for slot in ('hurt', 'death', 'idle'):
        alt = cfg.get(slot)
        if alt and alt in anims: anims[slot] = anims.pop(alt)
    for k in [k for k in anims if re.fullmatch(r'(hurt|death|idle)\d+', k)]: anims.pop(k)
    anims.pop('_still', None)
    # 공통 자르기 범위: 모든 프레임의 내용이 다 들어가게, 가로는 가운데 기준 대칭
    top, bottom, half = ch, 0, 0
    cx = cw // 2
    for frames in anims.values():
        for f in frames:
            b = f.getbbox()
            if not b: continue
            top = min(top, b[1]); bottom = max(bottom, b[3])
            half = max(half, cx - b[0], b[2] - cx)
    top = max(0, top - 1); half = min(cx, half + 1)
    box = (cx - half, top, cx + half, bottom)
    fw, fh = box[2] - box[0], box[3] - box[1]
    out = os.path.join(sprites_dir, cid); os.makedirs(out, exist_ok=True)
    entry = {'frameWidth': fw, 'frameHeight': fh, 'scale': cfg.get('scale', 3), 'facesRight': direction == 'east', 'anims': {}}
    for name, frames in anims.items():
        strip = Image.new('RGBA', (fw * len(frames), fh), (0, 0, 0, 0))
        for i, f in enumerate(frames): strip.paste(f.crop(box), (i * fw, 0))
        strip.save(os.path.join(out, name + '.png'))
        dur = DURATION.get(name, 0.1)
        if name == 'attack': dur = round(min(dur, 0.62 / len(frames)), 3)  # 프레임이 많아도 공격 한 번은 약 0.6초
        entry['anims'][name] = {'frames': len(frames), 'duration': dur, 'loop': name in LOOP}
    index[cid] = entry
    print(cid, 'cell', cw, 'x', ch, '-> frame', fw, 'x', fh, sorted(entry['anims']))
json.dump(index, open(index_path, 'w'), indent=1, sort_keys=True)
