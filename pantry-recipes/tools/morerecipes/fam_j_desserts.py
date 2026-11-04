from lib import R
F = "39-desserts.txt"
def D(t, cu, prep, cook, serv, ings, steps, meals="Dessert"):
    R(F, t, "Dessert", cu, prep, cook, serv, meals, ings, steps)

# fruit crisps and crumbles: title, cuisine, fruit, fruit seasoning, topping extras, minutes
CR = [
 ("Pear and Ginger Crisp", "American", "6|pears, peeled and sliced", "2 tbsp|brown sugar; 1 tsp|ground ginger; 1 tbsp|cornstarch", "50 g|pecans, chopped", 40),
 ("Rhubarb and Strawberry Crumble", "British", "400 g|rhubarb, in 2 cm pieces; 300 g|strawberries, halved", "80 g|sugar; 1 tbsp|cornstarch", "", 40),
 ("Cherry Almond Crisp", "American", "700 g|cherries, pitted", "50 g|sugar; 1 tbsp|cornstarch; 0.5 tsp|almond extract", "40 g|almonds, sliced", 35),
 ("Plum Crumble with Cinnamon", "British", "800 g|plums, halved and stoned", "60 g|sugar; 1 tsp|cinnamon", "", 35),
 ("Mixed Berry Oat Crisp", "American", "700 g|mixed berries, frozen are fine", "50 g|sugar; 2 tbsp|cornstarch; 1|lemon, zested", "", 40),
 ("Peach and Blueberry Crisp", "American", "5|peaches, sliced; 200 g|blueberries", "50 g|brown sugar; 1 tbsp|cornstarch", "", 40),
]
for t, cu, fruit, fs, extra, mins in CR:
    top = "100 g|flour; 80 g|rolled oats; 80 g|brown sugar; 100 g|butter, cold, diced; 1 pinch|salt" + ("" if "cinnamon" in fs else "; 0.5 tsp|cinnamon")
    D(t, cu, 15, mins, 6, fruit + "; " + fs.replace("brown sugar", "brown sugar, for the fruit") + "; " + top.replace("80 g|brown sugar", "80 g|brown sugar, for the topping" if "brown sugar" not in fs else "80 g|sugar, for the topping") + ("; " + extra if extra else ""),
      ["Heat the oven to 180°C (350°F) and butter a 20 x 30 cm baking dish",
       "Toss the fruit with the sugar, spices and cornstarch and spread in the dish",
       "Rub the flour, oats, sugar, cinnamon, salt and butter together with your fingertips until clumpy" + (", then mix in the nuts" if extra else ""),
       "Scatter the topping over the fruit and bake " + str(mins) + " minutes until golden and bubbling at the edges",
       "Cool 10 minutes and serve warm with ice cream, yogurt or custard"])

# puddings, custards and creamy desserts
D("Vanilla Custard Pudding", "American", 5, 12, 4, "500 ml|milk; 3|egg yolks; 70 g|sugar; 3 tbsp|cornstarch; 1 pinch|salt; 1 tbsp|butter; 1 tsp|vanilla extract",
  "Whisk the yolks, sugar, cornstarch and salt in a pan, then whisk in the milk / Cook over medium heat, whisking constantly, until it thickens and bubbles, then cook 1 minute more / Off the heat stir in the butter and vanilla / Pour into cups, press plastic wrap on the surface and chill 2 hours")
D("Butterscotch Pudding", "American", 5, 15, 4, "60 g|butter; 120 g|brown sugar; 500 ml|milk; 3 tbsp|cornstarch; 2|egg yolks; 1 tsp|vanilla extract; 1 pinch|salt",
  "Melt the butter with the brown sugar and cook 3 minutes until bubbling and darker / Whisk the milk, cornstarch, yolks and salt and pour in slowly, whisking / Cook, whisking, until thick, about 5 minutes / Add the vanilla, pour into cups and chill")
D("Coconut Chia Pudding with Mango", "Thai", 10, 0, 4, "400 ml|coconut milk; 200 ml|almond milk; 70 g|chia seeds; 2 tbsp|maple syrup; 1 tsp|vanilla extract; 1|mango, diced",
  "Whisk the coconut milk, almond milk, chia, maple syrup and vanilla / Rest 10 minutes, whisk again to break up clumps, then chill at least 3 hours or overnight / Serve topped with the mango", "Breakfast, Snack, Dessert")
