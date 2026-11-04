// Checks the v2.0 web core (js/core20.js) against reference results from the Java core (tests/vectors20.json, written by
// tools/V20Export.java): diet tags for every bundled recipe (incl. halal and kosher), diet blockers, region names and
// defaults, ZIP regions and BLS prices, Open Prices parsing, inventory, cuisine groups, the recipe editor and household alerts.
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
['core15', 'core16', 'data16', 'core17', 'data17', 'import15', 'data20', 'helpers20', 'core20'].forEach(f => require(path.join(ROOT, 'js/' + f + '.js')));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors20.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, msg) {
  checks++;
  const x = JSON.stringify(a), y = JSON.stringify(b);
  if (x !== y) { fails++; if (fails <= 40) console.log('FAIL ' + msg + '\n  got  ' + String(x).slice(0, 700) + '\n  want ' + String(y).slice(0, 700)); }
}
const D = PR.Diets, R = PR.Region, RP = PR.RegionalPrices, I = PR.Inventory, E = PR.RecipeEditor;
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
const byId = new Map(recipes.map(r => [r.id, r]));
const FOODS = PR.Nutrition.parse(window.PR_DATA16.foods);

// diets: every bundled recipe, and stored tags == runtime tags
let t0 = Date.now();
Object.keys(V.diets).forEach(id => {
  const r = byId.get(id);
  if (!r) { eq(null, id, 'recipe missing ' + id); return; }
  const got = D.classify(r, FOODS);
  eq(got, V.diets[id], 'diets ' + id + ' ' + r.title);
  eq(r.diets, V.diets[id], 'stored diets ' + id);
});
const dietMs = Date.now() - t0;
V.blockers.forEach(([id, diet, want]) => eq(D.blockers(byId.get(id), diet), want, 'blockers ' + id + ' ' + diet));
eq(D.IDS.length, 11, 'diet count');
eq(D.name('halal'), 'Halal-friendly', 'halal name');

// region
V.localize.forEach(([region, t, ing, steps]) => { eq(R.localize(t, region, false), ing, 'localize ' + region + ' ' + t); eq(R.localize(t, region, true), steps, 'localize steps ' + region + ' ' + t); });
V.guess.forEach(([t, want]) => eq(R.guess(t), want, 'guess ' + t));
Object.keys(V.regions).forEach(region => {
  const w = V.regions[region], d = R.defaults(region);
  ['units', 'weightUnit', 'currency', 'dateFormat', 'priceSource', 'showCups', 'gasMarks', 'fanNote', 'decimalComma', 'cup250', 'clock24', 'weekStartsSunday', 'allergens']
    .forEach(k => eq(d[k], w[k], 'defaults ' + region + ' ' + k));
  eq(PR.Allergens.ALL_IDS.map(id => R.allergenLabel(id, region)), w.labels, 'allergen labels ' + region);
  eq(R.allergenListName(region), w.listName, 'list name ' + region);
  eq(R.holidayDefaults(region), w.holidays, 'holidays ' + region);
  eq(R.formatDate('2026-03-07', d.dateFormat), w.date, 'date ' + region);
  eq([18 * 60 + 5, 5, 720].map(m => R.formatTime(m, d.clock24)).join('|'), w.time, 'time ' + region);
  eq(R.weekStart('2026-10-28', d.weekStartsSunday) + '|' + R.weekStart('2026-11-01', d.weekStartsSunday), w.week, 'week ' + region);
  eq(R.money(d.currency, 3.5, d.decimalComma), w.money, 'money ' + region);
});

// prices
V.zips.forEach(([z, clean, st, reg]) => { eq(RP.cleanZip(z), clean, 'zip ' + z); eq(RP.state(z), st, 'state ' + z); eq(RP.region(z), reg, 'region ' + z); });
const table = RP.parse(window.PR_DATA20.bls), est = PR.Cost.parseDefaults(window.PR_DATA16.prices);
Object.keys(V.prices).forEach(region => {
  const w = V.prices[region], src = new Map(), p = RP.defaults(table, region, est, src), rows = [];
  p.forEach((x, k) => rows.push([k, x.price, x.amount, x.unit, src.get(k).kind, src.get(k).label]));
  eq(table.factor(region), w.factor, 'factor ' + region);
  eq(RP.coverage(table, region), w.coverage, 'coverage ' + region);
  eq(rows.length, w.rows.length, 'price rows ' + region);
  rows.forEach((r, k) => eq(r, w.rows[k], 'price ' + region + ' ' + r[0]));
});
V.openPrices.forEach(([region, want]) => eq(RP.parseOpenPrices(V.openPricesJson, region, 10).map(p => [p.price, p.date, p.store, p.city, p.state, p.nearby]), want, 'open prices ' + region));
eq([RP.openPricesUrl('0 12345-67890 5'), RP.openPricesUrl('123'), RP.openPricesUrl(null)], V.openPricesUrl, 'open prices url');

