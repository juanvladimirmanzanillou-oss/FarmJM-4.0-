package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase del modelo: ArqueoCaja. */
public class ArqueoCaja {

    // Atributos privados (encapsulamiento)
    private int idArqueo;
    private Caja caja;
    private double montoEsperado;
    private double montoContado;
    private double diferencia;
    private LocalDateTime fecha;

    // Constructor vacío
    public ArqueoCaja() { }

    // Constructor con parámetros
    public ArqueoCaja(int idArqueo, Caja caja, double montoEsperado, double montoContado, double diferencia, LocalDateTime fecha) {
        this.idArqueo = idArqueo;
        this.caja = caja;
        this.montoEsperado = montoEsperado;
        this.montoContado = montoContado;
        this.diferencia = diferencia;
        this.fecha = fecha;
    }

    // Getters y setters
    public int getIdArqueo() { return idArqueo; }
    public void setIdArqueo(int idArqueo) { this.idArqueo = idArqueo; }
    public Caja getCaja() { return caja; }
    public void setCaja(Caja caja) { this.caja = caja; }
    public double getMontoEsperado() { return montoEsperado; }
    public void setMontoEsperado(double montoEsperado) { this.montoEsperado = montoEsperado; }
    public double getMontoContado() { return montoContado; }
    public void setMontoContado(double montoContado) { this.montoContado = montoContado; }
    public double getDiferencia() { return diferencia; }
    public void setDiferencia(double diferencia) { this.diferencia = diferencia; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    @Override
    public String toString() {
        return "Arqueo #" + idArqueo;
    }
}
