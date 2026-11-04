"""v2.1: more carnivore recipes (meat, fish, eggs, dairy, animal fats, salt, pepper, water only)."""
from v21lib import R
F = "84-v21-carnivore.txt"
def C(t, prep, cook, serv, ings, steps, meals="Dinner, Main dish", cat="Main"):
    R(F, t, cat, "American" if not isinstance(meals, tuple) else meals[1], prep, cook, serv, meals if not isinstance(meals, tuple) else meals[0], ings, steps)

# steaks: cut, ingredient line, thickness note, pan minutes per side (medium-rare), grill minutes per side
CUTS = [("Ribeye", "2|ribeye steaks, about 2.5 cm thick", 4, 5), ("Sirloin", "2|sirloin steaks, about 2.5 cm thick", 3, 4),
        ("Flat Iron", "2|flat iron steaks", 3, 4), ("Skirt Steak", "600 g|skirt steak", 2, 3), ("Hanger Steak", "600 g|hanger steak", 4, 5),
        ("T-Bone", "2|t-bone steaks, about 3 cm thick", 5, 6), ("Rump Steak", "2|rump steaks", 3, 4), ("Bavette", "600 g|bavette steak", 3, 4)]
for name, ing, pan, grill in CUTS:
    serv = 2 if ing.startswith("2|") else 3
    C("Butter-Basted " + name, 5, pan * 2 + 6, serv, ing + "; 1 tsp|salt; 40 g|butter; 0.5 tsp|black pepper",
      ["Pat the steak dry and salt it on both sides 30 minutes ahead if you have time (or just before cooking)",
       "Heat a heavy frying pan over high heat until smoking. Cook the steak %d minutes on the first side without moving it" % pan,
       "Turn it, add the butter and, as it foams, tilt the pan and spoon the butter over the steak for %d minutes for medium-rare (54 °C inside), longer if you like it more done" % pan,
       "Rest the steak 5 minutes, slice across the grain and pour over the pan butter. Finish with the pepper"])
    C("Tallow-Grilled " + name, 5, grill * 2 + 5, serv, ing + "; 1 tsp|salt; 1 tbsp|beef tallow, melted; 0.5 tsp|black pepper",
      ["Heat a grill or griddle pan until very hot. Brush the steak with the melted tallow and salt it well",
       "Grill %d minutes on each side for medium-rare, turning once; a thermometer should read 54 °C in the thickest part" % grill,
       "Rest 5 minutes on a warm plate, then slice across the grain and season with the pepper"])
    C("Air Fryer " + name, 5, grill * 2 + 2, serv, ing + "; 1 tsp|salt; 2 tsp|butter, melted",
      ["Preheat the air fryer to 200 °C for 5 minutes. Pat the steak dry, brush with the butter and salt both sides",
       "Air fry for %d minutes, turn and air fry %d minutes more for medium-rare (54 °C inside). Thinner cuts need a minute or two less" % (grill, grill),
       "Rest 5 minutes before slicing across the grain"])

# chops and roasts
C("Pan-Fried Pork Chops in Butter", 5, 14, 2, "2|pork chops, bone-in, about 2 cm thick; 1 tsp|salt; 30 g|butter; 0.5 tsp|black pepper",
  "Salt the chops and leave 15 minutes. Pat dry / Fry in a hot pan for 4 to 5 minutes per side until golden and 63 °C inside, adding the butter for the last 2 minutes and spooning it over / Rest 3 minutes and season with pepper")
C("Air Fryer Pork Chops", 5, 12, 2, "2|pork chops, boneless; 1 tsp|salt; 2 tsp|lard, melted",
  "Preheat the air fryer to 200 °C. Brush the chops with lard and salt them / Air fry 6 minutes, turn and air fry 5 to 6 minutes more until 63 °C inside / Rest 3 minutes")
