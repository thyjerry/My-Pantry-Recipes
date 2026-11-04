from lib import R
F = "60-bowls-salads.txt"
T = "61-tacos-wraps-sheetpan.txt"
D = "Lunch, Dinner, Main dish"
# ---- grain bowls (vegan or easy to make vegan): title, cuisine, grain, roast veg, protein, dressing, toppings
BW = [
 ("Roasted Sweet Potato and Black Bean Bowl", "Mexican", "200 g|quinoa", "2|sweet potatoes, cubed; 1 tsp|smoked paprika", "1 can|black beans, drained", "1|lime; 3 tbsp|olive oil; 1 tsp|cumin", "1|avocado; 1 handful|cilantro; 2 tbsp|pumpkin seeds"),
 ("Falafel Bowl with Tahini", "Middle Eastern", "200 g|bulgur wheat", "1|cucumber, diced; 200 g|cherry tomatoes, halved", "12|falafel, cooked", "4 tbsp|tahini; 1|lemon; 1 clove|garlic", "1 handful|parsley; 4 tbsp|pickled red onions; 2 tbsp|hummus"),
 ("Teriyaki Tofu Rice Bowl", "Japanese", "200 g|brown rice", "1|broccoli, in florets; 1|carrot, julienned", "400 g|firm tofu, cubed", "3 tbsp|soy sauce; 2 tbsp|maple syrup; 1 tbsp|rice vinegar; 1 tsp|ginger, grated", "2|green onions; 1 tsp|sesame seeds; 1|avocado"),
 ("Mediterranean Farro Bowl", "Mediterranean", "200 g|farro", "1|zucchini, cubed; 1|red bell pepper, chunked; 1|red onion, wedged", "1 can|chickpeas, drained", "3 tbsp|olive oil; 1|lemon; 1 tsp|dried oregano", "60 g|kalamata olives; 80 g|feta, opt; 1 handful|parsley"),
 ("Peanut Tempeh Noodle-Free Bowl", "Indonesian", "200 g|brown rice", "1|red cabbage, shredded; 1|carrot, grated; 1|cucumber, sliced", "250 g|tempeh, sliced", "3 tbsp|peanut butter; 2 tbsp|soy sauce; 1|lime; 1 tbsp|maple syrup", "2 tbsp|peanuts; 1 handful|cilantro"),
 ("Harissa Cauliflower and Lentil Bowl", "North African", "200 g|green lentils", "1|cauliflower, in florets; 2 tbsp|harissa", "100 g|spinach", "4 tbsp|yogurt, or vegan yogurt; 1|lemon; 1 tbsp|mint", "2 tbsp|almonds, toasted; 2 tbsp|pomegranate seeds"),
 ("Korean-Style Tofu Bibimbap Bowl", "Korean", "200 g|short-grain rice", "1|zucchini, julienned; 1|carrot, julienned; 150 g|shiitake mushrooms", "400 g|firm tofu, cubed", "2 tbsp|gochujang; 1 tbsp|sesame oil; 1 tbsp|rice vinegar; 1 tbsp|maple syrup", "150 g|kimchi; 1 tsp|sesame seeds"),
 ("Burrito Bowl with Cilantro Lime Rice", "Mexican", "200 g|long-grain rice", "1|red bell pepper, sliced; 1|onion, sliced; 150 g|sweetcorn", "1 can|pinto beans, drained", "2|limes; 1 handful|cilantro; 2 tbsp|olive oil", "1|avocado; 150 g|salsa; 1 handful|lettuce, shredded"),
 ("Moroccan Couscous Bowl", "Moroccan", "200 g|couscous", "2|carrots, sliced; 1|eggplant, cubed; 1 tsp|ras el hanout", "1 can|chickpeas, drained", "3 tbsp|olive oil; 1|orange; 1 tsp|cumin", "40 g|dried apricots, chopped; 2 tbsp|almonds; 1 handful|mint"),
 ("Thai Peanut Quinoa Bowl", "Thai", "200 g|quinoa", "1|red bell pepper, sliced; 150 g|edamame; 1|carrot, grated", "150 g|red cabbage, shredded", "3 tbsp|peanut butter; 1 tbsp|soy sauce; 1|lime; 1 tsp|chili garlic sauce", "2 tbsp|peanuts; 1 handful|cilantro; 1 handful|thai basil"),
 ("Greek Chicken Rice Bowl", "Greek", "200 g|long-grain rice", "1|cucumber, diced; 200 g|cherry tomatoes; 1|red onion, sliced", "500 g|chicken thighs, boneless", "150 g|tzatziki; 1|lemon; 1 tsp|dried oregano", "60 g|kalamata olives; 80 g|feta"),
 ("Salmon Sushi Bowl", "Japanese", "200 g|short-grain rice", "1|cucumber, diced; 1|avocado; 1|carrot, julienned", "300 g|salmon fillet, roasted and flaked", "2 tbsp|soy sauce; 1 tbsp|rice vinegar; 1 tbsp|mayonnaise; 1 tsp|sriracha", "1 sheet|nori, in strips; 1 tsp|sesame seeds; 2 tbsp|pickled ginger"),
 ("Cajun Shrimp and Corn Bowl", "Cajun", "200 g|long-grain rice", "200 g|sweetcorn; 1|green bell pepper, diced", "400 g|shrimp, peeled; 2 tsp|cajun seasoning", "2 tbsp|butter; 1|lemon; 2 cloves|garlic", "2|green onions; 1|avocado"),
 ("Buddha Bowl with Miso Dressing", "Japanese", "200 g|brown rice", "1|sweet potato, cubed; 1|broccoli, in florets; 150 g|red cabbage, shredded", "150 g|edamame", "2 tbsp|white miso; 1 tbsp|rice vinegar; 1 tbsp|maple syrup; 1 tbsp|sesame oil", "1|avocado; 1 tsp|sesame seeds"),
 ("Indian Chana Chaat Bowl", "Indian", "200 g|basmati rice", "1|cucumber, diced; 2|tomatoes, diced; 1|red onion, finely chopped", "2 can|chickpeas, drained", "1|lemon; 1 tsp|chaat masala; 2 tbsp|tamarind chutney", "1 handful|cilantro; 4 tbsp|yogurt; 2 tbsp|sev, opt"),
 ("Beet and Goat Cheese Grain Bowl", "American", "200 g|farro", "3|beets, roasted and diced; 100 g|arugula", "60 g|walnuts, toasted", "3 tbsp|olive oil; 1 tbsp|balsamic vinegar; 1 tsp|honey", "100 g|goat cheese; 1 handful|dill"),
]
for t, cu, grain, veg, prot, dress, top in BW:
    R(F, t, "Main", cu, 20, 30, 4, D, grain + "; " + veg + "; " + prot + "; " + dress + "; " + top + "; 2 tbsp|olive oil; |salt",
      ["Cook the grain in salted water according to the packet and drain or fluff",
       "Heat the oven to 220 °C (425 °F). Toss the vegetables that need cooking with the oil, any spice and salt and roast for 20 to 25 minutes, turning once (keep cucumber, cabbage, leaves and fresh tomatoes raw)",
       "Cook or warm the protein: pan-fry tofu, tempeh, chicken or shrimp until golden and cooked through; just rinse and drain canned beans",
       "Whisk the dressing ingredients with a splash of water and salt to taste",
       "Build the bowls: grain first, then the vegetables and protein, a spoonful of dressing and the toppings"])

