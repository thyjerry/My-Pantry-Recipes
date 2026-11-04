import org.pantryrecipes.app.core.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Reference results for the v1.5 web port (js/core15.js): dates, use-by, allergens/dislikes, amounts,
 * meal-plan shopping, ranking boost, hero order, JSON, backup files and themes. Written to tests/vectors15.json.
 * Usage: java -cp build V15Export <android project dir> <web dir>
 */
public class V15Export {
    static List<Object> L(Object... a) { return new ArrayList<Object>(Arrays.asList(a)); }
    static Object n(double d) { return Double.isNaN(d) ? null : (Object) d; }

    public static void main(String[] a) throws Exception {
        File android = new File(a[0]), web = new File(a[1]);
        List<Recipe> recipes = Recipe.parseList(new String(Files.readAllBytes(new File(android, "assets/recipes.json").toPath()), StandardCharsets.UTF_8));
        final Map<String, Recipe> byId = new HashMap<String, Recipe>();
        for (Recipe r : recipes) byId.put(r.id, r);
        Random rnd = new Random(1505);
        Map<String, Object> v = new LinkedHashMap<String, Object>();

        // ---- dates
        List<Object> dates = new ArrayList<Object>();
        List<String> isos = new ArrayList<String>(Arrays.asList("2024-02-29", "2023-02-29", "2026-10-29", "1900-01-01", "2999-12-31", "1899-12-31",
                "3000-01-01", "2026-13-01", "2026-00-10", "2026-1-01", "20261001", "", "2026-10-3x", "+026-10-01", "2026-+1-01", "2026-1\u0661-01", "２026-10-01", "2026-04-31",
                "2026-04-30", "2000-02-29", "2100-02-29", "1970-01-01", "2026-12-31", "2027-01-01"));
        for (int k = 0; k < 300; k++) isos.add(Dates.fromDays(rnd.nextInt(400000) - 25567));
        for (String s : isos) {
            boolean ok = Dates.valid(s);
            dates.add(L(s, ok, ok ? (Object) Dates.toDays(s) : null, Dates.label(s), ok ? Dates.addDays(s, 40) : null, ok ? Dates.addDays(s, -400) : null,
                    ok ? Dates.weekStart(s) : null, ok ? (Object) Dates.dayOfWeek(s) : null, ok ? Dates.week(s).get(6) : null,
                    ok ? (Object) Dates.between("2026-10-29", s) : null));
        }
        v.put("dates", dates);

        // ---- use-by
        List<Object> ub = new ArrayList<Object>();
        for (int d = -12; d <= 12; d++) ub.add(L(d, UseBy.status(d), UseBy.label(d)));
        v.put("useByLabels", ub);
        List<Object> dl = new ArrayList<Object>();
        for (String s : new String[]{"2026-10-29", "2026-10-30", "2026-11-01", "2026-11-02", "2026-10-28", "", "junk", "2027-10-29"})
            dl.add(L(s, UseBy.daysLeft(s, "2026-10-29")));
        v.put("daysLeft", dl);

        // ---- allergens + dislikes
        List<String> dis1 = Arrays.asList("mushroom", "coriander", "onion");
        List<String> dis2 = Arrays.asList("chicken", "milk", "pepper", "olive oil");
        List<String> av1 = Arrays.asList("peanuts", "gluten", "shellfish");
        List<String> av2 = new ArrayList<String>(Allergens.IDS);
        List<Object> al = new ArrayList<Object>();
        for (Recipe r : recipes) {
            int warn = 0;
            for (Ingredient i : r.ingredients) if (Allergens.ingredientWarn(i, dis2, av1)) warn++;
            al.add(L(r.id, Allergens.flags(r), Allergens.reasons(r, dis1, null), Allergens.reasons(r, dis2, av1), Allergens.hidden(r, dis1, av1),
                    Allergens.hidden(r, new ArrayList<String>(), new ArrayList<String>()), Allergens.reasons(r, null, av2), warn));
        }
        v.put("allergens", al);
        List<Object> dk = new ArrayList<Object>();
        for (String s : new String[]{"Mushrooms", "  Fresh Coriander ", "Spring onions", "", "Chillies", "bell pepper", "PEANUT BUTTER", "Crème fraîche"})
            dk.add(L(s, Allergens.dislikeKey(s)));
        v.put("dislikeKeys", dk);

        // ---- amounts
        List<Object> canon = new ArrayList<Object>();
        String[] units = {"g", "Grams", "kg", "KGs", "oz", "Ounces", "lb", "lbs.", "ml", "L", "litres", "tsp", "tsp.", "Tbsp", "tbs", "cup", "Cups", "c",
                "fl oz", "floz", "pint", "pt", "", "x", "pieces", "Large", "clove", "cloves", "can", "cans", "bunches", "dishes", "glass", "glasses",
                "slices", "sprigs", "pinch", "pinches", "bus", "  Tins ", "handfuls"};
        for (String u : units) canon.add(L(u, Amounts.canon(u), Amounts.dimension(u)));
        v.put("canon", canon);
        List<Object> conv = new ArrayList<Object>();
        for (String x : units) for (String y : new String[]{"g", "ml", "cup", "", "clove", "lb", "tbsp"})
            conv.add(L(1.5, x, y, n(Amounts.convert(1.5, x, y))));
        v.put("convert", conv);
        List<String> qtyTexts = Arrays.asList("500 g", "6", "2 cups", "1.5 kg", "1 lb", "3 cloves", "some", "", "250ml", "12 eggs", "1/2 cup", "2 tbsp", "1 can", "4 large");
        List<Object> checks = new ArrayList<Object>();
        for (int k = 0; k < 400; k++) {
            Recipe r = recipes.get(rnd.nextInt(recipes.size()));
            double scale = new double[]{0.5, 1, 1.5, 2, 0.25}[rnd.nextInt(5)];
            Map<String, String> q = new LinkedHashMap<String, String>();
            for (Ingredient i : r.ingredients) if (rnd.nextInt(2) == 0 && !i.key.isEmpty()) {
                String key = rnd.nextInt(6) == 0 && Normalizer.family(i.key) != null ? Normalizer.family(i.key) : i.key;
                String t = rnd.nextInt(3) == 0 ? qtyTexts.get(rnd.nextInt(qtyTexts.size()))
                        : (Double.isNaN(i.amount) ? "2" : Quantity.format(i.amount * scale * (0.5 + rnd.nextInt(3) * 0.5), i.unit, 1));
                q.put(key, t);
            }
            List<Object> out = new ArrayList<Object>();
            for (Amounts.Check c : Amounts.check(r, scale, q)) out.add(L(c.ingredient.key, c.status, c.haveText, Amounts.label(c)));
            List<Object> qp = new ArrayList<Object>();
            for (Map.Entry<String, String> e : q.entrySet()) qp.add(L(e.getKey(), e.getValue()));
            checks.add(L(r.id, scale, qp, out));
        }
        v.put("amountChecks", checks);

        // ---- meal plan shopping + use-by ranking
        List<String> allItems = new ArrayList<String>(Catalog.allItems());
        List<Object> plans = new ArrayList<Object>();
        List<Object> boosts = new ArrayList<Object>();
        for (int s = 0; s < 30; s++) {
            List<String> pantry = new ArrayList<String>();
            Collections.shuffle(allItems, rnd);
            for (int k = 0; k < 5 + s * 3 && k < allItems.size(); k++) pantry.add(Normalizer.normalize(allItems.get(k)));
            List<String> staples = new ArrayList<String>();
            for (String x : Catalog.DEFAULT_STAPLES) staples.add(Normalizer.normalize(x));
            Matcher m = new Matcher(pantry, staples);
            Map<String, String> q = new LinkedHashMap<String, String>();
            for (String p : pantry) if (rnd.nextInt(3) == 0) q.put(p, qtyTexts.get(rnd.nextInt(qtyTexts.size())));
            List<MealPlan.Entry> entries = new ArrayList<MealPlan.Entry>();
            int ne = 1 + rnd.nextInt(8);
            for (int k = 0; k < ne; k++) entries.add(new MealPlan.Entry(Dates.addDays("2026-10-26", rnd.nextInt(9) - 1),
                    rnd.nextInt(12) == 0 ? "missing:id" : recipes.get(rnd.nextInt(recipes.size())).id, 1 + rnd.nextInt(30)));
            List<Object> items = new ArrayList<Object>();
            for (MealPlan.Item it : MealPlan.shopping(MealPlan.forWeek(entries, "2026-10-26"), new MealPlan.Lookup() {
                public Recipe byId(String id) { return byId.get(id); }
            }, m, q)) items.add(L(it.key, it.name, n(it.amount), it.unit, new ArrayList<Object>(it.recipes), it.partial, it.text()));
            List<Object> ent = new ArrayList<Object>();
            for (MealPlan.Entry e : entries) ent.add(L(e.date, e.id, e.servings));
            List<Object> pq = new ArrayList<Object>();
            for (Map.Entry<String, String> e : q.entrySet()) pq.add(L(e.getKey(), e.getValue()));
            List<Object> wk = new ArrayList<Object>();
            for (MealPlan.Entry e : MealPlan.forWeek(entries, "2026-10-26")) wk.add(e.date + "|" + e.id);
            List<Object> pr = new ArrayList<Object>();
            for (MealPlan.Entry e : MealPlan.prune(entries, "2026-10-29", 1)) pr.add(e.date + "|" + e.id);
            plans.add(L(new ArrayList<Object>(pantry), new ArrayList<Object>(staples), pq, ent, wk, pr, items));

            // use-by boost
            Map<String, String> useBy = new LinkedHashMap<String, String>();
            for (String p : pantry) if (rnd.nextInt(3) == 0) useBy.put(p, Dates.addDays("2026-10-29", rnd.nextInt(12) - 3));
            Set<String> soon = UseBy.soonKeys(useBy, "2026-10-29");
            List<Matcher.Result> res = new ArrayList<Matcher.Result>();
            for (Recipe r : recipes) res.add(m.match(r));
            List<Matcher.Result> sorted = new ArrayList<Matcher.Result>(res);
            UseBy.sort(sorted, soon);
            List<Object> ids = new ArrayList<Object>();
            for (Matcher.Result r : sorted) ids.add(r.recipe.id);
            List<Object> up = new ArrayList<Object>();
            for (Matcher.Result r : UseBy.useItUp(res, soon, 12)) up.add(r.recipe.id);
            List<Object> ubp = new ArrayList<Object>();
            for (Map.Entry<String, String> e : useBy.entrySet()) ubp.add(L(e.getKey(), e.getValue()));
            boosts.add(L(new ArrayList<Object>(pantry), new ArrayList<Object>(staples), ubp, new ArrayList<Object>(soon), ids, up));
        }
        v.put("plans", plans);
        v.put("boosts", boosts);

        // ---- hero order
        List<Object> ho = new ArrayList<Object>();
        for (int k = 0; k < 200; k++) {
            List<String> in = new ArrayList<String>();
            for (String mt : MealType.ALL) if (rnd.nextInt(3) == 0) in.add(mt);
            Collections.shuffle(in, rnd);
            if (k % 7 == 0) in.add("Nonsense");
            ho.add(L(new ArrayList<Object>(in), new ArrayList<Object>(MealType.heroOrder(in))));
        }
        v.put("heroOrder", ho);

        // ---- JSON
        List<Object> js = new ArrayList<Object>();
        String[] jin = {"{}", "[]", "\"a\\u0041\\/\\q\"", "{\"a\":1,\"a\":2,\"b\":[1,2.5,-0,1e3,1E-7,12345678.5,3.0]}", "[1.,.5,+1,-.5e2]",
                "{\"1\":1,\"0\":2,\"b\":3}", "[\"\\b\\f\\u0001\\u001f\\u007f\u2028\"]", "tru", "[1,]", "{\"a\" 1}", "[1 2]", "  [ ]  x", "\"abc", "[1e]", "[--1]",
                "[\"\\u12\"]", "[\"\\u+041\"]", "nul", "[0.1,0.30000000000000004,1e21,1e-3,0.001,9999999.5,10000000.5,12345678901234,-999999999999999]", "",
                "[\"\t raw tab\"]", "{\"k\":{\"n\":[true,false,null]}}", "[1e400]", "[0.0001]", "[100.0]", "[-1.5e-9]"};
        for (String s : jin) {
            String out;
            try { out = Json.write(Json.parse(s)); } catch (RuntimeException e) { out = "ERR"; }
            js.add(L(s, out));
        }
        v.put("json", js);

        // ---- backups
        List<Object> bk = new ArrayList<Object>();
        List<String> inputs = new ArrayList<String>(Arrays.asList("", "   ", "not json", "[]", "{}", "{\"app\":\"Other\"}", "{\"app\":\"Pantry Recipes\"}",
                "{\"app\":\"Pantry Recipes\",\"format\":\"1\"}", "{\"app\":\"Pantry Recipes\",\"format\":2}", "{\"app\":\"Pantry Recipes\",\"format\":0}",
                "{\"app\":\"Pantry Recipes\",\"format\":1.9}",
                "{\"app\":\"Pantry Recipes\",\"format\":1,\"pantry\":[{\"label\":\"  Tomatoes \",\"staple\":1,\"useBy\":\"2026-02-30\",\"qty\":\" 500 g \"},"
                        + "{\"label\":\"tomato\"},{\"label\":\"\"},5,{\"label\":\"Salt\",\"staple\":true,\"useBy\":\"2026-11-02\"}],\"favorites\":[\"b:1\",\"b:1\",7,\"  \"],"
                        + "\"shopping\":[{\"name\":\"Milk\",\"amount\":-1},{\"name\":\"Eggs\",\"amount\":6,\"unit\":\"\",\"checked\":true},{\"name\":\"\"},{\"name\":\"Flour\",\"amount\":0.333333,\"unit\":\"cup\",\"recipe\":\"Cake\"}],"
                        + "\"ratings\":{\"b:2\":{\"stars\":9},\"b:3\":{\"stars\":0},\"b:4\":{\"stars\":2.7,\"note\":\" good \"},\"\":{\"stars\":3},\"10\":{\"stars\":1},\"2\":{\"stars\":2}},"
                        + "\"plan\":[{\"date\":\"2026-11-02\",\"id\":\"b:1\",\"servings\":100},{\"date\":\"bad\",\"id\":\"b:1\"},{\"date\":\"2026-11-03\",\"id\":\"b:2\"}],"
                        + "\"dislikes\":[\"Mushrooms\",\"mushroom\",\"\",3],\"allergens\":[\"sesame\",\"peanuts\",\"bogus\",\"sesame\"],"
                        + "\"settings\":{\"servings\":0,\"meals\":[\"Dinner\",\"Breakfast\",\"x\"],\"incDesserts\":false,\"diets\":[\"vegan\",\"keto\",\"vegan\"],\"maxTime\":15.7,\"mode\":3,\"online\":false,\"acceptedSubs\":[\"Buttermilk\",\"buttermilk\",\"\"],\"extra\":1},"
                        + "\"theme\":{\"active\":\"custom-2\",\"custom\":[{\"id\":\"custom-2\",\"name\":\"\",\"bg\":\"#fff\",\"surface\":\"FFFFFF\",\"primary\":\"#123456\",\"accent\":\"#abcdef\",\"text\":\"#000000\"},{\"id\":\"bad\",\"name\":\"x\"}]},"
                        + "\"recipes\":[{\"id\":\"u:1\",\"title\":\"Toast\",\"ingredients\":[{\"name\":\"Bread\",\"qty\":2}],\"steps\":[\"Toast it\",5],\"tags\":[\"vegan\",true],\"meals\":[\"breakfast\",\"Brunch\",\"breakfast\"],\"prep\":2.9},"
                        + "{\"id\":\"b:5\",\"title\":\"x\",\"ingredients\":[{\"name\":\"a\"}]},{\"id\":\"u:2\",\"title\":\" \",\"ingredients\":[{\"name\":\"a\"}]},{\"id\":\"u:3\",\"title\":\"No ing\"},"
                        + "{\"id\":\"u:4\",\"title\":\"Bad ing\",\"ingredients\":[5]},{\"id\":\"u:1\",\"title\":\"Dup\",\"ingredients\":[{\"name\":\"a\"}]},"
                        + "{\"id\":\"m:9\",\"title\":\"Meal\",\"category\":\"Seafood\",\"ingredients\":[{\"name\":\"Prawns\",\"qty\":200,\"unit\":\"g\",\"optional\":true}],\"servingsKnown\":false,\"image\":\"https://www.themealdb.com/x.jpg\"}]}",
                "{\"app\":\"Pantry Recipes\",\"format\":1,\"theme\":\"dark\"}",
                "{\"app\":\"Pantry Recipes\",\"format\":1,\"theme\":{\"active\":\"system\"}}",
                "{\"app\":\"Pantry Recipes\",\"format\":1,\"theme\":{\"active\":\"custom-1\",\"custom\":[]}}"));
        // random full backups written by Java
        for (int k = 0; k < 25; k++) inputs.add(Backup.write(randomData(rnd, recipes)));
        for (String s : inputs) {
            String out, sum;
            try {
                Backup.Data d = Backup.parse(s);
                out = Backup.write(d);
                sum = Backup.summary(d);
            } catch (Backup.BackupException e) {
                out = "ERR:" + e.getMessage();
                sum = null;
            }
            bk.add(L(s, out, sum));
        }
        v.put("backups", bk);

        // ---- palette
        List<Object> pal = new ArrayList<Object>();
        List<Palette.Base> bases = new ArrayList<Palette.Base>(Palette.PRESETS);
        for (int k = 0; k < 400; k++) bases.add(new Palette.Base("custom-" + k, "R" + k, rnd.nextInt(0x1000000), rnd.nextInt(0x1000000),
                rnd.nextInt(0x1000000), rnd.nextInt(0x1000000), rnd.nextInt(0x1000000)));
        for (Palette.Base b : bases) {
            List<Object> pairs = new ArrayList<Object>();
            Palette.Colors c = Palette.derive(b);
            for (Object[] p : Palette.textPairs(c)) pairs.add(Palette.ratio(Palette.contrast((Integer) p[1], (Integer) p[2])));
            pal.add(L(b.id, b.name, Palette.hex(b.bg), Palette.hex(b.surface), Palette.hex(b.primary), Palette.hex(b.accent), Palette.hex(b.text),
                    Json.write(c.toMap()), new ArrayList<Object>(Palette.warnings(b)), pairs));
        }
        v.put("palette", pal);
        List<Object> hsv = new ArrayList<Object>();
        for (int k = 0; k < 300; k++) {
            int c = rnd.nextInt(0x1000000);
            double[] h = Palette.toHsv(c);
            int hh = (int) Math.round(h[0]), ss = (int) Math.round(h[1] * 100), vv = (int) Math.round(h[2] * 100);
            hsv.add(L(c, h[0], h[1], h[2], Palette.fromHsv(hh, ss / 100.0, vv / 100.0)));
        }
        v.put("hsv", hsv);
        List<Object> hx = new ArrayList<Object>();
        for (String s : new String[]{"#2E9E5B", "2e9e5b", "#abc", "abc", " #ABC ", "#12345", "#1234567", "#GGGGGG", "", "#", null, "#00000g"})
            hx.add(L(s, Palette.parseHex(s)));
        v.put("parseHex", hx);
        List<Object> st = new ArrayList<Object>();
        String[] sts = {"", "junk", "[]", "{}", "{\"active\":\"ocean\"}", "{\"active\":\"system\"}", "{\"active\":\"nope\"}",
                "{\"active\":\"custom-3\",\"custom\":[{\"id\":\"custom-3\",\"name\":\"  A very long theme name that goes past forty characters  \",\"bg\":\"#111\",\"surface\":\"#222222\",\"primary\":\"#333333\",\"accent\":\"#444444\",\"text\":\"#eeeeee\"}]}",
                "{\"active\":\"custom-1\",\"custom\":[{\"id\":\"custom-1\",\"bg\":\"#111\",\"surface\":\"#222222\",\"primary\":\"#333333\",\"accent\":\"#444444\",\"text\":\"#eeeeee\"},{\"id\":\"custom-1\",\"bg\":\"#fff\",\"surface\":\"#fff\",\"primary\":\"#000\",\"accent\":\"#000\",\"text\":\"#000\"},{\"id\":\"custom-12345\",\"bg\":\"#fff\",\"surface\":\"#fff\",\"primary\":\"#000\",\"accent\":\"#000\",\"text\":\"#000\"},{\"id\":\"light\",\"bg\":\"#fff\",\"surface\":\"#fff\",\"primary\":\"#000\",\"accent\":\"#000\",\"text\":\"#000\"},{\"id\":\"custom-2\",\"bg\":\"#fff\",\"surface\":\"#fff\",\"primary\":\"#000\",\"accent\":\"#000\",\"text\":5}]}"};
        for (String s : sts) st.add(L(s, Json.write(Palette.parseState(s).toMap())));
        StringBuilder many = new StringBuilder("{\"active\":\"custom-14\",\"custom\":[");
        for (int k = 1; k <= 15; k++) many.append(k > 1 ? "," : "").append("{\"id\":\"custom-").append(k).append("\",\"name\":\"T").append(k).append("\",\"bg\":\"#fff\",\"surface\":\"#fff\",\"primary\":\"#000\",\"accent\":\"#000\",\"text\":\"#000\"}");
        many.append("]}");
        st.add(L(many.toString(), Json.write(Palette.parseState(many.toString()).toMap())));
        v.put("themeStates", st);

        Files.write(new File(web, "tests/vectors15.json").toPath(), Json.write(v).getBytes(StandardCharsets.UTF_8));
        System.out.println("vectors15.json written");
    }

