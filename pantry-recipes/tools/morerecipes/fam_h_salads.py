from lib import R
F = "37-salads.txt"
def S(t, cu, prep, cook, serv, ings, steps, meals="Lunch, Side dish, Salad"):
    R(F, t, "Salad", cu, prep, cook, serv, meals, ings, steps)
MAIN = "Lunch, Dinner, Salad"

# grain and pasta salads (title, cuisine, grain, cook min, mix-ins, dressing ings, dressing words, meals)
GS = [
 ("Mediterranean Orzo Salad", "Mediterranean", "250 g|orzo", 9, "1|cucumber, diced; 200 g|cherry tomatoes, halved; 80 g|olives, halved; 100 g|feta, crumbled; 0.5|red onion, finely chopped", "4 tbsp|olive oil; 2 tbsp|red wine vinegar; 1 tsp|dried oregano", "olive oil, vinegar and oregano", MAIN),
 ("Lemon Herb Couscous Salad", "North African", "200 g|couscous", 0, "1 can|chickpeas, drained; 1|cucumber, diced; 1 handful|parsley, chopped; 1 handful|mint, chopped; 50 g|dried apricots, chopped", "4 tbsp|olive oil; 1|lemon, juiced; 1 tsp|cumin", "olive oil, lemon juice and cumin", "Lunch, Side dish, Salad"),
 ("Wild Rice and Cranberry Salad", "American", "200 g|wild rice", 45, "60 g|dried cranberries; 60 g|pecans, toasted; 3|green onions, sliced; 1|celery stalk, diced", "3 tbsp|olive oil; 2 tbsp|apple cider vinegar; 1 tbsp|maple syrup", "olive oil, cider vinegar and maple syrup", "Lunch, Side dish, Salad"),
 ("Pearl Barley Salad with Roasted Squash", "British", "200 g|barley", 30, "400 g|butternut squash, cubed and roasted; 100 g|arugula; 80 g|goat cheese; 40 g|walnuts", "3 tbsp|olive oil; 1 tbsp|balsamic vinegar; 1 tsp|dijon mustard", "olive oil, balsamic and mustard", MAIN),
 ("Southwest Quinoa Salad", "Tex-Mex", "200 g|quinoa", 15, "1 can|black beans, drained; 200 g|sweetcorn; 1|red bell pepper, diced; 1|avocado, diced; 1 handful|cilantro, chopped", "3 tbsp|olive oil; 2|limes, juiced; 1 tsp|cumin; 0.5 tsp|chili powder", "olive oil, lime, cumin and chili powder", MAIN),
 ("Sesame Soba and Edamame Salad", "Japanese", "200 g|soba noodles", 5, "200 g|edamame, shelled; 1|carrot, cut into matchsticks; 1|red bell pepper, sliced; 3|green onions, sliced; 1 tbsp|sesame seeds", "2 tbsp|soy sauce; 1 tbsp|rice vinegar; 1 tbsp|sesame oil; 1 tbsp|honey", "soy sauce, rice vinegar, sesame oil and honey", MAIN),
 ("Pesto Pasta Salad", "Italian", "300 g|fusilli", 10, "200 g|cherry tomatoes, halved; 125 g|mozzarella, small pearls; 50 g|arugula; 30 g|pine nuts, toasted", "4 tbsp|pesto; 2 tbsp|olive oil; 1 tbsp|lemon juice", "pesto, olive oil and lemon juice", "Lunch, Side dish, Salad"),
 ("Greek Lentil and Bulgur Salad", "Greek", "150 g|bulgur", 0, "1 can|lentils, drained; 1|cucumber, diced; 2|tomatoes, diced; 80 g|feta, crumbled; 1 handful|parsley", "4 tbsp|olive oil; 2 tbsp|lemon juice; 1 tsp|dried oregano", "olive oil, lemon juice and oregano", MAIN),
 ("Farro Salad with Apples and Cheddar", "American", "200 g|farro", 25, "2|apples, diced; 100 g|sharp cheddar, diced; 50 g|walnuts, toasted; 50 g|spinach", "3 tbsp|olive oil; 2 tbsp|apple cider vinegar; 1 tsp|whole grain mustard; 1 tsp|honey", "olive oil, cider vinegar, mustard and honey", MAIN),
 ("Brown Rice and Tuna Salad", "International", "200 g|brown rice", 30, "2 can|tuna, drained; 150 g|sweetcorn; 1|red bell pepper, diced; 3|green onions, sliced", "3 tbsp|olive oil; 2 tbsp|lemon juice; 1 tsp|dijon mustard", "olive oil, lemon juice and mustard", MAIN),
 ("Macaroni Salad", "American", "250 g|macaroni", 9, "2|celery stalks, diced; 0.5|red onion, finely chopped; 1|carrot, grated; 2|eggs, hard-boiled and chopped", "150 g|mayonnaise; 1 tbsp|apple cider vinegar; 1 tsp|sugar; 0.5 tsp|paprika", "mayonnaise, vinegar, sugar and paprika", "Lunch, Side dish, Salad"),
 ("Freekeh Salad with Herbs and Pomegranate", "Middle Eastern", "200 g|freekeh", 25, "1 handful|parsley, chopped; 1 handful|mint, chopped; 80 g|pomegranate seeds; 40 g|pistachios", "3 tbsp|olive oil; 1 tbsp|pomegranate molasses; 1|lemon, juiced", "olive oil, pomegranate molasses and lemon", "Lunch, Side dish, Salad"),
]
for t, cu, grain, mins, mix, dres, dres_w, meals in GS:
    g = grain.split("|")[1]
    if mins == 0:
        first = "Put the " + g + " in a bowl, pour over boiling water to cover by 1 cm, cover and leave 10 minutes (15 for bulgur), then fluff with a fork"
    else:
        first = "Cook the " + g + " in salted boiling water for about " + str(mins) + " minutes until tender, then drain and rinse under cold water"
    S(t, cu, 15, mins, 4, grain + "; " + mix + "; " + dres + "; |salt; |black pepper",
      [first, "Whisk together the " + dres_w + " with salt and pepper", "Toss the cooled " + g + " with everything else and the dressing", "Taste for seasoning. It keeps well in the fridge for 2 to 3 days, so it is good for packed lunches"], meals)

