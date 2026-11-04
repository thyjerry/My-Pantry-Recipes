#!/usr/bin/env python3
"""v2.1: split assets/recipes.json into small chunks for the APK so the first screen can fill after one
small parse (Fire HD 8 first). Chunk 0 is small; later chunks are parsed in the background and appended.
Usage: chunk_recipes.py assets/recipes.json OUT_ASSETS_DIR"""
import json, os, shutil, sys
src, out = sys.argv[1], sys.argv[2]
FIRST, REST = 500, 700
shutil.rmtree(out, ignore_errors=True)
shutil.copytree(os.path.dirname(src), out, ignore=shutil.ignore_patterns('recipes.json', 'recipes-*.json'))
d = json.load(open(src, encoding='utf-8'))
rs = d['recipes']
rest = len(rs) - FIRST
n = max(1, -(-rest // REST))
sz = -(-rest // n) if rest > 0 else REST
parts = [rs[:FIRST]] + [rs[i:i + sz] for i in range(FIRST, len(rs), sz)]
back = []
for i, p in enumerate(parts):
    with open(os.path.join(out, 'recipes-%d.json' % i), 'w', encoding='utf-8') as f:
        json.dump({'license': d.get('license', ''), 'recipes': p}, f, ensure_ascii=False, separators=(',', ':'))
    back += json.load(open(os.path.join(out, 'recipes-%d.json' % i), encoding='utf-8'))['recipes']
assert back == rs, 'chunks do not round-trip'
print('chunks:', len(parts), [len(p) for p in parts],
      [os.path.getsize(os.path.join(out, 'recipes-%d.json' % i)) for i in range(len(parts))])
