package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * v2.0 Stage 2 kitchen helpers: equivalents table, Rescue my dish, spice blend builder, cooking times and ingredient
 * explainers with local names. Data lives in assets/helpers20.txt and is parsed once, on first use (nothing at start-up).
 * Mirrored by PR.Helpers20 in the web core20.js (parity vectors in tests/vectors20h.json).
 */
public final class Helpers20 {
    public static final class Equiv { public String group, amount, equals, note; String key; }
    public static final class Rescue { public String problem, dishes, prevent = ""; public final List<String> fixes = new ArrayList<String>(); }
    public static final class Part { public double parts; public String partsText, spice, flag; }
    public static final class Blend { public String name, country, use; public final List<Part> parts = new ArrayList<Part>(); }
    public static final class Time { public String group, item, method, time, note; String key; }
    public static final class Explain { public String name, text; public final Map<String, String> names = new LinkedHashMap<String, String>(); }

    public final List<Equiv> equivs = new ArrayList<Equiv>();
    public final List<Rescue> rescues = new ArrayList<Rescue>();
    public final List<Blend> blends = new ArrayList<Blend>();
    public final List<Time> times = new ArrayList<Time>();
    public final List<Explain> explains = new ArrayList<Explain>();

    private static String[] cols(String line, int n) {
        String[] out = new String[n];
        String[] p = line.split("\\|", -1);
        for (int i = 0; i < n; i++) out[i] = i < p.length ? p[i].trim() : "";
        return out;
    }

