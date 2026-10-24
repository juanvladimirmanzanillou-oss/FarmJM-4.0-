package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase del modelo: Caja. */
public class Caja {

    // Atributos privados (encapsulamiento)
    private int idCaja;
    private Usuario usuario;
    private double montoBase;
    private LocalDateTime apertura;
    private LocalDateTime cierre;
    private boolean abierta;

    // Constructor vacío
    public Caja() { }

    // Constructor con parámetros
    public Caja(int idCaja, Usuario usuario, double montoBase, LocalDateTime apertura, LocalDateTime cierre, boolean abierta) {
        this.idCaja = idCaja;
        this.usuario = usuario;
        this.montoBase = montoBase;
        this.apertura = apertura;
        this.cierre = cierre;
        this.abierta = abierta;
    }

    // Getters y setters
    public int getIdCaja() { return idCaja; }
    public void setIdCaja(int idCaja) { this.idCaja = idCaja; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public double getMontoBase() { return montoBase; }
    public void setMontoBase(double montoBase) { this.montoBase = montoBase; }
    public LocalDateTime getApertura() { return apertura; }
    public void setApertura(LocalDateTime apertura) { this.apertura = apertura; }
    public LocalDateTime getCierre() { return cierre; }
    public void setCierre(LocalDateTime cierre) { this.cierre = cierre; }
    public boolean isAbierta() { return abierta; }
    public void setAbierta(boolean abierta) { this.abierta = abierta; }

    @Override
    public String toString() {
        return "Caja #" + idCaja;
    }
}
