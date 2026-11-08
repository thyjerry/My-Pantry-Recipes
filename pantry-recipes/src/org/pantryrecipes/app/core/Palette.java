package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Colour themes shared by the Android and web apps: 130 presets in 12 groups (v2.0 adds 81 "Game day" presets) (Light, Dark and 47 others), user-made custom
 * themes, and the derivation of every UI colour from five base colours. Derived text colours are nudged until
 * they meet WCAG AA contrast (4.5:1), so even a careless custom theme stays readable.
 * Colours are 0xRRGGBB ints (no alpha).
 */
public final class Palette {
    private Palette() { }

    public static final String LIGHT = "light", DARK = "dark", SYSTEM = "system";
    public static final int MAX_CUSTOM = 12;
    public static final double AA = 4.5, AA_LARGE = 3.0;

    /** The five colours a theme is made of, plus the gallery group it is listed under (empty for custom themes). */
    public static final class Base {
        public final String id, name, group;
        public final int bg, surface, primary, accent, text;
        public Base(String id, String name, int bg, int surface, int primary, int accent, int text) {
            this(id, name, "", bg, surface, primary, accent, text);
        }
        public Base(String id, String name, String group, int bg, int surface, int primary, int accent, int text) {
            this.id = id; this.name = name; this.group = group == null ? "" : group;
            this.bg = bg & 0xFFFFFF; this.surface = surface & 0xFFFFFF; this.primary = primary & 0xFFFFFF;
            this.accent = accent & 0xFFFFFF; this.text = text & 0xFFFFFF;
        }
        public Base withColors(int bg, int surface, int primary, int accent, int text) {
            return new Base(id, name, group, bg, surface, primary, accent, text);
        }
        public Base named(String id, String name) { return new Base(id, name, "", bg, surface, primary, accent, text); }
    }

    /** Gallery groups, in display order. */
    public static final List<String> GROUPS = Collections.unmodifiableList(Arrays.asList(
            "Light", "Dark", "Colorful", "Pastel", "High contrast", "Warm", "Cool", "Earthy", "AMOLED black",
            "Paper & sepia", "Editor classics", "Bold bright", "Bold deep", "Game day"));

    private static Base p(String group, String id, String name, int bg, int surface, int primary, int accent, int text) {
        return new Base(id, name, group, bg, surface, primary, accent, text);
    }

