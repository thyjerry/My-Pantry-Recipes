package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Barcodes (EAN-13, EAN-8, UPC-A, UPC-E, GTIN-14) and the mapping of an Open Food Facts product to the closest
 * pantry catalog item. The user always confirms or edits the suggestion before anything is added.
 * Product data: Open Food Facts (openfoodfacts.org), Open Database License (ODbL).
 */
public final class Barcode {
    private Barcode() { }

    public static final String API = "https://world.openfoodfacts.org/api/v2/product/";
    public static final String FIELDS = "product_name,product_name_en,generic_name,generic_name_en,brands,categories_tags,quantity";
    public static final String ATTRIBUTION = "Product data from Open Food Facts (openfoodfacts.org), available under the Open Database License (ODbL).";

    /** Digits only (spaces and dashes dropped); "" if anything else is in it. */
    public static String clean(String raw) {
        if (raw == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
            else if (c != ' ' && c != '-') return "";
        }
        return b.toString();
    }

    /** GTIN check digit test for 8, 12, 13 or 14 digits; 8-digit UPC-E codes (leading 0/1) are accepted as well. */
    public static boolean valid(String code) {
        String c = clean(code);
        int n = c.length();
        if (n != 8 && n != 12 && n != 13 && n != 14) return false;
        int sum = 0;
        for (int i = 0; i < n - 1; i++) {
            int d = c.charAt(n - 2 - i) - '0';
            sum += (i % 2 == 0) ? d * 3 : d;
        }
        int check = (10 - sum % 10) % 10;
        if (check == c.charAt(n - 1) - '0') return true;
        return n == 8 && (c.charAt(0) == '0' || c.charAt(0) == '1') && upcE(c);
    }

