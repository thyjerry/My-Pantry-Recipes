package org.pantryrecipes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.pantryrecipes.app.core.Backup;
import org.pantryrecipes.app.core.Durations;
import org.pantryrecipes.app.core.Ingredient;
import org.pantryrecipes.app.core.Methods;
import org.pantryrecipes.app.core.Normalizer;
import org.pantryrecipes.app.core.Recipe;
import org.pantryrecipes.app.core.Substitutions;
import org.pantryrecipes.app.core.Units;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * v1.7 features for the Android app: measurement units (setting, in-recipe toggle), the conversions screen,
 * cooking-method tags and filters, the Instant Pot cheat sheet, swap details and the backup additions.
 * Shared logic lives in core/ (Units, Methods, Durations, Substitutions), the same code paths as the web app's core17.js.
 */
final class V17 {
    private V17() { }

    static int dp(Context c, float v) { return Ui.dp(c, v); }
    static SharedPreferences prefs(Context c) { return Store.get(c).prefs; }

    // ---------------- measurement units ----------------
    /**
     * The in-recipe toggle (Original / US / Metric / Weights); null = use the saved setting.
     * v1.8: remembered across app restarts (pref "detailUnits"); changing the units setting clears it.
     */
    static String detailMode;
    private static boolean detailLoaded;

    static void setDetailMode(Context c, String m) {
        detailMode = m;
        detailLoaded = true;
        prefs(c).edit().putString("detailUnits", m == null ? "" : m).apply();
    }

    static Units.Prefs units(Context c) {
        SharedPreferences p = prefs(c);
        return new Units.Prefs(Units.validMode(p.getString("units", Units.WEIGHTS)),
                "oz".equals(p.getString("weightUnit", "g")) ? "oz" : "g", p.getBoolean("showCups", true));
    }

    static Units.Prefs viewUnits(Context c) {
        Units.Prefs p = units(c);
        if (!detailLoaded) {
            String m = prefs(c).getString("detailUnits", "");
            detailMode = m.isEmpty() ? null : Units.validMode(m);
            detailLoaded = true;
        }
        if (detailMode != null) p.mode = detailMode;
        return p;
    }

    private static Units.Densities densities;

    static synchronized Units.Densities densities(Context c) {
        if (densities == null) densities = Units.parseDensities(V16.asset(c, "densities.tsv"));
        return densities;
    }

    /** Quantity text for a recipe ingredient (detail and cook mode). */
    static String qty(Context c, Ingredient i, double scale) {
        if (Double.isNaN(i.amount)) return i.unit;
        return Units.display(i, scale, viewUnits(c), densities(c));
    }

    /** Quantity text for the shopping list and planner (the saved setting). */
    static String qty(Context c, String name, double amount, String unit) {
        return Units.display(Normalizer.normalize(name), amount, unit, 1, units(c), densities(c));
    }

    static String step(Context c, String text) { return Units.stepText(V20.loc(c, text, true), viewUnits(c)); }

    // ---------------- cooking methods ----------------
    private static final Map<Recipe, List<String>> METHODS = new WeakHashMap<Recipe, List<String>>();

    static synchronized List<String> methods(Recipe r) {
        List<String> m = METHODS.get(r);
        if (m == null) { m = Methods.of(r); METHODS.put(r, m); }
        return m;
    }

    private static Set<String> methodSel;

    /** The saved method filter (cached; do not modify the returned set). */
    static synchronized Set<String> methodFilter(Context c) {
        if (methodSel == null) methodSel = new HashSet<String>(prefs(c).getStringSet("methods", new HashSet<String>()));
        return methodSel;
    }

    static synchronized void setMethodFilter(Context c, Set<String> f) {
        methodSel = new HashSet<String>(f);
        prefs(c).edit().putStringSet("methods", new HashSet<String>(f)).apply();
    }

    static boolean methodPasses(Set<String> filter, Recipe r) {
        if (filter.isEmpty()) return true;
        List<String> m = methods(r);
        for (String f : filter) if (!m.contains(f)) return false;
        return true;
    }

    static String methodIdForLabel(String label) {
        for (String id : Methods.IDS) if (Methods.label(id).equals(label)) return id;
        return null;
    }

