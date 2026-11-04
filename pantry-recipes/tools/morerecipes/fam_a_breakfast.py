from lib import R
F = "30-breakfast-brunch.txt"

# ---------------- muffins ----------------
MUF = [
 ("Banana Walnut Muffins", "American", "3|ripe bananas, mashed; 250 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 1 tsp|cinnamon; 1 pinch|salt; 100 g|brown sugar; 2|eggs; 120 ml|milk; 80 ml|vegetable oil; 80 g|walnuts, chopped",
  "the flour, baking powder, baking soda, cinnamon and salt", "the mashed bananas, brown sugar, eggs, milk and oil", "most of the walnuts", "Scatter over the last walnuts, then ", 20, "Breakfast, Brunch, Snack"),
 ("Lemon Poppy Seed Muffins", "American", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 1 pinch|salt; 150 g|sugar; 2 tbsp|poppy seeds; 2|lemons, zest and juice; 2|eggs; 200 g|yogurt; 80 ml|milk; 90 g|butter, melted",
  "the flour, baking powder, baking soda, salt, sugar, poppy seeds and lemon zest", "the eggs, yogurt, milk, melted butter and half the lemon juice", "nothing more", "", 20, "Breakfast, Brunch, Snack"),
 ("Raspberry White Chocolate Muffins", "British", "250 g|flour; 2 tsp|baking powder; 1 pinch|salt; 130 g|sugar; 2|eggs; 240 ml|milk; 90 g|butter, melted; 1 tsp|vanilla extract; 150 g|raspberries; 80 g|white chocolate, chopped",
  "the flour, baking powder, salt and sugar", "the eggs, milk, melted butter and vanilla", "the raspberries and white chocolate, gently so the berries stay whole", "", 22, "Breakfast, Brunch, Snack"),
 ("Apple Cinnamon Muffins", "American", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 2 tsp|cinnamon; 1 pinch|salt; 120 g|brown sugar; 2|eggs; 200 ml|milk; 80 ml|vegetable oil; 2|apples, peeled and diced small; 1 tbsp|sugar, for the tops",
  "the flour, baking powder, baking soda, cinnamon, salt and brown sugar", "the eggs, milk and oil", "the diced apple", "Sprinkle the tops with a little sugar, then ", 22, "Breakfast, Brunch, Snack"),
 ("Carrot Cake Muffins", "British", "220 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 2 tsp|cinnamon; 0.5 tsp|nutmeg; 1 pinch|salt; 120 g|brown sugar; 2|eggs; 100 ml|vegetable oil; 120 ml|milk; 2|carrots, finely grated; 60 g|raisins; 40 g|walnuts, chopped | opt",
  "the flour, baking powder, baking soda, spices, salt and brown sugar", "the eggs, oil and milk", "the grated carrot, raisins and walnuts", "", 22, "Breakfast, Brunch, Snack"),
 ("Double Chocolate Muffins", "American", "220 g|flour; 40 g|cocoa powder; 2 tsp|baking powder; 0.5 tsp|baking soda; 1 pinch|salt; 150 g|sugar; 2|eggs; 240 ml|milk; 100 ml|vegetable oil; 1 tsp|vanilla extract; 100 g|chocolate chips",
  "the flour, cocoa, baking powder, baking soda, salt and sugar", "the eggs, milk, oil and vanilla", "most of the chocolate chips", "Press the last chocolate chips on top, then ", 20, "Brunch, Snack, Dessert"),
 ("Cranberry Orange Muffins", "American", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 1 pinch|salt; 140 g|sugar; 1|orange, zest and juice; 2|eggs; 150 ml|milk; 90 g|butter, melted; 120 g|cranberries",
  "the flour, baking powder, baking soda, salt, sugar and orange zest", "the eggs, orange juice, milk and melted butter", "the cranberries", "", 22, "Breakfast, Brunch, Snack"),
 ("Peach Yogurt Muffins", "American", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 1 pinch|salt; 130 g|sugar; 2|eggs; 250 g|yogurt; 80 ml|vegetable oil; 1 tsp|vanilla extract; 2|peaches, diced",
  "the flour, baking powder, baking soda, salt and sugar", "the eggs, yogurt, oil and vanilla", "the diced peaches", "", 22, "Breakfast, Brunch, Snack"),
 ("Oatmeal Raisin Muffins", "American", "100 g|oats; 240 ml|milk; 160 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 1 tsp|cinnamon; 1 pinch|salt; 100 g|brown sugar; 1|egg; 80 ml|vegetable oil; 80 g|raisins",
  "the flour, baking powder, baking soda, cinnamon, salt and brown sugar", "the egg and oil into the oats, which you have soaked in the milk for 10 minutes", "the raisins", "", 20, "Breakfast, Brunch, Snack"),
 ("Pumpkin Spice Muffins", "American", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 2 tsp|pumpkin pie spice; 1 pinch|salt; 150 g|brown sugar; 2|eggs; 300 g|canned pumpkin; 100 ml|vegetable oil; 60 ml|milk; 2 tbsp|pumpkin seeds | opt",
  "the flour, baking powder, baking soda, pumpkin pie spice, salt and brown sugar", "the eggs, pumpkin puree, oil and milk", "nothing more", "Scatter pumpkin seeds on top, then ", 24, "Breakfast, Brunch, Snack"),
 ("Strawberry Vanilla Muffins", "American", "250 g|flour; 2 tsp|baking powder; 1 pinch|salt; 140 g|sugar; 2|eggs; 200 ml|milk; 90 g|butter, melted; 2 tsp|vanilla extract; 200 g|strawberries, diced",
  "the flour, baking powder, salt and sugar", "the eggs, milk, melted butter and vanilla", "the diced strawberries", "", 22, "Breakfast, Brunch, Snack"),
 ("Cheddar and Chive Muffins", "British", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|mustard powder; 0.5 tsp|salt; 2|eggs; 240 ml|milk; 80 ml|vegetable oil; 150 g|cheddar, grated; 3 tbsp|chives, snipped",
  "the flour, baking powder, mustard powder and salt", "the eggs, milk and oil", "most of the cheese and the chives", "Top with the last of the cheese, then ", 20, "Breakfast, Brunch, Lunch, Snack"),
 ("Ham and Cheese Breakfast Muffins", "American", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|salt; 2|eggs; 240 ml|milk; 80 ml|vegetable oil; 120 g|ham, diced; 100 g|cheddar, grated; 2|green onions, sliced",
  "the flour, baking powder and salt", "the eggs, milk and oil", "the ham, cheese and green onions", "", 20, "Breakfast, Brunch, Lunch, Snack"),
 ("Spinach and Feta Muffins", "Greek", "250 g|flour; 2 tsp|baking powder; 0.5 tsp|salt; 1 tsp|dried oregano; 2|eggs; 200 ml|milk; 80 ml|olive oil; 100 g|spinach, chopped; 120 g|feta, crumbled",
  "the flour, baking powder, salt and oregano", "the eggs, milk and olive oil", "the spinach and feta", "", 22, "Breakfast, Brunch, Lunch, Snack"),
 ("Vegan Blueberry Oat Muffins", "American", "240 ml|almond milk; 1 tbsp|apple cider vinegar; 200 g|flour; 60 g|oats; 2 tsp|baking powder; 0.5 tsp|baking soda; 1 pinch|salt; 120 g|sugar; 80 ml|vegetable oil; 1 tsp|vanilla extract; 150 g|blueberries",
  "the flour, oats, baking powder, baking soda, salt and sugar", "the almond milk (left with the vinegar for 5 minutes to curdle), oil and vanilla", "the blueberries", "", 22, "Breakfast, Brunch, Snack"),
 ("Almond Flour Banana Muffins", "American", "2|ripe bananas, mashed; 3|eggs; 60 ml|maple syrup; 60 ml|coconut oil, melted; 1 tsp|vanilla extract; 250 g|almond flour; 1 tsp|baking soda; 1 tsp|cinnamon; 1 pinch|salt; 60 g|chocolate chips | opt",
  "the almond flour, baking soda, cinnamon and salt", "the bananas, eggs, maple syrup, coconut oil and vanilla", "the chocolate chips, if using", "", 20, "Breakfast, Brunch, Snack"),
]
for t, cu, ings, dry, wet, mix, top, mins, meals in MUF:
    fold = "Pour the wet ingredients into the dry and fold with a spatula until just combined; a few lumps are fine." + ("" if mix == "nothing more" else " Fold in " + mix + ".")
    R(F, t, "Baking", cu, 15, mins, 12, meals + ", Baked goods", ings,
      ["Heat the oven to 200°C (180°C fan, 400°F) and line a 12-hole muffin tin with paper cases",
       "Whisk " + dry + " in a large bowl", "In a jug, whisk " + wet, fold,
       "Spoon the batter into the cases. " + (top + "bake" if top else "Bake") + " for " + str(mins) + " minutes, until risen and a skewer pushed into the middle comes out clean",
       "Cool in the tin for 5 minutes, then lift onto a rack. They keep for 2 days in a tin or 2 months in the freezer"])

