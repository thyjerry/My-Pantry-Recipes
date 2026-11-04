from lib import R
F = "41-drinks.txt"
def DR(t, cu, prep, cook, serv, ings, steps, meals="Drink"):
    R(F, t, "Drink", cu, prep, cook, serv, meals, ings, steps)

# lemonades and agua frescas: title, cuisine, fruit, sweetener, acid, extra, method
AF = [
 ("Strawberry Lemonade", "American", "300 g|strawberries", "100 g|sugar", "4|lemons, juiced", "", "blend"),
 ("Mint Limeade", "Middle Eastern", "1 large handful|mint", "80 g|sugar", "6|limes, juiced", "", "blend"),
 ("Cucumber Lime Agua Fresca", "Mexican", "2|cucumbers, chopped", "60 g|sugar", "3|limes, juiced", "", "blend"),
 ("Pineapple Agua Fresca", "Mexican", "600 g|pineapple, chopped", "40 g|sugar", "1|lime, juiced", "", "blend"),
 ("Cantaloupe Agua Fresca", "Mexican", "1|cantaloupe, peeled and chopped", "40 g|sugar", "1|lime, juiced", "", "blend"),
 ("Honey Mint Lemonade", "American", "1 handful|mint, bruised", "6 tbsp|honey", "5|lemons, juiced", "", "stir"),
 ("Ginger Lemonade", "American", "5 cm|ginger, grated", "100 g|sugar", "4|lemons, juiced", "", "steep"),
 ("Hibiscus Agua Fresca (Jamaica)", "Mexican", "30 g|dried hibiscus flowers", "100 g|sugar", "1|lime, juiced", "1|cinnamon stick", "steep"),
]
for t, cu, fruit, sweet, acid, extra, how in AF:
    if how == "blend":
        st = ["Blend the " + fruit.split("|")[1].split(",")[0] + " with 500 ml cold water and the " + sweet.split("|")[1] + " until smooth",
              "Strain through a sieve into a jug, pressing on the pulp (skip this if you like it pulpy)",
              "Stir in the lime or lemon juice and 500 ml more cold water. Taste and adjust the sweetness"]
    elif how == "steep":
        st = ["Bring 500 ml water to a boil with the " + sweet.split("|")[1] + ", add the " + fruit.split("|")[1].split(",")[0] + (" and cinnamon" if extra else "") + " and steep off the heat for 15 minutes",
              "Strain into a jug and add 750 ml cold water and the citrus juice"]
    else:
        st = ["Stir the honey into 250 ml warm water until dissolved", "Add the mint, lemon juice and 750 ml cold water and stir, then leave 10 minutes for the mint to infuse"]
    st.append("Chill and serve over plenty of ice")
    DR(t, cu, 10, 0 if how != "steep" else 5, 6, fruit + "; " + sweet + "; " + acid + ("; " + extra if extra else "") + "; 1 l|water; |ice|opt", st)

