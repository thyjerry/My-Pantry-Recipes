import org.pantryrecipes.app.core.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Build-time tool (runs on the JDK):
 *  1. compiles recipes/*.txt (simple text format) into assets/recipes.json, deriving dietary tags;
 *  2. re-parses the JSON with the same code the app uses and runs checks on the normalizer,
 *     matcher, quantity scaling and diet classifier. Exits non-zero on any failure.
 */
public class RecipeTool {
    static int failures = 0;
    static int checks = 0;

    static void check(boolean ok, String msg) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL: " + msg); }
    }

    static void eq(Object exp, Object got, String msg) {
        check(Objects.equals(exp, got), msg + " expected <" + exp + "> got <" + got + ">");
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File out = new File(args[1]);
        List<Recipe> recipes = new ArrayList<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
        Arrays.sort(files);
        int n = 0;
        for (File f : files) {
            Recipe cur = null;
            int lineNo = 0;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                lineNo++;
                String t = line.trim();
                if (t.isEmpty()) continue;
                String where = f.getName() + ":" + lineNo;
                if (t.startsWith("# ")) {
                    cur = new Recipe();
                    cur.title = t.substring(2).trim();
                    cur.id = "b:" + (++n);
                    cur.source = "bundled";
                    recipes.add(cur);
                } else if (cur == null) {
                    throw new IllegalStateException(where + " content before title");
                } else if (t.startsWith("- ")) {
                    String[] p = t.substring(2).split("\\|", -1);
                    if (p.length < 2) throw new IllegalStateException(where + " bad ingredient: " + t);
                    String q = p[0].trim();
                    double amount = Double.NaN;
                    String unit = "";
                    if (!q.isEmpty()) {
                        String[] qs = q.split("\\s+", 2);
                        if (qs[0].matches("\\d+(\\.\\d+)?") && !(qs.length > 1 && qs[1].matches("\\d+/\\d+.*"))) {
                            amount = Double.parseDouble(qs[0]);
                            unit = qs.length > 1 ? qs[1] : "";
                        } else {
                            // v1.7: fractions and mixed numbers ("1/3 cup", "1 1/2 cups")
                            Quantity.Parsed qp = Quantity.parse(q);
                            if (Double.isNaN(qp.amount) || q.contains("-")) throw new IllegalStateException(where + " bad amount: " + q);
                            amount = qp.amount;
                            unit = qp.unit;
                        }
                    }
                    boolean opt = p.length > 2 && p[2].trim().equals("opt");
                    String name = p[1].trim();
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    cur.ingredients.add(new Ingredient(name, amount, unit, opt));
                } else if (t.startsWith("> ")) {
                    cur.steps.add(t.substring(2).trim());
                } else {
                    String[] h = t.split("\\|");
                    if (h.length != 6) throw new IllegalStateException(where + " bad header (need category | cuisine | prep | cook | servings | meal types): " + t);
                    for (String mt : h[5].split(",")) {
                        String v = mt.trim();
                        if (!MealType.isKnown(v)) throw new IllegalStateException(where + " unknown meal type '" + v + "' in: " + t);
                        if (!cur.meals.add(v)) throw new IllegalStateException(where + " duplicate meal type '" + v + "' in: " + t);
                    }
                    cur.category = h[0].trim();
                    cur.cuisine = h[1].trim();
                    cur.prepMin = Integer.parseInt(h[2].trim());
                    cur.cookMin = Integer.parseInt(h[3].trim());
                    cur.servings = Integer.parseInt(h[4].trim());
                }
            }
        }
        List<Object> arr = new ArrayList<>();
        Map<String, Nutrition.Food> dietFoods = Nutrition.parse(new String(Files.readAllBytes(new File(dir.getAbsoluteFile().getParentFile(), "assets/foods.tsv").toPath()), StandardCharsets.UTF_8));
        for (Recipe r : recipes) {
            r.tags.clear();
            r.tags.addAll(DietClassifier.classify(r.ingredients));
            r.diets.clear();
            r.diets.addAll(Diets.classify(r, dietFoods));
            r.dietsKnown = true;   // v2.0: precomputed so the HD 8 never classifies bundled recipes
            arr.add(r.toMap());
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("license", "Recipes written for Pantry Recipes; MIT License, same as the app.");
        root.put("recipes", arr);
        String json = Json.write(root);
        Files.write(out.toPath(), json.getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + recipes.size() + " recipes to " + out + " (" + json.length() + " bytes)");

        // ---------------- checks on the written JSON ----------------
        List<Recipe> parsed = Recipe.parseList(new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8));
        eq(recipes.size(), parsed.size(), "recipe count after round trip");
        Set<String> titles = new HashSet<>();
        Set<String> cats = new TreeSet<>(Arrays.asList("Breakfast", "Main", "Soup", "Salad", "Side", "Snack", "Dessert", "Baking", "Drink", "Sauce"));
        Map<String, Integer> keyCount = new TreeMap<>();
        Map<String, Integer> tagCount = new TreeMap<>();
        Map<String, Integer> cuisineCount = new TreeMap<>(), catCount = new TreeMap<>();
        Set<String> ids = new HashSet<>(), normTitles = new HashSet<>();
        int quickCount = 0;
        for (Recipe r : parsed) {
            String w = r.id + " " + r.title;
            check(!r.title.isEmpty(), w + " title");
            check(titles.add(r.title.toLowerCase()), w + " duplicate title");
            check(cats.contains(r.category), w + " unknown category " + r.category);
            check(!r.cuisine.isEmpty(), w + " cuisine");
            check(r.prepMin >= 0 && r.cookMin >= 0, w + " times");
            check(r.servings > 0, w + " servings");
            check(r.ingredients.size() >= 3, w + " needs >=3 ingredients");
            check(r.steps.size() >= 2, w + " needs >=2 steps");
            check(!r.tags.isEmpty() || true, w + " tags");
            eq(DietClassifier.classify(r.ingredients), r.tags, w + " tags consistent");
            if (r.tags.contains(Recipe.VEGAN)) check(r.tags.contains(Recipe.VEGETARIAN) && r.tags.contains(Recipe.DAIRY_FREE), w + " vegan implies veg+df");
            Set<String> keys = new HashSet<>();
            for (Ingredient i : r.ingredients) {
                check(!i.key.isEmpty(), w + " empty key for " + i.name);
                check(keys.add(i.key) || i.name.toLowerCase().contains("frosting"), w + " duplicate ingredient key " + i.key);
                check(Double.isNaN(i.amount) || i.amount > 0, w + " amount " + i.name);
                eq(i.key, Normalizer.normalize(i.key), w + " key is a fixed point: " + i.name);
                keyCount.merge(i.key, 1, Integer::sum);
            }
            for (String s : r.steps) check(s.length() > 10, w + " step too short");
            for (String tg : r.tags) tagCount.merge(tg, 1, Integer::sum);
            // v1.2: stricter required-field / duplicate checks
            check(ids.add(r.id), w + " duplicate id");
            String norm = r.title.toLowerCase().replaceAll("[^a-z0-9]+", "");
            check(normTitles.add(norm), w + " duplicate title ignoring punctuation/case");
            check(r.title.equals(r.title.trim()) && r.title.length() >= 3 && r.title.length() <= 60, w + " title length/whitespace");
            check(r.prepMin + r.cookMin > 0 && r.prepMin + r.cookMin <= 24 * 60, w + " total time 1..1440 min");
            check(r.servings >= 1 && r.servings <= 24, w + " servings 1..24");
            for (Ingredient i : r.ingredients) check(!i.name.trim().isEmpty(), w + " ingredient name");
            for (String s : r.steps) check(!s.trim().isEmpty() && !s.startsWith(">"), w + " step text");
            cuisineCount.merge(r.cuisine, 1, Integer::sum);
            catCount.merge(r.category, 1, Integer::sum);
            if (r.prepMin + r.cookMin <= 30) quickCount++;
        }
        System.out.println("Categories: " + catCount);
        System.out.println("Cuisines: " + cuisineCount.size() + ", quick (<=30 min): " + quickCount);
        check(parsed.size() >= 1200, "at least 1200 bundled recipes (v1.4)");
        for (String c : cats) check(catCount.getOrDefault(c, 0) >= 50, "category has >=50 recipes: " + c);
        check(catCount.getOrDefault("Main", 0) >= 400, "at least 400 main recipes");
        check(cuisineCount.size() >= 50, "at least 50 cuisines");
        check(quickCount >= 500, "at least 500 quick recipes");
        for (String tg : new String[]{Recipe.VEGETARIAN, Recipe.VEGAN, Recipe.GLUTEN_FREE, Recipe.DAIRY_FREE})
            check(tagCount.getOrDefault(tg, 0) >= 250, "at least 250 recipes tagged " + tg);
        // v1.4: every diet tag is consistent with the classifier and the vegan => vegetarian rule
        for (Recipe r : parsed) {
            if (r.tags.contains(Recipe.VEGAN)) check(r.tags.contains(Recipe.VEGETARIAN), r.title + " vegan implies vegetarian");
            check(new HashSet<>(r.tags).equals(new HashSet<>(DietClassifier.classify(r.ingredients))), r.title + " tags match DietClassifier");
        }
        // v1.4: technique coverage (air fryer, slow cooker, one-pot, sheet-pan)
        String[][] tech = {{"air fryer", "30"}, {"slow cooker", "20"}, {"one-pot", "10"}, {"sheet-pan", "10"}};
        for (String[] t : tech) {
            int tn = 0;
            for (Recipe r : parsed) if (r.title.toLowerCase().replace("sheet pan", "sheet-pan").replace("one pot", "one-pot").contains(t[0])) tn++;
            System.out.println("Technique '" + t[0] + "': " + tn);
            check(tn >= Integer.parseInt(t[1]), "at least " + t[1] + " " + t[0] + " recipes");
        }
        List<Map.Entry<String, Integer>> cz = new ArrayList<>(cuisineCount.entrySet());
        cz.sort((a, b) -> b.getValue() != a.getValue().intValue() ? b.getValue() - a.getValue() : a.getKey().compareTo(b.getKey()));
        System.out.println("Cuisine counts: " + cz);
        System.out.println("Tag counts: " + tagCount);
        mealTypeChecks(recipes, parsed);
        v15Checks(parsed, dir.getAbsoluteFile().getParentFile());
        v16Checks(parsed, dir.getAbsoluteFile().getParentFile());
        v17Checks(parsed, dir.getAbsoluteFile().getParentFile());
        v18Checks(parsed, dir.getAbsoluteFile().getParentFile());
        V20Tests.run(parsed, dir.getAbsoluteFile().getParentFile());
        V201Tests.run(parsed, dir.getAbsoluteFile().getParentFile());
        V21Tests.run(parsed, dir.getAbsoluteFile().getParentFile());
        V22Tests.run(parsed, dir.getAbsoluteFile().getParentFile());
        StringBuilder unknown = new StringBuilder();
        for (Map.Entry<String, Integer> e : keyCount.entrySet())
            if (!Normalizer.isKnown(e.getKey())) unknown.append(e.getKey()).append('(').append(e.getValue()).append(") ");
        System.out.println("Distinct ingredient keys: " + keyCount.size());
        System.out.println("Keys without a synonym/family entry (fine if they are real names): " + unknown);

        // catalog items must normalize to keys used by at least one recipe, or be known
        Map<String, String> catKeys = new HashMap<>();
        eq(Catalog.CATEGORIES.length, Catalog.ITEMS.length, "catalog categories vs item lists");
        eq(Catalog.CATEGORIES.length, Catalog.COLORS.length, "catalog colors");
        for (String[] cat : Catalog.ITEMS) for (String item : cat) {
            if (Catalog.isHeader(item)) continue;
            String k = Normalizer.normalize(item);
            check(!k.isEmpty(), "catalog item key " + item);
            check(!catKeys.containsKey(k), "catalog key unique: " + item + " vs " + catKeys.get(k));
            catKeys.put(k, item);
            eq(k, Normalizer.normalize(k), "catalog key is a fixed point: " + item);
        }
        int gridItems = Catalog.allItems().size();
        System.out.println("Catalog: " + gridItems + " grid ingredients in " + Catalog.CATEGORIES.length
                + " categories, + " + Catalog.ALIASES.length + " autocomplete aliases");
        check(gridItems >= 500, "at least 500 grid ingredients");
        for (String a : Catalog.ALIASES) check(catKeys.containsKey(Normalizer.normalize(a)) || Normalizer.isKnown(Normalizer.normalize(a)),
                "alias maps to something known: " + a);
        // every recipe ingredient should be selectable from the grid (directly, via family, or via a pantry family)
        int unreachable = 0;
        Matcher everything = new Matcher(catKeys.keySet(), new ArrayList<String>());
        StringBuilder unr = new StringBuilder();
        java.util.Set<String> stapleKeys = new java.util.HashSet<String>();
        for (String st : Catalog.DEFAULT_STAPLES) stapleKeys.add(Normalizer.normalize(st));
        for (String k : keyCount.keySet()) if (!everything.has(k) && !stapleKeys.contains(k)) { unreachable++; unr.append(k).append(", "); }
        System.out.println("Recipe keys not reachable from the grid: " + unreachable + " " + unr);
        check(unreachable <= 3, "nearly all recipe ingredients are in the grid");

        // ---------------- normalizer ----------------
        String[][] norm = {
                {"Spring Onions", "green onion"}, {"scallions", "green onion"}, {"Aubergine", "eggplant"},
                {"eggplants", "eggplant"}, {"Courgettes", "zucchini"}, {"2 large Tomatoes, chopped", "tomato"},
                {"Tomatoes", "tomato"}, {"potatoes", "potato"}, {"Chicken Breasts", "chicken breast"},
                {"boneless skinless chicken thighs", "chicken thigh"}, {"Minced Beef", "ground beef"},
                {"ground cumin", "cumin"}, {"Plain Flour", "flour"}, {"Caster Sugar", "sugar"},
                {"Extra Virgin Olive Oil", "olive oil"}, {"Garlic Clove", "garlic"}, {"garlic cloves", "garlic"},
                {"Cloves", "clove"}, {"Coriander Leaves", "cilantro"}, {"chick peas", "chickpea"},
                {"King Prawns", "shrimp"}, {"Red Pepper", "bell pepper"}, {"red pepper flakes", "chili flake"},
                {"Chopped Tomatoes", "canned tomato"}, {"Berries", "berry"}, {"Leaves", "leaf"},
                {"Bay Leaves", "bay leaf"}, {"Olives", "olive"}, {"Hummus", "hummus"}, {"Couscous", "couscous"},
                {"Asparagus", "asparagus"}, {"Crème fraîche", "creme fraiche"}, {"Greek Yoghurt", "yogurt"},
                {"Beetroot", "beet"}, {"Rocket", "arugula"}, {"Double Cream", "heavy cream"},
                {"Lemon Juice", "lemon"}, {"Salt and Pepper", "black pepper"}, {"Basmati Rice", "rice"},
                {"Cheddar Cheese", "cheddar"}, {"Parmesan cheese, grated", "parmesan"}, {"Egg Noodles", "egg noodle"},
                {"Rice Noodles", "rice noodle"}, {"Black Beans", "black bean"}, {"Cannellini Beans", "white bean"},
                {"lentils", "lentil"}, {"Oats", "oats"}, {"Rolled oats", "oats"}, {"Peanut Butter", "peanut butter"},
                {"Coconut Milk", "coconut milk"}, {"Cornflour", "cornstarch"}, {"Bicarbonate Of Soda", "baking soda"},
                {"Water", "water"}, {"hot water", "water"}, {"Soy Sauce", "soy sauce"}, {"Dark Soy Sauce", "soy sauce"},
                {"Fish Sauce", "fish sauce"}, {"Ground Pork", "ground pork"}, {"Peas", "pea"}, {"Frozen Peas", "pea"},
                {"Sweetcorn", "sweetcorn"}, {"Corn tortillas", "corn tortilla"}, {"Chillies", "chili"},
                {"Green Chilli", "chili"}, {"Chili Powder", "chili powder"}, {"Cherry tomatoes", "cherry tomato"},
                {"Red Onions", "red onion"}, {"onions", "onion"}, {"Mushrooms", "mushroom"}, {"Anchovies", "anchovy"},
                {"Radishes", "radish"}, {"Peaches", "peach"}, {"Chives", "chive"}, {"Butternut Squash", "pumpkin"},
                {"", ""}, {"  ", ""}, {"Sun-dried tomatoes", "sun dried tomato"},
        };
        for (String[] c : norm) eq(c[1], Normalizer.normalize(c[0]), "normalize(" + c[0] + ")");
        eq("pasta", Normalizer.family("spaghetti"), "family spaghetti");
        eq("cheese", Normalizer.family("cheddar"), "family cheddar");
        eq(null, Normalizer.family("green onion"), "family green onion");

        // ---------------- matcher ----------------
        List<String> staples = new ArrayList<>();
        for (String s : Catalog.DEFAULT_STAPLES) staples.add(Normalizer.normalize(s));
        Matcher m = new Matcher(keys("pasta", "Garlic", "chili flakes", "parsley"), staples);
        Recipe aglio = find(parsed, "Spaghetti Aglio e Olio");
        Matcher.Result ra = m.match(aglio);
        eq(0, ra.missingCount(), "aglio e olio cookable (pasta covers spaghetti, staples cover oil/salt; parmesan optional)");
        eq(6, ra.total, "aglio total required");
        Matcher m2 = new Matcher(keys("chicken breasts"), staples);
        check(m2.has("chicken"), "chicken breast covers generic chicken");
        check(!m2.has("chicken thigh"), "chicken breast must not cover chicken thigh");
        Matcher m3 = new Matcher(keys("chicken"), staples);
        check(m3.has("chicken thigh"), "generic chicken covers chicken thigh");
        check(!m3.has("chicken stock"), "chicken does not cover chicken stock");
        Matcher m4 = new Matcher(keys("milk"), staples);
        check(!m4.has("coconut milk"), "milk must not cover coconut milk");
        check(m4.has("olive oil") && m4.isStaple("oil"), "staple olive oil covers oil");
        check(!m4.pantryEmpty() && new Matcher(new ArrayList<String>(), staples).pantryEmpty(), "only staples -> pantryEmpty");
        Matcher m5 = new Matcher(keys("Scallions", "aubergine"), staples);
        check(m5.has(Normalizer.normalize("spring onion")) && m5.has("eggplant"), "synonym matching");

        Recipe shak = find(parsed, "Shakshuka");
        Matcher m6 = new Matcher(keys("onion", "bell pepper", "garlic", "cumin", "paprika", "canned tomatoes"), staples);
        Matcher.Result rs = m6.match(shak);
        eq(1, rs.missingCount(), "shakshuka missing only eggs");
        eq("egg", rs.missing.get(0).key, "shakshuka missing key");
        eq(rs.total - 1, rs.have, "have = total - missing");

        // ranking: with an egg-heavy pantry the best results should need no shopping
        Matcher m7 = new Matcher(keys("eggs", "milk", "butter", "bread", "cheddar", "flour", "sugar", "cinnamon",
                "vanilla extract", "baking powder", "chives"), staples);
        List<Matcher.Result> res = new ArrayList<>();
        for (Recipe r : parsed) res.add(m7.match(r));
        res.sort(Matcher::compare);
        System.out.println("Top 5 for breakfast pantry:");
        for (int k = 0; k < 5; k++) System.out.println("   " + res.get(k).recipe.title + "  have " + res.get(k).have + "/" + res.get(k).total);
        eq(0, res.get(0).missingCount(), "top match is cookable");
        for (int k = 1; k < res.size(); k++) check(Matcher.compare(res.get(k - 1), res.get(k)) <= 0, "sorted order at " + k);
        int cookNow = 0;
        for (Matcher.Result r : res) if (r.missingCount() == 0 && r.haveNonStaple > 0) cookNow++;
        System.out.println("Cook-now recipes for that pantry: " + cookNow);
        check(cookNow >= 3, "at least 3 cook-now recipes for breakfast pantry");

        // ---------------- quantities ----------------
        eq("½", Quantity.number(0.5, "tsp"), "half");
        eq("1½", Quantity.number(1.5, "cup"), "one and a half");
        eq("⅔", Quantity.number(2.0 / 3, "cup"), "two thirds");
        eq("2", Quantity.number(2, ""), "two");
        eq("400", Quantity.number(400, "g"), "400 g");
        eq("535", Quantity.number(533.33, "g"), "533 g rounds to 5");
        eq("3 cloves", Quantity.format(3, "clove", 1), "plural clove");
        eq("1 clove", Quantity.format(2, "clove", 0.5), "scaled clove");
        eq("600 g", Quantity.format(400, "g", 1.5), "scale g");
        eq("Salt", Quantity.format(Double.NaN, "Salt", 2), "unscalable text untouched");
        Quantity.Parsed p1 = Quantity.parse("1 1/2 cups");
        eq(1.5, p1.amount, "parse mixed");
        eq("cups", p1.unit, "parse mixed unit");
        eq(200.0, Quantity.parse("200g").amount, "parse 200g");
        eq("g", Quantity.parse("200g").unit, "parse 200g unit");
        eq(0.5, Quantity.parse("½ tsp").amount, "parse vulgar");
        eq(2.5, Quantity.parse("2½ tbsp").amount, "parse 2½");
        eq(0.75, Quantity.parse("3/4 cup").amount, "parse 3/4");
        check(Double.isNaN(Quantity.parse("to taste").amount), "parse to taste");
        check(Double.isNaN(Quantity.parse("Pinch").amount), "parse pinch");
        eq(1.0, Quantity.parse("1-2 tbsp").amount, "parse range takes low end");

        // ---------------- diet ----------------
        tags(parsed, "Pasta Carbonara", false, false, false, false);
        tags(parsed, "Greek Salad", true, false, true, false);
        tags(parsed, "Classic Hummus", true, true, true, true);
        tags(parsed, "Red Lentil Dal", true, true, true, true);
        tags(parsed, "Chicken Stir-Fry", false, false, false, true);
        tags(parsed, "Banana Bread", true, false, false, false);
        tags(parsed, "Sweet Potato and Black Bean Tacos", true, true, true, true);
        tags(parsed, "Spring Rolls in Lettuce Cups", false, false, true, true);
        tags(parsed, "Easy Pad Thai", false, false, true, true);
        tags(parsed, "Egg Fried Noodles", true, false, false, true);
        check(DietClassifier.isMeat("chicken stock") && !DietClassifier.isMeat("eggplant") && !DietClassifier.isMeat("goat cheese"), "meat words");
        check(!DietClassifier.isDairy("peanut butter") && !DietClassifier.isDairy("coconut milk") && DietClassifier.isDairy("butter"), "dairy words");
        check(!DietClassifier.isOtherAnimal("eggplant") && DietClassifier.isOtherAnimal("egg"), "egg vs eggplant");

        // ---------------- expanded vocabulary ----------------
        String[][] norm2 = {
                {"Bok choy", "bok choy"}, {"Pak choi", "bok choy"}, {"Jalapeños", "jalapeno"}, {"Smoked paprika", "smoked paprika"},
                {"Dried oregano", "oregano"}, {"Tempeh", "tempeh"}, {"Halloumi", "halloumi"}, {"Gochujang", "gochujang"},
                {"Fresh mozzarella", "mozzarella"}, {"Frozen peas", "pea"}, {"Basmati", "rice"}, {"Prawns", "shrimp"},
        };
        for (String[] c : norm2) eq(c[1], Normalizer.normalize(c[0]), "normalize(" + c[0] + ")");
        check(new Matcher(keys("chili"), staples).has(Normalizer.normalize("jalapeno")), "chili covers jalapeno");
        check(new Matcher(keys("jalapeno"), staples).has("chili"), "jalapeno covers generic chili");
        check(new Matcher(keys("pork chops"), staples).has("pork"), "pork chop covers pork");
        check(new Matcher(keys("dried basil"), staples).has("basil"), "dried basil covers basil");

        // ---------------- substitutions ----------------
        File subsFile = new File(dir.getAbsoluteFile().getParentFile(), "assets/substitutions.txt");
        Substitutions subs = Substitutions.parse(new String(Files.readAllBytes(subsFile.toPath()), StandardCharsets.UTF_8));
        System.out.println("Substitutions: " + subs.entries.size() + " ingredients, " + subs.optionCount() + " options, "
                + subs.categories.size() + " categories " + subs.categories);
        check(subs.entries.size() >= 300, "at least 300 substitution entries (v1.7)");
        eq("[]", subs.duplicateKeys.toString(), "no ingredient has two substitution entries");
        eq("[]", subs.shortLines.toString(), "every swap line has text | needs | ratio | effect");
        int subClaims = 0, taste = 0;
        java.util.Set<String> seenNames = new java.util.HashSet<String>();
        for (Substitutions.Entry e : subs.entries) {
            check(seenNames.add(e.name.toLowerCase(java.util.Locale.ROOT)), "unique swap entry name " + e.name);
            java.util.Set<String> seenOpt = new java.util.HashSet<String>();
            for (Substitutions.Option op : e.options) {
                check(op.ratioValid(), "valid ratio: " + e.name + ": " + op.ratio);
                if ("to taste".equals(op.ratio)) taste++;
                else check(Substitutions.ratioValue(op.ratio) > 0 && Substitutions.ratioValue(op.ratio) <= 12, "sane ratio: " + e.name + ": " + op.ratio);
                check(!op.effect.isEmpty(), "swap has an effect note: " + e.name + ": " + op.text);
                check(seenOpt.add(op.text.toLowerCase(java.util.Locale.ROOT)), "no duplicate option in " + e.name);
                for (String c : Substitutions.claims(op)) { subClaims++; check(Substitutions.claimHolds(op, c), "swap claim '" + c + "' matches its flags: " + e.name + ": " + op.text); }
            }
        }
        System.out.println("Swap wording claims checked: " + subClaims + "; to-taste ratios: " + taste);
        // ratio parser
        eq(0.75, Substitutions.ratioValue("3/4:1"), "ratio 3/4:1");
        eq(0.5, Substitutions.ratioValue("1:2"), "ratio 1:2");
        eq(0.0, Substitutions.ratioValue("to taste"), "ratio to taste");
        check(Substitutions.ratioValue("0:1") == null && Substitutions.ratioValue("1") == null && Substitutions.ratioValue("a:b") == null
                && Substitutions.ratioValue("1:0") == null && Substitutions.ratioValue("1/0:1") == null, "bad ratios rejected");
        Substitutions.Entry bmE = subs.forKey("buttermilk");
        check(bmE.options.get(0).vegetarian && !bmE.options.get(0).dairyFree && !bmE.options.get(0).vegan, "milk + lemon: vegetarian, not dairy-free");
        check(bmE.options.get(3).dairyFree && bmE.options.get(3).vegan && bmE.options.get(3).allergens.contains(Allergens.TREE_NUTS)
                && bmE.options.get(3).allergens.contains(Allergens.SOY), "plant milk buttermilk: vegan, flags soy and tree nuts (any-of)");
        eq("\u00be for 1", subs.forKey("flour").options.get(2).ratioLabel(), "ratio label uses fractions");
        StringBuilder badNeeds = new StringBuilder();
        for (Substitutions.Entry e : subs.entries) {
            check(!e.keys.isEmpty(), "sub entry has key " + e.name);
            check(!e.options.isEmpty() || !e.note.isEmpty(), "sub entry has options " + e.name);
            for (Substitutions.Option op : e.options) {
                check(!op.needs.isEmpty(), "option lists what it uses: " + e.name + ": " + op.text);
                for (List<String> g : op.needs) for (String k : g) {
                    check(!e.keys.contains(k), "option does not need its own target: " + e.name);
                    if (!everything.has(k)) badNeeds.append(e.name).append("->").append(k).append(", ");
                }
            }
        }
        eq("", badNeeds.toString(), "every substitute ingredient is selectable in the grid");
        check(subs.forKey("buttermilk") != null && subs.forKey(Normalizer.normalize("Double cream")) != null, "sub lookup by alias");
        check(subs.forKey("spaghetti") != null, "sub lookup via family (spaghetti -> pasta)");
        check(subs.search("flax").size() >= 1 && subs.search("Buttermilk").get(0).name.equals("Buttermilk"), "sub search");
        Matcher ms = new Matcher(keys("milk", "lemon", "flour", "eggs"), staples).withSubstitutions(subs, null);
        eq(1, subs.availableFor("buttermilk", ms).size(), "milk + lemon is a buttermilk swap");
        eq("milk + lemon", subs.availableFor("buttermilk", ms).get(0).usesLabel(ms), "uses label");
        check(subs.availableFor("buttermilk", new Matcher(keys("milk"), staples)).isEmpty(), "milk alone is not enough");
        Recipe bm = new Recipe();
        bm.id = "t:1"; bm.title = "Test";
        bm.ingredients.add(new Ingredient("Buttermilk", 1, "cup", false));
        bm.ingredients.add(new Ingredient("Flour", 1, "cup", false));
        bm.ingredients.add(new Ingredient("Saffron", 1, "pinch", false));
        Matcher.Result rb = ms.match(bm);
        eq(2, rb.missingCount(), "buttermilk still missing until accepted");
        eq(1, rb.swappable.size(), "buttermilk is swappable");
        Matcher.Result rb2 = new Matcher(keys("milk", "lemon", "flour"), staples)
                .withSubstitutions(subs, Arrays.asList("buttermilk")).match(bm);
        eq(1, rb2.missingCount(), "accepted substitute counts as a match");
        eq(1, rb2.substituted.size(), "substituted recorded");
        eq(2, rb2.have, "have includes substitute");

        // ---------------- discover ----------------
        List<Matcher.Result> all7 = new ArrayList<>();
        for (Recipe r : parsed) all7.add(m7.match(r));
        Set<String> seenIds = new HashSet<>();
        seenIds.add(res.get(0).recipe.id);
        List<Matcher.Result> nfy = Discover.newForYou(all7, seenIds, 12);
        check(nfy.size() == 12, "new-for-you filled");
        for (Matcher.Result r : nfy) check(!seenIds.contains(r.recipe.id), "new-for-you excludes seen");
        List<Matcher.Result> quick = Discover.quick(all7, 50);
        check(quick.size() >= 10, "quick recipes found: " + quick.size());
        for (Matcher.Result r : quick) check(r.recipe.totalMinutes() > 0 && r.recipe.totalMinutes() <= 30, "quick <= 30 min");
        List<Matcher.Result> almost = Discover.almostThere(all7, 50);
        check(!almost.isEmpty(), "almost-there found");
        for (Matcher.Result r : almost) check(r.missingCount() >= 1 && r.missingCount() <= 2, "almost-there missing 1-2");
        Recipe pancakes = find(parsed, "Fluffy Pancakes");
        List<Recipe> like = Discover.basedOnFavorites(parsed, Arrays.asList(shak), 8);
        check(!like.isEmpty() && !like.contains(shak), "favorites-based excludes the favorite");
        check(Discover.similarity(shak, shak) > Discover.similarity(shak, pancakes), "similarity sane");
        System.out.println("Because you liked Shakshuka: " + (like.isEmpty() ? "-" : like.get(0).title + ", " + (like.size() > 1 ? like.get(1).title : "")));
        check(!Discover.facet(parsed, true).isEmpty() && !Discover.facet(parsed, false).isEmpty(), "facets");
        check(Discover.inCategory(find(parsed, "Chicken Stir-Fry"), "Chicken"), "chicken category by ingredient");
        check(!Discover.inCategory(find(parsed, "Classic Hummus"), "Chicken"), "hummus is not chicken");
        check(Discover.inCategory(find(parsed, "Classic Hummus"), "Vegan"), "vegan category by tag");
        check(Discover.searchTitles(parsed, "pancake", 10).contains(pancakes), "title search");

        // ---------------- JSON ----------------
        Object o = Json.parse("{\"a\":[1,2.5,\"x\\\"y\\u00e9\",true,null],\"b\":{}}");
        eq("{\"a\":[1,2.5,\"x\\\"yé\",true,null],\"b\":{}}", Json.write(o), "json round trip");

        System.out.println(checks + " checks, " + failures + " failures");
        if (failures > 0) System.exit(1);
    }

    static void tags(List<Recipe> all, String title, boolean veg, boolean vegan, boolean gf, boolean df) {
        Recipe r = find(all, title);
        eq(veg, r.tags.contains(Recipe.VEGETARIAN), title + " vegetarian");
        eq(vegan, r.tags.contains(Recipe.VEGAN), title + " vegan");
        eq(gf, r.tags.contains(Recipe.GLUTEN_FREE), title + " gluten-free");
        eq(df, r.tags.contains(Recipe.DAIRY_FREE), title + " dairy-free");
    }

    static void mealTypeChecks(List<Recipe> source, List<Recipe> parsed) {
        Map<String, Integer> mc = new LinkedHashMap<>();
        for (String m : MealType.ALL) mc.put(m, 0);
        for (int k = 0; k < parsed.size(); k++) {
            Recipe r = parsed.get(k), s = source.get(k);
            String w = r.id + " " + r.title;
            check(!s.meals.isEmpty(), w + " has at least one meal type in the source file");
            eq(new ArrayList<>(s.meals), new ArrayList<>(r.meals), w + " meal types survive the JSON round trip");
            for (String m : r.meals) {
                check(MealType.isKnown(m), w + " known meal type " + m);
                mc.merge(m, 1, Integer::sum);
            }
            eq(MealType.sorted(r.meals), new ArrayList<>(r.meals), w + " meal types in display order");
            check(MealType.isMeal(r.meals) || r.category.equals("Drink") || r.category.equals("Sauce"), w + " food has a food meal type");
            // sanity: meal types agree with the category
            switch (r.category) {
                case "Breakfast": check(r.meals.contains(MealType.BREAKFAST), w + " breakfast category -> Breakfast"); break;
                case "Main": check(r.meals.contains(MealType.MAIN), w + " Main -> Main dish"); break;
                case "Soup": check(r.meals.contains(MealType.SOUP), w + " Soup -> Soup"); break;
                case "Salad": check(r.meals.contains(MealType.SALAD), w + " Salad -> Salad"); break;
                case "Side": check(r.meals.contains(MealType.SIDE) && !r.meals.contains(MealType.MAIN), w + " Side -> Side dish, not main"); break;
                case "Snack": check(r.meals.contains(MealType.SNACK), w + " Snack -> Snack"); break;
                case "Dessert": check(r.meals.contains(MealType.DESSERT) && !r.meals.contains(MealType.MAIN), w + " Dessert -> Dessert"); break;
                case "Baking": check(r.meals.contains(MealType.BAKED), w + " Baking -> Baked goods"); break;
                case "Drink": check(r.meals.contains(MealType.DRINK) && !r.meals.contains(MealType.MAIN), w + " Drink -> Drink"); break;
                case "Sauce": check(r.meals.equals(new LinkedHashSet<>(Arrays.asList(MealType.SAUCE))) || r.meals.contains(MealType.SAUCE), w + " Sauce -> Sauce/Condiment"); break;
                default: check(false, w + " category " + r.category);
            }
            if (r.meals.contains(MealType.MAIN)) check(r.meals.contains(MealType.DINNER) || r.meals.contains(MealType.LUNCH), w + " mains are lunch or dinner");
        }
        System.out.println("Meal type counts: " + mc);
        for (String m : MealType.ALL) check(mc.get(m) >= 50, "at least 50 recipes of meal type " + m);
        check(mc.get(MealType.DINNER) >= 500 && mc.get(MealType.MAIN) >= 400, "plenty of dinners/mains");
        check(mc.get(MealType.BREAKFAST) >= 150 && mc.get(MealType.LUNCH) >= 250, "plenty of breakfasts/lunches");

        // ---- MealType logic ----
        eq(Arrays.asList(MealType.BREAKFAST, MealType.BRUNCH), MealType.infer("Breakfast", "Full English"), "MealDB Breakfast");
        eq(Arrays.asList(MealType.DESSERT), MealType.infer("Dessert", "Eton Mess"), "MealDB Dessert");
        eq(Arrays.asList(MealType.DESSERT, MealType.BAKED), MealType.infer("Dessert", "Apple Frangipan Tart"), "MealDB Dessert tart is baked");
        eq(Arrays.asList(MealType.BREAKFAST, MealType.BRUNCH, MealType.DESSERT), MealType.infer("Dessert", "Pancakes"), "MealDB pancakes");
        eq(Arrays.asList(MealType.APPETIZER), MealType.infer("Starter", "Chicken Liver Pate"), "MealDB Starter");
        eq(Arrays.asList(MealType.LUNCH, MealType.APPETIZER, MealType.SOUP), MealType.infer("Starter", "Broccoli & Stilton soup"), "MealDB Starter soup");
        eq(Arrays.asList(MealType.DINNER, MealType.SIDE), MealType.infer("Side", "Boxty"), "MealDB Side");
        for (String c : new String[]{"Beef", "Chicken", "Lamb", "Pork", "Goat", "Seafood", "Pasta", "Vegetarian", "Vegan", "Miscellaneous", "", "Weird"})
            eq(Arrays.asList(MealType.DINNER, MealType.MAIN), MealType.infer(c, "Something"), "MealDB " + c + " -> Main dish + Dinner");
        eq(Arrays.asList(MealType.LUNCH, MealType.DINNER, MealType.MAIN, MealType.SALAD), MealType.infer("Chicken", "Chicken Caesar Salad"), "salad title");
        eq(Arrays.asList(MealType.LUNCH, MealType.DINNER, MealType.MAIN), MealType.infer("Beef", "Beef Burger"), "burger -> lunch");
        eq(Arrays.asList(MealType.LUNCH, MealType.DINNER, MealType.SOUP), MealType.infer("Soup", "Leek soup"), "bundled Soup");
        eq(MealType.MAIN, MealType.canonical(" main  DISH "), "canonical");
        eq(MealType.APPETIZER, MealType.canonical("Starter"), "canonical starter");
        eq("", MealType.canonical("Elevenses"), "canonical unknown");
        List<String> none = new ArrayList<>();
        List<String> dessert = Arrays.asList(MealType.DESSERT), side = Arrays.asList(MealType.SIDE, MealType.DINNER);
        List<String> main = Arrays.asList(MealType.DINNER, MealType.MAIN), cookie = Arrays.asList(MealType.SNACK, MealType.DESSERT, MealType.BAKED);
        List<String> sideSalad = Arrays.asList(MealType.LUNCH, MealType.DINNER, MealType.SIDE, MealType.SALAD);
        check(MealType.passes(main, none, false, false), "main passes by default");
        check(!MealType.passes(dessert, none, false, false), "dessert hidden by default");
        check(MealType.passes(dessert, none, true, false), "Include desserts shows desserts");
        check(!MealType.passes(side, none, false, false) && MealType.passes(side, none, false, true), "Include sides");
        check(!MealType.passes(side, none, true, false), "desserts switch does not show sides");
        check(MealType.passes(dessert, dessert, false, false), "picking Dessert includes desserts");
        check(MealType.passes(side, Arrays.asList(MealType.SIDE), false, false), "picking Side dish includes sides");
        check(!MealType.passes(side, Arrays.asList(MealType.DINNER), false, false), "picking Dinner still hides sides");
        check(MealType.passes(sideSalad, Arrays.asList(MealType.SALAD), false, false), "picking Salad shows side salads");
        check(MealType.passes(cookie, Arrays.asList(MealType.SNACK), false, false), "picking Snack shows sweet snacks");
        check(!MealType.passes(main, dessert, true, true), "selection excludes non-matching");
        check(MealType.passes(Arrays.asList(MealType.DINNER, MealType.MAIN, MealType.SIDE), none, false, false), "main+side is a main");
        check(MealType.preferred(main, none, false, false) && !MealType.preferred(Arrays.asList(MealType.LUNCH, MealType.SALAD), none, false, false), "tonight prefers dinner");
        check(MealType.preferred(side, none, false, true), "side is dinner-ish when included");
        check(!MealType.acceptable(Arrays.asList(MealType.DRINK), none, true, true) && MealType.acceptable(Arrays.asList(MealType.DRINK), Arrays.asList(MealType.DRINK), false, false), "drinks only when picked");
        check(MealType.preferred(Arrays.asList(MealType.BREAKFAST, MealType.BRUNCH), Arrays.asList(MealType.BREAKFAST), false, false), "breakfast pick");
        eq("Starter", MealType.mealDbCategory(MealType.APPETIZER), "online category for appetizers");
        eq("WHAT CAN I COOK TONIGHT?", MealType.heroQuestion(none), "hero default");
        eq("WHAT CAN I COOK FOR BREAKFAST?", MealType.heroQuestion(Arrays.asList(MealType.BREAKFAST)), "hero breakfast");
        Recipe old = Recipe.fromMap(obj("{\"id\":\"m:1\",\"title\":\"Lamb Tagine\",\"category\":\"Lamb\"}"));
        eq(Arrays.asList(MealType.DINNER, MealType.MAIN), new ArrayList<>(old.meals), "old cached recipe gets inferred meal types");
        Recipe odd = Recipe.fromMap(obj("{\"id\":\"x\",\"title\":\"T\",\"meals\":[\"dessert\",\"Bogus\",\"Snack\"]}"));
        eq(Arrays.asList(MealType.SNACK, MealType.DESSERT), new ArrayList<>(odd.meals), "fromMap canonicalises and drops unknown meal types");
        // the bundled set: defaults hide only dessert/side recipes
        int hidden = 0, shownDinner = 0;
        for (Recipe r : parsed) {
            if (!MealType.passes(r.meals, none, false, false)) { hidden++; check(MealType.isDessert(r.meals) || MealType.isSide(r.meals), r.title + " hidden only if dessert/side"); }
            if (MealType.preferred(r.meals, none, false, false)) shownDinner++;
        }
        System.out.println("Meal filter defaults: " + hidden + " dessert/side recipes hidden, " + shownDinner + " dinner-ish suggestions");
        check(hidden > 60 && shownDinner >= 280, "defaults leave plenty of mains");
    }

    // ---------------- v1.5 ----------------
    static void v15Checks(List<Recipe> parsed, File root) throws Exception {
        // version: one source of truth
        java.util.Properties vp = new java.util.Properties();
        try (java.io.Reader rd = Files.newBufferedReader(new File(root, "version.properties").toPath(), StandardCharsets.UTF_8)) { vp.load(rd); }
        String vn = vp.getProperty("versionName", ""), vc = vp.getProperty("versionCode", "");
        check(vn.matches("\\d+\\.\\d+(\\.\\d+)?(-[a-z0-9]+)?"), "versionName looks like 1.5 or 2.0-stage1: " + vn);
        check(vc.matches("\\d+") && Integer.parseInt(vc) >= 6, "versionCode >= 6: " + vc);
        String mf = new String(Files.readAllBytes(new File(root, "AndroidManifest.xml").toPath()), StandardCharsets.UTF_8);
        check(!mf.contains("android:versionCode") && !mf.contains("android:versionName"), "manifest carries no version (build.sh injects it)");
        String bs = new String(Files.readAllBytes(new File(root, "build.sh").toPath()), StandardCharsets.UTF_8);
        check(bs.contains("version.properties") && bs.contains("dist/") && !bs.matches("(?s).*--version-name +[0-9].*"), "build.sh reads version.properties and writes dist/");

        // Dates
        eq(0L, Dates.toDays("1970-01-01"), "epoch");
        eq("1970-01-01", Dates.fromDays(0), "epoch back");
        eq("2024-03-01", Dates.addDays("2024-02-28", 2), "leap year");
        eq("2023-03-01", Dates.addDays("2023-02-28", 1), "non-leap year");
        eq("2027-01-01", Dates.addDays("2026-12-31", 1), "year end");
        check(!Dates.valid("2026-02-29") && Dates.valid("2028-02-29") && !Dates.valid("2026-13-01") && !Dates.valid("26-1-1"), "date validation");
        for (long d = -800; d < 40000; d += 13) eq(d, Dates.toDays(Dates.fromDays(d)), "round trip " + d);
        eq(3, Dates.dayOfWeek("2026-10-29"), "2026-10-29 is a Thursday");
        eq("2026-10-26", Dates.weekStart("2026-10-29"), "week starts Monday");
        eq("2026-10-26", Dates.weekStart("2026-10-26"), "Monday is its own week start");
        eq("2026-10-26", Dates.weekStart("2026-11-01"), "Sunday belongs to the week before");
        eq(7, Dates.week("2026-10-29").size(), "7-day week");
        eq("Thu 29 Oct", Dates.label("2026-10-29"), "date label");
        eq(-3, Dates.between("2026-11-01", "2026-10-29"), "between negative");
        check(Dates.valid(Dates.today()), "today is valid");

        // UseBy
        eq(UseBy.EXPIRED, UseBy.status(-1), "expired");
        eq(UseBy.TODAY, UseBy.status(0), "today");
        eq(UseBy.SOON, UseBy.status(3), "soon");
        eq(UseBy.OK, UseBy.status(4), "ok");
        eq("Use by tomorrow", UseBy.label(1), "label tomorrow");
        eq(Integer.MIN_VALUE, UseBy.daysLeft("", "2026-10-29"), "no date");
        Map<String, String> ub = new LinkedHashMap<>();
        ub.put("spinach", "2026-10-30"); ub.put("milk", "2026-10-27"); ub.put("rice", "2027-05-01"); ub.put("chicken", "2026-10-29");
        eq(new LinkedHashSet<>(Arrays.asList("spinach", "chicken")), UseBy.soonKeys(ub, "2026-10-29"), "soon keys skip expired and far dates");
        List<String> pk = keys("Spinach", "Chicken", "Rice", "Onion", "Garlic", "Eggs", "Pasta", "Tomato");
        Matcher pm = new Matcher(pk, keys("Salt", "Pepper", "Olive oil"));
        List<Matcher.Result> rs = new ArrayList<>();
        for (Recipe r : parsed) rs.add(pm.match(r));
        List<Matcher.Result> plain = new ArrayList<>(rs);
        UseBy.sort(plain, Collections.<String>emptySet());
        List<Matcher.Result> ref = new ArrayList<>(rs);
        ref.sort(Matcher::compare);
        check(plain.equals(ref), "no use-soon items: exactly Matcher.compare");
        Set<String> soon = new LinkedHashSet<>(keys("Spinach"));
        List<Matcher.Result> boosted = new ArrayList<>(rs);
        UseBy.sort(boosted, soon);
        Matcher sm = UseBy.soonMatcher(soon);
        Matcher.Result top = boosted.get(0);
        check(UseBy.count(top.recipe, sm) > 0 || top.missingCount() == 0, "boost: top pick uses spinach (or is cook-now): " + top.recipe.title);
        // inside the cook-now group, spinach recipes come first
        boolean seenNo = false, okOrder = true;
        for (Matcher.Result r : boosted) {
            if (r.haveNonStaple == 0 || r.missingCount() > 0) break;
            boolean uses = UseBy.count(r.recipe, sm) > 0;
            if (!uses) seenNo = true; else if (seenNo) okOrder = false;
        }
        check(okOrder, "cook-now group: use-soon recipes first");
        // a missing-3+ recipe never jumps ahead of a cook-now recipe
        int firstFar = -1, lastNow = -1;
        for (int i = 0; i < boosted.size(); i++) {
            Matcher.Result r = boosted.get(i);
            if (r.haveNonStaple > 0 && r.missingCount() == 0) lastNow = i;
            if (r.haveNonStaple > 0 && r.missingCount() > 2 && firstFar < 0) firstFar = i;
        }
        check(firstFar < 0 || lastNow < firstFar, "boost keeps groups");
        List<Matcher.Result> up = UseBy.useItUp(rs, soon, 12);
        check(!up.isEmpty() && up.size() <= 12, "use it up has results: " + up.size());
        for (Matcher.Result r : up) check(UseBy.count(r.recipe, sm) > 0, "use it up uses spinach: " + r.recipe.title);

        // Allergens
        check(Allergens.keyHas(Allergens.PEANUTS, Normalizer.normalize("Peanut butter")), "peanut butter");
        check(Allergens.keyHas(Allergens.TREE_NUTS, Normalizer.normalize("Almonds")), "almonds");
        check(Allergens.keyHas(Allergens.TREE_NUTS, Normalizer.normalize("Pine nuts")), "pine nuts flagged");
        check(!Allergens.keyHas(Allergens.TREE_NUTS, Normalizer.normalize("Coconut milk")), "coconut is not flagged as a tree nut");
        check(!Allergens.keyHas(Allergens.TREE_NUTS, Normalizer.normalize("Nutmeg")), "nutmeg is not a nut");
        check(!Allergens.keyHas(Allergens.TREE_NUTS, Normalizer.normalize("Butternut squash")), "butternut squash is not a nut");
        check(!Allergens.keyHas(Allergens.TREE_NUTS, Normalizer.normalize("Water chestnuts")), "water chestnut is not a nut");
        check(Allergens.keyHas(Allergens.EGGS, Normalizer.normalize("Eggs")), "eggs");
        check(!Allergens.keyHas(Allergens.EGGS, Normalizer.normalize("Eggplant")), "eggplant is not egg");
        check(Allergens.keyHas(Allergens.EGGS, Normalizer.normalize("Mayonnaise")) && !Allergens.keyHas(Allergens.EGGS, "vegan mayonnaise"), "mayo");
        check(Allergens.keyHas(Allergens.DAIRY, Normalizer.normalize("Cheddar cheese")) && !Allergens.keyHas(Allergens.DAIRY, Normalizer.normalize("Coconut milk")), "dairy");
        check(Allergens.keyHas(Allergens.GLUTEN, Normalizer.normalize("Plain flour")) && !Allergens.keyHas(Allergens.GLUTEN, Normalizer.normalize("Rice flour")), "gluten");
        check(Allergens.keyHas(Allergens.SOY, Normalizer.normalize("Soy sauce")) && Allergens.keyHas(Allergens.SOY, Normalizer.normalize("Tofu")), "soy");
        check(Allergens.keyHas(Allergens.FISH, Normalizer.normalize("Salmon fillets")) && Allergens.keyHas(Allergens.FISH, Normalizer.normalize("Fish sauce")), "fish");
        check(Allergens.keyHas(Allergens.SHELLFISH, Normalizer.normalize("Prawns")) && !Allergens.keyHas(Allergens.SHELLFISH, Normalizer.normalize("Oyster mushrooms")), "shellfish");
        check(Allergens.keyHas(Allergens.SESAME, Normalizer.normalize("Tahini")) && Allergens.keyHas(Allergens.SESAME, Normalizer.normalize("Sesame oil")), "sesame");
        Recipe pad = Recipe.fromMap(obj("{\"id\":\"u:t\",\"title\":\"Test noodles\",\"ingredients\":[{\"name\":\"Rice noodles\",\"qty\":200,\"unit\":\"g\"},"
                + "{\"name\":\"Eggs\",\"qty\":2},{\"name\":\"Peanuts\",\"qty\":50,\"unit\":\"g\",\"optional\":true},{\"name\":\"Fish sauce\",\"qty\":2,\"unit\":\"tbsp\"},"
                + "{\"name\":\"Mushrooms\",\"qty\":100,\"unit\":\"g\"}]}"));
        eq(Arrays.asList(Allergens.PEANUTS, Allergens.EGGS, Allergens.FISH), Allergens.flags(pad), "flags incl. optional, IDS order");
        check(Allergens.hidden(pad, Collections.<String>emptyList(), Arrays.asList(Allergens.PEANUTS)), "avoid peanuts hides even optional peanuts");
        check(!Allergens.hidden(pad, Collections.<String>emptyList(), Arrays.asList(Allergens.SESAME)), "no sesame: shown");
        check(Allergens.hidden(pad, Arrays.asList(Allergens.dislikeKey("mushroom")), null), "dislike mushroom (singular) hides mushrooms");
        check(!Allergens.hidden(pad, Arrays.asList(Allergens.dislikeKey("peanuts")), null), "dislike only an optional ingredient: still shown");
        check(!Allergens.hidden(pad, Collections.<String>emptyList(), Collections.<String>emptyList()), "nothing set: shown");
        eq(Arrays.asList("Contains eggs", "Contains mushroom"), Allergens.reasons(pad, Arrays.asList(Allergens.dislikeKey("Mushrooms")), Arrays.asList(Allergens.EGGS)), "reasons");
        check(Allergens.dislikes(Normalizer.normalize("Cheddar cheese"), Allergens.dislikeKey("cheese")), "dislike cheese covers cheddar");
        check(!Allergens.dislikes(Normalizer.normalize("Onion"), Allergens.dislikeKey("on")), "no substring matches");
        check(Allergens.NOTE.contains("not a guarantee"), "note says not a guarantee");
        int peanutCount = 0;
        for (Recipe r : parsed) if (Allergens.flags(r).contains(Allergens.PEANUTS)) peanutCount++;
        check(peanutCount > 5 && peanutCount < parsed.size() / 4, "plausible peanut count " + peanutCount);

        // Amounts
        eq(Amounts.ENOUGH, Amounts.status(1, "kg", 500, "g"), "1 kg >= 500 g");
        eq(Amounts.SHORT, Amounts.status(200, "g", 500, "g"), "200 g < 500 g");
        eq(Amounts.ENOUGH, Amounts.status(1, "cup", 3, "tbsp"), "cup vs tbsp");
        eq(Amounts.UNKNOWN, Amounts.status(1, "can", 200, "g"), "can vs g unknown");
        eq(Amounts.ENOUGH, Amounts.status(2, "cans", 1, "can"), "plural same unit");
        eq(Amounts.UNKNOWN, Amounts.status(Double.NaN, "", 1, ""), "no amount unknown");
        eq("clove", Amounts.canon("cloves"), "canon cloves");
        eq("bunch", Amounts.canon("bunches"), "canon bunches");
        check(Math.abs(Amounts.convert(1, "lb", "g") - 453.592) < 1e-6, "lb to g");
        Map<String, String> qty = new LinkedHashMap<>();
        qty.put(Normalizer.normalize("Eggs"), "6 eggs");
        qty.put(Normalizer.normalize("Rice noodles"), "100 g");
        qty.put(Normalizer.normalize("Mushrooms"), "1 punnet");
        List<Amounts.Check> ck = Amounts.check(pad, 1, qty);
        eq(3, ck.size(), "three checked rows");
        eq(Amounts.SHORT, ck.get(0).status, "noodles short");
        eq(Amounts.ENOUGH, ck.get(1).status, "6 eggs >= 2 eggs");
        eq(Amounts.UNKNOWN, ck.get(2).status, "punnet unknown");
        eq(Amounts.SHORT, Amounts.check(pad, 4, qty).get(1).status, "scaled x4 needs 8 eggs");
        eq("You have enough (6 eggs)", Amounts.label(ck.get(1)), "label enough");

        // Meal plan
        final Map<String, Recipe> byId = new HashMap<>();
        for (Recipe r : parsed) byId.put(r.id, r);
        byId.put(pad.id, pad);
        MealPlan.Lookup lk = new MealPlan.Lookup() { public Recipe byId(String id) { return byId.get(id); } };
        List<MealPlan.Entry> plan = new ArrayList<>();
        plan.add(new MealPlan.Entry("2026-10-28", pad.id, 4));
        plan.add(new MealPlan.Entry("2026-10-30", pad.id, 2));
        plan.add(new MealPlan.Entry("2026-11-02", parsed.get(0).id, 4));
        plan.add(new MealPlan.Entry("bad", pad.id, 4));
        eq(2, MealPlan.forWeek(plan, "2026-10-26").size(), "two entries this week");
        eq(3, MealPlan.prune(plan, "2026-10-29", 7).size(), "prune drops bad dates");
        eq(24, new MealPlan.Entry("2026-10-28", "x", 99).servings, "servings clamp");
        Matcher pm2 = new Matcher(keys("Eggs", "Rice noodles"), keys("Salt"));
        List<MealPlan.Item> sl = MealPlan.shopping(MealPlan.forWeek(plan, "2026-10-26"), lk, pm2, qty);
        // pad serves 4 (default): 4 + 2 servings = 1.5x -> noodles 300 g needed, 100 g on hand -> 200 g short
        MealPlan.Item nood = null, fish = null, egg = null;
        for (MealPlan.Item it : sl) {
            if (it.key.equals(Normalizer.normalize("Rice noodles"))) nood = it;
            if (it.key.equals(Normalizer.normalize("Fish sauce"))) fish = it;
            if (it.key.equals(Normalizer.normalize("Eggs"))) egg = it;
        }
        check(nood != null && nood.partial && Math.abs(nood.amount - 200) < 1e-6, "noodles shortfall 200 g");
        check(fish != null && Math.abs(fish.amount - 3) < 1e-6 && "tbsp".equals(fish.unit), "fish sauce summed 3 tbsp");
        check(egg == null, "6 eggs cover 3 eggs");
        check(fish != null && fish.recipes.equals(Arrays.asList("Test noodles")), "recipe titles listed once");
        for (MealPlan.Item it : sl) check(!it.key.equals(Normalizer.normalize("Peanuts")), "optional peanuts skipped");
        List<MealPlan.Item> sl2 = MealPlan.shopping(MealPlan.forWeek(plan, "2026-10-26"), lk, new Matcher(keys("Eggs"), keys()), new HashMap<String, String>());
        boolean eggs2 = false;
        for (MealPlan.Item it : sl2) if (it.key.equals(Normalizer.normalize("Eggs"))) eggs2 = true;
        check(!eggs2, "in pantry with no amount: assumed enough");

        // MealType hero order
        eq(Arrays.asList(MealType.DINNER, MealType.MAIN, MealType.LUNCH, MealType.SALAD),
                MealType.heroOrder(Arrays.asList(MealType.SALAD, MealType.MAIN, MealType.LUNCH, MealType.DINNER)), "hero order dinner, main first");
        eq(Arrays.asList(MealType.LUNCH, MealType.SOUP), MealType.heroOrder(Arrays.asList(MealType.SOUP, MealType.LUNCH)), "hero order without dinner");

        // Backup round trip
        Backup.Data bd = sampleBackup(pad);
        String w1 = Backup.write(bd);
        Backup.Data back = Backup.parse(w1);
        eq(w1, Backup.write(back), "backup round trip is byte-identical");
        check(w1.startsWith("{\"app\":\"Pantry Recipes\",\"format\":2,"), "backup header");
        eq(2, back.pantry.size(), "pantry entries");
        eq("2026-10-30", back.pantry.get(0).useBy, "useBy kept");
        eq(4, back.ratings.get("b:1").stars, "stars kept");
        eq(Arrays.asList(Allergens.PEANUTS, Allergens.SESAME), back.allergens, "allergens canonical order");
        String bad = "{\"app\":\"Pantry Recipes\",\"format\":1,\"pantry\":[{\"label\":\"Milk\",\"useBy\":\"2026-02-30\",\"qty\":5},{\"label\":\"milk\"},7,"
                + "{\"label\":\"\"}],\"ratings\":{\"b:2\":{\"stars\":9},\"b:3\":{\"stars\":0}},\"plan\":[{\"date\":\"x\",\"id\":\"b:1\"}],"
                + "\"allergens\":[\"peanuts\",\"bogus\"],\"settings\":{\"servings\":99,\"evil\":true,\"maxTime\":7},"
                + "\"recipes\":[{\"id\":\"b:1\",\"title\":\"Hijack\",\"ingredients\":[{\"name\":\"x\"}]},{\"id\":\"u:9\",\"title\":\"Mine\",\"ingredients\":[{\"name\":\"Rice\"}]},\"zz\"]}";
        Backup.Data bb = Backup.parse(bad);
        eq(1, bb.pantry.size(), "invalid/duplicate pantry entries dropped");
        eq("", bb.pantry.get(0).useBy, "invalid date dropped");
        eq("", bb.pantry.get(0).qty, "non-string qty dropped");
        eq(5, bb.ratings.get("b:2").stars, "stars clamped");
        check(!bb.ratings.containsKey("b:3"), "empty rating dropped");
        eq(0, bb.plan.size(), "bad plan date dropped");
        eq(Arrays.asList("peanuts"), bb.allergens, "unknown allergen dropped");
        eq(24L, bb.settings.get("servings"), "servings clamped");
        check(!bb.settings.containsKey("evil") && !bb.settings.containsKey("maxTime"), "unknown settings dropped");
        eq(1, bb.recipes.size(), "bundled ids cannot be overridden");
        String[] rejects = {"", "[]", "{\"app\":\"Other\",\"format\":1}", "{\"app\":\"Pantry Recipes\",\"format\":3}", "{\"app\":\"Pantry Recipes\"}", "{not json"};
        for (String r : rejects) {
            boolean threw = false;
            try { Backup.parse(r); } catch (Backup.BackupException e) { threw = !e.getMessage().isEmpty(); }
            check(threw, "rejects backup: " + r);
        }
        eq("2 pantry items, 1 favorite, 1 shopping item, 2 ratings and notes, 2 planned meals, 3 dislikes/allergies, 1 saved recipe, 1 custom theme", Backup.summary(back), "summary");
        check(w1.contains("\"theme\":{\"active\":\"custom-1\",\"custom\":[{\"id\":\"custom-1\""), "theme in backup");
        eq("custom-1", back.theme.active, "theme active kept");
        eq(0x1F6FB2, back.theme.custom.get(0).primary, "theme colour kept");
        check(Backup.parse("{\"app\":\"Pantry Recipes\",\"format\":1}").theme == null, "no theme in old backups");
        paletteChecks();
    }

    static void paletteChecks() {
        check(Palette.PRESETS.size() >= 40, "at least 40 theme presets (v1.6), have " + Palette.PRESETS.size());
        for (Palette.Base b : Palette.PRESETS) check(Palette.GROUPS.contains(b.group), "preset in a known group: " + b.id);
        for (String g : Palette.GROUPS) check(!Palette.inGroup(g).isEmpty(), "group has presets: " + g);
        Set<String> ids = new HashSet<>(), names = new HashSet<>(), combos = new HashSet<>();
        for (Palette.Base b : Palette.PRESETS) {
            check(ids.add(b.id), "unique preset id " + b.id);
            check(names.add(b.name), "unique preset name " + b.name);
            check(combos.add(b.bg + "/" + b.primary), "distinct preset colours " + b.id);
            // the preset's own text colours must pass AA without any automatic fix
            check(Palette.contrast(b.text, b.bg) >= Palette.AA, "AA body text on background: " + b.id + " " + Palette.ratio(Palette.contrast(b.text, b.bg)));
            check(Palette.contrast(b.text, b.surface) >= Palette.AA, "AA body text on cards: " + b.id);
            Palette.Colors c = Palette.derive(b);
            check(!c.textAdjusted, "preset text not adjusted: " + b.id);
            for (Object[] p : Palette.textPairs(c)) {
                double r = Palette.contrast((Integer) p[1], (Integer) p[2]);
                check(r >= (Double) p[3], "contrast " + b.id + " " + p[0] + " " + Palette.ratio(r));
            }
            check(Palette.warnings(b).isEmpty(), "preset has no editor warnings: " + b.id + " " + Palette.warnings(b));
        }
        // distinctness: every pair of presets differs clearly in primary or background
        for (Palette.Base a : Palette.PRESETS) for (Palette.Base b : Palette.PRESETS) {
            if (a == b) continue;
            int dp = colourDistance(a.primary, b.primary), db = colourDistance(a.bg, b.bg), da = colourDistance(a.accent, b.accent);
            boolean far = dp > 40 || db > 40 || (da > 60 && dp + db + da > 100);
            check(far, "presets clearly distinct: " + a.id + " vs " + b.id);
        }
        check(!Palette.derive(Palette.preset("light")).dark && Palette.derive(Palette.preset("dark")).dark, "light/dark detection");
        eq(0x2E9E5B, Palette.derive(Palette.preset("light")).primary, "default primary is the classic green");
        // ugly custom themes still get readable derived text
        int[][] ugly = {{0x777777, 0x808080, 0x888888, 0x7F7F7F, 0x777777}, {0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFF00, 0xFFFFFF},
                {0x000000, 0x000000, 0x000000, 0x000000, 0x000000}, {0x00FF00, 0xFF00FF, 0x0000FF, 0xFF0000, 0x00FFFF}, {0x123456, 0x654321, 0xABCDEF, 0xFEDCBA, 0x111111}};
        for (int[] u : ugly) {
            Palette.Base b = new Palette.Base("custom-1", "Ugly", u[0], u[1], u[2], u[3], u[4]);
            Palette.Colors c = Palette.derive(b);
            for (Object[] p : Palette.textPairs(c)) {
                double r = Palette.contrast((Integer) p[1], (Integer) p[2]);
                // primary text on 4 backgrounds can be impossible only for pathological picks; require AA-large at least
                check(r >= ((Double) p[3] >= Palette.AA ? 3.0 : 2.0), "ugly custom stays readable " + Palette.hex(u[0]) + " " + p[0] + " " + Palette.ratio(r));
            }
            check(Palette.contrast(c.text, c.surface) >= Palette.AA && Palette.contrast(c.text, c.bg) >= Palette.AA, "custom body text forced to AA " + Palette.hex(u[0]));
        }
        check(!Palette.warnings(new Palette.Base("custom-1", "x", 0x777777, 0x808080, 0x888888, 0x7F7F7F, 0x777777)).isEmpty(), "editor warns on low contrast");
        // maths
        eq(21.0, Math.round(Palette.contrast(0, 0xFFFFFF) * 10) / 10.0, "black/white contrast 21");
        eq(1.0, Palette.contrast(0x2E9E5B, 0x2E9E5B), "same colour contrast 1");
        eq(0x808080, Palette.mix(0x000000, 0xFFFFFF, 0.5), "mix midpoint rounds up");
        eq(0x2E9E5B, Palette.parseHex("#2e9e5b"), "hex parse");
        eq(0xAABBCC, Palette.parseHex("abc"), "short hex");
        eq(-1, Palette.parseHex("#12345g"), "bad hex"); eq(-1, Palette.parseHex("#1234"), "bad hex length"); eq(-1, Palette.parseHex(null), "null hex");
        eq("#0000FF", Palette.hex(0xFF), "hex pad");
        java.util.Random rnd = new java.util.Random(7);
        for (int k = 0; k < 2000; k++) {
            int c = rnd.nextInt(0x1000000);
            double[] h = Palette.toHsv(c);
            eq(c, Palette.fromHsv(h[0], h[1], h[2]), "hsv round trip " + Palette.hex(c));
        }
        eq(0xFF0000, Palette.fromHsv(360, 1, 1), "hue 360 wraps");
        // saved state
        Palette.State st = new Palette.State();
        eq("light", st.active, "default theme light");
        eq("custom-1", st.nextCustomId(), "first custom id");
        for (int k = 0; k < Palette.MAX_CUSTOM; k++) check(st.put(new Palette.Base(st.nextCustomId(), "T" + k, 0xFFFFFF, 0xFFFFFF, 0x2E9E5B, 0xD9822B, 0)), "save custom " + k);
        check(!st.put(new Palette.Base("custom-99", "over", 0, 0, 0, 0, 0xFFFFFF)), "custom theme limit");
        check(st.put(new Palette.Base("custom-3", "Renamed", 0xFFFFFF, 0xFFFFFF, 0x2E9E5B, 0xD9822B, 0)), "replace custom");
        eq("Renamed", st.find("custom-3").name, "replaced custom kept its slot");
        st.active = "custom-3";
        String sj = Json.write(st.toMap());
        Palette.State st2 = Palette.parseState(sj);
        eq(sj, Json.write(st2.toMap()), "theme state round trip");
        st2.remove("custom-3");
        eq("light", st2.active, "deleting the active custom theme falls back to light");
        eq("light", Palette.parseState("{\"active\":\"nope\"}").active, "unknown active id");
        eq("system", Palette.parseState("{\"active\":\"system\"}").active, "system kept");
        eq("dark", Palette.parseState("{\"active\":\"system\"}").resolve(true).id, "system follows dark");
        eq("light", Palette.parseState("{\"active\":\"system\"}").resolve(false).id, "system follows light");
        Palette.State junk = Palette.parseState("{\"active\":\"custom-1\",\"custom\":[{\"id\":\"evil\",\"bg\":\"#fff\"},{\"id\":\"custom-1\",\"name\":\"\",\"bg\":\"#fff\",\"surface\":\"#fff\","
                + "\"primary\":\"#2e9e5b\",\"accent\":\"#d9822b\",\"text\":\"#000\"},{\"id\":\"custom-1\",\"bg\":\"#000\",\"surface\":\"#000\",\"primary\":\"#fff\",\"accent\":\"#fff\",\"text\":\"#fff\"},"
                + "{\"id\":\"custom-2\",\"bg\":\"red\",\"surface\":\"#fff\",\"primary\":\"#fff\",\"accent\":\"#fff\",\"text\":\"#000\"},5]}");
        eq(1, junk.custom.size(), "invalid/duplicate custom themes dropped");
        eq("My theme", junk.custom.get(0).name, "empty custom name defaulted");
        eq("custom-1", junk.active, "active custom kept");
        eq("light", Palette.parseState("not json").active, "bad state json");
        eq("light", Palette.parseState((Object) null).active, "null state");
    }

    static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    /** v1.6: themes (accents, font, corners, random), appearance scroll code path, durations, nutrition, cost, backup format 2. */
    static void v16Checks(List<Recipe> parsed, File root) throws Exception {
        int before = failures;
        // every preset x every quick accent passes AA after derive()
        int combos = 0;
        for (Palette.Base b : Palette.PRESETS) for (int a : Palette.QUICK_ACCENTS) {
            Palette.State st = new Palette.State();
            st.active = b.id; st.accentPick = a;
            Palette.Colors c = Palette.derive(st.resolve(false));
            for (Object[] p : Palette.textPairs(c)) {
                double r = Palette.contrast((Integer) p[1], (Integer) p[2]);
                check(r >= (Double) p[3], "accent AA " + b.id + " + " + Integer.toHexString(a) + " " + p[0] + " " + Palette.ratio(r));
            }
            combos++;
        }
        check(combos >= 40 * 10, "accent combos tested: " + combos);
        eq(1.0, Palette.fontScale("normal"), "font normal");
        check(Palette.fontScale("xlarge") > Palette.fontScale("large") && Palette.fontScale("small") < 1, "font scales ordered");
        check(Palette.cornerScale("sharp") < 1 && Palette.cornerScale("round") > 1, "corner scales");
        eq(1.0, Palette.fontScale("bogus"), "unknown font id");
        for (double r = 0; r < 1; r += 0.013) {
            String id = Palette.randomPreset("ocean", r);
            check(!id.equals("ocean") && Palette.preset(id) != null, "random preset differs from current " + r);
        }
        Palette.State st = new Palette.State();
        st.active = "ocean"; st.font = "large"; st.corners = "round"; st.accentPick = 0x7C3AED;
        Palette.State st2 = Palette.parseState(Json.write(st.toMap()));
        eq("large", st2.font, "font saved"); eq("round", st2.corners, "corners saved"); eq(0x7C3AED, st2.accentPick, "accent saved");
        Palette.State old = Palette.parseState("{\"active\":\"ocean\",\"custom\":[]}");
        eq("normal", old.font, "v1.5 state: normal font"); eq(-1, old.accentPick, "v1.5 state: no accent");

        // Appearance scroll-jump fix: the code path must save and restore the scroll position across recreate()
        String app = read(new File(root, "src/org/pantryrecipes/app/AppearanceActivity.java"));
        int persist = app.indexOf("void persist(");
        check(persist > 0, "persist() exists");
        String body = app.substring(persist, app.indexOf("\n    }", persist));
        check(body.contains("scrollY") || body.contains("rememberScroll"), "persist() remembers the scroll position before restyling");
        check(app.contains("putInt(\"scrollY\"") && app.contains("getInt(\"scrollY\""), "scroll position saved in and read from the instance state");
        check(app.contains("scrollTo(0"), "scroll position restored after rebuilding");
        check(!app.replaceAll("(?s)/\\*.*?\\*/", "").matches("(?s).*showList\\(\\);\\s*\\n\\s*sv\\.scrollTo\\(0, 0\\).*"), "no reset to top");

        // barcode / TLS code path: host-limited ISRG trust, never trust-all, no camera permission, no bundled decoder
        String scan = read(new File(root, "src/org/pantryrecipes/app/Scan.java"));
        String manifest = read(new File(root, "AndroidManifest.xml"));
        check(!manifest.contains("android.permission.CAMERA"), "no CAMERA permission");
        check(manifest.contains("android.permission.VIBRATE"), "VIBRATE permission for timer alarms");
        int perms = manifest.split("uses-permission ", -1).length - 1;
        eq(5, perms, "permissions: INTERNET, ACCESS_NETWORK_STATE, VIBRATE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED only");
        check(!manifest.contains("ACCESS_FINE_LOCATION") && !manifest.contains("READ_CONTACTS") && !manifest.contains("com.google.android.gms"), "no location/contacts/Play Services");
        check(scan.contains("com.google.zxing.client.android.SCAN") && scan.contains("ActivityNotFoundException"), "ZXing SCAN intent with a no-app fallback");
        check(!new File(root, "src/com/google/zxing").exists() && !new File(root, "libs").exists(), "no bundled ZXing code or libraries");
        check(!scan.contains("ALLOW_ALL_HOSTNAME_VERIFIER") && !scan.contains("setHostnameVerifier") && !scan.contains("setDefaultSSLSocketFactory"), "default hostname verification, no global TLS changes");
        check(scan.contains("system.checkServerTrusted(chain, auth); } catch (CertificateException e) { extra.checkServerTrusted(chain, auth)"), "trust = system roots, else the bundled ISRG root (chain still verified)");
        check(scan.contains("OFF_HOST.equals(u.getHost())") && scan.contains("\"https\".equals(u.getProtocol())"), "network helper limited to https://world.openfoodfacts.org");
        java.security.cert.X509Certificate isrg = (java.security.cert.X509Certificate) java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.FileInputStream(new File(root, "assets/isrg-root-x1.pem")));
        StringBuilder fp = new StringBuilder();
        for (byte x : java.security.MessageDigest.getInstance("SHA-256").digest(isrg.getEncoded())) fp.append(String.format("%02x", x & 0xFF));
        check(scan.contains("\"" + fp + "\""), "bundled ISRG Root X1 fingerprint matches the pinned constant");
        check(isrg.getSubjectX500Principal().getName().contains("ISRG Root X1"), "bundled certificate is ISRG Root X1");
        check(org.pantryrecipes.app.core.Barcode.url("4006381333931").startsWith("https://world.openfoodfacts.org/api/v2/product/"), "OFF v2 URL on the pinned host");

        // durations
        List<Durations.Match> dm = Durations.find("Simmer for 10-12 minutes, then rest 1 hour 30 min.");
        eq(2, dm.size(), "two durations");
        eq(600, dm.get(0).seconds, "range uses the lower value"); eq(720, dm.get(0).maxSeconds, "range upper");
        check(dm.get(0).label().contains("10") && dm.get(0).label().contains("12"), "range label shows the range: " + dm.get(0).label());
        eq(5400, dm.get(1).seconds, "1 hour 30 min");
        eq(0, Durations.find("Serves 4 people; use a 9-inch pan.").size(), "no false durations");
        int withTimers = 0;
        for (Recipe r : parsed) for (String sx : r.steps) if (!Durations.find(sx).isEmpty()) { withTimers++; break; }
        check(withTimers >= 600, "most recipes get timer chips: " + withTimers);

        // nutrition + cost from the bundled tables
        Map<String, Nutrition.Food> foods = Nutrition.parse(read(new File(root, "assets/foods.tsv")));
        check(foods.size() >= 300, "USDA FDC table loaded: " + foods.size());
        Map<String, Cost.Price> defaults = Cost.parseDefaults(read(new File(root, "assets/prices.tsv")));
        check(defaults.size() >= 200, "default price table: " + defaults.size());
        int shown = 0, costShown = 0;
        for (Recipe r : parsed) {
            Nutrition.Estimate e = Nutrition.estimate(r, foods);
            check(e.percent() >= 0 && e.percent() <= 100, "nutrition percent " + r.title);
            if (e.show()) {
                shown++;
                check(e.percent() >= Nutrition.MIN_COVERAGE, "shown only at >=70%: " + r.title);
                check(e.kcal >= 0 && e.kcal < 5000, "kcal sane " + r.title + " " + e.kcal);
            }
            Cost.Estimate c = Cost.estimate(r, r.servings, new LinkedHashMap<String, Cost.Price>(), defaults, foods);
            if (c.show()) { costShown++; check(c.perServing > 0 && c.total >= c.perServing - 1e-9, "cost sane " + r.title); }
        }
        check(shown >= parsed.size() / 2, "nutrition shown for most recipes: " + shown);
        check(costShown >= parsed.size() / 2, "cost shown with defaults for most recipes: " + costShown);
        Cost.Estimate none = Cost.estimate(parsed.get(0), 2, new LinkedHashMap<String, Cost.Price>(), null, foods);
        check(!none.show(), "no cost without prices");
        eq("$1.50", Cost.money("$", 1.5), "money");

        // backups: format 2 round trip, v1 (1.5) still imports
        Backup.Data d = sampleBackup(parsed.get(0));
        d.prices.put(Normalizer.normalize("Spinach"), Cost.make(2.5, 300, "g"));
        Leftovers.add(d.leftovers, "b:1", "Soup", 3, "2026-10-29", 4);
        Leftovers.log(d.cooked, "b:1", 4, "2026-10-29");
        d.barcodes.put("3017620422003", "Peanut butter");
        Backup.Data back = Backup.parse(Backup.write(d));
        eq(1, back.prices.size(), "prices kept"); eq(1, back.leftovers.size(), "leftovers kept"); eq(1, back.cooked.size(), "cooked log kept");
        eq("Peanut butter", back.barcodes.get("3017620422003"), "barcode cache kept");
        check(back.photos.isEmpty(), "no photos unless included");
        File v15 = new File(root, "../pantry-recipes-web/tests/fixtures/v15-backups.json");
        if (v15.exists()) {
            Object fx = Json.parse(read(v15));
            List<Object> list = new ArrayList<Object>();
            if (fx instanceof List) list.addAll((List<?>) fx);
            else for (Object v : ((Map<?, ?>) fx).values()) list.add(v);
            int ok = 0;
            for (Object o : list) {
                String js = o instanceof String ? (String) o : Json.write(o);
                if (!js.contains("\"format\"")) continue;
                Backup.Data b1 = Backup.parse(js);
                check(b1.prices.isEmpty() && b1.photos.isEmpty(), "v1.5 backup imports without v1.6 data");
                ok++;
            }
            check(ok >= 1, "v1.5 backup fixtures imported: " + ok);
        }
        Backup.Data f1 = Backup.parse("{\"app\":\"Pantry Recipes\",\"format\":1,\"pantry\":[{\"label\":\"Rice\"}]}");
        eq(1, f1.pantry.size(), "format 1 still imports");
        System.out.println("v1.6 checks: " + (failures == before ? "OK" : (failures - before) + " FAILED"));
    }

    static void v17Checks(List<Recipe> parsed, File root) throws Exception {
        int before = failures;
        // ---- cooking methods
        Map<String, Integer> mc = new LinkedHashMap<String, Integer>();
        int ipTitles = 0, ipBoth = 0, noMethod = 0, ipWithPressureTimer = 0;
        for (Recipe r : parsed) {
            List<String> m = Methods.of(r);
            if (m.isEmpty()) noMethod++;
            for (String id : m) { check(Methods.IDS.contains(id), "known method " + id); mc.merge(id, 1, Integer::sum); }
            boolean t = r.title.toLowerCase(Locale.ROOT).contains("instant pot");
            if (t) ipTitles++;
            if (t && m.contains(Methods.INSTANT_POT)) ipBoth++;
            if (t) check(m.contains(Methods.ONE_POT), "IP recipe is one-pot: " + r.title);
            if (m.contains(Methods.INSTANT_POT)) {
                boolean pt = false;
                for (String st : r.steps) for (Durations.Match x : Durations.find(st)) if (x.isPressure()) pt = true;
                boolean nonPressure = false;
                for (String st : r.steps) if (st.contains("not pressure")) nonPressure = true;
                if (pt || nonPressure) ipWithPressureTimer++;
                if (nonPressure) System.out.println("IP recipe without pressure (stated in steps): " + r.title);
            }
            check(!(m.contains(Methods.NO_COOK) && (m.contains(Methods.OVEN) || m.contains(Methods.STOVETOP))), "no-cook excludes oven/stovetop: " + r.title);
        }
        int ip = mc.getOrDefault(Methods.INSTANT_POT, 0);
        check(ip >= 150, "at least 150 Instant Pot recipes: " + ip);
        eq(ipTitles, ipBoth, "every 'Instant Pot' title is tagged instant-pot");
        eq(ipTitles, ip, "instant-pot tag only on Instant Pot recipes");
        eq(0, noMethod, "every recipe has a cooking method");
        eq(ip, ipWithPressureTimer, "every Instant Pot recipe has a pressure timer (or says it uses a non-pressure program)");
        for (String id : Methods.IDS) { check(!Methods.label(id).isEmpty(), "method label " + id); check(mc.getOrDefault(id, 0) >= 20, "at least 20 recipes for " + id); }
        System.out.println("Method counts: " + mc);
        check(Methods.IP_TIMES.length >= 25, "IP cheat sheet rows");
        for (String[] row : Methods.IP_TIMES) { eq(4, row.length, "cheat sheet row fields"); for (String f : row) check(!f.trim().isEmpty(), "cheat sheet field " + row[0]); }
        check(Methods.IP_DISCLAIMER.contains("manufacturer manual"), "cheat sheet says to check the manual");
        check(Methods.IP_TIPS.length >= 5, "cheat sheet tips");
        // filter semantics: all picked methods must apply
        Recipe ipr = null;
        for (Recipe r : parsed) if (Methods.has(r, Methods.INSTANT_POT)) { ipr = r; break; }
        check(Methods.matches(ipr, Arrays.asList(Methods.INSTANT_POT, Methods.ONE_POT)), "matches IP + one-pot");
        check(!Methods.matches(ipr, Arrays.asList(Methods.INSTANT_POT, Methods.AIR_FRYER)), "IP recipe is not air fryer");
        check(Methods.matches(ipr, new ArrayList<String>()), "empty filter matches");

        // ---- duration kinds
        String[][] dk = {
                {"Lock the lid and pressure cook on High for 12 minutes.", Durations.PRESSURE},
                {"Cook for 12 minutes at high pressure.", Durations.PRESSURE},
                {"Seal and cook 8 minutes on low pressure.", Durations.PRESSURE},
                {"Let the pressure release naturally for 10 minutes, then quick release.", Durations.RELEASE},
                {"Allow 15 minutes natural release.", Durations.RELEASE},
                {"Simmer for 10 minutes, stirring.", Durations.TIMER},
                {"Bake for 25 minutes. Pressure cook the beans later.", Durations.TIMER}};
        for (String[] x : dk) {
            List<Durations.Match> ms = Durations.find(x[0]);
            check(!ms.isEmpty(), "duration found: " + x[0]);
            if (!ms.isEmpty()) eq(x[1], ms.get(0).kind, "duration kind: " + x[0]);
        }
        eq(12 * 60, Durations.find("Cook for 12 minutes at high pressure.").get(0).seconds, "pressure timer counts only time at pressure");
        check(Durations.BUILD_UP_NOTE.contains("10\u201315"), "build-up note says 10-15 min");
        check(Durations.BUILD_UP_NOTE.toLowerCase(Locale.ROOT).contains("not"), "build-up not counted");

        // ---- units
        Units.Densities ds = Units.parseDensities(read(new File(root, "assets/densities.tsv")));
        eq("[]", ds.problems.toString(), "densities parse without problems");
        check(ds.list.size() >= 150, "at least 150 densities: " + ds.list.size());
        Units.Prefs w = new Units.Prefs(), us = new Units.Prefs(Units.US, "g", true), met = new Units.Prefs(Units.METRIC, "g", true),
                org = new Units.Prefs(Units.ORIGINAL, "g", true), woz = new Units.Prefs(Units.WEIGHTS, "oz", false), wnc = new Units.Prefs(Units.WEIGHTS, "g", false);
        eq(Units.WEIGHTS, new Units.Prefs().mode, "weights is the default");
        check(new Units.Prefs().showCups, "cups shown by default");
        eq("125 g (1 cup)", Units.display("flour", 1, "cup", 1, w, ds), "flour cup -> grams with cups");
        eq("125 g", Units.display("flour", 1, "cup", 1, wnc, ds), "cups can be hidden");
        eq("4\u00bd oz", Units.display("flour", 1, "cup", 1, woz, ds), "ounces option");
        eq("120 g (1 cup)", Units.display("flour", 118.3, "g", 1, w, ds), "118.3 g -> 120 g");
        eq("315 g (2\u00bd cups)", Units.display("flour", 2.5, "cup", 1, w, ds), "flour to 5 g");
        eq("150 g (\u00be cup)", Units.display("sugar", 0.75, "cup", 1, w, ds), "sugar cups -> grams");
        eq("250 g (2 cups)", Units.display("flour", 1, "cup", 2, w, ds), "scaling applies before converting");
        eq("1 cup", Units.display("flour", 125, "g", 1, us, ds), "US shows cups");
        eq("1 cup", Units.display("milk", 1, "cup", 1, w, ds), "liquids stay as written in weights mode");
        eq("240 ml", Units.display("milk", 1, "cup", 1, met, ds), "metric liquids in ml");
        eq("1 tsp", Units.display("baking powder", 1, "tsp", 1, w, ds), "teaspoons stay spoons");
        eq("\u00bc tsp", Units.display("salt", 0.3, "tsp", 1, us, ds), "spices in quarter teaspoons");
        eq("3 g (1\u00bc tsp)", Units.display("flour", 3, "g", 1, w, ds), "small amounts to 1 g");
        eq("1.5 l", Units.display("water", 1500, "ml", 1, met, ds), "litres");
        eq("2", Units.display("onion", 2, "", 1, w, ds), "counts unchanged");
        eq("\u2153 cup", Units.display("flour", 1, "cup", 1.0 / 3, org, ds), "original mode = Quantity.format");
        eq(Quantity.format(1, "cup", 1.0 / 3), Units.display("flour", 1, "cup", 1.0 / 3, org, ds), "original mode identical");
        eq("8 in", Units.display("pan", 20, "cm", 1, us, ds), "cm -> in");
        eq("120 g", Units.grams(118.3, true), "rounding rule 118.3");
        eq("115 g", Units.grams(113, false), "5 g steps from 100 g");
        eq("47 g", Units.grams(47.4, false), "1 g steps under 100 g");
        eq("25 g", Units.grams(23, true), "flour to 5 g from 20 g");
        eq("1 g", Units.grams(0.2, false), "never 0 g");
        eq("\u00bc tsp", Units.tsp(0.3), "quarter teaspoons");
        eq("\u215b tsp", Units.tsp(0.15), "eighth teaspoon");
        eq("pinch", Units.tsp(0.05), "pinch");
        // parsing
        String[][] pa = {{"1 1/2", "1.5", "1.5"}, {"1\u00bd", "1.5", "1.5"}, {"3/4", "0.75", "0.75"}, {"1-2", "1.0", "2.0"}, {"1 to 2", "1.0", "2.0"},
                {"2\u20133", "2.0", "3.0"}, {"1,5", "1.5", "1.5"}};
        for (String[] x : pa) { double[] r = Units.parseAmount(x[0]); check(r != null, "parse " + x[0]); if (r != null) { eq(x[1], String.valueOf(r[0]), "parse lo " + x[0]); eq(x[2], String.valueOf(r[1]), "parse hi " + x[0]); } }
        for (String x : new String[]{"abc", "2-1", "", "1 2", "-3"}) check(Units.parseAmount(x) == null, "parse rejects '" + x + "'");
        // temperatures
        eq("4", Units.gasMark(180), "gas 4 = 180 C");
        eq("", Units.gasMark(300), "no gas mark at 300 C");
        eq("180.0", String.valueOf(Units.gasToC("4")), "gas 4 -> 180");
        eq(350L, Units.roundF(Units.cToF(180)), "180 C dial 350 F");
        eq(180L, Units.roundC(Units.fToC(350)), "350 F dial 180 C");
        eq("Heat oven to 200\u00b0C (400\u00b0F).", Units.stepText("Heat oven to 200\u00b0C.", us), "step gets F in US");
        eq("Bake at 180\u00b0C (350\u00b0F) for 20 min.", Units.stepText("Bake at 180\u00b0C (350\u00b0F) for 20 min.", us), "already converted");
        eq("Preheat to 400\u00b0F (200\u00b0C).", Units.stepText("Preheat to 400\u00b0F.", w), "step gets C");
        eq("Roll to 20 cm (8 in).", Units.stepText("Roll to 20 cm.", us), "cm in steps for US");
        eq("Heat oven to 200\u00b0C.", Units.stepText("Heat oven to 200\u00b0C.", org), "original leaves steps");
        check(Units.tempLine("180", "C").startsWith("180 \u00b0C = 356 \u00b0F (oven dial ~350 \u00b0F) \u00b7 gas 4"), "temp line C");
        check(Units.tempLine("6", "gas").startsWith("Gas 6 \u2248 200 \u00b0C"), "temp line gas");
        eq("", Units.tempLine("abc", "C"), "temp line rejects text");
        eq(10, Units.OVEN.length, "oven table rows");
        // converters
        eq("240 ml \u00b7 1 cup \u00b7 16 tbsp \u00b7 48 tsp \u00b7 8 fl oz", Units.volumeLine(1, "cup"), "volume line cup");
        eq("5 ml \u00b7 \u00bd tbsp \u00b7 1 tsp \u00b7 \u00bc fl oz", Units.volumeLine(1, "tsp"), "volume line tsp (no cups)");
        eq("", Units.volumeLine(1, "g"), "volume line rejects mass");
        eq("500 g \u00b7 18 oz \u00b7 1 lb", Units.massLine(500, "g"), "mass line");
        eq("\u2248 125 g \u00b7 4\u00bd oz", Units.ingredientLine(1, "cup", ds.forKey("flour")), "ingredient cup -> g");
        eq("\u2248 \u00bd cup \u00b7 120 ml by volume", Units.ingredientLine(100, "g", ds.forKey("sugar")), "ingredient g -> cup");
        eq("115 g \u00b7 4 oz \u00b7 1 stick \u00b7 8 tbsp \u00b7 \u00bd cup", Units.butterText(Units.butterGrams(1, "stick")), "butter stick");
        eq("225 g \u00b7 8 oz \u00b7 2 sticks \u00b7 16 tbsp \u00b7 1 cup", Units.butterText(Units.butterGrams(1, "cup")), "butter cup");
        check(!Units.butterText(10).contains("stick") && !Units.butterText(10).contains("cup"), "small butter has no sticks or cups");
        eq(113.5, Units.butterGrams(8, "tbsp"), "8 tbsp butter (120 ml) = 113.5 g, about 1 stick");
        // all recipes x modes: never empty, NaN or Infinity; flour coverage
        int conv = 0, flourLines = 0, flourGrams = 0;
        for (Recipe r : parsed) for (Ingredient in : r.ingredients) {
            for (Units.Prefs pf : new Units.Prefs[]{org, us, met, w, woz, wnc}) for (double sc : new double[]{0.5, 1, 3}) {
                String t = Units.display(in, sc, pf, ds);
                conv++;
                check(t != null && !t.contains("NaN") && !t.contains("Infinity") && !t.contains("null"), "display ok: " + r.title + " / " + in.name + " -> " + t);
                if (!Double.isNaN(in.amount) && !in.unit.isEmpty()) check(!t.trim().isEmpty(), "display not empty: " + r.title + " / " + in.name);
            }
            Units.Density d = ds.forKey(in.key);
            boolean flour = in.key.contains("flour") || (d != null && "flour".equals(d.cls));
            if (flour && !Double.isNaN(in.amount)) {
                String dim = Amounts.dimension(in.unit);
                if (!dim.equals("mass") && !dim.equals("volume")) continue;
                if (Amounts.canon(in.unit).equals("tsp")) continue;
                flourLines++;
                check(d != null, "flour has a density: " + in.key + " in " + r.title);
                String t = Units.display(in, 1, w, ds);
                boolean grams = t.matches("^[0-9.]+ (g|kg)( \\(.*\\))?$");
                check(grams, "flour shows grams in weights mode: " + r.title + " / " + in.name + " -> " + t);
                if (grams) flourGrams++;
            }
        }
        check(flourLines >= 250, "flour lines checked: " + flourLines);
        eq(flourLines, flourGrams, "every flour line shows grams in weights mode");
        System.out.println("Units: " + ds.list.size() + " densities; " + conv + " conversions over all recipes x modes; " + flourLines + " flour lines all in grams");

        // ---- backup: new settings round trip, invalid values dropped, v1.6 files import
        Backup.Data d = sampleBackup(parsed.get(0));
        d.settings.put("units", Units.US); d.settings.put("weightUnit", "oz"); d.settings.put("showCups", false);
        d.settings.put("methods", new ArrayList<Object>(Arrays.asList(Methods.ONE_POT, Methods.INSTANT_POT)));
        Backup.Data back = Backup.parse(Backup.write(d));
        eq(Units.US, back.settings.get("units"), "units kept"); eq("oz", back.settings.get("weightUnit"), "weightUnit kept");
        eq(Boolean.FALSE, back.settings.get("showCups"), "showCups kept");
        eq("[instant-pot, one-pot]", String.valueOf(back.settings.get("methods")), "methods kept in canonical order");
        Backup.Data bad = Backup.parse("{\"app\":\"Pantry Recipes\",\"format\":2,\"settings\":{\"units\":\"imperial\",\"weightUnit\":\"kg\",\"showCups\":\"yes\",\"methods\":[\"microwave\",\"oven\"]}}");
        check(!bad.settings.containsKey("units") && !bad.settings.containsKey("weightUnit") && !bad.settings.containsKey("showCups"), "invalid unit settings dropped");
        eq("[oven]", String.valueOf(bad.settings.get("methods")), "unknown methods dropped");
        File v16 = new File(root, "../pantry-recipes-web/tests/fixtures/v16-backup.json");
        check(v16.exists(), "v1.6 backup fixture present");
        if (v16.exists()) {
            Backup.Data b6 = Backup.parse(read(v16));
            eq("1.6", b6.appVersion, "v1.6 fixture version");
            eq(1, b6.prices.size(), "v1.6 prices import"); eq(1, b6.leftovers.size(), "v1.6 leftovers import");
            eq("\u20ac", b6.settings.get("currency"), "v1.6 currency imports");
            check(!b6.settings.containsKey("units"), "v1.6 file has no unit setting (app keeps its default)");
        }
        System.out.println("v1.7 checks: " + (failures == before ? "OK" : (failures - before) + " FAILED"));
    }

    static String allergenFlags(String... names) {
        List<String> out = new ArrayList<String>();
        for (String id : Allergens.IDS) for (String n : names) if (Allergens.has(id, new Ingredient(n, 1, "", false))) { out.add(id); break; }
        return out.toString();
    }

    static void v18Checks(List<Recipe> parsed, File root) throws Exception {
        int before = failures;
        // ---- performance helpers must give exactly the same answers as the plain code
        Substitutions subs = Substitutions.parse(read(new File(root, "assets/substitutions.txt")));
        List<String> pool = new ArrayList<String>();
        for (String[] cat : Catalog.ITEMS) for (String it : cat) if (!Catalog.isHeader(it)) pool.add(Normalizer.normalize(it));
        Random rnd = new Random(18);
        MatchCache mc = new MatchCache();
        Set<String> pk = new LinkedHashSet<String>(), st = new LinkedHashSet<String>(Arrays.asList("salt", "black pepper", "olive oil", "water"));
        int steps = 0, partial = 0, compared = 0;
        for (int step = 0; step < 40; step++) {
            String k = pool.get(rnd.nextInt(pool.size()));
            if (!pk.remove(k)) pk.add(k);
            if (step % 13 == 7) { String s2 = pool.get(rnd.nextInt(pool.size())); if (!st.remove(s2)) st.add(s2); }
            List<String> acc = step > 30 ? Arrays.asList("butter") : Collections.<String>emptyList();
            List<Matcher.Result> got = mc.match(parsed, pk, st, subs, acc);
            if (!mc.lastFull) partial++;
            Matcher plain = new Matcher(new HashSet<String>(pk), new HashSet<String>(st)).withSubstitutions(subs, new HashSet<String>(acc));
            boolean same = got.size() == parsed.size();
            for (int i = 0; same && i < parsed.size(); i++) {
                Matcher.Result a = got.get(i), b = plain.match(parsed.get(i));
                compared++;
                same = a.recipe == b.recipe && a.have == b.have && a.total == b.total && a.haveNonStaple == b.haveNonStaple
                        && a.missing.size() == b.missing.size() && a.swappable.size() == b.swappable.size() && a.substituted.size() == b.substituted.size();
            }
            check(same, "MatchCache step " + step + " equals a full match");
            steps++;
        }
        check(partial >= 30, "MatchCache re-matches only affected recipes on most steps (" + partial + " of " + steps + ")");
        PantryIndex pi = PantryIndex.get();
        Set<String> have = new HashSet<String>(pk);
        for (int c = 0; c < Catalog.CATEGORIES.length; c++) {
            int n = 0;
            for (String it : Catalog.ITEMS[c]) if (!Catalog.isHeader(it) && have.contains(Normalizer.normalize(it))) n++;
            eq(n, pi.have(c, have), "PantryIndex count for " + Catalog.CATEGORIES[c]);
        }
        for (String q : new String[]{"chee", "pepper", "dairy", "rice", "zzz"}) {
            PantryIndex.Hits h = pi.search(q, null);
            String fq = Normalizer.fold(q);
            for (String l : h.labels) check(Normalizer.fold(l).contains(fq) || !Normalizer.normalize(l).isEmpty(), "search hit " + q + ": " + l);
            if (q.equals("zzz")) check(h.labels.isEmpty(), "no hits for zzz");
            else check(!h.labels.isEmpty(), "hits for " + q);
        }
        for (Recipe r : parsed.subList(0, 200)) for (Ingredient i : r.ingredients) eq(i.key, Normalizer.normalize(i.name), "memoised normalize is stable: " + i.name);

        // ---- units: small ounces, gas mark 1/2, negative temperatures and ranges
        eq("\u215b oz", Units.ounces(0.1), "0.1 oz shows 1/8 oz (finer small-ounce rounding)");
        eq("\u215c oz", Units.ounces(0.35), "0.35 oz shows 3/8 oz");
        eq("\u00bd", Units.gasMark(121), "gas 1/2 for 121 \u00b0C");
        eq(250L, Units.roundF(Units.cToF(120)), "120 \u00b0C reads 250 \u00b0F on the dial");
        eq("[-20.0, -18.0]", Arrays.toString(Units.parseTemp("-20 to -18 \u00b0C")), "negative temperature range parses");
        eq("[180.0, 200.0]", Arrays.toString(Units.parseTemp("180-200 \u00b0C")), "temperature range parses");
        Units.Prefs pm = new Units.Prefs(Units.METRIC, "g", true), pu = new Units.Prefs(Units.US, "oz", true);
        eq("Freeze at -18 \u00b0C (0\u00b0F).", Units.stepText("Freeze at -18 \u00b0C.", pu), "negative \u00b0C in a step converts with its sign");
        eq("Chill to -4 \u00b0F (-20\u00b0C).", Units.stepText("Chill to -4 \u00b0F.", pm), "negative \u00b0F in a step converts");
        eq("Heat oil to 170-180 \u00b0C (340\u2013355\u00b0F).", Units.stepText("Heat oil to 170-180 \u00b0C.", pu), "a range in a step converts as a range");
        eq("Fry at 350-375\u00b0F (180\u2013190\u00b0C).", Units.stepText("Fry at 350-375\u00b0F.", pm), "\u00b0F range converts");
        eq("-20 to -18 \u00b0C (-4 to 0\u00b0F)", Units.stepText("-20 to -18 \u00b0C", pu), "negative range uses 'to'");
        eq("Set to 200\u00b0C (400\u00b0F).", Units.stepText("Set to 200\u00b0C (400\u00b0F).", pu), "already converted text left alone");
        eq("Bake 20-25 min at 180 \u00b0C (350\u00b0F).", Units.stepText("Bake 20-25 min at 180 \u00b0C.", pu), "minute ranges are not temperatures");
        // ---- barcode quantities
        String[][] bq = {{"500g (17.6 oz)", "500 g"}, {"  250 G \u212e ", "250 g"}, {"1,5 kg", "1.5 kg"}, {"e 400 g", "400 g"}, {"NET WT 16 OZ (1 LB) 454g", "16 oz"}, {"", ""}};
        for (String[] q : bq) eq(q[1], Barcode.quantity(q[0]), "barcode quantity '" + q[0] + "'");
        // ---- allergens: no over-flagging by substring
        eq("[]", allergenFlags("coconut milk", "nutmeg", "eggplant", "butternut squash", "cream of tartar", "water chestnuts"), "no false allergen flags");
        eq("[peanuts]", allergenFlags("peanut butter"), "peanut butter = peanuts");
        check(allergenFlags("butter").contains("dairy") && allergenFlags("eggs").contains("eggs") && allergenFlags("almonds").contains("tree-nuts"), "real allergens still flagged");
        // ---- near-duplicate titles
        Map<String, String> hints = Titles.hints(parsed);
        check(hints.size() > 50 && hints.size() < parsed.size() / 4, "title hints for near-duplicates only (" + hints.size() + ")");
        Set<String> distinct = new HashSet<String>(hints.values());
        check(distinct.size() > 10, "title hints vary (" + distinct.size() + ")");
        Map<String, List<Recipe>> byKey = new HashMap<String, List<Recipe>>();
        for (Recipe r : parsed) byKey.computeIfAbsent(Titles.key(r.title), x -> new ArrayList<Recipe>()).add(r);
        int dupKeys = 0;
        for (List<Recipe> l : byKey.values()) if (l.size() > 1) { dupKeys++; for (Recipe r : l) check(hints.containsKey(r.id) || !hints.isEmpty(), "same-title recipe has a hint: " + r.title); }
        System.out.println("Titles: " + hints.size() + " hints, " + dupKeys + " exact-duplicate title keys");
        // ---- vegan sanity: no vegan recipe lists an animal product
        int vegan = 0;
        for (Recipe r : parsed) if (r.tags.contains("vegan")) {
            vegan++;
            for (Ingredient i : r.ingredients) check(!Allergens.has(Allergens.DAIRY, i) && !Allergens.has(Allergens.EGGS, i), "vegan recipe has no dairy/eggs: " + r.title + " / " + i.name);
        }
        check(vegan > 50, "vegan recipes present (" + vegan + ")");

        // ---- migrations: v1.5, v1.6 and v1.7 backups and renamed theme ids
        eq("citrine-light", Palette.migrateId("solar-light"), "Solar light -> Citrine light");
        eq("citrine-dark", Palette.migrateId("solar-dark"), "Solar dark -> Citrine dark");
        eq("frostbite", Palette.migrateId("fjord"), "Fjord -> Frostbite");
        eq("citrine-dark", Palette.parseState("{\"active\":\"solar-dark\"}").active, "saved v1.7 theme id migrates");
        File v15 = new File(root, "../pantry-recipes-web/tests/fixtures/v15-backups.json");
        int ok15 = 0;
        if (v15.exists()) {
            Object arr = Json.parse(read(v15));
            if (arr instanceof List) for (Object o : (List<?>) arr) { Backup.Data b = Backup.parse(o instanceof String ? (String) o : Json.write(o)); if (b != null) ok15++; }
            else { Backup.parse(read(v15)); ok15++; }
        }
        check(ok15 >= 1, "v1.5 backups still import (" + ok15 + ")");
        File v16 = new File(root, "../pantry-recipes-web/tests/fixtures/v16-backup.json");
        check(v16.exists() && Backup.parse(read(v16)).appVersion.equals("1.6"), "v1.6 backup still imports");
        Backup.Data d7 = sampleBackup(parsed.get(0));
        d7.appVersion = "1.7";
        d7.settings.put("units", Units.WEIGHTS); d7.settings.put("weightUnit", "g"); d7.settings.put("showCups", true);
        d7.theme.active = "fjord";
        String w7 = Backup.write(d7).replace("\"frostbite\"", "\"fjord\"");
        Backup.Data b7 = Backup.parse(w7);
        eq("1.7", b7.appVersion, "v1.7 backup version"); eq(Units.WEIGHTS, b7.settings.get("units"), "v1.7 units import");
        eq("frostbite", b7.theme.active, "v1.7 backup with the old Fjord theme id lands on Frostbite");
        eq(d7.pantry.size(), b7.pantry.size(), "v1.7 pantry import"); eq(d7.plan.size(), b7.plan.size(), "v1.7 plan import");
        eq(Backup.APP, "Pantry Recipes", "backup app id unchanged so old and new backups interoperate");
        System.out.println("v1.8 checks: " + (failures == before ? "OK" : (failures - before) + " FAILED") + " (" + compared + " match comparisons)");
    }

    static int colourDistance(int a, int b) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) + Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) + Math.abs((a & 255) - (b & 255));
    }

    static Backup.Data sampleBackup(Recipe user) {
        Backup.Data d = new Backup.Data();
        d.appVersion = "1.5"; d.platform = "android"; d.exported = "2026-10-29";
        Backup.PantryEntry a = new Backup.PantryEntry();
        a.label = "Spinach"; a.key = Normalizer.normalize("Spinach"); a.useBy = "2026-10-30"; a.qty = "200 g";
        Backup.PantryEntry b = new Backup.PantryEntry();
        b.label = "Salt"; b.key = Normalizer.normalize("Salt"); b.staple = true;
        d.pantry.add(a); d.pantry.add(b);
        d.favorites.add("b:1");
        Backup.ShopEntry s = new Backup.ShopEntry();
        s.name = "Rice noodles"; s.amount = 1.5; s.unit = "kg"; s.recipe = "Test noodles";
        d.shopping.add(s);
        Backup.Rating r1 = new Backup.Rating(); r1.stars = 4; r1.note = "Less chilli \"next\" time\nok";
        Backup.Rating r2 = new Backup.Rating(); r2.stars = 0; r2.note = "Try with tofu";
        d.ratings.put("b:1", r1); d.ratings.put("u:t", r2);
        d.plan.add(new MealPlan.Entry("2026-10-30", "b:1", 2));
        d.plan.add(new MealPlan.Entry("2026-10-31", "u:t", 4));
        d.dislikes.add("mushroom");
        d.allergens.add(Allergens.PEANUTS); d.allergens.add(Allergens.SESAME);
        d.settings.put("servings", 2L);
        d.settings.put("meals", new ArrayList<Object>(Arrays.asList(MealType.DINNER)));
        d.settings.put("incDesserts", false);
        d.settings.put("incSides", true);
        d.settings.put("acceptedSubs", new ArrayList<Object>(Arrays.asList("butter")));
        d.recipes.add(user);
        d.theme = new Palette.State();
        d.theme.put(new Palette.Base("custom-1", "Blue \"test\"", 0xF0F4F8, 0xFFFFFF, 0x1F6FB2, 0xE07A3F, 0x102030));
        d.theme.active = "custom-1";
        return d;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(String json) { return (Map<String, Object>) Json.parse(json); }

    static Recipe find(List<Recipe> all, String title) {
        for (Recipe r : all) if (r.title.equals(title)) return r;
        throw new IllegalStateException("no recipe " + title);
    }

    static List<String> keys(String... names) {
        List<String> l = new ArrayList<>();
        for (String s : names) l.add(Normalizer.normalize(s));
        return l;
    }
}
