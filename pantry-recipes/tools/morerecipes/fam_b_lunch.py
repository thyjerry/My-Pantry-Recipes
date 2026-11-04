from lib import R
F = "31-lunch.txt"

# ---------------- sandwiches and wraps ----------------
SAND = [
 ("Chicken Pesto Ciabatta", "Italian", "1|ciabatta, cut into 2 sandwich lengths and split; 200 g|cooked chicken, sliced; 3 tbsp|pesto; 1|tomato, sliced; 1 handful|arugula; 60 g|mozzarella, sliced", "Spread the rolls with pesto, then layer the chicken, mozzarella, tomato and arugula", True),
 ("Turkey Cranberry Sandwich", "American", "4 slices|whole wheat bread; 150 g|deli turkey; 2 tbsp|cranberry sauce; 2 tbsp|mayonnaise; 2 slices|swiss cheese; 1 handful|lettuce", "Spread two slices with mayonnaise and two with cranberry sauce. Fill with turkey, cheese and lettuce", False),
 ("Open-Faced Tuna Cheddar Melts", "American", "1 can|tuna, drained; 3 tbsp|mayonnaise; 1|celery stalk, finely diced; 1 tsp|lemon juice; 4 slices|bread; 80 g|cheddar, grated; 1 tbsp|butter", "Mix the tuna, mayonnaise, celery and lemon juice. Butter and toast the bread, spread with the tuna, top with cheese and grill for 3 to 4 minutes until bubbling", True),
 ("Chickpea Salad Sandwich", "American", "1 can|chickpeas, drained; 3 tbsp|vegan mayonnaise; 1 tsp|dijon mustard; 1|celery stalk, diced; 1 tbsp|dill, chopped; 1|lemon, juice only; 4 slices|whole wheat bread; 1 handful|lettuce", "Mash the chickpeas roughly with a fork and mix in the mayonnaise, mustard, celery, dill and lemon juice. Season and pile between the bread with lettuce", False),
 ("Chive and Mustard Egg Salad Rolls", "American", "6|eggs; 4 tbsp|mayonnaise; 1 tsp|dijon mustard; 1 tbsp|chives, snipped; 4|bread rolls; |salt; |black pepper", "Boil the eggs for 10 minutes, cool and peel. Chop them and mix with the mayonnaise, mustard, chives, salt and pepper. Split the rolls and fill generously", False),
 ("Caprese Baguette", "Italian", "1|baguette; 2|tomatoes, sliced; 200 g|mozzarella, sliced; 1 handful|basil; 2 tbsp|olive oil; 1 tbsp|balsamic glaze", "Split the baguette, drizzle with olive oil and layer with tomato, mozzarella and basil. Finish with balsamic glaze and cut into four", False),
 ("Ham and Swiss Panini", "Italian", "4 slices|sourdough; 150 g|ham; 4 slices|swiss cheese; 2 tsp|dijon mustard; 1 tbsp|butter", "Spread the bread with mustard and fill with ham and cheese. Butter the outsides and press in a hot pan under a heavy pan for 3 minutes per side", True),
 ("Halloumi and Roasted Pepper Wraps", "Mediterranean", "250 g|halloumi, sliced; 150 g|roasted red peppers, sliced; 1 tbsp|olive oil; 4|tortillas; 6 tbsp|hummus; 1 handful|spinach; 1|lemon", "Fry the halloumi in the oil for 2 minutes per side until golden and squeeze over a little lemon. Spread the tortillas with hummus, add spinach, peppers and halloumi and roll up", True),
 ("Buffalo Chicken Wraps", "American", "300 g|cooked chicken, shredded; 4 tbsp|buffalo sauce; 4|tortillas; 1 handful|lettuce, shredded; 1|carrot, grated; 4 tbsp|ranch dressing", "Toss the chicken in the buffalo sauce. Fill the tortillas with lettuce, carrot, chicken and a drizzle of ranch, then roll up tightly", False),
 ("Greek Chicken Pitas with Tzatziki", "Greek", "500 g|chicken breast, sliced; 1 tsp|dried oregano; 1|lemon, juice only; 1 tbsp|olive oil; 4|pita; 1|tomato, diced; 0.5|red onion, thinly sliced; 8 tbsp|tzatziki", "Toss the chicken with oregano, lemon juice and oil and fry for 8 minutes until cooked. Warm the pitas and fill with chicken, tomato, onion and tzatziki", True),
 ("Chicken Club Sandwich", "American", "6 slices|bacon; 6 slices|bread; 200 g|cooked chicken, sliced; 3 tbsp|mayonnaise; 1|tomato, sliced; 4|lettuce leaves", "Fry the bacon until crisp and toast the bread. Spread with mayonnaise and stack in three layers: chicken and lettuce, then bacon and tomato. Hold together with cocktail sticks", True),
 ("Pulled Chicken BBQ Sliders", "American", "400 g|cooked chicken, shredded; 120 ml|bbq sauce; 8|bread rolls, small; 150 g|cabbage, finely shredded; 2 tbsp|mayonnaise; 1 tsp|apple cider vinegar", "Warm the chicken in the BBQ sauce. Toss the cabbage with mayonnaise and vinegar. Pile the chicken and slaw into the split rolls", True),
 ("Lemongrass Chicken Banh Mi", "Vietnamese", "400 g|chicken thighs, thinly sliced; 1 tbsp|lemongrass paste; 1 tbsp|fish sauce; 1|carrot, cut into thin strips; 0.5|cucumber, cut into strips; 3 tbsp|rice vinegar; 1 tsp|sugar; 2|baguettes, halved; 3 tbsp|mayonnaise; 1 handful|cilantro; 1 tsp|sriracha", "Toss the carrot and cucumber with the vinegar and sugar. Fry the chicken in a little oil with the lemongrass paste for 6 minutes until cooked, then add the fish sauce. Spread the bread with mayonnaise mixed with sriracha and fill with chicken, pickles and cilantro", True),
 ("Turkey Avocado Club Wraps", "American", "4|tortillas; 200 g|deli turkey; 1|avocado, sliced; 4 slices|bacon, cooked; 1|tomato, sliced; 3 tbsp|mayonnaise; 1 handful|lettuce", "Spread the tortillas with mayonnaise, then layer with lettuce, turkey, bacon, tomato and avocado. Fold in the sides and roll up tightly", False),
 ("Portobello Cheesesteak Sandwiches", "American", "4|portobello mushrooms, sliced; 1|onion, sliced; 1|bell pepper, sliced; 2 tbsp|vegetable oil; 4|hot dog buns; 4 slices|provolone", "Fry the onion and pepper in half the oil for 6 minutes. Add the rest of the oil and the mushrooms and fry 6 minutes until browned and juicy. Lay the cheese over to melt, then pile into the buns", True),
 ("Mediterranean Tuna Wraps", "Mediterranean", "2 can|tuna, drained; 8|olives, chopped; 1|tomato, diced; 2 tbsp|olive oil; 1|lemon, juice only; 4|tortillas; 1 handful|arugula; 50 g|feta, crumbled", "Mix the tuna with the olives, tomato, oil and lemon juice. Fill the tortillas with arugula, the tuna and feta, then roll up", False),
]
SERV = {"Caprese Baguette": 4, "Halloumi and Roasted Pepper Wraps": 4, "Buffalo Chicken Wraps": 4, "Greek Chicken Pitas with Tzatziki": 4, "Pulled Chicken BBQ Sliders": 4,
        "Lemongrass Chicken Banh Mi": 4, "Portobello Cheesesteak Sandwiches": 4, "Turkey Avocado Club Wraps": 4, "Mediterranean Tuna Wraps": 4}
