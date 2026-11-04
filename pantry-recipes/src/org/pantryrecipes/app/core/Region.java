package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v2.0 region: the first question of the welcome flow and a Settings entry. A region only sets editable defaults
 * (units, temperature, gas marks, currency, allergen list, price source, date and number format, holiday checklist)
 * and the preferred ingredient names for display. Matching, search, pantry and swaps already accept either name
 * (Normalizer synonyms). Shared with the web app (core20.js).
 */
public final class Region {
    private Region() { }

    public static final String US = "us", CA = "ca", UK = "uk", EU = "eu", ANZ = "anz", OTHER = "other";
    public static final List<String> IDS = Collections.unmodifiableList(Arrays.asList(US, CA, UK, EU, ANZ, OTHER));
    public static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(
            "United States", "Canada", "UK & Ireland", "Europe (EU)", "Australia / New Zealand", "Other"));

    public static String valid(String id) { return IDS.contains(id) ? id : ""; }

    public static String name(String id) {
        int k = IDS.indexOf(id);
        return k < 0 ? "Not set" : NAMES.get(k);
    }

    /** Best guess from a language tag such as "en-GB" or "de-DE" (the web app asks anyway; this only preselects). */
    public static String guess(String languageTag) {
        if (languageTag == null) return US;
        String t = languageTag.replace('_', '-').toLowerCase(Locale.ROOT);
        String[] p = t.split("-");
        String lang = p[0], c = p.length > 1 ? p[p.length - 1] : "";
        if (c.equals("us")) return US;
        if (c.equals("ca")) return CA;
        if (c.equals("gb") || c.equals("uk") || c.equals("ie")) return UK;
        if (c.equals("au") || c.equals("nz")) return ANZ;
        List<String> eu = Arrays.asList("at", "be", "bg", "hr", "cy", "cz", "dk", "ee", "fi", "fr", "de", "gr", "hu", "it", "lv", "lt", "lu", "mt",
                "nl", "pl", "pt", "ro", "sk", "si", "es", "se");
        if (eu.contains(c)) return EU;
        if (!c.isEmpty()) return OTHER;
        if (lang.equals("en")) return US;
        List<String> euLang = Arrays.asList("de", "fr", "it", "es", "nl", "pl", "pt", "sv", "da", "fi", "el", "cs", "sk", "sl", "hu", "ro", "bg", "hr", "et", "lv", "lt", "mt", "ga");
        return euLang.contains(lang) ? EU : OTHER;
    }

    /** Editable defaults a region sets. */
    public static final class Defaults {
        public String units, weightUnit, currency, dateFormat, priceSource;
        public boolean showCups, gasMarks, fanNote, decimalComma, cup250, clock24, weekStartsSunday;
        public List<String> allergens;
    }

    /** Allergen checklists: FDA major food allergens (US); EU/UK 14; Canada priority allergens; FSANZ. */
    public static final List<String> FDA_9 = Collections.unmodifiableList(Arrays.asList(
            Allergens.DAIRY, Allergens.EGGS, Allergens.FISH, Allergens.SHELLFISH, Allergens.TREE_NUTS, Allergens.PEANUTS, Allergens.GLUTEN, Allergens.SOY, Allergens.SESAME));
    public static final List<String> EU_14 = Collections.unmodifiableList(Arrays.asList(
            Allergens.GLUTEN, Allergens.SHELLFISH, Allergens.EGGS, Allergens.FISH, Allergens.PEANUTS, Allergens.SOY, Allergens.DAIRY, Allergens.TREE_NUTS,
            Allergens.CELERY, Allergens.MUSTARD, Allergens.SESAME, Allergens.SULPHITES, Allergens.LUPIN, Allergens.MOLLUSCS));
    public static final List<String> CANADA = Collections.unmodifiableList(Arrays.asList(
            Allergens.EGGS, Allergens.DAIRY, Allergens.MUSTARD, Allergens.PEANUTS, Allergens.SHELLFISH, Allergens.MOLLUSCS, Allergens.FISH, Allergens.SESAME,
            Allergens.SOY, Allergens.SULPHITES, Allergens.TREE_NUTS, Allergens.GLUTEN));
    public static final List<String> FSANZ = Collections.unmodifiableList(Arrays.asList(
            Allergens.PEANUTS, Allergens.TREE_NUTS, Allergens.EGGS, Allergens.DAIRY, Allergens.SESAME, Allergens.FISH, Allergens.SHELLFISH, Allergens.MOLLUSCS,
            Allergens.SOY, Allergens.LUPIN, Allergens.GLUTEN, Allergens.SULPHITES));

    public static List<String> allergens(String region) {
        if (US.equals(region)) return FDA_9;
        if (CA.equals(region)) return CANADA;
        if (UK.equals(region) || EU.equals(region)) return EU_14;
        if (ANZ.equals(region)) return FSANZ;
        return Allergens.IDS;
    }

    public static String allergenListName(String region) {
        if (US.equals(region)) return "U.S. FDA major food allergens (9)";
        if (CA.equals(region)) return "Health Canada priority allergens";
        if (UK.equals(region)) return "UK/EU 14 allergens";
        if (EU.equals(region)) return "EU 14 allergens";
        if (ANZ.equals(region)) return "FSANZ declared allergens";
        return "All allergen groups the app knows";
    }

    /** Region-specific allergen label (EU law says "cereals containing gluten", "crustaceans", "milk"). */
    public static String allergenLabel(String id, String region) {
        boolean eu = UK.equals(region) || EU.equals(region) || ANZ.equals(region);
        if (Allergens.GLUTEN.equals(id)) return eu ? "Cereals containing gluten" : US.equals(region) ? "Wheat / gluten" : "Gluten";
        if (Allergens.SHELLFISH.equals(id)) return eu || CA.equals(region) ? "Crustaceans (shellfish)" : US.equals(region) ? "Crustacean shellfish" : "Shellfish";
        if (Allergens.DAIRY.equals(id)) return eu || US.equals(region) || CA.equals(region) ? "Milk" : "Dairy";
        if (Allergens.SOY.equals(id)) return eu ? "Soya" : "Soy";
        if (Allergens.SULPHITES.equals(id)) return US.equals(region) ? "Sulfites" : "Sulphites";
        return Allergens.label(id);
    }

    public static Defaults defaults(String region) {
        Defaults d = new Defaults();
        String r = valid(region).isEmpty() ? US : region;
        d.allergens = allergens(r);
        d.showCups = true;
        if (US.equals(r)) {
            d.units = Units.US; d.weightUnit = "oz"; d.currency = "$"; d.dateFormat = "MM/DD/YYYY"; d.priceSource = "bls"; d.weekStartsSunday = true;
        } else if (CA.equals(r)) {
            d.units = Units.US; d.weightUnit = "g"; d.currency = "$"; d.dateFormat = "YYYY-MM-DD"; d.priceSource = "user"; d.weekStartsSunday = true;
        } else if (UK.equals(r)) {
            d.units = Units.WEIGHTS; d.weightUnit = "g"; d.currency = "\u00a3"; d.dateFormat = "DD/MM/YYYY"; d.priceSource = "user";
            d.gasMarks = true; d.fanNote = true; d.showCups = false; d.clock24 = true;
        } else if (EU.equals(r)) {
            d.units = Units.WEIGHTS; d.weightUnit = "g"; d.currency = "\u20ac"; d.dateFormat = "DD.MM.YYYY"; d.priceSource = "user";
            d.fanNote = true; d.decimalComma = true; d.showCups = false; d.clock24 = true;
        } else if (ANZ.equals(r)) {
            d.units = Units.WEIGHTS; d.weightUnit = "g"; d.currency = "$"; d.dateFormat = "DD/MM/YYYY"; d.priceSource = "user";
            d.fanNote = true; d.cup250 = true;
        } else {
            d.units = Units.METRIC; d.weightUnit = "g"; d.currency = "$"; d.dateFormat = "YYYY-MM-DD"; d.priceSource = "user";
            d.fanNote = true; d.clock24 = true;
        }
        return d;
    }

    public static final String FAN_NOTE = "Fan (convection) oven: about 20 \u00b0C lower than the temperature given.";
    public static final String CUP_NOTE = "Australian cups and tablespoons are slightly larger (250 ml cup, 20 ml tablespoon); the app uses 240 ml cups and 15 ml tablespoons.";
    public static final String PRICE_NOTE_NON_US = "No bundled prices for this region: costs use only the prices you enter, in your currency. "
            + "(Built-in averages are U.S. data and are not used here.)";

    /** Local measuring habits, explained (shown on the Conversions screen; display only, nothing is converted silently). */
    public static List<String[]> measuringNotes(String region) {
        List<String[]> out = new ArrayList<String[]>();
        boolean uk = UK.equals(region), anz = ANZ.equals(region), us = US.equals(region) || CA.equals(region);
        out.add(new String[]{"Pint", uk ? "A UK (imperial) pint is 568 ml; a U.S. pint is 473 ml. Recipes from Britain or Ireland mean 568 ml."
                : "A U.S. pint is 473 ml; a UK (imperial) pint is 568 ml, so British recipes need about 20% more."});
        out.add(new String[]{"Tablespoon", anz ? "An Australian tablespoon is 20 ml (4 teaspoons); U.S., UK and NZ tablespoons are 15 ml. The app uses 15 ml."
                : "A tablespoon is 15 ml (3 teaspoons); an Australian tablespoon is 20 ml."});
        out.add(new String[]{"Cup", anz ? "Australian and NZ cups are 250 ml; U.S. cups are about 240 ml. The app uses 240 ml, the difference rarely matters."
                : "A U.S. cup is about 240 ml; Australian and NZ cups are 250 ml; British recipes usually weigh instead."});
        out.add(new String[]{"Dessertspoon", "About 10 ml (2 teaspoons), a British and Irish measure."});
        out.add(new String[]{"A knob of butter", "A small lump, about 1 tablespoon (15–20 g). Not exact on purpose: it is for frying or finishing."});
        out.add(new String[]{"Stick of butter", "U.S. butter sticks are 113 g (½ cup, 8 tablespoons)" + (us ? "." : "; elsewhere butter is usually sold in 250 g blocks.")});
        out.add(new String[]{"Gas mark", "UK oven setting: gas 4 is about 180 °C / 350 °F. See the oven table."});
        out.add(new String[]{"Fan oven", "Fan (convection) ovens cook hotter: use about 20 °C (25 °F) lower than a recipe written for a conventional oven."});
        out.add(new String[]{"Ounce and fluid ounce", "1 oz (weight) = 28 g; 1 U.S. fl oz = 29.6 ml; 1 UK fl oz = 28.4 ml."});
        return out;
    }

    // ---------------- date and number formats ----------------

    /** yyyy-mm-dd shown in the region's format; invalid dates are returned unchanged. */
    public static String formatDate(String iso, String format) {
        if (!Dates.valid(iso)) return iso == null ? "" : iso;
        String y = iso.substring(0, 4), m = iso.substring(5, 7), d = iso.substring(8, 10);
        if ("MM/DD/YYYY".equals(format)) return m + "/" + d + "/" + y;
        if ("DD/MM/YYYY".equals(format)) return d + "/" + m + "/" + y;
        if ("DD.MM.YYYY".equals(format)) return d + "." + m + "." + y;
        return iso;
    }

    /** "18:30" or "6:30 PM" for minutes after midnight. */
    public static String formatTime(int minutes, boolean clock24) {
        int h = ((minutes / 60) % 24 + 24) % 24, m = ((minutes % 60) + 60) % 60;
        String mm = (m < 10 ? "0" : "") + m;
        if (clock24) return (h < 10 ? "0" : "") + h + ":" + mm;
        return (h % 12 == 0 ? 12 : h % 12) + ":" + mm + (h < 12 ? " AM" : " PM");
    }

    /** First day of the week containing iso: Monday, or Sunday when weekStartsSunday. */
    public static String weekStart(String iso, boolean sunday) {
        if (!sunday) return Dates.weekStart(iso);
        return Dates.addDays(iso, -((Dates.dayOfWeek(iso) + 1) % 7));
    }

    public static final List<String> DATE_FORMATS = Collections.unmodifiableList(Arrays.asList("MM/DD/YYYY", "DD/MM/YYYY", "DD.MM.YYYY", "YYYY-MM-DD"));

    /** Money with the region's decimal mark ("€3,50" with a decimal comma). */
    public static String money(String currency, double v, boolean decimalComma) {
        String s = Cost.money(currency, v);
        return decimalComma ? s.replace('.', ',') : s;
    }

    // ---------------- ingredient names ----------------

    /**
     * Preferred names: {US, CA, UK & IE / EU, ANZ}. Display only; keys are unchanged. Every name in a row is a
     * Normalizer synonym of the others (tested), so search, pantry, matching and swaps accept any of them.
     */
    static final String[][] TERMS = {
            {"zucchini", "zucchini", "courgette", "zucchini"},
            {"zucchinis", "zucchinis", "courgettes", "zucchinis"},
            {"eggplant", "eggplant", "aubergine", "eggplant"},
            {"eggplants", "eggplants", "aubergines", "eggplants"},
            {"cilantro", "cilantro", "coriander", "coriander"},
            {"arugula", "arugula", "rocket", "rocket"},
            {"superfine sugar", "superfine sugar", "caster sugar", "caster sugar"},
            {"powdered sugar", "icing sugar", "icing sugar", "icing sugar"},
            {"all-purpose flour", "all-purpose flour", "plain flour", "plain flour"},
            {"self-rising flour", "self-rising flour", "self-raising flour", "self-raising flour"},
            {"heavy cream", "whipping cream", "double cream", "thickened cream"},
            {"baking soda", "baking soda", "bicarbonate of soda", "bicarb soda"},
            {"green onion", "green onion", "spring onion", "spring onion"},
            {"green onions", "green onions", "spring onions", "spring onions"},
            {"scallions", "green onions", "spring onions", "spring onions"},
            {"scallion", "green onion", "spring onion", "spring onion"},
            {"rutabaga", "rutabaga", "swede", "swede"},
            {"ground beef", "ground beef", "beef mince", "beef mince"},
            {"ground pork", "ground pork", "pork mince", "pork mince"},
            {"ground lamb", "ground lamb", "lamb mince", "lamb mince"},
            {"ground turkey", "ground turkey", "turkey mince", "turkey mince"},
            {"ground chicken", "ground chicken", "chicken mince", "chicken mince"},
            {"shrimp", "shrimp", "prawns", "prawns"},
            {"jumbo shrimp", "jumbo shrimp", "king prawns", "king prawns"},
            {"bell pepper", "bell pepper", "pepper", "capsicum"},
            {"bell peppers", "bell peppers", "peppers", "capsicums"},
            {"cornstarch", "cornstarch", "cornflour", "cornflour"},
            {"beet", "beet", "beetroot", "beetroot"},
            {"beets", "beets", "beetroot", "beetroot"},
            {"snow peas", "snow peas", "mangetout", "snow peas"},
            {"cookies", "cookies", "biscuits", "biscuits"},
            {"heavy whipping cream", "whipping cream", "double cream", "thickened cream"},
            {"confectioners sugar", "icing sugar", "icing sugar", "icing sugar"},
            {"golden raisins", "golden raisins", "sultanas", "sultanas"},
            {"canola oil", "canola oil", "rapeseed oil", "canola oil"},
            {"broiler", "broiler", "grill", "grill"},
            {"plastic wrap", "plastic wrap", "cling film", "cling wrap"},
            {"parchment paper", "parchment paper", "baking paper", "baking paper"},
            {"skillet", "skillet", "frying pan", "frying pan"},
            {"molasses", "molasses", "black treacle", "molasses"},
            {"light corn syrup", "corn syrup", "golden syrup", "golden syrup"},
            {"sweetcorn", "corn", "sweetcorn", "corn"},
            {"cantaloupe", "cantaloupe", "melon", "rockmelon"}};

    /** Words that are kitchen words rather than ingredient names; only used in steps, never in pantry labels. */
    static final java.util.Set<String> STEP_ONLY = new java.util.HashSet<String>(Arrays.asList("broiler", "plastic wrap", "parchment paper", "skillet"));

    static int column(String region) {
        if (US.equals(region)) return 0;
        if (CA.equals(region)) return 1;
        if (UK.equals(region) || EU.equals(region) || OTHER.equals(region)) return 2;
        if (ANZ.equals(region)) return 3;
        return 0;
    }

    private static final Map<String, Pattern> PATTERNS = new java.util.HashMap<String, Pattern>();
    private static final Map<String, Map<String, String>> MAPS = new java.util.HashMap<String, Map<String, String>>();

    /** Lower-case source term -> preferred term for a region and use (ingredients or steps). Built once. */
    static synchronized Map<String, String> map(String region, boolean steps) {
        String k = column(region) + (steps ? "s" : "i");
        Map<String, String> m = MAPS.get(k);
        if (m != null) return m;
        m = new LinkedHashMap<String, String>();
        int c = column(region);
        // longest source phrases first so "heavy whipping cream" beats "heavy cream"
        List<String[]> pairs = new ArrayList<String[]>();
        for (String[] row : TERMS) {
            if (!steps && STEP_ONLY.contains(row[0])) continue;
            for (int j = 0; j < row.length; j++) if (j != c && !row[j].equals(row[c])) pairs.add(new String[]{row[j], row[c]});
        }
        Collections.sort(pairs, new java.util.Comparator<String[]>() {
            public int compare(String[] a, String[] b) { return b[0].length() != a[0].length() ? b[0].length() - a[0].length() : a[0].compareTo(b[0]); }
        });
        for (String[] p : pairs) if (!m.containsKey(p[0])) m.put(p[0], p[1]);
        // ambiguous in one direction: "pepper" (UK bell pepper) and "corn"/"grill"/"stone"/"melon"/"biscuits" are
        // also ordinary words, so they are never rewritten into another region's term
        for (String amb : new String[]{"pepper", "peppers", "corn", "grill", "stone", "melon", "biscuits", "coriander"}) m.remove(amb);
        MAPS.put(k, m);
        StringBuilder alt = new StringBuilder();
        for (String s : m.keySet()) { if (alt.length() > 0) alt.append('|'); alt.append(Pattern.quote(s)); }
        PATTERNS.put(k, alt.length() == 0 ? null : Pattern.compile("(?<![A-Za-z])(" + alt + ")(?![A-Za-z])", Pattern.CASE_INSENSITIVE));
        return m;
    }

    /**
     * An ingredient name (or step text, with steps=true) using the region's preferred terms; case of the first letter
     * and all-caps words are kept. Text with nothing to change comes back as the same object (cheap on the HD 8).
     */
    public static String localize(String text, String region, boolean steps) {
        if (text == null || text.isEmpty() || valid(region).isEmpty()) return text;
        Map<String, String> m = map(region, steps);
        Pattern p;
        synchronized (Region.class) { p = PATTERNS.get(column(region) + (steps ? "s" : "i")); }
        if (p == null) return text;
        Matcher mt = p.matcher(text);
        if (!mt.find()) return text;
        StringBuffer sb = new StringBuffer();
        do {
            String src = mt.group(1), rep = m.get(src.toLowerCase(Locale.ROOT));
            if (rep == null) rep = src;
            else if (src.equals(src.toUpperCase(Locale.ROOT)) && src.length() > 1) rep = rep.toUpperCase(Locale.ROOT);
            else if (Character.isUpperCase(src.charAt(0))) rep = Character.toUpperCase(rep.charAt(0)) + rep.substring(1);
            mt.appendReplacement(sb, Matcher.quoteReplacement(rep));
        } while (mt.find());
        mt.appendTail(sb);
        return sb.toString();
    }

    /** All rows of names, for tests and the "names used in your region" help list. */
    public static String[][] terms() { return TERMS.clone(); }

    // ---------------- holidays (the calendar itself is computed offline in a later stage) ----------------

    /** Holiday checklist ids switched on by default per region (Settings > Holidays lets the user change them). */
    public static List<String> holidayDefaults(String region) {
        String r = valid(region).isEmpty() ? US : region;
        List<String> common = Arrays.asList("new-year", "valentines", "easter", "halloween", "christmas-eve", "christmas", "new-years-eve");
        List<String> out = new ArrayList<String>(common);
        if (US.equals(r)) out.addAll(Arrays.asList("super-bowl", "st-patricks", "mothers-day-us", "memorial-day", "fathers-day", "july-4", "labor-day", "thanksgiving-us"));
        if (CA.equals(r)) out.addAll(Arrays.asList("pancake-day", "mothers-day-us", "victoria-day", "canada-day", "fathers-day", "thanksgiving-ca", "boxing-day"));
        if (UK.equals(r)) out.addAll(Arrays.asList("burns-night", "pancake-day", "mothering-sunday", "st-patricks", "easter-monday", "fathers-day", "bonfire-night", "boxing-day"));
        if (EU.equals(r)) out.addAll(Arrays.asList("epiphany", "carnival", "easter-monday", "midsummer", "bastille-day", "st-nicholas", "boxing-day"));
        if (ANZ.equals(r)) out.addAll(Arrays.asList("australia-day", "waitangi-day", "anzac-day", "easter-monday", "mothers-day-us", "fathers-day-anz", "boxing-day"));
        if (OTHER.equals(r)) out.addAll(Arrays.asList("lunar-new-year", "diwali", "eid-al-fitr"));
        return out;
    }
}
