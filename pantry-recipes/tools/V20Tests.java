import org.pantryrecipes.app.core.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** v2.0 Stage 1 checks: diets, inventory, regional prices, editor, region, cuisines, backup, sync (simulated devices + loopback). */
final class V20Tests {
    private V20Tests() { }

    static void check(boolean ok, String m) { RecipeTool.check(ok, m); }
    static void eq(Object e, Object g, String m) { RecipeTool.eq(e, g, m); }
    static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    static Recipe rec(String title, String category, String cuisine, int servings, String... ings) {
        Recipe r = new Recipe();
        r.id = "t:" + title; r.title = title; r.category = category; r.cuisine = cuisine; r.servings = servings;
        for (String s : ings) {
            String[] p = s.split("\\|");
            r.ingredients.add(new Ingredient(p[p.length - 1].trim(), p.length > 1 ? Double.parseDouble(p[0].trim().split(" ")[0]) : Double.NaN,
                    p.length > 1 && p[0].trim().contains(" ") ? p[0].trim().split(" ")[1] : "", false));
        }
        return r;
    }

    static boolean fits(Recipe r, String d, Map<String, Nutrition.Food> f) { return Diets.classify(r, f).contains(d); }

    static final Map<String, Integer> SPOT = new TreeMap<String, Integer>();
    static void spot(String diet, boolean expect, Recipe r, Map<String, Nutrition.Food> f) {
        SPOT.merge(diet, 1, Integer::sum);
        eq(expect, fits(r, diet, f), "diet " + diet + " for " + r.title);
    }

    static void run(List<Recipe> parsed, File root) throws Exception {
        int before = RecipeTool.failures, checks0 = RecipeTool.checks;
        helpers(root);
        Map<String, Nutrition.Food> foods = Nutrition.parse(read(new File(root, "assets/foods.tsv")));
        diets(parsed, foods);
        inventory(parsed);
        prices(root);
        stage3(parsed, root);
        editor(foods);
        region();
        cuisines(parsed);
        backup();
        sync();
        loopback();
        palette();
        System.out.println("v2.0 checks: " + (RecipeTool.checks - checks0) + " run, " + (RecipeTool.failures - before) + " failed");
    }

    // ------------------------------------------------------------------ diets
    static void diets(List<Recipe> parsed, Map<String, Nutrition.Food> foods) {
        Map<String, Integer> count = new LinkedHashMap<String, Integer>();
        for (String d : Arrays.asList(Recipe.VEGETARIAN, Recipe.VEGAN, Recipe.GLUTEN_FREE, Recipe.DAIRY_FREE)) count.put(d, 0);
        for (String d : Diets.IDS) count.put(d, 0);
        int mismatch = 0;
        for (Recipe r : parsed) {
            for (String t : r.tags) if (count.containsKey(t)) count.merge(t, 1, Integer::sum);
            for (String t : r.diets) count.merge(t, 1, Integer::sum);
            if (!Diets.classify(r, foods).equals(r.diets)) mismatch++;
        }
        eq(0, mismatch, "stored diets in recipes.json equal the runtime classifier");
        System.out.println("Diet counts (" + parsed.size() + " recipes): " + count);
        for (String d : Diets.IDS) check(Diets.RULES.containsKey(d), "diet has a plain-language rule: " + d);
        check(Diets.NOTE.contains("guide, not a certification"), "diet note says guide, not certification");
        for (String d : Arrays.asList(Diets.PESCATARIAN, Diets.MEDITERRANEAN, Diets.LOW_CARB, Diets.KETO, Diets.PALEO, Diets.WHOLE30,
                Diets.LOW_SODIUM, Diets.HIGH_PROTEIN, Diets.HALAL, Diets.KOSHER, Recipe.VEGAN, Recipe.VEGETARIAN, Recipe.GLUTEN_FREE, Recipe.DAIRY_FREE))
            check(count.get(d) >= 20, "at least 20 bundled recipes for " + d + " (" + count.get(d) + ")");
        check(count.get(Diets.CARNIVORE) >= 60, "60+ carnivore recipes (" + count.get(Diets.CARNIVORE) + ")");

        Recipe steak = rec("Steak", "Main", "American", 2, "2 | ribeye steak", "1 tsp | salt", "2 tbsp | butter");
        Recipe pasta = rec("Pasta", "Main", "Italian", 4, "400 g | spaghetti", "3 tbsp | olive oil", "2 cloves | garlic", "50 g | parmesan");
        Recipe salmon = rec("Salmon", "Main", "Greek", 2, "2 | salmon fillets", "2 tbsp | olive oil", "1 | lemon", "1 tsp | salt");
        Recipe bacon = rec("Bacon eggs", "Breakfast", "British", 2, "4 slices | bacon", "4 | eggs");
        Recipe cheeseburger = rec("Cheeseburger", "Main", "American", 4, "500 g | ground beef", "4 slices | cheddar", "4 | hamburger buns");
        Recipe shrimp = rec("Garlic shrimp", "Main", "Spanish", 4, "400 g | shrimp", "4 tbsp | olive oil", "4 cloves | garlic");
        Recipe wine = rec("Coq au vin", "Main", "French", 4, "8 | chicken thighs", "500 ml | red wine", "1 | onion");
        Recipe jelly = rec("Jelly", "Dessert", "British", 4, "4 sheets | gelatin", "500 ml | orange juice");
        Recipe lentil = rec("Lentil soup", "Soup", "Lebanese", 4, "250 g | red lentils", "3 tbsp | olive oil", "1 | onion", "1 tsp | cumin");
        Recipe beet = rec("Beet salad", "Salad", "International", 2, "4 | beets", "2 tbsp | olive oil", "50 g | feta");
        Recipe dates = rec("Date balls", "Dessert", "International", 4, "200 g | dates", "100 g | almonds");
        Recipe sausage = rec("Sausage", "Main", "British", 4, "8 | sausages", "1 kg | potatoes");
        Recipe chickenSausage = rec("Chicken sausage", "Main", "American", 4, "8 | chicken sausages", "2 | bell peppers");
        Recipe soy = rec("Stir fry", "Main", "Chinese", 4, "3 tbsp | soy sauce", "300 g | broccoli", "1 tbsp | vegetable oil");
        Recipe mirin = rec("Teriyaki salmon", "Main", "Japanese", 2, "2 | salmon fillets", "2 tbsp | mirin", "2 tbsp | soy sauce");
        Recipe chickenRice = rec("Chicken rice", "Main", "Chinese", 4, "4 | chicken breasts", "300 g | rice", "1 tbsp | vegetable oil");
        Recipe butterChicken = rec("Chicken in butter", "Main", "Indian", 4, "4 | chicken breasts", "3 tbsp | butter", "200 ml | heavy cream");
        Recipe fishCream = rec("Salmon cream", "Main", "French", 2, "2 | salmon fillets", "100 ml | heavy cream");
        Recipe catfish = rec("Catfish", "Main", "Southern US", 4, "4 | catfish fillets", "100 g | cornmeal");
        Recipe rabbit = rec("Rabbit", "Main", "French", 4, "1 | rabbit", "2 | carrots");
        Recipe hotdog = rec("Beef dogs", "Main", "American", 4, "4 | beef hot dogs", "4 | hot dog buns");

        spot(Diets.PESCATARIAN, true, salmon, foods); spot(Diets.PESCATARIAN, false, steak, foods); spot(Diets.PESCATARIAN, true, pasta, foods);
        spot(Diets.MEDITERRANEAN, true, salmon, foods); spot(Diets.MEDITERRANEAN, true, lentil, foods); spot(Diets.MEDITERRANEAN, false, steak, foods);
        spot(Diets.LOW_CARB, true, steak, foods); spot(Diets.LOW_CARB, false, pasta, foods); spot(Diets.LOW_CARB, false, lentil, foods); spot(Diets.LOW_CARB, true, beet, foods);
        spot(Diets.KETO, true, steak, foods); spot(Diets.KETO, false, beet, foods); spot(Diets.KETO, false, dates, foods);
        spot(Diets.PALEO, true, salmon, foods); spot(Diets.PALEO, false, pasta, foods); spot(Diets.PALEO, true, dates, foods); spot(Diets.PALEO, false, soy, foods);
        spot(Diets.WHOLE30, true, salmon, foods); spot(Diets.WHOLE30, false, dates, foods); spot(Diets.WHOLE30, false, bacon, foods); spot(Diets.WHOLE30, false, wine, foods);
        spot(Diets.CARNIVORE, true, steak, foods); spot(Diets.CARNIVORE, false, salmon, foods); spot(Diets.CARNIVORE, false, bacon, foods);
        spot(Diets.LOW_SODIUM, false, soy, foods); spot(Diets.LOW_SODIUM, true, lentil, foods);
        spot(Diets.HIGH_PROTEIN, true, steak, foods); spot(Diets.HIGH_PROTEIN, false, jelly, foods);
        spot(Diets.HALAL, false, bacon, foods); spot(Diets.HALAL, false, wine, foods); spot(Diets.HALAL, false, jelly, foods); spot(Diets.HALAL, false, mirin, foods);
        spot(Diets.HALAL, false, sausage, foods); spot(Diets.HALAL, true, chickenSausage, foods); spot(Diets.HALAL, true, chickenRice, foods);
        spot(Diets.HALAL, true, hotdog, foods); spot(Diets.HALAL, true, shrimp, foods);
        spot(Diets.KOSHER, false, cheeseburger, foods); spot(Diets.KOSHER, false, butterChicken, foods); spot(Diets.KOSHER, true, fishCream, foods);
        spot(Diets.KOSHER, false, shrimp, foods); spot(Diets.KOSHER, false, catfish, foods); spot(Diets.KOSHER, false, rabbit, foods);
        spot(Diets.KOSHER, false, wine, foods); spot(Diets.KOSHER, true, chickenRice, foods); spot(Diets.KOSHER, true, salmon, foods); spot(Diets.KOSHER, false, bacon, foods);
        check(Diets.blockers(cheeseburger, Diets.KOSHER).contains("meat or poultry together with dairy"), "kosher blocker explains meat with dairy");
        check(Diets.blockers(bacon, Diets.HALAL).contains("bacon"), "halal blocker names bacon");
        check(Diets.RULES.get(Diets.HALAL).contains("not certification") && Diets.RULES.get(Diets.KOSHER).contains("not certification"), "halal/kosher rules say not certification");
        check(Diets.RULES.get(Diets.WHOLE30).contains("not assumed"), "Whole30 explains its conservative exclusions");
        System.out.println("Diet spot tests per diet: " + SPOT);
        for (Recipe r : parsed) if (r.diets.contains(Diets.KETO)) check(r.diets.contains(Diets.LOW_CARB), "keto implies low-carb: " + r.title);
        for (Recipe r : parsed) if (r.diets.contains(Diets.WHOLE30)) check(r.diets.contains(Diets.PALEO), "Whole30 implies paleo: " + r.title);
    }

