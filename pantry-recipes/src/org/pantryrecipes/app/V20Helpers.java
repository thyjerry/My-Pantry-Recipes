package org.pantryrecipes.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.pantryrecipes.app.core.Helpers20;
import org.pantryrecipes.app.core.Normalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v2.0 Stage 2 kitchen helpers (Android): equivalents, Rescue my dish, spice blend builder, cooking times, ingredient
 * explainers with local names. The data (assets/helpers20.txt) is parsed on first open only; lists are recycled
 * ListViews so the Fire HD 8 never inflates more than a screenful of rows.
 */
final class V20Helpers {
    private V20Helpers() {}

    private static Helpers20 data;

    static synchronized Helpers20 data(Context c) {
        if (data == null) data = Helpers20.parse(V16.asset(c.getApplicationContext(), "helpers20.txt"));
        return data;
    }

    static void hub(final MainActivity a) {
        LinearLayout box = V20.box(a);
        final AlertDialog[] self = new AlertDialog[1];
        box.addView(Ui.caption(a, "Quick kitchen answers. Everything here works offline."));
        box.addView(V20.action(a, "Equivalents table\u2026", R.drawable.ic_swap, v -> { self[0].dismiss(); equivalents(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Rescue my dish\u2026", R.drawable.ic_warning, v -> { self[0].dismiss(); rescue(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Spice blend builder\u2026", R.drawable.ic_leaf, v -> { self[0].dismiss(); blends(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Cooking times\u2026", R.drawable.ic_timer, v -> { self[0].dismiss(); times(a); }), V20.rowLp(a));
        box.addView(V20.action(a, "Ingredient explainers & local names\u2026", R.drawable.ic_list, v -> { self[0].dismiss(); explainers(a); }), V20.rowLp(a));
        self[0] = V20.show(a, "Kitchen helpers", box, null, null);
    }

    // ---------------- shared searchable list ----------------
    interface Rows { List<CharSequence> rows(String query, List<Object> keys); }
    interface Pick { void pick(Object key); }

    private static final class Adapter extends BaseAdapter {
        final Context c; List<CharSequence> items = new ArrayList<CharSequence>();
        Adapter(Context c) { this.c = c; }
        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }
        public View getView(int i, View v, ViewGroup p) {
            TextView t = v instanceof TextView ? (TextView) v : Ui.body(c, "");
            if (v == null) t.setPadding(Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8));
            t.setText(items.get(i));
            return t;
        }
    }

    static AlertDialog searchList(final MainActivity a, String title, String hint, String caption, final Rows rows, final Pick pick) {
        LinearLayout box = Ui.column(a);
        box.setPadding(Ui.dp(a, 20), Ui.dp(a, 8), Ui.dp(a, 20), 0);
        final EditText q = new EditText(a);
        q.setHint(hint); q.setSingleLine(true);
        box.addView(q);
        if (caption != null) box.addView(Ui.caption(a, caption));
        final ListView lv = new ListView(a);
        lv.setDivider(null);
        final Adapter ad = new Adapter(a);
        final List<Object> keys = new ArrayList<Object>();
        ad.items = rows.rows("", keys);
        lv.setAdapter(ad);
        box.addView(lv, new LinearLayout.LayoutParams(-1, Ui.dp(a, 360)));
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            final Runnable apply = () -> { keys.clear(); ad.items = rows.rows(q.getText().toString(), keys); ad.notifyDataSetChanged(); lv.setSelection(0); };
            public void afterTextChanged(Editable s) { lv.removeCallbacks(apply); lv.postDelayed(apply, 150); } // debounce: one filter per pause (HD8)
        });
        if (pick != null) lv.setOnItemClickListener((p, v, i, id) -> { if (i < keys.size() && keys.get(i) != null) pick.pick(keys.get(i)); });
        return a.dialog().setTitle(title).setView(box).setNegativeButton("Close", null).show();
    }

    private static CharSequence row(String head, String main, String sub) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (head != null && !head.isEmpty()) {
            int s0 = sb.length(); sb.append(head).append('\n');
            sb.setSpan(new ForegroundColorSpan(Ui.TEXT2), s0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.85f), s0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int b0 = sb.length(); sb.append(main);
        int dash = main.indexOf(" = ");
        sb.setSpan(new StyleSpan(Typeface.BOLD), b0, b0 + (dash > 0 ? dash : main.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (sub != null && !sub.isEmpty()) {
            int s1 = sb.length(); sb.append('\n').append(sub);
            sb.setSpan(new ForegroundColorSpan(Ui.TEXT2), s1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    private static CharSequence header(String g) {
        SpannableStringBuilder sb = new SpannableStringBuilder(g.toUpperCase(java.util.Locale.ROOT));
        sb.setSpan(new ForegroundColorSpan(Ui.GREEN_DARK), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(0.8f), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    // ---------------- equivalents ----------------
    static void equivalents(final MainActivity a) {
        final Helpers20 h = data(a);
        searchList(a, "Equivalents (" + h.equivs.size() + ")", "Search, e.g. butter, flour cup, 180 C", "Weights are averages. Use a scale for baking when you can.", (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            String g = null;
            for (Helpers20.Equiv e : h.findEquivs(q)) {
                if (!e.group.equals(g)) { g = e.group; out.add(header(g)); keys.add(null); }
                out.add(row(null, e.amount + " = " + e.equals, e.note)); keys.add(e);
            }
            if (out.isEmpty()) { out.add("Nothing matches. Try one word."); keys.add(null); }
            return out;
        }, null);
    }

    // ---------------- cooking times ----------------
    static void times(final MainActivity a) {
        final Helpers20 h = data(a);
        searchList(a, "Cooking times", "Search, e.g. salmon, rice, instant pot", "Times are a starting point. Check doneness, and use a thermometer for meat.", (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            String g = null;
            for (Helpers20.Time t : h.findTimes(q)) {
                if (!t.group.equals(g)) { g = t.group; out.add(header(g)); keys.add(null); }
                out.add(row(null, t.item + " = " + t.time, t.method + (t.note.isEmpty() ? "" : " \u00b7 " + t.note))); keys.add(t);
            }
            if (out.isEmpty()) { out.add("Nothing matches. Try one word."); keys.add(null); }
            return out;
        }, null);
    }

    // ---------------- rescue my dish ----------------
    static void rescue(final MainActivity a) {
        final Helpers20 h = data(a);
        searchList(a, "Rescue my dish", "What went wrong? e.g. salty, thin, split", null, (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            for (Helpers20.Rescue r : h.findRescues(q)) { out.add(row(null, r.problem, r.dishes)); keys.add(r); }
            if (out.isEmpty()) { out.add("Nothing matches. Try: salty, spicy, thin, burnt, dry."); keys.add(null); }
            return out;
        }, k -> {
            Helpers20.Rescue r = (Helpers20.Rescue) k;
            LinearLayout box = V20.box(a);
            box.addView(Ui.caption(a, r.dishes));
            box.addView(Ui.sectionLabel(a, "Try this"), Ui.lpMargins(a, -2, -2, 0, 10, 0, 4));
            for (String f : r.fixes) box.addView(Ui.body(a, "\u2022 " + f), Ui.lpMargins(a, -1, -2, 0, 2, 0, 4));
            if (!r.prevent.isEmpty()) {
                box.addView(Ui.sectionLabel(a, "Next time"), Ui.lpMargins(a, -2, -2, 0, 10, 0, 4));
                box.addView(Ui.body(a, r.prevent));
            }
            V20.show(a, r.problem, box, null, null);
        });
    }

    // ---------------- ingredient explainers ----------------
    static void explainers(final MainActivity a) {
        final Helpers20 h = data(a);
        final String reg = V20.region(a);
        searchList(a, "Ingredients explained", "Search any name, e.g. courgette, maida", "Names differ by country. Your region's name is shown first.", (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            for (Helpers20.Explain e : h.findExplains(q)) {
                String local = Helpers20.localName(e, reg);
                out.add(row(null, e.name, local != null && !local.equalsIgnoreCase(e.name) ? "Here: " + local : null)); keys.add(e);
            }
            if (out.isEmpty()) { out.add("Nothing matches yet."); keys.add(null); }
            return out;
        }, k -> {
            Helpers20.Explain e = (Helpers20.Explain) k;
            LinearLayout box = V20.box(a);
            box.addView(Ui.body(a, e.text));
            box.addView(Ui.sectionLabel(a, "Local names"), Ui.lpMargins(a, -2, -2, 0, 12, 0, 4));
            for (Map.Entry<String, String> n : e.names.entrySet()) {
                boolean mine = n.getKey().equals(reg) || (Helpers20.localName(e, reg) != null && Helpers20.localName(e, reg).equals(n.getValue()) && !n.getKey().equals("also"));
                TextView t = Ui.body(a, Helpers20.regionLabel(n.getKey()) + ": " + n.getValue() + (mine ? "  \u2190 your region" : ""));
                if (mine) t.setTypeface(Ui.MEDIUM);
                box.addView(t, Ui.lpMargins(a, -1, -2, 0, 2, 0, 2));
            }
            V20.show(a, e.name, box, null, null);
        });
    }

    // ---------------- spice blend builder ----------------
    static void blends(final MainActivity a) {
        final Helpers20 h = data(a);
        searchList(a, "Spice blend builder", "Search, e.g. curry, BBQ, Moroccan", "Make your own blends from spices you have. Tap one to size and adjust it.", (q, keys) -> {
            List<CharSequence> out = new ArrayList<CharSequence>();
            for (Helpers20.Blend b : h.blends) if (Helpers20.matches(b.name + " " + b.country + " " + b.use, q)) { out.add(row(null, b.name, b.country + " \u00b7 " + b.use)); keys.add(b); }
            if (out.isEmpty()) { out.add("Nothing matches."); keys.add(null); }
            return out;
        }, k -> builder(a, (Helpers20.Blend) k));
    }

    static final double[] BATCH_TSP = {3, 6, 12, 24};
    static final String[] BATCH_LABEL = {"1 tbsp", "2 tbsp", "1/4 cup", "1/2 cup"};

    static void builder(final MainActivity a, final Helpers20.Blend b) {
        final int n = b.parts.size();
        final double[] parts = new double[n];
        for (int i = 0; i < n; i++) parts[i] = b.parts.get(i).parts;
        final int[] batch = {1};
        final Set<String> have = new HashSet<String>(Store.get(a).pantryKeys(true));
        have.addAll(Store.get(a).pantryKeys(false));
        final LinearLayout box = V20.box(a);
        box.addView(Ui.caption(a, b.country + " \u00b7 " + b.use));
        final LinearLayout chips = Ui.row(a);
        box.addView(chips, Ui.lpMargins(a, -1, -2, 0, 8, 0, 4));
        final CheckBox noSalt = V20.check(a, "Salt-free", false), mild = V20.check(a, "Milder (half the chilli)", false);
        box.addView(noSalt); box.addView(mild);
        final LinearLayout list = Ui.column(a);
        box.addView(list);
        final Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            chips.removeAllViews();
            for (int i = 0; i < BATCH_TSP.length; i++) {
                final int idx = i;
                TextView c = Ui.chip(a, BATCH_LABEL[i], batch[0] == i);
                c.setOnClickListener(v -> { batch[0] = idx; redraw[0].run(); });
                chips.addView(c, Ui.lpMargins(a, -2, -2, 0, 0, 6, 0));
            }
            list.removeAllViews();
            List<String[]> rows = Helpers20.build(b, parts, BATCH_TSP[batch[0]], noSalt.isChecked(), mild.isChecked());
            int ri = 0;
            for (int i = 0; i < n; i++) {
                final int idx = i;
                Helpers20.Part p = b.parts.get(i);
                boolean dropped = parts[i] <= 0 || (noSalt.isChecked() && "salt".equals(p.flag));
                String amt = dropped ? "left out" : rows.get(ri++)[0];
                boolean got = have.contains(Normalizer.normalize(p.spice));
                LinearLayout r = Ui.row(a);
                TextView t = Ui.body(a, amt + "  " + p.spice + ("opt".equals(p.flag) ? " (optional)" : "") + (got ? "  \u2713" : ""));
                r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
                TextView minus = Ui.chip(a, "\u2212", false), plus = Ui.chip(a, "+", false);
                minus.setContentDescription("Less " + p.spice); plus.setContentDescription("More " + p.spice);
                minus.setOnClickListener(v -> { parts[idx] = Math.max(0, parts[idx] - Math.max(0.25, parts[idx] >= 2 ? 0.5 : 0.25)); redraw[0].run(); });
                plus.setOnClickListener(v -> { parts[idx] = parts[idx] + (parts[idx] >= 2 ? 0.5 : 0.25); redraw[0].run(); });
                r.addView(minus, Ui.lpMargins(a, -2, -2, 4, 0, 4, 0));
                r.addView(plus);
                list.addView(r, Ui.lpMargins(a, -1, -2, 0, 4, 0, 4));
            }
            list.addView(Ui.caption(a, "\u2713 = in your pantry. Use \u2212 and + to change a spice's share."));
        };
        noSalt.setOnCheckedChangeListener((x, on) -> redraw[0].run());
        mild.setOnCheckedChangeListener((x, on) -> redraw[0].run());
        redraw[0].run();
        box.addView(V20.action(a, "Add missing spices to shopping", R.drawable.ic_cart, v -> {
            int k = 0;
            for (int i = 0; i < n; i++) {
                Helpers20.Part p = b.parts.get(i);
                if (parts[i] <= 0 || "opt".equals(p.flag) || (noSalt.isChecked() && "salt".equals(p.flag))) continue;
                if (!have.contains(Normalizer.normalize(p.spice)) && Store.get(a).addShopping(p.spice, Double.NaN, "", b.name)) k++;
            }
            Ui.toastShort(a, k == 0 ? "You have everything" : k + " added to shopping");
            a.refreshShopping();
        }), V20.rowLp(a));
        V20.show(a, b.name, box, "Copy", () -> {
            String txt = Helpers20.blendText(b, Helpers20.build(b, parts, BATCH_TSP[batch[0]], noSalt.isChecked(), mild.isChecked()), BATCH_TSP[batch[0]]);
            android.content.ClipboardManager cm = (android.content.ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) { cm.setPrimaryClip(android.content.ClipData.newPlainText(b.name, txt)); Ui.toastShort(a, "Copied"); }
        });
    }
}
