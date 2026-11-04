package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v2.0: more diets, tagged by rules from ingredient names only. This is a guide, not a certification: brands differ,
 * labels must be checked, and the rules are deliberately conservative (when in doubt a recipe is NOT tagged).
 * Optional ingredients ("to serve") are ignored. The same rules are in the web app (core20.js) and are compared
 * by the parity tests. Salt, pepper and water never disqualify a recipe except where stated (low-sodium, carnivore).
 */
public final class Diets {
    private Diets() { }

    public static final String PESCATARIAN = "pescatarian", MEDITERRANEAN = "mediterranean", KETO = "keto",
            LOW_CARB = "low-carb", PALEO = "paleo", WHOLE30 = "whole30", CARNIVORE = "carnivore",
            LOW_SODIUM = "low-sodium", HIGH_PROTEIN = "high-protein", HALAL = "halal", KOSHER = "kosher";

    /** The new diet ids in display order (the four original ones stay in Recipe.tags). */
    public static final List<String> IDS = Collections.unmodifiableList(Arrays.asList(
            PESCATARIAN, MEDITERRANEAN, LOW_CARB, KETO, PALEO, WHOLE30, CARNIVORE, LOW_SODIUM, HIGH_PROTEIN, HALAL, KOSHER));
    public static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(
            "Pescatarian", "Mediterranean-style", "Low-carb", "Keto", "Paleo", "Whole30-style", "Carnivore", "Low-sodium", "High-protein", "Halal-friendly", "Kosher-style"));

    public static String name(String id) {
        int k = IDS.indexOf(id);
        if (k >= 0) return NAMES.get(k);
        if (Recipe.VEGETARIAN.equals(id)) return "Vegetarian";
        if (Recipe.VEGAN.equals(id)) return "Vegan";
        if (Recipe.GLUTEN_FREE.equals(id)) return "Gluten-free";
        if (Recipe.DAIRY_FREE.equals(id)) return "Dairy-free";
        return id;
    }

    public static final String NOTE = "Diet tags are a guide, not a certification. They are worked out from ingredient names "
            + "with conservative rules; products and brands differ, so always check labels (and talk to a professional for medical diets).";

    /** Plain-language rules shown in the app (id -> text). */
    public static final Map<String, String> RULES;
    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(PESCATARIAN, "Vegetarian, or the only meat is fish or seafood.");
        m.put(MEDITERRANEAN, "Cooked with olive oil, and either a Mediterranean cuisine or built on fish, beans or lentils; no beef, pork or processed meat, no butter, cream or margarine, not a dessert or drink.");
        m.put(LOW_CARB, "No grains, flour, bread, pasta, rice, corn, potatoes, beans or lentils, sugar or sweet syrups, dried fruit, juice or bananas.");
        m.put(KETO, "Low-carb rules plus no milk, most fruit (berries, lemon, lime, avocado, tomato and coconut are fine), beets, carrots or winter squash. Amounts are not counted, so small amounts of an allowed food can still add up.");
        m.put(PALEO, "No grains, legumes (including peanuts and soy), dairy (ghee is fine), refined sugar, seed oils, margarine or alcohol. Honey and maple syrup are allowed.");
        m.put(WHOLE30, "Paleo rules plus no added sugar of any kind (including honey, maple syrup and dates as sweetener), no alcohol (also for cooking), "
                + "no processed meats (bacon, sausage, ham: compliant versions exist but are not assumed), no corn, no baked goods, desserts or treats. "
                + "Green beans, snow peas and sugar snap peas are allowed. Additives (carrageenan, MSG, sulfites) cannot be checked from names.");
        m.put(CARNIVORE, "Meat, fish, seafood, eggs, organ meats, bone broth, animal fats and dairy, plus salt, pepper and water (no plants: no herbs, vegetables, grains, sugar or sauces; no processed meats such as sausages or bacon).");
        m.put(LOW_SODIUM, "No salty ingredients (soy sauce, fish sauce, miso, cured meats, stock or broth, olives, capers, pickles, anchovies, hard or salty cheese) "
                + "and at most about \u00bc tsp of added salt per serving. \u201cSalt to taste\u201d is allowed: season lightly.");
        m.put(HIGH_PROTEIN, "At least 25 g protein per serving and at least 25% of calories from protein, by the nutrition estimate (only when 70% of the ingredients are matched).");
        m.put(HALAL, "No pork or pork products (bacon, ham, lard, most sausages and salami unless a halal meat is named), no gelatin, "
                + "no alcohol (also for cooking, including mirin and cooking wine), no blood. A guide, not certification: meat and poultry must be "
                + "bought halal, and you should check labels (vanilla extract, flavourings, cheese rennet and vinegars are not judged here).");
        m.put(KOSHER, "No pork, rabbit, shellfish or molluscs, or fish without scales (such as catfish, eel, monkfish, swordfish, shark); "
                + "meat or poultry never with dairy (butter, cream, cheese, milk, yogurt, ghee); no gelatin; no grape wine, brandy or vinegar-free grape spirits "
                + "unless certified, so wine-based ingredients are left out. A guide, not certification: meat must be kosher, and everything "
                + "should carry a hechsher; rules differ between communities.");
        RULES = Collections.unmodifiableMap(m);
    }

    private static Set<String> set(String s) { return new HashSet<String>(Arrays.asList(s.split(","))); }

    static final Set<String> FREE = set("salt,water,ice,black pepper,pepper,sea salt,kosher salt,ice cube,boiling water,cold water,warm water,hot water");
    static final Set<String> FISH_WORDS = set("fish,salmon,tuna,prawn,shrimp,crab,lobster,mussel,clam,squid,octopus,oyster,mackerel,sardine,"
            + "cod,haddock,kipper,scallop,anchovy,trout,snapper,halibut,monkfish,tilapia,pollock,hake,basa,caviar,roe,eel,herring,"
            + "swordfish,seabass,bass,bream,lox,calamari,bonito,dashi,crawfish,crayfish,langoustine,catfish,mahi,sole,flounder,whitebait,surimi");
    static final Set<String> GRAIN_WORDS = set("flour,bread,breadcrumb,rice,pasta,spaghetti,penne,macaroni,fusilli,linguine,rigatoni,farfalle,"
            + "tagliatelle,lasagne,lasagna,orzo,fettuccine,noodle,couscous,bulgur,barley,farro,semolina,oats,oat,quinoa,corn,cornmeal,"
            + "polenta,cornstarch,cornflour,tortilla,pita,naan,bun,roll,bagel,croissant,pastry,dough,cracker,biscuit,cookie,cake,"
            + "wheat,rye,millet,buckwheat,granola,cereal,panko,crouton,ramen,udon,soba,gnocchi,wonton,filo,phyllo,muffin,brioche,baguette,"
            + "ciabatta,sourdough,pretzel,pancake,waffle,pizza,wrap,chapati,roti,paratha,flatbread,grits,masa,popcorn,spelt,matzo,freekeh,"
            + "vermicelli,ravioli,tortellini,dumpling,stuffing,crumb,cous,arborio,risotto,basmati,jasmine,sorghum,amaranth,teff,tapioca,"
            + "sago,seitan,breadstick,scone,crumpet,taco,tostada,shell,graham,digestive,wafer,brownie,pie,tart,shortcrust,puff,bisquick,roux,"
            + "malt,beer,ale,stout,gravy,starch");
    static final Set<String> NOT_GRAIN = set("almond flour,coconut flour,cauliflower rice,riced cauliflower,corn salad_x,lettuce wrap,lettuce leaf,"
            + "egg roll_x,peppercorn,sweetcorn_x,flaxseed meal,almond meal,arrowroot_x,shell on_x,pie spice,pumpkin pie spice,rice vinegar,rice wine vinegar,"
            + "oat milk_x,cauliflower pizza_x,coconut wrap,nori wrap_x,cream of tartar,tartar sauce_x,water chestnut");
    static final Set<String> LEGUME_WORDS = set("bean,beans,lentil,lentils,chickpea,chickpeas,pea,peas,soy,soya,soybean,tofu,tempeh,edamame,miso,"
            + "peanut,peanuts,hummus,houmous,dal,dhal,daal,gram,urad,mung,falafel,tamari,natto,doubanjiang,gochujang,kecap,carob");
    static final Set<String> OK_LEGUME = set("green bean,green beans,snow pea,snow peas,sugar snap,sugar snap pea,sugar snap peas,mangetout,"
            + "runner bean,runner beans,french bean,french beans,string bean,string beans,haricot vert,vanilla bean,coffee bean,cocoa bean,bean sprout,bean sprouts,"
            + "chickpea_x,coconut aminos");
    static final Set<String> SUGAR_WORDS = set("sugar,honey,maple,agave,molasses,treacle,syrup,jam,jelly,marmalade,chocolate,marshmallow,"
            + "caramel,condensed,ketchup,bbq,barbecue,hoisin,teriyaki,sweetener,icing,frosting,fondant,candy,sprinkles,nutella,dulce,"
            + "date syrup,coconut sugar,brown sugar,muscovado,demerara,turbinado,dextrose,glucose,fructose,sweet chili,sweet chilli,mirin,"
            + "custard,meringue,lemonade,cola,soda,juice,cordial,liqueur,preserves,chutney");
    static final Set<String> NOT_SUGAR = set("sugar snap,sugar snap pea,sugar snap peas,sugar free_x,club soda_x,baking soda,bicarbonate of soda,soda water_x,"
            + "bread soda,lemon juice,lime juice,unsweetened chocolate_x,cocoa powder,sugar pumpkin_x");
    static final Set<String> PALEO_SWEET = set("honey,maple,maple syrup,date,dates,coconut sugar");
    static final Set<String> ALCOHOL_WORDS = set("wine,beer,ale,stout,lager,rum,vodka,whiskey,whisky,bourbon,brandy,sherry,sake,mirin,liqueur,"
            + "tequila,gin,vermouth,marsala,port,cognac,prosecco,champagne,cider,kirsch,amaretto,schnapps,mezcal,grappa,ouzo,shaoxing,"
            + "cointreau,kahlua,baileys,limoncello,calvados,madeira,cava,mead");
    static final Set<String> NOT_ALCOHOL = set("wine vinegar,red wine vinegar,white wine vinegar,rice wine vinegar,sherry vinegar,cider vinegar,"
            + "apple cider vinegar,port salut_x,ginger ale,root beer_x,non alcoholic_x,sake_x,portobello,portabella,portobello mushroom,"
            + "ginger beer_x,apple cider_x");
    static final Set<String> SEED_OIL = set("vegetable oil,canola oil,rapeseed oil,soybean oil,corn oil,sunflower oil,cottonseed oil,"
            + "grapeseed oil,safflower oil,margarine,shortening,cooking spray,peanut oil,rice bran oil,vegetable shortening,spread");
    static final Set<String> PROCESSED_MEAT = set("bacon,ham,sausage,salami,pepperoni,chorizo,pancetta,prosciutto,hot dog,bratwurst,mortadella,"
            + "nduja,jerky,kielbasa,andouille,frankfurter,lardon,gammon,bologna,spam,deli,luncheon,corned,pastrami,hot dogs,sausages,lardons");
    static final Set<String> RED_MEAT = set("beef,pork,veal,steak,brisket,chuck,sirloin,ribeye,oxtail,mince,meatball,meatballs,venison,ribs,"
            + "short rib,tenderloin,pork belly,pork chop");
    static final Set<String> STARCHY = set("potato,potatoes,sweet potato,sweet potatoes,yam,yams,plantain,plantains,parsnip,parsnips,"
            + "hash brown,hash browns,fries,chips_x,sweetcorn,cassava,taro");
    static final Set<String> HIGH_SUGAR_FRUIT = set("banana,bananas,raisin,raisins,sultana,sultanas,date,dates,fig,figs,prune,prunes,"
            + "dried fruit,dried cranberries,dried apricot,dried apricots,currant,currants,mango_x,juice,pineapple_x");
    static final Set<String> KETO_FRUIT_BAN = set("apple,apples,pear,pears,mango,mangoes,pineapple,grape,grapes,orange,oranges,peach,peaches,"
            + "nectarine,apricot,apricots,cherry,cherries,plum,plums,melon,watermelon,cantaloupe,kiwi,papaya,pomegranate,clementine,"
            + "tangerine,mandarin,persimmon,lychee,guava,passion fruit,applesauce,beet,beets,beetroot,carrot,carrots,butternut,"
            + "butternut squash,acorn squash,delicata squash,parsnip,parsnips,jicama_x,sweetcorn,corn");
    static final Set<String> SALTY = set("soy sauce,tamari,fish sauce,oyster sauce,miso,bacon,ham,salami,prosciutto,pancetta,chorizo,pepperoni,"
            + "sausage,sausages,hot dog,bouillon,stock,broth,stock cube,olive,olives,caper,capers,pickle,pickles,gherkin,anchovy,anchovies,feta,"
            + "parmesan,pecorino,halloumi,blue cheese,roquefort,gorgonzola,processed cheese,american cheese,teriyaki,hoisin,worcestershire,"
            + "kimchi,sauerkraut,soup mix,gravy,ketchup,bbq sauce,barbecue sauce,salted butter_x,cured,smoked salmon,lox,jerky,corned,"
            + "pastrami,kecap,gochujang,doubanjiang,dashi,bouillon cube,seasoning mix,taco seasoning,ranch,stock powder,msg,soy");
    static final Set<String> NOT_SALTY = set("low sodium stock,low sodium broth,unsalted stock,no salt stock,olive oil,extra virgin olive oil,"
            + "light olive oil,soy milk,soya milk,soy yogurt,soybean_x,soy free_x");
    static final Set<String> SALT_KEYS = set("salt,sea salt,kosher salt,table salt,flaky salt,rock salt,coarse salt,fine salt,celery salt,garlic salt,onion salt");
    static final Set<String> CARNIVORE_DAIRY_BAN = set("nutella,ranch,tzatziki,alfredo,custard,lassi,labneh_x");
    static final Set<String> ANIMAL_EXTRA = set("egg,eggs,yolk,egg yolk,egg white,egg yolks,egg whites,lard,tallow,suet,bone broth,beef broth,"
            + "chicken broth,beef stock,chicken stock,bone marrow,gelatin,gelatine,dripping,beef dripping,duck fat,schmaltz,ghee,butter,collagen,short rib,short ribs");
    static final Set<String> MED_CUISINES = set("greek,italian,spanish,turkish,lebanese,moroccan,middle eastern,mediterranean,israeli,"
            + "tunisian,cypriot,maltese,croatian,syrian,egyptian,portuguese,levantine,provencal,sicilian");

    static final Set<String> PORK = set("pork,bacon,ham,gammon,lard,pancetta,prosciutto,chorizo,salami,pepperoni,nduja,guanciale,mortadella,"
            + "lardon,lardons,speck,chicharron,chicharrones,pork belly,pork chop,pork rind,pork rinds,spam,hot dog,hot dogs,frankfurter,bratwurst,"
            + "kielbasa,andouille,sausage,sausages,hog,jamon,serrano ham,char siu,boar");
    /** Sausages that name a non-pork meat. */
    static final Set<String> NOT_PORK = set("chicken sausage,turkey sausage,beef sausage,lamb sausage,merguez,vegan sausage,veggie sausage,"
            + "vegetarian sausage,plant based sausage,turkey bacon,beef bacon,turkey ham,halal sausage,beef hot dog,chicken hot dog,"
            + "hamburger,hamburger bun,hot dog bun,hot dog buns,graham,ham hock_x,eggplant,vegan bacon,coconut bacon,mushroom bacon,beef salami,beef pepperoni,turkey pepperoni");
    static final Set<String> GELATIN = set("gelatin,gelatine,gelatin sheet,gelatine leaf,jelly crystal,jello,marshmallow,marshmallows,gummy,gummies,aspic");
    static final Set<String> NOT_GELATIN = set("agar,agar agar,vegan gelatin,vegan marshmallow,halal gelatin,kosher gelatin,fish gelatin");
    static final Set<String> NO_SCALE_FISH = set("catfish,eel,monkfish,swordfish,shark,sturgeon,caviar,skate,ray,octopus,squid,calamari,cuttlefish");
    static final Set<String> GRAPE_ALCOHOL = set("wine,brandy,cognac,sherry,port,marsala,champagne,prosecco,vermouth,madeira,cava,grappa,armagnac,calvados_x");
    static final Set<String> NON_KOSHER_MEAT = set("rabbit,hare,horse,frog,snail,snails,escargot,boar,venison_x");
    static final Set<String> SHELLFISH_AND_MOLLUSC = set("prawn,shrimp,crab,lobster,crayfish,crawfish,langoustine,scampi,scallop,mussel,clam,"
            + "oyster,cockle,whelk,abalone,oyster sauce,shrimp paste,fish sauce_x,dried shrimp");

    public static boolean isPork(String key) {
        if (phrase(key, NOT_PORK) || NOT_PORK.contains(key)) return false;
        if (key.contains("eggplant") || key.contains("hamburger") || key.contains("graham")) return false;
        return word(key, PORK) || phrase(key, PORK);
    }
    public static boolean isGelatin(String key) { return !phrase(key, NOT_GELATIN) && (word(key, GELATIN) || phrase(key, GELATIN)); }

    private static boolean word(String key, Set<String> words) {
        if (words.contains(key)) return true;
        for (String w : key.split(" ")) if (words.contains(w)) return true;
        return false;
    }

    /** Phrase match: the key is a phrase or contains one (whole words). */
    private static boolean phrase(String key, Set<String> phrases) {
        String k = " " + key + " ";
        for (String p : phrases) if (k.contains(" " + p + " ")) return true;
        return false;
    }

    private static boolean has(String key, Set<String> words, Set<String> except) {
        return (word(key, words) || phrase(key, words)) && !phrase(key, except);
    }

    public static boolean isFish(String key) { return word(key, FISH_WORDS) || key.equals("fish sauce") || key.equals("oyster sauce"); }
    public static boolean isGrain(String key) {
        if (phrase(key, NOT_GRAIN)) return false;
        int c = category(key);
        return c == GRAINS || c == PASTA || c == BREAD || has(key, GRAIN_WORDS, NOT_GRAIN);
    }
    public static boolean isLegume(String key) {
        if (phrase(key, OK_LEGUME)) return false;
        return category(key) == LEGUMES || has(key, LEGUME_WORDS, OK_LEGUME);
    }

    private static final int GRAINS = idx("Grains & rice"), PASTA = idx("Pasta & noodles"), BREAD = idx("Bread & tortillas"),
            LEGUMES = idx("Beans & legumes"), SWEET = idx("Sweeteners");
    private static int idx(String c) { return Arrays.asList(Catalog.CATEGORIES).indexOf(c); }
    private static Map<String, Integer> catOf;

    /** The pantry-grid category of an ingredient key, or -1 (built once, on first use). */
    public static synchronized int category(String key) {
        if (catOf == null) {
            Map<String, Integer> m = new java.util.HashMap<String, Integer>();
            for (int c = 0; c < Catalog.ITEMS.length; c++) for (String it : Catalog.ITEMS[c]) {
                if (Catalog.isHeader(it)) continue;
                String k = Normalizer.normalize(it);
                if (!k.isEmpty() && !m.containsKey(k)) m.put(k, c);
            }
            catOf = m;
        }
        Integer c = catOf.get(key);
        return c == null ? -1 : c;
    }
    public static boolean isSugar(String key) { return !phrase(key, NOT_SUGAR) && (category(key) == SWEET || has(key, SUGAR_WORDS, NOT_SUGAR)); }
    public static boolean isAlcohol(String key) { return has(key, ALCOHOL_WORDS, NOT_ALCOHOL); }
    public static boolean isSeedOil(String key) { return phrase(key, SEED_OIL); }
    public static boolean isProcessedMeat(String key) { return word(key, PROCESSED_MEAT) || phrase(key, PROCESSED_MEAT); }
    public static boolean isRedMeat(String key) { return (word(key, RED_MEAT) || phrase(key, RED_MEAT)) && !key.contains("beef tomato") && !key.contains("mushroom"); }
    public static boolean isStarchy(String key) { return phrase(key, STARCHY) || word(key, STARCHY); }
    public static boolean isSalty(String key) { return has(key, SALTY, NOT_SALTY); }

    static boolean dairyNotGhee(String key) { return DietClassifier.isDairy(key) && !key.equals("ghee") && !key.contains("ghee"); }

    static boolean animal(String key) {
        if (phrase(key, ANIMAL_EXTRA) || word(key, ANIMAL_EXTRA)) {
            if (key.contains("eggplant") || key.contains("peanut butter") || key.contains("nut butter") || key.contains("almond butter")
                    || key.contains("butter bean") || key.contains("cocoa butter") || key.contains("vegan") || key.contains("butternut")) return false;
            return true;
        }
        if (DietClassifier.isDairy(key)) return !phrase(key, CARNIVORE_DAIRY_BAN) && !word(key, CARNIVORE_DAIRY_BAN);
        if (DietClassifier.isMeat(key)) {
            // meat words that come with plant ingredients or sugar
            return !(key.contains("worcestershire") || key.contains("sauce") || key.contains("marshmallow") || key.contains("gravy")
                    || key.contains("bouillon") || key.contains("seasoning") || key.contains("mince pie") || key.contains("dashi"));
        }
        return false;
    }

    /** Salt per serving in teaspoons, or NaN when there is salt without a measured amount ("to taste"). */
    static double saltTspPerServing(Recipe r) {
        double tsp = 0; boolean unmeasured = false;
        for (Ingredient i : r.ingredients) {
            if (i.optional || !SALT_KEYS.contains(i.key)) continue;
            if (Double.isNaN(i.amount)) { unmeasured = true; continue; }
            String u = i.unit.toLowerCase();
            double f;
            if (u.startsWith("tsp") || u.startsWith("teaspoon")) f = 1;
            else if (u.startsWith("tbsp") || u.startsWith("tablespoon")) f = 3;
            else if (u.startsWith("pinch")) f = 1.0 / 16;
            else if (u.startsWith("dash")) f = 1.0 / 8;
            else if (u.equals("g")) f = 1.0 / 6;    // ~6 g table salt per tsp
            else if (u.startsWith("cup")) f = 48;
            else f = 1;                            // unknown unit: assume a teaspoon each (conservative)
            tsp += i.amount * f;
        }
        double per = tsp / Math.max(1, r.servings);
        return per == 0 && unmeasured ? Double.NaN : per;
    }

    /** The new diet ids a recipe fits. foods may be null (then high-protein is never tagged). */
    public static Set<String> classify(Recipe r, Map<String, Nutrition.Food> foods) {
        boolean meat = false, nonFishMeat = false, grain = false, legume = false, sugar = false, paleoSweetOnly = true, alcohol = false,
                seedOil = false, processed = false, red = false, starchy = false, sugarFruit = false, ketoFruit = false, milk = false,
                dairy = false, butterCream = false, fishOrLegume = false, oliveOil = false, salty = false, nonAnimal = false, corn = false, any = false,
                pork = false, gelatin = false, anyDairy = false, notKosherSea = false, grapeAlcohol = false, notKosherMeat = false;
        for (Ingredient i : r.ingredients) {
            String k = i.key;
            if (i.optional || k.isEmpty()) continue;
            any = true;
            if (DietClassifier.isMeat(k)) { meat = true; if (!isFish(k)) nonFishMeat = true; }
            if (isFish(k)) fishOrLegume = true;
            if (isGrain(k)) grain = true;
            if (isLegume(k)) { legume = true; fishOrLegume = true; }
            if (isSugar(k)) { sugar = true; if (!(phrase(k, PALEO_SWEET) || PALEO_SWEET.contains(k))) paleoSweetOnly = false; }
            if (isAlcohol(k)) alcohol = true;
            if (isSeedOil(k)) seedOil = true;
            if (isProcessedMeat(k)) processed = true;
            if (isRedMeat(k)) red = true;
            if (isStarchy(k)) starchy = true;
            if (phrase(k, HIGH_SUGAR_FRUIT) || word(k, HIGH_SUGAR_FRUIT)) sugarFruit = true;
            if (phrase(k, KETO_FRUIT_BAN) || KETO_FRUIT_BAN.contains(k)) ketoFruit = true;
            boolean d = dairyNotGhee(k);
            if (d) dairy = true;
            if (d && (k.equals("milk") || k.endsWith(" milk") && !k.contains("coconut") && !k.contains("almond") || k.contains("condensed") || k.contains("evaporated"))) milk = true;
            if (k.contains("butter") && DietClassifier.isDairy(k) || k.contains("cream") && DietClassifier.isDairy(k) || k.contains("margarine")) butterCream = true;
            if (k.contains("olive oil")) oliveOil = true;
            if (isSalty(k)) salty = true;
            if (k.equals("corn") || k.contains("sweetcorn") || k.contains("corn ") || k.startsWith("corn") || k.contains("polenta") || k.contains("cornmeal")) corn = true;
            if (!animal(k) && !FREE.contains(k)) nonAnimal = true;
            if (isPork(k)) pork = true;
            if (isGelatin(k)) gelatin = true;
            if (DietClassifier.isDairy(k)) anyDairy = true;
            if (word(k, SHELLFISH_AND_MOLLUSC) || phrase(k, SHELLFISH_AND_MOLLUSC) || word(k, NO_SCALE_FISH)) notKosherSea = true;
            if (isAlcohol(k) && (word(k, GRAPE_ALCOHOL) || phrase(k, GRAPE_ALCOHOL)) && !k.contains("rice wine") && !k.contains("shaoxing")) grapeAlcohol = true;
            if (word(k, NON_KOSHER_MEAT)) notKosherMeat = true;
        }
        Set<String> out = new LinkedHashSet<String>();
        if (!any) return out;
        String cat = r.category == null ? "" : r.category.toLowerCase();
        boolean treat = cat.equals("dessert") || cat.equals("baking") || r.meals.contains("Dessert") || r.meals.contains("Baked goods");
        boolean drink = cat.equals("drink") || r.meals.contains("Drink");
        if (!nonFishMeat) out.add(PESCATARIAN);
        boolean medCuisine = r.cuisine != null && MED_CUISINES.contains(r.cuisine.toLowerCase());
        if (oliveOil && (medCuisine || fishOrLegume) && !red && !processed && !butterCream && !treat && !drink && !seedOil) out.add(MEDITERRANEAN);
        boolean lowCarb = !grain && !sugar && !starchy && !sugarFruit && !corn && !legume;
        if (lowCarb) out.add(LOW_CARB);
        if (lowCarb && !milk && !ketoFruit) out.add(KETO);
        boolean paleo = !grain && !legume && !dairy && (!sugar || paleoSweetOnly) && !seedOil && !alcohol && !corn;
        if (paleo) out.add(PALEO);
        if (paleo && !sugar && !processed && !treat && !alcohol) out.add(WHOLE30);
        if (!nonAnimal && !processed && (meat || dairy)) out.add(CARNIVORE);
        if (!salty) {
            double s = saltTspPerServing(r);
            if (Double.isNaN(s) || s <= 0.25 + 1e-9) out.add(LOW_SODIUM);
        }
        if (!pork && !gelatin && !alcohol) out.add(HALAL);
        if (!pork && !gelatin && !notKosherSea && !grapeAlcohol && !notKosherMeat && !(nonFishMeat && anyDairy)) out.add(KOSHER);
        if (foods != null) {
            Nutrition.Estimate e = Nutrition.estimate(r, foods);
            if (e.show() && e.protein >= 25 && e.kcal > 0 && e.protein * 4 >= 0.25 * e.kcal) out.add(HIGH_PROTEIN);
        }
        return out;
    }

    /** Why a recipe does not fit a diet: the ingredient names that rule it out (empty when it fits or for high-protein). */
    public static List<String> blockers(Recipe r, String diet) {
        List<String> out = new ArrayList<String>();
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty()) continue;
            Recipe one = new Recipe();
            one.category = ""; one.servings = r.servings;
            one.ingredients.add(i);
            if (diet.equals(CARNIVORE)) { if (!animal(i.key) && !FREE.contains(i.key)) out.add(i.name); continue; }
            if (diet.equals(HIGH_PROTEIN) || diet.equals(MEDITERRANEAN)) continue;
            if (!classify(one, null).contains(diet) && !(diet.equals(LOW_SODIUM) && SALT_KEYS.contains(i.key))) out.add(i.name);
        }
        if (diet.equals(KOSHER) && out.isEmpty() && !classify(r, null).contains(KOSHER)) out.add("meat or poultry together with dairy");
        return out;
    }

    /** Whether a recipe fits every one of the given diet ids (old and new ones). */
    public static boolean fitsAll(Recipe r, java.util.Collection<String> diets) {
        for (String d : diets) if (!r.tags.contains(d) && !r.diets.contains(d)) return false;
        return true;
    }
}
