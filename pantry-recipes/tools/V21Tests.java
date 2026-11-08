import org.pantryrecipes.app.core.*;

import java.io.File;
import java.util.*;

/** v2.1/v2.2 checks: category moves, backup key, recipe growth per category. */
final class V21Tests {
    private V21Tests() { }
    static void check(boolean ok, String m) { RecipeTool.check(ok, m); }
    static void eq(Object e, Object g, String m) { RecipeTool.eq(e, g, m); }

    static void run(List<Recipe> parsed, File root) throws Exception {
        // ---- v2.2: botanical-fruit feature removed; every ingredient sits where it makes kitchen sense ----
        int veg = CategoryMoves.categoryIndex("Vegetables"), fr = CategoryMoves.categoryIndex("Fruits");
        for (String l : new String[]{"Bell pepper", "Chili", "Jalapeno", "Eggplant", "Zucchini", "Pumpkin", "Acorn squash", "Spaghetti squash", "Delicata squash", "Olives"})
            eq(veg, CategoryMoves.defaultCategory(Normalizer.normalize(l)), l + " in Vegetables");
        for (String l : new String[]{"Tomato", "Cherry tomatoes", "Cucumber", "Avocado"}) eq(fr, CategoryMoves.defaultCategory(Normalizer.normalize(l)), l + " in Fruits");
        check(PantryIndex.get().search("botanical fruits", null).labels.isEmpty(), "no botanical filter any more");
        check(PantryIndex.get().search("tomato", null).labels.contains("Tomato"), "plain search unchanged");
        String helpers = new String(java.nio.file.Files.readAllBytes(new File(root, "assets/helpers20.txt").toPath()), "UTF-8");
        check(!helpers.toLowerCase(Locale.ROOT).contains("botanic") && !helpers.contains("Nix v. Hedden"), "glossary has no botanical entry");
        // ---- category moves ----
        Map<String, Object> raw = new LinkedHashMap<String, Object>();
        raw.put("Tomato", "Vegetables"); raw.put("cucumber", "Vegetables"); raw.put("eggplant", "Nope"); raw.put("", "Fruits");
        raw.put("bell pepper", 7); raw.put("Olives", "Cans & jars");
        Map<String, String> mv = CategoryMoves.sanitize(raw);
        eq(3, mv.size(), "moves sanitized");
        eq("Vegetables", mv.get("tomato"), "move key normalized");
        eq(mv, CategoryMoves.parse(CategoryMoves.format(mv)), "moves round-trip");
        List<String> vs = CategoryMoves.sectionItems(veg, mv), fs = CategoryMoves.sectionItems(fr, mv);
        check(vs.contains("#Moved here by you") && vs.contains("Tomato") && vs.contains("Cucumber"), "moved items show in Vegetables");
        check(!fs.contains("Tomato") && !fs.contains("Cucumber") && fs.contains("Avocado") && !vs.contains("Olives"), "moved items leave their section");
        for (int i = 0; i < fs.size(); i++) if (Catalog.isHeader(fs.get(i))) check(i + 1 < fs.size() && !Catalog.isHeader(fs.get(i + 1)), "no empty header " + fs.get(i));
        eq(PantryIndex.get().sectionKeys(veg), CategoryMoves.sectionKeys(veg, new LinkedHashMap<String, String>()), "no moves = catalog");
        Set<String> pk = new HashSet<String>(Arrays.asList("tomato", "avocado"));
        eq(1, CategoryMoves.have(veg, mv, pk), "count follows the move");
        eq(1, CategoryMoves.have(fr, mv, pk), "count leaves the old section");
        Map<String, String> back = new LinkedHashMap<String, String>(); back.put("tomato", "Fruits");
        eq(CategoryMoves.sectionItems(fr, new LinkedHashMap<String, String>()), CategoryMoves.sectionItems(fr, back), "move to its own category = no change");
        // ---- backup ----
        check(Backup.SETTINGS.contains("catMoves"), "backup knows catMoves");
        Backup.Data bd = Backup.parse("{\"app\":\"Pantry Recipes\",\"format\":2,\"settings\":{\"catMoves\":{\"Tomato\":\"Vegetables\",\"x\":\"Bad\",\"apiKey\":5}}}");
        @SuppressWarnings("unchecked") Map<String, Object> cm = (Map<String, Object>) bd.settings.get("catMoves");
        eq(1, cm.size(), "backup catMoves sanitized");
        eq("Vegetables", cm.get("tomato"), "backup catMoves value");
        // ---- v2.1 Buy these next: ranked options, sorts, texts ----
        {
            List<String> ids = java.util.Arrays.asList("r1", "r2", "r3", "r4", "r5");
            List<List<String>> miss = new java.util.ArrayList<List<String>>();
            miss.add(java.util.Arrays.asList("lime")); miss.add(java.util.Arrays.asList("lime", "mint")); miss.add(java.util.Arrays.asList("mint"));
            miss.add(java.util.Arrays.asList("mint")); miss.add(java.util.Arrays.asList("a", "b", "c"));
            List<org.pantryrecipes.app.core.BuyNext.Option> rk = org.pantryrecipes.app.core.BuyNext.ranked(ids, miss, null);
            eq("mint,lime", rk.get(0).key + "," + rk.get(1).key, "buy-next ranked: most completed first");
            eq(2, rk.size(), "buy-next ranked skips recipes 3+ away");
            eq("Completes 2 recipes on its own and brings 1 more recipe within one ingredient.", org.pantryrecipes.app.core.BuyNext.why(rk.get(0)), "buy-next why");
            Map<String, org.pantryrecipes.app.core.Cost.Price> up = new java.util.HashMap<String, org.pantryrecipes.app.core.Cost.Price>();
            up.put("lime", org.pantryrecipes.app.core.Cost.make(0.5, 1, ""));
            eq("lime,mint", org.pantryrecipes.app.core.BuyNext.sort(rk, "cheapest", up, null).get(0).key + "," + org.pantryrecipes.app.core.BuyNext.sort(rk, "cheapest", up, null).get(1).key, "buy-next cheapest: priced first, unpriced last");
            eq("mint", org.pantryrecipes.app.core.BuyNext.sort(rk, "recipes", up, null).get(0).key, "buy-next recipes sort");
            eq("Your price: $0.50 each", org.pantryrecipes.app.core.BuyNext.costText(up.get("lime"), "$0.50", true), "buy-next cost text");
            eq(null, org.pantryrecipes.app.core.BuyNext.costText(null, "", false), "buy-next no price");
        }
        // ---- v2.1 faster greedy picks == the old loop, on real recipes and several pantries ----
        {
            String[][] pans = { {"chicken", "garlic", "onion", "rice", "egg"}, {"egg", "milk", "flour", "butter", "sugar", "salt"},
                    {"beef", "salt", "butter"}, {"tomato", "olive oil", "garlic", "pasta", "basil", "onion", "black pepper", "salt"}, {} };
            long tOld = 0, tNew = 0;
            for (String[] pan : pans) {
                Set<String> ps = new HashSet<String>();
                for (String x : pan) ps.add(Normalizer.normalize(x));
                List<String> ids = new ArrayList<String>(); List<List<String>> miss = new ArrayList<List<String>>(); Map<String, String> nm = new HashMap<String, String>();
                for (Recipe r : parsed) { List<String> m = new ArrayList<String>(); for (Ingredient i : r.ingredients) if (!i.optional && !ps.contains(i.key)) { m.add(i.key); nm.put(i.key, i.name); } ids.add(r.id); miss.add(m); }
                long t0 = System.nanoTime(); List<BuyNext.Pick> a1 = oldSuggest(ids, miss, nm, 3); long t1 = System.nanoTime(); List<BuyNext.Pick> b1 = BuyNext.suggest(ids, miss, nm, 3); long t2 = System.nanoTime();
                tOld += t1 - t0; tNew += t2 - t1;
                StringBuilder x = new StringBuilder(), y = new StringBuilder();
                for (BuyNext.Pick p : a1) x.append(p.key).append(p.unlocks).append(';');
                for (BuyNext.Pick p : b1) y.append(p.key).append(p.unlocks).append(';');
                eq(x.toString(), y.toString(), "buy-next fast picks == old loop for pantry " + Arrays.toString(pan));
            }
            System.out.printf("buy-next picks: old loop %.1f ms, v2.1 %.1f ms (5 pantries, JIT)%n", tOld / 1e6, tNew / 1e6);
        }
        // ---- v2.1 recipe growth ----
        Map<String, Integer> per = new TreeMap<String, Integer>();
        int carn = 0, v21 = 0;
        for (Recipe r : parsed) {
            per.put(r.category, per.containsKey(r.category) ? per.get(r.category) + 1 : 1);
            if (r.diets.contains("carnivore") || r.tags.contains("carnivore")) carn++;
            if (Integer.parseInt(r.id.substring(2)) > 2160) v21++;
        }
        check(parsed.size() >= 2600, "v2.1 bundle has 2600+ recipes (" + parsed.size() + ")");
        check(v21 >= 450, "v2.1 adds 450+ recipes (" + v21 + ")");
        check(carn >= 150, "carnivore 150+ (" + carn + ")");
        int[] base = {938, 178, 168, 163, 159, 147, 119, 107, 92, 89};
        String[] cats = {"Main", "Breakfast", "Soup", "Snack", "Side", "Baking", "Dessert", "Salad", "Drink", "Sauce"};
        for (int i = 0; i < cats.length; i++) check(per.containsKey(cats[i]) && per.get(cats[i]) >= base[i], "category kept or grew: " + cats[i]);
        System.out.println("v2.1 per category: " + per + " carnivore " + carn + " new " + v21);
    }

