# Data notes (v2.1)

## Caribbean dishes: country tags and disputed origins
Caribbean recipes are tagged with one country each (v2.0 Stage 1). Where a dish is shared or its origin is debated,
it is tagged with **the most common attribution**. Shared or disputed dishes are listed below. A tag means "often
associated with". It does not claim where the dish was invented.

| Recipe(s) | Tagged | Also claimed by / shared with |
|---|---|---|
| Arroz con Pollo | Cuban | Puerto Rico, Dominican Republic, Spain, much of Latin America |
| Picadillo, Cuban Picadillo | Cuban | Spain, Puerto Rico, Mexico, the Philippines |
| Ropa Vieja | Cuban | Canary Islands/Spain (origin), Puerto Rico, Panama |
| Cuban-Style Pressed Sandwiches | Cuban | Tampa and Miami (US) vs Havana: long-running dispute |
| Tostones | Puerto Rican | Dominican Republic, Cuba, Colombia/Panama ("patacones") |
| Puerto Rican Mofongo | Puerto Rican | West African fufu roots; Dominican and Cuban plantain mashes |
| Sancocho-Style Chicken Stew | Dominican | Colombia, Panama, Puerto Rico, Venezuela, Ecuador |
| Caribbean Callaloo | Trinidadian & Tobagonian | Jamaica, Grenada, Dominica, Guyana (recipes differ a lot) |
| Trinidad Chicken Pelau | Trinidadian & Tobagonian | Barbados, Guyana, St Kitts; related to pilau/pilaf |
| Trinidad/Trinidadian Curry Chickpeas (Channa) | Trinidadian & Tobagonian | Guyana, Suriname; Indian roots |
| Bajan Cou-Cou with Okra | Bajan | Antigua and the Virgin Islands ("fungee"/"fungi") |
| Bahamian Conch-Style Fritters | Bahamian | Turks & Caicos, Florida Keys |
| Guyanese Pepperpot-Style Beef Stew | Guyanese | Indigenous roots. Antigua's and Jamaica's pepperpots are different dishes with the same name |
| Rice and Peas (all versions) | Jamaican | Much of the English-speaking Caribbean |
| Curry Goat-Style Lamb Curry, Jamaican Curry Chicken | Jamaican | Trinidad & Tobago, Guyana |
| Jamaican Sorrel Drink, Homemade Ginger Beer | Jamaican | Trinidad & Tobago and the wider Caribbean |
| Grenadian Oil Down | Grenadian | Trinidad, Barbados and St Lucia have similar one-pots |
| Jerk Chicken, Jamaican Braised Oxtail, Brown Stew Chicken | Jamaican | Seldom disputed (jerk has Taíno and Maroon roots) |
| Dominican Mangú | Dominican | Seldom disputed. Similar mashes exist in Cuba and Puerto Rico |
| Haitian Griot, Haitian Pikliz | Haitian | Seldom disputed |

Corrections are welcome as a change to the recipe files. This note does not settle any of these debates.

## Carnivore (v2.0 Stage 2)
The rule allows meat, fish, seafood, eggs, organ meats, bone broth, animal fats and dairy, plus salt, pepper and
water. Plants are excluded, and so are processed meats such as bacon and sausages. 63 original carnivore recipes were added
in `recipes/82-v20-carnivore.txt`. Like every diet, the flag is worked out at build time and labelled "a guide, not certification".

## Botanical fruits (v2.1)
The 🍅 "Botanically a fruit" badge is shown for 24 ingredients: Tomato, Cherry tomatoes, Cucumber, Avocado, Bell pepper,
Chili, Jalapeno, Poblano, Habanero, Eggplant, Zucchini, Pumpkin, Acorn squash, Spaghetti squash, Delicata squash, Olives,
Tomatillos, Okra, Green beans, Snow peas, Sweetcorn, Chayote, Breadfruit, Plantains.
- A botanical fruit is the part of a plant that grows from its flower and holds its seeds. The list covers ingredients that
  cooks treat as vegetables.
- Sweet fruits (apples, berries and so on) are already under Fruits, so they get no badge.
- Peas and edamame are left out because we eat the seeds, not the pod. Snow peas and green beans are included because the
  whole pod is eaten.
- Grains and nuts are botanically fruits too, but they are not marked, to keep the list useful.
- Categories are a cooking choice. Tomato, cherry tomatoes, cucumber and avocado are under Fruits (user request). Peppers,
  eggplant, zucchini, pumpkin and squash stay in Vegetables. Anyone can move any ingredient in Settings.
- Fun fact source: Nix v. Hedden, 149 U.S. 304 (1893).

## Buy these next (v2.1)
- The "Buy these next" picks are greedy: each pick completes the most recipes, counting the earlier picks as bought.
- "See all" ranks every single missing ingredient on its own: first by recipes completed, then by recipes that use it.
- Prices are your own when set. Otherwise they are the built-in averages, when that setting is on: BLS regional averages
  for the US, rough defaults elsewhere.
- "Cheapest" compares the pack price ("$2.50 for 500 g"), not the amount a recipe uses.

## Recipe counts (v2.1)
- 2,620 recipes, 460 of them new.
- Per category: Main 1195, Breakfast 235, Side 192, Soup 189, Baking 174, Snack 174, Dessert 129, Drink 126,
  Salad 115, Sauce 91. Carnivore: 160.
- The new recipes are written for this app (MIT License). Measurements are in the usual kitchen units, and the baking
  recipes are in grams.
