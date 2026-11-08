package org.pantryrecipes.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.pantryrecipes.app.core.Allergens;
import org.pantryrecipes.app.core.Backup;
import org.pantryrecipes.app.core.Catalog;
import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.MealPlan;
import org.pantryrecipes.app.core.MealType;
import org.pantryrecipes.app.core.Normalizer;
import org.pantryrecipes.app.core.Recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persistent state: pantry, staples, favorites, shopping list (SQLite) and settings (prefs). */
final class Store extends SQLiteOpenHelper {
    static final class PantryItem {
        final String key;
        String label;
        boolean staple;
        String useBy = "";   // yyyy-mm-dd or ""
        String qty = "";     // free text such as "500 g", or ""

        PantryItem(String key, String label, boolean staple) {
            this.key = key;
            this.label = label;
            this.staple = staple;
        }
    }

    static final class ShopItem {
        long id;
        String key, name, unit, recipe;
        double amount;
        boolean checked;
    }

    private static Store instance;
    /** v1.8: pantry-table writes run here, in order, so a tap never waits for SQLite on the UI thread. */
    private final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();

    private void write(Runnable r) {
        try { io.execute(r); } catch (Exception e) { r.run(); }
    }

    /** Waits for queued pantry writes (before anything reads the pantry table back). */
    void flushIo() {
        try { io.submit(new Runnable() { public void run() { } }).get(); } catch (Exception ignored) { }
    }
    final SharedPreferences prefs;
    final LinkedHashMap<String, PantryItem> pantry = new LinkedHashMap<String, PantryItem>();
    final Set<String> favorites = new HashSet<String>();
    final LinkedHashMap<String, Backup.Rating> ratings = new LinkedHashMap<String, Backup.Rating>();

    static synchronized Store get(Context c) {
        if (instance == null) instance = new Store(c.getApplicationContext());
        return instance;
    }

    private final Context appCtx;

    Context appContext() { return appCtx; }

    private Store(Context c) {
        super(c, "pantry.db", null, 3);
        appCtx = c;
        prefs = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
        loadPantry();
        loadFavorites();
        loadRatings();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pantry (key TEXT PRIMARY KEY, label TEXT, staple INTEGER, added INTEGER, useBy TEXT, qty TEXT)");
        db.execSQL("CREATE TABLE favorites (id TEXT PRIMARY KEY, added INTEGER)");
        db.execSQL("CREATE TABLE shopping (_id INTEGER PRIMARY KEY AUTOINCREMENT, key TEXT, name TEXT, amount REAL, "
                + "unit TEXT, recipe TEXT, checked INTEGER, added INTEGER)");
        db.execSQL("CREATE TABLE cache (id TEXT PRIMARY KEY, json TEXT, fetched INTEGER)");
        db.execSQL("CREATE TABLE seen (id TEXT PRIMARY KEY, at INTEGER)");
        createV3Tables(db);
        long now = System.currentTimeMillis();
        for (String s : Catalog.DEFAULT_STAPLES) {
            ContentValues v = new ContentValues();
            v.put("key", Normalizer.normalize(s));
            v.put("label", s);
            v.put("staple", 1);
            v.put("added", now++);
            db.insert("pantry", null, v);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o < 2) db.execSQL("CREATE TABLE IF NOT EXISTS seen (id TEXT PRIMARY KEY, at INTEGER)");
        if (o < 3) {
            db.execSQL("ALTER TABLE pantry ADD COLUMN useBy TEXT");
            db.execSQL("ALTER TABLE pantry ADD COLUMN qty TEXT");
            createV3Tables(db);
        }
    }

    private static void createV3Tables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS ratings (id TEXT PRIMARY KEY, stars INTEGER, note TEXT, updated INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS plan (_id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, id TEXT, servings INTEGER, added INTEGER)");
    }

    // ---------------- pantry ----------------
    private void loadPantry() {
        pantry.clear();
        Cursor c = getReadableDatabase().rawQuery("SELECT key,label,staple,useBy,qty FROM pantry ORDER BY added", null);
        while (c.moveToNext()) {
            PantryItem p = new PantryItem(c.getString(0), c.getString(1), c.getInt(2) == 1);
            p.useBy = c.isNull(3) ? "" : c.getString(3);
            p.qty = c.isNull(4) ? "" : c.getString(4);
            pantry.put(p.key, p);
        }
        c.close();
    }

