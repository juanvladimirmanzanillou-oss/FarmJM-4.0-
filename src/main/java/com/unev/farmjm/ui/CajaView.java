package com.unev.farmjm.ui;

import com.unev.farmjm.Estado;
import com.unev.farmjm.model.ArqueoCaja;
import com.unev.farmjm.model.Caja;
import com.unev.farmjm.security.Accion;
import com.unev.farmjm.security.Modulo;
import com.unev.farmjm.security.Permisos;
import com.unev.farmjm.service.CajaService;
import com.unev.farmjm.util.Async;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Arqueo & Cierre de Caja. */
public class CajaView extends VBox implements Refrescable {

    private final Label estado = Ui.label("", "h1");
    private final Label cajero = new Label();
    private final Label apertura = new Label();
    private final Label base = new Label();
    private final Label ventas = new Label();
    private final Label esperado = Ui.label("", "stat");
    private final TextField contado = new TextField();
    private final Button btnCerrar = new Button("Cerrar caja y generar arqueo");
    private final Button btnAbrir = new Button("Abrir nueva caja");
    private final CajaService servicio = new CajaService();

    public CajaView() {
        setSpacing(14);
        GridPane g = new GridPane();
        g.setHgap(24);
        g.setVgap(10);
        g.addRow(0, Ui.label("CAJERO", "field-label"), cajero);
        g.addRow(1, Ui.label("APERTURA", "field-label"), apertura);
        g.addRow(2, Ui.label("MONTO BASE", "field-label"), base);
        g.addRow(3, Ui.label("VENTAS EN EFECTIVO", "field-label"), ventas);
        g.addRow(4, Ui.label("EFECTIVO ESPERADO", "field-label"), esperado);

        contado.setPromptText("Monto contado en caja (RD$)");
        contado.getStyleClass().add("input");
        btnCerrar.getStyleClass().add("primary-btn");
        btnAbrir.getStyleClass().add("primary-btn");
        btnCerrar.setOnAction(e -> cerrar());
        btnAbrir.setOnAction(e -> abrir());

        VBox panel = new VBox(14, estado, g, Ui.label("MONTO CONTADO", "field-label"), contado, btnCerrar, btnAbrir);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(22));
        panel.setMaxWidth(480);
        panel.setMinWidth(0);

        getChildren().addAll(Ui.label("Arqueo & Cierre de Caja", "h1"),
                Ui.label("Compara el efectivo esperado contra el efectivo contado.", "muted"), panel);
        refrescar();
    }

    private void cerrar() {
        double cont;
        try {
            cont = Double.parseDouble(contado.getText().trim().replace(",", ""));
        } catch (Exception ex) {
            Ui.info("Monto inválido", "Escriba el monto contado en números.");
            return;
        }
        Caja c = Estado.caja;
        double efectivo = Estado.ventasEfectivo;
        final double contadoFinal = cont;
        btnCerrar.setDisable(true);
        Async.run(() -> servicio.cerrar(Estado.usuario, c, efectivo, contadoFinal),
                a -> {
                    c.setAbierta(false);
                    c.setCierre(a.getFecha());
                    Estado.cajaAbierta.set(false);
                    Ui.info("Caja cerrada", "Esperado: " + Ui.dinero(a.getMontoEsperado()) + "\nContado: " + Ui.dinero(contadoFinal)
                            + "\nDiferencia: " + Ui.dinero(a.getDiferencia()));
                    contado.clear();
                    refrescar();
                },
                ex -> { Ui.error(Async.mensaje(ex)); refrescar(); });
    }

    private void abrir() {
        btnAbrir.setDisable(true);
        Async.run(() -> servicio.abrir(Estado.usuario),
                c -> {
                    Estado.caja = c;
                    Estado.ventasEfectivo = 0;
                    Estado.cajaAbierta.set(true);
                    btnAbrir.setDisable(false);
                    refrescar();
                },
                ex -> { btnAbrir.setDisable(false); Ui.error(Async.mensaje(ex)); });
    }

    @Override
    public void refrescar() {
        Caja c = Estado.caja;
        boolean abierta = Estado.cajaAbierta.get();
        estado.setText(c.getIdCaja() == 0 ? "Sin caja abierta" : String.format("Caja #%03d — %s", c.getIdCaja(), abierta ? "Abierta" : "Cerrada"));
        cajero.setText(Estado.usuario.getNombreCompleto());
        apertura.setText(c.getApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a")));
        base.setText(Ui.dinero(c.getMontoBase()));
        ventas.setText(Ui.dinero(Estado.ventasEfectivo));
        esperado.setText(Ui.dinero(c.getMontoBase() + Estado.ventasEfectivo));
        contado.setDisable(!abierta);
        btnCerrar.setDisable(!abierta);
        boolean puedeAbrir = !abierta && Permisos.puede(Estado.usuario, Modulo.CAJA, Accion.CREAR);
        btnAbrir.setVisible(puedeAbrir);
        btnAbrir.setManaged(puedeAbrir);
    }
}
