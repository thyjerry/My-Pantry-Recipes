package org.pantryrecipes.app.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * v2.0 Wi-Fi sync wire protocol: a tiny HTTP/1.0 server on the local network (one request per connection) and the
 * matching client. Pairing: GET /hello, then POST /pair with a random 256-bit secret sealed under a key stretched from
 * the code shown on the other screen. Sync: POST /sync with the sealed, gzipped item document; the answer is the
 * merged document, sealed and bound to the request nonce. No cloud, no third-party code.
 */
public final class SyncWire {
    private SyncWire() { }

    public static final int PROTO = 1;
    public static final String SERVICE_TYPE = "_mypantry._tcp.";
    public static final int MAX_BODY = 24 * 1024 * 1024, MAX_PLAIN = 64 * 1024 * 1024;
    public static final long MAX_SKEW_MS = 15 * 60 * 1000L;
    public static final int MAX_PAIR_ATTEMPTS = 5;
    static final int TIMEOUT_MS = 20000;

    /** What a device provides to the server and client. Methods may be called from server threads. */
    public interface Device {
        String id();
        String name();
        /** The pairing code while this device shows it ("" when pairing is closed). */
        String openPairingCode();
        /** Called on every pairing attempt; after MAX_PAIR_ATTEMPTS failures the device should close pairing. */
        void pairingAttempt(boolean ok);
        void paired(String peerId, String peerName, byte[] secret);
        /** The shared secret with a paired device, or null. */
        byte[] secretFor(String peerId);
        /** Merges a peer's document into this device (and applies it); returns the merged document. */
        TreeMap<String, SyncDoc.Entry> mergeFrom(String peerId, TreeMap<String, SyncDoc.Entry> remote, long now);
    }

    // ---------------- tiny HTTP ----------------

    static final class Req {
        String method = "", path = "";
        byte[] body = new byte[0];
    }

    static String readLine(InputStream in) throws IOException {
        StringBuilder b = new StringBuilder();
        int c;
        while ((c = in.read()) >= 0) {
            if (c == '\n') break;
            if (c != '\r') b.append((char) c);
            if (b.length() > 8192) throw new IOException("line too long");
        }
        if (c < 0 && b.length() == 0) throw new IOException("eof");
        return b.toString();
    }

    static byte[] readBody(InputStream in, int len) throws IOException {
        if (len < 0 || len > MAX_BODY) throw new IOException("body size");
        byte[] b = new byte[len];
        int k = 0;
        while (k < len) {
            int n = in.read(b, k, len - k);
            if (n < 0) throw new IOException("eof");
            k += n;
        }
        return b;
    }

    static Req readRequest(InputStream in) throws IOException {
        Req r = new Req();
        String[] first = readLine(in).split(" ");
        if (first.length < 2) throw new IOException("bad request");
        r.method = first[0];
        r.path = first[1];
        int len = 0, headers = 0;
        String h;
        while (!(h = readLine(in)).isEmpty()) {
            if (++headers > 50) throw new IOException("headers");
            int c = h.indexOf(':');
            if (c > 0 && h.substring(0, c).trim().equalsIgnoreCase("Content-Length")) {
                try { len = Integer.parseInt(h.substring(c + 1).trim()); } catch (NumberFormatException e) { throw new IOException("length"); }
            }
        }
        r.body = readBody(in, len);
        return r;
    }

    static void writeResponse(OutputStream out, int code, String json) throws IOException {
        byte[] b = json.getBytes(SyncCrypto.UTF8);
        String head = "HTTP/1.0 " + code + " " + (code == 200 ? "OK" : "Error") + "\r\nContent-Type: application/json\r\nContent-Length: " + b.length
                + "\r\nConnection: close\r\n\r\n";
        out.write(head.getBytes(SyncCrypto.UTF8));
        out.write(b);
        out.flush();
    }

    static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    static String err(String msg) { return Json.write(obj("error", msg)); }

    // ---------------- server ----------------

