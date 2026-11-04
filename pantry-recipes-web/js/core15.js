/* Pantry Recipes web - v1.5 core: use-by dates, allergens/dislikes, amounts, meal plan, backup file, themes.
   Line-by-line port of the Android app's core (org.pantryrecipes.app.core), checked against vectors
   exported from the Java code (tests/vectors.json). MIT License. */
(function (root) {
  'use strict';
  var PR = root.PR;
  var jtrim = PR.jtrim;

  // ======================= Java-compatible JSON (objects -> Map, keeps key order) =======================
  function JsonError(m) { var e = new Error(m); e.json = true; return e; }
  function jsonParse(s) {
    var i = 0, n = s.length;
    function ws() { while (i < n) { var c = s.charCodeAt(i); if (c === 32 || c === 10 || c === 13 || c === 9) i++; else break; } }
    function peek() { if (i >= n) throw JsonError('unexpected end'); return s.charAt(i); }
    function value(depth) {
      if (depth > 400) throw JsonError('too deep');
      if (i >= n) throw JsonError('unexpected end');
      var c = s.charAt(i);
      if (c === '{') return obj(depth);
      if (c === '[') return arr(depth);
      if (c === '"') return str();
      if (c === 't') { lit('true'); return true; }
      if (c === 'f') { lit('false'); return false; }
      if (c === 'n') { lit('null'); return null; }
      return num();
    }
    function lit(w) { if (s.substr(i, w.length) !== w) throw JsonError('bad literal'); i += w.length; }
    function obj(depth) {
      var m = new Map(); i++; ws();
      if (peek() === '}') { i++; return m; }
      for (;;) {
        ws();
        if (peek() !== '"') throw JsonError('expected key');
        var k = str(); ws();
        if (peek() !== ':') throw JsonError("expected ':'");
        i++; ws();
        m.set(k, value(depth + 1)); ws();
        var c = peek(); i++;
        if (c === '}') return m;
        if (c !== ',') throw JsonError("expected ',' or '}'");
      }
    }
    function arr(depth) {
      var l = []; i++; ws();
      if (peek() === ']') { i++; return l; }
      for (;;) {
        ws(); l.push(value(depth + 1)); ws();
        var c = peek(); i++;
        if (c === ']') return l;
        if (c !== ',') throw JsonError("expected ',' or ']'");
      }
    }
    function str() {
      i++;
      var b = '';
      for (;;) {
        var c = peek(); i++;
        if (c === '"') return b;
        if (c === '\\') {
          var e = peek(); i++;
          if (e === 'n') b += '\n'; else if (e === 'r') b += '\r'; else if (e === 't') b += '\t';
          else if (e === 'b') b += '\b'; else if (e === 'f') b += '\f';
          else if (e === 'u') {
            if (i + 4 > n) throw JsonError('bad escape');
            var h = /^([+-]?)([0-9a-fA-F]+)$/.exec(s.substring(i, i + 4));
            if (!h) throw JsonError('bad escape');
            var v = parseInt(h[2], 16) * (h[1] === '-' ? -1 : 1);
            b += String.fromCharCode(v & 0xFFFF);
            i += 4;
          } else b += e;
        } else b += c;
      }
    }
    function num() {
      var st = i;
      while (i < n && '+-0123456789.eE'.indexOf(s.charAt(i)) >= 0) i++;
      if (st === i) throw JsonError('unexpected character');
      var t = s.substring(st, i);
      if (!/^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/.test(t)) throw JsonError('bad number');
      var v = Number(t);
      if (v === 0) v = 0;   // no -0
      return v;
    }
    ws();
    var v = value(0);
    ws();
    if (i !== n) throw JsonError('trailing characters');
    return v;
  }

  function javaDouble(d) {
    var a = Math.abs(d);
    if (a >= 1e-3 && a < 1e7) { var s = String(d); return s.indexOf('.') < 0 ? s + '.0' : s; }
    var e = d.toExponential();             // "1.5e+21"
    var p = e.split('e'), m = p[0], x = parseInt(p[1], 10);
    if (m.indexOf('.') < 0) m += '.0';
    return m + 'E' + x;
  }
  function quote(s) {
    var b = '"';
    for (var k = 0; k < s.length; k++) {
      var c = s.charAt(k), cc = s.charCodeAt(k);
      if (c === '"') b += '\\"';
      else if (c === '\\') b += '\\\\';
      else if (c === '\n') b += '\\n';
      else if (c === '\r') b += '\\r';
      else if (c === '\t') b += '\\t';
      else if (cc < 0x20) b += '\\u' + ('000' + cc.toString(16)).slice(-4);
      else b += c;
    }
    return b + '"';
  }
  /** Same output as the Java Json.write (integral numbers as integers, minimal escaping). */
  function jsonWrite(v) {
    if (v === null || v === undefined) return 'null';
    if (typeof v === 'string') return quote(v);
    if (typeof v === 'boolean') return String(v);
    if (typeof v === 'number') {
      if (!isFinite(v)) return 'null';
      if (v === Math.round(v) && Math.abs(v) < 1e15) return String(v === 0 ? 0 : v);
      return javaDouble(v);
    }
    if (Array.isArray(v)) return '[' + v.map(jsonWrite).join(',') + ']';
    if (v instanceof Map) {
      var parts = [];
      v.forEach(function (val, k) { parts.push(quote(String(k)) + ':' + jsonWrite(val)); });
      return '{' + parts.join(',') + '}';
    }
    if (typeof v === 'object') return '{' + Object.keys(v).map(function (k) { return quote(k) + ':' + jsonWrite(v[k]); }).join(',') + '}';
    return quote(String(v));
  }
  /** Java Number.intValue() for a parsed JSON number. */
  function jInt(x) {
    if (x === Infinity) return 2147483647;
    if (x === -Infinity) return -2147483648;
    if (Number.isInteger(x) && Math.abs(x) < 9223372036854775807) return Number(BigInt.asIntN(32, BigInt(x)));
    var t = Math.trunc(x);
    return t > 2147483647 ? 2147483647 : t < -2147483648 ? -2147483648 : t;
  }
  function jLong(x) { return Math.trunc(x); }
  function isMap(o) { return o instanceof Map; }

  // ======================= Dates =======================
  var DAY_SHORT = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  var MONTH_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  var LONG_MIN = -9223372036854775808;
  function daysInMonth(y, m) {
    if (m === 2) return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0 ? 29 : 28;
    return m === 4 || m === 6 || m === 9 || m === 11 ? 30 : 31;
  }
  function jParseInt(t) { return /^[+-]?\d+$/.test(t) ? parseInt(t, 10) : NaN; }
  function parts(iso) {
    if (typeof iso !== 'string' || iso.length !== 10 || iso.charAt(4) !== '-' || iso.charAt(7) !== '-' || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return null;
    var y = jParseInt(iso.substring(0, 4)), m = jParseInt(iso.substring(5, 7)), d = jParseInt(iso.substring(8, 10));
    if (isNaN(y) || isNaN(m) || isNaN(d)) return null;
    if (y < 1900 || y > 2999 || m < 1 || m > 12 || d < 1 || d > daysInMonth(y, m)) return null;
    return [y, m, d];
  }
  function valid(iso) { return parts(iso) !== null; }
  function q(a, b) { return Math.trunc(a / b); }
  function toDays(iso) {
    var p = parts(iso);
    if (!p) return LONG_MIN;
    var y = p[0], m = p[1], d = p[2];
    y -= m <= 2 ? 1 : 0;
    var era = q(y >= 0 ? y : y - 399, 400);
    var yoe = y - era * 400;
    var doy = q(153 * (m + (m > 2 ? -3 : 9)) + 2, 5) + d - 1;
    var doe = yoe * 365 + q(yoe, 4) - q(yoe, 100) + doy;
    return era * 146097 + doe - 719468;
  }
  function pad(v, n) { var s = String(v); while (s.length < n) s = '0' + s; return s; }
  function fromDays(z) {
    z += 719468;
    var era = q(z >= 0 ? z : z - 146096, 146097);
    var doe = z - era * 146097;
    var yoe = q(doe - q(doe, 1460) + q(doe, 36524) - q(doe, 146096), 365);
    var y = yoe + era * 400;
    var doy = doe - (365 * yoe + q(yoe, 4) - q(yoe, 100));
    var mp = q(5 * doy + 2, 153);
    var d = doy - q(153 * mp + 2, 5) + 1;
    var m = mp < 10 ? mp + 3 : mp - 9;
    if (m <= 2) y++;
    return pad(y, 4) + '-' + pad(m, 2) + '-' + pad(d, 2);
  }
  function addDays(iso, n) { return fromDays(toDays(iso) + n); }
  function between(a, b) { return (toDays(b) - toDays(a)) | 0; }
  function dayOfWeek(iso) { return (((toDays(iso) + 3) % 7) + 7) % 7; }
  function weekStart(iso) { return addDays(iso, -dayOfWeek(iso)); }
  function week(iso) { var s = weekStart(iso), o = []; for (var i = 0; i < 7; i++) o.push(addDays(s, i)); return o; }
  function dateLabel(iso) {
    var p = parts(iso);
    if (!p) return iso == null ? '' : iso;
    return DAY_SHORT[dayOfWeek(iso)] + ' ' + p[2] + ' ' + MONTH_SHORT[p[1] - 1];
  }
  function today() { var d = new Date(); return pad(d.getFullYear(), 4) + '-' + pad(d.getMonth() + 1, 2) + '-' + pad(d.getDate(), 2); }
  var Dates = { DAY_SHORT: DAY_SHORT, MONTH_SHORT: MONTH_SHORT, valid: valid, toDays: toDays, fromDays: fromDays, addDays: addDays,
    between: between, dayOfWeek: dayOfWeek, weekStart: weekStart, week: week, label: dateLabel, today: today, LONG_MIN: LONG_MIN };

  // ======================= UseBy =======================
  var SOON_DAYS = 3, INT_MIN = -2147483648;
  var UseBy = {
    SOON_DAYS: SOON_DAYS, EXPIRED: 'expired', TODAY: 'today', SOON: 'soon', OK: 'ok',
    status: function (d) { return d < 0 ? 'expired' : d === 0 ? 'today' : d <= SOON_DAYS ? 'soon' : 'ok'; },
    label: function (d) {
      if (d < -1) return 'Past use-by (' + (-d) + ' days ago)';
      if (d === -1) return 'Past use-by (yesterday)';
      if (d === 0) return 'Use today';
      if (d === 1) return 'Use by tomorrow';
      if (d <= SOON_DAYS) return 'Use within ' + d + ' days';
      return d + ' days left';
    },
    daysLeft: function (useBy, t) { return !valid(useBy) || !valid(t) ? INT_MIN : between(t, useBy); },
    /** pairs: [[key, useBy], ...] in pantry order */
    soonKeys: function (pairs, t) {
      var out = [];
      pairs.forEach(function (p) {
        var d = UseBy.daysLeft(p[1], t);
        if (d !== INT_MIN && d >= 0 && d <= SOON_DAYS && out.indexOf(p[0]) < 0) out.push(p[0]);
      });
      return out;
    },
    count: function (r, soon) {
      var seen = new Set(), n = 0;
      r.ingredients.forEach(function (i) {
        if (i.optional || i.key === '' || seen.has(i.key)) return;
        seen.add(i.key);
        if (soon.has(i.key)) n++;
      });
      return n;
    },
    sort: function (list, soonKeys) {
      if (!soonKeys || soonKeys.length === 0) { list.sort(PR.compare); return list; }
      var soon = new PR.Matcher(soonKeys, []);
      var c = new Map();
      list.forEach(function (r) { c.set(r, UseBy.count(r.recipe, soon)); });
      function bucket(r) { return r.haveNonStaple === 0 ? 3 : r.missing.length === 0 ? 0 : r.missing.length <= 2 ? 1 : 2; }
      list.sort(function (a, b) {
        var ba = bucket(a), bb = bucket(b);
        if (ba !== bb) return ba - bb;
        if (ba <= 1) { var d = c.get(b) - c.get(a); if (d !== 0) return d; }
        return PR.compare(a, b);
      });
      return list;
    },
    useItUp: function (all, soonKeys, limit) {
      if (!soonKeys || soonKeys.length === 0) return [];
      var soon = new PR.Matcher(soonKeys, []);
      var out = all.filter(function (r) { return r.haveNonStaple > 0 && UseBy.count(r.recipe, soon) > 0; });
      UseBy.sort(out, soonKeys);
      return out.slice(0, limit);
    }
  };

  // ======================= Allergens =======================
  function wset(s) { return new Set(s.split(',')); }
  var IDS = ['peanuts', 'tree-nuts', 'dairy', 'eggs', 'gluten', 'soy', 'fish', 'shellfish', 'sesame'];
  var LABELS = { peanuts: 'Peanuts', 'tree-nuts': 'Tree nuts', dairy: 'Dairy', eggs: 'Eggs', gluten: 'Gluten', soy: 'Soy', fish: 'Fish', shellfish: 'Shellfish', sesame: 'Sesame',
    celery: 'Celery', mustard: 'Mustard', sulphites: 'Sulphites', lupin: 'Lupin', molluscs: 'Molluscs' };
  // v2.0 (same as Allergens.java): the other groups in the EU/UK 14, Canada and FSANZ lists
  var EXTRA_IDS = ['celery', 'mustard', 'sulphites', 'lupin', 'molluscs'], ALL_IDS = IDS.concat(EXTRA_IDS);
  var CELERY_WORDS = wset('celery,celeriac'), MUSTARD_WORDS = wset('mustard,dijon,piccalilli'), LUPIN_WORDS = wset('lupin,lupini,lupine');
  var MOLLUSC_WORDS = wset('squid,calamari,octopus,mussel,mussels,clam,clams,oyster,oysters,scallop,scallops,cockle,cockles,whelk,whelks,snail,snails,escargot,abalone,cuttlefish');
  var SULPHITE_WORDS = wset('wine,sherry,vermouth,marsala,madeira,prosecco,champagne,cider,port');
  var SULPHITE_PHRASES = ['dried apricot', 'dried fruit', 'sultana', 'raisin', 'dried cranberr', 'dried mango', 'dried fig', 'glace cherr', 'candied peel', 'mixed peel'];
  var PEANUT_WORDS = wset('peanut,peanuts,satay,groundnut,groundnuts');
  var TREE_NUT_WORDS = wset('almond,almonds,walnut,walnuts,pecan,pecans,cashew,cashews,hazelnut,hazelnuts,pistachio,pistachios,macadamia,macadamias,brazil,nut,nuts,praline,marzipan,frangipane,amaretti,pesto,nutella,gianduja,chestnut,chestnuts');
  var EGG_WORDS = wset('egg,eggs,yolk,yolks,mayonnaise,mayo,meringue,aioli,custard,eggnog,hollandaise,bearnaise,albumen,brioche,ladyfinger,ladyfingers,carbonara');
  var NOT_EGG = ['eggplant', 'vegan mayonnaise', 'egg free', 'eggless', 'custard powder', 'custard apple'];
  var SOY_WORDS = wset('soy,soya,tofu,edamame,miso,tempeh,tamari,teriyaki,hoisin,kecap');
  var FISH_WORDS = wset('fish,salmon,tuna,cod,haddock,anchovy,anchovies,sardine,sardines,mackerel,trout,tilapia,halibut,pollock,hake,monkfish,swordfish,snapper,bass,herring,kipper,kippers,pilchard,pilchards,plaice,sole,bream,carp,catfish,whitebait,caviar,roe,bonito,dashi,worcestershire,caesar,bottarga,surimi,eel');
  var SHELLFISH_WORDS = wset('prawn,prawns,shrimp,shrimps,crab,crabs,lobster,lobsters,crayfish,langoustine,langoustines,scallop,scallops,mussel,mussels,clam,clams,oyster,oysters,squid,calamari,octopus,cockle,cockles,whelk,whelks,scampi');
  var NOT_SHELLFISH = ['oyster mushroom', 'oyster mushrooms', 'crab apple', 'crabapple'];
  var SESAME_WORDS = wset("sesame,tahini,halva,halvah,gomasio,furikake,za'atar,zaatar,hummus,houmous");
  // v1.8 (same as Allergens.java): fewer gluten false positives, 'check label' gluten, name-based flags
  var NOT_GLUTEN = wset('spaghetti squash,ginger ale,pizza sauce,everything bagel seasoning');
  var MAYBE_GLUTEN = wset('oats,oat,oat milk,oat flour,steel oats,soba noodle,buckwheat noodle,miso,rice malt syrup,rice cereal,worcestershire sauce,fish cake,crispy fried onion,fried onion');
  var MAYBE_GLUTEN_WORDS = wset('oats,oat,soba,miso,worcestershire');
  var FREE = { peanuts: ['peanut', 'nut'], 'tree-nuts': ['nut', 'tree nut'], dairy: ['dairy', 'milk'], eggs: ['egg'], gluten: ['gluten'], soy: ['soy'], fish: ['fish'], shellfish: ['shellfish'], sesame: ['sesame'],
    celery: ['celery'], mustard: ['mustard'], sulphites: ['sulphite', 'sulfite'], lupin: ['lupin'], molluscs: ['mollusc', 'shellfish'] };
  function raw(name) {
    if (name == null) return '';
    var t = String(name).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z']+/g, ' ');
    t = PR.jtrim(t);
    var f = t.indexOf(' for the ');
    if (f >= 0) t = t.substring(0, f);
    return varietyFix(t);
  }
  function varietyFix(t) { return t.split('butter lettuce').join('lettuce').split('butterhead').join('lettuce').split('butter bean').join('lima bean'); }
  function has(id, i) {
    var rw = raw(i.name), fw = FREE[id] || [];
    for (var k = 0; k < fw.length; k++) if (rw.indexOf(fw[k] + ' free') >= 0 || rw.indexOf(fw[k] + 'free') >= 0) return false;
    if (keyHas(id, varietyFix(i.key))) return true;
    if (id === 'gluten') return rw.indexOf('fried onion') >= 0 || rw.indexOf('spring roll wrapper') >= 0 || rw.indexOf('fish cake') >= 0;
    if (rw === '' || rw === i.key) return false;
    if (id === 'dairy' && (rw.indexOf('dairy free') >= 0 || rw.indexOf('non dairy') >= 0 || rw.indexOf('vegan') >= 0 || rw.indexOf('plant based') >= 0 || rw.indexOf('plant butter') >= 0 || rw.indexOf('coconut') >= 0)) return false;
    if (id === 'eggs' && (rw.indexOf('vegan') >= 0 || rw.indexOf('egg free') >= 0 || rw.indexOf('eggless') >= 0)) return false;
    if (id === 'tree-nuts' && (rw.indexOf('chestnut mushroom') >= 0 || rw.indexOf('nut free') >= 0 || rw.indexOf('coconut') >= 0 || rw.indexOf('nutmeg') >= 0)) return false;
    if (id === 'soy' && rw.indexOf('soy free') >= 0) return false;
    return keyHas(id, rw);
  }
  function keyHasSure(key) {
    if (!keyHas('gluten', key) || MAYBE_GLUTEN.has(key)) return false;
    var w = key.split(' ');
    for (var k = 0; k < w.length; k++) if (!MAYBE_GLUTEN_WORDS.has(w[k]) && PR.hasGluten(w[k])) return true;
    return false;
  }
  function uncertain(id, i) { if (id === 'sulphites') return has(id, i); return id === 'gluten' && !keyHasSure(i.key) && has(id, i); }
  function anyWord(key, words) { var w = key.split(' '); for (var i = 0; i < w.length; i++) if (words.has(w[i])) return true; return false; }
  function phrase(key, list) { for (var i = 0; i < list.length; i++) if (key.indexOf(list[i]) >= 0) return true; return false; }
  function keyHas(id, key) {
    if (key == null || key === '') return false;
    switch (id) {
      case 'peanuts': return anyWord(key, PEANUT_WORDS);
      case 'tree-nuts':
        if (key.indexOf('pine nut') >= 0 || key.indexOf('pinenut') >= 0) return true;
        return anyWord(key, TREE_NUT_WORDS) && key.indexOf('water chestnut') < 0 && key.indexOf('nut free') < 0;
      case 'dairy': return PR.isDairy(key);
      case 'eggs': return anyWord(key, EGG_WORDS) && !phrase(key, NOT_EGG);
      case 'gluten': return PR.hasGluten(key) && !NOT_GLUTEN.has(key);
      case 'soy': return anyWord(key, SOY_WORDS) || key.indexOf('textured vegetable protein') >= 0;
      case 'fish': return anyWord(key, FISH_WORDS);
      case 'shellfish': return anyWord(key, SHELLFISH_WORDS) && !phrase(key, NOT_SHELLFISH);
      case 'sesame': return anyWord(key, SESAME_WORDS) || key.indexOf('za atar') >= 0 || key === 'everything bagel seasoning';
      case 'celery': return anyWord(key, CELERY_WORDS);
      case 'mustard': return anyWord(key, MUSTARD_WORDS);
      case 'lupin': return anyWord(key, LUPIN_WORDS);
      case 'molluscs': return (anyWord(key, MOLLUSC_WORDS) && !phrase(key, NOT_SHELLFISH)) || key.indexOf('oyster sauce') >= 0;
      case 'sulphites': return (anyWord(key, SULPHITE_WORDS) && key.indexOf('port wine cheese') < 0 && key.indexOf('wine free') < 0) || phrase(key, SULPHITE_PHRASES);
    }
    return false;
  }
  function dislikes(key, d) {
    if (!key || !d) return false;
    if (key === d) return true;
    var fk = PR.family(key);
    if (fk != null && (fk === d || fk === PR.family(d))) return true;
    return (' ' + key + ' ').indexOf(' ' + d + ' ') >= 0;
  }
  var Allergens = {
    NOTE: 'Allergen flags and hidden ingredients are matched by ingredient name only. They are not a guarantee: always check labels, and take extra care with serious allergies.',
    IDS: IDS, EXTRA_IDS: EXTRA_IDS, ALL_IDS: ALL_IDS, label: function (id) { return LABELS[id] || id; }, keyHas: keyHas, has: has, uncertain: uncertain, raw: raw, dislikes: dislikes,
    flags: function (r, ids) { return (ids || IDS).filter(function (id) { return r.ingredients.some(function (i) { return has(id, i); }); }); },
    uncertainFlags: function (r) { return Allergens.flags(r).filter(function (id) { return !r.ingredients.some(function (i) { return has(id, i) && !uncertain(id, i); }); }); },
    flagLabel: function (r, id, unsure) { return Allergens.label(id) + (unsure.indexOf(id) >= 0 ? ' (check label)' : ''); },
    dislikeKey: function (t) { return PR.normalize(t == null ? '' : t); },
    reasons: function (r, dis, avoid) {
      var out = [];
      function add(s) { if (out.indexOf(s) < 0) out.push(s); }
      if (avoid) ALL_IDS.forEach(function (id) {
        if (avoid.indexOf(id) < 0) return;
        if (r.ingredients.some(function (i) { return has(id, i); })) add('Contains ' + Allergens.label(id).toLowerCase());
      });
      if (dis) dis.forEach(function (d) {
        if (r.ingredients.some(function (i) { return !i.optional && dislikes(i.key, d); })) add('Contains ' + d);
      });
      return out;
    },
    hidden: function (r, dis, avoid) {
      var noD = !dis || dis.length === 0, noA = !avoid || avoid.length === 0;
      return !(noD && noA) && Allergens.reasons(r, dis, avoid).length > 0;
    },
    ingredientWarn: function (i, dis, avoid) {
      if (avoid) for (var a = 0; a < avoid.length; a++) if (has(avoid[a], i)) return true;
      if (dis) for (var d = 0; d < dis.length; d++) if (dislikes(i.key, dis[d])) return true;
      return false;
    }
  };

  // ======================= Amounts =======================
  var ALIAS = new Map(), FACTOR = new Map(), DIM = new Map();
  function unit(c, dim, f, aliases) { ALIAS.set(c, c); aliases.forEach(function (a) { ALIAS.set(a, c); }); DIM.set(c, dim); FACTOR.set(c, f); }
  unit('g', 'mass', 1, ['gram', 'grams', 'gr', 'grm']);
  unit('kg', 'mass', 1000, ['kilo', 'kilos', 'kilogram', 'kilograms', 'kgs']);
  unit('oz', 'mass', 28.3495, ['ounce', 'ounces']);
  unit('lb', 'mass', 453.592, ['lbs', 'pound', 'pounds']);
  unit('ml', 'volume', 1, ['millilitre', 'milliliter', 'millilitres', 'milliliters', 'mls']);
  unit('l', 'volume', 1000, ['litre', 'liter', 'litres', 'liters', 'ltr']);
  unit('tsp', 'volume', 5, ['teaspoon', 'teaspoons', 'tsps']);
  unit('tbsp', 'volume', 15, ['tablespoon', 'tablespoons', 'tbsps', 'tbs', 'tbl']);
  unit('cup', 'volume', 240, ['cups', 'c']);
  unit('fl oz', 'volume', 29.5735, ['floz', 'fluid ounce', 'fluid ounces']);
  unit('pint', 'volume', 568, ['pints', 'pt']);
  unit('', 'count', 1, ['x', 'pc', 'pcs', 'piece', 'pieces', 'each', 'whole', 'large', 'medium', 'small', 'item', 'items']);
  function endsWith(s, x) { return s.length >= x.length && s.slice(-x.length) === x; }
  function canon(u0) {
    var u = u0 == null ? '' : jtrim(String(u0)).toLowerCase().split('.').join('');
    if (ALIAS.has(u)) return ALIAS.get(u);
    if (endsWith(u, 'ches') || endsWith(u, 'shes')) return u.substring(0, u.length - 2);
    if (endsWith(u, 's') && u.length > 3 && !endsWith(u, 'ss')) return u.substring(0, u.length - 1);
    return u;
  }
  function dimension(u) { var c = canon(u); return DIM.has(c) ? DIM.get(c) : 'unit:' + c; }
  function convert(amount, from, to) {
    if (isNaN(amount)) return NaN;
    var a = canon(from), b = canon(to);
    if (a === b) return amount;
    if (dimension(a) !== dimension(b) || dimension(a).indexOf('unit:') === 0) return NaN;
    return amount * FACTOR.get(a) / FACTOR.get(b);
  }
  function amtStatus(have, hu, need, nu) {
    if (isNaN(have) || isNaN(need) || need <= 0) return 'unknown';
    var h = convert(have, hu, nu);
    if (isNaN(h)) return 'unknown';
    return h + 1e-9 >= need ? 'enough' : 'short';
  }
  function qtyFor(key, qty) {
    if (!qty || !key) return null;
    var v = qty.get(key);
    if (v != null && jtrim(v) !== '') return v;
    var f = PR.family(key);
    v = f == null ? undefined : qty.get(f);
    if (v != null && jtrim(v) !== '') return v;
    return null;
  }
  function selfUnit(hu, key) {
    var cu = canon(hu);
    return cu !== '' && dimension(cu).indexOf('unit:') === 0 && (' ' + key + ' ').indexOf(' ' + cu + ' ') >= 0 ? '' : hu;
  }
  var Amounts = {
    ENOUGH: 'enough', SHORT: 'short', UNKNOWN: 'unknown', canon: canon, dimension: dimension, convert: convert, status: amtStatus,
    parse: function (t) { return PR.parseQuantity(t == null ? '' : t); }, qtyFor: qtyFor,
    /** qty: Map key -> typed quantity text */
    check: function (r, scale, qty) {
      var out = [];
      if (!qty || qty.size === 0) return out;
      var sum = new Map(), nu = new Map(), mixed = new Set();
      r.ingredients.forEach(function (i) {
        if (i.optional || i.key === '' || isNaN(i.amount)) return;
        var v = i.amount * scale;
        if (!nu.has(i.key)) { nu.set(i.key, i.unit); sum.set(i.key, v); return; }
        var c = convert(v, i.unit, nu.get(i.key));
        if (isNaN(c)) mixed.add(i.key); else sum.set(i.key, sum.get(i.key) + c);
      });
      r.ingredients.forEach(function (i) {
        if (i.optional || i.key === '') return;
        var qt = qtyFor(i.key, qty);
        if (qt == null) return;
        var st = 'unknown';
        if (!isNaN(i.amount) && !mixed.has(i.key)) {
          var p = Amounts.parse(qt);
          st = amtStatus(p.amount, selfUnit(p.unit, i.key), sum.get(i.key), nu.get(i.key));
        }
        out.push({ ingredient: i, status: st, haveText: jtrim(qt) });
      });
      return out;
    },
    label: function (c) {
      if (c.status === 'enough') return 'You have enough (' + c.haveText + ')';
      if (c.status === 'short') return 'Not enough: you have ' + c.haveText;
      return 'You have ' + c.haveText;
    }
  };

  // ======================= MealPlan =======================
  var MAX_SERVINGS = 24;
  function entry(date, id, servings) { return { date: date, id: id, servings: Math.max(1, Math.min(MAX_SERVINGS, servings)) }; }
  function nameOf(i) {
    var n = i.name, c = n.indexOf(',');
    if (c > 0) n = n.substring(0, c);
    n = jtrim(n);
    return n === '' ? n : n.charAt(0).toUpperCase() + n.substring(1);
  }
  var MealPlan = {
    MAX_SERVINGS: MAX_SERVINGS, entry: entry,
    forWeek: function (all, ws) {
      var a = toDays(ws);
      return all.filter(function (e) { var d = toDays(e.date); return d >= a && d < a + 7; })
        .sort(function (x, y) { return x.date < y.date ? -1 : x.date > y.date ? 1 : 0; });
    },
    prune: function (all, t, keep) {
      var td = toDays(t);
      return all.filter(function (e) { var d = toDays(e.date); return d !== LONG_MIN && d >= td - keep; });
    },
    shopping: function (entries, byId, pantry, qty) {
      var items = new Map();
      entries.forEach(function (e) {
        var r = byId(e.id);
        if (!r) return;
        var scale = e.servings / Math.max(1, r.servings);
        r.ingredients.forEach(function (i) {
          if (i.optional || i.key === '') return;
          if (pantry && pantry.isStaple(i.key)) return;
          var dim = isNaN(i.amount) ? 'none' : dimension(i.unit);
          var id = i.key + '|' + dim, it = items.get(id);
          if (!it) {
            it = { key: i.key, name: nameOf(i), amount: NaN, unit: '', recipes: [], partial: false };
            if (!isNaN(i.amount)) { it.amount = i.amount * scale; it.unit = i.unit; }
            items.set(id, it);
          } else if (!isNaN(i.amount)) {
            it.amount += convert(i.amount * scale, i.unit, it.unit);
          }
          if (it.recipes.indexOf(r.title) < 0) it.recipes.push(r.title);
        });
      });
      var out = [];
      items.forEach(function (it) {
        if (pantry && pantry.has(it.key)) {
          var qt = qtyFor(it.key, qty);
          if (qt == null || isNaN(it.amount)) return;
          var p = Amounts.parse(qt);
          var have = convert(p.amount, selfUnit(p.unit, it.key), it.unit);
          if (isNaN(have) || have + 1e-9 >= it.amount) return;
          it.amount = it.amount - have;
          it.partial = true;
        }
        out.push(it);
      });
      return out;
    },
    text: function (it) { return isNaN(it.amount) ? '' : PR.formatQty(it.amount, it.unit, 1); }
  };

  function heroOrder(meals) {
    var s = PR.mealSorted(meals), out = [];
    if (s.indexOf('Dinner') >= 0) out.push('Dinner');
    if (s.indexOf('Main dish') >= 0) out.push('Main dish');
    s.forEach(function (m) { if (out.indexOf(m) < 0) out.push(m); });
    return out;
  }

  // ======================= Palette (themes) =======================
  var AA = 4.5, AA_LARGE = 3.0, MAX_CUSTOM = 12;
  function base(id, name, bg, surface, primary, accent, text, group) { return { id: id, name: name, group: group || '', bg: bg & 0xFFFFFF, surface: surface & 0xFFFFFF, primary: primary & 0xFFFFFF, accent: accent & 0xFFFFFF, text: text & 0xFFFFFF }; }
  function withColors(b, bg, surface, primary, accent, text) { return base(b.id, b.name, bg, surface, primary, accent, text, b.group); }
  var GROUPS = ['Light', 'Dark', 'Colorful', 'Pastel', 'High contrast', 'Warm', 'Cool', 'Earthy', 'AMOLED black', 'Paper & sepia', 'Editor classics', 'Game day'];
  // first 17 ids are the v1.5 ones (saved choices and backups keep resolving); generated from Palette.java
  var PRESETS = [
    base('light', 'Light', 0xF5F7F2, 0xFFFFFF, 0x2E9E5B, 0xD9822B, 0x1E2A22, 'Light'),
    base('dark', 'Dark', 0x121814, 0x1C2620, 0x5BC98A, 0xF0A35E, 0xE6EEE8, 'Dark'),
    base('ocean', 'Ocean', 0xEEF5FA, 0xFFFFFF, 0x1F6FB2, 0xE07A3F, 0x13263A, 'Cool'),
    base('midnight', 'Midnight', 0x0E1424, 0x182036, 0x7AA2FF, 0xFFB86B, 0xE4E9F5, 'Dark'),
    base('sunset', 'Sunset', 0xFFF4EC, 0xFFFFFF, 0xC2492C, 0x7B4FA0, 0x3A1E16, 'Warm'),
    base('lavender', 'Lavender', 0xF5F1FB, 0xFFFFFF, 0x6A4BB5, 0xD6467A, 0x241B35, 'Pastel'),
    base('rose', 'Rose', 0xFDF1F4, 0xFFFFFF, 0xB0305A, 0x2F8F83, 0x3A1822, 'Warm'),
    base('espresso', 'Espresso', 0x1B1411, 0x2A201B, 0xD9A066, 0x8FC1A9, 0xF2E8DF, 'Earthy'),
    base('slate', 'Slate', 0xEEF1F4, 0xFFFFFF, 0x3F5873, 0xC8553D, 0x1D2731, 'Cool'),
    base('forest', 'Forest night', 0x0F1C15, 0x17291F, 0x7FD39B, 0xE8C15A, 0xE3F1E7, 'Dark'),
    base('citrus', 'Citrus', 0xFFF3C4, 0xFFFFFF, 0x8A9A10, 0xE2711D, 0x2B2A10, 'Colorful'),
    base('mint', 'Mint', 0xEFFAF6, 0xFFFFFF, 0x0F8A74, 0xE65A6B, 0x12302A, 'Pastel'),
    base('plum', 'Plum', 0x1A1020, 0x281A30, 0xD08CE8, 0x7FD1C7, 0xF1E6F5, 'Dark'),
    base('contrast', 'High contrast', 0xFFFFFF, 0xFFFFFF, 0x0B5D1E, 0x9A3412, 0x000000, 'High contrast'),
    base('contrast-dark', 'High contrast dark', 0x000000, 0x0A0A0A, 0x7CFC9A, 0xFFD166, 0xFFFFFF, 'High contrast'),
    base('terracotta', 'Terracotta', 0xF3E3D3, 0xFFFAF5, 0x8E3B1E, 0x3E7C59, 0x2E1A12, 'Earthy'),
    base('arctic', 'Arctic night', 0x2E3440, 0x3B4252, 0x88C0D0, 0xEBCB8B, 0xECEFF4, 'Editor classics'),
    base('cloud', 'Cloud', 0xF3F5F9, 0xFFFFFF, 0x3D63DD, 0xE8833A, 0x1B2230, 'Light'),
    base('daylight', 'Daylight', 0xFAFAF7, 0xFFFFFF, 0x3F3FBF, 0xD9534F, 0x1F2526, 'Light'),
    base('sage', 'Sage leaf', 0xF1F4EE, 0xFBFCF9, 0x5B7F4A, 0xB5654A, 0x232B1F, 'Light'),
    base('porcelain', 'Porcelain', 0xF6F4F1, 0xFFFFFF, 0x4A5A8C, 0xC07A2C, 0x222326, 'Light'),
    base('graphite', 'Graphite', 0x18191B, 0x232528, 0x8AB4F8, 0xF6AE5B, 0xE8EAED, 'Dark'),
    base('deepsea', 'Deep sea', 0x0B1A22, 0x132833, 0x4FD1C5, 0xFF9F7A, 0xE2F1F4, 'Dark'),
    base('tropical', 'Tropical', 0xE9FBF6, 0xFFFFFF, 0x00897B, 0xF4511E, 0x0F2E2A, 'Colorful'),
    base('berrypop', 'Berry pop', 0xFFF0F6, 0xFFFFFF, 0xC2185B, 0x5E35B1, 0x2D0F1E, 'Colorful'),
    base('lagoon', 'Lagoon', 0xE8F7FF, 0xFFFFFF, 0x0277BD, 0xEF6C00, 0x0B2233, 'Colorful'),
    base('mango', 'Mango', 0xFFF6E5, 0xFFFFFF, 0xD26E00, 0x2E7D32, 0x2E1E05, 'Colorful'),
    base('peach', 'Peach blossom', 0xFFF3EE, 0xFFFFFF, 0xD8674A, 0x6B8F71, 0x3A2420, 'Pastel'),
    base('cottoncandy', 'Cotton candy', 0xFCF1FA, 0xFFFFFF, 0xB4479B, 0x3F8FC4, 0x33182E, 'Pastel'),
    base('skypastel', 'Sky pastel', 0xEEF6FD, 0xFFFFFF, 0x4C7FC0, 0xE88B9A, 0x1C2838, 'Pastel'),
    base('contrast-yellow', 'High contrast amber', 0x000000, 0x000000, 0xFFD400, 0x7FDBFF, 0xFFFFFF, 'High contrast'),
    base('amber', 'Amber glow', 0xFFF7E8, 0xFFFFFF, 0xB45309, 0x0F766E, 0x2B1B07, 'Warm'),
    base('cinnamon', 'Cinnamon', 0x2A1A14, 0x38241C, 0xF0915A, 0xE7C66B, 0xF7E9E1, 'Warm'),
    base('glacier', 'Glacier', 0xEDF7F9, 0xFFFFFF, 0x167A8F, 0x9A4FC6, 0x102A30, 'Cool'),
    base('steel', 'Steel', 0x1C232B, 0x26303A, 0x6CC4E8, 0xFF8F70, 0xE6EDF3, 'Cool'),
    base('moss', 'Moss', 0x1A1F14, 0x252C1D, 0xA8C46A, 0xE0A458, 0xEDF0E4, 'Earthy'),
    base('clay', 'Clay', 0xF4EBE4, 0xFFFBF7, 0x9C4F35, 0x5A7A4E, 0x2D1E17, 'Earthy'),
    base('olive', 'Olive grove', 0xF1F0E4, 0xFCFBF3, 0x6B6B1E, 0xA0522D, 0x26261A, 'Earthy'),
    base('black', 'Pure black', 0x000000, 0x0D0D0D, 0x5EEAD4, 0xFF9E80, 0xF2F2F2, 'AMOLED black'),
    base('black-blue', 'Black sapphire', 0x000000, 0x0B0D12, 0x82AAFF, 0xFFCB6B, 0xEEF1F8, 'AMOLED black'),
    base('black-ember', 'Black ember', 0x000000, 0x100B0A, 0xFF8A65, 0xFFD166, 0xF5ECE8, 'AMOLED black'),
    base('black-violet', 'Black violet', 0x000000, 0x0E0B12, 0xC59BFF, 0x7FE0C7, 0xF1ECF8, 'AMOLED black'),
    base('paper', 'Paper', 0xF7F4EC, 0xFFFDF7, 0x5C4B3A, 0x2F6F8F, 0x2A251E, 'Paper & sepia'),
    base('sepia', 'Sepia', 0xF1E7D0, 0xF8F1E0, 0x7A4E2D, 0x8A3D62, 0x3B2A1A, 'Paper & sepia'),
    base('sepia-night', 'Night sepia', 0x1F1A14, 0x2B241B, 0xE0B67A, 0xE57F6A, 0xEFE3CF, 'Paper & sepia'),
    base('citrine-light', 'Citrine light', 0xFDF6E3, 0xFFFBEF, 0x1F6E9C, 0xB8460F, 0x073642, 'Editor classics'),
    base('citrine-dark', 'Citrine dark', 0x002B36, 0x073642, 0x5FB3E8, 0xE3A33B, 0xEEE8D5, 'Editor classics'),
    base('frostbite', 'Frostbite', 0xECEFF4, 0xFFFFFF, 0x4C6A92, 0xBF616A, 0x2E3440, 'Editor classics'),
    base('nightshade', 'Nightshade', 0x22212C, 0x2E2D3D, 0xBD93F9, 0xFF79C6, 0xF8F8F2, 'Editor classics'),
    base('gd-gridiron-gold', 'Gridiron Gold', 0xF7F5EF, 0xFFFFFF, 0x7D6B41, 0x101820, 0x201C11, 'Game day'),
    base('gd-gridiron-gold-dark', 'Gridiron Gold dark', 0x15120B, 0x241F13, 0xB3995D, 0x6A6F74, 0xF7F5EF, 'Game day'),
    base('gd-harbor-orange-and-navy', 'Harbor Orange & Navy', 0xFFEDE8, 0xFFFFFF, 0xC33D10, 0x002244, 0x2D0E04, 'Game day'),
    base('gd-harbor-orange-and-navy-dark', 'Harbor Orange & Navy dark', 0x1E0902, 0x321004, 0xFB4F14, 0x596F85, 0xFFEDE8, 'Game day'),
    base('gd-crimson-and-cream', 'Crimson & Cream', 0xF5E8EB, 0xFFFFFF, 0x9E1B32, 0x918A7E, 0x1C0509, 'Game day'),
    base('gd-crimson-and-cream-dark', 'Crimson & Cream dark', 0x130306, 0x20050A, 0xBE6575, 0xF1E6D2, 0xF5E8EB, 'Game day'),
    base('gd-deep-field-green', 'Deep Field Green', 0xE6EDEE, 0xFFFFFF, 0x004C54, 0x888E90, 0x000E0F, 'Game day'),
    base('gd-royal-and-gold', 'Royal & Gold', 0xE6EBF6, 0xFFFFFF, 0x0033A0, 0xAC861E, 0x00091D, 'Game day'),
    base('gd-purple-and-gold', 'Purple & Gold', 0xEEE9F3, 0xFFFFFF, 0x552583, 0xB7861C, 0x0F0718, 'Game day'),
    base('gd-teal-and-black', 'Teal & Black', 0xE6F0F2, 0xFFFFFF, 0x006778, 0x101820, 0x001316, 'Game day'),
    base('gd-teal-and-black-dark', 'Teal & Black dark', 0x000C0E, 0x001518, 0x398996, 0x64696E, 0xE6F0F2, 'Game day'),
    base('gd-scarlet-and-gray', 'Scarlet & Gray', 0xF8E6E6, 0xFFFFFF, 0xBB0000, 0x666666, 0x220000, 'Game day'),
    base('gd-scarlet-and-gray-dark', 'Scarlet & Gray dark', 0x160000, 0x250000, 0xD15353, 0x666666, 0xF8E6E6, 'Game day'),
    base('gd-burnt-orange', 'Burnt Orange', 0xF9EEE6, 0xFFFFFF, 0xAC4E00, 0x333F48, 0x221000, 'Game day'),
    base('gd-burnt-orange-dark', 'Burnt Orange dark', 0x170A00, 0x261100, 0xC5681A, 0x616A71, 0xF9EEE6, 'Game day'),
    base('gd-maroon-and-white', 'Maroon & White', 0xF2E6E8, 0xFFFFFF, 0x7A0019, 0xAC8A22, 0x160005, 'Game day'),
    base('gd-forest-and-gold', 'Forest & Gold', 0xE9EBEA, 0xFFFFFF, 0x203731, 0xB9840D, 0x060A09, 'Game day'),
    base('gd-cardinal-red', 'Cardinal Red', 0xF5E9EC, 0xFFFFFF, 0x97233F, 0xB9840D, 0x1B060B, 'Game day'),
    base('gd-cardinal-red-dark', 'Cardinal Red dark', 0x120408, 0x1E070D, 0xB66579, 0xFFB612, 0xF5E9EC, 'Game day'),
    base('gd-powder-blue-and-gold', 'Powder Blue & Gold', 0xE6F2F9, 0xFFFFFF, 0x0070AD, 0xB3880A, 0x001724, 'Game day'),
    base('gd-powder-blue-and-gold-dark', 'Powder Blue & Gold dark', 0x000F18, 0x001A28, 0x138ACA, 0xFFC20E, 0xE6F2F9, 'Game day'),
    base('gd-silver-and-black', 'Silver & Black', 0xEFF0F1, 0xFFFFFF, 0x5F6A72, 0x000000, 0x111315, 'Game day'),
    base('gd-silver-and-black-dark', 'Silver & Black dark', 0x0B0D0E, 0x131517, 0x7B848B, 0x6C6C6C, 0xEFF0F1, 'Game day'),
    base('gd-navy-and-red', 'Navy & Red', 0xE7E9EC, 0xFFFFFF, 0x0C2340, 0xC8102E, 0x02060C, 'Game day'),
    base('gd-kelly-green', 'Kelly Green', 0xE6F2EB, 0xFFFFFF, 0x007A33, 0xAC8B4D, 0x001609, 'Game day'),
    base('gd-kelly-green-dark', 'Kelly Green dark', 0x000F06, 0x00180A, 0x2D9157, 0xBA9653, 0xE6F2EB, 'Game day'),
    base('gd-brick-and-navy', 'Brick & Navy', 0xF6E8EA, 0xFFFFFF, 0xA6192E, 0x13274F, 0x1E0508, 'Game day'),
    base('gd-brick-and-navy-dark', 'Brick & Navy dark', 0x140306, 0x210509, 0xC15E6D, 0x5A6884, 0xF6E8EA, 'Game day'),
    base('gd-garnet-and-gold', 'Garnet & Gold', 0xF2EAEC, 0xFFFFFF, 0x782F40, 0x9B8A66, 0x16080C, 'Game day'),
    base('gd-orange-and-blue', 'Orange & Blue', 0xFDF1E9, 0xFFFFFF, 0xA85619, 0x0C2C84, 0x2A1506, 'Game day'),
    base('gd-orange-and-blue-dark', 'Orange & Blue dark', 0x1C0E04, 0x2E1807, 0xE87722, 0x6F8FD8, 0xFDF1E9, 'Game day'),
    base('gd-sky-and-navy', 'Sky & Navy', 0xF0F7FC, 0xFFFFFF, 0x467094, 0x041E42, 0x131F29, 'Game day'),
    base('gd-sky-and-navy-dark', 'Sky & Navy dark', 0x0D151B, 0x16222E, 0x6CACE4, 0x627289, 0xF0F7FC, 'Game day'),
    base('gd-ice-blue-and-navy', 'Ice Blue & Navy', 0xE6E9EF, 0xFFFFFF, 0x00205B, 0x67929E, 0x000610, 'Game day'),
    base('gd-pitch-green', 'Pitch Green', 0xEBF2EC, 0xFFFFFF, 0x377741, 0x8C8B85, 0x0A170C, 'Game day'),
    base('gd-pitch-green-dark', 'Pitch Green dark', 0x070F08, 0x0C190E, 0x589160, 0xF4F1E8, 0xEBF2EC, 'Game day'),
    base('gd-steel-and-gold', 'Steel & Gold', 0xEAECEE, 0xFFFFFF, 0x3E4C59, 0xC9A227, 0x080B0E, 'Game day'),
    base('gd-hornet-teal-and-purple', 'Hornet Teal & Purple', 0xE6F2F4, 0xFFFFFF, 0x007589, 0x1D1160, 0x001619, 'Game day'),
    base('gd-lakefront-navy-and-orange', 'Lakefront Navy & Orange', 0xE7E8EA, 0xFFFFFF, 0x0B162A, 0xC83803, 0x020408, 'Game day'),
    base('gd-copper-and-black', 'Copper & Black', 0xF8F1EB, 0xFFFFFF, 0xA34E24, 0x4A4A4A, 0x211509, 'Game day'),
    base('gd-copper-and-black-dark', 'Copper & Black dark', 0x160E06, 0x25170A, 0xBA7738, 0x9A9A9A, 0xF8F1EB, 'Game day'),
    base('gd-cobalt-and-silver', 'Cobalt & Silver', 0xE6EDF7, 0xFFFFFF, 0x0047AB, 0x8A9193, 0x000D1F, 'Game day'),
    base('gd-emerald-and-white', 'Emerald & White', 0xE6F3EC, 0xFFFFFF, 0x007D3A, 0x8C8C8C, 0x00180B, 'Game day'),
    base('gd-emerald-and-white-dark', 'Emerald & White dark', 0x001007, 0x001A0C, 0x209355, 0xFFFFFF, 0xE6F3EC, 'Game day'),
    base('gd-plum-and-teal', 'Plum & Teal', 0xEFEAF3, 0xFFFFFF, 0x5A2D82, 0x009FA9, 0x100817, 'Game day'),
    base('gd-blaze-orange-and-black', 'Blaze Orange & Black', 0xFEEFE9, 0xFFFFFF, 0xBB461A, 0x111111, 0x2B1006, 'Game day'),
    base('gd-victory-red-and-royal', 'Victory Red & Royal', 0xFAE7EC, 0xFFFFFF, 0xCE1141, 0x1D428A, 0x25030C, 'Game day'),
    base('gd-victory-red-and-royal-dark', 'Victory Red & Royal dark', 0x190208, 0x29030D, 0xDA4D71, 0x4A68A1, 0xFAE7EC, 'Game day'),
    base('gd-frozen-pond-dark', 'Frozen Pond dark', 0x09121A, 0x0F1D2C, 0x4B92DB, 0x6A7176, 0xEDF4FB, 'Game day'),
    base('gd-backcourt-blue-and-yellow', 'Backcourt Blue & Yellow', 0xE8ECF3, 0xFFFFFF, 0x1D428A, 0xC9A000, 0x050C19, 'Game day'),
    base('gd-bayou-gold-and-black-dark', 'Bayou Gold & Black dark', 0x191711, 0x2A261C, 0xD3BC8D, 0x707479, 0xFBF8F4, 'Game day'),
    base('gd-seafoam-and-navy', 'Seafoam & Navy', 0xF0F9EA, 0xFFFFFF, 0x447C1A, 0x002244, 0x132207, 'Game day'),
    base('gd-seafoam-and-navy-dark', 'Seafoam & Navy dark', 0x0D1705, 0x152608, 0x69BE28, 0x60758A, 0xF0F9EA, 'Game day'),
    base('gd-wine-and-gold', 'Wine & Gold', 0xF3E6EB, 0xFFFFFF, 0x860038, 0xC8961E, 0x18000A, 'Game day'),
    base('gd-midnight-and-lime', 'Midnight & Lime', 0xE9ECEF, 0xFFFFFF, 0x10263F, 0x639D1A, 0x02060C, 'Game day'),
    base('gd-cherry-and-black', 'Cherry & Black', 0xFAE7EA, 0xFFFFFF, 0xC8102E, 0x000000, 0x240308, 'Game day'),
    base('gd-sunflower-and-navy', 'Sunflower & Navy', 0xFFF8E7, 0xFFFFFF, 0x93690A, 0x13294B, 0x2E2103, 'Game day'),
    base('gd-sunflower-and-navy-dark', 'Sunflower & Navy dark', 0x1F1602, 0x332404, 0xFFB612, 0x6C798F, 0xFFF8E7, 'Game day'),
    base('gd-tiger-orange-and-purple', 'Tiger Orange & Purple', 0xFEF0E6, 0xFFFFFF, 0xB24A00, 0x522D80, 0x2C1200, 'Game day'),
    base('gd-tiger-orange-and-purple-dark', 'Tiger Orange & Purple dark', 0x1D0C00, 0x311400, 0xF56600, 0x7D62A0, 0xFEF0E6, 'Game day'),
    base('gd-columbine-and-gray', 'Columbine & Gray', 0xF5F8FB, 0xFFFFFF, 0x5D6E7F, 0x53565A, 0x1C2126, 'Game day'),
    base('gd-columbine-and-gray-dark', 'Columbine & Gray dark', 0x131619, 0x1F252A, 0x9BB8D3, 0x717477, 0xF5F8FB, 'Game day'),
    base('gd-ember-and-charcoal', 'Ember & Charcoal', 0xFBE7E7, 0xFFFFFF, 0xD00A0A, 0x34302B, 0x260202, 'Game day'),
    base('gd-spruce-and-silver', 'Spruce & Silver', 0xE8EDEB, 0xFFFFFF, 0x154734, 0xA2ABA5, 0x040D09, 'Game day'),
    base('gd-lagoon-teal-and-orange-dark', 'Lagoon Teal & Orange dark', 0x001112, 0x001C1E, 0x06919A, 0xFC4C02, 0xE6F4F5, 'Game day'),
    base('gd-mustard-and-brown', 'Mustard & Brown', 0xFFF8E8, 0xFFFFFF, 0x936A10, 0x311D00, 0x2E2105, 'Game day'),
    base('gd-bronze-and-blue-dark', 'Bronze & Blue dark', 0x110D07, 0x1C150C, 0x9A7E53, 0x7C8FB5, 0xF4F0EB, 'Game day'),
    base('gd-twilight-purple-and-black', 'Twilight Purple & Black', 0xEDE9F3, 0xFFFFFF, 0x4F2683, 0x000000, 0x0E0718, 'Game day'),
    base('gd-electric-blue-and-red', 'Electric Blue & Red', 0xE6F1F8, 0xFFFFFF, 0x0070AD, 0xD6001C, 0x001521, 'Game day'),
    base('gd-electric-blue-and-red-dark', 'Electric Blue & Red dark', 0x000E16, 0x001824, 0x268BC1, 0xD6001C, 0xE6F1F8, 'Game day'),
    base('gd-clay-and-cream', 'Clay & Cream', 0xF6EDEB, 0xFFFFFF, 0xA4463A, 0xB0A48C, 0x1E0D0A, 'Game day'),
    base('gd-vintage-mint-and-black', 'Vintage Mint & Black', 0xECF8F3, 0xFFFFFF, 0x2A7A5C, 0x000000, 0x0B2019, 'Game day'),
    base('gd-vintage-mint-and-black-dark', 'Vintage Mint & Black dark', 0x071610, 0x0C241B, 0x3EB489, 0x737373, 0xECF8F3, 'Game day'),
    base('gd-pine-and-orange', 'Pine & Orange', 0xE6ECE8, 0xFFFFFF, 0x01411C, 0xDC6E32, 0x000C05, 'Game day'),
    base('gd-rose-and-gold', 'Rose & Gold', 0xF8ECF1, 0xFFFFFF, 0xB0426B, 0xAB8A21, 0x210C14, 'Game day'),
    base('gd-rose-and-gold-dark', 'Rose & Gold dark', 0x16080D, 0x240E16, 0xC26587, 0xC9A227, 0xF8ECF1, 'Game day'),
    base('gd-lake-blue-and-silver', 'Lake Blue & Silver', 0xE9EFF4, 0xFFFFFF, 0x236192, 0xA2AAAD, 0x06111A, 'Game day'),
    base('gd-lake-blue-and-silver-dark', 'Lake Blue & Silver dark', 0x040C12, 0x07131D, 0x5585AB, 0xC4CED4, 0xE9EFF4, 'Game day'),
    base('gd-canary-and-green-dark', 'Canary & Green dark', 0x1F1900, 0x332900, 0xFFCD00, 0x5B7E71, 0xFFFAE6, 'Game day'),
    base('gd-magenta-and-teal', 'Magenta & Teal', 0xF6E7F1, 0xFFFFFF, 0xA6116E, 0x00808F, 0x1E0314, 'Game day'),
    base('gd-magenta-and-teal-dark', 'Magenta & Teal dark', 0x14020D, 0x210316, 0xC1589A, 0x00808F, 0xF6E7F1, 'Game day')
  ];
  function inGroup(g) { return PRESETS.filter(function (b) { return b.group === g; }); }
  var QUICK_ACCENTS = [0x2E9E5B, 0x0F8A74, 0x0891B2, 0x1F6FB2, 0x4F46E5, 0x7C3AED, 0xB4479B, 0xD6467A, 0xC62828, 0xE2711D, 0xB7791F, 0x8D5A3B, 0x3F5873];
  var FONT_IDS = ['small', 'normal', 'large', 'xlarge'], FONT_NAMES = ['Small', 'Normal', 'Large', 'Extra large'], FONT_SCALES = [0.9, 1.0, 1.15, 1.3];
  var CORNER_IDS = ['sharp', 'normal', 'round'], CORNER_NAMES = ['Sharp', 'Normal', 'Round'], CORNER_SCALES = [0.25, 1.0, 1.6];
  function scaleOf(ids, v, id) { var k = ids.indexOf(id); return k >= 0 ? v[k] : 1.0; }
  function fontScale(id) { return scaleOf(FONT_IDS, FONT_SCALES, id); }
  function cornerScale(id) { return scaleOf(CORNER_IDS, CORNER_SCALES, id); }
  function randomPreset(current, r) {
    var pool = PRESETS.filter(function (b) { return b.id !== current; });
    return pool[Math.floor(Math.max(0, Math.min(0.999999, r)) * pool.length)].id;
  }
  // v1.8: renamed theme ids; old saved ids still resolve
  var RENAMED = { 'solar-light': 'citrine-light', 'solar-dark': 'citrine-dark', 'fjord': 'frostbite' };
  function migrateId(id) { return RENAMED[id] || id; }
  function preset(id) { id = migrateId(id); for (var i = 0; i < PRESETS.length; i++) if (PRESETS[i].id === id) return PRESETS[i]; return null; }
  function channel(c) { var s = c / 255; return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4); }
  function luminance(rgb) { return 0.2126 * channel((rgb >> 16) & 255) + 0.7152 * channel((rgb >> 8) & 255) + 0.0722 * channel(rgb & 255); }
  function contrast(a, b) { var la = luminance(a), lb = luminance(b); return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05); }
  function jround(v) { return Math.floor(v + 0.5); }
  function mix(a, b, t) {
    var r = jround(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
    var g = jround(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
    var bl = jround((a & 255) + ((b & 255) - (a & 255)) * t);
    return (r << 16) | (g << 8) | bl;
  }
  function minContrast(fg, bgs) { var m = 99; bgs.forEach(function (b) { m = Math.min(m, contrast(fg, b)); }); return m; }
  function ensure(fg, bgs, min, toward) {
    for (var k = 0; k <= 20; k++) { var c = mix(fg, toward, k / 20); if (minContrast(c, bgs) >= min) return c; }
    return toward;
  }
  function bw(bg) { return contrast(0xFFFFFF, bg) >= contrast(0x000000, bg) ? 0xFFFFFF : 0x000000; }
  function isDark(bg) { return luminance(bg) < 0.18; }
  var COLOR_NAMES = ['bg', 'surface', 'text', 'text2', 'text3', 'line', 'primary', 'primaryText', 'tint', 'tint2', 'selected',
    'fill', 'onFill', 'onFill2', 'heroEnd', 'accent', 'accentText', 'accentTint', 'danger', 'info', 'infoTint', 'neutral'];
  function derive(b) {
    var c = { bg: b.bg, surface: b.surface, dark: isDark(b.bg) };
    var bs = [b.surface, b.bg];
    c.text = ensure(b.text, bs, AA, bw(b.surface));
    c.textAdjusted = c.text !== b.text;
    c.text2 = ensure(mix(c.text, b.surface, 0.38), bs, AA, c.text);
    c.text3 = ensure(mix(c.text, b.surface, 0.58), [b.surface, b.bg, mix(b.primary, b.surface, 0.94)], AA, c.text); // v2.0: hints are small text, so AA (also on selected tiles)
    c.line = mix(c.text, b.surface, 0.86);
    c.neutral = mix(c.text, b.surface, 0.93);
    c.primary = b.primary;
    c.tint = mix(b.primary, b.surface, 0.84);
    c.tint2 = mix(b.primary, b.surface, 0.68);
    c.selected = mix(b.primary, b.surface, 0.94);
    c.primaryText = ensure(b.primary, [b.surface, b.bg, c.tint, c.selected], AA, c.text);
    c.ripple = b.primary;
    c.onFill = contrast(0xFFFFFF, b.primary) >= AA_LARGE ? 0xFFFFFF : 0x000000;
    var away = c.onFill === 0xFFFFFF ? 0x000000 : 0xFFFFFF;
    c.fill = ensure(b.primary, [c.onFill], AA, away);
    c.heroEnd = mix(c.fill, away, 0.25);
    c.onFill2 = ensure(mix(c.onFill, c.fill, 0.15), [c.fill, c.heroEnd], AA, c.onFill);
    c.accent = b.accent;
    c.accentTint = mix(b.accent, b.surface, 0.84);
    c.accentText = ensure(b.accent, [b.surface, b.bg, c.accentTint], AA, c.text);
    c.danger = ensure(c.dark ? 0xFF8A80 : 0xC2410C, bs, AA, c.text);
    var infoBase = c.dark ? 0x7CC4F0 : 0x2C7FB0;
    c.infoTint = mix(infoBase, b.surface, 0.86);
    c.info = ensure(infoBase, [b.surface, c.infoTint], AA, c.text);
    return c;
  }
  function colorsToMap(c) {
    var m = new Map();
    m.set('dark', c.dark);
    COLOR_NAMES.forEach(function (n) { m.set(n, hex(c[n])); });
    m.set('textAdjusted', c.textAdjusted);
    return m;
  }
  function textPairs(c) {
    return [['text on background', c.text, c.bg, AA], ['text on cards', c.text, c.surface, AA], ['secondary text', c.text2, c.surface, AA],
      ['secondary text on background', c.text2, c.bg, AA], ['hints', c.text3, c.surface, AA], ['hints on background', c.text3, c.bg, AA], ['hints on selected tiles', c.text3, c.selected, AA], ['links on cards', c.primaryText, c.surface, AA],
      ['selected chips', c.primaryText, c.tint, AA], ['button text', c.onFill, c.fill, AA], ['banner text', c.onFill2, c.heroEnd, AA],
      ['warnings', c.accentText, c.accentTint, AA], ['warnings on cards', c.accentText, c.surface, AA], ['errors', c.danger, c.surface, AA],
      ['swaps', c.info, c.infoTint, AA], ['header title on band', c.text, c.tint, AA], ['header icons on band', c.text2, c.tint, AA_LARGE],
      ['tab labels on track', c.text, c.neutral, AA]];
  }
  function ratio1(r) { var t = jround(r * 10); return Math.floor(t / 10) + '.' + (t % 10); }
  function warnings(b) {
    var w = [], t = Math.min(contrast(b.text, b.bg), contrast(b.text, b.surface));
    if (t < AA) w.push('Text contrast is ' + ratio1(t) + ':1, below the 4.5:1 AA minimum; text is darkened/lightened automatically.');
    if (contrast(b.primary, b.surface) < AA_LARGE) w.push('Primary is hard to see on cards (' + ratio1(contrast(b.primary, b.surface)) + ':1); links use an adjusted shade.');
    if (contrast(b.bg, b.surface) < 1.04 && b.bg !== b.surface) w.push('Background and surface are almost the same; cards will not stand out.');
    return w;
  }
  function hex(rgb) { return '#' + ('00000' + (rgb & 0xFFFFFF).toString(16).toUpperCase()).slice(-6); }
  function parseHex(s) {
    if (s == null) return -1;
    var t = jtrim(String(s));
    if (t.charAt(0) === '#') t = t.substring(1);
    if (t.length === 3) t = t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
    if (!/^[0-9a-fA-F]{6}$/.test(t)) return -1;
    return parseInt(t, 16);
  }
  function toHsv(rgb) {
    var r = ((rgb >> 16) & 255) / 255, g = ((rgb >> 8) & 255) / 255, b = (rgb & 255) / 255;
    var max = Math.max(r, g, b), min = Math.min(r, g, b), d = max - min, h = 0;
    if (d > 0) { if (max === r) h = 60 * (((g - b) / d) % 6); else if (max === g) h = 60 * ((b - r) / d + 2); else h = 60 * ((r - g) / d + 4); }
    if (h < 0) h += 360;
    return [h, max === 0 ? 0 : d / max, max];
  }
  function fromHsv(h, s, v) {
    h = ((h % 360) + 360) % 360;
    var c = v * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = v - c, r, g, b;
    if (h < 60) { r = c; g = x; b = 0; } else if (h < 120) { r = x; g = c; b = 0; } else if (h < 180) { r = 0; g = c; b = x; }
    else if (h < 240) { r = 0; g = x; b = c; } else if (h < 300) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
    return (jround((r + m) * 255) << 16) | (jround((g + m) * 255) << 8) | jround((b + m) * 255);
  }
  function State() { this.active = 'light'; this.custom = []; this.font = 'normal'; this.corners = 'normal'; this.accentPick = -1; }
  State.prototype.find = function (id) {
    var p = preset(id); if (p) return p;
    for (var i = 0; i < this.custom.length; i++) if (this.custom[i].id === id) return this.custom[i];
    return null;
  };
  State.prototype.resolve = function (systemDark) {
    var b = this.active === 'system' ? preset(systemDark ? 'dark' : 'light') : (this.find(this.active) || preset('light'));
    return this.accentPick >= 0 ? withColors(b, b.bg, b.surface, this.accentPick, b.accent, b.text) : b;
  };
  State.prototype.fontScale = function () { return fontScale(this.font); };
  State.prototype.cornerScale = function () { return cornerScale(this.corners); };
  State.prototype.nextCustomId = function () { var n = 1; while (this.find('custom-' + n)) n++; return 'custom-' + n; };
  State.prototype.put = function (b) {
    for (var k = 0; k < this.custom.length; k++) if (this.custom[k].id === b.id) { this.custom[k] = b; return true; }
    if (this.custom.length >= MAX_CUSTOM) return false;
    this.custom.push(b); return true;
  };
  State.prototype.remove = function (id) {
    for (var k = 0; k < this.custom.length; k++) if (this.custom[k].id === id) { this.custom.splice(k, 1); break; }
    if (id === this.active) this.active = 'light';
  };
  State.prototype.toMap = function () {
    var m = new Map();
    m.set('active', this.active);
    if (this.font !== 'normal') m.set('font', this.font);
    if (this.corners !== 'normal') m.set('corners', this.corners);
    if (this.accentPick >= 0) m.set('accent', hex(this.accentPick));
    m.set('custom', this.custom.map(function (b) {
      var o = new Map();
      o.set('id', b.id); o.set('name', b.name); o.set('bg', hex(b.bg)); o.set('surface', hex(b.surface));
      o.set('primary', hex(b.primary)); o.set('accent', hex(b.accent)); o.set('text', hex(b.text));
      return o;
    }));
    return m;
  };
  function jstr(o, max) { if (typeof o !== 'string') return ''; var v = jtrim(o); return v.length > max ? v.substring(0, max) : v; }
  function customId(id) { return /^custom-[0-9]{1,4}$/.test(id); }
  function parseState(o) {
    var s = new State();
    if (typeof o === 'string') { try { o = jsonParse(o); } catch (e) { return s; } }
    if (!isMap(o)) return s;
    var cl = o.get('custom');
    if (Array.isArray(cl)) cl.forEach(function (x) {
      if (!isMap(x) || s.custom.length >= MAX_CUSTOM) return;
      var id = jstr(x.get('id'), 20);
      if (!customId(id) || s.find(id)) return;
      var name = jstr(x.get('name'), 40);
      if (name === '') name = 'My theme';
      var v = ['bg', 'surface', 'primary', 'accent', 'text'].map(function (k) { return parseHex(jstr(x.get(k), 9)); });
      if (v.some(function (n) { return n < 0; })) return;
      s.custom.push(base(id, name, v[0], v[1], v[2], v[3], v[4]));
    });
    var f = jstr(o.get('font'), 10), co = jstr(o.get('corners'), 10);
    if (FONT_IDS.indexOf(f) >= 0) s.font = f;
    if (CORNER_IDS.indexOf(co) >= 0) s.corners = co;
    var ac = o.get('accent');
    if (typeof ac === 'string') s.accentPick = parseHex(jstr(ac, 9));
    var a = jstr(o.get('active'), 20);
    a = migrateId(a);
    s.active = a === 'system' || s.find(a) ? a : 'light';
    return s;
  }
  var Palette = {
    migrateId: migrateId, LIGHT: 'light', DARK: 'dark', SYSTEM: 'system', AA: AA, AA_LARGE: AA_LARGE, MAX_CUSTOM: MAX_CUSTOM, PRESETS: PRESETS, COLOR_NAMES: COLOR_NAMES,
    base: base, preset: preset, luminance: luminance, contrast: contrast, mix: mix, ensure: ensure, isDark: isDark, derive: derive,
    colorsToMap: colorsToMap, textPairs: textPairs, warnings: warnings, ratio: ratio1, hex: hex, parseHex: parseHex, toHsv: toHsv, fromHsv: fromHsv,
    State: State, parseState: parseState, GROUPS: GROUPS, inGroup: inGroup, QUICK_ACCENTS: QUICK_ACCENTS, FONT_IDS: FONT_IDS, FONT_NAMES: FONT_NAMES,
    FONT_SCALES: FONT_SCALES, CORNER_IDS: CORNER_IDS, CORNER_NAMES: CORNER_NAMES, CORNER_SCALES: CORNER_SCALES, fontScale: fontScale, cornerScale: cornerScale,
    randomPreset: randomPreset, withColors: withColors
  };

  // ======================= Backup =======================
  // format 2 (v1.6); format 1 files from v1.5 and earlier still import
  var APP = 'Pantry Recipes', FORMAT = 2, MAX_CHARS = 32 * 1024 * 1024, MAX_NOTE = 2000;
  var MAX_PHOTOS = 100, MAX_PHOTO_CHARS = 1500000, MAX_BARCODES = 500;
  var SETTINGS = ['servings', 'meals', 'incDesserts', 'incSides', 'diets', 'maxTime', 'mode', 'online', 'acceptedSubs',
    'currency', 'useDefaultPrices', 'deductPantry', 'leftoverDays', 'shopCost',
    // v1.7 (still format 2: older apps ignore these, older backups simply lack them)
    'units', 'weightUnit', 'showCups', 'methods',
    // v2.0 (still format 2)
    'zip', 'budget', 'budgetWeek', 'expiryReminders',
    'region', 'gasMarks', 'fanNote', 'dateFormat', 'decimalComma', 'clock24', 'weekStart', 'household',
    // v2.0 Stage 3 (still format 2)
    'holidays', 'fasts', 'holLead', 'holBanner', 'starter', 'bakeLog', 'holNotify', 'fastChips',
    // v2.0.1 medical diet guides: ticked guides and the optional per-meal limits (no secrets)
    'medGuides', 'medSodiumMg', 'medCarbsG',
    // v2.1: the user's ingredient category moves (ingredient key -> catalog category name)
    'catMoves'];
  // v2.0: allergen groups beyond the core nine (EU/UK 14, Canada, FSANZ) and regions, also defined in core20.js
  var ALLERGENS_ALL = ['peanuts', 'tree-nuts', 'dairy', 'eggs', 'gluten', 'soy', 'fish', 'shellfish', 'sesame', 'celery', 'mustard', 'sulphites', 'lupin', 'molluscs'];
  var REGIONS = ['us', 'ca', 'uk', 'eu', 'anz', 'other'], DATE_FORMATS = ['MM/DD/YYYY', 'DD/MM/YYYY', 'DD.MM.YYYY', 'YYYY-MM-DD'];
  // v2.0: extra diet ids (Diets.IDS in core20.js) and inventory locations, repeated here because Backup loads first
  var DIET_EXTRA = ['pescatarian', 'mediterranean', 'low-carb', 'keto', 'paleo', 'whole30', 'carnivore', 'low-sodium', 'high-protein', 'halal', 'kosher'];
  var LOCS = ['pantry', 'fridge', 'freezer', 'spice'];
  function newData() {
    return { appVersion: '', platform: '', exported: '', pantry: [], favorites: [], shopping: [], ratings: new Map(), plan: [],
      dislikes: [], allergens: [], settings: new Map(), recipes: [], theme: null,
      prices: new Map(), leftovers: [], cooked: [], barcodes: new Map(), photos: new Map() };
  }
  function backupToMap(d) {
    var m = new Map();
    m.set('app', APP); m.set('format', FORMAT); m.set('appVersion', d.appVersion); m.set('platform', d.platform); m.set('exported', d.exported);
    m.set('pantry', d.pantry.map(function (e) {
      var o = new Map();
      o.set('label', e.label); o.set('key', e.key); o.set('staple', !!e.staple);
      if (e.useBy) o.set('useBy', e.useBy);
      if (e.qty) o.set('qty', e.qty);
      if (e.loc) o.set('loc', e.loc);
      if (e.frozen) o.set('frozen', e.frozen);
      if (e.low) o.set('low', true);
      return o;
    }));
    m.set('favorites', d.favorites.slice());
    m.set('shopping', d.shopping.map(function (e) {
      var o = new Map();
      o.set('name', e.name);
      if (typeof e.amount === 'number' && !isNaN(e.amount)) o.set('amount', e.amount);
      if (e.unit) o.set('unit', e.unit);
      if (e.recipe) o.set('recipe', e.recipe);
      o.set('checked', !!e.checked);
      return o;
    }));
    var r = new Map();
    d.ratings.forEach(function (v, k) { var o = new Map(); o.set('stars', v.stars); if (v.note) o.set('note', v.note); r.set(k, o); });
    m.set('ratings', r);
    m.set('plan', d.plan.map(function (e) { var o = new Map(); o.set('date', e.date); o.set('id', e.id); o.set('servings', e.servings); return o; }));
    m.set('dislikes', d.dislikes.slice());
    m.set('allergens', d.allergens.slice());
    var st = new Map();
    SETTINGS.forEach(function (k) { if (d.settings.has(k)) st.set(k, d.settings.get(k)); });
    m.set('settings', st);
    if (d.theme) m.set('theme', d.theme.toMap());
    m.set('recipes', d.recipes.map(PR.recipeToMap));
    var pr = new Map();
    d.prices.forEach(function (p, k) { var o = new Map(); o.set('price', p.price); o.set('amount', p.amount); o.set('unit', p.unit); pr.set(k, o); });
    m.set('prices', pr);
    m.set('leftovers', d.leftovers.map(function (e) {
      var o = new Map(); o.set('id', e.id); o.set('recipe', e.recipe); o.set('title', e.title); o.set('portions', e.portions);
      o.set('cooked', e.cooked); o.set('useBy', e.useBy); return o;
    }));
    m.set('cooked', d.cooked.map(function (c) { var o = new Map(); o.set('date', c.date); o.set('id', c.recipe); o.set('servings', c.servings); return o; }));
    m.set('barcodes', new Map(d.barcodes));
    if (d.photos.size) m.set('photos', new Map(d.photos));
    return m;
  }
  function BackupError(msg) { var e = new Error(msg); e.backup = true; return e; }
  function list(o) { return Array.isArray(o) ? o : []; }
  /** Stage 3 (same as Backup.ids / logList in Java). */
  function s3ids(o, max) { var out = []; strings(o).forEach(function (x) { if (x.length <= 40 && /^[a-z0-9-]+$/.test(x) && out.indexOf(x) < 0 && out.length < max) out.push(x); }); return out; }
  function s3log(o, keys, max) {
    var out = [];
    o.forEach(function (e) {
      var get = e instanceof Map ? function (k) { return e.get(k); } : e && typeof e === 'object' && !Array.isArray(e) ? function (k) { return e[k]; } : null;
      if (!get) return;
      var dv = get('d');
      if (typeof dv !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(dv)) return;
      var c = new Map();
      keys.forEach(function (k) {
        var x = get(k);
        if (typeof x === 'string') c.set(k, x.length > 300 ? x.substring(0, 300) : x);
        else if (typeof x === 'number') c.set(k, Math.trunc(Math.max(0, Math.min(10000, x))));
      });
      out.push(c);
    });
    while (out.length > max) out.shift();
    return out;
  }
  function strings(o) { return list(o).filter(function (x) { return typeof x === 'string'; }); }
  /** Java Object.toString() for JSON values, as far as the web can tell (5 and 5.0 both print as "5"). */
  function jToString(o) {
    if (typeof o === 'string') return o;
    if (typeof o === 'number') return jsonWrite(o).replace(/^(-?\d+\.\d+)E/, '$1E');
    if (o === null) return 'null';
    if (Array.isArray(o)) return '[' + o.map(jToString).join(', ') + ']';
    if (isMap(o)) { var p = []; o.forEach(function (v, k) { p.push(k + '=' + jToString(v)); }); return '{' + p.join(', ') + '}'; }
    return String(o);
  }
  /** Recipe.fromMap on a parsed Map, with the same failures (non-object ingredient -> the recipe is skipped). */
  function recipeFromJsonMap(m) {
    function s(k, def) { var o = m.get(k); return o == null ? def : jToString(o); }
    function n(k, def) { var o = m.get(k); return typeof o === 'number' ? jInt(o) : def; }
    function b(k, def) { var o = m.get(k); return typeof o === 'boolean' ? o : def; }
    var plain = { id: s('id', ''), title: s('title', ''), category: s('category', ''), cuisine: s('cuisine', ''),
      prep: n('prep', -1), cook: n('cook', -1), servings: n('servings', 4), servingsKnown: b('servingsKnown', true),
      tagsInferred: b('tagsInferred', false), source: s('source', 'bundled'), image: s('image', ''), sourceUrl: s('sourceUrl', '') };
    var r = PR.recipeFromMap(plain);
    var tags = m.get('tags');
    if (Array.isArray(tags)) r.tags = tags.map(function (t) { return jToString(t); });
    var ml = m.get('meals'), meals = [];
    if (Array.isArray(ml)) ml.forEach(function (t) { var c = PR.mealCanonical(jToString(t)); if (c !== '') meals.push(c); });
    r.meals = meals.length ? PR.mealSorted(meals) : PR.inferMeals(r.category, r.title);
    var ing = m.get('ingredients');
    if (Array.isArray(ing)) ing.forEach(function (im) {
      if (!isMap(im)) throw BackupError('bad ingredient');
      var nm = im.get('name'), un = im.get('unit'), qt = im.get('qty'), op = im.get('optional');
      r.ingredients.push(PR.makeIngredient(nm == null ? '' : jToString(nm), typeof qt === 'number' ? qt : NaN, un == null ? '' : jToString(un), op === true));
    });
    var st = m.get('steps');
    if (Array.isArray(st)) r.steps = st.map(function (x) { return jToString(x); });
    return r;
  }
  function userRecipeId(id) { return (id.indexOf('m:') === 0 || id.indexOf('u:') === 0) && id.length > 2 && id.length <= 80; }
  function backupParse(json) {
    if (json == null || jtrim(json) === '') throw BackupError('The file is empty.');
    if (json.length > MAX_CHARS) throw BackupError('The file is too large to be a My Pantry Recipes backup.');
    var root;
    try { root = jsonParse(json); } catch (e) { throw BackupError('This is not a valid backup file (it is not JSON).'); }
    if (!isMap(root)) throw BackupError('This is not a My Pantry Recipes backup file.');
    var m = root;
    if (m.get('app') !== APP) throw BackupError('This is not a My Pantry Recipes backup file.');
    var f = m.get('format');
    if (typeof f !== 'number') throw BackupError('The backup has no format number.');
    var format = jInt(f);
    if (format > FORMAT) throw BackupError('This backup was made by a newer version of My Pantry Recipes. Update the app first.');
    if (format < 1) throw BackupError('Unknown backup format.');
    var d = newData();
    d.appVersion = jstr(m.get('appVersion'), 40);
    d.platform = jstr(m.get('platform'), 40);
    d.exported = jstr(m.get('exported'), 40);
    var keys = new Set();
    list(m.get('pantry')).forEach(function (e) {
      if (!isMap(e)) return;
      var p = { label: jstr(e.get('label'), 80) };
      p.key = PR.normalize(p.label);
      if (p.key === '' || keys.has(p.key)) return;
      keys.add(p.key);
      p.staple = e.get('staple') === true;
      var u = jstr(e.get('useBy'), 10);
      p.useBy = valid(u) ? u : '';
      p.qty = jstr(e.get('qty'), 40);
      var lc = jstr(e.get('loc'), 10);
      p.loc = LOCS.indexOf(lc) >= 0 ? lc : '';
      var fz = jstr(e.get('frozen'), 10);
      p.frozen = valid(fz) ? fz : '';
      p.low = e.get('low') === true;
      d.pantry.push(p);
    });
    list(m.get('favorites')).forEach(function (o) { var id = jstr(o, 80); if (id !== '' && d.favorites.indexOf(id) < 0) d.favorites.push(id); });
    var shop = list(m.get('shopping'));
    for (var si = 0; si < shop.length; si++) {
      var e = shop[si];
      if (!isMap(e)) continue;
      var x = { name: jstr(e.get('name'), 120) };
      if (x.name === '') continue;
      var a = e.get('amount');
      x.amount = typeof a === 'number' && a > 0 ? a : NaN;
      x.unit = jstr(e.get('unit'), 30);
      x.recipe = jstr(e.get('recipe'), 160);
      x.checked = e.get('checked') === true;
      d.shopping.push(x);
      if (d.shopping.length >= 1000) break;
    }
    var rt = m.get('ratings');
    if (isMap(rt)) rt.forEach(function (v, k) {
      if (!isMap(v) || k === '' || k.length > 80) return;
      var st = v.get('stars');
      var r = { stars: typeof st === 'number' ? Math.max(0, Math.min(5, jInt(st))) : 0, note: jstr(v.get('note'), MAX_NOTE) };
      if (r.stars === 0 && r.note === '') return;
      d.ratings.set(k, r);
    });
    var pl = list(m.get('plan'));
    for (var pi = 0; pi < pl.length; pi++) {
      var pe = pl[pi];
      if (!isMap(pe)) continue;
      var date = jstr(pe.get('date'), 10), id = jstr(pe.get('id'), 80);
      if (!valid(date) || id === '') continue;
      var sv = pe.get('servings');
      d.plan.push(entry(date, id, typeof sv === 'number' ? jInt(sv) : 4));
      if (d.plan.length >= 500) break;
    }
    list(m.get('dislikes')).forEach(function (o) { var k = Allergens.dislikeKey(jstr(o, 80)); if (k !== '' && d.dislikes.indexOf(k) < 0) d.dislikes.push(k); });
    var al = list(m.get('allergens'));
    ALLERGENS_ALL.forEach(function (id) { if (al.indexOf(id) >= 0) d.allergens.push(id); });
    var so = m.get('settings');
    if (isMap(so)) {
      var v;
      if (typeof (v = so.get('servings')) === 'number') d.settings.set('servings', Math.max(1, Math.min(24, jInt(v))));
      if (Array.isArray(v = so.get('meals'))) d.settings.set('meals', PR.mealSorted(strings(v)));
      if (typeof (v = so.get('incDesserts')) === 'boolean') d.settings.set('incDesserts', v);
      if (typeof (v = so.get('incSides')) === 'boolean') d.settings.set('incSides', v);
      if (Array.isArray(v = so.get('diets'))) {
        var dl = [];
        strings(v).forEach(function (x) { if ((x === 'vegetarian' || x === 'vegan' || x === 'gluten-free' || x === 'dairy-free' || DIET_EXTRA.indexOf(x) >= 0) && dl.indexOf(x) < 0) dl.push(x); });
        d.settings.set('diets', dl);
      }
      if (typeof (v = so.get('maxTime')) === 'number') { var t = jLong(v); if (t === 0 || t === 15 || t === 30 || t === 60) d.settings.set('maxTime', t); }
      if (typeof (v = so.get('mode')) === 'number') { var t2 = jLong(v); if (t2 >= 0 && t2 <= 2) d.settings.set('mode', t2); }
      if (typeof (v = so.get('online')) === 'boolean') d.settings.set('online', v);
      if (Array.isArray(v = so.get('acceptedSubs'))) {
        var a2 = [];
        strings(v).forEach(function (x) { var k = PR.normalize(x); if (k !== '' && a2.indexOf(k) < 0) a2.push(k); });
        d.settings.set('acceptedSubs', a2);
      }
    }
    if (isMap(so)) {
      var cv = so.get('currency');
      if (typeof cv === 'string') { var cc = jtrim(cv); if (cc !== '' && cc.length <= 4 && cc.indexOf('<') < 0 && cc.indexOf('>') < 0) d.settings.set('currency', cc); }
      ['useDefaultPrices', 'deductPantry', 'shopCost'].forEach(function (k) { var b = so.get(k); if (typeof b === 'boolean') d.settings.set(k, b); });
      var ld = so.get('leftoverDays');
      if (typeof ld === 'number') d.settings.set('leftoverDays', PR.Leftovers.clampDays(jInt(ld)));
      var uv = so.get('units');
      if (typeof uv === 'string' && ['original', 'us', 'metric', 'weights'].indexOf(uv) >= 0) d.settings.set('units', uv);
      var wu = so.get('weightUnit');
      if (wu === 'g' || wu === 'oz') d.settings.set('weightUnit', wu);
      if (typeof so.get('showCups') === 'boolean') d.settings.set('showCups', so.get('showCups'));
      var zv = so.get('zip');
      if (typeof zv === 'string' && /^\d{5}(-\d{4})?$/.test(zv.trim())) d.settings.set('zip', zv.trim().substring(0, 5));
      var bv = so.get('budget');
      if (typeof bv === 'number' && bv >= 0 && bv <= 1000) d.settings.set('budget', Math.round(bv * 100) / 100);
      var bw = so.get('budgetWeek');
      if (typeof bw === 'number' && bw >= 0 && bw <= 100000) d.settings.set('budgetWeek', Math.round(bw * 100) / 100);
      if (typeof so.get('expiryReminders') === 'boolean') d.settings.set('expiryReminders', so.get('expiryReminders'));
      if (REGIONS.indexOf(so.get('region')) >= 0) d.settings.set('region', so.get('region'));
      ['gasMarks', 'fanNote', 'decimalComma'].forEach(function (k) { if (typeof so.get(k) === 'boolean') d.settings.set(k, so.get(k)); });
      if (DATE_FORMATS.indexOf(so.get('dateFormat')) >= 0) d.settings.set('dateFormat', so.get('dateFormat'));
      if (typeof so.get('clock24') === 'boolean') d.settings.set('clock24', so.get('clock24'));
      if (so.get('weekStart') === 'mon' || so.get('weekStart') === 'sun') d.settings.set('weekStart', so.get('weekStart'));
      if (Array.isArray(so.get('household')) && PR.Household) d.settings.set('household', PR.Household.toList(PR.Household.parse(so.get('household'))));
      if (Array.isArray(so.get('holidays'))) d.settings.set('holidays', s3ids(so.get('holidays'), 100));
      if (Array.isArray(so.get('fasts'))) d.settings.set('fasts', s3ids(so.get('fasts'), 20));
      if (typeof so.get('holLead') === 'number') { var hl = jInt(so.get('holLead')); if (hl === 7 || hl === 14 || hl === 21) d.settings.set('holLead', hl); }
      if (typeof so.get('holBanner') === 'boolean') d.settings.set('holBanner', so.get('holBanner'));
      if (typeof so.get('holNotify') === 'boolean') d.settings.set('holNotify', so.get('holNotify'));
      if (typeof so.get('fastChips') === 'boolean') d.settings.set('fastChips', so.get('fastChips'));
      if (isMap(so.get('catMoves')) && PR.Botany) d.settings.set('catMoves', PR.Botany.sanitize(so.get('catMoves')));
      if (Array.isArray(so.get('medGuides'))) { var gl = []; so.get('medGuides').forEach(function (g) { if (['renal', 'cardiac', 'diabetic'].indexOf(g) >= 0 && gl.indexOf(g) < 0) gl.push(g); }); d.settings.set('medGuides', gl); }
      if (typeof so.get('medSodiumMg') === 'number' && !isNaN(so.get('medSodiumMg'))) d.settings.set('medSodiumMg', Math.max(0, Math.min(20000, Math.round(so.get('medSodiumMg')))));
      if (typeof so.get('medCarbsG') === 'number' && !isNaN(so.get('medCarbsG'))) d.settings.set('medCarbsG', Math.max(0, Math.min(1000, Math.round(so.get('medCarbsG')))));
      if (Array.isArray(so.get('starter'))) d.settings.set('starter', s3log(so.get('starter'), ['d', 'm', 'ratio', 'r'], 60));
      if (Array.isArray(so.get('bakeLog'))) d.settings.set('bakeLog', s3log(so.get('bakeLog'), ['d', 'what', 'note', 'stars'], 300));
      var mv = so.get('methods');
      if (Array.isArray(mv)) {
        var ms = strings(mv);
        d.settings.set('methods', ['instant-pot', 'slow-cooker', 'air-fryer', 'one-pot', 'oven', 'stovetop', 'no-cook'].filter(function (id) { return ms.indexOf(id) >= 0; }));
      }
    }
    var po = m.get('prices');
    if (isMap(po)) {
      var pk = Array.from(po.keys());
      for (var q = 0; q < pk.length; q++) {
        var k = PR.normalize(pk[q]), o = po.get(pk[q]);
        if (k === '' || !isMap(o) || d.prices.has(k)) continue;
        var pa = o.get('price'), pn = o.get('amount');
        if (typeof pa !== 'number' || typeof pn !== 'number') continue;
        var pp = PR.Cost.make(pa, pn, jstr(o.get('unit'), 8));
        if (pp) d.prices.set(k, pp);
        if (d.prices.size >= PR.Cost.MAX_PRICES) break;
      }
    }
    var lids = new Set(), lo = list(m.get('leftovers'));
    for (var li = 0; li < lo.length; li++) {
      var le = lo[li];
      if (!isMap(le)) continue;
      var lx = { id: jstr(le.get('id'), 20), recipe: jstr(le.get('recipe'), 80), title: jstr(le.get('title'), 120), cooked: jstr(le.get('cooked'), 10), useBy: jstr(le.get('useBy'), 10) };
      var lp = le.get('portions');
      lx.portions = typeof lp === 'number' ? Math.min(PR.Leftovers.MAX_PORTIONS, jInt(lp)) : 0;
      if (lx.id === '' || lids.has(lx.id) || lx.title === '' || lx.portions <= 0 || !valid(lx.cooked) || !valid(lx.useBy)) { if (lx.id !== '') lids.add(lx.id); continue; }
      lids.add(lx.id);
      d.leftovers.push(lx);
      if (d.leftovers.length >= PR.Leftovers.MAX_ENTRIES) break;
    }
    var ck = list(m.get('cooked'));
    for (var ci = 0; ci < ck.length; ci++) {
      var ce = ck[ci];
      if (!isMap(ce)) continue;
      var cn = ce.get('servings');
      var cx = { date: jstr(ce.get('date'), 10), recipe: jstr(ce.get('id'), 80), servings: typeof cn === 'number' ? Math.max(1, Math.min(72, jInt(cn))) : 1 };
      if (!valid(cx.date) || cx.recipe === '') continue;
      d.cooked.push(cx);
      if (d.cooked.length >= PR.Leftovers.MAX_LOG) break;
    }
    var bo = m.get('barcodes');
    if (isMap(bo)) {
      var bk = Array.from(bo.keys());
      for (var bi = 0; bi < bk.length; bi++) {
        var code = PR.Barcode.clean(bk[bi]), lab = jstr(bo.get(bk[bi]), 80);
        if (!PR.Barcode.valid(code) || PR.normalize(lab) === '') continue;
        d.barcodes.set(code, lab);
        if (d.barcodes.size >= MAX_BARCODES) break;
      }
    }
    var ph = m.get('photos');
    if (isMap(ph)) {
      var hk = Array.from(ph.keys());
      for (var hi = 0; hi < hk.length; hi++) {
        var hid = hk[hi], u = ph.get(hid);
        if (typeof u !== 'string' || hid === '' || hid.length > 80) continue;
        if (u.indexOf('data:image/jpeg;base64,') !== 0 || u.length > MAX_PHOTO_CHARS || !isBase64(u.substring(23))) continue;
        d.photos.set(hid, u);
        if (d.photos.size >= MAX_PHOTOS) break;
      }
    }
    if (isMap(m.get('theme'))) d.theme = parseState(m.get('theme'));
    var rid = new Set(), rl = list(m.get('recipes'));
    for (var ri = 0; ri < rl.length; ri++) {
      if (!isMap(rl[ri])) continue;
      var rec;
      try { rec = recipeFromJsonMap(rl[ri]); } catch (e2) { continue; }
      if (!userRecipeId(rec.id) || jtrim(rec.title) === '' || rec.ingredients.length === 0 || rid.has(rec.id)) continue;
      rid.add(rec.id);
      d.recipes.push(rec);
      if (d.recipes.length >= 2000) break;
    }
    return d;
  }
  function isBase64(b) {
    if (b === '' || b.length % 4 !== 0) return false;
    for (var i = 0; i < b.length; i++) {
      var c = b.charAt(i);
      if (!(/[A-Za-z0-9+\/]/.test(c) || (c === '=' && i >= b.length - 2))) return false;
    }
    return true;
  }
  function summary(d) {
    var b = [];
    function part(n, one, many) { if (n) b.push(n + ' ' + (n === 1 ? one : many)); }
    part(d.pantry.length, 'pantry item', 'pantry items');
    part(d.favorites.length, 'favorite', 'favorites');
    part(d.shopping.length, 'shopping item', 'shopping items');
    part(d.ratings.size, 'rating or note', 'ratings and notes');
    part(d.plan.length, 'planned meal', 'planned meals');
    part(d.dislikes.length + d.allergens.length, 'dislike/allergy', 'dislikes/allergies');
    part(d.recipes.length, 'saved recipe', 'saved recipes');
    if (d.theme) part(d.theme.custom.length, 'custom theme', 'custom themes');
    part(d.prices.size, 'price', 'prices');
    part(d.leftovers.length, 'leftover', 'leftovers');
    part(d.cooked.length, 'cooking log entry', 'cooking log entries');
    part(d.barcodes.size, 'saved barcode', 'saved barcodes');
    part(d.photos.size, 'photo', 'photos');
    return b.length ? b.join(', ') : 'nothing';
  }
  var Backup = { APP: APP, FORMAT: FORMAT, MAX_CHARS: MAX_CHARS, MAX_PHOTOS: MAX_PHOTOS, MAX_PHOTO_CHARS: MAX_PHOTO_CHARS, MAX_NOTE: MAX_NOTE, SETTINGS: SETTINGS, newData: newData,
    toMap: backupToMap, write: function (d) { return jsonWrite(backupToMap(d)); }, parse: backupParse, summary: summary };

  var Json = { parse: jsonParse, write: jsonWrite, jInt: jInt };
  PR.Json = Json; PR.Dates = Dates; PR.UseBy = UseBy; PR.Allergens = Allergens; PR.Amounts = Amounts; PR.MealPlan = MealPlan;
  PR.heroOrder = heroOrder; PR.Palette = Palette; PR.Backup = Backup;
})(typeof window !== 'undefined' ? window : globalThis);
