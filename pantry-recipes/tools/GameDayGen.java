import org.pantryrecipes.app.core.Palette;
import java.util.*;

/**
 * v2.0: generates the "Game day" theme presets from two-colour pairings that are common in sports (approximate,
 * hand-picked colours with neutral, original names; no team names or official colour codes). For each pairing it
 * makes a light and a dark variant, tunes the shades until every text pair passes WCAG AA, and keeps only presets
 * that are clearly distinct from all other presets. Prints Palette.java lines. Run: java -cp build/core GameDayGen
 */
public class GameDayGen {
    static final Object[][] PAIRS = {
        // name, primary, second colour
        {"Gridiron Gold", 0xB3995D, 0x101820}, {"Harbor Orange & Navy", 0xFB4F14, 0x002244}, {"Crimson & Cream", 0x9E1B32, 0xF1E6D2},
        {"Deep Field Green", 0x004C54, 0xA5ACAF}, {"Royal & Gold", 0x0033A0, 0xFFC72C}, {"Purple & Gold", 0x552583, 0xFDB927},
        {"Teal & Black", 0x006778, 0x101820}, {"Scarlet & Gray", 0xBB0000, 0x666666}, {"Burnt Orange", 0xBF5700, 0x333F48},
        {"Maroon & White", 0x7A0019, 0xFFCC33}, {"Forest & Gold", 0x203731, 0xFFB612}, {"Cardinal Red", 0x97233F, 0xFFB612},
        {"Powder Blue & Gold", 0x0080C6, 0xFFC20E}, {"Silver & Black", 0x5F6A72, 0x000000}, {"Navy & Red", 0x0C2340, 0xC8102E},
        {"Kelly Green", 0x007A33, 0xBA9653}, {"Brick & Navy", 0xA6192E, 0x13274F}, {"Garnet & Gold", 0x782F40, 0xCEB888},
        {"Orange & Blue", 0xE87722, 0x0B2240}, {"Sky & Navy", 0x6CACE4, 0x041E42}, {"Ice Blue & Navy", 0x00205B, 0x99D9EA},
        {"Pitch Green", 0x3A7D44, 0xF4F1E8}, {"Steel & Gold", 0x2C3E50, 0xFFB81C}, {"Hornet Teal & Purple", 0x00788C, 0x1D1160},
        {"Lakefront Navy & Orange", 0x0B162A, 0xC83803}, {"Copper & Black", 0xB87333, 0x1A1A1A}, {"Cobalt & Silver", 0x0047AB, 0xA2AAAD},
        {"Emerald & White", 0x00843D, 0xFFFFFF}, {"Plum & Teal", 0x5A2D82, 0x00A3AD}, {"Rust & Navy", 0xB7410E, 0x1F2A44},
        {"Blaze Orange & Black", 0xF15A22, 0x111111}, {"Old Gold & Black", 0xA28D5B, 0x000000}, {"Victory Red & Royal", 0xCE1141, 0x1D428A},
        {"Frozen Pond", 0x4B92DB, 0x111C24}, {"Backcourt Blue & Yellow", 0x1D428A, 0xFFC72C}, {"Desert Sun & Purple", 0xE56020, 0x1D1160},
        {"Bayou Gold & Black", 0xD3BC8D, 0x101820}, {"Seafoam & Navy", 0x69BE28, 0x002244}, {"Wine & Gold", 0x860038, 0xFDBB30},
        {"Midnight & Lime", 0x0C2340, 0x78BE20}, {"Cherry & Black", 0xC8102E, 0x000000}, {"Sunflower & Navy", 0xFFB612, 0x13294B},
        {"Tiger Orange & Purple", 0xF56600, 0x522D80}, {"Columbine & Gray", 0x9BB8D3, 0x53565A}, {"Ember & Charcoal", 0xD50A0A, 0x34302B},
        {"Spruce & Silver", 0x154734, 0xA2AAAD}, {"Lagoon Teal & Orange", 0x008E97, 0xFC4C02}, {"Mustard & Brown", 0xFFB81C, 0x311D00},
        {"Bronze & Blue", 0x8C6B3A, 0x0E2240}, {"Twilight Purple & Black", 0x4F2683, 0x000000}, {"Electric Blue & Red", 0x0076B6, 0xD6001C},
        {"Clay & Cream", 0xA4463A, 0xEFE6D5}, {"Vintage Mint & Black", 0x3EB489, 0x000000}, {"Pine & Orange", 0x01411C, 0xF47A38},
        {"Bluegrass Royal", 0x0033A0, 0xFFFFFF}, {"Rose & Gold", 0xB5446E, 0xC9A227}, {"Lake Blue & Silver", 0x236192, 0xC4CED4},
        {"Canary & Green", 0xFFCD00, 0x154734}, {"Sand & Navy", 0xC4B581, 0x0C2340}, {"Magenta & Teal", 0xA6116E, 0x00808F},
    };

