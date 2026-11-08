package org.pantryrecipes.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.pantryrecipes.app.core.CategoryMoves;
import org.pantryrecipes.app.core.Catalog;
import org.pantryrecipes.app.core.Normalizer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v2.1/v2.2 (Android): moving an ingredient to the category the user prefers (quick-add sections and their counts follow; backed up
 * as settings.catMoves, same on the web).
 */
final class V21 {
    private V21() { }

    private static final String PREF = "catMoves21";
    private static Map<String, String> moves;

    static SharedPreferences prefs(Context c) { return V20.prefs(c); }

    static synchronized Map<String, String> moves(Context c) {
        if (moves == null) moves = CategoryMoves.parse(prefs(c).getString(PREF, ""));
        return moves;
    }

    /** Moves key to category (null or its own catalog category = back to the default). */
    static synchronized void setMove(Context c, String key, String category) {
        Map<String, String> m = new LinkedHashMap<String, String>(moves(c));
        int def = CategoryMoves.defaultCategory(key);
        if (category == null || CategoryMoves.categoryIndex(category) < 0 || CategoryMoves.categoryIndex(category) == def) m.remove(key);
        else m.put(key, category);
        m = CategoryMoves.sanitize(m);
        moves = m;
        prefs(c).edit().putString(PREF, CategoryMoves.format(m)).apply();
    }

    static String categoryOf(Context c, String key) {
        String m = moves(c).get(key);
        if (m != null) return m;
        int d = CategoryMoves.defaultCategory(key);
        return d < 0 ? null : Catalog.CATEGORIES[d];
    }

    /** Category row for the ingredient details dialog. */
    static void detailRows(final Activity a, LinearLayout box, final String key, final Runnable changed) {
        final String cur = categoryOf(a, key);
        TextView cl = Ui.sectionLabel(a, "Category");
        cl.setPadding(0, Ui.dp(a, 10), 0, 0);
        box.addView(cl);
        final TextView pick = Ui.button(a, (cur == null ? "Not in a category" : cur) + "  \u2013 change\u2026", R.drawable.ic_list, false);
        pick.setContentDescription("Category: " + (cur == null ? "none" : cur) + ". Change");
        pick.setOnClickListener(v -> chooseCategory(a, key, () -> {
            String now = categoryOf(a, key);
            pick.setText((now == null ? "Not in a category" : now) + "  \u2013 change\u2026");
            if (changed != null) changed.run();
        }));
        box.addView(pick, Ui.lpMargins(a, -2, Ui.dp(a, 48), 0, 4, 0, 0));
        box.addView(Ui.caption(a, "Where this ingredient shows in quick add. Your choice only; recipes are not changed."));
    }

    static void chooseCategory(final Activity a, final String key, final Runnable done) {
        final int def = CategoryMoves.defaultCategory(key);
        final String[] names = new String[Catalog.CATEGORIES.length];
        for (int i = 0; i < names.length; i++) names[i] = Catalog.CATEGORIES[i] + (i == def ? " (default)" : "");
        String cur = categoryOf(a, key);
        int sel = cur == null ? -1 : CategoryMoves.categoryIndex(cur);
        Extras.dialog(a).setTitle("Show in category")
                .setSingleChoiceItems(names, sel, (d, w) -> {
                    setMove(a, key, Catalog.CATEGORIES[w]);
                    d.dismiss();
                    done.run();
                })
                .setNegativeButton("Cancel", null).show();
    }

    static void settingsRow(final MainActivity a, LinearLayout box, final Runnable dismiss) {
        int n = moves(a).size();
        box.addView(V20.action(a, "Ingredient categories" + (n > 0 ? " (" + n + " moved)" : "") + "\u2026", R.drawable.ic_list,
                v -> { dismiss.run(); movesDialog(a); }), V20.rowLp(a));
    }

    static void movesDialog(final MainActivity a) {
        LinearLayout box = V20.box(a);
        final android.app.AlertDialog[] self = new android.app.AlertDialog[1];
        box.addView(Ui.caption(a, "Open an ingredient in your pantry and tap Category to move it, for example peppers to Fruits."));
        Map<String, String> m = moves(a);
        if (m.isEmpty()) box.addView(Ui.body(a, "No ingredients moved."));
        for (final Map.Entry<String, String> e : m.entrySet()) {
            int d = CategoryMoves.defaultCategory(e.getKey());
            String lab = labelFor(e.getKey());
            TextView row = V20.action(a, lab + ": " + e.getValue() + (d >= 0 ? " (default " + Catalog.CATEGORIES[d] + ") \u2013 undo" : " \u2013 undo"), R.drawable.ic_close, v -> {
                setMove(a, e.getKey(), null);
                a.pantryCategoriesChanged();
                if (self[0] != null) self[0].dismiss();
                movesDialog(a);
            });
            row.setContentDescription("Move " + lab + " back to its default category");
            box.addView(row, V20.rowLp(a));
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.addView(box);
        android.app.AlertDialog.Builder b = Extras.dialog(a).setTitle("Ingredient categories").setView(sv).setPositiveButton("Done", null);
        if (!m.isEmpty()) b.setNeutralButton("Reset all", (d, w) -> {
            synchronized (V21.class) { moves = new LinkedHashMap<String, String>(); }
            prefs(a).edit().remove(PREF).apply();
            a.pantryCategoriesChanged();
        });
        self[0] = b.show();
    }

    static String labelFor(String key) {
        for (org.pantryrecipes.app.core.PantryIndex.Item it : org.pantryrecipes.app.core.PantryIndex.get().items) if (it.key.equals(key)) return it.label;
        return Ui.cap(key);
    }

    // ---------------- backup (settings.catMoves; same on web) ----------------
    static void backupOut(Context c, Map<String, Object> st) {
        Map<String, String> m = moves(c);
        if (!m.isEmpty()) st.put("catMoves", new LinkedHashMap<String, Object>(m));
    }

    static void backupIn(Context c, Map<?, ?> st, SharedPreferences.Editor ed) {
        if (!(st.get("catMoves") instanceof Map)) return;
        Map<String, String> m = CategoryMoves.sanitize((Map<?, ?>) st.get("catMoves"));
        ed.putString(PREF, CategoryMoves.format(m));
        synchronized (V21.class) { moves = m; }
    }

    static String key(String label) { return Normalizer.normalize(label); }
}
