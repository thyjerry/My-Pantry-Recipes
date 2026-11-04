package org.pantryrecipes.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import org.pantryrecipes.app.core.Json;
import org.pantryrecipes.app.core.Recipe;
import org.pantryrecipes.app.core.Substitutions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** All recipes: the bundled set plus TheMealDB recipes cached in SQLite. */
final class Repo {
    interface Listener { void onRepoChanged(); }

    private static Repo instance;
    private final Context app;
    private final Store store;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Recipe> bundled = new ArrayList<Recipe>();
    private final LinkedHashMap<String, Recipe> online = new LinkedHashMap<String, Recipe>();
    /** Recipes the user brought in (u: ids, from a web-app backup); always shown, never evicted. */
    private final LinkedHashMap<String, Recipe> mine = new LinkedHashMap<String, Recipe>();
    private final List<Listener> listeners = new ArrayList<Listener>();
    volatile boolean loaded;
    private volatile Substitutions subs;
    volatile boolean browsing;
    private final java.util.Set<String> namesBusy = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
    volatile boolean fetching;
    String lastError = "";
    String lastFetchSignature = "";
    int lastFetchAdded = -1;

    static synchronized Repo get(Context c) {
        if (instance == null) instance = new Repo(c.getApplicationContext());
        return instance;
    }

    private Repo(Context c) {
        app = c;
        store = Store.get(c);
        lastFetchSignature = store.prefs.getString("fetchSig", "");
        worker.execute(new Runnable() {
            @Override
            public void run() { loadLocal(); }
        });
    }

