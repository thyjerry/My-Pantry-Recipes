package org.pantryrecipes.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Lays children out left to right, wrapping to new lines (for chips). */
final class FlowLayout extends ViewGroup {
    private final int hGap, vGap;

    FlowLayout(Context c, int hGapPx, int vGapPx) {
        super(c);
        hGap = hGapPx;
        vGap = vGapPx;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int maxW = MeasureSpec.getSize(wSpec) - getPaddingLeft() - getPaddingRight();
        boolean unbounded = MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED;
        int x = 0, y = 0, lineH = 0, widest = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            ch.measure(MeasureSpec.makeMeasureSpec(unbounded ? 0 : maxW, unbounded ? MeasureSpec.UNSPECIFIED : MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (!unbounded && x > 0 && x + w > maxW) {
                x = 0;
                y += lineH + vGap;
                lineH = 0;
            }
            x += w + hGap;
            widest = Math.max(widest, x - hGap);
            lineH = Math.max(lineH, h);
        }
        int totalH = y + lineH + getPaddingTop() + getPaddingBottom();
        int width = unbounded ? widest + getPaddingLeft() + getPaddingRight() : MeasureSpec.getSize(wSpec);
        setMeasuredDimension(width, resolveSize(totalH, hSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxW = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, lineH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && x + w > maxW) {
                x = 0;
                y += lineH + vGap;
                lineH = 0;
            }
            ch.layout(getPaddingLeft() + x, getPaddingTop() + y, getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + hGap;
            lineH = Math.max(lineH, h);
        }
    }
}
