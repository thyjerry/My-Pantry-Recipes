import org.pantryrecipes.app.core.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** v2.2 checks: substitution search and pairings, meal-plan week dates, variety, themes, sources, images. */
final class V22Tests {
    private V22Tests() { }
    static void check(boolean ok, String m) { RecipeTool.check(ok, m); }
    static void eq(Object e, Object g, String m) { RecipeTool.eq(e, g, m); }

    /** Protein family of a name, or null (poultry = chicken + turkey). */
    static String protein(String t) {
        t = t.toLowerCase(Locale.ROOT);
        String[][] g = {
                {"shellfish", "shrimp", "prawn", "scallop", "squid", "crab", "lobster", "mussel", "clam"},
                {"fish", "fish", "salmon", "trout", "cod", "tuna", "haddock", "anchov", "sardine"},
                {"poultry", "chicken", "turkey", "duck"},
                {"beef", "beef", "steak", "sirloin", "ribeye", "brisket", "short rib", "chuck", "veal"},
                {"lamb", "lamb", "goat", "mutton"},
                {"pork", "pork", "bacon", "ham", "pancetta", "prosciutto", "sausage", "chorizo", "salami", "pepperoni"}};
        for (String[] r : g) for (int i = 1; i < r.length; i++) if (t.contains(r[i])) return r[0];
        return null;
    }

    static void run(List<Recipe> parsed, File root) throws Exception {
        // ---- substitutions: search shows the ingredient itself, not entries that merely mention it ----
        Substitutions subs = Substitutions.parse(new String(Files.readAllBytes(new File(root, "assets/substitutions.txt").toPath()), StandardCharsets.UTF_8));
        List<Substitutions.Entry> ct = subs.search("chicken thighs");
        check(!ct.isEmpty() && ct.get(0).name.equals("Chicken thighs"), "subs search: chicken thighs first");
        for (Substitutions.Entry e : ct) check(!e.name.toLowerCase(Locale.ROOT).startsWith("pork"), "subs search chicken thighs never lists " + e.name);
        Set<String> ctNeeds = new HashSet<String>();
        for (Substitutions.Option o : ct.get(0).options) ctNeeds.add(o.text.toLowerCase(Locale.ROOT));
        String all = ctNeeds.toString();
        check(all.contains("chicken breast") && all.contains("drumsticks") && all.contains("turkey thighs"), "chicken thighs: breast, drumsticks, turkey thighs");
        check(!all.contains("pork"), "chicken thighs: no pork");
        check(!subs.search("chicken thigh").isEmpty() && subs.search("chicken thigh").get(0).name.equals("Chicken thighs"), "singular query");
        check(!subs.search("buttermilk").isEmpty() && subs.search("buttermilk").get(0).name.equals("Buttermilk"), "subs search buttermilk");
        check(!subs.search("lemon juice").isEmpty(), "option-only search still finds something when no name matches");
        // ---- audit: every cross-protein swap in Proteins is labelled ----
        int cross = 0;
        for (Substitutions.Entry e : subs.entries) {
            if (!e.category.equals("Proteins")) continue;
            String pe = protein(e.name);
            if (pe == null) continue;
            for (Substitutions.Option o : e.options) {
                Set<String> po = new HashSet<String>();
                for (String n : o.text.split("[,/+]| or ")) { String p = protein(n); if (p != null) po.add(p); }
                if (po.isEmpty() || po.contains(pe)) continue;
                cross++;
                check(o.effect.startsWith("Different meat:") || o.effect.startsWith("Different protein:") || o.effect.startsWith("Vegan") || o.effect.startsWith("Vegetarian"),
                        "cross-protein swap is labelled: " + e.name + " -> " + o.text);
            }
        }
        check(cross > 0 && cross < 20, "cross-protein swaps are few (" + cross + ")");
        // ---- meal plan: header and grid share one week start ----
        eq("2026-11-01", Region.weekStart("2026-11-03", true), "Sunday week of Tue 3 Nov 2026");
        eq("2026-11-02", Region.weekStart("2026-11-03", false), "Monday week of Tue 3 Nov 2026");
        String[][] cases = {{"2026-11-01", "true"}, {"2026-11-07", "true"}, {"2026-11-08", "true"}, {"2026-11-01", "false"}, {"2026-11-02", "false"},
                {"2026-12-31", "true"}, {"2027-01-01", "false"}, {"2026-03-08", "true"}, {"2026-03-29", "false"}, {"2024-02-29", "true"}, {"2026-10-25", "true"}};
        for (String[] c : cases) {
            boolean sun = Boolean.parseBoolean(c[1]);
            String ws = Region.weekStart(c[0], sun);
            List<String> days = Dates.days7(ws);
            eq(ws, days.get(0), "grid starts at the header date " + c[0] + " " + c[1]);
            eq(Dates.addDays(ws, 6), days.get(6), "grid ends at the header end " + c[0]);
            check(days.contains(c[0]), "week contains its day " + c[0] + (sun ? " (Sun)" : " (Mon)"));
            eq(sun ? 6 : 0, Dates.dayOfWeek(ws), "week starts on " + (sun ? "Sunday" : "Monday") + " for " + c[0]);
        }
        check(!Dates.week(Region.weekStart("2026-11-03", true)).get(0).equals("2026-11-01"), "documents the v2.1 bug: Monday re-snap of a Sunday start");
        V22More.run(parsed, root);
    }
}