    void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }

    void removeListener(Listener l) { listeners.remove(l); }

    private void notifyChanged() {
        main.post(new Runnable() {
            @Override
            public void run() {
                for (Listener l : new ArrayList<Listener>(listeners)) l.onRepoChanged();
            }
        });
    }

    private static String readAsset(android.content.res.AssetManager am, String name) throws java.io.IOException {
        InputStream in = am.open(name);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    /** v2.1: true once every bundled chunk is in (loaded turns true after the first chunk). */
    volatile boolean complete;

    private void loadLocal() {
        // v2.1: recipes ship as small chunks (recipes-0.json, recipes-1.json, ...). The first one is published
        // straight away so the screens fill quickly on slow tablets; the rest are appended in the background.
        android.content.res.AssetManager am = app.getAssets();
        List<Recipe> b = new ArrayList<Recipe>();
        boolean chunked = true;
        try {
            b.addAll(Recipe.parseList(readAsset(am, "recipes-0.json")));
        } catch (java.io.FileNotFoundException e) {
            chunked = false;
            try {
                b.addAll(Recipe.parseList(readAsset(am, "recipes.json")));
            } catch (Exception e2) {
                lastError = "Could not read bundled recipes";
            }
        } catch (Exception e) {
            lastError = "Could not read bundled recipes";
        }
        LinkedHashMap<String, Recipe> on = new LinkedHashMap<String, Recipe>();
        LinkedHashMap<String, Recipe> my = new LinkedHashMap<String, Recipe>();
        for (Map.Entry<String, String> e : store.loadCache().entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) Json.parse(e.getValue());
                (e.getKey().startsWith("u:") ? my : on).put(e.getKey(), Recipe.fromMap(m));
            } catch (Exception ignored) { }
        }
        Substitutions sb;
        try {
            sb = Substitutions.parse(readAsset(am, "substitutions.txt"));
        } catch (Exception e) {
            sb = Substitutions.parse("");
        }
        subs = sb;
        synchronized (this) {
            bundled.clear();
            bundled.addAll(b);
            online.clear();
            online.putAll(on);
            mine.clear();
            mine.putAll(my);
        }
        publishBundled(b);
        if (chunked) {
            loaded = true;
            notifyChanged();
            List<Recipe> rest = new ArrayList<Recipe>();
            for (int i = 1; ; i++) {
                try {
                    rest.addAll(Recipe.parseList(readAsset(am, "recipes-" + i + ".json")));
                } catch (java.io.FileNotFoundException e) {
                    break;
                } catch (Exception e) {
                    lastError = "Could not read bundled recipes";
                    break;
                }
            }
            if (!rest.isEmpty()) {
                synchronized (this) { b = new ArrayList<Recipe>(bundled); }
                b.addAll(rest);
                synchronized (this) {
                    bundled.clear();
                    bundled.addAll(b);
                }
                publishBundled(b);
            }
        }
        loaded = true;
        complete = true;
        notifyChanged();
        prewarmMatches();
    }

    /** Index, autocomplete names and title hints for the bundled list {@code b} (worker thread). */
    private void publishBundled(List<Recipe> b) {
        HashMap<String, Recipe> byId = new HashMap<String, Recipe>(b.size() * 2);
        for (Recipe r : b) if (!byId.containsKey(r.id)) byId.put(r.id, r);
        synchronized (this) {
            bundledById.clear();
            bundledById.putAll(byId);
        }
        rebuildNames();
        // v1.8: near-duplicate titles get a short hint; online copies of bundled titles are hidden
        Map<String, String> th = org.pantryrecipes.app.core.Titles.hints(b);
        java.util.HashSet<String> tk = new java.util.HashSet<String>();
        for (Recipe r : b) tk.add(org.pantryrecipes.app.core.Titles.key(r.title));
        synchronized (this) { titleHints = th; bundledTitleKeys = tk; }
    }

    // ---------------- v1.8: work done once on the worker, not on the UI thread ----------------
    private final HashMap<String, Recipe> bundledById = new HashMap<String, Recipe>();
    private volatile List<String> names;
    private volatile java.util.Set<String> nameKeys;

    /** Autocomplete names (catalog, recipe ingredients, aliases), or null while loading. Pantry labels are added by the caller. */
    List<String> cachedNames() { return names; }

    java.util.Set<String> cachedNameKeys() { return nameKeys; }

    private void rebuildNames() {
        List<Recipe> src;
        synchronized (this) {
            src = new ArrayList<Recipe>(bundled);
            src.addAll(online.values());
        }
        java.util.Set<String> keys = new java.util.HashSet<String>();
        List<String> n = org.pantryrecipes.app.core.PantryIndex.ingredientNames(src, null, keys);
        nameKeys = keys;
        names = java.util.Collections.unmodifiableList(n);
    }

    /** Takes a pantry snapshot on the UI thread, then runs the first full match on the worker so the Recipes tab opens warm. */
    private void prewarmMatches() {
        main.post(new Runnable() {
            public void run() {
                final List<Recipe> all = all();
                final List<String> p = store.pantryKeys(false), st = store.pantryKeys(true);
                final java.util.Set<String> acc = store.acceptedSubs();
                final Substitutions sb = subs;
                worker.execute(new Runnable() {
                    public void run() { org.pantryrecipes.app.core.MatchCache.shared().match(all, p, st, sb, acc); }
                });
            }
        });
    }

    private Map<String, String> titleHints = new HashMap<String, String>();
    private java.util.Set<String> bundledTitleKeys = new java.util.HashSet<String>();

    /** "with lemon" / "Thai" / "45 min" for recipes whose title is nearly the same as another's, else null. */
    synchronized String titleHint(String id) { return titleHints.get(id); }

    /** An online recipe with the same title as a bundled one (after folding) is shown once, as the bundled one. */
    synchronized boolean isDuplicateOnline(Recipe r) {
        return r.id.startsWith("m:") && bundledTitleKeys.contains(org.pantryrecipes.app.core.Titles.key(r.title));
    }

    synchronized List<Recipe> all() {
        List<Recipe> out = new ArrayList<Recipe>(bundled.size() + online.size());
        out.addAll(bundled);
        out.addAll(mine.values());
        if (onlineEnabled()) for (Recipe r : online.values()) if (!isDuplicateOnline(r)) out.add(r);
        return out;
    }

    /** Re-reads cached and user recipes after a backup import. */
    void reload() {
        new Thread(new Runnable() {
            public void run() { loadLocal(); }
        }).start();
    }

    /** Online and user recipes a backup should carry: all user recipes plus online ones that are referenced. */
    synchronized List<Recipe> recipesForBackup(java.util.Set<String> referenced) {
        List<Recipe> out = new ArrayList<Recipe>(mine.values());
        for (Recipe r : online.values()) if (referenced.contains(r.id)) out.add(r);
        return out;
    }

    synchronized int onlineCount() { return online.size(); }

    synchronized Recipe byId(String id) {
        if (id == null) return null;
        Recipe r = online.get(id);
        if (r != null) return r;
        r = mine.get(id);
        if (r != null) return r;
        return bundledById.get(id);
    }

    /** All ingredient display names known to the app (for autocomplete). */
    List<String> ingredientNames() {
        List<String> n = names;
        java.util.Set<String> k = nameKeys;
        if (n == null || k == null) { // still loading: catalog names only (MainActivity swaps the adapter when loaded)
            k = new java.util.HashSet<String>();
            n = org.pantryrecipes.app.core.PantryIndex.ingredientNames(new ArrayList<Recipe>(), null, k);
        }
        List<String> out = new ArrayList<String>(n);
        boolean added = false;
        for (Store.PantryItem p : store.pantry.values()) if (!k.contains(p.key)) { out.add(p.label); added = true; }
        if (added) Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    boolean onlineEnabled() { return store.prefs.getBoolean("online", true); }

    String apiKey() { return store.prefs.getString("apiKey", "1"); }

    boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /** Whether the pantry changed since the last successful online fetch. */
    boolean needsFetch() {
        return onlineEnabled() && !store.pantrySignature().isEmpty()
                && !store.pantrySignature().equals(lastFetchSignature);
    }

    private void cache(Recipe r) {
        synchronized (this) {
            online.put(r.id, r);
        }
        store.putCache(r.id, Json.write(r.toMap()));
    }

    /**
     * Looks up TheMealDB meals that use the pantry's main ingredients (one filter request per
     * ingredient, max 5), ranks them by how many pantry ingredients they contain and caches the top ones.
     */
    void fetchForPantry() {
        if (fetching || !onlineEnabled()) return;
        if (!isOnline()) {
            lastError = "offline";
            notifyChanged();
            return;
        }
        final List<String> labels = new ArrayList<String>();
        final List<String> keys = new ArrayList<String>();
        List<Store.PantryItem> items = new ArrayList<Store.PantryItem>(store.pantry.values());
        Collections.reverse(items); // newest first
        for (Store.PantryItem p : items) {
            if (p.staple) continue;
            labels.add(p.label);
            keys.add(p.key);
            if (labels.size() == 5) break;
        }
        if (labels.isEmpty()) return;
        final String sig = store.pantrySignature();
        final String key = apiKey();
        fetching = true;
        lastError = "";
        notifyChanged();
        worker.execute(new Runnable() {
            @Override
            public void run() {
                int added = 0;
                boolean anyOk = false;
                try {
                    final Map<String, Integer> score = new HashMap<String, Integer>();
                    final Map<String, Integer> order = new HashMap<String, Integer>();
                    for (int i = 0; i < labels.size(); i++) {
                        List<String> ids;
                        try {
                            ids = MealDb.idsForIngredient(key, labels.get(i));
                            if (ids.isEmpty() && !keys.get(i).equalsIgnoreCase(labels.get(i)))
                                ids = MealDb.idsForIngredient(key, keys.get(i));
                            anyOk = true;
                        } catch (Exception e) {
                            continue;
                        }
                        for (String id : ids) {
                            Integer s = score.get(id);
                            score.put(id, s == null ? 1 : s + 1);
                            if (!order.containsKey(id)) order.put(id, order.size());
                        }
                    }
                    List<String> ranked = new ArrayList<String>(score.keySet());
                    Collections.sort(ranked, new Comparator<String>() {
                        @Override
                        public int compare(String a, String b) {
                            int c = score.get(b) - score.get(a);
                            return c != 0 ? c : order.get(a) - order.get(b);
                        }
                    });
                    int looked = 0;
                    for (String id : ranked) {
                        if (looked >= 24) break;
                        synchronized (Repo.this) {
                            if (online.containsKey("m:" + id)) continue;
                        }
                        looked++;
                        try {
                            Recipe r = MealDb.lookup(key, id);
                            if (r != null && !r.ingredients.isEmpty()) {
                                cache(r);
                                added++;
                                if (added % 6 == 0) notifyChanged();
                            }
                        } catch (Exception ignored) { }
                    }
                    if (anyOk) {
                        lastFetchSignature = sig;
                        store.prefs.edit().putString("fetchSig", sig).apply();
                    } else {
                        lastError = "TheMealDB could not be reached";
                    }
                } finally {
                    if (added > 0) rebuildNames();
                    lastFetchAdded = added;
                    fetching = false;
                    notifyChanged();
                }
            }
        });
    }

    interface SearchCallback { void done(List<Recipe> results, String error); }

    void searchOnline(final String query, final SearchCallback cb) {
        if (!isOnline()) {
            cb.done(new ArrayList<Recipe>(), "You're offline");
            return;
        }
        final String key = apiKey();
        worker.execute(new Runnable() {
            @Override
            public void run() {
                List<Recipe> res = new ArrayList<Recipe>();
                String err = null;
                try {
                    res = MealDb.search(key, query);
                    for (Recipe r : res) cache(r);
                } catch (Exception e) {
                    err = "TheMealDB could not be reached";
                }
                final List<Recipe> fr = res;
                final String fe = err;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.done(fr, fe);
                        for (Listener l : new ArrayList<Listener>(listeners)) l.onRepoChanged();
                    }
                });
            }
        });
    }

    // ===================== substitutions & discover =====================
    Substitutions substitutions() {
        Substitutions s = subs;
        return s != null ? s : Substitutions.parse("");
    }

    /** Cuisines (areas) or categories from TheMealDB, cached in prefs; refreshed weekly when online. */
    List<String> mealDbNames(final boolean areas) {
        final String pk = areas ? "mdbAreas" : "mdbCats";
        List<String> out = new ArrayList<String>();
        for (String s : store.prefs.getString(pk, "").split("\n")) if (!s.trim().isEmpty()) out.add(s.trim());
        long age = System.currentTimeMillis() - store.prefs.getLong(pk + "At", 0);
        if (onlineEnabled() && isOnline() && (out.isEmpty() || age > 7L * 24 * 3600 * 1000) && !namesBusy.contains(pk)) {
            namesBusy.add(pk);
            final String key = apiKey();
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<String> names = MealDb.listNames(key, areas);
                        if (!names.isEmpty()) {
                            store.prefs.edit().putString(pk, android.text.TextUtils.join("\n", names))
                                    .putLong(pk + "At", System.currentTimeMillis()).apply();
                            notifyChanged();
                        }
                    } catch (Exception ignored) {
                    } finally {
                        namesBusy.remove(pk);
                    }
                }
            });
        }
        return out;
    }

    /** Cached TheMealDB recipes known to belong to a cuisine ("a") or category ("c") from earlier browsing. */
    synchronized List<Recipe> cachedBrowse(char kind, String value) {
        List<Recipe> out = new ArrayList<Recipe>();
        if (!onlineEnabled()) return out;
        for (String id : store.prefs.getString("browse_" + kind + "_" + value.toLowerCase(java.util.Locale.US), "").split(",")) {
            Recipe r = online.get("m:" + id);
            if (r != null) out.add(r);
        }
        return out;
    }

    interface BrowseCallback { void done(int added, int remaining, String error); }

    /** Fetches up to {@code max} not-yet-cached meals for a cuisine/category and caches them. */
    void browseOnline(final char kind, final String value, final int max, final BrowseCallback cb) {
        if (!onlineEnabled()) { cb.done(0, 0, "Online recipes are turned off"); return; }
        if (!isOnline()) { cb.done(0, 0, "You're offline \u2013 showing saved recipes"); return; }
        if (browsing) { cb.done(0, 0, null); return; }
        browsing = true;
        final String key = apiKey();
        final String pk = "browse_" + kind + "_" + value.toLowerCase(java.util.Locale.US);
        worker.execute(new Runnable() {
            @Override
            public void run() {
                int added = 0, remaining = 0;
                String err = null;
                try {
                    List<String> ids = MealDb.filterIds(key, kind, value);
                    store.prefs.edit().putString(pk, android.text.TextUtils.join(",", ids)).apply();
                    for (String id : ids) {
                        boolean have;
                        synchronized (Repo.this) { have = online.containsKey("m:" + id); }
                        if (have) continue;
                        if (added >= max) { remaining++; continue; }
                        try {
                            Recipe r = MealDb.lookup(key, id);
                            if (r != null && !r.ingredients.isEmpty()) { cache(r); added++; }
                        } catch (Exception ignored) { }
                    }
                } catch (Exception e) {
                    err = "TheMealDB could not be reached";
                }
                final int fa = added, fr = remaining;
                final String fe = err;
                browsing = false;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.done(fa, fr, fe);
                        for (Listener l : new ArrayList<Listener>(listeners)) l.onRepoChanged();
                    }
                });
            }
        });
    }

    interface RandomCallback { void done(Recipe r, String error); }

    void randomOnline(final RandomCallback cb) {
        if (!onlineEnabled() || !isOnline()) { cb.done(null, "offline"); return; }
        final String key = apiKey();
        worker.execute(new Runnable() {
            @Override
            public void run() {
                Recipe r = null;
                String err = null;
                try {
                    r = MealDb.random(key);
                    if (r != null && !r.ingredients.isEmpty()) cache(r); else r = null;
                } catch (Exception e) {
                    err = "TheMealDB could not be reached";
                }
                final Recipe fr = r;
                final String fe = err;
                main.post(new Runnable() {
                    @Override
                    public void run() { cb.done(fr, fe); }
                });
            }
        });
    }
}
