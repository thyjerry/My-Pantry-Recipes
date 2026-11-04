from lib import R
F = "57-stir-fries-curries.txt"
P = "58-pasta-noodles.txt"
D = "Lunch, Dinner, Main dish"
# ---- stir-fries: title, cuisine, protein, protein words, vegetables, sauce, base
SF = [
 ("Ginger Pork with Snow Peas", "Chinese", "400 g|pork tenderloin, thinly sliced", "pork", "200 g|snow peas; 1|red bell pepper, sliced", "3 tbsp|soy sauce; 1 tbsp|rice vinegar; 1 tbsp|honey; 2 tbsp|ginger, grated", "long-grain rice"),
 ("Black Pepper Beef with Onions", "Chinese", "450 g|flank steak, thinly sliced", "beef", "2|onions, cut in wedges; 1|green bell pepper, sliced", "3 tbsp|soy sauce; 1 tbsp|oyster sauce; 2 tsp|black pepper, coarsely ground; 1 tsp|sugar", "long-grain rice"),
 ("Cashew Chicken", "Chinese", "500 g|chicken breasts, cubed", "chicken", "1|red bell pepper, diced; 1|zucchini, diced; 80 g|cashews", "3 tbsp|soy sauce; 1 tbsp|hoisin sauce; 1 tbsp|rice vinegar; 1 tsp|sugar", "jasmine rice"),
 ("Garlic Shrimp with Broccoli", "Chinese", "500 g|shrimp, peeled", "shrimp", "400 g|broccoli, in florets; 6 cloves|garlic, sliced", "2 tbsp|soy sauce; 1 tbsp|oyster sauce; 1 tbsp|shaoxing wine; 1 tsp|sesame oil", "jasmine rice"),
 ("Thai Basil Chicken (Pad Kra Pao)", "Thai", "500 g|ground chicken", "chicken", "4|thai chilies, sliced; 1|onion, sliced; 1 large handful|thai basil", "1 tbsp|oyster sauce; 1 tbsp|fish sauce; 1 tsp|soy sauce; 1 tsp|sugar", "jasmine rice"),
 ("Sweet and Sour Tofu", "Chinese", "400 g|firm tofu, pressed and cubed", "tofu", "1|red bell pepper, chunked; 1|onion, chunked; 200 g|pineapple, chunked", "3 tbsp|ketchup; 2 tbsp|rice vinegar; 2 tbsp|sugar; 1 tbsp|soy sauce", "long-grain rice"),
 ("Kung Pao Tofu", "Chinese", "400 g|firm tofu, pressed and cubed", "tofu", "1|red bell pepper, diced; 6|dried red chilies; 60 g|peanuts; 3|green onions", "2 tbsp|soy sauce; 1 tbsp|black vinegar; 1 tbsp|sugar; 1 tsp|sichuan peppercorns, ground", "long-grain rice"),
 ("Mongolian-Style Beef", "Chinese", "450 g|flank steak, thinly sliced", "beef", "6|green onions, cut in lengths; 3 cloves|garlic", "4 tbsp|soy sauce; 3 tbsp|brown sugar; 1 tbsp|ginger, grated", "long-grain rice"),
 ("Lemongrass Chicken Stir-Fry", "Vietnamese", "500 g|chicken thighs, boneless, sliced", "chicken", "2 stalks|lemongrass, finely chopped; 1|onion, sliced; 1|red chili", "2 tbsp|fish sauce; 1 tbsp|sugar; 1 tbsp|soy sauce", "jasmine rice"),
 ("Korean Spicy Pork Stir-Fry", "Korean", "450 g|pork shoulder, thinly sliced", "pork", "1|onion, sliced; 2|green onions; 150 g|cabbage, sliced", "2 tbsp|gochujang; 1 tbsp|soy sauce; 1 tbsp|sugar; 1 tbsp|sesame oil; 2 cloves|garlic", "short-grain rice"),
 ("Teriyaki Salmon Stir-Fry", "Japanese", "500 g|salmon fillets, skinned and cubed", "salmon", "200 g|snap peas; 1|carrot, sliced thinly", "3 tbsp|soy sauce; 2 tbsp|mirin; 1 tbsp|honey", "short-grain rice"),
 ("Tempeh and Green Bean Stir-Fry", "Indonesian", "250 g|tempeh, sliced", "tempeh", "300 g|green beans, cut; 2 cloves|garlic; 1|red chili", "2 tbsp|kecap manis; 1 tbsp|soy sauce; 1|lime", "jasmine rice"),
 ("Orange Chicken Stir-Fry", "Chinese", "500 g|chicken thighs, boneless, cubed", "chicken", "1|red bell pepper, diced; 3|green onions", "1|orange, zested and juiced; 2 tbsp|soy sauce; 2 tbsp|honey; 1 tbsp|rice vinegar", "long-grain rice"),
 ("Chili Garlic Eggplant", "Chinese", "2|eggplants, cut in batons", "eggplant", "4 cloves|garlic; 2|green onions; 1|red chili", "2 tbsp|soy sauce; 1 tbsp|black vinegar; 1 tbsp|chili garlic sauce; 1 tsp|sugar", "jasmine rice"),
 ("Bok Choy and Mushroom Stir-Fry", "Chinese", "300 g|shiitake mushrooms, sliced", "mushrooms", "400 g|bok choy, halved; 2 cloves|garlic", "2 tbsp|soy sauce; 1 tbsp|oyster sauce; 1 tsp|sesame oil", "long-grain rice"),
 ("Szechuan Green Beans with Pork", "Chinese", "200 g|ground pork", "pork", "450 g|green beans, trimmed; 3 cloves|garlic; 1 tbsp|ginger", "2 tbsp|soy sauce; 1 tbsp|chili bean paste; 1 tsp|sugar", "long-grain rice"),
 ("Thai Cashew Shrimp", "Thai", "400 g|shrimp, peeled", "shrimp", "1|onion, sliced; 1|red bell pepper, sliced; 60 g|cashews; 4|dried red chilies", "1 tbsp|oyster sauce; 1 tbsp|fish sauce; 1 tsp|sugar", "jasmine rice"),
 ("Hoisin Beef with Broccoli and Peppers", "Chinese", "450 g|sirloin steak, thinly sliced", "beef", "300 g|broccoli, in florets; 1|yellow bell pepper, sliced", "3 tbsp|hoisin sauce; 1 tbsp|soy sauce; 1 tbsp|rice vinegar", "long-grain rice"),
 ("Malaysian Sambal Prawns", "Malaysian", "500 g|shrimp, peeled", "shrimp", "1|onion, sliced; 2 cloves|garlic", "2 tbsp|sambal oelek; 1 tbsp|tamarind paste; 1 tbsp|brown sugar", "jasmine rice"),
 ("Filipino Pork Giniling", "Filipino", "500 g|ground pork", "pork", "1|onion, chopped; 2|potatoes, small dice; 2|carrots, small dice; 100 g|peas, frozen; 60 g|raisins", "120 ml|tomato sauce; 2 tbsp|soy sauce; 1 tbsp|fish sauce", "long-grain rice"),
]
for t, cu, prot, pw, veg, sauce, base in SF:
    R(F, t, "Main", cu, 15, 12, 4, D,
      "2 tbsp|vegetable oil; " + prot + "; 1 tbsp|cornstarch; " + veg + "; " + sauce + "; 300 g|" + base + ", cooked",
      ["Toss the %s with the cornstarch and a pinch of salt. Mix the sauce ingredients with 3 tbsp water" % pw,
       "Heat a wok or large frying pan over high heat until smoking, add 1 tbsp oil and stir-fry the %s in a single layer for 3 to 4 minutes until browned and just cooked, then tip out" % pw,
       "Add the rest of the oil and stir-fry the vegetables for 2 to 3 minutes until crisp-tender",
       "Return the %s, pour in the sauce and toss for 1 minute until glossy" % pw,
       "Serve straight away with hot rice"])

