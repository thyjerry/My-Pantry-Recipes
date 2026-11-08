# Changelog – My Pantry Recipes (formerly Pantry Recipes)

Android app (`org.pantryrecipes.app`) and web/PWA version. Web versions carry the same number with a `-web` suffix.
Entries for v1.0–v1.4 are summarised from the READMEs; detailed notes were not kept for those releases.

## v2.3 (versionCode 16, web 2.3-web)
- Dedicated **Settings** button, labeled in text. Settings precede the About text on Android.
- The standalone swap icon is removed from the main header. A labeled **Ingredient substitutions** tool remains inside Settings.
- Recipe details offer substitutions beside every supported ingredient, not just missing ingredients. The dialog names the exact selected ingredient and scaled original amount.
- Simple single-ingredient volume/count ratios show a scaled replacement estimate. Mixtures, mass conversions and unknown amounts do not invent a quantity; the original option instructions remain visible. To-taste options remain to taste.
- Conservative name-based checks flag replacements conflicting with saved dislikes, avoided allergens, or selected vegan/vegetarian/dairy-free/gluten-free filters. Conflicting options are not counted as usable pantry substitutions. These checks are not certification: read labels and consider cross-contact and cooking requirements.
- Substitution search only matches ingredient names/aliases/categories, not unrelated ingredients mentioned inside replacement text.
- Cards/rows now use associated TheMealDB recipe photos where available; the generic meat/plant drawings are no longer the default. Missing images use a restrained **No photo** fallback. This does not add photos to the bundled collection.
- Images stay on the existing TheMealDB host and cache path. Android may read cached thumbnails when online recipes are off, but does not fetch new ones. Web cards hide remote images when online recipes are off; when enabled, existing service-worker caching supports offline reuse. Failed image loads leave the fallback visible.
- **Unchanged data:** 3,774 bundled recipes, 0 bundled photos. Larger licensed recipe expansion is pending; there is no 6,000 cap and no inflated recipe count.
- No EPUB/PDF importer, camera scanner, microphone, tracking or publishing added.

## v2.2 (versionCode 15, `2.2`, web `2.2-web`) – 2026-11-03
- **Recipes:** 3,774 recipes (up from 2,620). Adds 1,154 original dinner mains across 27 cuisines and 24 cuts of
  chicken, beef, pork, lamb, turkey, salmon, cod and shrimp. Each one has doneness temperatures and steps written for
  that cut. Diet and allergen tags and costs are computed the same way as before. Loading is still chunked: the web
  version now has 6 data chunks and the first recipes still appear before the rest load.
- **Variety:** "Today's picks" (12 a day, stays the same all day, Shuffle changes it), two "Cuisine of the day" rows,
  "Surprise me" with any recipe, and "Try something new".
- **Pictures:** small built-in drawings (inline SVG, 18 shapes, about 2 KB of data, nothing downloaded). An optional
  free-licensed photo per recipe from Openverse (CC0, public domain or CC BY, with credit). Photos load only after
  you tap and agree, and can be removed.
- **Sources:** search links to 10 recipe sites, an in-app Wikibooks Cookbook search (asks first, sends nothing before
  you tap), and 7 public-domain cookbooks from Project Gutenberg. See ATTRIBUTIONS.md.
- **Appearance:** the settings preview updates as you change them.
- **Your own category moves** replace the v2.1 "botanical fruit" badge and filter, which are removed.
- **Fixed:** looking up substitutions for "chicken thighs" listed pork entries. Search matched words inside the
  option text, so "Pork shoulder" and "Pork chop" came up. Name matches now rank first, and swaps to a different
  protein are labelled.
- **Fixed:** with a Sunday week start, the web meal-plan grid did not line up with its header. The grid used the
  Monday-based week on top of the Sunday header. Android ignored the Sunday setting. Both now use the same start.
- **Fixed (web):** finding or removing a photo now refreshes the recipe header.
- **Planned for 3.0** (approved, recorded only, not built in 2.2):
  - A camera barcode scanner using ZXing (open source, Apache-2.0). It runs on the device only; no Google Play
    Services and no ML Kit.
  - Recipe import from EPUB and PDF files, done on the device.

## v2.1 (versionCode 14, `2.1`, web `2.1-web`) – 2026-11-03
v2.0.1 was skipped, and its fixes are included here.
- **v2.0.1 fixes:**
  - The welcome region line says the suggestion comes from the device language and that location is never used.
  - Starter and cuisine kits are removed.
  - Content clears the status bar, the gesture bar and display cutouts.
  - Chips wrap long names.
  - Avocado, tomatoes, cherry tomatoes and cucumbers are under Fruits.
  - Discover no longer uses the Recipes filters (it has its own meal chips, `dMeals`).
  - Renal, cardiac and diabetic diet guides, off by default, with the fixed disclaimer. A recipe is never called "safe".
    Their settings are in backup and sync.
