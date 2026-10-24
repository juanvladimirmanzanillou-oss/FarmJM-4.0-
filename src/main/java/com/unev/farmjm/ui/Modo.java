package com.unev.farmjm.ui;

/** Tamaños de referencia: escritorio (1366×768), tableta (1024×768 / 768×1024) y teléfono (412×915). */
public enum Modo {
    ESCRITORIO("modo-escritorio"), TABLETA("modo-tableta"), TELEFONO("modo-telefono");

    public final String clase;

    Modo(String clase) { this.clase = clase; }

    /** Modo según el ancho de la ventana. */
    public static Modo de(double ancho) {
        if (ancho >= 1100) return ESCRITORIO;
        if (ancho >= 700) return TABLETA;
        return TELEFONO;
    }
}
