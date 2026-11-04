// Checks the v1.7 web core (js/core17.js) against reference results from the Java core (tests/vectors17.json, written by tools/V17Export.java).
'use strict';
const fs = require('fs'), path = require('path'), crypto = require('crypto');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
require(path.join(ROOT, 'js/core15.js'));
require(path.join(ROOT, 'js/core16.js'));
require(path.join(ROOT, 'js/core17.js'));
require(path.join(ROOT, 'js/data17.js'));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors17.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, msg) {
  checks++;
  const x = JSON.stringify(a), y = JSON.stringify(b);
  if (x !== y) { fails++; if (fails <= 40) console.log('FAIL ' + msg + '\n  got  ' + String(x).slice(0, 600) + '\n  want ' + String(y).slice(0, 600)); }
}
function ok(c, msg) { eq(!!c, true, msg); }
const nan = (d) => (d == null ? NaN : d);
const num = (d) => (d == null || isNaN(d) ? null : d);
const U = PR.Units, M = PR.Methods, D = PR.Durations, S = PR.Substitutions;

const ds = U.parseDensities(window.PR_DATA17.densities);
eq(ds.list.length, V.densityCount, 'density count');
eq(ds.problems, V.densityProblems, 'density problems');
ok(ds.list.length >= 150, '150+ densities');
const PREFS = [new U.Prefs('original', 'g', true), new U.Prefs('us', 'g', true), new U.Prefs('metric', 'g', true),
  new U.Prefs('weights', 'g', true), new U.Prefs('weights', 'oz', true), new U.Prefs('weights', 'g', false)];
eq([new U.Prefs().mode, new U.Prefs().weightUnit, new U.Prefs().showCups], ['weights', 'g', true], 'default prefs: weights in grams with cups');
eq(new U.Prefs('bogus', 'kg', false).mode, 'weights', 'invalid mode falls back to weights');

// ---- display for every bundled ingredient line and step
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
const byId = new Map(recipes.map((r) => [r.id, r]));
let lines = 0, dispFail = 0;
V.display.forEach(([id, want], ri) => {
  const r = byId.get(id);
  let s = '';
  r.ingredients.forEach((i) => PREFS.forEach((p) => [1, 2.5, 0.5].forEach((sc) => { s += U.displayIng(i, sc, p, ds) + '|'; lines++; })));
  s += '#';
  r.steps.forEach((st) => PREFS.forEach((p) => { s += U.stepText(st, p) + '|'; }));
  const got = ri < 150 ? s : crypto.createHash('sha1').update(s, 'utf8').digest('hex');
  if (got !== want) dispFail++;
  eq(got, want, 'display/stepText ' + id + ' ' + r.title);
});
eq(lines, V.displayLines, 'display line count');
V.displayEdge.forEach(([k, u, am, ...want]) => eq(PREFS.map((p) => U.display(k, nan(am), u, 1, p, ds)), want, 'display edge ' + k + ' ' + am + ' ' + u));

// ---- methods and timer kinds
V.methods.forEach(([id, want]) => eq(M.of(byId.get(id)), want, 'methods ' + id));
V.kinds.forEach(([st, want]) => eq(D.find(st).map((m) => [m.start, m.end, m.seconds, m.kind]), want, 'kinds ' + st));
const pm = D.find('Pressure cook on High for 12 minutes.')[0];
ok(pm.isPressure() && !pm.isRelease() && D.timerNote(pm).indexOf('10\u201315 min') >= 0 && D.timerNote(pm).indexOf('not counted') >= 0, 'pressure timer note');
const ipCount = recipes.filter((r) => M.has(r, 'instant-pot')).length;
ok(ipCount >= 150, '150+ Instant Pot recipes (' + ipCount + ')');
ok(M.matches(recipes[0], []) && M.matches(recipes[0], null), 'empty method filter matches');
eq(M.canonical(['one-pot', 'instant-pot', 'x', 'one-pot']), ['instant-pot', 'one-pot'], 'canonical method order');

