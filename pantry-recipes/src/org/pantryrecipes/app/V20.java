package org.pantryrecipes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.pantryrecipes.app.core.Allergens;
import org.pantryrecipes.app.core.Backup;
import org.pantryrecipes.app.core.Cost;
import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.Diets;
import org.pantryrecipes.app.core.Household;
import org.pantryrecipes.app.core.Ingredient;
import org.pantryrecipes.app.core.Inventory;
import org.pantryrecipes.app.core.Json;
import org.pantryrecipes.app.core.Normalizer;
import org.pantryrecipes.app.core.Recipe;
import org.pantryrecipes.app.core.RecipeEditor;
import org.pantryrecipes.app.core.Region;
import org.pantryrecipes.app.core.RegionalPrices;
import org.pantryrecipes.app.core.Units;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * v2.0 Stage 1 on Android: region & formats, localized names, all 15 diets, household allergen alerts,
 * kitchen inventory (locations, freezer use-or-toss, restock, spice rack), regional prices and budget mode,
 * recipe editor / packs / cookbook, and the backup fields for all of it. Plain dialogs (no extra activity)
 * so nothing is built until it is opened - the Fire HD 8 pays nothing for features it does not use.
 */
final class V20 {
    private V20() { }

    static final String[] CORE_DIETS = {Recipe.VEGETARIAN, Recipe.VEGAN, Recipe.GLUTEN_FREE, Recipe.DAIRY_FREE};
    static final List<String> ALL_DIETS;
    static {
        List<String> a = new ArrayList<String>();
        for (String d : CORE_DIETS) a.add(d);
        a.addAll(Diets.IDS);
        ALL_DIETS = java.util.Collections.unmodifiableList(a);
    }

    static SharedPreferences prefs(Context c) { return Store.get(c).prefs; }
    static String region(Context c) { return Region.valid(prefs(c).getString("region", "")); }
    static boolean welcomed(Context c) { return prefs(c).getBoolean("welcomed", false) || !region(c).isEmpty(); }

    static int dp(Context c, float v) { return Ui.dp(c, v); }

    // ---------------- region ----------------
    static void applyRegion(Context c, String r) {
        Region.Defaults d = Region.defaults(r);
        prefs(c).edit().putString("region", r).putBoolean("welcomed", true)
                .putString("units", d.units).putString("weightUnit", d.weightUnit).putBoolean("showCups", d.showCups)
                .putString("currency", d.currency).putString("dateFormat", d.dateFormat)
                .putBoolean("decimalComma", d.decimalComma).putBoolean("clock24", d.clock24)
                .putString("weekStart", d.weekStartsSunday ? "sun" : "mon")
                .putBoolean("gasMarks", d.gasMarks).putBoolean("fanNote", d.fanNote).putString("detailUnits", "").commit();
        V17.importExtras(c, new Backup.Data()); // drops cached unit prefs
        priceMemo = null;
    }

    static String loc(Context c, String text, boolean steps) {
        String r = region(c);
        return r.isEmpty() || Region.US.equals(r) ? text : Region.localize(text, r, steps);
    }

    static String date(Context c, String iso) { return Region.formatDate(iso, prefs(c).getString("dateFormat", "YYYY-MM-DD")); }
    static boolean weekSunday(Context c) { return "sun".equals(prefs(c).getString("weekStart", "mon")); }

    static String money(Context c, double v) { return Region.money(V16.currency(c), v, prefs(c).getBoolean("decimalComma", false)); }

    // ---------------- diets ----------------
    private static final WeakHashMap<Recipe, Set<String>> DIET_MEMO = new WeakHashMap<Recipe, Set<String>>();

    static synchronized Set<String> dietsOf(Context c, Recipe r) {
        Set<String> d = DIET_MEMO.get(r);
        if (d == null) {
            d = new LinkedHashSet<String>();
            for (String x : CORE_DIETS) if (r.tags.contains(x)) d.add(x);
            d.addAll(r.dietsKnown || !r.diets.isEmpty() ? r.diets : Diets.classify(r, V16.foods(c)));
            DIET_MEMO.put(r, d);
        }
        return d;
    }

    static boolean dietsPass(Context c, Recipe r, Set<String> sel) {
        if (sel.isEmpty()) return true;
        Set<String> d = dietsOf(c, r);
        for (String x : sel) {
            if (x.startsWith(V30.FAST_PREFIX)) { if (!V30.fitsFast(c, x.substring(V30.FAST_PREFIX.length()), r)) return false; }
            else if (!d.contains(x)) return false;
        }
        return true;
    }

    static String dietName(String id) { return Diets.IDS.contains(id) ? Diets.name(id) : Ui.dietLabel(id); }

    // ---------------- household ----------------
    static List<Household.Person> household(Context c) {
        try { return Household.parse(Json.parse(prefs(c).getString("household", "[]"))); } catch (RuntimeException e) { return new ArrayList<Household.Person>(); }
    }

    static void saveHousehold(Context c, List<Household.Person> ps) { prefs(c).edit().putString("household", Json.write(Household.toList(ps))).apply(); }

    /** Own allergens plus those of household members marked "hide". */
    static List<String> avoid(Context c) {
        return new ArrayList<String>(Household.avoid(Store.get(c).avoidAllergens(), household(c)));
    }

    static List<String> regionAllergens(Context c) { String r = region(c); return Region.allergens(r.isEmpty() ? Region.US : r); }

    // ---------------- inventory (per-item fields kept in prefs "inv20": key -> {loc, frozen, low}) ----------------
    @SuppressWarnings("unchecked")
    static Map<String, Object> invMap(Context c) {
        try {
            Object o = Json.parse(prefs(c).getString("inv20", "{}"));
            if (o instanceof Map) return (Map<String, Object>) o;
        } catch (RuntimeException ignored) { }
        return new java.util.LinkedHashMap<String, Object>();
    }

    static void saveInv(Context c, Map<String, Object> m) { prefs(c).edit().putString("inv20", Json.write(m)).apply(); }

