package org.pantryrecipes.app.core;

import java.util.List;
import java.util.Map;

/** Plain-text versions of the shopping list and the week's plan, for the share sheet or the clipboard. */
public final class ShareText {
    private ShareText() { }

    /** Shopping rows are {name, amount (Double, NaN = none), unit, checked (Boolean)}; checked rows are left out. */
    public static String shopping(List<Object[]> rows) { return shopping(rows, null); }

    /** v1.8: formats a quantity in the user's units (name, amount, unit); null = as stored. */
    public interface QtyFormat { String format(String name, double amount, String unit); }

    public static String shopping(List<Object[]> rows, QtyFormat fmt) {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (Object[] r : rows) names.add((String) r[0]);
        StringBuilder b = new StringBuilder("Shopping list\n");
        for (Map.Entry<String, List<Integer>> g : Aisles.group(names).entrySet()) {
            StringBuilder sec = new StringBuilder();
            for (int i : g.getValue()) {
                Object[] r = rows.get(i);
                if (Boolean.TRUE.equals(r[3])) continue;
                double a = (Double) r[1];
                String q = Double.isNaN(a) ? "" : fmt != null ? fmt.format((String) r[0], a, (String) r[2]) : Quantity.format(a, (String) r[2], 1);
                sec.append("- ").append((String) r[0]).append(q.isEmpty() ? "" : " (" + q + ")").append('\n');
            }
            if (sec.length() > 0) b.append('\n').append(g.getKey()).append('\n').append(sec);
        }
        return b.toString().trim() + "\n";
    }

    /** Plan rows are {date, title, servings}; already sorted by date. */
    public static String plan(String weekLabel, List<Object[]> rows) {
        StringBuilder b = new StringBuilder("Meal plan");
        if (weekLabel != null && !weekLabel.isEmpty()) b.append(" \u2013 ").append(weekLabel);
        b.append('\n');
        String last = "";
        for (Object[] r : rows) {
            String d = (String) r[0];
            if (!d.equals(last)) { b.append('\n').append(Dates.label(d)).append('\n'); last = d; }
            b.append("- ").append((String) r[1]).append(" (").append(r[2]).append(r[2].equals(1) ? " serving)" : " servings)").append('\n');
        }
        if (rows.isEmpty()) b.append("\nNothing planned.\n");
        return b.toString().trim() + "\n";
    }
}
