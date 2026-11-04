from lib import R
F = "33-mains-sheetpan-slowcooker.txt"

# ---------------- sheet-pan dinners ----------------
# title, cuisine, protein, vegetables, seasoning, veg minutes, protein minutes, finish ingredient, finish words
SP = [
 ("Sheet Pan Pesto Chicken and Vegetables", "Italian", "4|chicken breasts", "1|zucchini, in chunks; 1|red bell pepper, in chunks; 1|red onion, wedged; 200 g|cherry tomatoes", "4 tbsp|pesto", "half the pesto", 10, 22, "30 g|parmesan, grated", "spoon over the rest of the pesto and scatter with parmesan"),
 ("Sheet Pan Honey Mustard Salmon and Asparagus", "American", "4|salmon fillets", "400 g|asparagus; 500 g|baby potatoes, halved", "2 tbsp|dijon mustard; 2 tbsp|honey", "the mustard mixed with the honey", 20, 12, "1|lemon", "squeeze over the lemon"),
 ("Sheet Pan Sausage with Fennel and Grapes", "Italian", "8|italian sausages", "2|fennel bulbs, in wedges; 1|red onion, wedged; 250 g|grapes", "1 tsp|fennel seeds; 1 tbsp|balsamic vinegar", "the fennel seeds and balsamic vinegar", 10, 25, "1 handful|parsley, chopped", "scatter with parsley"),
 ("Sheet Pan Shrimp Fajitas", "Tex-Mex", "500 g|shrimp, peeled", "2|bell peppers, sliced; 1|onion, sliced", "2 tbsp|fajita seasoning", "the fajita seasoning", 12, 6, "8|tortillas; 1|lime; 4 tbsp|sour cream", "warm the tortillas and serve with lime wedges and sour cream"),
 ("Sheet Pan Gnocchi with Tomatoes and Zucchini", "Italian", "500 g|gnocchi", "2|zucchini, diced; 300 g|cherry tomatoes; 1|red onion, wedged", "1 tsp|italian seasoning; 3 cloves|garlic, sliced", "the Italian seasoning and garlic", 0, 22, "125 g|mozzarella, torn; 1 handful|basil", "dot with mozzarella, return for 3 minutes and finish with basil"),
 ("Sheet Pan Chickpea Shawarma with Cauliflower", "Middle Eastern", "2 can|chickpeas, drained and patted dry", "1|cauliflower, in small florets; 1|red onion, wedged", "2 tsp|cumin; 2 tsp|paprika; 1 tsp|turmeric; 0.5 tsp|cinnamon", "the cumin, paprika, turmeric and cinnamon", 0, 30, "4 tbsp|tahini; 1|lemon; 4|pita", "drizzle with tahini thinned with lemon juice and serve with warm pita"),
 ("Sheet Pan Pork Tenderloin with Sweet Potatoes", "American", "600 g|pork tenderloin", "2|sweet potatoes, in wedges; 1|red onion, wedged", "1 tsp|smoked paprika; 1 tsp|dried thyme; 1 tbsp|maple syrup", "the paprika, thyme and maple syrup", 15, 20, "1 tbsp|apple cider vinegar", "rest the pork 5 minutes, slice and splash the vegetables with the vinegar"),
 ("Sheet Pan Harissa Chicken with Carrots", "North African", "8|chicken thighs, bone in", "500 g|carrots, halved lengthways; 1 can|chickpeas, drained", "3 tbsp|harissa; 1 tsp|cumin", "the harissa and cumin", 0, 40, "4 tbsp|yogurt; 1 handful|mint", "serve with yogurt and mint"),
 ("Sheet Pan Miso White Fish and Bok Choy", "Japanese", "4|white fish fillets", "4|bok choy, halved; 200 g|shiitake mushrooms", "2 tbsp|miso; 1 tbsp|mirin; 1 tbsp|soy sauce", "the miso mixed with the mirin and soy sauce", 5, 12, "2|green onions, sliced; 1 tsp|sesame seeds", "scatter with green onions and sesame seeds and serve with rice"),
 ("Sheet Pan Steak Bites with Potatoes", "American", "600 g|steak, in 3 cm cubes", "700 g|baby potatoes, halved; 200 g|green beans", "3 cloves|garlic, chopped; 1 tsp|dried rosemary; 2 tbsp|butter, melted", "the garlic, rosemary and butter", 25, 8, "1 handful|parsley, chopped", "scatter with parsley"),
 ("Sheet Pan Tandoori Paneer and Peppers", "Indian", "400 g|paneer, cubed", "2|bell peppers, in chunks; 1|red onion, in chunks", "3 tbsp|tandoori paste; 4 tbsp|yogurt", "the tandoori paste mixed with the yogurt", 0, 20, "1|lemon; 4|naan", "squeeze over lemon and serve with warm naan"),
 ("Sheet Pan Balsamic Chicken with Brussels Sprouts", "American", "8|chicken thighs, boneless", "500 g|brussels sprouts, halved; 1|red onion, wedged", "3 tbsp|balsamic vinegar; 1 tbsp|honey; 1 tsp|dried thyme", "the balsamic vinegar, honey and thyme", 0, 30, "30 g|parmesan, shaved", "scatter with parmesan"),
 ("Sheet Pan Gochujang Tofu and Sweet Potato", "Korean", "400 g|firm tofu, cubed and patted dry", "2|sweet potatoes, cubed; 200 g|broccoli, in florets", "2 tbsp|gochujang; 1 tbsp|maple syrup; 1 tbsp|soy sauce", "the gochujang, maple syrup and soy sauce", 15, 20, "2|green onions, sliced; 1 tbsp|sesame seeds", "scatter with green onions and sesame seeds"),
 ("Sheet Pan Mediterranean Salmon with Olives", "Mediterranean", "4|salmon fillets", "300 g|cherry tomatoes; 1|red onion, wedged; 80 g|olives; 1 can|white beans, drained", "1 tsp|dried oregano; 1|lemon, zest only", "the oregano and lemon zest", 10, 12, "50 g|feta, crumbled", "scatter with feta"),
 ("Sheet Pan Smoked Sausage with Potatoes and Peppers", "American", "400 g|smoked sausage, sliced", "700 g|potatoes, in small chunks; 2|bell peppers, in chunks; 1|onion, wedged", "1 tsp|smoked paprika; 1 tsp|garlic powder", "the paprika and garlic powder", 20, 15, "2 tbsp|dijon mustard", "serve with mustard"),
 ("Sheet Pan Cajun Chicken and Corn", "Cajun", "4|chicken breasts", "4|corn on the cob, halved; 2|bell peppers, in chunks; 1|red onion, wedged", "2 tbsp|cajun seasoning", "the Cajun seasoning", 10, 22, "2 tbsp|butter; 1|lime", "rub the corn with butter and squeeze over lime"),
]
for t, cu, prot, veg, sea, sea_w, vmin, pmin, fin_i, fin_w in SP:
    pw = prot.split("|")[1].split(",")[0]
    steps = ["Heat the oven to 220°C (200°C fan, 425°F) and line a large baking tray"]
    if vmin:
        steps.append("Toss the vegetables with 2 tbsp oil, salt and pepper on the tray and roast for " + str(vmin) + " minutes")
        steps.append("Rub the " + pw + " with " + sea_w + " and a little oil, then nestle it among the vegetables. Roast " + str(pmin) + " minutes more, until it is cooked through and the vegetables are tender")
    else:
        steps.append("Toss the vegetables and the " + pw + " with 2 tbsp oil, " + sea_w + ", salt and pepper, and spread out on the tray in one layer")
        steps.append("Roast for " + str(pmin) + " minutes, turning once, until golden and cooked through")
    steps.append("To finish, " + fin_w)
    R(F, t, "Main", cu, 15, vmin + pmin, 4, "Dinner, Main dish", prot + "; " + veg + "; " + sea + "; 3 tbsp|olive oil; |salt; |black pepper; " + fin_i, steps)