# ---------------- overnight oats ----------------
OATS = [
 ("Pear and Ginger Overnight Oats", "1|pear, grated; 0.5 tsp|ground ginger; 2 tbsp|walnuts, chopped; 1 tbsp|maple syrup", "the grated pear and ground ginger", "walnuts and a drizzle of maple syrup", "milk", "yogurt"),
 ("Peanut Butter Banana Overnight Oats", "2 tbsp|peanut butter; 1|banana, sliced; 1 tbsp|honey", "the peanut butter and honey", "banana slices", "milk", "yogurt"),
 ("Mango Coconut Overnight Oats", "1|mango, diced; 2 tbsp|shredded coconut; 1 tbsp|maple syrup", "the coconut and maple syrup", "diced mango", "coconut milk", "coconut yogurt"),
 ("Chocolate Cherry Overnight Oats", "1 tbsp|cocoa powder; 100 g|cherries, pitted; 1 tbsp|maple syrup", "the cocoa and maple syrup", "the cherries", "almond milk", "coconut yogurt"),
 ("Carrot Cake Overnight Oats", "1|carrot, finely grated; 1 tbsp|raisins; 0.5 tsp|cinnamon; 1 tbsp|maple syrup; 1 tbsp|pecans, chopped", "the carrot, raisins, cinnamon and maple syrup", "chopped pecans", "milk", "yogurt"),
 ("Raspberry Almond Overnight Oats", "100 g|raspberries; 2 tbsp|almond butter; 1 tbsp|honey; 1 tbsp|almonds, sliced", "half the raspberries, mashed, with the almond butter and honey", "the rest of the raspberries and the almonds", "almond milk", "yogurt"),
 ("Pumpkin Spice Overnight Oats", "4 tbsp|canned pumpkin; 0.5 tsp|pumpkin pie spice; 1 tbsp|maple syrup; 1 tbsp|pumpkin seeds", "the pumpkin, spice and maple syrup", "pumpkin seeds", "milk", "yogurt"),
 ("Blueberry Lemon Overnight Oats", "100 g|blueberries; 1|lemon, zest only; 1 tbsp|honey", "the lemon zest and honey", "the blueberries", "milk", "yogurt"),
]
for t, extra, mixin, topping, milk, yog in OATS:
    R(F, t, "Breakfast", "American", 5, 0, 2, "Breakfast, Brunch",
      "100 g|oats; 1 tbsp|chia seeds; 200 ml|" + milk + "; 100 g|" + yog + "; " + extra,
      ["In a bowl or two jars, stir together the oats, chia seeds, " + milk + " and " + yog + ".",
       "Stir in " + mixin + ". Cover and chill overnight, or for at least 4 hours.",
       "In the morning, loosen with a splash more " + milk + " if it is too thick and top with " + topping + ". Eat cold, or warm for 1 minute in the microwave"])