    /** UPC-E: expands to UPC-A and checks that check digit. */
    static boolean upcE(String c) {
        String d = c.substring(1, 7);
        char last = d.charAt(5);
        String m;
        switch (last) {
            case '0': case '1': case '2': m = d.substring(0, 2) + last + "0000" + d.substring(2, 5); break;
            case '3': m = d.substring(0, 3) + "00000" + d.substring(3, 5); break;
            case '4': m = d.substring(0, 4) + "00000" + d.charAt(4); break;
            default: m = d.substring(0, 5) + "0000" + last; break;
        }
        String a = c.charAt(0) + m + c.charAt(7);
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            int x = a.charAt(10 - i) - '0';
            sum += (i % 2 == 0) ? x * 3 : x;
        }
        return (10 - sum % 10) % 10 == a.charAt(11) - '0';
    }

    public static String url(String code) { return API + clean(code) + ".json?fields=" + FIELDS; }

    public static final class Product {
        public String code = "", name = "", generic = "", brands = "", quantity = "";
        public final List<String> categories = new ArrayList<String>();
        /** "Barilla Spaghetti n.5 (500 g)". */
        public String title() {
            String t = name.isEmpty() ? generic : name;
            String b0 = brands.split(",", -1)[0].trim();
            if (!brands.isEmpty() && !t.toLowerCase().contains(b0.toLowerCase())) t = b0 + " " + t;
            return (t.trim() + (quantity.isEmpty() ? "" : " (" + quantity + ")")).trim();
        }
    }

    private static String str(Object o, int max) {
        if (!(o instanceof String)) return "";
        String v = ((String) o).replace('\n', ' ').trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    /** Product from an Open Food Facts v2 response, or null when it was not found. Never throws. */
    @SuppressWarnings("unchecked")
    public static Product parse(String json) {
        try {
            Object root = Json.parse(json);
            if (!(root instanceof Map)) return null;
            Map<String, Object> m = (Map<String, Object>) root;
            Object st = m.get("status");
            if (!(st instanceof Number) || ((Number) st).intValue() != 1 || !(m.get("product") instanceof Map)) return null;
            Map<String, Object> p = (Map<String, Object>) m.get("product");
            Product out = new Product();
            out.code = clean(str(m.get("code"), 20));
            out.name = str(p.get("product_name"), 120);
            if (out.name.isEmpty()) out.name = str(p.get("product_name_en"), 120);
            out.generic = str(p.get("generic_name"), 120);
            if (out.generic.isEmpty()) out.generic = str(p.get("generic_name_en"), 120);
            out.brands = str(p.get("brands"), 80);
            out.quantity = quantity(str(p.get("quantity"), 60));
            Object c = p.get("categories_tags");
            if (c instanceof List) for (Object x : (List<Object>) c) {
                String t = str(x, 80);
                if (t.startsWith("en:")) out.categories.add(t.substring(3).replace('-', ' '));
                if (out.categories.size() >= 30) break;
            }
            if (out.name.isEmpty() && out.generic.isEmpty() && out.categories.isEmpty()) return null;
            return out;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final java.util.regex.Pattern NUM_COMMA = java.util.regex.Pattern.compile("(\\d),(\\d)");
    private static final java.util.regex.Pattern E_MARK = java.util.regex.Pattern.compile("(^|\\s)e(\\.|\\s|$)");
    private static final java.util.regex.Pattern NET = java.util.regex.Pattern.compile("^(net\\s*wt\\.?|net\\s*weight|net\\s*contents|poids\\s*net|netto|net)\\s*:?\\s*");
    private static final java.util.regex.Pattern MULTI = java.util.regex.Pattern.compile("^(\\d+)\\s*[x\u00d7*]\\s*(\\d+(?:\\.\\d+)?)\\s*([a-z. ]+?)\\.?$");
    private static final java.util.regex.Pattern ONE = java.util.regex.Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*([a-z. ]+?)\\.?$");
    private static final java.util.regex.Pattern SPACES = java.util.regex.Pattern.compile("\\s+");

    /** Unit spellings on packages -> {unit, factor}. */
    private static String[] unit(String u) {
        String x = u.replace(".", "").replace(" ", "");
        if (x.equals("g") || x.equals("gr") || x.equals("grams") || x.equals("gram") || x.equals("grs")) return new String[]{"g", "1"};
        if (x.equals("kg") || x.equals("kgs") || x.equals("kilo")) return new String[]{"kg", "1"};
        if (x.equals("mg")) return new String[]{"g", "0.001"};
        if (x.equals("ml") || x.equals("mls")) return new String[]{"ml", "1"};
        if (x.equals("cl")) return new String[]{"ml", "10"};
        if (x.equals("dl")) return new String[]{"ml", "100"};
        if (x.equals("l") || x.equals("lt") || x.equals("ltr") || x.equals("litre") || x.equals("liter") || x.equals("litres") || x.equals("liters")) return new String[]{"l", "1"};
        if (x.equals("oz") || x.equals("ounce") || x.equals("ounces")) return new String[]{"oz", "1"};
        if (x.equals("floz")) return new String[]{"fl oz", "1"};
        if (x.equals("lb") || x.equals("lbs") || x.equals("pound") || x.equals("pounds")) return new String[]{"lb", "1"};
        return null;
    }

    private static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return Units.dec2(v);
    }

    private static String amount(String text) {
        java.util.regex.Matcher m = MULTI.matcher(text);
        if (m.matches()) {
            String[] u = unit(m.group(3).trim());
            if (u != null) return num(Integer.parseInt(m.group(1)) * Double.parseDouble(m.group(2)) * Double.parseDouble(u[1])) + " " + u[0];
        }
        m = ONE.matcher(text);
        if (m.matches()) {
            String[] u = unit(m.group(2).trim());
            if (u != null) return num(Double.parseDouble(m.group(1)) * Double.parseDouble(u[1])) + " " + u[0];
        }
        return null;
    }

    /**
     * v1.8: a package size as Open Food Facts gives it, made fit for the pantry: the EU estimated sign is dropped
     * ("400 g e", "℮ 1 l"), decimal commas read ("1,5 l"), "Net wt" removed, multipacks totalled ("4 x 125 g" -> "500 g"),
     * cl/dl/mg converted, and "16 oz (454 g)" keeps the first size. Text that is not a size is kept, tidied.
     */
    public static String quantity(String raw) {
        if (raw == null) return "";
        String t = raw.replace('\u212e', ' ').replace('\u00a0', ' ').toLowerCase(java.util.Locale.ROOT).trim();
        t = NUM_COMMA.matcher(t).replaceAll("$1.$2");
        t = NET.matcher(t).replaceFirst("");
        t = E_MARK.matcher(t).replaceAll("$1 ");
        t = SPACES.matcher(t).replaceAll(" ").trim();
        if (t.isEmpty()) return "";
        int par = t.indexOf('(');
        if (par > 0) {
            String a = amount(t.substring(0, par).trim());
            if (a != null) return a;
            int close = t.indexOf(')', par);
            a = amount(t.substring(par + 1, close > par ? close : t.length()).trim());
            if (a != null) return a;
        }
        String a = amount(t);
        if (a != null) return a;
        String keep = SPACES.matcher(raw.replace('\u212e', ' ')).replaceAll(" ").trim();
        if (keep.endsWith(" e")) keep = keep.substring(0, keep.length() - 2).trim();
        return keep.length() > 30 ? keep.substring(0, 30).trim() : keep;
    }

    private static List<String> words(String s) {
        List<String> w = new ArrayList<String>();
        StringBuilder b = new StringBuilder();
        String t = s.toLowerCase() + " ";
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if ((c >= 'a' && c <= 'z') || c == '\u00e9' || c == '\u00e8') b.append(c);
            else if (b.length() > 0) { w.add(b.toString()); b.setLength(0); }
        }
        return w;
    }

    private static boolean containsWords(List<String> hay, List<String> needle) {
        if (needle.isEmpty()) return false;
        for (int i = 0; i + needle.size() <= hay.size(); i++) {
            boolean ok = true;
            for (int j = 0; j < needle.size(); j++) {
                String h = hay.get(i + j), n = needle.get(j);
                if (!(h.equals(n) || h.equals(n + "s") || h.equals(n + "es") || (n.endsWith("s") && h.equals(n.substring(0, n.length() - 1))))) { ok = false; break; }
            }
            if (ok) return true;
        }
        return false;
    }

    /**
     * Closest catalog item name for a product, or "" when nothing fits. The product name counts most, then the
     * generic name, then the most specific categories. Longer (more specific) item names win ties.
     */
    public static String match(Product p) {
        List<String> m = matches(p, 1);
        return m.isEmpty() ? "" : m.get(0);
    }

    /**
     * v1.8: the best {@code n} catalog items for a product, best first, for the confirm step. Brand words are left out
     * of the product name (a brand called "Sunflower" is not sunflower oil), and an item whose key equals the
     * product's normalized name wins outright.
     */
    public static List<String> matches(Product p, int n) {
        List<String> out = new ArrayList<String>();
        if (p == null || n <= 0) return out;
        List<String> name = words(p.name), gen = words(p.generic);
        List<String> brand = words(p.brands);
        if (!brand.isEmpty() && name.size() > brand.size()) {
            List<String> nb = new ArrayList<String>();
            for (String w : name) if (!brand.contains(w) || gen.contains(w)) nb.add(w);
            if (!nb.isEmpty()) name = nb;
        }
        String nameKey = Normalizer.normalize(join(name));
        List<List<String>> cats = new ArrayList<List<String>>();
        for (int k = p.categories.size() - 1; k >= 0; k--) cats.add(words(p.categories.get(k)));
        final List<String> items = new ArrayList<String>();
        final List<Integer> scores = new ArrayList<Integer>();
        for (String item : Catalog.allItems()) {
            List<String> iw = words(item);
            if (iw.isEmpty()) continue;
            int score = 0;
            if (!nameKey.isEmpty() && nameKey.equals(Normalizer.normalize(item))) score = 500;
            else if (containsWords(name, iw)) score = 300;
            else if (containsWords(gen, iw)) score = 200;
            else {
                for (int k = 0; k < cats.size() && k < 10; k++) {
                    if (containsWords(cats.get(k), iw)) { score = 100 - k * 5; break; }
                }
            }
            if (score == 0) continue;
            score += iw.size() * 10;
            items.add(item);
            scores.add(score);
        }
        Integer[] order = new Integer[items.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, new java.util.Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                int c = scores.get(b) - scores.get(a);
                if (c != 0) return c;
                c = items.get(b).length() - items.get(a).length();
                return c != 0 ? c : a - b;
            }
        });
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (Integer i : order) {
            if (out.size() >= n) break;
            String k = Normalizer.normalize(items.get(i));
            if (seen.add(k)) out.add(items.get(i));
        }
        return out;
    }

    private static String join(List<String> w) {
        StringBuilder b = new StringBuilder();
        for (String x : w) { if (b.length() > 0) b.append(' '); b.append(x); }
        return b.toString();
    }
}
