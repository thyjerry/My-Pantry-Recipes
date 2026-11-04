package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional pantry quantities ("500 g", "6", "2 cups") compared with what a recipe needs. Units convert
 * within mass (g, kg, oz, lb) and volume (ml, l, tsp, tbsp, cup, fl oz, pint); counts compare with counts;
 * anything else only compares with the very same unit. Unknown whenever it cannot be compared honestly.
 */
public final class Amounts {
    private Amounts() { }

    public static final String ENOUGH = "enough", SHORT = "short", UNKNOWN = "unknown";

    private static final Map<String, String> ALIAS = new HashMap<String, String>();
    private static final Map<String, Double> FACTOR = new HashMap<String, Double>();
    private static final Map<String, String> DIM = new HashMap<String, String>();

    private static void unit(String canon, String dim, double factor, String... aliases) {
        ALIAS.put(canon, canon);
        for (String a : aliases) ALIAS.put(a, canon);
        DIM.put(canon, dim);
        FACTOR.put(canon, factor);
    }

    static {
        unit("g", "mass", 1, "gram", "grams", "gr", "grm");
        unit("kg", "mass", 1000, "kilo", "kilos", "kilogram", "kilograms", "kgs");
        unit("oz", "mass", 28.3495, "ounce", "ounces");
        unit("lb", "mass", 453.592, "lbs", "pound", "pounds");
        unit("ml", "volume", 1, "millilitre", "milliliter", "millilitres", "milliliters", "mls");
        unit("l", "volume", 1000, "litre", "liter", "litres", "liters", "ltr");
        unit("tsp", "volume", 5, "teaspoon", "teaspoons", "tsps");
        unit("tbsp", "volume", 15, "tablespoon", "tablespoons", "tbsps", "tbs", "tbl");
        unit("cup", "volume", 240, "cups", "c");
        unit("fl oz", "volume", 29.5735, "floz", "fluid ounce", "fluid ounces");
        unit("pint", "volume", 568, "pints", "pt");
        unit("", "count", 1, "x", "pc", "pcs", "piece", "pieces", "each", "whole", "large", "medium", "small", "item", "items");
    }

    /** Canonical unit: "g", "ml", "" for plain counts, or the singular lower-case word ("clove", "can"). */
    public static String canon(String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase().replace(".", "");
        String c = ALIAS.get(u);
        if (c != null) return c;
        if (u.endsWith("ches") || u.endsWith("shes")) return u.substring(0, u.length() - 2);
        if (u.endsWith("s") && u.length() > 3 && !u.endsWith("ss")) return u.substring(0, u.length() - 1);
        return u;
    }

    public static String dimension(String unit) {
        String c = canon(unit);
        String d = DIM.get(c);
        return d != null ? d : "unit:" + c;
    }

    /** Converts {@code amount} of {@code from} into {@code to}; NaN when the units are not comparable. */
    public static double convert(double amount, String from, String to) {
        if (Double.isNaN(amount)) return Double.NaN;
        String a = canon(from), b = canon(to);
        if (a.equals(b)) return amount;
        if (!dimension(a).equals(dimension(b)) || dimension(a).startsWith("unit:")) return Double.NaN;
        return amount * FACTOR.get(a) / FACTOR.get(b);
    }

    /** enough / short / unknown for one need against what is on hand (both free amounts with units). */
    public static String status(double have, String haveUnit, double need, String needUnit) {
        if (Double.isNaN(have) || Double.isNaN(need) || need <= 0) return UNKNOWN;
        double h = convert(have, haveUnit, needUnit);
        if (Double.isNaN(h)) return UNKNOWN;
        return h + 1e-9 >= need ? ENOUGH : SHORT;
    }

    /** Parses a typed pantry quantity ("500 g", "6", "1.5 kg"). Amount is NaN if there is no leading number. */
    public static Quantity.Parsed parse(String text) { return Quantity.parse(text == null ? "" : text); }

