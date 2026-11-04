package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v1.8: incremental recipe matching. Keeps the last Matcher.Result of every recipe and an inverted
 * index from ingredient keys (and their families, and the keys their substitutes need) to recipes.
 * When the pantry changes by a few keys only the recipes that can be affected are matched again;
 * the results are identical to a full match (see RecipeTool's MatchCache checks).
 */
public final class MatchCache {
    private Recipe[] recipes = new Recipe[0];
    private Matcher.Result[] results = new Matcher.Result[0];
    private Map<String, int[]> index = new HashMap<String, int[]>();
    private Substitutions subs;
    private Set<String> pantry = new HashSet<String>(), staples = new HashSet<String>(), accepted = new HashSet<String>();
    private Matcher matcher;
    private boolean valid;
    /** Diagnostics: how many recipes the last call matched (all on a full pass). */
    public int lastMatched;
    public boolean lastFull;

    private static final MatchCache SHARED = new MatchCache();

    public static MatchCache shared() { return SHARED; }

    public synchronized Matcher matcher() { return matcher; }

    public synchronized void invalidate() { valid = false; }

    /** Results aligned with {@code list} (same order). The returned list is a fresh copy; the results are shared and must not be modified. */
    public synchronized List<Matcher.Result> match(List<Recipe> list, Collection<String> pantryKeys, Collection<String> stapleKeys,
                                                   Substitutions s, Collection<String> acceptedKeys) {
        Set<String> np = new HashSet<String>(pantryKeys), ns = new HashSet<String>(stapleKeys);
        Set<String> na = acceptedKeys == null ? new HashSet<String>() : new HashSet<String>(acceptedKeys);
        Matcher m = new Matcher(np, ns).withSubstitutions(s, na);
        boolean sameRecipes = valid && list.size() == recipes.length;
        if (sameRecipes) for (int i = 0; i < recipes.length; i++) if (list.get(i) != recipes[i]) { sameRecipes = false; break; }
        if (!sameRecipes || s != subs || !na.equals(accepted)) {
            recipes = list.toArray(new Recipe[0]);
            subs = s;
            buildIndex();
            results = new Matcher.Result[recipes.length];
            for (int i = 0; i < recipes.length; i++) results[i] = m.match(recipes[i]);
            lastMatched = recipes.length;
            lastFull = true;
        } else {
            Set<String> changed = new HashSet<String>();
            symDiff(pantry, np, changed);
            symDiff(staples, ns, changed);
            boolean[] hit = new boolean[recipes.length];
            int n = 0;
            for (String c : changed) {
                n += mark(c, hit);
                String f = Normalizer.family(c);
                if (f != null) n += mark(f, hit);
            }
            for (int i = 0; i < hit.length; i++) if (hit[i]) results[i] = m.match(recipes[i]);
            lastMatched = n;
            lastFull = false;
        }
        pantry = np;
        staples = ns;
        accepted = na;
        matcher = m;
        valid = true;
        List<Matcher.Result> out = new ArrayList<Matcher.Result>(results.length);
        for (Matcher.Result r : results) out.add(r);
        return out;
    }

    private int mark(String key, boolean[] hit) {
        int[] ids = index.get(key);
        if (ids == null) return 0;
        int n = 0;
        for (int i : ids) if (!hit[i]) { hit[i] = true; n++; }
        return n;
    }

    private static void symDiff(Set<String> a, Set<String> b, Set<String> out) {
        for (String k : a) if (!b.contains(k)) out.add(k);
        for (String k : b) if (!a.contains(k)) out.add(k);
    }

    private void buildIndex() {
        Map<String, List<Integer>> tmp = new HashMap<String, List<Integer>>();
        for (int i = 0; i < recipes.length; i++) {
            Set<String> deps = new HashSet<String>();
            for (Ingredient ing : recipes[i].ingredients) {
                if (ing.key.isEmpty()) continue;
                dep(deps, ing.key);
                if (subs != null) {
                    Substitutions.Entry e = subs.forKey(ing.key);
                    if (e != null) for (Substitutions.Option o : e.options) for (List<String> g : o.needs) for (String k : g) dep(deps, k);
                }
            }
            for (String d : deps) {
                List<Integer> l = tmp.get(d);
                if (l == null) tmp.put(d, l = new ArrayList<Integer>());
                l.add(i);
            }
        }
        index = new HashMap<String, int[]>(tmp.size() * 2);
        for (Map.Entry<String, List<Integer>> e : tmp.entrySet()) {
            List<Integer> l = e.getValue();
            int[] a = new int[l.size()];
            for (int j = 0; j < a.length; j++) a[j] = l.get(j);
            index.put(e.getKey(), a);
        }
    }

    private static void dep(Set<String> deps, String k) {
        deps.add(k);
        String f = Normalizer.family(k);
        if (f != null) deps.add(f);
    }
}
