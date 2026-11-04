import lib
from lib import R
lib.SKIP_DUP_FILES.add("54-baking-us-cups.txt")
# US-style baking written in cups and spoons (so the "weights for dry baking ingredients" mode has work to do).
F = "54-baking-us-cups.txt"
B = "Dessert, Snack"

# ---- muffins: title, cuisine, extra ingredients, mix-in words, sugar line, spice
MUF = [
 ("Blueberry Muffins", "1 1/2 cups|blueberries", "the blueberries", "3/4 cup|sugar", "1 tsp|lemon zest"),
 ("Banana Nut Muffins", "3|bananas, mashed; 1/2 cup|walnuts, chopped", "the banana and walnuts", "2/3 cup|brown sugar", "1 tsp|cinnamon"),
 ("Double Chocolate Muffins", "1/2 cup|cocoa powder; 1 cup|chocolate chips", "the chocolate chips (the cocoa goes in with the flour)", "1 cup|sugar", "1 pinch|instant coffee, opt"),
 ("Lemon Poppy Seed Muffins", "2 tbsp|poppy seeds; 1|lemon, zested and juiced", "the poppy seeds, lemon zest and juice", "3/4 cup|sugar", "1/4 tsp|nutmeg, opt"),
 ("Cranberry Orange Muffins", "1 cup|cranberries, chopped; 1|orange, zested", "the cranberries and orange zest", "3/4 cup|sugar", "1/2 tsp|cinnamon"),
 ("Apple Cinnamon Muffins", "2|apples, diced", "the apple", "3/4 cup|brown sugar", "2 tsp|cinnamon"),
 ("Pumpkin Spice Muffins", "1 cup|pumpkin puree", "the pumpkin", "3/4 cup|brown sugar", "2 tsp|pumpkin pie spice"),
 ("Corn Muffins", "1 cup|cornmeal; 1 tbsp|honey", "the honey (the cornmeal goes in with the flour, and use only 1 cup flour)", "1/4 cup|sugar", "1/4 tsp|black pepper, opt"),
 ("Raspberry White Chocolate Muffins", "1 cup|raspberries; 1/2 cup|white chocolate chips", "the raspberries and white chocolate", "3/4 cup|sugar", "1 tsp|lemon zest"),
 ("Bran Muffins", "1 1/2 cups|wheat bran; 1/2 cup|raisins; 1/4 cup|molasses", "the bran (soak it in the milk for 10 minutes first), raisins and molasses", "1/2 cup|brown sugar", "1 tsp|cinnamon"),
 ("Zucchini Muffins", "1 1/2 cups|zucchini, grated and squeezed dry", "the zucchini", "3/4 cup|brown sugar", "1 tsp|cinnamon"),
 ("Carrot Raisin Muffins", "1 1/2 cups|carrots, grated; 1/2 cup|raisins", "the carrot and raisins", "3/4 cup|brown sugar", "1 tsp|ginger, ground"),
 ("Strawberry Oat Muffins", "1 cup|strawberries, diced; 1/2 cup|rolled oats", "the strawberries and oats", "2/3 cup|sugar", "1/2 tsp|cinnamon"),
 ("Peach Streusel Muffins", "2|peaches, diced; 2 tbsp|butter, cold; 2 tbsp|brown sugar", "the peaches; rub the cold butter with the brown sugar and 3 tbsp flour for the topping", "3/4 cup|sugar", "1/2 tsp|cinnamon"),
 ("Morning Glory Muffins", "1 cup|carrots, grated; 1|apple, grated; 1/2 cup|raisins; 1/3 cup|shredded coconut; 1/3 cup|pecans, chopped", "the carrot, apple, raisins, coconut and pecans", "2/3 cup|brown sugar", "2 tsp|cinnamon"),
]
for t, extra, words, sugar, spice in MUF:
    R(F, t, "Baking", "American", 15, 22, 12, "Breakfast, Snack",
      "2 cups|flour; " + sugar + "; 2 tsp|baking powder; 1/2 tsp|baking soda; 1/2 tsp|salt; " + spice + "; 1|egg; 1 cup|milk; 1/3 cup|vegetable oil; 1 tsp|vanilla extract; " + extra,
      ["Heat the oven to 400 °F (200 °C) and line a 12-cup muffin tin with paper cases",
       "Whisk the flour, sugar, baking powder, baking soda, salt and spice in a large bowl. Spoon the flour into the cup and level it rather than scooping, or weigh it (1 cup is about 125 g)",
       "Whisk the egg, milk, oil and vanilla in a jug, pour into the dry ingredients and stir with a spatula just until no dry flour remains; lumps are fine",
       "Fold in " + words + ", then divide between the cases",
       "Bake for 18 to 22 minutes until risen and a toothpick comes out clean. Cool in the tin for 5 minutes, then on a rack"])