for t, cu, ings, how, hot in SAND:
    R(F, t, "Main", cu, 10, 10 if hot or "Egg Salad" in t else 0, SERV.get(t, 2),
      "Lunch, Dinner, Main dish" if hot else "Lunch, Main dish", ings,
      ["Get everything ready before you start: slice, wash and drain the fillings", how, "Cut in half and serve" + (" hot" if hot else " straight away, or wrap tightly for a packed lunch")])

# ---------------- grain bowls ----------------
BOWL = [
 ("Sesame Ginger Salmon Rice Bowls", "Japanese", "300 g|rice; 4|salmon fillets; 4 tbsp|soy sauce; 2 tbsp|honey; 1 tbsp|ginger, grated; 1|cucumber, sliced; 1|avocado, sliced; 150 g|edamame; 1 tbsp|sesame seeds", "Mix the soy sauce, honey and ginger, brush half over the salmon and roast at 200°C (400°F) for 12 minutes, brushing with more halfway", "the rice, cucumber, avocado and edamame", "the rest of the sauce and sesame seeds", 25),
 ("Taco-Spiced Chicken Rice Bowls", "Tex-Mex", "300 g|rice; 500 g|chicken breast, sliced; 2 tbsp|taco seasoning; 1 tbsp|vegetable oil; 1 can|black beans, drained; 200 g|sweetcorn; 150 g|salsa; 1|avocado, diced; 1|lime", "Toss the chicken with the taco seasoning and fry in the oil for 8 minutes until cooked. Warm the beans and corn together", "the rice, beans and corn", "salsa, avocado and a squeeze of lime", 25),
 ("Spiced Lamb Couscous Bowls", "Middle Eastern", "200 g|couscous; 500 g|ground lamb; 2 tsp|cumin; 1 tsp|cinnamon; 8 tbsp|hummus; 1|cucumber, diced; 200 g|cherry tomatoes, halved; 1 handful|parsley, chopped; 1|lemon; 2 tbsp|olive oil", "Pour 250 ml boiling water over the couscous, cover for 5 minutes and fluff. Fry the lamb with the cumin and cinnamon for 8 minutes until browned and crisp", "the couscous, cucumber and tomatoes", "the lamb, hummus, parsley, a squeeze of lemon and the oil", 15),
 ("Spicy Gochujang Chicken Bowls", "Korean", "300 g|rice; 600 g|chicken thighs, sliced; 2 tbsp|gochujang; 2 tbsp|soy sauce; 1 tbsp|brown sugar; 1 tbsp|sesame oil; 3 cloves|garlic, grated; 1 tsp|ginger, grated; 4|green onions, sliced; 150 g|kimchi; 4|eggs", "Fry the chicken for 8 minutes until browned and cooked, then add the gochujang, soy sauce, sugar, sesame oil, garlic and ginger and cook 2 minutes. Fry the eggs", "the rice and chicken", "the kimchi, a fried egg and green onions", 25),
 ("Quinoa Power Bowls", "American", "200 g|quinoa; 1|sweet potato, diced; 1 can|chickpeas, drained; 2 tbsp|olive oil; 1 tsp|smoked paprika; 100 g|kale, shredded; 1|avocado, sliced; 4 tbsp|tahini; 1|lemon", "Roast the sweet potato and chickpeas with the oil and paprika at 200°C (400°F) for 25 minutes. Simmer the quinoa in 450 ml water for 15 minutes", "the quinoa, kale, sweet potato and chickpeas", "avocado and the tahini thinned with lemon juice and water", 30),
 ("Peanut Tofu Noodle Bowls", "Thai", "250 g|rice noodles; 400 g|firm tofu, cubed; 1 tbsp|vegetable oil; 4 tbsp|peanut butter; 2 tbsp|tamari; 1|lime; 1 tbsp|maple syrup; 1|carrot, grated; 1|red bell pepper, sliced; 2 tbsp|peanuts, chopped", "Fry the tofu in the oil until golden. Whisk the peanut butter, tamari, lime juice, maple syrup and 4 tbsp hot water. Soak the noodles as the pack says", "the noodles, carrot, pepper and tofu", "the peanut sauce and chopped peanuts", 20),
 ("Mediterranean Farro Bowls", "Mediterranean", "200 g|farro; 1 can|chickpeas, drained; 1|cucumber, diced; 200 g|cherry tomatoes, halved; 12|olives; 100 g|feta, crumbled; 3 tbsp|olive oil; 1 tbsp|red wine vinegar; 1 tsp|dried oregano", "Simmer the farro in salted water for 25 minutes until chewy-tender, then drain. Whisk the oil, vinegar and oregano", "the farro, chickpeas, cucumber, tomatoes and olives", "the feta and dressing", 30),
 ("Shrimp Poke Bowls", "Hawaiian", "300 g|sushi rice; 400 g|cooked shrimp; 3 tbsp|soy sauce; 1 tsp|sesame oil; 1 tsp|sriracha; 1|mango, diced; 1|cucumber, diced; 1|avocado, diced; 2|green onions, sliced; 1 tbsp|sesame seeds", "Cook the rice and season with a splash of rice vinegar if you like. Toss the shrimp with the soy sauce, sesame oil and sriracha", "the rice, mango, cucumber and avocado", "the shrimp, green onions and sesame seeds", 20),
 ("Black Bean Sweet Potato Bowls", "Mexican", "300 g|brown rice; 2|sweet potatoes, diced; 2 tbsp|olive oil; 1 tsp|cumin; 1 tsp|chili powder; 1 can|black beans, drained; 150 g|salsa; 1|avocado; 1|lime; 1 handful|cilantro", "Roast the sweet potatoes with the oil, cumin and chili at 200°C (400°F) for 25 minutes. Cook the rice. Warm the beans", "the rice, beans and sweet potatoes", "salsa, sliced avocado, lime and cilantro", 30),
 ("Chicken Shawarma Bowls", "Middle Eastern", "300 g|rice; 600 g|chicken thighs, sliced; 2 tsp|cumin; 2 tsp|paprika; 1 tsp|turmeric; 0.5 tsp|cinnamon; 2 tbsp|olive oil; 2|lemons; 200 g|yogurt; 1 clove|garlic, grated; 1|cucumber, diced; 1|tomato, diced", "Toss the chicken with the spices, oil and juice of one lemon and roast at 220°C (425°F) for 20 minutes. Mix the yogurt with the garlic and a pinch of salt", "the rice, cucumber and tomato", "the chicken, garlic yogurt and lemon wedges", 25),
 ("Miso Salmon Soba Bowls", "Japanese", "250 g|soba noodles; 4|salmon fillets; 2 tbsp|miso; 1 tbsp|honey; 1 tbsp|rice vinegar; 200 g|broccoli, in florets; 1|carrot, grated; 2|green onions, sliced", "Mix the miso, honey and vinegar, spread over the salmon and roast at 200°C (400°F) for 12 minutes. Cook the soba with the broccoli for the last 3 minutes, then rinse in cold water", "the soba, broccoli and carrot", "flaked salmon and green onions", 20),
 ("Lentil and Roasted Veg Bowls", "International", "200 g|lentils; 1|eggplant, diced; 1|red onion, wedged; 1|zucchini, diced; 3 tbsp|olive oil; 1 tsp|cumin; 100 g|spinach; 4 tbsp|yogurt; 1|lemon", "Roast the eggplant, onion and zucchini with 2 tbsp oil and the cumin at 220°C (425°F) for 25 minutes. Simmer the lentils for 20 minutes, drain and toss with the rest of the oil and lemon juice", "the spinach and lentils", "the roasted vegetables and a spoon of yogurt", 30),
]
for t, cu, ings, cook, base, top, mins in BOWL:
    R(F, t, "Main", cu, 15, mins, 4, "Lunch, Dinner, Main dish", ings,
      [cook, "Divide " + base + " between four bowls", "Top with " + top + ". Leftovers keep in the fridge for 2 days, so these also make good packed lunches"])