# ---------------- egg bakes and frittatas ----------------
FRIT = [
 ("Potato and Onion Frittata", "Spanish", "2|potatoes, peeled and thinly sliced; 1|onion, thinly sliced", "Cook the potatoes and onion in the oil over medium heat, covered, for 12 minutes until tender, stirring now and then", "a handful of parsley", 8, "|parsley, chopped|opt"),
 ("Broccoli Cheddar Frittata", "American", "200 g|broccoli, in small florets; 80 g|cheddar, grated", "Cook the broccoli in the oil with a splash of water, covered, for 5 minutes until just tender", "the cheese", 8, ""),
 ("Zucchini and Mint Frittata", "Italian", "2|zucchini, thinly sliced; 40 g|parmesan, grated; 1 handful|mint, chopped", "Fry the zucchini in the oil for 6 minutes until golden at the edges", "the parmesan and mint", 8, ""),
 ("Sausage and Pepper Egg Bake", "American", "4|sausages, sliced; 1|bell pepper, diced; 60 g|cheddar, grated", "Brown the sausages in the oil for 6 minutes, then add the pepper and cook 3 minutes more", "the cheese", 8, ""),
 ("Asparagus and Goat Cheese Frittata", "French", "200 g|asparagus, cut into short lengths; 80 g|goat cheese, crumbled; 1 tbsp|chives, snipped", "Cook the asparagus in the oil for 4 minutes until bright green", "the goat cheese and chives", 8, ""),
 ("Mushroom and Spinach Egg Bake", "American", "250 g|mushrooms, sliced; 100 g|spinach; 2 cloves|garlic, chopped; 50 g|mozzarella, grated", "Fry the mushrooms in the oil for 6 minutes until browned, add the garlic and spinach and stir until wilted", "the mozzarella", 8, ""),
 ("Leftover Pasta Frittata", "Italian", "200 g|cooked spaghetti; 50 g|parmesan, grated; 1 handful|basil", "Warm the cooked pasta in the oil for 2 minutes, spreading it out in an even layer", "the parmesan and torn basil", 8, ""),
 ("Sweet Potato and Kale Frittata", "American", "1|sweet potato, diced small; 60 g|kale, shredded; 60 g|feta, crumbled", "Cook the sweet potato in the oil, covered, for 10 minutes until tender, then stir in the kale for 1 minute", "the feta", 8, ""),
 ("Smoked Salmon and Dill Frittata", "Scandinavian", "100 g|smoked salmon, torn; 2 tbsp|dill, chopped; 2|green onions, sliced; 3 tbsp|cream cheese", "Soften the green onions in the oil for 2 minutes", "the salmon, small spoonfuls of cream cheese and the dill", 8, ""),
 ("Chorizo and Potato Egg Bake", "Spanish", "100 g|chorizo, diced; 2|potatoes, diced small; 1|red onion, sliced", "Fry the chorizo for 2 minutes until it releases its oil, then add the potatoes and onion, cover and cook 12 minutes until tender", "the smoked paprika", 8, "1 tsp|smoked paprika"),
]
for t, cu, add, cook1, scatter, eggs, extra in FRIT:
    ing = str(eggs) + "|eggs; 4 tbsp|milk; |salt; |black pepper; 2 tbsp|olive oil; " + add + ("; " + extra if extra else "")
    R(F, t, "Breakfast", cu, 10, 25, 4, "Breakfast, Brunch, Lunch, Dinner", ing,
      ["Heat the oven to 190°C (170°C fan, 375°F). Beat the eggs with the milk and a good pinch of salt and pepper.",
       cook1 + ", using a 24 cm ovenproof frying pan.",
       "Pour in the eggs and scatter over " + scatter + ". Cook on the hob for 2 minutes until the edges set.",
       "Bake for 12 to 15 minutes until puffed and just firm in the middle. Rest 5 minutes, then cut into wedges. Good warm or cold"])