- **Botanical fruits:**
  - A 🍅 badge and a one-line fact in ingredient details for 24 ingredients.
  - An optional "Botanical fruits" filter chip, and the quick-add query "botanical".
  - A setting to move any ingredient to another category (`catMoves`, sanitized, in backup and sync), with undo and
    Reset all.
  - A "Botanical fruit" glossary entry with the Nix v. Hedden (1893) fun fact.
- **Buy these next, expandable:**
  - Tap a suggestion for why it is suggested, the recipes it unlocks (tappable), an estimated price (yours or the regional
    average) and Add to shopping list.
  - See all suggestions shows pages of 20 with Show more, sorted by Most recipes or Cheapest (the choice is remembered).
  - The details and the full list are computed lazily. The greedy top-3 pick runs in one pass per pick, with identical
    results and about 16× faster under `-Xint`.
- **+460 recipes (2,620 in all)** in every category: carnivore (160 in all), breakfast, drinks, slow cooker, Instant Pot,
  air fryer, 30-minute, vegetarian/vegan, baking by grams, world cuisines, and soups, salads and desserts.
- **Chunked lazy loading:**
  - Recipes ship in 5 chunks (500 + 4 × 530).
  - Android publishes chunk 0 first; Cook mode waits for the full set when it needs it.
  - The web app embeds or fetches the chunks and appends them after first paint.
  - The service worker precaches the chunks.
- **Planned for 3.0** (not in 2.1): URL recipe import, public-domain classics, chef link-outs, original "inspired by"
  recipes, and QR pairing.
- **Tests:**
  - 925,092 Java checks.
  - Web: validate 10,635; test-core21 125; browser test with new v2.1 checks.
  - Accessibility audit with 5 new views (light and dark): 0 findings.

## v2.0 (final, versionCode 13, `2.0`, web `2.0-web`) – 2026-11-03
- **Accessibility pass on all screens of both platforms:**
  - 48 dp (Android) / 44 px (web) minimum tap targets;
  - hint and secondary text raised to WCAG AA on every theme, including on selected tiles;
  - screen-reader names on icon-only controls and the new chips; decorative images hidden from screen readers;
  - darker ink on recipe thumbnails and letter tiles.
- **Fasting labels as optional filter chips** in the main diet filter. Off by default. They are memoised per recipe, so after
  the first pass filtering is a lookup.
- **Holiday notifications (Android):** optional and off by default. They use the notification permission and reboot re-arm
  path already used for use-by reminders. There is one notification per holiday, on a low-importance channel, "Holiday
  reminders".
- The fasting-chip and holiday-notification settings are in the backup and sync whitelist. API keys and secrets are still
  excluded.
- Lunar holiday tables still cover 2026–2030. Extending them is planned for a later release (README note).
- Version label: "v2.0" in the header and About.
- Tests: 803,751 Java checks; browser test 238; a new web accessibility audit (46 views × 2 themes, 0 findings).

## v2.0 Stage 3 (versionCode 12, `2.0-stage3`, web `2.0-stage3-web`) – 2026-11-02
This is a staged build, not final 2.0.
- **Fire HD 8 performance first (web):** the recipe data is now inert JSON that gets parsed after first paint in 300-recipe slices.
  Pantry and Shopping render before the recipes are ready. The budget first pass runs in 120-recipe slices.
  At 6× CPU throttle, boot-ready went from 697 to 472 ms and the heap from 15.3 to 8.5 MB.
- **Budget mode:** recipes with an unknown cost are now *kept, shown as price unknown* on both platforms.
  Android used to hide them, even though its label said otherwise.
- **Holidays:** 50 holidays worldwide (with many European ones), each with a date rule. Rules cover fixed dates,
  the nth weekday, Easter, Orthodox Easter, and published tables for lunar calendars for 2026–2030.
  - A *Choose holidays* checklist (region defaults until you change it).
  - A 12-month calendar.
  - A Pantry reminder 7, 14 or 21 days ahead, or off. It can be dismissed and only appears for ticked holidays.
  - A recipe section per holiday, matched on title, cuisine and ingredients. It respects each holiday's diet and exclusion
    rules and is capped at 60.
- **Fasting-day labels (opt-in):** meatless days, Orthodox fasting, vrat, Ekadashi, Jain and Buddhist vegetarian.
  Each ticked label shows on recipe pages and has a recipe list. They are labelled as a guide, since customs vary.