    /** Built-in themes. The first 17 ids are the v1.5 ones (kept so saved choices and backups still resolve). */
    public static final List<Base> PRESETS = Collections.unmodifiableList(Arrays.asList(
            p("Light", LIGHT, "Light", 0xF5F7F2, 0xFFFFFF, 0x2E9E5B, 0xD9822B, 0x1E2A22),
            p("Dark", DARK, "Dark", 0x121814, 0x1C2620, 0x5BC98A, 0xF0A35E, 0xE6EEE8),
            p("Cool", "ocean", "Ocean", 0xCEDFEE, 0xEBF2F8, 0x1F6FB2, 0xE07A3F, 0x13263A),
            p("Dark", "midnight", "Midnight", 0x0E1424, 0x182036, 0x7AA2FF, 0xFFB86B, 0xE4E9F5),
            p("Warm", "sunset", "Sunset", 0xF2D7D1, 0xFAEFEC, 0xC2492C, 0x7B4FA0, 0x3A1E16),
            p("Pastel", "lavender", "Lavender", 0xE7E2F3, 0xF8F6FB, 0x6A4BB5, 0xD6467A, 0x241B35),
            p("Warm", "rose", "Rose", 0xEED1DB, 0xF8ECF0, 0xB0305A, 0x2F8F83, 0x3A1822),
            p("Earthy", "espresso", "Espresso", 0x1B1411, 0x2A201B, 0xD9A066, 0x8FC1A9, 0xF2E8DF),
            p("Cool", "slate", "Slate", 0xD5DAE0, 0xEEF0F2, 0x3F5873, 0xC8553D, 0x1D2731),
            p("Dark", "forest", "Forest night", 0x0F1C15, 0x17291F, 0x7FD39B, 0xE8C15A, 0xE3F1E7),
            p("Colorful", "citrus", "Citrus", 0xFFF3C4, 0xFFFFFF, 0x8A9A10, 0xE2711D, 0x2B2A10),
            p("Pastel", "mint", "Mint", 0xD9ECE9, 0xF3F9F8, 0x0F8A74, 0xE65A6B, 0x12302A),
            p("Dark", "plum", "Plum", 0x1A1020, 0x281A30, 0xD08CE8, 0x7FD1C7, 0xF1E6F5),
            p("High contrast", "contrast", "High contrast", 0xFFFFFF, 0xFFFFFF, 0x0B5D1E, 0x9A3412, 0x000000),
            p("High contrast", "contrast-dark", "High contrast dark", 0x000000, 0x0A0A0A, 0x7CFC9A, 0xFFD166, 0xFFFFFF),
            p("Earthy", "terracotta", "Terracotta", 0xE8D8D2, 0xF7F1EF, 0x8E3B1E, 0x3E7C59, 0x2E1A12),
            p("Editor classics", "arctic", "Arctic night", 0x2E3440, 0x3B4252, 0x88C0D0, 0xEBCB8B, 0xECEFF4),
            // ---- added in 1.6 ----
            p("Light", "cloud", "Cloud", 0xD8E0F8, 0xF1F4FD, 0x3D63DD, 0xE8833A, 0x1B2230),
            p("Light", "daylight", "Daylight", 0xFAFAF7, 0xFFFFFF, 0x3F3FBF, 0xD9534F, 0x1F2526),
            p("Light", "sage", "Sage leaf", 0xDEE5DB, 0xF4F6F2, 0x5B7F4A, 0xB5654A, 0x232B1F),
            p("Light", "porcelain", "Porcelain", 0xDBDEE8, 0xF2F3F7, 0x4A5A8C, 0xC07A2C, 0x222326),
            p("Dark", "graphite", "Graphite", 0x18191B, 0x232528, 0x8AB4F8, 0xF6AE5B, 0xE8EAED),
            p("Dark", "deepsea", "Deep sea", 0x0B1A22, 0x132833, 0x4FD1C5, 0xFF9F7A, 0xE2F1F4),
            p("Colorful", "tropical", "Tropical", 0xBDE0DD, 0xDEF0EE, 0x00897B, 0xF4511E, 0x0F2E2A),
            p("Colorful", "berrypop", "Berry pop", 0xEFC3D4, 0xF7E1EA, 0xC2185B, 0x5E35B1, 0x2D0F1E),
            p("Colorful", "lagoon", "Lagoon", 0xBDDCEE, 0xDEEDF6, 0x0277BD, 0xEF6C00, 0x0B2233),
            p("Colorful", "mango", "Mango", 0xF3D9BD, 0xF9ECDE, 0xD26E00, 0x2E7D32, 0x2E1E05),
            p("Pastel", "peach", "Peach blossom", 0xF9E7E2, 0xFDF7F6, 0xD8674A, 0x6B8F71, 0x3A2420),
            p("Pastel", "cottoncandy", "Cotton candy", 0xF3E2EF, 0xFBF6FA, 0xB4479B, 0x3F8FC4, 0x33182E),
            p("Pastel", "skypastel", "Sky pastel", 0xE2EBF5, 0xF6F9FC, 0x4C7FC0, 0xE88B9A, 0x1C2838),
            p("High contrast", "contrast-yellow", "High contrast amber", 0x000000, 0x000000, 0xFFD400, 0x7FDBFF, 0xFFFFFF),
            p("Warm", "amber", "Amber glow", 0xEED9C9, 0xF8F0E9, 0xB45309, 0x0F766E, 0x2B1B07),
            p("Warm", "cinnamon", "Cinnamon", 0x2A1A14, 0x38241C, 0xF0915A, 0xE7C66B, 0xF7E9E1),
            p("Cool", "glacier", "Glacier", 0xCCE2E6, 0xEAF3F5, 0x167A8F, 0x9A4FC6, 0x102A30),
            p("Cool", "steel", "Steel", 0x1C232B, 0x26303A, 0x6CC4E8, 0xFF8F70, 0xE6EDF3),
            p("Earthy", "moss", "Moss", 0x1A1F14, 0x252C1D, 0xA8C46A, 0xE0A458, 0xEDF0E4),
            p("Earthy", "clay", "Clay", 0xEBDCD7, 0xF8F3F1, 0x9C4F35, 0x5A7A4E, 0x2D1E17),
            p("Earthy", "olive", "Olive grove", 0xE1E1D2, 0xF5F5EF, 0x6B6B1E, 0xA0522D, 0x26261A),
            p("AMOLED black", "black", "Pure black", 0x000000, 0x0D0D0D, 0x5EEAD4, 0xFF9E80, 0xF2F2F2),
            p("AMOLED black", "black-blue", "Black sapphire", 0x000000, 0x0B0D12, 0x82AAFF, 0xFFCB6B, 0xEEF1F8),
            p("AMOLED black", "black-ember", "Black ember", 0x000000, 0x100B0A, 0xFF8A65, 0xFFD166, 0xF5ECE8),
            p("AMOLED black", "black-violet", "Black violet", 0x000000, 0x0E0B12, 0xC59BFF, 0x7FE0C7, 0xF1ECF8),
            p("Paper & sepia", "paper", "Paper", 0xF7F4EC, 0xFFFDF7, 0x5C4B3A, 0x2F6F8F, 0x2A251E),
            p("Paper & sepia", "sepia", "Sepia", 0xF1E7D0, 0xF8F1E0, 0x7A4E2D, 0x8A3D62, 0x3B2A1A),
            p("Paper & sepia", "sepia-night", "Night sepia", 0x1F1A14, 0x2B241B, 0xE0B67A, 0xE57F6A, 0xEFE3CF),
            p("Editor classics", "citrine-light", "Citrine light", 0xFDF6E3, 0xFFFBEF, 0x1F6E9C, 0xB8460F, 0x073642),
            p("Editor classics", "citrine-dark", "Citrine dark", 0x002B36, 0x073642, 0x5FB3E8, 0xE3A33B, 0xEEE8D5),
            p("Editor classics", "frostbite", "Frostbite", 0xECEFF4, 0xFFFFFF, 0x4C6A92, 0xBF616A, 0x2E3440),
            p("Editor classics", "nightshade", "Nightshade", 0x22212C, 0x2E2D3D, 0xBD93F9, 0xFF79C6, 0xF8F8F2),

            // ---- v2.2: bold backgrounds (tools/themes/gen22.py) ----
            p("Bold bright", "bold-saffron", "Saffron", 0xFFC93C, 0xFFE7A7, 0x2D2A8C, 0xB3261E, 0x141414),
            p("Bold bright", "bold-coral", "Coral reef", 0xFF8A65, 0xFFCABA, 0x123C69, 0x00695C, 0x141414),
            p("Bold bright", "bold-aqua", "Aqua splash", 0x4FE3CF, 0xB0F2E9, 0x5B1A8C, 0xB3261E, 0x141414),
            p("Bold bright", "bold-lime", "Lime zest", 0xC6F432, 0xE5FAA3, 0x5A1E8C, 0x0D47A1, 0x141414),
            p("Bold bright", "bold-bubblegum", "Bubblegum", 0xFF9BCB, 0xFFD2E8, 0x0B4F6C, 0x5B2C00, 0x141414),
            p("Bold bright", "bold-sky", "Sky blue", 0x7CC8FF, 0xC4E6FF, 0x8C1D40, 0x4A3B00, 0x141414),
            p("Bold bright", "bold-tangerine", "Tangerine", 0xFFA53D, 0xFFD6A8, 0x1B3A8C, 0x6A1B5A, 0x141414),
            p("Bold bright", "bold-lilac", "Lilac", 0xC9A7FF, 0xE7D7FF, 0x1F5130, 0x7A2E00, 0x141414),
            p("Bold bright", "bold-mint", "Fresh mint", 0x7EF0B0, 0xC5F8DB, 0x6A1B4D, 0x0D3B8C, 0x141414),
            p("Bold bright", "bold-lemon", "Lemonade", 0xFFF06A, 0xFFF8BC, 0x7A1F3D, 0x0B4F6C, 0x141414),
            p("Bold bright", "bold-peach", "Peach fizz", 0xFFB38A, 0xFFDDCA, 0x214A6B, 0x5E2A84, 0x141414),
            p("Bold bright", "bold-turquoise", "Turquoise", 0x3DD6E0, 0xA8EDF1, 0x7A1F1F, 0x3E2A84, 0x141414),
            p("Bold deep", "deep-cobalt", "Cobalt", 0x163C9E, 0x2D50A8, 0xFFC145, 0x7CF0C8, 0xFFFFFF),
            p("Bold deep", "deep-emerald", "Emerald", 0x0B5E3C, 0x236E50, 0xFF9E6B, 0xFFE07A, 0xFFFFFF),
            p("Bold deep", "deep-crimson", "Crimson", 0x8E1B2C, 0x993241, 0x7FE3D0, 0xFFD166, 0xFFFFFF),
            p("Bold deep", "deep-violet", "Violet", 0x4B1F8C, 0x5D3598, 0xC6F432, 0xFF9BCB, 0xFFFFFF),
            p("Bold deep", "deep-teal", "Deep teal", 0x07595C, 0x206A6C, 0xFFB4A2, 0xFFE38A, 0xFFFFFF),
            p("Bold deep", "deep-indigo", "Indigo night", 0x26236B, 0x3C397A, 0xFF8FB1, 0x8EF0E0, 0xFFFFFF),
            p("Bold deep", "deep-burgundy", "Burgundy", 0x5C1030, 0x6C2845, 0x9BE7FF, 0xF7D774, 0xFFFFFF),
            p("Bold deep", "deep-forest", "Pine", 0x1F4D2B, 0x355F40, 0xFFB7D5, 0xFFE082, 0xFFFFFF),
            p("Bold deep", "deep-navy", "Navy", 0x0F2A52, 0x273F63, 0xFF9F43, 0x7FE7FF, 0xFFFFFF),
            p("Bold deep", "deep-plum", "Plum wine", 0x4E1442, 0x602C55, 0xA8F0B5, 0xFFC48A, 0xFFFFFF),
            p("Bold deep", "deep-rust", "Rust", 0x7A2E12, 0x87432A, 0x8FD8FF, 0xFFE48A, 0xFFFFFF),
            p("Bold deep", "deep-ocean", "Ocean deep", 0x053F6E, 0x1E527C, 0xFFD36E, 0xFF9AA8, 0xFFFFFF),
            // v2.0 "Game day": generated by tools/GameDayGen.java (sports-style colour pairings, original names)
            p("Game day", "gd-gridiron-gold", "Gridiron Gold", 0xF7F5EF, 0xFFFFFF, 0x7D6B41, 0x101820, 0x201C11),
            p("Game day", "gd-gridiron-gold-dark", "Gridiron Gold dark", 0x15120B, 0x241F13, 0xB3995D, 0x6A6F74, 0xF7F5EF),
            p("Game day", "gd-harbor-orange-and-navy", "Harbor Orange & Navy", 0xFFEDE8, 0xFFFFFF, 0xC33D10, 0x002244, 0x2D0E04),
            p("Game day", "gd-harbor-orange-and-navy-dark", "Harbor Orange & Navy dark", 0x1E0902, 0x321004, 0xFB4F14, 0x596F85, 0xFFEDE8),
            p("Game day", "gd-crimson-and-cream", "Crimson & Cream", 0xF5E8EB, 0xFFFFFF, 0x9E1B32, 0x918A7E, 0x1C0509),
            p("Game day", "gd-crimson-and-cream-dark", "Crimson & Cream dark", 0x130306, 0x20050A, 0xBE6575, 0xF1E6D2, 0xF5E8EB),
            p("Game day", "gd-deep-field-green", "Deep Field Green", 0xE6EDEE, 0xFFFFFF, 0x004C54, 0x888E90, 0x000E0F),
            p("Game day", "gd-royal-and-gold", "Royal & Gold", 0xE6EBF6, 0xFFFFFF, 0x0033A0, 0xAC861E, 0x00091D),
            p("Game day", "gd-purple-and-gold", "Purple & Gold", 0xEEE9F3, 0xFFFFFF, 0x552583, 0xB7861C, 0x0F0718),
            p("Game day", "gd-teal-and-black", "Teal & Black", 0xE6F0F2, 0xFFFFFF, 0x006778, 0x101820, 0x001316),
            p("Game day", "gd-teal-and-black-dark", "Teal & Black dark", 0x000C0E, 0x001518, 0x398996, 0x64696E, 0xE6F0F2),
            p("Game day", "gd-scarlet-and-gray", "Scarlet & Gray", 0xF8E6E6, 0xFFFFFF, 0xBB0000, 0x666666, 0x220000),
            p("Game day", "gd-scarlet-and-gray-dark", "Scarlet & Gray dark", 0x160000, 0x250000, 0xD15353, 0x666666, 0xF8E6E6),
            p("Game day", "gd-burnt-orange", "Burnt Orange", 0xF9EEE6, 0xFFFFFF, 0xAC4E00, 0x333F48, 0x221000),
            p("Game day", "gd-burnt-orange-dark", "Burnt Orange dark", 0x170A00, 0x261100, 0xC5681A, 0x616A71, 0xF9EEE6),
            p("Game day", "gd-maroon-and-white", "Maroon & White", 0xF2E6E8, 0xFFFFFF, 0x7A0019, 0xAC8A22, 0x160005),
            p("Game day", "gd-forest-and-gold", "Forest & Gold", 0xE9EBEA, 0xFFFFFF, 0x203731, 0xB9840D, 0x060A09),
            p("Game day", "gd-cardinal-red", "Cardinal Red", 0xF5E9EC, 0xFFFFFF, 0x97233F, 0xB9840D, 0x1B060B),
            p("Game day", "gd-cardinal-red-dark", "Cardinal Red dark", 0x120408, 0x1E070D, 0xB66579, 0xFFB612, 0xF5E9EC),
            p("Game day", "gd-powder-blue-and-gold", "Powder Blue & Gold", 0xE6F2F9, 0xFFFFFF, 0x0070AD, 0xB3880A, 0x001724),
            p("Game day", "gd-powder-blue-and-gold-dark", "Powder Blue & Gold dark", 0x000F18, 0x001A28, 0x138ACA, 0xFFC20E, 0xE6F2F9),
            p("Game day", "gd-silver-and-black", "Silver & Black", 0xEFF0F1, 0xFFFFFF, 0x5F6A72, 0x000000, 0x111315),
            p("Game day", "gd-silver-and-black-dark", "Silver & Black dark", 0x0B0D0E, 0x131517, 0x7B848B, 0x6C6C6C, 0xEFF0F1),
            p("Game day", "gd-navy-and-red", "Navy & Red", 0xE7E9EC, 0xFFFFFF, 0x0C2340, 0xC8102E, 0x02060C),
            p("Game day", "gd-kelly-green", "Kelly Green", 0xE6F2EB, 0xFFFFFF, 0x007A33, 0xAC8B4D, 0x001609),
            p("Game day", "gd-kelly-green-dark", "Kelly Green dark", 0x000F06, 0x00180A, 0x2D9157, 0xBA9653, 0xE6F2EB),
            p("Game day", "gd-brick-and-navy", "Brick & Navy", 0xF6E8EA, 0xFFFFFF, 0xA6192E, 0x13274F, 0x1E0508),
            p("Game day", "gd-brick-and-navy-dark", "Brick & Navy dark", 0x140306, 0x210509, 0xC15E6D, 0x5A6884, 0xF6E8EA),
            p("Game day", "gd-garnet-and-gold", "Garnet & Gold", 0xF2EAEC, 0xFFFFFF, 0x782F40, 0x9B8A66, 0x16080C),
            p("Game day", "gd-orange-and-blue", "Orange & Blue", 0xFDF1E9, 0xFFFFFF, 0xA85619, 0x0C2C84, 0x2A1506),
            p("Game day", "gd-orange-and-blue-dark", "Orange & Blue dark", 0x1C0E04, 0x2E1807, 0xE87722, 0x6F8FD8, 0xFDF1E9),
            p("Game day", "gd-sky-and-navy", "Sky & Navy", 0xF0F7FC, 0xFFFFFF, 0x467094, 0x041E42, 0x131F29),
            p("Game day", "gd-sky-and-navy-dark", "Sky & Navy dark", 0x0D151B, 0x16222E, 0x6CACE4, 0x627289, 0xF0F7FC),
            p("Game day", "gd-ice-blue-and-navy", "Ice Blue & Navy", 0xE6E9EF, 0xFFFFFF, 0x00205B, 0x67929E, 0x000610),
            p("Game day", "gd-pitch-green", "Pitch Green", 0xEBF2EC, 0xFFFFFF, 0x377741, 0x8C8B85, 0x0A170C),
            p("Game day", "gd-pitch-green-dark", "Pitch Green dark", 0x070F08, 0x0C190E, 0x589160, 0xF4F1E8, 0xEBF2EC),
            p("Game day", "gd-steel-and-gold", "Steel & Gold", 0xEAECEE, 0xFFFFFF, 0x3E4C59, 0xC9A227, 0x080B0E),
            p("Game day", "gd-hornet-teal-and-purple", "Hornet Teal & Purple", 0xE6F2F4, 0xFFFFFF, 0x007589, 0x1D1160, 0x001619),
            p("Game day", "gd-lakefront-navy-and-orange", "Lakefront Navy & Orange", 0xE7E8EA, 0xFFFFFF, 0x0B162A, 0xC83803, 0x020408),
            p("Game day", "gd-copper-and-black", "Copper & Black", 0xF8F1EB, 0xFFFFFF, 0xA34E24, 0x4A4A4A, 0x211509),
            p("Game day", "gd-copper-and-black-dark", "Copper & Black dark", 0x160E06, 0x25170A, 0xBA7738, 0x9A9A9A, 0xF8F1EB),
            p("Game day", "gd-cobalt-and-silver", "Cobalt & Silver", 0xE6EDF7, 0xFFFFFF, 0x0047AB, 0x8A9193, 0x000D1F),
            p("Game day", "gd-emerald-and-white", "Emerald & White", 0xE6F3EC, 0xFFFFFF, 0x007D3A, 0x8C8C8C, 0x00180B),
            p("Game day", "gd-emerald-and-white-dark", "Emerald & White dark", 0x001007, 0x001A0C, 0x209355, 0xFFFFFF, 0xE6F3EC),
            p("Game day", "gd-plum-and-teal", "Plum & Teal", 0xEFEAF3, 0xFFFFFF, 0x5A2D82, 0x009FA9, 0x100817),
            p("Game day", "gd-blaze-orange-and-black", "Blaze Orange & Black", 0xFEEFE9, 0xFFFFFF, 0xBB461A, 0x111111, 0x2B1006),
            p("Game day", "gd-victory-red-and-royal", "Victory Red & Royal", 0xFAE7EC, 0xFFFFFF, 0xCE1141, 0x1D428A, 0x25030C),
            p("Game day", "gd-victory-red-and-royal-dark", "Victory Red & Royal dark", 0x190208, 0x29030D, 0xDA4D71, 0x4A68A1, 0xFAE7EC),
            p("Game day", "gd-frozen-pond-dark", "Frozen Pond dark", 0x09121A, 0x0F1D2C, 0x4B92DB, 0x6A7176, 0xEDF4FB),
            p("Game day", "gd-backcourt-blue-and-yellow", "Backcourt Blue & Yellow", 0xE8ECF3, 0xFFFFFF, 0x1D428A, 0xC9A000, 0x050C19),
            p("Game day", "gd-bayou-gold-and-black-dark", "Bayou Gold & Black dark", 0x191711, 0x2A261C, 0xD3BC8D, 0x707479, 0xFBF8F4),
            p("Game day", "gd-seafoam-and-navy", "Seafoam & Navy", 0xF0F9EA, 0xFFFFFF, 0x447C1A, 0x002244, 0x132207),
            p("Game day", "gd-seafoam-and-navy-dark", "Seafoam & Navy dark", 0x0D1705, 0x152608, 0x69BE28, 0x60758A, 0xF0F9EA),
            p("Game day", "gd-wine-and-gold", "Wine & Gold", 0xF3E6EB, 0xFFFFFF, 0x860038, 0xC8961E, 0x18000A),
            p("Game day", "gd-midnight-and-lime", "Midnight & Lime", 0xE9ECEF, 0xFFFFFF, 0x10263F, 0x639D1A, 0x02060C),
            p("Game day", "gd-cherry-and-black", "Cherry & Black", 0xFAE7EA, 0xFFFFFF, 0xC8102E, 0x000000, 0x240308),
            p("Game day", "gd-sunflower-and-navy", "Sunflower & Navy", 0xFFF8E7, 0xFFFFFF, 0x93690A, 0x13294B, 0x2E2103),
            p("Game day", "gd-sunflower-and-navy-dark", "Sunflower & Navy dark", 0x1F1602, 0x332404, 0xFFB612, 0x6C798F, 0xFFF8E7),
            p("Game day", "gd-tiger-orange-and-purple", "Tiger Orange & Purple", 0xFEF0E6, 0xFFFFFF, 0xB24A00, 0x522D80, 0x2C1200),
            p("Game day", "gd-tiger-orange-and-purple-dark", "Tiger Orange & Purple dark", 0x1D0C00, 0x311400, 0xF56600, 0x7D62A0, 0xFEF0E6),
            p("Game day", "gd-columbine-and-gray", "Columbine & Gray", 0xF5F8FB, 0xFFFFFF, 0x5D6E7F, 0x53565A, 0x1C2126),
            p("Game day", "gd-columbine-and-gray-dark", "Columbine & Gray dark", 0x131619, 0x1F252A, 0x9BB8D3, 0x717477, 0xF5F8FB),
            p("Game day", "gd-ember-and-charcoal", "Ember & Charcoal", 0xFBE7E7, 0xFFFFFF, 0xD00A0A, 0x34302B, 0x260202),
            p("Game day", "gd-spruce-and-silver", "Spruce & Silver", 0xE8EDEB, 0xFFFFFF, 0x154734, 0xA2ABA5, 0x040D09),
            p("Game day", "gd-lagoon-teal-and-orange-dark", "Lagoon Teal & Orange dark", 0x001112, 0x001C1E, 0x06919A, 0xFC4C02, 0xE6F4F5),
            p("Game day", "gd-mustard-and-brown", "Mustard & Brown", 0xFFF8E8, 0xFFFFFF, 0x936A10, 0x311D00, 0x2E2105),
            p("Game day", "gd-bronze-and-blue-dark", "Bronze & Blue dark", 0x110D07, 0x1C150C, 0x9A7E53, 0x7C8FB5, 0xF4F0EB),
            p("Game day", "gd-twilight-purple-and-black", "Twilight Purple & Black", 0xEDE9F3, 0xFFFFFF, 0x4F2683, 0x000000, 0x0E0718),
            p("Game day", "gd-electric-blue-and-red", "Electric Blue & Red", 0xE6F1F8, 0xFFFFFF, 0x0070AD, 0xD6001C, 0x001521),
            p("Game day", "gd-electric-blue-and-red-dark", "Electric Blue & Red dark", 0x000E16, 0x001824, 0x268BC1, 0xD6001C, 0xE6F1F8),
            p("Game day", "gd-clay-and-cream", "Clay & Cream", 0xF6EDEB, 0xFFFFFF, 0xA4463A, 0xB0A48C, 0x1E0D0A),
            p("Game day", "gd-vintage-mint-and-black", "Vintage Mint & Black", 0xECF8F3, 0xFFFFFF, 0x2A7A5C, 0x000000, 0x0B2019),
            p("Game day", "gd-vintage-mint-and-black-dark", "Vintage Mint & Black dark", 0x071610, 0x0C241B, 0x3EB489, 0x737373, 0xECF8F3),
            p("Game day", "gd-pine-and-orange", "Pine & Orange", 0xE6ECE8, 0xFFFFFF, 0x01411C, 0xDC6E32, 0x000C05),
            p("Game day", "gd-rose-and-gold", "Rose & Gold", 0xF8ECF1, 0xFFFFFF, 0xB0426B, 0xAB8A21, 0x210C14),
            p("Game day", "gd-rose-and-gold-dark", "Rose & Gold dark", 0x16080D, 0x240E16, 0xC26587, 0xC9A227, 0xF8ECF1),
            p("Game day", "gd-lake-blue-and-silver", "Lake Blue & Silver", 0xE9EFF4, 0xFFFFFF, 0x236192, 0xA2AAAD, 0x06111A),
            p("Game day", "gd-lake-blue-and-silver-dark", "Lake Blue & Silver dark", 0x040C12, 0x07131D, 0x5585AB, 0xC4CED4, 0xE9EFF4),
            p("Game day", "gd-canary-and-green-dark", "Canary & Green dark", 0x1F1900, 0x332900, 0xFFCD00, 0x5B7E71, 0xFFFAE6),
            p("Game day", "gd-magenta-and-teal", "Magenta & Teal", 0xF6E7F1, 0xFFFFFF, 0xA6116E, 0x00808F, 0x1E0314),
            p("Game day", "gd-magenta-and-teal-dark", "Magenta & Teal dark", 0x14020D, 0x210316, 0xC1589A, 0x00808F, 0xF6E7F1)));

