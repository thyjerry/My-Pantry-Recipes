package org.pantryrecipes.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.pantryrecipes.app.core.MedGuides;
import org.pantryrecipes.app.core.Nutrition;
import org.pantryrecipes.app.core.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * v2.0.1 (Android): Renal, Cardiac and Diabetic diet guides. Off by default; nothing is loaded until a guide is ticked.
 * Flags for the bundled recipes come precomputed from assets/medflags.txt (built by RecipeTool), so the Fire HD 8 only
 * reads a small text file the first time a guide is used. A guide, never medical advice: see MedGuides.DISCLAIMER.
 */
final class V201 {
    private V201() { }

    private static Map<String, String[]> pre;

    static synchronized Map<String, String[]> pre(Context c) {
        if (pre == null) pre = MedGuides.parse(V16.asset(c.getApplicationContext(), "medflags.txt"));
        return pre;
    }

    static SharedPreferences prefs(Context c) { return V20.prefs(c); }

    static List<String> guides(Context c) {
        List<String> out = new ArrayList<String>();
        for (String s : prefs(c).getString("medGuides201", "").split(",")) if (MedGuides.IDS.contains(s) && !out.contains(s)) out.add(s);
        return out;
    }
    static int sodiumLimit(Context c) { return MedGuides.clampSodium(prefs(c).getInt("medNa201", 0)); }
    static int carbLimit(Context c) { return MedGuides.clampCarbs(prefs(c).getInt("medCarb201", 0)); }

    static void settingsRow(final MainActivity a, LinearLayout box, final Runnable dismiss) {
        box.addView(V20.action(a, "Medical diet guides (renal, cardiac, diabetic)\u2026", R.drawable.ic_warning, v -> { dismiss.run(); dialog(a); }), V20.rowLp(a));
    }

    static TextView disclaimer(Context c) {
        TextView t = Ui.text(c, MedGuides.DISCLAIMER, 14, Ui.AMBER_TEXT, Ui.MEDIUM);
        t.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 4));
        return t;
    }

    static void dialog(final MainActivity a) {
        LinearLayout box = V20.box(a);
        final android.app.AlertDialog[] self = new android.app.AlertDialog[1];
        box.addView(disclaimer(a));
        box.addView(Ui.caption(a, "Tick a guide to see, on each recipe page, ingredients that are often limited on that diet. Off by default."));
        for (final String g : MedGuides.IDS) {
            final android.widget.CheckBox cb = V20.check(a, MedGuides.name(g), guides(a).contains(g));
            cb.setOnCheckedChangeListener((b, on) -> {
                List<String> l = guides(a);
                if (on && !l.contains(g)) l.add(g);
                if (!on) l.remove(g);
                prefs(a).edit().putString("medGuides201", V30.join(l)).apply();
            });
            box.addView(cb);
            box.addView(Ui.caption(a, MedGuides.RULES.get(g)));
            box.addView(V20.action(a, "Recipes with no " + shortName(g) + " flags\u2026", R.drawable.ic_list, v -> {
                self[0].dismiss();
                List<Recipe> rs = new ArrayList<Recipe>();
                Map<String, String[]> p = pre(a);
                for (Recipe r : a.repo.all()) if (MedGuides.noFlags(MedGuides.flags(r, p), g)) rs.add(r);
                a.openRecipeList("No " + shortName(g) + " flags", rs, false,
                        "No flagged ingredient names were found. That does not mean a recipe suits you: amounts and hidden ingredients are not checked. " + MedGuides.DISCLAIMER);
            }), V20.rowLp(a));
        }
        box.addView(Ui.caption(a, "Your limits (optional, stored only on this device and in your backups; leave 0 for none):"), Ui.lpMargins(a, -1, -2, 0, 12, 0, 0));
        final EditText na = limitField(a, box, "Sodium, mg per meal", sodiumLimit(a));
        final EditText cbf = limitField(a, box, "Carbs, g per meal", carbLimit(a));
        box.addView(Ui.caption(a, MedGuides.DATA_NOTE), Ui.lpMargins(a, -1, -2, 0, 12, 0, 0));
        self[0] = V20.show(a, "Medical diet guides", box, "Save limits", () -> {
            prefs(a).edit().putInt("medNa201", MedGuides.clampSodium(Math.round(V30.val(na, 0))))
                    .putInt("medCarb201", MedGuides.clampCarbs(Math.round(V30.val(cbf, 0)))).apply();
            a.toast("Limits saved");
        });
    }

    static String shortName(String g) { return MedGuides.RENAL.equals(g) ? "renal" : MedGuides.CARDIAC.equals(g) ? "cardiac" : "diabetic"; }

    static EditText limitField(Context c, LinearLayout box, String label, int val) {
        box.addView(Ui.caption(c, label), Ui.lpMargins(c, -1, -2, 0, 8, 0, 0));
        EditText e = new EditText(c);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSingleLine(true);
        e.setMinHeight(Ui.dp(c, 48));
        e.setText(String.valueOf(val));
        e.setContentDescription(label);
        box.addView(e);
        return e;
    }

    /** The guide card for a recipe page, or null when no guide is ticked. */
    static View detailCard(MainActivity a, Recipe r) {
        List<String> gs = guides(a);
        if (gs.isEmpty()) return null;
        LinearLayout col = Ui.column(a);
        int[][] f = MedGuides.flags(r, pre(a));
        for (String g : gs) {
            List<String> l = MedGuides.lines(r, f, g);
            TextView h = Ui.text(a, MedGuides.name(g) + (l.isEmpty() ? ": no flagged ingredient names (amounts not checked)" : ":"), 14, Ui.TEXT, Ui.MEDIUM);
            col.addView(h, Ui.lpMargins(a, -1, -2, 0, 6, 0, 0));
            for (String s : l) col.addView(Ui.body(a, "\u2022 " + s));
        }
        Nutrition.Estimate e = Nutrition.estimate(r, V16.foods(a));
        for (String s : MedGuides.estimateLines(r, e.show() ? e.carbs : Double.NaN, gs, sodiumLimit(a), carbLimit(a))) col.addView(Ui.caption(a, s));
        col.addView(disclaimer(a), Ui.lpMargins(a, -1, -2, 0, 6, 0, 0));
        col.addView(Ui.caption(a, "Values are estimates; potassium and phosphorus data are incomplete."));
        return col;
    }

    // ---------------- backup (settings keys medGuides, medSodiumMg, medCarbsG; same on web) ----------------
    static void backupOut(SharedPreferences p, Map<String, Object> st) {
        if (p.contains("medGuides201")) {
            List<Object> l = new ArrayList<Object>();
            for (String s : p.getString("medGuides201", "").split(",")) if (MedGuides.IDS.contains(s) && !l.contains(s)) l.add(s);
            st.put("medGuides", l);
        }
        if (p.contains("medNa201")) st.put("medSodiumMg", MedGuides.clampSodium(p.getInt("medNa201", 0)));
        if (p.contains("medCarb201")) st.put("medCarbsG", MedGuides.clampCarbs(p.getInt("medCarb201", 0)));
    }
    static void backupIn(Map<?, ?> st, SharedPreferences.Editor ed) {
        if (st.get("medGuides") instanceof List) ed.putString("medGuides201", V30.joinObj((List<?>) st.get("medGuides")));
        if (st.get("medSodiumMg") instanceof Number) ed.putInt("medNa201", MedGuides.clampSodium(((Number) st.get("medSodiumMg")).longValue()));
        if (st.get("medCarbsG") instanceof Number) ed.putInt("medCarb201", MedGuides.clampCarbs(((Number) st.get("medCarbsG")).longValue()));
    }
}