# ---------------- quesadillas ----------------
QUE = [
 ("Chicken and Pepper Quesadillas", "300 g|cooked chicken, shredded; 1|bell pepper, sliced; 1|onion, sliced; 150 g|monterey jack, grated", "Fry the pepper and onion in a little oil for 6 minutes until soft"),
 ("Refried Bean and Jalapeno Quesadillas", "250 g|refried beans; 2 tbsp|jalapenos in jar, chopped; 1 tsp|cumin; 150 g|cheddar, grated", "Warm the refried beans with the cumin and stir in the jalapenos"),
 ("Spinach and Mushroom Quesadillas", "250 g|mushrooms, sliced; 100 g|spinach; 1 clove|garlic, chopped; 150 g|mozzarella, grated", "Fry the mushrooms for 5 minutes, add the garlic and spinach and stir until wilted"),
 ("Sweet Potato and Chipotle Quesadillas", "1|sweet potato, peeled and grated; 1 tbsp|chipotles in adobo, chopped; 150 g|pepper jack, grated", "Cook the sweet potato in a little oil for 6 minutes until soft, then stir in the chipotle"),
 ("Steak and Onion Quesadillas", "300 g|steak, thinly sliced; 1|onion, sliced; 1 tsp|chili powder; 150 g|monterey jack, grated", "Fry the onion for 5 minutes, then add the steak and chili powder and fry 2 minutes"),
 ("Kimchi Cheese Quesadillas", "150 g|kimchi, chopped; 2|green onions, sliced; 150 g|mozzarella, grated", "Squeeze excess juice from the kimchi and mix it with the green onions"),
]
for t, ings, prep in QUE:
    R(F, t, "Main", "Tex-Mex" if "Kimchi" not in t else "Korean", 10, 15, 4, "Lunch, Dinner, Main dish, Snack",
      "8|tortillas; 1 tbsp|vegetable oil; " + ings + "; 4 tbsp|sour cream; 4 tbsp|salsa",
      [prep, "Lay four tortillas flat, scatter with half the cheese, then the filling and the rest of the cheese. Top with the other tortillas",
       "Cook one at a time in a lightly oiled pan over medium heat for 2 to 3 minutes per side, pressing down, until crisp and the cheese melts",
       "Cut into wedges and serve with sour cream and salsa"])

