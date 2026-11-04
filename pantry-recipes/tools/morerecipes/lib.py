"""Helpers for writing the v1.4 recipe files (recipes/30-*.txt and up).
Every recipe is written out in the same plain-text format as the hand-written files; the families in
fam_*.py share step wording but every variant has its own ingredients, quantities, times and details."""
import os, re, glob

ORDER = ["Breakfast", "Brunch", "Lunch", "Dinner", "Main dish", "Side dish", "Appetizer", "Snack", "Soup", "Salad",
         "Dessert", "Baked goods", "Drink", "Sauce/Condiment"]
CATS = {"Breakfast", "Main", "Soup", "Salad", "Side", "Snack", "Dessert", "Baking", "Drink", "Sauce"}
HERE = os.path.dirname(os.path.abspath(__file__))
RDIR = os.path.join(HERE, "..", "..", "recipes")

def norm(t): return re.sub(r"[^a-z0-9]+", "", t.lower())

EXISTING = set()
for f in glob.glob(os.path.join(RDIR, "*.txt")):
    if re.match(r".*/[0-2]\d-", f):
        for line in open(f, encoding="utf-8"):
            if line.startswith("# "): EXISTING.add(norm(line[2:].strip()))

STAPLES = {"salt", "black pepper", "olive oil", "vegetable oil", "water"}
SEEN = set()
ERRORS = []
BOOK = {}
SKIP_DUP_FILES = set()  # v1.7: files where a title that already exists is skipped (counted) instead of failing
SKIPPED = []   # file -> list of recipe text blocks
NEAR = {}      # v1.7: near-duplicate signature -> first title
NEAR_SKIPPED = []
_FILLER = set("style inspired classic easy quick homemade simple the with and a of in on for best perfect my one two "
              "traditional authentic basic everyday weeknight".split())
def near(t):
    t = re.sub(r"\([^)]*\)", " ", t.lower()).replace("-", " ")
    w = [x for x in re.sub(r"[^a-z0-9 ]+", " ", t).split() if x not in _FILLER]
    w = [x[:-1] if len(x) > 3 and x.endswith("s") and not x.endswith("ss") else x for x in w]
    return " ".join(sorted(w))
for f in sorted(glob.glob(os.path.join(RDIR, "*.txt"))):
    if re.match(r".*/[0-4]\d-", f):
        for line in open(f, encoding="utf-8"):
            if line.startswith("# "): NEAR.setdefault(near(line[2:].strip()), line[2:].strip())

def R(file, title, cat, cuisine, prep, cook, serv, meals, ings, steps):
    assert cat in CATS, (title, cat)
    if isinstance(meals, str): meals = [m.strip() for m in meals.split(",") if m.strip()]
    for m in meals: assert m in ORDER, (title, m)
    if file[:2] >= "50":
        need = {"Baking": "Baked goods", "Sauce": "Sauce/Condiment", "Snack": "Snack", "Main": "Main dish",
                "Soup": "Soup", "Salad": "Salad", "Side": "Side dish", "Dessert": "Dessert", "Drink": "Drink", "Breakfast": "Breakfast"}[cat]
        if need not in meals: meals = meals + [need]
        if "Main dish" in meals and "Lunch" not in meals and "Dinner" not in meals: meals = meals + ["Dinner"]
        if cat in ("Side", "Dessert", "Drink", "Sauce"): meals = [m for m in meals if m != "Main dish"]
        if serv > 24:
            if isinstance(steps, str): steps = [x.strip() for x in steps.split(" / ") if x.strip()]
            steps = list(steps) + ["Makes about %d pieces (listed here as 24 servings so scaling stays simple)" % serv]
            serv = 24
    meals = [m for m in ORDER if m in meals]
    n = norm(title)
    if n in EXISTING or n in SEEN:
        if file in SKIP_DUP_FILES or file[:2] >= "50": SKIPPED.append(title); return
        ERRORS.append("duplicate title: " + title); return
    assert 3 <= len(title) <= 60, title
    if file[:2] >= "50":
        sig = near(title)
        if sig in NEAR and NEAR[sig] != title:
            NEAR_SKIPPED.append(title + " ~ " + NEAR[sig]); return
        NEAR.setdefault(sig, title)
    SEEN.add(n)
    if isinstance(ings, str): ings = [x.strip() for x in ings.split(";") if x.strip()]
    if isinstance(steps, str): steps = [x.strip() for x in steps.split(" / ") if x.strip()]
    assert len(ings) >= 3 and len(steps) >= 2, title
    out = ["# " + title, "%s | %s | %d | %d | %d | %s" % (cat, cuisine, prep, cook, serv, ", ".join(meals))]
    names = set()
    for i in ings:
        p = [x.strip() for x in i.split("|")]
        if len(p) == 1: p = ["", p[0]]
        base = p[1].split(",")[0].strip().lower()
        if base in names:
            if base in STAPLES: continue
            ERRORS.append("duplicate ingredient '%s' in %s" % (base, title)); continue
        names.add(base)
        line = "- " + (p[0] + " " if p[0] else "") + "| " + p[1]
        if len(p) > 2 and p[2] == "opt": line += " | opt"
        out.append(line)
    for s in steps:
        s = s.strip()
        if not s.endswith((".", "!", "?")): s += "."
        out.append("> " + s[0].upper() + s[1:])
    BOOK.setdefault(file, []).append("\n".join(out))