- **Baking section:**
  - recipes by group (breads, cakes, cookies & bars, pies & tarts, muffins/scones/pastry);
  - calculators: baker's percentages and hydration (and working back from a dough weight), a pan size converter,
    a dough timeline worked back from a ready time (3 presets with adjustable steps), oven conversions including
    gas mark and fan, high-altitude bands, and a party calculator;
  - a sourdough starter tracker and a bake log, both kept on the device;
  - troubleshooting (20 problems) and how long bakes keep (25 items).
- **Regional sets:** every country cuisine grouped by region, with recipe counts. 44 new original recipes for thin countries
  (Danish, Finnish, Norwegian, Chilean, Guatemalan, Ghanaian, Pakistani, Sri Lankan, Nepali, Burmese, Taiwanese, Ukrainian,
  Uzbek, Ecuadorian). There are now 2,160 recipes.
- **Backup:** the holiday, fasting, reminder, starter and bake-log settings are included, with the same keys on Android and web.
  API keys and secrets are still never written.
- **Accessibility:** new buttons have labels (content descriptions or aria-labels), and calculator results are live regions.
- **Planned for 3.0:** unchanged in this stage. No new ideas were approved.

## v2.0 Stage 2 (versionCode 11, `2.0-stage2`, web `2.0-stage2-web`) – 2026-11-02
This is a staged build, not final 2.0.
- **64 original carnivore recipes** covering beef, pork, lamb, poultry, fish and seafood, eggs, offal, bone broths, fats and egg/butter sauces.
  There are now 72 carnivore recipes and 2,116 recipes in total. The carnivore rule was updated, `short rib` is recognised as meat, and the catalog gained an *Offal & animal fats* section.
- **Kitchen helpers:** 151 equivalents, *Rescue my dish* (25 problems), a spice blend builder (27 blends: batch size,
  salt-free, milder, −/+ shares, pantry check, add missing to shopping), 93 cooking times and 62 ingredient explainers with local
  names by region. The data is parsed lazily, and on Android the search waits for a pause in typing.
- **Budget mode:** on Android the first pass runs on a background thread (it took about 400 ms interpreted on the bench box).
- **DATA-NOTES.md** lists disputed or shared Caribbean dish origins. Each dish keeps its most common attribution.
- **Trademark clean-up:** "Old Bay seasoning" became "Seafood seasoning" and "Nutella" became "Chocolate hazelnut spread".
- **Kept:** typed 10-character sync code (no QR).
- **Planned for 3.0 (recorded, not built):** PDF recipe import with on-device OCR (web and modern Android only, never
  the legacy HD 8 build); custom alarm sound import and a short alarm sound pack; optional game features behind one
  master off switch; and every later idea round. The full list is in README › *Planned for 3.0*.

## v2.0 Stage 1 (versionCode 10, `2.0-stage1`, web `2.0-stage1-web`) – 2026-10-30
This is the first of the staged 2.0 builds. It is not final 2.0.
- **Two APKs:** legacy (`org.pantryrecipes.app`, minSdk 21, Fire HD 8) and modern (`org.pantryrecipes.app.modern`, minSdk 26,
  target 35). The modern APK adds per-app language (English only), a themed icon, app shortcuts and a recipe card image.
  Both use the same signing key. No widgets.
- **Region** welcome and setting: formats, aliases and allergens per region. **Household** allergies (warn or hide).
- **15 diets** ("a guide, not certification"), including halal and kosher. Diets are precomputed at build time, so the HD 8
  never classifies the bundled recipes.
- **U.S. ZIP prices:** BLS regional averages for Sep 2026, labelled "regional average". User prices win. Other ingredients
  use built-in estimates × the region factor, and a coverage line is shown. Optional Open Prices lookup. **Budget mode.**
- **My recipes editor:** autocomplete, steps, times, servings, tags, a photo and *make my version*. Recipe packs, share as
  text and a printable cookbook. The card image is modern only.
- **Inventory:** locations, quantities and use-by dates. Freezer use-or-toss, a restock list and a spice rack with
  buy-next. Starter kits and 21 cuisine kits. A daily use-by reminder the day before, with *Cook this tonight*.
- **Themes:** 130 presets in 12 groups, including 81 Game day themes (50 light and 31 dark), all at AA. The grid shows one
  group at a time.
- **Cuisines:** 10 Caribbean countries, 166 recipes retagged, region-grouped cuisine chips, 16 new recipes (2,052 total).
- **Sync** Android ↔ Android over local Wi-Fi: code pairing, NSD or manual IP, encryption with HMAC, LWW with
  tombstones, and a conflict log. Secrets are excluded. Tested with simulated devices only. The web version has backup only.