// inventory
V.inventory.forEach(([key, label, locw, months, toss, status]) => {
  eq([I.defaultLocation(key, label), I.freezerMonths(key), I.useOrTossDate('2026-01-31', key), I.freezerStatus('2026-05-15', key, '2026-10-30')], [locw, months, toss, status], 'inventory ' + key);
});
eq(I.spicesToBuy(new Set(['salt', 'black pepper', 'cumin']), I.keyCounts(recipes), 8), V.buyNext, 'buy next');
const ct = I.cookTonight(recipes, new Set(['spinach', 'chicken breast']), new Set(['garlic', 'onion', 'olive oil', 'salt']));
eq(ct ? ct.id : null, V.cookTonight, 'cook tonight');
eq(I.KITS, undefined, 'v2.0.1: starter kits removed');
eq('kits' in (global.PR_DATA20 || (global.window || {}).PR_DATA20 || {}), false, 'v2.0.1: no kits data');
eq(I.addMonths('2026-01-31', 1), '2026-02-28', 'addMonths clamp');
eq(I.addMonths('2024-01-31', 1), '2024-02-29', 'addMonths leap');
eq(I.addMonths('2026-11-15', 3), '2027-02-15', 'addMonths year');

// cuisines
V.cuisineGroups.forEach(([c, g, k]) => { eq(PR.Cuisines.group(c), g, 'cuisine group ' + c); eq(PR.Cuisines.known(c), k, 'cuisine known ' + c); });

// editor
V.lines.forEach(w => {
  const i = E.ingredientLine(w[0]);
  eq(i == null ? [w[0], null] : [w[0], i.name, isNaN(i.amount) ? null : i.amount, i.unit, i.optional, E.lineOf(i)], w, 'line ' + w[0]);
});
const dr = E.newDraft();
Object.assign(dr, { title: '  Sunday   Stew ', cuisine: 'Irish', prep: 20, cook: 120, servings: 6,
  ingredients: '1 lb beef chuck\n2 carrots\n1 onion\nsalt, optional\n\n2 cups beef stock', steps: '1. Brown the beef.\nStep 2: Add the rest.\n\n  Simmer 2 hours.',
  tags: 'Winter, family, winter,  Slow  Cook', note: "Grandma's." });
const built = E.build(dr, 'u:test1', FOODS);
eq(PR.recipeToMap(built), V.built, 'build');
eq([E.problem(dr), E.problem(E.newDraft())], V.problem, 'problem');
eq(E.shareText(built, 3), V.share, 'share text');
eq(E.cookbookHtml("Smith & Sons' <Book>", [built, recipes[0]], new Map([['u:test1', { stars: 4, note: 'Less salt <b>' }]]), '2026-10-30'), V.cookbook, 'cookbook');
const mv = E.myVersion(recipes[77]);
eq([mv.title, mv.basedOn, mv.ingredients, mv.steps], V.myVersion, 'my version');
eq(JSON.parse(E.packWrite('Mine', [built, recipes[0]], '2.0', '2026-10-30')), JSON.parse(V.pack), 'pack write');
const back = E.packParse(V.pack, ['u:test1'], FOODS, 1000);
eq(back.length, 1, 'pack parse count');
eq(back[0].id !== 'u:test1' && back[0].id.indexOf('u:') === 0, true, 'pack parse new id');
eq(back[0].diets, built.diets, 'pack parse diets');
let threw = '';
try { E.packParse('{"kind":"recipe-pack","format":2,"recipes":[]}', [], FOODS, 1); } catch (e) { threw = e.message; }
eq(threw, 'This recipe pack is from a newer version of the app.', 'pack newer');

