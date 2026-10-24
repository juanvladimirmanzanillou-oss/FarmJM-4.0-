package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase del modelo: Pago. */
public class Pago {

    // Atributos privados (encapsulamiento)
    private int idPago;
    private Factura factura;
    private String metodo;
    private double monto;
    private LocalDateTime fecha;

    // Constructor vacío
    public Pago() { }

    // Constructor con parámetros
    public Pago(int idPago, Factura factura, String metodo, double monto, LocalDateTime fecha) {
        this.idPago = idPago;
        this.factura = factura;
        this.metodo = metodo;
        this.monto = monto;
        this.fecha = fecha;
    }

    // Getters y setters
    public int getIdPago() { return idPago; }
    public void setIdPago(int idPago) { this.idPago = idPago; }
    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    @Override
    public String toString() {
        return metodo + " RD$ " + monto;
    }
}
