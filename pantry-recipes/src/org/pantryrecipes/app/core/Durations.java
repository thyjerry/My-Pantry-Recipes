package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds cooking times in a recipe step ("10 minutes", "1 hr 30 mins", "1½ hours", "half an hour",
 * "10-12 min", "2 to 3 hours", "between 5 and 8 minutes", "forty-five seconds") for cook-mode timers.
 * A range starts its timer at the lower value and keeps the range for display. Implemented as a small
 * hand-written scanner (no regular expressions) so the web port gives identical results.
 */
public final class Durations {
    private Durations() { }

    public static final int MAX_SECONDS = 48 * 3600;

    /** v1.7 match kinds: a plain timer, time at pressure (Instant Pot), or a natural pressure release. */
    public static final String TIMER = "timer", PRESSURE = "pressure", RELEASE = "release";
    /** Internal: the time a pressure cooker takes to come up to pressure. Such spans are never offered as timers. */
    static final String BUILD_UP = "build-up";

    /** Shown next to a pressure timer: the build-up is not part of the cook time. */
    public static final String BUILD_UP_NOTE = "The pot first needs about 10\u201315 min to come up to pressure. "
            + "That build-up is not counted here: start this timer when the float valve pops up and the countdown begins.";
    public static final String RELEASE_NOTE = "Natural release: leave the valve sealed. After this timer, quick release any remaining "
            + "pressure, keeping hands and face away from the steam.";

    public static final class Match {
        public final int start, end, seconds, maxSeconds;
        public final String text;
        /** TIMER, PRESSURE or RELEASE (v1.7). */
        public String kind = TIMER;
        Match(int start, int end, int seconds, int maxSeconds, String text) {
            this.start = start; this.end = end; this.seconds = seconds; this.maxSeconds = maxSeconds; this.text = text;
        }
        public boolean isPressure() { return PRESSURE.equals(kind); }
        public boolean isRelease() { return RELEASE.equals(kind); }
        public boolean isRange() { return maxSeconds > seconds; }
        /** Chip label: "10 min", or "10–12 min" for a range. */
        public String label() { return isRange() ? rangeLabel(seconds, maxSeconds) : format(seconds); }
    }

    // ---------------- tokens ----------------
    private static final int NUM = 0, WORD = 1, DASH = 2, OTHER = 3;
    private static final class Tok {
        final int type, start, end; final double num; final String word; final boolean fraction;
        Tok(int type, int start, int end, double num, String word, boolean fraction) {
            this.type = type; this.start = start; this.end = end; this.num = num; this.word = word; this.fraction = fraction;
        }
    }

    private static double vulgar(char c) {
        switch (c) {
            case '\u00BD': return 0.5;
            case '\u00BC': return 0.25;
            case '\u00BE': return 0.75;
            case '\u2153': return 1.0 / 3;
            case '\u2154': return 2.0 / 3;
            case '\u215B': return 0.125;
            default: return -1;
        }
    }

    private static boolean digit(char c) { return c >= '0' && c <= '9'; }
    private static boolean letter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
    private static char lower(char c) { return c >= 'A' && c <= 'Z' ? (char) (c + 32) : c; }