    public static final class Server {
        final Device dev;
        final ServerSocket ss;
        volatile boolean running = true;
        final java.util.LinkedHashMap<String, Long> nonces = new java.util.LinkedHashMap<String, Long>();
        /** Simple status line for the UI (last request result). */
        public volatile String last = "";

        public Server(Device dev, int port) throws IOException {
            this.dev = dev;
            ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(port), 8);
            Thread t = new Thread(new Runnable() { public void run() { loop(); } }, "sync-server");
            t.setDaemon(true);
            t.start();
        }

        public int port() { return ss.getLocalPort(); }

        public void close() {
            running = false;
            try { ss.close(); } catch (IOException ignored) { }
        }

        void loop() {
            while (running) {
                final Socket s;
                try { s = ss.accept(); } catch (IOException e) { if (!running) return; continue; }
                Thread t = new Thread(new Runnable() { public void run() { serve(s); } }, "sync-conn");
                t.setDaemon(true);
                t.start();
            }
        }

        void serve(Socket s) {
            try {
                s.setSoTimeout(TIMEOUT_MS);
                Req r = readRequest(s.getInputStream());
                Object[] res = handle(r);
                writeResponse(s.getOutputStream(), (Integer) res[0], (String) res[1]);
            } catch (Exception e) {
                last = "Request failed";
            } finally {
                try { s.close(); } catch (IOException ignored) { }
            }
        }

        synchronized boolean freshNonce(String n, long ts, long now) {
            java.util.Iterator<Map.Entry<String, Long>> it = nonces.entrySet().iterator();
            while (it.hasNext()) if (it.next().getValue() < now - 2 * MAX_SKEW_MS) it.remove();
            if (nonces.containsKey(n) || Math.abs(now - ts) > MAX_SKEW_MS) return false;
            nonces.put(n, now);
            return true;
        }

        /** Runs one request without a socket (tests: replay and tampering). Returns {status, body}. */
        public Object[] handleRaw(String method, String path, String body) {
            Req r = new Req();
            r.method = method; r.path = path; r.body = body == null ? new byte[0] : body.getBytes(SyncCrypto.UTF8);
            return handle(r);
        }

