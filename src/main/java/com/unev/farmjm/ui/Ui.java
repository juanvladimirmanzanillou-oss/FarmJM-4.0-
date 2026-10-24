package com.unev.farmjm.ui;

import com.unev.farmjm.model.Lote;
import com.unev.farmjm.util.Fmt;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Utilidades de interfaz compartidas. */
public class Ui {

    private static String css;

    /** Hoja de estilos única del sistema (paleta con variables en .root). */
    public static String css() {
        if (css == null) css = Ui.class.getResource("/css/farmjm.css").toExternalForm();
        return css;
    }

    public static Label label(String texto, String... clases) {
        Label l = new Label(texto);
        l.getStyleClass().addAll(clases);
        return l;
    }

    public static Button boton(String texto, String clase, Runnable accion) {
        Button b = new Button(texto);
        b.getStyleClass().add(clase);
        b.setOnAction(e -> accion.run());
        return b;
    }

    public static Region espacio() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static String dinero(double v) { return Fmt.dinero(v); }

    /** Etiqueta del lote con color según los días para vencer (rojo ≤ 30, amarillo ≤ 90, verde > 90). */
    public static Label badgeLote(Lote l) {
        if (l == null) return label("S/L", "badge", "badge-warn");
        String clase = l.getDiasParaVencer() <= 30 ? "badge-bad" : l.getDiasParaVencer() <= 90 ? "badge-warn" : "badge-ok";
        return label(l.getCodigoLote(), "badge", clase);
    }

    private static void estilo(Dialog<?> d) {
        d.getDialogPane().getStylesheets().add(css());
        Responsive.clase(d.getDialogPane(), Responsive.modo.get());
    }

    public static void info(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("FarmJM");
        a.setHeaderText(titulo);
        a.setContentText(mensaje);
        estilo(a);
        a.showAndWait();
    }

    public static void error(String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("FarmJM");
        a.setHeaderText("No se pudo completar la acción");
        a.setContentText(mensaje);
        estilo(a);
        a.showAndWait();
    }

    public static boolean confirmar(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        a.setTitle("FarmJM");
        a.setHeaderText(titulo);
        estilo(a);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