    static int dist(int a, int b) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) + Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) + Math.abs((a & 255) - (b & 255));
    }

    static int toward(int c, int target, int[] bgs, double min) {
        for (int k = 0; k <= 40; k++) {
            int x = Palette.mix(c, target, k / 40.0);
            boolean ok = true;
            for (int b : bgs) if (Palette.contrast(x, b) < min) ok = false;
            if (ok) return x;
        }
        return target;
    }

    static boolean good(Palette.Base b) {
        if (!Palette.warnings(b).isEmpty()) return false;
        if (Palette.contrast(b.text, b.bg) < Palette.AA || Palette.contrast(b.text, b.surface) < Palette.AA) return false;
        Palette.Colors c = Palette.derive(b);
        if (c.textAdjusted) return false;
        for (Object[] p : Palette.textPairs(c)) if (Palette.contrast((Integer) p[1], (Integer) p[2]) < (Double) p[3]) return false;
        for (int a : Palette.QUICK_ACCENTS) {
            Palette.Colors ca = Palette.derive(b.withColors(b.bg, b.surface, a, b.accent, b.text));
            for (Object[] p : Palette.textPairs(ca)) if (Palette.contrast((Integer) p[1], (Integer) p[2]) < (Double) p[3]) return false;
        }
        return true;
    }

    static boolean distinct(Palette.Base a, List<Palette.Base> all) {
        for (Palette.Base b : all) {
            if (b.id.equals(a.id)) continue;
            int dp = dist(a.primary, b.primary), db = dist(a.bg, b.bg), da = dist(a.accent, b.accent);
            if (!(dp > 40 || db > 40 || (da > 60 && dp + db + da > 100))) return false;
        }
        return true;
    }

    static String slug(String n) { return "gd-" + n.toLowerCase().replace("&", "and").replaceAll("[^a-z0-9]+", "-").replaceAll("-+$", ""); }

    public static void main(String[] a) {
        List<Palette.Base> all = new ArrayList<>();
        for (Palette.Base b : Palette.PRESETS) if (!b.group.equals("Game day")) all.add(b);
        List<String> out = new ArrayList<>();
        int light = 0, dark = 0;
        for (Object[] p : PAIRS) {
            String name = (String) p[0];
            int pr = (Integer) p[1], se = (Integer) p[2];
            // light: white cards on a faint tint of the main colour
            int sL = 0xFFFFFF, bgL = Palette.mix(pr, 0xFFFFFF, 0.90);
            int prL = toward(pr, 0x000000, new int[]{sL, bgL}, 4.6);
            int acL = toward(se, 0x000000, new int[]{sL}, 3.2);
            if (dist(acL, prL) < 60) acL = toward(Palette.mix(se, 0x7A7A7A, 0.4), 0x000000, new int[]{sL}, 3.2);
            int txL = toward(Palette.mix(pr, 0x000000, 0.82), 0x000000, new int[]{sL, bgL}, 7.0);
            Palette.Base L = new Palette.Base(slug(name), name, "Game day", bgL, sL, prL, acL, txL);
            // dark: tinted near-black, light shades of both colours
            int bgD = Palette.mix(pr, 0x000000, 0.88), sD = Palette.mix(pr, 0x000000, 0.80);
            int prD = toward(pr, 0xFFFFFF, new int[]{sD, bgD}, 4.6);
            int acD = toward(se, 0xFFFFFF, new int[]{sD}, 3.2);
            if (dist(acD, prD) < 60) acD = toward(Palette.mix(se, 0xBFBFBF, 0.4), 0xFFFFFF, new int[]{sD}, 3.2);
            int txD = toward(Palette.mix(pr, 0xFFFFFF, 0.9), 0xFFFFFF, new int[]{sD, bgD}, 9.0);
            Palette.Base D = new Palette.Base(slug(name) + "-dark", name + " dark", "Game day", bgD, sD, prD, acD, txD);
            for (Palette.Base b : new Palette.Base[]{L, D}) {
                if (!good(b)) { System.err.println("skip (contrast) " + b.name); continue; }
                if (!distinct(b, all)) { System.err.println("skip (similar) " + b.name); continue; }
                all.add(b);
                if (b == L) light++; else dark++;
                out.add(String.format("            p(\"Game day\", \"%s\", \"%s\", 0x%06X, 0x%06X, 0x%06X, 0x%06X, 0x%06X)",
                        b.id, b.name, b.bg, b.surface, b.primary, b.accent, b.text));
            }
        }
        System.out.println(String.join(",\n", out));
        System.err.println("game day presets: " + out.size() + " (light " + light + ", dark " + dark + ")");
    }
}
