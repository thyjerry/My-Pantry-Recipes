#!/usr/bin/env python3
"""v2.1: writes data/recipes-<n>.json from data/recipes.json (same split as the Android APK: 500 first, then about
530 each). The app loads chunk 0, shows it, then appends the rest; chunk 0 carries "chunks": total.
data/recipes.json stays as the one source used by the tests and tools. MIT License."""
import glob, json, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FIRST, REST = 500, 700

def split(rs):
    rest = len(rs) - FIRST
    n = max(1, -(-rest // REST)) if rest > 0 else 0
    sz = -(-rest // n) if n else REST
    return [rs[:FIRST]] + [rs[i:i + sz] for i in range(FIRST, len(rs), sz)]

def main():
    d = json.load(open(os.path.join(ROOT, 'data/recipes.json'), encoding='utf-8'))
    for old in glob.glob(os.path.join(ROOT, 'data/recipes-*.json')):
        os.remove(old)
    parts = split(d['recipes'])
    out = []
    for i, p in enumerate(parts):
        root = {'license': d.get('license', ''), 'recipes': p}
        if i == 0:
            root['chunks'] = len(parts)
        s = json.dumps(root, ensure_ascii=False, separators=(',', ':'))
        open(os.path.join(ROOT, 'data/recipes-%d.json' % i), 'w', encoding='utf-8').write(s)
        out.append(s)
    print('chunks:', [len(p) for p in parts])
    return out

if __name__ == '__main__':
    main()