# ---------------- pancakes, waffles, crepes ----------------
PAN = [
 ("Strawberry Buttermilk Pancakes", "American", "200 g|flour; 1 tsp|baking powder; 0.5 tsp|baking soda; 1 tbsp|sugar; 1 pinch|salt; 300 ml|buttermilk; 1|egg; 2 tbsp|butter, melted; 150 g|strawberries, diced", "scatter a few pieces of strawberry over each pancake before flipping", "maple syrup"),
 ("Banana Oat Pancakes", "American", "2|ripe bananas; 2|eggs; 100 g|oats; 1 tsp|baking powder; 0.5 tsp|cinnamon; 120 ml|milk", "blend everything until smooth first", "sliced banana and honey"),
 ("Lemon Ricotta Pancakes", "Italian", "150 g|flour; 1 tsp|baking powder; 2 tbsp|sugar; 1 pinch|salt; 250 g|ricotta; 2|eggs; 150 ml|milk; 1|lemon, zest and juice", "use a gentle heat, as they brown quickly", "berries and powdered sugar"),
 ("Cornmeal Pancakes", "Southern US", "120 g|cornmeal; 100 g|flour; 2 tsp|baking powder; 1 tbsp|sugar; 0.5 tsp|salt; 300 ml|milk; 1|egg; 2 tbsp|butter, melted", "let the batter rest 5 minutes so the cornmeal softens", "butter and honey"),
 ("Chocolate Chip Pancakes", "American", "200 g|flour; 2 tsp|baking powder; 1 tbsp|sugar; 1 pinch|salt; 280 ml|milk; 1|egg; 2 tbsp|butter, melted; 80 g|chocolate chips", "sprinkle chocolate chips over each pancake before flipping", "sliced strawberries"),
 ("Vegan Fluffy Pancakes", "American", "200 g|flour; 2 tsp|baking powder; 1 tbsp|sugar; 1 pinch|salt; 300 ml|soy milk; 1 tbsp|apple cider vinegar; 2 tbsp|vegetable oil; 1 tsp|vanilla extract", "stir the vinegar into the soy milk 5 minutes before you start", "maple syrup and fruit"),
 ("Whole Wheat Apple Pancakes", "American", "200 g|whole wheat flour; 2 tsp|baking powder; 1 tsp|cinnamon; 1 tbsp|brown sugar; 1 pinch|salt; 300 ml|milk; 1|egg; 1|apple, grated", "stir the grated apple into the batter last", "yogurt and maple syrup"),
 ("Savory Scallion Pancakes with Egg", "Chinese", "200 g|flour; 0.5 tsp|salt; 250 ml|water; 2|eggs; 4|green onions, sliced; 1 tsp|sesame oil; 2 tbsp|soy sauce, to dip", "make 4 large thin pancakes and press each one flat with a spatula", "soy sauce for dipping"),
 ("Crisp Vanilla Waffles", "Belgian", "250 g|flour; 2 tsp|baking powder; 2 tbsp|sugar; 1 pinch|salt; 2|eggs; 400 ml|milk; 100 g|butter, melted; 1 tsp|vanilla extract", "cook in a hot greased waffle iron for 4 to 5 minutes each instead of a pan", "whipped cream and strawberries"),
 ("Buckwheat Crepes with Ham and Cheese", "French", "150 g|buckwheat flour; 1 pinch|salt; 1|egg; 350 ml|water; 4 slices|ham; 100 g|gruyere, grated; 1 tbsp|butter", "make 4 thin crepes; on the second side add ham and cheese, fold in the edges and cook until the cheese melts", "a green salad"),
 ("Pumpkin Pancakes", "American", "200 g|flour; 2 tsp|baking powder; 1 tsp|pumpkin pie spice; 2 tbsp|brown sugar; 1 pinch|salt; 150 g|canned pumpkin; 250 ml|milk; 1|egg; 2 tbsp|butter, melted", "cook a little lower and slower than usual so the middles set", "maple syrup and pecans"),
 ("Cottage Cheese Protein Pancakes", "American", "250 g|cottage cheese; 3|eggs; 80 g|oats; 1 tsp|baking powder; 1 tbsp|honey", "blend everything until smooth first", "berries"),
]
CUSTOM = {
 "Crisp Vanilla Waffles": ["Heat a waffle iron and brush it with a little oil", "Pour in enough batter to just cover the grid, close and cook for 4 to 5 minutes until deep golden and crisp"],
 "Buckwheat Crepes with Ham and Cheese": ["Heat a large non-stick pan over medium heat and wipe it with butter", "Pour in a quarter of the batter and swirl to coat the pan thinly. Cook 2 minutes, flip, then lay a slice of ham and some cheese in the middle. Fold in the four sides to make a square and cook until the cheese melts"],
 "Savory Scallion Pancakes with Egg": ["Heat a little oil in a large non-stick pan over medium heat", "Pour in a quarter of the batter and tilt to spread it thinly. Cook 3 minutes, pressing flat with a spatula, until golden underneath, then flip and cook 2 minutes more"],
}
for t, cu, ings, tip, serve in PAN:
    savory = "Scallion" in t or "Buckwheat" in t
    if t in CUSTOM:
        R(F, t, "Breakfast", cu, 10, 20, 4, "Breakfast, Brunch" + (", Lunch" if savory else ""), ings + "; 1 tbsp|vegetable oil, for the pan",
          ["Whisk the batter ingredients together until smooth" + (" (the eggs go into the batter)" if "Scallion" in t else "") + ", then let it rest for 10 minutes"] + CUSTOM[t] +
          ["Repeat with the rest of the batter. Serve hot with " + serve])
        continue
    meals = "Breakfast, Brunch" + (", Lunch" if savory else "")
    R(F, t, "Breakfast", cu, 10, 20, 4, meals, ings + ("" if "vegetable oil" in ings else "; 1 tbsp|vegetable oil, for the pan"),
      ["Mix the dry ingredients in a large bowl. Whisk the wet ingredients in a jug, pour them in and stir until just combined",
       "Heat a non-stick pan over medium heat and wipe it with a little oil. Tip: " + tip,
       "Cook ladlefuls of batter for 2 to 3 minutes until bubbles appear and the underside is golden, flip and cook 1 to 2 minutes more",
       "Keep warm in a low oven while you cook the rest. Serve with " + serve])

