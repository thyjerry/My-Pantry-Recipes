package org.pantryrecipes.app.core;

import java.util.*;

/**
 * v2.0 Stage 3: holidays (dates, approaching list, recipe sections), fasting-day labels and the baking helpers.
 * All data comes from assets/stage3.txt (original text); the web app has a line-by-line port in js/core30.js.
 */
public final class Stage3 {
    public static final class Holiday {
        public String id, name, rule, note = "";
        public final Set<String> regions = new LinkedHashSet<String>();
        public final List<String> title = new ArrayList<String>(), ing = new ArrayList<String>(), excl = new ArrayList<String>();
        public final Set<String> cuisines = new LinkedHashSet<String>();
        public String diet = "";
    }
    public static final class Fast { public String id, label, note = ""; public final Set<String> rules = new LinkedHashSet<String>(); }
    public static final class Trouble { public String problem, bakes; public final List<String> fixes = new ArrayList<String>(); }
    public static final class Upcoming {
        public final Holiday h; public final String date; public final int days;
        Upcoming(Holiday h, String date, int days) { this.h = h; this.date = date; this.days = days; }
    }

    public final List<Holiday> holidays = new ArrayList<Holiday>();
    public final List<Fast> fasts = new ArrayList<Fast>();
    public final List<Trouble> trouble = new ArrayList<Trouble>();
    public final List<String[]> storage = new ArrayList<String[]>(), altitude = new ArrayList<String[]>(), party = new ArrayList<String[]>();

    public static final String NOTE = "Holiday dates for lunar calendars come from published tables for 2026-2030 and may differ by a day where you live. "
            + "Fasting labels are a guide worked out from ingredient names; customs vary, so check your own tradition.";

