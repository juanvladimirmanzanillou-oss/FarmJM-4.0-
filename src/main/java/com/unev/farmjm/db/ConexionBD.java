package com.unev.farmjm.db;

import com.unev.farmjm.config.AppConfig;
import com.unev.farmjm.util.Log;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Conexión JDBC a MySQL mediante un pool (HikariCP).
 * Los datos (url, usuario, clave) vienen de config.properties; no hay credenciales en el código.
 */
public final class ConexionBD {

    private static HikariDataSource pool;

    private ConexionBD() { }

    private static synchronized HikariDataSource pool() throws SQLException {
        if (pool != null && !pool.isClosed()) return pool;
        String url = AppConfig.get("db.url", null);
        if (url == null || url.isBlank()) {
            throw new SQLException("No se encontró config.properties. Copie config.example.properties como "
                    + "config.properties y complete los datos de la base de datos.");
        }
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url.trim());
        cfg.setUsername(AppConfig.get("db.user", ""));
        cfg.setPassword(AppConfig.get("db.password", ""));
        cfg.setMaximumPoolSize(AppConfig.getInt("db.pool.size", 5));
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(AppConfig.getInt("db.timeout.ms", 10000));
        cfg.setPoolName("FarmJM-Pool");
        try {
            pool = new HikariDataSource(cfg);
        } catch (RuntimeException e) {
            Log.error("No se pudo crear el pool de conexiones", e);
            throw new SQLException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
        return pool;
    }

    /** Devuelve una conexión del pool; al cerrarla (try-with-resources) regresa al pool. */
    public static Connection getConexion() throws SQLException {
        return pool().getConnection();
    }

    public static synchronized void cerrar() {
        if (pool != null) pool.close();
    }

    /** Prueba de conexión: ejecute esta clase para evidenciar que conecta (local o en la nube). */
    public static void main(String[] args) {
        try (Connection c = getConexion()) {
            System.out.println("Conexión exitosa a la base de datos: " + c.getCatalog());
            System.out.println("Servidor: " + c.getMetaData().getDatabaseProductName()
                    + " " + c.getMetaData().getDatabaseProductVersion());
            System.out.println("Driver JDBC: " + c.getMetaData().getDriverName());
            System.out.println("Configuración leída de: " + AppConfig.origen());
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        } finally {
            cerrar();
        }
    }
}