    // ------------------------------------------------------------------ inventory
    static void inventory(List<Recipe> parsed) {
        eq("freezer", Inventory.defaultLocation(Normalizer.normalize("frozen peas"), "frozen peas"), "frozen peas -> freezer");
        eq("fridge", Inventory.defaultLocation("milk", "Milk"), "milk -> fridge");
        eq("spice", Inventory.defaultLocation("cumin", "Cumin"), "cumin -> spice rack");
        eq("pantry", Inventory.defaultLocation("potato", "Potatoes"), "potatoes stay in the pantry");
        eq("pantry", Inventory.defaultLocation("rice", "Rice"), "rice -> pantry");
        check(Inventory.freezerMonths("ground beef") > 0, "freezer months for ground beef");
        String fr = "2026-01-10";
        String toss = Inventory.useOrTossDate(fr, "ground beef");
        check(Dates.valid(toss) && toss.compareTo(fr) > 0, "use-or-toss after frozen date");
        eq(Inventory.OK, Inventory.freezerStatus(fr, "ground beef", "2026-01-20"), "fresh frozen is ok");
        eq(Inventory.PAST, Inventory.freezerStatus(fr, "ground beef", Dates.addDays(toss, 1)), "past best after the date");
        eq(Inventory.USE_SOON, Inventory.freezerStatus(fr, "ground beef", Dates.addDays(toss, -10)), "use soon within 30 days");
        // v2.0.1: starter kits removed
        check(!hasField(Inventory.class, "KITS"), "starter kits are gone (Inventory.KITS)");
        List<Inventory.Item> items = new ArrayList<Inventory.Item>();
        Inventory.Item a = new Inventory.Item("milk", "Milk"); a.low = true; a.useBy = "2026-11-02"; items.add(a);
        Inventory.Item b = new Inventory.Item("salt", "Salt"); b.low = true; b.staple = true; items.add(b);
        Inventory.Item c = new Inventory.Item("ground beef", "Ground beef"); c.loc = "freezer"; c.frozen = "2025-01-01"; items.add(c);
        eq("Salt", Inventory.restock(items).get(0).label, "staples first on the restock list");
        eq(1, Inventory.freezerAlerts(items, "2026-11-01").size(), "one freezer alert");
        eq(1, Inventory.expiringIn(items, "2026-11-01", 1).size(), "expiring tomorrow (evening-before reminder)");
        int[] n = Inventory.counts(items);
        eq(4, n.length, "four locations");
        Map<String, Integer> kc = Inventory.keyCounts(parsed);
        List<String[]> buy = Inventory.spicesToBuy(new HashSet<String>(Arrays.asList("salt")), kc, 5);
        check(buy.size() == 5, "spice rack buy-next gives 5");
        Recipe t = Inventory.cookTonight(parsed, new HashSet<String>(Arrays.asList("milk")), new HashSet<String>(Arrays.asList("milk", "eggs", "flour", "butter", "sugar")));
        check(t != null, "cook this tonight finds a recipe for milk");
    }

    // ------------------------------------------------------------------ prices
    static void prices(File root) throws Exception {
        RegionalPrices.Table t = RegionalPrices.parse(read(new File(root, "assets/bls-prices.tsv")));
        check(t.items.size() >= 25, "BLS table has items (" + t.items.size() + ")");
        check(t.date != null && t.date.matches("[A-Z][a-z]{2} 20\\d\\d"), "BLS table has a date: " + t.date);
        eq("NE", RegionalPrices.region("10001"), "NYC ZIP -> Northeast");
        eq("MW", RegionalPrices.region("60601"), "Chicago -> Midwest");
        eq("S", RegionalPrices.region("30301"), "Atlanta -> South");
        eq("W", RegionalPrices.region("94105"), "San Francisco -> West");
        eq("US", RegionalPrices.region("00901"), "Puerto Rico falls back to national");
        eq("", RegionalPrices.cleanZip("abc"), "bad ZIP rejected");
        for (String r : RegionalPrices.REGIONS) { double f = t.factor(r); check(f > 0.7 && f < 1.4, "factor sane for " + r + ": " + f); }
        Map<String, RegionalPrices.Source> src = new HashMap<String, RegionalPrices.Source>();
        Map<String, Cost.Price> est = new LinkedHashMap<String, Cost.Price>();
        est.put("saffron", new Cost.Price(8, 1, "g"));
        est.put("egg", new Cost.Price(0.2, 1, ""));
        Map<String, Cost.Price> d = RegionalPrices.defaults(t, "NE", est, src);
        check(src.get("egg").label.startsWith("BLS") && src.get("egg").label.contains(t.date), "BLS label (with date) wins over the estimate wins over the estimate: " + src.get("egg").label);
        check(src.get("saffron").kind.equals("estimate-factor"), "estimate x factor for items BLS lacks");
        Map<String, Cost.Price> user = new HashMap<String, Cost.Price>();
        user.put("egg", new Cost.Price(5, 12, ""));
        eq(5.0, Cost.find("egg", user, d).price, "user price wins");
        String op = "{\"items\":[{\"price\":3.49,\"currency\":\"USD\",\"date\":\"2026-09-01\",\"location\":{\"osm_name\":\"Shop A\",\"osm_address_postcode\":\"10001\",\"osm_address_country_code\":\"US\"}},"
                + "{\"price\":2.99,\"currency\":\"USD\",\"date\":\"2026-10-01\",\"location\":{\"osm_address_postcode\":\"94105\",\"osm_address_country_code\":\"US\"}},"
                + "{\"price\":1.99,\"currency\":\"EUR\",\"date\":\"2026-10-02\"},{\"price\":\"x\"}]}";
        List<RegionalPrices.OpenPrice> ops = RegionalPrices.parseOpenPrices(op, "NE", 10);
        eq(2, ops.size(), "Open Prices keeps USD only");
        check(ops.get(0).nearby && ops.get(0).price == 3.49, "nearby Open Price first");
        eq(0, RegionalPrices.parseOpenPrices("not json", "NE", 10).size(), "bad Open Prices JSON is ignored");
        check(RegionalPrices.openPricesUrl("0123456789012").startsWith("https://prices.openfoodfacts.org/api/v1/prices?product_code=0123456789012"), "Open Prices URL");
        check(!RegionalPrices.openPricesUrl("12a<>").contains("<"), "Open Prices URL sends digits only");
    }

