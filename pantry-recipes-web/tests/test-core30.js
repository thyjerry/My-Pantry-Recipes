// Checks the v2.0 Stage 3 web core (js/core30.js: holidays, fasting labels, baking helpers) against the Java core
// (tests/vectors20.json "stage3", written by tools/V20Export.java from core/Stage3.java).
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
['core15', 'core16', 'data16', 'core17', 'data17', 'import15', 'data20', 'helpers20', 'core20', 'stage3-data', 'core30'].forEach(f => require(path.join(ROOT, 'js/' + f + '.js')));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors20.json'), 'utf8')).stage3;
let checks = 0, fails = 0;
function eq(a, b, msg) {
  checks++;
  const x = JSON.stringify(a), y = JSON.stringify(b);
  if (x !== y) { fails++; if (fails <= 40) console.log('FAIL ' + msg + '\n  got  ' + String(x).slice(0, 500) + '\n  want ' + String(y).slice(0, 500)); }
}
const S = PR.Stage3;
let t0 = Date.now();
const s = S.data();
const parseMs = Date.now() - t0;
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
eq([s.holidays.length, s.fasts.length, s.trouble.length, s.storage.length, s.altitude.length, s.party.length].join(','), V.counts, 'section counts');
const ids = s.holidays.map(h => h.id);
s.holidays.forEach(h => {
  const ds = []; for (let y = 2025; y <= 2031; y++) ds.push(String(S.dateIn(h, y)));
  eq(ds.join(','), V.dates[h.id], 'dates ' + h.id);
  const dr = ['us', 'ca', 'uk', 'eu', 'anz', 'other', null].map(r => S.defaultOn(h, r) ? '1' : '0').join('');
  eq(dr, V.defaults[h.id], 'defaults ' + h.id);
});
t0 = Date.now();
s.holidays.forEach(h => eq(S.section(s, h, recipes, 60).map(r => r.id).join(','), V.sections[h.id], 'section ' + h.id));
const secMs = Date.now() - t0;
Object.keys(V.upcoming).forEach(d => eq(S.upcoming(s, ids, d, 21).map(u => u.h.id + '@' + u.date + '/' + u.days).join(','), V.upcoming[d], 'upcoming ' + d));
const fids = s.fasts.map(f => f.id);
t0 = Date.now();
let fl = {};
recipes.forEach(r => { const l = S.labels(s, r, fids).map(f => f.id).join(','); if (l) fl[r.id] = l; });
const fastMs = Date.now() - t0;
const want = V.fasting, keys = new Set(Object.keys(want).concat(Object.keys(fl)));
keys.forEach(k => eq(fl[k] || '', want[k] || '', 'fasting ' + k));
let cb = S.bakers(437, 68.5, 2.2, 0.8, 20).map(d => d.toFixed(4) + ',').join('');
cb += S.flourFor(900, 72, 2, 0, 25).toFixed(4) + ',';
cb += S.panFactor(S.panArea('round', 20, 0), S.panArea('rect', 23, 33)).toFixed(6) + ',';
S.timeline(22 * 60 + 15, [15, 60, 600, 50, 45]).forEach(t => { cb += S.clock(t, false) + ';'; });
cb += S.startFor(7 * 60, [45, 120, 30]) + ',';
[100, 130, 150, 175, 205, 250].forEach(c => { cb += S.gasMark(c) + '/'; });
[0, 3000, 4999, 5000, 8000].forEach(ft => { const a = S.altitudeFor(s, ft); cb += (a ? a[0] : '-') + '/'; });
s.party.forEach(p => { cb += S.partyNeed(p, 37) + '/'; });
eq(cb, V.calc[0], 'calculators');
eq(S.easter(2026), '2026-04-05', 'easter 2026');
eq(S.orthodoxEaster(2026), '2026-04-12', 'orthodox easter 2026');
eq(S.clock(-30, true), '23:30 (-1 day)', 'clock back');
eq(typeof PR.Diets.isGrain, 'function', 'Diets.isGrain exported');
// Stage 3 backup keys: Java and JS sanitise the same messy input to the same text
eq(PR.Backup.write(PR.Backup.parse(V.backupIn)), V.backupOut, 'stage3 backup parity');
eq(V.backupOut.indexOf('"holidays":["easter","diwali"]') > 0, true, 'stage3 backup ids cleaned');
console.log('Stage 3 core: parse ' + parseMs + ' ms, ' + s.holidays.length + ' sections ' + secMs + ' ms, fasting labels for ' + recipes.length + ' recipes ' + fastMs + ' ms');
console.log(checks + ' checks, ' + fails + ' failures');
process.exit(fails ? 1 : 0);
