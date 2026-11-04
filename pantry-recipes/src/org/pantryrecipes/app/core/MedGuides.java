package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * v2.0.1: rule-based Renal, Cardiac and Diabetic diet guides. Pure Java, no Android; the web port is js/core201.js.
 *
 * A recipe is never called "safe": the guides only point at ingredients that are often limited on these diets
 * (by name, from conservative word lists), plus the existing per-serving estimates (carbs from the USDA-based
 * nutrition estimate, sodium from measured added salt). Potassium, phosphorus, saturated fat and sugar amounts
 * are not estimated (the food table has no such columns), so those are name flags only.
 * Flags for the bundled recipes are worked out at build time (assets/medflags.txt, RecipeTool) and checked
 * against this class by the tests.
 */
public final class MedGuides {
    private MedGuides() { }

    public static final String RENAL = "renal", CARDIAC = "cardiac", DIABETIC = "diabetic";
    public static final List<String> IDS = Collections.unmodifiableList(Arrays.asList(RENAL, CARDIAC, DIABETIC));
    public static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList("Renal (kidney) guide", "Cardiac (heart) guide", "Diabetic guide"));

    /** The visible disclaimer (wording requested by the user). */
    public static final String DISCLAIMER = "This is a general guide, not medical advice. Limits are different for every person and stage of illness. "
            + "Follow the plan from your doctor or dietitian.";
    public static final String DATA_NOTE = "Flags come from ingredient names, and numbers are rough estimates. Potassium, phosphorus, saturated fat "
            + "and sugar are not measured: the food data has no values for them, so a recipe without flags can still contain them "
            + "(for example phosphate additives in processed foods, or salt already in canned goods). No flags does not mean a recipe suits you.";

    /** Flag codes: K high-potassium, P high-phosphorus, S high-sodium, F high saturated fat, G added sugar, C high-carb / starchy. */
    public static final String CODES = "KPSFGC";
    static final String[] LABELS = {"high-potassium", "high-phosphorus", "high-sodium", "high saturated fat", "added sugar", "high-carb"};

    /** Which flag codes each guide shows. */
    public static String codes(String guide) {
        if (RENAL.equals(guide)) return "KPS";
        if (CARDIAC.equals(guide)) return "SF";
        if (DIABETIC.equals(guide)) return "GC";
        return "";
    }

    public static String name(String guide) { int k = IDS.indexOf(guide); return k < 0 ? guide : NAMES.get(k); }

    /** Plain-language rules (guide -> text). */
    public static final Map<String, String> RULES;
    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(RENAL, "Points out ingredients that are often limited on kidney diets: high-potassium (potatoes, tomatoes, bananas, avocado, "
                + "spinach and chard, beans and lentils, dried fruit, oranges, winter squash, milk and yogurt, salt substitutes), high-phosphorus "
                + "(dairy, beans and lentils, nuts and seeds, whole grains and bran, organ meats, processed meats, cola, chocolate) and high-sodium "
                + "(salty sauces, stock, cured meats, cheese, pickles, more than about \u00bc tsp added salt per serving).");
        m.put(CARDIAC, "Points out high-sodium ingredients (as above) and high saturated fat ones (butter, ghee, lard, cream, cheese, "
                + "coconut milk and oil, palm oil, processed and fatty meats, pastry, chocolate).");
        m.put(DIABETIC, "Points out added sugars (sugar, honey, syrups, jam, sweet sauces, juice) and high-carb or starchy ingredients "
                + "(bread, rice, pasta, flour, potatoes, corn), and shows the estimated carbs per serving.");
        RULES = Collections.unmodifiableMap(m);
    }

    private static Set<String> set(String s) { return new HashSet<String>(Arrays.asList(s.split(","))); }

    static final Set<String> HIGH_K = set("potato,potatoes,sweet potato,sweet potatoes,yam,yams,plantain,plantains,banana,bananas,avocado,avocados,"
            + "tomato,tomatoes,cherry tomato,cherry tomatoes,tomato paste,tomato puree,passata,spinach,chard,swiss chard,beet greens,beet,beets,beetroot,"
            + "orange,oranges,orange juice,cantaloupe,honeydew,kiwi,raisin,raisins,sultana,sultanas,prune,prunes,date,dates,dried fruit,dried apricot,"
            + "dried apricots,pumpkin,butternut,butternut squash,acorn squash,winter squash,parsnip,parsnips,artichoke,artichokes,coconut water,"
            + "molasses,treacle,salt substitute,lo salt,potassium chloride,pomegranate,guava,milk,yogurt,yoghurt,buttermilk,evaporated milk,"
            + "condensed milk,kefir,bran");
    static final Set<String> NOT_K = set("potato starch,orange zest,orange blossom water,orange flower water,orange extract,coconut milk,"
            + "almond milk,oat milk,rice milk,coconut yogurt,milk chocolate_x,date syrup_x,pumpkin seeds,pumpkin seed,pumpkin pie spice,"
            + "pumpkin spice,tomato ketchup_x,sugar pumpkin_x");
    static final Set<String> HIGH_P = set("milk,cheese,yogurt,yoghurt,buttermilk,evaporated milk,condensed milk,powdered milk,milk powder,"
            + "kefir,paneer,ricotta,cottage cheese,ice cream,custard,almond,almonds,cashew,cashews,peanut,peanuts,peanut butter,pistachio,"
            + "pistachios,walnut,walnuts,pecan,pecans,hazelnut,hazelnuts,brazil nut,brazil nuts,sunflower seeds,pumpkin seeds,pumpkin seed,"
            + "sesame,sesame seeds,tahini,chia,chia seeds,flaxseed,flax seeds,hemp seeds,oats,oat,oatmeal,rolled oats,bran,wholemeal,whole wheat,"
            + "whole grain,wholegrain,brown rice,quinoa,liver,kidney_x,offal,sweetbreads,sardine,sardines,cola,cocoa,cocoa powder,chocolate,"
            + "processed cheese,american cheese,baking powder,cheddar,parmesan,mozzarella,gruyere,swiss cheese,emmental,gouda,edam,pecorino,"
            + "manchego,feta,halloumi,brie,camembert,blue cheese,gorgonzola,roquefort,monterey jack,colby,provolone,queso fresco,cotija");
    static final Set<String> NOT_P = set("coconut milk,almond milk,oat milk,rice milk,cocoa butter,almond extract,sesame oil,peanut oil,"
            + "toasted sesame oil,coconut yogurt,almond flour_x");
    static final Set<String> HIGH_SATFAT = set("butter,salted butter,unsalted butter,ghee,lard,tallow,suet,dripping,beef dripping,shortening,"
            + "cream,double cream,heavy cream,whipping cream,single cream,sour cream,soured cream,clotted cream,creme fraiche,cream cheese,"
            + "mascarpone,cheese,cheddar,parmesan,mozzarella,brie,camembert,gruyere,halloumi,feta,paneer,coconut milk,coconut cream,"
            + "coconut oil,desiccated coconut,creamed coconut,palm oil,pork belly,spare ribs,short ribs,oxtail,brisket,lamb shoulder,"
            + "chicken skin,duck fat,goose fat,pastry,puff pastry,shortcrust pastry,croissant,croissants,chocolate,ice cream,condensed milk");
    static final Set<String> NOT_SATFAT = set("peanut butter,nut butter,almond butter,cashew butter,butter bean,butter beans,butternut,"
            + "butternut squash,butterhead,butter lettuce,cream of tartar,ice cream cone_x,cocoa powder,vegan butter,vegan cheese,"
            + "cottage cheese,coconut water,light coconut milk_x,cream crackers_x,sunflower spread");

    static boolean word(String key, Set<String> words) {
        if (words.contains(key)) return true;
        for (String w : key.split(" ")) if (words.contains(w)) return true;
        return false;
    }
    static boolean phrase(String key, Set<String> phrases) {
        String k = " " + key + " ";
        for (String p : phrases) if (k.contains(" " + p + " ")) return true;
        return false;
    }
    static boolean has(String key, Set<String> words, Set<String> except) { return (word(key, words) || phrase(key, words)) && !phrase(key, except); }

    static boolean legumeK(String key) {
        if (!Diets.isLegume(key)) return false;
        return !(key.contains("sauce") || key.contains("oil") || key.contains("milk") || key.contains("tofu") || key.contains("miso")
                || key.contains("tamari") || key.contains("sprout"));
    }

    public static boolean isHighPotassium(String key) { return has(key, HIGH_K, NOT_K) || legumeK(key); }
    public static boolean isHighPhosphorus(String key) { return has(key, HIGH_P, NOT_P) || legumeK(key) || Diets.isProcessedMeat(key); }
    public static boolean isHighSodium(String key) { return Diets.isSalty(key); }
    public static boolean isHighSatFat(String key) {
        if (phrase(key, NOT_SATFAT)) return false;
        return has(key, HIGH_SATFAT, NOT_SATFAT) || Diets.isProcessedMeat(key);
    }
    public static boolean isAddedSugar(String key) { return Diets.isSugar(key); }
    public static boolean isHighCarb(String key) { return Diets.isGrain(key) || Diets.isStarchy(key); }

    static boolean test(int code, String key) {
        switch (code) {
            case 0: return isHighPotassium(key);
            case 1: return isHighPhosphorus(key);
            case 2: return isHighSodium(key);
            case 3: return isHighSatFat(key);
            case 4: return isAddedSugar(key);
            default: return isHighCarb(key);
        }
    }

    /** Sodium from measured added salt, mg per serving (1 tsp table salt is about 2,300 mg); NaN when there is only "salt to taste". */
    public static double saltSodiumMg(Recipe r) {
        double t = Diets.saltTspPerServing(r);
        return Double.isNaN(t) ? Double.NaN : t * 2300;
    }

    /**
     * Flags as an int[6][] of ingredient indexes per code (CODES order). Added salt above about 1/4 tsp per serving
     * flags the salt line(s) as high-sodium. Optional ingredients are included (they are still in the recipe).
     */
    public static int[][] compute(Recipe r) {
        List<List<Integer>> l = new ArrayList<List<Integer>>();
        for (int c = 0; c < 6; c++) l.add(new ArrayList<Integer>());
        double salt = Diets.saltTspPerServing(r);
        boolean saltHigh = !Double.isNaN(salt) && salt > 0.25 + 1e-9;
        for (int n = 0; n < r.ingredients.size(); n++) {
            Ingredient i = r.ingredients.get(n);
            String k = i.key;
            if (k == null || k.isEmpty()) continue;
            for (int c = 0; c < 6; c++) if (test(c, k)) l.get(c).add(n);
            if (saltHigh && Diets.SALT_KEYS.contains(k) && !l.get(2).contains(n)) l.get(2).add(n);
        }
        int[][] out = new int[6][];
        for (int c = 0; c < 6; c++) { out[c] = new int[l.get(c).size()]; for (int j = 0; j < out[c].length; j++) out[c][j] = l.get(c).get(j); }
        return out;
    }

    /** Compact form for assets/medflags.txt: "K0.3 P2 S5" (codes with ingredient indexes; empty = no flags). */
    public static String encode(int[][] f) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < 6; c++) {
            if (f[c].length == 0) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(CODES.charAt(c));
            for (int j = 0; j < f[c].length; j++) sb.append(j == 0 ? "" : ".").append(f[c][j]);
        }
        return sb.toString();
    }

    public static int[][] decode(String s) {
        int[][] out = new int[6][];
        for (int c = 0; c < 6; c++) out[c] = new int[0];
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(" ")) {
            if (part.isEmpty()) continue;
            int c = CODES.indexOf(part.charAt(0));
            if (c < 0) continue;
            String rest = part.substring(1);
            if (rest.isEmpty()) continue;
            String[] ns = rest.split("\\.");
            int[] v = new int[ns.length];
            try { for (int j = 0; j < ns.length; j++) v[j] = Integer.parseInt(ns[j]); } catch (NumberFormatException e) { continue; }
            out[c] = v;
        }
        return out;
    }

    /** Parses medflags.txt: "id\tflags\tingredientCount" lines, '#' comments. */
    public static Map<String, String[]> parse(String txt) {
        Map<String, String[]> m = new HashMap<String, String[]>();
        if (txt == null) return m;
        for (String line : txt.split("\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] p = line.split("\t", -1);
            if (p.length >= 3) m.put(p[0], new String[]{p[1], p[2]});
        }
        return m;
    }

    /** Precomputed flags when the bundled recipe is unchanged (same ingredient count), else computed now. */
    public static int[][] flags(Recipe r, Map<String, String[]> pre) {
        if (pre != null) {
            String[] e = pre.get(r.id);
            if (e != null && e[1].equals(String.valueOf(r.ingredients.size()))) return decode(e[0]);
        }
        return compute(r);
    }

    /** True when none of the guide's codes are flagged. */
    public static boolean noFlags(int[][] f, String guide) {
        for (char ch : codes(guide).toCharArray()) if (f[CODES.indexOf(ch)].length > 0) return false;
        return true;
    }

    static String lc(String s) { return s.toLowerCase(Locale.US).trim(); }

    /** "contains high-potassium ingredients: potato, tomato" lines for one guide (empty when nothing is flagged). */
    public static List<String> lines(Recipe r, int[][] f, String guide) {
        List<String> out = new ArrayList<String>();
        for (char ch : codes(guide).toCharArray()) {
            int c = CODES.indexOf(ch);
            if (f[c].length == 0) continue;
            List<String> names = new ArrayList<String>();
            for (int n : f[c]) {
                if (n < 0 || n >= r.ingredients.size()) continue;
                String nm = lc(r.ingredients.get(n).name);
                if (!nm.isEmpty() && !names.contains(nm)) names.add(nm);
            }
            if (names.isEmpty()) continue;
            StringBuilder sb = new StringBuilder("contains ").append(LABELS[c]).append(names.size() == 1 ? " ingredient: " : " ingredients: ");
            for (int j = 0; j < names.size(); j++) sb.append(j == 0 ? "" : ", ").append(names.get(j));
            out.add(sb.toString());
        }
        return out;
    }

    /** User limits (0 = not set). Stored on the device and in backups (no secrets). */
    public static final int MAX_SODIUM_MG = 20000, MAX_CARBS_G = 1000;
    public static int clampSodium(long v) { return (int) Math.max(0, Math.min(MAX_SODIUM_MG, v)); }
    public static int clampCarbs(long v) { return (int) Math.max(0, Math.min(MAX_CARBS_G, v)); }

    /**
     * Estimate lines for the recipe page: added-salt sodium and carbs per serving, compared with the user's limits.
     * carbs = NaN when there is no usable nutrition estimate.
     */
    public static List<String> estimateLines(Recipe r, double carbs, List<String> guides, int sodiumLimit, int carbLimit) {
        List<String> out = new ArrayList<String>();
        boolean na = guides.contains(RENAL) || guides.contains(CARDIAC), cb = guides.contains(DIABETIC);
        if (na) {
            double mg = saltSodiumMg(r);
            if (Double.isNaN(mg)) out.add("Added salt: \u201cto taste\u201d, not measured. Other ingredients add sodium too (not estimated).");
            else if (mg > 0) {
                String s = "Added salt alone \u2248 " + Math.round(mg) + " mg sodium per serving (estimate; other ingredients add more)";
                if (sodiumLimit > 0) s += mg > sodiumLimit ? " \u2013 above your " + sodiumLimit + " mg per meal limit" : " \u2013 your limit is " + sodiumLimit + " mg per meal";
                out.add(s + ".");
            } else if (sodiumLimit > 0) out.add("No measured added salt. Your sodium limit is " + sodiumLimit + " mg per meal; other ingredients still add sodium (not estimated).");
        }
        if (cb) {
            if (Double.isNaN(carbs)) out.add("Carbs per serving: no estimate (too few ingredients matched).");
            else {
                String s = "Carbs \u2248 " + Math.round(carbs) + " g per serving (estimate)";
                if (carbLimit > 0) s += carbs > carbLimit ? " \u2013 above your " + carbLimit + " g per meal limit" : " \u2013 your limit is " + carbLimit + " g per meal";
                out.add(s + ".");
            }
        }
        return out;
    }
}
