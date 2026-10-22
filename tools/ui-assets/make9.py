import sys
from PIL import Image
P=sys.argv[1]
def hx(c): return tuple(int(c[j:j+2],16) for j in (0,2,4))+(255,)
CREAM=hx('fff4dc'); BEVEL=hx('f2d3a2')

# ---------- panel ----------
im=Image.open(P+'/panel_raw.png').convert('RGBA')
x0,y0,x1,y1=im.getbbox()           # 6,3,58,61
im=im.crop((x0,y0,x1,y1)); W,H=im.size
# interior: scan middle row/col for the run of parchment colours
par={hx(c)[:3] for c in ['f2d3a2','fff4dc','d49a6a','ff9eaa']}
def run(pixels):
    idx=[i for i,p in enumerate(pixels) if p[:3] in par]
    return idx[0], idx[-1]
# interior sits between the inner shadow lines (a8704a): x 14..49, y 12..51 in the raw 64x64 image
ix0,ix1=14-x0,49-x0
iy0,iy1=12-y0,51-y0
print('panel',W,H,'interior x',ix0,ix1,'y',iy0,iy1)
for y in range(iy0,iy1+1):
    for x in range(ix0,ix1+1):
        edge = x in (ix0,ix1) or y in (iy0,iy1)
        im.putpixel((x,y), BEVEL if edge else CREAM)
# 9-patch: corner = up to 2px inside the interior, middle = 2px slice from the centre
cl=ix0+2; ct=iy0+2; cr=W-1-ix1+2; cb=H-1-iy1+2
mx=W//2; my=H//2; M=2
cols=list(range(0,cl))+list(range(mx,mx+M))+list(range(W-cr,W))
rows=list(range(0,ct))+list(range(my,my+M))+list(range(H-cb,H))
out=Image.new('RGBA',(len(cols),len(rows)))
for j,y in enumerate(rows):
    for i,x in enumerate(cols):
        out.putpixel((i,j),im.getpixel((x,y)))
out.save(P+'/panel.png'); print('panel.png',out.size,'split L R T B =',cl,cr,ct,cb)

# ---------- button ----------
b=Image.open(P+'/button_raw.png').convert('RGBA')
b=b.crop(b.getbbox()); W,H=b.size
# use the left end mirrored for the right end (the generated right end has scratches)
E=6
left=b.crop((0,0,E,H)); right=left.transpose(Image.FLIP_LEFT_RIGHT)
mid=b.crop((W//2-1,0,W//2+1,H))       # 2px clean middle slice
# clean the left end of scratches: recolour any dark pixel that is not on the outline into the plank colour
out=Image.new('RGBA',(E*2+2,H))
out.paste(left,(0,0)); out.paste(mid,(E,0)); out.paste(right,(E+2,0))
out.save(P+'/button.png'); print('button.png',out.size,'split L R =',E,E,'T B = 4 4')
for name,img in [('panel',Image.open(P+'/panel.png')),('button',out)]:
    img.resize((img.width*10,img.height*10),Image.NEAREST).save(P+'/%s_x10.png'%name)
