from lib import R
F = "42-sauces-dressings.txt"
def SA(t, cu, prep, cook, serv, ings, steps, meals="Sauce/Condiment"):
    R(F, t, "Sauce", cu, prep, cook, serv, meals, ings, steps)

# whisked dressings: title, cuisine, ingredients, note
DRS = [
 ("Lemon Tahini Dressing", "Middle Eastern", "4 tbsp|tahini; 1|lemon, juiced; 1 clove|garlic, grated; 4 tbsp|water; 1 tsp|maple syrup; |salt", "grain bowls, roasted vegetables and falafel"),
 ("Honey Mustard Vinaigrette", "American", "4 tbsp|olive oil; 2 tbsp|apple cider vinegar; 1 tbsp|dijon mustard; 1 tbsp|honey; |salt; |black pepper", "green salads and chicken"),
 ("Balsamic Vinaigrette", "Italian", "6 tbsp|olive oil; 2 tbsp|balsamic vinegar; 1 tsp|dijon mustard; 1 tsp|honey; 1 clove|garlic, grated; |salt", "tomato salads and leafy greens"),
 ("Sesame Ginger Dressing", "Japanese", "3 tbsp|rice vinegar; 2 tbsp|soy sauce; 1 tbsp|sesame oil; 2 tbsp|vegetable oil; 1 tbsp|honey; 1 tbsp|ginger, grated", "slaws, noodle salads and cold soba"),
 ("Creamy Caesar Dressing", "Italian-American", "120 g|mayonnaise; 30 g|parmesan, grated; 1 tbsp|lemon juice; 1 tsp|dijon mustard; 1 tsp|worcestershire sauce; 1 clove|garlic, grated; 2|anchovy fillets, mashed|opt", "romaine and croutons"),
 ("Greek Lemon Oregano Dressing", "Greek", "6 tbsp|olive oil; 2 tbsp|red wine vinegar; 1 tbsp|lemon juice; 1 tsp|dried oregano; 1 clove|garlic, grated; |salt", "Greek salads and grilled chicken"),
 ("Cilantro Lime Dressing", "Mexican", "1 handful|cilantro; 2|limes, juiced; 100 g|greek yogurt; 1 clove|garlic; 2 tbsp|olive oil; 1 tsp|honey; |salt", "taco salads and burrito bowls (blend it)"),
 ("Miso Ginger Dressing", "Japanese", "2 tbsp|miso; 2 tbsp|rice vinegar; 1 tbsp|ginger, grated; 1 tbsp|honey; 3 tbsp|vegetable oil; 1 tsp|sesame oil", "salads, roasted squash and salmon"),
 ("Blue Cheese Dressing", "American", "80 g|blue cheese, crumbled; 100 g|sour cream; 60 g|mayonnaise; 1 tbsp|lemon juice; |black pepper", "wedge salads and wings"),
 ("Green Goddess Dressing", "American", "150 g|greek yogurt; 1 handful|parsley; 1 handful|basil; 2 tbsp|chives; 1|lemon, juiced; 1 clove|garlic; 2 tbsp|olive oil; |salt", "salads and as a dip (blend it)"),
 ("Peanut Lime Dressing", "Thai", "3 tbsp|peanut butter; 2|limes, juiced; 1 tbsp|soy sauce; 1 tbsp|honey; 1 tsp|sriracha; 3 tbsp|water", "noodle salads and slaws"),
 ("Maple Dijon Dressing", "Canadian", "5 tbsp|olive oil; 2 tbsp|apple cider vinegar; 1 tbsp|maple syrup; 1 tbsp|dijon mustard; |salt", "kale and apple salads"),
]
for t, cu, ings, use in DRS:
    blend = "blend it" in use
    SA(t, cu, 5, 0, 6, ings, ["Put everything in a jar or bowl" + (" (or a blender)" if blend else ""),
      ("Blend until smooth and pale green" if blend else "Shake or whisk until smooth and emulsified, adding a splash of water if it is too thick"),
      "Taste and adjust the salt, acid and sweetness. Keeps in the fridge for up to 5 days; good on " + use.replace(" (blend it)", "")])

# salsas, relishes and chutneys
SA("Mango Salsa", "Mexican", 10, 0, 6, "1|mango, diced; 0.5|red onion, finely chopped; 1|jalapeno, finely chopped; 1 handful|cilantro, chopped; 1|lime, juiced; |salt",
  "Combine everything in a bowl and season with salt / Rest 10 minutes so the flavours mix. Great with fish tacos, grilled chicken or chips", "Appetizer, Sauce/Condiment")
SA("Salsa Verde (Tomatillo)", "Mexican", 10, 10, 8, "500 g|tomatillos, husked; 0.5|onion; 2 cloves|garlic; 1|jalapeno; 1 handful|cilantro; 1|lime, juiced; |salt",
  "Broil or char the tomatillos, onion, garlic and jalapeno until blackened in spots, about 8 to 10 minutes / Blend with the cilantro, lime and salt until chunky-smooth / Cool before serving", "Appetizer, Sauce/Condiment")
