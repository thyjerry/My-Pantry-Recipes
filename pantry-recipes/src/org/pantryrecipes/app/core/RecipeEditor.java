package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v2.0 own-recipe editor logic: ingredient lines, validation, "my version" copies, recipe packs (one JSON file),
 * plain-text sharing and the printable family cookbook. Shared with the web app (core20.js; the ingredient-line parser
 * matches import15.js).
 */
public final class RecipeEditor {
    private RecipeEditor() { }

    public static final int MAX_TITLE = 120, MAX_ING = 100, MAX_STEPS = 100, MAX_STEP = 2000, MAX_TAGS = 12, MAX_TAG = 30, MAX_NOTE = 2000;
    public static final String[] CATEGORIES = {"Main", "Breakfast", "Soup", "Salad", "Side", "Snack", "Dessert", "Baking", "Drink", "Sauce"};

    static final String[] UNITS = {"fl oz", "fluid ounces", "fluid ounce", "g", "gram", "grams", "gr", "kg", "kilo", "kilos", "kilogram", "kilograms", "mg", "ml", "millilitre", "millilitres", "milliliter", "milliliters",
            "l", "litre", "litres", "liter", "liters", "oz", "ounce", "ounces", "lb", "lbs", "pound", "pounds", "cup", "cups", "c", "tbsp", "tbs", "tbl", "tablespoon", "tablespoons",
            "tsp", "teaspoon", "teaspoons", "pinch", "pinches", "dash", "dashes", "clove", "cloves", "can", "cans", "tin", "tins", "jar", "jars", "packet", "packets", "pack", "packs",
            "bunch", "bunches", "slice", "slices", "stick", "sticks", "sprig", "sprigs", "handful", "handfuls", "piece", "pieces", "head", "heads", "pint", "pints", "qt", "quart", "quarts",
            "large", "medium", "small"};
    private static final String NUM = "(?:\\d+/\\d+(?![\\d/])|\\d+(?:[.,]\\d+)?(?:\\s+\\d+/\\d+)?(?:\\s*[\\u00bc-\\u00be\\u2153\\u2154])?|\\d+/\\d+|[\\u00bc-\\u00be\\u2153\\u2154])";
    private static final Pattern LEAD = Pattern.compile("^(" + NUM + ")(?:\\s*(?:-|\\u2013|to)\\s*" + NUM + ")?\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPT = Pattern.compile("\\(optional\\)|,\\s*optional\\b|\\boptional:?\\s*$|^optional:?\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAR = Pattern.compile("^(\\([^)]*\\))\\s*(.+)$");

    /** Parses one ingredient line ("1 1/2 cups milk", "2 eggs", "salt, optional"), or null for a blank line. */
    public static Ingredient ingredientLine(String line0) {
        if (line0 == null) return null;
        String line = line0.replaceAll("\\s+", " ").trim().replaceAll("^[-*\\u2022\\u25aa\\u25cf\\u2013\\u2014]+\\s*", "").replaceAll("^\\d+[.)]\\s+", "");
        if (line.isEmpty()) return null;
        boolean optional = false;
        if (OPT.matcher(line).find()) {
            optional = true;
            line = line.replaceFirst("(?i)\\s*\\(optional\\)\\s*", " ").replaceFirst("(?i),\\s*optional\\b", "").replaceFirst("(?i)\\boptional:?\\s*$", "")
                    .replaceFirst("(?i)^optional:?\\s+", "").trim();
        }
        double amount = Double.NaN;
        String unit = "", name = line;
        Matcher m = LEAD.matcher(line);
        if (m.find()) {
            String num = m.group(1).replace(',', '.'), rest = line.substring(m.end()), u = "";
            String lower = rest.toLowerCase(Locale.ROOT);
            for (String w : UNITS) {
                if (lower.startsWith(w)) {
                    char after = lower.length() > w.length() ? lower.charAt(w.length()) : 0;
                    if (after == 0 || after == ' ' || after == '.' || after == ',' || after == ')') {
                        if (after == 0 && (w.equals("large") || w.equals("medium") || w.equals("small") || w.equals("c"))) continue;
                        u = rest.substring(0, w.length());
                        rest = rest.substring(w.length()).replaceFirst("^\\.?\\s*", "").replaceFirst("(?i)^of\\s+", "");
                        break;
                    }
                }
            }
            if (u.equals("c") || u.equals("C")) u = "cup";
            Quantity.Parsed q = Quantity.parse((num + " " + u).trim());
            if (!Double.isNaN(q.amount) && !rest.trim().isEmpty()) { amount = q.amount; unit = q.unit; name = rest.trim(); }
        }
        name = name.replaceFirst("^[,;:\\s]+", "");
        Matcher pm = PAR.matcher(name);
        if (pm.matches()) name = pm.group(2) + " " + pm.group(1);
        if (name.isEmpty()) return null;
        if (name.length() > 120) name = name.substring(0, 120);
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return new Ingredient(name, amount, unit, optional);
    }

    /** The editable text of an ingredient ("1 1/2 cups Milk", "Salt, optional"). */
    public static String lineOf(Ingredient i) {
        String q = Double.isNaN(i.amount) ? "" : Quantity.format(i.amount, i.unit, 1);
        String s = (q.isEmpty() ? "" : q + " ") + i.name;
        return i.optional ? s + ", optional" : s;
    }

    /** Fields the editor collects. */
    public static final class Draft {
        public String title = "", category = "Main", cuisine = "", ingredients = "", steps = "", tags = "", note = "", basedOn = "";
        public int prep = -1, cook = -1, servings = 4;
        public final List<String> meals = new ArrayList<String>();
    }

    /** Problems that stop saving ("" = none). */
    public static String problem(Draft d) {
        if (d.title == null || d.title.trim().isEmpty()) return "Give the recipe a name.";
        if (d.title.trim().length() > MAX_TITLE) return "The name is too long (120 characters at most).";
        if (parseIngredients(d.ingredients).isEmpty()) return "Add at least one ingredient, one per line.";
        if (d.servings < 1 || d.servings > 24) return "Servings must be between 1 and 24.";
        if (d.prep > 1440 || d.cook > 2880) return "That time looks too long.";
        return "";
    }

    public static List<Ingredient> parseIngredients(String text) {
        List<Ingredient> out = new ArrayList<Ingredient>();
        if (text == null) return out;
        for (String l : text.split("\n")) {
            Ingredient i = ingredientLine(l);
            if (i != null && out.size() < MAX_ING) out.add(i);
        }
        return out;
    }

    public static List<String> parseSteps(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) return out;
        for (String l : text.split("\n")) {
            String s = l.trim().replaceFirst("(?i)^(?:step\\s*)?\\d+[.):]\\s*", "").trim();
            if (!s.isEmpty() && out.size() < MAX_STEPS) out.add(s.length() > MAX_STEP ? s.substring(0, MAX_STEP) : s);
        }
        return out;
    }

