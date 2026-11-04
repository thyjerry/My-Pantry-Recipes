package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v2.0 pantry inventory by location: pantry, fridge, freezer and spice rack, freezer "use or toss" dates, staples
 * the restock list and the spice-rack "buy next" ranking. Pure logic shared with the web app (core20.js).
 * Freezer times are for best quality (food kept frozen at 0 °F / -18 °C stays safe indefinitely); they are rounded
 * from the public USDA FoodKeeper guidance and are approximate.
 */
public final class Inventory {
    private Inventory() { }

    public static final String PANTRY = "pantry", FRIDGE = "fridge", FREEZER = "freezer", SPICE = "spice";
    public static final List<String> LOCATIONS = Collections.unmodifiableList(Arrays.asList(PANTRY, FRIDGE, FREEZER, SPICE));
    public static final List<String> LOCATION_NAMES = Collections.unmodifiableList(Arrays.asList("Pantry", "Fridge", "Freezer", "Spice rack"));

    public static String locationName(String id) {
        int k = LOCATIONS.indexOf(id);
        return k < 0 ? "Pantry" : LOCATION_NAMES.get(k);
    }

    public static String validLocation(String id) { return LOCATIONS.contains(id) ? id : ""; }

    private static int cat(String name) { return Arrays.asList(Catalog.CATEGORIES).indexOf(name); }
    private static final int VEG = cat("Vegetables"), FRUIT = cat("Fruits"), HERBS = cat("Herbs"), SPICES = cat("Spices & seasonings"),
            MEAT = cat("Meat"), POULTRY = cat("Poultry"), FISH = cat("Fish & seafood"), DELI = cat("Deli & cured"),
            DAIRY = cat("Eggs & dairy"), CHEESE = cat("Cheese"), PLANT = cat("Plant-based"), FROZEN = cat("Frozen"),
            BREAD = cat("Bread & tortillas");

    /** Fresh produce that keeps best outside the fridge. */
    private static final java.util.Set<String> COUNTER = new java.util.HashSet<String>(Arrays.asList(
            "potato", "sweet potato", "onion", "red onion", "shallot", "garlic", "banana", "tomato", "avocado", "winter squash",
            "butternut squash", "acorn squash", "spaghetti squash", "pumpkin", "delicata squash", "ginger", "pearl onion", "mango", "pineapple"));

    /** Where an item usually lives, by its label ("Dried ...", "Frozen ...") and pantry-grid category. */
    public static String defaultLocation(String key, String label) {
        String l = label == null ? "" : label.toLowerCase(java.util.Locale.ROOT).trim();
        if (l.startsWith("frozen ")) return FREEZER;
        if (l.startsWith("dried ") || l.startsWith("ground ") && Diets.category(key) == SPICES) return SPICE;
        return defaultLocation(key);
    }

    /** Where an item usually lives (by its pantry-grid category). */
    public static String defaultLocation(String key) {
        if (key == null) return PANTRY;
        if (key.startsWith("frozen ")) return FREEZER;
        int c = Diets.category(key);
        if (c == SPICES) return SPICE;
        if (c == FROZEN) return FREEZER;
        if (c == MEAT || c == POULTRY || c == FISH || c == DELI || c == DAIRY || c == CHEESE || c == PLANT) return FRIDGE;
        if (c == HERBS) return key.startsWith("dried ") ? SPICE : FRIDGE;
        if (c == VEG || c == FRUIT) return COUNTER.contains(key) ? PANTRY : FRIDGE;
        return PANTRY;
    }

    /** Best-quality freezer time in months (USDA FoodKeeper, rounded). */
    public static int freezerMonths(String key) {
        if (key == null) return 3;
        String k = key;
        if (k.contains("ground") || k.contains("mince") || k.contains("sausage") || k.contains("bacon") || k.contains("hot dog")) return k.contains("bacon") ? 1 : 3;
        if (k.contains("leftover") || k.contains("cooked") || k.contains("soup") || k.contains("stew") || k.contains("stock") || k.contains("broth")) return 3;
        int c = Diets.category(k);
        if (c == POULTRY) return k.contains("whole") ? 12 : 9;
        if (c == MEAT) return k.contains("steak") || k.contains("roast") || k.contains("chop") ? 6 : 4;
        if (c == FISH) {
            boolean fatty = k.contains("salmon") || k.contains("mackerel") || k.contains("sardine") || k.contains("trout") || k.contains("tuna") || k.contains("herring");
            if (k.contains("shrimp") || k.contains("prawn")) return 6;
            return fatty ? 3 : 6;
        }
        if (c == DELI) return 2;
        if (c == CHEESE) return k.contains("parmesan") || k.contains("cheddar") || k.contains("hard") ? 6 : 4;
        if (c == DAIRY) return k.contains("butter") ? 9 : k.contains("milk") ? 3 : k.contains("egg") ? 12 : 3;
        if (c == BREAD) return 3;
        if (c == VEG || c == FRUIT || c == FROZEN) return 10;
        if (c == HERBS) return 6;
        return 3;
    }

    public static final String OK = "ok", USE_SOON = "use soon", PAST = "past best";

    /** The "use or toss" date (yyyy-mm-dd) for something frozen on {@code frozen}, or "" for an invalid date. */
    public static String useOrTossDate(String frozen, String key) {
        if (!Dates.valid(frozen)) return "";
        return Dates.addMonths(frozen, freezerMonths(key));
    }

    /** ok / use soon (within 30 days of the date) / past best (after it). */
    public static String freezerStatus(String frozen, String key, String today) {
        String d = useOrTossDate(frozen, key);
        if (d.isEmpty() || !Dates.valid(today)) return OK;
        long left = Dates.between(today, d);
        if (left < 0) return PAST;
        if (left <= 30) return USE_SOON;
        return OK;
    }

