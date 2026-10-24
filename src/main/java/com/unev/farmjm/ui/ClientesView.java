package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Cliente;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.service.ClienteService;
import com.unev.farmjm.util.Async;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/** Módulo de clientes con CRUD en ventanas modales. */
public class ClientesView extends VBox implements Refrescable {

    private final ClienteService servicio = new ClienteService();
    private final ObservableList<Cliente> datos = FXCollections.observableArrayList();
    private final TableView<Cliente> tabla = new TableView<>(datos);
    private final Label lblError = Ui.label("", "error-text");

    public ClientesView() {
        setSpacing(14);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getColumns().add(col("Nombre", Cliente::getNombre));
        tabla.getColumns().add(col("Cédula / RNC", Cliente::getCedulaRnc));
        tabla.getColumns().add(col("Teléfono", Cliente::getTelefono));
        tabla.getColumns().add(col("Correo", Cliente::getEmail));
        Responsive.columnasPrincipales(tabla, "Nombre", "Teléfono");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = Ui.boton("＋ Nuevo cliente", "primary-btn", () -> abrir(null));
        Button editar = Ui.boton("Editar", "secondary-btn", () -> abrir(tabla.getSelectionModel().getSelectedItem()));
        Button eliminar = Ui.boton("Eliminar", "danger-btn", this::eliminar);
        editar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        eliminar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        nuevo.setVisible(Permisos.puede(Estado.usuario, Modulo.CLIENTES, Accion.CREAR));
        nuevo.setManaged(nuevo.isVisible());
        editar.setVisible(Permisos.puede(Estado.usuario, Modulo.CLIENTES, Accion.EDITAR));
        editar.setManaged(editar.isVisible());
        eliminar.setVisible(Permisos.puede(Estado.usuario, Modulo.CLIENTES, Accion.ELIMINAR));
        eliminar.setManaged(eliminar.isVisible());

        FlowPane barra = new FlowPane(10, 10, nuevo, editar, eliminar);
        barra.getStyleClass().add("toolbar");
        getChildren().addAll(Ui.label("Clientes", "h1"),
                Ui.label("Registro de clientes para facturación y recetas.", "muted"), barra, lblError, tabla);
    }

    private void abrir(Cliente c) {
        new ClienteDialog(getScene().getWindow(), servicio, c, this::refrescar).mostrar();
    }

    private void eliminar() {
        Cliente c = tabla.getSelectionModel().getSelectedItem();
        if (c == null) return;
        if (!Ui.confirmar("Eliminar cliente", "¿Eliminar a " + c.getNombre() + "? Sus facturas anteriores se conservan.")) return;
        Async.run(() -> { servicio.eliminar(Estado.usuario, c); return Boolean.TRUE; },
                ok -> refrescar(), ex -> Ui.error(Async.mensaje(ex)));
    }

    @Override
    public void refrescar() {
        lblError.setText("");
        Async.run(() -> servicio.listar(Estado.usuario), datos::setAll, ex -> lblError.setText(Async.mensaje(ex)));
    }

    private TableColumn<Cliente, String> col(String titulo, Function<Cliente, String> f) {
        TableColumn<Cliente, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue()) == null ? "" : f.apply(cd.getValue())));
        return c;
    }
}