# ---- drop cookies: title, mix-ins, words, bake minutes
CK = [
 ("Classic Chocolate Chip Cookies", "2 cups|chocolate chips", "the chocolate chips", 10),
 ("Oatmeal Raisin Cookies", "3 cups|rolled oats; 1 cup|raisins; 1 tsp|cinnamon", "the oats, raisins and cinnamon (use 1 1/2 cups flour)", 11),
 ("White Chocolate Macadamia Cookies", "1 1/2 cups|white chocolate chips; 1 cup|macadamia nuts, chopped", "the white chocolate and macadamias", 10),
 ("M&M-Style Candy Cookies", "1 1/2 cups|candy-coated chocolates", "the candies, pressing a few extra on top of each ball", 10),
 ("Double Chocolate Cookies", "1/2 cup|cocoa powder; 1 1/2 cups|chocolate chips", "the chocolate chips (whisk the cocoa into the flour and use 2 cups flour)", 10),
 ("Cranberry Oat White Chip Cookies", "2 cups|rolled oats; 1 cup|dried cranberries; 1 cup|white chocolate chips", "the oats, cranberries and white chocolate (use 1 3/4 cups flour)", 11),
 ("Butterscotch Pecan Cookies", "1 1/2 cups|butterscotch chips; 1 cup|pecans, chopped", "the butterscotch chips and pecans", 10),
 ("Toffee Chocolate Chunk Cookies", "1 cup|toffee bits; 1 1/2 cups|chocolate chunks", "the toffee and chocolate", 10),
 ("Monster Cookies", "1 cup|peanut butter; 2 cups|rolled oats; 1 cup|chocolate chips; 1 cup|candy-coated chocolates", "the peanut butter (beat it in with the butter), then the oats, chips and candies (use 1 1/2 cups flour)", 11),
 ("Cowboy Cookies", "2 cups|rolled oats; 1 cup|chocolate chips; 1 cup|shredded coconut; 1 cup|pecans, chopped", "the oats, chips, coconut and pecans (use 2 cups flour)", 12),
]
for t, mix, words, mins in CK:
    R(F, t, "Baking", "American", 20, mins, 36, B,
      "1 cup|butter, softened; 3/4 cup|sugar; 3/4 cup|brown sugar; 2|eggs; 2 tsp|vanilla extract; 2 1/4 cups|flour; 1 tsp|baking soda; 1/2 tsp|salt; " + mix,
      ["Heat the oven to 375 °F (190 °C) and line two baking sheets with parchment",
       "Beat the butter and both sugars until light and fluffy, about 3 minutes, then beat in the eggs one at a time and the vanilla",
       "Whisk the flour, baking soda and salt and mix in on low speed until just combined; spoon and level the flour, or weigh it",
       "Stir in " + words,
       "Scoop rounded tablespoons 2 inches (5 cm) apart and bake for %d to %d minutes until golden at the edges but still soft in the middle" % (mins - 1, mins + 1),
       "Cool on the sheet for 5 minutes, then move to a rack"])

R(F, "Snickerdoodles", "Baking", "American", 20, 10, 30, B,
  "1 cup|butter, softened; 1 1/2 cups|sugar; 2|eggs; 2 3/4 cups|flour; 2 tsp|cream of tartar; 1 tsp|baking soda; 1/4 tsp|salt; 2 tbsp|sugar, for rolling; 2 tsp|cinnamon",
  "Heat the oven to 375 °F (190 °C) / Beat the butter and 1 1/2 cups sugar until fluffy, then beat in the eggs / Whisk the flour, cream of tartar, baking soda and salt and mix in / Roll into 1 1/4-inch balls and roll in the cinnamon sugar / Bake 2 inches apart for 9 to 11 minutes until set at the edges; they crinkle as they cool")
R(F, "Peanut Butter Cookies", "Baking", "American", 15, 10, 30, B,
  "1/2 cup|butter, softened; 1 cup|peanut butter; 1/2 cup|sugar; 1/2 cup|brown sugar; 1|egg; 1 tsp|vanilla extract; 1 1/4 cups|flour; 3/4 tsp|baking soda; 1/4 tsp|salt",
  "Heat the oven to 350 °F (175 °C) / Beat the butter, peanut butter and sugars until creamy, then the egg and vanilla / Mix in the flour, baking soda and salt / Roll into 1-inch balls, set on lined sheets and press a criss-cross with a fork / Bake for 9 to 11 minutes")
R(F, "Molasses Spice Cookies", "Baking", "American", 20, 10, 30, B,
  "3/4 cup|butter, softened; 1 cup|brown sugar; 1|egg; 1/4 cup|molasses; 2 1/4 cups|flour; 2 tsp|baking soda; 2 tsp|ginger, ground; 1 tsp|cinnamon; 1/2 tsp|ground cloves; 1/4 tsp|salt; 1/4 cup|sugar, for rolling",
  "Heat the oven to 350 °F (175 °C) / Beat the butter and brown sugar, then the egg and molasses / Mix in the flour, baking soda, spices and salt / Roll into 1-inch balls and roll in sugar / Bake for 9 to 10 minutes; they should still look soft")
R(F, "Classic Shortbread", "Baking", "Scottish", 15, 25, 16, B,
  "1 cup|butter, softened; 1/2 cup|powdered sugar; 2 cups|flour; 1/4 tsp|salt; 1 tsp|vanilla extract",
  "Heat the oven to 325 °F (165 °C) / Beat the butter, powdered sugar and vanilla until smooth, then mix in the flour and salt to a soft dough / Press into an 8-inch (20 cm) square pan, prick all over with a fork and score into 16 pieces / Bake for 25 to 30 minutes until pale golden, then cut along the scores while warm")
