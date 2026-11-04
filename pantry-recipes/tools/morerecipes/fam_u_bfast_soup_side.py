from lib import R
F = "62-breakfast-more.txt"
S = "63-soups-more.txt"
SD = "64-sides-more.txt"
B = "Breakfast, Brunch"
# ---- overnight oats: title, flavour ings, words
OO = [
 ("Peanut Butter Banana Overnight Oats", "2 tbsp|peanut butter; 1|banana, sliced", "peanut butter and banana"),
 ("Apple Pie Overnight Oats", "1|apple, grated; 1 tsp|cinnamon; 2 tbsp|walnuts", "apple, cinnamon and walnuts"),
 ("Chocolate Cherry Overnight Oats", "1 tbsp|cocoa powder; 100 g|cherries, pitted", "cocoa and cherries"),
 ("Mango Coconut Overnight Oats", "1|mango, diced; 2 tbsp|shredded coconut", "mango and coconut"),
 ("Carrot Cake Overnight Oats", "1|carrot, finely grated; 1 tbsp|raisins; 0.5 tsp|cinnamon", "carrot, raisins and cinnamon"),
 ("Blueberry Lemon Overnight Oats", "100 g|blueberries; 1|lemon, zested", "blueberries and lemon zest"),
 ("Pumpkin Spice Overnight Oats", "4 tbsp|pumpkin puree; 0.5 tsp|pumpkin pie spice", "pumpkin and spice"),
 ("Matcha Overnight Oats", "1 tsp|matcha powder; 1|kiwi, sliced", "matcha (whisked into the milk) and kiwi"),
]
for t, ings, w in OO:
    R(F, t, "Breakfast", "International", 5, 0, 2, "Breakfast", "100 g|rolled oats; 250 ml|milk, or plant milk; 2 tbsp|yogurt; 1 tbsp|chia seeds; 1 tbsp|maple syrup; " + ings,
      ["Stir the oats, milk, yogurt, chia and maple syrup together in two jars", "Mix in the " + w + ", keeping a little fruit for the top",
       "Cover and refrigerate overnight, or for at least 4 hours", "Stir, loosen with a splash of milk and eat cold or warmed"])
# ---- smoothies: title, ings
SM = [
 ("Green Spinach Banana Smoothie", "100 g|spinach; 1|banana, frozen; 1 tbsp|peanut butter; 300 ml|almond milk"),
 ("Strawberry Banana Smoothie", "200 g|strawberries; 1|banana, frozen; 150 g|yogurt; 200 ml|milk"),
 ("Mango Lassi Smoothie", "1|mango; 250 g|yogurt; 150 ml|milk; 1 pinch|cardamom; 1 tbsp|honey"),
 ("Berry Oat Smoothie", "200 g|mixed berries, frozen; 30 g|rolled oats; 150 g|yogurt; 250 ml|milk"),
 ("Tropical Pineapple Smoothie", "200 g|pineapple; 1|banana; 200 ml|coconut milk; 1|lime"),
 ("Chocolate Peanut Butter Protein Smoothie", "1|banana, frozen; 2 tbsp|peanut butter; 1 tbsp|cocoa powder; 300 ml|milk; 1 tbsp|honey"),
 ("Peach Ginger Smoothie", "2|peaches; 1 tsp|ginger, grated; 150 g|yogurt; 200 ml|orange juice"),
 ("Avocado Kale Smoothie", "1|avocado; 60 g|kale; 1|apple; 1|lemon; 300 ml|water"),
]
for t, ings in SM:
    R(F, t, "Drink", "International", 5, 0, 2, "Breakfast, Drink, Snack", ings + "; 1 handful|ice cubes",
      ["Put everything in a blender, liquids first", "Blend on high for 45 to 60 seconds until completely smooth", "Taste, adjust the sweetness or thickness with more liquid, and serve straight away"])
