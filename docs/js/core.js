/*
 * Pantry Recipes web - core logic, ported 1:1 from the Android app's Java core
 * (Normalizer, Matcher, Quantity, DietClassifier, Substitutions, Discover, Recipe, MealDb.convert).
 * Data tables come from js/core-data.js, which is generated from the Java classes.
 * Works in browsers and in Node (tests). MIT License.
 */
(function (root) {
  'use strict';
  var D = root.PR_CORE_DATA;
  if (!D) throw new Error('core-data.js must be loaded before core.js');

  // ---------- Java-compatible string helpers ----------
  /** Java String.trim(): strips chars <= U+0020 only. */
  function jtrim(s) { return s.replace(/^[\u0000-\u0020]+|[\u0000-\u0020]+$/g, ''); }
  /** Java String.split(regex): drops trailing empty strings. */
  function jsplit(s, re) {
    var p = s.split(re);
    if (s.length === 0) return [''];
    while (p.length > 0 && p[p.length - 1] === '') p.pop();
    return p;
  }
  /** Java String.compareToIgnoreCase. */
  function cmpIC(a, b) {
    var n = Math.min(a.length, b.length);
    for (var i = 0; i < n; i++) {
      var c1 = a.charAt(i), c2 = b.charAt(i);
      if (c1 !== c2) {
        var u1 = c1.toUpperCase(), u2 = c2.toUpperCase();
        if (u1.length !== 1) u1 = c1;
        if (u2.length !== 1) u2 = c2;
        if (u1 !== u2) {
          var l1 = u1.toLowerCase(), l2 = u2.toLowerCase();
          if (l1.length !== 1) l1 = u1;
          if (l2.length !== 1) l2 = u2;
          if (l1 !== l2) return l1.charCodeAt(0) - l2.charCodeAt(0);
        }
      }
    }
    return a.length - b.length;
  }
  function eqIC(a, b) { return cmpIC(a, b) === 0; }
  function cmpStr(a, b) { return a < b ? -1 : a > b ? 1 : 0; }
  /** Java Double.parseDouble for plain decimal input; throws like Java on bad input. */
  function parseDouble(s) {
    var t = jtrim(s);
    if (!/^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?[dDfF]?$/.test(t)) throw new Error('NumberFormatException: ' + s);
    return parseFloat(t);
  }
  function toSet(arr) { var s = new Set(); for (var i = 0; i < arr.length; i++) s.add(arr[i]); return s; }
  function toMap(pairs) { var m = new Map(); for (var i = 0; i < pairs.length; i++) m.set(pairs[i][0], pairs[i][1]); return m; }

  // ---------- Normalizer ----------
  var SYN = toMap(D.syn), FAMILY = toMap(D.family), IRREGULAR = toMap(D.irregular);
  var DESCRIPTORS = toSet(D.descriptors), NO_SINGULAR = toSet(D.noSingular), MEATS_AFTER_GROUND = toSet(D.meatsAfterGround);
  var FAMILY_VALUES = new Set(); FAMILY.forEach(function (v) { FAMILY_VALUES.add(v); });
  var MARKS;
  try { MARKS = new RegExp('\\p{M}+', 'gu'); } catch (e) { MARKS = /[\u0300-\u036f\u1ab0-\u1aff\u1dc0-\u1dff\u20d0-\u20ff\ufe20-\ufe2f]+/g; }

  function singular(w) {
    if (w.length <= 3) return w;
    var irr = IRREGULAR.get(w);
    if (irr != null) return irr;
    if (NO_SINGULAR.has(w)) return w;
    var L = w.length;
    var ends = function (x) { return w.length >= x.length && w.slice(-x.length) === x; };
    if (ends('ies') && L > 4) return w.substring(0, L - 3) + 'y';
    if (ends('oes')) return w.substring(0, L - 2);
    if (ends('ches') || ends('shes') || ends('xes') || ends('sses') || ends('zes')) return w.substring(0, L - 2);
    if (ends('ss') || ends('us') || ends('is')) return w;
    if (ends('s')) return w.substring(0, L - 1);
    return w;
  }

  var normCache = new Map();
  function normalize(raw) {
    if (raw == null) return '';
    var c = normCache.get(raw);
    if (c !== undefined) return c;
    var out = normalize0(String(raw));
    if (normCache.size > 20000) normCache.clear();
    normCache.set(raw, out);
    return out;
  }
  function normalize0(raw) {
    var s = raw.normalize('NFD').replace(MARKS, '').toLowerCase();
    var comma = s.indexOf(',');
    if (comma > 0) s = s.substring(0, comma);
    s = s.replace(/\([^)]*\)/g, ' ');
    s = s.replace(/[&\-\/']/g, ' ');
    s = jtrim(s.replace(/[^a-z% ]/g, ' ').replace(/[ \t\n\x0B\f\r]+/g, ' '));
    if (s === '') return '';
    var words = s.split(' ');
    var sing = words.map(singular).join(' ');
    var hit = SYN.get(sing);
    if (hit != null) return hit;
    if (FAMILY.has(sing)) return sing;
    var t = sing.split(' ');
    var kept = [];
    for (var k = 0; k < t.length; k++) {
      var w = t[k];
      var drop = DESCRIPTORS.has(w);
      if (w === 'ground') drop = !(k + 1 < t.length && MEATS_AFTER_GROUND.has(t[k + 1]));
      if (w === 'free' && k > 0 && (t[k - 1] === 'gluten' || t[k - 1] === 'dairy')) drop = false; // v1.7: keep gluten-free / dairy-free
      if (w === 'smoked' && k + 1 < t.length && t[k + 1] === 'paprika') drop = true;
      if (!drop) kept.push(w);
    }
    var stripped = kept.length === 0 ? sing : kept.join(' ');
    hit = SYN.get(stripped);
    if (hit != null) return hit;
    return stripped;
  }
  function family(key) { var f = FAMILY.get(key); return f === undefined ? null : f; }
  function isKnown(key) { return (SYN.has(key) && SYN.get(key) === key) || FAMILY.has(key) || FAMILY_VALUES.has(key); }

  // ---------- Quantity ----------
  var WS = '[ \\t\\n\\x0B\\f\\r]';
  var LEAD = new RegExp('^' + WS + '*(\\d+' + WS + '+\\d+' + WS + '*/' + WS + '*\\d+|\\d+' + WS + '*[\\u00bc\\u00bd\\u00be\\u2153\\u2154]|\\d+' + WS +
    '*/' + WS + '*\\d+|\\d+(?:[.,]\\d+)?|[\\u00bc\\u00bd\\u00be\\u2153\\u2154])' + WS + '*(?:-' + WS + '*\\d+(?:[.,]\\d+)?' + WS + '*)?(.*)$');
  function vulgar(c) {
    switch (c) {
      case '\u00bc': return 0.25;
      case '\u00bd': return 0.5;
      case '\u00be': return 0.75;
      case '\u2153': return 1 / 3;
      case '\u2154': return 2 / 3;
      default: return NaN;
    }
  }
  function isDigit(c) { return c >= '0' && c <= '9'; }
  function parseNumber(s) {
    s = jtrim(s);
    try {
      if (s.length === 1 && !isDigit(s.charAt(0))) return vulgar(s.charAt(0));
      var last = s.charAt(s.length - 1);
      if (!isDigit(last)) return parseDouble(jtrim(s.substring(0, s.length - 1))) + vulgar(last);
      if (s.indexOf('/') >= 0) {
        var idx = s.search(/[ \t\n\x0B\f\r]/), sp = idx < 0 ? [s] : [s.substring(0, idx), s.substring(idx).replace(/^[ \t\n\x0B\f\r]+/, '')];
        var whole = 0, frac = s;
        if (sp.length === 2 && sp[0].indexOf('/') < 0) { whole = parseDouble(sp[0]); frac = sp[1]; }
        var nd = jsplit(frac.replace(/ /g, ''), '/');
        if (nd.length < 2) throw new Error('index');
        var d = parseDouble(nd[1]);
        if (d === 0) return NaN;
        return whole + parseDouble(nd[0]) / d;
      }
      return parseDouble(s.replace(/,/g, '.'));
    } catch (e) {
      return NaN;
    }
  }
  function parseQuantity(text) {
    if (text == null) return { amount: NaN, unit: '' };
    var t = jtrim(String(text));
    var m = LEAD.exec(t);
    if (!m) return { amount: NaN, unit: t };
    var a = parseNumber(m[1]);
    if (isNaN(a) || a <= 0) return { amount: NaN, unit: t };
    return { amount: a, unit: jtrim(m[2]) };
  }
  var GLYPH = ['', '\u00bc', '\u2153', '\u00bd', '\u2154', '\u00be', ''];
  var GVAL = [0, 0.25, 1 / 3, 0.5, 2 / 3, 0.75, 1];
  function jround(v) { return Math.floor(v + 0.5); }
  function number(v, unit) {
    if (isNaN(v)) return '';
    var u = unit == null ? '' : unit.toLowerCase();
    var metric = u === 'g' || u === 'ml' || u.indexOf('gram') === 0 || u === 'kg' || u === 'l';
    if (metric && v >= 10) {
      var r = v >= 100 ? jround(v / 5) * 5 : jround(v);
      return String(Math.trunc(r));
    }
    if (v >= 10) return String(jround(v));
    var whole = Math.floor(v);
    var frac = v - whole;
    var best = 0, bd = 9;
    for (var k = 0; k < GVAL.length; k++) {
      var d = Math.abs(frac - GVAL[k]);
      if (d < bd) { bd = d; best = k; }
    }
    if (bd > 0.07 && v < 3) {
      var r1 = jround(v * 10) / 10;
      return r1 === Math.round(r1) ? String(Math.trunc(r1)) : String(r1);
    }
    if (best === 6) { whole += 1; best = 0; }
    if (whole === 0 && best === 0) return '\u00bc';
    return (whole > 0 ? String(whole) : '') + GLYPH[best];
  }
  var PLURALS = toSet(D.plurals);
  function formatQty(amount, unit, scale) {
    if (amount == null || isNaN(amount)) return unit == null ? '' : unit;
    var n = number(amount * scale, unit);
    if (unit == null || unit === '') return n;
    var v = amount * scale, u = unit;
    if (v > 1.01 && PLURALS.has(u)) u = (/(ch|sh)$/.test(u)) ? u + 'es' : u + 's';
    return n + ' ' + u;
  }

  // ---------- Recipe / Ingredient ----------
  var VEGETARIAN = 'vegetarian', VEGAN = 'vegan', GLUTEN_FREE = 'gluten-free', DAIRY_FREE = 'dairy-free';
  function makeIngredient(name, amount, unit, optional) {
    name = name == null ? '' : String(name);
    return { name: name, key: normalize(name), amount: amount == null ? NaN : amount, unit: unit == null ? '' : String(unit), optional: !!optional };
  }
  function str(m, k, def) { var o = m[k]; return o == null ? def : String(o); }
  function int(m, k, def) { var o = m[k]; return typeof o === 'number' ? Math.trunc(o) : def; }
  function bool(m, k, def) { var o = m[k]; return typeof o === 'boolean' ? o : def; }
  // ---------- Meal types (mirrors core/MealType.java) ----------
  var BREAKFAST = 'Breakfast', BRUNCH = 'Brunch', LUNCH = 'Lunch', DINNER = 'Dinner', MAIN = 'Main dish', SIDE = 'Side dish',
    APPETIZER = 'Appetizer', SNACK = 'Snack', SOUP = 'Soup', SALAD = 'Salad', DESSERT = 'Dessert', BAKED = 'Baked goods',
    DRINK = 'Drink', SAUCE = 'Sauce/Condiment';
  var MEAL_TYPES = [BREAKFAST, BRUNCH, LUNCH, DINNER, MAIN, SIDE, APPETIZER, SNACK, SOUP, SALAD, DESSERT, BAKED, DRINK, SAUCE];
  var MEAL_TIMES = [BREAKFAST, BRUNCH, LUNCH, DINNER];
  var MEAL_ALIASES = { 'main': MAIN, 'mains': MAIN, 'main course': MAIN, 'entree': MAIN, 'side': SIDE, 'sides': SIDE,
    'starter': APPETIZER, 'starters': APPETIZER, 'appetiser': APPETIZER, 'sauce': SAUCE, 'condiment': SAUCE, 'dressing': SAUCE,
    'desserts': DESSERT, 'baking': BAKED, 'bakery': BAKED, 'drinks': DRINK, 'beverage': DRINK };
  function isMealType(s) { return MEAL_TYPES.indexOf(s) >= 0; }
  function mealCanonical(s) {
    if (s == null) return '';
    var k = jtrim(String(s)).toLowerCase().replace(/[ \t\n\x0B\f\r]+/g, ' ');
    for (var i = 0; i < MEAL_TYPES.length; i++) if (MEAL_TYPES[i].toLowerCase() === k) return MEAL_TYPES[i];
    return Object.prototype.hasOwnProperty.call(MEAL_ALIASES, k) ? MEAL_ALIASES[k] : '';
  }
  function mealSorted(list) { return MEAL_TYPES.filter(function (m) { return list.indexOf(m) >= 0; }); }
  function hasWord(hay, words) {
    var h = ' ' + hay + ' ';
    for (var i = 0; i < words.length; i++) if (h.indexOf(' ' + words[i]) >= 0) return true;
    return false;
  }
  function inferMeals(category, title) {
    var c = category == null ? '' : jtrim(String(category)).toLowerCase();
    var t = title == null ? '' : jtrim(String(title).toLowerCase().replace(/[^a-z0-9]+/g, ' '));
    var out = [];
    function add() { for (var i = 0; i < arguments.length; i++) if (out.indexOf(arguments[i]) < 0) out.push(arguments[i]); }
    switch (c) {
      case 'breakfast': add(BREAKFAST, BRUNCH); break;
      case 'dessert': case 'desserts': add(DESSERT); break;
      case 'starter': case 'starters': case 'appetizer': add(APPETIZER); break;
      case 'side': case 'sides': case 'side dish': add(SIDE, DINNER); break;
      case 'soup': add(SOUP, LUNCH, DINNER); break;
      case 'salad': add(SALAD, LUNCH); break;
      case 'snack': add(SNACK); break;
      case 'baking': add(BAKED); break;
      case 'drink': case 'drinks': add(DRINK); break;
      case 'sauce': add(SAUCE); break;
      default: add(MAIN, DINNER);
    }
    if (hasWord(t, ['soup', 'broth', 'chowder', 'bisque', 'pho', 'ramen', 'gazpacho'])) add(SOUP, LUNCH);
    if (hasWord(t, ['salad', 'slaw'])) add(SALAD, LUNCH);
    if (hasWord(t, ['sandwich', 'burger', 'wrap', 'toastie', 'panini', 'quesadilla', 'taco', 'pizza', 'baguette', 'sub '])) add(LUNCH);
    if (out.indexOf(DESSERT) >= 0 && hasWord(t, ['cake', 'cookie', 'biscuit', 'brownie', 'muffin', 'scone', 'bread', 'tart', 'pie', 'loaf', 'bun', 'roll'])) add(BAKED);
    if (out.indexOf(DESSERT) >= 0 && hasWord(t, ['pancake', 'crepe', 'waffle'])) add(BREAKFAST, BRUNCH);
    return mealSorted(out);
  }
  function has(list, x) { return list.indexOf(x) >= 0; }
  function isDessertMeal(meals) { return has(meals, DESSERT) && !has(meals, MAIN); }
  function isSideMeal(meals) { return has(meals, SIDE) && !has(meals, MAIN); }
  function mealExplicit(meals, sel) {
    for (var i = 0; i < sel.length; i++) if (has(meals, sel[i]) && MEAL_TIMES.indexOf(sel[i]) < 0) return true;
    return false;
  }
  function mealPasses(meals, sel, incDesserts, incSides) {
    if (sel.length) {
      var any = false;
      for (var i = 0; i < sel.length; i++) if (has(meals, sel[i])) { any = true; break; }
      if (!any) return false;
      if (mealExplicit(meals, sel)) return true;
    }
    if (!incDesserts && isDessertMeal(meals)) return false;
    if (!incSides && isSideMeal(meals)) return false;
    return true;
  }
  function isMealFood(meals) { for (var i = 0; i < meals.length; i++) if (meals[i] !== DRINK && meals[i] !== SAUCE) return true; return false; }
  function mealPreferred(meals, sel, d, s) {
    if (!mealPasses(meals, sel, d, s)) return false;
    if (!sel.length) return has(meals, DINNER) || has(meals, MAIN);
    return true;
  }
  function mealAcceptable(meals, sel, d, s) { return mealPasses(meals, sel, d, s) && (sel.length > 0 || isMealFood(meals)); }
  function mealDbCategory(meal) {
    return meal === BREAKFAST || meal === BRUNCH ? 'Breakfast' : meal === DESSERT ? 'Dessert' : meal === APPETIZER ? 'Starter' : meal === SIDE ? 'Side' : '';
  }
  function heroQuestion(sel) {
    if (!sel.length) return 'WHAT CAN I COOK TONIGHT?';
    if (sel.length === 1) {
      switch (sel[0]) {
        case BREAKFAST: return 'WHAT CAN I COOK FOR BREAKFAST?';
        case BRUNCH: return 'WHAT CAN I COOK FOR BRUNCH?';
        case LUNCH: return 'WHAT CAN I COOK FOR LUNCH?';
        case DINNER: return 'WHAT CAN I COOK TONIGHT?';
        case DESSERT: return 'WHAT CAN I MAKE FOR DESSERT?';
        case SNACK: return 'WHAT CAN I MAKE FOR A SNACK?';
        default: return 'WHAT CAN I MAKE? \u00b7 ' + sel[0].toUpperCase();
      }
    }
    return 'WHAT CAN I COOK? \u00b7 ' + sel.length + ' MEAL TYPES';
  }

  /** v2.0 diet ids (same list as core/Diets.java IDS). */
  var DIET_IDS = ['pescatarian', 'mediterranean', 'low-carb', 'keto', 'paleo', 'whole30', 'carnivore', 'low-sodium', 'high-protein', 'halal', 'kosher'];
  function recipeFromMap(m) {
    var r = {
      id: str(m, 'id', ''), title: str(m, 'title', ''), category: str(m, 'category', ''), cuisine: str(m, 'cuisine', ''),
      prepMin: int(m, 'prep', -1), cookMin: int(m, 'cook', -1), servings: int(m, 'servings', 4),
      servingsKnown: bool(m, 'servingsKnown', true), tagsInferred: bool(m, 'tagsInferred', false),
      source: str(m, 'source', 'bundled'), image: str(m, 'image', ''), sourceUrl: str(m, 'sourceUrl', ''),
      tags: [], meals: [], ingredients: [], steps: [], diets: [], basedOn: str(m, 'basedOn', ''), userTags: [], note: str(m, 'note', '')
    };
    if (Array.isArray(m.diets)) m.diets.forEach(function (t) { var s = String(t); if (DIET_IDS.indexOf(s) >= 0 && r.diets.indexOf(s) < 0) r.diets.push(s); });
    if (Array.isArray(m.userTags)) m.userTags.forEach(function (t) { var x = jtrim(String(t)); if (x !== '' && x.length <= 30 && r.userTags.length < 12 && r.userTags.indexOf(x) < 0) r.userTags.push(x); });
    if (r.note.length > 2000) r.note = r.note.substring(0, 2000);
    if (Array.isArray(m.meals)) m.meals.forEach(function (t) { var c = mealCanonical(String(t)); if (c !== '' && r.meals.indexOf(c) < 0) r.meals.push(c); });
    r.meals = r.meals.length ? mealSorted(r.meals) : inferMeals(r.category, r.title);
    if (Array.isArray(m.tags)) m.tags.forEach(function (t) { var s = String(t); if (r.tags.indexOf(s) < 0) r.tags.push(s); });
    if (Array.isArray(m.ingredients)) m.ingredients.forEach(function (im) {
      r.ingredients.push(makeIngredient(str(im, 'name', ''), typeof im.qty === 'number' ? im.qty : NaN, str(im, 'unit', ''), bool(im, 'optional', false)));
    });
    if (Array.isArray(m.steps)) m.steps.forEach(function (s) { r.steps.push(String(s)); });
    return r;
  }
  function recipeToMap(r) {
    var m = { id: r.id, title: r.title, category: r.category, cuisine: r.cuisine, prep: r.prepMin, cook: r.cookMin, servings: r.servings };
    if (!r.servingsKnown) m.servingsKnown = false;
    m.tags = r.tags.slice();
    if (r.tagsInferred) m.tagsInferred = true;
    if (r.diets && r.diets.length) m.diets = r.diets.slice();
    m.meals = (r.meals || []).slice();
    m.ingredients = r.ingredients.map(function (i) {
      var im = { name: i.name };
      if (!isNaN(i.amount)) im.qty = i.amount;
      if (i.unit !== '') im.unit = i.unit;
      if (i.optional) im.optional = true;
      return im;
    });
    m.steps = r.steps.slice();
    m.source = r.source;
    if (r.image) m.image = r.image;
    if (r.sourceUrl) m.sourceUrl = r.sourceUrl;
    if (r.basedOn) m.basedOn = r.basedOn;
    if (r.userTags && r.userTags.length) m.userTags = r.userTags.slice();
    if (r.note) m.note = r.note;
    return m;
  }
  function parseRecipeList(json) {
    var root = typeof json === 'string' ? JSON.parse(json) : json;
    var arr = Array.isArray(root) ? root : root.recipes;
    return arr.map(recipeFromMap);
  }
  function totalMinutes(r) {
    if (r.prepMin < 0 && r.cookMin < 0) return -1;
    return Math.max(0, r.prepMin) + Math.max(0, r.cookMin);
  }
  function isOnline(r) { return r.id.indexOf('m:') === 0; }

  // ---------- DietClassifier ----------
  var DS = {};
  Object.keys(D.diet).forEach(function (k) { DS[k] = toSet(D.diet[k]); });
  var DSL = {};
  Object.keys(D.diet).forEach(function (k) { DSL[k] = D.diet[k]; });
  function anyWord(key, set) { var w = key.split(' '); for (var i = 0; i < w.length; i++) if (set.has(w[i])) return true; return false; }
  function containsPhrase(key, list) { for (var i = 0; i < list.length; i++) if (key.indexOf(list[i]) >= 0) return true; return false; }
  function isMeat(k) {
    if (k === 'fish sauce' || k === 'oyster sauce' || k === 'hot dog') return true;
    return anyWord(k, DS.MEAT_WORDS) && !containsPhrase(k, DSL.NOT_MEAT_PHRASES);
  }
  function isDairy(k) {
    if (k === 'half and half') return true;
    return anyWord(k, DS.DAIRY_WORDS) && !containsPhrase(k, DSL.NOT_DAIRY_PHRASES);
  }
  function isOtherAnimal(k) { return anyWord(k, DS.OTHER_ANIMAL) && !containsPhrase(k, DSL.NOT_ANIMAL_PHRASES); }
  function hasGluten(k) { return anyWord(k, DS.GLUTEN_WORDS) && !containsPhrase(k, DSL.NOT_GLUTEN_PHRASES); }
  function classify(ingredients) {
    var meat = false, dairy = false, animal = false, gluten = false;
    ingredients.forEach(function (i) {
      var k = i.key;
      if (isMeat(k)) meat = true;
      if (isDairy(k)) dairy = true;
      if (isOtherAnimal(k)) animal = true;
      if (hasGluten(k)) gluten = true;
    });
    var tags = [];
    if (!meat) tags.push(VEGETARIAN);
    if (!meat && !dairy && !animal) tags.push(VEGAN);
    if (!gluten) tags.push(GLUTEN_FREE);
    if (!dairy) tags.push(DAIRY_FREE);
    return tags;
  }

  // ---------- Substitutions ----------
  function Substitutions() { this.entries = []; this.categories = []; this.byKey = new Map(); this.duplicateKeys = []; this.shortLines = []; }
  function optionAvailable(o, m) {
    if (o.needs.length === 0) return false;
    for (var gi = 0; gi < o.needs.length; gi++) {
      var g = o.needs[gi], any = false;
      for (var j = 0; j < g.length; j++) if (m.has(g[j])) { any = true; break; }
      if (!any) return false;
    }
    return true;
  }
  function usesLabel(o, m) {
    var parts = [];
    for (var gi = 0; gi < o.needs.length; gi++) {
      var g = o.needs[gi], pick = o.needNames[gi][0];
      if (m) for (var j = 0; j < g.length; j++) if (m.has(g[j])) { pick = o.needNames[gi][j]; break; }
      parts.push(pick);
    }
    return parts.join(' + ');
  }
  function entrySearchText(e) {
    if (e._st == null) {
      var s = e.name + ' ' + e.category;
      e.aliases.forEach(function (a) { s += ' ' + a; });
      e.options.forEach(function (o) { s += ' ' + o.text + ' ' + (o.effect || ''); });
      e._st = s.toLowerCase();
    }
    return e._st;
  }
  function addKey(e, name) { var k = normalize(name); if (k !== '' && e.keys.indexOf(k) < 0) e.keys.push(k); }
  Substitutions.parse = function (text) {
    var s = new Substitutions(), cat = 'Other', cur = null;
    var lines = jsplit(text, '\n');
    for (var li = 0; li < lines.length; li++) {
      var line = jtrim(lines[li]);
      if (line === '' || (line.charAt(0) === '#' && line.indexOf('##') !== 0)) continue;
      if (line.indexOf('##') === 0) {
        cat = jtrim(line.substring(2));
        if (s.categories.indexOf(cat) < 0) s.categories.push(cat);
      } else if (line.charAt(0) === '*') {
        var p = jsplit(line.substring(1), '|');
        cur = { category: cat, name: jtrim(p[0]), keys: [], aliases: [], options: [], note: '' };
        addKey(cur, cur.name);
        if (p.length > 1) jsplit(p[1], ',').forEach(function (a) {
          if (jtrim(a) === '') return;
          cur.aliases.push(jtrim(a));
          addKey(cur, jtrim(a));
        });
        s.entries.push(cur);
        cur.keys.forEach(function (k) { if (!s.byKey.has(k)) s.byKey.set(k, cur); else s.duplicateKeys.push(k + ' (' + s.byKey.get(k).name + ' / ' + cur.name + ')'); });
      } else if (line.charAt(0) === '=' && cur != null) {
        var q = jsplit(line.substring(1), '|');
        var o = { text: jtrim(q[0]), needs: [], needNames: [], ratio: q.length > 2 ? jtrim(q[2]) : '', effect: q.length > 3 ? jtrim(q[3]) : '' };
        if (q.length !== 4) s.shortLines.push(cur.name + ': ' + line);
        if (q.length > 1) jsplit(q[1], '+').forEach(function (g) {
          var keys = [], names = [];
          jsplit(g, '/').forEach(function (n) {
            var k = normalize(jtrim(n));
            if (k === '' || cur.keys.indexOf(k) >= 0) return;
            keys.push(k);
            names.push(jtrim(n));
          });
          if (keys.length) { o.needs.push(keys); o.needNames.push(names); }
        });
        if (Substitutions.computeFlags) Substitutions.computeFlags(o); // v1.7 (core17.js)
        cur.options.push(o);
      } else if (line.charAt(0) === '~' && cur != null) {
        cur.note = cur.note === '' ? jtrim(line.substring(1)) : cur.note + ' ' + jtrim(line.substring(1));
      }
    }
    return s;
  };
  Substitutions.prototype.forKey = function (key) {
    if (key == null) return null;
    var e = this.byKey.get(key);
    if (e === undefined) {
      var f = family(key);
      e = f != null ? this.byKey.get(f) : undefined;
    }
    return e === undefined ? null : e;
  };
  Substitutions.prototype.availableFor = function (key, m) {
    var e = this.forKey(key);
    if (e == null) return [];
    return e.options.filter(function (o) { return optionAvailable(o, m); });
  };
  Substitutions.prototype.search = function (query) {
    var q = query == null ? '' : jtrim(String(query)).toLowerCase();
    if (q === '') return this.entries;
    var qk = normalize(q);
    return this.entries.filter(function (e) { return entrySearchText(e).indexOf(q) >= 0 || (qk !== '' && e.keys.indexOf(qk) >= 0); });
  };
  Substitutions.prototype.optionCount = function () { var n = 0; this.entries.forEach(function (e) { n += e.options.length; }); return n; };

  // ---------- Matcher ----------
  function Matcher(pantryKeys, stapleKeys) {
    this.have = new Set(); this.general = new Set(); this.staples = new Set(); this.staplesGeneral = new Set();
    this.subs = null; this.accepted = new Set();
    var self = this;
    pantryKeys.forEach(function (k) { add(k, self.have, self.general); });
    stapleKeys.forEach(function (k) { add(k, self.have, self.general); add(k, self.staples, self.staplesGeneral); });
  }
  function add(k, exact, gen) {
    if (k == null || k === '') return;
    exact.add(k);
    var f = family(k);
    if (f != null) gen.add(f);
  }
  function covers(key, exact, gen) {
    if (exact.has(key)) return true;
    var f = family(key);
    if (f != null && exact.has(f)) return true;
    return gen.has(key);
  }
  Matcher.prototype.has = function (key) { return covers(key, this.have, this.general); };
  Matcher.prototype.isStaple = function (key) { return covers(key, this.staples, this.staplesGeneral); };
  Matcher.prototype.withSubstitutions = function (s, acceptedKeys) { this.subs = s; this.accepted = new Set(acceptedKeys || []); return this; };
  Matcher.prototype.isAccepted = function (key) { return this.accepted.has(key); };
  Matcher.prototype.pantryEmpty = function () { return this.have.size === this.staples.size; };
  Matcher.prototype.match = function (r) {
    var res = { recipe: r, total: 0, have: 0, haveNonStaple: 0, missing: [], swappable: [], substituted: [] };
    var seen = new Set();
    for (var n = 0; n < r.ingredients.length; n++) {
      var i = r.ingredients[n];
      if (i.optional || i.key === '' || seen.has(i.key)) continue;
      seen.add(i.key);
      res.total++;
      if (this.has(i.key)) {
        res.have++;
        if (!this.isStaple(i.key)) res.haveNonStaple++;
      } else if (this.subs != null && this.subs.availableFor(i.key, this).length > 0) {
        if (this.accepted.has(i.key)) { res.have++; res.haveNonStaple++; res.substituted.push(i); }
        else { res.missing.push(i); res.swappable.push(i); }
      } else {
        res.missing.push(i);
      }
    }
    return res;
  };
  function ratio(r) { return r.total === 0 ? 0 : r.have / r.total; }
  function compare(a, b) {
    var ra = a.haveNonStaple > 0, rb = b.haveNonStaple > 0;
    if (ra !== rb) return ra ? -1 : 1;
    var ca = a.missing.length === 0, cb = b.missing.length === 0;
    if (ca !== cb) return ca ? -1 : 1;
    var x = ratio(b), y = ratio(a);
    var c = x < y ? -1 : x > y ? 1 : 0;
    if (c !== 0) return c;
    c = a.missing.length - b.missing.length;
    if (c !== 0) return c;
    c = b.haveNonStaple - a.haveNonStaple;
    if (c !== 0) return c;
    return cmpIC(a.recipe.title, b.recipe.title);
  }

  // ---------- Discover ----------
  var QUICK_MINUTES = D.quickMinutes, BORING = toSet(D.boring);
  function signature(r) {
    if (r._sig) return r._sig;
    var s = new Set();
    r.ingredients.forEach(function (i) {
      if (i.optional || i.key === '' || BORING.has(i.key)) return;
      s.add(i.key);
      var f = family(i.key);
      if (f != null) s.add(f);
    });
    r._sig = s;
    return s;
  }
  function similarity(sa, a, sb, b) {
    var inter = 0;
    sa.forEach(function (k) { if (sb.has(k)) inter++; });
    var union = sa.size + sb.size - inter;
    var j = union === 0 ? 0 : inter / union;
    if (a.cuisine !== '' && eqIC(a.cuisine, b.cuisine)) j += 0.15;
    if (a.category !== '' && eqIC(a.category, b.category)) j += 0.10;
    return j;
  }
  function basedOnFavorites(all, favorites, limit) {
    if (favorites.length === 0) return [];
    var favIds = new Set(), favSig = [];
    favorites.forEach(function (f) { favIds.add(f.id); favSig.push(signature(f)); });
    var scored = [], seenR = new Set();
    all.forEach(function (r) {
      if (favIds.has(r.id) || seenR.has(r)) return;
      seenR.add(r);
      var sr = signature(r), best = 0;
      for (var i = 0; i < favorites.length; i++) best = Math.max(best, similarity(sr, r, favSig[i], favorites[i]));
      if (best >= 0.12) scored.push({ r: r, s: best });
    });
    scored.sort(function (x, y) { return y.s < x.s ? -1 : y.s > x.s ? 1 : cmpStr(x.r.title, y.r.title); });
    return scored.slice(0, limit).map(function (x) { return x.r; });
  }
  function sorted(results) { return results.slice().sort(compare); }
  function newForYou(results, seenIds, limit) {
    return sorted(results).filter(function (r) { return !seenIds.has(r.recipe.id); }).slice(0, limit);
  }
  function quick(results, limit) {
    return sorted(results).filter(function (r) { var t = totalMinutes(r.recipe); return t > 0 && t <= QUICK_MINUTES; }).slice(0, limit);
  }
  function almostThere(results, limit) {
    return sorted(results).filter(function (r) { var m = r.missing.length; return m >= 1 && m <= 2 && r.haveNonStaple > 0; }).slice(0, limit);
  }
  /** Distinct cuisines/categories with counts, case-insensitive alphabetical: [[name, count], ...]. */
  function facet(all, cuisine) {
    var list = [];
    all.forEach(function (r) {
      var v = cuisine ? r.cuisine : r.category;
      if (v == null || jtrim(v) === '' || eqIC(v, 'unknown')) return;
      v = jtrim(v);
      var lo = 0, hi = list.length;
      while (lo < hi) { var mid = (lo + hi) >> 1; if (cmpIC(list[mid][0], v) < 0) lo = mid + 1; else hi = mid; }
      if (lo < list.length && cmpIC(list[lo][0], v) === 0) list[lo][1]++;
      else list.splice(lo, 0, [v, 1]);
    });
    return list;
  }
  function inCategory(r, category) {
    var c = jtrim(category.toLowerCase());
    if (jtrim(r.category.toLowerCase()) === c) return true;
    if (c === 'vegetarian') return r.tags.indexOf(VEGETARIAN) >= 0;
    if (c === 'vegan') return r.tags.indexOf(VEGAN) >= 0;
    if (c === 'dessert') return eqIC(r.category, 'Baking') || eqIC(r.category, 'Desserts');
    if (c === 'starter' || c === 'side') return eqIC(r.category, 'Salad') || eqIC(r.category, 'Snack') || eqIC(r.category, 'Side') || eqIC(r.category, 'Starter');
    var fam = c;
    if (['beef', 'chicken', 'pork', 'lamb', 'pasta', 'seafood', 'goat'].indexOf(fam) >= 0) {
      for (var n = 0; n < r.ingredients.length; n++) {
        var i = r.ingredients[n];
        if (i.optional) continue;
        var f = family(i.key);
        if (i.key === fam || fam === f || (i.key.indexOf(fam + ' ') === 0 && !/stock$/.test(i.key))) return true;
        if (fam === 'seafood' && ((f != null && (f === 'fish' || f === 'seafood')) || i.key === 'fish')) return true;
      }
    }
    return false;
  }
  function searchTitles(all, query, limit) {
    var out = [];
    var q = query == null ? '' : jtrim(String(query).toLowerCase());
    if (q === '') return out;
    var words = q.split(/[ \t\n\x0B\f\r]+/);
    for (var n = 0; n < all.length; n++) {
      var r = all[n];
      var hay = (r.title + ' ' + r.cuisine + ' ' + r.category).toLowerCase();
      var ok = true;
      for (var k = 0; k < words.length; k++) if (hay.indexOf(words[k]) < 0) { ok = false; break; }
      if (ok) out.push(r);
      if (out.length >= limit) break;
    }
    return out;
  }

  // ---------- TheMealDB conversion (MealDb.convert / splitSteps) ----------
  function ms(m, k) { var o = m[k]; return o == null ? '' : jtrim(String(o)); }
  function splitSteps(text) {
    var out = [];
    text.split(/\r?\n/).forEach(function (line) {
      var t = jtrim(jtrim(line).replace(/^(step[ \t\n\x0B\f\r]*\d+[:.)]?|\d+[.)]|[-\u2022*])[ \t\n\x0B\f\r]*/i, ''));
      if (t.length < 3) return;
      if (/^step[ \t\n\x0B\f\r]*\d+$/i.test(t)) return;
      if (t.length > 450) {
        var sentences = splitSentences(t), b = '';
        sentences.forEach(function (sen) {
          if (b.length > 0 && b.length + sen.length > 280) { out.push(jtrim(b)); b = ''; }
          b += sen + ' ';
        });
        if (b.length > 0) out.push(jtrim(b));
      } else out.push(t);
    });
    return out;
  }
  /** Java t.split("(?<=[.!?])\\s+") without relying on lookbehind (older Safari). */
  function splitSentences(t) {
    var out = [], re = /[.!?]([ \t\n\x0B\f\r]+)/g, last = 0, m;
    while ((m = re.exec(t)) !== null) {
      out.push(t.substring(last, m.index + 1));
      last = m.index + m[0].length;
    }
    out.push(t.substring(last));
    while (out.length > 1 && out[out.length - 1] === '') out.pop();
    return out;
  }
  function convertMeal(m) {
    var id = ms(m, 'idMeal');
    if (id === '') return null;
    var r = {
      id: 'm:' + id, title: ms(m, 'strMeal'), category: ms(m, 'strCategory'), cuisine: ms(m, 'strArea'),
      prepMin: -1, cookMin: -1, servings: 4, servingsKnown: false, tagsInferred: true, source: 'TheMealDB',
      image: ms(m, 'strMealThumb'), sourceUrl: ms(m, 'strSource'), tags: [], meals: [], ingredients: [], steps: []
    };
    if (r.cuisine === '' || eqIC(r.cuisine, 'Unknown')) r.cuisine = ms(m, 'strCountry');
    if (r.sourceUrl === '') r.sourceUrl = ms(m, 'strYoutube');
    for (var i = 1; i <= 20; i++) {
      var name = ms(m, 'strIngredient' + i);
      if (name === '') continue;
      var p = parseQuantity(ms(m, 'strMeasure' + i));
      r.ingredients.push(makeIngredient(name, p.amount, p.unit, false));
    }
    r.steps = splitSteps(ms(m, 'strInstructions'));
    r.tags = classify(r.ingredients);
    r.meals = inferMeals(r.category, r.title);
    return r;
  }

  // ---------- Cook mode timer ----------
  var MINUTES = /(\d+)(?:[ \t\n\x0B\f\r]*(?:-|to|\u2013)[ \t\n\x0B\f\r]*(\d+))?[ \t\n\x0B\f\r]*(?:minutes|minute|mins|min)\b/i;
  var HOURS = /(\d+)[ \t\n\x0B\f\r]*(?:hours|hour|hrs|hr)\b/i;
  function stepMinutes(s) {
    var m = MINUTES.exec(s), v;
    if (m) { v = parseInt(m[2] != null ? m[2] : m[1], 10); return v > 0 && v <= 600 ? v : 0; }
    var h = HOURS.exec(s);
    if (h) { v = parseInt(h[1], 10) * 60; return v > 0 && v <= 600 ? v : 0; }
    return 0;
  }

  var Catalog = {
    CATEGORIES: D.catalog.categories, ITEMS: D.catalog.items, COLORS: D.catalog.colors, ALIASES: D.catalog.aliases,
    DEFAULT_STAPLES: D.catalog.defaultStaples,
    isHeader: function (s) { return s.charAt(0) === '#'; },
    allItems: function () { var o = []; D.catalog.items.forEach(function (c) { c.forEach(function (s) { if (s.charAt(0) !== '#') o.push(s); }); }); return o; },
    count: function (c) { return D.catalog.items[c].filter(function (s) { return s.charAt(0) !== '#'; }).length; }
  };

  var PR = {
    jtrim: jtrim, jsplit: jsplit, cmpIC: cmpIC, eqIC: eqIC,
    normalize: normalize, singular: singular, family: family, isKnown: isKnown,
    parseQuantity: parseQuantity, parseNumber: parseNumber, number: number, formatQty: formatQty,
    makeIngredient: makeIngredient, recipeFromMap: recipeFromMap, recipeToMap: recipeToMap, parseRecipeList: parseRecipeList,
    totalMinutes: totalMinutes, isOnline: isOnline,
    classify: classify, isMeat: isMeat, isDairy: isDairy, isOtherAnimal: isOtherAnimal, hasGluten: hasGluten,
    Substitutions: Substitutions, usesLabel: usesLabel, optionAvailable: optionAvailable,
    Matcher: Matcher, compare: compare, ratio: ratio,
    QUICK_MINUTES: QUICK_MINUTES, signature: signature, similarity: similarity, basedOnFavorites: basedOnFavorites,
    newForYou: newForYou, quick: quick, almostThere: almostThere, facet: facet, inCategory: inCategory, searchTitles: searchTitles,
    splitSteps: splitSteps, convertMeal: convertMeal, stepMinutes: stepMinutes, Catalog: Catalog,
    MEAL_TYPES: MEAL_TYPES, MEAL_TIMES: MEAL_TIMES, isMealType: isMealType, mealCanonical: mealCanonical, mealSorted: mealSorted,
    inferMeals: inferMeals, mealPasses: mealPasses, mealPreferred: mealPreferred, mealAcceptable: mealAcceptable,
    isDessertMeal: isDessertMeal, isSideMeal: isSideMeal, isMealFood: isMealFood, mealDbCategory: mealDbCategory, heroQuestion: heroQuestion,
    DIET_IDS: DIET_IDS, VEGETARIAN: VEGETARIAN, VEGAN: VEGAN, GLUTEN_FREE: GLUTEN_FREE, DAIRY_FREE: DAIRY_FREE
  };
  root.PR = PR;
  if (typeof module !== 'undefined' && module.exports) module.exports = PR;
})(typeof window !== 'undefined' ? window : globalThis);
