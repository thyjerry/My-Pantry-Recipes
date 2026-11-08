#!/usr/bin/env python3
"""v2.2 theme redesign: tints the backgrounds of light presets (so options differ clearly) and adds two bold groups.
Writes the preset lines into Palette.java and js/core15.js. Run from the repo root (pantry-recipes)."""
import re, sys
def lum(c):
    def ch(v):
        v/=255; return v/12.92 if v<=0.03928 else ((v+0.055)/1.055)**2.4
    return 0.2126*ch(c>>16&255)+0.7152*ch(c>>8&255)+0.0722*ch(c&255)
def con(a,b):
    la,lb=lum(a),lum(b); return (max(la,lb)+0.05)/(min(la,lb)+0.05)
def mix(a,b,t):
    return sum(int(round(((a>>s)&255)+(((b>>s)&255)-((a>>s)&255))*t))<<s for s in (16,8,0))
KEEP={'light','contrast','paper','sepia','citrine-light','frostbite','daylight'}
RETINT={'Light':0.80,'Colorful':0.74,'Pastel':0.84,'Warm':0.78,'Cool':0.78,'Earthy':0.80}
# Bold bright: bright saturated background, lighter card of the same hue, near-black text, a deep complementary primary.
BRIGHT=[('bold-saffron','Saffron',0xFFC93C,0x2D2A8C,0xB3261E),('bold-coral','Coral reef',0xFF8A65,0x123C69,0x00695C),
 ('bold-aqua','Aqua splash',0x4FE3CF,0x5B1A8C,0xB3261E),('bold-lime','Lime zest',0xC6F432,0x5A1E8C,0x0D47A1),
 ('bold-bubblegum','Bubblegum',0xFF9BCB,0x0B4F6C,0x5B2C00),('bold-sky','Sky blue',0x7CC8FF,0x8C1D40,0x4A3B00),
 ('bold-tangerine','Tangerine',0xFFA53D,0x1B3A8C,0x6A1B5A),('bold-lilac','Lilac',0xC9A7FF,0x1F5130,0x7A2E00),
 ('bold-mint','Fresh mint',0x7EF0B0,0x6A1B4D,0x0D3B8C),('bold-lemon','Lemonade',0xFFF06A,0x7A1F3D,0x0B4F6C),
 ('bold-peach','Peach fizz',0xFFB38A,0x214A6B,0x5E2A84),('bold-turquoise','Turquoise',0x3DD6E0,0x7A1F1F,0x3E2A84)]
# Bold deep: deep saturated background, a slightly lighter card, white text, a bright complementary primary.
DEEP=[('deep-cobalt','Cobalt',0x163C9E,0xFFC145,0x7CF0C8),('deep-emerald','Emerald',0x0B5E3C,0xFF9E6B,0xFFE07A),
 ('deep-crimson','Crimson',0x8E1B2C,0x7FE3D0,0xFFD166),('deep-violet','Violet',0x4B1F8C,0xC6F432,0xFF9BCB),
 ('deep-teal','Deep teal',0x07595C,0xFFB4A2,0xFFE38A),('deep-indigo','Indigo night',0x26236B,0xFF8FB1,0x8EF0E0),
 ('deep-burgundy','Burgundy',0x5C1030,0x9BE7FF,0xF7D774),('deep-forest','Pine',0x1F4D2B,0xFFB7D5,0xFFE082),
 ('deep-navy','Navy',0x0F2A52,0xFF9F43,0x7FE7FF),('deep-plum','Plum wine',0x4E1442,0xA8F0B5,0xFFC48A),
 ('deep-rust','Rust',0x7A2E12,0x8FD8FF,0xFFE48A),('deep-ocean','Ocean deep',0x053F6E,0xFFD36E,0xFF9AA8)]
def bright(id,name,bg,pri,acc):
    sur=mix(bg,0xFFFFFF,0.55); txt=0x141414
    return ('Bold bright',id,name,bg,sur,pri,acc,txt)
def deep(id,name,bg,pri,acc):
    sur=mix(bg,0xFFFFFF,0.10); txt=0xFFFFFF
    return ('Bold deep',id,name,bg,sur,pri,acc,txt)
