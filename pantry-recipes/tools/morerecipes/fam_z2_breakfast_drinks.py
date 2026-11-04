"""v2.1: more breakfasts and drinks."""
from v21lib import R
B = "85-v21-breakfast.txt"
D = "86-v21-drinks.txt"
def BR(t, cu, prep, cook, serv, ings, steps, meals="Breakfast, Brunch"):
    R(B, t, "Breakfast", cu, prep, cook, serv, meals, ings, steps)
def DR(t, cu, prep, cook, serv, ings, steps, meals="Drink"):
    R(D, t, "Drink", cu, prep, cook, serv, meals, ings, steps)

# overnight oats: flavour, extras
for t, extra in [("Apple Pie Overnight Oats", "1|apple, grated; 0.5 tsp|cinnamon; 1 tbsp|maple syrup; 2 tbsp|walnuts, chopped"),
                 ("Raspberry Almond Overnight Oats", "100 g|raspberries; 1 tbsp|almond butter; 1 tbsp|honey; 2 tbsp|sliced almonds"),
                 ("Tiramisu-Style Overnight Oats", "60 ml|coffee, cold; 1 tbsp|cocoa powder; 1 tbsp|maple syrup; 2 tbsp|mascarpone"),
                 ("Cherry Vanilla Overnight Oats", "100 g|cherries, pitted; 0.5 tsp|vanilla extract; 1 tbsp|honey"),
                 ("Pineapple Coconut Overnight Oats", "100 g|pineapple, chopped; 2 tbsp|shredded coconut; 1 tbsp|honey"),
                 ("Banana Bread Overnight Oats", "1|banana, mashed; 0.5 tsp|cinnamon; 1 tbsp|maple syrup; 2 tbsp|pecans, chopped"),
                 ("Matcha Overnight Oats", "1 tsp|matcha powder; 1 tbsp|honey; 50 g|blueberries"),
                 ("Fig and Honey Overnight Oats", "3|dried figs, chopped; 1 tbsp|honey; 2 tbsp|pistachios, chopped")]:
    BR(t, "American", 5, 0, 2, "100 g|rolled oats; 250 ml|milk; 120 g|greek yogurt; 1 tbsp|chia seeds; " + extra,
       ["Stir the oats, milk, yogurt and chia seeds together in a bowl or two jars",
        "Stir in the flavourings, keeping any nuts or crunchy toppings back",
        "Cover and chill overnight, or at least 4 hours. Stir, loosen with a splash of milk and add the toppings"])

# chia puddings
for t, liquid, flav in [("Vanilla Chia Pudding", "400 ml|almond milk", "1 tsp|vanilla extract; 2 tbsp|maple syrup; 100 g|mixed berries"),
                        ("Chocolate Chia Pudding", "400 ml|oat milk", "2 tbsp|cocoa powder; 2 tbsp|maple syrup; 1|banana, sliced"),
                        ("Mango Coconut Chia Pudding", "400 ml|coconut milk", "1|mango, diced; 1 tbsp|honey; 1|lime, zested"),
                        ("Peanut Butter Chia Pudding", "400 ml|milk", "2 tbsp|peanut butter; 1 tbsp|honey; 100 g|strawberries, sliced")]:
    BR(t, "American", 5, 0, 2, "60 g|chia seeds; " + liquid + "; " + flav,
       ["Whisk the chia seeds with the milk and any cocoa, nut butter, sweetener or flavouring until no clumps remain",
        "Leave 10 minutes, whisk again, then cover and chill at least 3 hours or overnight",
        "Spoon into bowls and top with the fruit"], "Breakfast, Snack")

