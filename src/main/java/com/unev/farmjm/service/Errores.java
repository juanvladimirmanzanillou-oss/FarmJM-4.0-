package com.unev.farmjm.service;

import com.unev.farmjm.util.Log;

import java.sql.SQLException;

/** Convierte errores de base de datos en mensajes amigables y los registra en el archivo de log. */
final class Errores {

    private Errores() { }

    static ServicioException bd(String contexto, SQLException e) {
        Log.error(contexto, e);
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("Stock insuficiente")) return new ServicioException(msg, e);
        if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
            return new ServicioException("Ese dato ya existe o está en uso por otro registro.", e);
        }
        boolean conexion = (e.getSQLState() != null && e.getSQLState().startsWith("08"))
                || msg.contains("No se pudo conectar") || msg.contains("config.properties")
                || msg.toLowerCase().contains("communications link") || msg.toLowerCase().contains("timeout");
        if (conexion) {
            return new ServicioException("No se pudo conectar con la base de datos. Verifique su conexión a Internet "
                    + "y los datos de config.properties. (Detalle en logs/farmjm.log)", e);
        }
        return new ServicioException("No se pudo completar la operación (" + contexto + "). Detalle en logs/farmjm.log.", e);
    }
}