R(F, "Sugar Cookies for Cutting", "Baking", "American", 30, 9, 30, B,
  "1 cup|butter, softened; 1 cup|sugar; 1|egg; 1 tsp|vanilla extract; 3 cups|flour; 1 1/2 tsp|baking powder; 1/2 tsp|salt",
  "Beat the butter and sugar until creamy, then the egg and vanilla / Mix in the flour, baking powder and salt, divide in two discs and chill for 1 hour / Heat the oven to 350 °F (175 °C). Roll to 1/4 inch (6 mm) on a floured surface and cut out shapes / Bake for 8 to 10 minutes until just set; cool before icing")
R(F, "Gingerbread Cookies", "Baking", "American", 30, 9, 30, B,
  "3/4 cup|butter, softened; 3/4 cup|brown sugar; 1|egg; 1/2 cup|molasses; 3 1/4 cups|flour; 1 tbsp|ginger, ground; 1 1/2 tsp|cinnamon; 1/4 tsp|ground cloves; 1 tsp|baking soda; 1/2 tsp|salt",
  "Beat the butter and sugar, then the egg and molasses / Mix in the flour, spices, baking soda and salt, wrap and chill for 2 hours / Heat the oven to 350 °F (175 °C), roll to 1/4 inch and cut out / Bake for 8 to 10 minutes")
R(F, "No-Bake Chocolate Oat Cookies", "Dessert", "American", 10, 5, 24, B,
  "1/2 cup|butter; 2 cups|sugar; 1/2 cup|milk; 1/4 cup|cocoa powder; 1/2 cup|peanut butter; 3 cups|quick oats; 1 tsp|vanilla extract; 1 pinch|salt",
  "Bring the butter, sugar, milk and cocoa to a rolling boil in a saucepan and boil for exactly 1 minute / Remove from the heat and stir in the peanut butter, oats, vanilla and salt / Drop spoonfuls onto parchment and leave for 30 minutes to set")

# ---- quick breads: title, ingredients, steps-middle, bake min
QB = [
 ("Classic Banana Bread", "3|bananas, very ripe; 1/3 cup|butter, melted; 3/4 cup|brown sugar; 1|egg; 1 tsp|vanilla extract; 1 tsp|baking soda; 1/4 tsp|salt; 1 1/2 cups|flour; 1/2 cup|walnuts, opt", "Mash the bananas in a bowl and stir in the butter, sugar, egg and vanilla. Sprinkle over the baking soda and salt, then stir in the flour and walnuts until just combined", 60),
 ("Zucchini Bread", "2 cups|zucchini, grated; 3|eggs; 1 cup|vegetable oil; 1 1/2 cups|sugar; 2 tsp|vanilla extract; 3 cups|flour; 1 tsp|baking soda; 1/4 tsp|baking powder; 1 tsp|salt; 1 tbsp|cinnamon", "Whisk the eggs, oil, sugar and vanilla, then stir in the zucchini. Whisk the flour, soda, baking powder, salt and cinnamon and fold in. This makes two loaves", 55),
 ("Pumpkin Bread", "1 can|pumpkin puree; 2|eggs; 1/2 cup|vegetable oil; 1/3 cup|water; 1 1/2 cups|sugar; 1 3/4 cups|flour; 1 tsp|baking soda; 1/2 tsp|salt; 2 tsp|pumpkin pie spice", "Whisk the pumpkin, eggs, oil, water and sugar. Whisk the flour, soda, salt and spice and fold in until smooth", 60),
 ("Irish Soda Bread", "4 cups|flour; 1 tsp|baking soda; 1 tsp|salt; 2 tbsp|sugar; 4 tbsp|butter, cold; 1 3/4 cups|buttermilk; 1/2 cup|raisins, opt", "Whisk the flour, soda, salt and sugar and rub in the butter. Stir in the buttermilk and raisins to a shaggy dough, shape into a round on a floured sheet and cut a deep cross on top", 40),
 ("Lemon Loaf Cake", "1 1/2 cups|flour; 2 tsp|baking powder; 1/2 tsp|salt; 1 cup|sugar; 2|lemons, zested and juiced; 2|eggs; 1/2 cup|milk; 1/2 cup|vegetable oil; 1 cup|powdered sugar", "Rub the zest into the sugar, then whisk in the eggs, milk, oil and 2 tbsp lemon juice. Fold in the flour, baking powder and salt. After baking, drizzle with the powdered sugar mixed with the remaining lemon juice", 50),
 ("Southern Skillet Cornbread", "1 cup|cornmeal; 1 cup|flour; 1 tbsp|baking powder; 1/2 tsp|baking soda; 1 tsp|salt; 2 tbsp|sugar; 2|eggs; 1 1/4 cups|buttermilk; 1/4 cup|butter, melted", "Heat a 10-inch cast-iron skillet with 1 tbsp butter in the oven. Whisk the dry ingredients, then the eggs, buttermilk and remaining butter, and stir together. Pour into the hot skillet", 22),
 ("Date Nut Bread", "1 cup|dates, chopped; 1 cup|water, boiling; 1 tsp|baking soda; 3/4 cup|brown sugar; 2 tbsp|butter; 1|egg; 1 3/4 cups|flour; 1/2 tsp|salt; 1/2 cup|walnuts, chopped", "Pour the boiling water over the dates and baking soda and leave 15 minutes. Beat the butter, sugar and egg, stir in the dates and liquid, then the flour, salt and nuts", 55),
 ("Cinnamon Swirl Quick Bread", "2 cups|flour; 1 cup|sugar; 2 tsp|baking powder; 1/2 tsp|salt; 1|egg; 1 cup|milk; 1/3 cup|vegetable oil; 1/3 cup|brown sugar; 2 tsp|cinnamon", "Whisk the flour, sugar, baking powder and salt, then stir in the egg, milk and oil. Pour half in the pan, sprinkle with the brown sugar and cinnamon, add the rest and swirl with a knife", 50),
 ("Blueberry Lemon Bread", "2 cups|flour; 1 tsp|baking powder; 1/2 tsp|salt; 1/2 cup|butter, softened; 1 cup|sugar; 2|eggs; 1/2 cup|milk; 1|lemon, zested; 1 1/2 cups|blueberries", "Beat the butter and sugar, then the eggs, milk and zest. Fold in the flour, baking powder and salt, then the blueberries tossed in 1 tbsp flour", 60),
 ("Beer Bread", "3 cups|self-rising flour; 3 tbsp|sugar; 12 fl oz|beer; 1/4 cup|butter, melted", "Stir the flour, sugar and beer to a sticky batter, spread in the pan and pour the butter over the top", 50),
]
for t, ings, mid, mins in QB:
    hot = "425 °F (220 °C)" if "Cornbread" in t or "Soda" in t else "350 °F (175 °C)"
    R(F, t, "Baking", "Irish" if "Irish" in t else "American", 15, mins, 10, "Breakfast, Snack", ings,
      ["Heat the oven to " + hot + ("" if "Skillet" in t or "Soda" in t else " and grease a 9x5-inch (23x13 cm) loaf pan"),
       mid,
       "Bake for about %d minutes until a skewer pushed into the middle comes out clean" % mins,
       "Cool for 10 minutes in the pan, then turn out onto a rack"])

