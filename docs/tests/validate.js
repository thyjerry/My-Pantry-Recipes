// Static checks: JSON validity, manifest, every file referenced by index.html, the manifest and the service worker exists.
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
let fails = 0, n = 0;
function check(c, m) { n++; if (!c) { fails++; console.log('FAIL ' + m); } }
const exists = p => fs.existsSync(path.join(ROOT, p));
const recipes = JSON.parse(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8')).recipes;
check(Array.isArray(recipes) && recipes.length >= 1200, 'recipes.json has >= 1200 recipes (' + recipes.length + ')');
const ids = new Set(recipes.map(r => r.id));
check(ids.size === recipes.length, 'recipe ids unique');
recipes.forEach(r => { check(r.title && Array.isArray(r.ingredients) && r.ingredients.length && Array.isArray(r.steps) && r.steps.length, 'recipe complete: ' + r.id); });
const MEALS = ['Breakfast', 'Brunch', 'Lunch', 'Dinner', 'Main dish', 'Side dish', 'Appetizer', 'Snack', 'Soup', 'Salad', 'Dessert', 'Baked goods', 'Drink', 'Sauce/Condiment'];
const mealCount = {};
recipes.forEach(r => {
  check(Array.isArray(r.meals) && r.meals.length >= 1, 'recipe has at least one meal type: ' + r.id);
  check((r.meals || []).every(m => MEALS.includes(m)), 'recipe has only known meal types: ' + r.id + ' ' + JSON.stringify(r.meals));
  check(new Set(r.meals).size === (r.meals || []).length, 'no duplicate meal types: ' + r.id);
  (r.meals || []).forEach(m => { mealCount[m] = (mealCount[m] || 0) + 1; });
});
MEALS.forEach(m => check((mealCount[m] || 0) >= 10, 'at least 10 recipes of meal type ' + m));
console.log('Meal type counts: ' + MEALS.map(m => m + '=' + (mealCount[m] || 0)).join(', '));
const man = JSON.parse(fs.readFileSync(path.join(ROOT, 'manifest.json'), 'utf8'));
check(man.name && man.short_name && man.start_url && man.display === 'standalone', 'manifest basics');
check(man.icons.some(i => i.sizes === '192x192') && man.icons.some(i => i.sizes === '512x512') && man.icons.some(i => /maskable/.test(i.purpose || '')), 'manifest icons 192/512/maskable');
man.icons.forEach(i => check(exists(i.src), 'manifest icon exists: ' + i.src));
function pngSize(p) { const b = fs.readFileSync(path.join(ROOT, p)); return [b.readUInt32BE(16), b.readUInt32BE(20)]; }
[['icons/apple-touch-icon.png', 180], ['icons/icon-192.png', 192], ['icons/icon-512.png', 512], ['icons/icon-maskable-512.png', 512], ['icons/favicon-32.png', 32]].forEach(([p, s]) => {
  const d = pngSize(p); check(d[0] === s && d[1] === s, p + ' is ' + s + 'x' + s + ' (got ' + d.join('x') + ')');
});
const html = fs.readFileSync(path.join(ROOT, 'index.html'), 'utf8');
const refs = [...html.matchAll(/(?:src|href)="([^"]+)"/g)].map(m => m[1]).filter(u => !/^(https?:|data:|#)/.test(u));
check(refs.length >= 8, 'index.html has local references (' + refs.length + ')');
refs.forEach(r => check(exists(r), 'index.html reference exists: ' + r));
['apple-mobile-web-app-capable', 'apple-mobile-web-app-status-bar-style', 'apple-mobile-web-app-title', 'viewport-fit=cover', 'theme-color', 'apple-touch-icon'].forEach(k => check(html.includes(k), 'index.html has ' + k));
const sw = fs.readFileSync(path.join(ROOT, 'sw.js'), 'utf8');
const pre = JSON.parse(sw.match(/var PRECACHE = (\[[\s\S]*?\]);/)[1].replace(/'/g, '"'));
pre.forEach(p => check(p === './' || exists(p), 'precache entry exists: ' + p));
refs.forEach(r => check(pre.includes(r), 'index.html reference is precached: ' + r));
man.icons.forEach(i => check(pre.includes(i.src), 'manifest icon is precached: ' + i.src));
// no third-party code / trackers
const JS_FILES = ['js/version.js', 'js/app.js', 'js/core.js', 'js/core15.js', 'js/core16.js', 'js/data16.js', 'js/core17.js', 'js/data17.js', 'js/import15.js', 'sw.js', 'index.html'];
const allJs = JS_FILES.map(p => fs.readFileSync(path.join(ROOT, p), 'utf8')).join('\n');
const hosts = new Set([...allJs.matchAll(/https?:\/\/([a-z0-9.-]+)/gi)].map(m => m[1].toLowerCase()));
const ALLOWED_HOSTS = ['www.themealdb.com', 'world.openfoodfacts.org', 'api.openverse.org', 'en.wikibooks.org', 'schema.org']; // schema.org is only a JSON-LD type name, never fetched
check([...hosts].every(h => ALLOWED_HOSTS.includes(h)), 'only network hosts are TheMealDB, Open Food Facts, Openverse and Wikibooks (each on user action) (' + [...hosts].join(', ') + ')');
check(/connect-src[^;]*https:\/\/world\.openfoodfacts\.org/.test(html), 'CSP allows the Open Food Facts lookup');
check(!/connect-src[^;]*\*/.test(html), 'CSP connect-src has no wildcard');
check(!/google-analytics|googletagmanager|gtag\(|facebook\.net|sentry|mixpanel|segment\.io/i.test(allJs), 'no analytics/trackers');
// ---- version: one source ----
const verSrc = fs.readFileSync(path.join(ROOT, 'js/version.js'), 'utf8');
const vm = verSrc.match(/self\.PR_VERSION\s*=\s*'(([0-9.]+(?:-[a-z0-9]+)?)-web)'/);
check(!!vm, 'js/version.js defines PR_VERSION');
const VERSION = vm ? vm[1] : '?', SHORT = vm ? vm[2] : '?';
check(VERSION === '2.3-web', 'version is 2.3-web (' + VERSION + ')');
check(/importScripts\('js\/version\.js'\)/.test(sw) && /'pr-core-' \+ self\.PR_VERSION/.test(sw), 'sw.js cache name derives from version.js');
check(!/pr-1\.4|1\.4-web/.test(sw), 'sw.js has no old hard-coded version');
const order = [...html.matchAll(/<script src="([^"]+)"/g)].map(m => m[1]);
check(JSON.stringify(order) === JSON.stringify(['js/version.js', 'js/core-data.js', 'js/icons.js', 'js/core.js', 'js/core15.js', 'js/core16.js', 'js/data16.js', 'js/core17.js', 'js/data17.js', 'js/import15.js', 'js/data20.js', 'js/helpers20.js', 'js/core20.js', 'js/stage3-data.js', 'js/core30.js', 'js/data201.js', 'js/core201.js', 'js/core21.js', 'js/data22.js', 'js/core22.js', 'js/app.js']), 'index.html script order ' + order.join(','));
const app = fs.readFileSync(path.join(ROOT, 'js/app.js'), 'utf8');
// ---- v2.1: recipe chunks (the app loads data/recipes-<n>.json; data/recipes.json stays the source for tests/tools) ----
{
  const cf = fs.readdirSync(path.join(ROOT, 'data')).filter(f => /^recipes-\d+\.json$/.test(f)).sort((a, b) => parseInt(a.slice(8)) - parseInt(b.slice(8)));
  const c0 = JSON.parse(fs.readFileSync(path.join(ROOT, 'data', 'recipes-0.json'), 'utf8'));
  check(c0.chunks === cf.length && cf.length >= 2, 'chunk 0 knows the chunk count (' + c0.chunks + ' of ' + cf.length + ')');
  const back = []; cf.forEach(f => JSON.parse(fs.readFileSync(path.join(ROOT, 'data', f), 'utf8')).recipes.forEach(r => back.push(r)));
  check(JSON.stringify(back) === JSON.stringify(recipes), 'chunks rebuild recipes.json exactly');
  check(c0.recipes.length <= 600, 'first chunk is small (' + c0.recipes.length + ')');
  cf.forEach(f => check(sw.indexOf("'data/" + f + "'") >= 0, 'sw.js precaches data/' + f));
  check(sw.indexOf("'data/recipes.json'") < 0, 'sw.js does not precache the unchunked file');
}
check(/window\.PR_VERSION/.test(app) && !/'1\.4-web'/.test(app), 'app.js reads PR_VERSION');
// ---- theming: no hard-coded light-green leftovers ----
const css = fs.readFileSync(path.join(ROOT, 'css/app.css'), 'utf8');
const rootEnd = css.indexOf('}', css.indexOf(':root'));
const after = css.slice(rootEnd + 1);
const hexes = [...after.matchAll(/#[0-9a-fA-F]{3,8}\b/g)].map(m => m[0].toLowerCase()).filter(h => h !== '#fff' && h !== '#000');
check(hexes.length === 0, 'css: no hex colours outside :root except #fff/#000 (' + hexes.join(',') + ')');
check(!/rgba?\(\s*(?:46|39|31)\s*,\s*(?:158|134|1[0-2]\d)\s*,/i.test(after), 'css: no hard-coded green rgba');
const appHex = [...app.matchAll(/['"]#([0-9a-fA-F]{6})['"]/g)].map(m => m[1].toUpperCase());
check(!appHex.some(h => ['2E9E5B', '1F7A44', '27864D', 'E8F5EC', 'F4F8F5'].includes(h)), 'app.js: no hard-coded light-green colours (' + appHex.join(',') + ')');
// ---- dist outputs ----
const dist = path.join(ROOT, 'dist');
const want = ['my-pantry-recipes-v' + SHORT + '.html', 'my-pantry-recipes-web-v' + SHORT + '.zip'];
check(fs.existsSync(dist) && JSON.stringify(fs.readdirSync(dist).sort()) === JSON.stringify(want.slice().sort()), 'dist/ holds exactly ' + want.join(', '));
check(!exists('pantry-recipes.html') && !exists('pantry-recipes-web.zip'), 'no unversioned build outputs in the project root');
const single = path.join(dist, want[0]);
if (fs.existsSync(single)) {
  const s = fs.readFileSync(single, 'utf8');
  check(fs.statSync(single).size < 25 * 1024 * 1024, 'single file < 25 MB');
  check(!/<script[^>]+src=/.test(s) && !/<link[^>]+stylesheet/.test(s), 'single file has no external scripts/styles');
  check(!/(?:src|href)="(?!data:|https:)[^"]*\.(?:js|css|png|json)"/.test(s), 'single file has no local file references');
  check(s.includes('window.PR_EMBEDDED'), 'single file embeds data');
  check(s.includes("self.PR_VERSION = '" + VERSION + "'"), 'single file carries the version');
  check(s.includes('RecipeImport') && s.includes('Palette'), 'single file includes v1.5 modules');
  check(s.includes('window.PR_DATA16') && s.includes('PR.Cost') && s.includes('PR.Barcode'), 'single file includes v1.6 modules');
  check(s.includes('window.PR_DATA17') && s.includes('PR.Units') && s.includes('PR.Methods') && s.includes('BUILD_UP_NOTE'), 'single file includes v1.7 modules (units, methods, pressure timers)');
}
console.log((n - fails) + '/' + n + ' checks passed' + (fails ? ', ' + fails + ' FAILED' : ''));
process.exit(fails ? 1 : 0);
