package org.pantryrecipes.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Thin rounded progress bar showing how much of a recipe the pantry covers. */
final class MatchBar extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float fraction;

    MatchBar(Context c) {
        super(c);
        track.setColor(Ui.LINE);
        fill.setColor(Ui.GREEN);
    }

    void set(int have, int total) {
        fraction = total == 0 ? 0 : (float) have / total;
        fill.setColor(have == total && total > 0 ? Ui.GREEN : (fraction >= 0.6f ? blendBar() : Ui.AMBER));
        invalidate();
    }

    private static int blendBar() { return MainActivity.blend(Ui.GREEN, Ui.AMBER, 0.35f); }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight(), w = getWidth();
        r.set(0, 0, w, h);
        c.drawRoundRect(r, h / 2, h / 2, track);
        if (fraction > 0) {
            r.set(0, 0, Math.max(h, w * fraction), h);
            c.drawRoundRect(r, h / 2, h / 2, fill);
        }
    }
}
