package org.pantryrecipes.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.pantryrecipes.app.core.Recipe;
import org.pantryrecipes.app.core.Sources;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * v2.2 UI: optional recipe photos (Openverse, CC0/PDM/CC BY, with credit), link-outs to recipe sites, Wikibooks
 * Cookbook search and public-domain cookbooks. Every network request starts from a tap.
 */
final class V22 {
    private V22() { }

    static SharedPreferences prefs(MainActivity a) { return a.getSharedPreferences("v22", 0); }

    static Sources.Photo photo(MainActivity a, String id) { return Sources.decode(prefs(a).getString("photo:" + id, "")); }

    static String fetch(String u) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(u).openConnection();
        con.setConnectTimeout(8000); con.setReadTimeout(12000);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "MyPantryRecipes/2.2 (Android; offline-first recipe app)");
        try {
            if (con.getResponseCode() != 200) throw new Exception("HTTP " + con.getResponseCode());
            InputStream in = con.getInputStream();
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            in.close();
            return o.toString("UTF-8");
        } finally { con.disconnect(); }
    }

    static void open(MainActivity a, String url) {
        try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Ui.toastShort(a, "No browser found"); }
    }

    /** Asks once before the first online lookup of a kind (photos, Wikibooks). */
    static void consent(final MainActivity a, final String kind, String what, final Runnable go) {
        if (prefs(a).getBoolean("ok:" + kind, false)) { go.run(); return; }
        Extras.dialog(a).setTitle("Look online?").setMessage(what + "\n\nOnly this search is sent. No account, no tracking. You can remove photos at any time.")
                .setPositiveButton("Continue", (d, w) -> { prefs(a).edit().putBoolean("ok:" + kind, true).apply(); go.run(); })
                .setNegativeButton("Cancel", null).show();
    }

    /** Header photo for a built-in recipe the user chose to illustrate with a licensed photo. Returns the credit line or null. */
    static View headerPhoto(final MainActivity a, FrameLayout header, Recipe r) {
        final Sources.Photo p = photo(a, r.id);
        if (p == null) return null;
        ImageView iv = new ImageView(a);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setContentDescription("Photo for " + r.title + ". " + p.credit());
        header.addView(iv, 0, new FrameLayout.LayoutParams(-1, -1));
        ImageLoader.get(a).load(iv, p.thumb, false);
        TextView cr = Ui.text(a, p.credit(), 12, Ui.TEXT2, Ui.REGULAR);
        cr.setPadding(a.dp(20), a.dp(6), a.dp(20), a.dp(2));
        cr.setMinHeight(a.dp(48));
        cr.setGravity(Gravity.CENTER_VERTICAL);
        cr.setContentDescription(p.credit() + ". Opens the original page");
        cr.setOnClickListener(v -> open(a, p.landing.isEmpty() ? p.url : p.landing));
        return cr;
    }

    /** "Photo and more sources" row on a recipe page. */
    static View sourcesRow(final MainActivity a, final Recipe r) {
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.row(a);
        row.setPadding(a.dp(20), a.dp(4), a.dp(20), a.dp(4));
        boolean builtIn = r.image.isEmpty();
        if (builtIn) {
            final Sources.Photo cur = photo(a, r.id);
            TextView ph = Ui.button(a, cur == null ? "Find a photo" : "Different photo", R.drawable.ic_photo, false);
            ph.setContentDescription(cur == null ? "Find a free-licensed photo online (Openverse)" : "Pick a different free-licensed photo");
            ph.setOnClickListener(v -> consent(a, "photos", "This searches Openverse (openverse.org) for a free-licensed photo (CC0, Public Domain or CC BY) of \u201c" + r.title + "\u201d. The photo is shown with its creator and licence.", () -> findPhoto(a, r, cur == null ? 0 : prefs(a).getInt("pidx:" + r.id, 0) + 1)));
            row.addView(ph, new LinearLayout.LayoutParams(-2, a.dp(48)));
            if (cur != null) {
                row.addView(Ui.space(a, 8, 1));
                TextView rm = Ui.button(a, "Remove photo", R.drawable.ic_delete, false);
                rm.setOnClickListener(v -> { prefs(a).edit().remove("photo:" + r.id).remove("pidx:" + r.id).apply(); a.openDetail(r.id, false); });
                row.addView(rm, new LinearLayout.LayoutParams(-2, a.dp(48)));
            }
            row.addView(Ui.space(a, 8, 1));
        }
        TextView more = Ui.button(a, "Find it elsewhere", R.drawable.ic_search, false);
        more.setContentDescription("Look this recipe up on other recipe sites");
        more.setOnClickListener(v -> linkOuts(a, r.title));
        row.addView(more, new LinearLayout.LayoutParams(-2, a.dp(48)));
        hs.addView(row);
        return hs;
    }

    static void findPhoto(final MainActivity a, final Recipe r, final int idx) {
        Ui.toastShort(a, "Looking for a photo\u2026");
        new Thread(() -> {
            String err = null; Sources.Photo got = null; int n = 0;
            try {
                List<Sources.Photo> l = Sources.parseOpenverse(fetch(Sources.openverseApi(r.title)));
                n = l.size();
                if (!l.isEmpty()) got = l.get(idx % l.size());
            } catch (Exception e) { err = "Couldn't reach Openverse. Check your connection."; }
            final Sources.Photo p = got; final String e2 = err; final int total = n;
            a.runOnUiThread(() -> {
                if (a.isFinishing()) return;
                if (e2 != null) { Ui.toastShort(a, e2); return; }
                if (p == null) { Ui.toastShort(a, "No free-licensed photo found for this recipe"); return; }
                prefs(a).edit().putString("photo:" + r.id, p.encode()).putInt("pidx:" + r.id, idx % Math.max(1, total)).apply();
                a.openDetail(r.id, false);
            });
        }).start();
    }

    static void linkOuts(final MainActivity a, final String q) {
        final int n = Sources.LINKS.length;
        String[] items = new String[n + 1];
        items[0] = "Search Wikibooks Cookbook here (open, CC BY-SA)";
        for (int i = 0; i < n; i++) items[i + 1] = Sources.LINKS[i][1] + "  \u00b7  " + Sources.LINKS[i][2];
        Extras.dialog(a).setTitle("Find \u201c" + q + "\u201d elsewhere")
                .setItems(items, (d, w) -> {
                    if (w == 0) wikibooks(a, q);
                    else open(a, Sources.linkUrl(Sources.LINKS[w - 1][0], q));
                })
                .setNegativeButton("Close", null).show();
    }

    static void wikibooks(final MainActivity a, final String q) {
        consent(a, "wikibooks", "This searches the Wikibooks Cookbook (en.wikibooks.org) for \u201c" + q + "\u201d.", () -> {
            Ui.toastShort(a, "Searching Wikibooks\u2026");
            new Thread(() -> {
                List<Sources.Hit> hits = null;
                try { hits = Sources.parseWikibooks(fetch(Sources.wikibooksApi(q))); } catch (Exception e) { hits = null; }
                final List<Sources.Hit> h = hits;
                a.runOnUiThread(() -> {
                    if (a.isFinishing()) return;
                    if (h == null) { Ui.toastShort(a, "Couldn't reach Wikibooks. Check your connection."); return; }
                    if (h.isEmpty()) { Ui.toastShort(a, "No Wikibooks Cookbook pages match"); return; }
                    String[] items = new String[h.size()];
                    for (int i = 0; i < h.size(); i++) items[i] = h.get(i).title + (h.get(i).snippet.isEmpty() ? "" : "\n" + (h.get(i).snippet.length() > 90 ? h.get(i).snippet.substring(0, 90) + "\u2026" : h.get(i).snippet));
                    Extras.dialog(a).setTitle("Wikibooks Cookbook").setItems(items, (d, w) -> open(a, h.get(w).url))
                            .setMessage(null).setNegativeButton("Close", null).show();
                    Ui.toastShort(a, "Text: Wikibooks contributors, CC BY-SA 4.0");
                });
            }).start();
        });
    }

    /** Discover card: where recipes come from, plus public-domain cookbooks. */
    static View sourcesCard(final MainActivity a) {
        LinearLayout c = Ui.card(a);
        c.addView(Ui.text(a, "More recipe sources", 16, Ui.TEXT, Ui.MEDIUM));
        c.addView(Ui.caption(a, "Open a recipe and tap \u201cFind it elsewhere\u201d to search Wikibooks, BBC Good Food, Serious Eats, Chefkoch, Marmiton and more. Classic public-domain cookbooks (Project Gutenberg):"));
        for (final String[] b : Sources.BOOKS) {
            TextView t = Ui.text(a, b[1] + " \u00b7 " + b[2] + " (" + b[3] + ")", 14, Ui.GREEN_DARK, Ui.MEDIUM);
            t.setMinHeight(a.dp(48)); t.setGravity(Gravity.CENTER_VERTICAL);
            t.setContentDescription(b[1] + " by " + b[2] + ", opens Project Gutenberg");
            t.setOnClickListener(v -> open(a, Sources.bookUrl(b[0])));
            c.addView(t);
        }
        TextView src = Ui.text(a, "Sources and licences", 14, Ui.GREEN_DARK, Ui.MEDIUM);
        src.setMinHeight(a.dp(48)); src.setGravity(Gravity.CENTER_VERTICAL);
        src.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (String[] o : Sources.OPEN) sb.append(o[0]).append(": ").append(o[1]).append("\n\n");
            sb.append("Recipe illustrations are original drawings made by the app. Link-outs open other sites in your browser; those sites have their own privacy policies.");
            Extras.dialog(a).setTitle("Sources and licences").setMessage(sb.toString()).setPositiveButton("OK", null).show();
        });
        c.addView(src);
        return c;
    }
}
