package org.pantryrecipes.app;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.pantryrecipes.app.core.BuyNext;
import org.pantryrecipes.app.core.Cost;
import org.pantryrecipes.app.core.Matcher;
import org.pantryrecipes.app.core.Normalizer;
import org.pantryrecipes.app.core.Recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v2.1 "Buy these next": tap a suggestion to expand it (why, the recipes it unlocks, an estimated price, add to
 * the shopping list); "See all" lists every missing ingredient of the recipes 1-2 items away with two sorts.
 * HD8: the expanded parts are built only on tap, and the full list is ranked only when "See all" opens.
 */
final class BuyNextUi {
    private BuyNextUi() { }

    static final int SHOW_RECIPES = 5;
    private static final Set<String> expanded = new HashSet<String>();

    static View card(final MainActivity a, final List<Matcher.Result> results) {
        final List<BuyNext.Pick> picks = BuyNext.fromResults(results, BuyNext.MAX);
        if (picks.isEmpty()) return null;
        LinearLayout card = Ui.card(a);
        card.addView(V16.head(a, "Buy these next", null));
        card.addView(Ui.caption(a, "The " + (picks.size() == 1 ? "item" : picks.size() + " items")
                + " that unlock the most recipes you're 1\u20132 ingredients away from (your diets, allergies, dislikes and substitutes count). Tap one for details."),
                Ui.lpMargins(a, -1, -2, 0, 2, 0, 8));
        for (int i = 0; i < picks.size(); i++) {
            final BuyNext.Pick p = picks.get(i);
            final int idx = i;
            addRow(a, card, p.key, p.name, "+" + V16.plural(p.unlocks.size(), "recipe", "recipes") + " you can make", p.unlocks.size(), new Detail() {
                @Override public String why() { return BuyNext.whyPick(p, idx, findOption(results, p.key)); }
                @Override public List<String> ids() { return p.unlocks; }
                @Override public String idsTitle() { return idx == 0 ? "Recipes it unlocks" : "Recipes it unlocks with the ones above"; }
            });
        }
        TextView all = Ui.button(a, "See all suggestions\u2026", R.drawable.ic_list, false);
        all.setContentDescription("See all suggestions, with sort options");
        all.setOnClickListener(v -> seeAll(a, results));
        card.addView(all, Ui.lpMargins(a, -2, Ui.dp(a, 48), 0, 4, 0, 0));
        return card;
    }

    interface Detail { String why(); List<String> ids(); String idsTitle(); }

    static BuyNext.Option findOption(List<Matcher.Result> results, String key) {
        for (BuyNext.Option o : BuyNext.rankedFromResults(results)) if (o.key.equals(key)) return o;
        return null;
    }

    /** One suggestion row; the details are built on first expand. */
    static void addRow(final MainActivity a, final LinearLayout parent, final String key, final String name, String sub, int n, final Detail d) {
        final LinearLayout wrap = Ui.column(a);
        LinearLayout row = Ui.row(a);
        row.setMinimumHeight(Ui.dp(a, 48));
        row.setPadding(Ui.dp(a, 6), Ui.dp(a, 8), Ui.dp(a, 6), Ui.dp(a, 8));
        row.setBackground(Ui.ripple(Ui.rounded(Ui.SELECTED, Ui.dp(a, 12)), Ui.dp(a, 12)));
        row.addView(Ui.icon(a, R.drawable.ic_cart, Ui.GREEN_DARK, 20));
        LinearLayout tc = Ui.column(a);
        tc.setPadding(Ui.dp(a, 12), 0, 0, 0);
        tc.addView(Ui.text(a, Ui.cap(name), 15, Ui.TEXT, Ui.MEDIUM));
        tc.addView(Ui.caption(a, sub));
        row.addView(tc, new LinearLayout.LayoutParams(0, -2, 1));
        final android.widget.ImageView chev = Ui.icon(a, R.drawable.ic_expand, Ui.TEXT3, 18);
        row.addView(chev);
        wrap.addView(row);
        final View[] body = new View[1];
        final boolean open = expanded.contains(key);
        final Runnable desc = () -> row.setContentDescription(name + ", " + n + " recipes. " + (expanded.contains(key) ? "Expanded. Tap to collapse." : "Tap for details."));
        desc.run();
        row.setOnClickListener(v -> {
            boolean now = !expanded.contains(key);
            if (now) expanded.add(key); else expanded.remove(key);
            if (now && body[0] == null) { body[0] = details(a, key, name, d); wrap.addView(body[0]); }
            if (body[0] != null) body[0].setVisibility(now ? View.VISIBLE : View.GONE);
            chev.setRotation(now ? 180 : 0);
            desc.run();
        });
        if (open) { body[0] = details(a, key, name, d); wrap.addView(body[0]); chev.setRotation(180); }
        parent.addView(wrap, Ui.lpMargins(a, -1, -2, 0, 0, 0, 6));
    }

