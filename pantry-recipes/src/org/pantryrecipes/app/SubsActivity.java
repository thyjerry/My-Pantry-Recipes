package org.pantryrecipes.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.pantryrecipes.app.core.Matcher;
import org.pantryrecipes.app.core.Substitutions;

import java.util.ArrayList;
import java.util.List;

/** Searchable list of the bundled ingredient substitutions, grouped by category. */
public class SubsActivity extends Activity implements Repo.Listener {
    @Override
    protected void attachBaseContext(android.content.Context base) { super.attachBaseContext(Ui.fontContext(base)); }

    Repo repo;
    Store store;
    Matcher matcher;
    String query = "";
    String category = "";
    final List<Object> rows = new ArrayList<Object>();
    BaseAdapter adapter;
    TextView count;
    LinearLayout catRow;

    int dp(float v) { return Ui.dp(this, v); }

    @Override
    protected void onCreate(Bundle saved) {
        Ui.themeActivity(this);
        super.onCreate(saved);
        Ui.styleWindow(this);
        repo = Repo.get(this);
        store = Store.get(this);
        String q = getIntent().getStringExtra("q");
        if (q != null) query = q;
        if (saved != null) {
            query = saved.getString("q", query);
            category = saved.getString("c", "");
        }
        repo.addListener(this);
        build();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("q", query);
        out.putString("c", category);
    }

    @Override
    protected void onDestroy() {
        repo.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onRepoChanged() { refresh(); }

    void build() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        LinearLayout bar = Ui.row(this);
        bar.setPadding(dp(6), dp(8), dp(12), dp(4));
        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Ui.TEXT, "Back");
        back.setOnClickListener(v -> finish());
        bar.addView(back);
        LinearLayout titles = Ui.column(this);
        titles.setPadding(dp(6), 0, 0, 0);
        titles.addView(Ui.title(this, "Substitutions", 20));
        count = Ui.caption(this, "");
        titles.addView(count);
        bar.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(Ui.icon(this, R.drawable.ic_swap, Ui.GREEN, 26));
        root.addView(bar);

