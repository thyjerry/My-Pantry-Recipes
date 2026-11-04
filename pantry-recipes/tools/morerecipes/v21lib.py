"""v2.1: helpers for the v2.1 recipe files (recipes/84-*.txt and up). build_v21.py writes only these files;
titles (and near-duplicate titles) already used in any older recipe file are skipped."""
import glob, os, re
import lib
for f in sorted(glob.glob(os.path.join(lib.RDIR, "*.txt"))):
    if os.path.basename(f) < "84":
        for line in open(f, encoding="utf-8"):
            if line.startswith("# "):
                t = line[2:].strip(); lib.EXISTING.add(lib.norm(t)); lib.NEAR.setdefault(lib.near(t), t)
lib.BOOK.clear()
def R(file, title, cat, cuisine, prep, cook, serv, meals, ings, steps):
    assert file[:2] >= "84", file
    lib.R(file, title, cat, cuisine, prep, cook, serv, meals, ings, steps)