# egg dishes
for t, cu, filling in [("Spinach and Feta Frittata", "Greek", "150 g|spinach; 100 g|feta, crumbled; 1|onion, sliced"),
                       ("Potato and Onion Frittata", "Spanish", "2|potatoes, thinly sliced; 1|onion, sliced; 30 g|parmesan, grated"),
                       ("Broccoli Cheddar Frittata", "American", "200 g|broccoli, small florets; 80 g|cheddar, grated; 2|spring onions, sliced"),
                       ("Mushroom and Thyme Frittata", "French", "250 g|mushrooms, sliced; 1 tsp|thyme; 40 g|gruyere, grated"),
                       ("Pepper and Chorizo Frittata", "Spanish", "1|red bell pepper, sliced; 80 g|chorizo, diced; 1|onion, sliced"),
                       ("Zucchini and Herb Frittata", "Italian", "2|zucchini, grated and squeezed; 1 handful|basil; 40 g|parmesan, grated")]:
    BR(t, cu, 10, 20, 4, "8|eggs; 0.5 tsp|salt; 2 tbsp|olive oil; " + filling,
       ["Heat the oven to 200 °C. Beat the eggs with the salt",
        "Heat the oil in an ovenproof frying pan and cook the vegetables (and chorizo, if using) for 6 to 8 minutes until soft",
        "Pour in the eggs, scatter over any cheese or herbs and cook on the hob 3 minutes until the edges set",
        "Bake 10 to 12 minutes until just set in the middle. Rest 5 minutes and cut into wedges"], "Breakfast, Brunch, Lunch")
for t, cu, add in [("Egg Muffin Cups with Spinach", "American", "100 g|spinach, chopped; 60 g|cheddar, grated"),
                   ("Egg Muffin Cups with Peppers", "American", "1|red bell pepper, diced; 2|spring onions, sliced"),
                   ("Ham and Cheese Egg Muffins", "American", "100 g|ham, diced; 60 g|cheddar, grated")]:
    BR(t, cu, 10, 20, 6, "10|eggs; 60 ml|milk; 0.5 tsp|salt; " + add,
       ["Heat the oven to 180 °C and oil a 12-hole muffin tin well",
        "Whisk the eggs, milk and salt. Divide the fillings between the holes and pour over the egg",
        "Bake 18 to 20 minutes until puffed and set. Cool 5 minutes before lifting out. They keep 4 days in the fridge"], "Breakfast, Snack")
BR("Turkish Eggs (Çılbır)", "Turkish", 10, 8, 2, "4|eggs; 250 g|greek yogurt; 1|garlic clove, grated; 30 g|butter; 1 tsp|chili flakes; 1 tbsp|dill, chopped",
   "Stir the garlic into the yogurt with a pinch of salt and spread on two plates / Poach the eggs in gently simmering water for 3 minutes / Melt the butter with the chili flakes until sizzling / Put the eggs on the yogurt, spoon over the chili butter and scatter with dill")
BR("Egg and Avocado Breakfast Tacos", "Tex-Mex", 5, 6, 2, "4|eggs; 4|corn tortillas; 1|avocado, sliced; 4 tbsp|salsa; 1 tbsp|butter",
   "Warm the tortillas in a dry pan / Scramble the eggs in the butter with a pinch of salt / Fill the tortillas with egg, avocado and salsa")
BR("Black Bean Breakfast Burrito", "Tex-Mex", 10, 10, 2, "2|flour tortillas, large; 4|eggs; 200 g|black beans, drained; 50 g|cheddar, grated; 4 tbsp|salsa; 1 tbsp|olive oil",
   "Warm the beans with a pinch of salt and mash lightly / Scramble the eggs in the oil / Fill the tortillas with beans, egg, cheese and salsa and roll up tightly / Toast seam side down in the pan for 1 to 2 minutes")
BR("Sweet Potato Breakfast Hash", "American", 10, 20, 2, "1|sweet potato, diced small; 1|red onion, diced; 1|red bell pepper, diced; 2 tbsp|olive oil; 0.5 tsp|smoked paprika; 4|eggs",
   "Cook the sweet potato in the oil, covered, over medium heat for 10 minutes / Add the onion, pepper, paprika and a pinch of salt and cook uncovered 6 to 8 minutes until browned / Make 4 hollows, crack in the eggs, cover and cook 4 to 5 minutes until the whites set")
BR("Corned Beef Hash with Eggs", "British", 10, 20, 2, "340 g|corned beef, diced; 2|potatoes, boiled and diced; 1|onion, diced; 1 tbsp|vegetable oil; 2|eggs",
   "Fry the onion in the oil for 5 minutes / Add the potatoes and corned beef, press down and cook 8 to 10 minutes, turning now and then, until crusty / Fry the eggs and serve on top")
