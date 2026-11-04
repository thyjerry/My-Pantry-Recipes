// Headless Chrome smoke test (desktop Chrome with an iPhone-sized viewport; NOT a real iPhone).
// Usage: NODE_PATH=/path/to/node_modules node tests/browser-test.js http://127.0.0.1:8765/
const puppeteer = require('puppeteer-core');
const path = require('path');
const fs = require('fs');
const ROOT = path.join(__dirname, '..');
const BASE = process.argv[2] || 'http://127.0.0.1:8765/';
let fails = 0;
function check(c, msg) { console.log((c ? 'PASS ' : 'FAIL ') + msg); if (!c) fails++; }
const DATA = JSON.parse(fs.readFileSync(path.join(ROOT, 'data/recipes.json'), 'utf8')).recipes;
const NREC = DATA.length, NDESSERT = DATA.filter(r => (r.meals || []).indexOf('Dessert') >= 0).length;
const VERSION = fs.readFileSync(path.join(ROOT, 'js/version.js'), 'utf8').match(/PR_VERSION = '([^']+)'/)[1], SHORT = VERSION.replace(/-web$/, '');
const SINGLE = path.join(ROOT, 'dist', 'my-pantry-recipes-v' + SHORT + '.html');
const sleep = ms => new Promise(r => setTimeout(r, ms));

// v2.0: older checks skip the first-run Region question; the v2.0 pass below answers it
async function np(c) { const p = await c.newPage(); await p.evaluateOnNewDocument(() => { window.PR_TEST_NO_WELCOME = true; }); return p; }
async function newPage(browser, errors, ios) {
  const page = await np(browser);
  await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
  if (ios) await page.setUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1');
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  return page;
}
const topText = pg => pg.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); return l ? l.textContent : ''; });
async function tap(page, sel) { await page.waitForSelector(sel, { timeout: 5000 }); await page.$eval(sel, el => el.click()); await sleep(150); }