        LinearLayout sr = Ui.row(this);
        sr.setPadding(dp(14), 0, dp(2), 0);
        sr.setBackground(Ui.outlined(Ui.CARD, Ui.LINE, dp(1), dp(24)));
        sr.addView(Ui.icon(this, R.drawable.ic_search, Ui.TEXT3, 20));
        final EditText search = new EditText(this);
        search.setHint("Search, e.g. buttermilk, egg, wine");
        search.setText(query);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.TEXT3);
        search.setBackground(null);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(10), 0, dp(4), 0);
        sr.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1));
        final ImageView clr = Ui.iconButton(this, R.drawable.ic_close, Ui.TEXT2, "Clear search");
        clr.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
        clr.setOnClickListener(v -> search.setText(""));
        sr.addView(clr);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) {
                query = e.toString().trim();
                clr.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                refresh();
            }
        });
        search.setOnEditorActionListener((v, a, e) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            return true;
        });
        root.addView(sr, Ui.lpMargins(this, -1, -2, 16, 4, 16, 8));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        hs.setPadding(dp(16), 0, dp(16), 0);
        catRow = Ui.row(this);
        hs.addView(catRow);
        root.addView(hs, Ui.lpMargins(this, -1, -2, 0, 0, 0, 4));

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setSelector(new android.graphics.drawable.ColorDrawable(0));
        list.setClipToPadding(false);
        list.setPadding(dp(12), dp(4), dp(12), dp(16));
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int p) { return rows.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public boolean isEnabled(int p) { return false; }
            @Override public int getViewTypeCount() { return 3; }
            @Override public int getItemViewType(int p) {
                Object o = rows.get(p);
                return o instanceof Substitutions.Entry ? 0 : o instanceof String ? 1 : 2;
            }
            @Override
            public View getView(int p, View cv, ViewGroup parent) {
                Object o = rows.get(p);
                if (o instanceof Substitutions.Entry) return entryView((Substitutions.Entry) o);
                if (o instanceof String) {
                    TextView h = Ui.sectionLabel(SubsActivity.this, (String) o);
                    h.setPadding(dp(8), dp(16), dp(8), dp(6));
                    return h;
                }
                TextView e = Ui.caption(SubsActivity.this, "No substitutions match \u201c" + query + "\u201d.");
                e.setGravity(Gravity.CENTER);
                e.setPadding(dp(16), dp(40), dp(16), dp(16));
                return e;
            }
        };
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        refresh();
    }

    void buildCats(Substitutions subs) {
        catRow.removeAllViews();
        List<String> cats = new ArrayList<String>();
        cats.add("");
        cats.addAll(subs.categories);
        for (final String c : cats) {
            TextView chip = Ui.chip(this, c.isEmpty() ? "All" : c, c.equals(category));
            chip.setOnClickListener(v -> { category = c; refresh(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
            lp.rightMargin = dp(8);
            catRow.addView(chip, lp);
        }
    }

    void refresh() {
        Substitutions subs = repo.substitutions();
        matcher = new Matcher(store.pantryKeys(false), store.pantryKeys(true)).withSwapAvoid(store.dislikes(), store.avoidAllergens(), store.prefs.getStringSet("diets", new java.util.HashSet<String>()));
        buildCats(subs);
        rows.clear();
        String last = null;
        int n = 0, have = 0;
        for (Substitutions.Entry e : subs.search(query)) {
            if (!category.isEmpty() && !category.equals(e.category)) continue;
            if (!e.category.equals(last)) { rows.add(e.category); last = e.category; }
            rows.add(e);
            n++;
            for (Substitutions.Option o : e.options) if (o.available(matcher) && matcher.optionAllowed(o)) { have++; break; }
        }
        if (n == 0) rows.add(Boolean.FALSE);
        count.setText(n + (n == 1 ? " ingredient" : " ingredients") + " \u00b7 " + have + " you can swap with your pantry");
        adapter.notifyDataSetChanged();
    }

    View entryView(Substitutions.Entry e) {
        LinearLayout card = Ui.card(this);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout head = Ui.row(this);
        head.addView(Ui.title(this, "Instead of " + e.name.toLowerCase(java.util.Locale.US), 16), new LinearLayout.LayoutParams(0, -2, 1));
        if (matcher.has(e.keys.iterator().next())) head.addView(Ui.text(this, "in pantry", 12, Ui.GREEN_DARK, Ui.MEDIUM));
        card.addView(head);
        if (!e.aliases.isEmpty()) {
            TextView a = Ui.caption(this, "Also: " + android.text.TextUtils.join(", ", e.aliases));
            a.setPadding(0, dp(1), 0, 0);
            card.addView(a);
        }
        for (Substitutions.Option o : e.options) {
            String caution = org.pantryrecipes.app.core.SwapAdvice.caution(o, store.dislikes(), store.avoidAllergens(), store.prefs.getStringSet("diets", new java.util.HashSet<String>()));
            boolean ok = o.available(matcher) && caution.isEmpty();
            LinearLayout r = Ui.row(this);
            r.setGravity(Gravity.TOP);
            r.setPadding(0, dp(8), 0, 0);
            ImageView mk = Ui.icon(this, ok ? R.drawable.ic_check : R.drawable.ic_swap, ok ? Ui.GREEN : Ui.TEXT3, 18);
            r.addView(mk);
            LinearLayout col = Ui.column(this);
            col.setPadding(dp(10), 0, 0, 0);
            col.addView(Ui.body(this, o.text));
            if (!caution.isEmpty()) col.addView(Ui.text(this, caution, 13, Ui.DANGER, Ui.MEDIUM));
            String det = V17.swapDetail(o);
            if (!det.isEmpty()) {
                TextView dt = Ui.caption(this, det);
                dt.setPadding(0, dp(2), 0, 0);
                col.addView(dt);
            }
            if (ok) {
                TextView y = Ui.text(this, "You have: " + o.usesLabel(matcher), 12, Ui.GREEN_DARK, Ui.MEDIUM);
                col.addView(y);
            }
            r.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(r);
        }
        if (!e.note.isEmpty()) {
            TextView nt = Ui.caption(this, "Tip: " + e.note);
            nt.setPadding(0, dp(8), 0, 0);
            card.addView(nt);
        }
        LinearLayout w = Ui.column(this);
        w.setPadding(dp(4), dp(4), dp(4), dp(4));
        w.addView(card);
        return w;
    }
}
