/* Node test: compares the v1.5 JavaScript core (js/core15.js) with reference results produced by
 * the Java core (tools/V15Export.java -> tests/vectors15.json). Also checks theme palettes for
 * WCAG AA contrast and theme/backup round trips. Run: node tests/test-core15.js */
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR; window.PR = PR;
require(path.join(ROOT, 'js/core15.js'));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors15.json'), 'utf8'));
const { Json, Dates, UseBy, Allergens, Amounts, MealPlan, Palette, Backup } = PR;
let checks = 0, fails = 0;
function eq(a, b, what) {
  checks++;
  const sa = JSON.stringify(a), sb = JSON.stringify(b);
  if (sa !== sb) { fails++; if (fails <= 40) console.log('FAIL ' + what + '\n  js:   ' + sa.slice(0, 500) + '\n  java: ' + sb.slice(0, 500)); }
}
function ok(c, what) { checks++; if (!c) { fails++; if (fails <= 40) console.log('FAIL ' + what); } }
const num = (x) => (x == null || Number.isNaN(x) ? null : x);
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
const byId = new Map(recipes.map((r) => [r.id, r]));

// dates
for (const [s, valid, days, label, plus40, minus400, ws, dow, w6, btw] of V.dates) {
  eq(Dates.valid(s), valid, 'valid ' + s);
  eq(Dates.label(s), label, 'label ' + s);
  if (!valid) continue;
  eq(Dates.toDays(s), days, 'toDays ' + s); eq(Dates.fromDays(days), s, 'fromDays ' + s);
  eq([Dates.addDays(s, 40), Dates.addDays(s, -400), Dates.weekStart(s), Dates.dayOfWeek(s), Dates.week(s)[6], Dates.between('2026-10-29', s)],
    [plus40, minus400, ws, dow, w6, btw], 'date ops ' + s);
}
// use-by
for (const [d, st, label] of V.useByLabels) eq([UseBy.status(d), UseBy.label(d)], [st, label], 'useBy ' + d);
for (const [s, d] of V.daysLeft) eq(UseBy.daysLeft(s, '2026-10-29'), d, 'daysLeft ' + s);
// allergens
const dis1 = ['mushroom', 'coriander', 'onion'], dis2 = ['chicken', 'milk', 'pepper', 'olive oil'], av1 = ['peanuts', 'gluten', 'shellfish'];
V.allergens.forEach(([id, flags, r1, r2, h1, h0, r3, warn]) => {
  const r = byId.get(id);
  eq([Allergens.flags(r), Allergens.reasons(r, dis1, null), Allergens.reasons(r, dis2, av1), Allergens.hidden(r, dis1, av1), Allergens.hidden(r, [], []),
    Allergens.reasons(r, null, Allergens.IDS.slice()), r.ingredients.filter((i) => Allergens.ingredientWarn(i, dis2, av1)).length],
  [flags, r1, r2, h1, h0, r3, warn], 'allergens ' + id);
});
for (const [s, k] of V.dislikeKeys) eq(Allergens.dislikeKey(s), k, 'dislikeKey ' + s);
ok(/name only/.test(Allergens.NOTE) && /not a guarantee/.test(Allergens.NOTE), 'allergen note wording');
// amounts
for (const [u, c, d] of V.canon) eq([Amounts.canon(u), Amounts.dimension(u)], [c, d], 'canon ' + u);
for (const [a, x, y, out] of V.convert) {
  const got = num(Amounts.convert(a, x, y));
  checks++; if (!((got === null && out === null) || (got !== null && out !== null && Math.abs(got - out) < 1e-9 * Math.max(1, Math.abs(out))))) { fails++; console.log('FAIL convert ' + x + '->' + y, got, out); }
}
for (const [id, scale, qp, out] of V.amountChecks) {
  const got = Amounts.check(byId.get(id), scale, new Map(qp)).map((c) => [c.ingredient.key, c.status, c.haveText, Amounts.label(c)]);
  eq(got, out, 'amounts ' + id + ' x' + scale);
}
// meal plan + use-by ranking
function close(a, b) { return a === b || (a != null && b != null && Math.abs(a - b) < 1e-9 * Math.max(1, Math.abs(b))); }
V.plans.forEach(([pantry, staples, pq, ent, wk, pr, items], n) => {
  const m = new PR.Matcher(pantry, staples);
  const entries = ent.map(([d, id, s]) => MealPlan.entry(d, id, s));
  const week = MealPlan.forWeek(entries, '2026-10-26');
  eq(week.map((e) => e.date + '|' + e.id), wk, 'forWeek ' + n);
  eq(MealPlan.prune(entries, '2026-10-29', 1).map((e) => e.date + '|' + e.id), pr, 'prune ' + n);
  const got = MealPlan.shopping(week, (id) => byId.get(id), m, new Map(pq));
  eq(got.length, items.length, 'shopping count ' + n);
  got.forEach((it, k) => {
    const j = items[k] || [];
    eq([it.key, it.name, it.unit, it.recipes, it.partial, MealPlan.text(it)], [j[0], j[1], j[3], j[4], j[5], j[6]], 'shopping item ' + n + '/' + k);
    ok(close(num(it.amount), j[2]), 'shopping amount ' + n + '/' + k);
  });
});
V.boosts.forEach(([pantry, staples, ubp, soon, ids, up], n) => {
  const m = new PR.Matcher(pantry, staples);
  const sk = UseBy.soonKeys(ubp, '2026-10-29');
  eq(sk.slice().sort(), soon.slice().sort(), 'soonKeys ' + n);
  const res = recipes.map((r) => m.match(r));
  eq(UseBy.sort(res.slice(), sk).map((r) => r.recipe.id), ids, 'useBy sort ' + n);
  eq(UseBy.useItUp(res, sk, 12).map((r) => r.recipe.id), up, 'useItUp ' + n);
});
for (const [inp, out] of V.heroOrder) eq(PR.heroOrder(inp), out, 'heroOrder ' + inp);
// JSON
for (const [s, out] of V.json) { let got; try { got = Json.write(Json.parse(s)); } catch (e) { got = 'ERR'; } eq(got, out, 'json ' + s); }
// backups
for (const [s, out, sum] of V.backups) {
  let got, gs = null;
  try { const d = Backup.parse(s); got = Backup.write(d); gs = Backup.summary(d); } catch (e) { if (!e.backup) throw e; got = 'ERR:' + e.message; }
  eq(got, out, 'backup ' + s.slice(0, 80)); eq(gs, sum, 'summary ' + s.slice(0, 80));
  if (!got.startsWith('ERR')) eq(Backup.write(Backup.parse(got)), got, 'backup idempotent');
}
// palette (Java parity)
const P = Palette;
for (const [id, name, bg, sf, pr, ac, tx, map, warn, pairs] of V.palette) {
  const b = P.base(id, name, P.parseHex(bg), P.parseHex(sf), P.parseHex(pr), P.parseHex(ac), P.parseHex(tx));
  const c = P.derive(b);
  eq(Json.write(P.colorsToMap(c)), map, 'derive ' + id);
  eq(P.warnings(b), warn, 'warnings ' + id);
  eq(P.textPairs(c).map((p) => P.ratio(P.contrast(p[1], p[2]))), pairs, 'pairs ' + id);
}
for (const [c, h, s, v, back] of V.hsv) {
  const g = P.toHsv(c);
  ok(close(g[0], h) && close(g[1], s) && close(g[2], v), 'toHsv ' + c);
  eq(P.fromHsv(Math.round(h), Math.round(s * 100) / 100, Math.round(v * 100) / 100), back, 'fromHsv ' + c);
}
for (const [s, n] of V.parseHex) eq(P.parseHex(s), n, 'parseHex ' + s);
for (const [s, out] of V.themeStates) eq(Json.write(P.parseState(s).toMap()), out, 'parseState ' + s.slice(0, 60));