# ---- egg dishes and others written out
EG = [
 ("Spinach Feta Frittata", "8|eggs; 150 g|spinach; 100 g|feta; 1|onion, chopped; 2 tbsp|olive oil; |salt; |black pepper", "Heat the oven grill. Cook the onion in the oil in an ovenproof pan for 5 minutes, add the spinach until wilted / Beat the eggs with salt and pepper, pour in and scatter with feta / Cook over medium-low heat for 6 minutes until the edges set / Grill for 3 to 4 minutes until puffed and set"),
 ("Denver Omelette", "3|eggs; 50 g|ham, diced; 0.5|green bell pepper, diced; 0.25|onion, diced; 40 g|cheddar, grated; 1 tbsp|butter; |salt", "Cook the ham, pepper and onion in the butter for 4 minutes and set aside / Beat the eggs with salt and pour into the pan, stirring gently until just set underneath / Add the filling and cheese to one half, fold and slide onto a plate"),
 ("Huevos Rancheros", "4|eggs; 4|corn tortillas; 1 can|refried beans; 250 ml|salsa, or ranchero sauce; 1|avocado; 60 g|cotija cheese; 2 tbsp|vegetable oil", "Warm the beans and sauce separately / Fry the tortillas briefly in oil until pliable, then fry the eggs / Spread each tortilla with beans, top with an egg and sauce / Finish with avocado and cheese"),
 ("Eggs Benedict", "7|eggs, 4 to poach and 3 yolks for the sauce; 2|english muffins, split and toasted; 4 slices|canadian bacon; 120 g|butter, melted; 1|lemon; 1 tbsp|white vinegar; |salt", "Whisk the yolks with 1 tbsp lemon juice over barely simmering water until thick, then slowly whisk in the melted butter for hollandaise; season / Warm the bacon / Poach the eggs in simmering water with the vinegar for 3 minutes / Stack muffin, bacon and egg and spoon over the hollandaise"),
 ("Breakfast Burritos", "6|eggs; 250 g|breakfast sausage; 1|potato, diced; 80 g|cheddar, grated; 4|flour tortillas; 150 g|salsa; 1 tbsp|vegetable oil; |salt", "Fry the potato in the oil until crisp, then add the sausage and cook through / Scramble in the eggs / Divide over the tortillas with cheese and salsa / Roll up tightly and toast seam-side down in a dry pan"),
 ("Sweet Potato Breakfast Hash", "2|sweet potatoes, diced; 1|onion, chopped; 1|red bell pepper, diced; 1 tsp|smoked paprika; 4|eggs; 2 tbsp|olive oil; 1 handful|cilantro; |salt", "Fry the sweet potato in the oil, covered, for 10 minutes / Add the onion, pepper and paprika and cook uncovered for 8 minutes until browned / Make four wells, crack in the eggs, cover and cook 5 minutes / Season and scatter with cilantro"),
 ("Tofu Scramble", "400 g|firm tofu, crumbled; 1 tbsp|olive oil; 0.5|onion, chopped; 1|red bell pepper, diced; 0.5 tsp|turmeric; 2 tbsp|nutritional yeast; 60 g|spinach; |salt; |black pepper", "Cook the onion and pepper in the oil for 4 minutes / Add the tofu, turmeric and nutritional yeast and cook for 5 minutes, stirring / Fold in the spinach until wilted / Season well; a pinch of kala namak gives an eggy flavour"),
 ("Avocado Toast with Everything Seasoning", "2 slices|sourdough bread, toasted; 1|avocado; 1|lemon; 1 tsp|everything bagel seasoning; 0.25 tsp|chili flakes; |salt", "Mash the avocado with a squeeze of lemon and salt / Spread on the warm toast / Sprinkle with the seasoning and chili flakes"),
 ("Chia Seed Pudding", "4 tbsp|chia seeds; 250 ml|coconut milk; 1 tbsp|maple syrup; 0.5 tsp|vanilla extract; 100 g|mixed berries", "Whisk the chia, coconut milk, maple syrup and vanilla / Leave 10 minutes and whisk again to break up clumps / Chill for at least 2 hours / Top with berries"),
 ("Granola with Maple and Pecans", "300 g|rolled oats; 100 g|pecans, chopped; 50 g|pumpkin seeds; 80 ml|maple syrup; 60 ml|coconut oil, melted; 1 tsp|cinnamon; 0.5 tsp|salt; 80 g|dried cranberries", "Heat the oven to 160 °C / Mix everything except the cranberries and spread on a lined tray / Bake for 25 to 30 minutes, stirring once, until golden / Cool completely on the tray (it crisps as it cools) and stir in the cranberries"),
 ("Savory Oatmeal with Egg", "80 g|rolled oats; 300 ml|vegetable stock; 1|egg; 1 tsp|soy sauce; 1|green onion; 1 tsp|chili crisp, opt", "Simmer the oats in the stock for 5 minutes until creamy / Fry or soft-boil the egg / Top the oats with the egg, soy sauce, green onion and chili crisp"),
 ("Banana Oat Pancakes", "2|bananas, ripe; 2|eggs; 100 g|rolled oats; 1 tsp|baking powder; 0.5 tsp|cinnamon; 1 tbsp|butter", "Blend the bananas, eggs, oats, baking powder and cinnamon until smooth and rest for 5 minutes / Cook small pancakes in a buttered pan over medium-low heat for 2 minutes per side / Serve with yogurt and fruit; these are gluten-free if you use certified gluten-free oats"),
 ("Cottage Cheese Pancakes (Syrniki)", "400 g|cottage cheese, or farmer cheese, drained; 1|egg; 3 tbsp|flour; 2 tbsp|sugar; 1 tsp|vanilla extract; 2 tbsp|vegetable oil; 4 tbsp|sour cream", "Mix the cheese, egg, flour, sugar and vanilla / Shape into 10 small patties and dust with flour / Fry in the oil over medium heat for 3 minutes per side until golden / Serve with sour cream and jam"),
 ("Japanese Tamagoyaki", "4|eggs; 1 tbsp|sugar; 1 tsp|soy sauce; 1 tbsp|mirin; 1 tbsp|vegetable oil", "Beat the eggs with the sugar, soy and mirin / Oil a small pan and pour in a thin layer; when nearly set roll it to one end / Add another layer, lifting the roll so egg flows under, and roll again; repeat / Rest the roll for 2 minutes and slice"),
 ("Mexican Chilaquiles Rojos", "200 g|tortilla chips; 500 ml|red enchilada sauce; 4|eggs; 60 g|queso fresco; 4 tbsp|sour cream; 0.5|onion, thinly sliced; 1 handful|cilantro", "Simmer the sauce in a wide pan / Add the chips and toss for 1 minute so they soften slightly / Top with fried eggs, cheese, sour cream, onion and cilantro / Serve at once"),
 ("Full English Breakfast", "4|pork sausages; 4 slices|bacon; 2|tomatoes, halved; 200 g|mushrooms; 1 can|baked beans; 4|eggs; 4 slices|bread; 1 tbsp|vegetable oil", "Fry the sausages for 12 to 15 minutes, adding the bacon for the last 6 / Fry the tomatoes and mushrooms in the fat / Warm the beans and fry the eggs / Toast or fry the bread and serve everything together"),
]
for i, (t, ings, steps) in enumerate(EG):
    cu = {"Huevos Rancheros": "Mexican", "Mexican Chilaquiles Rojos": "Mexican", "Full English Breakfast": "British", "Japanese Tamagoyaki": "Japanese", "Cottage Cheese Pancakes (Syrniki)": "Russian"}.get(t, "American")
    R(F, t, "Breakfast", cu, 10, 15, 2 if "Toast" in t or "Omelette" in t else 4, B, ings, steps)

