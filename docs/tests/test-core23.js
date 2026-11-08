'use strict';
const fs = require('fs'), path = require('path'), ROOT = path.join(__dirname,'..');
global.window=global;global.self=global;
require(path.join(ROOT,'js/core-data.js'));global.PR=require(path.join(ROOT,'js/core.js'));
require(path.join(ROOT,'js/core15.js'));require(path.join(ROOT,'js/core16.js'));require(path.join(ROOT,'js/core17.js'));
const S=PR.Substitutions, subs=S.parse(fs.readFileSync(path.join(ROOT,'data/substitutions.txt'),'utf8'));
let checks=0; function check(v,m){checks++;if(!v)throw Error(m);}
subs.entries.forEach(e=>e.options.forEach(o=>{
 check(!!S.caution(o,[],[],['vegan'])===!o.vegan,'vegan guard');
 [['vegetarian','vegetarian'],['dairy-free','dairyFree'],['gluten-free','glutenFree']].forEach(([d,f])=>check(!!S.caution(o,[],[],[d])===!o[f],d+' guard'));
 o.allergens.forEach(a=>check(!!S.caution(o,[],[a],[]),'allergen guard '+a));
 if(o.needs.length===1&&S.ratioValue(o.ratio)>0)check(S.quantityAdvice(o,2,'cup').includes(PR.formatQty(2*S.ratioValue(o.ratio),'cup',1)),'scaled volume ratio');
 check(!S.quantityAdvice(o,-1,'cup').startsWith('Replacement estimate'),'unknown amount');
 check(!S.quantityAdvice(o,100,'g').startsWith('Replacement estimate'),'no unsupported mass ratio');
 o.needs.forEach(g=>g.forEach(k=>check(!!S.caution(o,[k],[],[]),'dislike')));
 if(o.needs.length>1)check(!S.quantityAdvice(o,2,'cup').startsWith('Replacement estimate'),'compound mixture');
 const pantry=[].concat(...o.needs), m=new PR.Matcher(pantry,[]).withSubstitutions(subs,[]);
 m.swapGuard=x=>!S.caution(x,pantry,[],[]);
 check(subs.availableFor(e.keys[0],m).length===0,'matching does not accept avoided replacements');
}));
check(subs.search('chicken thighs').every(e=>!/pork/i.test(e.name)),'no unrelated meat');
console.log('core23: '+checks+' checks, 0 failures');