BR("Mushroom and Spinach Breakfast Skillet", "American", 5, 12, 2, "250 g|mushrooms, sliced; 100 g|spinach; 2|garlic cloves, sliced; 1 tbsp|olive oil; 4|eggs; 2 slices|sourdough bread",
   "Fry the mushrooms in the oil over high heat until browned / Add the garlic and spinach and stir until wilted. Season / Make hollows, crack in the eggs, cover and cook 4 minutes. Serve with toast")

# toasts
for t, cu, top, how in [("Ricotta Honey Toast", "Italian", "125 g|ricotta; 2 tbsp|honey; 2 tbsp|walnuts, chopped", "Spread the ricotta on the toast, drizzle with honey and scatter the walnuts"),
                        ("Smashed Pea and Feta Toast", "British", "150 g|peas, frozen; 50 g|feta, crumbled; 1|lemon", "Cook the peas 3 minutes, drain and crush with lemon juice and salt. Pile on the toast and crumble over the feta"),
                        ("Peanut Butter Banana Toast", "American", "3 tbsp|peanut butter; 1|banana, sliced; 1 pinch|cinnamon", "Spread the peanut butter on the toast and top with banana and a pinch of cinnamon"),
                        ("Tomato Toast with Olive Oil", "Spanish", "2|tomatoes, ripe; 1|garlic clove; 2 tbsp|olive oil", "Rub the hot toast with the cut garlic. Grate the tomatoes, season with salt and spoon on. Drizzle with oil"),
                        ("Smoked Salmon Cream Cheese Toast", "American", "100 g|smoked salmon; 60 g|cream cheese; 1 tbsp|capers", "Spread the cream cheese on the toast, top with salmon and scatter the capers"),
                        ("Strawberry Cottage Cheese Toast", "American", "150 g|cottage cheese; 150 g|strawberries, sliced; 1 tbsp|honey", "Spread the cottage cheese on the toast and top with the strawberries and honey"),
                        ("Hummus and Egg Toast", "Levantine", "4 tbsp|hummus; 2|eggs, boiled; 1 pinch|paprika", "Spread the hummus on the toast. Slice the eggs on top and dust with paprika"),
                        ("Beans on Toast", "British", "400 g|baked beans; 20 g|butter; 30 g|cheddar, grated", "Heat the beans until bubbling. Butter the toast, spoon over the beans and top with cheese")]:
    BR(t, cu, 5, 3, 2, "2 slices|sourdough bread; " + top, ["Toast the bread until golden", how], "Breakfast, Brunch, Snack")

# pancakes, waffles, porridges
BR("Buttermilk Blueberry Pancakes", "American", 10, 15, 4, "200 g|flour; 2 tsp|baking powder; 0.5 tsp|baking soda; 2 tbsp|sugar; 300 ml|buttermilk; 1|egg; 30 g|butter, melted; 150 g|blueberries",
   "Whisk the flour, baking powder, soda, sugar and a pinch of salt / Beat the buttermilk, egg and butter, then stir into the flour until just combined / Ladle onto a hot buttered pan, scatter with blueberries and cook until bubbles form, about 2 minutes. Flip and cook 1 minute more")
BR("Lemon Ricotta Pancakes", "Italian", 10, 15, 4, "250 g|ricotta; 3|eggs, separated; 120 ml|milk; 120 g|flour; 1 tsp|baking powder; 1|lemon, zested; 2 tbsp|sugar",
   "Whisk the ricotta, yolks, milk, zest and sugar / Fold in the flour and baking powder / Whisk the whites to soft peaks and fold in / Cook spoonfuls in a buttered pan over medium-low heat, 2 minutes per side")
BR("Cornmeal Pancakes", "Southern US", 10, 15, 4, "120 g|cornmeal; 100 g|flour; 2 tsp|baking powder; 1 tbsp|sugar; 300 ml|milk; 1|egg; 2 tbsp|butter, melted",
   "Whisk the dry ingredients with a pinch of salt / Beat in the milk, egg and butter / Cook ladlefuls on a hot greased griddle 2 minutes per side")
