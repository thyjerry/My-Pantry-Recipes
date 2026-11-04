package org.pantryrecipes.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building a consistent, polished UI in code. */
final class Ui {
    private Ui() { }

    // Theme colours. Not final: Ui.load() fills them from the active Palette (see core/Palette.java).
    static int GREEN = 0xFF2E9E5B;        // primary (icons, bars, accents)
    static int GREEN_DARK = 0xFF287547;   // primary text/links on cards (AA)
    static int GREEN_TINT = 0xFFE4F4E9;
    static int GREEN_TINT2 = 0xFFCDEBD7;
    static int BG = 0xFFF5F7F2;
    static int CARD = 0xFFFFFFFF;         // surface
    static int TEXT = 0xFF1E2A22;
    static int TEXT2 = 0xFF66756B;
    static int TEXT3 = 0xFF666E68;   // v2.0: AA for hint text
    static int LINE = 0xFFE6EBE4;
    static int AMBER = 0xFFD9822B;        // accent
    static int AMBER_TINT = 0xFFFDF1E4;
    static int AMBER_TEXT = 0xFF8A4B0F;
    static int RIPPLE = 0x332E9E5B;
    static int FILL = 0xFF2E9E5B, ON_FILL = 0xFFFFFFFF, ON_FILL2 = 0xE6FFFFFF, HERO_END = 0xFF1E7A44;
    static int DANGER = 0xFFC2410C, INFO = 0xFF2C7FB0, INFO_TINT = 0xFFEAF5FB, NEUTRAL = 0xFFF0F3EE, SELECTED = 0xFFF3FAF5;
    static final int STAR = 0xFFF2B33D, HEART = 0xFFE0475B;
    static boolean DARK = false;
    static String THEME_ID = "";
    static int themeGeneration = 0;
    /** Corner-roundness factor from Appearance (sharp / normal / round); applied in rounded(). */
    static float RAD = 1f;

    static int opaque(int rgb) { return 0xFF000000 | rgb; }

    static void apply(org.pantryrecipes.app.core.Palette.Colors c, String id) {
        GREEN = opaque(c.primary); GREEN_DARK = opaque(c.primaryText); GREEN_TINT = opaque(c.tint); GREEN_TINT2 = opaque(c.tint2);
        BG = opaque(c.bg); CARD = opaque(c.surface); TEXT = opaque(c.text); TEXT2 = opaque(c.text2); TEXT3 = opaque(c.text3);
        LINE = opaque(c.line); AMBER = opaque(c.accent); AMBER_TINT = opaque(c.accentTint); AMBER_TEXT = opaque(c.accentText);
        RIPPLE = 0x33000000 | c.primary; FILL = opaque(c.fill); ON_FILL = opaque(c.onFill); ON_FILL2 = opaque(c.onFill2);
        HERO_END = opaque(c.heroEnd); DANGER = opaque(c.danger); INFO = opaque(c.info); INFO_TINT = opaque(c.infoTint);
        NEUTRAL = opaque(c.neutral); SELECTED = opaque(c.selected); DARK = c.dark;
        if (!id.equals(THEME_ID)) themeGeneration++;
        THEME_ID = id;
    }

