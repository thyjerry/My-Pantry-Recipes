package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The weekly meal planner: plan entries and the combined shopping list for them. */
public final class MealPlan {
    private MealPlan() { }

    public static final int MAX_SERVINGS = 24;

    public static final class Entry {
        public final String date;
        public final String id;
        public final int servings;
        public Entry(String date, String id, int servings) {
            this.date = date;
            this.id = id;
            this.servings = Math.max(1, Math.min(MAX_SERVINGS, servings));
        }
    }

    public interface Lookup { Recipe byId(String id); }

    /** Entries on the seven days starting at {@code weekStart}, sorted by date (stable). */
    public static List<Entry> forWeek(List<Entry> all, String weekStart) {
        long a = Dates.toDays(weekStart);
        List<Entry> out = new ArrayList<Entry>();
        for (Entry e : all) {
            long d = Dates.toDays(e.date);
            if (d >= a && d < a + 7) out.add(e);
        }
        Collections.sort(out, new Comparator<Entry>() {
            public int compare(Entry x, Entry y) { return x.date.compareTo(y.date); }
        });
        return out;
    }

    /** Drops entries older than {@code keepDays} before today and anything with an invalid date. */
    public static List<Entry> prune(List<Entry> all, String today, int keepDays) {
        long t = Dates.toDays(today);
        List<Entry> out = new ArrayList<Entry>();
        for (Entry e : all) {
            long d = Dates.toDays(e.date);
            if (d != Long.MIN_VALUE && d >= t - keepDays) out.add(e);
        }
        return out;
    }

    /** One line of the combined list. */
    public static final class Item {
        public final String key;
        public String name;
        public double amount = Double.NaN;  // NaN: no amount (e.g. "to taste")
        public String unit = "";
        public final List<String> recipes = new ArrayList<String>();
        /** True when the pantry has some, but not enough (the amount is the shortfall). */
        public boolean partial;
        String dim;
        Item(String key) { this.key = key; }
        public String text() { return Double.isNaN(amount) ? "" : Quantity.format(amount, unit, 1); }
    }

    private static String nameOf(Ingredient i) {
        String n = i.name;
        int c = n.indexOf(',');
        if (c > 0) n = n.substring(0, c);
        n = n.trim();
        return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /**
     * Combined shopping list for the entries: required ingredients summed per key and unit dimension, scaled to
     * each entry's servings. Staples and items in the pantry are left out; when the pantry quantity is known and
     * short only the shortfall is listed.
     */
    public static List<Item> shopping(List<Entry> entries, Lookup lookup, Matcher pantry, Map<String, String> qtyByKey) {
        LinkedHashMap<String, Item> items = new LinkedHashMap<String, Item>();
        for (Entry e : entries) {
            Recipe r = lookup.byId(e.id);
            if (r == null) continue;
            double scale = (double) e.servings / Math.max(1, r.servings);
            for (Ingredient i : r.ingredients) {
                if (i.optional || i.key.isEmpty()) continue;
                if (pantry != null && pantry.isStaple(i.key)) continue;
                String dim = Double.isNaN(i.amount) ? "none" : Amounts.dimension(i.unit);
                String id = i.key + "|" + dim;
                Item it = items.get(id);
                if (it == null) {
                    it = new Item(i.key);
                    it.name = nameOf(i);
                    it.dim = dim;
                    if (!Double.isNaN(i.amount)) { it.amount = i.amount * scale; it.unit = i.unit; }
                    items.put(id, it);
                } else if (!Double.isNaN(i.amount)) {
                    it.amount += Amounts.convert(i.amount * scale, i.unit, it.unit);
                }
                if (!it.recipes.contains(r.title)) it.recipes.add(r.title);
            }
        }
        List<Item> out = new ArrayList<Item>();
        for (Item it : items.values()) {
            if (pantry != null && pantry.has(it.key)) {
                String q = Amounts.qtyFor(it.key, qtyByKey);
                if (q == null || Double.isNaN(it.amount)) continue;     // in the pantry, amount unknown: assume enough
                Quantity.Parsed p = Amounts.parse(q);
                String hu = p.unit;
                String cu = Amounts.canon(hu);
                if (!cu.isEmpty() && Amounts.dimension(cu).startsWith("unit:") && (" " + it.key + " ").contains(" " + cu + " ")) hu = "";
                double have = Amounts.convert(p.amount, hu, it.unit);
                if (Double.isNaN(have) || have + 1e-9 >= it.amount) continue;
                it.amount = it.amount - have;
                it.partial = true;
            }
            out.add(it);
        }
        return out;
    }
}
