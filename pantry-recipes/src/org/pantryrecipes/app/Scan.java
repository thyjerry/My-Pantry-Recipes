package org.pantryrecipes.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.pantryrecipes.app.core.Barcode;
import org.pantryrecipes.app.core.Normalizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Barcode to pantry. There is no camera decoder in this app: scanning hands off to an installed barcode app through the
 * standard ZXing SCAN intent (no CAMERA permission, no bundled ZXing code), or the user types the digits. The code is
 * looked up on Open Food Facts (API v2, ODbL) and mapped to the closest catalog item, which the user confirms or edits.
 */
final class Scan {
    private Scan() { }

    static final String SCAN_ACTION = "com.google.zxing.client.android.SCAN";
    static final String OFF_HOST = "world.openfoodfacts.org";
    /** SHA-256 of the bundled ISRG Root X1 certificate (Let's Encrypt), checked before it is used. */
    static final String ISRG_X1_SHA256 = "96bcec06264976f37460779acf28c5a7cfe8a3c0aae11a8ffcee05c0bddf08c6";

    static int dp(Context c, float v) { return Ui.dp(c, v); }

    static void open(final MainActivity a) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), 0);
        box.addView(Ui.caption(a, "This app has no built-in camera scanner. \u201cScan\u201d opens a barcode app you have installed "
                + "(for example the open-source Binary Eye or ZXing Barcode Scanner). Or type the digits under the barcode."));
        final TextView scan = Ui.button(a, "Scan with a barcode app", R.drawable.ic_barcode, true);
        box.addView(scan, Ui.lpMargins(a, -2, dp(a, 48), 0, 12, 0, 8));
        final EditText code = V16.field(a, "Barcode number (EAN/UPC)", "", InputType.TYPE_CLASS_NUMBER);
        box.addView(code);
        box.addView(Ui.caption(a, Barcode.ATTRIBUTION), Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
        final android.app.AlertDialog[] self = new android.app.AlertDialog[1];
        scan.setOnClickListener(v -> {
            if (self[0] != null) self[0].dismiss();
            Intent i = new Intent(SCAN_ACTION);
            i.putExtra("SCAN_FORMATS", "EAN_13,EAN_8,UPC_A,UPC_E");
            i.putExtra("SCAN_MODE", "PRODUCT_MODE");
            try {
                a.startActivityForResult(i, V16.REQ_SCAN);
            } catch (ActivityNotFoundException e) {
                new Ui.Dialog(a).setTitle("No barcode app")
                        .setMessage("No app that handles the standard SCAN request is installed. Install one (for example Binary Eye from F-Droid), or type the number instead.")
                        .setPositiveButton("Type number", (d, w) -> open(a)).setNegativeButton("Close", null).show();
            }
        });
        self[0] = new Ui.Dialog(a).setTitle("Add by barcode").setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Look up", (d, w) -> lookup(a, code.getText().toString())).show();
    }

    static void onResult(MainActivity a, Intent data) {
        String c = data == null ? null : data.getStringExtra("SCAN_RESULT");
        if (c == null || c.isEmpty()) { Ui.toastShort(a, "No barcode read"); return; }
        lookup(a, c);
    }

    static void lookup(final MainActivity a, String raw) {
        final String c = Barcode.clean(raw == null ? "" : raw);
        if (!Barcode.valid(c)) {
            new Ui.Dialog(a).setTitle("Not a valid barcode")
                    .setMessage("That isn\u2019t a valid EAN/UPC barcode (8, 12 or 13 digits with a correct check digit).")
                    .setPositiveButton("Try again", (d, w) -> open(a)).setNegativeButton("Close", null).show();
            return;
        }
        String cached = V16.data(a).barcodes.get(c);
        if (cached != null) { confirm(a, c, null, cached, "Remembered from an earlier scan."); return; }
        final android.app.ProgressDialog pd = new android.app.ProgressDialog(a, Ui.dialogTheme());
        pd.setMessage("Looking up " + c + " on Open Food Facts\u2026");
        pd.setCancelable(true);
        pd.show();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            String json = null;
            boolean failed = false;
            try { json = get(a, Barcode.url(c)); } catch (Exception e) { failed = true; }
            final boolean ff = failed;
            // v1.8: parse and match off the UI thread (the catalog scan is slow on old tablets)
            final Barcode.Product p = json == null ? null : Barcode.parse(json);
            final String best = p == null ? "" : Ui.cap(Barcode.match(p));
            main.post(() -> {
                if (!pd.isShowing() || a.isFinishing()) return;
                pd.dismiss();
                if (ff) { confirm(a, c, null, "", "Couldn\u2019t reach Open Food Facts. Check your connection, or type the item name."); return; }
                if (p == null) { confirm(a, c, null, "", "Open Food Facts doesn\u2019t know this barcode. Type what it is and it will be remembered."); return; }
                confirm(a, c, p, best, null);
            });
        }, "barcode").start();
    }

    /** The user confirms or edits the pantry name (and optionally keeps the package size as the amount). */
    static void confirm(final MainActivity a, final String code, final Barcode.Product p, String suggestion, String note) {
        LinearLayout box = Ui.column(a);
        box.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), 0);
        if (p != null) {
            box.addView(Ui.text(a, p.title(), 16, Ui.TEXT, Ui.MEDIUM));
            box.addView(Ui.caption(a, suggestion.isEmpty() ? "No close match in the ingredient list; type a name." : "Closest pantry item: " + suggestion));
        }
        if (note != null) box.addView(Ui.caption(a, note));
        box.addView(Ui.sectionLabel(a, "Pantry item"), Ui.lpMargins(a, -1, -2, 0, 12, 0, 0));
        final EditText name = V16.field(a, "e.g. Peanut butter", suggestion, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(name);
        if (p != null) {
            // v1.8: other close matches, one tap to use instead (the user still confirms with "Add to pantry")
            java.util.List<String> alts = new java.util.ArrayList<String>();
            for (String m : Barcode.matches(p, 4)) if (!Ui.cap(m).equalsIgnoreCase(suggestion)) alts.add(Ui.cap(m));
            if (!alts.isEmpty()) {
                box.addView(Ui.caption(a, "Or did you mean:"), Ui.lpMargins(a, -1, -2, 0, 8, 0, 4));
                FlowLayout fl = new FlowLayout(a, dp(a, 6), dp(a, 6));
                for (final String alt : alts) {
                    TextView c = Ui.chip(a, alt, false);
                    c.setOnClickListener(v -> { name.setText(alt); name.setSelection(alt.length()); });
                    fl.addView(c);
                }
                box.addView(fl);
            }
        }
        final CheckBox qty = new CheckBox(a);
        if (p != null && !p.quantity.isEmpty()) {
            qty.setText("Set the amount to " + p.quantity);
            qty.setTextColor(Ui.TEXT);
            qty.setChecked(true);
            box.addView(qty);
        }
        box.addView(Ui.caption(a, "Barcode " + code + (p != null ? " \u00b7 " + Barcode.ATTRIBUTION : "")), Ui.lpMargins(a, -1, -2, 0, 8, 0, 0));
        new Ui.Dialog(a).setTitle(p != null ? "Found it" : "Name this item").setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add to pantry", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) { Ui.toastShort(a, "Type a name for the pantry item"); return; }
                    Store store = Store.get(a);
                    boolean added = store.addToPantry(n, false);
                    String key = Normalizer.normalize(n);
                    Store.PantryItem it = store.pantry.get(key);
                    if (it != null && p != null && !p.quantity.isEmpty() && qty.isChecked()) store.setPantryDetails(key, it.useBy, p.quantity);
                    if (V16.data(a).barcodes.size() < org.pantryrecipes.app.core.Backup.MAX_BARCODES || V16.data(a).barcodes.containsKey(code)) {
                        V16.data(a).barcodes.put(code, Ui.cap(n));
                        V16.save(a);
                    }
                    Ui.toastShort(a, added ? "Added " + Ui.cap(n) : Ui.cap(n) + " is already in your pantry");
                    a.pantryChangedExternally();
                }).show();
    }

    // ---------------- network ----------------
    static String get(Context c, String url) throws IOException {
        URL u = new URL(url);
        if (!"https".equals(u.getProtocol()) || !OFF_HOST.equals(u.getHost())) throw new IOException("unexpected host");
        HttpsURLConnection con = (HttpsURLConnection) u.openConnection();
        // Android 7.0 and older don't trust ISRG Root X1 (Let's Encrypt); add it for this one host only
        if (Build.VERSION.SDK_INT < 25) {
            SSLSocketFactory f = isrgFactory(c);
            if (f != null) con.setSSLSocketFactory(f);
        }
        con.setConnectTimeout(10000);
        con.setReadTimeout(15000);
        con.setRequestProperty("User-Agent", "PantryRecipes/" + Extras.version(c) + " (Android; open source)");
        con.setRequestProperty("Accept", "application/json");
        try {
            int code = con.getResponseCode();
            if (code == 404) return "{\"status\":0}";
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            InputStream in = con.getInputStream();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) { total += n; if (total > 2_000_000) throw new IOException("too large"); b.write(buf, 0, n); }
            in.close();
            return b.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    private static SSLSocketFactory isrg;

    /** System trust plus the bundled ISRG Root X1 (pinned by fingerprint). Certificates are always verified; never trust-all. */
    static synchronized SSLSocketFactory isrgFactory(Context c) {
        if (isrg != null) return isrg;
        try {
            InputStream in = c.getAssets().open("isrg-root-x1.pem");
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            in.close();
            if (!ISRG_X1_SHA256.equals(hex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())))) return null;
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("isrg-root-x1", cert);
            final X509TrustManager extra = trustManager(ks), system = trustManager(null);
            if (extra == null || system == null) return null;
            X509TrustManager both = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String auth) throws CertificateException { system.checkClientTrusted(chain, auth); }
                @Override public void checkServerTrusted(X509Certificate[] chain, String auth) throws CertificateException {
                    try { system.checkServerTrusted(chain, auth); } catch (CertificateException e) { extra.checkServerTrusted(chain, auth); }
                }
                @Override public X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] s = system.getAcceptedIssuers(), o = new X509Certificate[s.length + 1];
                    System.arraycopy(s, 0, o, 0, s.length);
                    o[s.length] = extra.getAcceptedIssuers()[0];
                    return o;
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{both}, null);
            isrg = ctx.getSocketFactory();
            return isrg;
        } catch (Exception e) {
            return null;
        }
    }

    static X509TrustManager trustManager(KeyStore ks) throws Exception {
        TrustManagerFactory f = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        f.init(ks);
        for (TrustManager t : f.getTrustManagers()) if (t instanceof X509TrustManager) return (X509TrustManager) t;
        return null;
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format(java.util.Locale.US, "%02x", x & 0xFF));
        return s.toString();
    }
}
