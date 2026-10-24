package com.unev.farmjm.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Clase del modelo: OrdenCompra. */
public class OrdenCompra {

    // Atributos privados (encapsulamiento)
    private int idOrden;
    private Proveedor proveedor;
    private LocalDate fecha;
    private String estado;
    private double total;
    private List<DetalleOrdenCompra> detalles = new ArrayList<>();

    // Constructor vacío
    public OrdenCompra() { }

    // Constructor con parámetros
    public OrdenCompra(int idOrden, Proveedor proveedor, LocalDate fecha, String estado, double total) {
        this.idOrden = idOrden;
        this.proveedor = proveedor;
        this.fecha = fecha;
        this.estado = estado;
        this.total = total;
    }

    // Getters y setters
    public int getIdOrden() { return idOrden; }
    public void setIdOrden(int idOrden) { this.idOrden = idOrden; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public List<DetalleOrdenCompra> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleOrdenCompra> detalles) { this.detalles = detalles; }

    // Métodos auxiliares
    public void agregarDetalle(DetalleOrdenCompra d) { detalles.add(d); }

    @Override
    public String toString() {
        return "Orden #" + idOrden;
    }
}
