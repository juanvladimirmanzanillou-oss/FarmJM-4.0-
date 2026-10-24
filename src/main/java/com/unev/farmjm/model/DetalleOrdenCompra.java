package com.unev.farmjm.model;

/** Clase del modelo: DetalleOrdenCompra. */
public class DetalleOrdenCompra {

    // Atributos privados (encapsulamiento)
    private int idDetalle;
    private Producto producto;
    private int cantidad;
    private double costoUnitario;

    // Constructor vacío
    public DetalleOrdenCompra() { }

    // Constructor con parámetros
    public DetalleOrdenCompra(int idDetalle, Producto producto, int cantidad, double costoUnitario) {
        this.idDetalle = idDetalle;
        this.producto = producto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    // Getters y setters
    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }

    @Override
    public String toString() {
        return "Detalle orden #" + idDetalle;
    }
}
