package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Clase del modelo: Lote. */
public class Lote {

    // Atributos privados (encapsulamiento)
    private int idLote;
    private String codigoLote;
    private Producto producto;
    private LocalDate fechaVencimiento;
    private int cantidad;

    // Constructor vacío
    public Lote() { }

    // Constructor con parámetros
    public Lote(int idLote, String codigoLote, Producto producto, LocalDate fechaVencimiento, int cantidad) {
        this.idLote = idLote;
        this.codigoLote = codigoLote;
        this.producto = producto;
        this.fechaVencimiento = fechaVencimiento;
        this.cantidad = cantidad;
    }

    // Getters y setters
    public int getIdLote() { return idLote; }
    public void setIdLote(int idLote) { this.idLote = idLote; }
    public String getCodigoLote() { return codigoLote; }
    public void setCodigoLote(String codigoLote) { this.codigoLote = codigoLote; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    // Métodos auxiliares
    public long getDiasParaVencer() { return ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento); }
    public boolean estaVencido() { return getDiasParaVencer() < 0; }

    @Override
    public String toString() {
        return codigoLote;
    }
}