C("Lamb Chops with Browned Butter", 5, 10, 2, "6|lamb loin chops; 1 tsp|salt; 40 g|butter; 0.5 tsp|black pepper",
  "Salt the chops. Stand them fat side down in a hot dry pan for 2 minutes to render the fat / Lay them flat and cook 3 minutes per side for pink / Lift out, add the butter and cook until nutty brown, then pour over the chops with the pepper")
C("Air Fryer Lamb Chops", 5, 10, 2, "6|lamb loin chops; 1 tsp|salt; 0.5 tsp|black pepper",
  "Preheat the air fryer to 200 °C. Salt the chops / Air fry 5 minutes, turn and air fry 4 to 5 minutes more for pink (58 °C) / Rest 3 minutes and add the pepper")
C("Slow-Roasted Pork Shoulder with Crackling", 15, 300, 8, "2.5 kg|pork shoulder, skin on and scored; 1 tbsp|salt; 250 ml|water",
  "Dry the skin well and rub the salt into the scores. Leave uncovered in the fridge overnight if you can / Roast at 160 °C with the water in the tin for 4½ hours, topping up the water if it dries / Turn the oven to 230 °C for 20 to 30 minutes until the skin crackles all over / Rest 20 minutes, then pull or slice", meals="Dinner, Main dish")
C("Slow Cooker Pulled Pork Shoulder", 10, 480, 8, "2 kg|pork shoulder, boneless; 1 tbsp|salt; 1 tsp|black pepper; 150 ml|bone broth",
  "Rub the pork all over with the salt and pepper / Put it in the slow cooker with the broth, cover and cook on Low for 8 hours until it pulls apart / Shred with two forks and toss with some of the cooking juices")
C("Slow Cooker Beef Brisket", 10, 540, 8, "2 kg|beef brisket; 1 tbsp|salt; 1 tsp|black pepper; 250 ml|beef stock; 1 tbsp|beef tallow",
  "Season the brisket. Brown it all over in the tallow in a hot pan / Put it fat side up in the slow cooker with the stock. Cover and cook on Low for 9 hours until tender / Rest 15 minutes, slice across the grain and spoon over the juices")
C("Slow Cooker Beef Short Ribs", 10, 480, 4, "1.5 kg|beef short ribs; 2 tsp|salt; 300 ml|bone broth; 0.5 tsp|black pepper",
  "Salt the ribs and brown them in a hot dry pan, in batches / Put them in the slow cooker with the broth. Cover and cook on Low for 8 hours / Skim the fat from the juices and pour them over the ribs with the pepper")
C("Slow Cooker Lamb Shanks", 10, 480, 4, "4|lamb shanks; 2 tsp|salt; 400 ml|bone broth; 1 tbsp|ghee",
  "Salt the shanks and brown them in the ghee / Move them to the slow cooker with the broth. Cover and cook on Low for 7 to 8 hours until the meat falls from the bone / Skim the juices and spoon over")
C("Slow Cooker Oxtail in Bone Broth", 10, 540, 4, "1.5 kg|oxtail pieces; 2 tsp|salt; 500 ml|bone broth; 0.5 tsp|black pepper",
  "Salt and brown the oxtail in a hot pan / Put in the slow cooker with the broth. Cover and cook on Low for 9 hours / Chill the juices briefly so the fat sets, lift it off and reheat the oxtail in the juices")
C("Instant Pot Beef Chuck Roast", 10, 105, 6, "1.5 kg|beef chuck roast; 2 tsp|salt; 1 tbsp|beef tallow; 250 ml|beef stock",
  ["Salt the beef. Select Sauté and brown it all over in the tallow, about 8 minutes",
   "Pour in the stock and scrape the bottom of the inner pot clean so it doesn't show a burn warning",
   "Lock the lid, set the valve to Sealing and pressure cook on High for 70 minutes. The pot takes 10 to 15 minutes to come up to pressure first",
   "Let the pressure release naturally for 15 minutes, then release the rest. Slice or pull the beef and serve with the juices"])