BR("Buckwheat Crepes with Egg and Cheese", "French", 10, 20, 4, "125 g|buckwheat flour; 5|eggs; 300 ml|water; 80 g|gruyere, grated; 20 g|butter",
   "Whisk the flour, 1 of the eggs, the water and a pinch of salt into a thin batter and rest 30 minutes / Cook a thin crepe in a buttered pan for 1 minute, flip / Crack one of the remaining eggs in the middle, scatter with cheese and cook until the white sets, then fold the edges in. Repeat")
BR("Sweet Potato Waffles", "American", 10, 20, 4, "250 g|sweet potato, cooked and mashed; 2|eggs; 250 ml|milk; 200 g|flour; 2 tsp|baking powder; 0.5 tsp|cinnamon; 40 g|butter, melted",
   "Whisk the sweet potato, eggs, milk and butter / Stir in the flour, baking powder, cinnamon and a pinch of salt / Cook in a hot greased waffle iron for 4 to 5 minutes until crisp")
BR("Millet Porridge with Dates", "International", 5, 25, 2, "100 g|millet; 500 ml|milk; 4|dates, chopped; 0.5 tsp|cinnamon",
   "Rinse the millet and toast in a dry pan for 2 minutes / Add the milk, dates, cinnamon and a pinch of salt and simmer 20 to 25 minutes, stirring often, until creamy")
BR("Quinoa Breakfast Porridge", "Peruvian", 5, 20, 2, "100 g|quinoa, rinsed; 400 ml|milk; 1 tbsp|maple syrup; 0.5 tsp|cinnamon; 100 g|berries",
   "Simmer the quinoa in the milk with the cinnamon for 15 to 20 minutes, stirring, until soft and creamy / Sweeten with maple syrup and top with berries")
BR("Savory Oatmeal with Egg", "International", 5, 8, 1, "50 g|rolled oats; 250 ml|water; 1|egg; 1|spring onion, sliced; 1 tsp|soy sauce",
   "Simmer the oats in the water with a pinch of salt for 5 minutes / Fry or soft-boil the egg / Top the oats with the egg, spring onion and soy sauce")
BR("Polenta Breakfast Bowl", "Italian", 5, 10, 2, "80 g|polenta, quick-cook; 500 ml|milk; 30 g|parmesan, grated; 2|eggs; 1 tbsp|butter",
   "Bring the milk to a simmer and whisk in the polenta. Cook 5 minutes, whisking, until thick / Stir in the parmesan and butter / Top with fried or poached eggs")
BR("Breakfast Fried Rice", "International", 5, 10, 2, "300 g|rice, cooked and cold; 3|eggs; 2|spring onions, sliced; 100 g|peas, frozen; 1 tbsp|soy sauce; 1 tbsp|vegetable oil",
   "Scramble the eggs in half the oil and set aside / Fry the rice in the rest of the oil over high heat for 3 minutes, add the peas and cook 2 minutes / Toss in the egg, soy sauce and spring onions")
BR("Banana Oat Breakfast Cookies", "American", 10, 15, 12, "2|bananas, ripe; 150 g|rolled oats; 2 tbsp|peanut butter; 50 g|raisins; 0.5 tsp|cinnamon",
   "Heat the oven to 180 °C and line a tray / Mash the bananas and mix in everything else / Drop 12 spoonfuls on the tray and flatten. Bake 14 to 15 minutes until golden", "Breakfast, Snack")
BR("Yogurt Parfait with Granola and Berries", "American", 5, 0, 2, "400 g|greek yogurt; 100 g|granola; 150 g|mixed berries; 2 tbsp|honey",
   "Layer the yogurt, berries and granola in two glasses / Drizzle with honey and eat straight away so the granola stays crunchy", "Breakfast, Snack")
BR("Acai-Style Berry Smoothie Bowl", "Brazilian", 5, 0, 2, "300 g|mixed berries, frozen; 1|banana, frozen; 150 ml|apple juice; 50 g|granola; 2 tbsp|shredded coconut",
   "Blend the berries, banana and juice until thick and smooth, adding juice a little at a time / Spoon into bowls and top with granola and coconut")