# ---------------- flatbread pizzas ----------------
FLAT = [
 ("Four Cheese Naan Pizzas", "Italian", "4|naan; 150 ml|pizza sauce; 150 g|mozzarella, grated; 30 g|parmesan, grated; 100 g|ricotta; 60 g|goat cheese; 1 handful|basil", "spread with sauce, top with mozzarella and parmesan and dot with ricotta and goat cheese", "basil"),
 ("BBQ Chicken Flatbreads", "American", "4|flatbread; 6 tbsp|bbq sauce; 200 g|cooked chicken, shredded; 0.5|red onion, thinly sliced; 150 g|mozzarella, grated; 1 handful|cilantro", "spread with BBQ sauce and top with chicken, red onion and mozzarella", "cilantro"),
 ("Pesto Veggie Flatbreads", "Italian", "4|flatbread; 4 tbsp|pesto; 1|zucchini, thinly sliced; 150 g|cherry tomatoes, halved; 150 g|mozzarella, grated; 1 handful|arugula", "spread with pesto and top with zucchini, tomatoes and mozzarella", "arugula"),
 ("Pepperoni Pita Pizzas", "American", "4|pita; 150 ml|pizza sauce; 150 g|mozzarella, grated; 60 g|pepperoni; 1 tsp|dried oregano", "spread with sauce, top with mozzarella and pepperoni and sprinkle with oregano", "a few chili flakes if you like"),
 ("Mushroom and Goat Cheese Flatbreads", "French", "4|flatbread; 250 g|mushrooms, sliced; 1 tbsp|olive oil; 1 tsp|dried thyme; 100 g|goat cheese; 60 g|mozzarella, grated; 1 tbsp|honey", "top with the mushrooms (first fried in the oil with thyme for 5 minutes), mozzarella and crumbled goat cheese", "a drizzle of honey"),
 ("Greek Pita Pizzas", "Greek", "4|pita; 6 tbsp|hummus; 100 g|spinach; 12|olives, halved; 0.5|red onion, sliced; 100 g|feta, crumbled", "spread with hummus and top with spinach, olives, onion and feta", "a drizzle of olive oil"),
 ("Hawaiian Tortilla Pizzas", "American", "4|tortillas; 120 ml|pizza sauce; 150 g|mozzarella, grated; 120 g|ham, chopped; 150 g|pineapple, diced", "spread with sauce and top with mozzarella, ham and pineapple", "a pinch of black pepper"),
 ("Vegan Hummus Veggie Flatbreads", "Mediterranean", "4|flatbread; 8 tbsp|hummus; 1|bell pepper, sliced; 1|red onion, sliced; 150 g|cherry tomatoes, halved; 2 tbsp|olive oil; 1 tsp|za'atar; 1 handful|parsley", "spread with hummus and top with pepper, onion and tomatoes tossed in the oil", "za'atar and parsley"),
]
for t, cu, ings, top, fin in FLAT:
    R(F, t, "Main", cu, 10, 10, 4, "Lunch, Dinner, Main dish, Snack", ings,
      ["Heat the oven to 220°C (200°C fan, 425°F) and put the breads on two large baking trays",
       "Bake the plain breads for 3 minutes to crisp them, then " + top,
       "Bake for 7 to 9 minutes until the edges are crisp and the cheese, if any, is bubbling",
       "Finish with " + fin + ", cut into slices and serve"])

