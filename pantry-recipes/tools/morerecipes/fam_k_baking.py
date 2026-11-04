from lib import R
F = "40-baking.txt"
def B(t, cu, prep, cook, serv, ings, steps, meals="Baked goods, Snack"):
    R(F, t, "Baking", cu, prep, cook, serv, meals, ings, steps)

# quick loaves: title, cuisine, wet extras, dry extras, fold-ins, minutes
QL = [
 ("Lemon Poppy Seed Loaf", "American", "2|lemons, zest and juice", "1 tbsp|poppy seeds", "", 50),
 ("Chocolate Chip Banana Loaf", "American", "3|bananas, very ripe, mashed", "0.5 tsp|cinnamon", "100 g|chocolate chips", 55),
 ("Cranberry Orange Bread", "American", "1|orange, zest and juice", "0.5 tsp|cinnamon", "150 g|cranberries, fresh or frozen", 55),
 ("Apple Cinnamon Loaf", "American", "1|apple, grated", "1.5 tsp|cinnamon", "1|apple, diced", 55),
 ("Date and Walnut Loaf", "British", "150 g|dates, chopped and soaked in 150 ml hot water", "0.5 tsp|mixed spice", "60 g|walnuts, chopped", 55),
 ("Marbled Chocolate Loaf Cake", "European", "1 tsp|vanilla extract", "", "25 g|cocoa powder, mixed with 2 tbsp milk", 50),
 ("Strawberry Yogurt Loaf", "American", "120 g|greek yogurt; 1 tsp|vanilla extract", "", "150 g|strawberries, diced", 55),
 ("Pistachio Cardamom Loaf", "Middle Eastern", "1 tsp|rose water|opt", "1 tsp|cardamom", "60 g|pistachios, chopped", 50),
]
for t, cu, wet, dry, fold, mins in QL:
    base = "220 g|flour; 2 tsp|baking powder; 0.25 tsp|salt; 150 g|sugar; 2|eggs; 120 ml|milk; 100 ml|vegetable oil"
    ings = base + "; " + wet + ("; " + dry if dry else "") + ("; " + fold if fold else "")
    if "Apple Cinnamon" in t:
        ings = base + "; 2|apples (1 grated, 1 diced); 1.5 tsp|cinnamon"
    steps = ["Heat the oven to 180°C (350°F) and line a 900 g (2 lb) loaf tin",
             "Whisk the flour, baking powder, salt" + (" and spices" if dry else "") + " in a bowl",
             "In another bowl whisk the sugar, eggs, milk, oil and " + wet.split("|")[1].split(",")[0] + ", then stir into the dry ingredients until just combined",
             ("Pour half the batter into the tin, stir the cocoa paste into the rest, spoon it on top and swirl with a knife" if "Marbled" in t else ("Fold in the " + fold.split("|")[1].split(",")[0] + " and scrape into the tin" if fold else "Scrape into the tin")),
             "Bake " + str(mins) + " to " + str(mins + 10) + " minutes until a skewer comes out clean. Cool 10 minutes in the tin, then on a rack"]
    if "Apple Cinnamon" in t:
        steps[2] = "In another bowl whisk the sugar, eggs, milk, oil and grated apple, then stir into the dry ingredients until just combined"
        steps[3] = "Fold in the diced apple and scrape into the tin"
    B(t, cu, 15, mins, 10, ings, steps, "Breakfast, Snack, Baked goods")

# scones
SC = [
 ("Classic Cream Scones", "British", "", "", "jam and clotted or whipped cream"),
 ("Blueberry Lemon Scones", "American", "1|lemon, zested", "120 g|blueberries", "butter or lemon curd"),
 ("Cheddar Chive Scones", "British", "", "100 g|cheddar, grated; 2 tbsp|chives, snipped", "butter"),
 ("Cranberry Orange Scones", "American", "1|orange, zested", "80 g|dried cranberries", "butter"),
 ("Chocolate Chip Scones", "American", "1 tsp|vanilla extract", "100 g|chocolate chips", "a glass of milk"),
 ("Date and Oat Scones", "Scottish", "", "80 g|dates, chopped; 40 g|rolled oats", "butter and honey"),
]
for t, cu, flav, fold, serve in SC:
    savory = "Cheddar" in t
    B(t, cu, 15, 15, 8, "300 g|flour; 1 tbsp|baking powder; 0.5 tsp|salt" + ("" if savory else "; 50 g|sugar") + "; 80 g|butter, cold, diced; 180 ml|milk; 1|egg" + ("; " + flav if flav else "") + ("; " + fold if fold else ""),
      ["Heat the oven to 220°C (425°F) and line a tray", "Mix the flour, baking powder, salt" + ("" if savory else " and sugar") + ", then rub in the butter until it looks like breadcrumbs",
       "Stir in " + ("the flavourings and " if flav else "") + ("the " + " and ".join(x.split("|")[1].split(",")[0] for x in fold.split("; ")) + ", then " if fold else "") + "the milk beaten with the egg (keep 1 tbsp for brushing) to make a soft dough; do not overwork it",
       "Pat out 3 cm thick, cut into 8 wedges or rounds, brush with the reserved egg and bake 12 to 15 minutes until risen and golden",
       "Serve warm with " + serve], "Breakfast, Brunch, Snack, Baked goods")