# leafy and vegetable salads
S("Arugula, Pear and Parmesan Salad", "Italian", 10, 0, 4, "150 g|arugula; 2|pears, thinly sliced; 40 g|parmesan, shaved; 40 g|walnuts, toasted; 3 tbsp|olive oil; 1 tbsp|balsamic vinegar; |salt",
  "Whisk the oil, vinegar and a pinch of salt / Toss the arugula and pears with the dressing / Scatter with the parmesan and walnuts and serve straight away")
S("Spinach Strawberry Salad", "American", 10, 0, 4, "150 g|spinach; 250 g|strawberries, sliced; 50 g|goat cheese; 40 g|almonds, sliced and toasted; 3 tbsp|olive oil; 1 tbsp|balsamic vinegar; 1 tsp|honey",
  "Whisk the oil, vinegar and honey with a pinch of salt / Toss the spinach and strawberries with the dressing / Top with crumbled goat cheese and almonds")
S("Shaved Fennel and Orange Salad", "Italian", 15, 0, 4, "2|fennel bulbs, very thinly sliced; 2|oranges, segmented; 0.5|red onion, thinly sliced; 50 g|olives; 3 tbsp|olive oil; 1 tbsp|lemon juice; |salt",
  "Toss the fennel and onion with the lemon juice and salt and leave 5 minutes to soften / Add the orange segments and olives / Drizzle with the oil and serve")
S("Simple Green Salad with Dijon Dressing", "French", 10, 0, 4, "1 head|lettuce, such as butter lettuce, leaves torn; 1|cucumber, sliced; 3 tbsp|olive oil; 1 tbsp|white wine vinegar; 1 tsp|dijon mustard; 1|shallot, finely chopped; |salt; |black pepper",
  "Whisk the mustard, vinegar, shallot, salt and pepper, then whisk in the oil / Toss the lettuce and cucumber with just enough dressing to coat, right before serving")
