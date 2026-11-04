package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Name-based allergen flags and the user's dislikes. This only looks at ingredient names, so it is a hint
 * and never a guarantee (the UI says so wherever it is shown).
 */
public final class Allergens {
    private Allergens() { }

    public static final String NOTE = "Allergen flags and hidden ingredients are matched by ingredient name only. "
            + "They are not a guarantee: always check labels, and take extra care with serious allergies.";

    public static final String PEANUTS = "peanuts", TREE_NUTS = "tree-nuts", DAIRY = "dairy", EGGS = "eggs",
            GLUTEN = "gluten", SOY = "soy", FISH = "fish", SHELLFISH = "shellfish", SESAME = "sesame";

    public static final List<String> IDS = java.util.Collections.unmodifiableList(Arrays.asList(
            PEANUTS, TREE_NUTS, DAIRY, EGGS, GLUTEN, SOY, FISH, SHELLFISH, SESAME));

    /** v2.0: the other groups in the EU/UK 14, Canada and FSANZ lists. Shown when the region's list includes them. */
    public static final String CELERY = "celery", MUSTARD = "mustard", SULPHITES = "sulphites", LUPIN = "lupin", MOLLUSCS = "molluscs";
    public static final List<String> EXTRA_IDS = java.util.Collections.unmodifiableList(Arrays.asList(CELERY, MUSTARD, SULPHITES, LUPIN, MOLLUSCS));
    /** Every group the app can flag or avoid: IDS then EXTRA_IDS. */
    public static final List<String> ALL_IDS;
    static {
        List<String> a = new ArrayList<String>(IDS);
        a.addAll(EXTRA_IDS);
        ALL_IDS = java.util.Collections.unmodifiableList(a);
    }

    public static String label(String id) {
        if (PEANUTS.equals(id)) return "Peanuts";
        if (TREE_NUTS.equals(id)) return "Tree nuts";
        if (DAIRY.equals(id)) return "Dairy";
        if (EGGS.equals(id)) return "Eggs";
        if (GLUTEN.equals(id)) return "Gluten";
        if (SOY.equals(id)) return "Soy";
        if (FISH.equals(id)) return "Fish";
        if (SHELLFISH.equals(id)) return "Shellfish";
        if (SESAME.equals(id)) return "Sesame";
        if (CELERY.equals(id)) return "Celery";
        if (MUSTARD.equals(id)) return "Mustard";
        if (SULPHITES.equals(id)) return "Sulphites";
        if (LUPIN.equals(id)) return "Lupin";
        if (MOLLUSCS.equals(id)) return "Molluscs";
        return id;
    }

    private static Set<String> set(String s) { return new HashSet<String>(Arrays.asList(s.split(","))); }

    private static final Set<String> PEANUT_WORDS = set("peanut,peanuts,satay,groundnut,groundnuts");
    private static final Set<String> TREE_NUT_WORDS = set("almond,almonds,walnut,walnuts,pecan,pecans,cashew,cashews,"
            + "hazelnut,hazelnuts,pistachio,pistachios,macadamia,macadamias,brazil,nut,nuts,praline,marzipan,frangipane,"
            + "amaretti,pesto,nutella,gianduja,chestnut,chestnuts");
    private static final Set<String> EGG_WORDS = set("egg,eggs,yolk,yolks,mayonnaise,mayo,meringue,aioli,custard,"
            + "eggnog,hollandaise,bearnaise,albumen,brioche,ladyfinger,ladyfingers,carbonara");
    private static final Set<String> NOT_EGG = set("eggplant,vegan mayonnaise,egg free,eggless,custard powder,custard apple");
    private static final Set<String> SOY_WORDS = set("soy,soya,tofu,edamame,miso,tempeh,tamari,teriyaki,hoisin,kecap");
    private static final Set<String> FISH_WORDS = set("fish,salmon,tuna,cod,haddock,anchovy,anchovies,sardine,sardines,"
            + "mackerel,trout,tilapia,halibut,pollock,hake,monkfish,swordfish,snapper,bass,herring,kipper,"
            + "kippers,pilchard,pilchards,plaice,sole,bream,carp,catfish,whitebait,caviar,roe,bonito,dashi,worcestershire,"
            + "caesar,bottarga,surimi,eel");
    private static final Set<String> SHELLFISH_WORDS = set("prawn,prawns,shrimp,shrimps,crab,crabs,lobster,lobsters,"
            + "crayfish,langoustine,langoustines,scallop,scallops,mussel,mussels,clam,clams,oyster,oysters,squid,"
            + "calamari,octopus,cockle,cockles,whelk,whelks,scampi");
    private static final Set<String> NOT_SHELLFISH = set("oyster mushroom,oyster mushrooms,crab apple,crabapple");
    private static final Set<String> SESAME_WORDS = set("sesame,tahini,halva,halvah,gomasio,furikake,za'atar,zaatar,"
            + "hummus,houmous");