    static boolean systemDark(Context c) {
        return (c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    static org.pantryrecipes.app.core.Palette.State themeState(Context c) {
        return Store.get(c).themeState();
    }

    /** Loads the saved theme into the colour fields; returns a key that changes when the visible theme changes. */
    static String load(Context c) {
        org.pantryrecipes.app.core.Palette.State st = themeState(c);
        org.pantryrecipes.app.core.Palette.Base b = st.resolve(systemDark(c));
        String key = st.active + "|" + b.id + "|" + b.bg + "," + b.surface + "," + b.primary + "," + b.accent + "," + b.text
                + "|" + st.font + "|" + st.corners;
        RAD = (float) st.cornerScale();
        apply(org.pantryrecipes.app.core.Palette.derive(b), key);
        return key;
    }

    /** Context whose font scale includes the Appearance text size (call from attachBaseContext). */
    static Context fontContext(Context base) {
        try {
            double f = org.pantryrecipes.app.core.Palette.parseState(
                    base.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("theme", "{}")).fontScale();
            if (Math.abs(f - 1.0) < 1e-6) return base;
            android.content.res.Configuration conf = new android.content.res.Configuration(base.getResources().getConfiguration());
            conf.fontScale = (float) (conf.fontScale * f);
            return base.createConfigurationContext(conf);
        } catch (RuntimeException e) {
            return base;
        }
    }

    /** Base platform theme (widgets such as checkboxes, text fields and pickers) plus bar colours. */
    static void themeActivity(android.app.Activity a) {
        load(a);
        a.setTheme(DARK ? R.style.AppThemeDark : R.style.AppTheme);
    }

    /** Tints platform widgets (text fields, checkboxes, sliders, spinners) with the theme's primary colour. */
    static void tintTree(View v) {
        if (v == null) return;
        if (v instanceof android.widget.CompoundButton) {
            android.widget.CompoundButton cb = (android.widget.CompoundButton) v;
            if (tinted(cb.getButtonTintList())) return;
            cb.setButtonTintList(ColorStateList.valueOf(GREEN));
            if (cb.getCurrentTextColor() == 0xFF000000 || cb.getCurrentTextColor() == 0xDE000000) cb.setTextColor(TEXT);
        } else if (v instanceof android.widget.EditText) {
            if (!tinted(v.getBackgroundTintList())) v.setBackgroundTintList(ColorStateList.valueOf(GREEN));
        } else if (v instanceof android.widget.ProgressBar) {
            if (tinted(((android.widget.ProgressBar) v).getProgressTintList())) return;
            ((android.widget.ProgressBar) v).setProgressTintList(ColorStateList.valueOf(GREEN));
            ((android.widget.ProgressBar) v).setIndeterminateTintList(ColorStateList.valueOf(GREEN));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int k = 0; k < g.getChildCount(); k++) tintTree(g.getChildAt(k));
        }
    }

    private static boolean tinted(ColorStateList l) { return l != null && l.getDefaultColor() == GREEN; }

    /** Keeps widgets added later (dialogs, rebuilt screens) tinted too. */
    static void autoTint(android.view.Window w) {
        final View d = w.getDecorView();
        d.getViewTreeObserver().addOnGlobalLayoutListener(() -> tintTree(d));
    }

    static void styleWindow(android.app.Activity a) {
        android.view.Window w = a.getWindow();
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
        // before API 23 status-bar icons are always white, so light themes get a darkened bar
        w.setStatusBarColor(DARK || android.os.Build.VERSION.SDK_INT >= 23 ? BG : opaque(org.pantryrecipes.app.core.Palette.mix(GREEN & 0xFFFFFF, 0, 0.35)));
        // light navigation-bar icons need API 26; older light themes keep a dark bar so its icons stay visible
        w.setNavigationBarColor(DARK || android.os.Build.VERSION.SDK_INT >= 26 ? CARD : 0xFF1A1F1B);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            View d = w.getDecorView();
            int f = d.getSystemUiVisibility();
            f = DARK ? (f & ~0x2000) : (f | 0x2000);   // SYSTEM_UI_FLAG_LIGHT_STATUS_BAR (API 23)
            if (android.os.Build.VERSION.SDK_INT >= 26) f = DARK ? (f & ~0x10) : (f | 0x10); // LIGHT_NAVIGATION_BAR
            d.setSystemUiVisibility(f);
        }
        autoTint(w);
        fitInsets(a);
    }

    /** v2.0.1: keep content out from under the status bar, navigation/gesture bar and display cutout. Android 15 enforces
     *  edge-to-edge for targetSdk 35 (modern build), so the window draws behind the bars and nothing pads the content.
     *  Older/non-edge-to-edge windows deliver already-consumed (zero) insets here, so this is a no-op for them.
     *  Pure framework API (View.setOnApplyWindowInsetsListener, API 20); the cutout part is guarded for API 28. */
    static void fitInsets(android.app.Activity a) {
        final View c = a.findViewById(android.R.id.content);
        if (c == null) return;
        c.setOnApplyWindowInsetsListener((v, in) -> {
            int[] p = { in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(), in.getSystemWindowInsetRight(), in.getSystemWindowInsetBottom() };
            if (android.os.Build.VERSION.SDK_INT >= 28) cutout(in, p);
            v.setPadding(p[0], p[1], p[2], p[3]);
            return in.consumeSystemWindowInsets();
        });
        c.requestApplyInsets();
    }

    static void cutout(android.view.WindowInsets in, int[] p) {
        android.view.DisplayCutout d = in.getDisplayCutout();
        if (d == null) return;
        p[0] = Math.max(p[0], d.getSafeInsetLeft()); p[1] = Math.max(p[1], d.getSafeInsetTop());
        p[2] = Math.max(p[2], d.getSafeInsetRight()); p[3] = Math.max(p[3], d.getSafeInsetBottom());
    }

