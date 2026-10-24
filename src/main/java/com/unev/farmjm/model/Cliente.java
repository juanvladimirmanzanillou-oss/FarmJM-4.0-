package com.unev.farmjm.model;

/** Clase del modelo: Cliente. */
public class Cliente {

    // Atributos privados (encapsulamiento)
    private int idCliente;
    private String nombre;
    private String cedulaRnc;
    private String telefono;
    private String email;

    // Constructor vacío
    public Cliente() { }

    // Constructor con parámetros
    public Cliente(int idCliente, String nombre, String cedulaRnc, String telefono, String email) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.cedulaRnc = cedulaRnc;
        this.telefono = telefono;
        this.email = email;
    }

    // Getters y setters
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCedulaRnc() { return cedulaRnc; }
    public void setCedulaRnc(String cedulaRnc) { this.cedulaRnc = cedulaRnc; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return nombre;
    }
}