    // ------------------------------------------------------------------ editor
    static void editor(Map<String, Nutrition.Food> foods) throws Exception {
        Ingredient i = RecipeEditor.ingredientLine("2 cups flour");
        check(i != null && i.amount == 2 && i.unit.startsWith("cup") && i.key.equals("flour"), "editor line: 2 cups flour");
        Ingredient j = RecipeEditor.ingredientLine("1/2 tsp salt (optional)");
        check(j != null && Math.abs(j.amount - 0.5) < 1e-9 && j.optional, "editor line: fraction and optional");
        check(RecipeEditor.ingredientLine("   ") == null, "blank line ignored");
        for (String line : new String[]{"1 1/2 cups milk", "2 eggs", "250 g butter, softened", "1/4 tsp salt", "salt, optional", "3 cloves garlic", "1 l stock"}) {
            Ingredient a = RecipeEditor.ingredientLine(line), b = RecipeEditor.ingredientLine(RecipeEditor.lineOf(a));
            check(b != null && a.key.equals(b.key) && (Double.isNaN(a.amount) ? Double.isNaN(b.amount) : Math.abs(a.amount - b.amount) < 0.01)
                    && a.optional == b.optional && Quantity.parse("1 " + a.unit).unit.equals(Quantity.parse("1 " + b.unit).unit), "editor line round trip: " + line + " -> " + RecipeEditor.lineOf(a));
        }
        RecipeEditor.Draft d = new RecipeEditor.Draft();
        eq("Give the recipe a name.", RecipeEditor.problem(d), "editor asks for a name");
        d.title = "Grandma's <b>Soup</b>"; d.ingredients = "2 carrots\n1 onion\n1 l vegetable stock"; d.steps = "1. Chop.\n2) Simmer 20 min."; d.tags = "Family, family, winter";
        d.servings = 4; d.cook = 30;
        eq("", RecipeEditor.problem(d), "valid draft");
        Recipe r = RecipeEditor.build(d, "u:test1", foods);
        eq(2, r.steps.size(), "steps parsed");
        eq("Chop.", r.steps.get(0), "step numbers stripped");
        eq(2, r.userTags.size(), "tags deduplicated");
        check(r.tags.contains(Recipe.VEGAN), "built recipe gets diet tags");
        Recipe bundled = RecipeEditor.build(d, "u:x", foods); bundled.id = "b:77";
        Recipe mv = RecipeEditor.build(RecipeEditor.myVersion(bundled), "u:test2", foods);
        check(mv.title.endsWith("(my version)") && "b:77".equals(mv.basedOn), "my version copy of a bundled recipe");
        eq(r.title, RecipeEditor.myVersion(r).title, "my own recipe is edited in place, not copied");
        String pack = RecipeEditor.packWrite("Family", Arrays.asList(r, mv), "2.0", "2026-10-30");
        List<Recipe> back = RecipeEditor.packParse(pack, new HashSet<String>(Arrays.asList("u:test1")), foods, 7);
        eq(2, back.size(), "pack round trip");
        check(!back.get(0).id.equals("u:test1") && back.get(0).id.startsWith("u:"), "clashing id is renumbered");
        eq(r.title, back.get(0).title, "pack keeps titles");
        boolean threw = false;
        try { RecipeEditor.packParse("{\"kind\":\"other\"}", new HashSet<String>(), foods, 1); } catch (RecipeEditor.PackException e) { threw = true; }
        check(threw, "non-pack file rejected");
        String txt = RecipeEditor.shareText(r, 8);
        check(txt.contains(r.title) && txt.toLowerCase(Locale.ROOT).contains("4 carrots"), "share text scales: " + txt.replace('\n', '/'));
        String html = RecipeEditor.cookbookHtml("Our <cookbook>", Arrays.asList(r), new HashMap<String, Backup.Rating>(), "2026-10-30");
        check(!html.contains("<b>Soup") && html.contains("&lt;b&gt;"), "cookbook escapes HTML");
        check(!html.toLowerCase().contains("<script"), "cookbook has no scripts");
        check(html.contains("Content-Security-Policy"), "cookbook has a CSP");
    }

    // ------------------------------------------------------------------ region
    /** Words that are distinct products in the catalog or ambiguous: display-only, never merged for matching. */
    static final Set<String> ALIAS_DISTINCT = new HashSet<String>(Arrays.asList("coriander", "pepper", "peppers", "sultanas", "sultana",
            "golden syrup", "zucchinis", "courgettes", "corn", "biscuits", "melon", "grill"));
    static int aliasPairs;

