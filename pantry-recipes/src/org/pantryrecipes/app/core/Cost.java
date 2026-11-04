package org.pantryrecipes.app.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estimated cost per serving from ingredient prices the user sets ("$2.50 for 500 g"), optionally falling back
 * to a small bundled table of rough default prices. Amounts convert within mass, volume and counts, and across
 * them through the nutrition table's weights (g per ml, g per item). Shown only when enough ingredients are
 * priced; always labelled as an estimate.
 */
public final class Cost {
    private Cost() { }

    public static final int MIN_COVERAGE = 70;
    public static final String DEFAULT_CURRENCY = "$";
    /** Units a price can be given in ("" = per item). */
    public static final String[] UNITS = {"", "g", "kg", "oz", "lb", "ml", "l"};
    public static final String[] UNIT_NAMES = {"each", "g", "kg", "oz", "lb", "ml", "l"};
    public static final int MAX_PRICES = 1000;

    /** {@code price} buys {@code amount} {@code unit}. */
    public static final class Price {
        public final double price, amount;
        public final String unit;
        public Price(double price, double amount, String unit) {
            this.price = price; this.amount = amount; this.unit = unit == null ? "" : unit;
        }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("price", price);
            m.put("amount", amount);
            m.put("unit", unit);
            return m;
        }
    }

    public static boolean validUnit(String u) {
        for (String x : UNITS) if (x.equals(u)) return true;
        return false;
    }

    /** A price with sane bounds, or null. */
    public static Price make(double price, double amount, String unit) {
        if (Double.isNaN(price) || Double.isNaN(amount) || price < 0 || price > 100000 || amount <= 0 || amount > 100000 || !validUnit(unit)) return null;
        return new Price(price, amount, unit);
    }

    /** Parses prices.tsv: key, price, amount, unit. */
    public static Map<String, Price> parseDefaults(String tsv) {
        Map<String, Price> out = new LinkedHashMap<String, Price>();
        if (tsv == null) return out;
        for (String line : tsv.split("\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] f = line.split("\t", -1);
            if (f.length < 4) continue;
            try {
                Price p = make(Double.parseDouble(f[1]), Double.parseDouble(f[2]), f[3].trim());
                if (p != null) out.put(f[0], p);
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    /** User price first, then (when allowed) the default, by key then family. {price, fromDefault?} */
    public static Price find(String key, Map<String, Price> user, Map<String, Price> defaults) {
        if (key == null || key.isEmpty()) return null;
        String fam = Normalizer.family(key);
        if (user != null) {
            Price p = user.get(key);
            if (p == null) p = user.get(fam);
            if (p != null) return p;
        }
        if (defaults != null) {
            Price p = defaults.get(key);
            if (p == null) p = defaults.get(fam);
            return p;
        }
        return null;
    }

    /** Cost of {@code amount unit} of an ingredient at price p, or NaN when the units cannot be related. */
    public static double costOf(Price p, Nutrition.Food food, double amount, String unit) {
        if (p == null || Double.isNaN(amount)) return Double.NaN;
        double q = Amounts.convert(amount, unit, p.unit);
        if (!Double.isNaN(q)) return q / p.amount * p.price;
        double g = Nutrition.grams(food, amount, unit), gp = Nutrition.grams(food, p.amount, p.unit);
        if (Double.isNaN(g) || Double.isNaN(gp) || gp <= 0) return Double.NaN;
        return g / gp * p.price;
    }

    public static final class Estimate {
        public double total, perServing;
        public int covered, count, fromDefaults;
        public int percent() { return count == 0 ? 0 : (int) Math.round(100.0 * covered / count); }
        public boolean show() { return count > 0 && covered * 100 >= MIN_COVERAGE * count; }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("total", r2(total));
            m.put("perServing", r2(perServing));
            m.put("covered", (long) covered); m.put("count", (long) count); m.put("fromDefaults", (long) fromDefaults);
            m.put("percent", (long) percent()); m.put("show", show());
            return m;
        }
    }

    static double r2(double v) { return Math.round(v * 100) / 100.0; }

    private static boolean isDefault(Price p, Map<String, Price> user) {
        return user == null || !user.containsValue(p);
    }

    /** Cost of the recipe made for {@code servings} servings. */
    public static Estimate estimate(Recipe r, int servings, Map<String, Price> user, Map<String, Price> defaults, Map<String, Nutrition.Food> foods) {
        Estimate e = new Estimate();
        double scale = (double) Math.max(1, servings) / Math.max(1, r.servings);
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty()) continue;
            e.count++;
            Price p = find(i.key, user, defaults);
            if (Double.isNaN(i.amount)) {
                if (Nutrition.NEGLIGIBLE.contains(i.key)) e.covered++;
                continue;
            }
            if (p == null) continue;
            double c = costOf(p, Nutrition.lookup(foods, i.key), i.amount * scale, i.unit);
            if (Double.isNaN(c)) continue;
            e.covered++;
            if (isDefault(p, user)) e.fromDefaults++;
            e.total += c;
        }
        e.perServing = e.total / Math.max(1, servings);
        return e;
    }

    /** One shopping row's estimated cost, or NaN. */
    public static double item(String name, double amount, String unit, Map<String, Price> user, Map<String, Price> defaults, Map<String, Nutrition.Food> foods) {
        String key = Normalizer.normalize(name == null ? "" : name);
        Price p = find(key, user, defaults);
        return costOf(p, Nutrition.lookup(foods, key), amount, unit);
    }

    /** Sum over shopping rows: {total, priced rows, rows}. Rows are {name, amount, unit}. */
    public static double[] shopping(List<Object[]> rows, Map<String, Price> user, Map<String, Price> defaults, Map<String, Nutrition.Food> foods) {
        double t = 0;
        int n = 0;
        for (Object[] r : rows) {
            double c = item((String) r[0], (Double) r[1], (String) r[2], user, defaults, foods);
            if (!Double.isNaN(c)) { t += c; n++; }
        }
        return new double[]{t, n, rows.size()};
    }

    /** "$1.25" style money text; symbols up to 4 characters, two decimals. */
    public static String money(String currency, double v) {
        String c = currency == null || currency.trim().isEmpty() ? DEFAULT_CURRENCY : currency.trim();
        long cents = Math.round(v * 100);
        String s = (cents / 100) + "." + (cents % 100 < 10 ? "0" : "") + (cents % 100);
        return c + s;
    }

    /** "$2.50 for 500 g" / "$0.40 each". */
    public static String describe(String currency, Price p) {
        if (p.unit.isEmpty()) return p.amount == 1 ? money(currency, p.price) + " each" : money(currency, p.price) + " for " + Quantity.number(p.amount, "");
        return money(currency, p.price) + " for " + Quantity.number(p.amount, p.unit) + " " + p.unit;
    }

    public static final String NOTE = "Estimate from the prices you set (and, if switched on, rough built-in default prices). "
            + "Real prices vary by shop, brand and season.";
}