S("Chopped Italian Salad", "Italian-American", 15, 0, 4, "1 head|romaine lettuce, chopped; 80 g|salami, diced; 100 g|provolone, diced; 1 can|chickpeas, drained; 100 g|cherry tomatoes, halved; 4|pepperoncini, sliced; 0.5|red onion, diced; 4 tbsp|olive oil; 2 tbsp|red wine vinegar; 1 tsp|dried oregano",
  "Whisk the oil, vinegar, oregano, salt and pepper / Chop everything to a similar small size and combine in a large bowl / Toss with the dressing and serve", MAIN)
S("Kale Salad with Apple and Cranberry", "American", 15, 0, 4, "200 g|kale, stems removed, finely shredded; 1|apple, thinly sliced; 40 g|dried cranberries; 40 g|sunflower seeds; 3 tbsp|olive oil; 1|lemon, juiced; 1 tsp|maple syrup; |salt",
  "Massage the kale with the lemon juice, oil and a pinch of salt for 2 minutes until it softens and darkens / Add the maple syrup, apple, cranberries and seeds and toss / It keeps dressed for a day in the fridge")
S("Thai Cucumber Peanut Salad", "Thai", 10, 0, 4, "2|cucumbers, sliced; 0.5|red onion, thinly sliced; 1|red chili, sliced; 2 tbsp|lime juice; 1 tbsp|fish sauce; 1 tsp|sugar; 40 g|peanuts, chopped; 1 handful|cilantro",
  "Stir the lime juice, fish sauce and sugar until the sugar dissolves / Toss with the cucumber, onion and chili / Top with peanuts and cilantro and serve within an hour")
S("Mexican Street Corn Salad", "Mexican", 10, 8, 4, "500 g|sweetcorn; 1 tbsp|butter; 2 tbsp|mayonnaise; 2 tbsp|sour cream; 1|lime; 50 g|cotija cheese, crumbled; 0.5 tsp|chili powder; 1 handful|cilantro; 1|jalapeno, diced",
  "Char the corn in the butter in a hot pan for 6 to 8 minutes, then cool slightly / Mix the mayonnaise, sour cream, lime juice and chili powder / Toss with the corn, jalapeno and cilantro and top with the cheese")
S("Tomato and Cucumber Salad", "Middle Eastern", 10, 0, 4, "4|tomatoes, diced; 2|cucumbers, diced; 0.5|red onion, finely chopped; 1 handful|parsley, chopped; 3 tbsp|olive oil; 1|lemon, juiced; 0.5 tsp|sumac; |salt",
  "Combine the tomatoes, cucumbers, onion and parsley / Dress with olive oil, lemon juice, sumac and salt just before serving")
S("Broccoli Crunch Salad", "American", 15, 0, 6, "500 g|broccoli, in small florets; 0.5|red onion, finely chopped; 50 g|dried cranberries; 50 g|sunflower seeds; 4 slices|bacon, cooked and crumbled; 120 g|mayonnaise; 1 tbsp|apple cider vinegar; 1 tbsp|honey",
  "Whisk the mayonnaise, vinegar and honey with a pinch of salt / Toss with the broccoli, onion, cranberries and seeds / Chill 30 minutes, then top with the bacon")
S("Roasted Beet and Orange Salad", "Mediterranean", 10, 45, 4, "4|beets; 2|oranges, sliced; 50 g|arugula; 40 g|feta, crumbled; 30 g|pistachios; 3 tbsp|olive oil; 1 tbsp|red wine vinegar",
  "Wrap the beets in foil and roast at 200°C (400°F) for 45 minutes until tender, then peel and slice / Arrange with the orange slices and arugula / Drizzle with oil and vinegar, season and top with feta and pistachios")
S("Asian Sesame Chicken Salad", "Asian", 15, 0, 4, "300 g|cooked chicken, shredded; 300 g|napa cabbage, shredded; 1|carrot, grated; 1|red bell pepper, sliced; 3|green onions, sliced; 3 tbsp|soy sauce; 2 tbsp|rice vinegar; 1 tbsp|sesame oil; 1 tbsp|honey; 2 tbsp|sesame seeds",
  "Whisk the soy sauce, vinegar, sesame oil and honey / Toss the cabbage, carrot, pepper, green onions and chicken with the dressing / Scatter with sesame seeds", MAIN)