    /** Presets in one gallery group, in list order. */
    public static List<Base> inGroup(String group) {
        List<Base> out = new ArrayList<Base>();
        for (Base b : PRESETS) if (b.group.equals(group)) out.add(b);
        return out;
    }

    /** Quick "accent only" picks that replace a theme's main colour (buttons, links, highlights). */
    public static final int[] QUICK_ACCENTS = {0x2E9E5B, 0x0F8A74, 0x0891B2, 0x1F6FB2, 0x4F46E5, 0x7C3AED,
            0xB4479B, 0xD6467A, 0xC62828, 0xE2711D, 0xB7791F, 0x8D5A3B, 0x3F5873};

    /** Text-size choices and their scale factors. */
    public static final String[] FONT_IDS = {"small", "normal", "large", "xlarge"};
    public static final String[] FONT_NAMES = {"Small", "Normal", "Large", "Extra large"};
    public static final double[] FONT_SCALES = {0.9, 1.0, 1.15, 1.3};
    /** Corner-roundness choices and their scale factors for corner radii. */
    public static final String[] CORNER_IDS = {"sharp", "normal", "round"};
    public static final String[] CORNER_NAMES = {"Sharp", "Normal", "Round"};
    public static final double[] CORNER_SCALES = {0.25, 1.0, 1.6};

