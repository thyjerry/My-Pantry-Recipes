package org.pantryrecipes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.pantryrecipes.app.core.Allergens;
import org.pantryrecipes.app.core.Amounts;
import org.pantryrecipes.app.core.Catalog;
import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.UseBy;
import org.pantryrecipes.app.core.Discover;
import org.pantryrecipes.app.core.Variety;
import org.pantryrecipes.app.core.Substitutions;
import org.pantryrecipes.app.core.Ingredient;
import org.pantryrecipes.app.core.Matcher;
import org.pantryrecipes.app.core.MatchCache;
import org.pantryrecipes.app.core.PantryIndex;
import org.pantryrecipes.app.core.MealType;
import org.pantryrecipes.app.core.Normalizer;
import org.pantryrecipes.app.core.Quantity;
import org.pantryrecipes.app.core.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity implements Repo.Listener {
    @Override
    protected void attachBaseContext(android.content.Context base) { super.attachBaseContext(Ui.fontContext(base)); }

    static final int TAB_PANTRY = 0, TAB_DISCOVER = 1, TAB_RECIPES = 2, TAB_FAVORITES = 3, TAB_SHOPPING = 4, TAB_PLAN = 5;
    /** v1.8: the segmented tabs, in display order. Favorites lives under Recipes (a chip); Plan has its own tab. */
    static final int[] SEG_TABS = {TAB_PANTRY, TAB_RECIPES, TAB_DISCOVER, TAB_PLAN, TAB_SHOPPING};
    static final String[] SEG_NAMES = {"Pantry", "Recipes", "Discover", "Plan", "Shopping"};
    static final String[] TAB_NAMES = {"Pantry", "Discover", "Recipes", "Favorites", "Shopping"};
    static final int[] TAB_ICONS = {R.drawable.ic_kitchen, R.drawable.ic_explore, R.drawable.ic_restaurant, R.drawable.ic_heart, R.drawable.ic_cart};
    static final String[] DIETS = {Recipe.VEGETARIAN, Recipe.VEGAN, Recipe.GLUTEN_FREE, Recipe.DAIRY_FREE};

    Store store;
    Repo repo;
    boolean decoKey;
    boolean wide;
    int tab;
    String selectedId;
    boolean detailOpen;
    String query = "";
    int mode;
    int maxTime;
    final Set<String> diets = new HashSet<String>();
    /** Meal-type chips (shared by Recipes and Discover) and the Include desserts / Include sides switches. */
    final Set<String> mealSel = new LinkedHashSet<String>();
    boolean incDesserts, incSides;
    /** v2.0.1: Discover keeps its own meal-type filter; it never reads the Recipes tab filters (meal types, diets,
     *  time, methods, budget, mode, search). */
    final Set<String> dMealSel = new LinkedHashSet<String>();
    boolean dIncDesserts, dIncSides;
    Matcher matcher;
    int detailServings;

    FrameLayout content;
    LinearLayout nav;
    TextView subtitle;
    FlowLayout pantryFlow, stapleFlow;
    TextView pantryCount;
    final List<TextView> quickChipViews = new ArrayList<TextView>();
    final List<String> quickChipKeys = new ArrayList<String>();
    final List<TextView> filterChipViews = new ArrayList<TextView>();
    final List<String> filterChipKeys = new ArrayList<String>();
    final List<TextView> sectionCounts = new ArrayList<TextView>();
    FlowLayout recentFlow, filterFlow;
    LinearLayout recentBox, sectionsBox;
    String pantryFilter = "";
    // discover
    ScrollView discoverScroll;
    LinearLayout discoverBody;
    FrameLayout discoverSub;
    ResultAdapter subAdapter;
    Runnable subRefresh;
    ScrollView detailScroll;
    ListView list;
    ResultAdapter adapter;
    FrameLayout detailPane;
    FrameLayout browseRoot;
    LinearLayout heroBox;
    LinearLayout statusBox;
    View detailOverlay;
    LinearLayout shopList;
    LinearLayout expiringHolder;
    LinearLayout holidayHolder;
    int planWeek = 0;

    @Override
    protected void onCreate(Bundle saved) {
        Ui.themeActivity(this);
        themeKey = Ui.THEME_ID;
        decoKey = Deco.on(this);
        super.onCreate(saved);
        Ui.styleWindow(this);
        store = Store.get(this);
        repo = Repo.get(this);
        tab = Math.min(TAB_PLAN, Math.max(0, store.prefs.getInt("tab2", TAB_PANTRY)));
        mode = store.prefs.getInt("mode", 0);
        maxTime = store.prefs.getInt("maxTime", 0);
        diets.addAll(store.prefs.getStringSet("diets", new HashSet<String>()));
        V30.dropStaleFastDiets(this, diets);
        for (String m : store.prefs.getStringSet("meals", new HashSet<String>())) if (MealType.isKnown(m)) mealSel.add(m);
        incDesserts = store.prefs.getBoolean("incDesserts", false);
        incSides = store.prefs.getBoolean("incSides", false);
        for (String m : store.prefs.getStringSet("dMeals", new HashSet<String>())) if (MealType.isKnown(m)) dMealSel.add(m);
        dIncDesserts = store.prefs.getBoolean("dIncDesserts", false);
        dIncSides = store.prefs.getBoolean("dIncSides", false);
        if (saved != null) {
            selectedId = saved.getString("sel");
            detailOpen = saved.getBoolean("open");
            query = saved.getString("q", "");
        }
        repo.addListener(this);
        buildUi();
        // v2.0: first-run Region question, daily use-by reminder, "cook this tonight" from the notification
        tabsHost().post(() -> { V20.maybeWelcome(this); handleV20Intent(getIntent()); });
        V20Alarm.schedule(this);
    }

    View tabsHost() { return getWindow().getDecorView(); }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleV20Intent(i);
    }

    void handleV20Intent(Intent i) {
        if (i == null) return;
        if (i.getBooleanExtra("cookTonight", false)) { i.removeExtra("cookTonight"); V20.cookTonight(this); }
        String hol = i.getStringExtra("holiday");
        if (hol != null) { i.removeExtra("holiday"); V30.section(this, hol); }
        String sc = i.getStringExtra("shortcut");
        if (sc != null) {
            i.removeExtra("shortcut");
            if ("pantry".equals(sc)) showTab(TAB_PANTRY, false);
            else if ("shopping".equals(sc)) showTab(TAB_SHOPPING, false);
            else if ("plan".equals(sc)) showTab(TAB_PLAN, false);
            else if ("inventory".equals(sc)) V20.inventoryDialog(this);
        }
    }

    /** v2.0 settings changed (region, household, prices, diets): rebuild what depends on them. */
    void refreshAfterV20() {
        if (detailOpen && selectedId != null) reopenDetailKeepScroll(selectedId);
        refreshResults();
        refreshDiscover();
        if (tab == TAB_SHOPPING) refreshShopping();
    }

    String pendingOpen;

    void openAfterReload(String id) { pendingOpen = id; }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("sel", selectedId);
        out.putBoolean("open", detailOpen);
        out.putString("q", query);
    }

    @Override
    protected void onPause() {
        super.onPause();
        store.flushIo(); // v1.8: make sure queued pantry writes reach the database
        V20Sync.pause(this);
    }

    @Override
    protected void onDestroy() {
        repo.removeListener(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // theme changed in Appearance (or the system dark setting changed while "follow system" is on)
        Ui.load(this);
        if (!Ui.THEME_ID.equals(themeKey) || decoKey != Deco.on(this)) { recreate(); return; }
        if (tab == TAB_SHOPPING && shopList != null) refreshShopping();
        V20Sync.resume(this);
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        buildUi();
    }

    @Override
    public void onRepoChanged() {
        V20.repoChanged();
        if (pendingOpen != null && repo.byId(pendingOpen) != null) { String id = pendingOpen; pendingOpen = null; showTab(TAB_RECIPES, false); openDetail(id, true); }
        if (tab == TAB_DISCOVER) {
            if (discoverSub != null && subRefresh != null) subRefresh.run();
            else if (!detailOpen) refreshDiscover();
            return;
        }
        if (tab == TAB_RECIPES || tab == TAB_FAVORITES) {
            refreshResults();
            if (wide && selectedId == null) showDetailPlaceholderOrFirst();
        }
    }

    @Override
    public void onBackPressed() {
        if (detailOpen && ((!wide && (tab == TAB_RECIPES || tab == TAB_FAVORITES)) || tab == TAB_DISCOVER)) {
            closeDetail();
            return;
        }
        if (tab == TAB_DISCOVER && discoverSub != null) {
            closeDiscoverSub();
            return;
        }
        super.onBackPressed();
    }

    int dp(float v) { return Ui.dp(this, v); }

    // ===================== shell =====================
    void buildUi() {
        Configuration cfg = getResources().getConfiguration();
        wide = cfg.screenWidthDp >= 720;
        LinearLayout root = Ui.column(this);
        // v1.8: the window background is the only full-screen fill (no second opaque layer under the content)
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.BG));

        // header band: logo mark + live app name, centred; flat colour, no bitmap (the mark is a vector)
        LinearLayout bar = Ui.row(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(4), dp(4), dp(4));
        android.graphics.drawable.Drawable band = Deco.headerBand(this);
        if (band != null) bar.setBackground(band);
        boolean roomy = cfg.screenWidthDp >= 420;
        if (roomy) bar.addView(Ui.space(this, 88, 1)); // balances the two buttons on the right so the title sits centred
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_logo);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        bar.addView(logo, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView name = Ui.title(this, "My Pantry Recipes", roomy ? 20 : 18);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(dp(10), 0, 0, 0);
        bar.addView(name, new LinearLayout.LayoutParams(-2, -2));
        TextView ver = Ui.text(this, "v" + Extras.version(this), 11, Ui.GREEN_DARK, Ui.MEDIUM);
        ver.setBackground(Ui.rounded(Ui.CARD, dp(8)));
        ver.setPadding(dp(6), dp(1), dp(6), dp(1));
        ver.setContentDescription("Version " + Extras.version(this));
        bar.addView(ver, Ui.lpMargins(this, -2, -2, 8, 0, 0, 0));
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        subtitle = Ui.caption(this, ""); // v1.8: kept for screen readers only (not shown; one view less in the header)
        TextView settings = Ui.text(this, "Settings", 14, Ui.GREEN_DARK, Ui.MEDIUM);
        settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(10), 0, dp(10), 0);
        settings.setMinHeight(dp(48)); settings.setMinWidth(dp(48));
        settings.setContentDescription("Settings");
        settings.setBackground(Ui.rippleRound(Ui.CARD, dp(12)));
        settings.setOnClickListener(v -> showAbout());
        bar.addView(settings);
        root.addView(bar);

        // pill-shaped segmented tabs: one TextView per tab on a single rounded track; the one "selected"
        // pill drawable is moved between tabs (no icons, no per-tab containers)
        nav = Ui.row(this);
        nav.setBackground(Ui.rounded(Ui.NEUTRAL, dp(22)));
        nav.setPadding(dp(3), dp(3), dp(3), dp(3));
        float navSp = cfg.screenWidthDp >= 400 ? 14 : 12.5f;
        for (int i = 0; i < SEG_TABS.length; i++) {
            final int t = SEG_TABS[i];
            TextView item = Ui.text(this, SEG_NAMES[i], navSp, Ui.TEXT, Ui.MEDIUM);
            item.setGravity(Gravity.CENTER);
            item.setSingleLine(true);
            item.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.setMinHeight(dp(48));
            item.setPadding(dp(4), 0, dp(4), 0);
            item.setOnClickListener(v -> showTab(t, true));
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(wide ? dp(640) : -1, -2);
        nlp.gravity = Gravity.CENTER_HORIZONTAL;
        nlp.setMargins(dp(12), dp(2), dp(12), dp(8));
        root.addView(nav, nlp);
        segSel = Ui.rounded(Ui.FILL, dp(20));

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        showTab(tab, false);
    }

    android.graphics.drawable.Drawable segSel;

    void styleNav() {
        int shown = tab == TAB_FAVORITES ? TAB_RECIPES : tab;
        for (int i = 0; i < nav.getChildCount(); i++) {
            TextView item = (TextView) nav.getChildAt(i);
            boolean sel = SEG_TABS[i] == shown;
            if (!sel && item.getBackground() != null) item.setBackground(null);
            item.setTextColor(sel ? Ui.ON_FILL : Ui.TEXT);
            item.setSelected(sel);
            String label = SEG_NAMES[i];
            if (SEG_TABS[i] == TAB_SHOPPING) {
                int n = store.shoppingOpenCount();
                if (n > 0) label = "Shopping " + n;
                item.setContentDescription("Shopping, " + n + " to buy");
            }
            if (!label.contentEquals(item.getText())) item.setText(label);
        }
        for (int i = 0; i < nav.getChildCount(); i++) if (SEG_TABS[i] == shown) nav.getChildAt(i).setBackground(segSel);
    }

    void showTab(int t, boolean animate) {
        if (t != tab) detailOpen = false;
        tab = t;
        store.prefs.edit().putInt("tab2", t).apply();
        hideKeyboard();
        styleNav();
        content.removeAllViews();
        quickChipViews.clear();
        quickChipKeys.clear();
        filterChipViews.clear();
        filterChipKeys.clear();
        sectionCounts.clear();
        pendingSections.clear();
        recentChipViews.clear();
        recentChipKeys.clear();
        pantryChipCache.clear();
        pantryInput = null;
        ui.removeCallbacks(filterRun);
        recentFlow = null;
        filterFlow = null;
        discoverScroll = null;
        discoverBody = null;
        discoverSub = null;
        subAdapter = null;
        subRefresh = null;
        detailPane = null;
        browseRoot = null;
        list = null;
        adapter = null;
        shopList = null;
        pantryFlow = null;
        detailOverlay = null;
        View v;
        switch (t) {
            case TAB_PANTRY: v = buildPantry(); subtitle.setText("What's in your kitchen?"); break;
            case TAB_DISCOVER: v = buildDiscover(); subtitle.setText("Find something new to cook"); break;
            case TAB_RECIPES: v = buildBrowse(false); subtitle.setText("Matched to your pantry"); break;
            case TAB_FAVORITES: v = buildBrowse(true); subtitle.setText("Recipes you love"); break;
            case TAB_PLAN: v = buildPlan(); subtitle.setText("Your meal plan"); break;
            default: v = buildShopping(); subtitle.setText("Missing ingredients to buy"); break;
        }
        content.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (animate) {
            v.setAlpha(0f);
            v.setTranslationY(dp(10));
            v.animate().alpha(1f).translationY(0).setDuration(200).start();
        }
        if ((t == TAB_RECIPES || t == TAB_DISCOVER) && repo.needsFetch() && repo.isOnline()) repo.fetchForPantry();
    }

    void hideKeyboard() {
        View f = getCurrentFocus();
        if (f != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
            f.clearFocus();
        }
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    AlertDialog.Builder dialog() { return new Ui.Dialog(this); }

    String themeKey = "";

    // ===================== pantry =====================
    View buildPantry() {
        LinearLayout left = Ui.column(this);
        LinearLayout right = Ui.column(this);
        int pad = dp(16);

        holidayHolder = Ui.column(this);
        left.addView(holidayHolder);
        // v2.0 Stage 3: holiday banner computed after the first frame (stage3.txt parse stays off the startup path)
        final LinearLayout hh = holidayHolder;
        hh.postDelayed(() -> { if (hh == holidayHolder) { try { V30.banner(this, hh); } catch (RuntimeException e) { hh.removeAllViews(); } } }, 400);
        expiringHolder = Ui.column(this);
        left.addView(expiringHolder);
        leftoversHolder = Ui.column(this);
        left.addView(leftoversHolder);

        LinearLayout addCard = Ui.card(this);
        addCard.addView(Ui.title(this, "Add what you have", 17));
        TextView hint = Ui.caption(this, "Type an ingredient, or tap the common ones below.");
        hint.setPadding(0, dp(2), 0, dp(10));
        addCard.addView(hint);
        LinearLayout addRow = Ui.row(this);
        final AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setHint("e.g. eggs, spinach, rice");
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.TEXT3);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setThreshold(1);
        input.setBackground(Ui.outlined(Ui.BG, Ui.LINE, dp(1), dp(22)));
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setDropDownBackgroundDrawable(Ui.rounded(Ui.CARD, dp(12)));
        // v1.8: the autocomplete list (~3,000 names) is attached on first focus, not while the Pantry opens
        pantryInput = input;
        input.setOnFocusChangeListener((v, has) -> { if (has && input.getAdapter() == null) input.setAdapter(new SuggestAdapter(this, repo.ingredientNames())); });
        Runnable doAdd = () -> {
            String s = input.getText().toString().trim();
            if (s.isEmpty()) return;
            for (String part : s.split(",")) {
                if (part.trim().isEmpty()) continue;
                if (!store.addToPantry(part.trim(), false)) toast(Ui.cap(part.trim()) + " is already in your pantry");
            }
            input.setText("");
            refreshPantryViews();
        };
        input.setOnItemClickListener((p, v, pos, id) -> doAdd.run());
        input.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                doAdd.run();
                return true;
            }
            return false;
        });
        addRow.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView addBtn = Ui.button(this, "Add", R.drawable.ic_add, true);
        addBtn.setOnClickListener(v -> doAdd.run());
        LinearLayout.LayoutParams abp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        abp.leftMargin = dp(10);
        addRow.addView(addBtn, abp);
        addCard.addView(addRow);
        TextView bc = smallAction("Add by barcode", R.drawable.ic_barcode);
        bc.setContentDescription("Add by barcode: scan with a barcode app or type the number");
        bc.setOnClickListener(v -> Scan.open(this));
        addCard.addView(bc, Ui.lpMargins(this, -2, dp(48), 0, 10, 0, 0));
        left.addView(addCard, Ui.lpMargins(this, -1, -2, 0, 0, 0, 12));

        LinearLayout pc = Ui.card(this);
        LinearLayout head = Ui.row(this);
        LinearLayout ht = Ui.column(this);
        ht.addView(Ui.title(this, "In your pantry", 17));
        pantryCount = Ui.caption(this, "");
        ht.addView(pantryCount);
        head.addView(ht, new LinearLayout.LayoutParams(0, -2, 1));
        TextView find = Ui.button(this, "Find recipes", R.drawable.ic_restaurant, true);
        find.setOnClickListener(v -> showTab(TAB_RECIPES, true));
        head.addView(find, new LinearLayout.LayoutParams(-2, dp(48)));
        pc.addView(head);
        pantryFlow = new FlowLayout(this, dp(8), dp(0));
        pantryFlow.setPadding(0, dp(12), 0, dp(4));
        pc.addView(pantryFlow);
        TextView clear = Ui.text(this, "Clear pantry", 14, Ui.TEXT2, Ui.MEDIUM);
        clear.setPadding(dp(4), dp(10), dp(4), dp(2));
        clear.setOnClickListener(v -> dialog().setTitle("Clear pantry?")
                .setMessage("Removes everything except your staples.")
                .setPositiveButton("Clear", (d, w) -> { store.clearPantry(true); refreshPantryViews(); })
                .setNegativeButton("Cancel", null).show());
        pc.addView(clear);
        left.addView(pc, Ui.lpMargins(this, -1, -2, 0, 0, 0, 12));

        LinearLayout sc = Ui.card(this);
        LinearLayout sh = Ui.row(this);
        sh.addView(Ui.icon(this, R.drawable.ic_star, Ui.STAR, 20));
        TextView st = Ui.title(this, "Always on hand", 17);
        st.setPadding(dp(8), 0, 0, 0);
        sh.addView(st);
        sc.addView(sh);
        TextView sn = Ui.caption(this, "Staples like salt, pepper and oil are never counted as missing. Tap any pantry item to make it a staple.");
        sn.setPadding(0, dp(4), 0, 0);
        sc.addView(sn);
        stapleFlow = new FlowLayout(this, dp(8), dp(0));
        stapleFlow.setPadding(0, dp(12), 0, 0);
        sc.addView(stapleFlow);
        left.addView(sc, Ui.lpMargins(this, -1, -2, 0, 0, 0, 12));

        LinearLayout qc = Ui.card(this);
        qc.addView(Ui.title(this, "Quick add", 17));
        TextView qn = Ui.caption(this, PantryIndex.get().items.size() + " ingredients in " + Catalog.CATEGORIES.length
                + " groups. Tap to add or remove; green means you have it.");
        qn.setPadding(0, dp(2), 0, dp(10));
        qc.addView(qn);
        LinearLayout fr = Ui.row(this);
        fr.setPadding(dp(14), 0, dp(2), 0);
        fr.setBackground(Ui.outlined(Ui.BG, Ui.LINE, dp(1), dp(22)));
        fr.addView(Ui.icon(this, R.drawable.ic_search, Ui.TEXT3, 20));
        final EditText filter = new EditText(this);
        filter.setHint("Filter ingredients, e.g. cheese");
        filter.setText(pantryFilter);
        pantryFilterEdit = filter;
        filter.setSingleLine(true);
        filter.setTextSize(15);
        filter.setTextColor(Ui.TEXT);
        filter.setHintTextColor(Ui.TEXT3);
        filter.setBackground(null);
        filter.setImeOptions(EditorInfo.IME_ACTION_DONE);
        filter.setPadding(dp(10), 0, dp(4), 0);
        fr.addView(filter, new LinearLayout.LayoutParams(0, dp(48), 1));
        final ImageView fclr = Ui.iconButton(this, R.drawable.ic_close, Ui.TEXT2, "Clear filter");
        fclr.setVisibility(pantryFilter.isEmpty() ? View.GONE : View.VISIBLE);
        fclr.setOnClickListener(v -> { filter.setText(""); hideKeyboard(); });
        fr.addView(fclr);
        qc.addView(fr);
        filterFlow = new FlowLayout(this, dp(8), dp(0));
        filterFlow.setPadding(0, dp(12), 0, dp(4));
        qc.addView(filterFlow);
        recentBox = Ui.column(this);
        TextView rl = Ui.sectionLabel(this, "Recently used");
        rl.setPadding(0, dp(14), 0, dp(8));
        recentBox.addView(rl);
        recentFlow = new FlowLayout(this, dp(8), dp(0));
        recentBox.addView(recentFlow);
        qc.addView(recentBox);
        sectionsBox = Ui.column(this);
        qc.addView(sectionsBox);
        final Set<String> open = store.openSections();
        for (int c = 0; c < Catalog.CATEGORIES.length; c++) {
            final int ci = c;
            LinearLayout lr = Ui.row(this);
            lr.setPadding(dp(4), dp(12), dp(4), dp(12));
            lr.setBackground(Ui.ripple(Ui.rounded(Color.TRANSPARENT, dp(10)), dp(10)));
            View dot = new View(this);
            dot.setBackground(Ui.rounded(Catalog.COLORS[c], dp(5)));
            lr.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));
            TextView lab = Ui.text(this, Deco.category(this, c) + Catalog.CATEGORIES[c], 15, Ui.TEXT, Ui.MEDIUM);
            lab.setPadding(dp(10), 0, 0, 0);
            lr.addView(lab, new LinearLayout.LayoutParams(0, -2, 1));
            TextView cnt = Ui.caption(this, "");
            cnt.setTag(c);
            sectionCounts.add(cnt);
            lr.addView(cnt);
            final ImageView chev = Ui.icon(this, R.drawable.ic_expand, Ui.TEXT2, 22);
            ((LinearLayout.LayoutParams) chev.getLayoutParams()).leftMargin = dp(6);
            lr.addView(chev);
            final LinearLayout body = Ui.column(this);
            body.setPadding(0, 0, 0, dp(8));
            lr.setContentDescription(Catalog.CATEGORIES[c]);
            sectionsBox.addView(lr);
            sectionsBox.addView(body);
            View div = Ui.divider(this);
            sectionsBox.addView(div);
            final boolean[] isOpen = {open.contains(String.valueOf(c))};
            chev.setRotation(isOpen[0] ? 180 : 0);
            if (isOpen[0]) pendingSections.add(new Object[]{body, c}); else body.setVisibility(View.GONE);
            lr.setOnClickListener(v -> {
                isOpen[0] = !isOpen[0];
                store.setSectionOpen(ci, isOpen[0]);
                chev.animate().rotation(isOpen[0] ? 180 : 0).setDuration(150).start();
                if (isOpen[0] && body.getChildCount() == 0) fillSection(body, ci);
                body.setVisibility(isOpen[0] ? View.VISIBLE : View.GONE);
            });
        }
        filter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) {
                pantryFilter = e.toString().trim();
                fclr.setVisibility(pantryFilter.isEmpty() ? View.GONE : View.VISIBLE);
                // v1.8: debounced; clearing the box is applied at once
                ui.removeCallbacks(filterRun);
                if (pantryFilter.isEmpty()) renderFilter(); else ui.postDelayed(filterRun, 140);
            }
        });
        filter.setOnEditorActionListener((v, a, e) -> { hideKeyboard(); return true; });
        renderFilter();
        right.addView(qc, Ui.lpMargins(this, -1, -2, 0, 0, 0, 12));

        lastPantrySig = null;
        lastLeftSig = null;
        lastRecentSig = null;
        pantryChipCache.clear();
        refreshPantryViews();
        // v1.8: open quick-add sections are filled one per frame after the screen is shown
        final LinearLayout sb = sectionsBox;
        ui.post(new Runnable() {
            public void run() {
                if (sectionsBox != sb || pendingSections.isEmpty()) return;
                Object[] o = pendingSections.remove(0);
                LinearLayout body = (LinearLayout) o[0];
                if (body.getChildCount() == 0) fillSection(body, (Integer) o[1]);
                if (!pendingSections.isEmpty()) ui.post(this);
            }
        });
        if (wide) {
            LinearLayout rowL = Ui.row(this);
            rowL.setGravity(Gravity.TOP);
            ScrollView s1 = scroll(left, pad);
            ScrollView s2 = scroll(right, pad);
            s2.setPadding(0, pad, pad, pad);
            rowL.addView(s1, new LinearLayout.LayoutParams(0, -1, 1f));
            rowL.addView(s2, new LinearLayout.LayoutParams(0, -1, 1.25f));
            return rowL;
        }
        left.addView(right);
        return scroll(left, pad);
    }

    /** One quick-add chip that toggles an ingredient in the pantry. */
    TextView quickChip(final String label, boolean register, boolean filterList) {
        return quickChip(label, Normalizer.normalize(label), register, filterList);
    }

    TextView quickChip(final String label, final String key, boolean register, boolean filterList) {
        final TextView chip = Ui.chip(this, label, store.pantry.containsKey(key));
        chip.setOnClickListener(v -> {
            if (store.pantry.containsKey(key)) store.removeFromPantry(key);
            else store.addToPantry(label, false);
            refreshPantryViews();
        });
        if (register) {
            if (filterList) { filterChipViews.add(chip); filterChipKeys.add(key); }
            else { quickChipViews.add(chip); quickChipKeys.add(key); }
        }
        return chip;
    }

    /** Builds one category's chips (lazily, the first time it is opened), split by subcategory. */
    void fillSection(LinearLayout body, int c) {
        FlowLayout f = null;
        java.util.Map<String, String> mv = V21.moves(this);
        List<String> keys = mv.isEmpty() ? PantryIndex.get().sectionKeys(c) : org.pantryrecipes.app.core.CategoryMoves.sectionKeys(c, mv);
        int ki = 0;
        for (String item : mv.isEmpty() ? java.util.Arrays.asList(Catalog.ITEMS[c]) : org.pantryrecipes.app.core.CategoryMoves.sectionItems(c, mv)) {
            if (Catalog.isHeader(item)) {
                TextView sub = Ui.text(this, item.substring(1), 12, Ui.TEXT2, Ui.MEDIUM);
                sub.setLetterSpacing(0.04f);
                sub.setPadding(dp(2), dp(10), 0, dp(6));
                body.addView(sub);
                f = null;
                continue;
            }
            if (f == null) {
                f = new FlowLayout(this, dp(8), dp(0)); // chips carry 6dp of touch padding above and below
                body.addView(f);
            }
            f.addView(quickChip(item, keys.get(ki++), true, false));
        }
    }

    /** Flat search over every catalog item and alias; offers a custom chip when nothing fits exactly. */
    void renderFilter() {
        if (filterFlow == null) return;
        filterFlow.removeAllViews();
        filterChipViews.clear();
        filterChipKeys.clear();
        boolean on = !pantryFilter.isEmpty();
        filterFlow.setVisibility(on ? View.VISIBLE : View.GONE);
        sectionsBox.setVisibility(on ? View.GONE : View.VISIBLE);
        recentBox.setVisibility(on || store.recents().isEmpty() ? View.GONE : View.VISIBLE);
        if (!on) return;
        String qk = Normalizer.normalize(pantryFilter);
        Map<String, String> pl = new java.util.LinkedHashMap<String, String>();
        for (Store.PantryItem p : store.pantry.values()) pl.put(p.key, p.label);
        PantryIndex.Hits hr = PantryIndex.get().search(pantryFilter, pl);
        List<String> hits = hr.labels;
        boolean exact = hr.exact;
        int shown = 0;
        for (int i = 0; i < hits.size(); i++) {
            if (shown++ >= 80) break;
            filterFlow.addView(quickChip(hits.get(i), hr.keys.get(i), true, true));
        }
        if (!exact && !qk.isEmpty()) {
            final String label = Ui.cap(pantryFilter);
            TextView add = Ui.chip(this, "+ Add \u201c" + label + "\u201d", false);
            add.setTextColor(Ui.GREEN_DARK);
            add.setOnClickListener(v -> {
                store.addToPantry(label, false);
                toast(label + " added to your pantry");
                refreshPantryViews();
                renderFilter();
            });
            filterFlow.addView(add);
        }
        if (hits.isEmpty()) {
            TextView none = Ui.caption(this, "Not in the list \u2013 add it anyway, it will be matched by name.");
            none.setPadding(dp(2), dp(6), 0, 0);
            filterFlow.addView(none);
        }
    }

    void renderRecents() {
        if (recentFlow == null) return;
        List<String> rec = store.recents();
        String sig = android.text.TextUtils.join("\n", rec);
        if (sig.equals(lastRecentSig)) {
            for (int i = 0; i < recentChipViews.size(); i++)
                Ui.styleChip(this, recentChipViews.get(i), store.pantry.containsKey(recentChipKeys.get(i)));
        } else {
            lastRecentSig = sig;
            recentFlow.removeAllViews();
            recentChipViews.clear();
            recentChipKeys.clear();
            for (String r : rec) {
                String k = Normalizer.normalize(r);
                TextView chip = quickChip(r, k, false, false);
                recentChipViews.add(chip);
                recentChipKeys.add(k);
                recentFlow.addView(chip);
            }
        }
        recentBox.setVisibility(rec.isEmpty() || !pantryFilter.isEmpty() ? View.GONE : View.VISIBLE);
    }

    ScrollView scroll(View inner, int pad) {
        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(pad, pad, pad, pad);
        sv.setFillViewport(true);
        sv.addView(inner);
        return sv;
    }

    View pantryChip(final Store.PantryItem p) {
        LinearLayout chip = Ui.row(this);
        Ui.chipPadding(this, chip, dp(14), dp(4));
        chip.setMinimumHeight(dp(48));
        int r = dp(18);
        chip.setBackground(Ui.tall(this, Ui.ripple(Ui.rounded(p.staple ? Ui.AMBER_TINT : Ui.GREEN_TINT, r), r)));
        if (p.staple) {
            ImageView star = Ui.icon(this, R.drawable.ic_star, Ui.STAR, 16);
            ((LinearLayout.LayoutParams) star.getLayoutParams()).rightMargin = dp(6);
            chip.addView(star);
        }
        TextView t = Ui.text(this, p.label, 14, p.staple ? Ui.AMBER_TEXT : Ui.GREEN_DARK, Ui.MEDIUM);
        chip.addView(t);
        String det = Extras.chipDetail(p);
        if (!det.isEmpty()) {
            int dl = UseBy.daysLeft(p.useBy, Dates.today());
            boolean due = dl != Integer.MIN_VALUE && dl <= UseBy.SOON_DAYS;
            TextView b = Ui.text(this, det, 12, due ? Extras.statusColor(dl) : Ui.TEXT2, Ui.MEDIUM);
            b.setPadding(dp(6), 0, 0, 0);
            chip.addView(b);
            chip.setContentDescription(p.label + ", " + det);
        }
        ImageView x = Ui.icon(this, R.drawable.ic_close, p.staple ? Ui.AMBER_TEXT : Ui.GREEN_DARK, 28);
        x.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(48)));
        x.setPadding(dp(9), dp(13), dp(9), dp(13));
        x.setContentDescription("Remove " + p.label);
        x.setOnClickListener(v -> { store.removeFromPantry(p.key); refreshPantryViews(); });
        chip.addView(x);
        chip.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(this, v);
            pm.getMenu().add(0, 1, 0, p.staple ? "Not a staple (keep in pantry)" : "Mark as always on hand");
            pm.getMenu().add(0, 3, 0, "Use-by date & amount\u2026");
            pm.getMenu().add(0, 2, 0, "Remove");
            pm.setOnMenuItemClickListener(mi -> {
                if (mi.getItemId() == 3) { Extras.editPantryItem(this, store, p, this::pantryDetailsChanged); return true; }
                if (mi.getItemId() == 1) store.setStaple(p.key, !p.staple);
                else store.removeFromPantry(p.key);
                refreshPantryViews();
                return true;
            });
            pm.show();
        });
        return chip;
    }

    void pantryDetailsChanged() {
        refreshPantryViews();
        refreshResults();
    }

    // v2.1: the user moved an ingredient to another category; quick-add sections are rebuilt
    void pantryCategoriesChanged() {
        if (tab == TAB_PANTRY) showTab(TAB_PANTRY, false);
    }

    EditText pantryFilterEdit;

    void setPantryFilter(String q) {
        if (pantryFilterEdit != null) { pantryFilterEdit.setText(q); pantryFilterEdit.setSelection(q.length()); }
        else { pantryFilter = q; renderFilter(); }
    }

    LinearLayout leftoversHolder;

    // v1.8: the Pantry is updated in place. Chips are reused (keyed by their content), cards are rebuilt
    // only when what they show changed, and quick-add chips are restyled only when their state flips.
    final List<Object[]> pendingSections = new ArrayList<Object[]>();
    final List<TextView> recentChipViews = new ArrayList<TextView>();
    final List<String> recentChipKeys = new ArrayList<String>();
    final Map<String, View> pantryChipCache = new java.util.HashMap<String, View>();
    String lastPantrySig, lastLeftSig, lastRecentSig;
    AutoCompleteTextView pantryInput;
    final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable filterRun = new Runnable() { public void run() { renderFilter(); } };

    static String chipSig(Store.PantryItem p) {
        return System.identityHashCode(p) + "|" + p.label + "|" + p.staple + "|" + p.useBy + "|" + p.qty;
    }

    void refreshPantryViews() {
        if (pantryFlow == null) return;
        String today = Dates.today();
        StringBuilder sigB = new StringBuilder(today);
        for (Store.PantryItem p : store.pantry.values()) sigB.append('\n').append(chipSig(p));
        String sig = sigB.toString();
        int n = 0, s = 0;
        for (Store.PantryItem p : store.pantry.values()) { if (p.staple) s++; else n++; }
        if (!sig.equals(lastPantrySig)) {
            lastPantrySig = sig;
            expiringHolder.removeAllViews();
            View exp = Extras.expiringCard(this, store, this::pantryDetailsChanged);
            if (exp != null) expiringHolder.addView(exp, Ui.lpMargins(this, -1, -2, 0, 0, 0, 12));
            pantryFlow.removeAllViews();
            stapleFlow.removeAllViews();
            Map<String, View> next = new java.util.HashMap<String, View>();
            for (Store.PantryItem p : store.pantry.values()) {
                String k = today + chipSig(p);
                View v = pantryChipCache.get(k);
                if (v == null) v = pantryChip(p);
                next.put(k, v);
                (p.staple ? stapleFlow : pantryFlow).addView(v);
            }
            pantryChipCache.clear();
            pantryChipCache.putAll(next);
            if (n == 0) {
                TextView empty = Ui.body(this, Deco.emptyGlyph(this) + "Your pantry is empty. Add a few things you have at home and we'll find recipes that use them.");
                empty.setTextColor(Ui.TEXT2);
                pantryFlow.addView(empty);
            }
            if (s == 0) stapleFlow.addView(Ui.caption(this, "No staples yet."));
            pantryCount.setText(n == 1 ? "1 ingredient" : n + " ingredients");
        }
        String ls = V16.leftoversSig(this) + today;
        if (!ls.equals(lastLeftSig)) {
            lastLeftSig = ls;
            leftoversHolder.removeAllViews();
            View lo = V16.leftoversCard(this, this::refreshPantryViews);
            if (lo != null) leftoversHolder.addView(lo, Ui.lpMargins(this, -1, -2, 0, 0, 0, 12));
        }
        for (int i = 0; i < quickChipViews.size(); i++)
            Ui.styleChip(this, quickChipViews.get(i), store.pantry.containsKey(quickChipKeys.get(i)));
        for (int i = 0; i < filterChipViews.size(); i++)
            Ui.styleChip(this, filterChipViews.get(i), store.pantry.containsKey(filterChipKeys.get(i)));
        Set<String> keys = store.pantry.keySet();
        PantryIndex idx = PantryIndex.get();
        for (TextView cnt : sectionCounts) {
            int c = (Integer) cnt.getTag();
            java.util.Map<String, String> mv = V21.moves(this);
            int have = mv.isEmpty() ? idx.have(c, keys) : org.pantryrecipes.app.core.CategoryMoves.have(c, mv, keys);
            int total = mv.isEmpty() ? idx.sectionKeys(c).size() : org.pantryrecipes.app.core.CategoryMoves.sectionKeys(c, mv).size();
            String txt = have > 0 ? have + " of " + total : String.valueOf(total);
            if (!txt.contentEquals(cnt.getText())) {
                cnt.setText(txt);
                cnt.setTextColor(have > 0 ? Ui.GREEN_DARK : Ui.TEXT3);
            }
        }
        renderRecents();
    }

    // ===================== recipes & favorites =====================
    View buildBrowse(final boolean fav) {
        browseRoot = new FrameLayout(this);
        LinearLayout listPanel = Ui.column(this);

        if (!fav) {
            LinearLayout sr = Ui.row(this);
            sr.setPadding(dp(14), 0, dp(2), 0);
            sr.setBackground(Ui.outlined(Ui.CARD, Ui.LINE, dp(1), dp(24)));
            sr.addView(Ui.icon(this, R.drawable.ic_search, Ui.TEXT3, 20));
            final EditText search = new EditText(this);
            search.setHint("Search recipes or ingredients");
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
            clr.setOnClickListener(v -> { search.setText(""); hideKeyboard(); });
            sr.addView(clr);
            search.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                public void onTextChanged(CharSequence s, int a, int b, int c) { }
                public void afterTextChanged(Editable e) {
                    query = e.toString().trim();
                    clr.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    refreshResults();
                }
            });
            search.setOnEditorActionListener((v, a, e) -> { hideKeyboard(); return true; });
            listPanel.addView(sr, Ui.lpMargins(this, -1, -2, 16, 6, 16, 8));

            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            hs.setClipToPadding(false);
            hs.setPadding(dp(16), 0, dp(16), 0);
            LinearLayout fr = Ui.row(this);
            TextView favChip = Ui.chip(this, "\u2665 Favorites", false);
            favChip.setOnClickListener(v -> showTab(TAB_FAVORITES, true));
            fr.addView(favChip, chipLp());
            fr.addView(sep());
            String[] modes = {"Best match", "Cook now", "Missing 1\u20132"};
            final List<TextView> modeChips = new ArrayList<TextView>();
            for (int i = 0; i < modes.length; i++) {
                final int m = i;
                TextView c = Ui.chip(this, modes[i], mode == i);
                c.setOnClickListener(v -> {
                    mode = m;
                    store.prefs.edit().putInt("mode", m).apply();
                    for (int k = 0; k < modeChips.size(); k++) Ui.styleChip(this, modeChips.get(k), k == mode);
                    refreshResults();
                });
                modeChips.add(c);
                fr.addView(c, chipLp());
            }
            fr.addView(sep());
            for (final String d : V20.ALL_DIETS) {
                final TextView c = Ui.chip(this, V20.dietName(d), diets.contains(d));
                c.setOnClickListener(v -> {
                    if (!diets.remove(d)) diets.add(d);
                    store.prefs.edit().putStringSet("diets", new HashSet<String>(diets)).apply();
                    Ui.styleChip(this, c, diets.contains(d));
                    refreshResults();
                });
                fr.addView(c, chipLp());
            }
            // v2.0 Stage 4: ticked fasting-day labels as optional diet chips (results cached per recipe)
            for (final String d : V30.fastChipIds(this)) {
                final TextView c = Ui.chip(this, V30.fastName(this, d), diets.contains(d));
                c.setContentDescription("Fasting label filter: " + V30.fastName(this, d));
                c.setOnClickListener(v -> {
                    if (!diets.remove(d)) diets.add(d);
                    store.prefs.edit().putStringSet("diets", new HashSet<String>(diets)).apply();
                    Ui.styleChip(this, c, diets.contains(d));
                    refreshResults();
                });
                fr.addView(c, chipLp());
            }
            fr.addView(sep());
            final int[] times = {0, 15, 30, 60};
            final List<TextView> timeChips = new ArrayList<TextView>();
            for (int i = 0; i < times.length; i++) {
                final int tm = times[i];
                TextView c = Ui.chip(this, tm == 0 ? "Any time" : "\u2264 " + tm + " min", maxTime == tm);
                c.setOnClickListener(v -> {
                    maxTime = tm;
                    store.prefs.edit().putInt("maxTime", tm).apply();
                    for (int k = 0; k < timeChips.size(); k++) Ui.styleChip(this, timeChips.get(k), times[k] == maxTime);
                    refreshResults();
                });
                timeChips.add(c);
                fr.addView(c, chipLp());
            }
            fr.addView(sep());
            V17.addMethodChips(this, fr, this::refreshResults);
            hs.addView(fr);
            listPanel.addView(hs, Ui.lpMargins(this, -1, -2, 0, 0, 0, 4));
            listPanel.addView(mealFilters(16, this::refreshResults));
        }

        if (fav) {
            // v1.8: Favorites is a chip under the Recipes tab; this row leads back to all recipes
            LinearLayout fr = Ui.row(this);
            TextView all = Ui.chip(this, "All recipes", false);
            all.setOnClickListener(v -> showTab(TAB_RECIPES, true));
            fr.addView(all, chipLp());
            fr.addView(Ui.chip(this, "\u2665 Favorites", true), chipLp());
            listPanel.addView(fr, Ui.lpMargins(this, -1, -2, 16, 6, 16, 4));
        }
        list = new ListView(this);
        list.setDivider(null);
        list.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        list.setClipToPadding(false);
        list.setPadding(dp(12), dp(4), dp(12), dp(16));
        LinearLayout header = Ui.column(this);
        header.setPadding(dp(4), dp(6), dp(4), 0);
        heroBox = Ui.column(this);
        statusBox = Ui.column(this);
        header.addView(heroBox);
        header.addView(statusBox);
        list.addHeaderView(header, null, false);
        adapter = new ResultAdapter(fav);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            Object o = adapter.getItem(pos - list.getHeaderViewsCount());
            if (o instanceof Matcher.Result) openDetail(((Matcher.Result) o).recipe.id);
            else if ("search".equals(o)) doOnlineSearch();
        });
        listPanel.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

        if (wide) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.TOP);
            row.addView(listPanel, new LinearLayout.LayoutParams(0, -1, 0.44f));
            detailPane = new FrameLayout(this);
            detailPane.setPadding(0, dp(6), dp(16), 0);
            row.addView(detailPane, new LinearLayout.LayoutParams(0, -1, 0.56f));
            browseRoot.addView(row);
        } else {
            detailPane = null;
            browseRoot.addView(listPanel);
        }
        refreshResults();
        if (wide) showDetailPlaceholderOrFirst();
        else if (detailOpen && selectedId != null && repo.byId(selectedId) != null) openDetail(selectedId, false);
        return browseRoot;
    }

    LinearLayout.LayoutParams chipLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(48));
        p.rightMargin = dp(8);
        return p;
    }

    View sep() {
        View v = new View(this);
        v.setBackgroundColor(Ui.LINE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(1), dp(24));
        p.rightMargin = dp(8);
        p.leftMargin = dp(2);
        v.setLayoutParams(p);
        return v;
    }

    List<String> hideDislikes = new ArrayList<String>();
    List<String> hideAvoid = new ArrayList<String>();
    int prefHiddenN;

    /** Recipes hidden by the user's dislikes / allergies (name-based; see Allergens.NOTE). */
    boolean prefHidden(Recipe r) {
        return (!hideDislikes.isEmpty() || !hideAvoid.isEmpty()) && Allergens.hidden(r, hideDislikes, hideAvoid);
    }

    Set<String> soonKeys() { return UseBy.soonKeys(store.useByMap(), Dates.today()); }

    /**
     * v1.8: every recipe matched against the pantry through the shared incremental MatchCache
     * (aligned with repo.all()); also refreshes {@link #matcher} and the dislike / allergy lists.
     */
    List<Matcher.Result> cachedMatches() {
        hideDislikes = store.dislikes();
        hideAvoid = V20.avoid(this);   // v2.0: + household members marked hide
        List<Matcher.Result> res = MatchCache.shared().match(repo.all(), store.pantryKeys(false), store.pantryKeys(true),
                repo.substitutions(), store.acceptedSubs());
        matcher = MatchCache.shared().matcher();
        return res;
    }

    Matcher newMatcher() {
        hideDislikes = store.dislikes();
        hideAvoid = V20.avoid(this);   // v2.0: + household members marked hide
        return new Matcher(store.pantryKeys(false), store.pantryKeys(true))
                .withSubstitutions(repo.substitutions(), store.acceptedSubs()).withSwapAvoid(store.dislikes(), store.avoidAllergens(), store.prefs.getStringSet("diets", new HashSet<String>()));
    }

    // ===================== meal types =====================
    boolean mealPasses(Recipe r) { return MealType.passes(r.meals, mealSel, incDesserts, incSides); }

    /** v2.0.1: Discover's own meal-type check (see dMealSel). */
    boolean dMealPasses(Recipe r) { return MealType.passes(r.meals, dMealSel, dIncDesserts, dIncSides); }
    boolean dMealHidden(Recipe r) { return !dMealPasses(r) && MealType.passes(r.meals, dMealSel, true, true); }

    /** Hidden only because of Include desserts / Include sides. */
    boolean mealHidden(Recipe r) { return !mealPasses(r) && MealType.passes(r.meals, mealSel, true, true); }

    void saveMealPrefs() {
        store.prefs.edit().putStringSet("meals", new HashSet<String>(mealSel))
                .putBoolean("incDesserts", incDesserts).putBoolean("incSides", incSides)
                .putStringSet("dMeals", new HashSet<String>(dMealSel))
                .putBoolean("dIncDesserts", dIncDesserts).putBoolean("dIncSides", dIncSides).apply();
    }

    CheckBox mealCheck(String label, boolean on) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label);
        cb.setTextSize(14);
        cb.setTextColor(Ui.TEXT2);
        cb.setChecked(on);
        cb.setButtonTintList(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{Ui.GREEN, Ui.TEXT3}));
        cb.setPadding(dp(4), 0, dp(12), 0);
        return cb;
    }

    /** Include desserts / Include sides check boxes plus a row of multi-select meal-type chips. */
    View mealFilters(int sidePad, final Runnable onChange) { return mealFilters(sidePad, onChange, false); }

    /** disc = true: the Discover tab's own filter state (v2.0.1). */
    View mealFilters(int sidePad, final Runnable onChange, final boolean disc) {
        final Set<String> mealSel = disc ? dMealSel : this.mealSel;
        LinearLayout col = Ui.column(this);
        LinearLayout cr = Ui.row(this);
        cr.setPadding(dp(sidePad - 6), 0, dp(sidePad), 0);
        final CheckBox d = mealCheck("Include desserts", disc ? dIncDesserts : incDesserts);
        final CheckBox sd = mealCheck("Include sides", disc ? dIncSides : incSides);
        d.setOnCheckedChangeListener((b, on) -> { if (disc) dIncDesserts = on; else incDesserts = on; saveMealPrefs(); onChange.run(); });
        sd.setOnCheckedChangeListener((b, on) -> { if (disc) dIncSides = on; else incSides = on; saveMealPrefs(); onChange.run(); });
        cr.addView(d, new LinearLayout.LayoutParams(-2, dp(48)));
        cr.addView(sd, new LinearLayout.LayoutParams(-2, dp(48)));
        col.addView(cr);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        hs.setPadding(dp(sidePad), 0, dp(sidePad), 0);
        LinearLayout fr = Ui.row(this);
        final List<TextView> chips = new ArrayList<TextView>();
        final TextView any = Ui.chip(this, "All meal types", mealSel.isEmpty());
        any.setOnClickListener(v -> {
            mealSel.clear();
            saveMealPrefs();
            Ui.styleChip(this, any, true);
            for (TextView c : chips) Ui.styleChip(this, c, false);
            onChange.run();
        });
        fr.addView(any, chipLp());
        for (final String m : MealType.ALL) {
            final TextView c = Ui.chip(this, Deco.meal(this, m) + m, mealSel.contains(m));
            c.setContentDescription("Meal type " + m);
            c.setOnClickListener(v -> {
                if (!mealSel.remove(m)) mealSel.add(m);
                List<String> o = MealType.sorted(mealSel);
                mealSel.clear();
                mealSel.addAll(o);
                saveMealPrefs();
                Ui.styleChip(this, c, mealSel.contains(m));
                Ui.styleChip(this, any, mealSel.isEmpty());
                onChange.run();
            });
            chips.add(c);
            fr.addView(c, chipLp());
        }
        hs.addView(fr);
        col.addView(hs, Ui.lpMargins(this, -1, -2, 0, 0, 0, 4));
        return col;
    }

    static String mealText(Recipe r, int max) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (String m : r.meals) {
            if (n == max) break;
            if (n++ > 0) b.append(" \u00b7 ");
            b.append(m);
        }
        return b.toString();
    }

    /** v1.8: per-refresh values hoisted out of the per-recipe filter (method filter, lower-cased query and its key). */
    static final class FilterCtx {
        java.util.Set<String> methods;
        String q = "", qk = "";
    }

    FilterCtx filterCtx() {
        FilterCtx c = new FilterCtx();
        c.methods = V17.methodFilter(this);
        if (!query.isEmpty()) {
            c.q = query.toLowerCase(Locale.US);
            c.qk = Normalizer.normalize(query);
        }
        return c;
    }

    boolean passesFilters(Recipe r, Matcher.Result res) { return passesFilters(r, res, filterCtx()); }

    boolean passesFilters(Recipe r, Matcher.Result res, FilterCtx fc) {
        if (!mealPasses(r)) return false;
        if (!V20.dietsPass(this, r, diets)) return false;   // v2.0: 15 diets (a guide, not certification)
        if (!V20.budgetPass(this, r)) return false;
        if (!V17.methodPasses(fc.methods, r)) return false;
        if (maxTime > 0) {
            int t = r.totalMinutes();
            if (t >= 0 && t > maxTime) return false; // unknown times are kept and labelled
        }
        if (mode == 1 && res.missingCount() > 0) return false;
        if (mode == 2 && (res.missingCount() < 1 || res.missingCount() > 2)) return false;
        if (!fc.q.isEmpty() && !r.searchHit(fc.q, fc.qk)) return false;
        return true;
    }

    void refreshResults() {
        if (list == null || adapter == null) return;
        matcher = newMatcher();
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        if (adapter.fav) {
            for (String id : store.favoriteIdsNewestFirst()) {
                Recipe r = repo.byId(id);
                if (r != null) out.add(matcher.match(r));
            }
        } else {
            mealHiddenN = 0;
            prefHiddenN = 0;
            List<Matcher.Result> all = cachedMatches();
            boolean anyPref = !hideDislikes.isEmpty() || !hideAvoid.isEmpty();
            FilterCtx fc = filterCtx();
            for (Matcher.Result res : all) {
                Recipe r = res.recipe;
                if (anyPref && prefHidden(r)) { prefHiddenN++; continue; }
                if (passesFilters(r, res, fc)) out.add(res);
                else if (mealHidden(r)) {
                    boolean saved = incDesserts, saved2 = incSides;
                    incDesserts = true; incSides = true;
                    if (passesFilters(r, res, fc)) mealHiddenN++;
                    incDesserts = saved; incSides = saved2;
                }
            }
            UseBy.sort(out, soonKeys());
        }
        adapter.set(out);
        updateHeader(out);
    }

    int mealHiddenN;

    void updateHeader(List<Matcher.Result> results) {
        heroBox.removeAllViews();
        statusBox.removeAllViews();
        if (adapter.fav) {
            if (!results.isEmpty()) {
                TextView t = Ui.caption(this, results.size() + (results.size() == 1 ? " saved recipe" : " saved recipes") + " \u00b7 newest first");
                t.setPadding(dp(4), dp(4), 0, dp(8));
                statusBox.addView(t);
            }
            return;
        }
        boolean pantryEmpty = matcher.pantryEmpty();
        Matcher.Result best = null;
        for (Matcher.Result r : results) if (r.haveNonStaple > 0 && MealType.preferred(r.recipe.meals, mealSel, incDesserts, incSides)) { best = r; break; }
        if (best == null) for (Matcher.Result r : results) if (r.haveNonStaple > 0 && MealType.acceptable(r.recipe.meals, mealSel, incDesserts, incSides)) { best = r; break; }

        LinearLayout hero = Ui.column(this);
        GradientDrawable g = Ui.rounded(Ui.FILL, dp(20)); // v1.8: flat fill, no gradient or shadow
        hero.setBackground(g);
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        TextView over = Ui.text(this, MealType.heroQuestion(mealSel), 12, Ui.ON_FILL2, Ui.MEDIUM);
        over.setLetterSpacing(0.08f);
        hero.addView(over);
        if (!repo.loaded) {
            hero.addView(heroTitle("Loading recipes\u2026"));
        } else if (pantryEmpty) {
            hero.addView(heroTitle("Add a few ingredients to get ideas"));
            TextView s = Ui.text(this, "Tell us what's in your kitchen and every recipe gets ranked by what you already have.", 14, Ui.ON_FILL2, Ui.REGULAR);
            s.setPadding(0, dp(4), 0, dp(12));
            hero.addView(s);
            TextView b = heroButton("Go to pantry", R.drawable.ic_kitchen);
            b.setOnClickListener(v -> showTab(TAB_PANTRY, true));
            hero.addView(b, new LinearLayout.LayoutParams(-2, dp(48)));
        } else if (best == null) {
            hero.addView(heroTitle("Nothing matches yet"));
            TextView s = Ui.text(this, "Try other filters or add more ingredients.", 14, Ui.ON_FILL2, Ui.REGULAR);
            s.setPadding(0, dp(4), 0, 0);
            hero.addView(s);
        } else {
            final Recipe r = best.recipe;
            hero.addView(heroTitle(r.title));
            List<String> ho = MealType.heroOrder(r.meals);
            String mt = android.text.TextUtils.join(" \u00b7 ", ho.subList(0, Math.min(3, ho.size())));
            if (!mt.isEmpty()) {
                TextView mtv = Ui.text(this, mt, 12, Ui.ON_FILL2, Ui.MEDIUM);
                mtv.setPadding(0, dp(2), 0, 0);
                hero.addView(mtv);
            }
            String sub = best.missingCount() == 0 ? "You have everything \u00b7 " : "You have " + best.have + " of " + best.total + " \u00b7 ";
            sub += Ui.timeText(r.totalMinutes());
            TextView s = Ui.text(this, sub, 14, Ui.ON_FILL2, Ui.REGULAR);
            s.setPadding(0, dp(2), 0, dp(12));
            hero.addView(s);
            LinearLayout br = Ui.row(this);
            TextView open = heroButton("Open recipe", R.drawable.ic_restaurant);
            open.setOnClickListener(v -> openDetail(r.id));
            br.addView(open, new LinearLayout.LayoutParams(-2, dp(48)));
            br.addView(Ui.space(this, 10, 1));
            TextView sur = heroButtonOutline("Surprise me", R.drawable.ic_shuffle);
            sur.setOnClickListener(v -> surprise());
            br.addView(sur, new LinearLayout.LayoutParams(-2, dp(48)));
            hero.addView(br);
        }
        heroBox.addView(hero, Ui.lpMargins(this, -1, -2, 0, 0, 0, 10));

        LinearLayout st = Ui.row(this);
        st.setPadding(dp(4), dp(2), dp(4), dp(6));
        int onlineN = 0;
        for (Matcher.Result r : results) if (r.recipe.isOnline()) onlineN++;
        String countText = results.size() + (results.size() == 1 ? " recipe" : " recipes");
        if (onlineN > 0) countText += " \u00b7 " + onlineN + " from TheMealDB";
        if (mealHiddenN > 0) countText += " \u00b7 " + mealHiddenN + " desserts/sides hidden";
        if (prefHiddenN > 0) countText += " \u00b7 " + prefHiddenN + " hidden by dislikes/allergies";
        st.addView(Ui.caption(this, countText), new LinearLayout.LayoutParams(0, -2, 1));
        if (repo.fetching) {
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
            st.addView(pb, new LinearLayout.LayoutParams(dp(16), dp(16)));
            st.addView(Ui.caption(this, "  Finding online recipes\u2026"));
        } else if (!repo.onlineEnabled()) {
            st.addView(Ui.caption(this, "Online recipes off"));
        } else if (!repo.isOnline()) {
            st.addView(Ui.icon(this, R.drawable.ic_cloud_off, Ui.TEXT3, 16));
            TextView off = Ui.caption(this, "  Offline \u2013 built-in & saved recipes");
            off.setTextColor(Ui.TEXT3);
            st.addView(off);
        } else if (!pantryEmpty) {
            TextView more = Ui.text(this, "Find more online", 13, Ui.GREEN_DARK, Ui.MEDIUM);
            leftIcon(more, R.drawable.ic_cloud, Ui.GREEN_DARK, 16);
            more.setPadding(dp(8), dp(6), dp(8), dp(6));
            more.setBackground(Ui.rippleRound(Color.TRANSPARENT, dp(14)));
            more.setOnClickListener(v -> {
                repo.lastFetchSignature = "";
                repo.fetchForPantry();
            });
            st.addView(more);
        }
        statusBox.addView(st);
        if (!repo.fetching && repo.lastFetchAdded == 0 && repo.lastError.isEmpty()) {
            TextView e = Ui.caption(this, "No new TheMealDB recipes for your latest ingredients.");
            e.setPadding(dp(4), 0, 0, dp(8));
            e.setTextColor(Ui.TEXT3);
            statusBox.addView(e);
        }
        if (!repo.fetching && !repo.lastError.isEmpty() && !"offline".equals(repo.lastError) && repo.isOnline()) {
            TextView e = Ui.caption(this, repo.lastError + ". Showing built-in and saved recipes.");
            e.setPadding(dp(4), 0, 0, dp(8));
            e.setTextColor(Ui.AMBER_TEXT);
            statusBox.addView(e);
        }
    }

    void leftIcon(TextView t, int res, int color, int sizeDp) {
        android.graphics.drawable.Drawable d = getResources().getDrawable(res).mutate();
        d.setTint(color);
        int s = dp(sizeDp);
        d.setBounds(0, 0, s, s);
        t.setCompoundDrawables(d, null, null, null);
        t.setCompoundDrawablePadding(dp(6));
    }

    TextView heroTitle(String s) {
        TextView t = Ui.text(this, s, 22, Ui.ON_FILL, Ui.MEDIUM);
        t.setPadding(0, dp(6), 0, 0);
        Ui.ellipsize(t, 2);
        return t;
    }

    TextView heroButton(String label, int icon) {
        TextView b = Ui.button(this, label, icon, false);
        b.setTextColor(Ui.FILL);
        android.graphics.drawable.Drawable[] ds = b.getCompoundDrawables();
        if (ds[0] != null) ds[0].setTint(Ui.FILL);
        int r = dp(21);
        b.setBackground(Ui.ripple(Ui.rounded(Ui.ON_FILL, r), r));
        return b;
    }

    TextView heroButtonOutline(String label, int icon) {
        TextView b = Ui.button(this, label, 0, false);
        b.setTextColor(Ui.ON_FILL);
        int r = dp(21);
        b.setBackground(Ui.ripple(Ui.outlined(0x22000000 | (Ui.ON_FILL & 0xFFFFFF), 0x99000000 | (Ui.ON_FILL & 0xFFFFFF), dp(1.5f), r), r));
        leftIcon(b, icon, Ui.ON_FILL, 18);
        b.setCompoundDrawablePadding(dp(8));
        return b;
    }

    /** Random recipe you can make now (or with 1-2 missing) from the current results. */
    /** Preferred: the selected meal types, or dinner/main dishes when none is selected; desserts/sides per the switches. */
    List<Matcher.Result> suggestable(List<Matcher.Result> items) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : items) if (MealType.preferred(r.recipe.meals, mealSel, incDesserts, incSides)) out.add(r);
        if (out.isEmpty()) for (Matcher.Result r : items) if (MealType.acceptable(r.recipe.meals, mealSel, incDesserts, incSides)) out.add(r);
        return out;
    }

    void surprise() {
        if (adapter == null) return;
        surprise(adapter.items);
    }

    void surprise(List<Matcher.Result> items) {
        List<Matcher.Result> meals = suggestable(items);
        if (!meals.isEmpty()) items = meals;
        List<Matcher.Result> pool = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : items) if (r.missingCount() == 0 && r.haveNonStaple > 0) pool.add(r);
        if (pool.isEmpty()) for (Matcher.Result r : items) if (r.missingCount() <= 2 && r.haveNonStaple > 0) pool.add(r);
        if (pool.isEmpty()) for (Matcher.Result r : items) if (r.haveNonStaple > 0) pool.add(r);
        if (pool.isEmpty()) pool = new ArrayList<Matcher.Result>(items); // v2.2: empty pantry -> any recipe that passes the filters
        if (pool.isEmpty()) { toast("No recipes pass your filters"); return; }
        if (pool.size() > 1 && selectedId != null) {
            for (int i = 0; i < pool.size(); i++) if (pool.get(i).recipe.id.equals(selectedId)) { pool.remove(i); break; }
        }
        openDetail(pool.get(new Random().nextInt(pool.size())).recipe.id);
    }

    void doOnlineSearch() {
        final String q = query;
        if (q.isEmpty()) return;
        if (!repo.onlineEnabled()) { toast("Online recipes are turned off (see About)"); return; }
        toast("Searching TheMealDB for \u201c" + q + "\u201d\u2026");
        repo.searchOnline(q, (results, error) -> {
            if (error != null) toast(error);
            else if (results.isEmpty()) toast("No TheMealDB recipes found for \u201c" + q + "\u201d");
            else toast(results.size() + " online recipe" + (results.size() == 1 ? "" : "s") + " added");
        });
    }

    void showDetailPlaceholderOrFirst() {
        if (detailPane == null) return;
        Recipe r = selectedId == null ? null : repo.byId(selectedId);
        if (r != null && adapter != null && adapter.fav && !store.isFavorite(r.id)) r = null;
        if (r == null && adapter != null && !adapter.items.isEmpty()) {
            Matcher.Result first = adapter.items.get(0);
            if (first.haveNonStaple > 0 || adapter.fav) r = first.recipe;
        }
        detailPane.removeAllViews();
        if (r == null) {
            LinearLayout ph = Ui.column(this);
            ph.setGravity(Gravity.CENTER);
            ph.setBackground(Ui.rounded(Ui.CARD, dp(20)));
            ph.addView(Ui.icon(this, R.drawable.ic_restaurant, Ui.GREEN_TINT2, 64));
            TextView t = Ui.title(this, adapter != null && adapter.fav ? "No favorite selected" : "Pick a recipe", 18);
            t.setPadding(0, dp(12), 0, dp(4));
            ph.addView(t);
            ph.addView(Ui.caption(this, "Recipe details, ingredients and steps show up here."));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
            lp.bottomMargin = dp(16);
            lp.topMargin = dp(6);
            detailPane.addView(ph, lp);
            return;
        }
        selectedId = r.id;
        if (detailServings <= 0) detailServings = store.defaultServings();
        detailPane.addView(buildDetail(r, false), new FrameLayout.LayoutParams(-1, -1));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    void openDetail(String id) { openDetail(id, true); }

    void openDetail(String id, boolean animate) {
        Recipe r = repo.byId(id);
        if (r == null) return;
        selectedId = id;
        store.markSeen(id);
        detailServings = store.defaultServings();
        hideKeyboard();
        if (wide && detailPane != null) {
            detailPane.removeAllViews();
            View d = buildDetail(r, false);
            detailPane.addView(d, new FrameLayout.LayoutParams(-1, -1));
            if (animate) {
                d.setAlpha(0f);
                d.animate().alpha(1f).setDuration(180).start();
            }
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }
        if (browseRoot == null) return;
        if (detailOverlay != null) browseRoot.removeView(detailOverlay);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Ui.BG);
        overlay.setClickable(true);
        overlay.setPadding(dp(12), 0, dp(12), 0);
        overlay.addView(buildDetail(r, true), new FrameLayout.LayoutParams(-1, -1));
        browseRoot.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        detailOverlay = overlay;
        detailOpen = true;
        if (animate) {
            overlay.setTranslationX(dp(60));
            overlay.setAlpha(0f);
            overlay.animate().translationX(0).alpha(1f).setDuration(200).start();
        }
    }

    void closeDetail() {
        detailOpen = false;
        final View o = detailOverlay;
        detailOverlay = null;
        if (o != null) {
            o.animate().translationX(dp(60)).alpha(0f).setDuration(160).withEndAction(() -> {
                if (o.getParent() != null) ((ViewGroup) o.getParent()).removeView(o);
            }).start();
        }
        if (tab == TAB_DISCOVER) {
            if (discoverSub != null && subRefresh != null) subRefresh.run(); else refreshDiscover();
        }
        refreshResults();
    }

    /** Rebuilds the open recipe (after a substitute was accepted) and keeps the scroll position. */
    void reopenDetail(String id) {
        final int y = detailScroll == null ? 0 : detailScroll.getScrollY();
        int servings = detailServings;
        matcher = newMatcher();
        openDetail(id, false);
        detailServings = servings;
        if (detailScroll != null) {
            final ScrollView sv = detailScroll;
            sv.post(() -> sv.scrollTo(0, y));
        }
        if (tab == TAB_RECIPES || tab == TAB_FAVORITES) refreshResults();
    }

    void openSubs(String q) {
        Intent i = new Intent(this, SubsActivity.class);
        if (q != null) i.putExtra("q", q);
        startActivity(i);
    }

    // ===================== discover =====================
    interface ListSource { List<Matcher.Result> get(); }

    TextView subStatus, subMore;

    View buildDiscover() {
        browseRoot = new FrameLayout(this);
        detailPane = null;
        discoverScroll = new ScrollView(this);
        discoverScroll.setClipToPadding(false);
        discoverScroll.setPadding(0, dp(8), 0, dp(16));
        discoverBody = Ui.column(this);
        discoverScroll.addView(discoverBody);
        browseRoot.addView(discoverScroll, new FrameLayout.LayoutParams(-1, -1));
        refreshDiscover();
        if (detailOpen && selectedId != null && repo.byId(selectedId) != null) openDetail(selectedId, false);
        else detailOpen = false;
        return browseRoot;
    }

    /** All recipes that pass Discover's own meal-type filter. v2.0.1: no Recipes-tab filters (methods, diets, time...). */
    List<Matcher.Result> matchFiltered() {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : matchAll()) if (dMealPasses(r.recipe)) out.add(r);
        return out;
    }

    List<Matcher.Result> matchAll() {
        List<Matcher.Result> all = cachedMatches();
        if (hideDislikes.isEmpty() && hideAvoid.isEmpty()) return all;
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Matcher.Result r : all) if (!prefHidden(r.recipe)) out.add(r);
        return out;
    }

    int shuffleN = 0;
    int pickSeed(String salt) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return Variety.daySeed(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH), salt + (shuffleN > 0 ? "#" + shuffleN : ""));
    }

    void refreshDiscover() {
        if (discoverBody == null) return;
        final int y = discoverScroll.getScrollY();
        discoverBody.removeAllViews();
        if (!repo.loaded) {
            TextView t = Ui.caption(this, "Loading recipes\u2026");
            t.setPadding(dp(20), dp(20), dp(20), dp(20));
            discoverBody.addView(t);
            return;
        }
        final List<Recipe> all = repo.all();
        final List<Matcher.Result> results = matchFiltered();
        final Set<String> seen = store.seenIds();
        boolean pantryEmpty = matcher.pantryEmpty();

        // search + random
        LinearLayout top = Ui.card(this);
        LinearLayout sr = Ui.row(this);
        sr.setPadding(dp(14), 0, dp(4), 0);
        sr.setBackground(Ui.outlined(Ui.BG, Ui.LINE, dp(1), dp(24)));
        sr.addView(Ui.icon(this, R.drawable.ic_search, Ui.TEXT3, 20));
        final EditText search = new EditText(this);
        search.setHint("Search recipes by name");
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.TEXT3);
        search.setBackground(null);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(10), 0, dp(4), 0);
        sr.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1));
        final Runnable go = () -> {
            String q = search.getText().toString().trim();
            if (q.isEmpty()) return;
            hideKeyboard();
            openSearchSub(q);
        };
        search.setOnEditorActionListener((v, a, e) -> { go.run(); return true; });
        TextView goBtn = Ui.text(this, "Search", 14, Ui.GREEN_DARK, Ui.MEDIUM);
        goBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        goBtn.setBackground(Ui.rippleRound(Ui.GREEN_TINT, dp(18)));
        goBtn.setOnClickListener(v -> go.run());
        sr.addView(goBtn);
        top.addView(sr);
        LinearLayout mf = Ui.column(this);
        mf.setPadding(0, dp(8), 0, 0);
        mf.addView(mealFilters(0, this::refreshDiscover, true));
        top.addView(mf);
        HorizontalScrollView bhs = new HorizontalScrollView(this);
        bhs.setHorizontalScrollBarEnabled(false);
        LinearLayout br = Ui.row(this);
        br.setPadding(0, dp(12), 0, 0);
        // v2.2: Surprise me (any recipe passing Discover's meal filter) and Try something new (an unopened recipe from your least-opened cuisine)
        TextView sme = Ui.button(this, "Surprise me", R.drawable.ic_shuffle, true);
        sme.setContentDescription("Surprise me: a random recipe that passes your meal filters");
        sme.setOnClickListener(v -> {
            Matcher.Result r = Variety.one(matchFiltered(), Variety.seed(String.valueOf(System.nanoTime())), selectedId);
            if (r == null) toast("No recipes pass your meal filters"); else openDetail(r.recipe.id);
        });
        br.addView(sme, new LinearLayout.LayoutParams(-2, dp(48)));
        br.addView(Ui.space(this, 10, 1));
        TextView tnw = Ui.button(this, "Try something new", R.drawable.ic_explore, false);
        tnw.setContentDescription("Try something new: an unopened recipe from a cuisine you rarely open");
        tnw.setOnClickListener(v -> {
            Matcher.Result r = Variety.tryNew(matchFiltered(), store.seenIds(), Variety.seed(String.valueOf(System.nanoTime())));
            if (r == null) toast("No recipes pass your meal filters"); else openDetail(r.recipe.id);
        });
        br.addView(tnw, new LinearLayout.LayoutParams(-2, dp(48)));
        br.addView(Ui.space(this, 10, 1));
        TextView rnd = Ui.button(this, "Random recipe", R.drawable.ic_cloud, false);
        rnd.setOnClickListener(v -> randomRecipe());
        br.addView(rnd, new LinearLayout.LayoutParams(-2, dp(48)));
        br.addView(Ui.space(this, 10, 1));
        TextView sup = Ui.button(this, "Surprise me from my pantry", R.drawable.ic_kitchen, false);
        sup.setOnClickListener(v -> {
            if (matcher.pantryEmpty()) { toast("Add some ingredients to your pantry first"); showTab(TAB_PANTRY, true); return; }
            surprise(matchAll());
        });
        br.addView(sup, new LinearLayout.LayoutParams(-2, dp(48)));
        bhs.addView(br);
        top.addView(bhs);
        if (!repo.onlineEnabled() || !repo.isOnline()) {
            LinearLayout off = Ui.row(this);
            off.setPadding(0, dp(10), 0, 0);
            off.addView(Ui.icon(this, R.drawable.ic_cloud_off, Ui.TEXT3, 16));
            TextView ot = Ui.caption(this, repo.onlineEnabled() ? "  Offline \u2013 showing built-in and saved recipes" : "  Online recipes are off (see About)");
            off.addView(ot);
            top.addView(off);
        } else if (repo.fetching) {
            TextView ft = Ui.caption(this, "Finding online recipes for your pantry\u2026");
            ft.setPadding(0, dp(10), 0, 0);
            top.addView(ft);
        }
        discoverBody.addView(top, Ui.lpMargins(this, -1, -2, 16, 4, 16, 8));
        if (!pantryEmpty) {
            View bn = V16.buyNextCard(this, results);
            if (bn != null) discoverBody.addView(bn, Ui.lpMargins(this, -1, -2, 16, 4, 16, 8));
        }

        // v2.2: today's picks (seeded by date, Shuffle bumps the salt) and two rotating cuisine rows
        carousel("Today's picks" + (shuffleN > 0 ? " (shuffled)" : ""), "A fresh mix every day. Shuffle gives another set.",
                Variety.picks(results, pickSeed("picks"), 12), () -> Variety.picks(matchFiltered(), pickSeed("picks"), 1000),
                "No recipes pass your meal filters.");
        TextView shf = Ui.button(this, "Shuffle today's picks", R.drawable.ic_shuffle, false);
        shf.setOnClickListener(v -> { shuffleN++; refreshDiscover(); toast("Shuffled"); });
        discoverBody.addView(shf, Ui.lpMargins(this, -2, dp(48), 16, 0, 16, 8));
        for (final String cz : Variety.rotatingCuisines(results, pickSeed("cuisines"), 2, 8)) {
            carousel("Cuisine of the day: " + cz, "Rotates daily",
                    Variety.picks(Variety.inCuisine(results, cz), pickSeed("picks"), 12),
                    () -> Variety.picks(Variety.inCuisine(matchFiltered(), cz), pickSeed("picks"), 1000), "");
        }

        final Set<String> soon = soonKeys();
        if (!soon.isEmpty()) {
            List<Matcher.Result> uu = UseBy.useItUp(results, soon, 12);
            carousel("Use it up", "Uses pantry items due within " + UseBy.SOON_DAYS + " days",
                    uu, () -> UseBy.useItUp(matchFiltered(), soonKeys(), 1000),
                    "None of your recipes use the items that are due soon.");
        }
        carousel("New for you", "Recipes you haven't opened yet, best match first",
                Discover.newForYou(results, seen, 12),
                () -> Discover.newForYou(matchFiltered(), store.seenIds(), 1000),
                "You've opened every recipe! Browse a cuisine below to load more from TheMealDB.");
        carousel("Quick (under 30 min)", "Ready in half an hour or less",
                Discover.quick(results, 12), () -> Discover.quick(matchFiltered(), 1000),
                "No quick recipes yet.");
        carousel("Almost there", "Missing just 1\u20132 ingredients",
                Discover.almostThere(results, 12), () -> Discover.almostThere(matchFiltered(), 1000),
                pantryEmpty ? "Add ingredients to your pantry to see recipes you're one or two items away from."
                        : "Nothing is one or two ingredients away. Add a few more pantry items.");
        final List<Recipe> favs = new ArrayList<Recipe>();
        for (String id : store.favoriteIdsNewestFirst()) {
            Recipe r = repo.byId(id);
            if (r != null) favs.add(r);
        }
        if (!store.ratings.isEmpty()) {
            carousel("Your top rated", "Recipes you rated, best first", topRated(12), () -> topRated(1000), "");
        }
        carousel("Based on your favorites", "Similar ingredients, cuisine or course",
                likeFavorites(favs, 12), () -> likeFavorites(favs, 60),
                "Tap the heart on recipes you like and similar ones show up here.");

        mealFacetCard(all);
        discoverBody.addView(V17.methodsCard(this, all));
        facetCard("Browse by cuisine", 'a', Discover.facet(all, true), repo.mealDbNames(true));
        facetCard("Browse by category", 'c', Discover.facet(all, false), repo.mealDbNames(false));

        discoverBody.addView(V22.sourcesCard(this), Ui.lpMargins(this, -1, -2, 16, 8, 16, 8));
        int seenN = 0;
        for (Recipe r : all) if (seen.contains(r.id)) seenN++;
        LinearLayout foot = Ui.row(this);
        foot.setPadding(dp(20), dp(8), dp(20), dp(8));
        foot.addView(Ui.caption(this, "You've opened " + seenN + " of " + all.size() + " recipes"), new LinearLayout.LayoutParams(0, -2, 1));
        if (seenN > 0) {
            TextView reset = Ui.text(this, "Reset", 13, Ui.GREEN_DARK, Ui.MEDIUM);
            reset.setPadding(dp(10), dp(6), dp(10), dp(6));
            reset.setOnClickListener(v -> dialog().setTitle("Reset \u201cNew for you\u201d?")
                    .setMessage("Every recipe will count as new again.")
                    .setPositiveButton("Reset", (d, w) -> { store.clearSeen(); refreshDiscover(); })
                    .setNegativeButton("Cancel", null).show());
            foot.addView(reset);
        }
        discoverBody.addView(foot);
        discoverScroll.post(() -> discoverScroll.scrollTo(0, y));
    }

    List<Matcher.Result> topRated(int limit) {
        if (matcher == null) matcher = newMatcher();
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (final String id : store.ratings.keySet()) {
            Recipe r = repo.byId(id);
            if (r != null && store.stars(id) > 0) out.add(matcher.match(r));
        }
        Collections.sort(out, (a, b) -> {
            int c = store.stars(b.recipe.id) - store.stars(a.recipe.id);
            return c != 0 ? c : Matcher.compare(a, b);
        });
        return out.size() > limit ? new ArrayList<Matcher.Result>(out.subList(0, limit)) : out;
    }

    List<Matcher.Result> likeFavorites(List<Recipe> favs, int limit) {
        if (matcher == null) matcher = newMatcher();
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        for (Recipe r : Discover.basedOnFavorites(repo.all(), favs, limit * 3)) {
            if (out.size() >= limit) break;
            if (dMealPasses(r) && !prefHidden(r)) out.add(matcher.match(r));
        }
        return out;
    }

    void carousel(final String title, String sub, List<Matcher.Result> items, final ListSource all, String empty) {
        LinearLayout head = Ui.row(this);
        head.setPadding(dp(20), dp(14), dp(12), dp(2));
        LinearLayout tt = Ui.column(this);
        tt.addView(Ui.title(this, title, 17));
        tt.addView(Ui.caption(this, sub));
        head.addView(tt, new LinearLayout.LayoutParams(0, -2, 1));
        if (items.size() >= 4) {
            TextView see = Ui.text(this, "See all", 14, Ui.GREEN_DARK, Ui.MEDIUM);
            see.setPadding(dp(10), dp(8), dp(10), dp(8));
            see.setBackground(Ui.rippleRound(Color.TRANSPARENT, dp(16)));
            see.setOnClickListener(v -> openDiscoverSub(title, all, null));
            head.addView(see);
        }
        discoverBody.addView(head);
        if (items.isEmpty()) {
            TextView e = Ui.caption(this, empty);
            e.setPadding(dp(20), dp(6), dp(20), dp(6));
            discoverBody.addView(e);
            return;
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        hs.setPadding(dp(16), dp(8), dp(16), dp(8));
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.TOP);
        for (Matcher.Result r : items) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(156), -2);
            lp.rightMargin = dp(10);
            row.addView(recipeCard(r), lp);
        }
        hs.addView(row);
        discoverBody.addView(hs);
    }

    View recipeCard(final Matcher.Result res) {
        final Recipe r = res.recipe;
        LinearLayout c = Ui.column(this);
        int rad = dp(14);
        c.setBackground(Ui.ripple(Ui.outlined(Ui.CARD, Ui.LINE, dp(1), rad), rad));
        FrameLayout thumb = new FrameLayout(this);
        int cc = Ui.categoryColor(r.category);
        float rr = rad * Ui.RAD;
        boolean illo = false;
        if (illo) thumb.setBackground(IllustrationDrawable.forRecipe(this, r, rr, true)); // v2.2: illustrated card
        else {
            GradientDrawable tb = new GradientDrawable();
            tb.setColor(cc);
            tb.setCornerRadii(new float[]{rr, rr, rr, rr, 0, 0, 0, 0});
            thumb.setBackground(tb);
        }
        TextView letter = Ui.text(this, "No photo", 12, Ui.TEXT, Ui.REGULAR); // v2.0 a11y: dark ink reads on every tile colour
        letter.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        letter.setGravity(Gravity.CENTER);
        thumb.setBackground(Ui.rounded(Ui.NEUTRAL, rad));
        thumb.addView(letter, new FrameLayout.LayoutParams(-1, -1));
        ImageView cardPhoto = new ImageView(this);
        cardPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cardPhoto.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        thumb.addView(cardPhoto, new FrameLayout.LayoutParams(-1, -1));
        ImageLoader.get(this).load(cardPhoto, r.image, true, store.prefs.getBoolean("online", true));
        if (store.isFavorite(r.id)) {
            ImageView h = Ui.icon(this, R.drawable.ic_heart, Color.WHITE, 18);
            FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.TOP | Gravity.RIGHT);
            hl.topMargin = dp(8);
            hl.rightMargin = dp(8);
            h.setLayoutParams(hl);
            thumb.addView(h);
        }
        c.addView(thumb, new LinearLayout.LayoutParams(-1, dp(92)));
        LinearLayout body = Ui.column(this);
        body.setPadding(dp(10), dp(8), dp(10), dp(10));
        TextView t = Ui.text(this, r.title, 14, Ui.TEXT, Ui.MEDIUM);
        Ui.ellipsize(t, 2);
        t.setMinLines(2);
        body.addView(t);
        String m;
        if (res.haveNonStaple == 0) m = res.total + " ingredients";
        else if (res.missingCount() == 0) m = "Cook now";
        else m = "Missing " + res.missingCount();
        int tm = r.totalMinutes();
        if (tm > 0) m += " \u00b7 " + Ui.timeText(tm);
        TextView mt = Ui.text(this, m, 12, res.missingCount() == 0 && res.haveNonStaple > 0 ? Ui.GREEN_DARK : Ui.TEXT2, Ui.REGULAR);
        mt.setSingleLine(true);
        mt.setEllipsize(android.text.TextUtils.TruncateAt.END);
        mt.setPadding(0, dp(4), 0, 0);
        body.addView(mt);
        TextView ml = Ui.text(this, mealText(r, 2), 11, Ui.GREEN_DARK, Ui.MEDIUM);
        ml.setSingleLine(true);
        ml.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ml.setPadding(0, dp(3), 0, 0);
        body.addView(ml);
        c.addView(body);
        c.setOnClickListener(v -> openDetail(r.id));
        c.setContentDescription(r.title);
        return c;
    }

    void facetCard(String title, final char kind, Map<String, Integer> local, List<String> remote) {
        TextView h = Ui.title(this, title, 17);
        h.setPadding(dp(20), dp(16), dp(20), dp(8));
        discoverBody.addView(h);
        List<String> names = new ArrayList<String>(local.keySet());
        for (String n : remote) {
            boolean dup = false;
            for (String x : names) if (x.equalsIgnoreCase(n)) { dup = true; break; }
            if (!dup) names.add(n);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        Map<String, List<String>> groups = new java.util.LinkedHashMap<String, List<String>>();
        if (kind == 'a' && store.prefs.getBoolean("cuisineGroups", true)) groups.putAll(org.pantryrecipes.app.core.Cuisines.grouped(names));
        else groups.put("", names);
        for (Map.Entry<String, List<String>> ge : groups.entrySet()) {
            if (ge.getValue().isEmpty()) continue;
            if (!ge.getKey().isEmpty()) {
                TextView gl = Ui.caption(this, ge.getKey());
                gl.setPadding(dp(20), dp(8), dp(20), dp(4));
                discoverBody.addView(gl);
            }
            FlowLayout f = new FlowLayout(this, dp(8), dp(8));
            f.setPadding(dp(16), 0, dp(16), dp(4));
            for (final String n : ge.getValue()) {
                Integer cnt = local.get(n);
                TextView chip = Ui.chip(this, cnt == null ? n : n + "  " + cnt, false);
                chip.setOnClickListener(v -> openBrowseSub(kind, n));
                f.addView(chip);
            }
            discoverBody.addView(f);
        }
        if (remote.isEmpty() && repo.onlineEnabled()) {
            TextView note = Ui.caption(this, repo.isOnline() ? "Loading TheMealDB's list\u2026" : "More appear when you're online (from TheMealDB).");
            note.setPadding(dp(20), dp(6), dp(20), 0);
            discoverBody.addView(note);
        }
    }

    void mealFacetCard(List<Recipe> all) {
        TextView h = Ui.title(this, "Browse by meal type", 17);
        h.setPadding(dp(20), dp(16), dp(20), dp(8));
        discoverBody.addView(h);
        Map<String, Integer> cnt = new HashMap<String, Integer>();
        for (Recipe r : all) for (String m : r.meals) { Integer o = cnt.get(m); cnt.put(m, o == null ? 1 : o + 1); }
        FlowLayout f = new FlowLayout(this, dp(8), dp(8));
        f.setPadding(dp(16), 0, dp(16), dp(4));
        for (final String m : MealType.ALL) {
            Integer n = cnt.get(m);
            TextView chip = Ui.chip(this, n == null ? m : m + "  " + n, false);
            chip.setContentDescription("Browse " + m);
            chip.setOnClickListener(v -> openBrowseSub('m', m));
            f.addView(chip);
        }
        discoverBody.addView(f);
    }

    /** Meal-type filtering for a Discover list; remembers how many were hidden by the switches. */
    int subHidden;
    List<Matcher.Result> mealFilter(List<Matcher.Result> in) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        int hidden = 0;
        for (Matcher.Result r : in) {
            if (dMealPasses(r.recipe)) out.add(r);
            else if (dMealHidden(r.recipe)) hidden++;
        }
        subHidden = hidden;
        return out;
    }

    void openSearchSub(final String q) {
        openDiscoverSub("\u201c" + q + "\u201d", () -> {
            List<Recipe> hits = Discover.searchTitles(repo.all(), q, 500);
            if (hits.isEmpty()) {
                // fall back to ingredient search
                String qk = Normalizer.normalize(q);
                for (Recipe r : repo.all()) for (Ingredient i : r.ingredients) if (!qk.isEmpty() && i.key.equals(qk)) { hits.add(r); break; }
            }
            if (matcher == null) matcher = newMatcher();
            List<Matcher.Result> out = new ArrayList<Matcher.Result>();
            for (Recipe r : hits) out.add(matcher.match(r));
            Collections.sort(out, Matcher::compare);
            return mealFilter(out);
        }, () -> {
            if (!repo.onlineEnabled() || !repo.isOnline()) { setSubStatus(repo.onlineEnabled() ? "Offline \u2013 searched built-in and saved recipes" : "", false); return; }
            setSubStatus("Searching TheMealDB\u2026", false);
            repo.searchOnline(q, (results, error) -> {
                if (error != null) setSubStatus(error + " \u2013 showing built-in and saved recipes", false);
                else setSubStatus(results.isEmpty() ? "No TheMealDB matches" : results.size() + " from TheMealDB", false);
            });
        });
    }

    void openBrowseSub(final char kind, final String value) {
        final Runnable[] load = new Runnable[1];
        // meal types: online results come from the matching TheMealDB category, when there is one
        final char okind = kind == 'm' ? 'c' : kind;
        final String ovalue = kind == 'm' ? MealType.mealDbCategory(value) : kind == 'k' ? "" : value;
        final String methodId = kind == 'k' ? V17.methodIdForLabel(value) : null;
        load[0] = () -> {
            if (!repo.onlineEnabled() || ovalue.isEmpty()) { setSubStatus("", false); return; }
            if (!repo.isOnline()) { setSubStatus("Offline \u2013 showing built-in and saved recipes", false); return; }
            setSubStatus("Loading from TheMealDB\u2026", false);
            repo.browseOnline(okind, ovalue, 16, (added, remaining, error) -> {
                if (error != null) setSubStatus(error, false);
                else setSubStatus(remaining > 0 ? remaining + " more on TheMealDB" : (added > 0 ? "Added " + added + " from TheMealDB" : "All of TheMealDB's loaded"), remaining > 0);
            });
        };
        openDiscoverSub(value, () -> {
            List<Recipe> src = new ArrayList<Recipe>();
            Set<String> ids = new HashSet<String>();
            for (Recipe r : repo.all()) {
                boolean hit = kind == 'k' ? V17.methods(r).contains(methodId) : kind == 'a' ? r.cuisine.equalsIgnoreCase(value) : kind == 'm' ? r.meals.contains(value) : Discover.inCategory(r, value);
                if (hit && ids.add(r.id)) src.add(r);
            }
            if (!ovalue.isEmpty()) for (Recipe r : repo.cachedBrowse(okind, ovalue)) if ((kind != 'm' || r.meals.contains(value)) && ids.add(r.id)) src.add(r);
            if (matcher == null) matcher = newMatcher();
            List<Matcher.Result> out = new ArrayList<Matcher.Result>();
            for (Recipe r : src) out.add(matcher.match(r));
            Collections.sort(out, Matcher::compare);
            // cuisines follow the meal-type filters; a picked category or meal type is shown in full
            if (kind == 'a') return mealFilter(out);
            subHidden = 0;
            return out;
        }, load[0]);
        if (subMore != null) subMore.setOnClickListener(v -> load[0].run());
    }

    /** v2.0 Stage 3: a fixed recipe list (holiday section, fasting label, baking group) in the Discover sub-list. */
    void openRecipeList(String title, final List<Recipe> rs, final boolean keepOrder, final String caption) {
        if (tab != TAB_DISCOVER || browseRoot == null) showTab(TAB_DISCOVER, false);
        openDiscoverSub(title, () -> {
            if (matcher == null) matcher = newMatcher();
            List<Matcher.Result> out = new ArrayList<Matcher.Result>();
            for (Recipe r : rs) out.add(matcher.match(r));
            if (!keepOrder) Collections.sort(out, Matcher::compare);
            subHidden = 0;
            return out;
        }, () -> setSubStatus(caption == null ? "" : caption, false));
    }

    void openCuisine(String cuisine) {
        if (tab != TAB_DISCOVER || browseRoot == null) showTab(TAB_DISCOVER, false);
        openBrowseSub('a', cuisine);
    }

    /** Fasting labels or the "show as chips" option changed: drop stale chip selections and rebuild the Pantry filters. */
    void fastFiltersChanged() {
        V30.dropStaleFastDiets(this, diets);
        if (tab == TAB_PANTRY) showTab(TAB_PANTRY, false);
    }

    void refreshHolidayBanner() {
        if (holidayHolder != null) { try { V30.banner(this, holidayHolder); } catch (RuntimeException e) { holidayHolder.removeAllViews(); } }
    }

    void setSubStatus(String text, boolean more) {
        if (subStatus == null) return;
        subStatus.setText(text);
        subStatus.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        if (subMore != null) subMore.setVisibility(more ? View.VISIBLE : View.GONE);
    }

    void openDiscoverSub(String title, final ListSource src, Runnable online) {
        if (browseRoot == null) return;
        if (discoverSub != null) browseRoot.removeView(discoverSub);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Ui.BG);
        overlay.setClickable(true);
        LinearLayout col = Ui.column(this);
        LinearLayout top = Ui.row(this);
        top.setPadding(dp(8), dp(2), dp(16), 0);
        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Ui.TEXT, "Back");
        back.setOnClickListener(v -> closeDiscoverSub());
        top.addView(back);
        TextView tt = Ui.title(this, title, 18);
        tt.setPadding(dp(6), 0, 0, 0);
        Ui.ellipsize(tt, 1);
        top.addView(tt, new LinearLayout.LayoutParams(0, -2, 1));
        final TextView count = Ui.caption(this, "");
        top.addView(count);
        col.addView(top);
        subStatus = Ui.caption(this, "");
        subStatus.setPadding(dp(20), 0, dp(20), dp(4));
        subStatus.setVisibility(View.GONE);
        col.addView(subStatus);
        final ListView lv = new ListView(this);
        lv.setDivider(null);
        lv.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        lv.setClipToPadding(false);
        lv.setPadding(dp(12), dp(4), dp(12), dp(16));
        subMore = Ui.button(this, "Load more from TheMealDB", R.drawable.ic_cloud, false);
        subMore.setVisibility(View.GONE);
        LinearLayout fw = Ui.column(this);
        fw.setPadding(dp(4), dp(8), dp(4), dp(8));
        fw.addView(subMore, new LinearLayout.LayoutParams(-1, dp(48)));
        lv.addFooterView(fw, null, false);
        final ResultAdapter ad = new ResultAdapter(false);
        ad.plain = true;
        ad.emptyTitle = "Nothing here yet";
        ad.emptyText = repo.isOnline() ? "Try another cuisine or category." : "Connect to the internet to load recipes from TheMealDB.";
        lv.setAdapter(ad);
        lv.setOnItemClickListener((p, v, pos, id) -> {
            Object o = ad.getItem(pos - lv.getHeaderViewsCount());
            if (o instanceof Matcher.Result) openDetail(((Matcher.Result) o).recipe.id);
        });
        col.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1));
        overlay.addView(col, new FrameLayout.LayoutParams(-1, -1));
        browseRoot.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        discoverSub = overlay;
        subAdapter = ad;
        subRefresh = () -> {
            subHidden = 0;
            List<Matcher.Result> l = src.get();
            ad.set(l);
            String ct = l.size() == 1 ? "1 recipe" : l.size() + " recipes";
            if (subHidden > 0) ct += " \u00b7 " + subHidden + " hidden";
            count.setText(ct);
        };
        subRefresh.run();
        overlay.setTranslationX(dp(60));
        overlay.setAlpha(0f);
        overlay.animate().translationX(0).alpha(1f).setDuration(200).start();
        if (online != null) online.run();
    }

    void closeDiscoverSub() {
        final View o = discoverSub;
        discoverSub = null;
        subAdapter = null;
        subRefresh = null;
        subStatus = null;
        subMore = null;
        if (o != null) {
            o.animate().translationX(dp(60)).alpha(0f).setDuration(160).withEndAction(() -> {
                if (o.getParent() != null) ((ViewGroup) o.getParent()).removeView(o);
            }).start();
        }
        refreshDiscover();
    }

    void randomRecipe() {
        if (repo.onlineEnabled() && repo.isOnline()) {
            toast("Picking a random recipe\u2026");
            repo.randomOnline((r, err) -> {
                if (r != null) openDetail(r.id); else randomLocal(true);
            });
        } else {
            randomLocal(repo.onlineEnabled());
        }
    }

    void randomLocal(boolean say) {
        List<Recipe> all = repo.all();
        if (all.isEmpty()) return;
        Set<String> seen = store.seenIds();
        List<Recipe> pool = new ArrayList<Recipe>();
        for (Recipe r : all) if (!seen.contains(r.id)) pool.add(r);
        if (pool.isEmpty()) pool = all;
        if (say) toast("Offline \u2013 picked from built-in & saved recipes");
        openDetail(pool.get(new Random().nextInt(pool.size())).id);
    }

    // ===================== detail =====================
    View buildDetail(final Recipe r, boolean withBack) {
        if (detailServings <= 0) detailServings = store.defaultServings();
        matcher = newMatcher();
        final Matcher.Result res = matcher.match(r);
        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(dp(2), 0, dp(2), dp(8));
        detailScroll = sv;
        LinearLayout col = Ui.column(this);
        sv.addView(col);

        if (withBack) {
            LinearLayout top = Ui.row(this);
            ImageView back = Ui.iconButton(this, R.drawable.ic_back, Ui.TEXT, "Back");
            back.setOnClickListener(v -> closeDetail());
            top.addView(back);
            TextView bt = Ui.text(this, tab == TAB_DISCOVER ? "Back" : "Back to list", 15, Ui.TEXT2, Ui.MEDIUM);
            bt.setPadding(dp(4), dp(10), dp(12), dp(10));
            bt.setOnClickListener(v -> closeDetail());
            top.addView(bt);
            col.addView(top, Ui.lpMargins(this, -1, -2, 0, 2, 0, 4));
        }

        LinearLayout card = Ui.card(this);
        card.setPadding(0, 0, 0, dp(20));
        card.setClipToOutline(true);
        FrameLayout header = new FrameLayout(this);
        final android.graphics.Bitmap userPhoto = V16.loadPhoto(this, r.id);
        View credit = null;
        final boolean v22photo = userPhoto == null && r.image.isEmpty() && V22.photo(this, r.id) != null;
        int headerH = dp(r.image.isEmpty() && userPhoto == null && !v22photo ? 130 : 220);
        int cc = Ui.categoryColor(r.category);
        GradientDrawable band = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{cc, blend(cc, 0xFF000000, 0.25f)});
        float rad = dp(16) * Ui.RAD;
        band.setCornerRadii(new float[]{rad, rad, rad, rad, 0, 0, 0, 0});
        header.setBackground(band);
        if (userPhoto != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(userPhoto);
            iv.setContentDescription("Your photo of " + r.title);
            header.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else if (!r.image.isEmpty()) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            header.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            ImageLoader.get(this).load(iv, r.image, false, store.prefs.getBoolean("online", true));
        } else {
            if (v22photo) credit = V22.headerPhoto(this, header, r); // v2.2: licensed photo the user chose
            else {
                header.setBackground(Ui.rounded(Ui.NEUTRAL, rad));
                TextView noPhoto = Ui.text(this, "No photo", 14, Ui.TEXT, Ui.REGULAR);
                noPhoto.setGravity(Gravity.CENTER); header.addView(noPhoto, new FrameLayout.LayoutParams(-1, -1));
            }
            TextView cat = Ui.text(this, (r.category + " \u00b7 " + r.cuisine).toUpperCase(Locale.US), 12, Color.WHITE, Ui.MEDIUM);
            cat.setBackground(Ui.rounded(0x8C000000, dp(10)));
            cat.setPadding(dp(8), dp(3), dp(8), dp(3));
            cat.setLetterSpacing(0.1f);
            FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.LEFT);
            cl.leftMargin = dp(20);
            cl.bottomMargin = dp(14);
            header.addView(cat, cl);
        }
        final ImageView heart = new ImageView(this);
        int hp = dp(10);
        heart.setPadding(hp, hp, hp, hp);
        heart.setBackground(Ui.rippleRound(Ui.CARD, dp(22)));
        heart.setElevation(dp(3));
        setHeart(heart, store.isFavorite(r.id));
        heart.setContentDescription("Favorite");
        heart.setOnClickListener(v -> {
            boolean on = store.toggleFavorite(r.id);
            setHeart(heart, on);
            heart.setScaleX(0.7f);
            heart.setScaleY(0.7f);
            heart.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
            toast(on ? "Saved to favorites" : "Removed from favorites");
            if (adapter != null) {
                if (adapter.fav) refreshResults(); else adapter.notifyDataSetChanged();
            }
        });
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.RIGHT);
        hl.setMargins(0, dp(12), dp(12), 0);
        header.addView(heart, hl);
        card.addView(header, new LinearLayout.LayoutParams(-1, headerH));
        if (credit != null) card.addView(credit, new LinearLayout.LayoutParams(-1, -2));
        card.addView(V22.sourcesRow(this, r), new LinearLayout.LayoutParams(-1, -2));

        LinearLayout body = Ui.column(this);
        body.setPadding(dp(20), dp(16), dp(20), 0);
        body.addView(Ui.title(this, r.title, 24));
        String th18 = repo.titleHint(r.id);
        if (th18 != null) { TextView hv = Ui.caption(this, "Similar title to another recipe \u2013 this one: " + th18); hv.setPadding(0, dp(4), 0, 0); body.addView(hv); }

        FlowLayout meta = new FlowLayout(this, dp(8), dp(8));
        meta.setPadding(0, dp(10), 0, 0);
        meta.addView(metaChip(Ui.timeText(r.totalMinutes()), R.drawable.ic_timer));
        if (r.prepMin >= 0 && r.cookMin >= 0) meta.addView(metaChip("Prep " + r.prepMin + " \u00b7 Cook " + r.cookMin + " min", 0));
        if (!r.cuisine.isEmpty()) meta.addView(metaChip(r.cuisine, 0));
        for (String mt : r.meals) {
            TextView c = metaChip(mt, R.drawable.ic_restaurant);
            c.setContentDescription("Meal type " + mt);
            meta.addView(c);
        }
        for (String tg : r.tags) {
            TextView c = metaChip(Ui.dietLabel(tg) + (r.tagsInferred ? "?" : ""), R.drawable.ic_leaf);
            c.setBackground(Ui.rounded(Ui.GREEN_TINT, dp(14)));
            c.setTextColor(Ui.GREEN_DARK);
            meta.addView(c);
        }
        body.addView(meta);
        View rating = Extras.ratingRow(this, store, r.id, () -> { if (adapter != null) adapter.notifyDataSetChanged(); });
        rating.setPadding(0, dp(10), 0, 0);
        body.addView(rating);
        View alg = Extras.allergenBox(this, store, r);
        if (alg != null) body.addView(alg, Ui.lpMargins(this, -1, -2, 0, 10, 0, 0));
        body.addView(V20.detailExtras(this, r));
        if (r.tagsInferred) {
            TextView note = Ui.caption(this, "Diet tags marked ? are guessed from TheMealDB's ingredient list and may be wrong. Check labels if it matters.");
            note.setPadding(0, dp(8), 0, 0);
            body.addView(note);
        }

        LinearLayout mbox = Ui.column(this);
        mbox.setBackground(Ui.rounded(res.missingCount() == 0 ? Ui.GREEN_TINT : Ui.AMBER_TINT, dp(14)));
        mbox.setPadding(dp(16), dp(14), dp(16), dp(14));
        String mt = res.missingCount() == 0 ? "You have everything you need" : "You have " + res.have + " of " + res.total + " ingredients";
        TextView mtv = Ui.title(this, mt, 16);
        mtv.setTextColor(res.missingCount() == 0 ? Ui.GREEN_DARK : Ui.AMBER_TEXT);
        mbox.addView(mtv);
        if (!res.substituted.isEmpty() || !res.swappable.isEmpty()) {
            String note = !res.substituted.isEmpty()
                    ? "Counting " + res.substituted.size() + (res.substituted.size() == 1 ? " substitute" : " substitutes") + " you have."
                    : "";
            if (!res.swappable.isEmpty()) note += (note.isEmpty() ? "" : " ") + res.swappable.size()
                    + " missing " + (res.swappable.size() == 1 ? "item has" : "items have") + " a substitute in your pantry.";
            TextView sn0 = Ui.caption(this, note);
            sn0.setPadding(0, dp(2), 0, 0);
            mbox.addView(sn0);
        }
        MatchBar bar = new MatchBar(this);
        bar.set(res.have, res.total);
        mbox.addView(bar, Ui.lpMargins(this, -1, 6, 0, 10, 0, 12));
        LinearLayout actions = Ui.row(this);
        TextView cook = Ui.button(this, "Cook mode", R.drawable.ic_play, true);
        cook.setOnClickListener(v -> {
            Intent i = new Intent(this, CookActivity.class);
            i.putExtra("id", r.id);
            i.putExtra("servings", detailServings);
            startActivity(i);
        });
        actions.addView(cook, new LinearLayout.LayoutParams(-2, dp(48)));
        actions.addView(Ui.space(this, 10, 1));
        TextView plan = Ui.button(this, "Add to plan", R.drawable.ic_calendar, false);
        plan.setOnClickListener(v -> Extras.addToPlanDialog(this, store, r, detailServings, () -> { if (tab == TAB_SHOPPING) refreshShopping(); else if (tab == TAB_PLAN) refreshPlan(); }));
        actions.addView(plan, new LinearLayout.LayoutParams(-2, dp(48)));
        actions.addView(Ui.space(this, 10, 1));
        TextView cooked = Ui.button(this, "Cooked this", R.drawable.ic_check, false);
        cooked.setOnClickListener(v -> V16.cookedDialog(this, r, detailServings, () -> { reopenDetailKeepScroll(r.id); refreshPantryViews(); }));
        actions.addView(cooked, new LinearLayout.LayoutParams(-2, dp(48)));
        if (res.missingCount() > 0) {
            actions.addView(Ui.space(this, 10, 1));
            final int n = res.missingCount();
            TextView add = Ui.button(this, "Add " + n + " missing to list", R.drawable.ic_cart, false);
            add.setOnClickListener(v -> {
                double scale = (double) detailServings / Math.max(1, r.servings);
                int added = 0;
                for (Ingredient i : res.missing) {
                    double amt = Double.isNaN(i.amount) ? Double.NaN : i.amount * scale;
                    String unit = Double.isNaN(i.amount) ? "" : i.unit;
                    if (store.addShopping(i.name.split(",")[0], amt, unit, r.title)) added++;
                }
                styleNav();
                toast(added == 0 ? "Already on your shopping list" : "Added " + added + " item" + (added == 1 ? "" : "s") + " to your shopping list");
            });
            actions.addView(add, new LinearLayout.LayoutParams(-2, dp(48)));
        }
        HorizontalScrollView ahs = new HorizontalScrollView(this);
        ahs.setHorizontalScrollBarEnabled(false);
        ahs.setClipToPadding(false);
        ahs.addView(actions);
        mbox.addView(ahs);
        TextView cn = V16.cookedNote(this, r.id);
        if (cn != null) mbox.addView(cn, Ui.lpMargins(this, -1, -2, 0, 8, 0, 0));
        body.addView(mbox, Ui.lpMargins(this, -1, -2, 0, 16, 0, 0));

        LinearLayout sr = Ui.row(this);
        sr.setPadding(0, dp(20), 0, dp(4));
        sr.addView(Ui.title(this, "Ingredients", 18), new LinearLayout.LayoutParams(0, -2, 1));
        TextView sl = Ui.caption(this, "Servings");
        sl.setPadding(0, 0, dp(8), 0);
        sr.addView(sl);
        ImageView minus = roundStep(R.drawable.ic_remove, "Fewer servings");
        final TextView servingsText = Ui.title(this, String.valueOf(detailServings), 18);
        servingsText.setGravity(Gravity.CENTER);
        servingsText.setMinWidth(dp(40));
        ImageView plus = roundStep(R.drawable.ic_add, "More servings");
        sr.addView(minus);
        sr.addView(servingsText);
        sr.addView(plus);
        body.addView(sr);
        if (!r.servingsKnown) {
            TextView sn = Ui.caption(this, "TheMealDB doesn't list servings, so quantities assume the recipe serves " + r.servings + ".");
            sn.setPadding(0, 0, 0, dp(4));
            body.addView(sn);
        } else if (r.servings != detailServings) {
            body.addView(Ui.caption(this, "Quantities scaled from the original " + r.servings + " servings."));
        }
        // batch cooking: x1 / x2 / x3 of the original servings
        LinearLayout batch = Ui.row(this);
        batch.setPadding(0, dp(4), 0, dp(4));
        TextView bl = Ui.caption(this, "Batch");
        bl.setPadding(0, 0, dp(8), 0);
        batch.addView(bl);
        final List<TextView> batchChips = new ArrayList<TextView>();
        for (int k = 1; k <= 3; k++) {
            TextView bc = Ui.chip(this, "×" + k, false);
            bc.setContentDescription("Batch times " + k + ", " + Math.min(24, r.servings * k) + " servings");
            batch.addView(bc, Ui.lpMargins(this, -2, dp(48), 0, 0, 6, 0));
            batchChips.add(bc);
        }
        TextView bplan = smallAction("Add batch to plan", R.drawable.ic_calendar);
        bplan.setOnClickListener(v -> Extras.addToPlanDialog(this, store, r, detailServings, () -> { if (tab == TAB_SHOPPING) refreshShopping(); else if (tab == TAB_PLAN) refreshPlan(); }));
        HorizontalScrollView bhs = new HorizontalScrollView(this);
        bhs.setHorizontalScrollBarEnabled(false);
        batch.addView(bplan, Ui.lpMargins(this, -2, dp(48), 6, 0, 0, 0));
        bhs.addView(batch);
        body.addView(bhs);
        body.addView(V17.unitToggle(this, () -> reopenDetailKeepScroll(r.id)));
        String mtags = V17.methodsText(r);
        if (!mtags.isEmpty()) {
            TextView methodsCaption17 = Ui.caption(this, "Cooking method: " + mtags);
            methodsCaption17.setPadding(0, dp(2), 0, dp(4));
            body.addView(methodsCaption17);
        }
        final LinearLayout ingBox = Ui.column(this);
        body.addView(ingBox);
        final LinearLayout estBox = Ui.column(this);
        detailEstimates = estBox;
        detailEstimatesRecipe = r;
        final Runnable renderIng = () -> {
            servingsText.setText(String.valueOf(detailServings));
            for (int k = 0; k < batchChips.size(); k++) Ui.styleChip(this, batchChips.get(k), detailServings == Math.min(24, r.servings * (k + 1)));
            refreshDetailEstimates();
            ingBox.removeAllViews();
            double scale = (double) detailServings / Math.max(1, r.servings);
            detailChecks.clear();
            for (Amounts.Check c : Amounts.check(r, scale, store.qtyMap())) detailChecks.put(c.ingredient, c);
            for (Ingredient i : r.ingredients) ingBox.addView(ingredientRow(i, scale, res));
        };
        renderIng.run();
        minus.setOnClickListener(v -> {
            if (detailServings > 1) { detailServings--; store.setDefaultServings(detailServings); renderIng.run(); }
        });
        plus.setOnClickListener(v -> {
            if (detailServings < 24) { detailServings++; store.setDefaultServings(detailServings); renderIng.run(); }
        });
        for (int k = 0; k < batchChips.size(); k++) {
            final int mult = k + 1;
            batchChips.get(k).setOnClickListener(v -> { detailServings = Math.min(24, r.servings * mult); renderIng.run(); });
        }
        body.addView(estBox, Ui.lpMargins(this, -1, -2, 0, 16, 0, 0));
        refreshDetailEstimates();

        TextView mh = Ui.title(this, "Method", 18);
        mh.setPadding(0, dp(22), 0, dp(6));
        body.addView(mh);
        for (int i = 0; i < r.steps.size(); i++) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.TOP);
            row.setPadding(0, dp(8), 0, dp(8));
            TextView num = Ui.text(this, String.valueOf(i + 1), 14, Ui.GREEN_DARK, Ui.MEDIUM);
            num.setGravity(Gravity.CENTER);
            num.setBackground(Ui.rounded(Ui.GREEN_TINT, dp(14)));
            row.addView(num, new LinearLayout.LayoutParams(dp(28), dp(28)));
            TextView stt = Ui.body(this, V17.step(this, r.steps.get(i)));
            stt.setPadding(dp(14), dp(3), 0, 0);
            row.addView(stt, new LinearLayout.LayoutParams(0, -2, 1));
            body.addView(row);
        }

        TextView src;
        if (r.isOnline()) {
            src = Ui.caption(this, "Recipe and photo from TheMealDB (themealdb.com). Tap to view the original page.");
            final String url = "https://www.themealdb.com/meal/" + r.id.substring(2);
            src.setOnClickListener(v -> openUrl(url));
            src.setTextColor(Ui.GREEN_DARK);
        } else {
            src = Ui.caption(this, r.id.startsWith("u:") ? "Your recipe (imported in the web app) \u00b7 works offline"
                    : "My Pantry Recipes original recipe \u00b7 works offline");
        }
        body.addView(V16.photoCard(this, r, () -> reopenDetailKeepScroll(r.id)), Ui.lpMargins(this, -1, -2, 0, 18, 0, 0));
        src.setPadding(0, dp(18), 0, 0);
        body.addView(src);
        card.addView(body);
        col.addView(card, Ui.lpMargins(this, -1, -2, 0, withBack ? 0 : 6, 0, 12));
        return sv;
    }

    LinearLayout detailEstimates;
    Recipe detailEstimatesRecipe;

    void refreshDetailEstimates() {
        if (detailEstimates == null || detailEstimatesRecipe == null) return;
        detailEstimates.removeAllViews();
        detailEstimates.addView(V16.estimatesCard(this, detailEstimatesRecipe, detailServings));
    }

    /** Rebuilds the open recipe page and keeps its scroll position. */
    void reopenDetailKeepScroll(String id) { reopenDetail(id); }

    void pantryChangedExternally() {
        refreshPantryViews();
        refreshResults();
        refreshDiscover();
    }

    /** "Buy these next" pick: the recipes it unlocks. */
    void openBuyNext(final org.pantryrecipes.app.core.BuyNext.Pick p) { openBuyNext(p.name, p.unlocks); }

    void openBuyNext(final String name, List<String> unlocks) {
        final List<String> ids = new ArrayList<String>(unlocks);
        openDiscoverSub("Buy " + name + " · " + V16.plural(ids.size(), "recipe", "recipes"), new ListSource() {
            @Override public List<Matcher.Result> get() {
                Matcher m = newMatcher();
                List<Matcher.Result> out = new ArrayList<Matcher.Result>();
                for (String id : ids) { Recipe x = repo.byId(id); if (x != null) out.add(m.match(x)); }
                return out;
            }
        }, null);
    }

    ImageView roundStep(int icon, String desc) {
        ImageView b = Ui.iconButton(this, icon, Ui.GREEN_DARK, desc);
        b.setBackground(Ui.rippleRound(Ui.GREEN_TINT, dp(19)));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48))); // v2.0 a11y: 48dp target, 38dp circle drawn
        b.setBackground(new android.graphics.drawable.InsetDrawable(Ui.rippleRound(Ui.GREEN_TINT, dp(19)), dp(5)));
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        return b;
    }

    void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("No browser available");
        }
    }

    static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    void setHeart(ImageView iv, boolean on) {
        iv.setImageResource(on ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
        iv.setColorFilter(on ? Ui.HEART : Ui.TEXT2);
    }

    TextView metaChip(String text, int icon) {
        TextView t = Ui.text(this, text, 13, Ui.TEXT2, Ui.MEDIUM);
        t.setPadding(dp(10), dp(5), dp(12), dp(5));
        t.setBackground(Ui.rounded(Ui.NEUTRAL, dp(14)));
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (icon != 0) {
            leftIcon(t, icon, Ui.GREEN, 14);
            t.setCompoundDrawablePadding(dp(5));
        }
        return t;
    }

    final java.util.IdentityHashMap<Ingredient, Amounts.Check> detailChecks = new java.util.IdentityHashMap<Ingredient, Amounts.Check>();

    View ingredientRow(final Ingredient i, final double scale, final Matcher.Result res) {
        boolean missing = res.missing.contains(i);
        boolean subbed = res.substituted.contains(i);
        boolean have = !missing && !subbed && matcher.has(i.key);
        boolean staple = have && matcher.isStaple(i.key);
        LinearLayout wrap = Ui.column(this);
        LinearLayout row = Ui.row(this);
        wrap.setPadding(dp(10), dp(9), dp(10), dp(9));
        if (missing) wrap.setBackground(Ui.rounded(Ui.AMBER_TINT, dp(10)));
        else if (subbed) wrap.setBackground(Ui.rounded(Ui.INFO_TINT, dp(10)));
        ImageView mark = new ImageView(this);
        if (missing) {
            mark.setImageResource(R.drawable.ic_cart);
            mark.setColorFilter(Ui.AMBER);
        } else if (subbed) {
            mark.setImageResource(R.drawable.ic_swap);
            mark.setColorFilter(Ui.INFO);
        } else if (!have) {
            mark.setImageResource(R.drawable.ic_add);
            mark.setColorFilter(Ui.TEXT3);
        } else {
            mark.setImageResource(R.drawable.ic_check);
            mark.setColorFilter(Ui.GREEN);
        }
        row.addView(mark, new LinearLayout.LayoutParams(dp(20), dp(20)));
        String q = V17.qty(this, i, scale);
        TextView qty = Ui.text(this, q, 15, Ui.TEXT, Ui.MEDIUM);
        qty.setPadding(dp(12), 0, dp(8), 0);
        qty.setMinWidth(dp(100));
        qty.setMaxWidth(dp(170));
        row.addView(qty);
        TextView name = Ui.text(this, V20.loc(this, i.name, false), 15, missing ? Ui.AMBER_TEXT : Ui.TEXT, Ui.REGULAR);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        String tag = missing ? "missing" : subbed ? "swapped" : i.optional ? "optional" : staple ? "staple" : "";
        if (!tag.isEmpty()) row.addView(Ui.text(this, tag, 12, missing ? Ui.AMBER_TEXT : subbed ? Ui.INFO : Ui.TEXT2, Ui.MEDIUM));
        wrap.addView(row);
        Amounts.Check chk = have ? detailChecks.get(i) : null;
        if (chk != null) {
            boolean ok = Amounts.ENOUGH.equals(chk.status), sh = Amounts.SHORT.equals(chk.status);
            TextView ct = Ui.text(this, (ok ? "\u2713 " : sh ? "! " : "") + Amounts.label(chk), 13,
                    ok ? Ui.GREEN_DARK : sh ? Ui.AMBER_TEXT : Ui.TEXT2, Ui.MEDIUM);
            ct.setPadding(dp(32), dp(4), 0, 0);
            wrap.addView(ct);
        }
        if (!hideDislikes.isEmpty() || !hideAvoid.isEmpty()) {
            if (Allergens.ingredientWarn(i, hideDislikes, hideAvoid)) {
                TextView wt = Ui.text(this, "Matches your dislikes/allergies (by name)", 13, Ui.DANGER, Ui.MEDIUM);
                wt.setPadding(dp(32), dp(4), 0, 0);
                wrap.addView(wt);
            }
        }

        final Substitutions subs = matcher.substitutions();
        final Substitutions.Entry entry = subs == null ? null : subs.forKey(i.key);
        if (entry != null) {
            final String rid = res.recipe.id;
            List<Substitutions.Option> avail = new ArrayList<Substitutions.Option>();
            for (Substitutions.Option option : subs.availableFor(i.key, matcher)) if (swapCaution(option).isEmpty()) avail.add(option);
            LinearLayout hint = Ui.row(this);
            hint.setPadding(dp(32), dp(6), 0, 0);
            TextView ht;
            if (!avail.isEmpty()) {
                Substitutions.Option o = avail.get(0);
                ht = Ui.text(this, (subbed ? "Using " : "You could use ") + o.usesLabel(matcher) + ": " + o.text, 13,
                        subbed ? Ui.INFO : Ui.GREEN_DARK, Ui.REGULAR);
                String det = V17.swapDetail(o);
                if (!det.isEmpty()) ht.setText(ht.getText() + "\n" + det);
                hint.addView(ht, new LinearLayout.LayoutParams(0, -2, 1));
                TextView act = Ui.text(this, subbed ? "Undo" : "Use substitute", 13, Ui.GREEN_DARK, Ui.MEDIUM);
                act.setMinHeight(dp(48));
                act.setPadding(dp(10), dp(6), dp(10), dp(6));
                act.setBackground(Ui.rippleRound(subbed ? Color.TRANSPARENT : Ui.GREEN_TINT, dp(14)));
                final boolean on = !subbed;
                act.setOnClickListener(v -> {
                    store.setAcceptedSub(i.key, on);
                    toast(on ? "Counting the substitute as a match" : "Back to missing");
                    reopenDetail(rid);
                });
                hint.addView(act);
            } else {
                ht = Ui.text(this, "Substitutions for " + i.name, 13, Ui.GREEN_DARK, Ui.MEDIUM);
                ht.setPadding(0, dp(2), dp(8), dp(2));
                hint.addView(ht);
            }
            ht.setMinHeight(dp(48));
            ht.setOnClickListener(v -> showSubsDialog(entry, i, scale));
            wrap.addView(hint);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(2);
        wrap.setLayoutParams(lp);
        return wrap;
    }

    String swapCaution(Substitutions.Option o) {
        return org.pantryrecipes.app.core.SwapAdvice.caution(o, store.dislikes(), store.avoidAllergens(), store.prefs.getStringSet("diets", new HashSet<String>()));
    }
    void showSubsDialog(final Substitutions.Entry e) { showSubsDialog(e, null, 1); }
    void showSubsDialog(final Substitutions.Entry e, Ingredient selected, double scale) {
        StringBuilder sb = new StringBuilder();
        sb.append("Selected ingredient: ").append(selected == null ? e.name : Quantity.format(selected.amount, selected.unit, scale) + " " + selected.name);
        sb.append("\nCheck ingredient labels, cooking times and your dietary plan. Pantry availability is not a dietary assessment.\n\n");
        for (Substitutions.Option o : e.options) {
            String caution = swapCaution(o);
            boolean ok = o.available(matcher) && caution.isEmpty();
            sb.append(ok ? "\u2713 " : "\u2022 ").append(o.text);
            if (ok) sb.append("  (you have ").append(o.usesLabel(matcher)).append(")");
            if (!caution.isEmpty()) sb.append("\nCaution: ").append(caution);
            if (selected != null) sb.append("\n").append(org.pantryrecipes.app.core.SwapAdvice.quantity(o, selected.amount * scale, selected.unit));
            String det = V17.swapDetail(o);
            if (!det.isEmpty()) sb.append("\n   ").append(det.replace("\n", "\n   "));
            sb.append("\n\n");
        }
        if (!e.note.isEmpty()) sb.append("Tip: ").append(e.note);
        dialog().setTitle("Instead of " + e.name.toLowerCase(Locale.US))
                .setMessage(sb.toString().trim())
                .setPositiveButton("OK", null)
                .setNeutralButton("All substitutions", (d, w) -> openSubs(e.name))
                .show();
    }

    // ===================== list adapter =====================
    final class ResultAdapter extends BaseAdapter {
        final boolean fav;
        boolean plain;
        String emptyTitle = "No recipes here yet", emptyText = "";
        List<Matcher.Result> items = new ArrayList<Matcher.Result>();
        final List<Object> rows = new ArrayList<Object>();

        ResultAdapter(boolean fav) { this.fav = fav; }

        void set(List<Matcher.Result> r) {
            items = r;
            rows.clear();
            rows.addAll(r);
            if (r.isEmpty()) rows.add("empty");
            if (!fav && !plain && !query.isEmpty() && repo.onlineEnabled()) rows.add("search");
            notifyDataSetChanged();
        }

        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int p) { return p >= 0 && p < rows.size() ? rows.get(p) : null; }
        @Override public long getItemId(int p) { return p; }
        @Override public int getViewTypeCount() { return 3; }
        @Override public int getItemViewType(int p) {
            Object o = rows.get(p);
            return o instanceof Matcher.Result ? 0 : "empty".equals(o) ? 1 : 2;
        }
        @Override public boolean isEnabled(int p) { return getItemViewType(p) != 1; }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            Object o = rows.get(pos);
            if ("empty".equals(o)) return emptyView();
            if ("search".equals(o)) {
                TextView t = Ui.button(MainActivity.this, "Search TheMealDB for \u201c" + query + "\u201d", R.drawable.ic_search, false);
                t.setDuplicateParentStateEnabled(true);
                LinearLayout w = Ui.column(MainActivity.this);
                w.setPadding(dp(4), dp(10), dp(4), dp(10));
                w.addView(t, new LinearLayout.LayoutParams(-1, dp(48)));
                return w;
            }
            Holder h;
            if (cv == null || !(cv.getTag() instanceof Holder)) {
                h = new Holder();
                cv = h.build();
                cv.setTag(h);
            } else {
                h = (Holder) cv.getTag();
            }
            h.bind((Matcher.Result) o);
            return cv;
        }

        View emptyView() {
            LinearLayout box = Ui.column(MainActivity.this);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(dp(24), dp(36), dp(24), dp(24));
            box.addView(Ui.icon(MainActivity.this, fav ? R.drawable.ic_heart_outline : R.drawable.ic_search, Ui.GREEN_TINT2, 56));
            String t, s;
            if (plain) {
                t = emptyTitle;
                s = emptyText;
            } else if (fav) {
                t = "No favorites yet";
                s = "Tap the heart on any recipe to keep it here for quick access.";
            } else if (!repo.loaded) {
                t = "Loading\u2026";
                s = "";
            } else {
                t = "No recipes match";
                s = mode == 1 ? "Nothing is fully cookable with these filters. Try \u201cMissing 1\u20132\u201d or add more to your pantry."
                        : "Try removing a filter or searching for something else.";
            }
            TextView tt = Ui.title(MainActivity.this, t, 17);
            tt.setPadding(0, dp(12), 0, dp(4));
            box.addView(tt);
            TextView ss = Ui.caption(MainActivity.this, s);
            ss.setGravity(Gravity.CENTER);
            box.addView(ss);
            return box;
        }
    }

    /** v1.8: one flat letter-tile shape per category colour, shared by every row through its ConstantState. */
    final java.util.Map<Integer, android.graphics.drawable.Drawable.ConstantState> thumbStates = new java.util.HashMap<Integer, android.graphics.drawable.Drawable.ConstantState>();

    android.graphics.drawable.Drawable thumbFor(int cc) {
        android.graphics.drawable.Drawable.ConstantState cs = thumbStates.get(cc);
        if (cs == null) {
            cs = Ui.rounded(cc, dp(14)).getConstantState();
            thumbStates.put(cc, cs);
        }
        return cs.newDrawable();
    }

    /** Row card background: the outlined surface inset inside the row (so the row needs no wrapper view). */
    android.graphics.drawable.Drawable rowCard(boolean sel) {
        int rad = dp(16), ih = dp(4), iv = dp(5);
        GradientDrawable shape = sel ? Ui.outlined(Ui.SELECTED, Ui.GREEN, dp(1.5f), rad) : Ui.outlined(Ui.CARD, Ui.LINE, dp(1), rad);
        return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Ui.RIPPLE),
                new android.graphics.drawable.InsetDrawable(shape, ih, iv, ih, iv),
                new android.graphics.drawable.InsetDrawable(Ui.rounded(Color.WHITE, rad), ih, iv, ih, iv));
    }

    android.graphics.drawable.Drawable.ConstantState cookBg;

    /**
     * v1.8 recipe row: a surface card with a flat letter tile, title (+ heart), meta, match bar and one filled
     * "Cook" button. 11 views, at most 3 levels deep, no bitmaps (photos only on a recipe's own page), no
     * shadow, and every drawable made when the row view is created, never while it is bound.
     */
    final class Holder {
        LinearLayout root;
        TextView letter, title, meta, missing, count, badge, meals, cook;
        ImageView photo;
        MatchBar bar;
        android.graphics.drawable.Drawable heartD;
        String id;

        View build() {
            root = Ui.row(MainActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            letter = Ui.text(MainActivity.this, "", 24, 0xA6000000, Ui.MEDIUM); // v2.0 a11y: dark ink on the light tile colours
            letter.setGravity(Gravity.CENTER);
            letter.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout tile = new FrameLayout(MainActivity.this);
            letter.setTextSize(11); letter.setTextColor(Ui.TEXT); letter.setText("No photo");
            tile.setBackground(Ui.rounded(Ui.NEUTRAL, dp(10)));
            tile.addView(letter, new FrameLayout.LayoutParams(-1, -1));
            photo = new ImageView(MainActivity.this); photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.addView(photo, new FrameLayout.LayoutParams(-1, -1));
            root.addView(tile, new LinearLayout.LayoutParams(dp(56), dp(56)));
            bgNormal = rowCard(false);
            bgSelected = rowCard(true);
            LinearLayout col = Ui.column(MainActivity.this);
            col.setPadding(dp(14), 0, dp(10), 0);
            title = Ui.title(MainActivity.this, "", 16);
            Ui.ellipsize(title, 2);
            heartD = getResources().getDrawable(R.drawable.ic_heart).mutate();
            heartD.setTint(Ui.HEART);
            heartD.setBounds(0, 0, dp(15), dp(15));
            title.setCompoundDrawablePadding(dp(6));
            col.addView(title);
            meals = Ui.text(MainActivity.this, "", 12, Ui.GREEN_DARK, Ui.MEDIUM);
            meals.setSingleLine(true);
            meals.setEllipsize(android.text.TextUtils.TruncateAt.END);
            meals.setPadding(0, dp(2), 0, 0);
            col.addView(meals);
            meta = Ui.caption(MainActivity.this, "");
            meta.setSingleLine(true);
            meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
            meta.setPadding(0, dp(2), 0, dp(8));
            col.addView(meta);
            LinearLayout br = Ui.row(MainActivity.this);
            bar = new MatchBar(MainActivity.this);
            br.addView(bar, new LinearLayout.LayoutParams(0, dp(6), 1));
            count = Ui.text(MainActivity.this, "", 12, Ui.TEXT2, Ui.MEDIUM);
            count.setPadding(dp(10), 0, 0, 0);
            br.addView(count);
            col.addView(br);
            missing = Ui.text(MainActivity.this, "", 13, Ui.AMBER_TEXT, Ui.REGULAR);
            missing.setPadding(0, dp(6), 0, 0);
            Ui.ellipsize(missing, 1);
            col.addView(missing);
            badge = Ui.text(MainActivity.this, "THEMEALDB", 10, Ui.TEXT3, Ui.MEDIUM);
            badge.setLetterSpacing(0.08f);
            badge.setPadding(0, dp(4), 0, 0);
            col.addView(badge);
            root.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
            cook = Ui.text(MainActivity.this, "Cook", 14, Ui.ON_FILL, Ui.MEDIUM);
            cook.setGravity(Gravity.CENTER);
            cook.setPadding(dp(16), 0, dp(16), 0);
            if (cookBg == null) cookBg = Ui.rounded(Ui.FILL, dp(18)).getConstantState();
            cook.setBackground(Ui.ripple(cookBg.newDrawable(), dp(18)));
            cook.setOnClickListener(v -> {
                if (id == null) return;
                Intent i = new Intent(MainActivity.this, CookActivity.class);
                i.putExtra("id", id);
                startActivity(i);
            });
            root.addView(cook, new LinearLayout.LayoutParams(-2, dp(48)));
            setBg(false);
            return root;
        }

        android.graphics.drawable.Drawable bgNormal, bgSelected;
        int boundSel = -1, boundColor, boundHeart = -1;

        void setBg(boolean sel) {
            if (boundSel == (sel ? 1 : 0)) return;
            boundSel = sel ? 1 : 0;
            root.setBackground(sel ? bgSelected : bgNormal);
            root.setPadding(dp(16), dp(16), dp(16), dp(16)); // the background's inset + the card's own padding
        }

        String boundIllo;

        void bind(Matcher.Result res) {
            Recipe r = res.recipe;
            id = r.id;
            setBg(wide && r.id.equals(selectedId));
            photo.animate().cancel(); photo.setAlpha(1f); photo.setTag(null); photo.setImageDrawable(null);
            ImageLoader.get(MainActivity.this).load(photo, r.image, true, store.prefs.getBoolean("online", true));
            title.setText(r.title);
            int fav = store.isFavorite(r.id) ? 1 : 0;
            if (fav != boundHeart) {
                boundHeart = fav;
                title.setCompoundDrawablesRelative(null, null, fav == 1 ? heartD : null, null);
            }
            cook.setContentDescription("Cook " + r.title);
            StringBuilder m = new StringBuilder();
            if (!r.cuisine.isEmpty()) m.append(r.cuisine).append(" \u00b7 ");
            m.append(r.totalMinutes() < 0 ? "time not listed" : Ui.timeText(r.totalMinutes()));
            if (r.servings > 0) m.append(" \u00b7 serves ").append(r.servings);
            if (V20.priceUnknown(MainActivity.this, r)) m.append(" \u00b7 price unknown");
            String th = repo.titleHint(r.id);
            if (th != null) m.insert(0, th + " \u00b7 "); // v1.8: tells near-identical titles apart
            if (r.tags.contains(Recipe.VEGAN)) m.append(" \u00b7 vegan");
            else if (r.tags.contains(Recipe.VEGETARIAN)) m.append(" \u00b7 vegetarian");
            if (r.tagsInferred && r.tags.contains(Recipe.VEGETARIAN)) m.append("?");
            int stars = store.stars(r.id);
            if (stars > 0 && Deco.on(MainActivity.this)) {
                // v1.8: the star in the theme's accent text colour (AA on cards)
                m.append(" \u00b7 ");
                int at = m.length();
                m.append("\u2605 ").append(stars);
                android.text.SpannableString sp = new android.text.SpannableString(m);
                sp.setSpan(new android.text.style.ForegroundColorSpan(Ui.AMBER_TEXT), at, m.length(), 0);
                meta.setText(sp);
            } else {
                if (stars > 0) m.append(" \u00b7 \u2605 ").append(stars);
                meta.setText(m.toString());
            }
            String g = r.meals.isEmpty() ? "" : Deco.meal(MainActivity.this, r.meals.iterator().next());
            meals.setText(g + mealText(r, 3));
            meals.setVisibility(r.meals.isEmpty() ? View.GONE : View.VISIBLE);
            bar.set(res.have, res.total);
            count.setText(res.have + " of " + res.total
                    + (res.total > 0 && Deco.on(MainActivity.this) ? " \u00b7 " + Math.round(100.0 * res.have / res.total) + "%" : ""));
            if (res.missingCount() == 0) {
                missing.setText(res.haveNonStaple > 0 ? "Ready to cook \u2013 you have everything" : "Only needs staples");
                missing.setTextColor(Ui.GREEN_DARK);
            } else {
                StringBuilder names = new StringBuilder("Missing: ");
                for (int k = 0; k < res.missing.size(); k++) {
                    if (k > 0) names.append(", ");
                    String nm = res.missing.get(k).name;
                    int cm = nm.indexOf(',');
                    names.append((cm >= 0 ? nm.substring(0, cm) : nm).toLowerCase(Locale.getDefault()));
                    if (names.length() > 120) break; // one ellipsized line is all that shows
                }
                missing.setText(names);
                missing.setTextColor(Ui.AMBER_TEXT);
            }
            badge.setVisibility(r.isOnline() ? View.VISIBLE : View.GONE);
        }
    }

    // ===================== shopping list =====================
    static final String[] UNIT_WORDS = {"g", "kg", "ml", "l", "oz", "lb", "lbs", "cup", "cups", "can", "cans", "tin", "tins",
            "jar", "jars", "tbsp", "tsp", "bunch", "bunches", "head", "heads", "clove", "cloves", "packet", "packets",
            "pack", "packs", "bag", "bags", "bottle", "bottles", "box", "boxes", "dozen", "loaf", "loaves", "piece", "pieces"};

    View buildShopping() {
        LinearLayout col = Ui.column(this);
        int pad = dp(16);

        LinearLayout addRow = Ui.row(this);
        final EditText in = new EditText(this);
        in.setHint("Add item, e.g. 2 cans tomatoes");
        in.setSingleLine(true);
        in.setTextSize(15);
        in.setTextColor(Ui.TEXT);
        in.setHintTextColor(Ui.TEXT3);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        in.setImeOptions(EditorInfo.IME_ACTION_DONE);
        in.setBackground(Ui.outlined(Ui.CARD, Ui.LINE, dp(1), dp(22)));
        in.setPadding(dp(16), 0, dp(16), 0);
        final Runnable add = () -> {
            String s = in.getText().toString().trim();
            if (s.isEmpty()) return;
            Quantity.Parsed p = Quantity.parse(s);
            String name = s, unit = "";
            double amt = Double.NaN;
            if (!Double.isNaN(p.amount)) {
                amt = p.amount;
                String rest = p.unit;
                String[] w = rest.split("\\s+", 2);
                if (w.length == 2) {
                    for (String u : UNIT_WORDS) {
                        if (u.equalsIgnoreCase(w[0].replace(".", ""))) { unit = singularUnit(u); rest = w[1]; break; }
                    }
                }
                if (rest.toLowerCase(Locale.US).startsWith("of ")) rest = rest.substring(3);
                name = rest.trim();
                if (name.isEmpty()) { name = s; amt = Double.NaN; unit = ""; }
            }
            store.addShopping(name, amt, unit, "");
            in.setText("");
            refreshShopping();
        };
        in.setOnEditorActionListener((v, a, e) -> { add.run(); return true; });
        addRow.addView(in, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView ab = Ui.button(this, "Add", R.drawable.ic_add, true);
        ab.setOnClickListener(v -> add.run());
        LinearLayout.LayoutParams abp = new LinearLayout.LayoutParams(-2, dp(48));
        abp.leftMargin = dp(10);
        addRow.addView(ab, abp);
        col.addView(addRow, Ui.lpMargins(this, -1, -2, 0, 0, 0, 10));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout acts = Ui.row(this);
        TextView toPantry = smallAction("Checked \u2192 pantry", R.drawable.ic_kitchen);
        toPantry.setOnClickListener(v -> {
            int n = 0;
            for (Store.ShopItem s : store.shopping()) {
                if (!s.checked) continue;
                store.addToPantry(s.name, false);
                store.deleteShopping(s.id);
                n++;
            }
            toast(n == 0 ? "Check off items first" : "Moved " + n + " item" + (n == 1 ? "" : "s") + " to your pantry");
            refreshShopping();
        });
        acts.addView(toPantry, chipLp());
        // v2.2: Select all / Select none
        TextView selAll = smallAction("Select all", R.drawable.ic_check);
        selAll.setContentDescription("Check every item on the shopping list");
        selAll.setOnClickListener(v -> { int n = store.setAllChecked(true); toast(n == 0 ? "Everything is already checked" : "Checked " + n + " item" + (n == 1 ? "" : "s")); refreshShopping(); });
        acts.addView(selAll, chipLp());
        TextView selNone = smallAction("Select none", R.drawable.ic_check);
        selNone.setContentDescription("Uncheck every item on the shopping list");
        selNone.setOnClickListener(v -> { int n = store.setAllChecked(false); toast(n == 0 ? "Nothing is checked" : "Unchecked " + n + " item" + (n == 1 ? "" : "s")); refreshShopping(); });
        acts.addView(selNone, chipLp());
        TextView clearChecked = smallAction("Clear checked", R.drawable.ic_check);
        clearChecked.setOnClickListener(v -> { int n = store.clearChecked(); if (n == 0) toast("Nothing checked yet"); refreshShopping(); });
        acts.addView(clearChecked, chipLp());
        TextView clearAll = smallAction("Clear all", R.drawable.ic_delete);
        clearAll.setOnClickListener(v -> dialog().setTitle("Clear shopping list?")
                .setMessage("Removes every item from the list.")
                .setPositiveButton("Clear", (d, w) -> { store.clearShopping(); refreshShopping(); })
                .setNegativeButton("Cancel", null).show());
        acts.addView(clearAll, chipLp());
        TextView shareL = smallAction("Share list", R.drawable.ic_share);
        shareL.setOnClickListener(v -> {
            if (store.shopping().isEmpty()) { toast("The list is empty"); return; }
            V16.share(this, "Shopping list", V16.shoppingText(store));
        });
        acts.addView(shareL, chipLp());
        hs.addView(acts);
        col.addView(hs, Ui.lpMargins(this, -1, -2, 0, 0, 0, 10));

        planHolder = null; // v1.8: the planner has its own tab, so a shopping toggle no longer rebuilds it
        shopList = Ui.column(this);
        col.addView(shopList);
        refreshShopping();

        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(pad, pad, pad, pad);
        if (wide) {
            FrameLayout center = new FrameLayout(this);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.min(dp(760), getResources().getDisplayMetrics().widthPixels - 2 * pad), -2, Gravity.CENTER_HORIZONTAL);
            center.addView(col, lp);
            sv.addView(center);
        } else {
            sv.addView(col);
        }
        return sv;
    }

    static String singularUnit(String u) {
        String l = u.toLowerCase(Locale.US);
        if (l.equals("lbs")) return "lb";
        if (l.equals("loaves")) return "loaf";
        if (l.equals("boxes")) return "box";
        if (l.equals("bunches")) return "bunch";
        if (l.length() > 3 && l.endsWith("s")) return l.substring(0, l.length() - 1);
        if (l.equals("cups") || l.equals("cans") || l.equals("tins") || l.equals("jars") || l.equals("bags")) return l.substring(0, l.length() - 1);
        return l;
    }

    TextView smallAction(String label, int icon) {
        TextView t = Ui.text(this, label, 14, Ui.GREEN_DARK, Ui.MEDIUM);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(12), 0, dp(14), 0);
        int r = dp(18);
        t.setBackground(Ui.ripple(Ui.outlined(Ui.CARD, Ui.GREEN_TINT2, dp(1), r), r));
        leftIcon(t, icon, Ui.GREEN_DARK, 16);
        return t;
    }

    LinearLayout planHolder;

    void refreshPlan() {
        if (planHolder == null) return;
        planHolder.removeAllViews();
        planHolder.addView(Extras.plannerCard(this, store, repo, this::refreshPlan), Ui.lpMargins(this, -1, -2, 0, 0, 0, 12));
        styleNav();
    }

    /** v1.8: the Plan tab (the weekly planner, previously at the top of Shopping). */
    View buildPlan() {
        int pad = dp(16);
        planHolder = Ui.column(this);
        TextView h = Ui.title(this, "Meal plan", 18);
        Deco.underline(this, h);
        planHolder.setPadding(0, 0, 0, 0);
        LinearLayout col = Ui.column(this);
        col.addView(h, Ui.lpMargins(this, -2, -2, 0, 0, 0, 10));
        col.addView(planHolder);
        refreshPlan();
        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        sv.setPadding(pad, dp(4), pad, pad);
        if (wide) {
            FrameLayout center = new FrameLayout(this);
            center.addView(col, new FrameLayout.LayoutParams(Math.min(dp(760), getResources().getDisplayMetrics().widthPixels - 2 * pad), -2, Gravity.CENTER_HORIZONTAL));
            sv.addView(center);
        } else sv.addView(col);
        return sv;
    }

    void openPlannedRecipe(String id) {
        showTab(TAB_RECIPES, false);
        openDetail(id);
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (V20.onResult(this, req, result == RESULT_OK && data != null ? data.getData() : null)) return;
        if (req == V16.REQ_SCAN) { if (result == RESULT_OK) Scan.onResult(this, data); return; }
        if (req == V16.REQ_PHOTO) {
            String id = V16.takePendingPhoto(this);
            if (result != RESULT_OK || data == null || data.getData() == null || id == null) return;
            if (V16.savePhoto(this, id, data.getData())) { toast("Photo saved"); if (detailOpen && id.equals(selectedId)) reopenDetail(id); }
            else toast("Couldn\u2019t read that picture");
            return;
        }
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (req == Extras.REQ_EXPORT) Extras.writeExport(this, store, repo, data.getData());
        else if (req == Extras.REQ_IMPORT) Extras.readImport(this, store, repo, data.getData());
    }

    void refreshShopping() {
        if (shopList == null) return;
        refreshPlan();
        shopList.removeAllViews();
        List<Store.ShopItem> items = store.shopping();
        styleNav();
        if (items.isEmpty()) {
            LinearLayout box = Ui.column(this);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(dp(24), dp(40), dp(24), dp(24));
            box.addView(Ui.icon(this, R.drawable.ic_cart, Ui.GREEN_TINT2, 64));
            TextView t = Ui.title(this, "Your shopping list is empty", 17);
            t.setPadding(0, dp(12), 0, dp(4));
            box.addView(t);
            TextView s = Ui.caption(this, "Open a recipe and tap \u201cAdd missing to list\u201d, or add items above. Quantities follow the servings you picked.");
            s.setGravity(Gravity.CENTER);
            box.addView(s);
            shopList.addView(box);
            return;
        }
        final boolean costs = V16.shopCost(this);
        final java.util.Map<String, org.pantryrecipes.app.core.Cost.Price> userP = V16.data(this).prices, defP = V16.priceDefaults(this);
        final java.util.Map<String, org.pantryrecipes.app.core.Nutrition.Food> foods = costs ? V16.foods(this) : null;
        List<Store.ShopItem> openItems = new ArrayList<Store.ShopItem>(), done = new ArrayList<Store.ShopItem>();
        for (Store.ShopItem s : items) (s.checked ? done : openItems).add(s);
        List<String> names = new ArrayList<String>();
        for (Store.ShopItem s : openItems) names.add(s.name);
        LinearLayout card = Ui.card(this);
        card.setPadding(dp(6), dp(6), dp(6), dp(6));
        boolean first = true;
        for (java.util.Map.Entry<String, List<Integer>> g : org.pantryrecipes.app.core.Aisles.group(names).entrySet()) {
            List<Store.ShopItem> gi = new ArrayList<Store.ShopItem>();
            for (int i : g.getValue()) gi.add(openItems.get(i));
            if (!first) card.addView(Ui.divider(this));
            first = false;
            addAisle(card, g.getKey(), gi, costs, userP, defP, foods);
        }
        if (!done.isEmpty()) {
            if (!first) card.addView(Ui.divider(this));
            addAisle(card, "In the basket", done, costs, userP, defP, foods);
        }
        String sum = openItems.size() + " to buy \u00b7 " + done.size() + " checked";
        if (costs) {
            List<Object[]> rows = new ArrayList<Object[]>();
            for (Store.ShopItem s : openItems) rows.add(new Object[]{s.name, s.amount, s.unit == null ? "" : s.unit});
            double[] t = org.pantryrecipes.app.core.Cost.shopping(rows, userP, defP, foods);
            if (t[1] > 0) sum += " \u00b7 about " + V16.money(this, t[0]) + " for " + (int) t[1] + " of " + (int) t[2] + " priced (estimate)";
            else if (t[2] > 0) sum += " \u00b7 no prices yet";
        }
        TextView summary = Ui.caption(this, sum);
        summary.setPadding(dp(4), 0, 0, dp(8));
        shopList.addView(summary);
        shopList.addView(card);
    }

    /** One collapsible aisle group of the shopping list (collapsed state persists). */
    void addAisle(LinearLayout card, final String aisle, List<Store.ShopItem> list, boolean costs,
                  java.util.Map<String, org.pantryrecipes.app.core.Cost.Price> userP, java.util.Map<String, org.pantryrecipes.app.core.Cost.Price> defP,
                  java.util.Map<String, org.pantryrecipes.app.core.Nutrition.Food> foods) {
        final String pk = "aisleCollapsed:" + aisle;
        final boolean collapsed = store.prefs.getBoolean(pk, false);
        LinearLayout h = Ui.row(this);
        h.setPadding(dp(14), dp(10), dp(8), dp(6));
        h.setBackground(Ui.ripple(Ui.rounded(Color.TRANSPARENT, dp(12)), dp(12)));
        TextView lab = Ui.sectionLabel(this, aisle + " (" + list.size() + ")");
        h.addView(lab, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView chev = Ui.icon(this, collapsed ? R.drawable.ic_next : R.drawable.ic_expand, Ui.TEXT3, 18);
        h.addView(chev);
        h.setContentDescription(aisle + ", " + list.size() + " items, " + (collapsed ? "collapsed. Tap to expand" : "expanded. Tap to collapse"));
        h.setOnClickListener(v -> { store.prefs.edit().putBoolean(pk, !collapsed).apply(); refreshShopping(); });
        card.addView(h);
        if (collapsed) return;
        for (final Store.ShopItem s : list) {
            LinearLayout row = Ui.row(this);
            row.setPadding(dp(4), dp(2), dp(2), dp(2));
            row.setBackground(Ui.ripple(Ui.rounded(Color.TRANSPARENT, dp(12)), dp(12)));
            final CheckBox cb = new CheckBox(this);
            cb.setChecked(s.checked);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
            row.addView(cb);
            LinearLayout tc = Ui.column(this);
            tc.setPadding(dp(4), dp(8), 0, dp(8));
            TextView name = Ui.text(this, s.name, 16, s.checked ? Ui.TEXT3 : Ui.TEXT, Ui.REGULAR);
            if (s.checked) name.setPaintFlags(name.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tc.addView(name);
            String q = Double.isNaN(s.amount) ? "" : V17.qty(this, s.name, s.amount, s.unit);
            String sub = q;
            if (s.recipe != null && !s.recipe.isEmpty()) sub += (sub.isEmpty() ? "" : " \u00b7 ") + "for " + s.recipe;
            if (costs && !s.checked) {
                double c = org.pantryrecipes.app.core.Cost.item(s.name, s.amount, s.unit == null ? "" : s.unit, userP, defP, foods);
                if (!Double.isNaN(c)) sub += (sub.isEmpty() ? "" : " \u00b7 ") + "~" + V16.money(this, c);
            }
            if (!sub.isEmpty()) {
                TextView st = Ui.caption(this, sub);
                Ui.ellipsize(st, 2);
                tc.addView(st);
            }
            row.addView(tc, new LinearLayout.LayoutParams(0, -2, 1));
            ImageView del = Ui.iconButton(this, R.drawable.ic_close, Ui.TEXT3, "Delete " + s.name);
            del.setOnClickListener(v -> { store.deleteShopping(s.id); refreshShopping(); });
            row.addView(del);
            cb.setOnCheckedChangeListener((b, checked) -> { store.setChecked(s.id, checked); shopList.post(this::refreshShopping); });
            row.setOnClickListener(v -> cb.toggle());
            card.addView(row);
        }
    }

    // ===================== about =====================
    void showCrash(final String crash) {
        ScrollView sv = new ScrollView(this);
        TextView t = Ui.text(this, crash, 12, Ui.TEXT, android.graphics.Typeface.MONOSPACE);
        t.setTextIsSelectable(true);
        t.setPadding(dp(20), dp(12), dp(20), dp(12));
        sv.addView(t);
        dialog().setTitle("Last crash report").setView(sv)
                .setPositiveButton("Copy", (d, w) -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("My Pantry Recipes crash report", crash));
                    toast("Error report copied. Nothing was sent anywhere.");
                })
                .setNeutralButton("Delete", (d, w) -> { PantryApp.clearCrash(this); toast("Crash report deleted"); })
                .setNegativeButton("Close", null).show();
    }

    void showAbout() {
        LinearLayout box = Ui.column(this);
        box.setPadding(dp(24), dp(12), dp(24), dp(4));
        TextView vt = Ui.title(this, "Version " + Extras.version(this) + " (build " + Extras.versionCode(this) + ")", 16);
        vt.setTextColor(Ui.GREEN_DARK);
        vt.setPadding(0, 0, 0, dp(8));
        box.addView(vt);
        // v1.8: crash safety net (the report stays on this device)
        final String crash = PantryApp.lastCrash(this);
        if (crash != null) {
            TextView cr = smallAction("The app crashed last time \u2013 view/copy error", R.drawable.ic_warning);
            cr.setTextColor(Ui.AMBER_TEXT);
            cr.setOnClickListener(v -> showCrash(crash));
            box.addView(cr, Ui.lpMargins(this, -2, dp(48), 0, 0, 0, 8));
        }
        String text = "My Pantry Recipes \u2013 find recipes from what you already have.\n\n"
                + "Free & open source (MIT License). No ads, no tracking, no accounts. Your pantry, favorites and shopping list stay on this device.\n\n"
                + "Built-in recipes, ingredient swaps, densities and the Instant Pot cheat sheet: original content written for this app (MIT License); they work offline. "
                + "Converted amounts and cook times are rounded estimates; a kitchen scale and your cooker's manual are more accurate.\n\n"
                + "Online recipes and photos come from TheMealDB (www.themealdb.com), a free crowd-sourced database. "
                + "The app uses TheMealDB's public test API key \u201c1\u201d, which TheMealDB offers for development and educational/personal use. "
                + "If you redistribute this app (e.g. in an app store), get a supporter API key from TheMealDB and enter it below. "
                + "Barcode lookups (only when you add an item by barcode) go to world.openfoodfacts.org. "
                + "The only internet connections go to themealdb.com, world.openfoodfacts.org and (only when you look up a barcode price) prices.openfoodfacts.org. Sync between your own devices uses only your local Wi-Fi.\n\n"
                + "Barcodes: this app has no built-in camera decoder. \u201cScan\u201d hands off to a separate barcode app you install "
                + "(through the standard ZXing SCAN intent; no ZXing code is bundled), or you type the number. "
                + "Product data from Open Food Facts (openfoodfacts.org), available under the Open Database License (ODbL).\n\n"
                + "Nutrition estimates use USDA FoodData Central (SR Legacy) averages, which are in the public domain; they are shown only when at least 70% "
                + "of a recipe's ingredients can be matched, and are rough estimates, not medical advice.\n\n"
                + "Costs are estimates from the prices you enter, plus (U.S. only) BLS average prices, September 2026 (public domain), shown as a regional average, and built-in estimates \u00d7 a regional factor \u2013 not live store prices. Open Prices data (Open Food Facts) is under the ODbL. Diet labels are a guide, not certification.\n\n"
                + "Diet tags on TheMealDB recipes (shown with ?) are guessed from ingredient names and can be wrong. "
                + "TheMealDB doesn't list servings, so those are assumed to serve 4. Always check labels for allergies.\n\n"
                + "Icons: Material Design icons by Google (Apache License 2.0).";
        TextView t = Ui.body(this, text);
        t.setTextSize(14);
        final AlertDialog[] self = new AlertDialog[1];
        TextView setH = Ui.sectionLabel(this, "Settings");
        setH.setPadding(0, dp(16), 0, dp(6));
        box.addView(setH);
        TextView substitutions = smallAction("Ingredient substitutions", R.drawable.ic_swap);
        substitutions.setOnClickListener(v -> { if (self[0] != null) self[0].dismiss(); openSubs(null); });
        box.addView(substitutions, Ui.lpMargins(this, -2, dp(48), 0, 0, 0, 0));
        TextView app = smallAction("Appearance\u2026", R.drawable.ic_leaf);
        app.setContentDescription("Appearance: light, dark and colour themes");
        app.setOnClickListener(v -> { if (self[0] != null) self[0].dismiss(); startActivity(new Intent(this, AppearanceActivity.class)); });
        box.addView(app, Ui.lpMargins(this, -2, dp(48), 0, 0, 0, 0));
        TextView cv = smallAction("Measurement units & conversions\u2026", R.drawable.ic_swap);
        cv.setOnClickListener(v -> { if (self[0] != null) self[0].dismiss(); V17.conversions(this, () -> { if (tab == TAB_SHOPPING) refreshShopping(); if (detailOpen && selectedId != null) reopenDetailKeepScroll(selectedId); }); });
        box.addView(cv, Ui.lpMargins(this, -2, dp(48), 0, 8, 0, 0));
        TextView ipc = smallAction("Instant Pot cheat sheet\u2026", R.drawable.ic_timer);
        ipc.setOnClickListener(v -> V17.cheatSheet(this));
        box.addView(ipc, Ui.lpMargins(this, -2, dp(48), 0, 8, 0, 0));
        TextView pr = smallAction("Prices & currency\u2026", R.drawable.ic_cart);
        pr.setOnClickListener(v -> { if (self[0] != null) self[0].dismiss(); V16.pricesDialog(this, null, () -> { refreshShopping(); refreshDetailEstimates(); }); });
        box.addView(pr, Ui.lpMargins(this, -2, dp(48), 0, 8, 0, 0));
        V20.settingsRows(this, box, () -> { if (self[0] != null) self[0].dismiss(); });
        LinearLayout ld = Ui.row(this);
        ld.setPadding(0, dp(8), 0, 0);
        final int[] days = {V16.leftoverDays(this)};
        final TextView ldt = Ui.body(this, "");
        final Runnable ldr = () -> ldt.setText("Leftovers keep for " + days[0] + " days");
        ldr.run();
        ld.addView(ldt, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView ldm = roundStep(R.drawable.ic_remove, "Fewer days");
        ldm.setOnClickListener(v -> { days[0] = org.pantryrecipes.app.core.Leftovers.clampDays(days[0] - 1); ldr.run(); });
        ld.addView(ldm);
        ld.addView(Ui.space(this, 8, 1));
        ImageView ldp = roundStep(R.drawable.ic_add, "More days");
        ldp.setOnClickListener(v -> { days[0] = org.pantryrecipes.app.core.Leftovers.clampDays(days[0] + 1); ldr.run(); });
        ld.addView(ldp);
        box.addView(ld);
        final CheckBox deduct = new CheckBox(this);
        deduct.setText("\u201cCooked this\u201d offers to use up pantry amounts");
        deduct.setTextColor(Ui.TEXT);
        deduct.setChecked(V16.deductPantry(this));
        deduct.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        box.addView(deduct);
        TextView prefsH = Ui.sectionLabel(this, "Your data");
        prefsH.setPadding(0, dp(16), 0, dp(6));
        box.addView(prefsH);
        int nd = store.dislikes().size() + store.avoidAllergens().size();
        TextView dis = smallAction("Dislikes & allergies" + (nd > 0 ? " (" + nd + ")" : "") + "\u2026", R.drawable.ic_warning);
        dis.setOnClickListener(v -> {
            if (self[0] != null) self[0].dismiss();
            Extras.dislikesDialog(this, store, () -> { refreshResults(); refreshDiscover(); });
        });
        box.addView(dis, Ui.lpMargins(this, -2, dp(48), 0, 0, 0, 8));
        LinearLayout bk = Ui.row(this);
        final CheckBox withPhotos = new CheckBox(this);
        TextView exp = smallAction("Export backup", R.drawable.ic_cloud);
        exp.setOnClickListener(v -> { Extras.exportPhotos = withPhotos.isChecked(); if (self[0] != null) self[0].dismiss(); Extras.startExport(this); });
        bk.addView(exp, new LinearLayout.LayoutParams(-2, dp(48)));
        bk.addView(Ui.space(this, 8, 1));
        TextView imp = smallAction("Import backup", R.drawable.ic_list);
        imp.setOnClickListener(v -> { if (self[0] != null) self[0].dismiss(); Extras.startImport(this); });
        bk.addView(imp, new LinearLayout.LayoutParams(-2, dp(48)));
        box.addView(bk);
        TextView bn = Ui.caption(this, "One JSON file with your pantry (use-by dates, amounts), favorites, shopping list, ratings and notes, meal plan, "
                + "dislikes, settings and themes. The same file works in the My Pantry Recipes web app.");
        bn.setPadding(0, dp(6), 0, 0);
        box.addView(bn);
        withPhotos.setText("Include your recipe photos in the backup (larger file)");
        withPhotos.setTextColor(Ui.TEXT);
        withPhotos.setChecked(false);
        withPhotos.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        box.addView(withPhotos);
        final CheckBox online = new CheckBox(this);
        online.setText("Find recipes online (TheMealDB)");
        online.setTextColor(Ui.TEXT);
        online.setChecked(repo.onlineEnabled());
        online.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        box.addView(online, Ui.lpMargins(this, -1, -2, 0, 12, 0, 0));
        final EditText key = new EditText(this);
        key.setHint("TheMealDB API key (default: 1)");
        key.setText("1".equals(repo.apiKey()) ? "" : repo.apiKey());
        key.setSingleLine(true);
        key.setTextSize(14);
        box.addView(key);
        box.addView(t);
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        self[0] = dialog().setTitle("Settings & About")
                .setView(sv)
                .setPositiveButton("Save", (d, w) -> {
                    String k = key.getText().toString().trim();
                    store.prefs.edit().putBoolean("online", online.isChecked())
                            .putInt("leftoverDays", days[0]).putBoolean("deductPantry", deduct.isChecked())
                            .putString("apiKey", k.isEmpty() ? "1" : k).apply();
                    repo.lastFetchSignature = "";
                    if (tab == TAB_RECIPES || tab == TAB_FAVORITES) refreshResults();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // ===================== autocomplete =====================
    static final class SuggestAdapter extends android.widget.ArrayAdapter<String> {
        SuggestAdapter(Activity a, List<String> items) {
            super(a, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(items));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            if (v instanceof TextView) {
                TextView t = (TextView) v;
                t.setTextColor(Ui.TEXT);
                t.setTextSize(15);
                t.setTypeface(Ui.REGULAR);
                int p = Ui.dp(getContext(), 16);
                t.setPadding(p, 0, p, 0);
            }
            return v;
        }
    }
}
