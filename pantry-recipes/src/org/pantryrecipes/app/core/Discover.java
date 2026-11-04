package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Pure ranking helpers behind the Discover tab (kept here so the test harness can check them). */
public final class Discover {
    private Discover() { }

    public static final int QUICK_MINUTES = 30;

    private static final Set<String> BORING = new HashSet<String>(java.util.Arrays.asList(
            "salt", "black pepper", "water", "oil", "olive oil", "vegetable oil", "sugar", "flour", "butter", "garlic", "onion"));

    /** Ingredient keys that say something about a recipe (no salt/pepper/oil), plus their families. */
    public static Set<String> signature(Recipe r) {
        Set<String> s = new HashSet<String>();
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty() || BORING.contains(i.key)) continue;
            s.add(i.key);
            String f = Normalizer.family(i.key);
            if (f != null) s.add(f);
        }
        return s;
    }

    /** 0..1-ish similarity: ingredient Jaccard plus a bonus for the same cuisine or category. */
    public static double similarity(Recipe a, Recipe b) {
        return similarity(signature(a), a, signature(b), b);
    }

    static double similarity(Set<String> sa, Recipe a, Set<String> sb, Recipe b) {
        int inter = 0;
        for (String k : sa) if (sb.contains(k)) inter++;
        int union = sa.size() + sb.size() - inter;
        double j = union == 0 ? 0 : (double) inter / union;
        if (!a.cuisine.isEmpty() && a.cuisine.equalsIgnoreCase(b.cuisine)) j += 0.15;
        if (!a.category.isEmpty() && a.category.equalsIgnoreCase(b.category)) j += 0.10;
        return j;
    }

    /** Recipes most like the favourites (favourites themselves excluded), best first. */
    public static List<Recipe> basedOnFavorites(Collection<Recipe> all, Collection<Recipe> favorites, int limit) {
        final Map<Recipe, Double> score = new HashMap<Recipe, Double>();
        if (favorites.isEmpty()) return new ArrayList<Recipe>();
        Set<String> favIds = new HashSet<String>();
        List<Set<String>> favSig = new ArrayList<Set<String>>();
        List<Recipe> favList = new ArrayList<Recipe>(favorites);
        for (Recipe f : favList) { favIds.add(f.id); favSig.add(signature(f)); }
        for (Recipe r : all) {
            if (favIds.contains(r.id)) continue;
            Set<String> sr = signature(r);
            double best = 0;
            for (int i = 0; i < favList.size(); i++) best = Math.max(best, similarity(sr, r, favSig.get(i), favList.get(i)));
            if (best >= 0.12) score.put(r, best);
        }
        List<Recipe> out = new ArrayList<Recipe>(score.keySet());
        Collections.sort(out, new Comparator<Recipe>() {
            @Override public int compare(Recipe x, Recipe y) {
                int c = Double.compare(score.get(y), score.get(x));
                return c != 0 ? c : x.title.compareTo(y.title);
            }
        });
        return out.size() > limit ? new ArrayList<Recipe>(out.subList(0, limit)) : out;
    }

    private static List<Matcher.Result> sorted(List<Matcher.Result> in) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>(in);
        Collections.sort(out, new Comparator<Matcher.Result>() {
            @Override public int compare(Matcher.Result a, Matcher.Result b) { return Matcher.compare(a, b); }
        });
        return out;
    }

    private static List<Matcher.Result> cap(List<Matcher.Result> l, int limit) {
        return l.size() > limit ? new ArrayList<Matcher.Result>(l.subList(0, limit)) : l;
    }

    /** Recipes never opened, best pantry match first. */
    public static List<Matcher.Result> newForYou(List<Matcher.Result> results, Set<String> seenIds, int limit) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : sorted(results)) if (!seenIds.contains(r.recipe.id)) out.add(r);
        return cap(out, limit);
    }

    /** Known total time of at most {@link #QUICK_MINUTES}, best match first. */
    public static List<Matcher.Result> quick(List<Matcher.Result> results, int limit) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : sorted(results)) {
            int t = r.recipe.totalMinutes();
            if (t > 0 && t <= QUICK_MINUTES) out.add(r);
        }
        return cap(out, limit);
    }

    /** Recipes using the pantry that are missing only one or two ingredients. */
    public static List<Matcher.Result> almostThere(List<Matcher.Result> results, int limit) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : sorted(results)) {
            int m = r.missingCount();
            if (m >= 1 && m <= 2 && r.haveNonStaple > 0) out.add(r);
        }
        return cap(out, limit);
    }

    /** Distinct non-empty cuisines (or categories) with counts, alphabetical. */
    public static Map<String, Integer> facet(Collection<Recipe> all, boolean cuisine) {
        Map<String, Integer> m = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        for (Recipe r : all) {
            String v = cuisine ? r.cuisine : r.category;
            if (v == null || v.trim().isEmpty() || v.equalsIgnoreCase("unknown")) continue;
            Integer n = m.get(v.trim());
            m.put(v.trim(), n == null ? 1 : n + 1);
        }
        return m;
    }

    /** Whether a recipe belongs to a TheMealDB-style category chip such as "Beef", "Vegan" or "Dessert". */
    public static boolean inCategory(Recipe r, String category) {
        String c = category.toLowerCase(Locale.ROOT).trim();
        if (r.category.toLowerCase(Locale.ROOT).trim().equals(c)) return true;
        if (c.equals("vegetarian")) return r.tags.contains(Recipe.VEGETARIAN);
        if (c.equals("vegan")) return r.tags.contains(Recipe.VEGAN);
        if (c.equals("dessert")) return r.category.equalsIgnoreCase("Baking") || r.category.equalsIgnoreCase("Desserts");
        if (c.equals("starter") || c.equals("side")) return r.category.equalsIgnoreCase("Salad") || r.category.equalsIgnoreCase("Snack")
                || r.category.equalsIgnoreCase("Side") || r.category.equalsIgnoreCase("Starter");
        String fam = c.equals("seafood") ? "seafood" : c.equals("goat") ? "goat" : c;
        if (fam.equals("beef") || fam.equals("chicken") || fam.equals("pork") || fam.equals("lamb") || fam.equals("pasta")
                || fam.equals("seafood") || fam.equals("goat")) {
            for (Ingredient i : r.ingredients) {
                if (i.optional) continue;
                String f = Normalizer.family(i.key);
                if (i.key.equals(fam) || fam.equals(f) || i.key.startsWith(fam + " ") && !i.key.endsWith("stock")) return true;
                if (fam.equals("seafood") && (f != null && (f.equals("fish") || f.equals("seafood")) || i.key.equals("fish"))) return true;
            }
        }
        return false;
    }

    /** Local title search: every word of the query must appear in the title, cuisine or category. */
    public static List<Recipe> searchTitles(Collection<Recipe> all, String query, int limit) {
        List<Recipe> out = new ArrayList<Recipe>();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return out;
        String[] words = q.split("\\s+");
        for (Recipe r : all) {
            String hay = (r.title + " " + r.cuisine + " " + r.category).toLowerCase(Locale.ROOT);
            boolean ok = true;
            for (String w : words) if (!hay.contains(w)) { ok = false; break; }
            if (ok) out.add(r);
            if (out.size() >= limit) break;
        }
        return out;
    }
}
