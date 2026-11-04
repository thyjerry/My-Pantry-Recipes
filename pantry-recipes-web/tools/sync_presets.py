#!/usr/bin/env python3
"""Copies the theme presets from the Java core (Palette.java) into js/core15.js so both apps list the same themes."""
import re, os
here = os.path.dirname(os.path.abspath(__file__))
j = open(os.path.join(here, '../../pantry-recipes/src/org/pantryrecipes/app/core/Palette.java')).read()
rows = re.findall(r'p\("([^"]+)", (LIGHT|DARK|"[^"]+"), "([^"]+)", (0x\w+), (0x\w+), (0x\w+), (0x\w+), (0x\w+)\)', j)
lines = []
for g, i, n, *c in rows:
    i = {'LIGHT': "'light'", 'DARK': "'dark'"}.get(i, "'" + i.strip('"') + "'")
    lines.append("    base(%s, '%s', %s, '%s')" % (i, n, ', '.join(c), g))
p = os.path.join(here, '../js/core15.js')
s = open(p).read()
a = s.index('  var PRESETS = [\n') + len('  var PRESETS = [\n')
b = s.index('\n  ];', a)
s = s[:a] + ',\n'.join(lines) + s[b:]
open(p, 'w').write(s)
print(len(rows), 'presets')