    private static final Set<String> CELERY_WORDS = set("celery,celeriac");
    private static final Set<String> MUSTARD_WORDS = set("mustard,dijon,piccalilli");
    private static final Set<String> LUPIN_WORDS = set("lupin,lupini,lupine");
    private static final Set<String> MOLLUSC_WORDS = set("squid,calamari,octopus,mussel,mussels,clam,clams,oyster,oysters,scallop,scallops,"
            + "cockle,cockles,whelk,whelks,snail,snails,escargot,abalone,cuttlefish");
    /** Sulphites: foods that usually contain them; always shown as "check label". */
    private static final Set<String> SULPHITE_WORDS = set("wine,sherry,vermouth,marsala,madeira,prosecco,champagne,cider,port");
    private static final Set<String> SULPHITE_PHRASES = set("dried apricot,dried fruit,sultana,raisin,dried cranberr,dried mango,dried fig,"
            + "glace cherr,candied peel,mixed peel");

    /** v1.8: false positives removed from the gluten flag (the words "squash", "ale", "pizza", "bagel" alone). */
    private static final Set<String> NOT_GLUTEN = set("spaghetti squash,ginger ale,pizza sauce,everything bagel seasoning");
    /**
     * v1.8: gluten flags that depend on the product, shown as "Gluten (check label)" instead of a plain flag.
     * They still count when you avoid gluten, so a recipe is never shown as safe because of this list.
     */
    private static final Set<String> MAYBE_GLUTEN = set("oats,oat,oat milk,oat flour,steel oats,soba noodle,buckwheat noodle,"
            + "miso,rice malt syrup,rice cereal,worcestershire sauce,fish cake,crispy fried onion,fried onion");
    private static final Set<String> MAYBE_GLUTEN_WORDS = set("oats,oat,soba,miso,worcestershire");

    private static boolean anyWord(String key, Set<String> words) {
        for (String w : key.split(" ")) if (words.contains(w)) return true;
        return false;
    }

    private static boolean phrase(String key, Set<String> phrases) {
        for (String p : phrases) if (key.contains(p)) return true;
        return false;
    }

    /**
     * v1.8: whether an ingredient row mentions the allergen group: its key, and for every group except gluten also
     * the name as written (so "peanut oil", which the matcher treats as vegetable oil, or "cheese ravioli" still flag).
     * Names that say "dairy-free", "vegan", "egg-free", "nut-free" do not flag that group from the name alone.
     */
    public static boolean has(String id, Ingredient i) {
        String raw = raw(i.name);
        for (String f : freeWords(id)) if (raw.contains(f + " free") || raw.contains(f + "free")) return false;
        if (keyHas(id, varietyFix(i.key))) return true;
        if (GLUTEN.equals(id)) {
            return raw.contains("fried onion") || raw.contains("spring roll wrapper") || raw.contains("fish cake");
        }
        if (raw.isEmpty() || raw.equals(i.key)) return false;
        if (DAIRY.equals(id) && (raw.contains("dairy free") || raw.contains("non dairy") || raw.contains("vegan") || raw.contains("plant based")
                || raw.contains("plant butter") || raw.contains("coconut"))) return false;
        if (EGGS.equals(id) && (raw.contains("vegan") || raw.contains("egg free") || raw.contains("eggless"))) return false;
        if (TREE_NUTS.equals(id) && (raw.contains("chestnut mushroom") || raw.contains("nut free") || raw.contains("coconut") || raw.contains("nutmeg"))) return false;
        if (SOY.equals(id) && raw.contains("soy free")) return false;
        return keyHas(id, raw);
    }

