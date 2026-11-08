package org.pantryrecipes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.pantryrecipes.app.core.Palette;

import java.util.ArrayList;
import java.util.List;

/** Settings > Appearance: light/dark/system, preset palettes and a custom colour editor (no external libraries). */
public class AppearanceActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) { super.attachBaseContext(Ui.fontContext(base)); }

    static final String[] ROLES = {"Primary", "Accent", "Background", "Surface", "Text"};
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    Store store;
    Palette.State state;
    LinearLayout content;

    // editor state
    String editId;          // null = new custom theme
    int[] edit = new int[5]; // primary, accent, bg, surface, text
    int role = 0;
    boolean syncing;
    LinearLayout preview;
    TextView report;
    EditText hexIn, nameIn;
    final SeekBar[] hsv = new SeekBar[3], rgb = new SeekBar[3];
    final List<TextView> roleChips = new ArrayList<TextView>();
    final TextView[] sliderVals = new TextView[6];

    int dp(float v) { return Ui.dp(this, v); }

    @Override
    protected void onCreate(Bundle saved) {
        Ui.themeActivity(this);
        super.onCreate(saved);
        Ui.styleWindow(this);
        store = Store.get(this);
        state = store.themeState();
        String e = saved == null ? null : saved.getString("editing");
        if (saved != null && saved.getIntArray("edit") != null && e != null) {
            edit = saved.getIntArray("edit");
            editId = e.isEmpty() ? null : e;
            role = saved.getInt("role", 0);
            showEditor(saved.getString("name", ""));
        } else {
            showList();
        }
        if (saved != null) {
            listY = saved.getInt("scrollY", 0);
            if (!editing) restoreScroll(listY);
        }
    }

    boolean editing;
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        // keep the list's scroll position across recreate() (theme / text-size changes) and rotation
        out.putInt("scrollY", editing || scroll == null ? listY : scroll.getScrollY());
        if (editing) {
            out.putString("editing", editId == null ? "" : editId);
            out.putIntArray("edit", edit);
            out.putInt("role", role);
            out.putString("name", nameIn == null ? "" : nameIn.getText().toString());
        }
    }

    @Override
    public void onBackPressed() {
        if (editing) relist(); else super.onBackPressed();
    }

    LinearLayout frame(String title) {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        LinearLayout bar = Ui.row(this);
        bar.setPadding(dp(4), dp(6), dp(16), dp(6));
        View back = Ui.iconButton(this, R.drawable.ic_back, Ui.TEXT, "Back");
        back.setOnClickListener(v -> onBackPressed());
        bar.addView(back);
        TextView t = Ui.title(this, title, 20);
        bar.addView(t, Ui.lpMargins(this, 0, WRAP, 8, 0, 0, 0));
        ((LinearLayout.LayoutParams) t.getLayoutParams()).weight = 1;
        root.addView(bar, Ui.lp(MATCH, WRAP));
        ScrollView sv = new ScrollView(this);
        scroll = sv;
        content = Ui.column(this);
        content.setPadding(dp(16), dp(4), dp(16), dp(24));
        sv.addView(content, new ScrollView.LayoutParams(MATCH, WRAP));
        root.addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1));
        setContentView(root);
        return content;
    }

    ScrollView scroll;
    /** Scroll position of the theme list; survives recreate() via onSaveInstanceState (v1.6 fix: no jump to the top). */
    int listY;

    void persist(boolean restyle) {
        if (!editing && scroll != null) listY = scroll.getScrollY();   // remember the scrollY before restyling
        store.saveTheme(state);
        if (restyle) recreate();   // onSaveInstanceState stores listY; onCreate restores it
    }

    /** Scrolls the list back to y once it has been laid out (scrollTo before layout would clamp to 0). */
    void restoreScroll(final int y) {
        final ScrollView sv = scroll;
        if (sv == null || y <= 0) return;
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            int tries;
            @Override
            public void onGlobalLayout() {
                View ch = sv.getChildAt(0);
                if (ch != null && (ch.getHeight() > 0 || ++tries > 5)) {
                    sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    sv.scrollTo(0, y);
                }
            }
        });
    }

    void use(String id) {
        state.active = id;
        persist(true);
    }

    // ---------------- list ----------------
    /** Rebuilds the list where the user was (back from the editor, after a delete). */
    void relist() {
        int y = !editing && scroll != null ? scroll.getScrollY() : listY;
        showList();
        listY = y;
        restoreScroll(y);
    }

    void showList() {
        editing = false;
        LinearLayout c = frame("Appearance");
        // v2.2: live preview of the theme in use, then the big gallery, then fine-tuning
        c.addView(Ui.sectionLabel(this, "Live preview"), Ui.lpMargins(this, MATCH, WRAP, 4, 8, 0, 8));
        LinearLayout live = Ui.column(this);
        renderPreview(live, state.resolve(Ui.systemDark(this)), null);
        live.setContentDescription("Preview of the theme in use");
        c.addView(live, Ui.lp(MATCH, WRAP));
        c.addView(Ui.caption(this, "Tap a theme below: the app and this preview change at once."), Ui.lpMargins(this, MATCH, WRAP, 4, 6, 4, 0));
        // v2.0: group chips; only the chosen group's swatches are built (keeps the Fire HD 8 fast with 130 presets)
        final String cur = state.active;
        if (shownGroup == null) { Palette.Base pb = null; for (Palette.Base b : Palette.PRESETS) if (b.id.equals(cur)) pb = b; shownGroup = pb != null ? pb.group : Palette.GROUPS.get(0); }
        c.addView(Ui.sectionLabel(this, "Themes \u00b7 " + Palette.PRESETS.size()), Ui.lpMargins(this, MATCH, WRAP, 4, 20, 0, 8));
        final FlowLayout gchips = new FlowLayout(this, dp(8), dp(8));
        final LinearLayout gbox = Ui.column(this);
        final Runnable[] fill = new Runnable[1];
        fill[0] = () -> {
            gchips.removeAllViews();
            for (final String g : Palette.GROUPS) {
                List<Palette.Base> items = Palette.inGroup(g);
                if (items.isEmpty()) continue;
                TextView ch = Ui.chip(this, g + "  " + items.size(), g.equals(shownGroup));
                ch.setOnClickListener(v -> { shownGroup = g; fill[0].run(); });
                gchips.addView(ch);
            }
            gbox.removeAllViews();
            gbox.addView(grid(Palette.inGroup(shownGroup), cur), Ui.lp(MATCH, WRAP));
        };
        fill[0].run();
        c.addView(gchips, Ui.lp(MATCH, WRAP));
        c.addView(gbox, Ui.lpMargins(this, MATCH, WRAP, 0, 10, 0, 0));

        c.addView(Ui.sectionLabel(this, "Mode"), Ui.lpMargins(this, MATCH, WRAP, 4, 20, 0, 8));
        FlowLayout modes = new FlowLayout(this, dp(8), dp(8));
        String[][] m = {{Palette.SYSTEM, "Follow system"}, {Palette.LIGHT, "Light"}, {Palette.DARK, "Dark"}};
        for (String[] x : m) {
            TextView ch = Ui.chip(this, x[1], state.active.equals(x[0]));
            ch.setOnClickListener(v -> use(x[0]));
            modes.addView(ch);
        }
        TextView rnd = Ui.chip(this, "Random theme", false);
        Drawable sh = getResources().getDrawable(R.drawable.ic_shuffle).mutate();
        sh.setTint(Ui.GREEN_DARK);
        sh.setBounds(0, 0, dp(18), dp(18));
        rnd.setCompoundDrawables(sh, null, null, null);
        rnd.setCompoundDrawablePadding(dp(6));
        rnd.setContentDescription("Random theme");
        rnd.setOnClickListener(v -> use(Palette.randomPreset(state.active, Math.random())));
        modes.addView(rnd);
        c.addView(modes, Ui.lp(MATCH, WRAP));
        TextView cap = Ui.caption(this, "Follow system switches between Light and Dark with your device's dark-theme setting.");
        c.addView(cap, Ui.lpMargins(this, MATCH, WRAP, 4, 8, 4, 0));

        c.addView(Ui.sectionLabel(this, "Text size"), Ui.lpMargins(this, MATCH, WRAP, 4, 18, 0, 8));
        FlowLayout fonts = new FlowLayout(this, dp(8), dp(8));
        for (int k = 0; k < Palette.FONT_IDS.length; k++) {
            final String id = Palette.FONT_IDS[k];
            TextView ch = Ui.chip(this, Palette.FONT_NAMES[k], state.font.equals(id));
            ch.setOnClickListener(v -> { if (!state.font.equals(id)) { state.font = id; persist(true); } });
            fonts.addView(ch);
        }
        c.addView(fonts, Ui.lp(MATCH, WRAP));

        c.addView(Ui.sectionLabel(this, "Corners"), Ui.lpMargins(this, MATCH, WRAP, 4, 18, 0, 8));
        FlowLayout corners = new FlowLayout(this, dp(8), dp(8));
        for (int k = 0; k < Palette.CORNER_IDS.length; k++) {
            final String id = Palette.CORNER_IDS[k];
            TextView ch = Ui.chip(this, Palette.CORNER_NAMES[k], state.corners.equals(id));
            ch.setOnClickListener(v -> { if (!state.corners.equals(id)) { state.corners = id; persist(true); } });
            corners.addView(ch);
        }
        c.addView(corners, Ui.lp(MATCH, WRAP));

        // v1.8: static accents (category and meal glyphs, header band, coloured edges); off = plain
        c.addView(Ui.sectionLabel(this, "Decorations"), Ui.lpMargins(this, MATCH, WRAP, 4, 18, 0, 8));
        FlowLayout deco = new FlowLayout(this, dp(8), dp(8));
        final boolean dOn = Deco.on(this);
        TextView dYes = Ui.chip(this, "On", dOn), dNo = Ui.chip(this, "Off (plain)", !dOn);
        dYes.setOnClickListener(v -> { if (!Deco.on(this)) { Deco.setOn(this, true); persist(true); } });
        dNo.setOnClickListener(v -> { if (Deco.on(this)) { Deco.setOn(this, false); persist(true); } });
        deco.addView(dYes);
        deco.addView(dNo);
        c.addView(deco, Ui.lp(MATCH, WRAP));
        c.addView(Ui.caption(this, "Small food glyphs (when your device shows colour emoji), coloured edges and a tinted header. They cost no measurable speed."),
                Ui.lpMargins(this, MATCH, WRAP, 4, 8, 4, 0));

        c.addView(Ui.sectionLabel(this, "Accent colour"), Ui.lpMargins(this, MATCH, WRAP, 4, 18, 0, 4));
        c.addView(Ui.caption(this, "Replaces the theme's main colour (buttons, links, highlights). Contrast is kept at AA."), Ui.lpMargins(this, MATCH, WRAP, 4, 0, 4, 8));
        FlowLayout acc = new FlowLayout(this, dp(10), dp(10));
        acc.addView(accentDot(-1));
        for (int a : Palette.QUICK_ACCENTS) acc.addView(accentDot(a));
        c.addView(acc, Ui.lp(MATCH, WRAP));

        c.addView(Ui.sectionLabel(this, "Your themes"), Ui.lpMargins(this, MATCH, WRAP, 4, 20, 0, 8));
        if (state.custom.isEmpty()) {
            c.addView(Ui.caption(this, "No custom themes yet. Create one from the current colours."), Ui.lpMargins(this, MATCH, WRAP, 4, 0, 4, 8));
        }
        for (Palette.Base b : state.custom) c.addView(swatchRow(b, true), Ui.lpMargins(this, MATCH, WRAP, 0, 0, 0, 8));
        TextView create = Ui.button(this, "Customise current", R.drawable.ic_add, true);
        create.setOnClickListener(v -> {
            if (state.custom.size() >= Palette.MAX_CUSTOM) { Ui.toastShort(this, "You can keep up to " + Palette.MAX_CUSTOM + " custom themes."); return; }
            Palette.Base base = state.resolve(Ui.systemDark(this));
            edit = new int[]{base.primary, base.accent, base.bg, base.surface, base.text};
            editId = null; role = 0;
            showEditor("My theme");
        });
        c.addView(create, Ui.lpMargins(this, WRAP, WRAP, 0, 4, 0, 0));

        TextView reset = Ui.button(this, "Reset to default", 0, false);
        reset.setOnClickListener(v -> new Ui.Dialog(this).setTitle("Reset appearance?")
                .setMessage("Switches back to the default Light theme with normal text size, corners and accent. Your saved custom themes are kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d, w) -> {
                    state.font = "normal"; state.corners = "normal"; state.accentPick = -1;
                    use(Palette.LIGHT);
                }).show());
        c.addView(reset, Ui.lpMargins(this, WRAP, WRAP, 0, 24, 0, 0));
        c.addView(Ui.caption(this, Palette.PRESETS.size() + " built-in themes. Every one passes WCAG AA contrast for text, buttons and links."),
                Ui.lpMargins(this, MATCH, WRAP, 4, 16, 4, 0));
    }

    static String shownGroup;

    View accentDot(final int rgb) {
        boolean on = state.accentPick == rgb;
        FrameLayout f = new FrameLayout(this);
        int size = dp(40);
        View dot = new View(this);
        GradientDrawable g;
        if (rgb < 0) {
            Palette.Base b = Palette.preset(state.active);
            if (b == null) b = state.resolve(Ui.systemDark(this));
            g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Ui.opaque(b.primary), Ui.opaque(b.accent)});
            g.setShape(GradientDrawable.OVAL);
        } else {
            g = Ui.circle(Ui.opaque(rgb));
        }
        if (on) g.setStroke(dp(3), Ui.TEXT);
        else g.setStroke(Math.max(1, dp(1)), Ui.LINE);
        dot.setBackground(g);
        f.addView(dot, new FrameLayout.LayoutParams(size, size));
        if (on) {
            ImageView ck = Ui.icon(this, R.drawable.ic_check, 0xFFFFFFFF, 20);
            f.addView(ck, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        }
        f.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        f.setForeground(Ui.rippleRound(0x00000000, size / 2f / Math.max(0.01f, Ui.RAD)));
        f.setContentDescription((rgb < 0 ? "Theme default accent" : "Accent " + Palette.hex(rgb)) + (on ? ", selected" : ""));
        f.setOnClickListener(v -> { if (state.accentPick != rgb) { state.accentPick = rgb; persist(true); } });
        return f;
    }

    /** Swatch tiles, 3 per row (2 on very narrow screens). */
    View grid(List<Palette.Base> items, String cur) {
        LinearLayout col = Ui.column(this);
        int cols = getResources().getConfiguration().screenWidthDp >= 400 ? 3 : 2;
        if (getResources().getConfiguration().screenWidthDp >= 600) cols = 4;
        LinearLayout row = null;
        for (int k = 0; k < items.size(); k++) {
            if (k % cols == 0) {
                row = Ui.row(this);
                row.setGravity(Gravity.TOP);
                col.addView(row, Ui.lpMargins(this, MATCH, WRAP, 0, 0, 0, 10));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1);
            if (k % cols != cols - 1) lp.rightMargin = dp(10);
            row.addView(tile(items.get(k), items.get(k).id.equals(cur)), lp);
        }
        if (row != null) {
            int rest = (cols - items.size() % cols) % cols;
            for (int k = 0; k < rest; k++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1);
                if (k != rest - 1) lp.rightMargin = dp(10);
                row.addView(new View(this), lp);
            }
        }
        return col;
    }

    View tile(Palette.Base b, boolean sel) {
        LinearLayout t = Ui.column(this);
        int r = dp(14);
        t.setBackground(Ui.ripple(sel ? Ui.outlined(Ui.SELECTED, Ui.GREEN, dp(2), r) : Ui.outlined(Ui.CARD, Ui.LINE, Math.max(1, dp(1)), r), r));
        t.setPadding(dp(6), dp(6), dp(6), dp(8));
        FrameLayout prev = new FrameLayout(this);
        prev.setBackground(Ui.rounded(Ui.opaque(b.bg), dp(10)));
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.rounded(Ui.opaque(b.surface), dp(6)));
        card.setPadding(dp(6), dp(4), dp(6), dp(5));
        card.addView(Ui.text(this, "Aa", 16, Ui.opaque(b.text), Ui.MEDIUM));
        LinearLayout dots = Ui.row(this);
        View p1 = new View(this); p1.setBackground(Ui.rounded(Ui.opaque(b.primary), dp(3)));
        View p2 = new View(this); p2.setBackground(Ui.circle(Ui.opaque(b.accent)));
        dots.addView(p1, new LinearLayout.LayoutParams(dp(22), dp(8)));
        dots.addView(p2, Ui.lpMargins(this, dp(8), dp(8), 4, 0, 0, 0));
        ((LinearLayout.LayoutParams) p2.getLayoutParams()).width = dp(8);
        ((LinearLayout.LayoutParams) p2.getLayoutParams()).height = dp(8);
        card.addView(dots, Ui.lpMargins(this, WRAP, WRAP, 0, 3, 0, 0));
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.START | Gravity.CENTER_VERTICAL);
        cl.leftMargin = dp(8);
        prev.addView(card, cl);
        if (sel) {
            ImageView ck = Ui.icon(this, R.drawable.ic_check, Ui.ON_FILL, 16);
            ck.setBackground(Ui.circle(Ui.FILL));
            ck.setPadding(dp(2), dp(2), dp(2), dp(2));
            FrameLayout.LayoutParams kl = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.END | Gravity.TOP);
            kl.topMargin = dp(5); kl.rightMargin = dp(5);
            prev.addView(ck, kl);
        }
        t.addView(prev, new LinearLayout.LayoutParams(MATCH, dp(84)));
        TextView name = Ui.text(this, b.name, 13, Ui.TEXT, sel ? Ui.MEDIUM : Ui.REGULAR);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.addView(name, Ui.lpMargins(this, MATCH, WRAP, 2, 6, 2, 0));
        Palette.Colors dc = Palette.derive(b);
        String meta = (dc.dark ? "Dark" : "Light") + " \u00b7 " + Palette.ratio(Palette.contrast(dc.text, dc.surface)) + ":1";
        t.addView(Ui.text(this, meta, 12, Ui.TEXT2, Ui.REGULAR), Ui.lpMargins(this, MATCH, WRAP, 2, 0, 2, 0));
        t.setContentDescription(b.name + " theme, " + (dc.dark ? "dark" : "light") + ", text contrast " + Palette.ratio(Palette.contrast(dc.text, dc.surface)) + " to 1" + (sel ? ", in use" : ""));
        t.setOnClickListener(v -> { if (!state.active.equals(b.id)) use(b.id); });
        return t;
    }

    View swatches(Palette.Base b) {
        LinearLayout sw = Ui.row(this);
        int r = dp(12);
        GradientDrawable g = Ui.outlined(Ui.opaque(b.bg), Ui.LINE, Math.max(1, dp(1)), r);
        sw.setBackground(g);
        sw.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout card = Ui.row(this);
        card.setBackground(Ui.rounded(Ui.opaque(b.surface), dp(8)));
        card.setPadding(dp(8), dp(4), dp(8), dp(4));
        TextView aa = Ui.text(this, "Aa", 14, Ui.opaque(b.text), Ui.MEDIUM);
        card.addView(aa);
        card.addView(dot(b.primary), Ui.lpMargins(this, 14, 14, 6, 0, 0, 0));
        card.addView(dot(b.accent), Ui.lpMargins(this, 14, 14, 4, 0, 0, 0));
        fixDots(card);
        sw.addView(card);
        return sw;
    }

    View dot(int rgb) {
        View v = new View(this);
        v.setBackground(Ui.rounded(Ui.opaque(rgb), dp(7)));
        return v;
    }

    void fixDots(LinearLayout card) {
        for (int k = 1; k < card.getChildCount(); k++) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getChildAt(k).getLayoutParams();
            lp.width = dp(14); lp.height = dp(14);
        }
    }

    View swatchRow(Palette.Base b, boolean custom) {
        LinearLayout row = Ui.row(this);
        boolean sel = state.active.equals(b.id);
        int r = dp(16);
        row.setBackground(Ui.ripple(sel ? Ui.outlined(Ui.SELECTED, Ui.GREEN, dp(1.5f), r) : Ui.rounded(Ui.CARD, r), r));
        row.setPadding(dp(12), dp(10), dp(8), dp(10));
        row.addView(swatches(b));
        LinearLayout col = Ui.column(this);
        TextView name = Ui.text(this, b.name, 16, Ui.TEXT, Ui.MEDIUM);
        Ui.ellipsize(name, 1);
        col.addView(name);
        Palette.Colors d = Palette.derive(b);
        double min = 99;
        for (Object[] pr : Palette.textPairs(d)) min = Math.min(min, Palette.contrast((Integer) pr[1], (Integer) pr[2]));
        col.addView(Ui.caption(this, (d.dark ? "Dark" : "Light") + " \u00b7 text " + Palette.ratio(Palette.contrast(d.text, d.surface)) + ":1"
                + (sel ? " \u00b7 in use" : "")));
        row.addView(col, Ui.lpMargins(this, 0, WRAP, 12, 0, 0, 0));
        ((LinearLayout.LayoutParams) col.getLayoutParams()).weight = 1;
        if (custom) {
            TextView ed = Ui.text(this, "Edit", 14, Ui.GREEN_DARK, Ui.MEDIUM);
            ed.setPadding(dp(10), dp(10), dp(10), dp(10));
            ed.setBackground(Ui.rippleRound(0x00000000, dp(18)));
            ed.setOnClickListener(v -> {
                edit = new int[]{b.primary, b.accent, b.bg, b.surface, b.text};
                editId = b.id; role = 0;
                showEditor(b.name);
            });
            row.addView(ed);
            View del = Ui.iconButton(this, R.drawable.ic_delete, Ui.TEXT2, "Delete " + b.name);
            del.setOnClickListener(v -> new Ui.Dialog(this).setTitle("Delete \u201c" + b.name + "\u201d?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dd, w) -> {
                        boolean wasActive = b.id.equals(state.active);
                        state.remove(b.id);
                        persist(wasActive);
                        if (!wasActive) relist();
                    }).show());
            row.addView(del);
        }
        row.setOnClickListener(v -> use(b.id));
        return row;
    }

    // ---------------- editor ----------------
    Palette.Base editBase(String name) {
        return new Palette.Base(editId == null ? "preview" : editId, name, edit[2], edit[3], edit[0], edit[1], edit[4]);
    }

    void showEditor(String name) {
        if (!editing && scroll != null) listY = scroll.getScrollY();
        editing = true;
        roleChips.clear();
        LinearLayout c = frame(editId == null ? "New theme" : "Edit theme");

        c.addView(Ui.sectionLabel(this, "Preview"), Ui.lpMargins(this, MATCH, WRAP, 4, 8, 0, 8));
        preview = Ui.column(this);
        c.addView(preview, Ui.lp(MATCH, WRAP));
        report = Ui.caption(this, "");
        c.addView(report, Ui.lpMargins(this, MATCH, WRAP, 4, 8, 4, 0));

        LinearLayout ed = Ui.card(this);
        c.addView(ed, Ui.lpMargins(this, MATCH, WRAP, 0, 16, 0, 0));
        ed.addView(Ui.sectionLabel(this, "Colour to change"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = Ui.row(this);
        for (int k = 0; k < ROLES.length; k++) {
            final int kk = k;
            TextView ch = Ui.chip(this, ROLES[k], k == role);
            ch.setOnClickListener(v -> { role = kk; syncControls(); });
            roleChips.add(ch);
            chips.addView(ch, Ui.lpMargins(this, WRAP, WRAP, 0, 0, 8, 0));
        }
        hs.addView(chips);
        ed.addView(hs, Ui.lpMargins(this, MATCH, WRAP, 0, 8, 0, 8));

        LinearLayout hexRow = Ui.row(this);
        hexRow.addView(Ui.text(this, "Hex", 14, Ui.TEXT2, Ui.MEDIUM));
        hexIn = new EditText(this);
        hexIn.setSingleLine(true);
        hexIn.setTextColor(Ui.TEXT);
        hexIn.setHintTextColor(Ui.TEXT3);
        hexIn.setHint("#RRGGBB");
        hexIn.setTypeface(Typeface.MONOSPACE);
        hexIn.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hexIn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        hexIn.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            public void onTextChanged(CharSequence s, int a, int b, int d) { }
            public void afterTextChanged(Editable s) {
                if (syncing) return;
                String t = s.toString().trim();
                if (t.length() < 6 && !(t.startsWith("#") && t.length() == 4)) return;
                int v = Palette.parseHex(t);
                if (v < 0) { hexIn.setError("Use #RRGGBB"); return; }
                edit[role] = v;
                syncControls(false);
            }
        });
        hexRow.addView(hexIn, Ui.lpMargins(this, dp(140), WRAP, 12, 0, 0, 0));
        ed.addView(hexRow);

        String[] labels = {"Hue", "Saturation", "Brightness", "Red", "Green", "Blue"};
        int[] max = {360, 100, 100, 255, 255, 255};
        for (int k = 0; k < 6; k++) {
            if (k == 0) ed.addView(Ui.sectionLabel(this, "HSV"), Ui.lpMargins(this, MATCH, WRAP, 0, 12, 0, 0));
            if (k == 3) ed.addView(Ui.sectionLabel(this, "RGB"), Ui.lpMargins(this, MATCH, WRAP, 0, 12, 0, 0));
            LinearLayout r = Ui.row(this);
            TextView l = Ui.text(this, labels[k], 14, Ui.TEXT, Ui.REGULAR);
            r.addView(l, Ui.lp(dp(92), WRAP));
            SeekBar sb = new SeekBar(this);
            sb.setMax(max[k]);
            sb.setContentDescription(labels[k]);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                sb.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
                sb.setThumbTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
            }
            final int kk = k;
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean user) {
                    if (!user || syncing) return;
                    if (kk < 3) edit[role] = Palette.fromHsv(hsv[0].getProgress(), hsv[1].getProgress() / 100.0, hsv[2].getProgress() / 100.0);
                    else edit[role] = (rgb[0].getProgress() << 16) | (rgb[1].getProgress() << 8) | rgb[2].getProgress();
                    syncControls(true, kk < 3);
                }
                public void onStartTrackingTouch(SeekBar s) { }
                public void onStopTrackingTouch(SeekBar s) { }
            });
            if (k < 3) hsv[k] = sb; else rgb[k - 3] = sb;
            r.addView(sb, new LinearLayout.LayoutParams(0, dp(48), 1));
            TextView val = Ui.text(this, "", 13, Ui.TEXT2, Ui.REGULAR);
            val.setGravity(Gravity.END);
            sliderVals[k] = val;
            r.addView(val, Ui.lp(dp(40), WRAP));
            ed.addView(r, Ui.lp(MATCH, WRAP));
        }

        LinearLayout nm = Ui.card(this);
        c.addView(nm, Ui.lpMargins(this, MATCH, WRAP, 0, 16, 0, 0));
        nm.addView(Ui.sectionLabel(this, "Name"));
        nameIn = new EditText(this);
        nameIn.setSingleLine(true);
        nameIn.setText(name);
        nameIn.setTextColor(Ui.TEXT);
        nameIn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        nm.addView(nameIn, Ui.lp(MATCH, WRAP));

        LinearLayout btns = Ui.row(this);
        TextView save = Ui.button(this, editId == null ? "Save & use" : "Save & use", R.drawable.ic_check, true);
        save.setOnClickListener(v -> save(false));
        btns.addView(save);
        if (editId != null) {
            TextView copy = Ui.button(this, "Save as new", 0, false);
            copy.setOnClickListener(v -> save(true));
            btns.addView(copy, Ui.lpMargins(this, WRAP, WRAP, 8, 0, 0, 0));
        }
        c.addView(btns, Ui.lpMargins(this, MATCH, WRAP, 0, 16, 0, 0));
        LinearLayout btns2 = Ui.row(this);
        TextView def = Ui.button(this, "Start from Light", 0, false);
        def.setOnClickListener(v -> { Palette.Base l = Palette.preset(Palette.LIGHT); edit = new int[]{l.primary, l.accent, l.bg, l.surface, l.text}; syncControls(); });
        btns2.addView(def);
        TextView cancel = Ui.button(this, "Cancel", 0, false);
        cancel.setOnClickListener(v -> relist());
        btns2.addView(cancel, Ui.lpMargins(this, WRAP, WRAP, 8, 0, 0, 0));
        c.addView(btns2, Ui.lpMargins(this, MATCH, WRAP, 0, 8, 0, 0));
        syncControls();
    }

    void save(boolean asNew) {
        String name = nameIn.getText().toString().trim();
        if (name.isEmpty()) name = "My theme";
        String id = (editId == null || asNew) ? state.nextCustomId() : editId;
        if ((editId == null || asNew) && state.custom.size() >= Palette.MAX_CUSTOM) {
            Ui.toastShort(this, "You can keep up to " + Palette.MAX_CUSTOM + " custom themes.");
            return;
        }
        Palette.Base b = new Palette.Base(id, name, edit[2], edit[3], edit[0], edit[1], edit[4]);
        state.put(b);
        state.active = id;
        editing = false;
        persist(true);
    }

    void syncControls() { syncControls(false, false); }
    void syncControls(boolean fromSlider) { syncControls(fromSlider, false); }

    /** Pushes the edited colour into the controls (skipping the slider group the user is dragging) and redraws the preview. */
    void syncControls(boolean fromSlider, boolean fromHsv) {
        syncing = true;
        int v = edit[role];
        for (int k = 0; k < roleChips.size(); k++) Ui.styleChip(this, roleChips.get(k), k == role);
        if (!(fromSlider && fromHsv)) {
            double[] h = Palette.toHsv(v);
            hsv[0].setProgress((int) Math.round(h[0]) % 360);
            hsv[1].setProgress((int) Math.round(h[1] * 100));
            hsv[2].setProgress((int) Math.round(h[2] * 100));
        }
        if (!(fromSlider && !fromHsv)) {
            rgb[0].setProgress((v >> 16) & 0xFF);
            rgb[1].setProgress((v >> 8) & 0xFF);
            rgb[2].setProgress(v & 0xFF);
        }
        sliderVals[0].setText(hsv[0].getProgress() + "\u00b0");
        sliderVals[1].setText(hsv[1].getProgress() + "%");
        sliderVals[2].setText(hsv[2].getProgress() + "%");
        for (int k = 0; k < 3; k++) sliderVals[3 + k].setText(String.valueOf(rgb[k].getProgress()));
        String hx = Palette.hex(v);
        if (!hx.equalsIgnoreCase(hexIn.getText().toString().trim()) && (fromSlider || !hexIn.hasFocus() || Palette.parseHex(hexIn.getText().toString()) != v)) {
            hexIn.setText(hx);
            hexIn.setSelection(hexIn.getText().length());
        }
        hexIn.setError(null);
        syncing = false;
        drawPreview();
    }

    void drawPreview() {
        renderPreview(preview, editBase(nameIn == null ? "" : nameIn.getText().toString()), report);
    }

    /** v2.2: one preview renderer for the editor and the live preview at the top of the theme list. */
    void renderPreview(LinearLayout preview, Palette.Base b, TextView report) {
        Palette.Colors d = Palette.derive(b);
        preview.removeAllViews();
        int bg = Ui.opaque(d.bg), sf = Ui.opaque(d.surface);
        preview.setBackground(Ui.outlined(bg, Ui.LINE, Math.max(1, dp(1)), dp(16)));
        preview.setPadding(dp(12), dp(12), dp(12), dp(12));
        // banner
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Ui.opaque(d.fill), Ui.opaque(d.heroEnd)});
        g.setCornerRadius(dp(14));
        LinearLayout hero = Ui.column(this);
        hero.setBackground(g);
        hero.setPadding(dp(14), dp(12), dp(14), dp(12));
        hero.addView(Ui.text(this, "TONIGHT'S PICK", 11, Ui.opaque(d.onFill2), Ui.MEDIUM));
        hero.addView(Ui.text(this, "Garlic butter pasta", 18, Ui.opaque(d.onFill), Ui.MEDIUM));
        preview.addView(hero, Ui.lp(MATCH, WRAP));
        // card
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.rounded(sf, dp(14)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(Ui.text(this, "Tomato soup", 16, Ui.opaque(d.text), Ui.MEDIUM));
        card.addView(Ui.text(this, "Soup \u00b7 25 min \u00b7 you have 6 of 7", 13, Ui.opaque(d.text2), Ui.REGULAR));
        LinearLayout chips = Ui.row(this);
        TextView c1 = Ui.text(this, "Vegetarian", 13, Ui.opaque(d.primaryText), Ui.MEDIUM);
        c1.setBackground(Ui.rounded(Ui.opaque(d.tint), dp(12)));
        c1.setPadding(dp(10), dp(4), dp(10), dp(4));
        chips.addView(c1);
        TextView c2 = Ui.text(this, "Missing: basil", 13, Ui.opaque(d.accentText), Ui.MEDIUM);
        c2.setBackground(Ui.rounded(Ui.opaque(d.accentTint), dp(12)));
        c2.setPadding(dp(10), dp(4), dp(10), dp(4));
        chips.addView(c2, Ui.lpMargins(this, WRAP, WRAP, 6, 0, 0, 0));
        card.addView(chips, Ui.lpMargins(this, MATCH, WRAP, 0, 8, 0, 0));
        LinearLayout brow = Ui.row(this);
        TextView btn = Ui.text(this, "Cook", 14, Ui.opaque(d.onFill), Ui.MEDIUM);
        btn.setBackground(Ui.rounded(Ui.opaque(d.fill), dp(18)));
        btn.setPadding(dp(16), dp(8), dp(16), dp(8));
        brow.addView(btn);
        TextView link = Ui.text(this, "View recipe", 14, Ui.opaque(d.primaryText), Ui.MEDIUM);
        brow.addView(link, Ui.lpMargins(this, WRAP, WRAP, 14, 0, 0, 0));
        card.addView(brow, Ui.lpMargins(this, MATCH, WRAP, 0, 10, 0, 0));
        preview.addView(card, Ui.lpMargins(this, MATCH, WRAP, 0, 10, 0, 0));
        // nav strip
        LinearLayout nav = Ui.row(this);
        nav.setBackgroundColor(sf);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        String[] tabs = {"Pantry", "Recipes", "Saved"};
        for (int k = 0; k < 3; k++) {
            TextView t = Ui.text(this, tabs[k], 13, Ui.opaque(k == 0 ? d.primaryText : d.text2), k == 0 ? Ui.MEDIUM : Ui.REGULAR);
            t.setGravity(Gravity.CENTER);
            nav.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1));
        }
        preview.addView(nav, Ui.lpMargins(this, MATCH, WRAP, 0, 10, 0, 0));

        StringBuilder sb = new StringBuilder();
        sb.append("Contrast: text ").append(Palette.ratio(Math.min(Palette.contrast(d.text, d.bg), Palette.contrast(d.text, d.surface))))
          .append(":1 \u00b7 buttons ").append(Palette.ratio(Palette.contrast(d.onFill, d.fill)))
          .append(":1 \u00b7 links ").append(Palette.ratio(Palette.contrast(d.primaryText, d.surface))).append(":1 (AA needs 4.5)");
        for (String w : Palette.warnings(b)) sb.append("\n\u26a0 ").append(w);
        if (report != null) report.setText(sb.toString());
    }
}
