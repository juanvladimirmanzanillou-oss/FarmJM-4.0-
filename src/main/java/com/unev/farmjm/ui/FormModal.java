package com.unev.farmjm.ui;

import com.unev.farmjm.util.Async;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Ventana modal para crear o editar (Stage con Modality.APPLICATION_MODAL + initOwner).
 * Valida los datos y muestra los errores dentro del modal sin cerrarlo; al guardar con éxito
 * se cierra y avisa a la ventana principal para que actualice su tabla.
 */
public abstract class FormModal {

    protected final Stage stage = new Stage();
    private final VBox form = new VBox(10);
    private final Label lblError = Ui.label("", "error-text");
    private final Button btnGuardar = new Button("Guardar");
    private final Button btnCancelar = new Button("Cancelar");
    private final Runnable alGuardar;

    protected FormModal(Window dueno, String titulo, String subtitulo, Runnable alGuardar) {
        this.alGuardar = alGuardar;
        stage.initOwner(dueno);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(titulo);

        lblError.setWrapText(true);
        lblError.setMinHeight(Label.USE_PREF_SIZE);
        btnGuardar.getStyleClass().add("primary-btn");
        btnGuardar.setDefaultButton(true);
        btnCancelar.getStyleClass().add("secondary-btn");
        btnCancelar.setCancelButton(true);
        btnCancelar.setOnAction(e -> stage.close());
        btnGuardar.setOnAction(e -> intentarGuardar());

        HBox botones = new HBox(10, Ui.espacio(), btnCancelar, btnGuardar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        VBox raiz = new VBox(14, Ui.label(titulo, "h1"), Ui.label(subtitulo, "muted"), form, lblError, botones);
        raiz.getStyleClass().add("modal-root");
        raiz.setPadding(new Insets(22));
        raiz.setPrefWidth(460);

        ScrollPane sp = new ScrollPane(raiz);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("modal-scroll");
        Scene escena = new Scene(sp);
        escena.getStylesheets().add(Ui.css());
        Responsive.clase(sp, Responsive.modo.get());
        stage.setScene(escena);
        stage.setMinWidth(340);
        stage.setMaxHeight(760);
    }

    /** Agrega un campo con su etiqueta encima. */
    protected void campo(String etiqueta, Node control) {
        form.getChildren().add(new VBox(4, Ui.label(etiqueta, "field-label"), control));
    }

    /** Agrega un control sin etiqueta (por ejemplo, casillas). */
    protected void fila(Node... nodos) {
        form.getChildren().add(new HBox(18, nodos));
    }

    /** Validación en el formulario. Devuelve el mensaje de error o null si todo está correcto. */
    protected abstract String validar();

    /** Se ejecuta en segundo plano; lanza ServicioException si algo falla. */
    protected abstract void guardar();

    public void mostrar() {
        stage.showAndWait();
    }

    private void intentarGuardar() {
        String err = validar();
        if (err != null) {
            lblError.setText(err);
            return;
        }
        lblError.setText("");
        btnGuardar.setDisable(true);
        btnCancelar.setDisable(true);
        Async.run(() -> { guardar(); return Boolean.TRUE; },
                ok -> { stage.close(); alGuardar.run(); },
                ex -> {
                    lblError.setText(Async.mensaje(ex));
                    btnGuardar.setDisable(false);
                    btnCancelar.setDisable(false);
                });
    }
}
