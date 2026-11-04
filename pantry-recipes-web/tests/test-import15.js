// Recipe import (web only): pasted plain text and schema.org Recipe JSON-LD. Run: node tests/test-import15.js
global.window = global; global.self = global;
require('../js/core-data.js'); const PR = require('../js/core.js'); global.PR = PR; require('../js/core15.js'); require('../js/import15.js');
const RI = PR.RecipeImport;
let n = 0, fails = 0;
function eq(a, b, m) { n++; const A = JSON.stringify(a), B = JSON.stringify(b); if (A !== B) { fails++; console.log('FAIL ' + m + '\n  got ' + A + '\n  want ' + B); } }
function ok(c, m) { eq(!!c, true, m); }
function throws(f, re, m) { n++; try { f(); fails++; console.log('FAIL (no throw) ' + m); } catch (e) { if (!e.recipeImport || !re.test(e.message)) { fails++; console.log('FAIL ' + m + ': ' + e.message); } } }
function ing(l) { const x = RI.ingredientLine(l); return x && [x.qty === undefined ? null : x.qty, x.unit || '', x.name]; }

// ingredient lines
eq(ing('2-3 cloves garlic, crushed'), [2, 'cloves', 'Garlic, crushed'], 'range + unit');
eq(ing('1 can (400 g) chickpeas'), [1, 'can', 'Chickpeas (400 g)'], 'leading parenthetical moved');
eq(ing('1. 2 onions'), [2, '', 'Onions'], 'numbered prefix stripped');
eq(ing('- 200 g flour')[2], 'Flour', 'bullet stripped');
eq(ing('\u2022 1 tsp salt')[1], 'tsp', 'bullet + tsp');
eq(ing('   '), null, 'blank line');
ok(Math.abs(ing('\u00bd cup milk')[0] - 0.5) < 1e-9, 'unicode fraction');
ok(Math.abs(ing('1 1/2 cups sugar')[0] - 1.5) < 1e-9, 'mixed fraction');
eq(ing('Salt and pepper')[2], 'Salt and pepper', 'no quantity');
// durations
eq(RI.isoMinutes('PT1H30M'), 90, 'iso 1h30');
eq(RI.isoMinutes('PT45M'), 45, 'iso 45m');
eq(RI.isoMinutes('P1DT2H'), 1560, 'iso days');
eq(RI.isoMinutes('PT'), -1, 'iso empty');
eq(RI.isoMinutes('soon'), -1, 'iso junk');

// plain text with headings
const txt = `Lentil Soup
Prep: 10 min
Cook: 1 hr 5 mins
Serves 6

Ingredients
- 1 cup red lentils
- 2 carrots, diced
- 1 onion
- 1 l vegetable stock

Method
1. Fry the onion and carrots.
2. Add lentils and stock; simmer 30 minutes.`;
let r = RI.parse(txt);
eq(r.format, 'text', 'text format');
eq(r.recipe.title, 'Lentil Soup', 'text title');
eq([r.recipe.prep, r.recipe.cook, r.recipe.servings], [10, 65, 6], 'text times/servings');
eq(r.recipe.ingredients.map(i => i.name), ['Red lentils', 'Carrots, diced', 'Onion', 'Vegetable stock'], 'text ingredient names');
eq(r.recipe.steps, ['Fry the onion and carrots.', 'Add lentils and stock; simmer 30 minutes.'], 'text steps');
const rec = RI.toRecipe(r.recipe, 'u:1');
eq(rec.id, 'u:1', 'toRecipe id');
ok(rec.ingredients.length === 4 && rec.steps.length === 2, 'toRecipe content');
ok(Array.isArray(rec.tags), 'toRecipe tags');
// round-trip through the backup recipe map
const back = PR.recipeFromMap(PR.recipeToMap(rec));
eq([back.id, back.title, back.ingredients.length], ['u:1', 'Lentil Soup', 4], 'recipe map round trip');

// plain text without headings
r = RI.parse('Quick Toast\n2 slices bread\n1 tbsp butter\nToast the bread and butter it.');
eq(r.recipe.ingredients.map(i => i.name), ['Bread', 'Butter'], 'headingless ingredients');

// JSON-LD inside HTML, with @graph, HowToSection, entities
const html = `<html><head><script type="application/ld+json">{"@context":"https://schema.org","@graph":[{"@type":"WebPage","name":"x"},
{"@type":["Recipe"],"name":"Tomato &amp; Bean Soup","prepTime":"PT15M","cookTime":"PT45M","recipeYield":["4 bowls"],"recipeCategory":"Lunch, Soup","recipeCuisine":"Italian",
"url":"https://example.org/soup",
"recipeIngredient":["2 tbsp olive oil","1 onion, chopped","400 g tinned tomatoes","1 can cannellini beans"],
"recipeInstructions":[{"@type":"HowToSection","name":"Cook","itemListElement":[{"@type":"HowToStep","text":"Soften the <b>onion</b>."},{"@type":"HowToStep","text":"Add the rest &amp; simmer."}]}]}]}</script></head></html>`;
r = RI.parse(html);
eq(r.format, 'json-ld', 'json-ld format');
eq(r.recipe.title, 'Tomato & Bean Soup', 'entity decoded');
eq([r.recipe.prep, r.recipe.cook, r.recipe.servings], [15, 45, 4], 'json-ld times');
eq(r.recipe.steps, ['Soften the onion.', 'Add the rest & simmer.'], 'HowToSection steps, tags stripped');
eq(r.recipe.ingredients.length, 4, 'json-ld ingredients');
eq(r.recipe.sourceUrl, 'https://example.org/soup', 'source url kept');
// totalTime only
r = RI.parse(JSON.stringify({ '@type': 'Recipe', name: 'T', totalTime: 'PT1H', recipeIngredient: ['1 egg'], recipeInstructions: 'Boil it.' }));
ok(Math.max(0, r.recipe.prep) + Math.max(0, r.recipe.cook) === 60, 'totalTime used (' + r.recipe.prep + '+' + r.recipe.cook + ')');
// mainEntity
r = RI.parse(JSON.stringify({ '@type': 'WebPage', mainEntity: { '@type': 'Recipe', name: 'Main', recipeIngredient: ['1 egg'] } }));
eq(r.recipe.title, 'Main', 'mainEntity');
// unsafe url not kept
r = RI.parse(JSON.stringify({ '@type': 'Recipe', name: 'U', url: 'javascript:alert(1)', recipeIngredient: ['1 egg'] }));
ok(!r.recipe.sourceUrl, 'javascript: url dropped');

// errors
throws(() => RI.parse(''), /Paste/, 'empty');
throws(() => RI.parse('{"@type":"Person","name":"x"}'), /No schema.org Recipe/, 'json without recipe');
throws(() => RI.parse('{"broken": '), /JSON/, 'broken json');
throws(() => RI.parse('Just a title'), /ingredients/, 'no ingredients');

console.log((fails ? 'FAILED ' : 'OK ') + n + ' checks, ' + fails + ' failures (recipe import)');
process.exit(fails ? 1 : 0);
