package com.unev.farmjm.util;

import com.unev.farmjm.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Registro de errores en un archivo de log (logs/farmjm.log). También recibe los mensajes de HikariCP. */
public final class Log {

    private static final Logger LOGGER = Logger.getLogger("farmjm");

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%4$s] %5$s%6$s%n");
        try {
            Path dir = Path.of(AppConfig.get("log.dir", "logs"));
            Files.createDirectories(dir);
            FileHandler fh = new FileHandler(dir.resolve("farmjm.log").toString(), 1_000_000, 3, true);
            fh.setEncoding("UTF-8");
            fh.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(fh);
        } catch (IOException | SecurityException e) {
            System.err.println("No se pudo crear el archivo de log: " + e.getMessage());
        }
    }

    private Log() { }

    public static void info(String mensaje) { LOGGER.log(Level.INFO, mensaje); }

    public static void error(String mensaje, Throwable t) { LOGGER.log(Level.SEVERE, mensaje, t); }
}