(async () => {
  const browser = await puppeteer.launch({ executablePath: '/usr/bin/google-chrome', headless: 'new', args: ['--no-sandbox', '--disable-gpu'] });
  const shots = path.join(ROOT, 'screenshots');
  fs.mkdirSync(shots, { recursive: true });

  // ---------- hosted (http) version ----------
  const errors = [];
  const page = await newPage(browser, errors, true);
  // keep the run deterministic and offline-safe: block TheMealDB in this pass (graceful failure is part of the test)
  await page.setRequestInterception(true);
  page.on('request', r => { if (r.url().includes('themealdb.com')) r.abort(); else r.continue(); });
  await page.goto(BASE, { waitUntil: 'networkidle0' });
  await page.waitForFunction(() => window.PR_APP && window.PR_APP.state().loaded, { timeout: 15000 });
  { const part = await page.evaluate(() => ({ b: window.PR_APP.state().bundled, c: window.PR_APP.state().complete, t: window.__prLoadedAt }));
    check(part.b >= 300 && part.t > 0, 'v2.1: first recipe chunk usable at ' + Math.round(part.t) + ' ms with ' + part.b + ' recipes (complete=' + part.c + ')'); }
  await page.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 });
  let st = await page.evaluate(() => window.PR_APP.state());
  check(st.bundled === NREC && NREC >= 1200, 'loaded ' + NREC + ' bundled recipes (got ' + st.bundled + ')');
  check(await page.$('#ioshint') !== null, 'iOS "Add to Home Screen" hint shown for iPhone UA in a browser tab');
  const cats = await page.$$eval('.cat', e => e.length);
  check(cats === 29, '29 ingredient categories (got ' + cats + ')');
  // autocomplete
  await page.focus('#addIn');
  await page.keyboard.type('spin');
  await sleep(200);
  const ac = await page.$$eval('[data-region="ac"] button', b => b.map(x => x.textContent));
  check(ac.length > 0 && /spinach/i.test(ac[0]), 'autocomplete suggests spinach (' + ac.slice(0, 3).join(' | ') + ')');
  await tap(page, '[data-region="ac"] button');
  for (const item of ['eggs', 'onion', 'tomatoes', 'cheddar cheese', 'rice', 'chicken breast', 'garlic', 'butter', 'pasta', 'milk']) {
    await page.focus('#addIn'); await page.keyboard.type(item); await page.keyboard.press('Enter'); await sleep(60);
  }
  st = await page.evaluate(() => window.PR_APP.state());
  check(st.pantry.filter(p => !p.staple).length === 11, '11 non-staple pantry items (got ' + st.pantry.filter(p => !p.staple).length + ')');
  // grid toggles
  await tap(page, '.cat[class] > button[data-ci="1"]');
  check(await page.$$eval('.cat.open', e => e.length) >= 1, 'category accordion opens');
  await page.focus('#quickIn'); await page.keyboard.type('feta'); await sleep(300);
  check(await page.$$eval('[data-region="grid"] .chip', e => e.some(x => /feta/i.test(x.textContent))), 'quick-add filter finds feta');
  await page.$eval('#quickIn', el => el.blur());
  await page.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  await page.screenshot({ path: path.join(shots, '1-pantry.png') });

  // recipes
  await tap(page, '.tab[data-tab="recipes"]');
  await sleep(300);
  const rows = await page.$$eval('.rrow[data-id]', e => e.length);
  check(rows > 10, 'recipes list renders (' + rows + ' rows)');
  const heroTitle = await page.$eval('.hero h2', e => e.textContent);
  check(heroTitle && heroTitle !== 'Add a few ingredients to get ideas', 'hero shows best match: ' + heroTitle);
  // ranking equals core comparator
  const rankOk = await page.evaluate(() => {
    const ids = Array.from(document.querySelectorAll('.list .rrow[data-id]')).map(e => e.getAttribute('data-id'));
    const all = window.PR_APP.filteredResults().map(r => r.recipe.id);
    return ids.every((id, i) => id === all[i]);
  });
  check(rankOk, 'list order matches Matcher.compare');
  check(await page.$eval('.status', e => /TheMealDB could not|Find more|Finding/.test(e.parentNode.textContent) || true), 'status row present');
  await tap(page, '.chip[data-a="mode"][data-v="2"]');
  const allMissing12 = await page.evaluate(() => Array.from(document.querySelectorAll('.list .rrow .miss')).every(e => !/^Ready|Only needs/.test(e.textContent)));
  check(allMissing12, '"Missing 1-2" filter applied');
  await tap(page, '.chip[data-a="mode"][data-v="0"]');
  // ---- meal types + Include desserts / Include sides ----
  check(await page.$$eval('#main .mcheck input[type=checkbox]', e => e.length === 2 && e.every(x => !x.checked)), 'Recipes: 2 check boxes, both unchecked by default');
  check(await page.$$eval('#main .mealchips .chip', e => e.length) === 15, 'Recipes: "All meal types" + 14 meal chips');
  const onlyDS = id => { const m = window.PR_APP.mealsOf(id); return m.indexOf('Main dish') < 0 && (m.indexOf('Dessert') >= 0 || m.indexOf('Side dish') >= 0); };
  const defHidden = await page.evaluate(src => { const f = eval(src); return window.PR_APP.filteredResults().every(x => !f(x.recipe.id)); }, onlyDS.toString());
  check(defHidden, 'default: no dessert-only / side-only recipes in results');
  // The hero line shows only the first 3 meal types, so check the hero recipe's full meal list.
  const heroM = await page.evaluate(() => { const b = document.querySelector('.hero [data-a="open"][data-id]'); const m = b ? window.PR_APP.mealsOf(b.getAttribute('data-id')) : []; return document.querySelector('.hero .over').textContent + ' | ' + (m || []).join(', '); });
  check(/TONIGHT/.test(heroM) && /Dinner|Main dish/.test(heroM), 'hero is a dinner-ish pick: ' + heroM);
  check(/desserts\/sides hidden/.test(await page.$eval('[data-region="list"]', e => e.textContent)), 'status line shows hidden desserts/sides count');
  const n0 = await page.evaluate(() => window.PR_APP.filteredResults().length);
  await tap(page, '#main input[data-change="incDesserts"]');
  const n1 = await page.evaluate(() => window.PR_APP.filteredResults().length);
  const hasD = await page.evaluate(() => window.PR_APP.filteredResults().some(x => { const m = window.PR_APP.mealsOf(x.recipe.id); return m.indexOf('Dessert') >= 0 && m.indexOf('Main dish') < 0; }));
  check(n1 > n0 && hasD, 'Include desserts adds desserts (' + n0 + ' -> ' + n1 + ')');
  await tap(page, '#main input[data-change="incSides"]');
  const n2 = await page.evaluate(() => window.PR_APP.filteredResults().length);
  check(n2 > n1, 'Include sides adds side dishes (' + n1 + ' -> ' + n2 + ')');
  await tap(page, '#main input[data-change="incDesserts"]'); await tap(page, '#main input[data-change="incSides"]');
  check(await page.evaluate(() => window.PR_APP.filteredResults().length) === n0, 'unticking restores the default results');
  await tap(page, '#main .mealchips .chip[data-v="Dessert"]');
  const dSel = await page.evaluate(() => ({ n: window.PR_APP.filteredResults().length, all: window.PR_APP.filteredResults().every(x => window.PR_APP.mealsOf(x.recipe.id).indexOf('Dessert') >= 0), over: document.querySelector('.hero .over').textContent }));
  check(dSel.n > 0 && dSel.all && /DESSERT/.test(dSel.over), 'Dessert chip shows desserts even with Include desserts off (' + dSel.n + ', "' + dSel.over + '")');
  await tap(page, '#main .mealchips .chip[data-v="Dessert"]');
  await tap(page, '#main .mealchips .chip[data-v="Side dish"]');
  const sSel = await page.evaluate(() => ({ n: window.PR_APP.filteredResults().length, all: window.PR_APP.filteredResults().every(x => window.PR_APP.mealsOf(x.recipe.id).indexOf('Side dish') >= 0) }));
  check(sSel.n > 0 && sSel.all, 'Side dish chip shows side dishes (' + sSel.n + ')');
  await tap(page, '#main .mealchips .chip[data-a="mealAll"]');
  await tap(page, '#main .mealchips .chip[data-v="Breakfast"]');
  const bSel = await page.evaluate(() => ({ all: window.PR_APP.filteredResults().every(x => window.PR_APP.mealsOf(x.recipe.id).indexOf('Breakfast') >= 0), over: document.querySelector('.hero .over').textContent }));
  check(bSel.all && /BREAKFAST/.test(bSel.over), 'Breakfast chip filters results and hero: ' + bSel.over);
  check(await page.$$eval('.list .rrow .mtypes', e => e.length > 0 && e.every(x => /Breakfast/.test(x.textContent))), 'meal tags shown on recipe rows');
  await page.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  await page.screenshot({ path: path.join(shots, '2b-recipes-breakfast.png') });
  await tap(page, '#main input[data-change="incSides"]');
  await page.reload({ waitUntil: 'networkidle0' });
  await page.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
  st = await page.evaluate(() => window.PR_APP.state());
  const persisted = st.meals.join() === 'Breakfast' && st.incSides && !st.incDesserts && await page.$eval('#main input[data-change="incSides"]', e => e.checked);
  check(persisted, 'meal chips and check boxes persist across reload');
  await tap(page, '#main input[data-change="incSides"]');
  await tap(page, '#main .mealchips .chip[data-a="mealAll"]');
  st = await page.evaluate(() => window.PR_APP.state());
  check(!st.meals.length && !st.incSides, 'filters reset');
  await page.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  await page.screenshot({ path: path.join(shots, '2-recipes.png') });

  // detail
  await tap(page, '.list .rrow[data-id]');
  await page.waitForSelector('.screen .dtitle');
  const s0 = await page.$eval('.stepper b', e => +e.textContent);
  check(s0 === 4, 'default servings is 4');
  await tap(page, '.screen [data-a="serv"][data-d="1"]');
  check(await page.$eval('.stepper b', e => +e.textContent) === 5, 'servings stepper increments');
  await tap(page, '.screen [data-a="serv"][data-d="-1"]');
  check(await page.$$eval('.screen .ing', e => e.length) > 2, 'ingredient rows render');
  check(await page.$$eval('.screen .mchip.meal', e => e.length) >= 1, 'detail shows meal-type tags');
  await tap(page, '.screen [data-a="fav"]');
  st = await page.evaluate(() => window.PR_APP.state());
  check(st.favorites.length === 1, 'favorite saved');
  if (await page.$('.screen [data-a="addMissing"]')) {
    await tap(page, '.screen [data-a="addMissing"]');
    st = await page.evaluate(() => window.PR_APP.state());
    check(st.shopping.length > 0, 'missing ingredients added to shopping list (' + st.shopping.length + ')');
  }
  await page.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  await page.screenshot({ path: path.join(shots, '3-recipe-detail.png') });
  // cook mode
  await tap(page, '.screen [data-a="cook"]');
  await page.waitForSelector('.screen.cook .steptext');
  const wl = await page.$eval('.screen.cook', e => e.textContent);
  check(/Step 1 of/.test(wl), 'cook mode shows step 1');
  await sleep(300);
  const wlState = await page.evaluate(() => document.querySelector('.screen.cook .over').textContent + ' | ' + document.querySelector('.screen.cook .wlnote').textContent);
  console.log('INFO wake lock state in headless Chrome: ' + wlState);
  await tap(page, '.screen.cook [data-a="cookNext"]');
  check(/Step 2 of/.test(await page.$eval('.screen.cook', e => e.textContent)) || /Done/.test(await page.$eval('.screen.cook', e => e.textContent)), 'cook mode advances');
  await tap(page, '.screen.cook [data-a="back"]');
  await sleep(300);
  await tap(page, '.screen [data-a="back"]');
  await sleep(300);
  st = await page.evaluate(() => window.PR_APP.state());
  check(st.stack.length === 0, 'back navigation closes screens');

  // discover
  await tap(page, '.tab[data-tab="discover"]');
  await sleep(300);
  check(await page.$$eval('.dsec', e => e.length) === 4, '4 Discover carousels');
  check(await page.$$eval('[data-a="browse"]', e => e.length) > 10, 'cuisine/category chips');
  check(await page.$$eval('#main .mcheck input', e => e.length) === 2 && await page.$$eval('#main .mealchips .chip', e => e.length) === 15, 'Discover: check boxes + meal chips');
  const carOk = await page.evaluate(src => { const f = eval(src); const ids = Array.from(document.querySelectorAll('.dsec [data-id]')).map(e => e.getAttribute('data-id')); return ids.length > 0 && ids.every(id => !f(id)); }, onlyDS.toString());
  check(carOk, 'Discover carousels hide dessert-only/side-only recipes by default');
  check(await page.$$eval('[data-a="browse"][data-kind="m"]', e => e.length) === 14, 'Browse by meal type: 14 chips');
  await page.focus('#dq'); await page.keyboard.type('cake'); await page.keyboard.press('Enter'); await sleep(300);
  check(/hidden/.test(await page.$eval('[data-region="results"]', e => e.textContent)), 'Discover search notes hidden desserts for "cake"');
  await page.$eval('#dq', el => { el.value = ''; el.dispatchEvent(new Event('input', { bubbles: true })); }); await sleep(300);
  await tap(page, '[data-a="browse"][data-kind="m"][data-v="Dessert"]');
  await sleep(400);
  const dBrowse = await page.evaluate(() => { const ids = Array.from(document.querySelectorAll('.screen .rrow[data-id]')).map(e => e.getAttribute('data-id')); return { n: ids.length, all: ids.every(id => window.PR_APP.mealsOf(id).indexOf('Dessert') >= 0) }; });
  check(dBrowse.n === Math.min(NDESSERT, 120) && dBrowse.all, 'Browse by meal type "Dessert" shows the first ' + Math.min(NDESSERT, 120) + ' of ' + NDESSERT + ' desserts (' + dBrowse.n + ')');
  if (NDESSERT > 120) {
    await tap(page, '[data-a="listMore"]'); await sleep(400);
    const dAll = await page.evaluate(() => { const ids = Array.from(document.querySelectorAll('.screen .rrow[data-id]')).map(e => e.getAttribute('data-id')); return { n: ids.length, all: ids.every(id => window.PR_APP.mealsOf(id).indexOf('Dessert') >= 0) }; });
    check(dAll.n === NDESSERT && dAll.all, '"Show more" lists all ' + NDESSERT + ' bundled desserts (' + dAll.n + ')');
  }
  await page.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  await page.screenshot({ path: path.join(shots, '4b-browse-dessert.png') });
  await tap(page, '.screen [data-a="back"]'); await sleep(200);
  await page.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
  await page.screenshot({ path: path.join(shots, '4-discover.png') });
  await tap(page, '[data-a="browse"][data-kind="a"]');
  await sleep(500);
  check(await page.$('.screen') !== null, 'browse cuisine opens list (TheMealDB blocked: ' + (await page.$eval('.screen', e => (e.querySelector('.note') || {}).textContent || 'no note')) + ')');
  await tap(page, '.screen [data-a="back"]'); await sleep(200);
  // substitutions
  await tap(page, '[data-a="subs"]');
  await page.waitForSelector('.screen .scard');
  await page.focus('.screen input[data-in="subq"]'); await page.keyboard.type('buttermilk'); await sleep(400);
  check(await page.$$eval('.screen .scard', e => e.length) >= 1, 'substitution search works');
  await tap(page, '.screen [data-a="back"]'); await sleep(200);
  // shopping
  await tap(page, '.tab[data-tab="shopping"]');
  await page.focus('#shopIn'); await page.keyboard.type('2 cups flour'); await page.keyboard.press('Enter'); await sleep(150);
  st = await page.evaluate(() => window.PR_APP.state());
  check(st.shopping.some(s => s.key === 'flour' && s.amount === 2 && s.unit === 'cup'), 'shopping input parsed "2 cups flour"');
  await tap(page, '.shop .cbx');
  await tap(page, '[data-a="shopToPantry"]');
  // favorites
  await tap(page, '.tab[data-tab="recipes"]');
  await tap(page, '.chip[data-tab="favorites"]');
  check(await page.$eval('.tab.on', e => e.getAttribute('data-tab')) === 'recipes', 'v1.8: Favorites is a chip under the Recipes tab');
  check(await page.$$eval('.rrow[data-id]', e => e.length) === 1, 'favorites tab lists the saved recipe');
  // persistence across reload
  await page.reload({ waitUntil: 'networkidle0' });
  await page.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
  st = await page.evaluate(() => window.PR_APP.state());
  check(st.favorites.length === 1 && st.pantry.length > 11 && st.tab === 'favorites', 'state persists across reload');
  // service worker + offline
  const swOk = await page.evaluate(async () => { const r = await navigator.serviceWorker.ready; return !!r.active; });
  check(swOk, 'service worker registered and active');
  await page.reload({ waitUntil: 'networkidle0' }); // now controlled
  await page.setOfflineMode(true);
  await page.reload({ waitUntil: 'load' });
  await page.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 10000 }).catch(() => { });
  st = await page.evaluate(() => window.PR_APP ? window.PR_APP.state() : { bundled: 0 });
  check(st.bundled === NREC, 'works offline after first load (service worker)');
  await page.setOfflineMode(false);
  const appErrors = errors.filter(e => !/themealdb|ERR_FAILED|ERR_INTERNET_DISCONNECTED|net::/i.test(e));
  check(appErrors.length === 0, 'no console errors (hosted)' + (appErrors.length ? ': ' + appErrors.join(' / ') : '') + (errors.length ? ' [ignored network: ' + errors.length + ']' : ''));
  await page.close();

  // ---------- live TheMealDB pass (only if reachable) ----------
  {
    const e2 = [];
    const ctx = await browser.createBrowserContext();
    const p = await np(ctx);
    await p.setViewport({ width: 390, height: 844, isMobile: true, hasTouch: true });
    p.on('console', m => { if (m.type() === 'error') e2.push(m.text()); });
    p.on('pageerror', e => e2.push('pageerror: ' + e.message));
    await p.goto(BASE, { waitUntil: 'networkidle0' });
    await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
    await p.evaluate(() => { ['chicken', 'garlic', 'onion'].forEach(x => window.PR_APP.addToPantry(x)); window.PR_APP.showTab('recipes'); });
    await p.waitForFunction(() => !window.PR_APP.net.fetching && window.PR_APP.net.lastAdded >= 0, { timeout: 60000 }).catch(() => { });
    const s2 = await p.evaluate(() => ({ online: window.PR_APP.state().online, err: window.PR_APP.net.lastError }));
    console.log('INFO live TheMealDB: cached ' + s2.online + ' online recipes, error="' + s2.err + '"');
    check(s2.online > 0 || s2.err !== '', 'TheMealDB fetch either works or fails gracefully');
    if (s2.online > 0) {
      await p.evaluate(() => { window.PR_APP.showTab('discover'); });
      await sleep(400);
      await p.evaluate(() => { const b = document.querySelector('[data-a="browse"][data-kind="a"][data-v="Italian"]') || document.querySelector('[data-a="browse"][data-kind="a"]'); b.click(); });
      await p.waitForFunction(() => !window.PR_APP.net.browsing, { timeout: 60000 }).catch(() => { });
      await p.evaluate(() => { const r = document.querySelector('.screen .rrow .badge'); if (r) r.closest('.rrow').scrollIntoView(); });
      await sleep(3000);
      const imgs = await p.$$eval('.screen img', e => e.filter(i => i.complete && i.naturalWidth > 0).length);
      console.log('INFO TheMealDB photos loaded in browse list: ' + imgs);
      await p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
      await p.screenshot({ path: path.join(shots, '5-online-browse.png') });
    }
    const bad = e2.filter(e => !/net::|Failed to load resource/i.test(e));
    check(bad.length === 0, 'no console errors (live online pass)' + (bad.length ? ': ' + bad.join(' / ') : ''));
    await ctx.close();
  }

  // ---------- v1.5 features (fresh profile) ----------
  {
    const e5 = [];
    const ctx = await browser.createBrowserContext();
    const p = await np(ctx);
    await p.setViewport({ width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
    p.on('console', m => { if (m.type() === 'error') e5.push(m.text()); });
    p.on('pageerror', e => e5.push('pageerror: ' + e.message));
    await p.setRequestInterception(true);
    p.on('request', r => { if (r.url().includes('themealdb.com')) r.abort(); else r.continue(); });
    const ready = async () => { await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 }); await sleep(150); };
    const hideToast = () => p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
    const okDlg = async () => { await p.waitForSelector('#dlg .acts .btn:not(.ghost)'); await p.$eval('#dlg .acts .btn:not(.ghost)', b => b.click()); await sleep(200); };
    await p.goto(BASE, { waitUntil: 'networkidle0' });
    await ready();
    // version label + About + cache name
    const ver = await p.evaluate(() => ({ label: (document.querySelector('.top .ver') || {}).textContent, v: window.PR_APP.VERSION }));
    check(ver.label === 'v' + SHORT && ver.v === VERSION, 'header shows "' + ver.label + '", app VERSION ' + ver.v);
    await tap(p, '.top [data-a="about"]');
    const about = await topText(p);
    check(about.includes('Version ' + VERSION) && about.includes('pr-core-' + VERSION) && /Appearance/.test(about), 'Settings & About shows version, cache name and an "Appearance" entry');
    await tap(p, '.screen [data-a="back"]');
    await p.evaluate(async () => { await navigator.serviceWorker.ready; });
    await sleep(500);
    const keys = await p.evaluate(() => caches.keys());
    check(keys.includes('pr-core-' + VERSION) && !keys.some(k => /1\.4/.test(k)), 'service worker cache is pr-core-' + VERSION + ' (' + keys.join(', ') + ')');
    // pantry with use-by dates and quantities
    await p.evaluate(() => { ['eggs', 'spinach', 'onion', 'garlic', 'chicken breast', 'rice', 'tomatoes', 'butter', 'milk', 'flour'].forEach(x => window.PR_APP.addToPantry(x)); });
    const tomorrow = await p.evaluate(() => window.PR.Dates.addDays(window.PR.Dates.today(), 1));
    await p.evaluate(t => { window.PR_APP.setUseBy('spinach', t); window.PR_APP.setQty('eggs', '12'); window.PR_APP.setQty('milk', '50 ml'); }, tomorrow);
    await p.evaluate(() => window.PR_APP.showTab('pantry')); await sleep(300);
    const soon = await p.evaluate(() => { const c = document.querySelector('.card.soon'); return c ? c.textContent : ''; });
    check(/spinach/i.test(soon) && /tomorrow/i.test(soon), 'pantry "Use soon" card lists spinach (tomorrow)');
    check(await p.evaluate(() => window.PR_APP.soonKeys().some(k => /spinach/.test(k))), 'soonKeys contains spinach');
    check(await p.$$eval('.chip .cx', e => e.some(x => /12/.test(x.textContent))), 'quantity shown on the eggs chip');
    await hideToast(); await p.screenshot({ path: path.join(shots, '6-pantry-use-soon.png') });
    // use-soon boost
    await p.evaluate(() => window.PR_APP.showTab('recipes')); await sleep(400);
    const boost = await p.evaluate(() => {
      const res = window.PR_APP.filteredResults(), uses = x => x.recipe.ingredients.some(i => /spinach/i.test(i.name));
      // ranking keeps the "cook now" / "almost there" buckets; within each, recipes using a use-soon item come first
      const bucket = x => x.haveNonStaple === 0 ? 3 : x.missing.length === 0 ? 0 : x.missing.length <= 2 ? 1 : 2;
      let ordered = true, bucketFirst = {};
      for (const b of [0, 1]) { const inB = res.filter(x => bucket(x) === b); const fu = inB.findIndex(uses), ln = inB.length - 1 - inB.slice().reverse().findIndex(uses); const fn = inB.findIndex(x => !uses(x)); bucketFirst[b] = fu; if (fu >= 0 && fn >= 0 && fn < ln) ordered = false; }
      return { ordered, bucketFirst, badge: document.querySelectorAll('.list .usesoon').length };
    });
    check(boost.ordered && (boost.bucketFirst[0] === 0 || boost.bucketFirst[1] === 0) && boost.badge > 0, 'within each match bucket, recipes using spinach rank first and show a "use soon" badge (' + JSON.stringify(boost) + ')');
    // hero order: Dinner / Main dish first
    const hm = await p.evaluate(() => window.PR_APP.heroMeals());
    const toks = (hm || '').split(' \u00b7 ');
    const firstOther = toks.findIndex(t => t !== 'Dinner' && t !== 'Main dish'), lastMain = Math.max(toks.lastIndexOf('Dinner'), toks.lastIndexOf('Main dish'));
    check(hm && (lastMain < 0 || firstOther < 0 || lastMain < firstOther), 'hero meal types list Dinner/Main dish first: "' + hm + '"');
    const hoOk = await p.evaluate(() => JSON.stringify(window.PR.heroOrder(['Snack', 'Main dish', 'Lunch', 'Dinner'])));
    check(hoOk === JSON.stringify(['Dinner', 'Main dish', 'Snack', 'Lunch']) || hoOk.indexOf('["Dinner","Main dish"') === 0, 'PR.heroOrder puts Dinner, Main dish first (' + hoOk + ')');
    // detail: rating + note + have-enough + plan
    const eggId = await p.evaluate(() => { const x = window.PR_APP.filteredResults().find(x => x.recipe.ingredients.some(i => i.key === 'egg' && i.amount > 0)); return x && x.recipe.id; });
    await p.evaluate(id => window.PR_APP.openDetail(id), eggId); await sleep(300);
    check(await p.$$eval('.screen .amt', e => e.some(x => x.classList.contains('enough') || x.classList.contains('short'))), 'detail shows "you have enough"/short indicators for eggs');
    await tap(p, '.screen .star[data-v="4"]');
    await p.focus('.screen textarea.note'); await p.keyboard.type('Great with chilli'); await p.$eval('.screen textarea.note', e => e.blur()); await sleep(200);
    let rt = await p.evaluate(id => window.PR_APP.ratings()[id], eggId);
    check(rt && rt.stars === 4 && rt.note === 'Great with chilli', 'rating 4 stars + note saved (' + JSON.stringify(rt) + ')');
    check(await p.$$eval('.screen .star.on', e => e.length) === 4, '4 stars lit');
    check(/not a guarantee/i.test(await topText(p)), 'detail allergen flags carry the "not a guarantee" note');
    await tap(p, '.screen [data-a="planAdd"]'); await okDlg();
    let pl = await p.evaluate(() => window.PR_APP.plan());
    check(pl.length === 1 && pl[0].id === eggId, 'recipe added to the meal plan');
    await hideToast(); await p.screenshot({ path: path.join(shots, '7-detail-rating.png') });
    await tap(p, '.screen [data-a="back"]');
    // planner screen + combined list
    await tap(p, '.tab[data-tab="plan"]'); await sleep(300);
    check(await p.$$eval('#main .pday', e => e.length) === 7, 'v1.8: Plan tab shows 7 days');
    check(await p.$$eval('#main .pentry', e => e.length) >= 1, 'planner lists the planned recipe');
    check(JSON.stringify(await p.$$eval('#tabs .tab', e => e.map(x => x.getAttribute('data-tab')))) === '["pantry","recipes","discover","plan","shopping"]', 'v1.8: segmented tabs Pantry / Recipes / Discover / Plan / Shopping');
    await tap(p, '#main [data-a="planWeek"][data-d="7"]');
    check(await p.$$eval('#main .pentry', e => e.length) === 0 && !!(await p.$('#main [data-a="planWeek"][data-d="0"]')), 'Plan tab: next week is empty with a way back');
    await tap(p, '#main [data-a="planWeek"][data-d="0"]');
    const shopBefore = (await p.evaluate(() => window.PR_APP.state())).shopping.length;
    if (await p.$('#main [data-a="planToList"]')) await tap(p, '#main [data-a="planToList"]');
    const shopAfter = (await p.evaluate(() => window.PR_APP.state())).shopping.length;
    check(shopAfter > shopBefore, 'combined plan shopping list added (' + shopBefore + ' -> ' + shopAfter + ')');
    await hideToast(); await p.screenshot({ path: path.join(shots, '8-meal-plan.png') });
    // allergies / dislikes
    const before = await p.evaluate(() => window.PR_APP.filteredResults().length);
    await tap(p, '.top [data-a="about"]'); await tap(p, '.screen [data-a="avoid"]');
    check(/name-based|not a guarantee/i.test(await topText(p)), 'avoid screen says matching is name-based and not a guarantee');
    await tap(p, '.screen [data-a="avoidToggle"][data-v="eggs"]');
    await p.focus('#disIn'); await p.keyboard.type('spinach'); await p.keyboard.press('Enter'); await sleep(200);
    const hid = await p.evaluate(() => ({ h: window.PR_APP.hiddenByAvoid(), n: window.PR_APP.filteredResults().length, anyEgg: window.PR_APP.filteredResults().some(x => x.recipe.ingredients.some(i => /^eggs?$/i.test(i.name) || /spinach/i.test(i.name.replace(/^callaloo, or spinach$/i, '')))) }));  // callaloo is the primary; spinach is only the stated swap
    check(hid.h > 0 && hid.n < before && !hid.anyEgg, 'egg allergy + spinach dislike hide recipes (' + before + ' -> ' + hid.n + ', hidden ' + hid.h + ')');
    await tap(p, '.screen [data-a="avoidToggle"][data-v="eggs"]');
    await tap(p, '.screen [data-a="disDel"][data-v="spinach"]');
    check(await p.evaluate(() => window.PR_APP.hiddenByAvoid()) === 0, 'removing them shows the recipes again');
    await tap(p, '.screen [data-a="back"]');
    // appearance
    const getVars = () => p.evaluate(() => { const cs = getComputedStyle(document.documentElement); return { bg: cs.getPropertyValue('--bg').trim(), text: cs.getPropertyValue('--text').trim(), card: cs.getPropertyValue('--card').trim(), body: getComputedStyle(document.body).backgroundColor, dark: document.documentElement.classList.contains('dark'), meta: document.querySelector('meta[name=theme-color]').content, scheme: getComputedStyle(document.documentElement).colorScheme }; });
    const light = await getVars();
    await tap(p, '.screen [data-a="appearance"]');
    const presets = await p.$$eval('.screen [data-a="themeUse"]', e => e.map(x => x.getAttribute('data-id')));
    const chipsN = await p.$$eval('.screen [data-a="themeGroup"]', e => e.length), groupsN = await p.evaluate(() => window.PR.Palette.GROUPS.length);
    check(presets.includes('system') && presets.includes('light') && presets.includes('dark') && presets.length >= 4 && chipsN === groupsN, 'appearance lists Follow system, Light, Dark + presets of one group (' + presets.length + ' buttons, ' + chipsN + ' group chips; one group is rendered at a time for the HD8)');
    await tap(p, '.screen [data-a="themeUse"][data-id="dark"]');
    const dark = await getVars();
    check(dark.dark && dark.bg !== light.bg && dark.body !== light.body && /dark/.test(dark.scheme), 'Dark theme changes CSS variables and color-scheme (' + light.bg + ' -> ' + dark.bg + ')');
    check(dark.meta.toLowerCase() === dark.card.toLowerCase() || dark.meta.toLowerCase() !== light.meta.toLowerCase(), 'status-bar theme-color follows the theme (' + dark.meta + ')');
    // contrast of every preset, as rendered
    const contrast = await p.evaluate(() => {
      const lum = h => { const n = parseInt(h.replace('#', ''), 16), c = [n >> 16 & 255, n >> 8 & 255, n & 255].map(v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }); return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]; };
      const ratio = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
      const T = window.PR_APP.Theme, out = [], prev = T.state().active;
      window.PR.Palette.PRESETS.forEach(b => {
        T.use(b.id);
        const cs = getComputedStyle(document.documentElement), v = k => cs.getPropertyValue(k).trim();
        out.push({ id: b.id, bg: ratio(v('--text'), v('--bg')), card: ratio(v('--text'), v('--card')), t2: ratio(v('--text2'), v('--card')), btn: ratio(v('--on-fill'), v('--green')), link: ratio(v('--green-dark'), v('--card')) });
      });
      T.use(prev);
      return out;
    });
    const bad = contrast.filter(c => c.bg < 4.5 || c.card < 4.5 || c.t2 < 4.5 || c.btn < 4.5 || c.link < 4.5);
    check(contrast.length === await p.evaluate(() => window.PR.Palette.PRESETS.length) && contrast.length >= 40 && bad.length === 0, 'all ' + contrast.length + ' presets meet WCAG AA 4.5:1 for body text, secondary text, button labels and links as rendered' + (bad.length ? ' BAD: ' + JSON.stringify(bad) : ' (min ' + Math.min(...contrast.map(c => Math.min(c.bg, c.card, c.t2, c.btn, c.link))).toFixed(2) + ':1)'));
    await hideToast(); await p.screenshot({ path: path.join(shots, '9-appearance-dark.png') });
    // follow system
    await tap(p, '.screen [data-a="themeUse"][data-id="system"]');
    await p.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'dark' }]); await sleep(200);
    const sysD = await getVars();
    await p.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'light' }]); await sleep(200);
    const sysL = await getVars();
    check(sysD.dark && !sysL.dark, 'Follow system switches live with prefers-color-scheme');
    // custom theme editor
    await tap(p, '.screen [data-a="themeNew"]');
    await p.waitForSelector('.screen input.hex[data-teh="primary"]');
    const prevBefore = await p.$eval('.screen .tprev', e => e.getAttribute('style'));
    await p.$eval('.screen input.hex[data-teh="primary"]', e => { e.value = '#8E2C6F'; e.dispatchEvent(new Event('input', { bubbles: true })); });
    await p.$eval('.screen input.hex[data-teh="bg"]', e => { e.value = '#FFF6FB'; e.dispatchEvent(new Event('input', { bubbles: true })); }); await sleep(150);
    const prevAfter = await p.$eval('.screen .tprev', e => e.getAttribute('style'));
    check(prevBefore !== prevAfter && /#8E2C6F|142,\s*44,\s*111/i.test(prevAfter + (await p.$eval('.screen [data-te="primary"]', e => e.value))), 'editor live preview updates from hex input');
    check(await p.$$eval('.screen input[type=color][data-te]', e => e.length) === 5, '5 colour inputs in the editor');
    await p.$eval('.screen [data-tename]', e => { e.value = 'Plum'; });
    await tap(p, '.screen [data-a="teSave"]'); await sleep(200);
    let th = await p.evaluate(() => { const s = window.PR_APP.Theme.state(); return { active: s.active, custom: s.custom.map(c => c.name) }; });
    check(th.custom.includes('Plum') && /^custom-/.test(th.active), 'custom theme "Plum" saved and active (' + JSON.stringify(th) + ')');
    await p.evaluate(() => window.PR_APP.Theme.use('forest'));
    await hideToast(); await p.screenshot({ path: path.join(shots, '10-appearance-custom.png') });
    // persistence across reload
    await p.evaluate(() => window.PR_APP.Theme.use(window.PR_APP.Theme.state().custom[0].id));
    const cBg = (await getVars()).bg;
    await p.reload({ waitUntil: 'networkidle0' }); await ready();
    const afterReload = await getVars();
    th = await p.evaluate(() => ({ active: window.PR_APP.Theme.state().active, n: window.PR_APP.Theme.state().custom.length }));
    check(/^custom-/.test(th.active) && th.n === 1 && afterReload.bg === cBg, 'custom theme restored after reload (' + afterReload.bg + ')');
    // import a recipe from pasted text
    await tap(p, '.top [data-a="about"]'); await tap(p, '.screen [data-a="importRecipe"]');
    await p.$eval('#impText', e => { e.value = 'Weeknight Dal\nServes 3\nCook: 30 min\n\nIngredients\n1 cup red lentils\n1 onion\n2 cloves garlic\n\nMethod\nSimmer the lentils.\nFry onion and garlic and stir in.'; });
    await tap(p, '.screen [data-a="impParse"]');
    check(/Weeknight Dal/.test(await topText(p)), 'pasted recipe parsed and previewed');
    await tap(p, '.screen [data-a="impSave"]'); await sleep(300);
    check((await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const t = l && l.querySelector('.dtitle'); return t ? t.textContent : ''; })) === 'Weeknight Dal' && (await p.evaluate(() => window.PR_APP.userRecipes())).length === 1, 'imported recipe saved and opened');
    // JSON-LD import via the module
    const jl = await p.evaluate(() => window.PR.RecipeImport.parse('<script type="application/ld+json">{"@type":"Recipe","name":"LD Pancakes","recipeIngredient":["2 eggs","200 ml milk"],"recipeInstructions":[{"@type":"HowToStep","text":"Whisk."}]}<\/script>').format);
    check(jl === 'json-ld', 'JSON-LD import works in the browser');
    // backup round trip into a fresh profile
    const bk = await p.evaluate(() => window.PR_APP.backupText());
    const st1 = await p.evaluate(() => ({ s: window.PR_APP.state(), r: window.PR_APP.ratings(), pl: window.PR_APP.plan(), th: window.PR_APP.Theme.state().active, u: window.PR_APP.userRecipes() }));
    check(typeof JSON.parse(bk).format === 'number' && JSON.parse(bk).platform === 'web', 'backup is JSON with a format number');
    const ctx2 = await browser.createBrowserContext();
    const q = await np(ctx2);
    q.on('pageerror', e => e5.push('pageerror(2): ' + e.message));
    await q.setRequestInterception(true);
    q.on('request', r => { if (r.url().includes('themealdb.com')) r.abort(); else r.continue(); });
    await q.goto(BASE, { waitUntil: 'networkidle0' });
    await q.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
    {
      const V15 = JSON.parse(fs.readFileSync(path.join(ROOT, 'tests/vectors15.json'), 'utf8'));
      const jb = V15.backups.filter(x => !x[1].startsWith('ERR') && x[1].includes('"platform":"android"')).sort((a, b) => b[1].length - a[1].length)[0][1];
      const jr = await q.evaluate(t => { try { window.PR_APP.importBackupText(t); return { n: window.PR_APP.state().pantry.length, out: window.PR_APP.backupText() }; } catch (e) { return { err: e.message }; } }, jb);
      const want = JSON.parse(jb);
      check(!jr.err && jr.n === want.pantry.length && JSON.stringify(JSON.parse(jr.out).ratings) === JSON.stringify(want.ratings || {}), 'a Java-written (Android-format) backup imports in the browser (' + (jr.err || jr.n + ' pantry items') + ')');
    }
    const summary = await q.evaluate(t => window.PR_APP.importBackupText(t), bk);
    const st2 = await q.evaluate(() => ({ s: window.PR_APP.state(), r: window.PR_APP.ratings(), pl: window.PR_APP.plan(), th: window.PR_APP.Theme.state().active, u: window.PR_APP.userRecipes(), bg: getComputedStyle(document.documentElement).getPropertyValue('--bg').trim() }));
    const same = JSON.stringify(st1.s.pantry.map(x => [x.key, x.useBy || '', x.qty || ''])) === JSON.stringify(st2.s.pantry.map(x => [x.key, x.useBy || '', x.qty || ''])) &&
      JSON.stringify(st1.r) === JSON.stringify(st2.r) && JSON.stringify(st1.pl) === JSON.stringify(st2.pl) && st1.th === st2.th && JSON.stringify(st1.u) === JSON.stringify(st2.u) &&
      st1.s.shopping.length === st2.s.shopping.length && st1.s.favorites.length === st2.s.favorites.length;
    check(same && st2.bg === cBg, 'backup export -> import in a fresh profile restores pantry, use-by, qty, ratings, plan, shopping, theme, imported recipes (' + summary + ')');
    const bk2 = await q.evaluate(() => window.PR_APP.backupText());
    const strip = t => { const o = JSON.parse(t); delete o.exported; return JSON.stringify(o); };
    check(strip(bk) === strip(bk2), 'second export is identical apart from the timestamp');
    // the export button produces a download (Blob)
    await q.evaluate(() => { window.__dl = null; const o = HTMLAnchorElement.prototype.click; HTMLAnchorElement.prototype.click = function () { if (this.download) window.__dl = this.download; else o.call(this); }; });
    await q.evaluate(() => window.PR_APP.showTab('pantry'));
    await q.$eval('.top [data-a="about"]', b => b.click()); await sleep(200);
    await q.$eval('.screen [data-a="backup"]', b => b.click()); await sleep(200);
    await q.$eval('.screen [data-a="exportBackup"]', b => b.click()); await sleep(200);
    const dl = await q.evaluate(() => window.__dl);
    check(/^pantry-recipes-backup-\d{4}-\d\d-\d\d\.json$/.test(dl || ''), 'Export backup downloads ' + dl);
    // import a malformed file via the file input
    const fileIn = await q.$('#bkFile');
    const tmpBad = path.join(require('os').tmpdir(), 'pr-bad-backup.json');
    fs.writeFileSync(tmpBad, '{"not":"a backup"}');
    await fileIn.uploadFile(tmpBad); await sleep(500);
    check(/backup|read/i.test(await topText(q)) && !(await q.$('#dlg')), 'a non-backup file shows an error and changes nothing');
    await ctx2.close();
    // cook mode + dialogs in dark
    await p.evaluate(() => window.PR_APP.Theme.use('dark'));
    await p.evaluate(() => window.PR_APP.showTab('recipes')); await sleep(300);
    await hideToast(); await p.screenshot({ path: path.join(shots, '11-detail-dark.png') });
    const bad5 = e5.filter(e => !/net::|Failed to load resource|themealdb/i.test(e));
    check(bad5.length === 0, 'no console errors (v1.5 pass)' + (bad5.length ? ': ' + bad5.join(' / ') : ''));
    await ctx.close();
  }

  // ---------- v1.6 features (fresh profile) ----------
  {
    const e6 = [];
    const ctx = await browser.createBrowserContext();
    const p = await np(ctx);
    await p.setViewport({ width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
    p.on('console', m => { if (m.type() === 'error') e6.push(m.text()); });
    p.on('pageerror', e => e6.push('pageerror: ' + e.message));
    await p.setRequestInterception(true);
    let offHits = 0;
    const OFF_OK = '3017620422003';
    p.on('request', r => {
      const u = r.url();
      if (u.includes('themealdb.com')) return r.abort();
      if (u.includes('world.openfoodfacts.org')) {
        offHits++;
        const cors = { 'Access-Control-Allow-Origin': '*' };
        if (u.includes('/product/' + OFF_OK + '.json')) return r.respond({ status: 200, contentType: 'application/json', headers: cors, body: JSON.stringify({ status: 1, code: OFF_OK, product: { product_name: 'Crunchy Peanut Butter', brands: 'Test Brand', quantity: '340 g', categories_tags: ['en:spreads', 'en:peanut-butters'] } }) });
        return r.respond({ status: 404, contentType: 'application/json', headers: cors, body: '{"status":0,"status_verbose":"product not found"}' });
      }
      r.continue();
    });
    const ready = async () => { await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 }); await sleep(150); };
    const hideToast = () => p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
    const okDlg = async () => { await p.waitForSelector('#dlg .acts .btn:not(.ghost)'); await p.$eval('#dlg .acts .btn:not(.ghost)', b => b.click()); await sleep(200); };
    const mainTop = () => p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const m = l && l.querySelector('.main'); return m ? m.scrollTop : -1; });
    const setMainTop = y => p.evaluate(y => { const l = [...document.querySelectorAll('.screen')].pop(); const m = l.querySelector('.main'); m.scrollTop = y; return m.scrollTop; }, y);
    const clickIn = (sel) => p.evaluate(sel => { const l = [...document.querySelectorAll('.screen')].pop(); const b = (l || document).querySelector(sel); if (!b) return false; b.click(); return true; }, sel);
    await p.goto(BASE, { waitUntil: 'networkidle0' });
    await ready();
    check(await p.evaluate(() => window.PR_APP.VERSION) === VERSION && /v2\.1(?![-.\w])/.test(await p.$eval('.top .ver', e => e.textContent)), 'version ' + VERSION + ' with header label');

    // ---- Appearance: no jump to the top, grouped grid, font, corners, accent, random ----
    await tap(p, '.top [data-a="about"]'); await tap(p, '.screen [data-a="appearance"]');
    await clickIn('[data-a="themeGroup"][data-v="Game day"]'); await sleep(250);
    const grid = await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(), P = window.PR.Palette; return { tiles: l.querySelectorAll('.ttile').length, chips: l.querySelectorAll('[data-a="themeGroup"]').length, inG: P.inGroup('Game day').length, sum: P.GROUPS.reduce((a, g) => a + P.inGroup(g).length, 0), n: P.PRESETS.length, g: P.GROUPS.length }; });
    check(grid.tiles === grid.inG && grid.inG >= 50 && grid.chips === grid.g && grid.sum === grid.n && grid.n >= 40, 'Appearance: grouped swatch grid, ' + grid.n + ' presets in ' + grid.g + ' group chips, Game day group renders ' + grid.tiles + ' tiles');
    const scrollable = await p.evaluate(() => { const m = [...document.querySelectorAll('.screen')].pop().querySelector('.main'); return m.scrollHeight - m.clientHeight; });
    check(scrollable > 1500, 'Appearance screen scrolls (' + scrollable + ' px of scroll)');
    const tileId = await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(), m = l.querySelector('.main'), tiles = [...l.querySelectorAll('.ttile')]; const t = tiles[Math.floor(tiles.length * 0.7)]; m.scrollTop = t.offsetTop - 200; return t.getAttribute('data-id'); });
    const y0 = await mainTop();
    await clickIn('.ttile[data-id="' + tileId + '"]'); await sleep(250);
    const y1 = await mainTop();
    const act1 = await p.evaluate(() => window.PR_APP.Theme.state().active);
    check(y0 > 800 && Math.abs(y1 - y0) <= 1 && act1 === tileId, 'choosing preset "' + tileId + '" keeps the scroll position (' + y0 + ' -> ' + y1 + ')');
    check(await p.evaluate(id => !!document.querySelector('.screen .ttile.on[data-id="' + id + '"][aria-pressed="true"]'), tileId), 'chosen preset is marked current');
    await hideToast(); await p.screenshot({ path: path.join(shots, '12-appearance-grid.png') });
    let ys = [];
    for (const sel of ['[data-a="themeUse"][data-id="dark"]', '[data-a="themeFont"][data-v="large"]', '[data-a="themeCorners"][data-v="round"]', '[data-a="themeAccent"]:not([data-v="-1"])', '[data-a="themeRandom"]', '[data-a="themeUse"][data-id="light"]']) {
      await setMainTop(400); const a = await mainTop(); await clickIn(sel); await sleep(200); ys.push([sel.replace(/\[data-a="|"\]|"/g, ''), a, await mainTop()]);
    }
    check(ys.every(x => Math.abs(x[1] - x[2]) <= 1), 'mode, text size, corners, accent and random keep the scroll position (' + ys.map(x => x[1] + '->' + x[2]).join(', ') + ')');
    const vars6 = await p.evaluate(() => { const cs = getComputedStyle(document.documentElement); const h = document.querySelector('.screen h2'); return { fs: cs.getPropertyValue('--fs').trim(), rad: cs.getPropertyValue('--rad').trim(), h2: parseFloat(getComputedStyle(h).fontSize), st: window.PR_APP.Theme.state() }; });
    check(+vars6.fs > 1 && +vars6.rad > 1 && vars6.st.font === 'large' && vars6.st.corners === 'round', 'text size and corners set --fs=' + vars6.fs + ' --rad=' + vars6.rad + ' (h2 ' + vars6.h2 + 'px)');
    check(vars6.st.accentPick >= 0, 'accent quick pick stored (' + vars6.st.accentPick + ')');
    const accOk = await p.evaluate(() => {
      const P = window.PR.Palette, bad = [];
      P.PRESETS.forEach(b => P.QUICK_ACCENTS.forEach(a => { const s = P.parseState(''); s.active = b.id; s.accentPick = a; const r = s.resolve(false), c = P.derive(r); P.textPairs(c).forEach(t => { if (P.contrast(t[1], t[2]) + 1e-9 < t[3]) bad.push(b.id + '/' + P.hex(a) + ':' + t[0]); }); }));
      return bad;
    });
    check(accOk.length === 0, 'every preset x every quick accent keeps all text pairs at AA (' + (accOk.length ? accOk.slice(0, 5).join(', ') : 'all pass') + ')');
    // after a sub-screen and back the position is restored
    await setMainTop(700); const yb = await mainTop();
    await clickIn('[data-a="themeNew"]'); await sleep(250); await clickIn('[data-a="back"]'); await sleep(300);
    check(Math.abs((await mainTop()) - yb) <= 1, 'back from the theme editor returns to the same scroll position (' + yb + ' -> ' + (await mainTop()) + ')');
    await p.evaluate(() => { const T = window.PR_APP.Theme; T.set('font', 'normal'); T.set('corners', 'normal'); T.set('accentPick', -1); T.use('light'); });
    await clickIn('[data-a="back"]'); await sleep(200); await clickIn('[data-a="back"]'); await sleep(200);

    // ---- pick a recipe with nutrition + cost estimates, a gram ingredient and a timed step ----
    const pick = await p.evaluate(() => {
      const PR = window.PR, F = PR.Nutrition.parse(window.PR_DATA16.foods), DP = window.PR_APP.priceDefaults();
      const all = window.PR_APP.matchAll().map(x => x.recipe).filter(r => !PR.isOnline(r));
      for (const r of all) {
        const n = PR.Nutrition.estimate(r, F), c = PR.Cost.estimate(r, 4, new Map(), DP, F);
        const g = r.ingredients.find(i => !i.optional && i.unit === 'g' && i.amount > 0 && i.key);
        const st = r.steps.findIndex(s => PR.Durations.find(s).length > 0);
        if (n.show() && c.show() && g && st >= 0 && st < r.steps.length - 1 && r.servings === 4) return { id: r.id, title: r.title, kcal: Math.round(n.kcal), per: PR.Cost.money('$', c.perServing), g: { key: g.key, name: g.name, amount: g.amount }, step: st, first: PR.Durations.find(r.steps[st])[0].seconds };
      }
      return null;
    });
    check(!!pick, 'found a recipe with nutrition, cost, a gram ingredient and a timer (' + (pick && pick.title) + ')');
    await p.evaluate(g => { window.PR_APP.addToPantry(g.name); window.PR_APP.setQty(g.name, '5000 g'); }, pick.g);
    await p.evaluate(id => window.PR_APP.openDetail(id), pick.id); await sleep(400);
    let tt = await topText(p);
    check(/Nutrition per serving/.test(tt) && /Estimate · \d+%/.test(tt) && tt.includes(pick.kcal + 'kcal'), 'nutrition card shows "Estimate" + coverage and ' + pick.kcal + ' kcal');
    check(tt.includes(pick.per) && /per serving/.test(tt), 'cost card shows ' + pick.per + ' per serving (rough defaults)');
    // batch x2
    await clickIn('[data-a="batch"][data-k="2"]'); await sleep(200);
    check(await p.$eval('.screen .stepper b', e => +e.textContent) === 8, 'Batch ×2 scales servings to 8');
    await clickIn('[data-a="batch"][data-k="1"]'); await sleep(150);
    // prices: set a user price and a currency
    await clickIn('[data-a="pricesFor"]'); await sleep(300);
    check(/Ingredients in this recipe/.test(await topText(p)), 'prices screen opened from the recipe lists its ingredients');
    await clickIn('[data-a="priceEdit"][data-key="' + pick.g.key + '"]'); await sleep(200);
    await p.$eval('#dlg [data-dv="p"]', e => { e.value = '20'; }); await p.$eval('#dlg [data-dv="a"]', e => { e.value = '1'; }); await p.$eval('#dlg [data-dv="u"]', e => { e.value = 'kg'; });
    await okDlg();
    let v16 = await p.evaluate(() => window.PR_APP.v16());
    check(v16.prices[pick.g.key] && v16.prices[pick.g.key].price === 20 && v16.prices[pick.g.key].unit === 'kg', 'price saved for ' + pick.g.key + ' (20 per kg)');
    await p.$eval('.screen input[data-change="currency"]', e => { e.value = '€'; e.dispatchEvent(new Event('change', { bubbles: true })); }); await sleep(200);
    await p.$eval('.screen input[data-change="shopCost"]', e => { e.checked = true; e.dispatchEvent(new Event('change', { bubbles: true })); }); await sleep(200);
    check((await p.evaluate(() => window.PR_APP.v16())).currency === '€', 'currency symbol saved (€)');
    await hideToast(); await p.screenshot({ path: path.join(shots, '13-prices.png') });
    await clickIn('[data-a="back"]'); await sleep(300);
    const expCost = await p.evaluate(id => { const PR = window.PR, F = PR.Nutrition.parse(window.PR_DATA16.foods), DP = window.PR_APP.priceDefaults(), u = new Map(); const o = window.PR_APP.v16().prices; Object.keys(o).forEach(k => u.set(k, PR.Cost.make(o[k].price, o[k].amount, o[k].unit))); const r = window.PR_APP.matchAll().map(x => x.recipe).find(r => r.id === id); return PR.Cost.money('€', PR.Cost.estimate(r, 4, u, DP, F).perServing); }, pick.id);
    tt = await topText(p);
    check(tt.includes(expCost) && /from default prices \(US: BLS regional averages/.test(tt), 'cost card uses the new price and currency (' + expCost + ' per serving, Java/JS parity checked in test-core16)');
    // photo (file input, canvas downscale, IndexedDB)
    const ph = await p.$('.screen #photoIn');
    await ph.uploadFile(path.join(ROOT, 'tests/fixtures/photo-1600x1200.jpg'));
    await p.waitForFunction(() => window.PR_APP.v16().photos === 1, { timeout: 5000 }).catch(() => { });
    const phi = await p.evaluate(async () => { const im = [...document.querySelectorAll('.screen')].pop().querySelector('.band img'); if (!im) return null; await im.decode().catch(() => { }); return { src: im.src.slice(0, 23), w: im.naturalWidth, h: im.naturalHeight, mode: window.PR_APP.v16().photoMode }; });
    check(phi && phi.src === 'data:image/jpeg;base64,' && phi.w === 800 && phi.h === 600 && phi.mode === 'idb', 'photo downscaled to ' + (phi && phi.w + 'x' + phi.h) + ' JPEG and stored in IndexedDB');
    await hideToast(); await p.screenshot({ path: path.join(shots, '14-detail-estimates.png') });
    await p.evaluate(() => { const m = [...document.querySelectorAll('.screen')].pop().querySelector('.main'); const e = m.querySelector('.est'); m.scrollTop = e.offsetTop - 80; });
    await p.screenshot({ path: path.join(shots, '15-nutrition-cost.png') });
    // cooked this: log, leftovers, pantry deduction
    await clickIn('[data-a="cookedThis"]'); await sleep(200);
    await p.$eval('#dlg [data-dv="p"]', e => { e.value = '2'; }); await p.$eval('#dlg [data-dv="x"]', e => { e.checked = true; });
    await okDlg();
    v16 = await p.evaluate(() => window.PR_APP.v16());
    const today = await p.evaluate(() => window.PR.Dates.today()), exUse = await p.evaluate(t => window.PR.Dates.addDays(t, 3), today);
    check(v16.cooked.length === 1 && v16.cooked[0].recipe === pick.id && v16.cooked[0].servings === 4 && v16.cooked[0].date === today, '"Cooked this" logged date + servings');
    check(v16.leftovers.length === 1 && v16.leftovers[0].portions === 2 && v16.leftovers[0].useBy === exUse, 'leftovers: 2 portions, use by ' + exUse + ' (default 3 days)');
    const qty = await p.evaluate(k => (window.PR_APP.pantryItem(k) || {}).qty, pick.g.key), expQ = await p.evaluate(g => window.PR.Amounts.deduct('5000 g', g.key, g.amount, 'g'), pick.g);
    check(qty === expQ && qty !== '5000 g', 'pantry deduction (opt-in): 5000 g -> ' + qty);
    check(/Cooked once/.test(await topText(p)), 'cook history shown on the recipe');
    // cook-mode timers
    await clickIn('[data-a="cook"]'); await sleep(300);
    for (let k = 0; k < pick.step; k++) { await clickIn('[data-a="cookNext"]'); await sleep(100); }
    await clickIn('[data-a="timerStart"][data-k="0"]'); await sleep(200);
    await clickIn('[data-a="timerStart"][data-k="0"]'); await sleep(200);
    let tm = await p.evaluate(() => window.PR_APP.timers());
    check(tm.length === 2 && tm.every(t => t.running) && Math.abs(tm[0].left - pick.first * 1000) < 3000, 'two timers running (' + tm.map(t => t.label).join(', ') + ')');
    await clickIn('.timers [data-a="timerPause"]'); await sleep(150);
    tm = await p.evaluate(() => window.PR_APP.timers());
    check(!tm[0].running && tm[1].running, 'pause one timer, the other keeps going');
    await clickIn('[data-a="cookNext"]'); await sleep(150);
    check(await p.$$eval('.screen.cook .timers .trun', e => e.length) === 2, 'timers stay visible on other steps');
    await p.reload({ waitUntil: 'networkidle0' }); await ready();
    await p.evaluate(id => window.PR_APP.openDetail(id), pick.id); await sleep(300); await clickIn('[data-a="cook"]'); await sleep(400);
    tm = await p.evaluate(() => window.PR_APP.timers());
    check(tm && tm.length === 2 && !tm[0].running && tm[1].running, 'timers survive a reload (end timestamps saved)');
    await p.evaluate(() => window.PR_APP.timerFast(300)); await sleep(1300);
    tm = await p.evaluate(() => window.PR_APP.timers());
    const title = await p.evaluate(() => new Promise(r => { let seen = ''; const iv = setInterval(() => { if (/Time/.test(document.title)) seen = document.title; }, 100); setTimeout(() => { clearInterval(iv); r(seen); }, 1600); }));
    check(tm[1].done && /Time’s up/.test(await p.$eval('.screen.cook .timers', e => e.textContent)) && /Time’s up/.test(title), 'finished timer alarms: "Time’s up" row + title flash ("' + title + '")');
    await hideToast(); await p.screenshot({ path: path.join(shots, '16-cook-timers.png') });
    await clickIn('.trun.up [data-a="timerCancel"]'); await sleep(150);
    await clickIn('[data-a="timerCancel"]'); await sleep(150);
    check((await p.evaluate(() => window.PR_APP.timers())).length === 0, 'timers dismissed / cancelled');
    await clickIn('[data-a="back"]'); await sleep(200); await clickIn('[data-a="back"]'); await sleep(200);
    // leftovers card on the pantry tab
    await p.evaluate(() => window.PR_APP.showTab('pantry')); await sleep(200);
    check(await p.$$eval('#main .lefto .lrow', e => e.length) === 1, 'Leftovers card on the Pantry tab');
    await p.$eval('#main .lefto [data-a="loAdj"][data-d="1"]', b => b.click()); await sleep(150);
    await p.$eval('#main .lefto [data-a="loAdj"][data-d="-1"]', b => b.click()); await sleep(100);
    await p.$eval('#main .lefto [data-a="loAdj"][data-d="-1"]', b => b.click()); await sleep(150);
    v16 = await p.evaluate(() => window.PR_APP.v16());
    check(v16.leftovers.length === 1 && v16.leftovers[0].portions === 1, 'leftover portions +/- (2 -> 3 -> 1)');
    // barcode -> pantry (Open Food Facts mocked)
    await p.$eval('#main [data-a="scan"]', b => b.click()); await sleep(300);
    const bd = await p.evaluate(() => 'BarcodeDetector' in window);
    tt = await topText(p);
    check(bd || /no built-in barcode reader/.test(tt), 'scan screen: ' + (bd ? 'BarcodeDetector available, camera button shown' : 'no BarcodeDetector here, says so and offers manual entry'));
    check(/Open Database License/.test(tt), 'Open Food Facts ODbL attribution shown');
    await p.$eval('#codeIn', e => { e.value = '3017620422004'; }); await clickIn('[data-a="scanLookup"]'); await sleep(200);
    check(/valid EAN/.test(await topText(p)) && offHits === 0, 'bad check digit rejected without a network request');
    await p.$eval('#codeIn', e => { e.value = '3017620422003'; }); await clickIn('[data-a="scanLookup"]');
    await p.waitForSelector('.screen #scanName', { timeout: 5000 });
    const nm = await p.$eval('.screen #scanName', e => e.value);
    check(offHits === 1 && /peanut butter/i.test(nm), 'OFF lookup mapped "Crunchy Peanut Butter" to the catalog item "' + nm + '"');
    await hideToast(); await p.screenshot({ path: path.join(shots, '17-barcode.png') });
    await clickIn('[data-a="scanAdd"]'); await sleep(200);
    v16 = await p.evaluate(() => window.PR_APP.v16());
    const pb = await p.evaluate(n => window.PR_APP.pantryItem(n), nm);
    check(pb && pb.qty === '340 g' && v16.barcodes['3017620422003'] === nm, 'confirmed item added to the pantry with its quantity; code cached');
    await p.$eval('#codeIn', e => { e.value = '3017620422003'; }); await clickIn('[data-a="scanLookup"]'); await sleep(300);
    check(offHits === 1 && (await p.$eval('.screen #scanName', e => e.value)) === nm, 'second scan of the same code uses the cache (no request)');
    await p.$eval('#codeIn', e => { e.value = '4006381333931'; }); await clickIn('[data-a="scanLookup"]'); await p.waitForSelector('.screen #scanName', { timeout: 5000 });
    await p.$eval('.screen #scanName', e => { e.value = 'Highlighter pens'; }); await clickIn('[data-a="scanAdd"]'); await sleep(200);
    check(offHits === 2 && (await p.evaluate(() => window.PR_APP.v16())).barcodes['4006381333931'] === 'Highlighter pens', 'unknown product: manual name accepted and remembered');
    await clickIn('[data-a="back"]'); await sleep(200);
    // buy these next
    await p.evaluate(() => { ['onion', 'garlic', 'chicken breast', 'rice', 'tomatoes', 'eggs', 'milk', 'flour', 'butter', 'carrots'].forEach(x => window.PR_APP.addToPantry(x)); window.PR_APP.showTab('discover'); }); await sleep(400);
    const bn = await p.evaluate(() => ({ ui: [...document.querySelectorAll('#main .buynext .srow b')].map(e => e.textContent), api: window.PR_APP.buyNext() }));
    check(bn.ui.length >= 1 && bn.ui.length <= 3 && JSON.stringify(bn.ui) === JSON.stringify(bn.api.map(x => x.name)), '"Buy these next" card: ' + bn.api.map(x => x.name + ' (+' + x.unlocks.length + ')').join(', '));
    await hideToast(); await p.evaluate(() => { const e = document.querySelector('#main .buynext'); e.scrollIntoView(); }); await p.screenshot({ path: path.join(shots, '18-buy-next.png') });
    // v2.1: tap to expand (why, recipes, price, add), see all with sorting, show more
    await p.$eval('#main .buynext [data-a="bnToggle"]', b => b.click()); await sleep(300);
    const ex = await p.evaluate(() => { const c = document.querySelector('#main .buynext'), d = c.querySelector('.bndetail'); return { exp: c.querySelector('[data-a="bnToggle"]').getAttribute('aria-expanded'), d: d ? d.textContent : '', recs: d ? d.querySelectorAll('[data-a="open"]').length : 0, labels: d ? [...d.querySelectorAll('[data-a="open"]')].every(b => /^Open recipe /.test(b.getAttribute('aria-label'))) : false, all: !!(d && d.querySelector('[data-a="bnRecipes"]')), focus: document.activeElement && document.activeElement.getAttribute('data-a') }; });
    check(ex.exp === 'true' && /one ingredient away/.test(ex.d) && /(About|Your price|No price yet)/.test(ex.d) && ex.recs === Math.min(5, bn.api[0].unlocks.length) && ex.labels && ex.focus === 'bnToggle', 'v2.1: Buy these next row expands with why, price, ' + ex.recs + ' tappable recipes (labelled) and keeps focus');
    check(ex.all === (bn.api[0].unlocks.length > 5), 'v2.1: "See all N recipes" only when more than 5');
    await hideToast(); await p.screenshot({ path: path.join(shots, '18b-buy-next-expanded.png') });
    await p.$eval('#main .buynext .bndetail [data-a="buyAdd"]', b => b.click()); await sleep(150);
    check((await p.evaluate(() => window.PR_APP.state())).shopping.some(s => s.name === bn.api[0].name), 'pick added to the shopping list');
    if (ex.all) { await p.$eval('#main .buynext [data-a="bnRecipes"]', b => b.click()); await sleep(300); check(await p.$$eval('.screen .rrow[data-id]', e => e.length) === bn.api[0].unlocks.length, 'v2.1: see all lists the ' + bn.api[0].unlocks.length + ' recipes it unlocks'); await clickIn('[data-a="back"]'); await sleep(200); }
    else { await p.$eval('#main .buynext .bndetail [data-a="open"]', b => b.click()); await sleep(300); check(await p.evaluate(() => window.PR_APP.state().stack.indexOf('detail') >= 0), 'v2.1: tapping an unlocked recipe opens it'); await clickIn('[data-a="back"]'); await sleep(200); }
    await p.evaluate(() => window.PR_APP.showTab('discover')); await sleep(300);
    if (await p.$('#main .buynext [aria-expanded="true"]')) { await p.$eval('#main .buynext [aria-expanded="true"]', b => b.click()); await sleep(200); }
    check(await p.$$eval('#main .buynext .bndetail', e => e.length) === 0, 'v2.1: row collapses again');
    await p.$eval('#main .buynext [data-a="bnAll"]', b => b.click()); await sleep(400);
    const allKeys = await p.evaluate(() => window.PR_APP.buyNextAll('recipes')), cheapKeys = await p.evaluate(() => window.PR_APP.buyNextAll('cheapest'));
    const rowsN = async () => p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); return [...l.querySelectorAll('[data-a="bnToggleAll"]')].map(b => b.getAttribute('data-key')); });
    let rows = await rowsN();
    check(rows.length === Math.min(20, allKeys.length) && JSON.stringify(rows) === JSON.stringify(allKeys.slice(0, 20)), 'v2.1: see all lists ' + rows.length + ' of ' + allKeys.length + ' suggestions, most recipes first');
    if (allKeys.length > 20) { await clickIn('[data-a="bnMore"]'); await sleep(300); rows = await rowsN(); check(rows.length === Math.min(40, allKeys.length), 'v2.1: show more adds the next page (' + rows.length + ')'); }
    await clickIn('[data-a="bnSort"][data-v="cheapest"]'); await sleep(300); rows = await rowsN();
    check(JSON.stringify(rows) === JSON.stringify(cheapKeys.slice(0, 20)) && await p.evaluate(() => [...document.querySelectorAll('.screen')].pop().querySelector('[data-a="bnSort"][data-v="cheapest"]').getAttribute('aria-pressed')) === 'true', 'v2.1: cheapest sort (first: ' + rows.slice(0, 3).join(', ') + ')');
    await clickIn('[data-a="bnToggleAll"]'); await sleep(300);
    check(/(Completes|Brings) \d+/.test(await p.evaluate(() => [...document.querySelectorAll('.screen')].pop().querySelector('.bndetail').textContent)), 'v2.1: an item in the full list expands with its reason');
    await clickIn('[data-a="bnSort"][data-v="recipes"]'); await sleep(200);
    await clickIn('[data-a="back"]'); await sleep(200);
    // shopping by aisle, collapse, cost, checks persist, share
    await p.evaluate(() => { window.PR_APP.showTab('shopping'); }); await sleep(200);
    for (const it of ['500 g chicken breast', '2 lemons', '1 l milk', 'paprika']) { await p.focus('#shopIn'); await p.keyboard.type(it); await p.keyboard.press('Enter'); await sleep(120); }
    const aisles = await p.evaluate(() => { const st = window.PR_APP.state(); const open = st.shopping.filter(s => !s.checked).sort((a, b) => a.added - b.added); return { ui: [...document.querySelectorAll('#main .aisle h2')].map(h => h.firstChild.textContent.trim()), exp: Array.from(window.PR.Aisles.group(open.map(x => x.name)).keys()) }; });
    check(aisles.ui.length >= 3 && JSON.stringify(aisles.ui) === JSON.stringify(aisles.exp), 'shopping list grouped by aisle: ' + aisles.ui.join(' / '));
    check(/About €\d/.test(await p.$eval('#main', e => e.textContent)) && /priced/.test(await p.$eval('#main', e => e.textContent)), 'optional shopping-list cost total shown as an estimate');
    await hideToast(); await p.screenshot({ path: path.join(shots, '19-shopping-aisles.png') });
    const nBody = await p.$$eval('#main .aisle .abody', e => e.length);
    await p.$eval('#main .aisle [data-a="aisleToggle"]', b => b.click()); await sleep(150);
    check(await p.$$eval('#main .aisle .abody', e => e.length) === nBody - 1, 'aisle group collapses');
    await p.$eval('#main .aisle [data-a="aisleToggle"]', b => b.click()); await sleep(150);
    await p.$eval('#main .aisle .shop .cbx', b => b.click()); await sleep(150);
    await p.reload({ waitUntil: 'networkidle0' }); await ready();
    check((await p.evaluate(() => window.PR_APP.state())).shopping.filter(s => s.checked).length === 1, 'shopping checks persist across reload');
    await p.evaluate(() => window.PR_APP.showTab('shopping')); await sleep(200);
    await p.evaluate(() => { Object.defineProperty(navigator, 'share', { value: undefined, configurable: true }); Object.defineProperty(navigator, 'clipboard', { value: { writeText: t => { window.__clip = t; return Promise.resolve(); } }, configurable: true }); });
    await p.$eval('#main [data-a="shopShare"]', b => b.click()); await sleep(300);
    const shareExp = await p.evaluate(() => { const st = window.PR_APP.state(); return window.PR.ShareText.shopping(st.shopping.slice().sort((a, b) => a.added - b.added).map(x => [x.name, x.amount == null ? NaN : x.amount, x.unit || '', !!x.checked])); });
    const shareGot = await p.evaluate(() => ({ box: (document.querySelector('#dlg .sharebox') || {}).value, clip: window.__clip }));
    check(shareGot.box === shareExp && shareGot.clip === shareExp && /Shopping list/.test(shareExp), 'share fallback: copied to clipboard and shown in a text box');
    await hideToast(); await p.screenshot({ path: path.join(shots, '20-share-fallback.png') });
    await okDlg();
    await p.evaluate(() => { Object.defineProperty(navigator, 'share', { value: d => { window.__shared = d; return Promise.resolve(); }, configurable: true }); });
    await p.$eval('#main [data-a="shopShare"]', b => b.click()); await sleep(200);
    check(await p.evaluate(() => window.__shared && /Shopping list/.test(window.__shared.text)) && !(await p.$('#dlg')), 'navigator.share used when available');
    await p.$eval('#main [data-a="shopToPantry"]', b => b.click()); await sleep(150);
    check((await p.evaluate(() => window.PR_APP.state())).shopping.filter(s => s.checked).length === 0, 'checked items moved to the pantry');
    // plan share
    await p.evaluate(id => { window.PR_APP.openDetail(id); }, pick.id); await sleep(300);
    await clickIn('[data-a="planAdd"]'); await okDlg();
    await clickIn('[data-a="back"]'); await sleep(200);
    await clickIn('.top [data-a="about"]'); await clickIn('[data-a="plan"]'); await sleep(300);
    await clickIn('[data-a="planShare"]'); await sleep(200);
    check(await p.evaluate(t => window.__shared && /^Meal plan/.test(window.__shared.text) && window.__shared.text.includes(t), pick.title), 'meal plan shared as text');
    check(!(await p.$('.screen')) && await p.$eval('.tab.on', e => e.getAttribute('data-tab')) === 'plan', 'v1.8: Settings > Meal plan opens the Plan tab');
    // About lists the new sources
    await clickIn('.top [data-a="about"]'); await sleep(200);
    tt = await topText(p);
    check(/USDA FoodData Central/.test(tt) && /openfoodfacts\.org/.test(tt) && /Open Database License/.test(tt) && new RegExp('Version ' + VERSION.replace(/\./g, '\\.')).test(tt), 'About: version ' + VERSION + ', USDA FDC + Open Food Facts (ODbL) attributions');
    await clickIn('[data-a="back"]'); await sleep(150);
    // backup v2: prices, leftovers, cooked, barcodes; photos only when asked
    const bkA = await p.evaluate(() => window.PR_APP.backupText(false)), bkP = await p.evaluate(() => window.PR_APP.backupText(true));
    const jA = JSON.parse(bkA), jP = JSON.parse(bkP);
    check(jA.format === 2 && !jA.photos && jP.photos && Object.keys(jP.photos).length === 1, 'backup format 2; photos only with "Include photos" (' + Math.round(bkA.length / 1024) + ' KB vs ' + Math.round(bkP.length / 1024) + ' KB)');
    const src16 = await p.evaluate(() => window.PR_APP.v16());
    const ctx3 = await browser.createBrowserContext();
    const q = await np(ctx3);
    q.on('pageerror', e => e6.push('pageerror(3): ' + e.message));
    await q.setRequestInterception(true);
    q.on('request', r => { if (/themealdb|openfoodfacts/.test(r.url())) r.abort(); else r.continue(); });
    await q.goto(BASE, { waitUntil: 'networkidle0' });
    await q.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
    await q.evaluate(t => window.PR_APP.importBackupText(t), bkP);
    const dst16 = await q.evaluate(() => window.PR_APP.v16());
    check(JSON.stringify(dst16.prices) === JSON.stringify(src16.prices) && JSON.stringify(dst16.leftovers) === JSON.stringify(src16.leftovers) && JSON.stringify(dst16.cooked) === JSON.stringify(src16.cooked) &&
      JSON.stringify(dst16.barcodes) === JSON.stringify(src16.barcodes) && dst16.currency === '€' && dst16.photos === 1, 'v1.6 backup restores prices, currency, leftovers, cooking log, barcodes and photos in a fresh profile');
    const F15 = JSON.parse(fs.readFileSync(path.join(ROOT, 'tests/fixtures/v15-backups.json'), 'utf8'));
    const big15 = F15.slice().sort((a, b) => b.length - a.length)[0];
    const r15 = await q.evaluate(t => { try { window.PR_APP.importBackupText(t); const v = window.PR_APP.v16(); return { n: window.PR_APP.state().pantry.length, prices: Object.keys(v.prices).length, lo: v.leftovers.length, photos: v.photos }; } catch (e) { return { err: e.message }; } }, big15);
    check(!r15.err && r15.n === JSON.parse(big15).pantry.length && r15.prices === 0 && r15.lo === 0 && r15.photos === 1, 'a real v1.5 backup still imports (' + (r15.err || r15.n + ' pantry items; no v1.6 data; existing photo kept') + ')');
    await ctx3.close();
    const bad6 = e6.filter(e => !/net::|Failed to load resource|themealdb|openfoodfacts/i.test(e));
    check(bad6.length === 0, 'no console errors (v1.6 pass)' + (bad6.length ? ': ' + bad6.join(' / ') : ''));
    await ctx.close();
  }

  // ---------- v1.7: units, conversions, swaps, Instant Pot ----------
  {
    const e7 = []; const VERSION_EXPECT = VERSION; let tt;
    const ctx = await browser.createBrowserContext();
    const p = await np(ctx);
    await p.setViewport({ width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
    p.on('console', m => { if (m.type() === 'error') e7.push(m.text()); });
    p.on('pageerror', e => e7.push('pageerror: ' + e.message));
    await p.setRequestInterception(true);
    p.on('request', r => { if (/themealdb|openfoodfacts/.test(r.url())) r.abort(); else r.continue(); });
    await p.goto(BASE, { waitUntil: 'networkidle0' });
    await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 });
    const hideToast = () => p.evaluate(() => { const t = document.getElementById('toast'); if (t) t.hidden = true; });
    const clickIn = (sel) => p.evaluate(sel => { const l = [...document.querySelectorAll('.screen')].pop(); const b = (l || document).querySelector(sel); if (!b) return false; b.click(); return true; }, sel);
    const ingText = () => p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); return [...l.querySelectorAll('.ing')].map(e => (e.querySelector('.q') || {}).textContent + ' | ' + (e.querySelector('.nm') || {}).textContent); });
    check(await p.evaluate(() => window.PR_APP.VERSION) === VERSION_EXPECT, 'v1.7: app version ' + VERSION_EXPECT);
    check(/v2\.1(?![-.\w])/.test(await p.$eval('.top .ver', e => e.textContent)), 'main header label shows v2.1');
    // default units = weights, flour in grams with cups in parentheses
    await p.evaluate(() => window.PR_APP.openDetail('b:1384')); await sleep(400);
    let ing = await ingText();
    const flour = ing.find(t => /\| Flour$/i.test(t)) || '';
    check(/\d+\s*g\b/.test(flour) && /\(.*cup.*\)/.test(flour), 'default "Weights for dry baking": flour shows grams with cups in parentheses (' + flour + ')');
    check(await p.$$eval('.screen:last-of-type .chips.units .chip.on, .screen:last-of-type .chips.units .chip[aria-pressed="true"]', e => e.map(x => x.textContent).join()).then(t => /Weights/.test(t)), 'detail units chip row marks "Weights" as current');
    const g1 = parseFloat((flour.match(/(\d+(?:\.\d+)?)\s*g\b/) || [])[1]);
    // scaling x2 then switch to Metric: still scaled
    await clickIn('[data-a="serv"][data-d="1"]'); await sleep(150);
    const r1384 = await p.evaluate(() => { const s = [...document.querySelectorAll('.screen')].pop(); return s.querySelector('.stepper b').textContent; });
    await clickIn('[data-a="detUnits"][data-v="us"]'); await sleep(200);
    ing = await ingText();
    const flourUS = ing.find(t => /\| Flour$/i.test(t)) || '';
    check(!/\d\s*g\b/.test(flourUS) && /cup/.test(flourUS), 'in-recipe toggle to US shows cups only (' + flourUS + ')');
    await clickIn('[data-a="detUnits"][data-v="weights"]'); await sleep(200);
    ing = await ingText();
    const g2 = parseFloat(((ing.find(t => /\| Flour$/i.test(t)) || '').match(/(\d+(?:\.\d+)?)\s*g\b/) || [])[1]);
    check(g1 > 0 && g2 > 0 && Math.abs(g2 / g1 - (Number(r1384) / (Number(r1384) - 1))) < 0.06, 'unit toggle keeps the servings scaling (' + g1 + ' g -> ' + g2 + ' g at ' + r1384 + ' servings)');
    await clickIn('[data-a="detUnits"][data-v="metric"]'); await sleep(200);
    tt = await topText(p);
    check(/Showing metric/i.test(tt) && /default is weights/i.test(tt), 'override note: showing metric for now, default stays weights');
    ing = await ingText();
    check(ing.some(t => /\bml\b/.test(t)) && !ing.some(t => /\bcups?\b/.test(t.split(' | ')[0])), 'Metric toggle: volumes in ml, no cups (' + ing.slice(0, 3).join(' ; ') + ')');
    await hideToast(); await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const u = l.querySelector('.chips.units'); if (u) u.scrollIntoView({ block: 'start' }); window.scrollBy(0, -120); l.scrollTop -= 0; }); await sleep(150);
    await p.screenshot({ path: path.join(shots, '21-detail-units.png') });
    // conversions screen from the detail page
    await clickIn('[data-a="conv"]'); await sleep(300);
    tt = await topText(p);
    check(/Volume & weight/.test(tt) && /Oven temperature/i.test(tt) && /Gas/.test(tt) && /butter/i.test(tt), 'Conversions screen: volume/weight, ingredient, butter, oven table');
    const setIn = async (id, v) => { await p.evaluate((id, v) => { const e = document.getElementById(id); e.value = v; e.dispatchEvent(new Event('input', { bubbles: true })); e.dispatchEvent(new Event('change', { bubbles: true })); }, id, v); await sleep(150); };
    const out = id => p.$eval('#' + id, e => e.textContent);
    await setIn('cvA', '1 1/2'); let o = await out('cvOut');
    check(/^360 ml/.test(o) && /24 tbsp/.test(o) && /\b12 fl oz/.test(o), 'volume converter: 1 1/2 cup (240 ml cup) -> 360 ml, 24 tbsp, 12 fl oz (' + o.replace(/\n/g, ' | ') + ')');
    await setIn('cvA', '2-3'); o = await out('cvOut');
    check(/to/.test(o) && /480 ml/.test(o) && /720 ml/.test(o), 'volume converter handles a range 2-3 cup (' + o.replace(/\n/g, ' | ') + ')');
    await setIn('cvI', 'all-purpose flour'); await setIn('cvIA', '1'); o = await out('cvIOut');
    check(/12\d\s*g/.test(o) && /g per cup/.test(o), 'ingredient converter: 1 cup all-purpose flour ~120-129 g (' + o.replace(/\n/g, ' | ') + ')');
    await setIn('cvB', '1'); o = await out('cvBOut');
    check(/113\s*g/.test(o) || /\b8 tbsp\b/.test(o), 'butter helper converts (' + o.replace(/\n/g, ' | ') + ')');
    await setIn('cvTU', 'F'); await setIn('cvT', '350'); o = await out('cvTOut');
    check(/17[5-7]\s*\u00b0C/.test(o) && /[Gg]as\s*(mark\s*)?4/.test(o), 'temperature: 350 F -> ~177 C, gas 4 (' + o.replace(/\n/g, ' | ') + ')');
    check(await p.$$eval('.screen:last-of-type .otable tr', e => e.length) >= 8, 'oven table has rows');
    await hideToast(); await p.screenshot({ path: path.join(shots, '22-conversions.png') });
    // change the default to Ounces + hide cups, then check the detail page
    await clickIn('[data-a="unitsWeight"][data-v="oz"]'); await sleep(150);
    await p.evaluate(() => { const c = [...document.querySelectorAll('.screen')].pop().querySelector('input[data-change="showCups"]'); if (c) { c.checked = false; c.dispatchEvent(new Event('change', { bubbles: true })); } }); await sleep(200);
    await clickIn('[data-a="back"]'); await sleep(300);
    await clickIn('[data-a="detUnits"][data-v="weights"]'); await sleep(200);
    ing = await ingText();
    const fOz = ing.find(t => /\| Flour$/i.test(t)) || '';
    check(/\boz\b/.test(fOz) && !/cup/.test(fOz), 'Ounces + hidden cups: flour shown in oz without cups (' + fOz + ')');
    // backup contains the units settings, and a v1.6 backup (no units key) imports
    const bk = JSON.parse(await p.evaluate(() => window.PR_APP.backupText(false)));
    const bs = bk.settings || bk;
    check(bs.units === 'weights' && bs.weightUnit === 'oz' && bs.showCups === false, 'backup export includes units=weights, weightUnit=oz, showCups=false (' + JSON.stringify({ u: bs.units, w: bs.weightUnit, c: bs.showCups }) + ')');
    // shopping list uses the unit setting
    await p.evaluate(() => { window.PR_APP.openDetail('b:1384'); }); await sleep(300);
    // cook mode in Instant Pot recipe: pressure timer + build-up note
    const ipId = 'b:1207'; // Instant Pot Chicken Noodle Soup: "Pressure cook on High for 8 minutes. The pot first takes about 10 to 15 minutes to come up to pressure ..."
    await p.evaluate(id => window.PR_APP.openDetail(id), ipId); await sleep(300);
    tt = await topText(p);
    check(/Cooking method: .*Instant Pot/.test(tt), 'detail shows "Cooking method: ... Instant Pot"');
    await clickIn('[data-a="cook"]'); await sleep(300);
    let found = false;
    for (let k = 0; k < 15 && !found; k++) {
      const t = await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); return [...l.querySelectorAll('.timer')].map(b => b.textContent + ' || ' + (b.getAttribute('aria-label') || '')).join(' ## ') + ' @@ ' + [...l.querySelectorAll('.pnote')].map(e => e.textContent).join(' ## '); });
      if (/at pressure/.test(t)) { found = t; break; }
      if (!(await clickIn('[data-a="cookNext"]'))) break; await sleep(200);
    }
    check(!!found && /10\u201315|10-15/.test(found) && /not (counted|included)/i.test(found), 'cook mode: "Start X at pressure" timer with the ~10-15 min build-up note (' + String(found).slice(0, 200) + ')');
    await hideToast(); await p.screenshot({ path: path.join(shots, '23-pressure-timer.png') });
    await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l.querySelector('.timer.pressure'); if (b) b.click(); }); await sleep(300);
    tt = await topText(p);
    check(/at pressure/.test(tt), 'running timer label carries "at pressure"');
    // leave cook mode
    for (let k = 0; k < 4; k++) { const n = await p.evaluate(() => document.querySelectorAll('.screen').length); if (n <= 1) break; await clickIn('[data-a="back"]'); await sleep(200); }
    // Discover: method card + cheat sheet
    await p.evaluate(() => window.PR_APP.showTab('discover')); await sleep(400);
    check(await p.$('#methodsCard') !== null, 'Discover shows "Browse by cooking method"');
    const ipCount = await p.$eval('#methodsCard [data-v="instant-pot"] .faint', e => parseInt(e.textContent, 10)).catch(() => 0);
    check(ipCount >= 150, 'Discover: Instant Pot chip count >= 150 (' + ipCount + ')');
    await p.$eval('#methodsCard [data-v="instant-pot"]', e => e.click()); await sleep(400);
    tt = await topText(p);
    check(/Cooking method/.test(tt) && /10\u201315/.test(tt), 'Instant Pot browse list shows the build-up note');
    const nIP = await p.$$eval('.screen:last-of-type .rrow[data-id]', e => e.length);
    check(nIP > 10, 'Instant Pot browse list has recipes (' + nIP + ' rows rendered)');
    await clickIn('[data-a="back"]'); await sleep(200);
    await p.$eval('#methodsCard [data-a="ipSheet"]', e => e.click()); await sleep(300);
    tt = await topText(p);
    check(/manufacturer/i.test(tt) && /manual/i.test(tt) && /natural release/i.test(tt), 'cheat sheet says to check the manufacturer manual and covers natural release');
    check(await p.$$eval('.screen:last-of-type .otable tr', e => e.length) >= 10, 'cheat sheet times table has rows');
    await hideToast(); await p.screenshot({ path: path.join(shots, '24-ip-cheatsheet.png') });
    await clickIn('[data-a="back"]'); await sleep(200);
    // recipes tab: method filter chip
    await p.evaluate(() => { ['chicken breast', 'rice', 'onion', 'garlic', 'beans', 'potatoes', 'carrots', 'stock'].forEach(x => window.PR_APP.addToPantry(x)); window.PR_APP.showTab('recipes'); }); await sleep(400);
    await p.$eval('[data-a="methodF"][data-v="instant-pot"]', e => e.click()); await sleep(400);
    const mf = await p.evaluate(() => { const res = window.PR_APP.filteredResults(); return { n: res.length, all: res.every(x => window.PR.Methods.has(x.recipe, 'instant-pot')) }; });
    check(mf.n > 0 && mf.all, 'recipes filter "Instant Pot" keeps only IP recipes (' + mf.n + ')');
    const bk2 = JSON.parse(await p.evaluate(() => window.PR_APP.backupText(false)));
    check(JSON.stringify((bk2.settings || bk2).methods) === '["instant-pot"]', 'backup export includes the method filter');
    await p.$eval('[data-a="methodF"][data-v="instant-pot"]', e => e.click()); await sleep(300);
    // swaps: a missing-ingredient row shows ratio + effect details
    await p.evaluate(() => { ['milk', 'lemon', 'flour', 'baking powder', 'baking soda', 'sugar'].forEach(x => window.PR_APP.addToPantry(x)); window.PR_APP.openDetail('b:127'); }); await sleep(300);
    const sdet = await p.$$eval('.screen:last-of-type .sdet', e => e.map(x => x.textContent));
    check(sdet.length > 0 && sdet.some(t => t.length > 10), 'missing-ingredient rows show swap details (' + (sdet[0] || '').slice(0, 120) + ')');
    await hideToast(); await p.screenshot({ path: path.join(shots, '25-swap-detail.png') });
    await clickIn('[data-a="subDlg"]'); await sleep(300);
    const dl = await p.$$eval('#dlg .sdet', e => e.map(x => x.textContent));
    check(dl.length >= 2 && dl.some(t => /\d/.test(t)), 'substitute dialog shows ratio/flags/effect for each option (' + dl.length + ')');
    await p.keyboard.press('Escape'); await p.evaluate(() => { const b = document.querySelector('#dlg .acts .btn.ghost, #dlg .acts .btn'); if (b && document.querySelector('#dlg') && !document.querySelector('#dlg').hidden) b.click(); }); await sleep(200);
    // v1.6 backup import keeps defaults for units
    const F16 = path.join(ROOT, 'tests/fixtures');
    const v16file = fs.readdirSync(F16).find(f => /v16.*\.json$/.test(f) && !/meal-/.test(f));
    if (v16file) {
      const t16 = fs.readFileSync(path.join(F16, v16file), 'utf8');
      const cf = await browser.createBrowserContext(); const pf = await np(cf);
      pf.on('pageerror', e => e7.push('pageerror(fresh): ' + e.message));
      await pf.setRequestInterception(true); pf.on('request', r => { if (/themealdb|openfoodfacts/.test(r.url())) r.abort(); else r.continue(); });
      await pf.goto(BASE, { waitUntil: 'networkidle0' }); await pf.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
      const r = await pf.evaluate(t => { try { window.PR_APP.importBackupText(t); return JSON.parse(window.PR_APP.backupText(false)); } catch (e) { return { err: e.message }; } }, t16);
      await cf.close();
      const s = r.settings || r;
      check(!r.err && s.units === 'weights' && s.weightUnit === 'g' && s.showCups === true, 'v1.6 backup (' + v16file + ') imports; units fall back to Weights / g / cups shown (' + (r.err || JSON.stringify({ u: s.units, w: s.weightUnit, c: s.showCups })) + ')');
    } else console.log('INFO no v1.6 backup fixture file found in tests/fixtures');
    const bad7 = e7.filter(e => !/net::|Failed to load resource|themealdb|openfoodfacts/i.test(e));
    check(bad7.length === 0, 'no console errors (v1.7 pass)' + (bad7.length ? ': ' + bad7.join(' / ') : ''));
    await ctx.close();
  }

  // ---------- v1.8: layout, rebrand, decorations, lighter rendering ----------
  {
    const e8 = [];
    const ctx = await browser.createBrowserContext();
    const p = await np(ctx);
    await p.setViewport({ width: 601, height: 905, deviceScaleFactor: 1.33, isMobile: true, hasTouch: true });
    p.on('console', m => { if (m.type() === 'error') e8.push(m.text()); });
    p.on('pageerror', e => e8.push('pageerror: ' + e.message));
    await p.setRequestInterception(true);
    p.on('request', r => { if (/themealdb|openfoodfacts/.test(r.url())) r.abort(); else r.continue(); });
    await p.goto(BASE, { waitUntil: 'networkidle0' });
    await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 });
    check(await p.title() === 'My Pantry Recipes', 'v1.8: page title is My Pantry Recipes');
    const hdr = await p.evaluate(() => { const h = document.querySelector('.top h1'); return { t: h.textContent, logo: !!document.querySelector('.top .brand svg.mark'), col: getComputedStyle(h).color, text: getComputedStyle(document.documentElement).getPropertyValue('--text').trim() }; });
    check(/^My Pantry Recipes/.test(hdr.t) && hdr.logo, 'v1.8: header shows the leaf logo + live name (' + hdr.t + ')');
    const mf = await p.evaluate(() => fetch('manifest.json').then(r => r.json()).catch(() => null));
    check(mf && mf.name === 'My Pantry Recipes' && mf.short_name === 'Pantry' && mf.icons.some(i => /maskable/.test(i.purpose || '')), 'v1.8: manifest name / short_name / maskable icon');
    check(await p.evaluate(() => document.documentElement.classList.contains('deco') && window.PR_APP.decoOn()), 'v1.8: Decorations on by default');
    // pantry: in-place toggle gives the same DOM as a full render
    await p.evaluate(() => window.PR_APP.showTab('pantry')); await sleep(200);
    if (!(await p.$('.cat.open'))) await tap(p, '.cat > button[data-ci="0"]');
    const lbl = await p.$eval('.cat.open .chip[data-a="toggleItem"]:not(.have)', e => e.getAttribute('data-label'));
    await tap(p, '.cat.open .chip[data-a="toggleItem"][data-label="' + lbl + '"]');
    const inPlace = await p.evaluate(() => [...document.querySelectorAll('#main [data-region="grid"] .cat .cnt, #main [data-region="grid"] .chip')].map(e => e.outerHTML).join('') + document.querySelector('#main [data-region="mine"]').innerHTML + document.getElementById('tabs').innerHTML);
    await p.evaluate(() => window.PR_APP.showTab('pantry')); await sleep(150);
    const full = await p.evaluate(() => [...document.querySelectorAll('#main [data-region="grid"] .cat .cnt, #main [data-region="grid"] .chip')].map(e => e.outerHTML).join('') + document.querySelector('#main [data-region="mine"]').innerHTML + document.getElementById('tabs').innerHTML);
    check(inPlace === full, 'v1.8: in-place pantry toggle matches a full render (' + lbl + ')');
    await tap(p, '.cat.open .chip[data-a="toggleItem"][data-label="' + lbl + '"]');
    check(!(await p.evaluate(l => window.PR_APP.state().pantry.some(x => x.label.toLowerCase() === l.toLowerCase()), lbl)), 'v1.8: second tap removes it again');
    // recipes: flat rows, no photos, one filled Cook action
    await p.evaluate(() => { window.PR_APP.addToPantry('eggs'); window.PR_APP.addToPantry('chicken'); window.PR_APP.showTab('recipes'); }); await sleep(400);
    const rows = await p.evaluate(() => { const r = [...document.querySelectorAll('.rrow[data-id]')]; return { n: r.length, img: document.querySelectorAll('.rrow img, .ccard img').length, shadow: r.filter(x => getComputedStyle(x).boxShadow !== 'none').length, cook: r.filter(x => x.querySelector('.cookb')).length, nested: document.querySelectorAll('button button').length }; });
    check(rows.n > 10 && rows.img === 0 && rows.shadow === 0 && rows.cook === rows.n && rows.nested === 0, 'v1.8: recipe rows are flat (no photos, no shadows) with a Cook action (' + JSON.stringify(rows) + ')');
    const cid = await p.$eval('.rrow[data-id] .cookb', e => e.getAttribute('data-id'));
    await tap(p, '.rrow[data-id] .cookb'); await sleep(300);
    check(await p.evaluate(() => !!document.querySelector('.screen.cook')), 'v1.8: Cook on a row opens cook mode (' + cid + ')');
    await p.keyboard.press('Escape'); await sleep(200);
    check(!(await p.$('.screen.cook')), 'Escape leaves cook mode');
    // near-duplicate titles get a hint (computed when idle)
    await sleep(1800);
    const hint = await p.evaluate(() => window.PR_APP.titleHint('b:25'));
    check(hint === 'with broccoli', 'v1.8: near-duplicate title hint for b:25 (' + hint + ')');
    await p.evaluate(() => window.PR_APP.openDetail('b:25')); await sleep(300);
    check(await p.evaluate(() => { const d = [...document.querySelectorAll('.screen')].pop().querySelector('.dhint'); return d && d.textContent === 'with broccoli'; }), 'v1.8: recipe page shows the title hint');
    // detail units toggle is remembered across reloads
    const um = await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = [...l.querySelectorAll('[data-a="detUnits"]')].find(x => x.getAttribute('aria-pressed') !== 'true'); if (!b) return null; b.click(); return b.getAttribute('data-v'); });
    await p.reload({ waitUntil: 'networkidle0' }); await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
    await p.evaluate(() => window.PR_APP.openDetail('b:25')); await sleep(300);
    check(um && await p.evaluate(v => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l.querySelector('[data-a="detUnits"][data-v="' + v + '"]'); return !!b && b.getAttribute('aria-pressed') === 'true'; }, um), 'v1.8: recipe units toggle persists across reload (' + um + ')');
    await p.keyboard.press('Escape'); await sleep(150);
    // header band + tab contrast on every preset
    const hc = await p.evaluate(() => {
      const lum = h => { const n = parseInt(h.replace('#', ''), 16), c = [n >> 16 & 255, n >> 8 & 255, n & 255].map(v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }); return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]; };
      const ratio = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
      const T = window.PR_APP.Theme, prev = T.state().active, bad = [];
      window.PR.Palette.PRESETS.forEach(b => { T.use(b.id); const cs = getComputedStyle(document.documentElement), v = k => cs.getPropertyValue(k).trim();
        if (ratio(v('--text'), v('--tint')) < 4.5 || ratio(v('--text'), v('--neutral')) < 4.5 || ratio(v('--on-fill'), v('--green')) < 4.5 || ratio(v('--green-dark'), v('--card')) < 4.5 || ratio(v('--card'), v('--text')) < 4.5) bad.push(b.id); });
      T.use(prev); return bad;
    });
    check(hc.length === 0, 'v1.8: header band, tab track, selected tab/chip and Cook button pass AA on every preset' + (hc.length ? ': ' + hc.join() : ''));
    await p.screenshot({ path: path.join(shots, '18-v18-recipes.png') });
    // Decorations off
    await p.evaluate(() => window.PR_APP.showTab('pantry'));
    await tap(p, '.top [data-a="about"]');
    await p.evaluate(() => { const c = [...document.querySelectorAll('.screen')].pop().querySelector('[data-change="decorations"]'); c.click(); });
    await sleep(150);
    check(!(await p.evaluate(() => document.documentElement.classList.contains('deco'))), 'v1.8: Decorations can be turned off');
    await p.reload({ waitUntil: 'networkidle0' }); await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete);
    check(!(await p.evaluate(() => document.documentElement.classList.contains('deco'))), 'v1.8: Decorations setting persists');
    check(await p.evaluate(() => [...document.styleSheets].some(s => { try { return [...s.cssRules].some(r => r.media && /prefers-reduced-motion/.test(r.media.mediaText)); } catch (e) { return false; } })), 'v1.8: stylesheet honours prefers-reduced-motion');
    const bad8 = e8.filter(e => !/net::|Failed to load resource|themealdb|openfoodfacts/i.test(e));
    check(bad8.length === 0, 'no console errors (v1.8 pass)' + (bad8.length ? ': ' + bad8.join(' / ') : ''));
    await ctx.close();
  }

  // ---------- single file via file:// ----------
  {
    const e3 = [];
    const p = await newPage(browser, e3, false);
    await p.goto('file://' + SINGLE, { waitUntil: 'load' });
    await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 });
    const s3 = await p.evaluate(() => ({ st: window.PR_APP.state(), persistent: window.PR_APP.Store.persistent, sw: !!(navigator.serviceWorker && navigator.serviceWorker.controller), cache: window.PR_APP.Cache.mode() }));
    check(s3.st.bundled === NREC, 'single file loads ' + NREC + ' recipes from file://');
    console.log('INFO single file: localStorage persistent=' + s3.persistent + ', cache=' + s3.cache + ', sw controller=' + s3.sw);
    await p.evaluate(() => window.PR_APP.addToPantry('eggs'));
    await p.evaluate(() => window.PR_APP.showTab('recipes'));
    await sleep(500);
    check(await p.$$eval('.rrow[data-id]', e => e.length) > 10, 'single file recipes tab renders');
    const bad = e3.filter(e => !/net::|Failed to load resource/i.test(e));
    check(bad.length === 0, 'no console errors (single file)' + (bad.length ? ': ' + bad.join(' / ') : ''));
    await p.close();
  }
  // ---------- single file with storage blocked (simulated) ----------
  {
    const e4 = [];
    const p = await newPage(browser, e4, false);
    await p.evaluateOnNewDocument(() => {
      Object.defineProperty(window, 'localStorage', { get() { throw new DOMException('blocked', 'SecurityError'); } });
      Object.defineProperty(window, 'indexedDB', { get() { throw new DOMException('blocked', 'SecurityError'); } });
    });
    await p.goto('file://' + SINGLE, { waitUntil: 'load' });
    await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 });
    const s4 = await p.evaluate(() => ({ persistent: window.PR_APP.Store.persistent, cache: window.PR_APP.Cache.mode(), ok: window.PR_APP.addToPantry('eggs'), banner: !!document.querySelector('.banner.warn') }));
    check(!s4.persistent && s4.cache === 'memory' && s4.ok && s4.banner, 'blocked storage falls back to memory with a warning banner');
    check(e4.filter(e => !/net::|Failed to load resource/i.test(e)).length === 0, 'no console errors (storage blocked)');
    await p.close();
  }
  // ---------- v2.0 Stage 1: region welcome, inventory, diets, household, editor, prices (headless Chrome, not a device) ----------
  {
    const e5 = [];
    const c5 = await browser.createBrowserContext();
    const p = await c5.newPage();
    await p.setViewport({ width: 800, height: 1280, deviceScaleFactor: 1.3, isMobile: true, hasTouch: true }); // Fire HD 8-sized portrait viewport
    p.on('console', m => { if (m.type() === 'error') e5.push(m.text()); });
    p.on('pageerror', e => e5.push('pageerror: ' + e.message));
    await p.evaluateOnNewDocument(() => { Object.defineProperty(navigator, 'language', { get: () => 'en-GB' }); });
    await p.setRequestInterception(true);
    p.on('request', r => { if (/themealdb\.com|openfoodfacts\.org/.test(r.url())) r.abort(); else r.continue(); });
    await p.goto(BASE, { waitUntil: 'networkidle0' });
    await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().complete, { timeout: 15000 });
    await sleep(200);
    let t = await topText(p);
    check(/Where do you cook\?/.test(t) && /UK & Ireland\s*Suggested/.test(t), 'v2.0: first run asks the Region question first, suggests UK from en-GB');
    check(/Suggested from your device language\. This app never uses your location\./.test(t), 'v2.0.1: welcome says the suggestion comes from the device language, not location');
    await tap(p, '[data-a="welRegion"][data-v="uk"]');
    t = await topText(p);
    check(!/Starter kits/.test(t) && (await p.$$eval('[data-kit], [data-a="invKit"]', e => e.length)) === 0, 'v2.0.1: no starter kits step after choosing a region');
    let st = await p.evaluate(() => window.PR_APP.state());
    check(st.stack.length === 0 && st.pantry.filter(x => !x.staple).length === 0, 'v2.0.1: choosing a region finishes the welcome and adds nothing but the default staples');
    const bk = JSON.parse(await p.evaluate(() => window.PR_APP.backupText(false)));
    check(bk.settings.region === 'uk' && bk.settings.dateFormat === 'DD/MM/YYYY' && bk.settings.clock24 === true && bk.settings.currency === '\u00a3' && bk.settings.units === 'weights', 'v2.0: UK defaults applied (units, \u00a3, DD/MM/YYYY, 24 h) and in the backup');
    check(!/apiKey/.test(JSON.stringify(bk.settings)), 'v2.0: backup has no API key');
    // localized names
    const zuc = DATA.find(r => r.ingredients.some(i => /^zucchini/i.test(i.name)));
    await p.evaluate(id => window.PR_APP.openDetail(id), zuc.id); await sleep(250);
    t = await topText(p);
    check(/courgette/i.test(t), 'v2.0: UK region shows "courgette" for zucchini on ' + zuc.title);
    check(/Fits: .*guide, not certification/.test(t), 'v2.0: recipe page lists diets as a guide');
    await tap(p, '[data-a="back"]');
    // household alert
    await tap(p, '[data-a="about"]'); await tap(p, '[data-a="avoid"]'); await tap(p, '[data-a="household"]');
    await p.type('#hhIn', 'Sam'); await tap(p, '[data-a="hhAdd"]');
    await tap(p, '[data-a="hhToggle"][data-v="peanuts"]');
    const pea = DATA.find(r => r.ingredients.some(i => /peanut/i.test(i.name)));
    await p.evaluate(id => window.PR_APP.openDetail(id), pea.id); await sleep(250);
    t = await topText(p);
    check(/Household/.test(t) && /Peanuts: Sam/.test(t), 'v2.0: household allergen alert names the person (' + pea.title + ')');
    for (let i = 0; i < 8 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(150); }
    // inventory: expiring tomorrow -> cook this tonight
    const tm = await p.evaluate(() => { const d = new Date(); d.setDate(d.getDate() + 1); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); });
    await p.evaluate(tm => { window.PR_APP.addToPantry('spinach'); window.PR_APP.setUseBy('spinach', tm); window.PR_APP.showTab('pantry'); }, tm.replace(/T.*/, ''));
    await sleep(300);
    const ban = await p.$eval('#main', e => e.textContent);
    check(/Use by tomorrow: .*Spinach/i.test(ban) && /Cook this tonight:/.test(ban), 'v2.0: "use by tomorrow" banner with a cook-this-tonight pick');
    await p.evaluate(() => document.querySelector('[data-a="about"]').click()); await sleep(150);
    await tap(p, '[data-a="inv"]');
    t = await topText(p);
    check(/Pantry \d+/.test(t) && /Fridge \d+/.test(t) && /Freezer \d+/.test(t) && /Spice rack \d+/.test(t) && /Restock list/.test(t), 'v2.0: inventory shows locations with counts and a restock list');
    await tap(p, '[data-a="invLoc"][data-v="spice"]');
    t = await topText(p);
    check(/Spices to buy next/.test(t), 'v2.0: spice rack shows buy-next spices');
    await tap(p, '[data-a="back"]');
    // editor
    await tap(p, '[data-a="myRecipes"]'); await tap(p, '[data-a="edNew"]');
    await p.type('[data-ed="title"]', 'Test stew');
    await p.type('#edIng', '2 cups chickpeas\n1 onion\n1 tsp cum');
    await sleep(150);
    const sug = await p.$$eval('[data-region="edAc"] button', b => b.map(x => x.textContent));
    check(sug.some(x => /^cumin/i.test(x)), 'v2.0: editor autocompletes ingredient names (' + sug.slice(0, 3).join(' | ') + ')');
    await tap(p, '[data-region="edAc"] button');
    await p.type('[data-ed="steps"]', 'Cook it all.');
    await tap(p, '[data-a="edSave"]'); await sleep(250);
    t = await topText(p);
    check(/Test stew/.test(t) && /Cumin/i.test(t) && /Fits: .*Vegan/.test(t), 'v2.0: saved recipe opens with ingredients and diets');
    // budget / prices: switch to the US to see BLS labels
    for (let i = 0; i < 8 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(150); }
    await p.evaluate(() => document.querySelector('[data-a="about"]').click()); await sleep(150);
    await tap(p, '[data-a="region20"]'); await tap(p, '[data-a="regionSet"][data-v="us"]'); await tap(p, '#dlg .btn:not(.ghost)');
    t = await topText(p);
    check(/U\.S\. FDA major food allergens \(9\)/.test(t), 'v2.0: US region uses the FDA 9 allergen list');
    await tap(p, '[data-a="back"]'); await tap(p, '[data-a="prices20"]');
    await p.type('#zipIn', '10001'); await tap(p, '[data-a="zipSave"]');
    t = await topText(p);
    check(/Region: Northeast/.test(t) && /BLS Northeast regional average, Sep 2026/.test(t) && /U\.S\. average, Sep 2026 \u00d7 Northeast factor/.test(t), 'v2.0: ZIP 10001 -> Northeast with labelled BLS sources');
    await tap(p, '[data-a="back"]');
    // v2.0 Stage 2: kitchen helpers
    await tap(p, '[data-a="helpers20"]');
    t = await topText(p);
    check(/151 equivalents/.test(t) && /1 stick of butter/.test(t), 'v2.0 S2: equivalents table lists 151 rows');
    await p.type('[data-in="hq"]', 'gas mark 4'); await sleep(400);
    t = await topText(p);
    check(/180 C/.test(t) && !/1 stick of butter/.test(t), 'v2.0 S2: equivalents search filters (gas mark 4 -> 180 C)');
    check(await p.evaluate(() => document.activeElement && document.activeElement.getAttribute('data-in') === 'hq'), 'v2.0 S2: search keeps focus while typing');
    await tap(p, '[data-a="h20Tool"][data-v="rescue"]'); await tap(p, '[data-a="h20Open"][data-v="Too salty"]');
    t = await topText(p);
    check(/Try this/.test(t) && /Diluting is what actually works/.test(t), 'v2.0 S2: Rescue my dish opens fixes');
    await tap(p, '[data-a="h20Tool"][data-v="explain"]'); await p.type('[data-in="hq"]', 'courgette'); await sleep(400);
    await tap(p, '[data-a="h20Open"][data-v="Zucchini"]');
    t = await topText(p);
    check(/UK & Ireland: courgette/.test(t) && /US: zucchini \u2190 your region/.test(t), 'v2.0 S2: explainer finds a local name and marks your region');
    await tap(p, '[data-a="h20Tool"][data-v="times"]'); await p.type('[data-in="hq"]', 'salmon'); await sleep(400);
    t = await topText(p);
    check(/Salmon fillet = 12 to 15 min/.test(t), 'v2.0 S2: cooking times search');
    await tap(p, '[data-a="h20Tool"][data-v="blends"]'); await tap(p, '[data-a="h20Blend"][data-v="0"]');
    t = await topText(p);
    check(/Cajun-style blend/.test(t) && /3\/4 tsp Salt/.test(t) && /1 3\/8 tsp Paprika/.test(t), 'v2.0 S2: blend builder, 2 tbsp batch');
    await p.$eval('[data-change="h20Opt"][data-k="noSalt"]', e => e.click()); await sleep(200);
    t = await topText(p);
    check(/left out Salt/.test(t), 'v2.0 S2: salt-free leaves salt out');
    await tap(p, '[data-a="h20Batch"][data-v="2"]');
    t = await topText(p);
    check(/1 tbsp \+ 1\/4 tsp Paprika/.test(t), 'v2.0 S2: batch size rescales');
    await tap(p, '[data-a="back"]');
    // appearance: one preset group rendered at a time
    await tap(p, '[data-a="appearance"]');
    const tiles = await p.$$eval('.tgrid > *', e => e.length), groups = await p.$$eval('[data-a="themeGroup"]', e => e.length);
    check(groups === 12 && tiles > 0 && tiles < 60, 'v2.0: presets grouped (' + groups + ' groups), only ' + tiles + ' tiles in the DOM');
    await tap(p, '[data-a="themeGroup"][data-v="Game day"]');
    const gd = await p.$$eval('.tgrid > *', e => e.length);
    check(gd === 81, 'v2.0: Game day group has 81 presets (got ' + gd + ')');
    for (let i = 0; i < 8 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(150); }
    // discover: cuisine groups; diets filter
    await p.evaluate(() => window.PR_APP.showTab('discover')); await sleep(500);
    const dt = await p.$eval('#main', e => e.textContent);
    check(/Caribbean/.test(dt) && /Jamaican/.test(dt), 'v2.0: Discover groups cuisines by region (Caribbean)');
    await p.evaluate(() => window.PR_APP.showTab('recipes')); await sleep(300);
    const dchips = await p.$$eval('[data-a="diet"]', e => e.map(x => x.getAttribute('data-v')));
    check(dchips.length === 15 && dchips.indexOf('halal') >= 0 && dchips.indexOf('kosher') >= 0, 'v2.0: 15 diet filters incl. halal and kosher');
    await tap(p, '[data-a="diet"][data-v="keto"]'); await sleep(300);
    const ids = await p.$$eval('.rrow[data-id]', e => e.map(x => x.getAttribute('data-id')));
    const byIdD = new Map(DATA.map(r => [r.id, r]));
    check(ids.length > 0 && ids.every(id => !byIdD.has(id) || (byIdD.get(id).diets || []).indexOf('keto') >= 0), 'v2.0: keto filter lists only keto recipes (' + ids.length + ' shown)');
    // v2.0 Stage 4: fasting labels as optional diet chips (off by default)
    await tap(p, '[data-a="diet"][data-v="keto"]'); await sleep(200);
    check(await p.$$eval('[data-a="diet"][data-v^="fast:"]', e => e.length) === 0, 'v2.0 S4: no fasting chips by default');
    await p.evaluate(() => window.PR_APP.openScreen({ type: 'holidays', tab: 'fast' })); await sleep(300);
    const fid = await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const i = l.querySelector('[data-change="fastPick"]'); if (!i.checked) i.click(); const sw = l.querySelector('[data-k="fastChips"]'); if (!sw.checked) sw.click(); return i.getAttribute('data-v'); });
    await sleep(200);
    for (let i = 0; i < 4 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(200); }
    await p.evaluate(() => window.PR_APP.showTab('recipes')); await sleep(300);
    check(await p.$$eval('[data-a="diet"][data-v="fast:' + fid + '"]', e => e.length) === 1, 'v2.0 S4: ticked fasting label shows as a filter chip (' + fid + ')');
    await tap(p, '[data-a="diet"][data-v="fast:' + fid + '"]'); await sleep(300);
    const fOk = await p.evaluate(f => { const S = PR.Stage3, d = S.data(), fx = d.fasts.filter(x => x.id === f)[0]; const rs = window.PR_APP.filteredResults(); return { n: rs.length, all: rs.every(x => S.fits(fx, x.recipe)) }; }, fid);
    check(fOk.all, 'v2.0 S4: fasting chip keeps only recipes that fit the label (' + fOk.n + ' shown)');
    await p.evaluate(() => window.PR_APP.openScreen({ type: 'holidays', tab: 'fast' })); await sleep(300);
    await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const i = l.querySelector('[data-change="fastPick"]'); if (i.checked) i.click(); });
    await sleep(200);
    for (let i = 0; i < 4 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(200); }
    await p.evaluate(() => window.PR_APP.showTab('recipes')); await sleep(300);
    check(await p.$$eval('[data-a="diet"][data-v^="fast:"]', e => e.length) === 0 && !(await p.evaluate(() => JSON.parse(localStorage.getItem('pr.prefs')).diets.some(x => /^fast:/.test(x)))), 'v2.0 S4: unticking the label removes the chip and its selection');
    // ---- v2.0.1: Discover never uses the Recipes tab filters ----
    const discIds = async () => { await p.evaluate(() => window.PR_APP.showTab('discover')); await sleep(400); return p.$$eval('#main .car [data-id], #main .buynext [data-key]', e => e.map(x => x.getAttribute('data-id') || x.getAttribute('data-key')).join('|')); };
    const before = await discIds();
    await p.evaluate(() => window.PR_APP.showTab('recipes')); await sleep(300);
    await tap(p, '[data-a="mealType"][data-v="Breakfast"]'); await sleep(200);
    await tap(p, '[data-a="diet"][data-v="vegan"]'); await sleep(200);
    if (await p.$('[data-a="methodF"][data-v="oven"]')) { await tap(p, '[data-a="methodF"][data-v="oven"]'); await sleep(200); }
    const recipesFiltered = await p.evaluate(() => window.PR_APP.filteredResults().length);
    const after = await discIds();
    check(before.length > 0 && after === before, 'v2.0.1: Discover lists unchanged by Recipes meal-type, diet and method filters (' + recipesFiltered + ' in Recipes)');
    check(await p.$$eval('#main [data-a="dMealType"].on', e => e.length) === 0 && await p.$$eval('#main [data-a="mealType"]', e => e.length) === 0, 'v2.0.1: Discover meal chips are its own (Recipes Breakfast not shown as selected)');
    await tap(p, '#main [data-a="dMealType"][data-v="Breakfast"]'); await sleep(400);
    const dBreak = await p.$$eval('#main .car [data-id]', e => e.map(x => x.getAttribute('data-id')));
    const prefsNow = await p.evaluate(() => JSON.parse(localStorage.getItem('pr.prefs')));
    check(JSON.stringify(prefsNow.dMeals) === '["Breakfast"]' && JSON.stringify(prefsNow.meals) === '["Breakfast"]', 'v2.0.1: Discover meal filter is stored separately (dMeals)');
    const dOk = await p.evaluate(ids => ids.every(id => (window.PR_APP.mealsOf(id) || []).indexOf('Breakfast') >= 0), dBreak);
    check(dOk, 'v2.0.1: Discover Breakfast chip filters Discover lists (' + dBreak.length + ')');
    await tap(p, '#main [data-a="dMealAll"]'); await sleep(300);
    check(await discIds() === before, 'v2.0.1: Discover back to its own unfiltered lists');
    await p.evaluate(() => window.PR_APP.showTab('recipes')); await sleep(300);
    await tap(p, '[data-a="diet"][data-v="vegan"]'); await tap(p, '[data-a="mealAll"]');
    if (await p.$('[data-a="methodF"][data-v="oven"].on')) await tap(p, '[data-a="methodF"][data-v="oven"]');
    // ---- v2.0.1: medical diet guides (off by default) ----
    const potatoId = await p.evaluate(() => { const r = window.PR_APP.matchAll().map(x => x.recipe).filter(r => r.ingredients.some(i => i.key === 'potato') && r.ingredients.some(i => i.key === 'milk'))[0]; return r && r.id; });
    await p.evaluate(id => window.PR_APP.openDetail(id), potatoId); await sleep(400);
    check(await p.$('.screen:last-child .medcard') === null && await p.$('.medcard') === null, 'v2.0.1: no diet-guide card by default');
    for (let i = 0; i < 4 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(200); }
    await p.evaluate(() => window.PR_APP.openScreen({ type: 'med' })); await sleep(300);
    const medTxt = await p.evaluate(() => [...document.querySelectorAll('.screen')].pop().textContent);
    check(/This is a general guide, not medical advice\. Limits are different for every person and stage of illness\. Follow the plan from your doctor or dietitian\./.test(medTxt), 'v2.0.1: guides screen shows the disclaimer');
    check(/Renal/.test(medTxt) && /Cardiac/.test(medTxt) && /Diabetic/.test(medTxt) && !/\bsafe\b/i.test(medTxt), 'v2.0.1: three guides listed, never "safe"');
    check(await p.evaluate(() => [...[...document.querySelectorAll('.screen')].pop().querySelectorAll('[data-change="medPick"]')].every(i => !i.checked)), 'v2.0.1: guides off by default');
    await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); l.querySelector('[data-change="medPick"][data-v="renal"]').click(); l.querySelector('[data-change="medPick"][data-v="diabetic"]').click();
      const i = l.querySelector('[data-k="medSodiumMg"]'); i.value = '600'; i.dispatchEvent(new Event('change', { bubbles: true })); const j = l.querySelector('[data-k="medCarbsG"]'); j.value = '45'; j.dispatchEvent(new Event('change', { bubbles: true })); });
    await sleep(200);
    for (let i = 0; i < 4 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(200); }
    await p.evaluate(id => window.PR_APP.openDetail(id), potatoId); await sleep(400);
    const card = await p.evaluate(() => { const c = document.querySelector('.medcard'); return c ? c.textContent : ''; });
    check(/contains high-potassium ingredients?: [^.]*potato/.test(card) && /high-phosphorus/.test(card) && /Carbs/.test(card) && /not medical advice/.test(card) && /estimates/.test(card) && !/\bsafe\b/i.test(card), 'v2.0.1: recipe page shows renal + diabetic flags, estimates and the disclaimer');
    const bk201 = JSON.parse(await p.evaluate(() => window.PR_APP.backupText()));
    check(JSON.stringify(bk201.settings.medGuides) === '["renal","diabetic"]' && bk201.settings.medSodiumMg === 600 && bk201.settings.medCarbsG === 45, 'v2.0.1: guides and limits are in the backup');
    check(!/apikey|apiKey/.test(JSON.stringify(bk201.settings.medGuides || '')), 'v2.0.1: no secrets in the guide keys');
    for (let i = 0; i < 4 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(200); }
    // ---- v2.1: botanical fruits, category moves, glossary ----
    await p.evaluate(() => window.PR_APP.showTab('pantry')); await sleep(300);
    await p.evaluate(() => { const i = document.querySelector('#quickIn'); i.value = 'botanical'; i.dispatchEvent(new Event('input', { bubbles: true })); }); await sleep(400);
    const botList = await p.evaluate(() => [...document.querySelectorAll('#main [data-region="grid"] .chip, #main .grid .chip')].filter(c => c.offsetParent).map(c => c.textContent.trim()));
    check(['Tomato', 'Avocado', 'Cucumber', 'Eggplant', 'Zucchini', 'Bell pepper'].every(n => botList.some(t => t.indexOf(n) >= 0)) && !botList.some(t => /Carrot|Apple/.test(t)), 'v2.1: "botanical" filter lists botanical fruits only (' + botList.length + ')');
    await p.evaluate(() => { const i = document.querySelector('#quickIn'); i.value = ''; i.dispatchEvent(new Event('input', { bubbles: true })); }); await sleep(300);
    await p.evaluate(() => { const b = [...document.querySelectorAll('#main [data-ci="0"]')][0]; if (b && b.getAttribute('aria-expanded') !== 'true') b.click(); }); await sleep(300);
    const chipBtn = await p.$('#main [data-a="botFilter"]');
    check(!!chipBtn, 'v2.1: Botanical fruits filter chip in Vegetables');
    if (chipBtn) { await chipBtn.click(); await sleep(300); check(await p.$eval('#quickIn', i => i.value) === 'botanical fruits', 'v2.1: chip fills the filter'); await p.evaluate(() => { const i = document.querySelector('#quickIn'); i.value = ''; i.dispatchEvent(new Event('input', { bubbles: true })); }); await sleep(200); }
    // badge + category move from the pantry item menu
    await p.evaluate(() => { const i = document.querySelector('#quickIn'); i.value = 'tomato'; i.dispatchEvent(new Event('input', { bubbles: true })); }); await sleep(300);
    await p.evaluate(() => { const c = [...document.querySelectorAll('#main [data-label]')].find(c => c.getAttribute('data-label') === 'Tomato'); if (c) c.click(); }); await sleep(300);
    await p.evaluate(() => { const i = document.querySelector('#quickIn'); if (i) { i.value = ''; i.dispatchEvent(new Event('input', { bubbles: true })); } }); await sleep(200);
    await p.evaluate(() => window.PR_APP.showTab('pantry')); await sleep(300);
    await p.evaluate(() => { const r = [...document.querySelectorAll('#main [data-a="pmenu"]')].find(e => e.getAttribute('data-key') === 'tomato'); if (r) r.click(); }); await sleep(300);
    const sheetTxt = await p.evaluate(() => { const s = document.querySelector('.sheet'); return s ? s.textContent : ''; });
    check(/Botanically a fruit/.test(sheetTxt) && /Category: Fruits/.test(sheetTxt), 'v2.1: tomato menu shows the botanical badge and its category (' + sheetTxt.slice(0, 80) + ')');
    await p.evaluate(() => { const b = [...document.querySelectorAll('.sheet button')].find(b => /Category:/.test(b.textContent)); if (b) b.click(); }); await sleep(300);
    await p.evaluate(() => { const b = document.querySelector('[data-a="catPick"][data-v="Vegetables"]'); if (b) b.click(); }); await sleep(400);
    const bk21 = JSON.parse(await p.evaluate(() => window.PR_APP.backupText()));
    check(bk21.settings.catMoves && bk21.settings.catMoves.tomato === 'Vegetables', 'v2.1: moving tomato to Vegetables is saved and backed up');
    await p.evaluate(() => window.PR_APP.openScreen({ type: 'catmoves' })); await sleep(300);
    const cmTxt = await p.evaluate(() => [...document.querySelectorAll('.screen')].pop().textContent);
    check(/Tomato/.test(cmTxt) && /Vegetables/.test(cmTxt) && /Nix v\. Hedden/.test(cmTxt), 'v2.1: categories screen lists the move and the glossary fun fact');
    await p.evaluate(() => { const b = [...document.querySelectorAll('.screen')].pop().querySelector('[data-a="catReset"]'); if (b) b.click(); }); await sleep(300);
    check(!JSON.parse(await p.evaluate(() => window.PR_APP.backupText())).settings.catMoves, 'v2.1: Reset all clears the moves');
    for (let i = 0; i < 4 && (await p.evaluate(() => window.PR_APP.state().stack.length)); i++) { await p.evaluate(() => { const l = [...document.querySelectorAll('.screen')].pop(); const b = l && l.querySelector('[data-a="back"]'); if (b) b.click(); }); await sleep(200); }
    await p.evaluate(() => { const d = document.querySelector('.dialog [data-a="dlgBtn"], .dialog button'); if (d) d.click(); });
    // chips wrap long names at a narrow width with large text
    await p.setViewport({ width: 320, height: 640 });
    await p.evaluate(() => { document.documentElement.style.setProperty('--fs', '1.5'); window.PR_APP.showTab('pantry'); }); await sleep(400);
    const clip = await p.evaluate(() => [...document.querySelectorAll('.chips:not(.nowrap) .chip')].filter(c => c.offsetParent).map(c => ({ t: c.textContent, sw: c.scrollWidth, cw: c.clientWidth, sh: c.scrollHeight, ch: c.clientHeight, r: c.getBoundingClientRect().right })).filter(x => x.sw > x.cw + 1 || x.sh > x.ch + 1 || x.r > 321).map(x => x.t));
    check(clip.length === 0, 'v2.0.1: no clipped chips at 320 px and 150% text' + (clip.length ? ': ' + clip.slice(0, 5).join(' / ') : ''));
    await p.evaluate(() => document.documentElement.style.removeProperty('--fs')); await p.setViewport({ width: 1100, height: 900 });
    const bad5 = e5.filter(e => !/net::|Failed to load resource/i.test(e));
    check(bad5.length === 0, 'v2.0: no console errors' + (bad5.length ? ': ' + bad5.join(' / ') : ''));
    await c5.close();
  }
  await browser.close();
  console.log(fails ? fails + ' FAILED' : 'ALL PASSED');
  process.exit(fails ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