    public static double fontScale(String id) { return scale(FONT_IDS, FONT_SCALES, id); }
    public static double cornerScale(String id) { return scale(CORNER_IDS, CORNER_SCALES, id); }

    private static double scale(String[] ids, double[] v, String id) {
        for (int k = 0; k < ids.length; k++) if (ids[k].equals(id)) return v[k];
        return 1.0;
    }

    private static boolean oneOf(String[] ids, String id) {
        for (String s : ids) if (s.equals(id)) return true;
        return false;
    }

    /** A preset id for "Surprise me": r in [0,1) picks uniformly among the presets other than {@code current}. */
    public static String randomPreset(String current, double r) {
        List<Base> pool = new ArrayList<Base>();
        for (Base b : PRESETS) if (!b.id.equals(current)) pool.add(b);
        int k = (int) Math.floor(Math.max(0, Math.min(0.999999, r)) * pool.size());
        return pool.get(k).id;
    }

    public static Base preset(String id) {
        id = migrateId(id);
        for (Base b : PRESETS) if (b.id.equals(id)) return b;
        return null;
    }

    /** v1.8: theme ids renamed (Solar light/dark became Citrine light/dark, Fjord day became Frostbite); old saved ids still work. */
    public static String migrateId(String id) {
        if (id == null) return null;
        if (id.equals("solar-light")) return "citrine-light";
        if (id.equals("solar-dark")) return "citrine-dark";
        if (id.equals("fjord")) return "frostbite";
        return id;
    }