    public static Helpers20 parse(String text) {
        Helpers20 h = new Helpers20();
        String sec = "";
        Rescue r = null; Blend b = null; Explain e = null;
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[") && line.endsWith("]")) { sec = line.substring(1, line.length() - 1); continue; }
            if (sec.equals("equiv")) {
                String[] c = cols(line, 4);
                Equiv q = new Equiv(); q.group = c[0]; q.amount = c[1]; q.equals = c[2]; q.note = c[3]; q.key = (q.group + " " + q.amount + " " + q.equals + " " + q.note).toLowerCase(Locale.ROOT); h.equivs.add(q);
            } else if (sec.equals("times")) {
                String[] c = cols(line, 5);
                Time t = new Time(); t.group = c[0]; t.item = c[1]; t.method = c[2]; t.time = c[3]; t.note = c[4]; t.key = (t.group + " " + t.item + " " + t.method + " " + t.note).toLowerCase(Locale.ROOT); h.times.add(t);
            } else if (sec.equals("rescue")) {
                if (line.startsWith("@")) { String[] c = cols(line.substring(1), 2); r = new Rescue(); r.problem = c[0]; r.dishes = c[1]; h.rescues.add(r); }
                else if (r != null && line.startsWith("-")) r.fixes.add(line.substring(1).trim());
                else if (r != null && line.startsWith("!")) r.prevent = line.substring(1).trim();
            } else if (sec.equals("blends")) {
                if (line.startsWith("@")) { String[] c = cols(line.substring(1), 3); b = new Blend(); b.name = c[0]; b.country = c[1]; b.use = c[2]; h.blends.add(b); }
                else if (b != null && line.startsWith("-")) {
                    String[] c = cols(line.substring(1), 3);
                    Part p = new Part(); p.partsText = c[0]; p.parts = parseParts(c[0]); p.spice = c[1]; p.flag = c[2]; b.parts.add(p);
                }
            } else if (sec.equals("explain")) {
                if (line.startsWith("@")) { String[] c = cols(line.substring(1), 2); e = new Explain(); e.name = c[0]; e.text = c[1]; h.explains.add(e); }
                else if (e != null && line.startsWith("=")) { String[] c = cols(line.substring(1), 2); e.names.put(c[0].toLowerCase(Locale.ROOT), c[1]); }
            }
        }
        return h;
    }

    /** "1 1/2" -> 1.5, "1/4" -> 0.25, "2" -> 2. */
    public static double parseParts(String s) {
        double v = 0;
        for (String w : s.trim().split("\\s+")) {
            int sl = w.indexOf('/');
            try {
                if (sl > 0) v += Double.parseDouble(w.substring(0, sl)) / Double.parseDouble(w.substring(sl + 1));
                else if (!w.isEmpty()) v += Double.parseDouble(w);
            } catch (NumberFormatException ex) { /* ignore */ }
        }
        return v;
    }

    // ---------------- search ----------------
    /** True when every word of the query appears in the text (case-insensitive). Empty query matches everything. */
    public static boolean matches(String hay, String query) {
        if (query == null) return true;
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        String h = hay.toLowerCase(Locale.ROOT);
        for (String w : q.split("\\s+")) if (!h.contains(w)) return false;
        return true;
    }

    private static String[] words(String q) {
        String t = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return t.isEmpty() ? new String[0] : t.split("\\s+");
    }

    private static boolean hasAll(String lowerHay, String[] ws) {
        for (String w : ws) if (lowerHay.indexOf(w) < 0) return false;
        return true;
    }

    public List<Equiv> findEquivs(String q) {
        List<Equiv> out = new ArrayList<Equiv>();
        String[] ws = words(q);
        for (Equiv x : equivs) if (hasAll(x.key, ws)) out.add(x);
        return out;
    }

    public List<Time> findTimes(String q) {
        List<Time> out = new ArrayList<Time>();
        String[] ws = words(q);
        for (Time x : times) if (hasAll(x.key, ws)) out.add(x);
        return out;
    }

    public List<Rescue> findRescues(String q) {
        List<Rescue> out = new ArrayList<Rescue>();
        for (Rescue x : rescues) if (matches(x.problem + " " + x.dishes, q)) out.add(x);
        if (out.isEmpty() && q != null && !q.trim().isEmpty())
            for (Rescue x : rescues) { StringBuilder sb = new StringBuilder(); for (String f : x.fixes) sb.append(f).append(' '); if (matches(sb.toString(), q)) out.add(x); }
        return out;
    }

    public List<Explain> findExplains(String q) {
        List<Explain> out = new ArrayList<Explain>();
        for (Explain x : explains) {
            StringBuilder sb = new StringBuilder(x.name).append(' ');
            for (String v : x.names.values()) sb.append(v).append(' ');
            if (matches(sb.toString(), q)) out.add(x);
        }
        if (out.isEmpty() && q != null && !q.trim().isEmpty()) for (Explain x : explains) if (matches(x.text, q)) out.add(x);
        return out;
    }

    /** Groups in first-seen order. */
    public static List<String> groupsOf(List<String> all) {
        List<String> g = new ArrayList<String>();
        for (String s : all) if (!g.contains(s)) g.add(s);
        return g;
    }

    // ---------------- local names ----------------
    /** Local name for the app region (us, ca, uk, eu, anz, other), with sensible fallbacks; null when none. */
    public static String localName(Explain e, String region) {
        String r = region == null ? "" : region;
        String v = e.names.get(r);
        if (v == null && r.equals("ca")) v = e.names.get("us");
        if (v == null && r.equals("eu")) v = e.names.get("uk");
        return v;
    }

    public static String regionLabel(String code) {
        if (code.equals("us")) return "US";
        if (code.equals("uk")) return "UK & Ireland";
        if (code.equals("ca")) return "Canada";
        if (code.equals("anz")) return "Australia & NZ";
        if (code.equals("eu")) return "Europe";
        if (code.equals("in")) return "India";
        return "Also";
    }

    // ---------------- spice blend builder ----------------
    /**
     * Amounts for a batch of {@code totalTsp} teaspoons. {@code parts} may be the user's adjusted parts (null = recipe).
     * noSalt drops salt items; mild halves "heat" items. Returns rows {amount text, spice, flag}. Zero-part rows are left out.
     */
    public static List<String[]> build(Blend b, double[] parts, double totalTsp, boolean noSalt, boolean mild) {
        int n = b.parts.size();
        double[] w = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            Part p = b.parts.get(i);
            double v = parts != null && i < parts.length ? parts[i] : p.parts;
            if (noSalt && "salt".equals(p.flag)) v = 0;
            if (mild && "heat".equals(p.flag)) v = v / 2;
            w[i] = Math.max(0, v);
            sum += w[i];
        }
        List<String[]> out = new ArrayList<String[]>();
        if (sum <= 0) return out;
        for (int i = 0; i < n; i++) {
            if (w[i] <= 0) continue;
            Part p = b.parts.get(i);
            out.add(new String[]{spoons(w[i] / sum * totalTsp), p.spice, p.flag});
        }
        return out;
    }

    /** Teaspoons to friendly text: nearest 1/8 tsp, tablespoons from 3 tsp, "pinch" below 1/16. */
    public static String spoons(double tsp) {
        if (tsp < 1.0 / 16) return "pinch";
        long eighths = Math.round(tsp * 8);
        if (eighths == 0) eighths = 1;
        if (eighths >= 24) {
            long tb = eighths / 24, rest = eighths % 24;
            String s = tb + " tbsp";
            return rest == 0 ? s : s + " + " + frac(rest) + " tsp";
        }
        return frac(eighths) + " tsp";
    }

    private static String frac(long eighths) {
        long whole = eighths / 8, r = eighths % 8;
        String f = "";
        if (r != 0) { long g = gcd(r, 8); f = (r / g) + "/" + (8 / g); }
        if (whole == 0) return f;
        return f.isEmpty() ? String.valueOf(whole) : whole + " " + f;
    }

    private static long gcd(long a, long b) { return b == 0 ? a : gcd(b, a % b); }

    /** Plain text of a built blend (for copy/share). */
    public static String blendText(Blend b, List<String[]> rows, double totalTsp) {
        StringBuilder sb = new StringBuilder(b.name).append(" (").append(spoons(totalTsp)).append(")\n");
        for (String[] r : rows) sb.append("- ").append(r[0]).append(' ').append(r[1]).append('\n');
        sb.append("Use: ").append(b.use);
        return sb.toString();
    }

    public List<String> equivGroups() { List<String> a = new ArrayList<String>(); for (Equiv x : equivs) a.add(x.group); return groupsOf(a); }
    public List<String> timeGroups() { List<String> a = new ArrayList<String>(); for (Time x : times) a.add(x.group); return groupsOf(a); }
    public List<String> blendNames() { List<String> a = new ArrayList<String>(); for (Blend x : blends) a.add(x.name); return Collections.unmodifiableList(a); }
}
