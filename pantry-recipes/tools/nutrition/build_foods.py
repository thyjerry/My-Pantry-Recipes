#!/usr/bin/env python3
"""Builds assets/foods.tsv (per-100 g kcal/protein/carbs/fat plus gram conversions) from the USDA FoodData
Central SR Legacy CSV release (public domain). Usage:
  python3 build_foods.py <dir with FoodData_Central_sr_legacy_food_csv_2018-04> <freq.txt> <out.tsv>
freq.txt lists the recipe ingredient keys (key frequency table made from the bundled recipes); queries.json maps a
key to an SR Legacy description (exact), '#fdcId', or a word search; '-' = deliberately unmapped."""
import csv, json, os, re, sys
D = os.path.join(sys.argv[1], 'FoodData_Central_sr_legacy_food_csv_2018-04') + '/'
foods = {}
for r in csv.DictReader(open(D + 'food.csv', encoding='utf-8')): foods[r['fdc_id']] = {'desc': r['description'], 'n': {}, 'p': []}
for r in csv.DictReader(open(D + 'food_nutrient.csv', encoding='utf-8')):
    if r['nutrient_id'] in ('1003', '1004', '1005', '1008') and r['fdc_id'] in foods: foods[r['fdc_id']]['n'][r['nutrient_id']] = float(r['amount'])
for r in csv.DictReader(open(D + 'food_portion.csv', encoding='utf-8')):
    if r['fdc_id'] in foods: foods[r['fdc_id']]['p'].append((float(r['amount'] or 1), r['modifier'].strip().lower(), float(r['gram_weight'])))
Q = json.load(open(os.path.join(os.path.dirname(__file__), 'queries.json')))
OV = json.load(open(os.path.join(os.path.dirname(__file__), 'overrides.json')))
def words(s): return re.findall(r'[a-z]+', s.lower())
def stem(w): return w[:-3] + 'y' if w.endswith('ies') else (w[:-2] if w.endswith('oes') else (w[:-1] if w.endswith('s') and not w.endswith('ss') else w))
idx = [(fid, f['desc'], set(stem(w) for w in words(f['desc']))) for fid, f in foods.items() if '1008' in f['n']]
def search(q):
    if q.startswith('#'): return q[1:]
    ex = [fid for fid, d, ws in idx if d.lower() == q.lower()]
    if ex: return ex[0]
    pos = ' '.join(t for t in q.split() if not t.startswith('-'))
    must = [stem(w) for w in words(pos)]
    neg = [stem(w[1:]) for w in q.split() if w.startswith('-')]
    out = []
    for fid, d, ws in idx:
        if all(m in ws for m in must) and not any(m in ws for m in neg):
            first = stem(words(d)[0])
            sc = len(d) - (25 if 'raw' in ws else 0) - (15 if first == must[0] else 0) - (8 if 'fresh' in ws else 0) + (30 if any(b in d for b in ('Babyfood', 'Fast food', 'Restaurant', 'McDONALD', 'KRAFT', 'Infant')) else 0)
            out.append((sc, fid))
    out.sort()
    return out[0][1] if out else None
VOL = [('cup', 236.588), ('tbsp', 14.787), ('tablespoon', 14.787), ('tsp', 4.929), ('teaspoon', 4.929), ('fl oz', 29.574)]
EACH = ['medium', 'fruit', 'whole', 'each', 'large', 'piece', 'tortilla', 'bun', 'roll', 'pita', 'link', 'sausage', 'muffin', 'croissant', 'chop', 'steak', 'fillet', 'breast', 'thigh', 'drumstick', 'wing', 'pepper', 'bagel', 'small']
UNITS = ['clove', 'slice', 'sprig', 'stalk', 'head', 'sheet', 'leaf', 'bunch', 'can']
def portions(fid, key):
    ps = foods[fid]['p']
    gml = 0
    for name, ml in VOL:
        for a, mod, g in ps:
            if mod.startswith(name) and a > 0:
                gml = g / (a * ml); break
        if gml: break
    geach = 0
    pri = (['large'] if key == 'egg' else []) + EACH
    for w in pri:
        for a, mod, g in ps:
            if re.match(r'^(?:[a-z]+ )?' + w + r'\b', mod) and 'cup' not in mod and a > 0:
                geach = g / a; break
        if geach: break
    units = {}
    for u in UNITS:
        for a, mod, g in ps:
            if re.match(u + r's?\b', mod) and a > 0:
                units[u] = round(g / a, 1); break
    return round(gml, 3), round(geach, 1), units
freq = [l.rstrip('\n').split('\t') for l in open(sys.argv[2])]
seen = set(f[3] for f in freq)
freq += [['0', '9', '', k] for k in Q if k not in seen and not k.startswith('_')]  # extra keys only named in queries.json
rows = []
for f in freq:
    key = f[3]
    q = Q.get(key, key)
    if q == '-' or int(f[1]) < 2 and key not in Q: continue
    fid = search(q)
    if not fid: continue
    n = foods[fid]['n']
    if '1008' not in n: continue
    gml, geach, units = portions(fid, key)
    for k, v in OV.get(key, {}).items():
        if k == 'gml': gml = v
        elif k == 'geach': geach = v
        else: units[k] = v
    rows.append([key, '%g' % n.get('1008', 0), '%g' % n.get('1003', 0), '%g' % n.get('1005', 0), '%g' % n.get('1004', 0),
                 '%g' % gml, '%g' % geach, ';'.join('%s=%g' % kv for kv in sorted(units.items())), fid, foods[fid]['desc']])
rows.sort(key=lambda r: r[0])
with open(sys.argv[3], 'w', encoding='utf-8') as o:
    o.write('# Per 100 g: kcal, protein g, carbohydrate g, fat g; g per ml (0 = unknown); g per item (0 = unknown); grams per unit; USDA FDC id; SR Legacy description\n')
    o.write('# Source: USDA FoodData Central, SR Legacy (April 2018 release), public domain. Mapping of recipe ingredients to foods is approximate.\n')
    for r in rows: o.write('\t'.join(r) + '\n')
print(len(rows), 'foods')