D("Lemon Posset", "British", 5, 5, 4, "450 ml|heavy cream; 120 g|sugar; 2|lemons, zest and juice; 100 g|raspberries",
  "Bring the cream and sugar to a boil and simmer 3 minutes, stirring / Off the heat stir in the lemon zest and juice; it will thicken / Pour into glasses and chill at least 3 hours until set / Top with raspberries")
D("Chocolate Pots de Creme", "French", 10, 30, 6, "300 ml|heavy cream; 150 ml|milk; 150 g|dark chocolate, chopped; 4|egg yolks; 40 g|sugar; 1 pinch|salt",
  "Heat the oven to 150°C (300°F) / Bring the cream and milk to a simmer, pour over the chocolate and stir until smooth / Whisk the yolks, sugar and salt, then whisk in the chocolate cream / Pour into 6 small cups in a roasting tin, add hot water halfway up and bake 25 to 30 minutes until just set with a slight wobble. Chill")
D("Sticky Rice Pudding with Coconut", "Filipino", 5, 30, 6, "200 g|glutinous rice; 400 ml|coconut milk; 400 ml|water; 100 g|brown sugar; 1 pinch|salt; 1 tsp|vanilla extract",
  "Simmer the rice in the water for 15 minutes, stirring / Add the coconut milk, sugar and salt and cook 15 minutes more, stirring often, until thick and creamy / Stir in the vanilla and serve warm or cold")
D("Banana Pudding", "Southern US", 20, 10, 8, "500 ml|milk; 100 g|sugar; 3 tbsp|cornstarch; 3|egg yolks; 1 tsp|vanilla extract; 4|bananas, sliced; 200 g|vanilla wafers; 250 ml|heavy cream",
  "Cook the milk, sugar, cornstarch and yolks, whisking, until thick; stir in the vanilla and cool / Whip the cream to soft peaks and fold half into the custard / Layer wafers, bananas and custard in a dish twice, then spread the remaining cream on top / Chill at least 4 hours so the wafers soften")
D("Rice Pudding with Cardamom and Pistachios", "Indian", 5, 40, 6, "100 g|basmati rice; 1 l|milk; 80 g|sugar; 0.5 tsp|cardamom; 1 pinch|saffron; 30 g|pistachios, chopped",
  "Simmer the rice in the milk over low heat for 35 to 40 minutes, stirring often, until creamy and thick / Stir in the sugar, cardamom and saffron and cook 5 minutes more / Serve warm or chilled with pistachios")

# frozen desserts
D("No-Churn Vanilla Ice Cream", "American", 10, 0, 8, "500 ml|heavy cream; 1 can|sweetened condensed milk; 2 tsp|vanilla extract; 1 pinch|salt",
  "Whip the cream to stiff peaks / Fold in the condensed milk, vanilla and salt / Spread in a loaf tin, cover and freeze at least 6 hours")
D("No-Churn Strawberry Ice Cream", "American", 15, 0, 8, "500 ml|heavy cream; 1 can|sweetened condensed milk; 300 g|strawberries; 1 tbsp|lemon juice",
  "Blend the strawberries with the lemon juice / Whip the cream to stiff peaks and fold in the condensed milk / Fold in the strawberry puree, leaving some ripples / Freeze in a loaf tin for at least 6 hours")
D("Mango Sorbet", "International", 10, 0, 6, "600 g|frozen mango chunks; 3 tbsp|honey; 1|lime, juiced; 60 ml|water",
  "Blend everything in a food processor until completely smooth, scraping down often / Serve straight away as soft-serve, or freeze 1 hour for scoopable sorbet")
D("Lemon Granita", "Italian", 10, 0, 6, "500 ml|water; 150 g|sugar; 4|lemons, zest and juice",
  "Heat the water and sugar until dissolved, then cool and add the lemon zest and juice / Pour into a shallow dish and freeze / Every 30 minutes for 3 hours, scrape with a fork into icy crystals / Serve in chilled glasses")
D("Coffee Granita", "Italian", 5, 0, 6, "500 ml|coffee, strong, hot; 100 g|sugar; 150 ml|heavy cream",
  "Stir the sugar into the hot coffee until dissolved and cool / Freeze in a shallow dish, scraping with a fork every 30 minutes for 3 hours / Serve topped with lightly whipped cream")