    private static final String[] NO_FREE = {};

    /** Words that, followed by "free", rule a group out for that row ("egg-free mayo", "wheat-free tamari"). */
    private static String[] freeWords(String id) {
        if (PEANUTS.equals(id)) return new String[]{"peanut", "nut"};
        if (TREE_NUTS.equals(id)) return new String[]{"nut", "tree nut"};
        if (DAIRY.equals(id)) return new String[]{"dairy", "milk"}; // not "lactose-free": still milk
        if (EGGS.equals(id)) return new String[]{"egg"};
        if (GLUTEN.equals(id)) return new String[]{"gluten"};
        if (SOY.equals(id)) return new String[]{"soy"};
        if (FISH.equals(id)) return new String[]{"fish"};
        if (SHELLFISH.equals(id)) return new String[]{"shellfish"};
        if (SESAME.equals(id)) return new String[]{"sesame"};
        if (CELERY.equals(id)) return new String[]{"celery"};
        if (MUSTARD.equals(id)) return new String[]{"mustard"};
        if (SULPHITES.equals(id)) return new String[]{"sulphite", "sulfite"};
        if (LUPIN.equals(id)) return new String[]{"lupin"};
        if (MOLLUSCS.equals(id)) return new String[]{"mollusc", "shellfish"};
        return NO_FREE;
    }

    /** v1.8: the flag for this row depends on the product (check the label); see MAYBE_GLUTEN. */
    public static boolean uncertain(String id, Ingredient i) {
        if (SULPHITES.equals(id)) return has(id, i);
        return GLUTEN.equals(id) && !keyHasSure(i.key) && has(id, i);
    }

    private static boolean keyHasSure(String key) {
        if (!keyHas(GLUTEN, key) || MAYBE_GLUTEN.contains(key)) return false;
        for (String w : key.split(" ")) if (!MAYBE_GLUTEN_WORDS.contains(w) && DietClassifier.hasGluten(w)) return true;
        return false;
    }

    private static final java.util.regex.Pattern RAW_JUNK = java.util.regex.Pattern.compile("[^a-z']+");

    /** The ingredient name folded to lower-case letters and single spaces ("Peanut oil, for frying" -> "peanut oil for frying"). */
    static String raw(String name) {
        if (name == null) return "";
        String t = RAW_JUNK.matcher(Normalizer.fold(name)).replaceAll(" ").trim();
        // purpose notes and varieties that name another food: "sugar, 80 g for the custard", "lettuce, such as butter lettuce"
        int f = t.indexOf(" for the ");
        if (f >= 0) t = t.substring(0, f);
        return varietyFix(t);
    }

    /** Varieties that name another food: butter lettuce / butterhead are lettuce, butter beans are beans. */
    static String varietyFix(String t) {
        return t.replace("butter lettuce", "lettuce").replace("butterhead", "lettuce").replace("butter bean", "lima bean");
    }

    /** Groups found in a recipe whose every hit is uncertain (shown as "Gluten (check label)"). */
    public static List<String> uncertainFlags(Recipe r) {
        List<String> out = new ArrayList<String>();
        for (String id : flags(r)) {
            boolean sure = false;
            for (Ingredient i : r.ingredients) if (has(id, i) && !uncertain(id, i)) { sure = true; break; }
            if (!sure) out.add(id);
        }
        return out;
    }

    /** "Gluten" or "Gluten (check label)" for the flags list. */
    public static String flagLabel(Recipe r, String id, List<String> uncertainIds) {
        return label(id) + (uncertainIds.contains(id) ? " (check label)" : "");
    }

