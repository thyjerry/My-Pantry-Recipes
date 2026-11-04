package org.pantryrecipes.app.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Conservative dietary tagging from canonical ingredient keys. Used to build the bundled
 * tags and to infer tags for TheMealDB recipes (which carry no dietary data).
 */
public final class DietClassifier {
    private DietClassifier() { }

    private static Set<String> set(String s) { return new HashSet<String>(Arrays.asList(s.split(","))); }

    private static final Set<String> MEAT_WORDS = set("chicken,beef,pork,lamb,bacon,ham,fish,salmon,tuna,prawn,shrimp,"
            + "sausage,turkey,duck,crab,lobster,mussel,clam,squid,octopus,veal,venison,goat,mutton,chorizo,salami,"
            + "pepperoni,prosciutto,pancetta,gelatin,gelatine,lard,suet,oyster,mackerel,sardine,cod,haddock,kipper,"
            + "scallop,steak,meat,anchovy,trout,worcestershire,mince,brisket,chuck,sirloin,ribeye,lardon,meatball,"
            + "snapper,halibut,monkfish,tilapia,pollock,hake,basa,gammon,rabbit,quail,pheasant,goose,liver,kidney_x,"
            + "bouillon_x,caviar,roe,eel,herring,swordfish,seabass,bass,bream,partridge,pigeon,oxtail,tripe,bone,marrow,bratwurst,mortadella,nduja,jerky,dashi,kielbasa,andouille,"
            + "frankfurter,marshmallow,lox,calamari,bonito");
    private static final Set<String> NOT_MEAT_PHRASES = set("goat cheese,fish sauce_x,beef tomato,mushroom stock,"
            + "vegetable stock,oyster mushroom,meat free,vegan sausage,veggie sausage,vegetarian sausage,plant based mince");
    private static final Set<String> DAIRY_WORDS = set("milk,butter,cream,cheese,yogurt,ghee,buttermilk,whey,custard,"
            + "paneer,cheddar,parmesan,mozzarella,feta,ricotta,mascarpone,halloumi,gruyere,brie,gouda,camembert,"
            + "fraiche,quark,kefir,stilton,emmental,provolone,pecorino,manchego,raclette,curd,lassi,gorgonzola,roquefort,jack,labneh,cotija,burrata,queso,leche,nutella,"
            + "alfredo,ranch,colby,tzatziki");
    private static final Set<String> NOT_DAIRY_PHRASES = set("bean curd,coconut milk,almond milk,oat milk,soy milk,soya milk,rice milk,"
            + "coconut cream,peanut butter,almond butter,cashew butter,butter bean,cream of tartar,vegan cheese,"
            + "nut butter,apple butter,cocoa butter,butternut,butternut squash,vegan butter,dairy free spread,"
            + "coconut yogurt,soy yogurt,cream cracker_x,sunflower seed butter,dairy free milk,vegan cream,cashew cream,vegan yogurt,plant yogurt,dairy free yogurt,dairy free chocolate");
    private static final Set<String> OTHER_ANIMAL = set("egg,honey,mayonnaise,mayo,meringue,aioli,eggs,yolk,ladyfinger,amaretti,tartar,caesar");
    private static final Set<String> NOT_ANIMAL_PHRASES = set("eggplant,vegan mayonnaise,egg free_x,cream of tartar");
    private static final Set<String> GLUTEN_WORDS = set("flour,bread,breadcrumb,pasta,spaghetti,penne,macaroni,fusilli,"
            + "linguine,rigatoni,farfalle,tagliatelle,lasagne,lasagna,orzo,fettuccine,noodle,couscous,bulgur,barley,"
            + "farro,semolina,soy,tortilla,pita,naan,bun,roll,bagel,croissant,pastry,dough,cracker,biscuit,cookie,cake,"
            + "beer,ale,oats,oat,wheat,rye,seitan,worcestershire,malt,panko,crouton,ramen,udon,gnocchi,wonton,filo,"
            + "phyllo,hoisin,teriyaki,oyster,spelt,muffin,brioche,baguette,ciabatta,sourdough,pretzel,cereal,granola,"
            + "digestive,graham,wafer,crumpet,pancake,waffle,stuffing,suet,gravy,dumpling,vermicelli_x,ravioli,tortellini,"
            + "breadstick,stout,crumb,sponge,scone,matzo,freekeh,triticale,couscou,cornflake_x,stock_x,miso,gochujang,"
            + "pie,tart,shortcrust,puff,wrap,chapati,roti,paratha,pizza,brownie,pudding_x,bisquick,cous,flatbread,soba,kecap,"
            + "doubanjiang,ladyfinger,papadum_x,roux");
    private static final Set<String> NOT_GLUTEN_PHRASES = set("rice flour,corn flour_x,almond flour,chickpea flour,coconut flour,"
            + "buckwheat flour,tapioca flour,potato flour,rice noodle,glass noodle,corn tortilla,gluten free bread,"
            + "gluten free pasta,gluten free flour,tamari,rice paper,cornstarch,rice vermicelli,gluten free soy sauce,"
            + "soy milk,soya milk,soy yogurt,soybean,soy bean,edamame,butternut,butternut squash,cream of tartar,"
            + "puff rice_x,gluten free oats,arrowroot,potato starch,sweet potato,egg roll_x,bread soda_x,soy chunk_x,"
            + "pie pumpkin_x,rice cake,pumpkin pie spice,mixed spice,shortbread_x,oyster mushroom,mushroom oyster,"
            + "pastry_x,rice wrap,lettuce wrap,lettuce leaf");

    /** Returns the set of dietary tags implied by the given ingredient keys. */
    public static Set<String> classify(List<Ingredient> ingredients) {
        boolean meat = false, dairy = false, animal = false, gluten = false;
        for (Ingredient i : ingredients) {
            String k = i.key;
            if (isMeat(k)) meat = true;
            if (isDairy(k)) dairy = true;
            if (isOtherAnimal(k)) animal = true;
            if (hasGluten(k)) gluten = true;
        }
        Set<String> tags = new LinkedHashSet<String>();
        if (!meat) tags.add(Recipe.VEGETARIAN);
        if (!meat && !dairy && !animal) tags.add(Recipe.VEGAN);
        if (!gluten) tags.add(Recipe.GLUTEN_FREE);
        if (!dairy) tags.add(Recipe.DAIRY_FREE);
        return tags;
    }

    private static boolean anyWord(String key, Set<String> words) {
        for (String w : key.split(" ")) if (words.contains(w)) return true;
        return false;
    }

    private static boolean containsPhrase(String key, Set<String> phrases) {
        for (String p : phrases) if (key.equals(p) || key.contains(p)) return true;
        return false;
    }

    public static boolean isMeat(String key) {
        if (key.equals("fish sauce") || key.equals("oyster sauce") || key.equals("hot dog")) return true;
        return anyWord(key, MEAT_WORDS) && !containsPhrase(key, NOT_MEAT_PHRASES);
    }

    public static boolean isDairy(String key) {
        if (key.equals("half and half")) return true;
        return anyWord(key, DAIRY_WORDS) && !containsPhrase(key, NOT_DAIRY_PHRASES);
    }

    public static boolean isOtherAnimal(String key) {
        return anyWord(key, OTHER_ANIMAL) && !containsPhrase(key, NOT_ANIMAL_PHRASES);
    }

    public static boolean hasGluten(String key) {
        return anyWord(key, GLUTEN_WORDS) && !containsPhrase(key, NOT_GLUTEN_PHRASES);
    }
}
