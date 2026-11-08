package org.pantryrecipes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.pantryrecipes.app.core.Allergens;
import org.pantryrecipes.app.core.Backup;
import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.Region;
import org.pantryrecipes.app.core.MealPlan;
import org.pantryrecipes.app.core.Matcher;
import org.pantryrecipes.app.core.Recipe;
import org.pantryrecipes.app.core.UseBy;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** v1.5 features: use-by dates, quantities, ratings, dislikes/allergies, the meal planner and backups. */
final class Extras {
    private Extras() { }

    static final int REQ_EXPORT = 51, REQ_IMPORT = 52;

    static String version(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    static int versionCode(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    static int dp(Context c, float v) { return Ui.dp(c, v); }

    static AlertDialog.Builder dialog(Activity a) {
        return new Ui.Dialog(a);
    }

    // ---------------- use-by & quantity ----------------
    static int statusColor(int daysLeft) {
        String s = UseBy.status(daysLeft);
        if (UseBy.EXPIRED.equals(s)) return Ui.DANGER;
        if (UseBy.OK.equals(s)) return Ui.GREEN_DARK;
        return Ui.AMBER_TEXT;
    }

    /** "use by Fri 30 Oct · 200 g" style detail for a pantry chip (empty when nothing is set). */
    static String chipDetail(Store.PantryItem p) {
        String s = "";
        if (!p.useBy.isEmpty()) {
            int d = UseBy.daysLeft(p.useBy, Dates.today());
            s = d < 0 ? "past use-by" : d == 0 ? "use today" : d <= UseBy.SOON_DAYS ? "use in " + d + "d" : "";
        }
        if (!p.qty.isEmpty()) s += (s.isEmpty() ? "" : " \u00b7 ") + p.qty;
        return s;
    }

    static void editPantryItem(final Activity a, final Store store, final Store.PantryItem p, final Runnable done) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), 0);
        box.addView(Ui.sectionLabel(a, "Use-by date"));
        final String[] date = {p.useBy};
        final TextView dt = Ui.text(a, "", 16, Ui.TEXT, Ui.MEDIUM);
        dt.setPadding(0, dp(a, 6), 0, dp(a, 6));
        final Runnable show = () -> {
            if (date[0].isEmpty()) { dt.setText("No date set"); dt.setTextColor(Ui.TEXT3); return; }
            int d = UseBy.daysLeft(date[0], Dates.today());
            dt.setText(Dates.label(date[0]) + " \u00b7 " + UseBy.label(d));
            dt.setTextColor(statusColor(d));
        };
        show.run();
        box.addView(dt);
        FlowLayout quick = new FlowLayout(a, dp(a, 8), dp(a, 8));
        String[] labels = {"Pick date", "Today", "+1 day", "+3 days", "+1 week", "Clear"};
        int[] add = {Integer.MIN_VALUE, 0, 1, 3, 7, Integer.MAX_VALUE};
        for (int k = 0; k < labels.length; k++) {
            final int n = add[k];
            TextView c = Ui.chip(a, labels[k], k == 0);
            c.setContentDescription("Use-by " + labels[k]);
            c.setOnClickListener(v -> {
                if (n == Integer.MIN_VALUE) {
                    String base = date[0].isEmpty() ? Dates.today() : date[0];
                    int y = Integer.parseInt(base.substring(0, 4)), m = Integer.parseInt(base.substring(5, 7)), d = Integer.parseInt(base.substring(8));
                    DatePickerDialog pick = new DatePickerDialog(a, Ui.dialogTheme(), (dp, yy, mm, dd) -> {
                        date[0] = String.format(java.util.Locale.US, "%04d-%02d-%02d", yy, mm + 1, dd);
                        show.run();
                    }, y, m - 1, d);
                    pick.show();
                    Ui.style(a, pick);
                } else if (n == Integer.MAX_VALUE) {
                    date[0] = "";
                    show.run();
                } else {
                    date[0] = Dates.addDays(Dates.today(), n);
                    show.run();
                }
            });
            quick.addView(c);
        }
        box.addView(quick);
        TextView ql = Ui.sectionLabel(a, "Amount you have (optional)");
        ql.setPadding(0, dp(a, 18), 0, 0);
        box.addView(ql);
        final EditText qty = new EditText(a);
        qty.setHint("e.g. 500 g, 6, 1 l, 2 cans");
        qty.setText(p.qty);
        qty.setSingleLine(true);
        qty.setInputType(InputType.TYPE_CLASS_TEXT);
        box.addView(qty);
        TextView qn = Ui.caption(a, "With an amount, recipes show \u201cyou have enough\u201d or how much is missing. Grams, kilos, ml, litres, "
                + "spoons and cups convert; other units only compare with the same unit.");
        box.addView(qn);
        V21.detailRows(a, box, p.key, a instanceof MainActivity ? (Runnable) ((MainActivity) a)::pantryCategoriesChanged : null);
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        dialog(a).setTitle(p.label).setView(sv)
                .setPositiveButton("Save", (d, w) -> {
                    store.setPantryDetails(p.key, date[0], qty.getText().toString());
                    done.run();
                })
                .setNegativeButton("Cancel", null).show();
    }

    /** "Use soon" card for the pantry tab: dated items due within a week (and anything past its date). */
    static View expiringCard(final MainActivity a, final Store store, final Runnable changed) {
        final String today = Dates.today();
        List<Store.PantryItem> items = new ArrayList<Store.PantryItem>();
        for (Store.PantryItem p : store.pantry.values()) {
            int d = UseBy.daysLeft(p.useBy, today);
            if (d != Integer.MIN_VALUE && d <= 7) items.add(p);
        }
        if (items.isEmpty()) return null;
        Collections.sort(items, new Comparator<Store.PantryItem>() {
            public int compare(Store.PantryItem x, Store.PantryItem y) { return x.useBy.compareTo(y.useBy); }
        });
        LinearLayout card = Ui.card(a);
        LinearLayout head = Ui.row(a);
        head.addView(Ui.icon(a, R.drawable.ic_timer, Ui.AMBER, 20));
        TextView t = Ui.title(a, "Use soon", 17);
        t.setPadding(dp(a, 8), 0, 0, 0);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView go = Ui.button(a, "Recipes", R.drawable.ic_restaurant, false);
        go.setOnClickListener(v -> a.showTab(MainActivity.TAB_RECIPES, true));
        head.addView(go, new LinearLayout.LayoutParams(-2, dp(a, 48)));
        card.addView(head);
        TextView sub = Ui.caption(a, "Recipes that use items due within " + UseBy.SOON_DAYS + " days are ranked first.");
        sub.setPadding(0, dp(a, 2), 0, dp(a, 6));
        card.addView(sub);
        for (final Store.PantryItem p : items) {
            int d = UseBy.daysLeft(p.useBy, today);
            LinearLayout row = Ui.row(a);
            row.setPadding(dp(a, 4), dp(a, 8), dp(a, 4), dp(a, 8));
            row.setBackground(Ui.ripple(Ui.rounded(Color.TRANSPARENT, dp(a, 10)), dp(a, 10)));
            TextView n = Ui.text(a, p.label + (p.qty.isEmpty() ? "" : " (" + p.qty + ")"), 15, Ui.TEXT, Ui.REGULAR);
            row.addView(n, new LinearLayout.LayoutParams(0, -2, 1));
            TextView s = Ui.text(a, UseBy.label(d), 13, statusColor(d), Ui.MEDIUM);
            row.addView(s);
            row.setContentDescription(p.label + ", " + UseBy.label(d));
            row.setOnClickListener(v -> editPantryItem(a, store, p, changed));
            card.addView(row);
        }
        return card;
    }

    // ---------------- ratings & notes ----------------
    static View ratingRow(final Activity a, final Store store, final String id, final Runnable changed) {
        LinearLayout col = Ui.column(a);
        LinearLayout row = Ui.row(a);
        row.addView(Ui.text(a, "Your rating", 14, Ui.TEXT2, Ui.MEDIUM));
        final List<ImageView> stars = new ArrayList<ImageView>();
        final Runnable paint = () -> {
            int s = store.stars(id);
            for (int k = 0; k < stars.size(); k++) {
                stars.get(k).setImageResource(k < s ? R.drawable.ic_star : R.drawable.ic_star_outline);
                stars.get(k).setColorFilter(k < s ? Ui.STAR : Ui.TEXT3);
            }
        };
        for (int k = 1; k <= 5; k++) {
            final int n = k;
            ImageView st = Ui.iconButton(a, R.drawable.ic_star_outline, Ui.TEXT3, n + (n == 1 ? " star" : " stars"));
            st.setOnClickListener(v -> {
                int cur = store.stars(id);
                store.setRating(id, cur == n ? 0 : n, store.note(id));
                paint.run();
                if (changed != null) changed.run();
            });
            stars.add(st);
            row.addView(st);
        }
        paint.run();
        col.addView(row);
        final TextView note = Ui.text(a, "", 14, Ui.TEXT, Ui.REGULAR);
        note.setPadding(dp(a, 2), dp(a, 4), 0, dp(a, 2));
        final TextView edit = Ui.text(a, "", 14, Ui.GREEN_DARK, Ui.MEDIUM);
        edit.setPadding(dp(a, 2), dp(a, 6), dp(a, 8), dp(a, 6));
        final Runnable showNote = () -> {
            String s = store.note(id);
            note.setText(s);
            note.setVisibility(s.isEmpty() ? View.GONE : View.VISIBLE);
            edit.setText(s.isEmpty() ? "+ Add a note" : "Edit note");
        };
        showNote.run();
        edit.setOnClickListener(v -> {
            final EditText in = new EditText(a);
            in.setText(store.note(id));
            in.setHint("e.g. less chilli next time, kids loved it");
            in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            in.setMinLines(3);
            LinearLayout b = Ui.column(a);
            b.setPadding(dp(a, 20), dp(a, 4), dp(a, 20), 0);
            b.addView(in);
            dialog(a).setTitle("Your note").setView(b)
                    .setPositiveButton("Save", (d, w) -> {
                        store.setRating(id, store.stars(id), in.getText().toString());
                        showNote.run();
                        if (changed != null) changed.run();
                    })
                    .setNegativeButton("Cancel", null).show();
        });
        col.addView(note);
        col.addView(edit);
        return col;
    }

    // ---------------- allergens & dislikes ----------------
    /** Warning box on the detail screen, or null when there is nothing to say. */
    static View allergenBox(Activity a, Store store, Recipe r) {
        List<String> flags = Allergens.flags(r);
        List<String> reasons = Allergens.reasons(r, store.dislikes(), store.avoidAllergens());
        if (flags.isEmpty() && reasons.isEmpty()) return null;
        LinearLayout box = Ui.column(a);
        box.setBackground(Ui.rounded(reasons.isEmpty() ? Ui.NEUTRAL : Ui.AMBER_TINT, dp(a, 14)));
        box.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));
        LinearLayout head = Ui.row(a);
        head.addView(Ui.icon(a, R.drawable.ic_warning, reasons.isEmpty() ? Ui.TEXT2 : Ui.AMBER, 18));
        List<String> names = new ArrayList<String>();
        List<String> unsure = Allergens.uncertainFlags(r);
        for (String f : flags) names.add(Allergens.flagLabel(r, f, unsure));
        TextView t = Ui.text(a, flags.isEmpty() ? "Allergen check" : "May contain: " + android.text.TextUtils.join(", ", names), 14, Ui.TEXT, Ui.MEDIUM);
        t.setPadding(dp(a, 8), 0, 0, 0);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(head);
        if (!reasons.isEmpty()) {
            TextView rs = Ui.text(a, "Matches your dislikes/allergies: " + android.text.TextUtils.join(", ", reasons).replace("Contains ", ""),
                    14, Ui.AMBER_TEXT, Ui.MEDIUM);
            rs.setPadding(0, dp(a, 6), 0, 0);
            box.addView(rs);
        }
        TextView n = Ui.caption(a, Allergens.NOTE);
        n.setPadding(0, dp(a, 6), 0, 0);
        box.addView(n);
        return box;
    }

    static void dislikesDialog(final Activity a, final Store store, final Runnable done) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), 0);
        TextView intro = Ui.caption(a, "Recipes with these are hidden from suggestions and lists (favorites still show them, with a warning).");
        box.addView(intro);
        TextView al = Ui.sectionLabel(a, "Allergies / avoid");
        al.setPadding(0, dp(a, 14), 0, dp(a, 4));
        box.addView(al);
        final Set<String> avoid = new LinkedHashSet<String>(store.avoidAllergens());
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        final String rg20 = V20.region(a).isEmpty() ? org.pantryrecipes.app.core.Region.US : V20.region(a);
        box.addView(Ui.caption(a, org.pantryrecipes.app.core.Region.allergenListName(rg20) + ". Household members: Settings \u203a Household allergies."));
        for (final String id : V20.regionAllergens(a)) {
            CheckBox cb = new CheckBox(a);
            cb.setText(org.pantryrecipes.app.core.Region.allergenLabel(id, rg20));
            cb.setTextColor(Ui.TEXT);
            cb.setChecked(avoid.contains(id));
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
            cb.setTag(id);
            boxes.add(cb);
            box.addView(cb);
        }
        TextView dl = Ui.sectionLabel(a, "Ingredients you dislike");
        dl.setPadding(0, dp(a, 14), 0, dp(a, 4));
        box.addView(dl);
        final EditText in = new EditText(a);
        in.setHint("e.g. mushrooms, olives, coriander");
        in.setText(android.text.TextUtils.join(", ", store.dislikes()));
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        box.addView(in);
        TextView note = Ui.text(a, Allergens.NOTE, 13, Ui.AMBER_TEXT, Ui.MEDIUM);
        note.setPadding(0, dp(a, 10), 0, dp(a, 6));
        box.addView(note);
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        dialog(a).setTitle("Dislikes & allergies").setView(sv)
                .setPositiveButton("Save", (d, w) -> {
                    Set<String> sel = new HashSet<String>();
                    for (CheckBox cb : boxes) if (cb.isChecked()) sel.add((String) cb.getTag());
                    store.setAvoidAllergens(sel);
                    Set<String> keys = new LinkedHashSet<String>();
                    for (String s : in.getText().toString().split("[,\\n;]")) {
                        String k = Allergens.dislikeKey(s);
                        if (!k.isEmpty()) keys.add(k);
                    }
                    store.setDislikes(new ArrayList<String>(keys));
                    done.run();
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ---------------- meal planner ----------------
    static void addToPlanDialog(final Activity a, final Store store, final Recipe r, final int servings, final Runnable done) {
        final String today = Dates.today();
        final String[] days = new String[14];
        String[] labels = new String[14];
        for (int k = 0; k < 14; k++) {
            days[k] = Dates.addDays(today, k);
            labels[k] = (k == 0 ? "Today \u00b7 " : k == 1 ? "Tomorrow \u00b7 " : "") + Dates.label(days[k]);
        }
        dialog(a).setTitle("Add to plan (" + servings + " servings)")
                .setItems(labels, (d, w) -> {
                    store.addPlan(days[w], r.id, servings);
                    Ui.toastShort(a, "Planned for " + Dates.label(days[w]));
                    if (done != null) done.run();
                })
                .setNegativeButton("Cancel", null).show();
    }

    /** The planner card shown at the top of the Shopping tab. */
    static View plannerCard(final MainActivity a, final Store store, final Repo repo, final Runnable refresh) {
        final String weekStart = Dates.addDays(Region.weekStart(Dates.today(), V20.weekSunday(a)), 7 * a.planWeek);
        LinearLayout card = Ui.card(a);
        LinearLayout head = Ui.row(a);
        head.addView(Ui.icon(a, R.drawable.ic_calendar, Ui.GREEN, 20));
        LinearLayout tt = Ui.column(a);
        tt.setPadding(dp(a, 8), 0, 0, 0);
        tt.addView(Ui.title(a, "Meal plan", 17));
        String wl = a.planWeek == 0 ? "This week" : a.planWeek == 1 ? "Next week" : a.planWeek == -1 ? "Last week" : "Week of";
        tt.addView(Ui.caption(a, wl + " \u00b7 " + Dates.label(weekStart) + " \u2013 " + Dates.label(Dates.addDays(weekStart, 6))));
        head.addView(tt, new LinearLayout.LayoutParams(0, -2, 1));
        final String weekLabel = wl + " \u00b7 " + Dates.label(weekStart) + " \u2013 " + Dates.label(Dates.addDays(weekStart, 6));
        ImageView share = Ui.iconButton(a, R.drawable.ic_share, Ui.GREEN_DARK, "Share this week's plan");
        share.setOnClickListener(v -> {
            List<Object[]> out = new ArrayList<Object[]>();
            String end = Dates.addDays(weekStart, 6);
            for (Store.PlanRow pr : store.plan()) {
                String d = pr.entry.date;
                if (d.compareTo(weekStart) < 0 || d.compareTo(end) > 0) continue;
                Recipe r = repo.byId(pr.entry.id);
                out.add(new Object[]{d, r == null ? "Recipe not available" : r.title, pr.entry.servings});
            }
            java.util.Collections.sort(out, (x, y) -> ((String) x[0]).compareTo((String) y[0]));
            if (out.isEmpty()) { Ui.toastShort(a, "Nothing planned this week"); return; }
            V16.share(a, "Meal plan", org.pantryrecipes.app.core.ShareText.plan(weekLabel, out));
        });
        head.addView(share);
        ImageView prev = Ui.iconButton(a, R.drawable.ic_prev, Ui.GREEN_DARK, "Previous week");
        prev.setOnClickListener(v -> { a.planWeek--; refresh.run(); });
        head.addView(prev);
        ImageView next = Ui.iconButton(a, R.drawable.ic_next, Ui.GREEN_DARK, "Next week");
        next.setOnClickListener(v -> { a.planWeek++; refresh.run(); });
        head.addView(next);
        card.addView(head);
        List<Store.PlanRow> rows = store.plan();
        final List<MealPlan.Entry> week = new ArrayList<MealPlan.Entry>();
        String today = Dates.today();
        int n = 0;
        for (int k = 0; k < 7; k++) {
            String day = Dates.addDays(weekStart, k);
            LinearLayout dr = Ui.row(a);
            dr.setGravity(Gravity.TOP);
            dr.setPadding(dp(a, 2), dp(a, 8), 0, dp(a, 4));
            TextView dl = Ui.text(a, Dates.label(day).substring(0, 3) + "\n" + day.substring(8).replaceFirst("^0", ""), 13,
                    day.equals(today) ? Ui.GREEN_DARK : Ui.TEXT2, Ui.MEDIUM);
            dl.setMinWidth(dp(a, 48));
            dr.addView(dl);
            LinearLayout items = Ui.column(a);
            boolean any = false;
            for (final Store.PlanRow pr : rows) {
                if (!pr.entry.date.equals(day)) continue;
                any = true;
                week.add(pr.entry);
                n++;
                final Recipe r = repo.byId(pr.entry.id);
                LinearLayout ir = Ui.row(a);
                TextView it = Ui.text(a, (r == null ? "Recipe not available" : r.title) + " \u00b7 " + pr.entry.servings + " serv.", 15,
                        r == null ? Ui.TEXT3 : Ui.TEXT, Ui.REGULAR);
                it.setPadding(0, dp(a, 6), 0, dp(a, 6));
                if (r != null) it.setOnClickListener(v -> a.openPlannedRecipe(r.id));
                ir.addView(it, new LinearLayout.LayoutParams(0, -2, 1));
                ImageView del = Ui.iconButton(a, R.drawable.ic_close, Ui.TEXT3, "Remove from plan");
                del.setOnClickListener(v -> { store.deletePlan(pr.rowId); refresh.run(); });
                ir.addView(del);
                items.addView(ir);
            }
            if (!any) {
                TextView e = Ui.caption(a, "\u2013");
                e.setPadding(0, dp(a, 6), 0, dp(a, 6));
                e.setTextColor(Ui.TEXT3);
                items.addView(e);
            }
            dr.addView(items, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(dr);
            if (k < 6) card.addView(Ui.divider(a));
        }
        if (n == 0) {
            TextView tip = Ui.caption(a, "Open any recipe and tap \u201cAdd to plan\u201d to plan your week.");
            tip.setPadding(0, dp(a, 8), 0, 0);
            card.addView(tip);
        } else {
            LinearLayout br = Ui.row(a);
            br.setPadding(0, dp(a, 10), 0, 0);
            TextView add = Ui.button(a, "Add week to shopping list", R.drawable.ic_cart, true);
            add.setOnClickListener(v -> {
                Matcher m = a.newMatcher();
                List<MealPlan.Item> list = MealPlan.shopping(week, repo::byId, m, store.qtyMap());
                int added = 0;
                for (MealPlan.Item it : list) {
                    if (store.addShopping(it.name, it.amount, Double.isNaN(it.amount) ? "" : it.unit,
                            android.text.TextUtils.join(", ", it.recipes))) added++;
                }
                Ui.toastShort(a, list.isEmpty() ? "You have everything for this week's plan"
                        : "Added " + added + " item" + (added == 1 ? "" : "s") + " (pantry items and staples skipped)");
                refresh.run();
            });
            br.addView(add, new LinearLayout.LayoutParams(-2, dp(a, 48)));
            br.addView(Ui.space(a, 10, 1));
            TextView clr = Ui.button(a, "Clear week", R.drawable.ic_delete, false);
            clr.setOnClickListener(v -> dialog(a).setTitle("Clear this week's plan?")
                    .setPositiveButton("Clear", (d, w) -> { store.clearPlanWeek(weekStart); refresh.run(); })
                    .setNegativeButton("Cancel", null).show());
            br.addView(clr, new LinearLayout.LayoutParams(-2, dp(a, 48)));
            android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(a);
            hs.setHorizontalScrollBarEnabled(false);
            hs.addView(br);
            card.addView(hs);
        }
        return card;
    }

    // ---------------- backup ----------------
    static void startExport(Activity a) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "pantry-recipes-backup-" + Dates.today() + ".json");
        try {
            a.startActivityForResult(i, REQ_EXPORT);
        } catch (Exception e) {
            Ui.toastShort(a, "No file picker available on this device");
        }
    }

    static void startImport(Activity a) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
        try {
            a.startActivityForResult(i, REQ_IMPORT);
        } catch (Exception e) {
            Ui.toastShort(a, "No file picker available on this device");
        }
    }

    /** Set from About just before the export picker opens; photos are off by default. */
    static boolean exportPhotos;

    static void writeExport(Activity a, Store store, Repo repo, Uri uri) {
        try {
            Set<String> ref = new HashSet<String>(store.favorites);
            for (MealPlan.Entry e : store.planEntries()) ref.add(e.id);
            ref.addAll(store.ratings.keySet());
            Backup.Data d = store.exportBackup(version(a), repo.recipesForBackup(ref));
            V16.exportExtras(a, d, exportPhotos);
            V17.exportExtras(a, d);
            V20.exportExtras(a, d);
            OutputStream out = a.getContentResolver().openOutputStream(uri, "wt");
            if (out == null) throw new java.io.IOException("no stream");
            out.write(Backup.write(d).getBytes("UTF-8"));
            out.close();
            Ui.toastShort(a, "Backup saved: " + Backup.summary(d));
        } catch (Exception e) {
            Ui.toastShort(a, "Could not save the backup");
        }
    }

    static void readImport(final MainActivity a, final Store store, final Repo repo, Uri uri) {
        final Backup.Data d;
        try {
            InputStream in = a.getContentResolver().openInputStream(uri);
            if (in == null) throw new java.io.IOException("no stream");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > Backup.MAX_CHARS * 2) { in.close(); throw new Backup.BackupException("The file is too large to be a My Pantry Recipes backup."); }
                bos.write(buf, 0, n);
            }
            in.close();
            d = Backup.parse(bos.toString("UTF-8"));
        } catch (Backup.BackupException e) {
            dialog(a).setTitle("Can't import").setMessage(e.getMessage()).setPositiveButton("OK", null).show();
            return;
        } catch (Exception e) {
            dialog(a).setTitle("Can't import").setMessage("The file could not be read.").setPositiveButton("OK", null).show();
            return;
        }
        String from = (d.platform.isEmpty() ? "" : " from the " + ("web".equals(d.platform) ? "web app" : d.platform + " app"))
                + (d.appVersion.isEmpty() ? "" : " v" + d.appVersion) + (d.exported.isEmpty() ? "" : ", " + d.exported);
        dialog(a).setTitle("Replace your data?")
                .setMessage("This backup" + from + " has " + Backup.summary(d) + ".\n\nImporting replaces your current pantry, favorites, "
                        + "shopping list, ratings, notes, meal plan, dislikes and settings. Consider exporting a backup first.")
                .setPositiveButton("Replace", (x, w) -> {
                    store.importBackup(d);
                    V16.importExtras(a, d);
                    V17.importExtras(a, d);
                    V20.importExtras(a, d);
                    repo.reload();
                    Ui.toastShort(a, "Backup imported");
                    a.recreate();
                })
                .setNegativeButton("Cancel", null).show();
    }
}
