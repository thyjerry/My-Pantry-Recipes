/* Node test: compares the JavaScript port (js/core.js) with reference results produced by the
 * Java core (tools/WebExport.java -> tests/vectors.json). Run: node tests/test-core.js */
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, what) {
  checks++;
  const sa = JSON.stringify(a), sb = JSON.stringify(b);
  if (sa !== sb) { fails++; if (fails <= 40) console.log('FAIL ' + what + '\n  js:   ' + sa.slice(0, 400) + '\n  java: ' + sb.slice(0, 400)); }
}
const num = (x) => (x == null || Number.isNaN(x) ? null : x);

// normalize / family / isKnown
for (const [name, key, fam, known] of V.normalize) {
  const k = PR.normalize(name);
  eq(k, key, 'normalize(' + JSON.stringify(name) + ')');
  eq(PR.family(k), fam, 'family(' + k + ')');
  eq(PR.isKnown(k), known, 'isKnown(' + k + ')');
}
// recipes: keys, tags, total time
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
eq(recipes.length, V.recipes.length, 'recipe count');
const byId = new Map(recipes.map((r) => [r.id, r]));
V.recipes.forEach(([id, keys, tags, total], n) => {
  const r = recipes[n];
  eq(r.id, id, 'recipe order ' + n);
  eq(r.ingredients.map((i) => i.key), keys, 'keys ' + id);
  eq(PR.classify(r.ingredients), tags, 'classify ' + id);
  eq(PR.totalMinutes(r), total, 'total ' + id);
});
// bundled tags equal classifier (unless curated) - informative only
// quantity
for (const [s, a, u] of V.qparse) { const p = PR.parseQuantity(s); eq([num(p.amount), p.unit], [a, u], 'parse(' + JSON.stringify(s) + ')'); }
for (const [a, u, sc, out] of V.qformat) eq(PR.formatQty(a == null ? NaN : a, u, sc), out, 'format(' + a + ',' + u + ',' + sc + ')');
// substitutions
const subs = PR.Substitutions.parse(fs.readFileSync(path.join(ROOT, 'data/substitutions.txt'), 'utf8'));
eq(subs.categories, V.subsCategories, 'subs categories');
eq(subs.entries.length, V.subsEntries.length, 'subs entry count');
V.subsEntries.forEach((se, n) => {
  const e = subs.entries[n];
  if (!e) return;
  eq([e.category, e.name, e.keys, e.aliases, e.options.map((o) => [o.text, o.needs, o.needNames]), e.note], se, 'subs entry ' + se[1]);
});
for (const q of Object.keys(V.subsSearch)) {
  if (q.startsWith('forKey:')) { const e = subs.forKey(PR.normalize(q.slice(7))); eq(e ? e.name : null, V.subsSearch[q], q); }
  else eq(subs.search(q).map((e) => e.name), V.subsSearch[q], 'subs.search(' + q + ')');
}
// matcher + discover scenarios
V.scenarios.forEach((sc, si) => {
  const m = new PR.Matcher(sc.pantry, sc.staples).withSubstitutions(subs, sc.accepted);
  eq(m.pantryEmpty(), sc.pantryEmpty, 'pantryEmpty ' + si);
  const res = recipes.map((r) => m.match(r));
  const sorted = res.slice().sort(PR.compare);
  eq(sorted.length, sc.sorted.length, 'sorted length ' + si);
  sorted.forEach((r, n) => {
    eq([r.recipe.id, r.have, r.total, r.haveNonStaple, r.missing.map((i) => i.key), r.swappable.map((i) => i.key), r.substituted.map((i) => i.key)],
      sc.sorted[n], 'scenario ' + si + ' rank ' + n);
  });
  const ids = (l) => l.map((r) => r.recipe.id);
  eq(ids(PR.newForYou(res, new Set(sc.seen), 12)), sc.newForYou, 'newForYou ' + si);
  eq(ids(PR.quick(res, 12)), sc.quick, 'quick ' + si);
  eq(ids(PR.almostThere(res, 1000)), sc.almost, 'almost ' + si);
  eq(PR.basedOnFavorites(recipes, sc.favs.map((id) => byId.get(id)), 60).map((r) => r.id), sc.basedOnFavorites, 'basedOnFavorites ' + si);
  sc.subsAvailable.forEach(([k, opts]) => eq(subs.availableFor(k, m).map((o) => [o.text, PR.usesLabel(o, m)]), opts, 'availableFor ' + si + ' ' + k));
});
// facets
const fobj = (l) => { const o = {}; l.forEach(([k, v]) => { o[k] = v; }); return o; };
eq(fobj(PR.facet(recipes, true)), V.facets.cuisine, 'facet cuisine');
eq(fobj(PR.facet(recipes, false)), V.facets.category, 'facet category');
eq(Object.keys(fobj(PR.facet(recipes, true))), Object.keys(V.facets.cuisine), 'facet cuisine order');
for (const c of Object.keys(V.inCategory)) eq(recipes.filter((r) => PR.inCategory(r, c)).map((r) => r.id), V.inCategory[c], 'inCategory ' + c);
for (const q of Object.keys(V.searchTitles)) eq(PR.searchTitles(recipes, q, 500).map((r) => r.id), V.searchTitles[q], 'searchTitles ' + q);
// TheMealDB conversion
for (const [f, map] of V.mealdb) {
  const meal = JSON.parse(fs.readFileSync(path.join(__dirname, 'fixtures', f), 'utf8')).meals[0];
  const r = PR.convertMeal(meal);
  eq(PR.recipeToMap(r), map, 'mealdb convert ' + f);
  eq(PR.recipeToMap(PR.recipeFromMap(PR.recipeToMap(r))), map, 'mealdb roundtrip ' + f);
}
// meal types
eq(PR.MEAL_TYPES, V.mealTypes, 'meal type list');
for (const [c, t, out] of V.mealInfer) eq(PR.inferMeals(c, t), out, 'inferMeals(' + c + ', ' + t + ')');
for (const [c, out] of V.mealCanonical) eq(PR.mealCanonical(c), out, 'mealCanonical(' + c + ')');
for (const [m, c] of V.mealDbCategory) eq(PR.mealDbCategory(m), c, 'mealDbCategory(' + m + ')');
for (const [sel, d, sd, pass, pref, acc, hero] of V.mealFilters) {
  const w = JSON.stringify(sel) + ' d=' + d + ' s=' + sd;
  eq(recipes.filter((r) => PR.mealPasses(r.meals, sel, d, sd)).map((r) => r.id), pass, 'mealPasses ' + w);
  eq(recipes.filter((r) => PR.mealPreferred(r.meals, sel, d, sd)).map((r) => r.id), pref, 'mealPreferred ' + w);
  eq(recipes.filter((r) => PR.mealAcceptable(r.meals, sel, d, sd)).map((r) => r.id), acc, 'mealAcceptable ' + w);
  eq(PR.heroQuestion(sel), hero, 'heroQuestion ' + w);
}
recipes.forEach((r) => { eq(r.meals.length > 0 && r.meals.every(PR.isMealType), true, 'meal types valid ' + r.id); });
eq(PR.recipeFromMap({ id: 'm:1', title: 'Lamb Tagine', category: 'Lamb' }).meals, ['Dinner', 'Main dish'], 'old cached recipe infers meal types');
eq(PR.recipeFromMap({ id: 'x', title: 'T', meals: ['dessert', 'Bogus', 'Snack'] }).meals, ['Snack', 'Dessert'], 'fromMap canonicalises meal types');
// cook timer
for (const [s, mins] of V.stepMinutes) eq(PR.stepMinutes(s), mins, 'stepMinutes(' + s.slice(0, 50) + ')');
// round trip of every bundled recipe
recipes.forEach((r) => eq(PR.recipeToMap(PR.recipeFromMap(PR.recipeToMap(r))), PR.recipeToMap(r), 'roundtrip ' + r.id));

console.log(checks + ' checks, ' + fails + ' failures');
process.exit(fails ? 1 : 0);
