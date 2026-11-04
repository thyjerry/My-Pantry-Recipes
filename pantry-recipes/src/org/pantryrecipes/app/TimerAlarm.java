package org.pantryrecipes.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * v1.8: cook-mode timers keep working when the screen is off or another app is in front. Each running timer
 * has an AlarmManager alarm; when it fires this receiver shows a notification with the alarm sound and vibration
 * (tap opens cook mode again). No new permissions: on Android 5.x–11 the alarm is exact (setExact /
 * setExactAndAllowWhileIdle); on Android 12+ without the exact-alarm permission it falls back to a short window,
 * and on Android 13+ the notification only shows if the user allowed notifications for the app (we never ask).
 * When cook mode for the same recipe is on screen, the activity rings itself and the notification is skipped.
 */
public final class TimerAlarm extends BroadcastReceiver {
    static final String ACTION = "org.pantryrecipes.app.TIMER";
    static final String CHANNEL = "timers";
    /** The recipe whose cook mode is in the foreground (set in onResume, cleared in onPause). */
    static volatile String foregroundRecipe;

    private static int requestCode(String timerId) { return ("t" + timerId).hashCode(); }

    private static int immutable() { return Build.VERSION.SDK_INT >= 23 ? 0x04000000 /* FLAG_IMMUTABLE */ : 0; }

    private static PendingIntent pending(Context c, String recipeId, String timerId, String label, int flags) {
        Intent i = new Intent(c, TimerAlarm.class).setAction(ACTION);
        i.putExtra("recipe", recipeId);
        i.putExtra("timer", timerId);
        i.putExtra("label", label);
        return PendingIntent.getBroadcast(c, requestCode(timerId), i, flags | immutable());
    }

    /** Schedules (or re-schedules) the alarm for a running timer. Never throws. */
    static void schedule(Context c, String recipeId, String timerId, String label, long endWallMs) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = pending(c, recipeId, timerId, label, PendingIntent.FLAG_UPDATE_CURRENT);
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= 31) {
                try { exact = (Boolean) AlarmManager.class.getMethod("canScheduleExactAlarms").invoke(am); } catch (Throwable t) { exact = false; }
            }
            if (!exact) { am.setWindow(AlarmManager.RTC_WAKEUP, endWallMs, 10_000L, pi); return; }
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    AlarmManager.class.getMethod("setExactAndAllowWhileIdle", int.class, long.class, PendingIntent.class)
                            .invoke(am, AlarmManager.RTC_WAKEUP, endWallMs, pi);
                    return;
                } catch (Throwable ignored) { }
            }
            am.setExact(AlarmManager.RTC_WAKEUP, endWallMs, pi);
        } catch (Throwable ignored) {
            // a timer without a background alarm still rings while cook mode is open
        }
    }

    static void cancel(Context c, String timerId) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = pending(c, "", timerId, "", PendingIntent.FLAG_NO_CREATE);
            if (am != null && pi != null) { am.cancel(pi); pi.cancel(); }
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(requestCode(timerId));
        } catch (Throwable ignored) { }
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        String recipe = intent.getStringExtra("recipe"), timer = intent.getStringExtra("timer"), label = intent.getStringExtra("label");
        if (recipe == null || timer == null) return;
        if (recipe.equals(foregroundRecipe)) return; // cook mode is showing and rings by itself
        notify(c, recipe, timer, label == null ? "" : label);
    }

    @SuppressWarnings("deprecation")
    static void notify(Context c, String recipeId, String timerId, String label) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            Notification.Builder b = new Notification.Builder(c);
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    Class<?> ch = Class.forName("android.app.NotificationChannel");
                    Object channel = ch.getConstructor(String.class, CharSequence.class, int.class).newInstance(CHANNEL, "Cooking timers", 4 /* IMPORTANCE_HIGH */);
                    NotificationManager.class.getMethod("createNotificationChannel", ch).invoke(nm, channel);
                    Notification.Builder.class.getMethod("setChannelId", String.class).invoke(b, CHANNEL);
                } catch (Throwable ignored) { }
            }
            Intent open = new Intent(c, CookActivity.class).putExtra("id", recipeId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent content = PendingIntent.getActivity(c, ("o" + timerId).hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT | immutable());
            b.setSmallIcon(R.drawable.ic_timer)
                    .setContentTitle("Time\u2019s up" + (label.isEmpty() ? "" : ": " + label))
                    .setContentText("My Pantry Recipes cooking timer \u2013 tap to open cook mode")
                    .setContentIntent(content)
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setOnlyAlertOnce(true);
            nm.notify(requestCode(timerId), b.build());
        } catch (Throwable ignored) { }
    }
}
