package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Curated ingredient substitutions parsed from assets/substitutions.txt. */
public final class Substitutions {

    /** One way to replace an ingredient. */
    public static final class Option {
        public final String text;
        /** All groups are needed; any key inside a group will do. */
        public final List<List<String>> needs = new ArrayList<List<String>>();
        public final List<List<String>> needNames = new ArrayList<List<String>>();
        /** v1.7: "swap:original" (e.g. "3/4:1") or "to taste"; empty when the line has no ratio field. */
        public String ratio = "";
        /** v1.7: how the dish changes, e.g. "Tangier and lighter". */
        public String effect = "";
        /** v1.7: worked out from the needs. A diet flag holds only if every alternative satisfies it. */
        public boolean vegetarian, vegan, dairyFree, glutenFree;
        /** v1.7: allergen ids (Allergens.IDS order) that any needed ingredient may bring in. */
        public final List<String> allergens = new ArrayList<String>();
        Option(String text) { this.text = text; }

        /** Whether the ratio field is "to taste" or two positive numbers/fractions joined by ":". */
        public boolean ratioValid() { return ratioValue(ratio) != null; }

        /** Short label such as "¾ for 1" or "to taste"; "" when absent or invalid. */
        public String ratioLabel() {
            if (!ratioValid()) return "";
            if (ratio.equals("to taste")) return "to taste";
            String[] p = ratio.split(":");
            return pretty(p[0].trim()) + " for " + pretty(p[1].trim());
        }

        /** Diet labels in a fixed order (Vegan implies Vegetarian, so only one of them is shown). */
        public List<String> dietLabels() {
            List<String> out = new ArrayList<String>();
            if (vegan) out.add("Vegan"); else if (vegetarian) out.add("Vegetarian");
            if (dairyFree) out.add("Dairy-free");
            if (glutenFree) out.add("Gluten-free");
            return out;
        }

        /** One line for the UI: "Ratio ¾ for 1 · Vegan · Dairy-free · Contains: tree nuts". */
        public String flagsLine() {
            StringBuilder sb = new StringBuilder();
            String rl = ratioLabel();
            if (!rl.isEmpty()) sb.append("Ratio ").append(rl);
            for (String d : dietLabels()) { if (sb.length() > 0) sb.append(" \u00b7 "); sb.append(d); }
            if (!allergens.isEmpty()) {
                if (sb.length() > 0) sb.append(" \u00b7 ");
                sb.append("Contains: ");
                for (int i = 0; i < allergens.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(Allergens.label(allergens.get(i)).toLowerCase(java.util.Locale.ROOT));
                }
            }
            return sb.toString();
        }

        void computeFlags() {
            vegetarian = vegan = dairyFree = glutenFree = true;
            for (List<String> g : needs) for (String k : g) {
                boolean meat = DietClassifier.isMeat(k), dairy = DietClassifier.isDairy(k), other = DietClassifier.isOtherAnimal(k);
                if (meat) vegetarian = false;
                if (meat || dairy || other) vegan = false;
                if (dairy) dairyFree = false;
                if (DietClassifier.hasGluten(k)) glutenFree = false;
            }
            if (needs.isEmpty()) vegetarian = vegan = dairyFree = glutenFree = false;
            for (String id : Allergens.IDS) {
                boolean hit = false;
                for (List<String> g : needs) for (String k : g) if (Allergens.keyHas(id, k)) hit = true;
                if (hit) allergens.add(id);
            }
        }

        public boolean available(Matcher m) {
            if (needs.isEmpty()) return false;
            for (List<String> g : needs) {
                boolean any = false;
                for (String k : g) if (m.has(k)) { any = true; break; }
                if (!any) return false;
            }
            return true;
        }

        /** The pantry ingredient names this option would use, e.g. "milk + lemon". */
        public String usesLabel(Matcher m) {
            StringBuilder sb = new StringBuilder();
            for (int gi = 0; gi < needs.size(); gi++) {
                List<String> g = needs.get(gi);
                String pick = needNames.get(gi).get(0);
                if (m != null) for (int j = 0; j < g.size(); j++) if (m.has(g.get(j))) { pick = needNames.get(gi).get(j); break; }
                if (sb.length() > 0) sb.append(" + ");
                sb.append(pick);
            }
            return sb.toString();
        }
    }