S("Chicken Salad with Grapes and Celery", "American", 15, 0, 4, "400 g|cooked chicken, diced; 150 g|grapes, halved; 2|celery stalks, diced; 40 g|pecans, chopped; 120 g|mayonnaise; 2 tbsp|greek yogurt; 1 tsp|dijon mustard; 1 tbsp|lemon juice",
  "Mix the mayonnaise, yogurt, mustard, lemon juice, salt and pepper / Fold in the chicken, grapes, celery and pecans / Serve on lettuce or in sandwiches", MAIN)
S("Steak Salad with Blue Cheese", "American", 10, 10, 2, "300 g|sirloin steak; 1 head|romaine lettuce, chopped; 150 g|cherry tomatoes, halved; 0.5|red onion, sliced; 50 g|blue cheese, crumbled; 3 tbsp|olive oil; 1 tbsp|balsamic vinegar; |salt; |black pepper",
  "Season the steak and sear 3 to 4 minutes a side for medium-rare; rest 5 minutes and slice / Toss the lettuce, tomatoes and onion with the oil and vinegar / Top with the steak and blue cheese", MAIN)
S("Salmon Salad with Avocado", "International", 10, 12, 2, "2|salmon fillets; 100 g|mixed salad greens; 1|avocado, sliced; 1|cucumber, sliced; 3 tbsp|olive oil; 1|lemon; 1 tsp|dijon mustard; |salt",
  "Season the salmon and roast at 200°C (400°F) for 12 minutes, then flake / Whisk the oil, lemon juice and mustard / Toss the greens and cucumber with half the dressing, top with the salmon and avocado and drizzle with the rest", MAIN)
S("Halloumi and Watermelon Salad", "Cypriot", 10, 5, 4, "250 g|halloumi, sliced; 600 g|watermelon, cubed; 1|cucumber, diced; 1 handful|mint; 2 tbsp|olive oil; 1|lime",
  "Fry the halloumi in a dry pan 2 minutes a side until golden / Toss the watermelon, cucumber and mint with the oil and lime juice / Top with the warm halloumi", MAIN)
S("Edamame and Corn Salad", "International", 10, 5, 4, "300 g|edamame, shelled; 200 g|sweetcorn; 1|red bell pepper, diced; 3|green onions, sliced; 3 tbsp|olive oil; 2 tbsp|rice vinegar; 1 tsp|honey",
  "Blanch the edamame and corn for 3 minutes, then cool under cold water / Whisk the oil, vinegar, honey and salt / Toss everything together")
S("Moroccan Carrot Salad", "Moroccan", 10, 5, 4, "500 g|carrots, sliced; 3 tbsp|olive oil; 1 tsp|cumin; 1 tsp|paprika; 0.5 tsp|cinnamon; 1|lemon, juiced; 1 clove|garlic, grated; 1 handful|parsley, chopped; |salt",
  "Boil the carrots 4 to 5 minutes until just tender, then drain / While warm, toss with the oil, spices, lemon juice, garlic and salt / Cool, then stir in the parsley")
S("Italian Bean Salad with Tuna", "Italian", 10, 0, 4, "2 can|cannellini beans, drained; 2 can|tuna, drained; 0.5|red onion, thinly sliced; 1 handful|parsley, chopped; 4 tbsp|olive oil; 2 tbsp|red wine vinegar; |salt; |black pepper",
  "Soak the onion in the vinegar with a pinch of salt for 5 minutes to soften it / Add the beans, tuna, parsley and oil and toss gently / Season well and serve with bread", MAIN)
S("Creamy Cucumber Dill Salad", "Eastern European", 10, 0, 4, "2|cucumbers, thinly sliced; 0.5|red onion, thinly sliced; 150 g|sour cream; 1 tbsp|white wine vinegar; 1 tbsp|dill, chopped; 1 tsp|sugar; |salt",
  "Salt the cucumbers and leave 10 minutes, then squeeze out the liquid / Mix the sour cream, vinegar, dill and sugar / Fold in the cucumbers and onion and chill")
