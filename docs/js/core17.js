/* Pantry Recipes v1.7 web core: swap flags and ratios, cooking methods, measurement units and conversions,
 * and pressure/release timer kinds. A line-by-line port of the Java core (Substitutions, Methods, Units,
 * Durations), checked against tests/vectors17.json written by tools/V17Export.java. MIT License. */
(function (root) {
  'use strict';
  var PR = root.PR;
  var A = PR.Amounts;

  // ======================= Substitutions (v1.7 additions) =======================
  var S = PR.Substitutions;
  function computeFlags(o) {
    o.vegetarian = o.vegan = o.dairyFree = o.glutenFree = true;
    o.needs.forEach(function (g) {
      g.forEach(function (k) {
        var meat = PR.isMeat(k), dairy = PR.isDairy(k), other = PR.isOtherAnimal(k);
        if (meat) o.vegetarian = false;
        if (meat || dairy || other) o.vegan = false;
        if (dairy) o.dairyFree = false;
        if (PR.hasGluten(k)) o.glutenFree = false;
      });
    });
    if (o.needs.length === 0) o.vegetarian = o.vegan = o.dairyFree = o.glutenFree = false;
    o.allergens = PR.Allergens.IDS.filter(function (id) {
      return o.needs.some(function (g) { return g.some(function (k) { return PR.Allergens.keyHas(id, k); }); });
    });
  }
  function ratioNum(s) {
    if (!/^[0-9]+(\.[0-9]+)?(\/[0-9]+)?$/.test(s)) return null;
    var sl = s.indexOf('/');
    if (sl < 0) return Number(s);
    var d = Number(s.substring(sl + 1));
    if (d === 0) return null;
    return Number(s.substring(0, sl)) / d;
  }
  function ratioValue(r) {
    if (r == null) return null;
    r = PR.jtrim(r);
    if (r === 'to taste') return 0;
    var p = r.split(':');
    if (p.length !== 2) return null;
    var a = ratioNum(PR.jtrim(p[0])), b = ratioNum(PR.jtrim(p[1]));
    if (a == null || b == null || a <= 0 || b <= 0) return null;
    return a / b;
  }
  var PRETTY = { '1/2': '\u00bd', '1/3': '\u2153', '2/3': '\u2154', '1/4': '\u00bc', '3/4': '\u00be' };
  function pretty(f) { return PRETTY.hasOwnProperty(f) ? PRETTY[f] : f; }
  function ratioLabel(o) {
    if (ratioValue(o.ratio) == null) return '';
    if (o.ratio === 'to taste') return 'to taste';
    var p = o.ratio.split(':');
    return pretty(PR.jtrim(p[0])) + ' for ' + pretty(PR.jtrim(p[1]));
  }
  function dietLabels(o) {
    var out = [];
    if (o.vegan) out.push('Vegan'); else if (o.vegetarian) out.push('Vegetarian');
    if (o.dairyFree) out.push('Dairy-free');
    if (o.glutenFree) out.push('Gluten-free');
    return out;
  }
  function flagsLine(o) {
    var parts = [], rl = ratioLabel(o);
    if (rl !== '') parts.push('Ratio ' + rl);
    dietLabels(o).forEach(function (d) { parts.push(d); });
    if (o.allergens && o.allergens.length) parts.push('Contains: ' + o.allergens.map(function (id) { return PR.Allergens.label(id).toLowerCase(); }).join(', '));
    return parts.join(' \u00b7 ');
  }
  function swapDetail(o) {
    var f = flagsLine(o);
    if (!o.effect) return f;
    return f === '' ? o.effect : f + '\n' + o.effect;
  }
  var CLAIMS = ['vegan', 'vegetarian', 'dairy-free', 'gluten-free', 'nut-free', 'egg-free', 'sesame-free', 'peanut-free', 'soy-free'];
  function claims(o) {
    var out = [], t = o.text.toLowerCase(), ef = o.effect.toLowerCase(), first = PR.jtrim(ef.split(';')[0]);
    CLAIMS.forEach(function (c) {
      var neg = t.indexOf('not ' + c) >= 0 || ef.indexOf('not ' + c) >= 0;
      if (neg) out.push('!' + c);
      else if (t.indexOf('(' + c + ')') >= 0 || first === c) out.push(c);
    });
    if (t.indexOf('contains gluten') >= 0 || ef.indexOf('contains gluten') >= 0) out.push('!gluten-free');
    if (t.indexOf('contains dairy') >= 0 || ef.indexOf('contains dairy') >= 0) out.push('!dairy-free');
    return out;
  }
  function claimHolds(o, c) {
    var neg = c.charAt(0) === '!', k = neg ? c.substring(1) : c, v, al = o.allergens || [];
    function has(id) { return al.indexOf(id) >= 0; }
    if (k === 'vegan') v = o.vegan;
    else if (k === 'vegetarian') v = o.vegetarian;
    else if (k === 'dairy-free') v = o.dairyFree && !has('dairy');
    else if (k === 'gluten-free') v = o.glutenFree;
    else if (k === 'nut-free') v = !has('peanuts') && !has('tree-nuts');
    else if (k === 'egg-free') v = !has('eggs');
    else if (k === 'sesame-free') v = !has('sesame');
    else if (k === 'peanut-free') v = !has('peanuts');
    else if (k === 'soy-free') v = !has('soy');
    else return false;
    return neg ? !v : !!v;
  }
  S.computeFlags = computeFlags;
  S.ratioValue = ratioValue; S.ratioLabel = ratioLabel; S.dietLabels = dietLabels; S.flagsLine = flagsLine;
  S.swapDetail = swapDetail; S.claims = claims; S.claimHolds = claimHolds;

  // ======================= Durations: pressure and release kinds =======================
  var D = PR.Durations;
  var TIMER = 'timer', PRESSURE = 'pressure', RELEASE = 'release', BUILD_UP = 'build-up';
  function lowerAscii(s) { return s.replace(/[A-Z]/g, function (c) { return c.toLowerCase(); }); }
  function kindAt(text, start, end) {
    var from = Math.max(0, start - 60);
    for (var i = start - 1; i >= from; i--) {
      var c = text.charAt(i);
      if (c === '.' || c === ';' || c === ',' || c === '!' || c === '?') { from = i + 1; break; }
    }
    var before = lowerAscii(text.substring(from, start));
    var to = Math.min(text.length, end + 25);
    var after = lowerAscii(text.substring(end, to));
    function b(w) { return before.indexOf(w) >= 0; }
    function a(w) { return after.indexOf(w) === 0; }
    if (a(' to come up to pressure') || a(' to come to pressure') || a(' to reach pressure') || a(' to build pressure') || a(' to build up pressure') || a(' to pressurize') || a(' to pressurise')) return BUILD_UP;
    if (b('release naturally') || b('natural release') || b('naturally release') || b('natural pressure release') || a(' natural release') || a(' of natural release')) return RELEASE;
    if (b('pressure cook') || b('pressure-cook') || b('at high pressure') || b('at low pressure') || b('on high pressure') || b('on low pressure')
      || a(' at high pressure') || a(' at low pressure') || a(' on high pressure') || a(' on low pressure') || a(' under pressure') || a(' at pressure')) return PRESSURE;
    return TIMER;
  }
  var baseFind = D.find;
  D.find = function (text) {
    return baseFind(text).filter(function (m) { return kindAt(text, m.start, m.end) !== BUILD_UP; }).map(function (m) {
      m.kind = kindAt(text, m.start, m.end);
      m.isPressure = function () { return this.kind === PRESSURE; };
      m.isRelease = function () { return this.kind === RELEASE; };
      return m;
    });
  };
  D.TIMER = TIMER; D.PRESSURE = PRESSURE; D.RELEASE = RELEASE; D.kindAt = kindAt;
  D.BUILD_UP_NOTE = 'The pot first needs about 10\u201315 min to come up to pressure. '
    + 'That build-up is not counted here: start this timer when the float valve pops up and the countdown begins.';
  D.RELEASE_NOTE = 'Natural release: leave the valve sealed. After this timer, quick release any remaining '
    + 'pressure, keeping hands and face away from the steam.';
  D.timerNote = function (m) { return m.kind === PRESSURE ? D.BUILD_UP_NOTE : m.kind === RELEASE ? D.RELEASE_NOTE : ''; };

  // ======================= Methods =======================
  var IDS = ['instant-pot', 'slow-cooker', 'air-fryer', 'one-pot', 'oven', 'stovetop', 'no-cook'];
  var LABELS = { 'instant-pot': 'Instant Pot', 'slow-cooker': 'Slow cooker', 'air-fryer': 'Air fryer', 'one-pot': 'One-pot', oven: 'Oven', stovetop: 'Stovetop', 'no-cook': 'No-cook' };
  var PAN_WORDS = ['saucepan', 'frying pan', 'skillet', 'wok', 'griddle', 'stockpot', 'dutch oven', 'pot of', 'large pot', 'a pot', 'the pot over',
    'pan over', 'over medium', 'over high', 'over low', 'on the hob', 'on the stove', 'simmer', 'boil', ' pan', ' fry', 'fry ', 'saute', 'off the heat', 'toast '];
  function has(s, w) { return s.indexOf(w) >= 0; }
  function letter(c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
  function word(s, w) {
    var i = s.indexOf(w);
    while (i >= 0) {
      var l = i === 0 || !letter(s.charAt(i - 1)), e = i + w.length, r = e >= s.length || !letter(s.charAt(e));
      if (l && r) return true;
      i = s.indexOf(w, i + 1);
    }
    return false;
  }
  function rep(s, a, b) { return s.split(a).join(b); }
  var cache = typeof WeakMap !== 'undefined' ? new WeakMap() : null;
  function methodsOf(r) {
    if (cache && cache.has(r)) return cache.get(r);
    var title = r.title == null ? '' : r.title.toLowerCase(), all = title;
    r.steps.forEach(function (s) { all += ' \n ' + (s == null ? '' : s.toLowerCase()); });
    var steps = all.substring(title.length);
    var ip = has(all, 'instant pot') || has(all, 'pressure cook') || has(all, 'pressure-cook') || has(all, 'pressure cooker') || has(all, 'high pressure');
    var slow = has(all, 'slow cooker') || has(all, 'slow-cooker') || has(all, 'crockpot') || has(all, 'crock pot');
    var air = has(all, 'air fryer') || has(all, 'air fry') || has(all, 'air-fry');
    var ovenText = rep(rep(steps, 'dutch oven', 'pot'), 'microwave oven', 'microwave');
    var oven = word(ovenText, 'oven') || has(ovenText, 'broiler') || has(ovenText, 'under the grill') || has(title, 'baked') || has(title, 'roast')
      || has(title, 'sheet pan') || has(title, 'sheet-pan') || has(title, 'traybake');
    var stoveText = rep(steps, 'dutch oven', 'pot'), stove;
    if (ip || slow) stove = has(stoveText, 'saucepan') || has(stoveText, 'frying pan') || has(stoveText, 'skillet') || has(stoveText, 'wok');
    else stove = PAN_WORDS.some(function (w) { return has(stoveText, w); });
    var onePot = has(title, 'one-pot') || has(title, 'one pot') || has(title, 'one-pan') || has(title, 'one pan') || ip || slow;
    var noCook = r.cookMin === 0 && !ip && !slow && !air && !oven;
    var out = [];
    if (ip) out.push('instant-pot');
    if (slow) out.push('slow-cooker');
    if (air) out.push('air-fryer');
    if (onePot) out.push('one-pot');
    if (oven) out.push('oven');
    if (stove && !noCook) out.push('stovetop');
    if (noCook) out.push('no-cook');
    if (out.length === 0 && r.cookMin !== 0) out.push('stovetop');
    if (cache) cache.set(r, out);
    return out;
  }
  function matches(r, filter) {
    if (!filter || filter.length === 0) return true;
    var m = methodsOf(r);
    return filter.every(function (f) { return m.indexOf(f) >= 0; });
  }
  var Methods = { IDS: IDS, label: function (id) { return LABELS[id] || id; }, of: methodsOf, has: function (r, id) { return methodsOf(r).indexOf(id) >= 0; },
    matches: matches, word: word,
    canonical: function (list) { var s = new Set((list || []).map(String)); return IDS.filter(function (id) { return s.has(id); }); } };

  // ======================= Units =======================
  var ORIGINAL = 'original', US = 'us', METRIC = 'metric', WEIGHTS = 'weights', MODES = [ORIGINAL, US, METRIC, WEIGHTS], CUP_ML = 240;
  function modeLabel(m) {
    if (m === US) return 'US (cups, oz, \u00b0F)';
    if (m === METRIC) return 'Metric (g, ml, \u00b0C)';
    if (m === WEIGHTS) return 'Weights for dry baking ingredients (grams)';
    return 'Recipe\u2019s own units';
  }
  function validMode(m) { return MODES.indexOf(m) >= 0 ? m : WEIGHTS; }
  function Prefs(mode, weightUnit, showCups) {
    this.mode = mode === undefined ? WEIGHTS : validMode(mode);
    this.weightUnit = weightUnit === 'oz' ? 'oz' : 'g';
    this.showCups = showCups === undefined ? true : !!showCups;
  }
  var CLASSES = ['flour', 'baking', 'spice', 'fat', 'liquid', 'dairy', 'grain', 'produce', 'other'];
  function Densities() { this.list = []; this.byKey = new Map(); this.problems = []; }
  Densities.prototype.forKey = function (key) {
    if (key == null || key === '') return null;
    var d = this.byKey.get(key);
    if (d === undefined) { var f = PR.family(key); if (f != null) d = this.byKey.get(f); }
    return d === undefined ? null : d;
  };
  function jParseDouble(s) {
    var t = PR.jtrim(s);
    return /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?[dDfF]?$/.test(t) ? parseFloat(t) : NaN;
  }
  function parseDensities(text) {
    var d = new Densities();
    text.split('\n').forEach(function (raw) {
      var line = PR.jtrim(raw);
      if (line === '' || line.charAt(0) === '#') return;
      var p = PR.jsplit(line, '\t');
      if (p.length !== 3) { d.problems.push('fields: ' + line); return; }
      var g = jParseDouble(p[1]);
      if (isNaN(g)) { d.problems.push('number: ' + line); return; }
      var cls = PR.jtrim(p[2]);
      if (CLASSES.indexOf(cls) < 0 || g <= 0 || g > 400) { d.problems.push('value: ' + line); return; }
      var key = PR.normalize(PR.jtrim(p[0]));
      if (key === '') { d.problems.push('key: ' + line); return; }
      if (d.byKey.has(key)) { d.problems.push('duplicate key ' + key + ': ' + line); return; }
      var x = { name: PR.jtrim(p[0]), key: key, gPerCup: g, cls: cls, dryBaking: function () { return this.cls === 'flour' || this.cls === 'baking'; } };
      d.list.push(x);
      d.byKey.set(key, x);
    });
    return d;
  }
  var GLYPH8 = ['', '\u215b', '\u00bc', '\u215c', '\u00bd', '\u215d', '\u00be', '\u215e'];
  function jr(v) { return Math.floor(v + 0.5); }
  function frac(v, step) {
    var units = jr(v / step);
    if (units <= 0) units = 1;
    var eighths = jr(units * step * 8), w = Math.floor(eighths / 8), e = eighths % 8;
    return (w > 0 ? String(w) : '') + GLYPH8[e];
  }
  function whole(v) { var r = jr(v); return String(r === 0 ? 0 : r); }
  function dec2(v) {
    if (v < 0 && jr(-v * 100) > 0) return '-' + dec2(-v);
    var c = jr(v * 100);
    if (c < 0) c = 0;
    var w = Math.floor(c / 100), f = c % 100;
    if (f === 0) return String(w);
    if (f % 10 === 0) return w + '.' + (f / 10);
    return w + '.' + (f < 10 ? '0' : '') + f;
  }
  function grams(g, flour) {
    var r;
    if (g < 10) r = Math.max(1, jr(g));
    else if (g >= 100 || (flour && g >= 20)) r = jr(g / 5) * 5;
    else r = jr(g);
    return whole(r) + ' g';
  }
  function metricMass(g, flour) { return g >= 1000 ? dec2(jr(g / 50) * 0.05) + ' kg' : grams(g, flour); }
  function ounces(oz) {
    if (oz < 0.09375) return 'under \u215b oz (' + grams(oz * 28.3495, false) + ')';
    if (oz < 1) return frac(oz, 0.125) + ' oz';
    if (oz < 4) return frac(oz, 0.25) + ' oz';
    if (oz < 16) return frac(oz, 0.5) + ' oz';
    return whole(oz) + ' oz';
  }
  function usMass(g) { var oz = g / 28.3495; return oz < 16 ? ounces(oz) : frac(oz / 16, 0.25) + ' lb'; }
  function millilitres(ml) {
    if (ml >= 1000) return dec2(jr(ml / 50) * 0.05) + ' l';
    var r = ml < 100 ? Math.max(1, jr(ml)) : jr(ml / 5) * 5;
    return whole(r) + ' ml';
  }
  function tsp(v) { if (v < 0.1) return 'pinch'; if (v < 0.1875) return '\u215b tsp'; return frac(v, 0.25) + ' tsp'; }
  function tbsp(v) { return v < 0.875 ? tsp(v * 3) : frac(v, 0.5) + ' tbsp'; }
  function singular(n) { return n === '1' || (n.length === 1 && !(n.charAt(0) >= '0' && n.charAt(0) <= '9')); }
  var CUP_GLYPH = ['', '\u00bc', '\u2153', '\u00bd', '\u2154', '\u00be', ''], CUP_VAL = [0, 0.25, 1 / 3, 0.5, 2 / 3, 0.75, 1];
  function cupNumber(c) {
    if (c >= 10) return whole(c);
    var w = Math.floor(c), f = c - w, best = 0, bd = 9;
    for (var k = 0; k < CUP_VAL.length; k++) { var dd = Math.abs(f - CUP_VAL[k]); if (dd < bd - 1e-9) { bd = dd; best = k; } }
    if (best === 6) { w++; best = 0; }
    if (w === 0 && best === 0) return '\u00bc';
    return (w > 0 ? String(w) : '') + CUP_GLYPH[best];
  }
  function cups(c) { var n = cupNumber(c); return n + (singular(n) ? ' cup' : ' cups'); }
  function usVolume(ml) {
    if (ml >= CUP_ML / 4 - 4) return cups(ml / CUP_ML);
    if (ml >= 7.5) return tbsp(ml / 15);
    return tsp(ml / 5);
  }
  function spoon(c) { return c === 'tsp' || c === 'tbsp'; }
  function spoonText(v, c) { return c === 'tsp' ? tsp(v) : tbsp(v); }
  function weight(g, p, flour) { return p.weightUnit === 'oz' ? ounces(g / 28.3495) : grams(g, flour); }
  function display(key, amount, unit, scale, p, ds) {
    if (amount == null || isNaN(amount)) return unit == null ? '' : unit;
    if (p == null || p.mode === ORIGINAL) return PR.formatQty(amount, unit, scale);
    var v = amount * scale, c = A.canon(unit), dim = A.dimension(c);
    var d = ds == null ? null : ds.forKey(key);
    var flour = d != null && d.cls === 'flour', dry = d != null && d.dryBaking();
    if (c === 'cm' || c === 'inch' || c === 'in') {
      if (p.mode === US && c === 'cm') return frac(v / 2.54, 0.5) + ' in';
      if (p.mode === METRIC && c !== 'cm') return whole(v * 2.54) + ' cm';
      return PR.formatQty(amount, unit, scale);
    }
    if (dim === 'mass') {
      var g = A.convert(v, c, 'g');
      if (p.mode === US) {
        if (d != null && d.cls !== 'produce' && d.cls !== 'other') return usVolume(g / d.gPerCup * CUP_ML);
        return usMass(g);
      }
      if (p.mode === METRIC) return metricMass(g, flour);
      var w = weight(g, p, flour);
      if (dry && p.showCups) return w + ' (' + usVolume(g / d.gPerCup * CUP_ML) + ')';
      return w;
    }
    if (dim === 'volume') {
      var ml = A.convert(v, c, 'ml'), sp = spoon(c);
      if (p.mode === US) return sp ? spoonText(v, c) : usVolume(ml);
      if (p.mode === METRIC) {
        if (sp) return spoonText(v, c);
        if (d != null && d.cls !== 'liquid' && d.cls !== 'spice') return metricMass(ml / CUP_ML * d.gPerCup, flour);
        return millilitres(ml);
      }
      if (dry && c !== 'tsp') {
        var g2 = ml / CUP_ML * d.gPerCup, w2 = weight(g2, p, flour);
        if (p.showCups) return w2 + ' (' + (sp ? spoonText(v, c) : usVolume(ml)) + ')';
        return w2;
      }
      if (sp) return spoonText(v, c);
      return PR.formatQty(amount, unit, scale);
    }
    return PR.formatQty(amount, unit, scale);
  }
  function displayIng(i, scale, p, ds) { return display(i.key, i.amount, i.unit, scale, p, ds); }
  function cToF(c) { return c * 9 / 5 + 32; }
  function fToC(f) { return (f - 32) * 5 / 9; }
  function roundF(f) { return f >= 245 ? jr(f / 25) * 25 : jr(f); }
  function roundC(c) { return c >= 120 ? jr(c / 10) * 10 : jr(c); }
  // v1.8: also negative temperatures and ranges (same as Units.java); group 1 is the character before (no lookbehind on old WebViews)
  var TEMP = /(^|[^\w.\-\u2212])([-\u2212]?\d{1,3})(?:( ?(?:-|\u2013|to) ?)([-\u2212]?\d{1,3}))? ?\u00b0 ?([CF])(?![A-Za-z])( ?\()?/g, CM = /(\d{1,3}) ?cm(?![A-Za-z])( ?\()?/g;
  function tempInt(x) { return parseInt(x.replace('\u2212', '-'), 10); }
  function round5(v) { return jr(v / 5) * 5; }
  function stepRange(a, b, range, toF) {
    var ra = toF ? roundF(a) : roundC(a);
    if (!range) return String(ra);
    var rb = toF ? roundF(b) : roundC(b);
    if (ra === rb && a !== b) { ra = round5(a); rb = round5(b); }
    if (ra === rb) return String(ra);
    return ra + (ra < 0 || rb < 0 ? ' to ' : '\u2013') + rb;
  }
  function stepText(text, p) {
    if (text == null || p == null || p.mode === ORIGINAL) return text;
    var us = p.mode === US;
    var t = text.replace(TEMP, function (all, pre, a, sep, b, sc, paren) {
      if (paren) return all;
      var lo = tempInt(a), range = b != null && b !== '', hi = range ? tempInt(b) : lo;
      if (us && sc === 'C') return all + ' (' + stepRange(cToF(lo), cToF(hi), range, true) + '\u00b0F)';
      if (!us && sc === 'F') return all + ' (' + stepRange(fToC(lo), fToC(hi), range, false) + '\u00b0C)';
      return all;
    });
    if (!us) return t;
    return t.replace(CM, function (all, n, paren) { return paren ? all : all + ' (' + frac(parseInt(n, 10) / 2.54, 0.5) + ' in)'; });
  }
  var OVEN = [['120', '250', '\u00bd', '100', 'Very low'], ['140', '275', '1', '120', 'Very low'], ['150', '300', '2', '130', 'Low'],
    ['160', '325', '3', '140', 'Moderately low'], ['180', '350', '4', '160', 'Moderate'], ['190', '375', '5', '170', 'Moderately hot'],
    ['200', '400', '6', '180', 'Hot'], ['220', '425', '7', '200', 'Hot'], ['230', '450', '8', '210', 'Very hot'], ['240', '475', '9', '220', 'Very hot']];
  function gasMark(c) {
    if (c < 110 || c > 250) return '';
    var best = '', bd = 1e9;
    OVEN.forEach(function (r) { var d = Math.abs(Number(r[0]) - c); if (d < bd) { bd = d; best = r[2]; } });
    return best;
  }
  function ovenRow(mark) {
    var m = PR.jtrim(mark == null ? '' : mark).split('1/2').join('\u00bd');
    for (var i = 0; i < OVEN.length; i++) if (OVEN[i][2] === m) return OVEN[i];
    return null;
  }
  // v1.8: temperatures and ranges with negative numbers ("-20 to -5", "180-200", "350°F")
  var TEMP_IN = /^\s*([-+]?\d+(?:[.,]\d+)?)\s*(?:\u00b0?\s*[cf])?\s*(?:(?:-|\u2013|\u2014|to)\s*([-+]?\d+(?:[.,]\d+)?)\s*(?:\u00b0?\s*[cf])?\s*)?$/;
  function parseTemp(value) {
    if (value == null) return null;
    var m = TEMP_IN.exec(String(value).toLowerCase().split('\u2212').join('-'));
    if (!m) return null;
    var a = parseFloat(m[1].replace(',', '.')), b = m[2] == null ? a : parseFloat(m[2].replace(',', '.'));
    return a <= b ? [a, b] : [b, a];
  }
  function rangeText(a, b, sep) { return a === b ? String(a) : a + sep + b; }
  // v1.8 (same as Units.java): ranges with a negative end use ' to '; oven dial only for oven temperatures
  function tempRange(lo, hi, scale) {
    var f = scale === 'F', cLo = f ? fToC(lo) : lo, cHi = f ? fToC(hi) : hi, gLo = gasMark(cLo), gHi = gasMark(cHi);
    var gas = gLo === '' || gHi === '' ? '' : ' \u00b7 gas ' + (gLo === gHi ? gLo : gLo + '\u2013' + gHi);
    var oLo = f ? cToF(cLo) : cToF(lo), oHi = f ? cToF(cHi) : cToF(hi);
    var sep = lo < 0 || hi < 0 || (f ? cLo < 0 : oLo < 0) ? ' to ' : '\u2013';
    var oven = cLo >= 100;
    if (f) return dec2(lo) + sep + dec2(hi) + ' \u00b0F = ' + rangeText(jr(cLo), jr(cHi), sep) + ' \u00b0C'
      + (oven ? ' (oven dial ~' + rangeText(roundC(cLo), roundC(cHi), sep) + ' \u00b0C)' : '') + gas
      + (cLo >= 120 ? ' \u00b7 fan ~' + rangeText(roundC(cLo) - 20, roundC(cHi) - 20, sep) + ' \u00b0C' : '');
    return dec2(lo) + sep + dec2(hi) + ' \u00b0C = ' + rangeText(jr(oLo), jr(oHi), sep) + ' \u00b0F'
      + (oven ? ' (oven dial ~' + rangeText(roundF(oLo), roundF(oHi), sep) + ' \u00b0F)' : '') + gas
      + (lo >= 120 ? ' \u00b7 fan ~' + rangeText(jr(lo - 20), jr(hi - 20), sep) + ' \u00b0C' : '');
  }
  function gasToC(mark) {
    var m = PR.jtrim(mark == null ? '' : mark).split('1/2').join('\u00bd');
    for (var i = 0; i < OVEN.length; i++) if (OVEN[i][2] === m) return Number(OVEN[i][0]);
    return NaN;
  }
  function one(s) {
    s = PR.jtrim(s);
    if (s === '') return NaN;
    if (!/^[0-9.,\/ \u00bc\u00bd\u00be\u2153\u2154\u215b]+$/.test(s)) return NaN;
    var v = PR.parseNumber(s.replace(/ +/g, ' '));
    if (/^[0-9]+ +[0-9]+$/.test(s)) return NaN;
    return isNaN(v) || v < 0 ? NaN : v;
  }
  function parseAmount(text) {
    if (text == null) return null;
    var t = PR.jtrim(text).toLowerCase().split('\u2013').join('-').split('\u2014').join('-');
    if (t === '') return null;
    var cut = t.indexOf(' to ');
    if (cut < 0) { var dash = t.indexOf('-', 1); if (dash > 0) cut = dash; }
    if (cut > 0) {
      var skip = t.charAt(cut) === '-' ? 1 : 4;
      var a = one(t.substring(0, cut)), b = one(t.substring(cut + skip));
      if (isNaN(a) || isNaN(b) || b < a) return null;
      return [a, b];
    }
    var x = one(t);
    return isNaN(x) ? null : [x, x];
  }
  function volumeToGrams(v, unit, d) {
    var ml = A.convert(v, unit, 'ml');
    if (d == null || isNaN(ml)) return NaN;
    return ml / CUP_ML * d.gPerCup;
  }
  function gramsToVolume(v, unit, toUnit, d) {
    var g = A.convert(v, unit, 'g');
    if (d == null || isNaN(g)) return NaN;
    return A.convert(g / d.gPerCup * CUP_ML, 'ml', toUnit);
  }
  function butterGrams(v, unit) {
    var u = PR.jtrim(unit == null ? '' : unit).toLowerCase();
    if (u.indexOf('stick') === 0) return v * 113.4;
    var ml = A.convert(v, u, 'ml');
    if (!isNaN(ml)) return ml / CUP_ML * 227;
    return A.convert(v, u, 'g');
  }
  function butterText(g) {
    var sticks = g / 113.4;
    var st = sticks >= 0.2 ? ' \u00b7 ' + frac(sticks, 0.25) + (singular(frac(sticks, 0.25)) ? ' stick' : ' sticks') : '';
    return grams(g, false) + ' \u00b7 ' + ounces(g / 28.3495) + st + ' \u00b7 ' + tbsp(g / 227 * 16) + (g / 227 >= 0.23 ? ' \u00b7 ' + cups(g / 227) : '');
  }
  function volumeLine(v, unit) {
    var ml = A.convert(v, unit, 'ml');
    if (isNaN(ml) || A.dimension(unit) !== 'volume' || ml <= 0) return '';
    return millilitres(ml) + (ml >= CUP_ML / 4 - 4 ? ' \u00b7 ' + cups(ml / CUP_ML) : '') + ' \u00b7 ' + frac(ml / 15, 0.5) + ' tbsp \u00b7 '
      + frac(ml / 5, 0.25) + ' tsp \u00b7 ' + frac(ml / 30, 0.25) + ' fl oz';
  }
  function massLine(v, unit) {
    var g = A.convert(v, unit, 'g');
    if (isNaN(g) || A.dimension(unit) !== 'mass' || g <= 0) return '';
    var s = metricMass(g, false) + ' \u00b7 ' + ounces(g / 28.3495);
    if (g >= 453.592 / 2) s += ' \u00b7 ' + frac(g / 453.592, 0.25) + ' lb';
    return s;
  }
  function tempLine(value, scale) {
    if (scale === 'gas') {
      var row = ovenRow(value);
      if (row == null) return '';
      return 'Gas ' + row[2] + ' \u2248 ' + row[0] + ' \u00b0C \u00b7 ' + row[1] + ' \u00b0F \u00b7 fan ' + row[3] + ' \u00b0C';
    }
    var a = parseTemp(value);
    if (a == null) return '';
    if (a[1] !== a[0]) return tempRange(a[0], a[1], scale);
    var v = a[0], g;
    if (scale === 'F') {
      var cc = fToC(v);
      g = gasMark(cc);
      return dec2(v) + ' \u00b0F = ' + jr(cc) + ' \u00b0C' + (cc >= 100 ? ' (oven dial ~' + roundC(cc) + ' \u00b0C)' : '') + (g === '' ? '' : ' \u00b7 gas ' + g)
        + (cc >= 120 ? ' \u00b7 fan ~' + (roundC(cc) - 20) + ' \u00b0C' : '');
    }
    var f = cToF(v);
    g = gasMark(v);
    return dec2(v) + ' \u00b0C = ' + jr(f) + ' \u00b0F' + (v >= 100 ? ' (oven dial ~' + roundF(f) + ' \u00b0F)' : '') + (g === '' ? '' : ' \u00b7 gas ' + g)
      + (v >= 120 ? ' \u00b7 fan ~' + jr(v - 20) + ' \u00b0C' : '');
  }
  function ingredientLine(v, unit, d) {
    if (d == null || v <= 0) return '';
    var dim = A.dimension(unit);
    if (dim === 'volume') { var g = volumeToGrams(v, unit, d); return '\u2248 ' + grams(g, d.cls === 'flour') + ' \u00b7 ' + ounces(g / 28.3495); }
    if (dim === 'mass') { var g2 = A.convert(v, unit, 'g'), ml = g2 / d.gPerCup * CUP_ML; return '\u2248 ' + usVolume(ml) + ' \u00b7 ' + millilitres(ml) + ' by volume'; }
    return '';
  }
  function range(text, f) {
    var r = parseAmount(text);
    if (r == null) return '';
    var lo = f(r[0]);
    if (lo === '') return '';
    if (r[0] === r[1]) return lo;
    return lo + '\nto\n' + f(r[1]);
  }
  var Units = {
    ORIGINAL: ORIGINAL, US: US, METRIC: METRIC, WEIGHTS: WEIGHTS, MODES: MODES, CUP_ML: CUP_ML, CLASSES: CLASSES, OVEN: OVEN,
    VOLUME_UNITS: ['tsp', 'tbsp', 'cup', 'fl oz', 'ml', 'l'], MASS_UNITS: ['g', 'kg', 'oz', 'lb'],
    modeLabel: modeLabel, validMode: validMode, Prefs: Prefs, parseDensities: parseDensities,
    frac: frac, dec2: dec2, grams: grams, metricMass: metricMass, ounces: ounces, usMass: usMass, millilitres: millilitres, tsp: tsp, tbsp: tbsp,
    cups: cups, cupNumber: cupNumber, usVolume: usVolume, display: display, displayIng: displayIng, cToF: cToF, fToC: fToC, roundF: roundF, roundC: roundC,
    stepText: stepText, gasMark: gasMark, gasToC: gasToC, parseAmount: parseAmount, parseTemp: parseTemp, convert: A.convert, volumeToGrams: volumeToGrams,
    gramsToVolume: gramsToVolume, butterGrams: butterGrams, butterText: butterText, volumeLine: volumeLine, massLine: massLine, tempLine: tempLine,
    ingredientLine: ingredientLine, range: range
  };

  // ======================= v1.8 Titles (same as Titles.java) =======================
  var T_SKIP = new Set(['the', 'a', 'an', 'and', 'with', 'of', 'in', 'on', 'for', 'style']);
  var T_BORING = new Set(['salt', 'black pepper', 'pepper', 'water', 'olive oil', 'vegetable oil', 'oil', 'sugar', 'butter', 'garlic', 'onion']);
  function tfold(s) { s = s == null ? '' : String(s); return (/[^\x00-\x7f]/.test(s) ? s.normalize('NFD').replace(/[\u0300-\u036f]/g, '') : s).toLowerCase(); }
  function tWords(title) {
    var out = [], seen = new Set();
    tfold(title).split(/[^a-z0-9]+/).forEach(function (w) { if (w !== '' && !T_SKIP.has(w) && !seen.has(w)) { seen.add(w); out.push(w); } });
    return out;
  }
  function tKey(title) { return tWords(title).join(' '); }
  function tNear(a, b) {
    if (a.size === 0 || b.size === 0) return false;
    var inter = 0;
    a.forEach(function (w) { if (b.has(w)) inter++; });
    var union = a.size + b.size - inter;
    if (inter * 4 >= union * 3) return true;
    var sm = a.size <= b.size ? a : b;
    return inter === sm.size && sm.size >= 3 && union - inter <= 2;
  }
  function tStem(w) { return w.length > 3 && w.charAt(w.length - 1) === 's' ? w.substring(0, w.length - 1) : w; }
  function tInTitle(tw, key) {
    var t = new Set(); tw.forEach(function (w) { t.add(tStem(w)); });
    return tWords(key).every(function (w) { return t.has(tStem(w)); });
  }
  function tHint(r, group, all) {
    var others = new Set(), oc = new Set(), ot = new Set();
    group.forEach(function (j) {
      var o = all[j];
      if (o === r) return;
      o.ingredients.forEach(function (i) { others.add(i.key); });
      oc.add(o.cuisine); ot.add(PR.totalMinutes(o));
    });
    var tw = tWords(r.title);
    for (var k = 0; k < r.ingredients.length; k++) {
      var i = r.ingredients[k];
      if (i.optional || others.has(i.key) || T_BORING.has(i.key) || i.key === '') continue;
      if (tInTitle(tw, i.key)) continue;
      return 'with ' + i.key;
    }
    if (r.cuisine !== '' && !oc.has(r.cuisine)) return r.cuisine;
    var t = PR.totalMinutes(r);
    if (t > 0 && !ot.has(t)) return t >= 60 ? Math.floor(t / 60) + ' h' + (t % 60 === 0 ? '' : ' ' + (t % 60) + ' min') : t + ' min';
    return null;
  }
  function tHints(recipes) {
    var n = recipes.length, ws = [], byWord = new Map(), parent = [], any = [];
    function find(i) { while (parent[i] !== i) { parent[i] = parent[parent[i]]; i = parent[i]; } return i; }
    for (var i = 0; i < n; i++) {
      var w = new Set(tWords(recipes[i].title));
      ws.push(w); parent.push(i); any.push(false);
      w.forEach(function (x) { var l = byWord.get(x); if (!l) byWord.set(x, l = []); l.push(i); });
    }
    for (i = 0; i < n; i++) {
      var cand = new Set();
      ws[i].forEach(function (x) { byWord.get(x).forEach(function (j) { if (j > i) cand.add(j); }); });
      cand.forEach(function (j) {
        if (!tNear(ws[i], ws[j])) return;
        any[i] = any[j] = true;
        var a = find(i), b = find(j);
        if (a !== b) parent[Math.max(a, b)] = Math.min(a, b);
      });
    }
    var groups = new Map();
    for (i = 0; i < n; i++) if (any[i]) { var r0 = find(i), g = groups.get(r0); if (!g) groups.set(r0, g = []); g.push(i); }
    var out = new Map();
    groups.forEach(function (g) { g.forEach(function (i) { var h = tHint(recipes[i], g, recipes); if (h != null) out.set(recipes[i].id, h); }); });
    return out;
  }
  PR.Titles = { words: tWords, key: tKey, near: tNear, hints: tHints };

  PR.Methods = Methods;
  PR.Units = Units;
})(typeof window !== 'undefined' ? window : globalThis);