    // ---------------- colour maths ----------------
    private static double channel(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    /** WCAG relative luminance. */
    public static double luminance(int rgb) {
        return 0.2126 * channel((rgb >> 16) & 0xFF) + 0.7152 * channel((rgb >> 8) & 0xFF) + 0.0722 * channel(rgb & 0xFF);
    }

    /** WCAG contrast ratio (1..21). */
    public static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** Mix a towards b by t (0 = a, 1 = b). */
    public static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static double minContrast(int fg, int[] bgs) {
        double m = 99;
        for (int b : bgs) m = Math.min(m, contrast(fg, b));
        return m;
    }

    /** fg moved towards {@code toward} in 5% steps until it reaches {@code min} against every background. */
    public static int ensure(int fg, int[] bgs, double min, int toward) {
        for (int k = 0; k <= 20; k++) {
            int c = mix(fg, toward, k / 20.0);
            if (minContrast(c, bgs) >= min) return c;
        }
        return toward;
    }

    private static int bw(int bg) { return contrast(0xFFFFFF, bg) >= contrast(0x000000, bg) ? 0xFFFFFF : 0x000000; }

    public static boolean isDark(int bg) { return luminance(bg) < 0.18; }

    // ---------------- derived colours ----------------
    /** Every colour the UI uses, derived from a Base. */
    public static final class Colors {
        public boolean dark;
        public int bg, surface, text, text2, text3, line;
        public int primary, primaryText, tint, tint2, selected, ripple;
        public int fill, onFill, onFill2, heroEnd;
        public int accent, accentText, accentTint;
        public int danger, info, infoTint, neutral;
        public boolean textAdjusted;

        /** name -> 0xRRGGBB, in a fixed order (used by tests and the web port). */
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("dark", dark);
            String[] n = {"bg", "surface", "text", "text2", "text3", "line", "primary", "primaryText", "tint", "tint2", "selected",
                    "fill", "onFill", "onFill2", "heroEnd", "accent", "accentText", "accentTint", "danger", "info", "infoTint", "neutral"};
            int[] v = {bg, surface, text, text2, text3, line, primary, primaryText, tint, tint2, selected,
                    fill, onFill, onFill2, heroEnd, accent, accentText, accentTint, danger, info, infoTint, neutral};
            for (int k = 0; k < n.length; k++) m.put(n[k], hex(v[k]));
            m.put("textAdjusted", textAdjusted);
            return m;
        }
    }

