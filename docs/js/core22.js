/* v2.2 core: seeded variety. Line-for-line port of core/Variety.java (checked by tests/test-core22.js vectors). ES5. */
(function (PR) {
  'use strict';
  function seed(s) { var h = 0x811C9DC5 | 0; for (var i = 0; i < s.length; i++) { h ^= s.charCodeAt(i); h = Math.imul(h, 0x01000193); } return h | 0; }
  function p2(n) { return (n < 10 ? '0' : '') + n; }
  function daySeed(y, m, d, salt) { return seed(y + '-' + p2(m) + '-' + p2(d) + '|' + salt); }
  function todaySeed(salt) { var t = new Date(); return daySeed(t.getFullYear(), t.getMonth() + 1, t.getDate(), salt); }
  function Rng(s) { this.a = s | 0; }
  Rng.prototype.nextInt = function () {
    this.a = (this.a + 0x6D2B79F5) | 0;
    var t = this.a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= (t + Math.imul(t ^ (t >>> 7), t | 61)) | 0;
    return (t ^ (t >>> 14)) | 0;
  };
  Rng.prototype.next = function (n) { return (this.nextInt() >>> 0) % n; };
  function shuffle(items, s) {
    var out = items.slice(), r = new Rng(s);
    for (var i = out.length - 1; i > 0; i--) { var j = r.next(i + 1), t = out[i]; out[i] = out[j]; out[j] = t; }
    return out;
  }
  function byId(a, b) { return a.recipe.id < b.recipe.id ? -1 : a.recipe.id > b.recipe.id ? 1 : 0; }
  function cz(r) { return r.recipe.cuisine ? String(r.recipe.cuisine).trim() : ''; }
  function picks(results, s, n) { var x = shuffle(results.slice().sort(byId), s); return x.length > n ? x.slice(0, n) : x; }
  function one(results, s, avoidId) {
    if (!results.length) return null;
    var p = picks(results, s, 2);
    return p.length > 1 && avoidId != null && p[0].recipe.id === avoidId ? p[1] : p[0];
  }
  function rotatingCuisines(results, s, k, min) {
    var n = {}, keys = [];
    results.forEach(function (r) { var c = cz(r); if (!c) return; if (!n.hasOwnProperty(c)) { n[c] = 0; keys.push(c); } n[c]++; });
    keys.sort();
    var x = shuffle(keys.filter(function (c) { return n[c] >= min; }), s);
    return x.length > k ? x.slice(0, k) : x;
  }
  function inCuisine(results, c) { return results.filter(function (r) { return cz(r) === c; }); }
  function tryNew(results, seen, s) {
    if (!results.length) return null;
    var has = typeof seen.has === 'function' ? function (id) { return seen.has(id); } : function (id) { return seen.indexOf(id) >= 0; };
    var per = {}, cs = [];
    results.forEach(function (r) { var c = cz(r); if (!per.hasOwnProperty(c)) { per[c] = [0, 0]; cs.push(c); } per[c][has(r.recipe.id) ? 0 : 1]++; });
    cs.sort(); cs = shuffle(cs, s);
    var best = null, bo = Infinity;
    cs.forEach(function (c) { var v = per[c]; if (v[1] > 0 && v[0] < bo) { bo = v[0]; best = c; } });
    var pool = best === null ? [] : results.filter(function (r) { return cz(r) === best && !has(r.recipe.id); });
    if (!pool.length) pool = results.filter(function (r) { return !has(r.recipe.id); });
    if (!pool.length) pool = results;
    return one(pool, s ^ 0x5bd1e995, null);
  }
  PR.Variety = { seed: seed, daySeed: daySeed, todaySeed: todaySeed, Rng: Rng, shuffle: shuffle, picks: picks, one: one, rotatingCuisines: rotatingCuisines, inCuisine: inCuisine, tryNew: tryNew };
})(self.PR = self.PR || {});