BR("Green Smoothie Bowl", "International", 5, 0, 2, "1 handful|spinach; 2|bananas, frozen; 150 g|mango, frozen; 150 ml|almond milk; 2 tbsp|pumpkin seeds",
   "Blend the spinach, bananas, mango and milk until thick / Spoon into bowls and scatter with seeds")
BR("Congee with Ginger and Spring Onion", "Chinese", 5, 60, 4, "150 g|jasmine rice; 1.5 l|water; 3 cm|ginger, cut into matchsticks; 2|spring onions, sliced; 1 tbsp|soy sauce; 1 tsp|sesame oil",
   "Rinse the rice and put in a pot with the water and half the ginger / Simmer gently, partly covered, for 50 to 60 minutes, stirring now and then, until thick and silky / Season with salt and top with spring onions, the rest of the ginger, soy sauce and sesame oil")
BR("Masala Omelette", "Indian", 5, 5, 1, "3|eggs; 0.5|onion, finely chopped; 1|green chili, chopped; 1 tbsp|coriander, chopped; 0.25 tsp|turmeric; 1 tbsp|vegetable oil",
   "Beat the eggs with the onion, chili, coriander, turmeric and a pinch of salt / Cook in the hot oil over medium heat, lifting the edges, for 2 minutes. Flip or fold and cook 1 minute more")
BR("Japanese Rolled Omelette (Tamagoyaki)", "Japanese", 5, 8, 2, "4|eggs; 1 tbsp|sugar; 1 tsp|soy sauce; 1 tbsp|mirin; 1 tsp|vegetable oil",
   "Beat the eggs with the sugar, soy sauce and mirin / Oil a small pan over medium-low heat. Pour in a thin layer, let it just set and roll it to one side / Add another thin layer, lifting the roll so egg runs underneath, then roll back. Repeat / Rest 2 minutes and slice")
BR("Simit-Style Breakfast Plate", "Turkish", 10, 5, 2, "2|eggs, boiled; 100 g|feta; 1|cucumber, sliced; 2|tomatoes, sliced; 10|olives; 2|bread rolls",
   "Peel and halve the eggs / Arrange everything on a plate with the bread and a drizzle of olive oil")
BR("Arepas with Cheese", "Venezuelan", 15, 15, 4, "250 g|cornmeal, pre-cooked (masarepa); 350 ml|water, warm; 1 tsp|salt; 150 g|mozzarella, grated; 1 tbsp|vegetable oil",
   "Mix the flour, water and salt into a soft dough and rest 5 minutes / Shape into 6 patties about 1.5 cm thick / Cook in a lightly oiled pan over medium heat 6 to 7 minutes per side until crusty / Split and fill with the cheese while hot")
BR("Gallo Pinto with Eggs", "Costa Rican", 10, 15, 4, "400 g|rice, cooked; 400 g|black beans, cooked, with a little liquid; 1|onion, chopped; 1|red bell pepper, chopped; 2 tbsp|worcestershire sauce; 4|eggs",
   "Fry the onion and pepper in a little oil until soft / Add the beans with some liquid and the sauce and simmer 3 minutes / Stir in the rice until hot and well coloured / Fry the eggs and serve on top", "Breakfast, Brunch, Lunch")
BR("Ful Medames", "Egyptian", 5, 10, 4, "800 g|fava beans, cooked; 2|garlic cloves, crushed; 1|lemon, juiced; 1 tsp|cumin; 3 tbsp|olive oil; 1 handful|parsley, chopped",
   "Warm the beans with a splash of their liquid and mash roughly / Stir in the garlic, lemon, cumin and salt / Drizzle with the oil and scatter with parsley. Serve with flatbread")
BR("Swedish Rye Crispbread Breakfast", "Swedish", 5, 0, 2, "4|crackers, rye crispbread; 60 g|butter; 4 slices|cheese; 1|cucumber, sliced; 2|eggs, boiled",
   "Butter the crispbreads / Top with cheese, cucumber and sliced egg")

