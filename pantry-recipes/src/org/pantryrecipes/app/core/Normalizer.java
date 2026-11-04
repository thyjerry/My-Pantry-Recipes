package org.pantryrecipes.app.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns free-form ingredient names ("2 Chicken Breasts, diced", "Spring Onions", "aubergine")
 * into canonical keys ("chicken breast", "green onion", "eggplant") and knows simple
 * "family" relations (spaghetti -> pasta, cheddar -> cheese) for fuzzy pantry matching.
 */
public final class Normalizer {
    private Normalizer() { }

    private static final Map<String, String> SYN = new HashMap<String, String>();
    private static final Map<String, String> FAMILY = new HashMap<String, String>();
    private static final Set<String> DESCRIPTORS = new HashSet<String>();
    private static final Set<String> NO_SINGULAR = new HashSet<String>();
    private static final Map<String, String> IRREGULAR = new HashMap<String, String>();
    private static final Set<String> MEATS_AFTER_GROUND = new HashSet<String>();

    private static void syn(String canonical, String... variants) {
        SYN.put(canonical, canonical);
        for (String v : variants) SYN.put(v, canonical);
    }

    private static void fam(String family, String... members) {
        for (String m : members) FAMILY.put(m, family);
    }

    static {
        for (String w : ("fresh chopped diced sliced minced grated shredded large small medium finely roughly thinly "
                + "coarsely peeled boneless skinless raw cooked frozen dried whole organic ripe extra virgin plain "
                + "unsalted salted lowfat fat free range freerange to taste of can canned tinned jar jarred handful "
                + "pinch sprig bunch clove leaf heaped level softened melted beaten crushed packed light dark hot cold "
                + "warm boiling good quality baby cubed halved quartered trimmed rinsed drained cut into piece pieces "
                + "and or for serving garnish optional about approx fillet fillets thick thin skin on off mixed few some "
                + "a an the stick sticks head heads loaf block wedge wedges juiced zested deseeded seeded pitted stoned "
                + "firm soft mature strong mild new young lean premium free-range room temperature topping your choice "
                + "favourite favorite tablespoon teaspoon cup cups tbsp tsp g kg ml oz lb").split(" ")) {
            DESCRIPTORS.add(w);
        }
        for (String w : ("hummus couscous asparagus molasses swiss lemongrass bass watercress grass glass octopus "
                + "citrus hibiscus haricots gras mascarpone feta paneer series christmas brussels "
                + "harissa salsa pancetta pasta passata quinoa tahini this is us").split(" ")) {
            NO_SINGULAR.add(w);
        }
        IRREGULAR.put("leaves", "leaf");
        IRREGULAR.put("chillies", "chilli");
        IRREGULAR.put("chilies", "chili");
        IRREGULAR.put("halves", "half");
        IRREGULAR.put("loaves", "loaf");
        IRREGULAR.put("cookies", "cookie");
        IRREGULAR.put("brownies", "brownie");
        IRREGULAR.put("knives", "knife");
        IRREGULAR.put("oats", "oats");
        IRREGULAR.put("greens", "greens");
        IRREGULAR.put("noodles", "noodle");
        IRREGULAR.put("chives", "chive");
        IRREGULAR.put("olives", "olive");
        IRREGULAR.put("anchovies", "anchovy");
        IRREGULAR.put("tomatoes", "tomato");
        IRREGULAR.put("potatoes", "potato");
        IRREGULAR.put("mangoes", "mango");
        IRREGULAR.put("dates", "date");
        IRREGULAR.put("grapes", "grape");
        IRREGULAR.put("sauces", "sauce");
        IRREGULAR.put("pieces", "piece");
        IRREGULAR.put("slices", "slice");
        IRREGULAR.put("spices", "spice");
        IRREGULAR.put("juices", "juice");
        IRREGULAR.put("rices", "rice");
        IRREGULAR.put("cheeses", "cheese");
        IRREGULAR.put("breadcrumbs", "breadcrumb");
        for (String m : "beef pork turkey chicken lamb veal".split(" ")) MEATS_AFTER_GROUND.add(m);

        // ---- synonyms: variant -> canonical (all already singular, lower case) ----
        syn("green onion", "scallion", "spring onion", "salad onion", "green onion");
        syn("eggplant", "aubergine");
        syn("zucchini", "courgette");
        syn("cilantro", "coriander leaf", "fresh coriander", "chopped coriander", "coriander leave", "cilantro leaf");
        syn("coriander", "ground coriander", "coriander seed", "coriander powder");
        syn("chickpea", "garbanzo bean", "chick pea", "garbanzo", "chickpea");
        syn("shrimp", "prawn", "king prawn", "tiger prawn", "raw king prawn", "jumbo shrimp");
        syn("bell pepper", "capsicum", "red pepper", "green pepper", "yellow pepper", "orange pepper", "sweet pepper",
                "red bell pepper", "green bell pepper", "yellow bell pepper", "pepper bell");
        syn("chili", "chilli", "chile", "red chilli", "green chilli", "red chili", "green chili", "chili pepper",
                "chilli pepper", "jalapeno", "jalapeno pepper", "bird eye chilli", "thai chili", "fresno chili", "serrano");
        syn("chili flake", "chilli flake", "red pepper flake", "crushed red pepper", "red chilli flake",
                "dried chilli flake", "crushed chilli", "crushed chili", "pepper flake");
        syn("chili powder", "chilli powder", "red chilli powder", "cayenne", "cayenne pepper", "kashmiri chilli powder");
        syn("ground beef", "minced beef", "beef mince", "mince", "lean minced beef", "hamburger meat", "ground chuck");
        syn("ground pork", "minced pork", "pork mince");
        syn("ground lamb", "minced lamb", "lamb mince");
        syn("ground turkey", "minced turkey", "turkey mince");
        syn("ground chicken", "minced chicken", "chicken mince");
        syn("flour", "plain flour", "all purpose flour", "allpurpose flour", "white flour", "wheat flour",
                "plain white flour", "strong white bread flour", "bread flour");
        syn("self raising flour", "self rising flour", "selfraising flour");
        syn("sugar", "caster sugar", "granulated sugar", "white sugar", "superfine sugar", "castor sugar");
        syn("powdered sugar", "icing sugar", "confectioner sugar", "confectioners sugar");
        syn("brown sugar", "light brown sugar", "dark brown sugar", "muscovado sugar", "soft brown sugar", "demerara sugar");
        syn("cornstarch", "cornflour", "corn starch", "corn flour");
        syn("baking soda", "bicarbonate of soda", "bicarb", "bicarbonate soda", "bicarb soda");
        syn("heavy cream", "double cream", "whipping cream", "heavy whipping cream", "thickened cream");
        syn("cream", "single cream", "light cream", "cooking cream");
        syn("yogurt", "yoghurt", "natural yogurt", "greek yogurt", "greek yoghurt", "natural yoghurt", "plain yogurt");
        syn("arugula", "rocket");
        syn("beet", "beetroot");
        syn("rutabaga", "swede");
        // v2.0 catalog additions and local names
        syn("quark", "twarog", "tvorog", "farmer cheese", "farmers cheese", "topfen");
        syn("smetana", "smetana sour cream");
        syn("sazon seasoning", "sazon", "sazon goya");
        syn("cassava", "yuca", "manioc", "cassava root");
        syn("taro", "dasheen", "eddo", "eddoes", "taro root");
        syn("chayote", "cho cho", "christophene", "chocho", "mirliton");
        syn("pigeon pea", "gungo pea", "gandule", "gandules");
        syn("callaloo", "amaranth greens", "callaloo leaves");
        syn("ackee", "canned ackee", "ackees");
        syn("shichimi togarashi", "shichimi", "togarashi");
        syn("piment d espelette", "espelette pepper");
        syn("yuzu juice", "yuzu");
        syn("pandan leaves", "pandan", "pandan leaf", "screwpine leaves");
        syn("niter kibbeh", "nit ir kibe", "spiced clarified butter");
        syn("egusi seed", "egusi", "ground egusi", "melon seed");
        syn("suya spice", "yaji", "suya pepper");
        syn("fufu flour", "fufu");
        syn("green seasoning", "trinidad green seasoning", "bajan seasoning");
        syn("mojo marinade", "mojo", "mojo criollo");
        syn("yeast extract", "yeast extract spread");
        syn("lingonberry jam", "lingonberry preserves");
        syn("guava paste", "guava cheese", "goiabada");
        syn("snow pea", "mangetout", "sugar snap pea", "snap pea");
        syn("black pepper", "pepper", "ground black pepper", "peppercorn", "black peppercorn", "salt pepper",
                "cracked black pepper", "freshly ground black pepper", "white pepper");
        syn("salt", "sea salt", "kosher salt", "table salt", "flaky salt", "sea salt flake", "rock salt");
        syn("olive oil", "extra virgin olive oil", "evoo");
        syn("vegetable oil", "sunflower oil", "canola oil", "rapeseed oil", "corn oil", "groundnut oil", "peanut oil");
        syn("oil", "cooking oil");
        syn("chicken stock", "chicken broth", "chicken stock cube", "chicken bouillon", "chicken stock pot");
        syn("vegetable stock", "vegetable broth", "vegetable stock cube", "veg stock", "vegetable bouillon");
        syn("beef stock", "beef broth", "beef stock cube", "beef bouillon");
        syn("tomato paste", "tomato puree", "concentrated tomato puree", "tomato concentrate");
        syn("canned tomato", "chopped tomato", "crushed tomato", "tinned tomato", "plum tomato", "diced tomato",
                "can chopped tomato", "tinned chopped tomato", "canned chopped tomato", "whole peeled tomato",
                "peeled tomato", "tomato sauce", "passata", "tomato passata");
        syn("cherry tomato", "grape tomato");
        syn("sun dried tomato", "sundried tomato", "sun dried tomato in oil");
        syn("onion", "white onion", "yellow onion", "brown onion", "spanish onion");
        syn("red onion", "purple onion");
        syn("parmesan", "parmesan cheese", "parmigiano reggiano", "parmigianoreggiano", "grana padano", "pecorino");
        syn("cheddar", "cheddar cheese", "mature cheddar", "sharp cheddar");
        syn("mozzarella", "mozzarella cheese", "mozzarella ball", "buffalo mozzarella");
        syn("feta", "feta cheese");
        syn("cream cheese", "soft cheese", "philadelphia");
        syn("milk", "whole milk", "semi skimmed milk", "skimmed milk", "skim milk", "semiskimmed milk", "full fat milk", "2% milk");
        syn("egg", "free range egg", "hen egg", "egg yolk", "egg white");
        syn("lemon", "lemon juice", "lemon zest", "lemon rind");
        syn("lime", "lime juice", "lime zest");
        syn("ginger", "ginger root", "root ginger", "ground ginger", "ginger paste");
        syn("garlic", "garlic clove", "clove garlic", "garlic paste", "minced garlic");
        syn("soy sauce", "soya sauce", "light soy sauce", "dark soy sauce", "shoyu");
        syn("chicken", "whole chicken", "chicken piece");
        syn("chicken breast", "chicken breast fillet", "chicken fillet", "skinless chicken breast");
        syn("chicken thigh", "chicken thigh fillet", "boneless chicken thigh");
        syn("bacon", "streaky bacon", "smoked bacon", "back bacon", "bacon rasher", "bacon strip", "lardon");
        syn("sausage", "pork sausage", "sausage link", "sausage meat");
        syn("beef", "stewing beef", "beef chuck", "braising steak", "chuck steak", "stew meat", "beef brisket", "brisket");
        syn("steak", "sirloin steak", "beef steak", "rump steak", "ribeye steak", "sirloin", "ribeye", "flank steak",
                "bavette steak", "bavette", "flat iron steak", "hanger steak", "t-bone steak", "t bone steak", "porterhouse steak", "strip steak"); // v2.1
        syn("salmon", "salmon fillet", "salmon steak");
        syn("white fish", "cod", "haddock", "white fish fillet", "pollock", "tilapia", "hake", "basa");
        syn("tuna", "canned tuna", "tinned tuna", "tuna in brine", "tuna in oil", "tuna chunk");
        syn("potato", "baby potato", "baking potato", "new potato", "russet potato", "floury potato", "maris piper", "waxy potato", "yukon gold");
        syn("sweet potato", "yam");
        syn("bread", "white bread", "wholemeal bread", "whole wheat bread", "sliced bread", "bread slice", "sandwich bread");
        syn("breadcrumb", "panko", "panko breadcrumb", "bread crumb", "dried breadcrumb");
        syn("rice", "white rice", "long grain rice", "basmati rice", "jasmine rice", "basmati", "longgrain rice", "cooked rice");
        syn("arborio rice", "risotto rice", "carnaroli rice");
        syn("mushroom", "button mushroom", "chestnut mushroom", "cremini mushroom", "white mushroom", "baby bella mushroom");
        syn("lettuce", "romaine", "romaine lettuce", "iceberg lettuce", "little gem lettuce", "cos lettuce", "little gem");
        syn("green bean", "french bean", "string bean", "fine bean", "runner bean");
        syn("kidney bean", "red kidney bean");
        syn("white bean", "cannellini bean", "butter bean", "navy bean", "haricot bean", "lima bean", "great northern bean");
        syn("tortilla", "flour tortilla", "wrap", "tortilla wrap", "soft tortilla");
        syn("cumin", "ground cumin", "cumin seed", "cumin powder");
        syn("paprika", "smoked paprika", "sweet paprika", "hot paprika", "paprika powder");
        syn("turmeric", "ground turmeric", "turmeric powder");
        syn("cinnamon", "ground cinnamon", "cinnamon stick", "cinnamon powder");
        syn("nutmeg", "ground nutmeg");
        syn("parsley", "flat leaf parsley", "flatleaf parsley", "italian parsley", "curly parsley", "parsley leaf");
        syn("basil", "basil leaf", "sweet basil");
        syn("mint", "mint leaf");
        syn("oregano", "dried oregano");
        syn("thyme", "thyme leaf", "dried thyme");
        syn("bay leaf", "bay", "dried bay leaf");
        syn("water", "cold water", "warm water", "hot water", "boiling water", "ice water", "tap water");
        syn("spinach", "baby spinach", "spinach leaf");
        syn("butter", "unsalted butter", "salted butter");
        syn("honey", "clear honey", "runny honey");
        syn("maple syrup", "pure maple syrup");
        syn("vanilla extract", "vanilla", "vanilla essence", "vanilla pod");
        syn("oats", "rolled oat", "porridge oat", "oat", "rolled oats", "porridge oats", "oatmeal", "quick oat");
        syn("lentil", "red lentil", "green lentil", "brown lentil", "split red lentil", "puy lentil", "yellow lentil");
        syn("pasta", "dried pasta", "short pasta");
        syn("spaghetti", "spaghettini");
        syn("penne", "penne rigate", "penne pasta");
        syn("macaroni", "elbow macaroni", "macaroni pasta");
        syn("noodle", "ramen noodle", "instant noodle", "udon noodle", "wheat noodle", "lo mein noodle");
        syn("rice noodle", "rice vermicelli", "flat rice noodle", "pad thai noodle", "vermicelli rice noodle");
        syn("coconut milk", "light coconut milk", "coconut milk can");
        syn("peanut butter", "smooth peanut butter", "crunchy peanut butter");
        syn("sesame oil", "toasted sesame oil");
        syn("mayonnaise", "mayo");
        syn("sour cream", "soured cream");
        syn("creme fraiche", "creme fraich");
        syn("carrot", "baby carrot");
        syn("celery", "celery stalk", "celery stick", "celery rib");
        syn("cucumber", "english cucumber");
        syn("pea", "frozen pea", "garden pea", "petit pois", "green pea");
        syn("sweetcorn", "corn", "corn kernel", "sweet corn", "corn on the cob", "frozen corn", "canned corn", "corn cob", "corn ear");
        syn("broccoli", "broccoli floret", "tenderstem broccoli");
        syn("cauliflower", "cauliflower floret");
        syn("cabbage", "white cabbage", "green cabbage", "savoy cabbage", "red cabbage");
        syn("kale", "curly kale", "cavolo nero", "lacinato kale");
        syn("apple", "green apple", "red apple", "granny smith apple", "cooking apple", "bramley apple");
        syn("banana", "ripe banana");
        syn("avocado", "ripe avocado", "hass avocado");
        syn("tofu", "firm tofu", "extra firm tofu", "silken tofu", "tofu block");
        syn("dijon mustard", "dijon", "wholegrain mustard", "whole grain mustard", "mustard", "english mustard", "yellow mustard");
        syn("worcestershire sauce", "worcester sauce", "worcestershire");
        syn("hot sauce", "tabasco", "sriracha", "tabasco sauce", "sriracha sauce", "chili sauce", "chilli sauce");
        syn("garam masala", "garam masala powder");
        syn("curry powder", "mild curry powder", "madras curry powder", "hot curry powder");
        syn("chickpea flour", "gram flour", "besan");
        syn("baking powder", "baking powder gluten free");
        syn("ham", "cooked ham", "sliced ham", "smoked ham");
        syn("chorizo", "chorizo sausage");
        syn("quinoa", "white quinoa", "red quinoa");
        syn("couscous", "giant couscous", "pearl couscous");
        syn("balsamic vinegar", "balsamic");
        syn("white wine vinegar", "white vinegar", "distilled white vinegar");
        syn("cider vinegar", "apple cider vinegar");
        syn("rice vinegar", "rice wine vinegar");
        syn("red wine vinegar", "sherry vinegar");
        syn("pine nut", "pine kernel", "pinenut");
        syn("walnut", "walnut half", "walnut piece");
        syn("almond", "flaked almond", "slivered almond", "whole almond");
        syn("cashew", "cashew nut");
        syn("peanut", "roasted peanut", "unsalted peanut");
        syn("raisin", "sultana", "currant");
        syn("chocolate", "dark chocolate", "milk chocolate", "chocolate chip", "plain chocolate", "chocolate chunk");
        syn("cocoa powder", "cocoa", "unsweetened cocoa powder", "cacao powder");
        syn("lamb", "lamb shoulder", "leg of lamb", "lamb leg", "diced lamb", "lamb chop", "lamb shank");
        syn("pork", "pork shoulder", "pork loin", "pork chop", "pork tenderloin", "pork belly");
        syn("turkey", "turkey breast");
        syn("pita", "pitta", "pita bread", "pitta bread");
        syn("naan", "naan bread");
        syn("tahini", "tahini paste");
        syn("gochujang", "gochujang paste");
        syn("miso", "miso paste", "white miso", "red miso");
        syn("fish sauce", "thai fish sauce", "nam pla");
        syn("oyster sauce", "oyster flavored sauce");
        syn("puff pastry", "ready rolled puff pastry", "all butter puff pastry");
        syn("ricotta", "ricotta cheese");
        syn("goat cheese", "goats cheese", "chevre");
        syn("pesto", "basil pesto", "green pesto");
        syn("salsa", "tomato salsa");
        syn("sesame seed", "toasted sesame seed", "white sesame seed");
        syn("star anise", "star anise pod");
        syn("cardamom", "cardamom pod", "green cardamom", "ground cardamom");
        syn("clove", "whole clove", "ground clove");
        syn("dill", "dill weed", "fresh dill");
        syn("rosemary", "rosemary sprig");
        syn("sage", "sage leaf");
        syn("chive", "fresh chive");
        syn("shallot", "banana shallot", "echalion");
        syn("leek");
        syn("pumpkin", "butternut squash", "squash", "butternut", "pumpkin puree");
        syn("asparagus", "asparagus spear", "asparagus tip");
        syn("orange", "orange juice", "orange zest");
        syn("berry", "mixed berry", "frozen berry");
        syn("strawberry", "fresh strawberry");
        syn("blueberry", "fresh blueberry");

        // ---- families: specific -> general ----
        fam("chicken", "chicken breast", "chicken thigh", "chicken drumstick", "chicken wing", "chicken leg", "ground chicken");
        fam("beef", "ground beef", "steak");
        fam("pork", "ground pork", "pork chop");
        fam("lamb", "ground lamb");
        fam("turkey", "ground turkey");
        fam("fish", "salmon", "white fish", "tuna", "trout", "mackerel", "sardine");
        fam("cheese", "cheddar", "parmesan", "mozzarella", "feta", "cream cheese", "ricotta", "goat cheese", "halloumi",
                "gruyere", "brie", "paneer", "monterey jack", "swiss cheese", "blue cheese", "gouda", "mascarpone");
        fam("oil", "olive oil", "vegetable oil");
        fam("stock", "chicken stock", "vegetable stock", "beef stock");
        fam("pasta", "spaghetti", "penne", "macaroni", "fusilli", "linguine", "rigatoni", "farfalle", "tagliatelle",
                "lasagne sheet", "orzo", "fettuccine");
        fam("noodle", "rice noodle", "egg noodle");
        fam("tomato", "cherry tomato", "canned tomato");
        fam("onion", "red onion", "shallot");
        fam("rice", "arborio rice", "brown rice", "sushi rice");
        fam("bean", "kidney bean", "black bean", "white bean", "pinto bean");
        fam("vinegar", "white wine vinegar", "red wine vinegar", "balsamic vinegar", "cider vinegar", "rice vinegar", "malt vinegar");
        fam("sugar", "brown sugar", "powdered sugar");
        fam("cream", "heavy cream");
        fam("flour", "self raising flour");
        fam("bread", "baguette", "sourdough", "ciabatta", "pita", "naan", "bread roll", "burger bun");
        fam("nut", "walnut", "almond", "cashew", "peanut", "pine nut", "pecan", "hazelnut", "pistachio");
        fam("berry", "strawberry", "blueberry", "raspberry", "blackberry");
        // ---- expanded catalog vocabulary (v1.1) ----
        // vegetables
        syn("red cabbage", "purple cabbage");
        syn("napa cabbage", "chinese cabbage", "chinese leaf", "wombok");
        syn("bok choy", "pak choi", "bok choi", "baby bok choy", "pak choy", "baby pak choi");
        syn("broccolini", "baby broccoli");
        syn("kohlrabi");
        syn("brussels sprout", "brussel sprout", "brussels sprouts");
        syn("swiss chard", "chard", "rainbow chard", "silverbeet");
        syn("collard greens", "collard", "collards", "collard green");
        syn("watercress", "cress");
        syn("mixed salad greens", "salad leaf", "mixed leaf", "spring mix", "salad greens", "mixed greens", "mixed salad leaf", "salad mix");
        syn("endive", "chicory", "belgian endive");
        syn("radicchio");
        syn("parsnip");
        syn("turnip");
        syn("radish", "red radish");
        syn("celeriac", "celery root");
        syn("daikon", "mooli", "daikon radish", "white radish");
        syn("jicama");
        syn("horseradish", "horseradish root", "fresh horseradish");
        syn("acorn squash");
        syn("spaghetti squash");
        syn("jalapeno", "jalapeno pepper", "jalapeno chili", "jalapeno chilli");
        syn("poblano", "poblano pepper", "poblano chili");
        syn("habanero", "scotch bonnet", "habanero pepper", "scotch bonnet pepper");
        syn("portobello mushroom", "portobello", "portabella", "portabello mushroom", "portobello cap");
        syn("shiitake mushroom", "shiitake", "dried shiitake", "shitake mushroom", "shiitake mushroom cap");
        syn("oyster mushroom");
        syn("dried porcini", "porcini", "porcini mushroom", "dried porcini mushroom", "cep");
        syn("edamame", "edamame bean", "soybean", "soya bean", "frozen edamame");
        syn("okra", "lady finger okra", "bhindi");
        syn("artichoke", "globe artichoke");
        syn("fennel", "fennel bulb", "florence fennel");
        syn("bean sprout", "beansprout", "mung bean sprout");
        // fruit
        syn("grapefruit", "pink grapefruit");
        syn("mandarin", "clementine", "tangerine", "satsuma", "mandarin orange");
        syn("pear", "conference pear", "bartlett pear");
        syn("raspberry", "fresh raspberry");
        syn("blackberry", "fresh blackberry");
        syn("cranberry", "fresh cranberry");
        syn("peach", "fresh peach");
        syn("nectarine");
        syn("plum");
        syn("cherry", "sweet cherry", "fresh cherry");
        syn("apricot", "fresh apricot");
        syn("mango", "frozen mango", "mango chunk");
        syn("pineapple", "pineapple chunk", "canned pineapple", "pineapple ring");
        syn("papaya", "pawpaw");
        syn("kiwi", "kiwi fruit", "kiwifruit");
        syn("passion fruit", "passionfruit");
        syn("coconut", "fresh coconut");
        syn("pomegranate", "pomegranate seed", "pomegranate aril");
        syn("watermelon");
        syn("cantaloupe", "melon", "honeydew", "honeydew melon", "rockmelon");
        syn("grape", "red grape", "green grape", "seedless grape");
        syn("fig", "fresh fig");
        syn("dried cranberry", "craisin", "sweetened dried cranberry");
        syn("dried apricot");
        syn("date", "medjool date", "pitted date");
        syn("prune", "dried plum");
        syn("dried fig");
        // herbs
        syn("tarragon", "fresh tarragon");
        syn("thai basil", "holy basil");
        syn("lemongrass", "lemon grass", "lemongrass stalk");
        syn("curry leaf", "fresh curry leaf", "dried curry leaf");
        syn("dried basil");
        syn("dried thyme");
        syn("dried rosemary");
        syn("dried parsley", "parsley flake");
        syn("dried dill");
        syn("dried mint");
        syn("marjoram", "dried marjoram");
        syn("italian seasoning", "italian herb", "mixed herb", "dried mixed herb", "italian herb seasoning", "dried italian herb");
        syn("herbe de provence", "herb de provence");
        // salt, pepper, spices
        syn("white pepper", "ground white pepper");
        syn("garlic salt");
        syn("seasoned salt", "lawry seasoned salt");
        syn("smoked paprika", "pimenton", "smoked sweet paprika", "spanish smoked paprika");
        syn("ground ginger", "ginger powder");
        syn("allspice", "ground allspice", "pimento berry");
        syn("mustard powder", "dry mustard", "english mustard powder", "ground mustard");
        syn("fenugreek", "fenugreek seed", "methi seed", "ground fenugreek");
        syn("sumac", "ground sumac");
        syn("garlic powder", "granulated garlic", "garlic granule");
        syn("onion powder", "onion granule", "granulated onion");
        syn("cayenne", "cayenne pepper", "ground cayenne");
        syn("chipotle powder", "chipotle chili powder", "chipotle chilli powder", "ground chipotle");
        syn("kashmiri chili powder", "kashmiri chilli powder", "kashmiri chili", "kashmiri red chilli powder");
        syn("mustard seed", "black mustard seed", "yellow mustard seed", "brown mustard seed");
        syn("fennel seed", "ground fennel");
        syn("caraway seed", "caraway");
        syn("saffron", "saffron thread", "saffron strand");
        syn("vanilla bean", "vanilla pod");
        syn("taco seasoning", "taco seasoning mix", "fajita seasoning");
        syn("cajun seasoning", "cajun spice", "creole seasoning");
        syn("chinese five spice", "five spice", "five spice powder", "chinese five spice powder");
        syn("za atar", "zaatar", "zahtar");
        syn("ras el hanout");
        syn("pumpkin pie spice", "pumpkin spice", "mixed spice", "apple pie spice");
        syn("everything bagel seasoning", "everything seasoning");
        syn("seafood seasoning", "old bay seasoning", "old bay");
        syn("jerk seasoning", "jerk spice", "jamaican jerk seasoning");
        syn("bouillon powder", "stock powder", "bouillon", "vegetable bouillon powder", "chicken bouillon powder");
        // meat
        syn("brisket", "beef brisket");
        syn("beef roast", "chuck roast", "beef joint", "topside", "roasting joint", "pot roast", "rump roast", "beef rib roast", "rib roast", "prime rib"); // v2.1 adds rib roasts
        syn("short rib", "beef short rib");
        syn("pork chop", "pork loin chop", "bone in pork chop");
        syn("pork shoulder", "pork butt", "boston butt", "pork shoulder joint");
        syn("pork tenderloin", "pork fillet");
        syn("pork belly", "pork belly slice");
        syn("pork loin", "pork loin joint");
        syn("pork rib", "spare rib", "sparerib", "baby back rib", "pork spare rib", "rack of rib", "rib");
        syn("lamb chop", "lamb cutlet", "lamb loin chop");
        syn("lamb shoulder", "shoulder of lamb");
        syn("veal", "veal cutlet", "veal escalope");
        syn("goat", "goat meat", "mutton");
        syn("venison", "deer meat", "venison loin", "venison steak", "ground venison"); // v2.1 adds cuts
        syn("chicken drumstick", "drumstick", "chicken drum");
        syn("chicken wing", "wing", "chicken wingette");
        syn("rotisserie chicken", "roast chicken", "cooked chicken", "leftover chicken", "shredded chicken");
        syn("duck", "duck breast", "duck leg");
        // fish & seafood
        syn("tuna steak", "fresh tuna", "ahi tuna", "tuna loin");
        syn("trout", "rainbow trout", "trout fillet");
        syn("mackerel", "mackerel fillet", "smoked mackerel");
        syn("sardine", "canned sardine", "sardines in oil", "sardines in water"); // v2.1 adds in water
        syn("sea bass", "seabass", "branzino", "sea bass fillet");
        syn("swordfish", "swordfish steak");
        syn("smoked salmon", "lox", "gravlax");
        syn("anchovy", "anchovy fillet");
        syn("fish stick", "fish finger");
        syn("scallop", "sea scallop", "bay scallop");
        syn("mussel", "fresh mussel");
        syn("clam", "baby clam", "canned clam");
        syn("crab", "crab meat", "crabmeat", "lump crab", "crab stick", "imitation crab");
        syn("lobster", "lobster tail");
        syn("squid", "calamari", "squid ring");
        // deli
        syn("italian sausage", "sweet italian sausage", "hot italian sausage", "italian sausage link");
        syn("salami", "genoa salami");
        syn("pepperoni", "pepperoni slice");
        syn("prosciutto", "parma ham", "serrano ham");
        syn("pancetta", "cubed pancetta");
        syn("hot dog", "frankfurter", "wiener", "hot dog sausage", "frank");
        syn("deli turkey", "sliced turkey", "turkey slice", "turkey deli meat", "deli sliced turkey");
        syn("roast beef slice", "sliced roast beef", "deli roast beef");
        syn("smoked sausage", "kielbasa", "polish sausage", "andouille", "andouille sausage");
        syn("bratwurst", "brat");
        // dairy
        syn("buttermilk", "cultured buttermilk");
        syn("half and half", "half half", "halfandhalf");
        syn("evaporated milk");
        syn("condensed milk", "sweetened condensed milk");
        syn("whipped cream", "cool whip", "whipped topping", "squirty cream");
        syn("ghee", "clarified butter");
        syn("kefir");
        // cheese
        syn("monterey jack", "jack cheese", "monterey jack cheese");
        syn("swiss cheese", "emmental", "emmentaler", "jarlsberg");
        syn("american cheese", "processed cheese", "cheese slice", "kraft single");
        syn("colby jack", "colby", "colby cheese");
        syn("pepper jack", "pepper jack cheese");
        syn("provolone", "provolone cheese");
        syn("gruyere", "gruyere cheese", "comte");
        syn("manchego", "manchego cheese");
        syn("gouda", "smoked gouda", "gouda cheese");
        syn("cottage cheese");
        syn("brie", "camembert");
        syn("burrata");
        syn("halloumi", "haloumi", "halloumi cheese");
        syn("paneer", "paneer cube");
        syn("queso fresco", "queso blanco");
        syn("blue cheese", "gorgonzola", "stilton", "roquefort", "dolcelatte");
        syn("cotija", "cotija cheese");
        syn("mascarpone", "mascarpone cheese");
        // plant-based
        syn("tempeh");
        syn("seitan", "wheat gluten", "vital wheat gluten");
        syn("soy milk", "soya milk", "soymilk");
        syn("almond milk", "unsweetened almond milk");
        syn("oat milk", "oatly");
        syn("coconut yogurt", "coconut yoghurt", "dairy free yogurt", "vegan yogurt");
        syn("vegan butter", "plant butter", "dairy free spread", "margarine", "vegan margarine");
        syn("vegan cheese", "dairy free cheese", "plant based cheese");
        syn("nutritional yeast", "nooch", "nutritional yeast flake");
        syn("plant based mince", "vegan mince", "meatless crumble", "beyond meat", "impossible meat", "plant mince", "quorn mince");
        syn("veggie burger", "vegan burger patty", "veggie patty", "plant based burger");
        syn("textured vegetable protein", "tvp", "soy mince");
        syn("aquafaba", "chickpea water");
        // grains
        syn("brown rice", "brown basmati rice", "wholegrain rice");
        syn("sushi rice", "short grain rice");
        syn("wild rice");
        syn("sticky rice", "glutinous rice", "sweet rice");
        syn("bulgur", "bulgur wheat", "bulghur", "cracked wheat");
        syn("barley", "pearl barley");
        syn("farro", "spelt", "emmer");
        syn("polenta", "instant polenta", "quick cook polenta");
        syn("cornmeal", "yellow cornmeal", "corn meal", "fine cornmeal");
        syn("millet");
        syn("buckwheat", "buckwheat groat", "kasha");
        syn("grit", "hominy grit", "grits", "quick grit");
        // pasta & noodles
        syn("lasagne sheet", "lasagna sheet", "lasagna noodle", "lasagne", "lasagna", "lasagne pasta");
        syn("tortellini", "cheese tortellini");
        syn("gnocchi", "potato gnocchi");
        syn("ravioli", "cheese ravioli");
        syn("udon noodle", "udon");
        syn("soba noodle", "soba");
        syn("ramen noodle", "ramen", "instant noodle", "instant ramen");
        syn("glass noodle", "cellophane noodle", "mung bean noodle", "bean thread noodle", "glass vermicelli");
        syn("lo mein noodle", "chow mein noodle");
        // bread
        syn("whole wheat bread", "wholemeal bread", "brown bread", "whole grain bread", "multigrain bread");
        syn("sourdough", "sourdough bread");
        syn("bread roll", "dinner roll", "roll", "crusty roll");
        syn("burger bun", "hamburger bun", "brioche bun", "slider bun");
        syn("hot dog bun", "hotdog bun");
        syn("english muffin");
        syn("bagel", "plain bagel");
        syn("brioche", "brioche loaf");
        syn("croissant");
        syn("flatbread", "flat bread");
        syn("taco shell", "hard taco shell", "crispy taco shell");
        syn("pizza dough", "pizza base", "pizza crust");
        syn("cracker", "saltine", "water cracker", "cream cracker");
        syn("crouton");
        syn("corn tortilla", "corn tortillas", "white corn tortilla");
        // legumes
        syn("pinto bean");
        syn("refried bean", "refried beans");
        syn("baked bean", "baked beans in tomato sauce", "beans in tomato sauce");
        syn("split pea", "yellow split pea", "green split pea");
        syn("black eyed pea", "blackeyed pea", "cowpea");
        syn("mung bean", "green gram");
        syn("fava bean", "broad bean");
        // nuts & seeds
        syn("pecan", "pecan half", "pecan nut");
        syn("pistachio", "shelled pistachio", "pistachio nut");
        syn("hazelnut", "filbert");
        syn("macadamia nut", "macadamia");
        syn("sunflower seed");
        syn("pumpkin seed", "pepita");
        syn("chia seed");
        syn("flaxseed", "flax seed", "linseed", "ground flaxseed", "flaxseed meal", "ground flax");
        syn("hemp seed", "hemp heart");
        syn("poppy seed");
        syn("almond butter");
        syn("chocolate hazelnut spread", "nutella", "hazelnut spread");
        // baking
        syn("whole wheat flour", "wholemeal flour", "whole wheat flour", "wholewheat flour");
        syn("almond flour", "ground almond", "almond meal");
        syn("coconut flour");
        syn("rice flour", "white rice flour", "glutinous rice flour");
        syn("yeast", "dried yeast", "instant yeast", "active dry yeast", "fast action yeast", "dry yeast", "fast action dried yeast");
        syn("cream of tartar");
        syn("gelatin", "gelatine", "gelatin powder", "gelatine leaf");
        syn("white chocolate", "white chocolate chip");
        syn("almond extract", "almond essence");
        syn("shredded coconut", "desiccated coconut", "coconut flake", "flaked coconut", "unsweetened shredded coconut");
        syn("sprinkle", "hundreds and thousands", "sprinkles");
        syn("marshmallow", "mini marshmallow");
        syn("pie crust", "shortcrust pastry", "pastry case", "pie shell", "pie dough", "pastry");
        syn("phyllo dough", "filo pastry", "phyllo", "filo", "phyllo pastry", "filo sheet");
        syn("graham cracker", "digestive biscuit", "digestive", "graham cracker crumb");
        syn("food coloring", "food colouring", "food color", "gel food coloring");
        // sweeteners
        syn("agave syrup", "agave nectar", "agave");
        syn("molasses", "treacle", "black treacle", "blackstrap molasses");
        syn("golden syrup", "light treacle");
        syn("corn syrup", "light corn syrup");
        syn("coconut sugar", "palm sugar");
        syn("stevia", "sweetener");
        syn("date syrup");
        syn("jam", "jelly", "grape jelly", "fruit preserve", "strawberry jam", "raspberry jam", "preserve", "apricot jam");
        syn("caramel sauce", "caramel", "salted caramel sauce");
        // oils & vinegars
        syn("coconut oil", "virgin coconut oil");
        syn("avocado oil");
        syn("cooking spray", "oil spray", "nonstick spray", "spray oil", "nonstick cooking spray");
        syn("chili oil", "chilli oil", "chili crisp", "chilli crisp", "crispy chili oil");
        syn("truffle oil");
        syn("white vinegar", "distilled vinegar", "distilled white vinegar", "spirit vinegar");
        syn("malt vinegar");
        syn("sherry vinegar");
        // sauces & condiments
        syn("bbq sauce", "barbecue sauce", "barbeque sauce");
        syn("ranch dressing", "ranch");
        syn("relish", "pickle relish", "sweet relish");
        syn("steak sauce", "brown sauce", "hp sauce", "a1 sauce");
        syn("salad dressing", "vinaigrette", "italian dressing", "french dressing");
        syn("hoisin sauce", "hoisin");
        syn("teriyaki sauce", "teriyaki", "teriyaki marinade");
        syn("sriracha", "sriracha sauce", "sriracha chili sauce");
        syn("sweet chili sauce", "sweet chilli sauce", "thai sweet chili sauce");
        syn("buffalo sauce", "wing sauce", "frank red hot");
        syn("marinara sauce", "marinara", "pasta sauce", "tomato pasta sauce", "spaghetti sauce", "jarred pasta sauce");
        syn("alfredo sauce", "alfredo");
        syn("gravy", "gravy granule", "brown gravy");
        syn("horseradish sauce", "creamed horseradish", "prepared horseradish");
        syn("tartar sauce", "tartare sauce");
        syn("cranberry sauce");
        syn("liquid smoke");
        // cans & jars
        syn("cream of mushroom soup", "condensed mushroom soup", "condensed cream of mushroom soup");
        syn("water chestnut");
        syn("bamboo shoot");
        syn("roasted red pepper", "roasted pepper", "jarred roasted pepper", "pimento", "pimiento", "roasted red peppers");
        syn("caper", "caper berry", "baby caper");
        syn("pickle", "gherkin", "dill pickle", "cornichon", "pickled cucumber");
        syn("artichoke heart", "marinated artichoke", "canned artichoke heart");
        syn("coconut cream", "creamed coconut");
        syn("applesauce", "apple sauce", "apple puree", "unsweetened applesauce");
        syn("sauerkraut");
        syn("pickled jalapeno", "jalapeno in jar", "jarred jalapeno", "jalapenos in jar", "jalapeno slice");
        syn("olive", "black olive", "green olive", "kalamata olive", "pitted olive");
        // frozen
        syn("mixed vegetable", "frozen mixed vegetable", "mixed veg", "frozen mixed veg", "stir fry vegetable", "frozen vegetable");
        syn("french fry", "frozen fry", "fry", "oven chip", "fries", "french fries", "frozen french fry");
        syn("hash brown", "hash browns", "frozen hash brown");
        syn("pizza", "frozen pizza");
        syn("ice cream", "vanilla ice cream");
        syn("dumpling", "frozen dumpling", "potsticker", "gyoza", "dim sum");
        syn("waffle", "frozen waffle");
        syn("tater tot", "potato gem", "potato smiles");
        syn("spring roll", "frozen spring roll");
        // snacks
        syn("tortilla chip", "nacho chip", "corn chip", "tortilla chips");
        syn("potato chip", "crisp", "potato crisp");
        syn("pretzel");
        syn("popcorn", "popcorn kernel");
        syn("granola", "muesli");
        syn("granola bar", "cereal bar");
        syn("rice cake");
        syn("cookie", "biscuit", "shortbread");
        syn("trail mix");
        syn("beef jerky", "jerky");
        syn("cereal", "breakfast cereal", "cornflake", "corn flake");
        syn("seaweed snack");
        // drinks
        syn("coffee", "espresso", "instant coffee", "brewed coffee", "strong coffee", "coffee granule");
        syn("tea", "black tea", "green tea", "tea bag", "black tea bag", "green tea bag", "earl grey tea bag", "earl grey");
        syn("cranberry juice");
        syn("apple juice");
        syn("lemonade");
        syn("sparkling water", "soda water", "club soda", "carbonated water", "seltzer");
        syn("cola", "coca cola", "coke");
        syn("white wine", "dry white wine");
        syn("red wine", "dry red wine");
        syn("beer", "lager", "ale", "stout");
        syn("coconut water");
        syn("cocoa mix", "hot chocolate", "hot cocoa mix", "hot chocolate powder");
        syn("ginger ale");
        syn("rum", "dark rum", "white rum");
        // asian pantry
        syn("kimchi", "kimchee");
        syn("mirin", "rice wine mirin");
        syn("rice wine", "shaoxing wine", "chinese cooking wine", "shaoxing rice wine", "chinese rice wine");
        syn("sake", "cooking sake");
        syn("black bean sauce");
        syn("chili garlic sauce", "chilli garlic sauce");
        syn("curry paste");
        syn("thai red curry paste", "red curry paste");
        syn("thai green curry paste", "green curry paste");
        syn("nori", "nori sheet", "seaweed sheet", "sushi nori");
        syn("wonton wrapper", "wonton skin", "dumpling wrapper", "gyoza wrapper");
        syn("rice paper", "rice paper wrapper", "spring roll wrapper", "rice paper sheet");
        syn("dashi", "dashi stock", "dashi powder", "hondashi");
        syn("furikake");
        syn("tamari", "gluten free soy sauce");
        syn("doubanjiang", "chili bean paste", "toban djan", "chilli bean paste");
        syn("sambal oelek", "sambal");
        syn("wasabi", "wasabi paste");
        syn("pickled ginger", "sushi ginger", "gari");
        syn("lemongrass paste");
        syn("kecap manis", "sweet soy sauce");
        syn("tamarind", "tamarind paste", "tamarind concentrate", "tamarind pulp");
        // latin pantry
        syn("chipotle in adobo", "chipotle pepper in adobo", "chipotle", "chipotle chili in adobo", "chipotle in adobo sauce");
        syn("enchilada sauce", "red enchilada sauce");
        syn("salsa verde", "green salsa", "tomatillo salsa");
        syn("masa harina", "masa", "corn masa flour", "maseca");
        syn("dried ancho chili", "ancho chili", "ancho chile", "ancho", "dried ancho chile");
        syn("dried guajillo chili", "guajillo chili", "guajillo", "guajillo chile");
        syn("mole paste", "mole", "mole sauce");
        syn("hominy", "canned hominy");
        syn("plantain", "green plantain", "ripe plantain");
        syn("tomatillo");
        syn("achiote paste", "annatto", "achiote", "annatto seed");
        syn("sofrito");
        syn("dulce de leche");
        syn("chimichurri");
        // indian pantry
        syn("chana dal", "split chickpea", "bengal gram");
        syn("urad dal", "black gram", "urad");
        syn("toor dal", "arhar dal", "tuvar dal");
        syn("moong dal", "split mung bean", "mung dal", "yellow moong dal");
        syn("tandoori paste", "tandoori masala", "tandoori spice");
        syn("mango chutney");
        syn("tamarind chutney", "imli chutney");
        syn("asafoetida", "hing");
        syn("amchur", "amchoor", "mango powder", "dry mango powder");
        syn("kasuri methi", "dried fenugreek leaf", "fenugreek leaf", "methi leaf");
        syn("ginger garlic paste", "garlic ginger paste");
        syn("papadum", "poppadom", "papad", "pappadam");
        syn("jaggery", "gur");
        syn("rose water", "rosewater");
        syn("nigella seed", "kalonji", "black onion seed", "nigella");
        syn("poha", "flattened rice", "beaten rice");
        // middle eastern pantry
        syn("hummus", "houmous", "humus");
        syn("harissa", "harissa paste", "rose harissa");
        syn("pomegranate molasses");
        syn("preserved lemon");
        syn("dukkah", "duqqa");
        syn("baharat");
        syn("freekeh");
        syn("labneh", "labne");
        syn("halva", "halwa", "halvah");
        syn("orange blossom water", "orange flower water");
        syn("pita chip", "pitta chip");
        syn("falafel mix");
        syn("grape leaf", "vine leaf", "stuffed grape leaf");
        syn("aleppo pepper", "aleppo chili", "pul biber");
        // italian pantry
        syn("nduja");
        syn("anchovy paste");
        syn("balsamic glaze", "balsamic reduction");
        syn("mortadella");
        syn("giardiniera");
        syn("calabrian chili", "calabrian chili paste", "calabrian chilli");
        syn("san marzano tomato", "san marzano");
        syn("breadstick", "grissini");
        syn("ladyfinger", "savoiardi", "sponge finger", "lady finger");
        syn("amaretti", "amaretti biscuit");
        syn("semolina", "semolina flour", "durum wheat semolina", "durum semolina");
        syn("pizza sauce");
        syn("parmesan rind");

        // ---- new families ----
        fam("cabbage", "red cabbage", "napa cabbage");
        fam("chili", "jalapeno", "poblano", "habanero");
        fam("mushroom", "portobello mushroom", "shiitake mushroom", "oyster mushroom", "dried porcini");
        fam("pumpkin", "acorn squash", "spaghetti squash");
        fam("basil", "dried basil", "thai basil");
        fam("thyme", "dried thyme");
        fam("rosemary", "dried rosemary");
        fam("parsley", "dried parsley");
        fam("dill", "dried dill");
        fam("mint", "dried mint");
        fam("black pepper", "white pepper");
        fam("paprika", "smoked paprika");
        fam("ginger", "ground ginger");
        fam("chili powder", "cayenne", "chipotle powder", "kashmiri chili powder");
        fam("beef", "ground beef", "steak", "brisket", "beef roast", "short rib");
        fam("pork", "ground pork", "pork chop", "pork shoulder", "pork tenderloin", "pork belly", "pork loin", "pork rib");
        fam("lamb", "ground lamb", "lamb chop", "lamb shoulder");
        fam("chicken", "chicken breast", "chicken thigh", "chicken drumstick", "chicken wing", "chicken leg",
                "ground chicken", "rotisserie chicken");
        fam("fish", "salmon", "white fish", "tuna", "trout", "mackerel", "sardine", "tuna steak", "sea bass", "swordfish");
        fam("seafood", "shrimp", "scallop", "mussel", "clam", "crab", "lobster", "squid");
        fam("sausage", "italian sausage", "smoked sausage", "bratwurst", "hot dog");
        fam("cheese", "cheddar", "parmesan", "mozzarella", "feta", "cream cheese", "ricotta", "goat cheese", "halloumi",
                "gruyere", "brie", "paneer", "monterey jack", "swiss cheese", "blue cheese", "gouda", "mascarpone",
                "american cheese", "colby jack", "pepper jack", "provolone", "manchego", "cottage cheese", "burrata",
                "queso fresco", "cotija");
        fam("rice", "arborio rice", "brown rice", "sushi rice", "wild rice", "sticky rice");
        fam("pasta", "spaghetti", "penne", "macaroni", "fusilli", "linguine", "rigatoni", "farfalle", "tagliatelle",
                "lasagne sheet", "orzo", "fettuccine", "tortellini", "ravioli");
        fam("noodle", "rice noodle", "egg noodle", "udon noodle", "soba noodle", "ramen noodle", "glass noodle", "lo mein noodle");
        fam("bread", "baguette", "sourdough", "ciabatta", "pita", "naan", "bread roll", "burger bun", "whole wheat bread",
                "bagel", "brioche", "english muffin", "hot dog bun", "flatbread");
        fam("bean", "kidney bean", "black bean", "white bean", "pinto bean", "fava bean", "mung bean");
        fam("nut", "walnut", "almond", "cashew", "peanut", "pine nut", "pecan", "hazelnut", "pistachio", "macadamia nut");
        fam("berry", "strawberry", "blueberry", "raspberry", "blackberry", "cranberry");
        fam("flour", "self raising flour", "whole wheat flour");
        fam("oil", "olive oil", "vegetable oil", "coconut oil", "avocado oil");
        fam("vinegar", "white wine vinegar", "red wine vinegar", "balsamic vinegar", "cider vinegar", "rice vinegar",
                "malt vinegar", "white vinegar", "sherry vinegar");
        fam("hot sauce", "sriracha", "buffalo sauce");
        fam("curry paste", "thai red curry paste", "thai green curry paste");
        fam("tomato", "cherry tomato", "canned tomato", "san marzano tomato");
        fam("lentil", "chana dal", "urad dal", "toor dal", "moong dal");
        syn("parmesan", "pecorino romano", "romano");
        syn("chili", "serrano pepper", "serrano chili", "serrano chile");
        syn("milk powder", "powdered milk", "dry milk", "dried milk");
        // v1.7 recipe additions
        syn("allspice", "allspice berry", "whole allspice");
        syn("sausage", "breakfast sausage", "pork sausage");
        syn("steak", "beef sirloin", "sirloin");
        syn("beef", "beef chuck roast", "beef stew meat", "chuck roast");
        syn("corned beef", "corned beef brisket");
        syn("swiss cheese", "emmental cheese", "emmentaler");
        syn("pork rib", "pork back rib", "baby back rib", "pork spare rib");
        syn("chicken breast", "chicken tender", "chicken tenderloin");
        syn("tuna steak", "sushi grade tuna", "sashimi tuna");
        syn("tahini", "tahini sauce");
        syn("rice noodle", "wide rice noodle", "flat rice noodle");
        syn("parsley", "parsley stem");
        syn("curry powder", "malaysian curry powder", "madras curry powder");
        syn("sherry", "dry sherry", "cooking sherry");
        syn("sushi rice", "medium grain rice", "calrose rice");
        syn("gluten free pasta", "gluten pasta");
        syn("gluten free flour", "gluten free all purpose flour", "gluten free plain flour", "gluten free flour blend");
        syn("gluten free oats", "gluten free rolled oats", "certified gluten free oats");
        syn("baking powder", "gluten free baking powder");
        syn("chocolate", "dairy free chocolate chip", "vegan chocolate chip", "dairy free chocolate");
        syn("ice", "ice cube");
        syn("meatball", "meatballs");
        syn("kaffir lime", "makrut lime", "lime leaf", "makrut lime leaf");
        syn("berbere", "berbere spice", "berbere spice blend");
        syn("sesame paste", "chinese sesame paste");
        syn("ham hock", "smoked ham hock");
        syn("crispy fried onion", "fried onion");
        syn("nuoc cham", "nuoc cham dipping sauce", "nuoc cham sauce");
        syn("tapioca pearl", "pearl tapioca");
        syn("roast beef slice", "roast beef");
        syn("veal", "veal escalope", "veal cutlet");
        syn("bbq rub", "barbecue dry rub", "barbecue rub", "bbq dry rub", "dry rub");
        syn("sardine", "sardine in olive oil", "canned sardine");
        syn("salmon", "sushi grade salmon");
        syn("chocolate", "mini chocolate chip");
        syn("sprinkle", "chocolate sprinkle");
        syn("rice cereal", "puffed rice cereal", "crispy rice cereal");
        syn("red bean paste", "sweet red bean paste", "sweet bean paste", "anko");
        syn("orange liqueur", "triple sec", "cointreau", "grand marnier");
        syn("marsala wine", "marsala");
        syn("aioli", "garlic aioli");
        syn("peanut sauce", "peanut satay sauce", "satay sauce");
        syn("frosting", "cream cheese frosting", "buttercream frosting");
        syn("tortilla", "empanada disc", "empanada wrapper");
        syn("cannelloni", "cannelloni tube", "manicotti");
        syn("pork chop", "pork cutlet");
        fam("beef", "beef tenderloin");
        fam("fish", "halibut");
        fam("pasta", "cannelloni");
        syn("bacon", "bacon lardon", "lardon");
        syn("steak", "beef skirt", "skirt steak", "flank steak");
        fam("beef", "beef cutlet", "beef slice", "oxtail");
        syn("lamb shoulder", "lamb neck");
        fam("veal", "veal shank");
        syn("pork shoulder", "pulled pork");
        fam("sausage", "morcilla", "blood sausage");
        syn("pork chop", "pork loin cutlet");
        syn("fish stock", "seafood stock", "anchovy stock");
        syn("bread", "day old bread", "milk bread");
        syn("chive", "garlic chive");
        syn("mayonnaise", "japanese mayonnaise", "kewpie mayonnaise");
        syn("mozzarella", "mozzarella string cheese", "string cheese", "mozzarella stick");
        syn("salsa", "salsa roja", "red salsa");
        syn("potato", "yukon gold potato", "russet potato");
        syn("macadamia nut", "candlenut");
        syn("masarepa", "precooked cornmeal", "harina pan");
        syn("tapioca flour", "tapioca starch");
        syn("shrimp", "crawfish tail", "crawfish", "crayfish");
        fam("pasta", "fideo noodle", "fideo");
        syn("yogurt", "vanilla yogurt");
        fam("sausage", "merguez sausage", "toulouse sausage");
        fam("curry paste", "korma paste", "tikka masala paste", "vindaloo paste", "massaman curry paste",
                "panang curry paste", "yellow curry paste", "laksa paste");
        fam("pasta", "orecchiette", "pappardelle", "trofie pasta", "ditalini pasta", "jumbo pasta shell");
    }