- **Backup:** every new data feature is included in the JSON backup (secrets excluded). Old backups still import.
- **Fixed:** the web single-file build broke on `</` inside an inlined script; the diet chips dropped the 4 core diets.

## v1.8 (versionCode 9, web 1.8-web) – 2026-10-30
Rebrand, flat layout and speed for the Fire HD 8 (7th gen).
- **Rebrand** to **My Pantry Recipes** (manifest `short_name` "Pantry"), logo-derived icons. Package and signing key unchanged.
- **Layout:** centred header band with logo and name; pill segmented tabs Pantry / Recipes / Discover / Plan / Shopping
  (new **Plan** tab; Favorites is a chip under Recipes); filled selected chips; rounded flat recipe cards with letter tile,
  match bar and one dark **Cook** button; left-aligned section titles. No shadows, gradients, blur, animation or row bitmaps.
- **Decorations** setting (on by default): static accents only; web is CSS-only and respects `prefers-reduced-motion`.
  WCAG AA for all new text pairs in every theme.
- **Performance:** in-place Pantry updates on toggle; incremental matching (`MatchCache`); names/index built off the UI thread;
  lazy autocomplete; 30-row list pages; build-time title hints; memoised Discover browse cards. Web at 6× CPU throttle:
  toggle 45 → 31 ms, Recipes first paint 111 → 75 ms, DOM nodes on Recipes 854 → 505, heap 19 → 14 MB.
- **Fixed – units:** per-recipe Units switch now persists; Share, planner and shopping list honour units; finer small
  ounces; gas ½ = 250 °F; negative temperatures and ranges in steps (v1.7 dropped the minus sign and collapsed ranges).
- **Fixed – recipes:** near-duplicate titles get hints; online duplicates of bundled titles hidden; allergen over-flagging
  (coconut, nutmeg, eggplant, squash, cream of tartar, water chestnuts, ginger ale…); "Gluten (check label)" for oats/miso/soba.
- **Fixed – barcode:** cleaned package-size quantity and a confirm step with close matches.
- **Timers (Android):** AlarmManager alarm + notification when cook mode is not in front; no new permissions.
- **Themes renamed:** Citrine light, Citrine dark, Frostbite (old ids migrate).
- **Crash safety net (Android):** About shows "The app crashed last time – view/copy error" after a crash; report stays on device.
- Tests: migrations from v1.5/v1.6/v1.7 backups; 729,475 build-time checks; web Node suites and headless-Chrome test extended.

## v1.7 (versionCode 8, web 1.7-web)
- 830 more offline recipes (2,036 total), including 158 Instant Pot recipes.
- Measurement units (Recipe's own / US / Metric / Weights for dry baking, default) with a per-recipe switch; Conversions
  screen with 226 ingredient densities, butter helper and oven table; rounding rules.
- Cooking-method tags and filter chips; Instant Pot pressure/release timers and cheat sheet.
- Swap details (ratio, effect, diet/allergen flags). Backup adds units/methods settings (still format 2).

## v1.6 (versionCode 7, web 1.6-web)
- Appearance scroll-jump fix; 49 theme presets in 11 groups, text size, corners, accent quick pick, random theme (all AA).
- Add by barcode (Open Food Facts; external scanner intent on Android, BarcodeDetector on web).
- Nutrition estimate, cost per portion, "Cooked this" log with leftovers, batch scaling, your own photos.
- Multiple cook-mode timers, Buy these next, shopping list by aisle, Share list/plan. Backup format 2.

## v1.5 (versionCode 6, web 1.5-web)
- Use-by dates with Use soon section and ranking; optional pantry quantities ("You have enough").
- Weekly meal planner with a combined shopping list; ratings and notes; dislikes and allergies (name-based).
- JSON backup shared by Android and web; Appearance with light/dark, 15 presets and a custom theme editor.
- Web only: recipe import from text or JSON-LD.

## v1.4
- 645 more bundled recipes (generated from hand-written data in `tools/morerecipes`), more cuisines.

## v1.3
- Meal types on every recipe (Breakfast … Sauce/Condiment), meal chips on Recipes and Discover,
  Include desserts / Include sides, Browse by meal type.

## v1.0 – v1.2
- First releases: pantry with ingredient grid and autocomplete, recipe matching ("You have X of Y"), filters,
  Discover, substitutions, servings scaler, shopping list, favorites, cook mode, two-pane tablet layout, TheMealDB
  online recipes, then the web/PWA version with a single-file build (exact per-version split not recorded).
