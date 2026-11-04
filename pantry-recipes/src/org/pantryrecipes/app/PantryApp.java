package org.pantryrecipes.app;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * v1.8 crash safety net. An uncaught exception is written (stack trace, app version, Android version, time)
 * to files/last-crash.txt in the app's private storage, then handed to the system's default handler as
 * before. Nothing is sent anywhere: About shows the report and lets the user copy or delete it.
 */
public final class PantryApp extends Application {
    static final String CRASH_FILE = "last-crash.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        install(this);
    }

    static void install(final Context app) {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        if (prev instanceof Handler) return;
        Thread.setDefaultUncaughtExceptionHandler(new Handler(app.getApplicationContext(), prev));
    }

    static final class Handler implements Thread.UncaughtExceptionHandler {
        final Context app;
        final Thread.UncaughtExceptionHandler prev;
        Handler(Context app, Thread.UncaughtExceptionHandler prev) { this.app = app; this.prev = prev; }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            try { write(app, report(app, t, e)); } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
            else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10); }
        }
    }

    static String report(Context c, Thread t, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String ver = "?";
        try { ver = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; } catch (Throwable ignored) { }
        pw.println("My Pantry Recipes " + ver + " crashed");
        pw.println("Time: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", java.util.Locale.US).format(new java.util.Date()));
        pw.println("Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + "), "
                + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        pw.println("Thread: " + (t == null ? "?" : t.getName()));
        pw.println();
        if (e != null) e.printStackTrace(pw);
        pw.flush();
        String s = sw.toString();
        return s.length() > 60000 ? s.substring(0, 60000) : s;
    }

    static void write(Context c, String text) throws Exception {
        FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), CRASH_FILE));
        try { out.write(text.getBytes("UTF-8")); } finally { out.close(); }
    }

    /** The saved report, or null when the app has not crashed since the report was last deleted. */
    static String lastCrash(Context c) {
        File f = new File(c.getFilesDir(), CRASH_FILE);
        if (!f.isFile() || f.length() == 0) return null;
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                byte[] b = new byte[(int) Math.min(f.length(), 65536)];
                int n = 0, r;
                while (n < b.length && (r = in.read(b, n, b.length - n)) > 0) n += r;
                return new String(b, 0, n, "UTF-8");
            } finally { in.close(); }
        } catch (Exception ex) {
            return null;
        }
    }

    static void clearCrash(Context c) {
        new File(c.getFilesDir(), CRASH_FILE).delete();
    }
}
