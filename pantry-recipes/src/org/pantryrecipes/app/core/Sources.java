package org.pantryrecipes.app.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * v2.2: online recipe sources. Every network use is started by a tap. Two kinds:
 * - open APIs read in the app (TheMealDB, Wikibooks Cookbook search, Openverse photos, Open Food Facts), always shown
 *   with their attribution and licence;
 * - link-outs: a search on a reputable recipe site, opened in the browser. Nothing is scraped or copied.
 * The same lists are in js/core22.js (checked by vectors) and in ATTRIBUTIONS.md.
 */
public final class Sources {
    private Sources() {}

    /** id, name, region note, search URL with {q}. */
    public static final String[][] LINKS = {
        {"wikibooks", "Wikibooks Cookbook", "Open (CC BY-SA)", "https://en.wikibooks.org/w/index.php?ns102=1&search={q}"},
        {"bbcgoodfood", "BBC Good Food", "UK", "https://www.bbcgoodfood.com/search?q={q}"},
        {"seriouseats", "Serious Eats", "US", "https://www.seriouseats.com/search?q={q}"},
        {"allrecipes", "Allrecipes", "US", "https://www.allrecipes.com/search?q={q}"},
        {"recipetineats", "RecipeTin Eats", "Australia", "https://www.recipetineats.com/?s={q}"},
        {"kingarthur", "King Arthur Baking", "US, baking", "https://www.kingarthurbaking.com/search?query={q}"},
        {"chefkoch", "Chefkoch", "Germany (German)", "https://www.chefkoch.de/rs/s0/{q}/Rezepte.html"},
        {"marmiton", "Marmiton", "France (French)", "https://www.marmiton.org/recettes/recherche.aspx?aqt={q}"},
        {"giallozafferano", "GialloZafferano", "Italy (Italian)", "https://www.giallozafferano.it/ricerca-ricette/{q}/"},
        {"themealdb", "TheMealDB", "Open API", "https://www.themealdb.com/browse/search/?s={q}"},
    };

    /** Public-domain cookbooks on Project Gutenberg (ebook number, title, author, year). Opened in the browser. */
    public static final String[][] BOOKS = {
        {"10136", "The Book of Household Management", "Isabella Beeton", "1861"},
        {"22114", "A Plain Cookery Book for the Working Classes", "Charles Elm\u00e9 Francatelli", "1852"},
        {"29519", "Nelson's Home Comforts", "Mary Hooper", "1882"},
        {"26209", "The Golden Age Cook Book", "Henrietta Latham Dwight", "1898"},
        {"31534", "Fifty-two Sunday Dinners", "Elizabeth O. Hiller", "1913"},
        {"8542", "Things Mother Used to Make", "Lydia Maria Gurney", "1914"},
        {"15464", "Foods That Will Win the War", "C. Houston Goudiss and Alberta M. Goudiss", "1918"},
    };

    public static String bookUrl(String id) { return "https://www.gutenberg.org/ebooks/" + id; }

    /** In-app open sources with their licence lines (shown in the app's Sources screen). */
    public static final String[][] OPEN = {
        {"TheMealDB", "Recipes and photos from TheMealDB's free API (themealdb.com). Optional; off until you turn on online recipes."},
        {"Wikibooks Cookbook", "Search results and links to Wikibooks Cookbook pages, text under CC BY-SA 4.0 by Wikibooks contributors. Opened on Wikibooks, not copied."},
        {"Openverse", "Optional recipe photos found through Openverse (openverse.org), only CC0, Public Domain Mark or CC BY images, each shown with its creator and licence."},
        {"Open Food Facts", "Product names from Open Food Facts (openfoodfacts.org), data under the Open Database License (ODbL)."},
        {"Project Gutenberg", "Links to public-domain cookbooks on gutenberg.org."},
    };

    public static String enc(String q) {
        try { return URLEncoder.encode(q == null ? "" : q.trim(), "UTF-8").replace("+", "%20"); }
        catch (UnsupportedEncodingException e) { return ""; }
    }

    public static String linkUrl(String id, String q) {
        for (String[] l : LINKS) if (l[0].equals(id)) return l[3].replace("{q}", enc(q));
        return null;
    }

    /** Wikibooks search API URL (Cookbook namespace 102). */
    public static String wikibooksApi(String q) {
        return "https://en.wikibooks.org/w/api.php?action=query&list=search&srnamespace=102&srlimit=20&format=json&srsearch=" + enc(q);
    }

