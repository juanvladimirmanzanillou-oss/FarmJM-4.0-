package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase del modelo: AjusteInventario. */
public class AjusteInventario {

    // Atributos privados (encapsulamiento)
    private int idAjuste;
    private Producto producto;
    private int cantidad;
    private String motivo;
    private LocalDateTime fecha;
    private Usuario usuario;

    // Constructor vacío
    public AjusteInventario() { }

    // Constructor con parámetros
    public AjusteInventario(int idAjuste, Producto producto, int cantidad, String motivo, LocalDateTime fecha, Usuario usuario) {
        this.idAjuste = idAjuste;
        this.producto = producto;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.fecha = fecha;
        this.usuario = usuario;
    }

    // Getters y setters
    public int getIdAjuste() { return idAjuste; }
    public void setIdAjuste(int idAjuste) { this.idAjuste = idAjuste; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    @Override
    public String toString() {
        return "Ajuste #" + idAjuste + " (" + motivo + ")";
    }
}