SA("Charred Corn Salsa", "Tex-Mex", 10, 8, 6, "300 g|sweetcorn; 1|red bell pepper, diced; 0.5|red onion, diced; 1|jalapeno, diced; 1|lime, juiced; 1 handful|cilantro; 1 tbsp|olive oil; |salt",
  "Char the corn in the oil in a hot pan for 6 to 8 minutes, then cool / Mix with everything else and season", "Appetizer, Sauce/Condiment")
SA("Pineapple Salsa", "Caribbean", 10, 0, 6, "300 g|pineapple, diced; 0.5|red onion, finely chopped; 1|red chili, finely chopped; 1 handful|cilantro; 1|lime, juiced; |salt",
  "Mix everything and season / Serve with pork, fish, tacos or chips")
SA("Tomato Chutney", "Indian", 10, 40, 10, "800 g|tomatoes, chopped; 1|onion, chopped; 2 cloves|garlic; 1 tbsp|ginger, grated; 150 g|brown sugar; 150 ml|apple cider vinegar; 1 tsp|mustard seeds; 0.5 tsp|chili flakes; |salt",
  "Put everything in a heavy pan and bring to a boil, stirring until the sugar dissolves / Simmer uncovered 35 to 40 minutes, stirring more often near the end, until thick and jammy / Spoon into clean jars and keep in the fridge for up to 3 weeks")
SA("Quick Pickled Red Onions", "Mexican", 5, 2, 10, "2|red onions, thinly sliced; 250 ml|apple cider vinegar; 120 ml|water; 1 tbsp|sugar; 1 tsp|salt",
  "Pack the onions into a jar / Heat the vinegar, water, sugar and salt until dissolved and pour over / Cool, then chill at least 30 minutes; keeps 2 weeks")
SA("Cranberry Sauce", "American", 2, 12, 8, "340 g|cranberries; 150 g|sugar; 1|orange, zest and juice; 60 ml|water",
  "Bring everything to a boil, then simmer 10 minutes until most berries burst / Cool; it thickens as it sets")
SA("Cucumber Raita", "Indian", 10, 0, 6, "300 g|yogurt; 0.5|cucumber, grated and squeezed; 0.5 tsp|cumin, toasted; 1 tbsp|mint, chopped; |salt",
  "Stir everything together and chill / Serve with curries, biryani and kebabs")
SA("Apple Chutney", "British", 10, 40, 10, "4|apples, peeled and chopped; 1|onion, chopped; 80 g|raisins; 150 g|brown sugar; 150 ml|apple cider vinegar; 1 tsp|ground ginger; 0.5 tsp|cinnamon; |salt",
  "Combine everything in a heavy pan and bring to a boil / Simmer 35 to 40 minutes, stirring often, until thick / Cool and store in the fridge for up to 3 weeks; good with cheese and cold meats")

# pestos, cooked sauces and condiments
SA("Sun-Dried Tomato Pesto", "Italian", 10, 0, 8, "100 g|sun-dried tomatoes in oil, drained; 30 g|almonds; 30 g|parmesan, grated; 1 clove|garlic; 1 handful|basil; 80 ml|olive oil",
  "Blitz everything except the oil in a food processor / With the motor running, pour in the oil to make a thick paste / Season; keeps a week in the fridge under a layer of oil")
SA("Arugula Walnut Pesto", "Italian", 10, 0, 8, "80 g|arugula; 50 g|walnuts; 40 g|parmesan, grated; 1 clove|garlic; 1|lemon, juiced; 100 ml|olive oil; |salt",
  "Blitz the arugula, walnuts, parmesan, garlic and lemon juice / Stream in the oil and season")
SA("Vegan Cashew Pesto", "Italian", 10, 0, 8, "2 large handfuls|basil; 60 g|cashews; 2 tbsp|nutritional yeast; 1 clove|garlic; 1|lemon, juiced; 100 ml|olive oil; |salt",
  "Blitz everything except the oil until finely chopped / Stream in the oil and season")
SA("Romesco Sauce", "Spanish", 10, 5, 8, "1 jar|roasted red peppers, drained; 60 g|almonds, toasted; 1 slice|bread, toasted; 2 cloves|garlic; 1 tsp|smoked paprika; 2 tbsp|sherry vinegar; 80 ml|olive oil; |salt",
  "Blend everything except the oil until coarse / Add the oil and blend to a thick, slightly textured sauce / Season; excellent with grilled vegetables, fish and potatoes")
SA("Alfredo Sauce", "Italian-American", 2, 8, 4, "60 g|butter; 2 cloves|garlic, minced; 300 ml|heavy cream; 80 g|parmesan, grated; 1 pinch|nutmeg; |black pepper",
  "Melt the butter and cook the garlic gently for 1 minute / Add the cream and simmer 4 minutes / Off the heat stir in the parmesan until smooth, and season with nutmeg and pepper")