# ---------------- lettuce wraps and more lunches ----------------
R(F, "Chicken Lettuce Wraps", "Main", "Chinese", 10, 10, 4, "Lunch, Dinner, Main dish, Appetizer",
  "500 g|ground chicken; 1 tbsp|vegetable oil; 2 cloves|garlic, chopped; 1 tsp|ginger, grated; 1 can|water chestnuts, drained and chopped; 3 tbsp|hoisin sauce; 1 tbsp|soy sauce; 1 tsp|rice vinegar; 3|green onions, sliced; 1|lettuce, leaves separated",
  "Fry the chicken in the oil over high heat for 6 minutes, breaking it up, until browned / Add the garlic, ginger and water chestnuts and cook 1 minute / Stir in the hoisin, soy sauce and vinegar and cook 1 minute more. Add the green onions / Spoon into the lettuce cups and eat with your hands")
R(F, "Tofu Lettuce Cups", "Main", "Chinese", 10, 10, 4, "Lunch, Dinner, Main dish, Appetizer",
  "400 g|firm tofu, crumbled; 1 tbsp|vegetable oil; 150 g|shiitake mushrooms, chopped; 2 cloves|garlic, chopped; 2 tbsp|hoisin sauce; 1 tbsp|soy sauce; 1 tsp|sesame oil; 1|carrot, grated; 1|lettuce, leaves separated; 2 tbsp|peanuts, chopped",
  "Fry the tofu in the oil over high heat for 6 minutes until golden and a little crisp / Add the mushrooms and garlic and cook 3 minutes / Stir in the hoisin, soy sauce and sesame oil / Fill the lettuce cups with the tofu, grated carrot and peanuts")
