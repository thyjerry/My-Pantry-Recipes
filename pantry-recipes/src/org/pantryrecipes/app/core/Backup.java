package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The backup file shared by the Android app and the web app (format 2; format 1 files from v1.5 and earlier
 * still import). Writing is deterministic: the same
 * data gives the same JSON on both platforms. Parsing validates every field and drops anything it does not
 * understand, so a damaged or hand-edited file cannot inject unexpected data.
 */
public final class Backup {
    private Backup() { }

    public static final String APP = "Pantry Recipes";
    public static final int FORMAT = 2;
    /** Large enough for a backup that includes photos. */
    public static final int MAX_CHARS = 32 * 1024 * 1024;
    public static final int MAX_PHOTOS = 100, MAX_PHOTO_CHARS = 1500000, MAX_BARCODES = 500;
    public static final int MAX_NOTE = 2000;

    /** Settings both apps understand; anything else in "settings" is ignored. */
    public static final List<String> SETTINGS = java.util.Collections.unmodifiableList(Arrays.asList(
            "servings", "meals", "incDesserts", "incSides", "diets", "maxTime", "mode", "online", "acceptedSubs",
            "currency", "useDefaultPrices", "deductPantry", "leftoverDays", "shopCost",
            // v1.7 (still format 2: older apps ignore these, older backups simply lack them)
            "units", "weightUnit", "showCups", "methods",
            // v2.0 (still format 2)
            "zip", "budget", "budgetWeek", "expiryReminders",
            "region", "gasMarks", "fanNote", "dateFormat", "decimalComma", "clock24", "weekStart", "household",
            // v2.0 Stage 3 (still format 2)
            "holidays", "fasts", "holLead", "holBanner", "starter", "bakeLog", "holNotify", "fastChips",
            // v2.0.1 medical diet guides: ticked guides and the optional per-meal limits (no secrets)
            "medGuides", "medSodiumMg", "medCarbsG",
            // v2.1: the user's ingredient category moves (ingredient key -> catalog category name)
            "catMoves"));

    public static final class PantryEntry {
        public String label = "", key = "", useBy = "", qty = "";
        public boolean staple;
        /** v2.0 inventory: location ("" = automatic), date frozen, "running low". */
        public String loc = "", frozen = "";
        public boolean low;
    }

    public static final class ShopEntry {
        public String name = "", unit = "", recipe = "";
        public double amount = Double.NaN;
        public boolean checked;
    }

    public static final class Rating {
        public int stars;      // 0 = no stars, only a note
        public String note = "";
    }

    public static final class Data {
        public String appVersion = "", platform = "", exported = "";
        public final List<PantryEntry> pantry = new ArrayList<PantryEntry>();
        public final List<String> favorites = new ArrayList<String>();
        public final List<ShopEntry> shopping = new ArrayList<ShopEntry>();
        public final LinkedHashMap<String, Rating> ratings = new LinkedHashMap<String, Rating>();
        public final List<MealPlan.Entry> plan = new ArrayList<MealPlan.Entry>();
        public final List<String> dislikes = new ArrayList<String>();
        public final List<String> allergens = new ArrayList<String>();
        public final LinkedHashMap<String, Object> settings = new LinkedHashMap<String, Object>();
        /** Online (m:) and user-imported (u:) recipes, so favorites and plans keep working offline. */
        public final List<Recipe> recipes = new ArrayList<Recipe>();
        /** Appearance: active theme and custom themes (null when the file has none). */
        public Palette.State theme;
        /** Format 2: user prices by ingredient key. */
        public final LinkedHashMap<String, Cost.Price> prices = new LinkedHashMap<String, Cost.Price>();
        public final List<Leftovers.Entry> leftovers = new ArrayList<Leftovers.Entry>();
        public final List<Leftovers.Cooked> cooked = new ArrayList<Leftovers.Cooked>();
        /** Barcode -> pantry item label the user confirmed. */
        public final LinkedHashMap<String, String> barcodes = new LinkedHashMap<String, String>();
        /** Recipe id -> JPEG data URL; only filled when the user chose "include photos". */
        public final LinkedHashMap<String, String> photos = new LinkedHashMap<String, String>();
    }

