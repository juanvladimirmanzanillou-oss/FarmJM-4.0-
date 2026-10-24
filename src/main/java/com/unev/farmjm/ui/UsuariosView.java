package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Rol;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.service.UsuarioService;
import com.unev.farmjm.util.Async;
import com.unev.farmjm.util.Fmt;
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

import java.util.List;
import java.util.function.Function;

/** Administración de usuarios y roles (solo Administrador): crear, asignar rol, activar/desactivar y restablecer 2FA. */
public class UsuariosView extends VBox implements Refrescable {

    private final UsuarioService servicio = new UsuarioService();
    private final ObservableList<Usuario> datos = FXCollections.observableArrayList();
    private final TableView<Usuario> tabla = new TableView<>(datos);
    private final Label lblError = Ui.label("", "error-text");
    private List<Rol> roles = List.of();

    public UsuariosView() {
        setSpacing(14);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getColumns().add(col("Usuario", Usuario::getUsername));
        tabla.getColumns().add(col("Nombre", Usuario::getNombreCompleto));
        tabla.getColumns().add(col("Rol", u -> u.getRol().getNombre()));
        tabla.getColumns().add(col("Estado", u -> u.isActivo() ? "Activo" : "Desactivado"));
        tabla.getColumns().add(col("2FA", u -> u.isTotpActivo() ? "Activo" : "Pendiente"));
        tabla.getColumns().add(col("Bloqueo", u -> u.estaBloqueado() ? "Hasta " + Fmt.fechaHora(u.getBloqueadoHasta()) : "-"));
        Responsive.columnasPrincipales(tabla, "Usuario", "Rol", "Estado");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = Ui.boton("＋ Nuevo usuario", "primary-btn", () -> abrir(null));
        Button editar = Ui.boton("Editar rol / estado", "secondary-btn", () -> abrir(tabla.getSelectionModel().getSelectedItem()));
        Button reset = Ui.boton("Restablecer 2FA", "secondary-btn", this::restablecer2fa);
        editar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        reset.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        boolean puede = Permisos.puede(Estado.usuario, Modulo.USUARIOS, Accion.CREAR);
        nuevo.setDisable(!puede);

        FlowPane barra = new FlowPane(10, 10, nuevo, editar, reset);
        barra.getStyleClass().add("toolbar");
        getChildren().addAll(Ui.label("Usuarios y Roles", "h1"),
                Ui.label("Solo el Administrador gestiona cuentas. Las contraseñas se guardan con hash BCrypt.", "muted"),
                barra, lblError, tabla);
    }

    private void abrir(Usuario u) {
        new UsuarioDialog(getScene().getWindow(), servicio, roles, u, this::refrescar).mostrar();
    }

    private void restablecer2fa() {
        Usuario u = tabla.getSelectionModel().getSelectedItem();
        if (u == null) return;
        if (!Ui.confirmar("Restablecer 2FA", "Se borrará la clave 2FA de " + u.getUsername()
                + ". Deberá escanear un código QR nuevo en su próximo ingreso. ¿Continuar?")) return;
        Async.run(() -> { servicio.restablecer2fa(Estado.usuario, u); return Boolean.TRUE; },
                ok -> refrescar(), ex -> Ui.error(Async.mensaje(ex)));
    }

    @Override
    public void refrescar() {
        lblError.setText("");
        Async.run(() -> new Object[]{servicio.listar(Estado.usuario), servicio.roles(Estado.usuario)},
                r -> {
                    @SuppressWarnings("unchecked") List<Usuario> us = (List<Usuario>) r[0];
                    @SuppressWarnings("unchecked") List<Rol> rs = (List<Rol>) r[1];
                    roles = rs;
                    datos.setAll(us);
                },
                ex -> lblError.setText(Async.mensaje(ex)));
    }

    private TableColumn<Usuario, String> col(String titulo, Function<Usuario, String> f) {
        TableColumn<Usuario, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }
}