    @SuppressWarnings("unchecked")
    static List<Inventory.Item> items(Context c) {
        Store s = Store.get(c);
        Map<String, Object> m = invMap(c);
        List<Inventory.Item> out = new ArrayList<Inventory.Item>();
        for (Store.PantryItem p : new ArrayList<Store.PantryItem>(s.pantry.values())) {
            Inventory.Item it = new Inventory.Item(p.key, p.label);
            it.staple = p.staple; it.useBy = p.useBy; it.qty = p.qty;
            Object o = m.get(p.key);
            if (o instanceof Map) {
                Map<String, Object> e = (Map<String, Object>) o;
                it.loc = Inventory.validLocation(Json.str(e, "loc", ""));
                it.frozen = Json.str(e, "frozen", "");
                it.low = Boolean.TRUE.equals(e.get("low"));
            }
            out.add(it);
        }
        return out;
    }

    static void setInv(Context c, String key, String loc, String frozen, boolean low) {
        Map<String, Object> m = invMap(c);
        Map<String, Object> e = new java.util.LinkedHashMap<String, Object>();
        if (!loc.isEmpty()) e.put("loc", loc);
        if (!frozen.isEmpty()) e.put("frozen", frozen);
        if (low) e.put("low", true);
        if (e.isEmpty()) m.remove(key); else m.put(key, e);
        saveInv(c, m);
    }

    // ---------------- prices ----------------
    private static RegionalPrices.Table bls;
    private static Object[] priceMemo;   // {region, defaults, sources}

    static synchronized RegionalPrices.Table bls(Context c) {
        if (bls == null) bls = RegionalPrices.parse(V16.asset(c, "bls-prices.tsv"));
        return bls;
    }

    static String zip(Context c) { return RegionalPrices.cleanZip(prefs(c).getString("zip", "")); }
    static String priceRegion(Context c) { return RegionalPrices.region(zip(c)); }

    /** US (or region not set): BLS regional averages + built-in estimates x regional factor. Elsewhere: none (user prices only). */
    @SuppressWarnings("unchecked")
    static synchronized Map<String, Cost.Price> priceDefaults(Context c) {
        String r = region(c);
        if (!r.isEmpty() && !Region.US.equals(r)) return null;
        String pr = priceRegion(c);
        if (priceMemo == null || !pr.equals(priceMemo[0])) {
            Map<String, RegionalPrices.Source> src = new java.util.HashMap<String, RegionalPrices.Source>();
            priceMemo = new Object[]{pr, RegionalPrices.defaults(bls(c), pr, V16.defaultPrices(c), src), src};
        }
        return (Map<String, Cost.Price>) priceMemo[1];
    }

    @SuppressWarnings("unchecked")
    static synchronized Map<String, RegionalPrices.Source> priceSources(Context c) {
        if (priceDefaults(c) == null) return new java.util.HashMap<String, RegionalPrices.Source>();
        return (Map<String, RegionalPrices.Source>) priceMemo[2];
    }

    static double budget(Context c) { try { return Double.parseDouble(prefs(c).getString("budget", "0")); } catch (Exception e) { return 0; } }
    static boolean budgetOn(Context c) { return prefs(c).getBoolean("budgetMode", false) && budget(c) > 0; }

    private static final WeakHashMap<Recipe, Object[]> BUDGET_MEMO = new WeakHashMap<Recipe, Object[]>();
    static int priceVersion;

    /**
     * Budget mode: keep recipes whose estimate per serving fits (unknown costs are kept). Memoised per price version.
     * v2.0 Stage 2: the first pass never runs on the UI thread. On the UI thread an unknown recipe is kept for now and a
     * low-priority worker fills the memo, then refreshes the list once (the first pass took ~420 ms interpreted on the bench box, several times that on a Fire HD 8).
     */
    static boolean budgetPass(Context c, Recipe r) {
        if (!budgetOn(c)) return true;
        int ver;
        synchronized (V20.class) { ver = priceVersion; }
        Object[] m;
        synchronized (BUDGET_MEMO) { m = BUDGET_MEMO.get(r); }
        if (m != null && (Integer) m[0] == ver) return (Boolean) m[1];
        if (Looper.myLooper() == Looper.getMainLooper() && warmedVersion != ver) {
            if (c instanceof MainActivity) uiRef = new java.lang.ref.WeakReference<MainActivity>((MainActivity) c);
            warmBudget(c.getApplicationContext(), ver);
            return true;
        }
        return budgetCompute(c, r, ver);
    }

    /** v2.0 S3: true when budget mode is on and the memo says this recipe has no estimate (kept, shown as "price unknown"). Never computes. */
    static boolean priceUnknown(Context c, Recipe r) {
        if (!budgetOn(c)) return false;
        Object[] m;
        synchronized (BUDGET_MEMO) { m = BUDGET_MEMO.get(r); }
        return m != null && m.length > 2 && (Integer) m[0] == priceVersion && (Boolean) m[2];
    }

    static java.lang.ref.WeakReference<MainActivity> uiRef;
    private static int warmingVersion = -1;
    static volatile long lastWarmMs = -1;
    private static volatile int warmedVersion = -1;

    /** Recipes were reloaded (new objects): start a fresh budget pass. */
    static void repoChanged() { synchronized (V20.class) { priceVersion++; } }

    private static boolean budgetCompute(Context c, Recipe r, int ver) {
        Cost.Estimate e = Cost.estimate(r, r.servings > 0 ? r.servings : 4, V16.data(c).prices, V16.priceDefaults(c), V16.foods(c));
        boolean ok = RegionalPrices.withinBudget(e, budget(c));
        boolean unknown = e == null || !e.show();
        synchronized (BUDGET_MEMO) { BUDGET_MEMO.put(r, new Object[]{ver, ok, unknown}); }
        return ok;
    }