# ---- soups: title, cuisine, base veg, aromatics, liquid, main, simmer, finish, blend?
SO = [
 ("Roasted Red Pepper Tomato Soup", "Mediterranean", "4|red bell peppers, roasted; 1 can|canned tomatoes", "1|onion; 2 cloves|garlic; 1 tsp|smoked paprika", "750 ml|vegetable stock", "", 15, "2 tbsp|olive oil", True),
 ("Cream of Mushroom Soup", "French", "500 g|mushrooms, sliced", "1|onion; 2 cloves|garlic; 1 tsp|dried thyme; 2 tbsp|flour", "750 ml|chicken stock", "150 ml|heavy cream", 15, "chives", True),
 ("Broccoli Cheddar Soup", "American", "500 g|broccoli, chopped; 1|carrot, grated", "1|onion; 3 tbsp|butter; 3 tbsp|flour", "750 ml|vegetable stock; 250 ml|milk", "200 g|cheddar, grated", 15, "black pepper", True),
 ("Italian Wedding Soup", "Italian", "2|carrots, diced; 150 g|spinach", "1|onion; 2 cloves|garlic", "1.5 l|chicken stock", "400 g|meatballs, small; 100 g|acini di pepe pasta", 15, "parmesan", False),
 ("Avgolemono (Greek Lemon Chicken Soup)", "Greek", "100 g|long-grain rice", "1|onion; 1 stalk|celery", "1.5 l|chicken stock", "300 g|chicken, cooked and shredded; 3|eggs; 2|lemons", 20, "dill", False),
 ("Mulligatawny Soup", "Anglo-Indian", "1|apple, chopped; 1|carrot, chopped; 100 g|red lentils", "1|onion; 2 tsp|curry powder; 1 tbsp|ginger", "1.25 l|chicken stock", "200 ml|coconut milk; 200 g|chicken, cooked and diced", 25, "cilantro and lemon", True),
 ("Hot and Sour Soup", "Chinese", "150 g|shiitake mushrooms, sliced; 150 g|bamboo shoots, sliced", "1 tbsp|ginger; 2 cloves|garlic", "1.25 l|chicken stock", "200 g|firm tofu, in strips; 3 tbsp|rice vinegar; 2 tbsp|soy sauce; 1 tsp|white pepper; 2 tbsp|cornstarch; 1|egg, beaten", 8, "green onions and sesame oil", False),
 ("Miso Soup with Tofu and Wakame", "Japanese", "5 g|dried wakame; 200 g|silken tofu, cubed", "2|green onions", "1 l|dashi", "3 tbsp|white miso", 3, "green onions", False),
 ("Tom Yum Soup", "Thai", "200 g|mushrooms, halved; 2|tomatoes, in wedges", "2 stalks|lemongrass, bruised; 4|kaffir lime leaves; 3 slices|galangal; 3|thai chilies", "1 l|chicken stock", "300 g|shrimp, peeled; 3 tbsp|fish sauce; 2|limes", 10, "cilantro", False),
 ("Gazpacho", "Spanish", "1 kg|tomatoes, ripe; 1|cucumber; 1|red bell pepper", "1 clove|garlic; 0.5|red onion", "100 ml|water", "4 tbsp|olive oil; 2 tbsp|sherry vinegar; 1 slice|bread, stale", 0, "diced cucumber and olive oil", True),
 ("Vichyssoise", "French", "4|leeks, white parts, sliced; 500 g|potatoes, diced", "3 tbsp|butter", "1 l|chicken stock", "200 ml|heavy cream", 25, "chives (serve chilled)", True),
 ("Sopa de Lima", "Mexican", "2|tomatoes, chopped; 1|green bell pepper, chopped", "1|onion; 3 cloves|garlic; 1 tsp|dried oregano", "1.5 l|chicken stock", "400 g|chicken, cooked and shredded; 3|limes", 15, "tortilla strips and cilantro", False),
 ("Harira", "Moroccan", "1 can|chickpeas, drained; 100 g|green lentils; 1 can|canned tomatoes", "1|onion; 2 stalks|celery; 1 tsp|ginger, ground; 1 tsp|turmeric; 1 tsp|cinnamon", "1.5 l|vegetable stock", "50 g|vermicelli; 1 handful|cilantro; 1|lemon", 35, "lemon wedges", False),
 ("Cabbage Roll Soup", "Eastern European", "400 g|cabbage, chopped; 1 can|canned tomatoes", "1|onion; 2 cloves|garlic; 1 tsp|paprika", "1.25 l|beef stock", "400 g|ground beef, browned; 80 g|long-grain rice", 25, "sour cream and dill", False),
 ("Sweet Potato Peanut Soup", "West African", "2|sweet potatoes, diced; 1 can|canned tomatoes", "1|onion; 1 tbsp|ginger; 2 cloves|garlic; 1 tsp|cumin", "1.25 l|vegetable stock", "4 tbsp|peanut butter; 100 g|spinach", 20, "chopped peanuts and cilantro", False),
 ("Leek and White Bean Soup", "French", "3|leeks, sliced; 2 can|cannellini beans, drained", "2 cloves|garlic; 1 tsp|dried thyme; 2 tbsp|olive oil", "1 l|vegetable stock", "1|lemon", 20, "olive oil and black pepper", True),
 ("Spicy Sausage and Kale Soup", "Italian", "150 g|kale, chopped; 2|potatoes, diced; 1 can|cannellini beans, drained", "1|onion; 3 cloves|garlic; 0.5 tsp|red pepper flakes", "1.5 l|chicken stock", "400 g|Italian sausage, browned", 20, "parmesan", False),
 ("Chilled Cucumber Yogurt Soup", "Turkish", "3|cucumbers, chopped", "1 clove|garlic; 1 handful|mint; 1 handful|dill", "250 ml|cold water", "500 g|yogurt; 2 tbsp|olive oil", 0, "walnuts and mint (serve cold)", True),
 ("Ribollita", "Italian", "2|carrots, diced; 2 stalks|celery, diced; 200 g|cavolo nero, shredded; 1 can|cannellini beans, drained; 1 can|canned tomatoes", "1|onion; 3 cloves|garlic; 4 tbsp|olive oil", "1.25 l|vegetable stock", "200 g|bread, stale, torn", 40, "a generous drizzle of olive oil", False),
 ("Wonton Soup", "Chinese", "200 g|bok choy, halved", "1 tbsp|ginger; 2|green onions", "1.5 l|chicken stock", "24|wontons, frozen or homemade; 2 tbsp|soy sauce; 1 tsp|sesame oil", 6, "green onions and white pepper", False),
]
for t, cu, veg, aro, liq, main, mins, fin, blend in SO:
    steps = []
    if mins == 0:
        steps = ["Roughly chop the vegetables and put them in a blender with the aromatics, liquid and remaining ingredients",
                 "Blend until very smooth, season well and chill for at least 2 hours", "Serve cold topped with " + fin]
    else:
        steps = ["Heat 2 tbsp oil (or the butter) in a large pot and cook the aromatics, chopped, over medium heat for 5 minutes until soft (stir in any flour or spices for the last minute)",
                 "Add the vegetables and the liquid, bring to the boil, then simmer for about %d minutes until everything is tender" % mins]
        if blend: steps.append("Blend until smooth, then stir in " + (main if main else "a little more liquid if it is too thick") + " and heat gently without boiling")
        else: steps.append("Add the remaining ingredients (whisk any egg or egg-lemon mixture with a ladle of hot broth first, and stir in cornstarch mixed with a little water) and simmer until cooked through")
        steps.append("Season with salt and pepper and serve with " + fin)
    m = [x for x in main.split("; ") if x] if mins else []
    ings = "2 tbsp|olive oil; " + aro + "; " + veg + "; " + liq + ("; " + main if main else "") + "; |salt; |black pepper"
    R(S, t, "Soup", cu, 15, mins + 10 if mins else 0, 4, "Lunch, Dinner, Soup", ings, steps)

