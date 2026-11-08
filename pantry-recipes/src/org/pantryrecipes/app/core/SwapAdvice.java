package org.pantryrecipes.app.core;
import java.util.Collection;
/** Conservative name-based advice. Unknown brands and cross-contact are not assessed. */
public final class SwapAdvice {
    private SwapAdvice() {}
    public static String caution(Substitutions.Option o, Collection<String> dislikes, Collection<String> avoid, Collection<String> diets) {
        for (java.util.List<String> group : o.needs) for (String k : group) {
            if (dislikes != null) for (String d : dislikes) if (Allergens.dislikes(k, d)) return "Conflicts with your dislikes: " + d;
            if (avoid != null) for (String a : avoid) if (Allergens.keyHas(a, k)) return "Conflicts with your avoided allergens: " + Allergens.label(a);
        }
        if (diets != null) for (String d : diets) if ((d.equals("vegan") && !o.vegan) || (d.equals("vegetarian") && !o.vegetarian) || (d.equals("dairy-free") && !o.dairyFree) || (d.equals("gluten-free") && !o.glutenFree)) return "Not compatible with selected diet: " + d;
        return "";
    }
    private static String join(java.util.List<String> names) { StringBuilder s = new StringBuilder(); for (String n : names) { if (s.length() > 0) s.append(" / "); s.append(n); } return s.toString(); }
    public static String quantity(Substitutions.Option o, double amount, String unit) {
        if ("to taste".equals(o.ratio)) return "Add gradually to taste; no fixed replacement amount.";
        Double ratio = Substitutions.ratioValue(o.ratio);
        if (ratio == null || amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount) || o.needs.size() != 1 || !(unit == null || unit.isEmpty() || java.util.Arrays.asList("cup", "cups", "tbsp", "tsp", "ml", "l").contains(unit)))
            return "Use the quantities in this option; ratios are by volume or count, not a mass conversion.";
        return "Replacement estimate: " + Quantity.format(amount * ratio, unit, 1) + " " + join(o.needNames.get(0)) + ". Same-unit estimate; texture and cooking time may change.";
    }
}