    static void region() {
        eq(6, Region.IDS.size(), "six region choices");
        eq("uk", Region.guess("en-GB"), "en-GB -> UK");
        eq("uk", Region.guess("en-IE"), "en-IE -> UK & Ireland");
        eq("us", Region.guess("en-US"), "en-US -> US");
        eq("ca", Region.guess("fr-CA"), "fr-CA -> Canada");
        eq("eu", Region.guess("de-DE"), "de-DE -> EU");
        eq("eu", Region.guess("fr"), "fr -> EU");
        eq("anz", Region.guess("en-NZ"), "en-NZ -> ANZ");
        eq("other", Region.guess("ja-JP"), "ja-JP -> other");
        Region.Defaults us = Region.defaults("us"), uk = Region.defaults("uk"), eu = Region.defaults("eu"), anz = Region.defaults("anz"), ca = Region.defaults("ca");
        eq(Units.US, us.units, "US uses US customary"); eq("$", us.currency, "US dollar"); eq("bls", us.priceSource, "US uses BLS averages");
        eq(Units.WEIGHTS, uk.units, "UK uses weights in grams"); eq("\u00a3", uk.currency, "UK pound"); check(uk.gasMarks && uk.fanNote, "UK gas marks + fan note");
        eq("user", uk.priceSource, "UK: user prices only"); eq("user", eu.priceSource, "EU: user prices only"); eq("\u20ac", eu.currency, "EU euro");
        check(eu.decimalComma, "EU decimal comma"); check(anz.cup250, "ANZ cup note"); check(us.weekStartsSunday && !uk.weekStartsSunday, "week start by region");
        check(!us.clock24 && uk.clock24, "12/24 h by region"); eq("user", ca.priceSource, "Canada: user prices");
        eq("10/30/2026", Region.formatDate("2026-10-30", us.dateFormat), "US date");
        eq("30/10/2026", Region.formatDate("2026-10-30", uk.dateFormat), "UK date");
        eq("30.10.2026", Region.formatDate("2026-10-30", eu.dateFormat), "EU date");
        eq("6:30 PM", Region.formatTime(18 * 60 + 30, false), "12 h time"); eq("18:30", Region.formatTime(18 * 60 + 30, true), "24 h time");
        eq("2026-10-25", Region.weekStart("2026-10-30", true), "Sunday week start"); eq("2026-10-26", Region.weekStart("2026-10-30", false), "Monday week start");
        eq("\u20ac3,50", Region.money("\u20ac", 3.5, true), "money with decimal comma");
        // allergens
        eq(9, Region.allergens("us").size(), "FDA 9");
        eq(14, Region.allergens("uk").size(), "UK 14"); eq(14, Region.allergens("eu").size(), "EU 14");
        for (String id : Arrays.asList("celery", "mustard", "sesame", "lupin", "molluscs", "sulphites")) check(Region.allergens("eu").contains(id), "EU list has " + id);
        check(!Region.allergens("us").contains("mustard"), "FDA list has no mustard");
        check(Region.allergens("ca").contains("mustard") && Region.allergens("ca").contains("sulphites"), "Canada has mustard and sulphites");
        check(Region.allergens("anz").contains("lupin"), "FSANZ has lupin");
        for (String r : Region.IDS) for (String id : Region.allergens(r)) check(Allergens.ALL_IDS.contains(id), "region allergen is known: " + id);
        eq("Cereals containing gluten", Region.allergenLabel("gluten", "uk"), "EU wording");
        Ingredient mus = new Ingredient("Dijon mustard", 1, "tbsp", false), cel = new Ingredient("celeriac", 1, "", false),
                lup = new Ingredient("lupin flour", 50, "g", false), squid = new Ingredient("squid rings", 200, "g", false),
                wine = new Ingredient("dry white wine", 100, "ml", false), mushroom = new Ingredient("oyster mushrooms", 100, "g", false),
                apr = new Ingredient("dried apricots", 50, "g", false), port = new Ingredient("portobello mushrooms", 2, "", false);
        check(Allergens.has("mustard", mus), "mustard flag"); check(Allergens.has("celery", cel), "celeriac is celery");
        check(Allergens.has("lupin", lup), "lupin flag"); check(Allergens.has("molluscs", squid), "squid is a mollusc");
        check(Allergens.has("sulphites", wine) && Allergens.uncertain("sulphites", wine), "wine: sulphites (check label)");
        check(Allergens.has("sulphites", apr), "dried apricots: sulphites"); check(!Allergens.has("sulphites", port), "portobello is not port");
        check(!Allergens.has("molluscs", mushroom), "oyster mushrooms are not molluscs");
        Recipe mr = rec("Vinaigrette", "Sauce", "French", 4, "1 tbsp | Dijon mustard", "3 tbsp | olive oil");
        check(Allergens.reasons(mr, null, Arrays.asList("mustard")).size() == 1, "avoiding mustard hides mustard recipes");
        eq(Arrays.asList(), Allergens.flags(mr), "core 9 flags unchanged for mustard");
        eq(Arrays.asList("mustard"), Allergens.flags(mr, Region.allergens("uk")), "UK list flags mustard");
        // household
        List<Household.Person> hh = Household.parse(Json.parse("[{\"name\":\"Sam <x>\",\"allergens\":[\"mustard\",\"bogus\"],\"hide\":true},{\"name\":\"\"},{\"name\":\"Alex\",\"allergens\":[\"mustard\"]}]"));
        eq(2, hh.size(), "household parse drops nameless people");
        eq("Sam x", hh.get(0).name.replace("  ", " "), "household name sanitized");
        eq(Arrays.asList("Mustard: Sam x, Alex"), Household.alerts(mr, hh, "uk"), "household alert names people");
        check(Household.avoid(new ArrayList<String>(), hh).contains("mustard"), "household hide adds to avoid");
        // aliases: every term in a row is the same key, both ways
        for (String[] row : Region.terms()) {
            if (Arrays.asList("broiler", "plastic wrap", "parchment paper", "skillet").contains(row[0])) continue;
            String k0 = Normalizer.normalize(row[0]);
            for (String t : row) {
                if (ALIAS_DISTINCT.contains(t)) continue;
                eq(k0, Normalizer.normalize(t), "alias resolves to one key: " + t + " ~ " + row[0]);
                aliasPairs++;
            }
        }
        System.out.println("Alias pairs that resolve to one key: " + aliasPairs);
        eq("Courgette", Region.localize("Zucchini", "uk", false), "UK display courgette");
        eq("Zucchini", Region.localize("Courgette", "us", false), "US display zucchini");
        eq("2 aubergines, diced", Region.localize("2 eggplants, diced", "uk", false), "plural alias");
        eq("Coriander leaves", Region.localize("Cilantro leaves", "uk", false), "cilantro -> coriander (UK)");
        eq("Ground coriander", Region.localize("Ground coriander", "us", false), "coriander seed spice is never rewritten to cilantro");
        eq("Bicarbonate of soda", Region.localize("Baking soda", "uk", false), "baking soda -> bicarbonate of soda");
        eq("Baking soda", Region.localize("bicarbonate of soda", "us", false).substring(0, 1).toUpperCase() + Region.localize("bicarbonate of soda", "us", false).substring(1), "and back");
        eq("Plain flour", Region.localize("All-purpose flour", "uk", false), "plain flour");
        eq("Self-raising flour", Region.localize("Self-rising flour", "anz", false), "self-raising");
        eq("Double cream", Region.localize("Heavy cream", "uk", false), "double cream");
        eq("Spring onions", Region.localize("Scallions", "uk", false), "spring onions");
        eq("Swede", Region.localize("Rutabaga", "uk", false), "swede");
        eq("Beef mince", Region.localize("Ground beef", "uk", false), "mince");
        eq("Prawns", Region.localize("Shrimp", "uk", false), "prawns");
        eq("Shrimp", Region.localize("Prawns", "us", false), "shrimp");
        eq("Rocket", Region.localize("Arugula", "anz", false), "rocket");
        eq("Caster sugar", Region.localize("Superfine sugar", "uk", false), "caster sugar");
        eq("Icing sugar", Region.localize("Powdered sugar", "ca", false), "icing sugar in Canada");
        eq("Preheat the grill.", Region.localize("Preheat the broiler.", "uk", true), "steps: broiler -> grill");
        eq("Preheat the broiler.", Region.localize("Preheat the broiler.", "uk", false), "ingredient mode skips kitchen words");
        String same = "Garlic, minced";
        check(Region.localize(same, "uk", false) == same, "no change returns the same object (no allocation)");
        eq("Eggplant", Region.localize("Eggplant", "", false), "no region: unchanged");
        check(Region.measuringNotes("anz").get(1)[1].contains("20 ml"), "Australian tablespoon explained");
        check(Region.measuringNotes("uk").get(0)[1].contains("568"), "UK pint explained");
        boolean knob = false;
        for (String[] n : Region.measuringNotes("us")) if (n[0].contains("knob")) knob = true;
        check(knob, "knob of butter explained");
        for (String r : Region.IDS) check(!Region.holidayDefaults(r).isEmpty(), "holiday defaults for " + r);
        check(Region.holidayDefaults("uk").contains("bonfire-night") && Region.holidayDefaults("uk").contains("pancake-day"), "UK holidays");
        check(Region.holidayDefaults("anz").contains("anzac-day") && Region.holidayDefaults("ca").contains("canada-day"), "ANZ/CA holidays");
        check(Region.holidayDefaults("eu").contains("bastille-day") && Region.holidayDefaults("eu").contains("st-nicholas"), "EU holidays");
        // performance: localize is cheap enough for every visible ingredient row on the HD 8
        long t0 = System.nanoTime();
        for (int k = 0; k < 20000; k++) Region.localize("2 tbsp extra virgin olive oil " + (k & 7), "uk", false);
        long ms = (System.nanoTime() - t0) / 1000000;
        check(ms < 2000, "localize 20k rows in " + ms + " ms on the build machine");
    }

    // ------------------------------------------------------------------ cuisines
    static void cuisines(List<Recipe> parsed) {
        Map<String, Integer> lumps = new TreeMap<String, Integer>();
        int unknown = 0;
        for (Recipe r : parsed) {
            if (Cuisines.LUMPS.contains(r.cuisine)) lumps.merge(r.cuisine, 1, Integer::sum);
            if (!Cuisines.known(r.cuisine)) { unknown++; System.out.println("  cuisine not in the region table: " + r.cuisine + " (" + r.title + ")"); }
        }
        eq(new TreeMap<String, Integer>(), lumps, "no lumped cuisine labels (tag by country)");
        eq(0, unknown, "every cuisine has a region group");
        eq("Caribbean", Cuisines.group("Jamaican"), "Jamaican in Caribbean group");
        eq("Caribbean", Cuisines.group("Trinidadian & Tobagonian"), "T&T in Caribbean");
        eq("Caribbean", Cuisines.group("Bajan"), "Bajan in Caribbean");
        eq("International", Cuisines.group("Martian"), "unknown -> International");
        LinkedHashMap<String, List<String>> g = Cuisines.grouped(Arrays.asList("Thai", "Jamaican", "Italian", "Cuban"));
        eq(Arrays.asList("Caribbean", "Southern Europe", "Southeast Asia"), new ArrayList<String>(g.keySet()), "grouped in region order");
        eq(Arrays.asList("Jamaican", "Cuban"), g.get("Caribbean"), "Caribbean kept as separate countries");
        Set<String> carib = new TreeSet<String>();
        for (Recipe r : parsed) if (Cuisines.group(r.cuisine).equals("Caribbean")) carib.add(r.cuisine);
        check(carib.size() >= 8, "Caribbean recipes from at least 8 countries: " + carib);
        System.out.println("Caribbean cuisines: " + carib);
    }