S("Red Cabbage and Apple Slaw", "German", 15, 0, 6, "500 g|red cabbage, finely shredded; 2|apples, cut into matchsticks; 1|carrot, grated; 3 tbsp|olive oil; 2 tbsp|apple cider vinegar; 1 tbsp|honey; 1 tsp|whole grain mustard; |salt",
  "Whisk the oil, vinegar, honey, mustard and salt / Toss with the cabbage, apples and carrot / Leave 15 minutes to soften slightly before serving")
S("Creamy Broccoli Slaw", "American", 10, 0, 4, "400 g|broccoli, stems peeled and cut into matchsticks (or bagged broccoli slaw); 1|carrot, grated; 40 g|raisins; 100 g|mayonnaise; 1 tbsp|apple cider vinegar; 1 tsp|sugar",
  "Mix the mayonnaise, vinegar, sugar and a pinch of salt / Toss with the broccoli, carrot and raisins / Chill 20 minutes")
S("Mango Jicama Slaw", "Mexican", 15, 0, 4, "1|mango, cut into matchsticks; 300 g|jicama, cut into matchsticks; 150 g|red cabbage, shredded; 1 handful|cilantro; 2|limes, juiced; 2 tbsp|olive oil; 0.5 tsp|chili powder; |salt",
  "Whisk the lime juice, oil, chili powder and salt / Toss with the mango, jicama, cabbage and cilantro / Serve with tacos, grilled fish or chicken")
S("Peanut Ginger Slaw", "Asian", 15, 0, 6, "300 g|cabbage, shredded; 150 g|red cabbage, shredded; 2|carrots, grated; 4|green onions, sliced; 3 tbsp|peanut butter; 2 tbsp|soy sauce; 2 tbsp|rice vinegar; 1 tbsp|honey; 1 tsp|ginger, grated; 40 g|peanuts",
  "Whisk the peanut butter, soy sauce, vinegar, honey and ginger with 2 tbsp warm water / Toss with the vegetables / Top with peanuts")
S("Vinegar Coleslaw", "Southern US", 10, 0, 6, "600 g|cabbage, finely shredded; 1|carrot, grated; 0.5|onion, grated; 4 tbsp|apple cider vinegar; 3 tbsp|vegetable oil; 2 tbsp|sugar; 0.5 tsp|celery seed; |salt",
  "Warm the vinegar, oil, sugar, celery seed and 1 tsp salt until the sugar dissolves / Pour over the cabbage, carrot and onion and toss / Chill at least 1 hour, tossing once; it gets better overnight")
S("Fruit Salad with Honey Lime", "International", 15, 0, 6, "300 g|strawberries, halved; 2|kiwi, peeled and sliced; 1|mango, diced; 200 g|grapes, halved; 150 g|blueberries; 2 tbsp|honey; 1|lime, zest and juice; 1 handful|mint",
  "Whisk the honey with the lime zest and juice / Toss gently with the fruit / Scatter with mint and serve chilled", "Breakfast, Brunch, Side dish, Salad, Dessert")
S("Warm Lentil Salad with Roasted Vegetables", "French", 15, 30, 4, "200 g|green lentils; 1|red onion, wedged; 2|bell peppers, chopped; 1|zucchini, chopped; 3 tbsp|olive oil; 2 tbsp|red wine vinegar; 1 tsp|dijon mustard; 80 g|goat cheese",
  "Roast the onion, peppers and zucchini with 1 tbsp oil at 200°C (400°F) for 25 minutes / Simmer the lentils in water 20 to 25 minutes until just tender, then drain / Whisk the remaining oil with the vinegar and mustard and toss with the warm lentils and vegetables / Top with goat cheese", MAIN)
S("Potato Salad with Herbs and Mustard", "German", 10, 15, 6, "1 kg|baby potatoes; 1|red onion, finely chopped; 4 tbsp|olive oil; 3 tbsp|white wine vinegar; 1 tbsp|dijon mustard; 1 handful|chives, chopped; 1 handful|parsley, chopped; |salt",
  "Boil the potatoes in salted water 15 minutes until tender, drain and halve / Whisk the oil, vinegar, mustard and salt / Toss the warm potatoes with the dressing and onion so they soak it up / Stir in the herbs when cooled")
