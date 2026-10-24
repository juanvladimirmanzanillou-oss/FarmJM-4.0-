package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Rol;
import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.service.UsuarioService;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.util.List;

/** Modal para crear o editar usuarios (solo Administrador). */
public class UsuarioDialog extends FormModal {

    private final UsuarioService servicio;
    private final Usuario original;
    private final TextField txtNombre = new TextField();
    private final TextField txtApellido = new TextField();
    private final TextField txtUsername = new TextField();
    private final TextField txtEmail = new TextField();
    private final TextField txtLicencia = new TextField();
    private final ComboBox<Rol> cmbRol = new ComboBox<>();
    private final PasswordField txtClave = new PasswordField();
    private final CheckBox chkActivo = new CheckBox("Cuenta activa");

    public UsuarioDialog(Window dueno, UsuarioService servicio, List<Rol> roles, Usuario editar, Runnable alGuardar) {
        super(dueno, editar == null ? "Nuevo usuario" : "Editar usuario",
                editar == null ? "Cree la cuenta y asigne su rol. El 2FA se activa en el primer ingreso."
                        : "Cambie el rol, el estado o los datos. Deje la contraseña vacía para conservarla.", alGuardar);
        this.servicio = servicio;
        this.original = editar;
        for (TextField t : new TextField[]{txtNombre, txtApellido, txtUsername, txtEmail, txtLicencia}) t.getStyleClass().add("input");
        txtClave.getStyleClass().add("input");
        cmbRol.getStyleClass().add("combo-clean");
        cmbRol.setMaxWidth(Double.MAX_VALUE);
        cmbRol.getItems().setAll(roles);
        txtClave.setPromptText(editar == null ? "Mínimo 8 caracteres, letras y números" : "Nueva contraseña (opcional)");

        campo("NOMBRE *", txtNombre);
        campo("APELLIDO *", txtApellido);
        campo("USUARIO *", txtUsername);
        campo("CORREO ELECTRÓNICO", txtEmail);
        campo("LICENCIA / MATRÍCULA", txtLicencia);
        campo("ROL *", cmbRol);
        campo(editar == null ? "CONTRASEÑA *" : "NUEVA CONTRASEÑA", txtClave);
        fila(chkActivo);

        if (editar != null) {
            txtNombre.setText(editar.getNombre());
            txtApellido.setText(editar.getApellido());
            txtUsername.setText(editar.getUsername());
            txtUsername.setDisable(true);
            txtEmail.setText(editar.getEmail());
            txtLicencia.setText(editar.getLicencia());
            chkActivo.setSelected(editar.isActivo());
            roles.stream().filter(r -> r.getIdRol() == editar.getRol().getIdRol()).findFirst().ifPresent(cmbRol::setValue);
        } else {
            chkActivo.setSelected(true);
        }
    }

    @Override
    protected String validar() {
        if (txtNombre.getText().isBlank() || txtApellido.getText().isBlank() || txtUsername.getText().isBlank()) {
            return "Complete nombre, apellido y usuario.";
        }
        if (cmbRol.getValue() == null) return "Seleccione un rol.";
        if (original == null && txtClave.getText().isEmpty()) return "Escriba una contraseña para el nuevo usuario.";
        return null;
    }

    @Override
    protected void guardar() {
        Usuario u = original == null ? new Usuario() : copia(original);
        u.setNombre(txtNombre.getText().trim());
        u.setApellido(txtApellido.getText().trim());
        u.setUsername(txtUsername.getText().trim());
        u.setEmail(txtEmail.getText().trim());
        u.setLicencia(txtLicencia.getText().trim());
        u.setRol(cmbRol.getValue());
        u.setActivo(chkActivo.isSelected());
        if (original == null) servicio.crear(Estado.usuario, u, txtClave.getText());
        else servicio.actualizar(Estado.usuario, u, txtClave.getText());
    }

    private Usuario copia(Usuario o) {
        Usuario u = new Usuario();
        u.setIdUsuario(o.getIdUsuario());
        return u;
    }
}