    /** Whether an ingredient key (as produced by Normalizer) mentions the allergen group. */
    public static boolean keyHas(String id, String key) {
        if (key == null || key.isEmpty()) return false;
        if (PEANUTS.equals(id)) return anyWord(key, PEANUT_WORDS);
        if (TREE_NUTS.equals(id)) {
            if (key.contains("pine nut") || key.contains("pinenut")) return true;
            return anyWord(key, TREE_NUT_WORDS) && !key.contains("water chestnut") && !key.contains("nut free");
        }
        if (DAIRY.equals(id)) return DietClassifier.isDairy(key);
        if (EGGS.equals(id)) return anyWord(key, EGG_WORDS) && !phrase(key, NOT_EGG);
        if (GLUTEN.equals(id)) return DietClassifier.hasGluten(key) && !NOT_GLUTEN.contains(key);
        if (SOY.equals(id)) return anyWord(key, SOY_WORDS) || key.contains("textured vegetable protein");
        if (FISH.equals(id)) return anyWord(key, FISH_WORDS);
        if (SHELLFISH.equals(id)) return anyWord(key, SHELLFISH_WORDS) && !phrase(key, NOT_SHELLFISH);
        if (SESAME.equals(id)) return anyWord(key, SESAME_WORDS) || key.contains("za atar") || key.equals("everything bagel seasoning");
        if (CELERY.equals(id)) return anyWord(key, CELERY_WORDS);
        if (MUSTARD.equals(id)) return anyWord(key, MUSTARD_WORDS);
        if (LUPIN.equals(id)) return anyWord(key, LUPIN_WORDS);
        if (MOLLUSCS.equals(id)) return (anyWord(key, MOLLUSC_WORDS) && !phrase(key, NOT_SHELLFISH)) || key.contains("oyster sauce");
        if (SULPHITES.equals(id)) return (anyWord(key, SULPHITE_WORDS) && !key.contains("port wine cheese") && !key.contains("wine free"))
                || phrase(key, SULPHITE_PHRASES);
        return false;
    }

    /** Allergen groups found in any ingredient (optional ones included), in IDS order. */
    public static List<String> flags(Recipe r) { return flags(r, IDS); }

    /** v2.0: flags for a region's list (Region.allergens), in that list's order. */
    public static List<String> flags(Recipe r, List<String> ids) {
        List<String> out = new ArrayList<String>();
        for (String id : ids) {
            for (Ingredient i : r.ingredients) {
                if (has(id, i)) { out.add(id); break; }
            }
        }
        return out;
    }

    /** Normalized dislike key for free text typed by the user. */
    public static String dislikeKey(String text) { return Normalizer.normalize(text == null ? "" : text); }

    /** Whether an ingredient key is covered by a dislike (same key, same family, or the dislike as a whole word/phrase). */
    public static boolean dislikes(String key, String dislike) {
        if (key == null || key.isEmpty() || dislike == null || dislike.isEmpty()) return false;
        if (key.equals(dislike)) return true;
        String fk = Normalizer.family(key);
        if (fk != null && (fk.equals(dislike) || fk.equals(Normalizer.family(dislike)))) return true;
        return (" " + key + " ").contains(" " + dislike + " ");
    }

    /**
     * Reasons this recipe should be hidden: "Contains peanuts" for avoided groups (any ingredient) and
     * "Contains mushroom" for disliked ingredients (required ingredients only; optional ones can be left out).
     */
    public static List<String> reasons(Recipe r, Collection<String> dislikeKeys, Collection<String> avoid) {
        Set<String> out = new LinkedHashSet<String>();
        if (avoid != null) {
            for (String id : ALL_IDS) {
                if (!avoid.contains(id)) continue;
                for (Ingredient i : r.ingredients) {
                    if (has(id, i)) { out.add("Contains " + label(id).toLowerCase()); break; }
                }
            }
        }
        if (dislikeKeys != null) {
            for (String d : dislikeKeys) {
                for (Ingredient i : r.ingredients) {
                    if (!i.optional && dislikes(i.key, d)) { out.add("Contains " + d); break; }
                }
            }
        }
        return new ArrayList<String>(out);
    }

    public static boolean hidden(Recipe r, Collection<String> dislikeKeys, Collection<String> avoid) {
        boolean noD = dislikeKeys == null || dislikeKeys.isEmpty(), noA = avoid == null || avoid.isEmpty();
        return !(noD && noA) && !reasons(r, dislikeKeys, avoid).isEmpty();
    }

    /** Whether one ingredient row should carry a warning (avoided group or dislike). */
    public static boolean ingredientWarn(Ingredient i, Collection<String> dislikeKeys, Collection<String> avoid) {
        if (avoid != null) for (String id : avoid) if (has(id, i)) return true;
        if (dislikeKeys != null) for (String d : dislikeKeys) if (dislikes(i.key, d)) return true;
        return false;
    }
}
