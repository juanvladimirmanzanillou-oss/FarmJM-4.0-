package com.unev.farmjm.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lee la configuración externa (config.properties). Las credenciales NO se escriben en el código Java.
 * Orden de búsqueda: -Dfarmjm.config=ruta, ./config.properties, ~/.farmjm/config.properties.
 * Cualquier valor puede sobrescribirse con una variable de entorno FARMJM_DB_PASSWORD, FARMJM_DB_URL, etc.
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();
    private static String origen = null;

    static {
        String[] candidatos = {
                System.getProperty("farmjm.config"),
                "config.properties",
                System.getProperty("user.home") + "/.farmjm/config.properties"
        };
        for (String c : candidatos) {
            if (c == null) continue;
            Path p = Path.of(c);
            if (Files.isReadable(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    PROPS.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                    origen = p.toAbsolutePath().toString();
                    break;
                } catch (IOException e) {
                    System.err.println("No se pudo leer " + c + ": " + e.getMessage());
                }
            }
        }
    }

    private AppConfig() { }

    public static String get(String clave, String porDefecto) {
        String env = System.getenv("FARMJM_" + clave.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env;
        return PROPS.getProperty(clave, porDefecto);
    }

    public static int getInt(String clave, int porDefecto) {
        try {
            return Integer.parseInt(get(clave, String.valueOf(porDefecto)).trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    public static boolean cargada() { return origen != null; }

    public static String origen() { return origen; }
}
