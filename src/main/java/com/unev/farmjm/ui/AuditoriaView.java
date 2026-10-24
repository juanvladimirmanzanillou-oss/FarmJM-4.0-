package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.dao.BitacoraDAO.Acceso;
import com.unev.farmjm.model.LogActividad;
import com.unev.farmjm.service.BitacoraService;
import com.unev.farmjm.util.Async;
import com.unev.farmjm.util.Fmt;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Auditoría: actividad del sistema y bitácora de inicios de sesión (exitosos y fallidos). */
public class AuditoriaView extends VBox implements Refrescable {

    private final BitacoraService servicio = new BitacoraService();
    private final ObservableList<Acceso> accesos = FXCollections.observableArrayList();
    private final Label lblError = Ui.label("", "error-text");

    public AuditoriaView() {
        setSpacing(14);
        TableView<LogActividad> t = new TableView<>(Estado.logs);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.getColumns().add(col("Fecha y hora", l -> l.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm:ss a"))));
        t.getColumns().add(col("Usuario", l -> l.getUsuario().getNombreCompleto()));
        t.getColumns().add(col("Módulo", l -> l.getModulo()));
        t.getColumns().add(col("Acción", l -> l.getAccion()));
        Responsive.columnasPrincipales(t, "Módulo", "Acción");

        TableView<Acceso> b = new TableView<>(accesos);
        b.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        b.getColumns().add(colA("Fecha y hora", a -> Fmt.fechaHora(a.fecha())));
        b.getColumns().add(colA("Usuario", Acceso::usuario));
        b.getColumns().add(colA("Resultado", Acceso::resultado));
        b.getColumns().add(colA("Detalle", a -> a.detalle() == null ? "" : a.detalle()));
        Responsive.columnasPrincipales(b, "Usuario", "Resultado", "Detalle");

        Tab tActividad = new Tab("Actividad del sistema", t);
        Tab tAccesos = new Tab("Bitácora de accesos", b);
        tActividad.setClosable(false);
        tAccesos.setClosable(false);
        TabPane tabs = new TabPane(tActividad, tAccesos);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().addAll(Ui.label("Auditoría & Logs", "h1"),
                Ui.label("Cada acción importante y cada inicio de sesión (exitoso o fallido) queda registrado.", "muted"),
                lblError, tabs);
    }

    @Override
    public void refrescar() {
        lblError.setText("");
        Async.run(() -> servicio.recientes(Estado.usuario, 200), accesos::setAll, ex -> lblError.setText(Async.mensaje(ex)));
    }

    private TableColumn<LogActividad, String> col(String titulo, Function<LogActividad, String> f) {
        TableColumn<LogActividad, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }

    private TableColumn<Acceso, String> colA(String titulo, Function<Acceso, String> f) {
        TableColumn<Acceso, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }
}