D("Berry Yogurt Popsicles", "International", 10, 0, 8, "400 g|greek yogurt; 3 tbsp|honey; 250 g|mixed berries; 1 tsp|vanilla extract",
  "Stir the yogurt with the honey and vanilla / Crush the berries lightly / Layer yogurt and berries in popsicle molds, add sticks and freeze at least 5 hours", "Snack, Dessert")
D("Frozen Banana Bites", "American", 15, 0, 4, "3|bananas, in thick slices; 150 g|dark chocolate; 2 tbsp|peanut butter; 1 tbsp|coconut oil; 2 tbsp|peanuts, chopped",
  "Sandwich pairs of banana slices with a little peanut butter and freeze 30 minutes / Melt the chocolate with the coconut oil / Dip the banana sandwiches, sprinkle with peanuts and freeze on a lined tray for 1 hour", "Snack, Dessert")
D("Chocolate Peanut Butter Nice Cream", "International", 5, 0, 2, "3|bananas, frozen in chunks; 2 tbsp|cocoa powder; 2 tbsp|peanut butter; 3 tbsp|almond milk",
  "Blend everything in a food processor, scraping down, until smooth and creamy / Eat as soft-serve or freeze 1 hour to firm up")
D("Raspberry Semifreddo", "Italian", 20, 0, 8, "300 ml|heavy cream; 3|eggs; 100 g|sugar; 250 g|raspberries; 1 tsp|vanilla extract",
  "Line a loaf tin with plastic wrap. Mash two thirds of the raspberries / Whisk the eggs and sugar in a bowl over simmering water until thick, pale and hot to the touch (about 70°C/160°F), then whisk until cool / Whip the cream with the vanilla and fold into the eggs, then fold in the mashed raspberries and whole berries / Freeze at least 6 hours and slice to serve")

# fruit desserts
D("Grilled Peaches with Honey Yogurt", "American", 5, 6, 4, "4|peaches, halved and stoned; 1 tbsp|vegetable oil; 200 g|greek yogurt; 2 tbsp|honey; 30 g|pistachios, chopped",
  "Brush the cut sides of the peaches with oil / Grill or griddle cut side down 4 to 6 minutes until charred and softened / Serve with yogurt, drizzled with honey and pistachios")
D("Roasted Plums with Vanilla", "French", 5, 20, 4, "8|plums, halved; 3 tbsp|brown sugar; 1 tsp|vanilla extract; 2 tbsp|orange juice; 1 tbsp|butter",
  "Heat the oven to 200°C (400°F) / Put the plums cut side up in a dish, sprinkle with sugar, add the vanilla, juice and dots of butter / Roast 15 to 20 minutes until soft and syrupy / Serve warm with cream or ice cream")
D("Strawberries with Balsamic and Black Pepper", "Italian", 10, 0, 4, "500 g|strawberries, halved; 1 tbsp|balsamic vinegar; 1 tbsp|sugar; 1 pinch|black pepper; 1 handful|basil",
  "Toss the strawberries with the sugar, balsamic and pepper / Leave 15 minutes to release their juices / Scatter with torn basil and serve with mascarpone or ice cream")
D("Baked Pears with Walnuts and Maple", "American", 10, 30, 4, "4|pears, halved and cored; 40 g|walnuts, chopped; 3 tbsp|maple syrup; 0.5 tsp|cinnamon; 1 tbsp|butter",
  "Heat the oven to 190°C (375°F) / Set the pears cut side up in a dish, fill the hollows with walnuts and drizzle with the maple syrup and butter / Sprinkle with cinnamon and bake 25 to 30 minutes until tender")
D("Bananas Foster", "Cajun", 5, 8, 4, "4|bananas, halved lengthways; 60 g|butter; 100 g|brown sugar; 0.5 tsp|cinnamon; 3 tbsp|rum, optional|opt; 4 scoops|vanilla ice cream",
  "Melt the butter with the sugar and cinnamon until bubbling / Add the bananas and cook 2 minutes a side / If using rum, take the pan off the heat, add it and carefully return to the heat to bubble off the alcohol (keep a lid handy) / Spoon over ice cream")
D("Raspberry Fool", "British", 10, 5, 4, "300 g|raspberries; 3 tbsp|sugar; 300 ml|heavy cream; 150 g|greek yogurt",
  "Cook half the berries with the sugar for 5 minutes, then cool / Whip the cream to soft peaks and fold in the yogurt / Ripple through the cooked and fresh berries and spoon into glasses")