# ---- cakes and bars written individually
CAKES = [
 ("Classic Yellow Cake", "Baking", 25, 30, 12, "2 1/2 cups|flour; 2 1/2 tsp|baking powder; 1/2 tsp|salt; 1 cup|butter, softened; 1 1/2 cups|sugar; 4|eggs; 2 tsp|vanilla extract; 1 cup|milk",
  "Heat the oven to 350 °F (175 °C) and line two 9-inch (23 cm) round pans / Beat the butter and sugar for 4 minutes until pale, then the eggs one at a time and the vanilla / Whisk the flour, baking powder and salt and add in three parts, alternating with the milk / Divide between the pans and bake for 28 to 32 minutes / Cool completely before frosting"),
 ("Chocolate Sheet Cake", "Baking", 20, 25, 20, "2 cups|flour; 2 cups|sugar; 1 tsp|baking soda; 1/2 tsp|salt; 1 cup|butter; 1/3 cup|cocoa powder; 1 cup|water; 1/2 cup|buttermilk; 2|eggs; 1 tsp|vanilla extract; 3 cups|powdered sugar; 1/3 cup|milk; 1/2 cup|pecans, chopped, opt",
  "Heat the oven to 350 °F (175 °C) and grease a 10x15-inch (25x38 cm) sheet pan / Whisk the flour, sugar, soda and salt. Bring half the butter, half the cocoa and the water to a boil and pour over; stir in the buttermilk, eggs and vanilla / Bake for 20 to 22 minutes / Meanwhile melt the rest of the butter and cocoa with the milk, beat in the powdered sugar and pecans, and pour over the warm cake"),
 ("Carrot Cake with Cream Cheese Frosting", "Baking", 30, 35, 12, "2 cups|flour; 2 tsp|baking soda; 1/2 tsp|salt; 2 tsp|cinnamon; 4|eggs; 1 1/4 cups|vegetable oil; 1 cup|sugar; 1 cup|brown sugar; 3 cups|carrots, grated; 1 cup|walnuts, chopped; 8 oz|cream cheese, softened; 1/2 cup|butter, softened; 3 cups|powdered sugar; 1 tsp|vanilla extract",
  "Heat the oven to 350 °F (175 °C) and line two 9-inch pans / Whisk the eggs, oil and sugars, then fold in the flour, soda, salt and cinnamon, then the carrots and walnuts / Bake for 30 to 35 minutes and cool / Beat the cream cheese and butter, then the powdered sugar and vanilla, and frost"),
 ("Pound Cake", "Baking", 20, 70, 12, "1 cup|butter, softened; 2 cups|sugar; 4|eggs; 3 cups|cake flour; 1/2 tsp|baking powder; 1/2 tsp|salt; 1 cup|sour cream; 2 tsp|vanilla extract",
  "Heat the oven to 325 °F (165 °C) and grease a 10-inch tube pan / Beat the butter and sugar for 5 minutes, then the eggs one at a time / Whisk the cake flour, baking powder and salt and add alternately with the sour cream and vanilla / Bake for 65 to 75 minutes until a skewer comes out clean"),
 ("Applesauce Spice Cake", "Baking", 15, 35, 12, "2 cups|flour; 1 tsp|baking soda; 1/2 tsp|salt; 2 tsp|cinnamon; 1/2 tsp|nutmeg; 1/2 cup|butter, softened; 1 cup|brown sugar; 1|egg; 1 1/2 cups|applesauce; 1/2 cup|raisins",
  "Heat the oven to 350 °F (175 °C) and grease a 9x13-inch pan / Beat the butter and sugar, then the egg and applesauce / Fold in the dry ingredients and raisins / Bake for 30 to 35 minutes"),
 ("Crazy Cake (Egg-Free Chocolate Cake)", "Baking", 10, 30, 9, "1 1/2 cups|flour; 1 cup|sugar; 1/4 cup|cocoa powder; 1 tsp|baking soda; 1/2 tsp|salt; 1 cup|water; 1/3 cup|vegetable oil; 1 tbsp|white vinegar; 1 tsp|vanilla extract",
  "Heat the oven to 350 °F (175 °C) / Whisk the dry ingredients in an 8-inch square pan, make three wells and pour the oil into one, the vinegar into another and the vanilla into the third / Pour the water over and stir until smooth / Bake for 30 to 35 minutes. This cake contains no eggs, milk or butter"),
 ("Coffee Cake with Streusel", "Baking", 20, 40, 12, "2 cups|flour; 2 tsp|baking powder; 1/2 tsp|salt; 1/2 cup|butter, softened; 3/4 cup|sugar; 2|eggs; 1 cup|sour cream; 1 tsp|vanilla extract; 3/4 cup|brown sugar; 2 tsp|cinnamon; 1/2 cup|pecans, chopped",
  "Heat the oven to 350 °F (175 °C) and grease a 9-inch square pan / Mix the brown sugar, cinnamon, pecans and 1/3 cup of the flour with 3 tbsp of the butter for the streusel / Beat the rest of the butter with the sugar, then the eggs, sour cream and vanilla, and fold in the remaining flour, baking powder and salt / Spread in the pan, scatter the streusel over and bake for 40 to 45 minutes"),
 ("Fudgy Brownies", "Baking", 15, 25, 16, "1/2 cup|butter; 1 cup|sugar; 2|eggs; 1 tsp|vanilla extract; 1/3 cup|cocoa powder; 1/2 cup|flour; 1/4 tsp|salt; 1/4 tsp|baking powder; 1/2 cup|chocolate chips",
  "Heat the oven to 350 °F (175 °C) and line an 8-inch square pan / Melt the butter, stir in the sugar, then beat in the eggs and vanilla / Stir in the cocoa, flour, salt and baking powder, then the chips / Bake for 22 to 25 minutes until the top is set but a toothpick still shows moist crumbs / Cool fully before cutting"),
 ("Blondies", "Baking", 10, 25, 16, "1/2 cup|butter, melted; 1 cup|brown sugar; 1|egg; 1 tsp|vanilla extract; 1 cup|flour; 1/2 tsp|baking powder; 1/4 tsp|salt; 1/2 cup|white chocolate chips",
  "Heat the oven to 350 °F (175 °C) and line an 8-inch square pan / Whisk the butter and sugar, then the egg and vanilla / Stir in the flour, baking powder, salt and chips / Bake for 22 to 25 minutes"),
 ("Lemon Bars", "Baking", 20, 45, 16, "1 cup|flour; 1/2 cup|butter, softened; 1/4 cup|powdered sugar; 2|eggs; 1 cup|sugar; 2 tbsp|flour, for the filling; 1/2 tsp|baking powder; 3 tbsp|lemon juice; 1|lemon, zested",
  "Heat the oven to 350 °F (175 °C) / Mix 1 cup flour, the butter and powdered sugar, press into an 8-inch square pan and bake for 20 minutes / Whisk the eggs, sugar, 2 tbsp flour, baking powder, lemon juice and zest and pour over the hot crust / Bake for 25 minutes more until set; cool and dust with powdered sugar"),
 ("Oatmeal Jam Bars", "Baking", 15, 35, 16, "1 1/2 cups|flour; 1 1/2 cups|rolled oats; 1 cup|brown sugar; 1/2 tsp|baking soda; 1/4 tsp|salt; 3/4 cup|butter, melted; 1 cup|raspberry jam",
  "Heat the oven to 350 °F (175 °C) and line a 9-inch square pan / Mix the flour, oats, sugar, soda, salt and butter to a crumble / Press two-thirds into the pan, spread with jam and scatter the rest over / Bake for 30 to 35 minutes and cool before cutting"),
 ("Seven-Layer Bars", "Baking", 10, 25, 24, "1/2 cup|butter, melted; 1 1/2 cups|graham cracker crumbs; 1 cup|chocolate chips; 1 cup|butterscotch chips; 1 1/3 cups|shredded coconut; 1 cup|pecans, chopped; 1 can|sweetened condensed milk",
  "Heat the oven to 350 °F (175 °C) / Mix the butter and crumbs and press into a 9x13-inch pan / Layer the chips, coconut and pecans, then pour the condensed milk evenly over / Bake for 25 minutes until golden; cool before cutting"),
 ("Pumpkin Bars with Cream Cheese Frosting", "Baking", 20, 25, 24, "2 cups|flour; 2 tsp|baking powder; 1 tsp|baking soda; 1/2 tsp|salt; 2 tsp|pumpkin pie spice; 4|eggs; 1 can|pumpkin puree; 1 2/3 cups|sugar; 1 cup|vegetable oil; 4 oz|cream cheese, softened; 1/4 cup|butter, softened; 2 cups|powdered sugar",
  "Heat the oven to 350 °F (175 °C) and grease a 10x15-inch pan / Whisk the eggs, pumpkin, sugar and oil, then fold in the dry ingredients / Bake for 25 to 30 minutes and cool / Beat the cream cheese, butter and powdered sugar and spread on top"),
 ("Angel Food Cake", "Baking", 30, 40, 12, "1 cup|cake flour; 1 1/2 cups|sugar; 12|egg whites; 1 1/2 tsp|cream of tartar; 1/4 tsp|salt; 1 1/2 tsp|vanilla extract",
  "Heat the oven to 350 °F (175 °C). Do not grease the tube pan / Sift the cake flour with half the sugar twice / Whisk the whites, cream of tartar and salt to soft peaks, then add the rest of the sugar gradually to stiff glossy peaks and the vanilla / Fold in the flour mixture in four parts, spoon into the pan and bake for 38 to 42 minutes / Cool upside down in the pan"),
 ("Strawberry Shortcake", "Dessert", 25, 15, 8, "2 cups|flour; 1/4 cup|sugar; 1 tbsp|baking powder; 1/2 tsp|salt; 1/2 cup|butter, cold; 2/3 cup|heavy cream; 1 lb|strawberries, sliced; 3 tbsp|sugar, for the berries; 1 cup|heavy cream, for whipping",
  "Toss the strawberries with 3 tbsp sugar and leave to juice / Heat the oven to 425 °F (220 °C). Whisk the flour, sugar, baking powder and salt, cut in the butter, and stir in 2/3 cup cream / Pat 3/4 inch thick, cut 8 rounds and bake for 13 to 15 minutes / Whip the remaining cream, split the shortcakes and fill with berries and cream"),
]
for t, cat, prep, cook, serv, ings, steps in CAKES:
    R(F, t, cat, "Scottish" if "Shortbread" in t else "American", prep, cook, serv, B, ings, steps)