// ---- swaps
const subs = S.parse(fs.readFileSync(path.join(ROOT, 'data/substitutions.txt'), 'utf8'));
const opts = [];
subs.entries.forEach((e) => e.options.forEach((o) => opts.push([e, o])));
eq(opts.length, V.subs.length, 'swap option count');
opts.forEach(([e, o], i) => {
  const w = V.subs[i];
  eq([e.name, o.text, S.flagsLine(o), S.claims(o), num(S.ratioValue(o.ratio)), o.vegetarian, o.vegan, o.dairyFree, o.glutenFree, o.allergens], w, 'swap ' + e.name + ': ' + o.text);
  S.claims(o).forEach((c) => ok(S.claimHolds(o, c), 'claim holds ' + c + ' ' + o.text));
});
ok(subs.entries.length >= 300, '300+ swap entries (' + subs.entries.length + ')');
V.ratios.forEach(([s, want]) => eq(num(S.ratioValue(s)), want, 'ratio ' + s));

// ---- conversions screen
V.parse.forEach(([s, want]) => eq(U.parseAmount(s), want, 'parseAmount ' + s));
V.temp.forEach(([s, sc, want]) => eq(U.tempLine(s, sc), want, 'tempLine ' + s + ' ' + sc));
V.lines.forEach(([x, u, vl, ml]) => eq([U.volumeLine(x, u), U.massLine(x, u)], [vl, ml], 'lines ' + x + ' ' + u));
V.ingredientLines.forEach(([k, x, u, want]) => eq(U.ingredientLine(x, u, ds.byKey.get(k)), want, 'ingredientLine ' + k + ' ' + x + ' ' + u));
V.butter.forEach(([x, u, g, t]) => {
  const gg = U.butterGrams(x, u);
  eq([num(gg), isNaN(gg) || gg <= 0 ? '' : U.butterText(gg)], [g, t], 'butter ' + x + ' ' + u);
});
V.format.forEach(([x, ...want]) => eq([U.frac(x, 0.25), U.frac(x, 0.5), U.grams(x, true), U.grams(x, false), U.metricMass(x, true), U.ounces(x),
  U.usMass(x), U.millilitres(x), U.tsp(x), U.tbsp(x), U.cups(x), U.usVolume(x), U.gasMark(x), U.roundF(x), U.roundC(x)], want, 'format ' + x));

// ---- backups with the v1.7 settings, and the genuine v1.6 file
V.backups.forEach(([inp, out, sum], k) => {
  let got, s;
  try { const d = PR.Backup.parse(inp); got = PR.Backup.write(d); s = PR.Backup.summary(d); } catch (e) { got = 'ERR:' + e.message; }
  eq([got, s], [out, sum], 'backup ' + k);
});
const b16 = PR.Backup.parse(fs.readFileSync(path.join(__dirname, 'fixtures/v16-backup.json'), 'utf8'));
ok(b16.appVersion === '1.6' && !b16.settings.has('units') && b16.settings.get('currency') === '\u20ac' && b16.settings.get('leftoverDays') === 3, 'v1.6 backup imports (no units key, settings kept)');
const b17 = PR.Backup.parse(V.backups[0][0]);
eq([b17.settings.get('units'), b17.settings.get('weightUnit'), b17.settings.get('showCups'), b17.settings.get('methods')], ['metric', 'oz', false, ['instant-pot', 'one-pot']], 'v1.7 settings parsed');

// ---- cheat sheet data
const d17 = window.PR_DATA17;
ok(/manufacturer/.test(d17.ipDisclaimer) && d17.ipTips.length >= 6 && d17.ipTimes.length >= 25 && d17.ipTimes.every((r) => r.length === 4), 'cheat sheet data');

// build-up time is never a timer
{
  const ms = PR.Durations.find('Pressure cook on High for 8 minutes. The pot first takes about 10 to 15 minutes to come up to pressure; that is not part of the cook time.');
  ok(ms.length === 1 && ms[0].seconds === 480 && ms[0].isPressure(), 'build-up "10 to 15 minutes to come up to pressure" is not offered as a timer');
  const m2 = PR.Durations.find('Allow 12 minutes to reach pressure, then cook 5 minutes at high pressure.');
  ok(m2.length === 1 && m2[0].seconds === 300 && m2[0].isPressure(), '"12 minutes to reach pressure" skipped; 5 min at high pressure kept');
}
console.log('core17: ' + checks + ' checks, ' + fails + ' failures (display recipes failing: ' + dispFail + ')');
process.exit(fails ? 1 : 0);
