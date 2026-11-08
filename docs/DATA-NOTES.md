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

## Botanical fruits (v2.1, removed in v2.2)
The v2.1 botanical-fruit badge and filter were removed in v2.2. You can now move any ingredient to another category yourself.

## v2.2 dinner mains
- 1,154 recipes in recipes/95-v22-chicken.txt … 99-v22c-shrimp.txt. These file names sort last, so ids b:1–b:2620 do not change.
- The generator is tools/morerecipes/build_v22.py, using v22cuts.py (24 cuts, with method times) and v22cuisines.py (27 cuisines, 165 flavour sets).
- A cut is only paired with methods that suit it: stir-fry, braise, roast/grill, skillet, or ground.
- Halal-leaning cuisines skip pork. Indian skips beef and pork.
- Doneness temperatures: poultry 74 °C/165 °F; pork and whole cuts of beef and lamb 63 °C/145 °F plus rest; ground red meat 71 °C/160 °F; fish 63 °C.
- 35 candidates were skipped as near-duplicates of existing titles.

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
