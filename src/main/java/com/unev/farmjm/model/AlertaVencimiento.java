package com.unev.farmjm.model;

/** Clase del modelo: AlertaVencimiento. */
public class AlertaVencimiento {

    // Atributos privados (encapsulamiento)
    private int idAlerta;
    private Lote lote;
    private int diasRestantes;
    private boolean atendida;

    // Constructor vacío
    public AlertaVencimiento() { }

    // Constructor con parámetros
    public AlertaVencimiento(int idAlerta, Lote lote, int diasRestantes, boolean atendida) {
        this.idAlerta = idAlerta;
        this.lote = lote;
        this.diasRestantes = diasRestantes;
        this.atendida = atendida;
    }

    // Getters y setters
    public int getIdAlerta() { return idAlerta; }
    public void setIdAlerta(int idAlerta) { this.idAlerta = idAlerta; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public int getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(int diasRestantes) { this.diasRestantes = diasRestantes; }
    public boolean isAtendida() { return atendida; }
    public void setAtendida(boolean atendida) { this.atendida = atendida; }

    @Override
    public String toString() {
        return "Alerta #" + idAlerta;
    }
}
