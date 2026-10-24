package com.unev.farmjm.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * TOTP (RFC 6238) compatible con Google Authenticator y Microsoft Authenticator:
 * HMAC-SHA1, 6 dígitos, paso de 30 segundos, clave secreta en Base32.
 */
public final class Totp {

    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PASO_SEGUNDOS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() { }

    /** Genera una clave secreta aleatoria de 160 bits (32 caracteres Base32). */
    public static String nuevoSecreto() {
        byte[] b = new byte[20];
        RANDOM.nextBytes(b);
        return base32(b);
    }

    /** Código de 6 dígitos para el instante dado (en milisegundos). */
    public static String codigo(String secretoBase32, long milis) {
        return codigoPaso(secretoBase32, milis / 1000 / PASO_SEGUNDOS);
    }

    static String codigoPaso(String secretoBase32, long paso) {
        try {
            byte[] clave = decodificarBase32(secretoBase32);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(clave, "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(paso).array());
            int off = h[h.length - 1] & 0x0F;
            int bin = ((h[off] & 0x7F) << 24) | ((h[off + 1] & 0xFF) << 16)
                    | ((h[off + 2] & 0xFF) << 8) | (h[off + 3] & 0xFF);
            return String.format("%06d", bin % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA1 no disponible", e);
        }
    }

    /** Verifica el código aceptando una ventana de ±1 paso (30 s) por desfase de reloj. */
    public static boolean verificar(String secretoBase32, String codigo) {
        if (codigo == null || !codigo.matches("\\d{6}")) return false;
        long paso = System.currentTimeMillis() / 1000 / PASO_SEGUNDOS;
        boolean ok = false;
        for (int d = -1; d <= 1; d++) {
            byte[] esperado = codigoPaso(secretoBase32, paso + d).getBytes(StandardCharsets.UTF_8);
            ok |= MessageDigest.isEqual(esperado, codigo.getBytes(StandardCharsets.UTF_8));
        }
        return ok;
    }

    /** URI que se codifica en el QR (formato otpauth). */
    public static String uri(String emisor, String usuario, String secreto) {
        return "otpauth://totp/" + enc(emisor) + ":" + enc(usuario)
                + "?secret=" + secreto + "&issuer=" + enc(emisor) + "&algorithm=SHA1&digits=6&period=30";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String base32(byte[] datos) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALFABETO.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) sb.append(ALFABETO.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }

    static byte[] decodificarBase32(String s) {
        String limpio = s.replace(" ", "").replace("=", "").toUpperCase();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char c : limpio.toCharArray()) {
            int v = ALFABETO.indexOf(c);
            if (v < 0) throw new IllegalArgumentException("Base32 inválido");
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