SA("Creamy Tomato Pasta Sauce", "Italian-American", 5, 15, 4, "2 tbsp|olive oil; 1|shallot, finely chopped; 2 cloves|garlic; 3 tbsp|tomato paste; 1 can|canned tomatoes; 150 ml|heavy cream; 0.5 tsp|chili flakes; 30 g|parmesan, grated",
  "Soften the shallot and garlic in the oil / Cook the tomato paste and chili for 2 minutes until darkened / Add the tomatoes and simmer 10 minutes, then stir in the cream and parmesan")
SA("Enchilada Sauce from Scratch", "Tex-Mex", 5, 12, 6, "2 tbsp|vegetable oil; 2 tbsp|flour; 3 tbsp|chili powder; 1 tsp|cumin; 0.5 tsp|garlic powder; 2 tbsp|tomato paste; 500 ml|vegetable stock; |salt",
  "Cook the flour in the oil for 1 minute, then stir in the spices and tomato paste / Whisk in the stock gradually / Simmer 8 to 10 minutes until it coats a spoon")
SA("Hollandaise Sauce", "French", 5, 5, 4, "3|egg yolks; 1 tbsp|lemon juice; 150 g|butter, melted and hot; |salt; 1 pinch|cayenne",
  "Whisk the yolks and lemon juice in a bowl over barely simmering water until thick and pale / Take off the heat and whisk in the hot butter in a thin stream / Season with salt and cayenne and use straight away (eggs benedict, asparagus, fish)")
SA("Garlic Aioli", "Spanish", 5, 0, 6, "150 g|mayonnaise; 2 cloves|garlic, grated; 1 tbsp|lemon juice; 1 tbsp|olive oil; |salt",
  "Stir everything together / Rest 15 minutes for the garlic to mellow. Serve with fries, sandwiches and seafood", "Appetizer, Sauce/Condiment")
SA("Sweet Chili Sauce", "Thai", 5, 10, 8, "150 ml|rice vinegar; 150 g|sugar; 100 ml|water; 3 cloves|garlic, minced; 2|red chilies, finely chopped; 1 tbsp|cornstarch; 1 tsp|salt",
  "Simmer the vinegar, sugar, water, garlic, chilies and salt for 5 minutes / Stir in the cornstarch mixed with 2 tbsp water and simmer until thickened / Cool; keeps 2 weeks in the fridge")
SA("Chipotle Lime Crema", "Mexican", 5, 0, 6, "150 g|sour cream; 1|chipotle pepper in adobo, minced; 1|lime, juiced; |salt",
  "Stir everything together and chill / Drizzle over tacos, bowls and grilled corn")
SA("Spicy Peanut Chili Crisp", "Chinese", 10, 10, 10, "150 ml|vegetable oil; 4|shallots, thinly sliced; 6 cloves|garlic, thinly sliced; 3 tbsp|chili flakes; 40 g|peanuts, chopped; 1 tsp|sugar; 1 tsp|salt; 1 tbsp|soy sauce",
  "Fry the shallots in the oil over medium heat until golden, then the garlic until just golden (watch closely) / Pour the hot oil and aromatics over the chili flakes, peanuts, sugar and salt in a heatproof bowl / Stir in the soy sauce and cool. Keeps a month in the fridge")
SA("Mushroom Gravy", "American", 5, 15, 6, "2 tbsp|butter; 250 g|mushrooms, finely chopped; 1|shallot, chopped; 2 tbsp|flour; 400 ml|vegetable stock; 1 tbsp|soy sauce; 1 tsp|dried thyme",
  "Cook the mushrooms and shallot in the butter for 8 minutes until browned / Stir in the flour for 1 minute / Gradually whisk in the stock, soy sauce and thyme and simmer until thick")
SA("Ginger Scallion Sauce", "Chinese", 10, 2, 6, "6|green onions, finely sliced; 2 tbsp|ginger, finely grated; 80 ml|vegetable oil; 1 tsp|salt; 1 tsp|soy sauce",
  "Put the green onions, ginger and salt in a heatproof bowl / Heat the oil until shimmering and pour over to sizzle / Stir in the soy sauce. Spoon over rice, noodles, chicken or tofu")
SA("Quick Tartar Sauce", "British", 5, 0, 6, "150 g|mayonnaise; 2 tbsp|pickles, finely chopped; 1 tbsp|capers, chopped; 1 tbsp|lemon juice; 1 tbsp|parsley, chopped",
  "Stir everything together / Serve with fish, fish fingers and crab cakes")
SA("Buffalo Wing Sauce", "American", 2, 3, 6, "120 ml|hot sauce; 80 g|butter; 1 tbsp|honey; 0.5 tsp|garlic powder",
  "Melt the butter with the hot sauce, honey and garlic powder, whisking until smooth / Toss with wings, cauliflower or chicken tenders")
SA("Strawberry Chia Jam", "International", 5, 10, 10, "400 g|strawberries, chopped; 2 tbsp|maple syrup; 2 tbsp|chia seeds; 1 tbsp|lemon juice",
  "Cook the strawberries for 8 to 10 minutes until broken down, mashing them / Stir in the maple syrup, lemon and chia and cool / It sets in the fridge in 1 hour and keeps a week", "Breakfast, Sauce/Condiment")
