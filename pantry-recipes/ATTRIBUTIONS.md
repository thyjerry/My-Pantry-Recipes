# Attributions and licenses (My Pantry Recipes v2.0)

| What | Where | Source / license |
|---|---|---|
| App code | `src/`, `tools/` | Written for this project, MIT License (`LICENSE`) |
| 2,052 built-in recipes | `recipes/` → `assets/recipes.json` | Written for this project, MIT License |
| 323 ingredient swaps (560 options, ratios, effects, diet/allergen flags) | `assets/substitutions.txt` | Written for this project, MIT License |
| 226 ingredient densities (grams per 240 ml cup) | `assets/densities.tsv` | Written for this project from common spooned-and-levelled kitchen averages, MIT License; approximate |
| Instant Pot cheat sheet (30 foods, 8 tips) | `src/org/pantryrecipes/app/core/Methods.java` | Written for this project, MIT License; typical published ranges, rounded and approximate ("check the manufacturer manual") |
| Nutrient averages | `assets/foods.tsv` | USDA FoodData Central (SR Legacy), public domain (U.S. government work) |
| Default prices | `assets/prices.tsv` | Rough hand-set US estimates, MIT License; not live prices (used × a regional factor where BLS has no price) |
| U.S. regional average prices, Sep 2026 | `assets/bls-prices.tsv` | U.S. Bureau of Labor Statistics, Average Retail Food and Energy Prices; public domain (U.S. government work). Mapping to app ingredients written for this project |
| Product prices (fetched at runtime only, optional) | prices.openfoodfacts.org | Open Prices by Open Food Facts, Open Database License (ODbL) |
| Freezer / use-by times | `core/Inventory.java` | Rounded from USDA FoodKeeper guidance (public, U.S. government); approximate |
| Diets (15), region data (aliases, allergen lists, formats), cuisine kits, Game day themes | `core/Diets.java`, `core/Region.java`, `core/Inventory.java`, `core/Palette.java` | Written for this project, MIT License. Diet labels are a guide, not certification. Themes use neutral names; no team names, logos or trademarks |
| Wi-Fi sync (pairing, encryption, HMAC, merge) | `core/SyncCrypto.java`, `core/SyncWire.java`, `core/SyncDoc.java` | Written for this project on the Java standard library, MIT License; no third-party crypto code |
| Barcode product data (fetched at runtime only) | world.openfoodfacts.org | Open Food Facts, Open Database License (ODbL) |
| Online recipes and photos (fetched at runtime only) | www.themealdb.com | TheMealDB; public test key "1" for development/personal use; a supporter key is needed to redistribute |
| ISRG Root X1 certificate | `assets/isrg-root-x1.pem` | Internet Security Research Group public CA root (used on Android < 7.1 for openfoodfacts.org only) |
| UI icons | `res/drawable` vector icons | Material Design icons by Google, Apache License 2.0 |
| Barcode scanning | external `com.google.zxing.client.android.SCAN` intent (ZXing Barcode Scanner app, Apache License 2.0) | No ZXing code is bundled |

No Google Play Services, AndroidX, analytics, trackers or other third-party libraries are included.

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