    public static Map<String, Object> toMap(Data d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("app", APP);
        m.put("format", FORMAT);
        m.put("appVersion", d.appVersion);
        m.put("platform", d.platform);
        m.put("exported", d.exported);
        List<Object> p = new ArrayList<Object>();
        for (PantryEntry e : d.pantry) {
            Map<String, Object> o = new LinkedHashMap<String, Object>();
            o.put("label", e.label);
            o.put("key", e.key);
            o.put("staple", e.staple);
            if (!e.useBy.isEmpty()) o.put("useBy", e.useBy);
            if (!e.qty.isEmpty()) o.put("qty", e.qty);
            if (!e.loc.isEmpty()) o.put("loc", e.loc);
            if (!e.frozen.isEmpty()) o.put("frozen", e.frozen);
            if (e.low) o.put("low", true);
            p.add(o);
        }
        m.put("pantry", p);
        m.put("favorites", new ArrayList<Object>(d.favorites));
        List<Object> s = new ArrayList<Object>();
        for (ShopEntry e : d.shopping) {
            Map<String, Object> o = new LinkedHashMap<String, Object>();
            o.put("name", e.name);
            if (!Double.isNaN(e.amount)) o.put("amount", e.amount);
            if (!e.unit.isEmpty()) o.put("unit", e.unit);
            if (!e.recipe.isEmpty()) o.put("recipe", e.recipe);
            o.put("checked", e.checked);
            s.add(o);
        }
        m.put("shopping", s);
        Map<String, Object> r = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Rating> e : d.ratings.entrySet()) {
            Map<String, Object> o = new LinkedHashMap<String, Object>();
            o.put("stars", e.getValue().stars);
            if (!e.getValue().note.isEmpty()) o.put("note", e.getValue().note);
            r.put(e.getKey(), o);
        }
        m.put("ratings", r);
        List<Object> pl = new ArrayList<Object>();
        for (MealPlan.Entry e : d.plan) {
            Map<String, Object> o = new LinkedHashMap<String, Object>();
            o.put("date", e.date);
            o.put("id", e.id);
            o.put("servings", e.servings);
            pl.add(o);
        }
        m.put("plan", pl);
        m.put("dislikes", new ArrayList<Object>(d.dislikes));
        m.put("allergens", new ArrayList<Object>(d.allergens));
        Map<String, Object> st = new LinkedHashMap<String, Object>();
        for (String k : SETTINGS) if (d.settings.containsKey(k)) st.put(k, d.settings.get(k));
        m.put("settings", st);
        if (d.theme != null) m.put("theme", d.theme.toMap());
        List<Object> rs = new ArrayList<Object>();
        for (Recipe x : d.recipes) rs.add(x.toMap());
        m.put("recipes", rs);
        Map<String, Object> pr = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Cost.Price> e : d.prices.entrySet()) pr.put(e.getKey(), e.getValue().toMap());
        m.put("prices", pr);
        List<Object> lo = new ArrayList<Object>();
        for (Leftovers.Entry e : d.leftovers) lo.add(e.toMap());
        m.put("leftovers", lo);
        List<Object> ck = new ArrayList<Object>();
        for (Leftovers.Cooked e : d.cooked) ck.add(e.toMap());
        m.put("cooked", ck);
        m.put("barcodes", new LinkedHashMap<String, Object>(d.barcodes));
        if (!d.photos.isEmpty()) m.put("photos", new LinkedHashMap<String, Object>(d.photos));
        return m;
    }

    public static String write(Data d) { return Json.write(toMap(d)); }

    /** Thrown with a message that can be shown to the user. */
    public static final class BackupException extends Exception {
        public BackupException(String msg) { super(msg); }
    }

    private static String s(Object o, int max) {
        if (!(o instanceof String)) return "";
        String v = ((String) o).trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static boolean b(Object o) { return o instanceof Boolean && (Boolean) o; }

    private static boolean userRecipeId(String id) {
        return (id.startsWith("m:") || id.startsWith("u:")) && id.length() > 2 && id.length() <= 80;
    }

    @SuppressWarnings("unchecked")
    public static Data parse(String json) throws BackupException {
        if (json == null || json.trim().isEmpty()) throw new BackupException("The file is empty.");
        if (json.length() > MAX_CHARS) throw new BackupException("The file is too large to be a My Pantry Recipes backup.");
        Object root;
        try {
            root = Json.parse(json);
        } catch (RuntimeException e) {
            throw new BackupException("This is not a valid backup file (it is not JSON).");
        }
        if (!(root instanceof Map)) throw new BackupException("This is not a My Pantry Recipes backup file.");
        Map<String, Object> m = (Map<String, Object>) root;
        if (!APP.equals(m.get("app"))) throw new BackupException("This is not a My Pantry Recipes backup file.");
        Object f = m.get("format");
        if (!(f instanceof Number)) throw new BackupException("The backup has no format number.");
        int format = ((Number) f).intValue();
        if (format > FORMAT) throw new BackupException("This backup was made by a newer version of My Pantry Recipes. Update the app first.");
        if (format < 1) throw new BackupException("Unknown backup format.");
        Data d = new Data();
        d.appVersion = s(m.get("appVersion"), 40);
        d.platform = s(m.get("platform"), 40);
        d.exported = s(m.get("exported"), 40);

        Set<String> keys = new LinkedHashSet<String>();
        for (Object o : list(m.get("pantry"))) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> e = (Map<String, Object>) o;
            PantryEntry p = new PantryEntry();
            p.label = s(e.get("label"), 80);
            p.key = Normalizer.normalize(p.label);
            if (p.key.isEmpty() || !keys.add(p.key)) continue;
            p.staple = b(e.get("staple"));
            String u = s(e.get("useBy"), 10);
            p.useBy = Dates.valid(u) ? u : "";
            p.qty = s(e.get("qty"), 40);
            p.loc = Inventory.validLocation(s(e.get("loc"), 10));
            String fz = s(e.get("frozen"), 10);
            p.frozen = Dates.valid(fz) ? fz : "";
            p.low = b(e.get("low"));
            d.pantry.add(p);
        }
        Set<String> favs = new LinkedHashSet<String>();
        for (Object o : list(m.get("favorites"))) {
            String id = s(o, 80);
            if (!id.isEmpty()) favs.add(id);
        }
        d.favorites.addAll(favs);
        for (Object o : list(m.get("shopping"))) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> e = (Map<String, Object>) o;
            ShopEntry x = new ShopEntry();
            x.name = s(e.get("name"), 120);
            if (x.name.isEmpty()) continue;
            Object a = e.get("amount");
            x.amount = a instanceof Number && ((Number) a).doubleValue() > 0 ? ((Number) a).doubleValue() : Double.NaN;
            x.unit = s(e.get("unit"), 30);
            x.recipe = s(e.get("recipe"), 160);
            x.checked = b(e.get("checked"));
            d.shopping.add(x);
            if (d.shopping.size() >= 1000) break;
        }
        Object rt = m.get("ratings");
        if (rt instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) rt).entrySet()) {
                if (!(e.getValue() instanceof Map) || e.getKey().isEmpty() || e.getKey().length() > 80) continue;
                Map<String, Object> o = (Map<String, Object>) e.getValue();
                Rating r = new Rating();
                Object st = o.get("stars");
                r.stars = st instanceof Number ? Math.max(0, Math.min(5, ((Number) st).intValue())) : 0;
                r.note = s(o.get("note"), MAX_NOTE);
                if (r.stars == 0 && r.note.isEmpty()) continue;
                d.ratings.put(e.getKey(), r);
            }
        }
        for (Object o : list(m.get("plan"))) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> e = (Map<String, Object>) o;
            String date = s(e.get("date"), 10), id = s(e.get("id"), 80);
            if (!Dates.valid(date) || id.isEmpty()) continue;
            Object sv = e.get("servings");
            d.plan.add(new MealPlan.Entry(date, id, sv instanceof Number ? ((Number) sv).intValue() : 4));
            if (d.plan.size() >= 500) break;
        }
        Set<String> dis = new LinkedHashSet<String>();
        for (Object o : list(m.get("dislikes"))) {
            String k = Allergens.dislikeKey(s(o, 80));
            if (!k.isEmpty()) dis.add(k);
        }
        d.dislikes.addAll(dis);
        Set<String> al = new LinkedHashSet<String>();
        for (Object o : list(m.get("allergens"))) if (Allergens.ALL_IDS.contains(o)) al.add((String) o);
        for (String id : Allergens.ALL_IDS) if (al.contains(id)) d.allergens.add(id);   // canonical order
        Object so = m.get("settings");
        if (so instanceof Map) {
            Map<String, Object> st = (Map<String, Object>) so;
            Object v;
            if ((v = st.get("servings")) instanceof Number) d.settings.put("servings", (long) Math.max(1, Math.min(24, ((Number) v).intValue())));
            if ((v = st.get("meals")) instanceof List) {
                List<Object> ml = new ArrayList<Object>();
                for (Object x : MealType.sorted(strings(v))) ml.add(x);
                d.settings.put("meals", ml);
            }
            if ((v = st.get("incDesserts")) instanceof Boolean) d.settings.put("incDesserts", v);
            if ((v = st.get("incSides")) instanceof Boolean) d.settings.put("incSides", v);
            if ((v = st.get("diets")) instanceof List) {
                List<Object> dl = new ArrayList<Object>();
                for (String x : strings(v)) {
                    if ((x.equals(Recipe.VEGETARIAN) || x.equals(Recipe.VEGAN) || x.equals(Recipe.GLUTEN_FREE)
                            || x.equals(Recipe.DAIRY_FREE) || Diets.IDS.contains(x)) && !dl.contains(x)) dl.add(x);
                }
                d.settings.put("diets", dl);
            }
            if ((v = st.get("maxTime")) instanceof Number) {
                long t = ((Number) v).longValue();
                if (t == 0 || t == 15 || t == 30 || t == 60) d.settings.put("maxTime", t);
            }
            if ((v = st.get("mode")) instanceof Number) {
                long t = ((Number) v).longValue();
                if (t >= 0 && t <= 2) d.settings.put("mode", t);
            }
            if ((v = st.get("online")) instanceof Boolean) d.settings.put("online", v);
            if ((v = st.get("acceptedSubs")) instanceof List) {
                List<Object> al2 = new ArrayList<Object>();
                for (String x : strings(v)) {
                    String k = Normalizer.normalize(x);
                    if (!k.isEmpty() && !al2.contains(k)) al2.add(k);
                }
                d.settings.put("acceptedSubs", al2);
            }
        }
        if (so instanceof Map) {
            Map<String, Object> st = (Map<String, Object>) so;
            Object v;
            if ((v = st.get("currency")) instanceof String) {
                String c = ((String) v).trim();
                if (!c.isEmpty() && c.length() <= 4 && c.indexOf('<') < 0 && c.indexOf('>') < 0) d.settings.put("currency", c);
            }
            if ((v = st.get("useDefaultPrices")) instanceof Boolean) d.settings.put("useDefaultPrices", v);
            if ((v = st.get("deductPantry")) instanceof Boolean) d.settings.put("deductPantry", v);
            if ((v = st.get("shopCost")) instanceof Boolean) d.settings.put("shopCost", v);
            if ((v = st.get("leftoverDays")) instanceof Number) d.settings.put("leftoverDays", (long) Leftovers.clampDays(((Number) v).intValue()));
            if ((v = st.get("units")) instanceof String && Units.validMode((String) v).equals(v)) d.settings.put("units", v);
            if ((v = st.get("weightUnit")) instanceof String && ("g".equals(v) || "oz".equals(v))) d.settings.put("weightUnit", v);
            if ((v = st.get("showCups")) instanceof Boolean) d.settings.put("showCups", v);
            if ((v = st.get("zip")) instanceof String && !RegionalPrices.cleanZip((String) v).isEmpty()) d.settings.put("zip", RegionalPrices.cleanZip((String) v));
            if ((v = st.get("budget")) instanceof Number) {
                double bv = ((Number) v).doubleValue();
                if (bv >= 0 && bv <= 1000) d.settings.put("budget", Math.round(bv * 100) / 100.0);
            }
            if ((v = st.get("budgetWeek")) instanceof Number) {
                double bv = ((Number) v).doubleValue();
                if (bv >= 0 && bv <= 100000) d.settings.put("budgetWeek", Math.round(bv * 100) / 100.0);
            }
            if ((v = st.get("expiryReminders")) instanceof Boolean) d.settings.put("expiryReminders", v);
            if ((v = st.get("region")) instanceof String && !Region.valid((String) v).isEmpty()) d.settings.put("region", v);
            if ((v = st.get("gasMarks")) instanceof Boolean) d.settings.put("gasMarks", v);
            if ((v = st.get("fanNote")) instanceof Boolean) d.settings.put("fanNote", v);
            if ((v = st.get("decimalComma")) instanceof Boolean) d.settings.put("decimalComma", v);
            if ((v = st.get("dateFormat")) instanceof String && Region.DATE_FORMATS.contains(v)) d.settings.put("dateFormat", v);
            if ((v = st.get("clock24")) instanceof Boolean) d.settings.put("clock24", v);
            if ((v = st.get("weekStart")) instanceof String && ("mon".equals(v) || "sun".equals(v))) d.settings.put("weekStart", v);
            if ((v = st.get("household")) instanceof List) d.settings.put("household", Household.toList(Household.parse(v)));
            if ((v = st.get("holidays")) instanceof List) d.settings.put("holidays", ids(v, 100));
            if ((v = st.get("fasts")) instanceof List) d.settings.put("fasts", ids(v, 20));
            if ((v = st.get("holLead")) instanceof Number) { int n = ((Number) v).intValue(); if (n == 7 || n == 14 || n == 21) d.settings.put("holLead", n); }
            if ((v = st.get("holBanner")) instanceof Boolean) d.settings.put("holBanner", v);
            if ((v = st.get("holNotify")) instanceof Boolean) d.settings.put("holNotify", v);
            if ((v = st.get("fastChips")) instanceof Boolean) d.settings.put("fastChips", v);
            if ((v = st.get("medGuides")) instanceof List) {
                List<Object> gl = new ArrayList<Object>();
                for (Object g : (List<?>) v) if (g instanceof String && MedGuides.IDS.contains(g) && !gl.contains(g)) gl.add(g);
                d.settings.put("medGuides", gl);
            }
            if ((v = st.get("medSodiumMg")) instanceof Number && !Double.isNaN(((Number) v).doubleValue())) d.settings.put("medSodiumMg", MedGuides.clampSodium(Math.round(((Number) v).doubleValue())));
            if ((v = st.get("medCarbsG")) instanceof Number && !Double.isNaN(((Number) v).doubleValue())) d.settings.put("medCarbsG", MedGuides.clampCarbs(Math.round(((Number) v).doubleValue())));
            if ((v = st.get("catMoves")) instanceof Map) d.settings.put("catMoves", new java.util.LinkedHashMap<String, Object>(Botany.sanitize((Map<?, ?>) v)));
            if ((v = st.get("starter")) instanceof List) d.settings.put("starter", logList(v, new String[]{"d", "m", "ratio", "r"}, 60));
            if ((v = st.get("bakeLog")) instanceof List) d.settings.put("bakeLog", logList(v, new String[]{"d", "what", "note", "stars"}, 300));
            if ((v = st.get("methods")) instanceof List) {
                List<Object> ml = new ArrayList<Object>();
                for (String id : Methods.IDS) if (strings(v).contains(id)) ml.add(id);
                d.settings.put("methods", ml);
            }
        }
        Object po = m.get("prices");
        if (po instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) po).entrySet()) {
                String k = Normalizer.normalize(e.getKey());
                if (k.isEmpty() || !(e.getValue() instanceof Map) || d.prices.containsKey(k)) continue;
                Map<String, Object> o = (Map<String, Object>) e.getValue();
                Object a = o.get("price"), n = o.get("amount");
                if (!(a instanceof Number) || !(n instanceof Number)) continue;
                Cost.Price p = Cost.make(((Number) a).doubleValue(), ((Number) n).doubleValue(), s(o.get("unit"), 8));
                if (p != null) d.prices.put(k, p);
                if (d.prices.size() >= Cost.MAX_PRICES) break;
            }
        }
        Set<String> lids = new LinkedHashSet<String>();
        for (Object o : list(m.get("leftovers"))) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> e = (Map<String, Object>) o;
            Leftovers.Entry x = new Leftovers.Entry();
            x.id = s(e.get("id"), 20);
            x.recipe = s(e.get("recipe"), 80);
            x.title = s(e.get("title"), 120);
            x.cooked = s(e.get("cooked"), 10);
            x.useBy = s(e.get("useBy"), 10);
            Object n = e.get("portions");
            x.portions = n instanceof Number ? Math.min(Leftovers.MAX_PORTIONS, ((Number) n).intValue()) : 0;
            if (x.id.isEmpty() || !lids.add(x.id) || x.title.isEmpty() || x.portions <= 0 || !Dates.valid(x.cooked) || !Dates.valid(x.useBy)) continue;
            d.leftovers.add(x);
            if (d.leftovers.size() >= Leftovers.MAX_ENTRIES) break;
        }
        for (Object o : list(m.get("cooked"))) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> e = (Map<String, Object>) o;
            Leftovers.Cooked c = new Leftovers.Cooked();
            c.date = s(e.get("date"), 10);
            c.recipe = s(e.get("id"), 80);
            Object n = e.get("servings");
            c.servings = n instanceof Number ? Math.max(1, Math.min(MealPlan.MAX_SERVINGS * 3, ((Number) n).intValue())) : 1;
            if (!Dates.valid(c.date) || c.recipe.isEmpty()) continue;
            d.cooked.add(c);
            if (d.cooked.size() >= Leftovers.MAX_LOG) break;
        }
        Object bo = m.get("barcodes");
        if (bo instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) bo).entrySet()) {
                String code = Barcode.clean(e.getKey()), label = s(e.getValue(), 80);
                if (!Barcode.valid(code) || Normalizer.normalize(label).isEmpty()) continue;
                d.barcodes.put(code, label);
                if (d.barcodes.size() >= MAX_BARCODES) break;
            }
        }
        Object ph = m.get("photos");
        if (ph instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) ph).entrySet()) {
                String id = e.getKey();
                if (!(e.getValue() instanceof String) || id.isEmpty() || id.length() > 80) continue;
                String u = (String) e.getValue();
                if (!u.startsWith("data:image/jpeg;base64,") || u.length() > MAX_PHOTO_CHARS || !base64(u.substring(23))) continue;
                d.photos.put(id, u);
                if (d.photos.size() >= MAX_PHOTOS) break;
            }
        }
        if (m.get("theme") instanceof Map) d.theme = Palette.parseState(m.get("theme"));
        Set<String> rid = new LinkedHashSet<String>();
        for (Object o : list(m.get("recipes"))) {
            if (!(o instanceof Map)) continue;
            try {
                Recipe r = Recipe.fromMap((Map<String, Object>) o);
                if (!userRecipeId(r.id) || r.title.trim().isEmpty() || r.ingredients.isEmpty() || !rid.add(r.id)) continue;
                d.recipes.add(r);
                if (d.recipes.size() >= 2000) break;
            } catch (RuntimeException ignored) { }
        }
        return d;
    }

    private static boolean base64(String b) {
        if (b.isEmpty() || b.length() % 4 != 0) return false;
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || (c == '=' && i >= b.length() - 2))) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) { return o instanceof List ? (List<Object>) o : new ArrayList<Object>(); }

    /** Stage 3: short id strings ([a-z0-9-], at most 40 chars), no duplicates, at most max. */
    static List<Object> ids(Object o, int max) {
        List<Object> out = new ArrayList<Object>();
        for (String x : strings(o)) if (x.length() <= 40 && x.matches("[a-z0-9-]+") && !out.contains(x) && out.size() < max) out.add(x);
        return out;
    }

    /** Stage 3: starter feeds / bake log entries; keeps only known keys (strings up to 300 chars, whole numbers 0-10000) and a valid date d. */
    static List<Object> logList(Object o, String[] keys, int max) {
        List<Object> out = new ArrayList<Object>();
        for (Object e : (List<?>) o) {
            if (!(e instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) e;
            Object dv = m.get("d");
            if (!(dv instanceof String) || !((String) dv).matches("\\d{4}-\\d{2}-\\d{2}")) continue;
            Map<String, Object> c = new LinkedHashMap<String, Object>();
            for (String k : keys) {
                Object x = m.get(k);
                if (x instanceof String) c.put(k, ((String) x).length() > 300 ? ((String) x).substring(0, 300) : x);
                else if (x instanceof Number) c.put(k, (int) Math.max(0, Math.min(10000, ((Number) x).doubleValue())));
            }
            out.add(c);
        }
        while (out.size() > max) out.remove(0);
        return out;
    }

    private static List<String> strings(Object o) {
        List<String> out = new ArrayList<String>();
        for (Object x : list(o)) if (x instanceof String) out.add((String) x);
        return out;
    }

    /** "3 pantry items, 2 favorites, ..." summary used in the import confirmation. */
    public static String summary(Data d) {
        StringBuilder b = new StringBuilder();
        part(b, d.pantry.size(), "pantry item", "pantry items");
        part(b, d.favorites.size(), "favorite", "favorites");
        part(b, d.shopping.size(), "shopping item", "shopping items");
        part(b, d.ratings.size(), "rating or note", "ratings and notes");
        part(b, d.plan.size(), "planned meal", "planned meals");
        part(b, d.dislikes.size() + d.allergens.size(), "dislike/allergy", "dislikes/allergies");
        part(b, d.recipes.size(), "saved recipe", "saved recipes");
        if (d.theme != null) part(b, d.theme.custom.size(), "custom theme", "custom themes");
        part(b, d.prices.size(), "price", "prices");
        part(b, d.leftovers.size(), "leftover", "leftovers");
        part(b, d.cooked.size(), "cooking log entry", "cooking log entries");
        part(b, d.barcodes.size(), "saved barcode", "saved barcodes");
        part(b, d.photos.size(), "photo", "photos");
        return b.length() == 0 ? "nothing" : b.toString();
    }

    private static void part(StringBuilder b, int n, String one, String many) {
        if (n == 0) return;
        if (b.length() > 0) b.append(", ");
        b.append(n).append(' ').append(n == 1 ? one : many);
    }
}
