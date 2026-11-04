import org.pantryrecipes.app.core.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Reference results for the v1.8 web port: small-ounce rounding, temperatures (negative values and ranges, gas ½),
 * Open Food Facts quantity clean-up and ingredient matching, allergen flags (name-based, "check label" gluten),
 * near-duplicate title hints, and the unit-aware shopping share text.
 * Usage: java -cp build V18Export <android project dir> <web dir>   (writes tests/vectors18.json)
 */
public class V18Export {
    static List<Object> L(Object... a) { return new ArrayList<Object>(Arrays.asList(a)); }
    static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    static final String[] TEMPS = {"180", "200", "-18", "\u221218 \u00b0C", "-20 to -15", "-5--1", "180-200", "180 \u2013 200", "350\u2013400 \u00b0F",
            "0", "100 \u00b0C", "121", "120", "250", "425F", "60", "4", "-40", "1,5", "abc", "", "200-180", "160 to 180\u00b0c", "-10 \u2014 10", "+5"};
    static final String[] GAS = {"\u00bd", "1/2", "1", "4", "9", "10", "", "x"};
    static final String[] QTY = {"400 g e", "400 g \u212e", "400g\u212e", "1,5 kg", "Net wt 12 oz (340 g)", "NET WT. 16 OZ (1 LB) 454g", "6 x 330 ml",
            "75 cl", "2 dl", "500mg", "12 fl oz", "1 lb (454 g)", "about a bunch", "4 \u00d7 125 g", "1L", "0,75 l", "250 G", "330ml e", "2 x 1,5 l",
            "", "   ", "12", "approx. 1 kg", "100 g (4 x 25 g)", "1 kg - 2 kg", "This is a very long quantity text that goes on and on"};
    static final String[][] PRODUCTS = {
            {"Barilla Spaghetti n.5", "", "Barilla", "en:pastas,en:spaghetti", "500 g e"},
            {"Heinz Tomato Ketchup", "", "Heinz", "en:sauces,en:ketchup", "400 ml"},
            {"Organic Whole Milk", "Milk", "Horizon", "en:dairies,en:milks", "1 gal"},
            {"", "Canned chickpeas", "Goya", "en:legumes,en:chickpeas", "15.5 oz (439 g)"},
            {"Extra Virgin Olive Oil", "", "Bertolli", "en:fats,en:olive-oils", "75 cl"},
            {"Greek Style Yogurt Natural", "", "Fage", "en:dairies,en:yogurts", "4 x 125 g"},
            {"Peanut Butter Smooth", "", "Skippy", "en:spreads,en:peanut-butters", "340 g"},
            {"Basmati Rice", "", "Tilda", "en:cereals,en:rices", "1 kg"},
            {"Mystery Snack", "", "Acme", "en:snacks", "50 g"},
            {"Coca-Cola", "", "Coca-Cola", "en:beverages,en:sodas", "6 x 330 ml"},
            {"Free Range Large Eggs", "", "Happy Egg", "en:eggs", "12"},
            {"Plain Flour", "", "McDougalls", "en:flours,en:wheat-flours", "1,5 kg"}};
    static final String[] NAMES = {"fried onions", "crispy fried onions", "fish cake", "egg-free mayonnaise", "dairy-free milk", "lactose-free milk",
            "peanut oil", "butter lettuce", "butterhead lettuce", "butter beans", "oats", "rolled oats", "soba noodles", "worcestershire sauce",
            "spaghetti squash", "ginger ale", "milk chocolate", "coconut milk", "vegan butter", "tamari", "soy sauce", "flour", "miso paste",
            "za'atar", "shrimp", "anchovy", "fish sauce", "sesame oil", "almond milk", "nutmeg", "chestnut mushrooms", "peanut butter", "pine nuts",
            "gluten-free flour", "plain yogurt", "sugar, for the custard", "kecap manis", "textured vegetable protein", "everything bagel seasoning",
            "pizza sauce", "spring roll wrappers", "water chestnuts", "eggplant", "egg noodles", "nut-free pesto", "oyster mushrooms", "crab apple",
            "cheese ravioli", "plant-based butter", "eggless mayo", "soy-free tofu", "breadcrumbs", "panko", "beer", "malt vinegar", "rice", "corn tortillas",
            "Cr\u00e8me fra\u00eeche", "pur\u00e9e de tomates", ""};
    static final double[] OZ = {0.01, 0.05, 0.09, 0.1, 0.125, 0.2, 0.3, 0.4, 0.55, 0.7, 0.9, 0.99, 1, 1.1, 1.3, 2.6, 3.9, 4, 5.3, 8.8, 15.9, 16, 17.6, 35.3};