# ---------------- breakfast toasts ----------------
TOAST = [
 ("Ricotta Toast with Honey and Figs", "Italian", "4 slices|sourdough; 200 g|ricotta; 4|figs, sliced; 2 tbsp|honey; 1 tbsp|pistachios, chopped", "Spread thickly with ricotta, lay over the figs, drizzle with honey and scatter with pistachios", 0),
 ("Peanut Butter Banana Toast", "American", "4 slices|whole wheat bread; 4 tbsp|peanut butter; 2|bananas, sliced; 1 pinch|cinnamon; 1 tbsp|honey | opt", "Spread with peanut butter, top with banana slices, dust with cinnamon and drizzle with honey", 0),
 ("Tomato and Garlic Toast", "Spanish", "4 slices|sourdough; 1 clove|garlic, halved; 2|ripe tomatoes, halved; 2 tbsp|olive oil; |salt", "Rub with the cut garlic, then rub the cut tomato hard into the bread until only the skin is left. Drizzle with olive oil and sprinkle with salt", 0),
 ("Mushroom and Egg Toast", "British", "4 slices|sourdough; 250 g|mushrooms, sliced; 1 tbsp|butter; 1 tsp|dried thyme; 4|eggs; |salt; |black pepper", "Fry the mushrooms in butter with the thyme for 6 minutes. Fry the eggs in the same pan. Pile the mushrooms and eggs on the toast", 8),
 ("Beans on Toast with Cheese", "British", "4 slices|bread; 1 can|baked beans; 60 g|cheddar, grated; 1 tsp|worcestershire sauce | opt", "Heat the beans with the Worcestershire sauce, spoon over the toast and scatter with cheese", 5),
 ("Avocado Toast with Chili and Lime", "Mexican", "4 slices|sourdough; 2|avocados; 1|lime; 0.5 tsp|chili flakes; |salt; 1 tbsp|cilantro, chopped | opt", "Mash the avocado with lime juice and salt, spread on the toast and finish with chili flakes and cilantro", 0),
 ("Smoked Salmon Cream Cheese Toast", "Scandinavian", "4 slices|rye bread; 100 g|cream cheese; 120 g|smoked salmon; 0.5|cucumber, thinly sliced; 1 tbsp|dill; 1|lemon, cut into wedges", "Spread with cream cheese, top with cucumber and smoked salmon and finish with dill and a squeeze of lemon", 0),
 ("Hummus Toast with Cucumber", "Middle Eastern", "4 slices|whole wheat bread; 6 tbsp|hummus; 0.5|cucumber, sliced; 8|cherry tomatoes, halved; 1 tsp|za'atar; 1 tbsp|olive oil", "Spread with hummus, top with cucumber and tomatoes, then sprinkle with za'atar and drizzle with oil", 0),
 ("Cinnamon Sugar Toast", "American", "4 slices|bread; 3 tbsp|butter, soft; 3 tbsp|sugar; 2 tsp|cinnamon", "Mix the sugar and cinnamon. Spread the bread with butter, sprinkle generously and grill for 1 to 2 minutes until bubbling", 3),
 ("Spinach and Feta Egg Toast", "Greek", "4 slices|sourdough; 150 g|spinach; 1 tbsp|olive oil; 4|eggs; 60 g|feta, crumbled; |black pepper", "Wilt the spinach in the oil, then scramble the eggs into it and stir in the feta. Pile onto the toast", 6),
]
for t, cu, ings, how, cook in TOAST:
    sweet = any(w in t for w in ("Honey", "Banana", "Cinnamon"))
    meals = "Breakfast, Brunch" + ("" if sweet else ", Lunch") + (", Snack" if sweet or cook == 0 else "")
    R(F, t, "Breakfast", cu, 5, cook + 3, 4 if "4 slices" in ings else 2, meals, ings,
      ["Toast the bread until golden and crisp", how, "Eat straight away"])