    public static final class Hit {
        public final String title, snippet, url;
        Hit(String t, String s, String u) { title = t; snippet = s; url = u; }
    }

    /** Strips tags and entities from a MediaWiki search snippet. */
    public static String plain(String html) {
        String s = html.replaceAll("<[^>]*>", "").replace("&quot;", "\"").replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        return s.replaceAll("\\s+", " ").trim();
    }

    @SuppressWarnings("unchecked")
    public static List<Hit> parseWikibooks(String json) {
        List<Hit> out = new ArrayList<Hit>();
        Object o = Json.parse(json);
        if (!(o instanceof Map)) return out;
        Object q = ((Map<String, Object>) o).get("query");
        if (!(q instanceof Map)) return out;
        Object s = ((Map<String, Object>) q).get("search");
        if (!(s instanceof List)) return out;
        for (Object x : (List<Object>) s) {
            if (!(x instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) x;
            String t = Json.str(m, "title", "");
            if (!t.startsWith("Cookbook:")) continue;
            String name = t.substring(9);
            out.add(new Hit(name, plain(Json.str(m, "snippet", "")), "https://en.wikibooks.org/wiki/" + enc(t.replace(' ', '_')).replace("%3A", ":").replace("%2F", "/")));
        }
        return out;
    }

    // ---- Openverse photos ----

    public static String openverseApi(String q) {
        return "https://api.openverse.org/v1/images/?license=cc0,pdm,by&mature=false&page_size=12&q=" + enc(q);
    }

    public static final class Photo {
        public final String thumb, url, title, creator, license, version, landing, provider;
        Photo(String th, String u, String t, String c, String l, String v, String la, String p) { thumb = th; url = u; title = t; creator = c; license = l; version = v; landing = la; provider = p; }
        /** "Photo: “Chicken Curry” by rovingI, CC BY 2.0, via Openverse (flickr)". */
        public String credit() {
            String lic = license.equals("cc0") ? "CC0" : license.equals("pdm") ? "Public Domain Mark" : "CC " + license.toUpperCase(java.util.Locale.US) + (version.isEmpty() ? "" : " " + version);
            return "Photo: " + (title.isEmpty() ? "untitled" : "\u201c" + title + "\u201d") + (creator.isEmpty() ? "" : " by " + creator) + ", " + lic + ", via Openverse" + (provider.isEmpty() ? "" : " (" + provider + ")");
        }
        public String encode() { return Json.write(java.util.Arrays.asList((Object) thumb, url, title, creator, license, version, landing, provider)); }
    }

    @SuppressWarnings("unchecked")
    public static Photo decode(String s) {
        if (s == null || s.isEmpty()) return null;
        Object o = Json.parse(s);
        if (!(o instanceof List) || ((List<Object>) o).size() != 8) return null;
        List<Object> l = (List<Object>) o;
        return new Photo(String.valueOf(l.get(0)), String.valueOf(l.get(1)), String.valueOf(l.get(2)), String.valueOf(l.get(3)), String.valueOf(l.get(4)), String.valueOf(l.get(5)), String.valueOf(l.get(6)), String.valueOf(l.get(7)));
    }

    static boolean okLicense(String l) { return l.equals("cc0") || l.equals("pdm") || l.equals("by"); }

    /** Openverse results filtered to CC0 / PDM / CC BY over https, thumbnails preferred (small). */
    @SuppressWarnings("unchecked")
    public static List<Photo> parseOpenverse(String json) {
        List<Photo> out = new ArrayList<Photo>();
        Object o = Json.parse(json);
        if (!(o instanceof Map)) return out;
        Object r = ((Map<String, Object>) o).get("results");
        if (!(r instanceof List)) return out;
        for (Object x : (List<Object>) r) {
            if (!(x instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) x;
            String lic = Json.str(m, "license", "").toLowerCase(java.util.Locale.US), url = Json.str(m, "url", ""), th = Json.str(m, "thumbnail", "");
            if (!okLicense(lic) || !url.startsWith("https://")) continue;
            if (!th.startsWith("https://")) th = url;
            out.add(new Photo(th, url, Json.str(m, "title", "").trim(), Json.str(m, "creator", "").trim(), lic, Json.str(m, "license_version", ""), Json.str(m, "foreign_landing_url", ""), Json.str(m, "provider", "")));
        }
        return out;
    }
}