    public static void main(String[] a) throws Exception {
        File android = new File(a[0]), web = new File(a[1]);
        List<Recipe> recipes = Recipe.parseList(read(new File(android, "assets/recipes.json")));
        Units.Densities ds = Units.parseDensities(read(new File(android, "assets/densities.tsv")));
        Map<String, Object> v = new LinkedHashMap<String, Object>();

        List<Object> oz = new ArrayList<Object>();
        for (double d : OZ) oz.add(L(d, Units.ounces(d)));
        v.put("ounces", oz);

        List<Object> temps = new ArrayList<Object>();
        for (String t : TEMPS) {
            double[] p = Units.parseTemp(t);
            temps.add(L(t, p == null ? null : L(p[0], p[1]), Units.tempLine(t, "C"), Units.tempLine(t, "F")));
        }
        v.put("temps", temps);
        List<Object> gas = new ArrayList<Object>();
        for (String g : GAS) gas.add(L(g, Units.tempLine(g, "gas")));
        v.put("gas", gas);

        List<Object> qty = new ArrayList<Object>();
        for (String q : QTY) qty.add(L(q, Barcode.quantity(q)));
        v.put("quantity", qty);

        List<Object> prods = new ArrayList<Object>();
        for (String[] p : PRODUCTS) {
            Map<String, Object> prod = new LinkedHashMap<String, Object>();
            prod.put("product_name", p[0]);
            prod.put("generic_name", p[1]);
            prod.put("brands", p[2]);
            prod.put("categories_tags", new ArrayList<Object>(Arrays.asList(p[3].split(","))));
            prod.put("quantity", p[4]);
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put("status", 1);
            root.put("code", "0000000000000");
            root.put("product", prod);
            String json = Json.write(root);
            Barcode.Product bp = Barcode.parse(json);
            prods.add(L(json, bp == null ? null : bp.title(), bp == null ? null : bp.quantity, bp == null ? null : Barcode.match(bp),
                    bp == null ? null : new ArrayList<Object>(Barcode.matches(bp, 4))));
        }
        v.put("products", prods);

        // allergen bits for every distinct bundled ingredient name and the extra names
        LinkedHashSet<String> names = new LinkedHashSet<String>(Arrays.asList(NAMES));
        for (Recipe r : recipes) for (Ingredient i : r.ingredients) names.add(i.name);
        List<Object> al = new ArrayList<Object>();
        for (String n : names) {
            Ingredient i = new Ingredient(n, 1, "", false);
            StringBuilder b = new StringBuilder();
            for (String id : Allergens.IDS) if (Allergens.has(id, i)) b.append(b.length() == 0 ? "" : ",").append(id).append(Allergens.uncertain(id, i) ? "?" : "");
            al.add(L(n, b.toString()));
        }
        v.put("allergenNames", al);
        List<Object> rf = new ArrayList<Object>();
        for (Recipe r : recipes) {
            List<String> u = Allergens.uncertainFlags(r);
            StringBuilder b = new StringBuilder();
            for (String id : Allergens.flags(r)) b.append(b.length() == 0 ? "" : "|").append(Allergens.flagLabel(r, id, u));
            rf.add(L(r.id, b.toString()));
        }
        v.put("recipeFlags", rf);

        Map<String, String> hints = Titles.hints(recipes);
        List<Object> th = new ArrayList<Object>();
        for (Recipe r : recipes) if (hints.containsKey(r.id)) th.add(L(r.id, hints.get(r.id)));
        v.put("titleHints", th);
        List<Object> tk = new ArrayList<Object>();
        String[] extraTitles = {"The Best Garlic-Butter Salmon (in Foil)", "Cr\u00e8me Br\u00fbl\u00e9e", "  ", "Pad See Ew", "Chicken & Rice"};
        for (String t : extraTitles) tk.add(L(t, Titles.key(t)));
        for (int i = 0; i < recipes.size(); i += 37) tk.add(L(recipes.get(i).title, Titles.key(recipes.get(i).title)));
        v.put("titleKeys", tk);
        List<Object> near = new ArrayList<Object>();
        String[][] pairs = {{"Air Fryer Chicken Wings", "Crispy Air Fryer Chicken Wings"}, {"Garlic Butter Salmon", "Garlic Butter Salmon in Foil"},
                {"Egg Fried Rice", "Pineapple Fried Rice"}, {"Pad Thai", "Pad See Ew"}, {"Beef Stew", "Beef Stew"}, {"", "Soup"}};
        for (String[] p : pairs) near.add(L(p[0], p[1], Titles.near(Titles.words(p[0]), Titles.words(p[1]))));
        v.put("titleNear", near);

        // unit-aware shopping share text
        final Units.Densities fds = ds;
        List<Object[]> rows = new ArrayList<Object[]>();
        Object[][] raw = {{"Flour", 250.0, "g", false}, {"Milk", 500.0, "ml", false}, {"Butter", 2.0, "tbsp", false}, {"Eggs", 3.0, "", false},
                {"Chicken breast", 1.5, "lb", false}, {"Salt", Double.NaN, "", false}, {"Olive oil", 0.25, "cup", true}, {"Cheddar", 4.0, "oz", false}};
        for (Object[] r : raw) rows.add(r);
        List<Object> share = new ArrayList<Object>();
        for (final String mode : new String[]{"original", "us", "metric", "weights"}) {
            final Units.Prefs p = new Units.Prefs(mode, "oz".equals(mode) ? "oz" : "g", true);
            share.add(L(mode, ShareText.shopping(rows, new ShareText.QtyFormat() {
                public String format(String name, double amount, String unit) { return Units.display(Normalizer.normalize(name), amount, unit, 1, p, fds); }
            })));
        }
        share.add(L("none", ShareText.shopping(rows)));
        v.put("share", share);

        File out = new File(web, "tests/vectors18.json");
        Files.write(out.toPath(), Json.write(v).getBytes(StandardCharsets.UTF_8));
        System.out.println("vectors18.json: " + out.length() + " bytes, " + al.size() + " allergen names, " + th.size() + " title hints");
    }
}
