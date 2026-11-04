package org.pantryrecipes.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.pantryrecipes.app.core.Cuisines;
import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.Json;
import org.pantryrecipes.app.core.Recipe;
import org.pantryrecipes.app.core.Stage3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * v2.0 Stage 3 (Android): holidays (calendar, checklist, Pantry banner, recipe sections), fasting-day labels, the baking
 * hub (recipes, calculators, starter tracker, bake log, troubleshooting, storage) and regional sets. assets/stage3.txt is
 * parsed on first use; the Pantry banner is computed after the first frame so it never delays startup on the Fire HD 8.
 */
final class V30 {
    private V30() {}

    private static Stage3 data;

    static synchronized Stage3 data(Context c) {
        if (data == null) data = Stage3.parse(V16.asset(c.getApplicationContext(), "stage3.txt"));
        return data;
    }

    static SharedPreferences prefs(Context c) { return V20.prefs(c); }

    // ---------------- ticked holidays and fasting labels ----------------
    static List<String> ticked(Context c) {
        String raw = prefs(c).getString("holidays30", null);
        List<String> out = new ArrayList<String>();
        if (raw != null) { for (String s : raw.split(",")) if (!s.isEmpty()) out.add(s); return out; }
        String r = V20.region(c);
        for (Stage3.Holiday h : data(c).holidays) if (Stage3.defaultOn(h, r.isEmpty() ? null : r)) out.add(h.id);
        return out;
    }
    static void setTicked(Context c, List<String> ids) { prefs(c).edit().putString("holidays30", join(ids)).apply(); }
    static List<String> fasts(Context c) {
        List<String> out = new ArrayList<String>();
        for (String s : prefs(c).getString("fasts30", "").split(",")) if (!s.isEmpty()) out.add(s);
        return out;
    }
    static int lead(Context c) { int n = prefs(c).getInt("holLead30", 14); return n == 7 || n == 21 ? n : 14; }
    static String join(List<String> l) { StringBuilder sb = new StringBuilder(); for (String s : l) sb.append(sb.length() > 0 ? "," : "").append(s); return sb.toString(); }
    static String when(int days) { return days == 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days"; }

    // ---------------- Settings rows ----------------
    static void settingsRows(final MainActivity a, LinearLayout box, final Runnable dismiss) {
        box.addView(V20.action(a, "Baking (recipes, calculators, starter, log)\u2026", R.drawable.ic_timer, v -> { dismiss.run(); baking(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Holidays & fasting\u2026", R.drawable.ic_calendar, v -> { dismiss.run(); holidays(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Regional sets (recipes by country)\u2026", R.drawable.ic_explore, v -> { dismiss.run(); regional(a); }), V20.rowLp(a));
    }

    // ---------------- Pantry banner ----------------
    /** Fills the holder with a banner for the nearest ticked holiday within the lead time (or clears it). */
    static void banner(final MainActivity a, final LinearLayout holder) {
        holder.removeAllViews();
        if (!prefs(a).getBoolean("holBanner30", true)) return;
        String today = Dates.today(), dis = prefs(a).getString("holDismiss30", "");
        List<Stage3.Upcoming> up = data(a).upcoming(ticked(a), today, lead(a));
        Stage3.Upcoming u = null;
        int more = 0;
        for (Stage3.Upcoming x : up) {
            if (dis.contains("|" + x.h.id + "@" + x.date)) continue;
            if (u == null) u = x; else more++;
        }
        if (u == null) return;
        final Stage3.Upcoming f = u;
        LinearLayout card = Ui.card(a);
        LinearLayout row = Ui.row(a);
        LinearLayout txt = Ui.column(a);
        txt.addView(Ui.title(a, u.h.name + " " + when(u.days), 16));
        txt.addView(Ui.caption(a, V20.date(a, u.date) + (more > 0 ? " \u00b7 " + more + " more coming up" : "")));
        row.addView(txt, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(V20.action(a, "Recipes", R.drawable.ic_list, v -> section(a, f.h.id)), new LinearLayout.LayoutParams(-2, Ui.dp(a, 48)));
        ImageView x = Ui.iconButton(a, R.drawable.ic_close, Ui.TEXT2, "Dismiss " + u.h.name + " reminder");
        x.setOnClickListener(v -> {
            String d = prefs(a).getString("holDismiss30", "");
            if (d.length() > 600) d = d.substring(d.length() - 400);
            prefs(a).edit().putString("holDismiss30", d + "|" + f.h.id + "@" + f.date).apply();
            banner(a, holder);
        });
        row.addView(x);
        card.addView(row);
        card.setContentDescription(u.h.name + " " + when(u.days));
        holder.addView(card, Ui.lpMargins(a, -1, -2, 0, 0, 0, 12));
    }

    // ---------------- holidays ----------------
    static void holidays(final MainActivity a) {
        LinearLayout box = V20.box(a);
        final AlertDialog[] self = new AlertDialog[1];
        box.addView(Ui.caption(a, "Tick the holidays you keep; only those get a Pantry reminder and appear in the calendar."));
        box.addView(V20.action(a, "Calendar (next 12 months)\u2026", R.drawable.ic_calendar, v -> { self[0].dismiss(); calendar(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Choose holidays\u2026", R.drawable.ic_check, v -> { self[0].dismiss(); choose(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Reminder: " + (prefs(a).getBoolean("holBanner30", true) ? lead(a) + " days before" : "off") + "\u2026", R.drawable.ic_timer, v -> { self[0].dismiss(); leadDialog(a); }), V20.rowLp(a));
        final android.widget.CheckBox notify = V20.check(a, "Phone notification " + lead(a) + " days before (off by default)", prefs(a).getBoolean("holNotify30", false));
        notify.setOnCheckedChangeListener((b, on) -> {
            prefs(a).edit().putBoolean("holNotify30", on).apply();
            V20Alarm.schedule(a);
            if (on) V20Alarm.askPermission(a);
        });
        box.addView(notify);
        box.addView(V20.action(a, "Fasting-day labels\u2026", R.drawable.ic_leaf, v -> { self[0].dismiss(); fastingDialog(a); }), V20.rowLp(a));
        box.addView(Ui.caption(a, Stage3.NOTE), Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
        self[0] = V20.show(a, "Holidays & fasting", box, null, null);
    }

    static void calendar(final MainActivity a) {
        final List<Stage3.Upcoming> up = data(a).upcoming(ticked(a), Dates.today(), 365);
        V20Helpers.searchList(a, "Holiday calendar", "Search holidays", up.isEmpty() ? "No holidays ticked yet: open Choose holidays." : "Tap one for recipes.", (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            String mon = null, ql = q.trim().toLowerCase(Locale.ROOT);
            for (Stage3.Upcoming u : up) {
                if (!ql.isEmpty() && !u.h.name.toLowerCase(Locale.ROOT).contains(ql)) continue;
                String m = Dates.MONTH_SHORT[Integer.parseInt(u.date.substring(5, 7)) - 1] + " " + u.date.substring(0, 4);
                if (!m.equals(mon)) { mon = m; out.add(m.toUpperCase(Locale.ROOT)); keys.add(null); }
                out.add(u.h.name + "\n" + V20.date(a, u.date) + " \u00b7 " + when(u.days)); keys.add(u.h.id);
            }
            if (out.isEmpty()) { out.add("Nothing to show."); keys.add(null); }
            return out;
        }, k -> section(a, (String) k));
    }

    static void choose(final MainActivity a) {
        final List<Stage3.Holiday> hs = data(a).holidays;
        final List<String> t = ticked(a);
        String[] names = new String[hs.size()];
        final boolean[] on = new boolean[hs.size()];
        String today = Dates.today();
        for (int i = 0; i < hs.size(); i++) {
            String nx = Stage3.next(hs.get(i), today);
            names[i] = hs.get(i).name + (nx == null ? "" : "  (" + V20.date(a, nx) + ")");
            on[i] = t.contains(hs.get(i).id);
        }
        a.dialog().setTitle("Choose holidays").setMultiChoiceItems(names, on, (d, i, c) -> on[i] = c)
                .setPositiveButton("Save", (d, w) -> {
                    List<String> ids = new ArrayList<String>();
                    for (int i = 0; i < hs.size(); i++) if (on[i]) ids.add(hs.get(i).id);
                    setTicked(a, ids); a.refreshHolidayBanner();
                })
                .setNeutralButton("Region defaults", (d, w) -> { prefs(a).edit().remove("holidays30").apply(); a.refreshHolidayBanner(); })
                .setNegativeButton("Cancel", null).show();
    }

    static void leadDialog(final MainActivity a) {
        final String[] opts = {"7 days before", "14 days before", "21 days before", "Off"};
        int cur = !prefs(a).getBoolean("holBanner30", true) ? 3 : lead(a) == 7 ? 0 : lead(a) == 21 ? 2 : 1;
        a.dialog().setTitle("Pantry reminder").setSingleChoiceItems(opts, cur, (d, i) -> {
            SharedPreferences.Editor e = prefs(a).edit().putBoolean("holBanner30", i != 3);
            if (i != 3) e.putInt("holLead30", i == 0 ? 7 : i == 2 ? 21 : 14);
            e.apply(); d.dismiss(); a.refreshHolidayBanner();
        }).setNegativeButton("Cancel", null).show();
    }

    static void section(final MainActivity a, String id) {
        final Stage3 s = data(a);
        final Stage3.Holiday h = s.holiday(id);
        if (h == null) return;
        String nx = Stage3.next(h, Dates.today());
        a.openRecipeList(h.name, s.section(h, a.repo.all(), 60), true,
                (nx == null ? "No date in the 2026\u20132030 table. " : V20.date(a, nx) + " (" + when(Dates.between(Dates.today(), nx)) + "). ") + "Picked by title, cuisine and ingredients.");
    }

    // ---------------- fasting labels ----------------
    static void fastingDialog(final MainActivity a) {
        final Stage3 s = data(a);
        LinearLayout box = V20.box(a);
        final AlertDialog[] self = new AlertDialog[1];
        box.addView(Ui.caption(a, "Ticked labels show on recipe pages. Each list shows recipes whose ingredients fit the rule."));
        final android.widget.CheckBox chips = V20.check(a, "Also show ticked labels as Discover filter chips", prefs(a).getBoolean("fastChips30", false));
        chips.setOnCheckedChangeListener((b, c) -> { prefs(a).edit().putBoolean("fastChips30", c).apply(); a.fastFiltersChanged(); });
        box.addView(chips);
        final List<String> on = fasts(a);
        for (final Stage3.Fast f : s.fasts) {
            final android.widget.CheckBox cb = V20.check(a, f.label, on.contains(f.id));
            cb.setOnCheckedChangeListener((b, c) -> {
                List<String> l = fasts(a);
                if (c && !l.contains(f.id)) l.add(f.id);
                if (!c) l.remove(f.id);
                prefs(a).edit().putString("fasts30", join(l)).apply();
                a.fastFiltersChanged();
            });
            box.addView(cb);
            box.addView(Ui.caption(a, f.note));
            box.addView(V20.action(a, "See recipes\u2026", R.drawable.ic_list, v -> {
                self[0].dismiss();
                List<Recipe> rs = new ArrayList<Recipe>();
                for (Recipe r : a.repo.all()) if (Stage3.fits(f, r)) rs.add(r);
                a.openRecipeList(f.label, rs, false, f.note);
            }), V20.rowLp(a));
        }
        box.addView(Ui.caption(a, Stage3.NOTE + " Ramadan and Yom Kippur are holiday sections (meals around the fast), not filters."), Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
        self[0] = V20.show(a, "Fasting-day labels", box, null, null);
    }

    /** "Fasting days: ..." line for a recipe page, or null when no label is ticked or none fits. */
    static View fastingLine(Context c, Recipe r) {
        List<String> t = fasts(c);
        if (t.isEmpty()) return null;
        List<Stage3.Fast> l = data(c).labels(r, t);
        if (l.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("Fasting days: ");
        for (int i = 0; i < l.size(); i++) sb.append(i == 0 ? "" : " \u00b7 ").append(l.get(i).label);
        return Ui.caption(c, sb.append(" \u2014 a guide; customs vary.").toString());
    }

    // ---------------- regional sets ----------------
    static void regional(final MainActivity a) {
        final Map<String, Integer> cnt = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        for (Recipe r : a.repo.all()) { String cu = r.cuisine == null ? "" : r.cuisine.trim(); if (!cu.isEmpty()) cnt.put(cu, cnt.containsKey(cu) ? cnt.get(cu) + 1 : 1); }
        final Map<String, List<String>> by = new LinkedHashMap<String, List<String>>();
        List<String> order = new ArrayList<String>(Cuisines.GROUPS);
        order.add("International");
        for (String g : order) by.put(g, new ArrayList<String>());
        for (String cu : cnt.keySet()) { String g = Cuisines.group(cu); if (!by.containsKey(g)) by.put(g, new ArrayList<String>()); by.get(g).add(cu); }
        V20Helpers.searchList(a, "Regional sets", "Search a country", "Each recipe has one country of origin; shared dishes use the most common one.", (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            String ql = q.trim().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<String>> e : by.entrySet()) {
                boolean head = false;
                for (String cu : e.getValue()) {
                    if (!ql.isEmpty() && !cu.toLowerCase(Locale.ROOT).contains(ql) && !e.getKey().toLowerCase(Locale.ROOT).contains(ql)) continue;
                    if (!head) { head = true; out.add(e.getKey().toUpperCase(Locale.ROOT)); keys.add(null); }
                    out.add(cu + "  \u00b7 " + cnt.get(cu)); keys.add(cu);
                }
            }
            if (out.isEmpty()) { out.add("Nothing matches."); keys.add(null); }
            return out;
        }, k -> a.openCuisine((String) k));
    }

    // ---------------- baking ----------------
    static final String[][] GROUPS = {{"bread", "Breads"}, {"cake", "Cakes"}, {"cookie", "Cookies & bars"}, {"pie", "Pies & tarts"}, {"pastry", "Muffins, scones & pastry"}, {"other", "Other bakes"}};
    static final Pattern[] GROUP_RX = {
            Pattern.compile("\\b(bread|loaf|rolls?|buns?|focaccia|bagels?|naan|pitas?|flatbreads?|brioche|challah|sourdough|baguette|ciabatta|pretzels?|tortillas?|chapati|roti|crackers?|pizza dough)\\b"),
            Pattern.compile("\\b(cakes?|cupcakes?|sponge|gateau|torte|cheesecake|madeleines?)\\b"),
            Pattern.compile("\\b(cookies?|biscotti|brownies?|bars|shortbread|slice|blondies?|flapjacks?|biscuits?)\\b"),
            Pattern.compile("\\b(pies?|tarts?|galettes?|quiche|crumble|cobbler|crisp)\\b"),
            Pattern.compile("\\b(muffins?|scones?|croissants?|danish|pastry|pastries|puffs?|strudel|turnovers?|choux|eclairs?|churros|doughnuts?|donuts?)\\b")};
    static String group(Recipe r) {
        String t = r.title.toLowerCase(Locale.ROOT);
        for (int i = 0; i < GROUP_RX.length; i++) if (GROUP_RX[i].matcher(t).find()) return GROUPS[i][0];
        return "other";
    }
    static List<Recipe> bakes(MainActivity c) {
        List<Recipe> out = new ArrayList<Recipe>();
        for (Recipe r : c.repo.all()) {
            String cat = r.category == null ? "" : r.category;
            if (cat.equalsIgnoreCase("Baking") || (cat.equalsIgnoreCase("Dessert") && !group(r).equals("other") && !r.title.toLowerCase(Locale.ROOT).contains("no-bake"))) out.add(r);
        }
        return out;
    }

    static void baking(final MainActivity a) {
        LinearLayout box = V20.box(a);
        final AlertDialog[] self = new AlertDialog[1];
        box.addView(Ui.caption(a, "Calculators and guides work offline. Starter and bake log stay on this device."));
        Object[][] rows = {
                {"Baking recipes\u2026", R.drawable.ic_restaurant, (Runnable) () -> bakeRecipes(a)},
                {"Baker\u2019s percentages & hydration\u2026", R.drawable.ic_swap, (Runnable) () -> bakersDialog(a)},
                {"Pan size converter\u2026", R.drawable.ic_swap, (Runnable) () -> panDialog(a)},
                {"Dough timeline\u2026", R.drawable.ic_timer, (Runnable) () -> timelineDialog(a)},
                {"Oven & high altitude\u2026", R.drawable.ic_info, (Runnable) () -> ovenDialog(a)},
                {"Party calculator\u2026", R.drawable.ic_list, (Runnable) () -> partyDialog(a)},
                {"Sourdough starter tracker\u2026", R.drawable.ic_leaf, (Runnable) () -> starterDialog(a)},
                {"Bake log\u2026", R.drawable.ic_star, (Runnable) () -> logDialog(a)},
                {"Troubleshooting\u2026", R.drawable.ic_warning, (Runnable) () -> troubleDialog(a)},
                {"How long bakes keep\u2026", R.drawable.ic_kitchen, (Runnable) () -> storageDialog(a)}};
        for (final Object[] r : rows) box.addView(V20.action(a, (String) r[0], (Integer) r[1], v -> { self[0].dismiss(); ((Runnable) r[2]).run(); }), V20.rowLp(a));
        self[0] = V20.show(a, "Baking", box, null, null);
    }

    static void bakeRecipes(final MainActivity a) {
        final List<Recipe> all = bakes(a);
        final Map<String, List<Recipe>> by = new LinkedHashMap<String, List<Recipe>>();
        for (String[] g : GROUPS) by.put(g[0], new ArrayList<Recipe>());
        for (Recipe r : all) by.get(group(r)).add(r);
        List<String> labels = new ArrayList<String>();
        final List<String> ids = new ArrayList<String>();
        for (String[] g : GROUPS) if (!by.get(g[0]).isEmpty()) { labels.add(g[1] + " (" + by.get(g[0]).size() + ")"); ids.add(g[0]); }
        labels.add("All baking (" + all.size() + ")"); ids.add("");
        a.dialog().setTitle("Baking recipes").setItems(labels.toArray(new String[0]), (d, i) -> {
            String g = ids.get(i);
            String name = "All baking";
            for (String[] x : GROUPS) if (x[0].equals(g)) name = x[1];
            a.openRecipeList(name, g.isEmpty() ? all : by.get(g), false, "Tip: \u201cWeights where possible\u201d in Region & formats shows flour in grams.");
        }).setNegativeButton("Close", null).show();
    }

    // small helpers for calculator dialogs
    static EditText numField(Context c, LinearLayout box, String label, double val, final Runnable onChange) {
        TextView l = Ui.caption(c, label);
        box.addView(l, Ui.lpMargins(c, -1, -2, 0, 8, 0, 0));
        final EditText e = new EditText(c);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        e.setText(fmt(val));
        e.setContentDescription(label);
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int d) {}
            public void afterTextChanged(Editable s) { if (onChange == null) return; e.removeCallbacks(onChange); e.postDelayed(onChange, 120); }
        });
        box.addView(e);
        return e;
    }
    static double val(EditText e, double def) { try { return Double.parseDouble(e.getText().toString().trim().replace(',', '.')); } catch (Exception x) { return def; } }
    static String fmt(double v) { return v >= 100 || v == Math.rint(v) ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v); }
    static TextView result(Context c, LinearLayout box) {
        TextView t = Ui.title(c, "", 16);
        t.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        box.addView(t, Ui.lpMargins(c, -1, -2, 0, 12, 0, 4));
        return t;
    }

    static void bakersDialog(final MainActivity a) {
        LinearLayout box = V20.box(a);
        box.addView(Ui.caption(a, "Everything is a percentage of the flour weight. Hydration = water \u00f7 flour."));
        final EditText[] f = new EditText[6];
        final TextView out = Ui.title(a, "", 16), out2 = Ui.body(a, "");
        Runnable calc = () -> {
            double fl = val(f[0], 500), hy = val(f[1], 68), sa = val(f[2], 2), ye = val(f[3], 1), st = val(f[4], 0);
            double[] b = Stage3.bakers(fl, hy, sa, ye, st);
            out.setText("Water " + fmt(b[0]) + " g \u00b7 salt " + fmt(b[1]) + " g \u00b7 yeast " + fmt(b[2]) + " g" + (st > 0 ? " \u00b7 starter " + fmt(b[3]) + " g" : "") + "\nTotal dough " + fmt(b[4]) + " g");
            out2.setText("For " + fmt(val(f[5], 900)) + " g of dough use " + fmt(Stage3.flourFor(val(f[5], 900), hy, sa, ye, st)) + " g flour at these percentages.");
        };
        f[0] = numField(a, box, "Flour (g)", 500, calc);
        f[1] = numField(a, box, "Hydration (%)", 68, calc);
        f[2] = numField(a, box, "Salt (%)", 2, calc);
        f[3] = numField(a, box, "Instant yeast (%)", 1, calc);
        f[4] = numField(a, box, "Sourdough starter (%)", 0, calc);
        out.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        box.addView(out, Ui.lpMargins(a, -1, -2, 0, 12, 0, 4));
        f[5] = numField(a, box, "Work back: dough wanted (g)", 900, calc);
        box.addView(out2, Ui.lpMargins(a, -1, -2, 0, 8, 0, 4));
        box.addView(Ui.caption(a, "A 1:1 starter is half flour, half water; for exact hydration count those in too."));
        calc.run();
        V20.show(a, "Baker\u2019s percentages", box, null, null);
    }

    static void panDialog(final MainActivity a) {
        LinearLayout box = V20.box(a);
        final String[] shapes = {"round", "square", "rect"};
        final int[] shp = {0, 0};
        final EditText[] f = new EditText[4];
        final TextView[] out = new TextView[1];
        final Runnable calc = () -> {
            double fa = Stage3.panArea(shapes[shp[0]], val(f[0], 20), val(f[1], 30)), ta = Stage3.panArea(shapes[shp[1]], val(f[2], 23), val(f[3], 33));
            out[0].setText("Multiply the recipe by " + String.format(Locale.ROOT, "%.2f", Stage3.panFactor(fa, ta)));
        };
        box.addView(Ui.caption(a, "Same units for both pans (cm or inches). For rectangles enter length and width; for round and square only the first size is used."));
        for (int p = 0; p < 2; p++) {
            final int pp = p;
            box.addView(Ui.title(a, p == 0 ? "Recipe pan" : "Your pan", 15), Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
            final android.widget.RadioGroup rg = new android.widget.RadioGroup(a);
            rg.setOrientation(LinearLayout.HORIZONTAL);
            String[] lab = {"Round", "Square", "Rectangle"};
            for (int i = 0; i < 3; i++) { android.widget.RadioButton rb = new android.widget.RadioButton(a); rb.setText(lab[i]); rb.setId(1000 + p * 10 + i); rg.addView(rb); }
            rg.check(1000 + p * 10);
            rg.setOnCheckedChangeListener((g, id) -> { shp[pp] = id - 1000 - pp * 10; calc.run(); });
            box.addView(rg);
            f[p * 2] = numField(a, box, "Diameter, side or length", p == 0 ? 20 : 23, calc);
            f[p * 2 + 1] = numField(a, box, "Width (rectangles)", p == 0 ? 30 : 33, calc);
        }
        out[0] = result(a, box);
        box.addView(Ui.caption(a, "For the same depth. In a deeper pan bake a little cooler and longer; check early."));
        calc.run();
        V20.show(a, "Pan size converter", box, null, null);
    }

    static final Object[][] PRESETS = {
            {"Yeasted loaf", new String[]{"Mix and knead", "First rise", "Shape and second rise", "Bake", "Cool"}, new int[]{20, 90, 60, 40, 60}},
            {"Overnight sourdough", new String[]{"Mix and autolyse", "Bulk rise with folds", "Shape", "Cold proof in fridge", "Bake", "Cool"}, new int[]{60, 300, 20, 720, 45, 90}},
            {"Pizza dough", new String[]{"Mix and knead", "First rise", "Divide and rest", "Stretch, top and bake"}, new int[]{15, 120, 30, 15}}};

    static void timelineDialog(final MainActivity a) {
        final String[] opts = new String[PRESETS.length];
        for (int i = 0; i < PRESETS.length; i++) opts[i] = (String) PRESETS[i][0];
        a.dialog().setTitle("Dough timeline: pick a plan").setItems(opts, (d, i) -> timeline(a, i)).setNegativeButton("Close", null).show();
    }

    static void timeline(final MainActivity a, int preset) {
        final String[] names = (String[]) PRESETS[preset][1];
        final int[] mins = ((int[]) PRESETS[preset][2]).clone();
        final boolean h24 = prefs(a).getBoolean("clock24", false);
        LinearLayout box = V20.box(a);
        box.addView(Ui.caption(a, "Ready time and step lengths; the clock times work backwards. Rises depend on the room: watch the dough."));
        final EditText ready = numField(a, box, "Ready at (hour, 0\u201323)", 18, null);
        final TextView out = Ui.body(a, "");
        final Runnable calc = () -> {
            int end = (int) Math.round(val(ready, 18) * 60);
            int[] tl = Stage3.timeline(Stage3.startFor(end, mins), mins);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length; i++) sb.append(Stage3.clock(tl[i], h24)).append("  ").append(names[i]).append(" (").append(mins[i]).append(" min)\n");
            sb.append(Stage3.clock(tl[names.length], h24)).append("  Ready");
            out.setText(sb);
        };
        ready.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int x, int y, int z) {}
            public void onTextChanged(CharSequence s, int x, int y, int z) {}
            public void afterTextChanged(Editable s) { ready.removeCallbacks(calc); ready.postDelayed(calc, 120); }
        });
        for (int i = 0; i < names.length; i++) {
            final int ii = i;
            LinearLayout row = Ui.row(a);
            row.addView(Ui.body(a, names[i]), new LinearLayout.LayoutParams(0, -2, 1));
            ImageView minus = Ui.iconButton(a, R.drawable.ic_remove, Ui.TEXT, "Shorter " + names[i]);
            ImageView plus = Ui.iconButton(a, R.drawable.ic_add, Ui.TEXT, "Longer " + names[i]);
            minus.setOnClickListener(v -> { mins[ii] = Math.max(5, mins[ii] - step(mins[ii])); calc.run(); });
            plus.setOnClickListener(v -> { mins[ii] += step(mins[ii]); calc.run(); });
            row.addView(minus); row.addView(plus);
            box.addView(row);
        }
        out.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        box.addView(out, Ui.lpMargins(a, -1, -2, 0, 10, 0, 4));
        calc.run();
        V20.show(a, (String) PRESETS[preset][0], box, null, null);
    }
    static int step(int m) { return m >= 120 ? 30 : m >= 30 ? 10 : 5; }

    static void ovenDialog(final MainActivity a) {
        final Stage3 s = data(a);
        LinearLayout box = V20.box(a);
        final EditText[] f = new EditText[2];
        final TextView[] out = new TextView[2];
        Runnable calc = () -> {
            double c = val(f[0], 180), ft = val(f[1], 0);
            String g = Stage3.gasMark(c);
            out[0].setText(Math.round(c) + " \u00b0C = " + Math.round(Stage3.cToF(c)) + " \u00b0F" + (g.isEmpty() ? "" : " = gas " + g) + "\nFan: about " + Math.round(c - 20) + " \u00b0C (" + Math.round(Stage3.cToF(c - 20)) + " \u00b0F)");
            String[] r = s.altitudeFor(ft);
            out[1].setText(r == null ? "Below 3,000 ft most recipes work as written." : r[0] + "\u2013" + r[1] + " ft\n\u2022 Baking powder or soda: " + r[2] + " per teaspoon\n\u2022 Sugar: " + r[3] + " per cup\n\u2022 Liquid: " + r[4] + " per cup\n\u2022 Oven: " + r[5] + "\n\u2022 Time: " + r[6] + (r.length > 7 ? "\n" + r[7] : ""));
        };
        f[0] = numField(a, box, "Oven temperature (\u00b0C)", 180, calc);
        out[0] = result(a, box);
        f[1] = numField(a, box, "Your elevation (feet; metres \u00d7 3.28)", 0, calc);
        out[1] = Ui.body(a, "");
        box.addView(out[1], Ui.lpMargins(a, -1, -2, 0, 8, 0, 4));
        box.addView(Ui.caption(a, "Start with the smaller change and adjust after a test bake."));
        calc.run();
        V20.show(a, "Oven & high altitude", box, null, null);
    }

    static void partyDialog(final MainActivity a) {
        final Stage3 s = data(a);
        LinearLayout box = V20.box(a);
        final EditText[] g = new EditText[1];
        final TextView out = Ui.body(a, "");
        Runnable calc = () -> {
            int n = Math.max(1, (int) Math.round(val(g[0], 12)));
            StringBuilder sb = new StringBuilder();
            for (String[] p : s.party) sb.append(Stage3.partyNeed(p, n)).append(' ').append(p[2]).append(" \u00b7 ").append(p[0]).append(p.length > 3 && !p[3].isEmpty() ? "\n   " + p[3] : "").append('\n');
            out.setText(sb.toString().trim());
        };
        g[0] = numField(a, box, "Guests", 12, calc);
        out.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        box.addView(out, Ui.lpMargins(a, -1, -2, 0, 10, 0, 4));
        box.addView(Ui.caption(a, "If you serve several bakes, make less of each."));
        calc.run();
        V20.show(a, "Party calculator", box, null, null);
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Context c, String key) {
        try { Object o = Json.parse(prefs(c).getString(key, "[]")); if (o instanceof List) return (List<Object>) o; } catch (Exception e) { /* reset */ }
        return new ArrayList<Object>();
    }
    static void saveList(Context c, String key, List<Object> l, int max) {
        while (l.size() > max) l.remove(0);
        prefs(c).edit().putString(key, Json.write(l)).apply();
    }

    static void starterDialog(final MainActivity a) {
        final List<Object> feeds = list(a, "starter30");
        LinearLayout box = V20.box(a);
        final boolean h24 = prefs(a).getBoolean("clock24", false);
        StringBuilder sb = new StringBuilder();
        for (int i = feeds.size() - 1, n = 0; i >= 0 && n < 10; i--, n++) {
            Map<?, ?> m = (Map<?, ?>) feeds.get(i);
            sb.append(V20.date(a, String.valueOf(m.get("d")))).append(' ').append(Stage3.clock(((Number) m.get("m")).intValue(), h24)).append(" \u00b7 ").append(m.get("ratio"));
            Object note = m.get("r");
            if (note != null && !String.valueOf(note).isEmpty()) sb.append(" \u00b7 ").append(note);
            sb.append('\n');
        }
        box.addView(Ui.body(a, feeds.isEmpty() ? "No feeds logged yet." : "Recent feeds:\n" + sb.toString().trim()));
        final android.widget.Spinner ratio = new android.widget.Spinner(a);
        ratio.setAdapter(new android.widget.ArrayAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item, new String[]{"1:1:1", "1:2:2", "1:5:5"}));
        ratio.setContentDescription("Feed ratio");
        box.addView(Ui.caption(a, "Feed ratio (starter : flour : water)"), Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
        box.addView(ratio);
        final EditText note = V20.field(a, box, "Note (optional), e.g. doubled in 5 h", "", true);
        box.addView(Ui.caption(a, "At room temperature feed once or twice a day; in the fridge about once a week. Saved on this device only."));
        a.dialog().setTitle("Sourdough starter").setView(V20.wrap(a, box)).setPositiveButton("Log a feed now", (d, w) -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("d", Dates.today()); m.put("m", cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE));
            m.put("ratio", String.valueOf(ratio.getSelectedItem())); m.put("r", note.getText().toString().trim());
            feeds.add(m); saveList(a, "starter30", feeds, 60);
            a.toast("Feed logged");
        }).setNeutralButton("Clear history", (d, w) -> { prefs(a).edit().remove("starter30").apply(); a.toast("Cleared"); })
                .setNegativeButton("Close", null).show();
    }

    static void logDialog(final MainActivity a) {
        final List<Object> log = list(a, "bakeLog30");
        LinearLayout box = V20.box(a);
        final EditText what = V20.field(a, box, "What did you bake?", "", true);
        final EditText note = V20.field(a, box, "Notes (oven, timing, what to change)", "", true);
        final android.widget.RatingBar stars = new android.widget.RatingBar(a, null, android.R.attr.ratingBarStyle);
        stars.setNumStars(5); stars.setStepSize(1); stars.setContentDescription("How did it go, 1 to 5 stars");
        LinearLayout sw = Ui.row(a); sw.addView(stars, new LinearLayout.LayoutParams(-2, -2));
        box.addView(sw);
        StringBuilder sb = new StringBuilder();
        for (int i = log.size() - 1, n = 0; i >= 0 && n < 30; i--, n++) {
            Map<?, ?> m = (Map<?, ?>) log.get(i);
            int st = m.get("stars") instanceof Number ? ((Number) m.get("stars")).intValue() : 0;
            sb.append(V20.date(a, String.valueOf(m.get("d")))).append("  ").append(m.get("what")).append(st > 0 ? "  " + st + "\u2605" : "");
            Object nt = m.get("note");
            if (nt != null && !String.valueOf(nt).isEmpty()) sb.append("\n   ").append(nt);
            sb.append('\n');
        }
        box.addView(Ui.body(a, log.isEmpty() ? "Nothing logged yet. Saved on this device only." : sb.toString().trim()), Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
        a.dialog().setTitle("Bake log").setView(V20.wrap(a, box)).setPositiveButton("Save", (d, w) -> {
            String t = what.getText().toString().trim();
            if (t.isEmpty()) { a.toast("Type what you baked"); return; }
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("d", Dates.today()); m.put("what", t); m.put("note", note.getText().toString().trim()); m.put("stars", (int) stars.getRating());
            log.add(m); saveList(a, "bakeLog30", log, 300);
            a.toast("Saved");
        }).setNeutralButton(log.isEmpty() ? null : "Delete last", log.isEmpty() ? null : (android.content.DialogInterface.OnClickListener) (d, w) -> { log.remove(log.size() - 1); saveList(a, "bakeLog30", log, 300); a.toast("Deleted"); })
                .setNegativeButton("Close", null).show();
    }

    static void troubleDialog(final MainActivity a) {
        final Stage3 s = data(a);
        V20Helpers.searchList(a, "Troubleshooting", "What went wrong? e.g. dense, sank, soggy", null, (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            String ql = q.trim().toLowerCase(Locale.ROOT);
            for (Stage3.Trouble t : s.trouble) {
                String hay = (t.problem + " " + t.bakes).toLowerCase(Locale.ROOT);
                if (!ql.isEmpty() && !hay.contains(ql)) continue;
                StringBuilder sb = new StringBuilder(t.problem).append("  (").append(t.bakes).append(')');
                for (String f : t.fixes) sb.append("\n\u2022 ").append(f);
                out.add(sb.toString()); keys.add(null);
            }
            if (out.isEmpty()) { out.add("Nothing matches. Try: dense, sank, spread, soggy."); keys.add(null); }
            return out;
        }, null);
    }

    static void storageDialog(final MainActivity a) {
        final Stage3 s = data(a);
        V20Helpers.searchList(a, "How long bakes keep", "Search, e.g. bread, cheesecake", "Wrap well and label with the date. When in doubt, throw it out.", (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            String ql = q.trim().toLowerCase(Locale.ROOT);
            for (String[] r : s.storage) {
                if (!ql.isEmpty() && !r[0].toLowerCase(Locale.ROOT).contains(ql)) continue;
                out.add(r[0] + "\nRoom: " + r[1] + " \u00b7 Fridge: " + r[2] + " \u00b7 Freezer: " + r[3] + (r.length > 4 && !r[4].isEmpty() ? "\n" + r[4] : "")); keys.add(null);
            }
            if (out.isEmpty()) { out.add("Nothing matches."); keys.add(null); }
            return out;
        }, null);
    }

    static List<Recipe> sorted(List<Recipe> l) { List<Recipe> o = new ArrayList<Recipe>(l); Collections.sort(o, (x, y) -> x.title.compareToIgnoreCase(y.title)); return o; }

    // ---------------- backup (settings keys holidays, fasts, holLead, holBanner, starter, bakeLog; same on web) ----------------
    static void backupOut(SharedPreferences p, Map<String, Object> st) {
        if (p.contains("holidays30")) st.put("holidays", splitList(p.getString("holidays30", "")));
        if (p.contains("fasts30")) st.put("fasts", splitList(p.getString("fasts30", "")));
        if (p.contains("holLead30")) st.put("holLead", p.getInt("holLead30", 14));
        if (p.contains("holBanner30")) st.put("holBanner", p.getBoolean("holBanner30", true));
        if (p.contains("holNotify30")) st.put("holNotify", p.getBoolean("holNotify30", false));
        if (p.contains("fastChips30")) st.put("fastChips", p.getBoolean("fastChips30", false));
        try { if (p.contains("starter30")) st.put("starter", Json.parse(p.getString("starter30", "[]"))); } catch (RuntimeException e) { /* skip */ }
        try { if (p.contains("bakeLog30")) st.put("bakeLog", Json.parse(p.getString("bakeLog30", "[]"))); } catch (RuntimeException e) { /* skip */ }
    }
    static void backupIn(Map<?, ?> st, SharedPreferences.Editor ed) {
        if (st.get("holidays") instanceof List) ed.putString("holidays30", joinObj((List<?>) st.get("holidays")));
        if (st.get("fasts") instanceof List) ed.putString("fasts30", joinObj((List<?>) st.get("fasts")));
        if (st.get("holLead") instanceof Number) ed.putInt("holLead30", ((Number) st.get("holLead")).intValue());
        if (st.get("holBanner") instanceof Boolean) ed.putBoolean("holBanner30", (Boolean) st.get("holBanner"));
        if (st.get("holNotify") instanceof Boolean) ed.putBoolean("holNotify30", (Boolean) st.get("holNotify"));
        if (st.get("fastChips") instanceof Boolean) ed.putBoolean("fastChips30", (Boolean) st.get("fastChips"));
        if (st.get("starter") instanceof List) ed.putString("starter30", Json.write(st.get("starter")));
        if (st.get("bakeLog") instanceof List) ed.putString("bakeLog30", Json.write(st.get("bakeLog")));
    }
    static List<Object> splitList(String s) { List<Object> o = new ArrayList<Object>(); for (String x : s.split(",")) if (!x.isEmpty()) o.add(x); return o; }
    static String joinObj(List<?> l) { StringBuilder sb = new StringBuilder(); for (Object x : l) sb.append(sb.length() > 0 ? "," : "").append(x); return sb.toString(); }

    // ---------------- fasting labels as Discover diet chips (Stage 4) ----------------
    static final String FAST_PREFIX = "fast:";
    private static final java.util.WeakHashMap<Recipe, Integer> FAST_MEMO = new java.util.WeakHashMap<Recipe, Integer>();

    /** Chip ids for the ticked fasting labels when the "show as filter chips" option is on (off by default). */
    static List<String> fastChipIds(Context c) {
        List<String> out = new ArrayList<String>();
        if (!prefs(c).getBoolean("fastChips30", false)) return out;
        for (String id : fasts(c)) if (data(c).fast(id) != null) out.add(FAST_PREFIX + id);
        return out;
    }
    static String fastName(Context c, String chipId) {
        Stage3.Fast f = data(c).fast(chipId.substring(FAST_PREFIX.length()));
        return f == null ? chipId : f.label;
    }
    /** Memoised per recipe: bit i = fits fasting label i. */
    static synchronized boolean fitsFast(Context c, String id, Recipe r) {
        Stage3 s = data(c);
        int idx = -1;
        for (int i = 0; i < s.fasts.size(); i++) if (s.fasts.get(i).id.equals(id)) idx = i;
        if (idx < 0) return true;
        Integer m = FAST_MEMO.get(r);
        if (m == null) {
            int b = 0;
            for (int i = 0; i < s.fasts.size() && i < 31; i++) if (Stage3.fits(s.fasts.get(i), r)) b |= 1 << i;
            m = b;
            FAST_MEMO.put(r, m);
        }
        return (m & (1 << idx)) != 0;
    }
    /** Removes selected fasting chips that are no longer shown (label unticked or chips switched off). */
    static void dropStaleFastDiets(Context c, Set<String> diets) {
        boolean any = false;
        for (String d : diets) if (d.startsWith(FAST_PREFIX)) any = true;
        if (!any) return;
        List<String> ok = fastChipIds(c);
        java.util.Iterator<String> it = diets.iterator();
        boolean changed = false;
        while (it.hasNext()) { String d = it.next(); if (d.startsWith(FAST_PREFIX) && !ok.contains(d)) { it.remove(); changed = true; } }
        if (changed) prefs(c).edit().putStringSet("diets", new java.util.HashSet<String>(diets)).apply();
    }

    // ---------------- optional holiday notifications (Stage 4, off by default) ----------------
    /** Called from the daily V20Alarm tick: one notification per ticked holiday occurrence, once, when it enters the lead window. */
    static void notifyHolidays(Context c, android.app.NotificationManager nm) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("holNotify30", false)) return;
        String today = Dates.today(), done = p.getString("holNotified30", "");
        List<Stage3.Upcoming> up = data(c).upcoming(ticked(c), today, lead(c));
        Stage3.Upcoming u = null;
        for (Stage3.Upcoming x : up) if (!done.contains("|" + x.h.id + "@" + x.date)) { u = x; break; }
        if (u == null) return;
        if (done.length() > 600) done = done.substring(done.length() - 400);
        p.edit().putString("holNotified30", done + "|" + u.h.id + "@" + u.date).apply();
        android.content.Intent open = new android.content.Intent(c, MainActivity.class).putExtra("holiday", u.h.id).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(c, V20Alarm.ID + 1, open, android.app.PendingIntent.FLAG_UPDATE_CURRENT | V20Alarm.immutable());
        android.app.Notification.Builder b;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new android.app.NotificationChannel("holidays", "Holiday reminders", android.app.NotificationManager.IMPORTANCE_LOW));
            b = new android.app.Notification.Builder(c, "holidays");
        } else {
            b = new android.app.Notification.Builder(c);
        }
        b.setSmallIcon(R.drawable.ic_leaf)
                .setContentTitle(u.h.name + " " + when(u.days))
                .setContentText("Tap for recipes to plan ahead")
                .setContentIntent(pi)
                .setAutoCancel(true);
        try { nm.notify(V20Alarm.ID + 1, b.build()); } catch (SecurityException ignored) { }
    }
}
