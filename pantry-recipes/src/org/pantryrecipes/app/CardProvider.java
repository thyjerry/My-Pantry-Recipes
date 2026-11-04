package org.pantryrecipes.app;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import org.pantryrecipes.app.core.Ingredient;
import org.pantryrecipes.app.core.Recipe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * v2.0 (modern build only): draws a simple 1080x1350 recipe card and shares it read-only through this provider.
 * The legacy (Fire HD 8) build has no provider registered and never shows the button.
 */
public final class CardProvider extends ContentProvider {
    static String authority(Activity a) { return a.getPackageName() + ".cards"; }

    static void shareCard(final Activity a, final Recipe r) {
        new Thread(() -> {
            try {
                File dir = new File(a.getCacheDir(), "cards");
                dir.mkdirs();
                for (File f : dir.listFiles()) f.delete();
                File out = new File(dir, "recipe-card.png");
                Bitmap bm = draw(r);
                FileOutputStream fo = new FileOutputStream(out);
                bm.compress(Bitmap.CompressFormat.PNG, 100, fo);
                fo.close();
                bm.recycle();
                final Uri u = Uri.parse("content://" + authority(a) + "/recipe-card.png");
                a.runOnUiThread(() -> {
                    Intent i = new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try { a.startActivity(Intent.createChooser(i, "Share card")); } catch (Exception e) { Ui.toastShort(a, "No app to share with"); }
                });
            } catch (Exception e) {
                a.runOnUiThread(() -> Ui.toastShort(a, "Could not make the card"));
            }
        }).start();
    }

    static Bitmap draw(Recipe r) {
        int w = 1080, h = 1350;
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        cv.drawColor(0xFFFFFBF2);
        Paint band = new Paint();
        band.setColor(0xFF2E6B3A);
        cv.drawRect(0, 0, w, 220, band);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xFFFFFFFF);
        tp.setTextSize(64);
        tp.setFakeBoldText(true);
        String t = r.title.length() > 30 ? r.title.substring(0, 29) + "\u2026" : r.title;
        cv.drawText(t, 60, 140, tp);
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setColor(0xFF1F2A22);
        bp.setTextSize(40);
        int y = 310;
        cv.drawText("Serves " + r.servings + (r.totalMinutes() > 0 ? " \u00b7 " + r.totalMinutes() + " min" : ""), 60, y, bp);
        y += 80;
        for (Ingredient i : r.ingredients) {
            if (y > h - 140) { cv.drawText("\u2026", 60, y, bp); break; }
            String line = "\u2022 " + (i.quantityText(1).isEmpty() ? "" : i.quantityText(1) + " ") + i.name;
            if (line.length() > 44) line = line.substring(0, 43) + "\u2026";
            cv.drawText(line, 60, y, bp);
            y += 58;
        }
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFF5A6B5E);
        fp.setTextSize(30);
        cv.drawText("My Pantry Recipes", 60, h - 60, fp);
        return bm;
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read-only");
        File f = new File(new File(getContext().getCacheDir(), "cards"), "recipe-card.png");
        if (!"recipe-card.png".equals(uri.getLastPathSegment()) || !f.exists()) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        File f = new File(new File(getContext().getCacheDir(), "cards"), "recipe-card.png");
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        if (f.exists()) c.addRow(new Object[]{"recipe-card.png", f.length()});
        return c;
    }

    @Override public String getType(Uri uri) { return "image/png"; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
