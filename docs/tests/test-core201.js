// Checks the v2.0.1 web diet guides (js/core201.js) against the Java core: the precomputed flags (assets/medflags.txt,
// shipped in js/data201.js) must equal the JS rules for every bundled recipe, and estimate/guide lines match tests/vectors201.json.
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
['core15', 'core16', 'data16', 'core17', 'data17', 'import15', 'data20', 'helpers20', 'core20', 'data201', 'core201'].forEach(f => require(path.join(ROOT, 'js/' + f + '.js')));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors201.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, msg) {
  checks++;
  const x = JSON.stringify(a), y = JSON.stringify(b);
  if (x !== y) { fails++; if (fails <= 30) console.log('FAIL ' + msg + '\n  got  ' + String(x).slice(0, 500) + '\n  want ' + String(y).slice(0, 500)); }
}
const M = PR.MedGuides;
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
const byId = new Map(recipes.map(r => [r.id, r]));
const FOODS = PR.Nutrition.parse(window.PR_DATA16.foods);
const pre = M.precomputed();
eq(pre.size, recipes.length, 'precomputed covers all recipes');
let t0 = Date.now();
recipes.forEach(r => eq(M.encode(M.compute(r)), (pre.get(r.id) || [''])[0], 'flags ' + r.id + ' ' + r.title));
const ms = Date.now() - t0;
V.samples.forEach(s => {
  const r = byId.get(s.id), e = PR.Nutrition.estimate(r, FOODS);
  eq(M.estimateLines(r, e.show ? (typeof e.show === 'function' ? (e.show() ? e.carbs : NaN) : e.carbs) : NaN, M.IDS, s.na, s.cb), s.lines, 'estimate lines ' + s.id);
  const f = M.flags(r);
  eq(M.IDS.map(g => M.lines(r, f, g)), s.guideLines, 'guide lines ' + s.id);
});
eq(M.DISCLAIMER, 'This is a general guide, not medical advice. Limits are different for every person and stage of illness. Follow the plan from your doctor or dietitian.', 'disclaimer');
eq(M.encode(M.decode('K0.3 P2 S5')), 'K0.3 P2 S5', 'round trip');
eq(M.encode(M.decode('X1 K P? ')), '', 'junk');
eq([M.clampSodium(999999), M.clampCarbs(-4), M.clampCarbs(NaN)], [20000, 0, 0], 'clamps');
// backup keys sanitize like Java
const B = PR.Backup;
eq(['medGuides', 'medSodiumMg', 'medCarbsG'].every(k => B.SETTINGS.indexOf(k) >= 0), true, 'backup keys');
const bd = B.parse('{"app":"Pantry Recipes","format":2,"settings":{"medGuides":["renal","x","renal","diabetic"],"medSodiumMg":1500.4,"medCarbsG":-3}}');
eq([bd.settings.get('medGuides'), bd.settings.get('medSodiumMg'), bd.settings.get('medCarbsG')], [['renal', 'diabetic'], 1500, 0], 'backup sanitize');
// fruits (v2.0.1)
const fi = PR.Catalog.CATEGORIES.indexOf('Fruits');
['Tomato', 'Cherry tomatoes', 'Cucumber', 'Avocado'].forEach(n => eq(PR.Diets.category(PR.normalize(n)), fi, n + ' in Fruits'));
['Bell pepper', 'Eggplant', 'Zucchini', 'Pumpkin', 'Olives'].forEach(n => eq(PR.Diets.category(PR.normalize(n)) !== fi, true, n + ' not in Fruits'));
console.log('core201: ' + checks + ' checks, ' + fails + ' failures (flags for ' + recipes.length + ' recipes in ' + ms + ' ms)');
process.exit(fails ? 1 : 0);
