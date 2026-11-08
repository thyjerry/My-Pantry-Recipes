import org.pantryrecipes.app.core.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** v2.2 checks added as features land: variety (seeded picks), plus the vectors the web port is checked against. */
final class V22More {
    private V22More() { }
    static void check(boolean ok, String m) { RecipeTool.check(ok, m); }
    static void eq(Object e, Object g, String m) { RecipeTool.eq(e, g, m); }

    static String ids(List<Matcher.Result> l) { StringBuilder b = new StringBuilder(); for (Matcher.Result r : l) b.append(b.length() > 0 ? "," : "").append(r.recipe.id); return b.toString(); }
    static String q(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

    static void run(List<Recipe> parsed, File root) throws Exception {
        Matcher m = new Matcher(new HashSet<String>(), new ArrayList<String>());
        List<Matcher.Result> res = new ArrayList<Matcher.Result>();
        for (Recipe r : parsed) res.add(m.match(r));
        // determinism and day rotation
        int s1 = Variety.daySeed(2026, 11, 3, "picks"), s2 = Variety.daySeed(2026, 11, 4, "picks");
        eq(ids(Variety.picks(res, s1, 12)), ids(Variety.picks(res, s1, 12)), "variety: same day, same picks");
        List<Matcher.Result> rev = new ArrayList<Matcher.Result>(res); Collections.reverse(rev);
        eq(ids(Variety.picks(res, s1, 12)), ids(Variety.picks(rev, s1, 12)), "variety: input order does not matter");
        check(!ids(Variety.picks(res, s1, 12)).equals(ids(Variety.picks(res, s2, 12))), "variety: next day, new picks");
        // spread: over 60 days the first pick should be ~60 different recipes
        Set<String> first = new HashSet<String>();
        for (int d = 1; d <= 60; d++) first.add(Variety.picks(res, Variety.daySeed(2026, 1 + (d - 1) / 30, 1 + (d - 1) % 30, "picks"), 1).get(0).recipe.id);
        check(first.size() >= 55, "variety: 60 days give >=55 different first picks, got " + first.size());
        // shuffle is a permutation
        List<Integer> nums = new ArrayList<Integer>(); for (int i = 0; i < 50; i++) nums.add(i);
        List<Integer> sh = Variety.shuffle(nums, 42); List<Integer> sorted = new ArrayList<Integer>(sh); Collections.sort(sorted);
        eq(nums, sorted, "variety: shuffle keeps every item"); check(!sh.equals(nums), "variety: shuffle changes order");
        // one() avoids repeating the open recipe
        Matcher.Result a = Variety.one(res, s1, null), b = Variety.one(res, s1, a.recipe.id);
        check(!a.recipe.id.equals(b.recipe.id), "variety: surprise avoids the recipe already open");
        check(Variety.one(new ArrayList<Matcher.Result>(), s1, null) == null, "variety: empty -> null");
        // respects filters: picks only come from the list given
        List<Matcher.Result> ital = Variety.inCuisine(res, "Italian");
        for (Matcher.Result r : Variety.picks(ital, s1, 20)) eq("Italian", r.recipe.cuisine, "variety: picks stay inside the filtered list");
        // try something new: least-opened cuisine, unopened recipe
        Set<String> seen = new HashSet<String>(); for (Matcher.Result r : res) if (!"Thai".equals(r.recipe.cuisine)) seen.add(r.recipe.id);
        Matcher.Result tn = Variety.tryNew(res, seen, s1);
        eq("Thai", tn.recipe.cuisine, "variety: try something new goes to the unopened cuisine");
        List<String> rc = Variety.rotatingCuisines(res, s1, 3, 8);
        eq(3, rc.size(), "variety: 3 rotating cuisine rows");
        for (String c : rc) check(Variety.inCuisine(res, c).size() >= 8, "variety: rotating cuisine has >= 8 recipes " + c);

        // vectors for js/core22.js
        File web = new File(root.getParentFile(), "pantry-recipes-web/tests/vectors22.json");
        if (web.getParentFile().isDirectory()) {
            StringBuilder o = new StringBuilder("{\n\"seeds\":[");
            String[] ss = {"", "a", "2026-11-03|picks", "Caf\u00e9 \u00fcber", "x".concat("y")};
            for (int i = 0; i < ss.length; i++) o.append(i > 0 ? "," : "").append("[").append(q(ss[i])).append(",").append(Variety.seed(ss[i])).append("]");
            o.append("],\n\"rng\":[");
            Variety.Rng r = new Variety.Rng(12345); for (int i = 0; i < 20; i++) o.append(i > 0 ? "," : "").append(r.nextInt());
            o.append("],\n\"cases\":[");
            int k = 0;
            for (int d = 1; d <= 6; d++) {
                int sd = Variety.daySeed(2026, 11, d, "picks"), sc = Variety.daySeed(2026, 11, d, "cuisines"), sn = Variety.daySeed(2026, 11, d, "new");
                Set<String> sn2 = new HashSet<String>(); int c = 0; for (Matcher.Result x : res) if ((c++ % (d + 1)) != 0) sn2.add(x.recipe.id);
                List<String> seenL = new ArrayList<String>(sn2); Collections.sort(seenL);
                o.append(k++ > 0 ? ",\n" : "\n").append("{\"d\":").append(d).append(",\"seed\":").append(sd)
                 .append(",\"picks\":").append(q(ids(Variety.picks(res, sd, 12))))
                 .append(",\"one\":").append(q(Variety.one(res, sd, null).recipe.id))
                 .append(",\"cuisines\":").append(q(String.join(",", Variety.rotatingCuisines(res, sc, 3, 8))))
                 .append(",\"seenEvery\":").append(d + 1)
                 .append(",\"tryNew\":").append(q(Variety.tryNew(res, sn2, sn).recipe.id)).append("}");
            }
            o.append("]}\n");
            Writer w = new OutputStreamWriter(new FileOutputStream(web), StandardCharsets.UTF_8); w.write(o.toString()); w.close();
        }
    }
}