# smoothies: title, cuisine, ingredients, meals
SM = [
 ("Tropical Mango Pineapple Smoothie", "Caribbean", "150 g|mango, frozen; 150 g|pineapple, frozen; 1|banana; 250 ml|coconut water; 1|lime, juiced"),
 ("Peanut Butter Banana Smoothie", "American", "2|bananas, frozen; 2 tbsp|peanut butter; 300 ml|milk; 1 tbsp|honey; 2 tbsp|rolled oats"),
 ("Berry Spinach Smoothie", "American", "200 g|mixed berries, frozen; 1 handful|spinach; 1|banana; 250 ml|almond milk; 100 g|greek yogurt"),
 ("Chocolate Cherry Smoothie", "American", "200 g|cherries, frozen; 1 tbsp|cocoa powder; 1|banana; 300 ml|almond milk; 1 tbsp|maple syrup"),
 ("Kiwi Apple Green Smoothie", "International", "2|kiwi, peeled; 1|apple, cored; 1 handful|spinach; 0.5|cucumber; 250 ml|apple juice"),
 ("Coffee Banana Breakfast Smoothie", "American", "1|banana, frozen; 150 ml|coffee, cold; 150 ml|milk; 2 tbsp|rolled oats; 1 tbsp|almond butter"),
 ("Pumpkin Pie Smoothie", "American", "150 g|pumpkin puree; 1|banana, frozen; 250 ml|milk; 1 tbsp|maple syrup; 0.5 tsp|cinnamon; 1 pinch|nutmeg"),
 ("Strawberry Oat Protein Smoothie", "American", "200 g|strawberries, frozen; 150 g|greek yogurt; 3 tbsp|rolled oats; 250 ml|milk; 1 tbsp|honey"),
 ("Carrot Orange Ginger Smoothie", "International", "2|carrots, chopped; 2|oranges, peeled; 1 tsp|ginger, grated; 1|banana, frozen; 200 ml|water"),
]
for t, cu, ings in SM:
    DR(t, cu, 5, 0, 2, ings, ["Put everything in a blender, liquids first", "Blend on high for 45 to 60 seconds until completely smooth, adding a splash more liquid if it is too thick", "Pour into glasses and drink straight away"], "Breakfast, Snack, Drink")

# hot drinks
DR("Spiced Apple Cider", "American", 5, 20, 6, "1.5 l|apple juice; 2|cinnamon sticks; 4|cloves; 2|star anise; 1|orange, sliced; 2 tbsp|brown sugar|opt",
  "Put everything in a pan and bring just to a simmer / Keep it barely simmering for 15 to 20 minutes / Strain into mugs and serve hot")
DR("Mexican Hot Chocolate", "Mexican", 5, 8, 4, "1 l|milk; 120 g|dark chocolate, chopped; 2 tbsp|sugar; 1 tsp|cinnamon; 1 pinch|chili powder; 1 tsp|vanilla extract",
  "Heat the milk until steaming / Whisk in the chocolate, sugar, cinnamon and chili until melted / Add the vanilla and whisk hard until frothy")
DR("Fresh Ginger Lemon Tea", "International", 2, 10, 2, "5 cm|ginger, sliced; 500 ml|water; 1|lemon; 1 tbsp|honey",
  "Simmer the ginger in the water for 10 minutes / Strain into mugs and add lemon juice and honey to taste")
DR("Turkish-Style Apple Tea", "Turkish", 5, 15, 4, "2|apples, chopped; 1 l|water; 1|cinnamon stick; 2|cloves; 2 tbsp|honey; 1|lemon",
  "Simmer the apples, cinnamon and cloves in the water for 15 minutes / Mash lightly, strain and stir in the honey and a squeeze of lemon")
DR("London Fog Tea Latte", "Canadian", 2, 6, 2, "2|earl grey tea bags; 250 ml|water; 250 ml|milk; 1 tbsp|honey; 0.5 tsp|vanilla extract",
  "Steep the tea bags in the boiling water for 4 minutes and remove / Heat and froth the milk with the honey and vanilla / Pour over the tea")
DR("Mocha Coffee", "Italian", 3, 5, 2, "300 ml|coffee, hot; 250 ml|milk; 2 tbsp|cocoa powder; 2 tbsp|sugar; 2 tbsp|whipped cream|opt",
  "Whisk the cocoa and sugar into the hot coffee until dissolved / Heat and froth the milk and pour it in / Top with whipped cream if you like")
DR("Pumpkin Spice Latte", "American", 5, 5, 2, "300 ml|milk; 3 tbsp|pumpkin puree; 2 tbsp|maple syrup; 1 tsp|pumpkin pie spice; 1 tsp|vanilla extract; 200 ml|coffee, strong",
  "Whisk the milk, pumpkin, maple syrup and spice in a pan over medium heat until steaming / Take off the heat, add the vanilla and whisk until frothy / Pour over the hot coffee")
DR("Matcha Latte", "Japanese", 3, 3, 1, "1 tsp|matcha powder; 60 ml|water, hot but not boiling; 200 ml|milk; 1 tsp|honey",
  "Whisk the matcha with the hot water until smooth and frothy / Heat and froth the milk with the honey / Pour the milk over the matcha (or chill both and serve over ice)")

