package org.pantryrecipes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import org.pantryrecipes.app.core.Amounts;
import org.pantryrecipes.app.core.Backup;
import org.pantryrecipes.app.core.Barcode;
import org.pantryrecipes.app.core.BuyNext;
import org.pantryrecipes.app.core.Cost;
import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.Ingredient;
import org.pantryrecipes.app.core.Leftovers;
import org.pantryrecipes.app.core.Matcher;
import org.pantryrecipes.app.core.Normalizer;
import org.pantryrecipes.app.core.Nutrition;
import org.pantryrecipes.app.core.Quantity;
import org.pantryrecipes.app.core.Recipe;
import org.pantryrecipes.app.core.UseBy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v1.6 features for the Android app: nutrition and cost estimates, "cooked this" with leftovers, batch cooking,
 * recipe photos, prices, barcode lookup (Open Food Facts), "buy these next", sharing and the backup additions.
 * Shared logic lives in core/ (the same code paths as the web app's core16.js).
 */
final class V16 {
    private V16() { }

    static final int REQ_PHOTO = 7101, REQ_SCAN = 7102;
    static final int PHOTO_MAX_PX = 800, PHOTO_QUALITY = 82;

    static int dp(Context c, float v) { return Ui.dp(c, v); }
    static SharedPreferences prefs(Context c) { return Store.get(c).prefs; }

    // ---------------- stored data (prefs "v16", backup format so it is validated the same way) ----------------
    private static Backup.Data data;

    static synchronized Backup.Data data(Context c) {
        if (data == null) {
            try {
                data = Backup.parse(prefs(c).getString("v16", "{\"app\":\"Pantry Recipes\",\"format\":2}"));
            } catch (Exception e) {
                data = new Backup.Data();
            }
        }
        return data;
    }

    static synchronized void save(Context c) {
        Backup.Data d = data(c), s = new Backup.Data();
        s.prices.putAll(d.prices);
        s.leftovers.addAll(d.leftovers);
        s.cooked.addAll(d.cooked);
        s.barcodes.putAll(d.barcodes);
        prefs(c).edit().putString("v16", Backup.write(s)).apply();
    }

    static synchronized void reset() { data = null; }

    static String currency(Context c) { return prefs(c).getString("currency", Cost.DEFAULT_CURRENCY); }
    static boolean useDefaultPrices(Context c) { return prefs(c).getBoolean("useDefaultPrices", true); }
    static boolean deductPantry(Context c) { return prefs(c).getBoolean("deductPantry", false); }
    static boolean shopCost(Context c) { return prefs(c).getBoolean("shopCost", false); }
    static int leftoverDays(Context c) { return Leftovers.clampDays(prefs(c).getInt("leftoverDays", Leftovers.DEFAULT_DAYS)); }
    static String money(Context c, double v) { return Cost.money(currency(c), v); }

    // ---------------- bundled tables ----------------
    private static Map<String, Nutrition.Food> foods;
    private static Map<String, Cost.Price> defaults;

    static synchronized Map<String, Nutrition.Food> foods(Context c) {
        if (foods == null) foods = Nutrition.parse(asset(c, "foods.tsv"));
        return foods;
    }

    static synchronized Map<String, Cost.Price> defaultPrices(Context c) {
        if (defaults == null) defaults = Cost.parseDefaults(asset(c, "prices.tsv"));
        return defaults;
    }

    static Map<String, Cost.Price> priceDefaults(Context c) { return useDefaultPrices(c) ? V20.priceDefaults(c) : null; } // v2.0: BLS regional (US) or none

    static String asset(Context c, String name) {
        try {
            InputStream in = c.getAssets().open(name);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            in.close();
            return b.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    // ---------------- small UI helpers ----------------
    static TextView badge(Context c, String s) {
        TextView t = Ui.text(c, s, 12, Ui.AMBER_TEXT, Ui.MEDIUM);
        t.setBackground(Ui.rounded(Ui.AMBER_TINT, dp(c, 10)));
        t.setPadding(dp(c, 8), dp(c, 3), dp(c, 8), dp(c, 3));
        return t;
    }

    static LinearLayout stat(Context c, String big, String small) {
        LinearLayout l = Ui.column(c);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.addView(Ui.text(c, big, 18, Ui.TEXT, Ui.MEDIUM));
        l.addView(Ui.caption(c, small));
        return l;
    }

    static LinearLayout boxed(Context c) {
        LinearLayout box = Ui.column(c);
        box.setBackground(Ui.outlined(Ui.CARD, Ui.LINE, Math.max(1, dp(c, 1)), dp(c, 14)));
        box.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        return box;
    }

    static LinearLayout head(Context c, String title, String badge) {
        LinearLayout row = Ui.row(c);
        row.addView(Ui.title(c, title, 16), new LinearLayout.LayoutParams(0, -2, 1));
        if (badge != null) row.addView(badge(c, badge));
        return row;
    }

    static String plural(int n, String one, String many) { return n + " " + (n == 1 ? one : many); }

    static EditText field(Context c, String hint, String value, int type) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setInputType(type);
        e.setTextColor(Ui.TEXT);
        e.setHintTextColor(Ui.TEXT3);
        return e;
    }

    static int parseInt(EditText e, int def) {
        try { return Integer.parseInt(e.getText().toString().trim()); } catch (Exception x) { return def; }
    }

    // ---------------- nutrition + cost ----------------
    /** Nutrition and cost estimate boxes; each shows numbers only at >=70% coverage and is labelled "Estimate · N%". */
    static View estimatesCard(final MainActivity a, final Recipe r, int servings) {
        LinearLayout col = Ui.column(a);
        Nutrition.Estimate n = Nutrition.estimate(r, foods(a));
        LinearLayout nb = boxed(a);
        nb.addView(head(a, "Nutrition per serving", "Estimate \u00b7 " + n.percent() + "%"));
        if (n.show()) {
            LinearLayout stats = Ui.row(a);
            String[][] v = {{String.valueOf(Math.round(n.kcal)), "kcal"}, {Math.round(n.protein) + " g", "protein"},
                    {Math.round(n.carbs) + " g", "carbs"}, {Math.round(n.fat) + " g", "fat"}};
            for (String[] x : v) stats.addView(stat(a, x[0], x[1]), new LinearLayout.LayoutParams(0, -2, 1));
            nb.addView(stats, Ui.lpMargins(a, -1, -2, 0, 10, 0, 8));
            nb.addView(Ui.caption(a, "Based on the original " + plural(r.servings, "serving", "servings") + "; " + n.covered + " of " + n.total
                    + " ingredients matched. " + Nutrition.NOTE));
        } else {
            nb.addView(Ui.caption(a, "Not enough ingredients could be matched (" + n.percent() + "%, needs " + Nutrition.MIN_COVERAGE
                    + "%) for a useful estimate."), Ui.lpMargins(a, -1, -2, 0, 6, 0, 0));
        }
        col.addView(nb);
        Cost.Estimate c = Cost.estimate(r, servings, data(a).prices, priceDefaults(a), foods(a));
        LinearLayout cb = boxed(a);
        cb.addView(head(a, "Cost", "Estimate \u00b7 " + c.percent() + "%"));
        if (c.show()) {
            LinearLayout stats = Ui.row(a);
            stats.addView(stat(a, money(a, c.perServing), "per serving"), new LinearLayout.LayoutParams(0, -2, 1));
            stats.addView(stat(a, money(a, c.total), "for " + plural(servings, "serving", "servings")), new LinearLayout.LayoutParams(0, -2, 1));
            cb.addView(stats, Ui.lpMargins(a, -1, -2, 0, 10, 0, 8));
            cb.addView(Ui.caption(a, "Priced " + c.covered + " of " + c.count + " ingredients"
                    + (c.fromDefaults > 0 ? ", " + c.fromDefaults + " from rough default prices (USD ballpark)" : "") + ". " + Cost.NOTE));
        } else {
            cb.addView(Ui.caption(a, "Only " + c.percent() + "% of the ingredients have a price (needs " + Cost.MIN_COVERAGE
                    + "%). Add your prices to see a cost."), Ui.lpMargins(a, -1, -2, 0, 6, 0, 0));
        }
        TextView edit = a.smallAction("Edit prices for this recipe", R.drawable.ic_list);
        edit.setOnClickListener(v -> pricesDialog(a, r, a::refreshDetailEstimates));
        cb.addView(edit, Ui.lpMargins(a, -2, dp(a, 48), 0, 10, 0, 0));
        col.addView(cb, Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
        return col;
    }

    // ---------------- prices ----------------
    static void priceDialog(final Activity a, final String key, final String label, final Runnable done) {
        Cost.Price cur = data(a).prices.get(key);
        Cost.Price def = cur == null && useDefaultPrices(a) ? defaultPrices(a).get(key) : null;
        Cost.Price show = cur != null ? cur : def;
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), 0);
        box.addView(Ui.caption(a, "Price you pay and the package it buys (e.g. 2.50 for 500 g). Used only for estimates on this device."));
        final EditText price = field(a, "Price (" + currency(a) + ")", show == null ? "" : Quantity.number(show.price, ""),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(price);
        LinearLayout row = Ui.row(a);
        final EditText amt = field(a, "Amount", show == null ? "1" : Quantity.number(show.amount, show.unit),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        row.addView(amt, new LinearLayout.LayoutParams(0, -2, 1));
        final android.widget.Spinner unit = new android.widget.Spinner(a);
        android.widget.ArrayAdapter<String> ad = new android.widget.ArrayAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item, Cost.UNIT_NAMES);
        unit.setAdapter(ad);
        int sel = 0;
        for (int k = 0; k < Cost.UNITS.length; k++) if (show != null && Cost.UNITS[k].equals(show.unit)) sel = k;
        unit.setSelection(sel);
        row.addView(unit, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(row);
        if (def != null) box.addView(Ui.caption(a, "Showing the rough default price (an editable estimate)."));
        AlertDialog.Builder b = new Ui.Dialog(a).setTitle("Price: " + label).setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    double p, n;
                    try { p = Double.parseDouble(price.getText().toString().trim().replace(',', '.')); } catch (Exception e) { p = Double.NaN; }
                    try { n = Double.parseDouble(amt.getText().toString().trim().replace(',', '.')); } catch (Exception e) { n = Double.NaN; }
                    Cost.Price np = Cost.make(p, n, Cost.UNITS[Math.max(0, unit.getSelectedItemPosition())]);
                    if (np == null) { Ui.toastShort(a, "Enter a price and a package amount above 0"); return; }
                    if (!data(a).prices.containsKey(key) && data(a).prices.size() >= Cost.MAX_PRICES) { Ui.toastShort(a, "Price list is full"); return; }
                    data(a).prices.put(key, np);
                    save(a);
                    Ui.toastShort(a, "Price saved");
                    if (done != null) done.run();
                });
        if (cur != null) b.setNeutralButton("Remove", (d, w) -> { data(a).prices.remove(key); save(a); if (done != null) done.run(); });
        b.show();
    }

    /** Prices & currency: settings, this recipe's ingredients (when opened from a recipe) and your prices. */
    static void pricesDialog(final MainActivity a, final Recipe r, final Runnable changed) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), dp(a, 8));
        final AlertDialog[] self = new AlertDialog[1];
        final Runnable again = () -> { if (self[0] != null) self[0].dismiss(); if (changed != null) changed.run(); pricesDialog(a, r, changed); };
        box.addView(Ui.sectionLabel(a, "Currency symbol"));
        final EditText cur = field(a, "$", currency(a), InputType.TYPE_CLASS_TEXT);
        cur.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
        box.addView(cur);
        final CheckBox defs = new CheckBox(a);
        defs.setText("Use rough default prices (built-in US ballpark prices, editable estimates, not live prices)");
        defs.setTextColor(Ui.TEXT);
        defs.setChecked(useDefaultPrices(a));
        box.addView(defs, Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        final CheckBox shop = new CheckBox(a);
        shop.setText("Show cost on the shopping list");
        shop.setTextColor(Ui.TEXT);
        shop.setChecked(shopCost(a));
        box.addView(shop);
        if (r != null) {
            TextView h = Ui.sectionLabel(a, "Ingredients in " + r.title);
            h.setPadding(0, dp(a, 16), 0, dp(a, 4));
            box.addView(h);
            java.util.Set<String> seen = new java.util.HashSet<String>();
            for (final Ingredient i : r.ingredients) {
                if (i.key.isEmpty() || !seen.add(i.key)) continue;
                Cost.Price p = Cost.find(i.key, data(a).prices, priceDefaults(a));
                boolean mine = data(a).prices.containsKey(i.key);
                box.addView(priceRow(a, Ui.cap(i.name.split(",")[0]), p == null ? "No price" : Cost.describe(currency(a), p) + (mine ? "" : " \u00b7 default estimate"),
                        () -> priceDialog(a, i.key, Ui.cap(i.name.split(",")[0]), again)));
            }
        }
        TextView yh = Ui.sectionLabel(a, "Your prices \u00b7 " + data(a).prices.size());
        yh.setPadding(0, dp(a, 16), 0, dp(a, 4));
        box.addView(yh);
        if (data(a).prices.isEmpty()) box.addView(Ui.caption(a, "None yet. Tap an ingredient above, or open a recipe's cost card."));
        for (final Map.Entry<String, Cost.Price> e : new ArrayList<Map.Entry<String, Cost.Price>>(data(a).prices.entrySet())) {
            box.addView(priceRow(a, Ui.cap(e.getKey()), Cost.describe(currency(a), e.getValue()), () -> priceDialog(a, e.getKey(), Ui.cap(e.getKey()), again)));
        }
        box.addView(Ui.caption(a, Cost.NOTE), Ui.lpMargins(a, -1, -2, 0, 12, 0, 0));
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        self[0] = new Ui.Dialog(a).setTitle("Prices & currency").setView(sv)
                .setPositiveButton("Done", (d, w) -> {
                    String cv = cur.getText().toString().trim();
                    prefs(a).edit().putString("currency", cv.isEmpty() ? Cost.DEFAULT_CURRENCY : cv)
                            .putBoolean("useDefaultPrices", defs.isChecked()).putBoolean("shopCost", shop.isChecked()).apply();
                    if (changed != null) changed.run();
                }).show();
    }

    static View priceRow(Context c, String name, String sub, final Runnable click) {
        LinearLayout row = Ui.row(c);
        row.setPadding(dp(c, 4), dp(c, 8), dp(c, 4), dp(c, 8));
        row.setBackground(Ui.ripple(Ui.rounded(0x00000000, dp(c, 10)), dp(c, 10)));
        LinearLayout tc = Ui.column(c);
        tc.addView(Ui.text(c, name, 15, Ui.TEXT, Ui.REGULAR));
        tc.addView(Ui.caption(c, sub));
        row.addView(tc, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(Ui.text(c, "Edit", 14, Ui.GREEN_DARK, Ui.MEDIUM));
        row.setOnClickListener(v -> click.run());
        return row;
    }

    // ---------------- cooked this / leftovers ----------------
    static TextView cookedNote(Context c, String id) {
        Object[] h = Leftovers.history(data(c).cooked, id);
        int n = h[0] instanceof Number ? ((Number) h[0]).intValue() : 0;
        if (n == 0) return null;
        TextView t = Ui.caption(c, "\u2713 Cooked " + (n == 1 ? "once" : n + " times") + " \u00b7 last " + Dates.label((String) h[1]));
        t.setTextColor(Ui.GREEN_DARK);
        return t;
    }

    static void cookedDialog(final MainActivity a, final Recipe r, final int servings, final Runnable done) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), 0);
        box.addView(Ui.sectionLabel(a, "Servings cooked"));
        final EditText sv = field(a, "Servings", String.valueOf(servings), InputType.TYPE_CLASS_NUMBER);
        box.addView(sv);
        box.addView(Ui.sectionLabel(a, "Leftover portions to keep"));
        final EditText por = field(a, "0", "0", InputType.TYPE_CLASS_NUMBER);
        box.addView(por);
        box.addView(Ui.sectionLabel(a, "Use leftovers within (days)"));
        final EditText days = field(a, "Days", String.valueOf(leftoverDays(a)), InputType.TYPE_CLASS_NUMBER);
        box.addView(days);
        box.addView(Ui.caption(a, "Cooked food is usually safe in the fridge for 3\u20134 days. Change the default in Settings."));
        final CheckBox ded = new CheckBox(a);
        ded.setText("Take the used amounts out of my pantry");
        ded.setTextColor(Ui.TEXT);
        ded.setChecked(deductPantry(a));
        box.addView(ded, Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        box.addView(Ui.caption(a, "Only items with a quantity typed in the pantry change; units that can\u2019t be compared are left alone."));
        ScrollView s = new ScrollView(a);
        s.addView(box);
        new Ui.Dialog(a).setTitle("Cooked " + r.title).setView(s)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    int n = Math.max(1, Math.min(72, parseInt(sv, servings)));
                    int p = Math.max(0, Math.min(Leftovers.MAX_PORTIONS, parseInt(por, 0)));
                    int dd = Leftovers.clampDays(parseInt(days, leftoverDays(a)));
                    String today = Dates.today();
                    Leftovers.log(data(a).cooked, r.id, n, today);
                    if (p > 0) Leftovers.add(data(a).leftovers, r.id, r.title, p, today, dd);
                    save(a);
                    prefs(a).edit().putBoolean("deductPantry", ded.isChecked()).apply();
                    int changed = ded.isChecked() ? deduct(a, r, (double) n / Math.max(1, r.servings)) : 0;
                    Ui.toastShort(a, "Logged" + (p > 0 ? " \u00b7 " + plural(p, "leftover portion", "leftover portions") : "")
                            + (changed > 0 ? " \u00b7 " + plural(changed, "pantry item", "pantry items") + " updated" : ""));
                    if (done != null) done.run();
                }).show();
    }

    /** Takes the recipe's amounts out of pantry quantities (same rules as the web app). Returns the items changed. */
    static int deduct(Context c, Recipe r, double scale) {
        Store store = Store.get(c);
        int changed = 0;
        for (Store.PantryItem p : new ArrayList<Store.PantryItem>(store.pantry.values())) {
            if (p.qty == null || p.qty.isEmpty()) continue;
            Map<String, String> one = new LinkedHashMap<String, String>();
            one.put(p.key, p.qty);
            String cur = p.qty;
            boolean hit = false;
            for (Ingredient i : r.ingredients) {
                if (i.optional || i.key.isEmpty() || Double.isNaN(i.amount) || Amounts.qtyFor(i.key, one) == null) continue;
                String nx = Amounts.deduct(cur, p.key, i.amount * scale, i.unit);
                if (nx != null) { cur = nx; hit = true; }
            }
            if (!hit || cur.equals(p.qty)) continue;
            changed++;
            double left;
            try { left = Double.parseDouble(cur.split(" ")[0]); } catch (Exception e) { left = 1; }
            if (left == 0 && !p.staple) store.removeFromPantry(p.key);
            else store.setPantryDetails(p.key, p.useBy, cur);
        }
        return changed;
    }

    /** Leftovers card for the pantry tab (null when there are none). */
    /** v1.8: what the leftovers card shows, so the Pantry rebuilds it only when this changes. */
    static String leftoversSig(Context c) {
        StringBuilder b = new StringBuilder();
        for (Leftovers.Entry e : data(c).leftovers) b.append(e.id).append('|').append(e.portions).append('|').append(e.useBy).append('|').append(e.title).append('\n');
        return b.toString();
    }

    static View leftoversCard(final MainActivity a, final Runnable changed) {
        List<Leftovers.Entry> list = Leftovers.sorted(data(a).leftovers);
        if (list.isEmpty()) return null;
        LinearLayout card = Ui.card(a);
        card.setPadding(dp(a, 16), dp(a, 14), dp(a, 8), dp(a, 10));
        int total = 0;
        for (Leftovers.Entry e : list) total += e.portions;
        card.addView(head(a, "Leftovers", plural(total, "portion", "portions")));
        String today = Dates.today();
        for (final Leftovers.Entry e : list) {
            LinearLayout row = Ui.row(a);
            row.setPadding(0, dp(a, 6), 0, dp(a, 2));
            LinearLayout tc = Ui.column(a);
            TextView t = Ui.text(a, e.title, 15, Ui.TEXT, Ui.MEDIUM);
            Ui.ellipsize(t, 1);
            t.setOnClickListener(v -> a.openPlannedRecipe(e.recipe));
            tc.addView(t);
            int dl = UseBy.daysLeft(e.useBy, today);
            TextView st = Ui.caption(a, plural(e.portions, "portion", "portions") + " \u00b7 " + UseBy.label(dl));
            st.setTextColor(Extras.statusColor(dl));
            tc.addView(st);
            row.addView(tc, new LinearLayout.LayoutParams(0, -2, 1));
            ImageView minus = Ui.iconButton(a, R.drawable.ic_remove, Ui.GREEN_DARK, "One portion less of " + e.title);
            minus.setOnClickListener(v -> { Leftovers.adjust(data(a).leftovers, e.id, -1); save(a); changed.run(); });
            row.addView(minus);
            ImageView plus = Ui.iconButton(a, R.drawable.ic_add, Ui.GREEN_DARK, "One portion more of " + e.title);
            plus.setOnClickListener(v -> { Leftovers.adjust(data(a).leftovers, e.id, 1); save(a); changed.run(); });
            row.addView(plus);
            ImageView del = Ui.iconButton(a, R.drawable.ic_close, Ui.TEXT3, "Remove " + e.title + " leftovers");
            del.setOnClickListener(v -> { Leftovers.adjust(data(a).leftovers, e.id, -Leftovers.MAX_PORTIONS - 1); save(a); changed.run(); });
            row.addView(del);
            card.addView(row);
        }
        return card;
    }

    // ---------------- photos (private app files, ~800 px JPEG) ----------------
    static String pendingPhotoId;

    /** The recipe a photo is being picked for (kept in prefs too, in case the app was killed meanwhile). */
    static String takePendingPhoto(Context c) {
        String id = pendingPhotoId != null ? pendingPhotoId : prefs(c).getString("pendingPhoto", null);
        pendingPhotoId = null;
        prefs(c).edit().remove("pendingPhoto").apply();
        return id;
    }

    static File photoDir(Context c) { File d = new File(c.getFilesDir(), "photos"); d.mkdirs(); return d; }

    static File photoFile(Context c, String id) {
        StringBuilder b = new StringBuilder();
        for (char ch : id.toCharArray()) b.append(Character.isLetterOrDigit(ch) ? ch : '_');
        return new File(photoDir(c), b + "-" + Integer.toHexString(id.hashCode()) + ".jpg");
    }

    static boolean hasPhoto(Context c, String id) { return photoFile(c, id).isFile(); }

    static Bitmap loadPhoto(Context c, String id) {
        File f = photoFile(c, id);
        return f.isFile() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
    }

    static void pickPhoto(Activity a, String id) {
        pendingPhotoId = id;
        prefs(a).edit().putString("pendingPhoto", id).apply();
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            a.startActivityForResult(Intent.createChooser(i, "Choose a photo"), REQ_PHOTO);
        } catch (Exception e) {
            Ui.toastShort(a, "No photo picker available on this device");
        }
    }

    /** Decodes the picked image at a reduced size, scales its longest side to 800 px and saves a JPEG in private storage. */
    static boolean savePhoto(Context c, String id, Uri uri) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = c.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, o);
            if (in != null) in.close();
            if (o.outWidth <= 0 || o.outHeight <= 0) return false;
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= PHOTO_MAX_PX) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            in = c.getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            if (in != null) in.close();
            if (b == null) return false;
            Bitmap s = scaled(b, PHOTO_MAX_PX);
            File f = photoFile(c, id), tmp = new File(f.getPath() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            s.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out);
            out.close();
            if (s != b) s.recycle();
            b.recycle();
            boolean ok = tmp.renameTo(f);
            if (ok) rememberPhoto(c, id);
            return ok;
        } catch (Exception | OutOfMemoryError e) {
            return false;
        }
    }

    static Bitmap scaled(Bitmap b, int max) {
        int w = b.getWidth(), h = b.getHeight(), m = Math.max(w, h);
        if (m <= max) return b;
        double k = (double) max / m;
        return Bitmap.createScaledBitmap(b, Math.max(1, (int) Math.round(w * k)), Math.max(1, (int) Math.round(h * k)), true);
    }

    static View photoCard(final MainActivity a, final Recipe r, final Runnable changed) {
        LinearLayout box = boxed(a);
        boolean has = hasPhoto(a, r.id);
        box.addView(head(a, "Your photo", null));
        box.addView(Ui.caption(a, has ? "Shown at the top of this recipe. Stored only on this device (not in backups unless you include photos)."
                : "Add a photo of your dish. It is resized to about 800 px and stays on this device."), Ui.lpMargins(a, -1, -2, 0, 4, 0, 8));
        LinearLayout row = Ui.row(a);
        TextView pick = a.smallAction(has ? "Replace photo" : "Add photo", R.drawable.ic_photo);
        pick.setOnClickListener(v -> pickPhoto(a, r.id));
        row.addView(pick, new LinearLayout.LayoutParams(-2, dp(a, 48)));
        if (has) {
            row.addView(Ui.space(a, 8, 1));
            TextView del = a.smallAction("Remove", R.drawable.ic_delete);
            del.setOnClickListener(v -> { photoFile(a, r.id).delete(); changed.run(); });
            row.addView(del, new LinearLayout.LayoutParams(-2, dp(a, 48)));
        }
        box.addView(row);
        return box;
    }

    // ---------------- backup ----------------
    static void exportExtras(Context c, Backup.Data d, boolean withPhotos) {
        Backup.Data x = data(c);
        d.prices.putAll(x.prices);
        d.leftovers.addAll(x.leftovers);
        d.cooked.addAll(x.cooked);
        d.barcodes.putAll(x.barcodes);
        d.settings.put("currency", currency(c));
        d.settings.put("useDefaultPrices", useDefaultPrices(c));
        d.settings.put("deductPantry", deductPantry(c));
        d.settings.put("leftoverDays", (long) leftoverDays(c));
        d.settings.put("shopCost", shopCost(c));
        if (!withPhotos) return;
        File[] files = photoDir(c).listFiles();
        if (files == null) return;
        Map<String, String> names = new LinkedHashMap<String, String>();
        for (String id : photoIds(c)) names.put(photoFile(c, id).getName(), id);
        for (File f : files) {
            String id = names.get(f.getName());
            if (id == null || d.photos.size() >= Backup.MAX_PHOTOS) continue;
            try {
                byte[] bytes = readFile(f);
                String u = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                if (u.length() <= Backup.MAX_PHOTO_CHARS) d.photos.put(id, u);
            } catch (Throwable ignored) { }
        }
    }

    static byte[] readFile(File f) throws java.io.IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        try { while ((n = in.read(buf)) > 0) b.write(buf, 0, n); } finally { in.close(); }
        return b.toByteArray();
    }

    /** Recipe ids that have a photo (the file name keeps the id readable; the index is kept in prefs). */
    static List<String> photoIds(Context c) {
        List<String> out = new ArrayList<String>();
        for (String id : prefs(c).getStringSet("photoIds", new java.util.HashSet<String>())) if (hasPhoto(c, id)) out.add(id);
        return out;
    }

    static void rememberPhoto(Context c, String id) {
        java.util.Set<String> s = new java.util.HashSet<String>(prefs(c).getStringSet("photoIds", new java.util.HashSet<String>()));
        s.add(id);
        prefs(c).edit().putStringSet("photoIds", s).apply();
    }

    static void importExtras(Context c, Backup.Data d) {
        Backup.Data s = new Backup.Data();
        s.prices.putAll(d.prices);
        s.leftovers.addAll(d.leftovers);
        s.cooked.addAll(d.cooked);
        s.barcodes.putAll(d.barcodes);
        SharedPreferences.Editor ed = prefs(c).edit().putString("v16", Backup.write(s));
        Map<String, Object> st = d.settings;
        if (st.get("currency") instanceof String) ed.putString("currency", ((String) st.get("currency")).trim().isEmpty() ? "$" : ((String) st.get("currency")).trim());
        if (st.get("useDefaultPrices") instanceof Boolean) ed.putBoolean("useDefaultPrices", (Boolean) st.get("useDefaultPrices"));
        if (st.get("deductPantry") instanceof Boolean) ed.putBoolean("deductPantry", (Boolean) st.get("deductPantry"));
        if (st.get("leftoverDays") instanceof Number) ed.putInt("leftoverDays", Leftovers.clampDays(((Number) st.get("leftoverDays")).intValue()));
        if (st.get("shopCost") instanceof Boolean) ed.putBoolean("shopCost", (Boolean) st.get("shopCost"));
        ed.commit();
        reset();
        // photos are merged only when the file has them (a backup without photos keeps the ones on this device)
        for (Map.Entry<String, String> e : d.photos.entrySet()) {
            String u = e.getValue();
            int comma = u.indexOf(',');
            if (comma < 0) continue;
            try {
                byte[] bytes = android.util.Base64.decode(u.substring(comma + 1), android.util.Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (b == null) continue;
                Bitmap sc = scaled(b, PHOTO_MAX_PX);
                FileOutputStream out = new FileOutputStream(photoFile(c, e.getKey()));
                sc.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out);
                out.close();
                rememberPhoto(c, e.getKey());
            } catch (Throwable ignored) { }
        }
    }

    // ---------------- sharing ----------------
    static void share(Activity a, String subject, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, text);
        try {
            a.startActivity(Intent.createChooser(i, subject));
        } catch (Exception e) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText(subject, text));
            Ui.toastShort(a, "No app to share with; copied to the clipboard");
        }
    }

    static String shoppingText(Store store) {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (Store.ShopItem s : store.shopping()) rows.add(new Object[]{s.name, s.amount, s.unit == null ? "" : s.unit, s.checked});
        final Context c = store.appContext();
        // v1.8: the shared text uses the same units as the Shopping list on screen
        return org.pantryrecipes.app.core.ShareText.shopping(rows, (name, amount, unit) -> V17.qty(c, name, amount, unit));
    }

    // ---------------- buy these next ----------------
    static View buyNextCard(final MainActivity a, final List<Matcher.Result> results) {
        if (results != null) return BuyNextUi.card(a, results); // v2.1: expandable rows, see all, sort
        final List<BuyNext.Pick> picks = BuyNext.fromResults(results, BuyNext.MAX);
        if (picks.isEmpty()) return null;
        LinearLayout card = Ui.card(a);
        card.addView(head(a, "Buy these next", null));
        card.addView(Ui.caption(a, "The " + (picks.size() == 1 ? "item" : picks.size() + " items")
                + " that unlock the most recipes you're 1\u20132 ingredients away from (your diets, allergies, dislikes and substitutes count)."),
                Ui.lpMargins(a, -1, -2, 0, 2, 0, 8));
        for (final BuyNext.Pick p : picks) {
            LinearLayout row = Ui.row(a);
            row.setPadding(dp(a, 6), dp(a, 8), dp(a, 6), dp(a, 8));
            row.setBackground(Ui.ripple(Ui.rounded(Ui.SELECTED, dp(a, 12)), dp(a, 12)));
            row.addView(Ui.icon(a, R.drawable.ic_cart, Ui.GREEN_DARK, 20));
            LinearLayout tc = Ui.column(a);
            tc.setPadding(dp(a, 12), 0, 0, 0);
            tc.addView(Ui.text(a, Ui.cap(p.name), 15, Ui.TEXT, Ui.MEDIUM));
            tc.addView(Ui.caption(a, "+" + plural(p.unlocks.size(), "recipe", "recipes") + " you can make"));
            row.addView(tc, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(Ui.icon(a, R.drawable.ic_next, Ui.TEXT3, 18));
            row.setContentDescription("Buy " + p.name + ", unlocks " + p.unlocks.size() + " recipes");
            row.setOnClickListener(v -> a.openBuyNext(p));
            card.addView(row, Ui.lpMargins(a, -1, -2, 0, 0, 0, 6));
        }
        return card;
    }
}
