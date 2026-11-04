package org.pantryrecipes.app.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formatting, scaling and parsing of ingredient quantities. */
public final class Quantity {
    private Quantity() { }

    private static final Pattern LEAD = Pattern.compile(
            "^\\s*(\\d+\\s+\\d+\\s*/\\s*\\d+|\\d+\\s*[\\u00bc\\u00bd\\u00be\\u2153\\u2154]|\\d+\\s*/\\s*\\d+|\\d+(?:[.,]\\d+)?|[\\u00bc\\u00bd\\u00be\\u2153\\u2154])\\s*(?:-\\s*\\d+(?:[.,]\\d+)?\\s*)?(.*)$");

    /** Parsed quantity: amount (NaN if none) and the remaining unit text. */
    public static final class Parsed {
        public final double amount;
        public final String unit;
        Parsed(double a, String u) { amount = a; unit = u; }
    }

    /** Parses free text like "1 1/2 cups", "200g", "½ tsp", "to taste". */
    public static Parsed parse(String text) {
        if (text == null) return new Parsed(Double.NaN, "");
        String t = text.trim();
        Matcher m = LEAD.matcher(t);
        if (!m.matches()) return new Parsed(Double.NaN, t);
        double a = parseNumber(m.group(1));
        if (Double.isNaN(a) || a <= 0) return new Parsed(Double.NaN, t);
        return new Parsed(a, m.group(2).trim());
    }

    private static double vulgar(char c) {
        switch (c) {
            case '\u00bc': return 0.25;
            case '\u00bd': return 0.5;
            case '\u00be': return 0.75;
            case '\u2153': return 1.0 / 3;
            case '\u2154': return 2.0 / 3;
            default: return Double.NaN;
        }
    }

    static double parseNumber(String s) {
        s = s.trim();
        try {
            if (s.length() == 1 && !Character.isDigit(s.charAt(0))) return vulgar(s.charAt(0));
            char last = s.charAt(s.length() - 1);
            if (!Character.isDigit(last)) {
                return Double.parseDouble(s.substring(0, s.length() - 1).trim()) + vulgar(last);
            }
            if (s.contains("/")) {
                String[] sp = s.split("\\s+", 2);
                double whole = 0;
                String frac = s;
                if (sp.length == 2 && !sp[0].contains("/")) { whole = Double.parseDouble(sp[0]); frac = sp[1]; }
                String[] nd = frac.replace(" ", "").split("/");
                double d = Double.parseDouble(nd[1]);
                if (d == 0) return Double.NaN;
                return whole + Double.parseDouble(nd[0]) / d;
            }
            return Double.parseDouble(s.replace(',', '.'));
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    /** Human friendly number: fractions for small kitchen units, rounded for g/ml. */
    public static String number(double v, String unit) {
        if (Double.isNaN(v)) return "";
        String u = unit == null ? "" : unit.toLowerCase();
        boolean metric = u.equals("g") || u.equals("ml") || u.startsWith("gram") || u.equals("kg") || u.equals("l");
        if (metric && v >= 10) {
            double r = v >= 100 ? Math.round(v / 5.0) * 5.0 : Math.round(v);
            return String.valueOf((long) r);
        }
        if (v >= 10) return String.valueOf(Math.round(v));
        long whole = (long) Math.floor(v);
        double frac = v - whole;
        String[] glyph = {"", "\u00bc", "\u2153", "\u00bd", "\u2154", "\u00be", ""};
        double[] val = {0, 0.25, 1.0 / 3, 0.5, 2.0 / 3, 0.75, 1};
        int best = 0;
        double bd = 9;
        for (int k = 0; k < val.length; k++) {
            double d = Math.abs(frac - val[k]);
            if (d < bd) { bd = d; best = k; }
        }
        if (bd > 0.07 && v < 3) {
            // not close to a nice fraction: one decimal
            double r = Math.round(v * 10) / 10.0;
            return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
        }
        if (best == 6) { whole += 1; best = 0; }
        if (whole == 0 && best == 0) return "\u00bc"; // never show 0 for a real amount
        return (whole > 0 ? String.valueOf(whole) : "") + glyph[best];
    }

    private static final java.util.Set<String> PLURALS = new java.util.HashSet<String>(java.util.Arrays.asList(
            "cup", "can", "clove", "slice", "sprig", "bunch", "handful", "pinch", "stalk", "tin", "jar", "head",
            "fillet", "rasher", "sheet", "piece", "packet", "stick", "leaf_"));

    public static String format(double amount, String unit, double scale) {
        if (Double.isNaN(amount)) return unit == null ? "" : unit;
        String n = number(amount * scale, unit);
        if (unit == null || unit.isEmpty()) return n;
        double v = amount * scale;
        String u = unit;
        if (v > 1.01 && PLURALS.contains(u)) u = (u.endsWith("ch") || u.endsWith("sh")) ? u + "es" : u + "s";
        return n + " " + u;
    }
}
