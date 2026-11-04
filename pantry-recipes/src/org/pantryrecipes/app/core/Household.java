package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v2.0: household profiles for allergen alerts ("Sam: sesame, mustard"). Recipes are flagged with whose allergy a
 * group is; the app's own "avoid" list still hides recipes. Names stay on the device (backup and sync only).
 */
public final class Household {
    private Household() { }

    public static final int MAX = 12, MAX_NAME = 30;

    public static final class Person {
        public String name = "";
        public final Set<String> allergens = new LinkedHashSet<String>();
        public boolean hide;   // also hide recipes with these groups (like the main avoid list)
    }

    @SuppressWarnings("unchecked")
    public static List<Person> parse(Object o) {
        List<Person> out = new ArrayList<Person>();
        if (!(o instanceof List)) return out;
        for (Object x : (List<Object>) o) {
            if (!(x instanceof Map) || out.size() >= MAX) continue;
            Map<String, Object> m = (Map<String, Object>) x;
            Person p = new Person();
            p.name = Json.str(m, "name", "").trim().replaceAll("[<>\\s]+", " ").trim();
            if (p.name.length() > MAX_NAME) p.name = p.name.substring(0, MAX_NAME);
            if (p.name.isEmpty()) continue;
            Object al = m.get("allergens");
            if (al instanceof List) for (String id : Allergens.ALL_IDS) if (((List<Object>) al).contains(id)) p.allergens.add(id);
            p.hide = Boolean.TRUE.equals(m.get("hide"));
            out.add(p);
        }
        return out;
    }

    public static List<Object> toList(List<Person> ps) {
        List<Object> out = new ArrayList<Object>();
        for (Person p : ps) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("name", p.name);
            m.put("allergens", new ArrayList<Object>(p.allergens));
            if (p.hide) m.put("hide", true);
            out.add(m);
        }
        return out;
    }

    /** Groups to hide: the main avoid list plus every person marked "hide". */
    public static Set<String> avoid(java.util.Collection<String> own, List<Person> ps) {
        Set<String> s = new LinkedHashSet<String>(own);
        for (Person p : ps) if (p.hide) s.addAll(p.allergens);
        return s;
    }

    /** Alert lines for a recipe: "Mustard: Sam, Alex" for each group someone in the household is allergic to, ALL_IDS order. */
    public static List<String> alerts(Recipe r, List<Person> ps, String region) {
        List<String> out = new ArrayList<String>();
        if (ps.isEmpty()) return out;
        List<String> flagged = Allergens.flags(r, Allergens.ALL_IDS);
        for (String id : flagged) {
            StringBuilder who = new StringBuilder();
            for (Person p : ps) if (p.allergens.contains(id)) { if (who.length() > 0) who.append(", "); who.append(p.name); }
            if (who.length() > 0) out.add(Region.allergenLabel(id, region) + ": " + who);
        }
        return out;
    }
}