C("Instant Pot Pork Belly Bites", 10, 60, 4, "1 kg|pork belly, cut into 3 cm cubes; 1.5 tsp|salt; 250 ml|water",
  ["Put the pork and water in the pot. Lock the lid, set the valve to Sealing and pressure cook on High for 20 minutes",
   "Release the pressure naturally for 10 minutes, then quick release. Lift out the pork and pour off the liquid",
   "Select Sauté, return the pork with the salt and fry in its own fat, turning, for 10 minutes until crisp on all sides"])
C("Instant Pot Chicken Thighs in Broth", 5, 40, 4, "8|chicken thighs, bone-in and skin on; 1.5 tsp|salt; 250 ml|chicken broth; 20 g|butter",
  ["Salt the thighs. Select Sauté and brown them skin side down in the butter, in batches",
   "Pour in the broth and scrape the bottom clean, then return the chicken",
   "Lock the lid, set the valve to Sealing and pressure cook on High for 10 minutes. Natural release for 10 minutes, then release the rest",
   "Check they are 75 °C inside. Serve with the broth spooned over"])
C("Instant Pot Beef Bone Broth", 10, 180, 8, "2 kg|beef marrow bones; 2 l|water; 2 tsp|salt",
  ["Roast the bones at 220 °C for 30 minutes for a deeper flavour (optional)",
   "Put the bones in the pot and add the water up to the max line. Lock the lid, set to Sealing and pressure cook on High for 2 hours",
   "Let the pressure release naturally. Strain, season with the salt and chill; lift off the fat to cook with"])

# poultry
for t, ing, mins, how in [
    ("Crispy Air Fryer Chicken Wings", "1 kg|chicken wings", 24, "wings"), ("Air Fryer Chicken Drumsticks", "8|chicken drumsticks", 25, "drums"),
    ("Air Fryer Crispy Chicken Thighs", "6|chicken thighs, bone-in and skin on", 24, "thighs"), ("Air Fryer Duck Legs", "4|duck legs", 30, "duck")]:
    C(t, 5, mins, 4, ing + "; 1.5 tsp|salt; 0.5 tsp|black pepper",
      ["Pat the %s very dry and salt them. For extra-crisp skin leave them uncovered in the fridge for an hour" % ("chicken" if how != "duck" else "duck"),
       "Preheat the air fryer to 190 °C. Lay them in one layer, skin side down, in batches if needed",
       "Air fry %d minutes, turning halfway, then 3 to 4 minutes at 200 °C to crisp. They are done at 75 °C inside" % (mins - 4),
       "Season with the pepper and serve hot"])
C("Pan-Seared Duck Breast", 5, 18, 2, "2|duck breasts; 1 tsp|salt; 0.5 tsp|black pepper",
  "Score the skin in a criss-cross without cutting the meat and salt well / Lay skin side down in a cold pan, then put it over medium heat. Cook 10 to 12 minutes, pouring off the fat now and then, until the skin is crisp / Turn and cook 3 minutes for pink. Rest 5 minutes and slice; keep the fat for cooking")
C("Butter-Roasted Whole Chicken", 10, 85, 4, "1.6 kg|whole chicken; 2 tsp|salt; 40 g|butter, soft; 0.5 tsp|black pepper",
  "Dry the chicken and salt it inside and out. Rub the butter all over / Roast at 200 °C for 75 to 85 minutes until the juices at the thigh run clear and it reads 75 °C / Rest 15 minutes before carving; spoon the pan juices over")
C("Spatchcock Chicken with Schmaltz", 15, 50, 4, "1.6 kg|whole chicken; 2 tsp|salt; 2 tbsp|schmaltz",
  "Cut out the backbone with scissors and press the chicken flat. Salt it well / Brush with the schmaltz and roast skin up at 220 °C for 45 to 50 minutes until 75 °C at the thigh / Rest 10 minutes and cut into pieces")
C("Slow Cooker Whole Chicken", 10, 360, 4, "1.6 kg|whole chicken; 2 tsp|salt; 0.5 tsp|black pepper",
  "Salt the chicken inside and out / Sit it breast up in the slow cooker with no liquid; it makes its own. Cover and cook on Low for 6 hours until 75 °C at the thigh / Crisp the skin under a hot grill for 5 minutes if you like. Keep the juices for broth")