/* v2.2 illustrations and online sources: ports of core/Illustration.java and core/Sources.java. */
(function (PR) {
  'use strict';
  function parse(text) {
    var out = {};
    text.split('\n').forEach(function (line) {
      line = line.trim();
      if (!line || line.charAt(0) === '#') return;
      var p = line.split('|'), g = { name: p[0].trim(), bg: parseInt(p[1].trim(), 16), shapes: [] };
      p[2].split(';').forEach(function (s) {
        var t = s.trim().split(/\s+/);
        g.shapes.push({ ink: t[0].charAt(0), kind: t[1].charAt(0), v: t.slice(2).map(Number) });
      });
      out[g.name] = g;
    });
    return out;
  }
  function any(h, ws) { for (var i = 0; i < ws.length; i++) if (h.indexOf(ws[i]) >= 0) return true; return false; }
  function glyph(r) {
    var cat = r.category || '', h = ' ' + r.title.toLowerCase() + ' ';
    r.ingredients.forEach(function (i) { if (!i.optional) h += ' ' + i.key + ' '; });
    switch (cat) {
      case 'Dessert': return 'cake';
      case 'Baking': return any(h, ['cake', 'cookie', 'brownie', 'muffin', 'tart', 'pie ', 'scone']) ? 'cake' : 'bread';
      case 'Drink': return 'drink';
      case 'Sauce': return 'sauce';
      case 'Salad': return 'salad';
      case 'Soup': return 'soup';
    }
    if (any(h, ['shrimp', 'prawn', 'crab', 'lobster', 'mussel', 'clam', 'scallop', 'squid', 'octopus'])) return 'shrimp';
    if (any(h, ['chicken', 'turkey', 'duck', 'quail'])) return 'chicken';
    if (any(h, ['lamb', 'mutton', 'goat'])) return 'lamb';
    if (any(h, ['beef', 'steak', 'veal', 'brisket', 'oxtail', 'venison', 'bison'])) return 'beef';
    if (any(h, ['pork', 'bacon', 'ham ', 'sausage', 'chorizo', 'pancetta', 'prosciutto'])) return 'pork';
    if (any(h, ['fish', 'salmon', 'tuna', 'cod', 'haddock', 'trout', 'mackerel', 'sardine', 'tilapia', 'halibut', 'anchov', 'sea bass', 'snapper', 'catfish', 'pollock'])) return 'fish';
    if (any(h, ['pasta', 'spaghetti', 'noodle', 'macaroni', 'penne', 'fettuccine', 'linguine', 'lasagn', 'ravioli', 'gnocchi', 'orzo'])) return 'pasta';
    if (cat === 'Breakfast' && any(h, [' egg', 'omelet', 'frittata'])) return 'egg';
    if (any(h, [' rice ', 'risotto', 'pilaf', 'biryani', 'fried rice', 'paella'])) return 'rice';
    if (cat === 'Snack') return 'snack';
    if (cat === 'Breakfast') return any(h, ['toast', 'bread', 'pancake', 'waffle']) ? 'bread' : 'egg';
    var tg = r.tags || [];
    if (tg.indexOf('vegetarian') >= 0 || tg.indexOf('vegan') >= 0 || cat === 'Side') return 'leaf';
    return 'plate';
  }
  function hash(s) { var h = 0; for (var i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0; return h; }
  function mod(x, n) { return ((x % n) + n) % n; }
  function hsl(h, s, l) {
    var c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = l - c / 2, r, g, b;
    if (h < 60) { r = c; g = x; b = 0; } else if (h < 120) { r = x; g = c; b = 0; } else if (h < 180) { r = 0; g = c; b = x; }
    else if (h < 240) { r = 0; g = x; b = c; } else if (h < 300) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
    return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
  }
  function accent(c) { return hsl(mod(hash(c || ''), 360), 0.70, 0.88); }
  function pattern(c) { return mod(hash(c || '') >> 9, 4); }
  function hex(n) { return '#' + ('00000' + (n & 0xFFFFFF).toString(16)).slice(-6); }
  var G = null, SVG = {};
  function glyphs() { if (!G) G = parse((self.PR_DATA22 && self.PR_DATA22.illustrations) || ''); return G; }
  function f(n) { return Math.round(n * 100) / 100; }
  /** Inline SVG card (viewBox 0 0 160 100), cached per glyph + cuisine look: ~1 KB string, no network. */
  function svg(r) {
    var gn = glyph(r), ac = accent(r.cuisine), pt = pattern(r.cuisine), key = gn + '|' + pt + '|' + ac;
    if (SVG[key]) return SVG[key];
    var g = glyphs()[gn] || glyphs().plate;
    if (!g) return '';
    var bg = hex(g.bg), a = hex(ac), h = '<svg class="illo" viewBox="0 0 160 100" preserveAspectRatio="xMidYMid slice" aria-hidden="true" focusable="false"><rect width="160" height="100" fill="' + bg + '"/>';
    if (pt === 1) { h += '<g fill="#fff" fill-opacity=".22">'; for (var y = 6; y < 100; y += 17) for (var x = 6 + (Math.floor(y / 12) % 2) * 8; x < 160; x += 17) h += '<circle cx="' + x + '" cy="' + y + '" r="2.2"/>'; h += '</g>'; }
    else if (pt === 2) { h += '<g stroke="#fff" stroke-opacity=".22" stroke-width="3.6">'; for (var x2 = -100; x2 < 160; x2 += 14) h += '<line x1="' + x2 + '" y1="100" x2="' + (x2 + 100) + '" y2="0"/>'; h += '</g>'; }
    else if (pt === 3) { h += '<g stroke="#fff" stroke-opacity=".22" stroke-width="3">'; for (var y3 = 12; y3 < 100; y3 += 19) h += '<line x1="0" y1="' + y3 + '" x2="160" y2="' + y3 + '"/>'; h += '</g>'; }
    h += '<circle cx="80" cy="50" r="41" fill="' + a + '" fill-opacity=".6"/><g transform="translate(47 17) scale(2.75)">';
    g.shapes.forEach(function (s) {
      var v = s.v, col = s.ink === 'b' ? bg : s.ink === 'a' ? a : '#000', op = s.ink === 'k' ? ' fill-opacity=".6"' : '';
      if (s.kind === 'c') h += '<circle cx="' + v[0] + '" cy="' + v[1] + '" r="' + v[2] + '" fill="' + col + '"' + op + '/>';
      else if (s.kind === 'e') h += '<ellipse cx="' + v[0] + '" cy="' + v[1] + '" rx="' + v[2] + '" ry="' + v[3] + '" fill="' + col + '"' + op + '/>';
      else if (s.kind === 'r') h += '<rect x="' + v[0] + '" y="' + v[1] + '" width="' + v[2] + '" height="' + v[3] + '" rx="' + v[4] + '" fill="' + col + '"' + op + '/>';
      else if (s.kind === 'l') h += '<line x1="' + v[0] + '" y1="' + v[1] + '" x2="' + v[2] + '" y2="' + v[3] + '" stroke="' + col + '" stroke-width="' + v[4] + '" stroke-linecap="round"' + (s.ink === 'k' ? ' stroke-opacity=".6"' : '') + '/>';
      else if (s.kind === 'p') { var pts = []; for (var i = 0; i + 1 < v.length; i += 2) pts.push(f(v[i]) + ',' + f(v[i + 1])); h += '<polygon points="' + pts.join(' ') + '" fill="' + col + '"' + op + '/>'; }
    });
    h += '</g></svg>';
    return (SVG[key] = h);
  }
  PR.Illustration = { parse: parse, glyph: glyph, accent: accent, pattern: pattern, hsl: hsl, svg: svg, glyphs: glyphs, INK: 0x99000000 };

  // ---- Sources ----
  var LINKS = [
    ['wikibooks', 'Wikibooks Cookbook', 'Open (CC BY-SA)', 'https://en.wikibooks.org/w/index.php?ns102=1&search={q}'],
    ['bbcgoodfood', 'BBC Good Food', 'UK', 'https://www.bbcgoodfood.com/search?q={q}'],
    ['seriouseats', 'Serious Eats', 'US', 'https://www.seriouseats.com/search?q={q}'],
    ['allrecipes', 'Allrecipes', 'US', 'https://www.allrecipes.com/search?q={q}'],
    ['recipetineats', 'RecipeTin Eats', 'Australia', 'https://www.recipetineats.com/?s={q}'],
    ['kingarthur', 'King Arthur Baking', 'US, baking', 'https://www.kingarthurbaking.com/search?query={q}'],
    ['chefkoch', 'Chefkoch', 'Germany (German)', 'https://www.chefkoch.de/rs/s0/{q}/Rezepte.html'],
    ['marmiton', 'Marmiton', 'France (French)', 'https://www.marmiton.org/recettes/recherche.aspx?aqt={q}'],
    ['giallozafferano', 'GialloZafferano', 'Italy (Italian)', 'https://www.giallozafferano.it/ricerca-ricette/{q}/'],
    ['themealdb', 'TheMealDB', 'Open API', 'https://www.themealdb.com/browse/search/?s={q}']
  ];
  var BOOKS = [
    ['10136', 'The Book of Household Management', 'Isabella Beeton', '1861'],
    ['22114', 'A Plain Cookery Book for the Working Classes', 'Charles Elm\u00e9 Francatelli', '1852'],
    ['29519', "Nelson's Home Comforts", 'Mary Hooper', '1882'],
    ['26209', 'The Golden Age Cook Book', 'Henrietta Latham Dwight', '1898'],
    ['31534', 'Fifty-two Sunday Dinners', 'Elizabeth O. Hiller', '1913'],
    ['8542', 'Things Mother Used to Make', 'Lydia Maria Gurney', '1914'],
    ['15464', 'Foods That Will Win the War', 'C. Houston Goudiss and Alberta M. Goudiss', '1918']
  ];
  var OPEN = [
    ['TheMealDB', "Recipes and photos from TheMealDB's free API (themealdb.com). Optional; off until you turn on online recipes."],
    ['Wikibooks Cookbook', 'Search results and links to Wikibooks Cookbook pages, text under CC BY-SA 4.0 by Wikibooks contributors. Opened on Wikibooks, not copied.'],
    ['Openverse', 'Optional recipe photos found through Openverse (openverse.org), only CC0, Public Domain Mark or CC BY images, each shown with its creator and licence.'],
    ['Open Food Facts', 'Product names from Open Food Facts (openfoodfacts.org), data under the Open Database License (ODbL).'],
    ['Project Gutenberg', 'Links to public-domain cookbooks on gutenberg.org.']
  ];
  /** Same output as Java URLEncoder (UTF-8) with + as %20. */
  function enc(q) {
    return encodeURIComponent(String(q == null ? '' : q).trim()).replace(/[!'()~]/g, function (c) { return '%' + c.charCodeAt(0).toString(16).toUpperCase(); });
  }
  function linkUrl(id, q) { for (var i = 0; i < LINKS.length; i++) if (LINKS[i][0] === id) return LINKS[i][3].split('{q}').join(enc(q)); return null; }
  function wikibooksApi(q) { return 'https://en.wikibooks.org/w/api.php?action=query&list=search&srnamespace=102&srlimit=20&format=json&srsearch=' + enc(q); }
  function openverseApi(q) { return 'https://api.openverse.org/v1/images/?license=cc0,pdm,by&mature=false&page_size=12&q=' + enc(q); }
  function plain(html) {
    return String(html).replace(/<[^>]*>/g, '').split('&quot;').join('"').split('&#039;').join("'").split('&lt;').join('<').split('&gt;').join('>').split('&amp;').join('&').replace(/\s+/g, ' ').trim();
  }
  function str(m, k) { var v = m[k]; return v == null ? '' : String(v); }
  function parseWikibooks(json) {
    var o; try { o = typeof json === 'string' ? JSON.parse(json) : json; } catch (e) { return []; }
    var s = o && o.query && o.query.search;
    if (!Array.isArray(s)) return [];
    var out = [];
    s.forEach(function (m) {
      if (!m || typeof m !== 'object') return;
      var t = str(m, 'title');
      if (t.indexOf('Cookbook:') !== 0) return;
      out.push({ title: t.slice(9), snippet: plain(str(m, 'snippet')), url: 'https://en.wikibooks.org/wiki/' + enc(t.split(' ').join('_')).split('%3A').join(':').split('%2F').join('/') });
    });
    return out;
  }
  function credit(p) {
    var lic = p.license === 'cc0' ? 'CC0' : p.license === 'pdm' ? 'Public Domain Mark' : 'CC ' + p.license.toUpperCase() + (p.version ? ' ' + p.version : '');
    return 'Photo: ' + (p.title ? '\u201c' + p.title + '\u201d' : 'untitled') + (p.creator ? ' by ' + p.creator : '') + ', ' + lic + ', via Openverse' + (p.provider ? ' (' + p.provider + ')' : '');
  }
  function encodePhoto(p) { return JSON.stringify([p.thumb, p.url, p.title, p.creator, p.license, p.version, p.landing, p.provider]); }
  function decodePhoto(s) {
    if (!s) return null;
    var l; try { l = JSON.parse(s); } catch (e) { return null; }
    if (!Array.isArray(l) || l.length !== 8) return null;
    l = l.map(String);
    return { thumb: l[0], url: l[1], title: l[2], creator: l[3], license: l[4], version: l[5], landing: l[6], provider: l[7] };
  }
  function parseOpenverse(json) {
    var o; try { o = typeof json === 'string' ? JSON.parse(json) : json; } catch (e) { return []; }
    var r = o && o.results;
    if (!Array.isArray(r)) return [];
    var out = [];
    r.forEach(function (m) {
      if (!m || typeof m !== 'object') return;
      var lic = str(m, 'license').toLowerCase(), url = str(m, 'url'), th = str(m, 'thumbnail');
      if (!(lic === 'cc0' || lic === 'pdm' || lic === 'by') || url.indexOf('https://') !== 0) return;
      if (th.indexOf('https://') !== 0) th = url;
      out.push({ thumb: th, url: url, title: str(m, 'title').trim(), creator: str(m, 'creator').trim(), license: lic, version: str(m, 'license_version'), landing: str(m, 'foreign_landing_url'), provider: str(m, 'provider') });
    });
    return out;
  }
  PR.Sources = { LINKS: LINKS, BOOKS: BOOKS, OPEN: OPEN, enc: enc, linkUrl: linkUrl, bookUrl: function (id) { return 'https://www.gutenberg.org/ebooks/' + id; },
    wikibooksApi: wikibooksApi, openverseApi: openverseApi, plain: plain, parseWikibooks: parseWikibooks, parseOpenverse: parseOpenverse, credit: credit, encodePhoto: encodePhoto, decodePhoto: decodePhoto };
})(self.PR = self.PR || {});
