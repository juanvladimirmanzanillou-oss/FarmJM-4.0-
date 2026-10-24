package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase del modelo: Inventario. */
public class Inventario {

    // Atributos privados (encapsulamiento)
    private int idInventario;
    private Producto producto;
    private int existencia;
    private int stockMinimo;
    private LocalDateTime ultimaActualizacion;

    // Constructor vacío
    public Inventario() { }

    // Constructor con parámetros
    public Inventario(int idInventario, Producto producto, int existencia, int stockMinimo, LocalDateTime ultimaActualizacion) {
        this.idInventario = idInventario;
        this.producto = producto;
        this.existencia = existencia;
        this.stockMinimo = stockMinimo;
        this.ultimaActualizacion = ultimaActualizacion;
    }

    // Getters y setters
    public int getIdInventario() { return idInventario; }
    public void setIdInventario(int idInventario) { this.idInventario = idInventario; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getExistencia() { return existencia; }
    public void setExistencia(int existencia) { this.existencia = existencia; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    // Métodos auxiliares
    public boolean bajoMinimo() { return existencia <= stockMinimo; }

    @Override
    public String toString() {
        return "Inventario #" + idInventario;
    }
}
