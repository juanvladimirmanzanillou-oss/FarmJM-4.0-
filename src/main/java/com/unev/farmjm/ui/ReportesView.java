package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.MainApp;
import com.unev.farmjm.service.ReporteService;
import com.unev.farmjm.util.Async;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Reporte PDF con filtro por rango de fechas; el contenido depende del rol de quien lo genera. */
public class ReportesView extends VBox implements Refrescable {

    private final ReporteService servicio = new ReporteService();
    private final DatePicker dpDesde = new DatePicker();
    private final DatePicker dpHasta = new DatePicker();
    private final Label contenido = Ui.label("", "step-text");
    private final Label lblEstado = Ui.label("", "muted");
    private final Button btnGenerar = new Button("📄 Generar reporte PDF");
    private final ProgressIndicator progreso = new ProgressIndicator();

    public ReportesView() {
        setSpacing(14);
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
        btnGenerar.getStyleClass().add("primary-btn");
        btnGenerar.setOnAction(e -> generar());
        progreso.setPrefSize(22, 22);
        progreso.setVisible(false);
        contenido.setWrapText(true);
        lblEstado.setWrapText(true);

        FlowPane filtros = new FlowPane(16, 10,
                new VBox(4, Ui.label("DESDE", "field-label"), dpDesde),
                new VBox(4, Ui.label("HASTA", "field-label"), dpHasta));
        HBox accion = new HBox(12, btnGenerar, progreso);
        accion.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox panel = new VBox(14, Ui.label("CONTENIDO SEGÚN SU ROL", "field-label"), contenido, filtros, accion, lblEstado);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(22));
        panel.setMaxWidth(560);
        getChildren().addAll(Ui.label("Reportes", "h1"),
                Ui.label("Genere un PDF con encabezado (sistema, fecha y usuario) y filtro por fechas.", "muted"), panel);
        refrescar();
    }

    private void generar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte PDF");
        fc.setInitialFileName("Reporte_FarmJM_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File destino = fc.showSaveDialog(getScene().getWindow());
        if (destino == null) return;
        btnGenerar.setDisable(true);
        progreso.setVisible(true);
        lblEstado.getStyleClass().remove("error-text");
        lblEstado.setText("Generando el reporte...");
        Async.run(() -> servicio.generarPdf(Estado.usuario, desde, hasta, destino),
                f -> {
                    btnGenerar.setDisable(false);
                    progreso.setVisible(false);
                    lblEstado.setText("Reporte guardado en: " + f.getAbsolutePath());
                    if (Ui.confirmar("Reporte generado", "El reporte se guardó en:\n" + f.getAbsolutePath() + "\n\n¿Desea abrirlo ahora?")) {
                        MainApp.abrir(f);
                    }
                },
                ex -> {
                    btnGenerar.setDisable(false);
                    progreso.setVisible(false);
                    lblEstado.getStyleClass().add("error-text");
                    lblEstado.setText(Async.mensaje(ex));
                });
    }

    @Override
    public void refrescar() {
        contenido.setText(servicio.descripcionContenido(Estado.usuario) + ".");
    }
}
