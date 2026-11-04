package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Supermarket aisles for the shopping list, mapped from the 29 pantry categories ("Other" when unknown). */
public final class Aisles {
    private Aisles() { }

    public static final String OTHER = "Other";
    public static final String[] ORDER = {"Produce", "Meat & poultry", "Fish & seafood", "Deli", "Dairy & eggs",
            "Chilled & plant-based", "Bakery", "Pasta, rice & grains", "Cans, jars & beans", "Baking", "Spices & dried herbs",
            "Oils, sauces & condiments", "World foods", "Nuts & snacks", "Frozen", "Drinks", OTHER};

    /** Aisle for each Catalog.CATEGORIES entry, same order. */
    public static final String[] FOR_CATEGORY = {"Produce", "Produce", "Produce", "Spices & dried herbs", "Meat & poultry",
            "Meat & poultry", "Fish & seafood", "Deli", "Dairy & eggs", "Dairy & eggs", "Chilled & plant-based", "Pasta, rice & grains",
            "Pasta, rice & grains", "Bakery", "Cans, jars & beans", "Nuts & snacks", "Baking", "Baking", "Oils, sauces & condiments",
            "Oils, sauces & condiments", "Cans, jars & beans", "Frozen", "Nuts & snacks", "Drinks", "World foods", "World foods",
            "World foods", "World foods", "World foods"};

    private static Map<String, String> byKey;

    private static synchronized Map<String, String> map() {
        if (byKey != null) return byKey;
        Map<String, String> m = new HashMap<String, String>();
        for (int c = 0; c < Catalog.ITEMS.length; c++) {
            String aisle = FOR_CATEGORY[c];
            for (String s : Catalog.ITEMS[c]) {
                if (Catalog.isHeader(s)) {
                    // dried herbs live with the spices, not in produce
                    aisle = s.equals("#Dried herbs") ? "Spices & dried herbs" : FOR_CATEGORY[c];
                    continue;
                }
                String k = Normalizer.normalize(s);
                if (!k.isEmpty() && !m.containsKey(k)) m.put(k, aisle);
            }
        }
        byKey = m;
        return m;
    }

    /** Aisle for an ingredient name or key. */
    public static String of(String name) {
        String k = Normalizer.normalize(name == null ? "" : name);
        if (k.isEmpty()) return OTHER;
        String a = map().get(k);
        if (a == null) a = map().get(Normalizer.family(k));
        return a == null ? OTHER : a;
    }

    /** Groups item names by aisle in store order; each value lists indexes into {@code names}. */
    public static LinkedHashMap<String, List<Integer>> group(List<String> names) {
        Map<String, List<Integer>> tmp = new HashMap<String, List<Integer>>();
        for (int i = 0; i < names.size(); i++) {
            String a = of(names.get(i));
            List<Integer> l = tmp.get(a);
            if (l == null) tmp.put(a, l = new ArrayList<Integer>());
            l.add(i);
        }
        LinkedHashMap<String, List<Integer>> out = new LinkedHashMap<String, List<Integer>>();
        for (String a : ORDER) if (tmp.containsKey(a)) out.put(a, tmp.get(a));
        return out;
    }
}
