package com.unev.farmjm.ui;

import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.security.QrUtil;
import com.unev.farmjm.security.Totp;
import com.unev.farmjm.service.AuthException;
import com.unev.farmjm.service.AuthService;
import com.unev.farmjm.util.Async;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Segundo factor (pantalla separada del login). Si el usuario aún no tiene 2FA se muestra el código QR
 * para escanearlo con Google Authenticator o Microsoft Authenticator; si ya lo tiene, solo pide el código.
 */
public class DosFactoresView extends StackPane {

    private final TextField txtCodigo = new TextField();
    private final Label lblError = Ui.label("", "error-text");
    private final Button btnVerificar = new Button("Verificar");

    public DosFactoresView(Usuario usuario, AuthService auth, Consumer<Usuario> alExito, Consumer<String> alVolver) {
        getStyleClass().add("login-root");
        boolean activar = !usuario.isTotpActivo();
        final String secreto = activar ? auth.nuevoSecreto() : null;

        VBox card = new VBox(12);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(420);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        card.getChildren().add(Ui.label(activar ? "Active la verificación en dos pasos" : "Verificación en dos pasos", "login-title"));
        if (activar) {
            card.getChildren().addAll(
                    Ui.label("1. Instale Google Authenticator o Microsoft Authenticator en su teléfono.", "step-text"),
                    Ui.label("2. Escanee este código QR (o escriba la clave manualmente).", "step-text"));
            ImageView qr = new ImageView(QrUtil.generar(Totp.uri("FarmJM", usuario.getUsername(), secreto), 200));
            qr.setFitWidth(200);
            qr.setFitHeight(200);
            VBox caja = new VBox(qr);
            caja.setAlignment(Pos.CENTER);
            Label clave = Ui.label(agrupar(secreto), "secret-box");
            clave.setWrapText(true);
            clave.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().addAll(caja, Ui.label("CLAVE MANUAL", "field-label"), clave,
                    Ui.label("3. Escriba el código de 6 dígitos que muestra la aplicación.", "step-text"));
        } else {
            card.getChildren().add(Ui.label("Escriba el código de 6 dígitos de su aplicación Authenticator.", "step-text"));
        }

        txtCodigo.setPromptText("000000");
        txtCodigo.getStyleClass().addAll("input", "code-field");
        txtCodigo.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().matches("\\d{0,6}") ? c : null));
        lblError.setWrapText(true);
        btnVerificar.getStyleClass().add("primary-btn");
        btnVerificar.setMaxWidth(Double.MAX_VALUE);
        btnVerificar.setDefaultButton(true);
        Button volver = Ui.boton("Volver al inicio de sesión", "link-btn", () -> alVolver.accept(null));
        card.getChildren().addAll(Ui.label("CÓDIGO", "field-label"), txtCodigo, lblError, btnVerificar,
                Ui.label("Máximo " + AuthService.MAX_INTENTOS + " intentos; luego la cuenta se bloquea "
                        + AuthService.MINUTOS_BLOQUEO + " minutos.", "muted"), volver);

        btnVerificar.setOnAction(e -> {
            lblError.setText("");
            btnVerificar.setDisable(true);
            String codigo = txtCodigo.getText();
            Async.run(() -> activar ? auth.activarTotp(usuario, secreto, codigo) : auth.segundoFactor(usuario, codigo),
                    alExito::accept,
                    ex -> {
                        String msg = Async.mensaje(ex);
                        if (ex instanceof AuthException ae && ae.volverAlLogin()) {
                            alVolver.accept(msg);
                            return;
                        }
                        lblError.setText(msg);
                        txtCodigo.clear();
                        btnVerificar.setDisable(false);
                    });
        });

        card.setPadding(new Insets(24));
        for (javafx.scene.Node n : card.getChildren()) if (n instanceof Label l) l.setWrapText(true);
        VBox centro = new VBox(card);
        centro.setAlignment(Pos.CENTER);
        centro.setFillWidth(true);
        centro.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(centro);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("login-root");
        getChildren().add(scroll);
        javafx.application.Platform.runLater(txtCodigo::requestFocus);
    }

    private static String agrupar(String s) {
        return s.replaceAll("(.{4})", "$1 ").trim();
    }
}
