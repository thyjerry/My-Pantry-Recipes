from lib import R
F = "38-sides.txt"
def S(t, cu, prep, cook, serv, ings, steps, meals="Side dish"):
    R(F, t, "Side", cu, prep, cook, serv, meals, ings, steps)

# roasted vegetables: title, cuisine, veg, seasoning, seasoning words, minutes, finish ings, finish words
RV = [
 ("Parmesan Roasted Zucchini", "Italian", "4|zucchini, in thick batons", "1 tsp|garlic powder; 1 tsp|italian seasoning", "garlic powder and Italian seasoning", 20, "30 g|parmesan, grated", "sprinkle with parmesan for the last 5 minutes"),
 ("Maple Roasted Butternut Squash", "American", "1|butternut squash, peeled and cubed", "2 tbsp|maple syrup; 0.5 tsp|cinnamon", "maple syrup and cinnamon", 35, "1 tbsp|pecans, chopped", "scatter with pecans"),
 ("Roasted Green Beans with Almonds", "French", "500 g|green beans, trimmed", "2 cloves|garlic, sliced", "garlic", 15, "30 g|almonds, sliced; 1|lemon", "toss with the toasted almonds and a squeeze of lemon"),
 ("Smoky Roasted Cauliflower", "Spanish", "1|cauliflower, in florets", "2 tsp|smoked paprika; 1 tsp|cumin", "smoked paprika and cumin", 25, "1 handful|parsley, chopped", "scatter with parsley"),
 ("Roasted Asparagus with Parmesan", "Italian", "500 g|asparagus, trimmed", "1 clove|garlic, grated", "garlic", 12, "30 g|parmesan, shaved; 1|lemon", "top with parmesan and lemon zest"),
 ("Balsamic Roasted Beets", "Mediterranean", "6|beets, peeled and cut into wedges", "2 tbsp|balsamic vinegar; 1 tsp|dried thyme", "balsamic vinegar and thyme", 40, "50 g|feta, crumbled|opt", "top with feta if you like"),
 ("Crispy Roasted Potatoes with Rosemary", "British", "1 kg|potatoes, in chunks", "2 sprigs|rosemary, leaves chopped; 3 cloves|garlic, smashed", "rosemary and garlic", 45, "1 tbsp|parsley, chopped", "finish with flaky salt and parsley (parboiling the potatoes for 8 minutes first and roughing them up makes them extra crisp)"),
 ("Roasted Carrots and Parsnips with Honey", "British", "500 g|carrots, halved; 400 g|parsnips, quartered", "2 tbsp|honey; 1 tsp|dried thyme", "thyme (add the honey for the last 10 minutes)", 35, "1 tbsp|parsley, chopped", "scatter with parsley"),
 ("Garlic Roasted Mushrooms and Peppers", "International", "400 g|mushrooms, halved; 2|bell peppers, in chunks", "3 cloves|garlic, sliced; 1 tsp|dried oregano", "garlic and oregano", 20, "1 tbsp|balsamic vinegar", "splash with balsamic"),
 ("Spiced Roasted Sweet Potato Cubes", "International", "3|sweet potatoes, cubed", "1 tsp|cumin; 1 tsp|smoked paprika; 0.5 tsp|chili powder", "cumin, paprika and chili", 30, "1|lime", "squeeze over lime"),
 ("Roasted Broccolini with Chili and Lemon", "Italian", "400 g|broccolini", "0.5 tsp|chili flakes; 2 cloves|garlic, sliced", "chili flakes and garlic", 15, "1|lemon", "squeeze over lemon"),
 ("Za'atar Roasted Carrots", "Middle Eastern", "700 g|carrots, halved lengthways", "2 tbsp|za'atar", "za'atar", 30, "4 tbsp|yogurt", "serve over a swoosh of yogurt"),
 ("Roasted Eggplant with Yogurt and Mint", "Turkish", "2|eggplants, in thick rounds", "1 tsp|cumin", "cumin", 30, "150 g|yogurt; 1 handful|mint", "top with garlicky yogurt and mint"),
 ("Roasted Radishes with Butter", "French", "500 g|radishes, halved", "1 tsp|dried thyme", "thyme", 20, "1 tbsp|butter; 1 tbsp|chives, snipped", "toss with butter and chives"),
 ("Roasted Brussels Sprouts with Bacon", "American", "600 g|brussels sprouts, halved", "4 slices|bacon, chopped", "chopped bacon", 25, "1 tbsp|maple syrup", "drizzle with maple syrup"),
 ("Roasted Cabbage Wedges", "Eastern European", "1|cabbage, cut into 8 wedges", "1 tsp|caraway seeds; 1 tsp|garlic powder", "caraway and garlic powder", 30, "1 tbsp|apple cider vinegar", "splash with vinegar"),
]
for t, cu, veg, sea, sea_w, mins, fin_i, fin_w in RV:
    S(t, cu, 10, mins, 4, veg + "; 2 tbsp|olive oil; " + sea + "; |salt; |black pepper; " + fin_i,
      ["Heat the oven to 220°C (200°C fan, 425°F)",
       "Toss the vegetables with the oil, " + sea_w + ", salt and pepper and spread out on a baking tray in one layer, with space between the pieces",
       "Roast for about " + str(mins) + " minutes, turning halfway, until tender and browned at the edges",
       "To finish, " + fin_w])

