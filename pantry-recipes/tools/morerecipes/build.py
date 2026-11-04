#!/usr/bin/env python3
"""Regenerates recipes/30-*.txt ... from the fam_*.py family files."""
import glob, importlib, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib
# v2.1: do not re-run; files 30-80 were hand-tuned after generation. New recipes: build_v21.py
sys.exit("build.py is retired (v2.1): use build_v21.py")
for f in sorted(glob.glob(os.path.join(lib.HERE, "fam_*.py"))):
    if os.path.basename(f).startswith("fam_z"): continue
    importlib.import_module(os.path.basename(f)[:-3])
lib.write_all()