# shakes, lassis and creamy drinks
DR("Salted Lassi", "Indian", 5, 0, 2, "400 g|yogurt; 300 ml|water, cold; 0.5 tsp|salt; 0.5 tsp|cumin, toasted and ground; 1 handful|mint",
  "Blend the yogurt, water and salt until frothy / Pour over ice and sprinkle with the cumin and torn mint")
DR("Rose Lassi", "Indian", 5, 0, 2, "400 g|yogurt; 200 ml|milk; 3 tbsp|sugar; 1 tsp|rose water; 1 tbsp|pistachios, chopped",
  "Blend the yogurt, milk, sugar and rose water until smooth / Serve chilled, topped with pistachios")
DR("Chocolate Peanut Butter Milkshake", "American", 5, 0, 2, "4 scoops|vanilla ice cream; 250 ml|milk; 2 tbsp|peanut butter; 2 tbsp|cocoa powder",
  "Blend everything until thick and smooth / Pour into chilled glasses", "Dessert, Drink")
DR("Strawberry Milkshake", "American", 5, 0, 2, "4 scoops|vanilla ice cream; 200 ml|milk; 200 g|strawberries",
  "Blend until smooth / Serve in chilled glasses with an extra strawberry on top", "Dessert, Drink")
DR("Vietnamese-Style Iced Coffee", "Vietnamese", 5, 5, 2, "300 ml|coffee, very strong; 4 tbsp|sweetened condensed milk; |ice|opt",
  "Stir the condensed milk into the hot coffee / Pour over glasses full of ice and stir well")

# iced teas, coolers and mocktails
DR("Sweet Tea", "Southern US", 5, 10, 8, "6|black tea bags; 2 l|water; 150 g|sugar; 1|lemon, sliced; |ice|opt",
  "Bring 500 ml of the water to a boil, add the tea bags and steep 5 minutes / Remove the bags and stir in the sugar until dissolved / Add the remaining cold water and chill. Serve over ice with lemon")
DR("Iced Green Tea with Mint and Lime", "International", 5, 5, 4, "4|green tea bags; 1 l|water; 3 tbsp|honey; 1|lime, sliced; 1 handful|mint; |ice|opt",
  "Steep the tea bags in 400 ml hot (not boiling) water for 3 minutes and remove / Stir in the honey, then add the remaining cold water, lime and mint / Chill and serve over ice")
DR("Virgin Mojito", "Cuban", 5, 0, 2, "1 handful|mint; 2|limes, cut into wedges; 2 tbsp|sugar; 400 ml|sparkling water; |ice|opt",
  "Muddle the mint, lime wedges and sugar in two glasses / Fill with ice and top with sparkling water. Stir and serve")
DR("Cranberry Ginger Spritzer", "American", 5, 0, 4, "400 ml|cranberry juice; 400 ml|ginger ale; 1|lime; |ice|opt",
  "Fill glasses with ice / Half-fill with cranberry juice and top with ginger ale / Squeeze in lime and stir gently")
DR("Pineapple Coconut Cooler", "Caribbean", 5, 0, 4, "500 ml|pineapple juice; 250 ml|coconut water; 100 ml|coconut milk; 1|lime; |ice|opt",
  "Shake or whisk the pineapple juice, coconut water, coconut milk and lime juice / Pour over ice")
DR("Sparkling Watermelon Mint Cooler", "International", 10, 0, 4, "600 g|watermelon, cubed; 1 handful|mint; 1|lime, juiced; 400 ml|sparkling water; |ice|opt",
  "Blend the watermelon with the mint and lime juice and strain / Pour over ice and top with sparkling water")
DR("Shirley Temple Mocktail", "American", 2, 0, 2, "400 ml|ginger ale; 2 tbsp|grenadine; 2|maraschino cherries; |ice|opt",
  "Fill glasses with ice and add the ginger ale / Pour in the grenadine so it sinks, and add a cherry")
