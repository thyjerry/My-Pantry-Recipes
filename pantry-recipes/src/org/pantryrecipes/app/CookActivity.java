package org.pantryrecipes.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.pantryrecipes.app.core.Durations;
import org.pantryrecipes.app.core.Ingredient;
import org.pantryrecipes.app.core.Recipe;

import java.util.Locale;

/** Distraction-free, screen-on step-by-step cooking view. */
public class CookActivity extends Activity implements Repo.Listener {
    @Override
    protected void attachBaseContext(android.content.Context base) { super.attachBaseContext(Ui.fontContext(base)); }


    Repo repo;
    Recipe recipe;
    int step;
    int servings;
    boolean showIngredients;
    TextView stepLabel, stepText, prevBtn, nextBtn;
    FlowLayout timerChips;
    LinearLayout timersBar;
    MatchBar progress;
    Ringtone ringtone;
    /** Running, paused and finished timers. Each keeps an end timestamp, so it survives rotation and restarts. */
    final java.util.List<Timer> timers = new java.util.ArrayList<Timer>();
    final android.os.Handler handler = new android.os.Handler();
    static final int MAX_TIMERS = 8;

    static final class Timer {
        String id, label;
        int step;
        long total, end, left;   // end = wall-clock ms when running; left = ms remaining when paused
        boolean paused, done;
        long remaining(long now) { return done ? 0 : paused ? left : Math.max(0, end - now); }
    }

    int dp(float v) { return Ui.dp(this, v); }