    static String methodsText(Recipe r) {
        StringBuilder b = new StringBuilder();
        for (String id : methods(r)) { if (b.length() > 0) b.append(" \u00b7 "); b.append(Methods.label(id)); }
        return b.toString();
    }

    /** Method chips for the search filter row; each toggles (all picked methods must apply). */
    static void addMethodChips(final Activity a, LinearLayout row, final Runnable changed) {
        for (final String id : Methods.IDS) {
            final TextView c = Ui.chip(a, Methods.label(id), methodFilter(a).contains(id));
            c.setContentDescription("Cooking method: " + Methods.label(id));
            c.setOnClickListener(v -> {
                Set<String> f = new HashSet<String>(methodFilter(a));
                if (!f.remove(id)) f.add(id);
                setMethodFilter(a, f);
                Ui.styleChip(a, c, f.contains(id));
                changed.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(a, 48));
            lp.rightMargin = dp(a, 8);
            row.addView(c, lp);
        }
    }

    /** Discover card: browse by cooking method, with counts. */
    static View methodsCard(final MainActivity a, List<Recipe> all) {
        LinearLayout box = Ui.column(a);
        TextView h = Ui.title(a, "Browse by cooking method", 17);
        h.setPadding(dp(a, 20), dp(a, 16), dp(a, 20), dp(a, 8));
        box.addView(h);
        int[] cnt = new int[Methods.IDS.size()];
        for (Recipe r : all) for (String m : methods(r)) { int k = Methods.IDS.indexOf(m); if (k >= 0) cnt[k]++; }
        FlowLayout f = new FlowLayout(a, dp(a, 8), dp(a, 8));
        f.setPadding(dp(a, 16), 0, dp(a, 16), dp(a, 4));
        for (int k = 0; k < Methods.IDS.size(); k++) {
            final String label = Methods.label(Methods.IDS.get(k));
            TextView chip = Ui.chip(a, label + "  " + cnt[k], false);
            chip.setContentDescription("Browse " + label + " recipes");
            chip.setOnClickListener(v -> a.openBrowseSub('k', label));
            f.addView(chip);
        }
        box.addView(f);
        // v2.0.1: Discover is independent of the Recipes method filter (no "filtered to" note any more)
        TextView cs = Ui.text(a, "Instant Pot cheat sheet \u203a", 14, Ui.GREEN_DARK, Ui.MEDIUM);
        cs.setPadding(dp(a, 20), dp(a, 10), dp(a, 20), dp(a, 4));
        cs.setOnClickListener(v -> cheatSheet(a));
        box.addView(cs);
        return box;
    }

    // ---------------- in-recipe unit toggle ----------------
    static View unitToggle(final Activity a, final Runnable rerender) {
        LinearLayout row = Ui.row(a);
        row.setPadding(0, dp(a, 4), 0, dp(a, 4));
        TextView l = Ui.caption(a, "Units");
        l.setPadding(0, 0, dp(a, 8), 0);
        row.addView(l);
        final List<TextView> chips = new ArrayList<TextView>();
        final String cur = viewUnits(a).mode;
        for (final String m : Units.MODES) {
            TextView c = Ui.chip(a, Units.modeLabel(m), m.equals(cur));
            c.setContentDescription("Show quantities: " + Units.modeLabel(m));
            row.addView(c, Ui.lpMargins(a, -2, dp(a, 48), 0, 0, 6, 0));
            chips.add(c);
        }
        for (int k = 0; k < chips.size(); k++) {
            final String m = Units.MODES[k];
            chips.get(k).setOnClickListener(v -> {
                setDetailMode(a, m.equals(units(a).mode) ? null : m);
                for (int j = 0; j < chips.size(); j++) Ui.styleChip(a, chips.get(j), Units.MODES[j].equals(m));
                rerender.run();
            });
        }
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(row);
        return hs;
    }

    // ---------------- swaps ----------------
    /** "Ratio ¾ for 1 · Vegan · Contains: soy\nEffect…" for a swap option. */
    static String swapDetail(Substitutions.Option o) {
        String f = o.flagsLine();
        if (o.effect == null || o.effect.isEmpty()) return f;
        return f.isEmpty() ? o.effect : f + "\n" + o.effect;
    }

    // ---------------- settings ----------------
    static void unitsSettings(final Activity a, LinearLayout box, final Runnable changed) {
        box.addView(Ui.sectionLabel(a, "Measurement units"));
        final Units.Prefs p = units(a);
        FlowLayout modes = new FlowLayout(a, dp(a, 8), dp(a, 8));
        final List<TextView> mc = new ArrayList<TextView>();
        for (final String m : Units.MODES) {
            TextView c = Ui.chip(a, Units.modeLabel(m), m.equals(p.mode));
            mc.add(c);
            modes.addView(c);
        }
        for (int k = 0; k < mc.size(); k++) {
            final String m = Units.MODES[k];
            mc.get(k).setOnClickListener(v -> {
                prefs(a).edit().putString("units", m).apply();
                setDetailMode(a, null);
                for (int j = 0; j < mc.size(); j++) Ui.styleChip(a, mc.get(j), Units.MODES[j].equals(m));
                if (changed != null) changed.run();
            });
        }
        box.addView(modes, Ui.lpMargins(a, -1, -2, 0, 6, 0, 6));
        box.addView(Ui.caption(a, "\u201cWeights for dry baking\u201d shows flour, sugar and other dry baking ingredients in grams (or ounces) "
                + "and keeps everything else as written. Converted amounts are rounded and approximate; a kitchen scale is the most accurate."));
        FlowLayout wu = new FlowLayout(a, dp(a, 8), dp(a, 8));
        final TextView g = Ui.chip(a, "Grams", !"oz".equals(p.weightUnit)), oz = Ui.chip(a, "Ounces", "oz".equals(p.weightUnit));
        g.setOnClickListener(v -> { prefs(a).edit().putString("weightUnit", "g").apply(); Ui.styleChip(a, g, true); Ui.styleChip(a, oz, false); if (changed != null) changed.run(); });
        oz.setOnClickListener(v -> { prefs(a).edit().putString("weightUnit", "oz").apply(); Ui.styleChip(a, g, false); Ui.styleChip(a, oz, true); if (changed != null) changed.run(); });
        wu.addView(g);
        wu.addView(oz);
        box.addView(wu, Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        final CheckBox cups = new CheckBox(a);
        cups.setText("Show cups in brackets after weights");
        cups.setTextColor(Ui.TEXT);
        cups.setChecked(p.showCups);
        cups.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        cups.setOnCheckedChangeListener((b, on) -> { prefs(a).edit().putBoolean("showCups", on).apply(); if (changed != null) changed.run(); });
        box.addView(cups);
    }

    // ---------------- conversions screen ----------------
    interface Calc { String run(String text, String unit); }

    /** One converter block: an amount field, unit chips and a live result line. */
    static Runnable converter(final Activity a, LinearLayout box, String title, String hint, String initial, final String[] units, final String[] labels,
                          int sel, final Calc calc) {
        TextView h = Ui.sectionLabel(a, title);
        h.setPadding(0, dp(a, 18), 0, dp(a, 4));
        box.addView(h);
        final EditText amt = V16.field(a, hint, initial, InputType.TYPE_CLASS_TEXT);
        box.addView(amt);
        final String[] unit = {units[sel]};
        final TextView out = Ui.body(a, "");
        out.setTextColor(Ui.GREEN_DARK);
        final Runnable update = () -> {
            String r = calc.run(amt.getText().toString(), unit[0]);
            out.setText(r.isEmpty() ? "Type an amount, e.g. 1\u00bd, 3/4 or 1-2" : r);
        };
        FlowLayout f = new FlowLayout(a, dp(a, 6), dp(a, 6));
        final List<TextView> chips = new ArrayList<TextView>();
        for (int k = 0; k < units.length; k++) {
            final int kk = k;
            TextView c = Ui.chip(a, labels[k], k == sel);
            c.setOnClickListener(v -> {
                unit[0] = units[kk];
                for (int j = 0; j < chips.size(); j++) Ui.styleChip(a, chips.get(j), j == kk);
                update.run();
            });
            chips.add(c);
            f.addView(c);
        }
        box.addView(f, Ui.lpMargins(a, -1, -2, 0, 4, 0, 4));
        box.addView(out);
        amt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            public void onTextChanged(CharSequence s, int st, int b, int c) { }
            public void afterTextChanged(Editable e) { update.run(); }
        });
        update.run();
        return update;
    }

