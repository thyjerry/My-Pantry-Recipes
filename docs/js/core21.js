/* My Pantry Recipes web v2.1: "botanically a fruit" badge and facts, the glossary fun fact, and the user's own
 * ingredient category moves (port of the Android core Botany.java; texts come from js/data21.js). MIT License. */
(function (root) {
  'use strict';
  var PR = root.PR, C = PR.Catalog, D = root.PR_DATA21 || {};
  var LABELS = D.labels || [], FACTS = D.facts || {}, MAX_MOVES = 300;
  var byKey = null;
  function map() {
    if (!byKey) { byKey = new Map(); LABELS.forEach(function (l) { byKey.set(PR.normalize(l), l); }); }
    return byKey;
  }
  function isBotanicalFruit(key) { return key != null && map().has(key); }
  function label(key) { return map().get(key) || null; }
  function fact(key) { return isBotanicalFruit(key) ? FACTS[key] || null : null; }
  function isQuery(q) { return PR.normalize(String(q || '')).indexOf('botanic') === 0 || String(q || '').toLowerCase().trim().indexOf('botanic') === 0; }
  function categoryIndex(name) { return C.CATEGORIES.indexOf(name); }
  var defMemo = null;
  function defaultCategory(key) {
    if (!defMemo) {
      defMemo = new Map();
      C.ITEMS.forEach(function (items, ci) { items.forEach(function (s) { if (!C.isHeader(s)) { var k = PR.normalize(s); if (!defMemo.has(k)) defMemo.set(k, ci); } }); });
    }
    return defMemo.has(key) ? defMemo.get(key) : -1;
  }
  function labelFor(key) {
    var found = null;
    C.ITEMS.forEach(function (items) { items.forEach(function (s) { if (!found && !C.isHeader(s) && PR.normalize(s) === key) found = s; }); });
    return found || (key ? key.charAt(0).toUpperCase() + key.slice(1) : key);
  }
  /** Map or plain object in, Map out: real categories, normalized keys of 1-60 chars, at most 300. */
  function sanitize(m) {
    var out = new Map();
    if (!m || typeof m !== 'object' || Array.isArray(m)) return out;
    var entries = m instanceof Map ? Array.from(m.entries()) : Object.keys(m).map(function (k) { return [k, m[k]]; });
    for (var i = 0; i < entries.length && out.size < MAX_MOVES; i++) {
      var k = entries[i][0], v = entries[i][1];
      if (typeof k !== 'string' || typeof v !== 'string') continue;
      var nk = PR.normalize(k);
      if (!nk || nk.length > 60 || categoryIndex(v) < 0) continue;
      out.set(nk, v);
    }
    return out;
  }
  function toObject(m) { var o = {}; m.forEach(function (v, k) { o[k] = v; }); return o; }
  function sectionItems(ci, moves) {
    var out = [], pending = null;
    C.ITEMS[ci].forEach(function (s) {
      if (C.isHeader(s)) { pending = s; return; }
      var to = moves.get(PR.normalize(s));
      if (to != null && categoryIndex(to) !== ci) return;
      if (pending != null) { out.push(pending); pending = null; }
      out.push(s);
    });
    var head = false;
    moves.forEach(function (v, k) {
      if (categoryIndex(v) !== ci || defaultCategory(k) === ci) return;
      if (!head) { out.push('#Moved here by you'); head = true; }
      out.push(labelFor(k));
    });
    return out;
  }
  function sectionKeys(ci, moves) { return sectionItems(ci, moves).filter(function (s) { return !C.isHeader(s); }).map(PR.normalize); }
  function have(ci, moves, pantryKeys) { var n = 0; sectionKeys(ci, moves).forEach(function (k) { if (pantryKeys.has(k)) n++; }); return n; }
  PR.Botany = { LABELS: LABELS, TERM: D.term || 'Botanical fruit', GLOSSARY: D.glossary || '', FUN_FACT: D.funFact || '', MAX_MOVES: MAX_MOVES,
    isBotanicalFruit: isBotanicalFruit, label: label, fact: fact, isQuery: isQuery, categoryIndex: categoryIndex, defaultCategory: defaultCategory,
    labelFor: labelFor, sanitize: sanitize, toObject: toObject, sectionItems: sectionItems, sectionKeys: sectionKeys, have: have };

  // ---------- v2.1 "Buy these next": expand, see all, sort (same rules as Android BuyNext.java) ----------
  var BN = PR.BuyNext;
  function plural(n, one, many) { return n + ' ' + (n === 1 ? one : many); }
  function demand(o) { return o.alone.length + o.closer.length; }
  function ranked(ids, missing, names) {
    var by = new Map();
    ids.forEach(function (id, i) {
      var u = Array.from(new Set(missing[i]));
      if (u.length < 1 || u.length > 2) return;
      u.forEach(function (k) {
        var o = by.get(k);
        if (!o) { var nm = names ? names.get(k) : null; o = { key: k, name: nm == null || nm === '' ? k : nm, alone: [], closer: [] }; by.set(k, o); }
        (u.length === 1 ? o.alone : o.closer).push(id);
      });
    });
    return Array.from(by.values()).sort(function (a, b) {
      if (a.alone.length !== b.alone.length) return b.alone.length - a.alone.length;
      if (demand(a) !== demand(b)) return demand(b) - demand(a);
      return a.key < b.key ? -1 : a.key > b.key ? 1 : 0;
    });
  }
  function rankedFromResults(results) {
    var ids = [], miss = [], names = new Map();
    results.forEach(function (r) {
      if (r.missing.length < 1 || r.missing.length > 2) return;
      ids.push(r.recipe.id);
      miss.push(r.missing.map(function (i) { if (!names.has(i.key)) names.set(i.key, BN.shortName(i.name)); return i.key; }));
    });
    return ranked(ids, miss, names);
  }
  function sortOptions(list, by, user, defaults) {
    var out = list.slice();
    if (by !== 'cheapest') return out;
    var pr = new Map(), pos = new Map();
    out.forEach(function (o, i) { var p = PR.Cost.find(o.key, user, defaults); pr.set(o.key, p ? p.price : NaN); pos.set(o.key, i); });
    return out.sort(function (a, b) { // explicit tie-break on rank = stable on every browser
      var x = pr.get(a.key), y = pr.get(b.key), nx = isNaN(x), ny = isNaN(y), c;
      if (nx || ny) c = nx === ny ? 0 : nx ? 1 : -1; else c = x < y ? -1 : x > y ? 1 : 0;
      return c || pos.get(a.key) - pos.get(b.key);
    });
  }
  function whyPick(p, index, o) {
    var n = p.unlocks.length, b = index === 0 ? 'Buying it completes ' + plural(n, 'recipe', 'recipes') + " you're one ingredient away from"
      : 'With the item' + (index > 1 ? 's' : '') + ' above, it completes ' + plural(n, 'more recipe', 'more recipes');
    if (o && demand(o) > n) b += '; ' + plural(demand(o), 'recipe', 'recipes') + ' in all use it';
    return b + '.';
  }
  function why(o) {
    var a = o.alone.length, c = o.closer.length, b = '';
    if (a > 0) b += 'Completes ' + plural(a, 'recipe', 'recipes') + ' on its own';
    if (c > 0) b += (a > 0 ? ' and brings ' : 'Brings ') + plural(c, 'more recipe', 'more recipes') + ' within one ingredient';
    return b + '.';
  }
  function costText(p, money, mine) {
    if (!p) return null;
    var pack = p.unit === '' ? (p.amount === 1 ? money + ' each' : money + ' for ' + PR.number(p.amount, '')) : money + ' for ' + PR.number(p.amount, p.unit) + ' ' + p.unit;
    return mine ? 'Your price: ' + pack : 'About ' + pack + ' (average price)';
  }
  BN.TOP = 3; BN.PAGE = 20; BN.SORT_RECIPES = 'recipes'; BN.SORT_CHEAPEST = 'cheapest';
  BN.ranked = ranked; BN.rankedFromResults = rankedFromResults; BN.sort = sortOptions; BN.whyPick = whyPick; BN.why = why; BN.costText = costText; BN.demand = demand;
})(typeof self !== 'undefined' ? self : globalThis);