    @Override
    protected void onCreate(Bundle saved) {
        Ui.themeActivity(this);
        super.onCreate(saved);
        Ui.styleWindow(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        repo = Repo.get(this);
        servings = getIntent().getIntExtra("servings", Store.get(this).defaultServings());
        if (saved != null) {
            step = saved.getInt("step");
            showIngredients = saved.getBoolean("ing");
        }
        recipe = repo.byId(getIntent().getStringExtra("id"));
        if (recipe == null && repo.complete) { finish(); return; } // v1.8: unknown or missing id (e.g. a stale notification)
        if (recipe == null) {
            repo.addListener(this);
            TextView t = Ui.body(this, "Loading recipe\u2026");
            t.setGravity(Gravity.CENTER);
            setContentView(t);
        } else {
            build();
        }
    }

    @Override
    public void onRepoChanged() {
        if (recipe != null || !repo.loaded) return;
        recipe = repo.byId(getIntent().getStringExtra("id"));
        if (recipe == null) { if (repo.complete) finish(); return; }
        repo.removeListener(this);
        build();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("step", step);
        out.putBoolean("ing", showIngredients);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recipe != null) {
            TimerAlarm.foregroundRecipe = recipe.id;
            resumedAt = System.currentTimeMillis();
            // a timer that ended while we were away is shown as done right away (the notification already rang)
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    @Override
    protected void onPause() {
        TimerAlarm.foregroundRecipe = null;
        awaySince = System.currentTimeMillis();
        handler.removeCallbacks(tick); // while away the AlarmManager notification rings instead
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        repo.removeListener(this);
        handler.removeCallbacksAndMessages(null); // tick + the 30 s stopAlarm (no leaked activity)
        if (ringtone != null) ringtone.stop();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        if (recipe != null) build();
    }

    void build() {
        if (recipe.steps.isEmpty()) recipe.steps.add("No steps listed for this recipe.");
        if (step >= recipe.steps.size()) step = recipe.steps.size() - 1;
        Configuration cfg = getResources().getConfiguration();
        boolean side = cfg.screenWidthDp >= 720 || cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        LinearLayout top = Ui.row(this);
        top.setPadding(dp(8), dp(8), dp(12), dp(8));
        ImageView close = Ui.iconButton(this, R.drawable.ic_close, Ui.TEXT, "Close cook mode");
        close.setOnClickListener(v -> finish());
        top.addView(close);
        LinearLayout tc = Ui.column(this);
        tc.setPadding(dp(8), 0, dp(8), 0);
        TextView over = Ui.text(this, "COOK MODE \u00b7 SCREEN STAYS ON", 11, Ui.GREEN_DARK, Ui.MEDIUM);
        over.setLetterSpacing(0.08f);
        tc.addView(over);
        TextView title = Ui.title(this, recipe.title, 18);
        Ui.ellipsize(title, 1);
        tc.addView(title);
        top.addView(tc, new LinearLayout.LayoutParams(0, -2, 1));
        final TextView ingToggle = Ui.chip(this, "Ingredients", showIngredients);
        top.addView(ingToggle, new LinearLayout.LayoutParams(-2, dp(48)));
        root.addView(top);

        LinearLayout body = side ? Ui.row(this) : Ui.column(this);
        body.setGravity(Gravity.TOP);
        final View ingPanel = ingredientsPanel(side);
        LinearLayout main = Ui.column(this);
        main.setPadding(dp(28), dp(12), dp(28), dp(20));
        stepLabel = Ui.text(this, "", 16, Ui.GREEN_DARK, Ui.MEDIUM);
        main.addView(stepLabel);
        progress = new MatchBar(this);
        main.addView(progress, Ui.lpMargins(this, -1, 6, 0, 10, 0, 18));
        ScrollView sv = new ScrollView(this);
        stepText = Ui.text(this, "", side ? 28 : 26, Ui.TEXT, Ui.REGULAR);
        stepText.setLineSpacing(0, 1.25f);
        sv.addView(stepText);
        main.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        timerChips = new FlowLayout(this, dp(8), dp(8));
        main.addView(timerChips, Ui.lpMargins(this, -1, -2, 0, 12, 0, 0));
        timersBar = Ui.column(this);
        main.addView(timersBar, Ui.lpMargins(this, -1, -2, 0, 8, 0, 0));

        LinearLayout nav = Ui.row(this);
        nav.setPadding(0, dp(16), 0, 0);
        prevBtn = Ui.button(this, "Back", R.drawable.ic_prev, false);
        prevBtn.setTextSize(18);
        nextBtn = Ui.button(this, "Next", 0, true);
        nextBtn.setTextSize(18);
        nav.addView(prevBtn, new LinearLayout.LayoutParams(0, dp(60), 1));
        nav.addView(Ui.space(this, 14, 1));
        nav.addView(nextBtn, new LinearLayout.LayoutParams(0, dp(60), 1.4f));
        main.addView(nav);
        prevBtn.setOnClickListener(v -> go(step - 1));
        nextBtn.setOnClickListener(v -> {
            if (step >= recipe.steps.size() - 1) finish(); else go(step + 1);
        });

        if (side) {
            body.addView(ingPanel, new LinearLayout.LayoutParams(dp(300), -1));
            body.addView(main, new LinearLayout.LayoutParams(0, -1, 1));
        } else {
            body.addView(ingPanel, new LinearLayout.LayoutParams(-1, 0, 1));
            body.addView(main, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        ingPanel.setVisibility(showIngredients ? View.VISIBLE : View.GONE);
        final View mainF = main;
        ingToggle.setOnClickListener(v -> {
            showIngredients = !showIngredients;
            Ui.styleChip(this, ingToggle, showIngredients);
            ingPanel.setVisibility(showIngredients ? View.VISIBLE : View.GONE);
            if (!side) mainF.setVisibility(showIngredients ? View.GONE : View.VISIBLE);
        });
        if (!side && showIngredients) main.setVisibility(View.GONE);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        if (timers.isEmpty()) loadTimers();
        render(false);
        renderTimers();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    View ingredientsPanel(boolean side) {
        ScrollView sv = new ScrollView(this);
        LinearLayout col = Ui.column(this);
        col.setPadding(dp(20), dp(12), dp(20), dp(20));
        if (side) sv.setBackgroundColor(Ui.CARD);
        col.addView(Ui.title(this, "Ingredients \u00b7 serves " + servings, 18));
        double scale = (double) servings / Math.max(1, recipe.servings);
        for (Ingredient i : recipe.ingredients) {
            String q = V17.qty(this, i, scale);
            TextView t = Ui.body(this, (q.isEmpty() ? "" : q + "  ") + i.name + (i.optional ? " (optional)" : ""));
            t.setTextSize(18);
            t.setPadding(0, dp(8), 0, dp(8));
            col.addView(t);
        }
        sv.addView(col);
        return sv;
    }

    void go(int s) {
        if (s < 0 || s >= recipe.steps.size()) return;
        step = s;
        render(true);
    }

    void render(boolean animate) {
        int n = recipe.steps.size();
        stepLabel.setText(String.format(Locale.getDefault(), "Step %d of %d", step + 1, n));
        progress.set(step + 1, n);
        final String txt = V17.step(this, recipe.steps.get(step));
        if (animate) {
            stepText.setAlpha(0f);
            stepText.setTranslationY(dp(8));
            stepText.setText(txt);
            stepText.animate().alpha(1f).translationY(0).setDuration(180).start();
        } else {
            stepText.setText(txt);
        }
        prevBtn.setEnabled(step > 0);
        prevBtn.setAlpha(step > 0 ? 1f : 0.4f);
        nextBtn.setText(step >= n - 1 ? "Done" : "Next step");
        updateTimerButton();
    }

    /** Timer chips for the durations in the current step ("10–12 min" starts at the lower value). */
    void updateTimerButton() {
        if (timerChips == null) return;
        timerChips.removeAllViews();
        for (final Durations.Match m : Durations.find(recipe.steps.get(step))) {
            String kind = m.isPressure() ? " at pressure" : m.isRelease() ? " natural release" : "";
            TextView c = Ui.chip(this, "Start " + m.label() + kind, false);
            Drawable d = getResources().getDrawable(R.drawable.ic_timer).mutate();
            d.setTint(Ui.GREEN_DARK);
            d.setBounds(0, 0, dp(18), dp(18));
            c.setCompoundDrawables(d, null, null, null);
            c.setCompoundDrawablePadding(dp(6));
            c.setTextSize(16);
            c.setMinHeight(dp(48));
            c.setContentDescription("Start a " + m.label() + " timer" + (m.isRange() ? " (starts at " + Durations.format(m.seconds) + ")" : ""));
            c.setOnClickListener(v -> startTimer(m));
            timerChips.addView(c);
            String note = V17.timerNote(m);
            if (!note.isEmpty()) {
                c.setContentDescription(c.getContentDescription() + ". " + note);
                TextView nt = Ui.caption(this, note);
                nt.setPadding(dp(4), 0, dp(4), dp(4));
                timerChips.addView(nt);
            }
        }
        timerChips.setVisibility(timerChips.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    void startTimer(Durations.Match m) {
        stopAlarm();
        int running = 0;
        for (Timer t : timers) if (!t.done) running++;
        if (running >= MAX_TIMERS) { Ui.toastShort(this, "Up to " + MAX_TIMERS + " timers at once"); return; }
        Timer t = new Timer();
        t.id = Long.toString(System.currentTimeMillis(), 36) + timers.size();
        t.step = step;
        t.label = m.label() + (m.isPressure() ? " at pressure" : m.isRelease() ? " release" : "");
        t.total = m.seconds * 1000L;
        t.end = System.currentTimeMillis() + t.total;
        timers.add(t);
        TimerAlarm.schedule(this, recipe.id, t.id, t.label, t.end);
        saveTimers();
        renderTimers();
    }

    void pauseTimer(Timer t) {
        long now = System.currentTimeMillis();
        if (t.paused) { t.end = now + t.left; t.paused = false; TimerAlarm.schedule(this, recipe.id, t.id, t.label, t.end); }
        else { t.left = Math.max(0, t.end - now); t.paused = true; TimerAlarm.cancel(this, t.id); }
        saveTimers();
        renderTimers();
    }

    void cancelTimer(Timer t) {
        timers.remove(t);
        TimerAlarm.cancel(this, t.id);
        boolean ringing = false;
        for (Timer x : timers) ringing |= x.done;
        if (!ringing) stopAlarm();
        saveTimers();
        renderTimers();
    }

    final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            boolean fired = false;
            boolean silent = false;
            for (Timer t : timers) if (!t.done && !t.paused && t.end <= now) {
                t.done = true;
                // ended while cook mode was in the background: the notification already rang
                if (awaySince > 0 && t.end >= awaySince && t.end < resumedAt) silent = true; else fired = true;
            }
            if (fired) { saveTimers(); alarm(); renderTimers(); }
            else if (silent) { saveTimers(); renderTimers(); }
            else updateTimerTexts(now);
            handler.postDelayed(this, 500);
        }
    };

    long awaySince, resumedAt;

    final java.util.Map<String, TextView> timerTexts = new java.util.HashMap<String, TextView>();

    void updateTimerTexts(long now) {
        for (Timer t : timers) {
            TextView tv = timerTexts.get(t.id);
            if (tv != null && !t.done) tv.setText(fmt(t.remaining(now)));
        }
    }

    /** One bar per timer, visible on every step. */
    void renderTimers() {
        if (timersBar == null) return;
        timersBar.removeAllViews();
        timerTexts.clear();
        long now = System.currentTimeMillis();
        for (final Timer t : timers) {
            LinearLayout row = Ui.row(this);
            int r = dp(16);
            row.setBackground(Ui.rounded(t.done ? Ui.AMBER_TINT : Ui.GREEN_TINT, r));
            row.setPadding(dp(14), dp(6), dp(6), dp(6));
            LinearLayout col = Ui.column(this);
            TextView what = Ui.text(this, "Step " + (t.step + 1) + " \u00b7 " + t.label + (t.paused ? " \u00b7 paused" : ""), 13,
                    t.done ? Ui.AMBER_TEXT : Ui.GREEN_DARK, Ui.MEDIUM);
            col.addView(what);
            TextView left = Ui.text(this, t.done ? "Time's up!" : fmt(t.remaining(now)), 22, t.done ? Ui.AMBER_TEXT : Ui.TEXT, Ui.MEDIUM);
            col.addView(left);
            if (!t.done) timerTexts.put(t.id, left);
            row.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
            if (t.done) {
                TextView dis = Ui.button(this, "Dismiss", 0, true);
                dis.setOnClickListener(v -> cancelTimer(t));
                row.addView(dis, new LinearLayout.LayoutParams(-2, dp(48)));
            } else {
                ImageView pz = Ui.iconButton(this, t.paused ? R.drawable.ic_play : R.drawable.ic_pause, Ui.TEXT, (t.paused ? "Resume " : "Pause ") + t.label + " timer");
                pz.setOnClickListener(v -> pauseTimer(t));
                row.addView(pz);
                ImageView x = Ui.iconButton(this, R.drawable.ic_close, Ui.TEXT2, "Cancel " + t.label + " timer");
                x.setOnClickListener(v -> cancelTimer(t));
                row.addView(x);
            }
            timersBar.addView(row, Ui.lpMargins(this, -1, -2, 0, 0, 0, 8));
        }
        timersBar.setVisibility(timers.isEmpty() ? View.GONE : View.VISIBLE);
    }

    void alarm() {
        try {
            android.os.Vibrator vib = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) vib.vibrate(new long[]{0, 400, 200, 400, 200, 700}, -1);
        } catch (Exception ignored) { }
        try {
            if (ringtone != null && ringtone.isPlaying()) return;
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = u == null ? null : RingtoneManager.getRingtone(this, u);
            if (ringtone != null) ringtone.play();
            handler.postDelayed(this::stopAlarm, 30000);
        } catch (Exception ignored) { }
    }

    void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
    }

    String timersKey() { return "cookTimers:" + recipe.id; }

    void saveTimers() {
        org.json.JSONArray a = new org.json.JSONArray();
        try {
            for (Timer t : timers) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("id", t.id); o.put("label", t.label); o.put("step", t.step); o.put("total", t.total);
                o.put("end", t.end); o.put("left", t.left); o.put("paused", t.paused); o.put("done", t.done);
                a.put(o);
            }
        } catch (org.json.JSONException ignored) { }
        getSharedPreferences("cook", MODE_PRIVATE).edit().putString(timersKey(), a.toString()).apply();
    }

    void loadTimers() {
        timers.clear();
        try {
            org.json.JSONArray a = new org.json.JSONArray(getSharedPreferences("cook", MODE_PRIVATE).getString(timersKey(), "[]"));
            long now = System.currentTimeMillis();
            for (int k = 0; k < a.length() && timers.size() < MAX_TIMERS; k++) {
                org.json.JSONObject o = a.getJSONObject(k);
                Timer t = new Timer();
                t.id = o.optString("id", "t" + k); t.label = o.optString("label", ""); t.step = o.optInt("step");
                t.total = o.optLong("total"); t.end = o.optLong("end"); t.left = o.optLong("left");
                t.paused = o.optBoolean("paused"); t.done = o.optBoolean("done");
                // a timer that finished long ago (an hour+) while the app was closed is dropped
                if (t.done || (!t.paused && t.end <= now)) { if (now - t.end > 3600_000L) continue; t.done = true; }
                if (t.step < 0 || t.step >= recipe.steps.size()) t.step = 0;
                timers.add(t);
            }
        } catch (Exception ignored) { }
    }

    static String fmt(long ms) {
        return Durations.clock(ms);
    }
}
