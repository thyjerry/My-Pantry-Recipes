# My Pantry Recipes (web)

Find recipes from what you already have. This is the HTML5 / PWA version of the My Pantry Recipes Android app (formerly "Pantry Recipes"). **Version 2.0-web** (shown as "v2.0" in the top bar and in *Settings & About*): 2,160 built-in recipes in 95 cuisines with meal types and cooking methods (158 Instant Pot recipes), a 782-ingredient pantry grid, 323 substitutions (560 options) with ratios and diet flags, measurement units and conversions (226 ingredient densities), Discover, favorites, shopping list and cook mode. It works on iPhone/iPad (Safari), Android, Fire tablets and desktop browsers.

Plain HTML, CSS and vanilla JavaScript. No frameworks, no build step, no ads, no analytics, no trackers, no accounts. MIT License.
The only network connections go to TheMealDB (www.themealdb.com) for optional online recipes and photos, and to world.openfoodfacts.org when you look up a barcode.

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
- **Holiday reminders as phone notifications (Android app only, optional, off by default; the web keeps the Pantry banner):** in *Choose holidays*, "Phone
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
- **Budget mode:** on Android the first pass now runs on a background thread. The web version is single-threaded and memoises the pass as before.
- **Caribbean origins:** each dish keeps its most common country attribution. Disputed or shared dishes are listed in
  [DATA-NOTES.md](DATA-NOTES.md).
- **No trademarks:** "Old Bay seasoning" became "Seafood seasoning" and "Nutella" became "Chocolate hazelnut spread" in the
  catalog, recipes and tables. Brand words remain only as hidden input synonyms, so imported recipes still match.

## New in v2.0 Stage 1 (`2.0-stage1-web`)
The web version gets the same Stage 1 data features as Android, except sync. The Android sync is Wi-Fi only; on the web,
use JSON backup and restore to move data.
- **Region** welcome: the browser language suggests a region, but you are always asked. You can change it in *Settings*.
  Region sets formats, ingredient aliases and an allergen list. **Household** allergies can warn or hide.
- **15 diets**, labelled "a guide, not certification". **U.S. ZIP prices** use BLS regional averages for Sep 2026, labelled
  "regional average". Your own prices win, other ingredients use estimates × the region factor, and a coverage line is
  shown. Optional Open Prices lookup. **Budget mode.**
- **My recipes editor**, recipe packs, share as text and a printable cookbook. The recipe card image is in the modern Android app only.
- **Inventory** with locations, use-by dates, freezer use-or-toss, restock list and spice rack (kits removed in v2.1).
  The use-by tomorrow banner appears in the app. There are no closed-app notifications on the web.
- **Themes:** 130 presets in 12 groups, including 81 Game day themes with neutral names, shown one group at a time.
- **Cuisines:** Caribbean dishes by country, region-grouped cuisine chips, 2,052 recipes.
- Everything is in the JSON backup, except API keys. Nothing is uploaded, and there are no accounts, ads or tracking.
- Measured with Chrome at 6× CPU throttle (single HTML file): boot 609 ms, Recipes tab task 185 ms, Discover 132 ms,
  toggle 27 ms. Not measured on a real Fire HD 8.

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

## New in v1.8

Same features as Android v1.8 (version **1.8-web**; service worker cache `pr-core-1.8-web`):
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
* The crash safety net (*"The app crashed last time…"*) is Android only. Background timer notifications are Android
  only; web timers still need the page to stay open.

### Performance (headless Chrome, CPU throttled 6×, decorations ON, median of 3 runs)

| Measure | v1.7 | v1.8 |
|---|---|---|
| Boot to interactive (2.6 MB single file) | ~561 ms | ~564 ms (unchanged; parse-bound) |
| Recipes tab: first paint | 111 ms | 75 ms |
| Recipes tab: whole task | 246 ms | 230 ms |
| Recipes tab: DOM nodes | 854 | 505 |
| Pantry tab open | 42 ms | 42 ms |
| Toggle a pantry ingredient | 45 ms | 31 ms |
| Recipe search, total | 131 ms | 92 ms |
| JS heap | 19 MB | 14 MB |