    public static Colors derive(Base b) {
        Colors c = new Colors();
        c.bg = b.bg;
        c.surface = b.surface;
        c.dark = isDark(b.bg);
        int[] base = {b.surface, b.bg};
        int extreme = bw(b.surface);
        c.text = ensure(b.text, base, AA, extreme);
        c.textAdjusted = c.text != b.text;
        c.text2 = ensure(mix(c.text, b.surface, 0.38), base, AA, c.text);
        c.text3 = ensure(mix(c.text, b.surface, 0.58), new int[]{b.surface, b.bg, mix(b.primary, b.surface, 0.94)}, AA, c.text); // v2.0: hints are small text, so AA (also on selected tiles)
        c.line = mix(c.text, b.surface, 0.86);
        c.neutral = mix(c.text, b.surface, 0.93);
        c.primary = b.primary;
        c.tint = mix(b.primary, b.surface, 0.84);
        c.tint2 = mix(b.primary, b.surface, 0.68);
        c.selected = mix(b.primary, b.surface, 0.94);
        c.primaryText = ensure(b.primary, new int[]{b.surface, b.bg, c.tint, c.selected}, AA, c.text);
        c.ripple = b.primary;
        // white button text whenever a slightly deeper shade of the primary can carry it (keeps the classic look)
        c.onFill = contrast(0xFFFFFF, b.primary) >= AA_LARGE ? 0xFFFFFF : 0x000000;
        int away = c.onFill == 0xFFFFFF ? 0x000000 : 0xFFFFFF;
        c.fill = ensure(b.primary, new int[]{c.onFill}, AA, away);
        c.heroEnd = mix(c.fill, away, 0.25);
        c.onFill2 = ensure(mix(c.onFill, c.fill, 0.15), new int[]{c.fill, c.heroEnd}, AA, c.onFill);
        c.accent = b.accent;
        c.accentTint = mix(b.accent, b.surface, 0.84);
        c.accentText = ensure(b.accent, new int[]{b.surface, b.bg, c.accentTint}, AA, c.text);
        c.danger = ensure(c.dark ? 0xFF8A80 : 0xC2410C, base, AA, c.text);
        int infoBase = c.dark ? 0x7CC4F0 : 0x2C7FB0;
        c.infoTint = mix(infoBase, b.surface, 0.86);
        c.info = ensure(infoBase, new int[]{b.surface, c.infoTint}, AA, c.text);
        return c;
    }

