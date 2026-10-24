package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.Lote;
import com.unev.farmjm.model.Medicamento;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.service.MedicamentoService;
import com.unev.farmjm.util.Async;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

/** Inventario & Lotes: tabla enlazada a las propiedades observables de Medicamento, con CRUD en modales. */
public class InventarioView extends VBox implements Refrescable {

    private final MedicamentoService servicio = new MedicamentoService();
    private final TableView<Medicamento> tabla = new TableView<>(Estado.medicamentos);
    private final Label statProductos = Ui.label("0", "stat");
    private final Label statStock = Ui.label("0", "stat");
    private final Label statAlertas = Ui.label("0", "stat");
    private final Label lblError = Ui.label("", "error-text");

    public InventarioView() {
        setSpacing(14);
        Rejilla stats = new Rejilla(3, 3, 1);
        stats.setNodos(List.of(stat("Medicamentos", statProductos), stat("Unidades en stock", statStock),
                stat("Lotes vencidos o por vencer (≤ 90 días)", statAlertas)));

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getColumns().add(col("Código", m -> m.getCodigo()));
        tabla.getColumns().add(col("Medicamento", m -> m.getNombre()));

        TableColumn<Medicamento, String> activo = new TableColumn<>("Principio activo");
        activo.setCellValueFactory(c -> c.getValue().principioActivoProperty());   // propiedad observable
        tabla.getColumns().add(activo);
        TableColumn<Medicamento, String> pres = new TableColumn<>("Presentación");
        pres.setCellValueFactory(c -> c.getValue().presentacionProperty());       // propiedad observable
        tabla.getColumns().add(pres);

        tabla.getColumns().add(col("Categoría", m -> m.getCategoria().getNombre()));
        tabla.getColumns().add(col("Precio", m -> Ui.dinero(m.getPrecio())));
        tabla.getColumns().add(col("Stock", m -> String.valueOf(m.getStock())));
        tabla.getColumns().add(col("Lote", m -> m.getLoteProximo() == null ? "-" : m.getLoteProximo().getCodigoLote()));
        tabla.getColumns().add(col("Vence", m -> m.getLoteProximo() == null ? "-" : m.getLoteProximo().getFechaVencimiento().toString()));
        tabla.getColumns().add(col("Estado", m -> estado(m)));
        Responsive.columnasPrincipales(tabla,
                new String[]{"Código", "Medicamento", "Precio", "Stock", "Vence", "Estado"},
                new String[]{"Medicamento", "Precio", "Stock", "Estado"});
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = Ui.boton("＋ Nuevo medicamento", "primary-btn", () -> abrir(null));
        Button editar = Ui.boton("Editar", "secondary-btn", () -> abrir(tabla.getSelectionModel().getSelectedItem()));
        Button eliminar = Ui.boton("Eliminar", "danger-btn", this::eliminar);
        editar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        eliminar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        ocultarSiNoPuede(nuevo, Accion.CREAR);
        ocultarSiNoPuede(editar, Accion.EDITAR);
        ocultarSiNoPuede(eliminar, Accion.ELIMINAR);
        FlowPane barra = new FlowPane(10, 10, nuevo, editar, eliminar);
        barra.getStyleClass().add("toolbar");

        getChildren().addAll(Ui.label("Inventario & Lotes", "h1"),
                Ui.label("Existencias por medicamento y lote más próximo a vencer (regla FEFO).", "muted"),
                stats, barra, lblError, tabla);
        refrescar();
    }

    private void ocultarSiNoPuede(Button b, Accion a) {
        boolean ok = Permisos.puede(Estado.usuario, Modulo.INVENTARIO, a);
        b.setVisible(ok);
        b.setManaged(ok);
    }

    private void abrir(Medicamento m) {
        Async.run(servicio::categorias,
                cats -> new MedicamentoDialog(getScene().getWindow(), servicio, cats, m, this::recargar).mostrar(),
                ex -> Ui.error(Async.mensaje(ex)));
    }

    private void eliminar() {
        Medicamento m = tabla.getSelectionModel().getSelectedItem();
        if (m == null) return;
        if (!Ui.confirmar("Eliminar medicamento", "¿Eliminar " + m.getNombre() + " del inventario? "
                + "Sus ventas anteriores se conservan.")) return;
        Async.run(() -> { servicio.eliminar(Estado.usuario, m); return Boolean.TRUE; },
                ok -> recargar(), ex -> Ui.error(Async.mensaje(ex)));
    }

    /** Vuelve a leer los medicamentos de MySQL y actualiza la tabla sin reiniciar la aplicación. */
    private void recargar() {
        lblError.setText("");
        Async.run(() -> servicio.listar(Estado.usuario),
                lista -> { Estado.medicamentos.setAll(lista); refrescar(); },
                ex -> lblError.setText(Async.mensaje(ex)));
    }

    private String estado(Medicamento m) {
        Lote l = m.getLoteProximo();
        if (l != null && l.estaVencido()) return "Vencido";
        if (l != null && l.getDiasParaVencer() <= 30) return "Vence en " + l.getDiasParaVencer() + " días";
        if (l != null && l.getDiasParaVencer() <= 90) return "Por vencer";
        if (m.getStock() <= 10) return "Stock bajo";
        return "Vigente";
    }

    private TableColumn<Medicamento, String> col(String titulo, Function<Medicamento, String> f) {
        TableColumn<Medicamento, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }

    private VBox stat(String titulo, Label valor) {
        VBox v = new VBox(2, Ui.label(titulo, "field-label"), valor);
        v.getStyleClass().add("panel");
        v.setPadding(new Insets(12, 18, 12, 18));
        return v;
    }

    @Override
    public void refrescar() {
        statProductos.setText(String.valueOf(Estado.medicamentos.size()));
        statStock.setText(String.valueOf(Estado.medicamentos.stream().mapToInt(Medicamento::getStock).sum()));
        statAlertas.setText(String.valueOf(Estado.alertas().size()));
        tabla.refresh();
    }
}
