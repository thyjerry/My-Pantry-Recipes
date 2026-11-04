package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v2.1: "botanically a fruit" facts and the user's own category moves. Plain Java (same rules on the web, see
 * js/core21.js). A fruit, botanically, is the part of a plant that grows from the flower and holds the seeds;
 * many of these are cooked and sold as vegetables, and the app keeps most of them in Vegetables.
 */
public final class Botany {
    private Botany() { }

    /** Catalog labels (any category) that are botanically fruits but are often thought of as vegetables. */
    public static final List<String> LABELS = Collections.unmodifiableList(Arrays.asList(
            "Tomato", "Cherry tomatoes", "Cucumber", "Avocado", "Bell pepper", "Chili", "Jalapeno", "Poblano", "Habanero",
            "Eggplant", "Zucchini", "Pumpkin", "Acorn squash", "Spaghetti squash", "Delicata squash", "Olives", "Tomatillos",
            "Okra", "Green beans", "Snow peas", "Sweetcorn", "Chayote", "Breadfruit", "Plantains"));

    private static final String[][] FACTS = {
            {"Tomato", "Tomatoes are berries: each one grows from a flower and is full of seeds."},
            {"Cherry tomatoes", "Cherry tomatoes are small berries, like all tomatoes."},
            {"Cucumber", "Cucumbers are a kind of berry from the gourd family, like melons."},
            {"Avocado", "An avocado is a large berry with one big seed."},
            {"Bell pepper", "Peppers are berries; the seeds sit inside the hollow fruit."},
            {"Eggplant", "Eggplant is a berry and a cousin of the tomato."},
            {"Zucchini", "Zucchini is a squash: the fruit of a vine, picked young."},
            {"Pumpkin", "A pumpkin is a giant berry from the squash family."},
            {"Olives", "Olives are stone fruits, like cherries and plums."},
            {"Green beans", "Green beans are seed pods, and a pod is the fruit of the bean plant."},
            {"Snow peas", "Snow peas are eaten pod and all, and the pod is the fruit."},
            {"Sweetcorn", "Each corn kernel is a tiny dry fruit with one seed."},
            {"Okra", "Okra pods are the fruit of a plant in the hibiscus family."},
            {"Plantains", "Plantains are bananas for cooking, so they are fruits."},
    };

    /** Glossary entry, with a fun fact. */
    public static final String GLOSSARY_TERM = "Botanical fruit";
    public static final String GLOSSARY = "The part of a plant that grows from its flower and holds its seeds. Cooks sort food by taste "
            + "and use, so many botanical fruits (tomatoes, peppers, squash, cucumbers) are cooked as vegetables.";
    public static final String FUN_FACT = "Fun fact: in 1893 the U.S. Supreme Court (Nix v. Hedden) ruled that tomatoes count as "
            + "vegetables for import taxes, while agreeing that botanists call them fruits.";

    private static volatile Map<String, String> byKey;

    private static Map<String, String> map() {
        Map<String, String> m = byKey;
        if (m == null) {
            m = new LinkedHashMap<String, String>();
            for (String l : LABELS) m.put(Normalizer.normalize(l), l);
            byKey = m;
        }
        return m;
    }

    public static boolean isBotanicalFruit(String key) { return key != null && map().containsKey(key); }

    /** Catalog label for a botanical-fruit key, or null. */
    public static String label(String key) { return key == null ? null : map().get(key); }

    /** One short line about why it is a fruit. */
    public static String fact(String key) {
        String l = label(key);
        if (l == null) return null;
        for (String[] f : FACTS) if (f[0].equals(l)) return f[1];
        if (l.contains("squash")) return "Squashes are the fruits of a vine; the seeds are inside.";
        if (l.equals("Chili") || l.equals("Jalapeno") || l.equals("Poblano") || l.equals("Habanero")) return "Chilies are peppers, and peppers are berries.";
        return "It grows from the plant's flower and holds its seeds, so botanists call it a fruit.";
    }

    /** True when a pantry filter query asks for the botanical-fruit list ("botanical fruits", "botanically a fruit"). */
    public static boolean isQuery(String q) {
        String f = Normalizer.fold(q == null ? "" : q).trim();
        return f.equals("botanical fruit") || f.equals("botanical fruits") || f.equals("botanically a fruit") || f.equals("botanically fruit");
    }

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