# ---------------- slow cooker ----------------
# title, cuisine, ingredients, prep step or "", what goes in, low hours, finish
SC = [
 ("Slow Cooker Beef Ragu with Rigatoni", "Italian", "1 kg|beef chuck, in large pieces; 1|onion, chopped; 2|carrots, diced; 3 cloves|garlic; 2 can|canned tomatoes; 2 tbsp|tomato paste; 150 ml|red wine; 1 tsp|dried oregano; 400 g|rigatoni; 40 g|parmesan, grated",
  "Season the beef and brown it all over in a hot pan with a little oil", "the beef, onion, carrots, garlic, tomatoes, tomato paste, wine and oregano", 8,
  "Shred the beef with two forks in the sauce. Cook the rigatoni, toss with the ragu and serve with parmesan. Leftover ragu freezes well"),
 ("Slow Cooker Chicken and Mushroom Stew", "French", "8|chicken thighs, boneless; 300 g|mushrooms, halved; 1|onion, chopped; 2|carrots, sliced; 2 cloves|garlic; 400 ml|chicken stock; 1 tbsp|dijon mustard; 1 tsp|dried thyme; 2 tbsp|cornstarch; 100 ml|heavy cream",
  "", "the chicken, mushrooms, onion, carrots, garlic, stock, mustard and thyme", 6,
  "Mix the cornstarch with the cream, stir into the stew and cook on HIGH for 15 minutes until thickened. Serve with mashed potatoes or bread"),
 ("Slow Cooker Lentil and Sweet Potato Stew", "International", "250 g|lentils, rinsed; 2|sweet potatoes, diced; 1|onion, chopped; 2|carrots, diced; 3 cloves|garlic; 1 can|canned tomatoes; 1 l|vegetable stock; 2 tsp|cumin; 1 tsp|smoked paprika; 100 g|spinach",
  "", "everything except the spinach", 7,
  "Stir in the spinach until wilted, season well and serve with crusty bread"),
 ("Slow Cooker Hawaiian-Style Pulled Pork", "Hawaiian", "1.2 kg|pork shoulder; 1 can|pineapple chunks, with juice; 4 tbsp|soy sauce; 3 tbsp|brown sugar; 3 cloves|garlic; 1 tbsp|ginger, grated; 8|burger buns",
  "", "the pork, pineapple, soy sauce, sugar, garlic and ginger", 8,
  "Lift out the pork, shred it and return it to the juices. Pile into the buns with some of the pineapple"),
 ("Slow Cooker Chicken Korma", "Indian", "800 g|chicken thighs, cubed; 1|onion, chopped; 3 cloves|garlic; 1 tbsp|ginger, grated; 3 tbsp|curry paste; 400 ml|coconut milk; 60 g|almond flour; 150 g|yogurt; 300 g|rice",
  "", "the chicken, onion, garlic, ginger, curry paste and coconut milk", 6,
  "Stir in the almond flour and yogurt and cook 15 minutes more on HIGH until thick. Serve with rice"),
 ("Slow Cooker Beef and Beer Stew", "British", "1 kg|stewing beef, cubed; 3 tbsp|flour; 2|onions, chopped; 3|carrots, in chunks; 2|parsnips, in chunks; 330 ml|beer; 300 ml|beef stock; 1 tbsp|worcestershire sauce; 2|bay leaves",
  "Toss the beef in the flour and brown it in batches in a hot pan with a little oil", "the beef, onions, carrots, parsnips, beer, stock, Worcestershire sauce and bay leaves", 8,
  "Remove the bay leaves and season. Serve with mashed potatoes"),
 ("Slow Cooker White Chicken Chili", "Tex-Mex", "800 g|chicken breast; 2 can|white beans, drained; 1|onion, chopped; 2 cloves|garlic; 2|jalapenos, chopped; 700 ml|chicken stock; 2 tsp|cumin; 1 tsp|dried oregano; 100 g|cream cheese; 1 handful|cilantro",
  "", "the chicken, beans, onion, garlic, chilies, stock, cumin and oregano", 6,
  "Shred the chicken in the pot, stir in the cream cheese until melted and finish with cilantro"),
 ("Slow Cooker Chickpea Tagine", "Moroccan", "2 can|chickpeas, drained; 1|butternut squash, diced; 1|onion, chopped; 2 cloves|garlic; 1 can|canned tomatoes; 500 ml|vegetable stock; 100 g|dried apricots, chopped; 2 tsp|ras el hanout; 1 tsp|cinnamon; 200 g|couscous; 1 handful|cilantro",
  "", "everything except the couscous and cilantro", 6,
  "Pour 250 ml boiling water over the couscous, cover for 5 minutes and fluff. Serve the tagine over it with cilantro"),
 ("Slow Cooker Pork Chili Verde", "Mexican", "1.2 kg|pork shoulder, in large chunks; 400 g|salsa verde; 1|onion, chopped; 3 cloves|garlic; 2 tsp|cumin; 250 ml|chicken stock; 8|corn tortillas; 1|lime",
  "", "the pork, salsa verde, onion, garlic, cumin and stock", 8,
  "Shred the pork into the sauce. Serve in warm tortillas or over rice with lime wedges"),
 ("Slow Cooker Lamb with Rosemary and White Beans", "French", "1.2 kg|lamb shoulder; 4 cloves|garlic; 2 sprigs|rosemary; 1|onion, sliced; 2 can|white beans, drained; 1 can|canned tomatoes; 150 ml|white wine; 1 tbsp|olive oil",
  "Rub the lamb with oil, salt and pepper and brown it on all sides", "the onion, beans, tomatoes, wine, garlic and rosemary, with the lamb on top", 8,
  "Pull the lamb into chunks and serve with the beans and sauce"),
 ("Slow Cooker Teriyaki Chicken", "Japanese", "1 kg|chicken thighs, boneless; 120 ml|soy sauce; 4 tbsp|honey; 2 tbsp|rice vinegar; 3 cloves|garlic; 1 tbsp|ginger, grated; 2 tbsp|cornstarch; 1 tbsp|sesame seeds; 300 g|rice",
  "", "the chicken, soy sauce, honey, vinegar, garlic and ginger", 5,
  "Lift out the chicken and shred it. Stir the cornstarch with 2 tbsp water into the sauce and cook on HIGH 10 minutes until thick, then return the chicken. Serve on rice with sesame seeds"),
 ("Slow Cooker Tuscan Bean Stew", "Italian", "2 can|white beans, drained; 1 can|kidney beans, drained; 1|onion, chopped; 2|carrots, diced; 2|celery stalks, diced; 3 cloves|garlic; 1 can|canned tomatoes; 1 l|vegetable stock; 1 tsp|italian seasoning; 100 g|kale, shredded",
  "", "everything except the kale", 7,
  "Stir in the kale for the last 20 minutes. Season and serve with crusty bread and olive oil"),
 ("Slow Cooker Red Wine Short Ribs", "French", "1.5 kg|short ribs; 2 tbsp|flour; 1|onion, chopped; 2|carrots, in chunks; 3 cloves|garlic; 300 ml|red wine; 300 ml|beef stock; 2 tbsp|tomato paste; 2 sprigs|thyme",
  "Toss the ribs in the flour and brown them well on all sides in a hot pan", "the ribs, onion, carrots, garlic, wine, stock, tomato paste and thyme", 8,
  "Skim the fat from the sauce, season and serve the ribs with polenta or mashed potatoes"),
 ("Slow Cooker Mushroom Barley Stew", "Eastern European", "200 g|barley, rinsed; 500 g|mushrooms, sliced; 1|onion, chopped; 2|carrots, diced; 2|celery stalks, diced; 1.2 l|vegetable stock; 1 tbsp|soy sauce; 1 tsp|dried thyme; 1 handful|dill, chopped",
  "", "everything except the dill", 7,
  "Stir well, add a splash of water if it is very thick, and finish with dill"),
 ("Slow Cooker Thai Peanut Chicken", "Thai", "1 kg|chicken thighs, boneless; 1|red bell pepper, sliced; 120 g|peanut butter; 400 ml|coconut milk; 3 tbsp|soy sauce; 2 tbsp|honey; 1 tbsp|thai red curry paste; 1|lime; 2 tbsp|peanuts, chopped; 300 g|rice",
  "", "the chicken, pepper, peanut butter, coconut milk, soy sauce, honey and curry paste", 5,
  "Shred the chicken, stir in the lime juice and serve on rice with chopped peanuts"),
]
for t, cu, ings, prep, what, hrs, fin in SC:
    steps = []
    if prep: steps.append(prep)
    steps.append("Put " + what + " in the slow cooker, season with salt and pepper and stir")
    steps.append("Cover and cook on LOW for " + str(hrs) + " hours or on HIGH for " + str(max(3, hrs // 2)) + " hours, until " + ("the meat is falling apart" if any(w in ings for w in ("beef", "pork", "lamb", "ribs")) else "everything is tender"))
    steps.append(fin)
    R(F, t, "Main", cu, 15 + (10 if prep else 0), hrs * 60, 6, "Dinner, Main dish", ings + "; |salt; |black pepper", steps)

# ---------------- one-pot pastas ----------------
# title, cuisine, pasta, liquid ml, other ingredients, first step, what goes in, late addition, finish ingredients, finish words
OP = [
 ("One-Pot Tomato Basil Spaghetti", "Italian", "400 g|spaghetti", 900, "1 can|canned tomatoes; 1|onion, thinly sliced; 4 cloves|garlic, sliced; 0.5 tsp|chili flakes", "", "the spaghetti (snapped in half), tomatoes, onion, garlic and chili flakes", "", "1 handful|basil", "torn basil and an extra drizzle of olive oil"),
 ("One-Pot Sausage and Spinach Rigatoni", "Italian", "400 g|rigatoni", 1000, "4|italian sausages, meat squeezed out; 1 can|canned tomatoes; 3 cloves|garlic, chopped", "Brown the sausage meat in the oil for 5 minutes, breaking it up", "the rigatoni, tomatoes and garlic", "", "150 g|spinach; 40 g|parmesan, grated", "the spinach until wilted, then the parmesan"),
 ("One-Pot Lemon Garlic Shrimp Orzo", "Mediterranean", "300 g|orzo", 750, "4 cloves|garlic, chopped; 1|lemon, zest and juice; 1 tbsp|butter; 400 g|shrimp, peeled", "Melt the butter with the oil and fry the garlic and lemon zest for 1 minute", "the orzo", "Add the shrimp for the last 3 minutes", "1 handful|parsley, chopped; 30 g|parmesan, grated", "the lemon juice, parsley and parmesan"),
 ("One-Pot Creamy Mushroom Penne", "Italian", "400 g|penne", 900, "300 g|mushrooms, sliced; 3 cloves|garlic, chopped; 1 tsp|dried thyme; 1 tbsp|butter", "Fry the mushrooms in the butter and oil for 5 minutes until browned, adding the garlic and thyme at the end", "the penne", "", "120 ml|heavy cream; 40 g|parmesan, grated", "the cream and parmesan"),
 ("One-Pot Chicken Fajita Pasta", "Tex-Mex", "350 g|penne", 900, "400 g|chicken breast, sliced; 2|bell peppers, sliced; 1|onion, sliced; 2 tbsp|fajita seasoning; 1 can|canned tomatoes", "Brown the chicken, peppers and onion in the oil for 5 minutes with the fajita seasoning", "the penne and tomatoes", "", "100 g|cheddar, grated; 1 handful|cilantro", "the cheese and cilantro"),
 ("One-Pot Tuna and Pea Macaroni", "British", "350 g|macaroni", 850, "1|onion, finely chopped; 200 g|peas; 2 can|tuna, drained", "Soften the onion in the oil for 4 minutes", "the macaroni", "Add the peas for the last 3 minutes", "100 g|cheddar, grated; 1 tbsp|dijon mustard", "the tuna, cheese and mustard"),
 ("One-Pot Black Bean Taco Pasta", "Tex-Mex", "350 g|fusilli", 850, "1 can|black beans, drained; 150 g|sweetcorn; 1 can|canned tomatoes; 2 tbsp|taco seasoning", "", "the pasta, beans, corn, tomatoes and taco seasoning", "", "100 g|cheddar, grated; 4 tbsp|sour cream", "the cheese, then top each bowl with sour cream"),
 ("One-Pot Broccoli Cheddar Orzo", "American", "300 g|orzo", 750, "250 g|broccoli, finely chopped; 2 cloves|garlic, chopped", "Fry the garlic in the oil for 1 minute", "the orzo", "Add the broccoli for the last 5 minutes", "120 g|cheddar, grated; 60 ml|milk", "the cheese and milk"),
 ("One-Pot Pasta with Lentils and Tomato", "Italian", "250 g|macaroni", 1100, "150 g|red lentils, rinsed; 1|onion, chopped; 2|carrots, diced; 1 can|canned tomatoes; 1 tsp|dried rosemary", "Soften the onion and carrots in the oil for 5 minutes, then add the lentils, tomatoes, rosemary and stock and simmer 10 minutes", "the macaroni", "", "30 g|parmesan, grated|opt", "a drizzle of olive oil and parmesan if you like"),
 ("One-Pot Chorizo and Tomato Fusilli", "Spanish", "400 g|fusilli", 950, "120 g|chorizo, diced; 1|red onion, chopped; 1 can|canned tomatoes; 1 tsp|smoked paprika", "Fry the chorizo for 3 minutes until it releases its oil, then add the onion for 3 minutes", "the fusilli, tomatoes and paprika", "", "1 handful|parsley, chopped", "the parsley"),
 ("One-Pot Spinach Artichoke Pasta", "American", "400 g|penne", 900, "1 jar|artichoke hearts, chopped; 3 cloves|garlic, chopped; 150 g|spinach", "Fry the garlic in the oil for 1 minute", "the penne", "", "100 g|cream cheese; 40 g|parmesan, grated", "the artichokes, spinach, cream cheese and parmesan until creamy"),
 ("One-Pot Bacon and Pea Farfalle", "Italian", "400 g|farfalle", 900, "6 slices|bacon, chopped; 1|onion, chopped; 200 g|peas", "Crisp the bacon in the pot for 5 minutes, then soften the onion in the fat", "the farfalle", "Add the peas for the last 3 minutes", "60 g|parmesan, grated; 2 tbsp|creme fraiche", "the parmesan and creme fraiche"),
]
for t, cu, pasta, ml, other, first, what, late, fin_i, fin_w in OP:
    stock = "chicken stock" if any(w in other for w in ("chicken", "sausage", "bacon", "chorizo", "shrimp", "tuna")) else "vegetable stock"
    steps = ["Use a wide, deep pan or casserole and heat the oil over medium heat"]
    if first: steps.append(first)
    steps.append("Add " + what + ("" if "simmer 10" in first else " and the " + stock) + ". Bring to a boil, stirring so nothing sticks")
    steps.append("Simmer briskly for 10 to 12 minutes, stirring often, until the pasta is tender and most of the liquid has turned into a silky sauce. Add a splash of water if it gets dry" + (". " + late if late else ""))
    steps.append("Take off the heat and stir in " + fin_w + ". Season and serve straight away")
    R(F, t, "Main", cu, 10, 20, 4, "Dinner, Main dish" + (", Lunch" if "Macaroni" in t or "Orzo" in t else ""),
      pasta + "; " + other + "; " + str(ml) + " ml|" + stock + "; 1 tbsp|olive oil; |salt; |black pepper; " + fin_i, steps)
