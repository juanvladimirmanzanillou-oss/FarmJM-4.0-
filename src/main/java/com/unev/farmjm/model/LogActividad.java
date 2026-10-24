package com.unev.farmjm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Clase del modelo: LogActividad. */
public class LogActividad {

    // Atributos privados (encapsulamiento)
    private int idLog;
    private Usuario usuario;
    private String accion;
    private String modulo;
    private LocalDateTime fecha;

    // Constructor vacío
    public LogActividad() { }

    // Constructor con parámetros
    public LogActividad(int idLog, Usuario usuario, String accion, String modulo, LocalDateTime fecha) {
        this.idLog = idLog;
        this.usuario = usuario;
        this.accion = accion;
        this.modulo = modulo;
        this.fecha = fecha;
    }

    // Getters y setters
    public int getIdLog() { return idLog; }
    public void setIdLog(int idLog) { this.idLog = idLog; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    @Override
    public String toString() {
        return modulo + ": " + accion;
    }
}