# mashes and potatoes
S("Garlic Mashed Cauliflower", "American", 10, 15, 4, "1|cauliflower, in florets; 3 cloves|garlic; 2 tbsp|butter; 30 g|parmesan, grated; 2 tbsp|milk; |salt; |black pepper",
  "Boil the cauliflower and garlic in salted water 12 to 15 minutes until very soft / Drain well and let steam dry for 2 minutes / Mash or blend with the butter, parmesan and milk until smooth, then season")
S("Sweet Potato Mash with Brown Butter", "American", 10, 20, 4, "1 kg|sweet potatoes, peeled and cubed; 3 tbsp|butter; 1 pinch|nutmeg; |salt",
  "Boil the sweet potatoes 15 minutes until soft and drain / Cook the butter in a small pan until it smells nutty and turns golden brown / Mash the potatoes with the butter, nutmeg and salt")
S("Colcannon", "Irish", 15, 20, 6, "1 kg|potatoes, peeled and chopped; 300 g|cabbage, shredded; 4|green onions, sliced; 4 tbsp|butter; 150 ml|milk; |salt; |black pepper",
  "Boil the potatoes 15 to 20 minutes until tender and drain / Meanwhile soften the cabbage in 1 tbsp butter for 5 minutes, adding the green onions at the end / Warm the milk, then mash it into the potatoes with the rest of the butter / Fold in the cabbage, season and serve with a knob of butter on top")
S("Celeriac and Potato Mash", "French", 15, 20, 4, "500 g|celeriac, peeled and cubed; 500 g|potatoes, peeled and cubed; 3 tbsp|butter; 100 ml|milk; |salt; |white pepper",
  "Boil the celeriac and potatoes together for 18 to 20 minutes until soft / Drain and mash with the butter and warm milk / Season with salt and white pepper")
S("Hasselback Potatoes", "Swedish", 15, 50, 4, "8|medium potatoes; 3 tbsp|butter, melted; 2 tbsp|olive oil; 2 cloves|garlic, grated; 30 g|parmesan, grated; |salt",
  "Heat the oven to 220°C (425°F). Lay each potato between two chopsticks and cut thin slices down to the chopsticks so it stays joined at the base / Mix the butter, oil and garlic and brush over, getting into the cuts / Roast 45 to 50 minutes, brushing again halfway, until crisp / Sprinkle with parmesan and salt for the last 5 minutes")
S("Smashed Potatoes with Garlic", "American", 10, 45, 4, "800 g|baby potatoes; 3 tbsp|olive oil; 3 cloves|garlic, minced; 1 tbsp|parsley, chopped; |salt",
  "Boil the potatoes 15 to 20 minutes until tender, then drain / Put on an oiled tray and press each one flat with a glass / Drizzle with oil and garlic, season and roast at 220°C (425°F) for 25 minutes until crisp / Scatter with parsley")
