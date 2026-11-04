import org.pantryrecipes.app.core.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

/**
 * Reference results for the v1.7 web port (js/core17.js): measurement-unit display for every bundled ingredient
 * line in every setting, step temperature notes, cooking methods, pressure/release timer kinds, swap ratios and
 * flags, and the Conversions screen lines. Also writes js/data17.js (densities table and Instant Pot cheat sheet).
 * Usage: java -cp build V17Export <android project dir> <web dir>
 */
public class V17Export {
    static List<Object> L(Object... a) { return new ArrayList<Object>(Arrays.asList(a)); }
    static Object n(double d) { return Double.isNaN(d) ? null : (Object) d; }
    static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }
    static String sha1(String s) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        for (byte x : h) b.append(String.format("%02x", x));
        return b.toString();
    }
    static Units.Prefs[] PREFS = {new Units.Prefs("original", "g", true), new Units.Prefs("us", "g", true), new Units.Prefs("metric", "g", true),
            new Units.Prefs("weights", "g", true), new Units.Prefs("weights", "oz", true), new Units.Prefs("weights", "g", false)};

    public static void main(String[] a) throws Exception {
        File android = new File(a[0]), web = new File(a[1]);
        List<Recipe> recipes = Recipe.parseList(read(new File(android, "assets/recipes.json")));
        String densText = read(new File(android, "assets/densities.tsv"));
        Units.Densities ds = Units.parseDensities(densText);
        Map<String, Object> v = new LinkedHashMap<String, Object>();
        v.put("densityCount", ds.list.size());
        v.put("densityProblems", ds.problems);

        // ---- display: every ingredient line x 6 settings x 3 scales (full text for the first 150 recipes, a hash for the rest)
        double[] scales = {1, 2.5, 0.5};
        List<Object> disp = new ArrayList<Object>();
        int lines = 0;
        for (int ri = 0; ri < recipes.size(); ri++) {
            Recipe r = recipes.get(ri);
            StringBuilder sb = new StringBuilder();
            for (Ingredient i : r.ingredients) for (Units.Prefs p : PREFS) for (double s : scales) { sb.append(Units.display(i, s, p, ds)).append('|'); lines++; }
            sb.append('#');
            for (String st : r.steps) for (Units.Prefs p : PREFS) sb.append(Units.stepText(st, p)).append('|');
            disp.add(L(r.id, ri < 150 ? sb.toString() : sha1(sb.toString())));
        }
        v.put("display", disp);
        v.put("displayLines", lines);
        List<Object> ex = new ArrayList<Object>();
        String[] ek = {"flour", "plain flour", "bread flour", "sugar", "brown sugar", "butter", "milk", "salt", "rice", "cocoa powder", "honey", "onion", "unknown thing", ""};
        String[] eu = {"g", "kg", "oz", "lb", "cup", "cups", "tbsp", "tsp", "ml", "l", "fl oz", "", "cm", "inch", "pinch", "can"};
        double[] ea = {0.125, 0.33, 1, 1.5, 3, 20, 250, 1200, Double.NaN};
        for (String k : ek) for (String u : eu) for (double am : ea) {
            List<Object> row = L(k, u, n(am));
            for (Units.Prefs p : PREFS) row.add(Units.display(k, am, u, 1, p, ds));
            ex.add(row);
        }
        v.put("displayEdge", ex);

        // ---- methods and duration kinds
        List<Object> meth = new ArrayList<Object>();
        List<Object> kinds = new ArrayList<Object>();
        for (Recipe r : recipes) {
            meth.add(L(r.id, Methods.of(r)));
            for (String st : r.steps) {
                List<Object> ks = new ArrayList<Object>();
                for (Durations.Match m : Durations.find(st)) ks.add(L(m.start, m.end, m.seconds, m.kind));
                if (!ks.isEmpty()) kinds.add(L(st, ks));
            }
        }
        for (String st : new String[]{"Pressure cook on High for 12 minutes.", "Cook at high pressure for 8 min, then release naturally for 10 minutes.",
                "12 minutes at high pressure", "Let the pressure release naturally for about 10 minutes, then quick release.", "Natural release 15 min.",
                "Simmer 10 minutes. Pressure cook 5 min", "Bake 20 min", "Cook 3 min on low pressure", "Seal and cook 4 minutes under pressure", "10 min natural release",
                "The pot takes about 10 to 15 minutes to come up to pressure; that is not part of the cook time.", "Allow 12 minutes to reach pressure, then cook 5 minutes at high pressure."}) {
            List<Object> ks = new ArrayList<Object>();
            for (Durations.Match m : Durations.find(st)) ks.add(L(m.start, m.end, m.seconds, m.kind));
            kinds.add(L(st, ks));
        }
        v.put("methods", meth);
        v.put("kinds", kinds);

        // ---- substitutions
        Substitutions subs = Substitutions.parse(read(new File(android, "assets/substitutions.txt")));
        List<Object> sb2 = new ArrayList<Object>();
        for (Substitutions.Entry e : subs.entries) for (Substitutions.Option o : e.options)
            sb2.add(L(e.name, o.text, o.flagsLine(), Substitutions.claims(o), n(Substitutions.ratioValue(o.ratio) == null ? Double.NaN : Substitutions.ratioValue(o.ratio)),
                    o.vegetarian, o.vegan, o.dairyFree, o.glutenFree, o.allergens));
        v.put("subs", sb2);
        List<Object> rv = new ArrayList<Object>();
        for (String s : new String[]{"3/4:1", "1:2", "to taste", " to taste ", "0:1", "1", "a:b", "1:0", "1/0:1", "1:2:3", "1.5:1", "1 : 2", "1:", ":1", "2/3:1/2", "1.:1", null})
            rv.add(L(s, n(Substitutions.ratioValue(s) == null ? Double.NaN : Substitutions.ratioValue(s))));
        v.put("ratios", rv);

        // ---- conversions screen
        List<Object> pa = new ArrayList<Object>();
        for (String s : new String[]{"2", "1.5", "1,5", "3/4", "1 1/2", "1\u00bd", "\u00bd", "1-2", "1 \u2013 2", "1 to 2", "2-1", "", " ", "abc", "1 2", "-5", "1/0",
                "0", "\u215b", "2 to", "1--2", "1.2.3", "10 TO 12", "\u00be-1", "1e2", null}) {
            double[] r = Units.parseAmount(s);
            pa.add(L(s, r == null ? null : L(r[0], r[1])));
        }
        v.put("parse", pa);
        List<Object> tl = new ArrayList<Object>();
        for (String s : new String[]{"180", "350", "0", "-40", "-5.5", "100", "1e2", "250", "475", "120", "110", "260", "abc", "", "1/2", "1-2", "37.77", "1"})
            for (String sc : new String[]{"C", "F", "gas"}) tl.add(L(s, sc, Units.tempLine(s, sc)));
        for (String s : new String[]{"\u00bd", "1/2", " 4 ", "10", "9"}) tl.add(L(s, "gas", Units.tempLine(s, "gas")));
        v.put("temp", tl);
        List<Object> cv = new ArrayList<Object>();
        double[] amts = {0.1, 0.25, 1, 1.5, 3, 16, 100, 453.6, 1000, 2500, 0};
        for (double x : amts) {
            for (String u : Units.VOLUME_UNITS) cv.add(L(x, u, Units.volumeLine(x, u), Units.massLine(x, u)));
            for (String u : Units.MASS_UNITS) cv.add(L(x, u, Units.volumeLine(x, u), Units.massLine(x, u)));
        }
        v.put("lines", cv);
        List<Object> il = new ArrayList<Object>();
        for (Units.Density d : ds.list) for (double x : new double[]{1, 0.5, 100}) for (String u : new String[]{"cup", "tbsp", "g", "oz", "piece"})
            il.add(L(d.key, x, u, Units.ingredientLine(x, u, d)));
        v.put("ingredientLines", il);
        List<Object> bt = new ArrayList<Object>();
        for (double x : new double[]{0.25, 0.5, 1, 1.5, 2, 3, 8, 100, 250}) for (String u : new String[]{"stick", "sticks", "tbsp", "cup", "g", "oz", "tsp", "lb", "piece"}) {
            double g = Units.butterGrams(x, u);
            bt.add(L(x, u, n(g), Double.isNaN(g) || g <= 0 ? "" : Units.butterText(g)));
        }
        v.put("butter", bt);
        List<Object> fm = new ArrayList<Object>();
        for (double x : new double[]{0, 0.01, 0.1, 0.124, 0.125, 0.3, 0.5, 0.9, 1, 1.26, 2.6, 3.99, 9.9, 10.4, 15.9, 99, 101, 999, 1001, 1234.5, 5000})
            fm.add(L(x, Units.frac(x, 0.25), Units.frac(x, 0.5), Units.grams(x, true), Units.grams(x, false), Units.metricMass(x, true), Units.ounces(x),
                    Units.usMass(x), Units.millilitres(x), Units.tsp(x), Units.tbsp(x), Units.cups(x), Units.usVolume(x), Units.gasMark(x),
                    Units.roundF(x), Units.roundC(x)));
        v.put("format", fm);

        // ---- backups: v1.7 settings round trip, invalid values dropped, the genuine v1.6 file
        List<Object> bk = new ArrayList<Object>();
        String base = "{\"app\":\"Pantry Recipes\",\"format\":2,\"appVersion\":\"1.7\",\"pantry\":[\"Flour\"],\"settings\":";
        for (String st : new String[]{"{\"units\":\"metric\",\"weightUnit\":\"oz\",\"showCups\":false,\"methods\":[\"one-pot\",\"instant-pot\",\"bogus\",\"one-pot\"]}",
                "{\"units\":\"imperial\",\"weightUnit\":\"kg\",\"showCups\":\"yes\",\"methods\":\"oven\"}",
                "{\"units\":\"original\",\"methods\":[]}", "{\"units\":\"weights\",\"weightUnit\":\"g\",\"showCups\":true,\"methods\":[\"no-cook\",\"air-fryer\"]}"}) {
            String o = base + st + "}";
            Backup.Data back = Backup.parse(o);
            bk.add(L(o, Backup.write(back), Backup.summary(back)));
        }
        String v16 = read(new File(web, "tests/fixtures/v16-backup.json"));
        Backup.Data b16 = Backup.parse(v16);
        bk.add(L(v16, Backup.write(b16), Backup.summary(b16)));
        v.put("backups", bk);

        Files.write(new File(web, "tests/vectors17.json").toPath(), Json.write(v).getBytes(StandardCharsets.UTF_8));
        System.out.println("vectors17.json written: " + lines + " display lines");

        // ---- js/data17.js
        Map<String, Object> d = new LinkedHashMap<String, Object>();
        d.put("densities", densText);
        d.put("ipDisclaimer", Methods.IP_DISCLAIMER);
        d.put("ipTips", Arrays.asList(Methods.IP_TIPS));
        List<Object> times = new ArrayList<Object>();
        for (String[] row : Methods.IP_TIMES) times.add(Arrays.asList(row));
        d.put("ipTimes", times);
        String js = "/* Generated by tools/V17Export.java - do not edit.\n"
                + " * densities: grams per US cup (240 ml), self-authored kitchen averages (MIT); Instant Pot cheat sheet: self-authored approximate times (MIT). */\n"
                + "window.PR_DATA17 = " + Json.write(d) + ";\n";
        Files.write(new File(web, "js/data17.js").toPath(), js.getBytes(StandardCharsets.UTF_8));
        System.out.println("js/data17.js written: " + js.length() + " chars");
    }
}
