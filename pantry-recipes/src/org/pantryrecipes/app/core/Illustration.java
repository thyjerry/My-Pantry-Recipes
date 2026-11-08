package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * v2.2: a small illustrated card for every recipe, drawn from assets/illustrations.txt (original geometric glyphs on a
 * 24x24 grid). Zero bytes per recipe, zero network: the glyph comes from the category and main protein, the accent and
 * background pattern from the cuisine. Android draws it on a Canvas; the web emits the same shapes as inline SVG.
 */
public final class Illustration {
    private Illustration() {}

    public static final class Shape {
        public final char ink, kind; public final float[] v;
        Shape(char ink, char kind, float[] v) { this.ink = ink; this.kind = kind; this.v = v; }
    }
    public static final class Glyph {
        public final String name; public final int bg; public final List<Shape> shapes = new ArrayList<Shape>();
        Glyph(String n, int bg) { name = n; this.bg = bg; }
    }

    public static Map<String, Glyph> parse(String text) {
        Map<String, Glyph> out = new LinkedHashMap<String, Glyph>();
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\|");
            Glyph g = new Glyph(p[0].trim(), Integer.parseInt(p[1].trim(), 16));
            for (String s : p[2].split(";")) {
                String[] t = s.trim().split("\\s+");
                float[] v = new float[t.length - 2];
                for (int i = 2; i < t.length; i++) v[i - 2] = Float.parseFloat(t[i]);
                g.shapes.add(new Shape(t[0].charAt(0), t[1].charAt(0), v));
            }
            out.put(g.name, g);
        }
        return out;
    }

    static boolean any(String hay, String... words) { for (String w : words) if (hay.contains(w)) return true; return false; }

    /** The glyph for a recipe: protein first for mains, otherwise the course. */
    public static String glyph(Recipe r) {
        String cat = r.category == null ? "" : r.category;
        StringBuilder b = new StringBuilder(" ").append(r.title.toLowerCase(Locale.US)).append(' ');
        for (Ingredient i : r.ingredients) if (!i.optional) b.append(' ').append(i.key).append(' ');
        String h = b.toString();
        switch (cat) {
            case "Dessert": return "cake";
            case "Baking": return any(h, "cake", "cookie", "brownie", "muffin", "tart", "pie ", "scone") ? "cake" : "bread";
            case "Drink": return "drink";
            case "Sauce": return "sauce";
            case "Salad": return "salad";
            case "Soup": return "soup";
            default: break;
        }
        if (any(h, "shrimp", "prawn", "crab", "lobster", "mussel", "clam", "scallop", "squid", "octopus")) return "shrimp";
        if (any(h, "chicken", "turkey", "duck", "quail")) return "chicken";
        if (any(h, "lamb", "mutton", "goat")) return "lamb";
        if (any(h, "beef", "steak", "veal", "brisket", "oxtail", "venison", "bison")) return "beef";
        if (any(h, "pork", "bacon", "ham ", "sausage", "chorizo", "pancetta", "prosciutto")) return "pork";
        if (any(h, "fish", "salmon", "tuna", "cod", "haddock", "trout", "mackerel", "sardine", "tilapia", "halibut", "anchov", "sea bass", "snapper", "catfish", "pollock")) return "fish";
        if (any(h, "pasta", "spaghetti", "noodle", "macaroni", "penne", "fettuccine", "linguine", "lasagn", "ravioli", "gnocchi", "orzo")) return "pasta";
        if (cat.equals("Breakfast") && any(h, " egg", "omelet", "frittata")) return "egg";
        if (any(h, " rice ", "risotto", "pilaf", "biryani", "fried rice", "paella")) return "rice";
        if (cat.equals("Snack")) return "snack";
        if (cat.equals("Breakfast")) return any(h, "toast", "bread", "pancake", "waffle") ? "bread" : "egg";
        if (r.tags.contains("vegetarian") || r.tags.contains("vegan") || cat.equals("Side")) return "leaf";
        return "plate";
    }

    static int mod(int x, int n) { return ((x % n) + n) % n; } // Math.floorMod is API 24

    static int hash(String s) { int h = 0; for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i); return h; }

    /** Cuisine accent: a hue from the cuisine name at fixed saturation/lightness (decorative only). */
    public static int accent(String cuisine) {
        int hue = mod(hash(cuisine == null ? "" : cuisine), 360);
        return hsl(hue, 0.70, 0.88);
    }
    /** Background pattern 0..3 (none, dots, stripes, waves) from the cuisine. */
    public static int pattern(String cuisine) { return mod(hash(cuisine == null ? "" : cuisine) >> 9, 4); }

    public static int hsl(int h, double s, double l) {
        double c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs((h / 60.0) % 2 - 1)), m = l - c / 2, r, g, b;
        if (h < 60) { r = c; g = x; b = 0; } else if (h < 120) { r = x; g = c; b = 0; } else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; } else if (h < 300) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
        return ((int) Math.round((r + m) * 255) << 16) | ((int) Math.round((g + m) * 255) << 8) | (int) Math.round((b + m) * 255);
    }

    /** Key for caching drawn cards: glyph + cuisine look. */
    public static String key(Recipe r) { return glyph(r) + "|" + pattern(r.cuisine) + "|" + Integer.toHexString(accent(r.cuisine)); }

    /** Dark ink on the glyph background must reach 3:1 (WCAG non-text contrast); checked in tests. */
    public static final int INK = 0x99000000;
}