def write_all():
    total = 0
    for f, blocks in sorted(BOOK.items()):
        with open(os.path.join(RDIR, f), "w", encoding="utf-8") as fh:
            fh.write("\n\n".join(blocks) + "\n")
        total += len(blocks)
        print("%-32s %4d" % (f, len(blocks)))
    print("new recipes:", total)
    if SKIPPED: print("skipped as duplicates of existing recipes (%d): %s" % (len(SKIPPED), ", ".join(SKIPPED)))
    if NEAR_SKIPPED: print("skipped as near-duplicates (%d): %s" % (len(NEAR_SKIPPED), "; ".join(NEAR_SKIPPED)))
    for e in ERRORS: print("ERROR", e)
    if ERRORS: raise SystemExit(1)

# ---------------- v1.7: Instant Pot / electric pressure cooker recipes ----------------
BUILD_UP = 12  # minutes an electric pressure cooker typically needs to come up to pressure (not part of the cook time)

def IP(file, title, cat, cuisine, prep, serv, meals, ings, saute_min, saute, liquid, add, minutes, release, finish,
       pressure="High", pre=None, finish_min=3, steam=False):
    """Writes one Instant Pot recipe with the standard step order: sauté, deglaze (minimum liquid), seal,
    pressure cook (time at pressure only), release (natural / quick), finish."""
    steps = []
    if pre: steps += pre if isinstance(pre, list) else [pre]
    if saute_min and saute:
        steps.append("Select Sauté and heat the pot. " + saute)
    if liquid:
        steps.append("Pour in " + liquid + " and scrape the bottom of the inner pot clean, so nothing is stuck and the pot doesn't show a burn warning. "
                     "An electric pressure cooker needs at least 1 cup (250 ml) of thin liquid to come up to pressure")
    if add: steps.append(add)
    steps.append("Lock the lid and turn the valve to Sealing. Pressure cook on %s for %d minutes. The pot first takes about 10 to 15 minutes to come up to pressure; that is not part of the cook time" % (pressure, minutes))
    if release == "Q":
        steps.append("Quick release the pressure by turning the valve to Venting, keeping your hands and face away from the steam, then open the lid when the float valve drops")
        rel = 2
    elif release == "N":
        steps.append("Let the pressure release naturally for about 15 minutes, until the float valve drops, then open the lid")
        rel = 15
    else:
        n = int(release[1:])
        steps.append("Let the pressure release naturally for %d minutes, then quick release the rest and open the lid carefully" % n)
        rel = n
    if finish: steps += finish if isinstance(finish, list) else [x.strip() for x in finish.split(" / ") if x.strip()]
    cook = (saute_min or 0) + BUILD_UP + minutes + rel + finish_min
    R(file, title, cat, cuisine, prep, cook, serv, meals, ings, steps)
