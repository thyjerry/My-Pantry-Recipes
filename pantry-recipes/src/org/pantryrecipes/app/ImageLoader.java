package org.pantryrecipes.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal image loader for TheMealDB thumbnails: memory LRU + small disk cache. */
final class ImageLoader {
    private static ImageLoader instance;
    private final LruCache<String, Bitmap> mem = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final File dir;

    private ImageLoader(Context c) {
        dir = new File(c.getCacheDir(), "img");
        dir.mkdirs();
    }

    static synchronized ImageLoader get(Context c) {
        if (instance == null) instance = new ImageLoader(c.getApplicationContext());
        return instance;
    }

    /** Loads url (thumbnail size if small) into the view. */
    void load(final ImageView iv, String imageUrl, final boolean small) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        final String url = small ? imageUrl + "/preview" : imageUrl + "/medium";
        iv.setTag(url);
        Bitmap b = mem.get(url);
        if (b != null) { iv.setImageBitmap(b); return; }
        pool.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = fetch(url);
                if (bmp == null) return;
                mem.put(url, bmp);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (url.equals(iv.getTag())) {
                            iv.setAlpha(0f);
                            iv.setImageBitmap(bmp);
                            iv.animate().alpha(1f).setDuration(180).start();
                        }
                    }
                });
            }
        });
    }

    private Bitmap fetch(String url) {
        File f = new File(dir, Integer.toHexString(url.hashCode()) + ".jpg");
        try {
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getPath());
                if (b != null) return b;
            }
            if (!url.startsWith("https://www.themealdb.com/")) return null;
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent", "PantryRecipes/1.0 (Android)");
            if (con.getResponseCode() != 200) return null;
            InputStream in = con.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            byte[] data = bos.toByteArray();
            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (b != null) {
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(data);
                fo.close();
                trimDisk();
            }
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private void trimDisk() {
        File[] files = dir.listFiles();
        if (files == null || files.length < 400) return;
        java.util.Arrays.sort(files, new java.util.Comparator<File>() {
            @Override
            public int compare(File a, File b) { return Long.compare(a.lastModified(), b.lastModified()); }
        });
        for (int i = 0; i < files.length - 300; i++) files[i].delete();
    }
}