    /** Fills the budget memo for every recipe on a background thread, then refreshes the visible list once. */
    static void warmBudget(final Context app, final int ver) {
        synchronized (V20.class) { if (warmingVersion == ver) return; warmingVersion = ver; }
        Thread t = new Thread(() -> {
            long t0 = System.nanoTime();
            for (Recipe r : Repo.get(app).all()) {
                synchronized (V20.class) { if (priceVersion != ver) return; }
                Object[] m;
                synchronized (BUDGET_MEMO) { m = BUDGET_MEMO.get(r); }
                if (m == null || (Integer) m[0] != ver) budgetCompute(app, r, ver);
            }
            lastWarmMs = (System.nanoTime() - t0) / 1000000L;
            warmedVersion = ver;
            new android.os.Handler(Looper.getMainLooper()).post(() -> {
                MainActivity a = uiRef == null ? null : uiRef.get();
                if (a != null && !a.isFinishing()) a.refreshResults();
            });
        }, "budget-warm");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    // ---------------- backup ----------------
    static final String[] STR_KEYS = {"region", "zip", "budget", "dateFormat", "weekStart"};
    static final String[] BOOL_KEYS = {"budgetMode", "expiryReminders", "gasMarks", "fanNote", "decimalComma", "clock24", "cuisineGroups"};

    @SuppressWarnings("unchecked")
    static void exportExtras(Context c, Backup.Data d) {
        SharedPreferences p = prefs(c);
        for (String k : STR_KEYS) if (p.contains(k)) d.settings.put(k, p.getString(k, ""));
        for (String k : BOOL_KEYS) if (p.contains(k)) d.settings.put(k, p.getBoolean(k, false));
        V30.backupOut(p, d.settings);
        V201.backupOut(p, d.settings);
        V21.backupOut(c, d.settings);
        d.settings.put("household", Household.toList(household(c)));
        Map<String, Object> inv = invMap(c);
        for (Backup.PantryEntry e : d.pantry) {
            Object o = inv.get(e.key);
            if (o instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) o;
                e.loc = Inventory.validLocation(Json.str(m, "loc", ""));
                e.frozen = Json.str(m, "frozen", "");
                e.low = Boolean.TRUE.equals(m.get("low"));
            }
        }
    }

    static void importExtras(Context c, Backup.Data d) {
        SharedPreferences.Editor ed = prefs(c).edit();
        Map<String, Object> st = d.settings;
        for (String k : STR_KEYS) if (st.get(k) instanceof String) ed.putString(k, (String) st.get(k));
        for (String k : BOOL_KEYS) if (st.get(k) instanceof Boolean) ed.putBoolean(k, (Boolean) st.get(k));
        V30.backupIn(st, ed);
        V201.backupIn(st, ed);
        V21.backupIn(c, st, ed);
        if (st.get("household") instanceof List) ed.putString("household", Json.write(Household.toList(Household.parse(st.get("household")))));
        if (st.get("region") instanceof String && !Region.valid((String) st.get("region")).isEmpty()) ed.putBoolean("welcomed", true);
        Map<String, Object> inv = new java.util.LinkedHashMap<String, Object>();
        for (Backup.PantryEntry e : d.pantry) {
            Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
            if (!e.loc.isEmpty()) m.put("loc", e.loc);
            if (!e.frozen.isEmpty()) m.put("frozen", e.frozen);
            if (e.low) m.put("low", true);
            if (!m.isEmpty()) inv.put(Normalizer.normalize(e.label), m);
        }
        ed.putString("inv20", Json.write(inv));
        ed.commit();
        V20Alarm.schedule(c); // holiday notifications may have been switched on/off by the backup
        synchronized (V20.class) { priceMemo = null; priceVersion++; DIET_MEMO.clear(); }
    }

    // ---------------- UI helpers ----------------
    static TextView action(MainActivity a, String label, int icon, View.OnClickListener l) {
        TextView t = a.smallAction(label, icon);
        t.setOnClickListener(l);
        return t;
    }

    static LinearLayout.LayoutParams rowLp(Context c) { return Ui.lpMargins(c, -2, dp(c, 48), 0, 8, 0, 0); }

    static CheckBox check(Context c, String label, boolean on) {
        CheckBox b = new CheckBox(c);
        b.setText(label);
        b.setTextColor(Ui.TEXT);
        b.setChecked(on);
        b.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        return b;
    }

    static AlertDialog show(MainActivity a, String title, View content, String pos, Runnable onPos) {
        ScrollView sv = new ScrollView(a);
        sv.addView(content);
        AlertDialog.Builder b = a.dialog().setTitle(title).setView(sv);
        if (pos != null) b.setPositiveButton(pos, (d, w) -> { if (onPos != null) onPos.run(); });
        b.setNegativeButton(pos == null ? "Close" : "Cancel", null);
        return b.show();
    }

    static LinearLayout box(Context c) {
        LinearLayout box = Ui.column(c);
        box.setPadding(dp(c, 24), dp(c, 12), dp(c, 24), dp(c, 4));
        return box;
    }

    static void changed(MainActivity a) {
        synchronized (V20.class) { priceVersion++; }
        a.refreshAfterV20();
    }

    // ---------------- Settings rows (added to About) ----------------
    static void settingsRows(final MainActivity a, LinearLayout box, final Runnable dismiss) {
        String r = region(a);
        box.addView(action(a, "Region & formats" + (r.isEmpty() ? "" : " (" + Region.name(r) + ")") + "\u2026", R.drawable.ic_leaf, v -> { dismiss.run(); regionDialog(a, false); }), rowLp(a));
        box.addView(action(a, "Kitchen inventory\u2026", R.drawable.ic_list, v -> { dismiss.run(); inventoryDialog(a); }), rowLp(a));
        box.addView(action(a, "Kitchen helpers (times, swaps, blends)\u2026", R.drawable.ic_swap, v -> { dismiss.run(); V20Helpers.hub(a); }), rowLp(a));
        V30.settingsRows(a, box, dismiss);
        V201.settingsRow(a, box, dismiss);
        V21.settingsRow(a, box, dismiss);
        box.addView(action(a, "Household allergies\u2026", R.drawable.ic_warning, v -> { dismiss.run(); householdDialog(a); }), rowLp(a));
        box.addView(action(a, "My recipes, packs & cookbook\u2026", R.drawable.ic_add, v -> { dismiss.run(); myRecipesDialog(a); }), rowLp(a));
        box.addView(action(a, "Regional prices & budget\u2026", R.drawable.ic_cart, v -> { dismiss.run(); pricesDialog(a); }), rowLp(a));
        box.addView(action(a, "Sync with another Android device\u2026", R.drawable.ic_cloud, v -> { dismiss.run(); V20Sync.dialog(a); }), rowLp(a));
        final CheckBox cg = check(a, "Group cuisines by region in Discover", prefs(a).getBoolean("cuisineGroups", true));
        cg.setOnCheckedChangeListener((b, on) -> { prefs(a).edit().putBoolean("cuisineGroups", on).apply(); a.refreshDiscover(); });
        box.addView(cg);
    }

