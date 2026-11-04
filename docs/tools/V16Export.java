import org.pantryrecipes.app.core.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Reference results for the v1.6 web port (js/core16.js): cooking-time parsing, nutrition and cost estimates,
 * aisles, "buy these next", leftovers, pantry deduction, barcodes, share text and format-2 backups (including
 * genuine v1.5 backup files). Written to tests/vectors16.json.
 * Usage: java -cp build V16Export <android project dir> <web dir>
 */
public class V16Export {
    static List<Object> L(Object... a) { return new ArrayList<Object>(Arrays.asList(a)); }
    static Object n(double d) { return Double.isNaN(d) ? null : (Object) d; }
    static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }
    static <T> T pick(Random r, T[] a) { return a[r.nextInt(a.length)]; }

    public static void main(String[] a) throws Exception {
        File android = new File(a[0]), web = new File(a[1]);
        List<Recipe> recipes = Recipe.parseList(read(new File(android, "assets/recipes.json")));
        Map<String, Nutrition.Food> foods = Nutrition.parse(read(new File(android, "assets/foods.tsv")));
        Map<String, Cost.Price> defaults = Cost.parseDefaults(read(new File(android, "assets/prices.tsv")));
        Random rnd = new Random(1606);
        Map<String, Object> v = new LinkedHashMap<String, Object>();

        // ---- durations: every bundled step plus edge cases
        List<String> texts = new ArrayList<String>(Arrays.asList("Simmer for 10-12 minutes.", "Bake 1 hour 30 minutes", "Rest half an hour",
                "Cook 2 to 3 hours", "between 5 and 8 minutes", "forty-five seconds", "an hour and a half", "1\u00bd hours", "1 1/2 hrs",
                "a couple of minutes", "twenty five mins", "10 \u2013 15 min then 2 min", "the 1960s style", "3 eggs, 200 g", "100 hours", "5\u20134 min",
                "1.5 h", "2,5 minutes", "for 90 sec, then 1 min 30 sec", "MINUTES 5", "10 minutes or until golden", "Bake 20 or 25 minutes", ""));
        for (Recipe r : recipes) texts.addAll(r.steps);
        List<Object> dur = new ArrayList<Object>();
        for (String t : texts) {
            List<Object> ms = new ArrayList<Object>();
            for (Durations.Match m : Durations.find(t)) ms.add(L(m.start, m.end, m.seconds, m.maxSeconds, m.label(), m.text));
            if (!ms.isEmpty() || dur.size() < 40) dur.add(L(t, ms));
        }
        v.put("durations", dur);
        List<Object> df = new ArrayList<Object>();
        for (int s : new int[]{0, 1, 45, 60, 61, 90, 600, 3599, 3600, 3660, 5400, 7200, 86399, 172800})
            df.add(L(s, Durations.format(s), Durations.rangeLabel(s, s + 60), Durations.rangeLabel(s, s + 3600), Durations.clock(s * 1000L), Durations.clock(s * 1000L - 1)));
        v.put("durFormat", df);

        // ---- nutrition (every recipe) and grams
        List<Object> nut = new ArrayList<Object>();
        for (Recipe r : recipes) {
            Nutrition.Estimate e = Nutrition.estimate(r, foods);
            nut.add(L(r.id, Json.write(e.toMap()), Nutrition.summary(e)));
        }
        v.put("nutrition", nut);
        List<Object> gr = new ArrayList<Object>();
        String[] gu = {"g", "kg", "oz", "cup", "tbsp", "", "clove", "can", "pinch", "handful", "slice", "bunch", "sprig", "inch", "cloves", "large handful", "splash"};
        String[] gk = {"onion", "olive oil", "flour", "garlic", "canned tomato", "salt", "parmesan", "egg", "rice", "spinach", "unknown thing", "spaghetti", "cheddar"};
        for (String k : gk) for (String u : gu) gr.add(L(k, u, n(Nutrition.grams(Nutrition.lookup(foods, k), 1.5, u))));
        v.put("grams", gr);

        // ---- cost
        Map<String, Cost.Price> user = new LinkedHashMap<String, Cost.Price>();
        user.put("chicken thigh", Cost.make(2.99, 1, "lb"));
        user.put("egg", Cost.make(5.5, 12, ""));
        user.put("pasta", Cost.make(1.29, 500, "g"));
        user.put("milk", Cost.make(1.1, 1, "l"));
        user.put("onion", Cost.make(0.5, 1, ""));
        user.put("butter", Cost.make(3.2, 250, "g"));
        List<Object> cost = new ArrayList<Object>();
        for (Recipe r : recipes) {
            int sv = 1 + rnd.nextInt(10);
            cost.add(L(r.id, sv, Json.write(Cost.estimate(r, r.servings, null, defaults, foods).toMap()),
                    Json.write(Cost.estimate(r, sv, user, defaults, foods).toMap()), Json.write(Cost.estimate(r, sv, user, null, foods).toMap())));
        }
        v.put("cost", cost);
        v.put("costUser", Json.write(new LinkedHashMap<String, Object>() {{ for (Map.Entry<String, Cost.Price> e : user.entrySet()) put(e.getKey(), e.getValue().toMap()); }}));
        List<Object> money = new ArrayList<Object>();
        for (double m : new double[]{0, 0.004, 0.005, 0.1, 1.255, 2.5, 10, 99.999, 1234.5678})
            for (String c : new String[]{"$", "", " \u20ac ", "kr"}) money.add(L(m, c, Cost.money(c, m)));
        v.put("money", money);
        List<Object> desc = new ArrayList<Object>();
        for (String u : Cost.UNITS) for (double amt : new double[]{1, 0.5, 2.5, 500, 12}) desc.add(L(u, amt, Cost.describe("$", Cost.make(3.49, amt, u))));
        v.put("describe", desc);
        List<Object> shopRows = new ArrayList<Object>();
        List<Object[]> rowsJ = new ArrayList<Object[]>();
        for (int k = 0; k < 60; k++) {
            Recipe r = recipes.get(rnd.nextInt(recipes.size()));
            Ingredient i = r.ingredients.get(rnd.nextInt(r.ingredients.size()));
            Object[] row = {i.name, i.amount, i.unit, rnd.nextInt(4) == 0};
            rowsJ.add(row);
            shopRows.add(L(i.name, n(i.amount), i.unit, row[3], n(Cost.item(i.name, i.amount, i.unit, user, defaults, foods))));
        }
        double[] st = Cost.shopping(rowsJ, user, defaults, foods);
        v.put("shopRows", shopRows);
        v.put("shopTotal", L(st[0], (int) st[1], (int) st[2]));

        // ---- aisles
        List<Object> ai = new ArrayList<Object>();
        for (String s : Catalog.allItems()) ai.add(L(s, Aisles.of(s)));
        Set<String> seen = new HashSet<String>();
        for (Recipe r : recipes) for (Ingredient i : r.ingredients) if (seen.add(i.name)) ai.add(L(i.name, Aisles.of(i.name)));
        v.put("aisles", ai);

        // ---- share text
        v.put("shareShopping", ShareText.shopping(rowsJ));
        List<Object[]> planRows = new ArrayList<Object[]>();
        for (int k = 0; k < 9; k++) planRows.add(new Object[]{Dates.addDays("2026-10-26", k / 2), recipes.get(rnd.nextInt(recipes.size())).title, 1 + rnd.nextInt(5)});
        List<Object> pr = new ArrayList<Object>();
        for (Object[] r : planRows) pr.add(L(r));
        v.put("planRows", pr);
        v.put("sharePlan", ShareText.plan("26 Oct \u2013 1 Nov", planRows));
        v.put("sharePlanEmpty", ShareText.plan("", new ArrayList<Object[]>()));

        // ---- buy these next (random pantries)
        List<String> all = Catalog.allItems();
        List<Object> bn = new ArrayList<Object>();
        for (int t = 0; t < 40; t++) {
            List<String> pantry = new ArrayList<String>();
            int np = 5 + rnd.nextInt(40);
            for (int k = 0; k < np; k++) pantry.add(Normalizer.normalize(all.get(rnd.nextInt(all.size()))));
            Matcher m = new Matcher(pantry, Arrays.asList("salt", "black pepper", "olive oil", "vegetable oil", "water"));
            List<Matcher.Result> res = new ArrayList<Matcher.Result>();
            for (Recipe r : recipes) res.add(m.match(r));
            List<Object> picks = new ArrayList<Object>();
            for (BuyNext.Pick p : BuyNext.fromResults(res, 1 + t % 3)) picks.add(L(p.key, p.name, p.unlocks));
            bn.add(L(pantry, 1 + t % 3, picks));
        }
        v.put("buyNext", bn);

        // ---- leftovers
        List<Leftovers.Entry> lo = new ArrayList<Leftovers.Entry>();
        List<Object> ops = new ArrayList<Object>();
        for (int k = 0; k < 30; k++) {
            int op = rnd.nextInt(3);
            if (op == 0 || lo.isEmpty()) {
                String day = Dates.addDays("2026-10-20", rnd.nextInt(10));
                int p = rnd.nextInt(6) - 1, d = rnd.nextInt(20) - 2;
                Leftovers.Entry e = Leftovers.add(lo, "r" + k, "Dish " + k, p, day, d);
                ops.add(L("add", "r" + k, "Dish " + k, p, day, d, e == null ? null : e.id));
            } else {
                String id = lo.get(rnd.nextInt(lo.size())).id;
                int dl = rnd.nextInt(5) - 3;
                ops.add(L("adjust", id, dl, Leftovers.adjust(lo, id, dl)));
            }
            List<Object> state = new ArrayList<Object>();
            for (Leftovers.Entry e : Leftovers.sorted(lo)) state.add(L(Json.write(e.toMap()), Leftovers.status(e, "2026-10-27")));
            ops.add(state);
        }
        v.put("leftovers", ops);
        v.put("leftoverDays", L(Leftovers.clampDays(-3), Leftovers.clampDays(0), Leftovers.clampDays(4), Leftovers.clampDays(99)));

        // ---- deduction
        List<Object> ded = new ArrayList<Object>();
        String[] haves = {"500 g", "6", "2 cups", "1.5 kg", "1 lb", "3 cloves", "some", "", "250ml", "12 eggs", "1/2 cup", "2 tbsp", "1 can", "4 large", "1 L"};
        String[][] needs = {{"200", "g"}, {"2", ""}, {"1", "cup"}, {"3", "clove"}, {"700", "g"}, {"1", "can"}, {"100", "ml"}, {"2", "egg"}, {"0", "g"}};
        for (String h : haves) for (String[] nd : needs) for (String key : new String[]{"egg", "garlic", "flour"})
            ded.add(L(h, key, Double.parseDouble(nd[0]), nd[1], Amounts.deduct(h, key, Double.parseDouble(nd[0]), nd[1])));
        v.put("deduct", ded);

        // ---- barcodes
        List<Object> bc = new ArrayList<Object>();
        for (String c : new String[]{"4006381333931", "4006381333932", "036000291452", "036000291453", "96385074", "96385075", "01234565", "04252614",
                "00012345678905", "8001505005707", "  8001505-005707 ", "abc", "", "12345", "0", "123456789012345", "5000159484695", "3017620422003"})
            bc.add(L(c, Barcode.clean(c), Barcode.valid(c)));
        v.put("barcodes", bc);
        List<String> offs = Arrays.asList(
                "{\"code\":\"3017620422003\",\"status\":1,\"product\":{\"product_name\":\"Nutella\",\"brands\":\"Ferrero\",\"quantity\":\"400 g\",\"categories_tags\":[\"en:spreads\",\"en:sweet-spreads\",\"en:hazelnut-spreads\"]}}",
                "{\"code\":\"8001505005707\",\"status\":1,\"product\":{\"product_name\":\"Spaghetti n.5\",\"brands\":\"Barilla\",\"categories_tags\":[\"en:pastas\",\"en:spaghetti\"]}}",
                "{\"code\":\"5000159484695\",\"status\":1,\"product\":{\"product_name\":\"\",\"product_name_en\":\"Chopped Tomatoes\",\"brands\":\"Napolina, Princes\",\"categories_tags\":[\"en:canned-foods\",\"en:canned-tomatoes\"]}}",
                "{\"code\":\"1\",\"status\":0,\"status_verbose\":\"product not found\"}",
                "{\"status\":1,\"product\":{\"generic_name\":\"Whole milk\",\"categories_tags\":[\"en:dairies\",\"en:milks\",\"en:whole-milks\"]}}",
                "{\"status\":1,\"product\":{\"product_name\":\"Organic Extra Virgin Olive Oil\",\"brands\":\"Generic\"}}",
                "{\"status\":1,\"product\":{\"product_name\":\"Free range eggs x6\",\"categories_tags\":[\"en:eggs\"]}}",
                "{\"status\":1,\"product\":{\"product_name\":\"Basmati Rice\",\"brands\":\"Tilda\",\"quantity\":\"1 kg\"}}",
                "{\"status\":1,\"product\":{\"product_name\":\"Cola zero\",\"categories_tags\":[\"en:beverages\",\"en:sodas\"]}}",
                "{\"status\":1,\"product\":{}}", "not json", "[]", "{\"status\":\"1\"}",
                "{\"status\":1,\"product\":{\"product_name\":\"Greek Style Yogurt\",\"brands\":\",\"}}",
                "{\"status\":1,\"product\":{\"product_name\":\"Unsalted Butter\",\"categories_tags\":[\"fr:beurres\",\"en:butters\",42]}}");
        List<Object> off = new ArrayList<Object>();
        for (String j : offs) {
            Barcode.Product p = Barcode.parse(j);
            off.add(L(j, p == null ? null : L(p.code, p.name, p.generic, p.brands, p.quantity, p.categories, p.title(), Barcode.match(p))));
        }
        v.put("off", off);

        // ---- backups: format 2 round trips and genuine v1.5 files
        List<Object> bk = new ArrayList<Object>();
        for (int t = 0; t < 25; t++) {
            Backup.Data d = new Backup.Data();
            d.appVersion = "1.6"; d.platform = "android"; d.exported = "2026-10-29";
            for (int k = 0; k < rnd.nextInt(4); k++) {
                String key = Normalizer.normalize(all.get(rnd.nextInt(all.size())));
                d.prices.put(key, Cost.make(Math.round(rnd.nextDouble() * 1000) / 100.0, 1 + rnd.nextInt(500), pick(rnd, Cost.UNITS)));
            }
            for (int k = 0; k < rnd.nextInt(3); k++) Leftovers.add(d.leftovers, recipes.get(k).id, recipes.get(k).title, 1 + rnd.nextInt(4), "2026-10-2" + k, 3 + k);
            for (int k = 0; k < rnd.nextInt(3); k++) Leftovers.log(d.cooked, recipes.get(k).id, 2 + k, "2026-10-1" + k);
            if (rnd.nextBoolean()) d.barcodes.put("4006381333931", "Pencils");
            if (rnd.nextInt(3) == 0) d.photos.put(recipes.get(t).id, "data:image/jpeg;base64,/9j/4AAQSkZJRg==");
            if (rnd.nextBoolean()) d.settings.put("currency", pick(rnd, new String[]{"\u20ac", "kr", "$"}));
            if (rnd.nextBoolean()) d.settings.put("useDefaultPrices", rnd.nextBoolean());
            if (rnd.nextBoolean()) d.settings.put("leftoverDays", (long) (1 + rnd.nextInt(7)));
            if (rnd.nextBoolean()) {
                Palette.State s = new Palette.State();
                s.active = Palette.PRESETS.get(rnd.nextInt(Palette.PRESETS.size())).id;
                s.font = pick(rnd, Palette.FONT_IDS); s.corners = pick(rnd, Palette.CORNER_IDS);
                if (rnd.nextBoolean()) s.accentPick = Palette.QUICK_ACCENTS[rnd.nextInt(Palette.QUICK_ACCENTS.length)];
                d.theme = s;
            }
            String w = Backup.write(d);
            Backup.Data back = Backup.parse(w);
            bk.add(L(w, Backup.write(back), Backup.summary(back)));
        }
        String[] odd = {"{\"app\":\"Pantry Recipes\",\"format\":3}",
                "{\"app\":\"Pantry Recipes\",\"format\":2,\"prices\":{\"Eggs\":{\"price\":3,\"amount\":12,\"unit\":\"\"},\"x\":{\"price\":-1,\"amount\":1,\"unit\":\"g\"},\"flour\":{\"price\":2,\"amount\":1,\"unit\":\"cup\"},\"rice\":[1]},\"settings\":{\"currency\":\"<b>\",\"leftoverDays\":40,\"deductPantry\":true,\"shopCost\":\"yes\"}}",
                "{\"app\":\"Pantry Recipes\",\"format\":2,\"leftovers\":[{\"id\":\"l1\",\"recipe\":\"x\",\"title\":\"Soup\",\"portions\":2.7,\"cooked\":\"2026-10-01\",\"useBy\":\"2026-10-04\"},{\"id\":\"l1\",\"title\":\"Dup\",\"portions\":1,\"cooked\":\"2026-10-01\",\"useBy\":\"2026-10-04\"},{\"id\":\"l2\",\"title\":\"Bad\",\"portions\":0,\"cooked\":\"2026-10-01\",\"useBy\":\"2026-10-04\"}],\"cooked\":[{\"date\":\"2026-10-01\",\"id\":\"b1\",\"servings\":500},{\"date\":\"x\",\"id\":\"b1\"}]}",
                "{\"app\":\"Pantry Recipes\",\"format\":2,\"barcodes\":{\"4006381333931\":\"Pencils\",\"4006381333932\":\"Bad\",\"96385074\":\"\"},\"photos\":{\"a\":\"data:image/jpeg;base64,AAAA\",\"b\":\"data:image/png;base64,AAAA\",\"c\":\"data:image/jpeg;base64,A*AA\",\"d\":5}}"};
        for (String o : odd) {
            try {
                Backup.Data back = Backup.parse(o);
                bk.add(L(o, Backup.write(back), Backup.summary(back)));
            } catch (Backup.BackupException e) {
                bk.add(L(o, "ERR:" + e.getMessage(), null));
            }
        }
        List<String> v15 = new ArrayList<String>();
        Object fx = Json.parse(read(new File(web, "tests/fixtures/v15-backups.json")));
        for (Object o : (List<?>) fx) v15.add((String) o);
        for (String o : v15) {
            Backup.Data back = Backup.parse(o);
            bk.add(L(o, Backup.write(back), Backup.summary(back)));
        }
        v.put("backups", bk);

        // ---- palette 1.6 options
        List<Object> pal = new ArrayList<Object>();
        for (String s : new String[]{"{\"active\":\"sepia\",\"font\":\"large\",\"corners\":\"round\",\"accent\":\"#7C3AED\",\"custom\":[]}",
                "{\"active\":\"black\",\"font\":\"huge\",\"corners\":5,\"accent\":\"nope\"}", "{\"active\":\"nightshade\",\"accent\":\"#abc\"}"}) {
            Palette.State ps = Palette.parseState(s);
            pal.add(L(s, Json.write(ps.toMap()), Json.write(Palette.derive(ps.resolve(false)).toMap()), ps.fontScale(), ps.cornerScale()));
        }
        v.put("palette16", pal);
        List<Object> rp = new ArrayList<Object>();
        for (double r : new double[]{0, 0.1, 0.5, 0.99, 0.999999, 1}) rp.add(L(r, Palette.randomPreset("light", r), Palette.randomPreset("nightshade", r)));
        v.put("randomPreset", rp);

        Files.write(new File(web, "tests/vectors16.json").toPath(), Json.write(v).getBytes(StandardCharsets.UTF_8));
        System.out.println("vectors16.json written");
    }
}