# ---------------- porridges and bowls ----------------
R(F, "Millet Porridge with Dates", "Breakfast", "North African", 5, 25, 4, "Breakfast, Brunch",
  "150 g|millet; 500 ml|milk; 300 ml|water; 6|dates, chopped; 0.5 tsp|cinnamon; 1 pinch|salt; 2 tbsp|almonds, toasted",
  "Rinse the millet, then toast it in a dry saucepan for 3 minutes until it smells nutty / Add the milk, water, dates, cinnamon and salt. Bring to a simmer and cook gently for 20 minutes, stirring often, until creamy / Spoon into bowls and top with the toasted almonds")
R(F, "Barley Breakfast Porridge with Pear", "Breakfast", "Scandinavian", 5, 40, 4, "Breakfast, Brunch",
  "150 g|barley; 900 ml|water; 300 ml|milk; 2|pears, diced; 0.5 tsp|cardamom; 2 tbsp|honey; 1 pinch|salt",
  "Rinse the barley and simmer it in the water with the salt for 30 minutes until tender (or soak it overnight to save 10 minutes) / Add the milk, pears and cardamom and cook 10 minutes more, stirring, until creamy / Sweeten with the honey and serve warm")
R(F, "Savory Oats with Fried Egg", "Breakfast", "International", 5, 10, 2, "Breakfast, Brunch, Lunch",
  "100 g|oats; 400 ml|vegetable stock; 30 g|parmesan, grated; 2|eggs; 1 tbsp|olive oil; 2|green onions, sliced; 1 tsp|hot sauce | opt",
  "Simmer the oats in the stock for 5 minutes, stirring, until thick. Stir in the parmesan / Meanwhile fry the eggs in the oil until the whites are set and the yolks still runny / Spoon the oats into bowls, top each with an egg, the green onions and a few drops of hot sauce")
R(F, "Coconut Rice Pudding Porridge", "Breakfast", "Thai", 5, 25, 4, "Breakfast, Brunch, Dessert",
  "150 g|sticky rice; 400 ml|coconut milk; 300 ml|water; 2 tbsp|sugar; 1 pinch|salt; 1|mango, diced; 1 tbsp|sesame seeds, toasted",
  "Rinse the rice, then simmer it with the coconut milk, water, sugar and salt for 25 minutes, stirring often, until thick and soft / Add a splash more water if it gets too thick / Serve warm topped with the mango and sesame seeds")
R(F, "Cornmeal Porridge with Nutmeg", "Breakfast", "Jamaican", 5, 15, 4, "Breakfast, Brunch",
  "120 g|cornmeal; 800 ml|milk; 0.5 tsp|nutmeg; 0.5 tsp|cinnamon; 1 tsp|vanilla extract; 3 tbsp|condensed milk; 1 pinch|salt",
  "Whisk the cornmeal into 200 ml of cold milk until smooth / Heat the rest of the milk with the spices and salt. When it simmers, whisk in the cornmeal mixture and keep whisking for 2 minutes / Cook gently for 12 minutes, stirring often, until thick. Stir in the vanilla and condensed milk and serve hot")
R(F, "Buckwheat Porridge with Apple", "Breakfast", "Eastern European", 5, 15, 2, "Breakfast, Brunch",
  "120 g|buckwheat; 350 ml|water; 150 ml|milk; 1|apple, diced; 1 tbsp|honey; 1 tbsp|butter; 1 pinch|salt",
  "Rinse the buckwheat, then simmer it in the water with the salt, covered, for 10 minutes / Add the milk and apple and cook 5 minutes more until soft and creamy / Stir in the butter and honey and serve")

