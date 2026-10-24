package com.unev.farmjm.model;

/** Clase del modelo: Proveedor. */
public class Proveedor {

    // Atributos privados (encapsulamiento)
    private int idProveedor;
    private String nombre;
    private String rnc;
    private String telefono;
    private String direccion;

    // Constructor vacío
    public Proveedor() { }

    // Constructor con parámetros
    public Proveedor(int idProveedor, String nombre, String rnc, String telefono, String direccion) {
        this.idProveedor = idProveedor;
        this.nombre = nombre;
        this.rnc = rnc;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    // Getters y setters
    public int getIdProveedor() { return idProveedor; }
    public void setIdProveedor(int idProveedor) { this.idProveedor = idProveedor; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRnc() { return rnc; }
    public void setRnc(String rnc) { this.rnc = rnc; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    @Override
    public String toString() {
        return nombre;
    }
}