C("Turkey Thigh Roast with Butter", 10, 90, 4, "2|turkey thighs, bone-in; 2 tsp|salt; 40 g|butter",
  "Salt the thighs and rub with the butter / Roast at 180 °C for 80 to 90 minutes, basting twice, until 75 °C inside / Rest 10 minutes and slice off the bone")
C("Chicken Liver Pâté with Butter", 10, 10, 6, "400 g|chicken livers, trimmed; 150 g|butter, soft; 1 tsp|salt; 0.5 tsp|black pepper; 2 tbsp|heavy cream",
  "Fry the livers in 30 g of the butter over medium-high heat for 3 minutes per side until browned but just pink inside / Blend with the rest of the butter, the cream, salt and pepper until smooth / Press into a dish and chill for 2 hours", meals="Appetizer, Snack", cat="Snack")
C("Butter-Fried Chicken Livers", 5, 8, 2, "400 g|chicken livers, trimmed; 30 g|butter; 0.75 tsp|salt",
  "Pat the livers dry and salt them / Fry in the foaming butter over medium-high heat, 2 to 3 minutes per side, until browned and only just pink inside / Serve with the pan butter")
C("Pan-Fried Beef Liver in Butter", 5, 6, 2, "400 g|beef liver, sliced 1 cm thick; 30 g|butter; 0.75 tsp|salt; 0.25 tsp|black pepper",
  "Pat the liver dry and salt it / Fry in foaming butter over high heat 1½ to 2 minutes per side; overcooking makes it tough / Rest a minute and add the pepper")
C("Seared Beef Heart Steaks", 10, 6, 3, "600 g|beef heart, trimmed and sliced 1.5 cm thick; 1 tsp|salt; 1 tbsp|beef tallow",
  "Trim any sinew and fat from the heart and slice it. Salt well / Sear in very hot tallow 2 to 3 minutes per side for medium-rare / Rest 3 minutes and slice thinly across the grain")

# ground meat
for meat, ing in [("Beef", "800 g|ground beef"), ("Lamb", "800 g|ground lamb"), ("Pork", "800 g|ground pork"), ("Venison", "800 g|ground venison")]:
    C(meat + " Smash Patties" if meat != "Beef" else "Crispy Smash Patties", 5, 10, 4, ing + "; 1 tsp|salt; 1 tbsp|beef tallow; 4 slices|cheddar|opt",
      "Roll the meat into 8 loose balls / Heat a heavy pan or griddle until smoking and add a little tallow. Put in 2 balls and press each flat with a sturdy spatula / Salt, cook 2 minutes until deeply browned, turn, add cheese if using and cook 1 minute more / Repeat with the rest")
C("Carnivore Meatballs", 10, 20, 4, "800 g|ground beef; 1|egg; 40 g|parmesan, finely grated; 1 tsp|salt; 1 tbsp|butter",
  "Mix the beef, egg, parmesan and salt with your hands until just combined / Roll into 20 balls / Brown in the butter over medium heat for 10 minutes, then cover and cook 6 to 8 minutes more until cooked through")
C("Parmesan Beef Air Fryer Meatballs", 10, 12, 4, "800 g|ground beef; 1|egg; 40 g|parmesan, finely grated; 1 tsp|salt",
  "Mix everything and roll into 20 balls / Air fry at 190 °C for 10 to 12 minutes, shaking the basket halfway, until browned and 71 °C inside")
C("Beef and Egg Meatloaf", 10, 60, 6, "1 kg|ground beef; 2|eggs; 60 g|parmesan, grated; 1.5 tsp|salt; 0.5 tsp|black pepper",
  "Heat the oven to 180 °C. Mix everything well with your hands / Press into a loaf tin and bake 55 to 60 minutes until 71 °C in the middle / Pour off the fat and rest 10 minutes before slicing")