# ---------------- breakfast burritos, sandwiches, hashes ----------------
R(F, "Black Bean Breakfast Burritos", "Breakfast", "Tex-Mex", 10, 15, 4, "Breakfast, Brunch, Lunch",
  "6|eggs; 1 can|black beans, drained; 1 tsp|cumin; 4|tortillas; 80 g|cheddar, grated; 4 tbsp|salsa; 1|avocado, sliced; 1 tbsp|vegetable oil",
  "Warm the black beans with the cumin and a splash of water, mashing a few / Scramble the eggs in the oil over low heat / Warm the tortillas, fill each with beans, eggs, cheese, salsa and avocado, fold in the sides and roll up / Toast the burritos seam side down in a dry pan for 1 minute per side")
R(F, "Tofu Breakfast Burritos", "Breakfast", "Tex-Mex", 10, 15, 4, "Breakfast, Brunch, Lunch",
  "400 g|firm tofu, drained; 1 tbsp|vegetable oil; 0.5 tsp|turmeric; 1 tsp|cumin; 1|bell pepper, diced; 1 can|black beans, drained; 4|tortillas; 4 tbsp|salsa; 1|avocado, sliced",
  "Fry the pepper in the oil for 4 minutes. Crumble in the tofu with the turmeric, cumin and a pinch of salt and cook 5 minutes / Stir in the black beans to warm through / Fill the warmed tortillas with the tofu, salsa and avocado and roll up")
R(F, "Sausage and Egg Breakfast Muffins", "Breakfast", "American", 10, 15, 4, "Breakfast, Brunch",
  "300 g|ground pork; 1 tsp|dried sage; 0.5 tsp|black pepper; 0.5 tsp|salt; 4|eggs; 4|english muffins, split; 4 slices|american cheese; 1 tbsp|vegetable oil",
  "Mix the pork with the sage, salt and pepper and shape into 4 thin patties a little wider than the muffins / Fry the patties in the oil for 4 minutes per side until cooked through. Top each with cheese for the last minute / Fry the eggs, toast the muffins and stack muffin, patty, egg, muffin")
R(F, "Corned Beef-Style Potato Hash", "Breakfast", "British", 10, 25, 4, "Breakfast, Brunch, Dinner",
  "600 g|potatoes, diced; 1|onion, diced; 200 g|roast beef slices, chopped; 1 tbsp|worcestershire sauce; 2 tbsp|vegetable oil; 4|eggs; |salt; |black pepper",
  "Boil the potatoes for 6 minutes, then drain well / Fry the onion in the oil for 5 minutes, add the potatoes and press down. Cook 10 minutes, turning now and then, until crusty / Stir in the beef and Worcestershire sauce and heat through. Make 4 hollows, crack in the eggs, cover and cook 4 minutes until set")
R(F, "Chickpea Breakfast Hash", "Breakfast", "Middle Eastern", 10, 20, 4, "Breakfast, Brunch, Lunch, Dinner",
  "2|potatoes, diced; 1 can|chickpeas, drained; 1|red onion, diced; 1 tsp|smoked paprika; 1 tsp|cumin; 2 tbsp|olive oil; 100 g|spinach; 4|eggs; 4 tbsp|yogurt",
  "Fry the potatoes in the oil, covered, for 10 minutes until nearly tender / Add the onion, chickpeas and spices and fry uncovered 6 minutes until crisp. Stir in the spinach / Make 4 hollows, crack in the eggs, cover and cook 4 minutes. Serve with a spoon of yogurt")
R(F, "Breakfast Tostadas", "Breakfast", "Mexican", 10, 10, 4, "Breakfast, Brunch, Lunch",
  "4|corn tortillas; 1 tbsp|vegetable oil; 200 g|refried beans; 4|eggs; 4 tbsp|salsa; 50 g|queso fresco, crumbled; 1|avocado, sliced",
  "Brush the tortillas with oil and bake at 200°C (400°F) for 8 minutes until crisp, or fry them / Warm the beans and fry the eggs / Spread the tostadas with beans and top with an egg, salsa, queso fresco and avocado")
R(F, "Chorizo Egg Breakfast Quesadilla", "Breakfast", "Tex-Mex", 5, 12, 2, "Breakfast, Brunch, Lunch",
  "60 g|chorizo, diced; 4|eggs; 2|tortillas; 60 g|monterey jack, grated; 2|green onions, sliced; 2 tbsp|salsa; 1 tbsp|butter",
  "Fry the chorizo for 2 minutes, then scramble the eggs softly with it in half the butter with the green onions / Lay the eggs over half of each tortilla, scatter with cheese and fold / Cook in the rest of the butter for 2 minutes per side until golden. Cut into wedges and serve with salsa")
