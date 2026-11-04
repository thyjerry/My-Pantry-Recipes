package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** "Cooked this" log and leftover portions with a use-by date (default 3 days, editable). */
public final class Leftovers {
    private Leftovers() { }

    public static final int DEFAULT_DAYS = 3, MIN_DAYS = 1, MAX_DAYS = 14, MAX_PORTIONS = 99;
    public static final int MAX_ENTRIES = 200, MAX_LOG = 1000;

    public static final class Entry {
        public String id = "", recipe = "", title = "", cooked = "", useBy = "";
        public int portions;
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id); m.put("recipe", recipe); m.put("title", title); m.put("portions", (long) portions);
            m.put("cooked", cooked); m.put("useBy", useBy);
            return m;
        }
    }

    public static final class Cooked {
        public String date = "", recipe = "";
        public int servings;
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("date", date); m.put("id", recipe); m.put("servings", (long) servings);
            return m;
        }
    }

    public static int clampDays(int d) { return Math.max(MIN_DAYS, Math.min(MAX_DAYS, d)); }

    public static String nextId(List<Entry> list) {
        int n = 1;
        for (Entry e : list) {
            if (e.id.startsWith("l") && e.id.length() > 1) {
                try { n = Math.max(n, Integer.parseInt(e.id.substring(1)) + 1); } catch (NumberFormatException ignored) { }
            }
        }
        return "l" + n;
    }

    /** Adds leftovers for a cooking session; portions <= 0 adds nothing (returns null). */
    public static Entry add(List<Entry> list, String recipeId, String title, int portions, String today, int days) {
        if (portions <= 0 || !Dates.valid(today)) return null;
        Entry e = new Entry();
        e.id = nextId(list);
        e.recipe = recipeId == null ? "" : recipeId;
        e.title = title == null ? "" : (title.length() > 120 ? title.substring(0, 120) : title);
        e.portions = Math.min(MAX_PORTIONS, portions);
        e.cooked = today;
        e.useBy = Dates.addDays(today, clampDays(days));
        list.add(0, e);
        while (list.size() > MAX_ENTRIES) list.remove(list.size() - 1);
        return e;
    }

    /** Changes the portion count; an entry at 0 is removed. Returns the new count (0 = removed). */
    public static int adjust(List<Entry> list, String id, int delta) {
        for (int k = 0; k < list.size(); k++) {
            Entry e = list.get(k);
            if (!e.id.equals(id)) continue;
            e.portions = Math.max(0, Math.min(MAX_PORTIONS, e.portions + delta));
            if (e.portions == 0) list.remove(k);
            return e.portions;
        }
        return 0;
    }

    /** UseBy status words (expired/today/soon/ok) reused for leftovers. */
    public static String status(Entry e, String today) {
        return UseBy.status(UseBy.daysLeft(e.useBy, today));
    }

    /** Leftovers sorted by use-by date (soonest first), then newest cooked. */
    public static List<Entry> sorted(List<Entry> list) {
        List<Entry> out = new ArrayList<Entry>(list);
        java.util.Collections.sort(out, new java.util.Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                int c = a.useBy.compareTo(b.useBy);
                if (c != 0) return c;
                c = b.cooked.compareTo(a.cooked);
                return c != 0 ? c : a.id.compareTo(b.id);
            }
        });
        return out;
    }

    public static Cooked log(List<Cooked> log, String recipeId, int servings, String today) {
        Cooked c = new Cooked();
        c.date = today;
        c.recipe = recipeId;
        c.servings = Math.max(1, Math.min(MealPlan.MAX_SERVINGS * 3, servings));
        log.add(0, c);
        while (log.size() > MAX_LOG) log.remove(log.size() - 1);
        return c;
    }

    /** How many times a recipe was cooked, and the last date ("" if never). */
    public static Object[] history(List<Cooked> log, String recipeId) {
        int n = 0;
        String last = "";
        for (Cooked c : log) if (c.recipe.equals(recipeId)) { n++; if (c.date.compareTo(last) > 0) last = c.date; }
        return new Object[]{n, last};
    }
}
