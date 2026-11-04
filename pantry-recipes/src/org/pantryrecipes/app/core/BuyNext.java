package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Buy these next": the 1-3 missing ingredients that unlock the most recipes. Only recipes missing exactly one
 * or two ingredients count. The caller passes match results that already respect diets, allergies, dislikes and
 * accepted substitutions. Greedy: each pick maximises the recipes newly completed by the picks so far
 * (ties: more recipes that need it, then alphabetical key).
 */
public final class BuyNext {
    private BuyNext() { }

    public static final int MAX = 3;

    public static final class Pick {
        public final String key, name;
        /** Recipes that become fully makeable once this and the earlier picks are bought. */
        public final List<String> unlocks = new ArrayList<String>();
        Pick(String key, String name) { this.key = key; this.name = name; }
    }

    /** ids[i] needs missing.get(i) (keys); names gives a display name per key. */
    public static List<Pick> suggest(List<String> ids, List<List<String>> missing, Map<String, String> names, int max) {
        List<Integer> pool = new ArrayList<Integer>();
        Map<String, Integer> demand = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < ids.size(); i++) {
            List<String> m = missing.get(i);
            Set<String> u = new HashSet<String>(m);
            if (u.size() < 1 || u.size() > 2) continue;
            pool.add(i);
            for (String k : u) demand.put(k, demand.containsKey(k) ? demand.get(k) + 1 : 1);
        }
        // v2.1 (HD8): one pass per round. A recipe can be completed by key k only when k is its last unchosen missing
        // item, so each round buckets the open recipes by that item instead of re-testing every key against every
        // recipe. Same picks, same order and tie-breaks as before (checked against the old loop in V21Tests).
        List<Pick> out = new ArrayList<Pick>();
        Set<String> chosen = new HashSet<String>();
        Set<Integer> done = new HashSet<Integer>();
        for (int round = 0; round < max; round++) {
            Map<String, List<Integer>> nwBy = new java.util.HashMap<String, List<Integer>>();
            for (int i : pool) {
                if (done.contains(i)) continue;
                String last = null;
                boolean one = true;
                for (String m : missing.get(i)) {
                    if (chosen.contains(m) || m.equals(last)) continue;
                    if (last == null) last = m; else { one = false; break; }
                }
                if (!one || last == null) continue;
                List<Integer> l = nwBy.get(last);
                if (l == null) { l = new ArrayList<Integer>(); nwBy.put(last, l); }
                l.add(i);
            }
            String best = null;
            List<Integer> bestNew = null;
            for (String k : demand.keySet()) {
                if (chosen.contains(k)) continue;
                List<Integer> nw = nwBy.get(k);
                if (nw == null || nw.isEmpty()) continue;
                if (best == null || nw.size() > bestNew.size()
                        || (nw.size() == bestNew.size() && (demand.get(k) > demand.get(best)
                        || (demand.get(k).equals(demand.get(best)) && k.compareTo(best) < 0)))) {
                    best = k;
                    bestNew = nw;
                }
            }
            if (best == null) break;
            chosen.add(best);
            String nm = names == null ? null : names.get(best);
            Pick p = new Pick(best, nm == null || nm.isEmpty() ? best : nm);
            for (int i : bestNew) { done.add(i); p.unlocks.add(ids.get(i)); }
            out.add(p);
        }
        return out;
    }

    /** Convenience over Matcher results (keys of the missing ingredients; display names from the recipes). */
    public static List<Pick> fromResults(List<Matcher.Result> results, int max) {
        List<String> ids = new ArrayList<String>();
        List<List<String>> miss = new ArrayList<List<String>>();
        Map<String, String> names = new LinkedHashMap<String, String>();
        for (Matcher.Result r : results) {
            List<String> m = new ArrayList<String>();
            for (Ingredient i : r.missing) {
                m.add(i.key);
                if (!names.containsKey(i.key)) names.put(i.key, shortName(i.name));
            }
            ids.add(r.recipe.id);
            miss.add(m);
        }
        return suggest(ids, miss, names, max);
    }

    // ---------------- v2.1: expandable suggestions, "see all", sorting ----------------

    /** How many suggestions "Buy these next" shows before "See all". */
    public static final int TOP = 3, PAGE = 20;
    public static final String SORT_RECIPES = "recipes", SORT_CHEAPEST = "cheapest";

    /** One missing ingredient, ranked on its own (not greedy): what buying just this one does. */
    public static final class Option {
        public final String key, name;
        /** Recipes it completes on its own (they miss only this). */
        public final List<String> alone = new ArrayList<String>();
        /** Recipes that miss this and one other item: buying it gets them within one. */
        public final List<String> closer = new ArrayList<String>();
        Option(String key, String name) { this.key = key; this.name = name; }
        public int demand() { return alone.size() + closer.size(); }
    }

    /** Every missing ingredient of the recipes 1-2 items away, most recipes completed first (ties: more recipes
     *  that need it, then key). One pass over the results; nothing is computed for recipes further away. */
    public static List<Option> ranked(List<String> ids, List<List<String>> missing, Map<String, String> names) {
        Map<String, Option> by = new LinkedHashMap<String, Option>();
        for (int i = 0; i < ids.size(); i++) {
            Set<String> u = new HashSet<String>(missing.get(i));
            if (u.size() < 1 || u.size() > 2) continue;
            for (String k : u) {
                Option o = by.get(k);
                if (o == null) {
                    String nm = names == null ? null : names.get(k);
                    o = new Option(k, nm == null || nm.isEmpty() ? k : nm);
                    by.put(k, o);
                }
                (u.size() == 1 ? o.alone : o.closer).add(ids.get(i));
            }
        }
        List<Option> out = new ArrayList<Option>(by.values());
        java.util.Collections.sort(out, new java.util.Comparator<Option>() {
            @Override public int compare(Option a, Option b) {
                if (a.alone.size() != b.alone.size()) return b.alone.size() - a.alone.size();
                if (a.demand() != b.demand()) return b.demand() - a.demand();
                return a.key.compareTo(b.key);
            }
        });
        return out;
    }

    public static List<Option> rankedFromResults(List<Matcher.Result> results) {
        List<String> ids = new ArrayList<String>();
        List<List<String>> miss = new ArrayList<List<String>>();
        Map<String, String> names = new LinkedHashMap<String, String>();
        for (Matcher.Result r : results) {
            if (r.missing.size() < 1 || r.missing.size() > 2) continue;
            List<String> m = new ArrayList<String>();
            for (Ingredient i : r.missing) {
                m.add(i.key);
                if (!names.containsKey(i.key)) names.put(i.key, shortName(i.name));
            }
            ids.add(r.recipe.id);
            miss.add(m);
        }
        return ranked(ids, miss, names);
    }

    /** Re-sorts a ranked list: "cheapest" puts the lowest known pack price first (unpriced last, rank order kept
     *  for ties); anything else keeps the recipes order. Returns a new list. */
    public static List<Option> sort(List<Option> ranked, String by, final Map<String, Cost.Price> user, final Map<String, Cost.Price> defaults) {
        List<Option> out = new ArrayList<Option>(ranked);
        if (!SORT_CHEAPEST.equals(by)) return out;
        final Map<String, Double> price = new java.util.HashMap<String, Double>();
        for (Option o : out) { Cost.Price p = Cost.find(o.key, user, defaults); price.put(o.key, p == null ? Double.NaN : p.price); }
        java.util.Collections.sort(out, new java.util.Comparator<Option>() { // stable: ties keep the rank order
            @Override public int compare(Option a, Option b) {
                double x = price.get(a.key), y = price.get(b.key);
                boolean nx = Double.isNaN(x), ny = Double.isNaN(y);
                if (nx || ny) return nx == ny ? 0 : nx ? 1 : -1;
                return Double.compare(x, y);
            }
        });
        return out;
    }

    /** Why a top pick is suggested. index 0 is the first pick; later picks count the earlier ones as bought. */
    public static String whyPick(Pick p, int index, Option o) {
        StringBuilder b = new StringBuilder();
        int n = p.unlocks.size();
        if (index == 0) b.append("Buying it completes ").append(n).append(n == 1 ? " recipe" : " recipes").append(" you're one ingredient away from");
        else b.append("With the item").append(index > 1 ? "s" : "").append(" above, it completes ").append(n).append(n == 1 ? " more recipe" : " more recipes");
        if (o != null && o.demand() > n) b.append("; ").append(o.demand()).append(o.demand() == 1 ? " recipe" : " recipes").append(" in all use it");
        return b.append('.').toString();
    }

    public static String why(Option o) {
        int a = o.alone.size(), c = o.closer.size();
        StringBuilder b = new StringBuilder();
        if (a > 0) b.append("Completes ").append(a).append(a == 1 ? " recipe" : " recipes").append(" on its own");
        if (c > 0) b.append(a > 0 ? " and brings " : "Brings ").append(c).append(c == 1 ? " more recipe" : " more recipes").append(" within one ingredient");
        return b.append('.').toString();
    }

    /** "About $2.50 for 500 g (average price)" / "Your price: ..." / null when there is no price. money is the formatted pack price. */
    public static String costText(Cost.Price p, String money, boolean mine) {
        if (p == null) return null;
        String pack = p.unit.isEmpty() ? (p.amount == 1 ? money + " each" : money + " for " + Quantity.number(p.amount, ""))
                : money + " for " + Quantity.number(p.amount, p.unit) + " " + p.unit;
        return mine ? "Your price: " + pack : "About " + pack + " (average price)";
    }

    static String shortName(String n) {
        String s = n == null ? "" : n;
        int c = s.indexOf(',');
        if (c > 0) s = s.substring(0, c);
        c = s.indexOf('(');
        if (c > 0) s = s.substring(0, c);
        s = s.trim();
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
