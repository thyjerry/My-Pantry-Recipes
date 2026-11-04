package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v2.0 regional prices. An optional US ZIP code maps (offline, by its first three digits) to a state and a Census
 * region; bundled BLS average retail prices for that region (or the U.S. average times a regional factor) replace the
 * hand-made default estimates. Prices the user typed always win. Shared with the web app (core20.js).
 */
public final class RegionalPrices {
    private RegionalPrices() { }

    public static final String US = "US", NE = "NE", MW = "MW", S = "S", W = "W";
    public static final String[] REGIONS = {US, NE, MW, S, W};
    public static final String[] REGION_NAMES = {"U.S.", "Northeast", "Midwest", "South", "West"};

    public static String regionName(String r) {
        for (int i = 0; i < REGIONS.length; i++) if (REGIONS[i].equals(r)) return REGION_NAMES[i];
        return "U.S.";
    }

    /** ZIP3 ranges "from-to:STATE" (USPS prefixes, approximate; unassigned prefixes fall back to national). */
    static final String ZIP3 = "005-005:NY 010-027:MA 028-029:RI 030-038:NH 039-049:ME 050-054:VT 055-055:MA 056-059:VT "
            + "060-069:CT 070-089:NJ 100-149:NY 150-196:PA 197-199:DE 200-200:DC 201-201:VA 202-205:DC 206-219:MD "
            + "220-246:VA 247-268:WV 270-289:NC 290-299:SC 300-319:GA 320-339:FL 341-349:FL 350-369:AL 370-385:TN "
            + "386-397:MS 398-399:GA 400-427:KY 430-459:OH 460-479:IN 480-499:MI 500-528:IA 530-549:WI 550-567:MN "
            + "569-569:DC 570-577:SD 580-588:ND 590-599:MT 600-629:IL 630-658:MO 660-679:KS 680-693:NE 700-714:LA "
            + "716-729:AR 730-732:OK 733-733:TX 734-749:OK 750-799:TX 800-816:CO 820-831:WY 832-838:ID 840-847:UT "
            + "850-865:AZ 870-884:NM 885-885:TX 889-898:NV 900-961:CA 967-968:HI 970-979:OR 980-994:WA 995-999:AK";

    static final String NE_STATES = "CT ME MA NH NJ NY PA RI VT", MW_STATES = "IL IN IA KS MI MN MO NE ND OH SD WI",
            S_STATES = "AL AR DE DC FL GA KY LA MD MS NC OK SC TN TX VA WV", W_STATES = "AK AZ CA CO HI ID MT NV NM OR UT WA WY";

    private static int[][] ranges;
    private static String[] rangeStates;

