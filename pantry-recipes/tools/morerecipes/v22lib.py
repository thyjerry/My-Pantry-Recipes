"""v2.2: helpers for the v2.2 dinner files (recipes/95-*.txt and up). build_v22.py writes only these files;
titles (and near-duplicate titles) already used in any older recipe file are skipped, so ids b:1..b:2620 stay put."""
import glob, os
import lib
for f in sorted(glob.glob(os.path.join(lib.RDIR, "*.txt"))):
    if os.path.basename(f) < "95":
        for line in open(f, encoding="utf-8"):
            if line.startswith("# "):
                t = line[2:].strip(); lib.EXISTING.add(lib.norm(t)); lib.NEAR.setdefault(lib.near(t), t)
lib.BOOK.clear()
def R(file, title, cat, cuisine, prep, cook, serv, meals, ings, steps):
    assert file[:2] >= "95", file
    lib.R(file, title, cat, cuisine, prep, cook, serv, meals, ings, steps)
