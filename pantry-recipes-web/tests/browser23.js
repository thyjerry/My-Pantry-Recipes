'use strict';
const puppeteer=require('puppeteer-core');
(async()=>{
 const browser=await puppeteer.launch({executablePath:'/usr/bin/google-chrome',headless:'new',args:['--no-sandbox','--disable-gpu']});
 let checks=0;const check=(v,m)=>{if(!v)throw Error(m);checks++;console.log('PASS '+m);};
 try{
 const p=await browser.newPage();await p.setViewport({width:390,height:844});
 const errors=[];p.on('pageerror',e=>errors.push(String(e)));
 await p.evaluateOnNewDocument(()=>{window.PR_TEST_NO_WELCOME=true;});
 await p.setBypassServiceWorker(true);await p.setRequestInterception(true);let imageRequests=0;
 p.on('request',r=>{if(r.url().startsWith('https://www.themealdb.com/images/')){imageRequests++;return r.respond({status:200,contentType:'image/gif',body:Buffer.from('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7','base64')});} if(!r.url().startsWith('http://127.0.0.1:8765/')&&!r.url().startsWith('data:'))return r.abort();r.continue();});
 await p.goto(process.argv[2]||'http://127.0.0.1:8765/');await p.waitForFunction(()=>window.PR_APP&&PR_APP.state().complete);
 check(await p.$eval('.top [data-a="about"]',e=>e.textContent.trim()==='Settings'&&e.getBoundingClientRect().height>=48),'Settings is labeled, 48px target');
 check(!(await p.$('.top [data-a="subs"]')),'no standalone ambiguous substitution icon');
 await p.click('.top [data-a="about"]');check(!!(await p.$('.screen [data-a="subs"]')),'Settings retains labeled ingredient substitution tool');
 await p.evaluate(async()=>{
  const raw=await(await fetch('data/recipes.json')).json(); const rows=Array.isArray(raw)?raw:raw.recipes;
  const r=rows.find(r=>r.ingredients&&r.ingredients.some(i=>/milk|butter|egg/i.test(i.name)));
  r.id='m:fixture';r.title='Associated photo fixture';r.source='TheMealDB';r.image='https://www.themealdb.com/images/media/meals/fixture.jpg';
  PR_APP.Cache.put([{id:r.id,json:JSON.stringify(r),fetched:Date.now()}],[]);
  PR_APP.Store.set('favorites',[{id:r.id,at:Date.now()}]); const prefs=PR_APP.Store.get('prefs',{});prefs.online=true;prefs.welcomed=true;PR_APP.Store.set('prefs',prefs);
 });
 await p.waitForFunction(async()=> (await PR_APP.Cache.all()).some(r=>r.id==='m:fixture'));
 await p.reload();await p.waitForFunction(()=>window.PR_APP&&PR_APP.state().complete);await p.evaluate(()=>PR_APP.showTab('favorites'));
 await p.waitForSelector('#main .recipephoto img');await p.$eval('#main .recipephoto img',e=>e.scrollIntoView());await p.waitForFunction(()=>document.querySelector('#main .recipephoto img').naturalWidth>0);
 check(imageRequests>0,'associated recipe photo requested and displayed');
 check(!(await p.$('#main svg.illo')),'no generic illustrations on cards');
 await p.evaluate(()=>PR_APP.openDetail('m:fixture'));await p.waitForSelector('.screen [data-a="subDlg"]');
 const selected=await p.$eval('.screen [data-a="subDlg"]',e=>e.getAttribute('data-selected'));
 await p.click('.screen [data-a="subDlg"]');
 const text=await p.$eval('#dlg',e=>e.textContent);check(text.includes(selected),'dialog identifies exact selected ingredient and amount');
 check(/Same-unit estimate|ratios are by volume or count|Add gradually to taste/.test(text),'dialog shows quantity guidance without inventing mixture amounts');
 check(/Check ingredient labels/.test(text),'diet and cooking caution present');
 await p.evaluate(()=>{const prefs=PR_APP.Store.get('prefs',{});prefs.online=false;PR_APP.Store.set('prefs',prefs);});
 const before=imageRequests;await p.reload();await p.waitForFunction(()=>window.PR_APP&&PR_APP.state().complete);await p.evaluate(()=>PR_APP.showTab('favorites'));
 check(!(await p.$('#main .recipephoto img'))&&imageRequests===before,'online recipes off: no new card photo requests');
 check(await p.$eval('#main',e=>e.textContent.includes('No photo')),'restrained fallback when photo unavailable or disabled');
 await p.evaluate(async()=>{
  const prefs=PR_APP.Store.get('prefs',{});prefs.online=true;PR_APP.Store.set('prefs',prefs);
  const bytes=Uint8Array.from(atob('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'),c=>c.charCodeAt(0));
  const cache=await caches.open('pr-images');await cache.put('https://www.themealdb.com/images/media/meals/fixture.jpg/preview',new Response(bytes,{headers:{'Content-Type':'image/gif'}}));
  await navigator.serviceWorker.ready;
 });
 await p.setBypassServiceWorker(false);await p.setOfflineMode(true);await p.reload();
 await p.waitForFunction(()=>window.PR_APP&&PR_APP.state().complete);await p.evaluate(()=>PR_APP.showTab('favorites'));
 await p.waitForSelector('#main .recipephoto img');await p.$eval('#main .recipephoto img',e=>e.scrollIntoView());
 await p.waitForFunction(()=>document.querySelector('#main .recipephoto img').naturalWidth>0);
 check(true,'offline card loads previously cached associated photo (synthetic fixture)');
 check(errors.length===0,'no JavaScript errors');
 console.log(checks+' browser23 checks, 0 failures');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