R(F, "Loaded Baked Potatoes with Broccoli", "Main", "American", 5, 60, 4, "Lunch, Dinner, Main dish",
  "4|large potatoes; 1 tbsp|olive oil; 200 g|broccoli, in small florets; 100 g|cheddar, grated; 4 tbsp|sour cream; 2|green onions, sliced; |salt",
  "Heat the oven to 200°C (400°F). Prick the potatoes, rub with oil and salt and bake for 55 to 60 minutes until the skins are crisp / Steam or boil the broccoli for 4 minutes / Split the potatoes, fluff the insides with a fork and fill with broccoli and cheese. Return to the oven for 5 minutes to melt / Top with sour cream and green onions")
R(F, "Tuna Pasta Salad", "Salad", "Italian", 10, 12, 4, "Lunch, Main dish, Salad",
  "300 g|fusilli; 2 can|tuna, drained; 150 g|sweetcorn; 1|red bell pepper, diced; 4 tbsp|mayonnaise; 2 tbsp|yogurt; 1|lemon, juice only; 2|green onions, sliced",
  "Cook the pasta in salted water as the pack says, drain and rinse in cold water / Mix the mayonnaise, yogurt and lemon juice in a large bowl / Add the pasta, tuna, sweetcorn, pepper and green onions and toss. Season and chill until lunch")