    static int dialogTheme() {
        return DARK ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert;
    }

    /** AlertDialog builder whose dialogs use the theme's surface, text and primary colours. */
    static final class Dialog extends android.app.AlertDialog.Builder {
        final Context ctx;
        Dialog(Context c) { super(c, dialogTheme()); ctx = c; }
        @Override
        public android.app.AlertDialog show() {
            android.app.AlertDialog d = super.show();
            style(ctx, d);
            return d;
        }
    }

    static void style(Context c, android.app.AlertDialog d) {
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(rounded(CARD, dp(c, 16)));
        int[] ids = {android.app.AlertDialog.BUTTON_POSITIVE, android.app.AlertDialog.BUTTON_NEGATIVE, android.app.AlertDialog.BUTTON_NEUTRAL};
        for (int id : ids) {
            android.widget.Button b = d.getButton(id);
            if (b != null) b.setTextColor(GREEN_DARK);
        }
        int tid = c.getResources().getIdentifier("alertTitle", "id", "android");
        View t = tid == 0 ? null : d.findViewById(tid);
        if (t instanceof TextView) ((TextView) t).setTextColor(TEXT);
        View m = d.findViewById(android.R.id.message);
        if (m instanceof TextView) ((TextView) m).setTextColor(TEXT);
        if (d.getListView() != null) d.getListView().setBackgroundColor(CARD);
        if (d.getWindow() != null) autoTint(d.getWindow());
    }