S("Potatoes Lyonnaise", "French", 15, 30, 4, "800 g|potatoes, sliced; 2|onions, sliced; 3 tbsp|butter; 1 tbsp|vegetable oil; 1 tbsp|parsley, chopped; |salt",
  "Boil the potato slices for 5 minutes and drain / Cook the onions slowly in 1 tbsp butter for 15 minutes until golden / Fry the potatoes in the oil and remaining butter in batches until crisp / Toss with the onions, season and finish with parsley")

# greens and quick vegetables
S("Sauteed Kale with Garlic and Lemon", "Italian", 5, 8, 4, "300 g|kale, shredded; 2 tbsp|olive oil; 3 cloves|garlic, sliced; 0.25 tsp|chili flakes; 1|lemon; |salt",
  "Warm the oil with the garlic and chili until fragrant / Add the kale and a splash of water, cover and cook 4 minutes / Uncover and toss until tender, then season and squeeze over lemon")
S("Creamed Spinach", "American", 5, 10, 4, "500 g|spinach; 2 tbsp|butter; 1|shallot, chopped; 1 clove|garlic; 100 g|cream cheese; 60 ml|milk; 30 g|parmesan, grated; 1 pinch|nutmeg",
  "Wilt the spinach in a large pan with a splash of water, then squeeze dry and chop / Soften the shallot and garlic in the butter / Stir in the cream cheese, milk, parmesan and nutmeg until smooth / Fold in the spinach and heat through")
S("Stir-Fried Cabbage with Garlic", "Chinese", 5, 6, 4, "500 g|cabbage, shredded; 2 tbsp|vegetable oil; 3 cloves|garlic, sliced; 1 tbsp|soy sauce; 1 tsp|rice vinegar; 0.5 tsp|sugar",
  "Heat the oil in a wok until very hot and fry the garlic for 20 seconds / Add the cabbage and stir-fry 4 minutes until wilted with some charred edges / Add the soy sauce, vinegar and sugar and toss")
S("Peas with Mint and Butter", "British", 2, 5, 4, "500 g|frozen peas; 2 tbsp|butter; 1 handful|mint, chopped; |salt",
  "Cook the peas in boiling salted water for 3 minutes / Drain and toss with the butter, mint and a little salt")
S("Garlic Butter Corn on the Cob", "American", 5, 10, 4, "4|corn on the cob; 3 tbsp|butter, soft; 1 clove|garlic, grated; 1 tbsp|parsley, chopped; |salt",
  "Boil the corn for 6 to 8 minutes until tender / Mash the butter with the garlic, parsley and salt / Spread over the hot corn")
S("Sesame Green Beans", "Chinese", 5, 8, 4, "400 g|green beans; 1 tbsp|vegetable oil; 2 cloves|garlic, minced; 1 tbsp|soy sauce; 1 tsp|sesame oil; 1 tbsp|sesame seeds",
  "Blanch the beans for 2 minutes and drain / Stir-fry in the hot oil with the garlic for 3 minutes until blistered / Toss with the soy sauce, sesame oil and seeds")
S("Summer Vegetable Ratatouille", "French", 20, 45, 6, "4 tbsp|olive oil; 1|onion, chopped; 3 cloves|garlic; 1|eggplant, diced; 2|zucchini, diced; 2|bell peppers, diced; 1 can|canned tomatoes; 1 tsp|herbes de provence; 1 handful|basil",
  "Soften the onion and garlic in half the oil, then remove / Brown the eggplant, zucchini and peppers in batches in the rest of the oil / Return everything, add the tomatoes and herbs and simmer gently 30 minutes until soft and rich / Finish with torn basil. It is even better the next day", "Side dish, Dinner")
S("Glazed Turnips", "French", 5, 20, 4, "600 g|turnips, peeled and cut into wedges; 2 tbsp|butter; 1 tbsp|sugar; 250 ml|vegetable stock; |salt",
  "Put the turnips, butter, sugar and stock in a wide pan in one layer / Simmer uncovered for 15 minutes until the turnips are tender and the liquid has reduced / Keep cooking, shaking the pan, until they are coated in a shiny glaze")
