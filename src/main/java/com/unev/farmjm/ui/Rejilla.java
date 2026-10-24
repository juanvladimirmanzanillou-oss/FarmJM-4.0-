package com.unev.farmjm.ui;

import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;

/** Cuadrícula que reorganiza sus elementos según el modo: por ejemplo 4 columnas en escritorio, 2 en tableta y 1 en teléfono. */
public class Rejilla extends GridPane {

    private final int colEscritorio, colTableta, colTelefono;
    private final List<Node> nodos = new ArrayList<>();

    public Rejilla(int colEscritorio, int colTableta, int colTelefono) {
        this.colEscritorio = colEscritorio;
        this.colTableta = colTableta;
        this.colTelefono = colTelefono;
        setHgap(14);
        setVgap(14);
        Responsive.alCambiar(m -> distribuir());
    }

    public void setNodos(List<? extends Node> nuevos) {
        nodos.clear();
        nodos.addAll(nuevos);
        distribuir();
    }

    private void distribuir() {
        Modo m = Responsive.modo.get();
        int cols = m == Modo.ESCRITORIO ? colEscritorio : m == Modo.TABLETA ? colTableta : colTelefono;
        getChildren().clear();
        getColumnConstraints().clear();
        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(cc);
        }
        for (int i = 0; i < nodos.size(); i++) {
            Node n = nodos.get(i);
            if (n instanceof javafx.scene.layout.Region r) r.setMaxWidth(Double.MAX_VALUE);
            add(n, i % cols, i / cols);
        }
    }
}