    // v1.8: patterns compiled once, an ASCII fast path and a bounded memo (normalize() is called
    // tens of thousands of times while recipes load and on every pantry keystroke).
    private static final java.util.regex.Pattern MARKS = java.util.regex.Pattern.compile("\\p{M}+");
    private static final java.util.regex.Pattern PARENS = java.util.regex.Pattern.compile("\\([^)]*\\)");
    private static final java.util.regex.Pattern NON_ALPHA = java.util.regex.Pattern.compile("[^a-z% ]");
    private static final java.util.regex.Pattern SPACES = java.util.regex.Pattern.compile("\\s+");
    private static final int MEMO_MAX = 16384;
    private static final java.util.concurrent.ConcurrentHashMap<String, String> MEMO =
            new java.util.concurrent.ConcurrentHashMap<String, String>(4096);

    /** Accent-stripped lower case (NFD + drop combining marks); ASCII input skips the Unicode work. */
    public static String fold(String raw) {
        if (raw == null) return "";
        boolean ascii = true;
        for (int i = 0; i < raw.length(); i++) if (raw.charAt(i) > 127) { ascii = false; break; }
        if (ascii) return raw.toLowerCase(Locale.US);
        return MARKS.matcher(java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.US);
    }

    /** Canonical key for an ingredient name. Never returns null (may return ""). */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String m = MEMO.get(raw);
        if (m != null) return m;
        String r = normalizeUncached(raw);
        if (MEMO.size() >= MEMO_MAX) MEMO.clear();
        MEMO.put(raw, r);
        return r;
    }

    static String normalizeUncached(String raw) {
        String s = fold(raw);
        int comma = s.indexOf(',');
        if (comma > 0) s = s.substring(0, comma);
        if (s.indexOf('(') >= 0) s = PARENS.matcher(s).replaceAll(" ");
        s = s.replace('&', ' ').replace('-', ' ').replace('/', ' ').replace('\'', ' ');
        s = SPACES.matcher(NON_ALPHA.matcher(s).replaceAll(" ")).replaceAll(" ").trim();
        if (s.isEmpty()) return "";
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(singular(w));
        }
        String sing = sb.toString();
        String hit = SYN.get(sing);
        if (hit != null) return hit;
        if (FAMILY.containsKey(sing)) return sing;

        // strip descriptor words (keep "ground" before meats)
        String[] t = sing.split(" ");
        StringBuilder kept = new StringBuilder();
        for (int k = 0; k < t.length; k++) {
            String w = t[k];
            boolean drop = DESCRIPTORS.contains(w);
            if (w.equals("ground")) drop = !(k + 1 < t.length && MEATS_AFTER_GROUND.contains(t[k + 1]));
            if (w.equals("free") && k > 0 && (t[k - 1].equals("gluten") || t[k - 1].equals("dairy"))) drop = false; // v1.7
            if (w.equals("smoked") && k + 1 < t.length && t[k + 1].equals("paprika")) drop = true;
            if (!drop) {
                if (kept.length() > 0) kept.append(' ');
                kept.append(w);
            }
        }
        String stripped = kept.length() == 0 ? sing : kept.toString();
        hit = SYN.get(stripped);
        if (hit != null) return hit;
        return stripped;
    }

    static String singular(String w) {
        if (w.length() <= 3) return w;
        String irr = IRREGULAR.get(w);
        if (irr != null) return irr;
        if (NO_SINGULAR.contains(w)) return w;
        if (w.endsWith("ies") && w.length() > 4) return w.substring(0, w.length() - 3) + "y";
        if (w.endsWith("oes")) return w.substring(0, w.length() - 2);
        if (w.endsWith("ches") || w.endsWith("shes") || w.endsWith("xes") || w.endsWith("sses") || w.endsWith("zes"))
            return w.substring(0, w.length() - 2);
        if (w.endsWith("ss") || w.endsWith("us") || w.endsWith("is")) return w;
        if (w.endsWith("s")) return w.substring(0, w.length() - 1);
        return w;
    }

    /** General family of a key (e.g. "spaghetti" -> "pasta"), or null. */
    public static String family(String key) {
        return FAMILY.get(key);
    }

    /** True if the key is a known canonical name (used by tests and autocomplete). */
    public static boolean isKnown(String key) {
        return SYN.containsKey(key) && key.equals(SYN.get(key)) || FAMILY.containsKey(key) || FAMILY.containsValue(key);
    }
}