# drinks
for t, cu, ings in [("Mango Lassi", "Indian", "2|mangoes, ripe, chopped; 300 g|plain yogurt; 200 ml|milk; 2 tbsp|sugar; 1 pinch|cardamom"),
                    ("Salted Lassi", "Indian", "400 g|plain yogurt; 300 ml|water, cold; 0.5 tsp|salt; 0.5 tsp|cumin, toasted and ground"),
                    ("Rose Lassi", "Indian", "400 g|plain yogurt; 200 ml|milk; 2 tbsp|sugar; 1 tsp|rose water"),
                    ("Strawberry Lassi", "Indian", "250 g|strawberries; 300 g|plain yogurt; 150 ml|milk; 2 tbsp|honey"),
                    ("Ayran", "Turkish", "400 g|plain yogurt; 400 ml|water, cold; 0.5 tsp|salt")]:
    DR(t, cu, 5, 0, 3, ings, ["Blend everything until smooth and frothy", "Pour over ice and serve"], "Breakfast, Drink")
for t, cu, tea, flav in [("Iced Lemon Tea", "American", "4|black tea bags", "1|lemon, sliced; 3 tbsp|sugar"),
                         ("Iced Green Tea with Mint", "Moroccan", "4|green tea bags", "1 handful|mint; 2 tbsp|honey"),
                         ("Iced Hibiscus Tea", "Egyptian", "30 g|dried hibiscus flowers", "3 tbsp|sugar; 1|orange, sliced"),
                         ("Iced Raspberry Tea", "American", "4|black tea bags", "150 g|raspberries; 3 tbsp|sugar"),
                         ("Thai-Style Iced Tea", "Thai", "4|black tea bags", "4 tbsp|sweetened condensed milk; 1|star anise"),
                         ("Iced Jasmine Tea with Peach", "Chinese", "4|green tea bags, jasmine", "10|peaches, canned, sliced; 2 tbsp|sugar")]:
    DR(t, cu, 5, 5, 4, tea + "; 1 l|water; " + flav + "; |ice|opt",
       ["Bring half the water to a boil and steep the tea with any spices for 5 minutes (10 for hibiscus), then remove the bags or strain",
        "Stir in the sweetener while hot, then add the rest of the water as cold water",
        "Add the fruit or herbs, chill and serve over ice"])
for t, fruit in [("Watermelon Mint Cooler", "600 g|watermelon, chopped; 1 handful|mint; 1|lime, juiced"),
                 ("Cucumber Mint Spritzer", "1|cucumber, chopped; 1 handful|mint; 1|lime, juiced"),
                 ("Grapefruit Rosemary Spritzer", "2|grapefruits, juiced; 1 sprig|rosemary; 1 tbsp|honey"),
                 ("Blackberry Lime Fizz", "200 g|blackberries; 2|limes, juiced; 2 tbsp|sugar"),
                 ("Pomegranate Orange Spritzer", "1|pomegranate, seeds juiced; 2|oranges, juiced; 1|lime, juiced"),
                 ("Peach Ginger Fizz", "2|peaches, chopped; 2 cm|ginger, grated; 1 tbsp|honey")]:
    DR(t, "International", 10, 0, 4, fruit + "; 750 ml|sparkling water; |ice|opt",
       ["Blend or muddle the fruit, herbs and sweetener, then strain into a jug", "Top up with cold sparkling water just before serving and pour over ice"])
