package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v2.2: the user's own per-ingredient category moves (generic; replaces the v2.1 botanical-fruit feature, which was
 * removed). Plain Java (same rules on the web, see js/core21.js PR.CatMoves).
 */
public final class CategoryMoves {
    private CategoryMoves() { }

    // ---------------- the user's category moves ----------------
    public static final int MAX_MOVES = 300;

    /** Category index or -1. */
    public static int categoryIndex(String name) { return name == null ? -1 : Arrays.asList(Catalog.CATEGORIES).indexOf(name); }

    /** Keeps only real catalog categories and sane keys (lower-case keys, at most 60 chars, at most MAX_MOVES). */
    public static Map<String, String> sanitize(Map<?, ?> in) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (in == null) return out;
        for (Map.Entry<?, ?> e : in.entrySet()) {
            if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String)) continue;
            String k = Normalizer.normalize((String) e.getKey());
            if (k.isEmpty() || k.length() > 60 || categoryIndex((String) e.getValue()) < 0) continue;
            out.put(k, (String) e.getValue());
            if (out.size() >= MAX_MOVES) break;
        }
        return out;
    }

    /** "key\tCategory\n" lines (prefs storage). */
    public static String format(Map<String, String> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) sb.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        return sb.toString();
    }

    public static Map<String, String> parse(String s) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        if (s == null) return m;
        for (String line : s.split("\n")) {
            int t = line.indexOf('\t');
            if (t > 0) m.put(line.substring(0, t), line.substring(t + 1));
        }
        return sanitize(m);
    }

    /** Category the catalog puts key in, or -1. */
    public static int defaultCategory(String key) {
        for (PantryIndex.Item it : PantryIndex.get().items) if (it.key.equals(key)) return it.category;
        return -1;
    }

    /**
     * Section c's chips with the user's moves applied: catalog entries ("#Header" and labels) minus items moved
     * elsewhere, then a "#Moved here" group. Labels moved in come from the catalog (or the key itself).
     */
    public static List<String> sectionItems(int c, Map<String, String> moves) {
        List<String> out = new ArrayList<String>();
        String pendingHeader = null;
        for (String s : Catalog.ITEMS[c]) {
            if (Catalog.isHeader(s)) { pendingHeader = s; continue; }
            String to = moves.get(Normalizer.normalize(s));
            if (to != null && categoryIndex(to) != c) continue;
            if (pendingHeader != null) { out.add(pendingHeader); pendingHeader = null; }
            out.add(s);
        }
        boolean head = false;
        for (Map.Entry<String, String> e : moves.entrySet()) {
            if (categoryIndex(e.getValue()) != c || defaultCategory(e.getKey()) == c) continue;
            if (!head) { out.add("#Moved here by you"); head = true; }
            out.add(labelFor(e.getKey()));
        }
        return out;
    }

    /** Keys shown in section c with moves applied (same order as sectionItems, headers skipped). */
    public static List<String> sectionKeys(int c, Map<String, String> moves) {
        List<String> keys = new ArrayList<String>();
        for (String s : sectionItems(c, moves)) if (!Catalog.isHeader(s)) keys.add(Normalizer.normalize(s));
        return keys;
    }

    public static int have(int c, Map<String, String> moves, Set<String> pantryKeys) {
        if (moves.isEmpty()) return PantryIndex.get().have(c, pantryKeys);
        int n = 0;
        for (String k : sectionKeys(c, moves)) if (pantryKeys.contains(k)) n++;
        return n;
    }

    static String labelFor(String key) {
        for (PantryIndex.Item it : PantryIndex.get().items) if (it.key.equals(key)) return it.label;
        return key.isEmpty() ? key : Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }
}