# ---- roasted / cooked vegetable sides: title, cuisine, veg, flavour, method, minutes
SI = [
 ("Honey Roasted Carrots with Thyme", "American", "700 g|carrots, halved", "2 tbsp|honey; 1 tsp|dried thyme", "roast", 30),
 ("Garlic Parmesan Roasted Broccoli", "Italian", "1|broccoli, in florets", "3 cloves|garlic, sliced; 30 g|parmesan; 1|lemon", "roast", 20),
 ("Maple Roasted Butternut Squash", "American", "1|butternut squash, cubed", "2 tbsp|maple syrup; 0.5 tsp|cinnamon", "roast", 35),
 ("Crispy Smashed Potatoes", "American", "800 g|baby potatoes, boiled until tender", "3 cloves|garlic; 1 tbsp|rosemary; 2 tbsp|parmesan", "smash", 30),
 ("Roasted Cauliflower with Tahini", "Middle Eastern", "1|cauliflower, in florets", "1 tsp|cumin; 3 tbsp|tahini; 1|lemon; 1 handful|parsley", "roast", 30),
 ("Balsamic Roasted Mushrooms", "Italian", "600 g|mushrooms, halved", "2 tbsp|balsamic vinegar; 2 cloves|garlic; 1 tsp|dried thyme", "roast", 20),
 ("Lemon Garlic Green Beans", "American", "500 g|green beans, trimmed", "3 cloves|garlic; 1|lemon; 2 tbsp|almonds, sliced", "saute", 8),
 ("Sauteed Spinach with Garlic", "Italian", "500 g|spinach", "4 cloves|garlic, sliced; 0.25 tsp|red pepper flakes; 1|lemon", "saute", 4),
 ("Charred Corn with Lime Butter", "Mexican", "4 ears|corn on the cob", "2 tbsp|butter; 1|lime; 0.5 tsp|chili powder; 30 g|cotija cheese", "grill", 12),
 ("Roasted Asparagus with Lemon", "French", "500 g|asparagus, trimmed", "1|lemon; 30 g|parmesan", "roast", 12),
 ("Sesame Bok Choy", "Chinese", "500 g|bok choy, halved", "2 cloves|garlic; 1 tbsp|soy sauce; 1 tsp|sesame oil; 1 tsp|sesame seeds", "saute", 5),
 ("Roasted Root Vegetables", "British", "2|parsnips, chunked; 2|carrots, chunked; 1|beet, chunked; 1|red onion, wedged", "2 sprigs|rosemary; 1 tbsp|honey", "roast", 40),
 ("Mexican Street-Style Zucchini", "Mexican", "4|zucchini, halved lengthways", "2 tbsp|mayonnaise; 30 g|cotija cheese; 1|lime; 0.5 tsp|chili powder", "grill", 8),
 ("Spiced Roasted Chickpeas and Carrots", "North African", "500 g|carrots, sliced; 1 can|chickpeas, drained", "1 tsp|cumin; 1 tsp|coriander; 1 tbsp|honey", "roast", 30),
 ("Braised Red Cabbage", "German", "1|red cabbage, shredded", "1|apple, grated; 3 tbsp|red wine vinegar; 2 tbsp|brown sugar; 0.25 tsp|ground cloves", "braise", 45),
 ("Creamed Spinach", "American", "600 g|spinach", "2 tbsp|butter; 1|shallot; 120 ml|heavy cream; 0.25 tsp|nutmeg; 30 g|parmesan", "saute", 10),
 ("Roasted Delicata Squash", "American", "2|delicata squash, sliced in rings", "1 tsp|smoked paprika; 1 tbsp|maple syrup", "roast", 25),
 ("Glazed Brussels Sprouts with Bacon", "American", "600 g|Brussels sprouts, halved", "4 slices|bacon, chopped; 1 tbsp|maple syrup; 1 tbsp|balsamic vinegar", "roast", 25),
 ("Okra and Tomatoes", "Southern US", "450 g|okra, sliced; 1 can|canned tomatoes", "1|onion, chopped; 2 cloves|garlic; 0.5 tsp|cajun seasoning", "braise", 20),
 ("Roasted Radishes with Herbs", "French", "500 g|radishes, halved", "1 tbsp|butter; 1 tbsp|chives; 1 tbsp|dill", "roast", 20),
 ("Elote-Style Corn Salad Side", "Mexican", "500 g|sweetcorn, charred", "2 tbsp|mayonnaise; 1|lime; 40 g|cotija cheese; 1 handful|cilantro; 0.5 tsp|chili powder", "saute", 8),
 ("Herbed Couscous", "North African", "250 g|couscous", "1 handful|parsley; 1 handful|mint; 1|lemon; 30 g|almonds, toasted", "steep", 5),
 ("Lemon Herb Rice Pilaf", "Mediterranean", "300 g|long-grain rice", "1|onion, finely chopped; 2 tbsp|butter; 600 ml|chicken stock; 1|lemon; 1 handful|parsley", "pilaf", 20),
 ("Garlic Butter Mushrooms", "French", "500 g|mushrooms, sliced", "3 tbsp|butter; 3 cloves|garlic; 1 handful|parsley", "saute", 10),
 ("Turmeric Roasted Cauliflower", "Indian", "1|cauliflower, in florets", "1 tsp|turmeric; 1 tsp|cumin seeds; 1|lemon; 1 handful|cilantro", "roast", 25),
]
for t, cu, veg, fl, how, mins in SI:
    if how == "roast": st = ["Heat the oven to 220 °C (425 °F)", "Toss the vegetables with the oil, salt, pepper and any dry spices or herbs and spread in a single layer on a large tray", "Roast for about %d minutes, turning once, until tender and browned at the edges" % mins, "Toss with the remaining flavourings and serve"]
    elif how == "smash": st = ["Heat the oven to 230 °C (450 °F)", "Lay the boiled potatoes on an oiled tray and press each one flat with the bottom of a glass", "Brush with oil, scatter with garlic, rosemary and salt and roast for %d minutes until very crisp" % mins, "Sprinkle with parmesan and serve"]
    elif how == "grill": st = ["Heat a grill or griddle pan to high", "Brush the vegetables with a little oil and season", "Grill for about %d minutes, turning, until charred in places" % mins, "Finish with the remaining flavourings"]
    elif how == "braise": st = ["Heat the oil in a wide pan and cook any onion or garlic for 4 minutes", "Add the vegetables and the remaining flavourings with 4 tbsp water, cover and simmer gently for about %d minutes, stirring now and then, until tender" % mins, "Uncover to reduce any liquid and season"]
    elif how == "steep": st = ["Pour 300 ml boiling salted water over the couscous with 1 tbsp oil, cover and leave for 5 minutes", "Fluff with a fork", "Stir in the herbs, lemon juice and zest, and almonds, and season"]
    elif how == "pilaf": st = ["Melt the butter and cook the onion for 4 minutes", "Stir in the rice for 1 minute, add the stock and salt, bring to the boil, cover and cook on low for 18 minutes", "Rest 5 minutes, then fluff with lemon zest, juice and parsley"]
    else: st = ["Heat the oil (or butter) in a large frying pan over medium-high heat", "Add the vegetables with any garlic or shallot and cook, tossing, for about %d minutes until just tender" % mins, "Stir in the remaining flavourings, season and serve"]
    R(SD, t, "Side", cu, 10, mins + 5, 4, "Lunch, Dinner, Side dish", veg + "; " + fl + "; 2 tbsp|olive oil; |salt; |black pepper", st)
