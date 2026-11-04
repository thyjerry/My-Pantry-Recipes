#!/usr/bin/env python3
"""v2.1: writes only recipes/84-*.txt and up from the fam_z*.py files (the older files were hand-tuned after
generation, so build.py must not be re-run over them)."""
import glob, importlib, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib, v21lib
for f in sorted(glob.glob(os.path.join(lib.HERE, "fam_z*.py"))):
    importlib.import_module(os.path.basename(f)[:-3])
lib.write_all()