# ---- salads: title, cuisine, base, add, dressing, meals
SA = [
 ("Shaved Fennel and Orange Salad", "Italian", "2|fennel bulbs, shaved; 2|oranges, segmented", "60 g|black olives; 1 handful|mint", "3 tbsp|olive oil; 1 tbsp|white wine vinegar", "Lunch, Salad, Side dish"),
 ("Watermelon Feta Mint Salad", "Mediterranean", "800 g|watermelon, cubed; 1|cucumber, sliced", "150 g|feta; 1 handful|mint; 1|red onion, thinly sliced", "2 tbsp|olive oil; 1|lime", "Lunch, Salad, Side dish"),
 ("Kale Caesar with Chickpea Croutons", "American", "300 g|kale, shredded and massaged", "1 can|chickpeas, roasted until crisp; 40 g|parmesan", "4 tbsp|mayonnaise; 1|lemon; 1 tsp|dijon mustard; 1 clove|garlic; 2|anchovy fillets, opt", "Lunch, Salad"),
 ("Black Bean and Corn Salad", "Mexican", "2 can|black beans, drained; 300 g|sweetcorn", "1|red bell pepper, diced; 1|red onion, diced; 1|avocado; 1 handful|cilantro", "3 tbsp|olive oil; 2|limes; 1 tsp|cumin", "Lunch, Salad, Side dish"),
 ("Asian Sesame Slaw", "Asian", "300 g|napa cabbage, shredded; 150 g|red cabbage, shredded; 1|carrot, julienned", "3|green onions; 2 tbsp|sesame seeds; 50 g|cashews", "2 tbsp|rice vinegar; 1 tbsp|soy sauce; 1 tbsp|sesame oil; 1 tbsp|honey", "Lunch, Salad, Side dish"),
 ("Lentil Salad with Roasted Peppers", "French", "250 g|green lentils, cooked; 2|red bell peppers, roasted and sliced", "1|red onion, finely chopped; 100 g|goat cheese; 1 handful|parsley", "3 tbsp|olive oil; 1 tbsp|red wine vinegar; 1 tsp|dijon mustard", "Lunch, Salad"),
 ("Cucumber Tomato Salad", "Mediterranean", "2|cucumbers, diced; 4|tomatoes, diced", "1|red onion, thinly sliced; 1 handful|parsley", "3 tbsp|olive oil; 1|lemon", "Lunch, Salad, Side dish"),
 ("Chopped Italian Salad", "Italian-American", "1|romaine lettuce, chopped; 100 g|salami, diced; 100 g|provolone, diced", "1 can|chickpeas, drained; 100 g|pepperoncini; 150 g|cherry tomatoes", "4 tbsp|olive oil; 2 tbsp|red wine vinegar; 1 tsp|dried oregano", "Lunch, Salad"),
 ("Spinach Strawberry Salad", "American", "200 g|spinach; 250 g|strawberries, sliced", "60 g|pecans, toasted; 80 g|goat cheese", "3 tbsp|olive oil; 1 tbsp|balsamic vinegar; 1 tsp|honey", "Lunch, Salad, Side dish"),
 ("Thai Cucumber Salad", "Thai", "2|cucumbers, sliced; 1|red onion, thinly sliced", "2 tbsp|peanuts, chopped; 1 handful|cilantro; 1|red chili", "3 tbsp|rice vinegar; 1 tbsp|sugar; 1 tbsp|fish sauce", "Lunch, Salad, Side dish"),
 ("Roasted Beet and Citrus Salad", "American", "4|beets, roasted and sliced; 2|oranges, sliced", "100 g|arugula; 60 g|pistachios", "3 tbsp|olive oil; 1 tbsp|sherry vinegar", "Lunch, Salad, Side dish"),
 ("Wild Rice Cranberry Salad", "American", "200 g|wild rice, cooked; 60 g|dried cranberries", "60 g|pecans; 3|green onions; 2 stalks|celery, diced", "3 tbsp|olive oil; 1|orange; 1 tbsp|apple cider vinegar", "Lunch, Salad, Side dish"),
 ("Southwest Chicken Salad", "Southwestern US", "1|romaine lettuce, chopped; 400 g|chicken, cooked and sliced", "1 can|black beans, drained; 150 g|sweetcorn; 1|avocado; 1 handful|tortilla chips", "4 tbsp|sour cream; 2 tbsp|salsa; 1|lime", "Lunch, Dinner, Salad"),
 ("Cobb Salad", "American", "1|romaine lettuce, chopped; 300 g|chicken, cooked and diced", "4 slices|bacon, cooked and crumbled; 2|eggs, hard-boiled; 1|avocado; 100 g|blue cheese; 2|tomatoes, diced", "3 tbsp|olive oil; 1 tbsp|red wine vinegar; 1 tsp|dijon mustard", "Lunch, Dinner, Salad"),
 ("Quinoa Tabbouleh", "Middle Eastern", "200 g|quinoa, cooked and cooled; 2 large handfuls|parsley, finely chopped", "1 handful|mint; 3|tomatoes, diced; 1|cucumber, diced; 3|green onions", "4 tbsp|olive oil; 2|lemons", "Lunch, Salad, Side dish"),
 ("Japanese Seaweed Cucumber Salad", "Japanese", "10 g|dried wakame; 2|cucumbers, thinly sliced", "1 tbsp|sesame seeds", "3 tbsp|rice vinegar; 1 tbsp|soy sauce; 1 tbsp|sugar; 1 tsp|sesame oil", "Lunch, Salad, Side dish"),
 ("Waldorf Salad", "American", "3|apples, diced; 3 stalks|celery, sliced", "80 g|walnuts, toasted; 100 g|grapes, halved; 1|lettuce", "4 tbsp|mayonnaise; 2 tbsp|yogurt; 1|lemon", "Lunch, Salad"),
 ("Peach Burrata Salad", "Italian", "3|peaches, sliced; 2 balls|burrata", "100 g|arugula; 1 handful|basil; 40 g|prosciutto, opt", "3 tbsp|olive oil; 1 tbsp|balsamic vinegar", "Lunch, Salad, Appetizer"),
 ("Broccoli Crunch Salad", "American", "500 g|broccoli, finely chopped; 1|red onion, finely chopped", "60 g|dried cranberries; 60 g|sunflower seeds; 4 slices|bacon, cooked, opt", "4 tbsp|mayonnaise; 1 tbsp|apple cider vinegar; 1 tbsp|honey", "Lunch, Salad, Side dish"),
 ("Warm Potato Salad with Mustard Dressing", "German", "1 kg|baby potatoes, boiled and halved", "1|red onion, finely chopped; 2 tbsp|chives; 2 tbsp|capers", "4 tbsp|olive oil; 2 tbsp|white wine vinegar; 1 tbsp|whole grain mustard", "Lunch, Salad, Side dish"),
 ("Edamame Corn Salad", "Asian", "300 g|edamame, shelled; 200 g|sweetcorn", "1|red bell pepper, diced; 3|green onions; 1 handful|cilantro", "2 tbsp|rice vinegar; 1 tbsp|soy sauce; 1 tbsp|sesame oil", "Lunch, Salad, Side dish"),
 ("Chickpea Avocado Smash Salad", "American", "2 can|chickpeas, drained; 1|avocado", "1|red onion, finely chopped; 2 stalks|celery, diced; 1 handful|dill", "1|lemon; 1 tsp|dijon mustard; 1 tbsp|olive oil", "Lunch, Salad"),
 ("Grilled Halloumi Salad", "Cypriot", "250 g|halloumi, sliced and grilled; 150 g|mixed salad greens", "200 g|cherry tomatoes; 1|cucumber, diced; 2 tbsp|pomegranate seeds", "3 tbsp|olive oil; 1|lemon; 1 tsp|honey", "Lunch, Salad"),
 ("Soba Noodle Salad", "Japanese", "200 g|soba noodles, cooked and rinsed cold; 150 g|edamame", "1|carrot, julienned; 1|cucumber, julienned; 3|green onions", "3 tbsp|soy sauce; 1 tbsp|rice vinegar; 1 tbsp|sesame oil; 1 tsp|honey", "Lunch, Salad"),
]
for t, cu, base, add, dress, meals in SA:
    R(F, t, "Salad", cu, 15, 0, 4, meals, base + "; " + add + "; " + dress + "; |salt; |black pepper",
      ["Prepare the main ingredients (cook and cool any grains, beans, noodles or potatoes first) and put them in a large bowl",
       "Whisk the dressing ingredients together with salt and pepper",
       "Add the remaining ingredients, pour over the dressing and toss gently",
       "Taste for seasoning and serve; dress leafy salads just before eating"])

