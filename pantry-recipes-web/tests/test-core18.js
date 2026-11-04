// Checks the v1.8 web changes against reference results from the Java core (tests/vectors18.json, written by tools/V18Export.java):
// small-ounce rounding, temperatures (negatives, ranges, gas ½), Open Food Facts quantities and matching, allergen flags,
// near-duplicate title hints and the unit-aware shopping share text.
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
require(path.join(ROOT, 'js/core15.js'));
require(path.join(ROOT, 'js/core16.js'));
require(path.join(ROOT, 'js/core17.js'));
require(path.join(ROOT, 'js/data17.js'));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors18.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, msg) {
  checks++;
  const x = JSON.stringify(a), y = JSON.stringify(b);
  if (x !== y) { fails++; if (fails <= 40) console.log('FAIL ' + msg + '\n  got  ' + String(x).slice(0, 600) + '\n  want ' + String(y).slice(0, 600)); }
}
const U = PR.Units, B = PR.Barcode, A = PR.Allergens, T = PR.Titles;

V.ounces.forEach(([oz, want]) => eq(U.ounces(oz), want, 'ounces ' + oz));
V.temps.forEach(([t, p, c, f]) => {
  eq(U.parseTemp(t), p, 'parseTemp ' + t);
  eq(U.tempLine(t, 'C'), c, 'tempLine C ' + t);
  eq(U.tempLine(t, 'F'), f, 'tempLine F ' + t);
});
V.gas.forEach(([g, want]) => eq(U.tempLine(g, 'gas'), want, 'gas ' + g));
eq(U.tempLine('\u00bd', 'gas').indexOf('250 \u00b0F') > 0, true, 'gas ½ is 250 °F');

V.quantity.forEach(([q, want]) => eq(B.quantity(q), want, 'quantity ' + q));
V.products.forEach(([json, title, qty, match, matches]) => {
  const p = B.parse(json);
  eq(p == null ? null : p.title(), title, 'product title ' + title);
  eq(p == null ? null : p.quantity, qty, 'product quantity ' + title);
  eq(p == null ? null : B.match(p), match, 'product match ' + title);
  eq(p == null ? null : B.matches(p, 4), matches, 'product matches ' + title);
});

V.allergenNames.forEach(([n, want]) => {
  const i = PR.makeIngredient(n, 1, '', false);
  const bits = A.IDS.filter((id) => A.has(id, i)).map((id) => id + (A.uncertain(id, i) ? '?' : '')).join(',');
  eq(bits, want, 'allergens for "' + n + '"');
});
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
const byId = new Map(recipes.map((r) => [r.id, r]));
V.recipeFlags.forEach(([id, want]) => {
  const r = byId.get(id), u = A.uncertainFlags(r);
  eq(A.flags(r).map((x) => A.flagLabel(r, x, u)).join('|'), want, 'recipe flags ' + id);
});
// conservative: every vegan-tagged recipe is free of animal allergens
recipes.filter((r) => r.tags.indexOf ? r.tags.indexOf('vegan') >= 0 : r.tags.has('vegan')).forEach((r) => {
  const bad = ['dairy', 'eggs', 'fish', 'shellfish'].filter((id) => r.ingredients.some((i) => A.has(id, i)));
  if (bad.length) eq(bad, [], 'vegan recipe flagged ' + r.id);
});

const hints = T.hints(recipes);
eq(hints.size, V.titleHints.length, 'title hint count');
V.titleHints.forEach(([id, h]) => eq(hints.get(id), h, 'title hint ' + id));
// v1.8: the web app uses hints precomputed into core-data.js for the built-in recipes
const preH = global.PR_CORE_DATA.titleHints || (typeof window !== 'undefined' && window.PR_CORE_DATA.titleHints) || {};
eq(Object.keys(preH).length, V.titleHints.length, 'precomputed title hint count');
V.titleHints.forEach(([id, h]) => eq(preH[id], h, 'precomputed title hint ' + id));
V.titleKeys.forEach(([t, k]) => eq(T.key(t), k, 'title key ' + t));
V.titleNear.forEach(([a, b, want]) => eq(T.near(new Set(T.words(a)), new Set(T.words(b))), want, 'near ' + a + ' / ' + b));

const ds = U.parseDensities(window.PR_DATA17.densities);
const rows = [['Flour', 250, 'g', false], ['Milk', 500, 'ml', false], ['Butter', 2, 'tbsp', false], ['Eggs', 3, '', false],
  ['Chicken breast', 1.5, 'lb', false], ['Salt', NaN, '', false], ['Olive oil', 0.25, 'cup', true], ['Cheddar', 4, 'oz', false]];
V.share.forEach(([mode, want]) => {
  if (mode === 'none') { eq(PR.ShareText.shopping(rows), want, 'share text as stored'); return; }
  const p = new U.Prefs(mode, 'g', true);
  eq(PR.ShareText.shopping(rows, (n, a, u) => U.display(PR.normalize(n), a, u, 1, p, ds)), want, 'share text ' + mode);
});

// v1.8: negative temperatures and ranges inside steps (same strings as RecipeTool's v18Checks)
{
  const pu = new PR.Units.Prefs('us', 'oz', true), pm = new PR.Units.Prefs('metric', 'g', true), st = PR.Units.stepText;
  eq(st('Freeze at -18 \u00b0C.', pu), 'Freeze at -18 \u00b0C (0\u00b0F).', 'step negative C');
  eq(st('Chill to -4 \u00b0F.', pm), 'Chill to -4 \u00b0F (-20\u00b0C).', 'step negative F');
  eq(st('Heat oil to 170-180 \u00b0C.', pu), 'Heat oil to 170-180 \u00b0C (340\u2013355\u00b0F).', 'step C range');
  eq(st('Fry at 350-375\u00b0F.', pm), 'Fry at 350-375\u00b0F (180\u2013190\u00b0C).', 'step F range');
  eq(st('-20 to -18 \u00b0C', pu), '-20 to -18 \u00b0C (-4 to 0\u00b0F)', 'step negative range');
  eq(st('Set to 200\u00b0C (400\u00b0F).', pu), 'Set to 200\u00b0C (400\u00b0F).', 'step already converted');
  eq(st('Bake 20-25 min at 180 \u00b0C.', pu), 'Bake 20-25 min at 180 \u00b0C (350\u00b0F).', 'step minutes range');
}
console.log(checks + ' checks, ' + fails + ' failures');
process.exit(fails ? 1 : 0);
