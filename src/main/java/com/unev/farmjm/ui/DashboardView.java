package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.dao.DashboardDAO.Movimiento;
import com.unev.farmjm.service.DashboardData;
import com.unev.farmjm.service.DashboardData.Kpi;
import com.unev.farmjm.service.DashboardService;
import com.unev.farmjm.util.Async;
import com.unev.farmjm.util.Fmt;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Pantalla de inicio tras el login. Indicadores (KPI) y gráficos calculados con consultas a MySQL,
 * ejecutadas en segundo plano con Task. El contenido cambia según el rol.
 */
public class DashboardView extends ScrollPane implements Refrescable {

    private final DashboardService servicio = new DashboardService();
    private final Label subtitulo = Ui.label("", "muted");
    private final ProgressIndicator progreso = new ProgressIndicator();
    private final Label lblError = Ui.label("", "error-text");
    private final Rejilla kpis = new Rejilla(4, 2, 1);
    private final Rejilla graficos = new Rejilla(2, 1, 1);
    private final ObservableList<Movimiento> movimientos = FXCollections.observableArrayList();
    private final VBox panelBarras = new VBox(6);
    private final VBox panelPastel = new VBox(6);

    public DashboardView() {
        progreso.setPrefSize(22, 22);
        progreso.setVisible(false);
        Button actualizar = Ui.boton("↻ Actualizar", "secondary-btn", this::refrescar);
        HBox titulo = new HBox(10, Ui.label("Dashboard", "h1"), progreso, Ui.espacio(), actualizar);
        titulo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TableView<Movimiento> tabla = new TableView<>(movimientos);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getColumns().add(col("Fecha y hora", m -> Fmt.fechaHora(m.fecha())));
        tabla.getColumns().add(col("Usuario", Movimiento::usuario));
        tabla.getColumns().add(col("Módulo", Movimiento::modulo));
        tabla.getColumns().add(col("Acción", Movimiento::accion));
        tabla.setPrefHeight(300);
        tabla.setMinHeight(300);
        tabla.setPlaceholder(new Label("Sin actividad reciente"));
        Responsive.columnasPrincipales(tabla, "Módulo", "Acción");

        panelBarras.getStyleClass().add("chart-panel");
        panelPastel.getStyleClass().add("chart-panel");
        graficos.setNodos(List.of(panelBarras, panelPastel));

        VBox contenido = new VBox(14, titulo, subtitulo, lblError, kpis, graficos,
                Ui.label("Actividad reciente", "chart-title"), tabla);
        contenido.setPadding(new Insets(2, 6, 12, 2));
        setContent(contenido);
        setFitToWidth(true);
        Responsive.alCambiar(m -> setHbarPolicy(ScrollBarPolicy.NEVER));
    }

    @Override
    public void refrescar() {
        progreso.setVisible(true);
        lblError.setText("");
        Async.run(() -> servicio.cargar(Estado.usuario), this::mostrar, ex -> {
            progreso.setVisible(false);
            lblError.setText(Async.mensaje(ex));
        });
    }

    private void mostrar(DashboardData d) {
        progreso.setVisible(false);
        subtitulo.setText(d.subtitulo());

        List<Node> tarjetas = new ArrayList<>();
        for (Kpi k : d.kpis()) {
            VBox t = new VBox(2, Ui.label(k.etiqueta(), "kpi-label"), Ui.label(k.valor(), "kpi-value"));
            t.getStyleClass().add("kpi-card");
            if (!"normal".equals(k.nivel())) t.getStyleClass().add("kpi-" + k.nivel());
            tarjetas.add(t);
        }
        kpis.setNodos(tarjetas);

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> barras = new BarChart<>(x, y);
        barras.setLegendVisible(false);
        barras.setAnimated(false);
        barras.setPrefHeight(270);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Map.Entry<String, Double> e : d.barras().entrySet()) serie.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        barras.getData().add(serie);
        panelBarras.getChildren().setAll(Ui.label(d.tituloBarras(), "chart-title"), barras);

        PieChart pastel = new PieChart();
        pastel.setAnimated(false);
        pastel.setLabelsVisible(false);
        pastel.setPrefHeight(270);
        for (Map.Entry<String, Integer> e : d.pastel().entrySet()) {
            pastel.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        }
        panelPastel.getChildren().setAll(Ui.label(d.tituloPastel(), "chart-title"), pastel);
        VBox.setVgrow(barras, Priority.ALWAYS);
        VBox.setVgrow(pastel, Priority.ALWAYS);

        movimientos.setAll(d.movimientos());
    }

    private TableColumn<Movimiento, String> col(String titulo, Function<Movimiento, String> f) {
        TableColumn<Movimiento, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }
}
