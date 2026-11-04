#!/usr/bin/env python3
"""Builds the single-file app (CSS, JS, recipe and substitution data, icons inlined) as
dist/my-pantry-recipes-v<version>.html. Open it directly from disk; no server or service worker needed.
The version comes from js/version.js, the one version source. MIT License."""
import base64, json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# the exact <script> block index.html must contain, in load order
SCRIPTS = ['js/version.js', 'js/core-data.js', 'js/icons.js', 'js/core.js', 'js/core15.js', 'js/core16.js', 'js/data16.js', 'js/core17.js', 'js/data17.js', 'js/import15.js', 'js/data20.js', 'js/helpers20.js', 'js/core20.js', 'js/stage3-data.js', 'js/core30.js', 'js/data201.js', 'js/core201.js', 'js/data21.js', 'js/core21.js', 'js/app.js']

def read(p, mode='r'):
    with open(os.path.join(ROOT, p), mode, **({} if 'b' in mode else {'encoding': 'utf-8'})) as f:
        return f.read()

def version():
    m = re.search(r"self\.PR_VERSION\s*=\s*'([0-9]+(?:\.[0-9]+)*(?:-[a-z0-9]+)?)-web'\s*;", read('js/version.js'))
    if not m:
        sys.exit('js/version.js: no PR_VERSION')
    return m.group(1)          # e.g. "1.5" (the app shows "1.5-web")

def script_safe(js):
    # never let inlined code or data close the <script> element early
    return js.replace('</', '<\\/').replace('<!--', '<\\!--')

def data_uri(p):
    return 'data:image/png;base64,' + base64.b64encode(read(p, 'rb')).decode('ascii')

def build(out):
    html = read('index.html')
    import chunk_data
    chunks = chunk_data.main()  # v2.1: recipes ride as small JSON blocks; app.js shows block 0, then appends the rest
    subs = read('data/substitutions.txt')
    embedded = 'window.PR_EMBEDDED = {recipes: null, subs: ' + json.dumps(subs) + '};'
    # v2.0 S3: recipes ride in a non-executed JSON block (HTML-tokenised, not JS-parsed); app.js reads and removes it.
    rjson = ''.join('<script type="application/json" id="pr-recipes-%d">' % i + c.replace('</', '<\\/') + '</script>\n' for i, c in enumerate(chunks))
    # drop the CSP (inline scripts) and the manifest (no service worker / install from a plain file)
    html, n = re.subn(r'<meta http-equiv="Content-Security-Policy"[^>]*>\n', '', html)
    assert n == 1
    html = re.sub(r'<link rel="manifest"[^>]*>\n', '', html)
    html = html.replace('href="icons/apple-touch-icon.png"', 'href="' + data_uri('icons/apple-touch-icon.png') + '"')
    html = html.replace('href="icons/favicon-32.png"', 'href="' + data_uri('icons/favicon-32.png') + '"')
    html = re.sub(r'<link rel="icon" type="image/png" sizes="192x192"[^>]*>\n', '', html)
    css = read('css/app.css')
    html = html.replace('<link rel="stylesheet" href="css/app.css">', '<style>\n' + css.replace('</', '<\\/') + '\n</style>')
    tags = ''.join('<script src="%s"></script>\n' % p for p in SCRIPTS)
    assert tags in html, 'index.html script tags differ from SCRIPTS'
    found = re.findall(r'<script src="([^"]+)"', html)
    assert found == SCRIPTS, found
    scripts = '<script>\n' + script_safe(embedded) + '\n</script>\n' + ''.join('<script>\n' + script_safe(read(p)) + '\n</script>\n' for p in SCRIPTS)
    html = html.replace(tags, rjson + scripts)
    html = html.replace('<title>My Pantry Recipes</title>', '<!-- My Pantry Recipes v%s, single-file build. MIT License. -->\n<title>My Pantry Recipes</title>' % version())
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'w', encoding='utf-8') as f:
        f.write(html)
    n = os.path.getsize(out)
    print('wrote %s (%d bytes, %.2f MB)' % (out, n, n / 1048576.0))
    if n >= 25 * 1024 * 1024:
        sys.exit('too big')
    return out

def main():
    build(os.path.join(ROOT, 'dist', 'my-pantry-recipes-v%s.html' % version()))

if __name__ == '__main__':
    main()