for t, cu, ings, st in [
    ("Spiced Hot Chocolate with Cinnamon", "Mexican", "750 ml|milk; 100 g|dark chocolate, chopped; 1|cinnamon stick; 1 tbsp|sugar; 1 pinch|salt", "Heat the milk with the cinnamon until steaming and leave 5 minutes / Remove the cinnamon, whisk in the chocolate, sugar and salt until smooth and frothy"),
    ("Chai Hot Chocolate", "Indian", "600 ml|milk; 80 g|milk chocolate, chopped; 2|black tea bags; 4|cardamom pods, crushed; 1|cinnamon stick", "Heat the milk with the tea and spices until steaming. Leave 5 minutes, then strain / Whisk in the chocolate until melted"),
    ("Iced Oat Milk Latte", "International", "120 ml|coffee, strong and cold; 250 ml|oat milk; 1 tbsp|maple syrup; |ice", "Stir the maple syrup into the coffee / Fill a glass with ice, add the oat milk and pour over the coffee"),
    ("Dalgona-Style Whipped Coffee", "Korean", "2 tbsp|instant coffee; 2 tbsp|sugar; 2 tbsp|water, hot; 300 ml|milk, cold; |ice", "Whisk the coffee, sugar and hot water for 3 to 4 minutes until thick and pale / Pour milk over ice and spoon the whipped coffee on top"),
    ("Turmeric Ginger Tea", "Indian", "500 ml|water; 1 tsp|turmeric; 3 cm|ginger, sliced; 1 tbsp|honey; 0.5|lemon, juiced", "Simmer the turmeric and ginger in the water for 10 minutes / Strain and stir in the honey and lemon"),
    ("Cinnamon Oat Horchata", "Mexican", "100 g|rolled oats; 1 l|water; 1|cinnamon stick; 4 tbsp|sugar; 1 tsp|vanilla extract", "Soak the oats and cinnamon in the water for 30 minutes / Blend until smooth and strain through a fine sieve / Stir in the sugar and vanilla and serve over ice"),
    ("Chocolate Banana Milkshake", "American", "2|bananas, frozen; 400 ml|milk; 2 tbsp|cocoa powder; 2 scoops|vanilla ice cream", "Blend everything until thick and smooth / Pour into tall glasses"),
    ("Strawberry Milkshake", "American", "250 g|strawberries; 400 ml|milk; 3 scoops|vanilla ice cream; 1 tbsp|sugar", "Blend everything until smooth and frothy / Serve straight away"),
    ("Coffee Milkshake", "American", "150 ml|coffee, cold; 300 ml|milk; 3 scoops|vanilla ice cream", "Blend everything until smooth / Serve with a straw"),
    ("Date and Almond Milkshake", "Middle Eastern" if False else "Emirati", "8|dates, pitted; 500 ml|almond milk; 1 pinch|cardamom; |ice", "Blend the dates, milk and cardamom until smooth / Pour over ice"),
    ("Avocado Shake", "Filipino", "1|avocado, ripe; 400 ml|milk; 3 tbsp|sugar; |ice", "Blend the avocado, milk and sugar with a handful of ice until smooth and thick / Serve cold"),
    ("Tamarind Agua Fresca", "Mexican", "100 g|tamarind pulp; 1.2 l|water; 100 g|sugar; |ice", "Simmer the tamarind in 500 ml of the water for 5 minutes and mash / Strain, add the sugar and the rest of the water / Chill and serve over ice"),
    ("Mint Lemonade (Limonana)", "Israeli", "4|lemons, juiced; 1 large handful|mint; 100 g|sugar; 750 ml|water, cold; |ice", "Blend the lemon juice, mint, sugar and water until the mint is fine / Strain if you like and serve over plenty of ice"),
    ("Sweet Potato Pie Smoothie", "Southern US", "150 g|sweet potato, cooked and cold; 1|banana, frozen; 300 ml|milk; 1 tbsp|maple syrup; 0.5 tsp|cinnamon", "Blend everything until smooth / Serve straight away"),
    ("Blueberry Oat Smoothie", "American", "150 g|blueberries, frozen; 3 tbsp|rolled oats; 1|banana; 300 ml|milk; 1 tbsp|honey", "Blend everything for 1 minute until smooth / Drink straight away"),
    ("Papaya Lime Smoothie", "Mexican", "300 g|papaya, chopped; 1|lime, juiced; 150 g|plain yogurt; 150 ml|water; 1 tbsp|honey", "Blend everything until smooth / Serve cold"),
    ("Hot Spiced Pineapple Drink", "Jamaican", "1 l|pineapple juice; 3 cm|ginger, sliced; 1|cinnamon stick; 4|cloves", "Simmer everything gently for 10 minutes / Strain and serve hot"),
    ("Barley Water with Lemon", "British", "100 g|pearl barley; 1.5 l|water; 2|lemons; 3 tbsp|honey", "Rinse the barley and simmer in the water for 30 minutes / Strain, add the lemon zest and juice and the honey / Chill before drinking"),
]:
    DR(t, cu, 5, 0 if "Blend" in st.split(" / ")[0] or "Stir" in st.split(" / ")[0] or "Whisk the coffee" in st else 10, 2 if "Blend everything" in st else 4, ings, st)
