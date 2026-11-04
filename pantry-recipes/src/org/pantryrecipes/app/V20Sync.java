package org.pantryrecipes.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.pantryrecipes.app.core.Backup;
import org.pantryrecipes.app.core.Dates;
import org.pantryrecipes.app.core.MealPlan;
import org.pantryrecipes.app.core.SyncCrypto;
import org.pantryrecipes.app.core.SyncDoc;
import org.pantryrecipes.app.core.SyncWire;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * v2.0: Android <-> Android sync over the local Wi-Fi. Pair once with a 10-character code (or find the other device
 * through NSD / type its address), then "Sync now". Per-item last-writer-wins with tombstones and a conflict log;
 * HMAC-authenticated, encrypted messages (core SyncCrypto/SyncWire). Everything in the backup is synced; secrets
 * (API key, pairing secrets) never are. The server runs only while the Sync screen is open or the app is in front
 * with at least one paired device - no background service. Tested with simulated devices only.
 */
final class V20Sync implements SyncWire.Device {
    static final int PORT = 47821;
    static final long PAIR_MS = 2 * 60 * 1000L;
    private static V20Sync inst;

    final Context app;
    final SharedPreferences prefs;
    SyncWire.Server server;
    NsdManager.RegistrationListener reg;
    String code = "";
    long codeUntil;
    int fails;
    volatile String status = "";
    Runnable onChange;

    private V20Sync(Context c) {
        app = c.getApplicationContext();
        prefs = Store.get(c).prefs;
        if (prefs.getString("syncId", "").isEmpty()) prefs.edit().putString("syncId", SyncCrypto.hex(SyncCrypto.random(8))).commit();
    }

    static synchronized V20Sync get(Context c) {
        if (inst == null) inst = new V20Sync(c);
        return inst;
    }

    // ---------------- Device ----------------
    public String id() { return prefs.getString("syncId", ""); }
    public String name() { String n = prefs.getString("syncName", ""); return n.isEmpty() ? (Build.MANUFACTURER + " " + Build.MODEL).trim() : n; }
    public synchronized String openPairingCode() { if (System.currentTimeMillis() > codeUntil) code = ""; return code; }
    public synchronized void pairingAttempt(boolean ok) { if (ok || ++fails >= SyncWire.MAX_PAIR_ATTEMPTS) code = ""; }

    public synchronized void paired(String peerId, String peerName, byte[] secret) {
        List<SyncWire.Peer> ps = peers();
        SyncWire.Peer p = find(ps, peerId);
        if (p == null) { p = new SyncWire.Peer(); p.id = peerId; ps.add(p); }
        p.name = peerName;
        p.secret = SyncCrypto.b64(secret);
        savePeers(ps);
        status = "Paired with " + peerName;
        changed();
    }

    public synchronized byte[] secretFor(String peerId) {
        SyncWire.Peer p = find(peers(), peerId);
        try { return p == null || p.secret.isEmpty() ? null : SyncCrypto.unb64(p.secret); } catch (SyncCrypto.BadMessage e) { return null; }
    }

    public synchronized TreeMap<String, SyncDoc.Entry> mergeFrom(String peerId, TreeMap<String, SyncDoc.Entry> remote, long now) {
        TreeMap<String, SyncDoc.Entry> state = commit(now);
        List<SyncDoc.Conflict> cs = conflicts();
        state = SyncDoc.merge(state, remote, prefs.getLong("syncSince:" + peerId, 0), cs, now);
        adopt(peerId, state, cs, now);
        SyncWire.Peer p = find(peers(), peerId);
        status = "Synced with " + (p == null ? "a paired device" : p.name);
        return state;
    }

    // ---------------- state ----------------
    synchronized List<SyncWire.Peer> peers() { return SyncWire.readPeers(prefs.getString("syncPeers", "[]")); }
    synchronized void savePeers(List<SyncWire.Peer> ps) { prefs.edit().putString("syncPeers", SyncWire.writePeers(ps)).commit(); }
    static SyncWire.Peer find(List<SyncWire.Peer> ps, String id) { for (SyncWire.Peer p : ps) if (p.id.equals(id)) return p; return null; }

    List<SyncDoc.Conflict> conflicts() { return SyncDoc.readConflicts(prefs.getString("syncConflicts", "[]")); }