C("Lamb Kofta Skewers (Salt and Pepper)", 15, 10, 4, "800 g|ground lamb; 1 tsp|salt; 0.5 tsp|black pepper",
  "Knead the lamb with the salt and pepper for 2 minutes until sticky / Shape around 8 skewers into long sausages / Grill on high heat 8 to 10 minutes, turning, until browned and cooked through")
C("Instant Pot Ground Beef Chili (Meat Only)", 5, 35, 4, "1 kg|ground beef; 1.5 tsp|salt; 250 ml|beef stock; 100 g|cheddar, grated|opt; 4 tbsp|sour cream|opt",
  ["Select Sauté and brown the beef with the salt, breaking it up, for 8 minutes",
   "Pour in the stock and scrape the bottom clean. Lock the lid, set to Sealing and pressure cook on High for 10 minutes",
   "Quick release. Simmer on Sauté for 5 minutes to thicken and serve with the cheese and sour cream if you like"])

# seafood
for fish, ing, oven, air in [("Salmon", "4|salmon fillets", 12, 9), ("Cod", "4|cod fillets", 12, 10), ("Trout", "4|trout fillets", 9, 8),
                             ("Mackerel", "4|mackerel fillets", 8, 7), ("Halibut", "4|halibut fillets", 12, 10), ("Sea Bass", "4|sea bass fillets", 8, 7)]:
    C("Butter-Seared " + fish, 5, 8, 4, ing + "; 1 tsp|salt; 40 g|butter",
      "Pat the fish dry and salt it / Sear skin side down (or presentation side down) in a hot pan with half the butter for 3 to 4 minutes until crisp / Turn, add the rest of the butter and baste for 1 to 2 minutes until just cooked through")
    C("Buttered Air Fryer " + fish, 2, air, 4, ing + "; 1 tsp|salt; 1 tbsp|butter, melted",
      "Preheat the air fryer to 200 °C. Brush the fish with butter and salt it / Air fry skin side down for %d minutes, without turning, until it flakes / Serve straight away" % air)
C("Garlic-Free Butter Shrimp", 5, 5, 3, "500 g|shrimp, peeled; 40 g|butter; 0.75 tsp|salt",
  "Pat the shrimp dry and salt them / Cook in foaming butter over medium-high heat for 2 minutes per side until pink and curled / Serve with the butter")
C("Seared Scallops in Brown Butter", 5, 5, 2, "12|scallops; 30 g|butter; 0.5 tsp|salt",
  "Dry the scallops well on kitchen paper and salt them / Sear in a very hot pan with a little of the butter, 90 seconds per side, without moving / Add the rest of the butter, let it brown and spoon over")
C("Broiled Tuna Steaks", 5, 6, 2, "2|tuna steaks; 1 tsp|salt; 1 tbsp|ghee",
  "Brush the tuna with ghee and salt it / Cook under a very hot grill or broiler 2 to 3 minutes per side for pink in the middle / Slice and serve")
C("Steamed Mussels in Bone Broth", 10, 8, 2, "1 kg|mussels, cleaned; 250 ml|bone broth; 30 g|butter",
  "Throw away any mussels that are cracked or stay open when tapped / Bring the broth and butter to a boil, add the mussels, cover and cook 4 to 5 minutes, shaking the pan, until they open / Throw away any that stay shut")
C("Crab and Egg Scramble", 5, 5, 2, "5|eggs; 150 g|crab meat; 20 g|butter; 0.5 tsp|salt",
  "Beat the eggs with the salt / Melt the butter over low heat, add the eggs and stir slowly / When almost set, fold in the crab and warm through", meals="Breakfast, Brunch", cat="Breakfast")
C("Sardines on Fried Eggs", 3, 5, 2, "4|eggs; 1 can|sardines, in water, drained; 20 g|butter; 0.25 tsp|salt",
  "Fry the eggs in the butter until the whites set / Warm the sardines in the same pan for a minute / Serve the sardines on the eggs with the salt", meals="Breakfast, Lunch", cat="Breakfast")