    static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx * RAD);
        return g;
    }

    static GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    static GradientDrawable outlined(int fill, int stroke, int strokePx, float radiusPx) {
        GradientDrawable g = rounded(fill, radiusPx);
        g.setStroke(strokePx, stroke);
        return g;
    }

    /** Background with ripple feedback, clipped to the rounded shape. */
    static Drawable ripple(Drawable content, float radiusPx) {
        return new RippleDrawable(ColorStateList.valueOf(RIPPLE), content, rounded(Color.WHITE, radiusPx));
    }

    static Drawable rippleRound(int color, float radiusPx) {
        return ripple(rounded(color, radiusPx), radiusPx);
    }

    static TextView text(Context c, String s, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(true);
        return t;
    }

    static TextView title(Context c, String s, float sp) {
        TextView t = text(c, s, sp, TEXT, MEDIUM);
        t.setLetterSpacing(-0.005f);
        return t;
    }

    static TextView body(Context c, String s) {
        TextView t = text(c, s, 15, TEXT, REGULAR);
        t.setLineSpacing(0, 1.2f);
        return t;
    }

    static TextView caption(Context c, String s) {
        return text(c, s, 13, TEXT2, REGULAR);
    }

    static TextView sectionLabel(Context c, String s) {
        TextView t = text(c, s.toUpperCase(), 12, TEXT2, MEDIUM);
        t.setLetterSpacing(0.08f);
        Deco.underline(c, t);
        return t;
    }

    static ImageView icon(Context c, int res, int color, int sizeDp) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(color);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return iv;
    }

    /** Round icon button with ripple. */
    static ImageView iconButton(Context c, int res, int color, String desc) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(color);
        int p = dp(c, 10);
        iv.setPadding(p, p, p, p);
        iv.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE), null, null));
        iv.setContentDescription(desc);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48))); // v2.0 a11y: 48dp
        return iv;
    }

    /** Filled pill button. */
    static TextView button(Context c, String label, int iconRes, boolean filled) {
        TextView b = text(c, label, 15, filled ? ON_FILL : GREEN_DARK, MEDIUM);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        int r = dp(c, 24);
        b.setBackground(ripple(filled ? rounded(FILL, r) : outlined(CARD, GREEN_TINT2, dp(c, 1.5f), r), r));
        b.setPadding(dp(c, 18), 0, dp(c, 20), 0);
        b.setMinHeight(dp(c, 48));
        b.setMinimumHeight(dp(c, 48));
        if (iconRes != 0) {
            Drawable d = c.getResources().getDrawable(iconRes).mutate();
            d.setTint(filled ? ON_FILL : GREEN_DARK);
            int s = dp(c, 20);
            d.setBounds(0, 0, s, s);
            b.setCompoundDrawables(d, null, null, null);
            b.setCompoundDrawablePadding(dp(c, 8));
        }
        return b;
    }

    /** Selectable filter chip. */
    /** v1.8: a chip remembers its state so restyling an unchanged chip costs nothing. */
    static final class ChipView extends TextView {
        int state = -1;
        ChipView(Context c) { super(c); }
    }

    /** v2.0 a11y: draws a 36dp pill inside a 48dp-tall touch target. */
    static Drawable tall(Context c, Drawable d) { int v = dp(c, 6); return new TallInset(d, v); }

    /** v2.0.1: an InsetDrawable that does not report its insets as padding. A plain InsetDrawable made View.setBackground()
     *  replace the chip's own 14dp side padding with (0, 6dp, 0, 6dp), so text touched the pill border (v2.0 regression). */
    static final class TallInset extends InsetDrawable {
        TallInset(Drawable d, int v) { super(d, 0, v, 0, v); }
        @Override public boolean getPadding(android.graphics.Rect padding) { padding.set(0, 0, 0, 0); return false; }
    }

    /** v2.0.1: chip text padding. Vertical padding = 6dp inset + 4dp, so large font scales (130-150%) grow the chip
     *  instead of clipping at the pill edge. */
    static void chipPadding(Context c, View t, int left, int right) { t.setPadding(left, dp(c, 10), right, dp(c, 10)); }

    static TextView chip(Context c, String label, boolean selected) {
        TextView t = new ChipView(c);
        t.setText(label);
        t.setTextSize(14);
        t.setTextColor(TEXT);
        t.setTypeface(REGULAR);
        t.setGravity(Gravity.CENTER);
        // v2.0.1: wraps to a second line instead of clipping when a long name meets a narrow row or a big font scale
        t.setSingleLine(false);
        t.setMaxLines(3);
        t.setIncludeFontPadding(true);
        chipPadding(c, t, dp(c, 14), dp(c, 14));
        t.setMinHeight(dp(c, 48)); // v2.0 a11y: 48dp touch target, 36dp drawn (see tall())
        styleChip(c, t, selected);
        return t;
    }

    static void styleChip(Context c, TextView t, boolean selected) {
        if (t instanceof ChipView) {
            int st = selected ? 1 : 0;
            if (((ChipView) t).state == st) return;
            ((ChipView) t).state = st;
        }
        int r = dp(c, 18);
        t.setSelected(selected); // TalkBack announces "selected"
        if (selected) {
            t.setBackground(tall(c, ripple(rounded(FILL, r), r)));
            t.setTextColor(ON_FILL);
            t.setTypeface(MEDIUM);
        } else {
            t.setBackground(tall(c, ripple(outlined(CARD, LINE, dp(c, 1), r), r)));
            t.setTextColor(TEXT);
            t.setTypeface(REGULAR);
        }
    }

    /** Rounded surface card (flat, outlined; no elevation shadow since v1.8). */
    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(outlined(CARD, LINE, dp(c, 1), dp(c, 16))); // v1.8: hairline outline instead of a shadow
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams lpMargins(Context c, int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static View space(Context c, int wDp, int hDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp)));
        return v;
    }

    static View flex(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        return v;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1))));
        return v;
    }

    static void ellipsize(TextView t, int lines) {
        t.setMaxLines(lines);
        t.setEllipsize(TextUtils.TruncateAt.END);
    }

    /** Soft color for a placeholder tile, derived from the category. */
    static int categoryColor(String category) {
        switch (category == null ? "" : category) {
            case "Breakfast": return 0xFFF6C453;
            case "Soup": return 0xFFEF8354;
            case "Salad": return 0xFF7BC67E;
            case "Side": return 0xFF9CCC65;
            case "Snack": return 0xFFFFB74D;
            case "Dessert": return 0xFFF48FB1;
            case "Baking": return 0xFFD7A86E;
            case "Main": return 0xFF4DB6AC;
            case "Drink": return 0xFF64B5F6;
            case "Sauce": return 0xFFE57373;
            default: {
                int[] cols = {0xFF4DB6AC, 0xFF81C784, 0xFFFFB74D, 0xFF9575CD, 0xFF64B5F6, 0xFFE57373};
                return cols[Math.abs((category == null ? "" : category).hashCode()) % cols.length];
            }
        }
    }

    static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String timeText(int minutes) {
        if (minutes < 0) return "Time not listed";
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60, m = minutes % 60;
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    static String dietLabel(String tag) {
        switch (tag) {
            case "vegetarian": return "Vegetarian";
            case "vegan": return "Vegan";
            case "gluten-free": return "Gluten-free";
            case "dairy-free": return "Dairy-free";
            default: return cap(tag);
        }
    }

    static void toastShort(Context c, String s) { android.widget.Toast.makeText(c, s, android.widget.Toast.LENGTH_SHORT).show(); }
}
