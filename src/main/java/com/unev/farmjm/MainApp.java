package com.unev.farmjm;

import com.unev.farmjm.model.Usuario;
import com.unev.farmjm.service.AuthService;
import com.unev.farmjm.ui.*;
import com.unev.farmjm.util.Async;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;

/** Flujo: Login (1.er factor) → verificación 2FA (2.º factor) → carga de datos → aplicación según el rol. */
public class MainApp extends Application {

    private static HostServices servicios;
    private final AuthService auth = new AuthService();
    private Stage stage;
    private Scene scene;
    private AppShell shell;

    @Override
    public void start(Stage primaryStage) {
        servicios = getHostServices();
        stage = primaryStage;
        scene = new Scene(new VBox(), 1366, 740);
        scene.getStylesheets().add(Ui.css());          // única hoja de estilos con la paleta (variables en .root)
        Responsive.instalar(scene);                    // escucha scene.widthProperty() → escritorio / tableta / teléfono

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), () -> { if (shell != null) shell.atajoF2(); });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F4), () -> { if (shell != null) shell.mostrar(AppShell.CAJA); });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F8), () -> { if (shell != null) shell.mostrar(AppShell.INVENTARIO); });
        // Atajos para tomar las capturas de evidencia con los tamaños de referencia
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN), () -> tamano(1366, 768));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN), () -> tamano(1024, 768));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN), () -> tamano(768, 1024));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN), () -> tamano(412, 915));

        stage.setTitle("FarmJM - Sistema Integral de Farmacia");
        stage.setMinWidth(360);
        stage.setMinHeight(520);
        stage.setScene(scene);
        mostrarLogin(null);
        stage.show();
    }

    /** Ctrl+1 escritorio 1366×768 · Ctrl+2 tableta 1024×768 · Ctrl+3 tableta vertical 768×1024 · Ctrl+4 teléfono 412×915. */
    private void tamano(double ancho, double alto) {
        stage.setMaximized(false);
        stage.setWidth(ancho + (stage.getWidth() - scene.getWidth()));
        stage.setHeight(alto + (stage.getHeight() - scene.getHeight()));
    }

    private void mostrarLogin(String aviso) {
        shell = null;
        Estado.cerrarSesion();
        scene.setRoot(new LoginView(auth, aviso, this::mostrarVerificacion));
    }

    private void mostrarVerificacion(Usuario u) {
        scene.setRoot(new DosFactoresView(u, auth, this::iniciarSesion, this::mostrarLogin));
    }

    private void iniciarSesion(Usuario u) {
        VBox espera = new VBox(12, new ProgressIndicator(), Ui.label("Cargando datos desde MySQL...", "muted"));
        espera.setAlignment(Pos.CENTER);
        scene.setRoot(espera);
        Async.run(() -> Estado.cargar(u),
                carga -> {
                    Estado.aplicar(carga);
                    shell = new AppShell(() -> mostrarLogin(null));
                    scene.setRoot(shell);
                },
                ex -> mostrarLogin(Async.mensaje(ex)));
    }

    /** Abre un archivo con la aplicación predeterminada del sistema (por ejemplo, el PDF del reporte). */
    public static void abrir(File f) {
        if (servicios != null) servicios.showDocument(f.toURI().toString());
    }

    @Override
    public void stop() {
        com.unev.farmjm.db.ConexionBD.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