# ---- tacos: title, cuisine, filling, cooking, toppings
TA = [
 ("Baja Fish Tacos", "Mexican", "500 g|white fish fillets; 1 tsp|chili powder; 1 tsp|cumin", "Season the fish and pan-fry in 2 tbsp oil for 3 minutes per side, then flake", "200 g|cabbage, shredded; 4 tbsp|sour cream; 1 tbsp|sriracha; 1|lime"),
 ("Carne Asada Tacos", "Mexican", "600 g|skirt steak; 2|limes; 3 cloves|garlic; 1 tsp|cumin", "Marinate the steak in the lime juice, garlic, cumin and salt for 1 hour, then sear over high heat for 3 to 4 minutes per side and slice thinly", "1|onion, finely chopped; 1 handful|cilantro; 150 g|salsa verde"),
 ("Chicken Tinga Tacos", "Mexican", "500 g|chicken, cooked and shredded; 2|chipotle peppers in adobo; 1|onion, sliced; 1 can|canned tomatoes", "Cook the onion in 1 tbsp oil for 5 minutes, blend the tomatoes with the chipotles and simmer with the onion for 10 minutes, then stir in the chicken", "120 g|sour cream; 1|avocado; 60 g|cotija cheese"),
 ("Black Bean and Sweet Potato Tacos", "Mexican", "2|sweet potatoes, diced; 1 can|black beans, drained; 1 tsp|smoked paprika; 1 tsp|cumin", "Roast the sweet potato with oil and spices at 220 °C for 25 minutes, then toss with the warmed beans", "1|avocado; 4 tbsp|pickled red onions; 1 handful|cilantro"),
 ("Shrimp Tacos with Mango Salsa", "Mexican", "500 g|shrimp, peeled; 1 tsp|chili powder; 2 cloves|garlic", "Saute the shrimp with the garlic and chili powder in 1 tbsp oil for 3 minutes", "1|mango, diced; 1|red onion, finely chopped; 1|jalapeno, minced; 1|lime"),
 ("Al Pastor-Style Pork Tacos", "Mexican", "700 g|pork shoulder, thinly sliced; 2 tbsp|achiote paste; 120 ml|pineapple juice; 2 cloves|garlic", "Marinate the pork in the achiote, juice and garlic overnight, then sear in batches in a hot pan until charred", "200 g|pineapple, diced and charred; 1|onion, chopped; 1 handful|cilantro"),
 ("Mushroom Walnut Tacos", "Mexican", "500 g|mushrooms, finely chopped; 100 g|walnuts, chopped; 2 tsp|chili powder; 1 tsp|cumin", "Cook the mushrooms in 2 tbsp oil until dry and browned, about 10 minutes, then add the walnuts and spices", "1|avocado; 150 g|salsa; 1 handful|cilantro"),
 ("Breakfast Tacos", "Tex-Mex", "6|eggs; 200 g|chorizo, crumbled; 1|potato, small dice", "Fry the chorizo and potato until crisp, then scramble in the eggs", "100 g|cheddar, grated; 150 g|salsa; 1|avocado"),
 ("Cauliflower Tacos with Chipotle Crema", "Mexican", "1|cauliflower, in small florets; 1 tsp|smoked paprika; 1 tsp|cumin", "Roast the cauliflower with oil and spices at 220 °C for 25 minutes", "4 tbsp|sour cream; 1|chipotle pepper in adobo; 150 g|red cabbage, shredded; 1|lime"),
 ("Korean Beef Tacos", "Korean-Mexican", "500 g|ground beef; 2 tbsp|gochujang; 2 tbsp|soy sauce; 1 tbsp|brown sugar; 2 cloves|garlic", "Brown the beef, then stir in the gochujang, soy, sugar and garlic and cook 3 minutes", "150 g|kimchi, chopped; 3|green onions; 1 tbsp|sesame seeds"),
]
for t, cu, fill, cook, top in TA:
    R(T, t, "Main", cu, 20, 20, 4, D, fill + "; " + top + "; 2 tbsp|vegetable oil; 12|corn tortillas; |salt",
      [cook, "Warm the tortillas in a dry pan for 30 seconds per side and keep them wrapped in a towel",
       "Fill the tortillas with the hot filling and add the toppings", "Serve straight away with extra lime or salsa"])