    /** Pairs that carry text, each with its minimum ratio: {label, fg, bg, min}. */
    public static List<Object[]> textPairs(Colors c) {
        List<Object[]> l = new ArrayList<Object[]>();
        l.add(new Object[]{"text on background", c.text, c.bg, AA});
        l.add(new Object[]{"text on cards", c.text, c.surface, AA});
        l.add(new Object[]{"secondary text", c.text2, c.surface, AA});
        l.add(new Object[]{"secondary text on background", c.text2, c.bg, AA});
        l.add(new Object[]{"hints", c.text3, c.surface, AA});
        l.add(new Object[]{"hints on background", c.text3, c.bg, AA});
        l.add(new Object[]{"hints on selected tiles", c.text3, c.selected, AA});
        l.add(new Object[]{"links on cards", c.primaryText, c.surface, AA});
        l.add(new Object[]{"selected chips", c.primaryText, c.tint, AA});
        l.add(new Object[]{"button text", c.onFill, c.fill, AA});
        l.add(new Object[]{"banner text", c.onFill2, c.heroEnd, AA});
        l.add(new Object[]{"warnings", c.accentText, c.accentTint, AA});
        l.add(new Object[]{"warnings on cards", c.accentText, c.surface, AA});
        l.add(new Object[]{"errors", c.danger, c.surface, AA});
        l.add(new Object[]{"swaps", c.info, c.infoTint, AA});
        // v1.8 layout: app name on the header band, header icons on the band, segmented tab labels on their track
        l.add(new Object[]{"header title on band", c.text, c.tint, AA});
        l.add(new Object[]{"header icons on band", c.text2, c.tint, AA_LARGE});
        l.add(new Object[]{"tab labels on track", c.text, c.neutral, AA});
        return l;
    }

    /** Problems with the base colours as chosen (before automatic fixes), for the custom editor. */
    public static List<String> warnings(Base b) {
        List<String> w = new ArrayList<String>();
        double t = Math.min(contrast(b.text, b.bg), contrast(b.text, b.surface));
        if (t < AA) w.add("Text contrast is " + ratio(t) + ":1, below the 4.5:1 AA minimum; text is darkened/lightened automatically.");
        if (contrast(b.primary, b.surface) < AA_LARGE) w.add("Primary is hard to see on cards (" + ratio(contrast(b.primary, b.surface)) + ":1); links use an adjusted shade.");
        if (contrast(b.bg, b.surface) < 1.04 && b.bg != b.surface) w.add("Background and surface are almost the same; cards will not stand out.");
        return w;
    }

    public static String ratio(double r) {
        long t = Math.round(r * 10);
        return (t / 10) + "." + (t % 10);
    }

    // ---------------- hex & HSV ----------------
    public static String hex(int rgb) {
        String s = Integer.toHexString(rgb & 0xFFFFFF).toUpperCase();
        while (s.length() < 6) s = "0" + s;
        return "#" + s;
    }

