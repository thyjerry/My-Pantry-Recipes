// Checks js/core22.js (seeded variety) against the Java core (tests/vectors22.json from tools/V22More.java), plus speed.
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global; global.self = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
require(path.join(ROOT, 'js/data22.js'));
require(path.join(ROOT, 'js/core22.js'));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors22.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, msg) { checks++; const x = JSON.stringify(a), y = JSON.stringify(b); if (x !== y) { fails++; if (fails <= 20) console.log('FAIL ' + msg + '\n  got  ' + x.slice(0, 300) + '\n  want ' + y.slice(0, 300)); } }
const Va = PR.Variety;
V.seeds.forEach(s => eq(Va.seed(s[0]), s[1], 'seed ' + s[0]));
const r = new Va.Rng(12345); V.rng.forEach((x, i) => eq(r.nextInt(), x, 'rng ' + i));
const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
const res = recipes.map(x => ({ recipe: x }));
const ids = l => l.map(x => x.recipe.id).join(',');
let t0 = Date.now();
V.cases.forEach(c => {
  const sd = Va.daySeed(2026, 11, c.d, 'picks'); eq(sd, c.seed, 'daySeed ' + c.d);
  eq(ids(Va.picks(res, sd, 12)), c.picks, 'picks ' + c.d);
  eq(Va.one(res, sd, null).recipe.id, c.one, 'one ' + c.d);
  eq(Va.rotatingCuisines(res, Va.daySeed(2026, 11, c.d, 'cuisines'), 3, 8).join(','), c.cuisines, 'cuisines ' + c.d);
  const seen = new Set(); res.forEach((x, i) => { if (i % c.seenEvery !== 0) seen.add(x.recipe.id); });
  eq(Va.tryNew(res, seen, Va.daySeed(2026, 11, c.d, 'new')).recipe.id, c.tryNew, 'tryNew ' + c.d);
});
const ms = (Date.now() - t0) / V.cases.length;
const td = Va.todaySeed('picks'); eq(ids(Va.picks(res, td, 5)), ids(Va.picks(res.slice().reverse(), td, 5)), 'order-free');
// illustrations and sources vs Java (vectors22b.json)
const B = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors22b.json'), 'utf8'));
let t1 = Date.now();
eq(recipes.map(x => PR.Illustration.glyph(x)).join(','), B.glyphs, 'glyph per recipe');
const gms = Date.now() - t1;
const cz = Array.from(new Set(recipes.map(x => x.cuisine))).sort();
eq(cz.map(c => c + '=' + PR.Illustration.accent(c).toString(16) + '/' + PR.Illustration.pattern(c)).join(';'), B.looks, 'cuisine looks');
B.links.forEach(l => eq(PR.Sources.linkUrl(l[0], l[1]), l[2], 'link ' + l[0] + ' ' + l[1]));
eq(PR.Sources.wikibooksApi('chicken curry'), B.wbApi, 'wikibooks api'); eq(PR.Sources.openverseApi('Pão de queijo'), B.ovApi, 'openverse api');
eq(PR.Sources.parseWikibooks(B.wbIn).map(h => h.title + '|' + h.snippet + '|' + h.url).join('\n'), B.wbOut, 'wikibooks parse');
eq(PR.Sources.parseOpenverse(B.ovIn).map(p => p.thumb + '|' + PR.Sources.credit(p) + '|' + PR.Sources.encodePhoto(p)).join('\n'), B.ovOut, 'openverse parse');
const ph = PR.Sources.parseOpenverse(B.ovIn)[0]; eq(PR.Sources.decodePhoto(PR.Sources.encodePhoto(ph)), ph, 'photo round trip');
eq(PR.Sources.parseOpenverse('not json'), [], 'openverse bad json'); eq(PR.Sources.parseWikibooks('{}'), [], 'wikibooks empty');
const G = PR.Illustration.glyphs(); eq(Object.keys(G).length, 18, 'glyph count');
// every glyph background keeps the dark ink (0x99000000) at >= 3:1 (WCAG non-text contrast)
function lum(c) { return [16, 8, 0].map(s => { const v = ((c >> s) & 255) / 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }).reduce((a, v, i) => a + v * [0.2126, 0.7152, 0.0722][i], 0); }
function inkOn(bg) { const a = 0x99 / 255, mix = [16, 8, 0].map(s => Math.round(((bg >> s) & 255) * (1 - a))); return (mix[0] << 16) | (mix[1] << 8) | mix[2]; }
Object.keys(G).forEach(k => { const b = G[k].bg, r = (lum(b) + 0.05) / (lum(inkOn(b)) + 0.05); checks++; if (r < 3) { fails++; console.log('FAIL ink contrast ' + k + ' ' + r.toFixed(2)); } });
let t2 = Date.now(); let bytes = 0; recipes.forEach(x => { bytes += PR.Illustration.svg(x).length; }); const sms = Date.now() - t2;
const uniq = new Set(recipes.map(x => PR.Illustration.svg(x))).size;
eq(/<script|href|url\(/i.test(recipes.map(x => PR.Illustration.svg(x)).join('')), false, 'svg has no links or scripts');
console.log('illustrations: glyph for ' + recipes.length + ' recipes ' + gms + ' ms; svg build all ' + sms + ' ms; ' + uniq + ' distinct cards, avg ' + Math.round(bytes / recipes.length) + ' chars');
console.log('core22: ' + checks + ' checks, ' + fails + ' failures (picks+one+cuisines+tryNew over ' + res.length + ' recipes: ' + ms.toFixed(1) + ' ms per set)');
process.exit(fails ? 1 : 0);