C("Smoked Salmon Egg Bites", 10, 12, 4, "6|eggs; 60 ml|heavy cream; 0.5 tsp|salt; 80 g|smoked salmon, chopped",
  "Heat the oven to 160 °C and butter a 12-hole mini muffin tin / Whisk the eggs, cream and salt, stir in the salmon and pour into the holes / Bake 12 minutes until just set. Cool a little before turning out", meals="Breakfast, Snack", cat="Breakfast")

# eggs and dairy
C("Three-Egg Butter Omelette with Cheese", 2, 4, 1, "3|eggs; 15 g|butter; 30 g|gruyere, grated; 0.25 tsp|salt",
  "Beat the eggs with the salt / Melt the butter in a nonstick pan over medium heat, pour in the eggs and stir with a fork until softly set / Scatter over the cheese, fold and slide onto a plate", meals="Breakfast, Brunch", cat="Breakfast")
C("Slow Soft Scrambled Eggs with Cream", 2, 8, 2, "5|eggs; 30 g|butter; 2 tbsp|heavy cream; 0.25 tsp|salt",
  "Put the eggs and butter in a cold pan over low heat / Stir constantly with a spatula for 6 to 8 minutes, taking the pan off the heat now and then, until creamy / Stir in the cream and salt", meals="Breakfast, Brunch", cat="Breakfast")
C("Cheese and Egg Waffles (Chaffles)", 5, 10, 2, "2|eggs; 100 g|mozzarella, grated; 1 pinch|salt",
  "Heat a waffle iron. Mix the eggs, mozzarella and salt / Spoon half into the iron and cook 4 to 5 minutes until golden and crisp / Repeat with the rest", meals="Breakfast, Snack", cat="Breakfast")
C("Baked Parmesan Crisps", 5, 8, 4, "150 g|parmesan, finely grated; 1 pinch|black pepper; 1 pinch|flaky salt|opt",
  "Heat the oven to 200 °C and line a tray with baking paper / Make 12 small piles of the cheese, spaced apart, and flatten them / Bake 6 to 8 minutes until golden. Cool on the tray to crisp", meals="Snack, Appetizer", cat="Snack")
C("Air Fryer Cheese Crisps", 5, 6, 2, "100 g|cheddar, grated; 1 pinch|black pepper|opt; 1 pinch|flaky salt|opt",
  "Line the air fryer basket with a round of baking paper / Make 8 small piles of cheese, spaced apart / Air fry at 180 °C for 5 to 6 minutes until golden; cool to crisp", meals="Snack", cat="Snack")
C("Baked Egg Custard (No Sugar)", 10, 40, 4, "4|eggs; 400 ml|heavy cream; 1 pinch|salt",
  "Heat the oven to 150 °C. Whisk the eggs, cream and salt / Pour into 4 ramekins and set them in a roasting tin. Pour hot water halfway up the sides / Bake 35 to 40 minutes until set with a slight wobble. Chill", meals="Dessert, Snack", cat="Dessert")
C("Bacon-Free Pork Belly Breakfast Plate", 5, 30, 2, "300 g|pork belly slices; 4|eggs; 0.75 tsp|salt",
  "Lay the pork belly in a cold pan, salt it and cook over medium heat for 20 to 25 minutes, turning, until crisp / Fry the eggs in the rendered fat / Serve together", meals="Breakfast, Brunch", cat="Breakfast")
C("Steak and Eggs", 5, 12, 2, "1|sirloin steak, about 2.5 cm thick; 4|eggs; 1 tsp|salt; 20 g|butter",
  "Salt the steak. Sear in a smoking hot pan 3 to 4 minutes per side for medium-rare, then rest / Fry the eggs in the butter in the same pan / Slice the steak and serve with the eggs", meals="Breakfast, Brunch, Dinner, Main dish", cat="Breakfast")
C("Chicken Skin Cracklings", 5, 25, 4, "300 g|chicken skin; 1 tsp|salt; 1 pinch|black pepper|opt",
  "Lay the skin flat on a lined tray, pressing it out / Bake at 200 °C for 20 to 25 minutes, with a second tray on top for flat crisps, until golden / Drain on paper and salt while hot", meals="Snack", cat="Snack")