S("Sauteed Zucchini and Corn", "Mexican", 5, 10, 4, "2|zucchini, diced; 200 g|sweetcorn; 1 tbsp|olive oil; 0.5|onion, diced; 1 tsp|cumin; 40 g|queso fresco, crumbled; 1|lime",
  "Fry the onion in the oil for 3 minutes / Add the zucchini, corn and cumin and cook 6 minutes until tender / Season, squeeze over lime and top with the cheese")

# rice, grains and beans
S("Saffron Rice", "Persian", 5, 20, 4, "300 g|basmati rice; 1 pinch|saffron; 2 tbsp|butter; 500 ml|water; |salt",
  "Soak the saffron in 2 tbsp hot water / Rinse the rice, then fry it in the butter for 1 minute / Add the water, saffron and salt, bring to a boil, cover and cook on the lowest heat 15 minutes / Rest 5 minutes and fluff")
S("Brown Butter Orzo with Parmesan", "Italian", 5, 12, 4, "250 g|orzo; 3 tbsp|butter; 600 ml|chicken stock; 40 g|parmesan, grated; 1 tbsp|parsley, chopped",
  "Cook the orzo in the butter, stirring, until both are toasty brown / Add the stock and simmer 9 to 10 minutes, stirring, until absorbed / Stir in the parmesan and parsley")
S("Rice Pilaf with Vermicelli", "Middle Eastern", 5, 20, 4, "300 g|long grain rice; 50 g|vermicelli; 2 tbsp|butter; 550 ml|chicken stock; |salt",
  "Toast the vermicelli in the butter until golden brown / Add the rinsed rice and stir 1 minute / Add the stock and salt, bring to a boil, cover and cook on low 15 minutes / Rest 5 minutes and fluff")
S("Mushroom Rice", "International", 5, 20, 4, "300 g|rice; 250 g|mushrooms, chopped; 1|onion, finely chopped; 2 tbsp|butter; 600 ml|vegetable stock; 1 tsp|dried thyme",
  "Fry the mushrooms and onion in the butter for 6 minutes / Add the rice and thyme and stir 1 minute / Add the stock, cover and cook on low 15 minutes / Rest 5 minutes and fluff")
S("Coconut Turmeric Rice", "Indonesian", 5, 18, 4, "300 g|jasmine rice; 400 ml|coconut milk; 150 ml|water; 1 tsp|turmeric; 1 stalk|lemongrass, bruised; |salt",
  "Rinse the rice well / Put everything in a pan, bring to a boil, cover and cook on low 15 minutes / Rest 5 minutes, remove the lemongrass and fluff")
S("Garlic Butter Rice", "American", 5, 18, 4, "300 g|long grain rice; 3 tbsp|butter; 4 cloves|garlic, minced; 600 ml|chicken stock; 1 tbsp|parsley, chopped",
  "Fry the garlic in the butter for 1 minute until fragrant / Add the rice and stir to coat / Add the stock, cover and cook on low 15 minutes / Fluff and stir in the parsley")
S("Herbed Bulgur Pilaf", "Turkish", 5, 15, 4, "200 g|bulgur; 1|onion, finely chopped; 2 tbsp|olive oil; 1 tbsp|tomato paste; 400 ml|vegetable stock; 1 handful|parsley, chopped",
  "Soften the onion in the oil, stir in the tomato paste / Add the bulgur and stock, cover and simmer 12 minutes / Rest off the heat 5 minutes, fluff and stir in the parsley")
S("Creamy Polenta", "Italian", 5, 15, 4, "150 g|polenta; 900 ml|water; 2 tbsp|butter; 40 g|parmesan, grated; |salt",
  "Bring the water to a boil with 1 tsp salt / Whisk in the polenta slowly and cook, stirring often, as the packet says (5 minutes for quick-cook) / Beat in the butter and parmesan")