    private static final String[] CLAIMS = {"vegan", "vegetarian", "dairy-free", "gluten-free", "nut-free", "egg-free",
            "sesame-free", "peanut-free", "soy-free"};

    /**
     * Diet/allergen claims an option makes in its own words: "(vegan)" in the text, an effect that starts with
     * "Vegan;", and negations such as "not gluten-free" or "contains gluten" (returned as "!gluten-free").
     * Used by the build checks so the wording never promises more than the computed flags.
     */
    public static List<String> claims(Option o) {
        List<String> out = new ArrayList<String>();
        String t = o.text.toLowerCase(java.util.Locale.ROOT), ef = o.effect.toLowerCase(java.util.Locale.ROOT);
        String first = ef.split(";")[0].trim();
        for (String c : CLAIMS) {
            boolean neg = t.contains("not " + c) || ef.contains("not " + c);
            if (neg) out.add("!" + c);
            else if (t.contains("(" + c + ")") || first.equals(c)) out.add(c);
        }
        if (t.contains("contains gluten") || ef.contains("contains gluten")) out.add("!gluten-free");
        if (t.contains("contains dairy") || ef.contains("contains dairy")) out.add("!dairy-free");
        return out;
    }

    public static boolean claimHolds(Option o, String c) {
        boolean neg = c.startsWith("!");
        String k = neg ? c.substring(1) : c;
        boolean v;
        if (k.equals("vegan")) v = o.vegan;
        else if (k.equals("vegetarian")) v = o.vegetarian;
        else if (k.equals("dairy-free")) v = o.dairyFree && !o.allergens.contains(Allergens.DAIRY);
        else if (k.equals("gluten-free")) v = o.glutenFree;
        else if (k.equals("nut-free")) v = !o.allergens.contains(Allergens.PEANUTS) && !o.allergens.contains(Allergens.TREE_NUTS);
        else if (k.equals("egg-free")) v = !o.allergens.contains(Allergens.EGGS);
        else if (k.equals("sesame-free")) v = !o.allergens.contains(Allergens.SESAME);
        else if (k.equals("peanut-free")) v = !o.allergens.contains(Allergens.PEANUTS);
        else if (k.equals("soy-free")) v = !o.allergens.contains(Allergens.SOY);
        else return false;
        return neg ? !v : v;
    }

    private static String pretty(String f) {
        if (f.equals("1/2")) return "\u00bd";
        if (f.equals("1/3")) return "\u2153";
        if (f.equals("2/3")) return "\u2154";
        if (f.equals("1/4")) return "\u00bc";
        if (f.equals("3/4")) return "\u00be";
        return f;
    }

    /** Parses "a:b" (numbers or fractions, both > 0) to a/b; "to taste" gives 0; null if invalid. */
    public static Double ratioValue(String r) {
        if (r == null) return null;
        r = r.trim();
        if (r.equals("to taste")) return 0.0;
        String[] p = r.split(":", -1);
        if (p.length != 2) return null;
        Double a = num(p[0].trim()), b = num(p[1].trim());
        if (a == null || b == null || a <= 0 || b <= 0) return null;
        return a / b;
    }

    private static Double num(String s) {
        if (!s.matches("[0-9]+(\\.[0-9]+)?(/[0-9]+)?")) return null;
        int sl = s.indexOf('/');
        if (sl < 0) return Double.parseDouble(s);
        double d = Double.parseDouble(s.substring(sl + 1));
        if (d == 0) return null;
        return Double.parseDouble(s.substring(0, sl)) / d;
    }

    public static final class Entry {
        public final String category, name;
        public final Set<String> keys = new LinkedHashSet<String>();
        public final List<String> aliases = new ArrayList<String>();
        public final List<Option> options = new ArrayList<Option>();
        public String note = "";
        Entry(String category, String name) { this.category = category; this.name = name; }

