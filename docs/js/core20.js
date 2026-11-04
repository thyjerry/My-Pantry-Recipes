/* My Pantry Recipes web - v2.0 core: diets (incl. halal/kosher), inventory, regional prices, recipe editor helpers,
 * region defaults and ingredient names, cuisine groups, household allergen profiles. Port of the Java core
 * (core/Diets, Inventory, RegionalPrices, RecipeEditor, Region, Cuisines, Household); same results, checked by
 * tests/test-core20.js against vectors20.json. Diet tags are a guide, not a certification. MIT License. */
(function (root) {
  'use strict';
  var PR = root.PR;
  function set(s) { var o = new Set(); s.split(',').forEach(function (x) { o.add(x); }); return o; }
  function jd(x) { return Number.isInteger(x) ? x.toFixed(1) : String(x); }
  function jround(v) { return Math.floor(v + 0.5); }
  function cmpIC(a, b) { return PR.cmpIC ? PR.cmpIC(a, b) : (a.toLowerCase() < b.toLowerCase() ? -1 : a.toLowerCase() > b.toLowerCase() ? 1 : 0); }

  // ======================= Diets =======================
  var PESCATARIAN = 'pescatarian', MEDITERRANEAN = 'mediterranean', KETO = 'keto', LOW_CARB = 'low-carb', PALEO = 'paleo', WHOLE30 = 'whole30',
    CARNIVORE = 'carnivore', LOW_SODIUM = 'low-sodium', HIGH_PROTEIN = 'high-protein', HALAL = 'halal', KOSHER = 'kosher';
  var IDS = [PESCATARIAN, MEDITERRANEAN, LOW_CARB, KETO, PALEO, WHOLE30, CARNIVORE, LOW_SODIUM, HIGH_PROTEIN, HALAL, KOSHER];
  var NAMES = ['Pescatarian', 'Mediterranean-style', 'Low-carb', 'Keto', 'Paleo', 'Whole30-style', 'Carnivore', 'Low-sodium', 'High-protein', 'Halal-friendly', 'Kosher-style'];
  function dietName(id) {
    var k = IDS.indexOf(id);
    if (k >= 0) return NAMES[k];
    return { vegetarian: 'Vegetarian', vegan: 'Vegan', 'gluten-free': 'Gluten-free', 'dairy-free': 'Dairy-free' }[id] || id;
  }
  var NOTE = 'Diet tags are a guide, not a certification. They are worked out from ingredient names '
    + 'with conservative rules; products and brands differ, so always check labels (and talk to a professional for medical diets).';
  var RULES = {};
  RULES[PESCATARIAN] = 'Vegetarian, or the only meat is fish or seafood.';
  RULES[MEDITERRANEAN] = 'Cooked with olive oil, and either a Mediterranean cuisine or built on fish, beans or lentils; no beef, pork or processed meat, no butter, cream or margarine, not a dessert or drink.';
  RULES[LOW_CARB] = 'No grains, flour, bread, pasta, rice, corn, potatoes, beans or lentils, sugar or sweet syrups, dried fruit, juice or bananas.';
  RULES[KETO] = 'Low-carb rules plus no milk, most fruit (berries, lemon, lime, avocado, tomato and coconut are fine), beets, carrots or winter squash. Amounts are not counted, so small amounts of an allowed food can still add up.';
  RULES[PALEO] = 'No grains, legumes (including peanuts and soy), dairy (ghee is fine), refined sugar, seed oils, margarine or alcohol. Honey and maple syrup are allowed.';
  RULES[WHOLE30] = 'Paleo rules plus no added sugar of any kind (including honey, maple syrup and dates as sweetener), no alcohol (also for cooking), '
    + 'no processed meats (bacon, sausage, ham: compliant versions exist but are not assumed), no corn, no baked goods, desserts or treats. '
    + 'Green beans, snow peas and sugar snap peas are allowed. Additives (carrageenan, MSG, sulfites) cannot be checked from names.';
  RULES[CARNIVORE] = 'Meat, fish, seafood, eggs, organ meats, bone broth, animal fats and dairy, plus salt, pepper and water (no plants: no herbs, vegetables, grains, sugar or sauces; no processed meats such as sausages or bacon).';
  RULES[LOW_SODIUM] = 'No salty ingredients (soy sauce, fish sauce, miso, cured meats, stock or broth, olives, capers, pickles, anchovies, hard or salty cheese) '
    + 'and at most about \u00bc tsp of added salt per serving. \u201cSalt to taste\u201d is allowed: season lightly.';
  RULES[HIGH_PROTEIN] = 'At least 25 g protein per serving and at least 25% of calories from protein, by the nutrition estimate (only when 70% of the ingredients are matched).';
  RULES[HALAL] = 'No pork or pork products (bacon, ham, lard, most sausages and salami unless a halal meat is named), no gelatin, '
    + 'no alcohol (also for cooking, including mirin and cooking wine), no blood. A guide, not certification: meat and poultry must be '
    + 'bought halal, and you should check labels (vanilla extract, flavourings, cheese rennet and vinegars are not judged here).';
  RULES[KOSHER] = 'No pork, rabbit, shellfish or molluscs, or fish without scales (such as catfish, eel, monkfish, swordfish, shark); '
    + 'meat or poultry never with dairy (butter, cream, cheese, milk, yogurt, ghee); no gelatin; no grape wine, brandy or vinegar-free grape spirits '
    + 'unless certified, so wine-based ingredients are left out. A guide, not certification: meat must be kosher, and everything '
    + 'should carry a hechsher; rules differ between communities.';

  var FREE = set('salt,water,ice,black pepper,pepper,sea salt,kosher salt,ice cube,boiling water,cold water,warm water,hot water');
  var FISH_WORDS = set('fish,salmon,tuna,prawn,shrimp,crab,lobster,mussel,clam,squid,octopus,oyster,mackerel,sardine,'
    + 'cod,haddock,kipper,scallop,anchovy,trout,snapper,halibut,monkfish,tilapia,pollock,hake,basa,caviar,roe,eel,herring,'
    + 'swordfish,seabass,bass,bream,lox,calamari,bonito,dashi,crawfish,crayfish,langoustine,catfish,mahi,sole,flounder,whitebait,surimi');
  var GRAIN_WORDS = set('flour,bread,breadcrumb,rice,pasta,spaghetti,penne,macaroni,fusilli,linguine,rigatoni,farfalle,'
    + 'tagliatelle,lasagne,lasagna,orzo,fettuccine,noodle,couscous,bulgur,barley,farro,semolina,oats,oat,quinoa,corn,cornmeal,'
    + 'polenta,cornstarch,cornflour,tortilla,pita,naan,bun,roll,bagel,croissant,pastry,dough,cracker,biscuit,cookie,cake,'
    + 'wheat,rye,millet,buckwheat,granola,cereal,panko,crouton,ramen,udon,soba,gnocchi,wonton,filo,phyllo,muffin,brioche,baguette,'
    + 'ciabatta,sourdough,pretzel,pancake,waffle,pizza,wrap,chapati,roti,paratha,flatbread,grits,masa,popcorn,spelt,matzo,freekeh,'
    + 'vermicelli,ravioli,tortellini,dumpling,stuffing,crumb,cous,arborio,risotto,basmati,jasmine,sorghum,amaranth,teff,tapioca,'
    + 'sago,seitan,breadstick,scone,crumpet,taco,tostada,shell,graham,digestive,wafer,brownie,pie,tart,shortcrust,puff,bisquick,roux,'
    + 'malt,beer,ale,stout,gravy,starch');
  var NOT_GRAIN = set('almond flour,coconut flour,cauliflower rice,riced cauliflower,corn salad_x,lettuce wrap,lettuce leaf,'
    + 'egg roll_x,peppercorn,sweetcorn_x,flaxseed meal,almond meal,arrowroot_x,shell on_x,pie spice,pumpkin pie spice,rice vinegar,rice wine vinegar,'
    + 'oat milk_x,cauliflower pizza_x,coconut wrap,nori wrap_x,cream of tartar,tartar sauce_x,water chestnut');
  var LEGUME_WORDS = set('bean,beans,lentil,lentils,chickpea,chickpeas,pea,peas,soy,soya,soybean,tofu,tempeh,edamame,miso,'
    + 'peanut,peanuts,hummus,houmous,dal,dhal,daal,gram,urad,mung,falafel,tamari,natto,doubanjiang,gochujang,kecap,carob');
  var OK_LEGUME = set('green bean,green beans,snow pea,snow peas,sugar snap,sugar snap pea,sugar snap peas,mangetout,'
    + 'runner bean,runner beans,french bean,french beans,string bean,string beans,haricot vert,vanilla bean,coffee bean,cocoa bean,bean sprout,bean sprouts,'
    + 'chickpea_x,coconut aminos');
  var SUGAR_WORDS = set('sugar,honey,maple,agave,molasses,treacle,syrup,jam,jelly,marmalade,chocolate,marshmallow,'
    + 'caramel,condensed,ketchup,bbq,barbecue,hoisin,teriyaki,sweetener,icing,frosting,fondant,candy,sprinkles,nutella,dulce,'
    + 'date syrup,coconut sugar,brown sugar,muscovado,demerara,turbinado,dextrose,glucose,fructose,sweet chili,sweet chilli,mirin,'
    + 'custard,meringue,lemonade,cola,soda,juice,cordial,liqueur,preserves,chutney');
  var NOT_SUGAR = set('sugar snap,sugar snap pea,sugar snap peas,sugar free_x,club soda_x,baking soda,bicarbonate of soda,soda water_x,'
    + 'bread soda,lemon juice,lime juice,unsweetened chocolate_x,cocoa powder,sugar pumpkin_x');
  var PALEO_SWEET = set('honey,maple,maple syrup,date,dates,coconut sugar');
  var ALCOHOL_WORDS = set('wine,beer,ale,stout,lager,rum,vodka,whiskey,whisky,bourbon,brandy,sherry,sake,mirin,liqueur,'
    + 'tequila,gin,vermouth,marsala,port,cognac,prosecco,champagne,cider,kirsch,amaretto,schnapps,mezcal,grappa,ouzo,shaoxing,'
    + 'cointreau,kahlua,baileys,limoncello,calvados,madeira,cava,mead');
  var NOT_ALCOHOL = set('wine vinegar,red wine vinegar,white wine vinegar,rice wine vinegar,sherry vinegar,cider vinegar,'
    + 'apple cider vinegar,port salut_x,ginger ale,root beer_x,non alcoholic_x,sake_x,portobello,portabella,portobello mushroom,'
    + 'ginger beer_x,apple cider_x');
  var SEED_OIL = set('vegetable oil,canola oil,rapeseed oil,soybean oil,corn oil,sunflower oil,cottonseed oil,'
    + 'grapeseed oil,safflower oil,margarine,shortening,cooking spray,peanut oil,rice bran oil,vegetable shortening,spread');
  var PROCESSED_MEAT = set('bacon,ham,sausage,salami,pepperoni,chorizo,pancetta,prosciutto,hot dog,bratwurst,mortadella,'
    + 'nduja,jerky,kielbasa,andouille,frankfurter,lardon,gammon,bologna,spam,deli,luncheon,corned,pastrami,hot dogs,sausages,lardons');
  var RED_MEAT = set('beef,pork,veal,steak,brisket,chuck,sirloin,ribeye,oxtail,mince,meatball,meatballs,venison,ribs,'
    + 'short rib,tenderloin,pork belly,pork chop');
  var STARCHY = set('potato,potatoes,sweet potato,sweet potatoes,yam,yams,plantain,plantains,parsnip,parsnips,'
    + 'hash brown,hash browns,fries,chips_x,sweetcorn,cassava,taro');
  var HIGH_SUGAR_FRUIT = set('banana,bananas,raisin,raisins,sultana,sultanas,date,dates,fig,figs,prune,prunes,'
    + 'dried fruit,dried cranberries,dried apricot,dried apricots,currant,currants,mango_x,juice,pineapple_x');
  var KETO_FRUIT_BAN = set('apple,apples,pear,pears,mango,mangoes,pineapple,grape,grapes,orange,oranges,peach,peaches,'
    + 'nectarine,apricot,apricots,cherry,cherries,plum,plums,melon,watermelon,cantaloupe,kiwi,papaya,pomegranate,clementine,'
    + 'tangerine,mandarin,persimmon,lychee,guava,passion fruit,applesauce,beet,beets,beetroot,carrot,carrots,butternut,'
    + 'butternut squash,acorn squash,delicata squash,parsnip,parsnips,jicama_x,sweetcorn,corn');
  var SALTY = set('soy sauce,tamari,fish sauce,oyster sauce,miso,bacon,ham,salami,prosciutto,pancetta,chorizo,pepperoni,'
    + 'sausage,sausages,hot dog,bouillon,stock,broth,stock cube,olive,olives,caper,capers,pickle,pickles,gherkin,anchovy,anchovies,feta,'
    + 'parmesan,pecorino,halloumi,blue cheese,roquefort,gorgonzola,processed cheese,american cheese,teriyaki,hoisin,worcestershire,'
    + 'kimchi,sauerkraut,soup mix,gravy,ketchup,bbq sauce,barbecue sauce,salted butter_x,cured,smoked salmon,lox,jerky,corned,'
    + 'pastrami,kecap,gochujang,doubanjiang,dashi,bouillon cube,seasoning mix,taco seasoning,ranch,stock powder,msg,soy');
  var NOT_SALTY = set('low sodium stock,low sodium broth,unsalted stock,no salt stock,olive oil,extra virgin olive oil,'
    + 'light olive oil,soy milk,soya milk,soy yogurt,soybean_x,soy free_x');
  var SALT_KEYS = set('salt,sea salt,kosher salt,table salt,flaky salt,rock salt,coarse salt,fine salt,celery salt,garlic salt,onion salt');
  var CARNIVORE_DAIRY_BAN = set('nutella,ranch,tzatziki,alfredo,custard,lassi,labneh_x');
  var ANIMAL_EXTRA = set('egg,eggs,yolk,egg yolk,egg white,egg yolks,egg whites,lard,tallow,suet,bone broth,beef broth,'
    + 'chicken broth,beef stock,chicken stock,bone marrow,gelatin,gelatine,dripping,beef dripping,duck fat,schmaltz,ghee,butter,collagen,short rib,short ribs');
  var MED_CUISINES = set('greek,italian,spanish,turkish,lebanese,moroccan,middle eastern,mediterranean,israeli,'
    + 'tunisian,cypriot,maltese,croatian,syrian,egyptian,portuguese,levantine,provencal,sicilian');
  var PORK = set('pork,bacon,ham,gammon,lard,pancetta,prosciutto,chorizo,salami,pepperoni,nduja,guanciale,mortadella,'
    + 'lardon,lardons,speck,chicharron,chicharrones,pork belly,pork chop,pork rind,pork rinds,spam,hot dog,hot dogs,frankfurter,bratwurst,'
    + 'kielbasa,andouille,sausage,sausages,hog,jamon,serrano ham,char siu,boar');
  var NOT_PORK = set('chicken sausage,turkey sausage,beef sausage,lamb sausage,merguez,vegan sausage,veggie sausage,'
    + 'vegetarian sausage,plant based sausage,turkey bacon,beef bacon,turkey ham,halal sausage,beef hot dog,chicken hot dog,'
    + 'hamburger,hamburger bun,hot dog bun,hot dog buns,graham,ham hock_x,eggplant,vegan bacon,coconut bacon,mushroom bacon,beef salami,beef pepperoni,turkey pepperoni');
  var GELATIN = set('gelatin,gelatine,gelatin sheet,gelatine leaf,jelly crystal,jello,marshmallow,marshmallows,gummy,gummies,aspic');
  var NOT_GELATIN = set('agar,agar agar,vegan gelatin,vegan marshmallow,halal gelatin,kosher gelatin,fish gelatin');
  var NO_SCALE_FISH = set('catfish,eel,monkfish,swordfish,shark,sturgeon,caviar,skate,ray,octopus,squid,calamari,cuttlefish');
  var GRAPE_ALCOHOL = set('wine,brandy,cognac,sherry,port,marsala,champagne,prosecco,vermouth,madeira,cava,grappa,armagnac,calvados_x');
  var NON_KOSHER_MEAT = set('rabbit,hare,horse,frog,snail,snails,escargot,boar,venison_x');
  var SHELLFISH_AND_MOLLUSC = set('prawn,shrimp,crab,lobster,crayfish,crawfish,langoustine,scampi,scallop,mussel,clam,'
    + 'oyster,cockle,whelk,abalone,oyster sauce,shrimp paste,fish sauce_x,dried shrimp');

  function word(key, words) {
    if (words.has(key)) return true;
    var p = key.split(' ');
    for (var i = 0; i < p.length; i++) if (words.has(p[i])) return true;
    return false;
  }
  function phrase(key, phrases) {
    var k = ' ' + key + ' ', hit = false;
    phrases.forEach(function (p) { if (!hit && k.indexOf(' ' + p + ' ') >= 0) hit = true; });
    return hit;
  }
  function has(key, words, except) { return (word(key, words) || phrase(key, words)) && !phrase(key, except); }
  function isPork(key) {
    if (phrase(key, NOT_PORK) || NOT_PORK.has(key)) return false;
    if (key.indexOf('eggplant') >= 0 || key.indexOf('hamburger') >= 0 || key.indexOf('graham') >= 0) return false;
    return word(key, PORK) || phrase(key, PORK);
  }
  function isGelatin(key) { return !phrase(key, NOT_GELATIN) && (word(key, GELATIN) || phrase(key, GELATIN)); }
  function isFish(key) { return word(key, FISH_WORDS) || key === 'fish sauce' || key === 'oyster sauce'; }
  var catOf = null, CATS = null;
  function catIdx(c) { return PR.Catalog.CATEGORIES.indexOf(c); }
  function category(key) {
    if (catOf === null) {
      var m = new Map();
      PR.Catalog.ITEMS.forEach(function (items, c) {
        items.forEach(function (it) {
          if (PR.Catalog.isHeader(it)) return;
          var k = PR.normalize(it);
          if (k !== '' && !m.has(k)) m.set(k, c);
        });
      });
      catOf = m;
      CATS = { GRAINS: catIdx('Grains & rice'), PASTA: catIdx('Pasta & noodles'), BREAD: catIdx('Bread & tortillas'), LEGUMES: catIdx('Beans & legumes'),
        SWEET: catIdx('Sweeteners') };
    }
    var c = catOf.get(key);
    return c == null ? -1 : c;
  }
  function cats() { category(''); return CATS; }
  function isGrain(key) {
    if (phrase(key, NOT_GRAIN)) return false;
    var c = category(key), C = cats();
    return c === C.GRAINS || c === C.PASTA || c === C.BREAD || has(key, GRAIN_WORDS, NOT_GRAIN);
  }
  function isLegume(key) { if (phrase(key, OK_LEGUME)) return false; return category(key) === cats().LEGUMES || has(key, LEGUME_WORDS, OK_LEGUME); }
  function isSugar(key) { return !phrase(key, NOT_SUGAR) && (category(key) === cats().SWEET || has(key, SUGAR_WORDS, NOT_SUGAR)); }
  function isAlcohol(key) { return has(key, ALCOHOL_WORDS, NOT_ALCOHOL); }
  function isSeedOil(key) { return phrase(key, SEED_OIL); }
  function isProcessedMeat(key) { return word(key, PROCESSED_MEAT) || phrase(key, PROCESSED_MEAT); }
  function isRedMeat(key) { return (word(key, RED_MEAT) || phrase(key, RED_MEAT)) && key.indexOf('beef tomato') < 0 && key.indexOf('mushroom') < 0; }
  function isStarchy(key) { return phrase(key, STARCHY) || word(key, STARCHY); }
  function isSalty(key) { return has(key, SALTY, NOT_SALTY); }
  function dairyNotGhee(key) { return PR.isDairy(key) && key !== 'ghee' && key.indexOf('ghee') < 0; }
  function c(k, s) { return k.indexOf(s) >= 0; }
  function animal(key) {
    if (phrase(key, ANIMAL_EXTRA) || word(key, ANIMAL_EXTRA)) {
      if (c(key, 'eggplant') || c(key, 'peanut butter') || c(key, 'nut butter') || c(key, 'almond butter') || c(key, 'butter bean') || c(key, 'cocoa butter')
        || c(key, 'vegan') || c(key, 'butternut')) return false;
      return true;
    }
    if (PR.isDairy(key)) return !phrase(key, CARNIVORE_DAIRY_BAN) && !word(key, CARNIVORE_DAIRY_BAN);
    if (PR.isMeat(key)) {
      return !(c(key, 'worcestershire') || c(key, 'sauce') || c(key, 'marshmallow') || c(key, 'gravy') || c(key, 'bouillon') || c(key, 'seasoning')
        || c(key, 'mince pie') || c(key, 'dashi'));
    }
    return false;
  }
  function saltTspPerServing(r) {
    var tsp = 0, unmeasured = false;
    r.ingredients.forEach(function (i) {
      if (i.optional || !SALT_KEYS.has(i.key)) return;
      if (isNaN(i.amount)) { unmeasured = true; return; }
      var u = i.unit.toLowerCase(), f;
      if (u.indexOf('tsp') === 0 || u.indexOf('teaspoon') === 0) f = 1;
      else if (u.indexOf('tbsp') === 0 || u.indexOf('tablespoon') === 0) f = 3;
      else if (u.indexOf('pinch') === 0) f = 1 / 16;
      else if (u.indexOf('dash') === 0) f = 1 / 8;
      else if (u === 'g') f = 1 / 6;
      else if (u.indexOf('cup') === 0) f = 48;
      else f = 1;
      tsp += i.amount * f;
    });
    var per = tsp / Math.max(1, r.servings);
    return per === 0 && unmeasured ? NaN : per;
  }
  function classify(r, foods) {
    var meat = false, nonFishMeat = false, grain = false, legume = false, sugar = false, paleoSweetOnly = true, alcohol = false,
      seedOil = false, processed = false, red = false, starchy = false, sugarFruit = false, ketoFruit = false, milk = false,
      dairy = false, butterCream = false, fishOrLegume = false, oliveOil = false, salty = false, nonAnimal = false, corn = false, any = false,
      pork = false, gelatin = false, anyDairy = false, notKosherSea = false, grapeAlcohol = false, notKosherMeat = false;
    r.ingredients.forEach(function (i) {
      var k = i.key;
      if (i.optional || k === '') return;
      any = true;
      if (PR.isMeat(k)) { meat = true; if (!isFish(k)) nonFishMeat = true; }
      if (isFish(k)) fishOrLegume = true;
      if (isGrain(k)) grain = true;
      if (isLegume(k)) { legume = true; fishOrLegume = true; }
      if (isSugar(k)) { sugar = true; if (!(phrase(k, PALEO_SWEET) || PALEO_SWEET.has(k))) paleoSweetOnly = false; }
      if (isAlcohol(k)) alcohol = true;
      if (isSeedOil(k)) seedOil = true;
      if (isProcessedMeat(k)) processed = true;
      if (isRedMeat(k)) red = true;
      if (isStarchy(k)) starchy = true;
      if (phrase(k, HIGH_SUGAR_FRUIT) || word(k, HIGH_SUGAR_FRUIT)) sugarFruit = true;
      if (phrase(k, KETO_FRUIT_BAN) || KETO_FRUIT_BAN.has(k)) ketoFruit = true;
      var d = dairyNotGhee(k);
      if (d) dairy = true;
      if (d && (k === 'milk' || k.slice(-5) === ' milk' && !c(k, 'coconut') && !c(k, 'almond') || c(k, 'condensed') || c(k, 'evaporated'))) milk = true;
      if (c(k, 'butter') && PR.isDairy(k) || c(k, 'cream') && PR.isDairy(k) || c(k, 'margarine')) butterCream = true;
      if (c(k, 'olive oil')) oliveOil = true;
      if (isSalty(k)) salty = true;
      if (k === 'corn' || c(k, 'sweetcorn') || c(k, 'corn ') || k.indexOf('corn') === 0 || c(k, 'polenta') || c(k, 'cornmeal')) corn = true;
      if (!animal(k) && !FREE.has(k)) nonAnimal = true;
      if (isPork(k)) pork = true;
      if (isGelatin(k)) gelatin = true;
      if (PR.isDairy(k)) anyDairy = true;
      if (word(k, SHELLFISH_AND_MOLLUSC) || phrase(k, SHELLFISH_AND_MOLLUSC) || word(k, NO_SCALE_FISH)) notKosherSea = true;
      if (isAlcohol(k) && (word(k, GRAPE_ALCOHOL) || phrase(k, GRAPE_ALCOHOL)) && !c(k, 'rice wine') && !c(k, 'shaoxing')) grapeAlcohol = true;
      if (word(k, NON_KOSHER_MEAT)) notKosherMeat = true;
    });
    var out = [];
    if (!any) return out;
    var cat = (r.category || '').toLowerCase(), meals = r.meals || [];
    var treat = cat === 'dessert' || cat === 'baking' || meals.indexOf('Dessert') >= 0 || meals.indexOf('Baked goods') >= 0;
    var drink = cat === 'drink' || meals.indexOf('Drink') >= 0;
    if (!nonFishMeat) out.push(PESCATARIAN);
    var medCuisine = !!r.cuisine && MED_CUISINES.has(r.cuisine.toLowerCase());
    if (oliveOil && (medCuisine || fishOrLegume) && !red && !processed && !butterCream && !treat && !drink && !seedOil) out.push(MEDITERRANEAN);
    var lowCarb = !grain && !sugar && !starchy && !sugarFruit && !corn && !legume;
    if (lowCarb) out.push(LOW_CARB);
    if (lowCarb && !milk && !ketoFruit) out.push(KETO);
    var paleo = !grain && !legume && !dairy && (!sugar || paleoSweetOnly) && !seedOil && !alcohol && !corn;
    if (paleo) out.push(PALEO);
    if (paleo && !sugar && !processed && !treat && !alcohol) out.push(WHOLE30);
    if (!nonAnimal && !processed && (meat || dairy)) out.push(CARNIVORE);
    if (!salty) { var s = saltTspPerServing(r); if (isNaN(s) || s <= 0.25 + 1e-9) out.push(LOW_SODIUM); }
    if (!pork && !gelatin && !alcohol) out.push(HALAL);
    if (!pork && !gelatin && !notKosherSea && !grapeAlcohol && !notKosherMeat && !(nonFishMeat && anyDairy)) out.push(KOSHER);
    if (foods) {
      var e = PR.Nutrition.estimate(r, foods);
      if (e.show() && e.protein >= 25 && e.kcal > 0 && e.protein * 4 >= 0.25 * e.kcal) out.push(HIGH_PROTEIN);
    }
    return out;
  }
  function blockers(r, diet) {
    var out = [];
    r.ingredients.forEach(function (i) {
      if (i.optional || i.key === '') return;
      var one = { category: '', cuisine: '', meals: [], servings: r.servings, ingredients: [i] };
      if (diet === CARNIVORE) { if (!animal(i.key) && !FREE.has(i.key)) out.push(i.name); return; }
      if (diet === HIGH_PROTEIN || diet === MEDITERRANEAN) return;
      if (classify(one, null).indexOf(diet) < 0 && !(diet === LOW_SODIUM && SALT_KEYS.has(i.key))) out.push(i.name);
    });
    if (diet === KOSHER && !out.length && classify(r, null).indexOf(KOSHER) < 0) out.push('meat or poultry together with dairy');
    return out;
  }
  function fitsAll(r, diets) {
    for (var i = 0; i < diets.length; i++) if (r.tags.indexOf(diets[i]) < 0 && (r.diets || []).indexOf(diets[i]) < 0) return false;
    return true;
  }
  PR.Diets = { IDS: IDS, NAMES: NAMES, NOTE: NOTE, RULES: RULES, name: dietName, classify: classify, blockers: blockers, fitsAll: fitsAll,
    isPork: isPork, isGelatin: isGelatin, isFish: isFish, isGrain: isGrain, isLegume: isLegume, category: category, HALAL: HALAL, KOSHER: KOSHER, CARNIVORE: CARNIVORE,
    isSugar: isSugar, isSalty: isSalty, isStarchy: isStarchy, isProcessedMeat: isProcessedMeat, saltTspPerServing: saltTspPerServing, SALT_KEYS: SALT_KEYS };

  // ======================= Dates helper =======================
  function addMonths(iso, n) {
    if (!PR.Dates.valid(iso)) return '';
    var y0 = +iso.slice(0, 4), m0 = +iso.slice(5, 7), d0 = +iso.slice(8, 10);
    var t = y0 * 12 + (m0 - 1) + n, y = Math.trunc(t / 12), m = t % 12 + 1;
    var dim = [31, (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0 ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m - 1];
    var d = Math.min(d0, dim);
    function pad(v, k) { var s = String(v); while (s.length < k) s = '0' + s; return s; }
    return pad(y, 4) + '-' + pad(m, 2) + '-' + pad(d, 2);
  }

  // ======================= Inventory =======================
  var PANTRY = 'pantry', FRIDGE = 'fridge', FREEZER = 'freezer', SPICE = 'spice';
  var LOCATIONS = [PANTRY, FRIDGE, FREEZER, SPICE], LOCATION_NAMES = ['Pantry', 'Fridge', 'Freezer', 'Spice rack'];
  var IC = null;
  function icats() {
    if (!IC) IC = { VEG: catIdx('Vegetables'), FRUIT: catIdx('Fruits'), HERBS: catIdx('Herbs'), SPICES: catIdx('Spices & seasonings'), MEAT: catIdx('Meat'),
      POULTRY: catIdx('Poultry'), FISH: catIdx('Fish & seafood'), DELI: catIdx('Deli & cured'), DAIRY: catIdx('Eggs & dairy'), CHEESE: catIdx('Cheese'),
      PLANT: catIdx('Plant-based'), FROZEN: catIdx('Frozen'), BREAD: catIdx('Bread & tortillas') };
    return IC;
  }
  var COUNTER = set('potato,sweet potato,onion,red onion,shallot,garlic,banana,tomato,avocado,winter squash,butternut squash,acorn squash,'
    + 'spaghetti squash,pumpkin,delicata squash,ginger,pearl onion,mango,pineapple');
  function defaultLocationKey(key) {
    if (key == null) return PANTRY;
    if (key.indexOf('frozen ') === 0) return FREEZER;
    var cc = category(key), C = icats();
    if (cc === C.SPICES) return SPICE;
    if (cc === C.FROZEN) return FREEZER;
    if (cc === C.MEAT || cc === C.POULTRY || cc === C.FISH || cc === C.DELI || cc === C.DAIRY || cc === C.CHEESE || cc === C.PLANT) return FRIDGE;
    if (cc === C.HERBS) return key.indexOf('dried ') === 0 ? SPICE : FRIDGE;
    if (cc === C.VEG || cc === C.FRUIT) return COUNTER.has(key) ? PANTRY : FRIDGE;
    return PANTRY;
  }
  function defaultLocation(key, label) {
    if (label === undefined) return defaultLocationKey(key);
    var l = label == null ? '' : PR.jtrim(label.toLowerCase());
    if (l.indexOf('frozen ') === 0) return FREEZER;
    if (l.indexOf('dried ') === 0 || l.indexOf('ground ') === 0 && category(key) === icats().SPICES) return SPICE;
    return defaultLocationKey(key);
  }
  function freezerMonths(k) {
    if (k == null) return 3;
    if (c(k, 'ground') || c(k, 'mince') || c(k, 'sausage') || c(k, 'bacon') || c(k, 'hot dog')) return c(k, 'bacon') ? 1 : 3;
    if (c(k, 'leftover') || c(k, 'cooked') || c(k, 'soup') || c(k, 'stew') || c(k, 'stock') || c(k, 'broth')) return 3;
    var cc = category(k), C = icats();
    if (cc === C.POULTRY) return c(k, 'whole') ? 12 : 9;
    if (cc === C.MEAT) return c(k, 'steak') || c(k, 'roast') || c(k, 'chop') ? 6 : 4;
    if (cc === C.FISH) {
      var fatty = c(k, 'salmon') || c(k, 'mackerel') || c(k, 'sardine') || c(k, 'trout') || c(k, 'tuna') || c(k, 'herring');
      if (c(k, 'shrimp') || c(k, 'prawn')) return 6;
      return fatty ? 3 : 6;
    }
    if (cc === C.DELI) return 2;
    if (cc === C.CHEESE) return c(k, 'parmesan') || c(k, 'cheddar') || c(k, 'hard') ? 6 : 4;
    if (cc === C.DAIRY) return c(k, 'butter') ? 9 : c(k, 'milk') ? 3 : c(k, 'egg') ? 12 : 3;
    if (cc === C.BREAD) return 3;
    if (cc === C.VEG || cc === C.FRUIT || cc === C.FROZEN) return 10;
    if (cc === C.HERBS) return 6;
    return 3;
  }
  var OK = 'ok', USE_SOON = 'use soon', PAST = 'past best';
  function useOrTossDate(frozen, key) { return PR.Dates.valid(frozen) ? addMonths(frozen, freezerMonths(key)) : ''; }
  function freezerStatus(frozen, key, today) {
    var d = useOrTossDate(frozen, key);
    if (d === '' || !PR.Dates.valid(today)) return OK;
    var left = PR.Dates.between(today, d);
    if (left < 0) return PAST;
    if (left <= 30) return USE_SOON;
    return OK;
  }
  function item(key, label) { return { key: key || '', label: label || '', loc: '', frozen: '', useBy: '', qty: '', staple: false, low: false }; }
  function location(i) { return i.loc ? i.loc : defaultLocation(i.key, i.label); }
  function at(items, loc) { return items.filter(function (i) { return location(i) === loc; }).sort(function (a, b) { return cmpIC(a.label, b.label); }); }
  function counts(items) { var n = [0, 0, 0, 0]; items.forEach(function (i) { n[LOCATIONS.indexOf(location(i))]++; }); return n; }
  function restock(items) {
    return items.filter(function (i) { return i.low; }).sort(function (a, b) { if (a.staple !== b.staple) return a.staple ? -1 : 1; return cmpIC(a.label, b.label); });
  }
  function freezerAlerts(items, today) {
    return items.filter(function (i) { return location(i) === FREEZER && PR.Dates.valid(i.frozen) && freezerStatus(i.frozen, i.key, today) !== OK; })
      .sort(function (a, b) { var x = useOrTossDate(a.frozen, a.key), y = useOrTossDate(b.frozen, b.key); return x < y ? -1 : x > y ? 1 : 0; });
  }
  function expiringIn(items, today, days) { return items.filter(function (i) { return PR.Dates.valid(i.useBy) && PR.Dates.between(today, i.useBy) === days; }); }
  function spicesToBuy(have, keyCounts, n) {
    var rows = [], seen = new Set();
    PR.Catalog.ITEMS[icats().SPICES].forEach(function (it) {
      if (PR.Catalog.isHeader(it)) return;
      var k = PR.normalize(it);
      if (k === '' || have.has(k) || seen.has(k)) return;
      seen.add(k);
      var cn = keyCounts.get(k);
      if (!cn) return;
      rows.push([it, k, String(cn)]);
    });
    rows.sort(function (a, b) { var d = +b[2] - +a[2]; return d !== 0 ? d : cmpIC(a[0], b[0]); });
    return rows.slice(0, n);
  }
  function keyCounts(recipes) {
    var m = new Map();
    recipes.forEach(function (r) {
      var ks = new Set();
      r.ingredients.forEach(function (i) { if (i.key) ks.add(i.key); });
      ks.forEach(function (k) { m.set(k, (m.get(k) || 0) + 1); });
    });
    return m;
  }
  function cookTonight(recipes, expiring, pantry) {
    var best = null, bestUse = 0, bestHave = -1;
    recipes.forEach(function (r) {
      var use = 0, have = 0, total = 0;
      r.ingredients.forEach(function (i) {
        if (i.optional || !i.key) return;
        total++;
        if (expiring.has(i.key)) use++;
        if (pantry.has(i.key)) have++;
      });
      if (use === 0 || total === 0) return;
      var h = have / total;
      if (use > bestUse || use === bestUse && (h > bestHave + 1e-9 || Math.abs(h - bestHave) < 1e-9 && best && r.title < best.title)) { best = r; bestUse = use; bestHave = h; }
    });
    return best;
  }
  var FREEZER_NOTE = 'Freezer times are for best quality. Food kept frozen at 0 \u00b0F (-18 \u00b0C) stays safe, '
    + 'but taste and texture fade. Times are rounded from USDA FoodKeeper guidance.';
  PR.Inventory = { PANTRY: PANTRY, FRIDGE: FRIDGE, FREEZER: FREEZER, SPICE: SPICE, LOCATIONS: LOCATIONS, LOCATION_NAMES: LOCATION_NAMES,
    locationName: function (id) { var k = LOCATIONS.indexOf(id); return k < 0 ? 'Pantry' : LOCATION_NAMES[k]; },
    validLocation: function (id) { return LOCATIONS.indexOf(id) >= 0 ? id : ''; },
    defaultLocation: defaultLocation, freezerMonths: freezerMonths, useOrTossDate: useOrTossDate, freezerStatus: freezerStatus, OK: OK, USE_SOON: USE_SOON, PAST: PAST,
    FREEZER_NOTE: FREEZER_NOTE, item: item, location: location, at: at, counts: counts, restock: restock,
    freezerAlerts: freezerAlerts, expiringIn: expiringIn, spicesToBuy: spicesToBuy, keyCounts: keyCounts, cookTonight: cookTonight, addMonths: addMonths };

  // ======================= Regional prices =======================
  var REGIONS = ['US', 'NE', 'MW', 'S', 'W'], REGION_NAMES = ['U.S.', 'Northeast', 'Midwest', 'South', 'West'];
  var ZIP3 = '005-005:NY 010-027:MA 028-029:RI 030-038:NH 039-049:ME 050-054:VT 055-055:MA 056-059:VT '
    + '060-069:CT 070-089:NJ 100-149:NY 150-196:PA 197-199:DE 200-200:DC 201-201:VA 202-205:DC 206-219:MD '
    + '220-246:VA 247-268:WV 270-289:NC 290-299:SC 300-319:GA 320-339:FL 341-349:FL 350-369:AL 370-385:TN '
    + '386-397:MS 398-399:GA 400-427:KY 430-459:OH 460-479:IN 480-499:MI 500-528:IA 530-549:WI 550-567:MN '
    + '569-569:DC 570-577:SD 580-588:ND 590-599:MT 600-629:IL 630-658:MO 660-679:KS 680-693:NE 700-714:LA '
    + '716-729:AR 730-732:OK 733-733:TX 734-749:OK 750-799:TX 800-816:CO 820-831:WY 832-838:ID 840-847:UT '
    + '850-865:AZ 870-884:NM 885-885:TX 889-898:NV 900-961:CA 967-968:HI 970-979:OR 980-994:WA 995-999:AK';
  var NE_STATES = 'CT ME MA NH NJ NY PA RI VT', MW_STATES = 'IL IN IA KS MI MN MO NE ND OH SD WI',
    S_STATES = 'AL AR DE DC FL GA KY LA MD MS NC OK SC TN TX VA WV', W_STATES = 'AK AZ CA CO HI ID MT NV NM OR UT WA WY';
  var ranges = null;
  function regionName(r) { var i = REGIONS.indexOf(r); return i < 0 ? 'U.S.' : REGION_NAMES[i]; }
  function cleanZip(z) { if (z == null) return ''; z = String(z).trim(); return /^\d{5}(-\d{4})?$/.test(z) ? z.slice(0, 5) : ''; }
  function state(zip) {
    var z = cleanZip(zip);
    if (z === '') return '';
    if (!ranges) ranges = ZIP3.split(' ').map(function (p) { return [+p.slice(0, 3), +p.slice(4, 7), p.slice(8)]; });
    var p = +z.slice(0, 3);
    for (var i = 0; i < ranges.length; i++) if (p >= ranges[i][0] && p <= ranges[i][1]) return ranges[i][2];
    return '';
  }
  function regionOfState(st) {
    if (st == null || st.length !== 2) return 'US';
    if (NE_STATES.indexOf(st) >= 0) return 'NE';
    if (MW_STATES.indexOf(st) >= 0) return 'MW';
    if (S_STATES.indexOf(st) >= 0) return 'S';
    if (W_STATES.indexOf(st) >= 0) return 'W';
    return 'US';
  }
  function median(v) {
    if (!v.length) return 1;
    v.sort(function (a, b) { return a - b; });
    var n = v.length, m = n % 2 === 1 ? v[(n - 1) / 2] : (v[n / 2 - 1] + v[n / 2]) / 2;
    return jround(m * 1000) / 1000;
  }
  function Table() { this.date = ''; this.items = []; this._f = null; }
  Table.prototype.factor = function (region) {
    var self = this;
    if (!this._f) {
      var f = [1];
      for (var r = 1; r < 5; r++) {
        var rs = [];
        self.items.forEach(function (it) { if (!isNaN(it.price[0]) && !isNaN(it.price[r]) && it.price[0] > 0) rs.push(it.price[r] / it.price[0]); });
        f.push(median(rs));
      }
      this._f = f;
    }
    var k = REGIONS.indexOf(region);
    return k < 0 ? 1 : this._f[k];
  };
  function pnum(s) { return /^\s*[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?\s*$/.test(s) ? Number(s) : NaN; }
  function parseTable(tsv) {
    var t = new Table();
    if (tsv == null) return t;
    tsv.split('\n').forEach(function (line) {
      if (line === '' || line.charAt(0) === '#') return;
      var f = line.split('\t');
      if (f[0] === '@date' && f.length > 1) { t.date = f[1].trim(); return; }
      if (f.length < 9) return;
      var it = { name: f[0].trim(), keys: [], amount: pnum(f[2]), unit: f[3].trim(), price: [] };
      f[1].split(',').forEach(function (k) { if (k.trim()) it.keys.push(k.trim()); });
      if (isNaN(it.amount)) return;
      for (var r = 0; r < 5; r++) {
        var v = f[4 + r].trim();
        if (v === '-' || v === '') it.price.push(NaN);
        else { var x = pnum(v); if (isNaN(x)) return; it.price.push(x); }
      }
      if (PR.Cost.UNITS.indexOf(it.unit) < 0 || it.amount <= 0 || isNaN(it.price[0])) return;
      t.items.push(it);
    });
    return t;
  }
  function r3(v) { return jround(v * 1000) / 1000; }
  function defaults(t, region, estimates, sources) {
    var out = new Map(), k = Math.max(0, REGIONS.indexOf(region)), f = t.factor(region), rn = regionName(region), fs = jd(f);
    if (estimates) estimates.forEach(function (p, key) {
      out.set(key, k === 0 ? p : PR.Cost.make(r3(p.price * f), p.amount, p.unit));
      if (sources) sources.set(key, k === 0 ? { kind: 'estimate', label: 'Built-in rough estimate' }
        : { kind: 'estimate-factor', label: 'Built-in rough estimate \u00d7 ' + rn + ' factor (' + fs + ')' });
    });
    t.items.forEach(function (it) {
      var v, src;
      if (k > 0 && !isNaN(it.price[k])) { v = it.price[k]; src = { kind: 'regional', label: 'BLS ' + rn + ' regional average, ' + t.date }; }
      else if (k > 0) { v = r3(it.price[0] * f); src = { kind: 'us-factor', label: 'BLS U.S. average, ' + t.date + ' \u00d7 ' + rn + ' factor (' + fs + ')' }; }
      else { v = it.price[0]; src = { kind: 'us', label: 'BLS U.S. average, ' + t.date }; }
      it.keys.forEach(function (key) { out.set(key, PR.Cost.make(v, it.amount, it.unit)); if (sources) sources.set(key, src); });
    });
    return out;
  }
  function coverage(t, region) {
    var k = Math.max(0, REGIONS.indexOf(region)), keys = 0, regional = 0;
    t.items.forEach(function (it) { keys += it.keys.length; if (k > 0 && !isNaN(it.price[k])) regional += it.keys.length; });
    return [keys, regional];
  }
  function withinBudget(e, budget) { if (budget <= 0) return true; if (!e || !e.show()) return true; /* v2.0 S3: unknown cost is kept */ return e.perServing <= budget + 1e-9; }
  function trimTo(s, n) { s = s == null ? '' : String(s).trim(); return s.length > n ? s.slice(0, n) : s; }
  function parseOpenPrices(json, region, max) {
    var out = [];
    try {
      var o = JSON.parse(json);
      if (o && typeof o === 'object' && !Array.isArray(o) && Array.isArray(o.items)) o.items.forEach(function (m) {
        if (!m || typeof m !== 'object' || Array.isArray(m) || typeof m.price !== 'number') return;
        var p = { price: m.price, currency: m.currency == null ? '' : String(m.currency), date: m.date == null ? '' : String(m.date), store: '', city: '', state: '', nearby: false };
        if (p.currency !== 'USD' || !PR.Dates.valid(p.date) || p.price <= 0 || p.price > 10000) return;
        var l = m.location;
        if (l && typeof l === 'object' && !Array.isArray(l)) {
          p.store = trimTo(l.osm_name, 60); p.city = trimTo(l.osm_address_city, 60);
          var cc = l.osm_address_country_code == null ? '' : String(l.osm_address_country_code);
          if (cc !== '' && cc.toUpperCase() !== 'US') return;
          p.state = state(l.osm_address_postcode == null ? '' : String(l.osm_address_postcode));
          p.nearby = region !== 'US' && p.state !== '' && regionOfState(p.state) === region;
        }
        out.push(p);
      });
    } catch (e) { /* ignored */ }
    out = out.map(function (p, i) { return [p, i]; }).sort(function (a, b) {
      if (a[0].nearby !== b[0].nearby) return a[0].nearby ? -1 : 1;
      return a[0].date < b[0].date ? 1 : a[0].date > b[0].date ? -1 : a[1] - b[1];
    }).map(function (x) { return x[0]; });
    return out.slice(0, max);
  }
  function openPricesUrl(code) {
    var d = code == null ? '' : String(code).replace(/[^0-9]/g, '');
    if (d.length < 8 || d.length > 14) return '';
    return 'https://prices.openfoodfacts.org/api/v1/prices?product_code=' + d + '&order_by=-date&size=50';
  }
  PR.RegionalPrices = { REGIONS: REGIONS, REGION_NAMES: REGION_NAMES, regionName: regionName, cleanZip: cleanZip, state: state, regionOfState: regionOfState,
    region: function (zip) { return regionOfState(state(zip)); }, parse: parseTable, defaults: defaults, coverage: coverage, withinBudget: withinBudget,
    parseOpenPrices: parseOpenPrices, openPricesUrl: openPricesUrl,
    NOTE: 'Default prices are averages, not your shop\'s prices. BLS publishes about 30 everyday foods '
      + 'for the U.S. and four regions (many items only nationally); everything else is a rough built-in estimate scaled by a '
      + 'regional factor. Prices you enter always win.' };

  // ======================= Region =======================
  var US = 'us', CA = 'ca', UK = 'uk', EU = 'eu', ANZ = 'anz', OTHER = 'other';
  var RIDS = [US, CA, UK, EU, ANZ, OTHER], RNAMES = ['United States', 'Canada', 'UK & Ireland', 'Europe (EU)', 'Australia / New Zealand', 'Other'];
  function rvalid(id) { return RIDS.indexOf(id) >= 0 ? id : ''; }
  var EU_C = 'at,be,bg,hr,cy,cz,dk,ee,fi,fr,de,gr,hu,it,lv,lt,lu,mt,nl,pl,pt,ro,sk,si,es,se'.split(',');
  var EU_L = 'de,fr,it,es,nl,pl,pt,sv,da,fi,el,cs,sk,sl,hu,ro,bg,hr,et,lv,lt,mt,ga'.split(',');
  function guess(tag) {
    if (tag == null) return US;
    var p = String(tag).replace(/_/g, '-').toLowerCase().split('-'), lang = p[0], cc = p.length > 1 ? p[p.length - 1] : '';
    if (cc === 'us') return US;
    if (cc === 'ca') return CA;
    if (cc === 'gb' || cc === 'uk' || cc === 'ie') return UK;
    if (cc === 'au' || cc === 'nz') return ANZ;
    if (EU_C.indexOf(cc) >= 0) return EU;
    if (cc !== '') return OTHER;
    if (lang === 'en') return US;
    return EU_L.indexOf(lang) >= 0 ? EU : OTHER;
  }
  var FDA_9 = ['dairy', 'eggs', 'fish', 'shellfish', 'tree-nuts', 'peanuts', 'gluten', 'soy', 'sesame'];
  function A(name) { return PR.Allergens[name]; }
  var EU_14 = ['gluten', 'shellfish', 'eggs', 'fish', 'peanuts', 'soy', 'dairy', 'tree-nuts', 'celery', 'mustard', 'sesame', 'sulphites', 'lupin', 'molluscs'];
  var CANADA = ['eggs', 'dairy', 'mustard', 'peanuts', 'shellfish', 'molluscs', 'fish', 'sesame', 'soy', 'sulphites', 'tree-nuts', 'gluten'];
  var FSANZ = ['peanuts', 'tree-nuts', 'eggs', 'dairy', 'sesame', 'fish', 'shellfish', 'molluscs', 'soy', 'lupin', 'gluten', 'sulphites'];
  function rallergens(r) {
    if (r === US) return FDA_9;
    if (r === CA) return CANADA;
    if (r === UK || r === EU) return EU_14;
    if (r === ANZ) return FSANZ;
    return PR.Allergens.IDS;
  }
  function allergenListName(r) {
    return { us: 'U.S. FDA major food allergens (9)', ca: 'Health Canada priority allergens', uk: 'UK/EU 14 allergens', eu: 'EU 14 allergens',
      anz: 'FSANZ declared allergens' }[r] || 'All allergen groups the app knows';
  }
  function allergenLabel(id, r) {
    var eu = r === UK || r === EU || r === ANZ;
    if (id === 'gluten') return eu ? 'Cereals containing gluten' : r === US ? 'Wheat / gluten' : 'Gluten';
    if (id === 'shellfish') return eu || r === CA ? 'Crustaceans (shellfish)' : r === US ? 'Crustacean shellfish' : 'Shellfish';
    if (id === 'dairy') return eu || r === US || r === CA ? 'Milk' : 'Dairy';
    if (id === 'soy') return eu ? 'Soya' : 'Soy';
    if (id === 'sulphites') return r === US ? 'Sulfites' : 'Sulphites';
    return PR.Allergens.label(id);
  }
  function rdefaults(region) {
    var r = rvalid(region) === '' ? US : region;
    var d = { units: '', weightUnit: 'g', currency: '$', dateFormat: 'YYYY-MM-DD', priceSource: 'user', showCups: true, gasMarks: false, fanNote: false,
      decimalComma: false, cup250: false, clock24: false, weekStartsSunday: false, allergens: rallergens(r) };
    if (r === US) { d.units = 'us'; d.weightUnit = 'oz'; d.dateFormat = 'MM/DD/YYYY'; d.priceSource = 'bls'; d.weekStartsSunday = true; }
    else if (r === CA) { d.units = 'us'; d.weekStartsSunday = true; }
    else if (r === UK) { d.units = 'weights'; d.currency = '\u00a3'; d.dateFormat = 'DD/MM/YYYY'; d.gasMarks = true; d.fanNote = true; d.showCups = false; d.clock24 = true; }
    else if (r === EU) { d.units = 'weights'; d.currency = '\u20ac'; d.dateFormat = 'DD.MM.YYYY'; d.fanNote = true; d.decimalComma = true; d.showCups = false; d.clock24 = true; }
    else if (r === ANZ) { d.units = 'weights'; d.dateFormat = 'DD/MM/YYYY'; d.fanNote = true; d.cup250 = true; }
    else { d.units = 'metric'; d.fanNote = true; d.clock24 = true; }
    return d;
  }
  function measuringNotes(region) {
    var uk = region === UK, anz = region === ANZ, us = region === US || region === CA;
    return [
      ['Pint', uk ? 'A UK (imperial) pint is 568 ml; a U.S. pint is 473 ml. Recipes from Britain or Ireland mean 568 ml.'
        : 'A U.S. pint is 473 ml; a UK (imperial) pint is 568 ml, so British recipes need about 20% more.'],
      ['Tablespoon', anz ? 'An Australian tablespoon is 20 ml (4 teaspoons); U.S., UK and NZ tablespoons are 15 ml. The app uses 15 ml.'
        : 'A tablespoon is 15 ml (3 teaspoons); an Australian tablespoon is 20 ml.'],
      ['Cup', anz ? 'Australian and NZ cups are 250 ml; U.S. cups are about 240 ml. The app uses 240 ml, the difference rarely matters.'
        : 'A U.S. cup is about 240 ml; Australian and NZ cups are 250 ml; British recipes usually weigh instead.'],
      ['Dessertspoon', 'About 10 ml (2 teaspoons), a British and Irish measure.'],
      ['A knob of butter', 'A small lump, about 1 tablespoon (15\u201320 g). Not exact on purpose: it is for frying or finishing.'],
      ['Stick of butter', 'U.S. butter sticks are 113 g (\u00bd cup, 8 tablespoons)' + (us ? '.' : '; elsewhere butter is usually sold in 250 g blocks.')],
      ['Gas mark', 'UK oven setting: gas 4 is about 180 \u00b0C / 350 \u00b0F. See the oven table.'],
      ['Fan oven', 'Fan (convection) ovens cook hotter: use about 20 \u00b0C (25 \u00b0F) lower than a recipe written for a conventional oven.'],
      ['Ounce and fluid ounce', '1 oz (weight) = 28 g; 1 U.S. fl oz = 29.6 ml; 1 UK fl oz = 28.4 ml.']];
  }
  function formatDate(iso, f) {
    if (!PR.Dates.valid(iso)) return iso == null ? '' : iso;
    var y = iso.slice(0, 4), m = iso.slice(5, 7), d = iso.slice(8, 10);
    if (f === 'MM/DD/YYYY') return m + '/' + d + '/' + y;
    if (f === 'DD/MM/YYYY') return d + '/' + m + '/' + y;
    if (f === 'DD.MM.YYYY') return d + '.' + m + '.' + y;
    return iso;
  }
  function formatTime(min, c24) {
    var h = ((Math.trunc(min / 60) % 24) + 24) % 24, m = ((min % 60) + 60) % 60, mm = (m < 10 ? '0' : '') + m;
    if (c24) return (h < 10 ? '0' : '') + h + ':' + mm;
    return (h % 12 === 0 ? 12 : h % 12) + ':' + mm + (h < 12 ? ' AM' : ' PM');
  }
  function weekStartR(iso, sunday) { if (!sunday) return PR.Dates.weekStart(iso); return PR.Dates.addDays(iso, -((PR.Dates.dayOfWeek(iso) + 1) % 7)); }
  function rmoney(cur, v, dc) { var s = PR.Cost.money(cur, v); return dc ? s.replace(/\./g, ',') : s; }
  var TERMS = (root.PR_DATA20 || {}).terms || [];
  var STEP_ONLY = set('broiler,plastic wrap,parchment paper,skillet');
  var AMBIG = ['pepper', 'peppers', 'corn', 'grill', 'stone', 'melon', 'biscuits', 'coriander'];
  function column(r) { if (r === US) return 0; if (r === CA) return 1; if (r === UK || r === EU || r === OTHER) return 2; if (r === ANZ) return 3; return 0; }
  var MAPS = {};
  function esc(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
  function rmap(region, steps) {
    var key = column(region) + (steps ? 's' : 'i');
    if (MAPS[key]) return MAPS[key];
    var cc = column(region), pairs = [], m = new Map();
    TERMS.forEach(function (row) {
      if (!steps && STEP_ONLY.has(row[0])) return;
      for (var j = 0; j < row.length; j++) if (j !== cc && row[j] !== row[cc]) pairs.push([row[j], row[cc]]);
    });
    pairs.sort(function (a, b) { return b[0].length !== a[0].length ? b[0].length - a[0].length : (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0); });
    pairs.forEach(function (p) { if (!m.has(p[0])) m.set(p[0], p[1]); });
    AMBIG.forEach(function (a) { m.delete(a); });
    var alt = []; m.forEach(function (v, k) { alt.push(esc(k)); });
    MAPS[key] = { m: m, re: alt.length ? new RegExp('(^|[^A-Za-z])(' + alt.join('|') + ')(?![A-Za-z])', 'gi') : null };
    return MAPS[key];
  }
  function localize(text, region, steps) {
    if (text == null || text === '' || rvalid(region) === '') return text;
    var mp = rmap(region, !!steps);
    if (!mp.re) return text;
    mp.re.lastIndex = 0;
    if (!mp.re.test(text)) return text;
    mp.re.lastIndex = 0;
    return text.replace(mp.re, function (all, pre, src) {
      var rep = mp.m.get(src.toLowerCase());
      if (rep == null) rep = src;
      else if (src === src.toUpperCase() && src.length > 1) rep = rep.toUpperCase();
      else if (src.charAt(0) !== src.charAt(0).toLowerCase()) rep = rep.charAt(0).toUpperCase() + rep.slice(1);
      return pre + rep;
    });
  }
  function holidayDefaults(region) {
    var r = rvalid(region) === '' ? US : region;
    var out = ['new-year', 'valentines', 'easter', 'halloween', 'christmas-eve', 'christmas', 'new-years-eve'];
    var add = { us: ['super-bowl', 'st-patricks', 'mothers-day-us', 'memorial-day', 'fathers-day', 'july-4', 'labor-day', 'thanksgiving-us'],
      ca: ['pancake-day', 'mothers-day-us', 'victoria-day', 'canada-day', 'fathers-day', 'thanksgiving-ca', 'boxing-day'],
      uk: ['burns-night', 'pancake-day', 'mothering-sunday', 'st-patricks', 'easter-monday', 'fathers-day', 'bonfire-night', 'boxing-day'],
      eu: ['epiphany', 'carnival', 'easter-monday', 'midsummer', 'bastille-day', 'st-nicholas', 'boxing-day'],
      anz: ['australia-day', 'waitangi-day', 'anzac-day', 'easter-monday', 'mothers-day-us', 'fathers-day-anz', 'boxing-day'],
      other: ['lunar-new-year', 'diwali', 'eid-al-fitr'] }[r];
    return out.concat(add);
  }
  PR.Region = { US: US, CA: CA, UK: UK, EU: EU, ANZ: ANZ, OTHER: OTHER, IDS: RIDS, NAMES: RNAMES, valid: rvalid,
    name: function (id) { var k = RIDS.indexOf(id); return k < 0 ? 'Not set' : RNAMES[k]; }, guess: guess, FDA_9: FDA_9, EU_14: EU_14, CANADA: CANADA, FSANZ: FSANZ,
    allergens: rallergens, allergenListName: allergenListName, allergenLabel: allergenLabel, defaults: rdefaults, measuringNotes: measuringNotes,
    formatDate: formatDate, formatTime: formatTime, weekStart: weekStartR, money: rmoney, localize: localize, terms: function () { return TERMS.slice(); },
    holidayDefaults: holidayDefaults, DATE_FORMATS: ['MM/DD/YYYY', 'DD/MM/YYYY', 'DD.MM.YYYY', 'YYYY-MM-DD'],
    FAN_NOTE: 'Fan (convection) oven: about 20 \u00b0C lower than the temperature given.',
    CUP_NOTE: 'Australian cups and tablespoons are slightly larger (250 ml cup, 20 ml tablespoon); the app uses 240 ml cups and 15 ml tablespoons.',
    PRICE_NOTE_NON_US: 'No bundled prices for this region: costs use only the prices you enter, in your currency. (Built-in averages are U.S. data and are not used here.)' };

  // ======================= Cuisines =======================
  var CD = (root.PR_DATA20 || {}).cuisines || { groups: [], data: [], lumps: [] };
  var GROUP_OF = new Map();
  CD.data.forEach(function (row) { for (var i = 1; i < row.length; i++) GROUP_OF.set(row[i].toLowerCase(), row[0]); });
  GROUP_OF.set('trinidadian', 'Caribbean'); GROUP_OF.set('argentinian', 'Latin America');
  function cgroup(cu) { if (cu == null) return 'International'; return GROUP_OF.get(String(cu).trim().toLowerCase()) || 'International'; }
  PR.Cuisines = { GROUPS: CD.groups, LUMPS: CD.lumps, group: cgroup,
    known: function (cu) { return cu != null && GROUP_OF.has(String(cu).trim().toLowerCase()); },
    inGroup: function (g) { for (var i = 0; i < CD.data.length; i++) if (CD.data[i][0] === g) return CD.data[i].slice(1); return []; },
    grouped: function (names) {
      var out = new Map();
      CD.groups.forEach(function (g) { out.set(g, []); });
      names.forEach(function (n) { out.get(cgroup(n)).push(n); });
      CD.groups.forEach(function (g) { if (!out.get(g).length) out.delete(g); });
      return out;
    } };

  // ======================= Household =======================
  function hparse(o) {
    var out = [];
    if (!Array.isArray(o)) return out;
    o.forEach(function (m) {
      if (!m || typeof m !== 'object' || Array.isArray(m) || out.length >= 12) return;
      var get = function (k) { return m instanceof Map ? m.get(k) : m[k]; };
      var nm = get('name') == null ? '' : String(get('name'));
      nm = PR.jtrim(PR.jtrim(nm).replace(/[<>\t\n\x0B\f\r ]+/g, ' '));
      if (nm.length > 30) nm = nm.slice(0, 30);
      if (nm === '') return;
      var al = get('allergens'), ids = [];
      if (Array.isArray(al)) PR.Allergens.ALL_IDS.forEach(function (id) { if (al.some(function (x) { return String(x) === id; })) ids.push(id); });
      out.push({ name: nm, allergens: ids, hide: get('hide') === true });
    });
    return out;
  }
  function htoList(ps) { return ps.map(function (p) { var m = { name: p.name, allergens: p.allergens.slice() }; if (p.hide) m.hide = true; return m; }); }
  function havoid(own, ps) { var s = []; own.forEach(function (x) { if (s.indexOf(x) < 0) s.push(x); }); ps.forEach(function (p) { if (p.hide) p.allergens.forEach(function (x) { if (s.indexOf(x) < 0) s.push(x); }); }); return s; }
  function halerts(r, ps, region) {
    var out = [];
    if (!ps.length) return out;
    PR.Allergens.flags(r, PR.Allergens.ALL_IDS).forEach(function (id) {
      var who = ps.filter(function (p) { return p.allergens.indexOf(id) >= 0; }).map(function (p) { return p.name; });
      if (who.length) out.push(allergenLabel(id, region) + ': ' + who.join(', '));
    });
    return out;
  }
  PR.Household = { MAX: 12, MAX_NAME: 30, parse: hparse, toList: htoList, avoid: havoid, alerts: halerts };

  // ======================= Recipe editor helpers =======================
  var MAX_TITLE = 120, MAX_ING = 100, MAX_STEPS = 100, MAX_STEP = 2000, MAX_TAG = 30, MAX_TAGS = 12, MAX_NOTE = 2000;
  var CATEGORIES = ['Main', 'Breakfast', 'Soup', 'Salad', 'Side', 'Snack', 'Dessert', 'Baking', 'Drink', 'Sauce'];
  function ingredientLine(l) { if (l == null) return null; var o = PR.RecipeImport.ingredientLine(l); return o ? PR.makeIngredient(o.name, o.qty, o.unit, o.optional) : null; }
  function lineOf(i) {
    var q = isNaN(i.amount) ? '' : PR.formatQty(i.amount, i.unit, 1);
    var s = (q === '' ? '' : q + ' ') + i.name;
    return i.optional ? s + ', optional' : s;
  }
  function parseIngredients(text) {
    var out = [];
    if (text == null) return out;
    text.split('\n').forEach(function (l) { var i = ingredientLine(l); if (i && out.length < MAX_ING) out.push(i); });
    return out;
  }
  function parseSteps(text) {
    var out = [];
    if (text == null) return out;
    text.split('\n').forEach(function (l) {
      var s = l.trim().replace(/^(?:step\s*)?\d+[.):]\s*/i, '').trim();
      if (s !== '' && out.length < MAX_STEPS) out.push(s.length > MAX_STEP ? s.slice(0, MAX_STEP) : s);
    });
    return out;
  }
  function parseTags(text) {
    var out = [];
    if (text == null) return out;
    text.split(',').forEach(function (t) {
      var s = t.trim().replace(/\s+/g, ' ').toLowerCase();
      if (s.length > MAX_TAG) s = s.slice(0, MAX_TAG);
      if (s !== '' && out.indexOf(s) < 0 && out.length < MAX_TAGS) out.push(s);
    });
    return out;
  }
  function newDraft() { return { title: '', category: 'Main', cuisine: '', ingredients: '', steps: '', tags: '', note: '', basedOn: '', prep: -1, cook: -1, servings: 4, meals: [] }; }
  function problem(d) {
    if (d.title == null || d.title.trim() === '') return 'Give the recipe a name.';
    if (d.title.trim().length > MAX_TITLE) return 'The name is too long (120 characters at most).';
    if (!parseIngredients(d.ingredients).length) return 'Add at least one ingredient, one per line.';
    if (d.servings < 1 || d.servings > 24) return 'Servings must be between 1 and 24.';
    if (d.prep > 1440 || d.cook > 2880) return 'That time looks too long.';
    return '';
  }
  function build(d, id, foods) {
    var t = d.title.trim().replace(/\s+/g, ' ');
    var r = { id: id, title: t.length > MAX_TITLE ? t.slice(0, MAX_TITLE) : t, category: d.category && d.category.trim() ? d.category.trim() : 'Main',
      cuisine: (d.cuisine || '').trim().slice(0, 60),  prepMin: d.prep < 0 ? -1 : Math.min(d.prep, 1440), cookMin: d.cook < 0 ? -1 : Math.min(d.cook, 2880),
      servings: Math.max(1, Math.min(24, d.servings)), servingsKnown: true, tagsInferred: true, source: 'user', image: '', sourceUrl: '',
      tags: [], meals: (d.meals || []).filter(PR.isMealType), ingredients: parseIngredients(d.ingredients), steps: parseSteps(d.steps),
      diets: [], userTags: parseTags(d.tags), note: (d.note || '').trim().slice(0, MAX_NOTE), basedOn: d.basedOn || '' };
    r.tags = PR.classify(r.ingredients);
    r.diets = classify(r, foods);
    return r;
  }
  function draftOf(r) {
    var d = newDraft();
    d.title = r.title; d.category = r.category; d.cuisine = r.cuisine; d.prep = r.prepMin; d.cook = r.cookMin; d.servings = r.servings;
    d.meals = (r.meals || []).slice();
    d.ingredients = r.ingredients.map(lineOf).join('\n').trim();
    d.steps = r.steps.join('\n').trim();
    d.tags = (r.userTags || []).join(', ');
    d.note = r.note || ''; d.basedOn = r.basedOn || '';
    return d;
  }
  function myVersion(r) {
    var d = draftOf(r);
    if (r.id.indexOf('u:') !== 0) { d.title = (r.title + ' (my version)').slice(0, MAX_TITLE); d.basedOn = r.id; }
    return d;
  }
  function packWrite(name, recipes, ver, today) {
    var m = new Map([['app', 'My Pantry Recipes'], ['kind', 'recipe-pack'], ['format', 1], ['appVersion', ver], ['exported', today], ['name', name == null ? '' : name],
      ['recipes', recipes.filter(function (r) { return r.id.indexOf('u:') === 0; }).map(PR.recipeToMap)]]);
    return PR.Json.write(m);
  }
  function PackError(msg) { var e = new Error(msg); e.pack = true; return e; }
  function plain(o) {
    if (o instanceof Map) { var r = {}; o.forEach(function (v, k) { r[k] = plain(v); }); return r; }
    if (Array.isArray(o)) return o.map(plain);
    return o;
  }
  function packParse(text, existing, foods, seed) {
    var o;
    try { o = JSON.parse(text); } catch (e) { throw PackError('This file is not a recipe pack.'); }
    if (!o || typeof o !== 'object' || Array.isArray(o)) throw PackError('This file is not a recipe pack.');
    if (o.kind !== 'recipe-pack' && o.app !== 'Pantry Recipes' && o.app !== 'My Pantry Recipes') throw PackError('This file is not a recipe pack.');
    if (o.kind === 'recipe-pack' && (typeof o.format !== 'number' || Math.trunc(o.format) > 1)) throw PackError('This recipe pack is from a newer version of the app.');
    if (!Array.isArray(o.recipes)) throw PackError('This recipe pack has no recipes.');
    var out = [], used = new Set(existing), n = seed;
    for (var x = 0; x < o.recipes.length && out.length < 500; x++) {
      var m = o.recipes[x];
      if (!m || typeof m !== 'object' || Array.isArray(m)) continue;
      var r;
      try { r = PR.recipeFromMap(m); } catch (e) { continue; }
      if (r.id.indexOf('u:') !== 0 || r.id.length > 80 || r.title.trim() === '' || !r.ingredients.length) continue;
      if (r.title.length > MAX_TITLE) r.title = r.title.slice(0, MAX_TITLE);
      r.ingredients = r.ingredients.slice(0, MAX_ING); r.steps = r.steps.slice(0, MAX_STEPS);
      if (used.has(r.id)) { var id; do { id = 'u:' + (n++).toString(36); } while (used.has(id)); r.id = id; }
      r.tags = PR.classify(r.ingredients);
      r.diets = classify(r, foods);
      used.add(r.id);
      out.push(r);
    }
    if (!out.length) throw PackError('No recipes could be read from this pack.');
    return out;
  }
  function shareText(r, servings) {
    var b = r.title + '\n', t = PR.totalMinutes(r);
    b += servings + (servings === 1 ? ' serving' : ' servings');
    if (t > 0) b += ' \u00b7 ' + t + ' min';
    b += '\n\n';
    if (r.note) b += r.note + '\n\n';
    b += 'Ingredients\n';
    var scale = servings / Math.max(1, r.servings);
    r.ingredients.forEach(function (i) { var q = PR.formatQty(i.amount, i.unit, scale); b += '- ' + (q === '' ? '' : q + ' ') + i.name + (i.optional ? ' (optional)' : '') + '\n'; });
    if (r.steps.length) { b += '\nSteps\n'; r.steps.forEach(function (s, k) { b += (k + 1) + '. ' + s + '\n'; }); }
    return b + '\nFrom My Pantry Recipes';
  }
  function hesc(s) { return String(s).replace(/&/g, '&amp;').replace(/[<]/g, '&lt;').replace(/[>]/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;'); }
  function cookbookHtml(title, recipes, ratings, today) {
    var t = title == null || title.trim() === '' ? 'Our Family Cookbook' : title.trim(), b = [];
    b.push('<!DOCTYPE html>\n<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">\n');
    b.push('<meta http-equiv="Content-Security-Policy" content="default-src \'none\'; style-src \'unsafe-inline\'">\n');
    b.push('<title>' + hesc(t) + '</title>\n<style>\n'
      + 'body{font:16px/1.5 Georgia,serif;color:#222;max-width:46rem;margin:0 auto;padding:1.5rem}'
      + 'h1{text-align:center;font-size:2.2rem;margin:3rem 0 .5rem}.sub{text-align:center;color:#555}'
      + '.toc{margin:2rem 0}.toc li{margin:.15rem 0}'
      + 'section{page-break-before:always;break-before:page;padding-top:1rem}'
      + 'h2{font-size:1.6rem;margin:0 0 .25rem}.meta{color:#555;font-size:.9rem}.note{font-style:italic}'
      + 'ul,ol{padding-left:1.4rem}li{margin:.2rem 0}h3{font-size:1.05rem;margin:1rem 0 .3rem;text-transform:uppercase;letter-spacing:.05em}'
      + 'footer{margin-top:3rem;color:#777;font-size:.8rem;text-align:center}@media print{body{padding:0}a{color:inherit}}\n</style></head><body>\n');
    b.push('<h1>' + hesc(t) + '</h1>\n<p class="sub">' + recipes.length + (recipes.length === 1 ? ' recipe' : ' recipes') + ' \u00b7 ' + hesc(today) + '</p>\n');
    b.push('<ol class="toc">\n');
    recipes.forEach(function (r, k) { b.push('<li><a href="#r' + (k + 1) + '">' + hesc(r.title) + '</a></li>\n'); });
    b.push('</ol>\n');
    recipes.forEach(function (r, k) {
      b.push('<section id="r' + (k + 1) + '">\n<h2>' + hesc(r.title) + '</h2>\n<p class="meta">');
      var meta = [r.servings + (r.servings === 1 ? ' serving' : ' servings')];
      if (r.prepMin > 0) meta.push('prep ' + r.prepMin + ' min');
      if (r.cookMin > 0) meta.push('cook ' + r.cookMin + ' min');
      if (r.cuisine) meta.push(r.cuisine);
      var rt = ratings ? ratings.get(r.id) : null;
      if (rt && rt.stars > 0) { var st = ''; for (var s = 0; s < 5; s++) st += s < rt.stars ? '\u2605' : '\u2606'; meta.push(st); }
      b.push(hesc(meta.join(' \u00b7 ')) + '</p>\n');
      if (r.note) b.push('<p class="note">' + hesc(r.note) + '</p>\n');
      b.push('<h3>Ingredients</h3>\n<ul>\n');
      r.ingredients.forEach(function (i) { var q = PR.formatQty(i.amount, i.unit, 1); b.push('<li>' + hesc((q === '' ? '' : q + ' ') + i.name + (i.optional ? ' (optional)' : '')) + '</li>\n'); });
      b.push('</ul>\n');
      if (r.steps.length) { b.push('<h3>Steps</h3>\n<ol>\n'); r.steps.forEach(function (s) { b.push('<li>' + hesc(s) + '</li>\n'); }); b.push('</ol>\n'); }
      if (rt && rt.note) b.push('<p class="note">Notes: ' + hesc(rt.note) + '</p>\n');
      b.push('</section>\n');
    });
    b.push('<footer>Made with My Pantry Recipes</footer>\n</body></html>\n');
    return b.join('');
  }
  PR.RecipeEditor = { CATEGORIES: CATEGORIES, MAX_TITLE: MAX_TITLE, ingredientLine: ingredientLine, lineOf: lineOf, parseIngredients: parseIngredients,
    parseSteps: parseSteps, parseTags: parseTags, newDraft: newDraft, problem: problem, build: build, draftOf: draftOf, myVersion: myVersion,
    packWrite: packWrite, packParse: packParse, shareText: shareText, cookbookHtml: cookbookHtml, plain: plain };

  // ---------------- v2.0 Stage 2 kitchen helpers (parity with core/Helpers20.java) ----------------
  function kh_cols(line, n) { var p = line.split('|'), out = []; for (var i = 0; i < n; i++) out.push(i < p.length ? p[i].trim() : ''); return out; }
  function kh_parts(s) {
    var v = 0;
    String(s).trim().split(/\s+/).forEach(function (w) {
      var sl = w.indexOf('/');
      if (sl > 0) { var a = parseFloat(w.substring(0, sl)), b = parseFloat(w.substring(sl + 1)); if (!isNaN(a) && b) v += a / b; }
      else if (w && !isNaN(parseFloat(w))) v += parseFloat(w);
    });
    return v;
  }
  function kh_parse(text) {
    var h = { equivs: [], rescues: [], blends: [], times: [], explains: [] }, sec = '', r = null, b = null, e = null;
    String(text).split('\n').forEach(function (raw) {
      var line = raw.trim(), c;
      if (!line || line.charAt(0) === '#') return;
      if (line.charAt(0) === '[' && line.charAt(line.length - 1) === ']') { sec = line.substring(1, line.length - 1); return; }
      if (sec === 'equiv') { c = kh_cols(line, 4); h.equivs.push({ group: c[0], amount: c[1], equals: c[2], note: c[3] }); }
      else if (sec === 'times') { c = kh_cols(line, 5); h.times.push({ group: c[0], item: c[1], method: c[2], time: c[3], note: c[4] }); }
      else if (sec === 'rescue') {
        if (line.charAt(0) === '@') { c = kh_cols(line.substring(1), 2); r = { problem: c[0], dishes: c[1], fixes: [], prevent: '' }; h.rescues.push(r); }
        else if (r && line.charAt(0) === '-') r.fixes.push(line.substring(1).trim());
        else if (r && line.charAt(0) === '!') r.prevent = line.substring(1).trim();
      } else if (sec === 'blends') {
        if (line.charAt(0) === '@') { c = kh_cols(line.substring(1), 3); b = { name: c[0], country: c[1], use: c[2], parts: [] }; h.blends.push(b); }
        else if (b && line.charAt(0) === '-') { c = kh_cols(line.substring(1), 3); b.parts.push({ partsText: c[0], parts: kh_parts(c[0]), spice: c[1], flag: c[2] }); }
      } else if (sec === 'explain') {
        if (line.charAt(0) === '@') { c = kh_cols(line.substring(1), 2); e = { name: c[0], text: c[1], names: [] }; h.explains.push(e); }
        else if (e && line.charAt(0) === '=') { c = kh_cols(line.substring(1), 2); e.names.push([c[0].toLowerCase(), c[1]]); }
      }
    });
    return h;
  }
  function kh_matches(hay, q) {
    q = (q || '').trim().toLowerCase();
    if (!q) return true;
    hay = hay.toLowerCase();
    var ws = q.split(/\s+/);
    for (var i = 0; i < ws.length; i++) if (hay.indexOf(ws[i]) < 0) return false;
    return true;
  }
  function kh_findEquivs(h, q) { return h.equivs.filter(function (x) { return kh_matches(x.group + ' ' + x.amount + ' ' + x.equals + ' ' + x.note, q); }); }
  function kh_findTimes(h, q) { return h.times.filter(function (x) { return kh_matches(x.group + ' ' + x.item + ' ' + x.method + ' ' + x.note, q); }); }
  function kh_findRescues(h, q) {
    var out = h.rescues.filter(function (x) { return kh_matches(x.problem + ' ' + x.dishes, q); });
    if (!out.length && (q || '').trim()) out = h.rescues.filter(function (x) { return kh_matches(x.fixes.join(' ') + ' ', q); });
    return out;
  }
  function kh_findExplains(h, q) {
    var out = h.explains.filter(function (x) { return kh_matches(x.name + ' ' + x.names.map(function (n) { return n[1]; }).join(' ') + ' ', q); });
    if (!out.length && (q || '').trim()) out = h.explains.filter(function (x) { return kh_matches(x.text, q); });
    return out;
  }
  function kh_nameOf(e, code) { for (var i = 0; i < e.names.length; i++) if (e.names[i][0] === code) return e.names[i][1]; return null; }
  function kh_local(e, region) {
    var r = region || '', v = kh_nameOf(e, r);
    if (v == null && r === 'ca') v = kh_nameOf(e, 'us');
    if (v == null && r === 'eu') v = kh_nameOf(e, 'uk');
    return v;
  }
  var KH_REG = { us: 'US', uk: 'UK & Ireland', ca: 'Canada', anz: 'Australia & NZ', eu: 'Europe', 'in': 'India' };
  function kh_regionLabel(code) { return KH_REG[code] || 'Also'; }
  function kh_gcd(a, b) { return b === 0 ? a : kh_gcd(b, a % b); }
  function kh_frac(e8) { var whole = Math.floor(e8 / 8), r = e8 % 8, f = ''; if (r) { var g = kh_gcd(r, 8); f = (r / g) + '/' + (8 / g); } if (!whole) return f; return f ? whole + ' ' + f : String(whole); }
  function kh_spoons(tsp) {
    if (tsp < 1 / 16) return 'pinch';
    var e8 = Math.round(tsp * 8); // Java Math.round: half up, same for positives
    if (e8 === 0) e8 = 1;
    if (e8 >= 24) { var tb = Math.floor(e8 / 24), rest = e8 % 24, s = tb + ' tbsp'; return rest === 0 ? s : s + ' + ' + kh_frac(rest) + ' tsp'; }
    return kh_frac(e8) + ' tsp';
  }
  function kh_build(b, parts, totalTsp, noSalt, mild) {
    var n = b.parts.length, w = [], sum = 0, out = [];
    for (var i = 0; i < n; i++) {
      var p = b.parts[i], v = parts && i < parts.length ? parts[i] : p.parts;
      if (noSalt && p.flag === 'salt') v = 0;
      if (mild && p.flag === 'heat') v = v / 2;
      w[i] = Math.max(0, v); sum += w[i];
    }
    if (sum <= 0) return out;
    for (var j = 0; j < n; j++) if (w[j] > 0) out.push([kh_spoons(w[j] / sum * totalTsp), b.parts[j].spice, b.parts[j].flag]);
    return out;
  }
  function kh_blendText(b, rows, totalTsp) {
    return b.name + ' (' + kh_spoons(totalTsp) + ')\n' + rows.map(function (r) { return '- ' + r[0] + ' ' + r[1] + '\n'; }).join('') + 'Use: ' + b.use;
  }
  var kh_memo = null;
  function kh_data() { if (!kh_memo) kh_memo = kh_parse(root.PR_HELPERS20 || ''); return kh_memo; }
  PR.Helpers20 = { parse: kh_parse, data: kh_data, matches: kh_matches, parseParts: kh_parts, findEquivs: kh_findEquivs, findTimes: kh_findTimes,
    findRescues: kh_findRescues, findExplains: kh_findExplains, localName: kh_local, regionLabel: kh_regionLabel, spoons: kh_spoons, build: kh_build,
    blendText: kh_blendText, BATCH_TSP: [3, 6, 12, 24], BATCH_LABEL: ['1 tbsp', '2 tbsp', '1/4 cup', '1/2 cup'] };
})(typeof window !== 'undefined' ? window : globalThis);