R(F, "Chicken Caesar Crunch Wraps", "Main", "American", 10, 0, 4, "Lunch, Main dish",
  "300 g|cooked chicken, sliced; 1|romaine lettuce, shredded; 40 g|parmesan, shaved; 5 tbsp|caesar dressing; 4|tortillas; 40 g|croutons, crushed",
  "Toss the lettuce, chicken and parmesan with the dressing / Add the crushed croutons for crunch / Pile onto the tortillas, fold in the sides and roll up tightly. Cut in half to serve")
R(F, "Hummus and Veggie Snack Box", "Snack", "Mediterranean", 10, 0, 2, "Lunch, Snack",
  "6 tbsp|hummus; 1|carrot, cut into sticks; 0.5|cucumber, cut into sticks; 1|bell pepper, sliced; 8|cherry tomatoes; 2|pita, cut into wedges; 8|olives",
  "Divide the hummus between two small pots / Pack the vegetables, pita wedges and olives around them in two lunch boxes / Keep chilled until lunchtime")
R(F, "Cheese and Pickle Ploughman's Plate", "Main", "British", 10, 0, 2, "Lunch, Main dish",
  "150 g|cheddar, in thick slices; 4 slices|ham; 2 tbsp|relish; 1|apple, sliced; 4|pickles; 2|bread rolls; 2 tbsp|butter; 1 handful|lettuce",
  "Arrange the cheese, ham, apple, pickles and lettuce on two plates / Add a spoon of relish to each / Serve with the bread rolls and butter")
R(F, "Leftover Rice Fried Egg Bowl", "Main", "Asian", 5, 10, 2, "Lunch, Dinner, Main dish",
  "300 g|cooked rice; 2|eggs; 1 tbsp|vegetable oil; 1 tbsp|soy sauce; 1 tsp|sesame oil; 2|green onions, sliced; 1 tbsp|furikake | opt; 1 tsp|chili oil | opt",
  "Warm the rice in a pan with a splash of water, covered, for 3 minutes / Fry the eggs in the oil until crispy at the edges / Divide the rice between bowls, drizzle with soy sauce and sesame oil, top with the eggs, green onions, furikake and chili oil")
R(F, "Pita Pizza Snack Pockets", "Snack", "American", 5, 10, 4, "Lunch, Snack",
  "4|pita; 6 tbsp|pizza sauce; 120 g|mozzarella, grated; 40 g|pepperoni, chopped; 1 tsp|dried oregano",
  "Heat the oven to 200°C (400°F) / Split each pita into a pocket and fill with sauce, mozzarella, pepperoni and oregano / Bake for 8 to 10 minutes until the cheese melts and the bread is crisp. Cut in half")
R(F, "Sardines on Toast with Tomato", "Main", "Portuguese", 5, 5, 2, "Breakfast, Lunch, Main dish",
  "1 can|sardines, in olive oil; 4 slices|sourdough; 1|tomato, sliced; 0.5|lemon; 1 pinch|chili flakes; 1 tbsp|parsley, chopped",
  "Toast the bread and drizzle with a little oil from the can / Lay on the tomato slices and the sardines, mashing them lightly / Finish with lemon juice, chili flakes and parsley")
R(F, "Cucumber Cream Cheese Sandwiches", "Snack", "British", 10, 0, 4, "Lunch, Appetizer, Snack",
  "8 slices|bread; 150 g|cream cheese; 1|cucumber, very thinly sliced; 1 tbsp|dill, chopped; |salt; |black pepper",
  "Mix the cream cheese with the dill and a little pepper / Spread on all the bread, then layer cucumber over half the slices and sprinkle with salt / Close the sandwiches, cut off the crusts and cut each into four fingers")