R(F, "Japanese-Style Egg Sandwich", "Breakfast", "Japanese", 10, 10, 2, "Breakfast, Brunch, Lunch, Snack",
  "4|eggs; 3 tbsp|mayonnaise; 1 tsp|sugar; 1 tsp|milk; |salt; |white pepper; 4 slices|brioche",
  "Boil the eggs for 10 minutes, cool in cold water and peel / Mash the eggs with a fork and mix in the mayonnaise, sugar, milk, salt and white pepper / Spread thickly between the bread, press gently, trim the crusts if you like and cut in half")
R(F, "Croissant Breakfast Sandwiches", "Breakfast", "French", 5, 10, 4, "Breakfast, Brunch",
  "4|croissants, split; 6|eggs; 1 tbsp|butter; 4 slices|ham; 4 slices|swiss cheese; |black pepper",
  "Scramble the eggs softly in the butter with a pinch of pepper / Fill each croissant with ham, cheese and egg / Warm in a 180°C (350°F) oven for 4 minutes until the cheese melts")
R(F, "Breakfast Pizza with Bacon and Egg", "Breakfast", "American", 15, 15, 4, "Breakfast, Brunch",
  "500 g|pizza dough; 6 slices|bacon, chopped; 100 g|mozzarella, grated; 4|eggs; 2|green onions, sliced; 1 tbsp|olive oil",
  "Heat the oven to 240°C (220°C fan, 475°F). Stretch the dough onto an oiled tray / Scatter with mozzarella and bacon and bake for 7 minutes / Crack the eggs onto the pizza and bake 6 to 8 minutes more until the whites set. Finish with green onions")

# ---------------- granola and baked oats ----------------
GRAN = [
 ("Maple Pecan Granola", "300 g|oats; 100 g|pecans, chopped; 60 ml|maple syrup; 60 ml|vegetable oil; 1 tsp|cinnamon; 1 pinch|salt; 60 g|raisins", "the raisins"),
 ("Seeded Olive Oil Granola", "300 g|oats; 50 g|pumpkin seeds; 50 g|sunflower seeds; 2 tbsp|sesame seeds; 80 ml|maple syrup; 60 ml|olive oil; 1 tsp|cinnamon; 0.5 tsp|salt; 60 g|dried apricots, chopped", "the apricots"),
 ("Coconut Cashew Granola", "300 g|oats; 100 g|cashews; 50 g|shredded coconut; 60 ml|maple syrup; 60 ml|coconut oil, melted; 1 pinch|salt; 60 g|dried cranberries", "the cranberries"),
 ("Chocolate Hazelnut Granola", "300 g|oats; 100 g|hazelnuts, chopped; 2 tbsp|cocoa powder; 80 ml|maple syrup; 60 ml|vegetable oil; 1 pinch|salt; 60 g|chocolate chips", "the chocolate chips once completely cool"),
]
for t, ings, add in GRAN:
    R(F, t, "Breakfast", "American", 10, 30, 10, "Breakfast, Brunch, Snack", ings,
      ["Heat the oven to 160°C (140°C fan, 325°F) and line a large tray with baking paper",
       "Mix everything except " + add.split(" once")[0] + " in a large bowl until the oats are evenly coated",
       "Spread thinly on the tray and bake for 25 to 30 minutes, stirring halfway, until golden",
       "Leave to cool on the tray (it crisps as it cools), then mix in " + add + ". Keeps 3 weeks in a jar"])
BAKED = [
 ("Blueberry Baked Oatmeal", "150 g|blueberries", "Scatter the blueberries over the top"),
 ("Raspberry Coconut Baked Oatmeal", "150 g|raspberries; 30 g|shredded coconut", "Stir in the coconut and scatter the raspberries over the top"),
 ("Banana Chocolate Chip Baked Oatmeal", "2|ripe bananas, sliced; 60 g|chocolate chips", "Lay the banana slices on top and scatter with chocolate chips"),
 ("Peach Almond Baked Oatmeal", "3|peaches, sliced; 40 g|almonds, sliced; 0.5 tsp|almond extract", "Stir in the almond extract, lay the peaches on top and scatter with almonds"),
]
for t, add, how in BAKED:
    R(F, t, "Breakfast", "American", 10, 35, 6, "Breakfast, Brunch",
      "200 g|oats; 1 tsp|baking powder; 1 pinch|salt; 500 ml|milk; 2|eggs; 60 ml|maple syrup; 2 tbsp|butter, melted; 1 tsp|vanilla extract; " + add,
      ["Heat the oven to 190°C (170°C fan, 375°F) and butter a 20 x 30 cm baking dish",
       "Mix the oats, baking powder and salt. Whisk the milk, eggs, maple syrup, butter and vanilla and stir into the oats",
       "Pour into the dish. " + how,
       "Bake for 35 minutes until set and golden. Serve warm, cut into squares, with a splash of milk or yogurt. Reheats well"])