    // ---------------- welcome: region (v2.0.1: starter kits removed) ----------------
    static void maybeWelcome(MainActivity a) {
        if (welcomed(a)) return;
        regionDialog(a, true);
    }

    static void regionDialog(final MainActivity a, final boolean welcome) {
        LinearLayout box = box(a);
        String cur = region(a), guess = Region.guess(java.util.Locale.getDefault().toLanguageTag());
        box.addView(Ui.body(a, welcome ? "Where do you cook? This sets units, temperatures, currency, date format, week start and the allergen list. You can change any of it later in Settings."
                : "Choosing a region resets units, temperature notes, currency, date and number format, week start and clock to that region's defaults. Each can then be changed below."));
        // v2.0.1: the suggestion comes from the device language (Locale), never from location; no location API or permission exists in the app
        if (cur.isEmpty()) box.addView(Ui.caption(a, "Suggested from your device language. This app never uses your location. You can pick any region."));
        final android.widget.RadioGroup rg = new android.widget.RadioGroup(a);
        for (int i = 0; i < Region.IDS.size(); i++) {
            String id = Region.IDS.get(i);
            android.widget.RadioButton rb = new android.widget.RadioButton(a);
            rb.setId(1000 + i);
            rb.setText(Region.name(id) + (id.equals(guess) && cur.isEmpty() ? "  \u00b7 suggested" : ""));
            rb.setTextColor(Ui.TEXT);
            rb.setMinHeight(dp(a, 48));
            rg.addView(rb);
            if (id.equals(cur.isEmpty() ? guess : cur)) rg.check(1000 + i);
        }
        box.addView(rg);
        final EditText df = new EditText(a), cu = new EditText(a);
        final CheckBox dc = check(a, "Decimal comma (1,5)", prefs(a).getBoolean("decimalComma", false));
        final CheckBox c24 = check(a, "24-hour clock", prefs(a).getBoolean("clock24", false));
        final CheckBox sun = check(a, "Week starts on Sunday", weekSunday(a));
        final CheckBox gas = check(a, "Show gas marks next to oven temperatures", prefs(a).getBoolean("gasMarks", false));
        final CheckBox fan = check(a, "Show the fan-oven note", prefs(a).getBoolean("fanNote", false));
        if (!welcome && !cur.isEmpty()) {
            box.addView(Ui.sectionLabel(a, "Formats (editable)"), Ui.lpMargins(a, -2, -2, 0, 12, 0, 4));
            df.setText(prefs(a).getString("dateFormat", "YYYY-MM-DD")); df.setHint("Date format, e.g. DD/MM/YYYY"); df.setSingleLine(true);
            cu.setText(V16.currency(a)); cu.setHint("Currency symbol"); cu.setSingleLine(true);
            box.addView(df); box.addView(cu); box.addView(dc); box.addView(c24); box.addView(sun); box.addView(gas); box.addView(fan);
            box.addView(Ui.caption(a, "Units: Settings \u203a Measurement units. Allergen list: " + Region.allergenListName(cur) + "."));
        }
        AlertDialog.Builder b = a.dialog().setTitle(welcome ? "Welcome" : "Region & formats");
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        b.setView(sv).setCancelable(!welcome);
        b.setPositiveButton(welcome ? "Next" : "Save", (d, w) -> {
            int k = rg.getCheckedRadioButtonId() - 1000;
            String r = k >= 0 && k < Region.IDS.size() ? Region.IDS.get(k) : Region.OTHER;
            if (!r.equals(region(a))) applyRegion(a, r);
            else if (!welcome) {
                String f = df.getText().toString().trim();
                prefs(a).edit().putString("dateFormat", f.isEmpty() ? "YYYY-MM-DD" : f).putString("currency", cu.getText().toString().trim().isEmpty() ? "$" : cu.getText().toString().trim())
                        .putBoolean("decimalComma", dc.isChecked()).putBoolean("clock24", c24.isChecked()).putString("weekStart", sun.isChecked() ? "sun" : "mon")
                        .putBoolean("gasMarks", gas.isChecked()).putBoolean("fanNote", fan.isChecked()).apply();
            }
            prefs(a).edit().putBoolean("welcomed", true).apply();
            changed(a);
        });
        if (!welcome) b.setNegativeButton("Cancel", null);
        else b.setNegativeButton("Skip", (d, w) -> { applyRegion(a, guess.isEmpty() ? Region.US : guess); changed(a); });
        b.show();
    }