    /** The pantry quantity text that applies to an ingredient key: exact key, then family relations. */
    public static String qtyFor(String key, Map<String, String> qtyByKey) {
        if (qtyByKey == null || key == null || key.isEmpty()) return null;
        String q = qtyByKey.get(key);
        if (q != null && !q.trim().isEmpty()) return q;
        String f = Normalizer.family(key);
        q = qtyByKey.get(f);
        if (q != null && !q.trim().isEmpty()) return q;
        return null;
    }

    /** One ingredient row's check. */
    public static final class Check {
        public final Ingredient ingredient;
        public final String status;
        public final String haveText;
        Check(Ingredient i, String s, String h) { ingredient = i; status = s; haveText = h; }
    }

    /**
     * Checks every required ingredient that has a numeric amount and a pantry quantity. Ingredients sharing
     * a key are summed first so "2 eggs + 1 egg" needs 3 eggs.
     */
    public static List<Check> check(Recipe r, double scale, Map<String, String> qtyByKey) {
        List<Check> out = new ArrayList<Check>();
        if (qtyByKey == null || qtyByKey.isEmpty()) return out;
        Map<String, Double> needSum = new HashMap<String, Double>();
        Map<String, String> needUnit = new HashMap<String, String>();
        Map<String, Boolean> mixed = new HashMap<String, Boolean>();
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty() || Double.isNaN(i.amount)) continue;
            String u = needUnit.get(i.key);
            double v = i.amount * scale;
            if (u == null) { needUnit.put(i.key, i.unit); needSum.put(i.key, v); continue; }
            double c = convert(v, i.unit, u);
            if (Double.isNaN(c)) mixed.put(i.key, true);
            else needSum.put(i.key, needSum.get(i.key) + c);
        }
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty()) continue;
            String q = qtyFor(i.key, qtyByKey);
            if (q == null) continue;
            String st = UNKNOWN;
            if (!Double.isNaN(i.amount) && !Boolean.TRUE.equals(mixed.get(i.key))) {
                Quantity.Parsed p = parse(q);
                String hu = p.unit;
                // "6 eggs" for the key "egg": the unit is the item itself, so it is a plain count
                String cu = canon(hu);
                if (!cu.isEmpty() && dimension(cu).startsWith("unit:") && (" " + i.key + " ").contains(" " + cu + " ")) hu = "";
                st = status(p.amount, hu, needSum.get(i.key), needUnit.get(i.key));
            }
            out.add(new Check(i, st, q.trim()));
        }
        return out;
    }

    /**
     * Pantry quantity text after using {@code need needUnit} of {@code key}, e.g. "500 g" minus 200 g = "300 g".
     * Null when either side has no number or the units are not comparable (the pantry is then left alone).
     */
    public static String deduct(String haveText, String key, double need, String needUnit) {
        if (haveText == null || Double.isNaN(need) || need <= 0) return null;
        Quantity.Parsed p = parse(haveText);
        if (Double.isNaN(p.amount)) return null;
        String hu = p.unit;
        String cu = canon(hu);
        if (!cu.isEmpty() && dimension(cu).startsWith("unit:") && key != null && (" " + key + " ").contains(" " + cu + " ")) hu = "";
        double used = convert(need, needUnit, hu);
        if (Double.isNaN(used)) return null;
        double left = Math.max(0, p.amount - used);
        left = Math.round(left * 100) / 100.0;
        String unitText = hu.isEmpty() ? "" : p.unit.trim();
        if (left <= 1 && unitText.length() > 2 && unitText.endsWith("s") && cu.equals(canon(unitText.substring(0, unitText.length() - 1)))) unitText = unitText.substring(0, unitText.length() - 1);
        String num = left == Math.rint(left) ? String.valueOf((long) left) : String.valueOf(left);
        return unitText.isEmpty() ? num : num + " " + unitText;
    }

    /** Short indicator text for an ingredient row. */
    public static String label(Check c) {
        if (ENOUGH.equals(c.status)) return "You have enough (" + c.haveText + ")";
        if (SHORT.equals(c.status)) return "Not enough: you have " + c.haveText;
        return "You have " + c.haveText;
    }
}