// household
const ps = PR.Household.parse(JSON.parse(V.householdJson));
eq(PR.Household.toList(ps), V.household, 'household');
V.alerts.forEach(([id, want]) => eq(PR.Household.alerts(byId.get(id), ps, 'uk'), want, 'alerts ' + id));

// backup: v2.0 settings, secrets never kept
const bw = PR.Backup.write(PR.Backup.parse(V.backupSample));
eq(JSON.parse(bw).settings, JSON.parse(V.backupWrite).settings, 'backup v2.0 settings');
eq(JSON.parse(bw).pantry, JSON.parse(V.backupWrite).pantry, 'backup inventory fields');
eq(/SECRET/.test(bw), false, 'backup drops secrets');

// perf: localize 20k strings (HD8 budget)
t0 = Date.now();
for (let k = 0; k < 20000; k++) R.localize(V.localize[k % V.localize.length][1], 'uk', k % 2 === 0);
const locMs = Date.now() - t0;
// ---- v2.0 Stage 2 kitchen helpers: same expectations as V20Tests.helpers() in Java ----
(function () {
  const H = PR.Helpers20, t0 = Date.now(), h = H.data(), ms = Date.now() - t0;
  eq(true, h.equivs.length >= 150, 'equivalents 150+ (' + h.equivs.length + ')');
  eq(true, h.rescues.length >= 25, 'rescues 25+');
  eq(true, h.blends.length >= 25, 'blends 25+');
  eq(true, h.times.length >= 90, 'times 90+');
  eq(true, h.explains.length >= 60, 'explainers 60+');
  eq(V.helpers.counts, [h.equivs.length, h.rescues.length, h.blends.length, h.times.length, h.explains.length].join(','), 'helpers counts match Java');
  eq('1/4 tsp', H.spoons(0.25), 'spoons 1/4'); eq('1 1/2 tsp', H.spoons(1.5), 'spoons 1 1/2'); eq('1 tbsp', H.spoons(3), 'spoons 3');
  eq('2 tbsp + 1/2 tsp', H.spoons(6.5), 'spoons 6.5'); eq('pinch', H.spoons(0.05), 'spoons pinch'); eq(1.5, H.parseParts('1 1/2'), 'parts');
  const cj = h.blends[0], plain = H.build(cj, null, 8.5, false, false), ns = H.build(cj, null, 8.5, true, true);
  eq('2 tsp', plain[0][0], 'Cajun paprika'); eq(plain.length - 1, ns.length, 'no-salt drops salt'); eq('1 1/8 tsp', ns[1][0], 'no-salt+mild garlic');
  eq('coriander', H.localName(H.findExplains(h, 'cilantro')[0], 'anz'), 'local anz');
  eq('cilantro', H.localName(H.findExplains(h, 'cilantro')[0], 'ca'), 'local ca->us');
  eq('aubergine', H.localName(H.findExplains(h, 'eggplant')[0], 'eu'), 'local eu->uk');
  eq(true, H.findEquivs(h, 'butter stick').length >= 1, 'search butter stick');
  eq(true, H.findTimes(h, 'chickpeas instant pot').length >= 2, 'search chickpeas IP');
  eq(1, H.findRescues(h, 'salty').length, 'search salty');
  eq(1, H.findExplains(h, 'courgette').length, 'search courgette');
  // every blend at every batch size and option: identical to Java
  h.blends.forEach(function (b, i) { eq(V.helpers.blends[i], JSON.stringify(H.build(b, null, 12, i % 2 === 0, i % 3 === 0).map(function (r) { return r[0]; })), 'blend parity ' + b.name); });
  console.log('helpers: parse ' + ms + ' ms');
})();
eq(RP.withinBudget(null, 2), true, 'budget: unknown cost is kept');
eq(RP.withinBudget({ show: function () { return false; } }, 2), true, 'budget: no estimate is kept');
eq(RP.withinBudget({ show: function () { return true; }, perServing: 3 }, 2), false, 'budget: over is dropped');
console.log('core20: ' + checks + ' checks, ' + fails + ' failures (diets for ' + Object.keys(V.diets).length + ' recipes in ' + dietMs + ' ms; 20k localize ' + locMs + ' ms)');
process.exit(fails ? 1 : 0);
