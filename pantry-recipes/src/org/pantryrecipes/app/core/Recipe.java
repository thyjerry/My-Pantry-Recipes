package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A recipe from the bundled set or from TheMealDB (cached). */
public final class Recipe {
    public static final String VEGETARIAN = "vegetarian";
    public static final String VEGAN = "vegan";
    public static final String GLUTEN_FREE = "gluten-free";
    public static final String DAIRY_FREE = "dairy-free";

    public String id = "";
    public String title = "";
    public String category = "";
    public String cuisine = "";
    public int prepMin = -1;
    public int cookMin = -1;
    public int servings = 4;
    public boolean servingsKnown = true;
    public final Set<String> tags = new LinkedHashSet<String>();
    public boolean tagsInferred;
    /** Meal types (see {@link MealType}), display order. */
    public final Set<String> meals = new LinkedHashSet<String>();
    /** v2.0: the extra diet ids from Diets (pescatarian, keto, ...); the original four stay in tags. */
    public final Set<String> diets = new LinkedHashSet<String>();
    /** v2.0: diets were worked out already (an empty list means "none fit"), so the app never re-classifies. */
    public boolean dietsKnown;
    /** v2.0: for "my version" copies, the id of the recipe it was copied from; empty otherwise. */
    public String basedOn = "";
    /** v2.0: free-form user tags and a short intro note on the user's own recipes. */
    public final Set<String> userTags = new LinkedHashSet<String>();
    public String note = "";
    public final List<Ingredient> ingredients = new ArrayList<Ingredient>();
    public final List<String> steps = new ArrayList<String>();
    public String source = "bundled";
    public String image = "";
    public String sourceUrl = "";

    public boolean isOnline() { return id.startsWith("m:"); }

    // v1.8: lower-cased search text, built once per recipe instead of on every keystroke.
    private String lcHead, lcIngredients;

    /** Title, cuisine and category, lower case, separated by \u0001 (so a query never spans two fields). */
    public String lowerHead() {
        String s = lcHead;
        if (s == null) lcHead = s = (title + "\u0001" + cuisine + "\u0001" + category).toLowerCase(java.util.Locale.US);
        return s;
    }

    /** All ingredient names, lower case, separated by \u0001. */
    public String lowerIngredients() {
        String s = lcIngredients;
        if (s == null) {
            StringBuilder b = new StringBuilder();
            for (Ingredient i : ingredients) b.append(i.name.toLowerCase(java.util.Locale.US)).append('\u0001');
            lcIngredients = s = b.toString();
        }
        return s;
    }

    /** The Recipes search: title, cuisine, category or an ingredient name contains q (lower case), or an ingredient key equals qk. */
    public boolean searchHit(String q, String qk) {
        if (lowerHead().contains(q) || lowerIngredients().contains(q)) return true;
        if (qk != null && !qk.isEmpty()) for (Ingredient i : ingredients) if (i.key.equals(qk)) return true;
        return false;
    }

    /** Total minutes, or -1 when unknown. */
    public int totalMinutes() {
        if (prepMin < 0 && cookMin < 0) return -1;
        return Math.max(0, prepMin) + Math.max(0, cookMin);
    }

    @SuppressWarnings("unchecked")
    public static Recipe fromMap(Map<String, Object> m) {
        Recipe r = new Recipe();
        r.id = Json.str(m, "id", "");
        r.title = Json.str(m, "title", "");
        r.category = Json.str(m, "category", "");
        r.cuisine = Json.str(m, "cuisine", "");
        r.prepMin = Json.integer(m, "prep", -1);
        r.cookMin = Json.integer(m, "cook", -1);
        r.servings = Json.integer(m, "servings", 4);
        r.servingsKnown = Json.bool(m, "servingsKnown", true);
        r.tagsInferred = Json.bool(m, "tagsInferred", false);
        r.source = Json.str(m, "source", "bundled");
        r.image = Json.str(m, "image", "");
        r.sourceUrl = Json.str(m, "sourceUrl", "");
        Object tags = m.get("tags");
        if (tags instanceof List) for (Object t : (List<Object>) tags) r.tags.add(String.valueOf(t));
        Object dl = m.get("diets");
        if (dl instanceof List) r.dietsKnown = true;
        if (dl instanceof List) for (Object t : (List<Object>) dl) if (Diets.IDS.contains(String.valueOf(t))) r.diets.add(String.valueOf(t));
        r.basedOn = Json.str(m, "basedOn", "");
        Object ut = m.get("userTags");
        if (ut instanceof List) for (Object t : (List<Object>) ut) { String x = String.valueOf(t).trim(); if (!x.isEmpty() && x.length() <= 30 && r.userTags.size() < 12) r.userTags.add(x); }
        r.note = Json.str(m, "note", "");
        if (r.note.length() > 2000) r.note = r.note.substring(0, 2000);
        Object ml = m.get("meals");
        if (ml instanceof List) for (Object t : (List<Object>) ml) {
            String c = MealType.canonical(String.valueOf(t));
            if (!c.isEmpty()) r.meals.add(c);
        }
        if (r.meals.isEmpty()) r.meals.addAll(MealType.infer(r.category, r.title)); // older saved recipes
        else { List<String> o = MealType.sorted(r.meals); r.meals.clear(); r.meals.addAll(o); }
        Object ing = m.get("ingredients");
        if (ing instanceof List) {
            for (Object o : (List<Object>) ing) {
                Map<String, Object> im = (Map<String, Object>) o;
                r.ingredients.add(new Ingredient(Json.str(im, "name", ""), Json.dbl(im, "qty", Double.NaN),
                        Json.str(im, "unit", ""), Json.bool(im, "optional", false)));
            }
        }
        Object st = m.get("steps");
        if (st instanceof List) for (Object s : (List<Object>) st) r.steps.add(String.valueOf(s));
        return r;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("title", title);
        m.put("category", category);
        m.put("cuisine", cuisine);
        m.put("prep", prepMin);
        m.put("cook", cookMin);
        m.put("servings", servings);
        if (!servingsKnown) m.put("servingsKnown", false);
        m.put("tags", new ArrayList<Object>(tags));
        if (tagsInferred) m.put("tagsInferred", true);
        if (dietsKnown || !diets.isEmpty()) m.put("diets", new ArrayList<Object>(diets));
        m.put("meals", new ArrayList<Object>(meals));
        List<Object> ing = new ArrayList<Object>();
        for (Ingredient i : ingredients) {
            Map<String, Object> im = new LinkedHashMap<String, Object>();
            im.put("name", i.name);
            if (!Double.isNaN(i.amount)) im.put("qty", i.amount);
            if (!i.unit.isEmpty()) im.put("unit", i.unit);
            if (i.optional) im.put("optional", true);
            ing.add(im);
        }
        m.put("ingredients", ing);
        m.put("steps", new ArrayList<Object>(steps));
        m.put("source", source);
        if (!image.isEmpty()) m.put("image", image);
        if (!sourceUrl.isEmpty()) m.put("sourceUrl", sourceUrl);
        if (!basedOn.isEmpty()) m.put("basedOn", basedOn);
        if (!userTags.isEmpty()) m.put("userTags", new ArrayList<Object>(userTags));
        if (!note.isEmpty()) m.put("note", note);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static List<Recipe> parseList(String json) {
        Object root = Json.parse(json);
        List<Object> arr = root instanceof Map ? (List<Object>) ((Map<String, Object>) root).get("recipes") : (List<Object>) root;
        List<Recipe> out = new ArrayList<Recipe>();
        for (Object o : arr) out.add(fromMap((Map<String, Object>) o));
        return out;
    }
}
