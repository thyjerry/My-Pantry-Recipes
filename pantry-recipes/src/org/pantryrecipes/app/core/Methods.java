package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * v1.7 cooking-method tags worked out from a recipe's title and steps (no stored data, so imported and
 * online recipes get them too). Plain substring rules, mirrored exactly in js/core17.js.
 */
public final class Methods {
    private Methods() { }

    public static final String INSTANT_POT = "instant-pot", SLOW_COOKER = "slow-cooker", AIR_FRYER = "air-fryer",
            ONE_POT = "one-pot", OVEN = "oven", STOVETOP = "stovetop", NO_COOK = "no-cook";

    public static final List<String> IDS = Collections.unmodifiableList(java.util.Arrays.asList(
            INSTANT_POT, SLOW_COOKER, AIR_FRYER, ONE_POT, OVEN, STOVETOP, NO_COOK));

    public static String label(String id) {
        if (INSTANT_POT.equals(id)) return "Instant Pot";
        if (SLOW_COOKER.equals(id)) return "Slow cooker";
        if (AIR_FRYER.equals(id)) return "Air fryer";
        if (ONE_POT.equals(id)) return "One-pot";
        if (OVEN.equals(id)) return "Oven";
        if (STOVETOP.equals(id)) return "Stovetop";
        if (NO_COOK.equals(id)) return "No-cook";
        return id;
    }

    private static final String[] PAN_WORDS = {"saucepan", "frying pan", "skillet", "wok", "griddle", "stockpot", "dutch oven",
            "pot of", "large pot", "a pot", "the pot over", "pan over", "over medium", "over high", "over low", "on the hob",
            "on the stove", "simmer", "boil", " pan", " fry", "fry ", "saute", "off the heat", "toast "};

    private static boolean any(String s, String[] words) {
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }

