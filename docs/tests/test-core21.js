// v2.1: botanical fruits and category moves (js/core21.js) against the Java core (tests/vectors21.json), backup key catMoves.
'use strict';
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
global.window = global;
require(path.join(ROOT, 'js/core-data.js'));
const PR = require(path.join(ROOT, 'js/core.js'));
global.PR = PR;
['core15', 'core16', 'data16', 'core17', 'data17', 'import15', 'data20', 'helpers20', 'core20', 'data201', 'core201', 'data21', 'core21'].forEach(f => require(path.join(ROOT, 'js/' + f + '.js')));
const V = JSON.parse(fs.readFileSync(path.join(__dirname, 'vectors21.json'), 'utf8'));
let checks = 0, fails = 0;
function eq(a, b, msg) {
  checks++;
  const x = JSON.stringify(a), y = JSON.stringify(b);
  if (x !== y) { fails++; if (fails <= 30) console.log('FAIL ' + msg + '\n  got  ' + String(x).slice(0, 400) + '\n  want ' + String(y).slice(0, 400)); }
}
const B = PR.Botany;
eq(B.LABELS.length, 24, '24 botanical-fruit labels');
B.LABELS.forEach(l => { eq(B.isBotanicalFruit(PR.normalize(l)), true, 'badge ' + l); eq(typeof B.fact(PR.normalize(l)), 'string', 'fact ' + l); });
['apple', 'carrot', 'potato', 'pea'].forEach(k => eq(B.isBotanicalFruit(k), false, 'no badge ' + k));
const mv = B.sanitize(V.raw);
eq(B.toObject(mv), V.sanitized, 'sanitize like Java');
const pk = new Set(V.pantry);
Object.keys(V.sections).forEach(c => { eq(B.sectionItems(+c, mv), V.sections[c], 'section ' + c); eq(B.have(+c, mv, pk), V.have[c], 'have ' + c); });
eq(B.sectionItems(0, new Map()), PR.Catalog.ITEMS[0].filter((s, i, a) => !(PR.Catalog.isHeader(s) && (i + 1 >= a.length || PR.Catalog.isHeader(a[i + 1])))), 'no moves = catalog');
eq(B.isQuery('Botanical fruits') && B.isQuery('botanic') && !B.isQuery('tomato'), true, 'botanical query');
eq(B.FUN_FACT.indexOf('Nix v. Hedden') > 0 && B.GLOSSARY.indexOf('flower') > 0, true, 'glossary + fun fact');
const helpers = fs.readFileSync(path.join(ROOT, 'js/helpers20.js'), 'utf8');
eq(helpers.indexOf('@ Botanical fruit |') >= 0, true, 'glossary entry in Kitchen helpers');
// backup
const K = PR.Backup;
eq(K.SETTINGS.indexOf('catMoves') >= 0, true, 'backup knows catMoves');
const bd = K.parse('{"app":"Pantry Recipes","format":2,"settings":{"catMoves":{"Tomato":"Vegetables","x":"Bad","apiKey":5}}}');
const cm = bd.settings.get('catMoves');
eq(cm instanceof Map ? B.toObject(cm) : cm, { tomato: 'Vegetables' }, 'backup catMoves sanitized');
const out = JSON.parse(K.write ? K.write(bd) : K.toJson(bd));
eq(out.settings.catMoves, { tomato: 'Vegetables' }, 'backup catMoves written as an object');
// ---- v2.1 Buy these next: ranked list, sort, texts (vs Java) ----
{
  const BV = V.buyNext, BN = PR.BuyNext;
  const recipes = PR.parseRecipeList(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8'));
  const ps = new Set(BV.pantry.map(PR.normalize)), ids = [], miss = [], names = new Map();
  recipes.forEach(r => { const m = []; r.ingredients.forEach(i => { if (!i.optional && i.key && !ps.has(i.key) && m.indexOf(i.key) < 0) { m.push(i.key); if (!names.has(i.key)) names.set(i.key, i.name); } }); ids.push(r.id); miss.push(m); });
  const t0 = Date.now(), rk = BN.ranked(ids, miss, names), ms = Date.now() - t0;
  eq(rk.map(o => ({ key: o.key, name: o.name, alone: o.alone, closer: o.closer, why: BN.why(o) })), BV.ranked, 'ranked options like Java (' + rk.length + ')');
  const user = new Map([[BV.userPrice, PR.Cost.make(0.05, 1, '')]]), defs = PR.Cost.parseDefaults(fs.readFileSync(path.join(ROOT, '../pantry-recipes/assets/prices.tsv'), 'utf8'));
  eq(BN.sort(rk, 'cheapest', user, defs).map(o => o.key), BV.cheapest, 'cheapest sort like Java');
  eq(BN.sort(rk, 'recipes', user, defs).map(o => o.key), rk.map(o => o.key), 'recipes sort keeps rank');
  const picks = BN.suggest(ids, miss, names, BN.MAX);
  eq(picks.map((p, i) => BN.whyPick(p, i, rk.find(o => o.key === p.key))), BV.whyPick, 'why texts for the top picks');
  eq(rk.slice(0, 12).map((o, i) => { const p = PR.Cost.find(o.key, null, defs); return p ? BN.costText(p, PR.Cost.money('$', p.price), i % 2 === 0) : null; }), BV.costText, 'cost texts');
  eq(picks.length > 0 && picks[0].key === rk[0].key || picks[0].unlocks.length >= rk[0].alone.length, true, 'first pick is the top ranked');
  console.log('buy-next ranked ' + rk.length + ' options over ' + recipes.length + ' recipes in ' + ms + ' ms');
}
console.log(checks + ' checks, ' + fails + ' failures');
process.exit(fails ? 1 : 0);
