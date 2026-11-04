package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Calendar dates as ISO "yyyy-mm-dd" strings (no java.time, so it runs on Android 5). */
public final class Dates {
    private Dates() { }

    public static final String[] DAY_SHORT = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    public static final String[] MONTH_SHORT = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private static int[] parts(String iso) {
        if (iso == null || iso.length() != 10 || iso.charAt(4) != '-' || iso.charAt(7) != '-') return null;
        for (int i = 0; i < 10; i++) {
            char c = iso.charAt(i);
            if (i != 4 && i != 7 && (c < '0' || c > '9')) return null;
        }
        try {
            int y = Integer.parseInt(iso.substring(0, 4)), m = Integer.parseInt(iso.substring(5, 7)), d = Integer.parseInt(iso.substring(8, 10));
            if (y < 1900 || y > 2999 || m < 1 || m > 12 || d < 1 || d > daysInMonth(y, m)) return null;
            return new int[]{y, m, d};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static int daysInMonth(int y, int m) {
        if (m == 2) return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 ? 29 : 28;
        return m == 4 || m == 6 || m == 9 || m == 11 ? 30 : 31;
    }

    public static boolean valid(String iso) { return parts(iso) != null; }

    /** Days since 1970-01-01, or Long.MIN_VALUE for an invalid date. */
    public static long toDays(String iso) {
        int[] p = parts(iso);
        if (p == null) return Long.MIN_VALUE;
        long y = p[0], m = p[1], d = p[2];
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    public static String fromDays(long z) {
        z += 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        if (m <= 2) y++;
        return pad(y, 4) + "-" + pad(m, 2) + "-" + pad(d, 2);
    }

    private static String pad(long v, int n) {
        String s = String.valueOf(v);
        while (s.length() < n) s = "0" + s;
        return s;
    }

    public static String addDays(String iso, int n) { return fromDays(toDays(iso) + n); }

    /** v2.0: adds whole months, clamping the day to the month's length (2026-01-31 + 1 = 2026-02-28). */
    public static String addMonths(String iso, int n) {
        int[] p = parts(iso);
        if (p == null) return "";
        int t = p[0] * 12 + (p[1] - 1) + n, y = t / 12, m = t % 12 + 1;
        int d = Math.min(p[2], daysInMonth(y, m));
        return pad(y, 4) + "-" + pad(m, 2) + "-" + pad(d, 2);
    }

    /** b minus a in days (positive when b is later). */
    public static int between(String a, String b) { return (int) (toDays(b) - toDays(a)); }

    /** 0 = Monday ... 6 = Sunday. */
    public static int dayOfWeek(String iso) { return (int) (((toDays(iso) + 3) % 7 + 7) % 7); }

    public static String weekStart(String iso) { return addDays(iso, -dayOfWeek(iso)); }

    /** The seven dates (Monday first) of the week containing {@code iso}. */
    public static List<String> week(String iso) {
        List<String> out = new ArrayList<String>();
        String s = weekStart(iso);
        for (int i = 0; i < 7; i++) out.add(addDays(s, i));
        return out;
    }

    /** "Mon 2 Nov". */
    public static String label(String iso) {
        int[] p = parts(iso);
        if (p == null) return iso == null ? "" : iso;
        return DAY_SHORT[dayOfWeek(iso)] + " " + p[2] + " " + MONTH_SHORT[p[1] - 1];
    }

    /** Today in the device's time zone. */
    public static String today() {
        Calendar c = Calendar.getInstance();
        return pad(c.get(Calendar.YEAR), 4) + "-" + pad(c.get(Calendar.MONTH) + 1, 2) + "-" + pad(c.get(Calendar.DAY_OF_MONTH), 2);
    }
}
