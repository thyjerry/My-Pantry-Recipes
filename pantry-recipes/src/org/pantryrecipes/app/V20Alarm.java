package org.pantryrecipes.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.Inventory;

import java.util.Calendar;
import java.util.List;

/** v2.0: once a day (about 17:00) tells you what is use-by tomorrow (and, if turned on, a ticked holiday coming up); tapping opens a "cook this tonight" idea. On-device only. */
public final class V20Alarm extends BroadcastReceiver {
    static final String CHANNEL = "expiry";
    static final int ID = 2020;

    static int immutable() { return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0; }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(c, ID, new Intent(c, V20Alarm.class), PendingIntent.FLAG_UPDATE_CURRENT | immutable());
        am.cancel(pi);
        if (!Store.get(c).prefs.getBoolean("expiryReminders", true) && !Store.get(c).prefs.getBoolean("holNotify30", false)) return;
        Calendar k = Calendar.getInstance();
        k.set(Calendar.HOUR_OF_DAY, 17);
        k.set(Calendar.MINUTE, 0);
        k.set(Calendar.SECOND, 0);
        if (k.getTimeInMillis() <= System.currentTimeMillis()) k.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC, k.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    static void askPermission(Activity a) {
        if (Build.VERSION.SDK_INT >= 33 && a.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 33);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) { schedule(c); return; }
        NotificationManager nm0 = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm0 != null) { try { V30.notifyHolidays(c, nm0); } catch (RuntimeException ignored) { } } // Stage 4: optional, off by default
        if (!Store.get(c).prefs.getBoolean("expiryReminders", true)) return;
        List<Inventory.Item> exp = Inventory.expiringIn(V20.items(c), Dates.today(), 1);
        if (exp.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (Inventory.Item it : exp) sb.append(sb.length() == 0 ? "" : ", ").append(it.label);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Intent open = new Intent(c, MainActivity.class).putExtra("cookTonight", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, ID, open, PendingIntent.FLAG_UPDATE_CURRENT | immutable());
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new android.app.NotificationChannel(CHANNEL, "Use-by reminders", NotificationManager.IMPORTANCE_DEFAULT));
            b = new Notification.Builder(c, CHANNEL);
        } else {
            b = new Notification.Builder(c);
        }
        b.setSmallIcon(R.drawable.ic_leaf)
                .setContentTitle(exp.size() == 1 ? "Use by tomorrow: " + sb : exp.size() + " items use-by tomorrow")
                .setContentText(exp.size() == 1 ? "Tap for a \u201ccook this tonight\u201d idea" : sb + " \u2013 tap for a \u201ccook this tonight\u201d idea")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .addAction(0, "Cook this tonight", pi);
        try { nm.notify(ID, b.build()); } catch (SecurityException ignored) { }
    }
}
