package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Compares recipes with the user's pantry. */
public final class Matcher {
    private final Set<String> have = new HashSet<String>();      // pantry + staples keys
    private final Set<String> general = new HashSet<String>();   // families of what we have
    private final Set<String> staples = new HashSet<String>();
    private final Set<String> staplesGeneral = new HashSet<String>();

    public Matcher(Collection<String> pantryKeys, Collection<String> stapleKeys) {
        for (String k : pantryKeys) add(k, have, general);
        for (String k : stapleKeys) {
            add(k, have, general);
            add(k, staples, staplesGeneral);
        }
    }

    private static void add(String k, Set<String> exact, Set<String> gen) {
        if (k == null || k.isEmpty()) return;
        exact.add(k);
        String f = Normalizer.family(k);
        if (f != null) gen.add(f);
    }

    private static boolean covers(String key, Set<String> exact, Set<String> gen) {
        if (exact.contains(key)) return true;                 // same ingredient
        String f = Normalizer.family(key);
        if (f != null && exact.contains(f)) return true;      // pantry "pasta" covers "spaghetti"
        return gen.contains(key);                             // pantry "chicken breast" covers "chicken"
    }

    public boolean has(String key) { return covers(key, have, general); }

    public boolean isStaple(String key) { return covers(key, staples, staplesGeneral); }

    private Substitutions subs;
    private Set<String> accepted = new HashSet<String>();

    /** Lets match() suggest substitutes; keys in {@code acceptedKeys} count as matched when a swap is on hand. */
    public Matcher withSubstitutions(Substitutions s, Collection<String> acceptedKeys) {
        subs = s;
        accepted = acceptedKeys == null ? new HashSet<String>() : new HashSet<String>(acceptedKeys);
        return this;
    }

    private Collection<String> swapDislikes, swapAvoid, swapDiets;
    public Matcher withSwapAvoid(Collection<String> d, Collection<String> a, Collection<String> diets) { swapDislikes=d; swapAvoid=a; swapDiets=diets; return this; }
    public boolean optionAllowed(Substitutions.Option o) { return SwapAdvice.caution(o, swapDislikes, swapAvoid, swapDiets).isEmpty(); }
    public Substitutions substitutions() { return subs; }

    public boolean isAccepted(String key) { return accepted.contains(key); }

    public boolean pantryEmpty() { return have.size() == staples.size(); }

    /** Result of matching one recipe. */
    public static final class Result {
        public final Recipe recipe;
        public int total;          // required (non-optional) ingredients
        public int have;           // required ingredients in pantry or staples
        public int haveNonStaple;  // required ingredients matched by real pantry items
        public final List<Ingredient> missing = new ArrayList<Ingredient>();
        /** Missing ingredients the pantry can stand in for (not counted as matched). */
        public final List<Ingredient> swappable = new ArrayList<Ingredient>();
        /** Ingredients counted as matched because the user accepted a substitute. */
        public final List<Ingredient> substituted = new ArrayList<Ingredient>();

        Result(Recipe r) { recipe = r; }

        public int missingCount() { return missing.size(); }

        public double ratio() { return total == 0 ? 0 : (double) have / total; }
    }

    public Result match(Recipe r) {
        Result res = new Result(r);
        Set<String> seen = new HashSet<String>();
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty() || !seen.add(i.key)) continue;
            res.total++;
            if (has(i.key)) {
                res.have++;
                if (!isStaple(i.key)) res.haveNonStaple++;
            } else if (subs != null && !subs.availableFor(i.key, this).isEmpty()) {
                if (accepted.contains(i.key)) {
                    res.have++;
                    res.haveNonStaple++;
                    res.substituted.add(i);
                } else {
                    res.missing.add(i);
                    res.swappable.add(i);
                }
            } else {
                res.missing.add(i);
            }
        }
        return res;
    }

    /** Best match first: fewer missing among relevant recipes, then higher ratio, then more matched. */
    public static int compare(Result a, Result b) {
        boolean ra = a.haveNonStaple > 0, rb = b.haveNonStaple > 0;
        if (ra != rb) return ra ? -1 : 1;
        boolean ca = a.missingCount() == 0, cb = b.missingCount() == 0;
        if (ca != cb) return ca ? -1 : 1;
        int c = Double.compare(b.ratio(), a.ratio());
        if (c != 0) return c;
        c = a.missingCount() - b.missingCount();
        if (c != 0) return c;
        c = b.haveNonStaple - a.haveNonStaple;
        if (c != 0) return c;
        return a.recipe.title.compareToIgnoreCase(b.recipe.title);
    }
}
