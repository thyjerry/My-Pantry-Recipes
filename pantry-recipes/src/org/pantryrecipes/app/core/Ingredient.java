package org.pantryrecipes.app.core;

/** One recipe ingredient line. amount is NaN when there is no numeric quantity. */
public final class Ingredient {
    public final String name;
    public final String key;
    public final double amount;
    public final String unit;
    public final boolean optional;

    public Ingredient(String name, double amount, String unit, boolean optional) {
        this.name = name;
        this.key = Normalizer.normalize(name);
        this.amount = amount;
        this.unit = unit == null ? "" : unit;
        this.optional = optional;
    }

    /** "2½ cups" style text for the quantity at the given scale (empty when no quantity). */
    public String quantityText(double scale) {
        return Quantity.format(amount, unit, scale);
    }
}