C("Rendered Beef Tallow", 10, 180, 12, "1 kg|beef fat, chopped small; 100 ml|water; 1 pinch|salt|opt",
  "Put the fat and water in a heavy pan or slow cooker on low / Cook gently for 2 to 3 hours, stirring now and then, until the fat is clear and the bits are golden / Strain through a fine sieve into clean jars and keep in the fridge", meals="Sauce/Condiment", cat="Sauce")
C("Chicken Bone Broth (Stovetop)", 10, 360, 8, "2 kg|chicken wings; 3 l|water; 2 tsp|salt",
  "Put the wings (or carcasses saved from roast chickens) in a big pot with the water and bring slowly to a simmer, skimming the foam / Simmer very gently, uncovered, for 5 to 6 hours, topping up with water to keep the bones covered / Strain, season and chill. Lift off the fat to cook with", meals="Soup", cat="Soup")
C("Beef Broth Sipping Cup", 2, 5, 1, "300 ml|bone broth; 1 pinch|salt; 1 tsp|butter",
  "Heat the broth until steaming / Season with salt and stir in the butter. Drink from a mug", meals="Drink, Snack", cat="Drink")
C("Lamb Neck Braise in Bone Broth", 10, 150, 4, "1.2 kg|lamb neck fillets; 2 tsp|salt; 1 tbsp|ghee; 500 ml|bone broth",
  "Salt the lamb and brown it all over in the ghee / Add the broth, cover and cook at 150 °C for 2½ hours until tender / Reduce the juices on the hob and spoon over")
C("Venison Medallions in Butter", 5, 8, 2, "500 g|venison loin, cut into 4 medallions; 1 tsp|salt; 30 g|butter",
  "Salt the venison / Sear in a hot pan 2 to 3 minutes per side, adding the butter for the last minute, for rare to medium-rare; venison is very lean so don't overcook / Rest 5 minutes")
C("Roast Bone Marrow", 5, 20, 4, "4|beef marrow bones, split lengthways; 1 tsp|flaky salt; 1 pinch|black pepper|opt",
  "Heat the oven to 220 °C. Stand the bones cut side up on a tray / Roast 15 to 20 minutes until the marrow is soft and just starting to bubble / Scoop out and sprinkle with flaky salt", meals="Appetizer, Snack", cat="Snack")
C("Prime Rib Roast", 15, 150, 8, "3 kg|beef rib roast, bone-in; 1.5 tbsp|salt; 1 tsp|black pepper",
  "Salt the roast all over the day before and leave uncovered in the fridge / Roast at 110 °C for 2 to 2½ hours until 50 °C in the centre / Rest 30 minutes, then blast at 260 °C for 8 to 10 minutes to brown the crust. Carve and season with pepper")
C("Slow-Roasted Leg of Lamb", 10, 240, 8, "2.5 kg|leg of lamb; 1 tbsp|salt; 250 ml|water",
  "Salt the lamb well. Put it in a roasting tin with the water and cover tightly with foil / Roast at 150 °C for 4 hours until it pulls apart / Uncover for the last 20 minutes at 200 °C to brown")
C("Carnitas-Style Crispy Pork (Lard Only)", 15, 180, 6, "1.5 kg|pork shoulder, cut into 5 cm chunks; 2 tsp|salt; 100 g|lard; 250 ml|water",
  "Put the pork, salt, lard and water in a heavy pot / Simmer gently, partly covered, for 2½ hours until tender and the water has gone / Keep cooking in the fat, turning, for 15 minutes until browned and crisp at the edges")
C("Pork Rind Crumbed Chicken Tenders", 10, 15, 4, "600 g|chicken tenders; 2|eggs; 80 g|pork rinds, crushed fine; 1 tsp|salt",
  "Heat the oven to 200 °C. Beat the eggs with the salt / Dip the chicken in egg, then press into the pork rind crumbs / Bake on a rack 15 minutes, turning once, until golden and 75 °C inside")
