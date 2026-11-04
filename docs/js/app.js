/* Pantry Recipes web - user interface. Plain ES2017, no frameworks, no trackers. MIT License. */
(function () {
  'use strict';
  var PR = window.PR, C = PR.Catalog, ICONS = window.PR_ICONS || {};
  var VERSION = window.PR_VERSION || '1.6-web';           // set by js/version.js (the one version source)
  var SHORT_VERSION = 'v' + VERSION.replace(/-web$/, '');
  var API_BASE = 'https://www.themealdb.com/api/json/v1/';

  // =====================================================================
  // Storage: localStorage when available, otherwise in-memory (e.g. some browsers block storage on file://)
  // =====================================================================
  var Store = (function () {
    var mem = {}, ls = null;
    try {
      ls = window.localStorage;
      ls.setItem('pr.__t', '1');
      ls.removeItem('pr.__t');
    } catch (e) { ls = null; }
    return {
      persistent: !!ls,
      get: function (k, def) {
        var v = null;
        try { v = ls ? ls.getItem('pr.' + k) : (k in mem ? mem[k] : null); } catch (e) { v = k in mem ? mem[k] : null; }
        if (v == null) return def;
        try { return JSON.parse(v); } catch (e) { return def; }
      },
      set: function (k, val) {
        var s = JSON.stringify(val);
        mem[k] = s;
        if (ls) try { ls.setItem('pr.' + k, s); } catch (e) { /* quota or blocked: keep in memory */ }
      },
      del: function (k) { delete mem[k]; if (ls) try { ls.removeItem('pr.' + k); } catch (e) { } }
    };
  })();

  // =====================================================================
  // Theme (Appearance): presets, custom palettes, follow-system; every colour in css/app.css is a CSS variable
  // =====================================================================
  var Theme = (function () {
    var P = PR.Palette, mq = null, listeners = [];
    try { mq = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null; } catch (e) { mq = null; }
    var state = P.parseState(Store.get('theme', ''));
    function systemDark() { return !!(mq && mq.matches); }
    function current() { return state.resolve(systemDark()); }
    function rgba(n, a) { return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + a + ')'; }
    /** CSS variables for a base palette {bg, surface, primary, accent, text}. */
    function vars(b) {
      var c = P.derive(b), h = P.hex;
      return {
        dark: c.dark, colors: c, v: {
          bg: h(c.bg), card: h(c.surface), text: h(c.text), text2: h(c.text2), text3: h(c.text3), line: h(c.line), neutral: h(c.neutral),
          primary: h(c.primary), green: h(c.fill), 'green-dark': h(c.primaryText), tint: h(c.tint), tint2: h(c.tint2), selected: h(c.selected),
          'on-fill': h(c.onFill), 'on-fill2': h(c.onFill2), 'hero-end': h(c.heroEnd), 'on-fill-a18': rgba(c.onFill, 0.18), 'on-fill-a75': rgba(c.onFill, 0.75),
          amber: h(c.accentText), 'amber-tint': h(c.accentTint), 'amber-line': h(P.mix(c.accent, c.surface, 0.68)), staple: h(c.accentTint), 'staple-text': h(c.accentText),
          danger: h(c.danger), swap: h(c.info), 'swap-tint': h(c.infoTint),
          'toast-bg': h(c.text), 'toast-text': h(c.surface), scrim: c.dark ? 'rgba(0,0,0,.62)' : rgba(c.text, 0.42),
          shadow: c.dark ? 'rgba(0,0,0,.35)' : rgba(c.text, 0.06), shadow2: c.dark ? 'rgba(0,0,0,.6)' : rgba(c.text, 0.18),
          star: c.dark ? '#F5C451' : '#E0A526', heart: c.dark ? '#FF7A8C' : '#D63A50'
        }
      };
    }
    function styleText(b) { var x = vars(b), out = []; Object.keys(x.v).forEach(function (k) { out.push('--' + k + ':' + x.v[k]); }); out.push('color-scheme:' + (x.dark ? 'dark' : 'light')); return out.join(';'); }
    function meta(name, content) {
      var m = document.querySelector('meta[name="' + name + '"]');
      if (!m) { m = document.createElement('meta'); m.setAttribute('name', name); document.head.appendChild(m); }
      m.setAttribute('content', content);
    }
    var applied = null;
    function apply() {
      var b = current(), x = vars(b), st = document.documentElement.style;
      Object.keys(x.v).forEach(function (k) { st.setProperty('--' + k, x.v[k]); });
      st.colorScheme = x.dark ? 'dark' : 'light';
      st.setProperty('--fs', String(state.fontScale()));
      st.setProperty('--rad', String(state.cornerScale()));
      document.documentElement.setAttribute('data-theme', b.id);
      document.documentElement.classList.toggle('dark', x.dark);
      try { meta('theme-color', x.v.card); meta('color-scheme', x.dark ? 'dark' : 'light'); } catch (e) { }
      applied = b;
      listeners.forEach(function (f) { f(); });
    }
    function save() { Store.set('theme', PR.Json.write(state.toMap())); }
    if (mq) {
      var onSys = function () { if (state.active === 'system') apply(); };
      if (mq.addEventListener) mq.addEventListener('change', onSys); else if (mq.addListener) mq.addListener(onSys);
    }
    return {
      state: function () { return state; }, current: current, systemDark: systemDark, vars: vars, styleText: styleText, apply: apply,
      applied: function () { return applied; }, onChange: function (f) { listeners.push(f); },
      use: function (id) { state.active = id; save(); apply(); },
      set: function (k, v) { state[k] = v; save(); apply(); },
      setState: function (s) { state = s; save(); apply(); },
      put: function (b) { var ok = state.put(b); if (ok) save(); return ok; },
      remove: function (id) { state.remove(id); save(); apply(); },
      reset: function () { state.active = 'light'; save(); apply(); }
    };
  })();
  Theme.apply();

  /** TheMealDB recipe cache: IndexedDB when available, else localStorage, else memory. */
  var Cache = (function () {
    var db = null, mode = 'memory';
    function fallback() { mode = Store.persistent ? 'local' : 'memory'; }
    function open() {
      return new Promise(function (resolve) {
        var idb = null;
        try { idb = window.indexedDB; } catch (e) { idb = null; }
        if (!idb) { fallback(); resolve(); return; }
        var req;
        try { req = idb.open('pantry-recipes', 1); } catch (e) { fallback(); resolve(); return; }
        var done = false, finish = function () { if (!done) { done = true; resolve(); } };
        req.onupgradeneeded = function () { req.result.createObjectStore('cache', { keyPath: 'id' }); };
        req.onsuccess = function () { if (done) return; db = req.result; mode = 'idb'; finish(); };
        req.onerror = function () { fallback(); finish(); };
        req.onblocked = function () { fallback(); finish(); };
        setTimeout(function () { if (!done) { fallback(); finish(); } }, 3000);
      });
    }
    function all() {
      if (mode === 'idb') return new Promise(function (resolve) {
        try {
          var out = [], tx = db.transaction('cache', 'readonly'), cur = tx.objectStore('cache').openCursor();
          cur.onsuccess = function () { var c = cur.result; if (c) { out.push(c.value); c.continue(); } else resolve(out); };
          cur.onerror = function () { resolve([]); };
        } catch (e) { resolve([]); }
      });
      return Promise.resolve(Store.get('cache', []));
    }
    function put(rows, removeIds) {
      if (mode === 'idb') {
        try {
          var tx = db.transaction('cache', 'readwrite'), os = tx.objectStore('cache');
          rows.forEach(function (r) { os.put(r); });
          removeIds.forEach(function (id) { os.delete(id); });
        } catch (e) { }
        return;
      }
      var cur = Store.get('cache', []), byId = {};
      cur.forEach(function (r) { byId[r.id] = r; });
      rows.forEach(function (r) { byId[r.id] = r; });
      removeIds.forEach(function (id) { delete byId[id]; });
      Store.set('cache', Object.keys(byId).map(function (k) { return byId[k]; }));
    }
    return { open: open, all: all, put: put, mode: function () { return mode; } };
  })();

  /** Recipe photos: downscaled JPEG data URLs in IndexedDB (database "pantry-recipes-photos"); in memory only if IndexedDB is unavailable. */
  var Photos = (function () {
    var db = null, mem = new Map(), mode = 'memory';
    function open() {
      return new Promise(function (resolve) {
        var idb = null;
        try { idb = window.indexedDB; } catch (e) { idb = null; }
        if (!idb) { resolve(); return; }
        var req, done = false, finish = function () { if (!done) { done = true; resolve(); } };
        try { req = idb.open('pantry-recipes-photos', 1); } catch (e) { resolve(); return; }
        req.onupgradeneeded = function () { req.result.createObjectStore('photos'); };
        req.onsuccess = function () {
          if (done) return;
          db = req.result; mode = 'idb';
          try {
            var cur = db.transaction('photos', 'readonly').objectStore('photos').openCursor();
            cur.onsuccess = function () { var c = cur.result; if (c) { if (typeof c.value === 'string') mem.set(String(c.key), c.value); c.continue(); } else finish(); };
            cur.onerror = finish;
          } catch (e) { finish(); }
        };
        req.onerror = finish; req.onblocked = finish;
        setTimeout(finish, 3000);
      });
    }
    function write(fn) { if (mode !== 'idb') return; try { fn(db.transaction('photos', 'readwrite').objectStore('photos')); } catch (e) { } }
    return {
      open: open, mode: function () { return mode; }, get: function (id) { return mem.get(id) || null; }, all: function () { return mem; },
      put: function (id, url) { mem.set(id, url); write(function (os) { os.put(url, id); }); },
      del: function (id) { mem.delete(id); write(function (os) { os.delete(id); }); },
      clear: function () { mem.clear(); write(function (os) { os.clear(); }); }
    };
  })();
  /** Reads an image file, downscales it to at most 800 px on the long side and returns a JPEG data URL. */
  function downscalePhoto(file) {
    return new Promise(function (resolve, reject) {
      if (!file || !/^image\//.test(file.type || 'image/')) { reject(new Error('That is not an image.')); return; }
      var url = URL.createObjectURL(file), img = new Image();
      img.onload = function () {
        try {
          var w = img.naturalWidth, h = img.naturalHeight, k = Math.min(1, 800 / Math.max(w, h, 1));
          var c = document.createElement('canvas'); c.width = Math.max(1, Math.round(w * k)); c.height = Math.max(1, Math.round(h * k));
          c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
          var q = 0.82, out = c.toDataURL('image/jpeg', q);
          while (out.length > PR.Backup.MAX_PHOTO_CHARS && q > 0.4) { q -= 0.12; out = c.toDataURL('image/jpeg', q); }
          URL.revokeObjectURL(url);
          if (out.indexOf('data:image/jpeg;base64,') !== 0) reject(new Error('This browser could not convert the photo.')); else resolve(out);
        } catch (e) { URL.revokeObjectURL(url); reject(e); }
      };
      img.onerror = function () { URL.revokeObjectURL(url); reject(new Error('Could not read that image.')); };
      img.src = url;
    });
  }

  // =====================================================================
  // Data model (mirrors the Android Store / Repo)
  // =====================================================================
  var bundled = [], online = new Map(), onlineFetched = new Map(), subs = PR.Substitutions.parse(''), loaded = false;
  var pantry = Store.get('pantry', null);
  if (!Array.isArray(pantry)) {
    pantry = [];
    C.DEFAULT_STAPLES.forEach(function (s, n) { pantry.push({ key: PR.normalize(s), label: s, staple: true, added: n }); });
    Store.set('pantry', pantry);
  }
  var favorites = Store.get('favorites', []);       // [{id, at}]
  var shopping = Store.get('shopping', []);         // [{id,key,name,amount,unit,recipe,checked,added}]
  var seen = new Set(Store.get('seen', []));
  var accepted = new Set(Store.get('acceptedSubs', []));
  var recents = Store.get('recents', []);
  var openCats = new Set(Store.get('openCats', ['0']));
  var prefs = Store.get('prefs', {});
  function pref(k, def) { return prefs[k] === undefined ? def : prefs[k]; }
  function setPref(k, v) { prefs[k] = v; Store.set('prefs', prefs); }
  // v1.8: the in-recipe units toggle is remembered across reloads; changing the units setting clears it
  function setDetailUnits(v) { ui.detailUnits = v || null; setPref('detailUnits', v || ''); }
  var version = 0; // bumps whenever pantry, substitutions, recipes or dislikes change
  // v1.7: measurement units (default: weights for dry baking ingredients, in grams, cups in brackets) and cooking methods
  var DENS = null;
  function densities() { return DENS || (DENS = PR.Units.parseDensities(window.PR_DATA17.densities)); }
  function unitMode() { return PR.Units.validMode(pref('units', PR.Units.WEIGHTS)); }
  /** Unit preferences; view=true applies the in-recipe toggle (kept for this session only, like the Android app). */
  function unitPrefs(view) { return new PR.Units.Prefs(view && ui.detailUnits ? ui.detailUnits : unitMode(), pref('weightUnit', 'g'), pref('showCups', true) !== false); }
  function qtyText(key, amount, unit, scale, view) { return PR.Units.display(key, amount == null ? NaN : amount, unit, scale, unitPrefs(view), densities()); }
  function stepText(t, view) { return PR.Units.stepText(t, unitPrefs(view)); }
  function methodFilter() { return PR.Methods.canonical(pref('methods', [])); }
  // v1.5: ratings & notes, meal plan, dislikes/allergies, imported recipes
  var D = PR.Dates;
  var R20 = PR.Region, INV = PR.Inventory, RP = PR.RegionalPrices, DIET_MEMO = new Map(), BLS = null, PRICE_MEMO = null; // v2.0
  var BUDGET_VER = 0, BUDGET_MEMO = { k: null, m: new Map() }, BUDGET_WARM = null; // v2.0 S3: budget memo, filled in slices
  var ratings = Store.get('ratings', {});            // {recipeId: {stars, note}}
  if (!ratings || typeof ratings !== 'object' || Array.isArray(ratings)) ratings = {};
  var plan = Store.get('plan', []);                  // [{date, id, servings}]
  if (!Array.isArray(plan)) plan = [];
  plan = PR.MealPlan.prune(plan.filter(function (e) { return e && D.valid(e.date) && e.id; }), D.today(), 28);
  Store.set('plan', plan);
  var dislikes = Store.get('dislikes', []).filter(function (x) { return typeof x === 'string' && x; });
  var allergens = Store.get('allergens', []).filter(function (x) { return PR.Allergens.ALL_IDS.indexOf(x) >= 0; });
  var userRecipes = new Map();                       // id "u:..." -> Recipe (imported on this device)
  (Store.get('userRecipes', []) || []).forEach(function (m) {
    try { var r = PR.recipeFromMap(m); if (r.id.indexOf('u:') === 0 && r.ingredients.length) userRecipes.set(r.id, r); } catch (e) { }
  });
  // v1.6: prices, leftovers, cooking log, barcode cache, photos
  var DATA16 = window.PR_DATA16 || { foods: '', prices: '' };
  var FOODS = PR.Nutrition.parse(DATA16.foods), DEF_PRICES = PR.Cost.parseDefaults(DATA16.prices);
  var prices = new Map();
  (function () { var o = Store.get('prices', {}); if (o && typeof o === 'object') Object.keys(o).forEach(function (k) { var x = o[k], pp = x && PR.Cost.make(x.price, x.amount, x.unit); if (pp && k) prices.set(k, pp); }); })();
  function savePrices() { BUDGET_VER++; var o = {}; prices.forEach(function (v, k) { o[k] = v; }); Store.set('prices', o); }
  var leftovers = (Store.get('leftovers', []) || []).filter(function (e) { return e && e.id && e.title && e.portions > 0 && D.valid(e.useBy); });
  function saveLeftovers() { Store.set('leftovers', leftovers); }
  var cooked = (Store.get('cooked', []) || []).filter(function (c) { return c && D.valid(c.date) && c.recipe; });
  function saveCooked() { Store.set('cooked', cooked); }
  var barcodes = new Map();
  (function () { var o = Store.get('barcodes', {}); if (o && typeof o === 'object') Object.keys(o).forEach(function (k) { if (PR.Barcode.valid(k) && typeof o[k] === 'string') barcodes.set(k, o[k]); }); })();
  function saveBarcodes() { var o = {}; barcodes.forEach(function (v, k) { o[k] = v; }); Store.set('barcodes', o); }
  function currency() { return pref('currency', '$'); }
  function useDefaultPrices() { return pref('useDefaultPrices', true); }
  function leftoverDays() { return PR.Leftovers.clampDays(pref('leftoverDays', PR.Leftovers.DEFAULT_DAYS)); }
  function priceDefaults() { return priceDefaults20(); }
  function money(v) { return PR.Region.money(currency(), v, pref('decimalComma', false) === true); }
  function saveUserRecipes() { Store.set('userRecipes', Array.from(userRecipes.values()).map(PR.recipeToMap)); version++; userVer++; }
  function rating(id) { var r = ratings[id]; return r && (r.stars > 0 || r.note) ? r : null; }
  function setRating(id, stars, note) {
    var cur = ratings[id] || { stars: 0, note: '' };
    if (stars != null) cur.stars = Math.max(0, Math.min(5, stars | 0));
    if (note != null) cur.note = String(note).substring(0, PR.Backup.MAX_NOTE);
    if (cur.stars === 0 && !cur.note) delete ratings[id]; else ratings[id] = cur;
    Store.set('ratings', ratings);
  }
  function savePlan() { Store.set('plan', plan); }
  function saveAvoid() { Store.set('dislikes', dislikes); Store.set('allergens', allergens); version++; }
  function soonKeys() { return PR.UseBy.soonKeys(pantry.filter(function (p) { return p.useBy; }).map(function (p) { return [p.key, p.useBy]; }), D.today()); }
  function qtyMap() { var m = new Map(); pantry.forEach(function (p) { if (p.qty) m.set(p.key, p.qty); }); return m; }
  function daysLeft(p) { return p.useBy ? PR.UseBy.daysLeft(p.useBy, D.today()) : null; }

  function cap(s) { return s ? s.charAt(0).toUpperCase() + s.substring(1) : ''; }
  function savePantry() { Store.set('pantry', pantry); version++; }
  function pantryItem(key) { for (var i = 0; i < pantry.length; i++) if (pantry[i].key === key) return pantry[i]; return null; }
  function addRecent(label) {
    recents = recents.filter(function (r) { return r.toLowerCase() !== label.toLowerCase(); });
    recents.unshift(label);
    if (recents.length > 16) recents.length = 16;
    Store.set('recents', recents);
  }
  function addToPantry(label, staple) {
    var key = PR.normalize(label);
    if (key === '') return false;
    var ex = pantryItem(key);
    if (ex) { if (staple && !ex.staple) { ex.staple = true; savePantry(); } return false; }
    var clean = cap(PR.jtrim(label).replace(/\s+/g, ' '));
    pantry.push({ key: key, label: clean, staple: !!staple, added: Date.now() });
    savePantry();
    if (!staple) addRecent(clean);
    return true;
  }
  function removeFromPantry(key) { pantry = pantry.filter(function (p) { return p.key !== key; }); savePantry(); }
  function setStaple(key, on) { var p = pantryItem(key); if (p) { p.staple = on; savePantry(); } }
  function clearPantry(keepStaples) { pantry = keepStaples ? pantry.filter(function (p) { return p.staple; }) : []; savePantry(); }
  function pantryKeys(staples) { return pantry.filter(function (p) { return p.staple === staples; }).map(function (p) { return p.key; }); }
  function pantrySignature() { return pantry.filter(function (p) { return !p.staple; }).map(function (p) { return p.key + '|'; }).join(''); }

  function isFavorite(id) { return favorites.some(function (f) { return f.id === id; }); }
  function toggleFavorite(id) {
    var on = !isFavorite(id);
    if (on) favorites.push({ id: id, at: Date.now() }); else favorites = favorites.filter(function (f) { return f.id !== id; });
    Store.set('favorites', favorites);
    return on;
  }
  function favoriteIdsNewestFirst() { return favorites.slice().sort(function (a, b) { return b.at - a.at; }).map(function (f) { return f.id; }); }

  function mergeRecipe(a, b) {
    if (!a) return b || '';
    if (!b || a.indexOf(b) >= 0) return a;
    return a + ', ' + b;
  }
  function saveShopping() { Store.set('shopping', shopping); }
  function addShopping(name, amount, unit, recipe) {
    var key = PR.normalize(name);
    if (key === '') return false;
    unit = unit || '';
    var amt = amount == null || isNaN(amount) ? null : amount;
    for (var n = 0; n < shopping.length; n++) {
      var s = shopping[n];
      if (s.key !== key || s.checked) continue;
      if (s.amount != null && amt != null && PR.eqIC(unit, s.unit)) { s.amount += amt; s.recipe = mergeRecipe(s.recipe, recipe); saveShopping(); return true; }
      if (s.amount == null && amt == null) return false;
    }
    shopping.push({ id: Date.now() + '-' + Math.random().toString(36).slice(2, 7), key: key, name: cap(PR.jtrim(name)), amount: amt, unit: unit, recipe: recipe || '', checked: false, added: Date.now() + shopping.length });
    saveShopping();
    return true;
  }
  function shoppingOpenCount() { return shopping.filter(function (s) { return !s.checked; }).length; }
  function markSeen(id) { if (!seen.has(id)) { seen.add(id); Store.set('seen', Array.from(seen)); } }
  function setAccepted(key, on) { if (on) accepted.add(key); else accepted.delete(key); Store.set('acceptedSubs', Array.from(accepted)); version++; }
  function defaultServings() { return pref('servings', 4); }

  function onlineEnabled() { return pref('online', true); }
  function apiKey() { return pref('apiKey', '1') || '1'; }
  function isOnline() { return navigator.onLine !== false; }
  /** Every recipe the app can show, without the ones hidden by dislikes/allergies (name-based, see Allergens.NOTE). */
  var allMemo = { v: -1, list: [], hidden: 0 };
  var bundledKeys = null;
  function allRecipes() {
    if (allMemo.v === version) return allMemo.list;
    var out = [], hid = 0, avoidA = avoidAllergens(), any = dislikes.length || avoidA.length;
    var add = function (r) { if (any && PR.Allergens.hidden(r, dislikes, avoidA)) hid++; else out.push(r); };
    userRecipes.forEach(add);
    bundled.forEach(add);
    if (onlineEnabled()) {
      if (!bundledKeys) { bundledKeys = new Set(); bundled.forEach(function (r) { bundledKeys.add(PR.Titles.key(r.title)); }); }
      online.forEach(function (r) { if (!bundledKeys.has(PR.Titles.key(r.title))) add(r); }); // v1.8: same title as a built-in one = shown once
    }
    allMemo = { v: version, list: out, hidden: hid };
    return out;
  }
  function hiddenByAvoid() { allRecipes(); return allMemo.hidden; }
  function byId(id) {
    if (id == null) return null;
    if (userRecipes.has(id)) return userRecipes.get(id);
    if (online.has(id)) return online.get(id);
    for (var i = 0; i < bundled.length; i++) if (bundled[i].id === id) return bundled[i];
    return null;
  }

  var namesCache = null, namesVersion = -1;
  function ingredientNames() {
    if (namesCache && namesVersion === version) return namesCache;
    var byKey = new Map();
    C.allItems().forEach(function (s) { byKey.set(PR.normalize(s), s); });
    pantry.forEach(function (p) { if (!byKey.has(p.key)) byKey.set(p.key, p.label); });
    var src = bundled.slice();
    online.forEach(function (r) { src.push(r); });
    userRecipes.forEach(function (r) { src.push(r); });
    src.forEach(function (r) {
      r.ingredients.forEach(function (i) {
        if (i.key === '' || byKey.has(i.key)) return;
        var n = i.name, comma = n.indexOf(',');
        if (comma > 0) n = n.substring(0, comma);
        byKey.set(i.key, cap(PR.jtrim(n)));
      });
    });
    var out = Array.from(byKey.values());
    C.ALIASES.forEach(function (s) { if (out.indexOf(s) < 0) out.push(s); });
    out.sort(PR.cmpIC);
    namesCache = out; namesVersion = version;
    return out;
  }

  // matcher + results memo
  var memo = { v: -1, m: null, all: null };
  function matcher() {
    if (memo.v !== version || !memo.m) {
      memo.m = new PR.Matcher(pantryKeys(false), pantryKeys(true)).withSubstitutions(subs, Array.from(accepted));
      memo.v = version; memo.all = null;
    }
    return memo.m;
  }
  function matchAll() {
    var m = matcher();
    if (!memo.all) memo.all = allRecipes().map(function (r) { return m.match(r); });
    return memo.all;
  }
  function resultFor(r) {
    var all = matchAll();
    for (var i = 0; i < all.length; i++) if (all[i].recipe === r) return all[i];
    return matcher().match(r);
  }

  // =====================================================================
  // TheMealDB (online, optional)
  // =====================================================================
  var net = { fetching: false, lastError: '', lastAdded: -1, lastSig: pref('fetchSig', ''), browsing: false, namesBusy: {} };
  function apiGet(path) {
    var url = API_BASE + encodeURIComponent(apiKey()) + '/' + path;
    var ctl = typeof AbortController !== 'undefined' ? new AbortController() : null;
    var timer = setTimeout(function () { if (ctl) ctl.abort(); }, 12000);
    var opts = { headers: { Accept: 'application/json' }, credentials: 'omit', referrerPolicy: 'no-referrer' };
    if (ctl) opts.signal = ctl.signal;
    return fetch(url, opts)
      .then(function (res) { clearTimeout(timer); if (!res.ok) throw new Error('HTTP ' + res.status); return res.text(); },
        function (e) { clearTimeout(timer); throw e; })
      .then(function (t) { return t ? JSON.parse(t) : {}; });
  }
  function idsFrom(o) { return o && Array.isArray(o.meals) ? o.meals.map(function (m) { return String(m.idMeal); }) : []; }
  function idsForIngredient(ing) { return apiGet('filter.php?i=' + encodeURIComponent(PR.jtrim(ing).toLowerCase().replace(/ /g, '_'))).then(idsFrom); }
  function filterIds(kind, value) { return apiGet('filter.php?' + kind + '=' + encodeURIComponent(PR.jtrim(value).replace(/ /g, '_'))).then(idsFrom); }
  function lookupMeal(id) {
    return apiGet('lookup.php?i=' + encodeURIComponent(id)).then(function (o) {
      return o && Array.isArray(o.meals) && o.meals.length ? PR.convertMeal(o.meals[0]) : null;
    });
  }
  function cacheRecipe(r) {
    online.delete(r.id);
    online.set(r.id, r);
    onlineFetched.set(r.id, Date.now());
    version++;
    // keep the cache bounded (400 newest), but never drop favorites
    var ids = Array.from(onlineFetched.keys()).sort(function (a, b) { return onlineFetched.get(b) - onlineFetched.get(a); });
    var drop = [];
    ids.slice(400).forEach(function (id) { if (!isFavorite(id)) { drop.push(id); online.delete(id); onlineFetched.delete(id); } });
    Cache.put([{ id: r.id, json: JSON.stringify(PR.recipeToMap(r)), fetched: onlineFetched.get(r.id) }], drop);
  }
  /** Runs fn over items with a few requests in flight at a time. */
  function pool(items, fn) {
    var idx = 0;
    function worker() {
      if (idx >= items.length) return Promise.resolve();
      var it = items[idx++];
      return Promise.resolve().then(function () { return fn(it); }).then(null, function () { }).then(worker);
    }
    return Promise.all([worker(), worker(), worker()]);
  }
  function needsFetch() { var s = pantrySignature(); return onlineEnabled() && s !== '' && s !== net.lastSig; }
  function fetchForPantry() {
    if (net.fetching || !onlineEnabled()) return;
    if (!isOnline()) { net.lastError = 'offline'; refresh(); return; }
    var items = pantry.slice().reverse().filter(function (p) { return !p.staple; }).slice(0, 5);
    if (!items.length) return;
    var sig = pantrySignature();
    net.fetching = true; net.lastError = ''; refresh();
    var score = new Map(), order = new Map(), anyOk = false, added = 0;
    var chain = Promise.resolve();
    items.forEach(function (p) {
      chain = chain.then(function () {
        return idsForIngredient(p.label).then(function (ids) {
          if (!ids.length && !PR.eqIC(p.key, p.label)) return idsForIngredient(p.key);
          return ids;
        }).then(function (ids) {
          anyOk = true;
          ids.forEach(function (id) { score.set(id, (score.get(id) || 0) + 1); if (!order.has(id)) order.set(id, order.size); });
        }, function () { /* skip this ingredient */ });
      });
    });
    chain.then(function () {
      var ranked = Array.from(score.keys()).sort(function (a, b) { return (score.get(b) - score.get(a)) || (order.get(a) - order.get(b)); });
      var todo = [];
      for (var i = 0; i < ranked.length && todo.length < 24; i++) if (!online.has('m:' + ranked[i])) todo.push(ranked[i]);
      return pool(todo, function (id) {
        return lookupMeal(id).then(function (r) {
          if (r && r.ingredients.length) { cacheRecipe(r); added++; if (added % 6 === 0) refresh(); }
        });
      });
    }).then(function () {
      if (anyOk) { net.lastSig = sig; setPref('fetchSig', sig); } else net.lastError = 'TheMealDB could not be reached';
    }).then(function () { net.lastAdded = added; net.fetching = false; refresh(); }, function () { net.lastAdded = added; net.fetching = false; refresh(); });
  }
  function searchOnline(q) {
    if (!isOnline()) return Promise.resolve({ results: [], error: "You're offline" });
    return apiGet('search.php?s=' + encodeURIComponent(PR.jtrim(q))).then(function (o) {
      var res = (o && Array.isArray(o.meals) ? o.meals : []).map(PR.convertMeal).filter(Boolean);
      res.forEach(cacheRecipe);
      return { results: res, error: null };
    }, function () { return { results: [], error: 'TheMealDB could not be reached' }; });
  }
  function mealDbNames(areas) {
    var pk = areas ? 'mdbAreas' : 'mdbCats';
    var out = Store.get(pk, []), at = Store.get(pk + 'At', 0);
    if (onlineEnabled() && isOnline() && (!out.length || Date.now() - at > 7 * 24 * 3600 * 1000) && !net.namesBusy[pk]) {
      net.namesBusy[pk] = true;
      apiGet(areas ? 'list.php?a=list' : 'categories.php').then(function (o) {
        var arr = o && Array.isArray(areas ? o.meals : o.categories) ? (areas ? o.meals : o.categories) : [], names = [];
        arr.forEach(function (x) {
          var n = PR.jtrim(String((areas ? x.strArea : x.strCategory) || ''));
          if (n && !PR.eqIC(n, 'unknown') && names.indexOf(n) < 0) names.push(n);
        });
        if (names.length) { Store.set(pk, names); Store.set(pk + 'At', Date.now()); refresh(); }
      }, function () { }).then(function () { net.namesBusy[pk] = false; });
    }
    return out;
  }
  function browseKey(kind, value) { return 'browse_' + kind + '_' + value.toLowerCase(); }
  function cachedBrowse(kind, value) {
    if (!onlineEnabled()) return [];
    return Store.get(browseKey(kind, value), []).map(function (id) { return online.get('m:' + id); }).filter(Boolean);
  }
  function browseOnline(kind, value, max) {
    if (!onlineEnabled()) return Promise.resolve({ added: 0, remaining: 0, error: 'Online recipes are turned off' });
    if (!isOnline()) return Promise.resolve({ added: 0, remaining: 0, error: "You're offline \u2013 showing saved recipes" });
    if (net.browsing) return Promise.resolve({ added: 0, remaining: 0, error: null });
    net.browsing = true;
    var added = 0, remaining = 0;
    return filterIds(kind, value).then(function (ids) {
      Store.set(browseKey(kind, value), ids);
      var todo = [];
      ids.forEach(function (id) { if (online.has('m:' + id)) return; if (todo.length >= max) remaining++; else todo.push(id); });
      return pool(todo, function (id) {
        return lookupMeal(id).then(function (r) { if (r && r.ingredients.length) { cacheRecipe(r); added++; } });
      }).then(function () { return { added: added, remaining: remaining, error: null }; });
    }, function () { return { added: 0, remaining: 0, error: 'TheMealDB could not be reached' }; })
      .then(function (r) { net.browsing = false; return r; });
  }
  function randomOnline() {
    if (!onlineEnabled() || !isOnline()) return Promise.resolve({ r: null, error: 'offline' });
    return apiGet('random.php').then(function (o) {
      var r = o && Array.isArray(o.meals) && o.meals.length ? PR.convertMeal(o.meals[0]) : null;
      if (r && r.ingredients.length) cacheRecipe(r); else r = null;
      return { r: r, error: null };
    }, function () { return { r: null, error: 'TheMealDB could not be reached' }; });
  }

  // =====================================================================
  // UI helpers
  // =====================================================================
  function $(sel, root) { return (root || document).querySelector(sel); }
  function $$(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }
  var ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) { return ESC[c]; }); }
  function ic(name, cls) {
    var d = ICONS[name];
    if (!d) return '';
    return '<svg class="i ' + (cls || '') + '" viewBox="' + d.vb + '" aria-hidden="true">' + d.d.map(function (p) { return '<path d="' + p + '"/>'; }).join('') + '</svg>';
  }
  function plural(n, one, many) { return n + ' ' + (n === 1 ? one : many); }
  function timeText(m) {
    if (m < 0) return 'Time not listed';
    if (m < 60) return m + ' min';
    var h = Math.floor(m / 60), mm = m % 60;
    return mm === 0 ? h + ' h' : h + ' h ' + mm + ' min';
  }
  var DIET_LABELS = { vegetarian: 'Vegetarian', vegan: 'Vegan', 'gluten-free': 'Gluten-free', 'dairy-free': 'Dairy-free' };
  function dietLabel(t) { return DIET_LABELS[t] || PR.Diets.name(t); }
  function javaHash(s) { var h = 0; for (var i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0; return h; }
  var CAT_COLORS = { Breakfast: '#F6C453', Soup: '#EF8354', Salad: '#7BC67E', Side: '#9CCC65', Snack: '#FFB74D', Dessert: '#F48FB1', Baking: '#D7A86E', Main: '#4DB6AC', Drink: '#64B5F6', Sauce: '#E57373' };
  function catColor(c) {
    c = c || '';
    if (CAT_COLORS[c]) return CAT_COLORS[c];
    var cols = ['#4DB6AC', '#81C784', '#FFB74D', '#9575CD', '#64B5F6', '#E57373'];
    return cols[Math.abs(javaHash(c)) % cols.length];
  }
  function blend(a, b, t) {
    var pa = parseInt(a.slice(1), 16), pb = parseInt(b.slice(1), 16);
    var r = ((pa >> 16) & 255), g = ((pa >> 8) & 255), bl = pa & 255;
    var r2 = ((pb >> 16) & 255), g2 = ((pb >> 8) & 255), b2 = pb & 255;
    var h = function (x) { return ('0' + Math.round(x).toString(16)).slice(-2); };
    return '#' + h(r + (r2 - r) * t) + h(g + (g2 - g) * t) + h(bl + (b2 - bl) * t);
  }
  function thumb(r, cls) {
    var c = catColor(r.category);
    // v1.8: a flat letter tile; no photos in lists (a recipe's own page still shows its photo)
    return '<div class="' + cls + '" style="background:' + c + '"><span aria-hidden="true">' + esc(r.title.charAt(0)) + '</span></div>';
  }
  function bar(res) {
    var pct = res.total ? Math.round(100 * res.have / res.total) : 0;
    return '<div class="bar" role="img" aria-label="' + res.have + ' of ' + res.total + '"><i style="width:' + pct + '%"></i></div>';
  }
  function missingNames(res) { return res.missing.map(function (i) { return i.name.split(',')[0].toLowerCase(); }).join(', '); }
  function mealsOf(r) { return r.meals && r.meals.length ? r.meals : PR.inferMeals(r.category, r.title); }
  function mealPasses(r) { return PR.mealPasses(mealsOf(r), ui.meals, ui.incDesserts, ui.incSides); }
  function mealHidden(r) { return !mealPasses(r) && PR.mealPasses(mealsOf(r), ui.meals, true, true); }
  // v2.0.1: Discover keeps its own meal-type filter and never reads the Recipes tab filters (meal types, diets, time, methods, mode, search)
  function dMealPasses(r) { return PR.mealPasses(mealsOf(r), ui.dMeals, ui.dIncDesserts, ui.dIncSides); }
  function dMealHidden(r) { return !dMealPasses(r) && PR.mealPasses(mealsOf(r), ui.dMeals, true, true); }
  function mealText(r, max) { return mealsOf(r).slice(0, max).join(' \u00b7 '); }
  function suggestable(items) {
    var out = items.filter(function (x) { return PR.mealPreferred(mealsOf(x.recipe), ui.meals, ui.incDesserts, ui.incSides); });
    if (!out.length) out = items.filter(function (x) { return PR.mealAcceptable(mealsOf(x.recipe), ui.meals, ui.incDesserts, ui.incSides); });
    return out;
  }
  /** Include desserts / Include sides check boxes plus multi-select meal-type chips. disc = Discover's own state (v2.0.1). */
  function mealBar(disc) {
    var p = disc === true ? 'd' : '', ms = disc === true ? ui.dMeals : ui.meals, de = disc === true ? ui.dIncDesserts : ui.incDesserts, si = disc === true ? ui.dIncSides : ui.incSides;
    var D = function (k) { return p ? p + k.charAt(0).toUpperCase() + k.substring(1) : k; };
    return '<div class="mealopts"><label class="mcheck"><input type="checkbox" data-change="' + D('incDesserts') + '"' + (de ? ' checked' : '') + '> Include desserts</label>' +
      '<label class="mcheck"><input type="checkbox" data-change="' + D('incSides') + '"' + (si ? ' checked' : '') + '> Include sides</label></div>' +
      '<div class="hscroll"><div class="chips nowrap mealchips">' + chip('All meal types', !ms.length, D('mealAll')) +
      PR.MEAL_TYPES.map(function (m) { return chip(esc(m), ms.indexOf(m) >= 0, D('mealType'), ' data-v="' + esc(m) + '"'); }).join('') + '</div></div>';
  }
  function saveMeals() { setPref('meals', ui.meals); setPref('incDesserts', ui.incDesserts); setPref('incSides', ui.incSides); setPref('dMeals', ui.dMeals); setPref('dIncDesserts', ui.dIncDesserts); setPref('dIncSides', ui.dIncSides); ui.listLimit = PAGE; }

  var toastTimer = null;
  function toast(msg) {
    var t = $('#toast');
    if (!t) { t = document.createElement('div'); t.id = 'toast'; t.className = 'toast'; t.setAttribute('role', 'status'); document.body.appendChild(t); }
    t.textContent = msg; t.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.hidden = true; }, 2600);
  }

  // dialogs / action sheets
  var dlgHandlers = {};
  function closeDialog() { var d = $('#dlg'); if (d) d.remove(); dlgHandlers = {}; }
  function dialog(opts, handlers) {
    closeDialog();
    dlgHandlers = handlers || {};
    var s = document.createElement('div');
    s.className = 'scrim'; s.id = 'dlg';
    var btns = (opts.buttons || [{ label: 'OK' }]).map(function (b, n) {
      dlgHandlers['b' + n] = b.fn || null;
      return '<button class="btn ' + (b.primary ? '' : 'ghost') + '" data-a="dlg" data-n="b' + n + '">' + esc(b.label) + '</button>';
    }).join('');
    s.innerHTML = '<div class="dlg' + (opts.sheet ? ' sheet' : '') + '" role="dialog" aria-modal="true">' + (opts.title ? '<h3>' + esc(opts.title) + '</h3>' : '') +
      (opts.html || '') + (opts.sheet ? '' : '<div class="acts">' + btns + '</div>') + '</div>';
    document.body.appendChild(s);
  }
  function confirmBox(title, text, okLabel, fn) {
    dialog({ title: title, html: '<p class="muted">' + esc(text) + '</p>', buttons: [{ label: 'Cancel' }, { label: okLabel, primary: true, fn: fn }] });
  }
  function sheet(title, items) {
    var handlers = {};
    var html = items.map(function (it, n) {
      handlers['s' + n] = it.fn;
      return '<button class="it' + (it.red ? ' red' : '') + '" data-a="dlg" data-n="s' + n + '">' + ic(it.icon || 'check') + '<span>' + esc(it.label) + '</span></button>';
    }).join('');
    dialog({ title: title, sheet: true, html: html }, handlers);
  }

  // =====================================================================
  // App state, navigation
  // =====================================================================
  var PAGE = 30; // v1.8: recipe rows rendered per page (was 60); fewer DOM nodes on a Fire HD 8
  var TABS = [
    // v1.8: pill-shaped segmented tabs in this order; Favorites is a chip under Recipes, Plan is a tab
    { id: 'pantry', label: 'Pantry', icon: 'kitchen', sub: "What's in your kitchen?" },
    { id: 'recipes', label: 'Recipes', icon: 'restaurant', sub: 'Matched to your pantry' },
    { id: 'discover', label: 'Discover', icon: 'explore', sub: 'Find something new to cook' },
    { id: 'plan', label: 'Plan', icon: 'calendar', sub: 'Your meal plan' },
    { id: 'shopping', label: 'Shopping', icon: 'cart', sub: 'Missing ingredients to buy' }
  ];
  var FAV_TAB = { id: 'favorites', label: 'Favorites', icon: 'heart', sub: 'Recipes you love' };
  function tabInfo(id) { return id === 'favorites' ? FAV_TAB : TABS.filter(function (t) { return t.id === id; })[0]; }
  var ui = {
    tab: pref('tab', 'pantry'), query: '', mode: pref('mode', 0), diets: pref('diets', []), maxTime: pref('maxTime', 0),
    addText: '', quick: '', dq: '', stack: [], listLimit: PAGE, detailUnits: (prefs.detailUnits && PR.Units.MODES.indexOf(prefs.detailUnits) >= 0) ? prefs.detailUnits : null,
    meals: PR.mealSorted(pref('meals', []).filter(PR.isMealType)), incDesserts: pref('incDesserts', false), incSides: pref('incSides', false),
    dMeals: PR.mealSorted(pref('dMeals', []).filter(PR.isMealType)), dIncDesserts: pref('dIncDesserts', false), dIncSides: pref('dIncSides', false)
  };
  if (!tabInfo(ui.tab)) ui.tab = 'pantry';
  ui.planS = { type: 'plan', tabbed: true, week: wkStart(D.today()) };

  function shell() {
    document.body.innerHTML =
      '<div class="app" id="app">' +
      '<header class="top brandbar"><span class="hsp" aria-hidden="true"></span><div class="brand">' + (window.PR_LOGO || ic('leaf')) +
      '<h1>My Pantry Recipes <button class="ver" data-a="about" aria-label="Version ' + esc(VERSION) + ', open About">' + esc(SHORT_VERSION) + '</button></h1><div class="sub vh" id="subtitle"></div></div>' +
      '<span class="hbtns"><button class="iconbtn" data-a="subs" aria-label="Substitutions" title="Substitutions">' + ic('swap') + '</button>' +
      '<button class="iconbtn" data-a="about" aria-label="Settings and about" title="Settings &amp; about">' + ic('info') + '</button></span></header>' +
      '<nav class="tabs seg" id="tabs" aria-label="Sections"></nav>' +
      '<main class="main" id="main"></main></div>';
  }
  function renderTabs() {
    var shown = ui.tab === 'favorites' ? 'recipes' : ui.tab;
    var html = TABS.map(function (t) {
      var label = t.label;
      if (t.id === 'shopping') { var n = shoppingOpenCount(); if (n > 0) label += ' <span class="n">' + n + '</span>'; }
      return '<button class="tab' + (shown === t.id ? ' on' : '') + '" data-a="tab" data-tab="' + t.id + '"' + (shown === t.id ? ' aria-current="page"' : '') + '>' + label + '</button>';
    }).join('');
    var nav = $('#tabs');
    if (nav.__h !== html) { nav.innerHTML = html; nav.__h = html; } // v1.8: unchanged tabs are not re-parsed
    $('#subtitle').textContent = tabInfo(ui.tab).sub;
  }
  function showTab(id) {
    ui.tab = id; setPref('tab', id);
    renderTabs();
    if (!loaded) { if (earlyTab && (id === 'pantry' || id === 'shopping') && ui.stack.length === 0) { earlyTab = id; $('#main').innerHTML = '<div class="wrap">' + VIEWS[id].full() + '</div>'; } else $('#main').innerHTML = '<div class="loading"><span class="spin"></span> Loading recipes\u2026</div>'; return; }
    var v = VIEWS[id];
    $('#main').innerHTML = '<div class="wrap">' + v.full() + '</div>';
    $('#main').scrollTop = 0;
    if ((id === 'recipes' || id === 'discover') && needsFetch() && isOnline()) fetchForPantry();
  }
  /** Re-renders the dynamic regions of the current tab and the top screen (inputs keep focus). */
  function refresh() {
    if (!loaded && !earlyOk()) return;
    renderTabs();
    var v = VIEWS[ui.tab];
    $$('#main [data-region]').forEach(function (el) {
      var f = v.regions[el.getAttribute('data-region')];
      if (f && el.getAttribute('data-region') !== 'ac') el.innerHTML = f();
    });
    var top = ui.stack[ui.stack.length - 1];
    if (top) renderScreen(top, true);
  }
  function region(name, view) { return '<div data-region="' + name + '">' + view.regions[name]() + '</div>'; }

  // ---- screens (detail, lists, substitutions, about, cook) layered over the tabs, with history support ----
  function pushScreen(s) {
    var below = ui.stack[ui.stack.length - 1], bm = below && $('.main', below.el);
    if (bm) below.scrollY = bm.scrollTop;            // remembered so going back returns to the same place
    ui.stack.push(s);
    var el = document.createElement('div');
    el.className = 'screen' + (s.type === 'cook' ? ' cook' : '');
    document.body.appendChild(el);
    s.el = el;
    renderScreen(s, false);
    try { history.pushState({ depth: ui.stack.length }, ''); } catch (e) { }
  }
  function popScreen() {
    var s = ui.stack.pop();
    if (!s) return;
    if (s.onClose) s.onClose();
    s.el.parentNode.removeChild(s.el);
    closeDialog();
    var top = ui.stack[ui.stack.length - 1];
    if (top) renderScreen(top, false); else refresh();   // renderScreen keeps top's scroll position
  }
  function back() {
    if (!ui.stack.length) return;
    var st = history.state;
    if (st && st.depth === ui.stack.length) history.back(); else popScreen();
  }
  window.addEventListener('popstate', function (e) {
    var depth = e.state && e.state.depth ? e.state.depth : 0;
    if ($('#dlg')) closeDialog();
    while (ui.stack.length > depth) popScreen();
  });
  /** Re-renders a screen. The scroll position is always kept: a partial render (e.g. picking a theme) keeps it,
   *  and a full render of a screen that is already showing restores where it was (or s.scrollY when coming back). */
  function renderScreen(s, partial) {
    var v = SCREENS[s.type], keep = $('.main', s.el), y = keep ? keep.scrollTop : (s.scrollY || 0);
    if (partial && v.regions) {
      $$('[data-region]', s.el).forEach(function (el) { var f = v.regions[el.getAttribute('data-region')]; if (f) el.innerHTML = f(s); });
      var m0 = $('.main', s.el);
      if (m0 && m0.scrollTop !== y) m0.scrollTop = y;
      if (v.after) v.after(s);
      return;
    }
    if (s.scrollY != null && !keep) y = s.scrollY;
    s.el.innerHTML = v.full(s);
    var m = $('.main', s.el);
    if (m) m.scrollTop = s.resetScroll ? 0 : y;
    s.resetScroll = false; s.scrollY = null;
    if (v.after) v.after(s);
  }
  function topBar(title, sub, right) {
    return '<header class="top"><button class="iconbtn back" data-a="back" aria-label="Back">' + ic('back') + '</button><div class="ttl"><h1>' + esc(title) + '</h1>' +
      (sub ? '<div class="sub">' + esc(sub) + '</div>' : '') + '</div>' + (right || '') + '</header>';
  }
  function sregion(name, s) { return '<div data-region="' + name + '">' + SCREENS[s.type].regions[name](s) + '</div>'; }

  // =====================================================================
  // Recipe row / card renderers
  // =====================================================================
  function starsText(id) { var rt = rating(id); return rt && rt.stars ? '<span class="rstars" aria-label="' + rt.stars + ' of 5 stars">' + '\u2605'.repeat(rt.stars) + '<i>' + '\u2605'.repeat(5 - rt.stars) + '</i></span>' : ''; }
  var soonMemo = { v: -1, day: '', keys: [], m: null };
  function soonMatcher() {
    var day = D.today();
    if (soonMemo.v !== version || soonMemo.day !== day) { var k = soonKeys(); soonMemo = { v: version, day: day, keys: k, m: k.length ? new PR.Matcher(k, []) : null }; }
    return soonMemo;
  }
  function soonCount(r) { var sm = soonMatcher(); return sm.m ? PR.UseBy.count(r, sm.m) : 0; }
  function recipeRow(res) {
    var r = res.recipe, meta = [];
    if (r.cuisine) meta.push(esc(r.cuisine));
    var tm = PR.totalMinutes(r);
    meta.push(tm < 0 ? 'time not listed' : timeText(tm));
    var diet = r.tags.indexOf('vegan') >= 0 ? 'vegan' : r.tags.indexOf('vegetarian') >= 0 ? 'vegetarian' : '';
    if (diet) meta.push(diet + (r.tagsInferred ? '?' : ''));
    var miss = res.missing.length === 0
      ? '<div class="miss ok">' + (res.haveNonStaple > 0 ? 'Ready to cook \u2013 you have everything' : 'Only needs staples') + '</div>'
      : '<div class="miss">Missing: ' + esc(missingNames(res)) + '</div>';
    var th = titleHint(r.id);
    if (th) meta.unshift(esc(th));
    if (r.servings > 0) meta.push('serves ' + r.servings);
    if (priceUnknown(r)) meta.push('price unknown');
    return '<button class="rrow" data-a="open" data-id="' + esc(r.id) + '">' + thumb(r, 'thumb') +
      '<div class="body"><div class="tl"><div class="title">' + esc(r.title) + (PR.isOnline(r) ? '<span class="badge">THEMEALDB</span>' : '') + (isUser(r) ? '<span class="badge mine">MINE</span>' : '') + '</div>' +
      (isFavorite(r.id) ? '<span style="color:var(--heart)">' + ic('heart', 's') + '</span>' : '') + '</div>' +
      '<div class="mtypes">' + esc(mealText(r, 3)) + '</div><div class="meta">' + starsText(r.id) + meta.join(' \u00b7 ') + '</div>' + bar(res) +
      (soonCount(r) ? '<div class="usesoon">' + ic('timer', 'xs') + 'Uses ' + plural(soonCount(r), 'item', 'items') + ' to use soon</div>' : '') +
      '<div class="have">You have ' + res.have + ' of ' + res.total + (decoOn() && res.total ? ' \u00b7 ' + Math.round(100 * res.have / res.total) + '%' : '') + '</div>' + miss + '</div>' +
      '<span class="cookb" data-a="cookRow" data-id="' + esc(r.id) + '" aria-hidden="true">Cook</span></button>';
  }
  // v1.8: near-identical titles get a short hint ("with lemon", "Thai", "45 min"); computed once when the browser is idle
  var hintsMemo = { n: -1, map: null, pending: false };
  function titleHint(id) {
    if (!loaded) return null;
    var n = bundled.length + userRecipes.size;
    if (hintsMemo.n !== n && !userRecipes.size && window.PR_CORE_DATA && PR_CORE_DATA.titleHints) {
      var pre = new Map(); Object.keys(PR_CORE_DATA.titleHints).forEach(function (k) { pre.set(k, PR_CORE_DATA.titleHints[k]); });
      hintsMemo = { n: n, map: pre, pending: false }; // built-in recipes only: precomputed at build time (same as Titles.java)
    }
    if (hintsMemo.n !== n && !hintsMemo.pending) {
      hintsMemo.pending = true;
      idle(function () {
        var list = Array.from(userRecipes.values()).concat(bundled);
        hintsMemo = { n: bundled.length + userRecipes.size, map: PR.Titles.hints(list), pending: false };
        if (ui.tab === 'recipes' || ui.tab === 'favorites') refreshRegion('list');
      });
    }
    return hintsMemo.map ? hintsMemo.map.get(id) || null : null;
  }
  function idle(fn) { if (window.requestIdleCallback) requestIdleCallback(fn, { timeout: 1500 }); else setTimeout(fn, 200); }
  function decoOn() { return pref('decorations', true) !== false; }
  function recipeCard(res) {
    var r = res.recipe, m;
    if (res.haveNonStaple === 0) m = res.total + ' ingredients';
    else if (res.missing.length === 0) m = 'Cook now';
    else m = 'Missing ' + res.missing.length;
    var tm = PR.totalMinutes(r);
    if (tm > 0) m += ' \u00b7 ' + timeText(tm);
    var ok = res.missing.length === 0 && res.haveNonStaple > 0;
    return '<button class="ccard" data-a="open" data-id="' + esc(r.id) + '">' + thumb(r, 'thumb') +
      '<div class="cb"><div class="title">' + esc(r.title) + '</div><div class="meta"' + (ok ? ' style="color:var(--green-dark)"' : '') + '>' + m + '</div><div class="mtypes">' + esc(mealText(r, 2)) + '</div></div></button>';
  }
  function isUser(r) { return r.id.indexOf('u:') === 0; }
  function emptyBox(icon, title, text, btn) {
    return '<div class="empty"><div class="ic">' + ic(icon) + '</div><div class="big">' + esc(title) + '</div><div class="muted">' + esc(text) + '</div>' + (btn || '') + '</div>';
  }
  function chip(label, on, act, extra, cls) {
    return '<button class="chip' + (on ? ' on' : '') + (cls ? ' ' + cls : '') + '" data-a="' + act + '"' + (extra || '') + ' aria-pressed="' + (on ? 'true' : 'false') + '">' + label + '</button>';
  }

  // =====================================================================
  // Pantry tab
  // =====================================================================
  function isIOS() { return /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1); }
  function isStandalone() { return window.navigator.standalone === true || (window.matchMedia && window.matchMedia('(display-mode: standalone)').matches); }
  var SHARE_SVG = '<svg class="i xs" viewBox="0 0 24 24" aria-label="Share icon"><path d="M12 2l4.5 4.5-1.4 1.4L13 5.8V15h-2V5.8L8.9 7.9 7.5 6.5 12 2zM5 10h3v2H6v8h12v-8h-2v-2h3a1 1 0 011 1v10a1 1 0 01-1 1H5a1 1 0 01-1-1V11a1 1 0 011-1z"/></svg>';
  function bannersHtml() {
    var out = v20Banners();
    if (!Store.persistent) out += '<div class="banner warn">' + ic('info') + '<div class="grow muted">This browser is blocking storage for this page, so your pantry, favorites and shopping list will be forgotten when you close it. The hosted (https) version saves them.</div></div>';
    if (isIOS() && !isStandalone() && /^https?:$/.test(location.protocol) && !pref('iosHintDone', false))
      out += '<div class="banner" id="ioshint">' + ic('leaf') + '<div class="grow"><b>Add to your Home Screen</b><div class="muted">In Safari tap <b>Share</b> ' + SHARE_SVG +
        ' then <b>Add to Home Screen</b>. It opens full-screen and works offline.</div></div><button class="iconbtn" data-a="ioshint" aria-label="Dismiss">' + ic('close', 's') + '</button></div>';
    return out;
  }
  function pantryExtra(p) {
    var x = [];
    if (p.qty) x.push(esc(p.qty));
    var d = daysLeft(p);
    if (d != null && d !== -2147483648) x.push(d < 0 ? 'past use-by' : d === 0 ? 'use today' : d + 'd');
    return x.length ? ' <span class="cx">' + x.join(' \u00b7 ') + '</span>' : '';
  }
  function itemChip(have, label) {
    var k = PR.normalize(label), p = have.get(k);
    return '<button class="chip' + (p ? (p.staple ? ' staple' : ' have') : '') + '" data-a="toggleItem" data-label="' + esc(label) + '" aria-pressed="' + !!p + '">' + (p ? ic('check', 'xs') : '') + esc(label) + '</button>';
  }
  var Pantry = {
    full: function () {
      return region('banners', Pantry) +
        '<div class="card"><h2>Add what you have</h2><div class="muted" style="margin-bottom:10px">Type an ingredient, or tap the common ones below.</div>' +
        '<div class="row"><div class="field grow">' + ic('search', 's') +
        '<input type="text" id="addIn" data-in="add" data-enter="addPantry" placeholder="e.g. eggs, spinach, rice" autocomplete="off" autocorrect="off" autocapitalize="none" spellcheck="false" enterkeyhint="done" aria-label="Add ingredients">' +
        '<div class="ac" data-region="ac" hidden></div></div>' +
        '<button class="btn" data-a="addPantry">' + ic('add', 's') + 'Add</button><button class="btn alt" data-a="scan" aria-label="Scan or type a barcode" title="Barcode">' + ic('barcode', 's') + '<span class="hidesm">Barcode</span></button></div></div>' +
        region('leftovers', Pantry) + region('soon', Pantry) + region('mine', Pantry) +
        '<div class="card"><h2>Quick add</h2><div class="muted" style="margin-bottom:10px">Tap to add or remove. Sections show how many you have.</div>' +
        '<div class="field" style="margin-bottom:6px">' + ic('search', 's') + '<input type="search" id="quickIn" data-in="quick" placeholder="Filter ingredients, e.g. cheese" autocomplete="off" autocorrect="off" autocapitalize="none" spellcheck="false" enterkeyhint="search" aria-label="Filter ingredients"></div>' +
        region('grid', Pantry) + '</div>';
    },
    regions: {
      banners: bannersHtml,
      ac: function () { return ''; },
      leftovers: function () {
        if (!leftovers.length) return '';
        var t = D.today();
        return '<div class="card lefto"><h2>' + ic('restaurant', 's') + ' Leftovers</h2>' + PR.Leftovers.sorted(leftovers).map(function (e) {
          var d = PR.UseBy.daysLeft(e.useBy, t), st = PR.Leftovers.status(e, t);
          return '<div class="lrow"><button class="grow left plink" data-a="loOpen" data-id="' + esc(e.recipe) + '">' + esc(e.title) + '<span class="faint">Cooked ' + esc(D.label(e.cooked)) + '</span></button>' +
            '<span class="ubadge ' + st + '">' + esc(PR.UseBy.label(d)) + '</span>' +
            '<div class="stepper"><button class="iconbtn" data-a="loAdj" data-id="' + esc(e.id) + '" data-d="-1" aria-label="One portion eaten">' + ic('remove', 's') + '</button><b title="portions">' + e.portions + '</b>' +
            '<button class="iconbtn" data-a="loAdj" data-id="' + esc(e.id) + '" data-d="1" aria-label="One more portion">' + ic('add', 's') + '</button></div>' +
            '<button class="iconbtn" data-a="loDel" data-id="' + esc(e.id) + '" aria-label="Remove leftovers">' + ic('delete', 's') + '</button></div>';
        }).join('') + '<div class="faint" style="margin-top:6px">Use-by dates for leftovers are a rule of thumb. When in doubt, throw it out.</div></div>';
      },
      soon: function () {
        var list = pantry.filter(function (p) { var d = daysLeft(p); return d != null && d !== -2147483648 && d <= PR.UseBy.SOON_DAYS; })
          .sort(function (a, b) { return daysLeft(a) - daysLeft(b) || PR.cmpIC(a.label, b.label); });
        if (!list.length) return '';
        var uu = PR.UseBy.useItUp(matchAll(), soonMatcher().keys, 1000).length;
        return '<div class="card soon"><h2>' + ic('timer', 's') + ' Use soon</h2><div class="muted" style="margin-bottom:6px">Recipes that use these come first in Recipes.</div>' +
          list.map(function (p) {
            var d = daysLeft(p), st = PR.UseBy.status(d);
            return '<button class="srow" data-a="pmenu" data-key="' + esc(p.key) + '"><span class="grow">' + esc(p.label) + (p.qty ? ' <span class="faint">' + esc(p.qty) + '</span>' : '') + '</span><span class="ubadge ' + st + '">' + esc(PR.UseBy.label(d)) + '</span></button>';
          }).join('') +
          (uu ? '<button class="btn alt block" style="margin-top:10px" data-a="seeAll" data-k="useup">' + ic('restaurant', 's') + 'Recipes to use them up (' + uu + ')</button>' : '') + '</div>';
      },
      mine: function () {
        var items = pantry.filter(function (p) { return !p.staple; }), st = pantry.filter(function (p) { return p.staple; });
        var h = '<div class="card"><div class="row"><h2 class="grow" style="margin:0">In your pantry' + (items.length ? ' <span class="faint">(' + items.length + ')</span>' : '') + '</h2>' +
          (items.length ? '<button class="link" data-a="clearPantry">Clear pantry</button>' : '') + '</div>';
        if (!items.length) h += '<div class="muted">Your pantry is empty. Add a few things you have at home and we\'ll find recipes that use them.</div>';
        else h += '<div class="chips" style="margin-top:6px">' + items.map(function (p) {
          return '<button class="chip have" data-a="pmenu" data-key="' + esc(p.key) + '">' + esc(p.label) + pantryExtra(p) + '</button>';
        }).join('') + '</div>';
        h += '<div style="margin:16px 0 8px"><h3 style="margin:0;font-size:16px">Always on hand</h3><div class="faint">Staples like salt, pepper and oil are never counted as missing. Tap any pantry item to make it a staple.</div></div>';
        h += st.length ? '<div class="chips">' + st.map(function (p) {
          return '<button class="chip staple" data-a="pmenu" data-key="' + esc(p.key) + '">' + ic('star', 'xs') + esc(p.label) + pantryExtra(p) + '</button>';
        }).join('') + '</div>' : '<div class="muted">No staples yet.</div>';
        if (items.length) h += '<button class="btn block" style="margin-top:14px" data-a="tab" data-tab="recipes">' + ic('restaurant', 's') + 'See recipes</button>';
        return h + '</div>';
      },
      grid: function () {
        var have = new Map();
        pantry.forEach(function (p) { have.set(p.key, p); });
        var q = PR.jtrim(ui.quick).toLowerCase();
        if (q) {
          var out = [], keys = new Set(), exact = false, qk = PR.normalize(q);
          var push = function (label) {
            var k = PR.normalize(label);
            if (!k || keys.has(k) || out.length >= 80) return;
            keys.add(k); out.push(label);
            if (label.toLowerCase() === q || k === qk) exact = true;
          };
          if (PR.Botany.isQuery(q)) { PR.Botany.LABELS.forEach(push); exact = true; } // v2.1: the optional botanical-fruit filter
          else C.ITEMS.forEach(function (items, ci) {
            var catHit = C.CATEGORIES[ci].toLowerCase().indexOf(q) >= 0, subHit = false;
            items.forEach(function (s) {
              if (C.isHeader(s)) { subHit = s.substring(1).toLowerCase().indexOf(q) >= 0; return; }
              if (catHit || subHit || s.toLowerCase().indexOf(q) >= 0) push(s);
            });
          });
          C.ALIASES.forEach(function (s) { if (s.toLowerCase().indexOf(q) >= 0) push(s); });
          pantry.forEach(function (p) { if (p.label.toLowerCase().indexOf(q) >= 0) push(p.label); });
          var raw = PR.jtrim(ui.quick);
          var h = '<div class="chips" style="margin-top:8px">' + out.map(function (l) { return itemChip(have, l); }).join('');
          if (!exact) h += '<button class="chip add" data-a="addTyped" data-label="' + esc(raw) + '">+ Add \u201c' + esc(raw) + '\u201d</button>';
          h += '</div>';
          if (!out.length) h += '<div class="faint" style="margin-top:8px">Not in the list \u2013 add it anyway, it will be matched by name.</div>';
          return h;
        }
        var h2 = '';
        if (recents.length) h2 += '<div class="subh">Recently used</div><div class="chips" id="recentChips">' + recents.map(function (l) { return itemChip(have, l); }).join('') + '</div><div style="height:10px"></div>';
        var mv = catMoves();
        h2 += C.CATEGORIES.map(function (name, ci) {
          var items = mv.size ? PR.Botany.sectionItems(ci, mv) : C.ITEMS[ci], total = 0, n = 0;
          items.forEach(function (s) { if (!C.isHeader(s)) { total++; if (have.has(PR.normalize(s))) n++; } });
          var open = openCats.has(String(ci)), body = '';
          if (open) {
            var groups = [], cur = null;
            items.forEach(function (s) {
              if (C.isHeader(s)) { cur = { h: s.substring(1), items: [] }; groups.push(cur); }
              else { if (!cur) { cur = { h: '', items: [] }; groups.push(cur); } cur.items.push(s); }
            });
            body = '<div class="body">' + (ci <= 1 ? '<div class="chips"><button class="chip" data-a="botFilter" aria-label="Show ingredients that are botanically fruits">\ud83c\udf45 Botanical fruits</button></div>' : '') + groups.map(function (g) {
              return (g.h ? '<div class="subh">' + esc(g.h) + '</div>' : '') + '<div class="chips">' + g.items.map(function (l) { return itemChip(have, l); }).join('') + '</div>';
            }).join('') + '</div>';
          }
          return '<div class="cat' + (open ? ' open' : '') + '"><button data-a="cat" data-ci="' + ci + '" aria-expanded="' + open + '"><span class="dot" style="background:' + C.COLORS[ci] + '"></span>' +
            '<span class="nm">' + esc(name) + '</span><span class="cnt' + (n ? ' some' : '') + '">' + (n > 0 ? n + ' of ' + total : total) + '</span>' + ic('expand', 'chev') + '</button>' + body + '</div>';
        }).join('');
        return h2;
      }
    }
  };
  function acSuggestions(text) {
    var parts = text.split(','), last = PR.jtrim(parts[parts.length - 1]).toLowerCase();
    if (!last) return [];
    var names = ingredientNames(), a = [], b = [], c = [];
    for (var i = 0; i < names.length; i++) {
      var l = names[i].toLowerCase(), idx = l.indexOf(last);
      if (idx < 0) continue;
      if (idx === 0) a.push(names[i]); else if (l.charAt(idx - 1) === ' ') b.push(names[i]); else c.push(names[i]);
      if (a.length >= 8) break;
    }
    return a.concat(b, c).slice(0, 8);
  }
  function renderAc() {
    var box = $('[data-region="ac"]');
    if (!box) return;
    var list = acSuggestions(ui.addText);
    if (!list.length) { box.hidden = true; box.innerHTML = ''; return; }
    var last = PR.jtrim(ui.addText.split(',').pop()).toLowerCase();
    box.innerHTML = list.map(function (n) {
      var inP = !!pantryItem(PR.normalize(n)), idx = n.toLowerCase().indexOf(last);
      var label = esc(n.substring(0, idx)) + '<b>' + esc(n.substring(idx, idx + last.length)) + '</b>' + esc(n.substring(idx + last.length));
      return '<button data-a="acPick" data-label="' + esc(n) + '">' + label + (inP ? ' <span class="faint">\u00b7 in pantry</span>' : '') + '</button>';
    }).join('');
    box.hidden = false;
  }
  function addPantryText(text) {
    var parts = text.split(',').map(PR.jtrim).filter(Boolean), added = [], already = [];
    parts.forEach(function (p) { if (addToPantry(p, false)) added.push(cap(p)); else if (PR.normalize(p)) already.push(cap(p)); });
    if (already.length && !added.length) toast(already.join(', ') + (already.length === 1 ? ' is' : ' are') + ' already in your pantry');
    else if (added.length) toast('Added ' + added.join(', '));
    return added.length + already.length > 0;
  }

  // =====================================================================
  // Recipes tab
  // =====================================================================
  var CORE_DIETS = ['vegetarian', 'vegan', 'gluten-free', 'dairy-free'], DIETS = CORE_DIETS.concat(PR.Diets.IDS);
  var MODES = ['Best match', 'Cook now', 'Missing 1\u20132'];
  var TIMES = [0, 15, 30, 60];
  function passesFilters(r, res, ignoreMeal) {
    if (!ignoreMeal && !mealPasses(r)) return false;
    if (!dietsPass(r) || !budgetPass(r)) return false;
    if (ui.maxTime > 0) { var t = PR.totalMinutes(r); if (t >= 0 && t > ui.maxTime) return false; }
    if (!PR.Methods.matches(r, methodFilter())) return false;
    if (ui.mode === 1 && res.missing.length > 0) return false;
    if (ui.mode === 2 && (res.missing.length < 1 || res.missing.length > 2)) return false;
    var query = PR.jtrim(ui.query);
    if (query) {
      var q = query.toLowerCase();
      var hit = r.title.toLowerCase().indexOf(q) >= 0 || r.cuisine.toLowerCase().indexOf(q) >= 0 || r.category.toLowerCase().indexOf(q) >= 0;
      if (!hit) {
        var qk = PR.normalize(query);
        for (var n = 0; n < r.ingredients.length; n++) {
          var i = r.ingredients[n];
          if (i.name.toLowerCase().indexOf(q) >= 0 || (qk !== '' && i.key === qk)) { hit = true; break; }
        }
      }
      if (!hit) return false;
    }
    return true;
  }
  function filteredResults() {
    // ranked as before, but within "cook now" and "missing 1-2" recipes that use up soon-to-expire items come first
    return PR.UseBy.sort(matchAll().filter(function (res) { return passesFilters(res.recipe, res); }), soonMatcher().keys);
  }
  function surprise(items, avoid) {
    var meals = suggestable(items);
    if (meals.length) items = meals;
    var pool = items.filter(function (r) { return r.missing.length === 0 && r.haveNonStaple > 0; });
    if (!pool.length) pool = items.filter(function (r) { return r.missing.length <= 2 && r.haveNonStaple > 0; });
    if (!pool.length) pool = items.filter(function (r) { return r.haveNonStaple > 0; });
    if (!pool.length) { toast('Add some ingredients to your pantry first'); return; }
    if (pool.length > 1 && avoid) pool = pool.filter(function (r) { return r.recipe.id !== avoid; });
    openDetail(pool[Math.floor(Math.random() * pool.length)].recipe.id);
  }
  var lastOpened = null;
  var Recipes = {
    full: function () {
      return '<div class="field" style="margin-bottom:8px">' + ic('search', 's') + '<input type="search" id="rq" data-in="rq" data-enter="blur" value="' + esc(ui.query) +
        '" placeholder="Search recipes or ingredients" autocomplete="off" autocorrect="off" autocapitalize="none" spellcheck="false" enterkeyhint="search" aria-label="Search recipes"></div>' +
        '<div class="hscroll">' + region('filters', Recipes) + '</div>' + region('meals', Recipes) + region('list', Recipes);
    },
    regions: {
      meals: mealBar,
      filters: function () {
        return '<div class="chips nowrap">' + '<button class="chip" data-a="tab" data-tab="favorites">\u2665 Favorites</button><span class="sep"></span>' + MODES.map(function (m, i) { return chip(m, ui.mode === i, 'mode', ' data-v="' + i + '"'); }).join('') + '<span class="sep"></span>' +
          DIETS.map(function (d) { return chip(dietLabel(d), ui.diets.indexOf(d) >= 0, 'diet', ' data-v="' + d + '"'); }).join('') + fastChipIds().map(function (d) { var f = s3().fasts.filter(function (x) { return 'fast:' + x.id === d; })[0]; return chip(esc(f.label), ui.diets.indexOf(d) >= 0, 'diet', ' data-v="' + esc(d) + '" aria-label="Fasting label filter: ' + esc(f.label) + '"'); }).join('') + '<span class="sep"></span>' +
          TIMES.map(function (t) { return chip(t === 0 ? 'Any time' : '\u2264 ' + t + ' min', ui.maxTime === t, 'time', ' data-v="' + t + '"'); }).join('') + '<span class="sep"></span>' +
          methodChips() + '</div>';
      },
      list: function () {
        if (!loaded) return '<div class="loading"><span class="spin"></span> Loading recipes\u2026</div>';
        var results = filteredResults(), m = matcher(), pantryEmpty = m.pantryEmpty();
        var best = null;
        var cands = suggestable(results.filter(function (x) { return x.haveNonStaple > 0; }));
        best = cands.length ? cands[0] : null;
        var hidden = matchAll().filter(function (x) { return mealHidden(x.recipe) && passesFilters(x.recipe, x, true); }).length;
        var h = '<div class="hero"><div class="over">' + esc(PR.heroQuestion(ui.meals)) + '</div>';
        if (pantryEmpty) h += '<h2>Add a few ingredients to get ideas</h2><p>Tell us what\'s in your kitchen and every recipe gets ranked by what you already have.</p><div class="acts"><button class="btn white" data-a="tab" data-tab="pantry">' + ic('kitchen', 's') + 'Go to pantry</button></div>';
        else if (!best) h += '<h2>Nothing matches yet</h2><p>Try other filters or add more ingredients.</p>';
        else h += '<h2>' + esc(best.recipe.title) + '</h2><div class="hmeals">' + esc(PR.heroOrder(mealsOf(best.recipe)).slice(0, 3).join(' \u00b7 ')) + '</div><p>' + (best.missing.length === 0 ? 'You have everything \u00b7 ' : 'You have ' + best.have + ' of ' + best.total + ' \u00b7 ') + timeText(PR.totalMinutes(best.recipe)) + '</p>' +
          '<div class="acts"><button class="btn white" data-a="open" data-id="' + esc(best.recipe.id) + '">' + ic('restaurant', 's') + 'Open recipe</button><button class="btn outline-white" data-a="surprise">' + ic('shuffle', 's') + 'Surprise me</button></div>';
        h += '</div>';
        var onlineN = results.filter(function (r) { return PR.isOnline(r.recipe); }).length;
        var ct = plural(results.length, 'recipe', 'recipes') + (onlineN ? ' \u00b7 ' + onlineN + ' from TheMealDB' : '') + (hidden ? ' \u00b7 ' + hidden + ' desserts/sides hidden' : '') + (hiddenByAvoid() ? ' \u00b7 ' + hiddenByAvoid() + ' hidden by your dislikes/allergies' : '');
        h += '<div class="status"><span class="grow muted">' + ct + '</span>';
        if (net.fetching) h += '<span class="spin s"></span><span class="muted">Finding online recipes\u2026</span>';
        else if (!onlineEnabled()) h += '<span class="muted">Online recipes off</span>';
        else if (!isOnline()) h += '<span class="faint">' + ic('cloud_off', 'xs') + ' Offline \u2013 built-in &amp; saved recipes</span>';
        else if (!pantryEmpty) h += '<button class="link" data-a="findMore">' + ic('cloud', 'xs') + ' Find more online</button>';
        h += '</div>';
        if (!net.fetching && net.lastAdded === 0 && !net.lastError) h += '<div class="faint note">No new TheMealDB recipes for your latest ingredients.</div>';
        if (!net.fetching && net.lastError && net.lastError !== 'offline' && isOnline()) h += '<div class="note warnc">' + esc(net.lastError) + '. Showing built-in and saved recipes.</div>';
        if (!results.length) {
          h += emptyBox('search', 'No recipes match', ui.mode === 1 ? 'Nothing is fully cookable with these filters. Try \u201cMissing 1\u20132\u201d or add more to your pantry.' : 'Try removing a filter or searching for something else.');
        } else {
          h += '<div class="list two">' + results.slice(0, ui.listLimit).map(recipeRow).join('') + '</div>';
          if (results.length > ui.listLimit) h += '<button class="btn ghost block" data-a="more">Show more (' + (results.length - ui.listLimit) + ' left)</button>';
        }
        var q = PR.jtrim(ui.query);
        if (q && onlineEnabled()) h += '<button class="rrow searchrow" data-a="searchOnline" data-q="' + esc(q) + '">' + ic('cloud') + '<div class="body"><div class="title">Search TheMealDB for \u201c' + esc(q) + '\u201d</div><div class="meta">Online recipes are saved for offline use</div></div></button>';
        return h;
      }
    }
  };
  function doOnlineSearch(q) {
    if (!onlineEnabled()) { toast('Online recipes are turned off (see About)'); return; }
    toast('Searching TheMealDB\u2026');
    searchOnline(q).then(function (r) {
      if (r.error) toast(r.error);
      else if (!r.results.length) toast('No TheMealDB recipes for \u201c' + q + '\u201d');
      else {
        var m = matcher();
        pushScreen({ type: 'list', title: 'TheMealDB: ' + q, sub: plural(r.results.length, 'recipe', 'recipes'), items: function () { return r.results.map(function (x) { return byId(x.id) || x; }).map(function (x) { return m === matcher() ? resultFor(x) : matcher().match(x); }); } });
      }
      refresh();
    });
  }

  // =====================================================================
  // Discover tab
  // =====================================================================
  function likeFavorites(limit) {
    var favs = favoriteIdsNewestFirst().map(byId).filter(Boolean), m = matcher();
    return PR.basedOnFavorites(allRecipes(), favs, limit * 3).filter(dMealPasses).slice(0, limit).map(function (r) { return m.match(r); });
  }
  function carousel(title, sub, items, all, empty) {
    var h = '<div class="dsec"><div class="dhead"><div class="grow"><h3>' + esc(title) + '</h3><div class="muted">' + esc(sub) + '</div></div>' +
      (items.length >= 4 ? '<button class="link" data-a="seeAll" data-k="' + all + '">See all</button>' : '') + '</div>';
    if (!items.length) h += '<div class="faint pad">' + esc(empty) + '</div>';
    else h += '<div class="car">' + items.map(recipeCard).join('') + '</div>';
    return h + '</div>';
  }
  var SEE_ALL = {
    'new': { t: 'New for you', f: function () { return PR.newForYou(matchFiltered(), seen, 1000); } },
    quick: { t: 'Quick (under 30 min)', f: function () { return PR.quick(matchFiltered(), 1000); } },
    almost: { t: 'Almost there', f: function () { return PR.almostThere(matchFiltered(), 1000); } },
    favs: { t: 'Based on your favorites', f: function () { return likeFavorites(60); } },
    useup: { t: 'Use it up', f: function () { return PR.UseBy.useItUp(matchAll(), soonMatcher().keys, 1000); } }
  };
  /** Results for "Buy these next" (Discover): Discover's meal-type filter only (v2.0.1: no Recipes diet filter); avoided recipes are already out of allRecipes(), substitutes are in the matcher. */
  function buyNextResults() { return matchAll().filter(function (x) { return dMealPasses(x.recipe); }); }
  /** Discover lists: Discover's own meal-type filter only (v2.0.1: no Recipes method/diet/time filters). */
  function matchFiltered() { return matchAll().filter(function (x) { return dMealPasses(x.recipe); }); }
  /** v1.7 cooking-method filter chips (search filters); several chips must all match. */
  function lc1(t) { return t ? t.charAt(0).toLowerCase() + t.substring(1) : t; }
  function methodChips() {
    var mf = methodFilter();
    return PR.Methods.IDS.map(function (id) { return chip(PR.Methods.label(id), mf.indexOf(id) >= 0, 'methodF', ' data-v="' + id + '"'); }).join('');
  }
  // v1.8: the four "Browse by" cards only change when the recipe set, the method filter or TheMealDB's name lists change
  var browseMemo = { k: null, h: '' }, userVer = 0;
  function browseCards(all) {
    var ra = mealDbNames(true), rc = mealDbNames(false);
    var k = [all.length, all.length ? all[0].id + all[all.length - 1].id : '', userRecipes.size, userVer, online.size, dislikes.join(), allergens.join(), methodFilter().join(), onlineEnabled(), ra.length, rc.length].join('|');
    if (browseMemo.k !== k) browseMemo = { k: k, h: mealFacetCard(all) + methodsCard(all) + facetCard('Browse by cuisine', 'a', PR.facet(all, true), ra) + facetCard('Browse by category', 'c', PR.facet(all, false), rc) };
    return browseMemo.h;
  }
  function methodsCard(all) {
    var cnt = {};
    all.forEach(function (r) { PR.Methods.of(r).forEach(function (m) { cnt[m] = (cnt[m] || 0) + 1; }); });
    return '<div class="card" id="methodsCard"><h2>Browse by cooking method</h2><div class="chips">' + PR.Methods.IDS.map(function (id) {
      return '<button class="chip" data-a="browse" data-kind="k" data-v="' + id + '">' + esc(PR.Methods.label(id)) + (cnt[id] ? ' <span class="faint">' + cnt[id] + '</span>' : '') + '</button>';
    }).join('') + '</div>' +
      '<button class="link" data-a="ipSheet" style="margin-top:6px">' + ic('timer', 'xs') + ' Instant Pot cheat sheet</button></div>';
  }
  function mealFacetCard(all) {
    var cnt = {};
    all.forEach(function (r) { mealsOf(r).forEach(function (m) { cnt[m] = (cnt[m] || 0) + 1; }); });
    return '<div class="card"><h2>Browse by meal type</h2><div class="chips">' + PR.MEAL_TYPES.map(function (m) {
      return '<button class="chip" data-a="browse" data-kind="m" data-v="' + esc(m) + '">' + esc(m) + (cnt[m] ? ' <span class="faint">' + cnt[m] + '</span>' : '') + '</button>';
    }).join('') + '</div></div>';
  }
  function facetCard(title, kind, local, remote) {
    var names = local.slice(), have = {};
    names.forEach(function (n) { have[n[0].toLowerCase()] = true; });
    if (onlineEnabled()) remote.forEach(function (n) { if (!have[n.toLowerCase()]) { have[n.toLowerCase()] = true; names.push([n, 0]); } });
    names.sort(function (a, b) { return PR.cmpIC(a[0], b[0]); });
    var one = function (n) { return '<button class="chip" data-a="browse" data-kind="' + kind + '" data-v="' + esc(n[0]) + '">' + esc(n[0]) + (n[1] ? ' <span class="faint">' + n[1] + '</span>' : ' ' + ic('cloud', 'xs')) + '</button>'; };
    if (kind === 'a') {
      // v2.0: optional grouping by world region (Caribbean, Latin America, ...); cuisines stay tagged by country
      var grp = pref('cuisineGroups', true) !== false, tog = '<button class="link" data-a="cuGroups">' + (grp ? 'Show A\u2013Z' : 'Group by region') + '</button>';
      if (!grp) return '<div class="card"><div class="row"><h2 class="grow" style="margin:0">' + esc(title) + '</h2>' + tog + '</div><div class="chips">' + names.map(one).join('') + '</div></div>';
      var by = {}; names.forEach(function (n) { var g = PR.Cuisines.group(n[0]); (by[g] = by[g] || []).push(n); });
      var order = PR.Cuisines.GROUPS.concat(['International']).filter(function (g, i, a) { return by[g] && a.indexOf(g) === i; });
      return '<div class="card"><div class="row"><h2 class="grow" style="margin:0">' + esc(title) + '</h2>' + tog + '</div>' + order.map(function (g) { return '<div class="subh">' + esc(g) + '</div><div class="chips">' + by[g].map(one).join('') + '</div>'; }).join('') + '</div>';
    }
    return '<div class="card"><h2>' + esc(title) + '</h2><div class="chips">' + names.map(one).join('') + '</div></div>';
  }
  var Discover = {
    full: function () {
      return '<div class="field" style="margin-bottom:8px">' + ic('search', 's') + '<input type="search" id="dq" data-in="dq" data-enter="dsearch" value="' + esc(ui.dq) +
        '" placeholder="Search all recipes by name" autocomplete="off" autocorrect="off" autocapitalize="none" spellcheck="false" enterkeyhint="search" aria-label="Search recipes by name"></div>' +
        region('meals', Discover) + region('results', Discover) + region('body', Discover);
    },
    regions: {
      meals: function () { return mealBar(true); },
      results: function () {
        var q = PR.jtrim(ui.dq);
        if (!q) return '';
        var m = matcher(), hits = PR.searchTitles(allRecipes(), q, 500), found = hits.filter(dMealPasses).slice(0, 30).map(function (r) { return m.match(r); });
        var hid = hits.filter(dMealHidden).length;
        var h = '<div class="card"><h2>Results for \u201c' + esc(q) + '\u201d</h2>' + (hid ? '<div class="faint note">' + plural(hid, 'dessert/side', 'desserts/sides') + ' hidden \u2013 tick Include desserts / Include sides to show</div>' : '');
        h += found.length ? '<div class="list two">' + found.map(recipeRow).join('') + '</div>' : '<div class="muted">No saved recipe titles match.</div>';
        if (onlineEnabled()) h += '<button class="btn ghost block" style="margin-top:10px" data-a="searchOnline" data-q="' + esc(q) + '">' + ic('cloud', 's') + 'Search TheMealDB</button>';
        return h + '</div>';
      },
      body: function () {
        if (!loaded) return '<div class="loading"><span class="spin"></span> Loading recipes\u2026</div>';
        var results = matchFiltered(), all = allRecipes(), pantryEmpty = matcher().pantryEmpty();
        var h = '<div class="row wrapbtns"><button class="btn" data-a="random">' + ic('shuffle', 's') + 'Random recipe</button><button class="btn ghost" data-a="surpriseP">' + ic('kitchen', 's') + 'Surprise me from my pantry</button></div>';
        if (!onlineEnabled() || !isOnline()) h += '<div class="faint note">' + ic('cloud_off', 'xs') + ' ' + (onlineEnabled() ? 'Offline \u2013 showing built-in and saved recipes' : 'Online recipes are off (see About)') + '</div>';
        else if (net.fetching) h += '<div class="faint note"><span class="spin s"></span> Finding online recipes for your pantry\u2026</div>';
        if (soonMatcher().keys.length) h += carousel('Use it up', 'Recipes with pantry items that need using soon', PR.UseBy.useItUp(results, soonMatcher().keys, 12), 'useup', 'No recipes use your soon-to-expire items yet.');
        h += buyNextCard(buyNextResults(), pantryEmpty);
        h += carousel('New for you', "Recipes you haven't opened yet, best match first", PR.newForYou(results, seen, 12), 'new', "You've opened every recipe! Browse a cuisine below to load more from TheMealDB.");
        h += carousel('Quick (under 30 min)', 'Ready in half an hour or less', PR.quick(results, 12), 'quick', 'No quick recipes yet.');
        h += carousel('Almost there', 'Missing just 1\u20132 ingredients', PR.almostThere(results, 12), 'almost', pantryEmpty ? 'Add ingredients to your pantry to see recipes you\'re one or two items away from.' : 'Nothing is one or two ingredients away. Add a few more pantry items.');
        h += carousel('Based on your favorites', 'Similar ingredients, cuisine or course', likeFavorites(12), 'favs', 'Tap the heart on recipes you like and similar ones show up here.');
        h += browseCards(all);
        var seenN = all.filter(function (r) { return seen.has(r.id); }).length;
        h += '<div class="row foot"><span class="grow muted">You\'ve opened ' + seenN + ' of ' + all.length + ' recipes</span>' + (seenN > 0 ? '<button class="link" data-a="resetSeen">Reset</button>' : '') + '</div>';
        return h;
      }
    }
  };

  /** "Buy these next": the 1-3 missing ingredients that unlock the most recipes that are 1-2 items away (after diet, meal and avoid filters). */
  function buyNextCard(results, pantryEmpty) {
    if (pantryEmpty) return '';
    var picks = PR.BuyNext.fromResults(results, PR.BuyNext.MAX);
    if (!picks.length) return '';
    var rk = null; // v2.1: ranked only when a row is open (HD8: nothing extra on a plain render)
    return '<div class="card buynext"><h2>' + ic('cart', 's') + ' Buy these next</h2><div class="muted" style="margin-bottom:8px">Each one unlocks recipes you’re only one or two items away from. Tap one for details.</div>' +
      picks.map(function (p, k) {
        var open = bnOpen.has(p.key), body = '';
        if (open) {
          if (!rk) rk = PR.BuyNext.rankedFromResults(results);
          var o = rk.filter(function (x) { return x.key === p.key; })[0];
          body = bnDetails(p.key, p.name, PR.BuyNext.whyPick(p, k, o), p.unlocks, k ? 'Recipes it unlocks with the ones above' : 'Recipes it unlocks', 'pick:' + k);
        }
        return '<div class="bnitem"><button class="srow" data-a="bnToggle" data-key="' + esc(p.key) + '" aria-expanded="' + open + '" aria-label="' + esc(p.name + ', ' + plural(p.unlocks.length, 'recipe', 'recipes') + (open ? '. Tap to collapse.' : '. Tap for details.')) + '"><span class="grow"><b>' + esc(p.name) + '</b>' + (k ? ' <span class="faint">(with the one' + (k > 1 ? 's' : '') + ' above)</span>' : '') + '</span><span class="pbadge">' + plural(p.unlocks.length, 'recipe', 'recipes') + '</span>' + ic('expand', 's') + '</button>' + body + '</div>';
      }).join('') + '<button class="btn ghost" data-a="bnAll" aria-label="See all suggestions, with sort options">' + ic('list', 's') + 'See all suggestions</button></div>';
  }
  // ---------- v2.1: expandable "Buy these next" details and the full list ----------
  var bnOpen = new Set(), BN_SHOW = 5;
  function bnDetails(key, name, why, ids, idsTitle, src) {
    var p = PR.Cost.find(key, prices, priceDefaults()), mine = prices.has(key) || prices.has(PR.family(key));
    var ct = p ? PR.BuyNext.costText(p, money(p.price), mine) : null;
    var h = '<div class="bndetail"><p>' + esc(why) + '</p><div class="faint" style="margin:4px 0 6px">' + esc(ct || 'No price yet. Set one in Settings \u203a Prices to see an estimate.') + '</div>';
    if (ids.length) {
      h += '<div class="muted"><b>' + esc(idsTitle) + '</b></div>' + ids.slice(0, BN_SHOW).map(function (id) { var r = byId(id); return r ? '<button class="srow" data-a="open" data-id="' + esc(id) + '" aria-label="Open recipe ' + esc(r.title) + '">' + ic('restaurant', 's') + '<span class="grow">' + esc(r.title) + '</span>' + ic('next', 's') + '</button>' : ''; }).join('');
      if (ids.length > BN_SHOW) h += '<button class="btn ghost small" data-a="bnRecipes" data-key="' + esc(key) + '" data-src="' + esc(src) + '">See all ' + ids.length + ' recipes</button>';
    }
    return h + '<button class="btn small" data-a="buyAdd" data-v="' + esc(name) + '">' + ic('add', 's') + 'Add ' + esc(name) + ' to shopping list</button></div>';
  }

  // =====================================================================
  // Favorites tab
  // =====================================================================
  var Favorites = {
    full: function () {
      return '<div class="chips nowrap"><button class="chip" data-a="tab" data-tab="recipes" aria-pressed="false">All recipes</button>' +
        '<button class="chip on" data-a="tab" data-tab="favorites" aria-pressed="true">\u2665 Favorites</button></div>' + region('list', Favorites);
    },
    regions: {
      list: function () {
        if (!loaded) return '<div class="loading"><span class="spin"></span> Loading recipes\u2026</div>';
        var m = matcher(), out = favoriteIdsNewestFirst().map(byId).filter(Boolean).map(function (r) { return m.match(r); });
        if (!out.length) return emptyBox('heart_outline', 'No favorites yet', 'Tap the heart on any recipe to keep it here for quick access.');
        return '<div class="status"><span class="muted">' + out.length + (out.length === 1 ? ' saved recipe' : ' saved recipes') + ' \u00b7 newest first</span></div><div class="list two">' + out.map(recipeRow).join('') + '</div>';
      }
    }
  };

  // =====================================================================
  // Shopping tab
  // =====================================================================
  var UNIT_WORDS = ['cup', 'cups', 'tbsp', 'tsp', 'tablespoon', 'tablespoons', 'teaspoon', 'teaspoons', 'g', 'kg', 'mg', 'ml', 'l', 'oz', 'lb', 'lbs', 'pound', 'pounds', 'ounce', 'ounces', 'pinch', 'clove', 'cloves', 'can', 'cans', 'tin', 'tins', 'bunch', 'slice', 'slices', 'pack', 'packet', 'jar', 'bottle', 'piece', 'pieces', 'handful', 'dash', 'stick', 'sticks', 'head', 'heads'];
  function singularUnit(u) {
    var l = u.toLowerCase();
    var map = { cups: 'cup', tablespoon: 'tbsp', tablespoons: 'tbsp', teaspoon: 'tsp', teaspoons: 'tsp', lbs: 'lb', pound: 'lb', pounds: 'lb', ounce: 'oz', ounces: 'oz', cloves: 'clove', cans: 'can', tins: 'tin', slices: 'slice', pieces: 'piece', sticks: 'stick', heads: 'head' };
    return map[l] || l;
  }
  function parseShoppingInput(text) {
    var t = PR.jtrim(text), m = /^([0-9][0-9.\/ ]*|[\u00bc-\u00be\u2150-\u215e])\s*(.*)$/.exec(t);
    if (!m) return { name: t, amount: null, unit: '' };
    var amt = PR.parseNumber(PR.jtrim(m[1]));
    if (isNaN(amt)) return { name: t, amount: null, unit: '' };
    var rest = PR.jtrim(m[2]), unit = '', sp = rest.split(' ');
    if (sp.length > 1 && UNIT_WORDS.indexOf(sp[0].toLowerCase().replace(/\.$/, '')) >= 0) { unit = singularUnit(sp[0].replace(/\.$/, '')); rest = PR.jtrim(sp.slice(1).join(' ').replace(/^of /i, '')); }
    if (!rest) return { name: t, amount: null, unit: '' };
    return { name: rest, amount: amt, unit: unit };
  }
  function shopRow(s) {
    var q = s.amount != null ? qtyText(s.key || PR.normalize(s.name), s.amount, s.unit, 1, false) : '', cost = null;
    if (pref('shopCost', false)) { cost = PR.Cost.item(s.name, s.amount == null ? NaN : s.amount, s.unit, prices, priceDefaults(), FOODS); }
    return '<div class="shop' + (s.checked ? ' done' : '') + '"><button class="cbx" data-a="shopCheck" data-id="' + esc(s.id) + '" aria-pressed="' + s.checked + '" aria-label="' + (s.checked ? 'Uncheck ' : 'Check ') + esc(s.name) + '">' + (s.checked ? ic('check', 's') : '') + '</button>' +
      '<div class="grow"><div class="nm">' + (q ? '<b>' + esc(q) + '</b> ' : '') + esc(s.name) + '</div>' + (s.recipe ? '<div class="faint">For ' + esc(s.recipe) + '</div>' : '') + '</div>' +
      (cost != null && !isNaN(cost) ? '<span class="faint price">~' + esc(money(cost)) + '</span>' : '') +
      '<button class="iconbtn" data-a="shopDel" data-id="' + esc(s.id) + '" aria-label="Remove ' + esc(s.name) + '">' + ic('delete', 's') + '</button></div>';
  }
  function shopRowsData(list) { return list.map(function (x) { return [x.name, x.amount == null ? NaN : x.amount, x.unit || '', !!x.checked]; }); }
  var Shopping = {
    full: function () {
      return '<div class="card"><div class="row"><div class="field grow">' + ic('add', 's') + '<input type="text" id="shopIn" data-enter="shopAdd" placeholder="Add an item, e.g. 2 lemons" autocomplete="off" autocapitalize="none" enterkeyhint="done" aria-label="Add shopping item"></div>' +
        '<button class="btn" data-a="shopAdd">Add</button></div></div>' + region('list', Shopping);
    },
    regions: {
      list: function () {
        var open = shopping.filter(function (s) { return !s.checked; }).sort(function (a, b) { return a.added - b.added; });
        var done = shopping.filter(function (s) { return s.checked; });
        if (!shopping.length) return emptyBox('cart', 'Your list is empty', 'Open a recipe and tap \u201cAdd missing to list\u201d, or type an item above.');
        var coll = pref('shopCollapsed', []) || [];
        var h = '<div class="row wrapbtns shopbar"><span class="grow muted">' + open.length + ' to buy \u00b7 grouped by aisle</span><button class="btn alt small" data-a="shopShare">' + ic('share', 's') + 'Share</button></div>';
        if (pref('shopCost', false) && open.length) {
          var t = PR.Cost.shopping(shopRowsData(open), prices, priceDefaults(), FOODS);
          h += '<div class="card est"><div class="row"><b class="grow">' + (t[1] ? 'About ' + esc(money(t[0])) : 'No prices yet') + '</b><span class="ebadge">Estimate \u00b7 ' + t[1] + ' of ' + t[2] + ' priced</span></div><div class="faint">' + esc(PR.Cost.NOTE) + '</div><button class="link" data-a="prices" style="margin-top:6px">' + ic('list', 'xs') + ' Edit prices</button></div>';
        }
        if (!open.length) h += '<div class="card"><div class="muted">All done!</div></div>';
        PR.Aisles.group(open.map(function (x) { return x.name; })).forEach(function (idx, aisle) {
          var shut = coll.indexOf(aisle) >= 0;
          h += '<div class="card aisle"><button class="ahead" data-a="aisleToggle" data-v="' + esc(aisle) + '" aria-expanded="' + !shut + '"><h2 class="grow" style="margin:0">' + esc(aisle) + ' <span class="faint">(' + idx.length + ')</span></h2><span class="chev' + (shut ? '' : ' open') + '">' + ic('expand', 's') + '</span></button>' +
            (shut ? '' : '<div class="abody">' + idx.map(function (i) { return shopRow(open[i]); }).join('') + '</div>') + '</div>';
        });
        if (done.length) h += '<div class="card"><h2>In the basket <span class="faint">(' + done.length + ')</span></h2>' + done.map(shopRow).join('') +
          '<div class="row wrapbtns" style="margin-top:10px"><button class="btn" data-a="shopToPantry">' + ic('kitchen', 's') + 'Checked \u2192 pantry</button><button class="btn ghost" data-a="shopClearChecked">Clear checked</button></div></div>';
        h += '<button class="btn ghost block red" data-a="shopClearAll">' + ic('delete', 's') + 'Clear all</button>';
        return h;
      }
    }
  };
  /** Shares text with the system share sheet; otherwise copies it and shows it in a box to copy by hand. */
  function shareText(title, text) {
    function fallback(copied) {
      dialog({ title: title, html: '<p class="muted">' + (copied ? 'Copied to the clipboard. ' : '') + 'You can also select the text below and copy it.</p><textarea class="tin sharebox" readonly rows="10" aria-label="Text to share">' + esc(text) + '</textarea>', buttons: [{ label: 'Close', primary: true }] });
      setTimeout(function () { var t = $('#dlg .sharebox'); if (t) { t.focus(); t.select(); } }, 30);
    }
    if (navigator.share) {
      navigator.share({ title: title, text: text }).catch(function (e) { if (!e || e.name !== 'AbortError') fallback(false); });
      return;
    }
    if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(text).then(function () { fallback(true); }, function () { fallback(false); });
    else fallback(false);
  }
  // v1.8: the meal plan is a tab; it reuses the plan screen's renderer with a tab-owned state
  var PlanTab = {
    full: function () {
      var s = ui.planS, ws = s.week, we = D.addDays(ws, 6);
      return '<h2 class="secth">Meal plan <span class="faint">' + esc(D.label(ws)) + ' \u2013 ' + esc(D.label(we)) + '</span></h2>' +
        '<div class="row weeknav"><button class="btn alt small" data-a="planWeek" data-d="-7">' + ic('prev', 's') + 'Previous</button><span class="grow center">' +
        (ws === wkStart(D.today()) ? '<b>This week</b>' : '<button class="link" data-a="planWeek" data-d="0">Back to this week</button>') + '</span><button class="btn alt small" data-a="planWeek" data-d="7">Next' + ic('next', 's') + '</button></div>' +
        region('body', PlanTab);
    },
    regions: { body: function () { return SCREENS.plan.regions.body(ui.planS); } }
  };
  var VIEWS = { pantry: Pantry, recipes: Recipes, discover: Discover, favorites: Favorites, shopping: Shopping, plan: PlanTab };
  /** The plan being edited: the plan screen if one is open, else the Plan tab. */
  function planState() { var s = topScreen(); return s && s.type === 'plan' ? s : ui.planS; }
  function renderPlan(s, partial) { if (s.tabbed) { if (partial) refreshRegion('body'); else showTab('plan'); } else renderScreen(s, partial); }

  // =====================================================================
  // Screens
  // =====================================================================
  function openDetail(id) {
    var r = byId(id);
    if (!r) { toast('Recipe not available'); return; }
    markSeen(id); lastOpened = id;
    var top = ui.stack[ui.stack.length - 1];
    if (top && top.type === 'detail') { top.id = id; top.servings = defaultServings(); top.resetScroll = true; renderScreen(top, false); return; }
    pushScreen({ type: 'detail', id: id, servings: defaultServings() });
  }
  function metaChip(text, icon) { return '<span class="mchip">' + (icon ? ic(icon, 'xs') : '') + esc(text) + '</span>'; }
  function ingredientRow(i, scale, res, m, amt) {
    var missing = res.missing.indexOf(i) >= 0, subbed = res.substituted.indexOf(i) >= 0;
    var have = !missing && !subbed && m.has(i.key), staple = have && m.isStaple(i.key);
    var mark = missing ? ic('cart', 's') : subbed ? ic('swap', 's') : !have ? ic('add', 's') : ic('check', 's');
    var q = qtyText(i.key, i.amount, i.unit, scale, true);
    var tag = missing ? 'missing' : subbed ? 'swapped' : i.optional ? 'optional' : staple ? 'staple' : '';
    var warn = PR.Allergens.ingredientWarn(i, dislikes, allergens);
    var h = '<div class="ing' + (missing ? ' missing' : subbed ? ' subbed' : have ? ' have' : '') + '"><div class="row"><span class="mark">' + mark + '</span><span class="q">' + esc(q) + '</span><span class="nm grow">' + esc(loc(i.name, false)) + '</span>' +
      (tag ? '<span class="tag">' + tag + '</span>' : '') + '</div>';
    if (warn) h += '<div class="iwarn">' + ic('warning', 'xs') + 'On your dislikes/allergies list (matched by name)</div>';
    var ac = amt && amt.get(i);
    if (ac) h += '<div class="amt ' + ac.status + '">' + (ac.status === 'enough' ? ic('check', 'xs') : ac.status === 'short' ? ic('warning', 'xs') : ic('kitchen', 'xs')) + esc(PR.Amounts.label(ac)) + '</div>';
    var entry = (!missing && !subbed) ? null : subs.forKey(i.key);
    if (entry) {
      var avail = subs.availableFor(i.key, m);
      h += '<div class="hint">';
      if (avail.length) {
        var o = avail[0];
        h += '<button class="link grow left" data-a="subDlg" data-key="' + esc(i.key) + '">' + (subbed ? 'Using ' : 'You could use ') + esc(PR.usesLabel(o, m)) + ': ' + esc(o.text) + '</button>' +
          '<button class="pillbtn' + (subbed ? '' : ' on') + '" data-a="useSub" data-key="' + esc(i.key) + '" data-on="' + (!subbed) + '">' + (subbed ? 'Undo' : 'Use substitute') + '</button>';
      } else h += '<button class="link" data-a="subDlg" data-key="' + esc(i.key) + '">Substitute?</button>';
      h += '</div>';
      if (avail.length && PR.Substitutions.swapDetail(avail[0])) h += '<div class="faint sdet">' + esc(PR.Substitutions.swapDetail(avail[0])) + '</div>';
    }
    return h + '</div>';
  }
  function subsDialog(key) {
    var e = subs.forKey(key), m = matcher();
    if (!e) return;
    var html = '<div class="subq">' + e.options.map(function (o) {
      var ok = PR.optionAvailable(o, m);
      var det = PR.Substitutions.swapDetail(o);
      return '<p class="' + (ok ? 'ok' : '') + '">' + (ok ? '\u2713 ' : '\u2022 ') + esc(o.text) + (ok ? ' <span class="faint">(you have ' + esc(PR.usesLabel(o, m)) + ')</span>' : '') +
        (det ? '<span class="faint sdet">' + esc(det) + '</span>' : '') + '</p>';
    }).join('') + (e.note ? '<p class="tip">Tip: ' + esc(e.note) + '</p>' : '') + '</div>';
    dialog({ title: 'Instead of ' + e.name.toLowerCase(), html: html, buttons: [{ label: 'All substitutions', fn: function () { openSubs(e.name); } }, { label: 'OK', primary: true }] });
  }
  function planNote(id) {
    var t = D.today(), up = plan.filter(function (e) { return e.id === id && D.toDays(e.date) >= D.toDays(t); });
    return up.length ? '<div class="faint" style="margin-top:6px">' + ic('calendar', 'xs') + ' Planned for ' + esc(up.map(function (e) { return D.label(e.date); }).join(', ')) + '</div>' : '';
  }
  function useByDialog(p) {
    dialog({ title: 'Use-by date for ' + p.label.toLowerCase(), html: '<p class="muted">Items due within ' + PR.UseBy.SOON_DAYS + ' days show under \u201cUse soon\u201d, and recipes that use them are ranked first.</p>' +
      '<input class="tin" type="date" data-dv="d" value="' + esc(p.useBy || '') + '" min="2000-01-01" max="2099-12-31" aria-label="Use-by date">' +
      '<div class="chips" style="margin-top:10px">' + [[0, 'Today'], [1, 'Tomorrow'], [3, 'In 3 days'], [7, 'In a week']].map(function (x) { return '<button class="chip" data-a="dlgDate" data-v="' + x[0] + '">' + x[1] + '</button>'; }).join('') + '</div>',
      buttons: [{ label: 'Clear date', fn: function () { setUseBy(p.key, ''); toast('Use-by date cleared'); refresh(); } }, { label: 'Cancel' },
        { label: 'Save', primary: true, fn: function (v) { if (!D.valid(v.d || '')) { toast('Pick a date first'); return; } setUseBy(p.key, v.d); toast(p.label + ': ' + PR.UseBy.label(PR.UseBy.daysLeft(v.d, D.today())).toLowerCase()); refresh(); } }] });
  }
  function qtyDialog(p) {
    dialog({ title: 'How much ' + p.label.toLowerCase() + '?', html: '<p class="muted">Optional. Type an amount like \u201c500 g\u201d, \u201c2 cups\u201d or \u201c6\u201d. Recipes then show whether you have enough. Different kinds of units (e.g. grams vs cups) can\u2019t be compared.</p>' +
      '<input class="tin" type="text" data-dv="q" value="' + esc(p.qty || '') + '" maxlength="40" placeholder="e.g. 500 g" autocomplete="off" aria-label="Quantity">',
      buttons: [{ label: 'Clear', fn: function () { setQty(p.key, ''); refresh(); } }, { label: 'Cancel' }, { label: 'Save', primary: true, fn: function (v) { setQty(p.key, PR.jtrim(v.q || '')); refresh(); } }] });
    setTimeout(function () { var i = $('#dlg [data-dv="q"]'); if (i) i.focus(); }, 30);
  }
  function setUseBy(key, d) { var p = pantryItem(key); if (p) { p.useBy = d && D.valid(d) ? d : ''; savePantry(); } }
  function setQty(key, q) { var p = pantryItem(key); if (p) { p.qty = String(q || '').substring(0, 40); savePantry(); } }
  function planDialog(id, date0, servings) {
    var r = byId(id); if (!r) return;
    var t = D.today(), opts = '';
    for (var k = 0; k < 14; k++) { var d = D.addDays(t, k); opts += '<option value="' + d + '"' + (d === (date0 || t) ? ' selected' : '') + '>' + (k === 0 ? 'Today, ' : k === 1 ? 'Tomorrow, ' : '') + esc(D.label(d)) + '</option>'; }
    dialog({ title: 'Add to meal plan', html: '<p class="muted">' + esc(r.title) + '</p><label class="flabel">Day<select class="tin" data-dv="d">' + opts + '</select></label>' +
      '<label class="flabel">Servings<input class="tin" type="number" data-dv="s" min="1" max="24" value="' + (servings || defaultServings()) + '"></label>',
      buttons: [{ label: 'Cancel' }, { label: 'Add', primary: true, fn: function (v) { addToPlan(v.d, id, parseInt(v.s, 10) || defaultServings()); toast('Added to ' + D.label(v.d)); var s = topScreen(); if (s) renderScreen(s, true); } }] });
  }
  function openPrices(recipeId) { pushScreen({ type: 'prices', recipe: recipeId && byId(recipeId) ? recipeId : '' }); }
  function priceDialog(key, name) {
    var p = key ? prices.get(key) || PR.Cost.find(key, null, priceDefaults()) : null;
    dialog({ title: key ? 'Price of ' + (name || key) : 'Add a price', html: (key ? '' : '<label class="flabel">Ingredient<input class="tin" type="text" data-dv="n" maxlength="60" placeholder="e.g. chicken breast"></label>') +
      '<label class="flabel">Price (' + esc(currency()) + ')<input class="tin" type="number" data-dv="p" min="0" step="0.01" value="' + (p ? p.price : '') + '"></label>' +
      '<div class="row"><label class="flabel grow">For amount<input class="tin" type="number" data-dv="a" min="0" step="any" value="' + (p ? p.amount : 1) + '"></label>' +
      '<label class="flabel grow">Unit<select class="tin" data-dv="u">' + PR.Cost.UNITS.map(function (u, k) { return '<option value="' + u + '"' + ((p ? p.unit : '') === u ? ' selected' : '') + '>' + esc(PR.Cost.UNIT_NAMES[k]) + '</option>'; }).join('') + '</select></label></div>' +
      (p && !prices.has(key) ? '<div class="faint">Prefilled with the rough default price.</div>' : ''),
      buttons: [{ label: 'Cancel' }, { label: 'Save', primary: true, fn: function (v) {
        var k = key || PR.normalize(v.n || ''), pp = PR.Cost.make(parseFloat(v.p), parseFloat(v.a), v.u);
        if (!k) { toast('Type an ingredient name'); return; }
        if (!pp) { toast('Enter a price and an amount above 0'); return; }
        if (!prices.has(k) && prices.size >= PR.Cost.MAX_PRICES) { toast('Too many prices'); return; }
        prices.set(k, pp); savePrices(); version++; toast('Price saved'); var s = topScreen(); if (s) renderScreen(s, true);
      } }] });
  }
  var scanStream = null, scanLoop = null;
  function stopScan() { clearInterval(scanLoop); scanLoop = null; if (scanStream) { scanStream.getTracks().forEach(function (t) { t.stop(); }); scanStream = null; } }
  function openScan() {
    var s = { type: 'scan', code: '', state: '', camOk: !!(window.BarcodeDetector && navigator.mediaDevices && navigator.mediaDevices.getUserMedia), cam: false };
    s.onClose = stopScan;
    pushScreen(s);
  }
  function startCam(s) {
    var det;
    try { det = new window.BarcodeDetector({ formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e'] }); } catch (e) { s.camOk = false; renderScreen(s, true); return; }
    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false }).then(function (st) {
      if (topScreen() !== s) { st.getTracks().forEach(function (t) { t.stop(); }); return; }
      scanStream = st; s.cam = true; renderScreen(s, true);
      var v = $('#scanVid'); if (v) { v.srcObject = st; try { v.play(); } catch (e) { } }
      scanLoop = setInterval(function () {
        var vv = $('#scanVid'); if (!vv || vv.readyState < 2) return;
        det.detect(vv).then(function (codes) {
          var c = codes && codes[0] && PR.Barcode.clean(codes[0].rawValue || '');
          if (c && PR.Barcode.valid(c)) { stopScan(); s.cam = false; s.code = c; lookupCode(s, c); }
        }, function () { });
      }, 350);
    }, function () { s.cam = false; s.state = 'msg'; s.msg = 'The camera couldn\u2019t be opened (permission denied or no camera). Type the number instead.'; s.allowManual = false; renderScreen(s, true); });
  }
  function lookupCode(s, raw) {
    var c = PR.Barcode.clean(raw || '');
    s.code = raw; s.product = null; s.match = ''; s.alts = []; s.cached = false; s.allowManual = false;
    if (!PR.Barcode.valid(c)) { s.state = 'msg'; s.msg = 'That isn\u2019t a valid EAN/UPC barcode (8, 12 or 13 digits with a correct check digit).'; renderScreen(s, true); return; }
    s.code = c;
    if (barcodes.has(c)) { s.state = 'found'; s.cached = true; s.match = barcodes.get(c); renderScreen(s, true); return; }
    if (!isOnline()) { s.state = 'msg'; s.msg = 'You\u2019re offline, so the barcode can\u2019t be looked up. Type the item name instead.'; s.allowManual = true; renderScreen(s, true); return; }
    s.state = 'busy'; renderScreen(s, true);
    fetch(PR.Barcode.url(c), { credentials: 'omit', referrerPolicy: 'no-referrer' }).then(function (r) {
      if (r.status === 404) return '{"status":0}';
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.text();
    }).then(function (txt) {
      if (topScreen() !== s) return;
      var p = PR.Barcode.parse(txt);
      if (!p) { s.state = 'msg'; s.msg = 'Open Food Facts doesn\u2019t know this barcode. Type what it is and it will be remembered.'; s.allowManual = true; }
      else { s.state = 'found'; s.product = p; s.match = cap(PR.Barcode.match(p)); s.alts = PR.Barcode.matches(p, 4).map(cap).filter(function (m) { return m.toLowerCase() !== s.match.toLowerCase(); }); }
      renderScreen(s, true);
    }).catch(function () {
      if (topScreen() !== s) return;
      s.state = 'msg'; s.msg = 'Couldn\u2019t reach Open Food Facts. Check your connection, or type the item name.'; s.allowManual = true; renderScreen(s, true);
    });
  }
  /** "Cooked this": logs the date and servings, optionally keeps leftovers and (if ticked) takes the amounts out of the pantry. */
  function cookedDialog(id, servings) {
    var r = byId(id); if (!r) return;
    dialog({ title: 'Cooked ' + r.title, html: '<label class="flabel">Servings cooked<input class="tin" type="number" data-dv="s" min="1" max="72" value="' + servings + '"></label>' +
      '<label class="flabel">Leftover portions to keep<input class="tin" type="number" data-dv="p" min="0" max="99" value="0"></label>' +
      '<label class="flabel">Use leftovers within (days)<input class="tin" type="number" data-dv="d" min="1" max="14" value="' + leftoverDays() + '"></label>' +
      '<div class="faint">Cooked food is usually safe in the fridge for 3–4 days. Change the default in Settings.</div>' +
      '<label class="mcheck" style="margin-top:10px"><input type="checkbox" data-dv="x"' + (pref('deductPantry', false) ? ' checked' : '') + '> Take the used amounts out of my pantry</label>' +
      '<div class="faint">Only items with a quantity typed in the pantry change; units that can’t be compared are left alone.</div>',
      buttons: [{ label: 'Cancel' }, { label: 'Save', primary: true, fn: function (v) {
        var sv = Math.max(1, Math.min(72, parseInt(v.s, 10) || servings)), por = Math.max(0, Math.min(99, parseInt(v.p, 10) || 0));
        var days = PR.Leftovers.clampDays(parseInt(v.d, 10) || leftoverDays());
        PR.Leftovers.log(cooked, r.id, sv, D.today()); saveCooked();
        if (por > 0) { PR.Leftovers.add(leftovers, r.id, r.title, por, D.today(), days); saveLeftovers(); }
        setPref('deductPantry', !!v.x);
        var changed = v.x ? deductPantry(r, sv / Math.max(1, r.servings)) : 0;
        toast('Logged' + (por ? ' · ' + plural(por, 'leftover portion', 'leftover portions') : '') + (changed ? ' · ' + plural(changed, 'pantry item', 'pantry items') + ' updated' : ''));
        var s = topScreen(); if (s) renderScreen(s, true);
      } }] });
  }
  function deductPantry(r, scale) {
    var changed = 0, gone = [];
    pantry.forEach(function (p) {
      if (!p.qty) return;
      var one = new Map([[p.key, p.qty]]), cur = p.qty, hit = false;
      r.ingredients.forEach(function (i) {
        if (i.optional || i.key === '' || isNaN(i.amount) || PR.Amounts.qtyFor(i.key, one) == null) return;
        var nx = PR.Amounts.deduct(cur, p.key, i.amount * scale, i.unit);
        if (nx != null) { cur = nx; hit = true; }
      });
      if (!hit || cur === p.qty) return;
      changed++;
      if (parseFloat(cur) === 0 && !p.staple) gone.push(p.key); else p.qty = cur;
    });
    if (gone.length) pantry = pantry.filter(function (p) { return gone.indexOf(p.key) < 0; });
    if (changed) savePantry();
    return changed;
  }
  function addToPlan(date, id, servings) { if (!D.valid(date)) return; plan.push(PR.MealPlan.entry(date, id, servings)); savePlan(); }
  function openSubs(q) { pushScreen({ type: 'subs', q: q || '', cat: '' }); }
  function addMissing(s) {
    var r = byId(s.id), res = resultFor(r), scale = s.servings / Math.max(1, r.servings), added = 0;
    res.missing.forEach(function (i) {
      var amt = isNaN(i.amount) ? null : i.amount * scale, unit = isNaN(i.amount) ? '' : i.unit;
      if (addShopping(i.name.split(',')[0], amt, unit, r.title)) added++;
    });
    toast(added === 0 ? 'Already on your shopping list' : 'Added ' + added + ' item' + (added === 1 ? '' : 's') + ' to your shopping list');
    renderTabs();
  }

  function cookedNote(id) {
    var hs = PR.Leftovers.history(cooked, id);
    return hs[0] ? '<div class="faint" style="margin-top:6px">' + ic('check', 'xs') + ' Cooked ' + (hs[0] === 1 ? 'once' : hs[0] + ' times') + ' \u00b7 last ' + esc(D.label(hs[1])) + '</div>' : '';
  }
  /** Nutrition (USDA FDC table) and cost estimates. Each shows only when enough ingredients are covered, labelled with the coverage. */
  function estimatesCard(r, servings) {
    var n = PR.Nutrition.estimate(r, FOODS), c = PR.Cost.estimate(r, servings, prices, priceDefaults(), FOODS), h = '';
    h += '<div class="card est"><div class="row"><h2 class="grow" style="margin:0">Nutrition per serving</h2><span class="ebadge">Estimate \u00b7 ' + n.percent() + '%</span></div>';
    if (n.show()) {
      h += '<div class="nut">' + [['kcal', Math.round(n.kcal), ''], ['protein', Math.round(n.protein), ' g'], ['carbs', Math.round(n.carbs), ' g'], ['fat', Math.round(n.fat), ' g']].map(function (x) {
        return '<div><b>' + x[1] + x[2] + '</b><span class="faint">' + x[0] + '</span></div>'; }).join('') + '</div>' +
        '<div class="faint">Based on the original ' + plural(r.servings, 'serving', 'servings') + '; ' + n.covered + ' of ' + n.total + ' ingredients matched. ' + esc(PR.Nutrition.NOTE) + '</div>';
    } else h += '<div class="muted">Not enough ingredients could be matched (' + n.percent() + '%, needs ' + PR.Nutrition.MIN_COVERAGE + '%) for a useful estimate.</div>';
    h += '</div><div class="card est"><div class="row"><h2 class="grow" style="margin:0">Cost</h2><span class="ebadge">Estimate \u00b7 ' + c.percent() + '%</span></div>';
    if (c.show()) {
      h += '<div class="nut"><div><b>' + esc(money(c.perServing)) + '</b><span class="faint">per serving</span></div><div><b>' + esc(money(c.total)) + '</b><span class="faint">for ' + plural(servings, 'serving', 'servings') + '</span></div></div>' +
        '<div class="faint">Priced ' + c.covered + ' of ' + c.count + ' ingredients' + (c.fromDefaults ? ', ' + c.fromDefaults + ' from default prices (US: BLS regional averages, else built-in estimates \u00d7 region factor)' : '') + '. ' + esc(PR.Cost.NOTE) + '</div>';
    } else h += '<div class="muted">Only ' + c.percent() + '% of the ingredients have a price (needs ' + PR.Cost.MIN_COVERAGE + '%). Add your prices to see a cost.</div>';
    return h + '<button class="link" data-a="pricesFor">' + ic('list', 'xs') + ' Edit prices for this recipe</button></div>';
  }
  var SCREENS = {
    detail: {
      full: function (s) {
        var r = byId(s.id);
        if (!r) return topBar('Recipe', '') + '<div class="main"><div class="wrap">' + emptyBox('restaurant', 'Recipe not available', '') + '</div></div>';
        var fav = isFavorite(r.id);
        var heart = '<button class="iconbtn heart' + (fav ? ' on' : '') + '" data-a="fav" data-id="' + esc(r.id) + '" aria-pressed="' + fav + '" aria-label="Favorite">' + ic(fav ? 'heart' : 'heart_outline') + '</button>';
        var c = catColor(r.category);
        var band = '<div class="band" style="background:' + c + '"><span class="big">' + esc(r.title.charAt(0)) + '</span>' +
          (Photos.get(r.id) ? '<img alt="Your photo of ' + esc(r.title) + '" src="' + esc(Photos.get(r.id)) + '">' : r.image ? '<img alt="" decoding="async" referrerpolicy="no-referrer" src="' + esc(r.image + '/medium') + '">' : '') +
          '<div class="cap">' + esc([r.category, r.cuisine].filter(Boolean).join(' \u00b7 ').toUpperCase()) + '</div></div>';
        return topBar(r.title, '', heart) + '<div class="main"><div class="wrap">' + band + '<h2 class="dtitle">' + esc(r.title) + '</h2>' + (titleHint(r.id) ? '<div class="faint dhint">' + esc(titleHint(r.id)) + '</div>' : '') + sregion('body', s) + '</div></div>';
      },
      regions: {
        body: function (s) {
          var r = byId(s.id);
          if (!r) return '';
          var m = matcher(), res = resultFor(r), h = '<div class="chips meta">';
          var tm = PR.totalMinutes(r);
          h += metaChip(tm < 0 ? 'Time not listed' : timeText(tm), 'timer');
          if (r.prepMin >= 0 && r.cookMin >= 0) h += metaChip('Prep ' + r.prepMin + ' \u00b7 Cook ' + r.cookMin + ' min');
          mealsOf(r).forEach(function (t) { h += '<span class="mchip meal">' + ic('restaurant', 'xs') + esc(t) + '</span>'; });
          r.tags.forEach(function (t) { h += metaChip(dietLabel(t) + (r.tagsInferred ? '?' : ''), 'leaf'); });
          if (PR.isOnline(r)) h += '<span class="badge">THEMEALDB</span>';
          h += '</div>';
          var flags = PR.Allergens.flags(r), unsure = PR.Allergens.uncertainFlags(r), why = PR.Allergens.reasons(r, dislikes, allergens);
          if (why.length) h += '<div class="banner warn avoid">' + ic('warning') + '<div class="grow"><b>' + esc(why.join(' \u00b7 ')) + '</b><div class="muted">This recipe matches your dislikes/allergies, so it is hidden from lists. ' + esc(PR.Allergens.NOTE) + '</div></div></div>';
          if (flags.length) h += '<div class="aflags"><span class="faint">May contain:</span> ' + flags.map(function (f) { return '<span class="aflag' + (allergens.indexOf(f) >= 0 ? ' on' : '') + '">' + ic('warning', 'xs') + esc(PR.Allergens.flagLabel(r, f, unsure)) + '</span>'; }).join('') +
            '<div class="faint">Guessed from ingredient names, not a guarantee. Always check labels.</div></div>';
          if (r.tagsInferred && r.tags.length) h += '<div class="faint note">Diet tags marked ? are guessed from ' + (isUser(r) ? 'the' : 'TheMealDB\'s') + ' ingredient list and may be wrong. Check labels if it matters.</div>';
          h += detailExtras(r, s);
          var mt = res.missing.length === 0 ? 'You have everything you need' : 'You have ' + res.have + ' of ' + res.total + ' ingredients';
          var note = res.substituted.length ? 'Counting ' + res.substituted.length + (res.substituted.length === 1 ? ' substitute' : ' substitutes') + ' you have.' : '';
          if (res.swappable.length) note += (note ? ' ' : '') + res.swappable.length + ' missing ' + (res.swappable.length === 1 ? 'item has' : 'items have') + ' a substitute in your pantry.';
          h += '<div class="matchbox"><b>' + mt + '</b>' + (note ? '<div class="muted">' + esc(note) + '</div>' : '') + bar(res) +
            '<div class="row wrapbtns"><button class="btn" data-a="cook">' + ic('play', 's') + 'Cook mode</button>' +
            (res.missing.length ? '<button class="btn ghost" data-a="addMissing">' + ic('cart', 's') + 'Add ' + res.missing.length + ' missing to list</button>' : '') +
            '<button class="btn ghost" data-a="planAdd">' + ic('calendar', 's') + 'Add to plan</button>' +
            '<button class="btn ghost" data-a="cookedThis">' + ic('check', 's') + 'Cooked this</button></div>' + planNote(r.id) + cookedNote(r.id) + '</div>';
          var scale = s.servings / Math.max(1, r.servings);
          h += '<div class="card"><div class="row"><h2 class="grow" style="margin:0">Ingredients</h2><span class="muted">Servings</span>' +
            '<div class="stepper"><button class="iconbtn" data-a="serv" data-d="-1" aria-label="Fewer servings"' + (s.servings <= 1 ? ' disabled' : '') + '>' + ic('remove', 's') + '</button><b aria-live="polite">' + s.servings + '</b>' +
            '<button class="iconbtn" data-a="serv" data-d="1" aria-label="More servings"' + (s.servings >= 24 ? ' disabled' : '') + '>' + ic('add', 's') + '</button></div></div>';
          h += '<div class="chips batch"><span class="faint">Batch cook:</span>' + [1, 2, 3].map(function (k) { var v = Math.min(PR.MealPlan.MAX_SERVINGS, r.servings * k); return chip(k === 1 ? 'Recipe (' + r.servings + ')' : '\u00d7' + k + ' (' + v + ')', s.servings === v, 'batch', ' data-k="' + k + '"'); }).join('') +
            (s.servings > r.servings ? '<button class="link" data-a="planAdd">' + ic('calendar', 'xs') + ' Add batch to plan</button>' : '') + '</div>';
          var vm = unitPrefs(true).mode;
          h += '<div class="chips units" role="group" aria-label="Units for this recipe"><span class="faint">Units:</span>' + [['original', 'Recipe\u2019s own'], ['us', 'US'], ['metric', 'Metric'], ['weights', 'Weights']].map(function (u) {
            return chip(u[1], vm === u[0], 'detUnits', ' data-v="' + u[0] + '"');
          }).join('') + '<button class="link" data-a="conv">' + ic('swap', 'xs') + ' Conversions</button></div>';
          if (ui.detailUnits && ui.detailUnits !== unitMode()) h += '<div class="faint note">Showing ' + esc(lc1(PR.Units.modeLabel(ui.detailUnits))) + ' in recipes. Your default is ' + esc(lc1(PR.Units.modeLabel(unitMode()))) + ' (Settings \u2192 Measurement units).</div>';
          var meths = PR.Methods.of(r);
          if (meths.length) h += '<div class="faint note methods">Cooking method: ' + esc(meths.map(PR.Methods.label).join(' \u00b7 ')) + '</div>';
          if (!r.servingsKnown) h += '<div class="faint note">TheMealDB doesn\'t list servings, so quantities assume the recipe serves ' + r.servings + '.</div>';
          else if (s.servings !== r.servings) h += '<div class="faint note">Quantities scaled from the original ' + r.servings + ' servings.</div>';
          var amt = new Map();
          PR.Amounts.check(r, scale, qtyMap()).forEach(function (c) { amt.set(c.ingredient, c); });
          h += r.ingredients.map(function (i) { return ingredientRow(i, scale, res, m, amt); }).join('') + '</div>';
          h += estimatesCard(r, s.servings);
          h += '<div class="card"><h2>Method</h2>' + (r.steps.length ? '<ol class="steps">' + r.steps.map(function (st) { return '<li>' + esc(loc(stepText(st, true), true)) + '</li>'; }).join('') + '</ol>' : '<div class="muted">No steps listed.</div>') + '</div>';
          h += '<div class="card"><div class="row"><h2 class="grow" style="margin:0">Your photo</h2>' +
            '<input type="file" id="photoIn" accept="image/*" data-change="photoFile" hidden>' +
            '<button class="btn alt small" data-a="photoPick">' + ic('add', 's') + (Photos.get(r.id) ? 'Change' : 'Add photo') + '</button>' +
            (Photos.get(r.id) ? '<button class="btn ghost small" data-a="photoDel">Remove</button>' : '') + '</div>' +
            '<div class="faint" style="margin-top:6px">' + (Photos.mode() === 'idb' ? 'Saved in this browser only, shrunk to 800 px. Photos are not in backups unless you tick \u201cInclude photos\u201d.' : 'This browser can\u2019t store photos (no IndexedDB), so a photo stays only until you close the page.') + '</div></div>';
          var rt = rating(r.id) || { stars: 0, note: '' };
          h += '<div class="card rate"><h2>Your rating &amp; notes</h2><div class="stars" role="group" aria-label="Rating">' + [1, 2, 3, 4, 5].map(function (n) {
            return '<button class="star' + (n <= rt.stars ? ' on' : '') + '" data-a="rate" data-v="' + n + '" aria-label="' + n + ' star' + (n === 1 ? '' : 's') + '" aria-pressed="' + (n <= rt.stars) + '">' + ic(n <= rt.stars ? 'star' : 'star_outline') + '</button>';
          }).join('') + (rt.stars ? '<button class="link" data-a="rate" data-v="0">Clear</button>' : '') + '</div>' +
            '<textarea class="note" data-change="note" maxlength="' + PR.Backup.MAX_NOTE + '" rows="3" placeholder="Notes for next time, e.g. less salt, doubled the garlic" aria-label="Notes">' + esc(rt.note) + '</textarea><div class="faint">Notes save when you leave the box.</div></div>';
          if (isUser(r)) {
            h += '<div class="src">' + ic('leaf', 'xs') + ' Your imported recipe \u00b7 saved in this browser</div>';
            if (r.sourceUrl && /^https?:\/\//i.test(r.sourceUrl)) h += '<a class="src" href="' + esc(r.sourceUrl) + '" target="_blank" rel="noopener noreferrer">' + ic('cloud', 'xs') + ' Original page: ' + esc(r.sourceUrl.replace(/^https?:\/\//i, '').substring(0, 60)) + '</a>';
            h += '<button class="btn ghost block red" data-a="delRecipe">' + ic('delete', 's') + 'Delete this recipe</button>';
          } else if (PR.isOnline(r)) h += '<a class="src" href="https://www.themealdb.com/meal/' + esc(r.id.substring(2)) + '" target="_blank" rel="noopener noreferrer">' + ic('cloud', 'xs') + ' Recipe and photo from TheMealDB (themealdb.com). Tap to view the original page.</a>';
          else h += '<div class="src">' + ic('leaf', 'xs') + ' My Pantry Recipes original recipe \u00b7 works offline</div>';
          return h;
        }
      }
    },
    list: {
      full: function (s) { return topBar(s.title, s.sub || '') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
      regions: {
        body: function (s) {
          var items = s.items();
          var h = (s.top ? s.top(s) : '') + (s.hidden ? '<div class="faint note">' + plural(s.hidden, 'dessert/side', 'desserts/sides') + ' hidden \u2013 tick Include desserts / Include sides on Discover</div>' : '') + (items.length ? '<div class="list two">' + items.slice(0, s.limit || 120).map(recipeRow).join('') + '</div>' : emptyBox('search', 'No recipes here yet', s.empty || ''));
          if (items.length > (s.limit || 120)) h += '<button class="btn ghost block" data-a="listMore">Show more</button>';
          if (s.browse) {
            if (s.busy) h += '<div class="loading"><span class="spin"></span> Loading from TheMealDB\u2026</div>';
            else if (s.msg) h += '<div class="faint note center">' + esc(s.msg) + '</div>';
            if (!s.busy && s.remaining > 0) h += '<button class="btn ghost block" data-a="browseMore">' + ic('cloud', 's') + 'Load more from TheMealDB (' + s.remaining + ' left)</button>';
          }
          return h;
        }
      }
    },
    subs: {
      full: function (s) {
        return topBar('Substitutions', '') + '<div class="main"><div class="wrap"><div class="field">' + ic('search', 's') +
          '<input type="search" data-in="subq" value="' + esc(s.q) + '" placeholder="Search, e.g. buttermilk, egg, wine" autocomplete="off" autocorrect="off" autocapitalize="none" spellcheck="false" enterkeyhint="search" aria-label="Search substitutions"></div>' +
          '<div class="hscroll">' + sregion('cats', s) + '</div>' + sregion('body', s) + '</div></div>';
      },
      regions: {
        cats: function (s) {
          return '<div class="chips nowrap">' + [''].concat(subs.categories).map(function (c) { return chip(c === '' ? 'All' : esc(c), c === s.cat, 'subCat', ' data-v="' + esc(c) + '"'); }).join('') + '</div>';
        },
        body: function (s) {
          var m = matcher(), list = subs.search(s.q).filter(function (e) { return !s.cat || e.category === s.cat; });
          var have = list.filter(function (e) { return e.options.some(function (o) { return PR.optionAvailable(o, m); }); }).length;
          var h = '<div class="muted note">' + list.length + (list.length === 1 ? ' ingredient' : ' ingredients') + ' \u00b7 ' + have + ' you can swap with your pantry</div>';
          if (!list.length) return h + '<div class="muted center pad">No substitutions match \u201c' + esc(s.q) + '\u201d.</div>';
          return h + '<div class="list two">' + list.slice(0, s.limit || 80).map(function (e) {
            return '<div class="scard"><div class="row"><h3 class="grow">Instead of ' + esc(e.name.toLowerCase()) + '</h3>' + (m.has(e.keys[0]) ? '<span class="pbadge">in pantry</span>' : '') + '</div>' +
              (e.aliases.length ? '<div class="faint">Also: ' + esc(e.aliases.join(', ')) + '</div>' : '') +
              e.options.map(function (o) {
                var ok = PR.optionAvailable(o, m);
                var det = PR.Substitutions.swapDetail(o);
                return '<div class="opt' + (ok ? ' ok' : '') + '">' + (ok ? ic('check', 'xs') : '\u2022 ') + esc(o.text) + (ok ? '<div class="yes">You have: ' + esc(PR.usesLabel(o, m)) + '</div>' : '') +
                  (det ? '<div class="faint sdet">' + esc(det) + '</div>' : '') + '</div>';
              }).join('') + (e.note ? '<div class="tip">Tip: ' + esc(e.note) + '</div>' : '') + '</div>';
          }).join('') + '</div>' + (list.length > (s.limit || 80) ? '<button class="btn ghost block" data-a="subsMore">Show more</button>' : '');
        }
      }
    },
    about: {
      full: function () {
        var th = Theme.current(), act = Theme.state().active;
        var thName = act === 'system' ? 'Follow system (' + th.name + ' now)' : th.name;
        var row = function (act2, icon, title, sub) { return '<button class="setrow" data-a="' + act2 + '">' + ic(icon) + '<span class="grow"><b>' + title + '</b><span class="faint">' + sub + '</span></span>' + ic('next', 's') + '</button>'; };
        var avoidN = dislikes.length + allergens.length;
        return topBar('Settings & About', 'My Pantry Recipes ' + SHORT_VERSION) + '<div class="main"><div class="wrap">' +
          '<div class="card setlist"><h2>Settings</h2>' +
          row('region20', 'explore', 'Region &amp; formats', esc(R20.name(regionId())) + ' \u00b7 units, dates, week start, clock, allergen list') +
          row('inv', 'kitchen', 'Kitchen inventory', 'Pantry, fridge, freezer, spice rack \u00b7 restock list') +
          row('myRecipes', 'list', 'My recipes', 'Write, edit, share packs, printable cookbook' + (userRecipes.size ? ' \u00b7 ' + userRecipes.size : '')) +
          row('prices20', 'cart', 'Regional prices &amp; budget', (regionId() && regionId() !== R20.US ? 'Your prices only' : (RP.cleanZip(pref('zip', '')) ? 'ZIP ' + esc(RP.cleanZip(pref('zip', ''))) : 'No ZIP')) + (budget() ? ' \u00b7 budget ' + esc(money(budget())) : '')) +
          row('appearance', 'leaf', 'Appearance', esc(thName) + ' \u00b7 light, dark, presets and your own colours') +
          '<label class="switchrow"><span class="grow">Decorations<span class="faint" style="display:block">Coloured header, title underlines and small accents. Static only, no animations.</span></span><input type="checkbox" class="switch" data-change="decorations"' + (decoOn() ? ' checked' : '') + '></label>' +
          row('avoid', 'warning', 'Dislikes &amp; allergies', avoidN ? avoidN + ' set \u00b7 ' + hiddenByAvoid() + ' recipes hidden' : 'Hide recipes with ingredients you avoid') +
          row('plan', 'calendar', 'Meal plan', plan.length ? plural(plan.length, 'planned meal', 'planned meals') : 'Plan the week and get one shopping list') +
          row('conv', 'swap', 'Measurement units &amp; conversions', esc(PR.Units.modeLabel(unitMode())) + ' \u00b7 cups, grams, ounces, oven temperatures') +
          row('helpers20', 'swap', 'Kitchen helpers', 'Equivalents, Rescue my dish, spice blends, cooking times, ingredient names') +
          row('baking30', 'timer', 'Baking', 'Bake recipes, baker\u2019s %, pan sizes, dough timeline, oven &amp; altitude, starter, bake log') +
          row('holidays30', 'calendar', 'Holidays &amp; fasting', 'Calendar, reminders, holiday recipes, fasting-day labels') +
          row('catMoves21', 'list', 'Ingredient categories &amp; glossary', (catMoves().size ? catMoves().size + ' moved' : 'Move an ingredient to the category you prefer') + ' \u00b7 botanical fruits') +
          row('medGuides201', 'warning', 'Medical diet guides', 'Renal, cardiac and diabetic guides – off by default; not medical advice') +
          row('regional30', 'explore', 'Regional sets', 'Recipes grouped by region and country') +
          row('ipSheet', 'timer', 'Instant Pot cheat sheet', 'Approximate pressure-cooking times and tips') +
          row('prices', 'cart', 'Prices &amp; currency', (prices.size ? plural(prices.size, 'price', 'prices') + ' set' : 'Rough default prices') + ' \u00b7 ' + esc(currency()) + ' \u00b7 cost estimates') +
          '<div class="switchrow"><span class="grow">Leftovers keep for<span class="faint">Default use-by for \u201cCooked this\u201d</span></span><div class="stepper"><button class="iconbtn" data-a="loDays" data-d="-1" aria-label="Fewer days"' + (leftoverDays() <= 1 ? ' disabled' : '') + '>' + ic('remove', 's') + '</button><b>' + plural(leftoverDays(), 'day', 'days') + '</b><button class="iconbtn" data-a="loDays" data-d="1" aria-label="More days"' + (leftoverDays() >= 14 ? ' disabled' : '') + '>' + ic('add', 's') + '</button></div></div>' +
          row('importRecipe', 'add', 'Import a recipe', 'Paste text or schema.org JSON-LD' + (userRecipes.size ? ' \u00b7 ' + userRecipes.size + ' imported' : '')) +
          row('backup', 'cloud', 'Backup &amp; restore', 'One JSON file that works in the web and Android apps') +
          '<label class="switchrow"><span class="grow">Find recipes online (TheMealDB)</span><input type="checkbox" class="switch" data-change="online"' + (onlineEnabled() ? ' checked' : '') + '></label>' +
          '<div class="field" style="margin-top:6px"><input type="text" data-change="apikey" value="' + esc(apiKey() === '1' ? '' : apiKey()) + '" placeholder="TheMealDB API key (default: 1)" autocomplete="off" autocapitalize="none" spellcheck="false" aria-label="TheMealDB API key"></div></div>' +
          '<div class="card about"><h2>About</h2>' +
          '<p class="vline"><b>My Pantry Recipes</b> <span class="ver big">' + esc(SHORT_VERSION) + '</span><br><span class="faint">Version ' + esc(VERSION) + ' \u00b7 app files cache \u201cpr-core-' + esc(VERSION) + '\u201d</span></p>' +
          '<p>Find recipes from what you already have.</p>' +
          '<p>Free &amp; open source (MIT License). No ads, no tracking, no accounts. Your pantry, favorites, plan, ratings and shopping list stay in this browser on this device' + (Store.persistent ? '' : ' (storage is blocked here, so they are kept only until you close the page)') + '.</p>' +
          '<p>Built-in recipes: ' + bundled.length + ' original recipes written for this app; they work offline.</p>' +
          '<p>Online recipes and photos come from TheMealDB (www.themealdb.com), a free crowd-sourced database. The app uses TheMealDB\'s public test API key \u201c1\u201d, which TheMealDB offers for development and educational/personal use. If you redistribute this app, get a supporter API key from TheMealDB and enter it above.</p>' +
          '<p>Barcode lookups send only the barcode number to Open Food Facts (world.openfoodfacts.org), and only when you look one up. ' + esc(PR.Barcode.ATTRIBUTION) + '</p>' +
          '<p>Nutrition estimates use a small table derived from USDA FoodData Central (public domain, U.S. Department of Agriculture). ' + esc(PR.Nutrition.NOTE) + '</p>' +
          '<p>' + esc(PR.Cost.NOTE) + ' The bundled default prices are rough US ballpark figures, not live prices.</p>' +
          '<p>The only network connections go to themealdb.com and, for barcode lookups, world.openfoodfacts.org.</p>' +
          '<p>Diet tags on TheMealDB recipes (shown with ?) are guessed from ingredient names and can be wrong. TheMealDB doesn\'t list servings, so those are assumed to serve 4. ' + esc(PR.Allergens.NOTE) + '</p>' +
          '<p>Ingredient swaps (' + subs.entries.length + ' ingredients), the ingredient density table (' + densities().list.length + ' ingredients, grams per cup) and the Instant Pot cheat sheet are original content written for this app (MIT). Conversions between cups and grams are kitchen estimates: weigh when it matters. Pressure-cooking times are approximate; check your cooker\u2019s manual.</p>' +
          '<p>Icons: Material Design icons by Google (Apache License 2.0).</p>' +
          '<p class="faint">Saved TheMealDB recipes: ' + online.size + ' \u00b7 cache: ' + Cache.mode() + '</p>' +
          '</div></div></div>';
      }
    },
    conv: {
      full: function (s) {
        var U = PR.Units;
        var sel = function (id, list, def) { return '<select class="tin" id="' + id + '" data-change="conv" aria-label="Unit">' + list.map(function (u) { return '<option value="' + esc(u[0]) + '"' + (u[0] === def ? ' selected' : '') + '>' + esc(u[1]) + '</option>'; }).join('') + '</select>'; };
        var amt = function (id, val, label) { return '<input class="tin cvamt" type="text" inputmode="decimal" id="' + id + '" data-in="conv" value="' + esc(val) + '" autocomplete="off" aria-label="' + esc(label) + '" placeholder="e.g. 1 1/2 or 1-2">'; };
        var vu = U.VOLUME_UNITS.concat(U.MASS_UNITS).map(function (u) { return [u, u]; });
        return topBar('Measurement units & conversions', U.modeLabel(unitMode())) + '<div class="main"><div class="wrap conv">' +
          sregion('settings', s) +
          '<div class="card"><h2>Volume &amp; weight</h2><div class="row cvrow">' + amt('cvA', '1', 'Amount') + sel('cvU', vu, 'cup') + '</div><div class="cvout" id="cvOut" aria-live="polite"></div>' +
          '<div class="faint">US cup = 240 ml (nutrition-label size; the customary cup is 236.6 ml, about 1.5% less), tbsp = 15 ml, tsp = 5 ml, fl oz = 30 ml, oz = 28.35 g, lb = 453.6 g.</div></div>' +
          '<div class="card"><h2>Ingredient cups \u2194 grams \u2194 ounces</h2><label class="flabel">Ingredient<input class="tin" type="text" id="cvI" data-in="conv" list="cvDens" value="all-purpose flour" autocomplete="off" autocapitalize="none" aria-label="Ingredient"></label>' +
          '<datalist id="cvDens">' + densities().list.map(function (d) { return '<option value="' + esc(d.name) + '">'; }).join('') + '</datalist>' +
          '<div class="row cvrow">' + amt('cvIA', '1', 'Ingredient amount') + sel('cvIU', [['cup', 'cup'], ['tbsp', 'tbsp'], ['tsp', 'tsp'], ['ml', 'ml'], ['g', 'g'], ['oz', 'oz'], ['lb', 'lb'], ['kg', 'kg']], 'cup') + '</div>' +
          '<div class="cvout" id="cvIOut" aria-live="polite"></div><div class="faint">' + densities().list.length + ' ingredients. Spooned-and-levelled averages: a packed or sifted cup can differ by 10\u201320%, so weigh when it matters.</div></div>' +
          '<div class="card"><h2>Butter</h2><div class="row cvrow">' + amt('cvB', '1', 'Butter amount') + sel('cvBU', [['stick', 'US stick'], ['tbsp', 'tbsp'], ['cup', 'cup'], ['g', 'g'], ['oz', 'oz']], 'stick') + '</div>' +
          '<div class="cvout" id="cvBOut" aria-live="polite"></div><div class="faint">1 US stick = \u00bd cup = 8 tbsp \u2248 113 g = 4 oz.</div></div>' +
          '<div class="card"><h2>Oven temperature</h2><div class="row cvrow">' + amt('cvT', '180', 'Temperature') + sel('cvTU', [['C', '\u00b0C'], ['F', '\u00b0F'], ['gas', 'Gas mark']], 'C') + '</div>' +
          '<div class="cvout" id="cvTOut" aria-live="polite"></div>' +
          '<table class="otable"><caption class="faint">Oven guide (approximate; fan/convection ovens about 20 \u00b0C lower)</caption><thead><tr><th>\u00b0C</th><th>\u00b0F</th><th>Gas</th><th>Fan \u00b0C</th><th></th></tr></thead><tbody>' +
          U.OVEN.map(function (r) { return '<tr><td>' + r[0] + '</td><td>' + r[1] + '</td><td>' + r[2] + '</td><td>' + r[3] + '</td><td class="faint">' + esc(r[4]) + '</td></tr>'; }).join('') + '</tbody></table></div>' +
          '<div class="card"><h2>Rounding</h2><p class="faint">Grams: under 10 g to 1 g, flour from 20 g and anything from 100 g to 5 g. Teaspoons in \u00bc steps (\u215b tsp, then \u201cpinch\u201d), tablespoons in \u00bd steps, cups in \u00bc and \u2153 steps, ounces in \u00bc oz under 4 oz and \u00bd oz under 16 oz. Amounts can be typed as 1.5, 1,5, 3/4, 1 1/2, 1\u00bd or a range such as 1-2 or 1 to 2.</p>' +
          '<button class="link" data-a="ipSheet">' + ic('timer', 'xs') + ' Instant Pot cheat sheet</button></div>' +
          '</div></div>';
      },
      regions: {
        settings: function () {
          var mode = unitMode(), wu = pref('weightUnit', 'g') === 'oz' ? 'oz' : 'g';
          return '<div class="card"><h2>Measurement units</h2><div class="faint">How recipe amounts are shown in recipes, cook mode, the shopping list and the meal plan. Each recipe also has its own Units switch.</div>' +
            '<div class="chips" role="group" aria-label="Measurement units">' + PR.Units.MODES.map(function (m) { return chip(PR.Units.modeLabel(m), mode === m, 'unitsMode', ' data-v="' + m + '"'); }).join('') + '</div>' +
            (mode === 'weights' ? '<div class="chips" role="group" aria-label="Weight unit"><span class="faint">Weights in</span>' + chip('Grams', wu === 'g', 'unitsWeight', ' data-v="g"') + chip('Ounces', wu === 'oz', 'unitsWeight', ' data-v="oz"') + '</div>' +
              '<label class="switchrow"><span class="grow">Show cups in brackets after weights<span class="faint" style="display:block">For example 250 g (2 cups)</span></span><input type="checkbox" class="switch" data-change="showCups"' + (pref('showCups', true) !== false ? ' checked' : '') + '></label>' : '') +
            '<div class="faint note">Example: 2 cups flour \u2192 ' + esc(qtyText('flour', 2, 'cup', 1, false)) + ' \u00b7 1 cup milk \u2192 ' + esc(qtyText('milk', 1, 'cup', 1, false)) + ' \u00b7 200\u00b0C \u2192 ' + esc(stepText('200\u00b0C', false)) + '</div></div>';
        }
      },
      after: function () { convUpdate(); }
    },
    ipsheet: {
      full: function () {
        var d = window.PR_DATA17;
        return topBar('Instant Pot cheat sheet', 'Approximate times at High pressure') + '<div class="main"><div class="wrap">' +
          '<div class="banner warn">' + ic('warning') + '<div class="grow">' + esc(d.ipDisclaimer) + '</div></div>' +
          '<div class="card"><table class="otable ip"><thead><tr><th>Food</th><th>Liquid</th><th>Minutes at High</th><th>Release</th></tr></thead><tbody>' +
          d.ipTimes.map(function (r) { return '<tr><td>' + esc(r[0]) + '</td><td class="faint">' + esc(r[1]) + '</td><td><b>' + esc(r[2]) + '</b></td><td>' + esc(r[3]) + '</td></tr>'; }).join('') + '</tbody></table></div>' +
          '<div class="card"><h2>Tips</h2><ul class="tips">' + d.ipTips.map(function (t) { return '<li>' + esc(t) + '</li>'; }).join('') + '</ul></div>' +
          '<button class="btn ghost block" data-a="browse" data-kind="k" data-v="instant-pot">' + ic('restaurant', 's') + 'Instant Pot recipes</button>' +
          '</div></div>';
      }
    },
    plan: {
      full: function (s) {
        var ws = s.week, we = D.addDays(ws, 6);
        return topBar('Meal plan', D.label(ws) + ' \u2013 ' + D.label(we)) + '<div class="main"><div class="wrap">' +
          '<div class="row weeknav"><button class="btn alt small" data-a="planWeek" data-d="-7">' + ic('prev', 's') + 'Previous</button><span class="grow center">' +
          (ws === wkStart(D.today()) ? '<b>This week</b>' : '<button class="link" data-a="planWeek" data-d="0">Back to this week</button>') + '</span><button class="btn alt small" data-a="planWeek" data-d="7">Next' + ic('next', 's') + '</button></div>' +
          sregion('body', s) + '</div></div>';
      },
      regions: {
        body: function (s) {
          var t = D.today(), entries = PR.MealPlan.forWeek(plan, s.week), h = '';
          var bw = +pref('budgetWeek', 0) || 0;
          if (bw > 0) { var wc = planWeekCost(s.week); h += '<div class="banner' + (wc > bw ? ' warn' : '') + '">' + ic('cart') + '<div class="grow"><b>Weekly budget ' + esc(money(bw)) + '</b><div class="muted">' + (wc < 0 ? 'No cost estimate for this week yet.' : 'Planned meals: about ' + esc(money(wc)) + (wc > bw ? ', over budget' : '')) + '</div></div></div>'; }
          D.week(s.week).forEach(function (d) {
            var es = entries.filter(function (e) { return e.date === d; });
            h += '<div class="card pday' + (d === t ? ' today' : '') + '"><div class="row"><h2 class="grow" style="margin:0">' + esc(D.label(d)) + (d === t ? ' <span class="pbadge">Today</span>' : '') + '</h2>' +
              '<button class="link" data-a="planPick" data-d="' + d + '">' + ic('add', 's') + 'Add</button></div>';
            es.forEach(function (e) {
              var r = byId(e.id), i = plan.indexOf(e);
              h += '<div class="pentry">' + (r ? '<button class="grow left plink" data-a="open" data-id="' + esc(e.id) + '">' + esc(r.title) + '<span class="faint">' + esc(mealText(r, 2)) + '</span></button>' : '<span class="grow faint">Recipe not available on this device</span>') +
                '<div class="stepper"><button class="iconbtn" data-a="planServ" data-i="' + i + '" data-d="-1" aria-label="Fewer servings"' + (e.servings <= 1 ? ' disabled' : '') + '>' + ic('remove', 's') + '</button><b title="servings">' + e.servings + '</b>' +
                '<button class="iconbtn" data-a="planServ" data-i="' + i + '" data-d="1" aria-label="More servings"' + (e.servings >= 24 ? ' disabled' : '') + '>' + ic('add', 's') + '</button></div>' +
                '<button class="iconbtn" data-a="planDel" data-i="' + i + '" aria-label="Remove from plan">' + ic('delete', 's') + '</button></div>';
            });
            if (!es.length) h += '<div class="faint">Nothing planned.</div>';
            h += '</div>';
          });
          var items = PR.MealPlan.shopping(entries, byId, matcher(), qtyMap());
          h += '<div class="card"><h2>Shopping for this week <span class="faint">(' + items.length + ')</span></h2>';
          if (!entries.length) h += '<div class="muted">Add recipes to the plan and their ingredients are combined here. Staples and things in your pantry are left out.</div>';
          else if (!items.length) h += '<div class="muted">You have everything for this week\u2019s plan.</div>';
          else {
            h += '<div class="muted" style="margin-bottom:6px">Combined across recipes and scaled to the planned servings. Staples and pantry items are left out' + (qtyMap().size ? '; where you set a quantity, only the shortfall is listed' : '') + '.</div>';
            h += items.map(function (it) { var q = isNaN(it.amount) ? '' : qtyText(it.key, it.amount, it.unit, 1, false); return '<div class="pshop"><span class="grow">' + (q ? '<b>' + esc(q) + '</b> ' : '') + esc(it.name) + (it.partial ? ' <span class="faint">(more than you have)</span>' : '') + '<span class="faint">For ' + esc(it.recipes.join(', ')) + '</span></span></div>'; }).join('');
            h += '<button class="btn block" style="margin-top:10px" data-a="planToList">' + ic('cart', 's') + 'Add week to shopping list</button>';
          }
          h += '</div>';
          if (entries.length) h += '<button class="btn alt block" data-a="planShare">' + ic('share', 's') + 'Share this week’s plan</button>';
          return h;
        }
      }
    },
    avoid: {
      full: function (s) {
        return topBar('Dislikes & allergies', '') + '<div class="main"><div class="wrap">' +
          '<div class="banner warn">' + ic('warning') + '<div class="grow"><b>Name-based, not a guarantee</b><div class="muted">' + esc(PR.Allergens.NOTE) + '</div></div></div>' + sregion('body', s) + '</div></div>';
      },
      regions: {
        body: function () {
          var h = '<div class="card"><h2>Allergies to avoid</h2><div class="muted" style="margin-bottom:8px">Recipes with a matching ingredient name are hidden. Ingredients are flagged on every recipe.</div><div class="chips">' +
            regionAllergenIds().concat(allergens.filter(function (x) { return regionAllergenIds().indexOf(x) < 0; })).map(function (id) { var on = allergens.indexOf(id) >= 0; return chip((on ? ic('check', 'xs') : '') + esc(R20.allergenLabel(id, regionId())), on, 'avoidToggle', ' data-v="' + id + '"'); }).join('') + '</div><div class="faint" style="margin-top:6px">' + esc(R20.allergenListName(regionId())) + ' (Settings \u2192 Region).</div></div>' +
            '<button class="setrow card" data-a="household">' + ic('warning') + '<span class="grow"><b>Household allergies</b><span class="faint">' + plural(household().length, 'person', 'people') + ' \u00b7 alerts on recipes</span></span>' + ic('next', 's') + '</button>';
          h += '<div class="card"><h2>Ingredients I don\u2019t eat</h2><div class="muted" style="margin-bottom:8px">E.g. coriander, mushrooms, pork. Optional ingredients don\u2019t hide a recipe.</div>' +
            '<div class="row"><div class="field grow"><input type="text" id="disIn" data-enter="disAdd" placeholder="Add an ingredient" autocomplete="off" autocapitalize="none" aria-label="Add disliked ingredient"></div><button class="btn" data-a="disAdd">Add</button></div>' +
            (dislikes.length ? '<div class="chips" style="margin-top:10px">' + dislikes.map(function (d) { return '<button class="chip on" data-a="disDel" data-v="' + esc(d) + '" aria-label="Remove ' + esc(d) + '">' + esc(cap(d)) + ' ' + ic('close', 'xs') + '</button>'; }).join('') + '</div>' : '') + '</div>';
          var hn = hiddenByAvoid();
          h += '<div class="muted center pad">' + hn + (hn === 1 ? ' recipe is' : ' recipes are') + ' hidden right now.</div>';
          return h;
        }
      }
    },
    scan: {
      full: function (s) {
        return topBar('Barcode to pantry', 'Look up a product and add it') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>';
      },
      regions: {
        body: function (s) {
          var h = '<div class="card"><h2>Scan</h2>';
          if (s.camOk) h += (s.cam ? '<video id="scanVid" class="scanvid" playsinline muted autoplay></video><button class="btn ghost block" data-a="scanStop">Stop camera</button>' :
              '<button class="btn block" data-a="scanCam">' + ic('photo', 's') + 'Use camera</button>') + '<div class="faint" style="margin-top:6px">Uses this browser\u2019s built-in barcode reader. Nothing is recorded or uploaded.</div>';
          else h += '<div class="muted">This browser has no built-in barcode reader (BarcodeDetector), so camera scanning isn\u2019t available here. Type the number printed under the barcode instead.</div>';
          h += '<div class="row" style="margin-top:12px"><div class="field grow">' + ic('barcode', 's') + '<input type="text" id="codeIn" inputmode="numeric" data-enter="scanLookup" maxlength="20" value="' + esc(s.code || '') + '" placeholder="Barcode number, e.g. 3017620422003" autocomplete="off" aria-label="Barcode number"></div><button class="btn" data-a="scanLookup">Look up</button></div></div>';
          if (s.state === 'busy') h += '<div class="loading"><span class="spin"></span> Asking Open Food Facts\u2026</div>';
          else if (s.state === 'msg') h += '<div class="banner warn">' + ic('warning') + '<div class="grow">' + esc(s.msg) + '</div></div>';
          if (s.state === 'found' || s.state === 'msg' && s.allowManual) {
            h += '<div class="card"><h2>' + (s.product ? esc(s.product.title()) : s.cached ? 'Seen before' : 'Add it yourself') + '</h2>' +
              (s.product ? '<div class="faint" style="margin-bottom:8px">Barcode ' + esc(s.code) + (s.product.categories.length ? ' \u00b7 ' + esc(s.product.categories.slice(-2).join(', ')) : '') + '</div>' : '') +
              (s.state === 'found' && !s.match && !s.cached ? '<div class="muted" style="margin-bottom:8px">No close match in the ingredient list; edit the name below.</div>' : '') +
              '<label class="flabel">Pantry item<input class="tin" type="text" id="scanName" value="' + esc(s.match || '') + '" maxlength="60" aria-label="Pantry item name"></label>' +
              (s.alts && s.alts.length ? '<div class="faint" style="margin-top:6px">Or did you mean:</div><div class="chips">' + s.alts.map(function (m) { return '<button class="chip" data-a="scanPick" data-v="' + esc(m) + '">' + esc(m) + '</button>'; }).join('') + '</div>' : '') +
              (s.product && s.product.quantity ? '<label class="mcheck"><input type="checkbox" id="scanQty" checked> Also note the quantity (' + esc(s.product.quantity) + ')</label>' : '') +
              '<button class="btn block" style="margin-top:10px" data-a="scanAdd">' + ic('add', 's') + 'Add to pantry</button></div>';
          }
          return h + '<div class="faint note">' + esc(PR.Barcode.ATTRIBUTION) + ' Looking up sends only the barcode number to world.openfoodfacts.org. Codes you confirm are remembered on this device (' + barcodes.size + ' saved).</div>';
        }
      }
    },
    prices: {
      full: function (s) {
        return topBar('Prices & currency', s.recipe ? byId(s.recipe).title : 'For cost estimates') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>';
      },
      regions: {
        body: function (s) {
          var h = '<div class="card"><h2>Settings</h2><label class="flabel">Currency symbol<input class="tin" type="text" data-change="currency" maxlength="4" value="' + esc(currency()) + '" aria-label="Currency symbol"></label>' +
            '<label class="switchrow"><span class="grow">Use default prices<span class="faint">US regions only: BLS regional averages, else built-in estimates \u00d7 a regional factor, used where you haven\u2019t set one. Averages, not live store prices. Elsewhere only your own prices are used.</span></span><input type="checkbox" class="switch" data-change="useDefaultPrices"' + (useDefaultPrices() ? ' checked' : '') + '></label>' +
            '<label class="switchrow"><span class="grow">Show cost on the shopping list</span><input type="checkbox" class="switch" data-change="shopCost"' + (pref('shopCost', false) ? ' checked' : '') + '></label></div>';
          if (s.recipe) {
            var r = byId(s.recipe), keys = [];
            h += '<div class="card"><h2>Ingredients in this recipe</h2>' + r.ingredients.filter(function (i) { if (i.optional || !i.key || keys.indexOf(i.key) >= 0) return false; keys.push(i.key); return true; }).map(function (i) {
              var p = PR.Cost.find(i.key, prices, priceDefaults()), mine = prices.has(i.key) || prices.has(PR.family(i.key));
              return '<div class="prow"><span class="grow">' + esc(PR.BuyNext.shortName(i.name)) + '<span class="faint">' + (p ? esc(PR.Cost.describe(currency(), p)) + (mine ? '' : ' \u00b7 default') : 'No price') + '</span></span><button class="btn alt small" data-a="priceEdit" data-key="' + esc(i.key) + '" data-name="' + esc(PR.BuyNext.shortName(i.name)) + '">' + (mine ? 'Edit' : 'Set') + '</button></div>';
            }).join('') + '</div>';
          }
          var mine = Array.from(prices.keys()).sort();
          h += '<div class="card"><div class="row"><h2 class="grow" style="margin:0">Your prices <span class="faint">(' + mine.length + ')</span></h2><button class="link" data-a="priceEdit" data-key="">' + ic('add', 'xs') + ' Add</button></div>' +
            (mine.length ? mine.map(function (k) {
              return '<div class="prow"><span class="grow">' + esc(cap(k)) + '<span class="faint">' + esc(PR.Cost.describe(currency(), prices.get(k))) + '</span></span><button class="iconbtn" data-a="priceEdit" data-key="' + esc(k) + '" data-name="' + esc(cap(k)) + '" aria-label="Edit price">' + ic('list', 's') + '</button><button class="iconbtn" data-a="priceDel" data-key="' + esc(k) + '" aria-label="Delete price">' + ic('delete', 's') + '</button></div>';
            }).join('') : '<div class="muted">None yet. Prices are per package, e.g. 2.50 for 500 g, or 0.40 each.</div>') + '</div>';
          return h + '<div class="faint note">' + esc(PR.Cost.NOTE) + ' Prices are included in backups.</div>';
        }
      }
    },
    appearance: {
      full: function (s) {
        return topBar('Appearance', 'Theme, colours, text size and corners') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>';
      },
      regions: {
        body: function () {
          var st = Theme.state(), act = st.active, P = PR.Palette, cur = Theme.current();
          var tg = ui.themeGroup || (P.preset(act) && P.preset(act).group) || P.GROUPS[0]; if (P.GROUPS.indexOf(tg) < 0) tg = P.GROUPS[0];
          var h = '<div class="card"><div class="row"><h2 class="grow" style="margin:0">Mode</h2><button class="btn alt small" data-a="themeRandom">' + ic('shuffle', 's') + 'Random theme</button></div><div class="chips" style="margin-top:8px">' +
            chip('Follow system', act === 'system', 'themeUse', ' data-id="system"') + chip('Light', act === 'light', 'themeUse', ' data-id="light"') + chip('Dark', act === 'dark', 'themeUse', ' data-id="dark"') + '</div>' +
            '<div class="faint" style="margin-top:6px">Now using <b>' + esc(cur.name) + '</b>' + (act === 'system' ? ' (follows your device: ' + (Theme.systemDark() ? 'dark' : 'light') + ')' : '') + '.</div></div>';
          h += '<div class="card"><h2>Text size</h2><div class="chips">' + P.FONT_IDS.map(function (id, i) { return chip(P.FONT_NAMES[i], st.font === id, 'themeFont', ' data-v="' + id + '"'); }).join('') + '</div>' +
            '<h2 style="margin-top:14px">Corners</h2><div class="chips">' + P.CORNER_IDS.map(function (id, i) { return chip(P.CORNER_NAMES[i], st.corners === id, 'themeCorners', ' data-v="' + id + '"'); }).join('') + '</div>' +
            '<h2 style="margin-top:14px">Accent colour</h2><div class="faint" style="margin-bottom:8px">Overrides the main colour of any theme. Text stays readable: colours are adjusted to keep AA contrast.</div>' +
            '<div class="accents" role="group" aria-label="Accent colour"><button class="acc def' + (st.accentPick < 0 ? ' on' : '') + '" data-a="themeAccent" data-v="-1" aria-pressed="' + (st.accentPick < 0) + '" aria-label="Theme default">' + (st.accentPick < 0 ? ic('check', 'xs') : 'A') + '</button>' +
            P.QUICK_ACCENTS.map(function (c) { var on = st.accentPick === c; return '<button class="acc' + (on ? ' on' : '') + '" style="background:' + P.hex(c) + '" data-a="themeAccent" data-v="' + c + '" aria-pressed="' + on + '" aria-label="Accent ' + P.hex(c) + '">' + (on ? ic('check', 'xs') : '') + '</button>'; }).join('') + '</div></div>';
          h += '<div class="card"><div class="row"><h2 class="grow" style="margin:0">Presets <span class="faint">(' + P.PRESETS.length + ')</span></h2><button class="link" data-a="themeCopy" data-id="' + esc(cur.custom ? '' : cur.id) + '">' + ic('add', 'xs') + ' Customise current</button></div>' +
            '<div class="faint" style="margin:4px 0 8px">Every preset keeps body text at WCAG AA contrast (4.5:1) or better.</div>' +
            // v2.0: one group's tiles at a time (130 presets would be ~1300 DOM nodes on a Fire HD 8)
            '<div class="chips" role="tablist" aria-label="Preset groups">' + P.GROUPS.map(function (g) { return chip(esc(g) + ' <span class="faint">' + P.inGroup(g).length + '</span>', g === tg, 'themeGroup', ' data-v="' + esc(g) + '"'); }).join('') + '</div>' +
            '<div class="tgroup"><div class="tgrid">' + P.inGroup(tg).map(function (b) { return themeTile(b, act === b.id); }).join('') + '</div></div></div>';
          h += '<div class="card"><h2>Your themes <span class="faint">(' + st.custom.length + ' of ' + P.MAX_CUSTOM + ')</span></h2>' +
            (st.custom.length ? st.custom.map(function (b) { return themeRow(b, act === b.id, true); }).join('') : '<div class="muted">Make your own: pick background, cards, main colour, accent and text.</div>') +
            '<div class="row wrapbtns" style="margin-top:10px"><button class="btn" data-a="themeNew"' + (st.custom.length >= P.MAX_CUSTOM ? ' disabled' : '') + '>' + ic('add', 's') + 'Create custom theme</button>' +
            '<button class="btn ghost" data-a="themeReset">Reset to default</button></div></div>';
          return h;
        }
      }
    },
    themeEdit: {
      full: function (s) {
        var d = s.draft, P = PR.Palette;
        var roles = [['bg', 'Background'], ['surface', 'Cards (surface)'], ['primary', 'Main colour (primary)'], ['accent', 'Accent'], ['text', 'Text']];
        return topBar(s.editId ? 'Edit theme' : 'New theme', 'Live preview') + '<div class="main"><div class="wrap">' +
          '<div data-region="preview">' + SCREENS.themeEdit.regions.preview(s) + '</div>' +
          '<div class="card"><label class="flabel">Name<input class="tin" type="text" data-tename="1" maxlength="40" value="' + esc(d.name) + '" placeholder="My theme"></label>' +
          roles.map(function (r) {
            var hx = P.hex(d[r[0]]);
            return '<div class="crow"><span class="grow"><b>' + r[1] + '</b></span><input type="color" data-te="' + r[0] + '" value="' + hx.toLowerCase() + '" aria-label="' + r[1] + ' colour">' +
              '<input class="hex" type="text" data-teh="' + r[0] + '" value="' + hx + '" maxlength="7" spellcheck="false" autocapitalize="characters" autocomplete="off" aria-label="' + r[1] + ' hex code"></div>';
          }).join('') +
          '<div class="row wrapbtns" style="margin-top:8px"><span class="faint">Start from:</span><select class="tin small" data-change="teBase" aria-label="Start from a preset"><option value="">Choose a preset\u2026</option>' +
          P.PRESETS.map(function (b) { return '<option value="' + b.id + '">' + esc(b.name) + '</option>'; }).join('') + '</select></div></div>' +
          '<div data-region="checks">' + SCREENS.themeEdit.regions.checks(s) + '</div>' +
          '<div class="row wrapbtns" style="margin:4px 0 20px"><button class="btn" data-a="teSave">' + ic('check', 's') + 'Save &amp; use</button>' +
          (s.editId ? '<button class="btn alt" data-a="teSaveNew">Save as new</button>' : '') + '<button class="btn ghost" data-a="back">Cancel</button></div>' +
          '</div></div>';
      },
      regions: {
        preview: function (s) {
          var d = s.draft;
          return '<div class="tprev" style="' + Theme.styleText(d) + '"><div class="pv-top"><span class="logo">' + ic('leaf', 's') + '</span><b class="grow">My Pantry Recipes</b>' + ic('info', 's') + '</div>' +
            '<div class="pv-body"><div class="hero"><div class="over">WHAT\u2019S FOR DINNER?</div><h2>Tomato pasta</h2><div class="hmeals">Dinner \u00b7 Main dish</div><div class="acts"><span class="btn white small">Open recipe</span></div></div>' +
            '<div class="card"><b>Card title</b><div class="muted">Secondary text on a card.</div><div class="faint">A hint in smaller text</div>' +
            '<div class="chips" style="margin-top:8px"><span class="chip on">Selected</span><span class="chip have">' + ic('check', 'xs') + 'Eggs</span><span class="chip staple">Salt</span><span class="chip">Chip</span></div>' +
            '<div class="row" style="margin-top:8px"><span class="btn small">Button</span><span class="link">Link</span><span class="miss">Missing: basil</span></div></div>' +
            '<div class="pv-tabs"><span class="on">' + ic('kitchen', 's') + 'Pantry</span><span>' + ic('restaurant', 's') + 'Recipes</span><span>' + ic('cart', 's') + 'Shopping</span></div></div></div>';
        },
        checks: function (s) {
          var P = PR.Palette, c = P.derive(s.draft), w = P.warnings(s.draft);
          return '<div class="card"><h2>Contrast check</h2>' + (w.length ? w.map(function (x) { return '<div class="tw">' + ic('warning', 'xs') + esc(x) + '</div>'; }).join('') : '<div class="tok">' + ic('check', 'xs') + 'Looks good: all text meets its contrast target.</div>') +
            '<div class="pairs">' + P.textPairs(c).map(function (pr) {
              var r = P.contrast(pr[1], pr[2]), ok = r >= pr[3] - 1e-9;
              return '<div class="pair"><span class="sw" style="background:' + P.hex(pr[2]) + ';color:' + P.hex(pr[1]) + '">Aa</span><span class="grow">' + esc(pr[0]) + '</span><span class="' + (ok ? 'tok' : 'tw') + '">' + P.ratio(r) + ':1 ' + (ok ? (pr[3] >= 4.5 ? 'AA' : 'AA large') : 'low') + '</span></div>';
            }).join('') + '</div><div class="faint">Text and link colours are nudged automatically so body text always reaches 4.5:1.</div></div>';
        }
      }
    },
    backup: {
      full: function (s) {
        return topBar('Backup & restore', '') + '<div class="main"><div class="wrap"><div class="card"><h2>Export</h2>' +
          '<p class="muted">Saves your pantry (with use-by dates and quantities), favorites, shopping list, meal plan, ratings and notes, dislikes and allergies, prices, leftovers and cooking log, saved barcodes, settings, themes and imported recipes to one JSON file. The same file can be imported in the Android app.</p>' +
          '<label class="mcheck"><input type="checkbox" id="bkPhotos"' + (s.photos ? ' checked' : '') + ' data-change="bkPhotos"> Include photos (' + Photos.all().size + ')</label>' +
          '<div class="faint" style="margin-bottom:10px">Your recipe photos are <b>not</b> in the backup unless you tick this; they can make the file several MB.</div>' +
          '<button class="btn block" data-a="exportBackup">' + ic('cloud', 's') + 'Export backup file</button></div>' +
          '<div class="card"><h2>Import</h2><p class="muted">Replaces everything in this browser with the contents of a backup file from My Pantry Recipes on the web or Android.</p>' +
          '<input type="file" id="bkFile" accept=".json,application/json,text/plain" data-change="importFile" hidden><button class="btn alt block" data-a="importBackup">Choose backup file\u2026</button>' +
          (s.msg ? '<div class="note warnc" style="margin-top:10px">' + esc(s.msg) + '</div>' : '') + '</div></div></div>';
      }
    },
    importRecipe: {
      full: function (s) {
        return topBar('Import a recipe', 'Paste text or schema.org JSON-LD') + '<div class="main"><div class="wrap"><div class="card">' +
          '<p class="muted">Paste a recipe: either plain text (name on the first line, then an \u201cIngredients\u201d list and a \u201cMethod\u201d), or a recipe page\u2019s schema.org JSON-LD (the <code>application/ld+json</code> script, or the page source). Nothing is downloaded; photos are not imported.</p>' +
          '<textarea class="note big" id="impText" rows="10" placeholder="Pancakes&#10;Serves 4&#10;&#10;Ingredients&#10;200 g flour&#10;2 eggs&#10;300 ml milk&#10;&#10;Method&#10;Whisk everything together.&#10;Fry in a hot pan." aria-label="Recipe text">' + esc(s.text || '') + '</textarea>' +
          '<div class="row wrapbtns" style="margin-top:8px"><button class="btn" data-a="impParse">Read recipe</button></div></div>' + sregion('result', s) + '</div></div>';
      },
      regions: {
        result: function (s) {
          if (s.err) return '<div class="banner warn">' + ic('warning') + '<div class="grow">' + esc(s.err) + '</div></div>';
          if (!s.parsed) return '';
          var m = s.parsed.recipe;
          return '<div class="card"><h2>' + esc(m.title) + '</h2><div class="faint">' + (s.parsed.format === 'json-ld' ? 'Read from schema.org JSON-LD' : 'Read from text') + ' \u00b7 ' +
            plural(m.ingredients.length, 'ingredient', 'ingredients') + ' \u00b7 ' + plural(m.steps.length, 'step', 'steps') + (m.servingsKnown ? ' \u00b7 serves ' + m.servings : '') + '</div>' +
            '<ul class="ilist">' + m.ingredients.map(function (i) { return '<li>' + (i.qty != null ? '<b>' + esc(PR.formatQty(i.qty, i.unit || '', 1)) + '</b> ' : '') + esc(i.name) + (i.optional ? ' <span class="faint">(optional)</span>' : '') + '</li>'; }).join('') + '</ul>' +
            '<div class="faint">Check it looks right; you can delete it later from the recipe page.</div><button class="btn block" style="margin-top:10px" data-a="impSave">' + ic('check', 's') + 'Save recipe</button></div>';
        }
      }
    },
    cook: {
      full: function (s) {
        var r = byId(s.id);
        if (!r) return topBar('Cook mode', '');
        var steps = r.steps.length ? r.steps : ['No steps listed for this recipe.'], n = steps.length;
        if (s.step >= n) s.step = n - 1;
        var scale = s.servings / Math.max(1, r.servings);
        var ings = s.ing ? '<div class="card cing"><h3>Ingredients \u00b7 serves ' + s.servings + '</h3>' + r.ingredients.map(function (i) {
          var q = qtyText(i.key, i.amount, i.unit, scale, true);
          return '<div>' + (q ? '<b>' + esc(q) + '</b>  ' : '') + esc(i.name) + (i.optional ? ' (optional)' : '') + '</div>';
        }).join('') + '</div>' : '';
        return '<header class="top"><button class="iconbtn" data-a="back" aria-label="Close cook mode">' + ic('close') + '</button><div class="ttl"><div class="over" data-region="wl">' + SCREENS.cook.regions.wl(s) + '</div><h1>' + esc(r.title) + '</h1></div>' +
          chip('Ingredients', s.ing, 'cookIng') + '</header>' +
          '<div class="prog"><i style="width:' + Math.round(100 * (s.step + 1) / n) + '%"></i></div>' +
          '<div class="main"><div class="wrap">' + ings + '<div class="stepno">Step ' + (s.step + 1) + ' of ' + n + '</div><div class="steptext">' + esc(stepText(steps[s.step], true)) + '</div>' +
          '<div data-region="timer">' + SCREENS.cook.regions.timer(s) + '</div><div class="faint wlnote" data-region="wlnote">' + SCREENS.cook.regions.wlnote(s) + '</div></div></div>' +
          '<div class="timers" data-region="timers" aria-live="polite">' + SCREENS.cook.regions.timers(s) + '</div>' +
          '<div class="bottom"><button class="btn ghost" data-a="cookPrev"' + (s.step === 0 ? ' disabled' : '') + '>' + ic('prev', 's') + 'Back</button>' +
          '<button class="btn grow" data-a="cookNext">' + (s.step >= n - 1 ? 'Done' : 'Next step') + (s.step >= n - 1 ? ic('check', 's') : ic('next', 's')) + '</button></div>';
      },
      regions: {
        wl: function (s) { return s.wl === 'on' ? 'COOK MODE \u00b7 SCREEN STAYS ON' : 'COOK MODE'; },
        wlnote: function (s) {
          if (s.wl === 'on') return '';
          if (s.wl === 'unsupported') return 'This browser can\'t keep the screen on (Screen Wake Lock isn\'t supported here; Safari added it in iOS 16.4, and for Home Screen apps in iOS 18.4). Turn up Auto-Lock in Settings \u2192 Display & Brightness while cooking.';
          if (s.wl === 'denied') return 'The browser refused to keep the screen on (for example in Low Power Mode). Tap the screen now and then, or raise Auto-Lock in Settings.';
          return '';
        },
        timer: function (s) {
          var r = byId(s.id), steps = r && r.steps.length ? r.steps : [''], ms = PR.Durations.find(steps[s.step] || '');
          if (!ms.length) return '';
          var notes = [];
          ms.forEach(function (m) { var n = PR.Durations.timerNote(m); if (n && notes.indexOf(n) < 0) notes.push(n); });
          return '<div class="tchips">' + ms.map(function (m, k) {
            var kind = m.isPressure() ? ' at pressure' : m.isRelease() ? ' natural release' : '';
            return '<button class="btn ghost timer' + (kind ? ' pressure' : '') + '" data-a="timerStart" data-k="' + k + '"' + (kind ? ' aria-label="Start ' + esc(PR.Durations.format(m.seconds)) + kind + '. ' + esc(PR.Durations.timerNote(m)) + '"' : '') + '>' + ic('timer', 's') + 'Start ' + esc(PR.Durations.format(m.seconds)) + esc(kind) + (m.isRange() ? ' <span class="faint">(' + esc(m.label()) + ')</span>' : '') + '</button>';
          }).join('') + '</div>' + (ms.some(function (m) { return m.isRange(); }) ? '<div class="faint">Ranges start with the shorter time; check and add more if needed.</div>' : '') +
            notes.map(function (n) { return '<div class="faint pnote">' + ic('warning', 'xs') + ' ' + esc(n) + '</div>'; }).join('');
        },
        timers: function (s) {
          if (!s.timers.length) return '';
          var now = Date.now();
          return s.timers.map(function (t) {
            var left = timerLeft(t, now), lab = esc(t.label) + ' <span class="faint">step ' + (t.step + 1) + '</span>';
            if (t.done) return '<div class="trun up"><span class="grow"><b>Time\u2019s up!</b> ' + lab + '</span><button class="btn small" data-a="timerCancel" data-id="' + t.id + '">Dismiss</button></div>';
            return '<div class="trun' + (t.end ? '' : ' paused') + '"><span class="clock">' + PR.Durations.clock(left) + '</span><span class="grow">' + lab + (t.end ? '' : ' \u00b7 paused') + '</span>' +
              '<button class="iconbtn" data-a="timerPause" data-id="' + t.id + '" aria-label="' + (t.end ? 'Pause' : 'Resume') + ' timer">' + ic(t.end ? 'pause' : 'play', 's') + '</button>' +
              '<button class="iconbtn" data-a="timerCancel" data-id="' + t.id + '" aria-label="Cancel timer">' + ic('close', 's') + '</button></div>';
          }).join('');
        }
      }
    }
  };

  /** One swatch tile in the grouped preset grid; the current theme is marked with a check and aria-pressed. */
  function themeTile(b, on) {
    var P = PR.Palette, c = P.derive(b);
    return '<button class="ttile' + (on ? ' on' : '') + '" data-a="themeUse" data-id="' + esc(b.id) + '" aria-pressed="' + on + '" title="' + esc(b.name) + '" style="background:' + P.hex(c.bg) + ';color:' + P.hex(c.text) + '">' +
      '<span class="tcard" style="background:' + P.hex(c.surface) + '"><i style="background:' + P.hex(c.fill) + '"></i><i style="background:' + P.hex(b.accent) + '"></i></span>' +
      '<span class="tname">' + esc(b.name) + '</span>' + (on ? '<span class="tchk" style="background:' + P.hex(c.fill) + ';color:' + P.hex(c.onFill) + '">' + ic('check', 'xs') + '</span>' : '') + '</button>';
  }
  function themeRow(b, on, custom) {
    var c = PR.Palette.derive(b), r = PR.Palette.contrast(c.text, c.surface);
    var dots = [b.bg, b.surface, b.primary, b.accent, b.text].map(function (x) { return '<i style="background:' + PR.Palette.hex(x) + '"></i>'; }).join('');
    return '<div class="trow' + (on ? ' on' : '') + '"><button class="grow tpick" data-a="themeUse" data-id="' + esc(b.id) + '" aria-pressed="' + on + '"><span class="tsw" style="background:' + PR.Palette.hex(b.bg) + '">' + dots + '</span>' +
      '<span class="grow"><b>' + esc(b.name) + '</b><span class="faint">' + (c.dark ? 'Dark' : 'Light') + ' \u00b7 text ' + PR.Palette.ratio(r) + ':1</span></span>' + (on ? ic('check', 's') : '') + '</button>' +
      (custom ? '<button class="iconbtn" data-a="themeEdit" data-id="' + esc(b.id) + '" aria-label="Edit ' + esc(b.name) + '">' + ic('list', 's') + '</button><button class="iconbtn" data-a="themeDel" data-id="' + esc(b.id) + '" aria-label="Delete ' + esc(b.name) + '">' + ic('delete', 's') + '</button>'
        : '<button class="iconbtn" data-a="themeCopy" data-id="' + esc(b.id) + '" aria-label="Customise a copy of ' + esc(b.name) + '">' + ic('add', 's') + '</button>') + '</div>';
  }
  function openThemeEditor(src, editId) {
    var d = PR.Palette.base(editId || '', editId ? src.name : (src.custom ? src.name : src.name + ' (mine)'), src.bg, src.surface, src.primary, src.accent, src.text);
    if (!editId && d.name.length > 40) d.name = d.name.substring(0, 40);
    pushScreen({ type: 'themeEdit', draft: d, editId: editId || '' });
  }
  function teRefresh(s, skip) {
    $$('[data-region]', s.el).forEach(function (el) { var f = SCREENS.themeEdit.regions[el.getAttribute('data-region')]; if (f) el.innerHTML = f(s); });
    var P = PR.Palette;
    ['bg', 'surface', 'primary', 'accent', 'text'].forEach(function (k) {
      var c = $('[data-te="' + k + '"]', s.el), t = $('[data-teh="' + k + '"]', s.el), hx = P.hex(s.draft[k]);
      if (c && c !== skip) c.value = hx.toLowerCase();
      if (t && t !== skip) t.value = hx;
    });
  }
  function saveTheme(s, asNew) {
    var st = Theme.state(), d = s.draft, name = PR.jtrim(($('[data-tename]', s.el) || {}).value || d.name) || 'My theme';
    var id = !asNew && s.editId ? s.editId : st.nextCustomId();
    var b = PR.Palette.base(id, name.substring(0, 40), d.bg, d.surface, d.primary, d.accent, d.text);
    if (!Theme.put(b)) { toast('You can keep up to ' + PR.Palette.MAX_CUSTOM + ' custom themes. Delete one first.'); return; }
    Theme.use(id);
    toast('Saved \u201c' + b.name + '\u201d');
    back();
  }
  function backupData(withPhotos) {
    var d = PR.Backup.newData();
    d.appVersion = VERSION; d.platform = 'web'; d.exported = new Date().toISOString();
    d.pantry = pantry.map(function (p) { return { label: p.label, key: p.key, staple: p.staple, useBy: p.useBy || '', qty: p.qty || '', loc: p.loc || '', frozen: p.frozen || '', low: !!p.low }; });
    d.favorites = favoriteIdsNewestFirst();
    d.shopping = shopping.map(function (x) { return { name: x.name, amount: x.amount == null ? NaN : x.amount, unit: x.unit || '', recipe: x.recipe || '', checked: !!x.checked }; });
    Object.keys(ratings).forEach(function (k) { var r = ratings[k]; if (r && (r.stars || r.note)) d.ratings.set(k, { stars: r.stars | 0, note: r.note || '' }); });
    d.plan = plan.map(function (e) { return { date: e.date, id: e.id, servings: e.servings }; });
    d.dislikes = dislikes.slice(); d.allergens = allergens.slice();
    d.settings.set('servings', defaultServings()); d.settings.set('meals', ui.meals.slice()); d.settings.set('incDesserts', ui.incDesserts); d.settings.set('incSides', ui.incSides);
    d.settings.set('diets', ui.diets.slice()); d.settings.set('maxTime', ui.maxTime); d.settings.set('mode', ui.mode); d.settings.set('online', onlineEnabled()); d.settings.set('acceptedSubs', Array.from(accepted));
    d.settings.set('currency', currency()); d.settings.set('useDefaultPrices', useDefaultPrices()); d.settings.set('deductPantry', pref('deductPantry', false));
    d.settings.set('leftoverDays', leftoverDays()); d.settings.set('shopCost', pref('shopCost', false));
    d.settings.set('units', unitMode()); d.settings.set('weightUnit', pref('weightUnit', 'g') === 'oz' ? 'oz' : 'g'); d.settings.set('showCups', pref('showCups', true) !== false); d.settings.set('methods', methodFilter());
    // v2.0 settings (format 2); secrets such as the API key are never written
    if (RP.cleanZip(pref('zip', ''))) d.settings.set('zip', RP.cleanZip(pref('zip', '')));
    d.settings.set('budget', budget()); d.settings.set('budgetWeek', +pref('budgetWeek', 0) || 0); d.settings.set('expiryReminders', pref('expiryReminders', true) !== false);
    if (regionId()) d.settings.set('region', regionId());
    d.settings.set('gasMarks', !!pref('gasMarks', false)); d.settings.set('fanNote', !!pref('fanNote', false)); d.settings.set('dateFormat', pref('dateFormat', 'YYYY-MM-DD'));
    d.settings.set('decimalComma', !!pref('decimalComma', false)); d.settings.set('clock24', !!pref('clock24', false)); d.settings.set('weekStart', pref('weekStart', 'mon') === 'sun' ? 'sun' : 'mon');
    d.settings.set('household', PR.Household.toList(household()));
    // v2.0 Stage 3 (same keys as Android)
    if (Array.isArray(pref('holidays', null))) d.settings.set('holidays', pref('holidays', []).slice());
    if (Array.isArray(pref('fasts', null))) d.settings.set('fasts', pref('fasts', []).slice());
    if (pref('holLead', null) !== null) d.settings.set('holLead', +pref('holLead', 14) || 14);
    if (pref('holBanner', null) !== null) d.settings.set('holBanner', pref('holBanner', true) !== false);
    if (pref('fastChips', null) !== null) d.settings.set('fastChips', !!pref('fastChips', false));
    if (catMoves().size) d.settings.set('catMoves', catMoves()); // v2.1 (same key as Android)
    // v2.0.1 medical diet guides (same keys as Android)
    if (Array.isArray(pref('medGuides', null))) d.settings.set('medGuides', medGuides());
    if (pref('medSodiumMg', null) !== null) d.settings.set('medSodiumMg', PR.MedGuides.clampSodium(+pref('medSodiumMg', 0)));
    if (pref('medCarbsG', null) !== null) d.settings.set('medCarbsG', PR.MedGuides.clampCarbs(+pref('medCarbsG', 0)));
    if (pref('holNotify', null) !== null) d.settings.set('holNotify', !!pref('holNotify', false)); // Android-only; kept so a round trip does not lose it
    var stz = Store.get('starter', null); if (stz && Array.isArray(stz.feeds)) d.settings.set('starter', stz.feeds.slice());
    var blz = Store.get('bakeLog', null); if (Array.isArray(blz)) d.settings.set('bakeLog', blz.slice());
    prices.forEach(function (v, k) { d.prices.set(k, v); });
    d.leftovers = leftovers.map(function (e) { return { id: e.id, recipe: e.recipe, title: e.title, portions: e.portions, cooked: e.cooked, useBy: e.useBy }; });
    d.cooked = cooked.map(function (c) { return { date: c.date, recipe: c.recipe, servings: c.servings }; });
    barcodes.forEach(function (v, k) { d.barcodes.set(k, v); });
    if (withPhotos) Photos.all().forEach(function (v, k) { if (d.photos.size < PR.Backup.MAX_PHOTOS && v.length <= PR.Backup.MAX_PHOTO_CHARS) d.photos.set(k, v); });
    d.theme = Theme.state();
    // imported recipes, plus saved TheMealDB recipes that are favorites or planned, so they open offline after a restore
    var want = new Set(d.favorites.concat(d.plan.map(function (e) { return e.id; })));
    userRecipes.forEach(function (r) { d.recipes.push(r); });
    want.forEach(function (id) { if (id.indexOf('m:') === 0 && online.has(id)) d.recipes.push(online.get(id)); });
    return d;
  }
  // ---- v1.7 conversions screen ----
  function unitsChanged() {
    var s = topScreen();
    if (s) renderScreen(s, true); else refresh();
  }
  function convVal(id) { var e = $('#' + id); return e ? e.value : ''; }
  function convSet(id, text) { var e = $('#' + id); if (e) e.textContent = text || '\u2014'; }
  function convUpdate() {
    var U = PR.Units;
    if (!$('#cvOut')) return;
    var u = convVal('cvU');
    convSet('cvOut', U.range(convVal('cvA'), function (x) { return U.volumeLine(x, u) || U.massLine(x, u); }));
    var dn = PR.normalize(PR.jtrim(convVal('cvI'))), d = densities().forKey(dn), iu = convVal('cvIU');
    convSet('cvIOut', !d ? (PR.jtrim(convVal('cvI')) ? 'No density for that ingredient yet. Pick one from the list.' : '') : U.range(convVal('cvIA'), function (x) { return U.ingredientLine(x, iu, d); }) + (d ? '\n(' + d.name + ': ' + d.gPerCup + ' g per cup)' : ''));
    var bu = convVal('cvBU');
    convSet('cvBOut', U.range(convVal('cvB'), function (x) { var g = U.butterGrams(x, bu); return isNaN(g) || g <= 0 ? '' : U.butterText(g); }));
    var tu = convVal('cvTU');
    convSet('cvTOut', U.tempLine(convVal('cvT'), tu));
  }
  function backupText(withPhotos) { return PR.Backup.write(backupData(withPhotos)); }
  function applyBackup(d) {
    var now = Date.now();
    pantry = d.pantry.map(function (p, n) { return { key: p.key, label: p.label, staple: !!p.staple, added: now - 10000 + n, useBy: p.useBy || '', qty: p.qty || '', loc: p.loc || '', frozen: p.frozen || '', low: !!p.low }; });
    savePantry();
    favorites = d.favorites.map(function (id, n) { return { id: id, at: now - n }; }); Store.set('favorites', favorites);
    shopping = d.shopping.map(function (x, n) { return { id: now + '-' + n, key: PR.normalize(x.name), name: x.name, amount: isNaN(x.amount) ? null : x.amount, unit: x.unit || '', recipe: x.recipe || '', checked: !!x.checked, added: now + n }; });
    saveShopping();
    ratings = {}; d.ratings.forEach(function (v, k) { ratings[k] = { stars: v.stars, note: v.note || '' }; }); Store.set('ratings', ratings);
    plan = d.plan.map(function (e) { return PR.MealPlan.entry(e.date, e.id, e.servings); }); savePlan();
    dislikes = d.dislikes.slice(); allergens = d.allergens.slice(); saveAvoid();
    var st = d.settings;
    if (st.has('servings')) setPref('servings', st.get('servings'));
    if (st.has('meals')) ui.meals = PR.mealSorted(st.get('meals').filter(PR.isMealType));
    if (st.has('incDesserts')) ui.incDesserts = st.get('incDesserts');
    if (st.has('incSides')) ui.incSides = st.get('incSides');
    saveMeals();
    if (st.has('diets')) { ui.diets = st.get('diets').slice(); setPref('diets', ui.diets); }
    if (st.has('maxTime')) { ui.maxTime = st.get('maxTime'); setPref('maxTime', ui.maxTime); }
    if (st.has('mode')) { ui.mode = st.get('mode'); setPref('mode', ui.mode); }
    if (st.has('online')) setPref('online', st.get('online'));
    if (st.has('acceptedSubs')) { accepted = new Set(st.get('acceptedSubs')); Store.set('acceptedSubs', Array.from(accepted)); }
    userRecipes = new Map();
    d.recipes.forEach(function (r) { if (r.id.indexOf('u:') === 0) userRecipes.set(r.id, r); else if (r.id.indexOf('m:') === 0) cacheRecipe(r); });
    saveUserRecipes();
    if (st.has('currency')) setPref('currency', st.get('currency'));
    if (st.has('useDefaultPrices')) setPref('useDefaultPrices', st.get('useDefaultPrices'));
    if (st.has('deductPantry')) setPref('deductPantry', st.get('deductPantry'));
    if (st.has('leftoverDays')) setPref('leftoverDays', st.get('leftoverDays'));
    if (st.has('shopCost')) setPref('shopCost', st.get('shopCost'));
    // v1.7 settings; a v1.6 backup has none, so the current choices stay
    if (st.has('units')) setPref('units', st.get('units'));
    if (st.has('weightUnit')) setPref('weightUnit', st.get('weightUnit'));
    if (st.has('showCups')) setPref('showCups', st.get('showCups'));
    if (st.has('methods')) setPref('methods', st.get('methods').slice());
    // v2.0 settings; older backups lack them, so the current choices stay
    ['zip', 'budget', 'budgetWeek', 'expiryReminders', 'region', 'gasMarks', 'fanNote', 'dateFormat', 'decimalComma', 'clock24', 'weekStart'].forEach(function (k) { if (st.has(k)) setPref(k, st.get(k)); });
    if (st.has('region')) setPref('welcomed', true);
    if (st.has('household')) Store.set('household', PR.Household.toList(PR.Household.parse(st.get('household'))));
    ['holidays', 'fasts', 'holLead', 'holBanner', 'fastChips', 'holNotify', 'medGuides', 'medSodiumMg', 'medCarbsG'].forEach(function (k) { if (st.has(k)) setPref(k, st.get(k)); });
    if (st.has('catMoves')) setPref('catMoves', PR.Botany.toObject(PR.Botany.sanitize(st.get('catMoves'))));
    pruneFastDiets();
    var plain = function (l) { return l.map(function (m) { var o = {}; m.forEach(function (v, k) { o[k] = v; }); return o; }); };
    if (st.has('starter')) Store.set('starter', { feeds: plain(st.get('starter')) });
    if (st.has('bakeLog')) Store.set('bakeLog', plain(st.get('bakeLog')));
    PRICE_MEMO = null; BUDGET_VER++; DIET_MEMO = new Map();
    setDetailUnits(null);
    // v1.6 data; a v1.5 backup has none of it, so those lists come back empty
    prices = new Map(d.prices); savePrices();
    leftovers = d.leftovers.map(function (e) { return { id: e.id, recipe: e.recipe, title: e.title, portions: e.portions, cooked: e.cooked, useBy: e.useBy }; }); saveLeftovers();
    cooked = d.cooked.map(function (c) { return { date: c.date, recipe: c.recipe, servings: c.servings }; }); saveCooked();
    barcodes = new Map(d.barcodes); saveBarcodes();
    // photos are only replaced when the backup carries some, so a photo-less backup doesn't wipe this browser's photos
    if (d.photos.size) d.photos.forEach(function (v, k) { Photos.put(k, v); });
    if (d.theme) Theme.setState(d.theme);
    net.lastSig = ''; version++;
  }
  function importBackupText(text) {
    var d = PR.Backup.parse(text);
    applyBackup(d);
    return PR.Backup.summary(d);
  }
  function download(name, text, type) {
    var blob = new Blob([text], { type: type }), url = URL.createObjectURL(blob), a = document.createElement('a');
    a.href = url; a.download = name; a.rel = 'noopener'; a.style.display = 'none';
    document.body.appendChild(a); a.click();
    setTimeout(function () { URL.revokeObjectURL(url); a.remove(); }, 4000);
  }

  // =====================================================================
  // Cook mode: Screen Wake Lock + step timer
  // =====================================================================
  var wakeLock = null;
  function cookScreen() { for (var i = ui.stack.length - 1; i >= 0; i--) if (ui.stack[i].type === 'cook') return ui.stack[i]; return null; }
  function requestWakeLock(s) {
    if (!('wakeLock' in navigator) || !navigator.wakeLock || !navigator.wakeLock.request) { s.wl = 'unsupported'; return; }
    navigator.wakeLock.request('screen').then(function (lock) {
      wakeLock = lock; s.wl = 'on';
      lock.addEventListener('release', function () { if (wakeLock === lock) { wakeLock = null; var c = cookScreen(); if (c && document.visibilityState === 'visible') c.wl = 'off'; } });
      if (cookScreen() === s) renderScreen(s, true);
    }, function () { s.wl = 'denied'; if (cookScreen() === s) renderScreen(s, true); });
  }
  function releaseWakeLock() { if (wakeLock) { try { wakeLock.release(); } catch (e) { } wakeLock = null; } }
  document.addEventListener('visibilitychange', function () {
    var c = cookScreen();
    if (c && document.visibilityState === 'visible' && !wakeLock && c.wl !== 'unsupported') requestWakeLock(c);
    if (c) renderScreen(c, true);
  });
  var audioCtx = null;
  function primeAudio() {
    try { if (!audioCtx) { var AC = window.AudioContext || window.webkitAudioContext; if (AC) audioCtx = new AC(); } if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume(); } catch (e) { audioCtx = null; }
  }
  function beep() {
    try {
      if (!audioCtx) return;
      for (var k = 0; k < 3; k++) {
        var o = audioCtx.createOscillator(), g = audioCtx.createGain(), t = audioCtx.currentTime + k * 0.45;
        o.frequency.value = 880; o.connect(g); g.connect(audioCtx.destination);
        g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(0.4, t + 0.02); g.gain.exponentialRampToValueAtTime(0.0001, t + 0.35);
        o.start(t); o.stop(t + 0.4);
      }
    } catch (e) { }
    try { if (navigator.vibrate) navigator.vibrate([300, 150, 300, 150, 300]); } catch (e) { }
  }
  // ---- multiple timers; each keeps an end timestamp so it survives re-renders, reloads and screen rotation ----
  var tickTimer = null, flashTimer = null, baseTitle = document.title;
  function timerLeft(t, now) { return t.end ? Math.max(0, t.end - now) : t.left; }
  function saveTimers(s) { Store.set('cookTimers', { id: s.id, timers: s.timers.map(function (t) { return { id: t.id, label: t.label, step: t.step, end: t.end, left: t.left, done: t.done }; }) }); }
  function ensureTick() { if (!tickTimer) tickTimer = setInterval(tick, 250); }
  function flashTitle(on, label) {
    clearInterval(flashTimer); flashTimer = null; document.title = baseTitle;
    if (!on) return;
    var k = 0;
    flashTimer = setInterval(function () { k++; document.title = k % 2 ? '\u23f0 Time\u2019s up: ' + label : baseTitle; if (k > 120) flashTitle(false); }, 700);
  }
  function tick() {
    var s = cookScreen();
    if (!s || !s.timers.some(function (t) { return t.end && !t.done; })) { clearInterval(tickTimer); tickTimer = null; return; }
    var now = Date.now(), fired = null;
    s.timers.forEach(function (t) { if (t.end && !t.done && now >= t.end) { t.done = true; t.end = 0; t.left = 0; fired = t; } });
    if (fired) { beep(); flashTitle(true, fired.label); saveTimers(s); }
    var reg = $('[data-region="timers"]', s.el);
    if (reg) reg.innerHTML = SCREENS.cook.regions.timers(s);
  }
  function startTimer(s, k) {
    primeAudio();
    var r = byId(s.id), m = PR.Durations.find((r.steps[s.step]) || '')[k];
    if (!m) return;
    if (s.timers.length >= 8) { toast('Up to 8 timers at once'); return; }
    s.timers.push({ id: 't' + Date.now().toString(36) + s.timers.length, label: (m.isRange() ? m.label() : PR.Durations.format(m.seconds)) + (m.isPressure() ? ' at pressure' : m.isRelease() ? ' release' : ''), step: s.step, end: Date.now() + m.seconds * 1000, left: m.seconds * 1000, done: false });
    saveTimers(s); ensureTick(); renderScreen(s, true);
  }
  function findTimer(s, id) { for (var i = 0; i < s.timers.length; i++) if (s.timers[i].id === id) return s.timers[i]; return null; }
  function pauseTimer(s, id) {
    var t = findTimer(s, id); if (!t || t.done) return;
    if (t.end) { t.left = Math.max(0, t.end - Date.now()); t.end = 0; } else { t.end = Date.now() + t.left; ensureTick(); }
    saveTimers(s); renderScreen(s, true);
  }
  function cancelTimer(s, id) {
    s.timers = s.timers.filter(function (t) { return t.id !== id; });
    if (!s.timers.some(function (t) { return t.done; })) flashTitle(false);
    saveTimers(s); renderScreen(s, true);
  }
  function openCook(d) {
    var s = { type: 'cook', id: d.id, servings: d.servings, step: 0, ing: false, wl: 'pending', timers: [] };
    var saved = Store.get('cookTimers', null);
    if (saved && saved.id === d.id && Array.isArray(saved.timers)) s.timers = saved.timers.filter(function (t) { return t && t.id && (t.end > Date.now() - 3600000 || (!t.end && t.left > 0) || t.done); });
    s.onClose = function () { releaseWakeLock(); s.timers = []; Store.del('cookTimers'); flashTitle(false); };
    pushScreen(s);
    requestWakeLock(s);
    if (s.timers.length) { ensureTick(); tick(); }
  }

  // =====================================================================
  // Events (delegated)
  // =====================================================================
  function topScreen() { return ui.stack[ui.stack.length - 1] || null; }
  function rerenderMine() { refresh(); }
  var actions = {
    tab: function (el) {
      var n = ui.stack.length;
      while (ui.stack.length) popScreen();
      if (n) { try { if (history.state && history.state.depth === n) history.go(-n); } catch (e) { } }
      showTab(el.getAttribute('data-tab'));
    },
    back: function () { back(); },
    blur: function () { if (document.activeElement) document.activeElement.blur(); },
    subs: function () { openSubs(''); },
    about: function () { pushScreen({ type: 'about' }); },
    dlg: function (el) {
      var f = dlgHandlers[el.getAttribute('data-n')], vals = {}, d = $('#dlg');
      if (d) $$('[data-dv]', d).forEach(function (x) { vals[x.getAttribute('data-dv')] = x.type === 'checkbox' ? x.checked : x.value; });
      closeDialog(); if (f) f(vals);
    },
    ioshint: function () { setPref('iosHintDone', true); refresh(); },
    addPantry: function () {
      var inp = $('#addIn'); if (!inp) return;
      if (addPantryText(inp.value)) { inp.value = ''; ui.addText = ''; renderAc(); refresh(); }
    },
    acPick: function (el) {
      var inp = $('#addIn'), parts = inp.value.split(',');
      parts.pop();
      parts.map(PR.jtrim).filter(Boolean).forEach(function (p) { addToPantry(p, false); });
      if (!addToPantry(el.getAttribute('data-label'), false)) toast(el.getAttribute('data-label') + ' is already in your pantry');
      else toast('Added ' + el.getAttribute('data-label'));
      inp.value = ''; ui.addText = ''; renderAc(); refresh();
      inp.focus();
    },
    toggleItem: function (el) {
      var label = el.getAttribute('data-label'), k = PR.normalize(label);
      var hadRecents = recents.length;
      if (pantryItem(k)) removeFromPantry(k); else addToPantry(label, false);
      if (ui.tab !== 'pantry' || ui.stack.length || !$('#main [data-region="grid"]')) { refresh(); return; }
      // v1.8: update only what changed instead of rebuilding the whole ingredient grid (the slowest thing on a Fire HD 8)
      var have = new Map();
      pantry.forEach(function (p) { have.set(p.key, p); });
      $$('#main .chip[data-a="toggleItem"]').forEach(function (c) {
        var l = c.getAttribute('data-label');
        if (PR.normalize(l) === k) c.outerHTML = itemChip(have, l);
      });
      var rc = $('#recentChips');
      if (!PR.jtrim(ui.quick)) {
        if (rc) rc.innerHTML = recents.map(function (l) { return itemChip(have, l); }).join('');
        else if (!hadRecents && recents.length) { refreshRegion('grid'); rc = true; }
      }
      if (rc !== true) $$('#main .cat > button[data-ci]').forEach(function (b) {
        var mv = catMoves(), ci = +b.getAttribute('data-ci'), items = mv.size ? PR.Botany.sectionItems(ci, mv) : C.ITEMS[ci], total = 0, n = 0, hit = false;
        items.forEach(function (s) { if (!C.isHeader(s)) { total++; var sk = PR.normalize(s); if (sk === k) hit = true; if (have.has(sk)) n++; } });
        if (!hit) return;
        var cnt = b.querySelector('.cnt');
        cnt.className = 'cnt' + (n ? ' some' : '');
        cnt.textContent = n > 0 ? n + ' of ' + total : String(total);
      });
      ['banners', 'soon', 'mine'].forEach(refreshRegion);
      renderTabs();
    },
    addTyped: function (el) { addToPantry(el.getAttribute('data-label'), false); toast('Added ' + cap(el.getAttribute('data-label'))); ui.quick = ''; var q = $('#quickIn'); if (q) q.value = ''; refresh(); },
    cat: function (el) {
      var ci = el.getAttribute('data-ci');
      if (openCats.has(ci)) openCats.delete(ci); else openCats.add(ci);
      Store.set('openCats', Array.from(openCats));
      refresh();
    },
    pmenu: function (el) {
      var p = pantryItem(el.getAttribute('data-key')); if (!p) return;
      var bot = PR.Botany.isBotanicalFruit(p.key), cat = catOf(p.key);
      sheet(p.label, (bot ? [{ label: '\ud83c\udf45 Botanically a fruit \u2013 why?', icon: 'leaf', fn: function () { botanyDialog(p.key); } }] : []).concat([
        { label: 'Category: ' + (cat || 'none') + ' \u2013 change\u2026', icon: 'list', fn: function () { catDialog(p.key, p.label); } },
        p.staple ? { label: 'Not a staple', icon: 'star', fn: function () { setStaple(p.key, false); refresh(); } } : { label: 'Make it a staple (always on hand)', icon: 'star', fn: function () { setStaple(p.key, true); refresh(); } },
        { label: p.useBy ? 'Use-by ' + D.label(p.useBy) + ' \u2013 change\u2026' : 'Set use-by date\u2026', icon: 'calendar', fn: function () { useByDialog(p); } },
        { label: p.qty ? 'Quantity: ' + p.qty + ' \u2013 change\u2026' : 'Set quantity (optional)\u2026', icon: 'kitchen', fn: function () { qtyDialog(p); } },
        { label: 'Recipes with ' + p.label.toLowerCase(), icon: 'restaurant', fn: function () { ui.query = p.label; showTab('recipes'); } },
        { label: 'Substitutions for ' + p.label.toLowerCase(), icon: 'swap', fn: function () { openSubs(p.label); } },
        { label: 'Remove from pantry', icon: 'delete', red: true, fn: function () { removeFromPantry(p.key); toast('Removed ' + p.label); refresh(); } }
      ]));
    },
    botFilter: function () { ui.quick = 'botanical fruits'; var qi = $('#quickIn'); if (qi) qi.value = ui.quick; refreshRegion('grid'); },
    clearPantry: function () {
      confirmBox('Clear pantry?', 'This removes everything except your staples.', 'Clear', function () { clearPantry(true); refresh(); });
    },
    mode: function (el) { ui.mode = +el.getAttribute('data-v'); setPref('mode', ui.mode); ui.listLimit = PAGE; refresh(); },
    diet: function (el) { var d = el.getAttribute('data-v'), i = ui.diets.indexOf(d); if (i >= 0) ui.diets.splice(i, 1); else ui.diets.push(d); setPref('diets', ui.diets); ui.listLimit = PAGE; refresh(); },
    time: function (el) { ui.maxTime = +el.getAttribute('data-v'); setPref('maxTime', ui.maxTime); ui.listLimit = PAGE; refresh(); },
    more: function () { ui.listLimit += PAGE; refresh(); },
    open: function (el) { openDetail(el.getAttribute('data-id')); },
    surprise: function () { surprise(filteredResults(), lastOpened); },
    mealAll: function () { ui.meals = []; saveMeals(); refresh(); },
    dMealAll: function () { ui.dMeals = []; saveMeals(); refresh(); },
    dMealType: function (el) {
      var v = el.getAttribute('data-v'), i = ui.dMeals.indexOf(v);
      if (i >= 0) ui.dMeals.splice(i, 1); else ui.dMeals.push(v);
      ui.dMeals = PR.mealSorted(ui.dMeals); saveMeals(); refresh();
    },
    mealType: function (el) {
      var v = el.getAttribute('data-v'), i = ui.meals.indexOf(v);
      if (i >= 0) ui.meals.splice(i, 1); else ui.meals.push(v);
      ui.meals = PR.mealSorted(ui.meals); saveMeals(); refresh();
    },
    surpriseP: function () {
      if (matcher().pantryEmpty()) { toast('Add some ingredients to your pantry first'); showTab('pantry'); return; }
      surprise(matchAll(), lastOpened);
    },
    random: function () {
      if (onlineEnabled() && isOnline()) {
        randomOnline().then(function (x) {
          if (x.r) { refresh(); openDetail(x.r.id); }
          else { var all = allRecipes(); openDetail(all[Math.floor(Math.random() * all.length)].id); }
        });
      } else { var all = allRecipes(); openDetail(all[Math.floor(Math.random() * all.length)].id); }
    },
    findMore: function () { net.lastSig = ''; fetchForPantry(); },
    searchOnline: function (el) { doOnlineSearch(el.getAttribute('data-q')); },
    dsearch: function () { var q = PR.jtrim(ui.dq); if (document.activeElement) document.activeElement.blur(); if (q && onlineEnabled()) doOnlineSearch(q); },
    seeAll: function (el) { var k = SEE_ALL[el.getAttribute('data-k')]; pushScreen({ type: 'list', title: k.t, items: k.f }); },
    browse: function (el) {
      var kind = el.getAttribute('data-kind'), v = el.getAttribute('data-v');
      // meal types load more from the matching TheMealDB category (Breakfast, Dessert, Starter, Side), if any
      var okind = kind === 'm' ? 'c' : kind, ov = kind === 'm' ? PR.mealDbCategory(v) : kind === 'k' ? '' : v;
      var s = { type: 'list', title: kind === 'k' ? PR.Methods.label(v) : v, sub: kind === 'a' ? 'Cuisine' : kind === 'm' ? 'Meal type' : kind === 'k' ? 'Cooking method' : 'Category', browse: true, kind: okind, value: ov, remaining: 0, busy: false, msg: '' };
      s.items = function () {
        var m = matcher(), seenIds = {}, out = [];
        allRecipes().forEach(function (r) {
          var hit = kind === 'a' ? PR.eqIC(PR.jtrim(r.cuisine), v) : kind === 'm' ? mealsOf(r).indexOf(v) >= 0 : kind === 'k' ? PR.Methods.has(r, v) : PR.inCategory(r, v);
          if (hit && !seenIds[r.id]) { seenIds[r.id] = 1; out.push(r); }
        });
        if (ov) cachedBrowse(okind, ov).forEach(function (r) { if ((kind !== 'm' || mealsOf(r).indexOf(v) >= 0) && !seenIds[r.id]) { seenIds[r.id] = 1; out.push(r); } });
        // cuisines follow the meal-type filters; a picked category or meal type is shown in full
        s.hidden = kind === 'a' ? out.filter(dMealHidden).length : 0;
        if (kind === 'a') out = out.filter(dMealPasses);
        return out.map(function (r) { return m.match(r); }).sort(PR.compare);
      };
      if (kind === 'k' && v === 'instant-pot') s.top = function () { return '<div class="faint note">Pressure timers in cook mode count from when the pot reaches pressure (it takes about 10\u201315 min to get there). <button class="link" data-a="ipSheet">Cheat sheet</button></div>'; };
      s.empty = !ov ? '' : onlineEnabled() ? '' : 'Turn on online recipes in About to load more from TheMealDB.';
      pushScreen(s);
      if (onlineEnabled() && ov) loadBrowse(s);
    },
    browseMore: function () { var s = topScreen(); if (s) loadBrowse(s); },
    listMore: function () { var s = topScreen(); s.limit = (s.limit || 120) + 120; renderScreen(s, true); },
    resetSeen: function () { confirmBox('Reset \u201cNew for you\u201d?', 'Every recipe will count as new again.', 'Reset', function () { seen = new Set(); Store.set('seen', []); refresh(); }); },
    shopAdd: function () {
      var inp = $('#shopIn'); if (!inp || !PR.jtrim(inp.value)) return;
      var p = parseShoppingInput(inp.value);
      if (!addShopping(p.name, p.amount, p.unit, '')) toast('Already on your list');
      inp.value = ''; refresh();
    },
    shopCheck: function (el) { shopping.forEach(function (s) { if (s.id === el.getAttribute('data-id')) s.checked = !s.checked; }); saveShopping(); refresh(); },
    shopDel: function (el) { shopping = shopping.filter(function (s) { return s.id !== el.getAttribute('data-id'); }); saveShopping(); refresh(); },
    shopToPantry: function () {
      var n = 0;
      shopping.filter(function (s) { return s.checked; }).forEach(function (s) { if (addToPantry(s.name, false)) n++; });
      shopping = shopping.filter(function (s) { return !s.checked; }); saveShopping();
      toast(n ? 'Added ' + n + ' item' + (n === 1 ? '' : 's') + ' to your pantry' : 'Already in your pantry');
      refresh();
    },
    shopClearChecked: function () { shopping = shopping.filter(function (s) { return !s.checked; }); saveShopping(); refresh(); },
    shopClearAll: function () { confirmBox('Clear shopping list?', 'Every item will be removed.', 'Clear', function () { shopping = []; saveShopping(); refresh(); }); },
    fav: function (el) { var on = toggleFavorite(el.getAttribute('data-id')); toast(on ? 'Saved to favorites' : 'Removed from favorites'); var s = topScreen(); if (s) renderScreen(s, false); refresh(); },
    cook: function () { var s = topScreen(); primeAudio(); openCook({ id: s.id, servings: s.servings }); },
    cookRow: function (el) { var id = el.getAttribute('data-id'); if (!byId(id)) return; primeAudio(); openCook({ id: id, servings: defaultServings() }); },
    addMissing: function () { addMissing(topScreen()); },
    serv: function (el) {
      var s = topScreen(), v = Math.max(1, Math.min(24, s.servings + (+el.getAttribute('data-d'))));
      s.servings = v; setPref('servings', v); renderScreen(s, true);
    },
    useSub: function (el) {
      var on = el.getAttribute('data-on') === 'true';
      setAccepted(el.getAttribute('data-key'), on);
      toast(on ? 'Counting the substitute as a match' : 'Back to missing');
      refresh();
    },
    subDlg: function (el) { subsDialog(el.getAttribute('data-key')); },
    methodF: function (el) {
      var id = el.getAttribute('data-v'), mf = methodFilter(), i = mf.indexOf(id);
      if (i >= 0) mf.splice(i, 1); else mf.push(id);
      setPref('methods', PR.Methods.canonical(mf)); ui.listLimit = PAGE; refresh();
    },
    methodClear: function () { setPref('methods', []); refresh(); },
    detUnits: function (el) { var s = topScreen(), v = el.getAttribute('data-v'); setDetailUnits(v === unitMode() ? null : v); renderScreen(s, true); },
    conv: function () { pushScreen({ type: 'conv' }); },
    ipSheet: function () { pushScreen({ type: 'ipsheet' }); },
    unitsMode: function (el) { setPref('units', PR.Units.validMode(el.getAttribute('data-v'))); setDetailUnits(null); unitsChanged(); },
    unitsWeight: function (el) { setPref('weightUnit', el.getAttribute('data-v') === 'oz' ? 'oz' : 'g'); unitsChanged(); },
    subCat: function (el) { var s = topScreen(); s.cat = el.getAttribute('data-v'); s.limit = 80; renderScreen(s, true); },
    subsMore: function () { var s = topScreen(); s.limit = (s.limit || 80) + 80; renderScreen(s, true); },
    cookIng: function () { var s = topScreen(); s.ing = !s.ing; renderScreen(s, false); },
    cookPrev: function () { var s = topScreen(); if (s.step > 0) { s.step--; s.resetScroll = true; renderScreen(s, false); } },
    cookNext: function () {
      var s = topScreen(), r = byId(s.id), n = Math.max(1, r.steps.length);
      if (s.step >= n - 1) { back(); return; }
      s.step++; s.resetScroll = true; renderScreen(s, false);
    },
    timerStart: function (el) { startTimer(topScreen(), +el.getAttribute('data-k')); },
    timerPause: function (el) { pauseTimer(cookScreen(), el.getAttribute('data-id')); },
    timerCancel: function (el) { cancelTimer(cookScreen(), el.getAttribute('data-id')); },
    // ---- v1.5 ----
    plan: function () { actions.tab({ getAttribute: function () { return 'plan'; } }); },
    planWeek: function (el) { var s = planState(), d = +el.getAttribute('data-d'); s.week = d === 0 ? wkStart(D.today()) : D.addDays(s.week, d); renderPlan(s, false); },
    planAdd: function () { var s = topScreen(); planDialog(s.id, null, s.servings); },
    scan: function () { openScan(); },
    scanCam: function () { startCam(topScreen()); },
    scanStop: function () { var s = topScreen(); stopScan(); s.cam = false; renderScreen(s, true); },
    scanLookup: function () { var s = topScreen(), i = $('#codeIn'); stopScan(); s.cam = false; lookupCode(s, i ? i.value : ''); },
    scanPick: function (el) { var n = $('#scanName'); if (n) { n.value = el.getAttribute('data-v'); n.focus(); } },
    scanAdd: function () {
      var s = topScreen(), n = $('#scanName'), q = $('#scanQty'), name = PR.jtrim(n ? n.value : '');
      if (!name) { toast('Type a name for the pantry item'); return; }
      var key = PR.normalize(name), added = addToPantry(name, false);
      if (q && q.checked && s.product && s.product.quantity) setQty(key, s.product.quantity);
      if (PR.Barcode.valid(s.code)) { barcodes.set(s.code, cap(name)); saveBarcodes(); }
      toast(added ? 'Added ' + cap(name) : cap(name) + ' is already in your pantry');
      s.state = ''; s.code = ''; s.product = null; s.alts = []; renderScreen(s, true); refresh();
    },
    buyNext: function (el) {
      var p = PR.BuyNext.fromResults(buyNextResults(), PR.BuyNext.MAX)[+el.getAttribute('data-k')];
      if (!p) return;
      pushScreen({ type: 'list', title: 'Buy ' + p.name, sub: 'Unlocks ' + plural(p.unlocks.length, 'recipe', 'recipes'), items: function () { var m = matcher(); return p.unlocks.map(byId).filter(Boolean).map(function (r) { return m.match(r); }); },
        top: function () { return '<button class="btn block" style="margin-bottom:12px" data-a="buyAdd" data-v="' + esc(p.name) + '">' + ic('cart', 's') + 'Add ' + esc(p.name) + ' to shopping list</button>'; } });
    },
    bnToggle: function (el) {
      var k = el.getAttribute('data-key'); if (bnOpen.has(k)) bnOpen.delete(k); else bnOpen.add(k);
      var card = el.closest('.card.buynext'); if (!card) { refresh(); return; }
      var w = document.createElement('div'); w.innerHTML = buyNextCard(buyNextResults(), false); if (w.firstChild) card.parentNode.replaceChild(w.firstChild, card);
      var b = document.querySelector('.card.buynext [data-a="bnToggle"][data-key="' + k.replace(/["\\]/g, '') + '"]'); if (b) b.focus();
    },
    bnAll: function () { pushScreen({ type: 'buynext', sort: pref('buyNextSort', 'recipes') === 'cheapest' ? 'cheapest' : 'recipes', shown: PR.BuyNext.PAGE, open: new Set(), ranked: null }); },
    bnSort: function (el) { var s = topScreen(); s.sort = el.getAttribute('data-v'); s.shown = PR.BuyNext.PAGE; setPref('buyNextSort', s.sort); renderScreen(s, true); },
    bnMore: function () { var s = topScreen(); s.shown += PR.BuyNext.PAGE; renderScreen(s, true); },
    bnToggleAll: function (el) { var s = topScreen(), k = el.getAttribute('data-key'); if (s.open.has(k)) s.open.delete(k); else s.open.add(k); renderScreen(s, true); var sc = document.querySelectorAll('.screen'), b = sc.length && sc[sc.length - 1].querySelector('[data-a="bnToggleAll"][data-key="' + k.replace(/["\\]/g, '') + '"]'); if (b) b.focus(); },
    bnRecipes: function (el) {
      var k = el.getAttribute('data-key'), src = el.getAttribute('data-src'), ids, name;
      if (src === 'all') { var o = PR.BuyNext.rankedFromResults(buyNextResults()).filter(function (x) { return x.key === k; })[0]; if (!o) return; ids = o.alone.length ? o.alone : o.closer; name = o.name; }
      else { var p = PR.BuyNext.fromResults(buyNextResults(), PR.BuyNext.MAX)[+src.split(':')[1]]; if (!p) return; ids = p.unlocks; name = p.name; }
      pushScreen({ type: 'list', title: 'Buy ' + name, sub: plural(ids.length, 'recipe', 'recipes'), items: function () { var m = matcher(); return ids.map(byId).filter(Boolean).map(function (r) { return m.match(r); }); } });
    },
    buyAdd: function (el) { addShopping(el.getAttribute('data-v'), null, '', ''); toast('Added to shopping list'); renderTabs(); },
    aisleToggle: function (el) { var v = el.getAttribute('data-v'), c = (pref('shopCollapsed', []) || []).slice(), i = c.indexOf(v); if (i >= 0) c.splice(i, 1); else c.push(v); setPref('shopCollapsed', c); refresh(); },
    shopShare: function () { shareText('Shopping list', PR.ShareText.shopping(shopRowsData(shopping.slice().sort(function (a, b) { return a.added - b.added; })), function (name, amount, unit) { return qtyText(PR.normalize(name), amount, unit, 1, false); })); },
    planShare: function () {
      var s = planState(), es = PR.MealPlan.forWeek(plan, s.week).slice().sort(function (a, b) { return a.date < b.date ? -1 : a.date > b.date ? 1 : 0; });
      shareText('Meal plan', PR.ShareText.plan(D.label(s.week) + ' \u2013 ' + D.label(D.addDays(s.week, 6)), es.map(function (e) { var r = byId(e.id); return [e.date, r ? r.title : 'Recipe', e.servings]; })));
    },
    prices: function () { openPrices(''); },
    priceEdit: function (el) { priceDialog(el.getAttribute('data-key'), el.getAttribute('data-name')); },
    priceDel: function (el) { prices.delete(el.getAttribute('data-key')); savePrices(); version++; renderScreen(topScreen(), true); },
    loDays: function (el) { setPref('leftoverDays', PR.Leftovers.clampDays(leftoverDays() + (+el.getAttribute('data-d')))); renderScreen(topScreen(), true); },
    batch: function (el) { var s = topScreen(), r = byId(s.id); if (!r) return; s.servings = Math.min(PR.MealPlan.MAX_SERVINGS, r.servings * (+el.getAttribute('data-k'))); renderScreen(s, true); },
    cookedThis: function () { var s = topScreen(); cookedDialog(s.id, s.servings); },
    photoPick: function () { var i = $('#photoIn'); if (i) i.click(); },
    photoDel: function () { var s = topScreen(); confirmBox('Remove photo?', 'Your photo of this recipe will be deleted from this browser.', 'Remove', function () { Photos.del(s.id); renderScreen(s, false); }); },
    pricesFor: function () { var s = topScreen(); openPrices(s.id); },
    loAdj: function (el) { PR.Leftovers.adjust(leftovers, el.getAttribute('data-id'), +el.getAttribute('data-d')); saveLeftovers(); refresh(); },
    loDel: function (el) { var id = el.getAttribute('data-id'); leftovers = leftovers.filter(function (e) { return e.id !== id; }); saveLeftovers(); refresh(); },
    loOpen: function (el) { var id = el.getAttribute('data-id'); if (byId(id)) openDetail(id); },
    planPick: function (el) {
      var date = el.getAttribute('data-d'), m = matcher(), seenIds = {}, opts = [];
      favoriteIdsNewestFirst().map(byId).filter(Boolean).forEach(function (r) { if (!seenIds[r.id] && opts.length < 6) { seenIds[r.id] = 1; opts.push({ r: r, why: 'Favorite' }); } });
      suggestable(filteredResults().filter(function (x) { return x.haveNonStaple > 0; })).forEach(function (x) { if (!seenIds[x.recipe.id] && opts.length < 14) { seenIds[x.recipe.id] = 1; opts.push({ r: x.recipe, why: x.missing.length ? 'Missing ' + x.missing.length : 'Cook now' }); } });
      if (!opts.length) { toast('Open any recipe and tap \u201cAdd to plan\u201d'); return; }
      sheet('Add to ' + D.label(date), opts.map(function (o) {
        return { label: o.r.title + ' \u00b7 ' + o.why, icon: 'restaurant', fn: function () { addToPlan(date, o.r.id, defaultServings()); var s = planState(); renderPlan(s, true); } };
      }).concat([{ label: 'Find another recipe\u2026', icon: 'search', fn: function () { actions.tab({ getAttribute: function () { return 'recipes'; } }); toast('Open a recipe and tap \u201cAdd to plan\u201d'); } }]));
      void m;
    },
    planServ: function (el) { var e = plan[+el.getAttribute('data-i')]; if (!e) return; e.servings = Math.max(1, Math.min(24, e.servings + (+el.getAttribute('data-d')))); savePlan(); renderPlan(planState(), true); },
    planDel: function (el) { plan.splice(+el.getAttribute('data-i'), 1); savePlan(); renderPlan(planState(), true); },
    planToList: function () {
      var s = planState(), items = PR.MealPlan.shopping(PR.MealPlan.forWeek(plan, s.week), byId, matcher(), qtyMap()), n = 0;
      items.forEach(function (it) { if (addShopping(it.name, isNaN(it.amount) ? null : it.amount, isNaN(it.amount) ? '' : it.unit, it.recipes.join(', '))) n++; });
      toast(n ? 'Added ' + plural(n, 'item', 'items') + ' to your shopping list' : 'Already on your shopping list');
      renderTabs();
    },
    rate: function (el) {
      var s = topScreen(), v = +el.getAttribute('data-v'), cur = rating(s.id);
      if (cur && cur.stars === v && v > 0) v = 0;
      var ta = $('textarea[data-change="note"]', s.el);
      setRating(s.id, v, ta ? ta.value : null);
      toast(v ? 'Rated ' + v + ' of 5' : 'Rating cleared');
      renderScreen(s, true); refresh();
    },
    delRecipe: function () {
      var s = topScreen(), r = byId(s.id);
      if (!r || !isUser(r)) return;
      confirmBox('Delete \u201c' + r.title + '\u201d?', 'This removes your imported recipe from this browser, along with its favorite and meal-plan entries.', 'Delete', function () {
        userRecipes.delete(r.id); saveUserRecipes();
        favorites = favorites.filter(function (f) { return f.id !== r.id; }); Store.set('favorites', favorites);
        plan = plan.filter(function (e) { return e.id !== r.id; }); savePlan();
        setRating(r.id, 0, '');
        toast('Recipe deleted'); back(); refresh();
      });
    },
    dlgDate: function (el) { var i = $('#dlg [data-dv="d"]'); if (i) i.value = D.addDays(D.today(), +el.getAttribute('data-v')); },
    avoid: function () { pushScreen({ type: 'avoid' }); },
    avoidToggle: function (el) { var v = el.getAttribute('data-v'), i = allergens.indexOf(v); if (i >= 0) allergens.splice(i, 1); else allergens.push(v); allergens = PR.Allergens.ALL_IDS.filter(function (x) { return allergens.indexOf(x) >= 0; }); saveAvoid(); renderScreen(topScreen(), true); },
    disAdd: function () {
      var inp = $('#disIn'); if (!inp) return;
      var added = 0;
      inp.value.split(',').forEach(function (t) { var k = PR.Allergens.dislikeKey(t); if (k && dislikes.indexOf(k) < 0 && dislikes.length < 200) { dislikes.push(k); added++; } });
      if (!added) { if (PR.jtrim(inp.value)) toast('Already on your list'); return; }
      inp.value = ''; saveAvoid(); renderScreen(topScreen(), true);
      var again = $('#disIn'); if (again) again.focus();
    },
    disDel: function (el) { dislikes = dislikes.filter(function (d) { return d !== el.getAttribute('data-v'); }); saveAvoid(); renderScreen(topScreen(), true); },
    appearance: function () { pushScreen({ type: 'appearance' }); },
    themeUse: function (el) { Theme.use(el.getAttribute('data-id')); renderScreen(topScreen(), true); },
    themeFont: function (el) { Theme.set('font', el.getAttribute('data-v')); renderScreen(topScreen(), true); },
    themeCorners: function (el) { Theme.set('corners', el.getAttribute('data-v')); renderScreen(topScreen(), true); },
    themeAccent: function (el) { Theme.set('accentPick', +el.getAttribute('data-v')); renderScreen(topScreen(), true); },
    themeRandom: function () { var id = PR.Palette.randomPreset(Theme.state().active, Math.random()); Theme.use(id); renderScreen(topScreen(), true); toast('Theme: ' + PR.Palette.preset(id).name); },
    themeNew: function () { openThemeEditor(Theme.current(), ''); },
    themeCopy: function (el) { var b = PR.Palette.preset(el.getAttribute('data-id')) || Theme.current(); if (b) openThemeEditor(b, ''); },
    themeEdit: function (el) { var b = Theme.state().find(el.getAttribute('data-id')); if (b) openThemeEditor(b, b.id); },
    themeDel: function (el) {
      var b = Theme.state().find(el.getAttribute('data-id')); if (!b) return;
      confirmBox('Delete \u201c' + b.name + '\u201d?', 'This custom theme will be removed.' + (Theme.state().active === b.id ? ' The app switches back to Light.' : ''), 'Delete', function () { Theme.remove(b.id); renderScreen(topScreen(), true); });
    },
    themeReset: function () { confirmBox('Reset appearance?', 'Switches back to the default Light theme. Your custom themes are kept.', 'Reset', function () { Theme.reset(); renderScreen(topScreen(), true); toast('Back to the default theme'); }); },
    teSave: function () { saveTheme(topScreen(), false); },
    teSaveNew: function () { saveTheme(topScreen(), true); },
    backup: function () { pushScreen({ type: 'backup', msg: '' }); },
    exportBackup: function () {
      var t;
      var bs = topScreen();
      try { t = backupText(!!(bs && bs.photos)); } catch (e) { toast('Could not make the backup: ' + e.message); return; }
      download('pantry-recipes-backup-' + D.today() + '.json', t, 'application/json');
      toast('Backup saved (' + Math.max(1, Math.round(t.length / 1024)) + ' KB)');
    },
    importBackup: function () { var f = $('#bkFile'); if (f) { f.value = ''; f.click(); } },
    importRecipe: function () { pushScreen({ type: 'importRecipe', text: '', parsed: null, err: '' }); },
    impParse: function () {
      var s = topScreen(), ta = $('#impText', s.el);
      s.text = ta ? ta.value : ''; s.parsed = null; s.err = '';
      try { s.parsed = PR.RecipeImport.parse(s.text); } catch (e) { s.err = e.recipeImport ? e.message : 'Could not read that: ' + e.message; }
      renderScreen(s, true);
    },
    impSave: function () {
      var s = topScreen(); if (!s.parsed) return;
      var id = 'u:' + Date.now();
      while (userRecipes.has(id)) id += '1';
      var r = PR.RecipeImport.toRecipe(s.parsed.recipe, id);
      userRecipes.set(id, r); saveUserRecipes();
      toast('Saved \u201c' + r.title + '\u201d');
      s.parsed = null; s.text = ''; renderScreen(s, false);
      openDetail(id);
    }
  };
  function loadBrowse(s) {
    s.busy = true; s.msg = ''; renderScreen(s, true);
    browseOnline(s.kind, s.value, 16).then(function (res) {
      s.busy = false; s.remaining = res.remaining;
      s.msg = res.error ? res.error : res.added === 0 && res.remaining === 0 ? 'All TheMealDB recipes for ' + s.value + ' are loaded.' : '';
      if (ui.stack.indexOf(s) >= 0) renderScreen(s, true);
      refresh();
    });
  }
  document.addEventListener('click', function (e) {
    var el = e.target.closest ? e.target.closest('[data-a]') : null;
    if (!el) {
      if (e.target.id === 'dlg') closeDialog();
      if (!e.target.closest || !e.target.closest('.field')) { var b = $('[data-region="ac"]'); if (b) b.hidden = true; }
      return;
    }
    if (el.disabled) return;
    var f = actions[el.getAttribute('data-a')];
    if (f) { e.preventDefault(); f(el, e); }
  });
  var inTimers = {};
  document.addEventListener('input', function (e) {
    var t = e.target, te = t.getAttribute && (t.getAttribute('data-te') || t.getAttribute('data-teh'));
    if (te) {
      var s0 = topScreen();
      if (!s0 || s0.type !== 'themeEdit') return;
      var v = PR.Palette.parseHex(t.value);
      if (v < 0) { t.classList.add('bad'); return; }
      t.classList.remove('bad');
      s0.draft[te] = v; teRefresh(s0, t);
      return;
    }
    var k = e.target.getAttribute && e.target.getAttribute('data-in');
    if (!k) return;
    if (k === 'conv') { convUpdate(); return; }
    var val = e.target.value;
    if (k === 'add') { ui.addText = val; renderAc(); return; }
    clearTimeout(inTimers[k]);
    inTimers[k] = setTimeout(function () {
      if (k === 'quick') { ui.quick = val; refreshRegion('grid'); }
      else if (k === 'rq') { ui.query = val; ui.listLimit = PAGE; refreshRegion('list'); }
      else if (k === 'dq') { ui.dq = val; refreshRegion('results'); }
      else if (k === 'hq') { var s2 = topScreen(); if (s2 && s2.type === 'helpers20') { s2.q = val; s2.open = ''; var hb = $('[data-region="body"]', s2.el); if (hb) hb.innerHTML = SCREENS.helpers20.regions.body(s2); } }
      else if (k === 'subq') { var s = topScreen(); if (s && s.type === 'subs') { s.q = val; s.limit = 80; renderScreen(s, true); } }
    }, 120);
  });
  function refreshRegion(name) {
    var el = $('#main [data-region="' + name + '"]'), f = VIEWS[ui.tab].regions[name];
    if (el && f) el.innerHTML = f();
  }
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') { if ($('#dlg')) closeDialog(); else if (ui.stack.length) back(); return; }
    if (e.key !== 'Enter') return;
    var k = e.target.getAttribute && e.target.getAttribute('data-enter');
    if (!k) return;
    e.preventDefault();
    if (k === 'dsearch') ui.dq = e.target.value;
    actions[k](e.target);
  });
  document.addEventListener('change', function (e) {
    var k = e.target.getAttribute && e.target.getAttribute('data-change');
    if (v20Change(k, e.target)) return;
    if (k === 'online') { setPref('online', e.target.checked); net.lastSig = ''; setPref('fetchSig', ''); version++; refresh(); }
    else if (k === 'incDesserts' || k === 'incSides' || k === 'dIncDesserts' || k === 'dIncSides') { ui[k] = e.target.checked; saveMeals(); refresh(); }
    else if (k === 'apikey') { var v = PR.jtrim(e.target.value); setPref('apiKey', v || '1'); net.lastSig = ''; toast('API key saved'); }
    else if (k === 'note') { var s = topScreen(); if (s && s.type === 'detail') { setRating(s.id, null, e.target.value); toast('Note saved'); } }
    else if (k === 'currency') { var cv = PR.jtrim(e.target.value).substring(0, 4); setPref('currency', cv || '$'); e.target.value = currency(); toast('Currency saved'); renderScreen(topScreen(), true); }
    else if (k === 'decorations') { setPref('decorations', e.target.checked); applyDeco(); refresh(); }
    else if (k === 'showCups') { setPref('showCups', e.target.checked); unitsChanged(); }
    else if (k === 'conv') convUpdate();
    else if (k === 'useDefaultPrices' || k === 'shopCost') { setPref(k, e.target.checked); version++; renderScreen(topScreen(), true); }
    else if (k === 'bkPhotos') { var bsx = topScreen(); if (bsx) bsx.photos = e.target.checked; }
    else if (k === 'photoFile') {
      var pf = e.target.files && e.target.files[0], ps = topScreen();
      if (!pf || !ps || ps.type !== 'detail') return;
      downscalePhoto(pf).then(function (url) { Photos.put(ps.id, url); toast('Photo saved'); renderScreen(ps, false); }, function (err) { toast(err && err.message ? err.message : 'Could not use that photo'); });
      e.target.value = '';
    }
    else if (k === 'teBase') {
      var s2 = topScreen(), b = PR.Palette.preset(e.target.value);
      if (s2 && b) { ['bg', 'surface', 'primary', 'accent', 'text'].forEach(function (x) { s2.draft[x] = b[x]; }); teRefresh(s2, null); }
      e.target.value = '';
    }
    else if (k === 'importFile') {
      var f = e.target.files && e.target.files[0], bs = topScreen();
      if (!f) return;
      if (f.size > PR.Backup.MAX_CHARS * 2) { bs.msg = 'That file is too large to be a My Pantry Recipes backup.'; renderScreen(bs, false); return; }
      var rd = new FileReader();
      rd.onload = function () {
        var d;
        try { d = PR.Backup.parse(String(rd.result || '')); } catch (err) { bs.msg = err.backup ? err.message : 'Could not read the file.'; renderScreen(bs, false); return; }
        bs.msg = '';
        confirmBox('Replace everything?', 'This backup' + (d.platform ? ' (from ' + d.platform + (d.appVersion ? ' ' + d.appVersion : '') + (d.exported ? ', ' + d.exported.substring(0, 10) : '') + ')' : '') + ' has ' + PR.Backup.summary(d) +
          '. Importing replaces your current pantry, favorites, shopping list, plan, ratings, dislikes, prices, leftovers, settings, themes and imported recipes in this browser' + (d.photos.size ? '; its photos are added to yours' : '; your photos stay as they are') + '.', 'Import', function () {
          applyBackup(d); toast('Backup imported'); renderScreen(bs, false); refresh();
        });
      };
      rd.onerror = function () { bs.msg = 'Could not read the file.'; renderScreen(bs, false); };
      rd.readAsText(f);
    }
  });
  // drop broken thumbnails so the coloured letter shows through
  document.addEventListener('error', function (e) { if (e.target && e.target.tagName === 'IMG') e.target.remove(); }, true);
  window.addEventListener('online', function () { refresh(); });
  window.addEventListener('offline', function () { refresh(); });

  // =====================================================================
  // v2.0 Stage 1: region, inventory, ZIP prices, diets, household, editor
  // Heavy screens are built only when opened; nothing here runs at start-up
  // except one cheap check for the welcome question.
  // =====================================================================
  function regionId() { return R20.valid(pref('region', '')); }
  function household() { return PR.Household.parse(Store.get('household', [])); }
  function saveHousehold(ps) { Store.set('household', PR.Household.toList(ps)); version++; }
  function avoidAllergens() { return PR.Household.avoid(allergens, household()); }
  function regionAllergenIds() { return R20.allergens(regionId() || R20.US); }
  function wkStart(iso) { return R20.weekStart(iso, pref('weekStart', 'mon') === 'sun'); }
  function fmtDate(iso) { return R20.formatDate(iso, pref('dateFormat', 'YYYY-MM-DD')); }
  function loc(text, steps) { var r = regionId(); return r ? R20.localize(text, r, steps) : text; }
  function applyRegion(r) {
    var d = R20.defaults(r);
    setPref('region', r); setPref('units', d.units); setPref('weightUnit', d.weightUnit); setPref('currency', d.currency); setPref('showCups', d.showCups);
    setPref('dateFormat', d.dateFormat); setPref('gasMarks', d.gasMarks); setPref('fanNote', d.fanNote); setPref('decimalComma', d.decimalComma);
    setPref('clock24', d.clock24); setPref('weekStart', d.weekStartsSunday ? 'sun' : 'mon');
    PRICE_MEMO = null; BUDGET_VER++; version++;
  }
  // ---- diets: bundled recipes carry stored diets; others are classified once and memoised
  function dietsOf(r) {
    var m = DIET_MEMO.get(r.id);
    if (!m || m.r !== r) {
      var d = CORE_DIETS.filter(function (x) { return r.tags.indexOf(x) >= 0; });
      (Array.isArray(r.diets) ? r.diets : PR.Diets.classify(r, FOODS)).forEach(function (x) { if (d.indexOf(x) < 0) d.push(x); });
      m = { r: r, d: d }; DIET_MEMO.set(r.id, m);
    }
    return m.d;
  }
  function dietsPass(r) {
    if (!ui.diets.length) return true;
    var d = dietsOf(r);
    for (var i = 0; i < ui.diets.length; i++) {
      var x = ui.diets[i];
      if (x.lastIndexOf('fast:', 0) === 0) { if (!fitsFast(x.slice(5), r)) return false; }
      else if (d.indexOf(x) < 0) return false;
    }
    return true;
  }
  // v2.0 Stage 4: ticked fasting labels as optional diet chips (off by default); per-recipe result bits are memoised
  var fastMemo = new Map();
  function fastChipIds() { if (!loaded || !pref('fastChips', false)) return []; var all = s3().fasts.map(function (f) { return f.id; }); return fastTicked().filter(function (id) { return all.indexOf(id) >= 0; }).map(function (id) { return 'fast:' + id; }); }
  function fitsFast(id, r) {
    var fs = s3().fasts, idx = -1;
    for (var i = 0; i < fs.length; i++) if (fs[i].id === id) idx = i;
    if (idx < 0) return true;
    var m = fastMemo.get(r);
    if (m === undefined) { m = 0; for (var j = 0; j < fs.length && j < 31; j++) if (S3.fits(fs[j], r)) m |= 1 << j; fastMemo.set(r, m); }
    return (m & (1 << idx)) !== 0;
  }
  function pruneFastDiets() {
    if (!ui.diets.some(function (x) { return x.lastIndexOf('fast:', 0) === 0; })) return;
    var ok = fastChipIds();
    ui.diets = ui.diets.filter(function (x) { return x.lastIndexOf('fast:', 0) !== 0 || ok.indexOf(x) >= 0; });
    setPref('diets', ui.diets);
  }
  // ---- prices: US = BLS regional averages (+ built-in estimates x factor); elsewhere only the user's prices
  function blsTable() { return BLS || (BLS = RP.parse((window.PR_DATA20 || {}).bls || '')); }
  function priceRegion() { return RP.region(RP.cleanZip(pref('zip', ''))); }
  function priceSources() { priceDefaults20(); return PRICE_MEMO.src; }
  function priceDefaults20() {
    if (!useDefaultPrices()) return null;
    var r = regionId();
    if (r && r !== R20.US) return null;
    var pr = priceRegion();
    if (!PRICE_MEMO || PRICE_MEMO.k !== pr) { var src = new Map(); PRICE_MEMO = { k: pr, m: RP.defaults(blsTable(), pr, DEF_PRICES, src), src: src }; }
    return PRICE_MEMO.m;
  }
  function budget() { var b = +pref('budget', 0); return b > 0 ? b : 0; }
  function budgetOn() { return !!pref('budgetMode', false) && budget() > 0; }
  function budgetKey() { return BUDGET_VER + '|' + budget() + '|' + bundled.length; }
  /** 0 = over budget, 1 = within, 2 = price unknown (kept, shown as "price unknown"). */
  function budgetCompute(r) {
    var e = PR.Cost.estimate(r, r.servings, prices, priceDefaults(), FOODS);
    var v = !e || !e.show() ? 2 : RP.withinBudget(e, budget()) ? 1 : 0;
    BUDGET_MEMO.m.set(r.id, v); return v;
  }
  /** v2.0 S3: the first pass over all recipes runs in small slices after the current render (HD 8), then refreshes once. */
  function budgetWarm(k) {
    if (BUDGET_WARM === k) return; BUDGET_WARM = k;
    var list = allRecipes(), i = 0, t0 = Date.now();
    (function step() {
      if (BUDGET_MEMO.k !== k || BUDGET_WARM !== k) return;
      var end = Math.min(list.length, i + 120);
      for (; i < end; i++) if (!BUDGET_MEMO.m.has(list[i].id)) budgetCompute(list[i]);
      if (i < list.length) { setTimeout(step, 0); return; }
      BUDGET_WARM = 'done:' + k; window.__prBudgetWarmMs = Date.now() - t0; version++; refresh();
    })();
  }
  function budgetState(r) {
    if (!budgetOn()) return 1;
    var k = budgetKey();
    if (BUDGET_MEMO.k !== k) BUDGET_MEMO = { k: k, m: new Map() };
    var v = BUDGET_MEMO.m.get(r.id);
    if (v != null) return v;
    if (BUDGET_WARM !== 'done:' + k) { budgetWarm(k); return 1; }
    return budgetCompute(r);
  }
  function budgetPass(r) { return budgetState(r) !== 0; }
  function priceUnknown(r) { return budgetOn() && BUDGET_MEMO.k === budgetKey() && BUDGET_MEMO.m.get(r.id) === 2; }
  // ---- inventory
  function invItems() { return pantry.map(function (p) { var it = INV.item(p.key, p.label); it.loc = p.loc || ''; it.frozen = p.frozen || ''; it.useBy = p.useBy || ''; it.qty = p.qty || ''; it.staple = !!p.staple; it.low = !!p.low; return it; }); }
  function expiringTomorrow() { return INV.expiringIn(invItems(), D.today(), 1); }
  function cookTonightPick() {
    var ex = expiringTomorrow(); if (!ex.length) return null;
    var exp = new Set(ex.map(function (i) { return i.key; })), have = new Set(pantry.map(function (p) { return p.key; }));
    var r = INV.cookTonight(allRecipes().filter(function (x) { return !PR.isOnline(x) || online.has(x.id); }), exp, have);
    return r ? { recipe: r, items: ex } : null;
  }
  function v20Banners() {
    var out = holBanner();
    if (pref('expiryReminders', true)) {
      var ct = cookTonightPick();
      if (ct) out += '<div class="banner">' + ic('timer') + '<div class="grow"><b>Use by tomorrow: ' + esc(ct.items.map(function (i) { return i.label; }).slice(0, 3).join(', ')) + '</b>' +
        '<div class="muted">Cook this tonight: ' + esc(ct.recipe.title) + '</div></div><button class="btn small" data-a="open" data-id="' + esc(ct.recipe.id) + '">Open</button></div>';
    }
    var fz = INV.freezerAlerts(invItems(), D.today());
    if (fz.length) out += '<div class="banner warn">' + ic('warning') + '<div class="grow"><b>Freezer: use or toss</b><div class="muted">' + esc(fz.slice(0, 4).map(function (i) { return i.label + ' (best by ' + fmtDate(INV.useOrTossDate(i.frozen, i.key)) + ')'; }).join(', ')) + '</div></div><button class="btn small alt" data-a="inv">Inventory</button></div>';
    return out;
  }

  SCREENS.welcome = {
    full: function (s) {
      // v2.0.1: one step (starter kits removed). The suggestion comes from navigator.language only; no geolocation API is used.
      var g = s.guess, h = topBar('Welcome', '') + '<div class="main"><div class="wrap">';
      h += '<div class="card"><h2>Where do you cook?</h2><div class="muted" style="margin-bottom:10px">Sets units, temperatures, currency, date format, week start, clock and the allergen list. You can change each one later in Settings \u2192 Region &amp; formats.</div>' +
        '<div class="faint note" style="margin-bottom:10px">Suggested from your device language. This app never uses your location. You can pick any region.</div>' +
        R20.IDS.map(function (id) { return '<button class="setrow" data-a="welRegion" data-v="' + id + '"><span class="grow"><b>' + esc(R20.name(id)) + '</b>' + (id === g ? '<span class="faint">Suggested</span>' : '') + '</span>' + ic('next', 's') + '</button>'; }).join('') + '<div class="row wrapbtns" style="margin-top:10px"><button class="btn ghost" data-a="welDone">Skip</button></div></div>';
      return h + '</div></div>';
    }
  };
  SCREENS.region = {
    full: function (s) { return topBar('Region & formats', R20.name(regionId())) + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function () {
        var r = regionId(), sel = function (k, list, cur) { return '<select class="tin" data-change="v20sel" data-k="' + k + '" aria-label="' + k + '">' + list.map(function (o) { return '<option value="' + esc(o[0]) + '"' + (o[0] === cur ? ' selected' : '') + '>' + esc(o[1]) + '</option>'; }).join('') + '</select>'; };
        var sw = function (k, label, sub, def) { return '<label class="switchrow"><span class="grow">' + label + (sub ? '<span class="faint" style="display:block">' + sub + '</span>' : '') + '</span><input type="checkbox" class="switch" data-change="v20sw" data-k="' + k + '"' + (pref(k, def) ? ' checked' : '') + '></label>'; };
        var h = '<div class="card"><h2>Region</h2><div class="chips">' + R20.IDS.map(function (id) { return chip(R20.name(id), r === id, 'regionSet', ' data-v="' + id + '"'); }).join('') + '</div>' +
          '<div class="faint" style="margin-top:6px">Choosing a region resets the settings below to its defaults. Each one stays editable.</div></div>';
        h += '<div class="card"><h2>Defaults</h2>' +
          '<label class="flabel">Units' + sel('units', [['us', 'US cups & spoons'], ['metric', 'Metric (ml, g)'], ['weights', 'Weights where possible'], ['original', 'As written']], unitMode()) + '</label>' +
          '<label class="flabel">Date format' + sel('dateFormat', R20.DATE_FORMATS.map(function (f) { return [f, f + ' (' + R20.formatDate(D.today(), f) + ')']; }), pref('dateFormat', 'YYYY-MM-DD')) + '</label>' +
          '<label class="flabel">Week starts on' + sel('weekStart', [['mon', 'Monday'], ['sun', 'Sunday']], pref('weekStart', 'mon')) + '</label>' +
          '<label class="flabel">Currency symbol<input class="tin" type="text" data-change="currency" maxlength="4" value="' + esc(currency()) + '" aria-label="Currency symbol"></label>' +
          sw('clock24', '24-hour clock', 'Now: ' + R20.formatTime(19 * 60 + 30, pref('clock24', false)), false) +
          sw('decimalComma', 'Decimal comma', 'Shows 2,50 instead of 2.50 for money', false) +
          sw('gasMarks', 'Show gas marks', 'Oven temperatures get a gas mark', false) +
          sw('fanNote', 'Fan oven note', R20.FAN_NOTE, false) + '</div>';
        h += '<div class="card"><h2>Allergen list</h2><div class="muted">' + esc(R20.allergenListName(r)) + ': ' + esc(regionAllergenIds().map(function (id) { return R20.allergenLabel(id, r); }).join(', ')) + '</div></div>';
        h += '<div class="card"><h2>Measuring habits</h2>' + R20.measuringNotes(r).map(function (n) { return '<div class="prow"><span class="grow"><b>' + esc(n[0]) + '</b><span class="faint">' + esc(n[1]) + '</span></span></div>'; }).join('') + '</div>';
        h += '<div class="card"><h2>Ingredient names</h2><div class="muted">Recipes show the names used in your region (e.g. ' + esc(R20.localize('zucchini, cilantro, eggplant', r || R20.UK)) + '). Search finds either name.</div>' +
          '<div class="faint" style="margin-top:6px">' + R20.terms().length + ' name pairs. Holiday defaults for this region: ' + R20.holidayDefaults(r).length + ' (used by Settings \u2192 Holidays in a later update).</div></div>';
        return h + '<div class="faint note">' + esc(r && r !== R20.US ? R20.PRICE_NOTE_NON_US : RP.NOTE) + '</div>';
      }
    }
  };
  SCREENS.inv = {
    full: function (s) { return topBar('Kitchen inventory', pantry.length + ' items') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function (s) {
        var items = invItems(), n = INV.counts(items), t = D.today(), lc = s.loc || INV.PANTRY;
        var h = '<div class="chips">' + INV.LOCATIONS.map(function (l, i) { return chip(INV.LOCATION_NAMES[i] + ' <span class="faint">' + n[i] + '</span>', lc === l, 'invLoc', ' data-v="' + l + '"'); }).join('') + '</div>';
        var list = INV.at(items, lc), max = s.limit || PAGE;
        h += '<div class="card">' + (list.length ? list.slice(0, max).map(function (it) {
          var extra = [];
          if (it.qty) extra.push(esc(it.qty));
          if (it.useBy) extra.push('use by ' + esc(fmtDate(it.useBy)));
          if (lc === INV.FREEZER && it.frozen) { var st = INV.freezerStatus(it.frozen, it.key, t); extra.push('frozen ' + esc(fmtDate(it.frozen)) + ' \u00b7 best by ' + esc(fmtDate(INV.useOrTossDate(it.frozen, it.key))) + (st === INV.OK ? '' : st === INV.PAST ? ' \u00b7 toss or use now' : ' \u00b7 use soon')); }
          return '<div class="prow"><span class="grow">' + esc(it.label) + (it.staple ? ' <span class="faint">always on hand</span>' : '') + '<span class="faint">' + extra.join(' \u00b7 ') + '</span></span>' +
            '<button class="chip' + (it.low ? ' on' : '') + '" data-a="invLow" data-key="' + esc(it.key) + '" aria-pressed="' + it.low + '">Low</button>' +
            '<button class="iconbtn" data-a="invEdit" data-key="' + esc(it.key) + '" aria-label="Edit ' + esc(it.label) + '">' + ic('list', 's') + '</button></div>';
        }).join('') + (list.length > max ? '<button class="btn ghost block" data-a="invMore">Show more (' + (list.length - max) + ')</button>' : '') : '<div class="muted">Nothing here yet.</div>') + '</div>';
        if (lc === INV.FREEZER) h += '<div class="faint note">' + esc(INV.FREEZER_NOTE) + '</div>';
        var rs = INV.restock(items);
        h += '<div class="card"><div class="row"><h2 class="grow" style="margin:0">Restock list <span class="faint">(' + rs.length + ')</span></h2>' + (rs.length ? '<button class="link" data-a="invRestock">' + ic('cart', 'xs') + ' Add to shopping</button>' : '') + '</div>' +
          (rs.length ? '<div class="muted">' + esc(rs.map(function (i) { return i.label; }).join(', ')) + '</div>' : '<div class="muted">Mark items \u201cLow\u201d and they appear here.</div>') + '</div>';
        if (lc === INV.SPICE) {
          if (!s.spices) { var have = new Set(pantry.map(function (p) { return p.key; })); s.spices = INV.spicesToBuy(have, INV.keyCounts(bundled), 8); }
          h += '<div class="card"><h2>Spices to buy next</h2>' + (s.spices.length ? s.spices.map(function (x) { return '<div class="prow"><span class="grow">' + esc(x[0]) + '<span class="faint">used in ' + x[2] + ' recipes</span></span><button class="btn alt small" data-a="invShop" data-v="' + esc(x[0]) + '">Add to list</button></div>'; }).join('') : '<div class="muted">You have every common spice.</div>') + '</div>';
        }
        h += '<label class="switchrow"><span class="grow">Take used amounts out on \u201cCooked this\u201d</span><input type="checkbox" class="switch" data-change="v20sw" data-k="deductPantry"' + (pref('deductPantry', false) ? ' checked' : '') + '></label>' +
          '<label class="switchrow"><span class="grow">Expiry reminders<span class="faint" style="display:block">A banner the day before something expires, with a \u201ccook this tonight\u201d idea. The Android apps also send a notification; the web app cannot notify while closed.</span></span><input type="checkbox" class="switch" data-change="v20sw" data-k="expiryReminders"' + (pref('expiryReminders', true) ? ' checked' : '') + '></label>';
        return h;
      }
    }
  };
  function invEditDialog(key) {
    var p = pantryItem(key); if (!p) return;
    var lc = p.loc || INV.defaultLocation(p.key, p.label);
    dialog({ title: p.label, html: '<label class="flabel">Where<select class="tin" data-dv="l">' + INV.LOCATIONS.map(function (l, i) { return '<option value="' + l + '"' + (l === lc ? ' selected' : '') + '>' + INV.LOCATION_NAMES[i] + '</option>'; }).join('') + '</select></label>' +
      '<label class="flabel">Quantity<input class="tin" type="text" data-dv="q" maxlength="40" value="' + esc(p.qty || '') + '" placeholder="e.g. 2 cans, 500 g"></label>' +
      '<label class="flabel">Use by<input class="tin" type="date" data-dv="u" value="' + esc(p.useBy || '') + '"></label>' +
      '<label class="flabel">Frozen on (freezer only)<input class="tin" type="date" data-dv="f" value="' + esc(p.frozen || '') + '"></label>' +
      '<label class="mcheck"><input type="checkbox" data-dv="s"' + (p.staple ? ' checked' : '') + '> Always on hand</label>',
      buttons: [{ label: 'Remove', fn: function () { removeFromPantry(key); renderScreen(topScreen(), true); } }, { label: 'Cancel' },
        { label: 'Save', primary: true, fn: function (v) { p.loc = INV.validLocation(v.l); p.qty = String(v.q || '').substring(0, 40); p.useBy = D.valid(v.u) ? v.u : ''; p.frozen = p.loc === INV.FREEZER && D.valid(v.f) ? v.f : ''; p.staple = !!v.s; savePantry(); renderScreen(topScreen(), true); } }] });
  }
  SCREENS.household = {
    full: function (s) { return topBar('Household allergies', 'Alerts for the people you cook for') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function () {
        var ps = household(), r = regionId(), ids = regionAllergenIds();
        var h = '<div class="muted pad">Add people (first names or nicknames). Recipes show an alert naming who a recipe may not suit. Tick \u201cHide\u201d to hide those recipes like your own allergies. Stays on this device and in backups.</div>';
        h += ps.map(function (p, n) {
          return '<div class="card"><div class="row"><h2 class="grow" style="margin:0">' + esc(p.name) + '</h2><label class="mcheck"><input type="checkbox" data-change="hhHide" data-n="' + n + '"' + (p.hide ? ' checked' : '') + '> Hide</label><button class="iconbtn" data-a="hhDel" data-n="' + n + '" aria-label="Remove ' + esc(p.name) + '">' + ic('delete', 's') + '</button></div><div class="chips">' +
            ids.concat(p.allergens.filter(function (x) { return ids.indexOf(x) < 0; })).map(function (id) { var on = p.allergens.indexOf(id) >= 0; return chip((on ? ic('check', 'xs') : '') + esc(R20.allergenLabel(id, r)), on, 'hhToggle', ' data-n="' + n + '" data-v="' + id + '"'); }).join('') + '</div></div>';
        }).join('');
        if (ps.length < PR.Household.MAX) h += '<div class="row"><div class="field grow"><input type="text" id="hhIn" data-enter="hhAdd" maxlength="30" placeholder="Add a person" autocomplete="off" aria-label="Add a person"></div><button class="btn" data-a="hhAdd">Add</button></div>';
        return h + '<div class="faint note">' + esc(PR.Allergens.NOTE) + '</div>';
      }
    }
  };
  // ---- recipe editor (no OCR; photos use the existing light photo flow on the recipe page)
  SCREENS.editor = {
    full: function (s) {
      var d = s.draft, C = PR.RecipeEditor;
      var f = function (k, label, type, extra) { return '<label class="flabel">' + label + '<input class="tin" type="' + (type || 'text') + '" data-ed="' + k + '" value="' + esc(d[k] < 0 ? '' : d[k]) + '"' + (extra || '') + '></label>'; };
      var ta = function (k, label, rows, ph) { return '<label class="flabel">' + label + '<textarea class="tin" data-ed="' + k + '" rows="' + rows + '" placeholder="' + esc(ph) + '"' + (k === 'ingredients' ? ' id="edIng" data-in="edIng"' : '') + '>' + esc(d[k]) + '</textarea></label>'; };
      return topBar(s.id ? 'Edit recipe' : d.basedOn ? 'My version' : 'New recipe', d.basedOn ? 'Based on ' + ((byId(d.basedOn) || {}).title || '') : '') + '<div class="main"><div class="wrap"><div class="card">' +
        f('title', 'Title', 'text', ' maxlength="' + C.MAX_TITLE + '"') +
        '<label class="flabel">Category<select class="tin" data-ed="category">' + C.CATEGORIES.map(function (c) { return '<option' + (c === d.category ? ' selected' : '') + '>' + c + '</option>'; }).join('') + '</select></label>' +
        f('cuisine', 'Cuisine (country or region)', 'text', ' maxlength="60" list="edCu"') + '<datalist id="edCu">' + PR.facet(bundled, true).map(function (c) { return '<option value="' + esc(c[0]) + '">'; }).join('') + '</datalist>' +
        '<div class="row">' + f('servings', 'Servings', 'number', ' min="1" max="24"') + f('prep', 'Prep min', 'number', ' min="0" max="1440"') + f('cook', 'Cook min', 'number', ' min="0" max="2880"') + '</div>' +
        ta('ingredients', 'Ingredients, one per line', 8, '2 cups flour\n1 tsp salt\n3 eggs') + '<div data-region="edAc" class="chips"></div>' +
        ta('steps', 'Steps, one per line', 8, 'Heat the oven to 180 C.\nMix the dry ingredients.') +
        f('tags', 'Your tags, comma-separated', 'text', ' maxlength="300"') + ta('note', 'Note', 3, 'Where it came from, family tips') +
        (s.err ? '<div class="banner warn">' + ic('warning') + '<div class="grow">' + esc(s.err) + '</div></div>' : '') +
        '<div class="row wrapbtns"><button class="btn" data-a="edSave">Save</button><button class="btn ghost" data-a="back">Cancel</button></div></div>' +
        '<div class="faint note">Diet and allergen tags are worked out from the ingredient names when you save (a guide, not certification). Add a photo from the recipe page after saving.</div></div></div>';
    }
  };
  function edRead(s) { $$('[data-ed]').forEach(function (e) { var k = e.getAttribute('data-ed'), v = e.value; s.draft[k] = k === 'servings' ? Math.max(1, +v || 4) : k === 'prep' || k === 'cook' ? (v === '' ? -1 : Math.max(0, +v || 0)) : v; }); }
  function edAc() {
    var el = $('#edIng'), out = $('[data-region="edAc"]'); if (!el || !out) return;
    var lines = el.value.substring(0, el.selectionStart).split('\n'), cur = lines[lines.length - 1], o = PR.RecipeImport.ingredientLine(cur);
    var word = o ? o.name : PR.jtrim(cur);
    out.innerHTML = word.length >= 2 ? acSuggestions(word).slice(0, 6).map(function (n) { return '<button class="chip" data-a="edAcPick" data-v="' + esc(n) + '">' + esc(n) + '</button>'; }).join('') : '';
  }
  function edSave() {
    var s = topScreen(); edRead(s);
    var C = PR.RecipeEditor, p = C.problem(s.draft);
    if (p) { s.err = p; renderScreen(s, false); return; }
    var id = s.id || 'u:' + Date.now().toString(36), r = C.build(s.draft, id, FOODS), old = userRecipes.get(id);
    if (old && old.sourceUrl) r.sourceUrl = old.sourceUrl;
    userRecipes.set(id, r); saveUserRecipes(); DIET_MEMO.delete(id);
    popScreen(); toast('Recipe saved'); if (!s.id) openDetail(id); else renderScreen(topScreen(), false);
  }
  SCREENS.myRecipes = {
    full: function (s) { return topBar('My recipes', userRecipes.size + ' saved') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function (s) {
        var list = Array.from(userRecipes.values()).sort(function (a, b) { return PR.cmpIC(a.title, b.title); }), sel = s.sel;
        var h = '<div class="row wrapbtns"><button class="btn" data-a="edNew">' + ic('add', 's') + 'New recipe</button><button class="btn alt" data-a="importRecipe">Import text</button>' +
          '<button class="btn alt" data-a="packPick">Open a recipe pack</button><input type="file" id="packIn" accept=".json,application/json" data-change="packFile" hidden></div>';
        h += '<div class="card">' + (list.length ? list.map(function (r) { return '<div class="prow"><label class="mcheck grow"><input type="checkbox" data-change="mySel" data-id="' + esc(r.id) + '"' + (sel[r.id] ? ' checked' : '') + '> ' + esc(r.title) + (r.basedOn ? ' <span class="faint">my version</span>' : '') + '</label><button class="btn alt small" data-a="open" data-id="' + esc(r.id) + '">Open</button></div>'; }).join('') : '<div class="muted">No recipes of your own yet.</div>') + '</div>';
        var n = Object.keys(sel).filter(function (k) { return sel[k]; }).length;
        h += '<div class="card"><h2>Share or print the ticked recipes (' + n + ')</h2><div class="muted" style="margin-bottom:8px">A recipe pack is one small file anyone with this app (web or Android) can open. No accounts, nothing uploaded.</div>' +
          '<div class="row wrapbtns"><button class="btn" data-a="packSave"' + (n ? '' : ' disabled') + '>Save recipe pack</button><button class="btn ghost" data-a="cookbook"' + (n ? '' : ' disabled') + '>Printable family cookbook</button></div></div>';
        return h;
      }
    }
  };
  // v2.0 Stage 2: kitchen helpers (data parsed on first open by PR.Helpers20)
  var H20TABS = [['equiv', 'Equivalents'], ['rescue', 'Rescue my dish'], ['blends', 'Spice blends'], ['times', 'Cooking times'], ['explain', 'Ingredients']];
  var H20HINT = { equiv: 'Search, e.g. butter, flour cup, 180 C', rescue: 'What went wrong? e.g. salty, thin, split', blends: 'Search, e.g. curry, BBQ, Moroccan',
    times: 'Search, e.g. salmon, rice, instant pot', explain: 'Search any name, e.g. courgette, maida' };
  function h20Rows(list, groupOf, render) {
    var g = null, h = '';
    list.forEach(function (x, i) { var gg = groupOf ? groupOf(x) : null; if (gg && gg !== g) { if (g !== null) h += '</div>'; g = gg; h += '<div class="hgroup">' + esc(gg) + '</div><div class="card">'; } h += render(x, i); });
    if (g !== null) h += '</div>';
    return h;
  }
  function h20Have(spice) { return !!pantryItem(PR.normalize(spice)); }
  SCREENS.helpers20 = {
    full: function (s) {
      return topBar('Kitchen helpers', 'Works offline') + '<div class="main"><div class="wrap">' +
        '<div class="hscroll">' + sregion('tabs', s) + '</div>' + sregion('search', s) + sregion('body', s) + '</div></div>';
    },
    regions: {
      tabs: function (s) { return '<div class="chips nowrap">' + H20TABS.map(function (t) { return chip(t[1], s.tool === t[0], 'h20Tool', ' data-v="' + t[0] + '"'); }).join('') + '</div>'; },
      search: function (s) {
        if (s.tool === 'blends' && s.blend != null) return '';
        return '<div class="field">' + ic('search', 's') + '<input type="search" data-in="hq" value="' + esc(s.q) + '" placeholder="' + esc(H20HINT[s.tool]) +
          '" autocomplete="off" autocorrect="off" autocapitalize="none" spellcheck="false" enterkeyhint="search" aria-label="Search ' + esc(s.tool) + '"></div>';
      },
      body: function (s) {
        var H = PR.Helpers20, d = H.data(), q = s.q, h = '', none = '<div class="card muted">Nothing matches. Try one word.</div>';
        if (s.tool === 'equiv') {
          var eqs = H.findEquivs(d, q);
          return '<div class="muted" style="margin:6px 0">' + d.equivs.length + ' equivalents. Weights are averages. Use a scale for baking when you can.</div>' +
            (eqs.length ? h20Rows(eqs, function (x) { return x.group; }, function (x) { return '<div class="hrow"><b>' + esc(x.amount) + '</b> = ' + esc(x.equals) + (x.note ? '<div class="faint">' + esc(x.note) + '</div>' : '') + '</div>'; }) : none);
        }
        if (s.tool === 'times') {
          var ts = H.findTimes(d, q);
          return '<div class="muted" style="margin:6px 0">Times are a starting point. Check doneness, and use a thermometer for meat.</div>' +
            (ts.length ? h20Rows(ts, function (x) { return x.group; }, function (x) { return '<div class="hrow"><b>' + esc(x.item) + '</b> = ' + esc(x.time) + '<div class="faint">' + esc(x.method) + (x.note ? ' \u00b7 ' + esc(x.note) : '') + '</div></div>'; }) : none);
        }
        if (s.tool === 'rescue') {
          var rs = H.findRescues(d, q);
          if (!rs.length) return '<div class="card muted">Nothing matches. Try: salty, spicy, thin, burnt, dry.</div>';
          return '<div class="card">' + rs.map(function (r) {
            var open = s.open === r.problem;
            return '<button class="hrow btnrow" data-a="h20Open" data-v="' + esc(r.problem) + '" aria-expanded="' + open + '"><b>' + esc(r.problem) + '</b><div class="faint">' + esc(r.dishes) + '</div></button>' +
              (open ? '<div class="hrow"><b>Try this</b><ul>' + r.fixes.map(function (f) { return '<li>' + esc(f) + '</li>'; }).join('') + '</ul>' + (r.prevent ? '<b>Next time</b><div>' + esc(r.prevent) + '</div>' : '') + '</div>' : '');
          }).join('') + '</div>';
        }
        if (s.tool === 'explain') {
          var reg = regionId(), xs = H.findExplains(d, q);
          if (!xs.length) return none;
          return '<div class="muted" style="margin:6px 0">Names differ by country. Your region\u2019s name is marked.</div><div class="card">' + xs.map(function (x) {
            var open = s.open === x.name, local = H.localName(x, reg);
            return '<button class="hrow btnrow" data-a="h20Open" data-v="' + esc(x.name) + '" aria-expanded="' + open + '"><b>' + esc(x.name) + '</b>' +
              (local && local.toLowerCase() !== x.name.toLowerCase() ? '<div class="faint">Here: ' + esc(local) + '</div>' : '') + '</button>' +
              (open ? '<div class="hrow"><div>' + esc(x.text) + '</div><ul>' + x.names.map(function (n) {
                var mine = n[0] !== 'also' && local != null && n[1] === local;
                return '<li>' + (mine ? '<b>' : '') + esc(H.regionLabel(n[0])) + ': ' + esc(n[1]) + (mine ? ' \u2190 your region</b>' : '') + '</li>';
              }).join('') + '</ul></div>' : '');
          }).join('') + '</div>';
        }
        // blends
        if (s.blend == null) {
          var bs = d.blends.filter(function (b) { return H.matches(b.name + ' ' + b.country + ' ' + b.use, q); });
          return '<div class="muted" style="margin:6px 0">Make your own blends from spices you have. Tap one to size and adjust it.</div>' +
            (bs.length ? '<div class="card">' + bs.map(function (b) { return '<button class="hrow btnrow" data-a="h20Blend" data-v="' + d.blends.indexOf(b) + '"><b>' + esc(b.name) + '</b><div class="faint">' + esc(b.country) + ' \u00b7 ' + esc(b.use) + '</div></button>'; }).join('') + '</div>' : none);
        }
        var b = d.blends[s.blend], rows = H.build(b, s.parts, H.BATCH_TSP[s.batch], s.noSalt, s.mild), ri = 0;
        h = '<div class="card"><button class="btn ghost" data-a="h20Back">\u2190 All blends</button><h2>' + esc(b.name) + '</h2><div class="muted">' + esc(b.country) + ' \u00b7 ' + esc(b.use) + '</div>' +
          '<div class="chips" style="margin:8px 0">' + H.BATCH_LABEL.map(function (l, i) { return chip(esc(l), s.batch === i, 'h20Batch', ' data-v="' + i + '"'); }).join('') + '</div>' +
          '<label class="switchrow"><span class="grow">Salt-free</span><input type="checkbox" class="switch" data-change="h20Opt" data-k="noSalt"' + (s.noSalt ? ' checked' : '') + '></label>' +
          '<label class="switchrow"><span class="grow">Milder (half the chilli)</span><input type="checkbox" class="switch" data-change="h20Opt" data-k="mild"' + (s.mild ? ' checked' : '') + '></label>';
        b.parts.forEach(function (p, i) {
          var dropped = s.parts[i] <= 0 || (s.noSalt && p.flag === 'salt'), amt = dropped ? 'left out' : rows[ri++][0], got = h20Have(p.spice);
          h += '<div class="hspice"><span class="grow"><b>' + esc(amt) + '</b> ' + esc(p.spice) + (p.flag === 'opt' ? ' <span class="faint">(optional)</span>' : '') + (got ? ' \u2713' : '') + '</span>' +
            '<button class="iconbtn" data-a="h20Part" data-n="' + i + '" data-d="-1" aria-label="Less ' + esc(p.spice) + '">\u2212</button><button class="iconbtn" data-a="h20Part" data-n="' + i + '" data-d="1" aria-label="More ' + esc(p.spice) + '">+</button></div>';
        });
        h += '<div class="faint" style="margin-top:6px">\u2713 = in your pantry. Use \u2212 and + to change a spice\u2019s share.</div>' +
          '<div class="row wrapbtns" style="margin-top:10px"><button class="btn" data-a="h20Shop">Add missing to shopping</button><button class="btn alt" data-a="h20Copy">Copy or share</button></div></div>';
        return h;
      }
    }
  };
  function selRecipes(s) { return Object.keys(s.sel).filter(function (k) { return s.sel[k] && userRecipes.has(k); }).map(function (k) { return userRecipes.get(k); }); }
  SCREENS.prices20 = {
    full: function (s) { return topBar('Regional prices & budget', '') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function (s) {
        var r = regionId(), us = !r || r === R20.US, h = '';
        if (us) {
          var z = RP.cleanZip(pref('zip', '')), pr = priceRegion(), cov = RP.coverage(blsTable(), pr);
          h += '<div class="card"><h2>Your ZIP code</h2><div class="row"><div class="field grow"><input type="text" id="zipIn" inputmode="numeric" maxlength="10" data-enter="zipSave" value="' + esc(z) + '" placeholder="e.g. 10001" aria-label="ZIP code"></div><button class="btn" data-a="zipSave">Save</button></div>' +
            '<div class="muted" style="margin-top:8px">' + (z ? 'Region: <b>' + esc(RP.regionName(pr)) + '</b> (from the first three digits). ' : 'No ZIP: national averages. ') +
            (pr !== 'US' ? cov[1] + ' of ' + cov[0] + ' BLS-priced ingredients have a published ' + esc(RP.regionName(pr)) + ' average; the rest use the U.S. average \u00d7 a regional factor (' + blsTable().factor(pr) + ').' : cov[0] + ' ingredients have a BLS U.S. average.') + '</div>' +
            '<div class="faint" style="margin-top:6px">Only the ZIP\u2019s first digits are used, on this device. It is never sent anywhere.</div></div>';
          if (!s.rows) { var src = priceSources(); s.rows = []; if (src) src.forEach(function (v, k) { if (v.kind !== 'estimate' && v.kind !== 'estimate-factor') s.rows.push([k, v.label]); }); s.rows.sort(function (a, b) { return PR.cmpIC(a[0], b[0]); }); }
          h += '<details class="card"><summary><b>Average prices used (' + s.rows.length + ')</b></summary>' + s.rows.map(function (x) { var p = priceDefaults().get(x[0]); return '<div class="prow"><span class="grow">' + esc(cap(x[0])) + '<span class="faint">' + esc(PR.Cost.describe(currency(), p)) + ' \u00b7 ' + esc(x[1]) + '</span></span></div>'; }).join('') + '</details>';
          h += '<div class="card"><h2>Open Prices (optional)</h2><div class="muted" style="margin-bottom:8px">Look up prices people reported for one product barcode. Sends only that barcode to prices.openfoodfacts.org, and only when you tap Look up.</div>' +
            '<div class="row"><div class="field grow"><input type="text" id="opIn" inputmode="numeric" maxlength="14" value="' + esc(s.op || '') + '" placeholder="Barcode" aria-label="Barcode for Open Prices"></div><button class="btn alt" data-a="opLook">Look up</button></div>' +
            (s.opMsg ? '<div class="muted" style="margin-top:8px">' + esc(s.opMsg) + '</div>' : '') +
            (s.opRows ? s.opRows.map(function (p) { return '<div class="prow"><span class="grow">$' + p.price.toFixed(2) + '<span class="faint">' + esc([fmtDate(p.date), p.store, p.city, p.state].filter(Boolean).join(' \u00b7 ')) + (p.nearby ? ' \u00b7 your region' : '') + '</span></span></div>'; }).join('') : '') +
            '<div class="faint" style="margin-top:6px">Open Prices data: Open Food Facts contributors, ODbL. Reported prices are not used for estimates automatically; set your own price if one looks right.</div></div>';
        } else h += '<div class="banner">' + ic('info') + '<div class="grow">' + esc(R20.PRICE_NOTE_NON_US) + '</div></div>';
        h += '<div class="card"><h2>Budget mode</h2><label class="flabel">Most per serving (' + esc(currency()) + ')<input class="tin" type="number" step="0.05" min="0" data-change="v20num" data-k="budget" value="' + (budget() || '') + '"></label>' +
          '<label class="flabel">Weekly food budget (' + esc(currency()) + ', for the meal plan)<input class="tin" type="number" step="1" min="0" data-change="v20num" data-k="budgetWeek" value="' + (+pref('budgetWeek', 0) || '') + '"></label>' +
          '<label class="switchrow"><span class="grow">Only show recipes within budget<span class="faint" style="display:block">Recipes without enough prices for an estimate are kept, shown as \u201cprice unknown\u201d.</span></span><input type="checkbox" class="switch" data-change="v20sw" data-k="budgetMode"' + (pref('budgetMode', false) ? ' checked' : '') + '></label></div>';
        return h + '<div class="faint note">' + esc(RP.NOTE) + ' BLS averages: U.S. Bureau of Labor Statistics, public domain.</div>';
      }
    }
  };
  function planWeekCost(ws) {
    var t = 0, any = false;
    plan.forEach(function (e) { if (e.date < ws || e.date > D.addDays(ws, 6)) return; var r = byId(e.id); if (!r) return; var c = PR.Cost.estimate(r, e.servings, prices, priceDefaults(), FOODS); if (c.show()) { t += c.total; any = true; } });
    return any ? t : -1;
  }
  function detailExtras(r, s) {
    var h = '', al = PR.Household.alerts(r, household(), regionId());
    if (al.length) h += '<div class="banner warn">' + ic('warning') + '<div class="grow"><b>Household</b><div class="muted">' + esc(al.join(' \u00b7 ')) + '</div></div></div>';
    var d = dietsOf(r);
    if (d.length) h += '<div class="faint note diets">Fits: ' + esc(d.map(PR.Diets.name).join(' \u00b7 ')) + ' \u2014 a guide, not certification.</div>';
    h += fastLabelsHtml(r);
    h += medHtml(r);
    if (pref('fanNote', false) && r.steps.some(function (x) { return /\u00b0|degrees/i.test(x); })) h += '<div class="faint note">' + esc(R20.FAN_NOTE) + '</div>';
    h += '<div class="row wrapbtns"><button class="btn ghost small" data-a="myVersion">' + ic('add', 's') + (isUser(r) ? 'Edit' : 'Make my version') + '</button><button class="btn ghost small" data-a="shareRecipe">Share as text</button></div>';
    return h;
  }
  function v20Change(k, el) {
    if (s3Change(k, el)) return true;
    var s = topScreen();
    if (k === 'h20Opt') { var s3 = topScreen(); if (s3 && s3.type === 'helpers20') { s3[el.getAttribute('data-k')] = el.checked; renderScreen(s3, true); } return true; }
    if (k === 'v20sw') { var key = el.getAttribute('data-k'); setPref(key, el.checked); if (key === 'fastChips') pruneFastDiets(); version++; if (s) renderScreen(s, true); return true; }
    if (k === 'v20sel') { var k2 = el.getAttribute('data-k'); setPref(k2, el.value); if (k2 === 'units') setDetailUnits(null); version++; renderScreen(s, true); return true; }
    if (k === 'v20num') { var v = Math.max(0, +el.value || 0); setPref(el.getAttribute('data-k'), Math.round(v * 100) / 100); version++; return true; }
    if (k === 'hhHide') { var ps = household(), n = +el.getAttribute('data-n'); if (ps[n]) { ps[n].hide = el.checked; saveHousehold(ps); } return true; }
    if (k === 'mySel') { s.sel[el.getAttribute('data-id')] = el.checked; renderScreen(s, true); return true; }
    if (k === 'packFile') {
      var f = el.files && el.files[0]; if (!f) return true; el.value = '';
      var rd = new FileReader();
      rd.onload = function () {
        try {
          var got = PR.RecipeEditor.packParse(String(rd.result || ''), new Set(userRecipes.keys()), FOODS, Date.now());
          confirmBox('Add recipes?', got.length + (got.length === 1 ? ' recipe' : ' recipes') + ': ' + got.slice(0, 5).map(function (r) { return r.title; }).join(', ') + (got.length > 5 ? '\u2026' : ''), 'Add', function () {
            got.forEach(function (r) { userRecipes.set(r.id, r); }); saveUserRecipes(); toast('Added ' + got.length); renderScreen(topScreen(), true);
          });
        } catch (e) { toast(e.pack ? e.message : 'Not a recipe pack'); }
      };
      rd.readAsText(f); return true;
    }
    return false;
  }
  var actions20 = {
    welRegion: function (el) { applyRegion(el.getAttribute('data-v')); setPref('welcomed', true); popScreen(); refresh(); },
    welDone: function () { setPref('welcomed', true); popScreen(); refresh(); },
    region20: function () { pushScreen({ type: 'region' }); },
    regionSet: function (el) { var r = el.getAttribute('data-v'); confirmBox('Use ' + R20.name(r) + ' defaults?', 'Units, temperatures, currency, date format, week start and clock are reset for this region. Your pantry and recipes stay as they are.', 'Use', function () { applyRegion(r); renderScreen(topScreen(), false); }); },
    inv: function () { pushScreen({ type: 'inv', loc: INV.PANTRY }); },
    invLoc: function (el) { var s = topScreen(); s.loc = el.getAttribute('data-v'); s.limit = 0; renderScreen(s, true); },
    invMore: function () { var s = topScreen(); s.limit = (s.limit || PAGE) + PAGE; renderScreen(s, true); },
    invLow: function (el) { var p = pantryItem(el.getAttribute('data-key')); if (p) { p.low = !p.low; savePantry(); renderScreen(topScreen(), true); } },
    invEdit: function (el) { invEditDialog(el.getAttribute('data-key')); },
    invRestock: function () { var n = 0; INV.restock(invItems()).forEach(function (i) { if (addShopping(i.label, null, '', '')) n++; }); toast(n + ' added to shopping'); refresh(); },
    invShop: function (el) { addShopping(el.getAttribute('data-v'), null, '', ''); toast('Added to shopping'); refresh(); },
    household: function () { pushScreen({ type: 'household' }); },
    hhAdd: function () { var e = $('#hhIn'), ps = household(), nm = e ? PR.jtrim(e.value) : ''; if (!nm) return; ps.push({ name: nm, allergens: [], hide: false }); saveHousehold(PR.Household.parse(PR.Household.toList(ps))); renderScreen(topScreen(), true); },
    hhDel: function (el) { var ps = household(); ps.splice(+el.getAttribute('data-n'), 1); saveHousehold(ps); renderScreen(topScreen(), true); },
    hhToggle: function (el) { var ps = household(), p = ps[+el.getAttribute('data-n')], v = el.getAttribute('data-v'); if (!p) return; var i = p.allergens.indexOf(v); if (i >= 0) p.allergens.splice(i, 1); else p.allergens.push(v); saveHousehold(PR.Household.parse(PR.Household.toList(ps))); renderScreen(topScreen(), true); },
    myRecipes: function () { pushScreen({ type: 'myRecipes', sel: {} }); },
    helpers20: function () { pushScreen({ type: 'helpers20', tool: 'equiv', q: '', open: '', blend: null }); },
    h20Tool: function (el) { var s = topScreen(); s.tool = el.getAttribute('data-v'); s.q = ''; s.open = ''; s.blend = null; s.resetScroll = true; renderScreen(s); },
    h20Open: function (el) { var s = topScreen(), v = el.getAttribute('data-v'); s.open = s.open === v ? '' : v; renderScreen(s, true); },
    h20Blend: function (el) { var s = topScreen(), b = PR.Helpers20.data().blends[+el.getAttribute('data-v')]; if (!b) return; s.blend = +el.getAttribute('data-v'); s.parts = b.parts.map(function (p) { return p.parts; }); s.batch = 1; s.noSalt = false; s.mild = false; s.resetScroll = true; renderScreen(s); },
    h20Back: function () { var s = topScreen(); s.blend = null; renderScreen(s); },
    h20Batch: function (el) { var s = topScreen(); s.batch = +el.getAttribute('data-v'); renderScreen(s, true); },
    h20Part: function (el) { var s = topScreen(), i = +el.getAttribute('data-n'), d = +el.getAttribute('data-d'), v = s.parts[i], step = v >= 2 ? 0.5 : 0.25; s.parts[i] = Math.max(0, v + d * step); renderScreen(s, true); },
    h20Shop: function () {
      var s = topScreen(), b = PR.Helpers20.data().blends[s.blend], n = 0;
      b.parts.forEach(function (p, i) { if (s.parts[i] <= 0 || p.flag === 'opt' || (s.noSalt && p.flag === 'salt') || h20Have(p.spice)) return; if (addShopping(p.spice, null, '', b.name)) n++; });
      toast(n ? n + ' added to shopping' : 'You have everything'); refresh();
    },
    h20Copy: function () { var s = topScreen(), H = PR.Helpers20, b = H.data().blends[s.blend]; shareText(b.name, H.blendText(b, H.build(b, s.parts, H.BATCH_TSP[s.batch], s.noSalt, s.mild), H.BATCH_TSP[s.batch])); },
    edNew: function () { pushScreen({ type: 'editor', draft: PR.RecipeEditor.newDraft(), id: '' }); },
    myVersion: function () { var s = topScreen(), r = byId(s.id); if (!r) return; if (isUser(r)) pushScreen({ type: 'editor', draft: PR.RecipeEditor.draftOf(r), id: r.id }); else pushScreen({ type: 'editor', draft: PR.RecipeEditor.myVersion(r), id: '' }); },
    shareRecipe: function () { var s = topScreen(), r = byId(s.id); if (r) shareText(r.title, loc(PR.RecipeEditor.shareText(r, s.servings), true)); },
    edSave: edSave,
    edAcPick: function (el) {
      var e = $('#edIng'); if (!e) return;
      var pos = e.selectionStart, before = e.value.substring(0, pos), after = e.value.substring(pos), ls = before.lastIndexOf('\n') + 1, line = before.substring(ls), o = PR.RecipeImport.ingredientLine(line);
      var head = o && o.name ? line.substring(0, line.toLowerCase().lastIndexOf(o.name.toLowerCase())) : '';
      if (head.length > line.length) head = '';
      e.value = before.substring(0, ls) + head + el.getAttribute('data-v') + after; e.focus(); edAc();
    },
    packPick: function () { var i = $('#packIn'); if (i) i.click(); },
    packSave: function () { var s = topScreen(), list = selRecipes(s); if (!list.length) return; download('recipe-pack-' + D.today() + '.json', PR.RecipeEditor.packWrite('My recipes', list, VERSION, D.today()), 'application/json'); toast('Pack saved'); },
    cookbook: function () {
      var s = topScreen(), list = selRecipes(s); if (!list.length) return;
      var html = PR.RecipeEditor.cookbookHtml('Our family cookbook', list, ratings, D.today());
      var w = window.open('', '_blank');
      if (w && w.document) { w.document.open(); w.document.write(html); w.document.close(); } else download('family-cookbook-' + D.today() + '.html', html, 'text/html');
    },
    prices20: function () { pushScreen({ type: 'prices20' }); },
    zipSave: function () { var e = $('#zipIn'), z = RP.cleanZip(e ? e.value : ''); if (e && PR.jtrim(e.value) && !z) { toast('Use a 5-digit ZIP'); return; } setPref('zip', z); PRICE_MEMO = null; BUDGET_VER++; version++; var s = topScreen(); s.rows = null; renderScreen(s, true); toast(z ? 'ZIP saved' : 'ZIP cleared'); },
    opLook: function () {
      var s = topScreen(), e = $('#opIn'); s.op = e ? e.value : ''; var url = RP.openPricesUrl(s.op);
      if (!url) { s.opMsg = 'Type an 8 to 14 digit barcode.'; s.opRows = null; renderScreen(s, true); return; }
      if (!isOnline()) { s.opMsg = 'You are offline.'; renderScreen(s, true); return; }
      s.opMsg = 'Looking up\u2026'; s.opRows = null; renderScreen(s, true);
      fetch(url, { credentials: 'omit', referrerPolicy: 'no-referrer' }).then(function (x) { return x.ok ? x.text() : Promise.reject(new Error(String(x.status))); }).then(function (t) {
        s.opRows = RP.parseOpenPrices(t, priceRegion(), 10); s.opMsg = s.opRows.length ? 'Most recent U.S. reports' + (priceRegion() !== 'US' ? ', your region first' : '') + ':' : 'No U.S. price reports for that barcode yet.';
        renderScreen(s, true);
      }, function () { s.opMsg = 'Could not reach Open Prices.'; renderScreen(s, true); });
    }
  };
  actions20.themeGroup = function (el) { ui.themeGroup = el.getAttribute('data-v'); renderScreen(topScreen(), true); };
  actions20.cuGroups = function () { setPref('cuisineGroups', pref('cuisineGroups', true) === false); browseMemo = { k: null }; refresh(); };
  // ======================= v2.0 Stage 3: holidays, fasting labels, baking, regional sets =======================
  // Data (js/stage3-data.js) is parsed on first use by PR.Stage3; nothing here runs before the recipes have loaded.
  var S3 = PR.Stage3;
  function s3() { return S3.data(); }
  function s3Banners() { try { refreshRegion('banners'); } catch (e) { /* tab without banners */ } }
  function holTicked() {
    var t = pref('holidays', null);
    if (t && t.length !== undefined) return t;
    var reg = regionId();
    return s3().holidays.filter(function (h) { return S3.defaultOn(h, reg); }).map(function (h) { return h.id; });
  }
  function holLead() { var n = +pref('holLead', 14); return n === 7 || n === 21 ? n : 14; }
  function fastTicked() { var t = pref('fasts', []); return t && t.length !== undefined ? t : []; }
  function whenText(days) { return days === 0 ? 'today' : days === 1 ? 'tomorrow' : 'in ' + days + ' days'; }
  function holBanner() {
    if (!loaded || pref('holBanner', true) === false) return '';
    var up = S3.upcoming(s3(), holTicked(), D.today(), holLead()), dis = pref('holDismiss', []);
    up = up.filter(function (u) { return dis.indexOf(u.h.id + '@' + u.date) < 0; });
    if (!up.length) return '';
    var u = up[0], more = up.length - 1;
    return '<div class="banner" role="status">' + ic('calendar') + '<div class="grow"><b>' + esc(u.h.name) + ' ' + whenText(u.days) + '</b><div class="muted">' + esc(fmtDate(u.date)) +
      (more ? ' \u00b7 ' + plural(more, 'more holiday', 'more holidays') + ' coming up' : '') + '</div></div>' +
      '<button class="btn small" data-a="holSec" data-v="' + esc(u.h.id) + '">Recipes</button>' +
      '<button class="iconbtn" data-a="holDismiss" data-v="' + esc(u.h.id + '@' + u.date) + '" aria-label="Dismiss ' + esc(u.h.name) + ' reminder">' + ic('close', 's') + '</button></div>';
  }
  function holSection(id) {
    var h = s3().byId[id]; if (!h) return;
    var s = { type: 'list', title: h.name, sub: 'Holiday recipes', limit: 60 };
    s.items = function () { var m = matcher(); return S3.section(s3(), h, allRecipes(), 60).map(function (r) { return m.match(r); }); };
    s.top = function () {
      var d = S3.next(h, D.today());
      return '<div class="faint note">' + (d ? esc(fmtDate(d)) + ' (' + whenText(D.between(D.today(), d)) + '). ' : 'No date in the 2026\u20132030 table. ') + esc(h.note || '') +
        ' Picked by title, cuisine and ingredients; a starting point, not a rule.</div>';
    };
    pushScreen(s);
  }
  function fastList(id) {
    var f = s3().fastById[id]; if (!f) return;
    pushScreen({ type: 'list', title: f.label, sub: 'Fasting-day recipes', top: function () { return '<div class="faint note">' + esc(f.note) + ' ' + esc(S3.NOTE) + '</div>'; },
      items: function () { var m = matcher(); return allRecipes().filter(function (r) { return S3.fits(f, r); }).map(function (r) { return m.match(r); }).sort(PR.compare); } });
  }
  function fastLabelsHtml(r) {
    var t = fastTicked(); if (!t.length) return '';
    var l = S3.labels(s3(), r, t);
    return l.length ? '<div class="faint note diets">Fasting days: ' + esc(l.map(function (f) { return f.label; }).join(' \u00b7 ')) + ' \u2014 a guide; customs vary.</div>' : '';
  }
  SCREENS.buynext = {
    full: function (s) { return topBar('Buy these next', 'All suggestions') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function (s) {
        if (!s.ranked) s.ranked = PR.BuyNext.rankedFromResults(buyNextResults());
        var list = PR.BuyNext.sort(s.ranked, s.sort, prices, priceDefaults()), cheap = s.sort === 'cheapest';
        var h = '<div class="muted" style="margin:6px 0">' + plural(list.length, 'ingredient', 'ingredients') + ' would bring recipes closer. Prices are estimates.</div>' +
          '<div class="chips" role="group" aria-label="Sort">' + [['recipes', 'Most recipes'], ['cheapest', 'Cheapest']].map(function (x) { var on = (x[0] === 'cheapest') === cheap; return '<button class="chip' + (on ? ' on' : '') + '" data-a="bnSort" data-v="' + x[0] + '" aria-pressed="' + on + '" aria-label="Sort by ' + x[1].toLowerCase() + '">' + x[1] + '</button>'; }).join('') + '</div><div class="card">';
        list.slice(0, s.shown).forEach(function (o) {
          var open = s.open.has(o.key), sub = o.alone.length ? '+' + plural(o.alone.length, 'recipe', 'recipes') + ' you can make' : plural(o.closer.length, 'recipe', 'recipes') + ' one step closer';
          h += '<div class="bnitem"><button class="srow" data-a="bnToggleAll" data-key="' + esc(o.key) + '" aria-expanded="' + open + '" aria-label="' + esc(o.name + ', ' + sub + (open ? '. Tap to collapse.' : '. Tap for details.')) + '"><span class="grow"><b>' + esc(o.name) + '</b><span class="faint" style="display:block">' + esc(sub) + '</span></span>' + ic('expand', 's') + '</button>' +
            (open ? bnDetails(o.key, o.name, PR.BuyNext.why(o), o.alone.length ? o.alone : o.closer, o.alone.length ? 'Recipes it unlocks' : 'Recipes it brings within one ingredient', 'all') : '') + '</div>';
        });
        h += '</div>';
        if (list.length > s.shown) h += '<button class="btn ghost" data-a="bnMore">Show more (' + (list.length - s.shown) + ' left)</button>';
        return h;
      }
    }
  };
  // ---------- v2.1 botanical fruits and the user's category moves (same rules as Android Botany.java) ----------
  function catMoves() { return PR.Botany.sanitize(pref('catMoves', {})); }
  function setCatMove(key, cat) {
    var m = catMoves(), def = PR.Botany.defaultCategory(key);
    if (!cat || PR.Botany.categoryIndex(cat) < 0 || PR.Botany.categoryIndex(cat) === def) m.delete(key); else m.set(key, cat);
    setPref('catMoves', PR.Botany.toObject(PR.Botany.sanitize(m))); version++;
  }
  function catOf(key) { var m = catMoves(); if (m.has(key)) return m.get(key); var d = PR.Botany.defaultCategory(key); return d < 0 ? '' : C.CATEGORIES[d]; }
  function botanyDialog(key) {
    var B = PR.Botany;
    dialog({ title: key ? B.labelFor(key) : 'Glossary', html: (key ? '<p><span class="badge botbadge">\ud83c\udf45 Botanically a fruit</span></p><p>' + esc(B.fact(key) || '') + '</p>' : '') +
      '<h4>' + esc(B.TERM) + '</h4><p class="muted">' + esc(B.GLOSSARY) + '</p><p class="muted">' + esc(B.FUN_FACT) + '</p>' +
      '<div class="faint">In the ingredient list: ' + esc(B.LABELS.join(', ')) + '. Type \u201cbotanical\u201d in quick add to list them.</div>', buttons: [{ label: 'OK', primary: true }] });
  }
  function catDialog(key, label) {
    var def = PR.Botany.defaultCategory(key), cur = catOf(key);
    dialog({ title: 'Show ' + label.toLowerCase() + ' in', html: '<p class="muted">Where this ingredient shows in quick add. Your choice only; recipes are not changed.</p><div class="chips">' +
      C.CATEGORIES.map(function (c, i) { return '<button class="chip' + (c === cur ? ' on' : '') + '" data-a="catPick" data-k="' + esc(key) + '" data-v="' + esc(c) + '" aria-pressed="' + (c === cur) + '">' + esc(c) + (i === def ? ' (default)' : '') + '</button>'; }).join('') + '</div>',
      buttons: [{ label: 'Cancel' }] });
  }
  SCREENS.catmoves = {
    full: function (s) { return topBar('Ingredient categories', 'Your own sorting') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function () {
        var m = catMoves(), h = '<div class="muted" style="margin:6px 0">Tap an ingredient in your pantry, then \u201cCategory\u201d, to move it \u2013 for example tomatoes back to Vegetables.</div><div class="card">';
        if (!m.size) h += '<div class="muted">No ingredients moved.</div>';
        m.forEach(function (v, k) {
          var d = PR.Botany.defaultCategory(k);
          h += '<div class="switchrow"><span class="grow"><b>' + esc(PR.Botany.labelFor(k)) + '</b><span class="faint" style="display:block">In ' + esc(v) + (d >= 0 ? ' (default ' + esc(C.CATEGORIES[d]) + ')' : '') + '</span></span>' +
            '<button class="btn ghost small" data-a="catUndo" data-v="' + esc(k) + '" aria-label="Move ' + esc(PR.Botany.labelFor(k)) + ' back to its default category">Undo</button></div>';
        });
        h += '</div>' + (m.size ? '<button class="btn ghost" data-a="catReset">Reset all</button> ' : '') + '<button class="btn ghost" data-a="botGloss">Glossary: botanical fruit</button>';
        return h + '<div class="faint note">' + esc(PR.Botany.FUN_FACT) + '</div>';
      }
    }
  };
  // ---------- v2.0.1 medical diet guides (off by default; flags precomputed in js/data201.js) ----------
  function medGuides() { var g = pref('medGuides', []); return Array.isArray(g) ? g.filter(function (x, i, a) { return PR.MedGuides.IDS.indexOf(x) >= 0 && a.indexOf(x) === i; }) : []; }
  function medShort(g) { return g === 'renal' ? 'renal' : g === 'cardiac' ? 'cardiac' : 'diabetic'; }
  function medDisclaimer() { return '<div class="note medwarn" role="note"><b>' + esc(PR.MedGuides.DISCLAIMER) + '</b></div>'; }
  function medHtml(r) {
    var gs = medGuides(); if (!gs.length) return '';
    var M = PR.MedGuides, f = M.flags(r), h = '<div class="card medcard"><h2>' + ic('warning', 's') + ' Diet guides</h2>';
    gs.forEach(function (g) {
      var l = M.lines(r, f, g);
      h += '<div class="medg"><b>' + esc(M.name(g)) + (l.length ? ':' : ': no flagged ingredient names (amounts not checked)') + '</b>' + (l.length ? '<ul>' + l.map(function (x) { return '<li>' + esc(x) + '</li>'; }).join('') + '</ul>' : '') + '</div>';
    });
    var n = PR.Nutrition.estimate(r, FOODS);
    M.estimateLines(r, n.show() ? n.carbs : NaN, gs, M.clampSodium(+pref('medSodiumMg', 0)), M.clampCarbs(+pref('medCarbsG', 0))).forEach(function (x) { h += '<div class="faint">' + esc(x) + '</div>'; });
    return h + medDisclaimer() + '<div class="faint">Values are estimates; potassium and phosphorus data are incomplete.</div></div>';
  }
  function medList(g) {
    var M = PR.MedGuides;
    pushScreen({ type: 'list', title: 'No ' + medShort(g) + ' flags', sub: M.name(g), top: function () { return '<div class="faint note">No flagged ingredient names were found. That does not mean a recipe suits you: amounts and hidden ingredients are not checked.</div>' + medDisclaimer(); },
      items: function () { var m = matcher(); return allRecipes().filter(function (r) { return M.noFlags(M.flags(r), g); }).map(function (r) { return m.match(r); }).sort(PR.compare); } });
  }
  SCREENS.med = {
    full: function (s) { return topBar('Medical diet guides', 'Off by default') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function () {
        var M = PR.MedGuides, on = medGuides(), h = medDisclaimer() + '<div class="muted" style="margin:6px 0">Tick a guide to see, on each recipe page, ingredients that are often limited on that diet.</div><div class="card">';
        M.IDS.forEach(function (g) {
          h += '<label class="switchrow"><span class="grow"><b>' + esc(M.name(g)) + '</b><span class="faint" style="display:block">' + esc(M.RULES[g]) + '</span></span><input type="checkbox" class="switch" data-change="medPick" data-v="' + g + '"' + (on.indexOf(g) >= 0 ? ' checked' : '') + ' aria-label="' + esc(M.name(g)) + '"></label>' +
            '<button class="btn ghost small" data-a="medList" data-v="' + g + '" style="margin:0 0 8px">Recipes with no ' + medShort(g) + ' flags</button>';
        });
        h += '</div><div class="card"><h2>Your limits (optional)</h2><div class="faint">Stored only on this device and in your backups. Leave 0 for none.</div>' +
          '<label class="flabel">Sodium, mg per meal<input class="tin" type="number" inputmode="numeric" min="0" max="20000" step="1" data-change="medLim" data-k="medSodiumMg" value="' + M.clampSodium(+pref('medSodiumMg', 0)) + '"></label>' +
          '<label class="flabel">Carbs, g per meal<input class="tin" type="number" inputmode="numeric" min="0" max="1000" step="1" data-change="medLim" data-k="medCarbsG" value="' + M.clampCarbs(+pref('medCarbsG', 0)) + '"></label></div>';
        return h + '<div class="faint note">' + esc(M.DATA_NOTE) + '</div>';
      }
    }
  };
  var HOLTABS = [['cal', 'Calendar'], ['pick', 'Choose holidays'], ['fast', 'Fasting labels']];
  SCREENS.holidays = {
    full: function (s) { return topBar('Holidays & fasting', 'Works offline') + '<div class="main"><div class="wrap"><div class="hscroll">' + sregion('tabs', s) + '</div>' + sregion('body', s) + '</div></div>'; },
    regions: {
      tabs: function (s) { return '<div class="chips nowrap" role="tablist">' + HOLTABS.map(function (t) { return chip(t[1], s.tab === t[0], 'holTab', ' data-v="' + t[0] + '" role="tab"'); }).join('') + '</div>'; },
      body: function (s) {
        var d = s3(), today = D.today(), h = '';
        if (s.tab === 'cal') {
          var up = S3.upcoming(d, holTicked(), today, 365), mon = '';
          h += '<div class="muted" style="margin:6px 0">The next 12 months of the holidays you ticked. Tap one for recipes.</div>';
          if (!up.length) return h + '<div class="card muted">No holidays ticked. Open \u201cChoose holidays\u201d.</div>';
          up.forEach(function (u) {
            var m = u.date.slice(0, 7);
            if (m !== mon) { if (mon) h += '</div>'; mon = m; h += '<div class="hgroup">' + esc(D.MONTH_SHORT[+m.slice(5) - 1] + ' ' + m.slice(0, 4)) + '</div><div class="card">'; }
            h += '<button class="hrow btnrow" data-a="holSec" data-v="' + esc(u.h.id) + '"><b>' + esc(u.h.name) + '</b><div class="faint">' + esc(fmtDate(u.date)) + ' \u00b7 ' + whenText(u.days) + '</div></button>';
          });
          return h + '</div><div class="faint note">' + esc(S3.NOTE) + '</div>';
        }
        if (s.tab === 'pick') {
          var t = holTicked();
          h += '<div class="card"><h2>Reminder</h2><label class="switchrow"><span class="grow">Banner on the Pantry tab<span class="faint" style="display:block">Only for ticked holidays</span></span><input type="checkbox" class="switch" data-change="v20sw" data-k="holBanner"' + (pref('holBanner', true) !== false ? ' checked' : '') + '></label>' +
            '<div class="flabel">Show it this many days before</div><div class="chips">' + [7, 14, 21].map(function (n) { return chip(n + ' days', holLead() === n, 'holLead', ' data-v="' + n + '"'); }).join('') + '</div></div>';
          h += '<div class="card"><h2>Holidays (' + t.length + ' of ' + d.holidays.length + ' ticked)</h2><div class="row wrapbtns"><button class="btn ghost small" data-a="holAll" data-v="1">Tick all</button><button class="btn ghost small" data-a="holAll" data-v="0">Untick all</button><button class="btn ghost small" data-a="holAll" data-v="r">Region defaults</button></div>' +
            d.holidays.map(function (x) {
              var nx = S3.next(x, today);
              return '<label class="switchrow"><span class="grow">' + esc(x.name) + '<span class="faint" style="display:block">' + (nx ? esc(fmtDate(nx)) : 'date not in table') + '</span></span><input type="checkbox" class="switch" data-change="holPick" data-v="' + esc(x.id) + '"' + (t.indexOf(x.id) >= 0 ? ' checked' : '') + ' aria-label="' + esc(x.name) + '"></label>';
            }).join('') + '</div>';
          return h + '<div class="faint note">' + esc(S3.NOTE) + '</div>';
        }
        var ft = fastTicked();
        h += '<div class="muted" style="margin:6px 0">Tick the labels you want on recipe pages. Each list shows recipes whose ingredients fit the rule.</div>' +
          '<div class="card"><label class="switchrow"><span class="grow">Also show ticked labels as Pantry filter chips<span class="faint" style="display:block">Off by default</span></span><input type="checkbox" class="switch" data-change="v20sw" data-k="fastChips"' + (pref('fastChips', false) ? ' checked' : '') + '></label></div><div class="card">';
        d.fasts.forEach(function (f) {
          h += '<label class="switchrow"><span class="grow"><b>' + esc(f.label) + '</b><span class="faint" style="display:block">' + esc(f.note) + '</span></span><input type="checkbox" class="switch" data-change="fastPick" data-v="' + esc(f.id) + '"' + (ft.indexOf(f.id) >= 0 ? ' checked' : '') + ' aria-label="' + esc(f.label) + '"></label>' +
            '<button class="btn ghost small" data-a="fastList" data-v="' + esc(f.id) + '" style="margin:0 0 8px">See recipes</button>';
        });
        return h + '</div><div class="faint note">' + esc(S3.NOTE) + ' Ramadan and Yom Kippur are holiday sections (meals around the fast), not filters.</div>';
      }
    }
  };
  // ---------- baking ----------
  var BAKE_GROUPS = [['bread', 'Breads', /\b(bread|loaf|rolls?|buns?|focaccia|bagels?|naan|pitas?|flatbreads?|brioche|challah|sourdough|baguette|ciabatta|pretzels?|tortillas?|chapati|roti|crackers?|pizza dough)\b/],
    ['cake', 'Cakes', /\b(cakes?|cupcakes?|sponge|gateau|torte|cheesecake|madeleines?)\b/],
    ['cookie', 'Cookies & bars', /\b(cookies?|biscotti|brownies?|bars|shortbread|slice|blondies?|flapjacks?|biscuits?)\b/],
    ['pie', 'Pies & tarts', /\b(pies?|tarts?|galettes?|quiche|crumble|cobbler|crisp)\b/],
    ['pastry', 'Muffins, scones & pastry', /\b(muffins?|scones?|croissants?|danish|pastry|pastries|puffs?|strudel|turnovers?|choux|eclairs?|churros|doughnuts?|donuts?)\b/]];
  function bakeGroup(r) { var t = r.title.toLowerCase(); for (var i = 0; i < BAKE_GROUPS.length; i++) if (BAKE_GROUPS[i][2].test(t)) return BAKE_GROUPS[i][0]; return 'other'; }
  function bakeRecipes() { return allRecipes().filter(function (r) { return PR.inCategory(r, 'Baking') || (PR.inCategory(r, 'Dessert') && bakeGroup(r) !== 'other' && !/no-bake/i.test(r.title)); }); }
  var BAKETABS = [['recipes', 'Recipes'], ['pct', "Baker's %"], ['pan', 'Pan size'], ['time', 'Dough timeline'], ['oven', 'Oven & altitude'], ['party', 'Party'], ['starter', 'Starter'], ['log', 'Bake log'], ['fix', 'Troubleshoot'], ['store', 'Storage']];
  function num(s, k, def) { var v = s[k]; return v === undefined || v === '' || isNaN(+v) ? def : +v; }
  function nin(k, label, s, def, step) { return '<label class="flabel">' + label + '<input class="tin" type="number" inputmode="decimal" step="' + (step || 'any') + '" min="0" data-change="bkNum" data-k="' + k + '" value="' + esc(String(num(s, k, def))) + '"></label>'; }
  function g1(x) { return x >= 100 ? String(Math.round(x)) : (Math.round(x * 10) / 10).toString(); }
  function hhmm(m) { return S3.clock(m, !!pref('clock24', false)); }
  function bakeLog() { return Store.get('bakeLog', []); }
  function starter() { return Store.get('starter', { feeds: [] }); }
  SCREENS.baking = {
    full: function (s) { return topBar('Baking', 'Calculators work offline') + '<div class="main"><div class="wrap"><div class="hscroll">' + sregion('tabs', s) + '</div>' + sregion('body', s) + '</div></div>'; },
    regions: {
      tabs: function (s) { return '<div class="chips nowrap" role="tablist">' + BAKETABS.map(function (t) { return chip(esc(t[1]), s.tab === t[0], 'bkTab', ' data-v="' + t[0] + '" role="tab"'); }).join('') + '</div>'; },
      body: function (s) {
        var d = s3(), h = '', i;
        if (s.tab === 'recipes') {
          var all = bakeRecipes(), cnt = {};
          all.forEach(function (r) { var g = bakeGroup(r); cnt[g] = (cnt[g] || 0) + 1; });
          h += '<div class="card"><h2>Baking recipes (' + all.length + ')</h2>' + BAKE_GROUPS.concat([['other', 'Other bakes']]).map(function (g) {
            return cnt[g[0]] ? '<button class="hrow btnrow" data-a="bkList" data-v="' + g[0] + '"><b>' + esc(g[1]) + '</b><div class="faint">' + plural(cnt[g[0]], 'recipe', 'recipes') + '</div></button>' : '';
          }).join('') + '<button class="hrow btnrow" data-a="bkList" data-v=""><b>All baking</b></button></div>';
          return h + '<div class="faint note">Tip: \u201cWeights where possible\u201d in Region &amp; formats shows flour in grams.</div>';
        }
        if (s.tab === 'pct') {
          var fl = num(s, 'flour', 500), hy = num(s, 'hyd', 68), sa = num(s, 'salt', 2), ye = num(s, 'yeast', 1), st = num(s, 'starter', 0), b = S3.bakers(fl, hy, sa, ye, st);
          h += '<div class="card"><h2>Baker\u2019s percentages</h2><div class="muted">Everything is a percentage of the flour weight. Hydration = water \u00f7 flour.</div>' +
            nin('flour', 'Flour (g)', s, 500) + nin('hyd', 'Hydration (%)', s, 68) + nin('salt', 'Salt (%)', s, 2) + nin('yeast', 'Instant yeast (%)', s, 1) + nin('starter', 'Sourdough starter (%)', s, 0) +
            '<div class="hrow" role="status"><b>Water ' + g1(b[0]) + ' g</b> \u00b7 salt ' + g1(b[1]) + ' g \u00b7 yeast ' + g1(b[2]) + ' g' + (st ? ' \u00b7 starter ' + g1(b[3]) + ' g' : '') + '<div class="faint">Total dough ' + g1(b[4]) + ' g</div></div></div>';
          var dw = num(s, 'dough', 900);
          h += '<div class="card"><h2>Work back from a dough weight</h2>' + nin('dough', 'Dough wanted (g)', s, 900) +
            '<div class="hrow" role="status">Use <b>' + g1(S3.flourFor(dw, hy, sa, ye, st)) + ' g flour</b> at the percentages above.</div>' +
            '<div class="faint">A typical loaf tin (900 g / 2 lb) takes about 900 g of dough. A 1:1 starter is half flour, half water; for exact hydration count those in too.</div></div>';
          return h;
        }
        if (s.tab === 'pan') {
          var un = s.unit === 'in' ? 'in' : 'cm', shp = function (k) { return s[k] || 'round'; };
          var area = function (p) { return S3.panArea(shp(p), num(s, p + 'a', p === 'f' ? (un === 'in' ? 8 : 20) : (un === 'in' ? 9 : 23)), num(s, p + 'b', un === 'in' ? 13 : 33)); };
          var side = function (p, label) {
            return '<h3>' + label + '</h3><div class="chips">' + [['round', 'Round'], ['square', 'Square'], ['rect', 'Rectangle']].map(function (o) { return chip(o[1], shp(p) === o[0], 'bkShape', ' data-p="' + p + '" data-v="' + o[0] + '"'); }).join('') + '</div>' +
              nin(p + 'a', shp(p) === 'round' ? 'Diameter (' + un + ')' : shp(p) === 'square' ? 'Side (' + un + ')' : 'Length (' + un + ')', s, p === 'f' ? (un === 'in' ? 8 : 20) : (un === 'in' ? 9 : 23)) +
              (shp(p) === 'rect' ? nin(p + 'b', 'Width (' + un + ')', s, un === 'in' ? 13 : 33) : '');
          };
          var fct = S3.panFactor(area('f'), area('t'));
          return '<div class="card"><h2>Pan converter</h2><div class="chips">' + chip('cm', un === 'cm', 'bkUnit', ' data-v="cm"') + chip('inches', un === 'in', 'bkUnit', ' data-v="in"') + '</div>' +
            side('f', 'Recipe pan') + side('t', 'Your pan') +
            '<div class="hrow" role="status"><b>Multiply the recipe by ' + (Math.round(fct * 100) / 100) + '</b><div class="faint">For the same depth. A bigger factor in a deeper pan needs a longer, slightly cooler bake; check early and often.</div></div></div>';
        }
        if (s.tab === 'time') {
          var steps = s.steps || [['Mix and rest', 20], ['First rise', 90], ['Shape and second rise', 60], ['Bake', 40], ['Cool', 60]];
          s.steps = steps;
          var ready = num(s, 'ready', 18 * 60), mins = steps.map(function (x) { return x[1]; }), start = S3.startFor(ready, mins), tl = S3.timeline(start, mins);
          h += '<div class="card"><h2>Dough timeline</h2><div class="muted">Set when you want it ready and how long each step takes; the clock times work backwards.</div>' +
            '<label class="flabel">Ready at<input class="tin" type="time" data-change="bkReady" value="' + ('0' + Math.floor(ready / 60)).slice(-2) + ':' + ('0' + ready % 60).slice(-2) + '"></label>';
          steps.forEach(function (x, j) {
            h += '<div class="hspice"><span class="grow"><b>' + esc(hhmm(tl[j])) + '</b> ' + esc(x[0]) + ' <span class="faint">' + x[1] + ' min</span></span>' +
              '<button class="iconbtn" data-a="bkStep" data-n="' + j + '" data-d="-1" aria-label="Shorter ' + esc(x[0]) + '">\u2212</button><button class="iconbtn" data-a="bkStep" data-n="' + j + '" data-d="1" aria-label="Longer ' + esc(x[0]) + '">+</button></div>';
          });
          h += '<div class="hrow"><b>Start at ' + esc(hhmm(start)) + '</b></div><div class="row wrapbtns"><button class="btn ghost small" data-a="bkPreset" data-v="yeast">Yeasted loaf</button><button class="btn ghost small" data-a="bkPreset" data-v="sour">Overnight sourdough</button><button class="btn ghost small" data-a="bkPreset" data-v="pizza">Pizza dough</button></div>' +
            '<div class="faint" style="margin-top:6px">Rises depend on the room: watch the dough, not only the clock. Timers: use the cook-mode timers on a recipe page.</div></div>';
          return h;
        }
        if (s.tab === 'oven') {
          var c = num(s, 'oc', 180), ft = num(s, 'alt', 0), row = S3.altitudeFor(s3(), ft), gm = S3.gasMark(c);
          h += '<div class="card"><h2>Oven helper</h2>' + nin('oc', 'Oven temperature (\u00b0C)', s, 180, 5) +
            '<div class="hrow" role="status"><b>' + Math.round(c) + ' \u00b0C = ' + Math.round(S3.cToF(c)) + ' \u00b0F' + (gm ? ' = gas ' + gm : '') + '</b><div class="faint">Fan / convection: about ' + Math.round(c - 20) + ' \u00b0C (' + Math.round(S3.cToF(c - 20)) + ' \u00b0F). Dark tins bake faster; glass is slower to heat.</div></div>' +
            nin('of', 'Or enter \u00b0F', s, Math.round(S3.cToF(c)), 5) + '</div>';
          h += '<div class="card"><h2>High altitude</h2>' + nin('alt', 'Your elevation (feet)', s, 0, 100) + '<div class="faint">Metres \u00d7 3.28 = feet.</div>' +
            (row ? '<div class="hrow" role="status"><b>' + esc(row[0]) + '\u2013' + esc(row[1]) + ' ft</b><ul><li>Baking powder or soda: ' + esc(row[2]) + ' per teaspoon</li><li>Sugar: ' + esc(row[3]) + ' per cup</li><li>Liquid: ' + esc(row[4]) + ' per cup</li><li>Oven: ' + esc(row[5]) + '</li><li>Time: ' + esc(row[6]) + '</li></ul><div class="faint">' + esc(row[7] || '') + '</div></div>'
              : '<div class="hrow">Below 3,000 ft most recipes work as written.</div>') + '<div class="faint">Start with the smaller change and adjust after a test bake.</div></div>';
          return h;
        }
        if (s.tab === 'party') {
          var gu = Math.max(1, Math.round(num(s, 'guests', 12)));
          h += '<div class="card"><h2>Party calculator</h2>' + nin('guests', 'Guests', s, 12, 1) + d.party.map(function (p) {
            return '<div class="hrow"><b>' + S3.partyNeed(p, gu) + ' ' + esc(p[2]) + '</b> \u00b7 ' + esc(p[0]) + (p[3] ? '<div class="faint">' + esc(p[3]) + '</div>' : '') + '</div>';
          }).join('') + '<div class="faint">If you serve several bakes, make less of each.</div></div>';
          return h;
        }
        if (s.tab === 'starter') {
          var stt = starter(), last = stt.feeds.length ? stt.feeds[stt.feeds.length - 1] : null;
          h += '<div class="card"><h2>Sourdough starter</h2>' + (last ? '<div class="hrow"><b>Last fed ' + esc(fmtDate(last.d)) + ' ' + esc(hhmm(last.m)) + '</b>' + (last.r ? '<div class="faint">' + esc(last.r) + '</div>' : '') + '</div>' : '<div class="muted">No feeds logged yet.</div>') +
            '<label class="flabel">Feed ratio<select class="tin" id="stRatio" aria-label="Feed ratio"><option>1:1:1</option><option>1:2:2</option><option>1:5:5</option></select></label>' +
            '<label class="flabel">Note (optional)<input class="tin" type="text" id="stNote" maxlength="80" placeholder="e.g. doubled in 5 h"></label>' +
            '<button class="btn" data-a="stFeed">Log a feed now</button>' +
            (stt.feeds.length ? '<h3>Recent</h3>' + stt.feeds.slice(-10).reverse().map(function (f) { return '<div class="hrow">' + esc(fmtDate(f.d)) + ' ' + esc(hhmm(f.m)) + ' \u00b7 ' + esc(f.ratio) + (f.r ? ' \u00b7 ' + esc(f.r) : '') + '</div>'; }).join('') + '<button class="btn ghost small" data-a="stClear">Clear history</button>' : '') +
            '<div class="faint" style="margin-top:6px">Ratio is starter : flour : water by weight. At room temperature feed once or twice a day; in the fridge about once a week. Saved on this device only.</div></div>';
          return h;
        }
        if (s.tab === 'log') {
          var lg = bakeLog();
          h += '<div class="card"><h2>Bake log</h2><label class="flabel">What did you bake?<input class="tin" type="text" id="blWhat" maxlength="80"></label>' +
            '<label class="flabel">Notes (oven, timing, what to change)<input class="tin" type="text" id="blNote" maxlength="200"></label>' +
            '<div class="flabel">How did it go?</div><div class="chips">' + [1, 2, 3, 4, 5].map(function (n) { return chip(n + '\u2605', s.stars === n, 'blStar', ' data-v="' + n + '" aria-label="' + n + ' stars"'); }).join('') + '</div>' +
            '<button class="btn" data-a="blAdd">Save to log</button></div>';
          h += lg.length ? '<div class="card">' + lg.slice().reverse().slice(0, 100).map(function (e, j) {
            return '<div class="hspice"><span class="grow"><b>' + esc(e.what) + '</b> ' + (e.stars ? e.stars + '\u2605' : '') + '<div class="faint">' + esc(fmtDate(e.d)) + (e.note ? ' \u00b7 ' + esc(e.note) : '') + '</div></span><button class="iconbtn" data-a="blDel" data-n="' + (lg.length - 1 - j) + '" aria-label="Delete ' + esc(e.what) + '">' + ic('close', 's') + '</button></div>';
          }).join('') + '</div>' : '<div class="card muted">Nothing logged yet. Saved on this device only.</div>';
          return h;
        }
        if (s.tab === 'fix') {
          return '<div class="card">' + d.trouble.map(function (t) {
            var open = s.open === t.problem;
            return '<button class="hrow btnrow" data-a="bkOpen" data-v="' + esc(t.problem) + '" aria-expanded="' + open + '"><b>' + esc(t.problem) + '</b><div class="faint">' + esc(t.bakes) + '</div></button>' +
              (open ? '<div class="hrow"><ul>' + t.fixes.map(function (f) { return '<li>' + esc(f) + '</li>'; }).join('') + '</ul></div>' : '');
          }).join('') + '</div>';
        }
        h = '<div class="card"><h2>How long bakes keep</h2>';
        for (i = 0; i < d.storage.length; i++) { var r = d.storage[i]; h += '<div class="hrow"><b>' + esc(r[0]) + '</b><div>Room: ' + esc(r[1]) + ' \u00b7 Fridge: ' + esc(r[2]) + ' \u00b7 Freezer: ' + esc(r[3]) + '</div>' + (r[4] ? '<div class="faint">' + esc(r[4]) + '</div>' : '') + '</div>'; }
        return h + '<div class="faint">Wrap well and label with the date. When in doubt, throw it out.</div></div>';
      }
    }
  };
  var BK_PRESETS = { yeast: [['Mix and knead', 20], ['First rise', 90], ['Shape and second rise', 60], ['Bake', 40], ['Cool', 60]],
    sour: [['Mix and autolyse', 60], ['Bulk rise with folds', 300], ['Shape', 20], ['Cold proof in fridge', 720], ['Bake', 45], ['Cool', 90]],
    pizza: [['Mix and knead', 15], ['First rise', 120], ['Divide and rest', 30], ['Stretch, top and bake', 15]] };
  // ---------- regional sets ----------
  SCREENS.regional = {
    full: function (s) { return topBar('Regional sets', 'Recipes by country') + '<div class="main"><div class="wrap">' + sregion('body', s) + '</div></div>'; },
    regions: {
      body: function () {
        var by = {}, cnt = {};
        allRecipes().forEach(function (r) { var c = PR.jtrim(r.cuisine); if (!c) return; cnt[c] = (cnt[c] || 0) + 1; });
        Object.keys(cnt).forEach(function (c) { var g = PR.Cuisines.group(c); (by[g] = by[g] || []).push(c); });
        var order = PR.Cuisines.GROUPS.concat(['International']).filter(function (g, i, a) { return by[g] && a.indexOf(g) === i; });
        return order.map(function (g) {
          var list = by[g].sort(PR.cmpIC), tot = list.reduce(function (a, c) { return a + cnt[c]; }, 0);
          return '<div class="hgroup">' + esc(g) + ' \u00b7 ' + tot + '</div><div class="chips" style="margin-bottom:8px">' + list.map(function (c) { return chip(esc(c) + ' <span class="faint">' + cnt[c] + '</span>', false, 'browse', ' data-kind="a" data-v="' + esc(c) + '"'); }).join('') + '</div>';
        }).join('') + '<div class="faint note">Each recipe is labelled with one country of origin; dishes shared by several countries use the most common one.</div>';
      }
    }
  };
  function s3Change(k, el) {
    var s = topScreen();
    if (k === 'holPick') { var t = holTicked().slice(), id = el.getAttribute('data-v'), i = t.indexOf(id); if (el.checked && i < 0) t.push(id); if (!el.checked && i >= 0) t.splice(i, 1); setPref('holidays', t); s3Banners(); renderScreen(s, true); return true; }
    if (k === 'medPick') { var mg = medGuides(), gid = el.getAttribute('data-v'), gi = mg.indexOf(gid); if (el.checked && gi < 0) mg.push(gid); if (!el.checked && gi >= 0) mg.splice(gi, 1); setPref('medGuides', mg); version++; return true; }
    if (k === 'medLim') { var lk = el.getAttribute('data-k'), lv = lk === 'medSodiumMg' ? PR.MedGuides.clampSodium(+el.value) : PR.MedGuides.clampCarbs(+el.value); setPref(lk, lv); el.value = lv; version++; toast('Limit saved'); return true; }
    if (k === 'fastPick') { var f = fastTicked().slice(), fid = el.getAttribute('data-v'), j = f.indexOf(fid); if (el.checked && j < 0) f.push(fid); if (!el.checked && j >= 0) f.splice(j, 1); setPref('fasts', f); pruneFastDiets(); version++; return true; }
    if (k === 'bkNum') { var key = el.getAttribute('data-k'); s[key] = el.value; if (key === 'of') { s.oc = Math.round(S3.fToC(+el.value || 0)); delete s.of; } renderScreen(s, true); return true; }
    if (k === 'bkReady') { var p = String(el.value || '').split(':'); if (p.length === 2) s.ready = (+p[0]) * 60 + (+p[1]); renderScreen(s, true); return true; }
    return false;
  }
  var actions30 = {
    holidays30: function () { pushScreen({ type: 'holidays', tab: 'cal' }); },
    baking30: function () { pushScreen({ type: 'baking', tab: 'recipes' }); },
    regional30: function () { pushScreen({ type: 'regional' }); },
    holTab: function (el) { var s = topScreen(); s.tab = el.getAttribute('data-v'); s.resetScroll = true; renderScreen(s); },
    holSec: function (el) { holSection(el.getAttribute('data-v')); },
    holDismiss: function (el) { var d = pref('holDismiss', []).slice(-20); d.push(el.getAttribute('data-v')); setPref('holDismiss', d); s3Banners(); },
    holLead: function (el) { setPref('holLead', +el.getAttribute('data-v')); s3Banners(); renderScreen(topScreen(), true); },
    holAll: function (el) { var v = el.getAttribute('data-v'), reg = regionId(); setPref('holidays', s3().holidays.filter(function (h) { return v === '1' || (v === 'r' && S3.defaultOn(h, reg)); }).map(function (h) { return h.id; })); s3Banners(); renderScreen(topScreen(), true); },
    fastList: function (el) { fastList(el.getAttribute('data-v')); },
    medGuides201: function () { pushScreen({ type: 'med' }); },
    catMoves21: function () { pushScreen({ type: 'catmoves' }); },
    catUndo: function (el) { setCatMove(el.getAttribute('data-v'), null); renderScreen(topScreen(), true); },
    catReset: function () { setPref('catMoves', {}); version++; renderScreen(topScreen(), true); toast('Categories reset'); },
    botGloss: function () { botanyDialog(null); },
    catPick: function (el) { var k = el.getAttribute('data-k'), v = el.getAttribute('data-v'); closeDialog(); setCatMove(k, v); toast(PR.Botany.labelFor(k) + ' shows in ' + v); refresh(); },
    medList: function (el) { medList(el.getAttribute('data-v')); },
    bkTab: function (el) { var s = topScreen(); s.tab = el.getAttribute('data-v'); s.open = ''; s.resetScroll = true; renderScreen(s); },
    bkList: function (el) {
      var g = el.getAttribute('data-v'), name = g ? BAKE_GROUPS.concat([['other', 'Other bakes']]).filter(function (x) { return x[0] === g; })[0][1] : 'All baking';
      pushScreen({ type: 'list', title: name, sub: 'Baking', items: function () { var m = matcher(); return bakeRecipes().filter(function (r) { return !g || bakeGroup(r) === g; }).map(function (r) { return m.match(r); }).sort(PR.compare); } });
    },
    bkShape: function (el) { var s = topScreen(); s[el.getAttribute('data-p')] = el.getAttribute('data-v'); renderScreen(s, true); },
    bkUnit: function (el) { var s = topScreen(); s.unit = el.getAttribute('data-v'); ['fa', 'fb', 'ta', 'tb'].forEach(function (k) { delete s[k]; }); renderScreen(s, true); },
    bkStep: function (el) { var s = topScreen(), st = s.steps[+el.getAttribute('data-n')], d = +el.getAttribute('data-d'), step = st[1] >= 120 ? 30 : st[1] >= 30 ? 10 : 5; st[1] = Math.max(5, st[1] + d * step); renderScreen(s, true); },
    bkPreset: function (el) { var s = topScreen(); s.steps = BK_PRESETS[el.getAttribute('data-v')].map(function (x) { return x.slice(); }); renderScreen(s, true); },
    bkOpen: function (el) { var s = topScreen(), v = el.getAttribute('data-v'); s.open = s.open === v ? '' : v; renderScreen(s, true); },
    stFeed: function () { var st = starter(), now = new Date(), r = $('#stRatio'), n = $('#stNote'); st.feeds.push({ d: D.today(), m: now.getHours() * 60 + now.getMinutes(), ratio: r ? r.value : '1:1:1', r: n ? PR.jtrim(n.value) : '' }); st.feeds = st.feeds.slice(-60); Store.set('starter', st); toast('Feed logged'); renderScreen(topScreen(), true); },
    stClear: function () { confirmBox('Clear starter history?', 'This removes the feed log on this device.', 'Clear', function () { Store.set('starter', { feeds: [] }); renderScreen(topScreen(), true); }); },
    blStar: function (el) { var s = topScreen(); s.stars = +el.getAttribute('data-v'); var w = $('#blWhat'), n = $('#blNote'); s.w = w ? w.value : ''; s.n = n ? n.value : ''; renderScreen(s, true); var w2 = $('#blWhat'), n2 = $('#blNote'); if (w2) w2.value = s.w; if (n2) n2.value = s.n; },
    blAdd: function () { var s = topScreen(), w = $('#blWhat'), n = $('#blNote'), what = w ? PR.jtrim(w.value) : ''; if (!what) { toast('Type what you baked'); return; } var lg = bakeLog(); lg.push({ d: D.today(), what: what, note: n ? PR.jtrim(n.value) : '', stars: s.stars || 0 }); Store.set('bakeLog', lg.slice(-300)); s.stars = 0; toast('Saved'); renderScreen(s, true); },
    blDel: function (el) { var lg = bakeLog(); lg.splice(+el.getAttribute('data-n'), 1); Store.set('bakeLog', lg); renderScreen(topScreen(), true); }
  };
  Object.keys(actions30).forEach(function (k) { actions20[k] = actions30[k]; });
  Object.keys(actions20).forEach(function (k) { actions[k] = actions20[k]; });
  document.addEventListener('input', function (e) { if (e.target && e.target.id === 'edIng') edAc(); });
  function maybeWelcome() {
    if (pref('welcomed', false) || regionId() || window.PR_TEST_NO_WELCOME) return;
    pushScreen({ type: 'welcome', step: 0, guess: R20.guess(navigator.language || '') });
  }

  // =====================================================================
  // Start-up
  // =====================================================================
  function loadData() {
    var E = window.PR_EMBEDDED;
    if (E) {
      // v2.0 S3: the single-file build keeps the recipes in non-executed JSON <script>s (cheap to parse as HTML);
      // v2.1: one block per chunk. Each is read once and dropped so the text is not kept alive.
      var els = RJSON_ELS; RJSON_ELS = [];
      var t0 = els.length ? els[0].textContent : E.recipes;
      E.recipes = null;
      return Promise.resolve({ recipes: t0, subs: E.subs, more: function (i) { var e = els[i]; els[i] = null; return Promise.resolve(e ? e.textContent : null); } });
    }
    return Promise.all([
      fetch('data/recipes-0.json').then(function (r) { if (!r.ok) throw new Error('recipes ' + r.status); return r.text(); }),
      fetch('data/substitutions.txt').then(function (r) { if (!r.ok) throw new Error('subs ' + r.status); return r.text(); })
    ]).then(function (x) {
      return { recipes: x[0], subs: x[1], more: function (i) { return fetch('data/recipes-' + i + '.json').then(function (r) { if (!r.ok) throw new Error('recipes ' + r.status); return r.text(); }); } };
    });
  }
  /** v2.1: appends the remaining recipe chunks after the first one is on screen (one chunk at a time, HD 8 first). */
  var complete = false;
  function loadRest(n, more) {
    var i = 1, extra = [];
    (function next() {
      if (i >= n) {
        if (extra.length) { bundled = bundled.concat(extra); bundledKeys = null; version++; }
        complete = true; window.__prCompleteAt = (window.performance && performance.now) ? performance.now() : Date.now();
        if (extra.length) refresh();
        return;
      }
      more(i++).then(function (t) {
        if (t == null) { i = n; next(); return; }
        return parseRecipesChunked(t).then(function (list) { extra = extra.concat(list); afterPaint(next); });
      }).catch(function (err) { window.__err = err && err.stack; i = n; next(); });
    })();
  }
  /** v2.0 S3: converts the bundled recipes in small slices so no single task blocks the UI for long (HD 8). */
  function parseRecipesChunked(text) {
    return new Promise(function (resolve) {
      var root = typeof text === 'string' ? JSON.parse(text) : text; text = null;
      var arr = Array.isArray(root) ? root : root.recipes, out = new Array(arr.length), i = 0;
      out.chunks = (root && root.chunks) || 1;
      (function step() {
        var end = Math.min(arr.length, i + 300);
        for (; i < end; i++) { out[i] = PR.recipeFromMap(arr[i]); arr[i] = null; }
        if (i < arr.length) setTimeout(step, 0); else resolve(out);
      })();
    });
  }
  var earlyTab = null;
  /** Tabs that can be shown and used before the recipes are parsed (they only need the pantry / list). */
  function earlyOk() { return !loaded && earlyTab === ui.tab && ui.stack.length === 0; }
  function afterPaint(f) { if (window.requestAnimationFrame) requestAnimationFrame(function () { setTimeout(f, 0); }); else setTimeout(f, 0); }
  function applyDeco() { document.documentElement.classList.toggle('deco', decoOn()); }
  var RJSON_ELS = [];
  function start() {
    // detach the embedded recipe JSON blocks before shell() rewrites the body (single-file build only)
    for (var ci = 0; ; ci++) { var el = document.getElementById('pr-recipes-' + ci); if (!el) break; if (el.parentNode) el.parentNode.removeChild(el); RJSON_ELS.push(el); }
    applyDeco();
    shell();
    renderTabs();
    $('#main').innerHTML = '<div class="loading"><span class="spin"></span> Loading recipes\u2026</div>';
    try { history.replaceState({ depth: 0 }, ''); } catch (e) { }
    if (ui.tab === 'pantry' || ui.tab === 'shopping') {
      earlyTab = ui.tab; $('#main').innerHTML = '<div class="wrap">' + VIEWS[ui.tab].full() + '</div>';
    }
    afterPaint(function () {
      Promise.all([loadData().then(function (d) { return parseRecipesChunked(d.recipes).then(function (list) { return { list: list, subs: d.subs, more: d.more, chunks: list.chunks }; }); }), Cache.open().then(Cache.all), Photos.open()]).then(function (x) {
        var d = x[0];
        bundled = d.list;
        subs = PR.Substitutions.parse(d.subs);
        x[1].sort(function (a, b) { return a.fetched - b.fetched; }).forEach(function (row) {
          try { var r = PR.recipeFromMap(JSON.parse(row.json)); online.set(r.id, r); onlineFetched.set(r.id, row.fetched || 0); } catch (e) { }
        });
        var wasEarly = earlyTab === ui.tab && ui.stack.length === 0 && $('#main .wrap');
        loaded = true; pruneFastDiets(); version++; window.__prLoadedAt = (window.performance && performance.now) ? performance.now() : Date.now();
        if (wasEarly) refresh(); else showTab(ui.tab);
        earlyTab = null;
        maybeWelcome();
        if (d.chunks > 1 && d.more) afterPaint(function () { loadRest(d.chunks, d.more); });
        else { complete = true; window.__prCompleteAt = window.__prLoadedAt; }
      }).catch(function (err) {
        window.__err = err && err.stack;
        $('#main').innerHTML = '<div class="wrap">' + emptyBox('info', 'Could not load recipes', 'Open the app from a web server (or use pantry-recipes.html). ' + String(err && err.message || err)) + '</div>';
      });
    });
    if ('serviceWorker' in navigator && !window.PR_EMBEDDED && /^https?:$/.test(location.protocol)) {
      window.addEventListener('load', function () { navigator.serviceWorker.register('sw.js').catch(function () { }); });
    }
  }
  window.PR_APP = {
    VERSION: VERSION, Store: Store, Cache: Cache, state: function () { return { loaded: loaded, complete: complete, bundled: bundled.length, online: online.size, pantry: pantry.slice(), shopping: shopping.slice(), favorites: favorites.slice(), tab: ui.tab, meals: ui.meals.slice(), incDesserts: ui.incDesserts, incSides: ui.incSides, stack: ui.stack.map(function (s) { return s.type; }) }; },
    SHORT_VERSION: SHORT_VERSION, Theme: Theme, titleHint: titleHint, decoOn: decoOn, backupText: backupText, importBackupText: function (t) { var x = importBackupText(t); refresh(); return x; },
    setUseBy: function (k, d) { setUseBy(PR.normalize(k), d); refresh(); }, setQty: function (k, q) { setQty(PR.normalize(k), q); refresh(); }, soonKeys: soonKeys, hiddenByAvoid: hiddenByAvoid,
    plan: function () { return plan.slice(); }, ratings: function () { return JSON.parse(JSON.stringify(ratings)); }, userRecipes: function () { return Array.from(userRecipes.keys()); },
    heroMeals: function () { var h = $('.hero .hmeals'); return h ? h.textContent : null; },
    addToPantry: function (l) { var ok = addToPantry(l, false); refresh(); return ok; }, showTab: showTab, openScreen: function (s) { pushScreen(s); }, openDetail: openDetail, matchAll: matchAll, filteredResults: filteredResults, mealsOf: function (id) { var r = byId(id); return r ? mealsOf(r) : null; }, net: net, parseShoppingInput: parseShoppingInput,
    v16: function () {
      var o = {}; prices.forEach(function (v, k) { o[k] = v; }); var b = {}; barcodes.forEach(function (v, k) { b[k] = v; });
      return { prices: o, leftovers: JSON.parse(JSON.stringify(leftovers)), cooked: cooked.slice(), barcodes: b, photos: Photos.all().size, photoMode: Photos.mode(), currency: currency(), leftoverDays: leftoverDays() };
    },
    setPrice: function (k, p, a, u) { var pp = PR.Cost.make(p, a, u); if (pp) { prices.set(PR.normalize(k), pp); savePrices(); version++; } return !!pp; },
    timers: function () { var c = cookScreen(); return c ? c.timers.map(function (t) { return { label: t.label, step: t.step, running: !!t.end, done: t.done, left: timerLeft(t, Date.now()) }; }) : null; },
    timerFast: function (ms) { var c = cookScreen(); if (c) c.timers.forEach(function (t) { if (t.end) t.end = Date.now() + ms; }); },
    buyNext: function () { return PR.BuyNext.fromResults(buyNextResults(), PR.BuyNext.MAX); },
    buyNextAll: function (by) { return PR.BuyNext.sort(PR.BuyNext.rankedFromResults(buyNextResults()), by, prices, priceDefaults()).map(function (o) { return o.key; }); },
    priceDefaults: function () { return priceDefaults(); },
    pantryItem: function (k) { var p = pantryItem(PR.normalize(k)); return p ? JSON.parse(JSON.stringify(p)) : null; }
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();
