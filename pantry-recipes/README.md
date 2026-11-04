# My Pantry Recipes

(formerly "Pantry Recipes")

A small, free and open-source Android app that finds recipes from the ingredients you already have.
Built to run well on low-end tablets such as the Amazon Fire HD 8 (7th gen, Fire OS 5 / Android 5.1).

* **Package:** `org.pantryrecipes.app` · **minSdk 21** (Android 5.0) · targetSdk 34
* **Version:** 2.0 (versionCode 13), shown as a small "v2.0" label in the header and in *About* · **Size:** legacy 753,410 bytes, modern 762,192 bytes; no Google Play Services, no AndroidX, no third-party libraries, no analytics or trackers
* **Permissions:** INTERNET, ACCESS_NETWORK_STATE, VIBRATE, and (v2.0, for the optional use-by reminder) POST_NOTIFICATIONS (asked only on Android 13+ when you turn reminders on) and RECEIVE_BOOT_COMPLETED (re-arms the reminders). The optional holiday notifications use the same two permissions. No camera, location or contacts permission.
* **Privacy:** no ads, analytics, trackers or accounts. Everything is stored on the device. The only network
  connections go to `www.themealdb.com` (online recipes; can be turned off in *About*) and `world.openfoodfacts.org`
  (only when you look up a barcode).
* **License:** MIT (see `LICENSE`)

## Features

* **Pantry:** add ingredients by typing (with autocomplete, comma-separated for several at once) or by tapping
  a grid of **598 ingredients in 29 collapsible categories** (with subcategories), plus 86 extra aliases in
  autocomplete. A filter box searches the whole grid, there is a **Recently used** row, and custom
  ingredients you type show up as chips. The pantry is saved. Mark items as **"always on hand"**
  staples (salt, pepper, oil and water by default) so they never count as missing.
* **Matching:** each recipe shows **"You have X of Y"**, a match bar and the missing ingredients, highlighted.
  Ingredient names are normalized: plurals, accents, descriptors ("finely chopped red onion" → onion) and
  synonyms (scallion = spring onion = green onion, aubergine = eggplant, coriander = cilantro…). Generic
  families also match: "pasta" in the pantry covers spaghetti, and "cheddar" covers a recipe that asks for "cheese".
* **Sorting & filters:** best match first. Filters: *Cook now* (nothing missing), *Missing 1–2*, vegetarian, vegan,
  gluten-free, dairy-free, and time ≤15/30/60 min. Recipes with no listed time are never hidden by the time filter.
  The app remembers your filters.
