package com.unev.farmjm.model;

import java.util.ArrayList;
import java.util.List;

/** Clase del modelo: Producto. */
public class Producto {

    // Atributos privados (encapsulamiento)
    private int idProducto;
    private String codigo;
    private String nombre;
    private double precio;
    private int stock;
    private Categoria categoria;
    private Proveedor proveedor;
    private List<Lote> lotes = new ArrayList<>();

    // Constructor vacío
    public Producto() { }

    // Constructor con parámetros
    public Producto(int idProducto, String codigo, String nombre, double precio, int stock, Categoria categoria, Proveedor proveedor) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.categoria = categoria;
        this.proveedor = proveedor;
    }

    // Getters y setters
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    public List<Lote> getLotes() { return lotes; }
    public void setLotes(List<Lote> lotes) { this.lotes = lotes; }

    // Métodos auxiliares
    public void agregarLote(Lote l) { lotes.add(l); }

    /** Devuelve el lote con la fecha de vencimiento más cercana (regla FEFO). */
    public Lote getLoteProximo() {
        Lote r = null;
        for (Lote l : lotes) {
            if (r == null || l.getFechaVencimiento().isBefore(r.getFechaVencimiento())) r = l;
        }
        return r;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
