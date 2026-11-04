/* My Pantry Recipes web v2.0.1: Renal, Cardiac and Diabetic diet guides (port of the Android core MedGuides.java).
 * A guide from ingredient names and rough estimates, never medical advice; a recipe is never called suitable.
 * Word lists, texts and the precomputed flags for the bundled recipes come from js/data201.js. MIT License. */
(function (root) {
  'use strict';
  var PR = root.PR, D = root.PR_DATA201 || {};
  function set(a) { return new Set(a || []); }
  var HIGH_K = set(D.HIGH_K), NOT_K = set(D.NOT_K), HIGH_P = set(D.HIGH_P), NOT_P = set(D.NOT_P), HIGH_SATFAT = set(D.HIGH_SATFAT), NOT_SATFAT = set(D.NOT_SATFAT);
  var CODES = 'KPSFGC', LABELS = D.labels || [], IDS = D.ids || ['renal', 'cardiac', 'diabetic'];
  var RENAL = 'renal', CARDIAC = 'cardiac', DIABETIC = 'diabetic';
  function word(key, words) {
    if (words.has(key)) return true;
    var p = key.split(' ');
    for (var i = 0; i < p.length; i++) if (words.has(p[i])) return true;
    return false;
  }
  function phrase(key, phrases) {
    var k = ' ' + key + ' ', hit = false;
    phrases.forEach(function (p) { if (!hit && k.indexOf(' ' + p + ' ') >= 0) hit = true; });
    return hit;
  }
  function has(key, words, except) { return (word(key, words) || phrase(key, words)) && !phrase(key, except); }
  function c(k, s) { return k.indexOf(s) >= 0; }
  function legumeK(key) {
    if (!PR.Diets.isLegume(key)) return false;
    return !(c(key, 'sauce') || c(key, 'oil') || c(key, 'milk') || c(key, 'tofu') || c(key, 'miso') || c(key, 'tamari') || c(key, 'sprout'));
  }
  function isHighPotassium(k) { return has(k, HIGH_K, NOT_K) || legumeK(k); }
  function isHighPhosphorus(k) { return has(k, HIGH_P, NOT_P) || legumeK(k) || PR.Diets.isProcessedMeat(k); }
  function isHighSodium(k) { return PR.Diets.isSalty(k); }
  function isHighSatFat(k) { if (phrase(k, NOT_SATFAT)) return false; return has(k, HIGH_SATFAT, NOT_SATFAT) || PR.Diets.isProcessedMeat(k); }
  function isAddedSugar(k) { return PR.Diets.isSugar(k); }
  function isHighCarb(k) { return PR.Diets.isGrain(k) || PR.Diets.isStarchy(k); }
  var TESTS = [isHighPotassium, isHighPhosphorus, isHighSodium, isHighSatFat, isAddedSugar, isHighCarb];
  function codes(g) { return g === RENAL ? 'KPS' : g === CARDIAC ? 'SF' : g === DIABETIC ? 'GC' : ''; }
  function name(g) { var k = IDS.indexOf(g); return k < 0 ? g : D.names[k]; }
  function saltSodiumMg(r) { var t = PR.Diets.saltTspPerServing(r); return isNaN(t) ? NaN : t * 2300; }
  function compute(r) {
    var out = [[], [], [], [], [], []], salt = PR.Diets.saltTspPerServing(r), saltHigh = !isNaN(salt) && salt > 0.25 + 1e-9;
    r.ingredients.forEach(function (i, n) {
      var k = i.key;
      if (!k) return;
      for (var c = 0; c < 6; c++) if (TESTS[c](k)) out[c].push(n);
      if (saltHigh && PR.Diets.SALT_KEYS.has(k) && out[2].indexOf(n) < 0) out[2].push(n);
    });
    return out;
  }
  function encode(f) {
    var parts = [];
    for (var c = 0; c < 6; c++) if (f[c].length) parts.push(CODES.charAt(c) + f[c].join('.'));
    return parts.join(' ');
  }
  function decode(s) {
    var out = [[], [], [], [], [], []];
    if (!s) return out;
    s.split(' ').forEach(function (part) {
      if (!part) return;
      var c = CODES.indexOf(part.charAt(0)), rest = part.substring(1);
      if (c < 0 || !rest) return;
      var v = rest.split('.').map(function (x) { return /^-?\d+$/.test(x) ? parseInt(x, 10) : NaN; });
      if (v.some(isNaN)) return;
      out[c] = v;
    });
    return out;
  }
  var pre = null;
  function precomputed() {
    if (pre) return pre;
    pre = new Map();
    String(D.flags || '').split('\n').forEach(function (line) {
      if (!line || line.charAt(0) === '#') return;
      var p = line.split('\t');
      if (p.length >= 3) pre.set(p[0], [p[1], p[2]]);
    });
    return pre;
  }
  function flags(r) {
    var e = precomputed().get(r.id);
    if (e && e[1] === String(r.ingredients.length)) return decode(e[0]);
    return compute(r);
  }
  function noFlags(f, g) { var cs = codes(g); for (var i = 0; i < cs.length; i++) if (f[CODES.indexOf(cs.charAt(i))].length) return false; return true; }
  function lines(r, f, g) {
    var out = [], cs = codes(g);
    for (var i = 0; i < cs.length; i++) {
      var c = CODES.indexOf(cs.charAt(i));
      if (!f[c].length) continue;
      var names = [];
      f[c].forEach(function (n) {
        if (n < 0 || n >= r.ingredients.length) return;
        var nm = String(r.ingredients[n].name).toLowerCase().trim();
        if (nm && names.indexOf(nm) < 0) names.push(nm);
      });
      if (!names.length) continue;
      out.push('contains ' + LABELS[c] + (names.length === 1 ? ' ingredient: ' : ' ingredients: ') + names.join(', '));
    }
    return out;
  }
  function clampSodium(v) { return Math.max(0, Math.min(20000, Math.round(v) || 0)); }
  function clampCarbs(v) { return Math.max(0, Math.min(1000, Math.round(v) || 0)); }
  function estimateLines(r, carbs, guides, sodiumLimit, carbLimit) {
    var out = [], na = guides.indexOf(RENAL) >= 0 || guides.indexOf(CARDIAC) >= 0, cb = guides.indexOf(DIABETIC) >= 0, s;
    if (na) {
      var mg = saltSodiumMg(r);
      if (isNaN(mg)) out.push('Added salt: \u201cto taste\u201d, not measured. Other ingredients add sodium too (not estimated).');
      else if (mg > 0) {
        s = 'Added salt alone \u2248 ' + Math.round(mg) + ' mg sodium per serving (estimate; other ingredients add more)';
        if (sodiumLimit > 0) s += mg > sodiumLimit ? ' \u2013 above your ' + sodiumLimit + ' mg per meal limit' : ' \u2013 your limit is ' + sodiumLimit + ' mg per meal';
        out.push(s + '.');
      } else if (sodiumLimit > 0) out.push('No measured added salt. Your sodium limit is ' + sodiumLimit + ' mg per meal; other ingredients still add sodium (not estimated).');
    }
    if (cb) {
      if (isNaN(carbs)) out.push('Carbs per serving: no estimate (too few ingredients matched).');
      else {
        s = 'Carbs \u2248 ' + Math.round(carbs) + ' g per serving (estimate)';
        if (carbLimit > 0) s += carbs > carbLimit ? ' \u2013 above your ' + carbLimit + ' g per meal limit' : ' \u2013 your limit is ' + carbLimit + ' g per meal';
        out.push(s + '.');
      }
    }
    return out;
  }
  PR.MedGuides = { IDS: IDS, RENAL: RENAL, CARDIAC: CARDIAC, DIABETIC: DIABETIC, CODES: CODES, DISCLAIMER: D.disclaimer, DATA_NOTE: D.dataNote, RULES: D.rules || {},
    name: name, codes: codes, compute: compute, encode: encode, decode: decode, flags: flags, noFlags: noFlags, lines: lines, saltSodiumMg: saltSodiumMg,
    estimateLines: estimateLines, clampSodium: clampSodium, clampCarbs: clampCarbs, precomputed: precomputed };
})(typeof self !== 'undefined' ? self : globalThis);