    // ------------------------------------------------------------------ backup
    static final String SAMPLE = "{\"app\":\"Pantry Recipes\",\"format\":2,\"appVersion\":\"2.0\",\"platform\":\"android\",\"exported\":\"2026-10-30\","
            + "\"pantry\":[{\"label\":\"Milk\",\"key\":\"milk\",\"staple\":true,\"useBy\":\"2026-11-02\",\"qty\":\"1 l\",\"loc\":\"fridge\",\"low\":true},"
            + "{\"label\":\"Ground beef\",\"key\":\"ground beef\",\"staple\":false,\"loc\":\"freezer\",\"frozen\":\"2026-09-01\"},"
            + "{\"label\":\"Cumin\",\"key\":\"cumin\",\"staple\":false,\"loc\":\"bogus\"}],"
            + "\"favorites\":[\"b:1\"],\"shopping\":[{\"name\":\"Eggs\",\"amount\":12,\"checked\":false},{\"name\":\"eggs\",\"checked\":true}],"
            + "\"ratings\":{\"b:1\":{\"stars\":5,\"note\":\"great\"}},\"plan\":[{\"date\":\"2026-11-01\",\"id\":\"b:1\",\"servings\":4}],"
            + "\"dislikes\":[\"mushroom\"],\"allergens\":[\"peanuts\",\"mustard\",\"bogus\"],"
            + "\"settings\":{\"servings\":3,\"diets\":[\"keto\",\"halal\",\"kosher\",\"nope\"],\"zip\":\"10001-1234\",\"budget\":3.456,\"budgetWeek\":120,"
            + "\"expiryReminders\":true,\"region\":\"uk\",\"gasMarks\":true,\"fanNote\":true,\"dateFormat\":\"DD/MM/YYYY\",\"decimalComma\":false,"
            + "\"clock24\":true,\"weekStart\":\"mon\",\"household\":[{\"name\":\"Sam\",\"allergens\":[\"sesame\"],\"hide\":true}],"
            + "\"apiKey\":\"SECRET123\",\"token\":\"SECRET456\"},"
            + "\"recipes\":[],\"prices\":{\"egg\":{\"price\":4.5,\"amount\":12,\"unit\":\"\"}},\"leftovers\":[],\"cooked\":[],"
            + "\"barcodes\":{\"0123456789012\":\"Milk\"},\"photos\":{\"b:1\":\"data:image/jpeg;base64,AAAA\"}}";

    static void backup() throws Exception {
        Backup.Data d = Backup.parse(SAMPLE);
        eq("fridge", d.pantry.get(0).loc, "pantry location kept"); check(d.pantry.get(0).low, "running low kept");
        eq("2026-09-01", d.pantry.get(1).frozen, "frozen date kept"); eq("", d.pantry.get(2).loc, "bad location dropped");
        eq(Arrays.asList("peanuts", "mustard"), d.allergens, "allergens incl. EU groups, bogus dropped");
        eq("uk", d.settings.get("region"), "region in backup");
        eq("10001", d.settings.get("zip"), "ZIP cleaned");
        eq(3.46, d.settings.get("budget"), "budget rounded");
        check(d.settings.get("household") instanceof List && ((List<?>) d.settings.get("household")).size() == 1, "household in backup");
        String out = Backup.write(d);
        check(!out.contains("SECRET"), "secrets are never written to a backup");
        Backup.Data d2 = Backup.parse(out);
        eq(out, Backup.write(d2), "backup write/parse is stable");
        Backup.Data bad = Backup.parse(SAMPLE.replace("\"region\":\"uk\"", "\"region\":\"mars\""));
        check(!bad.settings.containsKey("region"), "unknown region dropped");
        // an old v1.8 backup still loads
        Backup.Data old = Backup.parse("{\"app\":\"Pantry Recipes\",\"format\":2,\"appVersion\":\"1.8\",\"pantry\":[{\"label\":\"Rice\",\"key\":\"rice\",\"staple\":true}],"
                + "\"settings\":{\"servings\":2,\"units\":\"metric\"}}");
        eq(1, old.pantry.size(), "v1.8 backup loads"); eq("", old.pantry.get(0).loc, "v1.8 pantry has automatic location");
        check(!old.settings.containsKey("region"), "v1.8 backup has no region (welcome flow asks)");
    }