# bars and traybakes
B("Oat Flapjacks", "British", 10, 25, 16, "250 g|rolled oats; 125 g|butter; 100 g|brown sugar; 4 tbsp|golden syrup; 1 pinch|salt",
  "Heat the oven to 180°C (350°F) and line a 20 cm square tin / Melt the butter, sugar and syrup together, then stir in the oats and salt / Press firmly into the tin and bake 20 to 25 minutes until golden at the edges / Cut into squares while warm, then cool completely in the tin")
B("Blondies", "American", 10, 25, 16, "150 g|butter, melted; 200 g|brown sugar; 1|egg; 1 tsp|vanilla extract; 180 g|flour; 0.5 tsp|baking powder; 0.25 tsp|salt; 120 g|white chocolate, chopped",
  "Heat the oven to 180°C (350°F) and line a 20 cm square tin / Whisk the butter and sugar, then the egg and vanilla / Fold in the flour, baking powder, salt and chocolate / Spread in the tin and bake 22 to 25 minutes until golden but still soft in the middle. Cool before cutting", "Dessert, Baked goods, Snack")
B("Raspberry Oat Bars", "American", 15, 35, 16, "200 g|flour; 150 g|rolled oats; 150 g|brown sugar; 180 g|butter, melted; 0.5 tsp|baking soda; 1 pinch|salt; 300 g|raspberry jam",
  "Heat the oven to 180°C (350°F) and line a 20 x 30 cm tin / Mix the flour, oats, sugar, butter, baking soda and salt into a crumbly dough / Press two thirds into the tin, spread with jam and scatter the rest over / Bake 30 to 35 minutes until golden. Cool before slicing", "Dessert, Baked goods, Snack")
B("Millionaire's Shortbread", "Scottish", 30, 25, 16, "250 g|flour; 80 g|sugar; 180 g|butter, divided; 1 can|sweetened condensed milk; 3 tbsp|golden syrup; 200 g|dark chocolate",
  "Heat the oven to 180°C (350°F). Rub the flour, sugar and 120 g butter into a dough, press into a lined 20 cm tin and bake 20 to 25 minutes until pale golden / Melt the rest of the butter with the condensed milk and syrup and cook, stirring constantly, for 8 to 10 minutes until thick and caramel coloured. Pour over the base and chill 1 hour / Melt the chocolate, spread over and chill until set / Cut with a hot knife", "Dessert, Baked goods")
B("Lemon Coconut Slice", "Australian", 15, 25, 16, "150 g|flour; 60 g|shredded coconut; 80 g|sugar; 120 g|butter, melted; 1 can|sweetened condensed milk; 2|lemons, zest and juice",
  "Heat the oven to 180°C (350°F) and line a 20 cm square tin / Mix the flour, coconut, sugar and butter and press into the tin; bake 12 minutes / Whisk the condensed milk with the lemon zest and juice and pour over / Bake 12 minutes more until just set. Chill and cut", "Dessert, Baked goods")
B("Chewy Granola Bars", "American", 10, 25, 12, "250 g|rolled oats; 60 g|almonds, chopped; 60 g|raisins; 30 g|pumpkin seeds; 120 g|honey; 60 g|peanut butter; 40 g|butter; 0.5 tsp|cinnamon; 1 pinch|salt",
  "Heat the oven to 170°C (340°F) and line a 20 cm square tin / Warm the honey, peanut butter and butter until smooth / Mix with the dry ingredients, press very firmly into the tin and bake 20 to 25 minutes / Cool completely before cutting into bars", "Breakfast, Snack, Baked goods")

# cookies
B("Double Chocolate Cookies", "American", 15, 11, 24, "125 g|butter, soft; 150 g|brown sugar; 50 g|sugar; 1|egg; 1 tsp|vanilla extract; 180 g|flour; 40 g|cocoa powder; 0.5 tsp|baking soda; 0.25 tsp|salt; 150 g|chocolate chips",
  "Heat the oven to 180°C (350°F) and line two trays / Cream the butter and sugars, beat in the egg and vanilla / Mix in the flour, cocoa, baking soda and salt, then the chips / Roll into balls, space well apart and bake 10 to 11 minutes; they firm up as they cool", "Dessert, Baked goods, Snack")