* **Discover tab:** search recipes by name, **Random recipe**, **Surprise me from my pantry**, and carousels for
  *New for you* (recipes you haven't opened yet), *Quick* (≤30 min), *Almost there* (missing 1–2) and *Based on
  your favorites*. Browse by cuisine or category (bundled recipes plus TheMealDB's lists). Results are cached for
  offline use, and when you're offline the app falls back to the bundled recipes.
* **Substitutions:** a searchable screen (top bar) with 323 ingredients and 560 swap options, grouped into 8
  categories; each option has a ratio, what it changes (taste/texture) and diet/allergen flags (v1.7). In a recipe, a missing ingredient shows **"You could use X"** when you have a substitute,
  with **Use substitute** (it then counts as a match everywhere) or a **Substitute?** link with ideas.
* **Meal types (v1.3):** every recipe has one or more meal types: Breakfast, Brunch, Lunch, Dinner, Main dish,
  Side dish, Appetizer, Snack, Soup, Salad, Dessert, Baked goods, Drink, Sauce/Condiment. On the Recipes and
  Discover tabs, multi-select **meal chips** (with *All meal types*) combine with the other filters, and two
  check boxes, **Include desserts** and **Include sides**, control whether dessert-only and side-only recipes
  appear. Both are unchecked by default, so main meals come first in results, "What can I cook tonight?" and
  *Surprise me*. Picking the *Dessert* or *Side dish* chip (or any course chip) shows those recipes regardless.
  The selection is shared by both tabs and remembered. When you pick a meal type, the tonight card and Surprise
  me follow it ("What can I cook for breakfast?"). Discover has **Browse by meal type** (with counts; Breakfast,
  Dessert, Appetizer and Side dish also load TheMealDB's matching category). Cards, list rows and the recipe
  screen show meal-type tags. TheMealDB recipes get meal types from their category (default: Main dish + Dinner).
  The Favorites tab, *Random recipe* and Browse by category/meal type are not filtered.
* **"What can I cook tonight?"** card with the top match, plus a **Surprise me** button.
* **Recipe detail:** a **servings scaler** (defaults to 4 servings, remembers your choice) scales every
  quantity. One tap adds all missing ingredients, at the scaled amounts, to the **shopping list**.
* **Shopping list:** saved; items from several recipes are merged and amounts summed. Check items off, clear
  checked items, or move checked items to the pantry. Free-text entry understands "2 cans tomatoes".
* **Favorites** screen.
* **Cook mode:** large text, one step at a time, big Back/Next buttons, and the screen stays on. Any step that
  mentions a time ("simmer 20 minutes") gets a one-tap timer that plays your notification sound when it ends.
* **Two-pane layout** on wide screens or in landscape (≥720dp): the list is on the left and the recipe on the right.
* **Recipes:**
  * 2,036 original recipes are bundled and work fully offline (v1.4 added 645, v1.7 added 830). They span 95 cuisines
    and 10 categories: Main 874, Breakfast 167, Soup 156, Side 152, Snack 148, Baking 145, Dessert 117, Salad 103,
    Drink 92 and Sauce 82. By cooking method (detected from the steps, v1.7): stovetop 1,244, oven 461, no-cook 263,
    one-pot 226, Instant Pot 158, air fryer 56, slow cooker 49. 1,026 take 30 minutes or less. Diet tags (set
    automatically from the ingredients): vegetarian 1,241, vegan 471, gluten-free 1,037, dairy-free 1,055. The v1.4 recipes are generated from hand-written data by
    `tools/morerecipes/build.py` into `recipes/30-*.txt` … `recipes/43-*.txt`. Meal types are set in each recipe file's
    header (6th field, e.g. `Main | Italian | 15 | 30 | 4 | Dinner, Main dish`; assigned by
    `tools/assign_meal_types.py` and hand-reviewed). Counts: Breakfast 262, Brunch 188, Lunch 779, Dinner 1,184,
    Main dish 900, Side dish 232, Appetizer 147, Snack 348, Soup 159, Salad 103, Dessert 205, Baked goods 156,
    Drink 92, Sauce/Condiment 84. Drinks and sauces are not suggested as "tonight's best match" and are not
    picked by "Surprise me" unless you pick that meal type.
  * Online, the app also looks up TheMealDB recipes that use your newest pantry ingredients, plus a search,
    and caches them for offline use.

## New in v2.1 (versionCode 14, `2.1`, web `2.1-web`)
v2.0.1 was skipped. Its fixes are part of this release.

**Fixes from the v2.0.1 feedback**
- **Welcome region:** the line now reads "Suggested from your device language. This app never uses your location."
- **Starter kits removed.** Choosing a region finishes the welcome and adds only the default staples. Cuisine kits are gone
  from Inventory too.
- **Status and navigation bars:** content no longer sits under the notification bar or the gesture bar (window insets, and
  the display cutout on Android 9+).
- **Chips fit:** long food names wrap inside their chip at large text sizes and narrow screens.
- **Fruits:** avocado, tomatoes, cherry tomatoes and cucumbers are listed under Fruits.
- **Discover is independent:** it never uses the Recipes tab's meal-type, diet or method filters. It has its own meal chips.
- **Diet guides for kidney (renal), heart (cardiac) and diabetes (diabetic) diets.** They are off by default and can be set
  in Settings. Recipes get flags (for example potassium, phosphorus, sodium, carbs) and per-serving estimates. Every
  guide screen and recipe card shows: "This is a general guide, not medical advice. Limits are different for every person
  and stage of illness. Follow the plan from your doctor or dietitian." A recipe is never called "safe".

**Botanical fruits and your own categories**
- Ingredients that are botanically fruits get a 🍅 "Botanically a fruit" badge in their details, with a one-line reason.
- An optional "🍅 Botanical fruits" filter chip in Vegetables and Fruits. Typing "botanical" in quick add lists them too.
- **Move an ingredient to the category you prefer** (for example tomatoes back to Vegetables). Use Category in the
  ingredient's details. Settings › Ingredient categories & glossary lists your moves, with undo and Reset all. The moves
  are saved in the backup and sync (`catMoves`). Recipes are not changed.
- **Glossary:** "Botanical fruit" entry, also in Kitchen helpers › Ingredient names, with a fun fact: in 1893 the
  U.S. Supreme Court (Nix v. Hedden) ruled that tomatoes count as vegetables for import taxes, while agreeing that
  botanists call them fruits.

**Buy these next, expandable**
- Tap a suggestion to expand it. You see:
  - why it is suggested;
  - the recipes it unlocks (the first 5 can be tapped; "See all N recipes" opens the rest);
  - an estimated price (your price, or the regional average when built-in prices are on);
  - an "Add to shopping list" button.
- **See all suggestions** lists every missing ingredient of the recipes you are 1–2 items away from. It shows 20 at a time
  with Show more. You can sort by Most recipes or Cheapest, and the sort choice is remembered.
- Speed: the details are built only when you expand a row, and the full list only when you open it. The top-3 pick
  calculation was also rewritten as one pass per pick. The picks are the same, and it is about 16× faster on the
  interpreted JVM.

**460 more recipes (2,620 in all)**
- New recipes in every group:
  - carnivore, 160 in all;
  - breakfast and drinks;
  - slow cooker and Instant Pot;
  - air fryer;
  - 30-minute meals;
  - vegetarian and vegan;
  - baking by grams;
  - more countries' cuisines;
  - soups, salads and desserts.
- Per category (v2.0 → v2.1):

  | Category | v2.0 | v2.1 |
  |---|---|---|
  | Main | 938 | 1195 |
  | Breakfast | 178 | 235 |
  | Side | 159 | 192 |
  | Soup | 168 | 189 |
  | Baking | 147 | 174 |
  | Snack | 163 | 174 |
  | Dessert | 119 | 129 |
  | Drink | 92 | 126 |
  | Salad | 107 | 115 |
  | Sauce | 89 | 91 |

**Fire HD 8 performance first: chunked lazy loading**
- The recipes ship in 5 chunks (500 + 4 × 530). The first chunk is shown at once and the rest are added in the background.
  The web app may refresh the current screen once when the last chunk arrives.
- Android (interpreted JVM, an estimate of HD 8 speed): first recipes appear after about 0.42 s instead of about 3.0 s.
- Web (6× CPU throttle): the first recipes load sooner than in v2.0 (551 ms vs 627 ms), even with 21% more recipes.

## New in v2.0 (final, versionCode 13, `2.0`, web `2.0-web`)
This is the final 2.0 release. It contains everything from Stages 1–3 below, plus:
- **Accessibility pass (all screens, both platforms):**
  - every tap target is at least 48 dp on Android and 44 CSS px on the web. This covers chips, steppers, icon buttons, tabs
    and small buttons;
  - hint and secondary text now meets WCAG AA (4.5:1) on every theme, including on selected tiles and on the page background;
  - screen-reader names were added to icon-only controls, fasting chips and decorative images (decorative images are hidden
    from screen readers).
- **Fasting labels as filter chips (optional, off by default):** under *Settings › Holidays & fasting › Fasting-day labels*,
  "Also show ticked labels as filter chips" adds each ticked label to the main diet filter row. Unticking a label removes
  its chip and clears it from the filter.
- **Holiday reminders as phone notifications (Android, optional, off by default):** in *Choose holidays*, "Phone
  notification N days before" sends one quiet notification per holiday. It uses the same notification permission and the
  same reboot re-arm as the use-by reminder. Tapping it opens that holiday's recipes. The Pantry banner still works as before.
- **Backup:** the two new settings (fasting chips, holiday notifications) are in the JSON backup and sync. API keys and
  secrets are still excluded.
- **Lunar holiday dates** (Eid, Diwali, Lunar New Year, and others) come from published tables for **2026–2030**. A later
  release will extend them past 2030. After 2030 those holidays say "No date in the 2026–2030 table" rather than guess, and their
  recipe sections still work. Fixed-date and computed holidays (for example Easter) are not affected.
- **Holiday sections** still appear automatically.

## New in v2.0 Stage 3 (versionCode 12, `2.0-stage3`, web `2.0-stage3-web`)
This is a staged build, not final 2.0. You'll find everything under *Settings* (the web uses the same names):
- **Holidays & fasting:**
  - *Calendar*: the next 12 months of your ticked holidays. Tap one to see its recipes.
  - *Choose holidays*: all 50, ticked by your region until you change them.
  - *Reminder*: a Pantry banner 7, 14 or 21 days ahead, or off.
  - *Fasting-day labels*: meatless days, Orthodox, vrat, Ekadashi, Jain, Buddhist. Each label has *See recipes*, and ticked
    labels show on recipe pages.
  - Lunar holiday dates come from published tables for 2026–2030 and can differ by a day locally. Fasting labels are a guide;
    customs vary.
- **Baking:**
  - recipes by group;
  - baker's percentages and hydration, a pan size converter, a dough timeline, oven and high altitude, a party calculator;
  - a sourdough starter tracker and a bake log, both on this device only;
  - troubleshooting, and how long bakes keep.
- **Regional sets:** recipes by country, grouped by region, with counts. Each recipe has one country of origin; shared dishes
  use the most common attribution (see DATA-NOTES.md).
- **Performance (web):** recipes load after first paint. On Android, Stage 3 data loads on first use and the Pantry banner is
  built after the first frame.
- **Budget mode:** unknown-cost recipes are kept and shown as *price unknown*.

## New in v2.0 Stage 2 (versionCode 11, `2.0-stage2`, web `2.0-stage2-web`)
This is a staged build, not final 2.0.
- **Carnivore:** 64 original carnivore recipes covering beef, pork, lamb, poultry, fish and seafood, eggs, offal (liver, heart, tongue,
  kidneys), bone broths, rendered fats and egg/butter sauces. Only salt, pepper and animal fats are added; there are no plants.
  The bundle now has 72 carnivore recipes out of 2,116. The rule is "meat, fish, seafood, eggs, organ meats, bone broth, animal fats and dairy, plus salt,
  pepper and water". Flags are worked out at build time and labelled "a guide, not certification". The catalog gains an
  *Offal & animal fats* section and poultry offal and bones.
- **Kitchen helpers** (Settings › Kitchen helpers, offline):
  - an **equivalents table** with 151 rows (volume, weight, cups by country, flour/sugar/fat weights, swaps, oven and pan sizes);
  - **Rescue my dish**, covering 25 problems with fixes and a "next time" tip;
  - a **spice blend builder** with 27 original blends. You can set the batch size, make it salt-free or milder, change each spice's share with −/+,
    see which spices are already in your pantry, add the missing ones to shopping, and copy or share the result;
  - **cooking times**, with 93 entries including safe internal temperatures and Instant Pot times;
  - **ingredient explainers** for 62 ingredients with **local names** by region (US, UK & Ireland, Canada, Australia & NZ, Europe, India).
    Your region's name is marked.
  The data is parsed on first open only, so start-up is unchanged. Measuring-habit helpers shipped in Stage 1.
- **Budget mode (Android):** the first pass over all recipes now runs on a low-priority background thread. The list shows
  recipes while it works and refreshes once when it is done. The web version is single-threaded and still memoises the pass.
- **Caribbean origins:** each dish keeps its most common country attribution. Disputed or shared dishes are listed in
  [DATA-NOTES.md](DATA-NOTES.md).
- **No trademarks:** "Old Bay seasoning" became "Seafood seasoning" and "Nutella" became "Chocolate hazelnut spread" in the
  catalog, recipes and tables. Brand words remain only as hidden input synonyms, so imported recipes still match.
- **Sync pairing** still uses the typed 10-character code. QR pairing was dropped.

## New in v2.0 Stage 1 (versionCode 10, `2.0-stage1`)
Two APKs from one source, both signed with the same key as v1.0–v1.8:

| | Legacy | Modern |
|---|---|---|
| File | `MyPantryRecipes-v2.0-stage1-legacy.apk` (final: `MyPantryRecipes-v2.0-legacy.apk`) | `MyPantryRecipes-v2.0-stage1-modern.apk` (final: `MyPantryRecipes-v2.0-modern.apk`) |
| Package | `org.pantryrecipes.app` (updates v1.x) | `org.pantryrecipes.app.modern` (installs beside it) |
| minSdk / targetSdk | 21 / 34 (Fire HD 8, Android 5.1) | 26 / 35 |
| Extra | – | Per-app language (English variants; the app text is English only), themed/monochrome adaptive icon, 4 app shortcuts, recipe card image (share as PNG). No widgets. |

The notification permission is requested only when you turn on use-by reminders, and only on Android 13 or later. The two-pane
tablet layout from v1.8 stays the same. All new screens are lazy dialogs, built only when you open them, so the HD 8 does
not pay for them at startup.

- **Region** (asked once on first start; change it any time in *About & settings › Region*): United States, Canada, UK & Ireland,
  Europe, Australia & New Zealand, or Other. Each region sets units, date format, week start, 24 h clock,
  decimal comma, gas marks and fan-oven notes, ingredient names (aliases such as aubergine/eggplant and coriander/cilantro),
  and its own allergen list. Only the U.S. gets ZIP prices; every other region uses the prices you type in.
- **Household allergies:** add members, each with an allergen list. Each member can be set to *warn* (shown on the recipe)
  or *hide*.
- **15 diets** (vegetarian, vegan, gluten-free, dairy-free, pescatarian, Mediterranean, low-carb, keto, paleo, Whole30,
  carnivore, low-sodium, high-protein, halal, kosher). These are worked out from ingredient names when the recipes are
  built. They are labelled **"a guide, not certification"**.
- **Prices by ZIP (U.S.):** the first three digits of the ZIP pick the BLS Census region (Northeast, Midwest, South, West).
  Costs are labelled "regional average, Sep 2026". Prices you enter always win. Ingredients BLS does not cover use
  built-in estimates × the region factor, and a coverage line shows how many recipe ingredients have a real price.
  There is an optional Open Prices (Open Food Facts) lookup by barcode. **Budget mode** hides recipes above a set cost
  per serving; recipes with unknown cost are kept.
- **My recipes editor:** ingredient autocomplete, steps, times, servings, tags and a light photo. *Make my version* copies
  a bundled recipe. You can share a recipe as text or as a recipe pack (one JSON file), import packs, and save a
  printable cookbook as HTML. There is no OCR. The recipe card image is modern only.
- **Inventory:** locations (fridge, freezer, pantry, spice rack and others), quantity and use-by date. Freezer items get a
  "use or toss" check (USDA FoodKeeper times). Optional auto-deduct after cooking. A restock list feeds the shopping list,
  and the spice rack has a *buy next* list. A daily reminder fires around
  5 pm the day before something expires (inexact alarm, re-armed after reboot) and offers **Cook this tonight**.
- **Game day themes:** 81 neutral-named sports-colour themes (50 light, 31 dark), 130 presets in 12 groups, all at WCAG AA.
  The grid renders one group at a time. No team names, logos or slogans.
- **Cuisines:** Caribbean dishes are tagged by country (10), 166 recipes were retagged, and the cuisine chips are grouped
  by region. Halal and kosher are diet guides. 16 new recipes bring the total to 2,052.
- **Sync, Android ↔ Android over Wi-Fi.** See below. The web version has backup only.

### Sync (Wi-Fi, Android only)
*About & settings › Sync.*
1. One device taps **Pair a device** and shows a 10-character code. The code is valid for 2 minutes or 5 wrong tries.
2. The other device picks it from the list (found with Android NSD, `_mypantry._tcp`) or types its IP and port (default
   47821), enters the code, then taps **Sync now**.

Everything in the JSON backup is synced except photos: pantry, favourites, ratings, plan, shopping, prices, my recipes,
inventory, household, settings and region. API keys and pairing secrets are never synced or backed up. The merge is
per item, last writer wins. Deletions are kept as tombstones so they also sync. A conflict log shows what was replaced and from
which device.

**Security:** the pairing code derives a shared key, and every message is encrypted and authenticated with HMAC. Sync
only talks to devices on your local network. There is no internet relay, account or cloud. The server runs only while the Sync screen
is open, or while the app is in front with a paired device. There is no background service.

**Honest status:** sync was tested with simulated devices, through real sockets on one machine (core tests). **No test with
two real devices has been done yet.** QR pairing is not in Stage 1, which uses a typed code.

## Planned for 3.0 (not built in 2.0 or 2.1)
User-approved ideas collected during 2.0, listed so nothing is lost. Heavy features (PDF/OCR/batch import, animations) are
web + modern Android only, never the legacy Fire HD 8 build. Everything stays on-device, with no accounts.

web sync; OCR; PIN lock; languages/translations; real European price data; cuisine explorer; drink pairings; skills guides; streaks; kids mode; rota; receipt import; route planner; plan-around-sales; widgets; AI ideas; S Pen; fridge-photo AI; TV/Roku; region-aware recipe packs; QR code for a recipe opening the web version; offline recipe sanity checks (odd amounts); diagnostics screen; dish stories (origin notes); cuisine pantry maps; regional variations side by side; sauce and stock library; user-created tags + saved filters; seasonal favorites pinning; wish list with needed-ingredients list; cooking goals; low-data mode; storage cleaner; theme scheduling (auto dark in the evening); altitude/humidity notes beyond baking; custom alarm sound import (local file, on-device, size limit, excluded from sync/backup if large); short alarm sound pack; ingredient freshness check cues; smart substitutions by what you have on hand; local seasonal cooking by region+month; smart doubling (non-linear scaling warnings); potluck planner; picky eater profiles; meal prep containers guide; budget challenge; date night/special occasion menus with timeline; plate balance guide; fiber/sodium/sugar highlights (opt-in); dietary-change recipe converter with before/after; pantry barcode scan history; offline help/FAQ; printable kitchen reference sheets; seasonal theme packs following holidays; dish of the week spotlight; monthly cooking history heat map. leftover-to-lunch pairing; busy-night calendar (auto quick meals); grocery budget tracker with progress bar; shopping list sharing as text/QR (no account); step-by-step "what now?" view with next-step preview; parallel cooking timeline merging two recipes; temperature probe log; knife and cut guide with diagrams; substitution confidence labels; recipe notes per attempt; source/cookbook name on recipes (plain text); duplicate recipe finder; half-star ratings with private notes; cookbook shelf view grouped by source; auto dark mode by sunset (device clock); large-print recipe view; per-recipe screen-awake toggle; haptic timer cue patterns; cooking dice challenge; flavor wheel; kitchen wins gallery (own photos). PDF recipe import — web and modern Android only (not legacy HD8): text PDFs parsed on-device (title, Ingredients, amounts, Directions/steps) with a review list before saving; scanned PDFs via on-device OCR page by page with a progress bar (user confirmed OCR included for this 3.0 item). All on-device, no uploads. Decided for 3.0: URL recipe import; public-domain classic cookbooks; chef link-outs; original "inspired by" recipes; QR pairing. Batch import from a folder of text/Markdown/Word files (same review list as PDF import); clipboard paste import that splits ingredients/steps; auto-tagging imports (cuisine, diet, meal type, cook time); merge/split recipe tools; collections and folders; cook-along progress bar with time left; ingredient prep grouping; oven/stove usage planner (conflicting temperatures warning); resting/carryover timers; smart timer naming from the step; shopping aisle-order learning; package-size helper; pantry expiry calendar view and "use first" shelf; bulk/freezer-friendly tags; recipe image card export; family recipe book PDF with cover/photos; import/export in common recipe formats; custom font size per screen; quiet hours for timers; pocket-friendly mode (big buttons, messy hands). Large PDF import (e.g. 300 pages) runs in chunks in the background with progress, pause/resume, and flags unparseable pages. Heavy features (OCR / PDF / batch import) are web + modern Android only, never the legacy HD8 build. Cooking class paths (short lessons); "why it works" notes; common-mistakes cards per technique; taste-adjusting guide; theme nights with a rotating pick; plan by effort level; weekly prep checklist; thaw reminders; calorie-neutral swaps; appliance manual notes; oven hot-spot notes and a personal temperature offset; pantry shelf-life guide for opened items; printable container/freezer labels; cookware inventory with pan-size hints; recipe swap by file or QR (no accounts); cook-together checklist for two; memory notes on recipes; anniversary/birthday reminders suggesting a favourite dish; screen-reader testing on real devices (TalkBack, VoiceOver) beyond the automated/code-level 2.0 pass; lunar holiday date tables past 2030; hold-to-confirm hands-off mode with big tap targets; extra colour/contrast adjustments. Home-screen quick-add widget (2.0 ships no widgets); "cook it again" last-five shortcut; smart rounding; fraction/decimal display toggle; recipe shortcuts on the Android home screen (pinned per-recipe; 2.0 modern only has the fixed app shortcuts); produce ripeness guide; meat doneness and cuts guide; cheese/pasta/rice guides; herb and spice substitution wheel; food storage cheat sheet; travel and camping mode; power-outage pantry meals; sick-day meals (with disclaimer); baby/toddler first foods (with disclaimer); big-batch holiday timeline countdown; auto-cleanup suggestions; backup to a user-chosen location (USB/SD); import from other recipe apps' exports; per-recipe change log with undo; cooking milestones (low-key); recipe roulette by mood and time; "on this day" history. All optional, behind one master off switch, all on-device, no accounts: chef levels with your own title; cuisine passport (a stamp per country cooked, uses the v2.0 country cuisine tags); technique tree unlocking cards; skill badges; seasonal/holiday badges; collectible dish cards with photo; weekly mini-quests; 30-day challenges (private); beat-your-own-time; pantry bingo; family challenge board; share badges as an image card; friend challenge by QR (no accounts); cook-off scorecards; streak freeze (one free skip per week); streak-free mode (badges without streaks); celebration animations (off with reduced motion, never on the legacy HD8 build); yearly recap poster image. Recipe difficulty rating; active vs hands-off time split; equipment-light filter (one pot/pan); ingredient overlap finder; flavour pairing suggestions; breakfast/brunch section with make-ahead; snack and dessert planner; soup/salad/sandwich builders; sauce of the week; drinks section (coffee, tea, smoothies, mocktails); cooking for one or two (small-batch scaling, freezer tips); student/dorm mode (microwave-only, minimal gear); dinner party hosting guide; small-kitchen mode (burner count, counter space); printable allergy-safe host card; multiple named/coloured timers on one screen; kitchen scale helper (step-by-step adds with tare reminders); measuring cup visual guide; in-recipe mini-calculator; first-use feature tour tips; custom home screen layout (reorder sections); quick settings sheet.

Decided for 3.0 (user decision, Nov 3 2026): URL recipe import, public-domain classic cookbooks, chef link-outs,
original "inspired by" recipes, and QR pairing between devices. None of these are in 2.1.

**Not planned:** feedback button; gentle diet-change helper; medication/food note.

### 2.0 stages
- **Stage 2 (done):** equivalents table, *Rescue my dish*, spice blend builder, cooking-times lookup, ingredient
  explainers with local names. Measuring-habit helpers shipped in Stage 1.
- **Stage 3 (done):** regional sets by country, holidays (calendar, reminder, sections, checklist), fasting-day
  labels, baking section; lazy web recipe load.
- **Stage 4 (done, final 2.0):** accessibility pass on all screens, optional fasting filter chips, optional holiday
  notifications (Android), final regression and packaging.

## New in v1.5

* **Use-by dates:** long-press/tap a pantry chip → *Set use-by date…*. Items due within 3 days appear in a
  **Use soon** section at the top of the Pantry tab (expired / today / in N days), recipes that use them get a
  "use soon" badge and are **ranked first**, and Discover gets a *Use it up* carousel.
* **Pantry quantities (optional):** *Set quantity…* ("500 g", "2 cups", "6"). Recipe ingredients then show
  **"You have enough"** or "You have X, need Y". Different kinds of units (grams vs cups) are not compared.
* **Weekly meal planner** (calendar button): Monday–Sunday, add recipes to days with servings, and turn the
  week into one **combined shopping list** (amounts merged and scaled). Plans older than 28 days are pruned.
* **Ratings & notes:** 1–5 stars and a private note on every recipe; stars show in lists.
* **Dislikes & allergies** (*About → Allergies / avoid*): 9 common allergens plus your own disliked
  ingredients hide matching recipes, and recipes show **"May contain"** allergen flags. **This is name-based
  only (it reads ingredient names) and is not a guarantee: always check labels.** The app says so on screen.
* **Backup:** *About → Export backup / Import backup* writes one JSON file (pantry with dates and quantities,
  favorites, shopping list, plan, ratings and notes, dislikes/allergies, settings, themes, saved online recipes).
  The same file format imports in the web version, and web backups import here. Import replaces current data.
* **Tonight's pick** lists *Dinner* and *Main dish* first among a recipe's meal types.
* **Appearance** (*About → Appearance…*): *Follow system*, *Light* (default, the light-green look) and *Dark*,
  plus 15 more named presets (Ocean, Midnight, Sunset, Lavender, Rose, Espresso, Slate, Forest night, Citrus,
  Mint, Plum, High contrast, High contrast dark, Terracotta, Arctic). Every preset is checked at build time to
  keep body and secondary text, button labels and links at WCAG AA (4.5:1). A **custom theme editor** (HSV and
  RGB sliders plus hex entry, live preview and contrast readout; no external libraries) saves up to 12 named
  custom themes. *Reset to default* goes back to Light and keeps your custom themes. The choice is saved, is
  restored on restart and is part of the backup. All screens, cards, chips, the bottom navigation, dialogs and
  cook mode use the theme colours; on dark themes the status and navigation bars and icons switch too
  (light status-bar icons need Android 6+; on Android 5 the status bar is just tinted darker).

## New in v1.6

* **Appearance fix:** picking a theme, mode, text size, corners or accent no longer jumps the Appearance screen to the top
  (Android: the scroll position is saved before the screen restyles and restored after layout; web: the list is re-rendered in place).
* **Themes:** 49 presets in 11 groups (Light, Dark, Colorful, Pastel, High contrast, Warm, Cool, Earthy, AMOLED black,
  Paper & sepia, Editor classics) plus *Follow system*, in a scrollable swatch grid with the current one marked. **Text size**
  (small / normal / large / extra large), **corners** (sharp / normal / round), an **accent quick pick** (8 colours) and a
  **Random theme** button. Every preset, and every preset with every quick accent, is tested for WCAG AA (4.5:1).
* **Add by barcode** (Pantry tab): looks the code up on Open Food Facts (API v2) and suggests the closest pantry
  ingredient; you confirm or edit the name (and optionally keep the package size as the amount). Codes you name are
  remembered, also for products Open Food Facts doesn't know. Manual entry of the digits always works.
* **Nutrition estimate** per serving (kcal, protein, carbs, fat) from USDA FoodData Central averages, shown only when
  at least 70% of the ingredients match, always labelled "Estimate" with the match %.
* **Cost per portion:** set prices per package (amount + unit, currency symbol, default `$`); optional rough built-in
  default prices (labelled as estimates). Recipes show cost per serving and in total with the priced %; the shopping
  list can show per-item prices and an estimated total. Prices are part of the backup.
* **Cooked this:** logs the date and servings ("Cooked 3× · last …"), keeps **leftovers** with portions (+/−) and a
  use-by date (default 3 days, 1–14 in Settings), and can optionally take the used amounts out of pantry quantities
  (off by default). **Batch** ×1/×2/×3 chips scale the recipe and *Add batch to plan* puts it in the planner.
* **Your photos** on recipes (stored privately on the device, scaled to ~800 px JPEG). Not in the backup unless you tick
  *Include photos*.
* **Cook-mode timers:** every time in a step ("10–12 minutes" uses 10 and shows the range) gets a chip; several timers
  can run at once, with pause/resume and cancel, and stay visible on every step. They are based on end times, so they
  survive rotation.
* **Buy these next:** on Discover, the 1–3 ingredients that unlock the most recipes you are 1–2 items away from
  (respecting diets, allergies, dislikes and substitutes); tap one to see the recipes.
* **Shopping list by aisle** in collapsible groups (remembered), plus checks, *Clear checked*, *Checked → pantry*.
* **Share** the shopping list or the week's meal plan as plain text.
* **Backup format 2** adds prices, leftovers, cooked log, barcodes, the new appearance settings and (optionally)
  photos. v1.5 (format 1) backups still import; v1.6 rejects files from a newer format with a clear message.

**Barcode scanning on Android is scaled down:** the app has **no built-in camera decoder**. *Scan with a barcode app*
sends the standard `com.google.zxing.client.android.SCAN` intent to a separately installed scanner (for example the
open-source Binary Eye or ZXing Barcode Scanner); if none is installed the app says so and you type the number. No
ZXing code is bundled and no CAMERA permission is used.
**TLS on Android 5–7.0:** these versions don't trust the ISRG Root X1 (Let's Encrypt) certificate used by
world.openfoodfacts.org. For that host only, and only on API < 25, the app adds the bundled `assets/isrg-root-x1.pem`
(its SHA-256 is checked against a pinned value) to the system roots. Chains are still fully verified and hostnames
are still checked; there is no trust-all code.

## New in v1.8

* **Rebrand:** the app is now called **My Pantry Recipes** everywhere (launcher label, header, About, web title and
  PWA manifest `name`; the manifest `short_name` is "Pantry"). New logo-derived icons. The Android package stays
  `org.pantryrecipes.app` and the signing key is unchanged, so v1.8 installs over v1.7 and keeps your data. The backup
  file still says `"app": "Pantry Recipes"` so older versions can still import it.
* **New layout (flat and light):** a header band with the logo and name centred; pill-shaped segmented tabs
  **Pantry / Recipes / Discover / Plan / Shopping** at the top (the meal planner has its own **Plan** tab; Favorites is a
  "♥ Favorites" chip on Recipes); chip rows with the selected chip filled; rounded recipe cards with a flat letter
  tile, a match bar and one filled dark **Cook** button that opens cook mode; left-aligned section titles. Flat colours
  only: no shadows, blur, gradients, animation or bitmaps in list rows. All text pairs are WCAG AA in every theme
  (tested at build time and in the browser test).
* **Decorations** (*Appearance → Decorations*, on by default): static theme-coloured accents only (tinted header band,
  short underline bars under section titles, coloured empty-state icons, match % on rows). No images and no animation;
  the web version uses CSS only and respects `prefers-reduced-motion`. Turning it off removes them.
* **Speed (Fire HD 8):** the Pantry tab is updated in place when you tap an ingredient instead of being rebuilt;
  matching is incremental (only the recipes that use the toggled ingredient are re-matched, via `MatchCache`);
  ingredient names and the search index are built on the worker thread; the autocomplete list is attached on first
  focus; list pages are 30 rows (was 60); title hints are precomputed at build time. See "Performance" below.
* **Units fixes:** the per-recipe Units switch is remembered across restarts; Share, the meal planner and the
  shopping list use your units; ounces are finer for small amounts (⅛ oz under 1 oz, grams in brackets under ⅛ oz);
  gas ½ = 120 °C = 250 °F; negative temperatures and ranges in steps ("-18 °C", "170-180 °C", "-20 to -18 °C") now
  convert correctly (v1.7 dropped the minus sign and collapsed ranges).
* **Recipes:** near-duplicate titles get a short hint in lists and on the recipe screen (e.g. "with broccoli"); online
  recipes whose title matches a bundled one are hidden. Allergen flags no longer over-flag (coconut milk, nutmeg,
  eggplant, butternut/spaghetti squash, cream of tartar, water chestnuts, ginger ale, pizza sauce…); product-dependent
  gluten items (oats, miso, soba, Worcestershire) show "Gluten (check label)" and still count when you avoid gluten.
* **Barcode:** the package size from Open Food Facts is cleaned up for the pantry (e.g. "500 g ℮" → "500 g") and
  there is a confirm step with the closest matches before anything is added.
* **Themes renamed:** Solar light/dark → **Citrine light/dark**, Fjord day → **Frostbite** (old saved ids and backups
  still resolve).
* **Timers in the background:** each running cook-mode timer also sets an `AlarmManager` alarm; when it fires, a
  notification with sound and vibration appears (tap opens cook mode). No new permissions. Exact on Android 5–11;
  on Android 12+ it may be a few minutes late (no exact-alarm permission requested) and on 13+ it only shows if
  notifications are allowed for the app. Fire OS power saving can still delay alarms, so keep the screen on for
  critical timings.
* **Crash safety net:** an uncaught crash is written to the app's private storage (nothing is sent). After a crash,
  *About* shows **"The app crashed last time – view/copy error"** to view, copy or delete the report.
* **Activity review:** static review of all activities (unknown/stale recipe ids from notifications, queued pantry
  writes flushed on pause, receiver not exported).

### Performance (measured, not on a device)

JVM `tools/PerfBench` on one CPU core (warm medians; `-Xint` approximates a slow interpreter, pessimistic for ART):

| Step | v1.7 JIT | v1.8 JIT | v1.7 -Xint | v1.8 -Xint |
|---|---|---|---|---|
| Pantry open: autocomplete names (UI thread) | 12.1 ms | 0.0 ms (worker) | 125.6 ms | 0.0 ms |
| Pantry open: chip keys + counts | 1.5 ms | 0.1 ms | 105.5 ms | 0.6 ms |
| Ingredient filter, 6 keystrokes | 29.3 ms | 0.7 ms | 843.5 ms | 7.8 ms |
| Toggle an ingredient: model + counts | 3.8 ms | 0.1 ms | 96.2 ms | 0.4 ms |
| Re-match all recipes + sort | 17.5 ms | 11.9 ms | 175.0 ms | 94.5 ms |
| Recipe search, 5 keystrokes | 44.5 ms | 15.2 ms | 1012.8 ms | 127.3 ms |
| Startup parse (worker thread) | 169.8 ms | 90.7 ms | 4205.5 ms | 1951.7 ms |

These are core-logic timings; view inflation/layout on the HD8 is not measured (no device).

## New in v1.7

* **830 more offline recipes** (2,036 in total), including **158 Instant Pot / electric pressure cooker recipes**
  (mostly in `recipes/50-*` … `53-*`). Every IP recipe says to come up to pressure and how to release; one
  (homemade yogurt) uses the Yogurt function and has no pressure step.
* **Measurement units** (*About → Measurement units & conversions*): *Recipe's own*, *US* (cups, oz, °F), *Metric*
  (g, ml, °C) or **Weights for dry baking ingredients** (the default). Weights shows flour, sugar, cocoa, oats and
  other dry baking ingredients in grams (or ounces: *Grams / Ounces*), with the cup amount in brackets
  ("125 g (1 cup)"; *Show cups in brackets* can turn that off). Teaspoon amounts stay in spoons. The setting applies
  to the recipe screen, cook mode and the shopping list (including lists made from the meal planner), and to oven
  temperatures in steps ("Bake at 350°F" reads "Bake at 350°F (180°C)" in Metric and Weights; "180°C" gets "(350°F)" in US). All 329 flour lines in the bundled recipes show
  grams by default (build-time test). Each recipe also has a **Units** switch (Recipe's own / US / Metric / Weights)
  that keeps the servings scaling; it lasts for the app session and does not change your default.
* **Conversions screen:** volume ⇄ ml/cups/tbsp/tsp/fl oz, weight g ⇄ oz/lb, an **ingredient converter** (cups ⇄
  grams ⇄ ounces for 226 ingredients, spooned-and-levelled averages), a **butter helper** (sticks, tbsp, cups, g, oz),
  oven temperature °F ⇄ °C ⇄ gas mark with fan values, an oven table and the rounding rules. Amounts accept
  fractions ("1/2"), mixed numbers ("1 1/2"), unicode fractions ("1½") and ranges ("2-3", "2 to 3"). Cups are
  US cups of 240 ml (tbsp 15 ml, tsp 5 ml, fl oz 30 ml). Conversions are estimates; weigh when it matters.
* **Rounding:** grams under 10 g to 1 g, flour from 20 g and anything from 100 g to 5 g; teaspoons in ¼ steps (⅛ tsp,
  then "pinch"), tablespoons in ½ steps, cups in ¼ and ⅓ steps, ounces in ¼ oz under 4 oz and ½ oz under 16 oz; oven
  temperatures to the nearest 10 °C / dial-friendly °F.
* **Cooking methods:** recipes are tagged Instant Pot, slow cooker, air fryer, one-pot, oven, stovetop or no-cook
  from their steps. Filter chips on the Recipes tab (multi-select, remembered, part of the backup), a **Browse by
  cooking method** card on Discover, and a "Cooking method" line on each recipe.
* **Instant Pot support:** cook-mode timers know pressure times ("Pressure cook on High for 8 minutes", "8 minutes
  at high pressure", "cook at high pressure for 8 min") and natural release ("release naturally for 10 minutes").
  The chip says **"Start 8 min at pressure"** with a note that the pot first needs about **10–15 minutes to come up to
  pressure, which is not counted** (start when the float valve pops up). Build-up times in the text ("about 10 to
  15 minutes to come up to pressure") never get a timer. A **Instant Pot cheat sheet** (30 foods with liquid,
  minutes at High and release, plus 8 safety tips) says clearly that times are approximate and to **check the
  manufacturer manual**.
* **Swap details:** the missing-ingredient hint, *Almost there* and the substitute dialog show the ratio, what the
  swap changes and its diet/allergen flags (vegan, dairy-free, gluten-free, nut-free, egg-free).
* **Backup:** v1.7 adds `units`, `weightUnit`, `showCups` and `methods` to the settings (still format 2). v1.6 and
  v1.5 backups import unchanged; missing keys fall back to the defaults (Weights / grams / cups shown / no method
  filter).

## Data sources & attribution

* **Open Food Facts** product data (barcodes): openfoodfacts.org, available under the **Open Database License (ODbL)**.
* **USDA FoodData Central** (SR Legacy) nutrient averages in `assets/foods.tsv`: public domain (U.S. government work).
* **Recipes, swaps, densities and the Instant Pot cheat sheet** (`recipes/`, `assets/substitutions.txt`,
  `assets/densities.tsv`, `core/Methods.java`): written for this app, MIT License. Densities are common
  spooned-and-levelled kitchen averages; cheat-sheet times are typical published ranges, rounded and approximate.
* **Default prices** in `assets/prices.tsv`: rough, hand-set US ballpark estimates, not live prices.
* **ZXing:** used only as an external intent (`com.google.zxing.client.android.SCAN`); no ZXing code is included.
* **ISRG Root X1** certificate (public CA root, Internet Security Research Group).
* **v2.0 – BLS Average Retail Food Prices** (U.S. and the four Census regions, September 2026) in `assets/bls-prices.tsv`:
  U.S. Bureau of Labor Statistics, public domain (U.S. government work). The mapping to app ingredients is ours.
* **v2.0 – Open Prices** (prices.openfoodfacts.org, optional lookup at runtime only): Open Food Facts, ODbL.
* **v2.0 – freezer and use-by times** (`core/Inventory.java`): rounded from public USDA FoodKeeper guidance; approximate.
* **v2.0 – diets, region data, kits and themes:** written for this app, MIT License. The diet labels are a guide, not certification.
  The Game day themes are neutral colour schemes, with no team names, logos or trademarks.

## TheMealDB attribution & terms

Online recipes and food photos come from **[TheMealDB](https://www.themealdb.com)**, a free crowd-sourced
recipe database. The app uses TheMealDB's public **test API key `1`**, which TheMealDB provides for
development and educational/personal use. **If you publish this app (for example in an app store), you
need a TheMealDB supporter API key.** Enter it under *About → API key*. Online recipes link back to their
TheMealDB page.

Because TheMealDB has no diet tags or serving counts:
* Diet tags on online recipes are **guessed from the ingredient list** and marked with "?" in the app. They
  can be wrong, so always check labels for allergies.
* Online recipes are assumed to serve 4. Their prep/cook time is shown as "time not listed".

## Building (no Gradle)

You need the Android SDK (build-tools 34.0.0, platforms android-34 for legacy and android-36 for modern), a JDK 17 and Python 3 (with Pillow, only
needed to regenerate the icons).

```sh
./build.sh          # -> dist/MyPantryRecipes-v<versionName>-legacy.apk and -modern.apk (version from version.properties)
```

`build.sh` reads `versionName`/`versionCode` from `version.properties` (the one version source) and does the following:
1. Compiles `recipes/*.txt` (a simple recipe text format) into `assets/recipes.json` with `tools/RecipeTool.java`.
   It then runs the JDK test harness: ~722,000 checks covering the bundled recipes (≥1,200 total, ≥50 per category and per meal type, ≥50 cuisines, technique counts, no duplicate ids/titles or ingredients, required fields, sane times and servings, ingredient reachability from the grid, diet tags equal to the classifier and vegan ⇒ vegetarian), the ingredient catalog, normalizer, matcher, substitutions, Discover sections, ranking, quantity
   parsing/scaling, diet classifier, JSON round trip and (v1.5) dates, use-by ranking, allergens, amounts, meal plan, palettes/WCAG contrast and backup round trips, and (v1.6) all 49 presets × 8 quick accents at WCAG AA, fonts/corners/random, the Appearance scroll-restore code path, durations, nutrition, cost, leftovers, buy-next, aisles, share text, barcode parsing/check digits, the TLS/permission code path and format-2 backups incl. v1.5 fixture imports, and (v1.7) ≥2,000 recipes, ≥150 Instant Pot recipes, ≥300 swaps with ratio/effect/flags and their wording claims, ≥150 densities, every ingredient line in all 4 unit settings (307,566 conversions), all flour lines in grams, fraction/range parsing, temperatures and gas marks, pressure/release timer detection and the v1.7 backup settings incl. a real v1.6 backup. The build fails if any check fails.
2. Runs `aapt2` compile/link, then `javac --release 8`, then `d8 --min-api 21`.
3. Runs `zipalign`, then `apksigner` (v1 + v2 signatures). If there is no `debug.keystore`, it creates a
   self-signed one first.

Use `SDK=...`/`ANDROID_HOME` and `JAVA_HOME` to point to your toolchain. The app icon is generated by
`tools/make_icons.py`.

Install on a Fire tablet: enable *Settings → Security → Apps from Unknown Sources*, then open the APK
(or run `adb install dist/MyPantryRecipes-v2.0-legacy.apk`). Use the legacy APK on a Fire HD 8. The modern APK is for Android 8+ phones and tablets and installs beside it as a separate app.

## Project layout

```
src/org/pantryrecipes/app/core/   pure Java: normalizer, matcher, quantities, diet tags, JSON (shared with tests)
src/org/pantryrecipes/app/        Android UI (MainActivity, CookActivity, SubsActivity, AppearanceActivity, Extras, V16, V17, Scan), SQLite store, TheMealDB client
recipes/                          bundled recipe sources
assets/substitutions.txt          substitution table (original text format)
assets/densities.tsv              ingredient densities (grams per 240 ml cup)
tools/                            RecipeTool (recipe compiler + tests), icon generator
res/, AndroidManifest.xml         resources (vector icons, PNG launcher icons)
```

## Status

**v2.1 is verified at build level only. It has not been run on a phone, tablet or emulator.**
- The Java build-time checks pass: 925,092, with 0 failures.
- Web: the Node suites, `validate.js` (10,635 checks) and the headless-Chrome browser test all pass. The accessibility audit
  covers the new screens (fruit menu, category chooser, glossary, categories screen, Buy these next expanded and See all,
  and the diet guides) and finds 0 problems in both themes.
- `apicheck.py` finds the same calls above API 21 as v2.0.1: the v2.0 set plus `DisplayCutout`, all guarded.
- Both APKs report versionName 2.1 and versionCode 14 and are signed with the same key.
- Fire HD 8 timings are estimates from `-Xint` and from Chrome at 6× CPU throttle.

### v2.0 status (kept for reference)

**v2.0 is verified at build level only.**
- Both APKs compile, and the 803,751 build-time checks pass with 0 failures. These include the Stage 4 checks: AA contrast for
  hint text on every preset theme, fasting-chip filtering, and backup round trips for the new settings.
- Web/Android parity is checked with Java-generated vectors.
- `apicheck.py` on the 217 legacy classes finds the same 5 calls above API 21. All of them are inside `Build.VERSION.SDK_INT`
  guards, including the holiday notification channel.
- `aapt2 dump badging` shows versionName 2.0 and versionCode 13 for both APKs:
  - legacy: `org.pantryrecipes.app`, minSdk 21, targetSdk 34;
  - modern: `org.pantryrecipes.app.modern`, minSdk 26, targetSdk 35.
- Permissions are unchanged from Stage 3. `apksigner verify` passes for both APKs with the v1.x key (certificate SHA-256
  `b346a25d…6e83`).

On Android the accessibility pass was done in code: 48 dp minimum sizes, content descriptions, and palette contrast checked
by the build-time tests. It has **not** been checked with TalkBack or the Accessibility Scanner. On the web, an automated
audit of 46 views in the light and dark themes checked names, labels, alt text, 44 px targets and AA contrast, and found 0
problems.

**Not run on a real device or emulator.** The Android UI (including the new chips, the holiday notification and the bigger
touch targets) has not been used on a phone or tablet. Fire HD 8 timings are estimates from interpreted-JVM (`-Xint`) runs on
the build machine.

### v2.0 Stage 3 status (kept for reference)

**v2.0 Stage 3 is verified at build level only.** Both APKs compile and the 800,101 build-time checks pass, with 0 failures.
These include the Stage 3 checks: holiday dates (Easter, Orthodox Easter, nth-weekday and table dates), region defaults, that
holiday sections respect their diet and exclusion rules, no fasting-label leaks (for example no egg in a Jain or Orthodox
label), and the baking calculators. Web/Android parity is checked with Java-generated vectors (test-core30.js, 1,597 checks).
`apicheck.py` on the legacy classes (217) finds the same 5 calls above API 21, all inside `Build.VERSION.SDK_INT` guards.
`aapt2 dump badging`:
- legacy: `org.pantryrecipes.app`, versionCode 12, 2.0-stage3, minSdk 21, targetSdk 34;
- modern: `org.pantryrecipes.app.modern`, minSdk 26, targetSdk 35.

Permissions are unchanged. `apksigner verify` passes for both with the v1.x key (certificate SHA-256 `b346a25d…6e83`).

**Not run on a real device or emulator.** The Stage 3 dialogs, the Pantry holiday banner and the baking tools have not been
used in an Android UI. HD 8 timings are estimates from interpreted-JVM (`-Xint`) runs on the bench box.

### v1.8 status (kept for reference)

The v1.8 build is verified at build level only: it compiles, the 729,475 build-time checks pass (including the new v1.8
checks: incremental matching vs the plain matcher, pantry index, units/temperatures, barcode sizes, allergen false
positives, title hints, migrations from v1.5/v1.6/v1.7 backups and theme ids), `apicheck.py` finds no API >21 calls (150
classes), `aapt2 dump badging` reports versionCode 9 / versionName 1.8 / label "My Pantry Recipes" / minSdk 21 / targetSdk 34
with INTERNET, ACCESS_NETWORK_STATE and VIBRATE (same as v1.7), and `apksigner verify` passes with the same signing key as
v1.5–v1.7 (certificate SHA-256 `b346a25d…6e83`). **The app has not been run on a real device or emulator.** The new layout,
the in-place Pantry updates, decorations, background timer notifications, the barcode confirm step and the crash report
screen are untested in practice; performance numbers above come from a desktop JVM, not the Fire HD 8.