    /** "#RRGGBB", "RRGGBB" or "#RGB" (any case) -> 0xRRGGBB, or -1 if invalid. */
    public static int parseHex(String s) {
        if (s == null) return -1;
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() == 3) t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
        if (t.length() != 6) return -1;
        for (int k = 0; k < 6; k++) if (Character.digit(t.charAt(k), 16) < 0) return -1;
        return Integer.parseInt(t, 16);
    }

    /** {h 0..360, s 0..1, v 0..1}. */
    public static double[] toHsv(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0, g = ((rgb >> 8) & 0xFF) / 255.0, b = (rgb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        double h = 0;
        if (d > 0) {
            if (max == r) h = 60 * (((g - b) / d) % 6);
            else if (max == g) h = 60 * ((b - r) / d + 2);
            else h = 60 * ((r - g) / d + 4);
        }
        if (h < 0) h += 360;
        return new double[]{h, max == 0 ? 0 : d / max, max};
    }

    public static int fromHsv(double h, double s, double v) {
        h = ((h % 360) + 360) % 360;
        double c = v * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = v - c, r, g, b;
        if (h < 60) { r = c; g = x; b = 0; } else if (h < 120) { r = x; g = c; b = 0; } else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; } else if (h < 300) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
        return ((int) Math.round((r + m) * 255) << 16) | ((int) Math.round((g + m) * 255) << 8) | (int) Math.round((b + m) * 255);
    }

    // ---------------- saved state (prefs + backup) ----------------
    /** The active theme id plus the user's custom themes. */
    public static final class State {
        public String active = LIGHT;
        public final List<Base> custom = new ArrayList<Base>();
        /** 1.6 options; the defaults are what a v1.5 state means. accentPick is -1 when unset. */
        public String font = "normal", corners = "normal";
        public int accentPick = -1;

        public Base find(String id) {
            Base p = preset(id);
            if (p != null) return p;
            for (Base b : custom) if (b.id.equals(id)) return b;
            return null;
        }

        /** The Base to show; "system" picks Light or Dark from the device setting. */
        public Base resolve(boolean systemDark) {
            Base b = SYSTEM.equals(active) ? preset(systemDark ? DARK : LIGHT) : find(active);
            if (b == null) b = preset(LIGHT);
            return accentPick >= 0 ? b.withColors(b.bg, b.surface, accentPick, b.accent, b.text) : b;
        }

        public double fontScale() { return Palette.fontScale(font); }
        public double cornerScale() { return Palette.cornerScale(corners); }

        public String nextCustomId() {
            int n = 1;
            while (find("custom-" + n) != null) n++;
            return "custom-" + n;
        }

        /** Adds or replaces (same id) a custom theme; false when the limit is reached. */
        public boolean put(Base b) {
            for (int k = 0; k < custom.size(); k++) {
                if (custom.get(k).id.equals(b.id)) { custom.set(k, b); return true; }
            }
            if (custom.size() >= MAX_CUSTOM) return false;
            custom.add(b);
            return true;
        }

        public void remove(String id) {
            for (int k = 0; k < custom.size(); k++) if (custom.get(k).id.equals(id)) { custom.remove(k); break; }
            if (id.equals(active)) active = LIGHT;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("active", active);
            // written only when changed, so a v1.5-style state serialises exactly as before
            if (!"normal".equals(font)) m.put("font", font);
            if (!"normal".equals(corners)) m.put("corners", corners);
            if (accentPick >= 0) m.put("accent", hex(accentPick));
            List<Object> l = new ArrayList<Object>();
            for (Base b : custom) {
                Map<String, Object> o = new LinkedHashMap<String, Object>();
                o.put("id", b.id);
                o.put("name", b.name);
                o.put("bg", hex(b.bg));
                o.put("surface", hex(b.surface));
                o.put("primary", hex(b.primary));
                o.put("accent", hex(b.accent));
                o.put("text", hex(b.text));
                l.add(o);
            }
            m.put("custom", l);
            return m;
        }
    }

    private static String str(Object o, int max) {
        if (!(o instanceof String)) return "";
        String v = ((String) o).trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static boolean customId(String id) {
        if (!id.startsWith("custom-") || id.length() < 8 || id.length() > 11) return false;
        for (int k = 7; k < id.length(); k++) if (id.charAt(k) < '0' || id.charAt(k) > '9') return false;
        return true;
    }

    /** Validates a saved/imported theme state; anything malformed is dropped. Never throws. */
    @SuppressWarnings("unchecked")
    public static State parseState(Object o) {
        State s = new State();
        if (!(o instanceof Map)) return s;
        Map<String, Object> m = (Map<String, Object>) o;
        Object cl = m.get("custom");
        if (cl instanceof List) {
            for (Object x : (List<Object>) cl) {
                if (!(x instanceof Map) || s.custom.size() >= MAX_CUSTOM) continue;
                Map<String, Object> e = (Map<String, Object>) x;
                String id = str(e.get("id"), 20);
                if (!customId(id) || s.find(id) != null) continue;
                String name = str(e.get("name"), 40);
                if (name.isEmpty()) name = "My theme";
                int[] v = new int[5];
                String[] k = {"bg", "surface", "primary", "accent", "text"};
                boolean ok = true;
                for (int i = 0; i < 5; i++) {
                    v[i] = parseHex(str(e.get(k[i]), 9));
                    if (v[i] < 0) ok = false;
                }
                if (ok) s.custom.add(new Base(id, name, v[0], v[1], v[2], v[3], v[4]));
            }
        }
        String f = str(m.get("font"), 10), co = str(m.get("corners"), 10);
        if (oneOf(FONT_IDS, f)) s.font = f;
        if (oneOf(CORNER_IDS, co)) s.corners = co;
        Object ac = m.get("accent");
        if (ac instanceof String) s.accentPick = parseHex(str(ac, 9));
        String a = migrateId(str(m.get("active"), 20));
        s.active = SYSTEM.equals(a) || s.find(a) != null ? a : LIGHT;
        return s;
    }

    public static State parseState(String json) {
        try {
            return parseState(Json.parse(json));
        } catch (RuntimeException e) {
            return new State();
        }
    }
}
