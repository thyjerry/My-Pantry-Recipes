#!/usr/bin/env python3
"""Builds the versioned web deliverables into dist/ (and nothing else there):
  dist/my-pantry-recipes-v<version>.html      single self-contained file
  dist/my-pantry-recipes-web-v<version>.zip   the full PWA source folder (plus that single file)
The version is read from js/version.js. MIT License."""
import os, shutil, sys, zipfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_single

ROOT = build_single.ROOT
SKIP_DIRS = {'dist', '.git', 'node_modules', '__pycache__'}

def main():
    v = build_single.version()
    dist = os.path.join(ROOT, 'dist')
    if os.path.isdir(dist):
        shutil.rmtree(dist)
    os.makedirs(dist)
    # old unversioned outputs must not linger next to the sources
    for old in ('pantry-recipes.html', 'pantry-recipes-web.zip'):
        p = os.path.join(ROOT, old)
        if os.path.exists(p):
            os.remove(p); print('removed', old)
    single = build_single.build(os.path.join(dist, 'my-pantry-recipes-v%s.html' % v))
    zpath = os.path.join(dist, 'my-pantry-recipes-web-v%s.zip' % v)
    top = 'my-pantry-recipes-web-v%s' % v
    n = 0
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for d, dirs, files in os.walk(ROOT):
            dirs[:] = sorted(x for x in dirs if x not in SKIP_DIRS)
            for f in sorted(files):
                if f.endswith('.pyc'):
                    continue
                full = os.path.join(d, f)
                z.write(full, os.path.join(top, os.path.relpath(full, ROOT))); n += 1
        z.write(single, os.path.join(top, os.path.basename(single))); n += 1
    print('wrote %s (%d files, %d bytes)' % (zpath, n, os.path.getsize(zpath)))
    left = sorted(os.listdir(dist))
    assert left == sorted([os.path.basename(single), os.path.basename(zpath)]), left

if __name__ == '__main__':
    main()
