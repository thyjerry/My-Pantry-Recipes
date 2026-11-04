package org.pantryrecipes.app.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * v2.0 sync crypto, built only from standard primitives available on Android 5 (HMAC-SHA256, AES-CTR):
 * PBKDF2-HMAC-SHA256 (written out here because Android 5 only ships the SHA-1 variant) stretches the one-time pairing
 * code; after pairing, a random 256-bit secret per device pair protects every message with encrypt-then-MAC
 * (AES-256-CTR, HMAC-SHA256 over everything, per-message keys from a fresh 16-byte nonce). Not independently audited.
 */
public final class SyncCrypto {
    private SyncCrypto() { }

    static final Charset UTF8 = Charset.forName("UTF-8");
    public static final int PAIR_ITERATIONS = 20000;
    /** Pairing codes use Crockford-style base32 without I, L, O, U (no look-alikes). */
    static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    public static final int CODE_CHARS = 10;   // 50 bits

    private static final SecureRandom RNG = new SecureRandom();

    public static byte[] random(int n) { byte[] b = new byte[n]; RNG.nextBytes(b); return b; }

    /** "ABCDE-FGHJK" style pairing code. */
    public static String newCode() {
        byte[] r = random(CODE_CHARS);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < CODE_CHARS; i++) {
            if (i == CODE_CHARS / 2) b.append('-');
            b.append(ALPHABET.charAt(r[i] & 31));
        }
        return b.toString();
    }

    /** Upper-case, look-alikes mapped (O->0, I/L->1), separators removed; "" if it is not a valid code. */
    public static String cleanCode(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toUpperCase(java.util.Locale.ROOT).toCharArray()) {
            if (c == '-' || c == ' ') continue;
            if (c == 'O') c = '0';
            if (c == 'I' || c == 'L') c = '1';
            if (ALPHABET.indexOf(c) < 0) return "";
            b.append(c);
        }
        return b.length() == CODE_CHARS ? b.toString() : "";
    }

    public static byte[] hmac(byte[] key, byte[]... parts) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(key, "HmacSHA256"));
            for (byte[] p : parts) m.update(p);
            return m.doFinal();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** PBKDF2-HMAC-SHA256 (RFC 8018), one 32-byte block. */
    public static byte[] pbkdf2(byte[] password, byte[] salt, int iterations) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(password, "HmacSHA256"));
            m.update(salt);
            m.update(new byte[]{0, 0, 0, 1});
            byte[] u = m.doFinal(), t = u.clone();
            for (int i = 1; i < iterations; i++) {
                u = m.doFinal(u);
                for (int k = 0; k < t.length; k++) t[k] ^= u[k];
            }
            return t;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The pairing key from the code both screens show, bound to both device ids and the joiner's nonce. */
    public static byte[] pairKey(String code, String hostId, String joinerId, byte[] nonce) {
        String c = cleanCode(code);
        return pbkdf2(c.getBytes(UTF8), cat(("my-pantry-pair-v1|" + hostId + "|" + joinerId + "|").getBytes(UTF8), nonce), PAIR_ITERATIONS);
    }

    static byte[] cat(byte[]... parts) {
        int n = 0;
        for (byte[] p : parts) n += p.length;
        byte[] o = new byte[n];
        int k = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, o, k, p.length); k += p.length; }
        return o;
    }

    /** Thrown when a message was changed, is for another purpose, or the key is wrong. */
    public static final class BadMessage extends Exception {
        public BadMessage(String m) { super(m); }
    }

    /** nonce(16) | AES-256-CTR ciphertext | HMAC-SHA256 tag(32). The label binds the message to its purpose. */
    public static byte[] seal(byte[] key, String label, byte[] plain) {
        byte[] nonce = random(16);
        byte[] lb = label.getBytes(UTF8);
        byte[] ek = hmac(key, "enc".getBytes(UTF8), lb, nonce), mk = hmac(key, "mac".getBytes(UTF8), lb, nonce);
        byte[] iv = new byte[16];
        System.arraycopy(hmac(key, "iv".getBytes(UTF8), lb, nonce), 0, iv, 0, 16);
        byte[] ct;
        try {
            Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ek, "AES"), new IvParameterSpec(iv));
            ct = c.doFinal(plain);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        byte[] tag = hmac(mk, lb, nonce, ct);
        return cat(nonce, ct, tag);
    }

    public static byte[] open(byte[] key, String label, byte[] box) throws BadMessage {
        if (box == null || box.length < 48) throw new BadMessage("too short");
        byte[] nonce = new byte[16], ct = new byte[box.length - 48], tag = new byte[32];
        System.arraycopy(box, 0, nonce, 0, 16);
        System.arraycopy(box, 16, ct, 0, ct.length);
        System.arraycopy(box, box.length - 32, tag, 0, 32);
        byte[] lb = label.getBytes(UTF8);
        byte[] mk = hmac(key, "mac".getBytes(UTF8), lb, nonce);
        if (!java.security.MessageDigest.isEqual(tag, hmac(mk, lb, nonce, ct))) throw new BadMessage("bad tag");
        byte[] ek = hmac(key, "enc".getBytes(UTF8), lb, nonce);
        byte[] iv = new byte[16];
        System.arraycopy(hmac(key, "iv".getBytes(UTF8), lb, nonce), 0, iv, 0, 16);
        try {
            Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(ek, "AES"), new IvParameterSpec(iv));
            return c.doFinal(ct);
        } catch (Exception e) {
            throw new BadMessage("decrypt");
        }
    }

    public static byte[] gzip(byte[] b) {
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            java.util.zip.GZIPOutputStream z = new java.util.zip.GZIPOutputStream(o);
            z.write(b);
            z.close();
            return o.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Gunzips at most {@code max} bytes (a zip bomb cannot blow up memory). */
    public static byte[] gunzip(byte[] b, int max) throws BadMessage {
        try {
            java.util.zip.GZIPInputStream z = new java.util.zip.GZIPInputStream(new ByteArrayInputStream(b));
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = z.read(buf)) > 0) {
                o.write(buf, 0, n);
                if (o.size() > max) throw new BadMessage("too large");
            }
            return o.toByteArray();
        } catch (java.io.IOException e) {
            throw new BadMessage("bad data");
        }
    }

    // ---------------- base64 (java.util.Base64 is API 26+) ----------------
    private static final char[] B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public static String b64(byte[] d) {
        StringBuilder b = new StringBuilder((d.length + 2) / 3 * 4);
        for (int i = 0; i < d.length; i += 3) {
            int n = (d[i] & 255) << 16 | (i + 1 < d.length ? (d[i + 1] & 255) << 8 : 0) | (i + 2 < d.length ? d[i + 2] & 255 : 0);
            b.append(B64[n >> 18 & 63]).append(B64[n >> 12 & 63]).append(i + 1 < d.length ? B64[n >> 6 & 63] : '=').append(i + 2 < d.length ? B64[n & 63] : '=');
        }
        return b.toString();
    }

    public static byte[] unb64(String s) throws BadMessage {
        if (s == null || s.length() % 4 != 0) throw new BadMessage("base64");
        int pad = s.endsWith("==") ? 2 : s.endsWith("=") ? 1 : 0;
        byte[] out = new byte[s.length() / 4 * 3 - pad];
        int k = 0;
        for (int i = 0; i < s.length(); i += 4) {
            int n = 0;
            for (int j = 0; j < 4; j++) {
                char c = s.charAt(i + j);
                int v = c >= 'A' && c <= 'Z' ? c - 'A' : c >= 'a' && c <= 'z' ? c - 'a' + 26 : c >= '0' && c <= '9' ? c - '0' + 52 : c == '+' ? 62 : c == '/' ? 63 : c == '=' && i + j >= s.length() - pad ? 0 : -1;
                if (v < 0) throw new BadMessage("base64");
                n = n << 6 | v;
            }
            if (k < out.length) out[k++] = (byte) (n >> 16);
            if (k < out.length) out[k++] = (byte) (n >> 8);
            if (k < out.length) out[k++] = (byte) n;
        }
        return out;
    }

    public static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x & 255));
        return s.toString();
    }
}