D("Eton Mess", "British", 10, 0, 4, "400 g|strawberries, chopped; 2 tbsp|sugar; 300 ml|heavy cream; 4|meringue nests, broken",
  "Toss the strawberries with the sugar and leave 10 minutes / Whip the cream to soft peaks / Fold together the cream, meringue and most of the strawberries and pile into glasses with the rest on top")
D("Caramelized Pineapple with Lime", "Caribbean", 5, 8, 4, "1|pineapple, in wedges; 2 tbsp|brown sugar; 1 tbsp|butter; 1|lime; 1 handful|mint",
  "Toss the pineapple with the sugar / Cook in the butter in a hot pan 3 to 4 minutes a side until caramelized / Squeeze over lime and scatter with mint")

# no-bake and quick
D("Chocolate Truffles", "French", 20, 5, 20, "200 g|dark chocolate, chopped; 150 ml|heavy cream; 1 tbsp|butter; 3 tbsp|cocoa powder",
  "Bring the cream to a simmer and pour over the chocolate and butter; stir until smooth / Chill 2 hours until firm / Scoop into small balls, roll quickly between your palms and toss in cocoa / Keep in the fridge", "Snack, Dessert")
D("No-Bake Chocolate Oat Cookies", "American", 10, 3, 20, "100 g|butter; 200 g|sugar; 120 ml|milk; 30 g|cocoa powder; 120 g|peanut butter; 250 g|rolled oats; 1 tsp|vanilla extract",
  "Bring the butter, sugar, milk and cocoa to a rolling boil and boil exactly 1 minute / Off the heat stir in the peanut butter, vanilla and oats / Drop spoonfuls onto parchment and leave 30 minutes to set", "Snack, Dessert")
D("Rocky Road", "British", 15, 5, 16, "300 g|dark chocolate; 100 g|butter; 3 tbsp|golden syrup; 150 g|digestive biscuits, broken; 100 g|marshmallows; 80 g|dried cherries",
  "Melt the chocolate, butter and syrup together gently / Stir in the biscuits, marshmallows and cherries / Press into a lined 20 cm square tin and chill 2 hours / Cut into squares", "Snack, Dessert")
D("Icebox Cake", "American", 20, 0, 8, "500 ml|heavy cream; 3 tbsp|powdered sugar; 1 tsp|vanilla extract; 250 g|chocolate wafer cookies; 20 g|dark chocolate, grated",
  "Whip the cream with the sugar and vanilla to soft peaks / In a loaf tin lined with plastic wrap, layer cookies and cream, finishing with cream / Chill overnight so the cookies soften into cake / Turn out and top with grated chocolate")
D("No-Bake Peanut Butter Bars", "American", 15, 2, 16, "200 g|graham crackers, crushed; 100 g|butter, melted; 200 g|peanut butter; 150 g|powdered sugar; 200 g|chocolate chips",
  "Mix the crumbs, butter, peanut butter and sugar and press into a lined 20 cm square tin / Melt the chocolate chips and spread over / Chill 1 hour and cut into bars", "Snack, Dessert")
D("Microwave Peanut Butter Mug Cake", "American", 3, 2, 1, "4 tbsp|flour; 2 tbsp|sugar; 2 tbsp|peanut butter; 0.25 tsp|baking powder; 3 tbsp|milk; 1 tbsp|vegetable oil",
  "Stir everything together in a large mug until smooth / Microwave 60 to 90 seconds until risen and just set; ovens vary, so check after 60 seconds / Cool 1 minute before eating")
D("Lemon Mug Cake", "American", 3, 2, 1, "4 tbsp|flour; 2 tbsp|sugar; 0.25 tsp|baking powder; 1|egg; 1 tbsp|butter, melted; 1|lemon, zest and 1 tbsp juice",
  "Whisk everything in a large mug / Microwave 60 to 80 seconds until risen / Drizzle with a little extra lemon juice mixed with sugar")
D("Chocolate Dipped Strawberries", "International", 15, 3, 4, "400 g|strawberries; 150 g|dark chocolate; 30 g|white chocolate|opt",
  "Pat the strawberries completely dry / Melt the dark chocolate in short bursts in the microwave, stirring / Dip each berry and set on parchment; drizzle with melted white chocolate if you like / Chill 20 minutes to set", "Snack, Dessert")
