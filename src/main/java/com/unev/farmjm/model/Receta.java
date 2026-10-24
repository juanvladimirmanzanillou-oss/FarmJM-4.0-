package com.unev.farmjm.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Clase del modelo: Receta. */
public class Receta {

    // Atributos privados (encapsulamiento)
    private int idReceta;
    private Medico medico;
    private Cliente cliente;
    private LocalDate fecha;
    private boolean controlada;
    private List<DetalleReceta> detalles = new ArrayList<>();

    // Constructor vacío
    public Receta() { }

    // Constructor con parámetros
    public Receta(int idReceta, Medico medico, Cliente cliente, LocalDate fecha, boolean controlada) {
        this.idReceta = idReceta;
        this.medico = medico;
        this.cliente = cliente;
        this.fecha = fecha;
        this.controlada = controlada;
    }

    // Getters y setters
    public int getIdReceta() { return idReceta; }
    public void setIdReceta(int idReceta) { this.idReceta = idReceta; }
    public Medico getMedico() { return medico; }
    public void setMedico(Medico medico) { this.medico = medico; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public boolean isControlada() { return controlada; }
    public void setControlada(boolean controlada) { this.controlada = controlada; }
    public List<DetalleReceta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleReceta> detalles) { this.detalles = detalles; }

    // Métodos auxiliares
    public void agregarDetalle(DetalleReceta d) { detalles.add(d); }

    @Override
    public String toString() {
        return "Receta #" + idReceta;
    }
}