    public static Stage3 parse(String text) {
        Stage3 s = new Stage3();
        String sec = "";
        Holiday h = null; Fast f = null; Trouble t = null;
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[") && line.endsWith("]")) { sec = line.substring(1, line.length() - 1); h = null; f = null; t = null; continue; }
            if (sec.equals("holidays")) {
                if (line.startsWith("@ ")) {
                    String[] p = cells(line.substring(2));
                    h = new Holiday(); h.id = p[0]; h.name = p[1]; h.rule = p[2];
                    for (String r : p[3].split(",")) { r = r.trim(); if (!r.isEmpty() && !r.equals("-")) h.regions.add(r); }
                    s.holidays.add(h);
                } else if (h != null && line.length() > 2 && line.charAt(1) == ':') {
                    char k = line.charAt(0); String v = line.substring(2).trim();
                    if (k == 'n') { h.note = v; continue; }
                    if (k == 'd') { h.diet = v; continue; }
                    for (String w : v.split(",")) {
                        w = w.trim().toLowerCase(Locale.ROOT);
                        if (w.isEmpty()) continue;
                        List<String> into = k == 't' ? h.title : k == 'i' ? h.ing : k == 'x' ? h.excl : null;
                        if (k == 'c') h.cuisines.add(w);
                        else if (into != null && !into.contains(w)) into.add(w);
                    }
                }
            } else if (sec.equals("fasting")) {
                if (line.startsWith("@ ")) {
                    String[] p = cells(line.substring(2));
                    f = new Fast(); f.id = p[0]; f.label = p[1];
                    for (String r : p[2].split(",")) if (!r.trim().isEmpty()) f.rules.add(r.trim());
                    s.fasts.add(f);
                } else if (f != null && line.startsWith("n:")) f.note = line.substring(2).trim();
            } else if (sec.equals("bake-trouble")) {
                if (line.startsWith("@ ")) { String[] p = cells(line.substring(2)); t = new Trouble(); t.problem = p[0]; t.bakes = p.length > 1 ? p[1] : ""; s.trouble.add(t); }
                else if (t != null && line.startsWith("- ")) t.fixes.add(line.substring(2).trim());
            } else if (sec.equals("bake-storage")) s.storage.add(cells(line));
            else if (sec.equals("altitude")) s.altitude.add(cells(line));
            else if (sec.equals("party")) s.party.add(cells(line));
        }
        return s;
    }

    static String[] cells(String line) {
        String[] p = line.split("\\|", -1);
        for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
        return p;
    }

    public Holiday holiday(String id) { for (Holiday h : holidays) if (h.id.equals(id)) return h; return null; }
    public Fast fast(String id) { for (Fast f : fasts) if (f.id.equals(id)) return f; return null; }

    // ---------------- dates ----------------
    static String iso(int y, int m, int d) { return String.format(Locale.ROOT, "%04d-%02d-%02d", y, m, d); }
    static int dim(int y, int m) { return m == 2 ? ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0 ? 29 : 28) : (m == 4 || m == 6 || m == 9 || m == 11) ? 30 : 31; }

    /** Western (Gregorian) Easter Sunday. */
    public static String easter(int y) {
        int a = y % 19, b = y / 100, c = y % 100, d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31, day = (h + l - 7 * m + 114) % 31 + 1;
        return iso(y, month, day);
    }

    /** Orthodox Easter (Julian computus), as a Gregorian date; valid 1900-2099. */
    public static String orthodoxEaster(int y) {
        int a = y % 4, b = y % 7, c = y % 19, d = (19 * c + 15) % 30, e = (2 * a + 4 * b - d + 34) % 7;
        int month = (d + e + 114) / 31, day = (d + e + 114) % 31 + 1;
        return Dates.addDays(iso(y, month, day), 13);
    }

    /** The date of a holiday in a given year, or null when the rule has no date for it (tables cover 2026-2030). */
    public static String dateIn(Holiday h, int y) {
        String r = h.rule;
        try {
            if (r.startsWith("fixed:")) { String[] p = r.substring(6).split("-"); return iso(y, Integer.parseInt(p[0]), Integer.parseInt(p[1])); }
            if (r.startsWith("easter:")) return Dates.addDays(easter(y), Integer.parseInt(r.substring(7).replace("+", "")));
            if (r.startsWith("oeaster:")) return Dates.addDays(orthodoxEaster(y), Integer.parseInt(r.substring(8).replace("+", "")));
            if (r.startsWith("nth:")) {
                String[] p = r.substring(4).split(":");
                int m = Integer.parseInt(p[0]), wd = Integer.parseInt(p[1]) - 1, n = Integer.parseInt(p[2]);
                if (n > 0) {
                    String first = iso(y, m, 1);
                    int off = ((wd - Dates.dayOfWeek(first)) % 7 + 7) % 7;
                    return Dates.addDays(first, off + 7 * (n - 1));
                }
                String last = iso(y, m, dim(y, m));
                int back = ((Dates.dayOfWeek(last) - wd) % 7 + 7) % 7;
                return Dates.addDays(last, -back);
            }
            if (r.startsWith("wday:")) {
                String[] p = r.substring(5).split(":");
                String[] md = p[0].split("-");
                String from = iso(y, Integer.parseInt(md[0]), Integer.parseInt(md[1]));
                int wd = Integer.parseInt(p[1]) - 1;
                return Dates.addDays(from, ((wd - Dates.dayOfWeek(from)) % 7 + 7) % 7);
            }
            if (r.startsWith("table:")) {
                String pre = y + "-";
                for (String d : r.substring(6).split(",")) if (d.trim().startsWith(pre)) return d.trim();
                return null;
            }
        } catch (RuntimeException e) { return null; }
        return null;
    }

    /** The next date on or after today (this year or next), or null. */
    public static String next(Holiday h, String today) {
        int y = Integer.parseInt(today.substring(0, 4));
        for (int yy = y; yy <= y + 1; yy++) {
            String d = dateIn(h, yy);
            if (d != null && d.compareTo(today) >= 0) return d;
        }
        return null;
    }

    /** Whether a holiday is ticked by default for a region (null/unknown region: only the "all" ones). */
    public static boolean defaultOn(Holiday h, String region) {
        return h.regions.contains("all") || (region != null && h.regions.contains(region));
    }

    /** Ticked holidays within the next {@code horizon} days, soonest first. */
    public List<Upcoming> upcoming(Collection<String> ticked, String today, int horizon) {
        List<Upcoming> out = new ArrayList<Upcoming>();
        for (Holiday h : holidays) {
            if (ticked != null && !ticked.contains(h.id)) continue;
            String d = next(h, today);
            if (d == null) continue;
            int n = Dates.between(today, d);
            if (n <= horizon) out.add(new Upcoming(h, d, n));
        }
        Collections.sort(out, new Comparator<Upcoming>() {
            @Override public int compare(Upcoming a, Upcoming b) { return a.days != b.days ? a.days - b.days : a.h.name.compareTo(b.h.name); }
        });
        return out;
    }

    // ---------------- recipe sections ----------------
    public static boolean hasWord(String hay, String w) {
        int i = hay.indexOf(w);
        while (i >= 0) {
            boolean a = i == 0 || !Character.isLetter(hay.charAt(i - 1));
            int e = i + w.length();
            boolean b = e >= hay.length() || !Character.isLetter(hay.charAt(e)) || (hay.charAt(e) == 's' && (e + 1 >= hay.length() || !Character.isLetter(hay.charAt(e + 1))));
            if (a && b) return true;
            i = hay.indexOf(w, i + 1);
        }
        return false;
    }

    /** Relevance of a recipe for a holiday section: 0 = not in it. */
    public int score(Holiday h, Recipe r) {
        if (!h.diet.isEmpty() && !dietOk(h.diet, r)) return 0;
        for (Ingredient i : r.ingredients) {
            if (i.optional) continue;
            for (String x : h.excl) if (hasWord(i.key, x)) return 0;
        }
        String t = r.title.toLowerCase(Locale.ROOT);
        int s = 0;
        // title words are listed most specific first: the first third adds 2, the next third 1
        for (int k = 0, n = h.title.size(); k < n; k++)
            if (hasWord(t, h.title.get(k))) { s += 3 + (3 * k < n ? 2 : 3 * k < 2 * n ? 1 : 0); break; }
        if (!h.cuisines.isEmpty() && h.cuisines.contains(r.cuisine.toLowerCase(Locale.ROOT))) s += 2;
        int ing = 0;
        for (Ingredient i : r.ingredients) for (String w : h.ing) if (hasWord(i.key, w)) { ing++; break; }
        s += Math.min(2, ing);
        // diet-only holidays (e.g. Lent) still need a title or cuisine hit
        return s >= 2 ? s : 0;
    }

    public boolean dietOk(String d, Recipe r) {
        Fast f = fast(d);
        if (f != null) return fits(f, r);
        return r.tags.contains(d) || r.diets.contains(d);
    }

    /** The recipes for a holiday, best first, at most {@code max}. */
    public List<Recipe> section(Holiday h, List<Recipe> all, int max) {
        final Map<Recipe, Integer> sc = new HashMap<Recipe, Integer>();
        List<Recipe> out = new ArrayList<Recipe>();
        for (Recipe r : all) { int s = score(h, r); if (s > 0) { sc.put(r, s); out.add(r); } }
        Collections.sort(out, new Comparator<Recipe>() {
            @Override public int compare(Recipe a, Recipe b) {
                int d = sc.get(b) - sc.get(a);
                return d != 0 ? d : a.title.compareToIgnoreCase(b.title);
            }
        });
        return out.size() > max ? new ArrayList<Recipe>(out.subList(0, max)) : out;
    }

    // ---------------- fasting labels ----------------
    static final Set<String> ALLIUM = new HashSet<String>(Arrays.asList("onion", "onions", "garlic", "leek", "leeks", "shallot", "shallots",
            "chive", "chives", "scallion", "scallions"));
    static final Set<String> EGG = new HashSet<String>(Arrays.asList("egg", "eggs", "yolk", "yolks", "mayonnaise", "mayo", "meringue", "aioli"));
    static final Set<String> ROOT = new HashSet<String>(Arrays.asList("potato", "potatoes", "carrot", "carrots", "beet", "beets", "beetroot",
            "radish", "radishes", "ginger", "turnip", "turnips", "parsnip", "parsnips", "yam", "yams", "cassava", "taro", "celeriac",
            "horseradish", "rutabaga", "swede", "daikon", "jicama"));
    static final List<String> ROOT_OK = Arrays.asList("ground ginger", "dried ginger", "ginger powder", "ginger ale", "potato starch");

    static boolean anyWord(String key, Set<String> set) {
        for (String w : key.split(" ")) if (set.contains(w)) return true;
        return false;
    }

    static final int B_EGG = 1, B_ALLIUM = 2, B_HONEY = 4, B_ROOT = 8, B_GRAIN = 16, B_LEGUME = 32;
    private static final java.util.concurrent.ConcurrentHashMap<String, Integer> BITS = new java.util.concurrent.ConcurrentHashMap<String, Integer>();

    /** Fasting-rule properties of one ingredient key, memoised (a few hundred distinct keys; keeps labels cheap on the HD 8). */
    static int bits(String k) {
        Integer c = BITS.get(k);
        if (c != null) return c;
        int b = 0;
        boolean allium = anyWord(k, ALLIUM);
        if (!k.startsWith("vegan ") && anyWord(k, EGG)) b |= B_EGG;
        if (allium) b |= B_ALLIUM;
        if ((" " + k + " ").contains(" honey ")) b |= B_HONEY;
        if (allium || (anyWord(k, ROOT) && !ROOT_OK.contains(k))) b |= B_ROOT;
        if (Diets.isGrain(k)) b |= B_GRAIN;
        if (Diets.isLegume(k)) b |= B_LEGUME;
        BITS.put(k, b);
        return b;
    }

    public static boolean fits(Fast f, Recipe r) {
        Set<String> rules = f.rules;
        if (rules.contains("vegan") && !r.tags.contains(Recipe.VEGAN)) return false;
        if (rules.contains("veg") && !r.tags.contains(Recipe.VEGETARIAN)) return false;
        if (rules.contains("nomeat") && !r.tags.contains(Recipe.VEGETARIAN) && !r.diets.contains(Diets.PESCATARIAN)) return false;
        int need = 0;
        if (rules.contains("noegg")) need |= B_EGG;
        if (rules.contains("noallium")) need |= B_ALLIUM;
        if (rules.contains("nohoney")) need |= B_HONEY;
        if (rules.contains("noroot")) need |= B_ROOT;
        if (rules.contains("nograin")) need |= B_GRAIN;
        if (rules.contains("nolegume")) need |= B_LEGUME;
        if (need == 0) return true;
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty()) continue;
            if ((bits(i.key) & need) != 0) return false;
        }
        return true;
    }

    /** The ticked fasting labels a recipe fits, in data order. */
    public List<Fast> labels(Recipe r, Collection<String> ticked) {
        List<Fast> out = new ArrayList<Fast>();
        for (Fast f : fasts) if (ticked.contains(f.id) && fits(f, r)) out.add(f);
        return out;
    }

    // ---------------- baking ----------------
    /** Baker's percentages: grams of water, salt, yeast and starter for a flour weight. */
    public static double[] bakers(double flourG, double hydrationPct, double saltPct, double yeastPct, double starterPct) {
        double water = flourG * hydrationPct / 100.0, salt = flourG * saltPct / 100.0, yeast = flourG * yeastPct / 100.0, starter = flourG * starterPct / 100.0;
        return new double[]{water, salt, yeast, starter, flourG + water + salt + yeast + starter};
    }

    /** Flour weight for a target dough weight at the given percentages (inverse of {@link #bakers}). */
    public static double flourFor(double doughG, double hydrationPct, double saltPct, double yeastPct, double starterPct) {
        return doughG / (1 + (hydrationPct + saltPct + yeastPct + starterPct) / 100.0);
    }

    /** Area of a pan in cm^2: shape "round" (a = diameter), "square" (a = side), "rect" (a x b). */
    public static double panArea(String shape, double a, double b) {
        if ("round".equals(shape)) return Math.PI * a * a / 4.0;
        if ("square".equals(shape)) return a * a;
        return a * b;
    }

    /** How much to multiply a recipe by when moving it from one pan to another of the same depth. */
    public static double panFactor(double fromArea, double toArea) { return fromArea <= 0 ? 0 : toArea / fromArea; }

    /** Clock times for consecutive steps starting at {@code startMin} (minutes after midnight); returns start minute of each step plus the end. */
    public static int[] timeline(int startMin, int[] stepMins) {
        int[] out = new int[stepMins.length + 1];
        int t = startMin;
        for (int i = 0; i < stepMins.length; i++) { out[i] = t; t += Math.max(0, stepMins[i]); }
        out[stepMins.length] = t;
        return out;
    }

    /** Works backwards: the start minute so the last step ends at {@code endMin}. */
    public static int startFor(int endMin, int[] stepMins) {
        int t = endMin;
        for (int m : stepMins) t -= Math.max(0, m);
        return t;
    }

    /** Wall-clock label (24 h or 12 h) for minutes after midnight, with "+1 day" when it rolls over. */
    public static String clock(int min, boolean h24) {
        int day = min >= 0 ? min / 1440 : -((-min + 1439) / 1440); // no Math.floorDiv (API 24)
        int m = min - day * 1440, hh = m / 60, mm = m % 60;
        String t = h24 ? String.format(Locale.ROOT, "%02d:%02d", hh, mm)
                : String.format(Locale.ROOT, "%d:%02d %s", hh % 12 == 0 ? 12 : hh % 12, mm, hh < 12 ? "AM" : "PM");
        return day == 0 ? t : day > 0 ? t + " (+" + day + " day" + (day > 1 ? "s" : "") + ")" : t + " (" + day + " day" + (day < -1 ? "s" : "") + ")";
    }

    /** The altitude row for an elevation in feet, or null below 3,000 ft. */
    public String[] altitudeFor(double feet) {
        String[] best = null;
        for (String[] r : altitude) if (feet >= Double.parseDouble(r[0])) best = r;
        return best;
    }

    /** Whole items needed for a number of guests (always rounded up). */
    public static int partyNeed(String[] row, int guests) {
        double per = Double.parseDouble(row[1]);
        return (int) Math.ceil(per * guests - 1e-9);
    }

    public static double cToF(double c) { return c * 9.0 / 5.0 + 32.0; }
    public static double fToC(double f) { return (f - 32.0) * 5.0 / 9.0; }
    /** Gas mark for a Celsius temperature (UK scale), 0 when below mark 1/4. */
    public static String gasMark(double c) {
        double[] t = {110, 120, 140, 150, 160, 180, 190, 200, 220, 230, 240};
        String[] g = {"1/4", "1/2", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        if (c < 105) return "";
        int best = 0;
        for (int i = 0; i < t.length; i++) if (Math.abs(t[i] - c) < Math.abs(t[best] - c)) best = i;
        return g[best];
    }
}