        /** Lower-case text used by the search box. */
        public String searchText() {
            StringBuilder sb = new StringBuilder(name).append(' ').append(category);
            for (String a : aliases) sb.append(' ').append(a);
            for (Option o : options) sb.append(' ').append(o.text).append(' ').append(o.effect);
            return sb.toString().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public final List<Entry> entries = new ArrayList<Entry>();
    public final List<String> categories = new ArrayList<String>();
    private final Map<String, Entry> byKey = new HashMap<String, Entry>();
    /** v1.7 lint: keys claimed by two entries, and option lines without 4 fields (for the build checks). */
    public final List<String> duplicateKeys = new ArrayList<String>();
    public final List<String> shortLines = new ArrayList<String>();

    public static Substitutions parse(String text) {
        Substitutions s = new Substitutions();
        String cat = "Other";
        Entry cur = null;
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || (line.startsWith("#") && !line.startsWith("##"))) continue;
            if (line.startsWith("##")) {
                cat = line.substring(2).trim();
                if (!s.categories.contains(cat)) s.categories.add(cat);
            } else if (line.startsWith("*")) {
                String[] p = line.substring(1).split("\\|");
                cur = new Entry(cat, p[0].trim());
                addKey(cur, cur.name);
                if (p.length > 1) for (String a : p[1].split(",")) {
                    if (a.trim().isEmpty()) continue;
                    cur.aliases.add(a.trim());
                    addKey(cur, a.trim());
                }
                s.entries.add(cur);
                for (String k : cur.keys) {
                    if (!s.byKey.containsKey(k)) s.byKey.put(k, cur);
                    else s.duplicateKeys.add(k + " (" + s.byKey.get(k).name + " / " + cur.name + ")");
                }
            } else if (line.startsWith("=") && cur != null) {
                String[] p = line.substring(1).split("\\|");
                Option o = new Option(p[0].trim());
                if (p.length > 1) for (String g : p[1].split("\\+")) {
                    List<String> keys = new ArrayList<String>(), names = new ArrayList<String>();
                    for (String n : g.split("/")) {
                        String k = Normalizer.normalize(n.trim());
                        if (k.isEmpty() || cur.keys.contains(k)) continue;
                        keys.add(k);
                        names.add(n.trim());
                    }
                    if (!keys.isEmpty()) { o.needs.add(keys); o.needNames.add(names); }
                }
                if (p.length > 2) o.ratio = p[2].trim();
                if (p.length > 3) o.effect = p[3].trim();
                if (p.length != 4) s.shortLines.add(cur.name + ": " + line);
                o.computeFlags();
                cur.options.add(o);
            } else if (line.startsWith("~") && cur != null) {
                cur.note = cur.note.isEmpty() ? line.substring(1).trim() : cur.note + " " + line.substring(1).trim();
            }
        }
        return s;
    }

    private static void addKey(Entry e, String name) {
        String k = Normalizer.normalize(name);
        if (!k.isEmpty()) e.keys.add(k);
    }

    /** The entry for an ingredient key: exact first, then its family ("cheddar" has none, "spaghetti" uses "pasta"). */
    public Entry forKey(String key) {
        if (key == null) return null;
        Entry e = byKey.get(key);
        if (e == null) {
            String f = Normalizer.family(key);
            if (f != null) e = byKey.get(f);
        }
        return e;
    }

    /** Options for this ingredient that the pantry can cover right now. */
    public List<Option> availableFor(String key, Matcher m) {
        Entry e = forKey(key);
        if (e == null) return Collections.emptyList();
        List<Option> out = new ArrayList<Option>();
        for (Option o : e.options) if (o.available(m)) out.add(o);
        return out;
    }

    public List<Entry> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isEmpty()) return entries;
        List<Entry> out = new ArrayList<Entry>();
        String qk = Normalizer.normalize(q);
        for (Entry e : entries) if (e.searchText().contains(q) || (!qk.isEmpty() && e.keys.contains(qk))) out.add(e);
        return out;
    }

    public int optionCount() {
        int n = 0;
        for (Entry e : entries) n += e.options.size();
        return n;
    }
}
