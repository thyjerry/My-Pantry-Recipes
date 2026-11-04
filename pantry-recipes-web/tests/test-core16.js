// Checks the v1.6 web core (js/core16.js) against reference results from the Java core (tests/vectors16.json, written by tools/V16Export.java).
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
require(path.join(ROOT, 'js/core15.js'));
require(path.join(ROOT, 'js/core16.js'));
require(path.join(ROOT, 'js/data16.js'));
require(path.join(ROOT, 'js/core17.js')); // the shipped app always loads v1.7 core (pressure build-up spans are not timers)
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors16.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, msg) {
  checks++;
  const x = JSON.stringify(a), y = JSON.stringify(b);
  if (x !== y) { fails++; if (fails <= 40) console.log('FAIL ' + msg + '\n  got  ' + String(x).slice(0, 400) + '\n  want ' + String(y).slice(0, 400)); }
}
function ok(c, msg) { eq(!!c, true, msg); }
const J = (m) => PR.Json.write(m);
const nan = (d) => (d == null ? NaN : d);
const num = (d) => (isNaN(d) ? null : d);

const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
const byId = new Map(recipes.map((r) => [r.id, r]));
const foods = PR.Nutrition.parse(window.PR_DATA16.foods);
const defaults = PR.Cost.parseDefaults(window.PR_DATA16.prices);
ok(foods.size > 300, 'foods table loaded');
ok(defaults.size > 150, 'default prices loaded');

// durations
V.durations.forEach(([t, ms]) => eq(PR.Durations.find(t).map((m) => [m.start, m.end, m.seconds, m.maxSeconds, m.label(), m.text]), ms, 'durations ' + t));
V.durFormat.forEach(([s, f, r1, r2, c1, c2]) => eq([PR.Durations.format(s), PR.Durations.rangeLabel(s, s + 60), PR.Durations.rangeLabel(s, s + 3600),
  PR.Durations.clock(s * 1000), PR.Durations.clock(s * 1000 - 1)], [f, r1, r2, c1, c2], 'durFormat ' + s));

// nutrition
eq(V.nutrition.length, recipes.length, 'nutrition count');
let shown = 0;
V.nutrition.forEach(([id, m, s]) => { const e = PR.Nutrition.estimate(byId.get(id), foods); if (e.show()) shown++; eq([J(e.toMap()), PR.Nutrition.summary(e)], [m, s], 'nutrition ' + id); });
ok(shown > 1100, 'nutrition shown for most recipes (' + shown + ')');
V.grams.forEach(([k, u, g]) => eq(num(PR.Nutrition.grams(PR.Nutrition.lookup(foods, k), 1.5, u)), g, 'grams ' + k + ' ' + u));

// cost
const userObj = PR.Json.parse(V.costUser), user = new Map();
userObj.forEach((p, k) => user.set(k, PR.Cost.make(p.get('price'), p.get('amount'), p.get('unit'))));
V.cost.forEach(([id, sv, a, b, c]) => {
  const r = byId.get(id);
  eq([J(PR.Cost.estimate(r, r.servings, null, defaults, foods).toMap()), J(PR.Cost.estimate(r, sv, user, defaults, foods).toMap()),
    J(PR.Cost.estimate(r, sv, user, null, foods).toMap())], [a, b, c], 'cost ' + id);
});
V.money.forEach(([m, c, s]) => eq(PR.Cost.money(c, m), s, 'money ' + m + ' ' + c));
V.describe.forEach(([u, a, s]) => eq(PR.Cost.describe('$', PR.Cost.make(3.49, a, u)), s, 'describe ' + u + ' ' + a));
const rows = V.shopRows.map(([n, a, u, chk]) => [n, nan(a), u, chk]);
V.shopRows.forEach(([n, a, u, chk, c], k) => eq(num(PR.Cost.item(n, nan(a), u, user, defaults, foods)), c, 'cost item ' + n));
eq(PR.Cost.shopping(rows, user, defaults, foods), V.shopTotal, 'shopping total');
eq(PR.Cost.make(-1, 1, 'g'), null, 'negative price rejected');
eq(PR.Cost.make(1, 0, 'g'), null, 'zero amount rejected');
eq(PR.Cost.make(1, 1, 'cup'), null, 'unknown unit rejected');

