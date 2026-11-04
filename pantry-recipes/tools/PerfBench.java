import org.pantryrecipes.app.core.*;
import java.nio.file.*;
import java.util.*;

/**
 * JVM benchmark of the work the Pantry flow does on the UI thread (no Android views).
 * Usage: java -cp classes PerfBench <assetsDir> [legacy|new] [reps]
 * "legacy" replays the v1.7 algorithms; "new" uses the v1.8 code paths (PantryIndex / MatchIndex).
 */
public class PerfBench {
    static double ms(long t0) { return (System.nanoTime() - t0) / 1e6; }
    static String fold(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.US);
    }
    static final String[] PANTRY = {"Eggs", "Spinach", "Rice", "Onion", "Garlic", "Tomatoes", "Chicken breast", "Cheddar cheese",
            "Milk", "Butter", "Flour", "Potatoes", "Carrots", "Lemon", "Pasta", "Canned chickpeas", "Bell pepper", "Yogurt"};
    static final String[] STAPLES = {"Salt", "Black pepper", "Olive oil", "Vegetable oil", "Water"};

    public static void main(String[] a) throws Exception {
        String dir = a[0];
        String mode = a.length > 1 ? a[1] : "legacy";
        int reps = a.length > 2 ? Integer.parseInt(a[2]) : 5;
        Map<String, List<Double>> t = new LinkedHashMap<String, List<Double>>();
        for (int rep = 0; rep < reps; rep++) {
            if ("new".equals(mode)) runNew(dir, t); else runLegacy(dir, t);
        }
        System.out.println("mode=" + mode + " reps=" + reps + " (first = cold, median of the rest = warm)");
        for (Map.Entry<String, List<Double>> e : t.entrySet()) {
            List<Double> v = e.getValue();
            List<Double> rest = new ArrayList<Double>(v.subList(1, v.size()));
            Collections.sort(rest);
            double med = rest.isEmpty() ? v.get(0) : rest.get(rest.size() / 2);
            System.out.printf(Locale.US, "  %-44s cold %9.1f ms   warm %9.1f ms%n", e.getKey(), v.get(0), med);
        }
    }

    static void put(Map<String, List<Double>> t, String k, double v) {
        List<Double> l = t.get(k);
        if (l == null) t.put(k, l = new ArrayList<Double>());
        l.add(v);
    }

    static List<Recipe> recipes;
    static Substitutions subs;

    static void load(String dir, Map<String, List<Double>> t) throws Exception {
        long t0 = System.nanoTime();
        String json = new String(Files.readAllBytes(Paths.get(dir, "recipes.json")), "UTF-8");
        recipes = Recipe.parseList(json);
        subs = Substitutions.parse(new String(Files.readAllBytes(Paths.get(dir, "substitutions.txt")), "UTF-8"));
        put(t, "1 startup: parse recipes.json + swaps (worker)", ms(t0));
    }

    // ---------------------------------------------------------------- v1.7 algorithms
    static void runLegacy(String dir, Map<String, List<Double>> t) throws Exception {
        load(dir, t);
        // Pantry open: ingredientNames() for autocomplete (UI thread in v1.7)
        long t0 = System.nanoTime();
        Map<String, String> byKey = new LinkedHashMap<String, String>();
        for (String s : Catalog.allItems()) byKey.put(Normalizer.normalize(s), s);
        List<String> extra = new ArrayList<String>(Arrays.asList(Catalog.ALIASES));
        for (Recipe r : recipes) for (Ingredient i : r.ingredients) {
            if (i.key.isEmpty() || byKey.containsKey(i.key)) continue;
            String n = i.name; int c = n.indexOf(','); if (c > 0) n = n.substring(0, c);
            byKey.put(i.key, n.trim());
        }
        List<String> out = new ArrayList<String>(byKey.values());
        for (String s : extra) if (!out.contains(s)) out.add(s);
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        double names = ms(t0);
        // Pantry open: quick chips of the open section(s): one normalize per chip; section counts
        t0 = System.nanoTime();
        Set<String> pantry = new HashSet<String>();
        for (String p : PANTRY) pantry.add(Normalizer.normalize(p));
        int chips = 0;
        for (String item : Catalog.ITEMS[0]) if (!Catalog.isHeader(item)) { Normalizer.normalize(item); chips++; }
        int have = 0;
        for (int c = 0; c < Catalog.ITEMS.length; c++)
            for (String item : Catalog.ITEMS[c]) if (!Catalog.isHeader(item) && pantry.contains(Normalizer.normalize(item))) have++;
        double open = ms(t0);
        put(t, "2 Pantry open: autocomplete names (UI)", names);
        put(t, "3 Pantry open: chip keys + counts (UI)", open);
        // Filter typing "c","ch","che","chee","chees","cheese": full scan per keystroke
        t0 = System.nanoTime();
        for (String q0 : new String[]{"c", "ch", "che", "chee", "chees", "cheese"}) {
            String q = fold(q0), qk = Normalizer.normalize(q0);
            Set<String> keys = new HashSet<String>(); List<String> hits = new ArrayList<String>();
            for (int c = 0; c < Catalog.CATEGORIES.length; c++) {
                boolean catHit = fold(Catalog.CATEGORIES[c]).contains(q); String sub = "";
                for (String item : Catalog.ITEMS[c]) {
                    if (Catalog.isHeader(item)) { sub = fold(item); continue; }
                    String k = Normalizer.normalize(item);
                    if ((catHit || sub.contains(q) || fold(item).contains(q) || k.equals(qk)) && keys.add(k)) hits.add(item);
                }
            }
            for (String al : Catalog.ALIASES) { String k = Normalizer.normalize(al); if ((fold(al).contains(q) || k.equals(qk)) && keys.add(k)) hits.add(al); }
            int shown = Math.min(80, hits.size());
            for (int i = 0; i < shown; i++) Normalizer.normalize(hits.get(i)); // quickChip() normalizes again
        }
        put(t, "4 filter: 6 keystrokes total (UI)", ms(t0));
        // Toggle one ingredient: per-chip restyle is view work (counted separately), plus section counts
        t0 = System.nanoTime();
        pantry.add(Normalizer.normalize("Feta"));
        for (int c = 0; c < Catalog.ITEMS.length; c++) for (String k : keysOf(c)) if (pantry.contains(k)) have++;
        put(t, "5 toggle: model + counts (UI)", ms(t0));
        // Re-match on the Recipes tab (v1.7: full match of all recipes on the UI thread)
        t0 = System.nanoTime();
        List<String> pk = new ArrayList<String>(), sk = new ArrayList<String>();
        for (String p : PANTRY) pk.add(Normalizer.normalize(p));
        pk.add(Normalizer.normalize("Feta"));
        for (String s : STAPLES) sk.add(Normalizer.normalize(s));
        Matcher m = new Matcher(pk, sk).withSubstitutions(subs, new HashSet<String>());
        List<Matcher.Result> res = new ArrayList<Matcher.Result>();
        for (Recipe r : recipes) res.add(m.match(r));
        UseBy.sort(res, new HashSet<String>(Arrays.asList(Normalizer.normalize("Spinach"))));
        put(t, "6 re-match all " + recipes.size() + " + sort (UI)", ms(t0));
        // Recipe search typing "chick" in Recipes: passesFilters normalizes the query once per recipe
        t0 = System.nanoTime();
        for (String q0 : new String[]{"c", "ch", "chi", "chic", "chick"}) {
            String q = q0.toLowerCase(Locale.US); int n = 0;
            for (Matcher.Result r0 : res) {
                Recipe r = r0.recipe;
                boolean hit = r.title.toLowerCase(Locale.US).contains(q) || r.cuisine.toLowerCase(Locale.US).contains(q) || r.category.toLowerCase(Locale.US).contains(q);
                if (!hit) { String qk = Normalizer.normalize(q0);
                    for (Ingredient i : r.ingredients) if (i.name.toLowerCase(Locale.US).contains(q) || (!qk.isEmpty() && i.key.equals(qk))) { hit = true; break; } }
                if (hit) n++;
            }
        }
        put(t, "7 recipe search: 5 keystrokes total (UI)", ms(t0));
    }

    static List<String> keysOf(int c) {
        List<String> l = new ArrayList<String>();
        for (String item : Catalog.ITEMS[c]) if (!Catalog.isHeader(item)) l.add(Normalizer.normalize(item));
        return l;
    }

    // ---------------------------------------------------------------- v1.8 code paths
    static void runNew(String dir, Map<String, List<Double>> t) throws Exception {
        try {
            Class.forName("PerfBenchNew").getMethod("run", String.class, Map.class).invoke(null, dir, t);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PerfBenchNew not compiled");
        }
    }
}
