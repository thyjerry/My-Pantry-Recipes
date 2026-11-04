package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Meal types ("courses"): a recipe has one or more. Bundled recipes carry an explicit, reviewed list; recipes from
 * TheMealDB get one inferred from their category (and a few title words).
 */
public final class MealType {
    private MealType() { }

    public static final String BREAKFAST = "Breakfast", BRUNCH = "Brunch", LUNCH = "Lunch", DINNER = "Dinner",
            MAIN = "Main dish", SIDE = "Side dish", APPETIZER = "Appetizer", SNACK = "Snack", SOUP = "Soup",
            SALAD = "Salad", DESSERT = "Dessert", BAKED = "Baked goods", DRINK = "Drink", SAUCE = "Sauce/Condiment";

    /** Every allowed value, in display order. */
    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(BREAKFAST, BRUNCH, LUNCH, DINNER,
            MAIN, SIDE, APPETIZER, SNACK, SOUP, SALAD, DESSERT, BAKED, DRINK, SAUCE));
    /** "When" types; picking one of these does not override the Include desserts / Include sides switches. */
    public static final List<String> TIMES = Collections.unmodifiableList(Arrays.asList(BREAKFAST, BRUNCH, LUNCH, DINNER));

    public static boolean isKnown(String s) { return ALL.contains(s); }

    /** Canonical spelling of a meal type (case/space-insensitive, a few aliases), or "" if unknown. */
    public static String canonical(String s) {
        if (s == null) return "";
        String k = s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        for (String m : ALL) if (m.toLowerCase(Locale.ROOT).equals(k)) return m;
        switch (k) {
            case "main": case "mains": case "main course": case "entree": return MAIN;
            case "side": case "sides": return SIDE;
            case "starter": case "starters": case "appetiser": return APPETIZER;
            case "sauce": case "condiment": case "dressing": return SAUCE;
            case "desserts": return DESSERT;
            case "baking": case "bakery": return BAKED;
            case "drinks": case "beverage": return DRINK;
            default: return "";
        }
    }

    /** Values sorted into display order, unknown ones dropped, duplicates removed. */
    public static List<String> sorted(Collection<String> in) {
        List<String> out = new ArrayList<String>();
        for (String m : ALL) if (in.contains(m)) out.add(m);
        return out;
    }

    private static boolean has(String hay, String... words) {
        for (String w : words) if ((" " + hay + " ").contains(" " + w)) return true;
        return false;
    }

    /** Order for the tonight's-pick card: Dinner and Main dish first, then the rest in the usual order. */
    public static List<String> heroOrder(Collection<String> meals) {
        List<String> s = sorted(meals);
        List<String> out = new java.util.ArrayList<String>();
        if (s.contains(DINNER)) out.add(DINNER);
        if (s.contains(MAIN)) out.add(MAIN);
        for (String m : s) if (!out.contains(m)) out.add(m);
        return out;
    }

    /**
     * Meal types for a recipe that has none listed: from a TheMealDB category (Breakfast, Dessert, Starter, Side, Beef,
     * Chicken, Pasta, Seafood, Vegetarian, ...) or a bundled category (Main, Soup, Salad, Side, Snack, Baking, Drink,
     * Sauce), refined by a few title words. Unknown categories default to Main dish + Dinner.
     */
    public static List<String> infer(String category, String title) {
        String c = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        Set<String> out = new LinkedHashSet<String>();
        switch (c) {
            case "breakfast": out.add(BREAKFAST); out.add(BRUNCH); break;
            case "dessert": case "desserts": out.add(DESSERT); break;
            case "starter": case "starters": case "appetizer": out.add(APPETIZER); break;
            case "side": case "sides": case "side dish": out.add(SIDE); out.add(DINNER); break;
            case "soup": out.add(SOUP); out.add(LUNCH); out.add(DINNER); break;
            case "salad": out.add(SALAD); out.add(LUNCH); break;
            case "snack": out.add(SNACK); break;
            case "baking": out.add(BAKED); break;
            case "drink": case "drinks": out.add(DRINK); break;
            case "sauce": out.add(SAUCE); break;
            default: out.add(MAIN); out.add(DINNER); // Beef, Chicken, Lamb, Pork, Goat, Seafood, Pasta, Vegetarian, Vegan, Miscellaneous, Main, ""
        }
        if (has(t, "soup", "broth", "chowder", "bisque", "pho", "ramen", "gazpacho")) { out.add(SOUP); out.add(LUNCH); }
        if (has(t, "salad", "slaw")) { out.add(SALAD); out.add(LUNCH); }
        if (has(t, "sandwich", "burger", "wrap", "toastie", "panini", "quesadilla", "taco", "pizza", "baguette", "sub ")) out.add(LUNCH);
        if (out.contains(DESSERT) && has(t, "cake", "cookie", "biscuit", "brownie", "muffin", "scone", "bread", "tart", "pie", "loaf", "bun", "roll"))
            out.add(BAKED);
        if (out.contains(DESSERT) && has(t, "pancake", "crepe", "waffle")) { out.add(BREAKFAST); out.add(BRUNCH); }
        return sorted(out);
    }

    /** A recipe's meal types: its own list, or an inferred one when it has none (e.g. an old cached online recipe). */
    public static List<String> of(Recipe r) {
        if (!r.meals.isEmpty()) return sorted(r.meals);
        return infer(r.category, r.title);
    }

    /** Recipes that only make sense as a dessert / a side (not also a main dish). */
    public static boolean isDessert(Collection<String> meals) { return meals.contains(DESSERT) && !meals.contains(MAIN); }
    public static boolean isSide(Collection<String> meals) { return meals.contains(SIDE) && !meals.contains(MAIN); }

    /** Whether the user explicitly asked for this recipe's kind: a selected course type (not a time of day) it has. */
    static boolean explicit(Collection<String> meals, Collection<String> selected) {
        for (String s : selected) if (meals.contains(s) && !TIMES.contains(s)) return true;
        return false;
    }

    /**
     * The meal-type part of the list filters. {@code selected} empty = any type; otherwise the recipe needs at least one
     * selected type. Dessert-only and side-only recipes are left out unless their switch is on, or a matching course chip
     * (Dessert, Side dish, Salad, Snack, ...) is selected.
     */
    public static boolean passes(Collection<String> meals, Collection<String> selected, boolean incDesserts, boolean incSides) {
        if (!selected.isEmpty()) {
            boolean any = false;
            for (String s : selected) if (meals.contains(s)) { any = true; break; }
            if (!any) return false;
            if (explicit(meals, selected)) return true;
        }
        if (!incDesserts && isDessert(meals)) return false;
        if (!incSides && isSide(meals)) return false;
        return true;
    }

    /** Drinks and sauces are never suggested as something to cook for a meal (unless picked explicitly). */
    public static boolean isMeal(Collection<String> meals) {
        for (String m : meals) if (!m.equals(DRINK) && !m.equals(SAUCE)) return true;
        return false;
    }

    /**
     * Preferred pick for "What can I cook tonight?" and Surprise me: passes the filters, and with no meal type selected
     * it is dinner-ish (Dinner or Main dish); with a selection, anything matching it (drinks/sauces only if picked).
     */
    public static boolean preferred(Collection<String> meals, Collection<String> selected, boolean incDesserts, boolean incSides) {
        if (!passes(meals, selected, incDesserts, incSides)) return false;
        if (selected.isEmpty()) return meals.contains(DINNER) || meals.contains(MAIN);
        return true;
    }

    /** Fallback when nothing is preferred: passes the filters and is a meal (not only a drink or sauce). */
    public static boolean acceptable(Collection<String> meals, Collection<String> selected, boolean incDesserts, boolean incSides) {
        return passes(meals, selected, incDesserts, incSides) && (!selected.isEmpty() || isMeal(meals));
    }

    /** The TheMealDB category that holds more recipes of this meal type, or "" if none maps cleanly. */
    public static String mealDbCategory(String meal) {
        switch (meal) {
            case BREAKFAST: case BRUNCH: return "Breakfast";
            case DESSERT: return "Dessert";
            case APPETIZER: return "Starter";
            case SIDE: return "Side";
            default: return "";
        }
    }

    /** Short heading for the hero card. */
    public static String heroQuestion(Collection<String> selected) {
        if (selected.isEmpty()) return "WHAT CAN I COOK TONIGHT?";
        if (selected.size() == 1) {
            String s = selected.iterator().next();
            switch (s) {
                case BREAKFAST: return "WHAT CAN I COOK FOR BREAKFAST?";
                case BRUNCH: return "WHAT CAN I COOK FOR BRUNCH?";
                case LUNCH: return "WHAT CAN I COOK FOR LUNCH?";
                case DINNER: return "WHAT CAN I COOK TONIGHT?";
                case DESSERT: return "WHAT CAN I MAKE FOR DESSERT?";
                case SNACK: return "WHAT CAN I MAKE FOR A SNACK?";
                default: return "WHAT CAN I MAKE? \u00b7 " + s.toUpperCase(Locale.ROOT);
            }
        }
        return "WHAT CAN I COOK? \u00b7 " + selected.size() + " MEAL TYPES";
    }
}