S("Mexican Black Beans", "Mexican", 5, 15, 4, "2 can|black beans, with liquid; 1 tbsp|vegetable oil; 0.5|onion, chopped; 2 cloves|garlic; 1 tsp|cumin; 0.5 tsp|dried oregano; 1|lime; |salt",
  "Soften the onion and garlic in the oil for 4 minutes / Add the beans with their liquid, cumin and oregano and simmer 10 minutes until thick / Season and squeeze over lime")
S("Braised White Beans with Garlic", "Italian", 5, 15, 4, "2 can|white beans, drained; 3 tbsp|olive oil; 3 cloves|garlic, sliced; 1 sprig|rosemary; 200 ml|vegetable stock; |salt",
  "Warm the garlic and rosemary in the oil until fragrant / Add the beans and stock and simmer 10 minutes, crushing a few beans to thicken / Season and drizzle with more oil")

# air fryer sides
S("Air Fryer Roasted Potatoes", "American", 5, 20, 4, "700 g|baby potatoes, halved; 1 tbsp|olive oil; 1 tsp|garlic powder; 1 tsp|paprika; |salt",
  "Toss the potatoes with the oil, garlic powder, paprika and salt / Air fry at 200°C (400°F) for 18 to 20 minutes, shaking twice, until crisp outside and tender inside")
S("Air Fryer Broccoli", "International", 5, 8, 4, "400 g|broccoli, in florets; 1 tbsp|olive oil; 0.5 tsp|garlic powder; 1|lemon; |salt",
  "Toss the broccoli with the oil, garlic powder and salt / Air fry at 190°C (375°F) for 7 to 8 minutes, shaking once, until crisp at the tips / Squeeze over lemon")
S("Air Fryer Zucchini Chips", "Italian", 10, 12, 4, "2|zucchini, in 5 mm rounds; 1|egg; 50 g|panko; 25 g|parmesan, grated; |cooking spray; |salt",
  "Dip the zucchini in beaten egg, then in the panko mixed with parmesan / Spray and air fry in a single layer at 200°C (400°F) for 10 to 12 minutes, turning once", "Side dish, Snack, Appetizer")
S("Air Fryer Onion Rings", "American", 15, 10, 4, "2|onions, in thick rings; 60 g|flour; 1|egg; 120 ml|buttermilk; 80 g|panko; 0.5 tsp|paprika; |cooking spray; |salt",
  "Coat the rings in seasoned flour, then in the egg whisked with buttermilk, then panko mixed with paprika / Spray and air fry at 200°C (400°F) in batches for 8 to 10 minutes, turning once, until golden", "Side dish, Snack, Appetizer")
S("Air Fryer Green Beans", "International", 5, 8, 4, "400 g|green beans; 1 tbsp|olive oil; 0.5 tsp|garlic powder; 20 g|parmesan, grated; |salt",
  "Toss the beans with the oil, garlic powder and salt / Air fry at 190°C (375°F) for 7 to 8 minutes, shaking once / Toss with parmesan")
S("Air Fryer Corn on the Cob", "American", 2, 12, 4, "4|corn on the cob; 2 tbsp|butter, melted; 0.5 tsp|chili powder; 1|lime; |salt",
  "Brush the corn with half the butter / Air fry at 200°C (400°F) for 10 to 12 minutes, turning halfway, until charred in spots / Brush with the rest of the butter, sprinkle with chili and salt and squeeze over lime")
S("Air Fryer Cauliflower with Turmeric", "Indian", 5, 14, 4, "1|cauliflower, in florets; 1 tbsp|vegetable oil; 1 tsp|turmeric; 1 tsp|cumin; 0.5 tsp|chili powder; |salt",
  "Toss the cauliflower with the oil and spices / Air fry at 190°C (375°F) for 12 to 14 minutes, shaking twice, until tender and browned")
S("Air Fryer Butternut Squash", "American", 10, 18, 4, "600 g|butternut squash, cubed; 1 tbsp|olive oil; 1 tsp|dried sage; 1 tbsp|maple syrup; |salt",
  "Toss the squash with the oil, sage and salt / Air fry at 200°C (400°F) for 15 to 18 minutes, shaking twice / Drizzle with maple syrup")