D("Affogato", "Italian", 2, 2, 2, "4 scoops|vanilla ice cream; 2 shots|espresso, hot; 1 tbsp|dark chocolate, grated",
  "Put 2 scoops of ice cream in each glass / Pour a shot of hot espresso over each / Top with grated chocolate and serve straight away")

# cakes, tarts and baked desserts
D("Cheesecake Bars", "American", 20, 35, 16, "200 g|graham crackers, crushed; 80 g|butter, melted; 500 g|cream cheese, soft; 120 g|sugar; 2|eggs; 100 g|sour cream; 1 tsp|vanilla extract; 1|lemon, zested",
  "Heat the oven to 160°C (325°F) and line a 20 cm square tin / Mix the crumbs and butter and press into the tin; bake 8 minutes / Beat the cream cheese and sugar until smooth, then the eggs, sour cream, vanilla and zest / Pour over the base and bake 30 to 35 minutes until set with a slight wobble. Cool, then chill 4 hours before cutting")
D("Flourless Chocolate Cake", "French", 15, 30, 10, "200 g|dark chocolate; 150 g|butter; 150 g|sugar; 4|eggs; 30 g|cocoa powder; 1 pinch|salt",
  "Heat the oven to 180°C (350°F) and line a 20 cm round tin / Melt the chocolate and butter together, then whisk in the sugar / Whisk in the eggs one at a time, then the cocoa and salt / Bake 25 to 30 minutes until the top is set but the middle is still a little soft. Cool in the tin; it sinks and becomes fudgy")
D("Rustic Apple Galette", "French", 20, 40, 8, "1|pie crust; 3|apples, thinly sliced; 50 g|sugar; 1 tsp|cinnamon; 1 tbsp|flour; 1 tbsp|butter; 1|egg, beaten; 2 tbsp|apricot jam",
  "Heat the oven to 200°C (400°F). Roll the pastry to a 30 cm circle on a lined tray / Toss the apples with the sugar, cinnamon and flour and pile in the middle, leaving a 5 cm border / Fold the border over the fruit, dot with butter and brush the pastry with egg / Bake 35 to 40 minutes until golden. Brush the apples with warm jam")
D("Pineapple Upside-Down Cake", "American", 20, 40, 10, "180 g|butter (60 g melted, 120 g soft); 100 g|brown sugar; 1 can|pineapple rings, drained; 8|maraschino cherries|opt; 180 g|flour; 1.5 tsp|baking powder; 150 g|sugar; 2|eggs; 120 ml|milk; 1 tsp|vanilla extract",
  "Heat the oven to 180°C (350°F). Pour the melted butter into a 23 cm round tin, sprinkle with the brown sugar and arrange the pineapple (and cherries) on top / Cream the soft butter and sugar, beat in the eggs and vanilla, then fold in the flour and baking powder with the milk / Spread over the fruit and bake 40 minutes until a skewer comes out clean / Cool 10 minutes, then turn out onto a plate")
D("Cherry Clafoutis", "French", 10, 35, 6, "400 g|cherries, pitted; 3|eggs; 80 g|sugar; 60 g|flour; 300 ml|milk; 1 tsp|vanilla extract; 1 tbsp|butter; 1 tbsp|powdered sugar",
  "Heat the oven to 180°C (350°F) and butter a 23 cm dish / Spread the cherries in the dish / Whisk the eggs and sugar, then the flour, then the milk and vanilla to a smooth batter / Pour over the cherries and bake 35 minutes until puffed and set. Dust with powdered sugar and serve warm")
D("Molten Chocolate Lava Cakes", "French", 15, 12, 4, "120 g|dark chocolate; 100 g|butter; 4|eggs, 2 whole and 2 yolks only; 50 g|sugar; 2 tbsp|flour; 1 tsp|cocoa powder",
  "Heat the oven to 220°C (425°F). Butter 4 ramekins and dust with cocoa / Melt the chocolate and butter together / Whisk the 2 eggs, 2 yolks and sugar until thick, fold in the chocolate, then the flour / Divide between the ramekins and bake 10 to 12 minutes until the edges are firm but the centres wobble. Rest 1 minute and turn out")
