package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * v2.0 Wi-Fi sync data model. Everything in a backup (except photos) is split into small items keyed like
 * "pantry/garlic" or "plan/2026-11-02/b123"; each item carries its value (canonical JSON, or null for a deletion
 * "tombstone"), the time it last changed and the device that changed it. Merging keeps, per item, the newest change
 * (ties: the larger device id); when both devices changed the same item since they last synced, the losing value is
 * written to the conflict log so nothing is lost silently.
 */
public final class SyncDoc {
    private SyncDoc() { }

    public static final long TOMBSTONE_DAYS = 90;
    public static final int MAX_CONFLICTS = 200;

    public static final class Entry {
        public final String v;   // null = deleted
        public final long t;
        public final String d;
        public Entry(String v, long t, String d) { this.v = v; this.t = t; this.d = d == null ? "" : d; }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("v", v);
            m.put("t", t);
            m.put("d", d);
            return m;
        }
        boolean sameValue(Entry o) { return o != null && (v == null ? o.v == null : v.equals(o.v)); }
    }

    public static final class Conflict {
        public String key = "", kept = "", lost = "", lostDevice = "", keptDevice = "";
        public long at;
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("key", key); m.put("kept", kept); m.put("lost", lost);
            m.put("keptDevice", keptDevice); m.put("lostDevice", lostDevice); m.put("at", at);
            return m;
        }
        @SuppressWarnings("unchecked")
        public static Conflict fromMap(Object o) {
            if (!(o instanceof Map)) return null;
            Map<String, Object> m = (Map<String, Object>) o;
            Conflict c = new Conflict();
            c.key = Json.str(m, "key", ""); c.kept = Json.str(m, "kept", ""); c.lost = Json.str(m, "lost", "");
            c.keptDevice = Json.str(m, "keptDevice", ""); c.lostDevice = Json.str(m, "lostDevice", "");
            Object a = m.get("at");
            c.at = a instanceof Number ? ((Number) a).longValue() : 0;
            return c.key.isEmpty() ? null : c;
        }
    }

    // ---------------- backup <-> items ----------------

    private static String j(Object o) { return Json.write(o); }

    /** Splits backup data into sync items (sorted keys). Photos are not synced. */
    @SuppressWarnings("unchecked")
    public static TreeMap<String, String> items(Backup.Data d) {
        Map<String, Object> m = Backup.toMap(d);
        TreeMap<String, String> out = new TreeMap<String, String>();
        for (Object o : (List<Object>) m.get("pantry")) {
            Map<String, Object> e = (Map<String, Object>) o;
            out.put("pantry/" + e.get("key"), j(e));
        }
        for (Object o : (List<Object>) m.get("favorites")) out.put("fav/" + o, "true");
        for (Object o : (List<Object>) m.get("shopping")) {
            Map<String, Object> e = (Map<String, Object>) o;
            String k = "shop/" + Normalizer.normalize(String.valueOf(e.get("name"))) + "|" + String.valueOf(e.get("name")).toLowerCase(java.util.Locale.ROOT)
                    + "|" + (e.containsKey("unit") ? e.get("unit") : "") + "|" + (e.containsKey("recipe") ? e.get("recipe") : "");
            if (!out.containsKey(k)) out.put(k, j(e));
        }
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("ratings")).entrySet()) out.put("rating/" + e.getKey(), j(e.getValue()));
        for (Object o : (List<Object>) m.get("plan")) {
            Map<String, Object> e = (Map<String, Object>) o;
            String k = "plan/" + e.get("date") + "/" + e.get("id");
            if (!out.containsKey(k)) out.put(k, j(e.get("servings")));
        }
        for (Object o : (List<Object>) m.get("dislikes")) out.put("dislike/" + o, "true");
        for (Object o : (List<Object>) m.get("allergens")) out.put("allergen/" + o, "true");
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("settings")).entrySet()) out.put("setting/" + e.getKey(), j(e.getValue()));
        if (m.get("theme") != null) out.put("theme", j(m.get("theme")));
        for (Object o : (List<Object>) m.get("recipes")) {
            Map<String, Object> e = (Map<String, Object>) o;
            out.put("recipe/" + e.get("id"), j(e));
        }
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("prices")).entrySet()) out.put("price/" + e.getKey(), j(e.getValue()));
        for (Object o : (List<Object>) m.get("leftovers")) {
            Map<String, Object> e = (Map<String, Object>) o;
            out.put("leftover/" + e.get("id"), j(e));
        }
        for (Object o : (List<Object>) m.get("cooked")) {
            Map<String, Object> e = (Map<String, Object>) o;
            String k = "cooked/" + e.get("date") + "/" + e.get("id");
            if (!out.containsKey(k)) out.put(k, j(e));
        }
        for (Map.Entry<String, Object> e : ((Map<String, Object>) m.get("barcodes")).entrySet()) out.put("barcode/" + e.getKey(), j(e.getValue()));
        return out;
    }

    /** Rebuilds a backup JSON text from live (non-deleted) items, for Backup.parse and the normal import path. */
    public static String assemble(Map<String, Entry> doc, String appVersion, String platform, String today) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("app", Backup.APP);
        m.put("format", (long) Backup.FORMAT);
        m.put("appVersion", appVersion);
        m.put("platform", platform);
        m.put("exported", today);
        List<Object> pantry = new ArrayList<Object>(), favs = new ArrayList<Object>(), shop = new ArrayList<Object>(), plan = new ArrayList<Object>(),
                dis = new ArrayList<Object>(), al = new ArrayList<Object>(), recipes = new ArrayList<Object>(), lo = new ArrayList<Object>(), ck = new ArrayList<Object>();
        Map<String, Object> ratings = new LinkedHashMap<String, Object>(), settings = new LinkedHashMap<String, Object>(),
                prices = new LinkedHashMap<String, Object>(), barcodes = new LinkedHashMap<String, Object>();
        Object theme = null;
        for (Map.Entry<String, Entry> e : new TreeMap<String, Entry>(doc).entrySet()) {
            String k = e.getKey(), v = e.getValue().v;
            if (v == null) continue;
            Object val;
            try { val = Json.parse(v); } catch (RuntimeException ex) { continue; }
            int s = k.indexOf('/');
            String type = s < 0 ? k : k.substring(0, s), rest = s < 0 ? "" : k.substring(s + 1);
            if (type.equals("pantry")) pantry.add(val);
            else if (type.equals("fav")) favs.add(rest);
            else if (type.equals("shop")) shop.add(val);
            else if (type.equals("rating")) ratings.put(rest, val);
            else if (type.equals("plan")) {
                int p = rest.indexOf('/');
                if (p < 0) continue;
                Map<String, Object> o = new LinkedHashMap<String, Object>();
                o.put("date", rest.substring(0, p)); o.put("id", rest.substring(p + 1)); o.put("servings", val);
                plan.add(o);
            }
            else if (type.equals("dislike")) dis.add(rest);
            else if (type.equals("allergen")) al.add(rest);
            else if (type.equals("setting")) settings.put(rest, val);
            else if (type.equals("theme")) theme = val;
            else if (type.equals("recipe")) recipes.add(val);
            else if (type.equals("price")) prices.put(rest, val);
            else if (type.equals("leftover")) lo.add(val);
            else if (type.equals("cooked")) ck.add(val);
            else if (type.equals("barcode")) barcodes.put(rest, val);
        }
        m.put("pantry", pantry); m.put("favorites", favs); m.put("shopping", shop); m.put("ratings", ratings); m.put("plan", plan);
        m.put("dislikes", dis); m.put("allergens", al); m.put("settings", settings);
        if (theme != null) m.put("theme", theme);
        m.put("recipes", recipes); m.put("prices", prices); m.put("leftovers", lo); m.put("cooked", ck); m.put("barcodes", barcodes);
        return Json.write(m);
    }

    // ---------------- change tracking and merging ----------------

    /**
     * Records local changes: items whose value differs from the state get time {@code now}; items missing from
     * {@code current} become tombstones. Returns the number of changed items. Old tombstones are dropped.
     */
    public static int track(Map<String, Entry> state, Map<String, String> current, long now, String device) {
        int n = 0;
        for (Map.Entry<String, String> e : current.entrySet()) {
            Entry old = state.get(e.getKey());
            if (old == null || old.v == null || !old.v.equals(e.getValue())) {
                state.put(e.getKey(), new Entry(e.getValue(), Math.max(now, old == null ? 0 : old.t + 1), device));
                n++;
            }
        }
        for (Map.Entry<String, Entry> e : new ArrayList<Map.Entry<String, Entry>>(state.entrySet())) {
            if (e.getValue().v != null && !current.containsKey(e.getKey())) {
                state.put(e.getKey(), new Entry(null, Math.max(now, e.getValue().t + 1), device));
                n++;
            }
        }
        gc(state, now);
        return n;
    }

    public static void gc(Map<String, Entry> state, long now) {
        long cut = now - TOMBSTONE_DAYS * 86400000L;
        java.util.Iterator<Map.Entry<String, Entry>> it = state.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            if (e.v == null && e.t < cut) it.remove();
        }
    }

    /** a wins over b? Newer time, then larger device id, then larger value text (fully deterministic). */
    static boolean wins(Entry a, Entry b) {
        if (a.t != b.t) return a.t > b.t;
        int c = a.d.compareTo(b.d);
        if (c != 0) return c > 0;
        String av = a.v == null ? "" : a.v, bv = b.v == null ? "" : b.v;
        return av.compareTo(bv) >= 0;
    }

    /**
     * Merges two documents. Both changed since {@code since} (the last successful sync between them, 0 = never)
     * with different values = a conflict: the newer change wins, the other is logged. Never throws.
     */
    public static TreeMap<String, Entry> merge(Map<String, Entry> a, Map<String, Entry> b, long since, List<Conflict> conflicts, long now) {
        TreeMap<String, Entry> out = new TreeMap<String, Entry>();
        java.util.Set<String> keys = new java.util.TreeSet<String>(a.keySet());
        keys.addAll(b.keySet());
        for (String k : keys) {
            Entry x = a.get(k), y = b.get(k);
            if (x == null) { out.put(k, y); continue; }
            if (y == null) { out.put(k, x); continue; }
            Entry w = wins(x, y) ? x : y, l = w == x ? y : x;
            out.put(k, w);
            if (!x.sameValue(y) && x.t > since && y.t > since && conflicts != null && !(l.v == null && w.v == null)) {
                Conflict c = new Conflict();
                c.key = k;
                c.kept = w.v == null ? "(deleted)" : w.v;
                c.lost = l.v == null ? "(deleted)" : l.v;
                c.keptDevice = w.d;
                c.lostDevice = l.d;
                c.at = now;
                conflicts.add(c);
            }
        }
        return out;
    }

    /** Items whose value differs between two documents (what applying the merge would change). */
    public static int differences(Map<String, Entry> a, Map<String, Entry> b) {
        int n = 0;
        java.util.Set<String> keys = new java.util.HashSet<String>(a.keySet());
        keys.addAll(b.keySet());
        for (String k : keys) {
            Entry x = a.get(k), y = b.get(k);
            String xv = x == null ? null : x.v, yv = y == null ? null : y.v;
            if (xv == null ? yv != null : !xv.equals(yv)) n++;
        }
        return n;
    }

    /** Human-readable label for an item key ("Pantry: garlic", "Plan: 2026-11-02"). */
    public static String describe(String key) {
        int s = key.indexOf('/');
        String type = s < 0 ? key : key.substring(0, s), rest = s < 0 ? "" : key.substring(s + 1);
        String[][] names = {{"pantry", "Pantry"}, {"fav", "Favorite"}, {"shop", "Shopping"}, {"rating", "Rating/note"}, {"plan", "Meal plan"},
                {"dislike", "Dislike"}, {"allergen", "Allergy"}, {"setting", "Setting"}, {"theme", "Appearance"}, {"recipe", "Own recipe"},
                {"price", "Price"}, {"leftover", "Leftover"}, {"cooked", "Cooking log"}, {"barcode", "Barcode"}};
        String n = type;
        for (String[] x : names) if (x[0].equals(type)) n = x[1];
        if (type.equals("shop")) { String[] p = rest.split("\\|", -1); rest = p.length > 1 ? p[1] : rest; }
        return rest.isEmpty() ? n : n + ": " + rest;
    }

    // ---------------- persistence ----------------

    public static String writeDoc(Map<String, Entry> doc) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Entry> e : new TreeMap<String, Entry>(doc).entrySet()) m.put(e.getKey(), e.getValue().toMap());
        return Json.write(m);
    }

    /** Reads a document; invalid entries are skipped (never throws). Values must themselves be JSON (or null). */
    @SuppressWarnings("unchecked")
    public static TreeMap<String, Entry> readDoc(String json) {
        TreeMap<String, Entry> out = new TreeMap<String, Entry>();
        Object o;
        try { o = Json.parse(json); } catch (RuntimeException e) { return out; }
        if (!(o instanceof Map)) return out;
        for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
            if (!(e.getValue() instanceof Map) || e.getKey().isEmpty() || e.getKey().length() > 300) continue;
            Map<String, Object> x = (Map<String, Object>) e.getValue();
            Object v = x.get("v"), t = x.get("t"), d = x.get("d");
            if (!(t instanceof Number) || !(v == null || v instanceof String) || !(d instanceof String)) continue;
            if (v != null && ((String) v).length() > 400000) continue;
            out.put(e.getKey(), new Entry((String) v, ((Number) t).longValue(), (String) d));
            if (out.size() > 50000) break;
        }
        return out;
    }

    public static String writeConflicts(List<Conflict> cs) {
        List<Object> l = new ArrayList<Object>();
        int from = Math.max(0, cs.size() - MAX_CONFLICTS);
        for (int i = from; i < cs.size(); i++) l.add(cs.get(i).toMap());
        return Json.write(l);
    }

    public static List<Conflict> readConflicts(String json) {
        List<Conflict> out = new ArrayList<Conflict>();
        try {
            Object o = Json.parse(json);
            if (o instanceof List) for (Object x : (List<?>) o) { Conflict c = Conflict.fromMap(x); if (c != null) out.add(c); }
        } catch (RuntimeException ignored) { }
        return out;
    }

    /** Puts a logged losing value back as a new local change (the user's "Restore" on a conflict). */
    public static void restore(Map<String, Entry> state, Conflict c, long now, String device) {
        String v = "(deleted)".equals(c.lost) ? null : c.lost;
        Entry old = state.get(c.key);
        state.put(c.key, new Entry(v, Math.max(now, old == null ? 0 : old.t + 1), device));
    }

    public static List<String> sortedKeys(Map<String, ?> m) {
        List<String> l = new ArrayList<String>(m.keySet());
        Collections.sort(l);
        return l;
    }
}
