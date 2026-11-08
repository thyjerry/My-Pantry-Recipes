# Attributions and licenses (My Pantry Recipes v2.0, web)

| What | Where | Source / license |
|---|---|---|
| App code | `js/`, `css/`, `tools/`, `tests/` | Written for this project, MIT License (`LICENSE`) |
| 2,052 built-in recipes | `data/recipes.json` | Written for this project, MIT License |
| 323 ingredient swaps (560 options, ratios, effects, diet/allergen flags) | `data/substitutions.txt` | Written for this project, MIT License |
| 226 ingredient densities (grams per 240 ml cup) | `js/data17.js` (exported from the Android `assets/densities.tsv`) | Written for this project from common spooned-and-levelled kitchen averages, MIT License; approximate |
| Instant Pot cheat sheet (30 foods, 8 tips) | `js/data17.js` (exported from the Android `core/Methods.java`) | Written for this project, MIT License; typical published ranges, rounded and approximate ("check the manufacturer manual") |
| Nutrient averages | `js/data16.js` | USDA FoodData Central (SR Legacy), public domain (U.S. government work) |
| Default prices | `js/data16.js` | Rough hand-set US estimates, MIT License; not live prices (× a regional factor where BLS has no price) |
| U.S. regional average prices, Sep 2026 | `js/data20.js` (exported from the Android `assets/bls-prices.tsv`) | U.S. Bureau of Labor Statistics, Average Retail Food and Energy Prices; public domain (U.S. government work) |
| Product prices (fetched at runtime only, optional) | prices.openfoodfacts.org | Open Prices by Open Food Facts, Open Database License (ODbL) |
| Freezer / use-by times | `js/core20.js` | Rounded from USDA FoodKeeper guidance (U.S. government); approximate |
| Diets, region data, kits, Game day themes | `js/core20.js`, `js/data20.js` | Written for this project, MIT License. Diet labels are a guide, not certification. Neutral theme names, no team names, logos or trademarks |
| ZXing | – | Not used by the web app (the Android app calls the external ZXing Barcode Scanner app, Apache License 2.0, by intent; no ZXing code bundled) |
| Barcode product data (fetched at runtime only) | world.openfoodfacts.org | Open Food Facts, Open Database License (ODbL) |
| Online recipes and photos (fetched at runtime only) | www.themealdb.com | TheMealDB; public test key "1" for development/personal use; a supporter key is needed to redistribute |
| Barcode camera scanning | browser BarcodeDetector API where available | No library bundled |
| UI icons | `js/icons.js` | Material Design icons by Google, Apache License 2.0 |

No frameworks, analytics, trackers or other third-party libraries are included.

## Kitchen helpers and data notes (v2.0 Stage 2)
The equivalents, rescue tips, spice blends, cooking times and ingredient explainers (`assets/helpers20.txt`) and the 64 carnivore recipes are original text written for this app. Weights are common kitchen averages, and safe temperatures follow widely published food-safety guidance. Disputed Caribbean dish origins are described in DATA-NOTES.md.

## v2.2 sources
- **Recipe text:** the 1,154 v2.2 dinner mains are original. A generator writes them from our own flavour sets and
  cooking methods (tools/morerecipes/build_v22.py). No text is copied from any site or book.
- **Photos (optional, on tap only):** Openverse (openverse.org) search API. Only CC0, Public Domain Mark and CC BY
  images are offered. The title, creator, licence and source link are shown with each photo and kept with it.
- **Wikibooks Cookbook** (en.wikibooks.org): searched only after the user agrees. Results link to Wikibooks; the text
  is CC BY-SA and is not copied into the app.
- **Project Gutenberg:** links to 7 public-domain cookbooks.
- **Search link-outs** (open in the browser, nothing sent before a tap): Wikibooks, BBC Good Food, Serious Eats,
  Allrecipes, RecipeTin Eats, King Arthur Baking, Chefkoch, Marmiton, GialloZafferano, TheMealDB. These names
  belong to their owners; linking to them does not mean they endorse this app.