    /** Runs a single-value converter over a typed amount or range ("1-2" gives both ends). */
    interface One { String run(double v); }

    static String range(String text, One f) {
        double[] r = Units.parseAmount(text);
        if (r == null) return "";
        String lo = f.run(r[0]);
        if (lo.isEmpty()) return "";
        if (r[0] == r[1]) return lo;
        return lo + "\nto\n" + f.run(r[1]);
    }

    static void conversions(final Activity a, final Runnable changed) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), dp(a, 8));
        unitsSettings(a, box, changed);
        String[] all = {"tsp", "tbsp", "cup", "fl oz", "ml", "l", "g", "kg", "oz", "lb"};
        converter(a, box, "Volume & weight", "Amount", "1", all, all, 2,
                (t, u) -> range(t, v -> { String s = Units.volumeLine(v, u); return s.isEmpty() ? Units.massLine(v, u) : s; }));
        box.addView(Ui.caption(a, "Cups are US cups (240 ml, the nutrition-label size; the older customary cup is 236.6 ml, about 1.5% less); a tablespoon is 15 ml, a teaspoon 5 ml and a fluid ounce 30 ml."));

        TextView ih = Ui.sectionLabel(a, "Ingredient: cups \u2194 grams \u2194 ounces");
        ih.setPadding(0, dp(a, 18), 0, dp(a, 4));
        box.addView(ih);
        final Units.Densities ds = densities(a);
        List<String> names = new ArrayList<String>();
        for (Units.Density d : ds.list) names.add(d.name);
        final AutoCompleteTextView ing = new AutoCompleteTextView(a);
        ing.setHint("Ingredient (" + ds.list.size() + " to choose from)");
        ing.setText("all-purpose flour");
        ing.setSingleLine(true);
        ing.setTextColor(Ui.TEXT);
        ing.setHintTextColor(Ui.TEXT3);
        ing.setThreshold(1);
        ing.setAdapter(new MainActivity.SuggestAdapter(a, names));
        box.addView(ing);
        final String[] iu = {"cup", "tbsp", "tsp", "g", "oz"};
        final Runnable ingUpdate = converter(a, box, "Amount of it", "Amount", "1", iu, iu, 0, (t, u) -> {
            Units.Density d = ds.forKey(Normalizer.normalize(ing.getText().toString()));
            if (d == null) return ing.getText().toString().trim().isEmpty() ? "" : "No density for \u201c" + ing.getText().toString().trim() + "\u201d yet. Pick one from the list.";
            final Units.Density dd = d;
            String r = range(t, v -> Units.ingredientLine(v, u, dd));
            return r.isEmpty() ? "" : d.name + ": " + r;
        });
        // re-run the ingredient converter when the ingredient changes
        ing.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            public void onTextChanged(CharSequence s, int st, int b, int c) { }
            public void afterTextChanged(Editable e) { ingUpdate.run(); }
        });
        box.addView(Ui.caption(a, "Densities are rounded averages for spooned-and-levelled cups; a kitchen scale is more accurate."));

        String[] bu = {"stick", "tbsp", "cup", "g", "oz"};
        String[] bl = {"sticks", "tbsp", "cups", "g", "oz"};
        converter(a, box, "Butter", "Amount", "1", bu, bl, 0, (t, u) -> range(t, v -> v <= 0 ? "" : Units.butterText(Units.butterGrams(v, u))));
        box.addView(Ui.caption(a, "1 US stick = 113 g = 8 tbsp = \u00bd cup = 4 oz."));

        String[] tu = {"C", "F", "gas"};
        String[] tl = {"\u00b0C", "\u00b0F", "Gas mark"};
        converter(a, box, "Oven temperature", "Temperature", "180", tu, tl, 0, (t, u) -> Units.tempLine(t, u));

        TextView oh = Ui.sectionLabel(a, "Oven table");
        oh.setPadding(0, dp(a, 18), 0, dp(a, 4));
        box.addView(oh);
        StringBuilder sb = new StringBuilder("\u00b0C \u00b7 \u00b0F \u00b7 Gas \u00b7 Fan \u00b0C\n");
        for (String[] r : Units.OVEN) sb.append(r[0]).append(" \u00b7 ").append(r[1]).append(" \u00b7 ").append(r[2]).append(" \u00b7 ").append(r[3]).append("  ").append(r[4]).append('\n');
        TextView ot = Ui.body(a, sb.toString().trim());
        ot.setTypeface(android.graphics.Typeface.MONOSPACE);
        ot.setTextSize(13);
        box.addView(ot);
        box.addView(Ui.caption(a, "Approximate. Fan (convection) ovens usually run about 20 \u00b0C lower; check your oven's manual."));
        TextView cs = Ui.text(a, "Instant Pot cheat sheet \u203a", 14, Ui.GREEN_DARK, Ui.MEDIUM);
        cs.setPadding(0, dp(a, 14), 0, dp(a, 4));
        box.addView(cs);
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        new Ui.Dialog(a).setTitle("Conversions").setView(sv).setPositiveButton("Done", null).show();
        cs.setOnClickListener(v -> cheatSheet(a));
    }

    // ---------------- Instant Pot cheat sheet ----------------
    static void cheatSheet(Activity a) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), dp(a, 8));
        TextView w = Ui.text(a, Methods.IP_DISCLAIMER, 13, Ui.AMBER_TEXT, Ui.MEDIUM);
        box.addView(w);
        TextView th = Ui.sectionLabel(a, "Minutes at High pressure");
        th.setPadding(0, dp(a, 16), 0, dp(a, 4));
        box.addView(th);
        for (String[] r : Methods.IP_TIMES) {
            LinearLayout row = Ui.column(a);
            row.setPadding(0, dp(a, 6), 0, dp(a, 6));
            row.addView(Ui.text(a, r[0], 15, Ui.TEXT, Ui.MEDIUM));
            row.addView(Ui.caption(a, r[2] + " min \u00b7 " + r[3] + " \u00b7 " + r[1]));
            box.addView(row);
        }
        TextView tp = Ui.sectionLabel(a, "Good to know");
        tp.setPadding(0, dp(a, 16), 0, dp(a, 4));
        box.addView(tp);
        for (String t : Methods.IP_TIPS) {
            TextView x = Ui.body(a, "\u2022 " + t);
            x.setTextSize(14);
            x.setPadding(0, dp(a, 4), 0, dp(a, 4));
            box.addView(x);
        }
        box.addView(Ui.caption(a, "NR = natural release, QR = quick release. In cook mode, pressure timers count only the time at pressure."),
                Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        new Ui.Dialog(a).setTitle("Instant Pot cheat sheet").setView(sv).setPositiveButton("Done", null).show();
    }

    // ---------------- cook mode ----------------
    static String timerNote(Durations.Match m) {
        if (m.isPressure()) return Durations.BUILD_UP_NOTE;
        if (m.isRelease()) return Durations.RELEASE_NOTE;
        return "";
    }

    // ---------------- backup ----------------
    static void exportExtras(Context c, Backup.Data d) {
        Units.Prefs p = units(c);
        d.settings.put("units", p.mode);
        d.settings.put("weightUnit", p.weightUnit);
        d.settings.put("showCups", p.showCups);
        List<Object> ms = new ArrayList<Object>();
        Set<String> f = methodFilter(c);
        for (String id : Methods.IDS) if (f.contains(id)) ms.add(id);
        d.settings.put("methods", ms);
    }

    static void importExtras(Context c, Backup.Data d) {
        Map<String, Object> st = d.settings;
        SharedPreferences.Editor ed = prefs(c).edit();
        if (st.get("units") instanceof String) ed.putString("units", Units.validMode((String) st.get("units")));
        if (st.get("weightUnit") instanceof String) ed.putString("weightUnit", "oz".equals(st.get("weightUnit")) ? "oz" : "g");
        if (st.get("showCups") instanceof Boolean) ed.putBoolean("showCups", (Boolean) st.get("showCups"));
        if (st.get("methods") instanceof List) {
            Set<String> s = new HashSet<String>();
            for (Object o : (List<?>) st.get("methods")) if (Methods.IDS.contains(String.valueOf(o))) s.add(String.valueOf(o));
            ed.putStringSet("methods", s);
        }
        ed.putString("detailUnits", ""); // follow the imported units setting
        ed.commit();
        synchronized (V17.class) { methodSel = null; }
        detailMode = null;
        detailLoaded = false;
    }
}
