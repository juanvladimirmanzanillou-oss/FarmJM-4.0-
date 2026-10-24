package com.unev.farmjm.model;

/** Clase del modelo: DetalleReceta. */
public class DetalleReceta {

    // Atributos privados (encapsulamiento)
    private int idDetalle;
    private Medicamento medicamento;
    private int cantidad;
    private String indicaciones;

    // Constructor vacío
    public DetalleReceta() { }

    // Constructor con parámetros
    public DetalleReceta(int idDetalle, Medicamento medicamento, int cantidad, String indicaciones) {
        this.idDetalle = idDetalle;
        this.medicamento = medicamento;
        this.cantidad = cantidad;
        this.indicaciones = indicaciones;
    }

    // Getters y setters
    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public Medicamento getMedicamento() { return medicamento; }
    public void setMedicamento(Medicamento medicamento) { this.medicamento = medicamento; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String indicaciones) { this.indicaciones = indicaciones; }

    @Override
    public String toString() {
        return "Detalle receta #" + idDetalle;
    }
}