# ---- breakfast bakes
BR = [
 ("Buttermilk Pancakes", 10, 15, 4, "2 cups|flour; 2 tbsp|sugar; 2 tsp|baking powder; 1/2 tsp|baking soda; 1/2 tsp|salt; 2 cups|buttermilk; 2|eggs; 3 tbsp|butter, melted; 1/4 cup|maple syrup",
  "Whisk the flour, sugar, baking powder, soda and salt / Whisk the buttermilk, eggs and butter, pour in and stir just until combined; leave lumpy and rest 5 minutes / Cook 1/4 cup scoops on a lightly buttered griddle over medium heat until bubbles form, about 2 minutes, then flip for 1 minute / Serve with maple syrup"),
 ("Belgian-Style Waffles", 10, 20, 4, "2 cups|flour; 2 tbsp|sugar; 1 tbsp|baking powder; 1/2 tsp|salt; 2|eggs, separated; 1 3/4 cups|milk; 1/2 cup|butter, melted; 1 tsp|vanilla extract",
  "Heat the waffle iron / Whisk the flour, sugar, baking powder and salt. Whisk the yolks, milk, butter and vanilla and stir in / Whip the egg whites to soft peaks and fold in / Cook about 2/3 cup per waffle until golden, 4 to 5 minutes"),
 ("Flaky Buttermilk Biscuits", 15, 14, 10, "2 cups|flour; 1 tbsp|baking powder; 1/2 tsp|baking soda; 1 tsp|salt; 1 tsp|sugar; 1/2 cup|butter, very cold; 3/4 cup|buttermilk, cold",
  "Heat the oven to 450 °F (230 °C) / Whisk the dry ingredients and grate or cut in the cold butter until in pea-sized pieces / Stir in the buttermilk to a shaggy dough, pat out, fold in thirds twice and pat to 3/4 inch / Cut straight down with a floured cutter without twisting and bake touching on a sheet for 12 to 14 minutes"),
 ("Drop Biscuits", 10, 14, 10, "2 cups|flour; 1 tbsp|baking powder; 1 tsp|salt; 1 tsp|sugar; 1/2 cup|butter, melted; 1 cup|milk",
  "Heat the oven to 450 °F (230 °C) / Whisk the dry ingredients, then stir in the butter and milk just until combined / Drop 1/4-cup mounds onto a lined sheet and bake for 12 to 14 minutes"),
 ("Cream Scones", 15, 16, 8, "2 cups|flour; 1/4 cup|sugar; 1 tbsp|baking powder; 1/2 tsp|salt; 1 1/4 cups|heavy cream; 1/2 cup|dried currants, opt; 1 tbsp|milk",
  "Heat the oven to 400 °F (200 °C) / Whisk the dry ingredients, then stir in the cream and currants until the dough just holds together / Pat into an 8-inch round, cut into 8 wedges, brush with milk / Bake for 15 to 18 minutes"),
 ("Blueberry Scones", 15, 20, 8, "2 cups|flour; 1/3 cup|sugar; 1 tbsp|baking powder; 1/2 tsp|salt; 1/2 cup|butter, cold; 1/2 cup|heavy cream; 1|egg; 1 cup|blueberries; 1|lemon, zested",
  "Heat the oven to 400 °F (200 °C) / Whisk the dry ingredients and zest, and cut in the butter / Whisk the cream and egg, stir in, then fold in the blueberries / Pat into a round, cut 8 wedges and bake for 18 to 20 minutes"),
 ("Cinnamon Rolls", 40, 25, 12, "4 cups|flour; 1/3 cup|sugar; 2 1/4 tsp|instant yeast; 1 tsp|salt; 1 cup|milk, warm; 1/3 cup|butter, melted; 2|eggs; 3/4 cup|brown sugar; 2 tbsp|cinnamon; 1/4 cup|butter, softened; 4 oz|cream cheese; 1 1/2 cups|powdered sugar",
  "Mix the flour, sugar, yeast and salt, add the milk, melted butter and eggs, and knead for 8 minutes until smooth / Cover and leave to rise until doubled, about 1 hour / Roll to a 16x12-inch rectangle, spread with the softened butter and sprinkle with brown sugar and cinnamon / Roll up, cut 12 rolls, set in a greased 9x13-inch pan and rise 30 minutes / Bake at 350 °F (175 °C) for 22 to 25 minutes and spread with the cream cheese beaten with the powdered sugar"),
 ("Dutch Baby Pancake", 5, 20, 3, "3|eggs; 1/2 cup|flour; 1/2 cup|milk; 1 tbsp|sugar; 1 pinch|salt; 3 tbsp|butter; 1|lemon; 2 tbsp|powdered sugar",
  "Heat the oven to 425 °F (220 °C) with a 10-inch cast-iron skillet inside / Blend the eggs, flour, milk, sugar and salt until smooth / Melt the butter in the hot skillet, pour in the batter and bake for 18 to 20 minutes until puffed and golden / Squeeze over lemon and dust with powdered sugar"),
 ("Baked Oatmeal", 10, 35, 6, "3 cups|rolled oats; 1/2 cup|brown sugar; 2 tsp|baking powder; 1 tsp|cinnamon; 1/2 tsp|salt; 2 cups|milk; 2|eggs; 1/4 cup|butter, melted; 1 tsp|vanilla extract; 1 cup|blueberries",
  "Heat the oven to 350 °F (175 °C) and grease an 8-inch square dish / Mix the oats, sugar, baking powder, cinnamon and salt / Whisk the milk, eggs, butter and vanilla and stir in, then fold in the blueberries / Bake for 35 to 40 minutes until set"),
 ("Popovers", 10, 35, 6, "2|eggs; 1 cup|milk; 1 cup|flour; 1/2 tsp|salt; 1 tbsp|butter, melted",
  "Heat the oven to 425 °F (220 °C) with a greased popover or muffin tin inside / Whisk everything to a smooth batter / Fill the hot cups halfway and bake for 20 minutes, then reduce to 350 °F (175 °C) for 15 minutes without opening the door / Serve at once"),
 ("Crepes", 10, 20, 4, "1 cup|flour; 2|eggs; 1 1/4 cups|milk; 2 tbsp|butter, melted; 1 tbsp|sugar; 1 pinch|salt; 1/2 cup|chocolate hazelnut spread, opt",
  "Blend everything except the spread until smooth and rest 15 minutes / Heat a lightly buttered 8-inch pan over medium heat, pour in 3 tbsp batter and swirl thin / Cook 1 minute until the edges lift, flip and cook 30 seconds / Fill with spread or fruit"),
 ("Sour Cream Coffee Muffins", 15, 20, 12, "2 cups|flour; 1 tsp|baking powder; 1 tsp|baking soda; 1/4 tsp|salt; 1/2 cup|butter, softened; 1 cup|sugar; 2|eggs; 1 cup|sour cream; 1 tsp|vanilla extract; 1/3 cup|brown sugar; 1 tsp|cinnamon",
  "Heat the oven to 375 °F (190 °C) and line a muffin tin / Beat the butter and sugar, then the eggs, sour cream and vanilla / Fold in the flour, baking powder, soda and salt / Layer the batter with the brown sugar and cinnamon in the cases and bake for 18 to 20 minutes"),
]
for t, prep, cook, serv, ings, steps in BR:
    R(F, t, "Breakfast" if "Muffin" not in t else "Baking", "French" if t in ("Crepes",) else "American", prep, cook, serv, "Breakfast, Brunch", ings, steps)

