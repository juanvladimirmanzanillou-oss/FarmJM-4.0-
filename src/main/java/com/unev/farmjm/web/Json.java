package com.unev.farmjm.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON mínimo (sin dependencias): escribe Map, List, String, Number, Boolean y null, y lee JSON a esos mismos tipos. */
public final class Json {

    private Json() { }

    // ------------------------------------------------------------ escribir
    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        escribir(sb, o);
        return sb.toString();
    }

    private static void escribir(StringBuilder sb, Object o) {
        if (o == null) sb.append("null");
        else if (o instanceof Boolean || o instanceof Integer || o instanceof Long) sb.append(o);
        else if (o instanceof Number n) {
            double d = n.doubleValue();
            sb.append(Double.isNaN(d) || Double.isInfinite(d) ? "0" : String.valueOf(d));
        } else if (o instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                cadena(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escribir(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Iterable<?> it) {
            sb.append('[');
            boolean primero = true;
            for (Object x : it) {
                if (!primero) sb.append(',');
                primero = false;
                escribir(sb, x);
            }
            sb.append(']');
        } else cadena(sb, o.toString());
    }

    private static void cadena(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------ leer
    public static Object parse(String s) {
        Lector l = new Lector(s);
        l.blancos();
        Object v = l.valor();
        l.blancos();
        if (l.i != s.length()) throw new IllegalArgumentException("JSON inválido");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> objeto(String s) {
        if (s == null || s.isBlank()) return new LinkedHashMap<>();
        Object o = parse(s);
        if (!(o instanceof Map)) throw new IllegalArgumentException("Se esperaba un objeto JSON");
        return (Map<String, Object>) o;
    }

    public static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString().trim();
    }

    public static double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        try {
            return v == null || v.toString().isBlank() ? 0 : Double.parseDouble(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor de \"" + k + "\" no es un número");
        }
    }

    public static int entero(Map<String, Object> m, String k) { return (int) Math.round(num(m, k)); }

    public static boolean bool(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Boolean b ? b : v != null && "true".equalsIgnoreCase(v.toString());
    }

    private static final class Lector {
        final String s;
        int i = 0;

        Lector(String s) { this.s = s; }

        void blancos() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        Object valor() {
            if (i >= s.length()) throw new IllegalArgumentException("JSON incompleto");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return lista();
            if (c == '"') return cadena();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int ini = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (ini == i) throw new IllegalArgumentException("JSON inválido");
            return Double.parseDouble(s.substring(ini, i));
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            blancos();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                blancos();
                String k = cadena();
                blancos();
                if (s.charAt(i++) != ':') throw new IllegalArgumentException("JSON inválido");
                blancos();
                m.put(k, valor());
                blancos();
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw new IllegalArgumentException("JSON inválido");
            }
        }

        List<Object> lista() {
            List<Object> l = new ArrayList<>();
            i++;
            blancos();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                blancos();
                l.add(valor());
                blancos();
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw new IllegalArgumentException("JSON inválido");
            }
        }

        String cadena() {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("JSON inválido");
            StringBuilder sb = new StringBuilder();
            i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> sb.append(e);
                    }
                } else sb.append(c);
            }
        }
    }
}
