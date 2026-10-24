package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Receta;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/** Recetas / Controlados. */
public class RecetasView extends VBox {

    public RecetasView() {
        setSpacing(14);
        TableView<Receta> t = new TableView<>(Estado.recetas);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.getColumns().add(col("N.º receta", r -> String.valueOf(r.getIdReceta())));
        t.getColumns().add(col("Fecha", r -> r.getFecha().toString()));
        t.getColumns().add(col("Médico", r -> r.getMedico().getNombre()));
        t.getColumns().add(col("Exequátur", r -> r.getMedico().getExequatur()));
        t.getColumns().add(col("Paciente", r -> r.getCliente().getNombre()));
        t.getColumns().add(col("Medicamento", r -> r.getDetalles().get(0).getMedicamento().getNombre()));
        t.getColumns().add(col("Cant.", r -> String.valueOf(r.getDetalles().get(0).getCantidad())));
        t.getColumns().add(col("Indicaciones", r -> r.getDetalles().get(0).getIndicaciones()));
        t.getColumns().add(col("Tipo", r -> r.isControlada() ? "CONTROLADO" : "Con receta"));
        Responsive.columnasPrincipales(t, "Paciente", "Medicamento", "Tipo");
        VBox.setVgrow(t, Priority.ALWAYS);
        getChildren().addAll(Ui.label("Recetas / Controlados", "h1"),
                Ui.label("Registro de recetas médicas y medicamentos de control especial.", "muted"), t);
    }

    private TableColumn<Receta, String> col(String titulo, Function<Receta, String> f) {
        TableColumn<Receta, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }
}
