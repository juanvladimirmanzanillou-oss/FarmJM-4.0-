package com.unev.farmjm.model;

/** Clase del modelo: Medico. */
public class Medico {

    // Atributos privados (encapsulamiento)
    private int idMedico;
    private String nombre;
    private String exequatur;
    private String especialidad;

    // Constructor vacío
    public Medico() { }

    // Constructor con parámetros
    public Medico(int idMedico, String nombre, String exequatur, String especialidad) {
        this.idMedico = idMedico;
        this.nombre = nombre;
        this.exequatur = exequatur;
        this.especialidad = especialidad;
    }

    // Getters y setters
    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int idMedico) { this.idMedico = idMedico; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getExequatur() { return exequatur; }
    public void setExequatur(String exequatur) { this.exequatur = exequatur; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    @Override
    public String toString() {
        return nombre;
    }
}