    /** Sets (or clears, with "") the use-by date and quantity of a pantry item. */
    void setPantryDetails(String key, String useBy, String qty) {
        PantryItem p = pantry.get(key);
        if (p == null) return;
        p.useBy = Dates.valid(useBy) ? useBy : "";
        p.qty = qty == null ? "" : qty.trim();
        ContentValues v = new ContentValues();
        v.put("useBy", p.useBy);
        v.put("qty", p.qty);
        final ContentValues fv = v;
        final String fk = key;
        write(new Runnable() { public void run() { getWritableDatabase().update("pantry", fv, "key=?", new String[]{fk}); } });
    }

    Map<String, String> useByMap() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (PantryItem p : pantry.values()) if (!p.useBy.isEmpty()) m.put(p.key, p.useBy);
        return m;
    }

    Map<String, String> qtyMap() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (PantryItem p : pantry.values()) if (!p.qty.isEmpty()) m.put(p.key, p.qty);
        return m;
    }

    /** Adds an ingredient; returns false if it was already there. */
    boolean addToPantry(String label, boolean staple) {
        String key = Normalizer.normalize(label);
        if (key.isEmpty()) return false;
        PantryItem existing = pantry.get(key);
        if (existing != null) {
            if (staple && !existing.staple) setStaple(key, true);
            return false;
        }
        String clean = Ui.cap(label.trim().replaceAll("\\s+", " "));
        pantry.put(key, new PantryItem(key, clean, staple));
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("label", clean);
        v.put("staple", staple ? 1 : 0);
        v.put("added", System.currentTimeMillis());
        final ContentValues fv = v;
        write(new Runnable() { public void run() { getWritableDatabase().insertWithOnConflict("pantry", null, fv, SQLiteDatabase.CONFLICT_REPLACE); } });
        if (!staple) addRecent(clean);
        return true;
    }

    void removeFromPantry(final String key) {
        pantry.remove(key);
        write(new Runnable() { public void run() { getWritableDatabase().delete("pantry", "key=?", new String[]{key}); } });
    }

    void setStaple(String key, boolean staple) {
        PantryItem p = pantry.get(key);
        if (p == null) return;
        p.staple = staple;
        ContentValues v = new ContentValues();
        v.put("staple", staple ? 1 : 0);
        final ContentValues fv = v;
        final String fk = key;
        write(new Runnable() { public void run() { getWritableDatabase().update("pantry", fv, "key=?", new String[]{fk}); } });
    }

    void clearPantry(boolean keepStaples) {
        flushIo();
        if (keepStaples) {
            getWritableDatabase().delete("pantry", "staple=0", null);
        } else {
            getWritableDatabase().delete("pantry", null, null);
        }
        loadPantry();
    }

    List<String> pantryKeys(boolean staples) {
        List<String> out = new ArrayList<String>();
        for (PantryItem p : pantry.values()) if (p.staple == staples) out.add(p.key);
        return out;
    }

    String pantrySignature() {
        StringBuilder b = new StringBuilder();
        for (PantryItem p : pantry.values()) if (!p.staple) b.append(p.key).append('|');
        return b.toString();
    }

    // ---------------- favorites ----------------
    private void loadFavorites() {
        Cursor c = getReadableDatabase().rawQuery("SELECT id FROM favorites", null);
        while (c.moveToNext()) favorites.add(c.getString(0));
        c.close();
    }

    boolean isFavorite(String id) { return favorites.contains(id); }

    boolean toggleFavorite(String id) {
        if (favorites.remove(id)) {
            getWritableDatabase().delete("favorites", "id=?", new String[]{id});
            return false;
        }
        favorites.add(id);
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("added", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("favorites", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        return true;
    }

    List<String> favoriteIdsNewestFirst() {
        List<String> out = new ArrayList<String>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id FROM favorites ORDER BY added DESC", null);
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    // ---------------- shopping list ----------------
    List<ShopItem> shopping() {
        List<ShopItem> out = new ArrayList<ShopItem>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,key,name,amount,unit,recipe,checked FROM shopping ORDER BY checked, added, _id", null);
        while (c.moveToNext()) {
            ShopItem s = new ShopItem();
            s.id = c.getLong(0);
            s.key = c.getString(1);
            s.name = c.getString(2);
            s.amount = c.isNull(3) ? Double.NaN : c.getDouble(3);
            s.unit = c.getString(4);
            s.recipe = c.getString(5);
            s.checked = c.getInt(6) == 1;
            out.add(s);
        }
        c.close();
        return out;
    }

    /** Adds or merges an item. Same ingredient + same unit amounts are summed. Returns true if new/changed. */
    boolean addShopping(String name, double amount, String unit, String recipe) {
        String key = Normalizer.normalize(name);
        if (key.isEmpty()) return false;
        if (unit == null) unit = "";
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT _id,amount,unit,recipe FROM shopping WHERE key=? AND checked=0", new String[]{key});
        try {
            while (c.moveToNext()) {
                double a = c.isNull(1) ? Double.NaN : c.getDouble(1);
                String u = c.getString(2);
                boolean bothNumeric = !Double.isNaN(a) && !Double.isNaN(amount);
                if (bothNumeric && unit.equalsIgnoreCase(u)) {
                    ContentValues v = new ContentValues();
                    v.put("amount", a + amount);
                    v.put("recipe", mergeRecipe(c.getString(3), recipe));
                    db.update("shopping", v, "_id=?", new String[]{String.valueOf(c.getLong(0))});
                    return true;
                }
                if (Double.isNaN(a) && Double.isNaN(amount)) return false; // already listed, no quantity
            }
        } finally {
            c.close();
        }
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("name", Ui.cap(name.trim()));
        if (Double.isNaN(amount)) v.putNull("amount"); else v.put("amount", amount);
        v.put("unit", unit);
        v.put("recipe", recipe == null ? "" : recipe);
        v.put("checked", 0);
        v.put("added", System.currentTimeMillis());
        db.insert("shopping", null, v);
        return true;
    }

    private static String mergeRecipe(String a, String b) {
        if (a == null || a.isEmpty()) return b == null ? "" : b;
        if (b == null || b.isEmpty() || a.contains(b)) return a;
        return a + ", " + b;
    }

    void setChecked(long id, boolean checked) {
        ContentValues v = new ContentValues();
        v.put("checked", checked ? 1 : 0);
        getWritableDatabase().update("shopping", v, "_id=?", new String[]{String.valueOf(id)});
    }

    void deleteShopping(long id) {
        getWritableDatabase().delete("shopping", "_id=?", new String[]{String.valueOf(id)});
    }

    /** v2.2: Select all / Select none. Returns rows changed. */
    int setAllChecked(boolean checked) {
        ContentValues v = new ContentValues();
        v.put("checked", checked ? 1 : 0);
        return getWritableDatabase().update("shopping", v, "checked=?", new String[]{checked ? "0" : "1"});
    }

    int clearChecked() {
        return getWritableDatabase().delete("shopping", "checked=1", null);
    }

    void clearShopping() {
        getWritableDatabase().delete("shopping", null, null);
    }

    int shoppingOpenCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM shopping WHERE checked=0", null);
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return n;
    }

    // ---------------- online recipe cache ----------------
    Map<String, String> loadCache() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,json FROM cache ORDER BY fetched DESC", null);
        while (c.moveToNext()) out.put(c.getString(0), c.getString(1));
        c.close();
        return out;
    }

    void putCache(String id, String json) {
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("json", json);
        v.put("fetched", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        db.insertWithOnConflict("cache", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        // keep the cache bounded, but never drop favorites, planned/rated recipes or the user's own recipes
        db.execSQL("DELETE FROM cache WHERE id NOT LIKE 'u:%' AND id NOT IN (SELECT id FROM favorites) "
                + "AND id NOT IN (SELECT id FROM plan) AND id NOT IN (SELECT id FROM ratings) AND id NOT IN "
                + "(SELECT id FROM cache ORDER BY fetched DESC LIMIT 400)");
    }

    // ---------------- settings ----------------
    int defaultServings() { return prefs.getInt("servings", 4); }

    void setDefaultServings(int s) { prefs.edit().putInt("servings", s).apply(); }

    // ---------------- discover: seen recipes ----------------
    private Set<String> seen;

    synchronized Set<String> seenIds() {
        if (seen == null) {
            seen = new HashSet<String>();
            Cursor c = getReadableDatabase().rawQuery("SELECT id FROM seen", null);
            while (c.moveToNext()) seen.add(c.getString(0));
            c.close();
        }
        return seen;
    }

    synchronized void markSeen(String id) {
        if (id == null || !seenIds().add(id)) return;
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("seen", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    synchronized void clearSeen() {
        seenIds().clear();
        getWritableDatabase().delete("seen", null, null);
    }

    // ---------------- recently used ingredients ----------------
    List<String> recents() {
        List<String> out = new ArrayList<String>();
        for (String s : prefs.getString("recent", "").split("\n")) if (!s.trim().isEmpty()) out.add(s);
        return out;
    }

    void addRecent(String label) {
        String clean = Ui.cap(label.trim().replaceAll("\\s+", " "));
        if (clean.isEmpty()) return;
        String key = Normalizer.normalize(clean);
        List<String> l = recents();
        for (int i = l.size() - 1; i >= 0; i--) if (Normalizer.normalize(l.get(i)).equals(key)) l.remove(i);
        l.add(0, clean);
        while (l.size() > 16) l.remove(l.size() - 1);
        prefs.edit().putString("recent", android.text.TextUtils.join("\n", l)).apply();
    }

    // ---------------- accepted substitutions ----------------
    Set<String> acceptedSubs() { return new HashSet<String>(prefs.getStringSet("acceptedSubs", new HashSet<String>())); }

    void setAcceptedSub(String key, boolean on) {
        Set<String> s = acceptedSubs();
        if (on) s.add(key); else s.remove(key);
        prefs.edit().putStringSet("acceptedSubs", s).apply();
    }

    // ---------------- quick-add sections ----------------
    Set<String> openSections() { return new HashSet<String>(prefs.getStringSet("openCats", new HashSet<String>(java.util.Arrays.asList("0")))); }

    void setSectionOpen(int idx, boolean open) {
        Set<String> s = openSections();
        if (open) s.add(String.valueOf(idx)); else s.remove(String.valueOf(idx));
        prefs.edit().putStringSet("openCats", s).apply();
    }

    // ---------------- ratings & notes ----------------
    private void loadRatings() {
        ratings.clear();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,stars,note FROM ratings ORDER BY updated", null);
        while (c.moveToNext()) {
            Backup.Rating r = new Backup.Rating();
            r.stars = c.getInt(1);
            r.note = c.isNull(2) ? "" : c.getString(2);
            ratings.put(c.getString(0), r);
        }
        c.close();
    }

    int stars(String id) { Backup.Rating r = ratings.get(id); return r == null ? 0 : r.stars; }

    String note(String id) { Backup.Rating r = ratings.get(id); return r == null ? "" : r.note; }

    void setRating(String id, int stars, String note) {
        stars = Math.max(0, Math.min(5, stars));
        note = note == null ? "" : note.trim();
        if (note.length() > Backup.MAX_NOTE) note = note.substring(0, Backup.MAX_NOTE);
        if (stars == 0 && note.isEmpty()) {
            ratings.remove(id);
            getWritableDatabase().delete("ratings", "id=?", new String[]{id});
            return;
        }
        Backup.Rating r = new Backup.Rating();
        r.stars = stars;
        r.note = note;
        ratings.put(id, r);
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("stars", stars);
        v.put("note", note);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("ratings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ---------------- meal plan ----------------
    static final class PlanRow {
        long rowId;
        MealPlan.Entry entry;
    }

    List<PlanRow> plan() {
        List<PlanRow> out = new ArrayList<PlanRow>();
        Cursor c = getReadableDatabase().rawQuery("SELECT _id,date,id,servings FROM plan ORDER BY date, added, _id", null);
        while (c.moveToNext()) {
            PlanRow r = new PlanRow();
            r.rowId = c.getLong(0);
            r.entry = new MealPlan.Entry(c.getString(1), c.getString(2), c.getInt(3));
            out.add(r);
        }
        c.close();
        return out;
    }

    List<MealPlan.Entry> planEntries() {
        List<MealPlan.Entry> out = new ArrayList<MealPlan.Entry>();
        for (PlanRow r : plan()) out.add(r.entry);
        return out;
    }

    void addPlan(String date, String id, int servings) {
        ContentValues v = new ContentValues();
        v.put("date", date);
        v.put("id", id);
        v.put("servings", Math.max(1, Math.min(MealPlan.MAX_SERVINGS, servings)));
        v.put("added", System.currentTimeMillis());
        getWritableDatabase().insert("plan", null, v);
    }

    void deletePlan(long rowId) { getWritableDatabase().delete("plan", "_id=?", new String[]{String.valueOf(rowId)}); }

    void clearPlanWeek(String weekStart) {
        getWritableDatabase().delete("plan", "date>=? AND date<?", new String[]{weekStart, Dates.addDays(weekStart, 7)});
    }

    /** Drops plan entries more than four weeks old. */
    void prunePlan() {
        getWritableDatabase().delete("plan", "date<?", new String[]{Dates.addDays(Dates.today(), -28)});
    }

    // ---------------- dislikes & allergies ----------------
    List<String> dislikes() {
        List<String> out = new ArrayList<String>();
        for (String s : prefs.getString("dislikes", "").split("\n")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    void setDislikes(List<String> keys) { prefs.edit().putString("dislikes", android.text.TextUtils.join("\n", keys)).apply(); }

    List<String> avoidAllergens() {
        Set<String> s = prefs.getStringSet("avoid", new HashSet<String>());
        List<String> out = new ArrayList<String>();
        for (String id : Allergens.ALL_IDS) if (s.contains(id)) out.add(id);
        return out;
    }

    void setAvoidAllergens(java.util.Collection<String> ids) { prefs.edit().putStringSet("avoid", new HashSet<String>(ids)).apply(); }

    // ---------------- backup ----------------
    org.pantryrecipes.app.core.Palette.State themeState() {
        return org.pantryrecipes.app.core.Palette.parseState(prefs.getString("theme", "{}"));
    }

    void saveTheme(org.pantryrecipes.app.core.Palette.State st) {
        prefs.edit().putString("theme", org.pantryrecipes.app.core.Json.write(st.toMap())).apply(); // v1.8: in-memory prefs update at once; disk write off the UI thread
    }

    Backup.Data exportBackup(String version, List<Recipe> extraRecipes) {
        Backup.Data d = new Backup.Data();
        d.appVersion = version;
        d.platform = "android";
        d.exported = Dates.today();
        for (PantryItem p : pantry.values()) {
            Backup.PantryEntry e = new Backup.PantryEntry();
            e.label = p.label; e.key = p.key; e.staple = p.staple; e.useBy = p.useBy; e.qty = p.qty;
            d.pantry.add(e);
        }
        List<String> favs = favoriteIdsNewestFirst();
        java.util.Collections.reverse(favs);   // oldest first, so re-importing keeps the order
        d.favorites.addAll(favs);
        for (ShopItem s : shopping()) {
            Backup.ShopEntry e = new Backup.ShopEntry();
            e.name = s.name; e.amount = s.amount; e.unit = s.unit == null ? "" : s.unit;
            e.recipe = s.recipe == null ? "" : s.recipe; e.checked = s.checked;
            d.shopping.add(e);
        }
        d.ratings.putAll(ratings);
        d.plan.addAll(planEntries());
        d.dislikes.addAll(dislikes());
        d.allergens.addAll(avoidAllergens());
        d.settings.put("servings", (long) defaultServings());
        d.settings.put("meals", new ArrayList<Object>(MealType.sorted(prefs.getStringSet("meals", new HashSet<String>()))));
        d.settings.put("incDesserts", prefs.getBoolean("incDesserts", false));
        d.settings.put("incSides", prefs.getBoolean("incSides", false));
        List<Object> diets = new ArrayList<Object>();
        Set<String> ds = prefs.getStringSet("diets", new HashSet<String>());
        for (String t : new String[]{Recipe.VEGETARIAN, Recipe.VEGAN, Recipe.GLUTEN_FREE, Recipe.DAIRY_FREE}) if (ds.contains(t)) diets.add(t);
        d.settings.put("diets", diets);
        d.settings.put("maxTime", (long) prefs.getInt("maxTime", 0));
        d.settings.put("mode", (long) prefs.getInt("mode", 0));
        d.settings.put("online", prefs.getBoolean("online", true));
        List<String> acc = new ArrayList<String>(acceptedSubs());
        java.util.Collections.sort(acc);
        d.settings.put("acceptedSubs", new ArrayList<Object>(acc));
        d.recipes.addAll(extraRecipes);
        d.theme = themeState();
        return d;
    }

    /** Replaces everything with the backup's contents (recipes in it are added to the local cache). */
    void importBackup(Backup.Data d) {
        flushIo();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("pantry", null, null);
            long now = System.currentTimeMillis();
            for (Backup.PantryEntry e : d.pantry) {
                ContentValues v = new ContentValues();
                v.put("key", Normalizer.normalize(e.label));
                v.put("label", Ui.cap(e.label));
                v.put("staple", e.staple ? 1 : 0);
                v.put("added", now++);
                v.put("useBy", e.useBy);
                v.put("qty", e.qty);
                db.insertWithOnConflict("pantry", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.delete("favorites", null, null);
            for (String id : d.favorites) {
                ContentValues v = new ContentValues();
                v.put("id", id);
                v.put("added", now++);
                db.insertWithOnConflict("favorites", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.delete("shopping", null, null);
            for (Backup.ShopEntry e : d.shopping) {
                ContentValues v = new ContentValues();
                v.put("key", Normalizer.normalize(e.name));
                v.put("name", e.name);
                if (Double.isNaN(e.amount)) v.putNull("amount"); else v.put("amount", e.amount);
                v.put("unit", e.unit);
                v.put("recipe", e.recipe);
                v.put("checked", e.checked ? 1 : 0);
                v.put("added", now++);
                db.insert("shopping", null, v);
            }
            db.delete("ratings", null, null);
            for (Map.Entry<String, Backup.Rating> e : d.ratings.entrySet()) {
                ContentValues v = new ContentValues();
                v.put("id", e.getKey());
                v.put("stars", e.getValue().stars);
                v.put("note", e.getValue().note);
                v.put("updated", now++);
                db.insert("ratings", null, v);
            }
            db.delete("plan", null, null);
            for (MealPlan.Entry e : d.plan) {
                ContentValues v = new ContentValues();
                v.put("date", e.date);
                v.put("id", e.id);
                v.put("servings", e.servings);
                v.put("added", now++);
                db.insert("plan", null, v);
            }
            for (Recipe r : d.recipes) {
                ContentValues v = new ContentValues();
                v.put("id", r.id);
                v.put("json", org.pantryrecipes.app.core.Json.write(r.toMap()));
                v.put("fetched", now++);
                db.insertWithOnConflict("cache", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("dislikes", android.text.TextUtils.join("\n", d.dislikes));
        ed.putStringSet("avoid", new HashSet<String>(d.allergens));
        if (d.theme != null) ed.putString("theme", org.pantryrecipes.app.core.Json.write(d.theme.toMap()));
        Map<String, Object> st = d.settings;
        if (st.get("servings") instanceof Number) ed.putInt("servings", ((Number) st.get("servings")).intValue());
        if (st.get("meals") instanceof List) ed.putStringSet("meals", strSet(st.get("meals")));
        if (st.get("incDesserts") instanceof Boolean) ed.putBoolean("incDesserts", (Boolean) st.get("incDesserts"));
        if (st.get("incSides") instanceof Boolean) ed.putBoolean("incSides", (Boolean) st.get("incSides"));
        if (st.get("diets") instanceof List) ed.putStringSet("diets", strSet(st.get("diets")));
        if (st.get("maxTime") instanceof Number) ed.putInt("maxTime", ((Number) st.get("maxTime")).intValue());
        if (st.get("mode") instanceof Number) ed.putInt("mode", ((Number) st.get("mode")).intValue());
        if (st.get("online") instanceof Boolean) ed.putBoolean("online", (Boolean) st.get("online"));
        if (st.get("acceptedSubs") instanceof List) ed.putStringSet("acceptedSubs", strSet(st.get("acceptedSubs")));
        ed.apply();
        favorites.clear();
        loadFavorites();
        loadPantry();
        loadRatings();
    }

    private static Set<String> strSet(Object o) {
        Set<String> s = new HashSet<String>();
        for (Object x : (List<?>) o) s.add(String.valueOf(x));
        return s;
    }
}
