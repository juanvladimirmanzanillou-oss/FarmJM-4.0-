package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase del modelo: Devolucion. */
public class Devolucion {

    // Atributos privados (encapsulamiento)
    private int idDevolucion;
    private Factura factura;
    private Producto producto;
    private int cantidad;
    private String motivo;
    private double monto;
    private String ncfNotaCredito;
    private LocalDateTime fecha;

    // Constructor vacío
    public Devolucion() { }

    // Constructor con parámetros
    public Devolucion(int idDevolucion, Factura factura, Producto producto, int cantidad, String motivo, double monto, String ncfNotaCredito, LocalDateTime fecha) {
        this.idDevolucion = idDevolucion;
        this.factura = factura;
        this.producto = producto;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.monto = monto;
        this.ncfNotaCredito = ncfNotaCredito;
        this.fecha = fecha;
    }

    // Getters y setters
    public int getIdDevolucion() { return idDevolucion; }
    public void setIdDevolucion(int idDevolucion) { this.idDevolucion = idDevolucion; }
    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public String getNcfNotaCredito() { return ncfNotaCredito; }
    public void setNcfNotaCredito(String ncfNotaCredito) { this.ncfNotaCredito = ncfNotaCredito; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    @Override
    public String toString() {
        return "Devolución #" + idDevolucion;
    }
}