# ---- pies and fruit desserts
PIE = [
 ("All-Butter Pie Crust", "Baking", 15, 0, 8, "1 1/4 cups|flour; 1/2 tsp|salt; 1 tsp|sugar; 1/2 cup|butter, very cold, cubed; 4 tbsp|ice water",
  "Whisk the flour, salt and sugar and cut in the butter until in pea-sized pieces / Sprinkle in the ice water a tablespoon at a time, tossing, until the dough holds when squeezed / Press into a disc, wrap and chill for at least 1 hour before rolling. Makes one 9-inch crust"),
 ("Apple Pie", "Dessert", 40, 60, 8, "2 1/2 cups|flour; 1 tsp|salt; 1 cup|butter, cold; 1/2 cup|ice water; 7|apples, peeled and sliced; 3/4 cup|sugar; 2 tbsp|flour, for the filling; 1 tsp|cinnamon; 1/4 tsp|nutmeg; 1 tbsp|lemon juice; 1|egg, beaten",
  "Make the pastry: cut the butter into the flour and salt and add the water to form a dough; divide in two and chill / Toss the apples with sugar, 2 tbsp flour, spices and lemon juice / Roll out and line a 9-inch pie dish, fill, cover with the second crust, crimp and cut vents, then brush with egg / Bake at 425 °F (220 °C) for 20 minutes, then 375 °F (190 °C) for 40 minutes until bubbling"),
 ("Pumpkin Pie", "Dessert", 20, 55, 8, "1|pie crust, unbaked; 1 can|pumpkin puree; 3/4 cup|sugar; 1/2 tsp|salt; 1 tsp|cinnamon; 1/2 tsp|ginger, ground; 1/4 tsp|ground cloves; 2|eggs; 1 can|evaporated milk",
  "Heat the oven to 425 °F (220 °C) / Whisk the pumpkin, sugar, salt, spices and eggs, then the evaporated milk / Pour into the crust and bake for 15 minutes, then reduce to 350 °F (175 °C) for 40 to 50 minutes until a knife comes out clean near the centre / Cool for 2 hours"),
 ("Pecan Pie", "Dessert", 15, 60, 8, "1|pie crust, unbaked; 1 cup|corn syrup; 3|eggs; 1 cup|brown sugar; 2 tbsp|butter, melted; 1 tsp|vanilla extract; 1 1/2 cups|pecans",
  "Heat the oven to 350 °F (175 °C) / Whisk the syrup, eggs, sugar, butter and vanilla, stir in the pecans and pour into the crust / Bake for 55 to 60 minutes until set with a slight wobble, covering the edges with foil if they darken / Cool completely"),
 ("Peach Cobbler", "Dessert", 15, 45, 8, "6|peaches, sliced; 1/2 cup|sugar, for the fruit; 1 tsp|cinnamon; 1/2 cup|butter; 1 cup|flour; 1 cup|sugar; 2 tsp|baking powder; 1/4 tsp|salt; 1 cup|milk",
  "Heat the oven to 350 °F (175 °C) and melt the butter in a 9x13-inch dish in the oven / Toss the peaches with sugar and cinnamon / Whisk the flour, sugar, baking powder, salt and milk and pour over the melted butter without stirring / Spoon the peaches on top and bake for 40 to 45 minutes until golden"),
 ("Apple Crisp", "Dessert", 20, 45, 8, "6|apples, sliced; 1/4 cup|sugar; 1 tsp|cinnamon; 1 tbsp|lemon juice; 3/4 cup|flour; 3/4 cup|rolled oats; 3/4 cup|brown sugar; 1/2 cup|butter, cold",
  "Heat the oven to 375 °F (190 °C) / Toss the apples with the sugar, cinnamon and lemon juice in a 9-inch square dish / Rub the flour, oats, brown sugar and butter together and scatter over / Bake for 40 to 45 minutes until the fruit bubbles"),
 ("Berry Crumble Bars", "Baking", 15, 45, 16, "3 cups|flour; 1 1/2 cups|sugar, divided; 1 tsp|baking powder; 1/4 tsp|salt; 1 cup|butter, cold; 1|egg; 4 cups|mixed berries; 4 tsp|cornstarch",
  "Heat the oven to 375 °F (190 °C) and grease a 9x13-inch pan / Mix the flour, 1 cup of the sugar, the baking powder and salt, cut in the butter and stir in the egg to a crumbly dough / Press half into the pan / Toss the berries with the remaining 1/2 cup sugar and the cornstarch, spread over and crumble the rest of the dough on top / Bake for 45 minutes"),
 ("Key Lime Pie", "Dessert", 20, 25, 8, "1 1/2 cups|graham cracker crumbs; 1/3 cup|sugar; 6 tbsp|butter, melted; 1 can|sweetened condensed milk; 4|egg yolks; 4|limes, zested and juiced (1/2 cup juice); 1 cup|heavy cream",
  "Heat the oven to 350 °F (175 °C) / Press the crumbs, sugar and butter into a 9-inch pie dish and bake for 10 minutes / Whisk the condensed milk, yolks, lime juice and zest, pour in and bake for 15 minutes / Chill for 3 hours and top with whipped cream"),
 ("Cherry Hand Pies", "Baking", 30, 22, 8, "2 1/2 cups|flour; 1 tsp|salt; 1 cup|butter, cold; 1/2 cup|ice water; 2 cups|cherries, pitted; 1/3 cup|sugar; 1 tbsp|cornstarch; 1|egg, beaten",
  "Make the pastry by cutting the butter into the flour and salt and adding the water; chill 1 hour / Simmer the cherries, sugar and cornstarch until thick and cool / Roll the pastry, cut 16 rectangles, fill half with cherries, cover, crimp and brush with egg / Bake at 400 °F (200 °C) for 20 to 22 minutes"),
 ("Banana Cream Pie", "Dessert", 30, 15, 8, "1|pie crust, baked; 3/4 cup|sugar; 1/3 cup|flour; 1/4 tsp|salt; 2 cups|milk; 3|egg yolks; 2 tbsp|butter; 1 tsp|vanilla extract; 3|bananas, sliced; 1 cup|heavy cream",
  "Whisk the sugar, flour and salt in a saucepan, then the milk and yolks, and cook over medium heat, stirring, until thick and bubbling / Off the heat stir in the butter and vanilla / Layer the bananas in the crust, pour over the custard and chill for 4 hours / Top with whipped cream"),
]
for t, cat, prep, cook, serv, ings, steps in PIE:
    R(F, t, cat, "American", prep, cook, serv, "Dessert", ings, steps)