    private static List<Tok> tokens(String s) {
        List<Tok> out = new ArrayList<Tok>();
        int n = s.length(), i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u00A0') { i++; continue; }
            if (digit(c)) {
                int j = i;
                while (j < n && digit(s.charAt(j))) j++;
                double v = Double.parseDouble(s.substring(i, j));
                boolean frac = false;
                if (j + 1 < n && (s.charAt(j) == '.' || s.charAt(j) == ',') && digit(s.charAt(j + 1))) {
                    int k = j + 1;
                    while (k < n && digit(s.charAt(k))) k++;
                    v = Double.parseDouble(s.substring(i, j) + "." + s.substring(j + 1, k));
                    j = k;
                } else if (j + 1 < n && s.charAt(j) == '/' && digit(s.charAt(j + 1))) {
                    int k = j + 1;
                    while (k < n && digit(s.charAt(k))) k++;
                    double d = Double.parseDouble(s.substring(j + 1, k));
                    if (d > 0) { v = v / d; frac = true; }
                    j = k;
                }
                if (j < n && vulgar(s.charAt(j)) >= 0) { v += vulgar(s.charAt(j)); j++; }
                out.add(new Tok(NUM, i, j, v, null, frac));
                i = j;
            } else if (vulgar(c) >= 0) {
                out.add(new Tok(NUM, i, i + 1, vulgar(c), null, true));
                i++;
            } else if (letter(c)) {
                int j = i;
                StringBuilder b = new StringBuilder();
                while (j < n && (letter(s.charAt(j)) || (s.charAt(j) == '-' && j + 1 < n && letter(s.charAt(j + 1)) && j > i))) {
                    b.append(lower(s.charAt(j)));
                    j++;
                }
                out.add(new Tok(WORD, i, j, 0, b.toString(), false));
                i = j;
            } else if (c == '-' || c == '\u2013' || c == '\u2014') {
                out.add(new Tok(DASH, i, i + 1, 0, null, false));
                i++;
            } else {
                out.add(new Tok(OTHER, i, i + 1, 0, null, false));
                i++;
            }
        }
        return out;
    }

    private static final String[] ONES = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
    private static final String[] TENS = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};

    /** Value of a number word ("seven", "forty-five", "thirty"), or -1. */
    static int wordValue(String w) {
        for (int k = 0; k < ONES.length; k++) if (ONES[k].equals(w)) return k;
        for (int k = 2; k < TENS.length; k++) {
            if (TENS[k].equals(w)) return k * 10;
            if (w.startsWith(TENS[k] + "-")) {
                String u = w.substring(TENS[k].length() + 1);
                for (int m = 1; m <= 9; m++) if (ONES[m].equals(u)) return k * 10 + m;
            }
        }
        return -1;
    }

    /** Seconds per unit word, or 0 when the word is not a time unit. */
    static int unitSeconds(String w) {
        if (w == null) return 0;
        if (w.equals("hours") || w.equals("hour") || w.equals("hrs") || w.equals("hr") || w.equals("h")) return 3600;
        if (w.equals("minutes") || w.equals("minute") || w.equals("mins") || w.equals("min")) return 60;
        if (w.equals("seconds") || w.equals("second") || w.equals("secs") || w.equals("sec")) return 1;
        return 0;
    }

    private static boolean isWord(List<Tok> t, int i, String w) {
        return i >= 0 && i < t.size() && t.get(i).type == WORD && t.get(i).word.equals(w);
    }

    /** An amount at i: {value, nextIndex}, or null. */
    private static double[] amount(List<Tok> t, int i) {
        if (i >= t.size()) return null;
        Tok a = t.get(i);
        if (a.type == NUM) {
            double v = a.num;
            int j = i + 1;
            // "1 1/2" or "1 ½"
            if (!a.fraction && j < t.size() && t.get(j).type == NUM && t.get(j).fraction && t.get(j).num < 1) { v += t.get(j).num; j++; }
            if (isWord(t, j, "and") && isWord(t, j + 1, "a") && isWord(t, j + 2, "half")) { v += 0.5; j += 3; }
            return new double[]{v, j};
        }
        if (a.type != WORD) return null;
        if (a.word.equals("half")) {
            // "half an hour", "half a minute", "half hour"
            int j = i + 1;
            if (isWord(t, j, "an") || isWord(t, j, "a")) j++;
            return new double[]{0.5, j};
        }
        if (a.word.equals("a") || a.word.equals("an")) {
            if (isWord(t, i + 1, "couple") && isWord(t, i + 2, "of")) return new double[]{2, i + 3};
            if (isWord(t, i + 1, "couple")) return new double[]{2, i + 2};
            if (isWord(t, i + 1, "half")) return new double[]{0.5, i + 2};
            return new double[]{1, i + 1};
        }
        int w = wordValue(a.word);
        if (w < 0) return null;
        int j = i + 1;
        // "twenty five"
        if (w >= 20 && w % 10 == 0 && j < t.size() && t.get(j).type == WORD) {
            int u = wordValue(t.get(j).word);
            if (u >= 1 && u <= 9) { w += u; j++; }
        }
        double v = w;
        if (isWord(t, j, "and") && isWord(t, j + 1, "a") && isWord(t, j + 2, "half")) { v += 0.5; j += 3; }
        return new double[]{v, j};
    }

    /** A full duration at i ("1 hour 30 minutes", "an hour and a half"): {seconds, nextIndex, firstUnit}, or null. */
    private static double[] duration(List<Tok> t, int i) {
        double[] a = amount(t, i);
        if (a == null) return null;
        int j = (int) a[1];
        if (j >= t.size() || t.get(j).type != WORD) return null;
        int u = unitSeconds(t.get(j).word);
        if (u == 0) return null;
        double total = a[0] * u;
        int first = u;
        j++;
        if (isWord(t, j, "and") && isWord(t, j + 1, "a") && isWord(t, j + 2, "half")) { total += 0.5 * u; j += 3; }
        // compound: "1 hour 30 minutes", "1 hr and 15 mins", "2 minutes 30 seconds"
        while (true) {
            int k = j;
            if (isWord(t, k, "and")) k++;
            if (k >= t.size() || t.get(k).type != NUM) break;
            double[] b = amount(t, k);
            if (b == null) break;
            int m = (int) b[1];
            if (m >= t.size() || t.get(m).type != WORD) break;
            int u2 = unitSeconds(t.get(m).word);
            if (u2 == 0 || u2 >= u) break;
            total += b[0] * u2;
            u = u2;
            j = m + 1;
        }
        return new double[]{total, j, first};
    }

    private static boolean rangeSep(List<Tok> t, int i) {
        return i < t.size() && (t.get(i).type == DASH || isWord(t, i, "to") || isWord(t, i, "or"));
    }

    /** Every time mentioned in {@code text}, in order, without overlaps. */
    public static List<Match> find(String text) {
        List<Match> out = new ArrayList<Match>();
        if (text == null || text.isEmpty()) return out;
        List<Tok> t = tokens(text);
        int i = 0;
        while (i < t.size()) {
            int lo = -1, hi = -1, startTok = i, endTok = -1;
            boolean between = isWord(t, i, "between");
            int s0 = between ? i + 1 : i;
            double[] d1 = duration(t, s0);
            if (d1 != null) {
                lo = (int) Math.round(d1[0]);
                hi = lo;
                endTok = (int) d1[1];
                int k = endTok;
                if (rangeSep(t, k) || (between && isWord(t, k, "and"))) {
                    double[] d2 = duration(t, k + 1);
                    if (d2 != null && d2[0] >= d1[0]) { hi = (int) Math.round(d2[0]); endTok = (int) d2[1]; }
                }
                if (!between) startTok = i; else startTok = i;
            } else {
                // bare first number of a range: "10-12 minutes", "2 to 3 hours", "between 5 and 8 minutes"
                double[] a = amount(t, s0);
                if (a != null && t.get(s0).type == NUM) {
                    int k = (int) a[1];
                    if (rangeSep(t, k) || (between && isWord(t, k, "and"))) {
                        double[] d2 = duration(t, k + 1);
                        if (d2 != null) {
                            int u = (int) d2[2];
                            double low = a[0] * u;
                            if (low > 0 && low <= d2[0]) {
                                lo = (int) Math.round(low);
                                hi = (int) Math.round(d2[0]);
                                endTok = (int) d2[1];
                            }
                        }
                    }
                }
            }
            if (lo > 0 && lo <= MAX_SECONDS && endTok > 0) {
                if (!between) startTok = s0;
                int st = t.get(startTok).start, en = t.get(endTok - 1).end;
                Match mm = new Match(st, en, lo, Math.min(hi, MAX_SECONDS), text.substring(st, en));
                mm.kind = kindAt(text, st, en);
                if (!BUILD_UP.equals(mm.kind)) out.add(mm); // "about 10 to 15 minutes to come up to pressure" is not a timer

                i = endTok;
            } else {
                i++;
            }
        }
        return out;
    }

    private static String lowerAscii(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) b.append(lower(s.charAt(i)));
        return b.toString();
    }

    /**
     * v1.7: what a time in a step measures. "Pressure cook on High for 12 minutes", "cook at high pressure for 8 min",
     * "12 minutes at high pressure" and "Manual/Pressure Cook 5 min" are PRESSURE; "release naturally for 10 minutes",
     * "natural release 15 min", "let the pressure release naturally for about 10 minutes" are RELEASE. Only the same
     * clause counts (text after the last ".", ";" or "," before the time, up to 60 characters).
     */
    static String kindAt(String text, int start, int end) {
        int from = Math.max(0, start - 60);
        for (int i = start - 1; i >= from; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == ';' || c == ',' || c == '!' || c == '?') { from = i + 1; break; }
        }
        String before = lowerAscii(text.substring(from, start));
        int to = Math.min(text.length(), end + 25);
        String after = lowerAscii(text.substring(end, to));
        if (after.startsWith(" to come up to pressure") || after.startsWith(" to come to pressure") || after.startsWith(" to reach pressure")
                || after.startsWith(" to build pressure") || after.startsWith(" to build up pressure") || after.startsWith(" to pressurize")
                || after.startsWith(" to pressurise"))
            return BUILD_UP;
        if (before.contains("release naturally") || before.contains("natural release") || before.contains("naturally release")
                || before.contains("natural pressure release") || after.startsWith(" natural release") || after.startsWith(" of natural release"))
            return RELEASE;
        if (before.contains("pressure cook") || before.contains("pressure-cook") || before.contains("at high pressure")
                || before.contains("at low pressure") || before.contains("on high pressure") || before.contains("on low pressure")
                || after.startsWith(" at high pressure") || after.startsWith(" at low pressure") || after.startsWith(" on high pressure")
                || after.startsWith(" on low pressure") || after.startsWith(" under pressure") || after.startsWith(" at pressure"))
            return PRESSURE;
        return TIMER;
    }

    /** "45 sec", "10 min", "1 h 30 min", "2 h", "1 min 30 sec". */
    public static String format(int seconds) {
        int h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        StringBuilder b = new StringBuilder();
        if (h > 0) b.append(h).append(" h");
        if (m > 0) { if (b.length() > 0) b.append(' '); b.append(m).append(" min"); }
        if (s > 0) { if (b.length() > 0) b.append(' '); b.append(s).append(" sec"); }
        return b.length() == 0 ? "0 sec" : b.toString();
    }

    /** "10–12 min" when both ends are whole minutes under an hour, otherwise "10 min – 1 h". */
    public static String rangeLabel(int lo, int hi) {
        if (lo % 60 == 0 && hi % 60 == 0 && hi < 3600) return (lo / 60) + "\u2013" + (hi / 60) + " min";
        if (lo % 3600 == 0 && hi % 3600 == 0) return (lo / 3600) + "\u2013" + (hi / 3600) + " h";
        return format(lo) + " \u2013 " + format(hi);
    }

    /** Countdown text: "9:05" or "1:02:03". */
    public static String clock(long ms) {
        long sec = Math.max(0, (ms + 999) / 1000);
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        String ss = (s < 10 ? "0" : "") + s;
        if (h > 0) return h + ":" + (m < 10 ? "0" : "") + m + ":" + ss;
        return m + ":" + ss;
    }
}