D("Sticky Toffee Pudding", "British", 20, 35, 8, "200 g|dates, chopped; 250 ml|boiling water; 1 tsp|baking soda; 80 g|butter, soft; 150 g|brown sugar; 2|eggs; 180 g|self-raising flour; 200 ml|heavy cream; 1 tbsp|golden syrup",
  "Heat the oven to 180°C (350°F) and butter a 20 x 30 cm dish / Soak the dates in the boiling water with the baking soda for 10 minutes / Cream 50 g butter with 100 g sugar, beat in the eggs, fold in the flour, then the dates and their liquid. Bake 30 to 35 minutes / For the sauce, simmer the rest of the butter and sugar with the cream and syrup for 3 minutes. Pour over the warm pudding")
D("Puff Pastry Apple Turnovers", "French", 20, 20, 8, "1 sheet|puff pastry; 2|apples, finely diced; 40 g|sugar; 0.5 tsp|cinnamon; 1 tbsp|butter; 1|egg, beaten",
  "Cook the apples with the butter, sugar and cinnamon for 5 minutes until just soft, then cool / Heat the oven to 200°C (400°F). Cut the pastry into 8 squares / Put a spoonful of apple on each, fold into triangles and press the edges with a fork / Brush with egg, cut a small slit in each and bake 18 to 20 minutes until puffed and golden", "Dessert, Baked goods, Snack")
D("Pavlova with Berries", "Australian", 20, 75, 8, "4|egg whites; 220 g|sugar; 1 tsp|cornstarch; 1 tsp|white wine vinegar; 300 ml|heavy cream; 400 g|mixed berries",
  "Heat the oven to 120°C (250°F) and line a tray / Whisk the whites to soft peaks, then add the sugar a spoonful at a time, whisking to a thick glossy meringue. Fold in the cornstarch and vinegar / Spread into a 20 cm round with a dip in the middle and bake 75 minutes, then cool in the switched-off oven / Top with whipped cream and berries just before serving")
D("Chocolate Bread Pudding", "American", 15, 40, 8, "300 g|bread, stale, cubed; 100 g|chocolate chips; 400 ml|milk; 200 ml|heavy cream; 3|eggs; 80 g|sugar; 2 tbsp|cocoa powder; 1 tsp|vanilla extract",
  "Heat the oven to 180°C (350°F) and butter a baking dish / Whisk the milk, cream, eggs, sugar, cocoa and vanilla / Toss the bread and chocolate chips in the dish, pour over the custard and press down; soak 15 minutes / Bake 35 to 40 minutes until set and crisp on top")
D("Peach Galette with Almond", "French", 20, 40, 8, "1|pie crust; 4|peaches, sliced; 50 g|sugar; 1 tbsp|cornstarch; 30 g|almond flour; 1|egg, beaten; 20 g|almonds, sliced",
  "Heat the oven to 200°C (400°F) and roll the pastry onto a lined tray / Sprinkle the almond flour in the middle, leaving a 5 cm border / Toss the peaches with sugar and cornstarch and pile on top, then fold up the edges / Brush with egg, scatter with almonds and bake 35 to 40 minutes until golden")
D("Vegan Chocolate Mousse with Aquafaba", "French", 20, 0, 4, "1 can|chickpeas, liquid only (keep the chickpeas for another recipe); 0.25 tsp|cream of tartar; 50 g|sugar; 150 g|dark chocolate, dairy-free, melted and cooled",
  "Whip the chickpea liquid with the cream of tartar to soft peaks, about 5 minutes / Add the sugar gradually and whip to stiff glossy peaks / Fold in the cooled chocolate gently in three parts / Spoon into glasses and chill at least 4 hours")
D("Baked Custard Tarts", "Portuguese", 20, 20, 12, "1 sheet|puff pastry; 3|egg yolks; 80 g|sugar; 1 tbsp|cornstarch; 250 ml|milk; 1 tsp|vanilla extract; 0.5 tsp|cinnamon",
  "Heat the oven to 240°C (475°F). Roll the pastry into a log, cut into 12 rounds and press each into a muffin tin cup / Cook the yolks, sugar, cornstarch and milk over medium heat, whisking, until thick, then add the vanilla / Fill the pastry cups three quarters full / Bake 15 to 20 minutes until the pastry is crisp and the tops have dark spots. Dust with cinnamon", "Dessert, Baked goods")