    public static final String FREEZER_NOTE = "Freezer times are for best quality. Food kept frozen at 0 \u00b0F (-18 \u00b0C) stays safe, "
            + "but taste and texture fade. Times are rounded from USDA FoodKeeper guidance.";

    /** One pantry item as the inventory logic sees it. */
    public static final class Item {
        public String key = "", label = "", loc = "", frozen = "", useBy = "", qty = "";
        public boolean staple, low;
        public Item() { }
        public Item(String key, String label) { this.key = key; this.label = label; }
        public String location() { return loc.isEmpty() ? defaultLocation(key, label) : loc; }
    }

    /** Items in one location, sorted by label (case-insensitive). */
    public static List<Item> at(List<Item> items, String loc) {
        List<Item> out = new ArrayList<Item>();
        for (Item i : items) if (i.location().equals(loc)) out.add(i);
        Collections.sort(out, new java.util.Comparator<Item>() {
            public int compare(Item a, Item b) { return a.label.compareToIgnoreCase(b.label); }
        });
        return out;
    }

    /** Count per location in LOCATIONS order. */
    public static int[] counts(List<Item> items) {
        int[] n = new int[LOCATIONS.size()];
        for (Item i : items) n[LOCATIONS.indexOf(i.location())]++;
        return n;
    }

    /** The restock list: items marked "running low", staples first, then by label. */
    public static List<Item> restock(List<Item> items) {
        List<Item> out = new ArrayList<Item>();
        for (Item i : items) if (i.low) out.add(i);
        Collections.sort(out, new java.util.Comparator<Item>() {
            public int compare(Item a, Item b) {
                if (a.staple != b.staple) return a.staple ? -1 : 1;
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    /** Frozen items whose best-quality date is near or past, soonest first. */
    public static List<Item> freezerAlerts(List<Item> items, String today) {
        List<Item> out = new ArrayList<Item>();
        for (Item i : items) {
            if (!FREEZER.equals(i.location()) || !Dates.valid(i.frozen)) continue;
            if (!OK.equals(freezerStatus(i.frozen, i.key, today))) out.add(i);
        }
        Collections.sort(out, new java.util.Comparator<Item>() {
            public int compare(Item a, Item b) { return useOrTossDate(a.frozen, a.key).compareTo(useOrTossDate(b.frozen, b.key)); }
        });
        return out;
    }

    /** Items with a use-by date exactly {@code days} days from today (1 = tomorrow, for the evening-before reminder). */
    public static List<Item> expiringIn(List<Item> items, String today, int days) {
        List<Item> out = new ArrayList<Item>();
        for (Item i : items) if (Dates.valid(i.useBy) && Dates.between(today, i.useBy) == days) out.add(i);
        return out;
    }

    /**
     * Spice rack "buy next": spices and dried herbs that you don't have, ranked by how many recipes use them (ties by
     * name). recipeKeyCounts maps ingredient key -> number of recipes using it.
     */
    public static List<String[]> spicesToBuy(java.util.Set<String> have, Map<String, Integer> recipeKeyCounts, int n) {
        List<String[]> rows = new ArrayList<String[]>();
        String[] items = Catalog.ITEMS[SPICES];
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (String it : items) {
            if (Catalog.isHeader(it)) continue;
            String k = Normalizer.normalize(it);
            if (k.isEmpty() || have.contains(k) || !seen.add(k)) continue;
            Integer c = recipeKeyCounts.get(k);
            if (c == null || c == 0) continue;
            rows.add(new String[]{it, k, String.valueOf(c)});
        }
        Collections.sort(rows, new java.util.Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                int d = Integer.parseInt(b[2]) - Integer.parseInt(a[2]);
                return d != 0 ? d : a[0].compareToIgnoreCase(b[0]);
            }
        });
        return rows.size() > n ? new ArrayList<String[]>(rows.subList(0, n)) : rows;
    }

    /** Recipe key counts for spicesToBuy (each recipe counted once per key). */
    public static Map<String, Integer> keyCounts(List<Recipe> recipes) {
        Map<String, Integer> m = new java.util.HashMap<String, Integer>();
        for (Recipe r : recipes) {
            java.util.Set<String> ks = new java.util.HashSet<String>();
            for (Ingredient i : r.ingredients) if (!i.key.isEmpty()) ks.add(i.key);
            for (String k : ks) { Integer c = m.get(k); m.put(k, c == null ? 1 : c + 1); }
        }
        return m;
    }

    /**
     * Pick for the "cook this tonight" reminder: the recipe using the most expiring items, then the best pantry match,
     * then the title. Returns null when no recipe uses any of them.
     */
    public static Recipe cookTonight(List<Recipe> recipes, java.util.Set<String> expiring, java.util.Set<String> pantry) {
        Recipe best = null; int bestUse = 0; double bestHave = -1;
        for (Recipe r : recipes) {
            int use = 0, have = 0, total = 0;
            for (Ingredient i : r.ingredients) {
                if (i.optional || i.key.isEmpty()) continue;
                total++;
                if (expiring.contains(i.key)) use++;
                if (pantry.contains(i.key)) have++;
            }
            if (use == 0 || total == 0) continue;
            double h = (double) have / total;
            if (use > bestUse || use == bestUse && (h > bestHave + 1e-9 || Math.abs(h - bestHave) < 1e-9 && best != null && r.title.compareTo(best.title) < 0)) {
                best = r; bestUse = use; bestHave = h;
            }
        }
        return best;
    }
}
