package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * v2.0: cuisines are tagged by country (or a named sub-region where a dish is honestly shared, such as Levantine or
 * West African); this groups them by world region for the "Browse by cuisine" filter only. Caribbean cuisines are
 * separate countries. Shared with the web app (core20.js).
 */
public final class Cuisines {
    private Cuisines() { }

    public static final List<String> GROUPS = Collections.unmodifiableList(Arrays.asList(
            "North America", "Caribbean", "Latin America", "British Isles", "Western Europe", "Southern Europe", "Northern Europe",
            "Central & Eastern Europe", "Middle East & North Africa", "Sub-Saharan Africa", "South Asia", "East Asia", "Southeast Asia",
            "Central Asia & Caucasus", "Oceania", "International"));

    private static final String[][] DATA = {
            {"North America", "American", "Southern US", "Cajun", "Creole", "Tex-Mex", "Italian-American", "Chinese-American", "Irish-American",
                    "Southwestern US", "New England", "Hawaiian", "Canadian", "Korean-Mexican", "Jewish"},
            {"Caribbean", "Jamaican", "Cuban", "Puerto Rican", "Dominican", "Haitian", "Trinidadian & Tobagonian", "Bajan", "Bahamian", "Guyanese",
                    "Grenadian", "Antiguan", "Martinican", "Guadeloupean", "Barbadian", "Belizean", "Surinamese"},
            {"Latin America", "Mexican", "Salvadoran", "Guatemalan", "Honduran", "Nicaraguan", "Costa Rican", "Panamanian", "Colombian", "Venezuelan",
                    "Ecuadorian", "Peruvian", "Bolivian", "Chilean", "Argentine", "Uruguayan", "Paraguayan", "Brazilian"},
            {"British Isles", "British", "English", "Scottish", "Welsh", "Irish", "British Indian", "Anglo-Indian"},
            {"Western Europe", "French", "Belgian", "Dutch", "German", "Austrian", "Swiss", "Luxembourgish"},
            {"Southern Europe", "Italian", "Spanish", "Portuguese", "Greek", "Cypriot", "Maltese", "Croatian", "Balkan", "Serbian", "Bosnian"},
            {"Northern Europe", "Swedish", "Norwegian", "Danish", "Finnish", "Icelandic", "Nordic", "Estonian", "Latvian", "Lithuanian"},
            {"Central & Eastern Europe", "Polish", "Czech", "Slovak", "Hungarian", "Romanian", "Bulgarian", "Ukrainian", "Russian", "Belarusian"},
            {"Middle East & North Africa", "Turkish", "Lebanese", "Syrian", "Palestinian", "Jordanian", "Israeli", "Levantine", "Iraqi", "Persian",
                    "Iranian", "Yemeni", "Saudi", "Emirati", "Egyptian", "Libyan", "Tunisian", "Algerian", "Moroccan"},
            {"Sub-Saharan Africa", "Nigerian", "Ghanaian", "Senegalese", "West African", "Ethiopian", "Eritrean", "Kenyan", "Tanzanian", "Ugandan",
                    "Somali", "South African", "Cameroonian", "Ivorian"},
            {"South Asia", "Indian", "South Indian", "Pakistani", "Bangladeshi", "Sri Lankan", "Nepali", "Afghan"},
            {"East Asia", "Chinese", "Cantonese", "Sichuan", "Taiwanese", "Japanese", "Korean", "Mongolian"},
            {"Southeast Asia", "Thai", "Vietnamese", "Indonesian", "Malaysian", "Singaporean", "Filipino", "Burmese", "Cambodian", "Lao"},
            {"Central Asia & Caucasus", "Uzbek", "Kazakh", "Georgian", "Armenian", "Azerbaijani"},
            {"Oceania", "Australian", "New Zealand", "Fijian", "Samoan"},
            {"International", "International", "Fusion"}};

    /** Lumped labels the recipe checks refuse (tag by country instead). */
    public static final List<String> LUMPS = Collections.unmodifiableList(Arrays.asList(
            "Caribbean", "Asian", "African", "Latin American", "Middle Eastern", "South American", "European", "Eastern European",
            "Scandinavian", "Mediterranean", "North African", "Central American"));

    private static final Map<String, String> GROUP_OF = new HashMap<String, String>();
    static {
        for (String[] row : DATA) for (int i = 1; i < row.length; i++) GROUP_OF.put(row[i].toLowerCase(Locale.ROOT), row[0]);
        GROUP_OF.put("trinidadian", "Caribbean");   // older spelling in saved/online recipes
        GROUP_OF.put("argentinian", "Latin America");
    }

    /** The world region for a cuisine label (TheMealDB areas included); unknown labels go to "International". */
    public static String group(String cuisine) {
        if (cuisine == null) return "International";
        String g = GROUP_OF.get(cuisine.trim().toLowerCase(Locale.ROOT));
        if (g != null) return g;
        return "International";
    }

    public static boolean known(String cuisine) { return cuisine != null && GROUP_OF.containsKey(cuisine.trim().toLowerCase(Locale.ROOT)); }

    /** Cuisine labels in a region (data order). */
    public static List<String> inGroup(String group) {
        for (String[] row : DATA) if (row[0].equals(group)) return Collections.unmodifiableList(Arrays.asList(row).subList(1, row.length));
        return Collections.emptyList();
    }

    /** Groups cuisine facet names (Discover.facet order kept inside each group) by region, in GROUPS order. */
    public static LinkedHashMap<String, List<String>> grouped(java.util.Collection<String> names) {
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        for (String g : GROUPS) out.put(g, new ArrayList<String>());
        for (String n : names) out.get(group(n)).add(n);
        for (String g : new ArrayList<String>(out.keySet())) if (out.get(g).isEmpty()) out.remove(g);
        return out;
    }
}