    TreeMap<String, SyncDoc.Entry> state() {
        java.io.File f = new java.io.File(app.getFilesDir(), "sync-state.json");
        if (!f.exists()) return new TreeMap<String, SyncDoc.Entry>();
        try { return SyncDoc.readDoc(new String(readAll(f), "UTF-8")); } catch (Exception e) { return new TreeMap<String, SyncDoc.Entry>(); }
    }

    void saveState(Map<String, SyncDoc.Entry> st) {
        try {
            java.io.File f = new java.io.File(app.getFilesDir(), "sync-state.json"), tmp = new java.io.File(app.getFilesDir(), "sync-state.tmp");
            java.io.FileOutputStream o = new java.io.FileOutputStream(tmp);
            o.write(SyncDoc.writeDoc(st).getBytes("UTF-8"));
            o.close();
            tmp.renameTo(f);
        } catch (Exception ignored) { }
    }

    static byte[] readAll(java.io.File f) throws java.io.IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toByteArray();
    }

    /** Everything a backup holds (no photos, no secrets), as sync items. */
    Backup.Data export() {
        Store store = Store.get(app);
        Repo repo = Repo.get(app);
        Set<String> ref = new HashSet<String>(store.favorites);
        for (MealPlan.Entry e : store.planEntries()) ref.add(e.id);
        ref.addAll(store.ratings.keySet());
        Backup.Data d = store.exportBackup(Extras.version(app), repo.recipesForBackup(ref));
        V16.exportExtras(app, d, false);
        V17.exportExtras(app, d);
        V20.exportExtras(app, d);
        return d;
    }

    synchronized TreeMap<String, SyncDoc.Entry> commit(long now) {
        TreeMap<String, SyncDoc.Entry> st = state();
        SyncDoc.track(st, SyncDoc.items(export()), now, id());
        saveState(st);
        return st;
    }

    synchronized void adopt(String peerId, TreeMap<String, SyncDoc.Entry> merged, List<SyncDoc.Conflict> cs, long now) {
        TreeMap<String, String> before = SyncDoc.items(export());
        saveState(merged);
        if (cs.size() > 50) cs = new ArrayList<SyncDoc.Conflict>(cs.subList(cs.size() - 50, cs.size()));
        prefs.edit().putString("syncConflicts", SyncDoc.writeConflicts(cs)).putLong("syncSince:" + peerId, now).commit();
        try {
            Backup.Data d = Backup.parse(SyncDoc.assemble(merged, Extras.version(app), "android", Dates.today()));
            if (!SyncDoc.items(d).equals(before)) {
                Store.get(app).importBackup(d);
                V16.importExtras(app, d);
                V17.importExtras(app, d);
                V20.importExtras(app, d);
                Repo.get(app).reload();
            }
        } catch (Exception e) {
            status = "Sync received data this version could not apply.";
        }
        changed();
    }

    void changed() {
        final Runnable r = onChange;
        if (r != null) new Handler(Looper.getMainLooper()).post(r);
    }

    // ---------------- server + NSD ----------------
    synchronized void startServer() {
        if (server != null) return;
        try { server = new SyncWire.Server(this, PORT); } catch (Exception e) {
            try { server = new SyncWire.Server(this, 0); } catch (Exception e2) { status = "Could not listen on the network."; return; }
        }
        try {
            NsdServiceInfo si = new NsdServiceInfo();
            si.setServiceName("My Pantry Recipes " + name());
            si.setServiceType(SyncWire.SERVICE_TYPE);
            si.setPort(server.port());
            reg = new NsdManager.RegistrationListener() {
                public void onRegistrationFailed(NsdServiceInfo s, int e) { }
                public void onUnregistrationFailed(NsdServiceInfo s, int e) { }
                public void onServiceRegistered(NsdServiceInfo s) { }
                public void onServiceUnregistered(NsdServiceInfo s) { }
            };
            ((NsdManager) app.getSystemService(Context.NSD_SERVICE)).registerService(si, NsdManager.PROTOCOL_DNS_SD, reg);
        } catch (Exception ignored) { reg = null; }
    }

    synchronized void stopServer() {
        if (server != null) { server.close(); server = null; }
        if (reg != null) { try { ((NsdManager) app.getSystemService(Context.NSD_SERVICE)).unregisterService(reg); } catch (Exception ignored) { } reg = null; }
    }

    static void resume(Context c) { V20Sync s = get(c); if (!s.peers().isEmpty()) s.startServer(); }
    static void pause(Context c) { if (inst != null && inst.onChange == null) inst.stopServer(); }

    static String myAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress ad : Collections.list(ni.getInetAddresses())) if (ad instanceof Inet4Address && ad.isSiteLocalAddress()) return ad.getHostAddress();
            }
        } catch (Exception ignored) { }
        return "";
    }

    // ---------------- client ----------------
    void syncNow(final SyncWire.Peer p0) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            List<SyncWire.Peer> ps = peers();
            SyncWire.Peer p = find(ps, p0.id);
            if (p == null) return;
            try {
                byte[] sec = SyncCrypto.unb64(p.secret);
                TreeMap<String, SyncDoc.Entry> st = commit(now);
                Object[] r = SyncWire.sync(p.host, p.port, p.id, sec, id(), st);
                @SuppressWarnings("unchecked") TreeMap<String, SyncDoc.Entry> merged = (TreeMap<String, SyncDoc.Entry>) r[1];
                adopt(p.id, merged, conflicts(), now);
                p.lastSync = now;
                p.lastResult = "OK";
                status = "Synced with " + p.name;
            } catch (SyncWire.SyncException e) {
                p.lastResult = e.getMessage();
                status = e.getMessage();
            } catch (Exception e) {
                p.lastResult = "Could not reach " + p.name;
                status = p.lastResult;
            }
            savePeers(ps);
            changed();
        }).start();
    }

    void join(final String host, final int port, final String pairCode) {
        new Thread(() -> {
            try {
                String[] r = SyncWire.pair(host, port, pairCode, this);
                List<SyncWire.Peer> ps = peers();
                SyncWire.Peer p = find(ps, r[0]);
                if (p != null) { p.host = host; p.port = port; savePeers(ps); }
                status = "Paired with " + r[1] + ". Tap Sync now.";
            } catch (SyncWire.SyncException e) {
                status = e.getMessage();
            } catch (Exception e) {
                status = "Could not reach " + host + ":" + port;
            }
            changed();
        }).start();
    }

    // ---------------- UI ----------------
    static void dialog(final MainActivity a) {
        final V20Sync s = get(a);
        s.startServer();
        final LinearLayout box = V20.box(a);
        final List<String[]> found = new ArrayList<String[]>();   // name, host, port
        final NsdManager nsd = (NsdManager) a.getSystemService(Context.NSD_SERVICE);
        final NsdManager.DiscoveryListener[] disc = new NsdManager.DiscoveryListener[1];
        final Runnable[] render = new Runnable[1];
        render[0] = () -> {
            box.removeAllViews();
            String ip = myAddress();
            box.addView(Ui.body(a, "This device: " + s.name() + (s.server == null ? " \u00b7 not listening" : " \u00b7 " + (ip.isEmpty() ? "no Wi-Fi address" : ip + ":" + s.server.port()))));
            if (!s.status.isEmpty()) { TextView st = Ui.body(a, s.status); st.setTextColor(Ui.GREEN_DARK); box.addView(st, Ui.lpMargins(a, -1, -2, 0, 6, 0, 0)); }
            String code = s.openPairingCode();
            if (!code.isEmpty()) {
                TextView ct = Ui.title(a, "Pairing code: " + code.substring(0, 5) + " " + code.substring(5), 20);
                box.addView(ct, Ui.lpMargins(a, -1, -2, 0, 10, 0, 0));
                box.addView(Ui.caption(a, "On the other device: Sync \u203a Join, pick this device (or type " + ip + ":" + (s.server == null ? PORT : s.server.port()) + ") and type the code. It closes in 2 minutes or after 5 wrong codes."));
            } else {
                box.addView(V20.action(a, "Pair a device (show a code)", R.drawable.ic_add, v -> {
                    synchronized (s) { s.code = SyncCrypto.newCode(); s.codeUntil = System.currentTimeMillis() + PAIR_MS; s.fails = 0; }
                    render[0].run();
                }), V20.rowLp(a));
            }
            box.addView(Ui.sectionLabel(a, "Join a device"), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
            for (final String[] f : new ArrayList<String[]>(found)) box.addView(V20.action(a, f[0] + " \u00b7 " + f[1], R.drawable.ic_cloud, v -> joinDialog(a, s, f[1], f[2])), V20.rowLp(a));
            box.addView(V20.action(a, "Type an address\u2026", R.drawable.ic_list, v -> joinDialog(a, s, "", String.valueOf(PORT))), V20.rowLp(a));
            List<SyncWire.Peer> ps = s.peers();
            box.addView(Ui.sectionLabel(a, "Paired devices " + ps.size()), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
            for (final SyncWire.Peer p : ps) {
                String last = p.lastSync > 0 ? "last sync " + new java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.US).format(new java.util.Date(p.lastSync)) : "never synced";
                box.addView(Ui.body(a, p.name + " \u00b7 " + last + (p.lastResult.isEmpty() || "OK".equals(p.lastResult) ? "" : " \u00b7 " + p.lastResult)));
                LinearLayout row = Ui.row(a);
                if (!p.host.isEmpty()) row.addView(V20.action(a, "Sync now", R.drawable.ic_cloud, v -> { s.status = "Syncing\u2026"; render[0].run(); s.syncNow(p); }), new LinearLayout.LayoutParams(-2, V20.dp(a, 38)));
                row.addView(Ui.space(a, 8, 1));
                row.addView(V20.action(a, "Forget", R.drawable.ic_remove, v -> { List<SyncWire.Peer> q = s.peers(); q.remove(find(q, p.id)); s.savePeers(q); render[0].run(); }), new LinearLayout.LayoutParams(-2, V20.dp(a, 38)));
                box.addView(row);
                if (p.host.isEmpty()) box.addView(Ui.caption(a, "This device answers when " + p.name + " taps Sync now."));
            }
            List<SyncDoc.Conflict> cs = s.conflicts();
            if (!cs.isEmpty()) {
                box.addView(Ui.sectionLabel(a, "Conflict log " + cs.size()), Ui.lpMargins(a, -2, -2, 0, 14, 0, 4));
                for (int i = cs.size() - 1; i >= Math.max(0, cs.size() - 10); i--) {
                    SyncDoc.Conflict c = cs.get(i);
                    box.addView(Ui.caption(a, SyncDoc.describe(c.key) + ": kept the newer change (" + c.keptDevice + "), replaced the one from " + c.lostDevice + "."));
                }
            }
            box.addView(Ui.caption(a, "Security: devices pair with a one-time code; every message is encrypted and signed (HMAC) with the shared key, and "
                    + "only works on your local network. Nothing goes through the internet or any account. API keys and pairing secrets are never synced. "
                    + "Note: tested with simulated devices only, not yet on two real devices."), Ui.lpMargins(a, -1, -2, 0, 14, 0, 0));
        };
        s.onChange = () -> { render[0].run(); a.refreshAfterV20(); };
        render[0].run();
        try {
            disc[0] = new NsdManager.DiscoveryListener() {
                public void onStartDiscoveryFailed(String t, int e) { }
                public void onStopDiscoveryFailed(String t, int e) { }
                public void onDiscoveryStarted(String t) { }
                public void onDiscoveryStopped(String t) { }
                public void onServiceLost(NsdServiceInfo si) { }
                public void onServiceFound(NsdServiceInfo si) {
                    if (si.getServiceName().contains(s.name())) return;
                    nsd.resolveService(si, new NsdManager.ResolveListener() {
                        public void onResolveFailed(NsdServiceInfo x, int e) { }
                        public void onServiceResolved(final NsdServiceInfo x) {
                            if (x.getHost() == null) return;
                            final String[] f = {x.getServiceName().replace("My Pantry Recipes ", ""), x.getHost().getHostAddress(), String.valueOf(x.getPort())};
                            a.runOnUiThread(() -> { for (String[] g : found) if (g[1].equals(f[1])) return; found.add(f); render[0].run(); });
                        }
                    });
                }
            };
            nsd.discoverServices(SyncWire.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, disc[0]);
        } catch (Exception e) { disc[0] = null; }
        AlertDialog d = V20.show(a, "Sync (Wi-Fi, Android)", box, null, null);
        d.setOnDismissListener(x -> {
            s.onChange = null;
            if (disc[0] != null) try { nsd.stopServiceDiscovery(disc[0]); } catch (Exception ignored) { }
            if (s.peers().isEmpty()) s.stopServer();
        });
    }

    static void joinDialog(final MainActivity a, final V20Sync s, String host, String port) {
        LinearLayout box = V20.box(a);
        final EditText h = V20.field(a, box, "Address, e.g. 192.168.1.20", host, true);
        final EditText p = V20.field(a, box, "Port", port, true);
        p.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText c = V20.field(a, box, "10-character code from the other device", "", true);
        V20.show(a, "Join a device", box, "Pair", () -> {
            s.status = "Pairing\u2026";
            s.changed();
            s.join(h.getText().toString().trim(), V20.num(p, PORT), c.getText().toString());
        });
    }
}
