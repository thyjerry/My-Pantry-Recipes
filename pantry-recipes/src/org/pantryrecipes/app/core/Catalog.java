package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Common ingredients for the tap-to-add grid and autocomplete, grouped by category.
 * Inside a category, an entry starting with '#' starts a subcategory.
 */
public final class Catalog {
    private Catalog() { }

    public static final String[] CATEGORIES = {
            "Vegetables", "Fruits", "Herbs", "Spices & seasonings", "Meat", "Poultry", "Fish & seafood", "Deli & cured",
            "Eggs & dairy", "Cheese", "Plant-based", "Grains & rice", "Pasta & noodles", "Bread & tortillas",
            "Beans & legumes", "Nuts & seeds", "Baking", "Sweeteners", "Oils & vinegars", "Sauces & condiments",
            "Cans & jars", "Frozen", "Snacks", "Drinks", "Asian pantry", "Latin pantry", "Indian pantry",
            "Middle Eastern pantry", "Italian pantry"
    };

    /** Same order as CATEGORIES. */
    public static final String[][] ITEMS = {
            {"#Onions & garlic", "Onion", "Red onion", "Shallot", "Green onion", "Leek", "Garlic", "Chives",
                    "#Roots & tubers", "Potato", "Sweet potato", "Carrot", "Parsnip", "Beet", "Turnip", "Rutabaga",
                    "Radish", "Celeriac", "Ginger", "Daikon", "Jicama", "Horseradish",
                    "#Leafy greens", "Spinach", "Kale", "Lettuce", "Arugula", "Swiss chard", "Collard greens",
                    "Watercress", "Mixed salad greens", "Endive", "Radicchio",
                    "#Cabbage family", "Cabbage", "Red cabbage", "Broccoli", "Cauliflower", "Brussels sprouts",
                    "Bok choy", "Napa cabbage", "Broccolini", "Kohlrabi",
                    "#Squash", "Zucchini", "Pumpkin", "Acorn squash", "Spaghetti squash",
                    "#Peppers & chilies", "Bell pepper", "Chili", "Jalapeno", "Poblano", "Habanero",
                    "#Mushrooms", "Mushrooms", "Portobello mushrooms", "Shiitake mushrooms", "Oyster mushrooms",
                    "Dried porcini",
                    "#Pods, peas & corn", "Green beans", "Peas", "Snow peas", "Edamame", "Sweetcorn", "Okra",
                    "#More vegetables", "Celery", "Eggplant",
                    "Asparagus", "Artichoke", "Fennel", "Bean sprouts", "Broccoli rabe", "Chinese broccoli", "Delicata squash", "Pearl onions", "Coleslaw mix"},
            {"#Tomatoes, cucumber & avocado", "Tomato", "Cherry tomatoes", "Cucumber", "Avocado", // v2.0.1: botanical fruits, moved from Vegetables
                    "#Citrus", "Lemon", "Lime", "Orange", "Grapefruit", "Mandarin",
                    "#Apples & pears", "Apple", "Pear",
                    "#Berries", "Strawberries", "Blueberries", "Raspberries", "Blackberries", "Cranberries",
                    "#Stone fruit", "Peach", "Nectarine", "Plum", "Cherries", "Apricot",
                    "#Tropical", "Banana", "Mango", "Pineapple", "Papaya", "Kiwi", "Rhubarb", "Passion fruit", "Coconut",
                    "Pomegranate",
                    "#Melons & grapes", "Watermelon", "Cantaloupe", "Grapes", "Figs",
                    "#Dried fruit", "Raisins", "Dried cranberries", "Dried apricots", "Dates", "Prunes",
                    "Dried figs", "Green papaya"},
            {"#Fresh herbs", "Parsley", "Cilantro", "Basil", "Mint", "Thyme", "Rosemary", "Dill", "Sage",
                    "Tarragon", "Thai basil", "Lemongrass", "Curry leaves",
                    "#Dried herbs", "Oregano", "Dried basil", "Dried thyme", "Dried rosemary", "Dried parsley",
                    "Dried dill", "Dried mint", "Bay leaves", "Marjoram", "Italian seasoning", "Herbes de Provence"},
            {"#Salt & pepper", "Salt", "Black pepper", "White pepper", "Garlic salt", "Seasoned salt",
                    "#Ground spices", "Cumin", "Paprika", "Smoked paprika", "Celery seed", "Turmeric", "Coriander", "Cinnamon",
                    "Nutmeg", "Allspice", "Ground ginger", "Cardamom", "Cloves", "Mustard powder", "Fenugreek",
                    "Sumac", "Garlic powder", "Onion powder",
                    "#Chili & heat", "Chili powder", "Chili flakes", "Cayenne", "Chipotle powder",
                    "Kashmiri chili powder",
                    "#Whole spices", "Mustard seeds", "Fennel seeds", "Star anise",
                    "Caraway seeds", "Saffron", "Vanilla bean",
                    "#Blends", "Curry powder", "Garam masala", "Taco seasoning", "Cajun seasoning",
                    "Chinese five spice", "Za'atar", "Ras el hanout", "Pumpkin pie spice", "Everything bagel seasoning",
                    "Seafood seasoning", "Jerk seasoning", "Bouillon powder", "Shawarma spice", "Juniper berries", "Sazon seasoning", "Shichimi togarashi", "Piment d'Espelette"},
            {"#Beef", "Ground beef", "Steak", "Stewing beef", "Beef roast", "Short ribs", "Beef brisket",
                    "#Pork", "Pork chops", "Pork shoulder", "Pork tenderloin", "Pork belly", "Ground pork",
                    "Pork ribs",
                    "#Lamb & more", "Lamb chops", "Lamb shoulder", "Ground lamb", "Veal", "Goat", "Venison", "Beef bones", "Ham hock", "Meatballs", "Corned beef", "Char siu pork", "Oxtail", "Veal shanks", "Lamb bones",
                    "#Offal & animal fats", "Beef liver", "Beef heart", "Beef tongue", "Lamb liver", "Lamb kidneys", "Pork cheeks", "Bone marrow", "Beef marrow bones", "Bone broth", "Beef tallow", "Beef suet", "Lard", "Schmaltz", "Pork rinds"},
            {"#Chicken", "Chicken breast", "Chicken thighs", "Chicken drumsticks", "Chicken wings",
                    "Whole chicken", "Ground chicken", "Rotisserie chicken",
                    "Chicken livers", "Chicken hearts", "Chicken bones",
                    "#Other poultry", "Turkey breast", "Turkey thighs", "Ground turkey", "Duck"},
            {"#Fish", "Salmon", "White fish", "Tuna steak", "Trout", "Mackerel", "Sardines", "Sea bass",
                    "Swordfish",
                    "#Shellfish", "Shrimp", "Scallops", "Mussels", "Clams", "Crab", "Lobster", "Squid",
                    "#Canned & smoked", "Tuna", "Smoked salmon", "Anchovies", "Fish sticks", "Smoked haddock", "Salt cod"},
            {"Bacon", "Ham", "Sausages", "Chorizo", "Guanciale", "Italian sausage", "Salami", "Pepperoni", "Prosciutto",
                    "Pancetta", "Hot dogs", "Deli turkey", "Roast beef slices", "Smoked sausage", "Bratwurst", "Canadian bacon"},
            {"#Eggs", "Eggs",
                    "#Milk & cream", "Milk", "Buttermilk", "Heavy cream", "Half and half", "Sour cream",
                    "Creme fraiche", "Evaporated milk", "Condensed milk", "Milk powder", "Whipped cream",
                    "#Butter & yogurt", "Butter", "Ghee", "Yogurt", "Kefir", "Skyr", "Smetana"},
            {"#Everyday", "Cheddar", "Mozzarella", "Monterey Jack", "Swiss cheese", "American cheese",
                    "Colby jack", "Pepper jack", "Provolone",
                    "#Hard & aged", "Parmesan", "Gruyere", "Manchego", "Gouda",
                    "#Soft & fresh", "Feta", "Cream cheese", "Ricotta", "Goat cheese", "Mascarpone", "Cottage cheese",
                    "Brie", "Burrata", "Halloumi", "Paneer", "Queso fresco", "Kasseri", "Blue cheese", "Ricotta salata", "Quark"},
            {"Tofu", "Tempeh", "Seitan", "Vegan sausage", "Soy milk", "Almond milk", "Oat milk", "Coconut yogurt", "Vegan butter",
                    "Vegan cheese", "Nutritional yeast", "Plant-based mince", "Veggie burgers", "Textured vegetable protein",
                    "Aquafaba", "Cashew cream", "Vegan cream cheese", "Falafel"},
            {"#Rice", "Rice", "Brown rice", "Arborio rice", "Sushi rice", "Wild rice", "Sticky rice",
                    "#Grains", "Oats", "Quinoa", "Couscous", "Bulgur", "Barley", "Farro", "Polenta", "Cornmeal",
                    "Millet", "Buckwheat", "Grits", "Steel-cut oats", "Gluten-free oats", "Paella rice", "Wheat bran"},
            {"#Pasta", "Pasta", "Spaghetti", "Penne", "Macaroni", "Fusilli", "Linguine", "Fettuccine", "Rigatoni",
                    "Farfalle", "Orzo", "Spaetzle", "Pierogi", "Lasagne sheets", "Tortellini", "Gnocchi", "Egg noodles", "Ravioli",
                    "#Noodles", "Noodles", "Rice noodles", "Udon noodles", "Soba noodles", "Ramen noodles", "Vermicelli",
                    "Glass noodles", "Lo mein noodles", "Gluten-free pasta"},
            {"#Bread", "Bread", "Sourdough", "Rye bread", "Roti", "Baguette", "Ciabatta", "Bread rolls",
                    "Burger buns", "Hot dog buns", "English muffins", "Bagels", "Brioche", "Croissants",
                    "#Wraps & flatbreads", "Tortillas", "Corn tortillas", "Pita", "Naan", "Flatbread",
                    "Taco shells", "Pizza dough",
                    "#Crumbs & crackers", "Breadcrumbs", "Crackers", "Croutons", "Egg roll wrappers"},
            {"#Canned & cooked", "Chickpeas", "Black beans", "Kidney beans", "White beans", "Pinto beans",
                    "Refried beans", "Baked beans", "#Dried", "Lentils", "Split peas", "Black-eyed peas", "Mung beans", "Fava beans"},
            {"#Nuts", "Almonds", "Walnuts", "Peanuts", "Cashews", "Pecans", "Pistachios", "Hazelnuts",
                    "Pine nuts", "Macadamia nuts",
                    "#Seeds", "Sesame seeds", "Sunflower seeds", "Pumpkin seeds", "Chia seeds", "Flaxseed",
                    "Hemp seeds", "Poppy seeds",
                    "#Butters", "Peanut butter", "Almond butter", "Sunflower seed butter", "Chocolate hazelnut spread"},
            {"#Flours", "Flour", "Whole wheat flour", "Rye flour", "Self-raising flour", "Almond flour",
                    "Coconut flour", "Chickpea flour", "Rice flour", "Buckwheat flour", "Cornstarch",
                    "#Leaveners", "Baking powder", "Baking soda", "Yeast", "Cream of tartar", "Gelatin",
                    "#Chocolate & cocoa", "Chocolate", "White chocolate", "Cocoa powder",
                    "#Extras", "Vanilla extract", "Almond extract", "Shredded coconut", "Sprinkles",
                    "Marshmallows", "Pie crust", "Puff pastry", "Phyllo dough", "Suet", "Graham crackers", "Food coloring", "Cake flour", "Gluten-free flour", "Bechamel sauce", "Crepes", "Rice cereal", "Red bean paste", "Oat flour", "Xanthan gum", "Psyllium husk", "Arrowroot", "Agar", "Sponge cake", "Marshmallow fluff", "Butterscotch chips", "Toffee bits", "Tapioca pearls", "Potato starch", "Tapioca flour", "Matzo meal", "Masarepa"},
            {"Sugar", "Brown sugar", "Powdered sugar", "Honey", "Maple syrup", "Agave syrup", "Molasses",
                    "Golden syrup", "Lingonberry jam", "Corn syrup", "Coconut sugar", "Stevia", "Date syrup", "Jam", "Caramel sauce", "Chocolate sauce", "Kaya (coconut jam)", "Marmalade"},
            {"#Oils", "Olive oil", "Vegetable oil", "Sesame oil", "Coconut oil", "Palm oil", "Avocado oil", "Cooking spray",
                    "Chili oil", "Truffle oil",
                    "#Vinegars", "Balsamic vinegar", "White wine vinegar", "Red wine vinegar", "Rice vinegar",
                    "Apple cider vinegar", "Distilled vinegar", "Malt vinegar", "Sherry vinegar", "Red palm oil"},
            {"#Everyday", "Ketchup", "Mayonnaise", "Dijon mustard", "Hot sauce", "BBQ sauce",
                    "Ranch dressing", "Caesar dressing", "Vegan mayonnaise", "Relish", "Steak sauce", "Worcestershire sauce", "Salad dressing",
                    "#Savory", "Soy sauce", "Fish sauce", "Oyster sauce", "Hoisin sauce", "Teriyaki sauce",
                    "Sriracha", "Sweet chili sauce", "Peanut sauce", "Buffalo sauce", "Tahini", "Yeast extract", "Pesto", "Salsa",
                    "Marinara sauce", "Alfredo sauce", "Gravy", "Horseradish sauce", "Tartar sauce",
                    "Cranberry sauce", "Liquid smoke"},
            {"#Tomatoes", "Canned tomatoes", "Tomato paste", "Sun-dried tomatoes",
                    "#Stock & soup", "Chicken stock", "Vegetable stock", "Beef stock", "Fish stock", "Cream of mushroom soup",
                    "#Canned vegetables & fruit", "Water chestnuts", "Bamboo shoots", "Roasted red peppers",
                    "#Jars", "Olives", "Capers", "Pickles", "Pepperoncini", "Artichoke hearts", "Coconut milk", "Coconut cream",
                    "Applesauce", "Sauerkraut", "Jalapenos in jar", "Pickled beets", "Pickled red onions", "Guacamole", "Aioli", "BBQ rub", "Frosting", "Remoulade sauce"},
            {"Frozen mixed vegetables",
                    "Frozen berries", "Frozen fries", "Hash browns", "Frozen pizza",
                    "Ice cream", "Frozen spring rolls", "Frozen dumplings", "Frozen waffles", "Tater tots", "Frozen fish fillets", "Peas and carrots"},
            {"Tortilla chips", "Potato chips", "Pretzels", "Popcorn", "Granola", "Granola bars", "Rice cakes",
                    "Cookies", "Vanilla wafers", "Chocolate wafer cookies", "Meringue nests", "Maraschino cherries", "Trail mix", "Beef jerky", "Cereal", "Seaweed snacks", "Gingersnap cookies", "Candy-coated chocolates", "Crispy fried onions", "Fried shallots"},
            {"Coffee", "Tea", "Cranberry juice", "Apple juice", "Pineapple juice", "Lemonade", "Sparkling water", "Cola",
                    "White wine", "Red wine", "Beer", "Coconut water", "Cocoa mix", "Ginger ale", "Rum", "Brandy", "Grenadine", "Matcha powder", "Hibiscus flowers", "Ice", "Apple cider", "Sherry", "Elderflower cordial", "Orange liqueur", "Marsala wine"},
            {"Miso", "Yuzu juice", "Pandan leaves", "Gochujang", "Kimchi", "Mirin", "Rice wine", "Sake", "Black bean sauce", "Chili garlic sauce",
                    "Curry paste", "Thai red curry paste", "Thai green curry paste", "Laksa paste", "Nori", "Wonton wrappers",
                    "Rice paper", "Dashi", "Furikake", "Tamari", "Doubanjiang", "Sambal oelek",
                    "Wasabi", "Pickled ginger", "Lemongrass paste", "Kecap manis", "Tamarind", "Black vinegar", "Gochugaru", "Galangal", "Kaffir lime leaves", "Shrimp paste", "Fermented black beans", "Sichuan peppercorns", "Japanese curry roux", "Toasted rice powder", "Wakame", "Chinese sesame paste", "Nuoc cham", "Doenjang", "Ssamjang", "Korean rice cakes", "Chinese sausage", "Okonomiyaki sauce", "Pickled radish", "Fish cakes", "Bonito flakes"},
            {"Chipotles in adobo", "Enchilada sauce", "Salsa verde", "Masa harina", "Cotija", "Dried ancho chilies",
                    "Dried guajillo chilies", "Mole paste", "Hominy", "Plantains", "Tomatillos", "Achiote paste",
                    "Sofrito", "Dulce de leche", "Chimichurri", "Aji amarillo paste", "Adobo seasoning", "Mojo marinade", "Culantro", "Epazote", "Green seasoning", "Cassareep", "Ackee", "Callaloo", "Cassava", "Breadfruit", "Taro", "Chayote", "Guava paste", "Pigeon peas"},
            {"Poha", "Chana dal", "Urad dal", "Toor dal", "Tandoori paste", "Mango chutney", "Tamarind chutney", "Asafoetida", "Amchur", "Kasuri methi",
                    "Ginger garlic paste", "Papadums", "Jaggery", "Sambar powder", "Rose water", "Nigella seeds", "Moong dal", "Chaat masala", "Chana masala spice", "Biryani masala", "Sev", "Dosa batter", "Pav bhaji masala", "Rasam powder"},
            {"Hummus", "Harissa", "Pomegranate molasses", "Preserved lemons", "Dukkah", "Baharat",
                    "Freekeh", "Labneh", "Tzatziki", "Halva", "Orange blossom water", "Pita chips", "Falafel mix", "Grape leaves",
                    "Aleppo pepper", "Berbere", "Amba sauce", "Barberries", "Romesco sauce", "Tonkatsu sauce", "Teff flour", "Injera", "Niter kibbeh", "Suya spice", "Egusi seeds", "Fufu flour"},
            {"Nduja", "Anchovy paste", "Balsamic glaze", "Mortadella", "Giardiniera", "Calabrian chilies", "San Marzano tomatoes", "Breadsticks",
                    "Ladyfingers", "Amaretti", "Semolina", "Pizza sauce", "Parmesan rind"},
    };

