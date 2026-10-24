package com.unev.farmjm.model;

/** Clase del modelo: ComprobanteFiscal. */
public class ComprobanteFiscal {

    // Atributos privados (encapsulamiento)
    private int idComprobante;
    private String tipo;
    private String descripcion;
    private String secuencia;

    // Constructor vacío
    public ComprobanteFiscal() { }

    // Constructor con parámetros
    public ComprobanteFiscal(int idComprobante, String tipo, String descripcion, String secuencia) {
        this.idComprobante = idComprobante;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.secuencia = secuencia;
    }

    // Getters y setters
    public int getIdComprobante() { return idComprobante; }
    public void setIdComprobante(int idComprobante) { this.idComprobante = idComprobante; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getSecuencia() { return secuencia; }
    public void setSecuencia(String secuencia) { this.secuencia = secuencia; }

    @Override
    public String toString() {
        return tipo + " - " + descripcion;
    }
}
