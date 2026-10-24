package com.unev.farmjm.model;

/** Clase del modelo: Descuento. */
public class Descuento {

    // Atributos privados (encapsulamiento)
    private int idDescuento;
    private String nombre;
    private double porcentaje;
    private boolean activo;

    // Constructor vacío
    public Descuento() { }

    // Constructor con parámetros
    public Descuento(int idDescuento, String nombre, double porcentaje, boolean activo) {
        this.idDescuento = idDescuento;
        this.nombre = nombre;
        this.porcentaje = porcentaje;
        this.activo = activo;
    }

    // Getters y setters
    public int getIdDescuento() { return idDescuento; }
    public void setIdDescuento(int idDescuento) { this.idDescuento = idDescuento; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return porcentaje > 0 ? nombre + " (" + (int) porcentaje + "%)" : nombre;
    }
}