    /** Accent color per category (ARGB) for chip dots. */
    public static final int[] COLORS = {
            0xFF43A047, 0xFFF9A825, 0xFF66BB6A, 0xFFE0662F, 0xFFE57373, 0xFFFFA07A, 0xFF4FC3F7, 0xFFC2185B,
            0xFF64B5F6, 0xFFFFCA28, 0xFF26A69A, 0xFFD4A15A, 0xFFF4A261, 0xFFB08968, 0xFF8D6E63, 0xFFA1887F,
            0xFFBA68C8, 0xFFFFB74D, 0xFF9CCC65, 0xFFEF5350, 0xFFEF8354, 0xFF81D4FA, 0xFFFF8A65, 0xFF7986CB,
            0xFFE53935, 0xFF2E7D32, 0xFFFF9800, 0xFF8E24AA, 0xFF388E3C
    };


    /** Extra names offered by autocomplete only (they map onto a grid item's key). */
    public static final String[] ALIASES = {
            "Butternut squash", "Cumin seeds", "Cinnamon sticks", "Peppercorns", "Tilapia", "Canned salmon",
            "Greek yogurt", "Pecorino", "Whole wheat bread", "Panko", "Butter beans", "Red lentils", "Bread flour",
            "Chocolate chips", "Dark chocolate", "Yellow mustard", "Passata", "Canned corn", "Frozen corn",
            "Canned pumpkin", "Canned peaches", "Canned pineapple", "Frozen peas", "Frozen spinach",
            "Frozen broccoli", "Frozen mango", "Frozen shrimp", "Orange juice", "Basmati rice", "Fresh mozzarella",
            "Pecorino romano", "Scallions", "Spring onions", "Aubergine", "Courgette", "Coriander leaves", "Prawns",
            "Capsicum", "Rocket", "Beetroot", "Garbanzo beans", "Cannellini beans", "Icing sugar", "Double cream",
            "Single cream", "Plain flour", "Caster sugar", "Minced beef", "Cod", "Haddock", "Swede", "Mangetout",
            "Sugar snap peas", "Cornflour", "Bicarbonate of soda", "Natural yogurt", "Grana Padano", "Sultanas",
            "Jasmine rice", "Coconut flakes", "Desiccated coconut", "Rice vermicelli", "Wraps", "Pitta bread",
            "Yam", "Serrano pepper", "Thai chili", "Romaine lettuce", "Iceberg lettuce", "Cremini mushrooms",
            "Button mushrooms", "Chestnut mushrooms", "Corn on the cob", "Silken tofu", "Firm tofu", "Mince",
            "Lardons", "Sirloin steak", "Ribeye steak", "Plum tomatoes", "Tomato puree", "Vine leaves",
            "Fish fingers", "Garam masala powder", "Cumin powder", "Turmeric powder",
    };

    public static final String[] DEFAULT_STAPLES = {"Salt", "Black pepper", "Olive oil", "Vegetable oil", "Water"};

    public static boolean isHeader(String s) { return s.startsWith("#"); }

    /** Every ingredient (headers removed), in catalog order. */
    public static List<String> allItems() {
        List<String> out = new ArrayList<String>();
        for (String[] cat : ITEMS) for (String s : cat) if (!isHeader(s)) out.add(s);
        return out;
    }

    /** Number of ingredients in a category. */
    public static int count(int category) {
        int n = 0;
        for (String s : ITEMS[category]) if (!isHeader(s)) n++;
        return n;
    }
}