    static List<BuyNext.Pick> oldSuggest(List<String> ids, List<List<String>> missing, Map<String, String> names, int max) {
        List<Integer> pool = new ArrayList<Integer>(); Map<String, Integer> demand = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < ids.size(); i++) { Set<String> u = new HashSet<String>(missing.get(i)); if (u.size() < 1 || u.size() > 2) continue; pool.add(i); for (String k : u) demand.put(k, demand.containsKey(k) ? demand.get(k) + 1 : 1); }
        List<BuyNext.Pick> out = new ArrayList<BuyNext.Pick>(); Set<String> chosen = new HashSet<String>(); Set<Integer> done = new HashSet<Integer>();
        for (int round = 0; round < max; round++) {
            String best = null; List<Integer> bestNew = null;
            for (String k : demand.keySet()) {
                if (chosen.contains(k)) continue;
                List<Integer> nw = new ArrayList<Integer>();
                for (int i : pool) { if (done.contains(i)) continue; boolean ok = true; for (String m : missing.get(i)) if (!m.equals(k) && !chosen.contains(m)) { ok = false; break; } if (ok) nw.add(i); }
                if (nw.isEmpty()) continue;
                if (best == null || nw.size() > bestNew.size() || (nw.size() == bestNew.size() && (demand.get(k) > demand.get(best) || (demand.get(k).equals(demand.get(best)) && k.compareTo(best) < 0)))) { best = k; bestNew = nw; }
            }
            if (best == null) break;
            chosen.add(best);
            List<String> one = new ArrayList<String>(); one.add(best);
            List<List<String>> mm = new ArrayList<List<String>>(); mm.add(one);
            BuyNext.Pick p = BuyNext.suggest(java.util.Collections.singletonList("x"), mm, java.util.Collections.singletonMap(best, names.get(best)), 1).get(0);
            p.unlocks.clear(); for (int i : bestNew) { done.add(i); p.unlocks.add(ids.get(i)); }
            out.add(p);
        }
        return out;
    }
}
