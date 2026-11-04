package org.pantryrecipes.app.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Rough nutrition per serving (kcal, protein, carbohydrate, fat) from a small bundled per-100 g table built from
 * USDA FoodData Central SR Legacy (public domain), plus unit-to-gram conversions. It is an estimate: ingredient
 * names are matched to a generic food, and amounts such as "1 onion" use an average weight. Shown only when at
 * least 70% of a recipe's ingredients (by count) could be converted.
 */
public final class Nutrition {
    private Nutrition() { }

    public static final int MIN_COVERAGE = 70;
    /** More than this much oil is taken as frying oil, of which about a tenth ends up in the food. */
    public static final double FRY_GRAMS = 200, FRY_ABSORBED = 0.1;

    /** One food row (values per 100 g). */
    public static final class Food {
        public final String key;
        public final double kcal, protein, carbs, fat, gPerMl, gEach;
        public final Map<String, Double> units;
        public final String fdc;
        Food(String key, double kcal, double protein, double carbs, double fat, double gPerMl, double gEach, Map<String, Double> units, String fdc) {
            this.key = key; this.kcal = kcal; this.protein = protein; this.carbs = carbs; this.fat = fat;
            this.gPerMl = gPerMl; this.gEach = gEach; this.units = units; this.fdc = fdc;
        }
    }

    /** Parses foods.tsv (see tools/nutrition/build_foods.py). Bad lines are skipped. */
    public static Map<String, Food> parse(String tsv) {
        Map<String, Food> out = new LinkedHashMap<String, Food>();
        if (tsv == null) return out;
        for (String line : tsv.split("\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] f = line.split("\t", -1);
            if (f.length < 9) continue;
            try {
                Map<String, Double> u = new HashMap<String, Double>();
                if (!f[7].isEmpty()) for (String kv : f[7].split(";")) {
                    int e = kv.indexOf('=');
                    if (e > 0) u.put(kv.substring(0, e), Double.parseDouble(kv.substring(e + 1)));
                }
                out.put(f[0], new Food(f[0], Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                        Double.parseDouble(f[4]), Double.parseDouble(f[5]), Double.parseDouble(f[6]), u, f[8]));
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    /** Typical grams for loose kitchen units when the food row has no better figure. */
    private static final Map<String, Double> GENERIC = new HashMap<String, Double>();
    static {
        GENERIC.put("handful", 25.0); GENERIC.put("large handful", 40.0); GENERIC.put("cm", 5.0); GENERIC.put("can", 400.0);
        GENERIC.put("tin", 400.0); GENERIC.put("jar", 300.0); GENERIC.put("scoop", 66.0); GENERIC.put("pack", 85.0);
        GENERIC.put("packet", 85.0); GENERIC.put("ring", 45.0); GENERIC.put("bunch", 50.0); GENERIC.put("head", 500.0);
        GENERIC.put("sprig", 1.0); GENERIC.put("stalk", 40.0); GENERIC.put("sheet", 25.0); GENERIC.put("slice", 25.0);
        GENERIC.put("clove", 3.0); GENERIC.put("leaf", 1.0); GENERIC.put("stick", 113.0);
    }
    /** Volume-like loose units, in ml. */
    private static final Map<String, Double> LOOSE_ML = new HashMap<String, Double>();
    static { LOOSE_ML.put("pinch", 0.31); LOOSE_ML.put("dash", 0.62); LOOSE_ML.put("shot", 30.0); LOOSE_ML.put("splash", 5.0); LOOSE_ML.put("drizzle", 5.0); }

    /** Ingredients that add (next to) nothing; counted as covered even without an amount. */
    public static final Set<String> NEGLIGIBLE = new HashSet<String>(Arrays.asList(
            "salt", "black pepper", "white pepper", "water", "ice", "sparkling water", "cooking spray"));

    /** Food row for an ingredient key: exact, then its family. */
    public static Food lookup(Map<String, Food> foods, String key) {
        if (foods == null || key == null || key.isEmpty()) return null;
        Food f = foods.get(key);
        if (f == null) f = foods.get(Normalizer.family(key));
        return f;
    }

    /** Grams for an amount of a food, or NaN when it cannot be converted honestly. */
    public static double grams(Food f, double amount, String unit) {
        if (Double.isNaN(amount) || amount < 0) return Double.NaN;
        String c = Amounts.canon(unit);
        String dim = Amounts.dimension(c);
        if (dim.equals("mass")) return Amounts.convert(amount, c, "g");
        if (f == null) return Double.NaN;
        if (dim.equals("volume")) {
            double ml = Amounts.convert(amount, c, "ml");
            return f.gPerMl > 0 ? ml * f.gPerMl : Double.NaN;
        }
        if (dim.equals("count")) return f.gEach > 0 ? amount * f.gEach : Double.NaN;
        String u = c;
        Double g = f.units.get(u);
        if (g != null) return amount * g;
        Double ml = LOOSE_ML.get(u);
        if (ml != null) return amount * ml * (f.gPerMl > 0 ? f.gPerMl : 1.0);
        g = GENERIC.get(u);
        return g != null ? amount * g : Double.NaN;
    }

    public static final class Estimate {
        /** Per serving. */
        public double kcal, protein, carbs, fat;
        public int covered, total;
        public int percent() { return total == 0 ? 0 : (int) Math.round(100.0 * covered / total); }
        public boolean show() { return total > 0 && covered * 100 >= MIN_COVERAGE * total; }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("kcal", r1(kcal)); m.put("protein", r1(protein)); m.put("carbs", r1(carbs)); m.put("fat", r1(fat));
            m.put("covered", (long) covered); m.put("total", (long) total); m.put("percent", (long) percent()); m.put("show", show());
            return m;
        }
    }

    static double r1(double v) { return Math.round(v * 10) / 10.0; }

    /** Estimate per serving of the recipe as written (scaling servings does not change per-serving values). */
    public static Estimate estimate(Recipe r, Map<String, Food> foods) {
        Estimate e = new Estimate();
        double kc = 0, pr = 0, cb = 0, ft = 0;
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty()) continue;
            e.total++;
            if (NEGLIGIBLE.contains(i.key) && (Double.isNaN(i.amount) || lookup(foods, i.key) == null)) { e.covered++; continue; }
            Food f = lookup(foods, i.key);
            if (f == null) continue;
            double g = grams(f, i.amount, i.unit);
            if (Double.isNaN(g)) continue;
            // a large amount of oil is deep-frying oil: count only the part a fried food typically absorbs
            if (g > FRY_GRAMS && i.key.endsWith("oil")) g *= FRY_ABSORBED;
            e.covered++;
            kc += f.kcal * g / 100; pr += f.protein * g / 100; cb += f.carbs * g / 100; ft += f.fat * g / 100;
        }
        int s = Math.max(1, r.servings);
        e.kcal = kc / s; e.protein = pr / s; e.carbs = cb / s; e.fat = ft / s;
        return e;
    }

    /** "≈ 520 kcal · 24 g protein · 61 g carbs · 18 g fat per serving". */
    public static String summary(Estimate e) {
        return "\u2248 " + Math.round(e.kcal) + " kcal \u00b7 " + Math.round(e.protein) + " g protein \u00b7 "
                + Math.round(e.carbs) + " g carbs \u00b7 " + Math.round(e.fat) + " g fat per serving";
    }

    public static final String NOTE = "Estimate from USDA FoodData Central (SR Legacy, public domain) averages, matched by "
            + "ingredient name with typical weights for items like \u201c1 onion\u201d. Not for medical or dietary decisions.";
}
