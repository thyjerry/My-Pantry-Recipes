#!/usr/bin/env python3
"""v2.2: dinner mains generator. Writes recipes/95-v22-<protein>.txt only (older files are untouched, so ids stay stable).
Every recipe = one cuisine flavour set x one cut whose cooking method suits it; steps carry cut-specific times and doneness."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib, v22lib
from v22cuts import CUTS
from v22cuisines import A
SHAPED = ("Meatballs", "Kofta", "Köfte", "Patties", "Kebabs", "Kebab", "Burgers")
def low(n): return n.lower()
def steps_for(m, cut, v, cz):
    name, grp, line, done, prm = cut
    _, label, dish, ings, s, q, f = v
    lc = low(name); ar = cz["arom"][1]; serve = cz["serve"]; p = prm[m]
    if m == "stirfry":
        bd = "browned outside but still a little pink inside" if grp == "beef" else done
        st = ["Slice the %s into thin strips (if not already), toss with the cornstarch and a pinch of salt and leave while you prep" % lc if grp != "shrimp" else "Pat the shrimp dry and toss with the cornstarch and a pinch of salt",
              "For the sauce, stir together %s" % q,
              "Heat the oil in a wok or large frying pan over high heat until shimmering. Stir-fry the %s in two batches, about %d minutes per batch, until %s; tip onto a plate" % (lc, p, bd),
              "Stir-fry %s with %s for 2 to 3 minutes" % (s, ar),
              "Return the %s with any juices, pour in the sauce and toss for 1 minute until glossy and hot. %s" % (lc, f),
              "Serve with %s" % serve]
        return st, 15, 2 * p + 6, ["1 tsp|cornstarch", "2 tbsp|vegetable oil"]
    if m == "braise":
        if grp == "fish":
            st = ["Cook %s in 2 tbsp oil over medium heat for 5 minutes until soft, then stir in %s for 1 minute" % (ar, s),
                  "Add %s and simmer 15 minutes so the flavours come together" % q,
                  "Season the cod with salt, nestle it into the sauce, cover and poach gently 8 to 10 minutes until %s" % done,
                  "%s. Taste for salt" % f, "Serve with %s" % serve]
            return st, 15, 35, ["2 tbsp|vegetable oil"]
        b, sim, tender = p
        h = "%d minutes" % sim if sim < 90 else ("%s hours" % {90: "1½", 120: "2", 150: "2½", 180: "3"}[sim])
        st = ["Pat the %s dry and season well with salt and %s" % (lc, s),
              "Heat 2 tbsp oil in a heavy pot over medium-high heat and brown the %s in batches, about %d minutes in total; set aside" % (lc, b),
              "Lower the heat and cook %s for 6 to 8 minutes until soft" % ar,
              "Add %s, scrape up the browned bits and return the meat. Cover and simmer gently for about %s (or bake covered at 160 °C), until %s%s" % (q, h, tender, " and " + done if grp == "chicken" else ""),
              "%s. Taste for salt" % f, "Serve with %s" % serve]
        return st, 20, b + 10 + sim + 5, ["2 tbsp|vegetable oil"]
    if m == "roast":
        T, mins, mar = p
        st = ["Mix %s with 2 tbsp oil and 1 tsp salt. Coat the %s and leave to marinate %s in the fridge" % (s, lc, mar)]
        if T == "grill":
            st += ["Heat a grill, barbecue or ridged grill pan to medium-high. %s" % q.replace("Grill hot and fast; ", "").replace("Grill hot and fast", "Shake off excess marinade").replace("lay on a lined tray", "cook on the grill").replace("Lay on a rack over a lined tray", "Lay on the hot grill").replace("Spread on a lined tray", "Lay on the hot grill"),
                   "Grill about %d minutes per side, turning once, until %s" % (mins, done),
                   "Rest 5 minutes. %s" % f, "Serve with %s" % serve]
            return st, 15, 2 * mins + 8, ["2 tbsp|vegetable oil"]
        st += ["Heat the oven to %d °C (%d °C fan). %s" % (T, T - 20, q.replace("Grill hot and fast; ", "").replace("Grill hot and fast", "Lay on a rack over a lined tray")),
               "Roast for about %d minutes, until %s%s" % (mins, done, "; for a darker finish, grill the top for the last 2 to 3 minutes" if grp == "chicken" else ""),
               "Rest 5 minutes. %s" % f, "Serve with %s" % serve]
        return st, 15, mins + 5, ["2 tbsp|vegetable oil"]
    if m == "skillet":
        ps, how = p
        st = ["Pat the %s dry and season with salt and %s" % (lc, s),
              "Heat 1 tbsp oil in a large frying pan over medium-high heat. %s, until %s. Rest on a warm plate" % (how[0].upper() + how[1:], done),
              "Lower the heat and, in the same pan, cook %s for 1 minute, then %s" % (ar, q) if not q.startswith("(") else "Meanwhile, %s" % q.strip("()").replace(") ", ", "),
              "Return the %s with its resting juices and turn in the sauce. %s" % (lc, f) if not q.startswith("(") else f,
              "Serve with %s" % serve]
        return st, 10, 2 * ps + 8, ["1 tbsp|vegetable oil"]
    if m == "ground":
        shaped = any(w in dish for w in SHAPED) or "meatball" in q
        if shaped:
            st = ["Mix the %s with %s and 1 tsp salt; with wet hands shape into %s" % (lc, s, "16 balls" if "ball" in dish.lower() else "8 patties or logs"),
                  "Brown in 2 tbsp oil over medium-high heat in batches, about 8 minutes, turning, until %s" % done if "simmer" not in q else "Brown in 2 tbsp oil over medium-high heat in batches, about 6 minutes, turning; they finish cooking in the sauce",
                  None if ("onion" in s and "simmer" not in q) else "Cook %s gently in the same pan for 5 minutes" % ar if not q.startswith("(") else "Meanwhile prepare %s" % q.strip("()"),
                  ("Add %s, until the meat is %s" % (q, done)) if "simmer" in q else ("Stir together %s" % q),
                  "%s" % f, "Serve with %s" % serve]
            return st, 20, 25, ["2 tbsp|vegetable oil"]
        st = ["Heat 2 tbsp oil in a large frying pan over medium-high heat and cook %s for 5 minutes" % ar,
              "Add the %s and %s and cook, breaking it up, for 6 to 8 minutes until %s" % (lc, s, done),
              "Stir in %s" % q, "%s" % f]
        if "Serve" not in f: st.append("Serve with %s" % serve)
        return st, 10, 25, ["2 tbsp|vegetable oil"]
FILES = {"chicken": "95-v22-chicken.txt", "beef": "96-v22-beef.txt", "pork": "97-v22-pork.txt", "lamb": "98-v22-lamb.txt",
         "turkey": "99-v22-turkey.txt", "fish": "99-v22b-fish.txt", "shrimp": "99-v22c-shrimp.txt"}
n = 0
for ck, cz in A.items():
    for v in cz["v"]:
        m = v[0]
        for key, cut in CUTS.items():
            name, grp, line = cut[0], cut[1], cut[2]
            if m not in cut[4] or grp in cz["no"]: continue
            if m == "ground" and v[2] == "Tofu" and grp == "turkey": continue
            title = "%s %s %s %s" % (cz["adj"], v[1], name, v[2])
            d = v[2]
            if d == "Skillet": title = "%s %s %s" % (cz["adj"], v[1], name)
            elif d == "Grill": title = "Grilled %s %s %s" % (cz["adj"], v[1], name)
            elif d == "Roast": title = "Roast %s %s %s" % (cz["adj"], v[1], name)
            elif d == "Braise": title = "%s %s Braised %s" % (cz["adj"], v[1], name)
            title = title.replace("Style Style", "Style")
            if len(title) > 60: title = "%s %s %s" % (v[1], name, v[2])
            if len(title) > 60: continue
            st, prep, cook, extra = steps_for(m, cut, v, cz)
            st = [x[0].upper() + x[1:] for x in st if x]
            st = [x.replace(". mix", ". Mix").replace(". thread", ". Thread") for x in st]
            for i, x in enumerate(st):
                if ". " in x:
                    a, b2 = x.split(". ", 1); st[i] = a + ". " + b2[0].upper() + b2[1:]
            if len(st) > 1 and st[-1].startswith("Serve with") and "Serve" in st[-2]: st.pop()
            raw = [line] + [x for x in v[3].split("; ")] + cz["arom"][0].split("; ") + extra
            ings, seen = [], set()
            for x in raw:
                b = x.split("|")[-1].split(",")[0].split(" (")[0].strip().lower().rstrip("s")
                if b in seen: continue
                seen.add(b); ings.append(x)
            v22lib.R(FILES[grp], title, "Main", cz["tag"], prep, cook, 4, "Dinner, Main dish", ings, st)
lib.write_all()