At 4×: toggle 37 → 21 ms, Recipes paint 62 → 43 ms, search 104 → 61 ms. A 6× throttle of a fast desktop CPU is only a
rough stand-in for a Fire HD 8 (7th gen); real-device numbers may be worse.

## New in v1.7

Same features and backup format (format 2, plus four settings) as Android v1.7:
* **830 more offline recipes** (2,036 in total) including **158 Instant Pot / electric pressure cooker recipes**.
  Meal types: Breakfast 262, Brunch 188, Lunch 779, Dinner 1,184, Main dish 900, Side dish 232, Appetizer 147,
  Snack 348, Soup 159, Salad 103, Dessert 205, Baked goods 156, Drink 92, Sauce/Condiment 84.
* **Measurement units** (*Settings → Measurement units & conversions*): *Recipe's own*, *US*, *Metric* or
  **Weights for dry baking ingredients** (the default): flour, sugar, cocoa, oats and other dry baking ingredients in
  grams (or ounces) with the cups in brackets, e.g. "125 g (1 cup)" (*Show cups in brackets* turns that off).
  Applies to the recipe screen, cook mode and the shopping list (also lists made from the meal planner), and adds the
  other scale to oven temperatures in steps ("Bake at 350°F (180°C)"). Each recipe has a **Units** switch that keeps the
  servings scaling (for this session; your default doesn't change).
* **Conversions screen:** volume/weight converter, an **ingredient converter** (cups ⇄ grams ⇄ ounces, 226
  ingredients), a **butter helper**, oven temperature °F ⇄ °C ⇄ gas mark, an oven table and the rounding rules.
  Accepts fractions, mixed numbers, unicode fractions and ranges ("2-3", "2 to 3"). US cup = 240 ml.
* **Cooking methods** (Instant Pot, slow cooker, air fryer, one-pot, oven, stovetop, no-cook): filter chips on the
  Recipes tab, a *Browse by cooking method* card on Discover and a "Cooking method" line on recipes.
* **Instant Pot:** pressure-time chips in cook mode ("Start 8 min at pressure") with the note that the pot needs about
  10–15 minutes to come up to pressure first, which is not counted; natural-release chips; build-up times never get a
  timer. An **Instant Pot cheat sheet** (30 foods, 8 tips) that says to check the manufacturer manual.
* **Swap details:** ratio, what the swap changes and diet/allergen flags in the missing-ingredient hint, *Almost
  there* and the substitute dialog.
* **Backup:** adds `units`, `weightUnit`, `showCups`, `methods`; v1.6 and v1.5 backups still import (defaults apply).
* The service worker cache is `pr-core-1.7-web`.

## New in v1.6

Same features and backup format (format 2) as Android v1.6:
* **Appearance fix:** picking a theme, mode, text size, corners or accent no longer jumps the Appearance screen to the top
  (the list is re-rendered in place and the scroll position kept).
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

Web specifics: barcode scanning uses the browser's **BarcodeDetector** with the camera where it exists (Chrome on Android);
elsewhere (Safari/iPhone, Firefox) you type the number. Photos are kept in IndexedDB in this browser. Share uses
`navigator.share`, else copies to the clipboard and shows the text in a box. Timer alarms use WebAudio, vibration where
supported and a flashing tab title. The version is in `js/version.js`; the service worker cache is `pr-core-<version>` (now `pr-core-1.8-web`).

## New in v1.5

Same features and the same backup format as Android v1.5:
- **Use-by dates** on pantry items (tap a chip → *Set use-by date…*): a **Use soon** section, recipes that use soon-to-expire items are ranked first with a badge, and a *Use it up* carousel.
- **Optional quantities** (*Set quantity…*): recipe ingredients show **"You have enough"** / "You have X, need Y" (different kinds of units are not compared).
- **Weekly meal planner** (calendar button in the top bar), Monday–Sunday, with a **combined shopping list**.
- **1–5 star ratings and notes** on every recipe.
- **Dislikes & allergies** (*Settings → Allergies / avoid*) hide recipes and show "May contain" flags. **Name-based only, not a guarantee: always check labels.**
- **Backup** (*Settings → Backup*): one JSON file, interchangeable with the Android app. Import replaces the data in this browser.
- **Recipe import (web only)** (*Settings → Import a recipe*): paste plain text (title, *Ingredients*, *Method*) or a page's schema.org Recipe JSON-LD. Nothing is downloaded.
- Tonight's pick lists *Dinner* and *Main dish* first.
- **Appearance** (*Settings → Appearance*): Follow system / Light (default light green) / Dark, 15 more named presets (all WCAG AA for body text, secondary text, buttons and links), and a custom editor (colour pickers + hex, live preview and contrast readout) for up to 12 named custom themes; *Reset to default*. Saved, restored on reload and included in the backup. Every colour in `css/app.css` is a CSS variable set from the theme; the browser `theme-color` (status bar on Android Chrome / iOS 15+ Safari tabs) follows it. The iOS Home Screen `apple-mobile-web-app-status-bar-style` cannot change at runtime and stays `default`.
- The version lives in one place, `js/version.js`; the service worker cache is named `pr-core-<version>` (`pr-core-1.5-web`), so a new version replaces the old cache.

## Meal types (v1.3+)

v1.4 recipe counts by meal type: Breakfast 173, Brunch 146, Lunch 329, Dinner 675, Main dish 514, Side dish 143, Appetizer 90, Snack 207, Soup 90, Salad 70, Dessert 122, Baked goods 88, Drink 53, Sauce/Condiment 57. Long lists (Browse) show 120 at a time with a *Show more* button.

Every recipe has one or more meal types: Breakfast, Brunch, Lunch, Dinner, Main dish, Side dish, Appetizer, Snack, Soup, Salad, Dessert, Baked goods, Drink, Sauce/Condiment. On the Recipes and Discover tabs, multi-select **meal chips** (with *All meal types*) combine with the other filters, and two check boxes, **Include desserts** and **Include sides**, control whether dessert-only and side-only recipes appear. Both are unchecked by default, so main meals come first in results, "What can I cook tonight?" and *Surprise me*. Picking the *Dessert* or *Side dish* chip (or any course chip) shows those recipes regardless. The selection is shared by both tabs and remembered. When you pick a meal type, the tonight card and Surprise me follow it ("What can I cook for breakfast?"). Discover has **Browse by meal type** (with counts; Breakfast, Dessert, Appetizer and Side dish also load TheMealDB's matching category). Cards, list rows and the recipe screen show meal-type tags. TheMealDB recipes get meal types from their category (default: Main dish + Dinner). The Favorites tab, *Random recipe* and Browse by category/meal type are not filtered.

## Two ways to use it

1. **Hosted (recommended)** - put this folder on any static web host served over **HTTPS**. Then it installs to the Home Screen and works offline (service worker).
   - iPhone/iPad: open the link in **Safari**, tap **Share -> Add to Home Screen**.
   - Android / Fire tablet (Silk or Chrome): menu -> **Install app** / **Add to Home screen**.
2. **Single file** - `dist/my-pantry-recipes-v1.8.html` has everything inlined (code, styles, recipes, substitutions, icons). Open it directly in a browser; no server needed. See "Single-file limitations" below.

## Building the deliverables

`python3 tools/build_dist.py` writes `dist/my-pantry-recipes-v<version>.html` (single file) and `dist/my-pantry-recipes-web-v<version>.zip` (this folder plus the single file), reading the version from `js/version.js`. The hosted app itself needs no build.

## Hosting options (any static host works; nothing to build)

- **GitHub Pages**: push the folder contents to a repo, Settings -> Pages -> deploy from branch (root).
- **Netlify Drop**: drag the folder onto https://app.netlify.com/drop.
- **Cloudflare Pages, Vercel, Codeberg Pages, your own nginx/Apache**: serve the folder as static files.
- Local test: `python3 -m http.server 8000` in this folder, then open http://localhost:8000/ (service workers work on localhost; other devices need HTTPS).

## Files

| Path | What |
|---|---|
| `index.html` | app shell, iOS/PWA meta tags, CSP |
| `manifest.json` | web app manifest (standalone, icons incl. maskable) |
| `sw.js` | service worker: precaches the whole app; caches TheMealDB photos (max 300); API calls go to the network |
| `css/app.css` | styles (all colours are theme variables; light green by default, safe-area insets, 100dvh, 44px touch targets) |
| `js/version.js` | the version (used by the app, the service worker cache name and the build) |
| `js/core15.js` | v1.5 core, ported from Java: dates, use-by, allergens, amounts, meal plan, palettes/contrast, backup |
| `js/core16.js`, `js/data16.js` | v1.6 core ported from Java (durations, nutrition, cost, leftovers, buy-next, aisles, share text, barcode) and its data (USDA-derived food table, default prices) |
| `js/core17.js`, `js/data17.js` | v1.7 core ported from Java (units and conversions, cooking methods, pressure/release timers, swap details) and its data (densities, Instant Pot cheat sheet) |
| `js/helpers20.js` | v2.0 Stage 2 kitchen-helper text, generated from the Android `assets/helpers20.txt` and parsed on first open by `PR.Helpers20` in `core20.js` |
| `js/import15.js` | recipe import from text / JSON-LD (web only) |
| `js/core.js` | port of the Java core: normalizer, matcher, synonyms, quantities and scaling, diet tags, substitutions, Discover, TheMealDB conversion |
| `js/core-data.js` | tables exported from the Java code (synonyms, families, catalog, diet word lists) |
| `js/icons.js` | Material icons (Apache 2.0) as SVG paths |
| `js/app.js` | user interface |
| `data/recipes.json`, `data/substitutions.txt` | bundled data |
| `icons/` | apple-touch-icon (180), 192, 512, maskable 512, favicon |
| `dist/` | versioned build outputs (`tools/build_dist.py`) |
| `tools/` | `WebExport.java` (exports tables + test vectors from the Android sources), `V15Export.java` / `V16Export.java` / `V17Export.java` / `V18Export.java` (v1.5–v1.8 vectors), `sync_presets.py`, `build_data16.py`, `make_web_icons.py`, `build_single.py`, `build_dist.py` |
| `tests/` | `test-core.js` (JS port vs Java vectors), `validate.js` (JSON, manifest, file references), `browser-test.js` (headless Chrome), `a11y-audit.js` (automated accessibility audit) |

## Tests

```
node tests/test-core.js          # 78,507 checks: JS port gives the same results as the Java code
node tests/test-core15.js        # 10,260 checks: v1.5 core (dates, use-by, allergens, amounts, plan, palettes incl. 49 presets, backup) vs Java
node tests/test-core16.js        # 11,143 checks: v1.6 core (durations, nutrition, cost, leftovers, buy-next, aisles, share, barcode, backup format 2) vs Java
node tests/test-core17.js        # 13,703 checks: v1.7 core (unit display for every ingredient line in 6 settings x 3 scales, conversions, parsing, methods, pressure timers, swaps, backups incl. a real v1.6 file) vs Java
node tests/test-core18.js        # 4,847 checks: v1.8 core (title hints, allergens, barcode sizes, negative temperatures/ranges, theme id migration) vs Java
node tests/test-import15.js      # 37 checks: recipe import
node tests/validate.js           # JSON, manifest, icons, references, version source, no hard-coded colours, dist/ contents
python3 -m http.server 8765 &    # then, with puppeteer-core installed somewhere:
NODE_PATH=/path/to/node_modules node tests/a11y-audit.js http://127.0.0.1:8765/ [dark]   # 46 views: names, labels, alt, 44 px targets, AA contrast; prints {} when clean
NODE_PATH=/path/to/node_modules node tests/browser-test.js http://127.0.0.1:8765/   # headless Chrome, incl. v1.5–v1.8 features (layout, decorations, in-place pantry toggle, units persistence, AA on the new header/tabs/chips/Cook button), theming, scroll preservation and all-preset contrast
```

## Testing status

**v2.1 is verified at build level only. It has not been run on a phone, tablet or emulator.**
- The Java build-time checks pass: 925,092, with 0 failures.
- Web: the Node suites, `validate.js` (10,635 checks) and the headless-Chrome browser test all pass. The accessibility audit
  covers the new screens (fruit menu, category chooser, glossary, categories screen, Buy these next expanded and See all,
  and the diet guides) and finds 0 problems in both themes.
- `apicheck.py` finds the same calls above API 21 as v2.0.1: the v2.0 set plus `DisplayCutout`, all guarded.
- Both APKs report versionName 2.1 and versionCode 14 and are signed with the same key.
- Fire HD 8 timings are estimates from `-Xint` and from Chrome at 6× CPU throttle.


v2.0 final: `validate.js` 8,770/8,770; all `test-core*.js` suites 0 failures; `browser-test.js` 238 PASS / 0 FAIL; an
automated accessibility audit of 46 views (light and dark) found 0 problems (names, labels, alt text, 44 px targets,
AA contrast). Screen readers (VoiceOver, TalkBack) were not used.

All of the above run in Node and in **headless desktop Chrome with an iPhone-sized viewport and an iPhone user agent**. That is
not a real iPhone, iPad, Android phone or Fire tablet: Safari/WebKit, Home Screen install, Wake Lock on iOS and real
camera barcode scanning have not been tested on devices.

## Single-file limitations (`my-pantry-recipes-v2.1.html` opened as a file)

- No service worker, so no install-to-Home-Screen and no offline cache of TheMealDB photos (the built-in recipes are inside the file, so they always work offline).
- Storage depends on the browser: Chrome/Firefox keep data per file; some browsers block storage for `file://` - then the app keeps data in memory only and shows a warning banner.
- TheMealDB requests from a `file://` page work in desktop Chrome; some browsers block them - the app then shows built-in and saved recipes.
- iPhone: the Files app's preview (Quick Look) does not run JavaScript. Host the folder instead (best), or open the file in a browser that runs local HTML.

## Differences from the Android app

- Screen stays on in cook mode only where the Screen Wake Lock API exists (Safari 16.4+, Home Screen apps on iOS 18.4+, Chrome 84+). Elsewhere cook mode says so and suggests raising Auto-Lock. No hidden-video "NoSleep" hack.
- Step timers run while the page is open; iOS pauses web pages when the phone is locked or the app is in the background, so the alarm sounds when you return (the remaining time is computed from the end time, so it stays accurate). No background notifications.
- No crash report screen and no background timer notifications (both Android only).
- No two-pane tablet layout; wide screens get a two-column recipe list instead.
- The timer alarm is a short WebAudio beep plus vibration instead of the Android notification sound (iPhone browsers do not vibrate; sound needs the ringer volume up).

## Credits

Recipes, substitutions, ingredient densities and the Instant Pot cheat sheet: written for My Pantry Recipes (MIT License); conversions and cheat-sheet times are approximate. Online recipes and photos: TheMealDB (public test key "1"; get a supporter key from TheMealDB if you redistribute, and enter it in About). Icons: Material Design icons by Google (Apache License 2.0). Barcode product data: Open Food Facts (openfoodfacts.org), Open Database License (ODbL). Nutrition: USDA FoodData Central (SR Legacy), public domain. Default prices: rough hand-set US estimates; U.S. regional averages from the Bureau of Labor Statistics (Sep 2026, public domain); optional Open Prices lookup (Open Food Facts, ODbL); freezer/use-by times rounded from USDA FoodKeeper guidance. Diet labels are a guide, not certification. ZXing is not used by the web app (Android only, as an external intent; no ZXing code bundled).