// aisles and share text
V.aisles.forEach(([n, a]) => eq(PR.Aisles.of(n), a, 'aisle ' + n));
eq(PR.ShareText.shopping(rows), V.shareShopping, 'share shopping');
eq(PR.ShareText.plan('26 Oct \u2013 1 Nov', V.planRows), V.sharePlan, 'share plan');
eq(PR.ShareText.plan('', []), V.sharePlanEmpty, 'share plan empty');

// buy these next
const staples = ['salt', 'black pepper', 'olive oil', 'vegetable oil', 'water'];
V.buyNext.forEach(([pantry, max, picks], t) => {
  const m = new PR.Matcher(pantry, staples);
  eq(PR.BuyNext.fromResults(recipes.map((r) => m.match(r)), max).map((p) => [p.key, p.name, p.unlocks]), picks, 'buyNext ' + t);
});

// leftovers (replay the same operations)
const lo = [];
const entryJ = (e) => { const o = new Map(); o.set('id', e.id); o.set('recipe', e.recipe); o.set('title', e.title); o.set('portions', e.portions); o.set('cooked', e.cooked); o.set('useBy', e.useBy); return J(o); };
for (let k = 0; k < V.leftovers.length; k += 2) {
  const op = V.leftovers[k], want = V.leftovers[k + 1];
  if (op[0] === 'add') { const e = PR.Leftovers.add(lo, op[1], op[2], op[3], op[4], op[5]); eq(e ? e.id : null, op[6], 'leftover add ' + k); }
  else eq(PR.Leftovers.adjust(lo, op[1], op[2]), op[3], 'leftover adjust ' + k);
  eq(PR.Leftovers.sorted(lo).map((e) => [entryJ(e), PR.Leftovers.status(e, '2026-10-27')]), want, 'leftover state ' + k);
}
eq([-3, 0, 4, 99].map(PR.Leftovers.clampDays), V.leftoverDays, 'leftover days clamp');

// deduction
V.deduct.forEach(([h, key, n, u, r]) => eq(PR.Amounts.deduct(h, key, n, u), r, 'deduct ' + h + ' - ' + n + ' ' + u + ' ' + key));

// barcodes
V.barcodes.forEach(([c, cl, v]) => eq([PR.Barcode.clean(c), PR.Barcode.valid(c)], [cl, v], 'barcode ' + c));
V.off.forEach(([j, w]) => {
  const p = PR.Barcode.parse(j);
  eq(p == null ? null : [p.code, p.name, p.generic, p.brands, p.quantity, p.categories, p.title(), PR.Barcode.match(p)], w, 'off ' + j.slice(0, 60));
});

// backups: format 2 round trips, odd input, genuine v1.5 files
V.backups.forEach(([inp, out, sum], k) => {
  let got, s = null;
  try { const d = PR.Backup.parse(inp); got = PR.Backup.write(d); s = PR.Backup.summary(d); } catch (e) { got = 'ERR:' + e.message; }
  eq([got, s], [out, sum], 'backup ' + k + ' ' + inp.slice(0, 80));
});
const v15 = JSON.parse(fs.readFileSync(path.join(__dirname, 'fixtures/v15-backups.json'), 'utf8'));
const NEW = ['prices', 'leftovers', 'cooked', 'barcodes', 'photos'];
v15.forEach((txt, k) => {
  const back = PR.Json.parse(PR.Backup.write(PR.Backup.parse(txt))), orig = PR.Json.parse(txt);
  NEW.forEach((x) => back.delete(x));
  back.set('format', 1);
  eq(J(back), J(orig), 'v1.5 backup ' + k + ' imports without loss');
});

// palette 1.6 options
V.palette16.forEach(([s, st, col, fs1, cs]) => {
  const ps = PR.Palette.parseState(s);
  eq([J(ps.toMap()), J(PR.Palette.colorsToMap(PR.Palette.derive(ps.resolve(false)))), ps.fontScale(), ps.cornerScale()], [st, col, fs1, cs], 'palette16 ' + s);
});
V.randomPreset.forEach(([r, a, b]) => eq([PR.Palette.randomPreset('light', r), PR.Palette.randomPreset('nightshade', r)], [a, b], 'randomPreset ' + r));
ok(PR.Palette.PRESETS.length >= 40, 'at least 40 presets');

console.log((fails ? 'FAILED ' : 'OK ') + checks + ' checks, ' + fails + ' failures (v1.6 core vs Java)');
process.exit(fails ? 1 : 0);
