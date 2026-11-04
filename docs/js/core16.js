/* Pantry Recipes web - v1.6 core: cooking-time parser, nutrition and cost estimates, aisles, "buy these next",
 * leftovers, barcodes, share text. Port of the Java core (same results, checked by tests/test-core16.js). MIT License. */
(function (root) {
  'use strict';
  var PR = root.PR;
  var A = PR.Amounts;

  // ======================= Durations =======================
  var MAX_SECONDS = 48 * 3600;
  var NUM = 0, WORD = 1, DASH = 2, OTHER = 3;
  function vulgar(c) {
    switch (c) {
      case '\u00BD': return 0.5; case '\u00BC': return 0.25; case '\u00BE': return 0.75;
      case '\u2153': return 1 / 3; case '\u2154': return 2 / 3; case '\u215B': return 0.125;
      default: return -1;
    }
  }
  function digit(c) { return c >= '0' && c <= '9'; }
  function letter(c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
  function tok(type, start, end, num, word, fraction) { return { type: type, start: start, end: end, num: num, word: word, fraction: fraction }; }
  function tokens(s) {
    var out = [], n = s.length, i = 0;
    while (i < n) {
      var c = s.charAt(i);
      if (c === ' ' || c === '\t' || c === '\n' || c === '\r' || c === '\u00A0') { i++; continue; }
      if (digit(c)) {
        var j = i;
        while (j < n && digit(s.charAt(j))) j++;
        var v = parseFloat(s.substring(i, j)), frac = false, k;
        if (j + 1 < n && (s.charAt(j) === '.' || s.charAt(j) === ',') && digit(s.charAt(j + 1))) {
          k = j + 1;
          while (k < n && digit(s.charAt(k))) k++;
          v = parseFloat(s.substring(i, j) + '.' + s.substring(j + 1, k));
          j = k;
        } else if (j + 1 < n && s.charAt(j) === '/' && digit(s.charAt(j + 1))) {
          k = j + 1;
          while (k < n && digit(s.charAt(k))) k++;
          var d = parseFloat(s.substring(j + 1, k));
          if (d > 0) { v = v / d; frac = true; }
          j = k;
        }
        if (j < n && vulgar(s.charAt(j)) >= 0) { v += vulgar(s.charAt(j)); j++; }
        out.push(tok(NUM, i, j, v, null, frac));
        i = j;
      } else if (vulgar(c) >= 0) {
        out.push(tok(NUM, i, i + 1, vulgar(c), null, true)); i++;
      } else if (letter(c)) {
        var j2 = i, b = '';
        while (j2 < n && (letter(s.charAt(j2)) || (s.charAt(j2) === '-' && j2 + 1 < n && letter(s.charAt(j2 + 1)) && j2 > i))) {
          var ch = s.charAt(j2);
          b += (ch >= 'A' && ch <= 'Z') ? ch.toLowerCase() : ch;
          j2++;
        }
        out.push(tok(WORD, i, j2, 0, b, false));
        i = j2;
      } else if (c === '-' || c === '\u2013' || c === '\u2014') {
        out.push(tok(DASH, i, i + 1, 0, null, false)); i++;
      } else {
        out.push(tok(OTHER, i, i + 1, 0, null, false)); i++;
      }
    }
    return out;
  }
  var ONES = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten',
    'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen', 'twenty'];
  var TENS = ['', '', 'twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy', 'eighty', 'ninety'];
  function wordValue(w) {
    var k = ONES.indexOf(w);
    if (k >= 0) return k;
    for (k = 2; k < TENS.length; k++) {
      if (TENS[k] === w) return k * 10;
      if (w.indexOf(TENS[k] + '-') === 0) {
        var u = w.substring(TENS[k].length + 1);
        for (var m = 1; m <= 9; m++) if (ONES[m] === u) return k * 10 + m;
      }
    }
    return -1;
  }
  function unitSeconds(w) {
    if (w == null) return 0;
    if (w === 'hours' || w === 'hour' || w === 'hrs' || w === 'hr' || w === 'h') return 3600;
    if (w === 'minutes' || w === 'minute' || w === 'mins' || w === 'min') return 60;
    if (w === 'seconds' || w === 'second' || w === 'secs' || w === 'sec') return 1;
    return 0;
  }
  function isWord(t, i, w) { return i >= 0 && i < t.length && t[i].type === WORD && t[i].word === w; }
  function amount(t, i) {
    if (i >= t.length) return null;
    var a = t[i], j, v;
    if (a.type === NUM) {
      v = a.num; j = i + 1;
      if (!a.fraction && j < t.length && t[j].type === NUM && t[j].fraction && t[j].num < 1) { v += t[j].num; j++; }
      if (isWord(t, j, 'and') && isWord(t, j + 1, 'a') && isWord(t, j + 2, 'half')) { v += 0.5; j += 3; }
      return [v, j];
    }
    if (a.type !== WORD) return null;
    if (a.word === 'half') {
      j = i + 1;
      if (isWord(t, j, 'an') || isWord(t, j, 'a')) j++;
      return [0.5, j];
    }
    if (a.word === 'a' || a.word === 'an') {
      if (isWord(t, i + 1, 'couple') && isWord(t, i + 2, 'of')) return [2, i + 3];
      if (isWord(t, i + 1, 'couple')) return [2, i + 2];
      if (isWord(t, i + 1, 'half')) return [0.5, i + 2];
      return [1, i + 1];
    }
    var w = wordValue(a.word);
    if (w < 0) return null;
    j = i + 1;
    if (w >= 20 && w % 10 === 0 && j < t.length && t[j].type === WORD) {
      var u = wordValue(t[j].word);
      if (u >= 1 && u <= 9) { w += u; j++; }
    }
    v = w;
    if (isWord(t, j, 'and') && isWord(t, j + 1, 'a') && isWord(t, j + 2, 'half')) { v += 0.5; j += 3; }
    return [v, j];
  }
  function duration(t, i) {
    var a = amount(t, i);
    if (a == null) return null;
    var j = a[1];
    if (j >= t.length || t[j].type !== WORD) return null;
    var u = unitSeconds(t[j].word);
    if (u === 0) return null;
    var total = a[0] * u, first = u;
    j++;
    if (isWord(t, j, 'and') && isWord(t, j + 1, 'a') && isWord(t, j + 2, 'half')) { total += 0.5 * u; j += 3; }
    while (true) {
      var k = j;
      if (isWord(t, k, 'and')) k++;
      if (k >= t.length || t[k].type !== NUM) break;
      var b = amount(t, k);
      if (b == null) break;
      var m = b[1];
      if (m >= t.length || t[m].type !== WORD) break;
      var u2 = unitSeconds(t[m].word);
      if (u2 === 0 || u2 >= u) break;
      total += b[0] * u2;
      u = u2;
      j = m + 1;
    }
    return [total, j, first];
  }
  function rangeSep(t, i) { return i < t.length && (t[i].type === DASH || isWord(t, i, 'to') || isWord(t, i, 'or')); }
  function jround(v) { return Math.floor(v + 0.5); }
  function durMatch(start, end, seconds, maxSeconds, text) {
    return { start: start, end: end, seconds: seconds, maxSeconds: maxSeconds, text: text,
      isRange: function () { return this.maxSeconds > this.seconds; },
      label: function () { return this.maxSeconds > this.seconds ? rangeLabel(this.seconds, this.maxSeconds) : fmt(this.seconds); } };
  }
  function find(text) {
    var out = [];
    if (text == null || text === '') return out;
    var t = tokens(text), i = 0;
    while (i < t.length) {
      var lo = -1, hi = -1, startTok = i, endTok = -1;
      var between = isWord(t, i, 'between');
      var s0 = between ? i + 1 : i;
      var d1 = duration(t, s0), k, d2;
      if (d1 != null) {
        lo = jround(d1[0]); hi = lo; endTok = d1[1];
        k = endTok;
        if (rangeSep(t, k) || (between && isWord(t, k, 'and'))) {
          d2 = duration(t, k + 1);
          if (d2 != null && d2[0] >= d1[0]) { hi = jround(d2[0]); endTok = d2[1]; }
        }
      } else {
        var a = amount(t, s0);
        if (a != null && t[s0].type === NUM) {
          k = a[1];
          if (rangeSep(t, k) || (between && isWord(t, k, 'and'))) {
            d2 = duration(t, k + 1);
            if (d2 != null) {
              var low = a[0] * d2[2];
              if (low > 0 && low <= d2[0]) { lo = jround(low); hi = jround(d2[0]); endTok = d2[1]; }
            }
          }
        }
      }
      if (lo > 0 && lo <= MAX_SECONDS && endTok > 0) {
        if (!between) startTok = s0;
        var st = t[startTok].start, en = t[endTok - 1].end;
        out.push(durMatch(st, en, lo, Math.min(hi, MAX_SECONDS), text.substring(st, en)));
        i = endTok;
      } else i++;
    }
    return out;
  }
  function fmt(seconds) {
    var h = Math.floor(seconds / 3600), m = Math.floor((seconds % 3600) / 60), s = seconds % 60, b = '';
    if (h > 0) b += h + ' h';
    if (m > 0) b += (b ? ' ' : '') + m + ' min';
    if (s > 0) b += (b ? ' ' : '') + s + ' sec';
    return b === '' ? '0 sec' : b;
  }
  function rangeLabel(lo, hi) {
    if (lo % 60 === 0 && hi % 60 === 0 && hi < 3600) return (lo / 60) + '\u2013' + (hi / 60) + ' min';
    if (lo % 3600 === 0 && hi % 3600 === 0) return (lo / 3600) + '\u2013' + (hi / 3600) + ' h';
    return fmt(lo) + ' \u2013 ' + fmt(hi);
  }
  function clock(ms) {
    var sec = Math.max(0, Math.trunc((ms + 999) / 1000));
    var h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = sec % 60;
    var ss = (s < 10 ? '0' : '') + s;
    return h > 0 ? h + ':' + (m < 10 ? '0' : '') + m + ':' + ss : m + ':' + ss;
  }
  PR.Durations = { MAX_SECONDS: MAX_SECONDS, find: find, format: fmt, rangeLabel: rangeLabel, clock: clock, wordValue: wordValue };

  // ======================= Nutrition =======================
  var MIN_COVERAGE = 70, FRY_GRAMS = 200, FRY_ABSORBED = 0.1;
  /** Double.parseDouble for plain decimal numbers (NaN where Java would throw). */
  function pd(t) { return /^\s*[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?\s*$/.test(t) ? Number(t) : NaN; }
  function parseFoods(tsv) {
    var out = new Map();
    if (tsv == null) return out;
    tsv.split('\n').forEach(function (line) {
      if (line === '' || line.charAt(0) === '#') return;
      var f = line.split('\t');
      if (f.length < 9) return;
      var nums = [1, 2, 3, 4, 5, 6].map(function (k) { return pd(f[k]); });
      if (nums.some(isNaN)) return;
      var u = new Map(), bad = false;
      if (f[7] !== '') f[7].split(';').forEach(function (kv) {
        var e = kv.indexOf('=');
        if (e > 0) { var g = pd(kv.substring(e + 1)); if (isNaN(g)) bad = true; else u.set(kv.substring(0, e), g); }
      });
      if (bad) return;
      out.set(f[0], { key: f[0], kcal: nums[0], protein: nums[1], carbs: nums[2], fat: nums[3], gPerMl: nums[4], gEach: nums[5], units: u, fdc: f[8] });
    });
    return out;
  }
  var GENERIC = new Map([['handful', 25], ['large handful', 40], ['cm', 5], ['can', 400], ['tin', 400], ['jar', 300], ['scoop', 66], ['pack', 85],
    ['packet', 85], ['ring', 45], ['bunch', 50], ['head', 500], ['sprig', 1], ['stalk', 40], ['sheet', 25], ['slice', 25], ['clove', 3], ['leaf', 1], ['stick', 113]]);
  var LOOSE_ML = new Map([['pinch', 0.31], ['dash', 0.62], ['shot', 30], ['splash', 5], ['drizzle', 5]]);
  var NEGLIGIBLE = new Set(['salt', 'black pepper', 'white pepper', 'water', 'ice', 'sparkling water', 'cooking spray']);
  function lookup(foods, key) {
    if (!foods || key == null || key === '') return null;
    var f = foods.get(key);
    if (f === undefined) f = foods.get(PR.family(key));
    return f === undefined ? null : f;
  }
  function grams(f, amt, unit) {
    if (isNaN(amt) || amt < 0) return NaN;
    var c = A.canon(unit), dim = A.dimension(c);
    if (dim === 'mass') return A.convert(amt, c, 'g');
    if (f == null) return NaN;
    if (dim === 'volume') { var ml = A.convert(amt, c, 'ml'); return f.gPerMl > 0 ? ml * f.gPerMl : NaN; }
    if (dim === 'count') return f.gEach > 0 ? amt * f.gEach : NaN;
    var g = f.units.get(c);
    if (g !== undefined) return amt * g;
    var lm = LOOSE_ML.get(c);
    if (lm !== undefined) return amt * lm * (f.gPerMl > 0 ? f.gPerMl : 1.0);
    g = GENERIC.get(c);
    return g !== undefined ? amt * g : NaN;
  }
  function r1(v) { return Math.round(v * 10) / 10; }
  function r2(v) { return Math.round(v * 100) / 100; }
  function pct(a, b) { return b === 0 ? 0 : jround(100 * a / b); }
  function NutEstimate() { this.kcal = 0; this.protein = 0; this.carbs = 0; this.fat = 0; this.covered = 0; this.total = 0; }
  NutEstimate.prototype.percent = function () { return pct(this.covered, this.total); };
  NutEstimate.prototype.show = function () { return this.total > 0 && this.covered * 100 >= MIN_COVERAGE * this.total; };
  NutEstimate.prototype.toMap = function () {
    var m = new Map();
    m.set('kcal', r1(this.kcal)); m.set('protein', r1(this.protein)); m.set('carbs', r1(this.carbs)); m.set('fat', r1(this.fat));
    m.set('covered', this.covered); m.set('total', this.total); m.set('percent', this.percent()); m.set('show', this.show());
    return m;
  };
  function estimate(r, foods) {
    var e = new NutEstimate(), kc = 0, pr = 0, cb = 0, ft = 0;
    r.ingredients.forEach(function (i) {
      if (i.optional || i.key === '') return;
      e.total++;
      if (NEGLIGIBLE.has(i.key) && (isNaN(i.amount) || lookup(foods, i.key) == null)) { e.covered++; return; }
      var f = lookup(foods, i.key);
      if (f == null) return;
      var g = grams(f, i.amount, i.unit);
      if (isNaN(g)) return;
      if (g > FRY_GRAMS && /oil$/.test(i.key)) g *= FRY_ABSORBED;
      e.covered++;
      kc += f.kcal * g / 100; pr += f.protein * g / 100; cb += f.carbs * g / 100; ft += f.fat * g / 100;
    });
    var s = Math.max(1, r.servings);
    e.kcal = kc / s; e.protein = pr / s; e.carbs = cb / s; e.fat = ft / s;
    return e;
  }
  function nutSummary(e) {
    return '\u2248 ' + jround(e.kcal) + ' kcal \u00b7 ' + jround(e.protein) + ' g protein \u00b7 ' + jround(e.carbs) + ' g carbs \u00b7 ' + jround(e.fat) + ' g fat per serving';
  }
  PR.Nutrition = { MIN_COVERAGE: MIN_COVERAGE, NEGLIGIBLE: NEGLIGIBLE, parse: parseFoods, lookup: lookup, grams: grams, estimate: estimate, summary: nutSummary,
    NOTE: 'Estimate from USDA FoodData Central (SR Legacy, public domain) averages, matched by ingredient name with typical weights for items like \u201c1 onion\u201d. Not for medical or dietary decisions.' };

  // ======================= Cost =======================
  var UNITS = ['', 'g', 'kg', 'oz', 'lb', 'ml', 'l'], UNIT_NAMES = ['each', 'g', 'kg', 'oz', 'lb', 'ml', 'l'];
  function price(p, amt, unit) { return { price: p, amount: amt, unit: unit == null ? '' : unit }; }
  function make(p, amt, unit) {
    if (typeof p !== 'number' || typeof amt !== 'number' || isNaN(p) || isNaN(amt) || p < 0 || p > 100000 || amt <= 0 || amt > 100000 || UNITS.indexOf(unit) < 0) return null;
    return price(p, amt, unit);
  }
  function parseDefaults(tsv) {
    var out = new Map();
    if (tsv == null) return out;
    tsv.split('\n').forEach(function (line) {
      if (line === '' || line.charAt(0) === '#') return;
      var f = line.split('\t');
      if (f.length < 4) return;
      var p = make(pd(f[1]), pd(f[2]), PR.jtrim(f[3]));
      if (p) out.set(f[0], p);
    });
    return out;
  }
  function findPrice(key, user, defaults) {
    if (key == null || key === '') return null;
    var fam = PR.family(key), p;
    if (user) {
      p = user.get(key);
      if (p === undefined) p = user.get(fam);
      if (p !== undefined) return p;
    }
    if (defaults) {
      p = defaults.get(key);
      if (p === undefined) p = defaults.get(fam);
      return p === undefined ? null : p;
    }
    return null;
  }
  function costOf(p, food, amt, unit) {
    if (p == null || isNaN(amt)) return NaN;
    var q = A.convert(amt, unit, p.unit);
    if (!isNaN(q)) return q / p.amount * p.price;
    var g = grams(food, amt, unit), gp = grams(food, p.amount, p.unit);
    if (isNaN(g) || isNaN(gp) || gp <= 0) return NaN;
    return g / gp * p.price;
  }
  function CostEstimate() { this.total = 0; this.perServing = 0; this.covered = 0; this.count = 0; this.fromDefaults = 0; }
  CostEstimate.prototype.percent = function () { return pct(this.covered, this.count); };
  CostEstimate.prototype.show = function () { return this.count > 0 && this.covered * 100 >= MIN_COVERAGE * this.count; };
  CostEstimate.prototype.toMap = function () {
    var m = new Map();
    m.set('total', r2(this.total)); m.set('perServing', r2(this.perServing)); m.set('covered', this.covered); m.set('count', this.count);
    m.set('fromDefaults', this.fromDefaults); m.set('percent', this.percent()); m.set('show', this.show());
    return m;
  };
  function isUser(p, user) { if (!user) return false; var hit = false; user.forEach(function (v) { if (v === p) hit = true; }); return hit; }
  function costEstimate(r, servings, user, defaults, foods) {
    var e = new CostEstimate();
    var scale = Math.max(1, servings) / Math.max(1, r.servings);
    r.ingredients.forEach(function (i) {
      if (i.optional || i.key === '') return;
      e.count++;
      var p = findPrice(i.key, user, defaults);
      if (isNaN(i.amount)) { if (NEGLIGIBLE.has(i.key)) e.covered++; return; }
      if (p == null) return;
      var c = costOf(p, lookup(foods, i.key), i.amount * scale, i.unit);
      if (isNaN(c)) return;
      e.covered++;
      if (!isUser(p, user)) e.fromDefaults++;
      e.total += c;
    });
    e.perServing = e.total / Math.max(1, servings);
    return e;
  }
  function costItem(name, amt, unit, user, defaults, foods) {
    var key = PR.normalize(name == null ? '' : name);
    return costOf(findPrice(key, user, defaults), lookup(foods, key), amt, unit);
  }
  function costShopping(rows, user, defaults, foods) {
    var t = 0, n = 0;
    rows.forEach(function (r) { var c = costItem(r[0], r[1], r[2], user, defaults, foods); if (!isNaN(c)) { t += c; n++; } });
    return [t, n, rows.length];
  }
  function money(cur, v) {
    var c = cur == null || PR.jtrim(cur) === '' ? '$' : PR.jtrim(cur);
    var cents = jround(v * 100), rest = cents % 100;
    return c + Math.trunc(cents / 100) + '.' + (rest < 10 ? '0' : '') + rest;
  }
  function describe(cur, p) {
    if (p.unit === '') return p.amount === 1 ? money(cur, p.price) + ' each' : money(cur, p.price) + ' for ' + PR.number(p.amount, '');
    return money(cur, p.price) + ' for ' + PR.number(p.amount, p.unit) + ' ' + p.unit;
  }
  PR.Cost = { MIN_COVERAGE: MIN_COVERAGE, DEFAULT_CURRENCY: '$', UNITS: UNITS, UNIT_NAMES: UNIT_NAMES, MAX_PRICES: 1000, make: make,
    parseDefaults: parseDefaults, find: findPrice, costOf: costOf, estimate: costEstimate, item: costItem, shopping: costShopping, money: money, describe: describe,
    NOTE: 'Estimate from the prices you set (and, if switched on, rough built-in default prices). Real prices vary by shop, brand and season.' };

  // ======================= Aisles =======================
  var OTHER_AISLE = 'Other';
  var ORDER = ['Produce', 'Meat & poultry', 'Fish & seafood', 'Deli', 'Dairy & eggs', 'Chilled & plant-based', 'Bakery', 'Pasta, rice & grains',
    'Cans, jars & beans', 'Baking', 'Spices & dried herbs', 'Oils, sauces & condiments', 'World foods', 'Nuts & snacks', 'Frozen', 'Drinks', OTHER_AISLE];
  var FOR_CATEGORY = ['Produce', 'Produce', 'Produce', 'Spices & dried herbs', 'Meat & poultry', 'Meat & poultry', 'Fish & seafood', 'Deli', 'Dairy & eggs',
    'Dairy & eggs', 'Chilled & plant-based', 'Pasta, rice & grains', 'Pasta, rice & grains', 'Bakery', 'Cans, jars & beans', 'Nuts & snacks', 'Baking', 'Baking',
    'Oils, sauces & condiments', 'Oils, sauces & condiments', 'Cans, jars & beans', 'Frozen', 'Nuts & snacks', 'Drinks', 'World foods', 'World foods',
    'World foods', 'World foods', 'World foods'];
  var byKey = null;
  function aisleMap() {
    if (byKey) return byKey;
    var m = new Map();
    PR.Catalog.ITEMS.forEach(function (items, c) {
      var aisle = FOR_CATEGORY[c];
      items.forEach(function (s) {
        if (s.charAt(0) === '#') { aisle = s === '#Dried herbs' ? 'Spices & dried herbs' : FOR_CATEGORY[c]; return; }
        var k = PR.normalize(s);
        if (k !== '' && !m.has(k)) m.set(k, aisle);
      });
    });
    byKey = m;
    return m;
  }
  function aisleOf(name) {
    var k = PR.normalize(name == null ? '' : name);
    if (k === '') return OTHER_AISLE;
    var a = aisleMap().get(k);
    if (a === undefined) a = aisleMap().get(PR.family(k));
    return a === undefined ? OTHER_AISLE : a;
  }
  function group(names) {
    var tmp = new Map();
    names.forEach(function (n, i) { var a = aisleOf(n); if (!tmp.has(a)) tmp.set(a, []); tmp.get(a).push(i); });
    var out = new Map();
    ORDER.forEach(function (a) { if (tmp.has(a)) out.set(a, tmp.get(a)); });
    return out;
  }
  PR.Aisles = { OTHER: OTHER_AISLE, ORDER: ORDER, FOR_CATEGORY: FOR_CATEGORY, of: aisleOf, group: group };

  // ======================= BuyNext =======================
  function suggest(ids, missing, names, max) {
    var pool = [], demand = new Map();
    ids.forEach(function (id, i) {
      var u = Array.from(new Set(missing[i]));
      if (u.length < 1 || u.length > 2) return;
      pool.push(i);
      u.forEach(function (k) { demand.set(k, (demand.get(k) || 0) + 1); });
    });
    // v2.1 (HD8): bucket the open recipes by their last unchosen missing item once per round (same picks as before)
    var out = [], chosen = new Set(), done = new Set();
    for (var round = 0; round < max; round++) {
      var best = null, bestNew = null, nwBy = new Map();
      pool.forEach(function (i) {
        if (done.has(i)) return;
        var last = null, one = true, m = missing[i];
        for (var q = 0; q < m.length; q++) { if (chosen.has(m[q]) || m[q] === last) continue; if (last == null) last = m[q]; else { one = false; break; } }
        if (!one || last == null) return;
        var l = nwBy.get(last); if (!l) { l = []; nwBy.set(last, l); } l.push(i);
      });
      demand.forEach(function (dv, k) {
        if (chosen.has(k)) return;
        var nw = nwBy.get(k);
        if (!nw || !nw.length) return;
        if (best == null || nw.length > bestNew.length || (nw.length === bestNew.length && (dv > demand.get(best) || (dv === demand.get(best) && k < best)))) {
          best = k; bestNew = nw;
        }
      });
      if (best == null) break;
      chosen.add(best);
      var nm = names ? names.get(best) : null;
      var p = { key: best, name: nm == null || nm === '' ? best : nm, unlocks: [] };
      bestNew.forEach(function (i) { done.add(i); p.unlocks.push(ids[i]); });
      out.push(p);
    }
    return out;
  }
  function shortName(n) {
    var s = n == null ? '' : n, c = s.indexOf(',');
    if (c > 0) s = s.substring(0, c);
    c = s.indexOf('(');
    if (c > 0) s = s.substring(0, c);
    s = PR.jtrim(s);
    return s === '' ? s : s.charAt(0).toUpperCase() + s.substring(1);
  }
  function fromResults(results, max) {
    var ids = [], miss = [], names = new Map();
    results.forEach(function (r) {
      var m = [];
      r.missing.forEach(function (i) { m.push(i.key); if (!names.has(i.key)) names.set(i.key, shortName(i.name)); });
      ids.push(r.recipe.id); miss.push(m);
    });
    return suggest(ids, miss, names, max);
  }
  PR.BuyNext = { MAX: 3, suggest: suggest, fromResults: fromResults, shortName: shortName };

  // ======================= Leftovers =======================
  var L = { DEFAULT_DAYS: 3, MIN_DAYS: 1, MAX_DAYS: 14, MAX_PORTIONS: 99, MAX_ENTRIES: 200, MAX_LOG: 1000 };
  L.clampDays = function (d) { return Math.max(L.MIN_DAYS, Math.min(L.MAX_DAYS, d)); };
  L.nextId = function (list) {
    var n = 1;
    list.forEach(function (e) { if (e.id.charAt(0) === 'l' && e.id.length > 1 && /^[+-]?\d+$/.test(e.id.substring(1))) { var v = parseInt(e.id.substring(1), 10); if (v < 2147483647) n = Math.max(n, v + 1); } });
    return 'l' + n;
  };
  L.add = function (list, recipeId, title, portions, today, days) {
    if (portions <= 0 || !PR.Dates.valid(today)) return null;
    var t = title == null ? '' : title;
    var e = { id: L.nextId(list), recipe: recipeId == null ? '' : recipeId, title: t.length > 120 ? t.substring(0, 120) : t,
      portions: Math.min(L.MAX_PORTIONS, portions), cooked: today, useBy: PR.Dates.addDays(today, L.clampDays(days)) };
    list.unshift(e);
    while (list.length > L.MAX_ENTRIES) list.pop();
    return e;
  };
  L.adjust = function (list, id, delta) {
    for (var k = 0; k < list.length; k++) {
      var e = list[k];
      if (e.id !== id) continue;
      e.portions = Math.max(0, Math.min(L.MAX_PORTIONS, e.portions + delta));
      if (e.portions === 0) list.splice(k, 1);
      return e.portions;
    }
    return 0;
  };
  L.status = function (e, today) { return PR.UseBy.status(PR.UseBy.daysLeft(e.useBy, today)); };
  function cmp(a, b) { return a < b ? -1 : a > b ? 1 : 0; }
  L.sorted = function (list) {
    return list.slice().sort(function (a, b) { return cmp(a.useBy, b.useBy) || cmp(b.cooked, a.cooked) || cmp(a.id, b.id); });
  };
  L.log = function (log, recipeId, servings, today) {
    var c = { date: today, recipe: recipeId, servings: Math.max(1, Math.min(72, servings)) };
    log.unshift(c);
    while (log.length > L.MAX_LOG) log.pop();
    return c;
  };
  L.history = function (log, recipeId) {
    var n = 0, last = '';
    log.forEach(function (c) { if (c.recipe === recipeId) { n++; if (c.date > last) last = c.date; } });
    return [n, last];
  };
  PR.Leftovers = L;

  // ======================= Amounts.deduct =======================
  A.deduct = function (haveText, key, need, needUnit) {
    if (haveText == null || isNaN(need) || need <= 0) return null;
    var p = A.parse(haveText);
    if (isNaN(p.amount)) return null;
    var hu = p.unit, cu = A.canon(hu);
    if (cu !== '' && A.dimension(cu).indexOf('unit:') === 0 && key != null && (' ' + key + ' ').indexOf(' ' + cu + ' ') >= 0) hu = '';
    var used = A.convert(need, needUnit, hu);
    if (isNaN(used)) return null;
    var left = Math.max(0, p.amount - used);
    left = Math.round(left * 100) / 100;
    var unitText = hu === '' ? '' : PR.jtrim(p.unit);
    if (left <= 1 && unitText.length > 2 && /s$/.test(unitText) && cu === A.canon(unitText.substring(0, unitText.length - 1))) unitText = unitText.substring(0, unitText.length - 1);
    var num = String(left);
    return unitText === '' ? num : num + ' ' + unitText;
  };

  // ======================= Barcode =======================
  var API = 'https://world.openfoodfacts.org/api/v2/product/';
  var FIELDS = 'product_name,product_name_en,generic_name,generic_name_en,brands,categories_tags,quantity';
  function clean(raw) {
    if (raw == null) return '';
    var b = '';
    for (var i = 0; i < raw.length; i++) {
      var c = raw.charAt(i);
      if (c >= '0' && c <= '9') b += c; else if (c !== ' ' && c !== '-') return '';
    }
    return b;
  }
  function upcE(c) {
    var d = c.substring(1, 7), last = d.charAt(5), m;
    if (last === '0' || last === '1' || last === '2') m = d.substring(0, 2) + last + '0000' + d.substring(2, 5);
    else if (last === '3') m = d.substring(0, 3) + '00000' + d.substring(3, 5);
    else if (last === '4') m = d.substring(0, 4) + '00000' + d.charAt(4);
    else m = d.substring(0, 5) + '0000' + last;
    var a = c.charAt(0) + m + c.charAt(7), sum = 0;
    for (var i = 0; i < 11; i++) { var x = a.charCodeAt(10 - i) - 48; sum += i % 2 === 0 ? x * 3 : x; }
    return (10 - sum % 10) % 10 === a.charCodeAt(11) - 48;
  }
  function validCode(code) {
    var c = clean(code), n = c.length;
    if (n !== 8 && n !== 12 && n !== 13 && n !== 14) return false;
    var sum = 0;
    for (var i = 0; i < n - 1; i++) { var d = c.charCodeAt(n - 2 - i) - 48; sum += i % 2 === 0 ? d * 3 : d; }
    if ((10 - sum % 10) % 10 === c.charCodeAt(n - 1) - 48) return true;
    return n === 8 && (c.charAt(0) === '0' || c.charAt(0) === '1') && upcE(c);
  }
  function bstr(o, max) {
    if (typeof o !== 'string') return '';
    var v = PR.jtrim(o.replace(/\n/g, ' '));
    return v.length > max ? v.substring(0, max) : v;
  }
  function product() {
    return { code: '', name: '', generic: '', brands: '', quantity: '', categories: [],
      title: function () {
        var t = this.name === '' ? this.generic : this.name;
        var b0 = PR.jtrim(this.brands.split(',')[0] || '');
        if (this.brands !== '' && t.toLowerCase().indexOf(b0.toLowerCase()) < 0) t = b0 + ' ' + t;
        return PR.jtrim(PR.jtrim(t) + (this.quantity === '' ? '' : ' (' + this.quantity + ')'));
      } };
  }
  function parseOff(json) {
    try {
      var m = PR.Json.parse(json);
      if (!(m instanceof Map)) return null;
      var st = m.get('status');
      if (typeof st !== 'number' || PR.Json.jInt(st) !== 1 || !(m.get('product') instanceof Map)) return null;
      var p = m.get('product'), out = product();
      out.code = clean(bstr(m.get('code'), 20));
      out.name = bstr(p.get('product_name'), 120);
      if (out.name === '') out.name = bstr(p.get('product_name_en'), 120);
      out.generic = bstr(p.get('generic_name'), 120);
      if (out.generic === '') out.generic = bstr(p.get('generic_name_en'), 120);
      out.brands = bstr(p.get('brands'), 80);
      out.quantity = quantity(bstr(p.get('quantity'), 60));
      var c = p.get('categories_tags');
      if (Array.isArray(c)) {
        for (var i = 0; i < c.length; i++) {
          var t = bstr(c[i], 80);
          if (t.indexOf('en:') === 0) out.categories.push(t.substring(3).replace(/-/g, ' '));
          if (out.categories.length >= 30) break;
        }
      }
      if (out.name === '' && out.generic === '' && out.categories.length === 0) return null;
      return out;
    } catch (e) { return null; }
  }
  // v1.8 (same as Barcode.java): package sizes made fit for the pantry
  var NUM_COMMA = /(\d),(\d)/g, E_MARK = /(^|\s)e(\.|\s|$)/g,
    NET = /^(net\s*wt\.?|net\s*weight|net\s*contents|poids\s*net|netto|net)\s*:?\s*/,
    MULTI = /^(\d+)\s*[x\u00d7*]\s*(\d+(?:\.\d+)?)\s*([a-z. ]+?)\.?$/, ONE = /^(\d+(?:\.\d+)?)\s*([a-z. ]+?)\.?$/;
  function qunit(u) {
    var x = u.split('.').join('').split(' ').join('');
    if (['g', 'gr', 'grams', 'gram', 'grs'].indexOf(x) >= 0) return ['g', 1];
    if (['kg', 'kgs', 'kilo'].indexOf(x) >= 0) return ['kg', 1];
    if (x === 'mg') return ['g', 0.001];
    if (x === 'ml' || x === 'mls') return ['ml', 1];
    if (x === 'cl') return ['ml', 10];
    if (x === 'dl') return ['ml', 100];
    if (['l', 'lt', 'ltr', 'litre', 'liter', 'litres', 'liters'].indexOf(x) >= 0) return ['l', 1];
    if (['oz', 'ounce', 'ounces'].indexOf(x) >= 0) return ['oz', 1];
    if (x === 'floz') return ['fl oz', 1];
    if (['lb', 'lbs', 'pound', 'pounds'].indexOf(x) >= 0) return ['lb', 1];
    return null;
  }
  function qdec2(v) {
    var c = Math.floor(v * 100 + 0.5); if (c < 0) c = 0;
    var w = Math.floor(c / 100), f = c % 100;
    if (f === 0) return String(w);
    if (f % 10 === 0) return w + '.' + (f / 10);
    return w + '.' + (f < 10 ? '0' : '') + f;
  }
  function qnum(v) { return Math.abs(v - Math.round(v)) < 1e-9 ? String(Math.round(v)) : qdec2(v); }
  function qamount(t) {
    var m = MULTI.exec(t), u;
    if (m) { u = qunit(PR.jtrim(m[3])); if (u) return qnum(parseInt(m[1], 10) * parseFloat(m[2]) * u[1]) + ' ' + u[0]; }
    m = ONE.exec(t);
    if (m) { u = qunit(PR.jtrim(m[2])); if (u) return qnum(parseFloat(m[1]) * u[1]) + ' ' + u[0]; }
    return null;
  }
  function quantity(raw) {
    if (raw == null) return '';
    var t = PR.jtrim(raw.split('\u212e').join(' ').split('\u00a0').join(' ').toLowerCase());
    t = t.replace(NUM_COMMA, '$1.$2').replace(NET, '').replace(E_MARK, '$1 ');
    t = PR.jtrim(t.replace(/\s+/g, ' '));
    if (t === '') return '';
    var par = t.indexOf('('), a;
    if (par > 0) {
      a = qamount(PR.jtrim(t.substring(0, par)));
      if (a != null) return a;
      var close = t.indexOf(')', par);
      a = qamount(PR.jtrim(t.substring(par + 1, close > par ? close : t.length)));
      if (a != null) return a;
    }
    a = qamount(t);
    if (a != null) return a;
    var keep = PR.jtrim(raw.split('\u212e').join(' ').replace(/\s+/g, ' '));
    if (keep.length >= 2 && keep.substring(keep.length - 2) === ' e') keep = PR.jtrim(keep.substring(0, keep.length - 2));
    return keep.length > 30 ? PR.jtrim(keep.substring(0, 30)) : keep;
  }
  function words(s) {
    var w = [], b = '', t = s.toLowerCase() + ' ';
    for (var i = 0; i < t.length; i++) {
      var c = t.charAt(i);
      if ((c >= 'a' && c <= 'z') || c === '\u00e9' || c === '\u00e8') b += c;
      else if (b.length > 0) { w.push(b); b = ''; }
    }
    return w;
  }
  function containsWords(hay, needle) {
    if (!needle.length) return false;
    for (var i = 0; i + needle.length <= hay.length; i++) {
      var ok = true;
      for (var j = 0; j < needle.length; j++) {
        var h = hay[i + j], n = needle[j];
        if (!(h === n || h === n + 's' || h === n + 'es' || (n.charAt(n.length - 1) === 's' && h === n.substring(0, n.length - 1)))) { ok = false; break; }
      }
      if (ok) return true;
    }
    return false;
  }
  function matchItem(p) { var m = matchList(p, 1); return m.length ? m[0] : ''; }
  function matchList(p, n) {
    var out = [];
    if (p == null || !(n > 0)) return out;
    var name = words(p.name), gen = words(p.generic), brand = words(p.brands), cats = [];
    if (brand.length && name.length > brand.length) {
      var nb = name.filter(function (w) { return brand.indexOf(w) < 0 || gen.indexOf(w) >= 0; });
      if (nb.length) name = nb;
    }
    var nameKey = PR.normalize(name.join(' '));
    for (var k = p.categories.length - 1; k >= 0; k--) cats.push(words(p.categories[k]));
    var cand = [];
    PR.Catalog.allItems().forEach(function (item) {
      var iw = words(item);
      if (!iw.length) return;
      var score = 0;
      if (nameKey !== '' && nameKey === PR.normalize(item)) score = 500;
      else if (containsWords(name, iw)) score = 300;
      else if (containsWords(gen, iw)) score = 200;
      else for (var q = 0; q < cats.length && q < 10; q++) if (containsWords(cats[q], iw)) { score = 100 - q * 5; break; }
      if (score === 0) return;
      cand.push({ item: item, score: score + iw.length * 10, i: cand.length });
    });
    cand.sort(function (a, b) { return (b.score - a.score) || (b.item.length - a.item.length) || (a.i - b.i); });
    var seen = new Set();
    for (var c = 0; c < cand.length && out.length < n; c++) {
      var key = PR.normalize(cand[c].item);
      if (!seen.has(key)) { seen.add(key); out.push(cand[c].item); }
    }
    return out;
  }
  PR.Barcode = { API: API, FIELDS: FIELDS, clean: clean, valid: validCode, url: function (c) { return API + clean(c) + '.json?fields=' + FIELDS; },
    parse: parseOff, match: matchItem, matches: matchList, quantity: quantity,
    ATTRIBUTION: 'Product data from Open Food Facts (openfoodfacts.org), available under the Open Database License (ODbL).' };

  // ======================= ShareText =======================
  function shareShopping(rows, fmt) {
    var names = rows.map(function (r) { return r[0]; }), b = 'Shopping list\n';
    group(names).forEach(function (idx, aisle) {
      var sec = '';
      idx.forEach(function (i) {
        var r = rows[i];
        if (r[3] === true) return;
        var q = isNaN(r[1]) ? '' : fmt ? fmt(r[0], r[1], r[2]) : PR.formatQty(r[1], r[2], 1);
        sec += '- ' + r[0] + (q === '' ? '' : ' (' + q + ')') + '\n';
      });
      if (sec !== '') b += '\n' + aisle + '\n' + sec;
    });
    return PR.jtrim(b) + '\n';
  }
  function sharePlan(weekLabel, rows) {
    var b = 'Meal plan';
    if (weekLabel) b += ' \u2013 ' + weekLabel;
    b += '\n';
    var last = '';
    rows.forEach(function (r) {
      if (r[0] !== last) { b += '\n' + PR.Dates.label(r[0]) + '\n'; last = r[0]; }
      b += '- ' + r[1] + ' (' + r[2] + (r[2] === 1 ? ' serving)' : ' servings)') + '\n';
    });
    if (!rows.length) b += '\nNothing planned.\n';
    return PR.jtrim(b) + '\n';
  }
  PR.ShareText = { shopping: shareShopping, plan: sharePlan };
})(typeof window !== 'undefined' ? window : globalThis);