# ---- stovetop curries: title, cuisine, protein, aromatics/paste, liquid, veg, simmer, finish
CU = [
 ("Sri Lankan Chicken Curry", "Sri Lankan", "700 g|chicken thighs, boneless, cubed", "2 tbsp|curry powder; 1 tsp|turmeric; 1|cinnamon stick; 10|curry leaves", "1 can|coconut milk", "2|tomatoes, chopped", 25, "a squeeze of lime"),
 ("Goan Fish Curry", "Indian", "600 g|white fish fillets, in chunks", "1 tbsp|ginger; 3 cloves|garlic; 2 tsp|kashmiri chili powder; 1 tsp|turmeric; 1 tsp|cumin", "1 can|coconut milk", "1 tbsp|tamarind paste", 10, "cilantro"),
 ("Kerala Egg Curry", "Indian", "8|eggs, hard-boiled and peeled", "1 tsp|mustard seeds; 10|curry leaves; 1 tbsp|ginger; 2 tsp|coriander; 1 tsp|turmeric; 1 tsp|garam masala", "1 can|coconut milk", "2|tomatoes, chopped", 12, "curry leaves fried in a little oil"),
 ("Thai Red Curry with Pumpkin", "Thai", "500 g|pumpkin, cubed", "3 tbsp|red curry paste", "1 can|coconut milk", "150 g|green beans; 1|red bell pepper", 18, "thai basil and lime"),
 ("Thai Yellow Chicken Curry", "Thai", "600 g|chicken thighs, boneless, sliced", "3 tbsp|yellow curry paste", "1 can|coconut milk", "2|potatoes, cubed; 1|onion", 20, "a squeeze of lime"),
 ("Panang Beef Curry", "Thai", "500 g|sirloin steak, thinly sliced", "3 tbsp|panang curry paste; 2 tbsp|peanut butter", "1 can|coconut milk", "1|red bell pepper; 4|kaffir lime leaves", 10, "thai basil"),
 ("Malaysian Chicken Curry", "Malaysian", "800 g|chicken thighs, bone-in, skinless", "3 tbsp|malaysian curry powder; 1 stalk|lemongrass; 1|cinnamon stick; 2|star anise", "1 can|coconut milk", "2|potatoes, cubed", 30, "fried shallots"),
 ("Saag Paneer", "Indian", "250 g|paneer, cubed", "1 tsp|cumin seeds; 3 cloves|garlic; 1 tbsp|ginger; 1|green chili; 1 tsp|garam masala", "120 ml|heavy cream", "500 g|spinach, blanched and chopped", 10, "a squeeze of lemon"),
 ("Matar Paneer", "Indian", "250 g|paneer, cubed", "1 tsp|cumin seeds; 1|onion, chopped; 3 cloves|garlic; 1 tbsp|ginger; 1 tsp|turmeric; 2 tsp|garam masala", "250 ml|water", "250 g|peas, frozen; 1 can|canned tomatoes", 15, "cilantro"),
 ("Chicken Vindaloo", "Indian", "700 g|chicken thighs, boneless, cubed", "4 cloves|garlic; 1 tbsp|ginger; 2 tbsp|vindaloo paste; 2 tbsp|white vinegar; 1 tsp|sugar", "250 ml|water", "1|onion, chopped; 2|potatoes, cubed", 30, "cilantro"),
 ("Burmese-Style Chicken Curry", "Burmese", "700 g|chicken thighs, boneless, cubed", "2|onions, blended; 4 cloves|garlic; 1 tbsp|ginger; 1 tsp|turmeric; 1 tsp|paprika", "250 ml|chicken stock", "1 tbsp|fish sauce", 30, "cilantro"),
 ("Cape Malay Vegetable Curry", "South African", "2|sweet potatoes, cubed", "2 tbsp|curry powder; 1 tsp|turmeric; 1 tsp|cinnamon; 2 tbsp|apricot jam", "500 ml|vegetable stock", "1 can|chickpeas, drained; 150 g|green beans", 25, "cilantro"),
 ("Trinidad Curry Chickpeas and Potatoes", "Trinidadian", "2 can|chickpeas, drained", "2 tbsp|curry powder; 1 tsp|cumin; 3 cloves|garlic; 1|scotch bonnet pepper, whole", "500 ml|water", "2|potatoes, cubed; 1|onion", 25, "culantro or cilantro"),
 ("Japanese Vegetable Curry", "Japanese", "2|potatoes, chunked", "100 g|japanese curry roux", "700 ml|water", "2|carrots, chunked; 1|onion, sliced; 1|eggplant, cubed", 20, "pickled ginger"),
 ("Coconut Shrimp Curry", "Indian", "500 g|shrimp, peeled", "1 tsp|mustard seeds; 3 cloves|garlic; 1 tbsp|ginger; 2 tsp|curry powder; 1 tsp|turmeric", "1 can|coconut milk", "2|tomatoes, chopped", 8, "cilantro and lime"),
 ("Chickpea and Spinach Coconut Curry", "Indian", "2 can|chickpeas, drained", "1|onion, chopped; 3 cloves|garlic; 1 tbsp|ginger; 2 tbsp|curry powder", "1 can|coconut milk", "150 g|spinach; 1 can|canned tomatoes", 15, "lime"),
 ("Thai Green Vegetable Curry", "Thai", "400 g|firm tofu, cubed", "3 tbsp|green curry paste", "1 can|coconut milk", "1|zucchini; 150 g|snow peas; 1|red bell pepper", 10, "thai basil and lime"),
 ("Lamb Keema with Peas", "Indian", "500 g|ground lamb", "1|onion, chopped; 3 cloves|garlic; 1 tbsp|ginger; 2 tsp|garam masala; 1 tsp|cumin; 1 tsp|chili powder", "250 ml|water", "200 g|peas, frozen; 1 can|canned tomatoes", 20, "cilantro"),
]
for t, cu, prot, aro, liq, veg, mins, fin in CU:
    R(F, t, "Main", cu, 15, mins + 12, 4, D,
      "2 tbsp|vegetable oil; " + prot + "; " + aro + "; " + liq + "; " + veg + "; |salt; 300 g|basmati rice, cooked",
      ["Heat the oil in a deep pan over medium heat and fry the aromatics and spices for 1 to 2 minutes until fragrant (cook any onion for 5 minutes first)",
       "Add the main ingredient and stir to coat; brown meat for 3 to 4 minutes, but add fish, shrimp, eggs or paneer later as noted below",
       "Pour in the liquid and the vegetables, season with salt and simmer for about %d minutes until everything is tender (fish and shrimp need only the last 5 to 8 minutes)" % mins,
       "Taste for salt and finish with " + fin + ". Serve with rice"])