def check(t):
    g,id,name,bg,sur,pri,acc,txt=t
    errs=[]
    if min(con(txt,bg),con(txt,sur))<4.5: errs.append('text %.2f'%min(con(txt,bg),con(txt,sur)))
    if con(pri,sur)<3.0: errs.append('primary %.2f'%con(pri,sur))
    if con(bg,sur)<1.04: errs.append('bg~surface')
    return errs
LINE=re.compile(r'p\("([^"]+)", (LIGHT|DARK|"[^"]+"), "([^"]+)", 0x(\w{6}), 0x(\w{6}), 0x(\w{6}), 0x(\w{6}), 0x(\w{6})\)')
JS=re.compile(r"base\((LIGHT|DARK|'[^']+'), '([^']+)', 0x(\w{6}), 0x(\w{6}), 0x(\w{6}), 0x(\w{6}), 0x(\w{6}), '([^']+)'\)")
pj='src/org/pantryrecipes/app/core/Palette.java'; s=open(pj).read()
changed={}
def fix(m):
    g,idr,name=m.group(1),m.group(2),m.group(3); bg,sur,pri,acc,txt=[int(m.group(i),16) for i in range(4,9)]
    id=idr.strip('"').lower() if idr.startswith('"') else idr.lower()
    if g in RETINT and id not in KEEP and lum(bg)>=0.18:
        t=RETINT[g]; nb=mix(pri,0xFFFFFF,t); ns=mix(pri,0xFFFFFF,min(0.95,t+0.13))
        for _ in range(12):
            if min(con(txt,nb),con(txt,ns))>=4.5: break
            nb=mix(nb,0xFFFFFF,0.2); ns=mix(ns,0xFFFFFF,0.2)
        if min(con(txt,nb),con(txt,ns))<4.5 or con(pri,ns)<3.0: return m.group(0)
        changed[id]=(nb,ns)
        return 'p("%s", %s, "%s", 0x%06X, 0x%06X, 0x%06X, 0x%06X, 0x%06X)'%(g,idr,name,nb,ns,pri,acc,txt)
    return m.group(0)
s=LINE.sub(fix,s)
new=[bright(*x) for x in BRIGHT]+[deep(*x) for x in DEEP]
bad=[(t[1],check(t)) for t in new if check(t)]
if bad: print('BAD',bad); sys.exit(1)
if 'bold-saffron' not in s:
    jl='\n            // ---- v2.2: bold backgrounds (tools/themes/gen22.py) ----\n'+',\n'.join('            p("%s", "%s", "%s", 0x%06X, 0x%06X, 0x%06X, 0x%06X, 0x%06X)'%t for t in new)+','
    s=s.replace('            // v2.0 "Game day"',jl+'\n            // v2.0 "Game day"',1)
    s=s.replace('"Paper & sepia", "Editor classics", "Game day"));','"Paper & sepia", "Editor classics", "Bold bright", "Bold deep", "Game day"));',1)
open(pj,'w').write(s)
pw='../pantry-recipes-web/js/core15.js'; w=open(pw).read()
def fixjs(m):
    idr=m.group(1); id=idr.strip("'") if idr.startswith("'") else idr.lower()
    if id in changed:
        nb,ns=changed[id]
        return "base(%s, '%s', 0x%06X, 0x%06X, 0x%s, 0x%s, 0x%s, '%s')"%(idr,m.group(2),nb,ns,m.group(5),m.group(6),m.group(7),m.group(8))
    return m.group(0)
w=JS.sub(fixjs,w)
if 'bold-saffron' not in w:
    jl='\n    // ---- v2.2: bold backgrounds (tools/themes/gen22.py) ----\n'+''.join("    base('%s', '%s', 0x%06X, 0x%06X, 0x%06X, 0x%06X, 0x%06X, '%s'),\n"%(t[1],t[2],t[3],t[4],t[5],t[6],t[7],t[0]) for t in new)
    i=w.index("    base('gd-"); w=w[:i]+jl.lstrip('\n')+w[i:]
    w=w.replace("'Paper & sepia', 'Editor classics', 'Game day'];","'Paper & sepia', 'Editor classics', 'Bold bright', 'Bold deep', 'Game day'];",1)
open(pw,'w').write(w)
print('retinted',len(changed),'added',len(new))
