package com.unev.farmjm.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Medicamento: hereda de Producto y agrega datos farmacéuticos (propiedades observables JavaFX). */
public class Medicamento extends Producto {

    // Atributos privados con propiedades observables
    private final StringProperty principioActivo = new SimpleStringProperty();
    private final StringProperty presentacion = new SimpleStringProperty();
    private final BooleanProperty requiereReceta = new SimpleBooleanProperty();
    private final BooleanProperty controlado = new SimpleBooleanProperty();

    // Constructor vacío
    public Medicamento() {
        super();
    }

    // Constructor con parámetros (llama al constructor de la clase padre)
    public Medicamento(int idProducto, String codigo, String nombre, double precio, int stock,
                       Categoria categoria, Proveedor proveedor,
                       String principioActivo, String presentacion,
                       boolean requiereReceta, boolean controlado) {
        super(idProducto, codigo, nombre, precio, stock, categoria, proveedor);
        this.principioActivo.set(principioActivo);
        this.presentacion.set(presentacion);
        this.requiereReceta.set(requiereReceta);
        this.controlado.set(controlado);
    }

    // Getters y setters
    public String getPrincipioActivo() { return principioActivo.get(); }
    public void setPrincipioActivo(String v) { principioActivo.set(v); }
    public String getPresentacion() { return presentacion.get(); }
    public void setPresentacion(String v) { presentacion.set(v); }
    public boolean isRequiereReceta() { return requiereReceta.get(); }
    public void setRequiereReceta(boolean v) { requiereReceta.set(v); }
    public boolean isControlado() { return controlado.get(); }
    public void setControlado(boolean v) { controlado.set(v); }

    // Propiedades observables (para enlazar con TableView / formularios)
    public StringProperty principioActivoProperty() { return principioActivo; }
    public StringProperty presentacionProperty() { return presentacion; }
    public BooleanProperty requiereRecetaProperty() { return requiereReceta; }
    public BooleanProperty controladoProperty() { return controlado; }

    // Polimorfismo: sobrescribe toString() de Producto
    @Override
    public String toString() {
        return getNombre() + " (" + principioActivo.get() + ")";
    }
}
