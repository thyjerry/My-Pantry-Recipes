// Automated accessibility audit (v2.0): 46 views; accessible names, input labels, img alt, 44px targets, AA contrast. Usage: NODE_PATH=... node tests/a11y-audit.js http://127.0.0.1:8765/ [themeId]. Prints {} when there are no findings. MIT License.
// Automated a11y audit of every web screen: accessible names, touch targets (>=44 CSS px), text contrast (AA), img alt.
const puppeteer = require('puppeteer-core');
const url = process.argv[2]; const theme = process.argv[3] || '';
const sleep = ms => new Promise(r => setTimeout(r, ms));
(async () => {
  const b = await puppeteer.launch({ executablePath: '/usr/bin/google-chrome', headless: 'new', args: ['--no-sandbox', '--disable-gpu'] });
  const p = await b.newPage();
  await p.setViewport({ width: 800, height: 1280, deviceScaleFactor: 1 });
  await p.evaluateOnNewDocument(() => { window.PR_TEST_NO_WELCOME = true; });
  await p.goto(url, { waitUntil: 'load' }); await sleep(3000);
  await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().loaded, { timeout: 60000 });
  await p.evaluate(() => ['chicken', 'garlic', 'onion', 'rice', 'egg'].forEach(x => window.PR_APP.addToPantry(x)));
  if (theme) { await p.evaluate(t => localStorage.setItem('pr.theme', JSON.stringify(JSON.stringify({ active: t }))), theme); await p.reload({ waitUntil: 'load' }); await sleep(1500); await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().loaded, { timeout: 60000 }); }
  console.error('dark?', await p.evaluate(() => document.documentElement.style.colorScheme));
  const firstId = await p.evaluate(() => window.PR_APP.filteredResults()[0].recipe.id);
  async function ready() { for (let i = 0; i < 4; i++) { try { await p.goto(url, { waitUntil: 'load' }); await p.waitForFunction(() => window.PR_APP && window.PR_APP.state().loaded, { timeout: 60000 }); await sleep(300); await p.evaluate(() => 1); return; } catch (e) { await sleep(500); } } }
  const views = [];
  for (const t of ['pantry', 'recipes', 'discover', 'plan', 'shopping', 'favorites']) views.push({ name: 'tab:' + t, go: `window.PR_APP.showTab('${t}')` });
  const scr = ['about', 'appearance', 'avoid', 'backup', 'conv', 'household', 'inv', 'ipsheet', 'myRecipes', 'prices', 'prices20', 'region', 'regional', 'subs'];
  for (const s of scr) views.push({ name: s, go: `window.PR_APP.openScreen({type:'${s}', loc:'pantry', q:'', cat:'', msg:'', sel:{}, text:'', parsed:null, err:''})` });
  for (const t of ['equiv', 'rescue', 'blends', 'times', 'explain']) views.push({ name: 'helpers20:' + t, go: `window.PR_APP.openScreen({type:'helpers20', tool:'${t}', q:'', open:'', blend:null})` });
  for (const t of ['cal', 'pick', 'fast']) views.push({ name: 'holidays:' + t, go: `window.PR_APP.openScreen({type:'holidays', tab:'${t}'})` });
  for (const t of ['recipes', 'pct', 'pan', 'time', 'oven', 'party', 'starter', 'log', 'fix', 'store']) views.push({ name: 'baking:' + t, go: `window.PR_APP.openScreen({type:'baking', tab:'${t}'})` });
  views.push({ name: 'importRecipe', go: `window.PR_APP.openScreen({type:'importRecipe', text:'', parsed:null, err:''})` });
  views.push({ name: 'editor', go: `window.PR_APP.openScreen({type:'editor', draft: PR.RecipeEditor.newDraft(), id:''})` });
  views.push({ name: 'detail', go: `window.PR_APP.openDetail('${firstId}')` });
  views.push({ name: 'cook', go: `window.PR_APP.openScreen({type:'cook', id:'${firstId}', servings:2, step:0, ing:false, wl:'pending', timers:[]})` });
  views.push({ name: 'welcome', go: `window.PR_APP.openScreen({type:'welcome', step:0, guess:''})` });
  views.push({ name: 'scan', go: `window.PR_APP.openScreen({type:'scan', code:'', state:'', camOk:false, cam:false})` });
  const all = [];
  for (const v of views) {
    await ready();

    try { await p.evaluate(v.go); await sleep(350); await p.evaluate(() => 1); } catch (e) { all.push({ view: v.name, kind: 'open-error', what: String(e).slice(0, 120) }); continue; }
    await sleep(350);
    const res = await p.evaluate((vn) => {
      const out = [];
      const root = [...document.querySelectorAll('.screen')].pop() || document.body;
      const vis = el => { const r = el.getBoundingClientRect(); const cs = getComputedStyle(el); return r.width > 0 && r.height > 0 && cs.visibility !== 'hidden' && cs.display !== 'none'; };
      const desc = el => { const t = el.tagName.toLowerCase(); return t + (el.className && typeof el.className === 'string' ? '.' + el.className.split(' ').join('.') : '') + (el.getAttribute('data-a') ? '[a=' + el.getAttribute('data-a') + ']' : '') + (el.getAttribute('data-change') ? '[c=' + el.getAttribute('data-change') + ']' : '') + ' "' + (el.getAttribute('aria-label') || el.textContent || '').trim().slice(0, 30) + '"'; };
      const name = el => {
        if (el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || el.title) return true;
        if ((el.textContent || '').trim()) return true;
        if (el.querySelector('img[alt]:not([alt=""])')) return true;
        if (el.labels && el.labels.length && [...el.labels].some(l => l.textContent.trim())) return true;
        if (el.closest('label') && el.closest('label').textContent.trim()) return true;
        if (el.placeholder) return true;
        return false;
      };
      const inter = [...root.querySelectorAll('button, a[href], input:not([type=hidden]), select, textarea, [role=button], [data-a], [tabindex]:not([tabindex="-1"])')].filter(vis);
      for (const el of inter) {
        if (!name(el)) out.push({ kind: 'no-name', what: desc(el) });
        const r = el.getBoundingClientRect();
        const lab = el.closest('label');
        let tgt = lab ? lab.getBoundingClientRect() : r;
        const af = getComputedStyle(el, '::after');
        if (!lab && af.content && af.content !== 'none' && af.position === 'absolute') { const t = -parseFloat(af.top) || 0, l = -parseFloat(af.left) || 0; tgt = { width: r.width + 2 * l, height: r.height + 2 * t }; }
        const inline = getComputedStyle(el).display === 'inline' && el.closest('p, .note, .faint, .muted, li');
        if (!inline && (tgt.height < 43.5 || tgt.width < 43.5)) out.push({ kind: 'small', what: desc(el) + ' ' + Math.round(tgt.width) + 'x' + Math.round(tgt.height) });
        if ((el.tagName === 'INPUT' && !['checkbox', 'radio', 'range', 'button', 'submit'].includes(el.type)) || el.tagName === 'SELECT' || el.tagName === 'TEXTAREA') {
          if (!el.getAttribute('aria-label') && !(el.labels && el.labels.length) && !el.closest('label') && !el.title) out.push({ kind: 'input-no-label', what: desc(el) + ' ph=' + (el.placeholder || '') });
        }
      }
      for (const im of root.querySelectorAll('img')) if (vis(im) && !im.hasAttribute('alt')) out.push({ kind: 'img-no-alt', what: im.src.slice(-40) });
      // contrast
      const parse = c => { const m = c.match(/rgba?\(([^)]+)\)/); if (!m) return null; const v = m[1].split(',').map(Number); return { r: v[0], g: v[1], b: v[2], a: v.length > 3 ? v[3] : 1 }; };
      const lum = c => { const f = x => { x /= 255; return x <= 0.03928 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c.r) + 0.7152 * f(c.g) + 0.0722 * f(c.b); };
      const bgOf = el => { let layers = []; for (let e = el; e; e = e.parentElement) { const cs = getComputedStyle(e); if (cs.backgroundImage && cs.backgroundImage !== 'none') return null; const c = parse(cs.backgroundColor); if (c && c.a > 0) { layers.push(c); if (c.a >= 1) break; } } let base = { r: 255, g: 255, b: 255 }; for (let i = layers.length - 1; i >= 0; i--) { const c = layers[i]; base = { r: c.r * c.a + base.r * (1 - c.a), g: c.g * c.a + base.g * (1 - c.a), b: c.b * c.a + base.b * (1 - c.a) }; } return base; };
      const seen = new Set();
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
      let n;
      while ((n = walker.nextNode())) {
        if (!n.textContent.trim()) continue;
        const el = n.parentElement; if (!el || seen.has(el) || !vis(el)) continue; seen.add(el);
        if (el.closest('[aria-hidden="true"], svg, button:disabled, [disabled]')) continue;
        const cs = getComputedStyle(el); const fg = parse(cs.color); const bg = bgOf(el); if (!fg || !bg) continue;
        let op = 1; for (let e = el; e; e = e.parentElement) op *= +getComputedStyle(e).opacity;
        const a = fg.a * op; const f = { r: fg.r * a + bg.r * (1 - a), g: fg.g * a + bg.g * (1 - a), b: fg.b * a + bg.b * (1 - a) };
        const L1 = lum(f), L2 = lum(bg); const ratio = (Math.max(L1, L2) + 0.05) / (Math.min(L1, L2) + 0.05);
        const px = parseFloat(cs.fontSize), bold = +cs.fontWeight >= 700; const large = px >= 24 || (bold && px >= 18.66);
        if (ratio < (large ? 3 : 4.5) - 0.02) out.push({ kind: 'contrast', what: desc(el) + ' ' + ratio.toFixed(2) + ' ' + cs.color + ' on ' + JSON.stringify(bg) + ' ' + px + 'px' });
      }
      return out.map(o => Object.assign({ view: vn }, o));
    }, v.name);
    all.push(...res);
  }
  const by = {};
  for (const x of all) { const k = x.kind + ' | ' + x.what.replace(/\d+x\d+$/, m => m); by[k] = (by[k] || []); if (by[k].length < 4) by[k].push(x.view); else by[k].more = (by[k].more || 0) + 1; }
  const kinds = {}; for (const x of all) kinds[x.kind] = (kinds[x.kind] || 0) + 1;
  console.log(JSON.stringify(kinds));
  for (const k of Object.keys(by).sort()) console.log(k, '::', by[k].join(','), by[k].more ? '+' + by[k].more : '');
  await b.close();
})();
