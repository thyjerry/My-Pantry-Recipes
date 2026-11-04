package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v1.8: everything the Pantry screen needs from the catalog, computed once: each quick-add item's
 * key, accent-folded label and subheader, per-category keys, and a filter that does no per-keystroke
 * normalising of the 727 catalog items and 86 aliases (v1.7 ran ~1,600 regex passes per keystroke).
 */
public final class PantryIndex {
    public static final class Item {
        public final String label, key, folded, subFolded;
        public final int category;
        Item(String label, String sub, int category) {
            this.label = label;
            this.key = Normalizer.normalize(label);
            this.folded = Normalizer.fold(label);
            this.subFolded = sub;
            this.category = category;
        }
    }

    /** Filter result: labels to show (catalog order, aliases, then pantry items) and whether the query is an exact item. */
    public static final class Hits {
        public final List<String> labels = new ArrayList<String>();
        public final List<String> keys = new ArrayList<String>();
        public boolean exact;
    }

    private static volatile PantryIndex instance;

    public static PantryIndex get() {
        PantryIndex p = instance;
        if (p == null) {
            synchronized (PantryIndex.class) {
                if (instance == null) instance = new PantryIndex();
                p = instance;
            }
        }
        return p;
    }

    public final List<Item> items = new ArrayList<Item>();
    public final List<Item> aliases = new ArrayList<Item>();
    private final String[] catFolded = new String[Catalog.CATEGORIES.length];
    private final List<List<String>> sectionKeys = new ArrayList<List<String>>();

    private PantryIndex() {
        for (int c = 0; c < Catalog.CATEGORIES.length; c++) {
            catFolded[c] = Normalizer.fold(Catalog.CATEGORIES[c]);
            List<String> keys = new ArrayList<String>();
            String sub = "";
            for (String s : Catalog.ITEMS[c]) {
                if (Catalog.isHeader(s)) { sub = Normalizer.fold(s); continue; }
                Item it = new Item(s, sub, c);
                items.add(it);
                keys.add(it.key);
            }
            sectionKeys.add(keys);
        }
        for (String a : Catalog.ALIASES) aliases.add(new Item(a, "", -1));
    }

    public List<String> sectionKeys(int c) { return sectionKeys.get(c); }

    /** How many of category c's items are in the pantry. */
    public int have(int c, Set<String> pantryKeys) {
        int n = 0;
        for (String k : sectionKeys.get(c)) if (pantryKeys.contains(k)) n++;
        return n;
    }

    /**
     * Same results as the v1.7 filter: catalog items whose category, subheader or name contains the
     * folded query (or whose key equals the query's key), then aliases, then pantry items by label.
     * pantry maps key to label.
     */
    public Hits search(String query, Map<String, String> pantry) {
        Hits h = new Hits();
        String q = Normalizer.fold(query == null ? "" : query);
        String qk = Normalizer.normalize(query);
        if (q.isEmpty()) return h;
        Set<String> seen = new HashSet<String>();
        if (q.startsWith("botanic")) { // v2.1: the optional "botanical fruits" filter
            for (String l : Botany.LABELS) if (seen.add(Normalizer.normalize(l))) { h.labels.add(l); h.keys.add(Normalizer.normalize(l)); }
            h.exact = true;
            return h;
        }
        boolean[] catHit = new boolean[catFolded.length];
        for (int c = 0; c < catFolded.length; c++) catHit[c] = catFolded[c].contains(q);
        for (Item it : items) {
            boolean eq = it.key.equals(qk);
            if (eq) h.exact = true;
            if ((catHit[it.category] || it.subFolded.contains(q) || it.folded.contains(q) || eq) && seen.add(it.key)) {
                h.labels.add(it.label);
                h.keys.add(it.key);
            }
        }
        for (Item it : aliases) {
            boolean eq = it.key.equals(qk);
            if (eq) h.exact = true;
            if ((it.folded.contains(q) || eq) && seen.add(it.key)) {
                h.labels.add(it.label);
                h.keys.add(it.key);
            }
        }
        if (pantry != null) for (Map.Entry<String, String> e : pantry.entrySet()) {
            if (e.getKey().equals(qk)) h.exact = true;
            if (Normalizer.fold(e.getValue()).contains(q) && seen.add(e.getKey())) {
                h.labels.add(e.getValue());
                h.keys.add(e.getKey());
            }
        }
        return h;
    }

    /** Autocomplete names: catalog items, pantry labels, recipe ingredient names, then aliases; sorted. */
    public static List<String> ingredientNames(Collection<Recipe> recipes, Map<String, String> pantry) {
        return ingredientNames(recipes, pantry, null);
    }

    /** As above; keysOut (if not null) receives the key of every name (aliases excluded). */
    public static List<String> ingredientNames(Collection<Recipe> recipes, Map<String, String> pantry, Set<String> keysOut) {
        PantryIndex idx = get();
        java.util.LinkedHashMap<String, String> byKey = new java.util.LinkedHashMap<String, String>();
        for (Item it : idx.items) byKey.put(it.key, it.label);
        if (pantry != null) for (Map.Entry<String, String> e : pantry.entrySet()) if (!byKey.containsKey(e.getKey())) byKey.put(e.getKey(), e.getValue());
        for (Recipe r : recipes) {
            for (Ingredient i : r.ingredients) {
                if (i.key.isEmpty() || byKey.containsKey(i.key)) continue;
                String n = i.name;
                int comma = n.indexOf(',');
                if (comma > 0) n = n.substring(0, comma);
                n = n.trim();
                byKey.put(i.key, n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1));
            }
        }
        if (keysOut != null) keysOut.addAll(byKey.keySet());
        List<String> out = new ArrayList<String>(byKey.values());
        Set<String> have = new HashSet<String>(out);
        for (Item it : idx.aliases) if (have.add(it.label)) out.add(it.label);
        java.util.Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }
}