        @SuppressWarnings("unchecked")
        Object[] handle(Req r) {
            try {
                if (r.method.equals("GET") && r.path.equals("/hello")) {
                    return new Object[]{200, Json.write(obj("app", "My Pantry Recipes", "proto", (long) PROTO, "id", dev.id(), "name", dev.name(),
                            "pairing", !dev.openPairingCode().isEmpty()))};
                }
                if (!r.method.equals("POST")) return new Object[]{404, err("not found")};
                Object o = Json.parse(new String(r.body, SyncCrypto.UTF8));
                if (!(o instanceof Map)) return new Object[]{400, err("bad request")};
                Map<String, Object> m = (Map<String, Object>) o;
                String peer = Json.str(m, "id", "");
                if (peer.isEmpty() || peer.length() > 64) return new Object[]{400, err("bad request")};
                if (r.path.equals("/pair")) {
                    String code = dev.openPairingCode();
                    if (code.isEmpty()) return new Object[]{403, err("pairing is not open")};
                    byte[] nonce = SyncCrypto.unb64(Json.str(m, "nonce", ""));
                    if (nonce.length != 16) return new Object[]{400, err("bad request")};
                    byte[] k = SyncCrypto.pairKey(code, dev.id(), peer, nonce);
                    byte[] plain;
                    try {
                        plain = SyncCrypto.open(k, "pair-req", SyncCrypto.unb64(Json.str(m, "box", "")));
                    } catch (SyncCrypto.BadMessage e) {
                        dev.pairingAttempt(false);
                        last = "Wrong pairing code";
                        return new Object[]{401, err("wrong code")};
                    }
                    Map<String, Object> pm = (Map<String, Object>) Json.parse(new String(plain, SyncCrypto.UTF8));
                    byte[] secret = SyncCrypto.unb64(Json.str(pm, "secret", ""));
                    if (secret.length != 32) return new Object[]{400, err("bad request")};
                    String pname = Json.str(m, "name", "Other device");
                    if (pname.length() > 60) pname = pname.substring(0, 60);
                    dev.paired(peer, pname, secret);
                    dev.pairingAttempt(true);
                    last = "Paired with " + pname;
                    byte[] box = SyncCrypto.seal(k, "pair-ok", Json.write(obj("id", dev.id(), "name", dev.name())).getBytes(SyncCrypto.UTF8));
                    return new Object[]{200, Json.write(obj("box", SyncCrypto.b64(box)))};
                }
                if (r.path.equals("/sync")) {
                    byte[] secret = dev.secretFor(peer);
                    if (secret == null) return new Object[]{404, err("not paired")};
                    String label = "sync-req|" + peer + "|" + dev.id();
                    byte[] plain;
                    try {
                        plain = SyncCrypto.gunzip(SyncCrypto.open(secret, label, SyncCrypto.unb64(Json.str(m, "box", ""))), MAX_PLAIN);
                    } catch (SyncCrypto.BadMessage e) {
                        last = "Rejected a message that failed its check";
                        return new Object[]{401, err("bad message")};
                    }
                    Map<String, Object> sm = (Map<String, Object>) Json.parse(new String(plain, SyncCrypto.UTF8));
                    long now = System.currentTimeMillis();
                    Object ts = sm.get("ts");
                    String rn = Json.str(sm, "nonce", "");
                    if (!(ts instanceof Number) || rn.isEmpty() || !freshNonce(rn, ((Number) ts).longValue(), now)) {
                        last = "Rejected an old or repeated message (check both clocks)";
                        return new Object[]{409, err("stale")};
                    }
                    TreeMap<String, SyncDoc.Entry> remote = SyncDoc.readDoc(Json.write(sm.get("doc")));
                    TreeMap<String, SyncDoc.Entry> merged = dev.mergeFrom(peer, remote, now);
                    Map<String, Object> resp = obj("ts", now, "doc", Json.parse(SyncDoc.writeDoc(merged)));
                    byte[] box = SyncCrypto.seal(secret, "sync-resp|" + peer + "|" + dev.id() + "|" + rn,
                            SyncCrypto.gzip(Json.write(resp).getBytes(SyncCrypto.UTF8)));
                    last = "Synced";
                    return new Object[]{200, Json.write(obj("box", SyncCrypto.b64(box)))};
                }
                return new Object[]{404, err("not found")};
            } catch (SyncCrypto.BadMessage e) {
                return new Object[]{400, err("bad request")};
            } catch (RuntimeException e) {
                return new Object[]{400, err("bad request")};
            }
        }
    }

    // ---------------- client ----------------

    /** Failure with a user-facing message. */
    public static final class SyncException extends Exception {
        public final int code;
        public SyncException(int code, String m) { super(m); this.code = code; }
    }

    /** The last request body the client sent (tests only use it to try a replay). */
    public static volatile String lastBody = "";

    static Object[] call(String host, int port, String method, String path, String body) throws SyncException {
        lastBody = body == null ? "" : body;
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), 6000);
            s.setSoTimeout(TIMEOUT_MS);
            byte[] b = body == null ? new byte[0] : body.getBytes(SyncCrypto.UTF8);
            OutputStream out = s.getOutputStream();
            out.write((method + " " + path + " HTTP/1.0\r\nHost: " + host + "\r\nContent-Type: application/json\r\nContent-Length: " + b.length + "\r\n\r\n").getBytes(SyncCrypto.UTF8));
            out.write(b);
            out.flush();
            InputStream in = s.getInputStream();
            String[] st = readLine(in).split(" ");
            int code = st.length > 1 ? Integer.parseInt(st[1]) : 0, len = -1;
            String h;
            while (!(h = readLine(in)).isEmpty()) {
                int c = h.indexOf(':');
                if (c > 0 && h.substring(0, c).trim().equalsIgnoreCase("Content-Length")) len = Integer.parseInt(h.substring(c + 1).trim());
            }
            byte[] rb;
            if (len >= 0) rb = readBody(in, len);
            else {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) { bo.write(buf, 0, n); if (bo.size() > MAX_BODY) throw new IOException("too big"); }
                rb = bo.toByteArray();
            }
            return new Object[]{code, new String(rb, SyncCrypto.UTF8)};
        } catch (java.net.SocketTimeoutException e) {
            throw new SyncException(0, "The other device did not answer in time.");
        } catch (IOException e) {
            throw new SyncException(0, "Could not reach the other device. Are both on the same Wi-Fi, with its Sync screen open?");
        } catch (NumberFormatException e) {
            throw new SyncException(0, "The other device sent an answer this app does not understand.");
        } finally {
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> jsonObj(String s) throws SyncException {
        try {
            Object o = Json.parse(s);
            if (o instanceof Map) return (Map<String, Object>) o;
        } catch (RuntimeException ignored) { }
        throw new SyncException(0, "The other device sent an answer this app does not understand.");
    }

    /** {id, name, pairing open?} of the device at host:port. */
    public static String[] hello(String host, int port) throws SyncException {
        Object[] r = call(host, port, "GET", "/hello", null);
        Map<String, Object> m = jsonObj((String) r[1]);
        if ((Integer) r[0] != 200 || !"My Pantry Recipes".equals(m.get("app"))) throw new SyncException((Integer) r[0], "That address is not My Pantry Recipes.");
        Object p = m.get("proto");
        if (!(p instanceof Number) || ((Number) p).intValue() != PROTO) throw new SyncException(0, "The other device runs a different app version. Update both.");
        return new String[]{Json.str(m, "id", ""), Json.str(m, "name", ""), String.valueOf(Boolean.TRUE.equals(m.get("pairing")))};
    }

    /** Pairs with the device at host:port using the code it shows; returns {hostId, hostName, secret(b64)}. */
    @SuppressWarnings("unchecked")
    public static String[] pair(String host, int port, String code, Device me) throws SyncException {
        if (SyncCrypto.cleanCode(code).isEmpty()) throw new SyncException(0, "Type the 10-character code shown on the other device.");
        String[] h = hello(host, port);
        if (!"true".equals(h[2])) throw new SyncException(403, "Pairing is not open on the other device. Tap \u201cPair a device\u201d there first.");
        byte[] nonce = SyncCrypto.random(16), secret = SyncCrypto.random(32);
        byte[] k = SyncCrypto.pairKey(code, h[0], me.id(), nonce);
        byte[] box = SyncCrypto.seal(k, "pair-req", Json.write(obj("secret", SyncCrypto.b64(secret))).getBytes(SyncCrypto.UTF8));
        Object[] r = call(host, port, "POST", "/pair", Json.write(obj("proto", (long) PROTO, "id", me.id(), "name", me.name(), "nonce", SyncCrypto.b64(nonce), "box", SyncCrypto.b64(box))));
        int code0 = (Integer) r[0];
        if (code0 == 401) throw new SyncException(401, "Wrong code. Check the code on the other device.");
        if (code0 == 403) throw new SyncException(403, "Pairing closed on the other device (it closes after 2 minutes or 5 wrong codes).");
        if (code0 != 200) throw new SyncException(code0, "Pairing failed.");
        try {
            Map<String, Object> ok = (Map<String, Object>) Json.parse(new String(SyncCrypto.open(k, "pair-ok", SyncCrypto.unb64(Json.str(jsonObj((String) r[1]), "box", ""))), SyncCrypto.UTF8));
            if (!h[0].equals(ok.get("id"))) throw new SyncException(0, "Pairing failed (device mismatch).");
        } catch (SyncCrypto.BadMessage e) {
            throw new SyncException(0, "Pairing failed: the answer did not check out.");
        }
        me.paired(h[0], h[1], secret);
        return new String[]{h[0], h[1], SyncCrypto.b64(secret)};
    }

    /** Sends our document to a paired device and returns {host time, merged document}. */
    @SuppressWarnings("unchecked")
    public static Object[] sync(String host, int port, String peerId, byte[] secret, String myId, Map<String, SyncDoc.Entry> doc) throws SyncException {
        String nonce = SyncCrypto.b64(SyncCrypto.random(16));
        Map<String, Object> req = obj("ts", System.currentTimeMillis(), "nonce", nonce, "doc", Json.parse(SyncDoc.writeDoc(doc)));
        byte[] box = SyncCrypto.seal(secret, "sync-req|" + myId + "|" + peerId, SyncCrypto.gzip(Json.write(req).getBytes(SyncCrypto.UTF8)));
        Object[] r = call(host, port, "POST", "/sync", Json.write(obj("proto", (long) PROTO, "id", myId, "box", SyncCrypto.b64(box))));
        int c = (Integer) r[0];
        if (c == 404) throw new SyncException(404, "The other device does not know this one any more. Pair again.");
        if (c == 401) throw new SyncException(401, "The other device rejected the message (pairing out of date?). Pair again.");
        if (c == 409) throw new SyncException(409, "The other device rejected the message as out of date. Check that both clocks are right.");
        if (c != 200) throw new SyncException(c, "Sync failed.");
        try {
            byte[] plain = SyncCrypto.gunzip(SyncCrypto.open(secret, "sync-resp|" + myId + "|" + peerId + "|" + nonce,
                    SyncCrypto.unb64(Json.str(jsonObj((String) r[1]), "box", ""))), MAX_PLAIN);
            Map<String, Object> m = (Map<String, Object>) Json.parse(new String(plain, SyncCrypto.UTF8));
            Object ts = m.get("ts");
            return new Object[]{ts instanceof Number ? ((Number) ts).longValue() : System.currentTimeMillis(), SyncDoc.readDoc(Json.write(m.get("doc")))};
        } catch (SyncCrypto.BadMessage e) {
            throw new SyncException(0, "The answer from the other device did not check out.");
        } catch (RuntimeException e) {
            throw new SyncException(0, "The other device sent an answer this app does not understand.");
        }
    }

    // ---------------- a reusable device engine (used by the app and by the simulated-device tests) ----------------

    /** One paired device as remembered locally. */
    public static final class Peer {
        public String id = "", name = "", host = "", secret = "";
        public int port;
        public long lastSync;
        public String lastResult = "";
        public Map<String, Object> toMap() {
            return obj("id", id, "name", name, "host", host, "port", (long) port, "secret", secret, "lastSync", lastSync, "lastResult", lastResult);
        }
        @SuppressWarnings("unchecked")
        public static Peer fromMap(Object o) {
            if (!(o instanceof Map)) return null;
            Map<String, Object> m = (Map<String, Object>) o;
            Peer p = new Peer();
            p.id = Json.str(m, "id", ""); p.name = Json.str(m, "name", ""); p.host = Json.str(m, "host", ""); p.secret = Json.str(m, "secret", "");
            Object po = m.get("port"), ls = m.get("lastSync");
            p.port = po instanceof Number ? ((Number) po).intValue() : 0;
            p.lastSync = ls instanceof Number ? ((Number) ls).longValue() : 0;
            p.lastResult = Json.str(m, "lastResult", "");
            return p.id.isEmpty() || p.secret.isEmpty() ? null : p;
        }
    }

    public static String writePeers(List<Peer> ps) {
        List<Object> l = new ArrayList<Object>();
        for (Peer p : ps) l.add(p.toMap());
        return Json.write(l);
    }

    public static List<Peer> readPeers(String json) {
        List<Peer> out = new ArrayList<Peer>();
        try {
            Object o = Json.parse(json);
            if (o instanceof List) for (Object x : (List<?>) o) { Peer p = Peer.fromMap(x); if (p != null) out.add(p); }
        } catch (RuntimeException ignored) { }
        return out;
    }
}
