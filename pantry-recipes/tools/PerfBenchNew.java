import org.pantryrecipes.app.core.*;
import java.util.*;

/** v1.8 code paths for PerfBench (same work items as the legacy run). */
public class PerfBenchNew {
    public static void run(String dir, Map<String, List<Double>> t) throws Exception {
        PerfBench.load(dir, t);
        List<Recipe> recipes = PerfBench.recipes;
        long t0 = System.nanoTime();
        // v1.8: autocomplete names are built on the worker after loading; the Pantry only reads the cached list
        List<String> names = PantryIndex.ingredientNames(recipes, new LinkedHashMap<String, String>());
        PerfBench.put(t, "1b names + index on worker (off UI, v1.8)", PerfBench.ms(t0));
        t0 = System.nanoTime();
        PerfBench.put(t, "2 Pantry open: autocomplete names (UI)", PerfBench.ms(t0)); // cached list: no work
        t0 = System.nanoTime();
        PantryIndex idx = PantryIndex.get();
        Set<String> pantry = new HashSet<String>();
        for (String p : PerfBench.PANTRY) pantry.add(Normalizer.normalize(p));
        for (PantryIndex.Item it : idx.items) if (it.category == 0) it.key.length();
        int have = 0;
        for (int c = 0; c < Catalog.CATEGORIES.length; c++) have += idx.have(c, pantry);
        PerfBench.put(t, "3 Pantry open: chip keys + counts (UI)", PerfBench.ms(t0));
        t0 = System.nanoTime();
        // v1.8 debounces: typing 6 letters quickly runs the filter once or twice; measured as 6 runs anyway
        for (String q0 : new String[]{"c", "ch", "che", "chee", "chees", "cheese"}) idx.search(q0, null);
        PerfBench.put(t, "4 filter: 6 keystrokes total (UI)", PerfBench.ms(t0));
        t0 = System.nanoTime();
        pantry.add(Normalizer.normalize("Feta"));
        for (int c = 0; c < Catalog.CATEGORIES.length; c++) have += idx.have(c, pantry);
        PerfBench.put(t, "5 toggle: model + counts (UI)", PerfBench.ms(t0));
        List<String> pk = new ArrayList<String>(), sk = new ArrayList<String>();
        for (String p : PerfBench.PANTRY) pk.add(Normalizer.normalize(p));
        for (String s : PerfBench.STAPLES) sk.add(Normalizer.normalize(s));
        MatchCache mc = new MatchCache();
        t0 = System.nanoTime();
        List<Matcher.Result> res = mc.match(recipes, pk, sk, PerfBench.subs, new HashSet<String>());
        PerfBench.put(t, "6a first full match + index (worker prewarm)", PerfBench.ms(t0));
        t0 = System.nanoTime();
        pk.add(Normalizer.normalize("Feta"));
        res = mc.match(recipes, pk, sk, PerfBench.subs, new HashSet<String>());
        UseBy.sort(res, new HashSet<String>(Arrays.asList(Normalizer.normalize("Spinach"))));
        PerfBench.put(t, "6 re-match all " + recipes.size() + " + sort (UI)", PerfBench.ms(t0));
        PerfBench.put(t, "6c   recipes re-matched after toggle (count)", mc.lastMatched);
        t0 = System.nanoTime();
        for (String q0 : new String[]{"c", "ch", "chi", "chic", "chick"}) {
            String q = q0.toLowerCase(Locale.US), qk = Normalizer.normalize(q0);
            int n = 0;
            for (Matcher.Result r : res) if (r.recipe.searchHit(q, qk)) n++;
        }
        PerfBench.put(t, "7 recipe search: 5 keystrokes total (UI)", PerfBench.ms(t0));
    }
}