// palette requirements (independent of Java)
ok(P.PRESETS.length >= 49, 'at least the 49 v1.6 presets (' + P.PRESETS.length + ')');
ok(P.PRESETS.slice(0, 17).map((b) => b.id).join() === 'light,dark,ocean,midnight,sunset,lavender,rose,espresso,slate,forest,citrus,mint,plum,contrast,contrast-dark,terracotta,arctic', 'v1.5 ids kept first');
const ids = new Set(), sigs = [];
for (const b of P.PRESETS) {
  ok(!ids.has(b.id), 'unique id ' + b.id); ids.add(b.id);
  const c = P.derive(b);
  for (const [what, fg, bgc, min] of P.textPairs(c)) ok(min >= 3 && P.contrast(fg, bgc) >= min, b.id + ': ' + what + ' ' + P.contrast(fg, bgc).toFixed(2));
  ok(P.contrast(b.text, b.bg) >= 4.5 && P.contrast(b.text, b.surface) >= 4.5, b.id + ' body text AA');
  ok(P.contrast(c.onFill, c.fill) >= 4.5, b.id + ' button text AA');
  eq(P.warnings(b), [], b.id + ' has no warnings');
  sigs.push([b.id, b.bg, b.primary, b.accent]);
}
// distinctness: every pair of presets differs clearly in background or primary
function dist(a, b) { const d = (s) => [(a >> s) & 255, (b >> s) & 255]; let t = 0; for (const s of [16, 8, 0]) { const [x, y] = d(s); t += (x - y) * (x - y); } return Math.sqrt(t); }
for (let i = 0; i < sigs.length; i++) for (let j = i + 1; j < sigs.length; j++) {
  const [ia, ba, pa, aa] = sigs[i], [ib, bb, pb, ab] = sigs[j];
  ok(dist(ba, bb) + dist(pa, pb) + dist(aa, ab) > 60, 'distinct ' + ia + ' vs ' + ib);
}
ok(P.isDark(P.preset('dark').bg) && !P.isDark(P.preset('light').bg), 'light/dark modes');
// persistence: state round trip incl. system mode and several customs
const st = new P.State();
for (let k = 0; k < 3; k++) st.put(P.base(st.nextCustomId(), 'Mine ' + k, 0x101010 * (k + 1), 0x202020, 0x2E9E5B, 0xF2B33D, 0xFFFFFF));
st.active = 'custom-2';
const st2 = P.parseState(Json.write(st.toMap()));
eq(Json.write(st2.toMap()), Json.write(st.toMap()), 'theme state round trip');
eq(st2.resolve(false).id, 'custom-2', 'resolve custom');
st2.active = 'system';
eq([P.parseState(Json.write(st2.toMap())).resolve(true).id, P.parseState(Json.write(st2.toMap())).resolve(false).id], ['dark', 'light'], 'system mode');
st2.remove('custom-2'); eq(st2.find('custom-2'), null, 'delete custom');
for (let k = 0; k < 20; k++) st2.put(P.base(st2.nextCustomId(), 'x', 0, 0, 0, 0, 0xFFFFFF));
eq(st2.custom.length, P.MAX_CUSTOM, 'custom limit');
// theme in backup
const d = Backup.newData(); d.theme = st; d.appVersion = '1.5-web'; d.platform = 'web';
const back = Backup.parse(Backup.write(d));
eq(Json.write(back.theme.toMap()), Json.write(st.toMap()), 'theme survives backup');

console.log((fails ? 'FAILED ' : 'OK ') + checks + ' checks, ' + fails + ' failures (v1.5 core vs Java)');
process.exit(fails ? 1 : 0);