# ---- sheet-pan dinners: title, cuisine, protein, veg, seasoning, minutes
SP = [
 ("Sheet-Pan Lemon Herb Chicken and Potatoes", "Mediterranean", "8|chicken thighs, bone-in, skin-on", "800 g|baby potatoes, halved; 1|red onion, wedged", "1|lemon; 3 cloves|garlic; 2 tsp|dried oregano", 40),
 ("Sheet-Pan Gnocchi with Cherry Tomatoes", "Italian", "500 g|potato gnocchi", "400 g|cherry tomatoes; 1|red onion, wedged; 1|zucchini, chunked", "3 cloves|garlic; 1 tsp|italian seasoning; 125 g|mozzarella, torn", 25),
 ("Sheet-Pan Honey Mustard Salmon", "American", "4|salmon fillets", "400 g|asparagus; 500 g|baby potatoes, halved", "2 tbsp|honey; 2 tbsp|dijon mustard; 1|lemon", 25),
 ("Sheet-Pan Chickpea Shawarma", "Middle Eastern", "2 can|chickpeas, drained and dried", "1|cauliflower, in florets; 1|red onion, wedged", "2 tbsp|shawarma spice; 1|lemon; 4 tbsp|tahini sauce", 30),
 ("Sheet-Pan Sausage and Sweet Potato", "American", "500 g|pork sausages", "2|sweet potatoes, cubed; 400 g|Brussels sprouts, halved", "1 tsp|smoked paprika; 1 tbsp|maple syrup", 30),
 ("Sheet-Pan Steak Fajitas", "Tex-Mex", "600 g|flank steak, in strips", "3|bell peppers, sliced; 1|onion, sliced", "2 tsp|chili powder; 1 tsp|cumin; 1|lime; 8|flour tortillas", 15),
 ("Sheet-Pan Teriyaki Chicken and Broccoli", "Japanese", "700 g|chicken thighs, boneless", "1|broccoli, in florets; 1|red bell pepper, chunked", "4 tbsp|teriyaki sauce; 1 tsp|sesame seeds", 25),
 ("Sheet-Pan Pork Chops with Apples", "American", "4|pork chops, bone-in", "3|apples, wedged; 1|red onion, wedged", "1 tsp|dried thyme; 1 tbsp|brown sugar; 1 tbsp|dijon mustard", 25),
 ("Sheet-Pan Mediterranean Tofu", "Mediterranean", "400 g|firm tofu, cubed", "1|zucchini, chunked; 1|red bell pepper, chunked; 200 g|cherry tomatoes", "1 tsp|dried oregano; 60 g|kalamata olives; 1|lemon", 30),
 ("Sheet-Pan Shrimp Boil", "Southern US", "600 g|shrimp, peeled", "500 g|baby potatoes, halved; 3 ears|corn on the cob, in thirds; 250 g|smoked sausage, sliced", "2 tbsp|old bay seasoning; 3 tbsp|butter, melted; 1|lemon", 30),
 ("Sheet-Pan Harissa Chicken with Carrots", "North African", "8|chicken drumsticks", "600 g|carrots, halved lengthways; 1 can|chickpeas, drained", "3 tbsp|harissa; 1|lemon; 4 tbsp|yogurt", 40),
 ("Sheet-Pan Eggs with Peppers and Potatoes", "American", "6|eggs", "500 g|potatoes, diced; 2|bell peppers, diced; 1|onion, diced", "1 tsp|smoked paprika; 60 g|cheddar, grated", 35),
]
for t, cu, prot, veg, season, mins in SP:
    eggs = "eggs" in prot
    R(T, t, "Main", cu, 15, mins + 5, 4, D, prot + "; " + veg + "; " + season + "; 3 tbsp|olive oil; |salt; |black pepper",
      ["Heat the oven to 220 °C (425 °F) and line a large sheet pan with parchment",
       "Toss the vegetables with half the oil, salt and pepper and spread them out in one layer; roast for 15 minutes first if they are potatoes or root vegetables",
       ("Make 6 wells in the vegetables, crack in the eggs and bake for 8 to 10 minutes until the whites are set" if eggs else
        "Toss the protein with the rest of the oil and the seasoning, add it to the pan and roast for about %d minutes, turning halfway, until cooked through (chicken to 74 °C / 165 °F)" % (mins - 10 if mins > 20 else mins)),
       "Finish with any lemon, sauce, cheese or herbs from the list and serve straight from the pan"])