B("Ginger Molasses Cookies", "American", 15, 10, 24, "170 g|butter, soft; 150 g|brown sugar; 1|egg; 80 g|molasses; 280 g|flour; 2 tsp|baking soda; 2 tsp|ground ginger; 1 tsp|cinnamon; 0.25 tsp|cloves; 50 g|sugar, for rolling",
  "Heat the oven to 180°C (350°F) / Cream the butter and brown sugar, then beat in the egg and molasses / Mix in the flour, baking soda and spices / Roll into balls, roll in sugar and bake 9 to 10 minutes until crackled", "Dessert, Baked goods, Snack")
B("Lemon Crinkle Cookies", "American", 15, 11, 24, "115 g|butter, soft; 150 g|sugar; 1|egg; 2|lemons, zest and 2 tbsp juice; 220 g|flour; 1 tsp|baking powder; 0.25 tsp|salt; 60 g|powdered sugar",
  "Heat the oven to 180°C (350°F) / Cream the butter and sugar, beat in the egg, zest and juice / Mix in the flour, baking powder and salt; chill 20 minutes / Roll into balls, coat thickly in powdered sugar and bake 10 to 11 minutes", "Dessert, Baked goods, Snack")
B("Almond Butter Cookies (Flourless)", "American", 10, 10, 16, "250 g|almond butter; 120 g|brown sugar; 1|egg; 1 tsp|baking soda; 1 tsp|vanilla extract; 1 pinch|salt",
  "Heat the oven to 180°C (350°F) / Mix everything into a soft dough / Roll into balls, press with a fork and bake 9 to 10 minutes. Cool on the tray; they are fragile while warm", "Dessert, Baked goods, Snack")
B("White Chocolate Cranberry Cookies", "American", 15, 11, 24, "125 g|butter, soft; 150 g|brown sugar; 1|egg; 1 tsp|vanilla extract; 220 g|flour; 0.5 tsp|baking soda; 0.25 tsp|salt; 100 g|white chocolate, chopped; 80 g|dried cranberries",
  "Heat the oven to 180°C (350°F) / Cream the butter and sugar, beat in the egg and vanilla / Mix in the flour, baking soda and salt, then the chocolate and cranberries / Bake spoonfuls 10 to 11 minutes until golden at the edges", "Dessert, Baked goods, Snack")
B("Thumbprint Jam Cookies", "European", 20, 14, 24, "200 g|butter, soft; 100 g|sugar; 1|egg yolk; 1 tsp|vanilla extract; 280 g|flour; 0.25 tsp|salt; 120 g|jam",
  "Heat the oven to 175°C (350°F) / Cream the butter and sugar, beat in the yolk and vanilla, then the flour and salt / Roll into small balls, press a dent in each with your thumb and fill with jam / Bake 12 to 14 minutes until pale golden", "Dessert, Baked goods, Snack")
B("Chocolate Chip Oatmeal Cookies", "American", 15, 12, 24, "125 g|butter, soft; 150 g|brown sugar; 1|egg; 1 tsp|vanilla extract; 130 g|flour; 0.5 tsp|baking soda; 0.5 tsp|cinnamon; 150 g|rolled oats; 120 g|chocolate chips",
  "Heat the oven to 180°C (350°F) / Cream the butter and sugar, beat in the egg and vanilla / Mix in the flour, baking soda, cinnamon, oats and chips / Bake spoonfuls 11 to 12 minutes", "Dessert, Baked goods, Snack")

# breads and savoury baking
B("Irish Soda Bread", "Irish", 10, 40, 8, "450 g|flour; 1 tsp|baking soda; 1 tsp|salt; 400 ml|buttermilk",
  "Heat the oven to 200°C (400°F) and flour a tray / Mix the flour, baking soda and salt, then stir in the buttermilk to a soft, sticky dough / Shape into a round, cut a deep cross on top and bake 35 to 40 minutes until it sounds hollow when tapped underneath / Cool on a rack wrapped in a tea towel for a soft crust", "Breakfast, Side dish, Baked goods")
B("Garlic Herb Pull-Apart Rolls", "American", 25, 25, 12, "500 g|flour; 7 g|yeast; 1 tbsp|sugar; 1 tsp|salt; 300 ml|milk, warm; 60 g|butter, melted; 3 cloves|garlic, minced; 2 tbsp|parsley, chopped",
  "Mix the flour, yeast, sugar and salt, add the milk and 30 g butter and knead 8 minutes until smooth. Cover and rise 1 hour / Shape into 12 balls and pack into a buttered dish; rise 30 minutes / Heat the oven to 190°C (375°F). Mix the remaining butter with the garlic and parsley and brush over / Bake 22 to 25 minutes until golden", "Side dish, Baked goods")
