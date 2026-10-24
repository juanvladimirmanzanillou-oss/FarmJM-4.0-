package com.unev.farmjm.ui;

import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.service.AuthService;
import com.unev.farmjm.util.Async;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Primer factor: usuario y contraseña. */
public class LoginView extends StackPane {

    private final AuthService auth;
    private final TextField txtUsuario = new TextField();
    private final PasswordField txtClave = new PasswordField();
    private final Label lblError = Ui.label("", "error-text");
    private final Button btnEntrar = new Button("Iniciar sesión");

    public LoginView(AuthService auth, String aviso, Consumer<Usuario> alValidar) {
        this.auth = auth;
        getStyleClass().add("login-root");

        txtUsuario.setPromptText("Usuario");
        txtUsuario.getStyleClass().add("input");
        txtClave.setPromptText("Contraseña");
        txtClave.getStyleClass().add("input");
        btnEntrar.getStyleClass().add("primary-btn");
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setDefaultButton(true);
        lblError.setWrapText(true);
        lblError.setText(aviso == null ? "" : aviso);

        Label logo = Ui.label("✚", "brand-icon");
        VBox marca = new VBox(6, logo, Ui.label("FARMACIA JM", "login-title"),
                Ui.label("Sistema Integral de Farmacia · Release 2.0", "muted"));
        marca.setAlignment(Pos.CENTER);

        VBox card = new VBox(12, marca, Ui.label("USUARIO", "field-label"), txtUsuario,
                Ui.label("CONTRASEÑA", "field-label"), txtClave, lblError, btnEntrar,
                Ui.label("Acceso protegido con verificación en dos pasos (2FA).", "muted"));
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setMaxHeight(Region_USE_PREF);
        card.setAlignment(Pos.CENTER_LEFT);
        for (javafx.scene.Node n : card.getChildren()) if (n instanceof Label l) l.setWrapText(true);
        StackPane.setMargin(card, new Insets(16));
        getChildren().add(card);
        setAlignment(Pos.CENTER);

        btnEntrar.setOnAction(e -> entrar(alValidar));
        javafx.application.Platform.runLater(txtUsuario::requestFocus);
    }

    private static final double Region_USE_PREF = javafx.scene.layout.Region.USE_PREF_SIZE;

    private void entrar(Consumer<Usuario> alValidar) {
        lblError.setText("");
        btnEntrar.setDisable(true);
        btnEntrar.setText("Verificando...");
        String u = txtUsuario.getText();
        String c = txtClave.getText();
        Async.run(() -> auth.primerFactor(u, c),
                usuario -> alValidar.accept(usuario),
                ex -> {
                    lblError.setText(Async.mensaje(ex));
                    txtClave.clear();
                    btnEntrar.setDisable(false);
                    btnEntrar.setText("Iniciar sesión");
                });
    }
}