    static View details(final MainActivity a, final String key, final String name, Detail d) {
        LinearLayout b = Ui.column(a);
        b.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 6), Ui.dp(a, 8));
        b.addView(Ui.body(a, d.why()));
        Map<String, Cost.Price> user = V16.data(a).prices;
        Cost.Price p = Cost.find(key, user, V16.priceDefaults(a));
        boolean mine = user != null && (user.containsKey(key) || user.containsKey(Normalizer.family(key)));
        String ct = p == null ? null : BuyNext.costText(p, V20.money(a, p.price), mine);
        b.addView(Ui.caption(a, ct != null ? ct : "No price yet. Set one in Settings \u203a Prices to see an estimate."), Ui.lpMargins(a, -1, -2, 0, 4, 0, 4));
        final List<String> ids = d.ids();
        if (!ids.isEmpty()) {
            b.addView(Ui.text(a, d.idsTitle(), 13, Ui.TEXT2, Ui.MEDIUM));
            for (int i = 0; i < ids.size() && i < SHOW_RECIPES; i++) {
                final Recipe r = a.repo.byId(ids.get(i));
                if (r == null) continue;
                TextView t = V20.action(a, r.title, R.drawable.ic_restaurant, v -> a.openDetail(r.id));
                t.setContentDescription("Open recipe " + r.title);
                b.addView(t, V20.rowLp(a));
            }
            if (ids.size() > SHOW_RECIPES) {
                TextView more = V20.action(a, "See all " + ids.size() + " recipes", R.drawable.ic_next, v -> a.openBuyNext(name, ids));
                b.addView(more, V20.rowLp(a));
            }
        }
        TextView add = Ui.button(a, "Add " + Ui.cap(name) + " to shopping list", R.drawable.ic_add, true);
        add.setOnClickListener(v -> {
            boolean ok = a.store.addShopping(Ui.cap(name), Double.NaN, "", "Buy these next");
            a.toast(ok ? "Added " + Ui.cap(name) + " to shopping list" : Ui.cap(name) + " is already on your shopping list");
            a.refreshShopping();
        });
        b.addView(add, Ui.lpMargins(a, -2, Ui.dp(a, 48), 0, 8, 0, 0));
        return b;
    }

    /** Every suggestion, sorted by most recipes or cheapest, a page at a time. */
    static void seeAll(final MainActivity a, final List<Matcher.Result> results) {
        final List<BuyNext.Option> ranked = BuyNext.rankedFromResults(results);
        final LinearLayout box = V20.box(a);
        final LinearLayout list = Ui.column(a);
        final String[] sort = { V16.prefs(a).getString("buyNextSort", BuyNext.SORT_RECIPES) };
        final int[] shown = { BuyNext.PAGE };
        LinearLayout chips = Ui.row(a);
        final TextView c1 = Ui.chip(a, "Most recipes", false), c2 = Ui.chip(a, "Cheapest", false);
        chips.addView(c1); chips.addView(c2, Ui.lpMargins(a, -2, -2, 8, 0, 0, 0));
        box.addView(Ui.caption(a, ranked.size() + " ingredients would bring recipes closer. Prices are estimates."));
        box.addView(chips, Ui.lpMargins(a, -1, -2, 0, 8, 0, 8));
        box.addView(list);
        final Runnable[] fill = new Runnable[1];
        fill[0] = () -> {
            list.removeAllViews();
            boolean cheap = BuyNext.SORT_CHEAPEST.equals(sort[0]);
            c1.setSelected(!cheap); c2.setSelected(cheap);
            Ui.styleChip(a, c1, !cheap); Ui.styleChip(a, c2, cheap);
            c1.setContentDescription("Sort by most recipes" + (!cheap ? ", selected" : ""));
            c2.setContentDescription("Sort by cheapest" + (cheap ? ", selected" : ""));
            List<BuyNext.Option> s = BuyNext.sort(ranked, sort[0], V16.data(a).prices, V16.priceDefaults(a));
            for (int i = 0; i < s.size() && i < shown[0]; i++) {
                final BuyNext.Option o = s.get(i);
                String sub = o.alone.isEmpty() ? V16.plural(o.closer.size(), "recipe", "recipes") + " one step closer" : "+" + V16.plural(o.alone.size(), "recipe", "recipes") + " you can make";
                addRow(a, list, "all:" + o.key, o.name, sub, o.demand(), new Detail() {
                    @Override public String why() { return BuyNext.why(o); }
                    @Override public List<String> ids() { return o.alone.isEmpty() ? o.closer : o.alone; }
                    @Override public String idsTitle() { return o.alone.isEmpty() ? "Recipes it brings within one ingredient" : "Recipes it unlocks"; }
                });
            }
            if (s.size() > shown[0]) {
                TextView more = Ui.button(a, "Show more (" + (s.size() - shown[0]) + " left)", R.drawable.ic_expand, false);
                more.setOnClickListener(v -> { shown[0] += BuyNext.PAGE; fill[0].run(); });
                list.addView(more, Ui.lpMargins(a, -2, Ui.dp(a, 48), 0, 4, 0, 0));
            }
        };
        c1.setOnClickListener(v -> { sort[0] = BuyNext.SORT_RECIPES; V16.prefs(a).edit().putString("buyNextSort", sort[0]).apply(); shown[0] = BuyNext.PAGE; fill[0].run(); });
        c2.setOnClickListener(v -> { sort[0] = BuyNext.SORT_CHEAPEST; V16.prefs(a).edit().putString("buyNextSort", sort[0]).apply(); shown[0] = BuyNext.PAGE; fill[0].run(); });
        fill[0].run();
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        Extras.dialog(a).setTitle("Buy these next \u00b7 all").setView(sv).setPositiveButton("Done", null).show();
    }
}
