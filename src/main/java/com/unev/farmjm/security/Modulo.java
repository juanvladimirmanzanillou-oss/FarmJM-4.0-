package com.unev.farmjm.security;

/** Módulos del sistema sobre los que se aplican permisos. */
public enum Modulo {
    DASHBOARD("Dashboard"), POS("Punto de Venta (POS)"), INVENTARIO("Inventario & Lotes"), CLIENTES("Clientes"),
    CAJA("Arqueo & Cierre Caja"), RECETAS("Recetas / Controlados"), DEVOLUCIONES("Devoluciones (NC)"),
    REPORTES("Reportes"), USUARIOS("Usuarios y Roles"), AUDITORIA("Auditoría & Logs");

    private final String titulo;

    Modulo(String titulo) { this.titulo = titulo; }

    public String titulo() { return titulo; }
}
