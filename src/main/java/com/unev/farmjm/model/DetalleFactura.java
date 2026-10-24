package com.unev.farmjm.model;

/** Clase del modelo: DetalleFactura. */
public class DetalleFactura {

    // Atributos privados (encapsulamiento)
    private int idDetalle;
    private Producto producto;
    private Lote lote;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;

    // Constructor vacío
    public DetalleFactura() { }

    // Constructor con parámetros
    public DetalleFactura(int idDetalle, Producto producto, Lote lote, int cantidad, double precioUnitario, double subtotal) {
        this.idDetalle = idDetalle;
        this.producto = producto;
        this.lote = lote;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    // Getters y setters
    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    // Métodos auxiliares
    public void cambiarCantidad(int nueva) {
        this.cantidad = nueva;
        this.subtotal = nueva * precioUnitario;
    }

    @Override
    public String toString() {
        return "Detalle #" + idDetalle;
    }
}