    // ------------------------------------------------------------------ sync: crypto + documents + simulated devices
    static String hex(byte[] b) { return SyncCrypto.hex(b); }
    static byte[] u(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    static final class Sim implements SyncWire.Device {
        final String id, nm;
        TreeMap<String, SyncDoc.Entry> state = new TreeMap<String, SyncDoc.Entry>();
        TreeMap<String, String> cur = new TreeMap<String, String>();
        final Map<String, byte[]> secrets = new HashMap<String, byte[]>();
        final Map<String, Long> since = new HashMap<String, Long>();
        final List<SyncDoc.Conflict> conflicts = new ArrayList<SyncDoc.Conflict>();
        String code = "";
        int fails;
        Sim(String id) { this.id = id; this.nm = "Device " + id; }
        public String id() { return id; }
        public String name() { return nm; }
        public synchronized String openPairingCode() { return code; }
        public synchronized void pairingAttempt(boolean ok) { if (ok) code = ""; else if (++fails >= SyncWire.MAX_PAIR_ATTEMPTS) code = ""; }
        public synchronized void paired(String p, String n, byte[] s) { secrets.put(p, s); }
        public synchronized byte[] secretFor(String p) { return secrets.get(p); }
        void commit(long now) { SyncDoc.track(state, cur, now, id); }
        public synchronized TreeMap<String, SyncDoc.Entry> mergeFrom(String peer, TreeMap<String, SyncDoc.Entry> remote, long now) {
            commit(now);
            Long s = since.get(peer);
            state = SyncDoc.merge(state, remote, s == null ? 0 : s, conflicts, now);
            apply();
            since.put(peer, now);
            return state;
        }
        void adopt(String peer, TreeMap<String, SyncDoc.Entry> merged, long now) { state = new TreeMap<String, SyncDoc.Entry>(merged); apply(); since.put(peer, now); }
        void apply() { cur = new TreeMap<String, String>(); for (Map.Entry<String, SyncDoc.Entry> e : state.entrySet()) if (e.getValue().v != null) cur.put(e.getKey(), e.getValue().v); }
    }

    /** a syncs with b directly (no sockets), at time now. */
    static void syncPair(Sim a, Sim b, long now) {
        a.commit(now);
        TreeMap<String, SyncDoc.Entry> copy = SyncDoc.readDoc(SyncDoc.writeDoc(a.state));
        TreeMap<String, SyncDoc.Entry> merged = b.mergeFrom(a.id, copy, now);
        a.adopt(b.id, SyncDoc.readDoc(SyncDoc.writeDoc(merged)), now);
    }

    static void sync() throws Exception {
        eq("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", hex(SyncCrypto.pbkdf2(u("password"), u("salt"), 1)), "PBKDF2-SHA256 c=1");
        eq("ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43", hex(SyncCrypto.pbkdf2(u("password"), u("salt"), 2)), "PBKDF2-SHA256 c=2");
        eq("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a", hex(SyncCrypto.pbkdf2(u("password"), u("salt"), 4096)), "PBKDF2-SHA256 c=4096");
        String code = SyncCrypto.newCode();
        eq(SyncCrypto.CODE_CHARS, SyncCrypto.cleanCode(code).length(), "pairing code length");
        byte[] key = SyncCrypto.random(32);
        byte[] box = SyncCrypto.seal(key, "lbl", u("hello"));
        eq("hello", new String(SyncCrypto.open(key, "lbl", box), StandardCharsets.UTF_8), "seal/open");
        int rejected = 0;
        for (int k = 0; k < box.length; k++) {
            byte[] t = box.clone(); t[k] ^= 1;
            try { SyncCrypto.open(key, "lbl", t); } catch (SyncCrypto.BadMessage e) { rejected++; }
        }
        eq(box.length, rejected, "every flipped bit is rejected");
        boolean lab = false; try { SyncCrypto.open(key, "other", box); } catch (SyncCrypto.BadMessage e) { lab = true; }
        check(lab, "wrong label rejected");
        boolean bomb = false; try { SyncCrypto.gunzip(SyncCrypto.gzip(new byte[2000000]), 1000); } catch (SyncCrypto.BadMessage e) { bomb = true; }
        check(bomb, "gunzip size limit");

        // backup <-> items
        Backup.Data d = Backup.parse(SAMPLE);
        TreeMap<String, String> items = SyncDoc.items(d);
        for (String k : items.keySet()) check(!k.startsWith("photo"), "photos are not synced: " + k);
        for (String v : items.values()) check(!v.contains("SECRET"), "no secrets in sync items");
        check(items.containsKey("setting/region") && items.containsKey("setting/household") && items.containsKey("allergen/mustard"), "region, household, EU allergens sync");
        TreeMap<String, SyncDoc.Entry> doc = new TreeMap<String, SyncDoc.Entry>();
        SyncDoc.track(doc, items, 1000, "A");
        Backup.Data d2 = Backup.parse(SyncDoc.assemble(doc, "2.0", "android", "2026-10-30"));
        eq(items, SyncDoc.items(d2), "items -> assemble -> parse -> items is stable");

        // two devices: add, delete, concurrent edit, delete vs edit, idempotence
        long t = 1_000_000;
        Sim a = new Sim("A"), b = new Sim("B");
        a.cur.put("pantry/rice", "{\"v\":1}"); a.cur.put("fav/b:1", "true");
        syncPair(a, b, t += 10);
        eq(a.cur, b.cur, "add reaches the other device");
        a.cur.remove("fav/b:1");
        syncPair(a, b, t += 10);
        check(!b.cur.containsKey("fav/b:1"), "delete reaches the other device (tombstone)");
        a.cur.put("pantry/rice", "{\"v\":\"A\"}"); a.commit(t += 10);
        b.cur.put("pantry/rice", "{\"v\":\"B\"}"); b.commit(t += 10);
        syncPair(a, b, t += 10);
        eq("{\"v\":\"B\"}", a.cur.get("pantry/rice"), "newer edit wins");
        eq(a.cur, b.cur, "both converge");
        int cl = a.conflicts.size() + b.conflicts.size();
        check(cl >= 1, "concurrent edit is logged as a conflict");
        boolean logged = false;
        for (SyncDoc.Conflict c : b.conflicts) if (c.lost.equals("{\"v\":\"A\"}")) logged = true;
        check(logged, "the losing value is kept in the conflict log (never silent loss)");
        a.cur.remove("pantry/rice"); a.commit(t += 10);
        b.cur.put("pantry/rice", "{\"v\":\"B2\"}"); b.commit(t += 10);
        syncPair(b, a, t += 10);
        eq("{\"v\":\"B2\"}", a.cur.get("pantry/rice"), "later edit beats earlier delete");
        TreeMap<String, String> snap = new TreeMap<String, String>(a.cur);
        syncPair(a, b, t += 10); syncPair(b, a, t += 10);
        eq(snap, a.cur, "sync is idempotent");
        eq(0, SyncDoc.differences(a.state, b.state), "no differences after repeat syncs");
        // restore from the conflict log
        SyncDoc.Conflict c0 = b.conflicts.get(0);
        SyncDoc.restore(b.state, c0, t += 10, "B");
        b.apply();
        eq(c0.lost, b.cur.get(c0.key), "restore from conflict log");
        // GC of tombstones after 90 days
        TreeMap<String, SyncDoc.Entry> g = new TreeMap<String, SyncDoc.Entry>();
        g.put("x", new SyncDoc.Entry(null, 0, "A")); g.put("y", new SyncDoc.Entry("1", 0, "A"));
        SyncDoc.gc(g, 91L * 86400000L);
        check(!g.containsKey("x") && g.containsKey("y"), "old tombstones collected, live items kept");

        // 3-device fuzz against a last-writer-wins oracle
        Random rnd = new Random(2026);
        int runs = 60, okRuns = 0;
        for (int run = 0; run < runs; run++) {
            Sim[] dv = {new Sim("A"), new Sim("B"), new Sim("C")};
            Map<String, String> oracle = new TreeMap<String, String>();
            long clock = 5_000_000L + run;
            for (int op = 0; op < 60; op++) {
                clock += 1 + rnd.nextInt(3);
                Sim s = dv[rnd.nextInt(3)];
                String k = "pantry/k" + rnd.nextInt(6);
                int what = rnd.nextInt(10);
                if (what < 6) { String v = "\"" + s.id + op + "\""; s.cur.put(k, v); s.commit(clock); oracle.put(k, v); }
                else if (what < 8 && s.cur.containsKey(k)) { s.cur.remove(k); s.commit(clock); oracle.remove(k); }
                else { Sim o = dv[rnd.nextInt(3)]; if (o != s) syncPair(s, o, clock); }
            }
            for (int round = 0; round < 3; round++) { syncPair(dv[0], dv[1], clock += 5); syncPair(dv[1], dv[2], clock += 5); syncPair(dv[2], dv[0], clock += 5); }
            if (dv[0].cur.equals(oracle) && dv[1].cur.equals(oracle) && dv[2].cur.equals(oracle)) okRuns++;
            else System.out.println("  fuzz run " + run + " oracle " + oracle + " got " + dv[0].cur);
        }
        eq(runs, okRuns, "3-device fuzz converges to the last-writer-wins result");
    }

    // ------------------------------------------------------------------ loopback sockets
    static void loopback() throws Exception {
        Sim host = new Sim("hostdevice1"), joiner = new Sim("joinerdev2");
        SyncWire.Server srv = new SyncWire.Server(host, 0);
        try {
            int port = srv.port();
            host.code = SyncCrypto.newCode();
            String[] h = SyncWire.hello("127.0.0.1", port);
            eq("hostdevice1", h[0], "hello id"); eq("true", h[2], "pairing open");
            int wrong = 0;
            try { SyncWire.pair("127.0.0.1", port, "AAAAAAAAAA".equals(host.code) ? "BBBBBBBBBB" : "AAAAAAAAAA", joiner); } catch (SyncWire.SyncException e) { wrong = e.code; }
            eq(401, wrong, "wrong code is rejected");
            String[] p = SyncWire.pair("127.0.0.1", port, host.code.toLowerCase(Locale.ROOT), joiner);
            eq("hostdevice1", p[0], "paired with the host");
            check(host.secretFor("joinerdev2") != null && Arrays.equals(host.secretFor("joinerdev2"), joiner.secretFor("hostdevice1")), "both sides share the secret");
            eq("", host.code, "pairing closes after success");
            int closed = 0;
            try { SyncWire.pair("127.0.0.1", port, "AAAAAAAAAA", new Sim("third3")); } catch (SyncWire.SyncException e) { closed = e.code; }
            eq(403, closed, "pairing closed afterwards");
            // five wrong codes close pairing
            host.code = SyncCrypto.newCode(); host.fails = 0;
            String bad = host.code.startsWith("A") ? "BBBBBBBBBB" : "AAAAAAAAAA";
            for (int k = 0; k < 5; k++) { try { SyncWire.pair("127.0.0.1", port, bad, new Sim("x" + k)); } catch (SyncWire.SyncException e) { } }
            eq("", host.openPairingCode(), "5 wrong codes close pairing");
            // sync over the socket
            host.cur.put("pantry/rice", "\"host\""); host.commit(System.currentTimeMillis());
            joiner.cur.put("fav/b:1", "true"); joiner.commit(System.currentTimeMillis());
            Object[] r = SyncWire.sync("127.0.0.1", port, "hostdevice1", joiner.secretFor("hostdevice1"), "joinerdev2", joiner.state);
            @SuppressWarnings("unchecked") TreeMap<String, SyncDoc.Entry> merged = (TreeMap<String, SyncDoc.Entry>) r[1];
            joiner.adopt("hostdevice1", merged, System.currentTimeMillis());
            eq(host.cur, joiner.cur, "loopback sync converges");
            check(joiner.cur.containsKey("pantry/rice") && host.cur.containsKey("fav/b:1"), "both directions");
            Object[] replay = srv.handleRaw("POST", "/sync", SyncWire.lastBody);
            eq(409, replay[0], "replayed request is refused");
            Object[] unpaired = srv.handleRaw("POST", "/sync", "{\"id\":\"stranger\",\"box\":\"AAAA\"}");
            eq(404, unpaired[0], "unknown device is refused");
            String tampered = SyncWire.lastBody.replace("\"box\":\"", "\"box\":\"AB");
            Object[] tam = srv.handleRaw("POST", "/sync", tampered);
            check(((Integer) tam[0]) == 401 || ((Integer) tam[0]) == 400, "tampered request is refused (" + tam[0] + ")");
            eq(404, srv.handleRaw("GET", "/anything", null)[0], "unknown path");
            int bodyErr = (Integer) srv.handleRaw("POST", "/sync", "not json")[0];
            eq(400, bodyErr, "garbage body");
        } finally {
            srv.close();
        }
        List<SyncWire.Peer> peers = new ArrayList<SyncWire.Peer>();
        SyncWire.Peer pe = new SyncWire.Peer(); pe.id = "x"; pe.name = "Tablet"; pe.host = "192.168.1.5"; pe.port = 47321; pe.secret = "c2VjcmV0"; peers.add(pe);
        List<SyncWire.Peer> back = SyncWire.readPeers(SyncWire.writePeers(peers));
        eq("Tablet", back.get(0).name, "peers round trip");
    }

    // ------------------------------------------------------------------ Game day
    static final List<String> BANNED = Arrays.asList("packers", "steelers", "cowboys", "patriots", "yankees", "lakers", "celtics", "dodgers", "raiders",
            "broncos", "chiefs", "49ers", "niners", "seahawks", "bears", "giants", "eagles", "jets", "dolphins", "bills", "ravens", "bengals", "browns",
            "titans", "colts", "jaguars", "texans", "chargers", "rams", "cardinals", "falcons", "saints", "buccaneers", "panthers", "vikings", "lions",
            "commanders", "redskins", "bulls", "knicks", "warriors", "heat", "cubs", "red sox", "mets", "braves", "astros", "united", "arsenal", "chelsea",
            "liverpool", "barcelona", "madrid", "juventus", "bayern", "maple leafs", "canadiens", "blackhawks", "nfl", "nba", "mlb", "nhl", "fifa", "uefa",
            "super bowl", "world series", "stanley cup", "premier league");

    /** v2.0 Stage 2 kitchen helpers. The same expectations are in the web tests/test-core20.js (parity). */
    static void helpers(File root) throws Exception {
        check(RegionalPrices.withinBudget(null, 2.0), "budget: unknown cost is kept");
        check(RegionalPrices.withinBudget(null, 0), "budget off keeps all");
        String txt = read(new File(root, "assets/helpers20.txt"));
        long t0 = System.nanoTime();
        Helpers20 h = Helpers20.parse(txt);
        long ms = (System.nanoTime() - t0) / 1000000L;
        check(h.equivs.size() >= 150, "150+ equivalents (" + h.equivs.size() + ")");
        check(h.rescues.size() >= 25, "25+ rescue problems (" + h.rescues.size() + ")");
        check(h.blends.size() >= 25, "25+ spice blends (" + h.blends.size() + ")");
        check(h.times.size() >= 90, "90+ cooking times (" + h.times.size() + ")");
        check(h.explains.size() >= 60, "60+ ingredient explainers (" + h.explains.size() + ")");
        for (Helpers20.Equiv q : h.equivs) check(!q.amount.isEmpty() && !q.equals.isEmpty() && !q.group.isEmpty(), "equivalent complete: " + q.amount);
        for (Helpers20.Time t : h.times) check(!t.item.isEmpty() && !t.method.isEmpty() && t.time.matches(".*\\d.*"), "time complete: " + t.item);
        for (Helpers20.Rescue r : h.rescues) { check(r.fixes.size() >= 1 && !r.prevent.isEmpty(), "rescue has fixes and prevention: " + r.problem); }
        for (Helpers20.Blend b : h.blends) {
            check(b.parts.size() >= 3, "blend has 3+ spices: " + b.name);
            check(Cuisines.known(b.country), "blend country is a known cuisine: " + b.name + " (" + b.country + ")");
            for (Helpers20.Part p : b.parts) check(p.parts > 0 && (p.flag.isEmpty() || p.flag.matches("heat|salt|opt")), "blend part valid: " + b.name + " " + p.spice);
            List<String[]> rows = Helpers20.build(b, null, 6, false, false);
            check(rows.size() == b.parts.size(), "blend builds every spice: " + b.name);
        }
        for (Helpers20.Explain e : h.explains) {
            check(!e.text.isEmpty() && !e.names.isEmpty(), "explainer has text and names: " + e.name);
            for (String k : e.names.keySet()) check(k.matches("us|uk|ca|anz|eu|in|also"), "explainer region code: " + e.name + " " + k);
        }
        eq("1/4 tsp", Helpers20.spoons(0.25), "spoons 1/4");
        eq("1 1/2 tsp", Helpers20.spoons(1.5), "spoons 1 1/2");
        eq("1 tbsp", Helpers20.spoons(3), "spoons 3 tsp");
        eq("2 tbsp + 1/2 tsp", Helpers20.spoons(6.5), "spoons 6.5 tsp");
        eq("pinch", Helpers20.spoons(0.05), "spoons pinch");
        eq(1.5, Helpers20.parseParts("1 1/2"), "parts 1 1/2");
        Helpers20.Blend cj = h.blends.get(0);
        List<String[]> plain = Helpers20.build(cj, null, 8.5, false, false), ns = Helpers20.build(cj, null, 8.5, true, true);
        eq("2 tsp", plain.get(0)[0], "Cajun 8.5 tsp: paprika 2 tsp");
        eq(plain.size() - 1, ns.size(), "no-salt drops salt");
        eq("1 1/8 tsp", ns.get(1)[0], "no-salt+mild garlic powder");
        eq("coriander", Helpers20.localName(h.findExplains("cilantro").get(0), "anz"), "local name anz");
        eq("cilantro", Helpers20.localName(h.findExplains("cilantro").get(0), "ca"), "local name ca falls back to us");
        eq("aubergine", Helpers20.localName(h.findExplains("eggplant").get(0), "eu"), "local name eu falls back to uk");
        check(h.findEquivs("butter stick").size() >= 1, "equivalent search: butter stick");
        check(h.findTimes("chickpeas instant pot").size() >= 2, "times search: chickpeas instant pot");
        check(h.findRescues("salty").size() == 1, "rescue search: salty");
        check(h.findExplains("courgette").size() == 1, "explainer search by local name");
        String low = txt.toLowerCase(Locale.ROOT);
        for (String w : new String[]{"old bay", "nutella", "tabasco", "bisquick", "crock-pot", "crockpot", "goya", "mccormick", "pyrex"})
            check(!low.contains(w), "helpers text has no brand name: " + w);
        System.out.println("Helpers: " + h.equivs.size() + " equivalents, " + h.rescues.size() + " rescues, " + h.blends.size() + " blends, "
                + h.times.size() + " times, " + h.explains.size() + " explainers; parse " + ms + " ms (JIT cold)");
    }

    static void palette() {
        List<Palette.Base> gd = Palette.inGroup("Game day");
        int light = 0, dark = 0;
        for (Palette.Base b : gd) {
            if (Palette.isDark(b.bg)) dark++; else light++;
            String n = (b.id + " " + b.name).toLowerCase(Locale.ROOT);
            for (String w : BANNED) check(!(" " + n.replace('-', ' ') + " ").contains(" " + w + " "), "Game day name is neutral: " + b.name + " (" + w + ")");
            check(Palette.warnings(b).isEmpty(), "Game day preset passes AA: " + b.id);
        }
        check(gd.size() >= 50, "at least 50 Game day presets (" + gd.size() + ")");
        check(light > 0 && dark > 0, "Game day has light and dark");
        System.out.println("Presets: " + Palette.PRESETS.size() + " total in " + Palette.GROUPS.size() + " groups; Game day " + gd.size() + " (" + light + " light, " + dark + " dark)");
    }

    static void stage3(List<Recipe> parsed, File root) throws Exception {
        Stage3 s = Stage3.parse(read(new File(root, "assets/stage3.txt")));
        check(s.holidays.size() >= 45, "45+ holidays (" + s.holidays.size() + ")");
        eq(6, s.fasts.size(), "six fasting labels");
        check(s.trouble.size() >= 18 && s.storage.size() >= 22 && s.altitude.size() == 3 && s.party.size() >= 8, "baking data sections parsed");
        eq("2026-04-05", Stage3.easter(2026), "Easter 2026");
        eq("2027-03-28", Stage3.easter(2027), "Easter 2027");
        eq("2030-04-21", Stage3.easter(2030), "Easter 2030");
        eq("2026-04-12", Stage3.orthodoxEaster(2026), "Orthodox Easter 2026");
        eq("2027-05-02", Stage3.orthodoxEaster(2027), "Orthodox Easter 2027");
        eq("2028-04-16", Stage3.orthodoxEaster(2028), "Orthodox Easter 2028 (same as western)");
        String[][] want = {{"thanksgiving-us", "2026", "2026-11-26"}, {"thanksgiving-us", "2027", "2027-11-25"}, {"thanksgiving-ca", "2026", "2026-10-12"},
                {"memorial-day", "2026", "2026-05-25"}, {"labor-day", "2026", "2026-09-07"}, {"mothers-day", "2026", "2026-05-10"},
                {"fathers-day", "2026", "2026-06-21"}, {"pancake-day", "2026", "2026-02-17"}, {"good-friday", "2026", "2026-04-03"},
                {"midsummer", "2026", "2026-06-19"}, {"diwali", "2026", "2026-11-08"}, {"diwali", "2029", "2029-11-05"},
                {"lunar-new-year", "2027", "2027-02-06"}, {"eid-fitr", "2026", "2026-03-20"}, {"hanukkah", "2026", "2026-12-05"},
                {"christmas", "2026", "2026-12-25"}, {"mothering-sunday", "2026", "2026-03-15"}};
        for (String[] w : want) {
            Stage3.Holiday h = s.holiday(w[0]);
            check(h != null, "holiday exists: " + w[0]);
            if (h != null) eq(w[2], Stage3.dateIn(h, Integer.parseInt(w[1])), "date of " + w[0] + " " + w[1]);
        }
        check(Stage3.dateIn(s.holiday("diwali"), 2031) == null, "lunar table has no 2031 date (skipped, not guessed)");
        eq("2026-11-26", Stage3.next(s.holiday("thanksgiving-us"), "2026-11-02"), "next Thanksgiving from Nov 2 2026");
        eq("2027-03-28", Stage3.next(s.holiday("easter"), "2026-11-02"), "next Easter rolls to next year");
        List<String> ids = new ArrayList<String>();
        for (Stage3.Holiday h : s.holidays) ids.add(h.id);
        List<Stage3.Upcoming> up = s.upcoming(ids, "2026-11-02", 14);
        check(!up.isEmpty() && up.get(0).days >= 0, "upcoming list within 14 days");
        for (int i = 1; i < up.size(); i++) check(up.get(i - 1).days <= up.get(i).days, "upcoming sorted soonest first");
        check(s.upcoming(Collections.<String>emptyList(), "2026-11-02", 400).isEmpty(), "nothing ticked means no banners");
        int usOn = 0, euOn = 0;
        for (Stage3.Holiday h : s.holidays) { if (Stage3.defaultOn(h, "us")) usOn++; if (Stage3.defaultOn(h, "eu")) euOn++; }
        check(Stage3.defaultOn(s.holiday("thanksgiving-us"), "us") && !Stage3.defaultOn(s.holiday("thanksgiving-us"), "eu"), "US Thanksgiving default on for US only");
        check(usOn > 5 && euOn > 5, "regions get default holidays (us " + usOn + ", eu " + euOn + ")");
        StringBuilder counts = new StringBuilder();
        int thin = 0;
        for (Stage3.Holiday h : s.holidays) {
            List<Recipe> sec = s.section(h, parsed, 60);
            counts.append(h.id).append('=').append(sec.size()).append(' ');
            if (sec.size() < 4) thin++;
            if (!h.diet.isEmpty()) for (Recipe r : sec) if (!s.dietOk(h.diet, r)) { check(false, "section diet respected: " + h.id + " " + r.title); break; }
            for (Recipe r : sec) for (Ingredient i : r.ingredients) if (!i.optional) for (String x : h.excl)
                if (Stage3.hasWord(i.key, x)) check(false, "section excludes " + x + ": " + h.id + " " + r.title);
        }
        System.out.println("Holiday sections: " + counts);
        check(thin <= 5, "at most 5 holidays with fewer than 4 recipes (" + thin + ")");
        StringBuilder fc = new StringBuilder();
        Map<String, Integer> fn = new LinkedHashMap<String, Integer>();
        for (Stage3.Fast f : s.fasts) { int n = 0; for (Recipe r : parsed) if (Stage3.fits(f, r)) n++; fn.put(f.id, n); fc.append(f.id).append('=').append(n).append(' '); }
        System.out.println("Fasting labels: " + fc);
        for (Map.Entry<String, Integer> e : fn.entrySet()) check(e.getValue() >= 20, "20+ recipes fit fasting label " + e.getKey());
        for (Recipe r : parsed) {
            if (Stage3.fits(s.fast("vrat"), r)) for (Ingredient i : r.ingredients) if (!i.optional && (Diets.isGrain(i.key) || Diets.isLegume(i.key) || i.key.contains("onion") || i.key.contains("garlic"))) { check(false, "vrat leak: " + r.title + " / " + i.key); break; }
            if (Stage3.fits(s.fast("jain"), r)) for (Ingredient i : r.ingredients) if (!i.optional && (i.key.equals("potatoes") || i.key.equals("potato") || i.key.contains("carrot") || i.key.contains("onion"))) { check(false, "jain leak: " + r.title + " / " + i.key); break; }
            if (Stage3.fits(s.fast("orthodox"), r)) check(r.tags.contains(Recipe.VEGAN), "orthodox strict implies vegan: " + r.title);
        }
        double[] b = Stage3.bakers(500, 70, 2, 1, 0);
        check(Math.abs(b[0] - 350) < 1e-9 && Math.abs(b[1] - 10) < 1e-9 && Math.abs(b[2] - 5) < 1e-9 && Math.abs(b[4] - 865) < 1e-9, "baker's percentages 500 g at 70%");
        check(Math.abs(Stage3.flourFor(865, 70, 2, 1, 0) - 500) < 1e-9, "flour for a target dough weight");
        double r20 = Stage3.panArea("round", 20, 0), r23 = Stage3.panArea("round", 23, 0);
        check(Math.abs(Stage3.panFactor(r20, r23) - 1.3225) < 1e-6, "20 cm to 23 cm round is x1.32");
        check(Math.abs(Stage3.panFactor(Stage3.panArea("rect", 23, 33), Stage3.panArea("square", 20, 0)) - 400.0 / 759.0) < 1e-9, "rect to square factor");
        int[] tl = Stage3.timeline(8 * 60, new int[]{20, 90, 45, 35});
        eq("08:00 08:20 09:50 10:35 11:10", Stage3.clock(tl[0], true) + " " + Stage3.clock(tl[1], true) + " " + Stage3.clock(tl[2], true) + " " + Stage3.clock(tl[3], true) + " " + Stage3.clock(tl[4], true), "dough timeline clock times");
        eq(6 * 60 + 30, Stage3.startFor(9 * 60, new int[]{60, 90}), "work back from a ready time");
        eq("11:30 PM (-1 day)", Stage3.clock(-30, false), "clock rolls back a day");
        eq("1:00 AM (+1 day)", Stage3.clock(1500, false), "clock rolls over a day");
        check(s.altitudeFor(2000) == null && "5000".equals(s.altitudeFor(6000)[0]) && "7000".equals(s.altitudeFor(9000)[0]), "altitude bands");
        eq(15, Stage3.partyNeed(s.party.get(0), 10), "cupcakes for 10 guests");
        eq(2, Stage3.partyNeed(new String[]{"Pie", "0.125"}, 9), "pies always round up");
        eq("4", Stage3.gasMark(180), "180 C is gas 4");
        check(Math.abs(Stage3.cToF(180) - 356) < 1e-9 && Math.abs(Stage3.fToC(212) - 100) < 1e-9, "C/F conversion");
    }

    static boolean hasField(Class<?> c, String n) { try { c.getField(n); return true; } catch (NoSuchFieldException e) { return false; } }
}
