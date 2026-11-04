package org.pantryrecipes.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pantryrecipes.app.core.DietClassifier;
import org.pantryrecipes.app.core.Ingredient;
import org.pantryrecipes.app.core.Quantity;
import org.pantryrecipes.app.core.MealType;
import org.pantryrecipes.app.core.Recipe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/** Client for TheMealDB's free JSON API (https://www.themealdb.com/api.php). */
final class MealDb {
    private MealDb() { }

    static final String BASE = "https://www.themealdb.com/api/json/v1/";

    static String get(String key, String path) throws IOException {
        URL url = new URL(BASE + (key == null || key.isEmpty() ? "1" : key) + "/" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(12000);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "PantryRecipes/1.0 (Android)");
        try {
            int code = con.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            InputStream in = con.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            return bos.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    /** filter.php?i= : ids of meals containing an ingredient (empty list when none). */
    static List<String> idsForIngredient(String key, String ingredient) throws Exception {
        String json = get(key, "filter.php?i=" + enc(ingredient.trim().toLowerCase().replace(' ', '_')));
        List<String> ids = new ArrayList<String>();
        JSONObject o = new JSONObject(json);
        JSONArray meals = o.optJSONArray("meals");
        if (meals == null) return ids;
        for (int i = 0; i < meals.length(); i++) ids.add(meals.getJSONObject(i).optString("idMeal"));
        return ids;
    }

    /** filter.php?a= (area/cuisine) or ?c= (category): meal ids, in TheMealDB's order. */
    static List<String> filterIds(String key, char kind, String value) throws Exception {
        String json = get(key, "filter.php?" + kind + "=" + enc(value.trim().replace(' ', '_')));
        List<String> ids = new ArrayList<String>();
        JSONArray meals = new JSONObject(json).optJSONArray("meals");
        if (meals == null) return ids;
        for (int i = 0; i < meals.length(); i++) ids.add(meals.getJSONObject(i).optString("idMeal"));
        return ids;
    }

    /** list.php?a=list (cuisines) or categories.php (categories). */
    static List<String> listNames(String key, boolean areas) throws Exception {
        String json = get(key, areas ? "list.php?a=list" : "categories.php");
        JSONObject o = new JSONObject(json);
        JSONArray arr = o.optJSONArray(areas ? "meals" : "categories");
        List<String> out = new ArrayList<String>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            String n = arr.getJSONObject(i).optString(areas ? "strArea" : "strCategory", "").trim();
            if (!n.isEmpty() && !n.equalsIgnoreCase("unknown") && !out.contains(n)) out.add(n);
        }
        return out;
    }

    /** random.php: one random meal. */
    static Recipe random(String key) throws Exception {
        JSONArray meals = new JSONObject(get(key, "random.php")).optJSONArray("meals");
        if (meals == null || meals.length() == 0) return null;
        return convert(meals.getJSONObject(0));
    }

    static Recipe lookup(String key, String id) throws Exception {
        JSONObject o = new JSONObject(get(key, "lookup.php?i=" + enc(id)));
        JSONArray meals = o.optJSONArray("meals");
        if (meals == null || meals.length() == 0) return null;
        return convert(meals.getJSONObject(0));
    }

    static List<Recipe> search(String key, String query) throws Exception {
        JSONObject o = new JSONObject(get(key, "search.php?s=" + enc(query.trim())));
        JSONArray meals = o.optJSONArray("meals");
        List<Recipe> out = new ArrayList<Recipe>();
        if (meals == null) return out;
        for (int i = 0; i < meals.length(); i++) {
            Recipe r = convert(meals.getJSONObject(i));
            if (r != null) out.add(r);
        }
        return out;
    }

    private static String s(JSONObject m, String k) {
        if (m.isNull(k)) return "";
        return m.optString(k, "").trim();
    }

    static Recipe convert(JSONObject m) {
        Recipe r = new Recipe();
        String id = s(m, "idMeal");
        if (id.isEmpty()) return null;
        r.id = "m:" + id;
        r.title = s(m, "strMeal");
        r.category = s(m, "strCategory");
        r.cuisine = s(m, "strArea");
        if (r.cuisine.isEmpty() || r.cuisine.equalsIgnoreCase("Unknown")) r.cuisine = s(m, "strCountry");
        r.image = s(m, "strMealThumb");
        r.sourceUrl = s(m, "strSource");
        if (r.sourceUrl.isEmpty()) r.sourceUrl = s(m, "strYoutube");
        r.source = "TheMealDB";
        r.prepMin = -1;
        r.cookMin = -1;
        r.servings = 4;
        r.servingsKnown = false;
        for (int i = 1; i <= 20; i++) {
            String name = s(m, "strIngredient" + i);
            if (name.isEmpty()) continue;
            String measure = s(m, "strMeasure" + i);
            Quantity.Parsed p = Quantity.parse(measure);
            r.ingredients.add(new Ingredient(name, p.amount, p.unit, false));
        }
        r.steps.addAll(splitSteps(s(m, "strInstructions")));
        r.tags.addAll(DietClassifier.classify(r.ingredients));
        r.tagsInferred = true;
        r.meals.addAll(MealType.infer(r.category, r.title));
        return r;
    }

    /** Splits TheMealDB's instruction blob into readable steps. */
    static List<String> splitSteps(String text) {
        List<String> out = new ArrayList<String>();
        for (String line : text.split("\\r?\\n")) {
            String t = line.trim().replaceFirst("^(?i)(step\\s*\\d+[:.)]?|\\d+[.)]|[-\\u2022*])\\s*", "").trim();
            if (t.length() < 3) continue;
            if (t.matches("(?i)step\\s*\\d+")) continue;
            if (t.length() > 450) {
                // very long paragraph: split into chunks of about two sentences
                String[] sentences = t.split("(?<=[.!?])\\s+");
                StringBuilder b = new StringBuilder();
                for (String sen : sentences) {
                    if (b.length() > 0 && b.length() + sen.length() > 280) {
                        out.add(b.toString().trim());
                        b.setLength(0);
                    }
                    b.append(sen).append(' ');
                }
                if (b.length() > 0) out.add(b.toString().trim());
            } else {
                out.add(t);
            }
        }
        return out;
    }
}
