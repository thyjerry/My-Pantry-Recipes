package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * v1.8: near-duplicate recipe titles ("Garlic Butter Salmon" / "Garlic Butter Salmon in Foil",
 * "Air Fryer Chicken Wings" / "Crispy Air Fryer Chicken Wings"). The bundled ones are genuinely different
 * recipes, so they are kept and each gets a short hint in the list ("with lemon", "Thai", "45 min").
 * Online results whose title is the same as a bundled recipe's (after folding) are hidden as duplicates.
 */
public final class Titles {
    private Titles() { }

    private static final Set<String> SKIP = new HashSet<String>(java.util.Arrays.asList(
            "the", "a", "an", "and", "with", "of", "in", "on", "for", "style"));
    private static final Set<String> BORING = new HashSet<String>(java.util.Arrays.asList(
            "salt", "black pepper", "pepper", "water", "olive oil", "vegetable oil", "oil", "sugar", "butter", "garlic", "onion"));

    /** The folded words of a title (accents, case and punctuation ignored; filler words dropped). */
    public static Set<String> words(String title) {
        Set<String> out = new LinkedHashSet<String>();
        String f = Normalizer.fold(title == null ? "" : title).toLowerCase(Locale.ROOT);
        for (String w : f.split("[^a-z0-9]+")) if (!w.isEmpty() && !SKIP.contains(w)) out.add(w);
        return out;
    }

    /** The key used for exact duplicates: the title's words in order. */
    public static String key(String title) {
        StringBuilder b = new StringBuilder();
        for (String w : words(title)) { if (b.length() > 0) b.append(' '); b.append(w); }
        return b.toString();
    }

    public static boolean near(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        int inter = 0;
        for (String w : a) if (b.contains(w)) inter++;
        int union = a.size() + b.size() - inter;
        if (inter * 4 >= union * 3) return true;                     // Jaccard ≥ 0.75
        Set<String> s = a.size() <= b.size() ? a : b;
        return inter == s.size() && s.size() >= 3 && union - inter <= 2; // "X Y Z" vs "X Y Z in Foil"
    }

    /** recipe id → hint for every recipe whose title is a near-duplicate of another one in the list. */
    public static Map<String, String> hints(List<Recipe> recipes) {
        int n = recipes.size();
        List<Set<String>> ws = new ArrayList<Set<String>>(n);
        Map<String, List<Integer>> byWord = new HashMap<String, List<Integer>>();
        for (int i = 0; i < n; i++) {
            Set<String> w = words(recipes.get(i).title);
            ws.add(w);
            for (String x : w) {
                List<Integer> l = byWord.get(x);
                if (l == null) byWord.put(x, l = new ArrayList<Integer>());
                l.add(i);
            }
        }
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        boolean[] any = new boolean[n];
        for (int i = 0; i < n; i++) {
            Set<Integer> cand = new HashSet<Integer>();
            for (String x : ws.get(i)) for (int j : byWord.get(x)) if (j > i) cand.add(j);
            for (int j : cand) if (near(ws.get(i), ws.get(j))) { any[i] = any[j] = true; union(parent, i, j); }
        }
        Map<Integer, List<Integer>> groups = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < n; i++) if (any[i]) {
            int r = find(parent, i);
            List<Integer> g = groups.get(r);
            if (g == null) groups.put(r, g = new ArrayList<Integer>());
            g.add(i);
        }
        Map<String, String> out = new HashMap<String, String>();
        for (List<Integer> g : groups.values()) {
            for (int i : g) {
                Recipe r = recipes.get(i);
                String h = hint(r, g, recipes);
                if (h != null) out.put(r.id, h);
            }
        }
        return out;
    }

    private static String hint(Recipe r, List<Integer> group, List<Recipe> all) {
        Set<String> others = new HashSet<String>();
        Set<String> otherCuisines = new HashSet<String>();
        Set<Integer> otherTimes = new HashSet<Integer>();
        for (int j : group) {
            Recipe o = all.get(j);
            if (o == r) continue;
            for (Ingredient i : o.ingredients) others.add(i.key);
            otherCuisines.add(o.cuisine);
            otherTimes.add(o.totalMinutes());
        }
        Set<String> tw = words(r.title);
        for (Ingredient i : r.ingredients) {
            if (i.optional || others.contains(i.key) || BORING.contains(i.key) || i.key.isEmpty()) continue;
            if (inTitle(tw, i.key)) continue; // already in the title
            return "with " + i.key;
        }
        if (!r.cuisine.isEmpty() && !otherCuisines.contains(r.cuisine)) return r.cuisine;
        int t = r.totalMinutes();
        if (t > 0 && !otherTimes.contains(t)) return t >= 60 ? t / 60 + " h" + (t % 60 == 0 ? "" : " " + t % 60 + " min") : t + " min";
        return null;
    }

    private static String stem(String w) { return w.length() > 3 && w.endsWith("s") ? w.substring(0, w.length() - 1) : w; }

    private static boolean inTitle(Set<String> titleWords, String key) {
        Set<String> t = new HashSet<String>();
        for (String w : titleWords) t.add(stem(w));
        for (String w : words(key)) if (!t.contains(stem(w))) return false;
        return true;
    }

    private static int find(int[] p, int i) { while (p[i] != i) { p[i] = p[p[i]]; i = p[i]; } return i; }

    private static void union(int[] p, int a, int b) { a = find(p, a); b = find(p, b); if (a != b) p[Math.max(a, b)] = Math.min(a, b); }
}