    public static List<String> parseTags(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) return out;
        for (String t : text.split(",")) {
            String s = t.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (s.length() > MAX_TAG) s = s.substring(0, MAX_TAG);
            if (!s.isEmpty() && !out.contains(s) && out.size() < MAX_TAGS) out.add(s);
        }
        return out;
    }

    /** Builds the stored recipe (id must start with "u:"); diet tags are worked out from the ingredients. */
    public static Recipe build(Draft d, String id, Map<String, Nutrition.Food> foods) {
        Recipe r = new Recipe();
        r.id = id;
        r.title = d.title.trim().replaceAll("\\s+", " ");
        if (r.title.length() > MAX_TITLE) r.title = r.title.substring(0, MAX_TITLE);
        r.category = d.category == null || d.category.trim().isEmpty() ? "Main" : d.category.trim();
        r.cuisine = d.cuisine == null ? "" : d.cuisine.trim();
        if (r.cuisine.length() > 60) r.cuisine = r.cuisine.substring(0, 60);
        r.prepMin = d.prep < 0 ? -1 : Math.min(d.prep, 1440);
        r.cookMin = d.cook < 0 ? -1 : Math.min(d.cook, 2880);
        r.servings = Math.max(1, Math.min(24, d.servings));
        for (String m : d.meals) if (MealType.isKnown(m)) r.meals.add(m);
        r.ingredients.addAll(parseIngredients(d.ingredients));
        r.steps.addAll(parseSteps(d.steps));
        r.userTags.addAll(parseTags(d.tags));
        r.note = d.note == null ? "" : d.note.trim();
        if (r.note.length() > MAX_NOTE) r.note = r.note.substring(0, MAX_NOTE);
        r.basedOn = d.basedOn == null ? "" : d.basedOn;
        r.source = "user";
        r.tags.addAll(DietClassifier.classify(r.ingredients));
        r.tagsInferred = true;
        r.diets.addAll(Diets.classify(r, foods));
        r.dietsKnown = true;
        return r;
    }

    /** The editor fields for an existing recipe (to edit it, or to start a "my version" copy). */
    public static Draft draftOf(Recipe r) {
        Draft d = new Draft();
        d.title = r.title; d.category = r.category; d.cuisine = r.cuisine; d.prep = r.prepMin; d.cook = r.cookMin; d.servings = r.servings;
        d.meals.addAll(r.meals);
        StringBuilder b = new StringBuilder();
        for (Ingredient i : r.ingredients) b.append(lineOf(i)).append('\n');
        d.ingredients = b.toString().trim();
        b.setLength(0);
        for (String s : r.steps) b.append(s).append('\n');
        d.steps = b.toString().trim();
        d.tags = join(new ArrayList<String>(r.userTags), ", ");
        d.note = r.note;
        d.basedOn = r.basedOn;
        return d;
    }

    /** Starts a "my version" copy of any recipe: same fields, " (my version)" title, basedOn = the original id. */
    public static Draft myVersion(Recipe r) {
        Draft d = draftOf(r);
        if (!r.id.startsWith("u:")) {
            d.title = (r.title + " (my version)");
            if (d.title.length() > MAX_TITLE) d.title = d.title.substring(0, MAX_TITLE);
            d.basedOn = r.id;
        }
        return d;
    }

    static String join(List<String> l, String sep) {
        StringBuilder b = new StringBuilder();
        for (String s : l) { if (b.length() > 0) b.append(sep); b.append(s); }
        return b.toString();
    }

    // ---------------- packs ----------------

    public static final String PACK_KIND = "recipe-pack";
    public static final int PACK_FORMAT = 1, MAX_PACK = 500;

    public static String packWrite(String name, List<Recipe> recipes, String appVersion, String today) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("app", "My Pantry Recipes");
        m.put("kind", PACK_KIND);
        m.put("format", (long) PACK_FORMAT);
        m.put("appVersion", appVersion);
        m.put("exported", today);
        m.put("name", name == null ? "" : name);
        List<Object> arr = new ArrayList<Object>();
        for (Recipe r : recipes) if (r.id.startsWith("u:")) arr.add(r.toMap());
        m.put("recipes", arr);
        return Json.write(m);
    }

    /** Thrown for files that are not recipe packs (message is user-facing). */
    public static final class PackException extends Exception {
        public PackException(String m) { super(m); }
    }

    /**
     * Reads a pack (or a backup's recipes): only own recipes ("u:" ids), each checked like a backup. Recipes get fresh
     * ids when they clash with an existing different recipe. Never more than MAX_PACK.
     */
    @SuppressWarnings("unchecked")
    public static List<Recipe> packParse(String text, java.util.Set<String> existingIds, Map<String, Nutrition.Food> foods, long seed) throws PackException {
        Object o;
        try { o = Json.parse(text); } catch (Exception e) { throw new PackException("This file is not a recipe pack."); }
        if (!(o instanceof Map)) throw new PackException("This file is not a recipe pack.");
        Map<String, Object> m = (Map<String, Object>) o;
        Object kind = m.get("kind");
        if (!PACK_KIND.equals(kind) && !"Pantry Recipes".equals(m.get("app")) && !"My Pantry Recipes".equals(m.get("app"))) throw new PackException("This file is not a recipe pack.");
        Object f = m.get("format");
        if (PACK_KIND.equals(kind) && (!(f instanceof Number) || ((Number) f).intValue() > PACK_FORMAT)) throw new PackException("This recipe pack is from a newer version of the app.");
        Object arr = m.get("recipes");
        if (!(arr instanceof List)) throw new PackException("This recipe pack has no recipes.");
        List<Recipe> out = new ArrayList<Recipe>();
        java.util.Set<String> used = new java.util.HashSet<String>(existingIds);
        long n = seed;
        for (Object x : (List<Object>) arr) {
            if (out.size() >= MAX_PACK) break;
            if (!(x instanceof Map)) continue;
            Recipe r;
            try { r = Recipe.fromMap((Map<String, Object>) x); } catch (Exception e) { continue; }
            if (!r.id.startsWith("u:") || r.id.length() > 80 || r.title.trim().isEmpty() || r.ingredients.isEmpty()) continue;
            if (r.title.length() > MAX_TITLE) r.title = r.title.substring(0, MAX_TITLE);
            while (r.ingredients.size() > MAX_ING) r.ingredients.remove(r.ingredients.size() - 1);
            while (r.steps.size() > MAX_STEPS) r.steps.remove(r.steps.size() - 1);
            if (used.contains(r.id)) {
                String id;
                do { id = "u:" + Long.toString(n++, 36); } while (used.contains(id));
                Map<String, Object> rm = r.toMap();
                rm.put("id", id);
                r = Recipe.fromMap(rm);
            }
            r.tags.clear();
            r.tags.addAll(DietClassifier.classify(r.ingredients));
            r.diets.clear();
            r.diets.addAll(Diets.classify(r, foods));
            r.dietsKnown = true;
            used.add(r.id);
            out.add(r);
        }
        if (out.isEmpty()) throw new PackException("No recipes could be read from this pack.");
        return out;
    }

    // ---------------- sharing ----------------

    /** Plain text for the share sheet. */
    public static String shareText(Recipe r, int servings) {
        StringBuilder b = new StringBuilder();
        b.append(r.title).append('\n');
        int t = r.totalMinutes();
        b.append(servings).append(servings == 1 ? " serving" : " servings");
        if (t > 0) b.append(" \u00b7 ").append(t).append(" min");
        b.append("\n\n");
        if (!r.note.isEmpty()) b.append(r.note).append("\n\n");
        b.append("Ingredients\n");
        double scale = (double) servings / Math.max(1, r.servings);
        for (Ingredient i : r.ingredients) {
            String q = i.quantityText(scale);
            b.append("- ").append(q.isEmpty() ? "" : q + " ").append(i.name).append(i.optional ? " (optional)" : "").append('\n');
        }
        if (!r.steps.isEmpty()) {
            b.append("\nSteps\n");
            for (int k = 0; k < r.steps.size(); k++) b.append(k + 1).append(". ").append(r.steps.get(k)).append('\n');
        }
        b.append("\nFrom My Pantry Recipes");
        return b.toString();
    }

    static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                case '\'': b.append("&#39;"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    /** A self-contained printable HTML cookbook (print it to PDF from a browser). No scripts, no remote content. */
    public static String cookbookHtml(String title, List<Recipe> recipes, Map<String, Backup.Rating> ratings, String today) {
        String t = title == null || title.trim().isEmpty() ? "Our Family Cookbook" : title.trim();
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        b.append("<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; style-src 'unsafe-inline'\">\n");
        b.append("<title>").append(esc(t)).append("</title>\n<style>\n")
                .append("body{font:16px/1.5 Georgia,serif;color:#222;max-width:46rem;margin:0 auto;padding:1.5rem}")
                .append("h1{text-align:center;font-size:2.2rem;margin:3rem 0 .5rem}.sub{text-align:center;color:#555}")
                .append(".toc{margin:2rem 0}.toc li{margin:.15rem 0}")
                .append("section{page-break-before:always;break-before:page;padding-top:1rem}")
                .append("h2{font-size:1.6rem;margin:0 0 .25rem}.meta{color:#555;font-size:.9rem}.note{font-style:italic}")
                .append("ul,ol{padding-left:1.4rem}li{margin:.2rem 0}h3{font-size:1.05rem;margin:1rem 0 .3rem;text-transform:uppercase;letter-spacing:.05em}")
                .append("footer{margin-top:3rem;color:#777;font-size:.8rem;text-align:center}@media print{body{padding:0}a{color:inherit}}\n</style></head><body>\n");
        b.append("<h1>").append(esc(t)).append("</h1>\n<p class=\"sub\">").append(recipes.size()).append(recipes.size() == 1 ? " recipe" : " recipes")
                .append(" \u00b7 ").append(esc(today)).append("</p>\n");
        b.append("<ol class=\"toc\">\n");
        for (int k = 0; k < recipes.size(); k++) b.append("<li><a href=\"#r").append(k + 1).append("\">").append(esc(recipes.get(k).title)).append("</a></li>\n");
        b.append("</ol>\n");
        for (int k = 0; k < recipes.size(); k++) {
            Recipe r = recipes.get(k);
            b.append("<section id=\"r").append(k + 1).append("\">\n<h2>").append(esc(r.title)).append("</h2>\n<p class=\"meta\">");
            List<String> meta = new ArrayList<String>();
            meta.add(r.servings + (r.servings == 1 ? " serving" : " servings"));
            if (r.prepMin > 0) meta.add("prep " + r.prepMin + " min");
            if (r.cookMin > 0) meta.add("cook " + r.cookMin + " min");
            if (!r.cuisine.isEmpty()) meta.add(r.cuisine);
            Backup.Rating rt = ratings == null ? null : ratings.get(r.id);
            if (rt != null && rt.stars > 0) {
                StringBuilder st = new StringBuilder();
                for (int s = 0; s < 5; s++) st.append(s < rt.stars ? '\u2605' : '\u2606');
                meta.add(st.toString());
            }
            b.append(esc(join(meta, " \u00b7 "))).append("</p>\n");
            if (!r.note.isEmpty()) b.append("<p class=\"note\">").append(esc(r.note)).append("</p>\n");
            b.append("<h3>Ingredients</h3>\n<ul>\n");
            for (Ingredient i : r.ingredients) {
                String q = i.quantityText(1);
                b.append("<li>").append(esc((q.isEmpty() ? "" : q + " ") + i.name + (i.optional ? " (optional)" : ""))).append("</li>\n");
            }
            b.append("</ul>\n");
            if (!r.steps.isEmpty()) {
                b.append("<h3>Steps</h3>\n<ol>\n");
                for (String s : r.steps) b.append("<li>").append(esc(s)).append("</li>\n");
                b.append("</ol>\n");
            }
            if (rt != null && !rt.note.isEmpty()) b.append("<p class=\"note\">Notes: ").append(esc(rt.note)).append("</p>\n");
            b.append("</section>\n");
        }
        b.append("<footer>Made with My Pantry Recipes</footer>\n</body></html>\n");
        return b.toString();
    }
}
