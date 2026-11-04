package org.pantryrecipes.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * v1.8 "Decorations": static, theme-coloured accents that cost next to nothing. No images, no animation,
 * no shadows on list rows and no drawables created while a list row is bound: glyphs are plain text, and
 * the few shapes are made once per colour and shared through their ConstantState.
 * Emoji are used only when the device's font draws them in colour (checked once and remembered per
 * system build); otherwise the category and meal glyphs are left out and the coloured dots remain.
 */
final class Deco {
    private Deco() { }

    private static Boolean on;
    private static Boolean emoji;

    static boolean on(Context c) {
        if (on == null) on = prefs(c).getBoolean("decorations", true);
        return on;
    }

    static void setOn(Context c, boolean v) {
        on = v;
        prefs(c).edit().putBoolean("decorations", v).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    /**
     * Whether colour emoji render here. Android 5.x ships an emoji font, but some Fire OS builds replace
     * fonts, so draw one Unicode 6.0 emoji into a 24x24 bitmap once and look for coloured pixels.
     */
    static synchronized boolean emojiOk(Context c) {
        if (emoji != null) return emoji;
        SharedPreferences p = prefs(c);
        String fp = Build.FINGERPRINT == null ? "" : Build.FINGERPRINT;
        if (fp.equals(p.getString("emojiFp", null))) return emoji = p.getBoolean("emojiOk", false);
        boolean ok = false;
        try {
            Bitmap b = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
            pt.setColor(Color.BLACK);
            pt.setTextSize(20);
            cv.drawText("\uD83C\uDF4E", 1, 20, pt); // red apple, Unicode 6.0
            for (int y = 0; y < 24 && !ok; y++) for (int x = 0; x < 24; x++) {
                int px = b.getPixel(x, y);
                int r = Color.red(px), g = Color.green(px), bl = Color.blue(px);
                if (Color.alpha(px) > 64 && Math.max(r, Math.max(g, bl)) - Math.min(r, Math.min(g, bl)) > 60) { ok = true; break; }
            }
            b.recycle();
        } catch (Throwable t) {
            ok = false;
        }
        p.edit().putString("emojiFp", fp).putBoolean("emojiOk", ok).apply();
        return emoji = ok;
    }

    // Unicode 6.0 emoji only (all in the Android 4.4+/5.x Noto Color Emoji font).
    private static final String[] CATEGORY = {
            "\uD83C\uDF3D", "\uD83C\uDF4E", "\uD83C\uDF3F", "\uD83D\uDD25", "\uD83C\uDF56", "\uD83C\uDF57", "\uD83D\uDC1F",
            "\uD83D\uDC16", "\uD83D\uDC04", "\uD83C\uDF55", "\uD83C\uDF31", "\uD83C\uDF5A", "\uD83C\uDF5D", "\uD83C\uDF5E",
            "\uD83C\uDF72", "\uD83C\uDF30", "\uD83C\uDF70", "\uD83C\uDF6F", "\uD83C\uDF76", "\uD83C\uDF45", "\uD83C\uDF71",
            "\uD83C\uDF67", "\uD83C\uDF6A", "\uD83C\uDF79", "\uD83C\uDF5C", "\uD83C\uDF4B", "\uD83C\uDF5B", "\uD83C\uDF46",
            "\uD83C\uDF77"};

    private static final Map<String, String> MEAL = new HashMap<String, String>();
    static {
        MEAL.put("Breakfast", "\uD83C\uDF73");
        MEAL.put("Brunch", "\u2615");
        MEAL.put("Lunch", "\uD83C\uDF71");
        MEAL.put("Dinner", "\uD83C\uDF74");
        MEAL.put("Main dish", "\uD83C\uDF5B");
        MEAL.put("Side dish", "\uD83C\uDF5A");
        MEAL.put("Appetizer", "\uD83C\uDF62");
        MEAL.put("Snack", "\uD83C\uDF6A");
        MEAL.put("Soup", "\uD83C\uDF72");
        MEAL.put("Salad", "\uD83C\uDF43");
        MEAL.put("Dessert", "\uD83C\uDF68");
        MEAL.put("Baked goods", "\uD83C\uDF69");
        MEAL.put("Drink", "\uD83C\uDF79");
        MEAL.put("Sauce/Condiment", "\uD83C\uDF45");
    }

    /** "🌽 " for a catalog category, or "" when decorations are off or emoji do not render. */
    static String category(Context c, int idx) {
        if (!on(c) || !emojiOk(c) || idx < 0 || idx >= CATEGORY.length) return "";
        return CATEGORY[idx] + " ";
    }

    /** Glyph plus a space for a meal type, or "". */
    static String meal(Context c, String meal) {
        if (!on(c) || !emojiOk(c)) return "";
        String g = MEAL.get(meal);
        return g == null ? "" : g + " ";
    }

    /** Empty-state lead glyph: a rice bowl emoji, or the plain symbol U+2727 when emoji do not render. */
    static String emptyGlyph(Context c) {
        if (!on(c)) return "";
        return emojiOk(c) ? "\uD83C\uDF5A  " : "\u2727  ";
    }

    // ---------------- shared shapes (one per colour) ----------------
    private static final Map<Integer, Drawable.ConstantState> SHAPES = new HashMap<Integer, Drawable.ConstantState>();

    /** A small rounded bar or pill of the given colour and size; one shape per (colour, size), shared. */
    static Drawable bar(Context c, int color, int wDp, int hDp) {
        int key = color * 31 + wDp * 1009 + hDp;
        Drawable.ConstantState cs = SHAPES.get(key);
        if (cs == null) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(color);
            g.setCornerRadius(Ui.dp(c, Math.min(wDp, hDp)) / 2f);
            g.setSize(Ui.dp(c, wDp), Ui.dp(c, hDp));
            cs = g.getConstantState();
            SHAPES.put(key, cs);
        }
        return cs.newDrawable();
    }

    /** Section header with a short accent underline (a compound drawable, no extra view). */
    static void underline(Context c, TextView t) {
        if (!on(c)) return;
        Drawable d = bar(c, Ui.GREEN, 22, 2);
        d.setBounds(0, 0, Ui.dp(c, 22), Ui.dp(c, 2));
        t.setCompoundDrawablesRelative(null, null, null, d);
        t.setCompoundDrawablePadding(Ui.dp(c, 4));
    }

    /** The main screen's header band: one flat theme-tint fill (no gradient, no bitmap). */
    static Drawable headerBand(Context c) {
        if (!on(c)) return null;
        return new android.graphics.drawable.ColorDrawable(Ui.GREEN_TINT);
    }
}