    static String pick(Random r, String... a) { return a[r.nextInt(a.length)]; }

    static Backup.Data randomData(Random rnd, List<Recipe> recipes) {
        Backup.Data d = new Backup.Data();
        d.appVersion = pick(rnd, "1.5", "1.5-web");
        d.platform = pick(rnd, "android", "web");
        d.exported = "2026-10-29T20:00:00Z";
        List<String> items = Catalog.allItems();
        for (int k = 0; k < rnd.nextInt(30); k++) {
            Backup.PantryEntry e = new Backup.PantryEntry();
            e.label = items.get(rnd.nextInt(items.size()));
            e.key = Normalizer.normalize(e.label);
            e.staple = rnd.nextInt(4) == 0;
            if (rnd.nextBoolean()) e.useBy = Dates.addDays("2026-10-29", rnd.nextInt(30) - 5);
            if (rnd.nextInt(3) == 0) e.qty = pick(rnd, "500 g", "6", "2 cups", "a bit \"of\" \\ it", "1\t2");
            d.pantry.add(e);
        }
        for (int k = 0; k < rnd.nextInt(8); k++) d.favorites.add(recipes.get(rnd.nextInt(recipes.size())).id);
        for (int k = 0; k < rnd.nextInt(10); k++) {
            Backup.ShopEntry e = new Backup.ShopEntry();
            e.name = pick(rnd, "Milk", "Eggs", "Flour \u00e9", "Line\nbreak", "Emoji \ud83c\udf45");
            if (rnd.nextBoolean()) e.amount = pick(rnd, "1", "2", "3").length() * (rnd.nextInt(1000) / 7.0);
            if (rnd.nextBoolean()) e.unit = pick(rnd, "g", "cup", "ml");
            if (rnd.nextBoolean()) e.recipe = recipes.get(rnd.nextInt(recipes.size())).title;
            e.checked = rnd.nextBoolean();
            d.shopping.add(e);
        }
        for (int k = 0; k < rnd.nextInt(6); k++) {
            Backup.Rating r = new Backup.Rating();
            r.stars = 1 + rnd.nextInt(5);
            if (rnd.nextBoolean()) r.note = pick(rnd, "Great", "Needs salt\nmore", "\u2605 fav");
            d.ratings.put(recipes.get(rnd.nextInt(recipes.size())).id, r);
        }
        for (int k = 0; k < rnd.nextInt(8); k++)
            d.plan.add(new MealPlan.Entry(Dates.addDays("2026-10-26", rnd.nextInt(14)), recipes.get(rnd.nextInt(recipes.size())).id, 1 + rnd.nextInt(8)));
        if (rnd.nextBoolean()) d.dislikes.add("mushroom");
        if (rnd.nextBoolean()) d.allergens.add("peanuts");
        if (rnd.nextBoolean()) d.settings.put("servings", (long) (1 + rnd.nextInt(8)));
        if (rnd.nextBoolean()) d.settings.put("meals", new ArrayList<Object>(Arrays.asList("Breakfast", "Dinner")));
        if (rnd.nextBoolean()) d.settings.put("online", rnd.nextBoolean());
        if (rnd.nextBoolean()) d.settings.put("maxTime", 30L);
        if (rnd.nextInt(3) > 0) {
            Palette.State s = new Palette.State();
            int nc = rnd.nextInt(4);
            for (int k = 0; k < nc; k++) s.put(new Palette.Base(s.nextCustomId(), "Theme " + k, rnd.nextInt(0x1000000), rnd.nextInt(0x1000000), rnd.nextInt(0x1000000), rnd.nextInt(0x1000000), rnd.nextInt(0x1000000)));
            s.active = nc > 0 && rnd.nextBoolean() ? "custom-1" : pick(rnd, "light", "dark", "system", "plum");
            d.theme = s;
        }
        if (rnd.nextInt(3) == 0) {
            Recipe r = new Recipe();
            r.id = "u:" + rnd.nextInt(1000);
            r.title = "Imported " + rnd.nextInt(100);
            r.category = "Main";
            r.ingredients.add(new Ingredient("Chicken thighs", 1.5, "lb", false));
            r.ingredients.add(new Ingredient("Salt", Double.NaN, "", true));
            r.steps.add("Cook it.");
            r.meals.addAll(MealType.infer(r.category, r.title));
            d.recipes.add(r);
        }
        return d;
    }
}