B("Pita Bread", "Middle Eastern", 20, 15, 8, "400 g|flour; 7 g|yeast; 1 tsp|sugar; 1 tsp|salt; 250 ml|water, warm; 2 tbsp|olive oil",
  "Mix everything into a soft dough and knead 8 minutes. Cover and rise 1 hour / Heat the oven to 250°C (480°F) with a baking tray inside / Divide into 8, roll into 15 cm rounds and rest 10 minutes / Bake 2 or 3 at a time on the hot tray for 4 to 5 minutes until puffed. Wrap in a towel to keep soft", "Lunch, Side dish, Baked goods")
B("Rosemary Olive Oil Bread", "Italian", 20, 35, 10, "500 g|bread flour; 7 g|yeast; 1.5 tsp|salt; 350 ml|water, warm; 3 tbsp|olive oil; 2 sprigs|rosemary, chopped",
  "Mix the flour, yeast, salt, rosemary, water and 2 tbsp oil and knead 10 minutes. Rise 1 hour until doubled / Shape into a round loaf on a floured tray and rise 45 minutes / Heat the oven to 220°C (425°F). Brush with the rest of the oil and slash the top / Bake 30 to 35 minutes until deep golden", "Side dish, Baked goods")
B("Cheese and Onion Pinwheels", "British", 15, 20, 12, "1 sheet|puff pastry; 120 g|cheddar, grated; 1|onion, very finely chopped; 1 tsp|dijon mustard; 1|egg, beaten",
  "Heat the oven to 200°C (400°F) / Spread the pastry with mustard, scatter with cheese and onion and roll up tightly from the long side / Slice into 12 rounds, lay on a lined tray and brush with egg / Bake 18 to 20 minutes until puffed and golden", "Snack, Appetizer, Baked goods")
B("Zucchini Cheddar Muffins", "British", 15, 22, 12, "250 g|flour; 2 tsp|baking powder; 0.5 tsp|salt; 2|zucchini, grated and squeezed; 120 g|cheddar, grated; 2|eggs; 200 ml|milk; 80 ml|vegetable oil; 2|green onions, sliced",
  "Heat the oven to 190°C (375°F) and line a 12-hole muffin tin / Mix the flour, baking powder and salt, then stir in the zucchini, most of the cheese and the green onions / Whisk the eggs, milk and oil and fold in until just combined / Spoon into the tin, top with the rest of the cheese and bake 20 to 22 minutes", "Breakfast, Lunch, Snack, Baked goods")
B("Victoria Sponge", "British", 20, 25, 10, "200 g|butter, soft; 200 g|sugar; 4|eggs; 200 g|self-raising flour; 1 tsp|baking powder; 2 tbsp|milk; 150 g|raspberry jam; 200 ml|heavy cream; 1 tbsp|powdered sugar",
  "Heat the oven to 180°C (350°F) and line two 20 cm round tins / Beat the butter, sugar, eggs, flour, baking powder and milk until smooth and pale / Divide between the tins and bake 22 to 25 minutes until springy / Cool, then sandwich with jam and whipped cream and dust with powdered sugar", "Dessert, Baked goods")
B("Olive Oil Orange Cake", "Mediterranean", 15, 45, 10, "200 g|flour; 2 tsp|baking powder; 0.25 tsp|salt; 180 g|sugar; 3|eggs; 160 ml|olive oil; 2|oranges, zest and 120 ml juice; 1 tbsp|powdered sugar",
  "Heat the oven to 180°C (350°F) and line a 23 cm round tin / Whisk the eggs and sugar until pale, then whisk in the oil, zest and juice / Fold in the flour, baking powder and salt / Bake 40 to 45 minutes until a skewer comes out clean. Dust with powdered sugar", "Dessert, Baked goods")
B("Apple Cake with Cinnamon Sugar", "German", 20, 50, 10, "200 g|flour; 2 tsp|baking powder; 150 g|butter, soft; 175 g|sugar, 150 g for the batter and 25 g for the topping; 3|eggs; 3 tbsp|milk; 3|apples, sliced; 1 tsp|cinnamon",
  "Heat the oven to 180°C (350°F) and line a 23 cm springform tin / Cream the butter and sugar, beat in the eggs, then fold in the flour, baking powder and milk / Spread in the tin, press the apple slices into the batter in rings and sprinkle with the cinnamon sugar / Bake 45 to 50 minutes until golden and a skewer comes out clean", "Dessert, Baked goods")
B("Gluten-Free Almond Orange Cake", "Spanish", 15, 40, 10, "250 g|almond flour; 200 g|sugar; 5|eggs; 2|oranges, zested; 1 tsp|baking powder, gluten-free; 1 tbsp|powdered sugar",
  "Heat the oven to 180°C (350°F) and line a 23 cm tin / Whisk the eggs and sugar until thick and pale / Fold in the almond flour, zest and baking powder / Bake 35 to 40 minutes until golden and set. Cool and dust with powdered sugar", "Dessert, Baked goods")
