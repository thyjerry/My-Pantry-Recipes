/* Pantry Recipes web - recipe import from pasted text or schema.org Recipe JSON-LD (web only).
   Everything happens in the browser; nothing is fetched. MIT License. */
(function (root) {
  'use strict';
  var PR = root.PR;
  var MAX_ING = 100, MAX_STEPS = 100;

  function ImportError(msg) { var e = new Error(msg); e.recipeImport = true; return e; }
  var ENT = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ', frac12: '\u00bd', frac14: '\u00bc', frac34: '\u00be', deg: '\u00b0', ndash: '\u2013', mdash: '\u2014', rsquo: '\u2019', lsquo: '\u2018', rdquo: '\u201d', ldquo: '\u201c', hellip: '\u2026' };
  function decode(s) {
    return s.replace(/&(#x[0-9a-f]+|#[0-9]+|[a-z]+[0-9]*);/gi, function (m, e) {
      if (e.charAt(0) === '#') {
        var n = e.charAt(1) === 'x' || e.charAt(1) === 'X' ? parseInt(e.substring(2), 16) : parseInt(e.substring(1), 10);
        return n > 0 && n < 0x110000 ? String.fromCodePoint(n) : m;
      }
      var k = e.toLowerCase();
      return Object.prototype.hasOwnProperty.call(ENT, k) ? ENT[k] : m;
    });
  }
  /** Plain text from a JSON-LD string value (which may contain HTML). */
  function clean(s) {
    if (s == null) return '';
    if (typeof s === 'number') return String(s);
    if (typeof s !== 'string') return '';
    return decode(s.replace(/<br\s*\/?>/gi, '\n').replace(/<\/(p|li|div|h\d)>/gi, '\n').replace(/<[^>]*>/g, '')).replace(/[ \t\u00a0]+/g, ' ').replace(/\s*\n\s*/g, '\n').trim();
  }
  function oneLine(s) { return clean(s).replace(/\s+/g, ' '); }
  function first(v) { return Array.isArray(v) ? first(v[0]) : v; }

  // ---------- durations / servings ----------
  function isoMinutes(v) {
    var s = oneLine(first(v));
    var m = /^P(?:(\d+(?:\.\d+)?)D)?(?:T(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?)?$/i.exec(s);
    if (!m || s === 'P' || s === 'PT') return -1;
    var t = (+(m[1] || 0)) * 1440 + (+(m[2] || 0)) * 60 + (+(m[3] || 0)) + (+(m[4] || 0)) / 60;
    return t > 0 && t < 100000 ? Math.round(t) : -1;
  }
  function textMinutes(s) {
    var h = /(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hour|hours)\b/i.exec(s), m = /(\d+)\s*(?:m|min|mins|minute|minutes)\b/i.exec(s);
    if (!h && !m) return -1;
    return Math.round((h ? +h[1] * 60 : 0) + (m ? +m[1] : 0));
  }
  function servingsOf(v) {
    var s = oneLine(first(v)), m = /(\d+)/.exec(s);
    if (!m) return -1;
    var n = +m[1];
    return n >= 1 && n <= 100 ? n : -1;
  }

  // ---------- ingredient lines ----------
  var UNITS = ['fl oz', 'fluid ounces', 'fluid ounce', 'g', 'gram', 'grams', 'gr', 'kg', 'kilo', 'kilos', 'kilogram', 'kilograms', 'mg', 'ml', 'millilitre', 'millilitres', 'milliliter', 'milliliters',
    'l', 'litre', 'litres', 'liter', 'liters', 'oz', 'ounce', 'ounces', 'lb', 'lbs', 'pound', 'pounds', 'cup', 'cups', 'c', 'tbsp', 'tbs', 'tbl', 'tablespoon', 'tablespoons',
    'tsp', 'teaspoon', 'teaspoons', 'pinch', 'pinches', 'dash', 'dashes', 'clove', 'cloves', 'can', 'cans', 'tin', 'tins', 'jar', 'jars', 'packet', 'packets', 'pack', 'packs',
    'bunch', 'bunches', 'slice', 'slices', 'stick', 'sticks', 'sprig', 'sprigs', 'handful', 'handfuls', 'piece', 'pieces', 'head', 'heads', 'pint', 'pints', 'qt', 'quart', 'quarts',
    'large', 'medium', 'small'];
  var NUM = '(?:\\d+\\/\\d+(?![\\d\\/])|\\d+(?:[.,]\\d+)?(?:\\s+\\d+\\/\\d+)?(?:\\s*[\\u00bc-\\u00be\\u2150-\\u215e])?|\\d+\\/\\d+|[\\u00bc-\\u00be\\u2150-\\u215e])';
  var LEAD = new RegExp('^(' + NUM + ')(?:\\s*(?:-|\\u2013|to)\\s*' + NUM + ')?\\s*', 'i');
  function ingredientLine(line0) {
    var line = oneLine(line0).replace(/^[-*\u2022\u25aa\u25cf\u2013\u2014]+\s*/, '').replace(/^\d+[.)]\s+/, '');
    if (line === '') return null;
    var optional = false;
    if (/\(optional\)|,\s*optional\b|\boptional:?\s*$|^optional:?\s+/i.test(line)) {
      optional = true;
      line = line.replace(/\s*\(optional\)\s*/i, ' ').replace(/,\s*optional\b/i, '').replace(/\boptional:?\s*$/i, '').replace(/^optional:?\s+/i, '').trim();
    }
    var amount = NaN, unit = '', name = line, m = LEAD.exec(line);
    if (m) {
      var num = m[1].replace(',', '.'), rest = line.substring(m[0].length), u = '';
      var lower = rest.toLowerCase();
      for (var k = 0; k < UNITS.length; k++) {
        var w = UNITS[k];
        if (lower.indexOf(w) === 0) {
          var after = lower.charAt(w.length);
          if (after === '' || after === ' ' || after === '.' || after === ',' || after === ')') {
            if (after === '' && (w === 'large' || w === 'medium' || w === 'small' || w === 'c')) continue;
            u = rest.substring(0, w.length); rest = rest.substring(w.length).replace(/^\.?\s*/, '').replace(/^of\s+/i, '');
            break;
          }
        }
      }
      if (u === 'c' || u === 'C') u = 'cup';
      var q = PR.parseQuantity((num + ' ' + u).trim());
      if (q.amount != null && !isNaN(q.amount) && rest.trim() !== '') { amount = q.amount; unit = q.unit; name = rest.trim(); }
    }
    name = name.replace(/^[,;:\s]+/, '');
    var par = /^(\([^)]*\))\s*(.+)$/.exec(name);
    if (par) name = par[2] + ' ' + par[1];
    if (name === '') return null;
    if (name.length > 120) name = name.substring(0, 120);
    var o = { name: name.charAt(0).toUpperCase() + name.substring(1) };
    if (!isNaN(amount)) o.qty = amount;
    if (unit) o.unit = unit;
    if (optional) o.optional = true;
    return o;
  }

  // ---------- JSON-LD ----------
  function isRecipe(o) {
    if (!o || typeof o !== 'object') return false;
    var t = o['@type'];
    return t === 'Recipe' || (Array.isArray(t) && t.indexOf('Recipe') >= 0) || t === 'schema:Recipe' || t === 'http://schema.org/Recipe' || t === 'https://schema.org/Recipe';
  }
  function findRecipe(o, depth) {
    if (!o || typeof o !== 'object' || depth > 8) return null;
    if (Array.isArray(o)) { for (var i = 0; i < o.length; i++) { var r = findRecipe(o[i], depth + 1); if (r) return r; } return null; }
    if (isRecipe(o)) return o;
    var keys = ['@graph', 'mainEntity', 'mainEntityOfPage', 'itemListElement', 'item'];
    for (var k = 0; k < keys.length; k++) { var x = findRecipe(o[keys[k]], depth + 1); if (x) return x; }
    return null;
  }
  function stepsOf(v, out) {
    if (out.length >= MAX_STEPS || v == null) return out;
    if (typeof v === 'string') { clean(v).split(/\n+/).forEach(function (s) { s = s.replace(/^\s*(?:step\s*)?\d+[.):]\s*/i, '').trim(); if (s && out.length < MAX_STEPS) out.push(s.substring(0, 2000)); }); return out; }
    if (Array.isArray(v)) { v.forEach(function (x) { stepsOf(x, out); }); return out; }
    if (typeof v === 'object') {
      if (v.itemListElement) return stepsOf(v.itemListElement, out);
      var t = v.text != null ? v.text : v.name;
      return stepsOf(t, out);
    }
    return out;
  }
  function categoriesOf(v) {
    var out = [];
    (Array.isArray(v) ? v : v == null ? [] : String(v).split(',')).forEach(function (c) { c = oneLine(c); if (c) out.push(c); });
    return out;
  }
  function fromJsonLd(o) {
    var title = oneLine(o.name || o.headline);
    var ings = [];
    var src = o.recipeIngredient || o.ingredients;
    (Array.isArray(src) ? src : typeof src === 'string' ? src.split(/\n+/) : []).forEach(function (l) { if (ings.length < MAX_ING) { var x = ingredientLine(typeof l === 'string' ? l : l && (l.text || l.name)); if (x) ings.push(x); } });
    var cats = categoriesOf(o.recipeCategory);
    var prep = isoMinutes(o.prepTime), cook = isoMinutes(o.cookTime), total = isoMinutes(o.totalTime);
    if (prep < 0 && cook < 0 && total > 0) cook = total;
    else if (prep >= 0 && cook < 0 && total > prep) cook = total - prep;
    else if (cook >= 0 && prep < 0 && total > cook) prep = total - cook;
    var url = oneLine(first(o.url) || (o.mainEntityOfPage && (o.mainEntityOfPage['@id'] || o.mainEntityOfPage.url)) || '');
    return {
      title: title, category: cats[0] || '', cuisine: categoriesOf(o.recipeCuisine)[0] || '', prep: prep, cook: cook, servings: servingsOf(o.recipeYield),
      meals: cats, ingredients: ings, steps: stepsOf(o.recipeInstructions, []), sourceUrl: /^https?:\/\//i.test(url) ? url.substring(0, 500) : '', format: 'json-ld'
    };
  }
  function jsonBlocks(text) {
    var out = [], re = /<script[^>]*type\s*=\s*["']?application\/ld\+json["']?[^>]*>([\s\S]*?)<\/script>/gi, m;
    while ((m = re.exec(text))) out.push(m[1]);
    var t = text.trim();
    if (!out.length && (t.charAt(0) === '{' || t.charAt(0) === '[')) out.push(t);
    return out;
  }

  // ---------- plain text ----------
  var H_ING = /^(ingredients?|you(?:'|\u2019)?ll need|what you need)\s*:?\s*$/i;
  var H_STEP = /^(method|instructions?|directions?|steps?|preparation|how to make( it)?)\s*:?\s*$/i;
  function fromText(text) {
    var lines = text.replace(/\r\n?/g, '\n').split('\n').map(function (l) { return l.replace(/[ \t\u00a0]+/g, ' ').trim(); });
    var title = '', ings = [], steps = [], mode = '', prep = -1, cook = -1, serv = -1, sawHeader = false, loose = [];
    for (var i = 0; i < lines.length; i++) {
      var l = lines[i];
      if (l === '') continue;
      var mm;
      if ((mm = /^(?:prep(?:aration)?(?: time)?)\s*:?\s*(.+)$/i.exec(l)) && textMinutes(mm[1]) >= 0) { prep = textMinutes(mm[1]); continue; }
      if ((mm = /^(?:cook(?:ing)?(?: time)?)\s*:?\s*(.+)$/i.exec(l)) && textMinutes(mm[1]) >= 0) { cook = textMinutes(mm[1]); continue; }
      if ((mm = /^(?:total(?: time)?)\s*:?\s*(.+)$/i.exec(l)) && textMinutes(mm[1]) >= 0) { if (prep < 0 && cook < 0) cook = textMinutes(mm[1]); continue; }
      if ((mm = /^(?:serves|servings|yield|makes|portions)\s*:?\s*(.+)$/i.exec(l)) && servingsOf(mm[1]) > 0) { serv = servingsOf(mm[1]); continue; }
      if (H_ING.test(l)) { mode = 'i'; sawHeader = true; continue; }
      if (H_STEP.test(l)) { mode = 's'; sawHeader = true; continue; }
      if (!title) { title = l.replace(/^#+\s*/, ''); continue; }
      if (mode === 'i') { if (ings.length < MAX_ING) { var x = ingredientLine(l); if (x) ings.push(x); } }
      else if (mode === 's') stepsOf(l, steps);
      else loose.push(l);
    }
    if (!sawHeader) {
      // no headings: bullet or quantity lines are ingredients, the rest are steps
      loose.forEach(function (l) {
        if (/^[-*\u2022]/.test(l) || LEAD.test(l.replace(/^[-*\u2022]+\s*/, '')) && l.length < 90 && !/^\d+[.)]\s+[A-Z]/.test(l)) { var x = ingredientLine(l); if (x && ings.length < MAX_ING) ings.push(x); }
        else stepsOf(l, steps);
      });
    }
    return { title: title.substring(0, 120), category: '', cuisine: '', prep: prep, cook: cook, servings: serv, meals: [], ingredients: ings, steps: steps, sourceUrl: '', format: 'text' };
  }

  /** Parses pasted text. Returns a plain recipe map for PR.recipeFromMap (without an id), or throws. */
  function parse(text) {
    if (text == null || String(text).trim() === '') throw ImportError('Paste a recipe first.');
    text = String(text);
    if (text.length > 2000000) throw ImportError('That is too much text for one recipe.');
    var blocks = jsonBlocks(text), found = null, jsonErr = false;
    for (var b = 0; b < blocks.length && !found; b++) {
      var o;
      try { o = JSON.parse(blocks[b].trim()); } catch (e) { jsonErr = true; continue; }
      found = findRecipe(o, 0);
    }
    var r;
    if (found) r = fromJsonLd(found);
    else if (blocks.length && /^\s*[{[]/.test(text) && !/<script/i.test(text)) throw ImportError(jsonErr ? 'This looks like JSON but it could not be read.' : 'No schema.org Recipe found in this JSON.');
    else r = fromText(/<script/i.test(text) ? clean(text.replace(/<script[\s\S]*?<\/script>/gi, '').replace(/<style[\s\S]*?<\/style>/gi, '')) : text);
    if (!r.title) throw ImportError('Could not find the recipe name. Put it on the first line.');
    if (!r.ingredients.length) throw ImportError('Could not find any ingredients. Put them under a line that says \u201cIngredients\u201d, one per line.');
    var map = {
      title: r.title, category: r.category.substring(0, 60), cuisine: r.cuisine.substring(0, 60), prep: r.prep, cook: r.cook,
      servings: r.servings > 0 ? Math.min(r.servings, 24) : 4, servingsKnown: r.servings > 0, tags: [], meals: r.meals,
      ingredients: r.ingredients, steps: r.steps, source: 'user', sourceUrl: r.sourceUrl
    };
    return { recipe: map, format: r.format };
  }
  /** Builds the stored recipe object (id "u:..."), with diet tags from the ingredient list. */
  function toRecipe(map, id) {
    var m = Object.assign({}, map, { id: id });
    var r = PR.recipeFromMap(m);
    r.tags = PR.classify(r.ingredients);
    r.tagsInferred = true;
    return r;
  }
  PR.RecipeImport = { parse: parse, toRecipe: toRecipe, ingredientLine: ingredientLine, isoMinutes: isoMinutes };
})(typeof window !== 'undefined' ? window : globalThis);
