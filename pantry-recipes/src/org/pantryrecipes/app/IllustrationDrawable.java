package org.pantryrecipes.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import org.pantryrecipes.app.core.Illustration;
import org.pantryrecipes.app.core.Recipe;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * v2.2: the per-recipe illustrated card (see core/Illustration). Vector shapes only: no bitmaps, no network, a few
 * dozen draw calls. One instance per (glyph, cuisine look, corner) is shared by every row that shows it.
 */
final class IllustrationDrawable extends Drawable {
    static Map<String, Illustration.Glyph> glyphs;
    static final Map<Illustration.Shape, Path> POLY = new HashMap<Illustration.Shape, Path>();
    static final Map<String, IllustrationDrawable> CACHE = new HashMap<String, IllustrationDrawable>();

    static synchronized Map<String, Illustration.Glyph> glyphs(Context c) {
        if (glyphs == null) {
            try {
                InputStream in = c.getAssets().open("illustrations.txt");
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                byte[] b = new byte[4096]; int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
                in.close();
                glyphs = Illustration.parse(o.toString("UTF-8"));
            } catch (Exception e) { glyphs = new HashMap<String, Illustration.Glyph>(); }
        }
        return glyphs;
    }

    /** Shared drawable for a recipe; {@code tlOnly} rounds the top corners only (carousel cards). */
    static Drawable forRecipe(Context c, Recipe r, float radius, boolean tlOnly) {
        String k = Illustration.key(r) + "|" + radius + "|" + tlOnly;
        IllustrationDrawable d = CACHE.get(k);
        if (d == null) {
            Illustration.Glyph g = glyphs(c).get(Illustration.glyph(r));
            if (g == null) g = glyphs(c).get("plate");
            if (CACHE.size() > 400) CACHE.clear();
            d = new IllustrationDrawable(g, Illustration.accent(r.cuisine), Illustration.pattern(r.cuisine), radius, tlOnly);
            CACHE.put(k, d);
        }
        return d.getConstantState().newDrawable();
    }

    final Illustration.Glyph g;
    final int accent, pattern;
    final float radius;
    final boolean tlOnly;
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path clip = new Path();
    final RectF rf = new RectF();

    IllustrationDrawable(Illustration.Glyph g, int accent, int pattern, float radius, boolean tlOnly) {
        this.g = g; this.accent = accent; this.pattern = pattern; this.radius = radius; this.tlOnly = tlOnly;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    int ink(char k) { return k == 'b' ? 0xFF000000 | g.bg : k == 'a' ? 0xFF000000 | accent : Illustration.INK; }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        if (w <= 0 || h <= 0) return;
        c.save();
        clip.reset();
        rf.set(b);
        float r = radius;
        clip.addRoundRect(rf, tlOnly ? new float[]{r, r, r, r, 0, 0, 0, 0} : new float[]{r, r, r, r, r, r, r, r}, Path.Direction.CW);
        c.clipPath(clip);
        c.drawColor(0xFF000000 | g.bg);
        // cuisine pattern (decorative): light marks on the background
        fill.setColor(0x38FFFFFF); line.setColor(0x38FFFFFF);
        float u = Math.max(6f, h / 8f);
        if (pattern == 1) { for (float y = u / 2; y < h; y += u * 1.4f) for (float x = u / 2 + ((int) (y / u) % 2) * u * 0.7f; x < w; x += u * 1.4f) c.drawCircle(b.left + x, b.top + y, u * 0.18f, fill); }
        else if (pattern == 2) { line.setStrokeWidth(u * 0.3f); for (float x = -h; x < w; x += u * 1.2f) c.drawLine(b.left + x, b.bottom, b.left + x + h, b.top, line); }
        else if (pattern == 3) { line.setStrokeWidth(u * 0.25f); for (float y = u; y < h; y += u * 1.6f) c.drawLine(b.left, b.top + y, b.right, b.top + y, line); }
        // accent disc behind the glyph
        float s = Math.min(w, h) * 0.66f, cx = b.left + w / 2f, cy = b.top + h / 2f;
        fill.setColor(0x99FFFFFF & (0x99000000 | accent));
        c.drawCircle(cx, cy, s * 0.62f, fill);
        c.translate(cx - s / 2f, cy - s / 2f);
        c.scale(s / 24f, s / 24f);
        for (Illustration.Shape sh : g.shapes) {
            float[] v = sh.v;
            int col = ink(sh.ink);
            fill.setColor(col);
            switch (sh.kind) {
                case 'c': c.drawCircle(v[0], v[1], v[2], fill); break;
                case 'e': rf.set(v[0] - v[2], v[1] - v[3], v[0] + v[2], v[1] + v[3]); c.drawOval(rf, fill); break;
                case 'r': rf.set(v[0], v[1], v[0] + v[2], v[1] + v[3]); c.drawRoundRect(rf, v[4], v[4], fill); break;
                case 'l': line.setColor(col); line.setStrokeWidth(v[4]); c.drawLine(v[0], v[1], v[2], v[3], line); break;
                case 'p': {
                    Path p = POLY.get(sh);
                    if (p == null) {
                        p = new Path(); p.moveTo(v[0], v[1]);
                        for (int i = 2; i + 1 < v.length; i += 2) p.lineTo(v[i], v[i + 1]);
                        p.close(); POLY.put(sh, p);
                    }
                    c.drawPath(p, fill); break;
                }
                default: break;
            }
        }
        c.restore();
    }

    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }

    final ConstantState cs = new ConstantState() {
        @Override public Drawable newDrawable() { return new IllustrationDrawable(g, accent, pattern, radius, tlOnly); }
        @Override public int getChangingConfigurations() { return 0; }
    };
    @Override public ConstantState getConstantState() { return cs; }
}