# ---- pasta: title, cuisine, pasta, sauce ings, method words, finish
PA = [
 ("Pasta alla Norma", "Italian", "400 g|rigatoni", "2|eggplants, cubed; 4 tbsp|olive oil; 3 cloves|garlic; 1 can|canned tomatoes; 1 handful|basil; 60 g|ricotta salata", "Fry the eggplant in the oil until golden, add the garlic, then the tomatoes and simmer for 10 minutes", "basil and grated ricotta salata"),
 ("Cacio e Pepe", "Italian", "400 g|spaghetti", "100 g|pecorino, finely grated; 2 tsp|black pepper, coarsely ground; 2 tbsp|butter", "Toast the pepper in a dry pan, add the butter and a ladle of pasta water", "the pecorino, tossing hard off the heat with splashes of pasta water until creamy"),
 ("Pasta e Ceci", "Italian", "250 g|ditalini pasta", "3 tbsp|olive oil; 3 cloves|garlic; 1 sprig|rosemary; 2 tbsp|tomato paste; 2 can|chickpeas, drained; 1 l|vegetable stock", "Fry the garlic and rosemary in the oil, stir in the tomato paste, add the chickpeas and stock and simmer 10 minutes, mashing some; cook the pasta in it", "a drizzle of olive oil and black pepper"),
 ("Orecchiette with Broccoli Rabe and Sausage", "Italian", "400 g|orecchiette", "300 g|Italian sausage, casings removed; 400 g|broccoli rabe, chopped; 3 cloves|garlic; 0.5 tsp|red pepper flakes; 3 tbsp|olive oil; 40 g|pecorino", "Brown the sausage in the oil, add the garlic and pepper flakes, then the blanched broccoli rabe", "pecorino"),
 ("Spaghetti Aglio e Olio", "Italian", "400 g|spaghetti", "6 tbsp|olive oil; 6 cloves|garlic, thinly sliced; 0.5 tsp|red pepper flakes; 1 handful|parsley", "Warm the oil with the garlic over low heat until pale golden, then add the pepper flakes", "parsley and a ladle of pasta water"),
 ("Pasta Puttanesca", "Italian", "400 g|spaghetti", "3 tbsp|olive oil; 3 cloves|garlic; 4|anchovy fillets; 1 can|canned tomatoes; 80 g|black olives; 2 tbsp|capers; 0.5 tsp|red pepper flakes", "Melt the anchovies in the oil with the garlic, add the tomatoes, olives, capers and pepper flakes and simmer for 10 minutes", "parsley"),
 ("Fettuccine Alfredo", "Italian", "400 g|fettuccine", "4 tbsp|butter; 240 ml|heavy cream; 80 g|parmesan, grated; 0.25 tsp|nutmeg", "Melt the butter with the cream and simmer for 3 minutes", "the parmesan and nutmeg, tossing until silky"),
 ("Lemon Ricotta Pasta", "Italian", "400 g|penne", "250 g|ricotta; 1|lemon, zested and juiced; 40 g|parmesan; 2 tbsp|olive oil; 1 handful|basil", "Mix the ricotta, lemon, parmesan and oil with a ladle of pasta water to a sauce", "basil and black pepper"),
 ("Pesto Trofie with Potatoes and Green Beans", "Italian", "400 g|trofie pasta", "2|potatoes, small dice; 150 g|green beans, cut; 120 g|basil pesto", "Cook the potatoes and beans in the pasta water, adding them before the pasta", "the pesto and a splash of pasta water"),
 ("Creamy Mushroom Pasta", "Italian", "400 g|tagliatelle", "3 tbsp|butter; 400 g|mushrooms, sliced; 2 cloves|garlic; 1 tsp|dried thyme; 150 ml|heavy cream; 40 g|parmesan", "Brown the mushrooms in the butter, add the garlic and thyme, then the cream and simmer 2 minutes", "parmesan and parsley"),
 ("Spinach and Ricotta Stuffed Shells", "Italian", "250 g|jumbo pasta shells", "500 g|ricotta; 200 g|spinach, cooked and squeezed; 1|egg; 60 g|parmesan; 700 g|marinara sauce; 150 g|mozzarella", "Mix the ricotta, spinach, egg and half the parmesan, fill the cooked shells and set them on marinara in a dish, then top with mozzarella and bake at 190 °C for 25 minutes", "the remaining parmesan"),
 ("Baked Ziti", "Italian", "450 g|ziti", "450 g|ground beef; 1|onion, chopped; 700 g|marinara sauce; 250 g|ricotta; 200 g|mozzarella, grated; 40 g|parmesan", "Brown the beef with the onion and stir in the marinara; toss with the undercooked ziti, layer with ricotta and mozzarella and bake at 190 °C for 25 minutes", "parmesan"),
 ("Sesame Peanut Noodles", "Chinese", "350 g|noodles, wheat", "4 tbsp|peanut butter; 3 tbsp|soy sauce; 1 tbsp|rice vinegar; 1 tbsp|sesame oil; 1 tbsp|honey; 1 tsp|chili oil; 1|cucumber, julienned", "Whisk the peanut butter, soy, vinegar, sesame oil, honey and chili oil with 4 tbsp hot water", "cucumber and sesame seeds"),
 ("Japchae", "Korean", "250 g|sweet potato glass noodles", "200 g|beef sirloin, thinly sliced; 1|carrot, julienned; 1|onion, sliced; 150 g|spinach; 150 g|shiitake mushrooms, sliced; 4 tbsp|soy sauce; 2 tbsp|sugar; 2 tbsp|sesame oil", "Stir-fry the beef, then each vegetable in turn in a little oil; toss everything with the drained noodles, soy, sugar and sesame oil", "sesame seeds"),
 ("Singapore Noodles", "Singaporean", "250 g|rice vermicelli", "200 g|shrimp, peeled; 150 g|char siu pork, sliced; 2|eggs; 1|red bell pepper, sliced; 100 g|bean sprouts; 2 tsp|curry powder; 2 tbsp|soy sauce", "Scramble the eggs and set aside; stir-fry the shrimp and pork, add the pepper and curry powder, then toss in the soaked noodles, soy and sprouts", "green onions"),
 ("Yaki Udon", "Japanese", "400 g|udon noodles", "200 g|pork belly, thinly sliced; 150 g|cabbage, sliced; 1|carrot, julienned; 100 g|shiitake mushrooms; 3 tbsp|soy sauce; 1 tbsp|mirin; 1 tbsp|oyster sauce", "Stir-fry the pork until crisp, add the vegetables for 2 minutes, then the loosened udon and sauces", "bonito flakes or sesame seeds"),
 ("Dan Dan Noodles", "Chinese", "350 g|noodles, wheat", "250 g|ground pork; 2 tbsp|chili oil; 2 tbsp|sesame paste; 2 tbsp|soy sauce; 1 tbsp|black vinegar; 1 tsp|sichuan peppercorns, ground; 2|green onions; 150 g|bok choy", "Fry the pork until crisp with 1 tbsp soy; whisk the sesame paste, chili oil, rest of the soy, vinegar and pepper in bowls with a little noodle water", "the noodles, blanched bok choy, pork and green onions"),
 ("Vegan Pad See Ew", "Thai", "400 g|wide rice noodles", "250 g|firm tofu, cubed; 300 g|chinese broccoli, chopped; 3 cloves|garlic; 3 tbsp|soy sauce, ideally 2 tbsp of it dark soy; 1 tbsp|sugar; 1 tbsp|rice vinegar", "Fry the tofu until golden, add the garlic and broccoli stems, then the noodles, sauces and sugar, and let the noodles char a little", "white pepper"),
 ("Garlic Butter Shrimp Linguine", "Italian", "400 g|linguine", "400 g|shrimp, peeled; 4 tbsp|butter; 5 cloves|garlic; 120 ml|dry white wine; 1|lemon; 0.25 tsp|red pepper flakes; 1 handful|parsley", "Cook the shrimp in half the butter for 2 minutes, add the garlic and pepper flakes, then the wine and simmer for 2 minutes", "the rest of the butter, lemon juice and parsley"),
 ("One-Pot Tomato Basil Pasta", "Italian", "400 g|linguine", "1 can|canned tomatoes; 1|onion, thinly sliced; 4 cloves|garlic, sliced; 1 handful|basil; 3 tbsp|olive oil; 1 l|water; 40 g|parmesan", "Put the raw pasta, tomatoes, onion, garlic, half the basil, oil, water and 2 tsp salt in one wide pot and boil, stirring often, for 9 to 10 minutes until the pasta is tender and the liquid has become a sauce", "the rest of the basil and parmesan"),
 ("One-Pot Chicken Orzo", "Greek", "300 g|orzo", "500 g|chicken thighs, boneless, cubed; 1|onion, chopped; 3 cloves|garlic; 1 tsp|dried oregano; 750 ml|chicken stock; 1|lemon; 100 g|spinach; 60 g|feta", "In one wide pot, brown the chicken, then the onion, garlic and oregano; stir in the orzo and stock and simmer, stirring, for 10 minutes until creamy", "lemon juice, spinach and feta"),
 ("Gluten-Free Pasta Primavera", "Italian", "350 g|gluten-free pasta", "3 tbsp|olive oil; 1|zucchini, sliced; 150 g|asparagus, cut; 150 g|peas, frozen; 150 g|cherry tomatoes, halved; 2 cloves|garlic; 1|lemon; 40 g|parmesan", "Saute the vegetables in the oil for 4 minutes with the garlic. Cook gluten-free pasta for the shortest time on the pack and rinse briefly in hot water so it doesn't stick", "lemon zest and parmesan"),
]
for t, cu, pasta, sauce, how, fin in PA:
    one = t.startswith("One-Pot")
    noodle = any(w in pasta for w in ("noodle", "udon", "vermicelli"))
    baked = "bake at" in how
    if one: steps = [how]
    elif noodle: steps = ["Cook or soak the noodles according to the packet, then drain, rinse briefly and toss with a few drops of oil so they don't stick", how, "Toss well over the heat (or in the bowls) and finish with " + fin]
    elif baked: steps = ["Heat the oven to 190 °C (375 °F). Cook the pasta in well-salted boiling water for 2 minutes less than the packet says and drain", how, "Finish with " + fin + " and rest 5 minutes before serving"]
    else: steps = ["Bring a large pot of well-salted water to the boil and cook the pasta until al dente, keeping a cup of the cooking water before draining", how, "Toss the drained pasta with the sauce and finish with " + fin]
    if one: steps.append("Finish with " + fin + " and serve straight from the pot")
    R(P, t, "Main", cu, 10, 20, 4, D, pasta + "; " + sauce + "; |salt", steps)