    // ---------------- recipe detail extras ----------------
    static View detailExtras(final MainActivity a, final Recipe r) {
        LinearLayout box = Ui.column(a);
        String rg = region(a);
        List<String> al = Household.alerts(r, household(a), rg.isEmpty() ? Region.US : rg);
        if (!al.isEmpty()) {
            StringBuilder sb = new StringBuilder("Household: ");
            for (int i = 0; i < al.size(); i++) sb.append(i == 0 ? "" : " \u00b7 ").append(al.get(i));
            TextView t = Ui.body(a, sb.toString());
            t.setTextColor(Ui.AMBER_TEXT);
            box.addView(t, Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        }
        Set<String> d = dietsOf(a, r);
        if (!d.isEmpty()) {
            StringBuilder sb = new StringBuilder("Fits: ");
            int i = 0;
            for (String x : d) sb.append(i++ == 0 ? "" : " \u00b7 ").append(dietName(x));
            box.addView(Ui.caption(a, sb.append(" \u2014 a guide, not certification.").toString()), Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        }
        View fl = V30.fastingLine(a, r);
        if (fl != null) box.addView(fl, Ui.lpMargins(a, -1, -2, 0, 4, 0, 0));
        View mg = V201.detailCard(a, r);
        if (mg != null) box.addView(mg, Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        if (prefs(a).getBoolean("fanNote", false)) for (String s : r.steps) if (s.contains("\u00b0") || s.toLowerCase(java.util.Locale.US).contains("degrees")) {
            box.addView(Ui.caption(a, Region.FAN_NOTE), Ui.lpMargins(a, -1, -2, 0, 6, 0, 0));
            break;
        }
        LinearLayout row = Ui.row(a);
        boolean mine = r.id.startsWith("u:");
        row.addView(action(a, mine ? "Edit" : "Make my version", R.drawable.ic_add, v -> editor(a, mine ? RecipeEditor.draftOf(r) : RecipeEditor.myVersion(r), mine ? r.id : null)), new LinearLayout.LayoutParams(-2, dp(a, 48)));
        row.addView(Ui.space(a, 8, 1));
        row.addView(action(a, "Share as text", R.drawable.ic_list, v -> shareText(a, r.title, RecipeEditor.shareText(r, r.servings))), new LinearLayout.LayoutParams(-2, dp(a, 48)));
        if (Flavor.MODERN) {
            row.addView(Ui.space(a, 8, 1));
            row.addView(action(a, "Card image", R.drawable.ic_leaf, v -> CardProvider.shareCard(a, r)), new LinearLayout.LayoutParams(-2, dp(a, 48)));
        }
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(a);
        hs.addView(row);
        box.addView(hs, Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        return box;
    }

    static void shareText(Activity a, String subject, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, text);
        try { a.startActivity(Intent.createChooser(i, "Share")); } catch (Exception e) { Ui.toastShort(a, "No app to share with"); }
    }

    // ---------------- household ----------------
    static void householdDialog(final MainActivity a) {
        final List<Household.Person> ps = household(a);
        final LinearLayout box = box(a);
        final Runnable[] render = new Runnable[1];
        final List<String> ids = regionAllergens(a);
        final String rg = region(a).isEmpty() ? Region.US : region(a);
        render[0] = () -> {
            box.removeAllViews();
            box.addView(Ui.body(a, "Add the people you cook for and their allergies. Recipes show an alert naming them; tick \u201chide\u201d to also hide those recipes. "
                    + "Matching is name-based and not a guarantee \u2014 always check labels."));
            for (final Household.Person p : ps) {
                box.addView(Ui.sectionLabel(a, p.name), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
                FlowLayout f = new FlowLayout(a, dp(a, 6), dp(a, 6));
                for (final String id : ids) {
                    final TextView ch = Ui.chip(a, Region.allergenLabel(id, rg), p.allergens.contains(id));
                    ch.setOnClickListener(v -> { if (!p.allergens.remove(id)) p.allergens.add(id); Ui.styleChip(a, ch, p.allergens.contains(id)); });
                    f.addView(ch);
                }
                box.addView(f);
                final CheckBox h = check(a, "Also hide these recipes", p.hide);
                h.setOnCheckedChangeListener((b, on) -> p.hide = on);
                box.addView(h);
                box.addView(action(a, "Remove " + p.name, R.drawable.ic_remove, v -> { ps.remove(p); render[0].run(); }), rowLp(a));
            }
            final EditText name = new EditText(a);
            name.setHint("Name");
            name.setSingleLine(true);
            box.addView(name, Ui.lpMargins(a, -1, -2, 0, 12, 0, 0));
            box.addView(action(a, "Add person", R.drawable.ic_add, v -> {
                String n = name.getText().toString().trim();
                if (n.isEmpty() || ps.size() >= 12) return;
                Household.Person p = new Household.Person();
                p.name = n.length() > 40 ? n.substring(0, 40) : n;
                ps.add(p);
                render[0].run();
            }), rowLp(a));
        };
        render[0].run();
        show(a, "Household allergies", box, "Save", () -> { saveHousehold(a, ps); changed(a); });
    }

    // ---------------- inventory ----------------
    static void inventoryDialog(final MainActivity a) {
        LinearLayout box = box(a);
        List<Inventory.Item> items = items(a);
        int[] n = Inventory.counts(items);
        final AlertDialog[] self = new AlertDialog[1];
        for (int i = 0; i < Inventory.LOCATIONS.size(); i++) {
            final String id = Inventory.LOCATIONS.get(i);
            box.addView(action(a, Inventory.locationName(id) + " " + n[i], R.drawable.ic_list, v -> { self[0].dismiss(); locationDialog(a, id); }), rowLp(a));
        }
        String today = Dates.today();
        List<Inventory.Item> fz = Inventory.freezerAlerts(items, today);
        if (!fz.isEmpty()) {
            box.addView(Ui.sectionLabel(a, "Freezer: use or toss"), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
            for (Inventory.Item it : fz) box.addView(Ui.body(a, it.label + " \u2013 " + Inventory.freezerStatus(it.frozen, it.key, today)));
        }
        final List<Inventory.Item> rs = Inventory.restock(items);
        box.addView(Ui.sectionLabel(a, "Restock list " + rs.size()), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
        if (rs.isEmpty()) box.addView(Ui.caption(a, "Mark items as running low to collect them here."));
        for (Inventory.Item it : rs) box.addView(Ui.body(a, "\u2022 " + it.label));
        if (!rs.isEmpty()) box.addView(action(a, "Add restock items to the shopping list", R.drawable.ic_cart, v -> {
            int k = 0;
            for (Inventory.Item it : rs) if (Store.get(a).addShopping(it.label, Double.NaN, "", "Restock")) k++;
            Ui.toastShort(a, k + " added");
            a.refreshShopping();
        }), rowLp(a));
        final CheckBox rem = check(a, "Remind me the day before a use-by date (with a \u201ccook this tonight\u201d idea)", prefs(a).getBoolean("expiryReminders", true));
        rem.setOnCheckedChangeListener((b, on) -> { prefs(a).edit().putBoolean("expiryReminders", on).apply(); V20Alarm.schedule(a); if (on) V20Alarm.askPermission(a); });
        box.addView(rem, Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
        box.addView(Ui.caption(a, "Auto-deduct after cooking: Settings \u203a \u201cCooked this\u201d offers to use up pantry amounts."));
        self[0] = show(a, "Kitchen inventory", box, null, null);
    }

    static void locationDialog(final MainActivity a, final String loc) {
        LinearLayout box = box(a);
        final AlertDialog[] self = new AlertDialog[1];
        List<Inventory.Item> at = Inventory.at(items(a), loc);
        if (at.isEmpty()) box.addView(Ui.caption(a, "Nothing here yet. Items go to a location automatically (you can move them)."));
        String today = Dates.today();
        for (final Inventory.Item it : at) {
            StringBuilder sb = new StringBuilder(it.label);
            if (!it.qty.isEmpty()) sb.append(" \u00b7 ").append(it.qty);
            if (!it.useBy.isEmpty()) sb.append(" \u00b7 use by ").append(date(a, it.useBy));
            if (Inventory.FREEZER.equals(loc) && !it.frozen.isEmpty()) sb.append(" \u00b7 ").append(Inventory.freezerStatus(it.frozen, it.key, today));
            if (it.low) sb.append(" \u00b7 low");
            box.addView(action(a, sb.toString(), R.drawable.ic_leaf, v -> { self[0].dismiss(); itemDialog(a, it); }), rowLp(a));
        }
        if (Inventory.SPICE.equals(loc)) {
            Set<String> have = new HashSet<String>(Store.get(a).pantry.keySet());
            List<String[]> buy = Inventory.spicesToBuy(have, Inventory.keyCounts(Repo.get(a).all()), 8);
            box.addView(Ui.sectionLabel(a, "Spices to buy next"), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
            for (String[] s : buy) box.addView(Ui.body(a, "\u2022 " + s[0] + (s.length > 1 ? " \u2013 " + s[1] : "")));
        }
        self[0] = show(a, Inventory.locationName(loc), box, null, null);
    }

    static void itemDialog(final MainActivity a, final Inventory.Item it) {
        LinearLayout box = box(a);
        final android.widget.Spinner sp = new android.widget.Spinner(a);
        List<String> names = new ArrayList<String>();
        for (String id : Inventory.LOCATIONS) names.add(Inventory.locationName(id));
        sp.setAdapter(new android.widget.ArrayAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item, names));
        sp.setSelection(Math.max(0, Inventory.LOCATIONS.indexOf(it.location())));
        box.addView(sp);
        final EditText qty = new EditText(a), use = new EditText(a), fr = new EditText(a);
        qty.setHint("Amount, e.g. 500 g"); qty.setText(it.qty); qty.setSingleLine(true);
        use.setHint("Use by (YYYY-MM-DD)"); use.setText(it.useBy); use.setSingleLine(true);
        fr.setHint("Frozen on (YYYY-MM-DD)"); fr.setText(it.frozen); fr.setSingleLine(true);
        box.addView(qty); box.addView(use); box.addView(fr);
        final CheckBox low = check(a, "Running low (add to restock list)", it.low);
        box.addView(low);
        show(a, it.label, box, "Save", () -> {
            String u = use.getText().toString().trim(), f = fr.getText().toString().trim();
            if (!u.isEmpty() && !Dates.valid(u)) u = it.useBy;
            if (!f.isEmpty() && !Dates.valid(f)) f = it.frozen;
            String l = Inventory.LOCATIONS.get(sp.getSelectedItemPosition());
            Store.get(a).setPantryDetails(it.key, u, qty.getText().toString().trim());
            setInv(a, it.key, l.equals(Inventory.defaultLocation(it.key, it.label)) ? "" : l, f, low.isChecked());
            a.pantryChangedExternally();
        });
    }

    // ---------------- prices & budget ----------------
    static void pricesDialog(final MainActivity a) {
        LinearLayout box = box(a);
        String r = region(a);
        final EditText zip = new EditText(a), bud = new EditText(a);
        if (!r.isEmpty() && !Region.US.equals(r)) {
            box.addView(Ui.body(a, Region.PRICE_NOTE_NON_US));
        } else {
            String pr = priceRegion(a);
            int[] cov = RegionalPrices.coverage(bls(a), pr);
            box.addView(Ui.body(a, "U.S. ZIP code (first 3 digits pick the BLS region; nothing is sent anywhere)."));
            zip.setHint("ZIP code"); zip.setText(prefs(a).getString("zip", "")); zip.setSingleLine(true);
            zip.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            box.addView(zip);
            box.addView(Ui.caption(a, "Region: " + RegionalPrices.regionName(pr) + ". BLS average prices, " + bls(a).date + ": "
                    + cov[0] + " items " + (pr.isEmpty() ? "(U.S. average)" : "with a regional average") + (cov.length > 1 ? ", " + cov[1] + " U.S.-average only" : "")
                    + ". Other items use built-in estimates \u00d7 the regional factor. Your own prices always win. Averages, not live store prices."));
        }
        box.addView(Ui.sectionLabel(a, "Budget mode"), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
        bud.setHint("Budget per serving, e.g. 2.50"); bud.setText(budget(a) > 0 ? prefs(a).getString("budget", "") : ""); bud.setSingleLine(true);
        bud.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(bud);
        final CheckBox on = check(a, "Only show recipes within budget (unknown costs are kept, shown as \u201cprice unknown\u201d)", prefs(a).getBoolean("budgetMode", false));
        box.addView(on);
        box.addView(action(a, "Your prices & currency\u2026", R.drawable.ic_cart, v -> V16.pricesDialog(a, null, () -> changed(a))), rowLp(a));
        box.addView(action(a, "Look up Open Prices for a barcode\u2026", R.drawable.ic_cloud, v -> openPrices(a)), rowLp(a));
        show(a, "Regional prices & budget", box, "Save", () -> {
            prefs(a).edit().putString("zip", RegionalPrices.cleanZip(zip.getText().toString())).putString("budget", bud.getText().toString().trim())
                    .putBoolean("budgetMode", on.isChecked()).apply();
            synchronized (V20.class) { priceMemo = null; }
            changed(a);
        });
    }

    static void openPrices(final MainActivity a) {
        LinearLayout box = box(a);
        final EditText bc = new EditText(a);
        bc.setHint("Barcode (EAN/UPC)");
        bc.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        box.addView(bc);
        box.addView(Ui.caption(a, "Optional: asks prices.openfoodfacts.org (Open Prices, ODbL) for prices other shoppers reported. Only the barcode is sent."));
        show(a, "Open Prices", box, "Look up", () -> {
            final String code = bc.getText().toString().replaceAll("[^0-9]", "");
            if (code.length() < 8) { Ui.toastShort(a, "Type a barcode"); return; }
            final String pr = priceRegion(a);
            new Thread(() -> {
                String txt;
                try {
                    String json = httpGet(RegionalPrices.openPricesUrl(code));
                    List<RegionalPrices.OpenPrice> ps = RegionalPrices.parseOpenPrices(json, pr, 10);
                    StringBuilder sb = new StringBuilder();
                    for (RegionalPrices.OpenPrice p : ps) sb.append(p.currency).append(' ').append(p.price).append(" \u00b7 ").append(p.date)
                            .append(p.store.isEmpty() ? "" : " \u00b7 " + p.store).append(p.nearby ? " (your region)" : "").append('\n');
                    txt = ps.isEmpty() ? "No reported prices for this barcode." : sb.toString() + "\nSource: Open Prices (Open Food Facts), ODbL.";
                } catch (Exception e) {
                    txt = "Could not reach Open Prices.";
                }
                final String t = txt;
                a.runOnUiThread(() -> a.dialog().setTitle("Open Prices").setMessage(t).setPositiveButton("OK", null).show());
            }).start();
        });
    }

    // ---------------- editor, my recipes, packs, cookbook ----------------
    static void myRecipesDialog(final MainActivity a) {
        LinearLayout box = box(a);
        final AlertDialog[] self = new AlertDialog[1];
        final List<Recipe> mine = mine(a);
        box.addView(action(a, "New recipe", R.drawable.ic_add, v -> { self[0].dismiss(); editor(a, new RecipeEditor.Draft(), null); }), rowLp(a));
        for (final Recipe r : mine) box.addView(action(a, r.title, R.drawable.ic_leaf, v -> { self[0].dismiss(); a.openDetail(r.id, true); }), rowLp(a));
        if (mine.isEmpty()) box.addView(Ui.caption(a, "No recipes of your own yet. Use \u201cMake my version\u201d on any recipe, or start a new one."));
        box.addView(Ui.sectionLabel(a, "Packs & cookbook"), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
        box.addView(action(a, "Share my recipes as a pack", R.drawable.ic_cloud, v -> {
            if (mine.isEmpty()) { Ui.toastShort(a, "No recipes to share"); return; }
            shareText(a, "My Pantry Recipes pack", RecipeEditor.packWrite("My recipes", mine, Extras.version(a), Dates.today()));
        }), rowLp(a));
        box.addView(action(a, "Import a recipe pack\u2026", R.drawable.ic_list, v -> {
            self[0].dismiss();
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
            try { a.startActivityForResult(i, REQ_PACK); } catch (Exception e) { Ui.toastShort(a, "No file picker"); }
        }), rowLp(a));
        box.addView(action(a, "Save a printable family cookbook (HTML)\u2026", R.drawable.ic_list, v -> {
            self[0].dismiss();
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/html")
                    .putExtra(Intent.EXTRA_TITLE, "family-cookbook.html");
            try { a.startActivityForResult(i, REQ_COOKBOOK); } catch (Exception e) { Ui.toastShort(a, "No file picker"); }
        }), rowLp(a));
        box.addView(Ui.caption(a, "The cookbook has your own recipes and favorites with ratings and notes; open it in a browser to print. No OCR or photo import."));
        self[0] = show(a, "My recipes", box, null, null);
    }

    static final int REQ_PACK = 2001, REQ_COOKBOOK = 2002;

    static List<Recipe> mine(Context c) {
        List<Recipe> out = new ArrayList<Recipe>();
        for (Recipe r : Repo.get(c).all()) if (r.id.startsWith("u:")) out.add(r);
        return out;
    }

    /** Handles the pack / cookbook pickers; true when the request was ours. */
    static boolean onResult(final MainActivity a, int req, android.net.Uri uri) {
        if (req != REQ_PACK && req != REQ_COOKBOOK) return false;
        if (uri == null) return true;
        try {
            if (req == REQ_COOKBOOK) {
                List<Recipe> rs = mine(a);
                Store s = Store.get(a);
                for (String id : s.favoriteIdsNewestFirst()) { Recipe r = Repo.get(a).byId(id); if (r != null && !rs.contains(r)) rs.add(r); }
                java.io.OutputStream out = a.getContentResolver().openOutputStream(uri, "wt");
                out.write(RecipeEditor.cookbookHtml("Our family cookbook", rs, s.ratings, Dates.today()).getBytes("UTF-8"));
                out.close();
                Ui.toastShort(a, "Cookbook saved (" + rs.size() + " recipes)");
            } else {
                java.io.InputStream in = a.getContentResolver().openInputStream(uri);
                java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n, total = 0;
                while ((n = in.read(buf)) > 0 && (total += n) < 8 * 1024 * 1024) b.write(buf, 0, n);
                in.close();
                Set<String> ids = new HashSet<String>();
                for (Recipe r : Repo.get(a).all()) ids.add(r.id);
                List<Recipe> got = RecipeEditor.packParse(b.toString("UTF-8"), ids, V16.foods(a), System.currentTimeMillis());
                for (Recipe r : got) Store.get(a).putCache(r.id, Json.write(r.toMap()));
                Repo.get(a).reload();
                Ui.toastShort(a, got.size() + " recipes added");
            }
        } catch (RecipeEditor.PackException e) {
            a.dialog().setTitle("Can't import").setMessage(e.getMessage()).setPositiveButton("OK", null).show();
        } catch (Exception e) {
            Ui.toastShort(a, "The file could not be used");
        }
        return true;
    }

    static void editor(final MainActivity a, final RecipeEditor.Draft d, final String id) {
        LinearLayout box = box(a);
        final EditText title = field(a, box, "Title", d.title, true);
        final EditText serv = field(a, box, "Servings", String.valueOf(d.servings), true);
        serv.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText prep = field(a, box, "Prep minutes", d.prep >= 0 ? String.valueOf(d.prep) : "", true);
        prep.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText cook = field(a, box, "Cook minutes", d.cook >= 0 ? String.valueOf(d.cook) : "", true);
        cook.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText cui = field(a, box, "Cuisine (optional)", d.cuisine, true);
        box.addView(Ui.caption(a, "Ingredients: one per line, e.g. \u201c2 cups chickpeas\u201d. Suggestions appear as you type."));
        final android.widget.MultiAutoCompleteTextView ing = new android.widget.MultiAutoCompleteTextView(a);
        ing.setText(d.ingredients);
        ing.setMinLines(4);
        ing.setThreshold(2);
        ing.setAdapter(new MainActivity.SuggestAdapter(a, Repo.get(a).ingredientNames()));
        ing.setTokenizer(new LineTokenizer());
        box.addView(ing);
        final EditText steps = field(a, box, "Steps, one per line", d.steps, false);
        final EditText tags = field(a, box, "Tags, comma-separated", d.tags, true);
        final EditText note = field(a, box, "Note", d.note, false);
        box.addView(Ui.caption(a, "Add a photo from the recipe page after saving."));
        a.dialog().setTitle(id == null ? (d.basedOn.isEmpty() ? "New recipe" : "My version") : "Edit recipe").setView(wrap(a, box))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (x, w) -> {
                    d.title = title.getText().toString().trim();
                    d.servings = num(serv, 4);
                    d.prep = num(prep, -1);
                    d.cook = num(cook, -1);
                    d.cuisine = cui.getText().toString().trim();
                    d.ingredients = ing.getText().toString();
                    d.steps = steps.getText().toString();
                    d.tags = tags.getText().toString();
                    d.note = note.getText().toString();
                    String p = RecipeEditor.problem(d);
                    if (!p.isEmpty()) { Ui.toastShort(a, p); editor(a, d, id); return; }
                    String rid = id != null ? id : "u:" + Long.toString(System.currentTimeMillis(), 36);
                    Recipe r = RecipeEditor.build(d, rid, V16.foods(a));
                    Store.get(a).putCache(rid, Json.write(r.toMap()));
                    Repo.get(a).reload();
                    a.openAfterReload(rid);
                }).show();
    }

    static ScrollView wrap(Context c, View v) { ScrollView sv = new ScrollView(c); sv.addView(v); return sv; }

    static int num(EditText e, int def) { try { return Integer.parseInt(e.getText().toString().trim()); } catch (Exception x) { return def; } }

    static EditText field(Context c, LinearLayout box, String hint, String val, boolean single) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(val);
        e.setSingleLine(single);
        if (!single) e.setMinLines(3);
        box.addView(e);
        return e;
    }

    /** Autocomplete tokens are whole lines; the suggestion replaces the ingredient name at the end of the line. */
    static final class LineTokenizer implements android.widget.MultiAutoCompleteTextView.Tokenizer {
        public int findTokenStart(CharSequence t, int cursor) {
            int i = cursor;
            while (i > 0 && t.charAt(i - 1) != '\n') i--;
            // skip a leading amount and unit ("2 cups ")
            int j = i;
            String line = t.subSequence(i, cursor).toString();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*[0-9\u00bc-\u00be\u2150-\u215e./ -]*\\s*(?:(?:cups?|tbsp|tsp|g|kg|ml|l|oz|lb|cans?|cloves?|pinch)\\s+)?").matcher(line);
            if (m.find()) j = i + m.end();
            return Math.min(j, cursor);
        }
        public int findTokenEnd(CharSequence t, int cursor) {
            int i = cursor;
            while (i < t.length() && t.charAt(i) != '\n') i++;
            return i;
        }
        public CharSequence terminateToken(CharSequence t) { return t; }
    }

    // ---------------- expiring tomorrow -> cook this tonight ----------------
    static void cookTonight(final MainActivity a) {
        List<Inventory.Item> exp = Inventory.expiringIn(items(a), Dates.today(), 1);
        Set<String> keys = new HashSet<String>();
        StringBuilder names = new StringBuilder();
        for (Inventory.Item it : exp) { keys.add(it.key); names.append(names.length() == 0 ? "" : ", ").append(it.label); }
        if (keys.isEmpty()) return;
        final Recipe r = Inventory.cookTonight(Repo.get(a).all(), keys, new HashSet<String>(Store.get(a).pantry.keySet()));
        AlertDialog.Builder b = a.dialog().setTitle("Use by tomorrow").setMessage(names + (r == null ? "" : "\n\nCook this tonight: " + r.title))
                .setNegativeButton("Close", null);
        if (r != null) b.setPositiveButton("Open recipe", (d, w) -> a.openDetail(r.id, true));
        b.show();
    }

    static String bannerText(Context c) {
        List<Inventory.Item> exp = Inventory.expiringIn(items(c), Dates.today(), 1);
        if (exp.isEmpty()) return null;
        return exp.size() == 1 ? exp.get(0).label + " is use-by tomorrow \u2013 cook this tonight?" : exp.size() + " items are use-by tomorrow \u2013 cook this tonight?";
    }

    static String httpGet(String u) throws java.io.IOException {
        java.net.HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL(u).openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(12000);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "MyPantryRecipes/2.0 (Android; open source)");
        try {
            if (con.getResponseCode() != 200) throw new java.io.IOException("HTTP " + con.getResponseCode());
            java.io.InputStream in = con.getInputStream();
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n, total = 0;
            while ((n = in.read(buf)) > 0 && (total += n) < 2 * 1024 * 1024) b.write(buf, 0, n);
            in.close();
            return b.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    static List<String> allergenIds() { return Allergens.ALL_IDS; }
    static String unitsNote(Context c) { return Units.validMode(prefs(c).getString("units", Units.WEIGHTS)); }
    static Ingredient unused() { return null; }
}
