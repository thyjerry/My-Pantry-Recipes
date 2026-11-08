package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * v2.2: seeded variety. One small deterministic RNG (FNV-1a seed + mulberry32) so "today's picks" are the same all
 * day and change at midnight, on Android and the web (js/core22.js is a line-for-line port, checked by vectors).
 * Everything here is O(n) over an already-filtered list: callers pass the results that passed the user's filters.
 */
public final class Variety {
    private Variety() {}

    static String cz(Matcher.Result r) { return r.recipe.cuisine == null ? "" : r.recipe.cuisine.trim(); }

    /** FNV-1a 32-bit over the UTF-16 code units. */
    public static int seed(String s) {
        int h = 0x811C9DC5;
        for (int i = 0; i < s.length(); i++) { h ^= s.charAt(i); h *= 0x01000193; }
        return h;
    }

    /** Seed for a calendar day plus a salt (row name, shuffle count). */
    public static int daySeed(int y, int m, int d, String salt) {
        return seed(y + "-" + (m < 10 ? "0" : "") + m + "-" + (d < 10 ? "0" : "") + d + "|" + salt);
    }

    /** mulberry32; same 32-bit results as the JS port (Math.imul). */
    public static final class Rng {
        int a;
        public Rng(int seed) { a = seed; }
        public int nextInt() {
            a += 0x6D2B79F5;
            int t = a;
            t = (t ^ (t >>> 15)) * (t | 1);
            t ^= t + (t ^ (t >>> 7)) * (t | 61);
            return t ^ (t >>> 14);
        }
        /** 0..n-1 */
        public int next(int n) { return (int) ((nextInt() & 0xFFFFFFFFL) % n); }
    }

    /** Fisher-Yates on a copy. */
    public static <T> List<T> shuffle(Collection<T> items, int seed) {
        List<T> out = new ArrayList<T>(items);
        Rng r = new Rng(seed);
        for (int i = out.size() - 1; i > 0; i--) { int j = r.next(i + 1); Collections.swap(out, i, j); }
        return out;
    }

    /** n picks in a seeded order (stable for a day: input is sorted by id first so list order does not matter). */
    public static List<Matcher.Result> picks(List<Matcher.Result> results, int seed, int n) {
        List<Matcher.Result> s = new ArrayList<Matcher.Result>(results);
        Collections.sort(s, (x, y) -> x.recipe.id.compareTo(y.recipe.id));
        s = shuffle(s, seed);
        return s.size() > n ? new ArrayList<Matcher.Result>(s.subList(0, n)) : s;
    }

    /** One pick, or null when the list is empty. Never repeats {@code avoidId} when there is another choice. */
    public static Matcher.Result one(List<Matcher.Result> results, int seed, String avoidId) {
        if (results.isEmpty()) return null;
        List<Matcher.Result> p = picks(results, seed, 2);
        if (p.size() > 1 && avoidId != null && p.get(0).recipe.id.equals(avoidId)) return p.get(1);
        return p.get(0);
    }

    /** Cuisines with at least {@code min} recipes, in a seeded order: the rotating "cuisine of the day" rows. */
    public static List<String> rotatingCuisines(List<Matcher.Result> results, int seed, int k, int min) {
        TreeMap<String, Integer> n = new TreeMap<String, Integer>();
        for (Matcher.Result r : results) {
            String c = cz(r);
            if (c.isEmpty()) continue;
            Integer v = n.get(c); n.put(c, v == null ? 1 : v + 1);
        }
        List<String> ok = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : n.entrySet()) if (e.getValue() >= min) ok.add(e.getKey());
        List<String> s = shuffle(ok, seed);
        return s.size() > k ? new ArrayList<String>(s.subList(0, k)) : s;
    }

    public static List<Matcher.Result> inCuisine(List<Matcher.Result> results, String cuisine) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : results) if (cuisine.equals(cz(r))) out.add(r);
        return out;
    }

    /**
     * "Try something new": an unopened recipe from the cuisine you have opened least (ties broken by the seed).
     * Falls back to any unopened recipe, then to any recipe.
     */
    public static Matcher.Result tryNew(List<Matcher.Result> results, Collection<String> seen, int seed) {
        if (results.isEmpty()) return null;
        Map<String, int[]> per = new HashMap<String, int[]>(); // cuisine -> {opened, unopened}
        for (Matcher.Result r : results) {
            String c = cz(r);
            int[] v = per.get(c); if (v == null) per.put(c, v = new int[2]);
            if (seen.contains(r.recipe.id)) v[0]++; else v[1]++;
        }
        List<String> cs = new ArrayList<String>(per.keySet());
        Collections.sort(cs);
        cs = shuffle(cs, seed);
        String best = null; int bo = Integer.MAX_VALUE;
        for (String c : cs) { int[] v = per.get(c); if (v[1] > 0 && v[0] < bo) { bo = v[0]; best = c; } }
        List<Matcher.Result> pool = new ArrayList<Matcher.Result>();
        if (best != null) for (Matcher.Result r : results) if (best.equals(cz(r)) && !seen.contains(r.recipe.id)) pool.add(r);
        if (pool.isEmpty()) for (Matcher.Result r : results) if (!seen.contains(r.recipe.id)) pool.add(r);
        if (pool.isEmpty()) pool = results;
        return one(pool, seed ^ 0x5bd1e995, null);
    }
}
