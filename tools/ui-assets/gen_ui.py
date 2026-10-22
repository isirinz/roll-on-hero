import sys
from PIL import Image
P=sys.argv[1]; OUT=P+'/out'
def hx(c): return tuple(int(c[j:j+2],16) for j in (0,2,4))+(255,)
O=hx('2b1d2e')  # outline
CLEAR=(0,0,0,0)

def rounded_mask(w,h,r):
    """픽셀아트 둥근 모서리: 위아래 몇 줄을 안쪽으로 들여 그린다."""
    insets={4:[3,1,1],3:[2,1],2:[1]}.get(r,[])
    m=[[False]*w for _ in range(h)]
    for y in range(h):
        k = insets[y] if y < len(insets) else (insets[h-1-y] if h-1-y < len(insets) else 0)
        for x in range(k, w-k): m[y][x]=True
    return m

def button(fill, light, shade, dark, pressed=False, w=16, h=16, r=4, depth=3):
    """나무판 버튼: 외곽선 + 윗면 하이라이트 + 아래 두께(depth). 눌림은 두께 없이 2px 내려간다."""
    im=Image.new('RGBA',(w,h),CLEAR)
    top = depth-1 if pressed else 0
    body_h = h - top
    m=rounded_mask(w,body_h,r)
    for y in range(body_h):
        for x in range(w):
            if not m[y][x]: continue
            edge = (x==0 or x==w-1 or y==0 or y==body_h-1 or
                    not m[y][x-1] or not m[y][x+1] or not m[y-1][x] or not m[y+1][x])
            yy=y+top
            if edge: c=O
            elif not pressed and y >= body_h-1-depth: c=dark if y==body_h-2 else shade
            elif pressed and y >= body_h-2: c=shade
            elif y==1: c=light
            else: c=fill
            im.putpixel((x,yy),c)
    return im

STYLES={
  'cream':('f2d3a2','fff4dc','d49a6a','a8704a'),
  'pink': ('ff9eaa','ffd6dc','e8607a','b04060'),
  'gold': ('ffd36e','fff4dc','f0a830','a8704a'),
  'green':('8fd16a','d8f5c0','4f9e5a','2f6a3a'),
  'blue': ('7ec8f0','e0f4ff','4a7fc8','2f4f8f'),
  'off':  ('7a4f3a','a8704a','4a3245','2b1d2e'),
}
for name,(f,l,s,d) in STYLES.items():
    button(hx(f),hx(l),hx(s),hx(d)).save(OUT+'/btn_%s.png'%name)
    button(hx(f),hx(l),hx(s),hx(d),pressed=True).save(OUT+'/btn_%s_down.png'%name)

def bar(fill, light, w=8, h=8, r=2):
    """게이지 채움: 외곽선 + 윗줄 하이라이트. fill=None이면 배경칸."""
    im=Image.new('RGBA',(w,h),CLEAR)
    m=rounded_mask(w,h,r)
    for y in range(h):
        for x in range(w):
            if not m[y][x]: continue
            edge=(x==0 or x==w-1 or y==0 or y==h-1 or not m[y][x-1] or not m[y][x+1] or not m[y-1][x] or not m[y+1][x])
            im.putpixel((x,y), O if edge else (light if y==1 else fill))
    return im
bar(hx('4a3245'),hx('4a3245')).save(OUT+'/bar_back.png')
bar(hx('e8607a'),hx('ff9eaa')).save(OUT+'/bar_hp.png')
bar(hx('4a7fc8'),hx('7ec8f0')).save(OUT+'/bar_skill.png')
bar(hx('f0a830'),hx('ffd36e')).save(OUT+'/bar_gold.png')
bar(hx('4f9e5a'),hx('8fd16a')).save(OUT+'/bar_green.png')

# panel (cream) + frame (transparent centre) from make9 output
pan=Image.open(P+'/panel.png'); pan.save(OUT+'/panel.png')
fr=pan.copy()
CREAM=hx('fff4dc'); BEV=hx('f2d3a2')
for y in range(fr.height):
    for x in range(fr.width):
        if 10<=x<fr.width-10 and 11<=y<fr.height-11: fr.putpixel((x,y),CLEAR)
# also clear the 1px bevel ring just inside the frame
for y in range(fr.height):
    for x in range(fr.width):
        if fr.getpixel((x,y)) in (CREAM,BEV): fr.putpixel((x,y),CLEAR)
fr.save(OUT+'/frame.png')

# preview sheet
sheet=Image.new('RGBA',(200,120),hx('a8704a'))
xx=4
for name in STYLES:
    sheet.paste(Image.open(OUT+'/btn_%s.png'%name),(xx,4),Image.open(OUT+'/btn_%s.png'%name))
    sheet.paste(Image.open(OUT+'/btn_%s_down.png'%name),(xx,24),Image.open(OUT+'/btn_%s_down.png'%name))
    xx+=20
for i,n in enumerate(['back','hp','skill','gold','green']):
    b=Image.open(OUT+'/bar_%s.png'%n); sheet.paste(b,(4+i*12,48),b)
sheet.paste(pan,(4,62),pan); sheet.paste(fr,(40,62),fr)
sheet.resize((sheet.width*5,sheet.height*5),Image.NEAREST).save(P+'/sheet_x5.png')
print('ok')
