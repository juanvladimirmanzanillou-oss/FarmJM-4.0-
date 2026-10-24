package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Devolucion;
import com.unev.farmjm.model.Factura;
import com.unev.farmjm.model.Medicamento;
import com.unev.farmjm.service.DevolucionService;
import com.unev.farmjm.util.Async;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Devoluciones y Notas de Crédito (NCF B04). */
public class DevolucionesView extends VBox {

    private final DevolucionService servicio = new DevolucionService();
    private final TextField txtFactura = new TextField();
    private final ComboBox<Medicamento> cmbProducto = new ComboBox<>(Estado.medicamentos);
    private final Spinner<Integer> spCantidad = new Spinner<>(1, 100, 1);
    private final TextField txtMotivo = new TextField();

    public DevolucionesView() {
        setSpacing(14);
        txtFactura.setPromptText("Ej.: F-00001");
        txtMotivo.setPromptText("Motivo de la devolución");
        for (TextField t : new TextField[]{txtFactura, txtMotivo}) t.getStyleClass().add("input");
        cmbProducto.getStyleClass().add("combo-clean");
        cmbProducto.setPrefWidth(260);
        spCantidad.setPrefWidth(90);
        HBox.setHgrow(txtMotivo, Priority.ALWAYS);

        Button emitir = new Button("Emitir nota de crédito");
        emitir.getStyleClass().add("primary-btn");
        emitir.setOnAction(e -> emitir());

        javafx.scene.layout.FlowPane form = new javafx.scene.layout.FlowPane(12, 10, campo("FACTURA", txtFactura), campo("MEDICAMENTO", cmbProducto),
                campo("CANTIDAD", spCantidad), campo("MOTIVO", txtMotivo), new VBox(emitir));
        form.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        form.getStyleClass().add("panel");
        form.setPadding(new Insets(16));

        TableView<Devolucion> t = new TableView<>(Estado.devoluciones);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.getColumns().add(col("Fecha", d -> d.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a"))));
        t.getColumns().add(col("Nota de crédito", d -> d.getNcfNotaCredito()));
        t.getColumns().add(col("Factura", d -> d.getFactura().getNumero()));
        t.getColumns().add(col("Medicamento", d -> d.getProducto().getNombre()));
        t.getColumns().add(col("Cant.", d -> String.valueOf(d.getCantidad())));
        t.getColumns().add(col("Motivo", d -> d.getMotivo()));
        t.getColumns().add(col("Monto", d -> Ui.dinero(d.getMonto())));
        Responsive.columnasPrincipales(t, "Nota de crédito", "Medicamento", "Monto");
        VBox.setVgrow(t, Priority.ALWAYS);

        getChildren().addAll(Ui.label("Devoluciones (NC)", "h1"),
                Ui.label("Cada devolución genera una nota de crédito (NCF B04) y regresa las unidades al inventario. La lista muestra las emitidas en esta sesión.", "muted"), form, t);
    }

    private VBox campo(String titulo, javafx.scene.Node n) {
        return new VBox(4, Ui.label(titulo, "field-label"), n);
    }

    private void emitir() {
        Medicamento m = cmbProducto.getValue();
        String numero = txtFactura.getText();
        String motivo = txtMotivo.getText();
        int cant = spCantidad.getValue();
        Async.run(() -> servicio.emitir(Estado.usuario, numero, m, cant, motivo),
                r -> {
                    Factura f = new Factura();
                    f.setNumero(numero.trim());
                    Estado.devoluciones.add(0, new Devolucion(Estado.devoluciones.size() + 1, f, m, cant,
                            motivo.trim(), r.monto(), r.ncf(), LocalDateTime.now()));
                    m.setStock(m.getStock() + cant);   // las unidades regresaron al inventario
                    Ui.info("Nota de crédito emitida", r.ncf() + "\nMonto: " + Ui.dinero(r.monto()));
                    txtFactura.clear();
                    txtMotivo.clear();
                },
                ex -> Ui.info("No se pudo emitir la nota de crédito", Async.mensaje(ex)));
    }

    private TableColumn<Devolucion, String> col(String titulo, Function<Devolucion, String> f) {
        TableColumn<Devolucion, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }
}