    private static synchronized void loadZip() {
        if (ranges != null) return;
        String[] parts = ZIP3.split(" ");
        int[][] r = new int[parts.length][2];
        String[] st = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            r[i][0] = Integer.parseInt(parts[i].substring(0, 3));
            r[i][1] = Integer.parseInt(parts[i].substring(4, 7));
            st[i] = parts[i].substring(8);
        }
        rangeStates = st;
        ranges = r;
    }

    /** A clean 5-digit ZIP (ZIP+4 accepted, the +4 dropped), or "" if it is not one. */
    public static String cleanZip(String zip) {
        if (zip == null) return "";
        String z = zip.trim();
        if (z.matches("\\d{5}(-\\d{4})?")) return z.substring(0, 5);
        return "";
    }

    /** Two-letter state for a ZIP, or "" (territories, military and unassigned prefixes). */
    public static String state(String zip) {
        String z = cleanZip(zip);
        if (z.isEmpty()) return "";
        loadZip();
        int p = Integer.parseInt(z.substring(0, 3));
        for (int i = 0; i < ranges.length; i++) if (p >= ranges[i][0] && p <= ranges[i][1]) return rangeStates[i];
        return "";
    }

    public static String regionOfState(String st) {
        if (st == null || st.length() != 2) return US;
        if (NE_STATES.contains(st)) return NE;
        if (MW_STATES.contains(st)) return MW;
        if (S_STATES.contains(st)) return S;
        if (W_STATES.contains(st)) return W;
        return US;
    }

    /** Census region for a ZIP: NE, MW, S, W, or US when unknown or not set. */
    public static String region(String zip) { return regionOfState(state(zip)); }

    /** One BLS item: ingredient keys, the amount and unit the price is for, and prices by region (NaN = not published). */
    public static final class Item {
        public String name;
        public List<String> keys = new ArrayList<String>();
        public double amount;
        public String unit;
        public final double[] price = new double[5];
    }

    public static final class Table {
        public String date = "";
        public final List<Item> items = new ArrayList<Item>();
        private double[] factors;

        /** Regional factor: median of region/US over items published for both (1 for US or with no data). */
        public synchronized double factor(String region) {
            if (factors == null) {
                double[] f = new double[5];
                f[0] = 1;
                for (int r = 1; r < 5; r++) {
                    List<Double> rs = new ArrayList<Double>();
                    for (Item it : items) if (!Double.isNaN(it.price[0]) && !Double.isNaN(it.price[r]) && it.price[0] > 0) rs.add(it.price[r] / it.price[0]);
                    f[r] = median(rs);
                }
                factors = f;
            }
            int k = idx(region);
            return k < 0 ? 1 : factors[k];
        }
    }

    static int idx(String region) {
        for (int i = 0; i < REGIONS.length; i++) if (REGIONS[i].equals(region)) return i;
        return -1;
    }

    static double median(List<Double> v) {
        if (v.isEmpty()) return 1;
        Collections.sort(v);
        int n = v.size();
        double m = n % 2 == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2;
        return Math.round(m * 1000) / 1000.0;
    }

    public static Table parse(String tsv) {
        Table t = new Table();
        if (tsv == null) return t;
        for (String line : tsv.split("\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] f = line.split("\t", -1);
            if (f[0].equals("@date") && f.length > 1) { t.date = f[1].trim(); continue; }
            if (f.length < 9) continue;
            Item it = new Item();
            it.name = f[0].trim();
            for (String k : f[1].split(",")) if (!k.trim().isEmpty()) it.keys.add(k.trim());
            try {
                it.amount = Double.parseDouble(f[2].trim());
                it.unit = f[3].trim();
                for (int r = 0; r < 5; r++) {
                    String v = f[4 + r].trim();
                    it.price[r] = v.equals("-") || v.isEmpty() ? Double.NaN : Double.parseDouble(v);
                }
            } catch (NumberFormatException e) { continue; }
            if (!Cost.validUnit(it.unit) || it.amount <= 0 || Double.isNaN(it.price[0])) continue;
            t.items.add(it);
        }
        return t;
    }

    /** Where a default price came from. */
    public static final class Source {
        public final String kind;    // "regional", "us", "us-factor", "estimate", "estimate-factor"
        public final String label;
        Source(String kind, String label) { this.kind = kind; this.label = label; }
    }

    /** The default-price map for a region: BLS regional > BLS U.S. x factor > hand-made estimate x factor. */
    public static Map<String, Cost.Price> defaults(Table t, String region, Map<String, Cost.Price> estimates, Map<String, Source> sources) {
        Map<String, Cost.Price> out = new LinkedHashMap<String, Cost.Price>();
        int k = Math.max(0, idx(region));
        double f = t.factor(region);
        String rn = regionName(region);
        String fs = String.valueOf(f);
        if (estimates != null) for (Map.Entry<String, Cost.Price> e : estimates.entrySet()) {
            Cost.Price p = e.getValue();
            out.put(e.getKey(), k == 0 ? p : new Cost.Price(round(p.price * f), p.amount, p.unit));
            if (sources != null) sources.put(e.getKey(), k == 0 ? new Source("estimate", "Built-in rough estimate")
                    : new Source("estimate-factor", "Built-in rough estimate \u00d7 " + rn + " factor (" + fs + ")"));
        }
        for (Item it : t.items) {
            double v; Source src;
            if (k > 0 && !Double.isNaN(it.price[k])) {
                v = it.price[k];
                src = new Source("regional", "BLS " + rn + " regional average, " + t.date);
            } else if (k > 0) {
                v = round(it.price[0] * f);
                src = new Source("us-factor", "BLS U.S. average, " + t.date + " \u00d7 " + rn + " factor (" + fs + ")");
            } else {
                v = it.price[0];
                src = new Source("us", "BLS U.S. average, " + t.date);
            }
            for (String key : it.keys) {
                out.put(key, new Cost.Price(v, it.amount, it.unit));
                if (sources != null) sources.put(key, src);
            }
        }
        return out;
    }

    static double round(double v) { return Math.round(v * 1000) / 1000.0; }

    /** How many ingredient keys have a real BLS price for the region (regional or U.S.), and how many are regional. */
    public static int[] coverage(Table t, String region) {
        int k = Math.max(0, idx(region)), keys = 0, regional = 0;
        for (Item it : t.items) {
            keys += it.keys.size();
            if (k > 0 && !Double.isNaN(it.price[k])) regional += it.keys.size();
        }
        return new int[]{keys, regional};
    }

    public static final String NOTE = "Default prices are averages, not your shop's prices. BLS publishes about 30 everyday foods "
            + "for the U.S. and four regions (many items only nationally); everything else is a rough built-in estimate scaled by a "
            + "regional factor. Prices you enter always win.";

    // ---------------- budget ----------------

    /** True when the per-serving estimate is shown and within the budget (budget <= 0 = off). */
    public static boolean withinBudget(Cost.Estimate e, double budgetPerServing) {
        if (budgetPerServing <= 0) return true;
        if (e == null || !e.show()) return true; // v2.0 S3: unknown cost is kept (shown as "price unknown")
        return e.perServing <= budgetPerServing + 1e-9;
    }

    // ---------------- Open Prices (open data, ODbL) ----------------

    /** One crowd-sourced price from Open Prices. */
    public static final class OpenPrice {
        public double price;
        public String currency = "", date = "", store = "", city = "", state = "";
        public boolean nearby;
    }

    /**
     * Parses an Open Prices /api/v1/prices response; keeps USD prices with a date, nearby ones (same region by
     * the location's ZIP) first, then newest. Never throws.
     */
    @SuppressWarnings("unchecked")
    public static List<OpenPrice> parseOpenPrices(String json, String region, int max) {
        List<OpenPrice> out = new ArrayList<OpenPrice>();
        try {
            Object o = Json.parse(json);
            if (!(o instanceof Map)) return out;
            Object items = ((Map<String, Object>) o).get("items");
            if (!(items instanceof List)) return out;
            for (Object x : (List<Object>) items) {
                if (!(x instanceof Map)) continue;
                Map<String, Object> m = (Map<String, Object>) x;
                Object pv = m.get("price");
                if (!(pv instanceof Number)) continue;
                OpenPrice p = new OpenPrice();
                p.price = ((Number) pv).doubleValue();
                p.currency = Json.str(m, "currency", "");
                p.date = Json.str(m, "date", "");
                if (!"USD".equals(p.currency) || !Dates.valid(p.date) || p.price <= 0 || p.price > 10000) continue;
                Object loc = m.get("location");
                if (loc instanceof Map) {
                    Map<String, Object> l = (Map<String, Object>) loc;
                    p.store = trim(Json.str(l, "osm_name", ""), 60);
                    p.city = trim(Json.str(l, "osm_address_city", ""), 60);
                    String cc = Json.str(l, "osm_address_country_code", "");
                    if (!cc.isEmpty() && !"US".equalsIgnoreCase(cc)) continue;
                    String pc = Json.str(l, "osm_address_postcode", "");
                    p.state = state(pc);
                    p.nearby = !US.equals(region) && !p.state.isEmpty() && regionOfState(p.state).equals(region);
                }
                out.add(p);
            }
        } catch (Exception ignored) { }
        Collections.sort(out, new java.util.Comparator<OpenPrice>() {
            public int compare(OpenPrice a, OpenPrice b) {
                if (a.nearby != b.nearby) return a.nearby ? -1 : 1;
                return b.date.compareTo(a.date);
            }
        });
        return out.size() > max ? new ArrayList<OpenPrice>(out.subList(0, max)) : out;
    }

    private static String trim(String s, int n) { s = s == null ? "" : s.trim(); return s.length() > n ? s.substring(0, n) : s; }

    /** "https://prices.openfoodfacts.org/api/v1/prices?product_code=...": only the barcode digits are sent. */
    public static String openPricesUrl(String barcode) {
        String d = barcode == null ? "" : barcode.replaceAll("[^0-9]", "");
        if (d.length() < 8 || d.length() > 14) return "";
        return "https://prices.openfoodfacts.org/api/v1/prices?product_code=" + d + "&order_by=-date&size=50";
    }
}
