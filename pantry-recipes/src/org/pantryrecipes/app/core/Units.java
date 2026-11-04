package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v1.7 measurement units: how an ingredient amount is shown under the "Measurement units" setting
 * (recipe's own, US, Metric, or weights for dry baking ingredients), plus the Conversions screen maths.
 * Pure functions with simple rounding rules, mirrored exactly in js/core17.js (checked by test vectors).
 *
 * Rounding: grams under 10 to 1 g, flours to 5 g, 100 g and up to 5 g, otherwise 1 g; millilitres the
 * same; teaspoons in ¼ steps (⅛ tsp and "pinch" below that), tablespoons in ½ steps, cups in ¼ and ⅓
 * steps; ounces in ¼ oz under 4 oz, ½ oz under 16 oz.
 */
public final class Units {
    private Units() { }

    public static final String ORIGINAL = "original", US = "us", METRIC = "metric", WEIGHTS = "weights";
    public static final String[] MODES = {ORIGINAL, US, METRIC, WEIGHTS};
    /** One US cup in ml (the US "legal" cup used on nutrition labels; Amounts uses the same). */
    public static final double CUP_ML = 240;

    public static String modeLabel(String mode) {
        if (US.equals(mode)) return "US (cups, oz, \u00b0F)";
        if (METRIC.equals(mode)) return "Metric (g, ml, \u00b0C)";
        if (WEIGHTS.equals(mode)) return "Weights for dry baking ingredients (grams)";
        return "Recipe\u2019s own units";
    }

    /** Display preferences. The default is weights for dry baking ingredients, in grams, with cups shown too. */
    public static final class Prefs {
        public String mode = WEIGHTS;
        public String weightUnit = "g";
        public boolean showCups = true;
        public Prefs() { }
        public Prefs(String mode, String weightUnit, boolean showCups) {
            this.mode = validMode(mode); this.weightUnit = "oz".equals(weightUnit) ? "oz" : "g"; this.showCups = showCups;
        }
    }

    public static String validMode(String m) {
        for (String x : MODES) if (x.equals(m)) return x;
        return WEIGHTS;
    }

    // ---------------------------------------------------------------- densities
    public static final class Density {
        public final String name, key, cls;
        public final double gPerCup;
        Density(String name, String key, double g, String cls) { this.name = name; this.key = key; this.gPerCup = g; this.cls = cls; }
        /** Dry baking ingredient: shown in grams by the weights setting. */
        public boolean dryBaking() { return "flour".equals(cls) || "baking".equals(cls); }
    }

    public static final class Densities {
        public final List<Density> list = new ArrayList<Density>();
        public final Map<String, Density> byKey = new LinkedHashMap<String, Density>();
        public final List<String> problems = new ArrayList<String>();

        public Density forKey(String key) {
            if (key == null || key.isEmpty()) return null;
            Density d = byKey.get(key);
            if (d == null) {
                String f = Normalizer.family(key);
                if (f != null) d = byKey.get(f);
            }
            return d;
        }
    }

    public static final String[] CLASSES = {"flour", "baking", "spice", "fat", "liquid", "dairy", "grain", "produce", "other"};

    public static Densities parseDensities(String text) {
        Densities d = new Densities();
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\t");
            if (p.length != 3) { d.problems.add("fields: " + line); continue; }
            double g;
            try { g = Double.parseDouble(p[1].trim()); } catch (NumberFormatException e) { d.problems.add("number: " + line); continue; }
            String cls = p[2].trim();
            boolean ok = false;
            for (String c : CLASSES) if (c.equals(cls)) ok = true;
            if (!ok || g <= 0 || g > 400) { d.problems.add("value: " + line); continue; }
            String key = Normalizer.normalize(p[0].trim());
            if (key.isEmpty()) { d.problems.add("key: " + line); continue; }
            if (d.byKey.containsKey(key)) { d.problems.add("duplicate key " + key + ": " + line); continue; }
            Density x = new Density(p[0].trim(), key, g, cls);
            d.list.add(x);
            d.byKey.put(key, x);
        }
        return d;
    }

    // ---------------------------------------------------------------- number formatting
    private static final String[] GLYPH8 = {"", "\u215b", "\u00bc", "\u215c", "\u00bd", "\u215d", "\u00be", "\u215e"};

    /** v rounded to a multiple of step (1/8, 1/4, 1/2 or 1) as "1½"; never "0" for a positive v. */
    public static String frac(double v, double step) {
        long units = Math.round(v / step);
        if (units <= 0) units = 1;
        long eighths = Math.round(units * step * 8);
        long whole = eighths / 8;
        int e = (int) (eighths % 8);
        return (whole > 0 ? String.valueOf(whole) : "") + GLYPH8[e];
    }

    /** Whole number text of a rounded value. */
    private static String whole(double v) { return String.valueOf(Math.round(v)); }

    /** Two-decimal text without trailing zeros: 1.25, 1.5, 2. */
    static String dec2(double v) {
        if (v < 0 && Math.round(-v * 100) > 0) return "-" + dec2(-v);
        long c = Math.round(v * 100);
        if (c < 0) c = 0;
        long w = c / 100, f = c % 100;
        if (f == 0) return String.valueOf(w);
        if (f % 10 == 0) return w + "." + (f / 10);
        return w + "." + (f < 10 ? "0" : "") + f;
    }

    /** Grams: under 10 to 1 g (at least 1), flours from 20 g to 5 g, 100 g and up to 5 g, else 1 g. */
    public static String grams(double g, boolean flour) {
        double r;
        if (g < 10) r = Math.max(1, Math.round(g));
        else if (g >= 100 || (flour && g >= 20)) r = Math.round(g / 5.0) * 5.0;
        else r = Math.round(g);
        return whole(r) + " g";
    }

    /** Metric mass for the Metric setting: kilograms from 1000 g (to 0.05 kg), else grams. */
    public static String metricMass(double g, boolean flour) {
        if (g >= 1000) return dec2(Math.round(g / 50.0) * 0.05) + " kg";
        return grams(g, flour);
    }

    /**
     * Ounces: v1.8 finer small amounts – ⅛ oz under 1 oz, and grams in brackets under ⅛ oz (about 3.5 g),
     * where ounces cannot express the amount; ¼ oz under 4 oz, ½ oz under 16 oz, whole ounces above.
     */
    public static String ounces(double oz) {
        if (oz < 0.09375) return "under \u215b oz (" + grams(oz * 28.3495, false) + ")";
        if (oz < 1) return frac(oz, 0.125) + " oz";
        if (oz < 4) return frac(oz, 0.25) + " oz";
        if (oz < 16) return frac(oz, 0.5) + " oz";
        return whole(oz) + " oz";
    }

    /** US mass: ounces under a pound, else pounds in ¼ steps. */
    public static String usMass(double g) {
        double oz = g / 28.3495;
        if (oz < 16) return ounces(oz);
        return frac(oz / 16, 0.25) + " lb";
    }

    /** Millilitres: under 10 to 1 ml, 100 ml and up to 5 ml (litres from 1000 ml, to 0.05 l). */
    public static String millilitres(double ml) {
        if (ml >= 1000) return dec2(Math.round(ml / 50.0) * 0.05) + " l";
        double r = ml < 100 ? Math.max(1, Math.round(ml)) : Math.round(ml / 5.0) * 5.0;
        return whole(r) + " ml";
    }

    /** Teaspoons in ¼ steps, ⅛ tsp for tiny amounts, "pinch" below that. */
    public static String tsp(double v) {
        if (v < 0.1) return "pinch";
        if (v < 0.1875) return "\u215b tsp";
        return frac(v, 0.25) + " tsp";
    }

    /** Tablespoons in ½ steps; under 1 tbsp becomes teaspoons. */
    public static String tbsp(double v) {
        if (v < 0.875) return tsp(v * 3);
        return frac(v, 0.5) + " tbsp";
    }

    /** Cups with quarter/third fractions (Quantity.number rules) and a plural. */
    public static String cups(double c) {
        String n = cupNumber(c);
        return n + (singular(n) ? " cup" : " cups");
    }

    /** "1", "½", "¾" read as singular ("1 cup", "½ cup"); everything else is plural. */
    static boolean singular(String n) {
        return n.equals("1") || (n.length() == 1 && !(n.charAt(0) >= '0' && n.charAt(0) <= '9'));
    }

    private static final String[] CUP_GLYPH = {"", "\u00bc", "\u2153", "\u00bd", "\u2154", "\u00be", ""};
    private static final double[] CUP_VAL = {0, 0.25, 1.0 / 3, 0.5, 2.0 / 3, 0.75, 1};

    /** Cups to the nearest quarter or third (whole cups from 10). */
    static String cupNumber(double c) {
        if (c >= 10) return whole(c);
        long w = (long) Math.floor(c);
        double f = c - w;
        int best = 0;
        double bd = 9;
        for (int k = 0; k < CUP_VAL.length; k++) {
            double dd = Math.abs(f - CUP_VAL[k]);
            if (dd < bd - 1e-9) { bd = dd; best = k; }
        }
        if (best == 6) { w++; best = 0; }
        if (w == 0 && best == 0) return "\u00bc";
        return (w > 0 ? String.valueOf(w) : "") + CUP_GLYPH[best];
    }

    /** A US volume from ml: cups from ¼ cup, then tablespoons from ½ tbsp, then teaspoons. */
    public static String usVolume(double ml) {
        if (ml >= CUP_ML / 4 - 4) return cups(ml / CUP_ML);
        if (ml >= 7.5) return tbsp(ml / 15);
        return tsp(ml / 5);
    }

    private static boolean spoon(String c) { return c.equals("tsp") || c.equals("tbsp"); }

    private static String spoonText(double v, String c) { return c.equals("tsp") ? tsp(v) : tbsp(v); }

    private static String weight(double g, Prefs p, boolean flour) {
        return "oz".equals(p.weightUnit) ? ounces(g / 28.3495) : grams(g, flour);
    }

    // ---------------------------------------------------------------- the display rule
    /**
     * The quantity text for one ingredient line under the given preferences. {@code key} is the normalized
     * ingredient key, used to look up a density. Amounts without a number keep their text.
     */
    public static String display(String key, double amount, String unit, double scale, Prefs p, Densities ds) {
        if (Double.isNaN(amount)) return unit == null ? "" : unit;
        if (p == null || ORIGINAL.equals(p.mode)) return Quantity.format(amount, unit, scale);
        double v = amount * scale;
        String c = Amounts.canon(unit);
        String dim = Amounts.dimension(c);
        Density d = ds == null ? null : ds.forKey(key);
        boolean flour = d != null && "flour".equals(d.cls);
        boolean dry = d != null && d.dryBaking();
        if (c.equals("cm") || c.equals("inch") || c.equals("in")) {
            if (US.equals(p.mode) && c.equals("cm")) return frac(v / 2.54, 0.5) + " in";
            if (METRIC.equals(p.mode) && !c.equals("cm")) return whole(v * 2.54) + " cm";
            return Quantity.format(amount, unit, scale);
        }
        if (dim.equals("mass")) {
            double g = Amounts.convert(v, c, "g");
            if (US.equals(p.mode)) {
                if (d != null && !"produce".equals(d.cls) && !"other".equals(d.cls)) return usVolume(g / d.gPerCup * CUP_ML);
                return usMass(g);
            }
            if (METRIC.equals(p.mode)) return metricMass(g, flour);
            // weights
            String w = weight(g, p, flour);
            if (dry && p.showCups) return w + " (" + usVolume(g / d.gPerCup * CUP_ML) + ")";
            return w;
        }
        if (dim.equals("volume")) {
            double ml = Amounts.convert(v, c, "ml");
            boolean sp = spoon(c);
            if (US.equals(p.mode)) return sp ? spoonText(v, c) : usVolume(ml);
            if (METRIC.equals(p.mode)) {
                if (sp) return spoonText(v, c);
                if (d != null && !"liquid".equals(d.cls) && !"spice".equals(d.cls)) return metricMass(ml / CUP_ML * d.gPerCup, flour);
                return millilitres(ml);
            }
            // weights: dry baking ingredients by weight (teaspoon amounts stay in spoons), everything else as written
            if (dry && !c.equals("tsp")) {
                double g = ml / CUP_ML * d.gPerCup;
                String w = weight(g, p, flour);
                if (p.showCups) return w + " (" + (sp ? spoonText(v, c) : usVolume(ml)) + ")";
                return w;
            }
            if (sp) return spoonText(v, c);
            return Quantity.format(amount, unit, scale);
        }
        return Quantity.format(amount, unit, scale);
    }

    /** Ingredient convenience wrapper. */
    public static String display(Ingredient i, double scale, Prefs p, Densities ds) {
        return display(i.key, i.amount, i.unit, scale, p, ds);
    }

    // ---------------------------------------------------------------- temperatures in steps
    public static double cToF(double c) { return c * 9 / 5 + 32; }
    public static double fToC(double f) { return (f - 32) * 5 / 9; }

    /** Oven-style rounding: °F to the nearest 25 from 250 °F, else to 1; °C to the nearest 10 from 120 °C, else to 1. */
    /** Oven dial °F: to 25 °F from 245 °F (v1.8: 120 °C reads 250 °F, not 248 °F), else whole degrees. */
    public static long roundF(double f) { return f >= 245 ? Math.round(f / 25.0) * 25 : Math.round(f); }
    public static long roundC(double c) { return c >= 120 ? Math.round(c / 10.0) * 10 : Math.round(c); }

    /** v1.8: also negative temperatures ("-18 °C") and ranges ("170-180 °C", "-20 to -18 °C"). Group 1 is the character before. */
    private static final java.util.regex.Pattern TEMP = java.util.regex.Pattern.compile(
            "(^|[^\\w.\\-\u2212])([-\u2212]?\\d{1,3})(?:( ?(?:-|\u2013|to) ?)([-\u2212]?\\d{1,3}))? ?\u00b0 ?([CF])(?![A-Za-z])( ?\\()?");
    private static int tempInt(String s) { return Integer.parseInt(s.replace('\u2212', '-')); }
    private static long round5(double v) { return Math.round(v / 5.0) * 5; }
    /** "350", or a range "340\u2013355" / "-20 to -10"; a range that oven rounding would collapse is rounded to 5 degrees instead. */
    private static String stepRange(double a, double b, boolean range, boolean toF) {
        long ra = toF ? roundF(a) : roundC(a);
        if (!range) return String.valueOf(ra);
        long rb = toF ? roundF(b) : roundC(b);
        if (ra == rb && a != b) { ra = round5(a); rb = round5(b); }
        if (ra == rb) return String.valueOf(ra);
        return ra + (ra < 0 || rb < 0 ? " to " : "\u2013") + rb;
    }
    private static final java.util.regex.Pattern CM = java.util.regex.Pattern.compile("(\\d{1,3}) ?cm(?![A-Za-z])( ?\\()?");

    /**
     * Adds the other temperature scale to a step when it gives only one: "200°C" becomes "200°C (400°F)" in the
     * US setting, "400°F" becomes "400°F (200°C)" in the Metric and weights settings; "23 cm" gains "(9 in)" in
     * US. A temperature already followed by "(" is left alone.
     */
    public static String stepText(String text, Prefs p) {
        if (text == null || p == null || ORIGINAL.equals(p.mode)) return text;
        boolean us = US.equals(p.mode);
        StringBuffer sb = new StringBuffer();
        java.util.regex.Matcher m = TEMP.matcher(text);
        while (m.find()) {
            String rep = m.group();
            if (m.group(6) == null) {
                int lo = tempInt(m.group(2));
                boolean range = m.group(4) != null;
                int hi = range ? tempInt(m.group(4)) : lo;
                if (us && m.group(5).equals("C")) rep = rep + " (" + stepRange(cToF(lo), cToF(hi), range, true) + "\u00b0F)";
                else if (!us && m.group(5).equals("F")) rep = rep + " (" + stepRange(fToC(lo), fToC(hi), range, false) + "\u00b0C)";
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        if (!us) return sb.toString();
        String t = sb.toString();
        sb = new StringBuffer();
        m = CM.matcher(t);
        while (m.find()) {
            String rep = m.group();
            if (m.group(2) == null) rep = rep + " (" + frac(Integer.parseInt(m.group(1)) / 2.54, 0.5) + " in)";
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ---------------------------------------------------------------- conversions screen
    /** Oven table rows: {°C, °F, gas mark, fan °C, description}. Approximate, like any oven chart. */
    public static final String[][] OVEN = {
            {"120", "250", "\u00bd", "100", "Very low"}, {"140", "275", "1", "120", "Very low"}, {"150", "300", "2", "130", "Low"},
            {"160", "325", "3", "140", "Moderately low"}, {"180", "350", "4", "160", "Moderate"}, {"190", "375", "5", "170", "Moderately hot"},
            {"200", "400", "6", "180", "Hot"}, {"220", "425", "7", "200", "Hot"}, {"230", "450", "8", "210", "Very hot"},
            {"240", "475", "9", "220", "Very hot"}};

    /** Gas mark for a °C temperature from the oven table (nearest row), or "" outside 110–250 °C. */
    public static String gasMark(double c) {
        if (c < 110 || c > 250) return "";
        String best = "";
        double bd = 1e9;
        for (String[] r : OVEN) {
            double d = Math.abs(Double.parseDouble(r[0]) - c);
            if (d < bd) { bd = d; best = r[2]; }
        }
        return best;
    }

    /** The oven table row for a gas mark, or null. */
    static String[] ovenRow(String mark) {
        String m = mark == null ? "" : mark.trim().replace("1/2", "\u00bd");
        for (String[] r : OVEN) if (r[2].equals(m)) return r;
        return null;
    }

    private static final java.util.regex.Pattern TEMP_IN = java.util.regex.Pattern.compile(
            "^\\s*([-+]?\\d+(?:[.,]\\d+)?)\\s*(?:\u00b0?\\s*[cf])?\\s*(?:(?:-|\u2013|\u2014|to)\\s*([-+]?\\d+(?:[.,]\\d+)?)\\s*(?:\u00b0?\\s*[cf])?\\s*)?$");

    /**
     * v1.8: a temperature or a range as typed, negative numbers included: "-18", "−18", "180-200", "-20 to -5",
     * "-20 – -5", "350°F". Returns {low, high} (equal for one value) or null.
     */
    public static double[] parseTemp(String value) {
        if (value == null) return null;
        String t = value.toLowerCase(java.util.Locale.ROOT).replace('\u2212', '-');
        java.util.regex.Matcher m = TEMP_IN.matcher(t);
        if (!m.matches()) return null;
        double a = Double.parseDouble(m.group(1).replace(',', '.'));
        double b = m.group(2) == null ? a : Double.parseDouble(m.group(2).replace(',', '.'));
        return a <= b ? new double[]{a, b} : new double[]{b, a};
    }

    private static String rangeText(long a, long b, String sep) { return a == b ? String.valueOf(a) : a + sep + b; }

    /**
     * "180–200 °C = 356–392 °F (oven dial ~350–400 °F) · gas 4–6 · fan ~160–180 °C" for a range. v1.8: ranges with a
     * negative end read "-20 to -15 °C = -4 to 5 °F"; the oven dial / fan parts only appear for oven temperatures.
     */
    static String tempRange(double lo, double hi, String scale) {
        boolean f = "F".equals(scale);
        double cLo = f ? fToC(lo) : lo, cHi = f ? fToC(hi) : hi;
        String gLo = gasMark(cLo), gHi = gasMark(cHi);
        String gas = gLo.isEmpty() || gHi.isEmpty() ? "" : " \u00b7 gas " + (gLo.equals(gHi) ? gLo : gLo + "\u2013" + gHi);
        double oLo = f ? cToF(cLo) : cToF(lo), oHi = f ? cToF(cHi) : cToF(hi);
        String sep = lo < 0 || hi < 0 || (f ? cLo < 0 : oLo < 0) ? " to " : "\u2013";
        boolean oven = cLo >= 100;
        if (f) {
            return dec2(lo) + sep + dec2(hi) + " \u00b0F = " + rangeText(Math.round(cLo), Math.round(cHi), sep) + " \u00b0C"
                    + (oven ? " (oven dial ~" + rangeText(roundC(cLo), roundC(cHi), sep) + " \u00b0C)" : "") + gas
                    + (cLo >= 120 ? " \u00b7 fan ~" + rangeText(roundC(cLo) - 20, roundC(cHi) - 20, sep) + " \u00b0C" : "");
        }
        return dec2(lo) + sep + dec2(hi) + " \u00b0C = " + rangeText(Math.round(oLo), Math.round(oHi), sep) + " \u00b0F"
                + (oven ? " (oven dial ~" + rangeText(roundF(oLo), roundF(oHi), sep) + " \u00b0F)" : "") + gas
                + (lo >= 120 ? " \u00b7 fan ~" + rangeText(Math.round(lo - 20), Math.round(hi - 20), sep) + " \u00b0C" : "");
    }

    /** °C for a gas mark ("½", "1".."9"), or NaN. */
    public static double gasToC(String mark) {
        String m = mark == null ? "" : mark.trim().replace("1/2", "\u00bd");
        for (String[] r : OVEN) if (r[2].equals(m)) return Double.parseDouble(r[0]);
        return Double.NaN;
    }

    /**
     * Parses what a person types: "2", "1.5", "1,5", "3/4", "1 1/2", "1½", "½", and ranges "1-2", "1 – 2",
     * "1 to 2". Returns {low, high} (equal for a single number) or null.
     */
    public static double[] parseAmount(String text) {
        if (text == null) return null;
        String t = text.trim().toLowerCase(java.util.Locale.ROOT).replace('\u2013', '-').replace('\u2014', '-');
        if (t.isEmpty()) return null;
        int cut = t.indexOf(" to ");
        if (cut < 0) {
            int dash = t.indexOf('-', 1);
            if (dash > 0) cut = dash;
        }
        if (cut > 0) {
            int skip = t.charAt(cut) == '-' ? 1 : 4;
            double a = one(t.substring(0, cut)), b = one(t.substring(cut + skip));
            if (Double.isNaN(a) || Double.isNaN(b) || b < a) return null;
            return new double[]{a, b};
        }
        double a = one(t);
        return Double.isNaN(a) ? null : new double[]{a, a};
    }

    private static double one(String s) {
        s = s.trim();
        if (s.isEmpty()) return Double.NaN;
        if (!s.matches("[0-9.,/ \u00bc\u00bd\u00be\u2153\u2154\u215b]+")) return Double.NaN;
        double v = Quantity.parseNumber(s.replaceAll(" +", " "));
        if (s.matches("[0-9]+ +[0-9]+")) return Double.NaN;
        return Double.isNaN(v) || v < 0 ? Double.NaN : v;
    }

    /** Converts between any two volume or any two mass units (Amounts units); NaN when not comparable. */
    public static double convert(double v, String from, String to) { return Amounts.convert(v, from, to); }

    /** Grams for a volume of an ingredient; NaN without a density. */
    public static double volumeToGrams(double v, String unit, Density d) {
        double ml = Amounts.convert(v, unit, "ml");
        if (d == null || Double.isNaN(ml)) return Double.NaN;
        return ml / CUP_ML * d.gPerCup;
    }

    /** Volume (in the given unit) for a mass of an ingredient; NaN without a density. */
    public static double gramsToVolume(double v, String unit, String toUnit, Density d) {
        double g = Amounts.convert(v, unit, "g");
        if (d == null || Double.isNaN(g)) return Double.NaN;
        return Amounts.convert(g / d.gPerCup * CUP_ML, "ml", toUnit);
    }

    /** Butter: 1 US stick = 113 g = 8 tbsp = ½ cup = 4 oz. Returns grams for an amount in sticks, tbsp, cups, g or oz. */
    public static double butterGrams(double v, String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase(java.util.Locale.ROOT);
        if (u.startsWith("stick")) return v * 113.4;
        double ml = Amounts.convert(v, u, "ml");
        if (!Double.isNaN(ml)) return ml / CUP_ML * 227;
        return Amounts.convert(v, u, "g");
    }

    /** "113 g · 4 oz · 1 stick · 8 tbsp · ½ cup" for an amount of butter in grams. */
    public static String butterText(double g) {
        double sticks = g / 113.4;
        String st = sticks >= 0.2 ? " \u00b7 " + frac(sticks, 0.25) + (singular(frac(sticks, 0.25)) ? " stick" : " sticks") : "";
        return grams(g, false) + " \u00b7 " + ounces(g / 28.3495) + st
                + " \u00b7 " + tbsp(g / 227 * 16) + (g / 227 >= 0.23 ? " \u00b7 " + cups(g / 227) : "");
    }

    // ---------------------------------------------------------------- converter result lines (shared with web)
    public static final String[] VOLUME_UNITS = {"tsp", "tbsp", "cup", "fl oz", "ml", "l"};
    public static final String[] MASS_UNITS = {"g", "kg", "oz", "lb"};

    /** "240 ml · 1 cup · 16 tbsp · 48 tsp · 8 fl oz" for a volume, or "" if the unit is not a volume. */
    public static String volumeLine(double v, String unit) {
        double ml = Amounts.convert(v, unit, "ml");
        if (Double.isNaN(ml) || !"volume".equals(Amounts.dimension(unit)) || ml <= 0) return "";
        return millilitres(ml) + (ml >= CUP_ML / 4 - 4 ? " \u00b7 " + cups(ml / CUP_ML) : "") + " \u00b7 " + frac(ml / 15, 0.5) + " tbsp \u00b7 "
                + frac(ml / 5, 0.25) + " tsp \u00b7 " + frac(ml / 30, 0.25) + " fl oz";
    }

    /** "500 g · 17½ oz · 1 lb" for a mass, or "" if the unit is not a mass. */
    public static String massLine(double v, String unit) {
        double g = Amounts.convert(v, unit, "g");
        if (Double.isNaN(g) || !"mass".equals(Amounts.dimension(unit)) || g <= 0) return "";
        String s = metricMass(g, false) + " \u00b7 " + ounces(g / 28.3495);
        if (g >= 453.592 / 2) s += " \u00b7 " + frac(g / 453.592, 0.25) + " lb";
        return s;
    }

    /** Temperature line for scale "C", "F" or "gas". "" when it cannot be read. */
    public static String tempLine(String value, String scale) {
        if ("gas".equals(scale)) {
            double c = gasToC(value);
            if (Double.isNaN(c)) return "";
            String[] row = ovenRow(value);
            return "Gas " + row[2] + " \u2248 " + row[0] + " \u00b0C \u00b7 " + row[1] + " \u00b0F \u00b7 fan " + row[3] + " \u00b0C";
        }
        double[] a = parseTemp(value);
        if (a == null) return "";
        if (a[1] != a[0]) return tempRange(a[0], a[1], scale);
        double v = a[0];
        if ("F".equals(scale)) {
            double c = fToC(v);
            String g = gasMark(c);
            return dec2(v) + " \u00b0F = " + Math.round(c) + " \u00b0C" + (c >= 100 ? " (oven dial ~" + roundC(c) + " \u00b0C)" : "") + (g.isEmpty() ? "" : " \u00b7 gas " + g)
                    + (c >= 120 ? " \u00b7 fan ~" + (roundC(c) - 20) + " \u00b0C" : "");
        }
        double f = cToF(v);
        String g = gasMark(v);
        return dec2(v) + " \u00b0C = " + Math.round(f) + " \u00b0F" + (v >= 100 ? " (oven dial ~" + roundF(f) + " \u00b0F)" : "") + (g.isEmpty() ? "" : " \u00b7 gas " + g)
                + (v >= 120 ? " \u00b7 fan ~" + Math.round(v - 20) + " \u00b0C" : "");
    }

    /** Ingredient converter: volume to weight or weight to volume using a density. "" without one. */
    public static String ingredientLine(double v, String unit, Density d) {
        if (d == null || v <= 0) return "";
        String dim = Amounts.dimension(unit);
        if ("volume".equals(dim)) {
            double g = volumeToGrams(v, unit, d);
            return "\u2248 " + grams(g, "flour".equals(d.cls)) + " \u00b7 " + ounces(g / 28.3495);
        }
        if ("mass".equals(dim)) {
            double g = Amounts.convert(v, unit, "g");
            double ml = g / d.gPerCup * CUP_ML;
            return "\u2248 " + usVolume(ml) + " \u00b7 " + millilitres(ml) + " by volume";
        }
        return "";
    }
}
