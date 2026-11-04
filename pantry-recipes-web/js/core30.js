/*
 * My Pantry Recipes web - v2.0 Stage 3 core: holidays, fasting-day labels and baking helpers.
 * Line-by-line port of the Android core/Stage3.java; data is js/stage3-data.js (raw text, parsed on first use).
 * Checked against tests/vectors20.json ("stage3") produced by the Java code.
 */
(function (root) {
  'use strict';
  var PR = root.PR;
  var D = PR.Dates;
  var NOTE = 'Holiday dates for lunar calendars come from published tables for 2026-2030 and may differ by a day where you live. '
    + 'Fasting labels are a guide worked out from ingredient names; customs vary, so check your own tradition.';

  function cells(line) { return line.split('|').map(function (c) { return c.trim(); }); }

  function parse(text) {
    var s = { holidays: [], fasts: [], trouble: [], storage: [], altitude: [], party: [], byId: {}, fastById: {} };
    var sec = '', h = null, f = null, t = null;
    String(text || '').split('\n').forEach(function (raw) {
      var line = raw.trim();
      if (!line || line.charAt(0) === '#') return;
      if (line.charAt(0) === '[' && line.charAt(line.length - 1) === ']') { sec = line.slice(1, -1); h = f = t = null; return; }
      var p;
      if (sec === 'holidays') {
        if (line.indexOf('@ ') === 0) {
          p = cells(line.slice(2));
          h = { id: p[0], name: p[1], rule: p[2], note: '', diet: '', regions: [], title: [], ing: [], excl: [], cuisines: [] };
          p[3].split(',').forEach(function (r) { r = r.trim(); if (r && r !== '-' && h.regions.indexOf(r) < 0) h.regions.push(r); });
          s.holidays.push(h); s.byId[h.id] = h;
        } else if (h && line.length > 2 && line.charAt(1) === ':') {
          var k = line.charAt(0), v = line.slice(2).trim();
          if (k === 'n') { h.note = v; return; }
          if (k === 'd') { h.diet = v; return; }
          v.split(',').forEach(function (w) {
            w = w.trim().toLowerCase();
            if (!w) return;
            var into = k === 't' ? h.title : k === 'i' ? h.ing : k === 'x' ? h.excl : k === 'c' ? h.cuisines : null;
            if (into && into.indexOf(w) < 0) into.push(w);
          });
        }
      } else if (sec === 'fasting') {
        if (line.indexOf('@ ') === 0) {
          p = cells(line.slice(2));
          f = { id: p[0], label: p[1], note: '', rules: [] };
          p[2].split(',').forEach(function (r) { r = r.trim(); if (r && f.rules.indexOf(r) < 0) f.rules.push(r); });
          s.fasts.push(f); s.fastById[f.id] = f;
        } else if (f && line.indexOf('n:') === 0) f.note = line.slice(2).trim();
      } else if (sec === 'bake-trouble') {
        if (line.indexOf('@ ') === 0) { p = cells(line.slice(2)); t = { problem: p[0], bakes: p.length > 1 ? p[1] : '', fixes: [] }; s.trouble.push(t); }
        else if (t && line.indexOf('- ') === 0) t.fixes.push(line.slice(2).trim());
      } else if (sec === 'bake-storage') s.storage.push(cells(line));
      else if (sec === 'altitude') s.altitude.push(cells(line));
      else if (sec === 'party') s.party.push(cells(line));
    });
    return s;
  }

  var memo = null;
  function data() { if (!memo) memo = parse(root.PR_STAGE3 || ''); return memo; }

  // ---------------- dates ----------------
  function pad(n, w) { var s = String(n); while (s.length < w) s = '0' + s; return s; }
  function iso(y, m, d) { return pad(y, 4) + '-' + pad(m, 2) + '-' + pad(d, 2); }
  function dim(y, m) { return m === 2 ? ((y % 4 === 0 && y % 100 !== 0) || y % 400 === 0 ? 29 : 28) : (m === 4 || m === 6 || m === 9 || m === 11) ? 30 : 31; }
  function idiv(a, b) { return Math.floor(a / b); }
  function mod7(n) { return ((n % 7) + 7) % 7; }

  function easter(y) {
    var a = y % 19, b = idiv(y, 100), c = y % 100, d = idiv(b, 4), e = b % 4, f = idiv(b + 8, 25), g = idiv(b - f + 1, 3);
    var h = (19 * a + b - d - g + 15) % 30, i = idiv(c, 4), k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = idiv(a + 11 * h + 22 * l, 451);
    return iso(y, idiv(h + l - 7 * m + 114, 31), (h + l - 7 * m + 114) % 31 + 1);
  }
  function orthodoxEaster(y) {
    var a = y % 4, b = y % 7, c = y % 19, d = (19 * c + 15) % 30, e = (2 * a + 4 * b - d + 34) % 7;
    return D.addDays(iso(y, idiv(d + e + 114, 31), (d + e + 114) % 31 + 1), 13);
  }
  function int(s) { var n = parseInt(String(s).replace('+', ''), 10); if (isNaN(n)) throw new Error('bad'); return n; }

  function dateIn(h, y) {
    var r = h.rule, p, md, from, wd;
    try {
      if (r.indexOf('fixed:') === 0) { p = r.slice(6).split('-'); return iso(y, int(p[0]), int(p[1])); }
      if (r.indexOf('easter:') === 0) return D.addDays(easter(y), int(r.slice(7)));
      if (r.indexOf('oeaster:') === 0) return D.addDays(orthodoxEaster(y), int(r.slice(8)));
      if (r.indexOf('nth:') === 0) {
        p = r.slice(4).split(':');
        var m = int(p[0]), n = int(p[2]); wd = int(p[1]) - 1;
        if (n > 0) { var first = iso(y, m, 1); return D.addDays(first, mod7(wd - D.dayOfWeek(first)) + 7 * (n - 1)); }
        var last = iso(y, m, dim(y, m));
        return D.addDays(last, -mod7(D.dayOfWeek(last) - wd));
      }
      if (r.indexOf('wday:') === 0) {
        p = r.slice(5).split(':'); md = p[0].split('-');
        from = iso(y, int(md[0]), int(md[1])); wd = int(p[1]) - 1;
        return D.addDays(from, mod7(wd - D.dayOfWeek(from)));
      }
      if (r.indexOf('table:') === 0) {
        var pre = y + '-', list = r.slice(6).split(',');
        for (var i = 0; i < list.length; i++) if (list[i].trim().indexOf(pre) === 0) return list[i].trim();
        return null;
      }
    } catch (e) { return null; }
    return null;
  }

  function next(h, today) {
    var y = parseInt(today.slice(0, 4), 10);
    for (var yy = y; yy <= y + 1; yy++) { var d = dateIn(h, yy); if (d !== null && d >= today) return d; }
    return null;
  }
  function defaultOn(h, region) { return h.regions.indexOf('all') >= 0 || (region != null && h.regions.indexOf(region) >= 0); }

  function upcoming(s, ticked, today, horizon) {
    var out = [];
    s.holidays.forEach(function (h) {
      if (ticked && ticked.indexOf(h.id) < 0) return;
      var d = next(h, today);
      if (d === null) return;
      var n = D.between(today, d);
      if (n <= horizon) out.push({ h: h, date: d, days: n });
    });
    out.sort(function (a, b) { return a.days !== b.days ? a.days - b.days : (a.h.name < b.h.name ? -1 : a.h.name > b.h.name ? 1 : 0); });
    return out;
  }

  // ---------------- recipe sections ----------------
  function isLetter(c) { return c !== undefined && c.toLowerCase() !== c.toUpperCase(); }
  function hasWord(hay, w) {
    var i = hay.indexOf(w);
    while (i >= 0) {
      var a = i === 0 || !isLetter(hay.charAt(i - 1));
      var e = i + w.length;
      var b = e >= hay.length || !isLetter(hay.charAt(e)) || (hay.charAt(e) === 's' && (e + 1 >= hay.length || !isLetter(hay.charAt(e + 1))));
      if (a && b) return true;
      i = hay.indexOf(w, i + 1);
    }
    return false;
  }

  function dietOk(s, d, r) {
    var f = s.fastById[d];
    if (f) return fits(f, r);
    return r.tags.indexOf(d) >= 0 || (r.diets || []).indexOf(d) >= 0;
  }

  function score(s, h, r) {
    var i, j, k;
    if (h.diet && !dietOk(s, h.diet, r)) return 0;
    for (i = 0; i < r.ingredients.length; i++) {
      if (r.ingredients[i].optional) continue;
      for (j = 0; j < h.excl.length; j++) if (hasWord(r.ingredients[i].key, h.excl[j])) return 0;
    }
    var t = r.title.toLowerCase(), sc = 0, n = h.title.length;
    for (k = 0; k < n; k++) if (hasWord(t, h.title[k])) { sc += 3 + (3 * k < n ? 2 : 3 * k < 2 * n ? 1 : 0); break; }
    if (h.cuisines.length && h.cuisines.indexOf(String(r.cuisine || '').toLowerCase()) >= 0) sc += 2;
    var ing = 0;
    for (i = 0; i < r.ingredients.length; i++) for (j = 0; j < h.ing.length; j++) if (hasWord(r.ingredients[i].key, h.ing[j])) { ing++; break; }
    sc += Math.min(2, ing);
    return sc >= 2 ? sc : 0;
  }

  function cmpTitle(a, b) { var x = a.title.toLowerCase(), y = b.title.toLowerCase(); return x < y ? -1 : x > y ? 1 : 0; }
  function section(s, h, all, max) {
    var out = [];
    all.forEach(function (r) { var sc = score(s, h, r); if (sc > 0) out.push({ r: r, s: sc }); });
    out.sort(function (a, b) { return b.s - a.s || cmpTitle(a.r, b.r); });
    return out.slice(0, max).map(function (o) { return o.r; });
  }

  // ---------------- fasting labels ----------------
  function set(a) { var o = {}; a.forEach(function (w) { o[w] = true; }); return o; }
  var ALLIUM = set(['onion', 'onions', 'garlic', 'leek', 'leeks', 'shallot', 'shallots', 'chive', 'chives', 'scallion', 'scallions']);
  var EGG = set(['egg', 'eggs', 'yolk', 'yolks', 'mayonnaise', 'mayo', 'meringue', 'aioli']);
  var ROOT = set(['potato', 'potatoes', 'carrot', 'carrots', 'beet', 'beets', 'beetroot', 'radish', 'radishes', 'ginger', 'turnip', 'turnips',
    'parsnip', 'parsnips', 'yam', 'yams', 'cassava', 'taro', 'celeriac', 'horseradish', 'rutabaga', 'swede', 'daikon', 'jicama']);
  var ROOT_OK = ['ground ginger', 'dried ginger', 'ginger powder', 'ginger ale', 'potato starch'];
  function anyWord(key, st) { var w = key.split(' '); for (var i = 0; i < w.length; i++) if (st[w[i]] === true) return true; return false; }

  function fits(f, r) {
    var ru = f.rules, has = function (x) { return ru.indexOf(x) >= 0; };
    if (has('vegan') && r.tags.indexOf('vegan') < 0) return false;
    if (has('veg') && r.tags.indexOf('vegetarian') < 0) return false;
    if (has('nomeat') && r.tags.indexOf('vegetarian') < 0 && (r.diets || []).indexOf('pescatarian') < 0) return false;
    for (var i = 0; i < r.ingredients.length; i++) {
      var ig = r.ingredients[i], k = ig.key;
      if (ig.optional || !k) continue;
      if (has('noegg') && k.indexOf('vegan ') !== 0 && anyWord(k, EGG)) return false;
      if (has('noallium') && anyWord(k, ALLIUM)) return false;
      if (has('nohoney') && (' ' + k + ' ').indexOf(' honey ') >= 0) return false;
      if (has('noroot') && (anyWord(k, ALLIUM) || (anyWord(k, ROOT) && ROOT_OK.indexOf(k) < 0))) return false;
      if (has('nograin') && PR.Diets.isGrain(k)) return false;
      if (has('nolegume') && PR.Diets.isLegume(k)) return false;
    }
    return true;
  }
  function labels(s, r, ticked) { return s.fasts.filter(function (f) { return ticked.indexOf(f.id) >= 0 && fits(f, r); }); }

  // ---------------- baking ----------------
  function bakers(flour, hyd, salt, yeast, starter) {
    var w = flour * hyd / 100, sa = flour * salt / 100, ye = flour * yeast / 100, st = flour * starter / 100;
    return [w, sa, ye, st, flour + w + sa + ye + st];
  }
  function flourFor(dough, hyd, salt, yeast, starter) { return dough / (1 + (hyd + salt + yeast + starter) / 100); }
  function panArea(shape, a, b) { return shape === 'round' ? Math.PI * a * a / 4 : shape === 'square' ? a * a : a * b; }
  function panFactor(from, to) { return from <= 0 ? 0 : to / from; }
  function timeline(start, steps) { var out = [], t = start; steps.forEach(function (m) { out.push(t); t += Math.max(0, m); }); out.push(t); return out; }
  function startFor(end, steps) { var t = end; steps.forEach(function (m) { t -= Math.max(0, m); }); return t; }
  function clock(min, h24) {
    var day = min >= 0 ? idiv(min, 1440) : -idiv(-min + 1439, 1440);
    var m = min - day * 1440, hh = idiv(m, 60), mm = m % 60;
    var t = h24 ? pad(hh, 2) + ':' + pad(mm, 2) : (hh % 12 === 0 ? 12 : hh % 12) + ':' + pad(mm, 2) + ' ' + (hh < 12 ? 'AM' : 'PM');
    return day === 0 ? t : day > 0 ? t + ' (+' + day + ' day' + (day > 1 ? 's' : '') + ')' : t + ' (' + day + ' day' + (day < -1 ? 's' : '') + ')';
  }
  function altitudeFor(s, feet) { var best = null; s.altitude.forEach(function (r) { if (feet >= parseFloat(r[0])) best = r; }); return best; }
  function partyNeed(row, guests) { return Math.ceil(parseFloat(row[1]) * guests - 1e-9); }
  function cToF(c) { return c * 9 / 5 + 32; }
  function fToC(f) { return (f - 32) * 5 / 9; }
  function gasMark(c) {
    var t = [110, 120, 140, 150, 160, 180, 190, 200, 220, 230, 240], g = ['1/4', '1/2', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
    if (c < 105) return '';
    var best = 0;
    for (var i = 0; i < t.length; i++) if (Math.abs(t[i] - c) < Math.abs(t[best] - c)) best = i;
    return g[best];
  }

  PR.Stage3 = { NOTE: NOTE, parse: parse, data: data, easter: easter, orthodoxEaster: orthodoxEaster, dateIn: dateIn, next: next,
    defaultOn: defaultOn, upcoming: upcoming, hasWord: hasWord, score: score, section: section, dietOk: dietOk, fits: fits, labels: labels,
    bakers: bakers, flourFor: flourFor, panArea: panArea, panFactor: panFactor, timeline: timeline, startFor: startFor, clock: clock,
    altitudeFor: altitudeFor, partyNeed: partyNeed, cToF: cToF, fToC: fToC, gasMark: gasMark };
})(typeof window !== 'undefined' ? window : globalThis);