    private static boolean asciiLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }

    /** Whether the word appears with non-letters (or the ends) on both sides. */
    static boolean word(String s, String w) {
        int i = s.indexOf(w);
        while (i >= 0) {
            boolean l = i == 0 || !asciiLetter(s.charAt(i - 1));
            int e = i + w.length();
            boolean r = e >= s.length() || !asciiLetter(s.charAt(e));
            if (l && r) return true;
            i = s.indexOf(w, i + 1);
        }
        return false;
    }

    /** Method ids in IDS order. */
    public static List<String> of(Recipe r) {
        String title = r.title == null ? "" : r.title.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(title);
        for (String s : r.steps) sb.append(" \n ").append(s == null ? "" : s.toLowerCase(Locale.ROOT));
        String all = sb.toString();
        // the pot itself is not a "pot of water" for the stovetop rule
        String steps = all.substring(title.length());
        boolean ip = all.contains("instant pot") || all.contains("pressure cook") || all.contains("pressure-cook")
                || all.contains("pressure cooker") || all.contains("high pressure");
        boolean slow = all.contains("slow cooker") || all.contains("slow-cooker") || all.contains("crockpot") || all.contains("crock pot");
        boolean air = all.contains("air fryer") || all.contains("air fry") || all.contains("air-fry");
        String ovenText = steps.replace("dutch oven", "pot").replace("microwave oven", "microwave");
        boolean oven = word(ovenText, "oven") || ovenText.contains("broiler") || ovenText.contains("under the grill") || title.contains("baked") || title.contains("roast") || title.contains("sheet pan")
                || title.contains("sheet-pan") || title.contains("traybake");
        String stoveText = steps.replace("dutch oven", "pot");
        boolean stove;
        if (ip || slow) stove = stoveText.contains("saucepan") || stoveText.contains("frying pan") || stoveText.contains("skillet")
                || stoveText.contains("wok");
        else stove = any(stoveText, PAN_WORDS);
        boolean onePot = title.contains("one-pot") || title.contains("one pot") || title.contains("one-pan") || title.contains("one pan")
                || ip || slow;
        boolean noCook = r.cookMin == 0 && !ip && !slow && !air && !oven;
        List<String> out = new ArrayList<String>();
        if (ip) out.add(INSTANT_POT);
        if (slow) out.add(SLOW_COOKER);
        if (air) out.add(AIR_FRYER);
        if (onePot) out.add(ONE_POT);
        if (oven) out.add(OVEN);
        if (stove && !noCook) out.add(STOVETOP);
        if (noCook) out.add(NO_COOK);
        // a cooked recipe that names no appliance is cooked on the hob
        if (out.isEmpty() && r.cookMin != 0) out.add(STOVETOP);
        return out;
    }

    public static boolean has(Recipe r, String id) { return of(r).contains(id); }

    /** True when the recipe has every method in the filter (an empty filter matches everything). */
    public static boolean matches(Recipe r, java.util.Collection<String> filter) {
        if (filter == null || filter.isEmpty()) return true;
        List<String> m = of(r);
        for (String f : filter) if (!m.contains(f)) return false;
        return true;
    }

    // ---------------------------------------------------------------- Instant Pot cheat sheet (shared with web)
    public static final String IP_DISCLAIMER = "Approximate times for an electric pressure cooker (such as an Instant Pot) at High pressure. "
            + "Times vary by model, size, altitude, amount and how big the pieces are, so check the manufacturer manual for your cooker "
            + "and its minimum liquid and maximum fill lines. Cook meat to a safe internal temperature.";

    public static final String[] IP_TIPS = {
            "Build-up time: the cooker takes about 10\u201315 minutes (longer when full or frozen) to come up to pressure. That time is not part of the cook time shown in recipes.",
            "Natural release (NR): leave the valve closed and let the pressure drop on its own (often 10\u201325 min). \u201c10 min NR\u201d means wait 10 minutes, then release the rest.",
            "Quick release (QR): turn the valve to venting, keeping hands and face away from the steam.",
            "Liquid: use at least the minimum your manual states (often about 1 cup / 250 ml for a 6-quart pot). Thick sauces can trigger a burn warning, so add them after cooking or layer them on top.",
            "Fill: no more than \u2154 full, or \u00bd full for beans, grains and other foods that foam.",
            "Foamy foods (beans, oats, grains, pasta): use natural release so foam does not spit from the valve.",
            "Altitude: above about 600 m (2,000 ft), cook times are often increased by about 5% per 300 m (1,000 ft). Check your manual.",
            "Never force the lid open; it unlocks when the float valve drops."};

    /** {food, amount/liquid hint, minutes at High pressure, release}. */
    public static final String[][] IP_TIMES = {
            {"White rice (long grain, rinsed)", "1 : 1 water", "4", "10 min NR"},
            {"Basmati / jasmine rice (rinsed)", "1 : 1 water", "4\u20135", "10 min NR"},
            {"Brown rice", "1 : 1\u00bc water", "22", "10 min NR"},
            {"Quinoa (rinsed)", "1 : 1\u00bc water", "1", "10 min NR"},
            {"Steel-cut oats", "1 : 3 water", "10", "10\u201315 min NR"},
            {"Rolled oats", "1 : 2 water", "3", "10 min NR"},
            {"Pearl barley", "1 : 2\u00bd water", "20", "10 min NR"},
            {"Pasta (dried)", "Just cover with water", "Half the packet time", "QR"},
            {"Black beans, soaked / unsoaked", "Cover by 5 cm (2 in)", "8\u201310 / 22\u201325", "Full NR"},
            {"Chickpeas, soaked / unsoaked", "Cover by 5 cm (2 in)", "12\u201315 / 35\u201340", "Full NR"},
            {"Red kidney beans, soaked / unsoaked", "Cover by 5 cm (2 in)", "10\u201312 / 25\u201330", "Full NR"},
            {"Brown or green lentils", "1 : 2 water", "8\u201310", "10 min NR"},
            {"Red lentils (dal)", "1 : 3 water", "5", "10 min NR"},
            {"Split peas", "1 : 3 water", "10\u201315", "Full NR"},
            {"Chicken breast, boneless (fresh / frozen)", "1 cup (250 ml) liquid", "6\u20138 / 10\u201312", "5\u201310 min NR"},
            {"Chicken thighs, boneless / bone-in", "1 cup (250 ml) liquid", "8\u201310 / 12\u201315", "10 min NR"},
            {"Whole chicken", "1 cup (250 ml) liquid, on the trivet", "6 per 450 g (1 lb)", "15 min NR"},
            {"Beef stew chunks (chuck)", "1 cup (250 ml) liquid", "30\u201335", "10\u201315 min NR"},
            {"Beef chuck roast, whole (1.4 kg / 3 lb)", "1 cup (250 ml) liquid", "60\u201375", "15 min NR"},
            {"Pork shoulder chunks", "1 cup (250 ml) liquid", "45\u201360", "15 min NR"},
            {"Pork ribs", "1 cup (250 ml) liquid, on the trivet", "25", "10 min NR"},
            {"Ground beef or turkey (thawed)", "\u00bd cup (125 ml) liquid", "5", "QR"},
            {"Bone broth / stock", "Up to the max line", "90\u2013120", "Full NR"},
            {"Potatoes, 4 cm (1\u00bd in) chunks / whole medium", "1 cup (250 ml) water, on the trivet", "4\u20136 / 10\u201312", "QR"},
            {"Sweet potatoes, whole medium", "1 cup (250 ml) water, on the trivet", "15\u201318", "10 min NR"},
            {"Carrots, sliced", "1 cup (250 ml) water", "2\u20133", "QR"},
            {"Broccoli or cauliflower florets", "1 cup (250 ml) water, in a steamer basket", "0\u20131", "QR"},
            {"Beetroot, whole medium", "1 cup (250 ml) water, on the trivet", "20\u201325", "10 min NR"},
            {"Eggs, hard-cooked", "1 cup (250 ml) water, on the trivet", "5", "5 min NR, then ice bath"},
            {"Cheesecake (15 cm / 6 in pan)", "1 cup (250 ml) water, on the trivet", "30\u201335", "10 min NR"}};
}
